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

package de.splatgames.aether.datafixers.core.diagnostic;

import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import de.splatgames.aether.datafixers.api.diagnostic.RuleApplication;
import de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DiagnosticRuleWrapper}.
 */
@DisplayName("DiagnosticRuleWrapper")
class DiagnosticRuleWrapperTest {

    private static final TypeReference PLAYER = new TypeReference("player");

    private TypeRewriteRule delegateRule;
    private DiagnosticContextImpl context;
    private Type<String> testType;
    private Typed<String> testInput;

    @BeforeEach
    void setUp() {
        delegateRule = Rules.noop();

        context = new DiagnosticContextImpl(
                DiagnosticOptions.builder()
                        .captureRuleDetails(true)
                        .build()
        );

        testType = Type.STRING;
        testInput = new Typed<>(testType, "Alice");
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("creates wrapper with valid arguments")
        void createsWrapperWithValidArguments() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            assertThat(wrapper).isNotNull();
        }

        @Test
        @DisplayName("rejects null delegate")
        void rejectsNullDelegate() {
            assertThatThrownBy(() -> new DiagnosticRuleWrapper(null, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("delegate");
        }

        @Test
        @DisplayName("rejects null context")
        void rejectsNullContext() {
            assertThatThrownBy(() -> new DiagnosticRuleWrapper(delegateRule, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context");
        }
    }

    @Nested
    @DisplayName("rewrite()")
    class Rewrite {

        @Test
        @DisplayName("delegates to underlying rule")
        void delegatesToUnderlyingRule() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            Optional<Typed<?>> result = wrapper.rewrite(testType, testInput);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("rejects null type")
        void rejectsNullType() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            assertThatThrownBy(() -> wrapper.rewrite(null, testInput))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("type");
        }

        @Test
        @DisplayName("rejects null input")
        void rejectsNullInput() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            assertThatThrownBy(() -> wrapper.rewrite(testType, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("input");
        }

        @Test
        @DisplayName("skips recording when captureRuleDetails is false")
        void skipsRecordingWhenCaptureRuleDetailsIsFalse() {
            DiagnosticContextImpl noDetailsContext = new DiagnosticContextImpl(
                    DiagnosticOptions.builder()
                            .captureRuleDetails(false)
                            .build()
            );

            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, noDetailsContext);

            Optional<Typed<?>> result = wrapper.rewrite(testType, testInput);

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("apply()")
    class Apply {

        @Test
        @DisplayName("applies rule and returns result")
        void appliesRuleAndReturnsResult() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            Typed<?> result = wrapper.apply(testInput);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("rejects null input")
        void rejectsNullInput() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            assertThatThrownBy(() -> wrapper.apply(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("input");
        }
    }

    @Nested
    @DisplayName("applyOrThrow()")
    class ApplyOrThrow {

        @Test
        @DisplayName("applies rule when it matches")
        void appliesRuleWhenItMatches() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            Typed<?> result = wrapper.applyOrThrow(testInput);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("rejects null input")
        void rejectsNullInput() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            assertThatThrownBy(() -> wrapper.applyOrThrow(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("input");
        }
    }

    @Nested
    @DisplayName("Combinator Methods")
    class CombinatorMethods {

        @Test
        @DisplayName("andThen() returns wrapped composition")
        void andThenReturnsWrappedComposition() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);
            TypeRewriteRule nextRule = Rules.noop();

            TypeRewriteRule result = wrapper.andThen(nextRule);

            assertThat(result).isInstanceOf(DiagnosticRuleWrapper.class);
        }

        @Test
        @DisplayName("andThen() rejects null")
        void andThenRejectsNull() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            assertThatThrownBy(() -> wrapper.andThen(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("next");
        }

        @Test
        @DisplayName("orElse() returns wrapped composition")
        void orElseReturnsWrappedComposition() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);
            TypeRewriteRule fallback = Rules.noop();

            TypeRewriteRule result = wrapper.orElse(fallback);

            assertThat(result).isInstanceOf(DiagnosticRuleWrapper.class);
        }

        @Test
        @DisplayName("orElse() rejects null")
        void orElseRejectsNull() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            assertThatThrownBy(() -> wrapper.orElse(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("fallback");
        }

        @Test
        @DisplayName("orKeep() returns wrapped rule")
        void orKeepReturnsWrappedRule() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            TypeRewriteRule result = wrapper.orKeep();

            assertThat(result).isInstanceOf(DiagnosticRuleWrapper.class);
        }

        @Test
        @DisplayName("ifType() returns wrapped rule")
        void ifTypeReturnsWrappedRule() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            TypeRewriteRule result = wrapper.ifType(testType);

            assertThat(result).isInstanceOf(DiagnosticRuleWrapper.class);
        }

        @Test
        @DisplayName("ifType() rejects null")
        void ifTypeRejectsNull() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            assertThatThrownBy(() -> wrapper.ifType(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("targetType");
        }

        @Test
        @DisplayName("named() returns wrapped rule")
        void namedReturnsWrappedRule() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            TypeRewriteRule result = wrapper.named("testName");

            assertThat(result).isInstanceOf(DiagnosticRuleWrapper.class);
        }

        @Test
        @DisplayName("named() rejects null")
        void namedRejectsNull() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            assertThatThrownBy(() -> wrapper.named(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("delegates to underlying rule")
        void delegatesToUnderlyingRule() {
            TypeRewriteRule namedRule = Rules.noop().named("testRule");
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(namedRule, context);

            String result = wrapper.toString();

            assertThat(result).contains("testRule");
        }
    }

    @Nested
    @DisplayName("Field-Level Metadata")
    class FieldLevelMetadata {

        @Test
        @DisplayName("extracts field operations from FieldAwareRule delegate")
        void extractsFieldOperationsFromFieldAwareRule() {
            // Create a FieldAwareRule via a field-aware rule factory method
            TypeRewriteRule fieldAwareRule = new TypeRewriteRule() {
                @NotNull
                @Override
                public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                                  @NotNull final Typed<?> input) {
                    return Optional.of(input);
                }
            };
            // Wrap it manually as FieldAwareRule
            TypeRewriteRule delegate = createFieldAwareDelegate(
                    List.of(FieldOperation.rename("old", "new"))
            );

            context.reportBuilder().startMigration(PLAYER, new de.splatgames.aether.datafixers.api.DataVersion(1),
                    new de.splatgames.aether.datafixers.api.DataVersion(2));
            context.reportBuilder().startFix(createMockFix());

            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegate, context);
            wrapper.rewrite(testType, testInput);

            final var mockFix = createMockFix();
            context.reportBuilder().endFix(mockFix, java.time.Duration.ofMillis(1), null);

            List<RuleApplication> ruleApps = context.reportBuilder()
                    .build().fixExecutions().get(0).ruleApplications();
            assertThat(ruleApps).hasSize(1);
            assertThat(ruleApps.get(0).fieldOperations()).hasSize(1);
            assertThat(ruleApps.get(0).fieldOperations().get(0).operationType())
                    .isEqualTo(FieldOperationType.RENAME);
        }

        @Test
        @DisplayName("returns empty field operations when captureFieldDetails is false")
        void returnsEmptyFieldOpsWhenCaptureFieldDetailsDisabled() {
            DiagnosticContextImpl noFieldDetailsCtx = new DiagnosticContextImpl(
                    DiagnosticOptions.builder()
                            .captureRuleDetails(true)
                            .captureFieldDetails(false)
                            .build()
            );
            TypeRewriteRule delegate = createFieldAwareDelegate(
                    List.of(FieldOperation.rename("old", "new"))
            );

            noFieldDetailsCtx.reportBuilder().startMigration(PLAYER,
                    new de.splatgames.aether.datafixers.api.DataVersion(1),
                    new de.splatgames.aether.datafixers.api.DataVersion(2));
            noFieldDetailsCtx.reportBuilder().startFix(createMockFix());

            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegate, noFieldDetailsCtx);
            wrapper.rewrite(testType, testInput);

            final var mockFix = createMockFix();
            noFieldDetailsCtx.reportBuilder().endFix(mockFix, java.time.Duration.ofMillis(1), null);

            List<RuleApplication> ruleApps = noFieldDetailsCtx.reportBuilder()
                    .build().fixExecutions().get(0).ruleApplications();
            assertThat(ruleApps).hasSize(1);
            assertThat(ruleApps.get(0).fieldOperations()).isEmpty();
        }

        @Test
        @DisplayName("returns empty field operations for non-FieldAwareRule delegate")
        void returnsEmptyFieldOpsForNonFieldAwareDelegate() {
            context.reportBuilder().startMigration(PLAYER,
                    new de.splatgames.aether.datafixers.api.DataVersion(1),
                    new de.splatgames.aether.datafixers.api.DataVersion(2));
            final var mockFix = createMockFix();
            context.reportBuilder().startFix(mockFix);

            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);
            wrapper.rewrite(testType, testInput);

            context.reportBuilder().endFix(mockFix, java.time.Duration.ofMillis(1), null);

            List<RuleApplication> ruleApps = context.reportBuilder()
                    .build().fixExecutions().get(0).ruleApplications();
            assertThat(ruleApps).hasSize(1);
            assertThat(ruleApps.get(0).fieldOperations()).isEmpty();
        }

        private TypeRewriteRule createFieldAwareDelegate(List<FieldOperation> ops) {
            return new FieldAwareTestRule(ops);
        }

        private de.splatgames.aether.datafixers.api.fix.DataFix<Object> createMockFix() {
            return new de.splatgames.aether.datafixers.api.fix.DataFix<>() {
                @Override
                public @NotNull String name() { return "test-fix"; }
                @Override
                public @NotNull de.splatgames.aether.datafixers.api.DataVersion fromVersion() {
                    return new de.splatgames.aether.datafixers.api.DataVersion(1);
                }
                @Override
                public @NotNull de.splatgames.aether.datafixers.api.DataVersion toVersion() {
                    return new de.splatgames.aether.datafixers.api.DataVersion(2);
                }
                @Override
                public @NotNull de.splatgames.aether.datafixers.api.dynamic.Dynamic<Object> apply(
                        @NotNull TypeReference type,
                        @NotNull de.splatgames.aether.datafixers.api.dynamic.Dynamic<Object> input,
                        @NotNull de.splatgames.aether.datafixers.api.fix.DataFixerContext ctx) {
                    return input;
                }
            };
        }
    }

