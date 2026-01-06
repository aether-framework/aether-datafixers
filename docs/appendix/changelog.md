# Documentation Changelog

History of documentation updates.

## Version 0.2.0

Extended rules, testkit module, and migration diagnostics release.

### Migration Diagnostics

New opt-in diagnostic system for capturing structured reports during migrations:

**Core API (`aether-datafixers-api`):**
- `DiagnosticOptions` — Configuration for diagnostic capture (snapshots, rule details, limits)
- `DiagnosticContext` — Context interface extending `DataFixerContext` for diagnostic migrations
- `MigrationReport` — Immutable report with timing, fixes, rules, warnings, and snapshots
- `FixExecution` — Record of individual fix executions with timing and rule applications
- `RuleApplication` — Record of individual rule applications within a fix

**Implementation (`aether-datafixers-core`):**
- `DiagnosticContextImpl` — Full implementation with logging and message formatting
- `MigrationReportImpl` — Immutable report implementation with builder
- `DiagnosticRuleWrapper` — Rule wrapper for capturing rule-level timing

**Features:**
- Zero overhead when diagnostics are not enabled (opt-in via `DiagnosticContext`)
- Configurable snapshot capture with truncation limits
- Per-fix and per-rule timing measurements
- Warning emission from DataFix implementations
- Summary generation for quick overview

**Presets:**
- `DiagnosticOptions.defaults()` — Full diagnostics with snapshots and rule details
- `DiagnosticOptions.minimal()` — Timing only, minimal overhead

### Extended Rewrite Rules

New convenience methods in `Rules` class for common transformation patterns:

**Core Rules:**
- `dynamicTransform(name, ops, fn)` — Custom Dynamic transformation
- `setField(ops, field, value)` — Set field (overwrites existing)

**Batch Operations:**
- `renameFields(ops, map)` — Batch rename multiple fields
- `removeFields(ops, fields...)` — Batch remove multiple fields

**Grouping and Moving:**
- `groupFields(ops, target, fields...)` — Group fields into nested object
- `flattenField(ops, field)` — Flatten nested object to root
- `moveField(ops, source, target)` — Move field between paths
- `copyField(ops, source, target)` — Copy field (keeps original)

**Path-Based Operations:**
- `transformFieldAt(ops, path, fn)` — Transform at nested path
- `renameFieldAt(ops, path, newName)` — Rename at nested path
- `removeFieldAt(ops, path)` — Remove at nested path
- `addFieldAt(ops, path, value)` — Add at nested path

**Conditional Rules:**
- `ifFieldExists(ops, field, rule)` — Apply rule if field exists
- `ifFieldMissing(ops, field, rule)` — Apply rule if field missing
- `ifFieldEquals(ops, field, value, rule)` — Apply rule if field equals value

### New How-To Guides

- [Batch Operations](../how-to/batch-operations.md) — Rename/remove multiple fields
- [Group Fields](../how-to/group-fields.md) — Grouping and flattening structures
- [Conditional Rules](../how-to/conditional-rules.md) — Conditional rule application

### Testkit Module

New module `aether-datafixers-testkit` for testing migrations:

- `TestData` — Fluent test data builders
- `AetherAssertions` — Custom AssertJ assertions for Dynamic, DataResult, Typed
- `DataFixTester` — Test harness for individual DataFix implementations
- `MigrationTester` — Test harness for complete migration chains
- `SchemaTester` — Test harness for Schema validation
- `QuickFix` — Factory methods for common fix patterns
- `MockSchemas` — Factory for mock Schema instances
- `RecordingContext` / `AssertingContext` — Test contexts

### Documentation Updates

- Added [Use Diagnostics](../how-to/use-diagnostics.md) guide for migration diagnostics
- Updated [Rewrite Rules](../concepts/rewrite-rules.md) with extended rules section
- Updated [Concepts Index](../concepts/index.md) with extended rules examples
- Updated [How-To Index](../how-to/index.md) with new guides
- Added [Test Migrations](../how-to/test-migrations.md) guide
- Added [Testkit documentation](../testkit/index.md) section
- Updated [Glossary](glossary.md) with testkit terms

---

## Version 0.1.0

Initial documentation release covering:

### Getting Started
- Installation guide with Maven, Gradle, BOM
- Quick start tutorial
- First migration walkthrough

### Core Concepts
- Architecture overview
- DataVersion and TypeReference
- Schema and Type systems
- DataFix and DataFixer
- Dynamic and DynamicOps
- Codec system
- DSL reference
- Rewrite rules
- DataResult error handling
- Thread safety guarantees

### Optics
- Lens, Prism, Iso
- Affine, Traversal, Getter
- Finder for Dynamic navigation

### Tutorials
- Basic migration
- Multi-version migration chains
- Schema inheritance
- Codec usage
- RecordCodecBuilder
- Polymorphic data
- Nested transformations
- Custom DynamicOps

### How-To Guides
- Field operations (rename, add, remove, transform)
- Restructuring data
- Type conversion
- Optional and unknown fields
- Composing fixes
- Debugging and testing
- Logging migrations
- Finder usage
- Bootstrap creation
- Gson integration

### Examples
- Complete game data example
- User profile migration
- Configuration migration
- Entity polymorphism

### API Reference
- Complete API documentation
- All packages covered
- Method signatures and examples

### Advanced Topics
- Traversal strategies
- Custom optics
- Recursive types
- Performance optimization
- Concurrent migrations
- Format conversion
- Framework extension

### Troubleshooting
- Common errors
- Debugging tips
- FAQ

### Appendix
- Glossary
- Type theory primer
- DFU comparison

---

## Contributing to Documentation

To suggest documentation improvements:
1. Open an issue on GitHub
2. Describe the improvement
3. Provide examples if applicable

