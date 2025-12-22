# Polymorphic Data Tutorial

This tutorial shows how to handle polymorphic data (sum types) using `TaggedChoice` in Aether Datafixers. This is essential when your data can be one of several different types.

## Goal

Learn how to:
- Model sum types with sealed interfaces
- Use `DSL.taggedChoice` in schemas
- Create codecs for polymorphic data
- Migrate polymorphic data between versions

## The Scenario

We have an entity system where entities can be Players, Monsters, or Items:

```java
// Each entity type has different fields
{"type": "player", "name": "Steve", "level": 10}
{"type": "monster", "species": "zombie", "health": 20}
{"type": "item", "itemId": "diamond", "count": 64}
```

## Step 1: Define the Sealed Interface

```java
package com.example.game;

public sealed interface Entity {

    record Player(String name, int level) implements Entity {}

    record Monster(String species, int health, int damage) implements Entity {}

    record Item(String itemId, int count) implements Entity {}
}
```

## Step 2: Define TypeReference

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.TypeReference;

public final class TypeReferences {

    public static final TypeReference ENTITY = new TypeReference("entity");

    private TypeReferences() {}
}
```

## Step 3: Create Schema with TaggedChoice

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

import java.util.Map;

public class Schema1 extends Schema {

    public Schema1() {
        super(new DataVersion(1), null, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.ENTITY, DSL.taggedChoice(
            "type",           // Discriminator field name
            DSL.string(),     // Discriminator type
            Map.of(
                "player", player(),
                "monster", monster(),
                "item", item()
            )
        ));
    }

    private DSL.TypeTemplate player() {
        return DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.remainder()
        );
    }

    private DSL.TypeTemplate monster() {
        return DSL.and(
            DSL.field("species", DSL.string()),
            DSL.field("health", DSL.intType()),
            DSL.field("damage", DSL.intType()),
            DSL.remainder()
        );
    }

    private DSL.TypeTemplate item() {
        return DSL.and(
            DSL.field("itemId", DSL.string()),
            DSL.field("count", DSL.intType()),
            DSL.remainder()
        );
    }
}
```

## Step 4: Create Codecs for Each Variant

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.codec.Codecs;
import de.splatgames.aether.datafixers.api.codec.RecordCodecBuilder;

public sealed interface Entity {

    record Player(String name, int level) implements Entity {
        public static final Codec<Player> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codecs.STRING.fieldOf("name").forGetter(Player::name),
                Codecs.INT.fieldOf("level").forGetter(Player::level)
            ).apply(instance, Player::new)
        );
    }

    record Monster(String species, int health, int damage) implements Entity {
        public static final Codec<Monster> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codecs.STRING.fieldOf("species").forGetter(Monster::species),
                Codecs.INT.fieldOf("health").forGetter(Monster::health),
                Codecs.INT.fieldOf("damage").forGetter(Monster::damage)
            ).apply(instance, Monster::new)
        );
    }

    record Item(String itemId, int count) implements Entity {
        public static final Codec<Item> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codecs.STRING.fieldOf("itemId").forGetter(Item::itemId),
                Codecs.INT.fieldOf("count").forGetter(Item::count)
            ).apply(instance, Item::new)
        );
    }
}
```

## Step 5: Create a Discriminated Union Codec

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.result.DataResult;

import java.util.Map;
import java.util.function.Function;

public class EntityCodec {

    private static final Map<String, Codec<? extends Entity>> CODECS = Map.of(
        "player", Entity.Player.CODEC,
        "monster", Entity.Monster.CODEC,
        "item", Entity.Item.CODEC
    );

    private static final Map<Class<? extends Entity>, String> TYPE_NAMES = Map.of(
        Entity.Player.class, "player",
        Entity.Monster.class, "monster",
        Entity.Item.class, "item"
    );

    public static final Codec<Entity> CODEC = new Codec<>() {

        @Override
        public <T> DataResult<T> encode(Entity input, DynamicOps<T> ops, T prefix) {
            String typeName = TYPE_NAMES.get(input.getClass());
            @SuppressWarnings("unchecked")
            Codec<Entity> codec = (Codec<Entity>) CODECS.get(typeName);

            return codec.encode(input, ops, prefix)
                .flatMap(encoded -> ops.mergeToMap(
                    encoded,
                    ops.createString("type"),
                    ops.createString(typeName)
                ));
        }

        @Override
        public <T> DataResult<Pair<Entity, T>> decode(Dynamic<T> input) {
            return input.get("type").asString()
                .flatMap(type -> {
                    Codec<? extends Entity> codec = CODECS.get(type);
                    if (codec == null) {
                        return DataResult.error("Unknown entity type: " + type);
                    }
                    return codec.decode(input).map(p -> Pair.of((Entity) p.getFirst(), p.getSecond()));
                });
        }
    };
}
```

## Step 6: Migrate Polymorphic Data

When a variant's structure changes, create a fix:

### Schema2: Monster gets a new field

