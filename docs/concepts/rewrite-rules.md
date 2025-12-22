# Rewrite Rules

Rewrite rules define how data is transformed during migrations. The `TypeRewriteRule` interface and `Rules` factory class provide a powerful, composable way to express transformations.

## Overview

A `TypeRewriteRule` represents a transformation that can be applied to data. Rules can be combined and composed to build complex migrations from simple operations.

```
┌─────────────────────────────────────────────────────────────────┐
│                     TypeRewriteRule                              │
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐       │
│  │  rename      │ +  │  transform   │ +  │  restructure │       │
│  │  field       │    │  value       │    │  data        │       │
│  └──────────────┘    └──────────────┘    └──────────────┘       │
│         │                   │                   │               │
│         └───────────────────┴───────────────────┘               │
│                             │                                    │
│                             ▼                                    │
│                    Combined Rule (seq)                           │
└─────────────────────────────────────────────────────────────────┘
```

## The Rules Factory

The `Rules` class provides factory methods for creating common transformations:

```java
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
```

## Basic Rules

### Rename Field

Rename a field without changing its value:

```java
// Rename "playerName" to "name"
Rules.renameField(TypeReferences.PLAYER, "playerName", "name")

// Rename multiple fields
Rules.seq(
    Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
    Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),
    Rules.renameField(TypeReferences.PLAYER, "hp", "health")
)
```

### Transform Field

Transform a field's value:

```java
// Transform a single field
Rules.transformField(
    TypeReferences.PLAYER,
    "gameMode",
    value -> {
        int mode = value.asInt().orElse(0);
        String name = switch (mode) {
            case 0 -> "survival";
            case 1 -> "creative";
            case 2 -> "adventure";
            case 3 -> "spectator";
            default -> "survival";
        };
        return value.createString(name);
    }
)
```

### Transform Type

Transform the entire data structure:

```java
// Transform entire player object
Rules.transform(TypeReferences.PLAYER, player -> {
    // Extract old fields
    double x = player.get("x").asDouble().orElse(0.0);
    double y = player.get("y").asDouble().orElse(0.0);
    double z = player.get("z").asDouble().orElse(0.0);

    // Create nested position
    Dynamic<?> position = player.emptyMap()
        .set("x", player.createDouble(x))
        .set("y", player.createDouble(y))
        .set("z", player.createDouble(z));

    // Update structure
    return player
        .remove("x").remove("y").remove("z")
        .set("position", position);
})
```

### Add Field

Add a new field with a default value:

```java
// Add field with literal default
Rules.addField(TypeReferences.PLAYER, "version", dynamic -> dynamic.createInt(2))

// Add field computed from existing data
Rules.addField(TypeReferences.PLAYER, "displayName", player -> {
    String name = player.get("name").asString().orElse("Unknown");
    int level = player.get("level").asInt().orElse(1);
    return player.createString(name + " [Lv." + level + "]");
})
```

### Remove Field

Remove a field from the data:

```java
// Remove obsolete field
Rules.removeField(TypeReferences.PLAYER, "deprecatedField")

// Remove multiple fields
Rules.seq(
    Rules.removeField(TypeReferences.PLAYER, "old1"),
    Rules.removeField(TypeReferences.PLAYER, "old2"),
    Rules.removeField(TypeReferences.PLAYER, "old3")
)
```

## Combining Rules

### Sequence (seq)

Apply rules in order:

```java
Rules.seq(
    Rules.renameField(TYPE, "a", "b"),
    Rules.transformField(TYPE, "b", transformer),
    Rules.addField(TYPE, "c", defaultValue)
)
```

**Order matters** — later rules see the result of earlier rules:

```java
// Correct: rename first, then transform
Rules.seq(
    Rules.renameField(TYPE, "oldName", "newName"),
    Rules.transformField(TYPE, "newName", transform)  // Uses new name
)

// Incorrect: transform uses old name
Rules.seq(
    Rules.transformField(TYPE, "newName", transform),  // Field doesn't exist yet!
    Rules.renameField(TYPE, "oldName", "newName")
)
```

### All

