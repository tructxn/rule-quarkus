package io.quarkus.bazel.packager;

import io.quarkus.bazel.augmentor.AugmentationContext;

import java.util.jar.Manifest;

/**
 * Builds JAR manifest.
 */
public class ManifestBuilder {
    
    public static Manifest build(AugmentationContext context) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Main-Class", context.getMainClass());
        return manifest;
    }
}
