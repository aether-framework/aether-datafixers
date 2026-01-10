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

package de.splatgames.aether.datafixers.api.dynamic;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;

/**
 * A format-agnostic interface for manipulating dynamic data structures.
 *
 * <p>{@code DynamicOps} provides a unified API for working with various data formats
 * (JSON, NBT, YAML, etc.) without coupling code to a specific format implementation. This is the core abstraction that
 * enables the codec system to be format-independent.</p>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>Value Creation:</b> Methods like {@link #createString(String)}, {@link #createInt(int)},
 *       {@link #createMap(Stream)} create values in the target format</li>
 *   <li><b>Value Reading:</b> Methods like {@link #getStringValue(Object)}, {@link #getNumberValue(Object)},
 *       {@link #getMapEntries(Object)} extract typed values from the format</li>
 *   <li><b>Type Checking:</b> Methods like {@link #isMap(Object)}, {@link #isList(Object)},
 *       {@link #isString(Object)} check the type of a value</li>
 *   <li><b>Structure Manipulation:</b> Methods like {@link #mergeToMap(Object, Object, Object)},
 *       {@link #mergeToList(Object, Object)} modify structures</li>
 * </ul>
 *
 * <h2>Standard Implementations</h2>
 * <ul>
 *   <li>{@code GsonOps} - For Google Gson {@code JsonElement}</li>
 *   <li>{@code JacksonOps} - For Jackson {@code JsonNode}</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Using GsonOps to work with JSON
 * DynamicOps<JsonElement> ops = GsonOps.INSTANCE;
 *
 * // Create values
 * JsonElement name = ops.createString("Alice");
 * JsonElement age = ops.createInt(30);
 *
 * // Create a map (JSON object)
 * JsonElement person = ops.createMap(Stream.of(
 *     Pair.of(ops.createString("name"), name),
 *     Pair.of(ops.createString("age"), age)
 * ));
 *
 * // Read values
 * String nameValue = ops.getStringValue(ops.get(person, "name")).result().orElse("unknown");
 * int ageValue = ops.getNumberValue(ops.get(person, "age")).map(Number::intValue).result().orElse(0);
 * }</pre>
 *
 * <h2>Implementing Custom DynamicOps</h2>
 * <p>To support a new format, implement all methods of this interface for your
 * format's value type. Ensure that:</p>
 * <ul>
 *   <li>All creation methods return non-null values</li>
 *   <li>All reading methods return {@link DataResult} with appropriate errors</li>
 *   <li>Structure operations preserve immutability where appropriate</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be thread-safe, typically by being stateless.</p>
 *
 * @param <T> the underlying value representation (e.g., {@code JsonElement}, {@code JsonNode})
 * @author Erik Pförtner
 * @see Dynamic
 * @see de.splatgames.aether.datafixers.api.codec.Codec
 * @since 0.1.0
 */
public interface DynamicOps<T> {

    // ==================== Empty/Null ====================

    /**
     * Creates an empty value representation.
     *
     * <p>Implementations typically return an empty map/object node, but the exact meaning is
     * defined by the concrete ops.</p>
     *
     * @return an empty value
     */
    @NotNull T empty();

    /**
     * Creates an empty map value.
     *
     * @return an empty map
     */
    @NotNull T emptyMap();

    /**
     * Creates an empty list value.
     *
     * @return an empty list
     */
    @NotNull T emptyList();

    // ==================== Type Checks ====================

    /**
     * Checks if the given value is a map/object node.
     *
     * @param value a value
     * @return {@code true} if the given value is a map/object node
     */
    boolean isMap(@NotNull final T value);

    /**
     * Checks if the given value is a list/array node.
     *
     * @param value a value
     * @return {@code true} if the given value is a list/array
     */
    boolean isList(@NotNull final T value);

    /**
     * Checks if the given value is a string.
     *
     * @param value a value
     * @return {@code true} if the given value is a string
     */
    boolean isString(@NotNull final T value);

    /**
     * Checks if the given value is a number.
     *
     * @param value a value
     * @return {@code true} if the given value is a number
     */
    boolean isNumber(@NotNull final T value);

    /**
     * Checks if the given value is a boolean.
     *
     * @param value a value
     * @return {@code true} if the given value is a boolean
     */
    boolean isBoolean(@NotNull final T value);

    // ==================== Primitive Creation ====================

    /**
     * Creates a string value.
     *
     * @param value the string value
     * @return the created value
     */
    @NotNull T createString(@NotNull final String value);

    /**
     * Creates an integer value.
     *
     * @param value the integer value
     * @return the created value
     */
    @NotNull T createInt(final int value);

    /**
     * Creates a long value.
     *
     * @param value the long value
     * @return the created value
     */
    @NotNull T createLong(final long value);

    /**
     * Creates a float value.
     *
     * @param value the float value
     * @return the created value
     */
    @NotNull T createFloat(final float value);

