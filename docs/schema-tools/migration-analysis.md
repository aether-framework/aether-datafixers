# Migration Analysis

The `schematools.analysis` package provides utilities for analyzing migration paths between schema versions and verifying that all schema changes have corresponding DataFixes.

## Overview

Migration analysis helps ensure your migrations are complete and well-structured:

- **Path Analysis** — Understand the sequence of steps from one version to another
- **Coverage Analysis** — Identify schema changes without corresponding DataFixes
- **Gap Detection** — Find types or fields that might not be migrated correctly

## Core Components

| Class | Description |
|-------|-------------|
| `MigrationAnalyzer` | Entry point with fluent API for analysis |
| `MigrationPath` | Complete migration sequence with all steps |
| `MigrationStep` | Single version transition in the path |
| `FixCoverage` | Coverage analysis identifying gaps |
| `CoverageGap` | Individual gap where a fix may be missing |

## Basic Usage

### Analyzing a Migration Path

```java
import de.splatgames.aether.datafixers.schematools.analysis.MigrationAnalyzer;
import de.splatgames.aether.datafixers.schematools.analysis.MigrationPath;

// From a DataFixerBootstrap
MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
    .from(100)
    .to(300)
    .analyze();

// Print the migration steps
System.out.println("Migration: " + path.stepCount() + " steps, " +
                   path.fixCount() + " fixes");

for (MigrationStep step : path.steps()) {
    System.out.println("v" + step.sourceVersion().value() +
                       " -> v" + step.targetVersion().value());
    step.fix().ifPresent(fix ->
        System.out.println("  Fix: " + fix.name()));
}
```

### Analyzing Fix Coverage

```java
import de.splatgames.aether.datafixers.schematools.analysis.FixCoverage;
import de.splatgames.aether.datafixers.schematools.analysis.CoverageGap;

FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
    .from(100)
    .to(300)
    .analyzeCoverage();

if (coverage.isFullyCovered()) {
    System.out.println("All schema changes are covered by fixes!");
} else {
    System.out.println("Missing coverage for " + coverage.gapCount() + " changes:");
    for (CoverageGap gap : coverage.gaps()) {
        System.out.println("  " + gap.type().name() + " @ v" +
            gap.sourceVersion().value() + " -> v" +
            gap.targetVersion().value() + ": " + gap.reason());
    }
}
```

## Creating a MigrationAnalyzer

### From Bootstrap

The most common approach — analyzes the bootstrap's schemas and fixes:

```java
MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(bootstrap);
```

### From Registries

For more control, use pre-built registries:

```java
MigrationAnalyzer analyzer = MigrationAnalyzer.forRegistries(
    schemaRegistry,
    dataFixRegistry
);
```

## Configuration Options

### Version Range

Specify the source and target versions:

```java
// Using int values
MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
    .from(100)
    .to(300)
    .analyze();

// Using DataVersion objects
MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
    .from(new DataVersion(100))
    .to(new DataVersion(300))
    .analyze();
```

### Field-Level Analysis

Enable field-level analysis for detailed coverage checking:

```java
FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
    .from(100)
    .to(300)
    .includeFieldLevel(true)  // Detect field-level gaps
    .analyzeCoverage();
```

## MigrationPath API

`MigrationPath` represents the complete sequence of migration steps:

### Basic Queries

```java
MigrationPath path = /* ... */;

// Number of steps and fixes
int steps = path.stepCount();
int fixes = path.fixCount();

// Status checks
boolean empty = path.isEmpty();
boolean hasChanges = path.hasChanges();

// All affected types across the entire path
Set<TypeReference> affected = path.affectedTypes();
```

### Accessing Steps

```java
// All steps in order
List<MigrationStep> steps = path.steps();

// First and last (optional, in case path is empty)
Optional<MigrationStep> first = path.firstStep();
Optional<MigrationStep> last = path.lastStep();
```

### Filtering Steps

```java
// Steps that affect a specific type
List<MigrationStep> playerSteps = path.stepsAffecting(TypeReferences.PLAYER);

// Steps with/without fixes
List<MigrationStep> withFixes = path.stepsWithFixes();
List<MigrationStep> withoutFixes = path.stepsWithoutFixes();

// Check if a type is affected anywhere in the path
boolean affectsPlayer = path.affects(TypeReferences.PLAYER);
```

## MigrationStep API

`MigrationStep` represents a single version transition:

```java
MigrationStep step = /* ... */;

// Version range
DataVersion from = step.sourceVersion();
DataVersion to = step.targetVersion();

// The DataFix applied (if any)
Optional<DataFix<?>> fix = step.fix();
boolean hasFix = step.hasFix();

// Schema diff for this step (if available)
Optional<SchemaDiff> diff = step.schemaDiff();

// Types affected in this step
Set<TypeReference> affected = step.affectedTypes();
boolean affectsPlayer = step.affects(TypeReferences.PLAYER);

// Status
boolean hasChanges = step.hasChanges();
```

## FixCoverage API

`FixCoverage` analyzes whether schema changes have corresponding fixes:

### Status Queries

```java
FixCoverage coverage = /* ... */;

// Overall status
boolean complete = coverage.isFullyCovered();
boolean hasGaps = coverage.hasGaps();
boolean hasOrphans = coverage.hasOrphanFixes();

// Counts
int gapCount = coverage.gapCount();
int orphanCount = coverage.orphanCount();
int uncoveredTypeCount = coverage.uncoveredTypeCount();

// Coverage percentage (given total number of changes)
double percent = coverage.coveragePercent(totalChanges);
```

