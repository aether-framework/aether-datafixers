# Iso (Isomorphism)

An **Iso** is an optic that represents a bidirectional, lossless conversion between two types. If you have an Iso between `A` and `B`, you can convert any `A` to a `B` and any `B` back to an `A` without losing information.

## Definition

```java
public interface Iso<S, A> extends Optic<S, A> {
    /** Convert from S to A */
    A get(S source);

    /** Convert from A to S */
    S reverseGet(A value);

    /** Reverse the iso direction */
    default Iso<A, S> reverse() {
        return Iso.of(this::reverseGet, this::get);
    }
}
```

**Type Parameters:**
- `S` — The source type
- `A` — The target type

## Conceptual Model

An Iso represents an equivalence between types:

```
┌─────────────┐     get()      ┌─────────────┐
│     S       │ ──────────────▶│     A       │
│   (source)  │                │   (target)  │
│             │◀──────────────│              │
└─────────────┘   reverseGet() └─────────────┘
```

The key property: `reverseGet(get(s)) == s` and `get(reverseGet(a)) == a`

## Creating Isos

### Basic Iso

```java
// Iso between Celsius and Fahrenheit
Iso<Double, Double> celsiusToFahrenheit = Iso.of(
    celsius -> celsius * 9.0 / 5.0 + 32,  // get: C → F
    fahrenheit -> (fahrenheit - 32) * 5.0 / 9.0  // reverseGet: F → C
);

double fahrenheit = celsiusToFahrenheit.get(100.0);  // 212.0
double celsius = celsiusToFahrenheit.reverseGet(212.0);  // 100.0
```

### Type Conversion

```java
// Iso between String and List<Character>
Iso<String, List<Character>> stringChars = Iso.of(
    s -> s.chars().mapToObj(c -> (char) c).toList(),
    chars -> chars.stream().map(String::valueOf).collect(Collectors.joining())
);

List<Character> chars = stringChars.get("hello");  // ['h', 'e', 'l', 'l', 'o']
String str = stringChars.reverseGet(List.of('h', 'i'));  // "hi"
```

### Wrapper Types

```java
// Iso between a wrapper and its wrapped value
public record UserId(long value) {}

Iso<UserId, Long> userIdValue = Iso.of(
    UserId::value,
    UserId::new
);

Long id = userIdValue.get(new UserId(42));  // 42L
UserId userId = userIdValue.reverseGet(42L);  // UserId(42)
```

## Using Isos

### get and reverseGet

Convert between types:

```java
Iso<String, Integer> stringInt = Iso.of(
    Integer::parseInt,
    String::valueOf
);

int num = stringInt.get("42");  // 42
String str = stringInt.reverseGet(42);  // "42"
```

### reverse

Flip the direction:

```java
Iso<Double, Double> celsiusToFahrenheit = ...;
Iso<Double, Double> fahrenheitToCelsius = celsiusToFahrenheit.reverse();

double celsius = fahrenheitToCelsius.get(212.0);  // 100.0
double fahrenheit = fahrenheitToCelsius.reverseGet(100.0);  // 212.0
```

### modify

Transform via the isomorphism:

```java
Iso<String, List<Character>> stringChars = ...;

// Reverse the characters in a string
String reversed = stringChars.modify("hello", chars -> {
    List<Character> rev = new ArrayList<>(chars);
    Collections.reverse(rev);
    return rev;
});
// "olleh"
```

## Composition

### Iso with Iso

Composing two Isos gives another Iso:

```java
Iso<String, List<Character>> stringChars = ...;
Iso<List<Character>, Character[]> listArray = Iso.of(
    list -> list.toArray(new Character[0]),
    Arrays::asList
);

// Composed: String ↔ Character[]
Iso<String, Character[]> stringArray = stringChars.compose(listArray);
```

### Iso with Lens

An Iso can be composed with a Lens:

```java
// Wrapper type
public record Config(Settings settings) {}
public record Settings(int timeout) {}

Iso<Config, Settings> configSettings = Iso.of(
    Config::settings,
    Config::new
);

Lens<Settings, Integer> settingsTimeout = Lens.of(
    Settings::timeout,
    (s, t) -> new Settings(t)
);

// Composed: Config → Integer
Lens<Config, Integer> configTimeout = configSettings.compose(settingsTimeout);
```

