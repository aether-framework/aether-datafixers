# Traversal Strategies

Strategies for traversing and transforming nested data structures.

## Overview

Traversal strategies determine the order in which transformations are applied to nested structures.

## TopDown

Apply transformation to parent before children:

```java
TypeRewriteRule topDown = Rules.topDown(
    Rules.transform(TYPE, this::transformNode)
);
```

## BottomUp

Apply transformation to children before parent:

```java
TypeRewriteRule bottomUp = Rules.bottomUp(
    Rules.transform(TYPE, this::transformNode)
);
```

## Everywhere

Apply transformation at all levels:

```java
TypeRewriteRule everywhere = Rules.everywhere(
    Rules.transform(TYPE, this::normalize)
);
```

## When to Use Each

| Strategy | Use Case |
|----------|----------|
| TopDown | Parent determines how to process children |
| BottomUp | Result depends on transformed children |
| Everywhere | Normalize all nodes uniformly |

## Example: Normalize IDs

```java
// Normalize all "id" fields throughout the structure
TypeRewriteRule normalizeIds = Rules.everywhere(
    Rules.transformField(TYPE, "id", id -> {
        String value = id.asString().orElse("");
        return id.createString(value.toLowerCase().replace(" ", "_"));
    })
);
```

## Example: Calculate Totals

```java
// BottomUp: calculate child totals before parent
TypeRewriteRule calculateTotals = Rules.bottomUp(
    Rules.transform(TYPE, node -> {
        List<Integer> childTotals = node.get("children")
            .asList().orElse(List.of())
            .stream()
            .map(c -> c.get("total").asInt().orElse(0))
            .toList();

        int total = node.get("value").asInt().orElse(0) +
            childTotals.stream().mapToInt(i -> i).sum();

        return node.set("total", node.createInt(total));
    })
);
```

## Related

- [Rewrite Rules](../concepts/rewrite-rules.md)
- [Nested Transformations](../tutorials/nested-transformations.md)

