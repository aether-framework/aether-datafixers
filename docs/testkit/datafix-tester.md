# DataFixTester

`DataFixTester` is a test harness for isolated testing of individual `DataFix` implementations. It simplifies the setup required to test a fix without bootstrapping the entire DataFixer system.

## Basic Usage

```java
import de.splatgames.aether.datafixers.testkit.harness.DataFixTester;

DataFixTester.forFix(myFix)
    .withInput(inputDynamic)
    .forType("player")
    .expectOutput(expectedDynamic)
    .verify();
```

## Creating a Tester

Start with `DataFixTester.forFix()`:

```java
DataFix<JsonElement> fix = new MyDataFix(inputSchema, outputSchema);

DataFixTester<JsonElement> tester = DataFixTester.forFix(fix);
```

## Setting Input

Provide the input data using `withInput()`:

```java
Dynamic<JsonElement> input = TestData.gson().object()
    .put("name", "Alice")
    .put("level", 10)
    .build();

DataFixTester.forFix(fix)
    .withInput(input)
    ...
```

## Setting Type Reference

Specify which type the fix should be applied to:

```java
// Using a string type id
DataFixTester.forFix(fix)
    .withInput(input)
    .forType("player")
    ...

// Using a TypeReference object
DataFixTester.forFix(fix)
    .withInput(input)
    .forType(new TypeReference("player"))
    ...
```

## Apply vs Verify

### apply()

Returns the result for further assertions:

```java
Dynamic<JsonElement> result = DataFixTester.forFix(fix)
    .withInput(input)
    .forType("player")
    .apply();

// Custom assertions on result
assertThat(result).hasStringField("name", "Alice");
assertThat(result).hasField("newField");
```

### verify()

Compares the result against expected output and throws if they don't match:

```java
DataFixTester.forFix(fix)
    .withInput(input)
    .forType("player")
    .expectOutput(expectedOutput)
    .verify();
```

## Recording Context

Use `recordingContext()` to capture context calls during fix execution:

```java
DataFixTester.DataFixVerification<JsonElement> verification = DataFixTester.forFix(fix)
    .withInput(input)
    .forType("player")
    .recordingContext()
    .verify();

// Check verification results
assertThat(verification.passed()).isTrue();
assertThat(verification.result()).hasField("name");
assertThat(verification.context()).isNotNull();

// Assert on recorded context
verification.assertNoWarnings();
verification.assertNoLogs();
```

### Verification Object

The `DataFixVerification` object provides:

```java
// Check if verification passed
boolean passed = verification.passed();

// Get the resulting Dynamic
Dynamic<JsonElement> result = verification.result();

// Get the recording context
RecordingContext context = verification.context();

// Convenience assertions
verification.assertNoWarnings();
verification.assertNoLogs();
```

## Fail on Warning

Use `failOnWarning()` to make the test fail if the fix logs any warnings:

```java
DataFixTester.forFix(fix)
    .withInput(input)
    .forType("player")
    .failOnWarning()
    .apply();
```

This is useful for catching unexpected warnings during migration.

## Error Handling

The tester validates the configuration before execution:

```java
// Throws IllegalStateException: "Input not set"
DataFixTester.forFix(fix)
    .forType("player")
    .apply();

// Throws IllegalStateException: "Type reference not set"
DataFixTester.forFix(fix)
    .withInput(input)
    .apply();
```

## Complete Examples

### Testing a Rename Field Fix

```java
@Test
void shouldRenamePlayerNameToName() {
    // Create the fix
    DataFix<JsonElement> fix = QuickFix.renameField(
        GsonOps.INSTANCE, "rename_player_name", 1, 2,
        "playerName", "name"
    );

    // Create input
    Dynamic<JsonElement> input = TestData.gson().object()
        .put("playerName", "Alice")
        .put("level", 10)
        .build();

    // Apply and verify
    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .apply();

    assertThat(result)
        .hasStringField("name", "Alice")
        .hasIntField("level", 10)
        .doesNotHaveField("playerName");
}
```

### Testing with Expected Output

```java
@Test
void shouldAddDefaultScore() {
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

    // Verify against expected - throws AssertionError if mismatch
    DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .expectOutput(expected)
        .verify();
}
```

### Testing with Context Recording

```java
@Test
void shouldNotProduceWarnings() {
    DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
    Dynamic<JsonElement> input = TestData.gson().object().build();

    var verification = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .recordingContext()
        .verify();

    verification.assertNoWarnings();
    verification.assertNoLogs();

    assertThat(verification.passed()).isTrue();
    assertThat(verification.result()).isMap();
}
```

### Testing Error Conditions

```java
@Test
void shouldThrowWhenInputNotSet() {
    DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);

    assertThatThrownBy(() -> DataFixTester.forFix(fix)
            .forType("player")
            .apply())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Input not set");
}

@Test
void shouldThrowWhenTypeNotSet() {
    DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
    Dynamic<JsonElement> input = TestData.gson().object().build();

    assertThatThrownBy(() -> DataFixTester.forFix(fix)
            .withInput(input)
            .apply())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Type reference not set");
}
```

## MigrationTester

For testing complete migration chains (multiple fixes), use `MigrationTester`:

```java
import de.splatgames.aether.datafixers.testkit.harness.MigrationTester;

MigrationTester.forFixer(fixer)
    .using(GsonOps.INSTANCE)
    .forType(PLAYER)
    .withInput(v1Data)
    .from(1).to(5)
    .expectOutput(v5Data)
    .verify();
```

## SchemaTester

For validating schema configurations, use `SchemaTester`:

```java
import de.splatgames.aether.datafixers.testkit.harness.SchemaTester;

SchemaTester.forSchema(schema110)
    .hasVersion(110)
    .containsType(PLAYER)
    .containsType(WORLD)
    .inheritsFrom(schema100)
    .verify();
```

## Related

- [Testkit Overview](index.md)
- [QuickFix Factories](quick-fix.md)
- [Test Data Builders](test-data-builders.md)
- [DataFix System](../concepts/datafix-system.md)
