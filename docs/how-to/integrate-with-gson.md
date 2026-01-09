# How to Integrate with Gson

This guide shows how to use Aether Datafixers with the Gson JSON library.

## Add Dependencies

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-codec</artifactId>
    <version>${aether.version}</version>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

## Basic Usage with GsonOps

```java
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;

// Create a JsonObject
JsonObject json = new JsonObject();
json.addProperty("name", "Steve");
json.addProperty("level", 10);

// Wrap in Dynamic
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);

// Read values
String name = dynamic.get("name").asString().orElse("Unknown");
int level = dynamic.get("level").asInt().orElse(1);
```

## Parse JSON String to Dynamic

```java
import com.google.gson.Gson;
import com.google.gson.JsonElement;

Gson gson = new Gson();

String jsonString = "{\"name\": \"Steve\", \"level\": 10}";
JsonElement element = gson.fromJson(jsonString, JsonElement.class);

Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, element);
```

## Convert Dynamic Back to JSON

```java
Dynamic<JsonElement> dynamic = // ... your dynamic

// Get the underlying JsonElement
JsonElement element = dynamic.value();

// Convert to string
Gson gson = new GsonBuilder().setPrettyPrinting().create();
String jsonString = gson.toJson(element);
```

## Full Migration Example

```java
public class JsonMigrator {

    private final AetherDataFixer fixer;
    private final Gson gson;

    public JsonMigrator(AetherDataFixer fixer) {
        this.fixer = fixer;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public String migrate(String jsonInput, int fromVersion, int toVersion) {
        // Parse input
        JsonElement element = gson.fromJson(jsonInput, JsonElement.class);
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, element);

        // Create tagged dynamic
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

        // Migrate
        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(fromVersion),
            new DataVersion(toVersion)
        );

        // Convert back to JSON string
        return gson.toJson(migrated.value().value());
    }
}
```

## Read JSON File

```java
public Dynamic<JsonElement> readJsonFile(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path)) {
        JsonElement element = JsonParser.parseReader(reader);
        return new Dynamic<>(GsonOps.INSTANCE, element);
    }
}
```

## Write JSON File

```java
public void writeJsonFile(Path path, Dynamic<JsonElement> dynamic) throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    try (Writer writer = Files.newBufferedWriter(path)) {
        gson.toJson(dynamic.value(), writer);
    }
}
```

## Create Values with GsonOps

```java
Dynamic<JsonElement> dynamic = // existing dynamic

// Create primitive values
Dynamic<JsonElement> stringVal = dynamic.createString("hello");
Dynamic<JsonElement> intVal = dynamic.createInt(42);
Dynamic<JsonElement> boolVal = dynamic.createBoolean(true);
Dynamic<JsonElement> doubleVal = dynamic.createDouble(3.14);

// Create empty structures
Dynamic<JsonElement> emptyMap = dynamic.emptyMap();
Dynamic<JsonElement> emptyList = dynamic.emptyList();
```

## Modify JSON Structure

```java
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonObject);

// Set a field
dynamic = dynamic.set("newField", dynamic.createString("value"));

// Remove a field
dynamic = dynamic.remove("oldField");

// Update a field
dynamic = dynamic.update("count", count ->
    count.createInt(count.asInt().orElse(0) + 1)
);
```

## Work with Nested Objects

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

## Work with Arrays

```java
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);

// Read list
List<String> names = dynamic.get("names")
    .asList()
    .orElse(List.of())
    .stream()
    .map(d -> d.asString().orElse(""))
    .toList();

// Create list
List<Dynamic<JsonElement>> items = List.of(
    dynamic.createString("item1"),
    dynamic.createString("item2"),
    dynamic.createString("item3")
);
Dynamic<JsonElement> list = dynamic.createList(items.stream());
```

## Use Codecs with Gson

```java
// Define a codec
Codec<Player> playerCodec = RecordCodecBuilder.create(instance ->
    instance.group(
        Codecs.STRING.fieldOf("name").forGetter(Player::name),
        Codecs.INT.fieldOf("level").forGetter(Player::level)
    ).apply(instance, Player::new)
);

// Decode from JsonElement
JsonElement json = gson.fromJson("{\"name\":\"Steve\",\"level\":10}", JsonElement.class);
DataResult<Player> result = playerCodec.decode(GsonOps.INSTANCE, json);
Player player = result.getOrThrow();

// Encode to JsonElement
DataResult<JsonElement> encoded = playerCodec.encode(player, GsonOps.INSTANCE, GsonOps.INSTANCE.empty());
JsonElement output = encoded.getOrThrow();
```

## Migration Service

Complete service for migrating JSON files:

```java
public class JsonMigrationService {

    private final AetherDataFixer fixer;
    private final Gson gson;
    private final DataVersion targetVersion;

    public JsonMigrationService(DataVersion targetVersion, DataFixerBootstrap bootstrap) {
        this.fixer = new DataFixerRuntimeFactory().create(targetVersion, bootstrap);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.targetVersion = targetVersion;
    }

    public void migrateFile(Path file, TypeReference type) throws IOException {
        // Read
        JsonObject json;
        try (Reader reader = Files.newBufferedReader(file)) {
            json = gson.fromJson(reader, JsonObject.class);
        }

        // Get version from file
        int version = json.has("_version")
            ? json.get("_version").getAsInt()
            : 1;

        // Skip if already current
        if (version >= targetVersion.version()) {
            return;
        }

        // Migrate
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);
        TaggedDynamic tagged = new TaggedDynamic(type, dynamic);

        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(version),
            targetVersion
        );

        // Update version field
        JsonElement result = (JsonElement) migrated.value().value();
        if (result.isJsonObject()) {
            result.getAsJsonObject().addProperty("_version", targetVersion.version());
        }

        // Write back
        try (Writer writer = Files.newBufferedWriter(file)) {
            gson.toJson(result, writer);
        }
    }
}
```

## Handle Null Values

```java
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);

// JsonNull is handled gracefully
if (dynamic.get("field").result().isEmpty()) {
    // Field is null or missing
}

// Read with fallback
String value = dynamic.get("nullableField")
    .asString()
    .result()
    .filter(s -> !s.isEmpty())
    .orElse("default");
```

## Best Practices

1. **Use GsonOps.INSTANCE** — It's a singleton, no need to create new instances

2. **Wrap early** — Convert to Dynamic as soon as you parse JSON

3. **Use codecs** for typed deserialization after migration

4. **Pretty print** for human-readable output:
   ```java
   new GsonBuilder().setPrettyPrinting().create()
   ```

5. **Handle JsonNull** — Check for null values when reading

## Related

- [Dynamic System](../concepts/dynamic-system.md)
- [Codec System](../concepts/codec-system.md)
- [Custom DynamicOps](../tutorials/custom-dynamicops.md)

