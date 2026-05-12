# Secure Configuration Examples

This document provides ready-to-use secure configurations for all supported formats. Copy and adapt these examples for your application.

## Quick Reference

| Format       | Primary Risk              | Required Configuration              |
|--------------|---------------------------|-------------------------------------|
| SnakeYAML    | RCE                       | `SafeConstructor` + `LoaderOptions` |
| Jackson JSON | Polymorphic typing, depth | `StreamReadConstraints`             |
| Jackson XML  | XXE                       | Disable external entities           |
| Jackson YAML | Depth                     | `StreamReadConstraints`             |
| Gson         | Large payloads            | Pre-validation                      |

---

## SnakeYAML Secure Configuration

### Basic Secure Setup

```java
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;

public class SecureSnakeYamlConfig {

    /**
     * Creates a secure Yaml instance for parsing untrusted input.
     */
    public static Yaml createSecureYaml() {
        LoaderOptions options = new LoaderOptions();

        // Prevent Billion Laughs (alias expansion attack)
        options.setMaxAliasesForCollections(50);

        // Prevent stack overflow from deep nesting
        options.setNestingDepthLimit(50);

        // Limit input size (3MB default)
        options.setCodePointLimit(3 * 1024 * 1024);

        // Reject duplicate keys for data integrity
        options.setAllowDuplicateKeys(false);

        // Use SafeConstructor to prevent arbitrary class instantiation
        return new Yaml(new SafeConstructor(options));
    }

    /**
     * Parses untrusted YAML securely and returns a Dynamic.
     */
    public static Dynamic<Object> parseSecurely(String yaml) {
        Yaml safeYaml = createSecureYaml();
        Object data = safeYaml.load(yaml);
        return new Dynamic<>(SnakeYamlOps.INSTANCE, data);
    }
}
```

### Usage Example

```java
// Parse untrusted YAML
String untrustedYaml = request.getBody();
Dynamic<Object> dynamic = SecureSnakeYamlConfig.parseSecurely(untrustedYaml);

// Migrate
TaggedDynamic<Object> tagged = new TaggedDynamic<>(TypeReferences.PLAYER, dynamic);
TaggedDynamic<Object> result = fixer.update(tagged, fromVersion, toVersion);
```

---

## Jackson JSON Secure Configuration

### Basic Secure Setup

```java
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;

public class SecureJacksonJsonConfig {

    /**
     * Creates a secure ObjectMapper for parsing untrusted JSON.
     */
    public static ObjectMapper createSecureMapper() {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
            .maxNestingDepth(50)           // Prevent stack overflow
            .maxNumberLength(100)          // Limit number string length
            .maxStringLength(1_000_000)    // 1MB max string
            .maxNameLength(1_000)          // Limit field name length
            .build();

        JsonFactory factory = JsonFactory.builder()
            .streamReadConstraints(constraints)
            .build();

        return new ObjectMapper(factory);
    }

    /**
     * Creates secure JacksonJsonOps instance.
     */
    public static JacksonJsonOps createSecureOps() {
        return new JacksonJsonOps(createSecureMapper());
    }

    /**
     * Parses untrusted JSON securely and returns a Dynamic.
     */
    public static Dynamic<JsonNode> parseSecurely(String json) throws Exception {
        ObjectMapper mapper = createSecureMapper();
        JsonNode node = mapper.readTree(json);
        return new Dynamic<>(JacksonJsonOps.INSTANCE, node);
    }

    /**
     * Parses untrusted JSON securely with custom ops.
     */
    public static Dynamic<JsonNode> parseSecurely(byte[] json) throws Exception {
        JacksonJsonOps ops = createSecureOps();
        JsonNode node = ops.mapper().readTree(json);
        return new Dynamic<>(ops, node);
    }
}
```

### Usage Example

```java
// Parse untrusted JSON
byte[] untrustedJson = request.getBodyAsBytes();
Dynamic<JsonNode> dynamic = SecureJacksonJsonConfig.parseSecurely(untrustedJson);

// Migrate
TaggedDynamic<JsonNode> tagged = new TaggedDynamic<>(TypeReferences.CONFIG, dynamic);
TaggedDynamic<JsonNode> result = fixer.update(tagged, fromVersion, toVersion);
```

---

## Jackson XML Secure Configuration (XXE Prevention)

### Basic Secure Setup

