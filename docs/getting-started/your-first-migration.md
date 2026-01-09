# Your First Migration

This tutorial walks you through creating a complete data migration system with schemas, fixes, and best practices.

## The Scenario

You're building a game that saves player data. Over time, the data format evolves:

**Version 1.0.0 (ID: 100)** — Initial release
```json
{
  "playerName": "Steve",
  "xp": 1500,
  "x": 100.5,
  "y": 64.0,
  "z": -200.25,
  "gameMode": 0
}
```

**Version 1.1.0 (ID: 110)** — Restructured
```json
{
  "name": "Steve",
  "experience": 1500,
  "position": {
    "x": 100.5,
    "y": 64.0,
    "z": -200.25
  },
  "gameMode": "survival"
}
```

We'll build the migration from v1.0.0 to v1.1.0.

---

## Step 1: Define Type References

Create a central class for all type identifiers:

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.TypeReference;

/**
 * Type references for all data types in the game.
 */
public final class TypeReferences {

    /** Player save data */
    public static final TypeReference PLAYER = new TypeReference("player");

    /** World/level data */
    public static final TypeReference WORLD = new TypeReference("world");

    private TypeReferences() {} // Prevent instantiation
}
```

---

## Step 2: Create Schema for Version 1.0.0

Define the data structure at version 100:

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

/**
 * Schema for Version 1.0.0 (ID: 100)
 *
 * Player structure:
 * - playerName: string
 * - xp: int
 * - x, y, z: double (flat coordinates)
 * - gameMode: int (0=survival, 1=creative, etc.)
 */
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
    }
}
```

---

## Step 3: Create Schema for Version 1.1.0

Define the updated structure:

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

/**
 * Schema for Version 1.1.0 (ID: 110)
 *
 * Changes from 100:
 * - playerName → name
 * - xp → experience
 * - x, y, z → nested position object
 * - gameMode: int → string
 */
public class Schema110 extends Schema {

    public Schema110(Schema parent) {
        super(new DataVersion(110), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("experience", DSL.intType()),
            DSL.field("position", position()),
            DSL.field("gameMode", DSL.string()),
            DSL.remainder()
        ));
    }

    /** Position type template */
    public static DSL.TypeTemplate position() {
        return DSL.and(
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType())
        );
    }
}
```

---

## Step 4: Create the Data Fix

Implement the migration logic:

```java
package com.example.game.fix;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import com.example.game.TypeReferences;

/**
 * Migrates player data from v1.0.0 (100) to v1.1.0 (110).
 */
public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super(
            "player_v1_to_v2",
            new DataVersion(100),
            new DataVersion(110),
            schemas
        );
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            // 1. Rename fields
            Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
            Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),

            // 2. Transform gameMode from int to string
            Rules.transformField(TypeReferences.PLAYER, "gameMode", this::gameModeToString),

            // 3. Group coordinates into position object
            Rules.transform(TypeReferences.PLAYER, this::groupPosition)
        );
    }

    private Dynamic<?> gameModeToString(Dynamic<?> value) {
        int mode = value.asInt().orElse(0);
        String modeName = switch (mode) {
            case 0 -> "survival";
            case 1 -> "creative";
            case 2 -> "adventure";
            case 3 -> "spectator";
            default -> "survival";
        };
        return value.createString(modeName);
    }

    private Dynamic<?> groupPosition(Dynamic<?> player) {
        // Extract coordinates
        double x = player.get("x").asDouble().orElse(0.0);
        double y = player.get("y").asDouble().orElse(0.0);
        double z = player.get("z").asDouble().orElse(0.0);

        // Create position object
        Dynamic<?> position = player.emptyMap()
            .set("x", player.createDouble(x))
            .set("y", player.createDouble(y))
            .set("z", player.createDouble(z));

        // Remove old fields and add position
        return player
            .remove("x")
            .remove("y")
            .remove("z")
            .set("position", position);
    }
}
```

---

## Step 5: Create the Bootstrap

Wire schemas and fixes together:

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import com.example.game.fix.PlayerV1ToV2Fix;
import com.example.game.schema.Schema100;
import com.example.game.schema.Schema110;

/**
 * Bootstrap for the game data fixer.
 */
public class GameDataBootstrap implements DataFixerBootstrap {

    /** Current (latest) version */
    public static final DataVersion CURRENT_VERSION = new DataVersion(110);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;

        // Register schemas in version order
        Schema100 v100 = new Schema100();
        Schema110 v110 = new Schema110(v100);

        schemas.register(v100);
        schemas.register(v110);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Register fixes
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
    }
}
```

---

## Step 6: Use the Fixer

```java
package com.example.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;

public class GameExample {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        // 1. Create the data fixer
        AetherDataFixer fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

        // 2. Simulate old v1.0.0 save data
        JsonObject oldSave = new JsonObject();
        oldSave.addProperty("playerName", "Steve");
        oldSave.addProperty("xp", 1500);
        oldSave.addProperty("x", 100.5);
        oldSave.addProperty("y", 64.0);
        oldSave.addProperty("z", -200.25);
        oldSave.addProperty("gameMode", 0);

        System.out.println("=== Old Data (v1.0.0) ===");
        System.out.println(GSON.toJson(oldSave));

        // 3. Wrap in Dynamic
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, oldSave);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

        // 4. Migrate from v1.0.0 to v1.1.0
        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(100),
            fixer.currentVersion()
        );

        // 5. Print result
        System.out.println("\n=== Migrated Data (v1.1.0) ===");
        @SuppressWarnings("unchecked")
        Dynamic<JsonElement> result = (Dynamic<JsonElement>) migrated.value();
        System.out.println(GSON.toJson(result.value()));
    }
}
```

---

## Output

```
=== Old Data (v1.0.0) ===
{
  "playerName": "Steve",
  "xp": 1500,
  "x": 100.5,
  "y": 64.0,
  "z": -200.25,
  "gameMode": 0
}

=== Migrated Data (v1.1.0) ===
{
  "name": "Steve",
  "experience": 1500,
  "position": {
    "x": 100.5,
    "y": 64.0,
    "z": -200.25
  },
  "gameMode": "survival"
}
```

---

## Best Practices

### 1. Version Numbering

Use a consistent scheme. Recommended: SemVer encoded as integers.

| SemVer | ID |
|--------|-----|
| 1.0.0 | 100 |
| 1.1.0 | 110 |
| 2.0.0 | 200 |

### 2. One Fix Per Version Step

Create separate fix classes for each migration step:
- `PlayerV1ToV2Fix` (100 → 110)
- `PlayerV2ToV3Fix` (110 → 200)

### 3. Keep TypeReferences Centralized

Define all type references in one class for easy discovery.

### 4. Use Parent Schemas

Chain schemas: `Schema110(v100)` inherits from `Schema100`.

### 5. Test Your Fixes

Write unit tests for each fix with sample data.

---

## Next Steps

Congratulations! You've built your first complete migration system.

Continue learning:

- [Schema System](../concepts/schema-system.md) — Deep dive into schemas
- [DataFix System](../concepts/datafix-system.md) — Understanding fixes
- [Rewrite Rules](../concepts/rewrite-rules.md) — Rule combinators
- [Multi-Version Migration](../tutorials/multi-version-migration.md) — Chain multiple fixes
