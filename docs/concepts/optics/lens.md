# Lens

A **Lens** is an optic that focuses on exactly one part of a data structure. It provides both a getter and a setter, allowing you to read and update a specific field.

## Definition

```java
public interface Lens<S, A> extends Optic<S, A> {
    /** Get the focused value */
    A get(S source);

    /** Set the focused value, returning a new source */
    S set(S source, A value);

    /** Modify the focused value with a function */
    default S modify(S source, Function<A, A> fn) {
        return set(source, fn.apply(get(source)));
    }
}
```

**Type Parameters:**
- `S` — The source type (whole structure)
- `A` — The focus type (the part you're accessing)

## Creating Lenses

### From Getter and Setter

```java
public record Player(String name, int level) {
    public Player withName(String name) {
        return new Player(name, this.level);
    }
    public Player withLevel(int level) {
        return new Player(this.name, level);
    }
}

// Create a lens for the "name" field
Lens<Player, String> playerName = Lens.of(
    Player::name,                     // getter
    (player, name) -> player.withName(name)  // setter
);

// Create a lens for the "level" field
Lens<Player, Integer> playerLevel = Lens.of(
    Player::level,
    Player::withLevel
);
```

### Using Factory Methods

```java
// Lens for a specific field type
Lens<Player, String> name = Lens.field("name", Player::name, Player::withName);
```

## Using Lenses

### Get

Extract the focused value:

```java
Player player = new Player("Steve", 10);

String name = playerName.get(player);   // "Steve"
int level = playerLevel.get(player);    // 10
```

### Set

Create a new structure with an updated value:

```java
Player player = new Player("Steve", 10);

Player renamed = playerName.set(player, "Alex");
// Player("Alex", 10)

Player leveled = playerLevel.set(player, 20);
// Player("Steve", 20)
```

### Modify

Transform the focused value with a function:

```java
Player player = new Player("steve", 10);

// Capitalize the name
Player capitalized = playerName.modify(player, String::toUpperCase);
// Player("STEVE", 10)

// Double the level
Player doubled = playerLevel.modify(player, level -> level * 2);
// Player("steve", 20)
```

## Composition

Lenses compose with other lenses to access nested fields:

```java
// Domain model
public record Address(String street, String city) {}
public record Person(String name, Address address) {}

// Individual lenses
Lens<Person, Address> personAddress = Lens.of(
    Person::address,
    (p, a) -> new Person(p.name(), a)
);

Lens<Address, String> addressCity = Lens.of(
    Address::city,
    (a, c) -> new Address(a.street(), c)
);

// Compose into a single lens
Lens<Person, String> personCity = personAddress.compose(addressCity);

// Use the composed lens
Person person = new Person("Steve", new Address("123 Main St", "NYC"));

String city = personCity.get(person);  // "NYC"
Person moved = personCity.set(person, "LA");
// Person("Steve", Address("123 Main St", "LA"))
```

### Deep Nesting

```java
// Company → Department → Manager → Name
Lens<Company, String> managerName = companyDept
    .compose(deptManager)
    .compose(managerName);

String name = managerName.get(company);
Company updated = managerName.set(company, "New Manager");
```

## Lens Laws

A proper Lens must satisfy these laws:

### 1. Get-Set Law

Setting what you get returns the original:

```java
// lens.set(s, lens.get(s)) == s
Player player = new Player("Steve", 10);
assert playerName.set(player, playerName.get(player)).equals(player);
```

### 2. Set-Get Law

Getting what you set returns that value:

```java
// lens.get(lens.set(s, a)) == a
Player player = new Player("Steve", 10);
String newName = "Alex";
assert playerName.get(playerName.set(player, newName)).equals(newName);
```

### 3. Set-Set Law

Setting twice is the same as setting once:

```java
// lens.set(lens.set(s, a), b) == lens.set(s, b)
Player player = new Player("Steve", 10);
Player setTwice = playerName.set(playerName.set(player, "Alex"), "Bob");
Player setOnce = playerName.set(player, "Bob");
assert setTwice.equals(setOnce);
```

## Practical Examples

### Configuration Access

```java
public record Config(
    int maxPlayers,
    String serverName,
    NetworkSettings network
) {}

public record NetworkSettings(
    String host,
    int port,
    boolean ssl
) {}

// Lenses for direct fields
Lens<Config, Integer> maxPlayers = Lens.of(
    Config::maxPlayers,
    (c, m) -> new Config(m, c.serverName(), c.network())
);

Lens<Config, NetworkSettings> network = Lens.of(
    Config::network,
    (c, n) -> new Config(c.maxPlayers(), c.serverName(), n)
);

Lens<NetworkSettings, Integer> port = Lens.of(
    NetworkSettings::port,
    (n, p) -> new NetworkSettings(n.host(), p, n.ssl())
);

// Composed lens for nested access
Lens<Config, Integer> configPort = network.compose(port);

// Usage
Config config = loadConfig();
int currentPort = configPort.get(config);
Config updated = configPort.set(config, 8443);
```

### Immutable Updates

```java
public record GameState(
    Player player,
    World world,
    long timestamp
) {}

Lens<GameState, Player> statePlayer = Lens.of(
    GameState::player,
    (s, p) -> new GameState(p, s.world(), s.timestamp())
);

Lens<GameState, Integer> statePlayerLevel = statePlayer.compose(playerLevel);

// Level up the player without mutating anything
GameState state = getCurrentState();
GameState newState = statePlayerLevel.modify(state, level -> level + 1);
```

### Working with Lists

Lens can work with list elements using indices:

```java
public record Inventory(List<Item> items) {}

// Lens for a specific index (unsafe - may throw)
Lens<Inventory, Item> firstItem = Lens.of(
    inv -> inv.items().get(0),
    (inv, item) -> {
        List<Item> newItems = new ArrayList<>(inv.items());
        newItems.set(0, item);
        return new Inventory(List.copyOf(newItems));
    }
);

// For safe access to optional elements, use Affine instead
```

## Lens vs Other Optics

| Optic | Focus | When to Use |
|-------|-------|-------------|
| **Lens** | Exactly 1 | Required fields |
| Affine | 0 or 1 | Optional fields |
| Prism | 0 or 1 | Sum type cases |
| Traversal | 0 to N | Collections |

### When to Use Lens

✅ Use Lens when:
- The field always exists
- You need both read and write access
- The focus is exactly one value

❌ Don't use Lens when:
- The field might be missing (use Affine)
- You're matching a sum type case (use Prism)
- You need to access multiple values (use Traversal)

## Common Patterns

### Builder-Style Updates

```java
// Chain multiple lens updates
GameState newState = state
    |> statePlayerLevel.modify(level -> level + 1)
    |> statePlayerName.set("Hero")
    |> stateWorld.modify(World::advance);

// Without pipe operator, use helper method
GameState newState = pipe(state,
    s -> statePlayerLevel.modify(s, level -> level + 1),
    s -> statePlayerName.set(s, "Hero"),
    s -> stateWorld.modify(s, World::advance)
);
```

### Conditional Modification

```java
// Only modify if condition is met
public <S, A> S modifyIf(Lens<S, A> lens, S source, Predicate<A> condition, Function<A, A> fn) {
    A current = lens.get(source);
    if (condition.test(current)) {
        return lens.set(source, fn.apply(current));
    }
    return source;
}

// Usage
Player player = modifyIf(playerLevel, player, level -> level < 100, level -> level + 1);
```

---

## Summary

| Method | Description |
|--------|-------------|
| `get(S)` | Extract the focused value |
| `set(S, A)` | Return new structure with updated value |
| `modify(S, A → A)` | Transform the focused value |
| `compose(Lens<A, B>)` | Compose with another lens |

---

## Related

- [Affine](affine.md) — For optional fields
- [Prism](prism.md) — For sum type cases
- [Iso](iso.md) — For bidirectional conversion
- [Optics Overview](index.md) — Optic hierarchy

