# Bazel Rules for Quarkus

Bazel rules for building Quarkus applications, similar to how `rules_spring` works for Spring Boot. These rules provide the functionality of the `quarkus-maven-plugin` but implemented with `.bzl` files for Bazel build support.

## Requirements

- Bazel 8.0 or higher
- Java 21 or higher (JDK, not JRE)

**Important:** Ensure you have a proper JDK installed, not just a JRE. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) if you encounter Java environment issues.

## Features

- ✅ Build Quarkus-style applications with Bazel
- ✅ HTTP server using Vert.x (same as Quarkus)
- ✅ Maven dependency management via `rules_jvm_external`
- ✅ Simple declarative BUILD file syntax
- ⚠️  **Note:** Currently uses simplified runtime without full Quarkus bootstrap (see [BAZEL_LIMITATIONS.md](BAZEL_LIMITATIONS.md))

## Setup

### In your MODULE.bazel

```starlark
bazel_dep(name = "rules_quarkus", version = "0.1.0")
```

Or if using this repository locally:

```starlark
local_path_override(
    module_name = "rules_quarkus",
    path = "../rule-quarkus",
)
```

## Usage

### Basic Quarkus Application

In your `BUILD.bazel`:

```starlark
load("@rules_quarkus//quarkus:defs.bzl", "quarkus_app")

quarkus_app(
    name = "my-app",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "@maven//:io_quarkus_quarkus_resteasy_reactive",
        "@maven//:io_quarkus_quarkus_resteasy_reactive_jackson",
        "@maven//:jakarta_ws_rs_jakarta_ws_rs_api",
    ],
    main_class = "com.example.QuarkusApp",
    application_properties = "src/main/resources/application.properties",
    jvm_flags = [
        "-Xmx512m",
        "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
    ],
)
```

### Build the application

```bash
bazel build //examples/hello-world
```

### Run the application

`bazel run` builds and executes the Quarkus application:

```bash
bazel run //examples/hello-world
```

This will:
1. Build the application and all dependencies
2. Start the Quarkus HTTP server on port 8080
3. Keep running until you press Ctrl+C

### Build a deploy JAR

```bash
bazel build //examples/hello-world:hello-world_deploy.jar
```

## Example Application

A simple "Hello World" Quarkus REST application is provided in `examples/hello-world/`.

### Project Structure

```
examples/hello-world/
├── BUILD.bazel
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── example/
        │           ├── QuarkusApp.java
        │           └── GreetingResource.java
        └── resources/
            └── application.properties
```

### Running the Example

```bash
# Build the example
bazel build //examples/hello-world

# Run the example
bazel run //examples/hello-world

# Test the endpoint (in another terminal)
curl http://localhost:8080/hello
```

Expected output: `Hello from Quarkus with Bazel!`

## API Reference

### quarkus_app

Builds a Quarkus application.

**Attributes:**

- `name` (string, required): Name of the target
- `srcs` (list, optional): Java source files
- `resources` (list, optional): Resource files
- `deps` (list, optional): List of dependencies (Maven artifacts)
- `main_class` (string, optional): Main class to run
- `application_properties` (label, optional): Path to application.properties file
- `jvm_flags` (list, optional): JVM flags for running the application

## Comparison with quarkus-maven-plugin

| Maven Plugin | Bazel Rules |
|--------------|-------------|
| `mvn quarkus:dev` | `bazel run //path/to:app` |
| `mvn package` | `bazel build //path/to:app` |
| `mvn package -DskipTests` | `bazel build //path/to:app` |
| Deploy JAR | `bazel build //path/to:app_deploy.jar` |

## How `bazel run` Works

The `quarkus_app` rule creates a standard `java_binary` target that:

1. **Compiles** your Java sources with Quarkus dependencies
2. **Packages** resources including `application.properties`
3. **Sets up** the Quarkus runtime with proper JVM flags
4. **Executes** your `main` method which calls `Quarkus.run()`

When you run `bazel run //examples/hello-world`, it:
- Builds the application JAR with all dependencies
- Launches the JVM with `-Djava.util.logging.manager=org.jboss.logmanager.LogManager`
- Runs your main class which initializes Quarkus
- Starts the HTTP server on the configured port (default 8080)
- Keeps running until you press Ctrl+C

### Main Class Requirements

Your main class must:
- Have a `public static void main(String... args)` method
- Call `Quarkus.run(args)` to start the Quarkus runtime
- Optionally use `@QuarkusMain` annotation

Example:
```java
@QuarkusMain
public class QuarkusApp {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
```

## Supported Quarkus Extensions

Currently configured with:
- `quarkus-core` - Core runtime
- `quarkus-arc` - CDI (Dependency Injection)
- `quarkus-rest` - REST endpoints (formerly resteasy-reactive)
- `quarkus-rest-jackson` - JSON support
- `quarkus-vertx-http` - HTTP server
- `jboss-logmanager` - Logging

Additional extensions can be added by including them in your MODULE.bazel's maven.install() configuration.

## Roadmap

Future enhancements:
- [ ] Native image compilation support (GraalVM)
- [ ] Dev mode with hot reload
- [ ] More packaging options (uber-jar, fast-jar)
- [ ] Container image building
- [ ] Test rules (`quarkus_test`)
- [ ] More Quarkus extensions out of the box
- [ ] Configuration profiles support
- [ ] Quarkus code generation integration

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## License

Apache 2.0

## References

- [Quarkus](https://quarkus.io/)
- [Quarkus Maven Plugin](https://quarkus.io/guides/quarkus-maven-plugin)
- [rules_spring](https://github.com/salesforce/rules_spring/)
- [Bazel](https://bazel.build/)
