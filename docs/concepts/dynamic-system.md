# Dynamic System

The Dynamic system is the core abstraction that allows Aether Datafixers to work with any serialization format. It provides format-agnostic data manipulation through the `Dynamic<T>` wrapper and `DynamicOps<T>` interface.

## Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                       Dynamic<T>                                  │
│  ┌────────────────────────┐    ┌─────────────────────────────┐   │
│  │     DynamicOps<T>      │    │         T value             │   │
│  │   (format operations)  │    │   (underlying data)         │   │
│  └────────────────────────┘    └─────────────────────────────┘   │
│                                                                   │
│  Example: Dynamic<JsonElement>                                    │
│  ├── ops: GsonOps.INSTANCE                                        │
│  └── value: {"name": "Steve", "level": 10}                        │
└──────────────────────────────────────────────────────────────────┘
```

The key insight is that **all data manipulation happens through `Dynamic`**, which delegates to the appropriate `DynamicOps` for the underlying format. This means the same fix code works with JSON, NBT, YAML, or any other format.

## Core Components

### Dynamic<T>

The wrapper that combines data with its operations:

```java
public class Dynamic<T> {
    private final DynamicOps<T> ops;
    private final T value;

    public Dynamic(DynamicOps<T> ops, T value) {
        this.ops = ops;
        this.value = value;
    }

    // Accessors
    public DynamicOps<T> ops() { return ops; }
    public T value() { return value; }

    // ... manipulation methods
}
```

### DynamicOps<T>

The interface defining format-specific operations:

```java
public interface DynamicOps<T> {
    // Type creation
    T createString(String value);
    T createInt(int value);
    T createLong(long value);
    T createDouble(double value);
    T createBoolean(boolean value);
    T createList(Stream<T> values);
    T createMap(Map<T, T> map);

    // Type extraction
    DataResult<String> getStringValue(T input);
    DataResult<Number> getNumberValue(T input);
    DataResult<Boolean> getBooleanValue(T input);
    DataResult<Stream<T>> getStream(T input);
    DataResult<Map<T, T>> getMapValues(T input);

    // Structure operations
    T empty();
    T emptyMap();
    T emptyList();
    DataResult<T> mergeToMap(T map, T key, T value);
    DataResult<T> mergeToList(T list, T value);

    // ... more operations
}
```

### TaggedDynamic

Combines a `Dynamic` with its `TypeReference`:

```java
public record TaggedDynamic(TypeReference type, Dynamic<?> value) {
    // Associates data with its type for routing to correct fixes
}
```

## Creating Dynamic Values

### From Existing Data

```java
// From JSON (Gson)
JsonObject json = new JsonObject();
json.addProperty("name", "Steve");
json.addProperty("level", 10);

Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);

// From JSON (Jackson)
ObjectNode node = objectMapper.createObjectNode();
node.put("name", "Steve");
node.put("level", 10);

Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonJsonOps.INSTANCE, node);
```

### Creating New Values

```java
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, GsonOps.INSTANCE.empty());

// Create primitives
Dynamic<JsonElement> stringVal = dynamic.createString("hello");
Dynamic<JsonElement> intVal = dynamic.createInt(42);
Dynamic<JsonElement> doubleVal = dynamic.createDouble(3.14);
Dynamic<JsonElement> boolVal = dynamic.createBoolean(true);

// Create structures
Dynamic<JsonElement> map = dynamic.emptyMap()
    .set("key1", dynamic.createString("value1"))
    .set("key2", dynamic.createInt(100));

Dynamic<JsonElement> list = dynamic.emptyList()
    .append(dynamic.createString("a"))
    .append(dynamic.createString("b"))
    .append(dynamic.createString("c"));
```

## Reading Values

### Direct Access

```java
Dynamic<JsonElement> player = new Dynamic<>(GsonOps.INSTANCE, playerJson);

// Get nested dynamic
OptionalDynamic<JsonElement> name = player.get("name");
OptionalDynamic<JsonElement> position = player.get("position");

// Extract primitive values
DataResult<String> nameResult = name.asString();
DataResult<Integer> levelResult = player.get("level").asInt();
DataResult<Double> xResult = player.get("position").get("x").asDouble();

