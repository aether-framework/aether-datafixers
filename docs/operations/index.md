# Operations Runbook

Operational guidance for running Aether Datafixers in production environments. This runbook covers error handling, debugging, monitoring, and recovery procedures.

## Quick Reference

| Scenario                       | Document                                        | Key Actions                           |
|--------------------------------|-------------------------------------------------|---------------------------------------|
| Migration fails with exception | [Error Scenarios](error-scenarios.md)           | Extract context, check exception type |
| Need detailed migration trace  | [Debugging Guide](debugging-guide.md)           | Enable `DiagnosticContext`            |
| Set up production monitoring   | [Monitoring & Alerting](monitoring-alerting.md) | Configure Micrometer metrics          |
| Partial migration / data loss  | [Recovery Procedures](recovery-procedures.md)   | Restore from backup, retry            |

## Documentation Structure

### [Error Scenarios](error-scenarios.md)

Exception handling reference for production troubleshooting:
- Exception hierarchy and context fields
- `FixException` - Migration logic failures
- `DecodeException` - Deserialization failures
- `EncodeException` - Serialization failures
- `RegistryException` - Missing type or version
- Schema mismatch detection and resolution

### [Debugging Guide](debugging-guide.md)

Systematic approach to diagnosing migration issues:
- SLF4J configuration (Logback, Log4j2)
- Using `MigrationReport` for diagnostics
- Tracing fix application order
- Step-by-step debugging workflow

### [Monitoring & Alerting](monitoring-alerting.md)

Production monitoring setup:
- Micrometer metrics reference
- Recommended alert thresholds
- Prometheus alerting rules
- Grafana dashboard templates
- Actuator health integration

### [Recovery Procedures](recovery-procedures.md)

Handling failures and data recovery:
- Backup recommendations
- Partial migration recovery
- Rollback strategies
- Incident response workflows

---

## Emergency Response

### Migration Completely Failed

1. **Check metrics** - Look at `aether.datafixers.migrations.failure` counter
2. **Extract context** - See [Error Scenarios](error-scenarios.md#extracting-exception-context)
3. **Enable DEBUG** - Set log level for `de.splatgames.aether.datafixers`
4. **Capture diagnostics** - Use `DiagnosticContext` on a sample record
5. **Restore if needed** - See [Recovery Procedures](recovery-procedures.md)

### High Failure Rate Alert

1. **Check error breakdown** - Query failures by `error_type` tag
2. **Identify pattern** - Same exception? Same data version?
3. **Isolate bad records** - Query for records at problematic version
4. **Apply targeted fix** - Fix data or code, retry migration

### Slow Migration Alert

1. **Check version span** - Large version jumps take longer
2. **Profile fixes** - Use `MigrationReport.fixExecutions()` timing
3. **Check data size** - Large objects slow down processing
4. **Consider batching** - Process in smaller batches

---

## Health Checks

### Actuator Endpoints

| Endpoint               | Purpose                   |
|------------------------|---------------------------|
| `/actuator/health`     | UP/DOWN status per domain |
| `/actuator/info`       | Version information       |
| `/actuator/datafixers` | Detailed domain status    |
| `/actuator/prometheus` | Metrics export            |

### Kubernetes Probes

```yaml
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
  initialDelaySeconds: 10
  periodSeconds: 5
```

---

## Key Metrics Overview

| Metric                                      | Type         | Alert Threshold |
|---------------------------------------------|--------------|-----------------|
| `aether.datafixers.migrations.success`      | Counter      | -               |
| `aether.datafixers.migrations.failure`      | Counter      | > 0 per minute  |
| `aether.datafixers.migrations.duration`     | Timer        | p99 > 1s        |
| `aether.datafixers.migrations.version.span` | Distribution | avg > 50        |

See [Monitoring & Alerting](monitoring-alerting.md) for complete metrics reference.

---

## Related

- [Troubleshooting](../troubleshooting/index.md) - Basic troubleshooting tips
- [Spring Boot Metrics](../spring-boot/metrics.md) - Detailed metrics reference
- [Spring Boot Actuator](../spring-boot/actuator.md) - Actuator integration
- [How to Use Diagnostics](../how-to/use-diagnostics.md) - Diagnostic API reference
