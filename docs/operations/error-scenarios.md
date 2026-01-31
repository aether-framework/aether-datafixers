# Error Scenarios

Detailed guide to handling exceptions in Aether Datafixers production environments.

## Exception Hierarchy Quick Reference

| Exception            | Context Fields                                         | Common Causes             |
|----------------------|--------------------------------------------------------|---------------------------|
| `DataFixerException` | `context`                                              | Base class for all errors |
| `FixException`       | `fixName`, `fromVersion`, `toVersion`, `typeReference` | Fix logic failure         |
| `DecodeException`    | `typeReference`, `path`                                | Invalid input data        |
| `EncodeException`    | `typeReference`, `failedValue`                         | Serialization failure     |
| `RegistryException`  | `missingType`, `missingVersion`                        | Missing registration      |

All exceptions extend `RuntimeException` (unchecked) and are immutable/thread-safe.

---

## FixException

Thrown when a DataFix fails to transform data from one version to another.

### Context Fields

| Field           | Accessor             | Description                 |
|-----------------|----------------------|-----------------------------|
| `fixName`       | `getFixName()`       | Name of the fix that failed |
| `fromVersion`   | `getFromVersion()`   | Source version of migration |
| `toVersion`     | `getToVersion()`     | Target version of migration |
| `typeReference` | `getTypeReference()` | Type being transformed      |

### Context String Format

```
fix=rename_player_name, version=100->200, type=player
```

### Common Causes

1. **Invalid input data** — Data doesn't match expected schema
2. **Missing required field** — Fix expects a field that doesn't exist
3. **Type mismatch** — Expected string but found number
4. **Rule application failure** — TypeRewriteRule failed to apply
5. **Null pointer** — Fix logic encountered null unexpectedly

### Resolution Steps

```java
try {
    Dynamic<?> result = fixer.update(typeRef, data, fromVersion, toVersion);
} catch (FixException e) {
    // 1. Log with full context
    logger.error("Migration failed: {} [{}]", e.getMessage(), e.getContext());

    // 2. Extract specific fields for analysis
    if (e.getFixName() != null) {
        logger.error("  Fix: {}", e.getFixName());
    }
    if (e.getFromVersion() != null && e.getToVersion() != null) {
        logger.error("  Version: {} -> {}",
            e.getFromVersion().getVersion(),
            e.getToVersion().getVersion());
    }
    if (e.getTypeReference() != null) {
        logger.error("  Type: {}", e.getTypeReference().getId());
    }

    // 3. Check root cause
    if (e.getCause() != null) {
        logger.error("  Root cause: {}", e.getCause().getMessage());
    }
}
```

### Diagnostic Integration

```java
// Use DiagnosticContext to capture snapshots
DiagnosticContext ctx = DiagnosticContext.create(
    DiagnosticOptions.builder()
        .captureSnapshots(true)
        .build()
);

try {
    fixer.update(typeRef, data, fromVersion, toVersion, ctx);
} catch (FixException e) {
    MigrationReport report = ctx.getReport();

    // Find which fix ran last (the one that failed)
    List<FixExecution> fixes = report.fixExecutions();
    if (!fixes.isEmpty()) {
        FixExecution lastFix = fixes.get(fixes.size() - 1);
        logger.error("Last fix before failure: {}", lastFix.fixName());
        lastFix.beforeSnapshotOpt().ifPresent(snap ->
            logger.error("Data before fix: {}", snap));
    }
}
```

---

## DecodeException

Thrown when deserialization from Dynamic to typed Java object fails.

### Context Fields

| Field           | Accessor             | Description                               |
|-----------------|----------------------|-------------------------------------------|
| `typeReference` | `getTypeReference()` | Type being decoded                        |
| `path`          | `getPath()`          | Location in data structure (dot notation) |

### Context String Format

```
type=player, path=inventory[0].item.name
```

### Path Notation

The path uses dot notation with array indices:
- `player.name` — Field `name` in object `player`
- `inventory[0]` — First element of array `inventory`
- `inventory[0].item.damage` — Nested field access

### Common Causes

1. **Missing required field** — Schema expects field that doesn't exist
2. **Invalid field type** — Expected number, got string
3. **Malformed data** — Corrupt or truncated input
4. **Schema mismatch** — Data version doesn't match expected schema
5. **Null value** — Non-nullable field is null

### Resolution Steps

