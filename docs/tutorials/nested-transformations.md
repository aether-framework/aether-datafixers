# Nested Transformations Tutorial

This tutorial shows how to transform deeply nested data structures during migrations. You'll learn patterns for restructuring, flattening, and nesting data.

## Goal

Learn how to:
- Flatten nested objects to top-level fields
- Nest flat fields into objects
- Transform data within nested structures
- Handle lists of nested objects

## Prerequisites

- Completed [Basic Migration](basic-migration.md) tutorial
- Understanding of the [Dynamic System](../concepts/dynamic-system.md)

## Scenario: Restructuring Player Data

We need to migrate player data through three structural changes:

**v1** (flat):
```json
{
  "name": "Steve",
  "x": 100.5, "y": 64.0, "z": -200.0,
  "health": 20, "maxHealth": 20, "mana": 100
}
```

**v2** (nested position):
```json
{
  "name": "Steve",
  "position": {"x": 100.5, "y": 64.0, "z": -200.0},
  "health": 20, "maxHealth": 20, "mana": 100
}
```

**v3** (nested stats):
```json
{
  "name": "Steve",
  "position": {"x": 100.5, "y": 64.0, "z": -200.0},
  "stats": {"health": 20, "maxHealth": 20, "mana": 100}
}
```

## Pattern 1: Nesting Flat Fields

### v1 → v2: Group x/y/z into position

```java
public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super("player_v1_to_v2", new DataVersion(1), new DataVersion(2), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.PLAYER, this::nestPosition);
    }

    private Dynamic<?> nestPosition(Dynamic<?> player) {
        // 1. Extract flat fields
        double x = player.get("x").asDouble().orElse(0.0);
        double y = player.get("y").asDouble().orElse(0.0);
        double z = player.get("z").asDouble().orElse(0.0);

        // 2. Create nested structure
        Dynamic<?> position = player.emptyMap()
            .set("x", player.createDouble(x))
            .set("y", player.createDouble(y))
            .set("z", player.createDouble(z));

        // 3. Remove old fields and add nested object
        return player
            .remove("x")
            .remove("y")
            .remove("z")
            .set("position", position);
    }
}
```

### v2 → v3: Group stats fields

```java
public class PlayerV2ToV3Fix extends SchemaDataFix {

    public PlayerV2ToV3Fix(SchemaRegistry schemas) {
        super("player_v2_to_v3", new DataVersion(2), new DataVersion(3), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.PLAYER, this::nestStats);
    }

    private Dynamic<?> nestStats(Dynamic<?> player) {
        // 1. Extract stat fields
        int health = player.get("health").asInt().orElse(20);
        int maxHealth = player.get("maxHealth").asInt().orElse(20);
        int mana = player.get("mana").asInt().orElse(100);

        // 2. Create stats object
        Dynamic<?> stats = player.emptyMap()
            .set("health", player.createInt(health))
            .set("maxHealth", player.createInt(maxHealth))
            .set("mana", player.createInt(mana));

        // 3. Update structure
        return player
            .remove("health")
            .remove("maxHealth")
            .remove("mana")
            .set("stats", stats);
    }
}
```

## Pattern 2: Flattening Nested Objects

Going the other direction — extracting nested fields to top level:

```java
private Dynamic<?> flattenPosition(Dynamic<?> player) {
    // 1. Get nested object
    Dynamic<?> position = player.get("position").orElseEmptyMap();

    // 2. Extract values
    double x = position.get("x").asDouble().orElse(0.0);
    double y = position.get("y").asDouble().orElse(0.0);
    double z = position.get("z").asDouble().orElse(0.0);

    // 3. Flatten to top level
    return player
        .remove("position")
        .set("x", player.createDouble(x))
        .set("y", player.createDouble(y))
        .set("z", player.createDouble(z));
}
```

## Pattern 3: Transforming Within Nested Structures

Modify values inside a nested object without changing structure:

```java
private Dynamic<?> healPlayer(Dynamic<?> player) {
    // Update stats.health to stats.maxHealth
    return player.update("stats", stats -> {
        int maxHealth = stats.get("maxHealth").asInt().orElse(20);
        return stats.set("health", stats.createInt(maxHealth));
    });
}

private Dynamic<?> doubleStats(Dynamic<?> player) {
    // Double all stat values
    return player.update("stats", stats ->
        stats
            .update("health", v -> v.createInt(v.asInt().orElse(0) * 2))
            .update("maxHealth", v -> v.createInt(v.asInt().orElse(0) * 2))
            .update("mana", v -> v.createInt(v.asInt().orElse(0) * 2))
    );
}
```

## Pattern 4: Deep Nested Access

For deeply nested structures:

```java
// Structure: player.inventory.equipped.weapon.damage

private Dynamic<?> upgradeWeaponDamage(Dynamic<?> player) {
    return player.update("inventory", inventory ->
        inventory.update("equipped", equipped ->
            equipped.update("weapon", weapon ->
                weapon.update("damage", damage ->
                    damage.createInt(damage.asInt().orElse(0) + 5)
                )
            )
        )
    );
}
```

