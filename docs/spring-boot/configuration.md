# Configuration Reference

This document provides a complete reference for all configuration properties available in the `aether-datafixers-spring-boot-starter`.

All properties use the prefix `aether.datafixers`.

---

## Property Summary

| Property                  | Type    | Default | Description                       |
|---------------------------|---------|---------|-----------------------------------|
| `enabled`                 | boolean | `true`  | Enable/disable auto-configuration |
| `default-format`          | enum    | `GSON`  | Default serialization format      |
| `default-current-version` | Integer | `null`  | Fallback version for all domains  |
| `domains.<name>.*`        | Map     | `{}`    | Per-domain configuration          |
| `actuator.*`              | object  | —       | Actuator settings                 |
| `metrics.*`               | object  | —       | Metrics settings                  |

---

## Core Properties

### `aether.datafixers.enabled`

**Type**: `boolean`
**Default**: `true`

Enables or disables the entire Aether Datafixers auto-configuration. When set to `false`, no beans are created automatically.

```yaml
aether:
  datafixers:
    enabled: false  # Disable auto-configuration
```

**Use cases**:
- Disable in test profiles that don't need migration
- Disable when providing manual configuration

---

### `aether.datafixers.default-format`

**Type**: `enum (GSON, JACKSON)`
**Default**: `GSON`

Specifies the default serialization format when both Gson and Jackson are on the classpath.

```yaml
aether:
  datafixers:
    default-format: JACKSON
```

**Values**:
- `GSON` — Use `GsonOps` for JSON serialization
- `JACKSON` — Use `JacksonJsonOps` for JSON serialization

**Note**: The corresponding library must be on the classpath. If only one is present, it is used automatically regardless of this setting.

---

### `aether.datafixers.default-current-version`

**Type**: `Integer`
**Default**: `null`

Fallback version number used when a domain's version cannot be determined from domain-specific properties or the bootstrap's `CURRENT_VERSION` constant.

```yaml
aether:
  datafixers:
    default-current-version: 100
```

**Version Resolution Order**:
1. Domain-specific: `aether.datafixers.domains.<domain>.current-version`
2. Bootstrap constant: `public static final DataVersion CURRENT_VERSION`
3. This fallback value

If none of these provide a version, startup fails with `IllegalStateException`.

---

## Domain Properties

Configure per-domain settings for multi-domain setups. Each key under `domains` is a domain name that matches your `@Qualifier` annotation.

### `aether.datafixers.domains.<name>.current-version`

**Type**: `Integer`
**Default**: `null`

The current (latest) schema version for this specific domain. Overrides any `CURRENT_VERSION` constant in the bootstrap.

```yaml
aether:
  datafixers:
    domains:
      game:
        current-version: 200
      user:
        current-version: 150
```

---

### `aether.datafixers.domains.<name>.primary`

**Type**: `boolean`
**Default**: `false`

Marks this domain as the primary bean for unqualified injection. Only one domain should have this set to `true`.

```yaml
aether:
  datafixers:
    domains:
      game:
        current-version: 200
        primary: true     # This will be injected without @Qualifier
      user:
        current-version: 150
        primary: false
```

**Note**: In multi-domain setups, either mark one domain as primary or always use `@Qualifier` for injection.

---

### `aether.datafixers.domains.<name>.description`

**Type**: `String`
**Default**: `null`

Human-readable description of the domain's purpose. Exposed through actuator endpoints for operational visibility.

```yaml
aether:
  datafixers:
    domains:
      game:
        current-version: 200
        description: "Game save data migrations for player progress"
      user:
        current-version: 150
        description: "User profile schema evolution"
```

---

## Actuator Properties

Configure the behavior of Spring Boot Actuator integration.

### `aether.datafixers.actuator.include-schema-details`

**Type**: `boolean`
**Default**: `true`

Include schema version information in actuator responses. Disable in production if you don't want to expose internal versioning.

```yaml
aether:
  datafixers:
    actuator:
      include-schema-details: false
```

---

### `aether.datafixers.actuator.include-fix-details`

**Type**: `boolean`
**Default**: `true`

Include DataFix information in actuator responses. Disable if fix details should not be exposed.

```yaml
aether:
  datafixers:
    actuator:
      include-fix-details: false
```

---

## Metrics Properties

Configure Micrometer metrics collection for migration operations.

