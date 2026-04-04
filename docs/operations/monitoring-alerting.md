# Monitoring & Alerting

Production monitoring setup for Aether Datafixers using Micrometer, Prometheus, and Grafana.

## Metric Quick Reference

| Metric                                      | Type         | Tags                   | Alert Threshold | Description           |
|---------------------------------------------|--------------|------------------------|-----------------|-----------------------|
| `aether.datafixers.migrations.success`      | Counter      | `domain`               | -               | Successful migrations |
| `aether.datafixers.migrations.failure`      | Counter      | `domain`, `error_type` | > 0/min         | Failed migrations     |
| `aether.datafixers.migrations.duration`     | Timer        | `domain`               | p99 > 1s        | Execution time        |
| `aether.datafixers.migrations.version.span` | Distribution | `domain`               | avg > 50        | Version distance      |

All metrics use the prefix `aether.datafixers.migrations`.

---

## Metric Details

### Success Counter

Tracks total successful migrations per domain.

**Prometheus format:**
```
aether_datafixers_migrations_success_total{domain="game"} 1234
```

**Use cases:**
- Calculate success rate
- Monitor throughput
- Track migration activity

### Failure Counter

Tracks failed migrations with error type breakdown.

**Prometheus format:**
```
aether_datafixers_migrations_failure_total{domain="game",error_type="FixException"} 5
aether_datafixers_migrations_failure_total{domain="game",error_type="DecodeException"} 2
```

**Tags:**
- `domain` - DataFixer domain name
- `error_type` - Exception class simple name

### Duration Timer

Tracks execution time distribution (includes both success and failure).

**Prometheus format:**
```
aether_datafixers_migrations_duration_seconds_count{domain="game"} 1239
aether_datafixers_migrations_duration_seconds_sum{domain="game"} 185.7
aether_datafixers_migrations_duration_seconds_bucket{domain="game",le="0.01"} 500
aether_datafixers_migrations_duration_seconds_bucket{domain="game",le="0.1"} 1100
aether_datafixers_migrations_duration_seconds_bucket{domain="game",le="1.0"} 1230
aether_datafixers_migrations_duration_seconds_bucket{domain="game",le="+Inf"} 1239
```

### Version Span Distribution

Tracks the distance between source and target versions (indicates data age).

**Prometheus format:**
```
aether_datafixers_migrations_version_span_count{domain="game"} 1234
aether_datafixers_migrations_version_span_sum{domain="game"} 45600
aether_datafixers_migrations_version_span_max{domain="game"} 150
```

---

## Recommended Alert Thresholds

### Critical Alerts (Page On-Call)

| Alert                  | Condition           | Duration | Action                  |
|------------------------|---------------------|----------|-------------------------|
| High Failure Rate      | > 5% failures       | 5m       | Immediate investigation |
| All Migrations Failing | 100% failure rate   | 2m       | Emergency response      |
| Service Down           | No metrics reported | 5m       | Check service health    |

### Warning Alerts (Notify Team)

| Alert                 | Condition     | Duration | Action                            |
|-----------------------|---------------|----------|-----------------------------------|
| Elevated Failure Rate | > 1% failures | 5m       | Investigate during business hours |
| Slow Migrations       | p95 > 1s      | 5m       | Performance review                |
| Very Slow Migrations  | p99 > 5s      | 5m       | Profile and optimize              |

### Informational Alerts (Dashboard/Log)

| Alert              | Condition      | Duration | Action                      |
|--------------------|----------------|----------|-----------------------------|
| Large Version Span | avg span > 50  | 1h       | Review data freshness       |
| Very Large Span    | max span > 200 | 1h       | Identify stale data sources |
| No Activity        | 0 migrations   | 1h       | Verify expected behavior    |

---

## Prometheus Alert Rules

### Complete Alert Configuration

