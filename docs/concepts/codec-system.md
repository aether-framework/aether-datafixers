# Codec System

The Codec system provides bidirectional transformation between typed Java objects and dynamic data representations. Codecs are the bridge between your domain objects and serialized data.

## Overview

```
                    encode()
    ┌──────────────────────────────────────────────────────┐
    │                                                      │
    │   Java Object                     Dynamic<T>         │
    │   ┌──────────┐                   ┌──────────┐        │
    │   │  Player  │ ───────────────▶  │  JSON    │        │
    │   │ {name,   │                   │ {name:   │        │
    │   │  level}  │                   │  level:} │        │
    │   └──────────┘                   └──────────┘        │
    │        ▲                              │              │
    │        │          decode()            │              │
    │        └──────────────────────────────┘              │
    │                                                      │
    └──────────────────────────────────────────────────────┘
                         Codec<Player>
```

A **Codec** encapsulates both serialization (encode) and deserialization (decode) logic for a specific type.

## Core Interfaces

### Encoder

Transforms a value into dynamic representation:

```java
public interface Encoder<A> {
    /**
     * Encode a value to dynamic format.
     *
     * @param input The value to encode
     * @param ops The format operations
     * @param prefix Existing data to merge with
     * @return Encoded data wrapped in DataResult
     */
    <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix);
}
```

### Decoder

Transforms dynamic representation into a typed value:

```java
public interface Decoder<A> {
    /**
     * Decode a value from dynamic format.
     *
     * @param input The dynamic data to decode
     * @return Decoded value with remaining data
     */
    <T> DataResult<Pair<A, T>> decode(Dynamic<T> input);
}
```

### Codec

Combines Encoder and Decoder:

```java
public interface Codec<A> extends Encoder<A>, Decoder<A> {
    // Inherits both encode() and decode()
}
```

### MapCodec

A codec that works with map structures:

```java
public interface MapCodec<A> {
    /**
     * Get the keys this codec reads/writes
     */
    Stream<String> keys();

    /**
     * Decode from a map context
     */
    <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input);

    /**
     * Encode to a map context
     */
    <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix);
}
```

## Built-in Codecs

The `Codecs` class provides primitive codecs:

```java
// Primitive codecs
Codecs.STRING    // String values
Codecs.INT       // int values
Codecs.LONG      // long values
Codecs.FLOAT     // float values
Codecs.DOUBLE    // double values
Codecs.BOOL      // boolean values
Codecs.BYTE      // byte values
Codecs.SHORT     // short values

// Special codecs
Codecs.EMPTY     // Unit/void codec
Codecs.PASSTHROUGH  // Pass-through (no transformation)
```

## Using Codecs

### Simple Encoding/Decoding

```java
// Encode a string
String name = "Steve";
DataResult<JsonElement> encoded = Codecs.STRING.encode(
    name,
    GsonOps.INSTANCE,
    GsonOps.INSTANCE.empty()
);
JsonElement json = encoded.result().orElseThrow();
// json = "Steve"

// Decode a string
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);
DataResult<Pair<String, JsonElement>> decoded = Codecs.STRING.decode(dynamic);
String value = decoded.result().orElseThrow().getFirst();
// value = "Steve"
```

### Field Codecs

Create codecs for object fields:

```java
// Create a field codec
MapCodec<String> nameField = Codecs.STRING.fieldOf("name");

// Encode to a field
RecordBuilder<JsonElement> builder = GsonOps.INSTANCE.mapBuilder();
builder = nameField.encode("Steve", GsonOps.INSTANCE, builder);
JsonElement result = builder.build(GsonOps.INSTANCE.empty()).result().orElseThrow();
// result = {"name": "Steve"}
```

## RecordCodecBuilder

For complex objects, use `RecordCodecBuilder`:

```java
public record Player(String name, int level, Position position) {

    public static final Codec<Player> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("name").forGetter(Player::name),
            Codecs.INT.fieldOf("level").forGetter(Player::level),
            Position.CODEC.fieldOf("position").forGetter(Player::position)
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
```

