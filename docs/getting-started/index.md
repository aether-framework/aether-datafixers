# Getting Started

Welcome to Aether Datafixers! This section will help you get up and running quickly.

## Prerequisites

Before you begin, ensure you have:

- **Java 17** or later installed
- A build tool: **Maven** or **Gradle**
- Basic understanding of Java and data serialization concepts

## Learning Path

### 1. Installation

First, add Aether Datafixers to your project:

→ [Installation Guide](installation.md)

Learn how to:
- Add dependencies with Maven or Gradle
- Use the Bill of Materials (BOM) for version management
- Choose which modules you need

### 2. Quick Start

Get a working example in 5 minutes:

→ [Quick Start](quick-start.md)

Learn how to:
- Create a simple data fix
- Apply a migration to JSON data
- See the transformation in action

### 3. Your First Complete Migration

Build a full migration system step by step:

→ [Your First Migration](your-first-migration.md)

Learn how to:
- Define `TypeReference` constants
- Create `Schema` classes for different versions
- Implement `DataFix` migrations
- Wire everything together with a `DataFixerBootstrap`
- Test your migrations

## What You'll Build

By the end of this getting started guide, you'll have:

1. A working data fixer that can migrate data between versions
2. Understanding of the core concepts
3. A foundation for building more complex migrations

## Key Concepts Preview

Before diving in, here's a quick overview of what you'll encounter:

| Concept | Purpose |
|---------|---------|
| `DataVersion` | Integer identifying a schema version (e.g., 100 = v1.0.0) |
| `TypeReference` | String identifier for a data type (e.g., "player") |
| `Schema` | Defines what types exist at a particular version |
| `DataFix` | A single migration from one version to another |
| `DataFixer` | Orchestrates applying multiple fixes |
| `Dynamic` | Format-agnostic data wrapper |

## Next Steps

Ready to begin? Start with the [Installation Guide](installation.md).

---

## Additional Resources

- [Concepts Overview](../concepts/index.md) — Deep dive into framework concepts
- [Tutorials](../tutorials/index.md) — Step-by-step guides for common tasks
- [Examples](../examples/index.md) — Complete working examples
- [API Reference](https://software.splatgames.de/docs/aether/aether-datafixers/) — Full API documentation
