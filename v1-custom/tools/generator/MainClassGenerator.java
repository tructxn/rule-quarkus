package io.quarkus.bazel.generator;

import io.quarkus.bazel.discovery.DiscoveryResult;
import io.quarkus.bazel.discovery.RouteInfo;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates main class using Gizmo.
 */
public class MainClassGenerator {
    
    public static GeneratedClass generate(DiscoveryResult discovery) throws Exception {
        Map<String, byte[]> classOutput = new HashMap<>();
        
        ClassCreator creator = createClassCreator(classOutput);
        MethodCreator main = createMainMethod(creator);
        
        addQuarkusBanner(main);
        addRouteLogging(main, discovery.getRoutes());
        
        main.returnValue(null);
        creator.close();
        
        return extractBytecode(classOutput);
    }
    
    private static ClassCreator createClassCreator(Map<String, byte[]> classOutput) {
        return ClassCreator.builder()
            .classOutput((name, data) -> {
                System.out.println("  Generated class: " + name + " (" + data.length + " bytes)");
                classOutput.put(name, data);
            })
            .className("io.quarkus.runner.GeneratedMain")
            .build();
    }
    
    private static MethodCreator createMainMethod(ClassCreator creator) {
        return creator.getMethodCreator("main", void.class, String[].class)
            .setModifiers(java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC);
    }
    
    private static void addQuarkusBanner(MethodCreator main) {
        ResultHandle systemOut = main.readStaticField(
            FieldDescriptor.of(System.class, "out", java.io.PrintStream.class)
        );
        
        String[] bannerLines = {
            "__  ____  __  _____   ___  __ ____  ______ ",
            " --/ __ \\/ / / / _ | / _ \\/ //_/ / / / __/ ",
            " -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\\ \\   ",
            "--\\___\\_\\____/_/ |_/_/|_/_/|_|\\____/___/   ",
            "",
            "Quarkus (Bazel Build) started",
            ""
        };
        
        for (String line : bannerLines) {
            println(main, systemOut, line);
        }
    }
    
    private static void addRouteLogging(MethodCreator main, List<RouteInfo> routes) {
        ResultHandle systemOut = main.readStaticField(
            FieldDescriptor.of(System.class, "out", java.io.PrintStream.class)
        );
        
        println(main, systemOut, "Discovered Routes:");
        
        for (RouteInfo route : routes) {
            String msg = String.format("  %s %s -> %s.%s()",
                route.getHttpMethod(),
                route.getPath(),
                route.getClassName(),
                route.getMethodName()
            );
            println(main, systemOut, msg);
        }
        
        println(main, systemOut, "");
    }
    
    private static void println(MethodCreator main, ResultHandle systemOut, String message) {
        main.invokeVirtualMethod(
            MethodDescriptor.ofMethod(java.io.PrintStream.class, "println", void.class, String.class),
            systemOut,
            main.load(message)
        );
    }
    
    private static GeneratedClass extractBytecode(Map<String, byte[]> classOutput) {
        byte[] bytecode = classOutput.get("io/quarkus/runner/GeneratedMain.class");
        if (bytecode == null) {
            bytecode = classOutput.get("io/quarkus/runner/GeneratedMain");
        }
        if (bytecode == null) {
            throw new RuntimeException("Failed to generate GeneratedMain class");
        }
        
        return new GeneratedClass("io/quarkus/runner/GeneratedMain.class", bytecode);
    }
}
