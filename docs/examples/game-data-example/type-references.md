# TypeReferences Pattern

Centralized type identifiers for consistent reference across schemas and fixes.

## Implementation

```java
package com.example.game.data;

import de.splatgames.aether.datafixers.api.TypeReference;

/**
 * Centralized type references for game data types.
 * Use these constants throughout schemas and fixes for consistency.
 */
public final class TypeReferences {

    /**
     * Player data type.
     * Contains player state, position, inventory, etc.
     */
    public static final TypeReference PLAYER = TypeReference.of("player");

    /**
     * World data type.
     * Contains world metadata, seed, time, etc.
     */
    public static final TypeReference WORLD = TypeReference.of("world");

    /**
     * Item data type.
     * Represents items in player inventory or world.
     */
    public static final TypeReference ITEM = TypeReference.of("item");

    /**
     * Entity data type.
     * NPCs, mobs, and other non-player entities.
     */
    public static final TypeReference ENTITY = TypeReference.of("entity");

    /**
     * Game configuration type.
     * Server and client settings.
     */
    public static final TypeReference CONFIG = TypeReference.of("config");

    // Private constructor - utility class
    private TypeReferences() {
        throw new AssertionError("No instances");
    }
}
```

## Usage in Schemas

```java
public class Schema100 extends Schema {

    @Override
    protected void registerTypes() {
        // Use TypeReferences constants
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.remainder()
        ));

        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("seed", DSL.longType()),
            DSL.remainder()
        ));
    }
}
```

## Usage in Fixes

```java
public class PlayerV1ToV2Fix extends SchemaDataFix {

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        // Use TypeReferences constant
        return Rules.renameField(
            TypeReferences.PLAYER,
            "playerName",
            "name"
        );
    }
}
```

## Usage at Runtime

```java
// Tag data with type reference
Dynamic<JsonElement> playerData = new Dynamic<>(GsonOps.INSTANCE, json);
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, playerData);

// Migrate
TaggedDynamic migrated = fixer.update(tagged, fromVersion, toVersion);
```

## Benefits

1. **Type Safety**: Compiler catches typos in type names
2. **Refactoring**: Rename in one place, updates everywhere
3. **Documentation**: Javadoc on constants documents each type
4. **Discoverability**: IDE autocomplete shows available types

## Organizing Types

For larger projects, group related types:

```java
public final class TypeReferences {

    // Player-related types
    public static final TypeReference PLAYER = TypeReference.of("player");
    public static final TypeReference INVENTORY = TypeReference.of("inventory");
    public static final TypeReference ACHIEVEMENTS = TypeReference.of("achievements");

    // World-related types
    public static final TypeReference WORLD = TypeReference.of("world");
    public static final TypeReference CHUNK = TypeReference.of("chunk");
    public static final TypeReference STRUCTURE = TypeReference.of("structure");

    // Entity types
    public static final TypeReference ENTITY = TypeReference.of("entity");
    public static final TypeReference MOB = TypeReference.of("mob");
    public static final TypeReference ITEM_ENTITY = TypeReference.of("item_entity");

    private TypeReferences() {}
}
```

## Related

- [Type Reference Concept](../../concepts/type-reference.md)
- [Schemas](schemas.md)
- [Fixes](fixes.md)

