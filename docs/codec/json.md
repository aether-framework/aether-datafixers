# JSON Support

The codec module provides two `DynamicOps` implementations for JSON: **GsonOps** (using Google Gson) and **JacksonJsonOps** (using Jackson Databind).

## GsonOps

### Overview

`GsonOps` works with Gson's `JsonElement` tree model. It's a lightweight choice with minimal dependencies.

**Package:** `de.splatgames.aether.datafixers.codec.json.gson`

### Dependencies

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-codec</artifactId>
    <version>${aether.version}</version>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>
```

### Basic Usage

```java
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;

// Create JSON data
JsonObject json = new JsonObject();
json.addProperty("name", "Steve");
json.addProperty("level", 42);

// Wrap in Dynamic
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);

// Read values
String name = dynamic.get("name").asString().orElse("Unknown");
int level = dynamic.get("level").asInt().orElse(1);

// Modify data
Dynamic<JsonElement> updated = dynamic
    .set("experience", dynamic.createInt(1500))
    .remove("level");
```

### Parse JSON String

```java
import com.google.gson.Gson;
import com.google.gson.JsonParser;

// Using JsonParser (recommended)
JsonElement element = JsonParser.parseString("{\"name\": \"Steve\"}");
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, element);

// Using Gson instance
Gson gson = new Gson();
JsonElement element = gson.fromJson(jsonString, JsonElement.class);
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, element);
```

### Write JSON String

```java
import com.google.gson.GsonBuilder;

Dynamic<JsonElement> dynamic = // ... your dynamic

// Pretty print
Gson gson = new GsonBuilder().setPrettyPrinting().create();
String json = gson.toJson(dynamic.value());

// Compact
String compact = new Gson().toJson(dynamic.value());
```

### File I/O

```java
// Read from file
public Dynamic<JsonElement> readJson(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path)) {
        JsonElement element = JsonParser.parseReader(reader);
        return new Dynamic<>(GsonOps.INSTANCE, element);
    }
}

// Write to file
public void writeJson(Path path, Dynamic<JsonElement> dynamic) throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    try (Writer writer = Files.newBufferedWriter(path)) {
        gson.toJson(dynamic.value(), writer);
    }
}
```

---

## JacksonJsonOps

### Overview

`JacksonJsonOps` works with Jackson's `JsonNode` tree model. It offers excellent performance and supports custom `ObjectMapper` configurations.

**Package:** `de.splatgames.aether.datafixers.codec.json.jackson`

### Dependencies

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-codec</artifactId>
    <version>${aether.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.2</version>
</dependency>
```

### Basic Usage

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;

// Create JSON data
ObjectMapper mapper = new ObjectMapper();
ObjectNode json = mapper.createObjectNode();
json.put("name", "Steve");
json.put("level", 42);

// Wrap in Dynamic
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonJsonOps.INSTANCE, json);

// Read values
String name = dynamic.get("name").asString().orElse("Unknown");
int level = dynamic.get("level").asInt().orElse(1);

// Modify data
Dynamic<JsonNode> updated = dynamic
    .set("experience", dynamic.createInt(1500))
    .remove("level");
```

### Parse JSON String

```java
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();
JsonNode node = mapper.readTree("{\"name\": \"Steve\"}");
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonJsonOps.INSTANCE, node);
```

### Write JSON String

```java
ObjectMapper mapper = new ObjectMapper();

// Pretty print
mapper.enable(SerializationFeature.INDENT_OUTPUT);
String json = mapper.writeValueAsString(dynamic.value());

// Compact
String compact = new ObjectMapper().writeValueAsString(dynamic.value());
```

### Custom ObjectMapper

```java
// Create custom ObjectMapper
ObjectMapper customMapper = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL);

// Create custom JacksonJsonOps instance
JacksonJsonOps customOps = new JacksonJsonOps(customMapper);

// Use with Dynamic
Dynamic<JsonNode> dynamic = new Dynamic<>(customOps, jsonNode);
```

### File I/O

```java
ObjectMapper mapper = new ObjectMapper();

// Read from file
public Dynamic<JsonNode> readJson(Path path) throws IOException {
    JsonNode node = mapper.readTree(path.toFile());
    return new Dynamic<>(JacksonJsonOps.INSTANCE, node);
}

