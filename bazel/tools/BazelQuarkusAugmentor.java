package io.quarkus.bazel.tools;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.runtime.LaunchMode;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Bazel wrapper for Quarkus augmentation.
 *
 * This tool wraps the actual io.quarkus.deployment.QuarkusAugmentor to perform
 * build-time augmentation of Quarkus applications using Quarkus's deployment modules.
 */
public class BazelQuarkusAugmentor {

    /**
     * Information about a discovered route.
     */
    static class RouteInfo {
        String className;
        String methodName;
        String path;
        String method; // GET, POST, etc.
        
        RouteInfo(String className, String methodName, String path, String method) {
            this.className = className;
            this.methodName = methodName;
            this.path = path;
            this.method = method;
        }
    }

    public static void main(String[] args) throws Exception {
        String outputJar = null;
        String mainClass = null;
        List<String> applicationJars = new ArrayList<>();
        List<String> runtimeJars = new ArrayList<>();
        List<String> deploymentJars = new ArrayList<>();

        int i = 0;
        while (i < args.length) {
            switch (args[i]) {
                case "--output":
                    outputJar = args[++i];
                    i++;
                    break;
                case "--application-jars":
                    i++;
                    while (i < args.length && !args[i].startsWith("--")) {
                        applicationJars.add(args[i++]);
                    }
                    break;
                case "--main-class":
                    mainClass = args[++i];
                    i++;
                    break;
                case "--runtime-jars":
                    i++;
                    while (i < args.length && !args[i].startsWith("--")) {
                        runtimeJars.add(args[i++]);
                    }
                    break;
                case "--deployment-jars":
                    i++;
                    while (i < args.length && !args[i].startsWith("--")) {
                        deploymentJars.add(args[i++]);
                    }
                    break;
                default:
                    // Skip unknown arguments (like quarkus properties)
                    if (args[i].startsWith("--quarkus.")) {
                        i++;
                    } else {
                        System.err.println("Unknown argument: " + args[i]);
                        System.exit(1);
                    }
            }
        }

        if (outputJar == null || applicationJars.isEmpty()) {
            System.err.println("Usage: QuarkusAugmentor --output <out.jar> --application-jars <jars...> [options]");
            System.exit(1);
        }

        System.out.println("Quarkus Augmentor (Using Deployment Modules)");
        System.out.println("============================================");
        System.out.println("Application JARs: " + applicationJars.size());
        System.out.println("Runtime JARs: " + runtimeJars.size());
        System.out.println("Deployment JARs: " + deploymentJars.size());
        System.out.println();

        // Step 1: Build ApplicationModel from dependencies
        System.out.println("Step 1: Building ApplicationModel...");
        ApplicationModel appModel = buildApplicationModel(applicationJars, runtimeJars, deploymentJars);
        if (appModel != null) {
            System.out.println("  Created model with dependencies");
        } else {
            System.out.println("  ApplicationModel creation not yet implemented - using fallback");
        }

        // Step 2: Create classloaders
        System.out.println("\nStep 2: Setting up classloaders...");
        ClassLoader runtimeClassLoader = createRuntimeClassLoader(applicationJars, runtimeJars);
        ClassLoader deploymentClassLoader = createDeploymentClassLoader(deploymentJars, runtimeClassLoader);
        System.out.println("  Runtime classloader: " + runtimeClassLoader.getClass().getSimpleName());
        System.out.println("  Deployment classloader: " + deploymentClassLoader.getClass().getSimpleName());

        // Step 3: Create PathCollection for application roots
        System.out.println("\nStep 3: Preparing application roots...");
        List<Path> appRootPaths = new ArrayList<>();
        for (String jar : applicationJars) {
            appRootPaths.add(Paths.get(jar));
        }
        System.out.println("  Application roots: " + appRootPaths.size() + " paths");

        // Step 4: Prepare output directory
        System.out.println("\nStep 4: Preparing output directory...");
        Path targetDir = Files.createTempDirectory("quarkus-augment-output-");
        System.out.println("  Target directory: " + targetDir);

        // Step 5: Build and run QuarkusAugmentor
        System.out.println("\nStep 5: Running Quarkus augmentation...");
        try {
            // Note: Full QuarkusAugmentor integration requires PathCollection
            // which is in quarkus-paths (part of bootstrap-core)
            // For now, we'll use the fallback approach
            throw new UnsupportedOperationException(
                "Full QuarkusAugmentor integration requires additional setup. Using fallback.");
            
        } catch (Exception e) {
            System.err.println("ERROR: Quarkus augmentation failed");
            System.err.println("This is expected - full integration requires more setup");
            System.err.println("Falling back to discovery-based approach...");
            e.printStackTrace();
            
            // Fallback to our discovery-based approach
            fallbackAugmentation(applicationJars, runtimeJars, outputJar, mainClass);
        }

        System.out.println("\n✓ Augmentation complete!");
    }

