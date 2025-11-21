# How Quarkus Maven Build Works

## Overview

Quarkus uses a **build-time augmentation** model where most framework work happens during compilation, not at runtime. This is why Quarkus has fast startup times.

## Maven Build Phases

### 1. **Compilation Phase** (`maven-compiler-plugin`)
```
src/main/java/*.java → target/classes/*.class
```

**What happens:**
- Standard Java compilation
- Annotation processors run (but NOT ArC processor yet)
- Produces raw bytecode without CDI/framework enhancements

**Output:** `target/classes/` with compiled `.class` files

---

### 2. **Augmentation Phase** (`quarkus-maven-plugin:build`)

This is the **core of Quarkus** - happens between compilation and packaging.

```xml
<plugin>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>build</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**What happens:**

#### Step 2.1: Build ApplicationModel
- Scans Maven dependencies
- Identifies Quarkus extensions (runtime + deployment modules)
- Creates dependency graph
- **Key class:** `io.quarkus.bootstrap.model.ApplicationModel`

#### Step 2.2: Create Jandex Index
- Indexes all `.class` files from `target/classes/`
- Indexes all dependency JARs
- Creates annotation metadata for fast lookup
- **Key class:** `org.jboss.jandex.Indexer`
- **Output:** In-memory `Index` object (or `jandex.idx` file)

#### Step 2.3: Setup Classloaders
- **Runtime ClassLoader:** Application classes + runtime dependencies
- **Deployment ClassLoader:** Deployment modules (not in final app)
- Isolated to prevent deployment code from leaking into runtime

#### Step 2.4: Run QuarkusAugmentor
**Key class:** `io.quarkus.deployment.QuarkusAugmentor`

```java
QuarkusAugmentor augmentor = new QuarkusAugmentor.Builder()
    .setRoot(applicationRoot)           // target/classes
    .setClassLoader(runtimeClassLoader)
    .setTargetDir(targetDir)            // target/quarkus-app
    .setDeploymentClassLoader(deploymentClassLoader)
    .build();

CuratedApplication curatedApp = augmentor.run();
```

**What QuarkusAugmentor does:**

##### 2.4.1: Discover Build Steps
- Scans deployment modules for `@BuildStep` methods
- Each extension contributes build steps
- Examples:
  - `ArcProcessor` → CDI bean discovery & generation
  - `VertxHttpProcessor` → HTTP server setup
  - `ResteasyProcessor` → JAX-RS endpoint registration

##### 2.4.2: Execute Build Steps (Deployment Pipeline)
Build steps run in dependency order:

**ArC (CDI) Processing:**
```java
@BuildStep
BeanContainerBuildItem build(
    CombinedIndexBuildItem combinedIndex,
    BuildProducer<GeneratedClassBuildItem> generatedClass) {
    
    // 1. Discover beans from index
    BeanProcessor.Builder builder = BeanProcessor.builder()
        .setImmutableBeanArchiveIndex(combinedIndex.getIndex());
    
    // 2. Process beans
    BeanProcessor processor = builder.build();
    processor.registerBeans();
    processor.initialize();
    
    // 3. Generate CDI implementation classes
    processor.generateResources(...);
    
    // Generated classes like:
    // - MyBean_ClientProxy.class
    // - MyBean_Bean.class
    // - Arc_Container.class
}
```

**HTTP Server Processing:**
```java
@BuildStep
void setupVertxRouter(
    BeanContainerBuildItem beanContainer,
    BuildProducer<RouteBuildItem> routes) {
    
    // Discover @Route, @Path annotations
    // Generate router configuration
    // Wire CDI beans to HTTP handlers
}
```

##### 2.4.3: Generate Bytecode
- Uses **Gizmo** (bytecode generation library)
- Generates optimized classes:
  - `io.quarkus.runner.ApplicationImpl` - Main application class
  - `io.quarkus.runner.GeneratedMain` - Entry point
  - CDI proxy classes
  - HTTP route handlers

##### 2.4.4: Record Build-Time Values
- Configuration values resolved at build time
- Recorded in `quarkus-application.dat`
- Replayed at runtime (no re-computation)

**Output:** `target/quarkus-app/` directory structure:
```
target/quarkus-app/
├── app/
│   └── application.jar           # Your augmented application
├── lib/
│   └── *.jar                     # Runtime dependencies
├── quarkus/
│   └── generated-bytecode.jar    # Generated classes
└── quarkus-run.jar               # Main entry point
```

---

### 3. **Packaging Phase** (`maven-jar-plugin` or `quarkus-maven-plugin:package`)

**Fast-JAR (default):**
```
target/quarkus-app/
└── quarkus-run.jar (thin launcher)
```
- Keeps JARs separate for faster startup
- ClassLoader optimization

**Uber-JAR (legacy):**
```
target/my-app-runner.jar (fat JAR)
```
- All dependencies bundled
- Slower startup

---

## Key Quarkus Maven Plugin Goals

### `quarkus:dev`
- Runs development mode
- Hot reload enabled
- Uses `QuarkusDevModeMain`
- Watches for file changes
- Re-runs augmentation on change

### `quarkus:build`
- Runs augmentation only
- Called automatically during `mvn package`

### `quarkus:generate-code`
- Generates sources before compilation
- For extensions that need code generation

---

## Dependency Types in Quarkus

### Runtime Dependencies
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-arc</artifactId>
</dependency>
```
- Included in final application
- Available at runtime
- Contains runtime APIs

