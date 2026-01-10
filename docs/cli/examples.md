# CLI Examples

This page demonstrates practical CLI usage scenarios with step-by-step examples.

## Prerequisites

All examples assume:
- Java 17+ installed
- CLI JAR available (see [Installation](installation.md))
- A `DataFixerBootstrap` implementation on the classpath

For these examples, we'll use a hypothetical bootstrap:
```
com.example.game.GameDataBootstrap
```

---

## Example 1: Basic Single File Migration

**Scenario:** Migrate a player save file from version 100 to version 200.

### Input File: `player.json`
```json
{
  "dataVersion": 100,
  "playerName": "Steve",
  "xp": 500,
  "health": 20
}
```

### Command
```bash
aether-cli migrate \
    --to 200 \
    --type player \
    --bootstrap com.example.game.GameDataBootstrap \
    player.json
```

### Output (stdout)
```json
{
  "dataVersion": 200,
  "name": "Steve",
  "experience": 500,
  "stats": {
    "health": 20,
    "maxHealth": 20
  }
}
```

---

## Example 2: Batch Migration with Backup

**Scenario:** Migrate all player files in a directory, creating backups.

### Directory Structure
```
saves/
├── player_001.json
├── player_002.json
└── player_003.json
```

### Command
```bash
aether-cli migrate \
    --to 200 \
    --type player \
    --backup \
    --bootstrap com.example.game.GameDataBootstrap \
    saves/*.json
```

### Result
```
saves/
├── player_001.json      # Migrated
├── player_001.json.bak  # Backup
├── player_002.json      # Migrated
├── player_002.json.bak  # Backup
├── player_003.json      # Migrated
└── player_003.json.bak  # Backup
```

### Console Output
```
Completed: 3 migrated, 0 errors
```

---

## Example 3: Migration to Output Directory

**Scenario:** Migrate files without modifying originals.

### Command
```bash
mkdir -p migrated

aether-cli migrate \
    --to 200 \
    --type player \
    --output ./migrated/ \
    --bootstrap com.example.game.GameDataBootstrap \
    saves/*.json
```

### Result
```
saves/
├── player_001.json  # Unchanged
├── player_002.json  # Unchanged
└── player_003.json  # Unchanged

migrated/
├── player_001.json  # Migrated copy
├── player_002.json  # Migrated copy
└── player_003.json  # Migrated copy
```

---

## Example 4: Pre-Migration Validation

**Scenario:** Check which files need migration before running a batch job.

### Command
```bash
aether-cli validate \
    --to 200 \
    --type player \
    --bootstrap com.example.game.GameDataBootstrap \
    saves/*.json
```

### Output
```
OK: saves/player_001.json (v200)
MIGRATE: saves/player_002.json (v150 -> v200)
MIGRATE: saves/player_003.json (v100 -> v200)

Summary: 1 up-to-date, 2 need migration, 0 errors
```

### Exit Code
```bash
echo $?
# 2 (indicates files need migration)
```

---

## Example 5: CI/CD Integration

**Scenario:** Validate data files in a CI pipeline, failing the build if migration is needed.

### GitHub Actions Workflow
```yaml
name: Validate Data Files

on: [push, pull_request]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build project
        run: mvn clean package -DskipTests

      - name: Validate data files
        run: |
          java -jar target/my-app-cli.jar validate \
              --to 200 \
              --type player \
              --bootstrap com.example.game.GameDataBootstrap \
              src/test/resources/data/*.json

      - name: Check validation result
        run: |
          exit_code=$?
          if [ $exit_code -eq 2 ]; then
            echo "::warning::Data files need migration"
            exit 1
          elif [ $exit_code -ne 0 ]; then
            echo "::error::Validation failed"
            exit 1
          fi
```

---

## Example 6: Migration with JSON Report

**Scenario:** Generate a machine-readable report for logging or monitoring.

### Command
```bash
aether-cli migrate \
    --to 200 \
    --type player \
    --report \
    --report-format json \
    --report-file migration-report.json \
    --bootstrap com.example.game.GameDataBootstrap \
    saves/*.json
```

### Report Output: `migration-report.json`
```json
{
  "file": "player_001.json",
  "type": "player",
  "fromVersion": 100,
  "toVersion": 200,
  "durationMs": 15
}
{
  "file": "player_002.json",
  "type": "player",
  "fromVersion": 150,
  "toVersion": 200,
  "durationMs": 8
}
```

### Processing with jq
```bash
# Count total migrations
cat migration-report.json | jq -s 'length'

# Calculate total duration
cat migration-report.json | jq -s '[.[].durationMs] | add'

# Find slowest migration
cat migration-report.json | jq -s 'max_by(.durationMs)'
```

