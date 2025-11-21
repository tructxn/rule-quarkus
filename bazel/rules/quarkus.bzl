"""
Quarkus Bazel Rules

Main entry point for Quarkus Bazel rules. Import this file to use
Quarkus rules in your BUILD files.

Example:
    load("//bazel/rules:quarkus.bzl", "quarkus_application", "quarkus_extension")

    quarkus_extension(
        name = "my-extension",
        runtime_srcs = glob(["runtime/src/main/java/**/*.java"]),
        deployment_srcs = glob(["deployment/src/main/java/**/*.java"]),
    )

    quarkus_application(
        name = "my-app",
        srcs = glob(["src/main/java/**/*.java"]),
        extensions = [":my-extension-runtime"],
        deployment_extensions = [":my-extension-deployment"],
    )
"""

# Re-export all Quarkus rules
load(
    "//bazel/rules:quarkus_application.bzl",
    _quarkus_application = "quarkus_application",
    _quarkus_native_image = "quarkus_native_image",
    _quarkus_test = "quarkus_test",
)
load(
    "//bazel/rules:quarkus_extension.bzl",
    _quarkus_extension = "quarkus_extension",
    _quarkus_extension_deployment = "quarkus_extension_deployment",
    _quarkus_extension_runtime = "quarkus_extension_runtime",
)
load(
    "//bazel/rules:jandex.bzl",
    _jandex_index = "jandex_index",
    _jandex_merge = "jandex_merge",
)

# Application rules
quarkus_application = _quarkus_application
quarkus_native_image = _quarkus_native_image
quarkus_test = _quarkus_test

# Extension rules
quarkus_extension = _quarkus_extension
quarkus_extension_runtime = _quarkus_extension_runtime
quarkus_extension_deployment = _quarkus_extension_deployment

# Utility rules
jandex_index = _jandex_index
jandex_merge = _jandex_merge