    /**
     * Creates a double value.
     *
     * @param value the double value
     * @return the created value
     */
    @NotNull T createDouble(final double value);

    /**
     * Creates a byte value.
     *
     * @param value the byte value
     * @return the created value
     */
    @NotNull T createByte(final byte value);

    /**
     * Creates a short value.
     *
     * @param value the short value
     * @return the created value
     */
    @NotNull T createShort(final short value);

    /**
     * Creates a boolean value.
     *
     * @param value the boolean value
     * @return the created value
     */
    @NotNull T createBoolean(final boolean value);

    /**
     * Creates a numeric value from a Number.
     *
     * @param value the number value
     * @return the created value
     */
    @NotNull T createNumeric(@NotNull final Number value);

    // ==================== Primitive Reading ====================

    /**
     * Reads a string value.
     *
     * @param input the input value
     * @return the string value, or an error if not a string
     */
    @NotNull DataResult<String> getStringValue(@NotNull final T input);

    /**
     * Reads a number value.
     *
     * @param input the input value
     * @return the number value, or an error if not a number
     */
    @NotNull DataResult<Number> getNumberValue(@NotNull final T input);

    /**
     * Reads a boolean value.
     *
     * @param input the input value
     * @return the boolean value, or an error if not a boolean
     */
    @NotNull DataResult<Boolean> getBooleanValue(@NotNull final T input);

    // ==================== List Operations ====================

    /**
     * Creates a list value from a stream of elements.
     *
     * @param values the stream of elements
     * @return the created list value
     */
    @NotNull T createList(@NotNull final Stream<T> values);

    /**
     * Reads a list value as a stream of elements.
     *
     * @param input the input value
     * @return a stream of elements, or an error if not a list
     */
    @NotNull DataResult<Stream<T>> getList(@NotNull final T input);

    /**
     * Merges a value into a list. If the list is empty or null, creates a new list.
     *
     * @param list  the list (or empty)
     * @param value the value to add
     * @return the updated list
     */
    @NotNull DataResult<T> mergeToList(@NotNull final T list,
                                       @NotNull final T value);

    // ==================== Map Operations ====================

    /**
     * Reads the value associated with {@code key} from the given map/object node.
     *
     * @param value a map/object node
     * @param key   the key
     * @return the child value, or {@code null} if missing (or not a map, depending on impl)
     */
    @Nullable T get(@NotNull final T value,
                    @NotNull final String key);

    /**
     * Returns a new value where {@code key} is set to {@code newValue}.
     *
     * @param value    a map/object node
     * @param key      the key
     * @param newValue the new value
     * @return the updated value
     */
    @NotNull T set(@NotNull final T value,
                   @NotNull final String key,
                   @NotNull final T newValue);

    /**
     * Returns a new value where {@code key} is removed.
     *
     * @param value a map/object node
     * @param key   the key
     * @return the updated value
     */
    @NotNull T remove(@NotNull final T value,
                      @NotNull final String key);

    /**
     * Checks if the given key exists in the map/object node.
     *
     * @param value a map/object node
     * @param key   the key
     * @return {@code true} if the key exists
     */
    boolean has(@NotNull final T value,
                @NotNull final String key);

    /**
     * Creates a map value from a stream of key-value pairs.
     *
     * @param entries the stream of key-value pairs
     * @return the created map value
     */
    @NotNull T createMap(@NotNull final Stream<Pair<T, T>> entries);

    /**
     * Creates a map value from a Java Map.
     *
     * @param map the map
     * @return the created map value
     */
    @NotNull
    default T createMap(@NotNull final Map<T, T> map) {
        Preconditions.checkNotNull(map, "map must not be null");
        return createMap(map.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue())));
    }

    /**
     * Reads a map value as a stream of key-value pairs.
     *
     * @param input the input value
     * @return a stream of key-value pairs, or an error if not a map
     */
    @NotNull DataResult<Stream<Pair<T, T>>> getMapEntries(@NotNull final T input);

    /**
     * Merges a key-value pair into a map.
     *
     * @param map   the map
     * @param key   the key
     * @param value the value
     * @return the updated map
     */
    @NotNull DataResult<T> mergeToMap(@NotNull final T map,
                                      @NotNull final T key,
                                      @NotNull final T value);

    /**
     * Merges all entries from one map into another.
     *
     * @param map   the target map
     * @param other the source map to merge from
     * @return the updated map
     */
    @NotNull DataResult<T> mergeToMap(@NotNull final T map,
                                      @NotNull final T other);

    // ==================== Conversion ====================

    /**
     * Converts a value from another DynamicOps representation.
     *
     * @param ops   the source ops
     * @param input the input value
     * @param <U>   the source value type
     * @return the converted value
     */
    @NotNull <U> T convertTo(@NotNull final DynamicOps<U> ops,
                             @NotNull final U input);
}