```yaml
groups:
  - name: aether-datafixers-critical
    rules:
      # High failure rate - immediate attention
      - alert: DataFixerHighFailureRate
        expr: |
          (
            sum(rate(aether_datafixers_migrations_failure_total[5m])) by (domain)
            / (
              sum(rate(aether_datafixers_migrations_success_total[5m])) by (domain)
              + sum(rate(aether_datafixers_migrations_failure_total[5m])) by (domain)
            )
          ) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Critical: DataFixer failure rate > 5% in domain {{ $labels.domain }}"
          description: "Failure rate is {{ $value | humanizePercentage }}. Check error logs and metrics."
          runbook_url: "https://docs.example.com/runbooks/datafixer-high-failure"

      # All migrations failing
      - alert: DataFixerAllFailing
        expr: |
          sum(rate(aether_datafixers_migrations_success_total[2m])) by (domain) == 0
          and sum(rate(aether_datafixers_migrations_failure_total[2m])) by (domain) > 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Critical: All migrations failing in domain {{ $labels.domain }}"
          description: "Zero successful migrations with active failures. Immediate attention required."
          runbook_url: "https://docs.example.com/runbooks/datafixer-total-failure"

  - name: aether-datafixers-warning
    rules:
      # Elevated failure rate
      - alert: DataFixerElevatedFailureRate
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
          summary: "Warning: DataFixer failure rate > 1% in domain {{ $labels.domain }}"
          description: "Failure rate is {{ $value | humanizePercentage }}. Investigate soon."

      # Slow migrations (p95)
      - alert: DataFixerSlowMigrations
        expr: |
          histogram_quantile(0.95,
            sum(rate(aether_datafixers_migrations_duration_seconds_bucket[5m])) by (le, domain)
          ) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Warning: Slow migrations in domain {{ $labels.domain }}"
          description: "p95 migration duration is {{ $value | humanizeDuration }}. Review performance."

      # Very slow migrations (p99)
      - alert: DataFixerVerySlowMigrations
        expr: |
          histogram_quantile(0.99,
            sum(rate(aether_datafixers_migrations_duration_seconds_bucket[5m])) by (le, domain)
          ) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Warning: Very slow migrations in domain {{ $labels.domain }}"
          description: "p99 migration duration is {{ $value | humanizeDuration }}. Profile and optimize."

  - name: aether-datafixers-info
    rules:
      # Large version span
      - alert: DataFixerLargeVersionSpan
        expr: |
          (
            rate(aether_datafixers_migrations_version_span_sum[1h])
            / rate(aether_datafixers_migrations_version_span_count[1h])
          ) > 50
        for: 1h
        labels:
          severity: info
        annotations:
          summary: "Info: Large version span in domain {{ $labels.domain }}"
          description: "Average span is {{ $value }} versions. Consider data freshness review."

      # No migration activity
      - alert: DataFixerNoActivity
        expr: |
          sum(rate(aether_datafixers_migrations_success_total[1h])) by (domain) == 0
          and sum(rate(aether_datafixers_migrations_failure_total[1h])) by (domain) == 0
        for: 1h
        labels:
          severity: info
        annotations:
          summary: "Info: No migration activity in domain {{ $labels.domain }}"
          description: "No migrations in the last hour. Verify this is expected."
```

---

## Grafana Dashboard

### Complete Dashboard JSON

