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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * A composable rule for rewriting typed values during data migration.
 *
 * <p>A {@code TypeRewriteRule} is the fundamental building block of the data fixing system.
 * It represents a transformation that may or may not apply to a given typed value. Rules can be composed, chained, and
 * filtered to build complex migration logic from simple, reusable pieces.</p>
 *
 * <h2>Core Concept</h2>
 * <p>A rewrite rule takes a typed value and either:</p>
 * <ul>
 *   <li>Returns a transformed value (wrapped in {@link Optional})</li>
 *   <li>Returns empty if the rule doesn't apply to this type/value</li>
 * </ul>
 * <p>This optionality enables rules to be composed without explicit type checking.</p>
 *
 * <h2>Rule Composition</h2>
 * <p>Rules can be combined in several ways:</p>
 * <ul>
 *   <li>{@link #andThen(TypeRewriteRule)} - Sequential composition: apply first, then second</li>
 *   <li>{@link #orElse(TypeRewriteRule)} - Fallback: try first, use second if first fails</li>
 *   <li>{@link #orKeep()} - Make optional rule always succeed (keep original on failure)</li>
 *   <li>{@link #ifType(Type)} - Filter to only apply to a specific type</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a rule that renames a field in player data
 * TypeRewriteRule renameRule = TypeRewriteRule.forType(
 *     "rename_player_name",
 *     playerType,
 *     player -> player.withName(player.playerName()) // rename field
 * );
 *
 * // Create a rule that adds a default value
 * TypeRewriteRule addDefault = TypeRewriteRule.simple(
 *     "add_default_score",
 *     typed -> {
 *         if (typed.get("score") == null) {
 *             return typed.set("score", 0);
 *         }
 *         return typed;
 *     }
 * );
 *
 * // Compose rules: apply rename, then add default
 * TypeRewriteRule migration = renameRule.andThen(addDefault);
 *
 * // Apply the rule to data
 * Typed<?> result = migration.apply(inputData);
 * }</pre>
 *
 * <h2>Built-in Rules</h2>
 * <ul>
 *   <li>{@link #identity()} - Always succeeds, returns input unchanged</li>
 *   <li>{@link #fail()} - Never matches, always returns empty</li>
 *   <li>{@link #simple(String, java.util.function.Function)} - Transform without type filtering</li>
 *   <li>{@link #forType(String, Type, java.util.function.Function)} - Transform only matching types</li>
 * </ul>
 *
 * <h2>Using in DataFix</h2>
 * <p>TypeRewriteRules are typically created in {@code SchemaDataFix.makeRule()}:</p>
 * <pre>{@code
 * public class MyFix extends SchemaDataFix {
 *     protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
 *         Type<?> playerType = inputSchema.require(TypeReferences.PLAYER);
 *         return TypeRewriteRule.forType("migrate_player", playerType,
 *             player -> migratePlayerData(player)
 *         );
 *     }
 * }
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <ul>
 *   <li>{@link #apply(Typed)} - Returns original if rule doesn't match (safe)</li>
 *   <li>{@link #applyOrThrow(Typed)} - Throws if rule doesn't match (strict)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>TypeRewriteRule implementations should be stateless and thread-safe.
 * The same rule may be applied concurrently to different data instances.</p>
 *
 * @author Erik Pförtner
 * @see Type
 * @see Typed
 * @see Rules
 * @since 0.1.0
 */
@FunctionalInterface
public interface TypeRewriteRule {

    /**
     * Creates an identity rule that always succeeds and returns the input unchanged.
     *
     * <p>The identity rule is the neutral element for rule composition. It matches
     * any type and any input, returning the input exactly as given. This is useful as a default or placeholder in rule
     * chains.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule identity = TypeRewriteRule.identity();
     *
     * Typed<?> result = identity.apply(anyData);
     * // result == anyData (same object)
     *
     * // Composition with identity is a no-op
     * rule.andThen(identity) // equivalent to just rule
     * }</pre>
     *
     * @return an identity rule that always succeeds, never {@code null}
     */
    @NotNull
    static TypeRewriteRule identity() {
        return (type, input) -> Optional.of(input);
    }

    /**
     * Creates a rule that never matches any input.
     *
     * <p>The fail rule always returns {@link Optional#empty()}, indicating that
     * it doesn't apply. This is useful as a fallback sentinel or for testing rule composition behavior.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule fail = TypeRewriteRule.fail();
     *
     * Optional<Typed<?>> result = fail.rewrite(anyType, anyData);
     * // result is always Optional.empty()
     *
     * // With orElse, fail causes fallback to be tried
     * rule.orElse(TypeRewriteRule.fail()) // equivalent to just rule
     * }</pre>
     *
     * @return a rule that never matches, never {@code null}
     */
    @NotNull
    static TypeRewriteRule fail() {
        return (type, input) -> Optional.empty();
    }

    /**
     * Creates a rule from a simple transformation function that always matches.
     *
     * <p>This factory creates a rule that applies to any type without type filtering.
     * The transformer function receives the typed input and must return a typed output. The rule always succeeds (never
     * returns empty).</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // A rule that logs the data being processed
     * TypeRewriteRule logRule = TypeRewriteRule.simple("log_data",
     *     typed -> {
     *         System.out.println("Processing: " + typed.type().describe());
     *         return typed;  // Return unchanged
     *     }
     * );
     *
     * // A rule that wraps values in a container
     * TypeRewriteRule wrapRule = TypeRewriteRule.simple("wrap",
     *     typed -> new Typed<>(containerType, new Container(typed.value()))
     * );
     * }</pre>
     *
     * @param name        a descriptive name for this rule, must not be {@code null}
     * @param transformer the function to transform typed values, must not be {@code null}
     * @return a new rule that always matches, never {@code null}
     * @throws NullPointerException if {@code name} or {@code transformer} is {@code null}
     */
    @NotNull
    static TypeRewriteRule simple(
            @NotNull final String name,
            @NotNull final Function<Typed<?>, Typed<?>> transformer
    ) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(transformer, "transformer must not be null");
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                Preconditions.checkNotNull(type, "type must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                return Optional.of(transformer.apply(input));
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    /**
     * Creates a type-specific rule that only applies to values of a particular type.
     *
     * <p>This is the most commonly used factory method for data migration rules.
     * It creates a rule that matches only when the input's type reference equals the target type reference. The
     * transformer receives the unwrapped value (not the Typed wrapper) and returns the transformed value.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Migrate player data: add new field and transform existing field
     * TypeRewriteRule playerFix = TypeRewriteRule.forType(
     *     "migrate_player_v2",
     *     playerType,
     *     player -> player.withVersion(2).withHealth(player.getHP() * 2)
     * );
     *
     * // Only matches player data, returns empty for other types
     * Optional<Typed<?>> result = playerFix.rewrite(playerType, playerData);  // matches
     * Optional<Typed<?>> other = playerFix.rewrite(entityType, entityData);    // empty
     * }</pre>
     *
     * @param name        a descriptive name for this rule, must not be {@code null}
     * @param targetType  the type this rule applies to, must not be {@code null}
     * @param transformer the function to transform values of the target type, must not be {@code null}
     * @param <A>         the Java type of values this rule transforms
     * @return a new type-specific rule, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    @SuppressWarnings("unchecked")
    static <A> TypeRewriteRule forType(@NotNull final String name,
                                       @NotNull final Type<A> targetType,
                                       @NotNull final Function<A, A> transformer) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(targetType, "targetType must not be null");
        Preconditions.checkNotNull(transformer, "transformer must not be null");
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type, @NotNull final Typed<?> input) {
                Preconditions.checkNotNull(type, "type must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                if (!type.reference().equals(targetType.reference())) {
                    return Optional.empty();
                }
                final Typed<A> typed = (Typed<A>) input;
                return Optional.of(new Typed<>(targetType, transformer.apply(typed.value())));
            }

            @Override
            public String toString() {
                return name + "[" + targetType.describe() + "]";
            }
        };
    }

    /**
     * Attempts to rewrite a typed value according to this rule.
     *
     * <p>This is the core operation of the rewrite system. The rule examines
     * the input and either transforms it (returning a non-empty {@link Optional}) or indicates that it doesn't apply
     * (returning {@link Optional#empty()}).</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule rule = TypeRewriteRule.forType("double_age", personType,
     *     person -> person.withAge(person.age() * 2)
     * );
     *
     * Typed<?> person = new Typed<>(personType, new Person("Alice", 30));
     * Optional<Typed<?>> result = rule.rewrite(personType, person);
     * // result.get().value() is Person("Alice", 60)
     * }</pre>
     *
     * @param type  the type descriptor of the input, must not be {@code null}
     * @param input the typed value to potentially rewrite, must not be {@code null}
     * @return an {@link Optional} containing the rewritten value if this rule applies, or {@link Optional#empty()} if
     * the rule doesn't match; never {@code null}
     * @throws NullPointerException if {@code type} or {@code input} is {@code null}
     */
    @NotNull
    Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                               @NotNull final Typed<?> input);

    /**
     * Applies this rule to a typed value, returning the original if the rule doesn't match.
     *
     * <p>This is a safe, convenient method for applying rules where non-matching
     * rules should be treated as no-ops. It's the most commonly used application method in data migration
     * pipelines.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule rule = TypeRewriteRule.forType("fix", playerType, Player::migrate);
     *
     * // Safe application - returns original if rule doesn't match
     * Typed<?> result = rule.apply(anyTypedValue);
     * // result is either migrated (if it was a player) or unchanged
     * }</pre>
     *
     * @param input the typed value to transform, must not be {@code null}
     * @return the rewritten result if the rule matched, or the original input unchanged if the rule didn't apply; never
     * {@code null}
     * @throws NullPointerException if {@code input} is {@code null}
     */
    @NotNull
    default Typed<?> apply(@NotNull final Typed<?> input) {
        Preconditions.checkNotNull(input, "input must not be null");
        return rewrite(input.type(), input).orElse(input);
    }

    /**
     * Applies this rule, throwing an exception if it doesn't match.
     *
     * <p>Use this method when the rule is expected to always match and a non-match
     * indicates a programming error or corrupt data. This is stricter than {@link #apply(Typed)} and should be used in
     * critical migration paths.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule criticalFix = TypeRewriteRule.forType("fix", playerType, Player::migrate);
     *
     * // Strict application - fails if rule doesn't match
     * try {
     *     Typed<?> result = criticalFix.applyOrThrow(playerData);
     * } catch (IllegalStateException e) {
     *     // Handle unexpected type mismatch
     *     logger.error("Player migration failed: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param input the typed value to transform, must not be {@code null}
     * @return the rewritten result, never {@code null}
     * @throws IllegalStateException if the rule doesn't match the input type
     * @throws NullPointerException  if {@code input} is {@code null}
     */
    @NotNull
    default Typed<?> applyOrThrow(@NotNull final Typed<?> input) {
        Preconditions.checkNotNull(input, "input must not be null");
        return rewrite(input.type(), input)
                .orElseThrow(() -> new IllegalStateException("Rule did not match: " + input.type().describe()));
    }

    /**
     * Composes this rule with another rule in sequence.
     *
     * <p>Creates a new rule that first applies this rule, then applies the next rule
     * to the result. The composed rule only succeeds if both rules succeed. This is an AND-like composition where all
     * rules in the chain must match.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule renameField = TypeRewriteRule.forType("rename", playerType,
     *     p -> p.withName(p.playerName())
     * );
     * TypeRewriteRule addScore = TypeRewriteRule.forType("add_score", playerType,
     *     p -> p.withScore(0)
     * );
     *
     * // Both rules must match for the composition to succeed
     * TypeRewriteRule migration = renameField.andThen(addScore);
     * Typed<?> result = migration.apply(playerData);
     * }</pre>
     *
     * @param next the rule to apply after this rule succeeds, must not be {@code null}
     * @return a composed rule that applies both rules in sequence, never {@code null}
     * @throws NullPointerException if {@code next} is {@code null}
     */
    @NotNull
    default TypeRewriteRule andThen(@NotNull final TypeRewriteRule next) {
        Preconditions.checkNotNull(next, "next must not be null");
        final TypeRewriteRule self = this;
        return (type, input) -> self.rewrite(type, input)
                .flatMap(result -> next.rewrite(result.type(), result));
    }

    // ==================== Static Factory Methods ====================

    /**
     * Creates a rule that tries this rule first, falling back to another on failure.
     *
     * <p>Creates an OR-like composition where the first matching rule wins. If this
     * rule matches, its result is used. If this rule doesn't match, the fallback rule is tried instead.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule v1Fix = TypeRewriteRule.forType("v1", v1Type, this::migrateV1);
     * TypeRewriteRule v2Fix = TypeRewriteRule.forType("v2", v2Type, this::migrateV2);
     *
     * // Try v1 migration first, fall back to v2
     * TypeRewriteRule migration = v1Fix.orElse(v2Fix);
     * Typed<?> result = migration.apply(data);
     * }</pre>
     *
     * @param fallback the rule to try if this rule doesn't match, must not be {@code null}
     * @return a rule that tries this rule first, then the fallback, never {@code null}
     * @throws NullPointerException if {@code fallback} is {@code null}
     */
    @NotNull
    default TypeRewriteRule orElse(@NotNull final TypeRewriteRule fallback) {
        Preconditions.checkNotNull(fallback, "fallback must not be null");
        final TypeRewriteRule self = this;
        return (type, input) -> {
            final Optional<Typed<?>> result = self.rewrite(type, input);
            if (result.isPresent()) {
                return result;
            }
            return fallback.rewrite(type, input);
        };
    }

    /**
     * Creates a rule that always succeeds, returning the input unchanged if this rule doesn't match.
     *
     * <p>This converts a potentially-failing rule into an always-succeeding rule.
     * It's equivalent to {@code this.orElse(TypeRewriteRule.identity())} and is useful for optional transformations in
     * pipelines.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule optionalFix = TypeRewriteRule.forType("fix", oldType, this::migrate);
     *
     * // Make the fix optional - non-matching types pass through unchanged
     * TypeRewriteRule safeFix = optionalFix.orKeep();
     *
     * // safeFix.rewrite() always returns Optional.of(...), never Optional.empty()
     * Typed<?> result = safeFix.apply(anyData);  // Always succeeds
     * }</pre>
     *
     * @return a rule that always succeeds (never returns empty), never {@code null}
     */
    @NotNull
    default TypeRewriteRule orKeep() {
        final TypeRewriteRule self = this;
        return (type, input) -> Optional.of(self.rewrite(type, input).orElse(input));
    }

    /**
     * Creates a rule that only applies if the input type matches a target type.
     *
     * <p>This adds a type filter to an existing rule. The resulting rule only
     * attempts to apply the original rule when the input's type reference matches the target type reference. For
     * non-matching types, it returns empty.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // A generic transformation rule
     * TypeRewriteRule genericFix = TypeRewriteRule.simple("fix",
     *     typed -> typed.update(d -> d.createInt(d.asInt().orElse(0) + 1))
     * );
     *
     * // Restrict it to only apply to score types
     * TypeRewriteRule scoreFix = genericFix.ifType(scoreType);
     *
     * scoreFix.apply(scoreData);   // Applies the fix
     * scoreFix.apply(playerData);  // Returns playerData unchanged
     * }</pre>
     *
     * @param targetType the type that must match for the rule to apply, must not be {@code null}
     * @return a filtered rule that only applies to the specified type, never {@code null}
     * @throws NullPointerException if {@code targetType} is {@code null}
     */
    @NotNull
    default TypeRewriteRule ifType(@NotNull final Type<?> targetType) {
        Preconditions.checkNotNull(targetType, "targetType must not be null");
        final TypeRewriteRule self = this;
        return (type, input) -> {
            if (!type.reference().equals(targetType.reference())) {
                return Optional.empty();
            }
            return self.rewrite(type, input);
        };
    }

    /**
     * Creates a named wrapper around this rule for debugging and logging purposes.
     *
     * <p>The name is used in the rule's {@link Object#toString()} representation,
     * making it easier to identify rules in logs, error messages, and debugging sessions. The rule's behavior is
     * unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TypeRewriteRule anonymous = (type, input) -> Optional.of(input);
     * TypeRewriteRule named = anonymous.named("identity_rule");
     *
     * System.out.println(named);  // Prints: "identity_rule"
     * }</pre>
     *
     * @param name a descriptive name for this rule, must not be {@code null}
     * @return a named rule with the same behavior, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @NotNull
    default TypeRewriteRule named(@NotNull final String name) {
        Preconditions.checkNotNull(name, "name must not be null");
        final TypeRewriteRule self = this;
        return new TypeRewriteRule() {
            @NotNull
            @Override
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                Preconditions.checkNotNull(type, "type must not be null");
                Preconditions.checkNotNull(input, "input must not be null");
                return self.rewrite(type, input);
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
