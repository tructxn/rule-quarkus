# Plan: QuarkusBootstrap Integration với Bazel

## Status: ✅ COMPLETED

**Ngày hoàn thành**: 2025-12-11

v2-bootstrap đã hoạt động hoàn chỉnh với:
- Full CDI/ArC support
- REST endpoints (quarkus-rest)
- HTTP server (Vert.x + Netty)
- Dependency injection working
- Same output as Maven build

---

## Kiến trúc tổng quan

### Three-Layer Build Process

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
│  Layer 3: Shell script                                          │
│  ────────────────────                                           │
│  Run with explicit classpath                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Kiến trúc ClassLoader của Quarkus

```
┌─────────────────────────────────────────────────────────────────┐
│                    System ClassLoader                           │
│                    (Bazel tool classpath)                       │
└───────────────────────────┬─────────────────────────────────────┘
                            │ parent
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Base ClassLoader                             │
│                    (quarkus-bootstrap-core)                     │
└───────────────────────────┬─────────────────────────────────────┘
                            │ parent
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Augment ClassLoader                             │
│           (deployment JARs: *-deployment.jar)                   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  AugmentActionImpl loaded here via reflection           │   │
│  │  from quarkus-core-deployment.jar                       │   │
│  └─────────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │ parent
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                Deployment ClassLoader                           │
│              (application + runtime JARs)                       │
│                                                                 │
│  Build steps execute here with TCCL = DeploymentClassLoader    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Các vấn đề đã giải quyết

### Issue 1: ApplicationModel dependencies showing 0

**Triệu chứng**:
```
Runtime classpath deps: 0
Extensions: 0
```

**Nguyên nhân**: Khi thêm deployment deps sau runtime deps, deployment deps overwrite flags của runtime deps thay vì merge.

**Giải pháp** (`ApplicationModelFactory.java`):
```java
// Build map of deployment JARs for quick lookup
Map<String, Path> deploymentJarMap = new HashMap<>();
for (Path jar : deploymentJars) {
    String key = extractArtifactKey(jar);
    deploymentJarMap.put(key, jar);
}

// Add runtime deps with proper flag merging
for (Path jar : runtimeJars) {
    String key = extractArtifactKey(jar);
    ArtifactCoords coords = parseCoords(jar);

    // Start with RUNTIME_CP flag
    int flags = DependencyFlags.RUNTIME_CP;

    // If this JAR is also in deployment, add DEPLOYMENT_CP flag too (merge, not replace)
    if (deploymentJarMap.containsKey(key)) {
        flags |= DependencyFlags.DEPLOYMENT_CP;
    }

    // Mark Quarkus extensions
    if (extensionArtifactIds.contains(coords.artifactId)) {
        flags |= DependencyFlags.RUNTIME_EXTENSION_ARTIFACT;
    }

    builder.addDependency(createDependency(coords, jar, flags));
}
```

**Kết quả**:
```
Runtime classpath deps: 43
Extensions: 2 (quarkus-arc, quarkus-vertx-http)
```

---

### Issue 2: ClassCastException với CuratedApplication

**Triệu chứng**:
```
ClassCastException: Cannot cast io.quarkus.bootstrap.app.CuratedApplication
                    to io.quarkus.bootstrap.app.CuratedApplication
```

**Nguyên nhân**: Deployment và runtime classloaders có conflict khi isolated.

**Giải pháp** (`BootstrapAugmentor.java`):
```java
QuarkusBootstrap bootstrap = QuarkusBootstrap.builder()
    .setApplicationRoot(appJars)
    .setExistingModel(model)
    .setTargetDirectory(outputDir)
    .setMode(QuarkusBootstrap.Mode.PROD)
    .setIsolateDeployment(false)    // KEY FIX: Don't isolate deployment classes
    .setFlatClassPath(true)         // KEY FIX: Use flat classpath
    .build();