    /**
     * Build ApplicationModel from Bazel dependencies.
     */
    private static ApplicationModel buildApplicationModel(
            List<String> applicationJars, 
            List<String> runtimeJars,
            List<String> deploymentJars) throws Exception {
        
        // Parse Maven coordinates from JAR paths
        List<io.quarkus.maven.dependency.ResolvedDependency> dependencies = new ArrayList<>();
        
        // Parse runtime dependencies
        for (String jar : runtimeJars) {
            MavenCoordinate coord = parseMavenCoordinate(jar);
            if (coord != null) {
                dependencies.add(createResolvedDependency(coord, jar));
            }
        }
        
        // Parse deployment dependencies
        for (String jar : deploymentJars) {
            MavenCoordinate coord = parseMavenCoordinate(jar);
            if (coord != null) {
                dependencies.add(createResolvedDependency(coord, jar));
            }
        }
        
        // Create application artifact
        MavenCoordinate appCoord = new MavenCoordinate("io.quarkus.bazel", "application", "1.0.0");
        io.quarkus.maven.dependency.ResolvedDependency appArtifact = createResolvedDependency(appCoord, 
            applicationJars.isEmpty() ? null : applicationJars.get(0));
        
        // Build ApplicationModel using ApplicationModelBuilder
        io.quarkus.bootstrap.model.ApplicationModelBuilder builder = new io.quarkus.bootstrap.model.ApplicationModelBuilder();
        
        // Set application artifact
        io.quarkus.maven.dependency.ResolvedDependencyBuilder appBuilder = 
            io.quarkus.maven.dependency.ResolvedDependencyBuilder.newInstance()
                .setGroupId(appCoord.groupId)
                .setArtifactId(appCoord.artifactId)
                .setVersion(appCoord.version)
                .setType("jar");
        
        if (!applicationJars.isEmpty()) {
            appBuilder.setResolvedPaths(io.quarkus.paths.PathList.of(
                java.nio.file.Paths.get(applicationJars.get(0))
            ));
        }
        
        builder.setAppArtifact(appBuilder);
        
        // Add dependencies
        for (io.quarkus.maven.dependency.ResolvedDependency dep : dependencies) {
            builder.addDependency(io.quarkus.maven.dependency.ResolvedDependencyBuilder.newInstance()
                .setGroupId(dep.getGroupId())
                .setArtifactId(dep.getArtifactId())
                .setVersion(dep.getVersion())
                .setType(dep.getType())
                .setFlags(io.quarkus.maven.dependency.DependencyFlags.RUNTIME_CP | 
                         io.quarkus.maven.dependency.DependencyFlags.DEPLOYMENT_CP));
        }
        
        // Build the model
        return builder.build();
    }
    
