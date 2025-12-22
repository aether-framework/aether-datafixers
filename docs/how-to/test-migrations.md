# How to Test Migrations

This guide shows how to write unit tests for your data migrations.

## Basic Test Structure

```java
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class PlayerMigrationTest {

    private static AetherDataFixer fixer;

    @BeforeAll
    static void setup() {
        fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
    }

    @Test
    void testV1ToV2Migration() {
        // Given: V1 data
        JsonObject v1 = new JsonObject();
        v1.addProperty("playerName", "Steve");
        v1.addProperty("xp", 1500);

        // When: Migrate
        Dynamic<JsonElement> result = migrate(v1, 1, 2);

        // Then: V2 structure
        assertEquals("Steve", result.get("name").asString().orElse(""));
        assertEquals(1500, result.get("experience").asInt().orElse(0));
        assertTrue(result.get("playerName").result().isEmpty());
    }

    private Dynamic<JsonElement> migrate(JsonObject data, int from, int to) {
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, data);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(from),
            new DataVersion(to)
        );

        return (Dynamic<JsonElement>) migrated.value();
    }
}
```

## Test Rename Fields

```java
@Test
void testFieldRename() {
    JsonObject input = new JsonObject();
    input.addProperty("oldName", "value");

    Dynamic<JsonElement> result = migrate(input, 1, 2);

    assertTrue(result.get("oldName").result().isEmpty(), "Old field should be gone");
    assertEquals("value", result.get("newName").asString().orElse(""), "New field should exist");
}
```

## Test Type Conversion

```java
@Test
void testGameModeConversion() {
    // Test each case
    assertGameMode(0, "survival");
    assertGameMode(1, "creative");
    assertGameMode(2, "adventure");
    assertGameMode(3, "spectator");
}

private void assertGameMode(int input, String expected) {
    JsonObject data = new JsonObject();
    data.addProperty("gameMode", input);

    Dynamic<JsonElement> result = migrate(data, 1, 2);

    assertEquals(expected, result.get("gameMode").asString().orElse(""));
}
```

## Test Default Values

```java
@Test
void testMissingFieldGetsDefault() {
    JsonObject input = new JsonObject();
    input.addProperty("name", "Steve");
    // level is missing

    Dynamic<JsonElement> result = migrate(input, 1, 2);

    assertEquals(1, result.get("level").asInt().orElse(-1), "Default level should be 1");
}
```

## Test Structure Changes

```java
@Test
void testPositionNesting() {
    JsonObject input = new JsonObject();
    input.addProperty("x", 100.5);
    input.addProperty("y", 64.0);
    input.addProperty("z", -200.0);

    Dynamic<JsonElement> result = migrate(input, 1, 2);

    // Old fields gone
    assertTrue(result.get("x").result().isEmpty());
    assertTrue(result.get("y").result().isEmpty());
    assertTrue(result.get("z").result().isEmpty());

    // New nested structure
    Dynamic<?> position = result.get("position").orElseEmptyMap();
    assertEquals(100.5, position.get("x").asDouble().orElse(0.0), 0.001);
    assertEquals(64.0, position.get("y").asDouble().orElse(0.0), 0.001);
    assertEquals(-200.0, position.get("z").asDouble().orElse(0.0), 0.001);
}
```

## Test Unknown Fields Preserved

```java
@Test
void testUnknownFieldsPreserved() {
    JsonObject input = new JsonObject();
    input.addProperty("name", "Steve");
    input.addProperty("customPlugin_data", "important");
    input.add("modSettings", new JsonObject());

    Dynamic<JsonElement> result = migrate(input, 1, 2);

    assertEquals("important", result.get("customPlugin_data").asString().orElse(""));
    assertTrue(result.get("modSettings").result().isPresent());
}
```

## Test Multi-Version Chain

```java
@Test
void testV1ToV3Chain() {
    JsonObject v1 = new JsonObject();
    v1.addProperty("playerName", "Steve");
    v1.addProperty("xp", 1500);

    Dynamic<JsonElement> result = migrate(v1, 1, 3);

    // V2 changes applied
    assertEquals("Steve", result.get("name").asString().orElse(""));
    assertEquals(1500, result.get("experience").asInt().orElse(0));

    // V3 changes applied
    assertEquals(15, result.get("level").asInt().orElse(0));
}
```

## Test No-Op for Current Version

```java
@Test
void testNoMigrationNeeded() {
    JsonObject current = new JsonObject();
    current.addProperty("name", "Steve");
    current.addProperty("level", 10);

    Dynamic<JsonElement> result = migrate(current, 3, 3);

    assertEquals("Steve", result.get("name").asString().orElse(""));
    assertEquals(10, result.get("level").asInt().orElse(0));
}
```

## Test Edge Cases

```java
@Test
void testEmptyData() {
    JsonObject empty = new JsonObject();

    Dynamic<JsonElement> result = migrate(empty, 1, 2);

    // Should not throw, should use defaults
    assertEquals("Unknown", result.get("name").asString().orElse("Unknown"));
}

@Test
void testNullValues() {
    JsonObject withNull = new JsonObject();
    withNull.add("name", JsonNull.INSTANCE);

    Dynamic<JsonElement> result = migrate(withNull, 1, 2);

    // Should handle gracefully
    assertNotNull(result.value());
}

@Test
void testWrongType() {
    JsonObject wrongType = new JsonObject();
    wrongType.addProperty("level", "not a number");

    Dynamic<JsonElement> result = migrate(wrongType, 1, 2);

    // Should use default
    assertEquals(1, result.get("level").asInt().orElse(1));
}
```

## Test List Transformations

```java
@Test
void testListItemMigration() {
    JsonObject input = new JsonObject();
    JsonArray items = new JsonArray();
    JsonObject item1 = new JsonObject();
    item1.addProperty("id", "sword");
    item1.addProperty("damage", 10);
    items.add(item1);
    input.add("inventory", items);

    Dynamic<JsonElement> result = migrate(input, 1, 2);

    List<Dynamic<?>> migratedItems = result.get("inventory").asList().orElse(List.of());
    assertEquals(1, migratedItems.size());
    assertEquals("sword", migratedItems.get(0).get("itemId").asString().orElse(""));
}
```

## Parameterized Tests

```java
@ParameterizedTest
@CsvSource({
    "0, survival",
    "1, creative",
    "2, adventure",
    "3, spectator",
    "99, survival"  // Unknown defaults to survival
})
void testGameModeConversions(int input, String expected) {
    JsonObject data = new JsonObject();
    data.addProperty("gameMode", input);

    Dynamic<JsonElement> result = migrate(data, 1, 2);

    assertEquals(expected, result.get("gameMode").asString().orElse(""));
}
```

## Test Helper Methods

```java
private void assertFieldExists(Dynamic<?> data, String field) {
    assertTrue(data.get(field).result().isPresent(),
        "Field '" + field + "' should exist");
}

private void assertFieldMissing(Dynamic<?> data, String field) {
    assertTrue(data.get(field).result().isEmpty(),
        "Field '" + field + "' should not exist");
}

private void assertStringField(Dynamic<?> data, String field, String expected) {
    assertEquals(expected, data.get(field).asString().orElse(null),
        "Field '" + field + "' should be '" + expected + "'");
}

private void assertIntField(Dynamic<?> data, String field, int expected) {
    assertEquals(expected, data.get(field).asInt().orElse(null),
        "Field '" + field + "' should be " + expected);
}
```

## Related

- [Debug Migrations](debug-migrations.md)
- [Log Migrations](log-migrations.md)
- [DataFix System](../concepts/datafix-system.md)

