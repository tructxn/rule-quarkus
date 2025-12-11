package io.quarkus.bazel.augmentor;

import java.nio.file.Path;
import java.util.List;

/**
 * Context object containing all inputs for augmentation.
 */
public class AugmentationContext {
    private final List<Path> applicationJars;
    private final List<Path> runtimeJars;
    private final List<Path> deploymentJars;
    private final Path outputJar;
    private final String mainClass;
    private final String applicationName;
    
    public AugmentationContext(
            List<Path> applicationJars,
            List<Path> runtimeJars,
            List<Path> deploymentJars,
            Path outputJar,
            String mainClass,
            String applicationName) {
        this.applicationJars = applicationJars;
        this.runtimeJars = runtimeJars;
        this.deploymentJars = deploymentJars;
        this.outputJar = outputJar;
        this.mainClass = mainClass;
        this.applicationName = applicationName;
    }
    
    public List<Path> getApplicationJars() {
        return applicationJars;
    }
    
    public List<Path> getRuntimeJars() {
        return runtimeJars;
    }
    
    public List<Path> getDeploymentJars() {
        return deploymentJars;
    }
    
    public Path getOutputJar() {
        return outputJar;
    }
    
    public String getMainClass() {
        return mainClass;
    }
    
    public String getApplicationName() {
        return applicationName;
    }
}
