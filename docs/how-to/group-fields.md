# How to Group and Flatten Fields

This guide shows how to group flat fields into nested objects or flatten nested objects to the root level.

## Problem

Data structures evolve. You might need to:
- **Group** flat fields (like `x`, `y`, `z`) into a nested object (like `position`)
- **Flatten** a nested object back to the root level
- **Move** or **copy** fields between different paths

## Solution: Grouping Fields

Use `Rules.groupFields()` to collect flat fields into a nested object:

```java
import de.splatgames.aether.datafixers.api.rewrite.Rules;

// Group x, y, z into a "position" object
TypeRewriteRule rule = Rules.groupFields(GsonOps.INSTANCE,
    "position",  // target object name
    "x", "y", "z"  // fields to group
);
```

**Input:**
```json
{
  "name": "Steve",
  "x": 100.5,
  "y": 64.0,
  "z": -200.0,
  "level": 10
}
```

**Output:**
```json
{
  "name": "Steve",
  "position": {
    "x": 100.5,
    "y": 64.0,
    "z": -200.0
  },
  "level": 10
}
```

## Solution: Flattening Fields

Use `Rules.flattenField()` to extract nested fields to the root:

```java
// Flatten "position" object to root level
TypeRewriteRule rule = Rules.flattenField(GsonOps.INSTANCE, "position");
```

**Input:**
```json
{
  "name": "Steve",
  "position": {
    "x": 100.5,
    "y": 64.0,
    "z": -200.0
  },
  "level": 10
}
```

**Output:**
```json
{
  "name": "Steve",
  "x": 100.5,
  "y": 64.0,
  "z": -200.0,
  "level": 10
}
```

## Moving Fields

Use `Rules.moveField()` to move a field from one path to another:

```java
// Move "x" to "position.x" (creates "position" if needed)
TypeRewriteRule rule = Rules.moveField(GsonOps.INSTANCE, "x", "position.x");
```

**Input:**
```json
{
  "x": 100.5,
  "name": "Steve"
}
```

**Output:**
```json
{
  "position": {
    "x": 100.5
  },
  "name": "Steve"
}
```

### Moving Between Nested Paths

```java
// Move from one nested location to another
Rules.moveField(ops, "old.location.x", "new.position.x")
```

## Copying Fields

Use `Rules.copyField()` to copy a field (keeping the original):

```java
// Copy "name" to "displayName"
TypeRewriteRule rule = Rules.copyField(GsonOps.INSTANCE, "name", "displayName");
```

**Input:**
```json
{
  "name": "Steve",
  "level": 10
}
```

**Output:**
```json
{
  "name": "Steve",
  "displayName": "Steve",
  "level": 10
}
```

## Complete Example

A migration that restructures player data:

```java
public class PlayerV2ToV3Fix extends SchemaDataFix {

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            // Group position coordinates
            Rules.groupFields(GsonOps.INSTANCE, "position", "x", "y", "z"),

            // Group player stats
            Rules.groupFields(GsonOps.INSTANCE, "stats", "health", "mana", "stamina"),

            // Copy name to displayName
            Rules.copyField(GsonOps.INSTANCE, "name", "displayName"),

            // Move settings into config object
            Rules.moveField(GsonOps.INSTANCE, "volume", "config.audio.volume"),
            Rules.moveField(GsonOps.INSTANCE, "brightness", "config.video.brightness")
        );
    }
}
```

**Before:**
```json
{
  "name": "Steve",
  "x": 100.5,
  "y": 64.0,
  "z": -200.0,
  "health": 20,
  "mana": 100,
  "stamina": 50,
  "volume": 0.8,
  "brightness": 1.0
}
```

**After:**
```json
{
  "name": "Steve",
  "displayName": "Steve",
  "position": {
    "x": 100.5,
    "y": 64.0,
    "z": -200.0
  },
  "stats": {
    "health": 20,
    "mana": 100,
    "stamina": 50
  },
  "config": {
    "audio": {
      "volume": 0.8
    },
    "video": {
      "brightness": 1.0
    }
  }
}
```

## Path-Based Operations

For more control, use path-based rules with dot notation:

```java
// Transform a nested field
Rules.transformFieldAt(ops, "position.x", d ->
    d.createDouble(d.asDouble().result().orElse(0.0) * 2)
)

// Rename a nested field
Rules.renameFieldAt(ops, "config.audio.vol", "volume")

// Remove a nested field
Rules.removeFieldAt(ops, "config.deprecated")

// Add a nested field (creates parents if needed)
Rules.addFieldAt(ops, "config.graphics.quality",
    new Dynamic<>(ops, ops.createString("high")))
```

## Best Practices

1. **Group related data** - Fields that belong together should be grouped
2. **Use meaningful names** - `position` is clearer than `coords` or `pos`
3. **Consider access patterns** - Structure data based on how it will be used
4. **Test round-trips** - If you flatten in one version, ensure grouping works in the next

## Related

- [Restructure Data](restructure-data.md) - General restructuring patterns
- [Batch Operations](batch-operations.md) - Rename/remove multiple fields
- [Rewrite Rules](../concepts/rewrite-rules.md) - Complete rules reference

