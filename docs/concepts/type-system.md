# Type System

The type system in Aether Datafixers provides a way to define the structure of your data at each version. Types connect schema definitions to codecs and enable the framework to understand and transform your data.

## Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                          Type                                    │
│  ┌─────────────────────┐    ┌─────────────────────────────────┐  │
│  │    TypeReference    │    │           Codec                 │  │
│  │     ("player")      │    │    encode/decode logic          │  │
│  └─────────────────────┘    └─────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│                      TypeRegistry                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  TypeReference    │    Type                              │  │
│  ├───────────────────┼──────────────────────────────────────┤  │
│  │  "player"         │    PlayerType (codec for player)     │  │
│  │  "world"          │    WorldType (codec for world)       │  │
│  │  "entity"         │    EntityType (codec for entity)     │  │
│  └───────────────────┴──────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

## Core Interfaces

### Type

The `Type` interface represents a data type with encoding/decoding capabilities:

```java
public interface Type {
    /** The type reference identifying this type */
    TypeReference reference();

    /** The codec for this type */
    Codec<?> codec();
}
```

### TypeRegistry

A registry mapping type references to types:

```java
public interface TypeRegistry {
    /** Register a type */
    void register(TypeReference ref, Type type);

    /** Look up a type by reference */
    Optional<Type> get(TypeReference ref);

    /** Get all registered types */
    Map<TypeReference, Type> all();
}
```

### TypeTemplate

A template for creating types, used by the DSL:

```java
public interface TypeTemplate {
    /** Apply the template to create a Type */
    Type apply(TypeFamily family);
}
```

## SimpleType

The `SimpleType` class is the default implementation of `Type`:

```java
public class SimpleType implements Type {
    private final TypeReference reference;
    private final Codec<?> codec;

    public SimpleType(TypeReference reference, Codec<?> codec) {
        this.reference = reference;
        this.codec = codec;
    }

    @Override
    public TypeReference reference() {
        return reference;
    }

    @Override
    public Codec<?> codec() {
        return codec;
    }
}
```

## SimpleTypeRegistry

The default `TypeRegistry` implementation:

```java
public class SimpleTypeRegistry implements TypeRegistry {
    private final Map<TypeReference, Type> types = new HashMap<>();

    @Override
    public void register(TypeReference ref, Type type) {
        types.put(ref, type);
    }

    @Override
    public Optional<Type> get(TypeReference ref) {
        return Optional.ofNullable(types.get(ref));
    }

    @Override
    public Map<TypeReference, Type> all() {
        return Collections.unmodifiableMap(types);
    }
}
```

## Creating Types

### Via DSL (Recommended)

The easiest way to create types is through the DSL in a schema:

```java
public class Schema100 extends Schema {
    @Override
    protected void registerTypes() {
        // DSL.and() creates a TypeTemplate that becomes a Type
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.remainder()
        ));
    }
}
```

The `registerType` method:
1. Takes a `TypeReference` and `TypeTemplate`
2. Applies the template to create a `Type`
3. Registers the type in the schema's `TypeRegistry`

### Via TemplateBasedType

When you need to wrap a DSL-generated type:

```java
TypeTemplate template = DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType())
);

Type type = new TemplateBasedType(TypeReferences.PLAYER, template);
registry.register(TypeReferences.PLAYER, type);
```

### Via Custom Type

For complete control, implement `Type` directly:

```java
public class PlayerType implements Type {
    private static final Codec<Player> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codecs.STRING.fieldOf("name").forGetter(Player::name),
            Codecs.INT.fieldOf("level").forGetter(Player::level)
        ).apply(instance, Player::new)
    );

    @Override
    public TypeReference reference() {
        return TypeReferences.PLAYER;
    }

    @Override
    public Codec<Player> codec() {
        return CODEC;
    }
}
```

## Type Lookup

Types are looked up through the schema's type registry:

```java
// From a schema
Schema schema = schemas.get(new DataVersion(100)).orElseThrow();

// Get type for a reference
Optional<Type> playerType = schema.getType(TypeReferences.PLAYER);

// Use the type's codec
Codec<?> codec = playerType.get().codec();
```

### Inheritance Lookup

When a schema has a parent, type lookup checks both:

```java
// Schema110 inherits from Schema100
// If WORLD is only registered in Schema100:

Type worldType = schema110.getType(TypeReferences.WORLD);
// Returns the type from Schema100 (inherited)
```

## TypeFamily

`TypeFamily` provides a context for resolving recursive type references:

```java
public interface TypeFamily {
    /** Look up a type by reference */
    Type apply(TypeReference ref);
}
```

This is used internally when applying type templates that reference other types.

## Typed Wrapper

The `Typed` class wraps a value with its type information:

```java
public class Typed<A> {
    private final Type type;
    private final A value;

    public Typed(Type type, A value) {
        this.type = type;
        this.value = value;
    }

    public Type type() { return type; }
    public A value() { return value; }

    /** Transform the value */
    public <B> Typed<B> map(Function<A, B> fn, Type newType) {
        return new Typed<>(newType, fn.apply(value));
    }
}
```

## Working with Types

### Getting a Type from Schema

```java
public void processPlayer(Schema schema, Dynamic<?> data) {
    // Get the player type
    Type playerType = schema.getType(TypeReferences.PLAYER)
        .orElseThrow(() -> new IllegalStateException("PLAYER type not found"));

    // Use the codec
    Codec<?> codec = playerType.codec();

    // Decode the data
    DataResult<?> result = codec.decode(data);
}
```

### Type-Based Transformation

```java
public Dynamic<?> transformByType(
    TypeReference ref,
    Schema inputSchema,
    Schema outputSchema,
    Dynamic<?> data
) {
    Type inputType = inputSchema.getType(ref).orElseThrow();
    Type outputType = outputSchema.getType(ref).orElseThrow();

    // Decode with input type
    Object decoded = inputType.codec().decode(data).result().orElseThrow();

    // Transform...
    Object transformed = transform(decoded);

    // Encode with output type
    return outputType.codec().encode(transformed, data.ops(), data.ops().empty());
}
```

## Type Templates in Detail

### Primitive Templates

```java
DSL.string()      // Creates StringType
DSL.intType()     // Creates IntType
DSL.longType()    // Creates LongType
DSL.floatType()   // Creates FloatType
DSL.doubleType()  // Creates DoubleType
DSL.bool()        // Creates BoolType
```

### Field Template

```java
// Required field
DSL.field("name", DSL.string())

// The template produces a type that:
// - Expects a field named "name"
// - Uses string codec for the value
```

### Optional Template

```java
// Optional field with default
DSL.optional("nickname", DSL.string())

// The template produces a type that:
// - Looks for a field named "nickname"
// - Returns empty/null if missing
// - Uses string codec if present
```

### And Template (Product Type)

```java
// Combine multiple fields
DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("level", DSL.intType()),
    DSL.field("active", DSL.bool())
)

// Creates a product type with all fields
```

### List Template

```java
// List of strings
DSL.list(DSL.string())

// List of complex objects
DSL.list(DSL.and(
    DSL.field("id", DSL.intType()),
    DSL.field("name", DSL.string())
))
```

### Remainder Template

```java
// Capture unknown fields
DSL.remainder()

// When combined with other fields:
DSL.and(
    DSL.field("known", DSL.string()),
    DSL.remainder()  // Preserves any other fields
)
```

### TaggedChoice Template (Sum Type)

```java
// Discriminated union
DSL.taggedChoice(
    "type",              // Discriminator field name
    DSL.string(),        // Discriminator type
    Map.of(
        "player", playerTemplate,
        "enemy", enemyTemplate,
        "item", itemTemplate
    )
)
```

## Best Practices

### 1. Use DSL for Type Definitions

Prefer DSL templates over manual type construction:

```java
// Good: DSL is declarative and clear
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.remainder()
));

// Avoid: Manual construction is verbose
Type type = new ProductType(
    new FieldType("name", new StringType()),
    new RemainderType()
);
```

### 2. Register Types in Schemas

Keep type registration in schema classes:

```java
// Good: Types live in schemas
public class Schema100 extends Schema {
    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, ...);
    }
}
```

### 3. Extract Common Templates

Share templates between schemas:

```java
public class CommonTypes {
    public static DSL.TypeTemplate position() {
        return DSL.and(
            DSL.field("x", DSL.doubleType()),
            DSL.field("y", DSL.doubleType()),
            DSL.field("z", DSL.doubleType())
        );
    }
}

// Usage in schemas
registerType(TypeReferences.PLAYER, DSL.and(
    DSL.field("name", DSL.string()),
    DSL.field("position", CommonTypes.position()),
    DSL.remainder()
));
```

### 4. Always Include Remainder

Include `remainder()` to handle forward compatibility:

```java
// Data might have additional fields from newer versions
registerType(TypeReferences.CONFIG, DSL.and(
    DSL.field("setting1", DSL.string()),
    DSL.field("setting2", DSL.intType()),
    DSL.remainder()  // Preserve unknown fields
));
```

---

## Summary

| Component      | Purpose                             |
|----------------|-------------------------------------|
| `Type`         | Associates TypeReference with Codec |
| `TypeRegistry` | Maps TypeReference → Type           |
| `TypeTemplate` | Blueprint for creating Types        |
| `SimpleType`   | Default Type implementation         |
| `Typed<A>`     | Wrapper combining value with type   |

---

## Related

- [Schema System](schema-system.md) — Where types are registered
- [DSL](dsl.md) — Type template language
- [Codec System](codec-system.md) — Encoding/decoding types

