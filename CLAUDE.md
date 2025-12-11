# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is `rules_quarkus` - Bazel rules for building Quarkus applications with build-time augmentation. The project provides two approaches to replicate Quarkus's Maven build process in Bazel.

## Two Approaches

### v1-custom (Educational/Lightweight)
Custom implementation of Quarkus augmentation. Manually implements Jandex indexing, annotation discovery, and bytecode generation.

**Status:** Compiles successfully, generates classes, but CDI runtime not fully working.

### v2-bootstrap (Production-Ready) ✅ RECOMMENDED
Uses official QuarkusBootstrap API for augmentation. Full Quarkus feature support including CDI, REST endpoints, and HTTP server.

**Status:** Fully working with HTTP server and REST endpoints.

---

## Quick Start

### v2-bootstrap (Recommended)

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

### v1-custom

```bash
# Build the application
bazel build //v1-custom/examples/hello-world:hello-world

# Run (Note: CDI not fully working)
bazel run //v1-custom/examples/hello-world:hello-world
```

---

## Build Commands Reference

```bash
# === v2-bootstrap (Recommended) ===
bazel build //v2-bootstrap/examples/hello-world:hello-world   # Build
bazel-bin/v2-bootstrap/examples/hello-world/hello-world       # Run

# === v1-custom ===
bazel build //v1-custom/examples/hello-world:hello-world      # Build
bazel run //v1-custom/examples/hello-world:hello-world        # Run

# === General ===
bazel build //...                    # Build all targets
bazel clean --expunge                # Clean build
bazel query "deps(//v2-bootstrap/examples/hello-world:hello-world)"  # Show deps
```

---

## Architecture

### Three-Layer Build Process

Both approaches follow Quarkus's Maven architecture:

1. **Layer 1 - Compilation** (`java_library`): Compiles application sources to bytecode
2. **Layer 2 - Augmentation**: Processes bytecode via Jandex indexing, annotation discovery, and bytecode generation
3. **Layer 3 - Runtime**: Packages augmented code into executable application

### Directory Structure

```
rules_quarkus/
├── MODULE.bazel              # Shared Maven dependencies
├── CLAUDE.md                 # This file
│
├── v1-custom/                # Approach 1: Custom augmentation
│   ├── rules/                # Starlark rules
│   │   ├── quarkus_application.bzl
│   │   ├── quarkus_augment.bzl
│   │   ├── jandex.bzl
│   │   └── quarkus_extension.bzl
│   ├── tools/                # Java augmentation tools
│   │   └── augmentor/
│   └── examples/
│       └── hello-world/
│
└── v2-bootstrap/             # Approach 2: QuarkusBootstrap API ✅
    ├── rules/                # Starlark rules
    │   ├── quarkus.bzl           # Main macro
    │   └── quarkus_bootstrap.bzl # Bootstrap rule
    ├── tools/                # Java bootstrap tools
    │   └── src/main/java/io/quarkus/bazel/bootstrap/
    │       ├── BootstrapAugmentor.java    # Main entry point
    │       ├── ApplicationModelFactory.java
    │       ├── ConfigParser.java
    │       ├── ExtensionDetector.java
    │       └── OutputHandler.java
    ├── examples/
    │   └── hello-world/      # Working example with REST
    └── PLAN.md               # Technical details
```

---

## v2-bootstrap Details

### How It Works

1. **Build ApplicationModel** from Bazel dependencies with correct flags:
   - `RUNTIME_CP` for runtime dependencies
   - `DEPLOYMENT_CP` for deployment modules
   - `RUNTIME_EXTENSION_ARTIFACT` for Quarkus extensions

2. **Create QuarkusBootstrap** with flat classpath to avoid classloader issues

3. **Run Augmentation** via `AugmentActionImpl`:
   - CDI bean discovery and proxy generation
   - REST endpoint registration
   - Bytecode optimization

4. **Output** to `quarkus-app/` directory:
   - `quarkus-run.jar` - launcher
   - `lib/boot/` - bootstrap runner
   - `lib/main/` - runtime dependencies
   - `app/` - application classes
   - `quarkus/generated-bytecode.jar` - generated code

### Creating a New Application

```python
# BUILD.bazel
load("//v2-bootstrap/rules:quarkus.bzl", "quarkus_application")

quarkus_application(
    name = "my-app",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),

    # Regular dependencies (APIs)
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
        "@maven//:io_quarkus_quarkus_vertx_http",
    ],

    # Quarkus deployment modules (for augmentation)
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

### Example REST Resource

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

---

## Dependencies

- Bazel 7.x+
- Java 21
- Quarkus 3.17.4
- Key libraries:
  - Jandex (bytecode indexing)
  - Gizmo (bytecode generation)
  - Vert.x + Netty (HTTP server)
  - ArC (CDI implementation)

---

## Troubleshooting

### Circular Dependency Errors
If you see errors about `org.eclipse.sisu`, the deployment modules need exclusions. Check `MODULE.bazel` for the `maven.artifact()` calls with exclusions.

### ClassNotFoundException during Augmentation
Ensure all required JARs are in the tool's classpath. Check `v2-bootstrap/tools/BUILD.bazel` for deps.

### HTTP Server Not Starting
Make sure `quarkus-rest` and `quarkus-vertx-http` are in both `runtime_extensions` and their `-deployment` counterparts in `deployment_extensions`.

### lib/boot/ Empty
The `OutputHandler.java` should copy `quarkus-bootstrap-runner.jar` to `lib/boot/`. Check the build output for the copy message.

---

## Known Limitations

### v1-custom
- CDI runtime fails with "Annotation is not a registered qualifier"
- Missing full ArC processor integration

### v2-bootstrap
- All features working
- Some Quarkus extensions may need additional exclusions for Maven circular dependencies
