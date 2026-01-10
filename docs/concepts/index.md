# Core Concepts

This section provides in-depth coverage of the fundamental concepts that power Aether Datafixers.

## Overview

Aether Datafixers is built around several interconnected concepts that work together to enable safe, versioned data migrations:

```
┌─────────────────────────────────────────────────────────────────┐
│                        DataFixer                                │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │   Schema    │───▶│   DataFix   │───▶│   Schema    │         │
│  │  (v1.0.0)   │    │  (v1→v2)    │    │  (v2.0.0)   │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
│         │                  │                  │                 │
│         ▼                  ▼                  ▼                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │TypeRegistry │    │RewriteRule  │    │TypeRegistry │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │  Dynamic<T>     │
                     │  + DynamicOps   │
                     └─────────────────┘
```

## Concept Categories

### Foundation Concepts

These are the building blocks you'll use in every migration:

| Concept                                  | Purpose                                 | Learn More |
|------------------------------------------|-----------------------------------------|------------|
| [DataVersion](data-version.md)           | Integer identifier for a schema version | →          |
| [TypeReference](type-reference.md)       | String identifier for a data type       | →          |
| [Architecture](architecture-overview.md) | How the framework fits together         | →          |

### Data Definition

How you define what your data looks like at each version:

| Concept                           | Purpose                                     | Learn More |
|-----------------------------------|---------------------------------------------|------------|
| [Schema System](schema-system.md) | Define type structures per version          | →          |
| [Type System](type-system.md)     | Type definitions and registries             | →          |
| [DSL](dsl.md)                     | Domain-specific language for type templates | →          |

### Migration System

How data gets transformed from one version to another:

| Concept                             | Purpose                              | Learn More |
|-------------------------------------|--------------------------------------|------------|
| [DataFix System](datafix-system.md) | Creating and applying migrations     | →          |
| [Rewrite Rules](rewrite-rules.md)   | Rule combinators for transformations | →          |

### Data Manipulation

Format-agnostic data handling:

| Concept                             | Purpose                          | Learn More |
|-------------------------------------|----------------------------------|------------|
| [Dynamic System](dynamic-system.md) | Format-agnostic data wrapper     | →          |
| [Codec System](codec-system.md)     | Encoding and decoding typed data | →          |

### Advanced Concepts

Composable data accessors and error handling:

| Concept                           | Purpose                             | Learn More |
|-----------------------------------|-------------------------------------|------------|
| [Optics](optics/index.md)         | Lens, Prism, Finder for data access | →          |
| [DataResult](data-result.md)      | Error handling with success/failure | →          |
| [Thread Safety](thread-safety.md) | Concurrency guarantees              | →          |

---

## How They Fit Together

### 1. Define Your Types

First, you create `TypeReference` constants to identify your data types:

```java
public static final TypeReference PLAYER = new TypeReference("player");
public static final TypeReference WORLD = new TypeReference("world");
```

### 2. Create Schemas for Each Version

Each `Schema` defines what types exist at a particular `DataVersion`:

```java
public class Schema100 extends Schema {
    public Schema100() {
        super(new DataVersion(100), null, SimpleTypeRegistry::new);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType())
        ));
    }
}
```

### 3. Create Fixes for Migrations

Each `DataFix` transforms data from one version to another:

```java
public class PlayerV1ToV2Fix extends SchemaDataFix {
    @Override
    protected TypeRewriteRule makeRule(Schema input, Schema output) {
        return Rules.renameField(TypeReferences.PLAYER, "xp", "experience");
    }
}
```

### 4. Wire Everything Together

A `DataFixerBootstrap` registers schemas and fixes:

```java
public class MyBootstrap implements DataFixerBootstrap {
    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        schemas.register(new Schema100());
        schemas.register(new Schema110(schema100));
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
    }
}
```

### 5. Apply Migrations

Use the `DataFixer` to migrate data:

```java
AetherDataFixer fixer = new DataFixerRuntimeFactory()
    .create(CURRENT_VERSION, new MyBootstrap());

TaggedDynamic migrated = fixer.update(
    oldData,
    new DataVersion(100),
    fixer.currentVersion()
);
```

---

## Recommended Reading Order

For newcomers, we recommend reading the concepts in this order:

1. **[Architecture Overview](architecture-overview.md)** — Understand the big picture
2. **[DataVersion](data-version.md)** — Version numbering
3. **[TypeReference](type-reference.md)** — Type identification
4. **[Schema System](schema-system.md)** — Defining data structures
5. **[DataFix System](datafix-system.md)** — Creating migrations
6. **[Dynamic System](dynamic-system.md)** — Data manipulation
7. **[DSL](dsl.md)** — Type template language
8. **[Rewrite Rules](rewrite-rules.md)** — Transformation rules
9. **[Codec System](codec-system.md)** — Encoding/decoding
10. **[Optics](optics/index.md)** — Advanced data access
11. **[DataResult](data-result.md)** — Error handling
12. **[Thread Safety](thread-safety.md)** — Concurrency

---

## Quick Reference

### Key Classes

| Class           | Module | Purpose                        |
|-----------------|--------|--------------------------------|
| `DataVersion`   | api    | Version identifier             |
| `TypeReference` | api    | Type identifier                |
| `Schema`        | api    | Type definitions for a version |
| `DataFix`       | api    | Migration interface            |
| `DataFixer`     | api    | Migration orchestrator         |
| `Dynamic`       | api    | Format-agnostic data wrapper   |
| `DynamicOps`    | api    | Operations for a format        |
| `Codec`         | api    | Bidirectional transformation   |
| `DSL`           | api    | Type template factory          |
| `Rules`         | api    | Rewrite rule factory           |

### Key Patterns

```java
// Version numbering (SemVer encoded)
new DataVersion(100)  // = v1.0.0
new DataVersion(110)  // = v1.1.0
new DataVersion(200)  // = v2.0.0

// Type references
TypeReference PLAYER = new TypeReference("player");

// DSL type templates
DSL.field("name", DSL.string())
DSL.optional("nickname", DSL.string())
DSL.list(DSL.string())
DSL.taggedChoice("type", entityTypes)

// Rewrite rules (basic)
Rules.renameField(TYPE, "old", "new")
Rules.transformField(TYPE, "field", transformer)
Rules.seq(rule1, rule2, rule3)

// Extended rules (batch, grouping, conditional)
Rules.renameFields(ops, Map.of("old1", "new1", "old2", "new2"))
Rules.groupFields(ops, "position", "x", "y", "z")
Rules.ifFieldExists(ops, "legacy", migrationRule)
```

---

## Next Steps

Start with the [Architecture Overview](architecture-overview.md) to understand how everything fits together, then proceed through the concepts in order.