```java
public class Schema2 extends Schema {

    public Schema2(Schema parent) {
        super(new DataVersion(2), parent, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        // Only ENTITY changed (Monster variant got new field)
        registerType(TypeReferences.ENTITY, DSL.taggedChoice(
            "type",
            DSL.string(),
            Map.of(
                "player", player(),
                "monster", monster(),  // Updated
                "item", item()
            )
        ));
    }

    private DSL.TypeTemplate player() {
        return DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.remainder()
        );
    }

    private DSL.TypeTemplate monster() {
        return DSL.and(
            DSL.field("species", DSL.string()),
            DSL.field("health", DSL.intType()),
            DSL.field("damage", DSL.intType()),
            DSL.field("lootTable", DSL.string()),  // NEW
            DSL.remainder()
        );
    }

    private DSL.TypeTemplate item() {
        return DSL.and(
            DSL.field("itemId", DSL.string()),
            DSL.field("count", DSL.intType()),
            DSL.remainder()
        );
    }
}
```

### Fix for Monster variant

```java
public class EntityV1ToV2Fix extends SchemaDataFix {

    public EntityV1ToV2Fix(SchemaRegistry schemas) {
        super("entity_v1_to_v2", new DataVersion(1), new DataVersion(2), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.ENTITY, this::addMonsterLootTable);
    }

    private Dynamic<?> addMonsterLootTable(Dynamic<?> entity) {
        // Check if this is a monster
        String type = entity.get("type").asString().orElse("");

        if ("monster".equals(type)) {
            // Add lootTable based on species
            String species = entity.get("species").asString().orElse("generic");
            String lootTable = "loot_tables/" + species + ".json";
            return entity.set("lootTable", entity.createString(lootTable));
        }

        // Not a monster, return unchanged
        return entity;
    }
}
```

## Complete Example

```java
package com.example.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;

public class PolymorphicExample {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        AetherDataFixer fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

        // Test with different entity types
        System.out.println("=== Player Entity ===");
        testEntity(fixer, createPlayer());

        System.out.println("\n=== Monster Entity ===");
        testEntity(fixer, createMonster());

        System.out.println("\n=== Item Entity ===");
        testEntity(fixer, createItem());
    }

    private static void testEntity(AetherDataFixer fixer, JsonObject entity) {
        System.out.println("Before:");
        System.out.println(GSON.toJson(entity));

        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, entity);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.ENTITY, dynamic);

        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(1),
            GameDataBootstrap.CURRENT_VERSION
        );

        @SuppressWarnings("unchecked")
        Dynamic<JsonElement> result = (Dynamic<JsonElement>) migrated.value();
        System.out.println("After:");
        System.out.println(GSON.toJson(result.value()));
    }

    private static JsonObject createPlayer() {
        JsonObject player = new JsonObject();
        player.addProperty("type", "player");
        player.addProperty("name", "Steve");
        player.addProperty("level", 10);
        return player;
    }

    private static JsonObject createMonster() {
        JsonObject monster = new JsonObject();
        monster.addProperty("type", "monster");
        monster.addProperty("species", "zombie");
        monster.addProperty("health", 20);
        monster.addProperty("damage", 3);
        return monster;
    }

    private static JsonObject createItem() {
        JsonObject item = new JsonObject();
        item.addProperty("type", "item");
        item.addProperty("itemId", "diamond");
        item.addProperty("count", 64);
        return item;
    }
}
```

## Expected Output

```
=== Player Entity ===
Before:
{
  "type": "player",
  "name": "Steve",
  "level": 10
}
After:
{
  "type": "player",
  "name": "Steve",
  "level": 10
}

=== Monster Entity ===
Before:
{
  "type": "monster",
  "species": "zombie",
  "health": 20,
  "damage": 3
}
After:
{
  "type": "monster",
  "species": "zombie",
  "health": 20,
  "damage": 3,
  "lootTable": "loot_tables/zombie.json"
}

=== Item Entity ===
Before:
{
  "type": "item",
  "itemId": "diamond",
  "count": 64
}
After:
{
  "type": "item",
  "itemId": "diamond",
  "count": 64
}
```

## Key Points

### TaggedChoice Structure

```java
DSL.taggedChoice(
    "discriminator_field",  // Field that determines the type
    DSL.string(),           // Type of the discriminator
    Map.of(
        "value1", template1,  // When discriminator = "value1"
        "value2", template2,  // When discriminator = "value2"
        ...
    )
)
```

### Pattern Matching in Fixes

```java
private Dynamic<?> transformEntity(Dynamic<?> entity) {
    String type = entity.get("type").asString().orElse("");

    return switch (type) {
        case "player" -> transformPlayer(entity);
        case "monster" -> transformMonster(entity);
        case "item" -> transformItem(entity);
        default -> entity;  // Unknown type, leave unchanged
    };
}
```

### Adding New Variants

When adding a new entity type:

1. Add to sealed interface
2. Add codec for new variant
3. Update schema with new choice
4. No fix needed (new data, not migration)

### Removing Variants

When removing an entity type:

1. Create fix to convert old type to replacement
2. Remove from schema
3. Keep handling in fix for migration

## Next Steps

- **[Nested Transformations](nested-transformations.md)** — Complex restructuring
- **[DSL Reference](../concepts/dsl.md)** — TaggedChoice details
- **[Prism Optic](../concepts/optics/prism.md)** — Sum type optics