```java
try {
    Typed<Player> typed = fixer.decode(version, typeRef, dynamic);
} catch (DecodeException e) {
    logger.error("Decode failed: {} [{}]", e.getMessage(), e.getContext());

    // Path tells you exactly where the problem is
    if (e.getPath() != null) {
        logger.error("Problem location: {}", e.getPath());

        // Navigate to the problematic field
        String[] pathParts = e.getPath().split("\\.");
        Dynamic<?> current = dynamic;
        for (String part : pathParts) {
            if (part.contains("[")) {
                // Array access
                String fieldName = part.substring(0, part.indexOf('['));
                int index = Integer.parseInt(
                    part.substring(part.indexOf('[') + 1, part.indexOf(']')));
                current = current.get(fieldName).get(index);
            } else {
                current = current.get(part);
            }
            logger.debug("  {} = {}", part, current.getValue());
        }
    }
}
```

### Data Inspection

```java
// Inspect the raw data at the failing path
DecodeException e = ...;
if (e.getPath() != null && e.getPath().contains(".")) {
    String parentPath = e.getPath().substring(0, e.getPath().lastIndexOf('.'));
    String fieldName = e.getPath().substring(e.getPath().lastIndexOf('.') + 1);

    logger.error("Parent object fields at '{}': {}", parentPath,
        navigateTo(dynamic, parentPath).asMap().keySet());
}
```

---

## EncodeException

Thrown when serialization from Java object to Dynamic representation fails.

### Context Fields

| Field           | Accessor             | Description                 |
|-----------------|----------------------|-----------------------------|
| `typeReference` | `getTypeReference()` | Type being encoded          |
| `failedValue`   | `getFailedValue()`   | Value that failed to encode |

### Context String Format

```
type=player
```

### Common Causes

1. **Null value** — Required field is null
2. **Unsupported type** — Codec doesn't support the value type
3. **Codec misconfiguration** — Encoder not properly set up
4. **Circular reference** — Object graph contains cycles

### Resolution Steps

```java
try {
    Dynamic<?> encoded = fixer.encode(version, typeRef, value, ops);
} catch (EncodeException e) {
    logger.error("Encode failed: {} [{}]", e.getMessage(), e.getContext());

    // Inspect the failed value (be careful with sensitive data)
    if (e.getFailedValue() != null) {
        logger.error("Failed value class: {}", e.getFailedValue().getClass().getName());
        // Only log non-sensitive values
        if (isSafeToLog(e.getFailedValue())) {
            logger.error("Failed value: {}", e.getFailedValue());
        }
    }

    // Check if it's a null issue
    if (e.getCause() instanceof NullPointerException) {
        logger.error("Null value encountered - check required fields");
    }
}
```

### Sensitive Data Warning

The `failedValue` may contain sensitive information (passwords, tokens, PII). Always sanitize before logging:

```java
private boolean isSafeToLog(Object value) {
    // Don't log objects that might contain sensitive data
    if (value instanceof String) {
        String str = (String) value;
        return str.length() < 100 && !str.toLowerCase().contains("password");
    }
    return value instanceof Number || value instanceof Boolean;
}
```

---

## RegistryException

Thrown when a registry lookup fails (type, schema, or codec not found).

### Context Fields

| Field            | Accessor              | Description             |
|------------------|-----------------------|-------------------------|
| `missingType`    | `getMissingType()`    | TypeReference not found |
| `missingVersion` | `getMissingVersion()` | DataVersion not found   |

### Context String Format

```
type=custom_entity, version=150
```

### Common Causes

1. **Type not registered** — Forgot to register type in bootstrap
2. **Schema not registered** — Version not registered in SchemaRegistry
3. **Version gap** — No schema exists for intermediate version
4. **Typo in TypeReference** — Type ID doesn't match registration

### Resolution Steps

```java
try {
    Schema schema = schemaRegistry.require(version);
} catch (RegistryException e) {
    logger.error("Registry lookup failed: {} [{}]", e.getMessage(), e.getContext());

    if (e.getMissingVersion() != null) {
        logger.error("Missing schema for version: {}",
            e.getMissingVersion().getVersion());

        // List available versions
        logger.info("Available versions: {}",
            schemaRegistry.getVersions().stream()
                .map(v -> String.valueOf(v.getVersion()))
                .collect(Collectors.joining(", ")));
    }

    if (e.getMissingType() != null) {
        logger.error("Missing type: {}", e.getMissingType().getId());

        // List registered types (at current version if available)
        logger.info("Registered types: {}",
            typeRegistry.getRegisteredTypes().stream()
                .map(TypeReference::getId)
                .collect(Collectors.joining(", ")));
    }
}
```

### Bootstrap Verification Checklist

When encountering RegistryException:

- [ ] Check `registerSchemas()` includes the required version
- [ ] Check type is registered in the schema for that version
- [ ] Verify no gaps in version chain (e.g., 100 -> 200 needs fixes, not just schemas)
- [ ] Check for typos in TypeReference IDs
- [ ] Verify bootstrap is loaded (not null)

---

## Schema Mismatch Scenarios

### Data Version Doesn't Match Expected

**Symptom**: Migration produces unexpected results or fails silently.

**Detection**:

```java
// Check data version before migration
Optional<Integer> dataVersion = dynamic.get("_version").asNumber()
    .map(Number::intValue);

if (dataVersion.isEmpty()) {
    logger.warn("Data has no version field - assuming oldest version");
}

int fromVersion = dataVersion.orElse(OLDEST_VERSION);
if (fromVersion > currentVersion.getVersion()) {
    throw new IllegalStateException(
        "Data version " + fromVersion + " is newer than current " + currentVersion);
}
```

### Type Structure Changed Without Fix

**Symptom**: Fields missing or have wrong type after migration.

**Detection**:

```java
// Use SchemaValidator to detect coverage gaps
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateFixCoverage()
    .validate();

if (!result.isValid()) {
    for (String error : result.getErrors()) {
        logger.error("Schema validation error: {}", error);
    }
}
```

**Resolution**: Write a DataFix to handle the schema change.

---

## Extracting Exception Context

### Complete Context Extraction

```java
public class ExceptionAnalyzer {

    public static void logException(DataFixerException e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Exception: ").append(e.getClass().getSimpleName()).append("\n");
        sb.append("Message: ").append(e.getMessage()).append("\n");

        if (e.getContext() != null) {
            sb.append("Context: ").append(e.getContext()).append("\n");
        }

        // Type-specific extraction
        if (e instanceof FixException fix) {
            if (fix.getFixName() != null) {
                sb.append("Fix Name: ").append(fix.getFixName()).append("\n");
            }
            if (fix.getFromVersion() != null) {
                sb.append("From Version: ").append(fix.getFromVersion().getVersion()).append("\n");
            }
            if (fix.getToVersion() != null) {
                sb.append("To Version: ").append(fix.getToVersion().getVersion()).append("\n");
            }
            if (fix.getTypeReference() != null) {
                sb.append("Type: ").append(fix.getTypeReference().getId()).append("\n");
            }
        } else if (e instanceof DecodeException decode) {
            if (decode.getTypeReference() != null) {
                sb.append("Type: ").append(decode.getTypeReference().getId()).append("\n");
            }
            if (decode.getPath() != null) {
                sb.append("Path: ").append(decode.getPath()).append("\n");
            }
        } else if (e instanceof EncodeException encode) {
            if (encode.getTypeReference() != null) {
                sb.append("Type: ").append(encode.getTypeReference().getId()).append("\n");
            }
            // Be careful with failedValue - may contain sensitive data
        } else if (e instanceof RegistryException registry) {
            if (registry.getMissingType() != null) {
                sb.append("Missing Type: ").append(registry.getMissingType().getId()).append("\n");
            }
            if (registry.getMissingVersion() != null) {
                sb.append("Missing Version: ").append(registry.getMissingVersion().getVersion()).append("\n");
            }
        }

        // Root cause chain
        Throwable cause = e.getCause();
        int depth = 0;
        while (cause != null && depth < 5) {
            sb.append("Caused by: ").append(cause.getClass().getSimpleName())
              .append(": ").append(cause.getMessage()).append("\n");
            cause = cause.getCause();
            depth++;
        }

        System.err.println(sb);
    }
}
```

### Logging Pattern for Production

```xml
<!-- logback.xml pattern for structured exception logging -->
<pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n%ex{full}</pattern>
```

```java
// Structured logging with MDC
import org.slf4j.MDC;

try {
    fixer.update(typeRef, data, fromVersion, toVersion);
} catch (FixException e) {
    MDC.put("fix_name", e.getFixName());
    MDC.put("from_version", String.valueOf(e.getFromVersion()));
    MDC.put("to_version", String.valueOf(e.getToVersion()));
    MDC.put("type", e.getTypeReference() != null ? e.getTypeReference().getId() : "unknown");

    logger.error("Migration failed", e);

    MDC.clear();
}
```

---

## Related

- [Debugging Guide](debugging-guide.md) — Systematic debugging approach
- [Recovery Procedures](recovery-procedures.md) — How to recover from failures
- [Common Errors](../troubleshooting/common-errors.md) — Quick error reference
- [How to Use Diagnostics](../how-to/use-diagnostics.md) — Diagnostic API reference
