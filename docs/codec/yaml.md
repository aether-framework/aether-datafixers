# YAML Support

The codec module provides two `DynamicOps` implementations for YAML: **SnakeYamlOps** (using SnakeYAML) and **JacksonYamlOps** (using Jackson YAML).

## SnakeYamlOps

### Overview

`SnakeYamlOps` works with native Java types (`Map<String, Object>`, `List<Object>`, primitives). It provides full YAML 1.1 feature support including anchors, aliases, and multi-document streams.

**Package:** `de.splatgames.aether.datafixers.codec.yaml.snakeyaml`

### Dependencies

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-codec</artifactId>
    <version>${aether.version}</version>
</dependency>
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>2.3</version>
</dependency>
```

### Basic Usage

```java
import org.yaml.snakeyaml.Yaml;
import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;

// Parse YAML
Yaml yaml = new Yaml();
Object data = yaml.load("""
    name: Steve
    level: 42
    position:
      x: 100.0
      y: 64.0
      z: -50.0
    """);

// Wrap in Dynamic
Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, data);

// Read values
String name = dynamic.get("name").asString().orElse("Unknown");
int level = dynamic.get("level").asInt().orElse(1);
double x = dynamic.get("position").get("x").asDouble().orElse(0.0);

// Modify data
Dynamic<Object> updated = dynamic
    .set("experience", dynamic.createInt(1500))
    .remove("level");
```

### Parse YAML String

```java
import org.yaml.snakeyaml.Yaml;

Yaml yaml = new Yaml();

// Load single document
Object data = yaml.load(yamlString);
Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, data);

// Load all documents
Iterable<Object> documents = yaml.loadAll(yamlString);
for (Object doc : documents) {
    Dynamic<Object> docDynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, doc);
    // Process each document
}
```

### Write YAML String

```java
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

// Configure output style
DumperOptions options = new DumperOptions();
options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
options.setPrettyFlow(true);
options.setIndent(2);

Yaml yaml = new Yaml(options);
String yamlString = yaml.dump(dynamic.value());
```

### File I/O

```java
Yaml yaml = new Yaml();

// Read from file
public Dynamic<Object> readYaml(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path)) {
        Object data = yaml.load(reader);
        return new Dynamic<>(SnakeYamlOps.INSTANCE, data);
    }
}

// Write to file
public void writeYaml(Path path, Dynamic<Object> dynamic) throws IOException {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    Yaml yaml = new Yaml(options);

    try (Writer writer = Files.newBufferedWriter(path)) {
        yaml.dump(dynamic.value(), writer);
    }
}
```

### Custom Yaml Configuration

```java
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

// Safe loading (prevents arbitrary code execution)
LoaderOptions loaderOptions = new LoaderOptions();
loaderOptions.setAllowDuplicateKeys(false);
loaderOptions.setMaxAliasesForCollections(50);

Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
Object data = yaml.load(untrustedYaml);
```

### Data Types

SnakeYamlOps works with native Java types:

| YAML Type | Java Type |
|-----------|-----------|
| Mapping | `LinkedHashMap<String, Object>` |
| Sequence | `ArrayList<Object>` |
| String | `String` |
| Integer | `Integer` or `Long` |
| Float | `Double` |
| Boolean | `Boolean` |
| Null | `null` |
| Date | `java.util.Date` |

---

## JacksonYamlOps

### Overview

`JacksonYamlOps` works with Jackson's `JsonNode` tree model, providing consistent handling across JSON and YAML formats. It supports YAML 1.2 via Jackson's YAML module.

**Package:** `de.splatgames.aether.datafixers.codec.yaml.jackson`

### Dependencies

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-codec</artifactId>
    <version>${aether.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
    <version>2.18.2</version>
</dependency>
```

### Basic Usage

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;

// Parse YAML
YAMLMapper mapper = new YAMLMapper();
JsonNode node = mapper.readTree("""
    name: Steve
    level: 42
    position:
      x: 100.0
      y: 64.0
      z: -50.0
    """);

// Wrap in Dynamic
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonYamlOps.INSTANCE, node);

// Read values
String name = dynamic.get("name").asString().orElse("Unknown");
int level = dynamic.get("level").asInt().orElse(1);

// Modify data
Dynamic<JsonNode> updated = dynamic
    .set("experience", dynamic.createInt(1500))
    .remove("level");
```

### Parse and Write

```java
YAMLMapper mapper = new YAMLMapper();

// Parse YAML string
JsonNode node = mapper.readTree(yamlString);
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonYamlOps.INSTANCE, node);

// Write YAML string
String yaml = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dynamic.value());
```

### Custom YAMLMapper

```java
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

// Configure YAML output
YAMLFactory factory = YAMLFactory.builder()
    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    .build();

YAMLMapper customMapper = new YAMLMapper(factory);

// Create custom JacksonYamlOps
JacksonYamlOps customOps = new JacksonYamlOps(customMapper);
Dynamic<JsonNode> dynamic = new Dynamic<>(customOps, node);
```

### File I/O

```java
YAMLMapper mapper = new YAMLMapper();

