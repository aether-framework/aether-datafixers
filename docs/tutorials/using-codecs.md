# Using Codecs Tutorial

This tutorial teaches you how to use codecs for encoding Java objects to dynamic formats and decoding them back. Codecs are essential for type-safe serialization and deserialization.

## Goal

Learn how to:
- Use primitive codecs
- Create field codecs
- Handle encoding/decoding results
- Build codecs for custom types

## Prerequisites

- Understanding of the [Dynamic System](../concepts/dynamic-system.md)
- Understanding of the [Codec System](../concepts/codec-system.md)

## Primitive Codecs

Aether Datafixers provides codecs for all primitive types:

```java
import de.splatgames.aether.datafixers.api.codec.Codecs;

// Available primitive codecs
Codecs.STRING   // String
Codecs.INT      // int / Integer
Codecs.LONG     // long / Long
Codecs.FLOAT    // float / Float
Codecs.DOUBLE   // double / Double
Codecs.BOOL     // boolean / Boolean
Codecs.BYTE     // byte / Byte
Codecs.SHORT    // short / Short
```

### Using Primitive Codecs

```java
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.codec.Codecs;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;

public class PrimitiveCodecExample {

    public static void main(String[] args) {
        // === ENCODING ===

        // Encode a string
        DataResult<JsonElement> stringResult = Codecs.STRING.encode(
            "Hello, World!",
            GsonOps.INSTANCE,
            GsonOps.INSTANCE.empty()
        );

        JsonElement stringJson = stringResult.result().orElseThrow();
        System.out.println("String: " + stringJson);  // "Hello, World!"

        // Encode an integer
        DataResult<JsonElement> intResult = Codecs.INT.encode(
            42,
            GsonOps.INSTANCE,
            GsonOps.INSTANCE.empty()
        );

        JsonElement intJson = intResult.result().orElseThrow();
        System.out.println("Int: " + intJson);  // 42

        // === DECODING ===

        // Decode a string
        Dynamic<JsonElement> stringDynamic = new Dynamic<>(GsonOps.INSTANCE, stringJson);
        DataResult<Pair<String, JsonElement>> decodedString = Codecs.STRING.decode(stringDynamic);

        String value = decodedString.result().orElseThrow().getFirst();
        System.out.println("Decoded: " + value);  // Hello, World!
    }
}
```

## Field Codecs

To encode/decode object fields, use `fieldOf`:

```java
import de.splatgames.aether.datafixers.api.codec.MapCodec;

// Create a field codec
MapCodec<String> nameCodec = Codecs.STRING.fieldOf("name");
MapCodec<Integer> ageCodec = Codecs.INT.fieldOf("age");
```

### Encoding Fields to JSON

```java
import de.splatgames.aether.datafixers.api.codec.RecordBuilder;

public JsonElement encodeFields() {
    MapCodec<String> nameCodec = Codecs.STRING.fieldOf("name");
    MapCodec<Integer> ageCodec = Codecs.INT.fieldOf("age");

    RecordBuilder<JsonElement> builder = GsonOps.INSTANCE.mapBuilder();

    // Add fields to the builder
    builder = nameCodec.encode("Steve", GsonOps.INSTANCE, builder);
    builder = ageCodec.encode(25, GsonOps.INSTANCE, builder);

    // Build the final result
    return builder.build(GsonOps.INSTANCE.empty()).result().orElseThrow();
    // {"name": "Steve", "age": 25}
}
```

## Optional Fields

Handle fields that might be missing:

```java
// Optional field - returns Optional<T>
MapCodec<Optional<String>> nicknameCodec = Codecs.STRING.optionalFieldOf("nickname");

// Optional field with default value
MapCodec<String> nicknameWithDefault = Codecs.STRING.optionalFieldOf("nickname", "Unknown");
MapCodec<Integer> livesWithDefault = Codecs.INT.optionalFieldOf("lives", 3);
```

### Using Optional Fields

```java
public void decodeOptionalFields(Dynamic<JsonElement> input) {
    MapCodec<Optional<String>> nicknameCodec = Codecs.STRING.optionalFieldOf("nickname");

    // Decode
    Optional<String> nickname = nicknameCodec.decode(
        GsonOps.INSTANCE,
        GsonOps.INSTANCE.getMap(input.value()).result().orElseThrow()
    ).result().orElse(Optional.empty());

    nickname.ifPresentOrElse(
        n -> System.out.println("Nickname: " + n),
        () -> System.out.println("No nickname set")
    );
}
```

## List Codecs

Create codecs for lists:

```java
// List of strings
Codec<List<String>> stringListCodec = Codecs.STRING.listOf();

// List of integers
Codec<List<Integer>> intListCodec = Codecs.INT.listOf();

// Encode a list
List<String> names = List.of("Steve", "Alex", "Bob");
DataResult<JsonElement> result = stringListCodec.encode(
    names,
    GsonOps.INSTANCE,
    GsonOps.INSTANCE.empty()
);
// ["Steve", "Alex", "Bob"]
```

## Map Codecs

Create codecs for maps:

```java
// Map with string keys and integer values
Codec<Map<String, Integer>> scoresCodec = Codec.unboundedMap(
    Codecs.STRING,
    Codecs.INT
);

// Encode a map
Map<String, Integer> scores = Map.of("Steve", 100, "Alex", 85);
DataResult<JsonElement> result = scoresCodec.encode(
    scores,
    GsonOps.INSTANCE,
    GsonOps.INSTANCE.empty()
);
// {"Steve": 100, "Alex": 85}
```

## Transforming Codecs