Apply multiple rules (order doesn't matter for independent operations):

```java
Rules.all(
    Rules.renameField(TYPE, "a", "x"),  // Independent
    Rules.renameField(TYPE, "b", "y"),  // Independent
    Rules.renameField(TYPE, "c", "z")   // Independent
)
```

### Conditional

Apply a rule only if a condition is met:

```java
Rules.conditional(
    // Condition: check if field exists
    player -> player.get("oldFormat").result().isPresent(),
    // Rule to apply if true
    Rules.transform(TYPE, this::migrateOldFormat)
)
```

## Complete Fix Example

```java
public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super("player_v1_to_v2", new DataVersion(100), new DataVersion(110), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            // 1. Field renames
            Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
            Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),

            // 2. Type conversions
            Rules.transformField(TypeReferences.PLAYER, "gameMode", this::gameModeToString),
            Rules.transformField(TypeReferences.PLAYER, "health", this::floatToDouble),

            // 3. Structure changes
            Rules.transform(TypeReferences.PLAYER, this::groupPosition),

            // 4. New fields
            Rules.addField(TypeReferences.PLAYER, "version", d -> d.createInt(2)),

            // 5. Removed fields
            Rules.removeField(TypeReferences.PLAYER, "obsoleteField")
        );
    }

    private Dynamic<?> gameModeToString(Dynamic<?> value) {
        int mode = value.asInt().orElse(0);
        return value.createString(switch (mode) {
            case 0 -> "survival";
            case 1 -> "creative";
            case 2 -> "adventure";
            case 3 -> "spectator";
            default -> "survival";
        });
    }

    private Dynamic<?> floatToDouble(Dynamic<?> value) {
        float f = value.asFloat().orElse(0f);
        return value.createDouble(f);
    }

    private Dynamic<?> groupPosition(Dynamic<?> player) {
        double x = player.get("x").asDouble().orElse(0.0);
        double y = player.get("y").asDouble().orElse(0.0);
        double z = player.get("z").asDouble().orElse(0.0);

        Dynamic<?> position = player.emptyMap()
            .set("x", player.createDouble(x))
            .set("y", player.createDouble(y))
            .set("z", player.createDouble(z));

        return player
            .remove("x").remove("y").remove("z")
            .set("position", position);
    }
}
```

## Multi-Type Rules

A single rule can transform multiple types:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        // Player transformations
        Rules.renameField(TypeReferences.PLAYER, "hp", "health"),
        Rules.transform(TypeReferences.PLAYER, this::migratePlayer),

        // Entity transformations
        Rules.renameField(TypeReferences.ENTITY, "hp", "health"),
        Rules.addField(TypeReferences.ENTITY, "age", d -> d.createInt(0)),

        // World transformations
        Rules.transformField(TypeReferences.WORLD, "difficulty", this::diffToString)
    );
}
```

## Traversal Strategies

Rules can specify how to traverse nested data:

### Top-Down

Apply rule from root to leaves:

```java
Rules.topDown(Rules.transform(TYPE, transformer))
```

### Bottom-Up

Apply rule from leaves to root:

```java
Rules.bottomUp(Rules.transform(TYPE, transformer))
```

### Everywhere

Apply rule at every matching node:

```java
Rules.everywhere(Rules.renameField(TYPE, "old", "new"))
```

## Common Patterns

### Migrate Enum Values

```java
private Dynamic<?> migrateEnumValue(Dynamic<?> value) {
    String oldValue = value.asString().orElse("");

    String newValue = switch (oldValue) {
        case "EASY" -> "easy";
        case "NORMAL" -> "normal";
        case "HARD" -> "hard";
        default -> oldValue.toLowerCase();
    };

    return value.createString(newValue);
}
```

### Flatten Nested Object

```java
private Dynamic<?> flattenPosition(Dynamic<?> data) {
    double x = data.get("position").get("x").asDouble().orElse(0.0);
    double y = data.get("position").get("y").asDouble().orElse(0.0);
    double z = data.get("position").get("z").asDouble().orElse(0.0);

    return data
        .remove("position")
        .set("x", data.createDouble(x))
        .set("y", data.createDouble(y))
        .set("z", data.createDouble(z));
}
```

### Nest Flat Fields

```java
private Dynamic<?> nestPosition(Dynamic<?> data) {
    double x = data.get("x").asDouble().orElse(0.0);
    double y = data.get("y").asDouble().orElse(0.0);
    double z = data.get("z").asDouble().orElse(0.0);

    Dynamic<?> position = data.emptyMap()
        .set("x", data.createDouble(x))
        .set("y", data.createDouble(y))
        .set("z", data.createDouble(z));

    return data
        .remove("x").remove("y").remove("z")
        .set("position", position);
}
```

### Transform List Items

```java
private Dynamic<?> transformInventory(Dynamic<?> player) {
    return player.update("inventory", inventory ->
        inventory.updateList(item -> {
            // Transform each item
            String id = item.get("id").asString().orElse("");
            String newId = migrateItemId(id);
            return item.set("id", item.createString(newId));
        })
    );
}
```

### Split Field

```java
private Dynamic<?> splitFullName(Dynamic<?> data) {
    String fullName = data.get("fullName").asString().orElse("Unknown");
    String[] parts = fullName.split(" ", 2);

    String firstName = parts[0];
    String lastName = parts.length > 1 ? parts[1] : "";

    return data
        .remove("fullName")
        .set("firstName", data.createString(firstName))
        .set("lastName", data.createString(lastName));
}
```

### Merge Fields

```java
private Dynamic<?> mergeNames(Dynamic<?> data) {
    String first = data.get("firstName").asString().orElse("");
    String last = data.get("lastName").asString().orElse("");
    String full = (first + " " + last).trim();

    return data
        .remove("firstName")
        .remove("lastName")
        .set("name", data.createString(full));
}
```

## Rules Reference

| Rule | Description |
|------|-------------|
| `renameField(type, old, new)` | Rename a field |
| `transformField(type, field, fn)` | Transform a field's value |
| `transform(type, fn)` | Transform entire structure |
| `addField(type, name, defaultFn)` | Add a new field |
| `removeField(type, name)` | Remove a field |
| `seq(rules...)` | Apply rules in sequence |
| `all(rules...)` | Apply all rules |
| `conditional(pred, rule)` | Conditional application |
| `topDown(rule)` | Traverse top to bottom |
| `bottomUp(rule)` | Traverse bottom to top |
| `everywhere(rule)` | Apply at all matching nodes |

## Best Practices

### 1. Order Rules Logically

```java
Rules.seq(
    // 1. Renames first (fix field names)
    Rules.renameField(...),

    // 2. Type conversions (fix value types)
    Rules.transformField(...),

    // 3. Structural changes (reorganize)
    Rules.transform(...),

    // 4. Additions (new fields)
    Rules.addField(...),

    // 5. Removals (cleanup)
    Rules.removeField(...)
)
```

### 2. Handle Missing Data

```java
private Dynamic<?> transform(Dynamic<?> data) {
    // Always use orElse for safety
    String value = data.get("field").asString().orElse("default");
    return data.set("field", data.createString(value.toUpperCase()));
}
```

### 3. Keep Transformations Pure

```java
// Good: Pure function, no side effects
private Dynamic<?> transform(Dynamic<?> data) {
    return data.set("value", data.createInt(42));
}

// Avoid: Side effects
private Dynamic<?> badTransform(Dynamic<?> data) {
    counter++;  // Side effect!
    log.info("Transforming...");  // Side effect!
    return data;
}
```

### 4. Test Rules Independently

Write unit tests for each transformation:

```java
@Test
void testGameModeConversion() {
    Dynamic<?> input = createDynamic(Map.of("gameMode", 0));
    Dynamic<?> output = gameModeToString(input);
    assertEquals("survival", output.get("gameMode").asString().orElse(""));
}
```

---

## Summary

| Concept | Purpose |
|---------|---------|
| `TypeRewriteRule` | Transformation definition |
| `Rules` | Factory for common rules |
| `seq` | Sequential combination |
| `all` | Parallel combination |
| Traversal | Top-down, bottom-up, everywhere |

---

## Related

- [DataFix System](datafix-system.md) — Where rules are used
- [Dynamic System](dynamic-system.md) — Data manipulation
- [Schema System](schema-system.md) — Input/output schemas

