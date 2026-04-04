# Spring Security Integration

This guide covers integrating secure Aether Datafixers usage with Spring Boot and Spring Security.

## Overview

When using the `aether-datafixers-spring-boot-starter`, additional security measures should be implemented at the Spring level:

1. **Secure Bean Configuration** - Configure secure parsers as Spring beans
2. **Request Validation** - Validate payloads before they reach migration endpoints
3. **Rate Limiting** - Prevent abuse of migration endpoints
4. **Audit Logging** - Track migration attempts for security monitoring

---

## Secure Bean Configuration

### Secure Parser Beans

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import javax.xml.stream.XMLInputFactory;

@Configuration
public class SecureDataFixerConfig {

    @Bean
    public Yaml secureYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(50);
        options.setNestingDepthLimit(50);
        options.setCodePointLimit(3 * 1024 * 1024);
        options.setAllowDuplicateKeys(false);
        return new Yaml(new SafeConstructor(options));
    }

    @Bean
    public ObjectMapper secureJsonMapper() {
        StreamReadConstraints constraints = StreamReadConstraints.builder()
            .maxNestingDepth(50)
            .maxNumberLength(100)
            .maxStringLength(1_000_000)
            .build();

        JsonFactory factory = JsonFactory.builder()
            .streamReadConstraints(constraints)
            .build();

        return new ObjectMapper(factory);
    }

    @Bean
    public XmlMapper secureXmlMapper() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        StreamReadConstraints constraints = StreamReadConstraints.builder()
            .maxNestingDepth(50)
            .maxStringLength(1_000_000)
            .build();

        XmlFactory factory = XmlFactory.builder()
            .xmlInputFactory(xmlInputFactory)
            .streamReadConstraints(constraints)
            .build();

        return XmlMapper.builder(factory).build();
    }
}
```

### Secure DynamicOps Beans

```java
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
import de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps;

@Configuration
public class SecureDynamicOpsConfig {

    @Bean
    public JacksonJsonOps secureJsonOps(ObjectMapper secureJsonMapper) {
        return new JacksonJsonOps(secureJsonMapper);
    }

    @Bean
    public JacksonXmlOps secureXmlOps(XmlMapper secureXmlMapper) {
        return new JacksonXmlOps(secureXmlMapper);
    }
}
```

---

## Request Validation Filter

### Payload Size Validation

```java
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PayloadSizeValidationFilter extends OncePerRequestFilter {

    private static final long MAX_PAYLOAD_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        // Check Content-Length header
        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_PAYLOAD_SIZE) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.getWriter().write("Payload exceeds maximum size");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter migration endpoints
        return !request.getRequestURI().startsWith("/api/migrate");
    }
}
```

### Content-Type Validation

```java
import org.springframework.http.MediaType;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ContentTypeValidationFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        MediaType.APPLICATION_JSON_VALUE,
        "application/yaml",
        "text/yaml",
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_XML_VALUE
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String contentType = request.getContentType();
        if (contentType != null) {
            String baseType = contentType.split(";")[0].trim().toLowerCase();
            if (!ALLOWED_CONTENT_TYPES.contains(baseType)) {
                response.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
                response.getWriter().write("Unsupported content type");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/migrate") ||
               !"POST".equalsIgnoreCase(request.getMethod());
    }
}
```

---

## Rate Limiting

### Using Resilience4j

Add dependency:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
```

Configuration:

```yaml
# application.yml
resilience4j:
  ratelimiter:
    instances:
      migration:
        limitForPeriod: 10
        limitRefreshPeriod: 1s
        timeoutDuration: 0
```

Controller:

```java
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@RestController
@RequestMapping("/api/migrate")
public class MigrationController {

    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/json")
    @RateLimiter(name = "migration", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<MigrationResult> migrateJson(
            @RequestBody byte[] data,
            @RequestParam int fromVersion,
            @RequestParam int toVersion,
            @RequestParam String type) {

        MigrationResult result = migrationService
            .migrate(data)
            .from(fromVersion)
            .to(toVersion)
            .execute();

        return ResponseEntity.ok(result);
    }

    public ResponseEntity<MigrationResult> rateLimitFallback(
            byte[] data, int fromVersion, int toVersion, String type, Throwable t) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(MigrationResult.error("Rate limit exceeded. Please try again later."));
    }
}
```

