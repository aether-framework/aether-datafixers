# FAQ

Frequently asked questions about Aether Datafixers.

## General

### What is Aether Datafixers?

A data migration framework for Java that handles schema evolution and data transformation between versions.

### How is it different from Minecraft's DFU?

Aether Datafixers is inspired by DFU but aims to be more lightweight and straightforward, with cleaner APIs and better documentation.

### What Java version is required?

Java 17 or higher.

## Usage

### Do I need to migrate all data at once?

No. You can migrate individual records on-demand when they're loaded.

### Can I migrate backwards (downgrade)?

The framework is designed for forward migration. Downgrading would require separate fixes.

### How do I store the version number with my data?

Add a `_version` or similar field to your data structure:

```java
player.set("_version", player.createInt(currentVersion.version()));
```

### What happens if a fix fails?

A `MigrationException` is thrown. Use try-catch to handle failures gracefully.

## Schemas

### Do I need a schema for every version?

Only for versions where the type structure changes. Schema inheritance handles unchanged types.

### Can I skip versions?

Yes. The fixer automatically chains fixes: v1 → v2 → v3.

### What is DSL.remainder()?

It captures and preserves unknown fields that aren't explicitly defined.

## Fixes

### How many fixes should I have?

One fix per version transition, containing all changes for that transition.

### Can one fix handle multiple types?

Yes. Use `Rules.seq()` to combine rules for different types.

### Should fixes be stateless?

Yes. Fixes should be immutable and stateless for thread safety.

## Performance

### Is it thread-safe?

Yes. The `DataFixer` is immutable and safe for concurrent use.

### How does it scale?

Well. The framework is designed for efficiency and can handle large datasets with parallel processing.

## Related

- [Common Errors](common-errors.md)
- [Getting Started](../getting-started/index.md)

