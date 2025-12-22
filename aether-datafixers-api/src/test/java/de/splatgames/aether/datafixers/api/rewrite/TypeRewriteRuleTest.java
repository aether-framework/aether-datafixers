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

import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TypeRewriteRule}.
 */
@DisplayName("TypeRewriteRule")
class TypeRewriteRuleTest {

    @Nested
    @DisplayName("identity()")
    class IdentityRule {

        @Test
        @DisplayName("always succeeds")
        void alwaysSucceeds() {
            final TypeRewriteRule rule = TypeRewriteRule.identity();
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("returns input unchanged")
        void returnsInputUnchanged() {
            final TypeRewriteRule rule = TypeRewriteRule.identity();
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result.orElseThrow()).isSameAs(input);
        }

        @Test
        @DisplayName("works with any type")
        void worksWithAnyType() {
            final TypeRewriteRule rule = TypeRewriteRule.identity();

            assertThat(rule.rewrite(Type.STRING, new Typed<>(Type.STRING, "test"))).isPresent();
            assertThat(rule.rewrite(Type.INT, new Typed<>(Type.INT, 42))).isPresent();
            assertThat(rule.rewrite(Type.BOOL, new Typed<>(Type.BOOL, true))).isPresent();
        }
    }

    @Nested
    @DisplayName("fail()")
    class FailRule {

        @Test
        @DisplayName("always returns empty")
        void alwaysReturnsEmpty() {
            final TypeRewriteRule rule = TypeRewriteRule.fail();
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for any type")
        void returnsEmptyForAnyType() {
            final TypeRewriteRule rule = TypeRewriteRule.fail();

            assertThat(rule.rewrite(Type.STRING, new Typed<>(Type.STRING, "test"))).isEmpty();
            assertThat(rule.rewrite(Type.INT, new Typed<>(Type.INT, 42))).isEmpty();
            assertThat(rule.rewrite(Type.BOOL, new Typed<>(Type.BOOL, true))).isEmpty();
        }
    }

    @Nested
    @DisplayName("simple()")
    class SimpleRule {

        @Test
        @DisplayName("applies transformation")
        @SuppressWarnings("unchecked")
        void appliesTransformation() {
            final TypeRewriteRule rule = TypeRewriteRule.simple("uppercase",
                    typed -> {
                        final Typed<String> stringTyped = (Typed<String>) typed;
                        return stringTyped.update(String::toUpperCase);
                    });

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("always succeeds")
        void alwaysSucceeds() {
            final TypeRewriteRule rule = TypeRewriteRule.simple("noop", typed -> typed);

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("has correct toString")
        void hasCorrectToString() {
            final TypeRewriteRule rule = TypeRewriteRule.simple("my_rule", typed -> typed);

            assertThat(rule.toString()).isEqualTo("my_rule");
        }
    }

    @Nested
    @DisplayName("forType()")
    class ForTypeRule {

        @Test
        @DisplayName("matches target type")
        void matchesTargetType() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("double",
                    Type.INT,
                    n -> n * 2);

            final Typed<Integer> input = new Typed<>(Type.INT, 21);
            final Optional<Typed<?>> result = rule.rewrite(Type.INT, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo(42);
        }

        @Test
        @DisplayName("returns empty for non-matching type")
        void returnsEmptyForNonMatchingType() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("double",
                    Type.INT,
                    n -> n * 2);

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("has correct toString")
        void hasCorrectToString() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("my_rule",
                    Type.STRING,
                    s -> s);

            assertThat(rule.toString()).contains("my_rule");
            assertThat(rule.toString()).contains("string");
        }
    }

    @Nested
    @DisplayName("apply()")
    class ApplyMethod {

        @Test
        @DisplayName("returns transformed value when rule matches")
        void returnsTransformedValueWhenRuleMatches() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("upper",
                    Type.STRING,
                    String::toUpperCase);

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Typed<?> result = rule.apply(input);

            assertThat(result.value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("returns original when rule doesn't match")
        void returnsOriginalWhenRuleDoesntMatch() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("double",
                    Type.INT,
                    n -> n * 2);

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Typed<?> result = rule.apply(input);

            assertThat(result).isSameAs(input);
        }
    }

    @Nested
    @DisplayName("applyOrThrow()")
    class ApplyOrThrowMethod {

        @Test
        @DisplayName("returns transformed value when rule matches")
        void returnsTransformedValueWhenRuleMatches() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("upper",
                    Type.STRING,
                    String::toUpperCase);

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Typed<?> result = rule.applyOrThrow(input);

            assertThat(result.value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("throws when rule doesn't match")
        void throwsWhenRuleDoesntMatch() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("double",
                    Type.INT,
                    n -> n * 2);

            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            assertThatThrownBy(() -> rule.applyOrThrow(input))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Rule did not match");
        }
    }

