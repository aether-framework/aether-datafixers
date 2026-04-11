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
import org.assertj.core.api.AbstractAssert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * AssertJ assertions for {@link FieldOperation} records.
 *
 * <p>Provides fluent assertions for verifying field operation metadata
 * captured by {@link de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule field-aware rules},
 * including operation type, field path, target field name, and description.</p>
 *
 * <h2>Operation Type Assertions</h2>
 * <pre>{@code
 * assertThat(operation)
 *     .hasOperationType(FieldOperationType.RENAME)
 *     .isNotNested();
 * }</pre>
 *
 * <h2>Field Path Assertions</h2>
 * <pre>{@code
 * // Single-segment path
 * assertThat(operation).hasFieldPath("playerName");
 *
 * // Multi-segment (nested) path
 * assertThat(operation).hasFieldPath("position", "x");
 *
 * // Or via dot-notation string
 * assertThat(operation).hasFieldPathString("position.x");
 * }</pre>
 *
 * <h2>Target Field Assertions</h2>
 * <pre>{@code
 * // Rename target
 * assertThat(operation)
 *     .hasOperationType(FieldOperationType.RENAME)
 *     .hasTargetFieldName("name");
 *
 * // Operation without a target
 * assertThat(removeOperation).hasNoTargetFieldName();
 * }</pre>
 *
 * @author Erik Pförtner
 * @see FieldOperation
 * @see FieldOperationType
 * @see AetherAssertions
 * @see FieldAwareRuleAssert
 * @since 1.0.0
 */
public final class FieldOperationAssert extends AbstractAssert<FieldOperationAssert, FieldOperation> {

    /**
     * Creates a new assertion for the given field operation.
     *
     * @param actual the field operation to assert on, may be {@code null}
     */
    public FieldOperationAssert(@Nullable final FieldOperation actual) {
        super(actual, FieldOperationAssert.class);
    }

    // ==================== Operation Type ====================

    /**
     * Asserts that the field operation has the given operation type.
     *
     * @param expected the expected operation type, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldOperationAssert hasOperationType(@NotNull final FieldOperationType expected) {
        isNotNull();
        if (this.actual.operationType() != expected) {
            failWithMessage("Expected operation type to be <%s> but was <%s>",
                    expected, this.actual.operationType());
        }
        return this;
    }

    // ==================== Field Path ====================

    /**
     * Asserts that the field operation's path equals the given segments.
     *
     * <p>Use this for both single-segment paths (e.g., {@code hasFieldPath("name")})
     * and nested paths (e.g., {@code hasFieldPath("position", "x")}).</p>
     *
     * @param expectedSegments the expected path segments, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldOperationAssert hasFieldPath(@NotNull final String... expectedSegments) {
        isNotNull();
        final List<String> expected = Arrays.asList(expectedSegments);
        if (!this.actual.fieldPath().equals(expected)) {
            failWithMessage("Expected field path to be <%s> but was <%s>",
                    expected, this.actual.fieldPath());
        }
        return this;
    }

    /**
     * Asserts that the field operation's path, joined as a dot-notation string,
     * equals the given value.
     *
     * <p>This is a convenience for nested paths: instead of
     * {@code hasFieldPath("position", "x")} you can write
     * {@code hasFieldPathString("position.x")}.</p>
     *
     * @param expected the expected dot-notation path, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldOperationAssert hasFieldPathString(@NotNull final String expected) {
        isNotNull();
        if (!this.actual.fieldPathString().equals(expected)) {
            failWithMessage("Expected field path string to be <%s> but was <%s>",
                    expected, this.actual.fieldPathString());
        }
        return this;
    }

    /**
     * Asserts that the field operation targets a nested field (path depth &gt; 1).
     *
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldOperationAssert isNested() {
        isNotNull();
        if (!this.actual.isNested()) {
            failWithMessage("Expected field operation to be nested but path was <%s>",
                    this.actual.fieldPath());
        }
        return this;
    }

    /**
     * Asserts that the field operation targets a top-level field (path depth == 1).
     *
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldOperationAssert isNotNested() {
        isNotNull();
        if (this.actual.isNested()) {
            failWithMessage("Expected field operation to be top-level but path was <%s>",
                    this.actual.fieldPath());
        }
        return this;
    }

    // ==================== Target Field Name ====================

    /**
     * Asserts that the field operation has the given target field name.
     *
     * @param expected the expected target name, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldOperationAssert hasTargetFieldName(@NotNull final String expected) {
        isNotNull();
        if (this.actual.targetFieldName() == null
                || !this.actual.targetFieldName().equals(expected)) {
            failWithMessage("Expected target field name to be <%s> but was <%s>",
                    expected, this.actual.targetFieldName());
        }
        return this;
    }

    /**
     * Asserts that the field operation has no target field name.
     *
     * <p>Operations like {@code REMOVE}, {@code ADD}, {@code TRANSFORM}, and
     * {@code SET} typically have no target — only operations like {@code RENAME},
     * {@code MOVE}, and {@code COPY} carry one.</p>
     *
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldOperationAssert hasNoTargetFieldName() {
        isNotNull();
        if (this.actual.targetFieldName() != null) {
            failWithMessage("Expected no target field name but was <%s>",
                    this.actual.targetFieldName());
        }
        return this;
    }

    // ==================== Description ====================

    /**
     * Asserts that the field operation has the given description.
     *
     * @param expected the expected description, must not be {@code null}
     * @return this assertion for chaining, never {@code null}
     */
    @NotNull
    public FieldOperationAssert hasDescription(@NotNull final String expected) {
        isNotNull();
        if (this.actual.description() == null
                || !this.actual.description().equals(expected)) {
            failWithMessage("Expected description to be <%s> but was <%s>",
                    expected, this.actual.description());
        }
        return this;
    }
}
