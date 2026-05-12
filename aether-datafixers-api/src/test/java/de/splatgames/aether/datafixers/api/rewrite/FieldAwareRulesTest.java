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

import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.optic.TestOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that {@link Rules} factory methods produce {@link FieldAwareRule} instances
 * with correct field operation metadata, and that composition methods properly
 * aggregate field operations from their children.
 */
@DisplayName("FieldAwareRule integration with Rules")
class FieldAwareRulesTest {

    private static final TestOps OPS = TestOps.INSTANCE;

    // ==================== Helper Methods ====================

    /**
     * Asserts that the given rule is a {@link FieldAwareRule} and returns it.
     */
    private static FieldAwareRule assertFieldAware(final TypeRewriteRule rule) {
        assertThat(rule).isInstanceOf(FieldAwareRule.class);
        return (FieldAwareRule) rule;
    }

    /**
     * Asserts that the given rule is a {@link FieldAwareRule} with the expected
     * number of field operations.
     */
    private static List<FieldOperation> assertFieldOpsCount(final TypeRewriteRule rule,
                                                            final int expectedCount) {
        final FieldAwareRule fieldAware = assertFieldAware(rule);
        final List<FieldOperation> ops = fieldAware.fieldOperations();
        assertThat(ops).hasSize(expectedCount);
        return ops;
    }

    // ==================== Individual Field Rules ====================

    @Nested
    @DisplayName("Individual Field Rules")
    class IndividualFieldRules {

        @Test
        @DisplayName("renameField should produce FieldAwareRule with 1 RENAME operation")
        void renameFieldShouldProduceRenameOperation() {
            final TypeRewriteRule rule = Rules.renameField(OPS, "old", "new");

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.RENAME);
            assertThat(ops.get(0).fieldPath()).containsExactly("old");
            assertThat(ops.get(0).targetFieldName()).isEqualTo("new");
        }

