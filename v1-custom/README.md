# Approach 1: Custom Augmentation

This directory contains the custom augmentation implementation that manually processes annotations and generates bytecode.

## Status

**Phase 1: Complete ✅**
- Jandex indexing working
- Route discovery working
- Bytecode generation with Gizmo working
- Clean modular architecture

**Limitation**: CDI not working - ArC processor not integrated.

## Architecture

```
v1-custom/
├── rules/                    # Bazel rules
│   ├── quarkus.bzl          # Main macro
│   ├── quarkus_augment.bzl  # Augmentation rule
│   └── jandex.bzl           # Jandex indexing
│
├── tools/                    # Java augmentation tools
│   ├── BazelQuarkusAugmentor.java   # Main (~50 lines)
│   ├── augmentor/                   # Config & parsing
│   ├── discovery/                   # Annotation discovery
│   ├── generator/                   # Bytecode generation
│   ├── indexer/                     # Jandex indexing
│   └── packager/                    # JAR packaging
│
└── examples/
    └── hello-world/
```

## Usage

```python
load("//v1-custom/rules:quarkus.bzl", "quarkus_application")

quarkus_application(
    name = "my-app",
    srcs = glob(["src/main/java/**/*.java"]),
    extensions = ["@maven//:io_quarkus_quarkus_arc"],
    deployment_extensions = ["@maven//:io_quarkus_arc_arc_processor"],
)
```

## Build

```bash
bazel build //v1-custom/examples/hello-world:hello-world
```

## Why Keep This Approach?

1. **Educational**: Shows how Quarkus augmentation works internally
2. **Lightweight**: No heavy Quarkus bootstrap dependencies
3. **Customizable**: Can add custom processing steps
4. **Debugging**: Easier to debug than QuarkusBootstrap black box

## Comparison with v2-bootstrap

| Feature | v1-custom | v2-bootstrap |
|---------|-----------|--------------|
| CDI Support | ❌ Manual (broken) | ✅ Full |
| Complexity | Lower | Higher |
| Dependencies | Fewer | More |
| Customization | Easy | Hard |
| Maintenance | High | Low |
