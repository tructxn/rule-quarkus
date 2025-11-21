# Quarkus Bazel Rules

Bazel rules for building Quarkus applications with build-time augmentation.

## Status

**Phase 1: Complete ✅**
- Clean, modular architecture (11 focused components)
- Successful compilation and build
- Jandex indexing working
- Route discovery working
- Bytecode generation working

**Phase 2: Planned**
- CDI/ArC processor integration
- Full Quarkus runtime support

See [PHASE1_COMPLETE.md](PHASE1_COMPLETE.md) for detailed status.

## Quick Start

```python
load("//bazel/rules:quarkus.bzl", "quarkus_application")

quarkus_application(
    name = "my-app",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    deps = [...],
    extensions = [
        "@maven//:io_quarkus_quarkus_arc",
        "@maven//:io_quarkus_arc_arc",
    ],
    deployment_extensions = [
        "@maven//:io_quarkus_arc_arc_processor",
    ],
    main_class = "com.example.MyApp",
)
```

## Build & Run

```bash
# Build
bazel build //examples/hello-world:hello-world

# Run (compiles but CDI not yet working - Phase 2)
bazel run //examples/hello-world:hello-world
```

## Architecture

Three-layer build process (matching Quarkus Maven):

1. **Layer 1: Compilation** - `java_library` compiles sources
2. **Layer 2: Augmentation** - `quarkus_augment` processes bytecode  
3. **Layer 3: Runtime** - `java_binary` packages final application

### Augmentation Tool Structure

```
bazel/tools/
├── BazelQuarkusAugmentor.java  # Main (50 lines)
├── augmentor/                  # Context & argument parsing
├── indexer/                    # Jandex indexing
├── discovery/                  # Annotation discovery
├── generator/                  # Bytecode generation
└── packager/                   # JAR packaging
```

## Documentation

- **[PHASE1_COMPLETE.md](PHASE1_COMPLETE.md)** - Current status and achievements
- **[QUARKUS_MAVEN_BUILD.md](QUARKUS_MAVEN_BUILD.md)** - How Quarkus Maven works
- **[REFACTORING_PLAN.md](REFACTORING_PLAN.md)** - Refactoring plan and roadmap
- **[CLEANUP_SUMMARY.md](CLEANUP_SUMMARY.md)** - What was cleaned up

## Requirements

- Bazel 7.x or higher
- Java 21
- Quarkus 3.17.4

## License

Apache 2.0
