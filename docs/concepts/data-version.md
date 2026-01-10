# DataVersion

`DataVersion` is the fundamental unit for identifying schema versions in Aether Datafixers. It's an integer-based identifier that determines which migrations need to be applied to data.

## Overview

```java
package de.splatgames.aether.datafixers.api;

public record DataVersion(int version) implements Comparable<DataVersion> {
    // ...
}
```

A `DataVersion` wraps a single integer that represents a point in your data's evolution. When data is saved, it's tagged with a `DataVersion`. When loaded, the framework compares this version against the current version to determine which fixes to apply.

## Creating DataVersions

```java
// Simple version identifiers
DataVersion v1 = new DataVersion(1);
DataVersion v2 = new DataVersion(2);
DataVersion v100 = new DataVersion(100);

// Version comparison
boolean needsMigration = savedVersion.compareTo(currentVersion) < 0;
```

## Version Numbering Strategies

### Strategy 1: Sequential Integers (Simple)

The simplest approach: increment by 1 for each change.

```java
new DataVersion(1)   // Initial release
new DataVersion(2)   // First update
new DataVersion(3)   // Second update
```

**Pros:** Simple, easy to understand
**Cons:** No semantic meaning, hard to correlate with release versions

### Strategy 2: SemVer Encoded (Recommended)

Encode semantic version as integer: `MAJOR * 100 + MINOR * 10 + PATCH` or similar.

```java
// v1.0.0 → 100, v1.1.0 → 110, v2.0.0 → 200
new DataVersion(100)  // v1.0.0
new DataVersion(110)  // v1.1.0
new DataVersion(111)  // v1.1.1
new DataVersion(200)  // v2.0.0
```

**Pros:** Correlates with release versions, leaves room for patches
**Cons:** Requires consistent encoding scheme

### Strategy 3: Date-Based

Use date-based identifiers like `YYYYMMDD`.

```java
new DataVersion(20250101)  // January 1, 2025
new DataVersion(20250115)  // January 15, 2025
new DataVersion(20250201)  // February 1, 2025
```

**Pros:** Self-documenting, easy to trace
**Cons:** Multiple changes per day need additional suffix

### Strategy 4: Build Number

Use CI/CD build numbers directly.

```java
new DataVersion(1234)  // Build #1234
new DataVersion(1235)  // Build #1235
```

**Pros:** Automatic, no manual tracking
**Cons:** Build numbers may have gaps, harder to reason about

## Best Practices

### 1. Define a Current Version Constant

Always define a constant for your current/latest version:

```java
public class MyBootstrap implements DataFixerBootstrap {
    /** The current data version. Update this when schema changes. */
    public static final DataVersion CURRENT_VERSION = new DataVersion(200);

    // ...
}
```

### 2. Document Version History

Keep a record of what changed at each version:

```java
public final class DataVersions {
    /** v1.0.0 - Initial release with basic player data */
    public static final DataVersion V1_0_0 = new DataVersion(100);

    /** v1.1.0 - Added position object, renamed playerName to name */
    public static final DataVersion V1_1_0 = new DataVersion(110);

    /** v2.0.0 - Added inventory system, converted gameMode to string */
    public static final DataVersion V2_0_0 = new DataVersion(200);

    public static final DataVersion CURRENT = V2_0_0;

    private DataVersions() {}
}
```

### 3. Never Reuse Version Numbers

Once a version is released, never change its schema. Always create a new version.

```java
// WRONG: Changing what v100 means
Schema100 {
    // Changed from "xp" to "experience" - DON'T DO THIS!
}

// RIGHT: Create a new version
Schema110 {
    // New field name "experience"
}
```

### 4. Leave Room for Patches

If using SemVer encoding, leave room for hotfixes:

```java
// Instead of:
// v1.0 → 10, v1.1 → 11, v2.0 → 20

// Use:
// v1.0.0 → 100, v1.0.1 → 101, v1.1.0 → 110
```

## Using DataVersion

### In Schemas

```java
public class Schema100 extends Schema {
    public Schema100() {
        super(new DataVersion(100), null, SimpleTypeRegistry::new);
    }
}

public class Schema110 extends Schema {
    public Schema110(Schema parent) {
        super(new DataVersion(110), parent, SimpleTypeRegistry::new);
    }
}
```

### In DataFixes

```java
public class MyFix extends SchemaDataFix {
    public MyFix(SchemaRegistry schemas) {
        super(
            "my_fix_name",
            new DataVersion(100),  // from version
            new DataVersion(110),  // to version
            schemas
        );
    }
}
```

### When Updating Data

```java
// Get version from saved data (however you store it)
int savedVersionInt = loadVersionFromFile();
DataVersion savedVersion = new DataVersion(savedVersionInt);

// Migrate to current version
TaggedDynamic migrated = fixer.update(
    data,
    savedVersion,
    MyBootstrap.CURRENT_VERSION
);
```

## Version Comparison

`DataVersion` implements `Comparable<DataVersion>`:

```java
DataVersion v100 = new DataVersion(100);
DataVersion v110 = new DataVersion(110);
DataVersion v200 = new DataVersion(200);

v100.compareTo(v110)  // < 0 (v100 is older)
v110.compareTo(v100)  // > 0 (v110 is newer)
v100.compareTo(v100)  // = 0 (same version)

// Practical usage
if (savedVersion.compareTo(currentVersion) < 0) {
    // Data needs migration
    data = fixer.update(data, savedVersion, currentVersion);
}
```

## Accessor Methods

```java
DataVersion version = new DataVersion(100);

// Get the integer value
int value = version.version();     // 100
int value = version.getVersion();  // 100 (alternative accessor)
```

## Common Patterns

### Version-Aware Save Format

Store the version alongside your data:

```json
{
  "version": 110,
  "data": {
    "name": "Steve",
    "experience": 1500
  }
}
```

```java
// When saving
JsonObject save = new JsonObject();
save.addProperty("version", MyBootstrap.CURRENT_VERSION.version());
save.add("data", serializedData);

// When loading
int version = save.get("version").getAsInt();
JsonElement data = save.get("data");
DataVersion savedVersion = new DataVersion(version);

// Migrate if needed
if (savedVersion.compareTo(MyBootstrap.CURRENT_VERSION) < 0) {
    Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, data);
    TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dynamic);
    tagged = fixer.update(tagged, savedVersion, MyBootstrap.CURRENT_VERSION);
    data = ((Dynamic<JsonElement>) tagged.value()).value();
}
```

### Skip Migration If Current

```java
public <T> TaggedDynamic updateIfNeeded(
    TaggedDynamic data,
    DataVersion from,
    DataVersion to
) {
    if (from.compareTo(to) >= 0) {
        // Already at or ahead of target version
        return data;
    }
    return fixer.update(data, from, to);
}
```

---

## Summary

| Aspect            | Details                                   |
|-------------------|-------------------------------------------|
| **Type**          | `record DataVersion(int version)`         |
| **Purpose**       | Identify schema versions                  |
| **Comparison**    | Implements `Comparable<DataVersion>`      |
| **Best Practice** | Use SemVer encoding (e.g., 100, 110, 200) |
| **Immutability**  | Immutable, thread-safe                    |

---

## Related

- [TypeReference](type-reference.md) — Type identifiers for data routing
- [Schema System](schema-system.md) — Associating versions with type definitions
- [DataFix System](datafix-system.md) — Creating migrations between versions

