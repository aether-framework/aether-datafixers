# Multi-Domain Setup

Multi-domain setups allow you to manage multiple independent DataFixer instances within a single application. Each domain has its own schema history, fixes, and version management, enabling different data types to evolve independently.

---

## When to Use Multi-Domain

Use multi-domain setups when:

- **Different data types evolve independently** — Game saves, user profiles, and configuration files may change at different rates
- **Isolated version histories** — You want clear separation between migration paths
- **Different migration strategies** — Some data types may require different approaches
- **Modular architecture** — Different teams manage different data types

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    DataFixerRegistry                         │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │    "game"    │  │    "user"    │  │   "world"    │       │
│  │  DataFixer   │  │  DataFixer   │  │  DataFixer   │       │
│  │   v1→v200    │  │   v1→v150    │  │   v1→v300    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    MigrationService                          │
│     .migrate(data).usingDomain("game").from(100).to(200)    │
└─────────────────────────────────────────────────────────────┘
```

---

## Step 1: Create Qualified Bootstrap Beans

Use `@Qualifier` to give each bootstrap a unique domain name:

```java
@Configuration
public class DataFixerConfiguration {

    @Bean
    @Qualifier("game")
    public DataFixerBootstrap gameBootstrap() {
        return new GameDataBootstrap();
    }

    @Bean
    @Qualifier("user")
    public DataFixerBootstrap userBootstrap() {
        return new UserDataBootstrap();
    }

    @Bean
    @Qualifier("world")
    public DataFixerBootstrap worldBootstrap() {
        return new WorldDataBootstrap();
    }
}
```

### Bootstrap Implementations

```java
public class GameDataBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(200);

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        schemas.register(new GameSchema100());
        schemas.register(new GameSchema200());
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(new GameV100ToV200Fix(
            fixes.getSchema(100),
            fixes.getSchema(200)
        ));
    }
}

public class UserDataBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(150);

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        schemas.register(new UserSchema100());
        schemas.register(new UserSchema150());
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(new UserV100ToV150Fix(
            fixes.getSchema(100),
            fixes.getSchema(150)
        ));
    }
}
```

---

## Step 2: Create Domain-Specific DataFixer Beans

Create `AetherDataFixer` beans for each domain using the factory method:

```java
@Configuration
public class DataFixerConfiguration {

    // Bootstrap beans from Step 1...

    @Bean("gameDataFixer")
    @Qualifier("game")
    public AetherDataFixer gameDataFixer(
            @Qualifier("game") DataFixerBootstrap bootstrap,
            AetherDataFixersProperties properties,
            DataFixerRegistry registry) {
        return DataFixerAutoConfiguration.createQualifiedFixer(
            bootstrap, "game", properties, registry);
    }

    @Bean("userDataFixer")
    @Qualifier("user")
    public AetherDataFixer userDataFixer(
            @Qualifier("user") DataFixerBootstrap bootstrap,
            AetherDataFixersProperties properties,
            DataFixerRegistry registry) {
        return DataFixerAutoConfiguration.createQualifiedFixer(
            bootstrap, "user", properties, registry);
    }

    @Bean("worldDataFixer")
    @Qualifier("world")
    @Primary  // Mark one as primary for unqualified injection
    public AetherDataFixer worldDataFixer(
            @Qualifier("world") DataFixerBootstrap bootstrap,
            AetherDataFixersProperties properties,
            DataFixerRegistry registry) {
        return DataFixerAutoConfiguration.createQualifiedFixer(
            bootstrap, "world", properties, registry);
    }
}
```

---

## Step 3: Configure Properties

Configure per-domain settings in `application.yml`:

```yaml
aether:
  datafixers:
    enabled: true

    domains:
      game:
        current-version: 200
        description: "Game save data migrations"

      user:
        current-version: 150
        description: "User profile migrations"

      world:
        current-version: 300
        primary: true  # Alternative to @Primary annotation
        description: "World chunk data migrations"
```

---

## Step 4: Use MigrationService with Domains

### Explicit Domain Selection

```java
@Service
public class MultiDomainService {

    private final MigrationService migrationService;

