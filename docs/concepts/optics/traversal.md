# Traversal

A **Traversal** is an optic that focuses on zero or more values within a structure. It generalizes both Lens (one focus) and Affine (zero or one focus) to handle collections and multiple focal points.

## Definition

```java
public interface Traversal<S, A> extends Optic<S, A> {
    /** Get all focused values */
    List<A> getAll(S source);

    /** Modify all focused values */
    S modify(S source, Function<A, A> fn);

    /** Set all focused values to the same value */
    default S set(S source, A value) {
        return modify(source, _ -> value);
    }
}
```

**Type Parameters:**
- `S` — The source type (whole structure)
- `A` — The focus type (each element)

## Conceptual Model

A Traversal can focus on any number of elements:

```
┌────────────────────────────────────────────────────────────────┐
│                         Source S                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ Focus A₁ │  │ Focus A₂ │  │ Focus A₃ │  │ Focus A₄ │  ...   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│                                                                │
│  getAll(s) → [A₁, A₂, A₃, A₄, ...]                             │
│  modify(s, f) → apply f to each Aᵢ                             │
└────────────────────────────────────────────────────────────────┘
```

## Creating Traversals

### For Lists

```java
// Traversal for list elements
public static <A> Traversal<List<A>, A> listTraversal() {
    return Traversal.of(
        list -> list,  // getAll: return the list itself
        (list, fn) -> list.stream().map(fn).toList()  // modify each element
    );
}

// Usage
Traversal<List<String>, String> stringList = listTraversal();
List<String> names = List.of("steve", "alex", "bob");

List<String> all = stringList.getAll(names);  // ["steve", "alex", "bob"]
List<String> upper = stringList.modify(names, String::toUpperCase);
// ["STEVE", "ALEX", "BOB"]
```

### For Record Collections

```java
public record Team(String name, List<Player> players) {}
public record Player(String name, int level) {}

Traversal<Team, Player> teamPlayers = Traversal.of(
    Team::players,
    (team, fn) -> new Team(team.name(), team.players().stream().map(fn).toList())
);

Lens<Player, Integer> playerLevel = Lens.of(
    Player::level,
    (p, l) -> new Player(p.name(), l)
);

// Traversal for all player levels in a team
Traversal<Team, Integer> teamLevels = teamPlayers.compose(playerLevel);
```

### For Optional

```java
// Traversal that focuses on 0 or 1 value
public static <A> Traversal<Optional<A>, A> optionalTraversal() {
    return Traversal.of(
        opt -> opt.map(List::of).orElse(List.of()),
        (opt, fn) -> opt.map(fn)
    );
}
```

## Using Traversals

### getAll

Get all focused values:

```java
Team team = new Team("Heroes", List.of(
    new Player("Steve", 10),
    new Player("Alex", 15),
    new Player("Bob", 8)
));

List<Integer> levels = teamLevels.getAll(team);  // [10, 15, 8]
List<Player> players = teamPlayers.getAll(team);  // all 3 players
```

### modify

Transform all focused values:

```java
// Level up all players
Team leveledUp = teamPlayers.modify(team, player ->
    new Player(player.name(), player.level() + 1)
);

// Or using composed traversal
Team leveledUp = teamLevels.modify(team, level -> level + 1);
```

### set

Set all focused values to the same value:

```java
// Reset all levels to 1
Team reset = teamLevels.set(team, 1);
// All players now have level 1
```

### fold

Reduce all focused values:

```java
// Sum of all levels
int totalLevels = teamLevels.getAll(team).stream()
    .mapToInt(Integer::intValue)
    .sum();
// 33

// Average level
double avgLevel = teamLevels.getAll(team).stream()
    .mapToInt(Integer::intValue)
    .average()
    .orElse(0.0);
// 11.0
```

## Composition

### Traversal with Traversal

Composing traversals multiplies the focus:

```java
public record League(List<Team> teams) {}

Traversal<League, Team> leagueTeams = Traversal.of(
    League::teams,
    (league, fn) -> new League(league.teams().stream().map(fn).toList())
);

// League → Team → Player
Traversal<League, Player> leaguePlayers = leagueTeams.compose(teamPlayers);

// League → Team → Player → level
Traversal<League, Integer> leagueLevels = leaguePlayers.compose(playerLevel);
```

### Traversal with Lens

```java
Traversal<Team, Player> teamPlayers = ...;
Lens<Player, String> playerName = ...;

// Traversal for all player names
Traversal<Team, String> teamPlayerNames = teamPlayers.compose(playerName);
```

### Traversal with Prism

