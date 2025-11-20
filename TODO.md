# TODO List for rules_quarkus

## High Priority

### 1. Fix Java Environment Setup
- [ ] Document proper JDK 21 installation steps for macOS
- [ ] Create setup script to verify Java configuration
- [ ] Add CI/CD configuration to test with proper JDK
- [ ] Resolve coursier @argsfile compatibility issue

### 2. Implement Full Quarkus Bootstrap
- [ ] Research Quarkus augmentation process
- [ ] Create `quarkus_augment` rule for build-time bytecode transformation
- [ ] Generate `quarkus-application.dat` metadata file
- [ ] Support production mode bootstrap (not IDE mode)
- [ ] Implement proper `ApplicationImpl` generated main class

### 3. Enable Annotation Scanning
- [ ] Implement `@Path` endpoint discovery
- [ ] Support `@Inject` CDI beans
- [ ] Enable `@ConfigProperty` configuration injection
- [ ] Support `@QuarkusMain` annotation properly
- [ ] Scan and register all JAX-RS resources automatically

## Medium Priority

### 4. CDI/Arc Integration
- [ ] Initialize Arc container at startup
- [ ] Support bean discovery and registration
- [ ] Implement dependency injection
- [ ] Support application scoped beans
- [ ] Enable interceptors and decorators

### 5. Configuration Management
- [ ] Implement proper `application.properties` loading
- [ ] Support profile-specific configs (application-dev.properties)
- [ ] Enable environment variable overrides
- [ ] Support `@ConfigProperty` injection
- [ ] Add configuration validation

### 6. REST Endpoint Auto-Registration
- [ ] Scan classpath for `@Path` annotated classes
- [ ] Register routes with Vert.x router automatically
- [ ] Support `@GET`, `@POST`, `@PUT`, `@DELETE`
- [ ] Handle path parameters `@PathParam`
- [ ] Support query parameters `@QueryParam`

### 7. Packaging Options
- [ ] Implement fast-jar packaging mode
- [ ] Support uber-jar (single JAR with all dependencies)
- [ ] Create thin-jar with separate lib/ directory
- [ ] Support legacy jar packaging
- [ ] Add mutable-jar for development

## Low Priority

### 8. Native Image Compilation
- [ ] Integrate GraalVM native-image
- [ ] Generate native configuration files
- [ ] Support reflection configuration
- [ ] Handle resource includes
- [ ] Create `quarkus_native_image` rule

### 9. Container Image Building
- [ ] Create `quarkus_container` rule
- [ ] Support Docker/OCI image creation
- [ ] Integrate with rules_docker/rules_oci
- [ ] Multi-stage build support
- [ ] Optimized layer caching

### 10. Dev Mode / Hot Reload
- [ ] Implement file watching
- [ ] Support incremental compilation
- [ ] Enable live coding
- [ ] Create `quarkus_dev` rule
- [ ] Support continuous testing

### 11. Testing Support
- [ ] Create `quarkus_test` rule
- [ ] Support `@QuarkusTest` annotation
- [ ] Enable test resource injection
- [ ] Support `@TestHTTPResource`
- [ ] Integrate with Bazel test framework

### 12. Additional Extensions
- [ ] Add more Quarkus extensions to MODULE.bazel
- [ ] Support quarkus-hibernate-orm
- [ ] Support quarkus-kafka-client
- [ ] Support quarkus-mongodb-client
- [ ] Support quarkus-security
- [ ] Support quarkus-oidc
- [ ] Support quarkus-reactive-messaging

### 13. Maven Lock File
- [ ] Generate and commit maven_install.json
- [ ] Enable `fail_if_repin_required = True`
- [ ] Speed up dependency resolution
- [ ] Add documentation for updating dependencies

### 14. Documentation
- [ ] Add more usage examples
- [ ] Create migration guide from Maven to Bazel
- [ ] Document all rule attributes
- [ ] Add troubleshooting for common issues
- [ ] Create video tutorial
- [ ] Write blog post about the project

### 15. Examples
- [ ] Create example with database (Hibernate)
- [ ] Add messaging example (Kafka)
- [ ] Create microservices example
- [ ] Add security/authentication example
- [ ] Create GraphQL API example
- [ ] Add gRPC example

### 16. Tooling
- [ ] Create Bazel completion for quarkus rules
- [ ] Add IDE support (IntelliJ plugin)
- [ ] Create project generator/scaffolding tool
- [ ] Add migration script from Maven projects
- [ ] Create Bazel build analyzer

### 17. Performance
- [ ] Optimize dependency resolution
- [ ] Enable remote caching
- [ ] Profile build times
- [ ] Reduce binary size
- [ ] Improve startup time

### 18. CI/CD
- [ ] Set up GitHub Actions workflow
- [ ] Add automated testing
- [ ] Create release pipeline
- [ ] Publish to Bazel Central Registry
- [ ] Set up dependency update bot

## Completed ✅

- [x] Create basic project structure
- [x] Implement initial `quarkus_app` rule
- [x] Set up Bazel 8.0+ with bzlmod
- [x] Create hello-world example
- [x] Add Vert.x-based HTTP server
- [x] Document Quarkus bootstrap limitations
- [x] Create troubleshooting guide
- [x] Add implementation notes

## Known Issues

1. **Java Environment**: Coursier fails with JRE (needs JDK)
2. **Quarkus Bootstrap**: `Quarkus.run()` expects Maven project structure
3. **No Annotation Scanning**: `@Path` classes must be manually registered
4. **No CDI**: Arc container not initialized
5. **Manual Routing**: HTTP endpoints must be coded manually

## Questions / Research Needed

- How does `quarkus-maven-plugin` perform augmentation?
- Can we reuse Quarkus build tools as Bazel actions?
- What's the best way to integrate GraalVM with Bazel?
- Should we generate a compatibility layer for existing Quarkus apps?
- How to support Quarkus extensions dynamically?

## Community

- [ ] Create CONTRIBUTING.md
- [ ] Set up issue templates
- [ ] Create discussion forum
- [ ] Add code of conduct
- [ ] Set up project roadmap
