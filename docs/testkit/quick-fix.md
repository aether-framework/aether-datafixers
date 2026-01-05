# QuickFix Factories

`QuickFix` provides factory methods for creating common `DataFix` implementations without boilerplate. These are ideal for unit tests where you need simple fixes quickly.

## Overview

Instead of creating full `DataFix` subclasses for testing:

```java
// Without QuickFix - verbose
public class RenamePlayerNameFix extends SchemaDataFix {
    public RenamePlayerNameFix(Schema input, Schema output) {
        super("rename_player_name", input, output);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema input, Schema output) {
        return Rules.renameField("playerName", "name");
    }
}
```

Use factory methods:

```java
// With QuickFix - concise
DataFix<JsonElement> fix = QuickFix.renameField(
    GsonOps.INSTANCE, "rename_player_name", 1, 2,
    "playerName", "name"
);
```

## Factory Methods

### simple()

Create a custom fix with a transformation function:

```java
DataFix<JsonElement> fix = QuickFix.simple(
    "add_version", 1, 2,
    input -> input.set("version", input.createInt(2))
);
```

The function receives the input `Dynamic` and returns the transformed `Dynamic`.

### identity()

Create a no-op fix that passes data through unchanged:

```java
DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
```

Useful for testing the test harness itself or as placeholders.

### renameField()

Rename a field from one name to another:

```java
DataFix<JsonElement> fix = QuickFix.renameField(
    GsonOps.INSTANCE,
    "rename_player_name",   // fix name
    1,                       // from version
    2,                       // to version
    "playerName",            // old field name
    "name"                   // new field name
);
```

### addStringField()

Add a string field with a default value:

```java
DataFix<JsonElement> fix = QuickFix.addStringField(
    GsonOps.INSTANCE,
    "add_status",     // fix name
    1,                 // from version
    2,                 // to version
    "status",          // field name
    "active"           // default value
);
```

### addIntField()

Add an integer field with a default value:

```java
DataFix<JsonElement> fix = QuickFix.addIntField(
    GsonOps.INSTANCE,
    "add_score",      // fix name
    1,                 // from version
    2,                 // to version
    "score",           // field name
    0                  // default value
);
```

### addBooleanField()

Add a boolean field with a default value:

```java
DataFix<JsonElement> fix = QuickFix.addBooleanField(
    GsonOps.INSTANCE,
    "add_active",     // fix name
    1,                 // from version
    2,                 // to version
    "active",          // field name
    true               // default value
);
```

### removeField()

Remove a field from the data:

```java
DataFix<JsonElement> fix = QuickFix.removeField(
    GsonOps.INSTANCE,
    "remove_legacy",  // fix name
    1,                 // from version
    2,                 // to version
    "legacyField"      // field to remove
);
```

### transformField()

Transform a field value using a function:

```java
// Uppercase a string field
DataFix<JsonElement> fix = QuickFix.transformField(
    GsonOps.INSTANCE,
    "uppercase_name",  // fix name
    1,                  // from version
    2,                  // to version
    "name",             // field name
    d -> d.createString(d.asString().result().orElse("").toUpperCase())
);

// Double a numeric field
DataFix<JsonElement> doubleFix = QuickFix.transformField(
    GsonOps.INSTANCE,
    "double_score",
    1, 2,
    "score",
    d -> d.createInt(d.asInt().result().orElse(0) * 2)
);
```

### conditional()

Apply a fix only when a condition is met:

```java
DataFix<JsonElement> fix = QuickFix.conditional(
    GsonOps.INSTANCE,
    "migrate_if_old",  // fix name
    1,                  // from version
    2,                  // to version
    // Condition: only apply if "legacy" field exists
    input -> input.get("legacy").result().isPresent(),
    // Transformation
    input -> input
        .remove("legacy")
        .set("migrated", input.createBoolean(true))
);
```

### compose()

Combine multiple fixes into one:

```java
DataFix<JsonElement> combined = QuickFix.compose(
    GsonOps.INSTANCE,
    "combined_migration",
    1, 2,
    // All fixes to apply in order
    QuickFix.renameField(GsonOps.INSTANCE, "rename", 1, 2, "oldName", "name"),
    QuickFix.addIntField(GsonOps.INSTANCE, "add", 1, 2, "score", 0),
    QuickFix.removeField(GsonOps.INSTANCE, "remove", 1, 2, "deprecated")
);
```

