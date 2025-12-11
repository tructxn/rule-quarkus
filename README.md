# Quarkus Bazel Rules

Bazel rules for building Quarkus applications with build-time augmentation.

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

curl http://localhost:8080/hello/World
# Output: Hello, World!
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

## v2-bootstrap (Production) ✅

Uses official `QuarkusBootstrap` API for full Quarkus feature support.

### Features
- ✅ Full CDI/ArC support
- ✅ REST endpoints (JAX-RS)
- ✅ HTTP server (Vert.x + Netty)
- ✅ Dependency injection
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
        "@maven//:io_quarkus_quarkus_vertx_http",
    ],

    deployment_extensions = [
        "@maven//:io_quarkus_quarkus_arc_deployment",
        "@maven//:io_quarkus_quarkus_rest_deployment",
        "@maven//:io_quarkus_quarkus_vertx_http_deployment",
    ],

    jvm_flags = [
        "-Xmx512m",
        "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
    ],
)
```

---

## Project Structure

```
rules_quarkus/
├── MODULE.bazel              # Shared Maven dependencies
├── CLAUDE.md                 # Detailed documentation
│
├── v1-custom/                # Approach 1: Custom augmentation
│   ├── rules/                # Starlark rules
│   ├── tools/                # Java augmentation tools
│   └── examples/hello-world/
│
└── v2-bootstrap/             # Approach 2: QuarkusBootstrap API ✅
    ├── rules/                # Starlark rules
    ├── tools/                # Bootstrap tools
    └── examples/hello-world/ # Working example with REST
```

---

## Requirements

- Bazel 7.x+
- Java 21
- Quarkus 3.17.4

---

## Documentation

- **[CLAUDE.md](CLAUDE.md)** - Detailed usage guide and troubleshooting
- **[v2-bootstrap/PLAN.md](v2-bootstrap/PLAN.md)** - Technical implementation details

---

## License

Apache 2.0
