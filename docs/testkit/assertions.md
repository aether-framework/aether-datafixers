# Custom Assertions

The testkit provides custom AssertJ assertions for `Dynamic<T>`, `DataResult<A>`, and `Typed<A>`. These make test assertions more expressive and provide better error messages.

## Entry Point: AetherAssertions

Import the static `assertThat` method from `AetherAssertions`:

```java
import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;

// Works with Dynamic, DataResult, and Typed
assertThat(dynamic).hasField("name");
assertThat(dataResult).isSuccess();
assertThat(typed).hasType(expectedType);
```

## DynamicAssert

Assertions for `Dynamic<T>` objects. These are the most commonly used assertions when testing migrations.

### Type Assertions

```java
// Check if map/object
assertThat(dynamic).isMap();

// Check if list/array
assertThat(dynamic).isList();

// Check if string
assertThat(dynamic).isString();

// Check if number
assertThat(dynamic).isNumber();

// Check if boolean
assertThat(dynamic).isBoolean();

// Check if null/empty
assertThat(dynamic).isNull();
assertThat(dynamic).isEmpty();
assertThat(dynamic).isNotEmpty();
```

### Field Assertions

```java
// Check field existence
assertThat(dynamic).hasField("name");
assertThat(dynamic).hasFields("name", "level", "active");
assertThat(dynamic).doesNotHaveField("deleted");

// Check typed field values
assertThat(dynamic).hasStringField("name", "Alice");
assertThat(dynamic).hasIntField("level", 10);
assertThat(dynamic).hasLongField("id", 12345678901L);
assertThat(dynamic).hasDoubleField("score", 95.5);
assertThat(dynamic).hasBooleanField("active", true);

// Check field count
assertThat(dynamic).hasFieldCount(3);
```

### Navigation

Navigate to nested fields for further assertions:

```java
// Navigate to field and continue asserting
assertThat(dynamic)
    .field("position")
    .hasIntField("x", 100)
    .hasIntField("y", 64);

// Chain multiple navigations
assertThat(dynamic)
    .field("player")
    .field("stats")
    .hasIntField("health", 100);
```

### List Assertions

```java
// Check list size
assertThat(dynamic).hasSize(3);

// Check element at index
assertThat(dynamic)
    .element(0)
    .hasStringField("id", "sword");

// Check all elements satisfy condition
assertThat(dynamic).allElementsSatisfy(element ->
    assertThat(element).hasField("id")
);
```

### Value Assertions

```java
// Direct value checks
assertThat(dynamic).hasStringValue("hello");
assertThat(dynamic).hasIntValue(42);
assertThat(dynamic).hasDoubleValue(3.14);
assertThat(dynamic).hasBooleanValue(true);

// Compare with another Dynamic
assertThat(result).isEqualTo(expected);
assertThat(result).isNotEqualTo(other);
```

### Chained Assertions

Chain multiple assertions for comprehensive checks:

```java
assertThat(result)
    .isMap()
    .hasField("name")
    .hasStringField("name", "Alice")
    .hasIntField("level", 10)
    .doesNotHaveField("deleted")
    .field("position")
        .hasIntField("x", 100)
        .hasIntField("y", 64)
        .hasIntField("z", -200);
```

## DataResultAssert

Assertions for `DataResult<A>` objects. Useful when testing codecs or operations that return DataResult.

### Success/Error Assertions

```java
// Check success
assertThat(result).isSuccess();
assertThat(result).isSuccessWithValue(expectedValue);

// Check error
assertThat(result).isError();
assertThat(result).isErrorWithMessage("Expected error message");
assertThat(result).isErrorWithMessageContaining("required field");

// Check partial
assertThat(result).isPartial();
```

### Value Assertions

