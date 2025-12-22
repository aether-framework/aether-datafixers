# Custom Optics

Creating custom optic implementations for domain-specific accessors.

## When to Create Custom Optics

- Repeated complex access patterns
- Domain-specific transformations
- Type-safe nested access

## Custom Lens Example

```java
public class PositionLens implements Lens<Player, Player, Position, Position> {

    @Override
    public Position get(Player player) {
        return player.position();
    }

    @Override
    public Player set(Position newPosition, Player player) {
        return new Player(player.name(), player.level(), newPosition);
    }

    @Override
    public Player modify(Function<Position, Position> f, Player player) {
        return set(f.apply(get(player)), player);
    }
}

// Usage
PositionLens positionLens = new PositionLens();
Position pos = positionLens.get(player);
Player moved = positionLens.modify(p -> p.withX(p.x() + 10), player);
```

## Composable Lenses

```java
// Compose lenses for deep access
Lens<Game, Game, Player, Player> playerLens = ...;
Lens<Player, Player, Position, Position> positionLens = ...;
Lens<Position, Position, Double, Double> xLens = ...;

Lens<Game, Game, Double, Double> gamePlayerX = playerLens
    .compose(positionLens)
    .compose(xLens);

double x = gamePlayerX.get(game);
Game moved = gamePlayerX.modify(v -> v + 10, game);
```

## Custom Prism Example

```java
public class CirclePrism implements Prism<Shape, Shape, Circle, Circle> {

    @Override
    public Option<Circle> getOption(Shape shape) {
        return shape instanceof Circle c ? Option.some(c) : Option.none();
    }

    @Override
    public Shape reverseGet(Circle circle) {
        return circle;
    }
}
```

## Dynamic Finder

```java
public class JsonPathFinder implements Finder<JsonElement> {

    private final String[] path;

    public JsonPathFinder(String... path) {
        this.path = path;
    }

    @Override
    public OptionalDynamic<JsonElement> find(Dynamic<JsonElement> source) {
        Dynamic<JsonElement> current = source;
        for (String segment : path) {
            current = current.get(segment).orElseEmptyMap();
        }
        return current.asOptional();
    }
}
```

## Related

- [Optics Overview](../concepts/optics/index.md)
- [Lens](../concepts/optics/lens.md)
- [Prism](../concepts/optics/prism.md)

