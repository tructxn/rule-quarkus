# Plan: QuarkusBootstrap Integration với Bazel

## Status: ✅ COMPLETED + EXTENSIONS ADDED

**Ngày hoàn thành cơ bản**: 2025-12-11
**Ngày thêm extensions**: 2025-12-12

### Phiên bản hiện tại
- **Quarkus**: 3.20.1
- **Bazel**: 7.x+
- **Java**: 21

### Tính năng đã hoàn thành
- Full CDI/ArC support
- REST endpoints (quarkus-rest + jackson)
- HTTP server (Vert.x + Netty)
- Dependency injection working
- Same output as Maven build
- **5 Tiers of Extensions** (see below)

---

## Extensions đã hỗ trợ (5 Tiers)

### Tier 1: Core (Must have)
| Extension | Artifact | Status |
|-----------|----------|--------|
| ArC (CDI) | `quarkus-arc` | ✅ |
| Vert.x HTTP | `quarkus-vertx-http` | ✅ |
| Mutiny | `quarkus-mutiny` | ✅ |
| REST Jackson | `quarkus-rest-jackson` | ✅ |

### Tier 2: Database (Reactive)
| Extension | Artifact | Status |
|-----------|----------|--------|
| Oracle Reactive | `quarkus-reactive-oracle-client` | ✅ |
| MySQL Reactive | `quarkus-reactive-mysql-client` | ✅ |
| Redis | `quarkus-redis-client` | ✅ |

### Tier 3: Messaging
| Extension | Artifact | Status |
|-----------|----------|--------|
| Kafka | `quarkus-messaging-kafka` | ✅ |
| RabbitMQ | `quarkus-messaging-rabbitmq` | ✅ |
| gRPC | `quarkus-grpc` | ✅ |

### Tier 4: Observability
| Extension | Artifact | Status |
|-----------|----------|--------|
| Prometheus Metrics | `quarkus-micrometer-registry-prometheus` | ✅ |
| SmallRye Health | `quarkus-smallrye-health` | ✅ |

### Tier 5: Quarkiverse
| Extension | Group | Version | Status |
|-----------|-------|---------|--------|
| Unleash | `io.quarkiverse.unleash` | 1.10.0 | ✅ |
| LangChain4j Core | `io.quarkiverse.langchain4j` | 0.26.1 | ✅ |
| LangChain4j OpenAI | `io.quarkiverse.langchain4j` | 0.26.1 | ✅ |
| LangChain4j Ollama | `io.quarkiverse.langchain4j` | 0.26.1 | ✅ |
| Tika | `io.quarkiverse.tika` | 2.1.0 | ✅ |

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

## Examples

### hello-world
Basic REST application với CDI.

```bash
bazel build //v2-bootstrap/examples/hello-world:hello-world
bazel-bin/v2-bootstrap/examples/hello-world/hello-world

curl http://localhost:8080/hello
# Hello from Quarkus (built with Bazel)!
```

### demo-extensions
Full demo của tất cả tiers (Tier 1 + 4 enabled by default).

```bash
bazel build //v2-bootstrap/examples/demo-extensions:demo-extensions
bazel-bin/v2-bootstrap/examples/demo-extensions/demo-extensions

# Test endpoints
curl http://localhost:8080/              # All endpoints
curl http://localhost:8080/api/users     # Tier 1: REST + Jackson
curl http://localhost:8080/q/health      # Tier 4: Health checks
curl http://localhost:8080/q/metrics     # Tier 4: Prometheus metrics
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

---

### Issue 2: ClassCastException với CuratedApplication

**Triệu chứng**:
```
ClassCastException: Cannot cast io.quarkus.bootstrap.app.CuratedApplication
                    to io.quarkus.bootstrap.app.CuratedApplication