### Deployment Dependencies
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-arc-deployment</artifactId>
    <scope>provided</scope>
</dependency>
```
- **NOT** included in final application
- Only used during augmentation
- Contains `@BuildStep` processors
- Automatically resolved by `quarkus-maven-plugin`

---

## Extension Structure

Each Quarkus extension has two modules:

### Runtime Module (`quarkus-arc`)
```
quarkus-arc/
├── src/main/java/
│   └── io/quarkus/arc/
│       ├── Arc.java              # Public API
│       ├── ArcContainer.java
│       └── runtime/              # Runtime recorders
└── pom.xml
```
- Public APIs
- Runtime behavior
- Minimal dependencies

### Deployment Module (`quarkus-arc-deployment`)
```
quarkus-arc-deployment/
├── src/main/java/
│   └── io/quarkus/arc/deployment/
│       ├── ArcProcessor.java     # @BuildStep methods
│       └── BeanArchiveBuildItem.java
└── pom.xml
```
- Build-time processing
- Bytecode generation
- Heavy dependencies (Jandex, Gizmo, ASM)

---

## Build-Time vs Runtime Principle

**Build-Time (Augmentation):**
- Annotation scanning
- Bean discovery
- Configuration resolution
- Bytecode generation
- Reflection registration

**Runtime (Application Startup):**
- Load pre-computed metadata
- Initialize containers
- Start services
- **NO** classpath scanning
- **NO** reflection discovery

This is why Quarkus starts in milliseconds.

---

## Comparison: Maven vs Bazel

| Aspect | Maven | Bazel (Our Goal) |
|--------|-------|------------------|
| **Compilation** | `maven-compiler-plugin` | `java_library` |
| **Augmentation** | `quarkus-maven-plugin:build` | `quarkus_augment` rule |
| **Packaging** | `maven-jar-plugin` | `java_binary` |
| **Dependency Resolution** | Maven resolver | Bazel external deps |
| **Classloader Setup** | Plugin handles | We must handle |
| **Extension Discovery** | Via `META-INF/quarkus-extension.properties` | We must implement |

---

## What We Need to Implement in Bazel

### ✅ Already Have:
1. Jandex indexing (`JandexIndexer.java`)
2. Route discovery (in `BazelQuarkusAugmentor.java`)
3. Bytecode generation with Gizmo
4. Three-layer architecture (compile → augment → runtime)

### ❌ Missing:
1. **ApplicationModel building** - Dependency graph from Bazel
2. **QuarkusAugmentor integration** - Run actual deployment pipeline
3. **Build step execution** - Run `@BuildStep` methods from deployment modules
4. **Extension discovery** - Find deployment modules from runtime deps
5. **Classloader isolation** - Proper runtime/deployment separation
6. **Build-time recorders** - Record/replay mechanism

---

## Critical Classes to Understand

### Bootstrap Layer
- `io.quarkus.bootstrap.model.ApplicationModel` - Dependency model
- `io.quarkus.bootstrap.app.CuratedApplication` - Augmented app
- `io.quarkus.bootstrap.classloading.QuarkusClassLoader` - Isolated classloader

### Deployment Layer
- `io.quarkus.deployment.QuarkusAugmentor` - Main augmentation orchestrator
- `io.quarkus.deployment.builditem.BuildItem` - Data passed between build steps
- `io.quarkus.deployment.annotations.BuildStep` - Processor methods

### ArC (CDI) Layer
- `io.quarkus.arc.processor.BeanProcessor` - CDI bean processing
- `io.quarkus.arc.processor.BeanDeployment` - Bean metadata
- `io.quarkus.arc.Arc` - Runtime container API

### Bytecode Generation
- `io.quarkus.gizmo.ClassCreator` - Generate classes
- `io.quarkus.gizmo.MethodCreator` - Generate methods
- `io.quarkus.gizmo.BytecodeCreator` - Low-level bytecode

---

## Maven POM Example

```xml
<project>
    <dependencies>
        <!-- Runtime: In final app -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-arc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy-reactive</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 1. Compile -->
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
            </plugin>

            <!-- 2. Augment + Package -->
            <plugin>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <goals>
                            <goal>build</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

The plugin automatically:
1. Finds deployment modules (via extension metadata)
2. Downloads them (scope=provided)
3. Runs augmentation
4. Packages the result

---

## Summary

**Quarkus Maven Build = 3 Phases:**

1. **Compile** → `.java` to `.class`
2. **Augment** → `.class` + Jandex + Deployment Modules → Generated bytecode
3. **Package** → Augmented classes + deps → Runnable JAR

**Key Innovation:** Most work happens at build time (phase 2), so runtime is fast.

**Our Bazel Challenge:** Replicate phase 2 (augmentation) in Bazel's build model.