### Helper Method for Deep Updates

```java
private Dynamic<?> updatePath(Dynamic<?> root, List<String> path, Function<Dynamic<?>, Dynamic<?>> transform) {
    if (path.isEmpty()) {
        return transform.apply(root);
    }

    String head = path.get(0);
    List<String> tail = path.subList(1, path.size());

    return root.update(head, nested -> updatePath(nested, tail, transform));
}

// Usage
Dynamic<?> updated = updatePath(player,
    List.of("inventory", "equipped", "weapon", "damage"),
    damage -> damage.createInt(damage.asInt().orElse(0) + 5)
);
```

## Pattern 5: Transforming Lists

### Transform Each Item

```java
// Upgrade all items in inventory
private Dynamic<?> upgradeInventory(Dynamic<?> player) {
    return player.update("inventory", inventory ->
        inventory.update("items", items ->
            items.updateList(this::upgradeItem)
        )
    );
}

private Dynamic<?> upgradeItem(Dynamic<?> item) {
    // Add enchantments field to each item
    if (item.get("enchantments").result().isEmpty()) {
        return item.set("enchantments", item.emptyList());
    }
    return item;
}
```

### Filter and Transform

```java
// Only transform items matching a condition
private Dynamic<?> transformMatchingItems(Dynamic<?> player) {
    return player.update("inventory", inventory ->
        inventory.updateList(item -> {
            String type = item.get("type").asString().orElse("");
            if ("weapon".equals(type)) {
                // Transform weapons
                return upgradeWeapon(item);
            }
            return item;  // Leave non-weapons unchanged
        })
    );
}
```

### Restructure List Items

```java
// Change item structure: {id, count, damage} → {id, count, meta: {damage}}
private Dynamic<?> restructureItems(Dynamic<?> inventory) {
    return inventory.updateList(item -> {
        int damage = item.get("damage").asInt().orElse(0);

        Dynamic<?> meta = item.emptyMap()
            .set("damage", item.createInt(damage));

        return item
            .remove("damage")
            .set("meta", meta);
    });
}
```

## Pattern 6: Merging Objects

Combine two nested objects into one:

```java
// Merge position and rotation into transform
private Dynamic<?> mergeToTransform(Dynamic<?> entity) {
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
}
```

## Pattern 7: Splitting Objects

Split one nested object into multiple:

```java
// Split transform into position and rotation
private Dynamic<?> splitTransform(Dynamic<?> entity) {
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
}
```

## Complete Example

```java
public class PlayerV1ToV3Fix extends SchemaDataFix {

    public PlayerV1ToV3Fix(SchemaRegistry schemas) {
        super("player_v1_to_v3", new DataVersion(1), new DataVersion(3), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.PLAYER, this::restructurePlayer);
    }

    private Dynamic<?> restructurePlayer(Dynamic<?> player) {
        // Chain all transformations
        return nestPosition(nestStats(player));
    }

    private Dynamic<?> nestPosition(Dynamic<?> player) {
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

    private Dynamic<?> nestStats(Dynamic<?> player) {
        int health = player.get("health").asInt().orElse(20);
        int maxHealth = player.get("maxHealth").asInt().orElse(20);
        int mana = player.get("mana").asInt().orElse(100);

        Dynamic<?> stats = player.emptyMap()
            .set("health", player.createInt(health))
            .set("maxHealth", player.createInt(maxHealth))
            .set("mana", player.createInt(mana));

        return player
            .remove("health").remove("maxHealth").remove("mana")
            .set("stats", stats);
    }
}
```

## Best Practices

### 1. Always Use orElse for Safe Access

```java
// Good: Handles missing fields
double x = player.get("x").asDouble().orElse(0.0);

// Bad: May throw
double x = player.get("x").asDouble().result().get();
```

### 2. Chain Operations Fluently

```java
// Good: Clear and readable
return player
    .remove("old1")
    .remove("old2")
    .set("new1", value1)
    .set("new2", value2);
```

### 3. Extract Helper Methods

```java
// Good: Reusable and testable
private Dynamic<?> createPosition(Dynamic<?> context, double x, double y, double z) {
    return context.emptyMap()
        .set("x", context.createDouble(x))
        .set("y", context.createDouble(y))
        .set("z", context.createDouble(z));
}
```

### 4. Test Each Transformation

```java
@Test
void testNestPosition() {
    JsonObject flat = new JsonObject();
    flat.addProperty("x", 100.5);
    flat.addProperty("y", 64.0);
    flat.addProperty("z", -200.0);

    Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, flat);
    Dynamic<JsonElement> output = fix.nestPosition(input);

    assertTrue(output.get("position").result().isPresent());
    assertEquals(100.5, output.get("position").get("x").asDouble().orElse(0.0));
}
```

## Next Steps

- **[Custom DynamicOps](custom-dynamicops.md)** — Support new formats
- **[Rewrite Rules](../concepts/rewrite-rules.md)** — Rule combinators
- **[How-To: Restructure Data](../how-to/restructure-data.md)** — More patterns

