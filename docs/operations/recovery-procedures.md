# Recovery Procedures

How to recover from migration failures, data issues, and production incidents.

## Quick Reference

| Scenario                 | Procedure              | Complexity |
|--------------------------|------------------------|------------|
| Single record failure    | Retry with diagnostics | Low        |
| Batch failure (< 5%)     | Isolate and retry      | Medium     |
| High failure rate (> 5%) | Stop, investigate, fix | High       |
| Data corruption          | Restore from backup    | High       |
| Schema mismatch          | Version alignment      | Medium     |

---

## Backup Recommendations

### Pre-Migration Backup Strategy

**Before major version bumps:**
1. Create full database backup
2. Verify backup integrity (test restore)
3. Document current schema version
4. Keep backup for rollback window (e.g., 7 days)

**Before routine operations:**
1. Enable point-in-time recovery
2. Verify incremental backups are current
3. Document migration batch parameters

### Backup Checklist

```markdown
## Pre-Migration Backup Checklist

- [ ] Database backup completed
- [ ] Backup verified (test restore on staging)
- [ ] Backup retention policy confirmed
- [ ] Schema version documented in backup metadata
- [ ] Rollback procedure documented
- [ ] Team notified of migration window
```

### Database Backup Patterns

**PostgreSQL:**
```bash
# Full backup before migration
pg_dump -Fc -f backup_v100_$(date +%Y%m%d).dump mydb

# With version in filename
pg_dump -Fc -f backup_schema_v100_to_v200_$(date +%Y%m%d_%H%M%S).dump mydb
```

**MongoDB:**
```bash
# Full backup
mongodump --db mydb --out ./backup_v100_$(date +%Y%m%d)

# Specific collection
mongodump --db mydb --collection players --out ./backup_players_v100
```

### Application-Level Snapshots

```java
// Create pre-migration snapshot for critical records
public void createMigrationSnapshot(List<String> recordIds) {
    Path snapshotDir = Path.of("snapshots",
        "migration_" + System.currentTimeMillis());
    Files.createDirectories(snapshotDir);

    for (String id : recordIds) {
        Dynamic<?> data = loadRecord(id);
        Path file = snapshotDir.resolve(id + ".json");
        Files.writeString(file, serializeToJson(data));
    }

    logger.info("Created snapshot of {} records at {}",
        recordIds.size(), snapshotDir);
}
```

---

## Partial Migration Recovery

### Detecting Partial Migrations

**Symptoms:**
- Some records at old version, some at new version
- Inconsistent data across related entities
- `aether.datafixers.migrations.failure` spike followed by recovery

**Detection Query (SQL):**
```sql
-- Find version distribution
SELECT data_version, COUNT(*) as count
FROM entities
WHERE type = 'player'
GROUP BY data_version
ORDER BY data_version;

-- Find records still at old version
SELECT id, data_version, updated_at
FROM entities
WHERE type = 'player'
  AND data_version < 200
ORDER BY updated_at DESC;
```

**Detection Query (MongoDB):**
```javascript
// Version distribution
db.entities.aggregate([
  { $match: { type: "player" } },
  { $group: { _id: "$dataVersion", count: { $sum: 1 } } },
  { $sort: { _id: 1 } }
]);

// Records at old version
db.entities.find({
  type: "player",
  dataVersion: { $lt: 200 }
}).sort({ updatedAt: -1 });
```

### Recovery Option 1: Retry Failed Records

Best for: Small number of failures, transient errors.

```java
public class MigrationRetryService {

    private final AetherDataFixer fixer;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void retryFailedRecords(List<String> failedIds, int targetVersion) {
        int success = 0;
        int failed = 0;

        for (String id : failedIds) {
            try {
                // Load record
                Dynamic<?> data = loadRecord(id);
                int currentVersion = extractVersion(data);

                // Skip if already migrated
                if (currentVersion >= targetVersion) {
                    logger.info("Record {} already at version {}", id, currentVersion);
                    continue;
                }

                // Enable diagnostics for retry
                DiagnosticContext ctx = DiagnosticContext.create(
                    DiagnosticOptions.builder()
                        .captureSnapshots(true)
                        .build()
                );

                // Retry migration
                Dynamic<?> result = fixer.update(
                    TypeReferences.PLAYER,
                    data,
                    new DataVersion(currentVersion),
                    new DataVersion(targetVersion),
                    ctx
                );

                // Save result
                saveRecord(id, result);
                success++;

            } catch (DataFixerException e) {
                failed++;
                logger.error("Retry failed for record {}: {} [{}]",
                    id, e.getMessage(), e.getContext());
            }
        }

        logger.info("Retry complete: {} success, {} failed", success, failed);
    }
}
```

### Recovery Option 2: Isolate and Skip

Best for: Specific data patterns causing failures.

