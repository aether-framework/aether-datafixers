# RecordCodecBuilder Tutorial

This tutorial teaches you how to use `RecordCodecBuilder` to create codecs for complex Java records and classes. This is the most powerful and commonly used way to define codecs.

## Goal

Learn how to:
- Build codecs for records with multiple fields
- Handle nested objects
- Use optional fields with defaults
- Compose complex codec structures

## Prerequisites

- Completed [Using Codecs](using-codecs.md) tutorial
- Understanding of Java records

## Basic Record Codec

### Step 1: Define Your Record

```java
public record Player(String name, int level) {}
```

### Step 2: Create the Codec

```java
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.codec.Codecs;
import de.splatgames.aether.datafixers.api.codec.RecordCodecBuilder;

public record Player(String name, int level) {

    public static final Codec<Player> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("name").forGetter(Player::name),
            Codecs.INT.fieldOf("level").forGetter(Player::level)
        ).apply(instance, Player::new)
    );
}
```

### Step 3: Use the Codec

```java
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;

public class RecordCodecExample {

    public static void main(String[] args) {
        // Create a player
        Player player = new Player("Steve", 42);

        // Encode to JSON
        DataResult<JsonElement> encoded = Player.CODEC.encode(
            player,
            GsonOps.INSTANCE,
            GsonOps.INSTANCE.empty()
        );

        JsonElement json = encoded.result().orElseThrow();
        System.out.println("Encoded: " + json);
        // {"name": "Steve", "level": 42}

        // Decode from JSON
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);
        DataResult<Player> decoded = Player.CODEC.decode(dynamic)
            .map(pair -> pair.getFirst());

        Player loadedPlayer = decoded.result().orElseThrow();
        System.out.println("Decoded: " + loadedPlayer);
        // Player[name=Steve, level=42]
    }
}
```

## Understanding the Pattern

```java
RecordCodecBuilder.create(instance ->
    instance.group(
        // Field 1: type, JSON key, getter
        Codecs.STRING.fieldOf("name").forGetter(Player::name),

        // Field 2: type, JSON key, getter
        Codecs.INT.fieldOf("level").forGetter(Player::level)

    ).apply(instance, Player::new)  // Constructor reference
);
```

### The Pattern Breakdown

1. **`RecordCodecBuilder.create(instance -> ...)`** — Starts the builder
2. **`instance.group(...)`** — Groups all field codecs
3. **`Codecs.TYPE.fieldOf("key")`** — Creates a field codec for a JSON key
4. **`.forGetter(Record::field)`** — Specifies how to get the value during encoding
5. **`.apply(instance, Constructor::new)`** — Specifies how to construct during decoding

## Multiple Fields (3+)

For records with more fields, just add them to the group:

```java
public record Player(
    String name,
    int level,
    int experience,
    double health,
    boolean isActive
) {

    public static final Codec<Player> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("name").forGetter(Player::name),
            Codecs.INT.fieldOf("level").forGetter(Player::level),
            Codecs.INT.fieldOf("experience").forGetter(Player::experience),
            Codecs.DOUBLE.fieldOf("health").forGetter(Player::health),
            Codecs.BOOL.fieldOf("active").forGetter(Player::isActive)
        ).apply(instance, Player::new)
    );
}
```

## Optional Fields

### With Default Values

```java
public record Settings(
    String language,
    int volume,
    boolean fullscreen
) {

    public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.optionalFieldOf("language", "en").forGetter(Settings::language),
            Codecs.INT.optionalFieldOf("volume", 100).forGetter(Settings::volume),
            Codecs.BOOL.optionalFieldOf("fullscreen", false).forGetter(Settings::fullscreen)
        ).apply(instance, Settings::new)
    );
}

// Decodes {"language": "de"} → Settings("de", 100, false)
// Missing fields use defaults
```

### With Optional Type

```java
public record Profile(
    String name,
    Optional<String> email,
    Optional<String> bio
) {

    public static final Codec<Profile> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("name").forGetter(Profile::name),
            Codecs.STRING.optionalFieldOf("email").forGetter(Profile::email),
            Codecs.STRING.optionalFieldOf("bio").forGetter(Profile::bio)
        ).apply(instance, Profile::new)
    );
}

// Decodes {"name": "Steve"} → Profile("Steve", Optional.empty(), Optional.empty())
```

## Nested Objects

Compose codecs for nested structures:

```java
public record Position(double x, double y, double z) {

    public static final Codec<Position> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.DOUBLE.fieldOf("x").forGetter(Position::x),
            Codecs.DOUBLE.fieldOf("y").forGetter(Position::y),
            Codecs.DOUBLE.fieldOf("z").forGetter(Position::z)
        ).apply(instance, Position::new)
    );
}

public record Player(String name, int level, Position position) {

    public static final Codec<Player> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("name").forGetter(Player::name),
            Codecs.INT.fieldOf("level").forGetter(Player::level),
            // Use the nested codec
            Position.CODEC.fieldOf("position").forGetter(Player::position)
        ).apply(instance, Player::new)
    );
}
```

### JSON Structure

```json
{
  "name": "Steve",
  "level": 42,
  "position": {
    "x": 100.5,
    "y": 64.0,
    "z": -200.25
  }
}
```

## Lists and Collections

