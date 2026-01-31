# Debugging Guide

Systematic approach to diagnosing migration issues in Aether Datafixers.

## Quick Reference

| Need               | Tool                | Configuration              |
|--------------------|---------------------|----------------------------|
| Basic logs         | SLF4J               | Set level to DEBUG         |
| Detailed trace     | `DiagnosticContext` | Enable with options        |
| Per-fix snapshots  | `DiagnosticOptions` | `captureSnapshots(true)`   |
| Rule-level detail  | `DiagnosticOptions` | `captureRuleDetails(true)` |
| Production minimal | `DiagnosticOptions` | `minimal()` preset         |

---

## SLF4J Configuration

### Default Logger Name

The default logger name for Aether Datafixers is:

```
de.splatgames.aether.datafixers
```

### Using Slf4jDataFixerContext

Route datafixer logs through your application's logging framework:

```java
import de.splatgames.aether.datafixers.core.fix.Slf4jDataFixerContext;

// Option 1: Default logger name
DataFixerContext context = new Slf4jDataFixerContext();

// Option 2: Custom logger name
DataFixerContext context = new Slf4jDataFixerContext("com.myapp.migrations");

// Option 3: Existing logger
Logger logger = LoggerFactory.getLogger(MyMigrationService.class);
DataFixerContext context = new Slf4jDataFixerContext(logger);

// Use in migration
fixer.update(typeRef, data, fromVersion, toVersion, context);
```

### Logback Configuration

```xml
<!-- logback.xml -->
<configuration>
    <!-- Console appender with pattern -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File appender for migration logs -->
    <appender name="MIGRATION_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/migrations.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/migrations.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n%ex{full}</pattern>
        </encoder>
    </appender>

    <!-- Aether Datafixers logger -->
    <logger name="de.splatgames.aether.datafixers" level="DEBUG" additivity="false">
        <appender-ref ref="MIGRATION_FILE"/>
        <appender-ref ref="CONSOLE"/>
    </logger>

    <!-- Spring Boot MigrationService -->
    <logger name="de.splatgames.aether.datafixers.spring.service" level="DEBUG"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

### Log4j2 Configuration

```xml
<!-- log4j2.xml -->
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>

        <RollingFile name="MigrationFile" fileName="logs/migrations.log"
                     filePattern="logs/migrations-%d{yyyy-MM-dd}.log.gz">
            <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n%ex{full}"/>
            <Policies>
                <TimeBasedTriggeringPolicy/>
            </Policies>
            <DefaultRolloverStrategy max="30"/>
        </RollingFile>
    </Appenders>

    <Loggers>
        <Logger name="de.splatgames.aether.datafixers" level="debug" additivity="false">
            <AppenderRef ref="MigrationFile"/>
            <AppenderRef ref="Console"/>
        </Logger>

        <Root level="info">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

### Production vs Development Settings

| Environment | Logger Level | Snapshots | Rule Details |
|-------------|--------------|-----------|--------------|
| Development | DEBUG        | Yes       | Yes          |
| Staging     | INFO         | Yes       | No           |
| Production  | WARN         | No        | No           |

---

## MigrationReport Diagnostics

### Enabling Diagnostics

```java
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;

// Full diagnostics for debugging
DiagnosticContext context = DiagnosticContext.create(
    DiagnosticOptions.builder()
        .captureSnapshots(true)
        .captureRuleDetails(true)
        .prettyPrintSnapshots(true)
        .build()
);

// Run migration
Dynamic<?> result = fixer.update(typeRef, data, fromVersion, toVersion, context);

// Get the report
MigrationReport report = context.getReport();
```

### Report Fields Reference

| Field           | Method                   | Description             | When to Use           |
|-----------------|--------------------------|-------------------------|-----------------------|
| Type            | `type()`                 | TypeReference migrated  | Always                |
| From Version    | `fromVersion()`          | Source version          | Always                |
| To Version      | `toVersion()`            | Target version          | Always                |
| Duration        | `totalDuration()`        | Total migration time    | Performance issues    |
| Fix Count       | `fixCount()`             | Number of fixes applied | Verify migration path |
| Fix Executions  | `fixExecutions()`        | Detailed fix list       | Tracing issues        |
| Rule Count      | `ruleApplicationCount()` | Total rules evaluated   | Deep debugging        |
| Touched Types   | `touchedTypes()`         | All types processed     | Complex migrations    |
| Warnings        | `warnings()`             | Non-fatal issues        | Data quality          |
| Input Snapshot  | `inputSnapshot()`        | Data before migration   | Transform debugging   |
| Output Snapshot | `outputSnapshot()`       | Data after migration    | Transform debugging   |

### Reading the Report