### Using the Codec

```java
// Encode
Player player = new Player("Steve", 10, new Position(100, 64, -200));
DataResult<JsonElement> encoded = Player.CODEC.encode(
    player,
    GsonOps.INSTANCE,
    GsonOps.INSTANCE.empty()
);

JsonElement json = encoded.result().orElseThrow();
// {
//   "name": "Steve",
//   "level": 10,
//   "position": {"x": 100.0, "y": 64.0, "z": -200.0}
// }

// Decode
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);
DataResult<Pair<Player, JsonElement>> decoded = Player.CODEC.decode(dynamic);
Player loaded = decoded.result().orElseThrow().getFirst();
```

## Codec Combinators

### List Codec

```java
// List of strings
Codec<List<String>> stringList = Codecs.STRING.listOf();

// List of players
Codec<List<Player>> playerList = Player.CODEC.listOf();
```

### Optional Codec

```java
// Optional field
MapCodec<Optional<String>> maybeNickname = Codecs.STRING.optionalFieldOf("nickname");

// Optional with default
MapCodec<String> nicknameWithDefault = Codecs.STRING.optionalFieldOf("nickname", "");
```

### Map Codec

```java
// Map with string keys
Codec<Map<String, Integer>> scores = Codec.unboundedMap(
    Codecs.STRING,
    Codecs.INT
);
```

### Either Codec

```java
// Value can be string OR int
Codec<Either<String, Integer>> stringOrInt = Codec.either(
    Codecs.STRING,
    Codecs.INT
);
```

### Pair Codec

```java
// Pair of values
Codec<Pair<String, Integer>> nameAndLevel = Codec.pair(
    Codecs.STRING.fieldOf("name").codec(),
    Codecs.INT.fieldOf("level").codec()
);
```

## Transforming Codecs

### xmap (Bidirectional)

Transform both encode and decode:

```java
// Transform string to enum and back
Codec<GameMode> gameModeCodec = Codecs.STRING.xmap(
    GameMode::valueOf,      // decode: String → GameMode
    GameMode::name          // encode: GameMode → String
);
```

### flatXmap (With Validation)

Transform with potential failure:

```java
Codec<PositiveInt> positiveInt = Codecs.INT.flatXmap(
    // decode: validate positive
    i -> i > 0
        ? DataResult.success(new PositiveInt(i))
        : DataResult.error("Must be positive: " + i),
    // encode: extract int
    pi -> DataResult.success(pi.value())
);
```

### comapFlatMap (One-way Validation)

Different handling for encode vs decode:

```java
Codec<String> nonEmpty = Codecs.STRING.comapFlatMap(
    // decode: validate non-empty
    s -> s.isEmpty()
        ? DataResult.error("String cannot be empty")
        : DataResult.success(s),
    // encode: no transformation
    Function.identity()
);
```

## Complete Example

```java
// Domain model
public record Inventory(List<ItemStack> items, int capacity) {

    public static final Codec<Inventory> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ItemStack.CODEC.listOf().fieldOf("items").forGetter(Inventory::items),
            Codecs.INT.optionalFieldOf("capacity", 36).forGetter(Inventory::capacity)
        ).apply(instance, Inventory::new)
    );
}

public record ItemStack(String itemId, int count, Map<String, String> nbt) {

    public static final Codec<ItemStack> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("id").forGetter(ItemStack::itemId),
            Codecs.INT.optionalFieldOf("count", 1).forGetter(ItemStack::count),
            Codec.unboundedMap(Codecs.STRING, Codecs.STRING)
                .optionalFieldOf("nbt", Map.of())
                .forGetter(ItemStack::nbt)
        ).apply(instance, ItemStack::new)
    );
}

// Usage
Inventory inventory = new Inventory(
    List.of(
        new ItemStack("diamond_sword", 1, Map.of("damage", "10")),
        new ItemStack("apple", 64, Map.of())
    ),
    36
);

// Encode
JsonElement json = Inventory.CODEC.encode(inventory, GsonOps.INSTANCE, GsonOps.INSTANCE.empty())
    .result().orElseThrow();

// Result:
// {
//   "items": [
//     {"id": "diamond_sword", "count": 1, "nbt": {"damage": "10"}},
//     {"id": "apple", "count": 64}
//   ],
//   "capacity": 36
// }

// Decode
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);
Inventory loaded = Inventory.CODEC.decode(dynamic)
    .result().orElseThrow().getFirst();
```

