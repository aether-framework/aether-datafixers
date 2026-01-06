# How to Use Migration Diagnostics

This guide shows how to capture detailed diagnostic information during data migrations using the opt-in diagnostics system.

## Overview

Migration Diagnostics provides structured reports about what happens during a migration:
- Applied fixes with timing information
- Touched TypeReferences
- Individual rule applications
- Before/after snapshots of data

Diagnostics are **opt-in** — migrations without a `DiagnosticContext` have zero overhead.

## Basic Usage

### Create a Diagnostic Context

```java
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;

// Create context with default options
DiagnosticContext context = DiagnosticContext.create();

// Or with custom options
DiagnosticContext context = DiagnosticContext.create(
    DiagnosticOptions.builder()
        .captureSnapshots(true)
        .captureRuleDetails(true)
        .build()
);
```

### Run Migration with Diagnostics

Pass the context to the `update()` method:

```java
Dynamic<JsonElement> result = fixer.update(
    TypeReferences.PLAYER,
    inputData,
    new DataVersion(1),
    new DataVersion(5),
    context  // Pass the diagnostic context
);

// Get the report after migration
MigrationReport report = context.getReport();
```

### Inspect the Report

```java
// Basic information
System.out.println("Type: " + report.type().getId());
System.out.println("From: v" + report.fromVersion().getVersion());
System.out.println("To: v" + report.toVersion().getVersion());
System.out.println("Duration: " + report.totalDuration().toMillis() + "ms");
System.out.println("Fixes applied: " + report.fixCount());

// Check for warnings
if (report.hasWarnings()) {
    System.out.println("Warnings:");
    for (String warning : report.warnings()) {
        System.out.println("  - " + warning);
    }
}

// Quick summary
System.out.println(report.toSummary());
```

## Diagnostic Options

Configure what diagnostics to capture:

```java
DiagnosticOptions options = DiagnosticOptions.builder()
    .captureSnapshots(true)       // Capture before/after data snapshots
    .captureRuleDetails(true)     // Capture individual rule applications
    .maxSnapshotLength(10000)     // Truncate snapshots longer than this
    .prettyPrintSnapshots(true)   // Pretty-print JSON snapshots
    .build();
```

### Presets

```java
// Full diagnostics (default)
DiagnosticOptions.defaults();

// Minimal overhead (timing only, no snapshots)
DiagnosticOptions.minimal();
```

| Preset | Snapshots | Rule Details | Pretty Print |
|--------|-----------|--------------|--------------|
| `defaults()` | Yes | Yes | Yes |
| `minimal()` | No | No | No |

## Working with Fix Executions

Each applied fix is recorded as a `FixExecution`:

```java
for (FixExecution fix : report.fixExecutions()) {
    System.out.println("Fix: " + fix.fixName());
    System.out.println("  Version: " + fix.fromVersion() + " -> " + fix.toVersion());
    System.out.println("  Duration: " + fix.durationMillis() + "ms");
    System.out.println("  Rules: " + fix.ruleCount());
    System.out.println("  Matched: " + fix.matchedRuleCount());

    // Per-fix snapshots (if enabled)
    fix.beforeSnapshotOpt().ifPresent(snap ->
        System.out.println("  Before: " + snap));
    fix.afterSnapshotOpt().ifPresent(snap ->
        System.out.println("  After: " + snap));
}
```

## Working with Rule Applications

Each rule application within a fix is recorded:

```java
for (FixExecution fix : report.fixExecutions()) {
    for (RuleApplication rule : fix.ruleApplications()) {
        System.out.println("Rule: " + rule.ruleName());
        System.out.println("  Type: " + rule.typeName());
        System.out.println("  Matched: " + rule.matched());
        System.out.println("  Duration: " + rule.durationMillis() + "ms");

        rule.descriptionOpt().ifPresent(desc ->
            System.out.println("  Description: " + desc));
    }
}
```

## Emitting Warnings from Fixes

Your DataFix implementations can emit warnings via the context:

```java
public class MyFix implements DataFix<JsonElement> {
    @Override
    public Dynamic<JsonElement> apply(
            TypeReference type,
            Dynamic<JsonElement> input,
            DataFixerContext context
    ) {
        // Check for missing optional field
        if (input.get("optionalField").result().isEmpty()) {
            context.warn("Missing optional field: optionalField");
        }

        // Continue with migration...
        return input;
    }
}
```

Warnings appear in the report:

```java
if (report.hasWarnings()) {
    for (String warning : report.warnings()) {
        logger.warn("Migration warning: {}", warning);
    }
}
```

## Snapshots

When `captureSnapshots` is enabled, the report includes serialized before/after data:

