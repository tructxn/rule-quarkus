package io.quarkus.bazel.augmentor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses command-line arguments for augmentation.
 */
public class ArgumentParser {
    
    public static AugmentationContext parse(String[] args) {
        ParsedArgs parsed = parseArguments(args);
        validate(parsed);
        return buildContext(parsed);
    }
    
    private static ParsedArgs parseArguments(String[] args) {
        ParsedArgs result = new ParsedArgs();
        
        int i = 0;
        while (i < args.length) {
            switch (args[i]) {
                case "--output":
                    result.outputJar = args[++i];
                    i++;
                    break;
                case "--application-jars":
                    i++;
                    while (i < args.length && !args[i].startsWith("--")) {
                        result.applicationJars.add(args[i++]);
                    }
                    break;
                case "--main-class":
                    result.mainClass = args[++i];
                    i++;
                    break;
                case "--runtime-jars":
                    i++;
                    while (i < args.length && !args[i].startsWith("--")) {
                        result.runtimeJars.add(args[i++]);
                    }
                    break;
                case "--deployment-jars":
                    i++;
                    while (i < args.length && !args[i].startsWith("--")) {
                        result.deploymentJars.add(args[i++]);
                    }
                    break;
                default:
                    if (args[i].startsWith("--quarkus.")) {
                        String key = args[i].substring(2);
                        if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                            result.properties.put(key, args[i + 1]);
                            i += 2;
                        } else {
                            i++;
                        }
                    } else {
                        i++;
                    }
            }
        }
        
        return result;
    }
    
    private static void validate(ParsedArgs args) {
        if (args.outputJar == null || args.applicationJars.isEmpty()) {
            System.err.println("Usage: QuarkusAugmentor --output <out.jar> --application-jars <jars...>");
            System.exit(1);
        }
    }
    
    private static AugmentationContext buildContext(ParsedArgs args) {
        List<Path> appJars = new ArrayList<>();
        for (String jar : args.applicationJars) {
            appJars.add(Paths.get(jar));
        }
        
        List<Path> runtimeJars = new ArrayList<>();
        for (String jar : args.runtimeJars) {
            runtimeJars.add(Paths.get(jar));
        }
        
        List<Path> deploymentJars = new ArrayList<>();
        for (String jar : args.deploymentJars) {
            deploymentJars.add(Paths.get(jar));
        }
        
        Path outputJar = Paths.get(args.outputJar);
        String mainClass = args.mainClass != null ? args.mainClass : "io.quarkus.runner.GeneratedMain";
        String appName = args.properties.getOrDefault("quarkus.application.name", "application");
        
        return new AugmentationContext(
            appJars,
            runtimeJars,
            deploymentJars,
            outputJar,
            mainClass,
            appName
        );
    }
    
    private static class ParsedArgs {
        String outputJar;
        String mainClass;
        List<String> applicationJars = new ArrayList<>();
        List<String> runtimeJars = new ArrayList<>();
        List<String> deploymentJars = new ArrayList<>();
        java.util.Map<String, String> properties = new java.util.HashMap<>();
    }
}
