# Aether Datafixers v0.2.0 — Testkit, Extended Rules, Diagnostics, and Performance

Extended rules, testkit module, migration diagnostics, and high-performance APIs.

---

## Highlights in v0.2.0

- **Testkit Module** — New `aether-datafixers-testkit` module with fluent test data builders, custom AssertJ assertions, and test harnesses for DataFix, Schema, and migration testing.
- **Extended Rewrite Rules** — Convenience methods for batch operations, field grouping/flattening, path-based operations, and conditional rules.
- **Migration Diagnostics** — Opt-in diagnostic system for structured reports with timing, snapshots, and warnings.
- **High-Performance APIs** — Batch transformations and single-pass conditionals for optimized migrations.
- **Performance Optimizations** — Internal improvements with memoized path parsing, pre-allocated lists, and reduced allocations.

---

## Installation

> [!TIP]
> All Aether artifacts are available on **Maven Central** — no extra repository required.

### Maven

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-core</artifactId>
    <version>0.2.0</version>
</dependency>
```

**Using the BOM**

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>de.splatgames.aether.datafixers</groupId>
            <artifactId>aether-datafixers-bom</artifactId>
            <version>0.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- No version needed -->
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-core</artifactId>
    </dependency>
</dependencies>
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-core:0.2.0'
    // Or with BOM:
    implementation platform('de.splatgames.aether.datafixers:aether-datafixers-bom:0.2.0')
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-core'
}
```

### Gradle (Kotlin)

```kotlin
dependencies {
    implementation("de.splatgames.aether.datafixers:aether-datafixers-core:0.2.0")
    // Or with BOM:
    implementation(platform("de.splatgames.aether.datafixers:aether-datafixers-bom:0.2.0"))
    implementation("de.splatgames.aether.datafixers:aether-datafixers-core")
}
```

---

## What's New

### Testkit Module

New module `aether-datafixers-testkit` for testing migrations:

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-testkit</artifactId>
    <version>0.2.0</version>
    <scope>test</scope>
</dependency>
```

**Features:**
- `TestData` — Fluent test data builders for Gson and Jackson
- `AetherAssertions` — Custom AssertJ assertions for `Dynamic`, `DataResult`, `Typed`
- `DataFixTester` — Test harness for individual DataFix implementations
- `MigrationTester` — Test harness for complete migration chains
- `SchemaTester` — Test harness for Schema validation
- `QuickFix` — Factory methods for common fix patterns
- `MockSchemas` — Factory for mock Schema instances
- `RecordingContext` / `AssertingContext` — Test contexts

**Example:**
```java
// Create test data fluently
Dynamic<JsonElement> input = TestData.gson().object()
    .put("name", "Alice")
    .put("level", 10)
    .build();

// Test a DataFix
DataFixTester.forFix(myFix)
    .withInput(input)
    .forType("player")
    .expectOutput(expected)
    .verify();

// Test a full migration chain
MigrationTester.forFixer(myFixer)
    .forType(PLAYER)
    .withInput(v1Data)
    .from(1).to(5)
    .expectOutput(v5Data)
    .verify();
```

### Extended Rewrite Rules

New convenience methods in `Rules` class:

| Rule | Purpose |
|------|---------|
| `dynamicTransform(name, ops, fn)` | Custom Dynamic transformation |
| `setField(ops, field, value)` | Set field (overwrites existing) |
| `renameFields(ops, map)` | Batch rename multiple fields |
| `removeFields(ops, fields...)` | Batch remove multiple fields |
| `groupFields(ops, target, fields...)` | Group fields into nested object |
| `flattenField(ops, field)` | Flatten nested object to root |
| `moveField(ops, source, target)` | Move field between paths |
| `copyField(ops, source, target)` | Copy field (keeps original) |
| `transformFieldAt(ops, path, fn)` | Transform at nested path |
| `renameFieldAt(ops, path, newName)` | Rename at nested path |
| `removeFieldAt(ops, path)` | Remove at nested path |
| `addFieldAt(ops, path, value)` | Add at nested path |
| `ifFieldExists(ops, field, rule)` | Conditional on existence |
| `ifFieldMissing(ops, field, rule)` | Conditional on absence |
| `ifFieldEquals(ops, field, value, rule)` | Conditional on value |

**Example:**
```java
Rules.seq(
    Rules.renameFields(ops, Map.of("playerName", "name", "xp", "experience")),
    Rules.groupFields(ops, "position", "x", "y", "z"),
    Rules.ifFieldMissing(ops, "version", Rules.setField(ops, "version", d -> d.createInt(1)))
)
```

### Migration Diagnostics

New opt-in diagnostic system for capturing structured reports:

**API:**
- `DiagnosticOptions` — Configuration for diagnostic capture
- `DiagnosticContext` — Context interface for diagnostic migrations
- `MigrationReport` — Immutable report with timing, fixes, rules, warnings, and snapshots

**Features:**
- Zero overhead when diagnostics are not enabled
- Configurable snapshot capture with truncation limits
- Per-fix and per-rule timing measurements
- Warning emission from DataFix implementations

**Presets:**
- `DiagnosticOptions.defaults()` — Full diagnostics with snapshots and rule details
- `DiagnosticOptions.minimal()` — Timing only, minimal overhead

### High-Performance APIs

**BatchTransform:**
```java
Rules.batch(ops, batch -> batch
    .rename("oldName", "newName")
    .remove("deprecated")
    .set("version", d -> d.createInt(2))
    .transform("count", d -> d.createInt(d.asInt(0) + 1))
    .addIfMissing("created", d -> d.createLong(System.currentTimeMillis()))
)
```

**Single-Pass Conditionals:**
```java
Rules.conditionalTransform(ops,
    d -> d.get("type").asString("").equals("legacy"),
    d -> d.set("migrated", d.createBoolean(true))
)
```

### Performance Optimizations

Internal optimizations with no API changes:
- Path parsing uses character-based parsing with memoization cache
- `DataFixRegistry.getFixes()` pre-allocates result list
- `DataFixerImpl` moves validation to registration time
- Reduced allocations in hot paths

---

## Changelog

**New in 0.2.0**

- Testkit module with fluent builders, assertions, and test harnesses
- Extended rewrite rules for batch, grouping, path, and conditional operations
- Migration diagnostics system with timing and snapshots
- High-performance batch transformations
- Single-pass conditional APIs
- Performance optimizations (memoization, pre-allocation)
- Comprehensive documentation updates

**Full Changelog:** [v0.1.0...v0.2.0](https://github.com/aether-framework/aether-datafixers/compare/v0.1.0...v0.2.0)

---

## Roadmap (next)

- **0.3.x**
    - Additional codec implementations
    - Schema validation enhancements
    - Migration dry-run mode

- **1.0.x**
    - Stable API surface
    - Comprehensive documentation
    - Production-ready release

---

## License

**MIT** — see `LICENSE`.