```java
public class MigrationIsolationService {

    public void migrateWithIsolation(Stream<Dynamic<?>> records, int targetVersion) {
        List<String> quarantined = new ArrayList<>();

        records.forEach(data -> {
            String id = extractId(data);
            try {
                Dynamic<?> result = fixer.update(
                    TypeReferences.PLAYER,
                    data,
                    extractVersion(data),
                    targetVersion
                );
                saveRecord(id, result);
            } catch (DataFixerException e) {
                // Quarantine failed record
                quarantined.add(id);
                saveToQuarantine(id, data, e);
                logger.warn("Quarantined record {}: {}", id, e.getMessage());
            }
        });

        if (!quarantined.isEmpty()) {
            logger.warn("Migration complete with {} quarantined records", quarantined.size());
            notifyTeam(quarantined);
        }
    }

    private void saveToQuarantine(String id, Dynamic<?> data, DataFixerException e) {
        // Save to quarantine table/collection for manual review
        QuarantineRecord record = new QuarantineRecord(
            id,
            serializeToJson(data),
            e.getClass().getSimpleName(),
            e.getMessage(),
            e.getContext(),
            Instant.now()
        );
        quarantineRepository.save(record);
    }
}
```

### Recovery Option 3: Manual Intervention

Best for: Complex data issues requiring human judgment.

```java
public class ManualRecoveryService {

    public void exportForManualReview(List<String> recordIds) {
        Path exportDir = Path.of("manual_review",
            LocalDate.now().toString());
        Files.createDirectories(exportDir);

        for (String id : recordIds) {
            Dynamic<?> data = loadRecord(id);

            // Export with metadata
            Map<String, Object> export = new LinkedHashMap<>();
            export.put("id", id);
            export.put("currentVersion", extractVersion(data));
            export.put("targetVersion", CURRENT_VERSION);
            export.put("data", data.getValue());
            export.put("exportedAt", Instant.now().toString());

            Path file = exportDir.resolve(id + ".json");
            Files.writeString(file, prettyJson(export));
        }

        logger.info("Exported {} records to {} for manual review",
            recordIds.size(), exportDir);
    }

    public void importManualFixes(Path fixesDir) {
        try (Stream<Path> files = Files.list(fixesDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                .forEach(file -> {
                    try {
                        Map<String, Object> fixed = parseJson(Files.readString(file));
                        String id = (String) fixed.get("id");
                        Object data = fixed.get("data");
                        int version = ((Number) fixed.get("fixedVersion")).intValue();

                        saveRecord(id, createDynamic(data, version));
                        logger.info("Imported manual fix for record {}", id);
                    } catch (Exception e) {
                        logger.error("Failed to import {}: {}", file, e.getMessage());
                    }
                });
        }
    }
}
```

---

## Rollback Strategies

### Important: Forward-Only Design

Aether Datafixers is designed for **forward migration only**. True rollback requires:

1. **Restore from backup** (recommended)
2. **Write compensating fixes** (complex, not recommended)

### Restore from Backup

**Full Restore:**

```bash
# PostgreSQL
pg_restore -d mydb backup_v100_20240115.dump

# MongoDB
mongorestore --db mydb ./backup_v100_20240115/mydb
```

**Selective Restore (specific records):**

```sql
-- PostgreSQL: Restore specific records from backup
-- 1. Restore backup to temporary schema
CREATE SCHEMA backup_restore;
pg_restore -d mydb -n backup_restore backup_v100.dump

-- 2. Copy specific records
INSERT INTO entities (id, type, data, data_version)
SELECT id, type, data, data_version
FROM backup_restore.entities
WHERE id IN ('record1', 'record2', 'record3')
ON CONFLICT (id) DO UPDATE
SET data = EXCLUDED.data, data_version = EXCLUDED.data_version;

-- 3. Clean up
DROP SCHEMA backup_restore CASCADE;
```

### Compensating Fixes (Advanced)

Only use when backup is unavailable and you understand the exact transformations to reverse.

```java
// Example: Reverse a field rename (name -> displayName back to name)
public class ReverseRenameDisplayNameFix extends SchemaDataFix {

    public ReverseRenameDisplayNameFix(Schema inputSchema, Schema outputSchema) {
        super("reverse_rename_display_name", inputSchema, outputSchema);
    }

    @Override
    protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
        return Rules.renameField(
            TypeReferences.PLAYER,
            "displayName",  // current name
            "name"          // original name
        );
    }
}
```

**Warning:** Compensating fixes are error-prone. Prefer backup restoration.

---

## Error Recovery Workflows

### Workflow 1: FixException Recovery

```
1. Extract exception context
   └─ Get fixName, fromVersion, toVersion, typeReference

2. Enable DiagnosticContext
   └─ captureSnapshots(true), captureRuleDetails(true)

3. Reproduce with single record
   └─ Run migration on isolated test record

4. Analyze MigrationReport
   └─ Check fix.beforeSnapshot vs fix.afterSnapshot
   └─ Find exact rule that failed

5. Identify root cause
   ├─ Missing field? → Check input data
   ├─ Wrong type? → Check codec/schema
   └─ Logic error? → Check fix implementation

6. Fix data or code
   ├─ Data issue → Clean/transform data
   └─ Code issue → Deploy fix, redeploy

7. Retry migration
   └─ Process failed records
```

