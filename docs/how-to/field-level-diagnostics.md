# How to Use Field-Level Diagnostics

This guide shows how to capture and inspect field-level diagnostic metadata during data migrations, giving you fine-grained visibility into which fields each rule affects.

## Overview

Standard type-level diagnostics tell you that a rule matched on a particular type, but not which fields were renamed, removed, or transformed. Field-level diagnostics solve this by capturing structured `FieldOperation` metadata for every field-aware rule application.

Key points:

- Field-level diagnostics are **opt-in** via `DiagnosticOptions.captureFieldDetails(true)` (enabled by default in `defaults()`)
- Rules created by the field operation methods in `Rules` automatically implement `FieldAwareRule`
- After migration, inspect `RuleApplication.fieldOperations()` for per-field metadata

## Quick Start

```java
import de.splatgames.aether.datafixers.api.diagnostic.*;
import de.splatgames.aether.datafixers.api.rewrite.Rules;

// 1. Create rules using Rules factory methods
TypeRewriteRule rule = Rules.renameField(ops, "playerName", "name");

// 2. The rule automatically implements FieldAwareRule — no extra work needed

// 3. Run migration with a DiagnosticContext
DiagnosticContext context = DiagnosticContext.create();
Dynamic<JsonElement> result = fixer.update(
    TypeReferences.PLAYER, inputData,
    new DataVersion(1), new DataVersion(2),
    context
);

// 4. Inspect field operations in the report
MigrationReport report = context.getReport();
for (FixExecution fix : report.fixExecutions()) {
    for (RuleApplication rule : fix.ruleApplications()) {
        for (FieldOperation op : rule.fieldOperations()) {
            System.out.println(op.toSummary());
            // Output: RENAME(playerName -> name)
        }
    }
}
```

## Which Rules Are Field-Aware?

All field operation methods in `Rules` produce rules that implement `FieldAwareRule`:

**Single-field operations:**
- `renameField`, `removeField`, `addField`, `transformField`, `setField`

**Batch operations:**
- `renameFields`, `removeFields`

**Structural operations:**
- `groupFields`, `flattenField`, `moveField`, `copyField`

**Path-based operations (nested fields):**
- `renameFieldAt`, `removeFieldAt`, `addFieldAt`, `transformFieldAt`

**Conditional operations:**
- `ifFieldExists`, `ifFieldMissing`, `ifFieldEquals`

**Batch transform:**
- `batch()` via `BatchTransform`

**Composition methods** (`seq`, `seqAll`, `choice`) aggregate field operations from their children. If all children are field-aware, the composed rule is also a `FieldAwareRule`.

**Non-field-aware** (traversal/structural): `all`, `one`, `everywhere`, `bottomUp`, `topDown`, `dynamicTransform` — these do not carry field-level metadata.

## The FieldOperation Record

`FieldOperation` is a record with four components:

| Component         | Type                   | Description                                              |
|-------------------|------------------------|----------------------------------------------------------|
| `operationType`   | `FieldOperationType`   | The kind of field operation (RENAME, REMOVE, etc.)       |
| `fieldPath`       | `List<String>`         | Path segments to the affected field                      |
| `targetFieldName` | `String` (nullable)    | Target field name for operations that have one            |
| `description`     | `String` (nullable)    | Optional human-readable description                      |

### Factory Methods

```java
FieldOperation.rename("old", "new")          // RENAME, ["old"], target="new"
FieldOperation.remove("field")               // REMOVE, ["field"]
FieldOperation.add("field")                  // ADD, ["field"]
FieldOperation.transform("field")            // TRANSFORM, ["field"]
FieldOperation.set("field")                  // SET, ["field"]
FieldOperation.move("a.b", "c.d")            // MOVE, ["a","b"], target="c.d"
FieldOperation.copy("a.b", "c.d")            // COPY, ["a","b"], target="c.d"
FieldOperation.renamePath("pos.x", "posX")   // RENAME, ["pos","x"], target="posX"
FieldOperation.group("pos", "x", "y", "z")   // GROUP, ["x","y","z"], target="pos"
FieldOperation.flatten("position")           // FLATTEN, ["position"]
FieldOperation.conditional("field", "exists") // CONDITIONAL, ["field"], desc="exists"
```

