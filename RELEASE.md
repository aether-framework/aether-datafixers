# 🚀 **Aether Datafixers v1.0.0-rc.1 - First Release Candidate**

The first **release candidate** for the upcoming 1.0.0 GA. Public API is frozen since v0.5.0 and now hardened with field-level diagnostics, modernized Javadoc, comprehensive null-safety annotations, and a stress/chaos testing framework. Deprecated APIs from the 0.5.0 line have been removed.

> [!IMPORTANT]
> This is a **release candidate** intended for evaluation and integration testing. The public API is considered stable, but until 1.0.0 GA we reserve the right to apply corrective changes in response to RC feedback.

---

## 🎯 Highlights since v0.5.0

- ✅ **API Hardening** - Public API surface frozen since 0.5.0; deprecated wrappers from the 0.5.0 line removed
- ✅ **Field-Level Diagnostics** - New `FieldOperation` / `FieldAwareRule` model surfaces per-field metadata in `MigrationReport`
- ✅ **Modernized Javadoc** - Comprehensive Javadoc overhaul across every module for 1.0.0 readiness
- ✅ **Pervasive Null-Safety** - `@NotNull` / `@Nullable` annotations applied across the public API and internals
- ✅ **JMH Benchmark Suite** - New `aether-datafixers-benchmarks` module with reproducible performance benchmarks
- ✅ **Stress & Chaos Testing** - High-concurrency, sustained-load, memory-pressure, and chaos injection ITs
- ✅ **Migration Guide** - Detailed v0.5.x → 1.0.0 migration documentation including automated patterns
- ✅ **Operational Runbook** - Production troubleshooting guide and operational documentation
- ✅ **Aether Style Guard** - CI-enforced style guard ensures consistent code style across all modules
- ✅ **DCO & Contributor Recognition** - Developer Certificate of Origin and `CONTRIBUTORS.md` added
- ✅ **Bug Fix Sweep** - 100+ issues addressed across API, codec, core, CLI, testkit, schema-tools, and Spring Boot starter

---

## 📦 Installation

> [!TIP]
> All Aether artifacts are published to **Maven Central** — no extra repository required.

> [!WARNING]
> Release candidates are pre-release artifacts. Pin the exact version; do **not** rely on version ranges in production code paths.

### Maven

```xml
<dependency>
  <groupId>de.splatgames.aether.datafixers</groupId>
  <artifactId>aether-datafixers-core</artifactId>
  <version>1.0.0-rc.1</version>
</dependency>
```

**Using the BOM**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>de.splatgames.aether.datafixers</groupId>
      <artifactId>aether-datafixers-bom</artifactId>
      <version>1.0.0-rc.1</version>
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
  implementation 'de.splatgames.aether.datafixers:aether-datafixers-core:1.0.0-rc.1'
  // Or with BOM:
  implementation platform('de.splatgames.aether.datafixers:aether-datafixers-bom:1.0.0-rc.1')
  implementation 'de.splatgames.aether.datafixers:aether-datafixers-core'
}
```

### Gradle (Kotlin)

```kotlin
dependencies {
  implementation("de.splatgames.aether.datafixers:aether-datafixers-core:1.0.0-rc.1")
  // Or with BOM:
  implementation(platform("de.splatgames.aether.datafixers:aether-datafixers-bom:1.0.0-rc.1"))
  implementation("de.splatgames.aether.datafixers:aether-datafixers-core")
}
```

---

## 🆕 What's New

### 🔬 Field-Level Diagnostics

`TypeRewriteRule` instances now carry structured `FieldOperation` metadata, enabling field-aware diagnostics throughout the migration pipeline.

```java
MigrationReport report = fixer.update(typeRef, dynamic, fromVersion, toVersion);

