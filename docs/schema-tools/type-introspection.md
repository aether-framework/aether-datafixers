# Type Introspection

The `schematools.introspection` package provides utilities for inspecting the internal structure of Types, extracting field information, and creating normalized representations for comparison.

## Overview

Type introspection enables:

- **Structure Analysis** — Understand the shape of complex nested types
- **Field Extraction** — Get metadata about all fields in a type
- **Type Classification** — Categorize types (primitive, list, product, etc.)
- **Comparison Support** — Create normalized structures for equality checking

## Core Components

| Class              | Description                                     |
|--------------------|-------------------------------------------------|
| `TypeIntrospector` | Utility class for analyzing types               |
| `TypeStructure`    | Normalized, comparable representation of a type |
| `FieldInfo`        | Metadata about a single field                   |

## Basic Usage

### Inspecting a Type

```java
import de.splatgames.aether.datafixers.schematools.introspection.TypeIntrospector;
import de.splatgames.aether.datafixers.schematools.introspection.TypeStructure;

Type<?> playerType = schema.getType(TypeReferences.PLAYER);

// Get complete structure
TypeStructure structure = TypeIntrospector.introspect(playerType);

System.out.println("Type: " + structure.reference());
System.out.println("Kind: " + structure.kind());
System.out.println("Fields: " + structure.fields().size());
```

### Extracting Fields

```java
import de.splatgames.aether.datafixers.schematools.introspection.FieldInfo;

Type<?> playerType = schema.getType(TypeReferences.PLAYER);

// Get all fields (including nested)
List<FieldInfo> fields = TypeIntrospector.extractFields(playerType);

for (FieldInfo field : fields) {
    System.out.println(field.path() + ": " + field.fieldType() +
        (field.isOptional() ? " (optional)" : ""));
}
```

### Checking for Fields

```java
Type<?> playerType = schema.getType(TypeReferences.PLAYER);

// Check if a field exists
boolean hasHealth = TypeIntrospector.hasField(playerType, "health");
boolean hasPosition = TypeIntrospector.hasField(playerType, "position");
```

## TypeIntrospector API

### Static Methods

```java
// Full structural analysis
TypeStructure structure = TypeIntrospector.introspect(type);

// Extract all fields recursively
List<FieldInfo> fields = TypeIntrospector.extractFields(type);

// Check field existence
boolean exists = TypeIntrospector.hasField(type, "fieldName");

// Determine type kind
TypeKind kind = TypeIntrospector.determineKind(type);
```

### TypeKind Values

The introspector classifies types into these categories:

| Kind            | Description                  | Examples                                                            |
|-----------------|------------------------------|---------------------------------------------------------------------|
| `PRIMITIVE`     | Basic value types            | `bool`, `int`, `long`, `float`, `double`, `byte`, `short`, `string` |
| `LIST`          | Collection of elements       | `DSL.list(DSL.string())`                                            |
| `OPTIONAL`      | May or may not have value    | `DSL.optional(DSL.int())`                                           |
| `PRODUCT`       | Combination of fields (AND)  | `DSL.and(field1, field2)`                                           |
| `SUM`           | Choice between variants (OR) | `DSL.or(type1, type2)`                                              |
| `FIELD`         | Named field wrapper          | `DSL.field("name", DSL.string())`                                   |
| `TAGGED_CHOICE` | Discriminated union          | Tagged choice types                                                 |
| `NAMED`         | Named/referenced type        | Type with a name reference                                          |
| `PASSTHROUGH`   | Pass-through wrapper         | Passthrough types                                                   |
| `UNKNOWN`       | Unrecognized type            | Custom or complex types                                             |

## TypeStructure API

`TypeStructure` provides a normalized view of a type for analysis and comparison:

### Properties

```java
TypeStructure structure = TypeIntrospector.introspect(type);

// Type reference (if available)
Optional<TypeReference> ref = structure.reference();

// Human-readable description
String desc = structure.description();

// Classification
TypeKind kind = structure.kind();

// Direct fields at this level
List<FieldInfo> fields = structure.fields();

// Child structures (for nested types)
List<TypeStructure> children = structure.children();
```

### Structural Comparison

Compare types structurally, ignoring cosmetic differences:

```java
TypeStructure struct1 = TypeIntrospector.introspect(type1);
TypeStructure struct2 = TypeIntrospector.introspect(type2);

// Compare structure (ignores description)
boolean same = struct1.structurallyEquals(struct2);
```

### Building TypeStructure

For testing or custom analysis:

```java
TypeStructure structure = TypeStructure.builder()
    .reference(TypeReferences.PLAYER)
    .description("Player data type")
    .kind(TypeKind.PRODUCT)
    .field(FieldInfo.of(nameFieldType))
    .field(FieldInfo.of(healthFieldType))
    .child(positionStructure)
    .build();
```

## FieldInfo API

`FieldInfo` contains metadata about a single field:

### Properties

```java
FieldInfo field = /* from TypeIntrospector or TypeStructure */;

// Field name
String name = field.name();

// Full hierarchical path (e.g., "player.position.x")
String path = field.path();

// Whether the field is optional
boolean optional = field.isOptional();

// The field's Type
Type<?> type = field.fieldType();
```

### Creating FieldInfo

```java
// From a field Type
FieldInfo field = FieldInfo.of(fieldType);

// With path prefix (for nested structures)
FieldInfo nested = FieldInfo.of(fieldType, "parent.child");

// Full control
FieldInfo custom = FieldInfo.create("myField", false, someType, "root.myField");
```

### Hierarchical Paths

Field paths represent the full location in nested structures:

