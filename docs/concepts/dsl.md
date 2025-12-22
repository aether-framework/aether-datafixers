# DSL (Domain-Specific Language)

The DSL provides a declarative way to define type structures for your schemas. It's a fluent API for building `TypeTemplate` instances that describe what your data looks like at each version.

## Overview

The DSL is accessed through the `DSL` class, which provides factory methods for creating type templates:

```java
import de.splatgames.aether.datafixers.api.dsl.DSL;

// Define a type structure
DSL.TypeTemplate playerType = DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType()),
    DSL.field("position", DSL.and(
        DSL.field("x", DSL.doubleType()),
        DSL.field("y", DSL.doubleType()),
        DSL.field("z", DSL.doubleType())
    )),
    DSL.remainder()
);
```

## Primitive Types

The DSL provides templates for all primitive types:

```java
DSL.string()      // String values
DSL.intType()     // int values
DSL.longType()    // long values
DSL.floatType()   // float values
DSL.doubleType()  // double values
DSL.bool()        // boolean values
DSL.byteType()    // byte values
DSL.shortType()   // short values
```

### Usage

```java
registerType(TypeReferences.CONFIG, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("maxPlayers", DSL.intType()),
    DSL.field("seed", DSL.longType()),
    DSL.field("spawnRadius", DSL.floatType()),
    DSL.field("worldScale", DSL.doubleType()),
    DSL.field("hardcore", DSL.bool())
));
```

## Field Templates

### Required Field

`field(name, template)` defines a required field:

```java
// Required string field
DSL.field("name", DSL.string())

// Required nested object
DSL.field("position", DSL.and(
    DSL.field("x", DSL.doubleType()),
    DSL.field("y", DSL.doubleType()),
    DSL.field("z", DSL.doubleType())
))
```

### Optional Field

`optional(name, template)` defines an optional field:

```java
// Optional string field
DSL.optional("nickname", DSL.string())

// Optional nested object
DSL.optional("metadata", DSL.and(
    DSL.field("createdAt", DSL.longType()),
    DSL.field("modifiedAt", DSL.longType())
))
```

### Default Field

`optionalFieldOf(name, template, default)` with a default value:

```java
// String with default
DSL.optionalFieldOf("difficulty", DSL.string(), "normal")

// Int with default
DSL.optionalFieldOf("lives", DSL.intType(), 3)
```

## Composite Templates

### And (Product Type)

`and(templates...)` combines multiple templates into a product type:

```java
// Multiple fields together
DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType()),
    DSL.field("active", DSL.bool())
)

// Nested structure
DSL.and(
    DSL.field("player", DSL.and(
        DSL.field("name", DSL.string()),
        DSL.field("level", DSL.intType())
    )),
    DSL.field("world", DSL.and(
        DSL.field("name", DSL.string()),
        DSL.field("seed", DSL.longType())
    ))
)
```

### List

`list(template)` defines a list/array of elements:

```java
// List of strings
DSL.list(DSL.string())

// List of objects
DSL.list(DSL.and(
    DSL.field("id", DSL.intType()),
    DSL.field("name", DSL.string())
))

// Nested lists
DSL.list(DSL.list(DSL.intType()))  // 2D array
```

### Example: Inventory

```java
registerType(TypeReferences.INVENTORY, DSL.and(
    DSL.field("slots", DSL.intType()),
    DSL.field("items", DSL.list(DSL.and(
        DSL.field("id", DSL.string()),
        DSL.field("count", DSL.intType()),
        DSL.optional("nbt", DSL.remainder())
    ))),
    DSL.remainder()
));
```

## Remainder Template

`remainder()` captures any unknown/extra fields:

```java
// Capture everything not explicitly defined
DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType()),
    DSL.remainder()  // Preserves any other fields
)
```

### Why Use Remainder?

1. **Forward compatibility**: Data from newer versions may have extra fields
2. **Preserve unknown fields**: Don't lose data during migration
3. **Partial type definitions**: Only define fields you care about

```java
// Input JSON:
// {"name": "Steve", "level": 10, "customField": "preserved"}

// With remainder():
// → customField is preserved during migration

// Without remainder():
// → customField might be lost
```

## Tagged Choice (Sum Type)

`taggedChoice(discriminator, type, choices)` defines a discriminated union:

```java
// Entity can be player, monster, or item
DSL.taggedChoice(
    "type",              // Discriminator field name
    DSL.string(),        // Discriminator type
    Map.of(
        "player", DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType())
        ),
        "monster", DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("health", DSL.intType()),
            DSL.field("damage", DSL.intType())
        ),
        "item", DSL.and(
            DSL.field("itemId", DSL.string()),
            DSL.field("count", DSL.intType())
        )
    )
)
```

### Example Data

```json
// Player entity
{"type": "player", "name": "Steve", "level": 10}

// Monster entity
{"type": "monster", "name": "Zombie", "health": 20, "damage": 5}

// Item entity
{"type": "item", "itemId": "diamond", "count": 64}
```

## Type References in DSL

`ref(typeReference)` references another registered type:

```java
// Reference another type
DSL.field("player", DSL.ref(TypeReferences.PLAYER))

// List of referenced types
DSL.list(DSL.ref(TypeReferences.ENTITY))
```

### Example: World with Entities

```java
// Entity type
registerType(TypeReferences.ENTITY, DSL.taggedChoice(
    "type", DSL.string(),
    Map.of(
        "player", playerTemplate,
        "monster", monsterTemplate
    )
));

// World type references Entity
registerType(TypeReferences.WORLD, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("seed", DSL.longType()),
    DSL.field("entities", DSL.list(DSL.ref(TypeReferences.ENTITY))),
    DSL.remainder()
));
```