// Read from file
public Dynamic<JsonNode> readYaml(Path path) throws IOException {
    JsonNode node = mapper.readTree(path.toFile());
    return new Dynamic<>(JacksonYamlOps.INSTANCE, node);
}

// Write to file
public void writeYaml(Path path, Dynamic<JsonNode> dynamic) throws IOException {
    mapper.writerWithDefaultPrettyPrinter()
          .writeValue(path.toFile(), dynamic.value());
}
```

---

## Comparison

| Feature | SnakeYamlOps | JacksonYamlOps |
|---------|--------------|----------------|
| **Data Type** | `Object` (Map/List) | `JsonNode` |
| **YAML Version** | 1.1 | 1.2 |
| **Library Size** | ~300 KB | ~100 KB + Jackson |
| **Anchors/Aliases** | Full support | Limited |
| **Multi-Document** | Full support | Limited |
| **Custom Tags** | Yes | No |
| **Cross-Format** | Separate handling | Same as JSON |

### When to Use SnakeYamlOps

- Full YAML 1.1 feature support needed
- Using anchors and aliases extensively
- Processing multi-document YAML streams
- Working with custom YAML tags
- Standalone YAML processing

### When to Use JacksonYamlOps

- Already using Jackson for JSON
- Want consistent `JsonNode` handling
- Converting between JSON and YAML
- Need YAML 1.2 compliance
- Simpler data structures

---

## Migration Example

```java
public class YamlMigrator {

    private final AetherDataFixer fixer;
    private final Yaml yaml;

    public YamlMigrator(AetherDataFixer fixer) {
        this.fixer = fixer;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
    }

    public String migrate(String yamlInput, int fromVersion, int toVersion, TypeReference type) {
        // Parse
        Object data = yaml.load(yamlInput);
        Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, data);

        // Migrate
        TaggedDynamic<Object> tagged = new TaggedDynamic<>(type, dynamic);
        TaggedDynamic<Object> result = fixer.update(
            tagged,
            new DataVersion(fromVersion),
            new DataVersion(toVersion)
        );

        // Serialize
        return yaml.dump(result.value().value());
    }
}

// Usage
YamlMigrator migrator = new YamlMigrator(fixer);
String migratedYaml = migrator.migrate(oldYaml, 1, 2, TypeReferences.CONFIG);
```

## Working with Nested Data

```java
Dynamic<Object> config = new Dynamic<>(SnakeYamlOps.INSTANCE, yamlData);

// Read nested value
String host = config.get("database").get("host").asString().orElse("localhost");
int port = config.get("database").get("port").asInt().orElse(5432);

// Create nested structure
Dynamic<Object> database = config.emptyMap()
    .set("host", config.createString("localhost"))
    .set("port", config.createInt(5432))
    .set("name", config.createString("mydb"));

config = config.set("database", database);
```

## Working with Lists

```java
Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, yamlData);

// Read list
List<String> servers = dynamic.get("servers")
    .asStream()
    .flatMap(d -> d.asString().result().stream())
    .toList();

// Create list
List<Dynamic<Object>> servers = List.of(
    dynamic.createString("server1.example.com"),
    dynamic.createString("server2.example.com"),
    dynamic.createString("server3.example.com")
);
Dynamic<Object> serverList = dynamic.createList(servers.stream());
dynamic = dynamic.set("servers", serverList);
```

## Configuration File Migration

Complete example for migrating YAML configuration files:

```java
public class ConfigMigrationService {

    private final AetherDataFixer fixer;
    private final Yaml yaml;

    public ConfigMigrationService(DataVersion currentVersion, DataFixerBootstrap bootstrap) {
        this.fixer = new DataFixerRuntimeFactory().create(currentVersion, bootstrap);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
    }

    public void migrateFile(Path configFile, TypeReference type) throws IOException {
        // Read config
        Object data;
        try (Reader reader = Files.newBufferedReader(configFile)) {
            data = yaml.load(reader);
        }

        // Check version
        Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, data);
        int version = dynamic.get("_version").asInt().orElse(1);

        if (version >= fixer.currentVersion().version()) {
            return; // Already up to date
        }

        // Migrate
        TaggedDynamic<Object> tagged = new TaggedDynamic<>(type, dynamic);
        TaggedDynamic<Object> result = fixer.update(
            tagged,
            new DataVersion(version),
            fixer.currentVersion()
        );

        // Update version field
        Dynamic<Object> migrated = result.value()
            .set("_version", result.value().createInt(fixer.currentVersion().version()));

        // Write back
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            yaml.dump(migrated.value(), writer);
        }
    }
}
```

## Best Practices

1. **Use SafeConstructor** - When loading untrusted YAML with SnakeYAML
2. **Preserve Order** - Both implementations preserve field order using `LinkedHashMap`
3. **Handle Nulls** - YAML `null` and `~` are handled gracefully
4. **Choose Wisely** - Use SnakeYamlOps for full YAML features, JacksonYamlOps for JSON compatibility

## Related

- [Codec Overview](index.md)
- [JSON Support](json.md)
- [Dynamic System](../concepts/dynamic-system.md)
- [Format Conversion](../advanced/format-conversion.md)
