# Configuration Example

Migrating application configuration files with defaults and restructuring.

## Scenario

An application stores configuration that evolves across versions:

| Version | Changes |
|---------|---------|
| v1 | Flat structure with basic settings |
| v2 | Grouped settings into categories |
| v3 | Added feature flags, deprecated old settings |

## Data Evolution

### Version 1 (Flat)

```json
{
  "serverHost": "localhost",
  "serverPort": 8080,
  "maxConnections": 100,
  "logLevel": "INFO",
  "logFile": "app.log",
  "cacheEnabled": true,
  "cacheSize": 1000
}
```

### Version 2 (Grouped)

```json
{
  "server": {
    "host": "localhost",
    "port": 8080,
    "maxConnections": 100
  },
  "logging": {
    "level": "INFO",
    "file": "app.log"
  },
  "cache": {
    "enabled": true,
    "size": 1000
  }
}
```

### Version 3 (Feature Flags)

```json
{
  "server": {
    "host": "localhost",
    "port": 8080,
    "maxConnections": 100,
    "ssl": false
  },
  "logging": {
    "level": "INFO",
    "file": "app.log",
    "format": "json"
  },
  "cache": {
    "enabled": true,
    "size": 1000,
    "ttlSeconds": 3600
  },
  "features": {
    "newUI": false,
    "analytics": true,
    "betaFeatures": false
  }
}
```

## TypeReferences

```java
public final class ConfigReferences {
    public static final TypeReference CONFIG = TypeReference.of("config");

    private ConfigReferences() {}
}
```

## Schemas

### Schema V1

```java
public class ConfigSchemaV1 extends Schema {

    public ConfigSchemaV1(Schema parent) {
        super(new DataVersion(1), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(ConfigReferences.CONFIG, DSL.and(
            DSL.field("serverHost", DSL.string()),
            DSL.field("serverPort", DSL.intType()),
            DSL.field("maxConnections", DSL.intType()),
            DSL.field("logLevel", DSL.string()),
            DSL.field("logFile", DSL.string()),
            DSL.field("cacheEnabled", DSL.bool()),
            DSL.field("cacheSize", DSL.intType()),
            DSL.remainder()
        ));
    }
}
```

### Schema V2

```java
public class ConfigSchemaV2 extends Schema {

    public ConfigSchemaV2(Schema parent) {
        super(new DataVersion(2), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(ConfigReferences.CONFIG, DSL.and(
            DSL.field("server", DSL.and(
                DSL.field("host", DSL.string()),
                DSL.field("port", DSL.intType()),
                DSL.field("maxConnections", DSL.intType()),
                DSL.remainder()
            )),
            DSL.field("logging", DSL.and(
                DSL.field("level", DSL.string()),
                DSL.field("file", DSL.string()),
                DSL.remainder()
            )),
            DSL.field("cache", DSL.and(
                DSL.field("enabled", DSL.bool()),
                DSL.field("size", DSL.intType()),
                DSL.remainder()
            )),
            DSL.remainder()
        ));
    }
}
```

### Schema V3

```java
public class ConfigSchemaV3 extends Schema {

    public ConfigSchemaV3(Schema parent) {
        super(new DataVersion(3), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(ConfigReferences.CONFIG, DSL.and(
            DSL.field("server", DSL.and(
                DSL.field("host", DSL.string()),
                DSL.field("port", DSL.intType()),
                DSL.field("maxConnections", DSL.intType()),
                DSL.field("ssl", DSL.bool()),
                DSL.remainder()
            )),
            DSL.field("logging", DSL.and(
                DSL.field("level", DSL.string()),
                DSL.field("file", DSL.string()),
                DSL.field("format", DSL.string()),
                DSL.remainder()
            )),
            DSL.field("cache", DSL.and(
                DSL.field("enabled", DSL.bool()),
                DSL.field("size", DSL.intType()),
                DSL.field("ttlSeconds", DSL.intType()),
                DSL.remainder()
            )),
            DSL.field("features", DSL.and(
                DSL.field("newUI", DSL.bool()),
                DSL.field("analytics", DSL.bool()),
                DSL.field("betaFeatures", DSL.bool()),
                DSL.remainder()
            )),
            DSL.remainder()
        ));
    }
}
```

## Fixes

### V1 to V2 Fix (Restructure)

