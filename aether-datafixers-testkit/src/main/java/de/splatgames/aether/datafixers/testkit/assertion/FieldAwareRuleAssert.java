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

import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule;
import org.assertj.core.api.AbstractAssert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * AssertJ assertions for {@link FieldAwareRule} instances.
 *
 * <p>Provides fluent assertions for verifying which {@link FieldOperation field operations}
 * a rule carries, with convenience helpers for the most common cases (rename, remove,
 * add, transform) and navigation into individual operations via
 * {@link FieldOperationAssert}.</p>
 *
 * <h2>Count Assertions</h2>
 * <pre>{@code
 * assertThat(rule)
 *     .hasFieldOperationCount(3)
 *     .hasFieldOperationCountOfType(FieldOperationType.RENAME, 2);
 * }</pre>
 *
 * <h2>Convenience Containment</h2>
 * <pre>{@code
 * assertThat(rule)
 *     .containsRename("oldName", "newName")
 *     .containsRemove("deprecated")
 *     .containsAdd("health");
 * }</pre>
 *
 * <h2>Navigation</h2>
 * <pre>{@code
 * assertThat(rule)
 *     .firstFieldOperation()
 *         .hasOperationType(FieldOperationType.RENAME)
 *         .hasFieldPath("oldName")
 *         .hasTargetFieldName("newName");
 *
 * assertThat(rule)
 *     .fieldOperation(2)
 *         .hasOperationType(FieldOperationType.REMOVE);
 * }</pre>
 *
 * @author Erik Pförtner
 * @see FieldAwareRule
 * @see FieldOperation
 * @see FieldOperationAssert
 * @see AetherAssertions
 * @since 1.0.0
 */
public final class FieldAwareRuleAssert extends AbstractAssert<FieldAwareRuleAssert, FieldAwareRule> {

    /**
     * Creates a new assertion for the given field-aware rule.
     *
     * @param actual the rule to assert on, may be {@code null}
     */
    public FieldAwareRuleAssert(@Nullable final FieldAwareRule actual) {
        super(actual, FieldAwareRuleAssert.class);
    }

    // ==================== Counts ====================