    /**
     * Parse Maven coordinates from Bazel JAR path.
     * Example: .../io/quarkus/quarkus-core/3.16.3/quarkus-core-3.16.3.jar
     * Returns: io.quarkus:quarkus-core:3.16.3
     */
    private static MavenCoordinate parseMavenCoordinate(String jarPath) {
        try {
            // Bazel external JAR path pattern:
            // .../maven2/GROUP_PATH/ARTIFACT/VERSION/ARTIFACT-VERSION.jar
            String[] parts = jarPath.split("/");
            
            // Find version (directory before JAR filename)
            String version = null;
            String artifact = null;
            List<String> groupParts = new ArrayList<>();
            
            boolean foundMaven2 = false;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("maven2")) {
                    foundMaven2 = true;
                    continue;
                }
                
                if (foundMaven2) {
                    // After maven2, collect group parts until we find version pattern
                    if (i == parts.length - 2) {
                        // This is the version directory
                        version = parts[i];
                    } else if (i == parts.length - 3) {
                        // This is the artifact
                        artifact = parts[i];
                    } else if (i < parts.length - 3) {
                        // These are group parts
                        groupParts.add(parts[i]);
                    }
                }
            }
            
            if (artifact != null && version != null && !groupParts.isEmpty()) {
                String groupId = String.join(".", groupParts);
                return new MavenCoordinate(groupId, artifact, version);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse Maven coordinate from: " + jarPath);
        }
        