```json
{
  "title": "Aether DataFixers Operations",
  "uid": "aether-datafixers-ops",
  "tags": ["aether", "datafixers", "migrations"],
  "timezone": "browser",
  "refresh": "30s",
  "panels": [
    {
      "title": "Migration Rate",
      "type": "timeseries",
      "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0},
      "targets": [
        {
          "expr": "sum(rate(aether_datafixers_migrations_success_total[5m])) by (domain)",
          "legendFormat": "Success ({{domain}})"
        },
        {
          "expr": "sum(rate(aether_datafixers_migrations_failure_total[5m])) by (domain)",
          "legendFormat": "Failure ({{domain}})"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "ops"
        }
      }
    },
    {
      "title": "Success Rate",
      "type": "gauge",
      "gridPos": {"h": 8, "w": 6, "x": 12, "y": 0},
      "targets": [
        {
          "expr": "(sum(rate(aether_datafixers_migrations_success_total[1h])) / (sum(rate(aether_datafixers_migrations_success_total[1h])) + sum(rate(aether_datafixers_migrations_failure_total[1h])))) * 100"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "percent",
          "min": 0,
          "max": 100,
          "thresholds": {
            "steps": [
              {"color": "red", "value": 0},
              {"color": "yellow", "value": 95},
              {"color": "green", "value": 99}
            ]
          }
        }
      }
    },
    {
      "title": "Current Error Rate",
      "type": "stat",
      "gridPos": {"h": 8, "w": 6, "x": 18, "y": 0},
      "targets": [
        {
          "expr": "sum(rate(aether_datafixers_migrations_failure_total[5m])) / (sum(rate(aether_datafixers_migrations_success_total[5m])) + sum(rate(aether_datafixers_migrations_failure_total[5m]))) * 100"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "percent",
          "thresholds": {
            "steps": [
              {"color": "green", "value": 0},
              {"color": "yellow", "value": 1},
              {"color": "red", "value": 5}
            ]
          }
        }
      }
    },
    {
      "title": "Migration Duration Percentiles",
      "type": "timeseries",
      "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8},
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
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "s"
        }
      }
    },
    {
      "title": "Version Span Distribution",
      "type": "timeseries",
      "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8},
      "targets": [
        {
          "expr": "rate(aether_datafixers_migrations_version_span_sum[5m]) / rate(aether_datafixers_migrations_version_span_count[5m])",
          "legendFormat": "Avg Span ({{domain}})"
        },
        {
          "expr": "aether_datafixers_migrations_version_span_max",
          "legendFormat": "Max Span ({{domain}})"
        }
      ]
    },
    {
      "title": "Failures by Error Type",
      "type": "piechart",
      "gridPos": {"h": 8, "w": 8, "x": 0, "y": 16},
      "targets": [
        {
          "expr": "sum(increase(aether_datafixers_migrations_failure_total[24h])) by (error_type)",
          "legendFormat": "{{error_type}}"
        }
      ]
    },
    {
      "title": "Failure Rate by Domain",
      "type": "timeseries",
      "gridPos": {"h": 8, "w": 8, "x": 8, "y": 16},
      "targets": [
        {
          "expr": "sum(rate(aether_datafixers_migrations_failure_total[5m])) by (domain)",
          "legendFormat": "{{domain}}"
        }
      ],
      "fieldConfig": {
        "defaults": {
          "unit": "ops"
        }
      }
    },
    {
      "title": "Recent Errors (Last 1h)",
      "type": "table",
      "gridPos": {"h": 8, "w": 8, "x": 16, "y": 16},
      "targets": [
        {
          "expr": "sum(increase(aether_datafixers_migrations_failure_total[1h])) by (domain, error_type) > 0",
          "format": "table",
          "instant": true
        }
      ],
      "transformations": [
        {
          "id": "organize",
          "options": {
            "renameByName": {
              "domain": "Domain",
              "error_type": "Error Type",
              "Value": "Count"
            }
          }
        }
      ]
    }
  ]
}
```

### Dashboard Import

1. Go to Grafana > Dashboards > Import
2. Paste the JSON above
3. Select your Prometheus data source
4. Click Import

---

## Actuator Integration

### Health Endpoint

The DataFixer health indicator reports UP/DOWN status per domain.

