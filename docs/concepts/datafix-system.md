# DataFix System

The DataFix system is the heart of Aether Datafixers. It defines how data is transformed from one version to another through a series of versioned fixes.

## Overview

A **DataFix** is a migration that transforms data from one `DataVersion` to another. The **DataFixer** orchestrates applying the correct sequence of fixes to migrate data to the target version.

```
┌──────────────────────────────────────────────────────────────────┐
│                         DataFixer                                │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                   Fix Chain for PLAYER                     │  │
│  │                                                            │  │
│  │  ┌──────────┐    ┌──────────┐    ┌──────────┐              │  │
│  │  │Fix100→110│───▶│Fix110→200│───▶│Fix200→210│              │  │
│  │  └──────────┘    └──────────┘    └──────────┘              │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│ Input: Data @ v100  ──────────────────────▶  Output: Data @ v210 │
└──────────────────────────────────────────────────────────────────┘
```

## Core Interfaces

### DataFix

The base interface for all data fixes:

```java
public interface DataFix<T> {
    /** Unique name for this fix */
    String name();

    /** Source version */
    DataVersion fromVersion();

    /** Target version */
    DataVersion toVersion();

    /** Apply the fix to transform data */
    Dynamic<T> apply(Dynamic<T> input);
}
```

### DataFixer

The orchestrator that applies fixes:

```java
public interface DataFixer {
    /** The current (latest) data version */
    DataVersion currentVersion();

    /** Update data from one version to another */
    TaggedDynamic update(
        TaggedDynamic data,
        DataVersion from,
        DataVersion to
    );
}
```

### FixRegistrar

Used to register fixes during bootstrap:

```java
public interface FixRegistrar {
    /** Register a fix for a specific type */
    void register(TypeReference type, DataFix<?> fix);

    /** Get all fixes for a type */
    List<DataFix<?>> getFixesFor(TypeReference type);
}
```

## Creating Fixes

### Simple DataFix

For straightforward transformations, implement `DataFix` directly:

```java
public class PlayerV1ToV2Fix implements DataFix<JsonElement> {

    @Override
    public String name() {
        return "player_v1_to_v2";
    }

    @Override
    public DataVersion fromVersion() {
        return new DataVersion(100);
    }

    @Override
    public DataVersion toVersion() {
        return new DataVersion(110);
    }

    @Override
    public Dynamic<JsonElement> apply(Dynamic<JsonElement> input) {
        // Read old fields
        String name = input.get("playerName").asString().orElse("Unknown");
        int xp = input.get("xp").asInt().orElse(0);

        // Create new structure
        return input
            .remove("playerName")
            .remove("xp")
            .set("name", input.createString(name))
            .set("experience", input.createInt(xp));
    }
}
```

### SchemaDataFix (Recommended)

For fixes that need schema access, extend `SchemaDataFix`:

```java
public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super(
            "player_v1_to_v2",          // Fix name
            new DataVersion(100),        // From version
            new DataVersion(110),        // To version
            schemas                      // Schema registry
        );
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            // 1. Rename fields
            Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
            Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),

            // 2. Transform gameMode from int to string
            Rules.transformField(
                TypeReferences.PLAYER,
                "gameMode",
                this::gameModeToString
            ),

            // 3. Restructure coordinates
            Rules.transform(TypeReferences.PLAYER, this::groupPosition)
        );
    }

    private Dynamic<?> gameModeToString(Dynamic<?> value) {
        int mode = value.asInt().orElse(0);
        String name = switch (mode) {
            case 0 -> "survival";
            case 1 -> "creative";
            case 2 -> "adventure";
            case 3 -> "spectator";
            default -> "survival";
        };
        return value.createString(name);
    }

    private Dynamic<?> groupPosition(Dynamic<?> player) {
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
}
```

## TypeRewriteRule

`TypeRewriteRule` defines how types are transformed. The `Rules` class provides factory methods:

### Basic Rules

```java
// Rename a field
Rules.renameField(TypeReferences.PLAYER, "oldName", "newName")

// Transform a field's value
Rules.transformField(TypeReferences.PLAYER, "field", value -> transform(value))

// Transform the entire object
Rules.transform(TypeReferences.PLAYER, dynamic -> transformDynamic(dynamic))

// Add a field with default value
Rules.addField(TypeReferences.PLAYER, "newField", defaultValue)

// Remove a field
Rules.removeField(TypeReferences.PLAYER, "obsoleteField")
```

### Combining Rules

```java
// Sequential application (order matters)
Rules.seq(rule1, rule2, rule3)

// All rules (combine multiple)
Rules.all(
    Rules.renameField(TYPE, "a", "b"),
    Rules.renameField(TYPE, "x", "y"),
    Rules.transformField(TYPE, "z", transformer)
)
```

## Registering Fixes

Fixes are registered in the bootstrap:

```java
public class GameDataBootstrap implements DataFixerBootstrap {

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;

        Schema100 v100 = new Schema100();
        Schema110 v110 = new Schema110(v100);
        Schema200 v200 = new Schema200(v110);

        schemas.register(v100);
        schemas.register(v110);
        schemas.register(v200);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Register fixes for each type
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
        fixes.register(TypeReferences.PLAYER, new PlayerV2ToV3Fix(schemas));

        fixes.register(TypeReferences.WORLD, new WorldV1ToV2Fix(schemas));

        fixes.register(TypeReferences.ENTITY, new EntityV1ToV2Fix(schemas));
    }
}
```

