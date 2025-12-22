# Troubleshooting

Solutions to common issues with Aether Datafixers.

## Quick Links

- [Common Errors](common-errors.md) — Error messages and solutions
- [Debugging Tips](debugging-tips.md) — Strategies for finding issues
- [FAQ](faq.md) — Frequently asked questions

## Quick Fixes

### Migration Not Applied

1. Check version numbers are correct
2. Verify fix is registered for the correct TypeReference
3. Ensure `fromVersion < toVersion`

### Field Not Found

1. Check field name spelling and case
2. Use `orElse()` for optional fields
3. Verify schema defines the field

### Type Mismatch

1. Check the field type in schema matches actual data
2. Use appropriate type conversion
3. Handle null/missing values

### Schema Not Found

1. Verify version is registered in bootstrap
2. Check version number matches data

## Getting Help

- Check [FAQ](faq.md) for common questions
- Review [examples](../examples/index.md)
- Open an issue on GitHub

## Related

- [Debug Migrations](../how-to/debug-migrations.md)
- [Test Migrations](../how-to/test-migrations.md)