## Complete Examples

### Testing Field Rename

```java
@Test
void shouldRenameField() {
    DataFix<JsonElement> fix = QuickFix.renameField(
        GsonOps.INSTANCE, "rename_player_name", 1, 2,
        "playerName", "name"
    );

    Dynamic<JsonElement> input = TestData.gson().object()
        .put("playerName", "Alice")
        .put("level", 10)
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .apply();

    assertThat(result)
        .hasStringField("name", "Alice")
        .doesNotHaveField("playerName");
}
```

### Testing Field Addition

```java
@Test
void shouldAddDefaultField() {
    DataFix<JsonElement> fix = QuickFix.addStringField(
        GsonOps.INSTANCE, "add_status", 1, 2,
        "status", "active"
    );

    Dynamic<JsonElement> input = TestData.gson().object()
        .put("name", "Alice")
        .build();

    Dynamic<JsonElement> expected = TestData.gson().object()
        .put("name", "Alice")
        .put("status", "active")
        .build();

    DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .expectOutput(expected)
        .verify();
}
```

### Testing Conditional Fix

```java
@Test
void shouldApplyConditionalFix() {
    DataFix<JsonElement> fix = QuickFix.conditional(
        GsonOps.INSTANCE, "migrate_old_format", 1, 2,
        input -> input.get("format").asString().result()
            .map(f -> f.equals("v1"))
            .orElse(false),
        input -> input
            .set("format", input.createString("v2"))
            .set("migrated", input.createBoolean(true))
    );

    // Input with old format
    Dynamic<JsonElement> oldFormat = TestData.gson().object()
        .put("format", "v1")
        .put("data", "test")
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(oldFormat)
        .forType("config")
        .apply();

    assertThat(result)
        .hasStringField("format", "v2")
        .hasBooleanField("migrated", true);

    // Input with new format - should not be modified
    Dynamic<JsonElement> newFormat = TestData.gson().object()
        .put("format", "v2")
        .put("data", "test")
        .build();

    Dynamic<JsonElement> unchanged = DataFixTester.forFix(fix)
        .withInput(newFormat)
        .forType("config")
        .apply();

    assertThat(unchanged)
        .hasStringField("format", "v2")
        .doesNotHaveField("migrated");
}
```

### Testing Composed Fixes

```java
@Test
void shouldApplyComposedFixes() {
    DataFix<JsonElement> combined = QuickFix.compose(
        GsonOps.INSTANCE, "full_migration", 1, 2,
        QuickFix.renameField(GsonOps.INSTANCE, "r1", 1, 2, "playerName", "name"),
        QuickFix.renameField(GsonOps.INSTANCE, "r2", 1, 2, "xp", "experience"),
        QuickFix.addIntField(GsonOps.INSTANCE, "a1", 1, 2, "level", 1)
    );

    Dynamic<JsonElement> input = TestData.gson().object()
        .put("playerName", "Alice")
        .put("xp", 1500)
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(combined)
        .withInput(input)
        .forType("player")
        .apply();

    assertThat(result)
        .hasStringField("name", "Alice")
        .hasIntField("experience", 1500)
        .hasIntField("level", 1)
        .doesNotHaveField("playerName")
        .doesNotHaveField("xp");
}
```

## Version Information

Each QuickFix carries version information:

```java
DataFix<JsonElement> fix = QuickFix.simple("test", 1, 2, d -> d);

assertThat(fix.name()).isEqualTo("test");
assertThat(fix.fromVersion().getVersion()).isEqualTo(1);
assertThat(fix.toVersion().getVersion()).isEqualTo(2);
```

## Using with DataVersion

You can also use `DataVersion` objects instead of integers:

```java
DataFix<JsonElement> fix = QuickFix.simple(
    "test",
    new DataVersion(100),
    new DataVersion(110),
    input -> input.set("version", input.createInt(110))
);
```

## Best Practices

1. **Use descriptive fix names** — Names appear in error messages and logs
2. **Keep fixes focused** — One fix should do one thing
3. **Use compose() sparingly** — Prefer individual fixes for clarity in tests
4. **Match production patterns** — Use QuickFix patterns that mirror your production code

## Related

- [Testkit Overview](index.md)
- [DataFixTester](datafix-tester.md)
- [DataFix System](../concepts/datafix-system.md)
- [How to Rename Fields](../how-to/rename-field.md)
