# Security Best Practices

This document provides general security best practices for processing untrusted data with Aether Datafixers. These practices apply across all serialization formats.

## Defense in Depth

Security should be implemented in layers. No single control is sufficient—combine multiple measures:

1. **Input Validation** — Check size and format before parsing
2. **Safe Parser Configuration** — Use security-hardened parser settings
3. **Resource Limits** — Enforce depth, size, and time limits
4. **Monitoring** — Log and alert on suspicious activity
5. **Sandboxing** — Isolate high-risk processing

---

## Input Validation Before Migration

### Size Validation

Always validate input size before parsing:

```java
public class InputValidator {

    private static final long MAX_PAYLOAD_SIZE = 10 * 1024 * 1024; // 10MB

    public void validateSize(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        if (input.length > MAX_PAYLOAD_SIZE) {
            throw new PayloadTooLargeException(
                "Payload size " + input.length + " exceeds maximum " + MAX_PAYLOAD_SIZE);
        }
    }

    public void validateSize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        if (input.length() > MAX_PAYLOAD_SIZE) {
            throw new PayloadTooLargeException(
                "Payload size " + input.length() + " exceeds maximum " + MAX_PAYLOAD_SIZE);
        }
    }

    public void validateSize(InputStream input, long contentLength) {
        if (contentLength > MAX_PAYLOAD_SIZE) {
            throw new PayloadTooLargeException(
                "Content-Length " + contentLength + " exceeds maximum " + MAX_PAYLOAD_SIZE);
        }
    }
}
```

### Size-Limited InputStream

For streaming scenarios, wrap the input stream:

```java
public class SizeLimitedInputStream extends FilterInputStream {

    private final long maxSize;
    private long bytesRead = 0;

    public SizeLimitedInputStream(InputStream in, long maxSize) {
        super(in);
        this.maxSize = maxSize;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            bytesRead++;
            checkLimit();
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n > 0) {
            bytesRead += n;
            checkLimit();
        }
        return n;
    }

    private void checkLimit() throws IOException {
        if (bytesRead > maxSize) {
            throw new IOException("Input exceeds maximum size of " + maxSize + " bytes");
        }
    }
}

// Usage
InputStream limited = new SizeLimitedInputStream(userInput, 10 * 1024 * 1024);
Object data = yaml.load(limited);
```

---

## Depth and Nesting Limits

Deep nesting can cause stack overflow or excessive memory consumption.

### Parser-Level Limits (Preferred)

Use built-in parser limits when available:

```java
// Jackson
StreamReadConstraints constraints = StreamReadConstraints.builder()
    .maxNestingDepth(50)
    .build();

// SnakeYAML
LoaderOptions options = new LoaderOptions();
options.setNestingDepthLimit(50);
```

### Application-Level Validation

For parsers without built-in limits, validate after parsing:

```java
public class DepthValidator {

    private static final int MAX_DEPTH = 50;

    public void validateDepth(Dynamic<?> dynamic) {
        validateDepth(dynamic, 0);
    }

    private void validateDepth(Dynamic<?> dynamic, int depth) {
        if (depth > MAX_DEPTH) {
            throw new SecurityException("Data exceeds maximum depth of " + MAX_DEPTH);
        }

        // Check map entries
        dynamic.getMap().result().ifPresent(map -> {
            map.values().forEach(value -> validateDepth(value, depth + 1));
        });

        // Check list elements
        dynamic.getList().result().ifPresent(list -> {
            list.forEach(element -> validateDepth(element, depth + 1));
        });
    }
}
```

---

## Timeout Configuration

Long-running migrations can be exploited for DoS. Implement timeouts:

```java
import java.util.concurrent.*;

public class TimedMigrationService {

    private final AetherDataFixer fixer;
    private final ExecutorService executor;
    private final Duration timeout;

    public TimedMigrationService(AetherDataFixer fixer, Duration timeout) {
        this.fixer = fixer;
        this.executor = Executors.newCachedThreadPool();
        this.timeout = timeout;
    }

    public <T> TaggedDynamic<T> migrateWithTimeout(
            TaggedDynamic<T> input,
            DataVersion from,
            DataVersion to) throws TimeoutException {

        Future<TaggedDynamic<T>> future = executor.submit(
            () -> fixer.update(input, from, to)
        );

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new MigrationTimeoutException(
                "Migration timed out after " + timeout, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MigrationException("Migration interrupted", e);
        } catch (ExecutionException e) {
            throw new MigrationException("Migration failed", e.getCause());
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
```

### Virtual Threads (Java 21+)

With Java 21+, use virtual threads for better resource efficiency:

```java
public class VirtualThreadMigrationService {

    private final AetherDataFixer fixer;
    private final Duration timeout;

    public VirtualThreadMigrationService(AetherDataFixer fixer, Duration timeout) {
        this.fixer = fixer;
        this.timeout = timeout;
    }

    public <T> TaggedDynamic<T> migrateWithTimeout(
            TaggedDynamic<T> input,
            DataVersion from,
            DataVersion to) throws TimeoutException {

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<TaggedDynamic<T>> future = executor.submit(
                () -> fixer.update(input, from, to)
            );
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new MigrationTimeoutException("Migration timed out", e);
        } catch (Exception e) {
            throw new MigrationException("Migration failed", e);
        }
    }
}
```

---

## Memory Limits

Limit JVM memory to contain resource exhaustion attacks:

```bash
# Limit heap size
java -Xmx512m -Xms256m -jar application.jar

# Enable GC logging for monitoring
java -Xlog:gc*:file=gc.log:time -jar application.jar
```

### Monitoring Memory During Migration

```java
public class MemoryMonitor {

    private static final long WARNING_THRESHOLD = 0.8; // 80% of max heap

    public void checkMemoryBeforeMigration() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        if ((double) usedMemory / maxMemory > WARNING_THRESHOLD) {
            // Trigger GC and recheck
            System.gc();
            usedMemory = runtime.totalMemory() - runtime.freeMemory();

            if ((double) usedMemory / maxMemory > WARNING_THRESHOLD) {
                throw new InsufficientMemoryException(
                    "Insufficient memory for migration. Used: " +
                    usedMemory + "/" + maxMemory);
            }
        }
    }
}
```

---

## Sandboxing Strategies

For high-risk scenarios, isolate migration processing:

### Process Isolation

Run migrations in a separate process with limited privileges:

```java
public class ProcessIsolatedMigration {

    public String migrateInSandbox(String input, String bootstrapClass) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "-Xmx256m",
            "-cp", "migration-worker.jar",
            "com.example.MigrationWorker",
            bootstrapClass
        );

        pb.environment().put("JAVA_TOOL_OPTIONS", "");  // Clear environment
        pb.redirectErrorStream(true);

        Process process = pb.start();
        process.getOutputStream().write(input.getBytes());
        process.getOutputStream().close();

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new TimeoutException("Migration process timed out");
        }

        return new String(process.getInputStream().readAllBytes());
    }
}
```

### Container Isolation

Use container limits for production:

```yaml
# docker-compose.yml
services:
  migration-worker:
    image: migration-service
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '0.5'
    security_opt:
      - no-new-privileges:true
    read_only: true
```

---

## Defense-in-Depth Checklist

Before processing untrusted data, verify:

### Input Validation
- [ ] Input size is checked before parsing
- [ ] Content-Type header matches expected format
- [ ] Input encoding is validated (UTF-8)

### Parser Configuration
- [ ] YAML: Using `SafeConstructor`
- [ ] YAML: Alias limit configured (`maxAliasesForCollections`)
- [ ] XML: External entities disabled
- [ ] XML: DTD processing disabled
- [ ] Jackson: Default typing is NOT enabled
- [ ] All: Nesting depth limits configured

