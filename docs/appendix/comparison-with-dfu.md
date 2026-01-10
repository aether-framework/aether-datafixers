# Comparison with DFU

How Aether Datafixers compares to Minecraft's DataFixer Upper (DFU).

## Overview

Aether Datafixers is inspired by DFU but designed to be more accessible and lightweight.

## Key Differences

| Aspect         | DFU       | Aether Datafixers |
|----------------|-----------|-------------------|
| Learning curve | Steep     | Moderate          |
| Documentation  | Limited   | Comprehensive     |
| API complexity | Complex   | Simplified        |
| Dependencies   | Minimal   | Minimal           |
| Use case       | Minecraft | General purpose   |

## Simplified APIs

### Schema Registration

**DFU:**
```java
// Complex builder pattern
Schema v1 = new Schema(1, parent) {
    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
        return EntityTypes.registerEntities(schema);
    }
    // ... more overrides
};
```

**Aether:**
```java
// Clean inheritance
public class Schema100 extends Schema {
    @Override
    protected void registerTypes() {
        registerType(TypeReferences.PLAYER, DSL.and(
            DSL.field("name", DSL.string()),
            DSL.remainder()
        ));
    }
}
```

### DataFix Creation

**DFU:**
```java
public class PlayerFix extends DataFix {
    public PlayerFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped("PlayerFix",
            getInputSchema().getType(References.PLAYER),
            getOutputSchema().getType(References.PLAYER),
            typed -> typed.update(DSL.remainderFinder(), dynamic -> ...));
    }
}
```

**Aether:**
```java
public class PlayerFix extends SchemaDataFix {
    public PlayerFix(SchemaRegistry schemas) {
        super(schemas, new DataVersion(1), new DataVersion(2), "player-fix");
    }

    @Override
    protected TypeRewriteRule makeRule(Schema input, Schema output) {
        return Rules.renameField(TypeReferences.PLAYER, "old", "new");
    }
}
```

## Cleaner DSL

**DFU:**
```java
DSL.optionalFields(
    DSL.field("Data", DSL.compoundList(DSL.string(), DSL.remainderType()))
)
```

**Aether:**
```java
DSL.and(
    DSL.optional("data", DSL.list(DSL.remainder())),
    DSL.remainder()
)
```

## Better Error Handling

**Aether** uses `DataResult` consistently with clear error messages and partial result support.

## Modular Design

Aether separates concerns into modules:
- `aether-datafixers-api`: Interfaces only
- `aether-datafixers-core`: Implementations
- `aether-datafixers-codec`: Format adapters

## Migration from DFU

1. Replace DFU imports with Aether imports
2. Extend `SchemaDataFix` instead of `DataFix`
3. Use `Rules` factory methods
4. Update bootstrap to implement `DataFixerBootstrap`

## Related

- [Getting Started](../getting-started/index.md)
- [Basic Migration Tutorial](../tutorials/basic-migration.md)

