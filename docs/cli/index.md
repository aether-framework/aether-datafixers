# Command-Line Interface

The Aether Datafixers CLI is a command-line tool for migrating and validating data files without writing Java code.

## Overview

The CLI module (`aether-datafixers-cli`) provides a standalone tool for:

- **Migrating** data files from one schema version to another
- **Validating** data files against a target version
- **Batch processing** multiple files with glob patterns
- **CI/CD integration** with machine-readable output formats

## Quick Start

```bash
# Migrate a single file
aether-cli migrate --to 200 --type player --bootstrap com.example.MyBootstrap input.json

# Validate files before migration
aether-cli validate --to 200 --type player --bootstrap com.example.MyBootstrap *.json

# Show available formats
aether-cli info --formats
```

## Available Commands

| Command    | Description                                                    |
|------------|----------------------------------------------------------------|
| `migrate`  | Migrate data files from one version to another                 |
| `validate` | Check if files need migration without modifying them           |
| `info`     | Display version info, available formats, and bootstrap details |
| `help`     | Show help for any command                                      |

## How It Works

The CLI uses your existing `DataFixerBootstrap` implementation to perform migrations:

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLI Workflow                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Load Bootstrap    ──▶  Your DataFixerBootstrap class        │
│                            (schemas + fixes)                    │
│                                                                 │
│  2. Parse Input       ──▶  FormatHandler reads the file         │
│                            (JSON via Gson or Jackson)           │
│                                                                 │
│  3. Extract Version   ──▶  Read version from data field         │
│                            (e.g., "dataVersion": 100)           │
│                                                                 │
│  4. Apply Fixes       ──▶  AetherDataFixer.update()             │
│                            runs all relevant DataFix instances  │
│                                                                 │
│  5. Write Output      ──▶  Serialized to stdout, file,          │
│                            or in-place with backup              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Exit Codes

The CLI uses standard exit codes for scripting and CI/CD integration:

| Code | Meaning                                            |
|------|----------------------------------------------------|
| `0`  | Success (all operations completed)                 |
| `1`  | Error occurred (file not found, parse error, etc.) |
| `2`  | Validation: one or more files need migration       |

## Requirements

- **Java 17** or later
- Your `DataFixerBootstrap` implementation on the classpath
- Input files in a supported format (JSON by default)

## Built-in Format Handlers

| Format ID      | Description           | Library          |
|----------------|-----------------------|------------------|
| `json-gson`    | JSON format (default) | Google Gson      |
| `json-jackson` | JSON format           | Jackson Databind |

Custom format handlers can be added via the ServiceLoader SPI.

---

## In This Section

- [Installation](installation.md) — Build and run the CLI
- [Command Reference](commands.md) — Detailed options for each command
- [Format Handlers](format-handlers.md) — Create custom format handlers
- [Examples](examples.md) — Real-world usage examples

---

## Next Steps

→ [Installation](installation.md) — Set up the CLI tool
