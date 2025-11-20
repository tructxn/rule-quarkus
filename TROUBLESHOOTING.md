# Troubleshooting Guide

## Java Environment Issues

### Error: "Could not find or load main class @.private..."

**Symptom:**
```
Error: Could not find or load main class @.private.var.tmp._bazel_tructxn...java_argsfile
WARNING: Ignoring JAVA_HOME, because it must point to a JDK, not a JRE.
```

**Cause:**
Bazel is detecting a JRE (Java Runtime Environment) from `/Library/Internet Plug-Ins/JavaAppletPlugin.plugin` instead of a proper JDK. The JRE doesn't support the `@argsfile` syntax used by coursier for Maven dependency resolution.

**Solution:**

1. **Install JDK 21** (if not already installed):
   ```bash
   # Using Homebrew
   brew install openjdk@21

   # Or download from Oracle/Adoptium/Amazon Corretto
   ```

2. **Set JAVA_HOME to a proper JDK**:
   ```bash
   # Find available JDKs
   /usr/libexec/java_home -V

   # Set JAVA_HOME (add to ~/.zshrc or ~/.bashrc)
   export JAVA_HOME=$(/usr/libexec/java_home -v 21)

   # Verify it's a JDK, not JRE
   $JAVA_HOME/bin/javac -version  # Should work
   ```

3. **Restart your terminal** and verify:
   ```bash
   echo $JAVA_HOME
   java -version
   javac -version  # Must work (not "command not found")
   ```

4. **Clean Bazel cache and rebuild**:
   ```bash
   bazel clean --expunge
   bazel build //examples/hello-world
   ```

## Maven Dependency Resolution

### Error: "not found: https://repo1.maven.org/maven2/..."

**Cause:**
Incorrect Maven artifact coordinates or the artifact doesn't exist in Maven Central.

**Solution:**
- Verify artifact names at https://mvnrepository.com/
- Quarkus 3.x uses different artifact names than Quarkus 2.x:
  - Use `quarkus-rest` instead of `quarkus-resteasy-reactive`
  - Use `quarkus-rest-jackson` instead of `quarkus-resteasy-reactive-jackson`

## Build Performance

### Slow Maven dependency resolution

**Solution:**
Add a lock file to speed up subsequent builds:

1. Update `MODULE.bazel` to include `lock_file`:
   ```starlark
   maven.install(
       name = "maven",
       lock_file = "//:maven_install.json",
       # ... rest of config
   )
   ```

2. Generate the lock file:
   ```bash
   bazel run @maven//:pin
   ```

This creates `maven_install.json` which caches dependency resolution.

## Bazel Version

This project requires Bazel 8.0+. If you're using an older version:

```bash
# Check version
bazel version

# Update via Bazelisk (recommended)
brew install bazelisk

# Or download from https://github.com/bazelbuild/bazel/releases
```

## Running the Example

Once the Java environment is properly configured:

```bash
# Build
bazel build //examples/hello-world

# Run
bazel run //examples/hello-world

# Test the endpoint (in another terminal)
curl http://localhost:8080/hello
```

Expected output: `Hello from Quarkus with Bazel!`
