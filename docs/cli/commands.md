# Command Reference

This page provides detailed documentation for all CLI commands and their options.

## Global Options

These options are available for all commands:

| Option | Description |
|--------|-------------|
| `-h`, `--help` | Show help message and exit |
| `-V`, `--version` | Show version information and exit |

---

## migrate

Migrate data files from one schema version to another.

### Synopsis

```
aether-cli migrate [OPTIONS] <files>...
```

### Required Options

| Option | Description |
|--------|-------------|
| `--to <version>` | Target data version to migrate to |
| `-t`, `--type <type>` | Type reference ID (e.g., `player`, `world`) |
| `--bootstrap <class>` | Fully qualified class name of your `DataFixerBootstrap` |

### Optional Options

| Option | Default | Description |
|--------|---------|-------------|
| `--from <version>` | (auto) | Source version (auto-detected from data if not specified) |
| `--version-field <path>` | `dataVersion` | Field path containing the version in input files |
| `-o`, `--output <path>` | (varies) | Output file or directory |
| `--format <id>` | `json-gson` | Input/output format handler |
| `--backup` | `true` | Create `.bak` backup before in-place modification |
| `--pretty` | `true` | Pretty-print output with indentation |
| `-v`, `--verbose` | `false` | Enable detailed progress output |
| `--fail-fast` | `false` | Stop immediately on first error |
| `--report` | `false` | Generate migration report |
| `--report-format <fmt>` | `text` | Report format: `text` or `json` |
| `--report-file <path>` | (stderr) | Write report to file instead of stderr |

### Arguments

| Argument | Description |
|----------|-------------|
| `<files>...` | One or more input files to migrate (glob patterns expanded by shell) |

### Output Behavior

The output destination depends on the combination of options:

| Scenario | Behavior |
|----------|----------|
| Single file, no `--output` | Write to stdout |
| Multiple files, no `--output` | Modify in-place (with backup if `--backup` is true) |
| `--output` is a file | Write single file there (error if multiple inputs) |
| `--output` is a directory | Write all files to that directory |

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | All files migrated successfully |
| `1` | One or more errors occurred |

### Examples

```bash
# Basic migration
aether-cli migrate --to 200 --type player --bootstrap com.example.MyBootstrap input.json

# Auto-detect source version from custom field
aether-cli migrate --to 200 --type player \
    --version-field meta.schemaVersion \
    --bootstrap com.example.MyBootstrap input.json

# Migrate multiple files in-place
aether-cli migrate --to 200 --type player \
    --bootstrap com.example.MyBootstrap \
    data/*.json

# Migrate to output directory
aether-cli migrate --to 200 --type player \
    --output ./migrated/ \
    --bootstrap com.example.MyBootstrap \
    data/*.json

# Compact output (no pretty printing)
aether-cli migrate --to 200 --type player \
    --pretty=false \
    --bootstrap com.example.MyBootstrap input.json

# Generate JSON report
aether-cli migrate --to 200 --type player \
    --report --report-format json --report-file migration.log \
    --bootstrap com.example.MyBootstrap data/*.json

# Verbose mode with fail-fast
aether-cli migrate --to 200 --type player \
    -v --fail-fast \
    --bootstrap com.example.MyBootstrap data/*.json
```

---

## validate

Validate data files and check if migration is needed without modifying them.

### Synopsis

```
aether-cli validate [OPTIONS] <files>...
```

### Required Options

| Option | Description |
|--------|-------------|
| `--to <version>` | Target version to validate against |
| `-t`, `--type <type>` | Type reference ID |
| `--bootstrap <class>` | Fully qualified class name of your `DataFixerBootstrap` |

### Optional Options

| Option | Default | Description |
|--------|---------|-------------|
| `--version-field <path>` | `dataVersion` | Field path containing the version |
| `--format <id>` | `json-gson` | Input format handler |

### Arguments

| Argument | Description |
|----------|-------------|
| `<files>...` | One or more input files to validate |

### Output Format

For each file, the command outputs one of:

```
OK: filename.json (v200)           # File is up-to-date
MIGRATE: filename.json (v100 -> v200)  # File needs migration
ERROR: filename.json - message     # Validation failed
```

Followed by a summary:

```
Summary: 5 up-to-date, 3 need migration, 1 errors
```

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | All files are up-to-date |
| `1` | One or more errors occurred |
| `2` | No errors, but one or more files need migration |

### Examples

```bash
# Validate a single file
aether-cli validate --to 200 --type player \
    --bootstrap com.example.MyBootstrap input.json

# Validate multiple files
aether-cli validate --to 200 --type player \
    --bootstrap com.example.MyBootstrap data/*.json

# Use custom version field
aether-cli validate --to 200 --type player \
    --version-field meta.version \
    --bootstrap com.example.MyBootstrap input.json

# CI/CD: Exit code indicates if migration is needed
if aether-cli validate --to 200 --type player --bootstrap com.example.MyBootstrap data/*.json; then
    echo "All files up-to-date"
else
    exit_code=$?
    if [ $exit_code -eq 2 ]; then
        echo "Migration needed"
    else
        echo "Validation error"
    fi
fi
```

---

## info

Display version and configuration information.

### Synopsis

```
aether-cli info [OPTIONS]
```

### Optional Options

| Option | Description |
|--------|-------------|
| `--formats` | List available format handlers |
| `--bootstrap <class>` | Show bootstrap information |
| `--to <version>` | Target version (required with `--bootstrap`) |

### Output

Without options (or with `--formats`):

```
Aether Datafixers CLI v0.4.0
============================

Available Formats:
  - json-gson: JSON format using Gson
    Extensions: json
  - json-jackson: JSON format using Jackson
    Extensions: json
```

With `--bootstrap`:

```
Aether Datafixers CLI v0.4.0
============================

Bootstrap Information:
  Class: com.example.MyBootstrap
  Target Version: 200
```

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Information displayed successfully |
| `1` | Error loading bootstrap or missing `--to` option |

### Examples

```bash
# Show available formats
aether-cli info --formats

# Show all info (formats shown by default)
aether-cli info

# Show bootstrap details
aether-cli info --bootstrap com.example.MyBootstrap --to 200
```

---

## help

Show help for the CLI or a specific command.

### Synopsis

```
aether-cli help [COMMAND]
```

### Examples

```bash
# General help
aether-cli help

# Help for migrate command
aether-cli help migrate

# Alternative: use --help flag
aether-cli migrate --help
```

---

## Version Field Path

The `--version-field` option supports dot notation for nested fields:

| Path | Example JSON | Extracted Version |
|------|--------------|-------------------|
| `dataVersion` | `{"dataVersion": 100}` | `100` |
| `meta.version` | `{"meta": {"version": 100}}` | `100` |
| `data.schema.v` | `{"data": {"schema": {"v": 100}}}` | `100` |

---

## Format Handlers

Available format handlers:

| ID | Description | Extensions |
|----|-------------|------------|
| `json-gson` | JSON using Google Gson (default) | `.json` |
| `json-jackson` | JSON using Jackson Databind | `.json` |

Use `aether-cli info --formats` to see all available handlers.

Custom handlers can be added via ServiceLoader. See [Format Handlers](format-handlers.md) for details.

---

## Next Steps

→ [Format Handlers](format-handlers.md) — Create custom format handlers
→ [Examples](examples.md) — Real-world usage examples
