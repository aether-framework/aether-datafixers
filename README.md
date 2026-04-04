<div align="center">

<h1>Aether Datafixers</h1>

<p><strong>A lightweight, format-agnostic data migration framework for the JVM.</strong></p>

<p>
  <a href="https://central.sonatype.com/artifact/de.splatgames.aether.datafixers/aether-datafixers"><img src="https://img.shields.io/maven-central/v/de.splatgames.aether.datafixers/aether-datafixers" alt="Maven Central"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License: MIT"></a>
  <a href="https://openjdk.org/projects/jdk/17/"><img src="https://img.shields.io/badge/Java-17%2B-orange" alt="Java 17+"></a>
  <a href="https://github.com/aether-framework/aether-datafixers/actions/workflows/ci-push.yml"><img src="https://github.com/aether-framework/aether-datafixers/actions/workflows/ci-push.yml/badge.svg" alt="CI"></a>
  <img src="https://sonarqube.splatgames.de/api/project_badges/measure?project=aether-framework_aether-datafixers_b5e07f1f-da6c-4600-8649-09a35a5d42d5&metric=alert_status&token=sqb_c8b086b266abfba4ae3dbf364edc22af3d650bea" alt="Code Quality">
  <img src="https://img.shields.io/badge/coverage-75%25%2B-brightgreen" alt="Coverage">
</p>

</div>

## Overview

Aether Datafixers is a data migration framework that enables **forward patching** of serialized data through versioned schema definitions and composable fixers. It is inspired by Minecraft's [DataFixer Upper (DFU)](docs/appendix/comparison-with-dfu.md) but designed from the ground up for **simplicity**, **clarity**, and **ease of use**.

The framework is **format-agnostic** - it works with JSON, YAML, TOML, XML, or any custom serialization format through the `Dynamic<T>` and `DynamicOps<T>` abstraction layer. All core types are **immutable and thread-safe**, making Aether Datafixers suitable for concurrent, high-throughput environments. The modular architecture lets you pick exactly what you need: just the core, or add the CLI, Spring Boot integration, testkit, schema analysis tools, and more.

## 📋 Table of Contents

