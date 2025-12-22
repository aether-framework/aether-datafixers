# Basic Migration Tutorial

This tutorial walks you through creating a complete, working data migration from scratch. You'll learn the essential patterns for versioning and migrating data.

## Goal

Create a migration system that transforms player data from v1 to v2:

**v1 format:**
```json
{"playerName": "Steve", "xp": 1500}
```

**v2 format:**
```json
{"name": "Steve", "experience": 1500}
```

## Prerequisites

- Java 17+
- Maven or Gradle project
- Basic understanding of [Core Concepts](../concepts/index.md)

## Setup

Add the dependencies to your project:

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-core</artifactId>
    <version>0.1.0</version>
</dependency>
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-codec</artifactId>
    <version>0.1.0</version>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>
```

## Step 1: Create TypeReference

First, create a constant to identify your data type:

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.TypeReference;

public final class TypeReferences {

    /** Player save data */
    public static final TypeReference PLAYER = new TypeReference("player");

    private TypeReferences() {}
}
```

**Why?** TypeReference acts as a key for routing data to the correct schemas and fixes.

## Step 2: Create Schema for V1

Define what the data looks like at version 1:

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

public class Schema1 extends Schema {

    public Schema1() {
        super(
            new DataVersion(1),      // Version ID
            null,                     // No parent (root schema)
            SimpleTypeRegistry::new   // Registry factory
        );
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("playerName", DSL.string()),
            DSL.field("xp", DSL.intType()),
            DSL.remainder()  // Preserve any extra fields
        ));
    }
}
```

## Step 3: Create Schema for V2

Define the updated structure:

```java
package com.example.game.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import com.example.game.TypeReferences;

public class Schema2 extends Schema {

    public Schema2(Schema parent) {
        super(
            new DataVersion(2),      // Version ID
            parent,                   // Parent schema (Schema1)
            SimpleTypeRegistry::new
        );
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),        // Renamed
            DSL.field("experience", DSL.intType()), // Renamed
            DSL.remainder()
        ));
    }
}
```

## Step 4: Create the DataFix

Implement the migration logic:

```java
package com.example.game.fix;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import com.example.game.TypeReferences;

public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super(
            "player_v1_to_v2",     // Unique fix name
            new DataVersion(1),    // From version
            new DataVersion(2),    // To version
            schemas
        );
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            // Rename playerName → name
            Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
            // Rename xp → experience
            Rules.renameField(TypeReferences.PLAYER, "xp", "experience")
        );
    }
}
```

## Step 5: Create the Bootstrap

Wire everything together:

```java
package com.example.game;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import com.example.game.fix.PlayerV1ToV2Fix;
import com.example.game.schema.Schema1;
import com.example.game.schema.Schema2;

public class GameDataBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(2);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;

        // Create schema chain
        Schema1 v1 = new Schema1();
        Schema2 v2 = new Schema2(v1);

        // Register both
        schemas.register(v1);
        schemas.register(v2);
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
    }
}
```

## Step 6: Use the Fixer

Put it all together in your application:

```java
package com.example.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;

public class BasicMigrationExample {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        // 1. Create the data fixer
        AetherDataFixer fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

        // 2. Create old v1 data
        JsonObject oldData = new JsonObject();
        oldData.addProperty("playerName", "Steve");
        oldData.addProperty("xp", 1500);

        System.out.println("=== Original Data (v1) ===");
        System.out.println(GSON.toJson(oldData));

        // 3. Wrap in Dynamic and tag with type
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, oldData);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

        // 4. Migrate from v1 to v2
        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(1),
            GameDataBootstrap.CURRENT_VERSION
        );

        // 5. Extract and print result
        @SuppressWarnings("unchecked")
        Dynamic<JsonElement> result = (Dynamic<JsonElement>) migrated.value();
        System.out.println("\n=== Migrated Data (v2) ===");
        System.out.println(GSON.toJson(result.value()));
    }
}
```

## Expected Output

```
=== Original Data (v1) ===
{
  "playerName": "Steve",
  "xp": 1500
}

=== Migrated Data (v2) ===
{
  "name": "Steve",
  "experience": 1500
}
```

## Complete Project Structure

```
src/main/java/com/example/game/
├── TypeReferences.java        # Type identifiers
├── GameDataBootstrap.java     # Bootstrap configuration
├── BasicMigrationExample.java # Main example
├── schema/
│   ├── Schema1.java           # V1 type definitions
│   └── Schema2.java           # V2 type definitions
└── fix/
    └── PlayerV1ToV2Fix.java   # V1→V2 migration
```

## Testing the Migration

Create a unit test:

```java
package com.example.game;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerMigrationTest {

    private static AetherDataFixer fixer;

    @BeforeAll
    static void setup() {
        fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
    }

    @Test
    void testV1ToV2Migration() {
        // Given: V1 data
        JsonObject v1 = new JsonObject();
        v1.addProperty("playerName", "Steve");
        v1.addProperty("xp", 1500);

        // When: Migrate to V2
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, v1);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);
        TaggedDynamic migrated = fixer.update(tagged, new DataVersion(1), new DataVersion(2));

        // Then: V2 structure
        @SuppressWarnings("unchecked")
        Dynamic<JsonElement> result = (Dynamic<JsonElement>) migrated.value();

        assertEquals("Steve", result.get("name").asString().orElse(""));
        assertEquals(1500, result.get("experience").asInt().orElse(0));
        assertTrue(result.get("playerName").result().isEmpty()); // Old field removed
        assertTrue(result.get("xp").result().isEmpty()); // Old field removed
    }

    @Test
    void testNoMigrationNeededForCurrentVersion() {
        // Given: V2 data
        JsonObject v2 = new JsonObject();
        v2.addProperty("name", "Alex");
        v2.addProperty("experience", 2000);

        // When: "Migrate" from V2 to V2
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, v2);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);
        TaggedDynamic result = fixer.update(tagged, new DataVersion(2), new DataVersion(2));

        // Then: Data unchanged
        @SuppressWarnings("unchecked")
        Dynamic<JsonElement> resultDynamic = (Dynamic<JsonElement>) result.value();
        assertEquals("Alex", resultDynamic.get("name").asString().orElse(""));
        assertEquals(2000, resultDynamic.get("experience").asInt().orElse(0));
    }
}
```

## Key Takeaways

1. **TypeReference** identifies your data types
2. **Schema** defines the structure at each version
3. **DataFix** transforms data between versions
4. **Bootstrap** wires everything together
5. **DataFixer** orchestrates the migrations

## Common Mistakes

| Mistake | Solution |
|---------|----------|
| Forgetting `DSL.remainder()` | Always include to preserve unknown fields |
| Not chaining schemas | Pass parent schema to child constructor |
| Wrong version numbers | Ensure fix `fromVersion`/`toVersion` match schemas |
| Reusing version numbers | Each schema change needs a new version |

## Next Steps

- **[Multi-Version Migration](multi-version-migration.md)** — Chain multiple fixes
- **[Schema Inheritance](schema-inheritance.md)** — Organize large projects
- **[How-To: Rename Field](../how-to/rename-field.md)** — More rename patterns

