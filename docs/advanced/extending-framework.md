# Extending the Framework

Adding custom functionality to Aether Datafixers.

## Custom Type Templates

```java
public class ValidatedFieldTemplate implements TypeTemplate {
    private final String fieldName;
    private final TypeTemplate valueTemplate;
    private final Predicate<Dynamic<?>> validator;

    @Override
    public Type<?> apply(TypeFamily family) {
        Type<?> valueType = valueTemplate.apply(family);
        return new ValidatedFieldType(fieldName, valueType, validator);
    }
}
```

## Custom Codecs

```java
public class UUIDCodec implements Codec<UUID> {

    @Override
    public <T> DataResult<T> encode(UUID input, DynamicOps<T> ops, T prefix) {
        return DataResult.success(ops.createString(input.toString()));
    }

    @Override
    public <T> DataResult<UUID> decode(DynamicOps<T> ops, T input) {
        return ops.getStringValue(input)
            .flatMap(s -> {
                try {
                    return DataResult.success(UUID.fromString(s));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Invalid UUID: " + s);
                }
            });
    }
}
```

## Custom DynamicOps

```java
public class TomlOps implements DynamicOps<TomlValue> {
    public static final TomlOps INSTANCE = new TomlOps();

    @Override
    public TomlValue createString(String value) {
        return new TomlString(value);
    }

    // Implement all DynamicOps methods...
}
```

## Custom DataFix Base Class

```java
public abstract class LoggingDataFix extends SchemaDataFix {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Dynamic<Object> apply(Dynamic<Object> input) {
        logger.debug("Applying fix: {}", name());
        long start = System.nanoTime();

        Dynamic<Object> result = super.apply(input);

        logger.debug("Fix {} completed in {}μs", name(), (System.nanoTime() - start) / 1000);
        return result;
    }
}
```

## Custom Rules

```java
public final class CustomRules {

    public static TypeRewriteRule validateField(
        TypeReference type, String field, Predicate<Dynamic<?>> validator
    ) {
        return Rules.transform(type, data -> {
            Dynamic<?> value = data.get(field).orElseEmptyMap();
            if (!validator.test(value)) {
                throw new ValidationException("Invalid " + field);
            }
            return data;
        });
    }
}
```

## Plugin System

```java
public interface DataFixerPlugin {
    void registerSchemas(SchemaRegistry schemas);
    void registerFixes(FixRegistrar fixes);
}

public class PluggableBootstrap implements DataFixerBootstrap {
    private final List<DataFixerPlugin> plugins;

    @Override
    public void registerSchemas(SchemaRegistry schemas) {
        plugins.forEach(p -> p.registerSchemas(schemas));
    }

    @Override
    public void registerFixes(FixRegistrar fixes) {
        plugins.forEach(p -> p.registerFixes(fixes));
    }
}
```

## Related

- [Custom Optics](custom-optics.md)
- [Custom DynamicOps Tutorial](../tutorials/custom-dynamicops.md)

