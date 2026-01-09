# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.4.0] - 2026-01-09

### Breaking Changes

#### Package Restructuring in `aether-datafixers-codec`

The codec module has been restructured to use a format-first package organization. This is a **breaking change** that requires updating import statements.

**Old Package Structure:**
```
de.splatgames.aether.datafixers.codec.gson.GsonOps
de.splatgames.aether.datafixers.codec.jackson.JacksonOps
```

**New Package Structure:**
```
de.splatgames.aether.datafixers.codec.json.gson.GsonOps
de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps
de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps
de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps
de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps
de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps
```

**Migration Steps:**
1. Update imports from `codec.gson.GsonOps` to `codec.json.gson.GsonOps`
2. Update imports from `codec.jackson.JacksonOps` to `codec.json.jackson.JacksonJsonOps`
3. Rename `JacksonOps` references to `JacksonJsonOps`

### Added

#### Multi-Format DynamicOps Implementations (`aether-datafixers-codec`)

New DynamicOps implementations for YAML, TOML, and XML formats:

**YAML Support:**
- `SnakeYamlOps` — Uses native Java types (`Map`, `List`, primitives) via SnakeYAML 2.x
- `JacksonYamlOps` — Uses `JsonNode` via Jackson YAML dataformat module

**TOML Support:**
- `JacksonTomlOps` — Uses `JsonNode` via Jackson TOML dataformat module
- Note: TOML requires top-level tables and doesn't support null values

**XML Support:**
- `JacksonXmlOps` — Uses `JsonNode` via Jackson XML dataformat module
- Note: XML requires a root element and has different structural semantics

**New Dependencies (all optional):**
```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.2</version>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-toml</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
    <optional>true</optional>
</dependency>
```

#### Spring Boot Starter Module (`aether-datafixers-spring-boot-starter`)

New module providing seamless Spring Boot integration with auto-configuration, fluent migration API, and observability features.

**Auto-Configuration (`spring.autoconfigure`):**
- `AetherDataFixersAutoConfiguration` — Main entry point coordinating all sub-configurations
- `DynamicOpsAutoConfiguration` — Auto-configures `GsonOps` and `JacksonOps` beans based on classpath
- `DataFixerAutoConfiguration` — Creates `AetherDataFixer` beans from `DataFixerBootstrap` definitions
- `MigrationServiceAutoConfiguration` — Configures the `MigrationService` with metrics integration
- `ActuatorAutoConfiguration` — Configures health indicators, info contributors, and custom endpoints
- Conditional activation via `aether.datafixers.enabled` property (default: `true`)
- Version resolution from properties, bootstrap `CURRENT_VERSION` constant, or global default

**Configuration Properties (`spring.config`):**
- `AetherDataFixersProperties` — Root configuration with `aether.datafixers.*` prefix
- `DataFixerDomainProperties` — Per-domain configuration (version, primary, description)
- `DynamicOpsFormat` — Enum for selecting default serialization format:
  - `GSON` — JSON via Google Gson
  - `JACKSON` — JSON via Jackson Databind
  - `JACKSON_YAML` — YAML via Jackson dataformat
  - `SNAKEYAML` — YAML via SnakeYAML (native Java types)
  - `JACKSON_TOML` — TOML via Jackson dataformat
  - `JACKSON_XML` — XML via Jackson dataformat
- `ActuatorProperties` — Control schema/fix detail exposure in actuator responses
- `MetricsProperties` — Configure timing, counting, and domain tag name

**Migration Service (`spring.service`):**
- `MigrationService` — High-level interface with fluent builder API
- `MigrationService.MigrationRequestBuilder` — Builder for configuring migrations
- `DefaultMigrationService` — Thread-safe implementation with metrics integration
- `MigrationResult` — Immutable result object with success/failure, data, versions, duration, error
- Fluent API: `.migrate(data).from(version).to(version).execute()`
- Domain selection: `.usingDomain("game")` for multi-domain setups
- Latest version resolution: `.toLatest()` resolves at execution time
- Async execution: `.executeAsync()` returns `CompletableFuture<MigrationResult>`
- Custom DynamicOps: `.withOps(ops)` for custom serialization

**Multi-Domain Support (`spring.autoconfigure`):**
- `DataFixerRegistry` — Thread-safe registry for managing multiple DataFixer instances
- Support for `@Qualifier` annotated bootstrap beans
- `createQualifiedFixer()` factory method for domain-specific DataFixer creation
- Domain availability checking via `hasDomain()` and `getAvailableDomains()`
- Default domain ("default") for single-bootstrap setups

**Actuator Integration (`spring.actuator`):**
- `DataFixerHealthIndicator` — Reports UP/DOWN/UNKNOWN based on DataFixer operational status
- `DataFixerInfoContributor` — Adds `aether-datafixers` section to `/actuator/info`
- `DataFixerEndpoint` — Custom endpoint at `/actuator/datafixers` with domain details
- Per-domain health status and version information
- Domain-specific endpoint: `/actuator/datafixers/{domain}`
- Fail-fast on first domain error with detailed error message

