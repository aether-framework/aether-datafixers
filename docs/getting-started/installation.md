# Installation

This guide covers how to add Aether Datafixers to your project using Maven or Gradle.

## Modules Overview

Aether Datafixers is modular. Choose the modules you need:

| Module                                  | Purpose                        | When to Use                             |
|-----------------------------------------|--------------------------------|-----------------------------------------|
| `aether-datafixers-api`                 | Core interfaces                | Always (transitive dependency)          |
| `aether-datafixers-core`                | Default implementations        | Always needed for runtime               |
| `aether-datafixers-codec`               | GsonOps, JacksonJsonOps        | When working with JSON                  |
| `aether-datafixers-spring-boot-starter` | Spring Boot integration        | For Spring Boot applications            |
| `aether-datafixers-cli`                 | Command-line interface         | For CLI-based data migration            |
| `aether-datafixers-schema-tools`        | Schema analysis and validation | For CI/CD validation, diffing, coverage |
| `aether-datafixers-testkit`             | Testing utilities              | For unit/integration testing            |
| `aether-datafixers-bom`                 | Version management             | Recommended for multi-module projects   |

## Maven

### Basic Setup

Add the core dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-core</artifactId>
    <version>0.4.0</version>
</dependency>
```

### With JSON Support (Gson)

```xml
<dependencies>
    <dependency>
        <groupId>de.splatgames.aether</groupId>
        <artifactId>aether-datafixers-core</artifactId>
        <version>0.4.0</version>
    </dependency>
    <dependency>
        <groupId>de.splatgames.aether</groupId>
        <artifactId>aether-datafixers-codec</artifactId>
        <version>0.4.0</version>
    </dependency>
    <!-- Gson is optional in codec module, add explicitly -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.13.2</version>
    </dependency>
</dependencies>
```

### With Testing Support

Add the testkit module for unit testing your migrations:

```xml
<dependencies>
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-core</artifactId>
        <version>0.4.0</version>
    </dependency>
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-codec</artifactId>
        <version>0.4.0</version>
    </dependency>
    <!-- Testkit for unit testing -->
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-testkit</artifactId>
        <version>0.4.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Using the BOM (Recommended)

The Bill of Materials (BOM) ensures consistent versions across all modules:

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
    <!-- No version needed when using BOM -->
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-core</artifactId>
    </dependency>
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-codec</artifactId>
    </dependency>
    <!-- Testkit for testing -->
    <dependency>
        <groupId>de.splatgames.aether.datafixers</groupId>
        <artifactId>aether-datafixers-testkit</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Gradle

### Groovy DSL

```groovy
dependencies {
    implementation 'de.splatgames.aether:aether-datafixers-core:0.4.0'
}
```

With JSON support:

```groovy
dependencies {
    implementation 'de.splatgames.aether:aether-datafixers-core:0.4.0'
    implementation 'de.splatgames.aether:aether-datafixers-codec:0.4.0'
    implementation 'com.google.code.gson:gson:2.13.2'
}
```

### Kotlin DSL

```kotlin
dependencies {
    implementation("de.splatgames.aether:aether-datafixers-core:0.4.0")
}
```

With JSON support:

```kotlin
dependencies {
    implementation("de.splatgames.aether:aether-datafixers-core:0.4.0")
    implementation("de.splatgames.aether:aether-datafixers-codec:0.4.0")
    implementation("com.google.code.gson:gson:2.13.2")
}
```

### With Testing Support

```groovy
dependencies {
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-core:0.4.0'
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-codec:0.4.0'
    testImplementation 'de.splatgames.aether.datafixers:aether-datafixers-testkit:0.4.0'
}
```

### Using the BOM

**Groovy DSL:**

```groovy
dependencies {
    implementation platform('de.splatgames.aether.datafixers:aether-datafixers-bom:0.4.0')

    // No version needed
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-core'
    implementation 'de.splatgames.aether.datafixers:aether-datafixers-codec'
    testImplementation 'de.splatgames.aether.datafixers:aether-datafixers-testkit'
}
```

**Kotlin DSL:**

```kotlin
dependencies {
    implementation(platform("de.splatgames.aether.datafixers:aether-datafixers-bom:0.4.0"))

    // No version needed
    implementation("de.splatgames.aether.datafixers:aether-datafixers-core")
    implementation("de.splatgames.aether.datafixers:aether-datafixers-codec")
    testImplementation("de.splatgames.aether.datafixers:aether-datafixers-testkit")
}
```

---

## Verifying Installation

Create a simple test to verify the installation:

```java
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;

public class VerifyInstallation {
    public static void main(String[] args) {
        DataVersion version = new DataVersion(100);
        TypeReference type = new TypeReference("test");

        System.out.println("Version: " + version.getVersion());
        System.out.println("Type: " + type.getId());
        System.out.println("Aether Datafixers installed successfully!");
    }
}
```

Expected output:
```
Version: 100
Type: test
Aether Datafixers installed successfully!
```

---

## Transitive Dependencies

Aether Datafixers has minimal dependencies:

| Dependency            | Scope    | Purpose                        |
|-----------------------|----------|--------------------------------|
| Guava                 | Compile  | `Preconditions` for validation |
| JetBrains Annotations | Compile  | `@NotNull`, `@Nullable`        |
| SLF4J API             | Compile  | Logging interface              |
| Gson                  | Optional | JSON support (codec module)    |
| Jackson               | Optional | JSON support (codec module)    |

---

## Spring Boot Installation

For Spring Boot applications, use the dedicated starter module:

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-spring-boot-starter</artifactId>
</dependency>
```

This includes auto-configuration for:
- Automatic DataFixer bean creation from `DataFixerBootstrap` beans
- `MigrationService` with fluent API
- Health indicators and Actuator endpoints
- Micrometer metrics integration

→ [Spring Boot Quick Start](../spring-boot/getting-started.md) — Get started with Spring Boot integration

---

## CLI Installation

For command-line usage without writing Java code, see the dedicated CLI documentation:

→ [CLI Installation](../cli/installation.md) — Build and run the CLI tool

---

## Schema Tools Installation

For schema analysis, validation, and migration coverage checking:

```xml
<dependency>
    <groupId>de.splatgames.aether.datafixers</groupId>
    <artifactId>aether-datafixers-schema-tools</artifactId>
</dependency>
```

→ [Schema Tools Overview](../schema-tools/index.md) — Learn about schema diffing, validation, and analysis

---

## Next Steps

Now that you have Aether Datafixers installed, proceed to:

→ [Quick Start](quick-start.md) — Get a working example in 5 minutes
