# Architecture Overview

This document provides a comprehensive view of how Aether Datafixers is structured and how data flows through the framework.

## High-Level Architecture

Aether Datafixers follows a **forward patching** model: data is migrated sequentially from older versions to newer versions by applying a series of fixes.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                              DataFixer                                    │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                        SchemaRegistry                               │  │
│  │  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐     │  │
│  │  │Schema100 │───▶│Schema110 │───▶│Schema200 │───▶│Schema210 │     │  │
│  │  │ (v1.0.0) │    │ (v1.1.0) │    │ (v2.0.0) │    │ (v2.1.0) │     │  │
│  │  └──────────┘    └──────────┘    └──────────┘    └──────────┘     │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                         FixRegistrar                                │  │
│  │  ┌────────────┐    ┌────────────┐    ┌────────────┐               │  │
│  │  │Fix 100→110 │    │Fix 110→200 │    │Fix 200→210 │               │  │
│  │  └────────────┘    └────────────┘    └────────────┘               │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

## Module Structure

The framework is divided into distinct modules with clear responsibilities:

### aether-datafixers-api

Contains all interfaces, abstract classes, and API contracts. This module defines *what* the framework can do without specifying *how*.

**Key packages:**

| Package       | Contents                                   |
|---------------|--------------------------------------------|
| `api`         | Core types: `DataVersion`, `TypeReference` |
| `api.schema`  | `Schema`, `SchemaRegistry` interfaces      |
| `api.fix`     | `DataFix`, `DataFixer`, `FixRegistrar`     |
| `api.dynamic` | `Dynamic`, `DynamicOps`, `TaggedDynamic`   |
| `api.codec`   | `Codec`, `Encoder`, `Decoder`              |
| `api.dsl`     | `DSL` for type template definitions        |
| `api.optic`   | Optics: `Lens`, `Prism`, `Finder`, etc.    |
| `api.rewrite` | `TypeRewriteRule`, `Rules`                 |
| `api.result`  | `DataResult` for error handling            |

### aether-datafixers-core

Provides default implementations of the API interfaces.

**Key packages:**

| Package          | Contents                              |
|------------------|---------------------------------------|
| `core`           | `AetherDataFixer` main implementation |
| `core.bootstrap` | `DataFixerRuntimeFactory`             |
| `core.schema`    | Schema base implementations           |
| `core.fix`       | `SchemaDataFix` base class            |
| `core.type`      | `SimpleType`, `SimpleTypeRegistry`    |
| `core.codec`     | Codec implementations                 |

### aether-datafixers-codec

Codec implementations for common serialization formats.

**Key classes:**

| Class            | Purpose                               |
|------------------|---------------------------------------|
| `GsonOps`        | `DynamicOps` for Gson's `JsonElement` |
| `JacksonJsonOps` | `DynamicOps` for Jackson's `JsonNode` |

### aether-datafixers-bom

Bill of Materials for version management across modules.

---

## Data Flow

### Migration Flow

When you call `fixer.update()`, data flows through the system like this:

```
                    Input: TaggedDynamic (old version)
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │ 1. Identify TypeReference     │
                    │    from TaggedDynamic         │
                    └───────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │ 2. Find applicable DataFixes  │
                    │    (fromVersion → toVersion)  │
                    └───────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │ 3. Apply fixes in order       │
                    │    Fix₁ → Fix₂ → Fix₃ → ...   │
                    └───────────────────────────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │ 4. Each fix transforms data   │
                    │    using TypeRewriteRule      │
                    └───────────────────────────────┘
                                    │
                                    ▼
                    Output: TaggedDynamic (new version)
```

### Bootstrap Flow

During initialization, the bootstrap wires everything together:

```
                    ┌───────────────────────────────┐
                    │ DataFixerRuntimeFactory       │
                    │      .create(version,         │
                    │              bootstrap)       │
                    └───────────────────────────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              ▼                     ▼                     ▼
   ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
   │registerSchemas() │  │ registerFixes()  │  │ Build DataFixer  │
   │                  │  │                  │  │                  │
   │ Schema100        │  │ Fix 100→110      │  │ Wire everything  │
   │ Schema110        │  │ Fix 110→200      │  │ together         │
   │ Schema200        │  │ ...              │  │                  │
   └──────────────────┘  └──────────────────┘  └──────────────────┘
```

---

## Component Relationships

### Schema and TypeRegistry

Each `Schema` owns a `TypeRegistry` that maps `TypeReference` to `Type`:

```
┌─────────────────────────────────────────────┐
│                  Schema                     │
│  ┌───────────────────────────────────────┐  │
│  │           TypeRegistry                │  │
│  │  ┌─────────────┬─────────────┐        │  │
│  │  │TypeReference│    Type     │        │  │
│  │  ├─────────────┼─────────────┤        │  │
│  │  │  "player"   │ PlayerType  │        │  │
│  │  │  "world"    │ WorldType   │        │  │
│  │  │  "entity"   │ EntityType  │        │  │
│  │  └─────────────┴─────────────┘        │  │
│  └───────────────────────────────────────┘  │
│                                             │
│  DataVersion: 100                           │
│  Parent: null                               │
└─────────────────────────────────────────────┘
```

### Schema Inheritance

Schemas can inherit from parent schemas:

```
┌─────────────────────┐
│     Schema100       │  ← Base schema (no parent)
│  DataVersion: 100   │
└─────────────────────┘
          │
          │ parent
          ▼
┌─────────────────────┐
│     Schema110       │  ← Inherits types from Schema100
│  DataVersion: 110   │     Only registers changed types
└─────────────────────┘
          │
          │ parent
          ▼
┌─────────────────────┐
│     Schema200       │  ← Inherits from Schema110
│  DataVersion: 200   │
└─────────────────────┘
```

### DataFix and Rules

A `DataFix` uses `TypeRewriteRule` to define transformations:

```
┌───────────────────────────────────────────────────────────┐
│                     SchemaDataFix                         │
│  ┌─────────────────────────────────────────────────────┐  │
│  │                 TypeRewriteRule                      │  │
│  │  ┌─────────────────────────────────────────────┐    │  │
│  │  │  Rules.seq(                                 │    │  │
│  │  │    Rules.renameField(PLAYER, "xp", "exp"),  │    │  │
│  │  │    Rules.transform(PLAYER, transform)       │    │  │
│  │  │  )                                          │    │  │
│  │  └─────────────────────────────────────────────┘    │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                           │
│  fromVersion: 100                                         │
│  toVersion: 110                                           │
└───────────────────────────────────────────────────────────┘
```

---

## Dynamic and DynamicOps

The `Dynamic<T>` wrapper decouples data manipulation from the underlying format:

```
┌────────────────────────────────────────────────────────────────┐
│                        Dynamic<JsonElement>                     │
│  ┌────────────────────┐    ┌─────────────────────────────────┐ │
│  │   DynamicOps<T>    │    │         T value                 │ │
│  │    (GsonOps)       │    │    (JsonElement)                │ │
│  └────────────────────┘    └─────────────────────────────────┘ │
│                                                                │
│  Methods:                                                      │
│  - get(key)          → Dynamic<T>                              │
│  - set(key, value)   → Dynamic<T>                              │
│  - remove(key)       → Dynamic<T>                              │
│  - asString()        → DataResult<String>                      │
│  - asInt()           → DataResult<Integer>                     │
│  - createString(s)   → Dynamic<T>                              │
└────────────────────────────────────────────────────────────────┘
```

This design allows the same fix logic to work with JSON, NBT, or any other format.

---

## Codec System

Codecs provide bidirectional transformation between typed Java objects and dynamic representations:

```
         encode()
    ┌────────────────┐
    │   Java Object  │ ──────────────────▶ Dynamic<T>
    │   (Player)     │                          │
    └────────────────┘                          │
            ▲                                   │
            │                                   │
            │         decode()                  │
            └───────────────────────────────────┘
```

### Codec Composition

Codecs can be composed to handle complex structures:

```java
RecordCodecBuilder.create(instance ->
    instance.group(
        Codecs.STRING.fieldOf("name").forGetter(Player::name),
        Codecs.INT.fieldOf("level").forGetter(Player::level),
        Position.CODEC.fieldOf("position").forGetter(Player::position)
    ).apply(instance, Player::new)
)
```

---

## Optics Integration

Optics provide composable access to nested data:

```
                    ┌─────────────────┐
                    │      Getter     │  (read-only)
                    └─────────────────┘
                            │
                            ▼
           ┌────────────────────────────────┐
           │             Affine             │  (read + optional write)
           └────────────────────────────────┘
                  ╱                   ╲
                 ╱                     ╲
    ┌───────────────────┐      ┌───────────────────┐
    │       Lens        │      │      Prism        │
    │ (focus on field)  │      │ (focus on variant)│
    └───────────────────┘      └───────────────────┘
                  ╲                   ╱
                   ╲                 ╱
            ┌───────────────────────────┐
            │           Iso             │  (bidirectional)
            └───────────────────────────┘
```

Optics are used internally by `Finder` to navigate `Dynamic` data:

```java
Finder<String> nameFinder = Finder.field("player").then(Finder.field("name"));
Dynamic<?> nameValue = nameFinder.find(rootDynamic);
```

---

## Thread Safety Model

Aether Datafixers is designed for thread-safe usage:

| Component       | Thread Safety                              |
|-----------------|--------------------------------------------|
| `DataVersion`   | Immutable, thread-safe                     |
| `TypeReference` | Immutable, thread-safe                     |
| `Schema`        | Immutable after construction               |
| `DataFixer`     | Thread-safe for concurrent updates         |
| `Dynamic`       | Immutable, operations return new instances |
| `DataResult`    | Immutable, thread-safe                     |

---

## Design Principles

### 1. Immutability

Most core types are immutable. Operations on `Dynamic` return new instances rather than modifying in place.

### 2. Format Agnosticism

The framework doesn't assume any particular serialization format. `DynamicOps` abstracts the details.

### 3. Forward Patching Only

Data always migrates forward (old → new), never backward. This simplifies reasoning about migrations.

### 4. Schema-Driven

Type definitions live in schemas, ensuring data structure is well-defined at each version.

### 5. Composition

Rules, codecs, and optics all support composition, enabling complex transformations from simple building blocks.

---

## Next Steps

- [DataVersion](data-version.md) — Understanding version identifiers
- [TypeReference](type-reference.md) — Type identification system
- [Schema System](schema-system.md) — Defining data structures

