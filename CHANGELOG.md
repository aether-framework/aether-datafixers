# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0-rc.1] - 2026-05-12

### Release Candidate

First **release candidate** for the upcoming 1.0.0 GA. The public API has been frozen since v0.5.0 and is now hardened with field-level diagnostics, pervasive null-safety annotations, modernized Javadoc, and a stress / chaos testing framework. Deprecated APIs announced in v0.5.0 have been removed.

### Breaking Changes

The deprecated wrappers announced in v0.5.0 have been **removed**:

| Removed                                                           | Replacement                                                                                                          |
|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `de.splatgames.aether.datafixers.codec.gson.GsonOps`              | `de.splatgames.aether.datafixers.codec.json.gson.GsonOps`                                                            |
| `de.splatgames.aether.datafixers.codec.jackson.JacksonOps`        | `codec.json.jackson.JacksonJsonOps` (JSON) or the format-specific classes (`JacksonYamlOps`, `JacksonTomlOps`, `JacksonXmlOps`) |
| `TestData.jackson()`                                              | `TestData.jacksonJson()`                                                                                             |

All other public API is source-compatible with v0.5.0.

### Added

#### Field-Level Diagnostics (`aether-datafixers-api`, `aether-datafixers-core`)

- New `FieldOperation` record capturing structured per-field metadata (type, field name, operation type, type reference)
- New `FieldOperationType` enum (`RENAME`, `ADD`, `REMOVE`, `TRANSFORM`, ...)
- New `FieldAwareRule` marker interface implemented by every field operation in `Rules`
- Field metadata propagates through `seq`, `seqAll`, `choice`, `batch`, `topDown`, and `bottomUp` combinators
- `MigrationReport` and `FixExecution` expose field operations per executed rule
- CLI `--diagnostics` flag emits field-level details in text and JSON reports
- Spring Boot Actuator `/actuator/datafixers` endpoint reports field operations per execution
- Static field-level coverage analysis available via `MigrationAnalyzer`

#### JMH Benchmark Suite (`aether-datafixers-benchmarks`)

- New module with reproducible JMH benchmarks for the core migration paths
- Shaded benchmark JAR built via `mvn package`; run via `./run-benchmarks.sh`
- Baseline results committed under `benchmark-results/`

#### Stress & Chaos Tests (`aether-datafixers-functional-tests`)

- `HighConcurrencyMigrationStressIT` — 100+ concurrent migration threads
- `SustainedLoadMigrationStressIT` — minutes-long sustained throughput
- `MixedRegistryAccessStressIT` — concurrent registry read/write workloads
- `MemoryPressureStressIT` — GC pressure and memory-leak detection
- `RandomDelaysChaosIT` — random delays injected into fix execution
- `RandomFailuresChaosIT` — random failure injection in fix execution
- Reusable harness: `StressTestConfig`, `StressTestMetrics`, `ThreadOrchestrator`, `ChaosInjector`, `FailingDataFix`

#### Documentation

- Migration guide `docs/migration/v0.5-to-v1.0.md` covering breaking changes, automated migration patterns, and troubleshooting
- Operational documentation under `docs/operations/` (debugging guide, error scenarios, monitoring & alerting, recovery procedures)
- Expanded `Rules` Javadoc with comprehensive field-aware API documentation
- Field-level diagnostics documentation and examples

#### Project Hygiene

- `DCO` file added; PRs now sign off under the Developer Certificate of Origin
- `CONTRIBUTORS.md` added to recognize project contributors
- `AI_USAGE.md` added documenting AI usage guidelines
- Hardened devcontainer for Java / Python / Claude Code sandboxing
- Aether Style Guard integrated into PR and push CI workflows; opt-in `styleguard` profile wired up on every module
- Automated PR retargeting from `main` to `develop` via GitHub workflow
- Dependabot configured to target `develop` directly

### Changed

- Comprehensive Javadoc modernization across every module for 1.0.0 readiness
- Pervasive `@NotNull` / `@Nullable` annotations applied across the public API and internals
- `MigrationResult` now implements `equals` / `hashCode`
- `Finder.index()` validates non-negative indices at construction (throws `IllegalArgumentException`)
- `Typed.encodeAndGet` now returns `DataResult` with structured error reporting for missing paths
- Stricter state validation in migration and fix handling
- CLI command descriptions refined; error classification improved
- `DataFixerRuntimeFactory` now logs schema registration via SLF4J
- `SchemaValidator` dynamically determines max schema version with stricter error handling
- `fixTypeEverywhere` simplified by directly returning `Optional` from `DataResult#result`

### Fixed