// With defaults
String nameValue = name.asString().orElse("Unknown");
int levelValue = player.get("level").asInt().orElse(1);
```

### Type Checking

```java
// Check if value exists
boolean hasName = player.get("name").result().isPresent();

// DataResult provides success/failure info
DataResult<String> result = player.get("name").asString();
if (result.result().isPresent()) {
    String value = result.result().get();
    // Use value
} else {
    // Handle missing or wrong type
    String error = result.error().map(e -> e.message()).orElse("Unknown error");
}
```

## Modifying Values

`Dynamic` is **immutable** — all operations return new instances:

### Setting Fields

```java
Dynamic<JsonElement> player = ...;

// Set a single field
Dynamic<JsonElement> updated = player.set("name", player.createString("Alex"));

// Set multiple fields
Dynamic<JsonElement> updated = player
    .set("name", player.createString("Alex"))
    .set("level", player.createInt(20))
    .set("active", player.createBoolean(true));
```

### Removing Fields

```java
// Remove a field
Dynamic<JsonElement> updated = player.remove("obsoleteField");

// Remove multiple fields
Dynamic<JsonElement> updated = player
    .remove("field1")
    .remove("field2")
    .remove("field3");
```

### Updating Nested Values

```java
// Update nested field
Dynamic<JsonElement> player = ...;

// Get position, modify, set back
Dynamic<JsonElement> position = player.get("position").orElseEmptyMap();
Dynamic<JsonElement> newPosition = position.set("x", position.createDouble(100.0));
Dynamic<JsonElement> updated = player.set("position", newPosition);
```

### Transforming Values

```java
// Transform a field's value
Dynamic<JsonElement> updated = player.update("level", level ->
    level.createInt(level.asInt().orElse(0) + 10)
);

// Transform entire structure
Dynamic<JsonElement> transformed = player.updateMapValues((key, value) -> {
    if (key.asString().orElse("").equals("score")) {
        return value.createInt(value.asInt().orElse(0) * 2);
    }
    return value;
});
```

## Working with Lists

```java
// Create a list
Dynamic<JsonElement> inventory = dynamic.emptyList();
inventory = inventory.append(itemDynamic1);
inventory = inventory.append(itemDynamic2);

// Get list as stream
Stream<Dynamic<JsonElement>> items = player.get("inventory")
    .asStream()
    .orElse(Stream.empty());

// Process list items
List<String> itemNames = items
    .map(item -> item.get("name").asString().orElse(""))
    .toList();

// Transform list items
Dynamic<JsonElement> updatedInventory = player.update("inventory", inv ->
    inv.updateList(item -> {
        int count = item.get("count").asInt().orElse(1);
        return item.set("count", item.createInt(count * 2));
    })
);
```

## Working with Maps

```java
// Create a map
Dynamic<JsonElement> stats = dynamic.emptyMap();
stats = stats.set("health", dynamic.createInt(100));
stats = stats.set("mana", dynamic.createInt(50));

// Get map entries
Map<Dynamic<JsonElement>, Dynamic<JsonElement>> entries = player.get("stats")
    .asMap()
    .orElse(Map.of());

// Transform map values
Dynamic<JsonElement> updated = player.update("stats", stats ->
    stats.updateMapValues((key, value) ->
        value.createInt(value.asInt().orElse(0) + 10)
    )
);
```

## OptionalDynamic

When accessing nested fields, you get an `OptionalDynamic`:

```java
OptionalDynamic<JsonElement> maybeName = player.get("name");

// Check if present
boolean exists = maybeName.result().isPresent();

// Get as DataResult
DataResult<Dynamic<JsonElement>> result = maybeName.result();

// Get with default
Dynamic<JsonElement> nameOrDefault = maybeName.orElseEmptyMap();
String nameValue = maybeName.asString().orElse("default");
```

## TaggedDynamic Usage

```java
// Create tagged dynamic
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, playerJson);
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

// Use with DataFixer
TaggedDynamic migrated = fixer.update(
    tagged,
    new DataVersion(100),
    new DataVersion(200)
);

// Extract type and value
TypeReference type = migrated.type();     // TypeReferences.PLAYER
Dynamic<?> value = migrated.value();       // Updated dynamic
```

## DynamicOps Implementations

### GsonOps

For working with Gson's `JsonElement`:

```java
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;

Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);

