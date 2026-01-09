# Codec Module Overview

The `aether-datafixers-codec` module provides `DynamicOps` implementations for various data serialization formats. These implementations enable Aether Datafixers to work with JSON, YAML, TOML, and XML data.

## Supported Formats

| Format | Implementation | Underlying Library | Data Type |
|--------|----------------|-------------------|-----------|
| **JSON** | `GsonOps` | Google Gson | `JsonElement` |
| **JSON** | `JacksonJsonOps` | Jackson Databind | `JsonNode` |
| **YAML** | `SnakeYamlOps` | SnakeYAML | `Object` (Map/List) |
| **YAML** | `JacksonYamlOps` | Jackson YAML | `JsonNode` |
| **TOML** | `JacksonTomlOps` | Jackson TOML | `JsonNode` |
| **XML** | `JacksonXmlOps` | Jackson XML | `JsonNode` |

## Quick Start

### Add Dependency

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-codec</artifactId>
    <version>${aether.version}</version>
</dependency>
```

All format-specific dependencies are **optional**. Add only the libraries you need:

```xml
<!-- For GsonOps -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>

<!-- For JacksonJsonOps -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.2</version>
</dependency>

<!-- For SnakeYamlOps -->
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.3</version>
</dependency>

<!-- For JacksonYamlOps -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
    <version>2.18.2</version>
</dependency>

<!-- For JacksonTomlOps -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-toml</artifactId>
    <version>2.18.2</version>
</dependency>

<!-- For JacksonXmlOps -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
    <version>2.18.2</version>
</dependency>
```

### Basic Usage

```java
// JSON with Gson
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
Dynamic<JsonElement> jsonDynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);

// YAML with SnakeYAML
import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;
Dynamic<Object> yamlDynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, yamlData);

// TOML with Jackson
import de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps;
Dynamic<JsonNode> tomlDynamic = new Dynamic<>(JacksonTomlOps.INSTANCE, tomlNode);

// XML with Jackson
import de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps;
Dynamic<JsonNode> xmlDynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, xmlNode);
```

## Package Structure

```
de.splatgames.aether.datafixers.codec
├── json
│   ├── gson/
│   │   └── GsonOps.java
│   └── jackson/
│       └── JacksonJsonOps.java
├── yaml
│   ├── snakeyaml/
│   │   └── SnakeYamlOps.java
│   └── jackson/
│       └── JacksonYamlOps.java
├── toml
│   └── jackson/
│       └── JacksonTomlOps.java
└── xml
    └── jackson/
        └── JacksonXmlOps.java
```

## Choosing an Implementation

### JSON

| Feature | GsonOps | JacksonJsonOps |
|---------|---------|----------------|
| Library Size | ~300 KB | ~1.7 MB |
| Performance | Good | Excellent |
| Streaming | No | Yes |
| Custom Mappers | No | Yes |
| Tree Model | `JsonElement` | `JsonNode` |

**Recommendation:** Use `GsonOps` for simple use cases with minimal dependencies. Use `JacksonJsonOps` when you need maximum performance or already use Jackson.

### YAML

| Feature | SnakeYamlOps | JacksonYamlOps |
|---------|--------------|----------------|
| Library Size | ~300 KB | ~100 KB + Jackson |
| Native Types | Yes (Map/List) | No (JsonNode) |
| YAML Features | Full | Subset |
| Anchors/Aliases | Yes | Limited |
| Multi-Document | Yes | Limited |

**Recommendation:** Use `SnakeYamlOps` for full YAML feature support. Use `JacksonYamlOps` when you need consistent `JsonNode` handling across formats.

### TOML

| Feature | JacksonTomlOps |
|---------|----------------|
| TOML 1.0 | Yes |
| Date/Time | Via String |
| Inline Tables | Yes |
| Arrays of Tables | Yes |

**Recommendation:** `JacksonTomlOps` is the standard choice for TOML support.

### XML

| Feature | JacksonXmlOps |
|---------|---------------|
| Attributes | Via Conventions |
| Namespaces | Limited |
| Mixed Content | Limited |
| Tree Model | JsonNode |

**Recommendation:** `JacksonXmlOps` provides basic XML support. For complex XML with namespaces, consider a dedicated XML library.

## Thread Safety

All singleton instances (`INSTANCE`) are thread-safe and can be shared across threads:

```java
// Safe to use from multiple threads
GsonOps.INSTANCE
JacksonJsonOps.INSTANCE
SnakeYamlOps.INSTANCE
JacksonYamlOps.INSTANCE
JacksonTomlOps.INSTANCE
JacksonXmlOps.INSTANCE
```

Custom instances with custom mappers are thread-safe if the underlying mapper is thread-safe.

## Format Conversion

Convert data between formats using different `DynamicOps`:

```java
// Read YAML
Yaml yaml = new Yaml();
Object yamlData = yaml.load(yamlString);
Dynamic<Object> yamlDynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, yamlData);

// Convert to JSON
Dynamic<JsonElement> jsonDynamic = yamlDynamic.convert(GsonOps.INSTANCE);

// Write JSON
Gson gson = new GsonBuilder().setPrettyPrinting().create();
String jsonString = gson.toJson(jsonDynamic.value());
```

See [Format Conversion](../advanced/format-conversion.md) for advanced techniques.

## Migration Example

Apply the same migration logic to different formats:

```java
public class FormatAgnosticMigrator {

    private final AetherDataFixer fixer;

    public <T> Dynamic<T> migrate(Dynamic<T> input, DataVersion from, DataVersion to) {
        TaggedDynamic<T> tagged = new TaggedDynamic<>(TypeReferences.CONFIG, input);
        TaggedDynamic<T> result = fixer.update(tagged, from, to);
        return result.value();
    }
}

// Use with any format
migrator.migrate(jsonDynamic, v1, v2);   // JSON
migrator.migrate(yamlDynamic, v1, v2);   // YAML
migrator.migrate(tomlDynamic, v1, v2);   // TOML
migrator.migrate(xmlDynamic, v1, v2);    // XML
```

## Related Documentation

- [JSON Support](json.md) — GsonOps and JacksonJsonOps details
- [YAML Support](yaml.md) — SnakeYamlOps and JacksonYamlOps details
- [TOML Support](toml.md) — JacksonTomlOps details
- [XML Support](xml.md) — JacksonXmlOps details
- [Dynamic System](../concepts/dynamic-system.md) — Core Dynamic concepts
- [Codec System](../concepts/codec-system.md) — Encoding and decoding
- [Custom DynamicOps](../tutorials/custom-dynamicops.md) — Create your own implementation
