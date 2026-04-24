# Getter

<show-structure for="chapter,procedure" depth="2"/>

<tldr>
    <p>A <code>Getter&lt;S, A&gt;</code> is the simplest optic: a pure read-only
       extraction from <code>S</code> to <code>A</code>, carrying an identifier so
       it can be named in logs and composed with other optics.</p>
</tldr>

## Shape

```java
public interface Getter<S, A> {
    String id();
    A      get(S source);
}
```

<note>
    <p>Unlike the four-parameter profunctor optics in the rest of the hierarchy,
       <code>Getter</code> has two type parameters &mdash; it cannot modify, so the
       "modified" source and focus types never come up.</p>
</note>

## When to Reach For a Getter

<deflist type="full">
    <def title="Expose derived or computed values">
        A <code>Getter</code> is ideal for values that are calculated rather than
        stored &mdash; area, hash, percentage, a formatted name.
    </def>
    <def title="Encapsulate read access">
        If the consumer must not update the value, hand them a
        <code>Getter</code>. A <code>Lens</code> would leak the setter.
    </def>
    <def title="Downgrade a Lens for a read-only API">
        Any <a href="lens.md">Lens</a> projects down to a <code>Getter</code> via
        <code>Getter.fromLens(lens)</code>.
    </def>
</deflist>

## Creating a Getter

<tabs>
<tab title="From a function">

```java
Getter<String, Integer> length = Getter.of("string.length", String::length);

int n = length.get("hello"); // 5
```

</tab>
<tab title="From a record accessor">

```java
public record Player(String name, int level) {}

Getter<Player, String>  name  = Getter.of("player.name",  Player::name);
Getter<Player, Integer> level = Getter.of("player.level", Player::level);

Player p = new Player("Steve", 10);
name.get(p);  // "Steve"
level.get(p); // 10
```

</tab>
<tab title="From a Lens">

```java
Lens<Player, Player, String, String> nameLens = Lens.of(
        "player.name",
        Player::name,
        (pl, n) -> new Player(n, pl.level())
);

Getter<Player, String> nameGetter = Getter.fromLens(nameLens);
// Read-only projection of the lens
```

</tab>
<tab title="Computed property">

```java
public record Rectangle(double width, double height) {}

Getter<Rectangle, Double> area =
        Getter.of("rectangle.area", r -> r.width() * r.height());

Getter<Rectangle, Double> perimeter =
        Getter.of("rectangle.perimeter", r -> 2 * (r.width() + r.height()));

Rectangle r = new Rectangle(3.0, 4.0);
area.get(r);      // 12.0
perimeter.get(r); // 14.0
```

</tab>
</tabs>

## The `id` Field

Every optic carries a stable identifier. For a Getter that means:

- The value shows up in diagnostic logs &mdash; a composed optic's id is built
  from its parts' ids (`"player.name"`, `"company.ceo.name"`).
- It makes two otherwise-identical anonymous optics distinguishable in
  migration reports.

Conventionally the id mirrors the access path: `"record.field"`,
`"collection.element"`, `"rectangle.area"`.

## Composing Getters

`getterA.compose(getterB)` reads as "first get A, then get B from A". The
produced Getter's id is the two ids joined by `.`.

```java
Getter<Company, CEO>    ceo      = Getter.of("company.ceo",  Company::ceo);
Getter<CEO,     String> ceoName  = Getter.of("ceo.name",     CEO::name);

Getter<Company, String> companyCeoName = ceo.compose(ceoName);
// id = "company.ceo.name"

String n = companyCeoName.get(acme);
```

## Using Getters With Higher-Order Code

```java
List<Player> players = ...;

Getter<Player, String> name = Getter.of("player.name", Player::name);

List<String> names = players.stream()
        .map(name::get)
        .toList();
```

Because a Getter is essentially a named `Function<S, A>`, it slots cleanly into
`Stream` pipelines, `Optional.map`, or any functional API that takes a function.

## Getter vs. Raw Method Reference

<compare type="top-bottom" first-title="Plain method reference" second-title="Getter">

```java
Function<Player, String> nameFn = Player::name;
nameFn.apply(player);
```

```java
Getter<Player, String> name = Getter.of("player.name", Player::name);
name.get(player);
name.id();           // "player.name"
name.compose(other); // composable with other optics
```

</compare>

The Getter adds an identity and a composition protocol. For a one-off read
inside a private method, a plain method reference is fine; for anything that
shows up in the optic graph of a fix, use a Getter.

## Rules of Thumb

<deflist type="full">
    <def title="One Getter per computed value">
        Declare them as <code>public static final</code> constants, named after
        the value they expose.
    </def>
    <def title="Use a descriptive id">
        Future-you looking at a migration report wants to see
        <code>player.experience</code>, not <code>getter-7</code>.
    </def>
    <def title="Stay stateless">
        Getters are pure functions &mdash; no caches, no side effects.
    </def>
</deflist>

<seealso style="cards">
    <category ref="wrs">
        <a href="lens.md"      summary="Read + write. Every Lens is also a Getter."/>
        <a href="iso.md"       summary="Bidirectional, lossless conversion."/>
        <a href="finder.md"    summary="Runtime navigation of Dynamic values."/>
        <a href="optics-overview.topic" summary="All optic types."/>
    </category>
</seealso>
