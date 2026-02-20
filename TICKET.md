# [Feature]: Experimental Java Object support in DynamicOps (`createObject` / `getObjectValue`)

**Labels:** `enhancement`, `experimental`, `api`, `codec`
**Priority:** medium
**Related Module:** api, codec
**Breaking Change:** no
**Target Version:** 1.1.0

---

## Problem / Motivation

The `DynamicOps<T>` interface provides symmetric create/read method pairs for all primitive types:

| Create | Read (DynamicOps) | Read (Dynamic) |
|---|---|---|
| `createString(String)` | `getStringValue(T)` | `asString()` |
| `createInt(int)` | `getNumberValue(T)` | `asInt()` |
| `createBoolean(boolean)` | `getBooleanValue(T)` | `asBoolean()` |
| **`createObject(Object)`** | **missing** | **missing** |

There is currently no way to pass an arbitrary Java `Object` through the `DynamicOps` abstraction layer and retrieve it on the other side. This limits special use cases where a `DynamicOps` implementation works directly with plain Java objects (e.g., `TestOps`, in-memory pipelines, or custom object-graph-based implementations) and needs to carry opaque values through the `Dynamic` wrapper without format-specific serialization.

Without this feature, users are forced to work around the type system by casting or storing objects outside the `Dynamic` pipeline, breaking the format-agnostic abstraction.

## Proposed Solution

Add a symmetric `createObject` / `getObjectValue` pair to `DynamicOps<T>` and corresponding wrapper methods to `Dynamic<T>`:

### DynamicOps (Interface Defaults)

- **`createObject(Object value)`** — Creates a format-specific representation of an arbitrary Java object. Default throws `UnsupportedOperationException`.
- **`getObjectValue(T input)`** — Extracts an arbitrary Java object from a format-specific value. Default returns `DataResult.error(...)`.

### Dynamic (Wrapper Methods)

- **`createObject(Object value)`** — Delegates to `ops.createObject(value)`, returns a new `Dynamic<T>`.
- **`asObject()`** — Delegates to `ops.getObjectValue(value)`, returns `DataResult<Object>`.

### Codec Module (Security Block)

All codec implementations (`GsonOps`, `JacksonJsonOps`, `JacksonXmlOps`, `JacksonYamlOps`, `JacksonTomlOps`, `SnakeYamlOps`) override both methods to throw `SecurityException`. Arbitrary Java object serialization/deserialization is intentionally blocked for security reasons in format-bound implementations, as it could enable untrusted deserialization attacks.

### TestOps (Trivial Implementation)

Since `TestOps` operates on `Object` directly, both methods are trivially implemented:
- `createObject(Object)` returns the value as-is.
- `getObjectValue(Object)` returns `DataResult.success(input)`.

### Annotations

All new methods are annotated with:
- `@Deprecated` — Signals highly experimental status
- `@ApiStatus.Experimental` — JetBrains annotation for experimental API
- `@since 1.1.0`

## API / Design Sketch

```java
// === DynamicOps<T> — Interface Defaults ===

@Deprecated
@ApiStatus.Experimental
default @NotNull T createObject(@NotNull final Object value) {
    throw new UnsupportedOperationException(
        "createObject is not supported by this DynamicOps implementation"
    );
}

@Deprecated
@ApiStatus.Experimental
default @NotNull DataResult<Object> getObjectValue(@NotNull final T input) {
    return DataResult.error(
        "getObjectValue is not supported by this DynamicOps implementation"
    );
}

// === Dynamic<T> — Wrapper Methods ===

@Deprecated
@ApiStatus.Experimental
public Dynamic<T> createObject(@NotNull final Object value) {
    return new Dynamic<>(this.ops, this.ops.createObject(value));
}

@Deprecated
@ApiStatus.Experimental
public DataResult<Object> asObject() {
    return this.ops.getObjectValue(this.value);
}

// === Codec Implementations (e.g., GsonOps) ===

@Deprecated
@Override
public JsonElement createObject(@NotNull final Object value) {
    throw new SecurityException(
        "createObject is not supported by GsonOps for security reasons"
    );
}

@Deprecated
@Override
public DataResult<Object> getObjectValue(@NotNull final JsonElement input) {
    throw new SecurityException(
        "getObjectValue is not supported by GsonOps for security reasons"
    );
}

// === TestOps — Trivial Pass-Through ===

@Override
public Object createObject(@NotNull final Object value) {
    return value;
}

@Override
public DataResult<Object> getObjectValue(@NotNull final Object input) {
    return DataResult.success(input);
}
```

## Alternatives / Workarounds

1. **Manual casting outside Dynamic:** Users can store objects in a side map and pass keys through `Dynamic`. This breaks the format-agnostic abstraction and adds bookkeeping complexity.
2. **Custom wrapper type in DynamicOps:** Users could implement a custom `DynamicOps` that wraps objects internally. This works but requires duplicating significant boilerplate without API support.
3. **Do nothing:** Leave the gap in the API. Users with special use cases would need to maintain their own fork or use unsafe casting patterns.

None of these alternatives provide a clean, framework-supported solution.

## Scope of Changes

### Files Modified

| File | Change |
|------|--------|
| `DynamicOps.java` | Add `createObject` and `getObjectValue` default methods |
| `Dynamic.java` | Add `createObject` and `asObject` wrapper methods |
| `TestOps.java` | Implement `createObject` and `getObjectValue` |
| `GsonOps.java` | Override with `SecurityException` |
| `JacksonJsonOps.java` | Override with `SecurityException` |
| `JacksonXmlOps.java` | Override with `SecurityException` |
| `JacksonYamlOps.java` | Override with `SecurityException` |
| `JacksonTomlOps.java` | Override with `SecurityException` |
| `SnakeYamlOps.java` | Override with `SecurityException` |

### Tests Added

| Test File | Tests |
|-----------|-------|
| `TestOpsTest.java` | `createObject` returns value directly, `getObjectValue` returns `DataResult.success` |
| `GsonOpsTest.java` | `createObject` throws `SecurityException`, `getObjectValue` throws `SecurityException` |
| `JacksonJsonOpsTest.java` | Same as above |
| `JacksonXmlOpsTest.java` | Same as above |
| `JacksonYamlOpsTest.java` | Same as above |
| `JacksonTomlOpsTest.java` | Same as above |
| `SnakeYamlOpsTest.java` | Same as above |

### Documentation Updated

| Document | Change |
|----------|--------|
| `docs/concepts/dynamic-system.md` | Added to interface listing, creating and reading sections |
| `docs/tutorials/custom-dynamicops.md` | Added to interface template |
| `docs/appendix/changelog.md` | Added Version 1.1.0 section |

## Checklist

- [x] I checked existing issues and discussions for duplicates.
- [x] This request is within the project's scope (not a support question).
