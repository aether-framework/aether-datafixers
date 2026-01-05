# Mock Schemas

`MockSchemas` provides factory methods for creating mock `Schema` and `SchemaRegistry` instances for testing. These are useful when you need schemas for testing but don't want to set up full production schemas.

## Overview

```java
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;

// Create a minimal empty schema
Schema schema = MockSchemas.minimal(100);

// Create a registry with multiple schemas
SchemaRegistry registry = MockSchemas.chain(schema100, schema110, schema200);
```

## Minimal Schemas

### minimal(int version)

Create a minimal schema with no registered types:

```java
Schema schema100 = MockSchemas.minimal(100);
Schema schema110 = MockSchemas.minimal(110);
```

### minimal(DataVersion version)

Create a minimal schema using a DataVersion:

```java
Schema schema = MockSchemas.minimal(new DataVersion(100));
```

### minimal(int version, Schema parent)

Create a minimal schema with a parent for inheritance:

```java
Schema schema100 = MockSchemas.minimal(100);
Schema schema110 = MockSchemas.minimal(110, schema100);

// schema110 now inherits from schema100
```

## Schema Chains

### chain(Schema... schemas)

Create a `SchemaRegistry` containing all provided schemas:

```java
Schema schema100 = MockSchemas.minimal(100);
Schema schema110 = MockSchemas.minimal(110, schema100);
Schema schema200 = MockSchemas.minimal(200, schema110);

SchemaRegistry registry = MockSchemas.chain(schema100, schema110, schema200);

// Registry contains all three schemas
Schema found = registry.getSchema(new DataVersion(110));
```

### chainMinimal(int... versions)

Create a `SchemaRegistry` with minimal schemas for the specified versions:

```java
SchemaRegistry registry = MockSchemas.chainMinimal(100, 110, 200, 300);

// Creates 4 minimal schemas and registers them
Schema schema = registry.getSchema(new DataVersion(200));
```

## Schema Builder

For more control, use the builder API:

### builder(int version)

Start building a custom schema:

```java
Schema schema = MockSchemas.builder(100)
    .withType(PLAYER, playerType)
    .withType(WORLD, worldType)
    .build();
```

### withParent(Schema parent)

Set the parent schema:

```java
Schema schema110 = MockSchemas.builder(110)
    .withParent(schema100)
    .build();
```

### withType(TypeReference, Type)

Register a type in the schema:

```java
Schema schema = MockSchemas.builder(100)
    .withType(new TypeReference("player"), playerType)
    .withType(new TypeReference("world"), worldType)
    .build();
```

## Complete Examples

### Testing Schema Validation

```java
import de.splatgames.aether.datafixers.testkit.harness.SchemaTester;

@Test
void shouldCreateSchemaWithCorrectVersion() {
    Schema schema = MockSchemas.minimal(100);

    SchemaTester.forSchema(schema)
        .hasVersion(100)
        .hasNoParent()
        .verify();
}

@Test
void shouldCreateSchemaChain() {
    Schema schema100 = MockSchemas.minimal(100);
    Schema schema110 = MockSchemas.minimal(110, schema100);

    SchemaTester.forSchema(schema110)
        .hasVersion(110)
        .hasParent()
        .inheritsFrom(schema100)
        .parentHasVersion(100)
        .verify();
}
```

### Testing with Schema Registry

```java
@Test
void shouldResolveSchemaFromRegistry() {
    SchemaRegistry registry = MockSchemas.chainMinimal(100, 110, 200);

    Schema found = registry.getSchema(new DataVersion(110));

    assertThat(found).isNotNull();
    assertThat(found.version().getVersion()).isEqualTo(110);
}
```

### Testing with Custom Types

```java
@Test
void shouldRegisterCustomTypes() {
    // Create a mock type
    Type<?> playerType = createMockPlayerType();

    Schema schema = MockSchemas.builder(100)
        .withType(new TypeReference("player"), playerType)
        .build();

    SchemaTester.forSchema(schema)
        .hasVersion(100)
        .containsType("player")
        .doesNotContainType("world")
        .verify();
}
```

### Testing Migration with Mock Schemas

```java
@Test
void shouldMigrateWithMockSchemas() {
    // Setup mock schemas
    Schema schema100 = MockSchemas.minimal(100);
    Schema schema110 = MockSchemas.minimal(110, schema100);

    // Create a fix that uses these schemas
    DataFix<JsonElement> fix = new MyFix(schema100, schema110);

    // Test the fix
    Dynamic<JsonElement> input = TestData.gson().object()
        .put("name", "Alice")
        .build();

    Dynamic<JsonElement> result = DataFixTester.forFix(fix)
        .withInput(input)
        .forType("player")
        .apply();

    assertThat(result).hasField("name");
}
```

## SchemaTester

For validating schema configurations, use `SchemaTester`:

### Basic Validation

```java
SchemaTester.forSchema(schema)
    .hasVersion(100)
    .verify();
```

### Type Validation

```java
SchemaTester.forSchema(schema)
    .hasVersion(100)
    .containsType(PLAYER)
    .containsType(WORLD)
    .containsTypes(ENTITY, INVENTORY, ITEM)
    .doesNotContainType(LEGACY)
    .verify();
```

### Inheritance Validation

```java
SchemaTester.forSchema(schema110)
    .hasVersion(110)
    .hasParent()
    .inheritsFrom(schema100)
    .parentHasVersion(100)
    .verify();

SchemaTester.forSchema(rootSchema)
    .hasVersion(100)
    .hasNoParent()
    .verify();
```

### Type Validator

Validate specific type configurations:

```java
SchemaTester.forSchema(schema)
    .hasVersion(100)
    .typeForReference(PLAYER, type -> {
        assertThat(type).isNotNull();
        assertThat(type.reference()).isEqualTo(PLAYER);
        // Additional type-specific assertions...
    })
    .verify();
```

## Best Practices

1. **Use minimal schemas for simple tests** — When you don't need registered types
2. **Use the builder for complex scenarios** — When types are needed
3. **Chain schemas for migration tests** — Set up proper parent relationships
4. **Validate with SchemaTester** — Ensure schema configuration is correct

## Related

- [Testkit Overview](index.md)
- [DataFixTester](datafix-tester.md)
- [Schema System](../concepts/schema-system.md)
