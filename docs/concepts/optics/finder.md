# Finder

A **Finder** is a specialized optic designed for navigating `Dynamic` data structures. It provides a type-safe way to locate and extract values from unstructured or semi-structured data like JSON, with optional type conversion.

## Definition

```java
public interface Finder<A> {
    /** Find the value in a Dynamic */
    Optional<A> find(Dynamic<?> dynamic);

    /** Compose with another finder */
    <B> Finder<B> then(Finder<B> next);
}
```

**Type Parameter:**
- `A` — The type of value being extracted

## Conceptual Model

Finder navigates through Dynamic data, handling the uncertainty at each step:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Dynamic<?> data                               │
│  {                                                               │
│    "player": {                                                   │
│      "name": "Steve",         ◀── Finder.field("player")        │
│      "stats": {                   .then(Finder.field("name"))   │
│        "level": 10                .then(Finder.asString())      │
│      }                                                           │
│    }                              Result: Optional.of("Steve")   │
│  }                                                               │
└─────────────────────────────────────────────────────────────────┘
```

## Creating Finders

### Field Navigation

```java
// Navigate to a field
Finder<Dynamic<?>> player = Finder.field("player");
Finder<Dynamic<?>> stats = Finder.field("stats");

// Chained navigation
Finder<Dynamic<?>> playerStats = Finder.field("player")
    .then(Finder.field("stats"));
```

### Type Extraction

```java
// Extract as specific types
Finder<String> asString = Finder.asString();
Finder<Integer> asInt = Finder.asInt();
Finder<Long> asLong = Finder.asLong();
Finder<Double> asDouble = Finder.asDouble();
Finder<Boolean> asBool = Finder.asBool();
```

### Complete Paths

```java
// Navigate and extract in one chain
Finder<String> playerName = Finder.field("player")
    .then(Finder.field("name"))
    .then(Finder.asString());

Finder<Integer> playerLevel = Finder.field("player")
    .then(Finder.field("stats"))
    .then(Finder.field("level"))
    .then(Finder.asInt());
```

## Using Finders

### Basic Navigation

```java
JsonObject json = new JsonObject();
json.addProperty("name", "Steve");
json.addProperty("level", 10);

Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);

// Find values
Finder<String> nameFinder = Finder.field("name").then(Finder.asString());
Finder<Integer> levelFinder = Finder.field("level").then(Finder.asInt());

Optional<String> name = nameFinder.find(dynamic);   // Optional.of("Steve")
Optional<Integer> level = levelFinder.find(dynamic); // Optional.of(10)
```

### Deep Navigation

```java
// Nested JSON structure
// { "player": { "position": { "x": 100.5, "y": 64.0, "z": -200.0 } } }

Finder<Double> playerX = Finder.field("player")
    .then(Finder.field("position"))
    .then(Finder.field("x"))
    .then(Finder.asDouble());

Optional<Double> x = playerX.find(dynamic);  // Optional.of(100.5)
```

### Handling Missing Data

```java
// If any step fails, the whole finder returns empty
Finder<String> deepPath = Finder.field("a")
    .then(Finder.field("b"))
    .then(Finder.field("c"))
    .then(Finder.asString());

// If "b" doesn't exist, result is Optional.empty()
Optional<String> result = deepPath.find(dynamic);
```

## Composition

### Sequential Composition

```java
// Build finders incrementally
Finder<Dynamic<?>> player = Finder.field("player");
Finder<Dynamic<?>> stats = Finder.field("stats");
Finder<Dynamic<?>> level = Finder.field("level");
Finder<Integer> asInt = Finder.asInt();

// Compose step by step
Finder<Integer> playerLevel = player
    .then(stats)
    .then(level)
    .then(asInt);
```

### Reusable Paths

```java
// Define reusable path segments
class PlayerPaths {
    public static final Finder<Dynamic<?>> PLAYER = Finder.field("player");
    public static final Finder<Dynamic<?>> STATS = Finder.field("stats");
    public static final Finder<Dynamic<?>> POSITION = Finder.field("position");

    public static final Finder<String> NAME = PLAYER
        .then(Finder.field("name"))
        .then(Finder.asString());

    public static final Finder<Integer> LEVEL = PLAYER
        .then(STATS)
        .then(Finder.field("level"))
        .then(Finder.asInt());

    public static final Finder<Double> X = PLAYER
        .then(POSITION)
        .then(Finder.field("x"))
        .then(Finder.asDouble());
}

// Usage
Optional<String> name = PlayerPaths.NAME.find(dynamic);
Optional<Integer> level = PlayerPaths.LEVEL.find(dynamic);
```

## Finder Types

### Field Finder

Navigates to a specific field:

```java
Finder<Dynamic<?>> namefield = Finder.field("name");
// Returns the Dynamic for the "name" field, or empty if not present
```

### Index Finder

Accesses list elements by index:

```java
Finder<Dynamic<?>> firstItem = Finder.index(0);
Finder<Dynamic<?>> thirdItem = Finder.index(2);

