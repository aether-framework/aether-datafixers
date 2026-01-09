# Custom DynamicOps Tutorial

This tutorial shows how to implement a custom `DynamicOps` to support a new data format. This allows Aether Datafixers to work with any serialization format.

## Goal

Learn how to:
- Understand the `DynamicOps` interface
- Implement essential operations
- Create and test a custom implementation
- Use your format with the framework

## Prerequisites

- Strong understanding of the [Dynamic System](../concepts/dynamic-system.md)
- Familiarity with your target data format

## The DynamicOps Interface

`DynamicOps<T>` defines operations for a data format where `T` is the underlying data type:

```java
public interface DynamicOps<T> {
    // === Value Creation ===
    T empty();
    T emptyMap();
    T emptyList();

    T createString(String value);
    T createInt(int value);
    T createLong(long value);
    T createFloat(float value);
    T createDouble(double value);
    T createBoolean(boolean value);

    T createList(Stream<T> values);
    T createMap(Map<T, T> map);

    // === Value Extraction ===
    DataResult<String> getStringValue(T input);
    DataResult<Number> getNumberValue(T input);
    DataResult<Boolean> getBooleanValue(T input);

    DataResult<Stream<T>> getStream(T input);
    DataResult<Map<T, T>> getMapValues(T input);

    // === Structure Operations ===
    DataResult<T> mergeToMap(T map, T key, T value);
    DataResult<T> mergeToList(T list, T value);

    T remove(T input, String key);

    // ... and more
}
```

## Example: YAML-like Map-Based Ops

Let's implement `SimpleMapOps` that works with `Map<String, Object>`:

### Step 1: Basic Structure

```java
package com.example.ops;

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;

import java.util.*;
import java.util.stream.Stream;

public class SimpleMapOps implements DynamicOps<Object> {

    public static final SimpleMapOps INSTANCE = new SimpleMapOps();

    private SimpleMapOps() {}
}
```

### Step 2: Empty Values

```java
@Override
public Object empty() {
    return null;
}

@Override
public Object emptyMap() {
    return new LinkedHashMap<String, Object>();
}

@Override
public Object emptyList() {
    return new ArrayList<Object>();
}
```

### Step 3: Value Creation

```java
@Override
public Object createString(String value) {
    return value;
}

@Override
public Object createInt(int value) {
    return value;
}

@Override
public Object createLong(long value) {
    return value;
}

@Override
public Object createFloat(float value) {
    return value;
}

@Override
public Object createDouble(double value) {
    return value;
}

@Override
public Object createBoolean(boolean value) {
    return value;
}

@Override
public Object createList(Stream<Object> values) {
    return values.toList();
}

@Override
public Object createMap(Map<Object, Object> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    map.forEach((k, v) -> {
        if (k instanceof String key) {
            result.put(key, v);
        }
    });
    return result;
}
```

### Step 4: Value Extraction

```java
@Override
public DataResult<String> getStringValue(Object input) {
    if (input instanceof String s) {
        return DataResult.success(s);
    }
    return DataResult.error("Not a string: " + input);
}

@Override
public DataResult<Number> getNumberValue(Object input) {
    if (input instanceof Number n) {
        return DataResult.success(n);
    }
    return DataResult.error("Not a number: " + input);
}

@Override
public DataResult<Boolean> getBooleanValue(Object input) {
    if (input instanceof Boolean b) {
        return DataResult.success(b);
    }
    return DataResult.error("Not a boolean: " + input);
}

@Override
@SuppressWarnings("unchecked")
public DataResult<Stream<Object>> getStream(Object input) {
    if (input instanceof List<?> list) {
        return DataResult.success(((List<Object>) list).stream());
    }
    return DataResult.error("Not a list: " + input);
}

@Override
@SuppressWarnings("unchecked")
public DataResult<Map<Object, Object>> getMapValues(Object input) {
    if (input instanceof Map<?, ?> map) {
        Map<Object, Object> result = new LinkedHashMap<>();
        ((Map<String, Object>) map).forEach((k, v) -> result.put(k, v));
        return DataResult.success(result);
    }
    return DataResult.error("Not a map: " + input);
}
```

### Step 5: Structure Operations

