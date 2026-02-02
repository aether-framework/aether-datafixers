# Test Data Builders

The test data builders provide a fluent API for creating `Dynamic<T>` test data without manual JSON construction. This eliminates boilerplate and makes tests more readable.

## Entry Point: TestData

The `TestData` class is the entry point for all test data creation:

```java
import de.splatgames.aether.datafixers.testkit.TestData;

// JSON - Gson (JsonElement)
TestData.gson()...

// JSON - Jackson (JsonNode)
TestData.jacksonJson()...

// YAML - SnakeYAML (native Java types: Object)
TestData.snakeYaml()...

// YAML - Jackson (JsonNode)
TestData.jacksonYaml()...

// TOML - Jackson (JsonNode)
TestData.jacksonToml()...

// XML - Jackson (JsonNode)
TestData.jacksonXml()...

// Custom DynamicOps
TestData.using(myCustomOps)...
```

## Creating Primitives

```java
// String
Dynamic<JsonElement> str = TestData.gson().string("hello");

// Integer
Dynamic<JsonElement> num = TestData.gson().integer(42);

// Long
Dynamic<JsonElement> lng = TestData.gson().longValue(9999999999L);

// Double
Dynamic<JsonElement> dbl = TestData.gson().doubleValue(3.14);

// Boolean
Dynamic<JsonElement> bool = TestData.gson().bool(true);

// Empty map
Dynamic<JsonElement> empty = TestData.gson().emptyMap();

// Empty list
Dynamic<JsonElement> emptyList = TestData.gson().emptyList();
```

## Building Objects

Use `object()` to start building a map/object structure:

```java
Dynamic<JsonElement> player = TestData.gson().object()
    .put("name", "Alice")
    .put("level", 10)
    .put("active", true)
    .put("score", 1500.5)
    .build();
```

### Nested Objects

Use `putObject()` for nested structures:

```java
Dynamic<JsonElement> player = TestData.gson().object()
    .put("name", "Alice")
    .putObject("position", pos -> pos
        .put("x", 100)
        .put("y", 64)
        .put("z", -200))
    .putObject("stats", stats -> stats
        .put("health", 100)
        .put("mana", 50))
    .build();
```

### Deeply Nested Objects

Chain `putObject()` calls for deeply nested structures:

```java
Dynamic<JsonElement> config = TestData.gson().object()
    .putObject("database", db -> db
        .put("host", "localhost")
        .put("port", 5432)
        .putObject("pool", pool -> pool
            .put("minSize", 5)
            .put("maxSize", 20)
            .putObject("timeout", timeout -> timeout
                .put("connect", 5000)
                .put("read", 30000))))
    .build();
```

### Adding Pre-Built Dynamics

Use `put(key, dynamic)` to add an existing `Dynamic`:

```java
Dynamic<JsonElement> position = TestData.gson().object()
    .put("x", 100)
    .put("y", 64)
    .build();

Dynamic<JsonElement> player = TestData.gson().object()
    .put("name", "Alice")
    .put("position", position)  // Inline existing Dynamic
    .build();
```

## Building Lists

### Lists in Objects

Use `putList()` to add list fields:

```java
Dynamic<JsonElement> player = TestData.gson().object()
    .put("name", "Alice")
    .putList("tags", list -> list
        .add("admin")
        .add("vip")
        .add("beta-tester"))
    .putList("scores", list -> list
        .add(100)
        .add(95)
        .add(87))
    .build();
```

### Lists with Objects

Use `addObject()` to add object elements:

```java
Dynamic<JsonElement> player = TestData.gson().object()
    .put("name", "Alice")
    .putList("inventory", list -> list
        .addObject(item -> item
            .put("id", "sword")
            .put("count", 1)
            .put("enchanted", true))
        .addObject(item -> item
            .put("id", "apple")
            .put("count", 64)
            .put("enchanted", false)))
    .build();
```

### Standalone Lists

Use `list()` to create a standalone list:

```java
Dynamic<JsonElement> numbers = TestData.gson().list()
    .add(1)
    .add(2)
    .add(3)
    .build();

Dynamic<JsonElement> items = TestData.gson().list()
    .addObject(item -> item.put("id", "sword"))
    .addObject(item -> item.put("id", "shield"))
    .build();
```

