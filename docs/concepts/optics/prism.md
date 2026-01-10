# Prism

A **Prism** is an optic that focuses on one case of a sum type (sealed interface, enum, or union). Unlike a Lens which always succeeds, a Prism may or may not find its focus depending on which variant the data represents.

## Definition

```java
public interface Prism<S, A> extends Optic<S, A> {
    /** Try to extract the value if this case matches */
    Optional<A> getOptional(S source);

    /** Construct the source from the focused value */
    S reverseGet(A value);
}
```

**Type Parameters:**
- `S` — The sum type (the whole)
- `A` — The focused case type (the part)

## Conceptual Model

A sum type represents "one of several possibilities":

```
┌──────────────────────────────────────────────────────────────┐
│                         Result<T>                            │
│     ┌─────────────────────┐  ┌─────────────────────────┐     │
│     │      Success        │  │       Failure           │     │
│     │  ┌───────────────┐  │  │  ┌───────────────────┐  │     │
│     │  │   value: T    │  │  │  │  error: String    │  │     │
│     │  └───────────────┘  │  │  └───────────────────┘  │     │
│     └─────────────────────┘  └─────────────────────────┘     │
│                                                              │
│  Prism<Result<T>, T> success = ...  // Focuses on Success    │
│  Prism<Result<T>, String> failure = ... // Focuses on Fail   │
└──────────────────────────────────────────────────────────────┘
```

## Creating Prisms

### For Sealed Types

```java
public sealed interface Shape {
    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}
    record Triangle(double a, double b, double c) implements Shape {}
}

// Prism for the Circle case
Prism<Shape, Shape.Circle> circlePrism = Prism.of(
    shape -> shape instanceof Shape.Circle c ? Optional.of(c) : Optional.empty(),
    circle -> circle
);

// Prism for Circle's radius
Prism<Shape, Double> circleRadius = circlePrism.compose(Lens.of(
    Shape.Circle::radius,
    (c, r) -> new Shape.Circle(r)
));
```

### For Optional Types

```java
// Prism for Optional.of (the "Some" case)
Prism<Optional<String>, String> some = Prism.of(
    opt -> opt,  // Optional already represents the optional nature
    Optional::of
);

// Usage
Optional<String> present = Optional.of("hello");
Optional<String> absent = Optional.empty();

some.getOptional(present)  // Optional.of("hello")
some.getOptional(absent)   // Optional.empty()
```

### For Either Types

```java
public sealed interface Either<L, R> {
    record Left<L, R>(L value) implements Either<L, R> {}
    record Right<L, R>(R value) implements Either<L, R> {}
}

// Prism for the Left case
Prism<Either<String, Integer>, String> left = Prism.of(
    either -> either instanceof Either.Left<String, Integer> l
        ? Optional.of(l.value())
        : Optional.empty(),
    value -> new Either.Left<>(value)
);

// Prism for the Right case
Prism<Either<String, Integer>, Integer> right = Prism.of(
    either -> either instanceof Either.Right<String, Integer> r
        ? Optional.of(r.value())
        : Optional.empty(),
    value -> new Either.Right<>(value)
);
```

## Using Prisms

### getOptional

Try to extract the focused case:

```java
Shape circle = new Shape.Circle(5.0);
Shape rectangle = new Shape.Rectangle(3.0, 4.0);

circlePrism.getOptional(circle)     // Optional.of(Circle(5.0))
circlePrism.getOptional(rectangle)  // Optional.empty()
```

### reverseGet

Construct the sum type from the focused case:

```java
Shape.Circle circle = new Shape.Circle(5.0);
Shape shape = circlePrism.reverseGet(circle);  // Circle(5.0) as Shape
```

### modify

Transform the focused case if it matches:

```java
Shape circle = new Shape.Circle(5.0);
Shape rectangle = new Shape.Rectangle(3.0, 4.0);

// Double the radius if it's a circle
Shape doubled = circlePrism.modify(circle, c -> new Shape.Circle(c.radius() * 2));
// Circle(10.0)

// No change for rectangle
Shape unchanged = circlePrism.modify(rectangle, c -> new Shape.Circle(c.radius() * 2));
// Rectangle(3.0, 4.0) - unchanged
```

## Composition

### Prism with Lens

Composing a Prism with a Lens gives an Affine:

```java
// Prism to match Circle
Prism<Shape, Shape.Circle> circlePrism = ...;

// Lens to access radius
Lens<Shape.Circle, Double> radiusLens = Lens.of(
    Shape.Circle::radius,
    (c, r) -> new Shape.Circle(r)
);

// Affine to optionally access radius of a shape
Affine<Shape, Double> shapeRadius = circlePrism.compose(radiusLens);

// Usage
shapeRadius.getOptional(circle)     // Optional.of(5.0)
shapeRadius.getOptional(rectangle)  // Optional.empty()
```

### Prism with Prism

Composing Prisms gives another Prism:

```java
// Nested sum types
public sealed interface Result {
    record Success(Value value) implements Result {}
    record Failure(Error error) implements Result {}
}

public sealed interface Value {
    record IntValue(int i) implements Value {}
    record StringValue(String s) implements Value {}
}

Prism<Result, Value> successValue = ...;
Prism<Value, Integer> intValue = ...;

// Composed prism: Result → int (if Success with IntValue)
Prism<Result, Integer> successInt = successValue.compose(intValue);
```

## Prism Laws

A proper Prism must satisfy these laws:

### 1. Partial Roundtrip

If you successfully extract a value, constructing gives you back the original:

```java
// If getOptional(s).isPresent(), then reverseGet(getOptional(s).get()) == s
Shape circle = new Shape.Circle(5.0);
Optional<Shape.Circle> extracted = circlePrism.getOptional(circle);
assert extracted.isPresent();
assert circlePrism.reverseGet(extracted.get()).equals(circle);
```

### 2. Total Roundtrip

Extracting from a constructed value always succeeds:

```java
// getOptional(reverseGet(a)) == Optional.of(a)
Shape.Circle original = new Shape.Circle(5.0);
Shape constructed = circlePrism.reverseGet(original);
assert circlePrism.getOptional(constructed).equals(Optional.of(original));
```

## Practical Examples

### JSON Value Types

```java
// JSON can be different value types
public sealed interface JsonValue {
    record JsonString(String value) implements JsonValue {}
    record JsonNumber(double value) implements JsonValue {}
    record JsonBool(boolean value) implements JsonValue {}
    record JsonArray(List<JsonValue> elements) implements JsonValue {}
    record JsonObject(Map<String, JsonValue> fields) implements JsonValue {}
    record JsonNull() implements JsonValue {}
}

// Prisms for each case
Prism<JsonValue, String> jsonString = Prism.of(
    json -> json instanceof JsonString s ? Optional.of(s.value()) : Optional.empty(),
    JsonString::new
);

Prism<JsonValue, Double> jsonNumber = Prism.of(
    json -> json instanceof JsonNumber n ? Optional.of(n.value()) : Optional.empty(),
    JsonNumber::new
);

// Extract string if present
JsonValue value = parseJson(input);
Optional<String> maybeString = jsonString.getOptional(value);
```

### Event Handling

```java
public sealed interface Event {
    record Click(int x, int y) implements Event {}
    record KeyPress(char key) implements Event {}
    record Scroll(int delta) implements Event {}
}

Prism<Event, Event.Click> clickPrism = Prism.of(
    e -> e instanceof Event.Click c ? Optional.of(c) : Optional.empty(),
    c -> c
);

// Handle only click events
public void handleEvent(Event event) {
    clickPrism.getOptional(event).ifPresent(click -> {
        System.out.println("Clicked at: " + click.x() + ", " + click.y());
    });
}
```

### Entity Types

```java
public sealed interface Entity {
    record Player(String name, int level) implements Entity {}
    record Monster(String type, int health) implements Entity {}
    record Item(String id, int count) implements Entity {}
}

Prism<Entity, Entity.Player> playerPrism = ...;
Prism<Entity, Entity.Monster> monsterPrism = ...;
Prism<Entity, Entity.Item> itemPrism = ...;

// Process a list of entities
public int countPlayers(List<Entity> entities) {
    return (int) entities.stream()
        .filter(e -> playerPrism.getOptional(e).isPresent())
        .count();
}

// Transform players only
public List<Entity> levelUpPlayers(List<Entity> entities) {
    return entities.stream()
        .map(e -> playerPrism.modify(e, p -> new Entity.Player(p.name(), p.level() + 1)))
        .toList();
}
```

## Prism vs Other Optics

| Optic     | Focus             | Extraction   | Construction |
|-----------|-------------------|--------------|--------------|
| **Prism** | 0 or 1 case       | May fail     | Always works |
| Lens      | Exactly 1 field   | Always works | Always works |
| Affine    | 0 or 1            | May fail     | May fail     |
| Iso       | 1 (bidirectional) | Always works | Always works |

### When to Use Prism

✅ Use Prism when:
- Working with sum types (sealed interfaces, enums)
- You need to match and extract one specific case
- You need to construct the sum type from a case

❌ Don't use Prism when:
- Accessing a product type field (use Lens)
- The field might just be missing (use Affine)
- You need bidirectional conversion (use Iso)

## Prism Operations Summary

| Operation          | Description             | Returns       |
|--------------------|-------------------------|---------------|
| `getOptional(S)`   | Try to extract the case | `Optional<A>` |
| `reverseGet(A)`    | Construct from the case | `S`           |
| `modify(S, A → A)` | Transform if matches    | `S`           |
| `compose(Lens)`    | Compose with lens       | `Affine`      |
| `compose(Prism)`   | Compose with prism      | `Prism`       |

---

## Related

- [Lens](lens.md) — For product type fields
- [Affine](affine.md) — For optional access
- [Iso](iso.md) — For bidirectional conversion
- [Optics Overview](index.md) — Optic hierarchy