**Micrometer Metrics (`spring.metrics`):**
- `MigrationMetrics` — Records migration metrics using Micrometer
- `aether.datafixers.migrations.success` — Counter for successful migrations (tagged by domain)
- `aether.datafixers.migrations.failure` — Counter for failed migrations (tagged by domain, error_type)
- `aether.datafixers.migrations.duration` — Timer for migration execution time
- `aether.datafixers.migrations.version.span` — Distribution summary of version spans
- Automatic metric recording in `DefaultMigrationService`
- Thread-safe meter caching per domain

### Documentation

#### Codec Formats Documentation
- Added new `docs/codec/` section with comprehensive format documentation
- [Codec Overview](docs/codec/index.md) — Format comparison, package structure, dependency guide
- [JSON Support](docs/codec/json.md) — GsonOps and JacksonJsonOps usage, examples, comparison
- [YAML Support](docs/codec/yaml.md) — SnakeYamlOps and JacksonYamlOps usage, examples, comparison
- [TOML Support](docs/codec/toml.md) — JacksonTomlOps usage, configuration file examples
- [XML Support](docs/codec/xml.md) — JacksonXmlOps usage, XML-to-JsonNode mapping
- Updated [Dynamic System](docs/concepts/dynamic-system.md) with all DynamicOps implementations table
- Updated [Codec System](docs/concepts/codec-system.md) with links to format-specific docs
- Updated [How-To Index](docs/how-to/index.md) with Format Integration section
- Updated [Custom DynamicOps Tutorial](docs/tutorials/custom-dynamicops.md) with built-in implementations reference

#### Spring Boot Integration Documentation
- Added comprehensive Spring Boot Integration documentation
- Quick Start Guide with complete code examples
- Configuration Reference for all `aether.datafixers.*` properties
- MigrationService API documentation with sync/async patterns
- Multi-Domain Setup guide with `@Qualifier` examples
- Actuator Integration guide with security recommendations
- Metrics Integration guide with PromQL queries, Grafana dashboard, and alerting rules
- Updated main documentation with Spring Boot module links

---

## [0.3.0] - 2026-01-08

### Added

#### Schema Tools Module (`aether-datafixers-schema-tools`)

New module for schema analysis, validation, and migration coverage checking.

**Schema Diffing (`schematools.diff`):**
- `SchemaDiffer` — Fluent API for comparing two schemas
- `SchemaDiff` — Immutable result with added/removed/common types
- `TypeDiff` — Field-level changes for types present in both schemas
- `FieldDiff` — Individual field change (ADDED, REMOVED, MODIFIED, UNCHANGED)
- `DiffKind` — Enumeration of change types
- Optional field-level diffing via `includeFieldLevel(true)`
- Type filtering via `ignoreTypes(...)`

**Migration Analysis (`schematools.analysis`):**
- `MigrationAnalyzer` — Fluent API for analyzing migration paths
- `MigrationPath` — Complete migration sequence with all steps
- `MigrationStep` — Single version transition with optional DataFix and SchemaDiff
- `FixCoverage` — Analysis result showing fix coverage for schema changes
- `CoverageGap` — Represents a schema change without corresponding DataFix
- Coverage gap reasons: TYPE_ADDED, TYPE_REMOVED, TYPE_MODIFIED, FIELD_ADDED, FIELD_REMOVED, FIELD_TYPE_CHANGED
- Orphan fix detection (fixes without schema changes)

**Schema Validation (`schematools.validation`):**
- `SchemaValidator` — Fluent API for validating schemas
- `ValidationResult` — Immutable collection of validation issues
- `ValidationIssue` — Single issue with severity, code, message, location, context
- `IssueSeverity` — ERROR, WARNING, INFO levels
- `StructureValidator` — Validates schema structure (cycles, version ordering, parent chains)
- `ConventionChecker` — Validates naming conventions for types, fields, classes
- `ConventionRules` — Configurable naming rules (STRICT, RELAXED, NONE, or custom)
- Schema class prefix/suffix validation (e.g., "Schema" prefix for Schema100, Schema200)
- Fix class prefix/suffix validation (e.g., "Fix" suffix for PlayerNameFix)
- Predefined patterns for snake_case, camelCase
- Custom validators via `customTypeValidator()` and `customFieldValidator()`

**Type Introspection (`schematools.introspection`):**
- `TypeIntrospector` — Utility for analyzing type structures
- `TypeStructure` — Normalized, comparable representation of a Type
- `FieldInfo` — Field metadata (name, path, optionality, type)
- `TypeKind` — Classification (PRIMITIVE, LIST, OPTIONAL, PRODUCT, SUM, FIELD, etc.)
- Recursive field extraction with hierarchical paths
- Structural equality comparison

#### CLI Module (`aether-datafixers-cli`)