### Resource Limits
- [ ] Timeout configured for migration operations
- [ ] Memory limits set on JVM/container
- [ ] Rate limiting applied for user requests

### Monitoring
- [ ] Failed migrations are logged
- [ ] Large payloads trigger alerts
- [ ] Timeout events are tracked
- [ ] Memory usage is monitored

### Error Handling
- [ ] Errors don't expose internal details
- [ ] Stack traces are not sent to clients
- [ ] Sensitive data is not logged

---

## Logging Security Events

Log security-relevant events for monitoring:

```java
public class SecureMigrationService {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("SECURITY");

    public <T> TaggedDynamic<T> migrate(TaggedDynamic<T> input, DataVersion from, DataVersion to) {
        long startTime = System.currentTimeMillis();

        try {
            TaggedDynamic<T> result = fixer.update(input, from, to);
            SECURITY_LOG.info("Migration success: type={}, from={}, to={}, duration={}ms",
                input.type().id(), from.version(), to.version(),
                System.currentTimeMillis() - startTime);
            return result;
        } catch (SecurityException e) {
            SECURITY_LOG.warn("Migration blocked: type={}, reason={}",
                input.type().id(), e.getMessage());
            throw e;
        } catch (Exception e) {
            SECURITY_LOG.error("Migration failed: type={}, error={}",
                input.type().id(), e.getMessage());
            throw e;
        }
    }
}
```

---

## Complete Secure Migration Service

Combining all best practices:

```java
public class SecureMigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(SecureMigrationService.class);
    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final int MAX_DEPTH = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final AetherDataFixer fixer;
    private final ExecutorService executor;

    public SecureMigrationService(AetherDataFixer fixer) {
        this.fixer = fixer;
        this.executor = Executors.newCachedThreadPool();
    }

    public <T> TaggedDynamic<T> migrateSecurely(
            byte[] untrustedInput,
            DynamicOps<T> ops,
            TypeReference type,
            DataVersion from,
            DataVersion to) {

        // 1. Size validation
        if (untrustedInput.length > MAX_SIZE) {
            throw new PayloadTooLargeException("Input exceeds " + MAX_SIZE + " bytes");
        }

        // 2. Parse with safe configuration (format-specific)
        T parsed = parseSecurely(untrustedInput, ops);

        // 3. Depth validation
        Dynamic<T> dynamic = new Dynamic<>(ops, parsed);
        validateDepth(dynamic, 0);

        // 4. Migrate with timeout
        TaggedDynamic<T> tagged = new TaggedDynamic<>(type, dynamic);
        return migrateWithTimeout(tagged, from, to);
    }

    private <T> T parseSecurely(byte[] input, DynamicOps<T> ops) {
        // Implementation depends on ops type
        // See format-specific guides
        throw new UnsupportedOperationException("Implement for specific ops");
    }

    private void validateDepth(Dynamic<?> dynamic, int depth) {
        if (depth > MAX_DEPTH) {
            throw new SecurityException("Exceeds max depth");
        }
        dynamic.getMap().result().ifPresent(map ->
            map.values().forEach(v -> validateDepth(v, depth + 1)));
        dynamic.getList().result().ifPresent(list ->
            list.forEach(e -> validateDepth(e, depth + 1)));
    }

    private <T> TaggedDynamic<T> migrateWithTimeout(
            TaggedDynamic<T> input, DataVersion from, DataVersion to) {
        Future<TaggedDynamic<T>> future = executor.submit(
            () -> fixer.update(input, from, to));
        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new MigrationTimeoutException("Timeout", e);
        } catch (Exception e) {
            throw new MigrationException("Failed", e);
        }
    }
}
```

---

## Related

- [Threat Model](threat-model.md)
- [Format-Specific Security](format-considerations/index.md)
- [Secure Configuration Examples](secure-configuration-examples.md)
- [Spring Security Integration](spring-security-integration.md)
