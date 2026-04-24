# DSL

<show-structure for="chapter,procedure" depth="2"/>

<tldr>
    <p>The <code>DSL</code> is a declarative API for building <code>TypeTemplate</code>s &mdash;
       blueprints that become <code>Type</code>s when registered against a
       <code>TypeReference</code>. It is the ergonomic way to describe what your data
       looks like at each schema version.</p>
</tldr>

## A Template In One Snippet

```java
import de.splatgames.aether.datafixers.api.dsl.DSL;

TypeTemplate player = DSL.and(
        DSL.field("name",     DSL.string()),
        DSL.field("level",    DSL.intType()),
        DSL.field("position", DSL.and(
                DSL.field("x", DSL.doubleType()),
                DSL.field("y", DSL.doubleType()),
                DSL.field("z", DSL.doubleType())
        )),
        DSL.remainder()
);
```

Registering that template against a `TypeReference` in a `Schema` produces a
`Type<?>` with a working `Codec<?>`.

## Primitive Builders

| Method            | Produces                  |
|-------------------|---------------------------|
| `DSL.bool()`      | `Type<Boolean>`           |
| `DSL.byteType()`  | `Type<Byte>`              |
| `DSL.shortType()` | `Type<Short>`             |
| `DSL.intType()`   | `Type<Integer>`           |
| `DSL.longType()`  | `Type<Long>`              |
| `DSL.floatType()` | `Type<Float>`             |
| `DSL.doubleType()`| `Type<Double>`            |
| `DSL.string()`    | `Type<String>`            |

```java
registerType(TypeReferences.CONFIG, DSL.and(
        DSL.field("name",        DSL.string()),
        DSL.field("maxPlayers",  DSL.intType()),
        DSL.field("seed",        DSL.longType()),
        DSL.field("spawnRadius", DSL.floatType()),
        DSL.field("worldScale",  DSL.doubleType()),
        DSL.field("hardcore",    DSL.bool())
));
```

## Fields

<tabs>
<tab title="Required: field(name, template)">

```java
DSL.field("name", DSL.string())

DSL.field("position", DSL.and(
        DSL.field("x", DSL.doubleType()),
        DSL.field("y", DSL.doubleType()),
        DSL.field("z", DSL.doubleType())
))
```

</tab>
<tab title="Optional: optional(name, template)">

```java
DSL.optional("nickname", DSL.string())

DSL.optional("metadata", DSL.and(
        DSL.field("createdAt",  DSL.longType()),
        DSL.field("modifiedAt", DSL.longType())
))
```

</tab>
<tab title="Optional with default">

```java
DSL.optionalFieldOf("difficulty", DSL.string(), "normal")
DSL.optionalFieldOf("lives",      DSL.intType(), 3)
```

</tab>
</tabs>

## Composite Templates

### Product: `and(...)`

`and` combines multiple templates into a record-shaped product type.

```java
DSL.and(
        DSL.field("name",   DSL.string()),
        DSL.field("level",  DSL.intType()),
        DSL.field("active", DSL.bool())
)
```

### List: `list(inner)`

```java
DSL.list(DSL.string())                   // [String]
DSL.list(DSL.and(                        // [{id, name}]
        DSL.field("id",   DSL.intType()),
        DSL.field("name", DSL.string())
))
DSL.list(DSL.list(DSL.intType()))        // [[int]]
```

### Remainder: `remainder()`

```java
DSL.and(
        DSL.field("name",  DSL.string()),
        DSL.field("level", DSL.intType()),
        DSL.remainder()                   // captures any other field
)
```

<warning>
    <p>Without <code>DSL.remainder()</code> any field not explicitly typed is
       <b>dropped</b> during codec round-trips. Always include it on records unless
       you have a specific reason to strip unknowns.</p>
</warning>

### Sum: `taggedChoice(discriminator, type, choices)`

Discriminated unions &mdash; an inner record is picked by the value of a tag
field.

```java
DSL.taggedChoice(
        "type",
        DSL.string(),
        Map.of(
                "player",  playerTemplate,
                "monster", monsterTemplate,
                "item",    itemTemplate
        )
)
```

```json
{"type": "player",  "name": "Steve", "level": 10}
{"type": "monster", "name": "Zombie", "health": 20, "damage": 5}
{"type": "item",    "itemId": "diamond", "count": 64}
```