### xmap: Bidirectional Transformation

Transform both encode and decode:

```java
// Transform string to uppercase and back
Codec<String> upperCodec = Codecs.STRING.xmap(
    String::toUpperCase,  // decode transformation
    String::toLowerCase   // encode transformation
);

// Encode
upperCodec.encode("Hello", GsonOps.INSTANCE, GsonOps.INSTANCE.empty());
// Result: "hello" (lowercase)

// Decode
upperCodec.decode(new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive("hello")));
// Result: "HELLO" (uppercase)
```

### Enum Transformation

```java
public enum GameMode {
    SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR
}

Codec<GameMode> gameModeCodec = Codecs.STRING.xmap(
    GameMode::valueOf,  // decode: String → GameMode
    GameMode::name      // encode: GameMode → String
);

// Encode
gameModeCodec.encode(GameMode.CREATIVE, GsonOps.INSTANCE, GsonOps.INSTANCE.empty());
// Result: "CREATIVE"

// Decode
Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive("SURVIVAL"));
gameModeCodec.decode(input);
// Result: GameMode.SURVIVAL
```

### flatXmap: With Validation

Transform with potential failure:

```java
// Codec for positive integers only
Codec<Integer> positiveInt = Codecs.INT.flatXmap(
    // decode: validate positive
    value -> value > 0
        ? DataResult.success(value)
        : DataResult.error("Value must be positive: " + value),
    // encode: no validation needed
    DataResult::success
);

// Valid input
positiveInt.decode(new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(42)));
// Success(42)

// Invalid input
positiveInt.decode(new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(-5)));
// Error("Value must be positive: -5")
```

## Error Handling

Codecs use `DataResult` for error handling:

```java
public void handleErrors(Dynamic<JsonElement> input) {
    DataResult<Pair<String, JsonElement>> result = Codecs.STRING.decode(input);

    // Check success
    if (result.result().isPresent()) {
        String value = result.result().get().getFirst();
        System.out.println("Success: " + value);
    }

    // Check error
    if (result.error().isPresent()) {
        String message = result.error().get().message();
        System.err.println("Error: " + message);
    }

    // Or use resultOrPartial with logging
    Optional<String> value = result.resultOrPartial(
        error -> System.err.println("Warning: " + error)
    ).map(Pair::getFirst);
}
```

## Complete Example

```java
package com.example;

import com.google.gson.*;
import de.splatgames.aether.datafixers.api.codec.*;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;

import java.util.List;

public class CodecExample {

    public static void main(String[] args) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // === Primitive Encoding ===
        System.out.println("=== Primitive Encoding ===");

        JsonElement name = Codecs.STRING.encode("Steve", GsonOps.INSTANCE, GsonOps.INSTANCE.empty())
            .result().orElseThrow();
        System.out.println("Name: " + name);

        JsonElement level = Codecs.INT.encode(42, GsonOps.INSTANCE, GsonOps.INSTANCE.empty())
            .result().orElseThrow();
        System.out.println("Level: " + level);

        // === List Encoding ===
        System.out.println("\n=== List Encoding ===");

        Codec<List<String>> namesCodec = Codecs.STRING.listOf();
        JsonElement namesList = namesCodec.encode(
            List.of("Steve", "Alex", "Bob"),
            GsonOps.INSTANCE,
            GsonOps.INSTANCE.empty()
        ).result().orElseThrow();
        System.out.println("Names: " + namesList);

        // === Decoding ===
        System.out.println("\n=== Decoding ===");

        Dynamic<JsonElement> nameDynamic = new Dynamic<>(GsonOps.INSTANCE, name);
        String decodedName = Codecs.STRING.decode(nameDynamic)
            .result().orElseThrow().getFirst();
        System.out.println("Decoded name: " + decodedName);

        // === Enum Codec ===
        System.out.println("\n=== Enum Codec ===");

        Codec<GameMode> gameModeCodec = Codecs.STRING.xmap(
            GameMode::valueOf,
            GameMode::name
        );

        JsonElement modeJson = gameModeCodec.encode(GameMode.CREATIVE, GsonOps.INSTANCE, GsonOps.INSTANCE.empty())
            .result().orElseThrow();
        System.out.println("GameMode: " + modeJson);

        // === Validation ===
        System.out.println("\n=== Validation ===");

        Codec<Integer> positiveCodec = Codecs.INT.flatXmap(
            v -> v > 0 ? DataResult.success(v) : DataResult.error("Must be positive"),
            DataResult::success
        );

        DataResult<Pair<Integer, JsonElement>> valid = positiveCodec.decode(
            new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(10))
        );
        System.out.println("Valid (10): " + valid.result().map(Pair::getFirst));

        DataResult<Pair<Integer, JsonElement>> invalid = positiveCodec.decode(
            new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(-5))
        );
        System.out.println("Invalid (-5): " + invalid.error().map(e -> e.message()));
    }

    enum GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }
}
```

## Expected Output

```
=== Primitive Encoding ===
Name: "Steve"
Level: 42

=== List Encoding ===
Names: ["Steve","Alex","Bob"]

=== Decoding ===
Decoded name: Steve

=== Enum Codec ===
GameMode: "CREATIVE"

=== Validation ===
Valid (10): Optional[10]
Invalid (-5): Optional[Must be positive]
```

## Next Steps

- **[RecordCodecBuilder](record-codec-builder.md)** — Build codecs for complex records
- **[API Reference](https://software.splatgames.de/docs/aether/aether-datafixers/)** — Full API documentation
- **[DataResult](../concepts/data-result.md)** — Error handling patterns

