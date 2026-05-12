# Examples

Practical examples demonstrating Aether Datafixers in real-world scenarios.

## Complete Examples

### 🎮 [Game Data Example](game-data-example/index.md)

A comprehensive example showing player data migration across multiple versions:

- [TypeReferences Pattern](game-data-example/type-references.md) - Centralized type identifiers
- [Schema Classes](game-data-example/schemas.md) - Version-specific type definitions
- [DataFix Implementations](game-data-example/fixes.md) - Migration logic
- [Bootstrap Creation](game-data-example/bootstrap.md) - Wiring it all together
- [Complete Example](game-data-example/complete-example.md) - Full working code

### 👤 [User Profile Example](user-profile-example.md)

Migrating user profile data with nested objects and optional fields.

### ⚙️ [Configuration Example](configuration-example.md)

Migrating application configuration files with defaults and restructuring.

### 🔷 [Entity Polymorphism Example](entity-polymorphism-example.md)

Handling polymorphic data with TaggedChoice for different entity types.

## Quick Reference Examples

### Minimal Migration

```java
// 1. Define type reference
TypeReference PLAYER = TypeReference.of("player");

// 2. Create bootstrap
DataFixerBootstrap bootstrap = new DataFixerBootstrap() {
    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        schemas.register(new DataVersion(1), parent -> {
            Schema schema = new Schema(new DataVersion(1), parent);
            schema.registerType(PLAYER, DSL.and(
                DSL.field("name", DSL.string()),
                DSL.remainder()
            ));
            return schema;
        });
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // No fixes for single version
    }
};

// 3. Create fixer
AetherDataFixer fixer = new DataFixerRuntimeFactory()
    .create(new DataVersion(1), bootstrap);
```

### Simple Field Rename

```java
public class RenameFieldFix extends SchemaDataFix {

    public RenameFieldFix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(1), new DataVersion(2), "rename-field");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.renameField(TypeReferences.PLAYER, "oldName", "newName");
    }
}
```

### Add Field with Default

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.addField(
        TypeReferences.PLAYER,
        "newField",
        player -> player.createInt(100)  // Default value
    );
}
```

### Transform Field Value

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.transformField(
        TypeReferences.PLAYER,
        "gameMode",
        mode -> {
            int modeInt = mode.asInt().orElse(0);
            String modeStr = switch (modeInt) {
                case 0 -> "survival";
                case 1 -> "creative";
                default -> "unknown";
            };
            return mode.createString(modeStr);
        }
    );
}
```

### Compose Multiple Rules

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        Rules.renameField(TypeReferences.PLAYER, "hp", "health"),
        Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),
        Rules.addField(TypeReferences.PLAYER, "level", p -> p.createInt(1))
    );
}
```

### Nest Fields

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.transform(TypeReferences.PLAYER, player -> {
        double x = player.get("x").asDouble().orElse(0.0);
        double y = player.get("y").asDouble().orElse(0.0);
        double z = player.get("z").asDouble().orElse(0.0);

        Dynamic<?> position = player.emptyMap()
            .set("x", player.createDouble(x))
            .set("y", player.createDouble(y))
            .set("z", player.createDouble(z));

        return player
            .remove("x").remove("y").remove("z")
            .set("position", position);
    });
}
```

## Example Project Structure

```
my-game/
├── src/main/java/
│   └── com/example/game/
│       ├── data/
│       │   ├── TypeReferences.java      # Type identifiers
│       │   ├── GameDataBootstrap.java   # Bootstrap
│       │   ├── schemas/
│       │   │   ├── Schema100.java       # v1.0.0 schema
│       │   │   ├── Schema110.java       # v1.1.0 schema
│       │   │   └── Schema200.java       # v2.0.0 schema
│       │   └── fixes/
│       │       ├── PlayerV1ToV2Fix.java
│       │       └── PlayerV2ToV3Fix.java
│       └── GameApplication.java
└── pom.xml
```

## Running the Examples

The examples module in the repository contains runnable examples:

```bash
cd aether-datafixers-examples
mvn compile exec:java -Dexec.mainClass="de.splatgames.aether.datafixers.examples.game.GameExample"
```

## Related

- [Quick Start](../getting-started/quick-start.md)
- [Basic Migration Tutorial](../tutorials/basic-migration.md)
- [DataFix System](../concepts/datafix-system.md)

