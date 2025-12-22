# How to Add a New Field

This guide shows how to add new fields during data migration.

## Using Rules.addField

Add a field with a static default:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.addField(TypeReferences.PLAYER, "version", d -> d.createInt(2));
}
```

**Before:**
```json
{"name": "Steve", "level": 10}
```

**After:**
```json
{"name": "Steve", "level": 10, "version": 2}
```

## Computed Default Value

Add a field based on existing data:

```java
return Rules.addField(TypeReferences.PLAYER, "displayName", player -> {
    String name = player.get("name").asString().orElse("Unknown");
    int level = player.get("level").asInt().orElse(1);
    return player.createString(name + " [Lv." + level + "]");
});
```

**Result:**
```json
{"name": "Steve", "level": 10, "displayName": "Steve [Lv.10]"}
```

## Add Multiple Fields

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        Rules.addField(TypeReferences.PLAYER, "version", d -> d.createInt(2)),
        Rules.addField(TypeReferences.PLAYER, "createdAt", d -> d.createLong(System.currentTimeMillis())),
        Rules.addField(TypeReferences.PLAYER, "isActive", d -> d.createBoolean(true))
    );
}
```

## Add Nested Object

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Create a new nested object
    Dynamic<?> metadata = player.emptyMap()
        .set("version", player.createInt(2))
        .set("format", player.createString("v2"));

    return player.set("metadata", metadata);
});
```

**Result:**
```json
{
  "name": "Steve",
  "metadata": {
    "version": 2,
    "format": "v2"
  }
}
```

## Add List Field

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Create empty list
    Dynamic<?> emptyList = player.emptyList();

    return player.set("achievements", emptyList);
});
```

Or with initial values:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    Dynamic<?> achievements = player.emptyList()
        .append(player.createString("first_login"))
        .append(player.createString("tutorial_complete"));

    return player.set("achievements", achievements);
});
```

## Conditional Addition

Only add if field doesn't exist:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    if (player.get("newField").result().isEmpty()) {
        return player.set("newField", player.createInt(0));
    }
    return player;
});
```

## Add Field Derived from Another

Calculate level from experience:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    int experience = player.get("experience").asInt().orElse(0);
    int level = calculateLevel(experience);

    return player.set("level", player.createInt(level));
});

private int calculateLevel(int experience) {
    return Math.max(1, experience / 100);
}
```

## Add Field with Map Structure

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    Dynamic<?> stats = player.emptyMap()
        .set("health", player.createInt(100))
        .set("mana", player.createInt(50))
        .set("stamina", player.createInt(100));

    return player.set("stats", stats);
});
```

**Result:**
```json
{
  "name": "Steve",
  "stats": {
    "health": 100,
    "mana": 50,
    "stamina": 100
  }
}
```

## Add to Nested Object

Add a field inside an existing nested structure:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    return player.update("stats", stats ->
        stats.set("armor", stats.createInt(0))
    );
});
```

## Schema Definition

Update your schema to include the new field:

```java
// New schema (v2)
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType()),
    DSL.field("version", DSL.intType()),  // NEW
    DSL.remainder()
));
```

Or use `optional` for backwards compatibility:

```java
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.optional("version", DSL.intType()),  // Optional for old data
    DSL.remainder()
));
```

## Related

- [Rename a Field](rename-field.md)
- [Remove a Field](remove-field.md)
- [Handle Optional Fields](handle-optional-fields.md)

