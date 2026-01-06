/*
 * Copyright (c) 2025 Splatgames.de Software and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.splatgames.aether.datafixers.api.rewrite;

import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.optic.Finder;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.jetbrains.annotations.NotNull;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Factory class providing common combinators for building {@link TypeRewriteRule} instances.
 *
 * <p>The {@code Rules} class is a comprehensive toolkit for constructing data migration rules.
 * It provides a rich set of combinators that allow complex migration logic to be built from
 * simple, composable primitives. These combinators follow functional programming patterns
 * and enable declarative specification of data transformations.</p>
 *
 * <h2>Combinator Categories</h2>
 * <ul>
 *   <li><strong>Basic Combinators:</strong> {@link #seq}, {@link #seqAll}, {@link #choice},
 *       {@link #checkOnce}, {@link #tryOnce}</li>
 *   <li><strong>Traversal Combinators:</strong> {@link #all}, {@link #one}, {@link #everywhere},
 *       {@link #bottomUp}, {@link #topDown}</li>
 *   <li><strong>Type-Specific:</strong> {@link #ifType}, {@link #transformType}</li>
 *   <li><strong>Field Operations:</strong> {@link #renameField}, {@link #removeField},
 *       {@link #addField}, {@link #transformField}</li>
 *   <li><strong>Utilities:</strong> {@link #noop}, {@link #log}</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build a complex migration rule using combinators
 * TypeRewriteRule migration = Rules.seq(
 *     // First, rename the old field
 *     Rules.renameField(GsonOps.INSTANCE, "playerName", "name"),
 *
 *     // Then add a default score if missing
 *     Rules.addField(GsonOps.INSTANCE, "score",
 *         new Dynamic<>(GsonOps.INSTANCE, JsonPrimitive(0))),
 *
 *     // Finally, transform the level field
 *     Rules.transformField(GsonOps.INSTANCE, "level",
 *         d -> d.createInt(d.asInt().orElse(0) + 1))
 * );
 *
 * // Apply the migration
 * Typed<?> result = migration.apply(inputData);
 * }</pre>
 *
 * <h2>Sequencing vs Choice</h2>
 * <ul>
 *   <li>{@link #seq} - All rules must succeed (AND-like)</li>
 *   <li>{@link #seqAll} - Apply all rules, continue on failure (forgiving AND)</li>
 *   <li>{@link #choice} - First successful rule wins (OR-like)</li>
 * </ul>
 *
 * <h2>Traversal Strategies</h2>
 * <p>For recursive data structures:</p>
 * <ul>
 *   <li>{@link #topDown} - Apply rule to parent first, then children</li>
 *   <li>{@link #bottomUp} - Apply rule to children first, then parent</li>
 *   <li>{@link #everywhere} - Apply rule at all levels</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All factory methods return stateless, thread-safe rules. The same rule
 * instance can be used concurrently for multiple migrations.</p>
 *
 * @author Erik Pförtner
 * @see TypeRewriteRule
 * @see Finder
 * @since 0.1.0
 */
public final class Rules {

    /**
     * Cache for parsed path finders to avoid repeated regex parsing.
     * Thread-safe via ConcurrentHashMap.
     *
     * @since 0.2.0
     */
    private static final Map<String, Finder<?>> PATH_CACHE = new ConcurrentHashMap<>();

    private Rules() {
        // private constructor to prevent instantiation
    }

    // ==================== Basic Combinators ====================

    /**
     * Creates a sequence of rules that are applied in order (strict AND composition).
     *
     * <p>All rules in the sequence must match for the combined rule to succeed.
     * If any rule returns empty, the entire sequence fails immediately. This is
     * useful when you have a pipeline of transformations that must all complete.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule migration = Rules.seq(
     *     Rules.renameField(ops, "playerName", "name"),
     *     Rules.addField(ops, "version", defaultVersion),
     *     Rules.transformField(ops, "health", d -> d.createInt(d.asInt().orElse(100)))
     * );
     *
     * // All three rules must succeed
     * Typed<?> result = migration.apply(playerData);
     * }</pre>
     *
     * @param rules the rules to apply in sequence; if empty, returns identity rule
     * @return a composed rule requiring all rules to match, never {@code null}
     * @throws NullPointerException if {@code rules} or any element is {@code null}
     */
    @NotNull
    public static TypeRewriteRule seq(@NotNull final TypeRewriteRule... rules) {
        if (rules.length == 0) {
            return TypeRewriteRule.identity();
        }
        if (rules.length == 1) {
            return rules[0];
        }
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                Typed<?> current = input;
                for (final TypeRewriteRule rule : rules) {
                    final Optional<Typed<?>> result = rule.rewrite(current.type(), current);
                    if (result.isEmpty()) {
                        return Optional.empty();
                    }
                    current = result.get();
                }
                return Optional.of(current);
            }

