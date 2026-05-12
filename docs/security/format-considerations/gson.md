# Gson Security

Gson is a relatively safe JSON library with a minimal attack surface. It does not support polymorphic deserialization by default, making it less susceptible to the deserialization attacks that affect other libraries.

## Overview

| Risk                        | Severity | Mitigation                       |
|-----------------------------|----------|----------------------------------|
| Large Payload DoS           | Medium   | Pre-validate size before parsing |
| Deep Nesting Stack Overflow | Medium   | Validate nesting depth           |
| Custom TypeAdapter Risks    | Low      | Review custom adapters carefully |

## Safe by Default

Unlike Jackson, Gson does **not** support polymorphic deserialization by default:

```java
// This is SAFE - Gson doesn't instantiate arbitrary classes
Gson gson = new Gson();
MyClass obj = gson.fromJson(untrustedJson, MyClass.class);
```

Gson only deserializes to the explicitly specified type (`MyClass`), not types specified in the JSON payload.

## Potential Risks

### Large Payload DoS

Gson will attempt to parse any JSON regardless of size. Very large payloads can cause memory exhaustion:

```java
// No built-in size limits
Gson gson = new Gson();
// This will try to parse a 1GB JSON string
JsonElement element = JsonParser.parseString(hugeJson);  // Potential OOM
```

### Deep Nesting Stack Overflow

Deeply nested JSON can cause stack overflow during parsing:

```json
{"a":{"a":{"a":{"a":{"a":{"a":{"a":{"a":...}}}}}}}}}
```

---

## Secure Configuration

### Pre-Validation Before Parsing

Always validate input before parsing:

```java
public class SecureGsonParser {

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_DEPTH = 50;

    private final Gson gson;

    public SecureGsonParser() {
        this.gson = new GsonBuilder()
            .disableHtmlEscaping()  // Optional: for data migration
            .create();
    }

    public JsonElement parse(String json) {
        // Validate size
        if (json.length() > MAX_SIZE) {
            throw new SecurityException("JSON exceeds maximum size");
        }

        // Parse
        JsonElement element = JsonParser.parseString(json);

        // Validate depth
        validateDepth(element, 0);

        return element;
    }

    private void validateDepth(JsonElement element, int depth) {
        if (depth > MAX_DEPTH) {
            throw new SecurityException("JSON exceeds maximum nesting depth");
        }

        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                validateDepth(entry.getValue(), depth + 1);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                validateDepth(item, depth + 1);
            }
        }
    }
}
```

### Integration with GsonOps

```java
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;

public class SecureGsonMigration {

    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final int MAX_DEPTH = 50;

    public Dynamic<JsonElement> parseSecurely(String json) {
        // 1. Size validation
        if (json.length() > MAX_SIZE) {
            throw new SecurityException("JSON exceeds maximum size");
        }

        // 2. Parse
        JsonElement element = JsonParser.parseString(json);

        // 3. Depth validation
        validateDepth(element, 0);

        // 4. Wrap in Dynamic
        return new Dynamic<>(GsonOps.INSTANCE, element);
    }

    private void validateDepth(JsonElement element, int depth) {
        if (depth > MAX_DEPTH) {
            throw new SecurityException("JSON exceeds maximum nesting depth");
        }

        if (element.isJsonObject()) {
            element.getAsJsonObject().entrySet()
                .forEach(e -> validateDepth(e.getValue(), depth + 1));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray()
                .forEach(e -> validateDepth(e, depth + 1));
        }
    }
}
```

---

## Streaming Parser for Large Files

For very large files, use Gson's streaming API with validation:

