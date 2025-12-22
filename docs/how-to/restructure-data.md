# How to Restructure Data

This guide shows how to change the structure of your data — nesting, flattening, merging, and splitting objects.

## Nesting Flat Fields

Group related flat fields into a nested object:

```java
// Before: {"x": 100, "y": 64, "z": -200}
// After:  {"position": {"x": 100, "y": 64, "z": -200}}

return Rules.transform(TypeReferences.PLAYER, player -> {
    // Extract values
    double x = player.get("x").asDouble().orElse(0.0);
    double y = player.get("y").asDouble().orElse(0.0);
    double z = player.get("z").asDouble().orElse(0.0);

    // Create nested object
    Dynamic<?> position = player.emptyMap()
        .set("x", player.createDouble(x))
        .set("y", player.createDouble(y))
        .set("z", player.createDouble(z));

    // Update structure
    return player
        .remove("x").remove("y").remove("z")
        .set("position", position);
});
```

## Flattening Nested Objects

Extract nested fields to the top level:

```java
// Before: {"position": {"x": 100, "y": 64, "z": -200}}
// After:  {"x": 100, "y": 64, "z": -200}

return Rules.transform(TypeReferences.PLAYER, player -> {
    Dynamic<?> position = player.get("position").orElseEmptyMap();

    double x = position.get("x").asDouble().orElse(0.0);
    double y = position.get("y").asDouble().orElse(0.0);
    double z = position.get("z").asDouble().orElse(0.0);

    return player
        .remove("position")
        .set("x", player.createDouble(x))
        .set("y", player.createDouble(y))
        .set("z", player.createDouble(z));
});
```

## Merging Two Objects

Combine multiple nested objects into one:

```java
// Before: {"position": {"x": 100}, "rotation": {"yaw": 90}}
// After:  {"transform": {"x": 100, "yaw": 90}}

return Rules.transform(TypeReferences.ENTITY, entity -> {
    Dynamic<?> position = entity.get("position").orElseEmptyMap();
    Dynamic<?> rotation = entity.get("rotation").orElseEmptyMap();

    Dynamic<?> transform = entity.emptyMap()
        .set("x", position.get("x").orElse(entity.createDouble(0.0)))
        .set("y", position.get("y").orElse(entity.createDouble(0.0)))
        .set("z", position.get("z").orElse(entity.createDouble(0.0)))
        .set("yaw", rotation.get("yaw").orElse(entity.createFloat(0.0f)))
        .set("pitch", rotation.get("pitch").orElse(entity.createFloat(0.0f)));

    return entity
        .remove("position")
        .remove("rotation")
        .set("transform", transform);
});
```

## Splitting One Object

Divide one nested object into multiple:

```java
// Before: {"transform": {"x": 100, "y": 64, "yaw": 90}}
// After:  {"position": {"x": 100, "y": 64}, "rotation": {"yaw": 90}}

return Rules.transform(TypeReferences.ENTITY, entity -> {
    Dynamic<?> transform = entity.get("transform").orElseEmptyMap();

    Dynamic<?> position = entity.emptyMap()
        .set("x", transform.get("x").orElse(entity.createDouble(0.0)))
        .set("y", transform.get("y").orElse(entity.createDouble(0.0)))
        .set("z", transform.get("z").orElse(entity.createDouble(0.0)));

    Dynamic<?> rotation = entity.emptyMap()
        .set("yaw", transform.get("yaw").orElse(entity.createFloat(0.0f)))
        .set("pitch", transform.get("pitch").orElse(entity.createFloat(0.0f)));

    return entity
        .remove("transform")
        .set("position", position)
        .set("rotation", rotation);
});
```

## Restructure List Items

Change the structure of items in a list:

```java
// Before: [{"id": "a", "count": 1}, {"id": "b", "count": 2}]
// After:  [{"item": {"id": "a"}, "quantity": 1}, ...]

return Rules.transform(TypeReferences.INVENTORY, inv -> {
    return inv.update("items", items ->
        items.updateList(item -> {
            String id = item.get("id").asString().orElse("");
            int count = item.get("count").asInt().orElse(1);

            Dynamic<?> itemObj = item.emptyMap()
                .set("id", item.createString(id));

            return item.emptyMap()
                .set("item", itemObj)
                .set("quantity", item.createInt(count));
        })
    );
});
```