### Using Bucket4j

```java
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

@Component
public class RateLimitingService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String userId) {
        return buckets.computeIfAbsent(userId, this::createBucket);
    }

    private Bucket createBucket(String userId) {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    public boolean tryConsume(String userId) {
        return resolveBucket(userId).tryConsume(1);
    }
}
```

---

## Audit Logging

### Audit Aspect

```java
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MigrationAuditAspect {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("MIGRATION_AUDIT");

    @Around("execution(* de.splatgames.aether.datafixers.spring.service.MigrationService.migrate(..))")
    public Object auditMigration(ProceedingJoinPoint joinPoint) throws Throwable {
        String user = getCurrentUser();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            AUDIT_LOG.info("MIGRATION_SUCCESS user={} duration={}ms", user, duration);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            AUDIT_LOG.warn("MIGRATION_FAILURE user={} duration={}ms error={}",
                user, duration, e.getMessage());

            throw e;
        }
    }

    private String getCurrentUser() {
        try {
            return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
```

### Structured Logging with MDC

```java
import org.slf4j.MDC;

@Component
public class MigrationAuditAspect {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationAuditAspect.class);

    @Around("execution(* MigrationService.migrate(..))")
    public Object auditMigration(ProceedingJoinPoint joinPoint) throws Throwable {
        String migrationId = UUID.randomUUID().toString();

        MDC.put("migrationId", migrationId);
        MDC.put("user", getCurrentUser());
        MDC.put("clientIp", getClientIp());

        try {
            Object result = joinPoint.proceed();
            LOG.info("Migration completed successfully");
            return result;
        } catch (SecurityException e) {
            LOG.warn("Migration blocked: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Migration failed: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

---

## Secure Migration Service

### Complete Integration Example

```java
import de.splatgames.aether.datafixers.spring.service.MigrationService;
import de.splatgames.aether.datafixers.spring.service.MigrationResult;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

@Service
public class SecureMigrationService {

    private static final long MAX_SIZE = 10 * 1024 * 1024;

    private final MigrationService migrationService;
    private final Yaml secureYaml;
    private final ObjectMapper secureJsonMapper;
    private final XmlMapper secureXmlMapper;

    public SecureMigrationService(
            MigrationService migrationService,
            Yaml secureYaml,
            ObjectMapper secureJsonMapper,
            XmlMapper secureXmlMapper) {
        this.migrationService = migrationService;
        this.secureYaml = secureYaml;
        this.secureJsonMapper = secureJsonMapper;
        this.secureXmlMapper = secureXmlMapper;
    }

    public MigrationResult migrateJsonSecurely(
            byte[] input,
            int fromVersion,
            int toVersion) {

        validateSize(input);

        try {
            JsonNode node = secureJsonMapper.readTree(input);
            TaggedDynamic<JsonNode> tagged = new TaggedDynamic<>(
                TypeReferences.DATA,
                new Dynamic<>(JacksonJsonOps.INSTANCE, node)
            );

            return migrationService
                .migrate(tagged)
                .from(fromVersion)
                .to(toVersion)
                .execute();
        } catch (Exception e) {
            throw new MigrationException("JSON migration failed", e);
        }
    }

    public MigrationResult migrateYamlSecurely(
            String input,
            int fromVersion,
            int toVersion) {

        validateSize(input);

        Object data = secureYaml.load(input);
        TaggedDynamic<Object> tagged = new TaggedDynamic<>(
            TypeReferences.DATA,
            new Dynamic<>(SnakeYamlOps.INSTANCE, data)
        );

        return migrationService
            .migrate(tagged)
            .from(fromVersion)
            .to(toVersion)
            .execute();
    }

