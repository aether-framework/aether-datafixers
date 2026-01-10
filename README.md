![License](https://img.shields.io/badge/license-MIT-red)
![Maven Central](https://img.shields.io/maven-central/v/de.splatgames.aether.datafixers/aether-datafixers)
![Version](https://img.shields.io/badge/version-0.4.0-orange)

# Aether Datafixers 🔧

**Aether Datafixers** is a lightweight **data migration framework** for the JVM.
It enables **forward patching** of serialized data through schema definitions and versioned fixers —
inspired by Minecraft's DataFixer Upper (DFU), with a focus on **simplicity**, **clarity**, and **ease of use**.

---

## ✨ Features (v0.4.0)

- ✅ **Schema-Based Versioning** — Define data types per version with `Schema` and `TypeRegistry`
- ✅ **Forward Patching** — Apply `DataFix` instances sequentially to migrate data across versions
- ✅ **Format-Agnostic** — Work with any serialization format via `Dynamic<T>` and `DynamicOps<T>`
- ✅ **Multi-Format Support** — JSON (Gson, Jackson), YAML (SnakeYAML, Jackson), TOML, and XML
- ✅ **Codec System** — Bidirectional transformation between typed Java objects and dynamic representations
- ✅ **Type Safety** — Strong typing with `TypeReference` identifiers for data routing
- ✅ **Testkit** — Fluent test data builders, custom assertions, and test harnesses for DataFix testing
- ✅ **CLI Tool** — Migrate and validate data files from the command line with batch processing
- ✅ **Schema Tools** — Schema diffing, validation, migration analysis, and type introspection
- ✅ **Spring Boot 3.x** — Auto-configuration, MigrationService with fluent API, Actuator integration
- ✅ **Migration Diagnostics** — Opt-in structured reports with timing, applied fixes, and snapshots
- ✅ **Extended Rewrite Rules** — Batch operations, path-based transforms, conditional rules
- ✅ **High-Performance APIs** — `Rules.batch()` for single-pass multi-operation transforms
- ✅ **JDK 17+** — Built and tested on modern LTS JVMs

---

## 📦 Modules

- **aether-datafixers-api** — Core interfaces and API contracts (no implementation logic)
- **aether-datafixers-core** — Default implementations of the API interfaces
- **aether-datafixers-codec** — Codec implementations for serialization formats
- **aether-datafixers-testkit** — Testing utilities for DataFix, Schema, and migration testing
- **aether-datafixers-cli** — Command-line interface for data migration and validation
- **aether-datafixers-schema-tools** — Schema analysis, validation, diffing, and introspection
- **aether-datafixers-spring-boot-starter** — Spring Boot 3.x auto-configuration with Actuator support
- **aether-datafixers-examples** — Practical examples demonstrating real-world usage
- **aether-datafixers-bom** — Bill of Materials for coordinated dependency management

---

## 🚀 Quickstart

### 1) Define a Bootstrap

```java
public class MyBootstrap implements DataFixerBootstrap {
    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        // Register schema for each version
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        // Register DataFix instances for type migrations
    }
}
```

### 2) Create the DataFixer

```java
AetherDataFixer fixer = new DataFixerRuntimeFactory()
    .create(currentVersion, new MyBootstrap());
```

### 3) Apply Migrations

```java
Dynamic<?> updated = fixer.update(
    new TypeReference("player"),
    inputDynamic,
    fromVersion,
    toVersion
);
```

---

## 📚 Installation

**Maven**

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-core</artifactId>
    <version>0.4.0</version>
</dependency>
```

**Gradle (Groovy)**

```groovy
dependencies {
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-core:0.4.0'
}
```

**Gradle (Kotlin)**

```kotlin
dependencies {
    implementation("de.splatgames.aether.datafixers:aether-datafixers-core:0.4.0")
}
```

> Use the **BOM** for coordinated version management across all modules.

---

## 📋 Using the BOM

The Bill of Materials (BOM) ensures consistent versions across all Aether Datafixers modules.

**Maven**

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
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-codec</artifactId>
    </dependency>
</dependencies>
```

**Gradle (Groovy)**

```groovy
dependencies {
    implementation platform('de.splatgames.aether.datafixers:aether-datafixers-bom:0.4.0')

    // No version needed
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-core'
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-codec'
}
```

**Gradle (Kotlin)**

```kotlin
dependencies {
    implementation(platform("de.splatgames.aether.datafixers:aether-datafixers-bom:0.4.0"))

    // No version needed
    implementation("de.splatgames.aether.datafixers:aether-datafixers-core")
    implementation("de.splatgames.aether.datafixers:aether-datafixers-codec")
}
```

---

## 🔑 Key Concepts

| Concept           | Description                                                                              |
|-------------------|------------------------------------------------------------------------------------------|
| **DataVersion**   | Integer-based version identifier for data schemas                                        |
| **TypeReference** | String-based identifier for data types (e.g., `"player"`, `"entity"`)                    |
| **Schema**        | Associates a `DataVersion` with a `TypeRegistry`                                         |
| **DataFix**       | Migration that transforms data from one version to another                               |
| **Dynamic**       | Format-agnostic data wrapper enabling manipulation without knowing the underlying format |
| **DynamicOps**    | Operations interface for a specific format (JSON, DAT, etc.)                             |
| **Codec**         | Bidirectional transformation between typed objects and `Dynamic` representations         |
| **Type**          | Combines a `TypeReference` with a `Codec`                                                |
| **Optic**         | Composable accessor for nested data structures (e.g., `Lens`, `Prism`, `Adapter`)        |
| **DSL**           | Domain-specific language for defining type templates and optic compositions              |

---

## 🔍 Optics

Optics provide composable, type-safe accessors for nested data structures. They are central to the data fixer system, enabling transformations without manual traversal code.

### Optic Hierarchy

| Optic         | Focus             | Description                                                   |
|---------------|-------------------|---------------------------------------------------------------|
| **Iso**       | 1 ↔ 1             | Reversible 1-to-1 transformation between two types            |
| **Lens**      | 1 → 1             | Focus on exactly one part of a product type (always succeeds) |
| **Prism**     | 1 → 0..1          | Focus on one case of a sum type (may not match)               |
| **Affine**    | 1 → 0..1          | Combines lens and prism capabilities                          |
| **Traversal** | 1 → 0..n          | Focus on zero or more parts                                   |
| **Getter**    | 1 → 1 (read-only) | Read-only focus (no modification)                             |
| **Finder**    | Type → Optic      | Locates nested types within a schema                          |

### Lens Example

A `Lens` focuses on exactly one field of a structure:

```java
record Address(String street, String city) {}
record Person(String name, Address address) {}

// Create a lens for Person -> Address
Lens<Person, Person, Address, Address> addressLens = Lens.of(
    "person.address",
    Person::address,
    (person, newAddress) -> new Person(person.name(), newAddress)
);

// Create a lens for Address -> City
Lens<Address, Address, String, String> cityLens = Lens.of(
    "address.city",
    Address::city,
    (address, newCity) -> new Address(address.street(), newCity)
);

// Compose lenses: Person -> Address -> City
Lens<Person, Person, String, String> personCityLens = addressLens.compose(cityLens);

// Use the composed lens
Person alice = new Person("Alice", new Address("Main St", "Boston"));
String city = personCityLens.get(alice);              // "Boston"
Person moved = personCityLens.set(alice, "Seattle");  // Alice now in Seattle
```

### Prism Example

A `Prism` focuses on one variant of a sum type:

```java
sealed interface JsonValue permits JsonString, JsonNumber, JsonNull {}
record JsonString(String value) implements JsonValue {}
record JsonNumber(double value) implements JsonValue {}
record JsonNull() implements JsonValue {}

// Create a prism for JsonValue -> JsonString
Prism<JsonValue, JsonValue, String, String> stringPrism = Prism.of(
    "json.string",
    json -> json instanceof JsonString js ? Optional.of(js.value()) : Optional.empty(),
    value -> new JsonString(value)
);

// Use the prism
JsonValue text = new JsonString("hello");
JsonValue number = new JsonNumber(42.0);

stringPrism.getOption(text);    // Optional.of("hello")
stringPrism.getOption(number);  // Optional.empty()

stringPrism.modify(text, String::toUpperCase);   // JsonString("HELLO")
stringPrism.modify(number, String::toUpperCase); // JsonNumber(42.0) - unchanged
```

---

## 📊 Data Flow

```
Input Data (e.g., JSON)
    ↓
DynamicOps parses to Dynamic<T>
    ↓
DataFixer.update() applies relevant DataFixes in version order
    ↓
Type.codec().decode() produces typed Java object
```

---

## 🔧 Extending SchemaDataFix

For fixes that need schema access, extend `SchemaDataFix`:

```java
public class MyFix extends SchemaDataFix {
    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        // Return a rule that transforms typed data
    }
}
```

---

## 🧪 Testing with Testkit

The `aether-datafixers-testkit` module provides utilities for testing your migrations:

```java
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;
import de.splatgames.aether.datafixers.testkit.harness.DataFixTester;
import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;

@Test
void testFieldRename() {
    // Create a quick fix for testing
    var fix = QuickFix.renameField(
        GsonOps.INSTANCE, "rename_player_name", 1, 2,
        "playerName", "name"
    );

    // Create test data fluently
    Dynamic<JsonElement> input = TestData.gson().object()
        .put("playerName", "Alice")
        .put("level", 10)
        .build();

    // Apply and verify
    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .apply();

    // Use custom assertions
    assertThat(result)
        .hasStringField("name", "Alice")
        .hasIntField("level", 10)
        .doesNotHaveField("playerName");
}
```

### Testkit Features

| Component            | Description                                                              |
|----------------------|--------------------------------------------------------------------------|
| **TestData**         | Fluent builders for creating test data (`TestData.gson().object()...`)   |
| **AetherAssertions** | Custom AssertJ assertions for `Dynamic`, `DataResult`, `Typed`           |
| **DataFixTester**    | Test harness for isolated DataFix testing                                |
| **QuickFix**         | Factory methods for common fix patterns (rename, add, remove, transform) |
| **MockSchemas**      | Mock schema utilities for testing                                        |

Add to your project:

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-testkit</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 🍃 Spring Boot Integration

The `aether-datafixers-spring-boot-starter` provides comprehensive Spring Boot 3.x integration.

### Installation

**Maven**

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-spring-boot-starter</artifactId>
    <version>0.4.0</version>
</dependency>
```

**Gradle (Kotlin)**

```kotlin
implementation("de.splatgames.aether.datafixers:aether-datafixers-spring-boot-starter:0.4.0")
```

### Quick Start

1. **Create a DataFixerBootstrap bean:**

```java
@Configuration
public class DataFixerConfig {
    @Bean
    public DataFixerBootstrap gameBootstrap() {
        return new GameDataBootstrap();
    }
}
```

2. **Inject and use MigrationService:**

```java
@Service
public class GameService {
    private final MigrationService migrationService;

    public GameService(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    public Dynamic<?> migratePlayerData(Dynamic<?> data, int fromVersion) {
        MigrationResult result = migrationService
            .migrate(data)
            .from(fromVersion)
            .toLatest()
            .execute();

        if (result.isSuccess()) {
            return result.getData();
        }
        throw new MigrationException(result.getErrorMessage());
    }
}
```

### Configuration Properties

```yaml
aether:
  datafixers:
    enabled: true                    # Enable/disable auto-config
    default-format: gson             # gson | jackson
    default-current-version: 200     # Fallback version
    domains:
      game:
        current-version: 200
        primary: true
      user:
        current-version: 150
    actuator:
      include-schema-details: true
      include-fix-details: true
    metrics:
      timing: true
      counting: true
```

### Multi-Domain Support

Support multiple DataFixers with `@Qualifier`:

```java
@Configuration
public class DataFixerConfig {
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
}

// Usage
MigrationResult result = migrationService
    .migrate(data)
    .usingDomain("game")  // Select domain
    .from(100)
    .toLatest()
    .execute();
```

### Actuator Endpoints

| Endpoint               | Description                               |
|------------------------|-------------------------------------------|
| `/actuator/health`     | Health indicator showing DataFixer status |
| `/actuator/info`       | Schema version information                |
| `/actuator/datafixers` | Detailed domain and version info          |

### Micrometer Metrics

| Metric                                      | Type         | Description             |
|---------------------------------------------|--------------|-------------------------|
| `aether.datafixers.migrations.success`      | Counter      | Successful migrations   |
| `aether.datafixers.migrations.failure`      | Counter      | Failed migrations       |
| `aether.datafixers.migrations.duration`     | Timer        | Migration duration      |
| `aether.datafixers.migrations.version.span` | Distribution | Version span statistics |

---

## 📖 Examples

The `aether-datafixers-examples` module provides a complete, runnable example demonstrating real-world usage patterns.

### Game Data Migration Example

A practical example showing how to migrate game save data through multiple versions:

```
TypeReferences.java     → Type IDs for routing (PLAYER, WORLD, etc.)
        ↓
Schema100/110/200.java  → Schema definitions for each version
        ↓
PlayerV1ToV2Fix.java    → Migration: V1.0.0 → V1.1.0
PlayerV2ToV3Fix.java    → Migration: V1.1.0 → V2.0.0
        ↓
GameDataBootstrap.java  → Registers schemas and fixes
        ↓
GameExample.java        → Main: demonstrates encode/update/decode workflow
```

### Migration Chain

| Version | ID  | Changes                                              |
|---------|-----|------------------------------------------------------|
| V1.0.0  | 100 | Initial flat structure (`playerName`, `x`, `y`, `z`) |
| V1.1.0  | 110 | Restructured with nested `position` object           |
| V2.0.0  | 200 | Extended with `health`, `maxHealth`, `level`         |

### Running the Example

```bash
mvn exec:java -pl aether-datafixers-examples
```

This will demonstrate the complete workflow: loading V1.0.0 data, applying fixes, and outputting V2.0.0 data.

---

## 🛠️ Building

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests
mvn test
```

---

## 🗺️ Roadmap

- **v0.1.0**
  - Core API and default implementations
  - Schema-based versioning with TypeRegistry
  - DataFix forward patching system
  - Dynamic/DynamicOps format abstraction
  - Basic codec infrastructure

- **v0.2.0**
  - **Testkit module** — Fluent test data builders, custom AssertJ assertions, test harnesses
  - **Migration diagnostics** — Opt-in structured reports with timing, applied fixes, and snapshots
  - **Extended rewrite rules** — Batch operations, path-based transforms, conditional rules
  - **High-performance APIs** — `Rules.batch()` and single-pass conditional transforms
  - **Performance optimizations** — Path caching, optimized fix registry, reduced allocations

- **v0.3.0**
  - **CLI module** — Migrate files from the command line with batch processing and reports
  - **Schema Tools module** — Schema diffing, migration analysis, validation, and introspection
  - **Fix coverage analysis** — Detect schema changes without corresponding DataFixes
  - **Convention checking** — Enforce naming conventions for types, fields, and classes

- **v0.4.0** (current)
  - **Spring Boot Starter** — Auto-configuration, MigrationService with fluent API
  - **Actuator integration** — Health indicator, info contributor, custom endpoint, Micrometer metrics
  - **Multi-domain support** — Multiple DataFixers with @Qualifier annotations
  - **DynamicOps auto-configuration** — Conditional beans for all supported formats
  - **Multi-format DynamicOps** — YAML (SnakeYAML, Jackson), TOML (Jackson), XML (Jackson)
  - **Package restructuring** — Format-first package organization (`codec.json.gson`, `codec.yaml.jackson`, etc.)

- **v0.5.0** (next, API freeze candidate)
  - **API stabilization pass** — Naming/packaging cleanup + deprecations completed
  - **Compatibility checks in CI** — Binary/source compatibility guardrails for public API
  - **Hardened error model** — Consistent exception types + structured error details
  - **Release readiness** — Final review of docs/examples against frozen API

- **v1.0.0**
  - Stable API surface
  - Comprehensive documentation
  - Production-ready release

---

## 🤝 Contributing

Contributions welcome! Please open issues/PRs with clear repros or targeted patches.

---

## 📄 License

MIT © Splatgames.de Software and Contributors
