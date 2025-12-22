# Game Data Example

A complete example demonstrating player data migration across three versions.

## Overview

This example shows how to migrate game player data:

| Version | Changes |
|---------|---------|
| v1 (100) | Initial format with `playerName`, `xp`, `hp` |
| v2 (110) | Renamed fields to `name`, `experience`, `health` |
| v3 (200) | Added `level` computed from experience |

## Data Evolution

### Version 1 Data

```json
{
  "playerName": "Steve",
  "xp": 1500,
  "hp": 20,
  "x": 100.5,
  "y": 64.0,
  "z": -200.0
}
```

### Version 2 Data

```json
{
  "name": "Steve",
  "experience": 1500,
  "health": 20,
  "x": 100.5,
  "y": 64.0,
  "z": -200.0
}
```

### Version 3 Data

```json
{
  "name": "Steve",
  "experience": 1500,
  "health": 20,
  "level": 15,
  "position": {
    "x": 100.5,
    "y": 64.0,
    "z": -200.0
  }
}
```

## Components

This example consists of:

1. **[TypeReferences](type-references.md)** — Centralized type identifiers
2. **[Schemas](schemas.md)** — Version-specific type definitions
3. **[Fixes](fixes.md)** — Migration logic
4. **[Bootstrap](bootstrap.md)** — Wiring it all together
5. **[Complete Example](complete-example.md)** — Full working code

## Quick Start

```java
// Create fixer
AetherDataFixer fixer = new DataFixerRuntimeFactory()
    .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

// Parse old data
JsonObject v1Data = parseOldPlayerFile();
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, v1Data);
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

// Migrate to current version
TaggedDynamic migrated = fixer.update(
    tagged,
    new DataVersion(100),  // from v1
    new DataVersion(200)   // to v3
);

// Use migrated data
Dynamic<?> result = migrated.value();
String name = result.get("name").asString().orElse("Unknown");
int level = result.get("level").asInt().orElse(1);
```

## Project Structure

```
com.example.game.data/
├── TypeReferences.java         # Type identifiers
├── GameDataBootstrap.java      # Bootstrap implementation
├── schemas/
│   ├── Schema100.java          # v1.0.0 schema
│   ├── Schema110.java          # v1.1.0 schema
│   └── Schema200.java          # v2.0.0 schema
└── fixes/
    ├── PlayerV1ToV2Fix.java    # v1 → v2 migration
    └── PlayerV2ToV3Fix.java    # v2 → v3 migration
```

## Key Concepts Demonstrated

- **Field Renaming**: `playerName` → `name`
- **Field Transformation**: Computing `level` from `experience`
- **Structural Changes**: Nesting coordinates into `position` object
- **Version Chaining**: v1 → v2 → v3 automatic chain

## Related

- [Basic Migration Tutorial](../../tutorials/basic-migration.md)
- [Multi-Version Migration](../../tutorials/multi-version-migration.md)
- [Schema System](../../concepts/schema-system.md)