### Cross-References: `ref(typeReference)`

Reference another registered type by name &mdash; enabling recursion or sharing.

```java
DSL.field("player",   DSL.ref(TypeReferences.PLAYER))
DSL.list(DSL.ref(TypeReferences.ENTITY))

registerType(TypeReferences.WORLD, DSL.and(
        DSL.field("name",     DSL.string()),
        DSL.field("seed",     DSL.longType()),
        DSL.field("entities", DSL.list(DSL.ref(TypeReferences.ENTITY))),
        DSL.remainder()
));
```

## Reusable Helpers

Extract recurring shapes into static methods and share them across schemas.

```java
public final class CommonTemplates {

    public static TypeTemplate position() {
        return DSL.and(
                DSL.field("x", DSL.doubleType()),
                DSL.field("y", DSL.doubleType()),
                DSL.field("z", DSL.doubleType())
        );
    }

    public static TypeTemplate positionWithRotation() {
        return DSL.and(
                DSL.field("x",     DSL.doubleType()),
                DSL.field("y",     DSL.doubleType()),
                DSL.field("z",     DSL.doubleType()),
                DSL.field("yaw",   DSL.floatType()),
                DSL.field("pitch", DSL.floatType())
        );
    }

    public static TypeTemplate itemStack() {
        return DSL.and(
                DSL.field("id",              DSL.string()),
                DSL.optionalFieldOf("count", DSL.intType(), 1),
                DSL.optional("nbt",          DSL.remainder())
        );
    }

    private CommonTemplates() {}
}
```

## A Realistic Schema

```java
public class Schema100 extends Schema {

    public Schema100() {
        super(100, null);
    }

    @Override
    protected TypeRegistry createTypeRegistry() {
        return new SimpleTypeRegistry();
    }

    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
                DSL.field("uuid",               DSL.string()),
                DSL.field("name",               DSL.string()),
                DSL.field("level",              DSL.intType()),
                DSL.field("experience",         DSL.intType()),
                DSL.field("position",           CommonTemplates.positionWithRotation()),
                DSL.field("inventory",          DSL.list(CommonTemplates.itemStack())),
                DSL.optional("achievements",    DSL.list(DSL.string())),
                DSL.remainder()
        ));

        registerType(TypeReferences.WORLD, DSL.and(
                DSL.field("name",       DSL.string()),
                DSL.field("seed",       DSL.longType()),
                DSL.field("gameMode",   DSL.intType()),
                DSL.field("difficulty", DSL.intType()),
                DSL.field("spawnPoint", CommonTemplates.position()),
                DSL.remainder()
        ));
    }
}
```

## DSL Cheat Sheet

| Method                                    | Purpose                     |
|-------------------------------------------|-----------------------------|
| `string`, `intType`, `longType`, …        | Primitive types             |
| `field(name, template)`                   | Required named field        |
| `optional(name, template)`                | Optional named field        |
| `optionalFieldOf(name, template, default)`| Optional with default value |
| `and(templates...)`                       | Product type                |
| `list(template)`                          | Homogeneous list            |
| `remainder()`                             | Preserves unknown fields    |
| `taggedChoice(tag, tagType, choices)`     | Sum / discriminated union   |
| `ref(typeReference)`                      | Reference another type      |

## Rules of Thumb

<deflist type="full">
    <def title="Always include remainder() on records">
        Ensures forward-compatibility and preserves plugin / extension data that your
        version does not know about.
    </def>
    <def title="Extract reusable templates">
        Positions, item stacks, UUIDs, timestamps &mdash; moving them into helpers
        keeps each schema focused on deltas.
    </def>
    <def title="Prefer taggedChoice over conditional decoding">
        Discriminated unions make intent explicit and migrate predictably.
    </def>
    <def title="Document non-obvious field semantics">
        Unit (seconds vs. ms), encoding (UUID as string vs. binary), and invariants
        save hours for future maintainers.
    </def>
</deflist>

<seealso style="cards">
    <category ref="wrs">
        <a href="schema-system.md" summary="Where DSL templates are registered."/>
        <a href="type-system.md" summary="How templates become typed Codecs."/>
        <a href="codec-system.md" summary="Encoding and decoding DSL-built types."/>
    </category>
</seealso>