```java
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps;

import javax.xml.stream.XMLInputFactory;

public class SecureJacksonXmlConfig {

    /**
     * Creates a secure XmlMapper with XXE prevention.
     */
    public static XmlMapper createSecureMapper() {
        // Configure secure XMLInputFactory
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

        // Disable external entities (XXE prevention)
        xmlInputFactory.setProperty(
            XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        // Disable DTD processing
        xmlInputFactory.setProperty(
            XMLInputFactory.SUPPORT_DTD, false);

        // Disable entity replacement
        xmlInputFactory.setProperty(
            XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

        // Configure read constraints
        StreamReadConstraints constraints = StreamReadConstraints.builder()
            .maxNestingDepth(50)
            .maxStringLength(1_000_000)
            .build();

        // Build secure factory
        XmlFactory factory = XmlFactory.builder()
            .xmlInputFactory(xmlInputFactory)
            .streamReadConstraints(constraints)
            .build();

        return XmlMapper.builder(factory).build();
    }

    /**
     * Creates secure JacksonXmlOps instance.
     */
    public static JacksonXmlOps createSecureOps() {
        return new JacksonXmlOps(createSecureMapper());
    }

    /**
     * Parses untrusted XML securely and returns a Dynamic.
     */
    public static Dynamic<JsonNode> parseSecurely(String xml) throws Exception {
        XmlMapper mapper = createSecureMapper();
        JsonNode node = mapper.readTree(xml);
        return new Dynamic<>(new JacksonXmlOps(mapper), node);
    }
}
```

### Usage Example

```java
// Parse untrusted XML
String untrustedXml = request.getBody();
Dynamic<JsonNode> dynamic = SecureJacksonXmlConfig.parseSecurely(untrustedXml);

// Migrate
TaggedDynamic<JsonNode> tagged = new TaggedDynamic<>(TypeReferences.SETTINGS, dynamic);
TaggedDynamic<JsonNode> result = fixer.update(tagged, fromVersion, toVersion);
```

---

## Jackson YAML Secure Configuration

### Basic Secure Setup

```java
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps;

public class SecureJacksonYamlConfig {

    /**
     * Creates a secure YAMLMapper for parsing untrusted input.
     */
    public static YAMLMapper createSecureMapper() {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
            .maxNestingDepth(50)
            .maxStringLength(1_000_000)
            .build();

        YAMLFactory factory = YAMLFactory.builder()
            .streamReadConstraints(constraints)
            .build();

        return new YAMLMapper(factory);
    }

    /**
     * Creates secure JacksonYamlOps instance.
     */
    public static JacksonYamlOps createSecureOps() {
        return new JacksonYamlOps(createSecureMapper());
    }

    /**
     * Parses untrusted YAML securely and returns a Dynamic.
     */
    public static Dynamic<JsonNode> parseSecurely(String yaml) throws Exception {
        YAMLMapper mapper = createSecureMapper();
        JsonNode node = mapper.readTree(yaml);
        return new Dynamic<>(new JacksonYamlOps(mapper), node);
    }
}
```

---

## Gson Secure Configuration

### Basic Secure Setup with Validation

```java
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;

public class SecureGsonConfig {

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_DEPTH = 50;

    /**
     * Creates a Gson instance (safe by default).
     */
    public static Gson createGson() {
        return new GsonBuilder()
            .disableHtmlEscaping()
            .create();
    }

    /**
     * Parses untrusted JSON securely with size and depth validation.
     */
    public static Dynamic<JsonElement> parseSecurely(String json) {
        // Validate size
        if (json.length() > MAX_SIZE) {
            throw new SecurityException("JSON exceeds maximum size of " + MAX_SIZE);
        }

        // Parse
        JsonElement element = JsonParser.parseString(json);

        // Validate depth
        validateDepth(element, 0);

        return new Dynamic<>(GsonOps.INSTANCE, element);
    }

    private static void validateDepth(JsonElement element, int depth) {
        if (depth > MAX_DEPTH) {
            throw new SecurityException("JSON exceeds maximum depth of " + MAX_DEPTH);
        }

        if (element.isJsonObject()) {
            element.getAsJsonObject().entrySet()
                .forEach(e -> validateDepth(e.getValue(), depth + 1));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray()
                .forEach(e -> validateDepth(e, depth + 1));
        }
    }
}
```

---

## Complete Migration Service

