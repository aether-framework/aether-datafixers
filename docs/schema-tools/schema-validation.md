# Schema Validation

The `schematools.validation` package provides utilities for validating schema structure, enforcing naming conventions, and identifying potential issues in your migration setup.

## Overview

Schema validation helps maintain quality and consistency:

- **Structure Validation** — Detect cycles, version ordering issues, empty schemas
- **Convention Checking** — Enforce naming patterns for types, fields, and classes
- **Fix Coverage Validation** — Ensure all schema changes have corresponding fixes

## Core Components

| Class | Description |
|-------|-------------|
| `SchemaValidator` | Main entry point with fluent API |
| `ValidationResult` | Immutable collection of validation issues |
| `ValidationIssue` | Single issue with severity, code, and message |
| `IssueSeverity` | Severity levels: `ERROR`, `WARNING`, `INFO` |
| `StructureValidator` | Validates schema structure and relationships |
| `ConventionChecker` | Checks naming conventions |
| `ConventionRules` | Configurable naming rules |

## Basic Usage

### Simple Validation

```java
import de.splatgames.aether.datafixers.schematools.validation.SchemaValidator;
import de.splatgames.aether.datafixers.schematools.validation.ValidationResult;

// Validate a bootstrap with all checks
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateAll()
    .validate();

if (result.isValid()) {
    System.out.println("All validations passed!");
} else {
    result.errors().forEach(error ->
        System.err.println("[ERROR] " + error.code() + ": " + error.message()));
    result.warnings().forEach(warning ->
        System.out.println("[WARN] " + warning.code() + ": " + warning.message()));
}
```

### Selective Validation

Enable only the validations you need:

```java
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateStructure()      // Check schema structure
    .validateConventions()    // Check naming conventions
    .validateFixCoverage()    // Check that all changes have fixes
    .validate();
```

## Creating a SchemaValidator

### From Bootstrap (Recommended)

Validates schemas, fixes, and their relationships:

```java
SchemaValidator validator = SchemaValidator.forBootstrap(bootstrap);
```

### From Schema Registry

Validates schemas only (no fix coverage):

```java
SchemaValidator validator = SchemaValidator.forRegistry(schemaRegistry);
```

### For Single Schema

Validates one schema in isolation:

```java
SchemaValidator validator = SchemaValidator.forSchema(schema);
```

## Validation Types

### Structure Validation

Checks the structural integrity of schemas:

```java
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateStructure()
    .validate();
```

**Issue Codes:**

| Code | Severity | Description |
|------|----------|-------------|
| `STRUCTURE_CYCLE` | ERROR | Schema inheritance cycle detected |
| `STRUCTURE_VERSION_ORDER` | ERROR | Parent version >= child version |
| `STRUCTURE_MISSING_PARENT` | ERROR | Referenced parent schema not found |
| `STRUCTURE_EMPTY_SCHEMA` | WARNING | Schema has no registered types |
| `STRUCTURE_NO_ROOT` | ERROR | No root schema (all schemas have parents) |

### Convention Validation

Checks naming patterns:

```java
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateConventions()
    .withConventions(ConventionRules.STRICT)  // Optional, RELAXED is default
    .validate();
```

**Issue Codes:**

| Code | Description |
|------|-------------|
| `CONVENTION_TYPE_NAME` | Type name doesn't match pattern |
| `CONVENTION_FIELD_NAME` | Field name doesn't match pattern |
| `CONVENTION_SCHEMA_CLASS` | Schema class name doesn't end with suffix |
| `CONVENTION_FIX_CLASS` | Fix class name doesn't end with suffix |

### Fix Coverage Validation

Checks that schema changes have corresponding fixes (requires bootstrap):

```java
ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateFixCoverage()
    .validate();
```

## ConventionRules

Configure naming conventions with `ConventionRules`:

### Predefined Rule Sets

