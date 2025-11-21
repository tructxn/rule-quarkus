package io.quarkus.bazel.processor;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs Quarkus augmentation using QuarkusBootstrap API.
 * 
 * Flow:
 * 1. Build ApplicationModel from dependencies
 * 2. Create QuarkusBootstrap
 * 3. Bootstrap application
 * 4. Run augmentation
 * 5. Return augmented output
 */
public class QuarkusAugmentorRunner {
    
    public static void run(
            List<Path> applicationJars,
            List<Path> runtimeJars,
            Path outputDir) throws Exception {
        
        System.out.println("Starting Quarkus augmentation...");
        
        ApplicationModel appModel = buildApplicationModel(applicationJars, runtimeJars);
        
        QuarkusBootstrap bootstrap = createBootstrap(applicationJars, appModel, outputDir);
        
        try (CuratedApplication curatedApp = bootstrap.bootstrap()) {
            AugmentAction augmentAction = curatedApp.createAugmentor();
            AugmentResult result = augmentAction.createProductionApplication();
            
            System.out.println("Augmentation completed successfully");
            System.out.println("Output directory: " + outputDir);
        }
    }
    
    private static ApplicationModel buildApplicationModel(
            List<Path> applicationJars,
            List<Path> runtimeJars) throws Exception {
        
        System.out.println("Building ApplicationModel...");
        System.out.println("  Application JARs: " + applicationJars.size());
        System.out.println("  Runtime JARs: " + runtimeJars.size());
        
        return ApplicationModelBuilder.build(applicationJars, runtimeJars);
    }
    
    private static QuarkusBootstrap createBootstrap(
            List<Path> applicationJars,
            ApplicationModel appModel,
            Path outputDir) throws Exception {
        
        PathsCollection appRoots = PathsCollection.from(applicationJars);
        
        return QuarkusBootstrap.builder()
            .setApplicationRoot(appRoots)
            .setTargetDirectory(outputDir)
            .setBaseName("quarkus-app")
            .setMode(QuarkusBootstrap.Mode.PROD)
            .setIsolateDeployment(true)
            .build();
    }
}