for (FixExecution exec : report.executions()) {
    for (FieldOperation op : exec.fieldOperations()) {
        System.out.printf("%s on field '%s' (type %s)%n",
            op.type(),      // FieldOperationType: RENAME, ADD, REMOVE, TRANSFORM, ...
            op.fieldName(),
            op.typeReference());
    }
}
```

**Features:**

- `FieldAwareRule` marker interface implemented by all field operations and compositions in `Rules`
- Field operations bubble up through `seq`, `seqAll`, `choice`, `batch`, `topDown`, `bottomUp`
- CLI exposes field-level details via the `--diagnostics` flag (`aether-datafixers-cli`)
- Spring Boot Actuator `/actuator/datafixers` endpoint reports field operations per execution
- Static field-level coverage analysis available via `MigrationAnalyzer`

### 📚 Modernized Javadoc

Every public type in every module received a Javadoc pass focused on the 1.0.0 audience:

- Consistent voice and structure across modules
- Explicit thread-safety contracts on every public type
- Behavioral guarantees, ordering invariants, and failure modes documented
- `{@inheritDoc}` with explicit `@param` / `@return` to satisfy the Aether Style Guard
- Cross-references (`{@link}`) between related types repaired and expanded

### 🛡️ Pervasive Null-Safety

`@NotNull` and `@Nullable` annotations now cover the public API and internals, providing static null-safety guarantees for callers using nullability-aware tooling (IntelliJ, Checker Framework, NullAway).

```java
// API contracts are now explicit at compile time
@NotNull DataResult<Typed<T>> apply(@NotNull Typed<T> input,
                                    @Nullable DiagnosticContext context);
