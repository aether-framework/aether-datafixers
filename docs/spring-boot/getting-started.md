# Spring Boot Quick Start Guide

This guide walks you through integrating Aether Datafixers into your Spring Boot application. By the end, you'll have a working data migration system with auto-configuration, health checks, and metrics.

## Prerequisites

- Java 17 or later
- Spring Boot 3.0 or later
- Maven or Gradle

---

## Step 1: Add Dependencies

### Maven

```xml
<dependencies>
    <!-- Core Spring Boot Starter -->
    <dependency>
        <groupId>de.splatgames.aether</groupId>
        <artifactId>aether-datafixers-spring-boot-starter</artifactId>
        <version>${aether-datafixers.version}</version>
    </dependency>

    <!-- For JSON support (choose one or both) -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
    </dependency>
    <!-- OR -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- Optional: Actuator for health checks and endpoints -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

### Gradle

```groovy
dependencies {
    implementation 'de.splatgames.aether:aether-datafixers-spring-boot-starter:${aetherDatafixersVersion}'

    // For JSON support (choose one or both)
    implementation 'com.google.code.gson:gson'
    // OR
    implementation 'com.fasterxml.jackson.core:jackson-databind'

    // Optional: Actuator for health checks and endpoints
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```

---

## Step 2: Define Your Type References

Create constants for your data types:

```java
package com.example.datafixer;

import de.splatgames.aether.datafixers.api.type.TypeReference;

public final class GameTypeReferences {

    public static final TypeReference PLAYER = TypeReference.of("player");
    public static final TypeReference INVENTORY = TypeReference.of("inventory");
    public static final TypeReference SETTINGS = TypeReference.of("settings");

    private GameTypeReferences() {}
}
```

---

## Step 3: Create Schema Definitions

Define schemas for each version of your data:

```java
package com.example.datafixer.schema;

import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import static com.example.datafixer.GameTypeReferences.*;

// Version 100 - Initial schema
public class Schema100 extends Schema {

    public Schema100() {
        super(100, null); // No parent schema
    }

    @Override
    protected void registerTypes() {
        registerType(PLAYER, DSL.and(
            DSL.field("playerName", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.field("xp", DSL.intType())
        ));
    }
}

// Version 200 - Renamed field, added new field
public class Schema200 extends Schema {

    public Schema200() {
        super(200, new Schema100()); // Inherits from Schema100
    }

    @Override
    protected void registerTypes() {
        // Only register types that changed
        registerType(PLAYER, DSL.and(
            DSL.field("name", DSL.string()),      // Renamed from playerName
            DSL.field("level", DSL.intType()),
            DSL.field("xp", DSL.intType()),
            DSL.field("health", DSL.intType())   // New field
        ));
    }
}
```

---

## Step 4: Create Data Fixes

Implement the migrations between versions:

```java
package com.example.datafixer.fix;

import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import static com.example.datafixer.GameTypeReferences.*;

/**
 * Migration from v100 to v200:
 * - Renames 'playerName' to 'name'
 * - Adds 'health' field with default value 100
 */
public class PlayerV100ToV200Fix extends SchemaDataFix {

    public PlayerV100ToV200Fix(Schema inputSchema, Schema outputSchema) {
        super(inputSchema, outputSchema);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            // Rename playerName -> name
            Rules.renameField(PLAYER, "playerName", "name"),

            // Add health field with default value
            Rules.transform(PLAYER, dynamic ->
                dynamic.set("health", dynamic.createInt(100))
            )
        );
    }
}
```

---

## Step 5: Create the Bootstrap Bean

Wire everything together with a Spring-managed bootstrap:

```java
package com.example.datafixer;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import com.example.datafixer.schema.*;
import com.example.datafixer.fix.*;
import org.springframework.stereotype.Component;

@Component
public class GameDataBootstrap implements DataFixerBootstrap {

    /**
     * Auto-detected by the starter for version resolution.
     */
    public static final DataVersion CURRENT_VERSION = new DataVersion(200);

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        schemas.register(new Schema100());
        schemas.register(new Schema200());
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        Schema schema100 = fixes.getSchema(100);
        Schema schema200 = fixes.getSchema(200);

        fixes.register(new PlayerV100ToV200Fix(schema100, schema200));
    }
}
```

---

## Step 6: Use the MigrationService

Inject and use the auto-configured `MigrationService`:

```java
package com.example.service;

