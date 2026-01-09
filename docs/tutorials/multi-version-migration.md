# Multi-Version Migration Tutorial

This tutorial shows how to chain multiple migrations together, allowing data to be automatically upgraded through several versions.

## Goal

Create a migration chain that handles three versions:

- **v1**: `{"playerName": "Steve", "xp": 1500}`
- **v2**: `{"name": "Steve", "experience": 1500}`
- **v3**: `{"name": "Steve", "experience": 1500, "level": 15}`

Data at v1 should automatically migrate through v2 to reach v3.

## Prerequisites

- Completed [Basic Migration](basic-migration.md) tutorial
- Understanding of schema inheritance

## The Migration Chain

```
v1 Data ──▶ Fix v1→v2 ──▶ v2 Data ──▶ Fix v2→v3 ──▶ v3 Data
    │                         │                         │
    └── playerName, xp        └── name, experience      └── + level
```

## Step 1: Define All Schemas

### Schema1 (v1)

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

public class Schema1 extends Schema {

    public Schema1() {
        super(new DataVersion(1), null, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("playerName", DSL.string()),
            DSL.field("xp", DSL.intType()),
            DSL.remainder()
        ));
    }
}
```

### Schema2 (v2)

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

public class Schema2 extends Schema {

    public Schema2(Schema parent) {
        super(new DataVersion(2), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("experience", DSL.intType()),
            DSL.remainder()
        ));
    }
}
```

### Schema3 (v3)

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

public class Schema3 extends Schema {

    public Schema3(Schema parent) {
        super(new DataVersion(3), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("experience", DSL.intType()),
            DSL.field("level", DSL.intType()),  // New field
            DSL.remainder()
        ));
    }
}
```

## Step 2: Create the Fixes

### Fix v1 → v2

```java
package com.example.game.fix;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import com.example.game.TypeReferences;

public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super("player_v1_to_v2", new DataVersion(1), new DataVersion(2), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
            Rules.renameField(TypeReferences.PLAYER, "xp", "experience")
        );
    }
}
```

### Fix v2 → v3

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

public class PlayerV2ToV3Fix extends SchemaDataFix {

    public PlayerV2ToV3Fix(SchemaRegistry schemas) {
        super("player_v2_to_v3", new DataVersion(2), new DataVersion(3), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        // Add level field computed from experience
        return Rules.transform(TypeReferences.PLAYER, this::addLevel);
    }

    private Dynamic<?> addLevel(Dynamic<?> player) {
        int experience = player.get("experience").asInt().orElse(0);
        int level = calculateLevel(experience);

        return player.set("level", player.createInt(level));
    }

    private int calculateLevel(int experience) {
        // Simple level calculation: level = experience / 100
        return Math.max(1, experience / 100);
    }
}
```

## Step 3: Update the Bootstrap

Register all schemas and fixes:

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import com.example.game.fix.PlayerV1ToV2Fix;
import com.example.game.fix.PlayerV2ToV3Fix;
import com.example.game.schema.Schema1;
import com.example.game.schema.Schema2;
import com.example.game.schema.Schema3;

public class GameDataBootstrap implements DataFixerBootstrap {

    // Update to latest version
    public static final DataVersion CURRENT_VERSION = new DataVersion(3);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;

        // Create schema chain (each references its parent)
        Schema1 v1 = new Schema1();
        Schema2 v2 = new Schema2(v1);
        Schema3 v3 = new Schema3(v2);

