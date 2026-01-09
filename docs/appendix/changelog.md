# Documentation Changelog

History of documentation updates. For code changes, see the main [CHANGELOG.md](../../CHANGELOG.md).

---

## Version 0.4.0

### New Section: Codec Formats

Added comprehensive documentation for all DynamicOps implementations in the codec module:

- [Codec Overview](../codec/index.md) — Introduction, format comparison, package structure
- [JSON Support](../codec/json.md) — GsonOps and JacksonJsonOps documentation
- [YAML Support](../codec/yaml.md) — SnakeYamlOps and JacksonYamlOps documentation
- [TOML Support](../codec/toml.md) — JacksonTomlOps documentation
- [XML Support](../codec/xml.md) — JacksonXmlOps documentation

### Key Codec Features Documented

- **Format-Based Package Structure**: New `codec.json`, `codec.yaml`, `codec.toml`, `codec.xml` packages
- **Multiple Implementations per Format**: JSON (Gson, Jackson), YAML (SnakeYAML, Jackson)
- **Consistent API**: All implementations follow the same `DynamicOps<T>` interface
- **Thread-Safe Singletons**: `INSTANCE` constants for all implementations
- **Custom Mapper Support**: Jackson-based implementations support custom mapper configuration
- **Migration Examples**: Complete examples for each format
- **Format Conversion**: Cross-format data conversion examples

### New DynamicOps Implementations

| Format | Implementation   | Data Type     | Library      |
|--------|------------------|---------------|--------------|
| JSON   | `GsonOps`        | `JsonElement` | Gson         |
| JSON   | `JacksonJsonOps` | `JsonNode`    | Jackson      |
| YAML   | `SnakeYamlOps`   | `Object`      | SnakeYAML    |
| YAML   | `JacksonYamlOps` | `JsonNode`    | Jackson YAML |
| TOML   | `JacksonTomlOps` | `JsonNode`    | Jackson TOML |
| XML    | `JacksonXmlOps`  | `JsonNode`    | Jackson XML  |

### New Section: Spring Boot Integration

Added comprehensive documentation for the Spring Boot Starter module:

- [Spring Boot Overview](../spring-boot/index.md) — Introduction, architecture diagram, auto-configuration classes
- [Quick Start Guide](../spring-boot/getting-started.md) — Step-by-step tutorial with complete code examples
- [Configuration Reference](../spring-boot/configuration.md) — Complete property reference (YAML and properties format)
- [MigrationService API](../spring-boot/migration-service.md) — Fluent API documentation, sync/async patterns, error handling
- [Multi-Domain Setup](../spring-boot/multi-domain.md) — Managing multiple DataFixer instances with `@Qualifier`
- [Actuator Integration](../spring-boot/actuator.md) — Health indicators, info contributors, custom endpoints, security
- [Metrics Integration](../spring-boot/metrics.md) — Micrometer metrics, PromQL queries, Grafana dashboard, alerting rules

### Key Features Documented

- **Auto-Configuration**: Automatic DataFixer bean creation from `DataFixerBootstrap` beans
- **Fluent Migration API**: `MigrationService` with builder pattern for migrations
- **Multi-Domain Support**: `DataFixerRegistry` for managing independent DataFixer instances
- **Actuator Endpoints**: `/actuator/health`, `/actuator/info`, `/actuator/datafixers`
- **Micrometer Metrics**: Success/failure counters, duration timers, version span tracking
- **Async Execution**: `CompletableFuture` support with configurable executors

### Updated Pages

- [Main README](../README.md) — Added Codec Formats and Spring Boot Integration sections, updated module table
- [Codec System](../concepts/codec-system.md) — Added links to format-specific documentation
- [Dynamic System](../concepts/dynamic-system.md) — Added table of all DynamicOps implementations
- [How-To Index](../how-to/index.md) — Added Format Integration section
- [Custom DynamicOps Tutorial](../tutorials/custom-dynamicops.md) — Added reference to built-in implementations
- [Installation Guide](../getting-started/installation.md) — Added Spring Boot module to overview and installation section

---

## Version 0.3.0

### New Section: Schema Tools Module