import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.spring.service.MigrationService;
import de.splatgames.aether.datafixers.spring.service.MigrationResult;
import org.springframework.stereotype.Service;

@Service
public class PlayerDataService {

    private final MigrationService migrationService;

    public PlayerDataService(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /**
     * Loads player data and migrates it to the latest version if needed.
     */
    public Player loadPlayer(TaggedDynamic<?> savedData, int savedVersion) {
        // Skip migration if already at latest version
        int currentVersion = migrationService.getCurrentVersion().getVersion();
        if (savedVersion >= currentVersion) {
            return deserialize(savedData);
        }

        // Perform migration
        MigrationResult result = migrationService
            .migrate(savedData)
            .from(savedVersion)
            .toLatest()
            .execute();

        if (result.isSuccess()) {
            return deserialize(result.getData());
        }

        // Handle failure
        throw new PlayerLoadException(
            "Failed to migrate player data from v" + savedVersion,
            result.getError().orElse(null)
        );
    }

    private Player deserialize(TaggedDynamic<?> data) {
        // Deserialize the Dynamic to your domain object
        return new Player(
            data.get("name").asString("Unknown"),
            data.get("level").asInt(1),
            data.get("xp").asInt(0),
            data.get("health").asInt(100)
        );
    }
}
```

---

## Step 7: Configure Properties (Optional)

Add configuration to `application.yml`:

```yaml
aether:
  datafixers:
    enabled: true
    default-format: GSON    # or JACKSON

    # Actuator settings
    actuator:
      include-schema-details: true
      include-fix-details: true

    # Metrics settings
    metrics:
      timing: true
      counting: true
      domain-tag: domain

# Expose actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health, info, datafixers
  endpoint:
    health:
      show-details: always
```

---

## Step 8: Run Your Application

Start your Spring Boot application. You should see log messages indicating the DataFixer was created:

```
INFO  DataFixerAutoConfiguration : Creating primary AetherDataFixer from bootstrap: com.example.datafixer.GameDataBootstrap
INFO  DataFixerAutoConfiguration : AetherDataFixer created with current version: 200
```

---

## Verify the Setup

### Check Health Endpoint

```bash
curl http://localhost:8080/actuator/health
```

Response:

```json
{
  "status": "UP",
  "components": {
    "dataFixer": {
      "status": "UP",
      "details": {
        "totalDomains": 1,
        "default.status": "UP",
        "default.currentVersion": 200
      }
    }
  }
}
```

### Check DataFixer Endpoint

```bash
curl http://localhost:8080/actuator/datafixers
```

Response:

```json
{
  "domains": {
    "default": {
      "currentVersion": 200,
      "status": "UP"
    }
  }
}
```

### Check Info Endpoint

```bash
curl http://localhost:8080/actuator/info
```

Response includes:

```json
{
  "aether-datafixers": {
    "domains": 1,
    "domainDetails": {
      "default": {
        "currentVersion": 200
      }
    }
  }
}
```

---

## What's Next?

- [Configuration Reference](configuration.md) — Learn all available configuration options
- [MigrationService API](migration-service.md) — Deep dive into the fluent migration API
- [Multi-Domain Setup](multi-domain.md) — Configure multiple independent DataFixers
- [Actuator Integration](actuator.md) — Health checks, endpoints, and monitoring
- [Metrics Integration](metrics.md) — Track migration performance with Micrometer

---

## Common Issues

### "Cannot determine current version"

**Problem**: The starter cannot find the version for your DataFixer.

**Solutions**:
1. Add a `public static final DataVersion CURRENT_VERSION` field to your bootstrap
2. Configure `aether.datafixers.default-current-version` in properties
3. Configure `aether.datafixers.domains.<domain>.current-version`

### "No DataFixers registered"

**Problem**: Health check reports no DataFixers.

**Solutions**:
1. Ensure your bootstrap class has `@Component` annotation
2. Check that `aether.datafixers.enabled` is `true` (default)
3. Verify your bootstrap implements `DataFixerBootstrap`

### Migration fails with "Unknown domain"

**Problem**: `MigrationService.migrate()` throws exception for domain.

**Solutions**:
1. Check the domain name matches your `@Qualifier` value
2. Use `migrationService.getAvailableDomains()` to see registered domains
3. For single-bootstrap setups, don't call `.usingDomain()` (uses "default")
