# Entity Polymorphism Example

Handling polymorphic data with TaggedChoice for different entity types.

## Scenario

A game has different entity types that share common fields but have type-specific data:

- **Player**: Has inventory and experience
- **Monster**: Has damage and drops
- **NPC**: Has dialogue and quests

Each entity type needs to migrate independently while sharing the common structure.

## Data Structure

### Entity with Type Tag

```json
{
  "id": "entity_12345",
  "type": "player",
  "position": { "x": 100.0, "y": 64.0, "z": -50.0 },
  "data": {
    "name": "Steve",
    "experience": 1500,
    "inventory": [...]
  }
}
```

```json
{
  "id": "entity_67890",
  "type": "monster",
  "position": { "x": 200.0, "y": 64.0, "z": 100.0 },
  "data": {
    "mobType": "zombie",
    "health": 20,
    "damage": 3
  }
}
```

## TypeReferences

```java
public final class EntityReferences {
    public static final TypeReference ENTITY = TypeReference.of("entity");
    public static final TypeReference PLAYER_DATA = TypeReference.of("player_data");
    public static final TypeReference MONSTER_DATA = TypeReference.of("monster_data");
    public static final TypeReference NPC_DATA = TypeReference.of("npc_data");

    private EntityReferences() {}
}
```

## Schema with TaggedChoice

```java
public class EntitySchemaV1 extends Schema {

    public EntitySchemaV1(Schema parent) {
        super(new DataVersion(1), parent);
    }

    @Override
    protected void registerTypes() {
        // Register type-specific data schemas
        registerType(EntityReferences.PLAYER_DATA, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("experience", DSL.intType()),
            DSL.optional("inventory", DSL.list(DSL.remainder())),
            DSL.remainder()
        ));

        registerType(EntityReferences.MONSTER_DATA, DSL.and(
            DSL.field("mobType", DSL.string()),
            DSL.field("health", DSL.intType()),
            DSL.field("damage", DSL.intType()),
            DSL.remainder()
        ));

        registerType(EntityReferences.NPC_DATA, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("dialogue", DSL.list(DSL.string())),
            DSL.remainder()
        ));

        // Register entity with TaggedChoice for polymorphic data
        registerType(EntityReferences.ENTITY, DSL.and(
            DSL.field("id", DSL.string()),
            DSL.field("type", DSL.string()),
            DSL.field("position", DSL.and(
                DSL.field("x", DSL.doubleType()),
                DSL.field("y", DSL.doubleType()),
                DSL.field("z", DSL.doubleType()),
                DSL.remainder()
            )),
            DSL.field("data", DSL.taggedChoice(
                "type",  // Field that determines the type
                Map.of(
                    "player", EntityReferences.PLAYER_DATA,
                    "monster", EntityReferences.MONSTER_DATA,
                    "npc", EntityReferences.NPC_DATA
                )
            )),
            DSL.remainder()
        ));
    }
}
```

## Schema V2 (Updated)

```java
public class EntitySchemaV2 extends Schema {

    public EntitySchemaV2(Schema parent) {
        super(new DataVersion(2), parent);
    }

    @Override
    protected void registerTypes() {
        // Player data: experience -> level + exp
        registerType(EntityReferences.PLAYER_DATA, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.field("exp", DSL.intType()),
            DSL.optional("inventory", DSL.list(DSL.remainder())),
            DSL.remainder()
        ));

        // Monster data: added lootTable
        registerType(EntityReferences.MONSTER_DATA, DSL.and(
            DSL.field("mobType", DSL.string()),
            DSL.field("health", DSL.intType()),
            DSL.field("damage", DSL.intType()),
            DSL.field("lootTable", DSL.string()),
            DSL.remainder()
        ));

        // NPC data: added questGiver flag
        registerType(EntityReferences.NPC_DATA, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("dialogue", DSL.list(DSL.string())),
            DSL.field("questGiver", DSL.bool()),
            DSL.remainder()
        ));

        // Entity structure unchanged
        registerType(EntityReferences.ENTITY, DSL.and(
            DSL.field("id", DSL.string()),
            DSL.field("type", DSL.string()),
            DSL.field("position", DSL.and(
                DSL.field("x", DSL.doubleType()),
                DSL.field("y", DSL.doubleType()),
                DSL.field("z", DSL.doubleType()),
                DSL.remainder()
            )),
            DSL.field("data", DSL.taggedChoice(
                "type",
                Map.of(
                    "player", EntityReferences.PLAYER_DATA,
                    "monster", EntityReferences.MONSTER_DATA,
                    "npc", EntityReferences.NPC_DATA
                )
            )),
            DSL.remainder()
        ));
    }
}
```

## Type-Specific Fixes

### Player Data Fix

