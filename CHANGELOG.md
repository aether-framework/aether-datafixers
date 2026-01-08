# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
- Predefined patterns for snake_case, camelCase, class suffixes
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