```

---

### Issue 3: ClassNotFoundException cho ASM classes

**Triệu chứng**:
```
ClassNotFoundException: io.quarkus.arc.impl.ParameterizedTypeImpl
```

**Nguyên nhân**: TCCL (Thread Context ClassLoader) không được set đúng cho ASM class loading.

**Giải pháp** (`BootstrapAugmentor.java`):
```java
try (CuratedApplication curatedApp = bootstrap.bootstrap()) {
    ClassLoader augmentCl = curatedApp.getOrCreateAugmentClassLoader();

    // KEY FIX: Set TCCL to augment classloader for ASM class loading
    ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(augmentCl);

    try {
        AugmentAction action = curatedApp.createAugmentor();
        AugmentResult result = action.createProductionApplication();
    } finally {
        Thread.currentThread().setContextClassLoader(originalCl);
    }
}
```

**Thêm deps vào tool** (`v2-bootstrap/tools/BUILD.bazel`):
```python
deps = [
    "@maven//:io_quarkus_arc_arc",
    "@maven//:io_quarkus_quarkus_core_deployment",
    "@maven//:io_quarkus_quarkus_arc_deployment",
    # ... existing deps
]
```

---

### Issue 4: ConfigValidationException

**Triệu chứng**:
```
ConfigValidationException: platform.quarkus.native.builder-image
```

**Giải pháp** (`application.properties`):
```properties
quarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21
```

---

### Issue 5: lib/boot/ empty

**Triệu chứng**: Quarkus app không chạy được vì thiếu bootstrap runner.

**Nguyên nhân**: Bootstrap runner JAR không được copy với đúng naming format.

**Giải pháp** (`OutputHandler.java`):
```java
private static void ensureBootstrapRunner(AugmentationConfig config, Path targetDir) throws IOException {
    Path bootDir = targetDir.resolve("lib/boot");
    Files.createDirectories(bootDir);

    // Find quarkus-bootstrap-runner JAR from deployment classpath
    for (Path jar : config.getDeploymentJars()) {
        String name = jar.getFileName().toString();
        if (name.contains("quarkus-bootstrap-runner")) {
            // Extract version from JAR name
            String version = extractVersion(name);

            // Quarkus expects: io.quarkus.quarkus-bootstrap-runner-VERSION.jar
            String targetFileName = "io.quarkus.quarkus-bootstrap-runner-" + version + ".jar";
            Path targetJar = bootDir.resolve(targetFileName);

            Files.copy(jar, targetJar, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Copied bootstrap runner: " + targetFileName);
            return;
        }
    }
}
```

---

### Issue 6: quarkus-run.jar empty Class-Path

**Triệu chứng**: Running quarkus-run.jar trực tiếp không work.

**Giải pháp** (`quarkus.bzl` - runner script):
```bash
#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
QUARKUS_APP="$SCRIPT_DIR/{name}_augmented-quarkus-app"

# Run with explicit classpath including lib/boot and lib/main
exec java {jvm_flags} \
    -cp "$QUARKUS_APP/lib/boot/*:$QUARKUS_APP/lib/main/*:$QUARKUS_APP/quarkus-run.jar" \
    io.quarkus.bootstrap.runner.QuarkusEntryPoint "$@"
```

---

### Issue 7: Circular dependency với org.eclipse.sisu

**Triệu chứng**:
```
Error: circular dependency: org.eclipse.sisu:org.eclipse.sisu.plexus
```

**Giải pháp** (`MODULE.bazel`):
```python
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
    artifact = "quarkus-vertx-http-deployment",
    exclusions = [
        "org.eclipse.sisu:org.eclipse.sisu.plexus",
        "org.apache.maven:maven-plugin-api",
        "org.apache.maven:maven-xml-impl",
        "org.codehaus.plexus:plexus-xml",
    ],
    group = "io.quarkus",
    version = QUARKUS_VERSION,
)
```

---

## Output Structure

```
quarkus-app/
├── app/
│   └── hello-world.jar           # Application classes
├── lib/
│   ├── boot/
│   │   └── io.quarkus.quarkus-bootstrap-runner-3.17.4.jar
│   └── main/
│       ├── io.quarkus.quarkus-core-3.17.4.jar
│       ├── io.quarkus.quarkus-arc-3.17.4.jar
│       ├── io.quarkus.quarkus-rest-3.17.4.jar
│       ├── io.quarkus.quarkus-vertx-http-3.17.4.jar
│       └── ... (runtime dependencies)
├── quarkus/
│   ├── generated-bytecode.jar    # CDI proxies, REST handlers
│   └── quarkus-application.dat   # Runtime config
└── quarkus-run.jar               # Launcher
```

---

## Key Source Files

| File | Purpose | Key Changes |
|------|---------|-------------|
| `tools/.../BootstrapAugmentor.java` | Main entry point | setIsolateDeployment(false), setFlatClassPath(true), TCCL setting |
| `tools/.../ApplicationModelFactory.java` | Build ApplicationModel | Flag merging (RUNTIME_CP \| DEPLOYMENT_CP), extension detection |
| `tools/.../ExtensionDetector.java` | Detect Quarkus extensions | META-INF/quarkus-extension.properties |
| `tools/.../OutputHandler.java` | Handle output | Bootstrap runner copy with correct naming |
| `tools/.../ConfigParser.java` | Parse CLI args | --app-jars, --runtime-jars, --deployment-jars, --output-dir |
| `rules/quarkus.bzl` | Main macro | quarkus_application(), runner script generation |
| `rules/quarkus_bootstrap.bzl` | Bootstrap rule | _quarkus_bootstrap_impl |

---

## Extension Detection

Quarkus extensions are identified by `META-INF/quarkus-extension.properties`:

```java
public static boolean isQuarkusExtension(Path jarPath) {
    try (JarFile jar = new JarFile(jarPath.toFile())) {
        return jar.getEntry("META-INF/quarkus-extension.properties") != null;
    }
}
```

Extensions found:
- `quarkus-arc` → CDI implementation
- `quarkus-rest` → REST endpoints
- `quarkus-vertx-http` → HTTP server

---

## DependencyFlags

Quan trọng nhất cho ApplicationModel:

| Flag | Value | Purpose |
|------|-------|---------|
| `RUNTIME_CP` | 1 | JAR is on runtime classpath |
| `DEPLOYMENT_CP` | 2 | JAR is on deployment classpath |
| `RUNTIME_EXTENSION_ARTIFACT` | 4096 | JAR is a Quarkus extension |

**Key insight**: Flags phải được OR'd (merged), không phải replaced.

---

## Commands Reference

```bash
# Build
bazel build //v2-bootstrap/examples/hello-world:hello-world

