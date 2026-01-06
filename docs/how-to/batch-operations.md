# How to Use Batch Operations

This guide shows how to rename or remove multiple fields in a single operation using the extended `Rules` methods.

## Problem

When migrating data, you often need to rename or remove several fields at once. Writing individual rules for each field is verbose:

```java
// Verbose approach
Rules.seq(
    Rules.renameField(ops, "playerName", "name"),
    Rules.renameField(ops, "xp", "experience"),
    Rules.renameField(ops, "hp", "health"),
    Rules.renameField(ops, "pos_x", "x"),
    Rules.renameField(ops, "pos_y", "y")
)
```

## Solution

Use `Rules.renameFields()` and `Rules.removeFields()` for batch operations.

### Batch Rename

Rename multiple fields using a `Map`:

```java
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import java.util.Map;

TypeRewriteRule rule = Rules.renameFields(GsonOps.INSTANCE, Map.of(
    "playerName", "name",
    "xp", "experience",
    "hp", "health",
    "pos_x", "x",
    "pos_y", "y"
));
```

**Input:**
```json
{
  "playerName": "Steve",
  "xp": 1500,
  "hp": 20,
  "pos_x": 100.5,
  "pos_y": 64.0
}
```

**Output:**
```json
{
  "name": "Steve",
  "experience": 1500,
  "health": 20,
  "x": 100.5,
  "y": 64.0
}
```

### Batch Remove

Remove multiple fields at once:

```java
TypeRewriteRule rule = Rules.removeFields(GsonOps.INSTANCE,
    "deprecated1",
    "deprecated2",
    "legacyField",
    "oldData"
);
```

**Input:**
```json
{
  "name": "Steve",
  "level": 10,
  "deprecated1": "old",
  "deprecated2": 123,
  "legacyField": true,
  "oldData": {}
}
```

**Output:**
```json
{
  "name": "Steve",
  "level": 10
}
```

## Complete Example

A complete DataFix using batch operations:

```java
public class PlayerV1ToV2Fix extends SchemaDataFix {

    public PlayerV1ToV2Fix(Schema input, Schema output) {
        super("player_v1_to_v2", input, output);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            // Batch rename legacy field names
            Rules.renameFields(GsonOps.INSTANCE, Map.of(
                "playerName", "name",
                "xp", "experience",
                "hp", "health"
            )),

            // Remove obsolete fields
            Rules.removeFields(GsonOps.INSTANCE,
                "legacyId",
                "oldFormat",
                "deprecated"
            ),

            // Add new required fields
            Rules.addField(PLAYER, "version", d -> d.createInt(2))
        );
    }
}
```

## Combining with Other Rules

Batch operations work seamlessly with other rules:

```java
Rules.seq(
    // First: batch rename
    Rules.renameFields(ops, Map.of(
        "oldName", "name",
        "oldLevel", "level"
    )),

    // Then: transform a field
    Rules.transformField(TYPE, "level", this::convertLevel),

    // Then: group fields
    Rules.groupFields(ops, "stats", "health", "mana", "stamina"),

    // Finally: remove obsolete
    Rules.removeFields(ops, "temp1", "temp2")
)
```

## Best Practices

1. **Order matters** - Rename fields before transforming them by their new names
2. **Combine logically** - Group related renames together
3. **Document the mapping** - For large migrations, document what each rename means
4. **Test thoroughly** - Verify all field mappings work correctly

## Related

- [Rename a Field](rename-field.md) - Single field renaming
- [Remove a Field](remove-field.md) - Single field removal
- [Compose Fixes](compose-fixes.md) - Combining multiple rules
- [Rewrite Rules](../concepts/rewrite-rules.md) - Complete rules reference

