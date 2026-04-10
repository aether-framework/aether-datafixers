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

package de.splatgames.aether.datafixers.testkit.assertion;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FieldAwareRuleAssert}.
 *
 * <p>Uses real {@link Rules} factory methods to construct field-aware rules,
 * verifying that the assertion API correctly inspects them.</p>
 */
@DisplayName("FieldAwareRuleAssert")
class FieldAwareRuleAssertTest {

    private static FieldAwareRule asFieldAware(final TypeRewriteRule rule) {
        return (FieldAwareRule) rule;
    }

    private static Dynamic<JsonElement> intDynamic(final int value) {
        return new Dynamic<>(GsonOps.INSTANCE, GsonOps.INSTANCE.createInt(value));
    }

    @Nested
    @DisplayName("Single-Operation Rules")
    class SingleOperationRules {

        @Test
        @DisplayName("rename rule has one rename operation")
        void renameRuleHasOneOperation() {
            final TypeRewriteRule rule = Rules.renameField(GsonOps.INSTANCE, "oldName", "newName");

            assertThat(asFieldAware(rule))
                    .hasFieldOperationCount(1)
                    .containsRename("oldName", "newName");
        }

        @Test
        @DisplayName("remove rule has one remove operation")
        void removeRuleHasOneOperation() {
            final TypeRewriteRule rule = Rules.removeField(GsonOps.INSTANCE, "deprecated");

            assertThat(asFieldAware(rule))
                    .hasFieldOperationCount(1)
                    .containsRemove("deprecated");
        }

        @Test
        @DisplayName("add rule has one add operation")
        void addRuleHasOneOperation() {
            final TypeRewriteRule rule = Rules.addField(
                    GsonOps.INSTANCE, "health", intDynamic(100));

            assertThat(asFieldAware(rule))
                    .hasFieldOperationCount(1)
                    .containsAdd("health");
        }

        @Test
        @DisplayName("transform rule has one transform operation")
        void transformRuleHasOneOperation() {
            final TypeRewriteRule rule = Rules.transformField(
                    GsonOps.INSTANCE, "stats", d -> d);

            assertThat(asFieldAware(rule))
                    .hasFieldOperationCount(1)
                    .containsTransform("stats");
        }
    }

    @Nested
    @DisplayName("Composition Rules")
    class CompositionRules {

        @Test
        @DisplayName("seq aggregates field operations from children")
        void seqAggregatesChildren() {
            final TypeRewriteRule rule = Rules.seq(
                    Rules.renameField(GsonOps.INSTANCE, "oldName", "newName"),
                    Rules.removeField(GsonOps.INSTANCE, "deprecated"),
                    Rules.addField(GsonOps.INSTANCE, "health", intDynamic(100))
            );

            assertThat(asFieldAware(rule))
                    .hasFieldOperationCount(3)
                    .containsRename("oldName", "newName")
                    .containsRemove("deprecated")
                    .containsAdd("health");
        }

        @Test
        @DisplayName("hasFieldOperationCountOfType counts by type")
        void hasFieldOperationCountOfType() {
            final TypeRewriteRule rule = Rules.seq(
                    Rules.renameField(GsonOps.INSTANCE, "a", "b"),
                    Rules.renameField(GsonOps.INSTANCE, "c", "d"),
                    Rules.removeField(GsonOps.INSTANCE, "e")
            );

            assertThat(asFieldAware(rule))
                    .hasFieldOperationCountOfType(FieldOperationType.RENAME, 2)
                    .hasFieldOperationCountOfType(FieldOperationType.REMOVE, 1)
                    .hasFieldOperationCountOfType(FieldOperationType.ADD, 0);
        }

        @Test
        @DisplayName("containsOperationOfType matches any operation of the type")
        void containsOperationOfType() {
            final TypeRewriteRule rule = Rules.seq(
                    Rules.renameField(GsonOps.INSTANCE, "a", "b"),
                    Rules.removeField(GsonOps.INSTANCE, "c")
            );

            assertThat(asFieldAware(rule))
                    .containsOperationOfType(FieldOperationType.RENAME)
                    .containsOperationOfType(FieldOperationType.REMOVE);
        }
    }

    @Nested
    @DisplayName("Failure Cases")
    class FailureCases {

        @Test
        @DisplayName("hasFieldOperationCount fails on wrong count")
        void hasFieldOperationCountFails() {
            final TypeRewriteRule rule = Rules.renameField(GsonOps.INSTANCE, "a", "b");

            assertThatThrownBy(() -> assertThat(asFieldAware(rule)).hasFieldOperationCount(5))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("<5>")
                    .hasMessageContaining("<1>");
        }

        @Test
        @DisplayName("containsRename fails when rename is missing")
        void containsRenameFailsWhenMissing() {
            final TypeRewriteRule rule = Rules.removeField(GsonOps.INSTANCE, "deprecated");

            assertThatThrownBy(() -> assertThat(asFieldAware(rule)).containsRename("a", "b"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("rename");
        }

        @Test
        @DisplayName("hasNoFieldOperations fails when operations are present")
        void hasNoFieldOperationsFails() {
            final TypeRewriteRule rule = Rules.renameField(GsonOps.INSTANCE, "a", "b");

            assertThatThrownBy(() -> assertThat(asFieldAware(rule)).hasNoFieldOperations())
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("containsOperationOfType fails when type is absent")
        void containsOperationOfTypeFailsWhenAbsent() {
            final TypeRewriteRule rule = Rules.renameField(GsonOps.INSTANCE, "a", "b");

            assertThatThrownBy(() -> assertThat(asFieldAware(rule)).containsOperationOfType(FieldOperationType.MOVE))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining(FieldOperationType.MOVE.toString());
        }
    }

    @Nested
    @DisplayName("Navigation")
    class Navigation {

        @Test
        @DisplayName("firstFieldOperation returns the first operation for chained assertions")
        void firstFieldOperationChains() {
            final TypeRewriteRule rule = Rules.renameField(GsonOps.INSTANCE, "oldName", "newName");

            assertThat(asFieldAware(rule))
                    .firstFieldOperation()
                    .hasOperationType(FieldOperationType.RENAME)
                    .hasFieldPath("oldName")
                    .hasTargetFieldName("newName");
        }

        @Test
        @DisplayName("fieldOperation(index) returns the operation at the given index")
        void fieldOperationByIndex() {
            final TypeRewriteRule rule = Rules.seq(
                    Rules.renameField(GsonOps.INSTANCE, "a", "b"),
                    Rules.removeField(GsonOps.INSTANCE, "c")
            );

            assertThat(asFieldAware(rule))
                    .fieldOperation(0).hasOperationType(FieldOperationType.RENAME);
            assertThat(asFieldAware(rule))
                    .fieldOperation(1).hasOperationType(FieldOperationType.REMOVE);
        }

        @Test
        @DisplayName("firstFieldOperation fails when no operations exist")
        void firstFieldOperationFailsWhenEmpty() {
            // Manually-built FieldAwareRule with empty list for this edge case
            final FieldAwareRule emptyRule = java.util.Collections::emptyList;

            assertThatThrownBy(() -> assertThat(emptyRule).firstFieldOperation())
                    .isInstanceOf(AssertionError.class);
        }
    }
}
