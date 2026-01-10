# Schema Diffing

The `schematools.diff` package provides utilities for comparing two schemas and identifying changes at both type and field levels.

## Overview

Schema diffing helps you understand what changed between two schema versions:

- **Type-level changes**: Types added, removed, or present in both schemas
- **Field-level changes**: Fields added, removed, or modified within types

## Core Components

| Class          | Description                                              |
|----------------|----------------------------------------------------------|
| `SchemaDiffer` | Entry point with fluent API for schema comparison        |
| `SchemaDiff`   | Immutable result containing all differences              |
| `TypeDiff`     | Field-level changes for a specific type                  |
| `FieldDiff`    | Individual field change with diff kind                   |
| `DiffKind`     | Enumeration: `ADDED`, `REMOVED`, `MODIFIED`, `UNCHANGED` |

## Basic Usage

### Comparing Two Schemas

```java
import de.splatgames.aether.datafixers.schematools.diff.SchemaDiffer;
import de.splatgames.aether.datafixers.schematools.diff.SchemaDiff;

// Compare schemas (type-level only)
SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
    .diff();

// Check for changes
if (diff.hasChanges()) {
    System.out.println("Added types: " + diff.addedTypes());
    System.out.println("Removed types: " + diff.removedTypes());
    System.out.println("Common types: " + diff.commonTypes());
}
```

### Enabling Field-Level Diffing

Field-level diffing is disabled by default for performance. Enable it when you need detailed change information:

```java
SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
    .includeFieldLevel(true)  // Enable field-level analysis
    .diff();

// Access field-level changes for a specific type
diff.typeDiff(TypeReferences.PLAYER).ifPresent(typeDiff -> {
    System.out.println("Player type changes:");
    typeDiff.addedFields().forEach(f ->
        System.out.println("  + " + f.fieldName() + ": " + f.source().fieldType()));
    typeDiff.removedFields().forEach(f ->
        System.out.println("  - " + f.fieldName()));
    typeDiff.modifiedFields().forEach(f ->
        System.out.println("  ~ " + f.fieldName() + ": " + f.source().fieldType() +
                           " -> " + f.target().fieldType()));
});
```

### Ignoring Specific Types

Exclude types from comparison when they're irrelevant or intentionally different:

```java
SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
    .includeFieldLevel(true)
    .ignoreTypes(TypeReferences.DEBUG, TypeReferences.INTERNAL)
    .diff();
```

## SchemaDiff API

### Type-Level Queries

```java
SchemaDiff diff = /* ... */;

// Types only in target schema (additions)
Set<TypeReference> added = diff.addedTypes();

// Types only in source schema (removals)
Set<TypeReference> removed = diff.removedTypes();

// Types present in both schemas
Set<TypeReference> common = diff.commonTypes();

// Quick status checks
boolean hasTypeChanges = diff.hasTypeChanges();
boolean hasAnyChanges = diff.hasChanges();
int totalChanges = diff.totalChangeCount();
```

### Field-Level Queries

Field-level data is only available when `includeFieldLevel(true)` is set:

```java
// Get all type diffs (map from TypeReference to TypeDiff)
Map<TypeReference, TypeDiff> typeDiffs = diff.typeDiffs();

// Get diff for specific type
Optional<TypeDiff> playerDiff = diff.typeDiff(TypeReferences.PLAYER);

// Check for field-level changes across all types
boolean hasFieldChanges = diff.hasFieldChanges();
```

## TypeDiff API

`TypeDiff` represents field-level changes for a type present in both schemas:

```java
TypeDiff typeDiff = /* from SchemaDiff.typeDiff() */;

// Access the type reference
TypeReference ref = typeDiff.reference();

// Get source and target Type objects
Type<?> sourceType = typeDiff.sourceType();
Type<?> targetType = typeDiff.targetType();

// Filter fields by change kind
List<FieldDiff> added = typeDiff.addedFields();
List<FieldDiff> removed = typeDiff.removedFields();
List<FieldDiff> modified = typeDiff.modifiedFields();
List<FieldDiff> unchanged = typeDiff.unchangedFields();

// All field diffs
List<FieldDiff> all = typeDiff.fieldDiffs();

// Status checks
boolean hasChanges = typeDiff.hasFieldChanges();
int changeCount = typeDiff.changedFieldCount();
```

## FieldDiff API

