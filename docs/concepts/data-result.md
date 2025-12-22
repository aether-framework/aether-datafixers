# DataResult

`DataResult<T>` is the error-handling mechanism used throughout Aether Datafixers. It represents an operation that can either succeed with a value or fail with an error message, similar to `Result` types in other languages.

## Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                       DataResult<T>                              │
│                                                                  │
│  ┌───────────────────────────┐  ┌───────────────────────────┐   │
│  │        Success            │  │         Error             │   │
│  │  ┌─────────────────────┐  │  │  ┌─────────────────────┐  │   │
│  │  │   value: T          │  │  │  │  message: String    │  │   │
│  │  └─────────────────────┘  │  │  └─────────────────────┘  │   │
│  └───────────────────────────┘  └───────────────────────────┘   │
│                                                                  │
│  Optional: partial result even on error                          │
└─────────────────────────────────────────────────────────────────┘
```

Unlike exceptions, `DataResult` makes error handling explicit and composable.

## Creating DataResults

### Success

```java
// Create a successful result
DataResult<String> success = DataResult.success("value");
DataResult<Integer> number = DataResult.success(42);
DataResult<Player> player = DataResult.success(new Player("Steve", 10));
```

### Error

```java
// Create an error result
DataResult<String> error = DataResult.error("Something went wrong");
DataResult<Integer> parseError = DataResult.error("Invalid number format");
DataResult<Player> notFound = DataResult.error("Player not found: " + name);
```

### Error with Partial Result

Sometimes an operation partially succeeds:

```java
// Error with partial data
DataResult<Player> partial = DataResult.error(
    "Invalid level, using default",
    new Player("Steve", 1)  // Partial result with default level
);
```

## Checking Results

### Using result() and error()

```java
DataResult<String> result = someOperation();

// Check for success
if (result.result().isPresent()) {
    String value = result.result().get();
    System.out.println("Success: " + value);
}

// Check for error
if (result.error().isPresent()) {
    DataResult.Error error = result.error().get();
    System.err.println("Error: " + error.message());
}
```

### Using isSuccess() and isError()

```java
if (result.isSuccess()) {
    // Handle success
} else {
    // Handle error
}
```

## Extracting Values

### orElse

Get the value or a default:

```java
String value = result.result().orElse("default");
int level = levelResult.result().orElse(1);
```

### orElseThrow

Get the value or throw an exception:

```java
String value = result.result()
    .orElseThrow(() -> new IllegalStateException("Expected value"));

// Or with message from error
String value = result.getOrThrow(error -> new RuntimeException(error));
```

### resultOrPartial

Get either the success value or partial result, with error logging:

```java
Optional<Player> player = result.resultOrPartial(
    error -> logger.warn("Parse error: {}", error)
);
```

## Transforming Results

### map

Transform the success value:

```java
DataResult<String> name = DataResult.success("steve");
DataResult<String> upper = name.map(String::toUpperCase);
// Success("STEVE")

DataResult<Integer> length = name.map(String::length);
// Success(5)
```

### flatMap

Chain operations that return DataResult:

```java
DataResult<String> input = DataResult.success("42");

DataResult<Integer> parsed = input.flatMap(s -> {
    try {
        return DataResult.success(Integer.parseInt(s));
    } catch (NumberFormatException e) {
        return DataResult.error("Not a number: " + s);
    }
});
// Success(42)

// With invalid input
DataResult<String> invalid = DataResult.success("not-a-number");
DataResult<Integer> failed = invalid.flatMap(s -> {
    try {
        return DataResult.success(Integer.parseInt(s));
    } catch (NumberFormatException e) {
        return DataResult.error("Not a number: " + s);
    }
});
// Error("Not a number: not-a-number")
```

### mapError

Transform the error message:

```java
DataResult<String> result = DataResult.error("raw error");
DataResult<String> formatted = result.mapError(
    msg -> "Operation failed: " + msg
);
// Error("Operation failed: raw error")
```

## Combining Results

### apply2

Combine two results:

```java
DataResult<String> name = DataResult.success("Steve");
DataResult<Integer> level = DataResult.success(10);

DataResult<Player> player = name.apply2(
    level,
    (n, l) -> new Player(n, l)
);
// Success(Player("Steve", 10))

// If either fails
DataResult<String> badName = DataResult.error("Missing name");
DataResult<Player> failed = badName.apply2(
    level,
    Player::new
);
// Error("Missing name")
```

### apply3, apply4, etc.

Combine more results:

```java
DataResult<String> name = DataResult.success("Steve");
DataResult<Integer> level = DataResult.success(10);
DataResult<Position> pos = DataResult.success(new Position(0, 64, 0));

DataResult<Player> player = name.apply3(
    level,
    pos,
    (n, l, p) -> new Player(n, l, p)
);
```

## Common Patterns

### Validation

```java
public DataResult<Integer> validateLevel(int level) {
    if (level < 1) {
        return DataResult.error("Level must be positive: " + level);
    }
    if (level > 100) {
        return DataResult.error("Level cannot exceed 100: " + level);
    }
    return DataResult.success(level);
}

// Usage
DataResult<Integer> result = validateLevel(50);  // Success(50)
DataResult<Integer> invalid = validateLevel(-5); // Error("Level must be positive: -5")
```

### Parsing

```java
public DataResult<Integer> parseInt(String s) {
    try {
        return DataResult.success(Integer.parseInt(s));
    } catch (NumberFormatException e) {
        return DataResult.error("Invalid integer: " + s);
    }
}

