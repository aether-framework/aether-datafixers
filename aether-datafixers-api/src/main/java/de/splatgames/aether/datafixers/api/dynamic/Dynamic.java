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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * An immutable wrapper pairing a value with its associated {@link DynamicOps}.
 *
 * <p>{@code Dynamic} provides a convenient, fluent API for working with dynamic
 * data structures. It encapsulates both the underlying value and the operations needed to manipulate it, enabling
 * chained operations without repeatedly passing the ops instance.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Type Checking:</b> {@link #isMap()}, {@link #isList()}, {@link #isString()}, etc.</li>
 *   <li><b>Value Reading:</b> {@link #asString()}, {@link #asInt()}, {@link #asBoolean()}, etc.</li>
 *   <li><b>Map Operations:</b> {@link #get(String)}, {@link #set(String, Dynamic)},
 *       {@link #update(String, Function)}, etc.</li>
 *   <li><b>List Operations:</b> {@link #asListStream()}, {@link #createList(Stream)}</li>
 *   <li><b>Value Creation:</b> {@link #createString(String)}, {@link #createInt(int)}, etc.</li>
 *   <li><b>Format Conversion:</b> {@link #convert(DynamicOps)}</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a Dynamic from a JSON element
 * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
 *
 * // Read values
 * String name = dynamic.get("name").asString().result().orElse("unknown");
 * int age = dynamic.get("age").asInt().result().orElse(0);
 *
 * // Modify values (returns new Dynamic instances)
 * Dynamic<JsonElement> updated = dynamic
 *     .set("name", dynamic.createString("Alice"))
 *     .set("age", dynamic.createInt(30));
 *
 * // Convert to a different format
 * Dynamic<JsonNode> jackson = dynamic.convert(JacksonOps.INSTANCE);
 * }</pre>
 *
 * <h2>Immutability</h2>
 * <p>{@code Dynamic} is immutable. All modification operations return new
 * {@code Dynamic} instances rather than modifying the original.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>{@code Dynamic} is thread-safe if the underlying value type and
 * {@link DynamicOps} implementation are thread-safe.</p>
 *
 * @param ops   the dynamic operations for the value's format, must not be {@code null}
 * @param value the underlying value, must not be {@code null}
 * @param <T>   the underlying value representation (e.g., {@code JsonElement})
 * @author Erik Pförtner
 * @see DynamicOps
 * @see TaggedDynamic
 * @since 0.1.0
 */
public record Dynamic<T>(@NotNull DynamicOps<T> ops, @NotNull T value) {

    /**
     * Creates a new Dynamic with validation.
     *
     * @throws NullPointerException if {@code ops} or {@code value} is {@code null}
     */
    public Dynamic {
        Preconditions.checkNotNull(ops, "DynamicOps must not be null");
        Preconditions.checkNotNull(value, "Value must not be null");
    }

    // ==================== Type Checks ====================

    /**
     * Checks if this value is a map/object.
     *
     * @return true if this is a map
     */
    public boolean isMap() {
        return this.ops.isMap(this.value);
    }

    /**
     * Checks if this value is a list/array.
     *
     * @return true if this is a list
     */
    public boolean isList() {
        return this.ops.isList(this.value);
    }

    /**
     * Checks if this value is a string.
     *
     * @return true if this is a string
     */
    public boolean isString() {
        return this.ops.isString(this.value);
    }

    /**
     * Checks if this value is a number.
     *
     * @return true if this is a number
     */
    public boolean isNumber() {
        return this.ops.isNumber(this.value);
    }

    /**
     * Checks if this value is a boolean.
     *
     * @return true if this is a boolean
     */
    public boolean isBoolean() {
        return this.ops.isBoolean(this.value);
    }

    // ==================== Primitive Reading ====================

    /**
     * Reads this value as a string.
     *
     * @return the string value, or an error
     */
    @NotNull
    public DataResult<String> asString() {
        return this.ops.getStringValue(this.value);
    }

    /**
     * Reads this value as a number.
     *
     * @return the number value, or an error
     */
    @NotNull
    public DataResult<Number> asNumber() {
        return this.ops.getNumberValue(this.value);
    }

    /**
     * Reads this value as an integer.
     *
     * @return the integer value, or an error
     */
    @NotNull
    public DataResult<Integer> asInt() {
        return this.ops.getNumberValue(this.value).map(Number::intValue);
    }

    /**
     * Reads this value as a long.
     *
     * @return the long value, or an error
     */
    @NotNull
    public DataResult<Long> asLong() {
        return this.ops.getNumberValue(this.value).map(Number::longValue);
    }

    /**
     * Reads this value as a float.
     *
     * @return the float value, or an error
     */
    @NotNull
    public DataResult<Float> asFloat() {
        return this.ops.getNumberValue(this.value).map(Number::floatValue);
    }

    /**
     * Reads this value as a double.
     *
     * @return the double value, or an error
     */
    @NotNull
    public DataResult<Double> asDouble() {
        return this.ops.getNumberValue(this.value).map(Number::doubleValue);
    }

    /**
     * Reads this value as a byte.
     *
     * @return the byte value, or an error
     */
    @NotNull
    public DataResult<Byte> asByte() {
        return this.ops.getNumberValue(this.value).map(Number::byteValue);
    }

    /**
     * Reads this value as a short.
     *
     * @return the short value, or an error
     */
    @NotNull
    public DataResult<Short> asShort() {
        return this.ops.getNumberValue(this.value).map(Number::shortValue);
    }

    /**
     * Reads this value as a boolean.
     *
     * @return the boolean value, or an error
     */
    @NotNull
    public DataResult<Boolean> asBoolean() {
        return this.ops.getBooleanValue(this.value);
    }

    // ==================== List Operations ====================

    /**
     * Reads this value as a stream of Dynamic elements.
     *
     * @return a stream of elements, or an error
     */
    @NotNull
    public DataResult<Stream<Dynamic<T>>> asListStream() {
        return this.ops.getList(this.value).map(s -> s.map(v -> new Dynamic<>(this.ops, v)));
    }

    /**
     * Creates a new list Dynamic from a stream of values.
     *
     * @param values the values
     * @return a new Dynamic containing a list
     */
    @NotNull
    public Dynamic<T> createList(@NotNull final Stream<Dynamic<T>> values) {
        Preconditions.checkNotNull(values, "values must not be null");
        return new Dynamic<>(this.ops, this.ops.createList(values.map(Dynamic::value)));
    }

    // ==================== Map Operations ====================

    /**
     * Checks if a key exists in this map.
     *
     * @param key the key
     * @return true if the key exists
     */
    public boolean has(@NotNull final String key) {
        Preconditions.checkNotNull(key, "key must not be null");
        return this.ops.has(this.value, key);
    }

    /**
     * Gets a value from this map by key.
     *
     * @param key the key
     * @return the value, or null if not found
     */
    @Nullable
    public Dynamic<T> get(@NotNull final String key) {
        Preconditions.checkNotNull(key, "key must not be null");
        final T child = this.ops.get(this.value, key);
        return child == null ? null : new Dynamic<>(this.ops, child);
    }

    /**
     * Gets a value from this map by key, or an empty value if not found.
     *
     * @param key the key
     * @return the value, or an empty Dynamic
     */
    @NotNull
    public Dynamic<T> getOrEmpty(@NotNull final String key) {
        final Dynamic<T> child = this.get(key);
        return child != null ? child : this.emptyMap();
    }

    /**
     * Gets a value from this map as an Optional.
     *
     * @param key the key
     * @return the value as Optional
     */
    @NotNull
    public Optional<Dynamic<T>> getOptional(@NotNull final String key) {
        return Optional.ofNullable(this.get(key));
    }

    /**
     * Sets a value in this map.
     *
     * @param key      the key
     * @param newValue the new value
     * @return a new Dynamic with the updated map
     */
    @NotNull
    public Dynamic<T> set(@NotNull final String key,
                          @NotNull final Dynamic<T> newValue) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(newValue, "newValue must not be null");
        Preconditions.checkArgument(this.ops == newValue.ops, "DynamicOps mismatch");

        final T updated = this.ops.set(this.value, key, newValue.value);
        return new Dynamic<>(this.ops, updated);
    }

    /**
     * Removes a key from this map.
     *
     * @param key the key to remove
     * @return a new Dynamic without the key
     */
    @NotNull
    public Dynamic<T> remove(@NotNull final String key) {
        Preconditions.checkNotNull(key, "key must not be null");
        final T updated = this.ops.remove(this.value, key);
        return new Dynamic<>(this.ops, updated);
    }

    /**
     * Updates a field in this map using the given function.
     *
     * @param key     the key
     * @param updater the update function
     * @return a new Dynamic with the updated field
     */
    @NotNull
    public Dynamic<T> update(@NotNull final String key,
                             @NotNull final Function<Dynamic<T>, Dynamic<T>> updater) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(updater, "updater must not be null");

        final Dynamic<T> current = this.get(key);
        if (current == null) {
            return this;
        }
        final Dynamic<T> updated = updater.apply(current);
        return this.set(key, updated);
    }

    /**
     * Reads this map as a stream of key-value pairs.
     *
     * @return a stream of pairs, or an error
     */
    @NotNull
    public DataResult<Stream<Pair<Dynamic<T>, Dynamic<T>>>> asMapStream() {
        return this.ops.getMapEntries(this.value)
                .map(s -> s.map(p -> Pair.of(
                        new Dynamic<>(this.ops, Objects.requireNonNull(p.first(), "p.first() must not be null")),
                        new Dynamic<>(this.ops, Objects.requireNonNull(p.second(), "p.second() must not be null"))
                )));
    }

    // ==================== Creation Helpers ====================

    /**
     * Creates an empty Dynamic using the default empty representation.
     *
     * @return an empty Dynamic
     */
    @NotNull
    public Dynamic<T> empty() {
        return new Dynamic<>(this.ops, this.ops.empty());
    }

    /**
     * Creates an empty map Dynamic.
     *
     * @return an empty map Dynamic
     */
    @NotNull
    public Dynamic<T> emptyMap() {
        return new Dynamic<>(this.ops, this.ops.emptyMap());
    }

    /**
     * Creates an empty list Dynamic.
     *
     * @return an empty list Dynamic
     */
    @NotNull
    public Dynamic<T> emptyList() {
        return new Dynamic<>(this.ops, this.ops.emptyList());
    }

    /**
     * Creates a Dynamic containing a string.
     *
     * @param value the string value
     * @return a new Dynamic
     */
    @NotNull
    public Dynamic<T> createString(@NotNull final String value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return new Dynamic<>(this.ops, this.ops.createString(value));
    }

    /**
     * Creates a Dynamic containing an integer.
     *
     * @param value the integer value
     * @return a new Dynamic
     */
    @NotNull
    public Dynamic<T> createInt(final int value) {
        return new Dynamic<>(this.ops, this.ops.createInt(value));
    }

    /**
     * Creates a Dynamic containing a long.
     *
     * @param value the long value
     * @return a new Dynamic
     */
    @NotNull
    public Dynamic<T> createLong(final long value) {
        return new Dynamic<>(this.ops, this.ops.createLong(value));
    }

    /**
     * Creates a Dynamic containing a float.
     *
     * @param value the float value
     * @return a new Dynamic
     */
    @NotNull
    public Dynamic<T> createFloat(final float value) {
        return new Dynamic<>(this.ops, this.ops.createFloat(value));
    }

    /**
     * Creates a Dynamic containing a double.
     *
     * @param value the double value
     * @return a new Dynamic
     */
    @NotNull
    public Dynamic<T> createDouble(final double value) {
        return new Dynamic<>(this.ops, this.ops.createDouble(value));
    }

    /**
     * Creates a Dynamic containing a byte.
     *
     * @param value the byte value
     * @return a new Dynamic
     */
    @NotNull
    public Dynamic<T> createByte(final byte value) {
        return new Dynamic<>(this.ops, this.ops.createByte(value));
    }

    /**
     * Creates a Dynamic containing a short.
     *
     * @param value the short value
     * @return a new Dynamic
     */
    @NotNull
    public Dynamic<T> createShort(final short value) {
        return new Dynamic<>(this.ops, this.ops.createShort(value));
    }

    /**
     * Creates a Dynamic containing a boolean.
     *
     * @param value the boolean value
     * @return a new Dynamic
     */
    @NotNull
    public Dynamic<T> createBoolean(final boolean value) {
        return new Dynamic<>(this.ops, this.ops.createBoolean(value));
    }

    // ==================== Conversion ====================

    /**
     * Converts this Dynamic to a different ops representation.
     *
     * @param targetOps the target ops
     * @param <U>       the target value type
     * @return a new Dynamic in the target representation
     */
    @NotNull
    public <U> Dynamic<U> convert(@NotNull final DynamicOps<U> targetOps) {
        Preconditions.checkNotNull(targetOps, "targetOps must not be null");
        final U converted = targetOps.convertTo(this.ops, this.value);
        return new Dynamic<>(targetOps, converted);
    }

    /**
     * Maps this Dynamic's value using the given function.
     *
     * @param mapper the mapping function
     * @return a new Dynamic with the mapped value
     */
    @NotNull
    public Dynamic<T> map(@NotNull final Function<T, T> mapper) {
        Preconditions.checkNotNull(mapper, "mapper must not be null");
        return new Dynamic<>(this.ops, mapper.apply(this.value));
    }
}
