# 🚀 **Aether Datafixers v0.5.0 — API Freeze & Release Readiness**

The API freeze release with stabilized public interfaces, comprehensive validation tooling, extended codec support, and full multi-format integration across all modules.

---

## 🎯 Highlights in v0.5.0

- ✅ **API Freeze** — Public API is now stable; no breaking changes expected before v1.0.0
- ✅ **Schema Validation Integration** — Full `MigrationAnalyzer` integration for fix coverage validation
- ✅ **MigrationService.withOps()** — Custom `DynamicOps` support for format conversion during migrations
- ✅ **Extended Codec Support** — Multi-format DynamicOps integration for CLI, Testkit and Spring Boot modules
- ✅ **Functional Tests Module** — New `aether-datafixers-functional-tests` with E2E and integration tests
- ✅ **Comprehensive Documentation** — Complete documentation suite covering all modules and features

---

## 📦 Installation

> [!TIP]
> All Aether artifacts are available on **Maven Central** — no extra repository required.

### Maven

```xml
<dependency>
  <groupId>de.splatgames.aether.datafixers</groupId>
  <artifactId>aether-datafixers-core</artifactId>
  <version>0.5.0</version>
</dependency>
```

**Using the BOM**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>de.splatgames.aether.datafixers</groupId>
      <artifactId>aether-datafixers-bom</artifactId>
      <version>0.5.0</version>
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
  implementation 'de.splatgames.aether.datafixers:aether-datafixers-core:0.5.0'
  // Or with BOM:
  implementation platform('de.splatgames.aether.datafixers:aether-datafixers-bom:0.5.0')
  implementation 'de.splatgames.aether.datafixers:aether-datafixers-core'
}
```

### Gradle (Kotlin)

```kotlin
dependencies {
  implementation("de.splatgames.aether.datafixers:aether-datafixers-core:0.5.0")
  // Or with BOM:
  implementation(platform("de.splatgames.aether.datafixers:aether-datafixers-bom:0.5.0"))
  implementation("de.splatgames.aether.datafixers:aether-datafixers-core")
}
```

---

## 🆕 What's New

### 🔍 SchemaValidator Fix Coverage Integration

The `SchemaValidator.validateFixCoverage()` method now performs actual coverage analysis using `MigrationAnalyzer`:

```java
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateFixCoverage()
    .validate();

if (result.hasWarnings()) {
    for (ValidationIssue issue : result.warnings()) {
        System.out.println("Missing fix: " + issue.message());
        // e.g., "Missing DataFix for type 'player' field 'health': FIELD_ADDED"
    }
}
```

**Features:**
- Automatically analyzes the full version range from schema registry
- Detects missing DataFixes for schema changes (field additions, removals, type changes)
- Reports field-level coverage gaps with detailed context
- Integrates seamlessly with existing validation pipeline

### 🔄 MigrationService.withOps() Support

The Spring Boot `MigrationService` now fully supports custom `DynamicOps` for format conversion:

```java
// Convert input data to Jackson YAML format during migration
MigrationResult result = migrationService
    .migrate(gsonData)
    .from(100)
    .to(200)
    .withOps(JacksonYamlOps.INSTANCE)  // Now fully functional!
    .execute();