# Run
bazel-bin/v2-bootstrap/examples/hello-world/hello-world

# Test HTTP
curl http://localhost:8080/hello
# Output: Hello from Quarkus (built with Bazel)!

curl http://localhost:8080/hello/World
# Output: Hello, World!

# Debug build
bazel build //v2-bootstrap/examples/hello-world:hello-world 2>&1 | tee build.log

# Clean rebuild
bazel clean --expunge && bazel build //v2-bootstrap/examples/hello-world:hello-world

# Query deps
bazel query "deps(//v2-bootstrap/examples/hello-world:hello-world)"
```

---

## Future Improvements

1. **Native image support** - GraalVM native compilation
2. **Dev mode** - Hot reload during development
3. **More extensions** - Hibernate, database, etc.
4. **Testing support** - @QuarkusTest integration
5. **Multi-module** - Shared libraries between apps

---

## References

- [Quarkus Class Loading Reference](https://quarkus.io/guides/class-loading-reference)
- [CuratedApplication.java](https://github.com/quarkusio/quarkus/blob/main/independent-projects/bootstrap/core/src/main/java/io/quarkus/bootstrap/app/CuratedApplication.java)
- [AugmentActionImpl.java](https://github.com/quarkusio/quarkus/blob/main/core/deployment/src/main/java/io/quarkus/runner/bootstrap/AugmentActionImpl.java)
- [DependencyFlags.java](https://github.com/quarkusio/quarkus/blob/main/independent-projects/bootstrap/app-model/src/main/java/io/quarkus/bootstrap/model/DependencyFlags.java)
- [QuarkusBootstrap.java](https://github.com/quarkusio/quarkus/blob/main/independent-projects/bootstrap/core/src/main/java/io/quarkus/bootstrap/app/QuarkusBootstrap.java)
