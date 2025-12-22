# How to Log Migrations

This guide shows how to add logging to track migration execution.

## Basic Logging in Transforms

```java
import java.util.logging.Logger;

public class PlayerV1ToV2Fix extends SchemaDataFix {
    private static final Logger LOGGER = Logger.getLogger(PlayerV1ToV2Fix.class.getName());

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.PLAYER, player -> {
            LOGGER.info("Migrating player from v1 to v2");

            String oldName = player.get("playerName").asString().orElse("Unknown");
            LOGGER.fine("Original name: " + oldName);

            Dynamic<?> result = player
                .set("name", player.createString(oldName))
                .remove("playerName");

            LOGGER.fine("Migration complete");
            return result;
        });
    }
}
```

## SLF4J Logging

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerV1ToV2Fix extends SchemaDataFix {
    private static final Logger log = LoggerFactory.getLogger(PlayerV1ToV2Fix.class);

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.PLAYER, player -> {
            log.debug("Starting player migration v1 -> v2");

            // Your transformation
            Dynamic<?> result = migratePlayer(player);

            log.debug("Completed player migration");
            return result;
        });
    }
}
```

## Log Before and After

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    if (log.isTraceEnabled()) {
        log.trace("BEFORE: {}", player.value());
    }

    Dynamic<?> result = performMigration(player);

    if (log.isTraceEnabled()) {
        log.trace("AFTER: {}", result.value());
    }

    return result;
});
```

## Conditional Logging

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    // Only log if something interesting happens
    boolean hadLegacyField = player.get("legacyData").result().isPresent();

    Dynamic<?> result = migratePlayer(player);

    if (hadLegacyField) {
        log.info("Migrated legacy data for player");
    }

    return result;
});
```

## Count Migrations

```java
public class CountingFix extends SchemaDataFix {
    private final AtomicLong migrationCount = new AtomicLong(0);

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(TypeReferences.PLAYER, player -> {
            long count = migrationCount.incrementAndGet();

            if (count % 1000 == 0) {
                log.info("Migrated {} players", count);
            }

            return migratePlayer(player);
        });
    }

    public long getMigrationCount() {
        return migrationCount.get();
    }
}
```

## Log Timing

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    long start = System.nanoTime();

    Dynamic<?> result = performMigration(player);

    long elapsed = System.nanoTime() - start;
    if (elapsed > 1_000_000) {  // > 1ms
        log.warn("Slow migration: {}ms", elapsed / 1_000_000);
    }

    return result;
});
```

## Logging Wrapper Fix

Create a reusable logging wrapper:

```java
public class LoggingFix<T> implements DataFix<T> {
    private final DataFix<T> delegate;
    private final Logger log;
    private final String fixName;

    public LoggingFix(DataFix<T> delegate) {
        this.delegate = delegate;
        this.log = LoggerFactory.getLogger(delegate.getClass());
        this.fixName = delegate.name();
    }

    @Override
    public Dynamic<T> apply(Dynamic<T> input) {
        log.debug("[{}] Starting migration v{} -> v{}",
            fixName, fromVersion(), toVersion());

        long start = System.nanoTime();
        Dynamic<T> result = delegate.apply(input);
        long elapsed = System.nanoTime() - start;

        log.debug("[{}] Completed in {}μs", fixName, elapsed / 1000);
        return result;
    }

    @Override
    public DataVersion fromVersion() {
        return delegate.fromVersion();
    }

    @Override
    public DataVersion toVersion() {
        return delegate.toVersion();
    }

    @Override
    public String name() {
        return delegate.name();
    }
}

// Usage in bootstrap
@Override
public void registerFixes(FixRegistrar fixes) {
    fixes.register(TypeReferences.PLAYER,
        new LoggingFix<>(new PlayerV1ToV2Fix(schemas)));
}
```

## Log Errors Without Throwing

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    try {
        return migratePlayer(player);
    } catch (Exception e) {
        log.error("Migration failed for player data: {}", player.value(), e);
        // Return original data unchanged
        return player;
    }
});
```

## Structured Logging

```java
return Rules.transform(TypeReferences.PLAYER, player -> {
    String playerId = player.get("id").asString().orElse("unknown");

    // Use MDC for structured logging
    MDC.put("playerId", playerId);
    MDC.put("fixVersion", "v1-to-v2");

    try {
        log.info("Starting migration");
        Dynamic<?> result = migratePlayer(player);
        log.info("Migration successful");
        return result;
    } finally {
        MDC.clear();
    }
});
```

## Log Summary Statistics

```java
public class StatisticsCollector {
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong success = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();

    public void recordSuccess(long timeNanos) {
        total.incrementAndGet();
        success.incrementAndGet();
        totalTime.addAndGet(timeNanos);
    }

    public void recordError() {
        total.incrementAndGet();
        errors.incrementAndGet();
    }

    public void logSummary(Logger log) {
        log.info("Migration Statistics:");
        log.info("  Total: {}", total.get());
        log.info("  Success: {}", success.get());
        log.info("  Errors: {}", errors.get());
        log.info("  Avg time: {}μs",
            total.get() > 0 ? totalTime.get() / total.get() / 1000 : 0);
    }
}
```

## Production Logging Configuration

```xml
<!-- logback.xml -->
<configuration>
    <!-- Only log warnings for migrations in production -->
    <logger name="com.example.fixes" level="WARN"/>

    <!-- Enable debug for specific problematic fix -->
    <logger name="com.example.fixes.PlayerV5ToV6Fix" level="DEBUG"/>
</configuration>
```

## Best Practices

1. **Use appropriate log levels**
   - TRACE: Full data before/after
   - DEBUG: Migration start/end
   - INFO: Significant milestones
   - WARN: Slow migrations, fallbacks
   - ERROR: Failures

2. **Avoid logging sensitive data** in production

3. **Use lazy evaluation** for expensive log messages:
   ```java
   log.debug("Data: {}", () -> serializeForLogging(data));
   ```

4. **Consider log volume** — migrations may process millions of records

## Related

- [Debug Migrations](debug-migrations.md)
- [Test Migrations](test-migrations.md)
- [DataFix System](../concepts/datafix-system.md)

