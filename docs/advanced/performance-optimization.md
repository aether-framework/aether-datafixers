# Performance Optimization

Tips for optimizing migrations with large datasets.

## Lazy Schema Initialization

```java
// Schemas are initialized lazily by default
// Only accessed schemas are fully constructed
```

## Minimize Dynamic Operations

```java
// Avoid: Multiple individual accesses
String name = data.get("name").asString().orElse("");
int level = data.get("level").asInt().orElse(0);
int health = data.get("health").asInt().orElse(100);

// Better: Single transformation
return data.update("stats", this::transformStats);
```

## Batch Processing

```java
// Process in batches for memory efficiency
List<JsonObject> batch = new ArrayList<>(BATCH_SIZE);

for (JsonObject item : largeDataset) {
    batch.add(item);
    if (batch.size() >= BATCH_SIZE) {
        processBatch(batch);
        batch.clear();
    }
}
processBatch(batch);  // Remaining items
```

## Parallel Processing

```java
List<TaggedDynamic> items = loadItems();

List<TaggedDynamic> migrated = items.parallelStream()
    .map(item -> fixer.update(item, fromVersion, toVersion))
    .toList();
```

## Cache Reused Values

```java
// Cache values used multiple times
private final DataVersion targetVersion = fixer.getCurrentVersion();

public void migrateItem(JsonObject item, int version) {
    if (version >= targetVersion.version()) {
        return;  // Skip current version
    }
    // Migrate...
}
```

## Skip Unchanged Data

```java
public TaggedDynamic migrateIfNeeded(TaggedDynamic data, DataVersion version) {
    if (!version.isOlderThan(fixer.getCurrentVersion())) {
        return data;  // Already current
    }
    return fixer.update(data, version, fixer.getCurrentVersion());
}
```

## Profiling

```java
long start = System.nanoTime();
TaggedDynamic result = fixer.update(data, from, to);
long elapsed = System.nanoTime() - start;

if (elapsed > THRESHOLD_NANOS) {
    logger.warn("Slow migration: {}ms for {}",
        elapsed / 1_000_000, data.type().id());
}
```

## Memory Considerations

- Process streams instead of collecting to lists
- Clear references to processed data
- Use appropriate batch sizes
- Consider memory-mapped files for very large datasets

## Related

- [Concurrent Migrations](concurrent-migrations.md)
- [Debug Migrations](../how-to/debug-migrations.md)