```java
MigrationReport report = context.getReport();

// Basic summary
System.out.println(report.toSummary());
// Output: "Migration of 'player' from v100 to v200: 150ms, 5 fixes"

// Detailed analysis
System.out.println("Type: " + report.type().getId());
System.out.println("Version: " + report.fromVersion().getVersion() +
    " -> " + report.toVersion().getVersion());
System.out.println("Duration: " + report.totalDuration().toMillis() + "ms");
System.out.println("Fixes Applied: " + report.fixCount());
System.out.println("Rules Evaluated: " + report.ruleApplicationCount());

// Check for warnings
if (report.hasWarnings()) {
    System.out.println("Warnings:");
    for (String warning : report.warnings()) {
        System.out.println("  - " + warning);
    }
}

// Snapshots (if enabled)
report.inputSnapshot().ifPresent(snap ->
    System.out.println("Input:\n" + snap));
report.outputSnapshot().ifPresent(snap ->
    System.out.println("Output:\n" + snap));
```

---

## Tracing Fix Order

### Understanding Fix Application

Fixes are applied in version order, from `fromVersion` to `toVersion`. Each fix transforms data from one version to the next.

```
v100 ──[Fix A]──> v110 ──[Fix B]──> v150 ──[Fix C]──> v200
```

### Listing Applied Fixes

```java
MigrationReport report = context.getReport();

System.out.println("Applied fixes in order:");
for (FixExecution fix : report.fixExecutions()) {
    System.out.println(fix.toSummary());
    // Output: "rename_field (v100 -> v110): 5ms, 3 rules (2 matched)"
}
```

### Detailed Fix Analysis

```java
for (FixExecution fix : report.fixExecutions()) {
    System.out.println("\nFix: " + fix.fixName());
    System.out.println("  Version: " + fix.fromVersion().getVersion() +
        " -> " + fix.toVersion().getVersion());
    System.out.println("  Duration: " + fix.durationMillis() + "ms");
    System.out.println("  Rules: " + fix.ruleCount() +
        " (" + fix.matchedRuleCount() + " matched)");

    // Per-fix snapshots
    fix.beforeSnapshotOpt().ifPresent(snap ->
        System.out.println("  Before: " + snap));
    fix.afterSnapshotOpt().ifPresent(snap ->
        System.out.println("  After: " + snap));

    // Rule-level details (if captureRuleDetails enabled)
    for (RuleApplication rule : fix.ruleApplications()) {
        System.out.println("    Rule: " + rule.ruleName() +
            " on " + rule.typeName() +
            " -> " + (rule.matched() ? "MATCHED" : "skipped") +
            " (" + rule.durationMillis() + "ms)");
    }
}
```

### Finding a Specific Fix

```java
// Find by name
Optional<FixExecution> fix = report.fixExecutions().stream()
    .filter(f -> f.fixName().equals("rename_player_field"))
    .findFirst();

// Find by version
Optional<FixExecution> fixAtVersion = report.fixExecutions().stream()
    .filter(f -> f.fromVersion().getVersion() == 150)
    .findFirst();
```

---

## DiagnosticOptions

### Available Presets

| Preset       | Snapshots | Rule Details | Pretty Print | Use Case              |
|--------------|-----------|--------------|--------------|-----------------------|
| `defaults()` | Yes       | Yes          | Yes          | Development debugging |
| `minimal()`  | No        | No           | No           | Production monitoring |

```java
// Full diagnostics (development)
DiagnosticContext devContext = DiagnosticContext.create(DiagnosticOptions.defaults());

// Minimal overhead (production)
DiagnosticContext prodContext = DiagnosticContext.create(DiagnosticOptions.minimal());

// No diagnostics (maximum performance)
fixer.update(typeRef, data, fromVersion, toVersion);  // No context
```

### Custom Configuration

```java
DiagnosticOptions options = DiagnosticOptions.builder()
    .captureSnapshots(true)           // Enable before/after snapshots
    .captureRuleDetails(true)         // Enable per-rule tracking
    .maxSnapshotLength(10000)         // Truncate large snapshots (0 = unlimited)
    .prettyPrintSnapshots(true)       // Format JSON for readability
    .build();
```

### Snapshot Truncation

Large data structures are truncated to prevent memory issues:

```java
DiagnosticOptions options = DiagnosticOptions.builder()
    .captureSnapshots(true)
    .maxSnapshotLength(500)  // Truncate to 500 characters
    .build();

// Truncated snapshots end with "... (truncated)"
```

---

## Step-by-Step Debugging Workflow

### 1. Reproduce the Issue

```java
// Isolate a single problematic record
Dynamic<?> problematicData = loadProblemRecord();
DataVersion fromVersion = new DataVersion(100);
DataVersion toVersion = new DataVersion(200);
```

### 2. Enable Full Diagnostics

