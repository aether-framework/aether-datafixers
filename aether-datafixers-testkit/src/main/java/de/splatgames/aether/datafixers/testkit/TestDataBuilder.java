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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A fluent builder for creating {@link Dynamic} objects.
 *
 * <p>This builder provides a clean, readable API for constructing test data
 * without the boilerplate of manual JSON construction. It supports primitives,
 * nested objects, and lists through method chaining.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * Dynamic<JsonElement> player = TestData.gson()
 *     .object()
 *     .put("name", "Alice")
 *     .put("level", 10)
 *     .put("active", true)
 *     .build();
 * }</pre>
 *
 * <h2>Nested Objects</h2>
 * <pre>{@code
 * Dynamic<JsonElement> data = TestData.gson()
 *     .object()
 *     .putObject("position", pos -> pos
 *         .put("x", 100)
 *         .put("y", 64)
 *         .put("z", -200))
 *     .putObject("stats", stats -> stats
 *         .put("health", 100)
 *         .put("mana", 50))
 *     .build();
 * }</pre>
 *
 * <h2>Lists</h2>
 * <pre>{@code
 * Dynamic<JsonElement> data = TestData.gson()
 *     .object()
 *     .putStrings("tags", "admin", "verified", "premium")
 *     .putInts("scores", 10, 20, 30)
 *     .putList("items", list -> list
 *         .addObject(item -> item.put("id", "sword"))
 *         .addObject(item -> item.put("id", "shield")))
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This builder is NOT thread-safe. Each test should create its own builder instance.</p>
 *
 * @param <T> the underlying value type (e.g., {@code JsonElement})
 * @author Erik Pförtner
 * @see TestData
 * @see TestDataListBuilder
 * @since 0.2.0
 */
public final class TestDataBuilder<T> {

    private final DynamicOps<T> ops;
    private final Map<String, Dynamic<T>> fields;
    private boolean isObjectMode;

    /**
     * Creates a new builder with the specified {@link DynamicOps}.
     *
     * @param ops the DynamicOps to use
     */
    TestDataBuilder(@NotNull final DynamicOps<T> ops) {
        this.ops = Preconditions.checkNotNull(ops, "ops must not be null");
        this.fields = new LinkedHashMap<>();
        this.isObjectMode = false;
    }

    // ==================== Entry Point ====================

    /**
     * Starts building an object (map) structure.
     *
     * <p>Call this method first before adding fields with {@code put} methods.</p>
     *
     * @return this builder for chaining
     */
    @NotNull
    public TestDataBuilder<T> object() {
        this.isObjectMode = true;
        return this;
    }

    /**
     * Creates a list builder using the same DynamicOps.
     *
     * <pre>{@code
     * Dynamic<JsonElement> list = TestData.gson()
     *     .list()
     *     .add("first")
     *     .add("second")
     *     .add("third")
     *     .build();
     * }</pre>
     *
     * @return a new TestDataListBuilder
     */
    @NotNull
    public TestDataListBuilder<T> list() {
        return new TestDataListBuilder<>(this.ops);
    }

    // ==================== Primitive Fields ====================

    /**
     * Adds a string field.
     *
     * @param key   the field name
     * @param value the string value
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} or {@code value} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> put(@NotNull final String key, @NotNull final String value) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        this.ensureObjectMode();
        this.fields.put(key, new Dynamic<>(this.ops, this.ops.createString(value)));
        return this;
    }

    /**
     * Adds an integer field.
     *
     * @param key   the field name
     * @param value the integer value
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> put(@NotNull final String key, final int value) {
        Preconditions.checkNotNull(key, "key must not be null");
        this.ensureObjectMode();
        this.fields.put(key, new Dynamic<>(this.ops, this.ops.createInt(value)));
        return this;
    }

    /**
     * Adds a long field.
     *
     * @param key   the field name
     * @param value the long value
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> put(@NotNull final String key, final long value) {
        Preconditions.checkNotNull(key, "key must not be null");
        this.ensureObjectMode();
        this.fields.put(key, new Dynamic<>(this.ops, this.ops.createLong(value)));
        return this;
    }

    /**
     * Adds a double field.
     *
     * @param key   the field name
     * @param value the double value
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> put(@NotNull final String key, final double value) {
        Preconditions.checkNotNull(key, "key must not be null");
        this.ensureObjectMode();
        this.fields.put(key, new Dynamic<>(this.ops, this.ops.createDouble(value)));
        return this;
    }

    /**
     * Adds a float field.
     *
     * @param key   the field name
     * @param value the float value
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> put(@NotNull final String key, final float value) {
        Preconditions.checkNotNull(key, "key must not be null");
        this.ensureObjectMode();
        this.fields.put(key, new Dynamic<>(this.ops, this.ops.createFloat(value)));
        return this;
    }

    /**
     * Adds a boolean field.
     *
     * @param key   the field name
     * @param value the boolean value
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> put(@NotNull final String key, final boolean value) {
        Preconditions.checkNotNull(key, "key must not be null");
        this.ensureObjectMode();
        this.fields.put(key, new Dynamic<>(this.ops, this.ops.createBoolean(value)));
        return this;
    }

    /**
     * Adds a byte field.
     *
     * @param key   the field name
     * @param value the byte value
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> put(@NotNull final String key, final byte value) {
        Preconditions.checkNotNull(key, "key must not be null");
        this.ensureObjectMode();
        this.fields.put(key, new Dynamic<>(this.ops, this.ops.createByte(value)));
        return this;
    }

    /**
     * Adds a short field.
     *
     * @param key   the field name
     * @param value the short value
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> put(@NotNull final String key, final short value) {
        Preconditions.checkNotNull(key, "key must not be null");
        this.ensureObjectMode();
        this.fields.put(key, new Dynamic<>(this.ops, this.ops.createShort(value)));
        return this;
    }

    /**
     * Adds a pre-built {@link Dynamic} field.
     *
     * @param key   the field name
     * @param value the Dynamic value
     * @return this builder for chaining
     * @throws NullPointerException     if {@code key} or {@code value} is null
     * @throws IllegalStateException    if {@link #object()} was not called first
     * @throws IllegalArgumentException if the Dynamic uses different DynamicOps
     */
    @NotNull
    public TestDataBuilder<T> put(@NotNull final String key, @NotNull final Dynamic<T> value) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkArgument(this.ops == value.ops(),
                "Dynamic uses different DynamicOps");
        this.ensureObjectMode();
        this.fields.put(key, value);
        return this;
    }

    // ==================== Nested Object Fields ====================

    /**
     * Adds a nested object field using a builder consumer.
     *
     * <pre>{@code
     * builder.putObject("position", pos -> pos
     *     .put("x", 100)
     *     .put("y", 64)
     *     .put("z", -200));
     * }</pre>
     *
     * @param key    the field name
     * @param nested the consumer to configure the nested object
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} or {@code nested} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> putObject(@NotNull final String key,
                                         @NotNull final Consumer<TestDataBuilder<T>> nested) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(nested, "nested must not be null");
        this.ensureObjectMode();

        final TestDataBuilder<T> nestedBuilder = new TestDataBuilder<>(this.ops).object();
        nested.accept(nestedBuilder);
        this.fields.put(key, nestedBuilder.build());
        return this;
    }

    // ==================== List Fields ====================

    /**
     * Adds a list field using a builder consumer.
     *
     * <pre>{@code
     * builder.putList("inventory", list -> list
     *     .addObject(item -> item.put("id", "sword").put("count", 1))
     *     .addObject(item -> item.put("id", "apple").put("count", 64)));
     * }</pre>
     *
     * @param key  the field name
     * @param list the consumer to configure the list
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} or {@code list} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> putList(@NotNull final String key,
                                       @NotNull final Consumer<TestDataListBuilder<T>> list) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(list, "list must not be null");
        this.ensureObjectMode();

        final TestDataListBuilder<T> listBuilder = new TestDataListBuilder<>(this.ops);
        list.accept(listBuilder);
        this.fields.put(key, listBuilder.build());
        return this;
    }

    /**
     * Adds a list of strings.
     *
     * @param key    the field name
     * @param values the string values
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} or any value is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> putStrings(@NotNull final String key,
                                          @NotNull final String... values) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(values, "values must not be null");
        this.ensureObjectMode();

        final T list = this.ops.createList(
                Stream.of(values).map(v -> {
                    Preconditions.checkNotNull(v, "value must not be null");
                    return this.ops.createString(v);
                })
        );
        this.fields.put(key, new Dynamic<>(this.ops, list));
        return this;
    }

    /**
     * Adds a list of integers.
     *
     * @param key    the field name
     * @param values the integer values
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> putInts(@NotNull final String key,
                                       final int... values) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(values, "values must not be null");
        this.ensureObjectMode();

        final T list = this.ops.createList(
                java.util.Arrays.stream(values)
                        .mapToObj(this.ops::createInt)
        );
        this.fields.put(key, new Dynamic<>(this.ops, list));
        return this;
    }

    /**
     * Adds a list of longs.
     *
     * @param key    the field name
     * @param values the long values
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> putLongs(@NotNull final String key,
                                        final long... values) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(values, "values must not be null");
        this.ensureObjectMode();

        final T list = this.ops.createList(
                java.util.Arrays.stream(values)
                        .mapToObj(this.ops::createLong)
        );
        this.fields.put(key, new Dynamic<>(this.ops, list));
        return this;
    }

    /**
     * Adds a list of doubles.
     *
     * @param key    the field name
     * @param values the double values
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> putDoubles(@NotNull final String key,
                                          final double... values) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(values, "values must not be null");
        this.ensureObjectMode();

        final T list = this.ops.createList(
                java.util.Arrays.stream(values)
                        .mapToObj(this.ops::createDouble)
        );
        this.fields.put(key, new Dynamic<>(this.ops, list));
        return this;
    }

    /**
     * Adds a list of booleans.
     *
     * @param key    the field name
     * @param values the boolean values
     * @return this builder for chaining
     * @throws NullPointerException  if {@code key} is null
     * @throws IllegalStateException if {@link #object()} was not called first
     */
    @NotNull
    public TestDataBuilder<T> putBooleans(@NotNull final String key,
                                           final boolean... values) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(values, "values must not be null");
        this.ensureObjectMode();

        final Stream.Builder<T> streamBuilder = Stream.builder();
        for (final boolean v : values) {
            streamBuilder.add(this.ops.createBoolean(v));
        }
        final T list = this.ops.createList(streamBuilder.build());
        this.fields.put(key, new Dynamic<>(this.ops, list));
        return this;
    }

    // ==================== Quick Primitives (Direct Return) ====================

    /**
     * Creates a string {@link Dynamic} directly.
     *
     * @param value the string value
     * @return a Dynamic containing the string
     * @throws NullPointerException if {@code value} is null
     */
    @NotNull
    public Dynamic<T> string(@NotNull final String value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return new Dynamic<>(this.ops, this.ops.createString(value));
    }

    /**
     * Creates an integer {@link Dynamic} directly.
     *
     * @param value the integer value
     * @return a Dynamic containing the integer
     */
    @NotNull
    public Dynamic<T> integer(final int value) {
        return new Dynamic<>(this.ops, this.ops.createInt(value));
    }

    /**
     * Creates a long {@link Dynamic} directly.
     *
     * @param value the long value
     * @return a Dynamic containing the long
     */
    @NotNull
    public Dynamic<T> longValue(final long value) {
        return new Dynamic<>(this.ops, this.ops.createLong(value));
    }

    /**
     * Creates a double {@link Dynamic} directly.
     *
     * @param value the double value
     * @return a Dynamic containing the double
     */
    @NotNull
    public Dynamic<T> doubleValue(final double value) {
        return new Dynamic<>(this.ops, this.ops.createDouble(value));
    }

    /**
     * Creates a boolean {@link Dynamic} directly.
     *
     * @param value the boolean value
     * @return a Dynamic containing the boolean
     */
    @NotNull
    public Dynamic<T> bool(final boolean value) {
        return new Dynamic<>(this.ops, this.ops.createBoolean(value));
    }

    /**
     * Creates an empty map {@link Dynamic} directly.
     *
     * @return an empty Dynamic map
     */
    @NotNull
    public Dynamic<T> emptyMap() {
        return new Dynamic<>(this.ops, this.ops.emptyMap());
    }

    /**
     * Creates an empty list {@link Dynamic} directly.
     *
     * @return an empty Dynamic list
     */
    @NotNull
    public Dynamic<T> emptyList() {
        return new Dynamic<>(this.ops, this.ops.emptyList());
    }

    // ==================== Build ====================

    /**
     * Builds the {@link Dynamic} object from the configured fields.
     *
     * @return the built Dynamic
     * @throws IllegalStateException if {@link #object()} was not called
     */
    @NotNull
    public Dynamic<T> build() {
        if (!this.isObjectMode) {
            throw new IllegalStateException(
                    "Call object() before building. Use string(), integer(), etc. for direct primitive creation.");
        }

        // Build the map from fields
        Dynamic<T> result = new Dynamic<>(this.ops, this.ops.emptyMap());
        for (final Map.Entry<String, Dynamic<T>> entry : this.fields.entrySet()) {
            result = result.set(entry.getKey(), entry.getValue());
        }
        return result;
    }

    // ==================== Internal ====================

    /**
     * Returns the DynamicOps used by this builder.
     *
     * @return the DynamicOps
     */
    @NotNull
    DynamicOps<T> ops() {
        return this.ops;
    }

    private void ensureObjectMode() {
        if (!this.isObjectMode) {
            throw new IllegalStateException("Call object() before adding fields");
        }
    }
}
