# How to Use Conditional Rules

This guide shows how to apply transformation rules conditionally based on the presence, absence, or value of fields.

## Problem

Not all data needs the same transformations:
- Some records have legacy fields that need migration, others don't
- Some records are missing fields that need defaults
- Different versions of data require different migration paths

## Solution: Conditional Rules

The `Rules` class provides three conditional rule factories:

- `ifFieldExists()` - Apply rule only when a field is present
- `ifFieldMissing()` - Apply rule only when a field is absent
- `ifFieldEquals()` - Apply rule only when a field has a specific value

### If Field Exists

Apply a rule only when a specific field exists:

```java
import de.splatgames.aether.datafixers.api.rewrite.Rules;

TypeRewriteRule rule = Rules.ifFieldExists(GsonOps.INSTANCE, "legacyData",
    // This rule only runs if "legacyData" field exists
    Rules.seq(
        Rules.renameField(PLAYER, "legacyData", "data"),
        Rules.addField(PLAYER, "migrated", d -> d.createBoolean(true))
    )
);
```

**Input with legacy field:**
```json
{
  "name": "Steve",
  "legacyData": {"old": "value"}
}
```

**Output:**
```json
{
  "name": "Steve",
  "data": {"old": "value"},
  "migrated": true
}
```

**Input without legacy field (unchanged):**
```json
{
  "name": "Steve",
  "data": {"new": "value"}
}
```

### If Field Missing

Apply a rule only when a specific field is absent:

```java
TypeRewriteRule rule = Rules.ifFieldMissing(GsonOps.INSTANCE, "version",
    // Add default version only if not present
    Rules.addField(PLAYER, "version", d -> d.createInt(1))
);
```

**Input without version:**
```json
{
  "name": "Steve",
  "level": 10
}
```

**Output:**
```json
{
  "name": "Steve",
  "level": 10,
  "version": 1
}
```

**Input with version (unchanged):**
```json
{
  "name": "Steve",
  "level": 10,
  "version": 5
}
```

### If Field Equals

Apply a rule only when a field has a specific value:

```java
// Migrate only version 1 data
TypeRewriteRule rule = Rules.ifFieldEquals(GsonOps.INSTANCE, "version", 1,
    Rules.seq(
        Rules.renameField(PLAYER, "oldField", "newField"),
        Rules.setField(ops, "version", new Dynamic<>(ops, ops.createInt(2)))
    )
);
```

**Input with version 1:**
```json
{
  "name": "Steve",
  "oldField": "value",
  "version": 1
}
```

**Output:**
```json
{
  "name": "Steve",
  "newField": "value",
  "version": 2
}
```

**Input with version 2 (unchanged):**
```json
{
  "name": "Steve",
  "newField": "value",
  "version": 2
}
```

## Supported Value Types

`ifFieldEquals()` supports multiple value types:

```java
// Integer comparison
Rules.ifFieldEquals(ops, "version", 1, rule)

// String comparison
Rules.ifFieldEquals(ops, "type", "player", rule)

// Boolean comparison
Rules.ifFieldEquals(ops, "active", true, rule)

// Double comparison
Rules.ifFieldEquals(ops, "scale", 1.0, rule)
```

## Complete Example

A migration that handles multiple data formats:

```java
public class UnifyPlayerDataFix extends SchemaDataFix {

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.seq(
            // Handle legacy format (has "legacyPlayer" field)
            Rules.ifFieldExists(GsonOps.INSTANCE, "legacyPlayer",
                Rules.seq(
                    Rules.flattenField(GsonOps.INSTANCE, "legacyPlayer"),
                    Rules.removeFields(GsonOps.INSTANCE, "formatVersion")
                )
            ),

            // Add version if missing (very old data)
            Rules.ifFieldMissing(GsonOps.INSTANCE, "version",
                Rules.addField(PLAYER, "version", d -> d.createInt(1))
            ),

            // Migrate from version 1
            Rules.ifFieldEquals(GsonOps.INSTANCE, "version", 1,
                Rules.seq(
                    Rules.renameFields(GsonOps.INSTANCE, Map.of(
                        "playerName", "name",
                        "xp", "experience"
                    )),
                    Rules.setField(GsonOps.INSTANCE, "version",
                        new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(2)))
                )
            ),

            // Migrate from version 2
            Rules.ifFieldEquals(GsonOps.INSTANCE, "version", 2,
                Rules.seq(
                    Rules.groupFields(GsonOps.INSTANCE, "position", "x", "y", "z"),
                    Rules.setField(GsonOps.INSTANCE, "version",
                        new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(3)))
                )
            )
        );
    }
}
```

## Nesting Conditions

Conditions can be nested for complex logic:

```java
Rules.ifFieldExists(ops, "config",
    Rules.ifFieldEquals(ops, "config.version", 1,
        Rules.transformFieldAt(ops, "config.settings", this::migrateSettings)
    )
)
```

## Best Practices

1. **Order conditions logically** - Check for legacy formats before current ones
2. **Use `ifFieldMissing` for defaults** - Safer than always setting
3. **Update version markers** - After conditional migration, update version field
4. **Test all code paths** - Ensure each condition path is tested
5. **Document conditions** - Explain why each condition exists

## Related

- [Handle Optional Fields](handle-optional-fields.md) - Working with optional data
- [Compose Fixes](compose-fixes.md) - Combining multiple rules
- [Rewrite Rules](../concepts/rewrite-rules.md) - Complete rules reference