## Codec Registry

For polymorphic types, use a `CodecRegistry`:

```java
public interface CodecRegistry {
    <T> void register(TypeReference ref, Codec<T> codec);
    <T> Optional<Codec<T>> get(TypeReference ref);
}

// Registration
registry.register(TypeReferences.PLAYER, Player.CODEC);
registry.register(TypeReferences.WORLD, World.CODEC);

// Lookup
Codec<Player> playerCodec = registry.get(TypeReferences.PLAYER).orElseThrow();
```

## Error Handling

Codecs use `DataResult` for error handling:

```java
DataResult<Pair<Player, JsonElement>> result = Player.CODEC.decode(dynamic);

// Check success
if (result.result().isPresent()) {
    Player player = result.result().get().getFirst();
    // Use player
}

// Check error
if (result.error().isPresent()) {
    String message = result.error().get().message();
    // Handle error
}

// Get with logging
Player player = result.resultOrPartial(error ->
    logger.warn("Decode error: {}", error)
).orElseThrow();
```

## Best Practices

### 1. Define Codecs as Static Fields

```java
public record Player(String name, int level) {
    public static final Codec<Player> CODEC = ...;
}
```

### 2. Use Optional Fields for Backwards Compatibility

```java
// New field with default
Codecs.STRING.optionalFieldOf("newField", "default")
```

### 3. Compose Complex Codecs

```java
// Build from smaller pieces
public static final Codec<Position> POSITION = ...;
public static final Codec<Stats> STATS = ...;

public static final Codec<Player> PLAYER = RecordCodecBuilder.create(instance ->
    instance.group(
        POSITION.fieldOf("position").forGetter(Player::position),
        STATS.fieldOf("stats").forGetter(Player::stats),
        // ... other fields
    ).apply(instance, Player::new)
);
```

### 4. Validate During Decode

```java
Codec<Level> levelCodec = Codecs.INT.flatXmap(
    level -> level >= 1 && level <= 100
        ? DataResult.success(new Level(level))
        : DataResult.error("Level must be 1-100, got: " + level),
    level -> DataResult.success(level.value())
);
```

### 5. Document Codec Contracts

```java
/**
 * Codec for Player data.
 *
 * <p>Expected JSON structure:</p>
 * <pre>{@code
 * {
 *   "name": "string",
 *   "level": int (1-100),
 *   "position": {"x": double, "y": double, "z": double}
 * }
 * }</pre>
 */
public static final Codec<Player> CODEC = ...;
```

---

## Summary

| Component            | Purpose                       |
|----------------------|-------------------------------|
| `Encoder<A>`         | A → Dynamic (serialization)   |
| `Decoder<A>`         | Dynamic → A (deserialization) |
| `Codec<A>`           | Both encode and decode        |
| `MapCodec<A>`        | Codec for map structures      |
| `RecordCodecBuilder` | Build codecs for records      |
| `Codecs`             | Primitive codec instances     |
| `DataResult`         | Success/error handling        |

---

## Related

- [Dynamic System](dynamic-system.md) — Format-agnostic data
- [Type System](type-system.md) — Types contain codecs
- [DataResult](data-result.md) — Error handling

## Format-Specific Documentation

For detailed documentation on `DynamicOps` implementations:

- [Codec Module Overview](../codec/index.md) — All available implementations
- [JSON Support](../codec/json.md) — GsonOps and JacksonJsonOps
- [YAML Support](../codec/yaml.md) — SnakeYamlOps and JacksonYamlOps
- [TOML Support](../codec/toml.md) — JacksonTomlOps
- [XML Support](../codec/xml.md) — JacksonXmlOps

