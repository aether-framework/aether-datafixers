# Actuator Integration

The Aether Datafixers Spring Boot Starter provides comprehensive integration with Spring Boot Actuator, offering health indicators, info contributors, and a custom management endpoint for operational visibility.

---

## Prerequisites

Add Spring Boot Actuator to your project:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

## Available Components

| Component        | Endpoint               | Description                                      |
|------------------|------------------------|--------------------------------------------------|
| Health Indicator | `/actuator/health`     | Reports UP/DOWN status for all DataFixer domains |
| Info Contributor | `/actuator/info`       | Adds DataFixer metadata to the info endpoint     |
| Custom Endpoint  | `/actuator/datafixers` | Dedicated endpoint for DataFixer management      |

---

## Health Indicator

The `DataFixerHealthIndicator` monitors the operational status of all registered DataFixer instances.

### Configuration

Expose the health endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: always
```

### Health Check Logic

The indicator checks each domain by:
1. Retrieving the DataFixer from the registry
2. Attempting to read the current version
3. Reporting individual domain status

### Response Examples

#### All Domains Healthy

```bash
GET /actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "dataFixer": {
      "status": "UP",
      "details": {
        "totalDomains": 2,
        "default.status": "UP",
        "default.currentVersion": 200,
        "game.status": "UP",
        "game.currentVersion": 150
      }
    }
  }
}
```

#### Domain Failure

```json
{
  "status": "DOWN",
  "components": {
    "dataFixer": {
      "status": "DOWN",
      "details": {
        "totalDomains": 2,
        "game.status": "DOWN",
        "game.error": "Schema not initialized"
      }
    }
  }
}
```

#### No Domains Registered

```json
{
  "status": "UP",
  "components": {
    "dataFixer": {
      "status": "UNKNOWN",
      "details": {
        "message": "No DataFixers registered"
      }
    }
  }
}
```

### Health Status Semantics

| Status    | Meaning                       |
|-----------|-------------------------------|
| `UP`      | All DataFixers operational    |
| `DOWN`    | At least one DataFixer failed |
| `UNKNOWN` | No DataFixers registered      |

### Kubernetes Integration

Use the health endpoint for liveness/readiness probes:

```yaml
# kubernetes deployment
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

Configure health groups:

```yaml
management:
  endpoint:
    health:
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, dataFixer
```

---

## Info Contributor

The `DataFixerInfoContributor` adds DataFixer metadata to the `/actuator/info` endpoint.

### Configuration

Enable the info endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: info
  info:
    env:
      enabled: true
```

### Response Example

```bash
GET /actuator/info
```

```json
{
  "app": {
    "name": "my-application",
    "version": "1.0.0"
  },
  "aether-datafixers": {
    "domains": 2,
    "domainDetails": {
      "default": {
        "currentVersion": 200
      },
      "game": {
        "currentVersion": 150
      }
    }
  }
}
```

### Error Reporting

If a domain fails to report its version:

```json
{
  "aether-datafixers": {
    "domains": 2,
    "domainDetails": {
      "default": {
        "currentVersion": 200
      },
      "game": {
        "error": "Schema not initialized"
      }
    }
  }
}
```

---

## Custom DataFixer Endpoint

The `DataFixerEndpoint` provides dedicated management capabilities at `/actuator/datafixers`.

### Configuration

Expose the endpoint:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, datafixers
  endpoint:
    datafixers:
      enabled: true
```

### Operations

| HTTP Method | Path                            | Description                 |
|-------------|---------------------------------|-----------------------------|
| GET         | `/actuator/datafixers`          | Summary of all domains      |
| GET         | `/actuator/datafixers/{domain}` | Details for specific domain |

### Summary Response

```bash
GET /actuator/datafixers
```

```json
{
  "domains": {
    "default": {
      "currentVersion": 200,
      "status": "UP"
    },
    "game": {
      "currentVersion": 150,
      "status": "UP"
    },
    "user": {
      "currentVersion": 100,
      "status": "UP"
    }
  }
}
```

### Domain Details Response

```bash
GET /actuator/datafixers/game
```

```json
{
  "domain": "game",
  "currentVersion": 150,
  "status": "UP"
}
```

### Error Response

For a domain with issues:

```json
{
  "domain": "game",
  "currentVersion": -1,
  "status": "DOWN: Schema not initialized"
}
```

### Domain Not Found

