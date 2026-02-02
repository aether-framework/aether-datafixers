# Security Overview

This section provides guidance for securely handling untrusted data with Aether Datafixers. When processing data from external sources—user uploads, APIs, message queues, or file imports—proper security measures are essential to prevent attacks.

## Quick Reference

| Threat                            | Affected Formats | Risk         | Mitigation                |
|-----------------------------------|------------------|--------------|---------------------------|
| Arbitrary Code Execution          | YAML (SnakeYAML) | **Critical** | Use `SafeConstructor`     |
| Billion Laughs (Entity Expansion) | YAML, XML        | High         | Limit aliases/entities    |
| XXE (External Entity Injection)   | XML              | High         | Disable external entities |
| Polymorphic Deserialization       | JSON (Jackson)   | Medium       | Avoid default typing      |
| Resource Exhaustion               | All              | Medium       | Size and depth limits     |
| Stack Overflow                    | All              | Medium       | Nesting depth limits      |

## When to Apply Security Measures

Apply the security recommendations in this documentation when:

- **User Uploads** — Processing files uploaded by users (game saves, configs, data imports)
- **External APIs** — Consuming data from third-party APIs
- **Message Queues** — Processing messages from queues (Kafka, RabbitMQ, etc.)
- **Database Blobs** — Migrating serialized data stored in databases
- **File Imports** — Reading configuration or data files from untrusted sources

## Documentation Structure

### [Threat Model](threat-model.md)

Understand the attack vectors and trust boundaries:
- Classification of untrusted data sources
- Detailed attack vector descriptions
- Impact assessment and risk analysis

### [Format-Specific Security](format-considerations/index.md)

Security considerations for each serialization format:
- [SnakeYAML Security](format-considerations/snakeyaml.md) — **Critical: RCE prevention**
- [Jackson Security](format-considerations/jackson.md) — XXE, polymorphic typing, depth limits
- [Gson Security](format-considerations/gson.md) — Safe defaults and validation

### [Best Practices](best-practices.md)

General security best practices:
- Input validation before migration
- Size and depth limits
- Timeout configuration
- Defense-in-depth checklist

### [Secure Configuration Examples](secure-configuration-examples.md)

Ready-to-use secure configurations:
- Safe `Yaml` setup for SnakeYAML
- Safe `ObjectMapper` setup for Jackson
- Safe `XmlMapper` setup for Jackson XML
- Complete migration service example

### [Spring Security Integration](spring-security-integration.md)

Integrating security with Spring Boot:
- Secure bean configuration
- Request validation filters
- Rate limiting
- Audit logging

## Quick Start: Secure Configuration

### SnakeYAML (Critical)

```java
// ALWAYS use SafeConstructor for untrusted YAML
LoaderOptions options = new LoaderOptions();
options.setMaxAliasesForCollections(50);
options.setNestingDepthLimit(50);
options.setCodePointLimit(3 * 1024 * 1024);

Yaml safeYaml = new Yaml(new SafeConstructor(options));
Object data = safeYaml.load(untrustedInput);
Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, data);
```

### Jackson JSON

```java
StreamReadConstraints constraints = StreamReadConstraints.builder()
    .maxNestingDepth(50)
    .maxStringLength(1_000_000)
    .build();

JsonFactory factory = JsonFactory.builder()
    .streamReadConstraints(constraints)
    .build();

ObjectMapper safeMapper = new ObjectMapper(factory);
JsonNode node = safeMapper.readTree(untrustedInput);
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonJsonOps.INSTANCE, node);
```

### Jackson XML (XXE Prevention)

```java
XMLInputFactory xmlFactory = XMLInputFactory.newFactory();
xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

XmlMapper safeMapper = XmlMapper.builder(
    XmlFactory.builder().xmlInputFactory(xmlFactory).build()
).build();
JsonNode node = safeMapper.readTree(untrustedInput);
Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, node);
```

## Related

- [Codec Overview](../codec/index.md)
- [YAML Support](../codec/yaml.md)
- [XML Support](../codec/xml.md)
- [JSON Support](../codec/json.md)