- [Quick Start](#-quick-start)
- [Installation](#-installation)
- [Key Concepts](#-key-concepts)
- [Modules](#-modules)
- [Data Flow](#-data-flow)
- [Code Examples](#-code-examples)
- [Spring Boot Integration](#-spring-boot-integration)
- [CLI Tool](#-cli-tool)
- [Testing with Testkit](#-testing-with-testkit)
- [Schema Tools](#-schema-tools)
- [Optics](#-optics)
- [Documentation](#-documentation)
- [Building from Source](#-building-from-source)
- [Contributing](#-contributing)
- [Security](#-security)
- [License](#-license)

## 🚀 Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

<details>
<summary>Gradle (Groovy / Kotlin)</summary>

```groovy
// Groovy
implementation 'de.splatgames.aether.datafixers:aether-datafixers-core:1.0.0'
```

```kotlin
// Kotlin
implementation("de.splatgames.aether.datafixers:aether-datafixers-core:1.0.0")
```

</details>

### 2. Define a Bootstrap

Register your schemas and fixes in a `DataFixerBootstrap`:

```java
public class GameDataBootstrap implements DataFixerBootstrap {

    public static final DataVersion CURRENT_VERSION = new DataVersion(200);

    private SchemaRegistry schemas;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        this.schemas = schemas;
        schemas.register(new Schema100());
        schemas.register(new Schema110());
        schemas.register(new Schema200());
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
        fixes.register(TypeReferences.PLAYER, new PlayerV2ToV3Fix(schemas));
    }
}
```

### 3. Create the DataFixer

```java
AetherDataFixer fixer = new DataFixerRuntimeFactory()
    .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
```

### 4. Apply Migrations

```java
// Wrap input data in a TaggedDynamic
Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, oldJsonData);
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, input);

// Migrate from version 100 to the current version
TaggedDynamic result = fixer.update(tagged, new DataVersion(100), fixer.currentVersion());

// Extract the migrated data
Dynamic<?> migrated = result.value();
```

> For a complete walkthrough, see the [Getting Started Guide](docs/getting-started/quick-start.md).

## 📦 Installation

**Maven**

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

<details>
<summary>Gradle (Groovy / Kotlin)</summary>

```groovy
// Groovy
implementation 'de.splatgames.aether.datafixers:aether-datafixers-core:1.0.0'
```

```kotlin
// Kotlin
implementation("de.splatgames.aether.datafixers:aether-datafixers-core:1.0.0")
```

</details>

### Using the BOM

The Bill of Materials ensures consistent versions across all Aether Datafixers modules.

**Maven**

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>de.splatgames.aether.datafixers</groupId>
            <artifactId>aether-datafixers-bom</artifactId>
            <version>1.0.0</version>
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
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-codec</artifactId>
    </dependency>
</dependencies>
```

<details>
<summary>Gradle (Groovy / Kotlin)</summary>

```groovy
// Groovy
dependencies {
    implementation platform('de.splatgames.aether.datafixers:aether-datafixers-bom:1.0.0')
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-core'
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-codec'
}
```

```kotlin
// Kotlin
dependencies {
    implementation(platform("de.splatgames.aether.datafixers:aether-datafixers-bom:1.0.0"))
    implementation("de.splatgames.aether.datafixers:aether-datafixers-core")
    implementation("de.splatgames.aether.datafixers:aether-datafixers-codec")
}
```

</details>

## 🔑 Key Concepts

| Concept             | Description                                                                           |
|---------------------|---------------------------------------------------------------------------------------|
| **DataVersion**     | Integer-based version identifier for schema snapshots                                 |
| **TypeReference**   | String-based key for routing data to the correct fixes (e.g., `"player"`, `"entity"`) |
| **Schema**          | Associates a `DataVersion` with a `TypeRegistry` defining the types at that version   |
| **DataFix**         | A transformation that migrates data from one version to another                       |
| **Dynamic\<T\>**    | Format-agnostic data wrapper - manipulate data without knowing the underlying format  |
| **DynamicOps\<T\>** | Operations interface for a specific format (Gson, Jackson, SnakeYAML, etc.)           |
| **Codec**           | Bidirectional transformation between typed Java objects and `Dynamic` representations |
| **TypeRewriteRule** | Composable rule defining how data is rewritten during a fix                           |

> For a complete concept reference, see [Concepts](docs/concepts/index.md) and the [Glossary](docs/appendix/glossary.md).

## 📦 Modules

| Module                                                                             | Description                                                                                             |
|------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| [**aether-datafixers-api**](aether-datafixers-api)                                 | Core interfaces and API contracts                                                                       |
| [**aether-datafixers-core**](aether-datafixers-core)                               | Default implementations of the API interfaces                                                           |
| [**aether-datafixers-codec**](aether-datafixers-codec)                             | Multi-format DynamicOps - JSON (Gson, Jackson), YAML (SnakeYAML, Jackson), TOML, XML                    |
| [**aether-datafixers-testkit**](aether-datafixers-testkit)                         | Fluent test builders, AssertJ assertions, DataFix test harnesses - [docs](docs/testkit/index.md)        |
| [**aether-datafixers-cli**](aether-datafixers-cli)                                 | Command-line migration and validation tool - [docs](docs/cli/index.md)                                  |
| [**aether-datafixers-schema-tools**](aether-datafixers-schema-tools)               | Schema diffing, validation, migration analysis - [docs](docs/schema-tools/index.md)                     |
| [**aether-datafixers-spring-boot-starter**](aether-datafixers-spring-boot-starter) | Spring Boot 3.x auto-config, MigrationService, Actuator, Micrometer - [docs](docs/spring-boot/index.md) |
| [**aether-datafixers-examples**](aether-datafixers-examples)                       | Runnable usage examples demonstrating real-world patterns                                               |
| [**aether-datafixers-bom**](aether-datafixers-bom)                                 | Bill of Materials for coordinated version management                                                    |
| [**aether-datafixers-benchmarks**](aether-datafixers-benchmarks)                   | JMH microbenchmarks for performance validation                                                          |
| [**aether-datafixers-functional-tests**](aether-datafixers-functional-tests)       | E2E, integration, stress, and chaos tests                                                               |

## 📊 Data Flow

```
Input Data (JSON, YAML, TOML, XML, ...)
       │
       ▼
DynamicOps<T> wraps raw data as Dynamic<T>
       │
       ▼
DataFixer.update() selects and applies DataFixes in version order
       │
       ▼
Each DataFix transforms Dynamic<T> using composable TypeRewriteRules
       │
       ▼
Output: migrated Dynamic<T> at the target version
```

The framework handles version ordering, type routing, and rule composition automatically. You define schemas and fixes - Aether Datafixers handles the rest.

## 💡 Code Examples

### Schema-Based Fix with Rules

For production use, extend `SchemaDataFix` and compose rules declaratively:

```java
public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(SchemaRegistry schemas) {
        super("player_v100_to_v110", new DataVersion(100), new DataVersion(110), schemas);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            Rules.renameField(GsonOps.INSTANCE, "playerName", "name"),
            Rules.renameField(GsonOps.INSTANCE, "xp", "experience"),
            Rules.transformField(GsonOps.INSTANCE, "gameMode",
                PlayerV1ToV2Fix::gameModeIntToString),
            Rules.addField(GsonOps.INSTANCE, "level",
                dynamic -> dynamic.createInt(1))
        );
    }

    private static Dynamic<?> gameModeIntToString(Dynamic<?> dynamic) {
        int mode = dynamic.asInt().result().orElse(0);
        String name = switch (mode) {
            case 1 -> "creative";
            case 2 -> "adventure";
            case 3 -> "spectator";
            default -> "survival";
        };
        return dynamic.createString(name);
    }
}
```

### Batch Operations

Use `Rules.batch()` to apply multiple field operations in a single pass for optimal performance:

```java
TypeRewriteRule rule = Rules.batch(GsonOps.INSTANCE, batch -> batch
    .rename("playerName", "name")
    .rename("xp", "experience")
    .remove("deprecated_field")
    .add("level", dynamic -> dynamic.createInt(1))
);
```

### Multi-Format Support

The same fix works transparently across all serialization formats:

```java
// JSON via Gson
Dynamic<JsonElement> gsonData = new Dynamic<>(GsonOps.INSTANCE, jsonElement);

// JSON via Jackson
Dynamic<JsonNode> jacksonData = new Dynamic<>(JacksonJsonOps.INSTANCE, jsonNode);

// YAML via SnakeYAML
Dynamic<Object> yamlData = new Dynamic<>(SnakeYamlOps.INSTANCE, yamlMap);

// TOML via Jackson
Dynamic<JsonNode> tomlData = new Dynamic<>(JacksonTomlOps.INSTANCE, tomlNode);
```

All migrations work identically regardless of the underlying format.

> See the [examples module](aether-datafixers-examples) and [tutorials](docs/tutorials/index.md) for complete runnable examples.

## 🍃 Spring Boot Integration

Add the starter dependency to get auto-configuration, a fluent MigrationService, and Actuator support out of the box.

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-spring-boot-starter</artifactId>
</dependency>
```

Register your bootstrap as a Spring bean:

```java
@Configuration
public class DataFixerConfig {
    @Bean
    public DataFixerBootstrap gameBootstrap() {
        return new GameDataBootstrap();
    }
}
```

Inject and use the `MigrationService` with its fluent API:

```java
@Service
public class GameService {

    private final MigrationService migrationService;

    public GameService(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public Dynamic<?> migratePlayerData(TaggedDynamic data, int fromVersion) {
        MigrationResult result = migrationService
            .migrate(data)
            .from(fromVersion)
            .toLatest()
            .execute();

        return result.getData().value();
    }
}
```

### Actuator Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health indicator showing DataFixer operational status |
| `/actuator/info` | Schema version information |
| `/actuator/datafixers` | Detailed domain and version info |

### Micrometer Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `aether.datafixers.migrations.success` | Counter | Successful migrations |
| `aether.datafixers.migrations.failure` | Counter | Failed migrations |
| `aether.datafixers.migrations.duration` | Timer | Migration execution time |
| `aether.datafixers.migrations.version.span` | Distribution | Version span statistics |

The starter also supports **multi-domain setups** with `@Qualifier` annotations, async execution via `executeAsync()`, and custom `DynamicOps` via `withOps()`.

> For configuration properties, multi-domain setup, and metrics integration, see the [Spring Boot documentation](docs/spring-boot/index.md).

## 💻 CLI Tool

Migrate and validate data files from the command line without writing Java code.

```bash
# Migrate a JSON file from version 100 to 200
aether-cli migrate --from 100 --to 200 --type player \
  --bootstrap com.example.GameDataBootstrap input.json

# Validate files without modifying them
aether-cli validate --to 200 --type player \
  --bootstrap com.example.GameDataBootstrap *.json

# Show available formats and bootstrap info
aether-cli info --formats
```

**Supported formats:** `json-gson`, `json-jackson`, `yaml-snakeyaml`, `yaml-jackson`, `toml-jackson`, `xml-jackson`

Features include batch processing, auto-version detection, in-place migration with backups, migration reports (text/JSON), and CI/CD-friendly exit codes.

> For complete usage and format handler details, see the [CLI documentation](docs/cli/index.md).

## 🧪 Testing with Testkit

The testkit module provides fluent builders, custom assertions, and test harnesses for comprehensive migration testing.

```java
@Test
void playerNameRenamed() {
    var fix = QuickFix.renameField(
        GsonOps.INSTANCE, "rename_name", 1, 2, "playerName", "name");

    Dynamic<JsonElement> input = TestData.gson().object()
        .put("playerName", "Alice")
        .put("level", 10)
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .apply();

    assertThat(result)
        .hasStringField("name", "Alice")
        .hasIntField("level", 10)
        .doesNotHaveField("playerName");
}
```

**Components:** `TestData` (fluent builders for all formats), `AetherAssertions` (AssertJ assertions for `Dynamic`, `DataResult`, `Typed`), `DataFixTester` / `MigrationTester` (test harnesses), `QuickFix` (inline fix factories), `MockSchemas` (mock schema utilities).

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-testkit</artifactId>
    <scope>test</scope>
</dependency>
```

> For all builders, assertions, and harness patterns, see the [Testkit documentation](docs/testkit/index.md).

## 🔍 Schema Tools

Analyze, validate, and diff your schemas to catch issues before they reach production.

```java
// Compare schemas between versions
SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
    .includeFieldLevel(true)
    .diff();

// Detect missing DataFixes for schema changes
FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
    .from(100).to(200)
    .analyzeCoverage();

// Validate schema structure and naming conventions
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateStructure()
    .validateConventions()
    .validate();
```

> For diffing details, coverage gap analysis, and convention rules, see the [Schema Tools documentation](docs/schema-tools/index.md).

## 🔭 Optics

Optics provide composable, type-safe accessors for nested data structures. They are central to the data fixer system, enabling transformations without manual traversal code.

| Optic | Focus | Description |
|-------|-------|-------------|
| **Iso** | 1 ↔ 1 | Reversible 1-to-1 transformation between two types |
| **Lens** | 1 → 1 | Focus on exactly one part of a product type (always succeeds) |
| **Prism** | 1 → 0..1 | Focus on one case of a sum type (may not match) |
| **Affine** | 1 → 0..1 | Combines lens and prism capabilities |
| **Traversal** | 1 → 0..n | Focus on zero or more parts |
| **Getter** | 1 → 1 | Read-only focus (no modification) |
| **Finder** | Type → Optic | Locates nested types within a schema |

```java
// Compose lenses for nested access: Person -> Address -> City
Lens<Person, Person, String, String> cityLens = addressLens.compose(cityLens);

String city = cityLens.get(alice);              // "Boston"
Person moved = cityLens.set(alice, "Seattle");  // Alice now in Seattle
```

> For detailed examples with Prism, Iso, Traversal, and Finder, see the [Optics documentation](docs/concepts/optics/index.md).

## 📚 Documentation

| Category | Description |
|----------|-------------|
| [Getting Started](docs/getting-started/index.md) | Installation, quick start, first migration |
| [Concepts](docs/concepts/index.md) | Architecture, schemas, codecs, optics, type system, thread safety |
| [Tutorials](docs/tutorials/index.md) | Step-by-step guides for common scenarios |
| [How-To Guides](docs/how-to/index.md) | Focused guides for specific operations (rename, add, remove, transform, batch, ...) |
| [Advanced Topics](docs/advanced/custom-optics.md) | Custom optics, concurrent migrations, recursive types, traversal strategies |
| [Codec Formats](docs/codec/index.md) | JSON, YAML, TOML, XML - format comparison and usage |
| [CLI](docs/cli/index.md) | Commands, format handlers, usage examples |
| [Schema Tools](docs/schema-tools/index.md) | Diffing, migration analysis, validation, introspection |
| [Spring Boot](docs/spring-boot/index.md) | Auto-configuration, MigrationService, Actuator, Metrics |
| [Testkit](docs/testkit/index.md) | Test builders, assertions, harnesses |
| [Troubleshooting](docs/troubleshooting/faq.md) | FAQ, common errors, debugging tips |
| [Appendix](docs/appendix/glossary.md) | Glossary, DFU comparison, type theory primer |

## 🔨 Building from Source

**Requirements:** Java 17+, Maven 3.9.5+

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run unit tests
mvn test

# Run integration & E2E tests
mvn verify -Pit

# Run stress & chaos tests
mvn verify -Pstress -pl aether-datafixers-functional-tests

# Run JMH benchmarks
java -jar aether-datafixers-benchmarks/target/benchmarks.jar
```

## 🤝 Contributing

We welcome contributions of all kinds - bug fixes, features, documentation improvements, and discussions.

Please read our [Contributing Guide](CONTRIBUTING.md) before submitting a pull request. This project follows the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). For AI-assisted contributions, please review our [AI Usage Guidelines](AI_USAGE.md).

## 🔒 Security

All release artifacts are **GPG-signed** and published to Maven Central. The project uses automated security scanning via **CodeQL**, **OWASP Dependency-Check**, and **Dependabot**.

To report a vulnerability, see our [Security Policy](SECURITY.md).

## 📄 License

This project is licensed under the [MIT License](LICENSE).

Copyright (c) 2025–2026 [Splatgames.de Software](https://software.splatgames.de) and Contributors.
