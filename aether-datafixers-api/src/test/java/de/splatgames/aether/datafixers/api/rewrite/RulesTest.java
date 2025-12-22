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

import de.splatgames.aether.datafixers.api.optic.TestOps;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.optic.Finder;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Rules}.
 */
@DisplayName("Rules")
class RulesTest {

    private static final TestOps OPS = TestOps.INSTANCE;

    @Nested
    @DisplayName("seq()")
    class SeqMethod {

        @Test
        @DisplayName("returns identity for empty rules")
        void returnsIdentityForEmptyRules() {
            final TypeRewriteRule rule = Rules.seq();
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("test");
        }

        @Test
        @DisplayName("returns single rule unchanged")
        void returnsSingleRuleUnchanged() {
            final TypeRewriteRule singleRule = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.seq(singleRule);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("applies rules in sequence")
        void appliesRulesInSequence() {
            final TypeRewriteRule rule1 = TypeRewriteRule.forType("trim", Type.STRING, String::trim);
            final TypeRewriteRule rule2 = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule seq = Rules.seq(rule1, rule2);
            final Typed<String> input = new Typed<>(Type.STRING, "  hello  ");

            final Optional<Typed<?>> result = seq.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("fails if any rule in sequence fails")
        void failsIfAnyRuleInSequenceFails() {
            final TypeRewriteRule rule1 = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule2 = TypeRewriteRule.fail();
            final TypeRewriteRule seq = Rules.seq(rule1, rule2);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = seq.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("fails fast on first failure")
        void failsFastOnFirstFailure() {
            final TypeRewriteRule fail = TypeRewriteRule.fail();
            final TypeRewriteRule shouldNotRun = TypeRewriteRule.forType("never", Type.STRING,
                    s -> { throw new AssertionError("Should not be called"); });
            final TypeRewriteRule seq = Rules.seq(fail, shouldNotRun);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = seq.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("has descriptive toString")
        void hasDescriptiveToString() {
            final TypeRewriteRule r1 = TypeRewriteRule.identity();
            final TypeRewriteRule r2 = TypeRewriteRule.identity();
            final TypeRewriteRule seq = Rules.seq(r1, r2);

            assertThat(seq.toString()).startsWith("seq(");
        }
    }

    @Nested
    @DisplayName("seqAll()")
    class SeqAllMethod {

        @Test
        @DisplayName("returns input unchanged for empty rules")
        void returnsInputUnchangedForEmptyRules() {
            final TypeRewriteRule rule = Rules.seqAll();
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("test");
        }

        @Test
        @DisplayName("applies all matching rules")
        void appliesAllMatchingRules() {
            final TypeRewriteRule rule1 = TypeRewriteRule.forType("trim", Type.STRING, String::trim);
            final TypeRewriteRule rule2 = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule seqAll = Rules.seqAll(rule1, rule2);
            final Typed<String> input = new Typed<>(Type.STRING, "  hello  ");

            final Optional<Typed<?>> result = seqAll.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("continues despite failing rules")
        void continuesDespiteFailingRules() {
            final TypeRewriteRule rule1 = TypeRewriteRule.fail();
            final TypeRewriteRule rule2 = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule seqAll = Rules.seqAll(rule1, rule2);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = seqAll.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("always succeeds even if all rules fail")
        void alwaysSucceedsEvenIfAllRulesFail() {
            final TypeRewriteRule rule1 = TypeRewriteRule.fail();
            final TypeRewriteRule rule2 = TypeRewriteRule.fail();
            final TypeRewriteRule seqAll = Rules.seqAll(rule1, rule2);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = seqAll.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("hello");
        }

        @Test
        @DisplayName("has descriptive toString")
        void hasDescriptiveToString() {
            final TypeRewriteRule r1 = TypeRewriteRule.identity();
            final TypeRewriteRule seqAll = Rules.seqAll(r1);

            assertThat(seqAll.toString()).startsWith("seqAll(");
        }
    }

    @Nested
    @DisplayName("choice()")
    class ChoiceMethod {

        @Test
        @DisplayName("returns empty for no rules")
        void returnsEmptyForNoRules() {
            final TypeRewriteRule rule = Rules.choice();
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("uses first matching rule")
        void usesFirstMatchingRule() {
            final TypeRewriteRule rule1 = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule2 = TypeRewriteRule.forType("lower", Type.STRING, String::toLowerCase);
            final TypeRewriteRule choice = Rules.choice(rule1, rule2);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = choice.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("tries next rule if first fails")
        void triesNextRuleIfFirstFails() {
            final TypeRewriteRule rule1 = TypeRewriteRule.fail();
            final TypeRewriteRule rule2 = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule choice = Rules.choice(rule1, rule2);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = choice.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("returns empty if no rule matches")
        void returnsEmptyIfNoRuleMatches() {
            final TypeRewriteRule rule1 = TypeRewriteRule.fail();
            final TypeRewriteRule rule2 = TypeRewriteRule.fail();
            final TypeRewriteRule choice = Rules.choice(rule1, rule2);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = choice.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("has descriptive toString")
        void hasDescriptiveToString() {
            final TypeRewriteRule r1 = TypeRewriteRule.identity();
            final TypeRewriteRule choice = Rules.choice(r1);

            assertThat(choice.toString()).startsWith("choice(");
        }
    }

    @Nested
    @DisplayName("checkOnce()")
    class CheckOnceMethod {

        @Test
        @DisplayName("passes through matching rule result")
        void passesThroughMatchingRuleResult() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.checkOnce(inner);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("fails if wrapped rule fails")
        void failsIfWrappedRuleFails() {
            final TypeRewriteRule rule = Rules.checkOnce(TypeRewriteRule.fail());
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("has descriptive toString")
        void hasDescriptiveToString() {
            final TypeRewriteRule r = TypeRewriteRule.identity();
            final TypeRewriteRule checkOnce = Rules.checkOnce(r);

            assertThat(checkOnce.toString()).startsWith("checkOnce(");
        }
    }

    @Nested
    @DisplayName("tryOnce()")
    class TryOnceMethod {

        @Test
        @DisplayName("applies matching rule")
        void appliesMatchingRule() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.tryOnce(inner);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("returns input unchanged if rule fails")
        void returnsInputUnchangedIfRuleFails() {
            final TypeRewriteRule rule = Rules.tryOnce(TypeRewriteRule.fail());
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("hello");
        }

        @Test
        @DisplayName("always succeeds")
        void alwaysSucceeds() {
            final TypeRewriteRule rule = Rules.tryOnce(TypeRewriteRule.fail());
            final Typed<Integer> input = new Typed<>(Type.INT, 42);

            final Optional<Typed<?>> result = rule.rewrite(Type.INT, input);

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("all() without DynamicOps")
    class AllWithoutDynamicOpsMethod {

        @Test
        @DisplayName("returns input unchanged (deprecated behavior)")
        @SuppressWarnings("deprecation")
        void returnsInputUnchanged() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            final TypeRewriteRule rule = Rules.all(inner);
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("test");
        }

        @Test
        @DisplayName("has descriptive toString")
        @SuppressWarnings("deprecation")
        void hasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.all(TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("all(");
        }
    }

    @Nested
    @DisplayName("one() without DynamicOps")
    class OneWithoutDynamicOpsMethod {

        @Test
        @DisplayName("returns empty (deprecated behavior)")
        @SuppressWarnings("deprecation")
        void returnsEmpty() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            final TypeRewriteRule rule = Rules.one(inner);
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("has descriptive toString")
        @SuppressWarnings("deprecation")
        void hasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.one(TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("one(");
        }
    }

    @Nested
    @DisplayName("everywhere() without DynamicOps")
    class EverywhereWithoutDynamicOpsMethod {

        @Test
        @DisplayName("applies to self only (deprecated behavior)")
        @SuppressWarnings("deprecation")
        void appliesToSelfOnly() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.everywhere(inner);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("returns input if rule doesnt match")
        @SuppressWarnings("deprecation")
        void returnsInputIfRuleDoesntMatch() {
            final TypeRewriteRule rule = Rules.everywhere(TypeRewriteRule.fail());
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("hello");
        }

        @Test
        @DisplayName("has descriptive toString")
        @SuppressWarnings("deprecation")
        void hasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.everywhere(TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("everywhere(");
        }
    }

    @Nested
    @DisplayName("bottomUp() without DynamicOps")
    class BottomUpWithoutDynamicOpsMethod {

        @Test
        @DisplayName("applies to self only (deprecated behavior)")
        @SuppressWarnings("deprecation")
        void appliesToSelfOnly() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.bottomUp(inner);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("has descriptive toString")
        @SuppressWarnings("deprecation")
        void hasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.bottomUp(TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("bottomUp(");
        }
    }

    @Nested
    @DisplayName("topDown() without DynamicOps")
    class TopDownWithoutDynamicOpsMethod {

        @Test
        @DisplayName("applies to self only (deprecated behavior)")
        @SuppressWarnings("deprecation")
        void appliesToSelfOnly() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.topDown(inner);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("has descriptive toString")
        @SuppressWarnings("deprecation")
        void hasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.topDown(TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("topDown(");
        }
    }

    @Nested
    @DisplayName("ifType()")
    class IfTypeMethod {

        @Test
        @DisplayName("applies rule for matching type")
        void appliesRuleForMatchingType() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.ifType(Type.STRING, inner);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("returns empty for non-matching type")
        void returnsEmptyForNonMatchingType() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.ifType(Type.STRING, inner);
            final Typed<Integer> input = new Typed<>(Type.INT, 42);

            final Optional<Typed<?>> result = rule.rewrite(Type.INT, input);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("transformType()")
    class TransformTypeMethod {

        @Test
        @DisplayName("transforms matching type")
        void transformsMatchingType() {
            final TypeRewriteRule rule = Rules.transformType("double", Type.INT, n -> n * 2);
            final Typed<Integer> input = new Typed<>(Type.INT, 21);

            final Optional<Typed<?>> result = rule.rewrite(Type.INT, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo(42);
        }

        @Test
        @DisplayName("returns empty for non-matching type")
        void returnsEmptyForNonMatchingType() {
            final TypeRewriteRule rule = Rules.transformType("upper", Type.STRING, String::toUpperCase);
            final Typed<Integer> input = new Typed<>(Type.INT, 42);

            final Optional<Typed<?>> result = rule.rewrite(Type.INT, input);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("noop()")
    class NoopMethod {

        @Test
        @DisplayName("returns input unchanged")
        void returnsInputUnchanged() {
            final TypeRewriteRule rule = Rules.noop();
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("test");
        }

        @Test
        @DisplayName("always succeeds")
        void alwaysSucceeds() {
            final TypeRewriteRule rule = Rules.noop();
            final Typed<Integer> input = new Typed<>(Type.INT, 42);

            final Optional<Typed<?>> result = rule.rewrite(Type.INT, input);

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("log()")
    class LogMethod {

        @Test
        @DisplayName("logs and applies wrapped rule")
        void logsAndAppliesWrappedRule() {
            final List<String> logged = new ArrayList<>();
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.log("test message", inner, logged::add);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
            assertThat(logged).hasSize(1);
            assertThat(logged.get(0)).contains("test message");
        }

        @Test
        @DisplayName("logs type description")
        void logsTypeDescription() {
            final List<String> logged = new ArrayList<>();
            final TypeRewriteRule rule = Rules.log("processing", TypeRewriteRule.identity(), logged::add);
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            rule.rewrite(Type.STRING, input);

            assertThat(logged).hasSize(1);
            assertThat(logged.get(0)).contains("string");
        }

        @Test
        @DisplayName("logs even if rule fails")
        void logsEvenIfRuleFails() {
            final List<String> logged = new ArrayList<>();
            final TypeRewriteRule rule = Rules.log("failing", TypeRewriteRule.fail(), logged::add);
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            rule.rewrite(Type.STRING, input);

            assertThat(logged).hasSize(1);
        }

        @Test
        @DisplayName("log with default logger works")
        void logWithDefaultLoggerWorks() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule rule = Rules.log("test", inner);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("has descriptive toString")
        void hasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.log("message", TypeRewriteRule.identity(), s -> {});

            assertThat(rule.toString()).startsWith("log(");
            assertThat(rule.toString()).contains("message");
        }
    }

    @Nested
    @DisplayName("Complex Compositions")
    class ComplexCompositions {

        @Test
        @DisplayName("seq with choice creates fallback pattern")
        void seqWithChoiceCreatesFallbackPattern() {
            // Try to parse as number, if fails, use default
            final TypeRewriteRule parseNumber = TypeRewriteRule.forType("parse", Type.STRING,
                    s -> {
                        try {
                            return String.valueOf(Integer.parseInt(s));
                        } catch (NumberFormatException e) {
                            throw new RuntimeException(e);
                        }
                    });
            final TypeRewriteRule defaultRule = TypeRewriteRule.forType("default", Type.STRING, s -> "0");

            final TypeRewriteRule rule = Rules.choice(
                    Rules.tryOnce(parseNumber),
                    defaultRule
            );

            final Typed<String> input = new Typed<>(Type.STRING, "42");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("42");
        }

        @Test
        @DisplayName("nested seq applies all transformations")
        void nestedSeqAppliesAllTransformations() {
            final TypeRewriteRule trim = TypeRewriteRule.forType("trim", Type.STRING, String::trim);
            final TypeRewriteRule upper = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule exclaim = TypeRewriteRule.forType("exclaim", Type.STRING, s -> s + "!");

            final TypeRewriteRule rule = Rules.seq(
                    Rules.seq(trim, upper),
                    exclaim
            );

            final Typed<String> input = new Typed<>(Type.STRING, "  hello  ");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO!");
        }

        @Test
        @DisplayName("seqAll with multiple optional rules")
        void seqAllWithMultipleOptionalRules() {
            // Only some rules will match
            final TypeRewriteRule stringRule = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);
            final TypeRewriteRule intRule = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);

            final TypeRewriteRule rule = Rules.seqAll(stringRule, intRule);
            final Typed<String> input = new Typed<>(Type.STRING, "hello");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("HELLO");
        }

        @Test
        @DisplayName("logged sequence tracks all steps")
        void loggedSequenceTracksAllSteps() {
            final List<String> logs = new ArrayList<>();

            final TypeRewriteRule step1 = Rules.log("step1",
                    TypeRewriteRule.forType("trim", Type.STRING, String::trim),
                    logs::add);
            final TypeRewriteRule step2 = Rules.log("step2",
                    TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase),
                    logs::add);

            final TypeRewriteRule rule = Rules.seq(step1, step2);
            final Typed<String> input = new Typed<>(Type.STRING, "  hello  ");

            rule.rewrite(Type.STRING, input);

            assertThat(logs).hasSize(2);
            assertThat(logs.get(0)).contains("step1");
            assertThat(logs.get(1)).contains("step2");
        }
    }

    @Nested
    @DisplayName("Traversal with DynamicOps")
    class TraversalWithDynamicOps {

        @Test
        @DisplayName("all applies rule to list elements")
        void allAppliesRuleToListElements() {
            final TypeRewriteRule doubleRule = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            final TypeRewriteRule rule = Rules.all(OPS, doubleRule);

            final Type<List<Integer>> listType = Type.list(Type.INT);
            final Typed<List<Integer>> input = new Typed<>(listType, List.of(1, 2, 3));

            final Optional<Typed<?>> result = rule.rewrite(listType, input);

            assertThat(result).isPresent();
            @SuppressWarnings("unchecked")
            final List<Integer> resultList = (List<Integer>) result.get().value();
            assertThat(resultList).containsExactly(2, 4, 6);
        }

        @Test
        @DisplayName("all returns input unchanged for no children")
        void allReturnsInputUnchangedForNoChildren() {
            final TypeRewriteRule doubleRule = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            final TypeRewriteRule rule = Rules.all(OPS, doubleRule);

            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("test");
        }

        @Test
        @DisplayName("one applies rule to first matching child")
        void oneAppliesToFirstMatchingChild() {
            final TypeRewriteRule doubleRule = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            final TypeRewriteRule rule = Rules.one(OPS, doubleRule);

            final Type<List<Integer>> listType = Type.list(Type.INT);
            final Typed<List<Integer>> input = new Typed<>(listType, List.of(1, 2, 3));

            final Optional<Typed<?>> result = rule.rewrite(listType, input);

            assertThat(result).isPresent();
            @SuppressWarnings("unchecked")
            final List<Integer> resultList = (List<Integer>) result.get().value();
            // Only first element is doubled
            assertThat(resultList).containsExactly(2, 2, 3);
        }

        @Test
        @DisplayName("one returns empty for no children")
        void oneReturnsEmptyForNoChildren() {
            final TypeRewriteRule doubleRule = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            final TypeRewriteRule rule = Rules.one(OPS, doubleRule);

            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("everywhere applies to all levels")
        void everywhereAppliesToAllLevels() {
            final TypeRewriteRule doubleRule = TypeRewriteRule.forType("double", Type.INT, n -> n * 2);
            final TypeRewriteRule rule = Rules.everywhere(OPS, doubleRule);

            final Type<List<Integer>> listType = Type.list(Type.INT);
            final Typed<List<Integer>> input = new Typed<>(listType, List.of(1, 2, 3));

            final Optional<Typed<?>> result = rule.rewrite(listType, input);

            assertThat(result).isPresent();
            @SuppressWarnings("unchecked")
            final List<Integer> resultList = (List<Integer>) result.get().value();
            assertThat(resultList).containsExactly(2, 4, 6);
        }

        @Test
        @DisplayName("bottomUp processes children first")
        void bottomUpProcessesChildrenFirst() {
            final List<String> order = new ArrayList<>();
            final TypeRewriteRule trackingRule = new TypeRewriteRule() {
                @Override
                public Optional<Typed<?>> rewrite(Type<?> type, Typed<?> input) {
                    order.add(type.describe());
                    return Optional.of(input);
                }
            };

            final TypeRewriteRule rule = Rules.bottomUp(OPS, trackingRule);

            final Type<List<String>> listType = Type.list(Type.STRING);
            final Typed<List<String>> input = new Typed<>(listType, List.of("a", "b"));

            rule.rewrite(listType, input);

            // Children are processed before parent in bottomUp
            assertThat(order).isNotEmpty();
        }

        @Test
        @DisplayName("topDown processes parent first")
        void topDownProcessesParentFirst() {
            final List<String> order = new ArrayList<>();
            final TypeRewriteRule trackingRule = new TypeRewriteRule() {
                @Override
                public Optional<Typed<?>> rewrite(Type<?> type, Typed<?> input) {
                    order.add(type.describe());
                    return Optional.of(input);
                }
            };

            final TypeRewriteRule rule = Rules.topDown(OPS, trackingRule);

            final Type<List<String>> listType = Type.list(Type.STRING);
            final Typed<List<String>> input = new Typed<>(listType, List.of("a", "b"));

            rule.rewrite(listType, input);

            // Parent is processed before children in topDown
            assertThat(order).isNotEmpty();
            // First entry should be the list type
            assertThat(order.get(0)).containsIgnoringCase("list");
        }

        @Test
        @DisplayName("all with DynamicOps has descriptive toString")
        void allWithDynamicOpsHasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.all(OPS, TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("all(");
        }

        @Test
        @DisplayName("one with DynamicOps has descriptive toString")
        void oneWithDynamicOpsHasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.one(OPS, TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("one(");
        }

        @Test
        @DisplayName("everywhere with DynamicOps has descriptive toString")
        void everywhereWithDynamicOpsHasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.everywhere(OPS, TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("everywhere(");
        }

        @Test
        @DisplayName("bottomUp with DynamicOps has descriptive toString")
        void bottomUpWithDynamicOpsHasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.bottomUp(OPS, TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("bottomUp(");
        }

        @Test
        @DisplayName("topDown with DynamicOps has descriptive toString")
        void topDownWithDynamicOpsHasDescriptiveToString() {
            final TypeRewriteRule rule = Rules.topDown(OPS, TypeRewriteRule.identity());

            assertThat(rule.toString()).startsWith("topDown(");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("empty seq returns identity")
        void emptySeqReturnsIdentity() {
            final TypeRewriteRule rule = Rules.seq();

            assertThat(rule.rewrite(Type.STRING, new Typed<>(Type.STRING, "test")))
                    .isPresent();
        }

        @Test
        @DisplayName("empty choice returns empty")
        void emptyChoiceReturnsEmpty() {
            final TypeRewriteRule rule = Rules.choice();

            assertThat(rule.rewrite(Type.STRING, new Typed<>(Type.STRING, "test")))
                    .isEmpty();
        }

        @Test
        @DisplayName("deeply nested compositions work")
        void deeplyNestedCompositionsWork() {
            TypeRewriteRule rule = TypeRewriteRule.identity();
            for (int i = 0; i < 10; i++) {
                rule = Rules.seq(rule, TypeRewriteRule.identity());
            }

            final Typed<String> input = new Typed<>(Type.STRING, "test");
            final Optional<Typed<?>> result = rule.rewrite(Type.STRING, input);

            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo("test");
        }

        @Test
        @DisplayName("tryOnce is equivalent to orKeep")
        void tryOnceIsEquivalentToOrKeep() {
            final TypeRewriteRule failing = TypeRewriteRule.fail();
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> tryOnceResult = Rules.tryOnce(failing).rewrite(Type.STRING, input);
            final Optional<Typed<?>> orKeepResult = failing.orKeep().rewrite(Type.STRING, input);

            assertThat(tryOnceResult).isPresent();
            assertThat(orKeepResult).isPresent();
            assertThat(tryOnceResult.get().value()).isEqualTo(orKeepResult.get().value());
        }

        @Test
        @DisplayName("noop is equivalent to identity")
        void noopIsEquivalentToIdentity() {
            final Typed<String> input = new Typed<>(Type.STRING, "test");

            final Optional<Typed<?>> noopResult = Rules.noop().rewrite(Type.STRING, input);
            final Optional<Typed<?>> identityResult = TypeRewriteRule.identity().rewrite(Type.STRING, input);

            assertThat(noopResult).isPresent();
            assertThat(identityResult).isPresent();
            assertThat(noopResult.get().value()).isEqualTo(identityResult.get().value());
        }

        @Test
        @DisplayName("ifType delegates correctly")
        void ifTypeDelegatesCorrectly() {
            final TypeRewriteRule inner = TypeRewriteRule.forType("upper", Type.STRING, String::toUpperCase);

            final Optional<Typed<?>> directResult = inner.ifType(Type.STRING)
                    .rewrite(Type.STRING, new Typed<>(Type.STRING, "hello"));
            final Optional<Typed<?>> rulesResult = Rules.ifType(Type.STRING, inner)
                    .rewrite(Type.STRING, new Typed<>(Type.STRING, "hello"));

            assertThat(directResult).isPresent();
            assertThat(rulesResult).isPresent();
            assertThat(directResult.get().value()).isEqualTo(rulesResult.get().value());
        }
    }
}
