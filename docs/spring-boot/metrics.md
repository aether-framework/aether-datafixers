# Metrics Integration

The Aether Datafixers Spring Boot Starter provides comprehensive Micrometer integration for monitoring migration operations. Metrics are automatically recorded when using the `MigrationService`.

---

## Prerequisites

Add Micrometer to your project (typically included with Spring Boot Actuator):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

For Prometheus export:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## Recorded Metrics

All metrics use the prefix `aether.datafixers.migrations`.

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `.success` | Counter | `domain` | Total successful migrations |
| `.failure` | Counter | `domain`, `error_type` | Total failed migrations |
| `.duration` | Timer | `domain` | Migration execution time |
| `.version.span` | Distribution Summary | `domain` | Version span distribution |

---

## Metric Tags

### `domain`

The DataFixer domain name (e.g., "default", "game", "user").

### `error_type`

For failure metrics only. The simple class name of the exception (e.g., "IllegalStateException", "MigrationException").

---

## Metric Details

### Success Counter

Incremented for each successful migration.

```
aether_datafixers_migrations_success_total{domain="game"} 42
```

### Failure Counter

Incremented for each failed migration, tagged with error type.

```
aether_datafixers_migrations_failure_total{domain="game",error_type="IllegalStateException"} 3
aether_datafixers_migrations_failure_total{domain="game",error_type="MigrationException"} 1
```

### Duration Timer

Records execution time for all migrations (success and failure).

```
aether_datafixers_migrations_duration_seconds_count{domain="game"} 45
aether_datafixers_migrations_duration_seconds_sum{domain="game"} 2.34
aether_datafixers_migrations_duration_seconds_max{domain="game"} 0.523
```

With histograms (for percentiles):

```
aether_datafixers_migrations_duration_seconds_bucket{domain="game",le="0.01"} 10
aether_datafixers_migrations_duration_seconds_bucket{domain="game",le="0.05"} 35
aether_datafixers_migrations_duration_seconds_bucket{domain="game",le="0.1"} 42
aether_datafixers_migrations_duration_seconds_bucket{domain="game",le="+Inf"} 45
```

### Version Span Distribution

Records the absolute difference between target and source versions.

```
aether_datafixers_migrations_version_span_count{domain="game"} 42
aether_datafixers_migrations_version_span_sum{domain="game"} 4200
aether_datafixers_migrations_version_span_max{domain="game"} 150
```

---

## Configuration

### Enable/Disable Metrics

```yaml
aether:
  datafixers:
    metrics:
      timing: true      # Enable duration timer
      counting: true    # Enable success/failure counters
```

### Custom Domain Tag

```yaml
aether:
  datafixers:
    metrics:
      domain-tag: datafixer_domain
```

Results in:

```
aether_datafixers_migrations_success_total{datafixer_domain="game"} 42
```

---

## PromQL Queries

### Success Rate

```promql
# Success rate over the last hour
sum(rate(aether_datafixers_migrations_success_total[1h])) by (domain)
```

### Failure Rate

```promql
# Failure rate by domain
sum(rate(aether_datafixers_migrations_failure_total[1h])) by (domain)

# Failure rate by error type
sum(rate(aether_datafixers_migrations_failure_total[1h])) by (domain, error_type)
```

### Success/Failure Ratio

```promql
# Success percentage per domain
(
  sum(rate(aether_datafixers_migrations_success_total[1h])) by (domain)
  /
  (
    sum(rate(aether_datafixers_migrations_success_total[1h])) by (domain)
    + sum(rate(aether_datafixers_migrations_failure_total[1h])) by (domain)
  )
) * 100
```

### Average Duration

```promql
# Average migration duration by domain
rate(aether_datafixers_migrations_duration_seconds_sum[5m])
  / rate(aether_datafixers_migrations_duration_seconds_count[5m])
```

### Percentile Duration

```promql
# 95th percentile migration duration
histogram_quantile(0.95,
  rate(aether_datafixers_migrations_duration_seconds_bucket[5m])
)

# 99th percentile migration duration
histogram_quantile(0.99,
  rate(aether_datafixers_migrations_duration_seconds_bucket[5m])
)
```

### Version Span Analysis

```promql
# Average version span (indicates data age)
rate(aether_datafixers_migrations_version_span_sum[1h])
  / rate(aether_datafixers_migrations_version_span_count[1h])

# Maximum version span (outliers)
max(aether_datafixers_migrations_version_span_max) by (domain)
```

### Throughput

```promql
# Migrations per second
sum(rate(aether_datafixers_migrations_success_total[5m]))
  + sum(rate(aether_datafixers_migrations_failure_total[5m]))
```

---

## Grafana Dashboard

### Recommended Panels

#### 1. Migration Success/Failure Rate

```yaml
Panel: Time Series
Title: Migration Success/Failure Rate

Queries:
- Legend: Success ({{domain}})
  Query: sum(rate(aether_datafixers_migrations_success_total[5m])) by (domain)

- Legend: Failure ({{domain}})
  Query: sum(rate(aether_datafixers_migrations_failure_total[5m])) by (domain)
```

#### 2. Migration Duration (p95)

```yaml
Panel: Time Series
Title: Migration Duration (p95)

Query: histogram_quantile(0.95,
         sum(rate(aether_datafixers_migrations_duration_seconds_bucket[5m])) by (le, domain)
       )

Unit: seconds
```

#### 3. Success Rate Gauge