### Convenience Methods

| Method                 | Return Type         | Description                                           |
|------------------------|---------------------|-------------------------------------------------------|
| `fieldPathString()`    | `String`            | Dot-notation path (e.g., `"position.x"`)              |
| `isNested()`           | `boolean`           | `true` if field path has more than one segment        |
| `toSummary()`          | `String`            | Human-readable summary (e.g., `"RENAME(old -> new)"`) |
| `targetFieldNameOpt()` | `Optional<String>`  | Target field name as Optional                         |
| `descriptionOpt()`     | `Optional<String>`  | Description as Optional                               |

## The FieldOperationType Enum

Ten operation types classify what a rule does to a field:

| Constant        | Display Name    | Requires Target | Structural |
|-----------------|-----------------|-----------------|------------|
| `RENAME`        | `"rename"`      | Yes             | No         |
| `REMOVE`        | `"remove"`      | No              | No         |
| `ADD`           | `"add"`         | No              | No         |
| `TRANSFORM`     | `"transform"`   | No              | No         |
| `SET`           | `"set"`         | No              | No         |
| `MOVE`          | `"move"`        | Yes             | Yes        |
| `COPY`          | `"copy"`        | Yes             | Yes        |
| `GROUP`         | `"group"`       | Yes             | Yes        |
| `FLATTEN`       | `"flatten"`     | No              | Yes        |
| `CONDITIONAL`   | `"conditional"` | No              | No         |

### Methods

| Method             | Return Type | Description                                   |
|--------------------|-------------|-----------------------------------------------|
| `displayName()`    | `String`    | Lowercase name (`"rename"`, `"remove"`, etc.) |
| `requiresTarget()` | `boolean`   | `true` for RENAME, MOVE, COPY, GROUP          |
| `isStructural()`   | `boolean`   | `true` for MOVE, COPY, GROUP, FLATTEN         |
| `toString()`       | `String`    | Returns `displayName()`                       |

## The FieldAwareRule Interface

Any rule can be checked for field-level metadata at runtime:

```java
if (rule instanceof FieldAwareRule fieldAware) {
    List<FieldOperation> ops = fieldAware.fieldOperations();
    // inspect field operations
}
```

Custom rules can implement `FieldAwareRule` to participate in field-level diagnostics:

```java
public class MyCustomRule implements TypeRewriteRule, FieldAwareRule {
    @Override
    public Optional<Typed<?>> rewrite(Type<?> type, Typed<?> input) {
        // rule logic
    }

    @Override
    public List<FieldOperation> fieldOperations() {
        return List.of(FieldOperation.transform("myField"));
    }
}
```

## Composition Aggregation

Composition methods aggregate field operations from their children:

```java
TypeRewriteRule composed = Rules.seq(
    Rules.renameField(ops, "old", "new"),     // 1 RENAME
    Rules.addField(ops, "score", defaultVal)  // 1 ADD
);

// composed instanceof FieldAwareRule → true
// fieldOperations() contains 2 entries: RENAME + ADD
```

The same aggregation applies to `seqAll()`, `choice()`, and `batch()`.

If **all** children are non-field-aware (e.g., all are `dynamicTransform` rules), the composition is **not** a `FieldAwareRule`.

## Aggregation in Reports

### Per-Rule

```java
for (RuleApplication rule : fix.ruleApplications()) {
    if (rule.hasFieldOperations()) {
        for (FieldOperation op : rule.fieldOperations()) {
            System.out.println(op.toSummary());
        }
    }
}
```

### Per-Fix

```java
List<FieldOperation> allOps = fix.allFieldOperations();
int count = fix.fieldOperationCount();
```

### Per-Migration

```java
int totalOps = report.totalFieldOperationCount();
```

### Filtering by Type

```java
List<FieldOperation> renames = rule.fieldOperationsOfType(FieldOperationType.RENAME);
```

## Complete Example

