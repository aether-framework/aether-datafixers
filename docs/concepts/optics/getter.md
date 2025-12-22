# Getter

A **Getter** is the simplest optic — it provides read-only access to a value. Unlike a Lens, a Getter cannot update the focused value; it can only extract it.

## Definition

```java
public interface Getter<S, A> extends Optic<S, A> {
    /** Get the focused value */
    A get(S source);
}
```

**Type Parameters:**
- `S` — The source type (whole structure)
- `A` — The focus type (the part being read)

## Conceptual Model

A Getter is a one-way accessor:

```
┌─────────────────────────────────────────────┐
│               Source S                       │
│  ┌─────────────────────────────────────┐    │
│  │            Focus A                  │    │
│  └─────────────────────────────────────┘    │
│                                              │
│  get(s) → A    (read-only, no set)           │
└─────────────────────────────────────────────┘
```

## Creating Getters

### From Function

```java
// Simple getter from a method reference
Getter<String, Integer> stringLength = Getter.of(String::length);

int len = stringLength.get("hello");  // 5
```

### From Record Accessor

```java
public record Player(String name, int level) {}

Getter<Player, String> playerName = Getter.of(Player::name);
Getter<Player, Integer> playerLevel = Getter.of(Player::level);

Player player = new Player("Steve", 10);
String name = playerName.get(player);  // "Steve"
int level = playerLevel.get(player);    // 10
```

### Computed Properties

```java
public record Rectangle(double width, double height) {}

// Computed getter (no corresponding field)
Getter<Rectangle, Double> area = Getter.of(r -> r.width() * r.height());
Getter<Rectangle, Double> perimeter = Getter.of(r -> 2 * (r.width() + r.height()));

Rectangle rect = new Rectangle(3.0, 4.0);
double a = area.get(rect);       // 12.0
double p = perimeter.get(rect);  // 14.0
```

## Using Getters

### Basic Get

```java
Getter<Player, String> playerName = Getter.of(Player::name);
Player player = new Player("Steve", 10);

String name = playerName.get(player);  // "Steve"
```

### With Higher-Order Functions

```java
List<Player> players = List.of(
    new Player("Steve", 10),
    new Player("Alex", 15),
    new Player("Bob", 8)
);

// Extract all names
List<String> names = players.stream()
    .map(playerName::get)
    .toList();
// ["Steve", "Alex", "Bob"]
```

### Composing Getters

```java
Getter<Player, String> playerName = Getter.of(Player::name);
Getter<String, Integer> stringLength = Getter.of(String::length);

// Composed getter: Player → name length
Getter<Player, Integer> nameLength = playerName.compose(stringLength);

int len = nameLength.get(new Player("Steve", 10));  // 5
```

## Composition

### Getter with Getter

```java
Getter<Company, CEO> companyCeo = Getter.of(Company::ceo);
Getter<CEO, String> ceoName = Getter.of(CEO::name);

Getter<Company, String> companyCeoName = companyCeo.compose(ceoName);
```

### Getter with Lens

A Getter can be derived from a Lens:

```java
Lens<Player, String> nameLens = Lens.of(Player::name, Player::withName);

// Use lens as a getter
Getter<Player, String> nameGetter = nameLens.asGetter();
// Or just use the lens's get method
String name = nameLens.get(player);
```

### Lens to Getter (Widening)

Any Lens is also a Getter (Lens extends Getter):

```java
Lens<Player, String> nameLens = ...;
Getter<Player, String> nameGetter = nameLens;  // Widening works
```

## Practical Examples

### Derived Properties

```java
public record Order(List<LineItem> items) {}
public record LineItem(String product, int quantity, double price) {}

Getter<LineItem, Double> lineTotal = Getter.of(
    item -> item.quantity() * item.price()
);

Getter<Order, Double> orderTotal = Getter.of(
    order -> order.items().stream()
        .mapToDouble(lineTotal::get)
        .sum()
);

// Usage
Order order = new Order(List.of(
    new LineItem("Widget", 2, 10.0),
    new LineItem("Gadget", 1, 25.0)
));
double total = orderTotal.get(order);  // 45.0
```

### Projection

```java
public record Event(String type, long timestamp, Map<String, Object> data) {}

// Getters for common projections
Getter<Event, String> eventType = Getter.of(Event::type);
Getter<Event, Long> eventTime = Getter.of(Event::timestamp);

// Group events by type
Map<String, List<Event>> byType = events.stream()
    .collect(Collectors.groupingBy(eventType::get));
```

### Formatting

```java
public record Person(String firstName, String lastName, LocalDate birthDate) {}

Getter<Person, String> fullName = Getter.of(
    p -> p.firstName() + " " + p.lastName()
);

Getter<Person, String> formattedBirthDate = Getter.of(
    p -> p.birthDate().format(DateTimeFormatter.ISO_DATE)
);

Getter<Person, Integer> age = Getter.of(
    p -> Period.between(p.birthDate(), LocalDate.now()).getYears()
);
```

## Getter vs Other Optics

| Optic | Read | Write | Focus |
|-------|------|-------|-------|
| **Getter** | ✅ | ❌ | 1 |
| Lens | ✅ | ✅ | 1 |
| Affine | ✅ (optional) | ✅ | 0-1 |
| Prism | ✅ (optional) | construct | 0-1 |

### When to Use Getter

✅ Use Getter when:
- You only need to read a value
- The value is computed/derived
- You want to prevent accidental modification
- Defining a projection or view of data

❌ Don't use Getter when:
- You need to modify the value (use Lens)
- The value might not exist (use Affine's getOptional)

## Getter Laws

A Getter has no laws beyond being a pure function. The same input always produces the same output.

```java
// Pure: get(s) always returns the same value for the same s
Getter<Player, String> name = Getter.of(Player::name);
Player player = new Player("Steve", 10);

assert name.get(player).equals(name.get(player));  // Always true
```

## Relationship to Functions

A Getter is essentially a wrapper around a function `S → A`:

```java
// These are equivalent
Getter<String, Integer> lengthGetter = Getter.of(String::length);
Function<String, Integer> lengthFunction = String::length;

// Getter provides composition with other optics
// Function is just a plain function
```

## Converting Between Optics

### Lens to Getter

```java
Lens<Player, String> nameLens = ...;
Getter<Player, String> nameGetter = nameLens.asGetter();
```

### Getter cannot become Lens

A Getter cannot be "promoted" to a Lens because it lacks update capability.

## Getter Summary

| Operation | Description | Returns |
|-----------|-------------|---------|
| `get(S)` | Extract the focused value | `A` |
| `compose(Getter)` | Compose with another getter | `Getter` |
| `compose(Lens)` | Compose with lens | `Getter` |

---

## Related

- [Lens](lens.md) — Read-write access
- [Affine](affine.md) — Optional read-write
- [Optics Overview](index.md) — Optic hierarchy

