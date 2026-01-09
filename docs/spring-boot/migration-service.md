# MigrationService API

The `MigrationService` provides a high-level, fluent API for executing data migrations in Spring Boot applications. It abstracts away the complexity of working directly with `AetherDataFixer` and integrates seamlessly with multi-domain setups, metrics, and async execution.

---

## Overview

```java
@Service
public class DataService {

    private final MigrationService migrationService;

    public DataService(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public void migrateData(TaggedDynamic<?> data, int fromVersion) {
        MigrationResult result = migrationService
            .migrate(data)           // Start builder
            .from(fromVersion)       // Source version
            .toLatest()              // Target: current version
            .execute();              // Execute migration

        if (result.isSuccess()) {
            processData(result.getData());
        }
    }
}
```

---

## Builder API

### Starting a Migration

```java
MigrationRequestBuilder builder = migrationService.migrate(data);
```

The `migrate()` method accepts a `TaggedDynamic<?>` and returns a builder for configuring the migration.

---

### Specifying Source Version

```java
// Using DataVersion
builder.from(new DataVersion(100));

// Using integer (convenience method)
builder.from(100);
```

**Required**: You must specify the source version before execution.

---

### Specifying Target Version

```java
// Explicit version
builder.to(new DataVersion(200));
builder.to(200);

// Latest version (resolved at execution time)
builder.toLatest();
```

**Required**: You must specify either an explicit target or use `toLatest()`.

---

### Selecting a Domain

```java
// Use a specific domain (for multi-domain setups)
builder.usingDomain("game");
builder.usingDomain("user");

// Default domain is "default" (single-bootstrap setups)
```

**Optional**: If not specified, uses the default domain.

---

### Custom DynamicOps

```java
// Override the auto-configured DynamicOps
builder.withOps(GsonOps.INSTANCE);
builder.withOps(JacksonOps.INSTANCE);
```

**Optional**: If not specified, uses the auto-configured ops.

---

### Executing the Migration

```java
// Synchronous execution
MigrationResult result = builder.execute();

// Asynchronous execution
CompletableFuture<MigrationResult> future = builder.executeAsync();
```

---

## Complete Examples

### Basic Migration

```java
MigrationResult result = migrationService
    .migrate(playerData)
    .from(100)
    .to(200)
    .execute();

if (result.isSuccess()) {
    TaggedDynamic<?> migratedData = result.getData();
    // Process the migrated data
}
```

### Migration to Latest Version

```java
MigrationResult result = migrationService
    .migrate(gameData)
    .from(savedVersion)
    .toLatest()        // Resolves to current version at runtime
    .execute();
```

### Multi-Domain Migration

```java
// Migrate game data
MigrationResult gameResult = migrationService
    .migrate(gameSave)
    .usingDomain("game")
    .from(100)
    .toLatest()
    .execute();

// Migrate user data
MigrationResult userResult = migrationService
    .migrate(userProfile)
    .usingDomain("user")
    .from(50)
    .to(150)
    .execute();
```

### Asynchronous Migration

```java
CompletableFuture<MigrationResult> future = migrationService
    .migrate(largeDataSet)
    .from(100)
    .to(200)
    .executeAsync();

// Non-blocking callback
future.thenAccept(result -> {
    if (result.isSuccess()) {
        processData(result.getData());
    } else {
        handleError(result.getError().orElse(null));
    }
});

// Or wait with timeout
MigrationResult result = future.get(30, TimeUnit.SECONDS);
```

---

## MigrationResult

The `MigrationResult` is an immutable object containing all information about the migration outcome.

### Checking Success/Failure

```java
if (result.isSuccess()) {
    // Migration completed successfully
}

if (result.isFailure()) {
    // Migration failed
}
```

### Accessing Migrated Data

```java
// Throws IllegalStateException if migration failed
TaggedDynamic<?> data = result.getData();

// Safe access with Optional
Optional<TaggedDynamic> dataOpt = result.getDataOptional();
dataOpt.ifPresent(d -> processData(d));

// With default
TaggedDynamic<?> data = result.getDataOptional()
    .orElseGet(() -> createDefaultData());
```

### Version Information

```java
DataVersion fromVersion = result.getFromVersion();
DataVersion toVersion = result.getToVersion();
int versionSpan = result.getVersionSpan();  // |toVersion - fromVersion|

System.out.printf("Migrated from v%d to v%d (span: %d)%n",
    fromVersion.getVersion(),
    toVersion.getVersion(),
    versionSpan);
```

### Domain and Duration

```java
String domain = result.getDomain();
Duration duration = result.getDuration();

System.out.printf("Migration in domain '%s' took %dms%n",
    domain,
    duration.toMillis());
```

### Error Information

```java
Optional<Throwable> error = result.getError();

error.ifPresent(e -> {
    log.error("Migration failed: {}", e.getMessage(), e);
});

// Chain as cause in custom exception
throw new DataLoadException(
    "Failed to load data",
    result.getError().orElse(null)
);
```

---

## Service Methods

### Get Current Version

