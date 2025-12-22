# How to Debug Migrations

This guide shows strategies for troubleshooting migration issues.

## Print Before/After

Add logging to see what's happening:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    System.out.println("=== BEFORE ===");
    System.out.println(player.value());

    // Your transformation
    Dynamic<?> result = player.set("name", player.createString("test"));

    System.out.println("=== AFTER ===");
    System.out.println(result.value());

    return result;
});
```

## Step-by-Step Debugging

Break complex transformations into steps:

```java
private Dynamic<?> migratePlayer(Dynamic<?> player) {
    System.out.println("Step 0 (input): " + player.value());

    Dynamic<?> step1 = renameFields(player);
    System.out.println("Step 1 (renamed): " + step1.value());

    Dynamic<?> step2 = transformValues(step1);
    System.out.println("Step 2 (transformed): " + step2.value());

    Dynamic<?> step3 = restructure(step2);
    System.out.println("Step 3 (restructured): " + step3.value());

    return step3;
}
```

## Check Field Existence

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Debug: Print all fields
    System.out.println("Available fields:");
    player.asMap().result().ifPresent(map -> {
        map.keySet().forEach(key -> {
            System.out.println("  - " + key.asString().orElse("?"));
        });
    });

    // Check specific field
    boolean hasName = player.get("name").result().isPresent();
    System.out.println("Has 'name': " + hasName);

    return player;
});
```

## Validate Field Values

```java
private Dynamic<?> validateAndMigrate(Dynamic<?> player) {
    // Check expected field types
    OptionalDynamic<?> nameField = player.get("name");

    if (nameField.result().isEmpty()) {
        System.err.println("WARNING: 'name' field is missing!");
    } else {
        DataResult<String> nameResult = nameField.asString();
        if (nameResult.error().isPresent()) {
            System.err.println("WARNING: 'name' is not a string: " +
                nameResult.error().get().message());
        }
    }

    // Continue with migration...
    return player;
}
```

## Use DataResult for Error Tracking

```java
private Dynamic<?> safeMigrate(Dynamic<?> player) {
    DataResult<String> nameResult = player.get("name").asString();

    return nameResult.map(name -> {
        // Success path
        return player.set("displayName", player.createString("[" + name + "]"));
    }).resultOrPartial(error -> {
        // Log errors but continue
        System.err.println("Migration warning: " + error);
    }).orElse(player);  // Return original if failed
}
```

## Test with Known Data

Create a minimal test case:

```java
@Test
void debugSpecificIssue() {
    // Create exact input that's causing problems
    JsonObject input = new JsonObject();
    input.addProperty("playerName", "Steve");
    input.addProperty("xp", 100);
    // Add problematic data
    input.add("weirdField", JsonNull.INSTANCE);

    // Apply migration with logging
    Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, input);

    System.out.println("Input: " + new Gson().toJson(input));

    // Apply each step and print
    Dynamic<?> result = fix.apply(dynamic);

    System.out.println("Output: " + new Gson().toJson(result.value()));
}
```

## Check Version Chain

Verify fixes are being applied:

```java
public class DebugBootstrap implements DataFixerBootstrap {

    @Override
    public void registerFixes(FixRegistrar fixes) {
        System.out.println("Registering fixes:");

        DataFix<?> fix1 = new PlayerV1ToV2Fix(schemas);
        System.out.println("  - " + fix1.name() +
            " (" + fix1.fromVersion() + " → " + fix1.toVersion() + ")");
        fixes.register(TypeReferences.PLAYER, fix1);

        DataFix<?> fix2 = new PlayerV2ToV3Fix(schemas);
        System.out.println("  - " + fix2.name() +
            " (" + fix2.fromVersion() + " → " + fix2.toVersion() + ")");
        fixes.register(TypeReferences.PLAYER, fix2);
    }
}
```

## Trace Migration Path

```java
public TaggedDynamic debugUpdate(
    AetherDataFixer fixer,
    TaggedDynamic data,
    DataVersion from,
    DataVersion to
) {
    System.out.println("=== Migration Debug ===");
    System.out.println("Type: " + data.type().id());
    System.out.println("From version: " + from.version());
    System.out.println("To version: " + to.version());
    System.out.println("Input data: " + data.value().value());

    TaggedDynamic result = fixer.update(data, from, to);

    System.out.println("Output data: " + result.value().value());
    System.out.println("======================");

    return result;
}
```

## Common Issues

### Field Not Found

```
// Symptom: orElse always returns default
// Debug:
player.asMap().result().ifPresent(map -> {
    System.out.println("Actual fields: " + map.keySet());
});
// Check for typos, case sensitivity
```

### Wrong Type

```
// Symptom: asString() fails on a field
// Debug:
Object rawValue = ((Dynamic<?>) player.get("field").orElseEmptyMap()).value();
System.out.println("Type: " + rawValue.getClass());
System.out.println("Value: " + rawValue);
```

### Nested Object Missing

```
// Symptom: get("nested").get("field") returns empty
// Debug:
Dynamic<?> nested = player.get("nested").orElseEmptyMap();
System.out.println("Nested exists: " + player.get("nested").result().isPresent());
System.out.println("Nested value: " + nested.value());
System.out.println("Nested type: " + nested.value().getClass());
```

### Fix Not Applied

```
// Symptom: Data unchanged after migration
// Check:
// 1. Correct TypeReference used?
// 2. Version numbers match?
// 3. Fix registered in bootstrap?
// 4. fromVersion < toVersion?
```

## Enable Verbose Logging

Add a debugging fix wrapper:

```java
public class DebuggingFix implements DataFix<JsonElement> {
    private final DataFix<JsonElement> delegate;

    public DebuggingFix(DataFix<JsonElement> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Dynamic<JsonElement> apply(Dynamic<JsonElement> input) {
        System.out.println("[" + name() + "] Input: " + input.value());
        Dynamic<JsonElement> result = delegate.apply(input);
        System.out.println("[" + name() + "] Output: " + result.value());
        return result;
    }

    // Delegate other methods...
}
```

## Related

- [Test Migrations](test-migrations.md)
- [Log Migrations](log-migrations.md)
- [DataResult](../concepts/data-result.md)