```java
// Strict: snake_case, required suffixes, violations are errors
ConventionRules.STRICT

// Relaxed: Flexible naming, violations are warnings (default)
ConventionRules.RELAXED

// None: No convention checking
ConventionRules.NONE
```

### Custom Rules

```java
ConventionRules rules = ConventionRules.builder()
    .enabled(true)
    .typeNamePattern(Pattern.compile("[a-z][a-z0-9_]*"))  // snake_case
    .fieldNamePattern(Pattern.compile("[a-z][a-zA-Z0-9]*"))  // camelCase
    .schemaClassSuffix("Schema")
    .fixClassSuffix("Fix")
    .requireTypePrefix("game_")  // All types must start with "game_"
    .treatViolationsAsErrors(true)  // Violations are ERROR, not WARNING
    .build();

ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
    .validateConventions()
    .withConventions(rules)
    .validate();
```

### Custom Validators

Add completely custom validation logic:

```java
ConventionRules rules = ConventionRules.builder()
    .customTypeValidator(name -> {
        // Custom logic: type names must not exceed 30 characters
        return name.length() <= 30;
    })
    .customFieldValidator(name -> {
        // Custom logic: no "temp" or "tmp" field names
        return !name.contains("temp") && !name.contains("tmp");
    })
    .build();
```

## ValidationResult API

### Status Queries

```java
ValidationResult result = /* ... */;

// Overall status
boolean valid = result.isValid();        // No ERROR-level issues
boolean hasIssues = result.hasIssues();  // Any issues at all
boolean hasErrors = result.hasErrors();
boolean hasWarnings = result.hasWarnings();

// Counts
int totalIssues = result.issueCount();
int errorCount = result.errorCount();
int warningCount = result.warningCount();
int infoCount = result.infoCount();
```

### Filtering Issues

```java
// By severity
List<ValidationIssue> errors = result.errors();
List<ValidationIssue> warnings = result.warnings();
List<ValidationIssue> infos = result.infos();

// By code
List<ValidationIssue> cycleIssues = result.byCode("STRUCTURE_CYCLE");

// By location
List<ValidationIssue> playerIssues = result.atLocation("player");
```

### Merging Results

Combine multiple validation results:

```java
ValidationResult combined = result1.merge(result2);
```

## ValidationIssue API

```java
ValidationIssue issue = /* ... */;

// Properties
IssueSeverity severity = issue.severity();
String code = issue.code();
String message = issue.message();
Optional<String> location = issue.location();
Map<String, Object> context = issue.context();

// Type checks
boolean isError = issue.isError();
boolean isWarning = issue.isWarning();
boolean isInfo = issue.isInfo();
```

### Creating Issues

For custom validators or testing:

```java
// Factory methods
ValidationIssue error = ValidationIssue.error("MY_CODE", "Something went wrong");
ValidationIssue warning = ValidationIssue.warning("MY_CODE", "Consider this");
ValidationIssue info = ValidationIssue.info("MY_CODE", "FYI");

// With location
ValidationIssue locatedError = ValidationIssue.error("MY_CODE", "Problem here")
    .at("player.position.x");

// With context
ValidationIssue contextError = ValidationIssue.error("MY_CODE", "Invalid value")
    .withContext("value", actualValue)
    .withContext("expected", expectedValue);
```

## Example: Complete Validation Pipeline