// Result data is now in Jackson YAML format
Dynamic<JsonNode> yamlResult = (Dynamic<JsonNode>) result.getData();
```

**Use Cases:**
- Convert between serialization formats during migration
- Normalize data to a specific format for downstream processing
- Work with format-specific features (e.g., YAML anchors, TOML tables)

### 🔌 Extended Codec Support for CLI & Testkit

Full multi-format DynamicOps integration for the CLI and Testkit modules:

**CLI Format Handlers:**

| Format ID        | Library      | Data Type     | File Extensions |
|------------------|--------------|---------------|-----------------|
| `json-gson`      | Gson         | `JsonElement` | `.json`         |
| `json-jackson`   | Jackson      | `JsonNode`    | `.json`         |
| `yaml-snakeyaml` | SnakeYAML    | `Object`      | `.yaml`, `.yml` |
| `yaml-jackson`   | Jackson YAML | `JsonNode`    | `.yaml`, `.yml` |
| `toml-jackson`   | Jackson TOML | `JsonNode`    | `.toml`         |
| `xml-jackson`    | Jackson XML  | `JsonNode`    | `.xml`          |

**Testkit Factory Methods:**

```java
// All formats now supported in TestData
Dynamic<JsonElement> gson = TestData.gson().object().put("key", "value").build();
Dynamic<JsonNode> jackson = TestData.jacksonJson().object().put("key", "value").build();
Dynamic<Object> yaml = TestData.snakeYaml().object().put("key", "value").build();
Dynamic<JsonNode> yamlJ = TestData.jacksonYaml().object().put("key", "value").build();
Dynamic<JsonNode> toml = TestData.jacksonToml().object().put("key", "value").build();
Dynamic<JsonNode> xml = TestData.jacksonXml().object().put("key", "value").build();
```

### 🧪 Functional Tests Module

New `aether-datafixers-functional-tests` module with comprehensive E2E and integration tests:

| Test Category           | Description                                                  |
|-------------------------|--------------------------------------------------------------|
| Cross-Format Migration  | Validate migrations work identically across all DynamicOps   |
| Error Recovery          | Test graceful handling of malformed data and fix failures    |
| Field Transformations   | E2E tests for rename, add, remove, and restructure operations|

Run integration tests with:

```bash
mvn verify -Pit
```

---

## 📋 Module Overview

| Module                                  | Description                                                  |
|-----------------------------------------|--------------------------------------------------------------|
| `aether-datafixers-api`                 | Core interfaces and API contracts (stable)                   |
| `aether-datafixers-core`                | Default implementations                                      |
| `aether-datafixers-codec`               | DynamicOps for JSON, YAML, TOML, XML                         |
| `aether-datafixers-testkit`             | Testing utilities with fluent API + multi-format support     |
| `aether-datafixers-cli`                 | Command-line interface with multi-format handlers            |
| `aether-datafixers-schema-tools`        | Schema analysis, validation, and diffing                     |
| `aether-datafixers-spring-boot-starter` | Spring Boot 3.x auto-configuration                           |
| `aether-datafixers-examples`            | Practical usage examples                                     |
| `aether-datafixers-functional-tests`    | E2E and integration tests                                    |
| `aether-datafixers-bom`                 | Bill of Materials for version management                     |

---

## 📝 Changelog

**New in 0.5.0**

- `SchemaValidator.validateFixCoverage()` now performs actual coverage analysis via `MigrationAnalyzer`
- `MigrationService.withOps()` fully implemented for format conversion during migrations
- Extended codec support: CLI format handlers for YAML, TOML, XML
- Extended codec support: Testkit factory methods for all DynamicOps implementations
- New `aether-datafixers-functional-tests` module with comprehensive E2E and IT tests
- Cross-format migration tests (Gson, Jackson JSON, Jackson YAML, SnakeYAML, TOML, XML)
- Error recovery integration tests with graceful failure handling
- Field transformation E2E tests (rename, add, group operations)
- API stabilization: public interfaces frozen for v1.0.0
- Comprehensive documentation updates across all modules

**Deprecations (removal planned for v1.0.0)**

- `de.splatgames.aether.datafixers.codec.gson.GsonOps` — Use `codec.json.gson.GsonOps`
- `de.splatgames.aether.datafixers.codec.jackson.JacksonOps` — Use `codec.json.jackson.JacksonJsonOps` for JSON, or the format-specific classes (`JacksonYamlOps`, `JacksonTomlOps`, `JacksonXmlOps`)
- `TestData.jackson()` — Use `TestData.jacksonJson()` instead

**Full Changelog:** [v0.4.0...v0.5.0](https://github.com/aether-framework/aether-datafixers/compare/v0.4.0...v0.5.0)

---

## 🔄 Migration from v0.4.0

### Breaking Changes

None. v0.5.0 is fully backward compatible with v0.4.0.

### Deprecated API Updates

If using deprecated wrapper classes, update imports:

```java
// Old (deprecated)
import de.splatgames.aether.datafixers.codec.gson.GsonOps;

// New
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
```

---

## 🗺️ Roadmap

### v1.0.0 (next)

- **Stable Release** — Production-ready with semantic versioning guarantees
- **Performance Benchmarks** — Published benchmark suite
- **Extended Documentation** — Video tutorials and cookbook examples

---

## 📜 License

**MIT** — see `LICENSE`.