**Request:**
```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "datafixer": {
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

### Custom Endpoint

Get detailed DataFixer information at `/actuator/datafixers`:

```bash
curl http://localhost:8080/actuator/datafixers
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
    }
  }
}
```

### Kubernetes Probes

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: app
      livenessProbe:
        httpGet:
          path: /actuator/health/liveness
          port: 8080
        initialDelaySeconds: 30
        periodSeconds: 10
        failureThreshold: 3

      readinessProbe:
        httpGet:
          path: /actuator/health/readiness
          port: 8080
        initialDelaySeconds: 10
        periodSeconds: 5
        failureThreshold: 3

      startupProbe:
        httpGet:
          path: /actuator/health
          port: 8080
        initialDelaySeconds: 5
        periodSeconds: 5
        failureThreshold: 30
```

### Prometheus Scraping Actuator

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'spring-actuator'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
    scrape_interval: 15s
```

---

## Multi-Domain Monitoring

### Per-Domain Dashboards

Use Grafana variables to filter by domain:

```json
{
  "templating": {
    "list": [
      {
        "name": "domain",
        "type": "query",
        "query": "label_values(aether_datafixers_migrations_success_total, domain)",
        "refresh": 2
      }
    ]
  }
}
```

Then use `domain=~\"$domain\"` in queries:

```promql
sum(rate(aether_datafixers_migrations_success_total{domain=~"$domain"}[5m]))
```

### Cross-Domain Comparison

Compare performance across domains:

```promql
# Success rate by domain
(
  sum(rate(aether_datafixers_migrations_success_total[1h])) by (domain)
  / (
    sum(rate(aether_datafixers_migrations_success_total[1h])) by (domain)
    + sum(rate(aether_datafixers_migrations_failure_total[1h])) by (domain)
  )
) * 100
```

---

## PagerDuty Integration

### Alertmanager Configuration

```yaml
# alertmanager.yml
global:
  pagerduty_url: 'https://events.pagerduty.com/v2/enqueue'

route:
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty-critical'
    - match:
        severity: warning
      receiver: 'slack-warning'

receivers:
  - name: 'default'
    email_configs:
      - to: 'team@example.com'

  - name: 'pagerduty-critical'
    pagerduty_configs:
      - service_key: '<YOUR_PAGERDUTY_SERVICE_KEY>'
        description: '{{ .CommonAnnotations.summary }}'
        details:
          runbook: '{{ .CommonAnnotations.runbook_url }}'
          domain: '{{ .CommonLabels.domain }}'

  - name: 'slack-warning'
    slack_configs:
      - api_url: '<YOUR_SLACK_WEBHOOK_URL>'
        channel: '#alerts'
        title: '{{ .CommonAnnotations.summary }}'
        text: '{{ .CommonAnnotations.description }}'
```

---

## Application Configuration

### Enable Metrics

```yaml
# application.yml
aether:
  datafixers:
    enabled: true
    metrics:
      timing: true
      counting: true

management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, datafixers
  metrics:
    export:
      prometheus:
        enabled: true
  endpoint:
    health:
      show-details: always
```

### Custom Metrics Extension

```java
@Component
public class ExtendedMigrationMetrics extends MigrationMetrics {

    private final Counter largeSpanCounter;

    public ExtendedMigrationMetrics(MeterRegistry registry) {
        super(registry);
        this.largeSpanCounter = Counter.builder("aether.datafixers.migrations.large_span")
            .description("Migrations with version span > 100")
            .register(registry);
    }

    @Override
    public void recordSuccess(String domain, int fromVersion, int toVersion, Duration duration) {
        super.recordSuccess(domain, fromVersion, toVersion, duration);

        // Track large version spans separately
        if (Math.abs(toVersion - fromVersion) > 100) {
            largeSpanCounter.increment();
        }
    }
}
```

---

## Related

- [Spring Boot Metrics](../spring-boot/metrics.md) - Complete metrics reference
- [Spring Boot Actuator](../spring-boot/actuator.md) - Actuator integration
- [Debugging Guide](debugging-guide.md) - Diagnosing issues
- [Recovery Procedures](recovery-procedures.md) - Responding to alerts