### Workflow 2: DecodeException Recovery

```
1. Get path from exception
   └─ e.getPath() returns "player.inventory[0].item"

2. Navigate to problematic field
   └─ Use path to find exact location in data

3. Determine expected vs actual type
   └─ Check schema definition
   └─ Compare with actual data

4. Clean/transform data
   ├─ Missing field? → Add default value
   ├─ Wrong type? → Convert or remove
   └─ Malformed? → Parse and fix

5. Retry migration
```

### Workflow 3: RegistryException Recovery

```
1. Check missing type/version
   └─ e.getMissingType() or e.getMissingVersion()

2. Verify bootstrap registration
   └─ Check DataFixerBootstrap implementation

3. Check version chain completeness
   └─ Ensure no gaps in version sequence

4. Add missing registrations
   └─ Register missing type or schema

5. Redeploy and retry
```

---

## Incident Response

### Severity Levels

| Level | Criteria | Response Time | Escalation |
|-------|----------|---------------|------------|
| P1 | All migrations failing | Immediate | On-call + Lead + Manager |
| P2 | > 5% failure rate | 15 min | On-call + Lead |
| P3 | > 1% failure rate | 1 hour | On-call |
| P4 | Isolated failures | 4 hours | Next business day |

### Incident Response Checklist

#### Initial Response (0-5 min)

```markdown
## Initial Response Checklist

- [ ] Acknowledge alert
- [ ] Check metrics dashboard
  - Current failure rate
  - Error type breakdown
  - Affected domains
- [ ] Review recent deployments (last 24h)
- [ ] Check actuator health endpoint
- [ ] Initial assessment posted to incident channel
```

#### Investigation (5-30 min)

```markdown
## Investigation Checklist

- [ ] Enable DEBUG logging for de.splatgames.aether.datafixers
- [ ] Capture sample failures (3-5 records)
- [ ] Enable DiagnosticContext on sample records
- [ ] Analyze MigrationReport for patterns
- [ ] Check database/storage health
- [ ] Check upstream service health
- [ ] Root cause hypothesis documented
```

#### Resolution

```markdown
## Resolution Checklist

- [ ] Root cause confirmed
- [ ] Fix identified
  - [ ] Data fix (transformation/cleanup)
  - [ ] Code fix (bug fix)
  - [ ] Configuration fix (settings change)
- [ ] Fix tested in staging
- [ ] Fix deployed to production
- [ ] Metrics returning to normal
- [ ] Failed records reprocessed
```

#### Post-Incident

```markdown
## Post-Incident Checklist

- [ ] Timeline documented
- [ ] Root cause analysis complete
- [ ] Post-mortem scheduled (within 48h)
- [ ] Runbook updated if needed
- [ ] Preventive measures identified
- [ ] Follow-up tasks created
```

---

## Data Validation After Recovery

### Consistency Checks

```sql
-- Check for version consistency
SELECT
    type,
    MIN(data_version) as min_version,
    MAX(data_version) as max_version,
    COUNT(*) as count
FROM entities
GROUP BY type;

-- Check for orphaned references
SELECT e.id, e.type
FROM entities e
LEFT JOIN entities parent ON e.parent_id = parent.id
WHERE e.parent_id IS NOT NULL AND parent.id IS NULL;
```

### Version Alignment

```java
public void verifyVersionAlignment(int expectedVersion) {
    // Count records at wrong version
    long wrongVersion = entityRepository.countByDataVersionNot(expectedVersion);

    if (wrongVersion > 0) {
        logger.error("Found {} records at wrong version (expected {})",
            wrongVersion, expectedVersion);

        // List samples
        List<Entity> samples = entityRepository
            .findByDataVersionNot(expectedVersion, PageRequest.of(0, 10));

        for (Entity e : samples) {
            logger.error("  {} at version {} (expected {})",
                e.getId(), e.getDataVersion(), expectedVersion);
        }
    } else {
        logger.info("All records at expected version {}", expectedVersion);
    }
}
```

### Functional Verification

```java
@Test
void verifyMigrationSuccess() {
    // Load sample migrated records
    List<Entity> samples = entityRepository.findRandomSample(100);

    for (Entity entity : samples) {
        // Verify can decode at current version
        assertDoesNotThrow(() -> {
            Typed<?> typed = fixer.decode(
                CURRENT_VERSION,
                entity.getTypeReference(),
                entity.getData()
            );
            assertNotNull(typed.getValue());
        }, "Failed to decode entity " + entity.getId());

        // Verify key fields present
        Dynamic<?> data = entity.getData();
        assertTrue(data.get("id").asString().result().isPresent());
        assertTrue(data.get("_version").asNumber().result().isPresent());
    }
}
```

---

## Related

- [Error Scenarios](error-scenarios.md) — Exception handling reference
- [Debugging Guide](debugging-guide.md) — Diagnosing issues
- [Monitoring & Alerting](monitoring-alerting.md) — Detecting problems
- [Troubleshooting](../troubleshooting/index.md) — Quick fixes
