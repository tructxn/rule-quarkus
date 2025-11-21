"""
Jandex indexing rules for Quarkus

Jandex is used by Quarkus to index classes at build time for reflection
and annotation scanning without runtime classpath scanning.
"""

def _jandex_index_impl(ctx):
    """Implementation of the jandex_index rule.

    Generates a Jandex index (.idx file) from compiled Java classes.
    """
    output = ctx.actions.declare_file(ctx.label.name + ".idx")

    # Collect all jar files from deps
    jars = []
    for dep in ctx.attr.deps:
        if JavaInfo in dep:
            jars.extend([jar.class_jar for jar in dep[JavaInfo].outputs.jars])

    # Create arguments for jandex
    args = ctx.actions.args()
    args.add("--output", output)
    for jar in jars:
        args.add(jar)

    # Run jandex indexer
    ctx.actions.run(
        outputs = [output],
        inputs = jars,
        executable = ctx.executable._jandex_tool,
        arguments = [args],
        mnemonic = "JandexIndex",
        progress_message = "Generating Jandex index for %s" % ctx.label.name,
    )

    return [
        DefaultInfo(files = depset([output])),
        OutputGroupInfo(jandex_index = depset([output])),
    ]

jandex_index = rule(
    implementation = _jandex_index_impl,
    attrs = {
        "deps": attr.label_list(
            providers = [JavaInfo],
            doc = "Java libraries to index",
        ),
        "_jandex_tool": attr.label(
            default = Label("//bazel/tools:jandex"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = """
    Generates a Jandex index from Java class files.

    Jandex indexing is required by Quarkus for build-time reflection
    and annotation scanning. This rule creates a .idx file that contains
    metadata about classes and annotations.

    Example:
        jandex_index(
            name = "my_index",
            deps = [":my_lib"],
        )
    """,
)

def _jandex_merge_impl(ctx):
    """Merges multiple Jandex indexes into a single index."""
    output = ctx.actions.declare_file(ctx.label.name + ".idx")

    # Collect all index files
    indexes = [index[DefaultInfo].files.to_list()[0] for index in ctx.attr.indexes]

    # Merge indexes
    args = ctx.actions.args()
    args.add("--output", output)
    args.add("--merge")
    args.add_all(indexes)

    ctx.actions.run(
        outputs = [output],
        inputs = indexes,
        executable = ctx.executable._jandex_tool,
        arguments = [args],
        mnemonic = "JandexMerge",
        progress_message = "Merging Jandex indexes for %s" % ctx.label.name,
    )

    return [DefaultInfo(files = depset([output]))]

jandex_merge = rule(
    implementation = _jandex_merge_impl,
    attrs = {
        "indexes": attr.label_list(
            allow_files = [".idx"],
            doc = "Jandex indexes to merge",
        ),
        "_jandex_tool": attr.label(
            default = Label("//bazel/tools:jandex"),
            executable = True,
            cfg = "exec",
        ),
    },
    doc = "Merges multiple Jandex index files into one.",
)