### Bulk Add

Use `addAll()` for multiple values:

```java
Dynamic<JsonElement> data = TestData.gson().object()
    .putList("numbers", list -> list.addAll(1, 2, 3, 4, 5))
    .putList("names", list -> list.addAll("Alice", "Bob", "Carol"))
    .putList("flags", list -> list.addAll(true, false, true))
    .build();
```

### Nested Lists

Use `addList()` for nested lists:

```java
Dynamic<JsonElement> matrix = TestData.gson().list()
    .addList(row -> row.addAll(1, 2, 3))
    .addList(row -> row.addAll(4, 5, 6))
    .addList(row -> row.addAll(7, 8, 9))
    .build();
```

## Working with Different Formats

All builders work identically across formats:

### Jackson JSON

```java
import com.fasterxml.jackson.databind.JsonNode;

Dynamic<JsonNode> player = TestData.jacksonJson().object()
    .put("name", "Alice")
    .put("level", 10)
    .putObject("position", pos -> pos
        .put("x", 100)
        .put("y", 64))
    .build();
```

### SnakeYAML

```java
Dynamic<Object> config = TestData.snakeYaml().object()
    .put("database", "localhost")
    .put("port", 5432)
    .putObject("options", opts -> opts
        .put("timeout", 30)
        .put("retries", 3))
    .build();
```

### Jackson YAML / TOML / XML

```java
// All Jackson-based formats use JsonNode
Dynamic<JsonNode> yamlData = TestData.jacksonYaml().object()
    .put("key", "value")
    .build();

Dynamic<JsonNode> tomlData = TestData.jacksonToml().object()
    .put("key", "value")
    .build();

Dynamic<JsonNode> xmlData = TestData.jacksonXml().object()
    .put("key", "value")
    .build();
```

## Custom DynamicOps

Use `using()` with any DynamicOps implementation:

```java
Dynamic<MyFormat> data = TestData.using(myCustomOps).object()
    .put("key", "value")
    .build();
```

## Complete Example

```java
@Test
void testPlayerMigration() {
    // Create comprehensive test data
    Dynamic<JsonElement> oldPlayer = TestData.gson().object()
        .put("playerName", "Steve")
        .put("xp", 15000)
        .put("x", 100.5)
        .put("y", 64.0)
        .put("z", -200.0)
        .put("gameMode", 1)
        .putList("inventory", list -> list
            .addObject(item -> item
                .put("itemId", "minecraft:diamond_sword")
                .put("count", 1)
                .put("damage", 100))
            .addObject(item -> item
                .put("itemId", "minecraft:golden_apple")
                .put("count", 64)
                .put("damage", 0)))
        .putObject("abilities", abilities -> abilities
            .put("canFly", true)
            .put("instaBuild", true)
            .put("invulnerable", false))
        .build();

    // Expected result after migration
    Dynamic<JsonElement> expectedPlayer = TestData.gson().object()
        .put("name", "Steve")
        .put("experience", 15000)
        .put("level", 15)
        .putObject("position", pos -> pos
            .put("x", 100.5)
            .put("y", 64.0)
            .put("z", -200.0))
        .put("gameMode", "creative")
        .putList("inventory", list -> list
            .addObject(item -> item
                .put("id", "minecraft:diamond_sword")
                .put("amount", 1)
                .put("durability", 100))
            .addObject(item -> item
                .put("id", "minecraft:golden_apple")
                .put("amount", 64)
                .put("durability", 0)))
        .putObject("abilities", abilities -> abilities
            .put("flight", true)
            .put("creativeBuild", true)
            .put("godMode", false))
        .build();

    // Run migration and verify
    Dynamic<JsonElement> result = migrate(oldPlayer, 1, 5);

    assertThat(result).isEqualTo(expectedPlayer);
}
```

## Best Practices

1. **Use descriptive variable names** — `oldPlayer`, `expectedResult` rather than `data1`, `data2`
2. **Group related fields** — Use `putObject()` for logical groupings
3. **Reuse common structures** — Extract common test data to helper methods
4. **Match production structure** — Keep test data structure close to real data

## Related

- [Testkit Overview](index.md)
- [Custom Assertions](assertions.md)
- [Dynamic System](../concepts/dynamic-system.md)
