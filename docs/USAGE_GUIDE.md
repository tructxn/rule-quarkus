# Hướng dẫn sử dụng Quarkus Bazel Rules

Hướng dẫn này giúp bạn tích hợp `rules_quarkus` vào project Quarkus + Bazel của mình.

## Mục lục

- [Yêu cầu](#yêu-cầu)
- [Cấu trúc Project](#cấu-trúc-project)
- [Bước 1: Copy Rules](#bước-1-copy-rules)
- [Bước 2: Cấu hình MODULE.bazel](#bước-2-cấu-hình-modulebazel)
- [Bước 3: Tạo BUILD.bazel](#bước-3-tạo-buildbazel)
- [Bước 4: Viết Code](#bước-4-viết-code)
- [Bước 5: Build và Run](#bước-5-build-và-run)
- [Cấu hình Extensions](#cấu-hình-extensions)
- [Ví dụ đầy đủ](#ví-dụ-đầy-đủ)
- [Troubleshooting](#troubleshooting)

---

## Yêu cầu

| Component | Version |
|-----------|---------|
| Bazel | 7.x+ |
| Java | 21 |
| Quarkus | 3.20.1 |

---

## Cấu trúc Project

```
my-quarkus-app/
├── MODULE.bazel                    # Maven dependencies
├── BUILD.bazel                     # Root build (có thể trống)
├── .bazelrc                        # Bazel config (optional)
│
├── v2-bootstrap/                   # ← Copy từ rules_quarkus
│   ├── rules/
│   │   ├── quarkus.bzl
│   │   ├── quarkus_bootstrap.bzl
│   │   └── defs.bzl
│   └── tools/
│       ├── BUILD.bazel
│       └── src/main/java/...
│
├── app/                            # Application của bạn
│   ├── BUILD.bazel
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/
│           │       ├── GreetingResource.java
│           │       └── GreetingService.java
│           └── resources/
│               └── application.properties
│
└── services/                       # Thêm services khác (optional)
    ├── user-service/
    └── order-service/
```

---

## Bước 1: Copy Rules

Copy thư mục `v2-bootstrap/` từ `rules_quarkus` vào project của bạn:

```bash
# Clone rules_quarkus
git clone https://github.com/user/rules_quarkus.git /tmp/rules_quarkus

# Copy v2-bootstrap vào project
cp -r /tmp/rules_quarkus/v2-bootstrap ./

# Cleanup
rm -rf /tmp/rules_quarkus
```

Hoặc thêm như git submodule:

```bash
git submodule add https://github.com/user/rules_quarkus.git vendor/rules_quarkus
ln -s vendor/rules_quarkus/v2-bootstrap ./v2-bootstrap
```

---

## Bước 2: Cấu hình MODULE.bazel

Tạo file `MODULE.bazel` ở root của project:

```python
module(name = "my-quarkus-app", version = "1.0.0")

# ===========================================
# Bazel Dependencies
# ===========================================
bazel_dep(name = "rules_java", version = "7.6.1")
bazel_dep(name = "rules_jvm_external", version = "6.5")

# ===========================================
# Maven Dependencies
# ===========================================
maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")

QUARKUS_VERSION = "3.20.1"

maven.install(
    name = "maven",
    artifacts = [
        # ===========================================
        # Quarkus Bootstrap (Required for augmentation)
        # ===========================================
        "io.quarkus:quarkus-bootstrap-core:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-bootstrap-app-model:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-bootstrap-runner:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-core:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-core-deployment:%s" % QUARKUS_VERSION,

        # ===========================================
        # Jakarta APIs (Compile-time)
        # ===========================================
        "jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1",
        "jakarta.inject:jakarta.inject-api:2.0.1",
        "jakarta.ws.rs:jakarta.ws.rs-api:3.1.0",
        "jakarta.annotation:jakarta.annotation-api:2.1.1",

        # ===========================================
        # Quarkus Runtime Extensions
        # ===========================================
        "io.quarkus:quarkus-arc:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-rest:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-rest-jackson:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-vertx-http:%s" % QUARKUS_VERSION,

        # ===========================================
        # Quarkus Deployment Extensions
        # ===========================================
        "io.quarkus:quarkus-arc-deployment:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-vertx-http-deployment:%s" % QUARKUS_VERSION,

        # ===========================================
        # Additional Libraries
        # ===========================================
        "com.fasterxml.jackson.core:jackson-databind:2.17.0",
        "com.fasterxml.jackson.core:jackson-annotations:2.17.0",
        "io.quarkus.gizmo:gizmo:1.8.0",
        "org.jboss:jandex:3.1.6",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

# ===========================================
# Deployment artifacts với exclusions
# (Tránh circular dependencies với Maven/Sisu)
# ===========================================
maven.artifact(
    artifact = "quarkus-rest-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
        "org.apache.maven:maven-xml-impl",
        "org.codehaus.plexus:plexus-xml",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)

maven.artifact(
    artifact = "quarkus-rest-jackson-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
        "org.apache.maven:maven-xml-impl",
        "org.codehaus.plexus:plexus-xml",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)

use_repo(maven, "maven")
```

---

## Bước 3: Tạo BUILD.bazel

### Root BUILD.bazel

```python
# BUILD.bazel (root - có thể trống hoặc chứa common configs)
```

### Application BUILD.bazel

Tạo file `app/BUILD.bazel`:

```python
load("//v2-bootstrap/rules:quarkus.bzl", "quarkus_application")

quarkus_application(
    name = "my-app",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),

    # ===========================================
    # Compile-time dependencies (APIs only)
    # ===========================================
    deps = [
        "@maven//:io_quarkus_quarkus_core",
        "@maven//:jakarta_enterprise_jakarta_enterprise_cdi_api",
        "@maven//:jakarta_inject_jakarta_inject_api",
        "@maven//:jakarta_ws_rs_jakarta_ws_rs_api",
        "@maven//:jakarta_annotation_jakarta_annotation_api",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
    ],

    # ===========================================
    # Quarkus Runtime Extensions
    # ===========================================
    runtime_extensions = [
        "@maven//:io_quarkus_quarkus_arc",
        "@maven//:io_quarkus_quarkus_rest",
        "@maven//:io_quarkus_quarkus_rest_jackson",
        "@maven//:io_quarkus_quarkus_vertx_http",
    ],

    # ===========================================
    # Quarkus Deployment Extensions (for augmentation)
    # ===========================================
    deployment_extensions = [
        "@maven//:io_quarkus_quarkus_arc_deployment",
        "@maven//:io_quarkus_quarkus_rest_deployment",
        "@maven//:io_quarkus_quarkus_rest_jackson_deployment",
        "@maven//:io_quarkus_quarkus_vertx_http_deployment",
    ],

    # ===========================================
    # JVM Flags
    # ===========================================
    jvm_flags = [
        "-Xmx512m",
        "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
    ],
)
```

---

## Bước 4: Viết Code

### REST Resource

`app/src/main/java/com/example/GreetingResource.java`:

```java
package com.example;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @Inject
    GreetingService greetingService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return greetingService.getDefaultGreeting();
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String helloName(@PathParam("name") String name) {
        return greetingService.greet(name);
    }
}
```

### CDI Service

`app/src/main/java/com/example/GreetingService.java`:

```java
package com.example;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String getDefaultGreeting() {
        return "Hello from Quarkus (built with Bazel)!";
    }

    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
```

### Application Properties

`app/src/main/resources/application.properties`:

```properties
# Server config
quarkus.http.port=8080

# Logging
quarkus.log.level=INFO
quarkus.log.category."com.example".level=DEBUG

# Native image (optional)
quarkus.native.builder-image=graalvm
```

---

## Bước 5: Build và Run

```bash
# Build application
bazel build //app:my-app

# Run application
bazel-bin/app/my-app

# Test endpoints (trong terminal khác)
curl http://localhost:8080/hello
# Output: Hello from Quarkus (built with Bazel)!

curl http://localhost:8080/hello/World
# Output: Hello, World!
```

### Các lệnh hữu ích

```bash
# Build tất cả targets
bazel build //...

# Clean build
bazel clean --expunge

# Xem dependencies
bazel query "deps(//app:my-app)"

# Xem dependency graph
bazel query "deps(//app:my-app)" --output graph | dot -Tpng > deps.png
```

---

## Cấu hình Extensions

### Extension Mapping Table

Mỗi Quarkus extension cần cả **runtime** và **deployment**:

| Extension | Runtime Artifact | Deployment Artifact |
|-----------|-----------------|---------------------|
| CDI (ArC) | `quarkus-arc` | `quarkus-arc-deployment` |
| REST | `quarkus-rest` | `quarkus-rest-deployment` |
| REST Jackson | `quarkus-rest-jackson` | `quarkus-rest-jackson-deployment` |
| Vert.x HTTP | `quarkus-vertx-http` | `quarkus-vertx-http-deployment` |
| Health | `quarkus-smallrye-health` | `quarkus-smallrye-health-deployment` |
| Metrics | `quarkus-micrometer-registry-prometheus` | `quarkus-micrometer-registry-prometheus-deployment` |
| Mutiny | `quarkus-mutiny` | `quarkus-mutiny-deployment` |

### Thêm Health Checks (Tier 4)

**MODULE.bazel** - thêm dependencies:

```python
maven.install(
    artifacts = [
        # ... existing artifacts ...

        # Health check API
        "org.eclipse.microprofile.health:microprofile-health-api:4.0.1",

        # Runtime
        "io.quarkus:quarkus-smallrye-health:%s" % QUARKUS_VERSION,
    ],
)

maven.artifact(
    artifact = "quarkus-smallrye-health-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)
```

**BUILD.bazel** - thêm extensions:

```python
quarkus_application(
    name = "my-app",
    # ...

    deps = [
        # ... existing deps ...
        "@maven//:org_eclipse_microprofile_health_microprofile_health_api",
    ],

    runtime_extensions = [
        # ... existing extensions ...
        "@maven//:io_quarkus_quarkus_smallrye_health",
    ],

    deployment_extensions = [
        # ... existing extensions ...
        "@maven//:io_quarkus_quarkus_smallrye_health_deployment",
    ],
)
```

**Java Code** - Health check class:

```java
package com.example.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class ApplicationHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("application");
    }
}
```

**Test**:

```bash
curl http://localhost:8080/q/health
curl http://localhost:8080/q/health/live
curl http://localhost:8080/q/health/ready
```

### Thêm Prometheus Metrics (Tier 4)

**MODULE.bazel**:

```python
maven.install(
    artifacts = [
        # ... existing ...
        "io.quarkus:quarkus-micrometer-registry-prometheus:%s" % QUARKUS_VERSION,
    ],
)

maven.artifact(
    artifact = "quarkus-micrometer-registry-prometheus-deployment",
    exclusions = [...],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)
```

**BUILD.bazel**:

```python
runtime_extensions = [
    # ...
    "@maven//:io_quarkus_quarkus_micrometer_registry_prometheus",
],

deployment_extensions = [
    # ...
    "@maven//:io_quarkus_quarkus_micrometer_registry_prometheus_deployment",
],
```

**Test**:

```bash
curl http://localhost:8080/q/metrics
```

---

## Ví dụ đầy đủ

### Full MODULE.bazel với 5 Tiers

```python
module(name = "my-quarkus-app", version = "1.0.0")

bazel_dep(name = "rules_java", version = "7.6.1")
bazel_dep(name = "rules_jvm_external", version = "6.5")

maven = use_extension("@rules_jvm_external//:extensions.bzl", "maven")

QUARKUS_VERSION = "3.20.1"
LANGCHAIN4J_VERSION = "0.26.1"

maven.install(
    name = "maven",
    artifacts = [
        # Bootstrap
        "io.quarkus:quarkus-bootstrap-core:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-bootstrap-app-model:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-bootstrap-runner:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-core:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-core-deployment:%s" % QUARKUS_VERSION,

        # Jakarta APIs
        "jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1",
        "jakarta.inject:jakarta.inject-api:2.0.1",
        "jakarta.ws.rs:jakarta.ws.rs-api:3.1.0",
        "jakarta.annotation:jakarta.annotation-api:2.1.1",

        # Tier 1: Core
        "io.quarkus:quarkus-arc:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-rest:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-rest-jackson:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-vertx-http:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-mutiny:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-arc-deployment:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-vertx-http-deployment:%s" % QUARKUS_VERSION,

        # Tier 2: Database
        "io.quarkus:quarkus-reactive-oracle-client:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-reactive-mysql-client:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-redis-client:%s" % QUARKUS_VERSION,

        # Tier 3: Messaging
        "io.quarkus:quarkus-messaging-kafka:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-messaging-rabbitmq:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-grpc:%s" % QUARKUS_VERSION,

        # Tier 4: Observability
        "io.quarkus:quarkus-smallrye-health:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-micrometer-registry-prometheus:%s" % QUARKUS_VERSION,
        "org.eclipse.microprofile.health:microprofile-health-api:4.0.1",

        # Tier 5: Quarkiverse
        "io.quarkiverse.unleash:quarkus-unleash:1.10.0",
        "io.quarkiverse.langchain4j:quarkus-langchain4j-core:%s" % LANGCHAIN4J_VERSION,
        "io.quarkiverse.langchain4j:quarkus-langchain4j-openai:%s" % LANGCHAIN4J_VERSION,
        "io.quarkiverse.langchain4j:quarkus-langchain4j-ollama:%s" % LANGCHAIN4J_VERSION,
        "io.quarkiverse.tika:quarkus-tika:2.1.0",

        # Additional
        "com.fasterxml.jackson.core:jackson-databind:2.17.0",
        "com.fasterxml.jackson.core:jackson-annotations:2.17.0",
        "io.smallrye.reactive:mutiny:2.6.0",
        "io.quarkus.gizmo:gizmo:1.8.0",
        "org.jboss:jandex:3.1.6",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

# Deployment artifacts với exclusions
maven.artifact(
    artifact = "quarkus-rest-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
        "org.apache.maven:maven-xml-impl",
        "org.codehaus.plexus:plexus-xml",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)

maven.artifact(
    artifact = "quarkus-rest-jackson-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
        "org.apache.maven:maven-xml-impl",
        "org.codehaus.plexus:plexus-xml",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)

maven.artifact(
    artifact = "quarkus-mutiny-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)

maven.artifact(
    artifact = "quarkus-smallrye-health-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)

maven.artifact(
    artifact = "quarkus-micrometer-registry-prometheus-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)

use_repo(maven, "maven")
```

### Full BUILD.bazel

```python
load("//v2-bootstrap/rules:quarkus.bzl", "quarkus_application")

quarkus_application(
    name = "my-app",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),

    deps = [
        # Jakarta APIs
        "@maven//:jakarta_enterprise_jakarta_enterprise_cdi_api",
        "@maven//:jakarta_inject_jakarta_inject_api",
        "@maven//:jakarta_ws_rs_jakarta_ws_rs_api",
        "@maven//:jakarta_annotation_jakarta_annotation_api",

        # Quarkus Core
        "@maven//:io_quarkus_quarkus_core",

        # Mutiny API
        "@maven//:io_smallrye_reactive_mutiny",

        # Health API
        "@maven//:org_eclipse_microprofile_health_microprofile_health_api",

        # Jackson
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_fasterxml_jackson_core_jackson_annotations",
    ],

    runtime_extensions = [
        # Tier 1: Core
        "@maven//:io_quarkus_quarkus_arc",
        "@maven//:io_quarkus_quarkus_rest",
        "@maven//:io_quarkus_quarkus_rest_jackson",
        "@maven//:io_quarkus_quarkus_mutiny",
        "@maven//:io_quarkus_quarkus_vertx_http",

        # Tier 4: Observability
        "@maven//:io_quarkus_quarkus_smallrye_health",
        "@maven//:io_quarkus_quarkus_micrometer_registry_prometheus",
    ],

    deployment_extensions = [
        # Tier 1: Core
        "@maven//:io_quarkus_quarkus_arc_deployment",
        "@maven//:io_quarkus_quarkus_rest_deployment",
        "@maven//:io_quarkus_quarkus_rest_jackson_deployment",
        "@maven//:io_quarkus_quarkus_mutiny_deployment",
        "@maven//:io_quarkus_quarkus_vertx_http_deployment",

        # Tier 4: Observability
        "@maven//:io_quarkus_quarkus_smallrye_health_deployment",
        "@maven//:io_quarkus_quarkus_micrometer_registry_prometheus_deployment",
    ],

    jvm_flags = [
        "-Xmx512m",
        "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
    ],
)
```

---

## Troubleshooting

### Lỗi: Circular dependency với org.eclipse.sisu

**Triệu chứng**:
```
Error: circular dependency detected
```

**Giải pháp**: Thêm `exclusions` cho deployment artifacts trong MODULE.bazel:

```python
maven.artifact(
    artifact = "quarkus-xxx-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)
```

### Lỗi: Duplicate labels

**Triệu chứng**:
```
Error: duplicate label '@maven//:io_quarkus_quarkus_arc'
```

**Giải pháp**: Không đặt cùng một artifact trong cả `deps` và `runtime_extensions`. Runtime extensions đã bao gồm trong classpath.

### Lỗi: ClassNotFoundException during augmentation

**Triệu chứng**:
```
ClassNotFoundException: org.objectweb.asm.ClassVisitor
```

**Giải pháp**: Kiểm tra `v2-bootstrap/tools/BUILD.bazel` có đầy đủ dependencies cho augmentor tool.

### Lỗi: lib/boot/ empty

**Triệu chứng**: Application không start, báo lỗi không tìm thấy bootstrap runner.

**Giải pháp**: Kiểm tra `OutputHandler.java` đã copy `quarkus-bootstrap-runner.jar` với đúng format tên.

### Lỗi: Messaging extensions không start

**Triệu chứng**: Application hang khi dùng Kafka/RabbitMQ extensions.

**Giải pháp**: Messaging extensions cố gắng connect tới broker khi startup. Cấu hình broker trong `application.properties` hoặc bỏ khỏi `runtime_extensions` nếu chỉ cần compile-time.

```properties
# application.properties
kafka.bootstrap.servers=localhost:9092
rabbitmq-host=localhost
rabbitmq-port=5672
```

---

## Tham khảo

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Bazel Documentation](https://bazel.build/docs)
- [rules_jvm_external](https://github.com/bazelbuild/rules_jvm_external)
- [Quarkus Extension Guide](https://quarkus.io/guides/writing-extensions)

---

## Hỗ trợ

Nếu gặp vấn đề:

1. Kiểm tra [Troubleshooting](#troubleshooting)
2. Xem ví dụ trong `v2-bootstrap/examples/`
3. Tạo issue tại repository