```java
// Given this type structure:
// player {
//   name: string
//   position {
//     x: int
//     y: int
//     z: int
//   }
// }

List<FieldInfo> fields = TypeIntrospector.extractFields(playerType);

// Fields will have paths like:
// - "name"
// - "position"
// - "position.x"
// - "position.y"
// - "position.z"
```

## Example: Type Documentation Generator

Generate documentation from type definitions:

```java
public class TypeDocGenerator {

    public static void document(Schema schema) {
        System.out.println("# Schema v" + schema.version().value() + " Types");
        System.out.println();

        for (TypeReference ref : schema.allTypes()) {
            Type<?> type = schema.getType(ref);
            TypeStructure structure = TypeIntrospector.introspect(type);

            System.out.println("## " + ref.name());
            System.out.println();
            System.out.println("**Kind:** " + structure.kind());
            System.out.println();

            List<FieldInfo> fields = structure.fields();
            if (!fields.isEmpty()) {
                System.out.println("**Fields:**");
                System.out.println();
                System.out.println("| Name | Type | Optional |");
                System.out.println("|------|------|----------|");
                for (FieldInfo field : fields) {
                    System.out.println("| `" + field.name() + "` | " +
                        describeType(field.fieldType()) + " | " +
                        (field.isOptional() ? "Yes" : "No") + " |");
                }
                System.out.println();
            }

            // Recursively document nested fields
            List<FieldInfo> allFields = TypeIntrospector.extractFields(type);
            List<FieldInfo> nestedFields = allFields.stream()
                .filter(f -> f.path().contains("."))
                .toList();

            if (!nestedFields.isEmpty()) {
                System.out.println("**Nested Fields:**");
                System.out.println();
                for (FieldInfo field : nestedFields) {
                    System.out.println("- `" + field.path() + "`: " +
                        describeType(field.fieldType()));
                }
                System.out.println();
            }
        }
    }

    private static String describeType(Type<?> type) {
        TypeKind kind = TypeIntrospector.determineKind(type);
        return switch (kind) {
            case PRIMITIVE -> type.toString();
            case LIST -> "list";
            case OPTIONAL -> "optional";
            case PRODUCT -> "object";
            case SUM -> "union";
            default -> kind.name().toLowerCase();
        };
    }
}
```

## Example: Schema Compatibility Checker

Check if two types are structurally compatible:

```java
public class CompatibilityChecker {

    public static boolean areCompatible(Type<?> oldType, Type<?> newType) {
        List<FieldInfo> oldFields = TypeIntrospector.extractFields(oldType);
        List<FieldInfo> newFields = TypeIntrospector.extractFields(newType);

        // Build maps for comparison
        Map<String, FieldInfo> oldByPath = oldFields.stream()
            .collect(Collectors.toMap(FieldInfo::path, f -> f));
        Map<String, FieldInfo> newByPath = newFields.stream()
            .collect(Collectors.toMap(FieldInfo::path, f -> f));

        // Check for breaking changes
        for (FieldInfo oldField : oldFields) {
            FieldInfo newField = newByPath.get(oldField.path());

            if (newField == null) {
                // Field removed - breaking if it was required
                if (!oldField.isOptional()) {
                    System.out.println("BREAKING: Required field removed: " +
                        oldField.path());
                    return false;
                }
            } else {
                // Field exists in both - check type compatibility
                TypeKind oldKind = TypeIntrospector.determineKind(oldField.fieldType());
                TypeKind newKind = TypeIntrospector.determineKind(newField.fieldType());

                if (oldKind != newKind) {
                    System.out.println("BREAKING: Field type changed: " +
                        oldField.path() + " (" + oldKind + " -> " + newKind + ")");
                    return false;
                }

                // Optional -> Required is breaking
                if (oldField.isOptional() && !newField.isOptional()) {
                    System.out.println("BREAKING: Field became required: " +
                        oldField.path());
                    return false;
                }
            }
        }

        // Check new required fields
        for (FieldInfo newField : newFields) {
            if (!oldByPath.containsKey(newField.path()) && !newField.isOptional()) {
                System.out.println("BREAKING: New required field: " + newField.path());
                return false;
            }
        }

        return true;
    }
}
```

## Example: Field Dependency Analysis

Find which fields depend on other types:

```java
public class DependencyAnalyzer {

    public static Map<TypeReference, Set<String>> findDependencies(Schema schema) {
        Map<TypeReference, Set<String>> dependencies = new HashMap<>();

        for (TypeReference ref : schema.allTypes()) {
            Type<?> type = schema.getType(ref);
            Set<String> deps = new HashSet<>();

            for (FieldInfo field : TypeIntrospector.extractFields(type)) {
                TypeStructure fieldStructure =
                    TypeIntrospector.introspect(field.fieldType());

                fieldStructure.reference().ifPresent(depRef -> {
                    if (!depRef.equals(ref)) {  // Exclude self-references
                        deps.add(depRef.name() + " (via " + field.path() + ")");
                    }
                });
            }

            if (!deps.isEmpty()) {
                dependencies.put(ref, deps);
            }
        }

        return dependencies;
    }
}
```

## Performance Notes

- **Introspection is recursive** — Complex nested types require more processing
- **Results are immutable** — Safe to cache `TypeStructure` and `FieldInfo` instances
- **Field extraction traverses all levels** — For large types, consider limiting depth
- **Use `hasField()` for simple checks** — Faster than extracting all fields

---

## Related

- [Schema Diffing](schema-diffing.md) — Uses introspection for field-level diffs
- [Schema Validation](schema-validation.md) — Uses introspection for convention checking
- [Type System](../concepts/type-system.md) — Core type concepts
- [DSL](../concepts/dsl.md) — How types are defined
