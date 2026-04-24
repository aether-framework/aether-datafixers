# Iso

<show-structure for="chapter,procedure" depth="2"/>

<tldr>
    <p>An <code>Iso&lt;S, T, A, B&gt;</code> is a <b>lossless, bidirectional</b>
       conversion between two representations of the same information. Both
       directions always succeed &mdash; there is no "maybe" and no data loss.</p>
</tldr>

## Shape

```java
public interface Iso<S, T, A, B> {
    String id();
    A      to(S source);           // S → A
    S      from(A target);         // A → S
    default S modify(S, Function<A, A>);
    default Iso<B, A, T, S> reverse();
}
```

<note>
    <p>Because an Iso is lossless in both directions, it is at once a
       <a href="lens.md">Lens</a>, a <a href="prism.md">Prism</a>, and more. Any
       Iso can be used where any of those is required &mdash; the type system
       reflects this through default methods (<code>get</code>, <code>set</code>,
       <code>getOption</code>, <code>reverseGet</code>).</p>
</note>

## The Iso Laws

<deflist type="full">
    <def title="Round-trip (forward)">
        <code>iso.from(iso.to(s)) == s</code>
    </def>
    <def title="Round-trip (backward)">
        <code>iso.to(iso.from(a)) == a</code>
    </def>
</deflist>

If either law is broken, it is not an Iso &mdash; you probably want a
<a href="prism.md">Prism</a> (lossy in one direction) or a
<a href="lens.md">Lens</a> (one-way get, independent set).

## Creating an Iso

<tabs>
<tab title="String ↔ UUID">

```java
Iso<String, String, UUID, UUID> uuidIso = Iso.of(
        "string.uuid",
        UUID::fromString,
        UUID::toString
);

String s  = "550e8400-e29b-41d4-a716-446655440000";
UUID   u  = uuidIso.to(s);
String s2 = uuidIso.from(u);         // equal to s
```

</tab>
<tab title="Seconds ↔ Duration">

```java
Iso<Long, Long, Duration, Duration> secondsIso = Iso.of(
        "long.seconds",
        Duration::ofSeconds,
        Duration::toSeconds
);

Duration d = secondsIso.to(90L);      // PT1M30S
long     s = secondsIso.from(d);      // 90
```

</tab>
<tab title="Identity">

```java
Iso<String, String, String, String> id = Iso.identity();
id.to("hello");    // "hello"
id.from("hello");  // "hello"
```

Composing any Iso with the identity yields the original. Useful as a neutral
element when you build Iso chains generically.

</tab>
</tabs>

## Using an Iso

```java
// get / set / modify come free from the Iso laws
uuidIso.get(someString);                        // UUID
uuidIso.set(someString, otherUuid);             // new String
uuidIso.modify(someString, UUID::toString /*…*/); // still a String

// Lossless reverse
Iso<UUID, UUID, String, String> reversed = uuidIso.reverse();
reversed.to(someUuid);     // String
```

## Composition

Composing two `Iso`s yields another `Iso`:

```java
Iso<String, String, UUID, UUID>     uuidIso     = Iso.of(/* … */);
Iso<UUID,   UUID,   byte[], byte[]> bytesIso    = Iso.of(/* … */);

Iso<String, String, byte[], byte[]> stringBytes = uuidIso.compose(bytesIso);
// id = "string.uuid.<bytes-id>"
```

Compose an Iso with weaker optics and you get the weaker optic:

| `compose` target     | Result type               |
|----------------------|---------------------------|
| another `Iso`        | `Iso`                     |
| `Lens`               | `Lens`                    |
| `Prism`              | `Prism`                   |
| `Affine`             | `Affine`                  |
| `Traversal`          | `Traversal`               |

## Classic Iso Use Cases

<deflist type="full">
    <def title="Encoding representations">
        <code>UUID</code> &harr; <code>String</code>,
        <code>Instant</code> &harr; <code>long</code> epoch-millis,
        <code>byte[]</code> &harr; <code>Base64 String</code>.
    </def>
    <def title="Unit conversions">
        Kelvin &harr; Celsius + 273.15 style mappings where both directions are
        total and exact.
    </def>
    <def title="Wrapper newtypes">
        <code>EmailAddress</code> &harr; <code>String</code> where the wrapper
        adds nominal typing but no information.
    </def>
    <def title="Record ↔ tuple">
        A record and a <code>Pair&lt;A, B&gt;</code> carrying the same two fields
        are typically isomorphic.
    </def>
</deflist>

## Iso vs. Prism vs. Lens

<compare type="top-bottom" first-title="Iso (lossless, total)" second-title="Prism (lossy, partial)">

```java
Iso<Long, Long, Duration, Duration> secondsIso = /* total both ways */;
Duration d = secondsIso.to(90L);         // always succeeds
long     s = secondsIso.from(d);         // always succeeds
```

```java
Prism<String, String, Integer, Integer> intPrism = /* may fail */;
Optional<Integer> parsed = intPrism.getOption("abc"); // empty
```

</compare>

If you can write `from(to(s)) == s` and `to(from(a)) == a` **without a single
<code>Optional</code>**, you have an Iso.

<seealso style="cards">
    <category ref="wrs">
        <a href="lens.md"   summary="One focus, get/set independently."/>
        <a href="prism.md"  summary="Maybe-there variant matching."/>
        <a href="optics-overview.topic" summary="All optic types."/>
    </category>
</seealso>