// Create values
JsonElement stringJson = GsonOps.INSTANCE.createString("hello");
JsonElement intJson = GsonOps.INSTANCE.createInt(42);
```

### JacksonJsonOps

For working with Jackson's `JsonNode`:

```java
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;

Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonJsonOps.INSTANCE, jsonNode);
```

### Custom DynamicOps

You can implement `DynamicOps` for other formats:

```java
public class YamlOps implements DynamicOps<Object> {
    public static final YamlOps INSTANCE = new YamlOps();

    @Override
    public Object createString(String value) {
        return value;
    }

    @Override
    public DataResult<String> getStringValue(Object input) {
        if (input instanceof String s) {
            return DataResult.success(s);
        }
        return DataResult.error("Not a string");
    }

    // ... implement all methods
}
```

## Common Patterns

### Safe Field Extraction

```java
public String extractName(Dynamic<?> data) {
    return data.get("name")
        .asString()
        .result()
        .orElse("Unknown");
}

public int extractLevel(Dynamic<?> data) {
    return data.get("level")
        .asInt()
        .result()
        .orElse(1);
}
```

### Restructuring Data

```java
public Dynamic<?> flattenPosition(Dynamic<?> player) {
    // Extract nested position
    double x = player.get("position").get("x").asDouble().orElse(0.0);
    double y = player.get("position").get("y").asDouble().orElse(0.0);
    double z = player.get("position").get("z").asDouble().orElse(0.0);

    // Flatten to root level
    return player
        .remove("position")
        .set("x", player.createDouble(x))
        .set("y", player.createDouble(y))
        .set("z", player.createDouble(z));
}

public Dynamic<?> nestPosition(Dynamic<?> player) {
    // Extract flat coordinates
    double x = player.get("x").asDouble().orElse(0.0);
    double y = player.get("y").asDouble().orElse(0.0);
    double z = player.get("z").asDouble().orElse(0.0);

    // Create nested position
    Dynamic<?> position = player.emptyMap()
        .set("x", player.createDouble(x))
        .set("y", player.createDouble(y))
        .set("z", player.createDouble(z));

    return player
        .remove("x").remove("y").remove("z")
        .set("position", position);
}
```

### Type Conversion

```java
public Dynamic<?> convertGameMode(Dynamic<?> player) {
    int mode = player.get("gameMode").asInt().orElse(0);

    String modeName = switch (mode) {
        case 0 -> "survival";
        case 1 -> "creative";
        case 2 -> "adventure";
        case 3 -> "spectator";
        default -> "survival";
    };

    return player.set("gameMode", player.createString(modeName));
}
```

## Best Practices

### 1. Always Use Defaults

```java
// Good: Handle missing data
String name = data.get("name").asString().orElse("Unknown");
int level = data.get("level").asInt().orElse(1);

// Avoid: Throwing on missing data
String name = data.get("name").asString().result().get(); // May throw!
```

### 2. Chain Immutable Operations

```java
// Good: Fluent chaining
Dynamic<?> result = data
    .remove("old1")
    .remove("old2")
    .set("new1", value1)
    .set("new2", value2);

// Avoid: Reassignment
Dynamic<?> result = data;
result = result.remove("old1");
result = result.remove("old2");
// etc.
```

### 3. Preserve Unknown Fields

When transforming data, preserve fields you don't recognize:

```java
// The DSL.remainder() captures unknown fields
registerType(TYPE, DSL.and(
    DSL.field("known", DSL.string()),
    DSL.remainder()  // Preserve everything else
));
```

### 4. Use TaggedDynamic for Routing

```java
// Good: Tagged data routes to correct fixes
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);
fixer.update(tagged, fromVersion, toVersion);

// The fixer knows which fixes apply to PLAYER type
```

---

## Summary

| Component | Purpose |
|-----------|---------|
| `Dynamic<T>` | Format-agnostic data wrapper |
| `DynamicOps<T>` | Format-specific operations |
| `OptionalDynamic` | Safe nested access |
| `TaggedDynamic` | Associates data with type |
| `GsonOps` | Gson JsonElement operations |
| `JacksonJsonOps` | Jackson JsonNode operations |

---

## Related

- [Codec System](codec-system.md) — Encoding/decoding typed data
- [DataFix System](datafix-system.md) — Using Dynamic in fixes
- [DSL](dsl.md) — Type templates with remainder

