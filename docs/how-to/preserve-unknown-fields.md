# How to Preserve Unknown Fields

This guide shows how to ensure fields you don't explicitly handle are preserved during migration.

## The Problem

Without proper handling, unknown fields may be lost:

```json
// Input (v1)
{
  "name": "Steve",
  "level": 10,
  "customMod_data": "important!",  // Unknown to your schema
  "anotherPlugin_settings": {...}   // Unknown to your schema
}

// Output (if not careful)
{
  "name": "Steve",
  "level": 10
  // Custom fields LOST!
}
```

## Use DSL.remainder()

Always include `remainder()` in your schema:

```java
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType()),
    DSL.remainder()  // Captures and preserves all other fields
));
```

With `remainder()`:
```json
// Output
{
  "name": "Steve",
  "level": 10,
  "customMod_data": "important!",     // Preserved!
  "anotherPlugin_settings": {...}      // Preserved!
}
```

## Transform Without Losing Fields

When transforming, avoid recreating the entire object:

### Good: Modify in place

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Good: Just set/remove specific fields
    return player
        .set("name", player.createString("NewName"))
        .remove("oldField");
    // All other fields are preserved
});
```

### Bad: Recreate from scratch

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Bad: Creates new object, loses unknown fields
    return player.emptyMap()
        .set("name", player.createString("NewName"))
        .set("level", player.get("level").orElse(player.createInt(1)));
    // Unknown fields are lost!
});
```

## Explicitly Copy Unknown Fields

If you must rebuild the object:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Start with the original (preserves all fields)
    Dynamic<?> result = player;

    // Now modify what you need
    result = result.set("name", result.createString("NewName"));
    result = result.remove("oldField");

    return result;
});
```

## Merge Unknown Fields

When combining structures:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Create new base structure
    Dynamic<?> newPlayer = player.emptyMap()
        .set("displayName", player.createString("Name"));

    // Merge all fields from original
    player.asMap().result().orElse(Map.of()).forEach((key, value) -> {
        String keyStr = key.asString().orElse("");
        // Skip fields we're replacing
        if (!keyStr.equals("displayName")) {
            newPlayer = newPlayer.set(keyStr, value);
        }
    });

    return newPlayer;
});
```

## Preserve in Nested Objects

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // update() preserves other fields in the nested object
    return player.update("stats", stats -> {
        // Only modify health, preserve everything else in stats
        return stats.set("health", stats.createInt(100));
    });
});
```

## Copy to Archive

Preserve old structure in an archive field:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Archive the entire original structure
    return player.set("_v1_archive", player);
});
```

## Test Preservation

Write tests to verify fields are preserved:

```java
@Test
void testUnknownFieldsPreserved() {
    JsonObject input = new JsonObject();
    input.addProperty("name", "Steve");
    input.addProperty("level", 10);
    input.addProperty("unknownField", "should be preserved");

    // Apply fix
    Dynamic<JsonElement> result = applyFix(input);

    // Verify unknown field is still there
    assertTrue(result.get("unknownField").result().isPresent());
    assertEquals("should be preserved",
        result.get("unknownField").asString().orElse(""));
}
```

## Schema Examples

### Correct: With remainder

```java
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType()),
    DSL.remainder()  // Always include!
));
```

### Incorrect: Without remainder

```java
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType())
    // Missing remainder() - unknown fields may be lost
));
```

## Nested Remainder

```java
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("stats", DSL.and(
        DSL.field("health", DSL.intType()),
        DSL.remainder()  // Preserve unknown stats
    )),
    DSL.remainder()  // Preserve unknown top-level fields
));
```

## Best Practices

1. **Always use `DSL.remainder()`** in all type definitions
2. **Never recreate objects from scratch** unless absolutely necessary
3. **Test with extra fields** to verify preservation
4. **Document known fields** so users know what's recognized

## Related

- [Schema System](../concepts/schema-system.md)
- [DSL Reference](../concepts/dsl.md)
- [Handle Optional Fields](handle-optional-fields.md)