## Wrap with Container

Add a wrapper object around data:

```java
// Before: {"name": "Steve", "level": 10}
// After:  {"data": {"name": "Steve", "level": 10}, "version": 2}

return Rules.transform(TypeReferences.SAVE, save -> {
    // The current save becomes the "data" field
    Dynamic<?> data = save;

    return save.emptyMap()
        .set("data", data)
        .set("version", save.createInt(2));
});
```

## Unwrap Container

Remove a wrapper object:

```java
// Before: {"data": {"name": "Steve", "level": 10}, "version": 2}
// After:  {"name": "Steve", "level": 10}

return Rules.transform(TypeReferences.SAVE, save -> {
    return save.get("data").orElseEmptyMap();
});
```

## Move Field to Different Location

Move a field from one nested path to another:

```java
// Before: {"player": {"config": {"volume": 80}}}
// After:  {"settings": {"audio": {"volume": 80}}}

return Rules.transform(TypeReferences.SAVE, save -> {
    // Extract the value
    int volume = save.get("player").get("config").get("volume")
        .asInt().orElse(100);

    // Build new structure
    Dynamic<?> audio = save.emptyMap()
        .set("volume", save.createInt(volume));
    Dynamic<?> settings = save.emptyMap()
        .set("audio", audio);

    // Remove old, add new
    return save
        .update("player", player -> player.remove("config"))
        .set("settings", settings);
});
```

## Convert Map to List

Change from map structure to list:

```java
// Before: {"items": {"sword": 1, "apple": 64}}
// After:  {"items": [{"id": "sword", "count": 1}, {"id": "apple", "count": 64}]}

return Rules.transform(TypeReferences.INVENTORY, inv -> {
    Dynamic<?> itemsMap = inv.get("items").orElseEmptyMap();

    // Convert map entries to list items
    List<Dynamic<?>> itemsList = new ArrayList<>();
    itemsMap.asMap().result().orElse(Map.of()).forEach((key, value) -> {
        String id = key.asString().orElse("");
        int count = value.asInt().orElse(1);

        Dynamic<?> item = inv.emptyMap()
            .set("id", inv.createString(id))
            .set("count", inv.createInt(count));
        itemsList.add(item);
    });

    Dynamic<?> listDynamic = inv.emptyList();
    for (Dynamic<?> item : itemsList) {
        listDynamic = listDynamic.append(item);
    }

    return inv.set("items", listDynamic);
});
```

## Convert List to Map

Change from list to map structure:

```java
// Before: {"items": [{"id": "sword", "count": 1}, {"id": "apple", "count": 64}]}
// After:  {"items": {"sword": 1, "apple": 64}}

return Rules.transform(TypeReferences.INVENTORY, inv -> {
    Dynamic<?> itemsMap = inv.emptyMap();

    inv.get("items").asStream().orElse(Stream.empty()).forEach(item -> {
        String id = item.get("id").asString().orElse("");
        int count = item.get("count").asInt().orElse(1);
        // Note: itemsMap must be updated in place or accumulated differently
    });

    // Build the map
    Dynamic<?> finalMap = inv.emptyMap();
    for (Dynamic<?> item : inv.get("items").asList().orElse(List.of())) {
        String id = item.get("id").asString().orElse("");
        int count = item.get("count").asInt().orElse(1);
        finalMap = finalMap.set(id, inv.createInt(count));
    }

    return inv.set("items", finalMap);
});
```

## Best Practices

1. **Extract helper methods** for complex restructuring
2. **Handle missing data** with `orElse` defaults
3. **Test thoroughly** with sample data
4. **Document the before/after** structure in comments

## Related

- [Transform Field Values](transform-field.md)
- [Nested Transformations](../tutorials/nested-transformations.md)
- [Preserve Unknown Fields](preserve-unknown-fields.md)

