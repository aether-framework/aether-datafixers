# Installation

This guide covers how to add Aether Datafixers to your project using Maven or Gradle.

## Modules Overview

Aether Datafixers is modular. Choose the modules you need:

| Module | Purpose | When to Use |
|--------|---------|-------------|
| `aether-datafixers-api` | Core interfaces | Always (transitive dependency) |
| `aether-datafixers-core` | Default implementations | Always needed for runtime |
| `aether-datafixers-codec` | GsonOps, JacksonOps | When working with JSON |
| `aether-datafixers-bom` | Version management | Recommended for multi-module projects |

## Maven

### Basic Setup

Add the core dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>de.splatgames.aether</groupId>
    <artifactId>aether-datafixers-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

### With JSON Support (Gson)

```xml
<dependencies>
    <dependency>
        <groupId>de.splatgames.aether</groupId>
        <artifactId>aether-datafixers-core</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>de.splatgames.aether</groupId>
        <artifactId>aether-datafixers-codec</artifactId>
        <version>0.1.0</version>
    </dependency>
    <!-- Gson is optional in codec module, add explicitly -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.11.0</version>
    </dependency>
</dependencies>
```

### Using the BOM (Recommended)

The Bill of Materials (BOM) ensures consistent versions across all modules:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>de.splatgames.aether</groupId>
            <artifactId>aether-datafixers-bom</artifactId>
            <version>0.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- No version needed when using BOM -->
    <dependency>
        <groupId>de.splatgames.aether</groupId>
        <artifactId>aether-datafixers-core</artifactId>
    </dependency>
    <dependency>
        <groupId>de.splatgames.aether</groupId>
        <artifactId>aether-datafixers-codec</artifactId>
    </dependency>
</dependencies>
```

---

## Gradle

### Groovy DSL

```groovy
dependencies {
    implementation 'de.splatgames.aether:aether-datafixers-core:0.1.0'
}
```

With JSON support:

```groovy
dependencies {
    implementation 'de.splatgames.aether:aether-datafixers-core:0.1.0'
    implementation 'de.splatgames.aether:aether-datafixers-codec:0.1.0'
    implementation 'com.google.code.gson:gson:2.11.0'
}
```

### Kotlin DSL

```kotlin
dependencies {
    implementation("de.splatgames.aether:aether-datafixers-core:0.1.0")
}
```

With JSON support:

```kotlin
dependencies {
    implementation("de.splatgames.aether:aether-datafixers-core:0.1.0")
    implementation("de.splatgames.aether:aether-datafixers-codec:0.1.0")
    implementation("com.google.code.gson:gson:2.11.0")
}
```

### Using the BOM

**Groovy DSL:**

```groovy
dependencies {
    implementation platform('de.splatgames.aether:aether-datafixers-bom:0.1.0')

    // No version needed
    implementation 'de.splatgames.aether:aether-datafixers-core'
    implementation 'de.splatgames.aether:aether-datafixers-codec'
}
```

**Kotlin DSL:**

```kotlin
dependencies {
    implementation(platform("de.splatgames.aether:aether-datafixers-bom:0.1.0"))

    // No version needed
    implementation("de.splatgames.aether:aether-datafixers-core")
    implementation("de.splatgames.aether:aether-datafixers-codec")
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

| Dependency | Scope | Purpose |
|------------|-------|---------|
| Guava | Compile | `Preconditions` for validation |
| JetBrains Annotations | Compile | `@NotNull`, `@Nullable` |
| SLF4J API | Compile | Logging interface |
| Gson | Optional | JSON support (codec module) |
| Jackson | Optional | JSON support (codec module) |

---

## Next Steps

Now that you have Aether Datafixers installed, proceed to:

→ [Quick Start](quick-start.md) — Get a working example in 5 minutes
