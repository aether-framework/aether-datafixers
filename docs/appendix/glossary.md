# Glossary

Terminology used in Aether Datafixers.

## Core Terms

**Codec**
: Bidirectional encoder/decoder that converts between Java types and Dynamic data.

**DataFix**
: A transformation that migrates data from one version to another.

**DataFixer**
: The main orchestrator that applies DataFix instances to migrate data.

**DataResult**
: A result type representing success or failure, with optional partial results.

**DataVersion**
: An integer-based version identifier for data schemas.

**DSL**
: Domain-Specific Language for defining type templates declaratively.

**Dynamic**
: A format-agnostic data wrapper that pairs a value with its DynamicOps.

**DynamicOps**
: Interface defining operations for a specific data format (JSON, NBT, etc.).

## Type System

**Schema**
: Associates a DataVersion with a TypeRegistry, defining types for that version.

**SchemaRegistry**
: Registry that manages schemas across versions.

**Type**
: Combines a TypeReference with a Codec for typed serialization.

**TypeReference**
: String-based identifier for data types (e.g., "player", "world").

**TypeRegistry**
: Registry mapping TypeReferences to Types within a schema.

**TypeTemplate**
: DSL-based template defining type structure.

## Optics

**Optic**
: A composable way to focus on and transform parts of data structures.

**Lens**
: Optic for accessing a field in a product type (always succeeds).

**Prism**
: Optic for accessing a variant in a sum type (may fail).

**Iso**
: Optic for bidirectional conversion between isomorphic types.

**Affine**
: Optic for optional field access (combination of Lens and Prism).

**Traversal**
: Optic for focusing on multiple elements (e.g., list items).

**Getter**
: Read-only optic for extracting values.

**Finder**
: Optic for navigating Dynamic data structures.

## Rules

**TypeRewriteRule**
: A transformation rule that can be applied to typed data.

**Rules**
: Factory class providing methods to create common rewrite rules.

## Bootstrap

**DataFixerBootstrap**
: Interface for configuring schemas and fixes.

**DataFixerRuntimeFactory**
: Factory for creating DataFixer instances from a bootstrap.

## Data Structures

**Pair**
: Two-element tuple.

**Either**
: Left/right union type for representing one of two possible values.

**Unit**
: Singleton type representing void in generic contexts.

**TaggedDynamic**
: Dynamic data associated with a TypeReference.

## Testkit

**AetherAssertions**
: Entry point for custom AssertJ assertions for Dynamic, DataResult, and Typed.

**AssertingContext**
: A DataFix context that fails tests when warnings or errors are logged.

**DataFixTester**
: Test harness for isolated testing of individual DataFix implementations.

**DataFixVerification**
: Result of a DataFixTester verification, including result, context, and pass/fail status.

**DynamicAssert**
: Custom AssertJ assertions for Dynamic objects (field checks, navigation, value assertions).

**DataResultAssert**
: Custom AssertJ assertions for DataResult (success/error checking).

**MigrationTester**
: Test harness for testing complete migration chains through multiple versions.

**MockSchemas**
: Factory for creating mock Schema and SchemaRegistry instances for testing.

**QuickFix**
: Factory methods for creating common DataFix implementations without boilerplate.

**RecordingContext**
: A DataFix context that records all log and warning calls during execution.

**SchemaTester**
: Test harness for validating Schema configurations.

**TestData**
: Entry point for fluent test data builders (TestData.gson(), TestData.jackson()).

**TestDataBuilder**
: Fluent builder for creating Dynamic objects with fields.

**TestDataListBuilder**
: Fluent builder for creating Dynamic lists.

**TypedAssert**
: Custom AssertJ assertions for Typed objects (type and value checks).

## Schema Tools

**ConventionChecker**
: Validates naming conventions for types, fields, and class names.

**ConventionRules**
: Configurable naming rules for convention validation (STRICT, RELAXED, NONE, or custom).

**CoverageGap**
: Represents a schema change without a corresponding DataFix.

**DiffKind**
: Enumeration of change types: ADDED, REMOVED, MODIFIED, UNCHANGED.

**FieldDiff**
: Represents a change to a single field between schema versions.

**FieldInfo**
: Metadata about a field including name, path, optionality, and type.

**FixCoverage**
: Analysis result showing which schema changes have corresponding DataFixes.

**IssueSeverity**
: Validation issue severity levels: ERROR, WARNING, INFO.

**MigrationAnalyzer**
: Entry point for analyzing migration paths and fix coverage between versions.

**MigrationPath**
: Complete migration sequence containing all steps from source to target version.

**MigrationStep**
: Single version transition in a migration path, with optional DataFix and SchemaDiff.

**SchemaDiff**
: Result of comparing two schemas, containing added/removed types and field changes.

**SchemaDiffer**
: Fluent API for comparing schemas and generating SchemaDiff results.

**SchemaValidator**
: Fluent API for validating schema structure, conventions, and fix coverage.

**StructureValidator**
: Validates schema structural integrity (cycles, version ordering, parent chains).

**TypeDiff**
: Field-level changes for a specific type present in both compared schemas.

**TypeIntrospector**
: Utility for analyzing type structure and extracting field information.

**TypeKind**
: Classification of types: PRIMITIVE, LIST, OPTIONAL, PRODUCT, SUM, FIELD, etc.

**TypeStructure**
: Normalized, comparable representation of a Type for analysis.

**ValidationIssue**
: Single validation issue with severity, code, message, location, and context.

**ValidationResult**
: Immutable collection of validation issues with filtering and status methods.

