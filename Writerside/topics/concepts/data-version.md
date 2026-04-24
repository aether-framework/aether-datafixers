# DataVersion

<show-structure for="chapter,procedure" depth="2"/>

<tldr>
    <p><code>DataVersion</code> is the integer-based identifier for a schema version.
       Higher numbers are newer. It is immutable, thread-safe, and implements
       <code>Comparable</code>.</p>
</tldr>

## At a Glance

<deflist type="medium">
    <def title="Type">
        <code>final class DataVersion implements Comparable&lt;DataVersion&gt;</code>
    </def>
    <def title="Constructor">
        <code>new DataVersion(int version)</code> &mdash; rejects negative values.
    </def>
    <def title="Accessor">
        <code>dataVersion.getVersion()</code> returns the wrapped <code>int</code>.
    </def>
    <def title="Comparison">
        Natural ordering via <code>compareTo</code>: lower value is older.
    </def>
    <def title="Thread-safety">
        Fully immutable.
    </def>
</deflist>

## Creating Versions

```java
DataVersion v1   = new DataVersion(1);
DataVersion v100 = new DataVersion(100);
DataVersion v200 = new DataVersion(200);

int raw = v100.getVersion();          // 100
boolean older = v100.compareTo(v200) < 0; // true
```

<warning>
    <p>Version numbers must be <b>non-negative</b>. Passing a negative integer throws
       an <code>IllegalArgumentException</code>.</p>
</warning>

## Versioning Strategies

Pick one scheme and stick with it. All that matters is that the numbers are
**monotonic**: each new version has a strictly greater value than the previous.

<tabs>
<tab title="SemVer-encoded (recommended)">

Encode `MAJOR * 100 + MINOR * 10 + PATCH` &mdash; readable, leaves room for hot-fixes,
and correlates with the release stream.

```java
new DataVersion(100); // v1.0.0
new DataVersion(101); // v1.0.1 (patch)
new DataVersion(110); // v1.1.0
new DataVersion(200); // v2.0.0
```

This is the scheme used throughout the examples and tests in this repository.

</tab>
<tab title="Sequential">

Simplest possible scheme &mdash; increment by one for each change.

```java
new DataVersion(1);
new DataVersion(2);
new DataVersion(3);
```

Good for small projects; offers no correlation with release versions.

</tab>
<tab title="Date-based">

`YYYYMMDD` is self-documenting and trivial to trace back to a release date.

```java
new DataVersion(20260101); // 2026-01-01
new DataVersion(20260215); // 2026-02-15
```

Append a suffix (e.g. `20260101_01`) if you need multiple changes per day &mdash;
but that requires a wider format, so plan ahead.

</tab>
<tab title="Build number">

Use CI build numbers. Zero manual bookkeeping, but gaps and out-of-order numbers
make reasoning harder.

```java
new DataVersion(1234);
new DataVersion(1235);
```

</tab>
</tabs>

## Recommended Patterns

### Centralise versions

```java
public final class DataVersions {

    /** v1.0.0 &mdash; initial release */
    public static final DataVersion V1_0_0 = new DataVersion(100);

    /** v1.1.0 &mdash; nested position, renamed xp → experience */
    public static final DataVersion V1_1_0 = new DataVersion(110);

    /** v2.0.0 &mdash; inventory, gameMode int → string */
    public static final DataVersion V2_0_0 = new DataVersion(200);

    public static final DataVersion CURRENT = V2_0_0;

    private DataVersions() {}
}
```

### Migrate only if needed

```java
if (savedVersion.compareTo(DataVersions.CURRENT) < 0) {
    migrated = fixer.update(type, dynamic, savedVersion, DataVersions.CURRENT);
}
```

### Store the version beside the data

A common on-disk shape:

```json
{
  "version": 110,
  "data": {
    "name": "Steve",
    "experience": 1500
  }
}
```

## Comparison

`DataVersion` implements `Comparable<DataVersion>` using `Integer.compare`:

| Call                          | Result                       |
|-------------------------------|------------------------------|
| `v100.compareTo(v110)`        | negative (v100 is older)     |
| `v110.compareTo(v100)`        | positive (v110 is newer)     |
| `v100.compareTo(v100)`        | zero                         |

`equals` and `hashCode` are value-based, so `DataVersion` is safe as a `Map` key.

## Rules of Thumb

<deflist type="full">
    <def title="Never reuse a released version number">
        Once data tagged with <code>DataVersion(X)</code> exists in the wild, its schema
        must not change. Create a new version instead.
    </def>
    <def title="Leave room for patches">
        SemVer-encoded <code>100, 101, 110, 200</code> gives you 9 patches and 9 minors
        per major &mdash; almost always enough without needing a wider encoding.
    </def>
    <def title="Document what changed">
        Keep a changelog on the version constants themselves (Javadoc) so the team can
        see the evolution in one place.
    </def>
</deflist>

<seealso style="cards">
    <category ref="wrs">
        <a href="type-reference.md" summary="Type identifiers for routing data to fixes."/>
        <a href="schema-system.md" summary="Associating versions with type definitions."/>
        <a href="datafix-system.md" summary="Creating migrations between versions."/>
    </category>
</seealso>
