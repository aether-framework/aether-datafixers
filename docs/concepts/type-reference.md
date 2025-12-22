# TypeReference

`TypeReference` is a string-based identifier that uniquely names a data type in Aether Datafixers. It serves as the key for routing data to the appropriate schema definitions and data fixes.

## Overview

```java
package de.splatgames.aether.datafixers.api;

public record TypeReference(String id) {
    // ...
}
```

A `TypeReference` is simply a wrapper around a string identifier. It's used throughout the framework to:

1. Register type definitions in schemas
2. Associate data with its type (via `TaggedDynamic`)
3. Route data to the correct fixes
4. Look up codecs and type information

## Creating TypeReferences

```java
// Create a type reference
TypeReference playerType = new TypeReference("player");
TypeReference worldType = new TypeReference("world");
TypeReference entityType = new TypeReference("entity");

// Get the identifier
String id = playerType.id();      // "player"
String id = playerType.getId();   // "player" (alternative accessor)
```

## The TypeReferences Pattern

The recommended pattern is to define all your type references in a central class:

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.TypeReference;

/**
 * Central registry of all type references used in the game.
 *
 * <p>Each constant identifies a distinct data type that can be
 * versioned and migrated independently.</p>
 */
public final class TypeReferences {

    // ═══════════════════════════════════════════════════════════
    // Player Data
    // ═══════════════════════════════════════════════════════════

    /** Player save data including stats, inventory, and position */
    public static final TypeReference PLAYER = new TypeReference("player");

    /** Player settings and preferences */
    public static final TypeReference PLAYER_SETTINGS = new TypeReference("player_settings");

    // ═══════════════════════════════════════════════════════════
    // World Data
    // ═══════════════════════════════════════════════════════════

    /** World/level metadata */
    public static final TypeReference WORLD = new TypeReference("world");

    /** Chunk data including blocks and entities */
    public static final TypeReference CHUNK = new TypeReference("chunk");

    /** Block entity data (chests, signs, etc.) */
    public static final TypeReference BLOCK_ENTITY = new TypeReference("block_entity");

    // ═══════════════════════════════════════════════════════════
    // Entity Data
    // ═══════════════════════════════════════════════════════════

    /** Base entity data */
    public static final TypeReference ENTITY = new TypeReference("entity");

    /** Item entity data */
    public static final TypeReference ITEM = new TypeReference("item");

    // ═══════════════════════════════════════════════════════════
    // Configuration
    // ═══════════════════════════════════════════════════════════

    /** Game configuration */
    public static final TypeReference CONFIG = new TypeReference("config");

    // Prevent instantiation
    private TypeReferences() {}
}
```

## Using TypeReferences

### In Schema Registration

```java
public class Schema100 extends Schema {
    @Override
    protected void registerTypes() {
        // Register player type
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.remainder()
        ));

        // Register world type
        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("seed", DSL.longType()),
            DSL.remainder()
        ));
    }
}
```

### In DataFix Registration

```java
public class MyBootstrap implements DataFixerBootstrap {
    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Register fixes for specific types
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
        fixes.register(TypeReferences.WORLD, new WorldV1ToV2Fix(schemas));
        fixes.register(TypeReferences.ENTITY, new EntityV1ToV2Fix(schemas));
    }
}
```

### In Rewrite Rules

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        // Rules target specific TypeReferences
        Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
        Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),
        Rules.transform(TypeReferences.PLAYER, this::transformPlayer)
    );
}
```

### In TaggedDynamic

```java
// Tag data with its type
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonData);
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

// The fixer uses the type reference to find applicable fixes
TaggedDynamic migrated = fixer.update(tagged, fromVersion, toVersion);

// Get the type reference back
TypeReference type = migrated.type();  // TypeReferences.PLAYER
```

## Naming Conventions

### Use Descriptive Names

```java
// Good: Clear and descriptive
public static final TypeReference PLAYER = new TypeReference("player");
public static final TypeReference PLAYER_INVENTORY = new TypeReference("player_inventory");
public static final TypeReference BLOCK_ENTITY = new TypeReference("block_entity");

// Avoid: Abbreviated or unclear
public static final TypeReference P = new TypeReference("p");
public static final TypeReference BE = new TypeReference("be");
```

