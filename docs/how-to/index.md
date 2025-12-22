# How-To Guides

This section contains task-oriented guides that show you how to accomplish specific goals with Aether Datafixers. Each guide is focused on solving a particular problem.

## Field Operations

| Guide | Description |
|-------|-------------|
| [Rename a Field](rename-field.md) | Change a field's name without modifying its value |
| [Add a New Field](add-field.md) | Add fields with default or computed values |
| [Remove a Field](remove-field.md) | Safely remove obsolete fields |
| [Transform Field Values](transform-field.md) | Convert field values (types, formats, etc.) |

## Structural Changes

| Guide | Description |
|-------|-------------|
| [Restructure Data](restructure-data.md) | Nest, flatten, merge, and split structures |
| [Convert Types](convert-types.md) | Change field types (int→string, etc.) |
| [Handle Optional Fields](handle-optional-fields.md) | Work with fields that may be missing |
| [Preserve Unknown Fields](preserve-unknown-fields.md) | Keep fields you don't know about |

## Fix Composition

| Guide | Description |
|-------|-------------|
| [Compose Fixes](compose-fixes.md) | Combine multiple transformation rules |
| [Create a Bootstrap](create-bootstrap.md) | Wire schemas and fixes together |

## Development & Testing

| Guide | Description |
|-------|-------------|
| [Debug Migrations](debug-migrations.md) | Troubleshoot migration issues |
| [Test Migrations](test-migrations.md) | Write unit tests for your fixes |
| [Log Migrations](log-migrations.md) | Add logging to track migration progress |

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

