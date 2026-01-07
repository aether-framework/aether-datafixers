# Changelog

All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.3.0] - 2026-01-07

### Added

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
