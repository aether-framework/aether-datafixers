# 🚀 **Aether Datafixers v0.4.0 — Spring Boot Integration & Multi-Format DynamicOps**

Spring Boot auto-configuration with fluent MigrationService API, Actuator integration, and comprehensive multi-format support for YAML, TOML, and XML.

---

## 🎯 Highlights in v0.4.0

- ✅ **Spring Boot Starter** — New `aether-datafixers-spring-boot-starter` module with auto-configuration, fluent `MigrationService` API, multi-domain support, Actuator health/info/endpoints, and Micrometer metrics
- ✅ **Multi-Format DynamicOps** — New DynamicOps implementations for YAML (SnakeYAML, Jackson), TOML (Jackson), and XML (Jackson)
- ✅ **Package Restructuring** — Format-first package organization (`codec.json.gson`, `codec.yaml.jackson`, etc.)
- ✅ **Comprehensive Documentation** — New Spring Boot integration docs and codec format guides

---

## 📦 Installation

> [!TIP]
> All Aether artifacts are available on **Maven Central** — no extra repository required.

### Maven

```xml
<dependency>
  <groupId>de.splatgames.aether.datafixers</groupId>
  <artifactId>aether-datafixers-core</artifactId>
  <version>0.4.0</version>
</dependency>
```

**Using the BOM**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>de.splatgames.aether.datafixers</groupId>
      <artifactId>aether-datafixers-bom</artifactId>
      <version>0.4.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- No version needed -->
  <dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-core</artifactId>
  </dependency>
</dependencies>
```

### Gradle (Groovy)

```groovy
dependencies {
  implementation 'de.splatgames.aether.datafixers:aether-datafixers-core:0.4.0'
  // Or with BOM:
  implementation platform('de.splatgames.aether.datafixers:aether-datafixers-bom:0.4.0')
  implementation 'de.splatgames.aether.datafixers:aether-datafixers-core'
}
```

### Gradle (Kotlin)

```kotlin
dependencies {
  implementation("de.splatgames.aether.datafixers:aether-datafixers-core:0.4.0")
  // Or with BOM:
  implementation(platform("de.splatgames.aether.datafixers:aether-datafixers-bom:0.4.0"))
  implementation("de.splatgames.aether.datafixers:aether-datafixers-core")
}
```

---

## 🆕 What's New

### 🍃 Spring Boot Starter Module

New module `aether-datafixers-spring-boot-starter` for seamless Spring Boot 3.x integration:

```xml
<dependency>
  <groupId>de.splatgames.aether.datafixers</groupId>
  <artifactId>aether-datafixers-spring-boot-starter</artifactId>
  <version>0.4.0</version>
</dependency>
```

**Key Features:**

| Feature               | Description                                                         |
|-----------------------|---------------------------------------------------------------------|
| 🔧 Auto-Configuration | Automatic DataFixer bean creation from `DataFixerBootstrap` beans   |
| 🔄 MigrationService   | Fluent API: `.migrate(data).from(100).to(200).execute()`            |
| 🏷️ Multi-Domain      | Multiple DataFixers with `@Qualifier` and `.usingDomain("game")`    |
| 💚 Actuator           | Health indicator, info contributor, `/actuator/datafixers` endpoint |
| 📊 Metrics            | Micrometer counters, timers, and distribution summaries             |
| ⚡ Async               | `CompletableFuture` support via `.executeAsync()`                   |

**Quick Start:**

```java
@Configuration
public class DataFixerConfig {
    @Bean
    public DataFixerBootstrap gameBootstrap() {
        return new GameDataBootstrap();
    }
}

@Service
public class GameService {
    private final MigrationService migrationService;

    public Dynamic<?> migrateData(Dynamic<?> data, int fromVersion) {
        return migrationService
            .migrate(data)
            .from(fromVersion)
            .toLatest()
            .execute()
            .getData();
    }
}
```

**Configuration Properties:**

```yaml
aether:
  datafixers:
    enabled: true
    default-format: gson  # gson | jackson | jackson_yaml | snakeyaml | jackson_toml | jackson_xml
    default-current-version: 200
    domains:
      game:
        current-version: 200
        primary: true
    actuator:
      include-schema-details: true
    metrics:
      timing: true
      counting: true
```

### 🔌 Multi-Format DynamicOps

New DynamicOps implementations in the codec module:

| Format | Implementation   | Data Type     | Library      |
|--------|------------------|---------------|--------------|
| JSON   | `GsonOps`        | `JsonElement` | Gson         |
| JSON   | `JacksonJsonOps` | `JsonNode`    | Jackson      |
| YAML   | `SnakeYamlOps`   | `Object`      | SnakeYAML    |
| YAML   | `JacksonYamlOps` | `JsonNode`    | Jackson YAML |
| TOML   | `JacksonTomlOps` | `JsonNode`    | Jackson TOML |
| XML    | `JacksonXmlOps`  | `JsonNode`    | Jackson XML  |

**Example:**

```java
// YAML with SnakeYAML (native Java types)
Dynamic<Object> yaml = new Dynamic<>(SnakeYamlOps.INSTANCE, yamlData);

// TOML with Jackson
Dynamic<JsonNode> toml = new Dynamic<>(JacksonTomlOps.INSTANCE, tomlData);

// Cross-format conversion
Dynamic<JsonElement> json = yaml.convert(GsonOps.INSTANCE);
```

### ⚠️ Breaking Change: Package Restructuring

The codec module now uses format-first package organization:

**Old:**
```java
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.codec.jackson.JacksonOps;
```

**New:**
```java
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;
import de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps;
import de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps;
import de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps;
```

---

## 📝 Changelog

**New in 0.4.0**

- Spring Boot Starter with auto-configuration and MigrationService
- Actuator health indicator, info contributor, and custom endpoint
- Micrometer metrics for migration success/failure/duration
- Multi-domain DataFixer support with `@Qualifier`
- SnakeYamlOps for YAML (native Java types)
- JacksonYamlOps for YAML (Jackson dataformat)
- JacksonTomlOps for TOML
- JacksonXmlOps for XML
- Format-first package restructuring in codec module
- Comprehensive Spring Boot and codec documentation

**Full Changelog:** [v0.3.0...v0.4.0](https://github.com/aether-framework/aether-datafixers/compare/v0.3.0...v0.4.0)

---

## 🗺️ Roadmap (next)

- **v0.5.0** (API freeze candidate)
  - **API stabilization pass** — Naming/packaging cleanup + deprecations completed
  - **Compatibility checks in CI** — Binary/source compatibility guardrails for public API
  - **Hardened error model** — Consistent exception types + structured error details
  - **Release readiness** — Final review of docs/examples against frozen API

---

## 📜 License

**MIT** — see `LICENSE`.
