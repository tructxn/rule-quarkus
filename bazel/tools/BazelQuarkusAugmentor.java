package io.quarkus.bazel.tools;

import io.quarkus.bazel.augmentor.AugmentationContext;
import io.quarkus.bazel.augmentor.ArgumentParser;
import io.quarkus.bazel.discovery.DiscoveryResult;
import io.quarkus.bazel.discovery.RouteDiscovery;
import io.quarkus.bazel.generator.GeneratedClass;
import io.quarkus.bazel.generator.MainClassGenerator;
import io.quarkus.bazel.indexer.IndexBuilder;
import io.quarkus.bazel.packager.JarPackager;
import org.jboss.jandex.IndexView;

import java.util.ArrayList;
import java.util.List;

/**
 * Quarkus augmentation tool for Bazel.
 * 
 * Flow:
 * 1. Parse arguments
 * 2. Create Jandex index
 * 3. Discover routes
 * 4. Generate main class
 * 5. Package augmented JAR
 */
public class BazelQuarkusAugmentor {

    public static void main(String[] args) throws Exception {
        System.out.println("Quarkus Augmentor");
        System.out.println("=================");
        
        AugmentationContext context = ArgumentParser.parse(args);
        
        System.out.println("Application JARs: " + context.getApplicationJars().size());
        System.out.println("Runtime JARs: " + context.getRuntimeJars().size());
        System.out.println();
        
        IndexView index = IndexBuilder.createIndex(context);
        
        DiscoveryResult discovery = RouteDiscovery.discover(index);
        System.out.println("Discovered " + discovery.getRoutes().size() + " routes");
        
        List<GeneratedClass> generated = new ArrayList<>();
        generated.add(MainClassGenerator.generate(discovery));
        
        JarPackager.packageJar(context, generated);
        
        System.out.println("\n✓ Augmentation complete!");
    }
}
