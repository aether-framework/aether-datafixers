# Schema Tools

The **Aether Datafixers Schema Tools** module provides utilities for analyzing, comparing, validating, and introspecting schemas. It helps ensure migration completeness, detect schema changes, and enforce naming conventions.

## Overview

Schema Tools (`aether-datafixers-schema-tools`) is designed for:

- **Schema Diffing** — Compare two schemas to see what types and fields changed
- **Migration Analysis** — Analyze migration paths and verify fix coverage
- **Validation** — Validate schema structure and enforce naming conventions
- **Introspection** — Inspect type structures and extract field metadata

## Quick Start

```java
import de.splatgames.aether.datafixers.schematools.diff.SchemaDiffer;
import de.splatgames.aether.datafixers.schematools.analysis.MigrationAnalyzer;
import de.splatgames.aether.datafixers.schematools.validation.SchemaValidator;

// Compare two schemas
SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
    .includeFieldLevel(true)
    .diff();

// Analyze migration path
MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
    .from(100).to(200)
    .analyze();

// Validate schemas
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateAll()
    .validate();
```

## Module Contents

| Package | Description |
|---------|-------------|
| `schematools.diff` | Schema comparison and diff generation |
| `schematools.analysis` | Migration path analysis and fix coverage |
| `schematools.validation` | Schema validation and convention checking |
| `schematools.introspection` | Type structure inspection and field extraction |

## Installation

Add schema-tools as a dependency:

**Maven:**
```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-schema-tools</artifactId>
</dependency>
```

**Gradle:**
```groovy
implementation 'de.splatgames.aether.datafixers:aether-datafixers-schema-tools'
```

## Use Cases

### Pre-Release Validation

Before releasing a new version, validate that all schema changes have corresponding DataFixes:

```java
FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
    .from(oldVersion)
    .to(newVersion)
    .analyzeCoverage();

if (!coverage.isFullyCovered()) {
    for (CoverageGap gap : coverage.gaps()) {
        System.err.println("Missing fix: " + gap.type() + " - " + gap.reason());
    }
    throw new IllegalStateException("Not all schema changes are covered by fixes!");
}
```

### CI/CD Integration

Integrate schema validation into your build pipeline:

```java
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateStructure()
    .validateConventions()
    .withConventions(ConventionRules.STRICT)
    .validate();

if (!result.isValid()) {
    result.errors().forEach(error ->
        System.err.println("[ERROR] " + error.code() + ": " + error.message())
    );
    System.exit(1);
}
```

### Schema Change Documentation

Generate documentation of schema changes between versions:

```java
SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
    .includeFieldLevel(true)
    .diff();

System.out.println("Schema Changes V1 -> V2:");
System.out.println("  Added types: " + diff.addedTypes());
System.out.println("  Removed types: " + diff.removedTypes());

for (TypeDiff typeDiff : diff.typeDiffs().values()) {
    System.out.println("  " + typeDiff.reference().name() + ":");
    typeDiff.addedFields().forEach(f ->
        System.out.println("    + " + f.fieldName()));
    typeDiff.removedFields().forEach(f ->
        System.out.println("    - " + f.fieldName()));
}
```

---

## In This Section

- [Schema Diffing](schema-diffing.md) — Compare schemas and detect changes
- [Migration Analysis](migration-analysis.md) — Analyze migration paths and fix coverage
- [Schema Validation](schema-validation.md) — Validate structure and conventions
- [Type Introspection](type-introspection.md) — Inspect type structures

---

## Next Steps

→ [Schema Diffing](schema-diffing.md) — Learn how to compare schemas