```java
public record Inventory(List<ItemStack> items, int capacity) {

    public static final Codec<Inventory> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(Inventory::items),
            Codecs.INT.optionalFieldOf("capacity", 36).forGetter(Inventory::capacity)
        ).apply(instance, Inventory::new)
    );
}

public record ItemStack(String itemId, int count) {

    public static final Codec<ItemStack> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("id").forGetter(ItemStack::itemId),
            Codecs.INT.optionalFieldOf("count", 1).forGetter(ItemStack::count)
        ).apply(instance, ItemStack::new)
    );
}
```

### JSON Structure

```json
{
  "items": [
    {"id": "diamond_sword", "count": 1},
    {"id": "apple", "count": 64}
  ],
  "capacity": 36
}
```

## Maps

```java
public record PlayerData(
    String name,
    Map<String, Integer> stats,
    Map<String, String> attributes
) {

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("name").forGetter(PlayerData::name),
            Codec.unboundedMap(Codecs.STRING, Codecs.INT)
                .fieldOf("stats").forGetter(PlayerData::stats),
            Codec.unboundedMap(Codecs.STRING, Codecs.STRING)
                .fieldOf("attributes").forGetter(PlayerData::attributes)
        ).apply(instance, PlayerData::new)
    );
}
```

### JSON Structure

```json
{
  "name": "Steve",
  "stats": {
    "health": 20,
    "mana": 100,
    "strength": 15
  },
  "attributes": {
    "class": "warrior",
    "guild": "heroes"
  }
}
```

## Complex Example

Here's a complete example with all features:

```java
public record GameSave(
    String version,
    Player player,
    World world,
    Settings settings,
    List<Achievement> achievements
) {

    public static final Codec<GameSave> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("version").forGetter(GameSave::version),
            Player.CODEC.fieldOf("player").forGetter(GameSave::player),
            World.CODEC.fieldOf("world").forGetter(GameSave::world),
            Settings.CODEC.optionalFieldOf("settings", Settings.DEFAULT)
                .forGetter(GameSave::settings),
            Achievement.CODEC.listOf().optionalFieldOf("achievements", List.of())
                .forGetter(GameSave::achievements)
        ).apply(instance, GameSave::new)
    );
}

public record Player(String name, int level, Position position, Inventory inventory) {

    public static final Codec<Player> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("name").forGetter(Player::name),
            Codecs.INT.fieldOf("level").forGetter(Player::level),
            Position.CODEC.fieldOf("position").forGetter(Player::position),
            Inventory.CODEC.fieldOf("inventory").forGetter(Player::inventory)
        ).apply(instance, Player::new)
    );
}

public record Position(double x, double y, double z) {

    public static final Codec<Position> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.DOUBLE.fieldOf("x").forGetter(Position::x),
            Codecs.DOUBLE.fieldOf("y").forGetter(Position::y),
            Codecs.DOUBLE.fieldOf("z").forGetter(Position::z)
        ).apply(instance, Position::new)
    );
}

public record Inventory(List<ItemStack> items) {

    public static final Codec<Inventory> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(Inventory::items)
        ).apply(instance, Inventory::new)
    );
}

public record ItemStack(String id, int count) {

    public static final Codec<ItemStack> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("id").forGetter(ItemStack::id),
            Codecs.INT.optionalFieldOf("count", 1).forGetter(ItemStack::count)
        ).apply(instance, ItemStack::new)
    );
}

public record World(String name, long seed) {

    public static final Codec<World> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("name").forGetter(World::name),
            Codecs.LONG.fieldOf("seed").forGetter(World::seed)
        ).apply(instance, World::new)
    );
}

public record Settings(String language, int volume) {

    public static final Settings DEFAULT = new Settings("en", 100);

    public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.optionalFieldOf("language", "en").forGetter(Settings::language),
            Codecs.INT.optionalFieldOf("volume", 100).forGetter(Settings::volume)
        ).apply(instance, Settings::new)
    );
}

public record Achievement(String id, long timestamp) {

    public static final Codec<Achievement> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("id").forGetter(Achievement::id),
            Codecs.LONG.fieldOf("timestamp").forGetter(Achievement::timestamp)
        ).apply(instance, Achievement::new)
    );
}
```

## Best Practices

### 1. Define Codec as Static Final Field

```java
public record Player(...) {
    public static final Codec<Player> CODEC = ...;
}
```

### 2. Use Meaningful Field Names

```java
// JSON will use these names
.fieldOf("player_name")  // → {"player_name": "..."}
.fieldOf("playerName")   // → {"playerName": "..."}
```

### 3. Document the JSON Structure

```java
/**
 * Player codec.
 *
 * JSON structure:
 * <pre>{@code
 * {
 *   "name": "string",
 *   "level": int,
 *   "position": {"x": double, "y": double, "z": double}
 * }
 * }</pre>
 */
public static final Codec<Player> CODEC = ...;
```

### 4. Use optionalFieldOf for Backwards Compatibility

```java
// New field with default - old data without this field will work
.optionalFieldOf("newField", defaultValue)
```

## Common Mistakes

| Mistake | Solution |
|---------|----------|
| Wrong field order in `apply` | Order must match `group` order |
| Missing `forGetter` | Each field needs a getter |
| Wrong constructor arity | Constructor params must match field count |
| Using wrong codec type | Ensure codec type matches field type |

## Next Steps

- **[Polymorphic Data](polymorphic-data.md)** — Handle sum types
- **[API Reference](https://software.splatgames.de/docs/aether/aether-datafixers/)** — Full API
- **[Using Codecs](using-codecs.md)** — Codec basics

