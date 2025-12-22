# How to Remove a Field

This guide shows how to remove obsolete fields during data migration.

## Using Rules.removeField

Remove a single field:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.removeField(TypeReferences.PLAYER, "obsoleteField");
}
```

**Before:**
```json
{"name": "Steve", "level": 10, "obsoleteField": "old_value"}
```

**After:**
```json
{"name": "Steve", "level": 10}
```

## Remove Multiple Fields

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        Rules.removeField(TypeReferences.PLAYER, "oldField1"),
        Rules.removeField(TypeReferences.PLAYER, "oldField2"),
        Rules.removeField(TypeReferences.PLAYER, "deprecatedData")
    );
}
```

## Using Transform

Manual removal with more control:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    return player
        .remove("field1")
        .remove("field2")
        .remove("field3");
});
```

## Remove and Migrate Data First

Save important data before removing:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Extract data from old field before removing
    String oldValue = player.get("legacyName").asString().orElse("");

    // Use it elsewhere if needed
    Dynamic<?> result = player;
    if (!oldValue.isEmpty()) {
        result = result.set("migratedName", player.createString(oldValue));
    }

    // Remove the old field
    return result.remove("legacyName");
});
```

## Conditional Removal

Only remove if the field exists:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    if (player.get("maybeExists").result().isPresent()) {
        return player.remove("maybeExists");
    }
    return player;
});
```

## Remove Nested Field

Remove a field from inside a nested object:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    return player.update("stats", stats ->
        stats.remove("obsoleteStat")
    );
});
```

**Before:**
```json
{
  "name": "Steve",
  "stats": {
    "health": 100,
    "obsoleteStat": "remove_me"
  }
}
```

**After:**
```json
{
  "name": "Steve",
  "stats": {
    "health": 100
  }
}
```

## Remove Entire Nested Object

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    return player.remove("oldNestedObject");
});
```

## Remove from List Items

Remove a field from every item in a list:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    return player.update("inventory", inventory ->
        inventory.updateList(item ->
            item.remove("deprecatedField")
        )
    );
});
```

## Archive Before Removing

Move to an "archive" field instead of deleting:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Get the value to archive
    Dynamic<?> oldData = player.get("legacyData").orElseEmptyMap();

    // Get or create archive
    Dynamic<?> archive = player.get("_archive").orElseEmptyMap();
    archive = archive.set("legacyData", oldData);

    // Update structure
    return player
        .remove("legacyData")
        .set("_archive", archive);
});
```

## Remove Multiple Types

Remove the same field from multiple types:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        Rules.removeField(TypeReferences.PLAYER, "debugInfo"),
        Rules.removeField(TypeReferences.WORLD, "debugInfo"),
        Rules.removeField(TypeReferences.CONFIG, "debugInfo")
    );
}
```

## Schema Definition

Update schema to not include the removed field:

```java
// Old schema (v1)
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("oldField", DSL.string()),  // Will be removed
    DSL.remainder()
));

// New schema (v2)
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    // oldField is gone
    DSL.remainder()
));
```

## Handling Remainder

Using `DSL.remainder()` ensures removed fields don't cause issues with extra data:

```java
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.remainder()  // Unknown fields preserved, but can be removed by fix
));
```

## Best Practices

1. **Document removed fields** — Keep a record of what was removed and why
2. **Consider archiving** — For important data, archive instead of delete
3. **Test thoroughly** — Ensure removal doesn't break dependent data
4. **Update all schemas** — Make sure new schema doesn't expect the field

## Related

- [Rename a Field](rename-field.md)
- [Add a Field](add-field.md)
- [Preserve Unknown Fields](preserve-unknown-fields.md)

