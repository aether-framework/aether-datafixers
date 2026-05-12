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
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entry point for Aether Datafixers custom AssertJ assertions.
 *
 * <p>Import this class statically to access fluent assertions for
 * {@link Dynamic}, {@link DataResult}, and {@link Typed} objects.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;
 *
 * // Dynamic assertions
 * assertThat(playerDynamic)
 *     .isMap()
 *     .hasField("name")
 *     .hasStringField("name", "Alice")
 *     .hasIntField("level", 10);
 *
 * // DataResult assertions
 * assertThat(result)
 *     .isSuccess()
 *     .hasValue(expectedValue);
 *
 * // Typed assertions
 * assertThat(typed)
 *     .hasType(expectedType)
 *     .hasValue(expectedValue);
 * }</pre>
 *
 * <h2>Navigation</h2>
 * <pre>{@code
 * // Navigate to nested fields
 * assertThat(data)
 *     .field("position")
 *         .hasIntField("x", 100)
 *         .hasIntField("y", 64);
 *
 * // Path-based navigation
 * assertThat(data)
 *     .atPath("position.x")
 *     .hasIntValue(100);
 *
 * // List navigation
 * assertThat(data)
 *     .field("items")
 *     .atIndex(0)
 *     .hasStringField("id", "sword");
 * }</pre>
 *
 * <h2>Field-Level Diagnostics</h2>
 * <pre>{@code
 * // Assert on a captured FieldOperation record
 * assertThat(operation)
 *     .hasOperationType(FieldOperationType.RENAME)
 *     .hasFieldPath("oldName")
 *     .hasTargetFieldName("newName");
 *
 * // Assert on a FieldAwareRule (e.g., from Rules.renameField, seq, choice)
 * assertThat((FieldAwareRule) rule)
 *     .hasFieldOperationCount(2)
 *     .containsRename("oldName", "newName")
 *     .containsRemove("deprecated");
 * }</pre>
 *
 * @author Erik Pförtner
 * @see DynamicAssert
 * @see DataResultAssert
 * @see TypedAssert
 * @see FieldOperationAssert
 * @see FieldAwareRuleAssert
 * @since 0.2.0
 */
public final class AetherAssertions {

    private AetherAssertions() {
        // Entry point class
    }

    /**
     * Creates assertions for a {@link Dynamic} object.
     *
     * @param actual the Dynamic to assert on
     * @param <T>    the underlying value type
     * @return a new {@link DynamicAssert}
     */
    @NotNull
    public static <T> DynamicAssert<T> assertThat(@NotNull final Dynamic<T> actual) {
        return new DynamicAssert<>(actual);
    }

    /**
     * Creates assertions for a {@link DataResult} object.
     *
     * @param actual the DataResult to assert on
     * @param <A>    the result value type
     * @return a new {@link DataResultAssert}
     */
    @NotNull
    public static <A> DataResultAssert<A> assertThat(@NotNull final DataResult<A> actual) {
        return new DataResultAssert<>(actual);
    }

    /**
     * Creates assertions for a {@link Typed} object.
     *
     * @param actual the Typed to assert on
     * @param <A>    the typed value type
     * @return a new {@link TypedAssert}
     */
    @NotNull
    public static <A> TypedAssert<A> assertThat(@NotNull final Typed<A> actual) {
        return new TypedAssert<>(actual);
    }

    /**
     * Creates assertions for a {@link FieldOperation} record.
     *
     * @param actual the field operation to assert on, may be {@code null}
     * @return a new {@link FieldOperationAssert}, never {@code null}
     * @since 1.0.0
     */
    @NotNull
    public static FieldOperationAssert assertThat(@Nullable final FieldOperation actual) {
        return new FieldOperationAssert(actual);
    }

    /**
     * Creates assertions for a {@link FieldAwareRule} instance.
     *
     * <p>Use this when you have a rule produced by
     * {@link de.splatgames.aether.datafixers.api.rewrite.Rules Rules} factory methods
     * (e.g., {@code renameField}, {@code seq}, {@code choice}) and want to inspect
     * the field operations it carries.</p>
     *
     * @param actual the field-aware rule to assert on, may be {@code null}
     * @return a new {@link FieldAwareRuleAssert}, never {@code null}
     * @since 1.0.0
     */
    @NotNull
    public static FieldAwareRuleAssert assertThat(@Nullable final FieldAwareRule actual) {
        return new FieldAwareRuleAssert(actual);
    }
}