```java
public class SchemaValidationPipeline {

    public static void validate(DataFixerBootstrap bootstrap) {
        // Create custom conventions
        ConventionRules conventions = ConventionRules.builder()
            .typeNamePattern(Pattern.compile("[a-z][a-z0-9_]*"))
            .fieldNamePattern(Pattern.compile("[a-z][a-zA-Z0-9]*"))
            .schemaClassSuffix("Schema")
            .fixClassSuffix("Fix")
            .treatViolationsAsErrors(false)  // Warnings only
            .build();

        // Run full validation
        ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
            .validateStructure()
            .validateConventions()
            .withConventions(conventions)
            .validateFixCoverage()
            .validate();

        // Report results
        printReport(result);

        // Fail on errors
        if (!result.isValid()) {
            throw new IllegalStateException(
                "Schema validation failed with " + result.errorCount() + " errors"
            );
        }
    }

    private static void printReport(ValidationResult result) {
        System.out.println("=== Schema Validation Report ===");
        System.out.println();

        if (result.isValid() && !result.hasIssues()) {
            System.out.println("All validations passed with no issues.");
            return;
        }

        // Group by severity
        if (result.hasErrors()) {
            System.out.println("ERRORS (" + result.errorCount() + "):");
            result.errors().forEach(e -> printIssue(e, "  "));
            System.out.println();
        }

        if (result.hasWarnings()) {
            System.out.println("WARNINGS (" + result.warningCount() + "):");
            result.warnings().forEach(w -> printIssue(w, "  "));
            System.out.println();
        }

        if (result.infoCount() > 0) {
            System.out.println("INFO (" + result.infoCount() + "):");
            result.infos().forEach(i -> printIssue(i, "  "));
            System.out.println();
        }

        // Summary
        System.out.println("Summary: " + result.issueCount() + " total issues " +
            "(" + result.errorCount() + " errors, " +
            result.warningCount() + " warnings, " +
            result.infoCount() + " info)");
        System.out.println("Status: " + (result.isValid() ? "PASSED" : "FAILED"));
    }

    private static void printIssue(ValidationIssue issue, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent);
        sb.append("[").append(issue.code()).append("] ");
        sb.append(issue.message());
        issue.location().ifPresent(loc -> sb.append(" @ ").append(loc));

        if (!issue.context().isEmpty()) {
            sb.append(" {");
            issue.context().forEach((k, v) ->
                sb.append(k).append("=").append(v).append(", "));
            sb.setLength(sb.length() - 2);  // Remove trailing ", "
            sb.append("}");
        }

        System.out.println(sb);
    }
}
```

## Example: JUnit Integration

Use schema validation in your test suite:

```java
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SchemaValidationTest {

    @Test
    void schemasShouldHaveValidStructure() {
        ValidationResult result = SchemaValidator.forBootstrap(new MyBootstrap())
            .validateStructure()
            .validate();

        assertThat(result.errors())
            .describedAs("Schema structure errors")
            .isEmpty();
    }

    @Test
    void schemasShouldFollowConventions() {
        ValidationResult result = SchemaValidator.forBootstrap(new MyBootstrap())
            .validateConventions()
            .withConventions(ConventionRules.STRICT)
            .validate();

        // Allow warnings, but no errors
        assertThat(result.errors())
            .describedAs("Naming convention errors")
            .isEmpty();

        // Optionally, fail on warnings too
        if (result.hasWarnings()) {
            result.warnings().forEach(System.out::println);
            fail("Schema has " + result.warningCount() + " convention warnings");
        }
    }

    @Test
    void allSchemaChangesShouldHaveFixes() {
        ValidationResult result = SchemaValidator.forBootstrap(new MyBootstrap())
            .validateFixCoverage()
            .validate();

        assertThat(result.isValid())
            .describedAs("All schema changes should have corresponding fixes")
            .isTrue();
    }
}
```

## Best Practices

1. **Run validation in CI** — Catch issues before they reach production
2. **Start with RELAXED conventions** — Tighten rules gradually as the codebase matures
3. **Use location context** — Helps identify exactly where issues occur
4. **Treat coverage gaps as errors** — Missing migrations cause data loss
5. **Document intentional deviations** — If you skip a convention, document why

---

## Related

- [Migration Analysis](migration-analysis.md) — Detailed coverage analysis
- [Schema Diffing](schema-diffing.md) — Understanding schema changes
- [Type Introspection](type-introspection.md) — How types are analyzed