```java
public class PlayerDataV1ToV2Fix extends SchemaDataFix {

    public PlayerDataV1ToV2Fix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(1), new DataVersion(2), "player-data-v1-to-v2");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(EntityReferences.PLAYER_DATA, this::migratePlayerData);
    }

    private Dynamic<?> migratePlayerData(Dynamic<?> player) {
        // Convert experience to level + remaining exp
        int totalExp = player.get("experience").asInt().orElse(0);
        int level = totalExp / 100;
        int remainingExp = totalExp % 100;

        return player
            .remove("experience")
            .set("level", player.createInt(level))
            .set("exp", player.createInt(remainingExp));
    }
}
```

### Monster Data Fix

```java
public class MonsterDataV1ToV2Fix extends SchemaDataFix {

    public MonsterDataV1ToV2Fix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(1), new DataVersion(2), "monster-data-v1-to-v2");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(EntityReferences.MONSTER_DATA, this::migrateMonsterData);
    }

    private Dynamic<?> migrateMonsterData(Dynamic<?> monster) {
        // Add loot table based on mob type
        String mobType = monster.get("mobType").asString().orElse("zombie");
        String lootTable = "loot:" + mobType;

        return monster.set("lootTable", monster.createString(lootTable));
    }
}
```

### NPC Data Fix

```java
public class NPCDataV1ToV2Fix extends SchemaDataFix {

    public NPCDataV1ToV2Fix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(1), new DataVersion(2), "npc-data-v1-to-v2");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.addField(
            EntityReferences.NPC_DATA,
            "questGiver",
            npc -> npc.createBoolean(false)  // Default to false
        );
    }
}
```

## Bootstrap

```java
public class EntityBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(2);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;
        schemas.register(new DataVersion(1), EntitySchemaV1::new);
        schemas.register(new DataVersion(2), EntitySchemaV2::new);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Register fixes for each type-specific data
        fixes.register(EntityReferences.PLAYER_DATA, new PlayerDataV1ToV2Fix(schemas));
        fixes.register(EntityReferences.MONSTER_DATA, new MonsterDataV1ToV2Fix(schemas));
        fixes.register(EntityReferences.NPC_DATA, new NPCDataV1ToV2Fix(schemas));
    }
}
```

## Usage

```java
public class EntityMigrator {

    private final AetherDataFixer fixer;

    public EntityMigrator() {
        this.fixer = new DataFixerRuntimeFactory()
            .create(EntityBootstrap.CURRENT_VERSION, new EntityBootstrap());
    }

    public JsonObject migrateEntity(JsonObject entity, int fromVersion) {
        // Determine which type reference to use based on entity type
        String type = entity.get("type").getAsString();
        TypeReference dataType = switch (type) {
            case "player" -> EntityReferences.PLAYER_DATA;
            case "monster" -> EntityReferences.MONSTER_DATA;
            case "npc" -> EntityReferences.NPC_DATA;
            default -> throw new IllegalArgumentException("Unknown entity type: " + type);
        };

        // Migrate the data field specifically
        JsonObject data = entity.getAsJsonObject("data");
        Dynamic<JsonElement> dataDynamic = new Dynamic<>(GsonOps.INSTANCE, data);
        TaggedDynamic taggedData = new TaggedDynamic(dataType, dataDynamic);

        TaggedDynamic migratedData = fixer.update(
            taggedData,
            new DataVersion(fromVersion),
            EntityBootstrap.CURRENT_VERSION
        );

        // Replace data in entity
        JsonObject result = entity.deepCopy();
        result.add("data", (JsonElement) migratedData.value().value());

        return result;
    }
}
```

## Example Migration

### Input (V1 Player Entity)

```json
{
  "id": "player_001",
  "type": "player",
  "position": { "x": 100.0, "y": 64.0, "z": -50.0 },
  "data": {
    "name": "Steve",
    "experience": 1234
  }
}
```

### Output (V2 Player Entity)

```json
{
  "id": "player_001",
  "type": "player",
  "position": { "x": 100.0, "y": 64.0, "z": -50.0 },
  "data": {
    "name": "Steve",
    "level": 12,
    "exp": 34
  }
}
```

### Input (V1 Monster Entity)

```json
{
  "id": "monster_001",
  "type": "monster",
  "position": { "x": 200.0, "y": 64.0, "z": 100.0 },
  "data": {
    "mobType": "skeleton",
    "health": 20,
    "damage": 4
  }
}
```

### Output (V2 Monster Entity)

```json
{
  "id": "monster_001",
  "type": "monster",
  "position": { "x": 200.0, "y": 64.0, "z": 100.0 },
  "data": {
    "mobType": "skeleton",
    "health": 20,
    "damage": 4,
    "lootTable": "loot:skeleton"
  }
}
```

## Key Patterns Demonstrated

1. **TaggedChoice** — Type discriminator for polymorphic data
2. **Type-specific fixes** — Different migrations per entity type
3. **Shared structure** — Common fields with varying data
4. **Independent evolution** — Each type evolves separately

## Related

- [Polymorphic Data Tutorial](../tutorials/polymorphic-data.md)
- [DSL Reference](../concepts/dsl.md)
- [Prism Optic](../concepts/optics/prism.md)