Added complete documentation for the new schema tools module:

- [Schema Tools Overview](../schema-tools/index.md) — Introduction, use cases, and quick start
- [Schema Diffing](../schema-tools/schema-diffing.md) — Compare schemas and detect type/field changes
- [Migration Analysis](../schema-tools/migration-analysis.md) — Analyze migration paths and fix coverage
- [Schema Validation](../schema-tools/schema-validation.md) — Validate structure and naming conventions
- [Type Introspection](../schema-tools/type-introspection.md) — Inspect type structures and extract field metadata

### New Section: CLI Module

Added complete documentation for the new CLI module:

- [CLI Overview](../cli/index.md) — Introduction, quick start, and workflow diagram
- [Installation](../cli/installation.md) — Build instructions, aliases, classpath setup
- [Command Reference](../cli/commands.md) — Detailed options for migrate, validate, info
- [Format Handlers](../cli/format-handlers.md) — Custom format handler development guide
- [Examples](../cli/examples.md) — 11 practical usage scenarios (CI/CD, Docker, scripting)

### Updated Pages

- [Main README](../README.md) — Added CLI and Schema Tools to navigation and module table
- [Installation Guide](../getting-started/installation.md) — Added CLI module to overview table

---

## Version 0.2.0

### New Section: Testkit Module

Added complete documentation for the testkit module:

- [Testkit Overview](../testkit/index.md) — Introduction to testing utilities
- [Test Data Builders](../testkit/test-data-builders.md) — Fluent API for test data
- [Custom Assertions](../testkit/assertions.md) — AssertJ assertions for Dynamic, DataResult, Typed
- [DataFixTester](../testkit/datafix-tester.md) — Test harness for isolated DataFix testing
- [QuickFix Factories](../testkit/quick-fix.md) — Factory methods for common fix patterns
- [Mock Schemas](../testkit/mock-schemas.md) — Mock schema utilities

### New How-To Guides

- [Batch Operations](../how-to/batch-operations.md) — Rename/remove multiple fields
- [Group Fields](../how-to/group-fields.md) — Grouping and flattening structures
- [Conditional Rules](../how-to/conditional-rules.md) — Conditional rule application
- [Use Diagnostics](../how-to/use-diagnostics.md) — Migration diagnostics guide
- [Test Migrations](../how-to/test-migrations.md) — Testing migrations with testkit

### Updated Pages

- [Rewrite Rules](../concepts/rewrite-rules.md) — Extended rules section
- [Concepts Index](../concepts/index.md) — Extended rules examples
- [How-To Index](../how-to/index.md) — Links to new guides
- [Glossary](glossary.md) — Testkit terminology

---

## Version 0.1.0

### Initial Documentation

Complete documentation covering all modules and concepts:

**Getting Started:**
- Installation (Maven, Gradle, BOM)
- Quick start tutorial
- First migration walkthrough

**Core Concepts:**
- Architecture overview
- DataVersion and TypeReference
- Schema and Type systems
- DataFix and DataFixer
- Dynamic and DynamicOps
- Codec system
- DSL reference
- Rewrite rules
- DataResult error handling
- Thread safety

**Optics:**
- Lens, Prism, Iso
- Affine, Traversal, Getter
- Finder for Dynamic navigation

**Tutorials:**
- Basic and multi-version migrations
- Schema inheritance
- Codec usage and RecordCodecBuilder
- Polymorphic data
- Nested transformations
- Custom DynamicOps

**How-To Guides:**
- Field operations (rename, add, remove, transform)
- Restructuring, type conversion
- Optional and unknown fields
- Composing fixes, debugging, logging
- Finder usage, bootstrap creation
- Gson integration

**Examples:**
- Game data, user profile, configuration
- Entity polymorphism

**Advanced Topics:**
- Traversal strategies, custom optics
- Recursive types, performance
- Concurrent migrations, format conversion
- Framework extension

**Reference:**
- Troubleshooting, common errors, FAQ
- Glossary, type theory primer, DFU comparison

---

## Contributing to Documentation

To suggest documentation improvements:
1. Open an issue on GitHub
2. Describe the improvement
3. Provide examples if applicable
