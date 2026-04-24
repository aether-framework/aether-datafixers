# TypeReference

<show-structure for="chapter,procedure" depth="2"/>

<tldr>
    <p><code>TypeReference</code> is a string-based identifier that uniquely names a
       data type inside %product%. It is the key the framework uses to route data to
       the right <code>Schema</code>, <code>DataFix</code>, and <code>Codec</code>.</p>
</tldr>

## At a Glance

<deflist type="medium">
    <def title="Type">
        <code>final class TypeReference</code>
    </def>
    <def title="Constructor">
        <code>new TypeReference(String id)</code> &mdash; rejects <code>null</code> and
        empty strings.
    </def>
    <def title="Accessor">
        <code>typeReference.getId()</code> returns the underlying string.
    </def>
    <def title="Equality">
        Value-based on the id string (case-sensitive).
    </def>
    <def title="Thread-safety">
        Fully immutable.
    </def>
</deflist>

## What TypeReference Is Used For

<deflist type="full">
    <def title="Type registration">
        Identifies a type when you register it in a <code>Schema</code>&apos;s
        <code>TypeRegistry</code>.
    </def>
    <def title="Fix routing">
        The <code>FixRegistrar</code> binds each <code>DataFix</code> to a
        <code>TypeReference</code>. When <code>fixer.update(...)</code> runs, only the
        fixes for that reference are applied.
    </def>
    <def title="Type lookup">
        <code>schema.require(TYPE_REFERENCE)</code> pulls the <code>Type&lt;?&gt;</code>
        registered for that reference at that version.
    </def>
    <def title="Self-describing data">
        <code>TaggedDynamic</code> pairs a <code>Dynamic</code> value with the
        <code>TypeReference</code> that describes it.
    </def>
</deflist>

## Creating References

```java
TypeReference player = new TypeReference("player");
TypeReference world  = new TypeReference("world");

String id = player.getId();           // "player"
boolean same = player.equals(new TypeReference("player")); // true
```

## The TypeReferences Class Pattern

The convention is to define every reference your app uses as a constant in one
place:

```java
public final class TypeReferences {

    // Player
    public static final TypeReference PLAYER          = new TypeReference("player");
    public static final TypeReference PLAYER_SETTINGS = new TypeReference("player_settings");

    // World
    public static final TypeReference WORLD        = new TypeReference("world");
    public static final TypeReference CHUNK        = new TypeReference("chunk");
    public static final TypeReference BLOCK_ENTITY = new TypeReference("block_entity");

    // Entities
    public static final TypeReference ENTITY = new TypeReference("entity");
    public static final TypeReference ITEM   = new TypeReference("item");

    // Config
    public static final TypeReference CONFIG = new TypeReference("config");

    private TypeReferences() {}
}
```

> This matches the style used by the `aether-datafixers-examples` module &mdash; the
> `TypeReferences` class is the single source of truth for every data type your
> migrations touch.
> {style="tip"}

## Using References

<tabs>
<tab title="In a Schema">

```java
@Override
protected void registerTypes() {
    registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name",  DSL.string()),
            DSL.field("level", DSL.intType()),
            DSL.remainder()
    ));
}
```

</tab>
<tab title="In the bootstrap">

```java
@Override
public void registerFixes(FixRegistrar fixes) {
    fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
    fixes.register(TypeReferences.WORLD,  new WorldV1ToV2Fix(schemas));
}
```

</tab>
<tab title="At migration time">

```java
Dynamic<JsonElement> dyn = new Dynamic<>(GsonOps.INSTANCE, rawJson);

Dynamic<JsonElement> migrated = fixer.update(
        TypeReferences.PLAYER,
        dyn,
        new DataVersion(100),
        fixer.currentVersion()
);
```

</tab>
<tab title="With TaggedDynamic">

```java
TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, dyn);
TaggedDynamic migrated = fixer.update(tagged, new DataVersion(100), fixer.currentVersion());

TypeReference type = migrated.type();   // TypeReferences.PLAYER
Dynamic<?> value   = migrated.value();
```

</tab>
</tabs>

## Naming Conventions

<deflist type="full">
    <def title="Use descriptive names">
        <code>"player"</code>, <code>"player_inventory"</code>, <code>"block_entity"</code>
        beat <code>"p"</code>, <code>"pi"</code>, or <code>"be"</code>.
    </def>
    <def title="Prefer snake_case">
        Consistent across the framework and the examples &mdash; <code>"player_settings"</code>
        not <code>"playerSettings"</code> or <code>"player-settings"</code>.
    </def>
    <def title="Match your domain">
        Names should mirror how the team already talks about the data: "quest",
        "inventory", "chunk".
    </def>
    <def title="One type per logical entity">
        Split data that can evolve independently. A <code>PLAYER_SETTINGS</code> type
        can be migrated without touching <code>PLAYER</code>.
    </def>
</deflist>

## Hierarchy: Polymorphic Types

The framework models discriminated unions through the DSL's
`taggedChoice`, so you rarely need one reference per subtype:

```java
registerType(TypeReferences.ENTITY, DSL.taggedChoice(
        "type",
        DSL.string(),
        Map.of(
                "player",  playerTemplate,
                "monster", monsterTemplate,
                "npc",     npcTemplate
        )
));
```

If you want flat hierarchical names instead (e.g. `"entity/player"`), treat the
slash as a naming convention &mdash; the framework only checks string equality.

## Equality and Hashing

`TypeReference` is value-based:

```java
TypeReference a = new TypeReference("player");
TypeReference b = new TypeReference("player");

a.equals(b); // true
a.hashCode() == b.hashCode(); // true

Map<TypeReference, DataFix<?>> fixes = new HashMap<>();
fixes.put(a, someFix); // retrievable via b
```

<seealso style="cards">
    <category ref="wrs">
        <a href="data-version.md" summary="Version identifiers for schemas."/>
        <a href="schema-system.md" summary="Registering types per version."/>
        <a href="dynamic-system.md" summary="TaggedDynamic for typed data."/>
    </category>
</seealso>
