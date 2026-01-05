# How to Test Migrations

This guide shows how to write unit tests for your data migrations using the **Aether Datafixers Testkit** module.

## Setup

Add the testkit dependency to your project:

**Maven:**
```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-testkit</artifactId>
    <scope>test</scope>
</dependency>
```

**Gradle:**
```groovy
testImplementation 'de.splatgames.aether.datafixers:aether-datafixers-testkit'
```

## Basic Test with Testkit

```java
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;
import de.splatgames.aether.datafixers.testkit.harness.DataFixTester;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;

class PlayerMigrationTest {

    @Test
    void testFieldRename() {
        // Create a fix
        var fix = QuickFix.renameField(
            GsonOps.INSTANCE, "rename_player_name", 1, 2,
            "playerName", "name"
        );

        // Create input data
        Dynamic<JsonElement> input = TestData.gson().object()
            .put("playerName", "Steve")
            .put("level", 10)
            .build();

        // Apply and verify
        Dynamic<JsonElement> result = DataFixTester.forFix(fix)
            .withInput(input)
            .forType("player")
            .apply();

        assertThat(result)
            .hasStringField("name", "Steve")
            .hasIntField("level", 10)
            .doesNotHaveField("playerName");
    }
}
```

## Test with Expected Output

```java
@Test
void testWithExpectedOutput() {
    var fix = QuickFix.addIntField(
        GsonOps.INSTANCE, "add_score", 1, 2, "score", 0
    );

    Dynamic<JsonElement> input = TestData.gson().object()
        .put("name", "Steve")
        .build();

    Dynamic<JsonElement> expected = TestData.gson().object()
        .put("name", "Steve")
        .put("score", 0)
        .build();

    // Throws AssertionError if output doesn't match
    DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .expectOutput(expected)
        .verify();
}
```

## Test Type Conversions

```java
@Test
void testGameModeConversion() {
    var fix = QuickFix.transformField(
        GsonOps.INSTANCE, "convert_gamemode", 1, 2, "gameMode",
        d -> {
            int mode = d.asInt().result().orElse(0);
            String name = switch (mode) {
                case 0 -> "survival";
                case 1 -> "creative";
                case 2 -> "adventure";
                case 3 -> "spectator";
                default -> "survival";
            };
            return d.createString(name);
        }
    );

    assertGameModeConversion(fix, 0, "survival");
    assertGameModeConversion(fix, 1, "creative");
    assertGameModeConversion(fix, 2, "adventure");
    assertGameModeConversion(fix, 3, "spectator");
}

private void assertGameModeConversion(DataFix<JsonElement> fix, int input, String expected) {
    Dynamic<JsonElement> data = TestData.gson().object()
        .put("gameMode", input)
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(data)
        .forType("player")
        .apply();

    assertThat(result).hasStringField("gameMode", expected);
}
```

## Test Default Values

```java
@Test
void testMissingFieldGetsDefault() {
    var fix = QuickFix.addIntField(
        GsonOps.INSTANCE, "add_level", 1, 2, "level", 1
    );

    Dynamic<JsonElement> input = TestData.gson().object()
        .put("name", "Steve")
        // level is missing
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .apply();

    assertThat(result)
        .hasIntField("level", 1);  // Default applied
}
```

## Test Structure Changes

```java
@Test
void testPositionNesting() {
    var fix = QuickFix.simple("nest_position", 1, 2, input -> {
        var x = input.get("x").asDouble().result().orElse(0.0);
        var y = input.get("y").asDouble().result().orElse(0.0);
        var z = input.get("z").asDouble().result().orElse(0.0);

        return input
            .remove("x").remove("y").remove("z")
            .set("position", input.emptyMap()
                .set("x", input.createDouble(x))
                .set("y", input.createDouble(y))
                .set("z", input.createDouble(z)));
    });

    Dynamic<JsonElement> input = TestData.gson().object()
        .put("name", "Steve")
        .put("x", 100.5)
        .put("y", 64.0)
        .put("z", -200.0)
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .apply();

    assertThat(result)
        .doesNotHaveField("x")
        .doesNotHaveField("y")
        .doesNotHaveField("z")
        .field("position")
            .hasDoubleField("x", 100.5)
            .hasDoubleField("y", 64.0)
            .hasDoubleField("z", -200.0);
}
```

