# Common Errors

Error messages and their solutions.

## SchemaNotFoundException

**Message:** `Schema not found for version X`

**Cause:** Requested a version that isn't registered.

**Solutions:**
1. Register the schema in your bootstrap
2. Check the version number is correct
3. Verify schemas are registered in order

```java
// Fix: Register the missing schema
schemas.register(new DataVersion(X), SchemaX::new);
```

## TypeNotFoundException

**Message:** `Type 'X' not found in schema version Y`

**Cause:** Type reference not registered in the schema.

**Solutions:**
1. Register the type in the schema
2. Check TypeReference ID spelling

```java
// Fix: Register the type
registerType(TypeReferences.PLAYER, DSL.and(...));
```

## MigrationException

**Message:** `Fix 'X' failed: ...`

**Cause:** Error during fix application.

**Solutions:**
1. Check the fix logic for errors
2. Handle missing/null fields with `orElse()`
3. Add logging to identify the issue

## EncodingException / DecodingException

**Message:** `Failed to encode/decode: ...`

**Cause:** Codec couldn't process the value.

**Solutions:**
1. Check field types match
2. Handle optional fields properly
3. Validate data before encoding

## IllegalArgumentException: Version already registered

**Message:** `Version X is already registered`

**Cause:** Duplicate schema registration.

**Solution:** Register each version only once

## NullPointerException in Dynamic

**Cause:** Accessing null field without handling.

**Solution:** Always use `orElse()`:
```java
// Bad
String name = data.get("name").asString().result().get();

// Good
String name = data.get("name").asString().orElse("default");
```

## ClassCastException

**Cause:** Wrong type assumption for a field.

**Solution:** Check actual type in data:
```java
// Debug the actual type
System.out.println(data.get("field").value().getClass());
```

## Related

- [Debugging Tips](debugging-tips.md)
- [FAQ](faq.md)

