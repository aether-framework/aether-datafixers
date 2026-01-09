# Spring Boot Integration

The `aether-datafixers-spring-boot-starter` module provides seamless integration between Aether Datafixers and Spring Boot applications. It offers auto-configuration, a fluent migration API, multi-domain support, Actuator endpoints, and Micrometer metrics integration.

## Key Features

| Feature                  | Description                                                              |
|--------------------------|--------------------------------------------------------------------------|
| **Auto-Configuration**   | Automatic setup of DataFixer beans from `DataFixerBootstrap` definitions |
| **Fluent Migration API** | High-level `MigrationService` with builder pattern                       |
| **Multi-Domain Support** | Independent DataFixer instances for different data types                 |
| **Actuator Integration** | Health indicators, info contributors, and custom endpoints               |
| **Micrometer Metrics**   | Success/failure counters, timing, and version span tracking              |
| **Async Execution**      | Non-blocking migration with `CompletableFuture` support                  |

---

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-spring-boot-starter</artifactId>
    <version>${aether-datafixers.version}</version>
</dependency>
```

### 2. Create a Bootstrap Bean

```java
@Component
public class GameDataBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(200);

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        schemas.register(new Schema100());
        schemas.register(new Schema200());
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(new RenamePlayerNameFix());
        fixes.register(new AddHealthFieldFix());
    }
}
```

### 3. Use the MigrationService

```java
@Service
public class GameDataService {

    private final MigrationService migrationService;

    public GameDataService(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public GameSave loadAndMigrate(TaggedDynamic<?> data, int version) {
        MigrationResult result = migrationService
            .migrate(data)
            .from(version)
            .toLatest()
            .execute();

        if (result.isSuccess()) {
            return deserialize(result.getData());
        }
        throw new MigrationException("Migration failed",
            result.getError().orElse(null));
    }
}
```

---

## Documentation Sections

### Getting Started

- [Quick Start Guide](getting-started.md) — Add the starter to your project and run your first migration

### Configuration

- [Configuration Reference](configuration.md) — Complete reference for all `aether.datafixers.*` properties

### Core Features

- [MigrationService API](migration-service.md) — Fluent API for executing migrations
- [Multi-Domain Setup](multi-domain.md) — Managing multiple independent DataFixer instances

### Observability

- [Actuator Integration](actuator.md) — Health indicators, info contributors, and custom endpoints
- [Metrics Integration](metrics.md) — Micrometer metrics for migration observability

---

## Architecture Overview

The Spring Boot starter organizes components into the following layers:

```
┌──────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│   ┌──────────────────────────────────────────────────────┐   │
│   │           MigrationService (Fluent API)              │   │
│   │   migrate(data).from(100).toLatest().execute()       │   │
│   └──────────────────────────────────────────────────────┘   │
├──────────────────────────────────────────────────────────────┤
│                     Service Layer                            │
│   ┌──────────────────┐  ┌─────────────────────────────────┐  │
│   │ DataFixerRegistry│  │       MigrationMetrics          │  │
│   │  (Multi-Domain)  │  │     (Micrometer Integration)    │  │
│   └──────────────────┘  └─────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────┤
│                      Core Layer                              │
│   ┌─────────────────┐  ┌──────────────────────────────────┐  │
│   │ AetherDataFixer │  │     DataFixerBootstrap           │  │
│   │  (Per Domain)   │  │   (Schema + Fix Registration)    │  │
│   └─────────────────┘  └──────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────┤
│                   Actuator Layer                             │
│   ┌──────────────┐  ┌───────────────┐  ┌──────────────────┐  │
│   │    Health    │  │InfoContributor│  │ DataFixerEndpoint│  │
│   │  Indicator   │  │               │  │                  │  │
│   │   /health    │  │    /info      │  │  /datafixers     │  │
│   └──────────────┘  └───────────────┘  └──────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

## Auto-Configuration Classes

| Configuration Class                 | Purpose                                                     |
|-------------------------------------|-------------------------------------------------------------|
| `AetherDataFixersAutoConfiguration` | Main entry point, imports all sub-configurations            |
| `DynamicOpsAutoConfiguration`       | Creates `GsonOps` and `JacksonJsonOps` beans                |
| `DataFixerAutoConfiguration`        | Creates `AetherDataFixer` beans from bootstraps             |
| `MigrationServiceAutoConfiguration` | Creates the `MigrationService` bean                         |
| `ActuatorAutoConfiguration`         | Creates health indicators, info contributors, and endpoints |

---

## Conditional Activation

The auto-configuration activates when:

1. **Property Enabled**: `aether.datafixers.enabled=true` (default: `true`)
2. **Bootstrap Present**: At least one `DataFixerBootstrap` bean exists

To disable auto-configuration:

```yaml
aether:
  datafixers:
    enabled: false
```

---

## Requirements

| Requirement      | Version |
|------------------|---------|
| Java             | 17+     |
| Spring Boot      | 3.0+    |
| Spring Framework | 6.0+    |

### Optional Dependencies

| Dependency                     | Purpose                                                |
|--------------------------------|--------------------------------------------------------|
| `spring-boot-starter-actuator` | Health indicators, info contributors, custom endpoints |
| `micrometer-core`              | Migration metrics recording                            |
| `gson`                         | JSON support via `GsonOps`                             |
| `jackson-databind`             | JSON support via `JacksonJsonOps`                      |

---

## Next Steps

Start with the [Quick Start Guide](getting-started.md) to add the starter to your project and execute your first migration.
