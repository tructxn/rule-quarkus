# Approach 2: QuarkusBootstrap API Integration

This directory contains the implementation of Quarkus Bazel rules using the official **QuarkusBootstrap API** instead of custom augmentation logic.

## Quick Start

```bash
# Build hello-world
bazel build //v2-bootstrap/examples/hello-world:hello-world
bazel-bin/v2-bootstrap/examples/hello-world/hello-world
curl http://localhost:8080/hello

# Build demo-extensions (full featured)
bazel build //v2-bootstrap/examples/demo-extensions:demo-extensions
bazel-bin/v2-bootstrap/examples/demo-extensions/demo-extensions
curl http://localhost:8080/api/users
curl http://localhost:8080/q/health
```

## Version Info

| Component | Version |
|-----------|---------|
| Quarkus | 3.20.1 |
| Bazel | 7.x+ |
| Java | 21 |

## Supported Extensions (5 Tiers)

### Tier 1: Core (Must have)
- `quarkus-arc` - CDI implementation
- `quarkus-vertx-http` - HTTP server
- `quarkus-mutiny` - Reactive programming
- `quarkus-rest-jackson` - REST + JSON

### Tier 2: Database (Reactive)
- `quarkus-reactive-oracle-client`
- `quarkus-reactive-mysql-client`
- `quarkus-redis-client`

### Tier 3: Messaging
- `quarkus-messaging-kafka`
- `quarkus-messaging-rabbitmq`
- `quarkus-grpc`

### Tier 4: Observability
- `quarkus-micrometer-registry-prometheus` - Metrics
- `quarkus-smallrye-health` - Health checks

### Tier 5: Quarkiverse
- `quarkus-unleash` (1.10.0) - Feature flags
- `quarkus-langchain4j-core/openai/ollama` (0.26.1) - AI/LLM
- `quarkus-tika` (2.1.0) - Content analysis

## Why This Approach?

### Problems with Approach 1 (Custom Augmentation)

- Had to re-implement ArC/CDI processor manually
- Hundreds of `@BuildStep` methods to replicate
- Maintenance burden with each Quarkus version
- CDI runtime fails because proxies aren't generated

### Benefits of Approach 2 (QuarkusBootstrap API)

- Uses official Quarkus augmentation pipeline
- All `@BuildStep` methods run automatically
- Full CDI/ArC support out of the box
- Follows Quarkus version upgrades naturally
- Same output as Maven build

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Bazel Build                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 1: java_library        Layer 2: quarkus_bootstrap        │
│  ─────────────────────        ──────────────────────────        │
│  Compile *.java → *.class     Run QuarkusBootstrap API          │
│                                                                 │
│                               ┌───────────────────────────┐     │
│                               │ 1. Parse Bazel deps       │     │
│                               │ 2. Detect extensions      │     │
│                               │ 3. Build ApplicationModel │     │
│                               │ 4. QuarkusBootstrap       │     │
│                               │ 5. CuratedApplication     │     │
│                               │ 6. AugmentAction.run()    │     │
│                               │ 7. Output quarkus-app/    │     │
│                               └───────────────────────────┘     │
│                                                                 │
│  Layer 3: java_binary                                           │
│  ────────────────────                                           │
│  Package runtime with augmented classes                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Key Components

### Java Tools (`tools/`)

```
tools/src/main/java/io/quarkus/bazel/bootstrap/
├── BootstrapAugmentor.java      # Main entry point
├── AugmentationConfig.java      # Configuration object
├── ConfigParser.java            # Parse CLI arguments
├── ApplicationModelFactory.java # Build ApplicationModel from Bazel deps
├── ExtensionDetector.java       # Detect Quarkus extensions from JARs
├── DependencyMapper.java        # Map runtime → deployment artifacts
└── OutputHandler.java           # Handle augmentation output
```

### Bazel Rules (`rules/`)

```
rules/
├── quarkus.bzl                  # Main macro: quarkus_application()
├── quarkus_bootstrap.bzl        # Bootstrap augmentation rule
└── defs.bzl                     # Public API exports
```

## Usage

### BUILD.bazel

```python
load("//v2-bootstrap/rules:quarkus.bzl", "quarkus_application")

quarkus_application(
    name = "my-app",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),

    # Compile-time dependencies (APIs)
    deps = [
        "@maven//:io_quarkus_quarkus_core",
        "@maven//:jakarta_enterprise_jakarta_enterprise_cdi_api",
        "@maven//:jakarta_inject_jakarta_inject_api",
        "@maven//:jakarta_ws_rs_jakarta_ws_rs_api",
    ],

    # Quarkus runtime extensions
    runtime_extensions = [
        "@maven//:io_quarkus_quarkus_arc",
        "@maven//:io_quarkus_quarkus_rest",
        "@maven//:io_quarkus_quarkus_rest_jackson",
        "@maven//:io_quarkus_quarkus_vertx_http",
        "@maven//:io_quarkus_quarkus_smallrye_health",
    ],

    # Quarkus deployment modules (for augmentation)
    deployment_extensions = [
        "@maven//:io_quarkus_quarkus_arc_deployment",
        "@maven//:io_quarkus_quarkus_rest_deployment",
        "@maven//:io_quarkus_quarkus_rest_jackson_deployment",
        "@maven//:io_quarkus_quarkus_vertx_http_deployment",
        "@maven//:io_quarkus_quarkus_smallrye_health_deployment",
    ],

    # JVM flags for running
    jvm_flags = [
        "-Xmx512m",
        "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
    ],
)
```