## Creating the DataFixer

Use `DataFixerRuntimeFactory` to create a configured fixer:

```java
public class MyApp {
    private static final AetherDataFixer FIXER;

    static {
        FIXER = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
    }

    public static AetherDataFixer fixer() {
        return FIXER;
    }
}
```

## Applying Fixes

### Basic Usage

```java
// 1. Create Dynamic from your data
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonData);

// 2. Tag with type
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

// 3. Apply fixes
TaggedDynamic migrated = fixer.update(
    tagged,
    new DataVersion(100),  // from version
    fixer.currentVersion() // to version (latest)
);

// 4. Extract result
Dynamic<JsonElement> result = (Dynamic<JsonElement>) migrated.value();
JsonElement json = result.value();
```

### Complete Example

```java
public JsonObject migratePlayerData(JsonObject oldData, int savedVersion) {
    // 1. Wrap in Dynamic
    Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, oldData);
    TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

    // 2. Migrate
    TaggedDynamic migrated = fixer.update(
        tagged,
        new DataVersion(savedVersion),
        GameDataBootstrap.CURRENT_VERSION
    );

    // 3. Extract
    @SuppressWarnings("unchecked")
    Dynamic<JsonElement> result = (Dynamic<JsonElement>) migrated.value();
    return result.value().getAsJsonObject();
}
```

## Fix Ordering

Fixes are applied in version order:

```
Data @ v100
    │
    ▼ Fix 100→110
    │
Data @ v110
    │
    ▼ Fix 110→200
    │
Data @ v200
    │
    ▼ Fix 200→210
    │
Data @ v210 (current)
```

The framework automatically:
1. Finds all fixes between `fromVersion` and `toVersion`
2. Sorts them by version
3. Applies them sequentially

## Skip If Current

If data is already at the target version, no fixes are applied:

```java
// No-op if already current
TaggedDynamic result = fixer.update(
    data,
    new DataVersion(200),  // Already at 200
    new DataVersion(200)   // Target is 200
);
// result == data (no changes)
```

## Error Handling

Fixes should handle missing or malformed data gracefully:

```java
private Dynamic<?> transformPlayer(Dynamic<?> player) {
    // Use orElse for safe defaults
    String name = player.get("name").asString().orElse("Unknown");
    int level = player.get("level").asInt().orElse(1);

    // Check before accessing nested data
    OptionalDynamic<?> maybePosition = player.get("position");
    double x = maybePosition.get("x").asDouble().orElse(0.0);

    // ... rest of transformation
}
```

## Multi-Type Fixes

A single fix can transform multiple types:

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        // Player changes
        Rules.renameField(TypeReferences.PLAYER, "hp", "health"),

        // Entity changes
        Rules.renameField(TypeReferences.ENTITY, "hp", "health"),

        // World changes
        Rules.transformField(TypeReferences.WORLD, "difficulty", this::difficultyToString)
    );
}
```

## Best Practices

### 1. One Fix Per Version Step

Create separate fix classes for each migration:

```java
// Good: Clear, focused fixes
PlayerV1ToV2Fix.java  // 100 → 110
PlayerV2ToV3Fix.java  // 110 → 200
PlayerV3ToV4Fix.java  // 200 → 210

// Avoid: Monolithic fix
PlayerFix.java  // 100 → 210 (too much in one)
```

### 2. Name Fixes Descriptively

```java
// Good: Clear naming
"player_v1_to_v2_rename_fields"
"world_v2_restructure_chunks"

// Avoid: Vague names
"fix1"
"update"
```

### 3. Handle Edge Cases

```java
private Dynamic<?> transform(Dynamic<?> data) {
    // Handle missing fields
    String value = data.get("field").asString().orElse("default");

    // Handle null values
    if (data.get("optional").result().isEmpty()) {
        return data.set("optional", data.createString("default"));
    }

    return data;
}
```

### 4. Keep Fixes Immutable

Fixes should not have mutable state:

```java
// Good: Stateless fix
public class MyFix extends SchemaDataFix {
    @Override
    protected TypeRewriteRule makeRule(...) {
        return Rules.transform(TYPE, this::transform);
    }
}

// Avoid: Mutable state
public class BadFix extends SchemaDataFix {
    private int counter = 0;  // BAD: Mutable state
}
```

### 5. Test Fixes Independently

Write unit tests for each fix:

```java
@Test
void testPlayerV1ToV2Migration() {
    // Given: v1 data
    JsonObject v1 = new JsonObject();
    v1.addProperty("playerName", "Steve");
    v1.addProperty("xp", 100);

    // When: Apply fix
    Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, v1);
    Dynamic<JsonElement> output = fix.apply(input);

    // Then: v2 structure
    assertEquals("Steve", output.get("name").asString().orElse(""));
    assertEquals(100, output.get("experience").asInt().orElse(0));
}
```

---

## Summary

| Component         | Purpose                     |
|-------------------|-----------------------------|
| `DataFix`         | Single migration (v1 → v2)  |
| `DataFixer`       | Orchestrates multiple fixes |
| `SchemaDataFix`   | Fix with schema access      |
| `TypeRewriteRule` | Transformation logic        |
| `Rules`           | Rule factory methods        |
| `FixRegistrar`    | Fix registration            |

---

## Related

- [Rewrite Rules](rewrite-rules.md) — Rule combinators in detail
- [Schema System](schema-system.md) — Where types are defined
- [Dynamic System](dynamic-system.md) — Data manipulation

