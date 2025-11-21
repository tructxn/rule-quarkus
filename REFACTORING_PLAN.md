# Refactoring Plan: Align Bazel Rules with Quarkus Maven Architecture

## Current State Analysis

### What We Have (in `bazel/` directory)

#### ✅ Good Architecture
```
bazel/
├── rules/
│   ├── quarkus_application.bzl    # 3-layer: compile → augment → runtime
│   ├── quarkus_augment.bzl        # Augmentation rule
│   ├── jandex.bzl                 # Jandex indexing
│   └── quarkus_extension.bzl      # Extension building
└── tools/
    ├── JandexIndexer.java         # Working Jandex tool
    └── BazelQuarkusAugmentor.java # 755 lines - TOO BIG, needs refactoring
```

#### ❌ Problems
1. **BazelQuarkusAugmentor.java is monolithic** (755 lines)
   - Violates "minimum logic" rule
   - Mixes argument parsing, indexing, discovery, generation, packaging
   - Hard to maintain and test

2. **Missing Maven-equivalent components**
   - No ApplicationModel building
   - No proper QuarkusAugmentor integration
   - No build step execution
   - No extension discovery mechanism

3. **Duplicate/removed code**
   - `quarkus/` directory was removed (good!)
   - But we lost the ArcProcessorRunner concept

---

## Refactoring Goals

### 1. Match Quarkus Maven Architecture
```
Maven Phase              →  Bazel Equivalent
─────────────────────────────────────────────────
maven-compiler-plugin    →  java_library (Layer 1)
quarkus-maven-plugin     →  quarkus_augment (Layer 2)
maven-jar-plugin         →  java_binary (Layer 3)
```

### 2. Follow User's Coding Rules
- ✅ Break into small functions/files
- ✅ Main contains only flow steps
- ✅ Implementation in separate files/classes
- ✅ Use proper objects (not raw JSON/strings)
- ✅ Remove all unused code

### 3. Clean Separation of Concerns
Each tool does ONE thing well.

---

## New Architecture

### Directory Structure
```
bazel/
├── rules/                          # Bazel rules (unchanged)
│   ├── quarkus_application.bzl
│   ├── quarkus_augment.bzl
│   ├── jandex.bzl
│   └── quarkus_extension.bzl
│
├── tools/                          # Build tools (refactored)
│   ├── BUILD.bazel
│   │
│   ├── augmentor/                  # Augmentation pipeline
│   │   ├── BazelQuarkusAugmentor.java      # Main flow (minimal)
│   │   ├── AugmentationContext.java        # Context object
│   │   ├── ArgumentParser.java             # Parse CLI args
│   │   ├── ApplicationModelBuilder.java    # Build dependency model
│   │   ├── ClassLoaderFactory.java         # Create isolated classloaders
│   │   └── AugmentationPipeline.java       # Run augmentation steps
│   │
│   ├── indexer/                    # Jandex indexing
│   │   ├── JandexIndexer.java              # Main indexer (keep as-is)
│   │   └── IndexBuilder.java               # Helper for index creation
│   │
│   ├── discovery/                  # Annotation discovery
│   │   ├── RouteDiscovery.java             # Discover @Route
│   │   ├── BeanDiscovery.java              # Discover CDI beans
│   │   └── DiscoveryResult.java            # Result object
│   │
│   ├── generator/                  # Bytecode generation
│   │   ├── MainClassGenerator.java         # Generate main class
│   │   ├── BannerGenerator.java            # Generate Quarkus banner
│   │   └── GeneratedClass.java             # Generated class object
│   │
│   └── packager/                   # JAR packaging
│       ├── JarPackager.java                # Package augmented JAR
│       └── ManifestBuilder.java            # Build JAR manifest
```

---

## Refactoring Steps

### Step 1: Create Object Models (Domain Objects)

#### `AugmentationContext.java`
```java
package io.quarkus.bazel.augmentor;

public class AugmentationContext {
    private final List<Path> applicationJars;
    private final List<Path> runtimeJars;
    private final List<Path> deploymentJars;
    private final Path outputJar;
    private final String mainClass;
    private final String applicationName;
    
    // Constructor, getters
}
```