    /**
     * A test implementation of both TypeRewriteRule and FieldAwareRule.
     */
    private static class FieldAwareTestRule implements TypeRewriteRule, FieldAwareRule {
        private final List<FieldOperation> fieldOperations;

        FieldAwareTestRule(List<FieldOperation> fieldOperations) {
            this.fieldOperations = List.copyOf(fieldOperations);
        }

        @Override
        public @NotNull Optional<Typed<?>> rewrite(@NotNull Type<?> type, @NotNull Typed<?> input) {
            return Optional.of(input);
        }

        @Override
        public @NotNull List<FieldOperation> fieldOperations() {
            return this.fieldOperations;
        }
    }

    @Nested
    @DisplayName("wrap()")
    class Wrap {

        @Test
        @DisplayName("wraps non-wrapper rule")
        void wrapsNonWrapperRule() {
            TypeRewriteRule result = DiagnosticRuleWrapper.wrap(delegateRule, context);

            assertThat(result).isInstanceOf(DiagnosticRuleWrapper.class);
        }

        @Test
        @DisplayName("returns same instance if already wrapped")
        void returnsSameInstanceIfAlreadyWrapped() {
            DiagnosticRuleWrapper wrapper = new DiagnosticRuleWrapper(delegateRule, context);

            TypeRewriteRule result = DiagnosticRuleWrapper.wrap(wrapper, context);

            assertThat(result).isSameAs(wrapper);
        }

        @Test
        @DisplayName("rejects null rule")
        void rejectsNullRule() {
            assertThatThrownBy(() -> DiagnosticRuleWrapper.wrap(null, context))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("rule");
        }

        @Test
        @DisplayName("rejects null context")
        void rejectsNullContext() {
            assertThatThrownBy(() -> DiagnosticRuleWrapper.wrap(delegateRule, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context");
        }
    }
}
