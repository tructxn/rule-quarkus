"""
Bazel rules for building Quarkus applications.

A Quarkus application uses a three-layer build architecture:
1. Layer 1 (Compilation): Compile application code to bytecode
2. Layer 2 (Augmentation): Process bytecode to generate CDI beans and optimizations
3. Layer 3 (Runtime): Package augmented code into executable application

This matches Quarkus's actual build model where augmentation happens after compilation.
"""

load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_test")
load("//v1-custom/rules:quarkus_augment.bzl", _quarkus_augment = "quarkus_augment")

def quarkus_application(
        name,
        srcs = [],
        resources = [],
        deps = [],
        extensions = [],
        deployment_extensions = [],
        main_class = None,
        jvm_flags = [],
        visibility = None,
        tags = [],
        **kwargs):
    """
    Builds a Quarkus application using three-layer architecture.

    Build Flow (Three Layers):
    1. Layer 1 - Compilation: Compile application sources to bytecode
    2. Layer 2 - Augmentation: Process bytecode to generate CDI beans and optimizations
    3. Layer 3 - Runtime: Package augmented code into executable application

    This matches Quarkus's actual build model where augmentation happens after compilation.

    Args:
        name: Name of the application
        srcs: Java source files
        resources: Application resources (application.properties, etc.)
        deps: Dependencies (non-Quarkus libraries)
        extensions: Quarkus extension runtime modules
        deployment_extensions: Quarkus extension deployment modules
        main_class: Main class (auto-detected if not specified)
        jvm_flags: JVM flags for running the application
        visibility: Target visibility
        tags: Build tags
        **kwargs: Additional arguments
    """

    # ============================================================================
    # LAYER 1: COMPILATION
    # Compile application sources to bytecode
    # ============================================================================
    lib_name = name + "_lib"
    
    java_library(
        name = lib_name,
        srcs = srcs,
        resources = resources,
        deps = deps + extensions,
        javacopts = [
            "-parameters",  # Required by Quarkus for parameter name retention
        ],
        tags = tags + ["manual"],
        visibility = ["//visibility:private"],
    )

    # ============================================================================
    # LAYER 2: AUGMENTATION (Middleware)
    # Process compiled bytecode to generate CDI beans and optimizations
    # ============================================================================
    augmented_name = name + "_augmented"
    
    _quarkus_augment(
        name = augmented_name,
        application = [":" + lib_name],
        runtime_deps = extensions,
        deployment_deps = deployment_extensions,
        main_class = main_class or "io.quarkus.runner.GeneratedMain",
        application_name = name,
        tags = tags + ["manual"],
        visibility = ["//visibility:private"],
    )

    # ============================================================================
    # LAYER 3: RUNTIME
    # Package augmented code into executable application
    # ============================================================================
    java_binary(
        name = name,
        runtime_deps = [
            ":" + augmented_name,
        ] + extensions,
        main_class = main_class or "io.quarkus.runner.GeneratedMain",
        jvm_flags = jvm_flags + [
            "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
            "-Dquarkus.application.name=" + name,
        ],
        visibility = visibility,
        tags = tags,
        **kwargs
    )

def quarkus_native_image(
        name,
        application,
        visibility = None,
        tags = []):
    """
    Builds a GraalVM native image from a Quarkus application.

    This creates a standalone native executable with instant startup
    and minimal memory footprint.

    Args:
        name: Name of the native image
        application: Quarkus application target
        visibility: Target visibility
        tags: Build tags
    """

    # TODO: Implement native image compilation
    # This will require:
    # 1. GraalVM native-image toolchain
    # 2. Native image configuration from augmentation
    # 3. Proper linking of native libraries
    native.genrule(
        name = name,
        srcs = [application],
        outs = [name + "_binary"],
        cmd = """
        echo "Native image compilation not yet implemented" > $@
        echo "Application: $(location {app})" >> $@
        """.format(app = application),
        visibility = visibility,
        tags = tags + ["manual", "native"],
    )

def quarkus_test(
        name,
        srcs,
        deps = [],
        extensions = [],
        test_class = None,
        resources = [],
        jvm_flags = [],
        visibility = None,
        tags = [],
        **kwargs):
    """
    Creates a Quarkus test target.

    Quarkus tests use @QuarkusTest annotation and run with a
    Quarkus application context.

    Args:
        name: Test name
        srcs: Test source files
        deps: Test dependencies
        extensions: Quarkus extensions needed for tests
        test_class: Main test class (auto-detected if not specified)
        resources: Test resources
        jvm_flags: JVM flags for tests
        visibility: Target visibility
        tags: Build tags
        **kwargs: Additional arguments passed to java_test
    """
    java_test(
        name = name,
        srcs = srcs,
        deps = deps + extensions + [
            "@maven//:io_quarkus_quarkus_junit5",
            "@maven//:org_junit_jupiter_junit_jupiter_api",
            "@maven//:org_junit_jupiter_junit_jupiter_engine",
        ],
        test_class = test_class,
        resources = resources,
        jvm_flags = jvm_flags + [
            "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
        ],
        javacopts = ["-parameters"],
        visibility = visibility,
        tags = tags,
        **kwargs
    )