```java
// Overall migration snapshots
report.inputSnapshot().ifPresent(input ->
    System.out.println("Migration input: " + input));
report.outputSnapshot().ifPresent(output ->
    System.out.println("Migration output: " + output));

// Per-fix snapshots
for (FixExecution fix : report.fixExecutions()) {
    fix.beforeSnapshotOpt().ifPresent(before ->
        System.out.println(fix.fixName() + " before: " + before));
    fix.afterSnapshotOpt().ifPresent(after ->
        System.out.println(fix.fixName() + " after: " + after));
}
```

### Snapshot Truncation

Large data structures are truncated to prevent memory issues:

```java
DiagnosticOptions options = DiagnosticOptions.builder()
    .captureSnapshots(true)
    .maxSnapshotLength(100)  // Truncate to 100 characters
    .build();

// Truncated snapshots end with "... (truncated)"
```

## Complete Example

```java
import de.splatgames.aether.datafixers.api.diagnostic.*;

public class MigrationService {
    private final DataFixer fixer;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Dynamic<JsonElement> migrateWithDiagnostics(
            Dynamic<JsonElement> data,
            DataVersion from,
            DataVersion to
    ) {
        // Create diagnostic context
        DiagnosticContext context = DiagnosticContext.create(
            DiagnosticOptions.builder()
                .captureSnapshots(true)
                .captureRuleDetails(true)
                .build()
        );

        // Run migration
        Dynamic<JsonElement> result = fixer.update(
            TypeReferences.PLAYER,
            data,
            from,
            to,
            context
        );

        // Log the report
        MigrationReport report = context.getReport();
        logReport(report);

        return result;
    }

    private void logReport(MigrationReport report) {
        logger.info("Migration completed: {}", report.toSummary());

        if (report.hasWarnings()) {
            for (String warning : report.warnings()) {
                logger.warn("  Warning: {}", warning);
            }
        }

        for (FixExecution fix : report.fixExecutions()) {
            logger.debug("  Fix '{}': {}ms, {} rules ({} matched)",
                fix.fixName(),
                fix.durationMillis(),
                fix.ruleCount(),
                fix.matchedRuleCount()
            );
        }
    }
}
```

## Performance Considerations

- **Without diagnostics**: Zero overhead — migrations run at full speed
- **With minimal options**: Small overhead for timing measurement
- **With snapshots**: Additional overhead for serialization (use for debugging only)

For production, consider:

```java
// Development/debugging
DiagnosticContext devContext = DiagnosticContext.create(DiagnosticOptions.defaults());

// Production (if needed at all)
DiagnosticContext prodContext = DiagnosticContext.create(DiagnosticOptions.minimal());

// Best: No context for maximum performance
fixer.update(type, data, from, to);  // No context = no overhead
```

## API Reference

### DiagnosticContext

| Method | Description |
|--------|-------------|
| `create()` | Create with default options |
| `create(options)` | Create with custom options |
| `isDiagnosticEnabled()` | Always returns `true` |
| `getReport()` | Get the migration report |
| `options()` | Get the diagnostic options |
| `warn(message, args...)` | Emit a warning |

### MigrationReport

| Method | Description |
|--------|-------------|
| `type()` | The migrated TypeReference |
| `fromVersion()` | Source version |
| `toVersion()` | Target version |
| `startTime()` | When migration started |
| `totalDuration()` | Total migration time |
| `fixCount()` | Number of fixes applied |
| `fixExecutions()` | List of FixExecution records |
| `ruleApplicationCount()` | Total rule applications |
| `touchedTypes()` | Set of touched TypeReferences |
| `inputSnapshot()` | Optional input snapshot |
| `outputSnapshot()` | Optional output snapshot |
| `hasWarnings()` | Whether warnings were emitted |
| `warnings()` | List of warning messages |
| `toSummary()` | Human-readable summary string |

### FixExecution

| Method | Description |
|--------|-------------|
| `fixName()` | Name of the fix |
| `fromVersion()` | Fix input version |
| `toVersion()` | Fix output version |
| `startTime()` | When fix started |
| `duration()` | Fix execution time |
| `durationMillis()` | Duration in milliseconds |
| `ruleApplications()` | List of RuleApplication records |
| `ruleCount()` | Number of rules |
| `matchedRuleCount()` | Number of matched rules |
| `beforeSnapshotOpt()` | Optional before snapshot |
| `afterSnapshotOpt()` | Optional after snapshot |
| `toSummary()` | Human-readable summary |

### RuleApplication

| Method | Description |
|--------|-------------|
| `ruleName()` | Name of the rule |
| `typeName()` | TypeReference name |
| `timestamp()` | When rule was applied |
| `duration()` | Rule execution time |
| `durationMillis()` | Duration in milliseconds |
| `matched()` | Whether rule matched |
| `description()` | Optional description |
| `descriptionOpt()` | Description as Optional |
| `toSummary()` | Human-readable summary |

## Related

- [Debug Migrations](debug-migrations.md)
- [Log Migrations](log-migrations.md)
- [Test Migrations](test-migrations.md)
- [DataFix System](../concepts/datafix-system.md)

