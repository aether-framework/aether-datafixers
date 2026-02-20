# [Feature]: Experimental Java Object support via Capability Pattern (`ObjectAwareDynamicOps`)

**Labels:** `enhancement`, `experimental`, `api`
**Priority:** medium
**Related Module:** api
**Breaking Change:** no
**Target Version:** 1.1.0

---

## Problem / Motivation

The `DynamicOps<T>` interface provides symmetric create/read method pairs for all primitive types:

| Create                     | Read (DynamicOps)    | Read (Dynamic) |
|----------------------------|----------------------|----------------|
| `createString(String)`     | `getStringValue(T)`  | `asString()`   |
| `createInt(int)`           | `getNumberValue(T)`  | `asInt()`      |
| `createBoolean(boolean)`   | `getBooleanValue(T)` | `asBoolean()`  |
| **`createObject(Object)`** | **missing**          | **missing**    |

There is currently no way to pass an arbitrary Java `Object` through the `DynamicOps` abstraction layer and retrieve it on the other side. This limits special use cases where a `DynamicOps` implementation works directly with plain Java objects (e.g., `TestOps`, in-memory pipelines, or custom object-graph-based implementations) and needs to carry opaque values through the `Dynamic` wrapper without format-specific serialization.

Without this feature, users are forced to work around the type system by casting or storing objects outside the `Dynamic` pipeline, breaking the format-agnostic abstraction.

### Security Consideration

Placing `createObject`/`getObjectValue` directly on `DynamicOps<T>` as default methods is dangerous: uninformed users implementing custom `DynamicOps` might naively map these methods to their format library's object deserialization (e.g., Jackson's `ObjectMapper.treeToValue()`), potentially enabling Remote Code Execution (RCE) vulnerabilities.

## Proposed Solution

Use the **Capability Pattern** to provide `createObject`/`getObjectValue` through a separate sub-interface `ObjectAwareDynamicOps<T>`, discoverable at runtime via `DynamicOps.asObjectAware()`.

This ensures that:
- The methods are **not visible** on the base `DynamicOps<T>` interface
- Only implementations that **explicitly opt in** by implementing `ObjectAwareDynamicOps<T>` expose them
- Standard codec implementations (GsonOps, JacksonJsonOps, etc.) never see these methods
- No `SecurityException` overrides are needed in codec implementations

### ObjectAwareDynamicOps<T> (Sub-Interface)

- **`createObject(Object value)`** — Creates a format-specific representation of an arbitrary Java object.
- **`getObjectValue(T input)`** — Extracts an arbitrary Java object from a format-specific value. Returns `DataResult<Object>`.

### DynamicOps<T> (Capability Discovery)

- **`asObjectAware()`** — Uses `instanceof` to check if the implementation is an `ObjectAwareDynamicOps`. No override needed.
- **`requireObjectAware()`** — Throwing variant of `asObjectAware()`. Use when the capability is a hard requirement.

### Dynamic<T> (Wrapper Methods)

- **`createObject(Object value)`** — Delegates via `ops.requireObjectAware()`. Throws `UnsupportedOperationException` if the ops does not support it.
- **`asObject()`** — Delegates via `ops.asObjectAware()`. Returns `DataResult.error(...)` if the ops does not support it.

### TestOps (Trivial Implementation)

Since `TestOps` operates on `Object` directly, it implements `ObjectAwareDynamicOps<Object>` with trivial pass-through:
- `createObject(Object)` returns the value as-is.
- `getObjectValue(Object)` returns `DataResult.success(input)`.

### Annotations

All new methods are annotated with:
- `@ApiStatus.Experimental` — JetBrains annotation for experimental API
- `@since 1.1.0`

## API / Design Sketch