```java
Traversal<List<Shape>, Shape> shapes = listTraversal();
Prism<Shape, Circle> circlePrism = ...;

// Traversal for all circles in the list
Traversal<List<Shape>, Circle> circles = shapes.compose(circlePrism);
```

## Practical Examples

### JSON Array Processing

```java
public record JsonArray(List<JsonValue> elements) {}

Traversal<JsonArray, JsonValue> arrayElements = Traversal.of(
    JsonArray::elements,
    (arr, fn) -> new JsonArray(arr.elements().stream().map(fn).toList())
);

// Transform all string values to uppercase
Prism<JsonValue, String> jsonString = ...;
Traversal<JsonArray, String> arrayStrings = arrayElements.compose(jsonString);

JsonArray result = arrayStrings.modify(inputArray, String::toUpperCase);
```

### Nested Collections

```java
public record Department(List<Employee> employees) {}
public record Employee(String name, List<Skill> skills) {}
public record Skill(String name, int proficiency) {}

Traversal<Department, Employee> deptEmployees = ...;
Traversal<Employee, Skill> employeeSkills = ...;
Lens<Skill, Integer> skillProficiency = ...;

// All skill proficiencies in a department
Traversal<Department, Integer> allProficiencies = deptEmployees
    .compose(employeeSkills)
    .compose(skillProficiency);

// Increase all proficiencies by 10%
Department improved = allProficiencies.modify(dept, p -> (int)(p * 1.1));
```

### Filtered Traversal

```java
// Traversal that only focuses on matching elements
public static <A> Traversal<List<A>, A> filtered(Predicate<A> predicate) {
    return Traversal.of(
        list -> list.stream().filter(predicate).toList(),
        (list, fn) -> list.stream()
            .map(a -> predicate.test(a) ? fn.apply(a) : a)
            .toList()
    );
}

// Only players with level > 10
Traversal<List<Player>, Player> highLevel = filtered(p -> p.level() > 10);

// Modify only high-level players
List<Player> modified = highLevel.modify(players, p ->
    new Player(p.name() + " [Elite]", p.level())
);
```

### Index-Based Traversal

```java
// Traversal for specific index
public static <A> Traversal<List<A>, A> index(int i) {
    return Traversal.of(
        list -> i < list.size() ? List.of(list.get(i)) : List.of(),
        (list, fn) -> {
            if (i >= list.size()) return list;
            List<A> result = new ArrayList<>(list);
            result.set(i, fn.apply(list.get(i)));
            return List.copyOf(result);
        }
    );
}

// Access first element
Traversal<List<String>, String> first = index(0);
```

## Traversal vs Other Optics

| Optic         | Focus Count | Use Case                   |
|---------------|-------------|----------------------------|
| Lens          | Exactly 1   | Single required field      |
| Prism         | 0 or 1      | Sum type case              |
| Affine        | 0 or 1      | Optional field             |
| **Traversal** | 0 to N      | Collections, multiple foci |

### When to Use Traversal

✅ Use Traversal when:
- Working with collections (List, Set, Array)
- Need to modify all elements uniformly
- Multiple parts of a structure should be accessed together
- Composing access to nested collections

❌ Don't use Traversal when:
- Only one value is accessed (use Lens)
- Accessing optional value (use Affine)
- Matching sum type case (use Prism)

## Traversal Laws

### 1. Identity Modification

Modifying with identity doesn't change the source:

```java
// modify(s, a -> a) == s
Team team = ...;
assert teamPlayers.modify(team, p -> p).equals(team);
```

### 2. Composition of Modifications

Two modifications compose:

```java
// modify(modify(s, f), g) == modify(s, g ∘ f)
Team result1 = teamLevels.modify(teamLevels.modify(team, x -> x + 1), x -> x * 2);
Team result2 = teamLevels.modify(team, x -> (x + 1) * 2);
assert result1.equals(result2);
```

## Traversal Summary

| Operation            | Description            | Returns     |
|----------------------|------------------------|-------------|
| `getAll(S)`          | Get all focused values | `List<A>`   |
| `modify(S, A → A)`   | Transform all values   | `S`         |
| `set(S, A)`          | Set all to same value  | `S`         |
| `compose(Traversal)` | Compose traversals     | `Traversal` |
| `compose(Lens)`      | Compose with lens      | `Traversal` |
| `compose(Prism)`     | Compose with prism     | `Traversal` |

---

## Related

- [Lens](lens.md) — Single-focus access
- [Affine](affine.md) — Optional access
- [Finder](finder.md) — Dynamic data navigation
- [Optics Overview](index.md) — Optic hierarchy

