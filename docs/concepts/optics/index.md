# Optics

Optics are composable, type-safe accessors for navigating and transforming nested data structures. They provide a powerful abstraction for working with deeply nested or optional data in a functional way.

## What are Optics?

Optics generalize the concept of getters and setters. Instead of writing specialized code for each nested field access, optics let you compose simple accessors into complex ones:

```java
// Without optics: Manual nested access
String cityName = person.getAddress().getCity().getName();

// With optics: Composable accessor
Lens<Person, String> cityName = personAddress.compose(addressCity).compose(cityName);
String name = cityName.get(person);
```

## Optic Hierarchy

Optics form a hierarchy based on their capabilities:

```
                    ┌─────────────────┐
                    │      Getter     │  (read-only)
                    │   S → A         │
                    └─────────────────┘
                            │
                            ▼
           ┌────────────────────────────────┐
           │             Affine             │  (0 or 1 focus)
           │   S → Maybe A                  │
           │   (S, A) → S                   │
           └────────────────────────────────┘
                  ╱                   ╲
                 ╱                     ╲
    ┌───────────────────┐      ┌───────────────────┐
    │       Lens        │      │      Prism        │
    │   (exactly 1)     │      │   (0 or 1)        │
    │   focus on field  │      │   focus on case   │
    └───────────────────┘      └───────────────────┘
                  ╲                   ╱
                   ╲                 ╱
            ┌───────────────────────────┐
            │           Iso             │  (bidirectional)
            │   A ↔ B                   │
            └───────────────────────────┘
                          │
                          ▼
            ┌───────────────────────────┐
            │        Traversal          │  (0 to many)
            │   S → [A]                 │
            └───────────────────────────┘
```

## Optic Types

| Optic                     | Focus Count       | Primary Use             |
|---------------------------|-------------------|-------------------------|
| [Getter](getter.md)       | 1 (read-only)     | Extract a value         |
| [Lens](lens.md)           | Exactly 1         | Access a field          |
| [Prism](prism.md)         | 0 or 1            | Match a case/variant    |
| [Affine](affine.md)       | 0 or 1            | Optional field access   |
| [Iso](iso.md)             | 1 (bidirectional) | Type conversion         |
| [Traversal](traversal.md) | 0 to many         | Collection elements     |
| [Finder](finder.md)       | 0 or 1            | Dynamic data navigation |

## Quick Examples

### Lens: Access a Field

```java
// Define a lens for the "name" field
Lens<Player, String> playerName = Lens.of(
    Player::name,                    // getter
    (player, name) -> player.withName(name)  // setter
);

// Use the lens
Player player = new Player("Steve", 10);
String name = playerName.get(player);        // "Steve"
Player renamed = playerName.set(player, "Alex");  // Player("Alex", 10)
```

### Prism: Match a Variant

```java
// Define a prism for the "Some" case of Optional
Prism<Optional<String>, String> some = Prism.of(
    opt -> opt.orElse(null),         // extract (may return null)
    Optional::of                      // construct
);

// Use the prism
Optional<String> opt = Optional.of("hello");
String value = some.getOptional(opt).orElse("default");  // "hello"
```

### Finder: Navigate Dynamic Data

```java
// Navigate to nested field in Dynamic
Finder<String> playerName = Finder.field("player")
    .then(Finder.field("name"))
    .then(Finder.asString());

// Use the finder
Dynamic<?> data = ...;
Optional<String> name = playerName.find(data);
```

## Composition

Optics can be composed to access deeply nested data:

```java
// Individual optics
Lens<Company, Address> companyAddress = ...;
Lens<Address, City> addressCity = ...;
Lens<City, String> cityName = ...;

// Compose into a single optic
Lens<Company, String> companyCityName = companyAddress
    .compose(addressCity)
    .compose(cityName);

// Access nested data
String name = companyCityName.get(company);
Company updated = companyCityName.set(company, "New York");
```

### Composition Rules

| First ◦ Second  | Result    |
|-----------------|-----------|
| Lens ◦ Lens     | Lens      |
| Lens ◦ Prism    | Affine    |
| Prism ◦ Lens    | Affine    |
| Prism ◦ Prism   | Prism     |
| Affine ◦ Affine | Affine    |
| Iso ◦ Iso       | Iso       |
| Traversal ◦ any | Traversal |

## Optics in Aether Datafixers

Optics are used internally for:

1. **Type navigation**: Navigating through schema type definitions
2. **Finder operations**: `Finder` optic for navigating `Dynamic` data
3. **Transformation rules**: Focusing on specific parts of data to transform

### Finder for Dynamic Data

The most commonly used optic in data migration is `Finder`:

```java
// Find a nested value in Dynamic data
Finder<Integer> playerLevel = Finder.field("player")
    .then(Finder.field("stats"))
    .then(Finder.field("level"))
    .then(Finder.asInt());

Dynamic<?> data = loadData();
Optional<Integer> level = playerLevel.find(data);
```

### Using Optics in Fixes

```java
@Override
protected TypeRewriteRule makeRule(Schema input, Schema output) {
    // Finder to locate the field to transform
    Finder<Dynamic<?>> gameModeFinder = Finder.type(TypeReferences.PLAYER)
        .then(Finder.field("gameMode"));

    return Rules.transformAt(gameModeFinder, this::convertGameMode);
}
```

## Benefits of Optics

### 1. Composability

Build complex accessors from simple ones:

```java
// Simple optics
Lens<A, B> ab = ...;
Lens<B, C> bc = ...;
Lens<C, D> cd = ...;

// Compose into complex accessor
Lens<A, D> ad = ab.compose(bc).compose(cd);
```

### 2. Type Safety

Optics preserve types through composition:

```java
Lens<Person, Address> address = ...;
Lens<Address, String> street = ...;

// Type-safe: result is Lens<Person, String>
Lens<Person, String> personStreet = address.compose(street);
```

### 3. Immutability Support

Optics work naturally with immutable data:

```java
// Setting returns a new object
Person updated = personName.set(person, "Alex");
// Original is unchanged
assert person.name().equals("Steve");
```

### 4. Abstraction

Same optic interface for different data sources:

```java
// Works with POJOs
Lens<Player, String> pojoName = ...;

// Works with Dynamic
Finder<String> dynamicName = Finder.field("name").then(Finder.asString());

// Works with records
Lens<PlayerRecord, String> recordName = ...;
```

## When to Use Each Optic

| Scenario                            | Optic     |
|-------------------------------------|-----------|
| Access a required field             | Lens      |
| Access an optional field            | Affine    |
| Match one of several variants       | Prism     |
| Convert between equivalent types    | Iso       |
| Access all elements in a collection | Traversal |
| Read-only access                    | Getter    |
| Navigate Dynamic data               | Finder    |

---

## Detailed Guides

- [Lens](lens.md) — Focus on exactly one field
- [Prism](prism.md) — Focus on a variant
- [Iso](iso.md) — Bidirectional conversion
- [Affine](affine.md) — Optional focus
- [Traversal](traversal.md) — Multiple focus points
- [Getter](getter.md) — Read-only access
- [Finder](finder.md) — Dynamic data navigation

---

## Related

- [DSL](../dsl.md) — Type definitions with optic support
- [Dynamic System](../dynamic-system.md) — Where Finder is used
- [Rewrite Rules](../rewrite-rules.md) — Rules use optics internally

