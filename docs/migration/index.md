# Migration Guides

This section contains guides for upgrading between major versions of Aether Datafixers.

## Available Guides

| From   | To     | Guide                              |
|--------|--------|------------------------------------|
| v0.5.x | v1.0.0 | [Migration Guide](v0.5-to-v1.0.md) |

## Before You Migrate

1. **Back up your project** — Create a commit or backup before starting
2. **Read the full guide** — Understand all changes before making modifications
3. **Update dependencies first** — Ensure your build file references the new version
4. **Run tests after** — Verify your migrations still work correctly

## General Migration Strategy

1. Update the version in your build file (`pom.xml` or `build.gradle`)
2. Attempt to compile — note all compilation errors
3. Apply automated migration patterns from the guide
4. Fix any remaining issues manually
5. Run your test suite
6. Enable deprecation warnings to catch deprecated API usage
