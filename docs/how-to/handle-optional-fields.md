# How to Handle Optional Fields

This guide shows how to work with fields that may or may not be present in data.

## Reading Optional Fields Safely

Always use `orElse` when reading:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Safe: won't throw if missing
    String name = player.get("name").asString().orElse("Unknown");
    int level = player.get("level").asInt().orElse(1);
    double health = player.get("health").asDouble().orElse(100.0);

    // Continue transformation...
    return player;
});
```

## Check If Field Exists

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Check existence
    if (player.get("optionalField").result().isPresent()) {
        // Field exists, process it
        String value = player.get("optionalField").asString().orElse("");
        return player.set("processedField", player.createString(value.toUpperCase()));
    }
    // Field doesn't exist, leave unchanged
    return player;
});
```

## Set Default for Missing Field

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Add default if missing
    if (player.get("newField").result().isEmpty()) {
        return player.set("newField", player.createInt(0));
    }
    return player;
});
```

## Optional Field in Schema

Use `DSL.optional` for fields that may not exist:

```java
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),           // Required
    DSL.optional("nickname", DSL.string()),    // Optional
    DSL.optional("bio", DSL.string()),         // Optional
    DSL.remainder()
));
```

## Optional Field with Default in Schema

```java
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.optionalFieldOf("level", DSL.intType(), 1),     // Default: 1
    DSL.optionalFieldOf("health", DSL.doubleType(), 100.0), // Default: 100.0
    DSL.remainder()
));
```

## Migrate Nested Optional

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Get optional nested object
    Dynamic<?> maybeSettings = player.get("settings").orElseEmptyMap();

    // Read optional nested fields
    int volume = maybeSettings.get("volume").asInt().orElse(100);
    boolean muted = maybeSettings.get("muted").asBoolean().orElse(false);

    // Process...
    return player;
});
```

## Convert Required to Optional

When a previously required field becomes optional:

```java
// Schema v1: field is required
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("email", DSL.string()),  // Required
    DSL.remainder()
));

// Schema v2: field is now optional
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.optional("email", DSL.string()),  // Optional
    DSL.remainder()
));
```

No fix needed — old data with the field still works.

## Convert Optional to Required

When adding a required field (must provide default):

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // If missing, add with default
    if (player.get("requiredField").result().isEmpty()) {
        return player.set("requiredField", player.createString("default"));
    }
    return player;
});
```

## Nullable vs Missing

Handle both null values and missing fields:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Get the dynamic (may be null or missing)
    OptionalDynamic<?> maybeEmail = player.get("email");

    // Check if present and not null
    String email = maybeEmail.asString()
        .result()
        .filter(s -> !s.isEmpty() && !"null".equals(s))
        .orElse("no-email@example.com");

    return player.set("email", player.createString(email));
});
```

## Preserve or Remove Based on Presence

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Only process if field exists
    OptionalDynamic<?> maybeField = player.get("conditionalField");

    if (maybeField.result().isPresent()) {
        // Transform existing field
        String value = maybeField.asString().orElse("");
        return player.set("conditionalField", player.createString(value.toUpperCase()));
    }

    // Don't add if it didn't exist
    return player;
});
```

## Optional in Lists

Handle optional fields in list items:

```java
return Rules.transform(TypeReferences.INVENTORY, inv -> {
    return inv.update("items", items ->
        items.updateList(item -> {
            // Handle optional fields in each item
            String name = item.get("name").asString().orElse("Unknown Item");
            int count = item.get("count").asInt().orElse(1);
            String rarity = item.get("rarity").asString().orElse("common");

            return item
                .set("displayName", item.createString("[" + rarity + "] " + name));
        })
    );
});
```

## Fallback Chain

Try multiple fields in order:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Try displayName, then name, then "Unknown"
    String displayName = player.get("displayName").asString()
        .result()
        .or(() -> player.get("name").asString().result())
        .orElse("Unknown");

    return player.set("finalName", player.createString(displayName));
});
```

## Best Practices

1. **Always use orElse** — Never assume a field exists
2. **Provide sensible defaults** — Defaults should be safe values
3. **Document optionality** — Note which fields are optional in schemas
4. **Test both cases** — Test with and without optional fields

## Related

- [Add a Field](add-field.md)
- [Preserve Unknown Fields](preserve-unknown-fields.md)
- [Schema System](../concepts/schema-system.md)