    public MultiDomainService(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public GameSave loadGameSave(TaggedDynamic<?> data, int version) {
        MigrationResult result = migrationService
            .migrate(data)
            .usingDomain("game")      // Select game domain
            .from(version)
            .toLatest()
            .execute();

        return result.isSuccess()
            ? deserializeGame(result.getData())
            : handleGameError(result);
    }

    public UserProfile loadUserProfile(TaggedDynamic<?> data, int version) {
        MigrationResult result = migrationService
            .migrate(data)
            .usingDomain("user")      // Select user domain
            .from(version)
            .toLatest()
            .execute();

        return result.isSuccess()
            ? deserializeUser(result.getData())
            : handleUserError(result);
    }
}
```

### Checking Domain Availability

```java
@Service
public class DynamicDomainService {

    private final MigrationService migrationService;

    public MigrationResult migrateForDomain(
            String domain,
            TaggedDynamic<?> data,
            int fromVersion) {

        // Check if domain exists
        if (!migrationService.hasDomain(domain)) {
            throw new IllegalArgumentException(
                "Unknown domain: " + domain +
                ". Available: " + migrationService.getAvailableDomains());
        }

        return migrationService
            .migrate(data)
            .usingDomain(domain)
            .from(fromVersion)
            .toLatest()
            .execute();
    }
}
```

---

## Direct DataFixer Injection

You can also inject DataFixer beans directly for fine-grained control:

### Using @Qualifier

```java
@Service
public class GameService {

    private final AetherDataFixer gameFixer;
    private final AetherDataFixer userFixer;

    public GameService(
            @Qualifier("game") AetherDataFixer gameFixer,
            @Qualifier("user") AetherDataFixer userFixer) {
        this.gameFixer = gameFixer;
        this.userFixer = userFixer;
    }

    public TaggedDynamic<?> migrateGameData(TaggedDynamic<?> data, DataVersion from) {
        return gameFixer.update(data, from, gameFixer.currentVersion());
    }
}
```

### Using Primary Bean

```java
@Service
public class DefaultService {

    // Injects the @Primary DataFixer (no qualifier needed)
    private final AetherDataFixer fixer;

    public DefaultService(AetherDataFixer fixer) {
        this.fixer = fixer;
    }
}
```

---

## Using DataFixerRegistry Directly

For dynamic domain lookup at runtime:

```java
@Service
public class RegistryBasedService {

    private final DataFixerRegistry registry;

    public RegistryBasedService(DataFixerRegistry registry) {
        this.registry = registry;
    }

    public TaggedDynamic<?> migrate(
            String domain,
            TaggedDynamic<?> data,
            DataVersion from) {

        // Get fixer for domain (throws if not found)
        AetherDataFixer fixer = registry.require(domain);

        return fixer.update(data, from, fixer.currentVersion());
    }

    public Set<String> listDomains() {
        return registry.getDomains();
    }

    public Map<String, Integer> getAllVersions() {
        Map<String, Integer> versions = new HashMap<>();
        for (String domain : registry.getDomains()) {
            AetherDataFixer fixer = registry.require(domain);
            versions.put(domain, fixer.currentVersion().getVersion());
        }
        return versions;
    }
}
```

---

## Domain-Specific Version Queries

```java
@Service
public class VersionService {

    private final MigrationService migrationService;

    public VersionService(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public Map<String, Integer> getCurrentVersions() {
        Map<String, Integer> versions = new HashMap<>();

        for (String domain : migrationService.getAvailableDomains()) {
            DataVersion version = migrationService.getCurrentVersion(domain);
            versions.put(domain, version.getVersion());
        }

        return versions;
    }

    public boolean needsMigration(String domain, int dataVersion) {
        DataVersion current = migrationService.getCurrentVersion(domain);
        return dataVersion < current.getVersion();
    }
}
```

---

## Actuator Response with Multiple Domains

### Health Endpoint

```bash
GET /actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "dataFixer": {
      "status": "UP",
      "details": {
        "totalDomains": 3,
        "game.status": "UP",
        "game.currentVersion": 200,
        "user.status": "UP",
        "user.currentVersion": 150,
        "world.status": "UP",
        "world.currentVersion": 300
      }
    }
  }
}
```

### DataFixer Endpoint

```bash
GET /actuator/datafixers
```

```json
{
  "domains": {
    "game": {
      "currentVersion": 200,
      "status": "UP"
    },
    "user": {
      "currentVersion": 150,
      "status": "UP"
    },
    "world": {
      "currentVersion": 300,
      "status": "UP"
    }
  }
}
```

### Domain-Specific Details

```bash
GET /actuator/datafixers/game
```

```json
{
  "domain": "game",
  "currentVersion": 200,
  "status": "UP"
}
```

---

## Metrics by Domain

Metrics are automatically tagged with the domain name:

```promql
# Success count by domain
sum(rate(aether_datafixers_migrations_success_total[5m])) by (domain)

