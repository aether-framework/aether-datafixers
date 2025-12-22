# Schema Inheritance Tutorial

This tutorial shows how to use schema inheritance to efficiently manage type definitions across many versions without duplicating code.

## Goal

Learn how to:
- Chain schemas with parent-child relationships
- Only define types that changed in each version
- Inherit unchanged types automatically

## The Problem

Without inheritance, each schema must define all types:

```java
// Schema1: Define PLAYER, WORLD, CONFIG
// Schema2: Define PLAYER (changed), WORLD (same), CONFIG (same)  ← Duplication!
// Schema3: Define PLAYER (same), WORLD (changed), CONFIG (same)  ← More duplication!
```

## The Solution: Schema Inheritance

With inheritance, schemas only define what changed:

```java
// Schema1: Define PLAYER, WORLD, CONFIG (base)
// Schema2: Define PLAYER only (WORLD, CONFIG inherited)
// Schema3: Define WORLD only (PLAYER, CONFIG inherited)
```

## Step-by-Step Example

### Setup: Multiple Data Types

Let's say we have three data types that evolve differently:

| Version | PLAYER | WORLD | CONFIG |
|---------|--------|-------|--------|
| v1 | Initial | Initial | Initial |
| v2 | **Changed** | Same | Same |
| v3 | Same | **Changed** | Same |
| v4 | Same | Same | **Changed** |

### Schema1: The Base Schema

The root schema defines all types:

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

public class Schema1 extends Schema {

    public Schema1() {
        // No parent - this is the root
        super(new DataVersion(1), null, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        // PLAYER v1
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("health", DSL.intType()),
            DSL.remainder()
        ));

        // WORLD v1
        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("seed", DSL.longType()),
            DSL.remainder()
        ));

        // CONFIG v1
        registerType(TypeReferences.CONFIG, DSL.and(
            DSL.field("difficulty", DSL.intType()),
            DSL.field("maxPlayers", DSL.intType()),
            DSL.remainder()
        ));
    }
}
```

### Schema2: Only PLAYER Changed

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

public class Schema2 extends Schema {

    public Schema2(Schema parent) {
        // Parent is Schema1
        super(new DataVersion(2), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        // Only PLAYER changed - added maxHealth field
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("health", DSL.intType()),
            DSL.field("maxHealth", DSL.intType()),  // NEW
            DSL.remainder()
        ));

        // WORLD - inherited unchanged from Schema1
        // CONFIG - inherited unchanged from Schema1
    }
}
```

### Schema3: Only WORLD Changed

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

public class Schema3 extends Schema {

    public Schema3(Schema parent) {
        // Parent is Schema2
        super(new DataVersion(3), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        // Only WORLD changed - added time field
        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("seed", DSL.longType()),
            DSL.field("time", DSL.longType()),  // NEW
            DSL.remainder()
        ));

        // PLAYER - inherited from Schema2 (with maxHealth)
        // CONFIG - inherited from Schema1
    }
}
```

### Schema4: Only CONFIG Changed

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

public class Schema4 extends Schema {

    public Schema4(Schema parent) {
        // Parent is Schema3
        super(new DataVersion(4), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        // Only CONFIG changed - difficulty now a string
        registerType(TypeReferences.CONFIG, DSL.and(
            DSL.field("difficulty", DSL.string()),  // CHANGED: int → string
            DSL.field("maxPlayers", DSL.intType()),
            DSL.remainder()
        ));

        // PLAYER - inherited from Schema2
        // WORLD - inherited from Schema3
    }
}
```

## How Inheritance Works

When the framework looks up a type:

```
Schema4.getType(PLAYER)
    └─▶ Not defined in Schema4
        └─▶ Check parent (Schema3)
            └─▶ Not defined in Schema3
                └─▶ Check parent (Schema2)
                    └─▶ Found! Return PLAYER from Schema2
```

```
Schema4.getType(CONFIG)
    └─▶ Found in Schema4! Return CONFIG from Schema4
```

## The Bootstrap

Wire the schema chain:

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import com.example.game.schema.*;

