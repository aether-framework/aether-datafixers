# Thread Safety

Aether Datafixers is designed to be thread-safe, allowing concurrent data migrations without synchronization in most cases. This document explains the thread-safety guarantees and best practices.

## Thread Safety Guarantees

### Immutable Types

The following types are immutable and fully thread-safe:

| Type            | Thread Safety | Notes                           |
|-----------------|---------------|---------------------------------|
| `DataVersion`   | Immutable     | Safe to share between threads   |
| `TypeReference` | Immutable     | Safe to share between threads   |
| `Dynamic<T>`    | Immutable     | Operations return new instances |
| `DataResult<T>` | Immutable     | Safe to share between threads   |
| `Pair<A, B>`    | Immutable     | Safe to share between threads   |
| `Either<L, R>`  | Immutable     | Safe to share between threads   |

### Thread-Safe Components

| Component        | Thread Safety        | Notes                                |
|------------------|----------------------|--------------------------------------|
| `DataFixer`      | Thread-safe          | Concurrent `update()` calls are safe |
| `Schema`         | Immutable after init | Safe to share after construction     |
| `SchemaRegistry` | Thread-safe          | Concurrent reads are safe            |
| `TypeRegistry`   | Immutable after init | Safe to share after construction     |

### Usage Guidelines

| Scenario                   | Safe? | Notes                                      |
|----------------------------|-------|--------------------------------------------|
| Concurrent migrations      | Yes   | Multiple threads can call `fixer.update()` |
| Shared fixer instance      | Yes   | Single fixer for entire application        |
| Concurrent reads of result | Yes   | `Dynamic` and `DataResult` are immutable   |
| Bootstrap registration     | No    | Not thread-safe during setup               |

## Immutability in Practice

### Dynamic Operations

`Dynamic` is immutable — all operations return new instances:

```java
Dynamic<JsonElement> original = new Dynamic<>(GsonOps.INSTANCE, json);

// This creates a NEW Dynamic, original is unchanged
Dynamic<JsonElement> modified = original.set("name", original.createString("Alex"));

// original still has the old value
String originalName = original.get("name").asString().orElse("");  // "Steve"
String modifiedName = modified.get("name").asString().orElse("");  // "Alex"
```

### Safe Concurrent Access

```java
// Shared fixer - created once at startup
private static final AetherDataFixer FIXER = createFixer();

// Safe: Multiple threads can use the same fixer
public TaggedDynamic migratePlayer(JsonObject data) {
    Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, data);
    TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

    // Thread-safe operation
    return FIXER.update(tagged, savedVersion, currentVersion);
}

// Called from multiple threads simultaneously
executor.submit(() -> migratePlayer(player1Data));
executor.submit(() -> migratePlayer(player2Data));
executor.submit(() -> migratePlayer(player3Data));
```

## Bootstrap Phase

The bootstrap phase is **not** thread-safe. Complete initialization before sharing:

```java
public class Application {
    private static final AetherDataFixer FIXER;

    static {
        // Bootstrap runs in a single thread during class initialization
        FIXER = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
        // After this point, FIXER is safely publishable
    }

    public static AetherDataFixer fixer() {
        return FIXER;  // Safe to call from any thread
    }
}
```

### Unsafe Pattern (Avoid)

```java
// UNSAFE: Don't bootstrap from multiple threads
public AetherDataFixer getFixer() {
    if (fixer == null) {  // Race condition!
        fixer = new DataFixerRuntimeFactory()
            .create(version, new MyBootstrap());
    }
    return fixer;
}
```

### Safe Pattern

```java
// SAFE: Use holder pattern
public class FixerHolder {
    private FixerHolder() {}

    // Initialized on first access, thread-safe by JVM guarantee
    private static class Holder {
        static final AetherDataFixer INSTANCE = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
    }

    public static AetherDataFixer get() {
        return Holder.INSTANCE;
    }
}
```

## Stateless DataFixes

DataFix implementations should be stateless:

### Good: Stateless Fix

