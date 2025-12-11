# Plan: QuarkusBootstrap Integration với Bazel

## Vấn đề hiện tại

Khi chạy `CuratedApplication.createAugmentor()`:

```
NoSuchMethodException: io.quarkus.runner.bootstrap.AugmentActionImpl.<init>(CuratedApplication)
```

**Nguyên nhân**: `AugmentActionImpl` nằm trong `quarkus-core-deployment` JAR, nhưng Quarkus load class này thông qua **isolated Augment ClassLoader**, không phải từ tool's classpath.

## Kiến trúc ClassLoader của Quarkus

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

## Vấn đề cốt lõi

1. **Quarkus tự build classpath**: `CuratedApplication` tự resolve deployment JARs từ `ApplicationModel`
2. **ApplicationModel không đúng**: Của ta đang set tất cả vào `DEPLOYMENT_CP` thay vì đúng flags
3. **Deployment JARs không có trong model**: Extensions không được link đến deployment modules

## Plan giải quyết

### Phase 1: Fix ApplicationModel (Ưu tiên cao)

**Mục tiêu**: Build đúng `ApplicationModel` để Quarkus có thể tự load deployment JARs

#### Task 1.1: Sửa flag logic trong ApplicationModelFactory

```java
// Hiện tại: Tất cả runtime JARs có flag = RUNTIME_CP
// Cần: Extensions có thêm RUNTIME_EXTENSION_ARTIFACT

// Hiện tại: Deployment JARs có flag = DEPLOYMENT_CP
// Cần: Deployment JARs cũng cần DEPLOYMENT_CP flag ĐÚNG
```

**File**: `ApplicationModelFactory.java`

**Thay đổi**:
1. Runtime dependencies: `RUNTIME_CP | COMPILE_ONLY` (cho non-extensions)
2. Runtime extensions: `RUNTIME_CP | RUNTIME_EXTENSION_ARTIFACT`
3. Deployment dependencies: `DEPLOYMENT_CP`

#### Task 1.2: Link Runtime Extension → Deployment Module

Quarkus cần biết `quarkus-arc` → `quarkus-arc-deployment`:

```java
// Trong ResolvedDependencyBuilder
.setDeploymentModuleKey(ArtifactKey.of(groupId, artifactId + "-deployment", classifier, type))
```

**File**: `ApplicationModelFactory.java`

#### Task 1.3: Set Platform BOM imports

```java
// ApplicationModelBuilder cần platform imports
builder.addPlatformImport(platformKey);
```

### Phase 2: Verify CuratedApplication (Ưu tiên cao)

**Mục tiêu**: Đảm bảo `CuratedApplication` được tạo đúng

#### Task 2.1: Debug ApplicationModel output

Thêm logging để verify:
```java
System.out.println("Runtime deps with RUNTIME_CP: " +
    model.getDependencies().stream()
        .filter(d -> d.isRuntimeCp())
        .count());

System.out.println("Extensions: " +
    model.getDependencies().stream()
        .filter(d -> d.isRuntimeExtensionArtifact())
        .map(d -> d.getArtifactId())
        .collect(Collectors.toList()));
```

#### Task 2.2: Verify AugmentClassLoader có deployment JARs

```java
// Sau khi bootstrap()
ClassLoader augmentCL = curatedApp.getOrCreateAugmentClassLoader();
// Try load AugmentActionImpl
augmentCL.loadClass("io.quarkus.runner.bootstrap.AugmentActionImpl");
```

### Phase 3: Alternative Approach - Manual ClassLoader (Backup plan)

Nếu Phase 1-2 không thành công, tạo ClassLoader thủ công:

#### Task 3.1: Tạo custom AugmentClassLoader

```java
public class BazelAugmentClassLoader extends URLClassLoader {
    public BazelAugmentClassLoader(List<Path> deploymentJars, ClassLoader parent) {
        super(toURLs(deploymentJars), parent);
    }

    private static URL[] toURLs(List<Path> jars) {
        return jars.stream()
            .map(p -> p.toUri().toURL())
            .toArray(URL[]::new);
    }
}
```

#### Task 3.2: Load AugmentActionImpl manually

```java
ClassLoader augmentCL = new BazelAugmentClassLoader(deploymentJars, getClass().getClassLoader());
Class<?> augmentorClass = augmentCL.loadClass("io.quarkus.runner.bootstrap.AugmentActionImpl");
Constructor<?> ctor = augmentorClass.getConstructor(CuratedApplication.class);
AugmentAction augmentor = (AugmentAction) ctor.newInstance(curatedApp);
```

### Phase 4: Simplify - Direct Augmentation (Alternative)

Thay vì dùng `QuarkusBootstrap`, gọi trực tiếp build steps:

#### Task 4.1: Load BuildChain manually

```java
// Load all @BuildStep processors from deployment JARs
ServiceLoader<BuildStep> buildSteps = ServiceLoader.load(BuildStep.class, augmentCL);

// Create BuildChain
BuildChainBuilder chainBuilder = BuildChain.builder();
for (BuildStep step : buildSteps) {
    chainBuilder.addBuildStep(step);
}
BuildChain chain = chainBuilder.build();

// Execute
BuildResult result = chain.execute();
```

**Ưu điểm**: Không phụ thuộc vào QuarkusBootstrap internal
**Nhược điểm**: Phức tạp hơn, cần hiểu BuildChain API

---

## Implementation Order

```
Week 1: Phase 1 (Fix ApplicationModel)
├── Task 1.1: Fix flag logic
├── Task 1.2: Link runtime → deployment
└── Task 1.3: Platform imports

Week 2: Phase 2 (Verify & Debug)
├── Task 2.1: Debug output
├── Task 2.2: Verify classloader
└── Test với hello-world example

Week 3: Phase 3 hoặc 4 (nếu cần)
├── Nếu Phase 1-2 OK → Done
├── Nếu không → Phase 3 (manual classloader)
└── Hoặc Phase 4 (direct BuildChain)
```

---

## Immediate Next Steps

1. **Đọc source code `CuratedApplication.java`** để hiểu chính xác cách `getOrCreateAugmentClassLoader()` hoạt động

2. **Sửa `ApplicationModelFactory.java`**:
   - Đảm bảo runtime deps có `RUNTIME_CP` flag
   - Đảm bảo extensions có `RUNTIME_EXTENSION_ARTIFACT` flag
   - Không đưa deployment jars vào model (để Quarkus tự resolve)

3. **Test từng bước**:
   ```bash
   # Build với verbose logging
   bazel build //v2-bootstrap/examples/hello-world:hello-world 2>&1 | tee build.log
   ```

---

## References

- [Quarkus Class Loading Reference](https://quarkus.io/guides/class-loading-reference)
- [CuratedApplication.java](https://github.com/quarkusio/quarkus/blob/main/independent-projects/bootstrap/core/src/main/java/io/quarkus/bootstrap/app/CuratedApplication.java)
- [AugmentActionImpl.java](https://github.com/quarkusio/quarkus/blob/main/core/deployment/src/main/java/io/quarkus/runner/bootstrap/AugmentActionImpl.java)
- [QuarkusClassLoader.java](https://github.com/quarkusio/quarkus/blob/main/independent-projects/bootstrap/core/src/main/java/io/quarkus/bootstrap/classloading/QuarkusClassLoader.java)
