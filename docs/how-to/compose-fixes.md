# How to Compose Fixes

This guide shows how to combine multiple transformation rules into complex migrations.

## Using Rules.seq

Apply rules in sequence (order matters):

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
        Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),
        Rules.transformField(TypeReferences.PLAYER, "gameMode", this::gameModeToString),
        Rules.transform(TypeReferences.PLAYER, this::nestPosition)
    );
}
```

## Order Matters

Later rules see the result of earlier rules:

```java
// Correct order
Rules.seq(
    Rules.renameField(TYPE, "old", "new"),      // 1. Rename first
    Rules.transformField(TYPE, "new", fn)       // 2. Transform using new name
)

// Wrong order - "new" doesn't exist yet!
Rules.seq(
    Rules.transformField(TYPE, "new", fn),      // Error: "new" doesn't exist
    Rules.renameField(TYPE, "old", "new")
)
```

## Using Rules.all

Apply multiple independent rules (order doesn't matter):

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.all(
        Rules.renameField(TYPE, "field1", "newField1"),  // Independent
        Rules.renameField(TYPE, "field2", "newField2"),  // Independent
        Rules.renameField(TYPE, "field3", "newField3")   // Independent
    );
}
```

## Combine seq and all

```java
return Rules.seq(
    // First: all renames (can be parallel)
    Rules.all(
        Rules.renameField(TYPE, "a", "x"),
        Rules.renameField(TYPE, "b", "y"),
        Rules.renameField(TYPE, "c", "z")
    ),

    // Then: transformations (depends on renamed fields)
    Rules.all(
        Rules.transformField(TYPE, "x", this::transformX),
        Rules.transformField(TYPE, "y", this::transformY)
    ),

    // Finally: structural changes
    Rules.transform(TYPE, this::restructure)
);
```

## Multiple Types in One Fix

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        // Player changes
        Rules.renameField(TypeReferences.PLAYER, "hp", "health"),
        Rules.transform(TypeReferences.PLAYER, this::migratePlayer),

        // World changes
        Rules.addField(TypeReferences.WORLD, "version", d -> d.createInt(2)),

        // Config changes
        Rules.transformField(TypeReferences.CONFIG, "difficulty", this::difficultyToString)
    );
}
```

## Extract Helper Methods

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        playerRenameRules(),
        playerTransformRules(),
        playerStructureRules()
    );
}

private TypeRewriteRule playerRenameRules() {
    return Rules.all(
        Rules.renameField(TypeReferences.PLAYER, "playerName", "name"),
        Rules.renameField(TypeReferences.PLAYER, "xp", "experience"),
        Rules.renameField(TypeReferences.PLAYER, "hp", "health")
    );
}

private TypeRewriteRule playerTransformRules() {
    return Rules.all(
        Rules.transformField(TypeReferences.PLAYER, "gameMode", this::gameModeToString),
        Rules.transformField(TypeReferences.PLAYER, "difficulty", this::difficultyToInt)
    );
}

private TypeRewriteRule playerStructureRules() {
    return Rules.transform(TypeReferences.PLAYER, this::nestPosition);
}
```

## Conditional Composition

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    List<TypeRewriteRule> rules = new ArrayList<>();

    // Always apply base rules
    rules.add(Rules.renameField(TYPE, "old", "new"));

    // Conditionally add more rules
    if (shouldMigratePosition) {
        rules.add(Rules.transform(TYPE, this::migratePosition));
    }

    if (shouldAddNewFields) {
        rules.add(Rules.addField(TYPE, "newField", d -> d.createInt(0)));
    }

    return Rules.seq(rules.toArray(TypeRewriteRule[]::new));
}
```

## Reusable Rule Builders

```java
public class CommonRules {

    public static TypeRewriteRule renameHealthField(TypeReference type) {
        return Rules.renameField(type, "hp", "health");
    }

    public static TypeRewriteRule addVersionField(TypeReference type, int version) {
        return Rules.addField(type, "version", d -> d.createInt(version));
    }

    public static TypeRewriteRule nestCoordinates(TypeReference type) {
        return Rules.transform(type, data -> {
            double x = data.get("x").asDouble().orElse(0.0);
            double y = data.get("y").asDouble().orElse(0.0);
            double z = data.get("z").asDouble().orElse(0.0);

            Dynamic<?> position = data.emptyMap()
                .set("x", data.createDouble(x))
                .set("y", data.createDouble(y))
                .set("z", data.createDouble(z));

            return data.remove("x").remove("y").remove("z")
                .set("position", position);
        });
    }
}

// Usage
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    return Rules.seq(
        CommonRules.renameHealthField(TypeReferences.PLAYER),
        CommonRules.addVersionField(TypeReferences.PLAYER, 2),
        CommonRules.nestCoordinates(TypeReferences.PLAYER)
    );
}
```

## Empty/No-Op Rule

When you need a placeholder:

```java
// No-op rule for conditional composition
TypeRewriteRule noOp = Rules.seq();  // Empty sequence does nothing
```

## Debug Composition

Add logging between rules:

```java
return Rules.seq(
    Rules.renameField(TYPE, "old", "new"),
    Rules.transform(TYPE, data -> {
        System.out.println("After rename: " + data);
        return data;  // Pass through unchanged
    }),
    Rules.transformField(TYPE, "new", transform)
);
```

## Best Practices

1. **Group related rules** with `all()` when order doesn't matter
2. **Use `seq()` for dependent** rules where order matters
3. **Extract helper methods** for readability
4. **Document rule order** with comments
5. **Test each rule** independently first

## Related

- [Rewrite Rules](../concepts/rewrite-rules.md)
- [DataFix System](../concepts/datafix-system.md)
- [Debug Migrations](debug-migrations.md)