```java
// Default domain
DataVersion version = migrationService.getCurrentVersion();

// Specific domain
DataVersion gameVersion = migrationService.getCurrentVersion("game");
DataVersion userVersion = migrationService.getCurrentVersion("user");
```

### Check Domain Availability

```java
if (migrationService.hasDomain("game")) {
    // Domain exists
}

// Get all available domains
Set<String> domains = migrationService.getAvailableDomains();
// Returns: ["default", "game", "user"]
```

---

## Error Handling Patterns

### Result-Based Error Handling

```java
MigrationResult result = migrationService
    .migrate(data)
    .from(100)
    .to(200)
    .execute();

if (result.isFailure()) {
    log.error("Migration failed from v{} to v{} in domain '{}' after {}ms",
        result.getFromVersion().getVersion(),
        result.getToVersion().getVersion(),
        result.getDomain(),
        result.getDuration().toMillis());

    result.getError().ifPresent(error -> {
        log.error("Cause: {}", error.getMessage(), error);
        alertService.notifyMigrationFailure(error);
    });

    // Fail fast or use fallback
    throw new MigrationException("Data migration failed", result.getError().orElse(null));
}
```

### Validation Errors

Validation errors (missing configuration) are thrown as exceptions, not captured in the result:

```java
try {
    MigrationResult result = migrationService
        .migrate(data)
        // Missing: .from(version)
        .to(200)
        .execute();
} catch (IllegalStateException e) {
    // "Source version not specified. Call .from(version) before execute()."
}

try {
    MigrationResult result = migrationService
        .migrate(data)
        .from(100)
        .usingDomain("nonexistent")
        .toLatest()
        .execute();
} catch (IllegalArgumentException e) {
    // "Unknown domain: 'nonexistent'. Available domains: [default, game]"
}
```

### Try-With-Fallback Pattern

```java
public Player loadPlayer(TaggedDynamic<?> data, int version) {
    // Check if migration needed
    int currentVersion = migrationService.getCurrentVersion().getVersion();
    if (version >= currentVersion) {
        return deserialize(data);
    }

    // Try migration
    MigrationResult result = migrationService
        .migrate(data)
        .from(version)
        .toLatest()
        .execute();

    if (result.isSuccess()) {
        return deserialize(result.getData());
    }

    // Fallback: try to load with defaults
    log.warn("Migration failed, attempting fallback load");
    return loadWithDefaults(data);
}
```

---

## Async Patterns

### Fire and Forget

```java
migrationService
    .migrate(data)
    .from(100)
    .toLatest()
    .executeAsync()
    .thenAccept(result -> {
        if (result.isSuccess()) {
            saveToCache(result.getData());
        }
    })
    .exceptionally(throwable -> {
        log.error("Async migration failed", throwable);
        return null;
    });
```

### Parallel Migrations

```java
List<TaggedDynamic<?>> dataItems = loadAllData();

List<CompletableFuture<MigrationResult>> futures = dataItems.stream()
    .map(data -> migrationService
        .migrate(data)
        .from(100)
        .toLatest()
        .executeAsync())
    .toList();

// Wait for all to complete
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
    .join();

// Collect results
List<MigrationResult> results = futures.stream()
    .map(CompletableFuture::join)
    .toList();

long successCount = results.stream().filter(MigrationResult::isSuccess).count();
log.info("Migrated {} of {} items successfully", successCount, results.size());
```

### Custom Executor

For applications with specific threading requirements:

```java
@Configuration
public class MigrationConfig {

    @Bean
    public MigrationService migrationService(
            DataFixerRegistry registry,
            @Nullable MigrationMetrics metrics) {
        // Use virtual threads (Java 21+)
        Executor executor = Executors.newVirtualThreadPerTaskExecutor();
        return new DefaultMigrationService(registry, metrics, executor);
    }
}
```

---

## Thread Safety

- `MigrationService` is **thread-safe** and can be shared across threads
- Each `migrate()` call returns a new builder instance
- `MigrationRequestBuilder` is **NOT thread-safe** — use separate instances per thread
- `MigrationResult` is **immutable** and thread-safe

```java
// SAFE: Service shared, builders separate
@Service
public class ConcurrentService {

    private final MigrationService migrationService;  // Thread-safe

    public void processInParallel(List<TaggedDynamic<?>> items) {
        items.parallelStream().forEach(data -> {
            // Each thread gets its own builder
            MigrationResult result = migrationService
                .migrate(data)
                .from(100)
                .toLatest()
                .execute();
            // Process result...
        });
    }
}
```

---

## Metrics Integration

When `MigrationMetrics` is available, the service automatically records:

- **Success counter**: Incremented on successful migration
- **Failure counter**: Incremented on failed migration (with error type tag)
- **Duration timer**: Records migration duration
- **Version span**: Records the number of versions migrated

No additional code is needed — metrics are recorded transparently.

---

## Related Documentation

- [Configuration Reference](configuration.md) — Configure service behavior
- [Multi-Domain Setup](multi-domain.md) — Working with multiple domains
- [Metrics Integration](metrics.md) — Detailed metrics reference