```java
@Override
@SuppressWarnings("unchecked")
public DataResult<Object> mergeToMap(Object map, Object key, Object value) {
    if (!(map instanceof Map)) {
        map = new LinkedHashMap<String, Object>();
    }
    if (!(key instanceof String keyStr)) {
        return DataResult.error("Key must be string: " + key);
    }

    Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) map);
    result.put(keyStr, value);
    return DataResult.success(result);
}

@Override
@SuppressWarnings("unchecked")
public DataResult<Object> mergeToList(Object list, Object value) {
    List<Object> result;
    if (list instanceof List<?> existing) {
        result = new ArrayList<>((List<Object>) existing);
    } else {
        result = new ArrayList<>();
    }
    result.add(value);
    return DataResult.success(result);
}

@Override
@SuppressWarnings("unchecked")
public Object remove(Object input, String key) {
    if (input instanceof Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) map);
        result.remove(key);
        return result;
    }
    return input;
}
```

### Step 6: Map Access

```java
@Override
@SuppressWarnings("unchecked")
public DataResult<Object> get(Object input, String key) {
    if (input instanceof Map<?, ?> map) {
        Object value = ((Map<String, Object>) map).get(key);
        if (value != null) {
            return DataResult.success(value);
        }
        return DataResult.error("Key not found: " + key);
    }
    return DataResult.error("Not a map: " + input);
}

@Override
public DataResult<MapLike<Object>> getMap(Object input) {
    if (input instanceof Map<?, ?>) {
        return DataResult.success(new MapLikeImpl(input));
    }
    return DataResult.error("Not a map: " + input);
}

// MapLike implementation
private class MapLikeImpl implements MapLike<Object> {
    private final Map<String, Object> map;

    @SuppressWarnings("unchecked")
    MapLikeImpl(Object input) {
        this.map = (Map<String, Object>) input;
    }

    @Override
    public Object get(Object key) {
        if (key instanceof String s) {
            return map.get(s);
        }
        return null;
    }

    @Override
    public Object get(String key) {
        return map.get(key);
    }

    @Override
    public Stream<Pair<Object, Object>> entries() {
        return map.entrySet().stream()
            .map(e -> Pair.of(e.getKey(), e.getValue()));
    }
}
```

## Complete Implementation

```java
package com.example.ops;

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.dynamic.MapLike;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;

import java.util.*;
import java.util.stream.Stream;

public class SimpleMapOps implements DynamicOps<Object> {

    public static final SimpleMapOps INSTANCE = new SimpleMapOps();

    private SimpleMapOps() {}

    // === Empty Values ===

    @Override
    public Object empty() {
        return null;
    }

    @Override
    public Object emptyMap() {
        return new LinkedHashMap<String, Object>();
    }

    @Override
    public Object emptyList() {
        return new ArrayList<Object>();
    }

    // === Value Creation ===

    @Override
    public Object createString(String value) {
        return value;
    }

    @Override
    public Object createInt(int value) {
        return value;
    }

    @Override
    public Object createLong(long value) {
        return value;
    }

    @Override
    public Object createFloat(float value) {
        return value;
    }

    @Override
    public Object createDouble(double value) {
        return value;
    }

    @Override
    public Object createBoolean(boolean value) {
        return value;
    }

    @Override
    public Object createList(Stream<Object> values) {
        return values.toList();
    }

    @Override
    public Object createMap(Map<Object, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (k instanceof String key) {
                result.put(key, v);
            }
        });
        return result;
    }

    // === Value Extraction ===

    @Override
    public DataResult<String> getStringValue(Object input) {
        if (input instanceof String s) {
            return DataResult.success(s);
        }
        return DataResult.error("Not a string: " + input);
    }

    @Override
    public DataResult<Number> getNumberValue(Object input) {
        if (input instanceof Number n) {
            return DataResult.success(n);
        }
        return DataResult.error("Not a number: " + input);
    }

    @Override
    public DataResult<Boolean> getBooleanValue(Object input) {
        if (input instanceof Boolean b) {
            return DataResult.success(b);
        }
        return DataResult.error("Not a boolean: " + input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Stream<Object>> getStream(Object input) {
        if (input instanceof List<?> list) {
            return DataResult.success(((List<Object>) list).stream());
        }
        return DataResult.error("Not a list: " + input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Map<Object, Object>> getMapValues(Object input) {
        if (input instanceof Map<?, ?> map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            ((Map<String, Object>) map).forEach((k, v) -> result.put(k, v));
            return DataResult.success(result);
        }
        return DataResult.error("Not a map: " + input);
    }

    // === Structure Operations ===

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Object> mergeToMap(Object map, Object key, Object value) {
        if (!(map instanceof Map)) {
            map = new LinkedHashMap<String, Object>();
        }
        if (!(key instanceof String keyStr)) {
            return DataResult.error("Key must be string: " + key);
        }

        Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) map);
        result.put(keyStr, value);
        return DataResult.success(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Object> mergeToList(Object list, Object value) {
        List<Object> result;
        if (list instanceof List<?> existing) {
            result = new ArrayList<>((List<Object>) existing);
        } else {
            result = new ArrayList<>();
        }
        result.add(value);
        return DataResult.success(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object remove(Object input, String key) {
        if (input instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) map);
            result.remove(key);
            return result;
        }
        return input;
    }
}
```

