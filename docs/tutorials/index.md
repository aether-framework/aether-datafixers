# Tutorials

This section contains step-by-step tutorials that guide you through common tasks with Aether Datafixers. Each tutorial builds on the concepts from the [Core Concepts](../concepts/index.md) section.

## Tutorial List

### Getting Started

| Tutorial | Description | Prerequisites |
|----------|-------------|---------------|
| [Basic Migration](basic-migration.md) | Create your first complete migration | Concepts overview |
| [Multi-Version Migration](multi-version-migration.md) | Chain multiple migrations | Basic migration |
| [Schema Inheritance](schema-inheritance.md) | Use parent-child schemas | Multi-version migration |

### Working with Codecs

| Tutorial | Description | Prerequisites |
|----------|-------------|---------------|
| [Using Codecs](using-codecs.md) | Build and use codecs | Dynamic system |
| [RecordCodecBuilder](record-codec-builder.md) | Create complex record codecs | Using codecs |

### Advanced Patterns

| Tutorial | Description | Prerequisites |
|----------|-------------|---------------|
| [Polymorphic Data](polymorphic-data.md) | Handle sum types with TaggedChoice | Schema system, DSL |
| [Nested Transformations](nested-transformations.md) | Transform deeply nested data | Rewrite rules |
| [Custom DynamicOps](custom-dynamicops.md) | Support new data formats | Dynamic system |

## Learning Path

### For Beginners

If you're new to data migration, follow this path:

1. **[Basic Migration](basic-migration.md)** — Learn the fundamentals
2. **[Multi-Version Migration](multi-version-migration.md)** — Handle version chains
3. **[Schema Inheritance](schema-inheritance.md)** — Organize your schemas

### For JSON/Data Processing

If you're focused on data processing:

1. **[Using Codecs](using-codecs.md)** — Encode and decode data
2. **[RecordCodecBuilder](record-codec-builder.md)** — Build complex codecs
3. **[Custom DynamicOps](custom-dynamicops.md)** — Add format support

### For Complex Migrations

If you need advanced migration patterns:

1. **[Schema Inheritance](schema-inheritance.md)** — Efficient schema organization
2. **[Polymorphic Data](polymorphic-data.md)** — Handle type variants
3. **[Nested Transformations](nested-transformations.md)** — Deep restructuring

## Tutorial Format

Each tutorial follows a consistent format:

1. **Goal** — What you'll accomplish
2. **Prerequisites** — What you need to know
3. **Setup** — Project configuration
4. **Step-by-Step Instructions** — Detailed walkthrough
5. **Complete Code** — Full working example
6. **Testing** — How to verify it works
7. **Next Steps** — Where to go from here

## Quick Reference

### Key Imports

```java
// Core types
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;

// Schema
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;

// Dynamic
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;

// DSL
import de.splatgames.aether.datafixers.api.dsl.DSL;

// Fix
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;

// Rules
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;

// Codec
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;

// Bootstrap
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
```

### Common Patterns

```java
// Create a fixer
AetherDataFixer fixer = new DataFixerRuntimeFactory()
    .create(CURRENT_VERSION, new MyBootstrap());

// Migrate data
Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonData);
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);
TaggedDynamic migrated = fixer.update(tagged, oldVersion, newVersion);

// Extract result
JsonElement result = ((Dynamic<JsonElement>) migrated.value()).value();
```

---

## Need Help?

- **Concepts unclear?** Review the [Core Concepts](../concepts/index.md)
- **Looking for specific tasks?** Check the [How-To Guides](../how-to/index.md)
- **Want working examples?** See the [Examples](../examples/index.md)
- **API details?** Consult the [API Reference](https://software.splatgames.de/docs/aether/aether-datafixers/)

