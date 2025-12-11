# Approach 2: QuarkusBootstrap API Integration

This directory contains the implementation of Quarkus Bazel rules using the official **QuarkusBootstrap API** instead of custom augmentation logic.

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
├── BootstrapAugmentor.java      # Main entry point (~50 lines)
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

## How It Works

### Step 1: Build ApplicationModel

```java
// Detect which JARs are Quarkus extensions
List<ExtensionInfo> extensions = ExtensionDetector.detect(runtimeJars);

// Build ApplicationModel with proper flags
ApplicationModel model = ApplicationModelFactory.builder()
    .setAppArtifact(applicationJars)
    .addRuntimeDependencies(runtimeJars, DependencyFlags.RUNTIME_CP)
    .addDeploymentDependencies(deploymentJars, DependencyFlags.DEPLOYMENT_CP)
    .markExtensions(extensions)
    .build();
```

### Step 2: Run QuarkusBootstrap

```java
QuarkusBootstrap bootstrap = QuarkusBootstrap.builder()
    .setApplicationRoot(appJars)
    .setExistingModel(model)        // Use our pre-built model
    .setTargetDirectory(outputDir)
    .setMode(QuarkusBootstrap.Mode.PROD)
    .setIsolateDeployment(true)     // Separate deployment classloader
    .build();

try (CuratedApplication app = bootstrap.bootstrap()) {
    AugmentAction action = app.createAugmentor();
    AugmentResult result = action.createProductionApplication();
    // Output is in outputDir/quarkus-app/
}
```

### Step 3: Bazel Packages Output

```python
# quarkus_bootstrap rule outputs directory structure:
# outputDir/
# ├── quarkus-app/
# │   ├── app/
# │   │   └── application.jar      # Your augmented code
# │   ├── lib/
# │   │   └── *.jar                 # Runtime dependencies
# │   ├── quarkus/
# │   │   └── generated-bytecode.jar
# │   └── quarkus-run.jar          # Main entry point
```

## Extension Detection

Quarkus extensions are identified by `META-INF/quarkus-extension.properties`:

```java
public static boolean isQuarkusExtension(Path jarPath) {
    try (JarFile jar = new JarFile(jarPath.toFile())) {
        return jar.getEntry("META-INF/quarkus-extension.properties") != null;
    }
}
```

## Runtime ↔ Deployment Mapping

Convention: `quarkus-{name}` → `quarkus-{name}-deployment`

```java
public static Path findDeploymentArtifact(Path runtimeJar, List<Path> deploymentJars) {
    String runtimeName = extractArtifactId(runtimeJar);  // e.g., "quarkus-arc"
    String deploymentName = runtimeName + "-deployment"; // e.g., "quarkus-arc-deployment"

    return deploymentJars.stream()
        .filter(jar -> extractArtifactId(jar).equals(deploymentName))
        .findFirst()
        .orElse(null);
}
```

## Usage

### BUILD.bazel

```python
load("//v2-bootstrap/rules:quarkus.bzl", "quarkus_application")

quarkus_application(
    name = "my-app",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "@maven//:jakarta_enterprise_jakarta_enterprise_cdi_api",
        "@maven//:jakarta_inject_jakarta_inject_api",
    ],
    extensions = [
        "@maven//:io_quarkus_quarkus_arc",
        "@maven//:io_quarkus_quarkus_resteasy_reactive",
    ],
    main_class = "io.quarkus.runner.GeneratedMain",  # Auto-generated
)
```

### Build & Run

```bash
# Build
bazel build //examples/hello-world:hello-world

# Run
bazel run //examples/hello-world:hello-world
```

## Required Dependencies

```python
# MODULE.bazel
QUARKUS_VERSION = "3.17.4"

maven.install(
    artifacts = [
        # Bootstrap (required for augmentation tool)
        "io.quarkus:quarkus-bootstrap-core:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-bootstrap-app-model:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-core:%s" % QUARKUS_VERSION,

        # Deployment modules (for augmentation)
        "io.quarkus:quarkus-core-deployment:%s" % QUARKUS_VERSION,
        "io.quarkus:quarkus-arc-deployment:%s" % QUARKUS_VERSION,

        # Runtime modules (for application)
        "io.quarkus:quarkus-arc:%s" % QUARKUS_VERSION,
    ],
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

- [ ] Core tools implementation
- [ ] ApplicationModel building
- [ ] Extension detection
- [ ] Bazel rules
- [ ] Example application
- [ ] CDI working
- [ ] REST endpoints working
