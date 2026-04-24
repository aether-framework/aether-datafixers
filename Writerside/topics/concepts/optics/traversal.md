# Traversal

<show-structure for="chapter,procedure" depth="2"/>

<tldr>
    <p>A <code>Traversal&lt;S, T, A, B&gt;</code> focuses on
       <b>zero-to-many</b> values inside a source: every element of a list, every
       value of a map, every node in a tree. One operation visits them all.</p>
</tldr>

## Shape

```java
public interface Traversal<S, T, A, B> {
    String     id();
    Stream<A>  getAll(S source);
    T          modify(S source, Function<A, B> modifier);
    default T  set(S source, B value);
    default List<A> toList(S source);
}
```

Traversal is the "plural" sibling of Lens &mdash; the focus count goes from
*exactly one* to *any number*, including zero.

## Creating a Traversal

<tabs>
<tab title="List elements">

```java
Traversal<List<String>, List<String>, String, String> listElems =
        Traversal.of(
                "list.elements",
                List::stream,
                (list, fn) -> list.stream().map(fn).toList()
        );

List<String> names = List.of("alice", "bob", "charlie");
List<String> upper = listElems.modify(names, String::toUpperCase);
// ["ALICE", "BOB", "CHARLIE"]
```

</tab>
<tab title="Map values">

```java
Traversal<Map<String, Integer>, Map<String, Integer>, Integer, Integer> values =
        Traversal.of(
                "map.values",
                m -> m.values().stream(),
                (m, fn) -> m.entrySet().stream().collect(
                        java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> fn.apply(e.getValue())
                        )
                )
        );

Map<String, Integer> scores = Map.of("alice", 85, "bob", 92);
Map<String, Integer> bumped = values.modify(scores, n -> n + 10);
```

</tab>
<tab title="From a Lens">

```java
Lens<Company, Company, Team, Team> teamLens = Lens.of(/* … */);

// A Lens is a single-element Traversal.
Traversal<Company, Company, Team, Team> teamAsTraversal =
        Traversal.fromLens(teamLens);
```

</tab>
</tabs>

## Reading All Foci

```java
List<String> names = List.of("alice", "bob");

listElems.getAll(names).toList();  // [alice, bob]
listElems.toList(names);           // same, shorter
```

`getAll` returns a `Stream<A>`, so you compose it with the rest of your
functional pipeline:

```java
int totalLength = listElems.getAll(names).mapToInt(String::length).sum();
```

## Writing To All Foci

```java
// Uniform update: everyone becomes "X"
List<String> replaced = listElems.set(names, "X");
// ["X", "X"]

// Per-element modification
List<String> bumped = listElems.modify(names, s -> s + "!");
// ["alice!", "bob!"]
```

<warning>
    <p>
        <code>set</code> replaces every focus with <b>the same value</b>. If you
        need per-element values (e.g. zip two collections together), that's no
        longer a Traversal &mdash; reach for a plain stream pipeline or a
        different abstraction.
    </p>
</warning>

## Composition

Composing two `Traversal`s walks the Cartesian product: for each focus of the
outer, enumerate the foci of the inner.

```java
// Company → Stream<Employee>
Traversal<Company, Company, Employee, Employee> employees = /* … */;

// Employee → Stream<String>  (skills)
Traversal<Employee, Employee, String, String> skills = /* … */;

// Company → Stream<String>   (every skill across every employee)
Traversal<Company, Company, String, String> allSkills = employees.compose(skills);
```

A `Traversal` composed with a `Lens` stays a `Traversal`. A `Traversal`
composed with a `Prism` or an `Affine` likewise stays a `Traversal` &mdash; the
weaker/looser optic wins.

## Typical Uses

<deflist type="full">
    <def title="Uniform updates">
        Add a suffix to every name, double every score, normalize every path.
    </def>
    <def title="Data summaries">
        <code>toList</code> / <code>getAll</code> exposes every focus as a stream
        for folding, counting, averaging.
    </def>
    <def title="Nested walks">
        Compose two traversals to walk a list-of-lists or a tree level-by-level.
    </def>
    <def title="DataFix scaffolding">
        When a fix needs to apply the same rule to every element of an inventory,
        a Traversal is a clean way to describe "everywhere this type appears".
    </def>
</deflist>

## Traversal vs. Lens vs. Stream

<compare type="top-bottom" first-title="Lens (one focus)" second-title="Traversal (many foci)">

```java
Lens<Order, Order, Customer, Customer> customer = Lens.of(...);
Customer c = customer.get(order);               // one
Order updated = customer.modify(order, fn);
```

```java
Traversal<Order, Order, LineItem, LineItem> items = Traversal.of(...);
List<LineItem> all = items.toList(order);        // many
Order updated      = items.modify(order, fn);    // all at once
```

</compare>

Where a `Stream` is a one-shot pipeline, a `Traversal` is a reusable,
composable, named accessor &mdash; and it round-trips the structure so the
result is still the original type.

<seealso style="cards">
    <category ref="wrs">
        <a href="lens.md"        summary="Exactly one focus."/>
        <a href="affine.md"      summary="Zero or one focus; Traversal's more precise sibling."/>
        <a href="finder.md"      summary="Path-aware navigation inside Dynamic values."/>
        <a href="optics-overview.topic" summary="All optic types."/>
    </category>
</seealso>
