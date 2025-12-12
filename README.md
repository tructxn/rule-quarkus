# Quarkus Bazel Rules

Bazel rules for building Quarkus applications with build-time augmentation.

## Version

| Component | Version |
|-----------|---------|
| Quarkus | 3.20.1 |
| Bazel | 7.x+ |
| Java | 21 |

## Two Approaches

| Approach | Directory | Status | Use Case |
|----------|-----------|--------|----------|
| **v1-custom** | `v1-custom/` | Compiles, CDI limited | Educational, lightweight |
| **v2-bootstrap** | `v2-bootstrap/` | ✅ **Fully Working** | Production, full features |

---

## Quick Start (v2-bootstrap - Recommended)

```bash
# Build the application
bazel build //v2-bootstrap/examples/hello-world:hello-world

# Run the application
bazel-bin/v2-bootstrap/examples/hello-world/hello-world

# Test HTTP endpoints (in another terminal)
curl http://localhost:8080/hello
# Output: Hello from Quarkus (built with Bazel)!
```

### Demo Extensions (Full Featured)

```bash
# Build demo with all extensions
bazel build //v2-bootstrap/examples/demo-extensions:demo-extensions

# Run
bazel-bin/v2-bootstrap/examples/demo-extensions/demo-extensions

# Test endpoints
curl http://localhost:8080/              # All endpoints
curl http://localhost:8080/api/users     # REST + Jackson
curl http://localhost:8080/q/health      # Health checks
curl http://localhost:8080/q/metrics     # Prometheus metrics
```

---

## Supported Extensions (5 Tiers)

### Tier 1: Core
- `quarkus-arc` - CDI implementation
- `quarkus-vertx-http` - HTTP server
- `quarkus-mutiny` - Reactive programming
- `quarkus-rest-jackson` - REST + JSON

### Tier 2: Database
- `quarkus-reactive-oracle-client`
- `quarkus-reactive-mysql-client`
- `quarkus-redis-client`

### Tier 3: Messaging
- `quarkus-messaging-kafka`
- `quarkus-messaging-rabbitmq`
- `quarkus-grpc`

### Tier 4: Observability
- `quarkus-micrometer-registry-prometheus`
- `quarkus-smallrye-health`

### Tier 5: Quarkiverse
- `quarkus-unleash` (1.10.0)
- `quarkus-langchain4j-core/openai/ollama` (0.26.1)
- `quarkus-tika` (2.1.0)

---

## v2-bootstrap (Production) ✅

Uses official `QuarkusBootstrap` API for full Quarkus feature support.

### Features
- ✅ Full CDI/ArC support
- ✅ REST endpoints (JAX-RS) with Jackson
- ✅ HTTP server (Vert.x + Netty)
- ✅ Reactive programming (Mutiny)
- ✅ Health checks & Metrics
- ✅ Quarkiverse extensions (LangChain4j, etc.)
- ✅ Same output as Maven build

### Creating a New Application

```python
# BUILD.bazel
load("//v2-bootstrap/rules:quarkus.bzl", "quarkus_application")

quarkus_application(
    name = "my-app",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),

    deps = [
        "@maven//:io_quarkus_quarkus_core",
        "@maven//:jakarta_enterprise_jakarta_enterprise_cdi_api",
        "@maven//:jakarta_inject_jakarta_inject_api",
        "@maven//:jakarta_ws_rs_jakarta_ws_rs_api",
    ],

    runtime_extensions = [
        "@maven//:io_quarkus_quarkus_arc",
        "@maven//:io_quarkus_quarkus_rest",
        "@maven//:io_quarkus_quarkus_rest_jackson",
        "@maven//:io_quarkus_quarkus_vertx_http",
        "@maven//:io_quarkus_quarkus_smallrye_health",
    ],

    deployment_extensions = [
        "@maven//:io_quarkus_quarkus_arc_deployment",
        "@maven//:io_quarkus_quarkus_rest_deployment",
        "@maven//:io_quarkus_quarkus_rest_jackson_deployment",
        "@maven//:io_quarkus_quarkus_vertx_http_deployment",
        "@maven//:io_quarkus_quarkus_smallrye_health_deployment",
    ],

    jvm_flags = [
        "-Xmx512m",
        "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
    ],
)
```

---

## v1-custom (Educational)

Custom implementation of Quarkus augmentation. Manually implements Jandex indexing, annotation discovery, and bytecode generation.

```bash
# Build
bazel build //v1-custom/examples/hello-world:hello-world

# Run (Note: CDI not fully working)
bazel run //v1-custom/examples/hello-world:hello-world
```

**Limitations:** CDI runtime not fully working (ArC processor not integrated)

---

## Project Structure

```
rules_quarkus/
├── MODULE.bazel              # Shared Maven dependencies (Quarkus 3.20.1)
├── CLAUDE.md                 # Detailed documentation
│
├── docs/                     # Documentation
│   └── USAGE_GUIDE.md        # Hướng dẫn tích hợp
│
├── v1-custom/                # Approach 1: Custom augmentation
│   ├── rules/                # Starlark rules
│   ├── tools/                # Java augmentation tools
│   └── examples/hello-world/
│
└── v2-bootstrap/             # Approach 2: QuarkusBootstrap API ✅
    ├── rules/                # Starlark rules
    ├── tools/                # Bootstrap tools
    ├── PLAN.md               # Technical details
    └── examples/
        ├── hello-world/      # Basic REST example
        └── demo-extensions/  # Full extensions demo
```

---

## Documentation

- **[docs/USAGE_GUIDE.md](docs/USAGE_GUIDE.md)** - Hướng dẫn tích hợp vào project của bạn
- **[CLAUDE.md](CLAUDE.md)** - Detailed usage guide and troubleshooting
- **[v2-bootstrap/README.md](v2-bootstrap/README.md)** - v2-bootstrap documentation
- **[v2-bootstrap/PLAN.md](v2-bootstrap/PLAN.md)** - Technical implementation details

---

## License

Apache 2.0
