# 🚀 **Aether Datafixers v0.3.0 — CLI, Schema Tools, and Convention Validation**

Command-line interface for batch migrations, schema analysis tools, and naming convention validation.

---

## 🎯 Highlights in v0.3.0

- ✅ **CLI Module** — New `aether-datafixers-cli` module for migrating and validating data files from the command line with batch processing and reports.
- ✅ **Schema Tools Module** — New `aether-datafixers-schema-tools` module for schema diffing, migration analysis, validation, and type introspection.
- ✅ **Fix Coverage Analysis** — Detect schema changes without corresponding DataFixes to ensure complete migration coverage.
- ✅ **Convention Checking** — Enforce naming conventions for types, fields, schema classes, and fix classes.

---

## 📦 Installation

> [!TIP]
> All Aether artifacts are available on **Maven Central** — no extra repository required.

### Maven

```xml
<dependency>
  <groupId>de.splatgames.aether.datafixers</groupId>
  <artifactId>aether-datafixers-core</artifactId>
  <version>0.3.0</version>
</dependency>
```

**Using the BOM**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>de.splatgames.aether.datafixers</groupId>
      <artifactId>aether-datafixers-bom</artifactId>
      <version>0.3.0</version>
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
  implementation 'de.splatgames.aether.datafixers:aether-datafixers-core:0.3.0'
  // Or with BOM:
  implementation platform('de.splatgames.aether.datafixers:aether-datafixers-bom:0.3.0')
  implementation 'de.splatgames.aether.datafixers:aether-datafixers-core'
}
```

### Gradle (Kotlin)

```kotlin
dependencies {
  implementation("de.splatgames.aether.datafixers:aether-datafixers-core:0.3.0")
  // Or with BOM:
  implementation(platform("de.splatgames.aether.datafixers:aether-datafixers-bom:0.3.0"))
  implementation("de.splatgames.aether.datafixers:aether-datafixers-core")
}
```

---

## 🆕 What's New

### 🖥️ CLI Module

New module `aether-datafixers-cli` for command-line data migration:

```xml
<dependency>
  <groupId>de.splatgames.aether.datafixers</groupId>
  <artifactId>aether-datafixers-cli</artifactId>
  <version>0.3.0</version>
</dependency>
```

**Commands:**

| Command | Description |
|---------|-------------|
| `migrate` | Migrate data files from one schema version to another |
| `validate` | Check if files need migration without modifying them |
| `info` | Display version info, available formats, and bootstrap details |

**Features:**
- Batch processing of multiple files with shell glob expansion
- Auto-detection of source version from configurable data field path
- In-place file modification with automatic `.bak` backup
- Output to stdout, file, or directory
- Pretty-printed or compact JSON output
- Migration reports in text or JSON format
- Verbose mode with detailed progress and stack traces
- CI/CD friendly exit codes (0=success, 1=error, 2=migration needed)

**Example:**
```bash
# Migrate a single file
aether-cli migrate --from 100 --to 200 --type player --bootstrap com.example.MyBootstrap input.json

# Migrate with auto-detected version
aether-cli migrate --to 200 --type player --version-field dataVersion --bootstrap com.example.MyBootstrap input.json

# Validate files (check without modifying)
aether-cli validate --to 200 --type player --bootstrap com.example.MyBootstrap *.json

# Show available formats and bootstrap info
aether-cli info --formats
aether-cli info --bootstrap com.example.MyBootstrap
```

### 🔧 Schema Tools Module

New module `aether-datafixers-schema-tools` for schema analysis and validation:

```xml
<dependency>
  <groupId>de.splatgames.aether.datafixers</groupId>
  <artifactId>aether-datafixers-schema-tools</artifactId>
  <version>0.3.0</version>
</dependency>
```

#### Schema Diffing

Compare schemas between versions to see what changed:

```java
SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
    .includeFieldLevel(true)
    .diff();

// Check results
diff.addedTypes();    // Types new in schemaV2
diff.removedTypes();  // Types removed from schemaV1
diff.commonTypes();   // Types in both schemas
diff.typeDiffs();     // Field-level changes for common types
```

#### Migration Analysis

Analyze migration paths and detect coverage gaps:

```java
FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
    .from(100).to(200)
    .analyzeCoverage();

// Find schema changes without DataFixes
for (CoverageGap gap : coverage.gaps()) {
    System.out.println("Missing fix for: " + gap.type() + " (" + gap.reason() + ")");
}

// Detect orphan fixes (fixes without schema changes)
coverage.orphanFixes();
```

#### Schema Validation

Validate schema structure and naming conventions:

```java
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateStructure()
    .validateConventions(ConventionRules.STRICT)
    .validate();

// Check for issues
for (ValidationIssue issue : result.issues()) {
    System.out.println(issue.severity() + ": " + issue.message());
}
```

#### Convention Checking

Enforce naming conventions:

```java
ConventionChecker checker = ConventionChecker.withRules(ConventionRules.builder()
    .typeNamePattern(Pattern.compile("[a-z][a-z0-9_]*"))  // snake_case types
    .fieldNamePattern(Pattern.compile("[a-z][a-zA-Z0-9]*")) // camelCase fields
    .schemaClassPrefix("Schema")  // Schema100, Schema200
    .fixClassSuffix("Fix")        // PlayerNameFix, PositionFix
    .build());

List<ValidationIssue> issues = checker.check(bootstrap);
```

#### Type Introspection

Inspect type structures programmatically:

```java
TypeStructure structure = TypeIntrospector.introspect(type);

// Get all fields with their paths
for (FieldInfo field : structure.fields()) {
    System.out.println(field.path() + " : " + field.typeKind());
}

// Compare structures
boolean equal = TypeIntrospector.structurallyEqual(type1, type2);
```

---

## 📝 Changelog

**New in 0.3.0**

- CLI module with migrate, validate, and info commands
- Schema Tools module with diffing, analysis, validation, and introspection
- Fix coverage analysis to detect missing DataFixes
- Convention checking for type, field, and class names
- Format handler SPI with Gson and Jackson implementations
- Comprehensive documentation updates

**Full Changelog:** [v0.2.0...v0.3.0](https://github.com/aether-framework/aether-datafixers/compare/v0.2.0...v0.3.0)

---

## 🗺️ Roadmap (next)

- **v0.4.0**
  - **Spring Boot integration** — Auto-configuration for DataFixer in Spring apps
  - **Extra ops modules** — Optional YAML/TOML support (format adapters)
  - **Debug utilities** — Pretty printers / tree diff for Dynamic structures (dev-facing)

- **v0.5.0** (API freeze candidate)
  - **API stabilization pass** — Naming/packaging cleanup + deprecations completed
  - **Compatibility checks in CI** — Binary/source compatibility guardrails for public API
  - **Hardened error model** — Consistent exception types + structured error details
  - **Release readiness** — Final review of docs/examples against frozen API

---

## 📜 License

**MIT** — see `LICENSE`.
