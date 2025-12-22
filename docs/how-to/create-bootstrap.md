# How to Create a Bootstrap

This guide shows how to create a `DataFixerBootstrap` to configure your data fixer.

## Basic Bootstrap Structure

```java
public class GameDataBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(3);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;

        // Register schemas in version order
        schemas.register(new DataVersion(1), this::createSchemaV1);
        schemas.register(new DataVersion(2), this::createSchemaV2);
        schemas.register(new DataVersion(3), this::createSchemaV3);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Register fixes between versions
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
        fixes.register(TypeReferences.PLAYER, new PlayerV2ToV3Fix(schemas));
    }

    private Schema createSchemaV1(Schema parent) {
        Schema schema = new Schema(new DataVersion(1), parent);
        schema.registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("playerName", DSL.string()),
            DSL.field("xp", DSL.intType()),
            DSL.remainder()
        ));
        return schema;
    }

    private Schema createSchemaV2(Schema parent) {
        Schema schema = new Schema(new DataVersion(2), parent);
        schema.registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("experience", DSL.intType()),
            DSL.remainder()
        ));
        return schema;
    }

    private Schema createSchemaV3(Schema parent) {
        Schema schema = new Schema(new DataVersion(3), parent);
        schema.registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("experience", DSL.intType()),
            DSL.field("level", DSL.intType()),
            DSL.remainder()
        ));
        return schema;
    }
}
```

## Use the Bootstrap

```java
// Create the data fixer
AetherDataFixer fixer = new DataFixerRuntimeFactory()
    .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

// Use it to migrate data
Dynamic<JsonElement> data = new Dynamic<>(GsonOps.INSTANCE, jsonObject);
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, data);

TaggedDynamic migrated = fixer.update(
    tagged,
    new DataVersion(1),  // from
    new DataVersion(3)   // to
);
```

## Define Type References

Create a central class for type references:

```java
public final class TypeReferences {
    public static final TypeReference PLAYER = TypeReference.of("player");
    public static final TypeReference WORLD = TypeReference.of("world");
    public static final TypeReference ITEM = TypeReference.of("item");
    public static final TypeReference CONFIG = TypeReference.of("config");

    private TypeReferences() {}  // Utility class
}
```

## Separate Schema Classes

For larger projects, use separate schema classes:

```java
public class GameDataBootstrap implements DataFixerBootstrap {

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;

        schemas.register(new DataVersion(100), Schema100::new);
        schemas.register(new DataVersion(110), Schema110::new);
        schemas.register(new DataVersion(200), Schema200::new);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
        fixes.register(TypeReferences.PLAYER, new PlayerV2ToV3Fix(schemas));
    }
}

// Separate schema class
public class Schema100 extends Schema {

    public Schema100(Schema parent) {
        super(new DataVersion(100), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.remainder()
        ));
    }
}
```

## Multiple Type References

Register multiple types in schemas:

```java
private Schema createSchemaV1(Schema parent) {
    Schema schema = new Schema(new DataVersion(1), parent);

    // Player type
    schema.registerType(TypeReferences.PLAYER, DSL.and(
        DSL.field("name", DSL.string()),
        DSL.field("health", DSL.intType()),
        DSL.remainder()
    ));

    // World type
    schema.registerType(TypeReferences.WORLD, DSL.and(
        DSL.field("name", DSL.string()),
        DSL.field("seed", DSL.longType()),
        DSL.remainder()
    ));

    // Item type
    schema.registerType(TypeReferences.ITEM, DSL.and(
        DSL.field("id", DSL.string()),
        DSL.field("count", DSL.intType()),
        DSL.remainder()
    ));

    return schema;
}
```

## Register Multiple Fixes

```java
@Override
public void registerFixes(FixRegistrar fixes) {
    // Player fixes
    fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
    fixes.register(TypeReferences.PLAYER, new PlayerV2ToV3Fix(schemas));

    // World fixes
    fixes.register(TypeReferences.WORLD, new WorldV1ToV2Fix(schemas));

    // Item fixes
    fixes.register(TypeReferences.ITEM, new ItemV1ToV2Fix(schemas));
    fixes.register(TypeReferences.ITEM, new ItemV2ToV3Fix(schemas));
}
```

## Conditional Registration

```java
@Override
public void registerFixes(FixRegistrar fixes) {
    // Always register core fixes
    fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));

    // Conditionally register optional fixes
    if (enableLegacySupport) {
        fixes.register(TypeReferences.PLAYER, new LegacyPlayerFix(schemas));
    }
}
```

## Versioning Strategy

Choose a versioning approach:

```java
// Sequential integers
public static final DataVersion V1 = new DataVersion(1);
public static final DataVersion V2 = new DataVersion(2);
public static final DataVersion V3 = new DataVersion(3);

// Semantic version encoded
public static final DataVersion V1_0_0 = new DataVersion(100);
public static final DataVersion V1_1_0 = new DataVersion(110);
public static final DataVersion V2_0_0 = new DataVersion(200);

// Date-based
public static final DataVersion V2024_01 = new DataVersion(202401);
public static final DataVersion V2024_06 = new DataVersion(202406);
```

## Factory Pattern

Use a factory for consistent creation:

```java
public class DataFixerFactory {

    private static AetherDataFixer instance;

    public static synchronized AetherDataFixer getInstance() {
        if (instance == null) {
            instance = new DataFixerRuntimeFactory()
                .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
        }
        return instance;
    }

    // For testing with custom version
    public static AetherDataFixer create(DataVersion targetVersion) {
        return new DataFixerRuntimeFactory()
            .create(targetVersion, new GameDataBootstrap());
    }
}
```

## Bootstrap with Dependencies

```java
public class GameDataBootstrap implements DataFixerBootstrap {

    private final GameConfig config;
    private SchemaRegistry schemas;

    public GameDataBootstrap(GameConfig config) {
        this.config = config;
    }

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;
        // Use config to determine which schemas to register
        schemas.register(new DataVersion(1), parent -> createSchemaV1(parent, config));
    }

    private Schema createSchemaV1(Schema parent, GameConfig config) {
        Schema schema = new Schema(new DataVersion(1), parent);
        // Use config values in schema definition
        return schema;
    }
}
```

## Testing the Bootstrap

```java
class GameDataBootstrapTest {

    @Test
    void testBootstrapCreatesValidFixer() {
        GameDataBootstrap bootstrap = new GameDataBootstrap();
        AetherDataFixer fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, bootstrap);

        assertNotNull(fixer);
    }

    @Test
    void testMigrationV1ToV3() {
        AetherDataFixer fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

        JsonObject v1 = new JsonObject();
        v1.addProperty("playerName", "Steve");
        v1.addProperty("xp", 100);

        Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, v1);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, input);

        TaggedDynamic result = fixer.update(tagged,
            new DataVersion(1), new DataVersion(3));

        Dynamic<?> output = result.value();
        assertEquals("Steve", output.get("name").asString().orElse(""));
        assertEquals(100, output.get("experience").asInt().orElse(0));
    }
}
```

## Best Practices

1. **Keep schemas in version order** for clarity
2. **Use separate classes** for complex schemas and fixes
3. **Define type references centrally** in a TypeReferences class
4. **Document version changes** with comments
5. **Test migrations** for each version transition

## Related

- [DataFix System](../concepts/datafix-system.md)
- [Schema System](../concepts/schema-system.md)
- [Quick Start](../getting-started/quick-start.md)