```java
// Get and check success value
assertThat(result)
    .isSuccess()
    .hasValue(expectedValue);

// Check value satisfies condition
assertThat(result)
    .isSuccess()
    .hasValueSatisfying(value -> {
        assertThat(value.name()).isEqualTo("Alice");
        assertThat(value.level()).isEqualTo(10);
    });

// Check result with consumer
assertThat(result).satisfies(r -> {
    r.result().ifPresent(value -> {
        // custom assertions
    });
});
```

### Error Message Assertions

```java
assertThat(result)
    .isError()
    .hasErrorMessage("Field 'name' is required");

assertThat(result)
    .isError()
    .hasErrorMessageContaining("required")
    .hasErrorMessageContaining("name");
```

## TypedAssert

Assertions for `Typed<A>` objects. Useful when testing typed data structures.

### Type Assertions

```java
// Check type
assertThat(typed).hasType(expectedType);
assertThat(typed).hasTypeReference(TypeReference.of("player"));
assertThat(typed).hasTypeReference("player");
assertThat(typed).hasTypeDescription("player");
```

### Value Assertions

```java
// Check value
assertThat(typed).hasValue(expectedValue);

// Check value satisfies condition
assertThat(typed).hasValueSatisfying(value -> {
    assertThat(value.name()).isEqualTo("Alice");
});

// Check value type
assertThat(typed).hasValueInstanceOf(Player.class);
```

### Encoding Assertions

Encode the typed value and continue with DynamicAssert:

```java
assertThat(typed)
    .encodedWith(GsonOps.INSTANCE)
    .hasField("name")
    .hasStringField("name", "Alice");
```

### Extraction

Extract parts for standard AssertJ assertions:

```java
// Extract value
assertThat(typed)
    .extractingValue()
    .isInstanceOf(Player.class);

// Extract type
assertThat(typed)
    .extractingType()
    .isNotNull();
```

## Complete Example

```java
import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;

@Test
void testPlayerMigration() {
    // Arrange
    Dynamic<JsonElement> input = TestData.gson().object()
        .put("playerName", "Alice")
        .put("xp", 1500)
        .build();

    // Act
    Dynamic<JsonElement> result = migrate(input, 1, 2);

    // Assert with fluent assertions
    assertThat(result)
        .isMap()
        .hasFields("name", "experience", "level")
        .doesNotHaveField("playerName")
        .doesNotHaveField("xp")
        .hasStringField("name", "Alice")
        .hasIntField("experience", 1500)
        .hasIntField("level", 15);
}

@Test
void testCodecDecoding() {
    // Arrange
    Dynamic<JsonElement> input = TestData.gson().object()
        .put("name", "Alice")
        .put("level", 10)
        .build();

    // Act
    DataResult<Player> result = PLAYER_CODEC.parse(input);

    // Assert
    assertThat(result)
        .isSuccess()
        .hasValueSatisfying(player -> {
            org.assertj.core.api.Assertions.assertThat(player.name()).isEqualTo("Alice");
            org.assertj.core.api.Assertions.assertThat(player.level()).isEqualTo(10);
        });
}

@Test
void testCodecError() {
    // Arrange - missing required field
    Dynamic<JsonElement> input = TestData.gson().object()
        .put("level", 10)
        .build();

    // Act
    DataResult<Player> result = PLAYER_CODEC.parse(input);

    // Assert
    assertThat(result)
        .isError()
        .hasErrorMessageContaining("name");
}
```

## Mixing with Standard AssertJ

You can mix testkit assertions with standard AssertJ:

```java
import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat; // Standard AssertJ

@Test
void testMixedAssertions() {
    Dynamic<JsonElement> result = migrate(input, 1, 2);

    // Testkit assertions for Dynamic
    de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions
        .assertThat(result)
        .hasStringField("name", "Alice");

    // Standard AssertJ for other values
    org.assertj.core.api.Assertions
        .assertThat(someString)
        .isEqualTo("expected");
}
```

## Related

- [Testkit Overview](index.md)
- [Test Data Builders](test-data-builders.md)
- [DataResult](../concepts/data-result.md)
