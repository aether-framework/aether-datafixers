# Format Handlers

Format handlers enable the CLI to read and write data in various serialization formats. This guide explains how to use the built-in handlers and create custom ones.

## Built-in Format Handlers

The CLI includes two JSON format handlers out of the box:

### json-gson (Default)

Uses Google Gson for JSON parsing and serialization.

```bash
aether-cli migrate --format json-gson --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.json
```

**Characteristics:**
- Lenient parsing (allows comments, trailing commas)
- Pretty printing with 2-space indentation
- Preserves insertion order

### json-jackson

Uses Jackson Databind for JSON parsing and serialization.

```bash
aether-cli migrate --format json-jackson --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.json
```

**Characteristics:**
- Strict JSON parsing
- High performance for large files
- Configurable via Jackson modules

---

## Listing Available Formats

Use the `info` command to see all registered format handlers:

```bash
aether-cli info --formats
```

Output:
```
Aether Datafixers CLI v0.3.0
============================

Available Formats:
  - json-gson: JSON format using Gson
    Extensions: json
  - json-jackson: JSON format using Jackson
    Extensions: json
```

---

## Creating a Custom Format Handler

You can extend the CLI to support additional formats (YAML, TOML, XML, etc.) by implementing the `FormatHandler` interface.

### Step 1: Implement the Interface

```java
package com.example.format;

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.cli.format.FormatHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Format handler for YAML files.
 */
public class YamlFormatHandler implements FormatHandler<YamlNode> {

    private final YamlOps ops = new YamlOps();
    private final YamlParser parser = new YamlParser();

    @Override
    @NotNull
    public String formatId() {
        return "yaml";
    }

    @Override
    @NotNull
    public String description() {
        return "YAML format";
    }

    @Override
    @NotNull
    public String[] fileExtensions() {
        return new String[]{"yaml", "yml"};
    }

    @Override
    @NotNull
    public DynamicOps<YamlNode> ops() {
        return ops;
    }

    @Override
    @NotNull
    public YamlNode parse(@NotNull String content) {
        return parser.parse(content);
    }

    @Override
    @NotNull
    public String serialize(@NotNull YamlNode data) {
        return parser.dump(data);
    }

    @Override
    @NotNull
    public String serializePretty(@NotNull YamlNode data) {
        // YAML is typically already human-readable
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

public class YamlOps implements DynamicOps<YamlNode> {

    @Override
    public YamlNode empty() {
        return YamlNode.nullNode();
    }

    @Override
    public YamlNode createString(String value) {
        return YamlNode.string(value);
    }

    @Override
    public YamlNode createInt(int value) {
        return YamlNode.number(value);
    }

    @Override
    public Optional<String> getStringValue(YamlNode input) {
        return input.isString()
            ? Optional.of(input.asString())
            : Optional.empty();
    }

    @Override
    public Optional<Number> getNumberValue(YamlNode input) {
        return input.isNumber()
            ? Optional.of(input.asNumber())
            : Optional.empty();
    }

    @Override
    public YamlNode createMap(Map<YamlNode, YamlNode> map) {
        return YamlNode.mapping(map);
    }

    @Override
    public Optional<Stream<Map.Entry<YamlNode, YamlNode>>> getMapValues(YamlNode input) {
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
com.example.format.YamlFormatHandler
```

### Step 4: Package and Use

1. Build your format handler as a JAR
2. Include it on the classpath when running the CLI
3. Use the new format:

```bash
java -cp "aether-cli.jar:yaml-handler.jar" \
    de.splatgames.aether.datafixers.cli.AetherCli \
    migrate --format yaml --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.yaml
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