    @Nested
    @DisplayName("andThen()")
    class AndThenComposition {

        @Test
        @DisplayName("applies rules in sequence")
        void appliesRulesInSequence() {
            final TypeRewriteRule first = TypeRewriteRule.simple("add1",
                    typed -> new Typed<>(Type.INT, ((Integer) typed.value()) + 1));
            final TypeRewriteRule second = TypeRewriteRule.simple("double",
                    typed -> new Typed<>(Type.INT, ((Integer) typed.value()) * 2));

            final TypeRewriteRule combined = first.andThen(second);
            final Typed<Integer> input = new Typed<>(Type.INT, 5);

            final Optional<Typed<?>> result = combined.rewrite(Type.INT, input);

            // (5 + 1) * 2 = 12
            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo(12);
        }

        @Test
        @DisplayName("fails if first rule fails")
        void failsIfFirstRuleFails() {
            final TypeRewriteRule first = TypeRewriteRule.fail();
            final TypeRewriteRule second = TypeRewriteRule.identity();

            final TypeRewriteRule combined = first.andThen(second);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = combined.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("fails if second rule fails")
        void failsIfSecondRuleFails() {
            final TypeRewriteRule first = TypeRewriteRule.identity();
            final TypeRewriteRule second = TypeRewriteRule.fail();

            final TypeRewriteRule combined = first.andThen(second);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = combined.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("orElse()")
    class OrElseComposition {

        @Test
        @DisplayName("uses first rule if it matches")
        void usesFirstRuleIfItMatches() {
            final TypeRewriteRule first = TypeRewriteRule.simple("first",
                    typed -> new Typed<>(Type.STRING, "first"));
            final TypeRewriteRule second = TypeRewriteRule.simple("second",
                    typed -> new Typed<>(Type.STRING, "second"));

            final TypeRewriteRule combined = first.orElse(second);
            final Typed<String> input = new Typed<>(Type.STRING, "input");

            final Optional<Typed<?>> result = combined.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("first");
        }

        @Test
        @DisplayName("uses second rule if first fails")
        void usesSecondRuleIfFirstFails() {
            final TypeRewriteRule first = TypeRewriteRule.fail();
            final TypeRewriteRule second = TypeRewriteRule.simple("second",
                    typed -> new Typed<>(Type.STRING, "second"));

            final TypeRewriteRule combined = first.orElse(second);
            final Typed<String> input = new Typed<>(Type.STRING, "input");

            final Optional<Typed<?>> result = combined.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("second");
        }

        @Test
        @DisplayName("fails if both rules fail")
        void failsIfBothRulesFail() {
            final TypeRewriteRule first = TypeRewriteRule.fail();
            final TypeRewriteRule second = TypeRewriteRule.fail();

            final TypeRewriteRule combined = first.orElse(second);
            final Typed<String> input = new Typed<>(Type.STRING, "input");

            final Optional<Typed<?>> result = combined.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("orKeep()")
    class OrKeepComposition {

        @Test
        @DisplayName("returns transformed value when rule matches")
        void returnsTransformedValueWhenRuleMatches() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("upper",
                    Type.STRING,
                    String::toUpperCase).orKeep();

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("returns original when rule doesn't match")
        void returnsOriginalWhenRuleDoesntMatch() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("double",
                    Type.INT,
                    n -> n * 2).orKeep();

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(input);
        }

        @Test
        @DisplayName("never returns empty")
        void neverReturnsEmpty() {
            final TypeRewriteRule rule = TypeRewriteRule.fail().orKeep();

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("ifType()")
    class IfTypeFilter {

        @Test
        @DisplayName("applies rule for matching type")
        void appliesRuleForMatchingType() {
            final TypeRewriteRule base = TypeRewriteRule.simple("upper",
                    typed -> new Typed<>(Type.STRING, ((String) typed.value()).toUpperCase()));
            final TypeRewriteRule filtered = base.ifType(Type.STRING);

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = filtered.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("returns empty for non-matching type")
        void returnsEmptyForNonMatchingType() {
            final TypeRewriteRule base = TypeRewriteRule.simple("upper",
                    typed -> typed);
            final TypeRewriteRule filtered = base.ifType(Type.STRING);

            final Typed<Integer> input = new Typed<>(Type.INT, 42);
            final Optional<Typed<?>> result = filtered.rewrite(Type.INT, input);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("named()")
    class NamedWrapper {

        @Test
        @DisplayName("preserves behavior")
        void preservesBehavior() {
            final TypeRewriteRule base = TypeRewriteRule.forType("double",
                    Type.INT,
                    n -> n * 2);
            final TypeRewriteRule named = base.named("my_double_rule");

            final Typed<Integer> input = new Typed<>(Type.INT, 21);

            assertThat(named.rewrite(Type.INT, input)).isPresent();
            assertThat(named.rewrite(Type.INT, input).get().value()).isEqualTo(42);
        }

        @Test
        @DisplayName("updates toString")
        void updatesToString() {
            final TypeRewriteRule base = TypeRewriteRule.identity();
            final TypeRewriteRule named = base.named("custom_name");

            assertThat(named.toString()).isEqualTo("custom_name");
        }
    }

    @Nested
    @DisplayName("Complex Compositions")
    class ComplexCompositions {

        @Test
        @DisplayName("chain of transformations")
        void chainOfTransformations() {
            final TypeRewriteRule add10 = TypeRewriteRule.simple("add10",
                    typed -> new Typed<>(Type.INT, ((Integer) typed.value()) + 10));
            final TypeRewriteRule multiply2 = TypeRewriteRule.simple("multiply2",
                    typed -> new Typed<>(Type.INT, ((Integer) typed.value()) * 2));
            final TypeRewriteRule subtract5 = TypeRewriteRule.simple("subtract5",
                    typed -> new Typed<>(Type.INT, ((Integer) typed.value()) - 5));

            final TypeRewriteRule chain = add10.andThen(multiply2).andThen(subtract5);
            final Typed<Integer> input = new Typed<>(Type.INT, 5);

            // ((5 + 10) * 2) - 5 = 25
            final Optional<Typed<?>> result = chain.rewrite(Type.INT, input);
            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo(25);
        }

        @Test
        @DisplayName("fallback chain")
        void fallbackChain() {
            final AtomicInteger attempts = new AtomicInteger(0);

            final TypeRewriteRule attempt1 = (type, input) -> {
                attempts.incrementAndGet();
                return Optional.empty();
            };
            final TypeRewriteRule attempt2 = (type, input) -> {
                attempts.incrementAndGet();
                return Optional.empty();
            };
            final TypeRewriteRule attempt3 = (type, input) -> {
                attempts.incrementAndGet();
                return Optional.of(new Typed<>(Type.STRING, "success"));
            };

            final TypeRewriteRule chain = attempt1.orElse(attempt2).orElse(attempt3);
            final Typed<String> input = new Typed<>(Type.STRING, "input");

            final Optional<Typed<?>> result = chain.rewrite(Type.STRING, input);

            assertThat(attempts.get()).isEqualTo(3);
            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("success");
        }

        @Test
        @DisplayName("type-specific with fallback")
        void typeSpecificWithFallback() {
            final TypeRewriteRule stringRule = TypeRewriteRule.forType("string",
                    Type.STRING,
                    s -> s.toUpperCase());
            final TypeRewriteRule intRule = TypeRewriteRule.forType("int",
                    Type.INT,
                    n -> n * 2);
            final TypeRewriteRule defaultRule = TypeRewriteRule.identity();

            final TypeRewriteRule combined = stringRule.orElse(intRule).orElse(defaultRule);

            // String input
            final Typed<String> stringInput = new Typed<>(Type.STRING, "hello");
            assertThat(combined.apply(stringInput).value()).isEqualTo("HELLO");

            // Int input
            final Typed<Integer> intInput = new Typed<>(Type.INT, 21);
            assertThat(combined.apply(intInput).value()).isEqualTo(42);

            // Other input falls through to identity
            final Typed<Boolean> boolInput = new Typed<>(Type.BOOL, true);
            assertThat(combined.apply(boolInput)).isSameAs(boolInput);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("identity composed with identity is identity")
        void identityComposedWithIdentityIsIdentity() {
            final TypeRewriteRule rule = TypeRewriteRule.identity()
                    .andThen(TypeRewriteRule.identity())
                    .andThen(TypeRewriteRule.identity());

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(input);
        }

        @Test
        @DisplayName("fail orElse identity is identity")
        void failOrElseIdentityIsIdentity() {
            final TypeRewriteRule rule = TypeRewriteRule.fail().orElse(TypeRewriteRule.identity());

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get()).isSameAs(input);
        }

        @Test
        @DisplayName("identity andThen fail is fail")
        void identityAndThenFailIsFail() {
            final TypeRewriteRule rule = TypeRewriteRule.identity().andThen(TypeRewriteRule.fail());

            final Typed<String> input = new Typed<>(Type.STRING, "hello");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty string transformation")
        void emptyStringTransformation() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("length",
                    Type.STRING,
                    s -> s.length() + "");

            final Typed<String> input = new Typed<>(Type.STRING, "");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("0");
        }

        @Test
        @DisplayName("null-returning transformer (returns same value)")
        void zeroValueTransformation() {
            final TypeRewriteRule rule = TypeRewriteRule.forType("identity",
                    Type.INT,
                    n -> n);

            final Typed<Integer> input = new Typed<>(Type.INT, 0);
            final Optional<Typed<?>> result = rule.rewrite(Type.INT, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo(0);
        }
    }
}