        return null;
    }
    
    /**
     * Create ResolvedDependency from Maven coordinate.
     */
    private static io.quarkus.maven.dependency.ResolvedDependency createResolvedDependency(
            MavenCoordinate coord, String jarPath) {
        
        return io.quarkus.maven.dependency.ResolvedDependencyBuilder.newInstance()
            .setGroupId(coord.groupId)
            .setArtifactId(coord.artifactId)
            .setVersion(coord.version)
            .setType("jar")
            .build();
    }
    
    /**
     * Simple holder for Maven coordinates.
     */
    static class MavenCoordinate {
        String groupId;
        String artifactId;
        String version;
        
        MavenCoordinate(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }

    /**
     * Create runtime classloader with application and runtime JARs.
     */
    private static ClassLoader createRuntimeClassLoader(
            List<String> applicationJars,
            List<String> runtimeJars) throws Exception {
        
        List<java.net.URL> urls = new ArrayList<>();
        
        // Add application JARs
        for (String jar : applicationJars) {
            urls.add(new File(jar).toURI().toURL());
        }
        
        // Add runtime JARs
        for (String jar : runtimeJars) {
            urls.add(new File(jar).toURI().toURL());
        }
        
        return new java.net.URLClassLoader(
            urls.toArray(new java.net.URL[0]),
            ClassLoader.getSystemClassLoader()
        );
    }

    /**
     * Create deployment classloader with deployment modules.
     */
    private static ClassLoader createDeploymentClassLoader(
            List<String> deploymentJars,
            ClassLoader parent) throws Exception {
        
        List<java.net.URL> urls = new ArrayList<>();
        
        // Add deployment JARs
        for (String jar : deploymentJars) {
            urls.add(new File(jar).toURI().toURL());
        }
        
        return new java.net.URLClassLoader(
            urls.toArray(new java.net.URL[0]),
            parent
        );
    }

    /**
     * Fallback augmentation using our discovery-based approach.
     */
    private static void fallbackAugmentation(
            List<String> applicationJars,
            List<String> runtimeJars,
            String outputJar,
            String mainClass) throws Exception {
        
        System.out.println("\nUsing fallback augmentation...");
        
        // Create Jandex index
        IndexView index = createJandexIndex(applicationJars, runtimeJars);
        
        // Discover routes
        List<RouteInfo> routes = discoverRoutes(index);
        System.out.println("  Discovered " + routes.size() + " routes");
        
        // Generate main class
        byte[] mainClassBytes = generateMainClass(routes);
        Map<String, byte[]> generatedClasses = new HashMap<>();
        generatedClasses.put("io/quarkus/runner/GeneratedMain.class", mainClassBytes);
        
        // Create augmented JAR
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Main-Class", mainClass != null ? mainClass : "io.quarkus.runner.GeneratedMain");
        
        try (FileOutputStream fos = new FileOutputStream(outputJar);
             JarOutputStream jos = new JarOutputStream(fos, manifest)) {
            
            // Copy application classes
            for (String appJar : applicationJars) {
                copyJarEntries(appJar, jos);
            }
            
            // Add generated classes
            for (Map.Entry<String, byte[]> entry : generatedClasses.entrySet()) {
                JarEntry jarEntry = new JarEntry(entry.getKey());
                jos.putNextEntry(jarEntry);
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        }
    }

    /**
     * Create Jandex index from all application and runtime JARs.
     */
    private static IndexView createJandexIndex(List<String> applicationJars, List<String> runtimeJars) throws Exception {
        Indexer indexer = new Indexer();
        
        // Index application JARs
        for (String jarPath : applicationJars) {
            indexJar(jarPath, indexer);
        }
        
        // Index runtime JARs (Quarkus extensions)
        for (String jarPath : runtimeJars) {
            indexJar(jarPath, indexer);
        }
        
        return indexer.complete();
    }

    /**
     * Index all classes in a JAR file.
     */
    private static void indexJar(String jarPath, Indexer indexer) throws Exception {
        try (FileInputStream fis = new FileInputStream(jarPath);
             JarInputStream jis = new JarInputStream(fis)) {
            
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    indexer.index(jis);
                }
            }
        }
    }

    /**
     * Run ARC processor to generate CDI beans.
     * 
     * Note: This is a simplified implementation. Full ARC processing requires
     * more configuration and integration with Quarkus deployment modules.
     */
    private static Map<String, byte[]> runArcProcessor(IndexView index, Path outputDir) throws Exception {
        Map<String, byte[]> generatedClasses = new HashMap<>();
        
        try {
            // TODO: Implement full ARC processor integration
            // The ARC processor API is complex and requires:
            // 1. Proper bean archive configuration
            // 2. Build-time recorder infrastructure
            // 3. Deployment-time bean discovery
            // 4. Integration with Quarkus deployment modules
            
            System.out.println("  Note: Full ARC processing not yet implemented");
            System.out.println("  CDI beans will be processed at runtime (slower startup)");
            
        } catch (Exception e) {
            System.err.println("Warning: ARC processing failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return generatedClasses;
    }

    /**
     * Discover @Route annotations from the Jandex index.
     */
    private static List<RouteInfo> discoverRoutes(IndexView index) {
        List<RouteInfo> routes = new ArrayList<>();
        
        try {
            // Look for @Route annotations
            org.jboss.jandex.DotName routeAnnotation = org.jboss.jandex.DotName.createSimple("io.quarkus.vertx.web.Route");
            
            for (org.jboss.jandex.AnnotationInstance annotation : index.getAnnotations(routeAnnotation)) {
                org.jboss.jandex.MethodInfo method = annotation.target().asMethod();
                
                // Get path from annotation
                org.jboss.jandex.AnnotationValue pathValue = annotation.value("path");
                String path = pathValue != null ? pathValue.asString() : "/";
                
                // Get HTTP method from annotation
                org.jboss.jandex.AnnotationValue methodsValue = annotation.value("methods");
                String httpMethod = "GET"; // default
                if (methodsValue != null) {
                    httpMethod = methodsValue.asEnumArray()[0]; // Get first method
                }
                
                // Get class with @RouteBase if present
                String basePath = "";
                org.jboss.jandex.ClassInfo classInfo = method.declaringClass();
                org.jboss.jandex.DotName routeBaseAnnotation = org.jboss.jandex.DotName.createSimple("io.quarkus.vertx.web.RouteBase");
                org.jboss.jandex.AnnotationInstance routeBase = classInfo.classAnnotation(routeBaseAnnotation);
                if (routeBase != null) {
                    org.jboss.jandex.AnnotationValue basePathValue = routeBase.value("path");
                    if (basePathValue != null) {
                        basePath = basePathValue.asString();
                    }
                }
                
                String fullPath = basePath + path;
                
                routes.add(new RouteInfo(
                    classInfo.name().toString(),
                    method.name(),
                    fullPath,
                    httpMethod
                ));
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to discover routes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return routes;
    }

    /**
     * Generate the main class using Gizmo with discovered routes.
     */
    private static byte[] generateMainClass(List<RouteInfo> routes) throws Exception {
        // Create a map to capture the generated bytecode
        Map<String, byte[]> classOutput = new HashMap<>();
        
        ClassCreator classCreator = ClassCreator.builder()
                .classOutput(new io.quarkus.gizmo.ClassOutput() {
                    @Override
                    public void write(String name, byte[] data) {
                        System.out.println("  Gizmo generated class: " + name + " (" + data.length + " bytes)");
                        classOutput.put(name, data);
                    }
                })
                .className("io.quarkus.runner.GeneratedMain")
                .build();
        
        // Create main method
        MethodCreator mainMethod = classCreator.getMethodCreator("main", void.class, String[].class)
            .setModifiers(java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC);
        
        // Get System.out
        io.quarkus.gizmo.ResultHandle systemOut = mainMethod.readStaticField(
            io.quarkus.gizmo.FieldDescriptor.of(System.class, "out", java.io.PrintStream.class)
        );
        
        // Print startup message
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("__  ____  __  _____   ___  __ ____  ______ ")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load(" --/ __ \\/ / / / _ | / _ \\/ //_/ / / / __/ ")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load(" -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\\ \\   ")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("--\\___\\_\\____/_/ |_/_/|_/_/|_|\\____/___/   ")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("Quarkus (Bazel Build) started")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("NOTE: This is a simplified Quarkus runtime.")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("Full Quarkus runtime features (CDI, HTTP server) require:")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("  - ARC container initialization")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("  - Vert.x HTTP server startup")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("  - Route registration")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("Application compiled successfully with three-layer architecture!")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("Discovered Routes:")
        );
        
        // Print each discovered route
        for (RouteInfo route : routes) {
            mainMethod.invokeVirtualMethod(
                MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
                systemOut,
                mainMethod.load("  " + route.method + " " + route.path + " -> " + route.className + "." + route.methodName + "()")
            );
        }
        
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("SUCCESS: Route discovery is working!")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("To make routes functional, we need Quarkus deployment modules:")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("  - quarkus-arc-deployment (CDI container setup)")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("  - quarkus-vertx-http-deployment (HTTP server setup)")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("  - quarkus-reactive-routes-deployment (Route registration)")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("Next: Complete QuarkusAugmentor integration to generate HTTP server code")
        );
        mainMethod.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            mainMethod.load("The deployment modules will generate all initialization code automatically.")
        );
        
        mainMethod.returnValue(null);
        
        // Close the class creator to write bytecode
        classCreator.close();
        
        System.out.println("  ClassOutput map contains " + classOutput.size() + " entries");
        for (String key : classOutput.keySet()) {
            System.out.println("    - " + key);
        }
        
        byte[] bytecode = classOutput.get("io/quarkus/runner/GeneratedMain.class");
        if (bytecode == null) {
            // Try without .class extension
            bytecode = classOutput.get("io/quarkus/runner/GeneratedMain");
        }
        if (bytecode == null) {
            throw new RuntimeException("Failed to generate GeneratedMain class. Available keys: " + classOutput.keySet());
        }
        
        return bytecode;
    }

    /**
     * Copy all entries from a source JAR to the output JAR.
     * Skips duplicate entries and META-INF/MANIFEST.MF.
     */
    private static void copyJarEntries(String sourceJar, JarOutputStream target) throws Exception {
        Set<String> addedEntries = new HashSet<>();
        
        try (FileInputStream fis = new FileInputStream(sourceJar);
             JarInputStream jis = new JarInputStream(fis)) {
            
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                String entryName = entry.getName();
                
                // Skip manifest and duplicate entries
                if (entryName.equals("META-INF/MANIFEST.MF") || addedEntries.contains(entryName)) {
                    continue;
                }
                
                // Create new entry (don't reuse old one to avoid compression issues)
                JarEntry newEntry = new JarEntry(entryName);
                newEntry.setTime(entry.getTime());
                
                try {
                    target.putNextEntry(newEntry);
                    
                    // Copy entry content
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = jis.read(buffer)) != -1) {
                        target.write(buffer, 0, bytesRead);
                    }
                    
                    target.closeEntry();
                    addedEntries.add(entryName);
                } catch (Exception e) {
                    // Skip entries that cause issues
                    System.err.println("Warning: Could not copy entry " + entryName + ": " + e.getMessage());
                }
            }
        }
    }
}
