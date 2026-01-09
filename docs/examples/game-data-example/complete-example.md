# Complete Example

Full working code for the game data migration example.

## Project Structure

```
src/main/java/com/example/game/
├── data/
│   ├── TypeReferences.java
│   ├── GameDataBootstrap.java
│   ├── schemas/
│   │   ├── Schema100.java
│   │   ├── Schema110.java
│   │   └── Schema200.java
│   └── fixes/
│       ├── PlayerV1ToV2Fix.java
│       ├── PlayerV2ToV3Fix.java
│       └── WorldV1ToV2Fix.java
├── model/
│   ├── Player.java
│   └── Position.java
└── GameExample.java
```

## pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>game-data-migration</artifactId>
    <version>1.0.0</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <aether.version>1.0.0</aether.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>de.splatgames.aether</groupId>
                <artifactId>aether-datafixers-bom</artifactId>
                <version>${aether.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>de.splatgames.aether</groupId>
            <artifactId>aether-datafixers-core</artifactId>
        </dependency>
        <dependency>
            <groupId>de.splatgames.aether</groupId>
            <artifactId>aether-datafixers-codec</artifactId>
        </dependency>
    </dependencies>
</project>
```

## TypeReferences.java

```java
package com.example.game.data;

import de.splatgames.aether.datafixers.api.TypeReference;

public final class TypeReferences {
    public static final TypeReference PLAYER = TypeReference.of("player");
    public static final TypeReference WORLD = TypeReference.of("world");

    private TypeReferences() {}
}
```

## Schema100.java

```java
package com.example.game.data.schemas;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import com.example.game.data.TypeReferences;

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
            DSL.remainder()
        ));
    }
}
```

## Schema110.java

```java
package com.example.game.data.schemas;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import com.example.game.data.TypeReferences;

public class Schema110 extends Schema {

    public Schema110(Schema parent) {
        super(new DataVersion(110), parent);
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("experience", DSL.intType()),
            DSL.field("health", DSL.intType()),
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType()),
            DSL.remainder()
        ));
    }
}
```

## Schema200.java

```java
package com.example.game.data.schemas;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import com.example.game.data.TypeReferences;

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
            DSL.field("level", DSL.intType()),
            DSL.field("position", DSL.and(
                DSL.field("x", DSL.doubleType()),
                DSL.field("y", DSL.doubleType()),
                DSL.field("z", DSL.doubleType()),
                DSL.remainder()
            )),
            DSL.remainder()
        ));
    }
}
```

## PlayerV1ToV2Fix.java

```java
package com.example.game.data.fixes;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import com.example.game.data.TypeReferences;

public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(100), new DataVersion(110), "player-v1-to-v2");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.all(
            Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
            Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),
            Rules.renameField(TypeReferences.PLAYER, "hp", "health")
        );
    }
}
```

## PlayerV2ToV3Fix.java

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

public class PlayerV2ToV3Fix extends SchemaDataFix {

    public PlayerV2ToV3Fix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(110), new DataVersion(200), "player-v2-to-v3");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            Rules.addField(TypeReferences.PLAYER, "level", this::calculateLevel),
            Rules.transform(TypeReferences.PLAYER, this::nestPosition)
        );
    }

    private Dynamic<?> calculateLevel(Dynamic<?> player) {
        int experience = player.get("experience").asInt().orElse(0);
        return player.createInt(Math.max(1, experience / 100));
    }

    private Dynamic<?> nestPosition(Dynamic<?> player) {
        double x = player.get("x").asDouble().orElse(0.0);
        double y = player.get("y").asDouble().orElse(0.0);
        double z = player.get("z").asDouble().orElse(0.0);

        Dynamic<?> position = player.emptyMap()
            .set("x", player.createDouble(x))
            .set("y", player.createDouble(y))
            .set("z", player.createDouble(z));

        return player.remove("x").remove("y").remove("z").set("position", position);
    }
}
```

## GameDataBootstrap.java

```java
package com.example.game.data;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerBootstrap;
import com.example.game.data.schemas.*;
import com.example.game.data.fixes.*;

public class GameDataBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(200);

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
```

## GameExample.java (Main)

```java
package com.example.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import com.example.game.data.GameDataBootstrap;
import com.example.game.data.TypeReferences;

public class GameExample {

    public static void main(String[] args) {
        // Create the data fixer
        AetherDataFixer fixer = new DataFixerRuntimeFactory()
            .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());

        // Simulate loading v1 player data
        JsonObject v1Player = new JsonObject();
        v1Player.addProperty("playerName", "Steve");
        v1Player.addProperty("xp", 1500);
        v1Player.addProperty("hp", 20);
        v1Player.addProperty("x", 100.5);
        v1Player.addProperty("y", 64.0);
        v1Player.addProperty("z", -200.0);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        System.out.println("=== Version 1 Data ===");
        System.out.println(gson.toJson(v1Player));

        // Wrap in Dynamic
        Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, v1Player);
        TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);

        // Migrate v1 → v2 (current)
        TaggedDynamic migrated = fixer.update(
            tagged,
            new DataVersion(100),               // from v1
            GameDataBootstrap.CURRENT_VERSION   // to current
        );

        // Get result
        JsonElement result = (JsonElement) migrated.value().value();

        System.out.println("\n=== Migrated to Version 2 ===");
        System.out.println(gson.toJson(result));

        // Read values from migrated data
        Dynamic<?> output = migrated.value();
        String name = output.get("name").asString().orElse("Unknown");
        int level = output.get("level").asInt().orElse(0);
        double x = output.get("position").get("x").asDouble().orElse(0.0);

        System.out.println("\n=== Extracted Values ===");
        System.out.println("Name: " + name);
        System.out.println("Level: " + level);
        System.out.println("Position X: " + x);
    }
}
```

## Output

```
=== Version 1 Data ===
{
  "playerName": "Steve",
  "xp": 1500,
  "hp": 20,
  "x": 100.5,
  "y": 64.0,
  "z": -200.0
}

=== Migrated to Version 2 ===
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

=== Extracted Values ===
Name: Steve
Level: 15
Position X: 100.5
```

## Related

- [Game Data Example Overview](index.md)
- [Basic Migration Tutorial](../../tutorials/basic-migration.md)
- [Quick Start Guide](../../getting-started/quick-start.md)