public DataResult<UUID> parseUUID(String s) {
    try {
        return DataResult.success(UUID.fromString(s));
    } catch (IllegalArgumentException e) {
        return DataResult.error("Invalid UUID: " + s);
    }
}
```

### Chaining Operations

```java
public DataResult<Player> loadPlayer(String input) {
    return parseJson(input)                           // DataResult<JsonElement>
        .flatMap(this::extractPlayerData)             // DataResult<JsonObject>
        .flatMap(this::validatePlayerData)            // DataResult<JsonObject>
        .flatMap(this::deserializePlayer);            // DataResult<Player>
}
```

### Accumulating Errors

```java
public DataResult<Config> validateConfig(Config config) {
    List<String> errors = new ArrayList<>();

    if (config.maxPlayers() < 1) {
        errors.add("maxPlayers must be positive");
    }
    if (config.maxPlayers() > 1000) {
        errors.add("maxPlayers cannot exceed 1000");
    }
    if (config.name() == null || config.name().isBlank()) {
        errors.add("name is required");
    }

    if (errors.isEmpty()) {
        return DataResult.success(config);
    } else {
        return DataResult.error(String.join("; ", errors));
    }
}
```

## Using with Dynamic

`Dynamic` operations return `DataResult`:

```java
Dynamic<JsonElement> player = ...;

// Getting primitive values
DataResult<String> name = player.get("name").asString();
DataResult<Integer> level = player.get("level").asInt();
DataResult<Double> x = player.get("position").get("x").asDouble();

// Safe extraction with defaults
String nameValue = name.result().orElse("Unknown");
int levelValue = level.result().orElse(1);

// Chained with validation
DataResult<Player> parsed = name.apply2(
    level,
    (n, l) -> new Player(n, l)
);
```

## Using with Codecs

Codec operations also return `DataResult`:

```java
// Decoding
Dynamic<JsonElement> dynamic = ...;
DataResult<Pair<Player, JsonElement>> decoded = Player.CODEC.decode(dynamic);

// Handle result
Player player = decoded.resultOrPartial(error ->
    logger.warn("Decode error: {}", error)
).map(Pair::getFirst).orElseThrow();

// Encoding
Player player = new Player("Steve", 10);
DataResult<JsonElement> encoded = Player.CODEC.encode(
    player,
    GsonOps.INSTANCE,
    GsonOps.INSTANCE.empty()
);

JsonElement json = encoded.result().orElseThrow();
```

## Lifecycle

DataResult includes lifecycle information for tracking side effects:

```java
// Check lifecycle
Lifecycle lifecycle = result.lifecycle();

// Stable: Can be cached
// Experimental: May change, use with caution
```

## Best Practices

### 1. Prefer DataResult Over Exceptions

```java
// Good: Explicit error handling
public DataResult<Player> findPlayer(String id) {
    Player player = database.find(id);
    if (player == null) {
        return DataResult.error("Player not found: " + id);
    }
    return DataResult.success(player);
}

// Avoid: Hidden control flow
public Player findPlayer(String id) throws PlayerNotFoundException {
    Player player = database.find(id);
    if (player == null) {
        throw new PlayerNotFoundException(id);  // Hidden!
    }
    return player;
}
```

### 2. Use flatMap for Chaining

```java
// Good: Clear chain of operations
public DataResult<ProcessedData> process(String input) {
    return parse(input)
        .flatMap(this::validate)
        .flatMap(this::transform)
        .flatMap(this::finalize);
}

// Avoid: Nested if-else
public DataResult<ProcessedData> process(String input) {
    DataResult<ParsedData> parsed = parse(input);
    if (parsed.isError()) {
        return DataResult.error(parsed.error().get().message());
    }
    // ... more nesting
}
```

### 3. Provide Helpful Error Messages

```java
// Good: Detailed error message
DataResult.error("Field 'level' must be between 1 and 100, got: " + level)

// Avoid: Vague message
DataResult.error("Invalid level")
```

### 4. Use resultOrPartial for Logging

```java
// Good: Log errors while extracting value
Optional<Player> player = result.resultOrPartial(
    error -> logger.warn("Player parse error: {}", error)
);

// Handle the optional appropriately
player.ifPresent(this::processPlayer);
```

### 5. Combine with map for Transformations

```java
// Good: Clean transformation chain
DataResult<String> displayName = loadPlayer(id)
    .map(Player::name)
    .map(String::toUpperCase)
    .map(n -> "[" + n + "]");
```

---

## Summary

| Method | Purpose |
|--------|---------|
| `success(value)` | Create success result |
| `error(message)` | Create error result |
| `result()` | Get Optional success value |
| `error()` | Get Optional error |
| `map(fn)` | Transform success value |
| `flatMap(fn)` | Chain DataResult operations |
| `mapError(fn)` | Transform error message |
| `apply2(other, fn)` | Combine two results |
| `resultOrPartial(logger)` | Get value with error logging |

---

## Related

- [Dynamic System](dynamic-system.md) — Returns DataResult for operations
- [Codec System](codec-system.md) — Encode/decode with DataResult
- [Type System](type-system.md) — Type operations use DataResult

