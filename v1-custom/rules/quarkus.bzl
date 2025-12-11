"""
Quarkus Bazel Rules (v1-custom)

Approach 1: Custom augmentation implementation.

Example:
    load("//v1-custom/rules:quarkus.bzl", "quarkus_application")

    quarkus_application(
        name = "my-app",
        srcs = glob(["src/main/java/**/*.java"]),
        extensions = ["@maven//:io_quarkus_quarkus_arc"],
        deployment_extensions = ["@maven//:io_quarkus_arc_arc_processor"],
    )
"""

# Re-export all Quarkus rules
load(
    "//v1-custom/rules:quarkus_application.bzl",
    _quarkus_application = "quarkus_application",
    _quarkus_native_image = "quarkus_native_image",
    _quarkus_test = "quarkus_test",
)
load(
    "//v1-custom/rules:quarkus_extension.bzl",
    _quarkus_extension = "quarkus_extension",
    _quarkus_extension_deployment = "quarkus_extension_deployment",
    _quarkus_extension_runtime = "quarkus_extension_runtime",
)
load(
    "//v1-custom/rules:jandex.bzl",
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