```

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

---

### Issue 4-7: Các vấn đề khác

| Issue | Giải pháp |
|-------|-----------|
| ConfigValidationException | Add `quarkus.native.builder-image` to application.properties |
| lib/boot/ empty | Copy bootstrap-runner với đúng naming format |
| quarkus-run.jar empty Class-Path | Sử dụng runner script với explicit classpath |
| Circular dependency với org.eclipse.sisu | Thêm exclusions vào maven.artifact() |

---

## Output Structure

```
quarkus-app/
├── app/
│   └── {app-name}.jar           # Application classes
├── lib/
│   ├── boot/
│   │   └── io.quarkus.quarkus-bootstrap-runner-3.20.1.jar
│   └── main/
│       ├── io.quarkus.quarkus-core-3.20.1.jar
│       ├── io.quarkus.quarkus-arc-3.20.1.jar
│       ├── io.quarkus.quarkus-rest-3.20.1.jar
│       └── ... (runtime dependencies)
├── quarkus/
│   ├── generated-bytecode.jar    # CDI proxies, REST handlers
│   └── quarkus-application.dat   # Runtime config
└── quarkus-run.jar               # Launcher
```

---

## Key Source Files

| File | Purpose |
|------|---------|
| `tools/.../BootstrapAugmentor.java` | Main entry point, QuarkusBootstrap configuration |
| `tools/.../ApplicationModelFactory.java` | Build ApplicationModel with flag merging |
| `tools/.../ExtensionDetector.java` | Detect Quarkus extensions via META-INF |
| `tools/.../OutputHandler.java` | Handle output, copy bootstrap runner |
| `rules/quarkus.bzl` | Main `quarkus_application` macro |
| `rules/quarkus_bootstrap.bzl` | Bootstrap augmentation rule |

---

## DependencyFlags

| Flag | Value | Purpose |
|------|-------|---------|
| `RUNTIME_CP` | 1 | JAR is on runtime classpath |
| `DEPLOYMENT_CP` | 2 | JAR is on deployment classpath |
| `RUNTIME_EXTENSION_ARTIFACT` | 4096 | JAR is a Quarkus extension |

**Key insight**: Flags phải được OR'd (merged), không phải replaced.

---

## Commands Reference

```bash
# Build examples
bazel build //v2-bootstrap/examples/hello-world:hello-world
bazel build //v2-bootstrap/examples/demo-extensions:demo-extensions

# Run
bazel-bin/v2-bootstrap/examples/hello-world/hello-world
bazel-bin/v2-bootstrap/examples/demo-extensions/demo-extensions

# Test HTTP
curl http://localhost:8080/hello
curl http://localhost:8080/api/users
curl http://localhost:8080/q/health
curl http://localhost:8080/q/metrics

# Clean rebuild
bazel clean --expunge && bazel build //...

# Query deps
bazel query "deps(//v2-bootstrap/examples/demo-extensions:demo-extensions)"
```

---

## Future Improvements

1. **Native image support** - GraalVM native compilation
2. **Dev mode** - Hot reload during development
3. **Proto/gRPC rules** - Compile .proto files
4. **Testing support** - @QuarkusTest integration
5. **Multi-module** - Shared libraries between apps

---

## References

- [Quarkus Class Loading Reference](https://quarkus.io/guides/class-loading-reference)
- [CuratedApplication.java](https://github.com/quarkusio/quarkus/blob/main/independent-projects/bootstrap/core/src/main/java/io/quarkus/bootstrap/app/CuratedApplication.java)
- [AugmentActionImpl.java](https://github.com/quarkusio/quarkus/blob/main/core/deployment/src/main/java/io/quarkus/runner/bootstrap/AugmentActionImpl.java)
- [DependencyFlags.java](https://github.com/quarkusio/quarkus/blob/main/independent-projects/bootstrap/app-model/src/main/java/io/quarkus/bootstrap/model/DependencyFlags.java)
- [QuarkusBootstrap.java](https://github.com/quarkusio/quarkus/blob/main/independent-projects/bootstrap/core/src/main/java/io/quarkus/bootstrap/app/QuarkusBootstrap.java)