```yaml
Panel: Gauge
Title: Migration Success Rate

Query: (
  sum(rate(aether_datafixers_migrations_success_total[1h]))
  / (
    sum(rate(aether_datafixers_migrations_success_total[1h]))
    + sum(rate(aether_datafixers_migrations_failure_total[1h]))
  )
) * 100

Unit: percent
Thresholds:
  - 99: green
  - 95: yellow
  - 0: red
```

#### 4. Failure Breakdown by Error Type

```yaml
Panel: Pie Chart
Title: Failures by Error Type

Query: sum(increase(aether_datafixers_migrations_failure_total[24h])) by (error_type)
```

#### 5. Version Span Distribution

```yaml
Panel: Histogram
Title: Version Span Distribution

Query: sum(rate(aether_datafixers_migrations_version_span_bucket[1h])) by (le)
```

### Sample Dashboard JSON

```json
{
  "title": "Aether DataFixers",
  "panels": [
    {
      "title": "Migration Rate",
      "type": "graph",
      "targets": [
        {
          "expr": "sum(rate(aether_datafixers_migrations_success_total[5m])) by (domain)",
          "legendFormat": "Success ({{domain}})"
        },
        {
          "expr": "sum(rate(aether_datafixers_migrations_failure_total[5m])) by (domain)",
          "legendFormat": "Failure ({{domain}})"
        }
      ]
    },
    {
      "title": "Duration Percentiles",
      "type": "graph",
      "targets": [
        {
          "expr": "histogram_quantile(0.50, sum(rate(aether_datafixers_migrations_duration_seconds_bucket[5m])) by (le, domain))",
          "legendFormat": "p50 ({{domain}})"
        },
        {
          "expr": "histogram_quantile(0.95, sum(rate(aether_datafixers_migrations_duration_seconds_bucket[5m])) by (le, domain))",
          "legendFormat": "p95 ({{domain}})"
        },
        {
          "expr": "histogram_quantile(0.99, sum(rate(aether_datafixers_migrations_duration_seconds_bucket[5m])) by (le, domain))",
          "legendFormat": "p99 ({{domain}})"
        }
      ]
    }
  ]
}
```

---

## Alerting Rules

### High Failure Rate

```yaml
groups:
  - name: aether-datafixers
    rules:
      - alert: DataFixerHighFailureRate
        expr: |
          (
            sum(rate(aether_datafixers_migrations_failure_total[5m])) by (domain)
            / (
              sum(rate(aether_datafixers_migrations_success_total[5m])) by (domain)
              + sum(rate(aether_datafixers_migrations_failure_total[5m])) by (domain)
            )
          ) > 0.01
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High DataFixer failure rate in domain {{ $labels.domain }}"
          description: "Failure rate is {{ $value | humanizePercentage }} over the last 5 minutes"
```

### Slow Migrations

```yaml
      - alert: DataFixerSlowMigrations
        expr: |
          histogram_quantile(0.95,
            sum(rate(aether_datafixers_migrations_duration_seconds_bucket[5m])) by (le, domain)
          ) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow migrations in domain {{ $labels.domain }}"
          description: "p95 migration duration is {{ $value | humanizeDuration }}"
```

### Large Version Spans

```yaml
      - alert: DataFixerLargeVersionSpan
        expr: |
          (
            rate(aether_datafixers_migrations_version_span_sum[1h])
            / rate(aether_datafixers_migrations_version_span_count[1h])
          ) > 100
        for: 1h
        labels:
          severity: info
        annotations:
          summary: "Large average version span in domain {{ $labels.domain }}"
          description: "Average span is {{ $value }} versions, indicating very outdated data"
```

### No Migrations

```yaml
      - alert: DataFixerNoActivity
        expr: |
          sum(rate(aether_datafixers_migrations_success_total[1h])) == 0
          and sum(rate(aether_datafixers_migrations_failure_total[1h])) == 0
        for: 1h
        labels:
          severity: info
        annotations:
          summary: "No migration activity detected"
          description: "No migrations have occurred in the last hour"
```

---

## Memory Considerations

Metrics are cached per domain (and per error type for failures). Memory usage scales with:

| Metric Type | Cardinality |
|-------------|-------------|
| Success counters | 1 per domain |
| Failure counters | 1 per (domain × error_type) |
| Duration timers | 1 per domain |
| Version span summaries | 1 per domain |

**Recommendations**:
- Limit the number of domains to a reasonable count
- Monitor unique error types to prevent cardinality explosion
- Consider disabling version span tracking if not needed

---

## Exporting Metrics

### Prometheus

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

Access at: `GET /actuator/prometheus`

### Datadog

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-datadog</artifactId>
</dependency>
```

```yaml
management:
  metrics:
    export:
      datadog:
        enabled: true
        api-key: ${DATADOG_API_KEY}
```

### CloudWatch

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-cloudwatch2</artifactId>
</dependency>
```

---

## Custom Metrics

Extend the `MigrationMetrics` class for custom tracking:

```java
@Component
public class ExtendedMigrationMetrics extends MigrationMetrics {

    private final Counter customCounter;

    public ExtendedMigrationMetrics(MeterRegistry registry) {
        super(registry);
        this.customCounter = Counter.builder("aether.datafixers.custom.migrations")
            .description("Custom migration counter")
            .register(registry);
    }

    @Override
    public void recordSuccess(String domain, int fromVersion, int toVersion, Duration duration) {
        super.recordSuccess(domain, fromVersion, toVersion, duration);

        // Custom tracking
        if (toVersion - fromVersion > 50) {
            customCounter.increment();
        }
    }
}
```

---

## Related Documentation

- [Configuration Reference](configuration.md) — Metrics property settings
- [Actuator Integration](actuator.md) — Health checks and endpoints
- [MigrationService API](migration-service.md) — Automatic metrics recording