- 100+ targeted bug fixes across `aether-datafixers-api`, `-codec`, `-core`, `-cli`, `-testkit`, `-schema-tools`, and `-spring-boot-starter` (see merged bugfix batches in PRs #223–#274)
- `Type.equals` no longer treats objects with same `ref`/`describe` but different `codec` as equal
- `Finder.index()` rejects negative indices at construction
- Robustness of dynamic key extraction in `Rules` improved (null checks, streamlined `asString` handling)
- Javadoc typos in `TypeDiff`, `DataVersion`, optics, and format classes corrected
- `RandomFailuresChaosIT` updated to handle `FixException` instead of `ChaosException`
- `DataFixerEndpointTest` now uses a real `FixExecution` record instead of mocks

### Removed

- Deprecated `de.splatgames.aether.datafixers.codec.gson.GsonOps` wrapper class
- Deprecated `de.splatgames.aether.datafixers.codec.jackson.JacksonOps` wrapper class
- Deprecated `TestData.jackson()` factory method
- Unused `compose` method from reversed `Iso` implementation
- Unnecessary checked exceptions from `ConventionRules.Builder` and `DiagnosticOptions.Builder` constructors

### Documentation Index

- See [Documentation Changelog](docs/appendix/changelog.md) for documentation-specific changes
- See [Migration Guide](docs/migration/v0.5-to-v1.0.md) for the v0.5.x → 1.0.0 upgrade walkthrough

---

## [0.5.0] - 2026-01-16

### API Freeze

This release marks the **API freeze** for Aether Datafixers. The public API is now stable and no breaking changes are expected before v1.0.0.

### Added

#### SchemaValidator Fix Coverage Integration (`aether-datafixers-schema-tools`)

The `SchemaValidator.validateFixCoverage()` method now performs actual coverage analysis using `MigrationAnalyzer`:

- **Full MigrationAnalyzer integration** - Detects missing DataFixes for schema changes
- **Automatic version range detection** - Scans the entire schema registry to determine version bounds
- **Field-level coverage gaps** - Reports issues for added, removed, or modified fields without fixes
- **Detailed context** - Issues include type, field name, version range, and gap reason

**Usage:**
```java
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateFixCoverage()
    .validate();

for (ValidationIssue issue : result.warnings()) {
    System.out.println(issue.message());
    // "Missing DataFix for type 'player' field 'health': FIELD_ADDED"
}
```

#### MigrationService.withOps() Implementation (`aether-datafixers-spring-boot-starter`)

The Spring Boot `MigrationService` now fully supports custom `DynamicOps` for format conversion:

- **Format conversion during migration** - Convert input data to specified format before migration
- **Seamless API integration** - Works with the existing fluent builder API
- **All formats supported** - Gson, Jackson JSON, YAML (both), TOML, and XML

**Usage:**
```java
// Convert Gson data to Jackson YAML format during migration
MigrationResult result = migrationService
    .migrate(gsonData)
    .from(100)
    .to(200)
    .withOps(JacksonYamlOps.INSTANCE)
    .execute();

// Result data is now in Jackson YAML format
Dynamic<JsonNode> yamlResult = (Dynamic<JsonNode>) result.getData();
```

#### Functional Tests Module (`aether-datafixers-functional-tests`)

New module with comprehensive end-to-end and integration tests:

- **Cross-format migration tests** - Verify migrations work identically across all DynamicOps implementations
- **Error recovery tests** - Test graceful handling of malformed data and fix failures
- **Field transformation E2E tests** - Validate rename, add, remove, and group operations

**Running:**
```bash
mvn verify -Pit
```

#### CLI Format Handlers for YAML, TOML, and XML (`aether-datafixers-cli`)

Extended the CLI with four new built-in format handlers:

**YAML Support:**
- `YamlSnakeYamlFormatHandler` - Format ID `yaml-snakeyaml`, uses SnakeYAML with native Java types
- `YamlJacksonFormatHandler` - Format ID `yaml-jackson`, uses Jackson YAML with JsonNode

**TOML Support:**
- `TomlJacksonFormatHandler` - Format ID `toml-jackson`, uses Jackson TOML with JsonNode

**XML Support:**
- `XmlJacksonFormatHandler` - Format ID `xml-jackson`, uses Jackson XML with JsonNode

**Usage Examples:**
```bash
# Migrate YAML files
aether-cli migrate --format yaml-snakeyaml --to 200 --type config input.yaml

# Migrate TOML configuration
aether-cli migrate --format toml-jackson --to 200 --type config config.toml

# Migrate XML data
aether-cli migrate --format xml-jackson --to 200 --type player data.xml
```

**New CLI Dependencies:**
```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-toml</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

#### Testkit Format Factory Methods (`aether-datafixers-testkit`)

Added new factory methods to `TestData` for all supported formats:

| Method            | Format | Data Type     | DynamicOps Used     |
|-------------------|--------|---------------|---------------------|
| `jacksonJson()`   | JSON   | `JsonNode`    | `JacksonJsonOps`    |
| `snakeYaml()`     | YAML   | `Object`      | `SnakeYamlOps`      |
| `jacksonYaml()`   | YAML   | `JsonNode`    | `JacksonYamlOps`    |
| `jacksonToml()`   | TOML   | `JsonNode`    | `JacksonTomlOps`    |
| `jacksonXml()`    | XML    | `JsonNode`    | `JacksonXmlOps`     |

**Usage:**
```java
// YAML test data with SnakeYAML
Dynamic<Object> yamlData = TestData.snakeYaml().object()
    .put("name", "test")
    .put("value", 42)
    .build();

// TOML test data
Dynamic<JsonNode> tomlData = TestData.jacksonToml().object()
    .put("key", "value")
    .build();

// XML test data
Dynamic<JsonNode> xmlData = TestData.jacksonXml().object()
    .put("element", "content")
    .build();
```

**New Testkit Dependencies:**
```xml
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-toml</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

#### Comprehensive Format Handler Tests (`aether-datafixers-cli`)

Added test classes for all new format handlers with full coverage:

- `YamlSnakeYamlFormatHandlerTest` - 21 tests for SnakeYAML handler
- `YamlJacksonFormatHandlerTest` - 20 tests for Jackson YAML handler
- `TomlJacksonFormatHandlerTest` - 19 tests for Jackson TOML handler
- `XmlJacksonFormatHandlerTest` - 20 tests for Jackson XML handler

Tests cover parsing, serialization, error handling, and round-trip consistency.

### Deprecated

#### `TestData.jackson()` (`aether-datafixers-testkit`)

The `TestData.jackson()` method is deprecated and will be removed in version 1.0.0.

**Reason:** With the addition of multiple Jackson-based format handlers (JSON, YAML, TOML, XML), explicit format naming is required for clarity.

**Migration:**
```java
// Before (deprecated)
Dynamic<JsonNode> data = TestData.jackson().object().build();

// After
Dynamic<JsonNode> data = TestData.jacksonJson().object().build();
```

### Documentation

- Updated [Format Handlers](docs/cli/format-handlers.md) with all new built-in handlers
- Updated [Test Data Builders](docs/testkit/test-data-builders.md) with new factory methods and deprecation notice
- Added version 0.5.0 section to [Documentation Changelog](docs/appendix/changelog.md)

---

## [0.4.0] - 2026-01-10

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
- `SnakeYamlOps` - Uses native Java types (`Map`, `List`, primitives) via SnakeYAML 2.x
- `JacksonYamlOps` - Uses `JsonNode` via Jackson YAML dataformat module

**TOML Support:**
- `JacksonTomlOps` - Uses `JsonNode` via Jackson TOML dataformat module
- Note: TOML requires top-level tables and doesn't support null values

**XML Support:**
- `JacksonXmlOps` - Uses `JsonNode` via Jackson XML dataformat module
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
- `AetherDataFixersAutoConfiguration` - Main entry point coordinating all sub-configurations
- `DynamicOpsAutoConfiguration` - Auto-configures `GsonOps` and `JacksonOps` beans based on classpath
- `DataFixerAutoConfiguration` - Creates `AetherDataFixer` beans from `DataFixerBootstrap` definitions
- `MigrationServiceAutoConfiguration` - Configures the `MigrationService` with metrics integration
- `ActuatorAutoConfiguration` - Configures health indicators, info contributors, and custom endpoints
- Conditional activation via `aether.datafixers.enabled` property (default: `true`)
- Version resolution from properties, bootstrap `CURRENT_VERSION` constant, or global default

**Configuration Properties (`spring.config`):**
- `AetherDataFixersProperties` - Root configuration with `aether.datafixers.*` prefix
- `DataFixerDomainProperties` - Per-domain configuration (version, primary, description)
- `DynamicOpsFormat` - Enum for selecting default serialization format:
  - `GSON` - JSON via Google Gson
  - `JACKSON` - JSON via Jackson Databind
  - `JACKSON_YAML` - YAML via Jackson dataformat
  - `SNAKEYAML` - YAML via SnakeYAML (native Java types)
  - `JACKSON_TOML` - TOML via Jackson dataformat
  - `JACKSON_XML` - XML via Jackson dataformat
- `ActuatorProperties` - Control schema/fix detail exposure in actuator responses
- `MetricsProperties` - Configure timing, counting, and domain tag name

**Migration Service (`spring.service`):**
- `MigrationService` - High-level interface with fluent builder API
- `MigrationService.MigrationRequestBuilder` - Builder for configuring migrations
- `DefaultMigrationService` - Thread-safe implementation with metrics integration
- `MigrationResult` - Immutable result object with success/failure, data, versions, duration, error
- Fluent API: `.migrate(data).from(version).to(version).execute()`
- Domain selection: `.usingDomain("game")` for multi-domain setups
- Latest version resolution: `.toLatest()` resolves at execution time
- Async execution: `.executeAsync()` returns `CompletableFuture<MigrationResult>`
- Custom DynamicOps: `.withOps(ops)` for custom serialization

**Multi-Domain Support (`spring.autoconfigure`):**
- `DataFixerRegistry` - Thread-safe registry for managing multiple DataFixer instances
- Support for `@Qualifier` annotated bootstrap beans
- `createQualifiedFixer()` factory method for domain-specific DataFixer creation
- Domain availability checking via `hasDomain()` and `getAvailableDomains()`
- Default domain ("default") for single-bootstrap setups

**Actuator Integration (`spring.actuator`):**
- `DataFixerHealthIndicator` - Reports UP/DOWN/UNKNOWN based on DataFixer operational status
- `DataFixerInfoContributor` - Adds `aether-datafixers` section to `/actuator/info`
- `DataFixerEndpoint` - Custom endpoint at `/actuator/datafixers` with domain details
- Per-domain health status and version information
- Domain-specific endpoint: `/actuator/datafixers/{domain}`
- Fail-fast on first domain error with detailed error message

**Micrometer Metrics (`spring.metrics`):**
- `MigrationMetrics` - Records migration metrics using Micrometer
- `aether.datafixers.migrations.success` - Counter for successful migrations (tagged by domain)
- `aether.datafixers.migrations.failure` - Counter for failed migrations (tagged by domain, error_type)
- `aether.datafixers.migrations.duration` - Timer for migration execution time
- `aether.datafixers.migrations.version.span` - Distribution summary of version spans
- Automatic metric recording in `DefaultMigrationService`
- Thread-safe meter caching per domain

### Documentation

#### Codec Formats Documentation
- Added new `docs/codec/` section with comprehensive format documentation
- [Codec Overview](docs/codec/index.md) - Format comparison, package structure, dependency guide
- [JSON Support](docs/codec/json.md) - GsonOps and JacksonJsonOps usage, examples, comparison
- [YAML Support](docs/codec/yaml.md) - SnakeYamlOps and JacksonYamlOps usage, examples, comparison
- [TOML Support](docs/codec/toml.md) - JacksonTomlOps usage, configuration file examples
- [XML Support](docs/codec/xml.md) - JacksonXmlOps usage, XML-to-JsonNode mapping
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
- `SchemaDiffer` - Fluent API for comparing two schemas
- `SchemaDiff` - Immutable result with added/removed/common types
- `TypeDiff` - Field-level changes for types present in both schemas
- `FieldDiff` - Individual field change (ADDED, REMOVED, MODIFIED, UNCHANGED)
- `DiffKind` - Enumeration of change types
- Optional field-level diffing via `includeFieldLevel(true)`
- Type filtering via `ignoreTypes(...)`

**Migration Analysis (`schematools.analysis`):**
- `MigrationAnalyzer` - Fluent API for analyzing migration paths
- `MigrationPath` - Complete migration sequence with all steps
- `MigrationStep` - Single version transition with optional DataFix and SchemaDiff
- `FixCoverage` - Analysis result showing fix coverage for schema changes
- `CoverageGap` - Represents a schema change without corresponding DataFix
- Coverage gap reasons: TYPE_ADDED, TYPE_REMOVED, TYPE_MODIFIED, FIELD_ADDED, FIELD_REMOVED, FIELD_TYPE_CHANGED
- Orphan fix detection (fixes without schema changes)

**Schema Validation (`schematools.validation`):**
- `SchemaValidator` - Fluent API for validating schemas
- `ValidationResult` - Immutable collection of validation issues
- `ValidationIssue` - Single issue with severity, code, message, location, context
- `IssueSeverity` - ERROR, WARNING, INFO levels
- `StructureValidator` - Validates schema structure (cycles, version ordering, parent chains)
- `ConventionChecker` - Validates naming conventions for types, fields, classes
- `ConventionRules` - Configurable naming rules (STRICT, RELAXED, NONE, or custom)
- Schema class prefix/suffix validation (e.g., "Schema" prefix for Schema100, Schema200)
- Fix class prefix/suffix validation (e.g., "Fix" suffix for PlayerNameFix)
- Predefined patterns for snake_case, camelCase
- Custom validators via `customTypeValidator()` and `customFieldValidator()`

**Type Introspection (`schematools.introspection`):**
- `TypeIntrospector` - Utility for analyzing type structures
- `TypeStructure` - Normalized, comparable representation of a Type
- `FieldInfo` - Field metadata (name, path, optionality, type)
- `TypeKind` - Classification (PRIMITIVE, LIST, OPTIONAL, PRODUCT, SUM, FIELD, etc.)
- Recursive field extraction with hierarchical paths
- Structural equality comparison

#### CLI Module (`aether-datafixers-cli`)

New command-line interface for data migration without writing Java code.

**Commands:**
- `migrate` - Migrate data files from one schema version to another
- `validate` - Check if files need migration without modifying them
- `info` - Display version info, available formats, and bootstrap details

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
- `FormatHandler<T>` - SPI for pluggable serialization formats
- `FormatRegistry` - ServiceLoader-based handler discovery
- `json-gson` - JSON format using Google Gson (default)
- `json-jackson` - JSON format using Jackson Databind

**Utilities:**
- `BootstrapLoader` - Reflective loading of DataFixerBootstrap implementations
- `VersionExtractor` - Extract version from nested JSON paths (dot notation)
- `ReportFormatter` - Text and JSON migration report formatting
- `TextReportFormatter` - Human-readable single-line reports
- `JsonReportFormatter` - Machine-readable JSON reports

**Exceptions:**
- `BootstrapLoadException` - Bootstrap class loading failures
- `FormatParseException` - Input parsing failures

### Documentation

- Added comprehensive Schema Tools documentation (diffing, analysis, validation, introspection)
- Added CLI module documentation (commands, format handlers, examples)
- Updated glossary with Schema Tools terminology
- Updated installation guide with new modules

---

## [0.2.0] - 2026-01-07

### Added

#### Testkit Module (`aether-datafixers-testkit`)
- **TestData** - Fluent builders for creating test data (`TestData.gson().object()...`)
- **AetherAssertions** - Custom AssertJ assertions for `Dynamic`, `DataResult`, `Typed`
- **DataFixTester** - Test harness for isolated DataFix testing with fluent API
- **MigrationTester** - Test harness for full migration chain testing
- **SchemaTester** - Test harness for schema validation
- **QuickFix** - Factory methods for common fix patterns (rename, add, remove, transform)
- **MockSchemas** - Mock schema utilities for testing
- **RecordingContext** - Context that records warnings for test verification

#### Migration Diagnostics
- **DiagnosticContext** - Opt-in diagnostic context for capturing migration reports
- **DiagnosticOptions** - Configurable options (snapshots, rule details, pretty print)
- **MigrationReport** - Structured report with timing, applied fixes, and touched types
- **FixExecution** - Per-fix execution details with before/after snapshots
- **RuleApplication** - Per-rule application details

#### Extended Rewrite Rules
- `Rules.renameFields(ops, map)` - Batch rename multiple fields
- `Rules.removeFields(ops, fields...)` - Batch remove multiple fields
- `Rules.groupFields(ops, target, fields...)` - Group flat fields into nested object
- `Rules.flattenField(ops, field)` - Flatten nested object to root level
- `Rules.moveField(ops, source, target)` - Move field between paths
- `Rules.copyField(ops, source, target)` - Copy field to new location
- `Rules.transformFieldAt(ops, path, fn)` - Transform at nested path
- `Rules.renameFieldAt(ops, path, newName)` - Rename at nested path
- `Rules.removeFieldAt(ops, path)` - Remove at nested path
- `Rules.addFieldAt(ops, path, value)` - Add at nested path
- `Rules.ifFieldExists(ops, field, rule)` - Conditional on field existence
- `Rules.ifFieldMissing(ops, field, rule)` - Conditional on field absence
- `Rules.ifFieldEquals(ops, field, value, rule)` - Conditional on field value

#### High-Performance APIs
- **BatchTransform** - Builder for batching multiple field operations
- `Rules.batch(ops, builder)` - Apply multiple operations in single encode/decode cycle
- `Rules.conditionalTransform(ops, predicate, transform)` - Single-pass conditional transform
- `Rules.ifFieldExists(ops, field, transform)` - Single-pass version (Function overload)
- `Rules.ifFieldMissing(ops, field, transform)` - Single-pass version (Function overload)
- `Rules.ifFieldEquals(ops, field, value, transform)` - Single-pass version (Function overload)

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
