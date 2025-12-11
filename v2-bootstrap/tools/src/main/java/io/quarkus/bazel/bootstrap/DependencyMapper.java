package io.quarkus.bazel.bootstrap;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps runtime extension artifacts to their deployment counterparts.
 *
 * This is critical for QuarkusBootstrap to know which deployment
 * modules contain the @BuildStep processors for each extension.
 */
public class DependencyMapper {

    /**
     * Result of dependency mapping.
     */
    public static class MappingResult {
        private final Map<ExtensionInfo, Path> runtimeToDeployment;
        private final List<ExtensionInfo> unmappedExtensions;

        public MappingResult(Map<ExtensionInfo, Path> runtimeToDeployment,
                           List<ExtensionInfo> unmappedExtensions) {
            this.runtimeToDeployment = runtimeToDeployment;
            this.unmappedExtensions = unmappedExtensions;
        }

        public Map<ExtensionInfo, Path> getRuntimeToDeployment() {
            return runtimeToDeployment;
        }

        public List<ExtensionInfo> getUnmappedExtensions() {
            return unmappedExtensions;
        }

        public boolean hasUnmappedExtensions() {
            return !unmappedExtensions.isEmpty();
        }
    }

    /**
     * Map detected extensions to their deployment JARs.
     */
    public static MappingResult mapExtensions(List<ExtensionInfo> extensions, List<Path> deploymentJars) {
        Map<ExtensionInfo, Path> mapping = new HashMap<>();
        java.util.List<ExtensionInfo> unmapped = new java.util.ArrayList<>();

        // Build index of deployment JARs by artifact ID
        Map<String, Path> deploymentIndex = buildDeploymentIndex(deploymentJars);

        for (ExtensionInfo ext : extensions) {
            String expectedDeploymentId = ext.getExpectedDeploymentArtifactId();
            Path deploymentJar = deploymentIndex.get(expectedDeploymentId);

            if (deploymentJar != null) {
                mapping.put(ext, deploymentJar);
            } else {
                unmapped.add(ext);
            }
        }

        return new MappingResult(mapping, unmapped);
    }

    /**
     * Find deployment JAR for a single extension.
     */
    public static Path findDeploymentJar(ExtensionInfo extension, List<Path> deploymentJars) {
        String expectedId = extension.getExpectedDeploymentArtifactId();

        return deploymentJars.stream()
            .filter(jar -> extractArtifactId(jar).equals(expectedId))
            .findFirst()
            .orElse(null);
    }

    private static Map<String, Path> buildDeploymentIndex(List<Path> deploymentJars) {
        Map<String, Path> index = new HashMap<>();

        for (Path jar : deploymentJars) {
            String artifactId = extractArtifactId(jar);
            if (artifactId != null) {
                index.put(artifactId, jar);
            }
        }

        return index;
    }

    /**
     * Extract artifact ID from JAR path.
     * Path pattern: .../ARTIFACT/VERSION/ARTIFACT-VERSION.jar
     */
    static String extractArtifactId(Path jarPath) {
        String fileName = jarPath.getFileName().toString();

        // Remove .jar extension
        if (fileName.endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }

        // Find last dash followed by a digit (version separator)
        int lastDashBeforeVersion = -1;
        for (int i = fileName.length() - 1; i >= 0; i--) {
            if (fileName.charAt(i) == '-') {
                if (i + 1 < fileName.length() && Character.isDigit(fileName.charAt(i + 1))) {
                    lastDashBeforeVersion = i;
                    break;
                }
            }
        }

        if (lastDashBeforeVersion > 0) {
            return fileName.substring(0, lastDashBeforeVersion);
        }

        // Fallback: try to extract from path structure
        String[] parts = jarPath.toString().split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            // Look for pattern: artifact/version/artifact-version.jar
            if (i >= 2 && parts[i].endsWith(".jar")) {
                return parts[i - 2];
            }
        }

        return fileName;
    }
}