---

## Example 7: Custom Version Field

**Scenario:** Data files use a different field for version tracking.

### Input File
```json
{
  "metadata": {
    "schemaVersion": 100,
    "lastModified": "2024-01-15"
  },
  "data": {
    "name": "Steve",
    "score": 1000
  }
}
```

### Command
```bash
aether-cli migrate \
    --to 200 \
    --type player \
    --version-field metadata.schemaVersion \
    --bootstrap com.example.game.GameDataBootstrap \
    input.json
```

---

## Example 8: Verbose Mode for Debugging

**Scenario:** Debug migration issues with detailed output.

### Command
```bash
aether-cli migrate \
    --to 200 \
    --type player \
    --verbose \
    --bootstrap com.example.game.GameDataBootstrap \
    problematic.json
```

### Output
```
Migrated: problematic.json (v100 -> v200 in 42ms)
Completed: 1 migrated, 0 errors
```

If an error occurs:
```
Error processing problematic.json: Missing required field 'playerName'
java.lang.IllegalStateException: Missing required field 'playerName'
    at com.example.game.fixes.RenamePlayerNameFix.apply(RenamePlayerNameFix.java:45)
    at de.splatgames.aether.datafixers.core.AetherDataFixer.update(AetherDataFixer.java:123)
    ...
```

---

## Example 9: Using Jackson Format

**Scenario:** Use Jackson for better performance with large files.

### Command
```bash
aether-cli migrate \
    --to 200 \
    --type player \
    --format json-jackson \
    --bootstrap com.example.game.GameDataBootstrap \
    large-world.json
```

---

## Example 10: Shell Script Wrapper

**Scenario:** Create a reusable migration script.

### Script: `migrate-player-data.sh`
```bash
#!/bin/bash
set -e

# Configuration
BOOTSTRAP="com.example.game.GameDataBootstrap"
TARGET_VERSION=200
TYPE="player"
CLI_JAR="./tools/aether-cli.jar"

# Parse arguments
INPUT_DIR="${1:-.}"
OUTPUT_DIR="${2:-./migrated}"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Validate first
echo "Validating files..."
if java -jar "$CLI_JAR" validate \
    --to "$TARGET_VERSION" \
    --type "$TYPE" \
    --bootstrap "$BOOTSTRAP" \
    "$INPUT_DIR"/*.json; then
    echo "All files up-to-date, nothing to do."
    exit 0
fi

# Check if validation found files needing migration (exit code 2)
if [ $? -eq 2 ]; then
    echo "Migrating files..."
    java -jar "$CLI_JAR" migrate \
        --to "$TARGET_VERSION" \
        --type "$TYPE" \
        --output "$OUTPUT_DIR" \
        --report \
        --report-file "$OUTPUT_DIR/migration.log" \
        --bootstrap "$BOOTSTRAP" \
        "$INPUT_DIR"/*.json

    echo "Migration complete. Report: $OUTPUT_DIR/migration.log"
else
    echo "Validation failed with errors."
    exit 1
fi
```

### Usage
```bash
chmod +x migrate-player-data.sh
./migrate-player-data.sh ./saves ./saves-migrated
```

---

## Example 11: Docker Integration

**Scenario:** Run migrations in a Docker container.

### Dockerfile
```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy CLI and bootstrap
COPY target/aether-cli.jar /app/
COPY target/my-bootstrap.jar /app/

# Entry point
ENTRYPOINT ["java", "-cp", "/app/aether-cli.jar:/app/my-bootstrap.jar", \
            "de.splatgames.aether.datafixers.cli.AetherCli"]
```

### Usage
```bash
docker build -t aether-cli .

docker run -v $(pwd)/data:/data aether-cli migrate \
    --to 200 \
    --type player \
    --output /data/migrated \
    --bootstrap com.example.game.GameDataBootstrap \
    /data/*.json
```

---

## Summary

| Use Case             | Key Options                     |
|----------------------|---------------------------------|
| Basic migration      | `--to`, `--type`, `--bootstrap` |
| Batch with backup    | Add `--backup`                  |
| Output to directory  | Add `--output <dir>`            |
| Pre-check            | Use `validate` command          |
| Machine-readable     | `--report --report-format json` |
| Custom version field | `--version-field <path>`        |
| Debug issues         | Add `--verbose`                 |
| Performance          | `--format json-jackson`         |

---

## Next Steps

→ [Command Reference](commands.md) — Full option documentation
→ [Format Handlers](format-handlers.md) — Support additional formats
