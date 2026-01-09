# Aether Datafixers Documentation

Welcome to the official documentation for **Aether Datafixers**, a lightweight data migration framework for the JVM.

## What is Aether Datafixers?

Aether Datafixers is a framework for migrating serialized data through schema versions using **forward patching**. Inspired by Minecraft's DataFixer Upper (DFU), it provides a simpler and more approachable API while maintaining the power and flexibility needed for complex data migrations.

### Key Features

- **Schema-Based Versioning** — Define data types per version with `Schema` and `TypeRegistry`
- **Forward Patching** — Apply `DataFix` instances sequentially to migrate data across versions
- **Format-Agnostic** — Work with any serialization format via `Dynamic<T>` and `DynamicOps<T>`
- **Codec System** — Bidirectional transformation between typed Java objects and dynamic representations
- **Profunctor Optics** — Composable, type-safe accessors for nested data (`Lens`, `Prism`, `Finder`)
- **Type Safety** — Strong typing with `TypeReference` identifiers for data routing
- **Migration Diagnostics** — Opt-in structured reports with timing, rules, and snapshots
- **JDK 17+** — Built and tested on modern LTS JVMs

---

## Quick Navigation

### Getting Started

New to Aether Datafixers? Start here:

- [Installation](getting-started/installation.md) — Add the framework to your project
- [Quick Start](getting-started/quick-start.md) — Get up and running in 5 minutes
- [Your First Migration](getting-started/your-first-migration.md) — Complete tutorial for beginners

### Core Concepts

Understand the fundamental concepts:

- [Architecture Overview](concepts/architecture-overview.md) — How the framework fits together
- [DataVersion](concepts/data-version.md) — Version identifiers for data schemas
- [TypeReference](concepts/type-reference.md) — Type identifiers for data routing
- [Schema System](concepts/schema-system.md) — Defining data structures per version
- [DataFix System](concepts/datafix-system.md) — Creating and applying migrations
- [Dynamic System](concepts/dynamic-system.md) — Format-agnostic data manipulation
- [Codec System](concepts/codec-system.md) — Encoding and decoding typed data
- [Optics](concepts/optics/index.md) — Composable data accessors

### Tutorials

Step-by-step learning guides:

- [Basic Migration](tutorials/basic-migration.md) — Your first complete migration
- [Multi-Version Migration](tutorials/multi-version-migration.md) — Chaining migrations
- [Using Codecs](tutorials/using-codecs.md) — Building custom codecs
- [RecordCodecBuilder](tutorials/record-codec-builder.md) — Composing record codecs
- [Nested Transformations](tutorials/nested-transformations.md) — Restructuring data

### How-To Guides

Practical task-oriented guides:

- [Rename a Field](how-to/rename-field.md)
- [Add a New Field](how-to/add-field.md)
- [Remove a Field](how-to/remove-field.md)
- [Transform Field Values](how-to/transform-field.md)
- [Restructure Data](how-to/restructure-data.md)
- [Debug Migrations](how-to/debug-migrations.md)
- [Use Diagnostics](how-to/use-diagnostics.md)
- [View all How-To Guides](how-to/index.md)

### Examples

Working code examples:

- [Game Data Example](examples/game-data-example/index.md) — Complete game save migration
- [User Profile Example](examples/user-profile-example.md) — User data migration
- [Configuration Example](examples/configuration-example.md) — Config file versioning

### Testing

Test your DataFixes and migrations with the Testkit module:

- [Testkit Overview](testkit/index.md) — Introduction to the testing utilities
- [Test Data Builders](testkit/test-data-builders.md) — Fluent API for creating test data
- [Custom Assertions](testkit/assertions.md) — AssertJ assertions for Dynamic, DataResult, Typed
- [DataFixTester](testkit/datafix-tester.md) — Test harness for isolated DataFix testing
- [QuickFix Factories](testkit/quick-fix.md) — Factory methods for common fix patterns
- [Mock Schemas](testkit/mock-schemas.md) — Mock schema utilities

