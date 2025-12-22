# How to Convert Types

This guide shows how to change field types during migration (int to string, string to list, etc.).

## Int to String

```java
// gameMode: 0 → "survival"
return Rules.transformField(TypeReferences.PLAYER, "gameMode", value -> {
    int mode = value.asInt().orElse(0);
    String name = switch (mode) {
        case 0 -> "survival";
        case 1 -> "creative";
        case 2 -> "adventure";
        case 3 -> "spectator";
        default -> "survival";
    };
    return value.createString(name);
});
```

## String to Int

```java
// difficulty: "hard" → 3
return Rules.transformField(TypeReferences.CONFIG, "difficulty", value -> {
    String name = value.asString().orElse("normal").toLowerCase();
    int level = switch (name) {
        case "peaceful" -> 0;
        case "easy" -> 1;
        case "normal" -> 2;
        case "hard" -> 3;
        default -> 2;
    };
    return value.createInt(level);
});
```

## Int to Boolean

```java
// enabled: 1 → true, 0 → false
return Rules.transformField(TypeReferences.CONFIG, "enabled", value -> {
    int intVal = value.asInt().orElse(0);
    return value.createBoolean(intVal != 0);
});
```

## Boolean to Int

```java
// enabled: true → 1, false → 0
return Rules.transformField(TypeReferences.CONFIG, "enabled", value -> {
    boolean boolVal = value.asBoolean().orElse(false);
    return value.createInt(boolVal ? 1 : 0);
});
```

## String to Boolean

```java
// active: "yes"/"true"/"1" → true
return Rules.transformField(TypeReferences.PLAYER, "active", value -> {
    String str = value.asString().orElse("").toLowerCase();
    boolean active = str.equals("yes") || str.equals("true") || str.equals("1");
    return value.createBoolean(active);
});
```

## Float to Double

```java
return Rules.transformField(TypeReferences.PLAYER, "health", value -> {
    float floatVal = value.asFloat().orElse(0f);
    return value.createDouble(floatVal);
});
```

## String to Long (Timestamp)

```java
// date: "2024-01-15" → 1705276800000
return Rules.transformField(TypeReferences.PLAYER, "createdAt", value -> {
    String dateStr = value.asString().orElse("");
    try {
        LocalDate date = LocalDate.parse(dateStr);
        long timestamp = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        return value.createLong(timestamp);
    } catch (Exception e) {
        return value.createLong(0L);
    }
});
```

## Long to String (Timestamp)

```java
// timestamp: 1705276800000 → "2024-01-15"
return Rules.transformField(TypeReferences.PLAYER, "createdAt", value -> {
    long timestamp = value.asLong().orElse(0L);
    Instant instant = Instant.ofEpochMilli(timestamp);
    String dateStr = instant.atZone(ZoneOffset.UTC).toLocalDate().toString();
    return value.createString(dateStr);
});
```

## Scalar to List

```java
// tag: "important" → ["important"]
return Rules.transformField(TypeReferences.ITEM, "tags", value -> {
    String tag = value.asString().orElse("");
    if (tag.isEmpty()) {
        return value.emptyList();
    }
    return value.emptyList().append(value.createString(tag));
});
```

## List to Scalar (First Element)

```java
// tags: ["important", "urgent"] → "important"
return Rules.transformField(TypeReferences.ITEM, "tag", value -> {
    return value.asList().orElse(List.of()).stream()
        .findFirst()
        .map(d -> d.asString().result())
        .flatMap(o -> o)
        .map(value::createString)
        .orElse(value.createString(""));
});
```

## Comma-Separated String to List

```java
// tags: "a,b,c" → ["a", "b", "c"]
return Rules.transformField(TypeReferences.ITEM, "tags", value -> {
    String str = value.asString().orElse("");
    if (str.isEmpty()) {
        return value.emptyList();
    }

    Dynamic<?> list = value.emptyList();
    for (String tag : str.split(",")) {
        list = list.append(value.createString(tag.trim()));
    }
    return list;
});
```

## List to Comma-Separated String

```java
// tags: ["a", "b", "c"] → "a,b,c"
return Rules.transformField(TypeReferences.ITEM, "tags", value -> {
    String joined = value.asList().orElse(List.of()).stream()
        .map(d -> d.asString().orElse(""))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.joining(","));
    return value.createString(joined);
});
```

## Object to String (Serialization)

```java
// metadata: {"key": "value"} → "{\"key\":\"value\"}"
return Rules.transformField(TypeReferences.ITEM, "metadata", value -> {
    // This requires serializing the dynamic to a string
    // Implementation depends on your DynamicOps
    JsonElement json = ((Dynamic<JsonElement>) value).value();
    String serialized = new Gson().toJson(json);
    return value.createString(serialized);
});
```

## String to Object (Deserialization)

```java
// metadata: "{\"key\":\"value\"}" → {"key": "value"}
return Rules.transformField(TypeReferences.ITEM, "metadata", value -> {
    String json = value.asString().orElse("{}");
    try {
        JsonElement parsed = JsonParser.parseString(json);
        return new Dynamic<>(GsonOps.INSTANCE, parsed);
    } catch (Exception e) {
        return value.emptyMap();
    }
});
```

## Enum String Case Change

```java
// status: "ACTIVE" → "active"
return Rules.transformField(TypeReferences.PLAYER, "status", value -> {
    String status = value.asString().orElse("").toLowerCase();
    return value.createString(status);
});
```

## UUID String Normalization

```java
// uuid: "550e8400e29b41d4a716446655440000" → "550e8400-e29b-41d4-a716-446655440000"
return Rules.transformField(TypeReferences.PLAYER, "uuid", value -> {
    String raw = value.asString().orElse("").replaceAll("-", "");
    if (raw.length() == 32) {
        String formatted = raw.substring(0, 8) + "-" +
                          raw.substring(8, 12) + "-" +
                          raw.substring(12, 16) + "-" +
                          raw.substring(16, 20) + "-" +
                          raw.substring(20);
        return value.createString(formatted);
    }
    return value;
});
```

## Schema Definition

Remember to update schemas to reflect type changes:

```java
// Old: int
DSL.field("gameMode", DSL.intType())

// New: string
DSL.field("gameMode", DSL.string())
```

## Related

- [Transform Field Values](transform-field.md)
- [Restructure Data](restructure-data.md)
- [Handle Optional Fields](handle-optional-fields.md)

