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

package de.splatgames.aether.datafixers.testkit;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A fluent builder for creating {@link Dynamic} list values.
 *
 * <p>This builder is typically used through {@link TestDataBuilder#putList(String, Consumer)}
 * to create list fields, but can also be used standalone for building list data.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Through TestDataBuilder
 * Dynamic<JsonElement> data = TestData.gson()
 *     .object()
 *     .putList("inventory", list -> list
 *         .addObject(item -> item.put("id", "sword").put("count", 1))
 *         .addObject(item -> item.put("id", "apple").put("count", 64)))
 *     .build();
 *
 * // Mixed types in a list
 * Dynamic<JsonElement> data = TestData.gson()
 *     .object()
 *     .putList("mixed", list -> list
 *         .add("string")
 *         .add(42)
 *         .add(true))
 *     .build();
 *
 * // Bulk add
 * Dynamic<JsonElement> data = TestData.gson()
 *     .object()
 *     .putList("numbers", list -> list
 *         .addAll(1, 2, 3, 4, 5))
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This builder is NOT thread-safe. Each test should create its own builder instance.</p>
 *
 * @param <T> the underlying value type (e.g., {@code JsonElement})
 * @author Erik Pförtner
 * @see TestData
 * @see TestDataBuilder
 * @since 0.2.0
 */
public final class TestDataListBuilder<T> {

    private final DynamicOps<T> ops;
    private final List<Dynamic<T>> elements;

    /**
     * Creates a new list builder with the specified {@link DynamicOps}.
     *
     * @param ops the DynamicOps to use
     */
    TestDataListBuilder(@NotNull final DynamicOps<T> ops) {
        this.ops = Preconditions.checkNotNull(ops, "ops must not be null");
        this.elements = new ArrayList<>();
    }

    // ==================== Add Primitives ====================

    /**
     * Adds a string element to the list.
     *
     * @param value the string value
     * @return this builder for chaining
     * @throws NullPointerException if {@code value} is null
     */
    @NotNull
    public TestDataListBuilder<T> add(@NotNull final String value) {
        Preconditions.checkNotNull(value, "value must not be null");
        this.elements.add(new Dynamic<>(this.ops, this.ops.createString(value)));
        return this;
    }

    /**
     * Adds an integer element to the list.
     *
     * @param value the integer value
     * @return this builder for chaining
     */
    @NotNull
    public TestDataListBuilder<T> add(final int value) {
        this.elements.add(new Dynamic<>(this.ops, this.ops.createInt(value)));
        return this;
    }

    /**
     * Adds a long element to the list.
     *
     * @param value the long value
     * @return this builder for chaining
     */
    @NotNull
    public TestDataListBuilder<T> add(final long value) {
        this.elements.add(new Dynamic<>(this.ops, this.ops.createLong(value)));
        return this;
    }

    /**
     * Adds a double element to the list.
     *
     * @param value the double value
     * @return this builder for chaining
     */
    @NotNull
    public TestDataListBuilder<T> add(final double value) {
        this.elements.add(new Dynamic<>(this.ops, this.ops.createDouble(value)));
        return this;
    }

    /**
     * Adds a float element to the list.
     *
     * @param value the float value
     * @return this builder for chaining
     */
    @NotNull
    public TestDataListBuilder<T> add(final float value) {
        this.elements.add(new Dynamic<>(this.ops, this.ops.createFloat(value)));
        return this;
    }

    /**
     * Adds a boolean element to the list.
     *
     * @param value the boolean value
     * @return this builder for chaining
     */
    @NotNull
    public TestDataListBuilder<T> add(final boolean value) {
        this.elements.add(new Dynamic<>(this.ops, this.ops.createBoolean(value)));
        return this;
    }

    /**
     * Adds a byte element to the list.
     *
     * @param value the byte value
     * @return this builder for chaining
     */
    @NotNull
    public TestDataListBuilder<T> add(final byte value) {
        this.elements.add(new Dynamic<>(this.ops, this.ops.createByte(value)));
        return this;
    }

    /**
     * Adds a short element to the list.
     *
     * @param value the short value
     * @return this builder for chaining
     */
    @NotNull
    public TestDataListBuilder<T> add(final short value) {
        this.elements.add(new Dynamic<>(this.ops, this.ops.createShort(value)));
        return this;
    }

    /**
     * Adds a pre-built {@link Dynamic} element to the list.
     *
     * @param value the Dynamic value
     * @return this builder for chaining
     * @throws NullPointerException     if {@code value} is null
     * @throws IllegalArgumentException if the Dynamic uses different DynamicOps
     */
    @NotNull
    public TestDataListBuilder<T> add(@NotNull final Dynamic<T> value) {
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkArgument(this.ops == value.ops(),
                "Dynamic uses different DynamicOps");
        this.elements.add(value);
        return this;
    }

    // ==================== Add Nested Objects ====================

    /**
     * Adds a nested object element to the list using a builder consumer.
     *
     * <pre>{@code
     * listBuilder.addObject(item -> item
     *     .put("id", "sword")
     *     .put("count", 1)
     *     .put("enchanted", true));
     * }</pre>
     *
     * @param nested the consumer to configure the nested object
     * @return this builder for chaining
     * @throws NullPointerException if {@code nested} is null
     */
    @NotNull
    public TestDataListBuilder<T> addObject(@NotNull final Consumer<TestDataBuilder<T>> nested) {
        Preconditions.checkNotNull(nested, "nested must not be null");

        final TestDataBuilder<T> nestedBuilder = new TestDataBuilder<>(this.ops).object();
        nested.accept(nestedBuilder);
        this.elements.add(nestedBuilder.build());
        return this;
    }

    /**
     * Adds a nested list element to the list using a builder consumer.
     *
     * <pre>{@code
     * // Creates a list of lists: [[1, 2], [3, 4]]
     * listBuilder.addList(inner -> inner.addAll(1, 2))
     *            .addList(inner -> inner.addAll(3, 4));
     * }</pre>
     *
     * @param nested the consumer to configure the nested list
     * @return this builder for chaining
     * @throws NullPointerException if {@code nested} is null
     */
    @NotNull
    public TestDataListBuilder<T> addList(@NotNull final Consumer<TestDataListBuilder<T>> nested) {
        Preconditions.checkNotNull(nested, "nested must not be null");

        final TestDataListBuilder<T> nestedBuilder = new TestDataListBuilder<>(this.ops);
        nested.accept(nestedBuilder);
        this.elements.add(nestedBuilder.build());
        return this;
    }

    // ==================== Bulk Add ====================

    /**
     * Adds multiple string elements to the list.
     *
     * @param values the string values
     * @return this builder for chaining
     * @throws NullPointerException if {@code values} or any value is null
     */
    @NotNull
    public TestDataListBuilder<T> addAll(@NotNull final String... values) {
        Preconditions.checkNotNull(values, "values must not be null");
        for (final String value : values) {
            this.add(value);
        }
        return this;
    }

    /**
     * Adds multiple integer elements to the list.
     *
     * @param values the integer values
     * @return this builder for chaining
     * @throws NullPointerException if {@code values} is null
     */
    @NotNull
    public TestDataListBuilder<T> addAll(final int... values) {
        Preconditions.checkNotNull(values, "values must not be null");
        for (final int value : values) {
            this.add(value);
        }
        return this;
    }

    /**
     * Adds multiple long elements to the list.
     *
     * @param values the long values
     * @return this builder for chaining
     * @throws NullPointerException if {@code values} is null
     */
    @NotNull
    public TestDataListBuilder<T> addAll(final long... values) {
        Preconditions.checkNotNull(values, "values must not be null");
        for (final long value : values) {
            this.add(value);
        }
        return this;
    }

    /**
     * Adds multiple double elements to the list.
     *
     * @param values the double values
     * @return this builder for chaining
     * @throws NullPointerException if {@code values} is null
     */
    @NotNull
    public TestDataListBuilder<T> addAll(final double... values) {
        Preconditions.checkNotNull(values, "values must not be null");
        for (final double value : values) {
            this.add(value);
        }
        return this;
    }

    /**
     * Adds multiple boolean elements to the list.
     *
     * @param values the boolean values
     * @return this builder for chaining
     * @throws NullPointerException if {@code values} is null
     */
    @NotNull
    public TestDataListBuilder<T> addAll(final boolean... values) {
        Preconditions.checkNotNull(values, "values must not be null");
        for (final boolean value : values) {
            this.add(value);
        }
        return this;
    }

    // ==================== Build ====================

    /**
     * Builds the {@link Dynamic} list from the added elements.
     *
     * @return the built Dynamic list
     */
    @NotNull
    public Dynamic<T> build() {
        final T list = this.ops.createList(
                this.elements.stream().map(Dynamic::value)
        );
        return new Dynamic<>(this.ops, list);
    }

    /**
     * Returns the current number of elements in the builder.
     *
     * @return the element count
     */
    public int size() {
        return this.elements.size();
    }

    /**
     * Returns whether the builder has no elements.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return this.elements.isEmpty();
    }
}