```java
DiagnosticContext context = DiagnosticContext.create(
    DiagnosticOptions.builder()
        .captureSnapshots(true)
        .captureRuleDetails(true)
        .prettyPrintSnapshots(true)
        .build()
);
```

### 3. Run Migration with Diagnostics

```java
try {
    Dynamic<?> result = fixer.update(typeRef, problematicData, fromVersion, toVersion, context);
    System.out.println("Migration succeeded");
} catch (DataFixerException e) {
    System.err.println("Migration failed: " + e.getMessage());
} finally {
    // Always get the report (even on failure)
    MigrationReport report = context.getReport();
    analyzeReport(report);
}
```

### 4. Analyze the Report

```java
private void analyzeReport(MigrationReport report) {
    System.out.println("\n=== Migration Report ===");
    System.out.println(report.toSummary());

    // Check warnings
    if (report.hasWarnings()) {
        System.out.println("\nWarnings:");
        report.warnings().forEach(w -> System.out.println("  - " + w));
    }

    // Find slow fixes
    System.out.println("\nFix timing:");
    report.fixExecutions().stream()
        .sorted((a, b) -> Long.compare(b.durationMillis(), a.durationMillis()))
        .forEach(fix -> System.out.println("  " + fix.fixName() + ": " + fix.durationMillis() + "ms"));

    // Check for unmatched rules
    long unmatchedRules = report.fixExecutions().stream()
        .flatMap(fix -> fix.ruleApplications().stream())
        .filter(rule -> !rule.matched())
        .count();
    System.out.println("\nUnmatched rules: " + unmatchedRules);
}
```

### 5. Examine Snapshots

```java
// Compare before/after for the failing fix
for (FixExecution fix : report.fixExecutions()) {
    System.out.println("\n--- " + fix.fixName() + " ---");

    fix.beforeSnapshotOpt().ifPresent(before -> {
        System.out.println("BEFORE:");
        System.out.println(before);
    });

    fix.afterSnapshotOpt().ifPresent(after -> {
        System.out.println("AFTER:");
        System.out.println(after);
    });
}
```

---

## Spring Boot Integration

### Diagnostics via MigrationService

```java
@Autowired
private MigrationService migrationService;

public void migrateWithDiagnostics(TaggedDynamic<?> data) {
    DiagnosticContext context = DiagnosticContext.create(DiagnosticOptions.defaults());

    MigrationResult result = migrationService
        .migrate(data)
        .from(100)
        .to(200)
        .withContext(context)
        .execute();

    // Analyze diagnostics
    MigrationReport report = context.getReport();
    logReport(report);
}

private void logReport(MigrationReport report) {
    logger.info("Migration: {}", report.toSummary());

    for (String warning : report.warnings()) {
        logger.warn("  Warning: {}", warning);
    }

    for (FixExecution fix : report.fixExecutions()) {
        logger.debug("  Fix '{}': {}ms, {} rules ({} matched)",
            fix.fixName(),
            fix.durationMillis(),
            fix.ruleCount(),
            fix.matchedRuleCount());
    }
}
```

### Conditional Diagnostics in Production

```java
@Value("${aether.datafixers.diagnostics.enabled:false}")
private boolean diagnosticsEnabled;

public MigrationResult migrate(TaggedDynamic<?> data) {
    MigrationService.MigrationBuilder builder = migrationService
        .migrate(data)
        .from(100)
        .to(200);

    if (diagnosticsEnabled) {
        DiagnosticContext context = DiagnosticContext.create(DiagnosticOptions.minimal());
        builder.withContext(context);
    }

    return builder.execute();
}
```

---

## Common Debugging Scenarios

### Scenario: Migration Produces Wrong Output

1. Enable snapshots
2. Compare `inputSnapshot` with `outputSnapshot`
3. Check each fix's before/after snapshots
4. Identify which fix introduced the problem

### Scenario: Migration is Slow

1. Enable `DiagnosticOptions.minimal()` (low overhead)
2. Check `report.totalDuration()`
3. Sort fixes by duration
4. Profile the slowest fix

```java
report.fixExecutions().stream()
    .sorted((a, b) -> Long.compare(b.durationMillis(), a.durationMillis()))
    .limit(5)
    .forEach(fix -> System.out.println(fix.fixName() + ": " + fix.durationMillis() + "ms"));
```

### Scenario: Warning During Migration

1. Check `report.warnings()`
2. Enable rule details to see which rule emitted the warning
3. Review the fix implementation for `context.warn()` calls

---

## Related

- [Error Scenarios](error-scenarios.md) — Exception handling reference
- [How to Use Diagnostics](../how-to/use-diagnostics.md) — Full API reference
- [How to Debug Migrations](../how-to/debug-migrations.md) — Basic debugging tips
- [Monitoring & Alerting](monitoring-alerting.md) — Production monitoring
