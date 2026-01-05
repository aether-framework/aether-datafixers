# Testkit Overview

The **Aether Datafixers Testkit** module provides testing utilities designed to help developers test their own DataFixes, Schemas, and migration logic. It follows the framework's philosophy: **"Easy to start, powerful when needed."**

## Philosophy

Writing tests for data migrations shouldn't require extensive boilerplate. The testkit provides:

- **Fluent builders** for creating test data without manual JSON construction
- **Custom AssertJ assertions** for verifying Dynamic, DataResult, and Typed values
- **Test harnesses** for isolated DataFix and migration testing
- **Factory methods** for common fix patterns to reduce test setup code

## Module Contents

| Package | Description |
|---------|-------------|
| `testkit` | `TestData`, `TestDataBuilder`, `TestDataListBuilder` — Test data creation |
| `testkit.assertion` | `AetherAssertions`, `DynamicAssert`, `DataResultAssert`, `TypedAssert` |
| `testkit.harness` | `DataFixTester`, `MigrationTester`, `SchemaTester` |
| `testkit.factory` | `QuickFix`, `MockSchemas` |
| `testkit.context` | `RecordingContext`, `AssertingContext` |

## Installation

Add the testkit as a test dependency:

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

## Quick Start

### 1. Create Test Data

```java
import static de.splatgames.aether.datafixers.testkit.TestData.gson;

// Create test data fluently
Dynamic<JsonElement> player = gson().object()
    .put("name", "Alice")
    .put("level", 10)
    .putObject("position", pos -> pos
        .put("x", 100)
        .put("y", 64)
        .put("z", -200))
    .putList("inventory", list -> list
        .addObject(item -> item.put("id", "sword").put("count", 1))
        .addObject(item -> item.put("id", "apple").put("count", 64)))
    .build();
```

### 2. Assert Results

```java
import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;

// Fluent assertions
assertThat(result)
    .isMap()
    .hasField("name")
    .hasStringField("name", "Alice")
    .hasIntField("level", 10);

// Navigate and assert
assertThat(result)
    .field("position")
    .hasIntField("x", 100);
```

### 3. Test a DataFix

```java
import de.splatgames.aether.datafixers.testkit.harness.DataFixTester;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;

// Create a quick fix for testing
DataFix<JsonElement> fix = QuickFix.renameField(
    GsonOps.INSTANCE, "rename_name", 1, 2,
    "playerName", "name"
);

// Test it
DataFixTester.forFix(fix)
    .withInput(inputData)
    .forType("player")
    .expectOutput(expectedData)
    .verify();
```

### 4. Use QuickFix Factories

```java
// Common patterns without boilerplate
DataFix<JsonElement> addStatus = QuickFix.addStringField(
    GsonOps.INSTANCE, "add_status", 1, 2, "status", "active"
);

DataFix<JsonElement> removeOld = QuickFix.removeField(
    GsonOps.INSTANCE, "remove_old", 2, 3, "legacyField"
);

DataFix<JsonElement> transform = QuickFix.transformField(
    GsonOps.INSTANCE, "uppercase", 3, 4, "name",
    d -> d.createString(d.asString().result().orElse("").toUpperCase())
);
```

## Guides

- [Test Data Builders](test-data-builders.md) — Creating test data without boilerplate
- [Custom Assertions](assertions.md) — AssertJ assertions for Dynamic, DataResult, Typed
- [DataFixTester](datafix-tester.md) — Test harness for isolated DataFix testing
- [QuickFix Factories](quick-fix.md) — Factory methods for common fix patterns
- [Mock Schemas](mock-schemas.md) — Mock schema utilities for testing

## Example: Complete DataFix Test

```java
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;
import de.splatgames.aether.datafixers.testkit.harness.DataFixTester;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;

class PlayerFixTest {

    @Test
    void shouldRenamePlayerNameToName() {
        // Given: A fix that renames playerName to name
        DataFix<JsonElement> fix = QuickFix.renameField(
            GsonOps.INSTANCE, "rename_player_name", 1, 2,
            "playerName", "name"
        );

        // And: Input data with the old field name
        Dynamic<JsonElement> input = TestData.gson().object()
            .put("playerName", "Alice")
            .put("level", 10)
            .build();

        // When: Applying the fix
        Dynamic<JsonElement> result = DataFixTester.forFix(fix)
            .withInput(input)
            .forType("player")
            .apply();

        // Then: The field should be renamed
        assertThat(result)
            .hasStringField("name", "Alice")
            .hasIntField("level", 10)
            .doesNotHaveField("playerName");
    }

    @Test
    void shouldVerifyFixWithExpectedOutput() {
        DataFix<JsonElement> fix = QuickFix.addIntField(
            GsonOps.INSTANCE, "add_score", 1, 2, "score", 0
        );

        Dynamic<JsonElement> input = TestData.gson().object()
            .put("name", "Bob")
            .build();

        Dynamic<JsonElement> expected = TestData.gson().object()
            .put("name", "Bob")
            .put("score", 0)
            .build();

        // Verify with expected output - throws if mismatch
        DataFixTester.forFix(fix)
            .withInput(input)
            .forType("player")
            .expectOutput(expected)
            .verify();
    }

    @Test
    void shouldRecordContextDuringFixExecution() {
        DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
        Dynamic<JsonElement> input = TestData.gson().object().build();

        var verification = DataFixTester.forFix(fix)
            .withInput(input)
            .forType("player")
            .recordingContext()
            .verify();

        // Assert on recorded context
        verification.assertNoWarnings();
        verification.assertNoLogs();
        assertThat(verification.result()).isMap();
    }
}
```

## Related

- [How to Test Migrations](../how-to/test-migrations.md) — Task-oriented testing guide
- [DataFix System](../concepts/datafix-system.md) — Understanding DataFix
- [Dynamic System](../concepts/dynamic-system.md) — Understanding Dynamic