## Test Unknown Fields Preserved

```java
@Test
void testUnknownFieldsPreserved() {
    var fix = QuickFix.renameField(
        GsonOps.INSTANCE, "rename", 1, 2, "oldName", "newName"
    );

    Dynamic<JsonElement> input = TestData.gson().object()
        .put("oldName", "value")
        .put("customPlugin_data", "important")
        .putObject("modSettings", obj -> obj.put("enabled", true))
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("config")
        .apply();

    assertThat(result)
        .hasStringField("newName", "value")
        .hasStringField("customPlugin_data", "important")
        .hasField("modSettings");
}
```

## Test List Transformations

```java
@Test
void testListItemMigration() {
    var fix = QuickFix.simple("migrate_inventory", 1, 2, input -> {
        var inventory = input.get("inventory");
        var migratedItems = inventory.asListStream().result()
            .orElse(java.util.stream.Stream.empty())
            .map(item -> item
                .set("itemId", item.get("id"))
                .remove("id")
                .set("amount", item.get("count"))
                .remove("count"))
            .toList();

        return input.set("inventory",
            input.createList(migratedItems.stream().map(Dynamic::value)));
    });

    Dynamic<JsonElement> input = TestData.gson().object()
        .putList("inventory", list -> list
            .addObject(item -> item.put("id", "sword").put("count", 1))
            .addObject(item -> item.put("id", "apple").put("count", 64)))
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .apply();

    assertThat(result)
        .field("inventory")
        .hasSize(2);
}
```

## Test with Recording Context

```java
@Test
void testNoWarningsDuringMigration() {
    var fix = QuickFix.identity("noop", 1, 2);
    Dynamic<JsonElement> input = TestData.gson().object().build();

    var verification = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .recordingContext()
        .verify();

    verification.assertNoWarnings();
    verification.assertNoLogs();

    assertThat(verification.passed()).isTrue();
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
    var fix = createGameModeConversionFix();

    Dynamic<JsonElement> data = TestData.gson().object()
        .put("gameMode", input)
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(data)
        .forType("player")
        .apply();

    assertThat(result).hasStringField("gameMode", expected);
}
```

## Test Edge Cases

```java
@Test
void testEmptyData() {
    var fix = QuickFix.addStringField(
        GsonOps.INSTANCE, "add_name", 1, 2, "name", "Unknown"
    );

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(TestData.gson().object().build())
        .forType("player")
        .apply();

    assertThat(result).hasStringField("name", "Unknown");
}

@Test
void testNullSafety() {
    var fix = QuickFix.simple("safe_transform", 1, 2, input -> {
        var name = input.get("name").asString().result().orElse("default");
        return input.set("name", input.createString(name));
    });

    Dynamic<JsonElement> input = TestData.gson().object()
        // name field is missing
        .put("level", 10)
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .apply();

    assertThat(result).hasStringField("name", "default");
}
```

## Full Migration Chain Test

For testing complete migration chains, use the full DataFixer:

```java
class FullMigrationTest {

    private static AetherDataFixer fixer;

    @BeforeAll
    static void setup() {
        fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
    }

    @Test
    void testV1ToV3Chain() {
        Dynamic<JsonElement> v1 = TestData.gson().object()
            .put("playerName", "Steve")
            .put("xp", 1500)
            .build();

        TaggedDynamic tagged = new TaggedDynamic(PLAYER, v1);
        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(1),
            new DataVersion(3)
        );

        Dynamic<JsonElement> result = (Dynamic<JsonElement>) migrated.value();

        assertThat(result)
            .hasStringField("name", "Steve")         // V2 rename
            .hasIntField("experience", 1500)         // V2 rename
            .hasIntField("level", 15);               // V3 computed
    }
}
```

## Related

- [Testkit Overview](../testkit/index.md) — Complete testkit documentation
- [Test Data Builders](../testkit/test-data-builders.md) — Creating test data
- [Custom Assertions](../testkit/assertions.md) — Available assertions
- [QuickFix Factories](../testkit/quick-fix.md) — Factory methods
- [Debug Migrations](debug-migrations.md)
- [DataFix System](../concepts/datafix-system.md)
