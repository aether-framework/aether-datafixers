# Bootstrap Creation

Wiring schemas and fixes together in a DataFixerBootstrap.

## GameDataBootstrap Implementation

```java
package com.example.game.data;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerBootstrap;
import com.example.game.data.schemas.*;
import com.example.game.data.fixes.*;

/**
 * Bootstrap for game data migrations.
 *
 * Registers all schemas and fixes needed to migrate
 * game data between versions.
 *
 * Version History:
 * - 100 (v1.0.0): Initial format
 * - 110 (v1.1.0): Renamed player fields
 * - 200 (v2.0.0): Added level, nested position
 */
public class GameDataBootstrap implements DataFixerBootstrap {

    /**
     * The current/target data version.
     */
    public static final DataVersion CURRENT_VERSION = new DataVersion(200);

    /**
     * All known data versions.
     */
    public static final DataVersion V1_0_0 = new DataVersion(100);
    public static final DataVersion V1_1_0 = new DataVersion(110);
    public static final DataVersion V2_0_0 = new DataVersion(200);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;

        // Register schemas in version order
        schemas.register(V1_0_0, Schema100::new);
        schemas.register(V1_1_0, Schema110::new);
        schemas.register(V2_0_0, Schema200::new);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Player fixes
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
        fixes.register(TypeReferences.PLAYER, new PlayerV2ToV3Fix(schemas));

        // World fixes
        fixes.register(TypeReferences.WORLD, new WorldV1ToV2Fix(schemas));
    }
}
```

## Creating the DataFixer

```java
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;

public class GameApplication {

    private final AetherDataFixer fixer;

    public GameApplication() {
        // Create the data fixer instance
        this.fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
    }

    public AetherDataFixer getFixer() {
        return fixer;
    }
}
```

## Singleton Pattern

```java
public class DataFixerProvider {

    private static volatile AetherDataFixer instance;

    public static AetherDataFixer getInstance() {
        if (instance == null) {
            synchronized (DataFixerProvider.class) {
                if (instance == null) {
                    instance = new DataFixerRuntimeFactory()
                        .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
                }
            }
        }
        return instance;
    }

    // Prevent instantiation
    private DataFixerProvider() {}
}
```

## Using the Fixer

```java
public class PlayerDataLoader {

    private final AetherDataFixer fixer;
    private final Gson gson = new Gson();

    public PlayerDataLoader() {
        this.fixer = DataFixerProvider.getInstance();
    }

    public PlayerData loadPlayer(Path file) throws IOException {
        // Read raw JSON
        JsonObject json;
        try (Reader reader = Files.newBufferedReader(file)) {
            json = gson.fromJson(reader, JsonObject.class);
        }

        // Get data version from file
        int version = json.has("_dataVersion")
            ? json.get("_dataVersion").getAsInt()
            : 100;  // Assume v1 if no version field

        // Migrate if needed
        Dynamic<JsonElement> data = new Dynamic<>(GsonOps.INSTANCE, json);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, data);

        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(version),
            GameDataBootstrap.CURRENT_VERSION
        );

        // Decode to typed object
        return PlayerCodec.INSTANCE.decode(migrated.value());
    }
}
```

## Registration Order

```
┌─────────────────────────────────────────────────────────────┐
│                    Bootstrap Execution                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. registerSchemas(SchemaRegistry)                         │
│     ├── Schema100 (v1.0.0) ─────────────────┐               │
│     ├── Schema110 (v1.1.0) ─────────────────┤ Schema Chain  │
│     └── Schema200 (v2.0.0) ─────────────────┘               │
│                                                              │
│  2. registerFixes(FixRegistrar)                             │
│     ├── PlayerV1ToV2Fix (100 → 110)                        │
│     ├── PlayerV2ToV3Fix (110 → 200)                        │
│     └── WorldV1ToV2Fix (100 → 200)                         │
│                                                              │
│  3. DataFixer Built                                          │
│     └── Ready for update() calls                            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Multiple Bootstraps

For modular systems, combine multiple bootstraps:

```java
public class CombinedBootstrap implements DataFixerBootstrap {

    private final List<DataFixerBootstrap> delegates;

    public CombinedBootstrap(DataFixerBootstrap... bootstraps) {
        this.delegates = List.of(bootstraps);
    }

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        for (DataFixerBootstrap bootstrap : delegates) {
            bootstrap.registerSchemas(schemas);
        }
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        for (DataFixerBootstrap bootstrap : delegates) {
            bootstrap.registerFixes(fixes);
        }
    }
}

// Usage
AetherDataFixer fixer = new DataFixerRuntimeFactory().create(
    CURRENT_VERSION,
    new CombinedBootstrap(
        new PlayerDataBootstrap(),
        new WorldDataBootstrap(),
        new ItemDataBootstrap()
    )
);
```

## Related

- [DataFix System](../../concepts/datafix-system.md)
- [Schema System](../../concepts/schema-system.md)
- [Complete Example](complete-example.md)

