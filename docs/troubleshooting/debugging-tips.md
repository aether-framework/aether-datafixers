# Debugging Tips

Strategies for finding and fixing migration issues.

## Print Data Before/After

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    System.out.println("BEFORE: " + player.value());

    Dynamic<?> result = migratePlayer(player);

    System.out.println("AFTER: " + result.value());
    return result;
});
```

## List All Fields

```java
player.asMap().result().ifPresent(map -> {
    System.out.println("Fields:");
    map.keySet().forEach(key ->
        System.out.println("  " + key.asString().orElse("?")));
});
```

## Check Field Existence

```java
boolean exists = player.get("fieldName").result().isPresent();
System.out.println("Field exists: " + exists);
```

## Check Field Type

```java
Dynamic<?> field = player.get("field").orElseEmptyMap();
Object value = field.value();
System.out.println("Type: " + value.getClass().getName());
System.out.println("Value: " + value);
```

## Step-by-Step Migration

```java
private Dynamic<?> migrate(Dynamic<?> player) {
    System.out.println("Step 0: " + player.value());

    Dynamic<?> step1 = renameFields(player);
    System.out.println("Step 1: " + step1.value());

    Dynamic<?> step2 = addDefaults(step1);
    System.out.println("Step 2: " + step2.value());

    return step2;
}
```

## Verify Fix Registration

```java
@Override
public void registerFixes(FixRegistrar fixes) {
    DataFix<?> fix = new PlayerV1ToV2Fix(schemas);
    System.out.println("Registering: " + fix.name() +
        " (" + fix.fromVersion() + " -> " + fix.toVersion() + ")");
    fixes.register(TypeReferences.PLAYER, fix);
}
```

## Unit Test Specific Case

```java
@Test
void debugSpecificIssue() {
    // Exact problematic data
    JsonObject input = new JsonObject();
    input.addProperty("weirdField", "problematic value");

    Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, input);

    System.out.println("Input: " + input);

    Dynamic<?> result = fix.apply(dynamic);

    System.out.println("Output: " + result.value());
}
```

## Related

- [Common Errors](common-errors.md)
- [Debug Migrations How-To](../how-to/debug-migrations.md)
- [Test Migrations](../how-to/test-migrations.md)