A complete service combining all security measures:

```java
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.AetherDataFixer;
import de.splatgames.aether.datafixers.api.schema.DataVersion;
import de.splatgames.aether.datafixers.api.type.TaggedDynamic;
import de.splatgames.aether.datafixers.api.type.TypeReference;

import java.util.concurrent.*;

public class SecureMigrationService {

    private static final long MAX_PAYLOAD_SIZE = 10 * 1024 * 1024;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final AetherDataFixer fixer;
    private final ExecutorService executor;

    public SecureMigrationService(AetherDataFixer fixer) {
        this.fixer = fixer;
        this.executor = Executors.newCachedThreadPool();
    }

    /**
     * Migrates untrusted JSON using Jackson.
     */
    public TaggedDynamic<JsonNode> migrateJson(
            byte[] untrustedJson,
            TypeReference type,
            DataVersion from,
            DataVersion to) throws Exception {

        validateSize(untrustedJson);
        Dynamic<JsonNode> dynamic = SecureJacksonJsonConfig.parseSecurely(untrustedJson);
        return migrateWithTimeout(new TaggedDynamic<>(type, dynamic), from, to);
    }

    /**
     * Migrates untrusted YAML using SnakeYAML.
     */
    public TaggedDynamic<Object> migrateYaml(
            String untrustedYaml,
            TypeReference type,
            DataVersion from,
            DataVersion to) throws Exception {

        validateSize(untrustedYaml);
        Dynamic<Object> dynamic = SecureSnakeYamlConfig.parseSecurely(untrustedYaml);
        return migrateWithTimeout(new TaggedDynamic<>(type, dynamic), from, to);
    }

    /**
     * Migrates untrusted XML using Jackson.
     */
    public TaggedDynamic<JsonNode> migrateXml(
            String untrustedXml,
            TypeReference type,
            DataVersion from,
            DataVersion to) throws Exception {

        validateSize(untrustedXml);
        Dynamic<JsonNode> dynamic = SecureJacksonXmlConfig.parseSecurely(untrustedXml);
        return migrateWithTimeout(new TaggedDynamic<>(type, dynamic), from, to);
    }

    private void validateSize(byte[] data) {
        if (data.length > MAX_PAYLOAD_SIZE) {
            throw new PayloadTooLargeException(
                "Payload exceeds maximum size of " + MAX_PAYLOAD_SIZE);
        }
    }

    private void validateSize(String data) {
        if (data.length() > MAX_PAYLOAD_SIZE) {
            throw new PayloadTooLargeException(
                "Payload exceeds maximum size of " + MAX_PAYLOAD_SIZE);
        }
    }

    private <T> TaggedDynamic<T> migrateWithTimeout(
            TaggedDynamic<T> input,
            DataVersion from,
            DataVersion to) throws Exception {

        Future<TaggedDynamic<T>> future = executor.submit(
            () -> fixer.update(input, from, to)
        );

        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new MigrationTimeoutException("Migration timed out after " + TIMEOUT, e);
        } catch (ExecutionException e) {
            throw new MigrationException("Migration failed", e.getCause());
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
```

### Usage

```java
SecureMigrationService service = new SecureMigrationService(fixer);

// Migrate JSON
TaggedDynamic<JsonNode> result = service.migrateJson(
    jsonBytes,
    TypeReferences.PLAYER,
    new DataVersion(100),
    new DataVersion(200)
);

// Migrate YAML
TaggedDynamic<Object> yamlResult = service.migrateYaml(
    yamlString,
    TypeReferences.CONFIG,
    new DataVersion(1),
    new DataVersion(5)
);

// Migrate XML
TaggedDynamic<JsonNode> xmlResult = service.migrateXml(
    xmlString,
    TypeReferences.SETTINGS,
    new DataVersion(1),
    new DataVersion(3)
);
```

---

## Exception Classes

```java
public class PayloadTooLargeException extends SecurityException {
    public PayloadTooLargeException(String message) {
        super(message);
    }
}

public class MigrationTimeoutException extends RuntimeException {
    public MigrationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class MigrationException extends RuntimeException {
    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## Related

- [Best Practices](best-practices.md)
- [SnakeYAML Security](format-considerations/snakeyaml.md)
- [Jackson Security](format-considerations/jackson.md)
- [Gson Security](format-considerations/gson.md)
- [Spring Security Integration](spring-security-integration.md)
