# Implementation Notes

## How Quarkus Running Works with Bazel

### The Challenge

Quarkus applications require a specific runtime setup that differs from regular Java applications. The main challenges were:

1. **Main Method Requirement**: Quarkus needs a proper `main(String... args)` method
2. **Runtime Dependencies**: Specific Quarkus runtime JARs must be on the classpath
3. **JVM Configuration**: JBoss LogManager must be configured as the logging manager
4. **CDI Initialization**: Arc (Quarkus CDI) needs to bootstrap properly

### The Solution

#### 1. Main Class Structure

The main class must follow this pattern:

```java
@QuarkusMain
public class QuarkusApp {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
```

**Why this works:**
- `public static void main(String... args)` - Standard Java entry point
- `Quarkus.run(args)` - Initializes the Quarkus runtime
- `@QuarkusMain` - Tells Quarkus this is the main entry point

**What doesn't work:**
```java
// ❌ Missing main method
@QuarkusMain
public class QuarkusApp implements QuarkusApplication {
    public int run(String... args) { ... }
}
```

#### 2. quarkus_app Rule Implementation

The `quarkus_app.bzl` rule handles:

**a) Core Dependencies**
```python
quarkus_deps = [
    "@maven//:io_quarkus_quarkus_core",
    "@maven//:io_quarkus_quarkus_arc",
    "@maven//:jakarta_enterprise_jakarta_enterprise_cdi_api",
    "@maven//:org_jboss_logging_jboss_logging",
    "@maven//:org_jboss_logmanager_jboss_logmanager",
]
```

**b) JVM Flags**
```python
default_jvm_flags = [
    "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
]
```

This is **critical** - without this flag, Quarkus logging won't work properly.

**c) Java Binary Creation**
```python
native.java_binary(
    name = name,
    main_class = main_class,
    runtime_deps = [":" + name + "_lib"],
    jvm_flags = all_jvm_flags,
)
```

### How `bazel run` Works

When you execute `bazel run //examples/hello-world`:

```
1. Bazel resolves dependencies
   └─> Fetches Maven artifacts (quarkus-core, quarkus-arc, etc.)

2. Compiles Java sources
   └─> javac compiles *.java files with Quarkus on classpath

3. Packages resources
   └─> Includes application.properties in JAR

4. Executes java_binary
   └─> Runs: java -Djava.util.logging.manager=... \
              -cp <all-jars> \
              com.example.QuarkusApp

5. Main method executes
   └─> Calls Quarkus.run(args)

6. Quarkus initializes
   ├─> Starts Arc (CDI container)
   ├─> Scans for @Path annotated classes
   ├─> Initializes Vert.x HTTP server
   └─> Binds to port 8080

7. Application runs
   └─> HTTP server accepts requests
   └─> Waits for Ctrl+C
```

### Comparison with Maven

| Maven (quarkus-maven-plugin) | Bazel (rules_quarkus) |
|------------------------------|----------------------|
| `mvn quarkus:dev` | `bazel run //path:app` |
| Maven compiles & augments bytecode | Bazel compiles normally |
| Maven generates `*-runner.jar` | Bazel generates standard JAR |
| Uses Quarkus classloader | Uses standard Java classloader |
| Hot reload enabled | No hot reload (yet) |

### Key Differences from Maven Plugin

**Maven quarkus-maven-plugin does:**
- Bytecode augmentation at build time
- Generates runner classes
- Creates optimized fast-jar packaging
- Provides dev mode with hot reload
- Native image compilation via GraalVM

**Our Bazel rules currently do:**
- Standard Java compilation
- Standard JAR packaging
- Basic runtime initialization
- Direct Quarkus.run() invocation

### Future Improvements

To fully match `quarkus-maven-plugin` functionality:

1. **Augmentation Phase**
   - Use Quarkus Maven plugin JARs as build tools
   - Run augmentation on compiled classes
   - Generate optimized bytecode

2. **Fast-JAR Packaging**
   - Create multi-layered JAR structure
   - Separate app code from dependencies
   - Faster startup time

3. **Native Compilation**
   - Integrate GraalVM native-image
   - Generate native executables
   - Support substrate VM configuration

4. **Dev Mode**
   - File watching for hot reload
   - Incremental compilation
   - Live coding support

### Maven Artifacts Used

```starlark
"io.quarkus:quarkus-core:3.17.4"              # Core runtime
"io.quarkus:quarkus-arc:3.17.4"               # CDI implementation
"io.quarkus:quarkus-rest:3.17.4"              # REST framework
"io.quarkus:quarkus-rest-jackson:3.17.4"      # JSON serialization
"io.quarkus:quarkus-vertx-http:3.17.4"        # HTTP server
"jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0"
"jakarta.ws.rs:jakarta.ws.rs-api:4.0.0"       # JAX-RS API
"org.jboss.logging:jboss-logging:3.6.1.Final"
"org.jboss.logmanager:jboss-logmanager:3.0.6.Final"
```

### REST Endpoint Example

```java
@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus with Bazel!";
    }
}
```

**How it works:**
1. Arc scans for `@Path` annotations
2. Registers endpoint with Vert.x router
3. Maps HTTP GET /hello to `hello()` method
4. Returns plain text response

### Testing the Application

```bash
# Terminal 1: Start the server
bazel run //examples/hello-world

# Terminal 2: Test the endpoint
curl http://localhost:8080/hello
# Output: Hello from Quarkus with Bazel!
```

### Debugging

To debug the application:

```bash
bazel run //examples/hello-world -- \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
```

Then attach your IDE debugger to port 5005.

### Summary

The `bazel run` support works by:
1. Creating a proper Java main class with `Quarkus.run()`
2. Including all necessary Quarkus runtime dependencies
3. Setting JBoss LogManager as the logging manager
4. Using `java_binary` to create an executable target
5. Letting Quarkus initialize normally at runtime

This provides basic Quarkus functionality through Bazel, similar to `rules_spring` for Spring Boot.
