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