```bash
GET /actuator/datafixers/nonexistent
```

Returns HTTP 404 Not Found.

---

## Security Configuration

Actuator endpoints can expose sensitive information. Secure them in production:

### Basic Security

```java
@Configuration
@EnableWebSecurity
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                // Secured endpoints
                .requestMatchers(EndpointRequest.to("datafixers")).hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }
}
```

### Hide Sensitive Details

```yaml
aether:
  datafixers:
    actuator:
      include-schema-details: false
      include-fix-details: false
```

### Disable in Production

```yaml
# application-prod.yml
management:
  endpoint:
    datafixers:
      enabled: false
```

---

## Use Cases

### Version Monitoring

Track deployed schema versions across environments:

```bash
# Check version in staging
curl https://staging.example.com/actuator/datafixers

# Compare with production
curl https://prod.example.com/actuator/datafixers
```

### Deployment Verification

Verify DataFixer configuration after deployment:

```bash
#!/bin/bash
# deploy-verify.sh

HEALTH=$(curl -s http://localhost:8080/actuator/health)
DATAFIXER_STATUS=$(echo $HEALTH | jq -r '.components.dataFixer.status')

if [ "$DATAFIXER_STATUS" != "UP" ]; then
    echo "DataFixer health check failed!"
    exit 1
fi

echo "DataFixer healthy: $(echo $HEALTH | jq '.components.dataFixer.details')"
```

### Alerting

Configure alerts based on health status:

```yaml
# Prometheus alerting rule
groups:
  - name: datafixer
    rules:
      - alert: DataFixerUnhealthy
        expr: up{job="spring-boot"} == 1 and absent(spring_boot_health_status{status="UP",component="dataFixer"})
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "DataFixer health check failing"
          description: "DataFixer has been unhealthy for more than 5 minutes"
```

### Multi-Instance Comparison

Compare versions across service replicas:

```bash
#!/bin/bash
# compare-versions.sh

INSTANCES=("pod-1" "pod-2" "pod-3")
for instance in "${INSTANCES[@]}"; do
    echo "=== $instance ==="
    kubectl exec $instance -- curl -s localhost:8080/actuator/datafixers | jq
done
```

---

## Customizing Actuator Components

### Custom Health Indicator

Extend the default health check:

```java
@Component
public class ExtendedDataFixerHealthIndicator implements HealthIndicator {

    private final DataFixerRegistry registry;
    private final MigrationMetrics metrics;

    public ExtendedDataFixerHealthIndicator(
            DataFixerRegistry registry,
            @Nullable MigrationMetrics metrics) {
        this.registry = registry;
        this.metrics = metrics;
    }

    @Override
    public Health health() {
        if (registry.isEmpty()) {
            return Health.unknown()
                .withDetail("message", "No DataFixers registered")
                .build();
        }

        Health.Builder builder = Health.up();
        builder.withDetail("totalDomains", registry.size());

        for (Map.Entry<String, AetherDataFixer> entry : registry.getAll().entrySet()) {
            String domain = entry.getKey();
            try {
                int version = entry.getValue().currentVersion().getVersion();
                builder.withDetail(domain + ".version", version);
                builder.withDetail(domain + ".status", "UP");

                // Add metrics info if available
                if (metrics != null) {
                    // Custom metric retrieval...
                }
            } catch (Exception e) {
                return Health.down()
                    .withDetail(domain + ".status", "DOWN")
                    .withDetail(domain + ".error", e.getMessage())
                    .build();
            }
        }

        return builder.build();
    }
}
```

### Custom Endpoint Extension

Add operations to the DataFixer endpoint:

```java
@Component
@EndpointWebExtension(endpoint = DataFixerEndpoint.class)
public class DataFixerEndpointExtension {

    private final DataFixerRegistry registry;
    private final MigrationMetrics metrics;

    @ReadOperation
    public Map<String, Object> extendedInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        // Add migration statistics
        info.put("totalDomains", registry.size());
        info.put("domains", registry.getDomains());

        // Add custom metrics if available
        if (metrics != null) {
            info.put("metricsEnabled", true);
        }

        return info;
    }
}
```

---

## Related Documentation

- [Configuration Reference](configuration.md) — Actuator property settings
- [Metrics Integration](metrics.md) — Micrometer metrics for dashboards
- [Multi-Domain Setup](multi-domain.md) — Domain-specific health checks
