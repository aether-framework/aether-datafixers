# Format Conversion

Converting data between different formats using DynamicOps.

## Concept

Since `Dynamic` is format-agnostic, you can convert between formats by re-encoding:

```java
// JSON to Jackson
Dynamic<JsonElement> gsonDynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
Dynamic<JsonNode> jacksonDynamic = gsonDynamic.convert(JacksonJsonOps.INSTANCE);
```

## JSON to Jackson

```java
public JsonNode gsonToJackson(JsonElement gson) {
    Dynamic<JsonElement> gsonDynamic = new Dynamic<>(GsonOps.INSTANCE, gson);
    Dynamic<JsonNode> jacksonDynamic = gsonDynamic.convert(JacksonJsonOps.INSTANCE);
    return jacksonDynamic.value();
}
```

## Jackson to Gson

```java
public JsonElement jacksonToGson(JsonNode jackson) {
    Dynamic<JsonNode> jacksonDynamic = new Dynamic<>(JacksonJsonOps.INSTANCE, jackson);
    Dynamic<JsonElement> gsonDynamic = jacksonDynamic.convert(GsonOps.INSTANCE);
    return gsonDynamic.value();
}
```

## Generic Conversion

```java
public <S, T> T convert(S source, DynamicOps<S> sourceOps, DynamicOps<T> targetOps) {
    Dynamic<S> sourceDynamic = new Dynamic<>(sourceOps, source);
    Dynamic<T> targetDynamic = sourceDynamic.convert(targetOps);
    return targetDynamic.value();
}
```

## Migrate and Convert

```java
public JsonNode migrateAndConvert(JsonElement input, int fromVersion) {
    // Parse with Gson
    Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, input);
    TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

    // Migrate
    TaggedDynamic migrated = fixer.update(tagged, new DataVersion(fromVersion), currentVersion);

    // Convert to Jackson
    return ((Dynamic<?>) migrated.value()).convert(JacksonJsonOps.INSTANCE).value();
}
```

## Custom Format Support

Implement `DynamicOps` for custom formats:

```java
public class YamlOps implements DynamicOps<Object> {
    public static final YamlOps INSTANCE = new YamlOps();

    @Override
    public Object createString(String value) {
        return value;
    }

    @Override
    public DataResult<String> getStringValue(Object input) {
        return input instanceof String s
            ? DataResult.success(s)
            : DataResult.error(() -> "Not a string");
    }

    // Implement other methods...
}
```

## Related

- [API Reference](https://software.splatgames.de/docs/aether/aether-datafixers/)
- [Custom DynamicOps](../tutorials/custom-dynamicops.md)