### `aether.datafixers.metrics.timing`

**Type**: `boolean`
**Default**: `true`

Enable timing metrics that record migration duration. Creates `Timer` metrics.

```yaml
aether:
  datafixers:
    metrics:
      timing: true
```

**Recorded metric**: `aether.datafixers.migrations.duration`

---

### `aether.datafixers.metrics.counting`

**Type**: `boolean`
**Default**: `true`

Enable counting metrics that track success/failure counts. Creates `Counter` metrics.

```yaml
aether:
  datafixers:
    metrics:
      counting: true
```

**Recorded metrics**:
- `aether.datafixers.migrations.success`
- `aether.datafixers.migrations.failure`

---

### `aether.datafixers.metrics.domain-tag`

**Type**: `String`
**Default**: `"domain"`

The tag name used to identify domains in metric labels.

```yaml
aether:
  datafixers:
    metrics:
      domain-tag: datafixer_domain
```

**Example metric with default tag**:
```
aether_datafixers_migrations_success_total{domain="game"} 42
```

**Example metric with custom tag**:
```
aether_datafixers_migrations_success_total{datafixer_domain="game"} 42
```

---

## Complete Configuration Example

### YAML Format

```yaml
aether:
  datafixers:
    # Core settings
    enabled: true
    default-format: GSON
    default-current-version: 100

    # Multi-domain configuration
    domains:
      game:
        current-version: 200
        primary: true
        description: "Game save data migrations"
      user:
        current-version: 150
        primary: false
        description: "User profile migrations"
      world:
        current-version: 300
        description: "World chunk data migrations"

    # Actuator settings
    actuator:
      include-schema-details: true
      include-fix-details: true

    # Metrics settings
    metrics:
      timing: true
      counting: true
      domain-tag: domain

# Spring Boot Actuator configuration
management:
  endpoints:
    web:
      exposure:
        include: health, info, datafixers
  endpoint:
    health:
      show-details: always
  info:
    env:
      enabled: true
```

### Properties Format

```properties
# Core settings
aether.datafixers.enabled=true
aether.datafixers.default-format=GSON
aether.datafixers.default-current-version=100

# Domain: game
aether.datafixers.domains.game.current-version=200
aether.datafixers.domains.game.primary=true
aether.datafixers.domains.game.description=Game save data migrations

# Domain: user
aether.datafixers.domains.user.current-version=150
aether.datafixers.domains.user.primary=false
aether.datafixers.domains.user.description=User profile migrations

# Actuator settings
aether.datafixers.actuator.include-schema-details=true
aether.datafixers.actuator.include-fix-details=true

# Metrics settings
aether.datafixers.metrics.timing=true
aether.datafixers.metrics.counting=true
aether.datafixers.metrics.domain-tag=domain

# Spring Boot Actuator
management.endpoints.web.exposure.include=health,info,datafixers
management.endpoint.health.show-details=always
```

---

## Environment-Specific Configuration

Use Spring profiles to override settings per environment:

### `application-dev.yml`

```yaml
aether:
  datafixers:
    actuator:
      include-schema-details: true
      include-fix-details: true
```

### `application-prod.yml`

```yaml
aether:
  datafixers:
    actuator:
      include-schema-details: false  # Hide internal details
      include-fix-details: false
```

---

## Programmatic Configuration

For advanced scenarios, you can override auto-configuration with explicit beans:

```java
@Configuration
public class DataFixerConfig {

    @Bean
    @Primary
    public AetherDataFixer customDataFixer(
            DataFixerBootstrap bootstrap,
            AetherDataFixersProperties properties,
            DataFixerRegistry registry) {

        // Custom version resolution logic
        DataVersion version = determineVersionFromExternalSource();

        AetherDataFixer fixer = new DataFixerRuntimeFactory()
            .create(version, bootstrap);

        registry.register("custom", fixer);
        return fixer;
    }

    private DataVersion determineVersionFromExternalSource() {
        // Load version from database, config server, etc.
        return new DataVersion(250);
    }
}
```

---

## Related Documentation

- [MigrationService API](migration-service.md) — Using the fluent migration API
- [Multi-Domain Setup](multi-domain.md) — Configuring multiple DataFixer domains
- [Actuator Integration](actuator.md) — Health indicators and endpoints
- [Metrics Integration](metrics.md) — Micrometer metrics reference
