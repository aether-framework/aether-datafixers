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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.codec.jackson.JacksonOps;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for creating test data using a fluent API.
 *
 * <p>{@code TestData} provides convenient factory methods for building {@link Dynamic}
 * objects without the boilerplate of manual JSON construction. It follows the Aether
 * Datafixers philosophy: simple to start, powerful when needed.</p>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Simple object
 * Dynamic<JsonElement> player = TestData.gson()
 *     .object()
 *     .put("name", "Alice")
 *     .put("level", 10)
 *     .build();
 *
 * // Nested structures
 * Dynamic<JsonElement> data = TestData.gson()
 *     .object()
 *     .put("name", "Alice")
 *     .putObject("position", pos -> pos
 *         .put("x", 100)
 *         .put("y", 64)
 *         .put("z", -200))
 *     .putList("inventory", list -> list
 *         .addObject(item -> item.put("id", "sword").put("count", 1))
 *         .addObject(item -> item.put("id", "apple").put("count", 64)))
 *     .build();
 * }</pre>
 *
 * <h2>Different Formats</h2>
 * <pre>{@code
 * // Using Gson (default)
 * Dynamic<JsonElement> gsonData = TestData.gson().object().put("key", "value").build();
 *
 * // Using Jackson
 * Dynamic<JsonNode> jacksonData = TestData.jackson().object().put("key", "value").build();
 *
 * // Using custom DynamicOps
 * Dynamic<MyFormat> customData = TestData.using(myOps).object().put("key", "value").build();
 * }</pre>
 *
 * <h2>Quick Primitives</h2>
 * <pre>{@code
 * Dynamic<JsonElement> str = TestData.gson().string("hello");
 * Dynamic<JsonElement> num = TestData.gson().integer(42);
 * Dynamic<JsonElement> bool = TestData.gson().bool(true);
 * }</pre>
 *
 * @author Erik Pförtner
 * @see TestDataBuilder
 * @see TestDataListBuilder
 * @since 0.2.0
 */
public final class TestData {

    private TestData() {
        // Utility class
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a builder using the specified {@link DynamicOps}.
     *
     * <p>Use this method when working with custom {@code DynamicOps} implementations
     * beyond the built-in Gson and Jackson support.</p>
     *
     * @param ops the DynamicOps to use for building
     * @param <T> the underlying value type
     * @return a new {@link TestDataBuilder}
     * @throws NullPointerException if {@code ops} is null
     */
    @NotNull
    public static <T> TestDataBuilder<T> using(@NotNull final DynamicOps<T> ops) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        return new TestDataBuilder<>(ops);
    }

    /**
     * Creates a builder using {@link GsonOps}.
     *
     * <p>This is the most common choice for testing, as Gson is lightweight
     * and widely used in the Aether Datafixers ecosystem.</p>
     *
     * @return a new {@link TestDataBuilder} for Gson JSON
     */
    @NotNull
    public static TestDataBuilder<JsonElement> gson() {
        return new TestDataBuilder<>(GsonOps.INSTANCE);
    }

    /**
     * Creates a builder using {@link JacksonOps}.
     *
     * <p>Use this when testing with Jackson's JSON representation.</p>
     *
     * @return a new {@link TestDataBuilder} for Jackson JSON
     */
    @NotNull
    public static TestDataBuilder<JsonNode> jackson() {
        return new TestDataBuilder<>(JacksonOps.INSTANCE);
    }

    // ==================== Quick Primitive Helpers ====================

    /**
     * Creates a {@link Dynamic} containing a string value.
     *
     * @param ops   the DynamicOps to use
     * @param value the string value
     * @param <T>   the underlying value type
     * @return a Dynamic containing the string
     * @throws NullPointerException if {@code ops} or {@code value} is null
     */
    @NotNull
    public static <T> Dynamic<T> string(@NotNull final DynamicOps<T> ops,
                                         @NotNull final String value) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        return new Dynamic<>(ops, ops.createString(value));
    }

    /**
     * Creates a {@link Dynamic} containing an integer value.
     *
     * @param ops   the DynamicOps to use
     * @param value the integer value
     * @param <T>   the underlying value type
     * @return a Dynamic containing the integer
     * @throws NullPointerException if {@code ops} is null
     */
    @NotNull
    public static <T> Dynamic<T> integer(@NotNull final DynamicOps<T> ops,
                                          final int value) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        return new Dynamic<>(ops, ops.createInt(value));
    }

    /**
     * Creates a {@link Dynamic} containing a long value.
     *
     * @param ops   the DynamicOps to use
     * @param value the long value
     * @param <T>   the underlying value type
     * @return a Dynamic containing the long
     * @throws NullPointerException if {@code ops} is null
     */
    @NotNull
    public static <T> Dynamic<T> longValue(@NotNull final DynamicOps<T> ops,
                                            final long value) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        return new Dynamic<>(ops, ops.createLong(value));
    }

    /**
     * Creates a {@link Dynamic} containing a double value.
     *
     * @param ops   the DynamicOps to use
     * @param value the double value
     * @param <T>   the underlying value type
     * @return a Dynamic containing the double
     * @throws NullPointerException if {@code ops} is null
     */
    @NotNull
    public static <T> Dynamic<T> doubleValue(@NotNull final DynamicOps<T> ops,
                                              final double value) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        return new Dynamic<>(ops, ops.createDouble(value));
    }

    /**
     * Creates a {@link Dynamic} containing a boolean value.
     *
     * @param ops   the DynamicOps to use
     * @param value the boolean value
     * @param <T>   the underlying value type
     * @return a Dynamic containing the boolean
     * @throws NullPointerException if {@code ops} is null
     */
    @NotNull
    public static <T> Dynamic<T> bool(@NotNull final DynamicOps<T> ops,
                                       final boolean value) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        return new Dynamic<>(ops, ops.createBoolean(value));
    }

    /**
     * Creates an empty {@link Dynamic} map.
     *
     * @param ops the DynamicOps to use
     * @param <T> the underlying value type
     * @return an empty Dynamic map
     * @throws NullPointerException if {@code ops} is null
     */
    @NotNull
    public static <T> Dynamic<T> emptyMap(@NotNull final DynamicOps<T> ops) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        return new Dynamic<>(ops, ops.emptyMap());
    }

    /**
     * Creates an empty {@link Dynamic} list.
     *
     * @param ops the DynamicOps to use
     * @param <T> the underlying value type
     * @return an empty Dynamic list
     * @throws NullPointerException if {@code ops} is null
     */
    @NotNull
    public static <T> Dynamic<T> emptyList(@NotNull final DynamicOps<T> ops) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        return new Dynamic<>(ops, ops.emptyList());
    }
}
