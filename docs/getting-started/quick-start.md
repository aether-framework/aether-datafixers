# Quick Start

Get Aether Datafixers working in 5 minutes with this minimal example.

## The Scenario

You have player data in version 1 format:

```json
{
  "playerName": "Steve",
  "xp": 1500
}
```

You want to migrate it to version 2 format:

```json
{
  "name": "Steve",
  "experience": 1500
}
```

## Step 1: Define Type Reference

Create an identifier for your data type:

```java
import de.splatgames.aether.datafixers.api.TypeReference;

public class TypeReferences {
    public static final TypeReference PLAYER = new TypeReference("player");
}
```

## Step 2: Create a Simple Fix

Create a fix that renames the fields:

```java
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;

public class PlayerV1ToV2Fix implements DataFix<JsonElement> {

    @Override
    public String name() {
        return "player_v1_to_v2";
    }

    @Override
    public DataVersion fromVersion() {
        return new DataVersion(1);
    }

    @Override
    public DataVersion toVersion() {
        return new DataVersion(2);
    }

    @Override
    public Dynamic<JsonElement> apply(Dynamic<JsonElement> input) {
        // Read old fields
        String name = input.get("playerName").asString().orElse("Unknown");
        int xp = input.get("xp").asInt().orElse(0);

        // Create new structure
        return input
            .remove("playerName")
            .remove("xp")
            .set("name", input.createString(name))
            .set("experience", input.createInt(xp));
    }
}
```

## Step 3: Create Bootstrap

Wire everything together:

```java
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;

public class MyBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(2);

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        // For this simple example, we skip schema definitions
        // See "Your First Migration" for complete schema usage
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix());
    }
}
```

## Step 4: Apply the Migration

```java
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;

public class QuickStartExample {

    public static void main(String[] args) {
        // 1. Create the data fixer
        AetherDataFixer fixer = new DataFixerRuntimeFactory()
            .create(MyBootstrap.CURRENT_VERSION, new MyBootstrap());

        // 2. Create old v1 data
        JsonObject oldData = new JsonObject();
        oldData.addProperty("playerName", "Steve");
        oldData.addProperty("xp", 1500);

        System.out.println("Before: " + oldData);

        // 3. Wrap in Dynamic
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, oldData);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

        // 4. Apply migration
        TaggedDynamic result = fixer.update(
            tagged,
            new DataVersion(1),  // from version
            new DataVersion(2)   // to version
        );

        // 5. Get result
        System.out.println("After: " + result.value().value());
    }
}
```

## Output

```
Before: {"playerName":"Steve","xp":1500}
After: {"name":"Steve","experience":1500}
```

## What Just Happened?

1. **DataFixerRuntimeFactory** created a configured `AetherDataFixer` from our bootstrap
2. **Dynamic** wrapped our JSON data with format-agnostic operations
3. **TaggedDynamic** associated the data with its type (`PLAYER`)
4. **fixer.update()** found the `PlayerV1ToV2Fix` and applied it
5. The fix transformed the data structure

## Key Points

- **DataVersion** is just an integer — use any numbering scheme (e.g., 1, 2, 3 or 100, 200, 300)
- **TypeReference** routes data to the correct fixes
- **Dynamic** operations are immutable — they return new instances
- **DataFix** defines the actual transformation logic

---

## Next Steps

This was a minimal example. For production use, you'll want:

- **Schema definitions** for type safety
- **SchemaDataFix** base class for rule-based transformations
- **DSL** for declaring type structures

→ [Your First Migration](your-first-migration.md) — Complete tutorial with schemas

→ [Concepts Overview](../concepts/index.md) — Understand the framework in depth
