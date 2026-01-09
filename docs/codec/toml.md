# TOML Support

The codec module provides `JacksonTomlOps` for working with TOML (Tom's Obvious, Minimal Language) configuration files.

## JacksonTomlOps

### Overview

`JacksonTomlOps` works with Jackson's `JsonNode` tree model, providing TOML 1.0 support via Jackson's TOML module. It's ideal for configuration files that benefit from TOML's clean, human-readable syntax.

**Package:** `de.splatgames.aether.datafixers.codec.toml.jackson`

### Dependencies

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-codec</artifactId>
    <version>${aether.version}</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-toml</artifactId>
    <version>2.18.2</version>
</dependency>
```

### Basic Usage

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;

// Parse TOML
TomlMapper mapper = new TomlMapper();
JsonNode node = mapper.readTree("""
    title = "My Application"
    version = "1.0.0"

    [database]
    host = "localhost"
    port = 5432
    name = "mydb"

    [database.credentials]
    username = "admin"
    password = "secret"
    """);

// Wrap in Dynamic
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonTomlOps.INSTANCE, node);

// Read values
String title = dynamic.get("title").asString().orElse("Untitled");
String host = dynamic.get("database").get("host").asString().orElse("localhost");
int port = dynamic.get("database").get("port").asInt().orElse(5432);

// Modify data
Dynamic<JsonNode> updated = dynamic
    .set("version", dynamic.createString("2.0.0"));
```

### Parse TOML String

```java
TomlMapper mapper = new TomlMapper();

// Parse TOML string
JsonNode node = mapper.readTree(tomlString);
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonTomlOps.INSTANCE, node);

// Parse from InputStream
try (InputStream is = Files.newInputStream(path)) {
    JsonNode node = mapper.readTree(is);
    Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonTomlOps.INSTANCE, node);
}
```

### Write TOML String

```java
TomlMapper mapper = new TomlMapper();

// Write TOML string
String toml = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dynamic.value());
```

> **Note:** Jackson's TOML writer produces valid TOML but may not preserve the original formatting style (inline tables vs standard tables).

### File I/O

```java
TomlMapper mapper = new TomlMapper();

// Read from file
public Dynamic<JsonNode> readToml(Path path) throws IOException {
    JsonNode node = mapper.readTree(path.toFile());
    return new Dynamic<>(JacksonTomlOps.INSTANCE, node);
}

// Write to file
public void writeToml(Path path, Dynamic<JsonNode> dynamic) throws IOException {
    mapper.writerWithDefaultPrettyPrinter()
          .writeValue(path.toFile(), dynamic.value());
}
```

### Custom TomlMapper

```java
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

// Create custom TomlMapper
TomlMapper customMapper = TomlMapper.builder()
    .enable(SerializationFeature.INDENT_OUTPUT)
    .build();

// Create custom JacksonTomlOps
JacksonTomlOps customOps = new JacksonTomlOps(customMapper);
Dynamic<JsonNode> dynamic = new Dynamic<>(customOps, node);
```

---

## TOML Features

### Tables (Sections)

```toml
[server]
host = "localhost"
port = 8080

[server.ssl]
enabled = true
certificate = "/path/to/cert.pem"
```

```java
Dynamic<JsonNode> config = new Dynamic<>(JacksonTomlOps.INSTANCE, node);

// Read table values
String host = config.get("server").get("host").asString().orElse("localhost");
boolean ssl = config.get("server").get("ssl").get("enabled").asBoolean().orElse(false);
```

### Arrays

```toml
ports = [8080, 8081, 8082]

[[servers]]
name = "alpha"
ip = "10.0.0.1"

[[servers]]
name = "beta"
ip = "10.0.0.2"
```

```java
// Read simple array
List<Integer> ports = config.get("ports")
    .asStream()
    .flatMap(d -> d.asInt().result().stream())
    .toList();

// Read array of tables
List<String> serverNames = config.get("servers")
    .asStream()
    .flatMap(d -> d.get("name").asString().result().stream())
    .toList();
```

### Inline Tables

```toml
point = { x = 1, y = 2 }
```

```java
int x = config.get("point").get("x").asInt().orElse(0);
int y = config.get("point").get("y").asInt().orElse(0);
```

### Date/Time

TOML supports native date/time types, but they are read as strings through Jackson:

```toml
created = 2024-01-15T10:30:00Z
date = 2024-01-15
time = 10:30:00
```

```java
String created = config.get("created").asString().orElse("");
LocalDateTime dateTime = LocalDateTime.parse(created, DateTimeFormatter.ISO_DATE_TIME);
```

---

## Migration Example

```java
public class TomlConfigMigrator {

    private final AetherDataFixer fixer;
    private final TomlMapper mapper;

    public TomlConfigMigrator(AetherDataFixer fixer) {
        this.fixer = fixer;
        this.mapper = new TomlMapper();
    }

    public String migrate(String tomlInput, int fromVersion, int toVersion, TypeReference type) {
        try {
            // Parse
            JsonNode node = mapper.readTree(tomlInput);
            Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonTomlOps.INSTANCE, node);

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
            throw new RuntimeException("TOML migration failed", e);
        }
    }
}
```

## Configuration File Service

Complete service for managing versioned TOML configuration:

```java
public class TomlConfigService {

    private final AetherDataFixer fixer;
    private final TomlMapper mapper;
    private final TypeReference configType;

    public TomlConfigService(DataVersion currentVersion, DataFixerBootstrap bootstrap, TypeReference type) {
        this.fixer = new DataFixerRuntimeFactory().create(currentVersion, bootstrap);
        this.mapper = new TomlMapper();
        this.configType = type;
    }

    public Dynamic<JsonNode> load(Path configFile) throws IOException {
        // Read config
        JsonNode node = mapper.readTree(configFile.toFile());
        Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonTomlOps.INSTANCE, node);

        // Check version
        int version = dynamic.get("_version").asInt().orElse(1);

        if (version < fixer.currentVersion().version()) {
            // Migrate
            TaggedDynamic<JsonNode> tagged = new TaggedDynamic<>(configType, dynamic);
            TaggedDynamic<JsonNode> result = fixer.update(
                tagged,
                new DataVersion(version),
                fixer.currentVersion()
            );
            dynamic = result.value();

            // Update version and save
            dynamic = dynamic.set("_version", dynamic.createInt(fixer.currentVersion().version()));
            save(configFile, dynamic);
        }

        return dynamic;
    }

    public void save(Path configFile, Dynamic<JsonNode> config) throws IOException {
        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(configFile.toFile(), config.value());
    }
}

// Usage
TomlConfigService service = new TomlConfigService(
    new DataVersion(3),
    new ConfigBootstrap(),
    TypeReferences.CONFIG
);

Dynamic<JsonNode> config = service.load(Path.of("config.toml"));
String dbHost = config.get("database").get("host").asString().orElse("localhost");
```

## Working with Nested Structures

```java
Dynamic<JsonNode> config = new Dynamic<>(JacksonTomlOps.INSTANCE, tomlNode);

// Read deeply nested value
String username = config
    .get("database")
    .get("credentials")
    .get("username")
    .asString()
    .orElse("root");

// Create nested structure
Dynamic<JsonNode> credentials = config.emptyMap()
    .set("username", config.createString("admin"))
    .set("password", config.createString("secret"));

Dynamic<JsonNode> database = config.emptyMap()
    .set("host", config.createString("localhost"))
    .set("port", config.createInt(5432))
    .set("credentials", credentials);

config = config.set("database", database);
```

## Codec Integration

```java
// Define codec for configuration
public record DatabaseConfig(String host, int port, String name) {

    public static final Codec<DatabaseConfig> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("host").forGetter(DatabaseConfig::host),
            Codecs.INT.optionalFieldOf("port", 5432).forGetter(DatabaseConfig::port),
            Codecs.STRING.fieldOf("name").forGetter(DatabaseConfig::name)
        ).apply(instance, DatabaseConfig::new)
    );
}

// Decode from TOML
TomlMapper mapper = new TomlMapper();
JsonNode node = mapper.readTree(tomlString);

DataResult<DatabaseConfig> result = DatabaseConfig.CODEC.decode(
    JacksonTomlOps.INSTANCE,
    node.get("database")
);
DatabaseConfig config = result.getOrThrow();
```

## Best Practices

1. **Version Your Config** - Include a `_version` field for migration tracking
2. **Use Tables** - Organize related settings into TOML tables (sections)
3. **Handle Missing Keys** - TOML configs often have optional fields
4. **Validate After Load** - Use codecs for type-safe configuration objects

## Limitations

- **Date/Time** - Read as strings, not native Java types
- **Write Formatting** - Jackson may not preserve original formatting style
- **Comments** - Comments are not preserved during round-trip

## Related

- [Codec Overview](index.md)
- [JSON Support](json.md) - JacksonTomlOps shares the JsonNode model
- [Configuration Example](../examples/configuration-example.md)
- [Dynamic System](../concepts/dynamic-system.md)
