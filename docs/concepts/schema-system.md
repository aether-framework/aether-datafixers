# Schema System

The schema system is the foundation of Aether Datafixers. It defines what data types exist at each version and how they're structured.

## Overview

A **Schema** represents the state of your data model at a specific version. It associates a `DataVersion` with a `TypeRegistry` containing type definitions.

```
┌──────────────────────────────────────────────────────────────┐
│                         Schema                               │
│  ┌─────────────────┐    ┌─────────────────────────────────┐  │
│  │  DataVersion    │    │         TypeRegistry            │  │
│  │     (100)       │    │  ┌───────────────────────────┐  │  │
│  └─────────────────┘    │  │ TypeReference → Type      │  │  │
│                         │  │ "player"    → PlayerType  │  │  │
│  ┌─────────────────┐    │  │ "world"     → WorldType   │  │  │
│  │  Parent Schema  │    │  └───────────────────────────┘  │  │
│  │   (optional)    │    └─────────────────────────────────┘  │
│  └─────────────────┘                                         │
└──────────────────────────────────────────────────────────────┘
```

## Core Components

### Schema

The abstract base class for all schema definitions:

```java
public abstract class Schema {
    private final DataVersion version;
    private final Schema parent;
    private final TypeRegistry typeRegistry;

    protected Schema(
        DataVersion version,
        Schema parent,
        Supplier<TypeRegistry> registryFactory
    ) {
        // ...
    }

    /** Override to register types for this version */
    protected abstract void registerTypes();

    /** Register a type using DSL template */
    protected final void registerType(TypeReference ref, DSL.TypeTemplate template) {
        // ...
    }
}
```

### SchemaRegistry

Manages all schemas in version order:

```java
public interface SchemaRegistry {
    /** Register a schema */
    void register(Schema schema);

    /** Get schema for a specific version */
    Optional<Schema> get(DataVersion version);

    /** Get all registered schemas */
    Collection<Schema> all();
}
```

### TypeRegistry

Maps type references to type definitions within a schema:

```java
public interface TypeRegistry {
    /** Register a type */
    void register(TypeReference ref, Type type);

    /** Look up a type */
    Optional<Type> get(TypeReference ref);
}
```

## Creating Schemas

### Basic Schema (No Parent)

The first/root schema has no parent:

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

/**
 * Schema for Version 1.0.0 (ID: 100).
 *
 * <p>Initial data format with basic player fields.</p>
 */
public class Schema100 extends Schema {

    public Schema100() {
        super(
            new DataVersion(100),   // This schema's version
            null,                    // No parent (root schema)
            SimpleTypeRegistry::new  // Registry factory
        );
    }

    @Override
    protected void registerTypes() {
        // Player: name, level, x, y, z
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType()),
            DSL.remainder()
        ));

        // World: name, seed
        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("seed", DSL.longType()),
            DSL.remainder()
        ));
    }
}
```

### Child Schema (With Parent)

Subsequent schemas inherit from a parent:

```java
/**
 * Schema for Version 1.1.0 (ID: 110).
 *
 * <p>Changes from 100:</p>
 * <ul>
 *   <li>Player: x/y/z → nested position object</li>
 *   <li>Player: added experience field</li>
 * </ul>
 */
public class Schema110 extends Schema {

    public Schema110(Schema parent) {
        super(
            new DataVersion(110),   // This schema's version
            parent,                  // Parent schema (Schema100)
            SimpleTypeRegistry::new
        );
    }

    @Override
    protected void registerTypes() {
        // Only register types that CHANGED from parent
        // World type is inherited unchanged

        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.field("experience", DSL.intType()),  // NEW
            DSL.field("position", position()),       // CHANGED: nested object
            DSL.remainder()
        ));
    }

    /** Reusable position template */
    public static DSL.TypeTemplate position() {
        return DSL.and(
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType())
        );
    }
}
```

## Schema Inheritance

Child schemas inherit type definitions from their parent. Only register types that changed:

```
Schema100 (v1.0.0)              Schema110 (v1.1.0)
├── PLAYER: {name, level,       ├── PLAYER: {name, level,
│            x, y, z}           │            experience, position}
└── WORLD: {name, seed}         └── WORLD: (inherited from parent)
```

### How Inheritance Works

```java
public class Schema200 extends Schema {

    public Schema200(Schema parent) {
        super(new DataVersion(200), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        // Only register PLAYER - WORLD is inherited from Schema110

        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.field("experience", DSL.intType()),
            DSL.field("position", Schema110.position()),
            DSL.field("inventory", inventory()),  // NEW in v2.0.0
            DSL.remainder()
        ));
    }

    private DSL.TypeTemplate inventory() {
        return DSL.list(DSL.and(
            DSL.field("item", DSL.string()),
            DSL.field("count", DSL.intType())
        ));
    }
}
```

## Registering with SchemaRegistry

In your bootstrap, register schemas in version order:

```java
public class GameDataBootstrap implements DataFixerBootstrap {

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;