# Average duration by domain
rate(aether_datafixers_migrations_duration_seconds_sum[5m])
  / rate(aether_datafixers_migrations_duration_seconds_count[5m])

# Filter by specific domain
aether_datafixers_migrations_success_total{domain="game"}
```

---

## Best Practices

### 1. Use Meaningful Domain Names

```java
// Good - descriptive names
@Qualifier("game-saves")
@Qualifier("user-profiles")
@Qualifier("server-config")

// Avoid - generic names
@Qualifier("domain1")
@Qualifier("data")
```

### 2. Mark One Domain as Primary

Avoid injection ambiguity by marking one domain as primary:

```java
@Bean
@Qualifier("game")
@Primary  // Used for unqualified injection
public AetherDataFixer gameDataFixer(...) { }
```

Or via configuration:

```yaml
aether:
  datafixers:
    domains:
      game:
        primary: true
```

### 3. Separate Type References

Keep TypeReference constants separate per domain:

```java
public final class GameTypeReferences {
    public static final TypeReference PLAYER = TypeReference.of("player");
    public static final TypeReference INVENTORY = TypeReference.of("inventory");
}

public final class UserTypeReferences {
    public static final TypeReference PROFILE = TypeReference.of("profile");
    public static final TypeReference SETTINGS = TypeReference.of("settings");
}
```

### 4. Use MigrationService Over Direct Injection

Prefer `MigrationService` for migrations — it provides metrics, error handling, and async support:

```java
// Preferred
migrationService.migrate(data).usingDomain("game").from(100).toLatest().execute();

// Only when needed for low-level access
@Qualifier("game") AetherDataFixer gameFixer;
```

---

## Complete Multi-Domain Example

```java
@Configuration
public class MultiDomainConfig {

    // Bootstraps
    @Bean @Qualifier("game")
    public DataFixerBootstrap gameBootstrap() {
        return new GameDataBootstrap();
    }

    @Bean @Qualifier("user")
    public DataFixerBootstrap userBootstrap() {
        return new UserDataBootstrap();
    }

    // DataFixers
    @Bean @Qualifier("game") @Primary
    public AetherDataFixer gameFixer(
            @Qualifier("game") DataFixerBootstrap bootstrap,
            AetherDataFixersProperties props,
            DataFixerRegistry registry) {
        return DataFixerAutoConfiguration.createQualifiedFixer(
            bootstrap, "game", props, registry);
    }

    @Bean @Qualifier("user")
    public AetherDataFixer userFixer(
            @Qualifier("user") DataFixerBootstrap bootstrap,
            AetherDataFixersProperties props,
            DataFixerRegistry registry) {
        return DataFixerAutoConfiguration.createQualifiedFixer(
            bootstrap, "user", props, registry);
    }
}

@Service
public class DataMigrationFacade {

    private final MigrationService migrationService;

    public DataMigrationFacade(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public GameSave migrateGame(TaggedDynamic<?> data, int version) {
        return migrate("game", data, version, this::deserializeGame);
    }

    public UserProfile migrateUser(TaggedDynamic<?> data, int version) {
        return migrate("user", data, version, this::deserializeUser);
    }

    private <T> T migrate(
            String domain,
            TaggedDynamic<?> data,
            int version,
            Function<TaggedDynamic<?>, T> deserializer) {

        MigrationResult result = migrationService
            .migrate(data)
            .usingDomain(domain)
            .from(version)
            .toLatest()
            .execute();

        if (result.isSuccess()) {
            return deserializer.apply(result.getData());
        }

        throw new MigrationException(
            String.format("Failed to migrate %s data from v%d", domain, version),
            result.getError().orElse(null)
        );
    }
}
```

---

## Related Documentation

- [Configuration Reference](configuration.md) — Per-domain property settings
- [MigrationService API](migration-service.md) — Using `usingDomain()` method
- [Actuator Integration](actuator.md) — Domain-specific health and endpoints
- [Metrics Integration](metrics.md) — Domain-tagged metrics
