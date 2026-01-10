# Type Theory Primer

Background on type theory concepts used in Aether Datafixers.

## Product Types

A product type combines multiple values into one. In Java, records and classes are product types:

```java
record Player(String name, int level) {}
// Player = String × Int
```

Product types are accessed with **Lenses**.

## Sum Types

A sum type represents one of several alternatives. In Java, sealed interfaces model sum types:

```java
sealed interface Shape permits Circle, Rectangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
// Shape = Circle | Rectangle
```

Sum types are accessed with **Prisms**.

## Isomorphism

Two types are isomorphic if you can convert between them without losing information:

```java
// String ≅ UUID (for valid UUID strings)
UUID fromString(String s);
String toString(UUID id);
```

Isomorphisms are represented by **Iso** optics.

## Functors

A functor is a container that supports `map`:

```java
Optional<String> name = Optional.of("Steve");
Optional<Integer> length = name.map(String::length);
```

## Applicatives

Applicatives support combining independent computations:

```java
DataResult<String> name = ...;
DataResult<Integer> level = ...;
DataResult<Player> player = name.apply2(level, Player::new);
```

## Monads

Monads support sequential, dependent computations with `flatMap`:

```java
DataResult<Config> result = parseJson(input)
    .flatMap(this::validate)
    .flatMap(this::applyDefaults);
```

## Optics

Optics are composable accessors for data structures:

| Optic     | Source    | Focus    | Get    | Modify |
|-----------|-----------|----------|--------|--------|
| Lens      | Product   | Field    | Always | Always |
| Prism     | Sum       | Variant  | Maybe  | Always |
| Iso       | Type      | Type     | Always | Always |
| Affine    | Optional  | Field    | Maybe  | Maybe  |
| Traversal | Container | Elements | All    | All    |

## Composition

Optics compose to access nested structures:

```java
Lens<Game, Player> playerLens = ...;
Lens<Player, Position> positionLens = ...;
Lens<Game, Position> gamePositionLens = playerLens.compose(positionLens);
```

## Related

- [Optics Overview](../concepts/optics/index.md)
- [Glossary](glossary.md)