        // Create schema chain
        Schema100 v100 = new Schema100();
        Schema110 v110 = new Schema110(v100);
        Schema200 v200 = new Schema200(v110);

        // Register all schemas
        schemas.register(v100);
        schemas.register(v110);
        schemas.register(v200);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Fixes can now access schemas
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
    }
}
```

## DSL Type Templates

The DSL provides factory methods for building type templates:

### Primitive Types

```java
DSL.string()      // String field
DSL.intType()     // Integer field
DSL.longType()    // Long field
DSL.floatType()   // Float field
DSL.doubleType()  // Double field
DSL.bool()        // Boolean field
```

### Composite Types

```java
// Required field
DSL.field("name", DSL.string())

// Optional field
DSL.optional("nickname", DSL.string())

// Combine multiple fields
DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("age", DSL.intType())
)

// List of values
DSL.list(DSL.string())
DSL.list(DSL.and(
    DSL.field("id", DSL.intType()),
    DSL.field("value", DSL.string())
))

// Remainder (captures unknown fields)
DSL.remainder()
```

### Polymorphic Types

```java
// Tagged choice (discriminated union)
DSL.taggedChoice(
    "type",           // Discriminator field
    DSL.string(),     // Discriminator type
    Map.of(
        "player", playerTemplate,
        "monster", monsterTemplate,
        "item", itemTemplate
    )
)
```

## Complete Example

```java
// TypeReferences.java
public final class TypeReferences {
    public static final TypeReference PLAYER = new TypeReference("player");
    public static final TypeReference WORLD = new TypeReference("world");
    public static final TypeReference ENTITY = new TypeReference("entity");
    private TypeReferences() {}
}

// Schema100.java
public class Schema100 extends Schema {
    public Schema100() {
        super(new DataVersion(100), null, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("playerName", DSL.string()),
            DSL.field("xp", DSL.intType()),
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType()),
            DSL.field("gameMode", DSL.intType()),
            DSL.remainder()
        ));

        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("worldName", DSL.string()),
            DSL.field("seed", DSL.longType()),
            DSL.field("difficulty", DSL.intType()),
            DSL.remainder()
        ));
    }
}

// Schema110.java
public class Schema110 extends Schema {
    public Schema110(Schema parent) {
        super(new DataVersion(110), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),           // renamed
            DSL.field("experience", DSL.intType()),    // renamed
            DSL.field("position", position()),         // restructured
            DSL.field("gameMode", DSL.string()),       // type changed
            DSL.remainder()
        ));
        // WORLD inherits unchanged
    }

    public static DSL.TypeTemplate position() {
        return DSL.and(
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType())
        );
    }
}
```

## Best Practices

### 1. One Schema Per Version

Create a distinct schema class for each data version:

```java
Schema100.java  // v1.0.0
Schema110.java  // v1.1.0
Schema200.java  // v2.0.0
```

### 2. Document Changes

Document what changed from the parent schema:

```java
/**
 * Schema for Version 1.1.0 (ID: 110).
 *
 * <p>Changes from {@link Schema100}:</p>
 * <ul>
 *   <li>PLAYER: renamed playerName → name</li>
 *   <li>PLAYER: renamed xp → experience</li>
 *   <li>PLAYER: restructured x/y/z → position object</li>
 *   <li>PLAYER: gameMode int → string</li>
 * </ul>
 */
public class Schema110 extends Schema { ... }
```

### 3. Extract Reusable Templates

Create static methods for reusable type templates:

```java
public class CommonTemplates {
    public static DSL.TypeTemplate position() {
        return DSL.and(
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType())
        );
    }

    public static DSL.TypeTemplate itemStack() {
        return DSL.and(
            DSL.field("id", DSL.string()),
            DSL.field("count", DSL.intType()),
            DSL.optional("nbt", DSL.remainder())
        );
    }
}
```

### 4. Always Include Remainder

Include `DSL.remainder()` to preserve unknown fields:

```java
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType()),
    DSL.remainder()  // Preserves any additional fields
));
```

### 5. Keep Schemas Immutable

Schema definitions should never change after release. If you need to modify a type, create a new schema version.

---

## Summary

| Component        | Purpose                                   |
|------------------|-------------------------------------------|
| `Schema`         | Defines types at a specific version       |
| `SchemaRegistry` | Manages all schemas                       |
| `TypeRegistry`   | Maps TypeReference → Type within a schema |
| `DSL`            | Factory for type templates                |
| Parent Schema    | Enables inheritance of unchanged types    |

---

## Related

- [Type System](type-system.md) — Type and TypeRegistry details
- [DSL](dsl.md) — Type template language reference
- [DataFix System](datafix-system.md) — Using schemas in fixes