New command-line interface for data migration without writing Java code.

**Commands:**
- `migrate` — Migrate data files from one schema version to another
- `validate` — Check if files need migration without modifying them
- `info` — Display version info, available formats, and bootstrap details

**Core Features:**
- Batch processing of multiple files with shell glob expansion
- Auto-detection of source version from configurable data field path
- In-place file modification with automatic `.bak` backup
- Output to stdout, file, or directory
- Pretty-printed or compact JSON output
- Migration reports in text or JSON format
- Verbose mode with detailed progress and stack traces
- Fail-fast or continue-on-error modes
- CI/CD friendly exit codes (0=success, 1=error, 2=migration needed)

**Format Handler System:**
- `FormatHandler<T>` — SPI for pluggable serialization formats
- `FormatRegistry` — ServiceLoader-based handler discovery
- `json-gson` — JSON format using Google Gson (default)
- `json-jackson` — JSON format using Jackson Databind

**Utilities:**
- `BootstrapLoader` — Reflective loading of DataFixerBootstrap implementations
- `VersionExtractor` — Extract version from nested JSON paths (dot notation)
- `ReportFormatter` — Text and JSON migration report formatting
- `TextReportFormatter` — Human-readable single-line reports
- `JsonReportFormatter` — Machine-readable JSON reports

**Exceptions:**
- `BootstrapLoadException` — Bootstrap class loading failures
- `FormatParseException` — Input parsing failures

### Documentation

- Added comprehensive Schema Tools documentation (diffing, analysis, validation, introspection)
- Added CLI module documentation (commands, format handlers, examples)
- Updated glossary with Schema Tools terminology
- Updated installation guide with new modules

---

## [0.2.0] - 2026-01-07

### Added

#### Testkit Module (`aether-datafixers-testkit`)
- **TestData** — Fluent builders for creating test data (`TestData.gson().object()...`)
- **AetherAssertions** — Custom AssertJ assertions for `Dynamic`, `DataResult`, `Typed`
- **DataFixTester** — Test harness for isolated DataFix testing with fluent API
- **MigrationTester** — Test harness for full migration chain testing
- **SchemaTester** — Test harness for schema validation
- **QuickFix** — Factory methods for common fix patterns (rename, add, remove, transform)
- **MockSchemas** — Mock schema utilities for testing
- **RecordingContext** — Context that records warnings for test verification

#### Migration Diagnostics
- **DiagnosticContext** — Opt-in diagnostic context for capturing migration reports
- **DiagnosticOptions** — Configurable options (snapshots, rule details, pretty print)
- **MigrationReport** — Structured report with timing, applied fixes, and touched types
- **FixExecution** — Per-fix execution details with before/after snapshots
- **RuleApplication** — Per-rule application details

#### Extended Rewrite Rules
- `Rules.renameFields(ops, map)` — Batch rename multiple fields
- `Rules.removeFields(ops, fields...)` — Batch remove multiple fields
- `Rules.groupFields(ops, target, fields...)` — Group flat fields into nested object
- `Rules.flattenField(ops, field)` — Flatten nested object to root level
- `Rules.moveField(ops, source, target)` — Move field between paths
- `Rules.copyField(ops, source, target)` — Copy field to new location
- `Rules.transformFieldAt(ops, path, fn)` — Transform at nested path
- `Rules.renameFieldAt(ops, path, newName)` — Rename at nested path
- `Rules.removeFieldAt(ops, path)` — Remove at nested path
- `Rules.addFieldAt(ops, path, value)` — Add at nested path
- `Rules.ifFieldExists(ops, field, rule)` — Conditional on field existence
- `Rules.ifFieldMissing(ops, field, rule)` — Conditional on field absence
- `Rules.ifFieldEquals(ops, field, value, rule)` — Conditional on field value

#### High-Performance APIs
- **BatchTransform** — Builder for batching multiple field operations
- `Rules.batch(ops, builder)` — Apply multiple operations in single encode/decode cycle
- `Rules.conditionalTransform(ops, predicate, transform)` — Single-pass conditional transform
- `Rules.ifFieldExists(ops, field, transform)` — Single-pass version (Function overload)
- `Rules.ifFieldMissing(ops, field, transform)` — Single-pass version (Function overload)
- `Rules.ifFieldEquals(ops, field, value, transform)` — Single-pass version (Function overload)

### Changed

#### Performance Optimizations
- Path parsing now uses character-based parsing instead of regex with memoization cache
- `DataFixRegistry.getFixes()` pre-allocates result list and avoids second copy
- `DataFixerImpl` removes redundant validation in hot path (moved to registration time)
- Fix version ordering validated once at registration instead of per-application

### Documentation
- Added comprehensive how-to guides for all extended rules
- Added migration diagnostics usage guide
- Updated quick reference with new APIs (including Testkit module)

---

## [0.1.0] - 2025-12-22

### 🎉 Initial Release

- First stable release of the project.
