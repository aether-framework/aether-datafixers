# Recursive Types

Handling recursive data structures like trees and graphs.

## Defining Recursive Types

```java
// Tree node with children of the same type
registerType(TypeReferences.TREE_NODE, DSL.and(
    DSL.field("value", DSL.string()),
    DSL.field("children", DSL.list(DSL.ref(TypeReferences.TREE_NODE))),
    DSL.remainder()
));
```

## Using DSL.ref

`DSL.ref` creates a forward reference to a type being defined:

```java
// Self-referential structure
TypeTemplate treeTemplate = DSL.and(
    DSL.field("name", DSL.string()),
    DSL.optional("parent", DSL.ref(TypeReferences.TREE_NODE)),
    DSL.field("children", DSL.list(DSL.ref(TypeReferences.TREE_NODE))),
    DSL.remainder()
);
```

## Migrating Recursive Structures

```java
@Override
protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
    // Use Rules.everywhere to transform all nodes
    return Rules.everywhere(
        Rules.transform(TypeReferences.TREE_NODE, this::migrateNode)
    );
}

private Dynamic<?> migrateNode(Dynamic<?> node) {
    // Migrate this node (children are handled recursively)
    return node.set("nodeId", node.createString(generateId()));
}
```

## Depth Limiting

For very deep structures, limit recursion:

```java
private Dynamic<?> migrateWithDepth(Dynamic<?> node, int maxDepth) {
    if (maxDepth <= 0) {
        return node;  // Stop recursing
    }

    // Migrate this node
    Dynamic<?> migrated = migrateNode(node);

    // Recursively migrate children
    return migrated.update("children", children ->
        children.updateList(child -> migrateWithDepth(child, maxDepth - 1))
    );
}
```

## Cycle Detection

For graphs with potential cycles:

```java
private Dynamic<?> migrateWithVisited(Dynamic<?> node, Set<String> visited) {
    String id = node.get("id").asString().orElse("");

    if (visited.contains(id)) {
        return node;  // Already visited, skip
    }

    visited.add(id);

    // Continue migration
    return migrateNode(node);
}
```

## Related

- [API Reference](https://software.splatgames.de/docs/aether/aether-datafixers/)
- [Traversal Strategies](traversal-strategies.md)