## Using Your Custom Ops

```java
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;

public class CustomOpsExample {

    public static void main(String[] args) {
        // Create data using your ops
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "Steve");
        data.put("level", 42);

        // Wrap in Dynamic
        Dynamic<Object> dynamic = new Dynamic<>(SimpleMapOps.INSTANCE, data);

        // Use Dynamic operations
        String name = dynamic.get("name").asString().orElse("Unknown");
        int level = dynamic.get("level").asInt().orElse(0);

        System.out.println("Name: " + name);   // Steve
        System.out.println("Level: " + level); // 42

        // Modify data
        Dynamic<Object> updated = dynamic
            .set("name", dynamic.createString("Alex"))
            .set("experience", dynamic.createInt(1000));

        System.out.println("Updated: " + updated.value());
        // {name=Alex, level=42, experience=1000}
    }
}
```

## Testing Your Implementation

```java
@Test
void testBasicOperations() {
    SimpleMapOps ops = SimpleMapOps.INSTANCE;

    // Test creation
    assertEquals("hello", ops.createString("hello"));
    assertEquals(42, ops.createInt(42));
    assertEquals(true, ops.createBoolean(true));

    // Test extraction
    assertEquals("hello", ops.getStringValue("hello").result().orElse(null));
    assertEquals(42, ops.getNumberValue(42).result().map(Number::intValue).orElse(null));
}

@Test
void testMapOperations() {
    SimpleMapOps ops = SimpleMapOps.INSTANCE;

    Object map = ops.emptyMap();
    map = ops.mergeToMap(map, "key", "value").result().orElseThrow();

    assertEquals("value", ops.get(map, "key").result().orElse(null));
}

@Test
void testDynamicIntegration() {
    Map<String, Object> data = Map.of("name", "Steve", "level", 10);
    Dynamic<Object> dynamic = new Dynamic<>(SimpleMapOps.INSTANCE, data);

    assertEquals("Steve", dynamic.get("name").asString().orElse(""));
    assertEquals(10, dynamic.get("level").asInt().orElse(0));
}
```

## Best Practices

### 1. Use Immutable Operations

```java
// Good: Create new collections
Map<String, Object> result = new LinkedHashMap<>(original);
result.put(key, value);
return result;

// Bad: Modify in place
original.put(key, value);
return original;
```

### 2. Handle Null Safely

```java
@Override
public Object empty() {
    return null;  // or a sentinel value
}

@Override
public DataResult<String> getStringValue(Object input) {
    if (input == null) {
        return DataResult.error("Input is null");
    }
    // ...
}
```

### 3. Provide Clear Error Messages

```java
return DataResult.error("Expected string but got " + input.getClass().getSimpleName() + ": " + input);
```

### 4. Make It Thread-Safe

```java
// Use thread-safe collections or ensure immutability
public static final SimpleMapOps INSTANCE = new SimpleMapOps();
```

## Next Steps

- **[API Reference](https://software.splatgames.de/docs/aether/aether-datafixers/)** — Full interface
- **[Dynamic System](../concepts/dynamic-system.md)** — How it all fits together

## Built-in Implementations

Before creating a custom implementation, check if one of the built-in implementations fits your needs:

| Format | Implementation | Package |
|--------|----------------|---------|
| JSON | `GsonOps` | `codec.json.gson` |
| JSON | `JacksonJsonOps` | `codec.json.jackson` |
| YAML | `SnakeYamlOps` | `codec.yaml.snakeyaml` |
| YAML | `JacksonYamlOps` | `codec.yaml.jackson` |
| TOML | `JacksonTomlOps` | `codec.toml.jackson` |
| XML | `JacksonXmlOps` | `codec.xml.jackson` |

See [Codec Module Documentation](../codec/index.md) for details.