#### `DiscoveryResult.java`
```java
package io.quarkus.bazel.discovery;

public class DiscoveryResult {
    private final List<RouteInfo> routes;
    private final List<BeanInfo> beans;
    private final IndexView index;
    
    // Constructor, getters
}
```

#### `GeneratedClass.java`
```java
package io.quarkus.bazel.generator;

public class GeneratedClass {
    private final String className;
    private final byte[] bytecode;
    
    // Constructor, getters
}
```

---

### Step 2: Break Down BazelQuarkusAugmentor.java

#### New `BazelQuarkusAugmentor.java` (Main Flow Only)
```java
package io.quarkus.bazel.augmentor;

public class BazelQuarkusAugmentor {
    
    public static void main(String[] args) throws Exception {
        // Flow only - no implementation details
        AugmentationContext context = ArgumentParser.parse(args);
        
        IndexView index = IndexBuilder.createIndex(context);
        
        DiscoveryResult discovery = runDiscovery(index);
        
        List<GeneratedClass> generated = generateClasses(discovery);
        
        JarPackager.packageJar(context, generated);
        
        System.out.println("✓ Augmentation complete");
    }
    
    private static DiscoveryResult runDiscovery(IndexView index) {
        List<RouteInfo> routes = RouteDiscovery.discover(index);
        List<BeanInfo> beans = BeanDiscovery.discover(index);
        return new DiscoveryResult(routes, beans, index);
    }
    
    private static List<GeneratedClass> generateClasses(DiscoveryResult discovery) {
        List<GeneratedClass> classes = new ArrayList<>();
        classes.add(MainClassGenerator.generate(discovery));
        return classes;
    }
}
```

**Lines: ~30** (down from 755!)

---

### Step 3: Implement Each Component

#### `ArgumentParser.java`
```java
package io.quarkus.bazel.augmentor;

public class ArgumentParser {
    
    public static AugmentationContext parse(String[] args) {
        Arguments parsed = parseArguments(args);
        validate(parsed);
        return buildContext(parsed);
    }
    
    private static Arguments parseArguments(String[] args) {
        // Parse logic
    }
    
    private static void validate(Arguments args) {
        // Validation logic
    }
    
    private static AugmentationContext buildContext(Arguments args) {
        // Build context object
    }
}
```

#### `IndexBuilder.java`
```java
package io.quarkus.bazel.indexer;

public class IndexBuilder {
    
    public static IndexView createIndex(AugmentationContext context) {
        Indexer indexer = new Indexer();
        
        indexJars(context.getApplicationJars(), indexer);
        indexJars(context.getRuntimeJars(), indexer);
        
        return indexer.complete();
    }
    
    private static void indexJars(List<Path> jars, Indexer indexer) {
        // Index logic
    }
}
```

#### `RouteDiscovery.java`
```java
package io.quarkus.bazel.discovery;

public class RouteDiscovery {
    
    public static List<RouteInfo> discover(IndexView index) {
        List<RouteInfo> routes = new ArrayList<>();
        
        Collection<AnnotationInstance> annotations = findRouteAnnotations(index);
        
        for (AnnotationInstance annotation : annotations) {
            RouteInfo route = extractRouteInfo(annotation);
            routes.add(route);
        }
        
        return routes;
    }
    
    private static Collection<AnnotationInstance> findRouteAnnotations(IndexView index) {
        // Find @Route annotations
    }
    
    private static RouteInfo extractRouteInfo(AnnotationInstance annotation) {
        // Extract route details
    }
}
```