            @Override
            public String toString() {
                return "seq(" + Arrays.toString(rules) + ")";
            }
        };
    }

    /**
     * Creates a sequence of rules that applies all rules, ignoring non-matching ones.
     *
     * <p>Unlike {@link #seq}, this combinator continues even when rules don't match.
     * Each rule is tried against the current result, and non-matching rules are
     * simply skipped. The final result is always returned (never empty).</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Apply multiple optional fixes
     * TypeRewriteRule migration = Rules.seqAll(
     *     Rules.renameField(ops, "oldName", "newName"),    // May not match
     *     Rules.removeField(ops, "deprecated"),            // May not match
     *     Rules.transformField(ops, "version", v -> v.createInt(2))
     * );
     *
     * // Always succeeds, applies whatever rules match
     * Typed<?> result = migration.apply(data);
     * }</pre>
     *
     * @param rules the rules to try in sequence; non-matching rules are skipped
     * @return a composed rule that always succeeds, never {@code null}
     * @throws NullPointerException if {@code rules} or any element is {@code null}
     */
    @NotNull
    public static TypeRewriteRule seqAll(@NotNull final TypeRewriteRule... rules) {
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                Typed<?> current = input;
                for (final TypeRewriteRule rule : rules) {
                    current = rule.rewrite(current.type(), current).orElse(current);
                }
                return Optional.of(current);
            }

            @Override
            public String toString() {
                return "seqAll(" + Arrays.toString(rules) + ")";
            }
        };
    }

    /**
     * Creates a rule that tries each rule in order until one succeeds (OR composition).
     *
     * <p>The choice combinator implements "first match wins" semantics. Rules are
     * tried in order, and the first rule that returns a non-empty result is used.
     * If no rule matches, the combined rule returns empty.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Handle different data versions
     * TypeRewriteRule versionFix = Rules.choice(
     *     TypeRewriteRule.forType("v1_fix", v1Type, this::migrateV1),
     *     TypeRewriteRule.forType("v2_fix", v2Type, this::migrateV2),
     *     TypeRewriteRule.forType("v3_fix", v3Type, this::migrateV3)
     * );
     *
     * // Tries v1, then v2, then v3 - uses first match
     * Typed<?> result = versionFix.apply(data);
     * }</pre>
     *
     * @param rules the rules to try in order; first match wins
     * @return a composed rule that uses the first matching rule, never {@code null}
     * @throws NullPointerException if {@code rules} or any element is {@code null}
     */
    @NotNull
    public static TypeRewriteRule choice(@NotNull final TypeRewriteRule... rules) {
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                for (final TypeRewriteRule rule : rules) {
                    final Optional<Typed<?>> result = rule.rewrite(type, input);
                    if (result.isPresent()) {
                        return result;
                    }
                }
                return Optional.empty();
            }

            @Override
            public String toString() {
                return "choice(" + Arrays.toString(rules) + ")";
            }
        };
    }

    /**
     * Creates a rule that requires the wrapped rule to match exactly once.
     *
     * <p>This is a strict wrapper that passes through the rule's result unchanged.
     * It serves as documentation and a point for adding validation in the future.
     * The rule fails (returns empty) if the wrapped rule doesn't match.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule criticalFix = Rules.checkOnce(
     *     TypeRewriteRule.forType("fix", playerType, this::migratePlayer)
     * );
     *
     * // Fails if the rule doesn't match - no silent pass-through
     * Optional<Typed<?>> result = criticalFix.rewrite(type, data);
     * }</pre>
     *
     * @param rule the rule that must succeed, must not be {@code null}
     * @return a rule requiring the wrapped rule to match, never {@code null}
     * @throws NullPointerException if {@code rule} is {@code null}
     */
    @NotNull
    public static TypeRewriteRule checkOnce(@NotNull final TypeRewriteRule rule) {
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                return rule.rewrite(type, input);
            }

            @Override
            public String toString() {
                return "checkOnce(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that tries to apply another rule, returning the input unchanged on failure.
     *
     * <p>This is a convenience wrapper equivalent to {@code rule.orKeep()}. It makes
     * any rule "optional" - if it matches, its result is used; if not, the input
     * passes through unchanged. The resulting rule always succeeds.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule optionalFix = Rules.tryOnce(
     *     TypeRewriteRule.forType("fix", oldType, this::migrate)
     * );
     *
     * // Always succeeds - applies fix if type matches, otherwise no-op
     * Typed<?> result = optionalFix.apply(anyData);
     * }</pre>
     *
     * @param rule the rule to try, must not be {@code null}
     * @return a rule that always succeeds, never {@code null}
     * @throws NullPointerException if {@code rule} is {@code null}
     */
    @NotNull
    public static TypeRewriteRule tryOnce(@NotNull final TypeRewriteRule rule) {
        return rule.orKeep();
    }

    // ==================== Traversal Combinators ====================

    /**
     * Creates a rule that applies a rule to all immediate children.
     *
     * <p>This combinator iterates over all child values of a typed value and
     * applies the given rule to each. If all children are successfully transformed,
     * the result is reconstructed with the new children. If any child transformation
     * fails, the entire operation fails.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Double all integer values in a list
     * Type<List<Integer>> listType = Type.list(Type.INT);
     * Typed<List<Integer>> typedList = new Typed<>(listType, List.of(1, 2, 3));
     *
     * TypeRewriteRule doubleRule = Rules.transformType("double", Type.INT, n -> n * 2);
     * TypeRewriteRule allDouble = Rules.all(GsonOps.INSTANCE, doubleRule);
     *
     * Optional<Typed<?>> result = allDouble.rewrite(listType, typedList);
     * // result contains [2, 4, 6]
     * }</pre>
     *
     * @param ops  the dynamic operations for encoding/decoding children
     * @param rule the rule to apply to each child
     * @param <T>  the dynamic type
     * @return a rule that applies the given rule to all children, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static <T> TypeRewriteRule all(@NotNull final DynamicOps<T> ops,
                                          @NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(rule, "TypeRewriteRule rule must not be null");

        return new TypeRewriteRule() {
            @NotNull
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                final DataResult<List<Typed<?>>> childrenResult = input.children(ops);
                if (childrenResult.isError()) {
                    return Optional.empty();
                }

                final List<Typed<?>> children = childrenResult.result().orElse(List.of());
                if (children.isEmpty()) {
                    // No children - return input unchanged
                    return Optional.of(input);
                }

                // Apply rule to all children
                final List<Typed<?>> newChildren = new java.util.ArrayList<>();
                for (final Typed<?> child : children) {
                    final Optional<Typed<?>> transformed = rule.rewrite(child.type(), child);
                    if (transformed.isEmpty()) {
                        return Optional.empty(); // All must succeed
                    }
                    newChildren.add(transformed.get());
                }

                // Reconstruct with new children
                final DataResult<Typed<?>> reconstructed = (DataResult<Typed<?>>) (DataResult)
                        input.withChildren(ops, newChildren);
                return reconstructed.result();
            }

            @Override
            public String toString() {
                return "all(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that applies a rule to all immediate children (without DynamicOps).
     *
     * <p>This is a convenience overload that returns a no-op rule, as child traversal
     * requires DynamicOps for encoding/decoding. Use {@link #all(DynamicOps, TypeRewriteRule)}
     * for actual child traversal.</p>
     *
     * @param rule the rule to apply to children
     * @return a rule that does nothing (no children without DynamicOps), never {@code null}
     * @deprecated Use {@link #all(DynamicOps, TypeRewriteRule)} for actual child traversal
     */
    @Deprecated
    @NotNull
    public static TypeRewriteRule all(@NotNull final TypeRewriteRule rule) {
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                // Without DynamicOps, we cannot traverse children
                return Optional.of(input);
            }

            @Override
            public String toString() {
                return "all(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that applies a rule to the first matching child.
     *
     * <p>This combinator iterates over child values and applies the rule to each
     * until one succeeds. The successful transformation is used and remaining
     * children keep their original values. If no child matches, the operation fails.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Transform the first string child found
     * TypeRewriteRule upperRule = Rules.transformType("upper", Type.STRING, String::toUpperCase);
     * TypeRewriteRule oneUpper = Rules.one(GsonOps.INSTANCE, upperRule);
     *
     * // Only the first matching child is transformed
     * }</pre>
     *
     * @param ops  the dynamic operations for encoding/decoding children
     * @param rule the rule to apply
     * @param <T>  the dynamic type
     * @return a rule that applies to the first matching child, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static <T> TypeRewriteRule one(@NotNull final DynamicOps<T> ops,
                                          @NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(rule, "TypeRewriteRule rule must not be null");

        return new TypeRewriteRule() {
            @NotNull
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                final DataResult<List<Typed<?>>> childrenResult = input.children(ops);
                if (childrenResult.isError()) {
                    return Optional.empty();
                }

                final List<Typed<?>> children = childrenResult.result().orElse(List.of());
                if (children.isEmpty()) {
                    return Optional.empty(); // No children to match
                }

                // Try to apply rule to each child, stop at first success
                final List<Typed<?>> newChildren = new java.util.ArrayList<>(children);
                boolean matched = false;
                for (int i = 0; i < children.size(); i++) {
                    final Typed<?> child = children.get(i);
                    final Optional<Typed<?>> transformed = rule.rewrite(child.type(), child);
                    if (transformed.isPresent()) {
                        newChildren.set(i, transformed.get());
                        matched = true;
                        break; // Stop at first match
                    }
                }

                if (!matched) {
                    return Optional.empty();
                }

                // Reconstruct with new children
                final DataResult<Typed<?>> reconstructed = (DataResult<Typed<?>>) (DataResult)
                        input.withChildren(ops, newChildren);
                return reconstructed.result();
            }

            @Override
            public String toString() {
                return "one(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that applies a rule to the first matching child (without DynamicOps).
     *
     * @param rule the rule to apply
     * @return a rule that always fails (no children without DynamicOps), never {@code null}
     * @deprecated Use {@link #one(DynamicOps, TypeRewriteRule)} for actual child traversal
     */
    @Deprecated
    @NotNull
    public static TypeRewriteRule one(@NotNull final TypeRewriteRule rule) {
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                // Without DynamicOps, we cannot traverse children
                return Optional.empty();
            }

            @Override
            public String toString() {
                return "one(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that recursively applies a rule everywhere in a structure.
     *
     * <p>This combinator applies the rule to the current node, then recursively
     * applies {@code everywhere(rule)} to each child. The rule is applied at
     * every level of the structure. If the rule doesn't match at a particular
     * node, traversal continues to children.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Uppercase all strings in a nested structure
     * TypeRewriteRule upperRule = Rules.transformType("upper", Type.STRING, String::toUpperCase);
     * TypeRewriteRule everywhereUpper = Rules.everywhere(GsonOps.INSTANCE, upperRule);
     *
     * // All strings at all levels will be uppercased
     * }</pre>
     *
     * @param ops  the dynamic operations for encoding/decoding
     * @param rule the rule to apply recursively
     * @param <T>  the dynamic type
     * @return a recursive rule, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static <T> TypeRewriteRule everywhere(@NotNull final DynamicOps<T> ops,
                                                 @NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(rule, "TypeRewriteRule rule must not be null");

        return new TypeRewriteRule() {
            @NotNull
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                // Apply rule to self (continue even if it doesn't match)
                Typed<?> current = rule.rewrite(type, input).orElse(input);

                // Get children
                final DataResult<List<Typed<?>>> childrenResult = current.children(ops);
                if (childrenResult.isError()) {
                    return Optional.of(current);
                }

                final List<Typed<?>> children = childrenResult.result().orElse(List.of());
                if (children.isEmpty()) {
                    return Optional.of(current);
                }

                // Recursively apply everywhere to all children
                final List<Typed<?>> newChildren = new java.util.ArrayList<>();
                for (final Typed<?> child : children) {
                    // Recursively call everywhere on each child
                    final Optional<Typed<?>> transformed = this.rewrite(child.type(), child);
                    newChildren.add(transformed.orElse(child));
                }

                // Reconstruct with new children
                final DataResult<Typed<?>> reconstructed = (DataResult<Typed<?>>) (DataResult)
                        current.withChildren(ops, newChildren);
                return reconstructed.result().or(() -> Optional.of(current));
            }

            @Override
            public String toString() {
                return "everywhere(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that recursively applies a rule everywhere (without DynamicOps).
     *
     * @param rule the rule to apply recursively
     * @return a rule that applies to self only (no child traversal without DynamicOps)
     * @deprecated Use {@link #everywhere(DynamicOps, TypeRewriteRule)} for actual recursive traversal
     */
    @Deprecated
    @NotNull
    public static TypeRewriteRule everywhere(@NotNull final TypeRewriteRule rule) {
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                // Without DynamicOps, just apply to self
                return Optional.of(rule.rewrite(type, input).orElse(input));
            }

            @Override
            public String toString() {
                return "everywhere(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that applies a rule bottom-up (children first, then parent).
     *
     * <p>This combinator first recursively applies itself to all children,
     * then applies the rule to the parent node. This is useful when the
     * parent transformation depends on child values being already transformed.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Process leaves before branches
     * TypeRewriteRule processRule = Rules.transformType("process", someType, this::process);
     * TypeRewriteRule bottomUpProcess = Rules.bottomUp(GsonOps.INSTANCE, processRule);
     *
     * // Children are processed before their parents
     * }</pre>
     *
     * @param ops  the dynamic operations for encoding/decoding
     * @param rule the rule to apply
     * @param <T>  the dynamic type
     * @return a bottom-up rule, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static <T> TypeRewriteRule bottomUp(@NotNull final DynamicOps<T> ops,
                                               @NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(rule, "TypeRewriteRule rule must not be null");

        return new TypeRewriteRule() {
            @NotNull
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                // First, recursively apply to children
                Typed<?> current = input;

                final DataResult<List<Typed<?>>> childrenResult = current.children(ops);
                if (childrenResult.result().isPresent()) {
                    final List<Typed<?>> children = childrenResult.result().get();
                    if (!children.isEmpty()) {
                        // Recursively apply bottomUp to all children
                        final List<Typed<?>> newChildren = new java.util.ArrayList<>();
                        for (final Typed<?> child : children) {
                            final Optional<Typed<?>> transformed = this.rewrite(child.type(), child);
                            newChildren.add(transformed.orElse(child));
                        }

                        // Reconstruct with new children
                        final DataResult<Typed<?>> reconstructed = (DataResult<Typed<?>>) (DataResult)
                                current.withChildren(ops, newChildren);
                        current = reconstructed.result().orElse(current);
                    }
                }

                // Then apply rule to self
                return Optional.of(rule.rewrite(current.type(), current).orElse(current));
            }

            @Override
            public String toString() {
                return "bottomUp(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that applies a rule bottom-up (without DynamicOps).
     *
     * @param rule the rule to apply
     * @return a rule that applies to self only (no child traversal without DynamicOps)
     * @deprecated Use {@link #bottomUp(DynamicOps, TypeRewriteRule)} for actual bottom-up traversal
     */
    @Deprecated
    @NotNull
    public static TypeRewriteRule bottomUp(@NotNull final TypeRewriteRule rule) {
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                // Without DynamicOps, just apply to self
                return Optional.of(rule.rewrite(type, input).orElse(input));
            }

            @Override
            public String toString() {
                return "bottomUp(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that applies a rule top-down (parent first, then children).
     *
     * <p>This combinator first applies the rule to the parent node,
     * then recursively applies itself to all children. This is useful when
     * child transformations depend on the parent being already transformed.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Process parent before children
     * TypeRewriteRule processRule = Rules.transformType("process", someType, this::process);
     * TypeRewriteRule topDownProcess = Rules.topDown(GsonOps.INSTANCE, processRule);
     *
     * // Parents are processed before their children
     * }</pre>
     *
     * @param ops  the dynamic operations for encoding/decoding
     * @param rule the rule to apply
     * @param <T>  the dynamic type
     * @return a top-down rule, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static <T> TypeRewriteRule topDown(@NotNull final DynamicOps<T> ops,
                                              @NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(rule, "TypeRewriteRule rule must not be null");

        return new TypeRewriteRule() {
            @NotNull
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                // First, apply rule to self
                Typed<?> current = rule.rewrite(type, input).orElse(input);

                // Then recursively apply to children
                final DataResult<List<Typed<?>>> childrenResult = current.children(ops);
                if (childrenResult.result().isEmpty()) {
                    return Optional.of(current);
                }

                final List<Typed<?>> children = childrenResult.result().get();
                if (children.isEmpty()) {
                    return Optional.of(current);
                }

                // Recursively apply topDown to all children
                final List<Typed<?>> newChildren = new java.util.ArrayList<>();
                for (final Typed<?> child : children) {
                    final Optional<Typed<?>> transformed = this.rewrite(child.type(), child);
                    newChildren.add(transformed.orElse(child));
                }

                // Reconstruct with new children
                final DataResult<Typed<?>> reconstructed = (DataResult<Typed<?>>) (DataResult)
                        current.withChildren(ops, newChildren);
                return reconstructed.result().or(() -> Optional.of(current));
            }

            @Override
            public String toString() {
                return "topDown(" + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that applies a rule top-down (without DynamicOps).
     *
     * @param rule the rule to apply
     * @return a rule that applies to self only (no child traversal without DynamicOps)
     * @deprecated Use {@link #topDown(DynamicOps, TypeRewriteRule)} for actual top-down traversal
     */
    @Deprecated
    @NotNull
    public static TypeRewriteRule topDown(@NotNull final TypeRewriteRule rule) {
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                // Without DynamicOps, just apply to self
                return Optional.of(rule.rewrite(type, input).orElse(input));
            }

            @Override
            public String toString() {
                return "topDown(" + rule + ")";
            }
        };
    }

    // ==================== Type-Specific Combinators ====================

    /**
     * Creates a rule that only applies to a specific type.
     *
     * @param targetType the type to match
     * @param rule       the rule to apply
     * @return a filtered rule
     */
    @NotNull
    public static TypeRewriteRule ifType(@NotNull final Type<?> targetType,
                                         @NotNull final TypeRewriteRule rule) {
        return rule.ifType(targetType);
    }

    /**
     * Creates a rule that transforms a specific type.
     *
     * @param name        the rule name
     * @param type        the type to transform
     * @param transformer the transformation
     * @param <A>         the value type
     * @return a type-specific rule
     */
    @NotNull
    public static <A> TypeRewriteRule transformType(@NotNull final String name,
                                                    @NotNull final Type<A> type,
                                                    @NotNull final Function<A, A> transformer) {
        return TypeRewriteRule.forType(name, type, transformer);
    }

    // ==================== Dynamic Transformation Combinators ====================

    /**
     * Creates a rule that transforms the dynamic representation at a specific path.
     *
     * @param <T>     the dynamic type
     * @param name    the rule name
     * @param ops     the dynamic ops
     * @param finder  the finder to locate the target
     * @param updater the update function
     * @return a rule that updates at a path
     */
    @NotNull
    public static <T> TypeRewriteRule updateAt(@NotNull final String name,
                                               @NotNull final DynamicOps<T> ops,
                                               @NotNull final Finder<?> finder,
                                               @NotNull final Function<Dynamic<?>, Dynamic<?>> updater) {
        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                                       @NotNull final Typed<?> input) {
                final DataResult<Typed<?>> updateResult = (DataResult<Typed<?>>) (DataResult) input.updateAt(ops, finder, updater);
                return updateResult.result();
            }

            @Override
            public String toString() {
                return name + "[" + finder.id() + "]";
            }
        };
    }

    /**
     * Creates a rule that renames a field in the data structure.
     *
     * <p>This rule encodes the typed value to dynamic form, renames the field
     * if present, and decodes back. If the old field doesn't exist, the data
     * is returned unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Rename "playerName" to "name" in player data
     * TypeRewriteRule renameRule = Rules.renameField(
     *     GsonOps.INSTANCE,
     *     "playerName",
     *     "name"
     * );
     *
     * // Input:  {"playerName": "Alice", "age": 30}
     * // Output: {"name": "Alice", "age": 30}
     * }</pre>
     *
     * @param <T>     the underlying data format type (e.g., JsonElement)
     * @param ops     the dynamic operations for the data format, must not be {@code null}
     * @param oldName the current field name to rename, must not be {@code null}
     * @param newName the new field name, must not be {@code null}
     * @return a rule that renames the field, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static <T> TypeRewriteRule renameField(@NotNull final DynamicOps<T> ops,
                                                  @NotNull final String oldName,
                                                  @NotNull final String newName) {
        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                                       @NotNull final Typed<?> input) {
                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                return encodeResult.flatMap(dynamic -> {
                    final Dynamic<T> value = dynamic.get(oldName);
                    final Dynamic<T> updated;
                    if (value == null) {
                        updated = dynamic;
                    } else {
                        updated = dynamic.remove(oldName).set(newName, value);
                    }
                    final Type rawType = input.type();
                    return rawType.read(updated);
                }).map(newValue -> new Typed<>((Type) input.type(), newValue)).result();
            }

            @Override
            public String toString() {
                return "renameField(" + oldName + " -> " + newName + ")";
            }
        };
    }

    /**
     * Creates a rule that removes a field from the data structure.
     *
     * <p>This rule encodes the typed value to dynamic form, removes the specified
     * field if present, and decodes back. Useful for cleaning up deprecated fields
     * during migration.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Remove deprecated "legacyScore" field
     * TypeRewriteRule removeRule = Rules.removeField(
     *     GsonOps.INSTANCE,
     *     "legacyScore"
     * );
     *
     * // Input:  {"name": "Alice", "legacyScore": 100, "score": 150}
     * // Output: {"name": "Alice", "score": 150}
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the field to remove, must not be {@code null}
     * @return a rule that removes the field, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static <T> TypeRewriteRule removeField(@NotNull final DynamicOps<T> ops,
                                                  @NotNull final String fieldName) {
        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                                       @NotNull final Typed<?> input) {
                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                return encodeResult.flatMap(dynamic -> {
                    final Dynamic<T> updated = dynamic.remove(fieldName);
                    final Type rawType = input.type();
                    return rawType.read(updated);
                }).map(newValue -> new Typed<>((Type) input.type(), newValue)).result();
            }

            @Override
            public String toString() {
                return "removeField(" + fieldName + ")";
            }
        };
    }

    /**
     * Creates a rule that adds a field with a default value if it doesn't exist.
     *
     * <p>This rule encodes the typed value to dynamic form, adds the field with
     * the default value only if the field doesn't already exist, and decodes back.
     * Existing field values are preserved.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Add "version" field with default value 1
     * TypeRewriteRule addRule = Rules.addField(
     *     GsonOps.INSTANCE,
     *     "version",
     *     new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(1))
     * );
     *
     * // Input:  {"name": "Alice"}
     * // Output: {"name": "Alice", "version": 1}
     *
     * // If field exists, it's unchanged:
     * // Input:  {"name": "Alice", "version": 2}
     * // Output: {"name": "Alice", "version": 2}
     * }</pre>
     *
     * @param <T>          the underlying data format type (e.g., JsonElement)
     * @param ops          the dynamic operations for the data format, must not be {@code null}
     * @param fieldName    the name of the field to add, must not be {@code null}
     * @param defaultValue the default value for the field, must not be {@code null}
     * @return a rule that adds the field with a default value, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static <T> TypeRewriteRule addField(@NotNull final DynamicOps<T> ops,
                                               @NotNull final String fieldName,
                                               @NotNull final Dynamic<T> defaultValue) {
        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                                       @NotNull final Typed<?> input) {
                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                return encodeResult.flatMap(dynamic -> {
                    final Dynamic<T> updated;
                    // Only add if field doesn't exist
                    if (dynamic.get(fieldName) != null) {
                        updated = dynamic;
                    } else {
                        updated = dynamic.set(fieldName, defaultValue);
                    }
                    final Type rawType = input.type();
                    return rawType.read(updated);
                }).map(newValue -> new Typed<>((Type) input.type(), newValue)).result();
            }

            @Override
            public String toString() {
                return "addField(" + fieldName + ")";
            }
        };
    }

    /**
     * Creates a rule that transforms the value of a specific field.
     *
     * <p>This rule uses a {@link Finder} to locate the field and applies the
     * transformation function to its value. The transformation is only applied
     * if the field exists.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Double the value of the "score" field
     * TypeRewriteRule transformRule = Rules.transformField(
     *     GsonOps.INSTANCE,
     *     "score",
     *     d -> d.createInt(d.asInt().orElse(0) * 2)
     * );
     *
     * // Input:  {"name": "Alice", "score": 50}
     * // Output: {"name": "Alice", "score": 100}
     *
     * // Convert string level to integer
     * TypeRewriteRule levelFix = Rules.transformField(
     *     GsonOps.INSTANCE,
     *     "level",
     *     d -> d.createInt(Integer.parseInt(d.asString().orElse("1")))
     * );
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the field to transform, must not be {@code null}
     * @param transform the function to transform the field's value, must not be {@code null}
     * @return a rule that transforms the field value, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static <T> TypeRewriteRule transformField(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final String fieldName,
                                                     @NotNull final Function<Dynamic<?>, Dynamic<?>> transform) {
        return updateAt(
                "transformField(" + fieldName + ")",
                ops,
                Finder.field(fieldName),
                transform
        );
    }

    // ==================== Batch Operations ====================

    /**
     * Creates a rule that applies multiple field operations in a single pass.
     *
     * <p>This is significantly more efficient than chaining multiple individual rules
     * (e.g., via {@link #seq}) because it performs all operations in a single
     * encode/decode cycle instead of one cycle per operation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Instead of 4 encode/decode cycles:
     * // Rules.seq(
     * //     Rules.renameField(ops, "playerName", "name"),
     * //     Rules.renameField(ops, "xp", "experience"),
     * //     Rules.removeField(ops, "deprecated"),
     * //     Rules.addField(ops, "version", defaultVersion)
     * // )
     *
     * // Use batch for just 1 encode/decode cycle:
     * TypeRewriteRule batchRule = Rules.batch(GsonOps.INSTANCE, b -> b
     *     .rename("playerName", "name")
     *     .rename("xp", "experience")
     *     .remove("deprecated")
     *     .set("version", d -> d.createInt(2))
     * );
     * }</pre>
     *
     * @param <T>     the underlying data format type (e.g., JsonElement)
     * @param ops     the dynamic operations for the data format, must not be {@code null}
     * @param builder a consumer that configures the batch operations, must not be {@code null}
     * @return a rule that applies all operations in a single pass, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @see BatchTransform
     * @since 0.3.0
     */
    @NotNull
    public static <T> TypeRewriteRule batch(@NotNull final DynamicOps<T> ops,
                                            @NotNull final Consumer<BatchTransform<T>> builder) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(builder, "Consumer builder must not be null");

        final BatchTransform<T> batch = new BatchTransform<>(ops);
        builder.accept(batch);

        if (batch.isEmpty()) {
            return TypeRewriteRule.identity();
        }

        return dynamicTransform("batch[" + batch.size() + " ops]", ops, dynamic -> {
            @SuppressWarnings("unchecked")
            final Dynamic<T> typedDynamic = (Dynamic<T>) dynamic;
            return batch.apply(typedDynamic);
        });
    }

    // ==================== Extended Dynamic Transformation Combinators ====================

    /**
     * Creates a rule that applies a custom transformation function to the dynamic representation.
     *
     * <p>This is the general-purpose combinator for custom transformations. It encodes the
     * typed value to dynamic form, applies the transformation function, and decodes back.
     * Use this when the built-in combinators don't cover your use case.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Compute a level field from experience
     * TypeRewriteRule computeLevel = Rules.dynamicTransform(
     *     "computeLevel",
     *     GsonOps.INSTANCE,
     *     dynamic -> {
     *         int xp = dynamic.get("experience").asInt().result().orElse(0);
     *         int level = Math.max(1, (int) Math.sqrt(xp / 100.0));
     *         return dynamic.set("level", dynamic.createInt(level));
     *     }
     * );
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param name      a descriptive name for debugging and logging, must not be {@code null}
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param transform the transformation function to apply, must not be {@code null}
     * @return a rule that applies the custom transformation, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule dynamicTransform(@NotNull final String name,
                                                       @NotNull final DynamicOps<T> ops,
                                                       @NotNull final Function<Dynamic<?>, Dynamic<?>> transform) {
        Preconditions.checkNotNull(name, "String name must not be null");
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(transform, "Function transform must not be null");

        return new TypeRewriteRule() {
            @Override
            @NotNull
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                return input.encode(ops).flatMap(dynamic -> {
                    final Dynamic<?> transformed = transform.apply(dynamic);
                    final Type rawType = input.type();
                    return rawType.read(transformed);
                }).map(value -> new Typed<>((Type) input.type(), value)).result();
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Creates a rule that sets a field to a value, regardless of whether it exists.
     *
     * <p>Unlike {@link #addField} which only adds the field if it doesn't exist,
     * this method always sets the field to the specified value, overwriting any
     * existing value.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Always set version to 2
     * TypeRewriteRule setVersion = Rules.setField(
     *     GsonOps.INSTANCE,
     *     "version",
     *     new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(2))
     * );
     *
     * // Input:  {"name": "Alice", "version": 1}
     * // Output: {"name": "Alice", "version": 2}
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the field to set, must not be {@code null}
     * @param value     the value to set, must not be {@code null}
     * @return a rule that sets the field, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule setField(@NotNull final DynamicOps<T> ops,
                                               @NotNull final String fieldName,
                                               @NotNull final Dynamic<T> value) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(fieldName, "String fieldName must not be null");
        Preconditions.checkNotNull(value, "Dynamic<T> value must not be null");

        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                                       @NotNull final Typed<?> input) {
                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                return encodeResult.flatMap(dynamic -> {
                    final Dynamic<T> updated = dynamic.set(fieldName, value);
                    final Type rawType = input.type();
                    return rawType.read(updated);
                }).map(newValue -> new Typed<>((Type) input.type(), newValue)).result();
            }

            @Override
            public String toString() {
                return "setField(" + fieldName + ")";
            }
        };
    }

    /**
     * Creates a rule that renames multiple fields in a single pass.
     *
     * <p>This is more efficient than chaining multiple {@link #renameField} calls
     * as it processes all renames in a single encode/decode cycle.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Rename multiple fields at once
     * TypeRewriteRule renames = Rules.renameFields(
     *     GsonOps.INSTANCE,
     *     Map.of(
     *         "playerName", "name",
     *         "xp", "experience",
     *         "oldHealth", "health"
     *     )
     * );
     *
     * // Input:  {"playerName": "Steve", "xp": 1500, "oldHealth": 20}
     * // Output: {"name": "Steve", "experience": 1500, "health": 20}
     * }</pre>
     *
     * @param <T>     the underlying data format type (e.g., JsonElement)
     * @param ops     the dynamic operations for the data format, must not be {@code null}
     * @param renames a map from old field names to new field names, must not be {@code null}
     * @return a rule that renames all specified fields, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule renameFields(@NotNull final DynamicOps<T> ops,
                                                   @NotNull final java.util.Map<String, String> renames) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(renames, "Map renames must not be null");

        if (renames.isEmpty()) {
            return TypeRewriteRule.identity();
        }

        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                                       @NotNull final Typed<?> input) {
                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                return encodeResult.flatMap(dynamic -> {
                    Dynamic<T> current = dynamic;
                    for (final var entry : renames.entrySet()) {
                        final String oldName = entry.getKey();
                        final String newName = entry.getValue();
                        final Dynamic<T> value = current.get(oldName);
                        if (value != null) {
                            current = current.remove(oldName).set(newName, value);
                        }
                    }
                    final Type rawType = input.type();
                    return rawType.read(current);
                }).map(newValue -> new Typed<>((Type) input.type(), newValue)).result();
            }

            @Override
            public String toString() {
                return "renameFields(" + renames + ")";
            }
        };
    }

    /**
     * Creates a rule that removes multiple fields in a single pass.
     *
     * <p>This is more efficient than chaining multiple {@link #removeField} calls
     * as it processes all removals in a single encode/decode cycle.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Remove deprecated fields
     * TypeRewriteRule cleanup = Rules.removeFields(
     *     GsonOps.INSTANCE,
     *     "deprecated1", "deprecated2", "legacyField"
     * );
     *
     * // Input:  {"name": "Alice", "deprecated1": true, "deprecated2": "old", "legacyField": 42}
     * // Output: {"name": "Alice"}
     * }</pre>
     *
     * @param <T>        the underlying data format type (e.g., JsonElement)
     * @param ops        the dynamic operations for the data format, must not be {@code null}
     * @param fieldNames the names of the fields to remove, must not be {@code null}
     * @return a rule that removes all specified fields, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule removeFields(@NotNull final DynamicOps<T> ops,
                                                   @NotNull final String... fieldNames) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(fieldNames, "String[] fieldNames must not be null");

        if (fieldNames.length == 0) {
            return TypeRewriteRule.identity();
        }

        return new TypeRewriteRule() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public @NotNull Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                                       @NotNull final Typed<?> input) {
                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                return encodeResult.flatMap(dynamic -> {
                    Dynamic<T> current = dynamic;
                    for (final String fieldName : fieldNames) {
                        current = current.remove(fieldName);
                    }
                    final Type rawType = input.type();
                    return rawType.read(current);
                }).map(newValue -> new Typed<>((Type) input.type(), newValue)).result();
            }

            @Override
            public String toString() {
                return "removeFields(" + Arrays.toString(fieldNames) + ")";
            }
        };
    }

    // ==================== Grouping and Moving Combinators ====================

    /**
     * Creates a rule that groups multiple fields into a nested object.
     *
     * <p>This is useful when restructuring flat data into nested structures.
     * The source fields are removed from the root and placed into a new
     * nested object with the specified name.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Group coordinates into a position object
     * TypeRewriteRule groupPosition = Rules.groupFields(
     *     GsonOps.INSTANCE,
     *     "position",
     *     "x", "y", "z"
     * );
     *
     * // Input:  {"name": "Steve", "x": 100.5, "y": 64.0, "z": -200.25}
     * // Output: {"name": "Steve", "position": {"x": 100.5, "y": 64.0, "z": -200.25}}
     * }</pre>
     *
     * @param <T>          the underlying data format type (e.g., JsonElement)
     * @param ops          the dynamic operations for the data format, must not be {@code null}
     * @param targetField  the name of the new nested object, must not be {@code null}
     * @param sourceFields the fields to group into the new object, must not be {@code null}
     * @return a rule that groups the specified fields, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule groupFields(@NotNull final DynamicOps<T> ops,
                                                  @NotNull final String targetField,
                                                  @NotNull final String... sourceFields) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(targetField, "String targetField must not be null");
        Preconditions.checkNotNull(sourceFields, "String[] sourceFields must not be null");

        if (sourceFields.length == 0) {
            return TypeRewriteRule.identity();
        }

        return dynamicTransform("groupFields(" + targetField + ")", ops, dynamic -> {
            @SuppressWarnings("unchecked")
            Dynamic<T> typedDynamic = (Dynamic<T>) dynamic;

            // Create the nested object with the source fields
            Dynamic<T> nested = typedDynamic.emptyMap();
            for (final String fieldName : sourceFields) {
                final Dynamic<T> value = typedDynamic.get(fieldName);
                if (value != null) {
                    nested = nested.set(fieldName, value);
                }
            }

            // Remove source fields and add the nested object
            Dynamic<T> result = typedDynamic;
            for (final String fieldName : sourceFields) {
                result = result.remove(fieldName);
            }
            return result.set(targetField, nested);
        });
    }

    /**
     * Creates a rule that flattens a nested object's fields to the root level.
     *
     * <p>This is the inverse of {@link #groupFields}. All fields from the nested
     * object are moved to the root level, and the nested object itself is removed.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Flatten position object to root
     * TypeRewriteRule flattenPosition = Rules.flattenField(
     *     GsonOps.INSTANCE,
     *     "position"
     * );
     *
     * // Input:  {"name": "Steve", "position": {"x": 100.5, "y": 64.0, "z": -200.25}}
     * // Output: {"name": "Steve", "x": 100.5, "y": 64.0, "z": -200.25}
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the nested object to flatten, must not be {@code null}
     * @return a rule that flattens the nested object, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule flattenField(@NotNull final DynamicOps<T> ops,
                                                   @NotNull final String fieldName) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(fieldName, "String fieldName must not be null");

        return dynamicTransform("flattenField(" + fieldName + ")", ops, dynamic -> {
            @SuppressWarnings("unchecked")
            Dynamic<T> typedDynamic = (Dynamic<T>) dynamic;

            final Dynamic<T> nested = typedDynamic.get(fieldName);
            if (nested == null || !nested.isMap()) {
                return dynamic; // No nested object to flatten
            }

            // Get all entries from the nested object
            final var entriesResult = nested.asMapStream();
            if (entriesResult.isError()) {
                return dynamic;
            }

            // Remove the nested object and add its fields to root
            Dynamic<T> result = typedDynamic.remove(fieldName);
            final var entries = entriesResult.result().orElse(java.util.stream.Stream.empty()).toList();
            for (final var entry : entries) {
                final String key = entry.first().asString().result().orElse(null);
                if (key != null) {
                    @SuppressWarnings("unchecked")
                    final Dynamic<T> value = (Dynamic<T>) entry.second();
                    result = result.set(key, value);
                }
            }
            return result;
        });
    }

    /**
     * Creates a rule that moves a field from one location to another.
     *
     * <p>The source field is removed and its value is placed at the target location.
     * Both source and target support dot-notation for nested paths.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Move flat field into nested object
     * TypeRewriteRule moveX = Rules.moveField(
     *     GsonOps.INSTANCE,
     *     "x",
     *     "position.x"
     * );
     *
     * // Input:  {"name": "Steve", "x": 100.5, "position": {"y": 64.0}}
     * // Output: {"name": "Steve", "position": {"x": 100.5, "y": 64.0}}
     * }</pre>
     *
     * @param <T>        the underlying data format type (e.g., JsonElement)
     * @param ops        the dynamic operations for the data format, must not be {@code null}
     * @param sourcePath the path to the source field (dot-notation), must not be {@code null}
     * @param targetPath the path to the target field (dot-notation), must not be {@code null}
     * @return a rule that moves the field, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule moveField(@NotNull final DynamicOps<T> ops,
                                                @NotNull final String sourcePath,
                                                @NotNull final String targetPath) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(sourcePath, "String sourcePath must not be null");
        Preconditions.checkNotNull(targetPath, "String targetPath must not be null");

        final Finder<?> sourceFinder = parsePath(sourcePath);
        final Finder<?> targetFinder = parsePath(targetPath);

        return dynamicTransform("moveField(" + sourcePath + " -> " + targetPath + ")", ops, dynamic -> {
            final Dynamic<?> value = sourceFinder.get(dynamic);
            if (value == null) {
                return dynamic; // Source doesn't exist, nothing to move
            }

            // Remove source and set target
            Dynamic<?> result = removeAtPath(dynamic, sourcePath);
            return setAtPath(result, targetPath, value);
        });
    }

    /**
     * Creates a rule that copies a field from one location to another.
     *
     * <p>Unlike {@link #moveField}, this preserves the original field.
     * Both source and target support dot-notation for nested paths.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Copy name to displayName
     * TypeRewriteRule copyName = Rules.copyField(
     *     GsonOps.INSTANCE,
     *     "name",
     *     "displayName"
     * );
     *
     * // Input:  {"name": "Steve", "level": 10}
     * // Output: {"name": "Steve", "displayName": "Steve", "level": 10}
     * }</pre>
     *
     * @param <T>        the underlying data format type (e.g., JsonElement)
     * @param ops        the dynamic operations for the data format, must not be {@code null}
     * @param sourcePath the path to the source field (dot-notation), must not be {@code null}
     * @param targetPath the path to the target field (dot-notation), must not be {@code null}
     * @return a rule that copies the field, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule copyField(@NotNull final DynamicOps<T> ops,
                                                @NotNull final String sourcePath,
                                                @NotNull final String targetPath) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(sourcePath, "String sourcePath must not be null");
        Preconditions.checkNotNull(targetPath, "String targetPath must not be null");

        final Finder<?> sourceFinder = parsePath(sourcePath);

        return dynamicTransform("copyField(" + sourcePath + " -> " + targetPath + ")", ops, dynamic -> {
            final Dynamic<?> value = sourceFinder.get(dynamic);
            if (value == null) {
                return dynamic; // Source doesn't exist, nothing to copy
            }

            return setAtPath(dynamic, targetPath, value);
        });
    }

    // ==================== Path-Based Combinators ====================

    /**
     * Creates a rule that transforms a field at a nested path.
     *
     * <p>This combines {@link Finder} composition with {@link #transformField} for
     * convenient nested field transformation using dot-notation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Double the x coordinate in position
     * TypeRewriteRule doubleX = Rules.transformFieldAt(
     *     GsonOps.INSTANCE,
     *     "position.x",
     *     d -> d.createDouble(d.asDouble().result().orElse(0.0) * 2)
     * );
     *
     * // Input:  {"position": {"x": 100.0, "y": 64.0}}
     * // Output: {"position": {"x": 200.0, "y": 64.0}}
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param path      the dot-notation path to the field, must not be {@code null}
     * @param transform the transformation function, must not be {@code null}
     * @return a rule that transforms the nested field, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule transformFieldAt(@NotNull final DynamicOps<T> ops,
                                                       @NotNull final String path,
                                                       @NotNull final Function<Dynamic<?>, Dynamic<?>> transform) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(path, "String path must not be null");
        Preconditions.checkNotNull(transform, "Function transform must not be null");

        final Finder<?> finder = parsePath(path);
        return updateAt("transformFieldAt(" + path + ")", ops, finder, transform);
    }

    /**
     * Creates a rule that renames a field at a nested path.
     *
     * <p>Only the last segment of the path is renamed. The parent path remains unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Rename position.posX to position.x
     * TypeRewriteRule renameX = Rules.renameFieldAt(
     *     GsonOps.INSTANCE,
     *     "position.posX",
     *     "x"
     * );
     *
     * // Input:  {"position": {"posX": 100.0, "y": 64.0}}
     * // Output: {"position": {"x": 100.0, "y": 64.0}}
     * }</pre>
     *
     * @param <T>     the underlying data format type (e.g., JsonElement)
     * @param ops     the dynamic operations for the data format, must not be {@code null}
     * @param path    the dot-notation path to the field to rename, must not be {@code null}
     * @param newName the new name for the field (just the name, not a path), must not be {@code null}
     * @return a rule that renames the nested field, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule renameFieldAt(@NotNull final DynamicOps<T> ops,
                                                    @NotNull final String path,
                                                    @NotNull final String newName) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(path, "String path must not be null");
        Preconditions.checkNotNull(newName, "String newName must not be null");

        final int lastDot = path.lastIndexOf('.');
        if (lastDot == -1) {
            // Simple case: no nesting
            return renameField(ops, path, newName);
        }

        // Build parent path using substring (faster than split + join)
        final String parentPath = path.substring(0, lastDot);
        final String oldName = path.substring(lastDot + 1);
        final Finder<?> parentFinder = parsePath(parentPath);

        return dynamicTransform("renameFieldAt(" + path + " -> " + newName + ")", ops, dynamic -> {
            final Dynamic<?> parent = parentFinder.get(dynamic);
            if (parent == null) {
                return dynamic;
            }

            final Dynamic<?> value = parent.get(oldName);
            if (value == null) {
                return dynamic;
            }

            @SuppressWarnings("unchecked")
            final Dynamic<Object> typedParent = (Dynamic<Object>) parent;
            @SuppressWarnings("unchecked")
            final Dynamic<Object> typedValue = (Dynamic<Object>) value;

            final Dynamic<?> updatedParent = typedParent.remove(oldName).set(newName, typedValue);
            return parentFinder.set(dynamic, updatedParent);
        });
    }

    /**
     * Creates a rule that removes a field at a nested path.
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Remove deprecated nested field
     * TypeRewriteRule removeDeprecated = Rules.removeFieldAt(
     *     GsonOps.INSTANCE,
     *     "metadata.deprecated"
     * );
     *
     * // Input:  {"data": "value", "metadata": {"deprecated": true, "version": 1}}
     * // Output: {"data": "value", "metadata": {"version": 1}}
     * }</pre>
     *
     * @param <T>  the underlying data format type (e.g., JsonElement)
     * @param ops  the dynamic operations for the data format, must not be {@code null}
     * @param path the dot-notation path to the field to remove, must not be {@code null}
     * @return a rule that removes the nested field, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule removeFieldAt(@NotNull final DynamicOps<T> ops,
                                                    @NotNull final String path) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(path, "String path must not be null");

        return dynamicTransform("removeFieldAt(" + path + ")", ops,
                dynamic -> removeAtPath(dynamic, path));
    }

    /**
     * Creates a rule that adds a field at a nested path if it doesn't exist.
     *
     * <p>Parent objects are created automatically if they don't exist.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Add a new nested field
     * TypeRewriteRule addW = Rules.addFieldAt(
     *     GsonOps.INSTANCE,
     *     "position.w",
     *     new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(0.0))
     * );
     *
     * // Input:  {"position": {"x": 100.0, "y": 64.0}}
     * // Output: {"position": {"x": 100.0, "y": 64.0, "w": 0.0}}
     * }</pre>
     *
     * @param <T>          the underlying data format type (e.g., JsonElement)
     * @param ops          the dynamic operations for the data format, must not be {@code null}
     * @param path         the dot-notation path to the field, must not be {@code null}
     * @param defaultValue the default value if the field doesn't exist, must not be {@code null}
     * @return a rule that adds the nested field, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule addFieldAt(@NotNull final DynamicOps<T> ops,
                                                 @NotNull final String path,
                                                 @NotNull final Dynamic<T> defaultValue) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(path, "String path must not be null");
        Preconditions.checkNotNull(defaultValue, "Dynamic<T> defaultValue must not be null");

        final Finder<?> finder = parsePath(path);

        return dynamicTransform("addFieldAt(" + path + ")", ops, dynamic -> {
            // Only add if field doesn't exist
            if (finder.get(dynamic) != null) {
                return dynamic;
            }
            return setAtPath(dynamic, path, defaultValue);
        });
    }

    // ==================== Conditional Combinators ====================

    /**
     * Creates a rule that only executes if a field exists.
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Only migrate if legacy field exists
     * TypeRewriteRule conditionalMigrate = Rules.ifFieldExists(
     *     GsonOps.INSTANCE,
     *     "legacyField",
     *     Rules.seq(
     *         Rules.renameField(ops, "legacyField", "newField"),
     *         Rules.removeField(ops, "oldFlag")
     *     )
     * );
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the field to check, must not be {@code null}
     * @param rule      the rule to execute if the field exists, must not be {@code null}
     * @return a conditional rule, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule ifFieldExists(@NotNull final DynamicOps<T> ops,
                                                    @NotNull final String fieldName,
                                                    @NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(fieldName, "String fieldName must not be null");
        Preconditions.checkNotNull(rule, "TypeRewriteRule rule must not be null");

        return new TypeRewriteRule() {
            @Override
            @NotNull
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                final boolean fieldExists = encodeResult.result()
                        .map(dynamic -> dynamic.get(fieldName) != null)
                        .orElse(false);

                if (fieldExists) {
                    return rule.rewrite(type, input);
                } else {
                    return Optional.of(input);
                }
            }

            @Override
            public String toString() {
                return "ifFieldExists(" + fieldName + ", " + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that only executes if a field is missing.
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Add default value if field is missing
     * TypeRewriteRule addDefault = Rules.ifFieldMissing(
     *     GsonOps.INSTANCE,
     *     "version",
     *     Rules.addField(ops, "version", defaultVersion)
     * );
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the field to check, must not be {@code null}
     * @param rule      the rule to execute if the field is missing, must not be {@code null}
     * @return a conditional rule, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T> TypeRewriteRule ifFieldMissing(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final String fieldName,
                                                     @NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(fieldName, "String fieldName must not be null");
        Preconditions.checkNotNull(rule, "TypeRewriteRule rule must not be null");

        return new TypeRewriteRule() {
            @Override
            @NotNull
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                final boolean fieldMissing = encodeResult.result()
                        .map(dynamic -> dynamic.get(fieldName) == null)
                        .orElse(true);

                if (fieldMissing) {
                    return rule.rewrite(type, input);
                } else {
                    return Optional.of(input);
                }
            }

            @Override
            public String toString() {
                return "ifFieldMissing(" + fieldName + ", " + rule + ")";
            }
        };
    }

    /**
     * Creates a rule that only executes if a field equals a specific value.
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Only migrate version 1 data
     * TypeRewriteRule migrateV1 = Rules.ifFieldEquals(
     *     GsonOps.INSTANCE,
     *     "version",
     *     1,
     *     migrateFromV1Rule
     * );
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param <V>       the type of the value to compare
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the field to check, must not be {@code null}
     * @param value     the value to compare against, must not be {@code null}
     * @param rule      the rule to execute if the field equals the value, must not be {@code null}
     * @return a conditional rule, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.2.0
     */
    @NotNull
    public static <T, V> TypeRewriteRule ifFieldEquals(@NotNull final DynamicOps<T> ops,
                                                       @NotNull final String fieldName,
                                                       @NotNull final V value,
                                                       @NotNull final TypeRewriteRule rule) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(fieldName, "String fieldName must not be null");
        Preconditions.checkNotNull(value, "V value must not be null");
        Preconditions.checkNotNull(rule, "TypeRewriteRule rule must not be null");

        return new TypeRewriteRule() {
            @Override
            @NotNull
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                final DataResult<Dynamic<T>> encodeResult = input.encode(ops);
                final boolean matches = encodeResult.result()
                        .map(dynamic -> {
                            final Dynamic<T> field = dynamic.get(fieldName);
                            if (field == null) {
                                return false;
                            }
                            // Try different type comparisons
                            if (value instanceof Integer) {
                                return field.asInt().result().map(v -> v.equals(value)).orElse(false);
                            } else if (value instanceof Long) {
                                return field.asLong().result().map(v -> v.equals(value)).orElse(false);
                            } else if (value instanceof Double) {
                                return field.asDouble().result().map(v -> v.equals(value)).orElse(false);
                            } else if (value instanceof Float) {
                                return field.asFloat().result().map(v -> v.equals(value)).orElse(false);
                            } else if (value instanceof Boolean) {
                                return field.asBoolean().result().map(v -> v.equals(value)).orElse(false);
                            } else if (value instanceof String) {
                                return field.asString().result().map(v -> v.equals(value)).orElse(false);
                            }
                            return false;
                        })
                        .orElse(false);

                if (matches) {
                    return rule.rewrite(type, input);
                } else {
                    return Optional.of(input);
                }
            }

            @Override
            public String toString() {
                return "ifFieldEquals(" + fieldName + " == " + value + ", " + rule + ")";
            }
        };
    }

    // ==================== Single-Pass Conditional Combinators ====================

    /**
     * Creates a rule that conditionally applies a transformation based on a predicate.
     *
     * <p>This is the most efficient conditional combinator as it performs the condition check
     * and transformation in a single encode/decode cycle. Use this when you need custom
     * condition logic.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Only migrate if specific conditions are met
     * TypeRewriteRule conditionalFix = Rules.conditionalTransform(
     *     GsonOps.INSTANCE,
     *     dynamic -> dynamic.get("type").asString().result().orElse("").equals("player"),
     *     dynamic -> dynamic.set("migrated", dynamic.createBoolean(true))
     * );
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param condition the predicate to test, must not be {@code null}
     * @param transform the transformation to apply if condition is true, must not be {@code null}
     * @return a conditional rule that operates in a single pass, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.3.0
     */
    @NotNull
    public static <T> TypeRewriteRule conditionalTransform(
            @NotNull final DynamicOps<T> ops,
            @NotNull final Predicate<Dynamic<T>> condition,
            @NotNull final Function<Dynamic<T>, Dynamic<T>> transform) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(condition, "Predicate condition must not be null");
        Preconditions.checkNotNull(transform, "Function transform must not be null");

        return dynamicTransform("conditionalTransform", ops, dynamic -> {
            @SuppressWarnings("unchecked")
            final Dynamic<T> typedDynamic = (Dynamic<T>) dynamic;
            if (condition.test(typedDynamic)) {
                return transform.apply(typedDynamic);
            }
            return dynamic;
        });
    }

    /**
     * Creates a rule that applies a transformation if a field exists (single-pass version).
     *
     * <p>This is more efficient than {@link #ifFieldExists(DynamicOps, String, TypeRewriteRule)}
     * when the nested rule would perform another encode/decode cycle. This version performs
     * the condition check and transformation in a single encode/decode cycle.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Migrate legacy field in single pass
     * TypeRewriteRule migrateLegacy = Rules.ifFieldExists(
     *     GsonOps.INSTANCE,
     *     "legacyField",
     *     dynamic -> dynamic
     *         .remove("legacyField")
     *         .set("newField", dynamic.get("legacyField"))
     * );
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the field to check, must not be {@code null}
     * @param transform the transformation to apply if field exists, must not be {@code null}
     * @return a conditional rule that operates in a single pass, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.3.0
     */
    @NotNull
    public static <T> TypeRewriteRule ifFieldExists(
            @NotNull final DynamicOps<T> ops,
            @NotNull final String fieldName,
            @NotNull final Function<Dynamic<T>, Dynamic<T>> transform) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(fieldName, "String fieldName must not be null");
        Preconditions.checkNotNull(transform, "Function transform must not be null");

        return dynamicTransform("ifFieldExists(" + fieldName + ")", ops, dynamic -> {
            @SuppressWarnings("unchecked")
            final Dynamic<T> typedDynamic = (Dynamic<T>) dynamic;
            if (typedDynamic.get(fieldName) != null) {
                return transform.apply(typedDynamic);
            }
            return dynamic;
        });
    }

    /**
     * Creates a rule that applies a transformation if a field is missing (single-pass version).
     *
     * <p>This is more efficient than {@link #ifFieldMissing(DynamicOps, String, TypeRewriteRule)}
     * when the nested rule would perform another encode/decode cycle. This version performs
     * the condition check and transformation in a single encode/decode cycle.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Add default values in single pass
     * TypeRewriteRule addDefaults = Rules.ifFieldMissing(
     *     GsonOps.INSTANCE,
     *     "version",
     *     dynamic -> dynamic
     *         .set("version", dynamic.createInt(1))
     *         .set("migrated", dynamic.createBoolean(true))
     * );
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the field to check, must not be {@code null}
     * @param transform the transformation to apply if field is missing, must not be {@code null}
     * @return a conditional rule that operates in a single pass, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.3.0
     */
    @NotNull
    public static <T> TypeRewriteRule ifFieldMissing(
            @NotNull final DynamicOps<T> ops,
            @NotNull final String fieldName,
            @NotNull final Function<Dynamic<T>, Dynamic<T>> transform) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(fieldName, "String fieldName must not be null");
        Preconditions.checkNotNull(transform, "Function transform must not be null");

        return dynamicTransform("ifFieldMissing(" + fieldName + ")", ops, dynamic -> {
            @SuppressWarnings("unchecked")
            final Dynamic<T> typedDynamic = (Dynamic<T>) dynamic;
            if (typedDynamic.get(fieldName) == null) {
                return transform.apply(typedDynamic);
            }
            return dynamic;
        });
    }

    /**
     * Creates a rule that applies a transformation if a field equals a specific value (single-pass version).
     *
     * <p>This is more efficient than {@link #ifFieldEquals(DynamicOps, String, Object, TypeRewriteRule)}
     * when the nested rule would perform another encode/decode cycle. This version performs
     * the condition check and transformation in a single encode/decode cycle.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Migrate version 1 data in single pass
     * TypeRewriteRule migrateV1 = Rules.ifFieldEquals(
     *     GsonOps.INSTANCE,
     *     "version",
     *     1,
     *     dynamic -> dynamic
     *         .set("version", dynamic.createInt(2))
     *         .set("migrated", dynamic.createBoolean(true))
     * );
     * }</pre>
     *
     * @param <T>       the underlying data format type (e.g., JsonElement)
     * @param <V>       the type of the value to compare
     * @param ops       the dynamic operations for the data format, must not be {@code null}
     * @param fieldName the name of the field to check, must not be {@code null}
     * @param value     the value to compare against, must not be {@code null}
     * @param transform the transformation to apply if field equals value, must not be {@code null}
     * @return a conditional rule that operates in a single pass, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @since 0.3.0
     */
    @NotNull
    public static <T, V> TypeRewriteRule ifFieldEquals(
            @NotNull final DynamicOps<T> ops,
            @NotNull final String fieldName,
            @NotNull final V value,
            @NotNull final Function<Dynamic<T>, Dynamic<T>> transform) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(fieldName, "String fieldName must not be null");
        Preconditions.checkNotNull(value, "V value must not be null");
        Preconditions.checkNotNull(transform, "Function transform must not be null");

        return dynamicTransform("ifFieldEquals(" + fieldName + " == " + value + ")", ops, dynamic -> {
            @SuppressWarnings("unchecked")
            final Dynamic<T> typedDynamic = (Dynamic<T>) dynamic;
            final Dynamic<T> field = typedDynamic.get(fieldName);

            if (field == null) {
                return dynamic;
            }

            // Check if field value matches
            final boolean matches = matchesValue(field, value);
            if (matches) {
                return transform.apply(typedDynamic);
            }
            return dynamic;
        });
    }

    /**
     * Checks if a Dynamic field value matches the expected value.
     * Supports Integer, Long, Double, Float, Boolean, and String comparisons.
     *
     * @param field the dynamic field to check
     * @param value the expected value
     * @param <T>   the dynamic type
     * @param <V>   the value type
     * @return true if the field value matches
     */
    private static <T, V> boolean matchesValue(@NotNull final Dynamic<T> field, @NotNull final V value) {
        if (value instanceof Integer) {
            return field.asInt().result().map(v -> v.equals(value)).orElse(false);
        } else if (value instanceof Long) {
            return field.asLong().result().map(v -> v.equals(value)).orElse(false);
        } else if (value instanceof Double) {
            return field.asDouble().result().map(v -> v.equals(value)).orElse(false);
        } else if (value instanceof Float) {
            return field.asFloat().result().map(v -> v.equals(value)).orElse(false);
        } else if (value instanceof Boolean) {
            return field.asBoolean().result().map(v -> v.equals(value)).orElse(false);
        } else if (value instanceof String) {
            return field.asString().result().map(v -> v.equals(value)).orElse(false);
        }
        return false;
    }

    // ==================== Private Helpers ====================

    /**
     * Parses a dot-notation path into a composed Finder.
     *
     * <p>Results are cached for performance. Uses character-based parsing instead of regex.</p>
     *
     * <p>Supports field names and numeric indices. For example:
     * <ul>
     *   <li>{@code "name"} → {@code Finder.field("name")}</li>
     *   <li>{@code "position.x"} → {@code Finder.field("position").then(Finder.field("x"))}</li>
     *   <li>{@code "items.0.id"} → {@code Finder.field("items").then(Finder.index(0)).then(Finder.field("id"))}</li>
     * </ul>
     *
     * @param path the dot-notation path, must not be {@code null}
     * @return a composed Finder for the path, never {@code null}
     * @since 0.2.0 - Added caching and optimized parsing
     */
    @NotNull
    private static Finder<?> parsePath(@NotNull final String path) {
        return PATH_CACHE.computeIfAbsent(path, Rules::parsePathInternal);
    }

    /**
     * Internal method that parses a path without caching.
     * Uses character-based parsing for better performance than regex.
     *
     * @param path the dot-notation path, must not be {@code null}
     * @return a composed Finder for the path, never {@code null}
     */
    @NotNull
    private static Finder<?> parsePathInternal(@NotNull final String path) {
        Finder<?> finder = Finder.identity();
        int start = 0;
        final int length = path.length();

        for (int i = 0; i <= length; i++) {
            if (i == length || path.charAt(i) == '.') {
                if (i > start) {
                    final String part = path.substring(start, i);
                    finder = isNumeric(part)
                            ? finder.then(Finder.index(Integer.parseInt(part)))
                            : finder.then(Finder.field(part));
                }
                start = i + 1;
            }
        }
        return finder;
    }

    /**
     * Checks if a string contains only digit characters.
     * More efficient than regex for simple numeric checks.
     *
     * @param s the string to check, must not be {@code null}
     * @return {@code true} if the string is non-empty and all characters are digits
     */
    private static boolean isNumeric(@NotNull final String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes a value at a nested path.
     *
     * @param dynamic the root dynamic, must not be {@code null}
     * @param path    the dot-notation path, must not be {@code null}
     * @return the dynamic with the value removed
     */
    @NotNull
    private static Dynamic<?> removeAtPath(@NotNull final Dynamic<?> dynamic,
                                           @NotNull final String path) {
        final int lastDot = path.lastIndexOf('.');
        if (lastDot == -1) {
            // Simple case: no nesting
            return dynamic.remove(path);
        }

        // Build parent path using substring (faster than split + join)
        final String parentPath = path.substring(0, lastDot);
        final String fieldName = path.substring(lastDot + 1);
        final Finder<?> parentFinder = parsePath(parentPath);

        final Dynamic<?> parent = parentFinder.get(dynamic);
        if (parent == null) {
            return dynamic;
        }

        final Dynamic<?> updatedParent = parent.remove(fieldName);
        return parentFinder.set(dynamic, updatedParent);
    }

    /**
     * Sets a value at a nested path, creating parent objects as needed.
     *
     * @param dynamic the root dynamic, must not be {@code null}
     * @param path    the dot-notation path, must not be {@code null}
     * @param value   the value to set, must not be {@code null}
     * @return the dynamic with the value set
     */
    @NotNull
    @SuppressWarnings("unchecked")
    private static Dynamic<?> setAtPath(@NotNull final Dynamic<?> dynamic,
                                        @NotNull final String path,
                                        @NotNull final Dynamic<?> value) {
        final int firstDot = path.indexOf('.');
        if (firstDot == -1) {
            // Simple case: no nesting
            return ((Dynamic<Object>) dynamic).set(path, (Dynamic<Object>) value);
        }

        // Navigate and create parent objects as needed - use split only when needed
        final String[] parts = splitPath(path);
        return setAtPathRecursive((Dynamic<Object>) dynamic, parts, 0, (Dynamic<Object>) value);
    }

    /**
     * Splits a path by dots without using regex.
     * More efficient than String.split("\\\\.")
     *
     * @param path the dot-notation path, must not be {@code null}
     * @return an array of path segments
     */
    @NotNull
    private static String[] splitPath(@NotNull final String path) {
        // Count dots to pre-size array
        int dotCount = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '.') {
                dotCount++;
            }
        }

        final String[] parts = new String[dotCount + 1];
        int partIndex = 0;
        int start = 0;

        for (int i = 0; i <= path.length(); i++) {
            if (i == path.length() || path.charAt(i) == '.') {
                parts[partIndex++] = path.substring(start, i);
                start = i + 1;
            }
        }
        return parts;
    }

    /**
     * Recursively sets a value at a path, creating intermediate objects.
     */
    @NotNull
    private static Dynamic<Object> setAtPathRecursive(@NotNull final Dynamic<Object> dynamic,
                                                      @NotNull final String[] parts,
                                                      final int index,
                                                      @NotNull final Dynamic<Object> value) {
        if (index == parts.length - 1) {
            // Last part - set the value
            return dynamic.set(parts[index], value);
        }

        // Get or create intermediate object
        final String part = parts[index];
        Dynamic<Object> child = dynamic.get(part);
        if (child == null) {
            child = dynamic.emptyMap();
        }

        // Recursively set in child
        final Dynamic<Object> updatedChild = setAtPathRecursive(child, parts, index + 1, value);
        return dynamic.set(part, updatedChild);
    }

    // ==================== Noop and Debug ====================

    /**
     * Creates the identity rule.
     *
     * @return an identity rule
     */
    @NotNull
    public static TypeRewriteRule noop() {
        return TypeRewriteRule.identity();
    }

    /**
     * Creates a rule that logs when applied using the default System.out logger.
     *
     * <p>For production use, prefer the overload that accepts a {@link Consumer}
     * to integrate with your logging framework.</p>
     *
     * @param message the message to log
     * @param rule    the wrapped rule
     * @return a logging rule
     * @see #log(String, TypeRewriteRule, Consumer)
     */
    @NotNull
    public static TypeRewriteRule log(@NotNull final String message,
                                      @NotNull final TypeRewriteRule rule) {
        return log(message, rule, System.out::println);
    }

    /**
     * Creates a rule that logs when applied using a custom logger.
     *
     * <p>The logger receives formatted messages including the type description.
     * This allows integration with SLF4J, Log4j, or any other logging framework.</p>
     *
     * <p><b>Usage Example</b></p>
     * <pre>{@code
     * // With SLF4J
     * Logger logger = LoggerFactory.getLogger(MyClass.class);
     * TypeRewriteRule logged = Rules.log("Processing", rule, logger::debug);
     *
     * // With custom lambda
     * TypeRewriteRule logged = Rules.log("Processing", rule,
     *     msg -> System.err.println("[DEBUG] " + msg));
     * }</pre>
     *
     * @param message the message to log
     * @param rule    the wrapped rule
     * @param logger  the consumer that receives log messages
     * @return a logging rule
     */
    @NotNull
    public static TypeRewriteRule log(@NotNull final String message,
                                      @NotNull final TypeRewriteRule rule,
                                      @NotNull final Consumer<String> logger) {
        Preconditions.checkNotNull(message, "String message must not be null");
        Preconditions.checkNotNull(rule, "TypeRewriteRule rule must not be null");
        Preconditions.checkNotNull(logger, "Consumer<String> logger must not be null");

        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type, @NotNull final Typed<?> input) {
                logger.accept("[Rule] " + message + " on type: " + type.describe());
                return rule.rewrite(type, input);
            }

            @Override
            public String toString() {
                return "log(" + message + ", " + rule + ")";
            }
        };
    }
}
