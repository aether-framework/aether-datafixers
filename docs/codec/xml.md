# XML Support

The codec module provides `JacksonXmlOps` for working with XML data via Jackson's XML module.

## JacksonXmlOps

### Overview

`JacksonXmlOps` works with Jackson's `JsonNode` tree model, providing XML support through Jackson's XML dataformat. It converts XML to a JSON-like tree structure for consistent handling across formats.

**Package:** `de.splatgames.aether.datafixers.codec.xml.jackson`

### Dependencies

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-codec</artifactId>
    <version>${aether.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
    <version>2.18.2</version>
</dependency>
```

### Basic Usage

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;

// Parse XML
XmlMapper mapper = new XmlMapper();
JsonNode node = mapper.readTree("""
    <player>
        <name>Steve</name>
        <level>42</level>
        <position>
            <x>100.0</x>
            <y>64.0</y>
            <z>-50.0</z>
        </position>
    </player>
    """);

// Wrap in Dynamic
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, node);

// Read values
String name = dynamic.get("name").asString().orElse("Unknown");
int level = dynamic.get("level").asInt().orElse(1);
double x = dynamic.get("position").get("x").asDouble().orElse(0.0);

// Modify data
Dynamic<JsonNode> updated = dynamic
    .set("experience", dynamic.createInt(1500))
    .remove("level");
```

### Parse XML String

```java
XmlMapper mapper = new XmlMapper();

// Parse XML string
JsonNode node = mapper.readTree(xmlString);
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, node);

// Parse from InputStream
try (InputStream is = Files.newInputStream(path)) {
    JsonNode node = mapper.readTree(is);
    Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, node);
}
```

### Write XML String

```java
XmlMapper mapper = new XmlMapper();

// Write XML string
String xml = mapper.writerWithDefaultPrettyPrinter()
                   .writeValueAsString(dynamic.value());
```

### File I/O

```java
XmlMapper mapper = new XmlMapper();

// Read from file
public Dynamic<JsonNode> readXml(Path path) throws IOException {
    JsonNode node = mapper.readTree(path.toFile());
    return new Dynamic<>(JacksonXmlOps.INSTANCE, node);
}

// Write to file
public void writeXml(Path path, Dynamic<JsonNode> dynamic) throws IOException {
    mapper.writerWithDefaultPrettyPrinter()
          .writeValue(path.toFile(), dynamic.value());
}
```

### Custom XmlMapper

```java
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

// Create custom XmlMapper
XmlMapper customMapper = XmlMapper.builder()
    .enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
    .enable(SerializationFeature.INDENT_OUTPUT)
    .build();

// Create custom JacksonXmlOps
JacksonXmlOps customOps = new JacksonXmlOps(customMapper);
Dynamic<JsonNode> dynamic = new Dynamic<>(customOps, node);
```

---

## XML to JsonNode Mapping

Jackson maps XML to JSON-like structures:

### Elements to Fields

```xml
<root>
    <name>Steve</name>
    <level>42</level>
</root>
```

Maps to:
```json
{
    "name": "Steve",
    "level": "42"
}
```

> **Note:** All values are read as strings. Use `asInt()`, `asDouble()`, etc. for conversion.

### Repeated Elements to Arrays

```xml
<players>
    <player>Steve</player>
    <player>Alex</player>
    <player>Herobrine</player>
</players>
```

Maps to:
```json
{
    "player": ["Steve", "Alex", "Herobrine"]
}
```

### Attributes

XML attributes are typically mapped with a prefix (default: empty or configurable):

```xml
<player id="123" active="true">
    <name>Steve</name>
</player>
```

```java
// Configure attribute handling
XmlMapper mapper = XmlMapper.builder()
    .defaultUseWrapper(false)
    .build();
```

### Nested Elements

```xml
<player>
    <position>
        <x>100</x>
        <y>64</y>
        <z>-50</z>
    </position>
</player>
```

```java
Dynamic<JsonNode> player = new Dynamic<>(JacksonXmlOps.INSTANCE, node);
double x = player.get("position").get("x").asDouble().orElse(0.0);
```

---

## Migration Example

```java
public class XmlMigrator {

    private final AetherDataFixer fixer;
    private final XmlMapper mapper;

    public XmlMigrator(AetherDataFixer fixer) {
        this.fixer = fixer;
        this.mapper = new XmlMapper();
    }

    public String migrate(String xmlInput, int fromVersion, int toVersion, TypeReference type) {
        try {
            // Parse
            JsonNode node = mapper.readTree(xmlInput);
            Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, node);

            // Migrate
            TaggedDynamic<JsonNode> tagged = new TaggedDynamic<>(type, dynamic);
            TaggedDynamic<JsonNode> result = fixer.update(
                tagged,
                new DataVersion(fromVersion),
                new DataVersion(toVersion)
            );

            // Serialize
            return mapper.writerWithDefaultPrettyPrinter()
                         .writeValueAsString(result.value().value());
        } catch (IOException e) {
            throw new RuntimeException("XML migration failed", e);
        }
    }
}
```

## XML Configuration Service

Complete service for versioned XML configuration files:

```java
public class XmlConfigService {

    private final AetherDataFixer fixer;
    private final XmlMapper mapper;
    private final TypeReference configType;

    public XmlConfigService(DataVersion currentVersion, DataFixerBootstrap bootstrap, TypeReference type) {
        this.fixer = new DataFixerRuntimeFactory().create(currentVersion, bootstrap);
        this.mapper = XmlMapper.builder()
            .enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();
        this.configType = type;
    }

    public Dynamic<JsonNode> load(Path configFile) throws IOException {
        JsonNode node = mapper.readTree(configFile.toFile());
        Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, node);

        // Check version (stored as element or attribute)
        int version = dynamic.get("version").asInt()
            .or(() -> dynamic.get("_version").asInt())
            .orElse(1);

        if (version < fixer.currentVersion().version()) {
            TaggedDynamic<JsonNode> tagged = new TaggedDynamic<>(configType, dynamic);
            TaggedDynamic<JsonNode> result = fixer.update(
                tagged,
                new DataVersion(version),
                fixer.currentVersion()
            );
            dynamic = result.value();

            // Update version
            dynamic = dynamic.set("version", dynamic.createInt(fixer.currentVersion().version()));
            save(configFile, dynamic);
        }

        return dynamic;
    }

    public void save(Path configFile, Dynamic<JsonNode> config) throws IOException {
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(configFile.toFile(), config.value());
    }
}
```

## Working with Lists

```java
Dynamic<JsonNode> data = new Dynamic<>(JacksonXmlOps.INSTANCE, xmlNode);

// Read list of elements
List<String> items = data.get("items").get("item")
    .asStream()
    .flatMap(d -> d.asString().result().stream())
    .toList();

// Create list
List<Dynamic<JsonNode>> itemList = List.of(
    data.createString("sword"),
    data.createString("shield"),
    data.createString("potion")
);
Dynamic<JsonNode> items = data.createList(itemList.stream());
```

## Codec Integration

```java
public record ServerConfig(String host, int port, boolean ssl) {

    public static final Codec<ServerConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("host").forGetter(ServerConfig::host),
            Codecs.INT.optionalFieldOf("port", 8080).forGetter(ServerConfig::port),
            Codecs.BOOL.optionalFieldOf("ssl", false).forGetter(ServerConfig::ssl)
        ).apply(instance, ServerConfig::new)
    );
}

// Decode from XML
XmlMapper mapper = new XmlMapper();
JsonNode node = mapper.readTree("""
    <server>
        <host>localhost</host>
        <port>8443</port>
        <ssl>true</ssl>
    </server>
    """);

DataResult<ServerConfig> result = ServerConfig.CODEC.decode(JacksonXmlOps.INSTANCE, node);
ServerConfig config = result.getOrThrow();
```

## Best Practices

1. **Use Simple Structures** - Jackson XML works best with simple, well-structured XML
2. **Avoid Mixed Content** - Don't mix text and elements in the same parent
3. **Handle Type Conversion** - XML values are strings; use `asInt()`, `asDouble()` explicitly
4. **Test Round-Trips** - Verify XML survives read-write cycles with your specific structure

## Limitations

- **Attributes** - Limited support, may require custom configuration
- **Namespaces** - Basic support only
- **Mixed Content** - Text mixed with elements is problematic
- **Ordering** - Element order may not be preserved for non-repeated elements
- **Comments** - Not preserved during round-trip
- **Processing Instructions** - Not supported

## When to Use

**Good fit:**
- Simple, well-structured XML configuration files
- XML data with known, consistent structure
- Converting between XML and JSON/YAML
- Legacy XML data migration

**Consider alternatives for:**
- Complex XML with namespaces
- XML with attributes on every element
- Mixed content documents
- XML requiring schema validation

## Related

- [Codec Overview](index.md)
- [JSON Support](json.md) - JacksonXmlOps shares the JsonNode model
- [YAML Support](yaml.md) - Alternative for configuration files
- [Dynamic System](../concepts/dynamic-system.md)