    public MigrationResult migrateXmlSecurely(
            String input,
            int fromVersion,
            int toVersion) {

        validateSize(input);

        try {
            JsonNode node = secureXmlMapper.readTree(input);
            TaggedDynamic<JsonNode> tagged = new TaggedDynamic<>(
                TypeReferences.DATA,
                new Dynamic<>(new JacksonXmlOps(secureXmlMapper), node)
            );

            return migrationService
                .migrate(tagged)
                .from(fromVersion)
                .to(toVersion)
                .execute();
        } catch (Exception e) {
            throw new MigrationException("XML migration failed", e);
        }
    }

    private void validateSize(byte[] input) {
        if (input.length > MAX_SIZE) {
            throw new PayloadTooLargeException("Input exceeds maximum size");
        }
    }

    private void validateSize(String input) {
        if (input.length() > MAX_SIZE) {
            throw new PayloadTooLargeException("Input exceeds maximum size");
        }
    }
}
```

---

## Exception Handling

### Global Exception Handler

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MigrationExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationExceptionHandler.class);

    @ExceptionHandler(PayloadTooLargeException.class)
    public ResponseEntity<ErrorResponse> handlePayloadTooLarge(PayloadTooLargeException e) {
        LOG.warn("Payload too large: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ErrorResponse("PAYLOAD_TOO_LARGE", "Payload exceeds maximum size"));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
        LOG.warn("Security violation: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("SECURITY_VIOLATION", "Invalid input detected"));
    }

    @ExceptionHandler(MigrationTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(MigrationTimeoutException e) {
        LOG.error("Migration timeout: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.GATEWAY_TIMEOUT)
            .body(new ErrorResponse("MIGRATION_TIMEOUT", "Migration timed out"));
    }

    @ExceptionHandler(MigrationException.class)
    public ResponseEntity<ErrorResponse> handleMigrationError(MigrationException e) {
        LOG.error("Migration failed: {}", e.getMessage());
        // Don't expose internal error details
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("MIGRATION_FAILED", "Migration failed"));
    }

    public record ErrorResponse(String code, String message) {}
}
```

---

## Configuration Properties

### Security Properties

```yaml
# application.yml
aether:
  datafixers:
    security:
      max-payload-size: 10485760  # 10MB
      max-nesting-depth: 50
      migration-timeout: 30s
      rate-limit:
        requests-per-minute: 60
```

```java
@ConfigurationProperties(prefix = "aether.datafixers.security")
public record SecurityProperties(
    long maxPayloadSize,
    int maxNestingDepth,
    Duration migrationTimeout,
    RateLimitProperties rateLimit
) {
    public record RateLimitProperties(int requestsPerMinute) {}
}
```

---

## Health Indicator

### Security Health Check

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class SecureMigrationHealthIndicator implements HealthIndicator {

    private final Yaml secureYaml;
    private final ObjectMapper secureJsonMapper;

    public SecureMigrationHealthIndicator(Yaml secureYaml, ObjectMapper secureJsonMapper) {
        this.secureYaml = secureYaml;
        this.secureJsonMapper = secureJsonMapper;
    }

    @Override
    public Health health() {
        try {
            // Verify secure configurations are active
            verifyYamlSecurity();
            verifyJacksonSecurity();

            return Health.up()
                .withDetail("yaml", "SafeConstructor enabled")
                .withDetail("jackson", "StreamReadConstraints configured")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }

    private void verifyYamlSecurity() {
        // Attempt to parse a malicious payload should fail
        String malicious = "!!java.lang.ProcessBuilder [[\"test\"]]";
        try {
            secureYaml.load(malicious);
            throw new IllegalStateException("SafeConstructor not configured!");
        } catch (org.yaml.snakeyaml.constructor.ConstructorException e) {
            // Expected - SafeConstructor is working
        }
    }

    private void verifyJacksonSecurity() {
        // Verify constraints are configured
        JsonFactory factory = secureJsonMapper.getFactory();
        StreamReadConstraints constraints = factory.streamReadConstraints();
        if (constraints.getMaxNestingDepth() > 100) {
            throw new IllegalStateException("Nesting depth limit too high");
        }
    }
}
```

---

## Related

- [Best Practices](best-practices.md)
- [Secure Configuration Examples](secure-configuration-examples.md)
- [Spring Boot Overview](../spring-boot/index.md)
- [MigrationService API](../spring-boot/migration-service.md)