```java
// === ObjectAwareDynamicOps<T> — Sub-Interface ===

@ApiStatus.Experimental
public interface ObjectAwareDynamicOps<T> extends DynamicOps<T> {

    @NotNull T createObject(@NotNull final Object value);

    @NotNull DataResult<Object> getObjectValue(@NotNull final T input);
}

// === DynamicOps<T> — Capability Discovery (instanceof-based) ===

@ApiStatus.Experimental
default Optional<ObjectAwareDynamicOps<T>> asObjectAware() {
    return (this instanceof ObjectAwareDynamicOps<T> objectOps)
            ? Optional.of(objectOps)
            : Optional.empty();
}

@ApiStatus.Experimental
default ObjectAwareDynamicOps<T> requireObjectAware() {
    return asObjectAware().orElseThrow(() ->
            new UnsupportedOperationException(
                    getClass().getSimpleName()
                    + " does not implement ObjectAwareDynamicOps. "
                    + "Use asObjectAware() to check for capability before calling this method, "
                    + "or ensure that the DynamicOps implementation supports Java object operations."
            ));
}

// === Dynamic<T> — Wrapper Methods ===

@ApiStatus.Experimental
public Dynamic<T> createObject(@NotNull final Object value) {
    Preconditions.checkNotNull(value, "value must not be null");
    return new Dynamic<>(this.ops, this.ops.requireObjectAware().createObject(value));
}

@ApiStatus.Experimental
public DataResult<Object> asObject() {
    return this.ops.asObjectAware()
            .map(oa -> oa.getObjectValue(this.value))
            .orElseGet(() -> DataResult.error(
                    "The DynamicOps implementation does not support Java object operations. "
                    + "Only ObjectAwareDynamicOps implementations support asObject()."
            ));
}

// === TestOps — Implements ObjectAwareDynamicOps<Object> ===

public class TestOps implements ObjectAwareDynamicOps<Object> {
    @Override
    public Object createObject(@NotNull final Object value) {
        return value;
    }

    @Override
    public DataResult<Object> getObjectValue(@NotNull final Object input) {
        return DataResult.success(input);
    }
}
```

## Alternatives / Workarounds

1. **Default methods on DynamicOps (rejected):** The original approach. Exposes the methods to all implementors, creating a risk that users naively implement them with unsafe deserialization. Required `SecurityException` overrides in all 6 codec implementations.
2. **Manual casting outside Dynamic:** Users can store objects in a side map and pass keys through `Dynamic`. This breaks the format-agnostic abstraction and adds bookkeeping complexity.
3. **Custom wrapper type in DynamicOps:** Users could implement a custom `DynamicOps` that wraps objects internally. This works but requires duplicating significant boilerplate without API support.
4. **Do nothing:** Leave the gap in the API. Users with special use cases would need to maintain their own fork or use unsafe casting patterns.

The Capability Pattern provides the cleanest solution: opt-in, discoverable, and secure by default.

## Scope of Changes

### Files Modified

| File                         | Change                                                                              |
|------------------------------|-------------------------------------------------------------------------------------|
| `ObjectAwareDynamicOps.java` | **New** — Sub-interface with `createObject` and `getObjectValue`                         |
| `DynamicOps.java`            | Added `asObjectAware()` capability discovery method                                 |
| `Dynamic.java`               | Added `createObject` and `asObject` wrapper methods (delegate via `asObjectAware()`) |
| `TestOps.java`               | Implements `ObjectAwareDynamicOps<Object>` with trivial pass-through                |

### Tests Added

| Test File                 | Tests                                                                            |
|---------------------------|----------------------------------------------------------------------------------|
| `TestOpsTest.java`        | `isObjectAwareDynamicOps`, `asObjectAware()` returns present, `createObject` returns value, `getObjectValue` returns success |
| `GsonOpsTest.java`        | `asObjectAware()` returns empty                                                  |
| `JacksonJsonOpsTest.java` | `asObjectAware()` returns empty                                                  |
| `JacksonXmlOpsTest.java`  | `asObjectAware()` returns empty                                                  |
| `JacksonYamlOpsTest.java` | `asObjectAware()` returns empty                                                  |
| `JacksonTomlOpsTest.java` | `asObjectAware()` returns empty                                                  |
| `SnakeYamlOpsTest.java`   | `asObjectAware()` returns empty                                                  |

### Documentation Updated

| Document                              | Change                                                              |
|---------------------------------------|---------------------------------------------------------------------|
| `docs/concepts/dynamic-system.md`     | Added `ObjectAwareDynamicOps` section, updated interface listing    |
| `docs/tutorials/custom-dynamicops.md` | Added `ObjectAwareDynamicOps` section, updated interface template   |
| `docs/appendix/changelog.md`          | Added Version 1.1.0 section                                        |
| `package-info.java`                   | Added `ObjectAwareDynamicOps` to Key Classes list                  |

## Checklist

- [x] I checked existing issues and discussions for duplicates.
- [x] This request is within the project's scope (not a support question).
