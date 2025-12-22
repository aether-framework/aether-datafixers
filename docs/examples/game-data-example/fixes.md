# DataFix Implementations

Migration logic for the game data example.

## PlayerV1ToV2Fix (100 → 110)

Renames fields from v1 to v2 format:

```java
package com.example.game.data.fixes;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import com.example.game.data.TypeReferences;

/**
 * Migrates player data from v1.0.0 to v1.1.0.
 *
 * Changes:
 * - playerName → name
 * - xp → experience
 * - hp → health
 */
public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super(
            schemas,
            new DataVersion(100),  // from
            new DataVersion(110),  // to
            "player-v1-to-v2"      // fix name
        );
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        // Use Rules.all for independent renames (order doesn't matter)
        return Rules.all(
            Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
            Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),
            Rules.renameField(TypeReferences.PLAYER, "hp", "health")
        );
    }
}
```

## PlayerV2ToV3Fix (110 → 200)

Adds level field and nests position:

```java
package com.example.game.data.fixes;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import com.example.game.data.TypeReferences;

/**
 * Migrates player data from v1.1.0 to v2.0.0.
 *
 * Changes:
 * - Add level (calculated from experience / 100)
 * - Nest x, y, z into position object
 */
public class PlayerV2ToV3Fix extends SchemaDataFix {

    public PlayerV2ToV3Fix(SchemaRegistry schemas) {
        super(
            schemas,
            new DataVersion(110),  // from
            new DataVersion(200),  // to
            "player-v2-to-v3"      // fix name
        );
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        // Use Rules.seq for ordered operations
        return Rules.seq(
            // First: add level field
            Rules.addField(TypeReferences.PLAYER, "level", this::calculateLevel),

            // Then: nest position (depends on reading x, y, z first)
            Rules.transform(TypeReferences.PLAYER, this::nestPosition)
        );
    }

    /**
     * Calculate level from experience.
     * Level = experience / 100 (minimum 1)
     */
    private Dynamic<?> calculateLevel(Dynamic<?> player) {
        int experience = player.get("experience").asInt().orElse(0);
        int level = Math.max(1, experience / 100);
        return player.createInt(level);
    }

    /**
     * Nest x, y, z coordinates into a position object.
     */
    private Dynamic<?> nestPosition(Dynamic<?> player) {
        // Read current coordinates
        double x = player.get("x").asDouble().orElse(0.0);
        double y = player.get("y").asDouble().orElse(0.0);
        double z = player.get("z").asDouble().orElse(0.0);

        // Create nested position object
        Dynamic<?> position = player.emptyMap()
            .set("x", player.createDouble(x))
            .set("y", player.createDouble(y))
            .set("z", player.createDouble(z));

        // Remove old fields and add nested object
        return player
            .remove("x")
            .remove("y")
            .remove("z")
            .set("position", position);
    }
}
```

## WorldV1ToV2Fix (100 → 200)

Adds version field to world data:

```java
package com.example.game.data.fixes;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import com.example.game.data.TypeReferences;

/**
 * Migrates world data from v1.0.0 to v2.0.0.
 *
 * Changes:
 * - Add version field with default value 1
 */
public class WorldV1ToV2Fix extends SchemaDataFix {

    public WorldV1ToV2Fix(SchemaRegistry schemas) {
        super(
            schemas,
            new DataVersion(100),
            new DataVersion(200),
            "world-v1-to-v2"
        );
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.addField(
            TypeReferences.WORLD,
            "version",
            world -> world.createInt(1)  // Default version
        );
    }
}
```

## Fix Patterns Summary

### Field Renaming

```java
Rules.renameField(TYPE, "oldName", "newName")
```

### Adding Fields

```java
Rules.addField(TYPE, "fieldName", data -> data.createValue(...))
```

### Complex Transformations

```java
Rules.transform(TYPE, this::transformMethod)

private Dynamic<?> transformMethod(Dynamic<?> data) {
    // Transform and return
}
```

### Ordered Operations (seq)

```java
Rules.seq(
    Rules.renameField(...),    // First
    Rules.addField(...),        // Second
    Rules.transform(...)        // Third
)
```

### Independent Operations (all)

```java
Rules.all(
    Rules.renameField(...),     // Any order
    Rules.renameField(...),     // Any order
    Rules.renameField(...)      // Any order
)
```

## Related

- [Rewrite Rules](../../concepts/rewrite-rules.md)
- [DataFix System](../../concepts/datafix-system.md)
- [Bootstrap](bootstrap.md)