### Build & Run

```bash
# Build
bazel build //v2-bootstrap/examples/hello-world:hello-world

# Run directly
bazel-bin/v2-bootstrap/examples/hello-world/hello-world

# Test HTTP endpoints (in another terminal)
curl http://localhost:8080/hello
# Output: Hello from Quarkus (built with Bazel)!
```

## Examples

### hello-world

Basic REST application with CDI.

**Endpoints:**
- `GET /hello` - Returns greeting
- `GET /hello/{name}` - Returns personalized greeting

### demo-extensions

Full-featured demo showcasing all extension tiers.

**Endpoints:**
- `GET /` - List all available endpoints
- `GET /api/users` - Tier 1: REST + Jackson + Mutiny
- `GET /api/db/mysql/status` - Tier 2: Database client info
- `GET /api/messaging/kafka/status` - Tier 3: Messaging info
- `GET /q/health` - Tier 4: Health checks
- `GET /q/metrics` - Tier 4: Prometheus metrics
- `GET /api/ai/status` - Tier 5: LangChain4j info

## Required Dependencies

```python
# MODULE.bazel
QUARKUS_VERSION = "3.20.1"

maven.install(
    artifacts = [
        # Bootstrap (required for augmentation tool)
        "io.quarkus:quarkus-bootstrap-core:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-bootstrap-app-model:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-bootstrap-runner:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-core:%s" % QUARKUS_VERSION,

        # Deployment modules (for augmentation)
        "io.quarkus:quarkus-core-deployment:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-arc-deployment:%s" % QUARKUS_VERSION,

        # Runtime modules (for application)
        "io.quarkus:quarkus-arc:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-rest:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-rest-jackson:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-vertx-http:%s" % QUARKUS_VERSION,
    ],
)

# Handle circular dependencies with exclusions
maven.artifact(
    artifact = "quarkus-rest-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)
```

## Comparison with Approach 1

| Aspect | Approach 1 | Approach 2 |
|--------|------------|------------|
| CDI Support | Manual (broken) | Full (automatic) |
| Build Steps | Custom 1 step | All Quarkus steps |
| Maintenance | High | Low |
| Compatibility | Partial | Full |
| Output | Custom JAR | Standard quarkus-app/ |

## Implementation Status

- [x] Core tools implementation
- [x] ApplicationModel building (with proper flag merging)
- [x] Extension detection (via META-INF/quarkus-extension.properties)
- [x] Bazel rules (quarkus_application macro)
- [x] Example application (hello-world with REST)
- [x] Demo application (demo-extensions with all tiers)
- [x] CDI working (full ArC support)
- [x] REST endpoints working (quarkus-rest + vertx-http)
- [x] HTTP server (Vert.x + Netty)
- [x] Bootstrap runner (lib/boot/ with correct naming)
- [x] Health checks & Metrics (Tier 4)
- [x] Quarkiverse extensions (Tier 5)

## Troubleshooting

### ClassCastException: Cannot cast CuratedApplication to CuratedApplication

This happens when deployment and runtime classloaders conflict. Solution: Use flat classpath:

```java
QuarkusBootstrap.builder()
    .setIsolateDeployment(false)
    .setFlatClassPath(true)
    .build();
```

### Circular dependency errors with Maven/Sisu

Some Quarkus deployment modules have circular dependencies with Maven/Sisu. Use exclusions:

```python
maven.artifact(
    artifact = "quarkus-rest-deployment",
    exclusions = ["org.eclipse.sisu:org.eclipse.sisu.plexus"],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)
```

### Empty lib/boot/ directory

The bootstrap runner JAR must be named correctly: `io.quarkus.quarkus-bootstrap-runner-VERSION.jar`

### Messaging extensions not starting

Kafka/RabbitMQ extensions try to connect to brokers on startup. Configure connection in application.properties or use without runtime_extensions for mock-only demo.

## Future Improvements

1. **Native image support** - GraalVM native compilation
2. **Dev mode** - Hot reload during development
3. **Proto/gRPC rules** - Compile .proto files
4. **Testing support** - @QuarkusTest integration
5. **Multi-module** - Shared libraries between apps