```java
public class ConfigV1ToV2Fix extends SchemaDataFix {

    public ConfigV1ToV2Fix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(1), new DataVersion(2), "config-v1-to-v2");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.transform(ConfigReferences.CONFIG, this::restructure);
    }

    private Dynamic<?> restructure(Dynamic<?> config) {
        // Extract values from flat structure
        String host = config.get("serverHost").asString().orElse("localhost");
        int port = config.get("serverPort").asInt().orElse(8080);
        int maxConn = config.get("maxConnections").asInt().orElse(100);

        String logLevel = config.get("logLevel").asString().orElse("INFO");
        String logFile = config.get("logFile").asString().orElse("app.log");

        boolean cacheEnabled = config.get("cacheEnabled").asBoolean().orElse(true);
        int cacheSize = config.get("cacheSize").asInt().orElse(1000);

        // Create grouped structure
        Dynamic<?> server = config.emptyMap()
            .set("host", config.createString(host))
            .set("port", config.createInt(port))
            .set("maxConnections", config.createInt(maxConn));

        Dynamic<?> logging = config.emptyMap()
            .set("level", config.createString(logLevel))
            .set("file", config.createString(logFile));

        Dynamic<?> cache = config.emptyMap()
            .set("enabled", config.createBoolean(cacheEnabled))
            .set("size", config.createInt(cacheSize));

        // Remove old flat fields and add grouped ones
        return config
            .remove("serverHost")
            .remove("serverPort")
            .remove("maxConnections")
            .remove("logLevel")
            .remove("logFile")
            .remove("cacheEnabled")
            .remove("cacheSize")
            .set("server", server)
            .set("logging", logging)
            .set("cache", cache);
    }
}
```

### V2 to V3 Fix (Add Defaults)

```java
public class ConfigV2ToV3Fix extends SchemaDataFix {

    public ConfigV2ToV3Fix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(2), new DataVersion(3), "config-v2-to-v3");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            Rules.transform(ConfigReferences.CONFIG, this::addServerDefaults),
            Rules.transform(ConfigReferences.CONFIG, this::addLoggingDefaults),
            Rules.transform(ConfigReferences.CONFIG, this::addCacheDefaults),
            Rules.transform(ConfigReferences.CONFIG, this::addFeatureFlags)
        );
    }

    private Dynamic<?> addServerDefaults(Dynamic<?> config) {
        return config.update("server", server ->
            server.get("ssl").result().isPresent() ? server
                : server.set("ssl", server.createBoolean(false))
        );
    }

    private Dynamic<?> addLoggingDefaults(Dynamic<?> config) {
        return config.update("logging", logging ->
            logging.get("format").result().isPresent() ? logging
                : logging.set("format", logging.createString("json"))
        );
    }

    private Dynamic<?> addCacheDefaults(Dynamic<?> config) {
        return config.update("cache", cache ->
            cache.get("ttlSeconds").result().isPresent() ? cache
                : cache.set("ttlSeconds", cache.createInt(3600))
        );
    }

    private Dynamic<?> addFeatureFlags(Dynamic<?> config) {
        if (config.get("features").result().isEmpty()) {
            Dynamic<?> features = config.emptyMap()
                .set("newUI", config.createBoolean(false))
                .set("analytics", config.createBoolean(true))
                .set("betaFeatures", config.createBoolean(false));
            return config.set("features", features);
        }
        return config;
    }
}
```

## Bootstrap

```java
public class ConfigBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(3);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;
        schemas.register(new DataVersion(1), ConfigSchemaV1::new);
        schemas.register(new DataVersion(2), ConfigSchemaV2::new);
        schemas.register(new DataVersion(3), ConfigSchemaV3::new);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(ConfigReferences.CONFIG, new ConfigV1ToV2Fix(schemas));
        fixes.register(ConfigReferences.CONFIG, new ConfigV2ToV3Fix(schemas));
    }
}
```

## Usage

```java
public class ConfigMigrator {

    private final AetherDataFixer fixer;

    public ConfigMigrator() {
        this.fixer = new DataFixerRuntimeFactory()
            .create(ConfigBootstrap.CURRENT_VERSION, new ConfigBootstrap());
    }

    public JsonObject migrateConfig(Path configFile) throws IOException {
        Gson gson = new Gson();

        // Read existing config
        JsonObject config;
        try (Reader reader = Files.newBufferedReader(configFile)) {
            config = gson.fromJson(reader, JsonObject.class);
        }

        // Detect version (v1 has flat structure, v2+ has "server" object)
        int version = config.has("server") ?
            (config.has("features") ? 3 : 2) : 1;

        if (version >= ConfigBootstrap.CURRENT_VERSION.version()) {
            return config; // Already current
        }

        // Migrate
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, config);
        TaggedDynamic tagged = new TaggedDynamic(ConfigReferences.CONFIG, dynamic);

        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(version),
            ConfigBootstrap.CURRENT_VERSION
        );

        return (JsonObject) migrated.value().value();
    }
}
```

## Key Patterns Demonstrated

1. **Flat to nested restructuring** — Group related settings
2. **Adding defaults** — New settings with sensible defaults
3. **Feature flags** — Conditional behavior toggles
4. **Version detection** — Infer version from structure

## Related

- [Restructure Data](../how-to/restructure-data.md)
- [Add Field](../how-to/add-field.md)
- [Game Data Example](game-data-example/index.md)

