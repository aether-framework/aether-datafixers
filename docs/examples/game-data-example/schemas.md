# Schema Classes

Version-specific type definitions for the game data example.

## Schema100 (Version 1.0.0)

Initial schema with original field names:

```java
package com.example.game.data.schemas;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import com.example.game.data.TypeReferences;

/**
 * Schema for game data version 1.0.0.
 *
 * Player format:
 * - playerName (string): Player's display name
 * - xp (int): Experience points
 * - hp (int): Health points
 * - x, y, z (double): Position coordinates
 */
public class Schema100 extends Schema {

    public Schema100(Schema parent) {
        super(new DataVersion(100), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("playerName", DSL.string()),
            DSL.field("xp", DSL.intType()),
            DSL.field("hp", DSL.intType()),
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType()),
            DSL.remainder()  // Preserve unknown fields
        ));

        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("seed", DSL.longType()),
            DSL.remainder()
        ));
    }
}
```

## Schema110 (Version 1.1.0)

Renamed fields for clarity:

```java
package com.example.game.data.schemas;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import com.example.game.data.TypeReferences;

/**
 * Schema for game data version 1.1.0.
 *
 * Changes from 1.0.0:
 * - playerName → name
 * - xp → experience
 * - hp → health
 */
public class Schema110 extends Schema {

    public Schema110(Schema parent) {
        super(new DataVersion(110), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),         // Renamed from playerName
            DSL.field("experience", DSL.intType()),  // Renamed from xp
            DSL.field("health", DSL.intType()),      // Renamed from hp
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType()),
            DSL.remainder()
        ));

        // World schema unchanged
        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("seed", DSL.longType()),
            DSL.remainder()
        ));
    }
}
```

## Schema200 (Version 2.0.0)

Added level field and nested position:

```java
package com.example.game.data.schemas;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import com.example.game.data.TypeReferences;

/**
 * Schema for game data version 2.0.0.
 *
 * Changes from 1.1.0:
 * - Added level (computed from experience)
 * - Nested x, y, z into position object
 */
public class Schema200 extends Schema {

    public Schema200(Schema parent) {
        super(new DataVersion(200), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("experience", DSL.intType()),
            DSL.field("health", DSL.intType()),
            DSL.field("level", DSL.intType()),       // New field
            DSL.field("position", DSL.and(           // Nested object
                DSL.field("x", DSL.doubleType()),
                DSL.field("y", DSL.doubleType()),
                DSL.field("z", DSL.doubleType()),
                DSL.remainder()
            )),
            DSL.remainder()
        ));

        // World schema adds version tracking
        registerType(TypeReferences.WORLD, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("seed", DSL.longType()),
            DSL.field("version", DSL.intType()),     // New field
            DSL.remainder()
        ));
    }
}
```

## Schema Evolution Summary

| Field | v1.0.0 (100) | v1.1.0 (110) | v2.0.0 (200) |
|-------|--------------|--------------|--------------|
| playerName | ✓ | → name | name |
| xp | ✓ | → experience | experience |
| hp | ✓ | → health | health |
| level | - | - | ✓ (new) |
| x, y, z | ✓ | ✓ | → position.x/y/z |

## DSL Patterns Used

### Required Fields

```java
DSL.field("name", DSL.string())  // Must exist
```

### Optional Fields

```java
DSL.optional("nickname", DSL.string())  // May not exist
```

### Nested Objects

```java
DSL.field("position", DSL.and(
    DSL.field("x", DSL.doubleType()),
    DSL.field("y", DSL.doubleType()),
    DSL.field("z", DSL.doubleType()),
    DSL.remainder()
))
```

### Preserve Unknown Fields

```java
DSL.remainder()  // Always include at end
```

## Related

- [DSL Reference](../../concepts/dsl.md)
- [Schema System](../../concepts/schema-system.md)
- [Fixes](fixes.md)

