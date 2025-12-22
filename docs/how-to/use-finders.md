# How to Use Finders

This guide shows how to use Finder optics to navigate and transform Dynamic data structures.

## What is a Finder?

A Finder navigates to specific locations within a Dynamic data structure:

```java
// Navigate to a nested field
Finder<Dynamic<?>> nameFinder = Finder.field("user").field("profile").field("name");

// Apply finder to data
OptionalDynamic<?> name = nameFinder.find(data);
```

## Basic Field Access

```java
// Create a finder for a single field
Finder<Dynamic<?>> idFinder = Finder.field("id");

// Use with Dynamic data
Dynamic<JsonElement> data = new Dynamic<>(GsonOps.INSTANCE, jsonObject);
OptionalDynamic<?> id = idFinder.find(data);

// Read the value
String idValue = id.asString().orElse("unknown");
```

## Chained Field Access

Navigate through nested structures:

```java
// Navigate: data -> settings -> audio -> volume
Finder<Dynamic<?>> volumeFinder = Finder.field("settings")
    .field("audio")
    .field("volume");

int volume = volumeFinder.find(data)
    .asInt()
    .orElse(100);
```

## List Element Access

Access elements within lists:

```java
// Get first item in inventory
Finder<Dynamic<?>> firstItemFinder = Finder.field("inventory")
    .element(0);

// Get all items
Finder<Dynamic<?>> allItemsFinder = Finder.field("inventory")
    .allElements();
```

## Use in Transformations

Transform data at a specific location:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Create finder for nested field
    Finder<Dynamic<?>> healthFinder = Finder.field("stats").field("health");

    // Modify at that location
    return healthFinder.modify(player, health -> {
        int current = health.asInt().orElse(100);
        return health.createInt(Math.min(current, 100)); // Cap at 100
    });
});
```

## Conditional Finding

Find only if condition is met:

```java
Finder<Dynamic<?>> premiumFinder = Finder.field("subscription")
    .filter(sub -> sub.get("type").asString().orElse("").equals("premium"));

OptionalDynamic<?> premium = premiumFinder.find(data);

if (premium.result().isPresent()) {
    // User has premium subscription
}
```

## Transform All Matching

Transform all elements that match:

```java
return Rules.transform(TypeReferences.INVENTORY, inv -> {
    // Find all items and transform each
    Finder<Dynamic<?>> itemsFinder = Finder.field("items").allElements();

    return itemsFinder.modifyAll(inv, item -> {
        // Upgrade item format
        String id = item.get("id").asString().orElse("unknown");
        return item.set("itemId", item.createString("item:" + id))
                   .remove("id");
    });
});
```

## Compose Finders

Combine multiple finders:

```java
// Finder for player position
Finder<Dynamic<?>> positionFinder = Finder.field("position");

// Finder for x coordinate within position
Finder<Dynamic<?>> xFinder = Finder.field("x");

// Compose them
Finder<Dynamic<?>> playerXFinder = positionFinder.then(xFinder);

double x = playerXFinder.find(player).asDouble().orElse(0.0);
```

## Find with Default

Return a default if not found:

```java
Finder<Dynamic<?>> settingsFinder = Finder.field("settings");

Dynamic<?> settings = settingsFinder.findOrDefault(data, data.emptyMap());
```

## Find in List by Predicate

Find a specific element in a list:

```java
// Find item with specific ID
Finder<Dynamic<?>> swordFinder = Finder.field("inventory")
    .elementWhere(item ->
        item.get("id").asString().orElse("").equals("sword"));

OptionalDynamic<?> sword = swordFinder.find(player);
```

## Nested List Access

Navigate nested lists:

```java
// Access: guilds[0].members[0].name
Finder<Dynamic<?>> firstMemberNameFinder = Finder.field("guilds")
    .element(0)
    .field("members")
    .element(0)
    .field("name");
```

## Use in DataFix

Complete example in a DataFix:

```java
public class NestedFieldFix extends SchemaDataFix {

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.PLAYER, this::migratePlayer);
    }

    private Dynamic<?> migratePlayer(Dynamic<?> player) {
        // Define finders
        Finder<Dynamic<?>> oldLocationFinder = Finder.field("data")
            .field("location");

        Finder<Dynamic<?>> newLocationFinder = Finder.field("position");

        // Get old value
        OptionalDynamic<?> oldLocation = oldLocationFinder.find(player);

        if (oldLocation.result().isEmpty()) {
            return player;
        }

        // Move to new location
        Dynamic<?> locationData = oldLocation.orElseEmptyMap();

        return player
            .set("position", locationData)
            .update("data", data -> data.remove("location"));
    }
}
```

## Safe Deep Updates

Update deeply nested data safely:

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Safely update nested field, creating path if needed
    return Finder.field("settings")
        .field("graphics")
        .field("quality")
        .setOrCreate(player, player.createString("high"));
});
```

## Extract Multiple Values

Extract values from multiple locations:

```java
Finder<Dynamic<?>> xFinder = Finder.field("x");
Finder<Dynamic<?>> yFinder = Finder.field("y");
Finder<Dynamic<?>> zFinder = Finder.field("z");

double x = xFinder.find(data).asDouble().orElse(0.0);
double y = yFinder.find(data).asDouble().orElse(0.0);
double z = zFinder.find(data).asDouble().orElse(0.0);
```

## Finder vs Direct Access

Compare approaches:

```java
// Direct access (verbose for deep nesting)
Dynamic<?> volume = data
    .get("settings").orElseEmptyMap()
    .get("audio").orElseEmptyMap()
    .get("volume").orElseEmptyMap();

// Finder (cleaner for deep nesting)
Finder<Dynamic<?>> volumeFinder = Finder.field("settings")
    .field("audio")
    .field("volume");
Dynamic<?> volume = volumeFinder.find(data).orElseEmptyMap();
```

## Best Practices

1. **Define finders as constants** for reuse:
   ```java
   private static final Finder<Dynamic<?>> PLAYER_NAME =
       Finder.field("player").field("name");
   ```

2. **Use finders for repeated access patterns**

3. **Compose finders** rather than building long chains repeatedly

4. **Handle missing values** with orElse or orElseEmptyMap

## Related

- [Finder Optic](../concepts/optics/finder.md)
- [Dynamic System](../concepts/dynamic-system.md)
- [Transform Field](transform-field.md)