## Iso Laws

A proper Iso must satisfy these laws:

### 1. Roundtrip (get → reverseGet)

```java
// reverseGet(get(s)) == s
String original = "hello";
assert stringChars.reverseGet(stringChars.get(original)).equals(original);
```

### 2. Roundtrip (reverseGet → get)

```java
// get(reverseGet(a)) == a
List<Character> original = List.of('h', 'i');
assert stringChars.get(stringChars.reverseGet(original)).equals(original);
```

## Practical Examples

### Unit Conversions

```java
// Length units
Iso<Double, Double> metersToFeet = Iso.of(
    meters -> meters * 3.28084,
    feet -> feet / 3.28084
);

Iso<Double, Double> metersToInches = metersToFeet.compose(Iso.of(
    feet -> feet * 12,
    inches -> inches / 12
));

double inches = metersToInches.get(1.0);  // 39.37...
```

### Coordinate Systems

```java
public record Cartesian(double x, double y) {}
public record Polar(double r, double theta) {}

Iso<Cartesian, Polar> cartesianToPolar = Iso.of(
    c -> new Polar(
        Math.sqrt(c.x() * c.x() + c.y() * c.y()),
        Math.atan2(c.y(), c.x())
    ),
    p -> new Cartesian(
        p.r() * Math.cos(p.theta()),
        p.r() * Math.sin(p.theta())
    )
);
```

### Encoding/Decoding

```java
// Base64 encoding
Iso<byte[], String> base64 = Iso.of(
    bytes -> Base64.getEncoder().encodeToString(bytes),
    str -> Base64.getDecoder().decode(str)
);

String encoded = base64.get("hello".getBytes());
byte[] decoded = base64.reverseGet(encoded);
```

### Newtype Pattern

```java
// Type-safe wrappers
public record Email(String value) {}
public record UserId(long value) {}
public record OrderId(UUID value) {}

Iso<Email, String> emailIso = Iso.of(Email::value, Email::new);
Iso<UserId, Long> userIdIso = Iso.of(UserId::value, UserId::new);
Iso<OrderId, UUID> orderIdIso = Iso.of(OrderId::value, OrderId::new);

// Extract raw value
String raw = emailIso.get(new Email("user@example.com"));

// Wrap raw value
Email email = emailIso.reverseGet("user@example.com");
```

### JSON String Parsing

```java
public record Person(String name, int age) {}

// Assumes well-formed JSON
Iso<String, Person> personJson = Iso.of(
    json -> gson.fromJson(json, Person.class),
    person -> gson.toJson(person)
);

Person person = personJson.get("{\"name\":\"Steve\",\"age\":30}");
String json = personJson.reverseGet(new Person("Steve", 30));
```

## Iso vs Other Optics

| Optic   | Forward      | Backward     | Always Succeeds   |
|---------|--------------|--------------|-------------------|
| **Iso** | ✅            | ✅            | ✅                 |
| Lens    | ✅            | ✅            | ✅ (single focus)  |
| Prism   | ✅ (optional) | ✅            | ❌ (may not match) |
| Affine  | ✅ (optional) | ✅ (optional) | ❌                 |

### When to Use Iso

✅ Use Iso when:
- Two types represent the same information
- Conversion is lossless in both directions
- You need to transform data through different representations

❌ Don't use Iso when:
- Conversion might fail (use Prism)
- Types aren't truly equivalent (use other optics)
- Only one direction is needed (use plain function)

## Identity Iso

The identity iso converts a type to itself:

```java
Iso<String, String> identity = Iso.of(
    s -> s,
    s -> s
);

// Useful as a neutral element in composition
```

## Iso Summary

| Operation          | Description        | Returns     |
|--------------------|--------------------|-------------|
| `get(S)`           | Convert S to A     | `A`         |
| `reverseGet(A)`    | Convert A to S     | `S`         |
| `reverse()`        | Flip the direction | `Iso<A, S>` |
| `modify(S, A → A)` | Transform via iso  | `S`         |
| `compose(Iso)`     | Compose with iso   | `Iso`       |
| `compose(Lens)`    | Compose with lens  | `Lens`      |

---

## Related

- [Lens](lens.md) — For field access
- [Prism](prism.md) — For partial conversion
- [Optics Overview](index.md) — Optic hierarchy

