# Prism

<show-structure for="chapter,procedure" depth="2"/>

<tldr>
    <p>A <code>Prism&lt;S, T, A, B&gt;</code> focuses on <b>zero or one</b> variant
       of a sum type. Think of it as "maybe-here" &mdash; it matches a specific
       case of <code>S</code> and extracts the payload, or fails to match.</p>
</tldr>

## Shape

```java
public interface Prism<S, T, A, B> {
    String           id();
    Optional<A>      getOption(S source);   // extract if case matches
    T                reverseGet(B value);    // build an S from the payload
    default T        modify(S, Function<A, B>);
    default T        set(S, B value);
}
```

A Prism is the dual of a <a href="lens.md">Lens</a>:

| Optic      | Focus direction                                             |
|------------|-------------------------------------------------------------|
| **Lens**   | Always-present part &rarr; get + set.                       |
| **Prism**  | Maybe-present variant &rarr; <code>getOption</code> + build.|

## The Prism Laws

<deflist type="full">
    <def title="Preview-Review: round-trip a payload without loss">
        <code>prism.getOption(prism.reverseGet(a)) == Optional.of(a)</code>
    </def>
    <def title="Review-Preview: preserve a matching source">
        If <code>prism.getOption(s) == Optional.of(a)</code>, then
        <code>prism.reverseGet(a) == s</code>.
    </def>
</deflist>

## Creating a Prism

<tabs>
<tab title="Variant of Either">

```java
// A prism for the "Left" case of Either<String, Integer>
Prism<Either<String, Integer>, Either<String, Integer>, String, String> left =
        Prism.of(
                "either.left",
                either -> either.left(),      // Optional<String>
                Either::left                  // String -> Either
        );
```

</tab>
<tab title="Parse a primitive">

```java
// "string looks like an int"
Prism<String, String, Integer, Integer> intPrism = Prism.of(
        "string.int",
        s -> {
            try     { return Optional.of(Integer.parseInt(s)); }
            catch (NumberFormatException e) { return Optional.empty(); }
        },
        Object::toString
);

intPrism.getOption("42");    // Optional[42]
intPrism.getOption("abc");   // Optional.empty
intPrism.reverseGet(42);     // "42"
```

</tab>
<tab title="Optional subclass">

```java
// "Some" case of Optional<String>
Prism<Optional<String>, Optional<String>, String, String> some = Prism.of(
        "optional.some",
        opt -> opt,
        Optional::of
);
```

</tab>
</tabs>

<note>
    <p>
        The <code>getOption</code> function is your <b>match + extract</b> step in
        one go: return <code>Optional.of(payload)</code> when the case matches,
        <code>Optional.empty()</code> otherwise. <code>reverseGet</code> is the
        constructor of the variant.
    </p>
</note>

## Reading and Modifying

```java
String  looksInt = "42";
String  looksWord = "abc";

intPrism.getOption(looksInt);  // Optional[42]
intPrism.getOption(looksWord); // Optional.empty

// modify: applied only if the case matches
intPrism.modify(looksInt, n -> n * 2);  // "84"
intPrism.modify(looksWord, n -> n * 2); // "abc" (unchanged)

// set: replace the payload if matched; pass-through otherwise
intPrism.set(looksInt, 99);  // "99"
intPrism.set(looksWord, 99); // "abc"
```

## Composition

```java
// Prism + Prism: drill into nested variants
Prism<OuterSum, OuterSum, InnerSum, InnerSum> inner = outer.compose(innerPrism);
```

Composing a `Prism` with another `Prism` gives another `Prism`. Composing with
a `Lens` gives an <a href="affine.md">Affine</a> (Prism + Lens = Affine &mdash;
maybe-present + read-write).

## Practical Uses

<deflist type="full">
    <def title="Matching taggedChoice variants">
        A <a href="dsl.md">taggedChoice</a> DSL template produces a sum type
        whose cases are ideal Prism targets.
    </def>
    <def title="Safe parsing / coercion">
        <code>"looks like a UUID"</code>, <code>"looks like an ISO date"</code>,
        <code>"parses as JSON"</code> &mdash; all Prism territory.
    </def>
    <def title="Option-style access">
        Standard library types such as <code>Optional</code> and <code>Either</code>
        have a prism per case.
    </def>
    <def title="Flagging legacy shapes">
        Match an old structure to extract it, then <code>reverseGet</code> the
        migrated one &mdash; a tidy way to express forward-patching.
    </def>
</deflist>

## Prism vs. Lens vs. Affine

<compare type="top-bottom" first-title="Always present (Lens)" second-title="Maybe present (Prism)">

```java
Lens<Player, Player, String, String> name = Lens.of(...);
String n = name.get(player);            // must exist
```

```java
Prism<JsonValue, JsonValue, String, String> asString = Prism.of(...);
Optional<String> s = asString.getOption(json);  // may be empty
```

</compare>

If the focus is a subtype/variant of `S`, you want a Prism. If it's always
there, you want a Lens. If it's a field that may be absent on the whole,
reach for <a href="affine.md">Affine</a>.

<seealso style="cards">
    <category ref="wrs">
        <a href="lens.md"        summary="Exactly-one focus &mdash; the dual of Prism."/>
        <a href="affine.md"      summary="Prism + Lens &mdash; maybe-there, read/write field."/>
        <a href="iso.md"         summary="Bidirectional lossless mapping."/>
        <a href="optics-overview.topic" summary="All optic types."/>
    </category>
</seealso>