// Write to file
public void writeJson(Path path, Dynamic<JsonNode> dynamic) throws IOException {
    mapper.writerWithDefaultPrettyPrinter()
          .writeValue(path.toFile(), dynamic.value());
}
```

---

## Comparison

| Feature | GsonOps | JacksonJsonOps |
|---------|---------|----------------|
| **Data Type** | `JsonElement` | `JsonNode` |
| **Library Size** | ~300 KB | ~1.7 MB |
| **Performance** | Good | Excellent |
| **Streaming API** | No | Yes |
| **Custom Config** | No | Yes (ObjectMapper) |
| **Null Handling** | `JsonNull` | `NullNode` |
| **Dependencies** | Gson only | Jackson Databind |

## Migration Example

```java
public class JsonMigrator<T> {

    private final AetherDataFixer fixer;
    private final DynamicOps<T> ops;

    public JsonMigrator(AetherDataFixer fixer, DynamicOps<T> ops) {
        this.fixer = fixer;
        this.ops = ops;
    }

    public T migrate(T input, int fromVersion, int toVersion, TypeReference type) {
        Dynamic<T> dynamic = new Dynamic<>(ops, input);
        TaggedDynamic<T> tagged = new TaggedDynamic<>(type, dynamic);

        TaggedDynamic<T> result = fixer.update(
            tagged,
            new DataVersion(fromVersion),
            new DataVersion(toVersion)
        );

        return result.value().value();
    }
}

// Usage with Gson
JsonMigrator<JsonElement> gsonMigrator = new JsonMigrator<>(fixer, GsonOps.INSTANCE);
JsonElement migrated = gsonMigrator.migrate(jsonElement, 1, 2, TypeReferences.PLAYER);

// Usage with Jackson
JsonMigrator<JsonNode> jacksonMigrator = new JsonMigrator<>(fixer, JacksonJsonOps.INSTANCE);
JsonNode migrated = jacksonMigrator.migrate(jsonNode, 1, 2, TypeReferences.PLAYER);
```

## Working with Nested Data

```java
Dynamic<JsonElement> player = new Dynamic<>(GsonOps.INSTANCE, playerJson);

// Read nested value
double x = player.get("position").get("x").asDouble().orElse(0.0);

// Create nested structure
Dynamic<JsonElement> position = player.emptyMap()
    .set("x", player.createDouble(100.0))
    .set("y", player.createDouble(64.0))
    .set("z", player.createDouble(-50.0));

player = player.set("position", position);
```

## Working with Lists

```java
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);

// Read list
List<String> names = dynamic.get("names")
    .asStream()
    .flatMap(d -> d.asString().result().stream())
    .toList();

// Create list
List<Dynamic<JsonElement>> items = List.of(
    dynamic.createString("sword"),
    dynamic.createString("shield"),
    dynamic.createString("potion")
);
Dynamic<JsonElement> inventory = dynamic.createList(items.stream());
```

## Codec Integration

```java
// Define codec
Codec<Player> playerCodec = RecordCodecBuilder.create(instance ->
    instance.group(
        Codecs.STRING.fieldOf("name").forGetter(Player::name),
        Codecs.INT.fieldOf("level").forGetter(Player::level)
    ).apply(instance, Player::new)
);

// Decode with GsonOps
DataResult<Player> result = playerCodec.decode(GsonOps.INSTANCE, jsonElement);
Player player = result.getOrThrow();

// Encode with JacksonJsonOps
DataResult<JsonNode> encoded = playerCodec.encode(player, JacksonJsonOps.INSTANCE, JacksonJsonOps.INSTANCE.empty());
JsonNode node = encoded.getOrThrow();
```

## Best Practices

1. **Use Singletons** - `GsonOps.INSTANCE` and `JacksonJsonOps.INSTANCE` are thread-safe singletons
2. **Wrap Early** - Convert to `Dynamic` as soon as you parse JSON
3. **Use Codecs** - For type-safe deserialization after migration
4. **Handle Nulls** - Both implementations handle JSON null gracefully

## Related

- [Codec Overview](index.md)
- [How to Integrate with Gson](../how-to/integrate-with-gson.md)
- [Dynamic System](../concepts/dynamic-system.md)
- [Codec System](../concepts/codec-system.md)
