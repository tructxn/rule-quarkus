package io.quarkus.bazel.processor;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds ApplicationModel from Bazel dependencies.
 */
public class ApplicationModelBuilder {
    
    public static ApplicationModel build(List<Path> applicationJars, List<Path> runtimeJars) throws Exception {
        io.quarkus.bootstrap.model.ApplicationModelBuilder builder = 
            new io.quarkus.bootstrap.model.ApplicationModelBuilder();
        
        setApplicationArtifact(builder, applicationJars);
        addRuntimeDependencies(builder, runtimeJars);
        
        return builder.build();
    }
    
    private static void setApplicationArtifact(
            io.quarkus.bootstrap.model.ApplicationModelBuilder builder,
            List<Path> applicationJars) {
        
        ResolvedDependencyBuilder appBuilder = ResolvedDependencyBuilder.newInstance()
            .setGroupId("io.quarkus.bazel")
            .setArtifactId("application")
            .setVersion("1.0.0")
            .setType("jar");
        
        if (!applicationJars.isEmpty()) {
            appBuilder.setResolvedPaths(PathList.from(applicationJars));
        }
        
        builder.setAppArtifact(appBuilder);
    }
    
    private static void addRuntimeDependencies(
            io.quarkus.bootstrap.model.ApplicationModelBuilder builder,
            List<Path> runtimeJars) {
        
        for (Path jar : runtimeJars) {
            MavenCoords coords = parseMavenCoords(jar);
            if (coords != null) {
                builder.addDependency(ResolvedDependencyBuilder.newInstance()
                    .setGroupId(coords.groupId)
                    .setArtifactId(coords.artifactId)
                    .setVersion(coords.version)
                    .setType("jar")
                    .setResolvedPaths(PathList.of(jar)));
            }
        }
    }
    
    private static MavenCoords parseMavenCoords(Path jarPath) {
        String path = jarPath.toString();
        
        // Bazel external JAR path pattern:
        // .../maven2/GROUP_PATH/ARTIFACT/VERSION/ARTIFACT-VERSION.jar
        String[] parts = path.split("/");
        
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
                if (i == parts.length - 2) {
                    version = parts[i];
                } else if (i == parts.length - 3) {
                    artifact = parts[i];
                } else if (i < parts.length - 3) {
                    groupParts.add(parts[i]);
                }
            }
        }
        
        if (artifact != null && version != null && !groupParts.isEmpty()) {
            String groupId = String.join(".", groupParts);
            return new MavenCoords(groupId, artifact, version);
        }
        
        return null;
    }
    
    static class MavenCoords {
        String groupId;
        String artifactId;
        String version;
        
        MavenCoords(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }
}
