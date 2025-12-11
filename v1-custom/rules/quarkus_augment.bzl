"""
Quarkus Augmentation Rule

This rule implements the Quarkus "augmentation phase" - the middleware layer that:
1. Takes compiled application JAR as input
2. Runs Jandex indexing on all classes
3. Executes ARC processor to generate CDI beans
4. Generates optimized bytecode
5. Outputs augmented JAR ready for runtime

This is the core of Quarkus's build-time optimization.
"""

load("@rules_java//java:defs.bzl", "JavaInfo")

def _quarkus_augment_impl(ctx):
    """
    Implementation of the Quarkus augmentation phase.
    
    This is the middleware layer between compilation and runtime.
    """
    
    # Output augmented JAR
    augmented_jar = ctx.actions.declare_file(ctx.label.name + "-augmented.jar")
    
    # Collect all input JARs
    application_jars = []
    for dep in ctx.attr.application:
        if JavaInfo in dep:
            application_jars.extend([jar.class_jar for jar in dep[JavaInfo].outputs.jars])
    
    # Collect runtime dependency JARs
    runtime_jars = []
    for dep in ctx.attr.runtime_deps:
        if JavaInfo in dep:
            runtime_jars.extend([jar.class_jar for jar in dep[JavaInfo].outputs.jars])
    
    # Collect deployment dependency JARs (for augmentation)
    deployment_jars = []
    for dep in ctx.attr.deployment_deps:
        if JavaInfo in dep:
            deployment_jars.extend([jar.class_jar for jar in dep[JavaInfo].outputs.jars])
    
    # Create arguments for augmentation tool
    args = ctx.actions.args()
    args.add("--output", augmented_jar)
    args.add_all("--application-jars", application_jars)
    args.add_all("--runtime-jars", runtime_jars)
    args.add_all("--deployment-jars", deployment_jars)
    
    if ctx.attr.main_class:
        args.add("--main-class", ctx.attr.main_class)
    
    # Add Quarkus build properties
    args.add("--quarkus.application.name=" + ctx.attr.application_name)
    
    # Run augmentation tool
    ctx.actions.run(
        outputs = [augmented_jar],
        inputs = application_jars + runtime_jars + deployment_jars,
        executable = ctx.executable._augmentor,
        arguments = [args],
        mnemonic = "QuarkusAugment",
        progress_message = "Augmenting Quarkus application %s" % ctx.label.name,
    )
    
    # Return JavaInfo provider so this can be used as a dependency
    return [
        DefaultInfo(files = depset([augmented_jar])),
        JavaInfo(
            output_jar = augmented_jar,
            compile_jar = augmented_jar,
        ),
    ]

quarkus_augment = rule(
    implementation = _quarkus_augment_impl,
    attrs = {
        "application": attr.label_list(
            providers = [JavaInfo],
            mandatory = True,
            doc = "Application library to augment (compiled classes)",
        ),
        "runtime_deps": attr.label_list(
            providers = [JavaInfo],
            default = [],
            doc = "Runtime dependencies (Quarkus extensions, libraries)",
        ),
        "deployment_deps": attr.label_list(
            providers = [JavaInfo],
            default = [],
            doc = "Deployment dependencies (Quarkus deployment modules for augmentation)",
        ),
        "main_class": attr.string(
            doc = "Main class name",
        ),
        "application_name": attr.string(
            mandatory = True,
            doc = "Quarkus application name",
        ),
        "_augmentor": attr.label(
            default = Label("//v1-custom/tools:quarkus_augmentor"),
            executable = True,
            cfg = "exec",
            doc = "Quarkus augmentation tool",
        ),
    },
    doc = """
    Quarkus augmentation middleware layer.
    
    This rule takes compiled application classes and runs the Quarkus build-time
    augmentation process to generate optimized bytecode and CDI beans.
    
    Build Flow:
    1. Input: Compiled application JAR (from java_library)
    2. Process: Run Jandex indexing + ARC processor + bytecode optimization
    3. Output: Augmented JAR with generated classes
    
    Example:
        quarkus_augment(
            name = "my-app-augmented",
            application = [":my-app-lib"],
            runtime_deps = ["@maven//:io_quarkus_quarkus_arc"],
            deployment_deps = ["@maven//:io_quarkus_quarkus_arc_deployment"],
            application_name = "my-app",
        )
    """,
)