    /**
     * Asserts that the rule carries exactly the given number of field operations.
     *
     * @param expected the expected count, must be non-negative
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldAwareRuleAssert hasFieldOperationCount(final int expected) {
        isNotNull();
        final int actualCount = this.actual.fieldOperations().size();
        if (actualCount != expected) {
            failWithMessage("Expected rule to have <%d> field operations but had <%d>",
                    expected, actualCount);
        }
        return this;
    }

    /**
     * Asserts that the rule carries exactly the given number of field operations
     * of the specified type.
     *
     * @param type     the operation type to count, must not be {@code null}
     * @param expected the expected count, must be non-negative
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldAwareRuleAssert hasFieldOperationCountOfType(@NotNull final FieldOperationType type,
                                                             final int expected) {
        isNotNull();
        final long actualCount = this.actual.fieldOperations().stream()
                .filter(op -> op.operationType() == type)
                .count();
        if (actualCount != expected) {
            failWithMessage("Expected rule to have <%d> field operations of type <%s> but had <%d>",
                    expected, type, actualCount);
        }
        return this;
    }

    /**
     * Asserts that the rule carries no field operations.
     *
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldAwareRuleAssert hasNoFieldOperations() {
        isNotNull();
        if (!this.actual.fieldOperations().isEmpty()) {
            failWithMessage("Expected rule to have no field operations but had <%d>: %s",
                    this.actual.fieldOperations().size(), this.actual.fieldOperations());
        }
        return this;
    }

    // ==================== Convenience Containment ====================

    /**
     * Asserts that the rule contains a {@code RENAME} operation from the given
     * source name to the given target name.
     *
     * @param oldName the original field name, must not be {@code null}
     * @param newName the new field name, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldAwareRuleAssert containsRename(@NotNull final String oldName,
                                               @NotNull final String newName) {
        isNotNull();
        final boolean found = this.actual.fieldOperations().stream()
                .anyMatch(op -> op.operationType() == FieldOperationType.RENAME
                        && op.fieldPath().size() == 1
                        && op.fieldPath().get(0).equals(oldName)
                        && newName.equals(op.targetFieldName()));
        if (!found) {
            failWithMessage("Expected rule to contain rename(%s -> %s) but field operations were: %s",
                    oldName, newName, this.actual.fieldOperations());
        }
        return this;
    }

    /**
     * Asserts that the rule contains a {@code REMOVE} operation on the given field.
     *
     * @param fieldName the field name, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldAwareRuleAssert containsRemove(@NotNull final String fieldName) {
        return containsTopLevelOperation(FieldOperationType.REMOVE, fieldName);
    }

    /**
     * Asserts that the rule contains an {@code ADD} operation on the given field.
     *
     * @param fieldName the field name, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldAwareRuleAssert containsAdd(@NotNull final String fieldName) {
        return containsTopLevelOperation(FieldOperationType.ADD, fieldName);
    }

    /**
     * Asserts that the rule contains a {@code TRANSFORM} operation on the given field.
     *
     * @param fieldName the field name, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldAwareRuleAssert containsTransform(@NotNull final String fieldName) {
        return containsTopLevelOperation(FieldOperationType.TRANSFORM, fieldName);
    }

    /**
     * Asserts that the rule contains at least one field operation of the given type.
     *
     * @param type the operation type, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldAwareRuleAssert containsOperationOfType(@NotNull final FieldOperationType type) {
        isNotNull();
        final boolean found = this.actual.fieldOperations().stream()
                .anyMatch(op -> op.operationType() == type);
        if (!found) {
            failWithMessage("Expected rule to contain at least one operation of type <%s> but field operations were: %s",
                    type, this.actual.fieldOperations());
        }
        return this;
    }

    /**
     * Internal helper for asserting a top-level (single-segment path) operation
     * of a given type on a given field.
     *
     * @param type      the operation type
     * @param fieldName the field name
     * @return this assertion for chaining
     */
    @NotNull
    private FieldAwareRuleAssert containsTopLevelOperation(@NotNull final FieldOperationType type,
                                                           @NotNull final String fieldName) {
        isNotNull();
        final boolean found = this.actual.fieldOperations().stream()
                .anyMatch(op -> op.operationType() == type
                        && op.fieldPath().size() == 1
                        && op.fieldPath().get(0).equals(fieldName));
        if (!found) {
            failWithMessage("Expected rule to contain %s(%s) but field operations were: %s",
                    type, fieldName, this.actual.fieldOperations());
        }
        return this;
    }

    // ==================== Navigation ====================

    /**
     * Returns a {@link FieldOperationAssert} for the field operation at the given index.
     *
     * <p>Use this to drill down into a specific operation for detailed assertions:</p>
     * <pre>{@code
     * assertThat(rule)
     *     .fieldOperation(0)
     *         .hasOperationType(FieldOperationType.RENAME)
     *         .hasFieldPath("oldName");
     * }</pre>
     *
     * @param index the zero-based index, must be in range
     * @return a new {@link FieldOperationAssert}, never {@code null}
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @NotNull
    public FieldOperationAssert fieldOperation(final int index) {
        isNotNull();
        final List<FieldOperation> ops = this.actual.fieldOperations();
        if (index < 0 || index >= ops.size()) {
            failWithMessage("Expected field operation index <%d> to be in range [0, %d)",
                    index, ops.size());
        }
        return new FieldOperationAssert(ops.get(index));
    }

    /**
     * Returns a {@link FieldOperationAssert} for the first field operation.
     *
     * @return a new {@link FieldOperationAssert}, never {@code null}
     */
    @NotNull
    public FieldOperationAssert firstFieldOperation() {
        isNotNull();
        if (this.actual.fieldOperations().isEmpty()) {
            failWithMessage("Expected rule to have at least one field operation but had none");
        }
        return new FieldOperationAssert(this.actual.fieldOperations().get(0));
    }
}