// Navigate to first item's name
Finder<String> firstName = Finder.field("items")
    .then(Finder.index(0))
    .then(Finder.field("name"))
    .then(Finder.asString());
```

### Type Finder

Filters by TypeReference:

```java
Finder<Dynamic<?>> playerType = Finder.type(TypeReferences.PLAYER);
// Only succeeds if the data is tagged with PLAYER type
```

### Conversion Finders

Convert Dynamic to specific types:

```java
Finder.asString()   // Dynamic → String
Finder.asInt()      // Dynamic → Integer
Finder.asLong()     // Dynamic → Long
Finder.asDouble()   // Dynamic → Double
Finder.asBool()     // Dynamic → Boolean
Finder.asList()     // Dynamic → List<Dynamic<?>>
Finder.asMap()      // Dynamic → Map<Dynamic<?>, Dynamic<?>>
```

## Practical Examples

### Configuration Reading

```java
public class ConfigReader {
    private final Dynamic<?> config;

    public ConfigReader(Dynamic<?> config) {
        this.config = config;
    }

    public String getServerName() {
        return Finder.field("server")
            .then(Finder.field("name"))
            .then(Finder.asString())
            .find(config)
            .orElse("default-server");
    }

    public int getPort() {
        return Finder.field("server")
            .then(Finder.field("port"))
            .then(Finder.asInt())
            .find(config)
            .orElse(8080);
    }

    public boolean isDebugEnabled() {
        return Finder.field("debug")
            .then(Finder.asBool())
            .find(config)
            .orElse(false);
    }
}
```

### Data Migration

```java
public class PlayerMigration {

    // Old format: { "playerName": "...", "xp": 100 }
    // New format: { "name": "...", "experience": 100 }

    private static final Finder<String> OLD_NAME =
        Finder.field("playerName").then(Finder.asString());

    private static final Finder<Integer> OLD_XP =
        Finder.field("xp").then(Finder.asInt());

    public Dynamic<?> migrate(Dynamic<?> old) {
        String name = OLD_NAME.find(old).orElse("Unknown");
        int xp = OLD_XP.find(old).orElse(0);

        return old
            .remove("playerName")
            .remove("xp")
            .set("name", old.createString(name))
            .set("experience", old.createInt(xp));
    }
}
```

### Validation

```java
public class DataValidator {

    public List<String> validate(Dynamic<?> data) {
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (Finder.field("name").then(Finder.asString()).find(data).isEmpty()) {
            errors.add("Missing required field: name");
        }

        if (Finder.field("email").then(Finder.asString()).find(data).isEmpty()) {
            errors.add("Missing required field: email");
        }

        // Validate numeric ranges
        Finder.field("age").then(Finder.asInt()).find(data).ifPresent(age -> {
            if (age < 0 || age > 150) {
                errors.add("Invalid age: " + age);
            }
        });

        return errors;
    }
}
```

### List Processing

```java
public List<String> extractNames(Dynamic<?> data) {
    // { "items": [{"name": "a"}, {"name": "b"}, {"name": "c"}] }

    return Finder.field("items")
        .then(Finder.asList())
        .find(data)
        .orElse(List.of())
        .stream()
        .map(item -> Finder.field("name")
            .then(Finder.asString())
            .find(item)
            .orElse("unnamed"))
        .toList();
}
```

## Finder vs Other Optics

| Optic      | Target       | Optional | Type-Safe  |
|------------|--------------|----------|------------|
| Lens       | Static types | No       | Yes        |
| Affine     | Static types | Yes      | Yes        |
| Prism      | Static types | Yes      | Yes        |
| **Finder** | Dynamic data | Yes      | At runtime |

### When to Use Finder

✅ Use Finder when:
- Working with `Dynamic` data
- Parsing JSON/config without full deserialization
- Navigating unstructured or semi-structured data
- Building data migration tools

❌ Don't use Finder when:
- Working with typed Java objects (use Lens)
- Data structure is known at compile time

## Finder Summary

| Method          | Description            | Returns           |
|-----------------|------------------------|-------------------|
| `find(Dynamic)` | Extract the value      | `Optional<A>`     |
| `then(Finder)`  | Compose with next step | `Finder<B>`       |
| `field(name)`   | Navigate to field      | `Finder<Dynamic>` |
| `index(i)`      | Navigate to list index | `Finder<Dynamic>` |
| `asString()`    | Extract as String      | `Finder<String>`  |
| `asInt()`       | Extract as Integer     | `Finder<Integer>` |
| `asDouble()`    | Extract as Double      | `Finder<Double>`  |
| `asList()`      | Extract as List        | `Finder<List>`    |

---

## Related

- [Dynamic System](../dynamic-system.md) — The data Finder navigates
- [Affine](affine.md) — Similar optional semantics
- [Optics Overview](index.md) — Optic hierarchy

