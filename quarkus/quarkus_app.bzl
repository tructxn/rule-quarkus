"""Quarkus application rule implementation."""

def quarkus_app(
        name,
        srcs = [],
        resources = [],
        deps = [],
        main_class = None,
        application_properties = None,
        jvm_flags = [],
        **kwargs):
    """Builds a Quarkus application.

    Args:
        name: Name of the target
        srcs: Java source files
        resources: Resource files
        deps: List of dependencies (Maven artifacts from @maven//:*)
        main_class: Main class to run (optional, will use Quarkus default)
        application_properties: Path to application.properties file
        jvm_flags: JVM flags for running the application
        **kwargs: Additional arguments passed to java_binary
    """

    # Use resources as-is (application_properties should already be in resources glob if needed)
    all_resources = list(resources)

    # Standard Quarkus runtime dependencies
    quarkus_deps = [
        "@maven//:io_quarkus_quarkus_core",
        "@maven//:io_quarkus_quarkus_arc",
        "@maven//:jakarta_enterprise_jakarta_enterprise_cdi_api",
        "@maven//:org_jboss_logging_jboss_logging",
        "@maven//:org_jboss_logmanager_jboss_logmanager",
    ]

    all_deps = quarkus_deps + deps

    # Default JVM flags for Quarkus
    default_jvm_flags = [
        "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
    ]
    all_jvm_flags = default_jvm_flags + jvm_flags

    # Create a library with all the application code
    native.java_library(
        name = name + "_lib",
        srcs = srcs,
        resources = all_resources,
        deps = all_deps,
        resource_strip_prefix = native.package_name(),
        **{k: v for k, v in kwargs.items() if k in ["visibility", "testonly", "tags"]}
    )

    # Determine main class - Quarkus uses io.quarkus.runner.GeneratedMain for fast-jar
    # For simplicity, we'll use a standard main class or Quarkus bootstrap
    if not main_class:
        main_class = "io.quarkus.bootstrap.runner.QuarkusEntryPoint"

    # Create the executable binary (also generates _deploy.jar automatically)
    native.java_binary(
        name = name,
        main_class = main_class,
        runtime_deps = [
            ":" + name + "_lib",
        ],
        jvm_flags = all_jvm_flags,
        deploy_manifest_lines = [
            "Main-Class: " + main_class,
        ],
        **{k: v for k, v in kwargs.items() if k not in ["srcs", "resources", "deps"]}
    )