```java
public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super("player_v1_to_v2", new DataVersion(100), new DataVersion(110), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema input, Schema output) {
        // Stateless: No instance fields modified
        return Rules.transform(TypeReferences.PLAYER, this::transform);
    }

    private Dynamic<?> transform(Dynamic<?> player) {
        // Pure function: no side effects
        String name = player.get("name").asString().orElse("Unknown");
        return player.set("displayName", player.createString("[" + name + "]"));
    }
}
```

### Bad: Stateful Fix (Avoid)

```java
public class BadFix extends SchemaDataFix {
    // UNSAFE: Mutable state!
    private int counter = 0;
    private List<String> names = new ArrayList<>();

    @Override
    protected TypeRewriteRule makeRule(Schema input, Schema output) {
        return Rules.transform(TypeReferences.PLAYER, player -> {
            counter++;  // Race condition!
            names.add(player.get("name").asString().orElse(""));  // Race condition!
            return player;
        });
    }
}
```

## Thread-Safe Patterns

### Pattern 1: Singleton Fixer

```java
public enum DataFixerService {
    INSTANCE;

    private final AetherDataFixer fixer;

    DataFixerService() {
        this.fixer = new DataFixerRuntimeFactory()
            .create(CURRENT_VERSION, new GameDataBootstrap());
    }

    public TaggedDynamic migrate(TaggedDynamic data, DataVersion from) {
        return fixer.update(data, from, fixer.currentVersion());
    }
}

// Usage from any thread
TaggedDynamic migrated = DataFixerService.INSTANCE.migrate(data, savedVersion);
```

### Pattern 2: Dependency Injection

```java
@Singleton
public class MigrationService {
    private final AetherDataFixer fixer;

    @Inject
    public MigrationService(AetherDataFixer fixer) {
        this.fixer = fixer;
    }

    public Dynamic<?> migratePlayer(Dynamic<?> data, DataVersion from) {
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, data);
        return fixer.update(tagged, from, fixer.currentVersion()).value();
    }
}
```

### Pattern 3: Concurrent Processing

```java
public class BatchMigrator {
    private final AetherDataFixer fixer;
    private final ExecutorService executor;

    public CompletableFuture<List<Dynamic<?>>> migrateAll(
        List<Dynamic<?>> items,
        DataVersion from
    ) {
        List<CompletableFuture<Dynamic<?>>> futures = items.stream()
            .map(item -> CompletableFuture.supplyAsync(
                () -> migrate(item, from),
                executor
            ))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    private Dynamic<?> migrate(Dynamic<?> data, DataVersion from) {
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, data);
        return fixer.update(tagged, from, fixer.currentVersion()).value();
    }
}
```

## Performance Considerations

### No Contention

Because `Dynamic` is immutable, there's no lock contention during concurrent migrations:

```java
// Each thread works with its own Dynamic instances
// No synchronization needed

Thread 1: original1 → modified1 (new instance)
Thread 2: original2 → modified2 (new instance)
Thread 3: original3 → modified3 (new instance)
```

### Memory Considerations

Immutability means more object allocations:

```java
// Each operation creates new objects
Dynamic<?> step1 = original.set("a", value1);    // New Dynamic
Dynamic<?> step2 = step1.set("b", value2);       // New Dynamic
Dynamic<?> step3 = step2.set("c", value3);       // New Dynamic
```

For high-throughput scenarios:
- Use `-XX:+UseG1GC` or ZGC for better GC performance
- Consider batching operations
- Profile to identify bottlenecks

## Summary

### Thread-Safe

- `DataFixer.update()` calls
- Reading `Dynamic` values
- All immutable types (`DataVersion`, `TypeReference`, etc.)
- Sharing fixer instances across threads

### Not Thread-Safe

- Bootstrap/initialization phase
- Mutable state in DataFix implementations (avoid this)
- Modifying shared collections during fixes (avoid this)

### Best Practices

1. Initialize fixer once at startup
2. Keep DataFix implementations stateless
3. Use pure functions in transformations
4. Share the fixer instance freely
5. Each thread gets its own `Dynamic` instances

---

## Related

- [DataFix System](datafix-system.md) — Creating thread-safe fixes
- [Dynamic System](dynamic-system.md) — Immutable data wrapper
- [Architecture Overview](architecture-overview.md) — Framework design