public class GameDataBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(4);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;

        // Build the chain - each schema references its parent
        Schema1 v1 = new Schema1();
        Schema2 v2 = new Schema2(v1);
        Schema3 v3 = new Schema3(v2);
        Schema4 v4 = new Schema4(v3);

        // Register all
        schemas.register(v1);
        schemas.register(v2);
        schemas.register(v3);
        schemas.register(v4);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Player fixes
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));

        // World fixes
        fixes.register(TypeReferences.WORLD, new WorldV2ToV3Fix(schemas));

        // Config fixes
        fixes.register(TypeReferences.CONFIG, new ConfigV3ToV4Fix(schemas));
    }
}
```

## The Fixes

### PlayerV1ToV2Fix

```java
public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super("player_v1_to_v2", new DataVersion(1), new DataVersion(2), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.PLAYER, player -> {
            // Add maxHealth = health
            int health = player.get("health").asInt().orElse(100);
            return player.set("maxHealth", player.createInt(health));
        });
    }
}
```

### WorldV2ToV3Fix

```java
public class WorldV2ToV3Fix extends SchemaDataFix {

    public WorldV2ToV3Fix(SchemaRegistry schemas) {
        super("world_v2_to_v3", new DataVersion(2), new DataVersion(3), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.WORLD, world -> {
            // Add time = 0 (morning)
            return world.set("time", world.createLong(0L));
        });
    }
}
```

### ConfigV3ToV4Fix

```java
public class ConfigV3ToV4Fix extends SchemaDataFix {

    public ConfigV3ToV4Fix(SchemaRegistry schemas) {
        super("config_v3_to_v4", new DataVersion(3), new DataVersion(4), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transformField(TypeReferences.CONFIG, "difficulty", value -> {
            // Convert int to string
            int level = value.asInt().orElse(1);
            String name = switch (level) {
                case 0 -> "peaceful";
                case 1 -> "easy";
                case 2 -> "normal";
                case 3 -> "hard";
                default -> "normal";
            };
            return value.createString(name);
        });
    }
}
```

## Benefits of Inheritance

### 1. Less Code

Without inheritance:
```java
// Schema4 would need to define all 3 types
// Even though only CONFIG changed
```

With inheritance:
```java
// Schema4 only defines CONFIG
// PLAYER and WORLD are inherited
```

### 2. Single Source of Truth

A type definition only exists in one place:

```
PLAYER: Defined in Schema2 (latest change)
WORLD:  Defined in Schema3 (latest change)
CONFIG: Defined in Schema4 (latest change)
```

### 3. Easy to Track Changes

Looking at a schema tells you exactly what changed:

```java
public class Schema3 extends Schema {
    @Override
    protected void registerTypes() {
        // Only WORLD definition here = WORLD changed in v3
        registerType(TypeReferences.WORLD, ...);
    }
}
```

### 4. Safe Refactoring

Change a type's definition in one place:

```java
// Update PLAYER in Schema2
// All later schemas automatically get the update
```

## Visualization

```
Schema1 (v1)
├── PLAYER (v1)  ───────────────────────────────────────┐
├── WORLD (v1)   ──────────────────────┐                │
└── CONFIG (v1)  ───────┐              │                │
                        │              │                │
Schema2 (v2)            │              │                │
├── PLAYER (v2)  ───────┼──────────────┼────────────────┤ (overrides)
├── WORLD        ───────┼──────────────│ (inherits v1)  │
└── CONFIG       ───────│ (inherits v1)│                │
                        │              │                │
Schema3 (v3)            │              │                │
├── PLAYER       ───────┼──────────────┼─ (inherits v2) │
├── WORLD (v3)   ───────┼──────────────┤ (overrides)    │
└── CONFIG       ───────│ (inherits v1)│                │
                        │              │                │
Schema4 (v4)            │              │                │
├── PLAYER       ───────┼──────────────┼─ (inherits v2) │
├── WORLD        ───────┼─ (inherits v3)                │
└── CONFIG (v4)  ───────┤ (overrides)                   │
```

## Best Practices

### 1. Document What Changed

```java
/**
 * Schema for v3.
 *
 * Changes from v2:
 * - WORLD: Added "time" field for day/night cycle
 */
public class Schema3 extends Schema {
```

### 2. Use Descriptive Comments

```java
@Override
protected void registerTypes() {
    // WORLD v3: Added time field
    registerType(TypeReferences.WORLD, DSL.and(
        DSL.field("name", DSL.string()),
        DSL.field("seed", DSL.longType()),
        DSL.field("time", DSL.longType()),  // NEW in v3
        DSL.remainder()
    ));

    // PLAYER: Inherited from Schema2 (has maxHealth)
    // CONFIG: Inherited from Schema1
}
```

### 3. Keep Schema Classes Small

Each schema should only contain changed types. If a schema is getting large, consider splitting types into separate helper methods.

## Next Steps

- **[Polymorphic Data](polymorphic-data.md)** — Handle sum types
- **[Nested Transformations](nested-transformations.md)** — Complex restructuring
- **[DSL Reference](../concepts/dsl.md)** — All type templates