        // Register all schemas
        schemas.register(v1);
        schemas.register(v2);
        schemas.register(v3);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Register all fixes
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
        fixes.register(TypeReferences.PLAYER, new PlayerV2ToV3Fix(schemas));
    }
}
```

## Step 4: Test the Chain

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

public class MultiVersionExample {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        AetherDataFixer fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

        // Test 1: v1 → v3 (full chain)
        System.out.println("=== Test 1: v1 → v3 ===");
        testMigration(fixer, createV1Data(), new DataVersion(1));

        // Test 2: v2 → v3 (partial chain)
        System.out.println("\n=== Test 2: v2 → v3 ===");
        testMigration(fixer, createV2Data(), new DataVersion(2));

        // Test 3: v3 → v3 (no migration needed)
        System.out.println("\n=== Test 3: v3 → v3 (no-op) ===");
        testMigration(fixer, createV3Data(), new DataVersion(3));
    }

    private static void testMigration(AetherDataFixer fixer, JsonObject data, DataVersion from) {
        System.out.println("Input:");
        System.out.println(GSON.toJson(data));

        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, data);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

        TaggedDynamic migrated = fixer.update(
            tagged,
            from,
            GameDataBootstrap.CURRENT_VERSION
        );

        @SuppressWarnings("unchecked")
        Dynamic<JsonElement> result = (Dynamic<JsonElement>) migrated.value();
        System.out.println("Output:");
        System.out.println(GSON.toJson(result.value()));
    }

    private static JsonObject createV1Data() {
        JsonObject data = new JsonObject();
        data.addProperty("playerName", "Steve");
        data.addProperty("xp", 1500);
        return data;
    }

    private static JsonObject createV2Data() {
        JsonObject data = new JsonObject();
        data.addProperty("name", "Alex");
        data.addProperty("experience", 2500);
        return data;
    }

    private static JsonObject createV3Data() {
        JsonObject data = new JsonObject();
        data.addProperty("name", "Bob");
        data.addProperty("experience", 3000);
        data.addProperty("level", 30);
        return data;
    }
}
```

## Expected Output

```
=== Test 1: v1 → v3 ===
Input:
{
  "playerName": "Steve",
  "xp": 1500
}
Output:
{
  "name": "Steve",
  "experience": 1500,
  "level": 15
}

=== Test 2: v2 → v3 ===
Input:
{
  "name": "Alex",
  "experience": 2500
}
Output:
{
  "name": "Alex",
  "experience": 2500,
  "level": 25
}

=== Test 3: v3 → v3 (no-op) ===
Input:
{
  "name": "Bob",
  "experience": 3000,
  "level": 30
}
Output:
{
  "name": "Bob",
  "experience": 3000,
  "level": 30
}
```

## How the Chain Works

The DataFixer automatically:

1. **Finds applicable fixes** between `fromVersion` and `toVersion`
2. **Sorts them** by version number
3. **Applies them sequentially**

For v1 → v3:
```
v1 data
  │
  ▼ PlayerV1ToV2Fix
  │ (rename fields)
  │
v2 data
  │
  ▼ PlayerV2ToV3Fix
  │ (add level)
  │
v3 data
```

## Adding More Versions

When you need a v4, simply:

1. Create `Schema4` extending `Schema3`
2. Create `PlayerV3ToV4Fix`
3. Register both in the bootstrap
4. Update `CURRENT_VERSION`

```java
// Schema4.java
public class Schema4 extends Schema {
    public Schema4(Schema parent) {
        super(new DataVersion(4), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("experience", DSL.intType()),
            DSL.field("level", DSL.intType()),
            DSL.field("rank", DSL.string()),  // New in v4
            DSL.remainder()
        ));
    }
}

// PlayerV3ToV4Fix.java
public class PlayerV3ToV4Fix extends SchemaDataFix {
    // ... add rank based on level
}

// Bootstrap
public static final DataVersion CURRENT_VERSION = new DataVersion(4);
// ... register Schema4 and PlayerV3ToV4Fix
```

## Key Points

1. **Order matters**: Fixes are applied in version order
2. **Chain is automatic**: Just specify start and end versions
3. **Partial chains work**: v2→v3 skips v1→v2 fix
4. **No-op when current**: Same version returns unchanged data
5. **Schema inheritance**: Each schema extends its predecessor

## Common Patterns

### Skip Intermediate Versions

You don't need a direct v1→v3 fix. The framework chains v1→v2→v3 automatically.

### Multiple Types

Each type can have its own fix chain:

```java
@Override
public void registerFixes(FixRegistrar fixes) {
    // Player fixes
    fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
    fixes.register(TypeReferences.PLAYER, new PlayerV2ToV3Fix(schemas));

    // World fixes (different chain)
    fixes.register(TypeReferences.WORLD, new WorldV1ToV2Fix(schemas));
}
```

### Conditional Fixes

Check data before transforming:

```java
private Dynamic<?> maybeAddLevel(Dynamic<?> player) {
    // Only add if level doesn't exist
    if (player.get("level").result().isEmpty()) {
        int exp = player.get("experience").asInt().orElse(0);
        return player.set("level", player.createInt(exp / 100));
    }
    return player;
}
```

## Next Steps

- **[Schema Inheritance](schema-inheritance.md)** — Efficient schema organization
- **[Nested Transformations](nested-transformations.md)** — Complex restructuring
- **[Testing Migrations](../how-to/test-migrations.md)** — Write robust tests