`FieldDiff` represents a change to a single field:

```java
FieldDiff fieldDiff = /* from TypeDiff */;

// Get the diff kind
DiffKind kind = fieldDiff.kind();  // ADDED, REMOVED, MODIFIED, UNCHANGED

// Get field name
String name = fieldDiff.fieldName();

// Access source and target FieldInfo (may be null depending on kind)
FieldInfo source = fieldDiff.source();  // null for ADDED
FieldInfo target = fieldDiff.target();  // null for REMOVED

// Check if this is a change
boolean isChange = fieldDiff.isChanged();  // true for ADDED, REMOVED, MODIFIED
```

### DiffKind Values

| Kind        | Description                      | `source()` | `target()` |
|-------------|----------------------------------|------------|------------|
| `ADDED`     | Field exists only in target      | `null`     | Present    |
| `REMOVED`   | Field exists only in source      | Present    | `null`     |
| `MODIFIED`  | Field exists in both but differs | Present    | Present    |
| `UNCHANGED` | Field is identical in both       | Present    | Present    |

## FieldInfo

`FieldInfo` provides metadata about a field:

```java
FieldInfo field = fieldDiff.source();

// Field name
String name = field.name();

// Full hierarchical path (e.g., "player.position.x")
String path = field.path();

// Whether the field is optional
boolean optional = field.isOptional();

// The field's Type
Type<?> fieldType = field.fieldType();
```

## Creating FieldDiff Manually

For testing or custom analysis, you can create `FieldDiff` instances directly:

```java
// Using factory methods
FieldDiff added = FieldDiff.added(targetFieldInfo);
FieldDiff removed = FieldDiff.removed(sourceFieldInfo);
FieldDiff modified = FieldDiff.modified(sourceFieldInfo, targetFieldInfo);
FieldDiff unchanged = FieldDiff.unchanged(sourceFieldInfo, targetFieldInfo);

// Automatic comparison
FieldDiff auto = FieldDiff.compare(sourceFieldInfo, targetFieldInfo);
```

## Example: Schema Change Report

Generate a human-readable schema change report:

```java
public class SchemaChangeReport {

    public static void printReport(Schema from, Schema to) {
        SchemaDiff diff = SchemaDiffer.compare(from, to)
            .includeFieldLevel(true)
            .diff();

        System.out.println("=== Schema Change Report ===");
        System.out.println("From: v" + from.version().value());
        System.out.println("To:   v" + to.version().value());
        System.out.println();

        if (!diff.hasChanges()) {
            System.out.println("No changes detected.");
            return;
        }

        // Type additions
        if (!diff.addedTypes().isEmpty()) {
            System.out.println("Added Types:");
            diff.addedTypes().forEach(t ->
                System.out.println("  + " + t.name()));
            System.out.println();
        }

        // Type removals
        if (!diff.removedTypes().isEmpty()) {
            System.out.println("Removed Types:");
            diff.removedTypes().forEach(t ->
                System.out.println("  - " + t.name()));
            System.out.println();
        }

        // Field-level changes
        if (diff.hasFieldChanges()) {
            System.out.println("Modified Types:");
            for (TypeDiff typeDiff : diff.typeDiffs().values()) {
                if (typeDiff.hasFieldChanges()) {
                    System.out.println("  " + typeDiff.reference().name() + ":");
                    typeDiff.addedFields().forEach(f ->
                        System.out.println("    + " + f.fieldName()));
                    typeDiff.removedFields().forEach(f ->
                        System.out.println("    - " + f.fieldName()));
                    typeDiff.modifiedFields().forEach(f ->
                        System.out.println("    ~ " + f.fieldName() +
                            " (type changed)"));
                }
            }
        }

        System.out.println();
        System.out.println("Total changes: " + diff.totalChangeCount());
    }
}
```

## Performance Considerations

- **Type-level diffing** is fast and suitable for all use cases
- **Field-level diffing** requires type introspection and may be slower for schemas with many complex types
- Use `ignoreTypes()` to exclude types you don't need to analyze
- Consider caching `SchemaDiff` results if you need to query them multiple times

---

## Related

- [Type Introspection](type-introspection.md) — Understanding how types are analyzed
- [Migration Analysis](migration-analysis.md) — Using diffs to analyze migration paths
- [Schema System](../concepts/schema-system.md) — Core schema concepts