### Accessing Gaps

```java
// All gaps
List<CoverageGap> gaps = coverage.gaps();

// Gaps for a specific type
List<CoverageGap> playerGaps = coverage.gapsForType(TypeReferences.PLAYER);

// Gaps by reason
List<CoverageGap> addedTypeGaps = coverage.gapsByReason(CoverageGap.Reason.TYPE_ADDED);

// Types with incomplete coverage
Set<TypeReference> uncovered = coverage.uncoveredTypes();

// Check if a specific type is fully covered
boolean playerCovered = coverage.isCovered(TypeReferences.PLAYER);
```

### Orphan Fixes

Fixes that don't correspond to any detected schema change:

```java
List<DataFix<?>> orphans = coverage.orphanFixes();
```

## CoverageGap API

`CoverageGap` represents a schema change without a corresponding fix:

```java
CoverageGap gap = /* ... */;

// The affected type
TypeReference type = gap.type();

// Version range where the gap exists
DataVersion from = gap.sourceVersion();
DataVersion to = gap.targetVersion();

// Why the gap exists
CoverageGap.Reason reason = gap.reason();

// For field-level gaps, the field name
Optional<String> fieldName = gap.fieldName();

// Detailed type diff (if available)
Optional<TypeDiff> typeDiff = gap.typeDiff();

// Gap classification
boolean isFieldLevel = gap.isFieldLevel();
boolean isTypeLevel = gap.isTypeLevel();
```

### Gap Reasons

| Reason | Level | Description |
|--------|-------|-------------|
| `TYPE_ADDED` | Type | New type without initialization fix |
| `TYPE_REMOVED` | Type | Type removed without cleanup fix |
| `TYPE_MODIFIED` | Type | Type structure changed without fix |
| `FIELD_ADDED` | Field | New field without default value fix |
| `FIELD_REMOVED` | Field | Field removed without cleanup fix |
| `FIELD_TYPE_CHANGED` | Field | Field type changed without conversion fix |

## Example: CI/CD Validation

Integrate coverage checking into your build pipeline:

```java
public class MigrationCoverageValidator {

    public static void validate(DataFixerBootstrap bootstrap,
                                int minVersion,
                                int maxVersion) {
        FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
            .from(minVersion)
            .to(maxVersion)
            .includeFieldLevel(true)
            .analyzeCoverage();

        if (coverage.isFullyCovered()) {
            System.out.println("Migration coverage: 100%");
            return;
        }

        System.err.println("MIGRATION COVERAGE FAILURE");
        System.err.println("==========================");
        System.err.println();

        // Group gaps by type for readability
        Map<TypeReference, List<CoverageGap>> byType = coverage.gaps().stream()
            .collect(Collectors.groupingBy(CoverageGap::type));

        for (var entry : byType.entrySet()) {
            System.err.println("Type: " + entry.getKey().name());
            for (CoverageGap gap : entry.getValue()) {
                System.err.println("  v" + gap.sourceVersion().value() +
                    " -> v" + gap.targetVersion().value() +
                    ": " + gap.reason() +
                    gap.fieldName().map(f -> " (field: " + f + ")").orElse(""));
            }
            System.err.println();
        }

        // Also report orphan fixes
        if (coverage.hasOrphanFixes()) {
            System.err.println("Orphan fixes (no corresponding schema change):");
            for (DataFix<?> orphan : coverage.orphanFixes()) {
                System.err.println("  - " + orphan.name());
            }
            System.err.println();
        }

        throw new AssertionError(
            "Migration coverage incomplete: " + coverage.gapCount() + " gaps found"
        );
    }
}
```

## Example: Migration Summary Report

Generate a summary of what happens during migration:

```java
public class MigrationSummary {

    public static void print(DataFixerBootstrap bootstrap, int from, int to) {
        MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
            .from(from)
            .to(to)
            .analyze();

        System.out.println("Migration Summary: v" + from + " -> v" + to);
        System.out.println("=".repeat(50));
        System.out.println();
        System.out.println("Total steps: " + path.stepCount());
        System.out.println("Total fixes: " + path.fixCount());
        System.out.println("Affected types: " + path.affectedTypes().size());
        System.out.println();

        if (path.isEmpty()) {
            System.out.println("No migration needed - schemas are compatible.");
            return;
        }

        System.out.println("Steps:");
        for (MigrationStep step : path.steps()) {
            String fixName = step.fix()
                .map(f -> f.name())
                .orElse("(no fix)");
            System.out.println("  " + step.sourceVersion().value() +
                " -> " + step.targetVersion().value() + ": " + fixName);
        }

        System.out.println();
        System.out.println("Types affected:");
        for (TypeReference type : path.affectedTypes()) {
            long stepCount = path.stepsAffecting(type).size();
            System.out.println("  " + type.name() + " (" + stepCount + " steps)");
        }
    }
}
```

## Best Practices

1. **Run coverage analysis in CI** — Catch missing fixes before release
2. **Use field-level analysis for thorough checks** — Detect subtle field changes
3. **Review orphan fixes** — They might indicate outdated code or schema mismatches
4. **Generate migration summaries for releases** — Document what changes for users

---

## Related

- [Schema Diffing](schema-diffing.md) — Understanding how diffs are computed
- [Schema Validation](schema-validation.md) — Validating schema structure
- [DataFix System](../concepts/datafix-system.md) — Core DataFix concepts