        @Test
        @DisplayName("removeField should produce FieldAwareRule with 1 REMOVE operation")
        void removeFieldShouldProduceRemoveOperation() {
            final TypeRewriteRule rule = Rules.removeField(OPS, "field");

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.REMOVE);
            assertThat(ops.get(0).fieldPath()).containsExactly("field");
        }

        @Test
        @DisplayName("addField should produce FieldAwareRule with 1 ADD operation")
        void addFieldShouldProduceAddOperation() {
            final Dynamic<Object> defaultVal = new Dynamic<>(OPS, 42);
            final TypeRewriteRule rule = Rules.addField(OPS, "field", defaultVal);

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.ADD);
            assertThat(ops.get(0).fieldPath()).containsExactly("field");
        }

        @Test
        @DisplayName("transformField should produce FieldAwareRule with 1 TRANSFORM operation")
        void transformFieldShouldProduceTransformOperation() {
            final TypeRewriteRule rule = Rules.transformField(OPS, "field",
                    d -> d.createInt(d.asInt().result().orElse(0) + 1));

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.TRANSFORM);
            assertThat(ops.get(0).fieldPath()).containsExactly("field");
        }

        @Test
        @DisplayName("setField should produce FieldAwareRule with 1 SET operation")
        void setFieldShouldProduceSetOperation() {
            final Dynamic<Object> value = new Dynamic<>(OPS, "value");
            final TypeRewriteRule rule = Rules.setField(OPS, "field", value);

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.SET);
            assertThat(ops.get(0).fieldPath()).containsExactly("field");
        }

        @Test
        @DisplayName("renameFields should produce FieldAwareRule with 2 RENAME operations")
        void renameFieldsShouldProduceMultipleRenameOperations() {
            final TypeRewriteRule rule = Rules.renameFields(OPS, Map.of("a", "b", "c", "d"));

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 2);
            assertThat(ops).allMatch(op -> op.operationType() == FieldOperationType.RENAME);
        }

        @Test
        @DisplayName("removeFields should produce FieldAwareRule with 2 REMOVE operations")
        void removeFieldsShouldProduceMultipleRemoveOperations() {
            final TypeRewriteRule rule = Rules.removeFields(OPS, "a", "b");

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 2);
            assertThat(ops).allMatch(op -> op.operationType() == FieldOperationType.REMOVE);
        }

        @Test
        @DisplayName("moveField should produce FieldAwareRule with 1 MOVE operation")
        void moveFieldShouldProduceMoveOperation() {
            final TypeRewriteRule rule = Rules.moveField(OPS, "a", "b");

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.MOVE);
            assertThat(ops.get(0).fieldPath()).containsExactly("a");
            assertThat(ops.get(0).targetFieldName()).isEqualTo("b");
        }

        @Test
        @DisplayName("copyField should produce FieldAwareRule with 1 COPY operation")
        void copyFieldShouldProduceCopyOperation() {
            final TypeRewriteRule rule = Rules.copyField(OPS, "a", "b");

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.COPY);
            assertThat(ops.get(0).fieldPath()).containsExactly("a");
            assertThat(ops.get(0).targetFieldName()).isEqualTo("b");
        }

        @Test
        @DisplayName("groupFields should produce FieldAwareRule with 1 GROUP operation")
        void groupFieldsShouldProduceGroupOperation() {
            final TypeRewriteRule rule = Rules.groupFields(OPS, "pos", "x", "y");

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.GROUP);
            assertThat(ops.get(0).targetFieldName()).isEqualTo("pos");
        }

        @Test
        @DisplayName("flattenField should produce FieldAwareRule with 1 FLATTEN operation")
        void flattenFieldShouldProduceFlattenOperation() {
            final TypeRewriteRule rule = Rules.flattenField(OPS, "pos");

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.FLATTEN);
            assertThat(ops.get(0).fieldPath()).containsExactly("pos");
        }

        @Test
        @DisplayName("ifFieldExists (3-arg) should produce FieldAwareRule with 1 CONDITIONAL operation")
        void ifFieldExistsShouldProduceConditionalOperation() {
            final TypeRewriteRule someRule = Rules.removeField(OPS, "field");
            final TypeRewriteRule rule = Rules.ifFieldExists(OPS, "field", someRule);

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.CONDITIONAL);
            assertThat(ops.get(0).fieldPath()).containsExactly("field");
            assertThat(ops.get(0).description()).isEqualTo("exists");
        }

        @Test
        @DisplayName("ifFieldMissing (3-arg) should produce FieldAwareRule with 1 CONDITIONAL operation")
        void ifFieldMissingShouldProduceConditionalOperation() {
            final TypeRewriteRule someRule = Rules.addField(OPS, "field", new Dynamic<>(OPS, 1));
            final TypeRewriteRule rule = Rules.ifFieldMissing(OPS, "field", someRule);

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.CONDITIONAL);
            assertThat(ops.get(0).fieldPath()).containsExactly("field");
            assertThat(ops.get(0).description()).isEqualTo("missing");
        }

        @Test
        @DisplayName("ifFieldEquals (4-arg) should produce FieldAwareRule with 1 CONDITIONAL operation")
        void ifFieldEqualsShouldProduceConditionalOperation() {
            final TypeRewriteRule someRule = Rules.removeField(OPS, "field");
            final TypeRewriteRule rule = Rules.ifFieldEquals(OPS, "field", 1, someRule);

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.CONDITIONAL);
            assertThat(ops.get(0).fieldPath()).containsExactly("field");
            assertThat(ops.get(0).description()).isEqualTo("equals");
        }
    }

    // ==================== Composition Aggregation ====================

    @Nested
    @DisplayName("Composition Aggregation")
    class CompositionAggregation {

        @Test
        @DisplayName("seq should aggregate field operations from children")
        void seqShouldAggregateFieldOperations() {
            final TypeRewriteRule renameRule = Rules.renameField(OPS, "old", "new");
            final TypeRewriteRule addRule = Rules.addField(OPS, "field", new Dynamic<>(OPS, 1));
            final TypeRewriteRule composed = Rules.seq(renameRule, addRule);

            final List<FieldOperation> ops = assertFieldOpsCount(composed, 2);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.RENAME);
            assertThat(ops.get(1).operationType()).isEqualTo(FieldOperationType.ADD);
        }

        @Test
        @DisplayName("seqAll should aggregate field operations from all children")
        void seqAllShouldAggregateFieldOperations() {
            final TypeRewriteRule renameRule = Rules.renameField(OPS, "old", "new");
            final TypeRewriteRule removeRule = Rules.removeField(OPS, "deprecated");
            final TypeRewriteRule addRule = Rules.addField(OPS, "field", new Dynamic<>(OPS, 1));
            final TypeRewriteRule composed = Rules.seqAll(renameRule, removeRule, addRule);

            final List<FieldOperation> ops = assertFieldOpsCount(composed, 3);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.RENAME);
            assertThat(ops.get(1).operationType()).isEqualTo(FieldOperationType.REMOVE);
            assertThat(ops.get(2).operationType()).isEqualTo(FieldOperationType.ADD);
        }

        @Test
        @DisplayName("choice should aggregate field operations from children")
        void choiceShouldAggregateFieldOperations() {
            final TypeRewriteRule renameRule = Rules.renameField(OPS, "old", "new");
            final TypeRewriteRule addRule = Rules.addField(OPS, "field", new Dynamic<>(OPS, 1));
            final TypeRewriteRule composed = Rules.choice(renameRule, addRule);

            final List<FieldOperation> ops = assertFieldOpsCount(composed, 2);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.RENAME);
            assertThat(ops.get(1).operationType()).isEqualTo(FieldOperationType.ADD);
        }

        @Test
        @DisplayName("seq with identity should only include field-aware operations")
        void seqWithIdentityShouldOnlyIncludeFieldAwareOps() {
            final TypeRewriteRule renameRule = Rules.renameField(OPS, "old", "new");
            final TypeRewriteRule composed = Rules.seq(renameRule, TypeRewriteRule.identity());

            final List<FieldOperation> ops = assertFieldOpsCount(composed, 1);
            assertThat(ops.get(0).operationType()).isEqualTo(FieldOperationType.RENAME);
        }

        @Test
        @DisplayName("seq of pure non-field-aware rules should not be FieldAwareRule")
        void seqOfNonFieldAwareRulesShouldNotBeFieldAware() {
            final TypeRewriteRule composed = Rules.seq(
                    TypeRewriteRule.identity(),
                    TypeRewriteRule.identity()
            );

            assertThat(composed).isNotInstanceOf(FieldAwareRule.class);
        }
    }

    // ==================== Batch Operations ====================

    @Nested
    @DisplayName("Batch Operations")
    class BatchOperations {

        @Test
        @DisplayName("batch should produce FieldAwareRule with aggregated operations")
        void batchShouldProduceFieldAwareRuleWithAggregatedOps() {
            final TypeRewriteRule rule = Rules.batch(OPS, b -> b
                    .rename("a", "b")
                    .remove("c")
            );

            final List<FieldOperation> ops = assertFieldOpsCount(rule, 2);
            assertThat(ops).extracting(FieldOperation::operationType)
                    .containsExactlyInAnyOrder(FieldOperationType.RENAME, FieldOperationType.REMOVE);
        }
    }
}