## Reusable Templates

Extract common patterns into methods:

```java
public class CommonTemplates {

    /** 3D position template */
    public static DSL.TypeTemplate position() {
        return DSL.and(
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType())
        );
    }

    /** 3D position with rotation */
    public static DSL.TypeTemplate positionWithRotation() {
        return DSL.and(
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType()),
            DSL.field("yaw", DSL.floatType()),
            DSL.field("pitch", DSL.floatType())
        );
    }

    /** Item stack template */
    public static DSL.TypeTemplate itemStack() {
        return DSL.and(
            DSL.field("id", DSL.string()),
            DSL.optionalFieldOf("count", DSL.intType(), 1),
            DSL.optional("nbt", DSL.remainder())
        );
    }

    /** UUID template (as string) */
    public static DSL.TypeTemplate uuid() {
        return DSL.string();  // UUIDs stored as strings
    }

    /** Timestamp template */
    public static DSL.TypeTemplate timestamp() {
        return DSL.longType();  // Epoch milliseconds
    }
}

// Usage in schemas
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("id", CommonTemplates.uuid()),
    DSL.field("name", DSL.string()),
    DSL.field("position", CommonTemplates.positionWithRotation()),
    DSL.field("inventory", DSL.list(CommonTemplates.itemStack())),
    DSL.field("lastLogin", CommonTemplates.timestamp()),
    DSL.remainder()
));
```

## Complete Schema Example

```java
public class Schema100 extends Schema {

    public Schema100() {
        super(new DataVersion(100), null, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        // Player type
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("uuid", DSL.string()),
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.field("experience", DSL.intType()),
            DSL.field("position", position()),
            DSL.field("inventory", inventory()),
            DSL.optional("achievements", DSL.list(DSL.string())),
            DSL.remainder()
        ));

        // World type
        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("seed", DSL.longType()),
            DSL.field("gameMode", DSL.intType()),
            DSL.field("difficulty", DSL.intType()),
            DSL.field("spawnPoint", position()),
            DSL.remainder()
        ));

        // Config type
        registerType(TypeReferences.CONFIG, DSL.and(
            DSL.field("version", DSL.intType()),
            DSL.field("settings", DSL.and(
                DSL.field("maxPlayers", DSL.intType()),
                DSL.field("pvpEnabled", DSL.bool()),
                DSL.optional("motd", DSL.string())
            )),
            DSL.remainder()
        ));
    }

    private static DSL.TypeTemplate position() {
        return DSL.and(
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType())
        );
    }

    private static DSL.TypeTemplate inventory() {
        return DSL.list(DSL.and(
            DSL.field("slot", DSL.intType()),
            DSL.field("item", DSL.and(
                DSL.field("id", DSL.string()),
                DSL.field("count", DSL.intType()),
                DSL.optional("damage", DSL.intType()),
                DSL.optional("nbt", DSL.remainder())
            ))
        ));
    }
}
```

## DSL Reference

| Method | Description | Example |
|--------|-------------|---------|
| `string()` | String type | `DSL.string()` |
| `intType()` | Integer type | `DSL.intType()` |
| `longType()` | Long type | `DSL.longType()` |
| `floatType()` | Float type | `DSL.floatType()` |
| `doubleType()` | Double type | `DSL.doubleType()` |
| `bool()` | Boolean type | `DSL.bool()` |
| `field(name, template)` | Required field | `DSL.field("name", DSL.string())` |
| `optional(name, template)` | Optional field | `DSL.optional("nick", DSL.string())` |
| `and(templates...)` | Product type | `DSL.and(field1, field2)` |
| `list(template)` | List type | `DSL.list(DSL.string())` |
| `remainder()` | Unknown fields | `DSL.remainder()` |
| `taggedChoice(...)` | Sum type | `DSL.taggedChoice("type", ...)` |
| `ref(typeRef)` | Type reference | `DSL.ref(TypeReferences.PLAYER)` |

## Best Practices

### 1. Always Include Remainder

```java
// Good: Preserves unknown fields
DSL.and(
    DSL.field("name", DSL.string()),
    DSL.remainder()
)

// Risk: May lose fields from newer versions
DSL.and(
    DSL.field("name", DSL.string())
)
```

### 2. Extract Reusable Templates

```java
// Good: Reusable, maintainable
private static DSL.TypeTemplate position() { ... }
private static DSL.TypeTemplate itemStack() { ... }

// Avoid: Duplicated inline definitions
```

### 3. Document Complex Structures

```java
/**
 * Player type structure:
 * - uuid: string (player's unique ID)
 * - name: string (display name)
 * - position: {x, y, z} (world coordinates)
 * - inventory: [{slot, item}] (inventory contents)
 */
registerType(TypeReferences.PLAYER, ...);
```

### 4. Use Tagged Choice for Polymorphism

```java
// Good: Explicit type discrimination
DSL.taggedChoice("type", DSL.string(), variants)

// Avoid: Implicit type guessing
```

---

## Summary

| Template | Purpose |
|----------|---------|
| Primitives | Basic data types |
| `field` | Required named field |
| `optional` | Optional named field |
| `and` | Combine multiple fields |
| `list` | Array of elements |
| `remainder` | Unknown fields |
| `taggedChoice` | Discriminated union |
| `ref` | Reference other types |

---

## Related

- [Schema System](schema-system.md) — Where DSL is used
- [Type System](type-system.md) — Templates become Types
- [Codec System](codec-system.md) — Encoding/decoding

