# How to Transform Field Values

This guide shows how to transform the values of fields during migration.

## Using Rules.transformField

Transform a single field's value:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.transformField(TypeReferences.PLAYER, "name", value -> {
        String name = value.asString().orElse("");
        return value.createString(name.toUpperCase());
    });
}
```

**Before:**
```json
{"name": "steve", "level": 10}
```

**After:**
```json
{"name": "STEVE", "level": 10}
```

## Int to String Conversion

```java
return Rules.transformField(TypeReferences.PLAYER, "gameMode", value -> {
    int mode = value.asInt().orElse(0);
    String modeName = switch (mode) {
        case 0 -> "survival";
        case 1 -> "creative";
        case 2 -> "adventure";
        case 3 -> "spectator";
        default -> "survival";
    };
    return value.createString(modeName);
});
```

## String to Int Conversion

```java
return Rules.transformField(TypeReferences.PLAYER, "difficulty", value -> {
    String name = value.asString().orElse("normal");
    int level = switch (name.toLowerCase()) {
        case "peaceful" -> 0;
        case "easy" -> 1;
        case "normal" -> 2;
        case "hard" -> 3;
        default -> 2;
    };
    return value.createInt(level);
});
```

## Numeric Transformations

### Scale a Value

```java
// Convert health from 0-100 to 0-20 scale
return Rules.transformField(TypeReferences.PLAYER, "health", value -> {
    int oldHealth = value.asInt().orElse(100);
    int newHealth = oldHealth / 5;  // 100 → 20
    return value.createInt(newHealth);
});
```

### Apply Formula

```java
// Convert experience to level
return Rules.transformField(TypeReferences.PLAYER, "experience", value -> {
    int xp = value.asInt().orElse(0);
    int level = (int) Math.floor(Math.sqrt(xp / 100.0));
    return value.createInt(level);
});
```

## String Transformations

### Format Change

```java
// Convert "first_name last_name" to "last_name, first_name"
return Rules.transformField(TypeReferences.PLAYER, "fullName", value -> {
    String name = value.asString().orElse("");
    String[] parts = name.split(" ", 2);
    if (parts.length == 2) {
        return value.createString(parts[1] + ", " + parts[0]);
    }
    return value;
});
```

### Normalize

```java
// Normalize username
return Rules.transformField(TypeReferences.PLAYER, "username", value -> {
    String username = value.asString().orElse("")
        .toLowerCase()
        .replaceAll("[^a-z0-9_]", "_");
    return value.createString(username);
});
```

## Boolean Transformations

### Invert

```java
return Rules.transformField(TypeReferences.CONFIG, "disabled", value -> {
    boolean disabled = value.asBoolean().orElse(false);
    return value.createBoolean(!disabled);  // Now "enabled"
});
// Also rename the field afterward
```

### From Int

```java
// Convert 0/1 to boolean
return Rules.transformField(TypeReferences.CONFIG, "enabled", value -> {
    int intVal = value.asInt().orElse(0);
    return value.createBoolean(intVal != 0);
});
```

## Transform Multiple Fields

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        Rules.transformField(TypeReferences.PLAYER, "name", this::normalizeName),
        Rules.transformField(TypeReferences.PLAYER, "level", this::clampLevel),
        Rules.transformField(TypeReferences.PLAYER, "gameMode", this::gameModeToString)
    );
}

private Dynamic<?> normalizeName(Dynamic<?> value) {
    return value.createString(value.asString().orElse("").trim());
}

private Dynamic<?> clampLevel(Dynamic<?> value) {
    int level = Math.clamp(value.asInt().orElse(1), 1, 100);
    return value.createInt(level);
}

private Dynamic<?> gameModeToString(Dynamic<?> value) {
    // ... conversion logic
}
```

## Transform Nested Field

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    return player.update("stats", stats ->
        stats.update("health", health -> {
            int value = health.asInt().orElse(100);
            return health.createInt(Math.min(value, 20));  // Cap at 20
        })
    );
});
```

## Conditional Transform

```java
return Rules.transformField(TypeReferences.PLAYER, "status", value -> {
    String status = value.asString().orElse("");

    // Only transform certain values
    if ("active".equals(status) || "online".equals(status)) {
        return value.createString("ONLINE");
    } else if ("inactive".equals(status) || "offline".equals(status)) {
        return value.createString("OFFLINE");
    }

    // Keep unknown values unchanged
    return value;
});
```

## Transform List Values

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    return player.update("tags", tags ->
        tags.updateList(tag -> {
            String value = tag.asString().orElse("");
            return tag.createString(value.toLowerCase());
        })
    );
});
```

## Complex Object Transform

Transform a field from one structure to another:

```java
// Transform {"x": 100, "y": 64} to {"coords": [100, 64]}
return Rules.transformField(TypeReferences.PLAYER, "position", pos -> {
    double x = pos.get("x").asDouble().orElse(0.0);
    double y = pos.get("y").asDouble().orElse(0.0);

    return pos.emptyMap()
        .set("coords", pos.emptyList()
            .append(pos.createDouble(x))
            .append(pos.createDouble(y))
        );
});
```

## Schema Definition

Make sure both schemas reflect the type change:

```java
// Old schema: gameMode is int
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("gameMode", DSL.intType()),
    DSL.remainder()
));

// New schema: gameMode is string
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("gameMode", DSL.string()),
    DSL.remainder()
));
```

## Related

- [Rename a Field](rename-field.md)
- [Convert Types](convert-types.md)
- [Restructure Data](restructure-data.md)

