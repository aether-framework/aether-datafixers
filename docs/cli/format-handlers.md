# Format Handlers

Format handlers enable the CLI to read and write data in various serialization formats. This guide explains how to use the built-in handlers and create custom ones.

## Built-in Format Handlers

The CLI includes six format handlers out of the box, covering JSON, YAML, TOML, and XML:

### JSON Handlers

#### json-gson (Default)

Uses Google Gson for JSON parsing and serialization.

```bash
aether-cli migrate --format json-gson --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.json
```

**Characteristics:**
- Lenient parsing (allows comments, trailing commas)
- Pretty printing with 2-space indentation
- Preserves insertion order

#### json-jackson

Uses Jackson Databind for JSON parsing and serialization.

```bash
aether-cli migrate --format json-jackson --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.json
```

**Characteristics:**
- Strict JSON parsing
- High performance for large files
- Configurable via Jackson modules

### YAML Handlers

#### yaml-snakeyaml

Uses SnakeYAML for YAML parsing and serialization with native Java types.

```bash
aether-cli migrate --format yaml-snakeyaml --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.yaml
```

**Characteristics:**
- Native Java type representation (Map, List, String, Number, Boolean)
- Block-style pretty printing
- Supports multiline strings and anchors

#### yaml-jackson

Uses Jackson YAML for YAML parsing and serialization with JsonNode.

```bash
aether-cli migrate --format yaml-jackson --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.yaml
```

**Characteristics:**
- Consistent with other Jackson-based handlers
- JsonNode representation for unified processing
- Supports Jackson annotations and modules

### TOML Handler

#### toml-jackson

Uses Jackson TOML for TOML parsing and serialization.

```bash
aether-cli migrate --format toml-jackson --to 200 --type config \
    --bootstrap com.example.MyBootstrap config.toml
```

**Characteristics:**
- Full TOML 1.0 specification support
- Table and inline table support
- Preserves arrays and nested structures

**Note:** TOML has structural constraints: root element must be a table, arrays can only contain elements of the same type, and null values are not supported.

### XML Handler

#### xml-jackson

Uses Jackson XML for XML parsing and serialization.

```bash
aether-cli migrate --format xml-jackson --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.xml
```

**Characteristics:**
- Automatic attribute/element handling
- CDATA section support
- XML declaration with UTF-8 encoding

**Note:** XML requires a single root element, and element names must follow XML naming conventions.

---

## Listing Available Formats

Use the `info` command to see all registered format handlers:

```bash
aether-cli info --formats
```

Output:
```
Aether Datafixers CLI v0.5.0
============================

Available Formats:
  - json-gson: JSON format using Gson
    Extensions: json
  - json-jackson: JSON format using Jackson
    Extensions: json
  - yaml-snakeyaml: YAML format using SnakeYAML (native Java types)
    Extensions: yaml, yml
  - yaml-jackson: YAML format using Jackson
    Extensions: yaml, yml
  - toml-jackson: TOML format using Jackson
    Extensions: toml
  - xml-jackson: XML format using Jackson
    Extensions: xml
```

---

## Creating a Custom Format Handler

You can extend the CLI to support additional formats by implementing the `FormatHandler` interface. This is useful for proprietary formats, binary formats, or alternative libraries.

> **Note:** JSON, YAML, TOML, and XML are already supported out of the box. Only implement custom handlers for formats not listed above.

### Step 1: Implement the Interface

```java
package com.example.format;

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.cli.format.FormatHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Format handler for a custom binary format.
 */
public class BinaryFormatHandler implements FormatHandler<BinaryNode> {

    private final BinaryOps ops = new BinaryOps();
    private final BinaryParser parser = new BinaryParser();

    @Override
    @NotNull
    public String formatId() {
        return "binary-custom";
    }

    @Override
    @NotNull
    public String description() {
        return "Custom binary format";
    }

    @Override
    @NotNull
    public String[] fileExtensions() {
        return new String[]{"bin", "dat"};
    }

    @Override
    @NotNull
    public DynamicOps<BinaryNode> ops() {
        return this.ops;
    }

    @Override
    @NotNull
    public BinaryNode parse(@NotNull String content) {
        return this.parser.parse(content);
    }

    @Override
    @NotNull
    public String serialize(@NotNull BinaryNode data) {
        return this.parser.dump(data);
    }

    @Override
    @NotNull
    public String serializePretty(@NotNull BinaryNode data) {
        return serialize(data);
    }
}
```

### Step 2: Create a DynamicOps Implementation

Your format handler needs a corresponding `DynamicOps<T>` implementation:

```java
package com.example.format;

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class BinaryOps implements DynamicOps<BinaryNode> {

    @Override
    public BinaryNode empty() {
        return BinaryNode.nullNode();
    }

    @Override
    public BinaryNode createString(String value) {
        return BinaryNode.string(value);
    }

    @Override
    public BinaryNode createInt(int value) {
        return BinaryNode.number(value);
    }

    @Override
    public Optional<String> getStringValue(BinaryNode input) {
        return input.isString()
            ? Optional.of(input.asString())
            : Optional.empty();
    }

    @Override
    public Optional<Number> getNumberValue(BinaryNode input) {
        return input.isNumber()
            ? Optional.of(input.asNumber())
            : Optional.empty();
    }

    @Override
    public BinaryNode createMap(Map<BinaryNode, BinaryNode> map) {
        return BinaryNode.mapping(map);
    }

    @Override
    public Optional<Stream<Map.Entry<BinaryNode, BinaryNode>>> getMapValues(BinaryNode input) {
        return input.isMapping()
            ? Optional.of(input.entries().stream())
            : Optional.empty();
    }

    // ... implement remaining DynamicOps methods
}
```

### Step 3: Register via ServiceLoader

Create a service provider configuration file:

**File:** `META-INF/services/de.splatgames.aether.datafixers.cli.format.FormatHandler`

```
com.example.format.BinaryFormatHandler
```

### Step 4: Package and Use

1. Build your format handler as a JAR
2. Include it on the classpath when running the CLI
3. Use the new format:

```bash
java -cp "aether-cli.jar:binary-handler.jar" \
    de.splatgames.aether.datafixers.cli.AetherCli \
    migrate --format binary-custom --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.bin
```

---

## FormatHandler Interface Reference

```java
public interface FormatHandler<T> {

    /**
     * Unique identifier for the format (e.g., "yaml", "toml").
     * Used with the --format CLI option.
     */
    @NotNull
    String formatId();

    /**
     * Human-readable description shown in 'info --formats'.
     */
    @NotNull
    String description();

    /**
     * File extensions without dots (e.g., {"yaml", "yml"}).
     * Used for informational display.
     */
    @NotNull
    String[] fileExtensions();

    /**
     * The DynamicOps for this format.
     * Enables format-agnostic data manipulation.
     */
    @NotNull
    DynamicOps<T> ops();

    /**
     * Parse string content into the format's data type.
     * @throws FormatParseException on parse errors
     */
    @NotNull
    T parse(@NotNull String content);

    /**
     * Serialize data to a compact string.
     */
    @NotNull
    String serialize(@NotNull T data);

    /**
     * Serialize with pretty printing (default: delegates to serialize).
     */
    @NotNull
    default String serializePretty(@NotNull T data) {
        return serialize(data);
    }
}
```

---

## Error Handling

When parsing fails, throw `FormatParseException`:

```java
import de.splatgames.aether.datafixers.cli.format.FormatParseException;

@Override
@NotNull
public YamlNode parse(@NotNull String content) {
    try {
        return parser.parse(content);
    } catch (YamlParseException e) {
        throw new FormatParseException(
            "Failed to parse YAML: " + e.getMessage(), e);
    }
}
```

The CLI will catch this exception and display a user-friendly error:

```
Error processing input.yaml: Failed to parse YAML: Invalid syntax at line 5
```

---

## Best Practices

### 1. Use Meaningful Format IDs

Choose IDs that clearly identify the format and library:

```java
// Good
"yaml-snakeyaml"
"toml-toml4j"
"xml-jaxb"

// Less clear
"yaml"     // Which YAML library?
"format1"  // Not descriptive
```

### 2. Handle Null Values Consistently

Ensure your `DynamicOps` handles null/missing values properly:

```java
@Override
public YamlNode empty() {
    return YamlNode.nullNode();  // Not null!
}
```

### 3. Support Pretty Printing

If your format supports formatting options, implement `serializePretty`:

```java
@Override
@NotNull
public String serializePretty(@NotNull YamlNode data) {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setIndent(2);
    return new Yaml(options).dump(data);
}
```

### 4. Document Requirements

Note any dependencies users need to add:

```java
/**
 * YAML format handler using SnakeYAML.
 *
 * <p>Requires the following dependency:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.yaml</groupId>
 *     <artifactId>snakeyaml</artifactId>
 *     <version>2.2</version>
 * </dependency>
 * }</pre>
 */
public class YamlFormatHandler implements FormatHandler<YamlNode> {
```

---

## Next Steps

→ [Examples](examples.md) — See format handlers in real-world scenarios