```java
import de.splatgames.aether.datafixers.api.diagnostic.*;
import de.splatgames.aether.datafixers.api.rewrite.*;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;

public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(Schema inputSchema, Schema outputSchema) {
        super("PlayerV1ToV2", inputSchema, outputSchema);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            Rules.renameField(ops(), "playerName", "name"),
            Rules.removeField(ops(), "legacyId"),
            Rules.addField(ops(), "score", ops().createInt(0)),
            Rules.transformField(ops(), "health", value ->
                ops().createFloat(Math.max(0f, ops().getFloat(value))))
        );
    }
}

// --- Using the fix with diagnostics ---

DiagnosticContext context = DiagnosticContext.create(
    DiagnosticOptions.builder()
        .captureRuleDetails(true)
        .captureFieldDetails(true)
        .build()
);

Dynamic<JsonElement> result = fixer.update(
    TypeReferences.PLAYER, inputData,
    new DataVersion(1), new DataVersion(2),
    context
);

MigrationReport report = context.getReport();

for (FixExecution fix : report.fixExecutions()) {
    System.out.println(fix.fixName() + ": " + fix.fieldOperationCount() + " field ops");

    for (RuleApplication rule : fix.ruleApplications()) {
        if (rule.hasFieldOperations()) {
            for (FieldOperation op : rule.fieldOperations()) {
                System.out.println("  " + op.toSummary());
            }
        }
    }
}

// Output:
//   PlayerV1ToV2: 4 field ops
//     RENAME(playerName -> name)
//     REMOVE(legacyId)
//     ADD(score)
//     TRANSFORM(health)

System.out.println("Total field operations: " + report.totalFieldOperationCount());
```

## Performance Considerations

- **Field metadata is collected at rule construction time** — there is zero runtime cost for the metadata itself
- `captureFieldDetails(false)` skips the `instanceof` check and list copy in `DiagnosticRuleWrapper`
- `captureRuleDetails` must also be `true` for field details to have any effect
- For production, consider `DiagnosticOptions.minimal()` which disables both rule details and field details

```java
// Development/debugging — full field-level diagnostics
DiagnosticContext devContext = DiagnosticContext.create(DiagnosticOptions.defaults());

// Production — timing only, no field details
DiagnosticContext prodContext = DiagnosticContext.create(DiagnosticOptions.minimal());

// Best: No context for maximum performance
fixer.update(type, data, from, to);  // No context = no overhead
```

## API Reference

### FieldOperation

| Method / Component     | Type                    | Description                                     |
|------------------------|-------------------------|-------------------------------------------------|
| `operationType()`      | `FieldOperationType`    | The kind of field operation                     |
| `fieldPath()`          | `List<String>`          | Path segments to the affected field             |
| `targetFieldName()`    | `String` (nullable)     | Target field name (rename target, destination)  |
| `description()`        | `String` (nullable)     | Optional human-readable description             |
| `fieldPathString()`    | `String`                | Dot-notation path string                        |
| `isNested()`           | `boolean`               | Whether path has more than one segment          |
| `targetFieldNameOpt()` | `Optional<String>`      | Target as Optional                              |
| `descriptionOpt()`     | `Optional<String>`      | Description as Optional                         |
| `toSummary()`          | `String`                | Human-readable summary                          |

### FieldOperationType

| Constant       | `displayName()`  | `requiresTarget()`   | `isStructural()`   |
|----------------|------------------|----------------------|--------------------|
| `RENAME`       | `"rename"`       | `true`               | `false`            |
| `REMOVE`       | `"remove"`       | `false`              | `false`            |
| `ADD`          | `"add"`          | `false`              | `false`            |
| `TRANSFORM`    | `"transform"`    | `false`              | `false`            |
| `SET`          | `"set"`          | `false`              | `false`            |
| `MOVE`         | `"move"`         | `true`               | `true`             |
| `COPY`         | `"copy"`         | `true`               | `true`             |
| `GROUP`        | `"group"`        | `true`               | `true`             |
| `FLATTEN`      | `"flatten"`      | `false`              | `true`             |
| `CONDITIONAL`  | `"conditional"`  | `false`              | `false`            |

### FieldAwareRule

| Method              | Return Type            | Description                     |
|---------------------|------------------------|---------------------------------|
| `fieldOperations()` | `List<FieldOperation>` | Field-level operations metadata |

## Related

- [Use Diagnostics](use-diagnostics.md)
- [Compose Fixes](compose-fixes.md)
- [Batch Operations](batch-operations.md)
- [Conditional Rules](conditional-rules.md)