### Command-Line Interface

Migrate and validate data files from the command line:

- [CLI Overview](cli/index.md) — Introduction to the CLI tool
- [Installation](cli/installation.md) — Building and running the CLI
- [Command Reference](cli/commands.md) — Detailed command documentation
- [Format Handlers](cli/format-handlers.md) — Custom format handler development
- [Examples](cli/examples.md) — Practical usage examples

### Schema Tools

Analyze, compare, and validate your schemas:

- [Schema Tools Overview](schema-tools/index.md) — Introduction to schema tooling
- [Schema Diffing](schema-tools/schema-diffing.md) — Compare schemas and detect changes
- [Migration Analysis](schema-tools/migration-analysis.md) — Analyze migration paths and fix coverage
- [Schema Validation](schema-tools/schema-validation.md) — Validate structure and conventions
- [Type Introspection](schema-tools/type-introspection.md) — Inspect type structures

### Advanced Topics

For experienced users:

- [Traversal Strategies](advanced/traversal-strategies.md)
- [Custom Optics](advanced/custom-optics.md)
- [Performance Optimization](advanced/performance-optimization.md)
- [Extending the Framework](advanced/extending-framework.md)

### Spring Boot Integration

Seamlessly integrate Aether Datafixers into Spring Boot applications:

- [Spring Boot Overview](spring-boot/index.md) — Introduction to the Spring Boot starter
- [Quick Start](spring-boot/getting-started.md) — Add the starter and run your first migration
- [Configuration Reference](spring-boot/configuration.md) — All configuration properties
- [MigrationService API](spring-boot/migration-service.md) — Fluent migration API
- [Multi-Domain Setup](spring-boot/multi-domain.md) — Managing multiple DataFixer instances
- [Actuator Integration](spring-boot/actuator.md) — Health indicators and endpoints
- [Metrics Integration](spring-boot/metrics.md) — Micrometer metrics for observability

### Support

- [Troubleshooting](troubleshooting/index.md)
- [Common Errors](troubleshooting/common-errors.md)
- [FAQ](troubleshooting/faq.md)
- [Glossary](appendix/glossary.md)

---

## Modules

| Module | Description |
|--------|-------------|
| `aether-datafixers-api` | Core interfaces and API contracts |
| `aether-datafixers-core` | Default implementations |
| `aether-datafixers-codec` | GsonOps, JacksonOps implementations |
| `aether-datafixers-spring-boot-starter` | Spring Boot auto-configuration, MigrationService, Actuator, Metrics |
| `aether-datafixers-cli` | Command-line interface for data migration |
| `aether-datafixers-testkit` | Testing utilities for DataFix, Schema, and migration testing |
| `aether-datafixers-schema-tools` | Schema analysis, validation, and diffing utilities |
| `aether-datafixers-examples` | Practical usage examples |
| `aether-datafixers-bom` | Bill of Materials for version management |

---

## Quick Example

```java
// 1. Define type references
public static final TypeReference PLAYER = new TypeReference("player");

// 2. Create schemas for each version
public class Schema100 extends Schema {
    @Override
    protected void registerTypes() {
        registerType(PLAYER, DSL.and(
            DSL.field("playerName", DSL.string()),
            DSL.field("xp", DSL.intType())
        ));
    }
}

// 3. Create fixes for migrations
public class RenamePlayerNameFix extends SchemaDataFix {
    @Override
    protected TypeRewriteRule makeRule(Schema input, Schema output) {
        return Rules.renameField("playerName", "name");
    }
}

// 4. Bootstrap and create the fixer
AetherDataFixer fixer = new DataFixerRuntimeFactory()
    .create(CURRENT_VERSION, new MyBootstrap());

// 5. Migrate data
TaggedDynamic updated = fixer.update(
    oldData,
    new DataVersion(100),
    fixer.currentVersion()
);
```

---

## Requirements

- **Java 17** or later
- No required runtime dependencies (Gson/Jackson are optional)

## License

MIT © Splatgames.de Software and Contributors
