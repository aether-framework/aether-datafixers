# Format-Specific Security Considerations

Each serialization format supported by Aether Datafixers has unique security characteristics. This section provides detailed guidance for secure configuration of each format.

## Risk Summary

| Format | Library   | Risk Level   | Primary Concerns                              |
|--------|-----------|--------------|-----------------------------------------------|
| YAML   | SnakeYAML | **Critical** | Arbitrary code execution, Billion Laughs      |
| YAML   | Jackson   | Low-Medium   | Depth limits only                             |
| XML    | Jackson   | **High**     | XXE, Entity expansion                         |
| JSON   | Jackson   | Medium       | Polymorphic typing (if enabled), depth limits |
| JSON   | Gson      | Low          | Minimal attack surface                        |
| TOML   | Jackson   | Low          | Limited attack surface                        |

## Format-Specific Guides

### [SnakeYAML Security](snakeyaml.md)

**Risk Level: Critical**

SnakeYAML's default configuration allows arbitrary Java class instantiation, making it extremely dangerous for untrusted input. This guide covers:

- Arbitrary code execution prevention
- Billion Laughs attack mitigation
- Safe `LoaderOptions` configuration
- Complete secure setup example

### [Jackson Security](jackson.md)

**Risk Level: Medium-High (format dependent)**

Jackson is used for JSON, YAML, XML, and TOML. Security considerations vary by format:

- **JSON:** Polymorphic deserialization risks
- **YAML:** Fewer risks than SnakeYAML (no arbitrary constructors)
- **XML:** XXE vulnerabilities
- **All:** Depth and size limits

### [Gson Security](gson.md)

**Risk Level: Low**

Gson has a relatively small attack surface by default. This guide covers:

- Safe default behavior
- Pre-validation recommendations
- Depth validation patterns

## Quick Reference

### SnakeYAML: Always Use SafeConstructor

```java
LoaderOptions options = new LoaderOptions();
options.setMaxAliasesForCollections(50);
options.setNestingDepthLimit(50);

Yaml safeYaml = new Yaml(new SafeConstructor(options));
```

### Jackson XML: Disable External Entities

```java
XMLInputFactory xmlFactory = XMLInputFactory.newFactory();
xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
```

### Jackson JSON: Configure Read Constraints

```java
StreamReadConstraints constraints = StreamReadConstraints.builder()
    .maxNestingDepth(50)
    .maxStringLength(1_000_000)
    .build();
```

### Jackson: Never Enable Default Typing

```java
// DANGEROUS - Never do this with untrusted data:
// mapper.enableDefaultTyping();

// DANGEROUS - Also avoid:
// mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator());
```

## Decision Matrix

Use this matrix to determine which security measures to apply:

| Data Source       | SnakeYAML                | Jackson JSON    | Jackson XML    | Gson           |
|-------------------|--------------------------|-----------------|----------------|----------------|
| User uploads      | SafeConstructor + limits | Depth limits    | XXE + limits   | Pre-validation |
| External APIs     | SafeConstructor + limits | Depth limits    | XXE + limits   | Pre-validation |
| Message queues    | SafeConstructor + limits | Depth limits    | XXE + limits   | Pre-validation |
| Internal services | Consider SafeConstructor | Optional limits | XXE prevention | Default OK     |
| Local config      | Default OK               | Default OK      | Default OK     | Default OK     |

## Related

- [Threat Model](../threat-model.md)
- [Best Practices](../best-practices.md)
- [Secure Configuration Examples](../secure-configuration-examples.md)