```

### ⚡ JMH Benchmark Suite

The new `aether-datafixers-benchmarks` module provides reproducible JMH benchmarks for the core migration paths:

```bash
# Build the shaded benchmark JAR and run the suite
./run-benchmarks.sh
```

Initial baselines are committed under `benchmark-results/` for regression comparisons.

### 🧪 Stress & Chaos Testing

New stress / chaos integration tests under `aether-datafixers-functional-tests`:

| Test                                  | Focus                                          |
|---------------------------------------|------------------------------------------------|
| `HighConcurrencyMigrationStressIT`    | 100+ concurrent migration threads              |
| `SustainedLoadMigrationStressIT`      | Minutes-long sustained throughput              |
| `MixedRegistryAccessStressIT`         | Concurrent registry read/write workloads       |
| `MemoryPressureStressIT`              | GC pressure and memory-leak detection          |
| `RandomDelaysChaosIT`                 | Random delays injected into fix execution      |
| `RandomFailuresChaosIT`               | Random failure injection in fix execution      |

Run with:

```bash
mvn verify -Pstress -pl aether-datafixers-functional-tests
mvn verify -Pit    -pl aether-datafixers-functional-tests -Dgroups=chaos
```

### 🎨 Aether Style Guard

Code style is now enforced in CI via the Aether Style Guard. The opt-in `styleguard` profile is wired up on every module and validated by `.github/workflows/ci-pr.yml` and `ci-push.yml`.

### 📖 Migration Guide & Runbook

- **v0.5.x → 1.0.0 Migration Guide** — step-by-step migration patterns, breaking changes, and troubleshooting ([`docs/migration/`](docs/migration/v0.5-to-v1.0.md))
- **Operational Runbook** — production debugging, error scenarios, monitoring, and recovery procedures ([`docs/operations/`](docs/operations/index.md))
- **Troubleshooting Guide** — common errors and debugging tips ([`docs/troubleshooting/`](docs/troubleshooting/index.md))

### 🤝 DCO & Contributor Recognition

- `DCO` file added; PRs now sign off under the Developer Certificate of Origin
- `CONTRIBUTORS.md` recognizes every contributor to the project

---

## 💥 Breaking Changes from v0.5.0

The deprecated wrappers announced in v0.5.0 have been **removed** in 1.0.0:

| Removed                                                           | Replacement                                                                                                          |
|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `de.splatgames.aether.datafixers.codec.gson.GsonOps`              | `de.splatgames.aether.datafixers.codec.json.gson.GsonOps`                                                            |
| `de.splatgames.aether.datafixers.codec.jackson.JacksonOps`        | `codec.json.jackson.JacksonJsonOps` (JSON) or the format-specific classes (`JacksonYamlOps`, `JacksonTomlOps`, `JacksonXmlOps`) |
| `TestData.jackson()`                                              | `TestData.jacksonJson()`                                                                                             |

See [docs/migration/v0.5-to-v1.0.md](docs/migration/v0.5-to-v1.0.md) for the full migration guide.

---

## 📋 Module Overview

| Module                                  | Description                                                  |
|-----------------------------------------|--------------------------------------------------------------|
| `aether-datafixers-api`                 | Core interfaces and API contracts (stable, frozen)           |
| `aether-datafixers-core`                | Default implementations                                      |
| `aether-datafixers-codec`               | DynamicOps for JSON, YAML, TOML, XML                         |
| `aether-datafixers-testkit`             | Testing utilities with fluent API + multi-format support     |
| `aether-datafixers-cli`                 | Command-line interface with multi-format handlers            |
| `aether-datafixers-schema-tools`        | Schema analysis, validation, and diffing                     |
| `aether-datafixers-spring-boot-starter` | Spring Boot 3.x auto-configuration                           |
| `aether-datafixers-examples`            | Practical usage examples                                     |
| `aether-datafixers-functional-tests`    | E2E, integration, stress, and chaos tests                    |
| `aether-datafixers-benchmarks`          | JMH benchmark suite                                          |
| `aether-datafixers-bom`                 | Bill of Materials for version management                     |

---

## 📝 Changelog

**New in 1.0.0-rc.1**

- Introduced field-level diagnostics: `FieldOperation`, `FieldOperationType`, and `FieldAwareRule` propagated through every combinator in `Rules`
- Static field-level coverage analysis added to `MigrationAnalyzer`
- CLI `--field-diagnostics` flag and Spring Boot Actuator endpoint surface field-level details
- New `aether-datafixers-benchmarks` module with JMH benchmark suite and baseline results
- Stress and chaos integration tests: `HighConcurrencyMigrationStressIT`, `SustainedLoadMigrationStressIT`, `MixedRegistryAccessStressIT`, `MemoryPressureStressIT`, `RandomDelaysChaosIT`, `RandomFailuresChaosIT`
- Comprehensive Javadoc modernization across all modules
- Pervasive `@NotNull` / `@Nullable` annotations across the public API and internals
- Aether Style Guard integrated into PR and push CI workflows
- Migration guide (v0.5.x → 1.0.0), benchmark guide, and operational runbook added
- Developer Certificate of Origin (DCO) and `CONTRIBUTORS.md` introduced
- `MigrationResult` now implements `equals` / `hashCode`
- `Finder.index()` validates non-negative indices at construction
- `Typed.encodeAndGet` now returns `DataResult` with structured error reporting
- 100+ targeted bug fixes across API, codec, core, CLI, testkit, schema-tools, and the Spring Boot starter
- Hardened devcontainer for Java / Python / Claude Code sandboxing

**Removed (previously deprecated in 0.5.0)**

- `de.splatgames.aether.datafixers.codec.gson.GsonOps` wrapper
- `de.splatgames.aether.datafixers.codec.jackson.JacksonOps` wrapper
- `TestData.jackson()` factory method

**Full Changelog:** [v0.5.0...v1.0.0-rc.1](https://github.com/aether-framework/aether-datafixers/compare/v0.5.0...v1.0.0-rc.1)

---

## 🔄 Migration from v0.5.0

### Breaking Changes

Only the three deprecated wrappers from 0.5.0 are removed (see table above). All other public API is source-compatible with v0.5.0.

### Recommended Steps

1. Update Maven / Gradle coordinates to `1.0.0-rc.1`
2. Replace any remaining imports of the removed wrappers with their replacements
3. Re-run `mvn verify` against your project; the compiler will catch every removed reference
4. Optional: opt into field-level diagnostics by inspecting `FixExecution#fieldOperations` on `MigrationReport`

Detailed walkthrough in [docs/migration/v0.5-to-v1.0.md](docs/migration/v0.5-to-v1.0.md).

---

## 🧪 RC Feedback

Please file RC feedback as GitHub issues against the [aether-datafixers repository](https://github.com/aether-framework/aether-datafixers/issues). Any blocker reported during the RC window will be considered for inclusion in the 1.0.0 GA.

---

## 🗺️ Roadmap

### v1.0.0 (GA)

- Promote `1.0.0-rc.1` to `1.0.0` after the RC feedback window
- Final documentation polish and tutorial coverage
- Semantic-versioning guarantees activated for the 1.0.x line

### Post-1.0.0

- Extended cookbook and video tutorials
- Additional codec integrations driven by community demand

---

## 📜 License

**MIT** - see `LICENSE`.
