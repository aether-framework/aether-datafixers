# How-To Guides

This section contains task-oriented guides that show you how to accomplish specific goals with Aether Datafixers. Each guide is focused on solving a particular problem.

## Field Operations

| Guide | Description |
|-------|-------------|
| [Rename a Field](rename-field.md) | Change a field's name without modifying its value |
| [Add a New Field](add-field.md) | Add fields with default or computed values |
| [Remove a Field](remove-field.md) | Safely remove obsolete fields |
| [Transform Field Values](transform-field.md) | Convert field values (types, formats, etc.) |
| [Batch Operations](batch-operations.md) | Rename or remove multiple fields at once |

## Structural Changes

| Guide | Description |
|-------|-------------|
| [Restructure Data](restructure-data.md) | Nest, flatten, merge, and split structures |
| [Group Fields](group-fields.md) | Group flat fields into nested objects |
| [Convert Types](convert-types.md) | Change field types (int→string, etc.) |
| [Handle Optional Fields](handle-optional-fields.md) | Work with fields that may be missing |
| [Preserve Unknown Fields](preserve-unknown-fields.md) | Keep fields you don't know about |

## Fix Composition

| Guide | Description |
|-------|-------------|
| [Compose Fixes](compose-fixes.md) | Combine multiple transformation rules |
| [Conditional Rules](conditional-rules.md) | Apply rules based on field conditions |
| [Create a Bootstrap](create-bootstrap.md) | Wire schemas and fixes together |

## Development & Testing

| Guide | Description |
|-------|-------------|
| [Debug Migrations](debug-migrations.md) | Troubleshoot migration issues |
| [Test Migrations](test-migrations.md) | Write unit tests for your fixes |
| [Log Migrations](log-migrations.md) | Add logging to track migration progress |
| [Use Diagnostics](use-diagnostics.md) | Capture structured migration reports |

## Advanced Usage

| Guide | Description |
|-------|-------------|
| [Use Finders](use-finders.md) | Navigate Dynamic data with Finder optics |
| [Integrate with Gson](integrate-with-gson.md) | Use GsonOps for JSON processing |

---

## Quick Reference

### Common Rules

```java
// Rename
Rules.renameField(TYPE, "old", "new")

// Add
Rules.addField(TYPE, "field", d -> d.createInt(0))

// Remove
Rules.removeField(TYPE, "obsolete")

// Transform value
Rules.transformField(TYPE, "field", value -> transform(value))

// Transform entire object
Rules.transform(TYPE, obj -> transform(obj))

// Combine rules
Rules.seq(rule1, rule2, rule3)
```

### Extended Rules

```java
// Batch operations (multiple rules)
Rules.renameFields(ops, Map.of("old1", "new1", "old2", "new2"))
Rules.removeFields(ops, "field1", "field2", "field3")

// High-performance batch (single encode/decode cycle)
Rules.batch(ops, b -> b
    .rename("playerName", "name")
    .rename("xp", "experience")
    .remove("deprecated")
    .set("version", d -> d.createInt(2))
)

// Grouping and flattening
Rules.groupFields(ops, "position", "x", "y", "z")
Rules.flattenField(ops, "position")

// Move and copy
Rules.moveField(ops, "x", "position.x")
Rules.copyField(ops, "name", "displayName")

// Path-based operations
Rules.transformFieldAt(ops, "position.x", d -> d.createDouble(d.asDouble().orElse(0.0) * 2))
Rules.addFieldAt(ops, "settings.graphics.quality", defaultValue)

// Conditional rules (with TypeRewriteRule)
Rules.ifFieldExists(ops, "legacyField", migrationRule)
Rules.ifFieldMissing(ops, "version", addVersionRule)
Rules.ifFieldEquals(ops, "version", 1, migrateV1Rule)

// Single-pass conditional rules (with Function - more efficient)
Rules.ifFieldExists(ops, "legacy", d -> d.remove("legacy").set("new", d.get("legacy")))
Rules.ifFieldMissing(ops, "version", d -> d.set("version", d.createInt(1)))
Rules.ifFieldEquals(ops, "version", 1, d -> d.set("version", d.createInt(2)))
Rules.conditionalTransform(ops, d -> d.get("type").asString()..., transform)
```

### Common Dynamic Operations

```java
// Read
String name = dynamic.get("name").asString().orElse("default");
int level = dynamic.get("level").asInt().orElse(0);

// Write
dynamic.set("name", dynamic.createString("value"))
dynamic.set("level", dynamic.createInt(42))

// Remove
dynamic.remove("obsolete")

// Nested access
dynamic.get("parent").get("child").asString()

// Update in place
dynamic.update("field", value -> transform(value))
```

---

## Need More?

- **Conceptual Understanding?** See [Concepts](../concepts/index.md)
- **Step-by-Step Learning?** See [Tutorials](../tutorials/index.md)
- **Working Examples?** See [Examples](../examples/index.md)
- **API Details?** See [API Reference](https://software.splatgames.de/docs/aether/aether-datafixers/)