#### `MainClassGenerator.java`
```java
package io.quarkus.bazel.generator;

public class MainClassGenerator {
    
    public static GeneratedClass generate(DiscoveryResult discovery) {
        ClassCreator creator = createClassCreator();
        
        MethodCreator main = createMainMethod(creator);
        
        BannerGenerator.addBanner(main);
        addRouteLogging(main, discovery.getRoutes());
        
        main.returnValue(null);
        creator.close();
        
        return extractBytecode(creator);
    }
    
    private static ClassCreator createClassCreator() {
        // Create class creator
    }
    
    private static MethodCreator createMainMethod(ClassCreator creator) {
        // Create main method
    }
    
    private static void addRouteLogging(MethodCreator main, List<RouteInfo> routes) {
        // Add route logging
    }
    
    private static GeneratedClass extractBytecode(ClassCreator creator) {
        // Extract bytecode
    }
}
```

#### `JarPackager.java`
```java
package io.quarkus.bazel.packager;

public class JarPackager {
    
    public static void packageJar(AugmentationContext context, List<GeneratedClass> generated) {
        Manifest manifest = ManifestBuilder.build(context);
        
        try (JarOutputStream jos = createJarOutputStream(context.getOutputJar(), manifest)) {
            copyApplicationClasses(jos, context.getApplicationJars());
            addGeneratedClasses(jos, generated);
        }
    }
    
    private static JarOutputStream createJarOutputStream(Path output, Manifest manifest) {
        // Create JAR output stream
    }
    
    private static void copyApplicationClasses(JarOutputStream jos, List<Path> jars) {
        // Copy classes
    }
    
    private static void addGeneratedClasses(JarOutputStream jos, List<GeneratedClass> classes) {
        // Add generated classes
    }
}
```

---

## Implementation Priority

### Phase 1: Refactor Existing Code (Week 1)
1. ✅ Create object models (Context, Result, GeneratedClass)
2. ✅ Extract ArgumentParser
3. ✅ Extract IndexBuilder
4. ✅ Extract RouteDiscovery
5. ✅ Extract MainClassGenerator
6. ✅ Extract JarPackager
7. ✅ Refactor BazelQuarkusAugmentor to use new components
8. ✅ Update BUILD.bazel files
9. ✅ Test with hello-world example

### Phase 2: Add Missing Components (Week 2)
1. ⏳ Implement ApplicationModelBuilder
2. ⏳ Implement ClassLoaderFactory
3. ⏳ Implement BeanDiscovery
4. ⏳ Add extension discovery mechanism
5. ⏳ Test with CDI example

### Phase 3: Full QuarkusAugmentor Integration (Week 3-4)
1. ⏳ Integrate io.quarkus.deployment.QuarkusAugmentor
2. ⏳ Implement build step execution
3. ⏳ Add build-time recorders
4. ⏳ Test with complex application

---

## File Size Guidelines

Following "minimum logic" rule:

- **Main flow class:** < 50 lines
- **Parser/Builder classes:** < 100 lines
- **Discovery classes:** < 150 lines
- **Generator classes:** < 200 lines
- **Packager classes:** < 150 lines

If any class exceeds these limits, split it further.

---

## Testing Strategy

### Unit Tests
```
bazel/tools/augmentor/
├── ArgumentParserTest.java
├── IndexBuilderTest.java
├── RouteDiscoveryTest.java
└── MainClassGeneratorTest.java
```

### Integration Tests
```
examples/
├── hello-world/          # Basic test
├── cdi-example/          # CDI test
└── rest-example/         # REST + CDI test
```

---

## Success Criteria

### Phase 1 Complete When:
- ✅ BazelQuarkusAugmentor.java < 50 lines
- ✅ All logic extracted to separate files
- ✅ Each file has single responsibility
- ✅ hello-world example builds and runs
- ✅ No unused code remains

### Phase 2 Complete When:
- ✅ CDI bean discovery works
- ✅ ApplicationModel builds correctly
- ✅ Classloaders properly isolated
- ✅ cdi-example builds and runs

### Phase 3 Complete When:
- ✅ Full QuarkusAugmentor integration
- ✅ All Quarkus features work
- ✅ Matches Maven build output
- ✅ Complex applications build and run

---

## Next Steps

1. **Review this plan** - Confirm approach
2. **Start Phase 1** - Begin refactoring
3. **Create flow.md** - Document the flow
4. **Implement step-by-step** - One component at a time
5. **Test continuously** - Verify each step works

Ready to proceed?