```java
import com.google.gson.stream.JsonReader;

public class StreamingGsonParser {

    private static final int MAX_DEPTH = 50;
    private int currentDepth = 0;

    public void parseWithDepthLimit(Reader input) throws IOException {
        try (JsonReader reader = new JsonReader(input)) {
            parseValue(reader);
        }
    }

    private void parseValue(JsonReader reader) throws IOException {
        switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                checkDepth();
                currentDepth++;
                reader.beginObject();
                while (reader.hasNext()) {
                    reader.nextName();
                    parseValue(reader);
                }
                reader.endObject();
                currentDepth--;
            }
            case BEGIN_ARRAY -> {
                checkDepth();
                currentDepth++;
                reader.beginArray();
                while (reader.hasNext()) {
                    parseValue(reader);
                }
                reader.endArray();
                currentDepth--;
            }
            case STRING -> reader.nextString();
            case NUMBER -> reader.nextDouble();
            case BOOLEAN -> reader.nextBoolean();
            case NULL -> reader.nextNull();
            default -> throw new IllegalStateException("Unexpected token");
        }
    }

    private void checkDepth() {
        if (currentDepth >= MAX_DEPTH) {
            throw new SecurityException("Maximum nesting depth exceeded");
        }
    }
}
```

---

## Custom TypeAdapter Security

If you use custom `TypeAdapter` implementations, review them for security:

```java
// DANGEROUS - Deserializes arbitrary classes
public class UnsafeTypeAdapter extends TypeAdapter<Object> {
    @Override
    public Object read(JsonReader in) {
        String className = in.nextString();
        return Class.forName(className).newInstance(); // VULNERABLE!
    }
}

// SAFE - Only handles known types
public class SafeTypeAdapter extends TypeAdapter<MyClass> {
    @Override
    public MyClass read(JsonReader in) {
        // Only deserialize to MyClass, not arbitrary types
        return new MyClass(in.nextString());
    }
}
```

---

## Complete Secure Service

```java
public class SecureGsonMigrationService {

    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final int MAX_DEPTH = 50;

    private final AetherDataFixer fixer;
    private final Gson gson;

    public SecureGsonMigrationService(AetherDataFixer fixer) {
        this.fixer = fixer;
        this.gson = new GsonBuilder().create();
    }

    public TaggedDynamic<JsonElement> migrate(
            String untrustedJson,
            TypeReference type,
            DataVersion from,
            DataVersion to) {

        // Validate
        validateInput(untrustedJson);

        // Parse
        JsonElement element = JsonParser.parseString(untrustedJson);
        validateDepth(element, 0);

        // Migrate
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, element);
        TaggedDynamic<JsonElement> tagged = new TaggedDynamic<>(type, dynamic);
        return fixer.update(tagged, from, to);
    }

    private void validateInput(String json) {
        if (json == null || json.isEmpty()) {
            throw new IllegalArgumentException("JSON input cannot be null or empty");
        }
        if (json.length() > MAX_SIZE) {
            throw new SecurityException("JSON exceeds maximum size of " + MAX_SIZE + " bytes");
        }
    }

    private void validateDepth(JsonElement element, int depth) {
        if (depth > MAX_DEPTH) {
            throw new SecurityException("JSON exceeds maximum depth of " + MAX_DEPTH);
        }
        if (element.isJsonObject()) {
            element.getAsJsonObject().entrySet()
                .forEach(e -> validateDepth(e.getValue(), depth + 1));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray()
                .forEach(e -> validateDepth(e, depth + 1));
        }
    }
}
```

---

## Comparison with Jackson

| Feature                        | Gson                      | Jackson                         |
|--------------------------------|---------------------------|---------------------------------|
| Polymorphic Deserialization    | Not supported by default  | Opt-in (dangerous if enabled)   |
| Built-in Size Limits           | No                        | Yes (StreamReadConstraints)     |
| Built-in Depth Limits          | No                        | Yes (StreamReadConstraints)     |
| Attack Surface                 | Small                     | Larger                          |
| Recommended for Untrusted Data | Yes (with pre-validation) | Yes (with proper configuration) |

---

## Related

- [Threat Model](../threat-model.md)
- [Best Practices](../best-practices.md)
- [JSON Support](../../codec/json.md)
- [Secure Configuration Examples](../secure-configuration-examples.md)
