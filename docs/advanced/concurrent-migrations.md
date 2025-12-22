# Concurrent Migrations

Thread-safe patterns for parallel data migration.

## Thread Safety of DataFixer

`DataFixer` and its components are immutable after construction:

```java
// Safe: Create once, use from multiple threads
AetherDataFixer fixer = new DataFixerRuntimeFactory()
    .create(targetVersion, bootstrap);

// Safe: Concurrent migrations
ExecutorService executor = Executors.newFixedThreadPool(4);

List<Future<TaggedDynamic>> futures = items.stream()
    .map(item -> executor.submit(() -> fixer.update(item, from, to)))
    .toList();
```

## Parallel Stream Processing

```java
List<TaggedDynamic> results = items.parallelStream()
    .map(item -> fixer.update(item, from, to))
    .toList();
```

## ForkJoinPool

```java
ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

List<TaggedDynamic> results = pool.submit(() ->
    items.parallelStream()
        .map(item -> fixer.update(item, from, to))
        .toList()
).get();
```

## CompletableFuture

```java
List<CompletableFuture<TaggedDynamic>> futures = items.stream()
    .map(item -> CompletableFuture.supplyAsync(() ->
        fixer.update(item, from, to), executor))
    .toList();

List<TaggedDynamic> results = futures.stream()
    .map(CompletableFuture::join)
    .toList();
```

## Thread-Safe Fix Implementation

```java
public class ThreadSafeFix extends SchemaDataFix {

    // Immutable state only
    private final String fieldName;
    private final String defaultValue;

    @Override
    protected TypeRewriteRule makeRule(Schema input, Schema output) {
        // Rules are immutable
        return Rules.addField(TYPE, fieldName, d -> d.createString(defaultValue));
    }
}
```

## Avoid Shared Mutable State

```java
// BAD: Shared mutable counter
private int count = 0;  // Race condition!

// GOOD: Thread-local or atomic
private final AtomicInteger count = new AtomicInteger(0);
```

## Related

- [Performance Optimization](performance-optimization.md)
- [Thread Safety Concept](../concepts/thread-safety.md)