### Use Snake Case for IDs

```java
// Recommended: snake_case for consistency
new TypeReference("player_settings")
new TypeReference("block_entity")
new TypeReference("world_metadata")

// Also acceptable: lowercase without separators
new TypeReference("player")
new TypeReference("world")
```

### Match Domain Concepts

```java
// Good: Matches how you think about your data
TypeReference PLAYER = new TypeReference("player");
TypeReference INVENTORY = new TypeReference("inventory");
TypeReference QUEST = new TypeReference("quest");

// These represent distinct, identifiable data concepts
```

## Hierarchical Types

You can model hierarchical relationships using naming conventions:

```java
// Parent type
public static final TypeReference ENTITY = new TypeReference("entity");

// Child types (specializations)
public static final TypeReference ENTITY_PLAYER = new TypeReference("entity/player");
public static final TypeReference ENTITY_MONSTER = new TypeReference("entity/monster");
public static final TypeReference ENTITY_NPC = new TypeReference("entity/npc");
```

Or use polymorphic types with `TaggedChoice` in the DSL:

```java
registerType(TypeReferences.ENTITY, DSL.taggedChoice(
    "type",
    DSL.string(),
    Map.of(
        "player", playerTemplate,
        "monster", monsterTemplate,
        "npc", npcTemplate
    )
));
```

## Type Reference Equality

`TypeReference` uses the string ID for equality:

```java
TypeReference ref1 = new TypeReference("player");
TypeReference ref2 = new TypeReference("player");
TypeReference ref3 = new TypeReference("world");

ref1.equals(ref2)  // true (same ID)
ref1.equals(ref3)  // false (different ID)

// Safe to use as Map keys
Map<TypeReference, DataFix> fixes = new HashMap<>();
fixes.put(TypeReferences.PLAYER, playerFix);
```

## Best Practices

### 1. Centralize Type References

Always define type references in a single, central class. This:
- Prevents typos and inconsistencies
- Makes discovery easy
- Documents all data types in one place

### 2. Make References Final

```java
// Good: Can't be reassigned
public static final TypeReference PLAYER = new TypeReference("player");

// Bad: Could be reassigned
public static TypeReference PLAYER = new TypeReference("player");
```

### 3. Document Each Reference

```java
/**
 * Player save data.
 *
 * <p>Includes:</p>
 * <ul>
 *   <li>Basic info (name, level)</li>
 *   <li>Position data</li>
 *   <li>Inventory contents</li>
 * </ul>
 *
 * @see Schema100#registerTypes() for v1.0.0 structure
 * @see Schema200#registerTypes() for v2.0.0 structure
 */
public static final TypeReference PLAYER = new TypeReference("player");
```

### 4. One Type Per Logical Entity

Create separate type references for logically distinct data:

```java
// Good: Separate concerns
public static final TypeReference PLAYER = new TypeReference("player");
public static final TypeReference PLAYER_SETTINGS = new TypeReference("player_settings");
public static final TypeReference PLAYER_STATS = new TypeReference("player_stats");

// These can evolve independently
```

### 5. Avoid Generic Names

```java
// Good: Specific and meaningful
public static final TypeReference PLAYER_POSITION = new TypeReference("player_position");

// Avoid: Too generic
public static final TypeReference DATA = new TypeReference("data");
public static final TypeReference OBJECT = new TypeReference("object");
```

---

## Summary

| Aspect | Details |
|--------|---------|
| **Type** | `record TypeReference(String id)` |
| **Purpose** | Identify data types for routing |
| **Usage** | Schema registration, fix routing, tagging data |
| **Best Practice** | Centralize in `TypeReferences` class |
| **Immutability** | Immutable, thread-safe |

---

## Related

- [DataVersion](data-version.md) — Version identifiers for schemas
- [Schema System](schema-system.md) — Registering types per version
- [Dynamic System](dynamic-system.md) — TaggedDynamic for typed data

