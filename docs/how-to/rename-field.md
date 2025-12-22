# How to Rename a Field

This guide shows how to rename fields during data migration.

## Using Rules.renameField

The simplest way to rename a field:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.renameField(TypeReferences.PLAYER, "playerName", "name");
}
```

**Before:**
```json
{"playerName": "Steve", "level": 10}
```

**After:**
```json
{"name": "Steve", "level": 10}
```

## Multiple Renames

Rename several fields in one fix:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
        Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),
        Rules.renameField(TypeReferences.PLAYER, "hp", "health")
    );
}
```

## Manual Rename (Using Transform)

When you need more control:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.transform(TypeReferences.PLAYER, player -> {
        // Get old value
        String oldName = player.get("playerName").asString().orElse("Unknown");

        // Remove old field and add new one
        return player
            .remove("playerName")
            .set("name", player.createString(oldName));
    });
}
```

## Rename with Transformation

Rename and modify the value at the same time:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Get and transform
    String name = player.get("playerName").asString().orElse("Unknown");
    String displayName = "[Player] " + name;

    // Apply both changes
    return player
        .remove("playerName")
        .set("displayName", player.createString(displayName));
});
```

## Rename Nested Fields

Rename a field inside a nested object:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Get the nested object
    Dynamic<?> stats = player.get("stats").orElseEmptyMap();

    // Rename within the nested object
    int hp = stats.get("hp").asInt().orElse(100);
    Dynamic<?> newStats = stats
        .remove("hp")
        .set("health", stats.createInt(hp));

    // Put it back
    return player.set("stats", newStats);
});
```

## Conditional Rename

Rename only if the old field exists:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Only rename if oldField exists
    if (player.get("oldField").result().isPresent()) {
        String value = player.get("oldField").asString().orElse("");
        return player
            .remove("oldField")
            .set("newField", player.createString(value));
    }
    return player;
});
```

## Rename Across Multiple Types

Rename the same field in different types:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        Rules.renameField(TypeReferences.PLAYER, "hp", "health"),
        Rules.renameField(TypeReferences.MONSTER, "hp", "health"),
        Rules.renameField(TypeReferences.NPC, "hp", "health")
    );
}
```

## Schema Definition

Remember to update your schema to reflect the new field name:

```java
// Old schema (v1)
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("playerName", DSL.string()),  // Old name
    DSL.remainder()
));

// New schema (v2)
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),  // New name
    DSL.remainder()
));
```

## Common Mistakes

| Mistake | Solution |
|---------|----------|
| Forgetting to remove old field | Use `remove()` before `set()` |
| Wrong field name in rule | Double-check spelling |
| Not updating schema | Both fix and schema must reflect the change |
| Case sensitivity | Field names are case-sensitive |

## Related

- [Add a Field](add-field.md)
- [Remove a Field](remove-field.md)
- [Transform Field Values](transform-field.md)

