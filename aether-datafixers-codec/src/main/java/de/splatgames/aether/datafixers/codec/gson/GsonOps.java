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

package de.splatgames.aether.datafixers.codec.gson;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Backwards-compatibility wrapper for {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps}.
 *
 * <p>This class provides API compatibility for code written against the pre-0.4.0 package structure.
 * It delegates all operations to the new {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps}
 * implementation in the reorganized package hierarchy.</p>
 *
 * <h2>Migration Guide</h2>
 * <p>To migrate to the new API, update your imports:</p>
 * <pre>{@code
 * // Old import (deprecated)
 * import de.splatgames.aether.datafixers.codec.gson.GsonOps;
 *
 * // New import (recommended)
 * import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
 * }</pre>
 *
 * <h2>Removal Timeline</h2>
 * <p>This class is scheduled for removal in version 1.0.0. All functionality remains
 * fully operational until removal, but users should migrate to the new package structure
 * at their earliest convenience.</p>
 *
 * <h2>Delegation Pattern</h2>
 * <p>This wrapper implements the delegation pattern, forwarding all method calls to the
 * underlying {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps} instance.
 * This ensures identical behavior between the deprecated and new implementations.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The singleton {@link #INSTANCE} can be safely shared
 * across multiple threads, as the underlying implementation is also thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.codec.json.gson.GsonOps
 * @see DynamicOps
 * @since 0.1.0
 * @deprecated Since 0.4.0. Use {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps}
 *             from the {@code codec.json.gson} package instead. This class will be removed
 *             in version 1.0.0 as part of the package reorganization.
 */
@Deprecated(forRemoval = true, since = "0.4.0")
public class GsonOps implements DynamicOps<JsonElement> {

    /**
     * The singleton instance of the deprecated {@code GsonOps} wrapper.
     *
     * <p>This instance wraps {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#INSTANCE}
     * and provides full backwards compatibility. It is thread-safe and can be shared across
     * the entire application.</p>
     *
     * <h3>Migration</h3>
     * <p>Replace usages with:</p>
     * <pre>{@code
     * // Old usage (deprecated)
     * GsonOps.INSTANCE
     *
     * // New usage (recommended)
     * de.splatgames.aether.datafixers.codec.json.gson.GsonOps.INSTANCE
     * }</pre>
     *
     * @deprecated Use {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#INSTANCE} instead.
     */
    @Deprecated(forRemoval = true, since = "0.4.0")
    public static final GsonOps INSTANCE = new GsonOps(de.splatgames.aether.datafixers.codec.json.gson.GsonOps.INSTANCE);

    /**
     * The underlying {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps} instance
     * to which all operations are delegated.
     *
     * <p>This field holds the actual implementation that performs all DynamicOps operations.
     * The wrapper simply forwards all method calls to this instance, ensuring behavioral
     * equivalence between the deprecated and new implementations.</p>
     */
    private final de.splatgames.aether.datafixers.codec.json.gson.GsonOps baseOps;

    /**
     * Creates a new deprecated {@code GsonOps} wrapper delegating to the specified base implementation.
     *
     * <p>This constructor allows wrapping any {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps}
     * instance, though typically the singleton {@link #INSTANCE} should be used instead.</p>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Typically use the singleton instead
     * GsonOps ops = GsonOps.INSTANCE;
     *
     * // Or wrap a custom instance if needed
     * GsonOps customOps = new GsonOps(
     *     de.splatgames.aether.datafixers.codec.json.gson.GsonOps.INSTANCE
     * );
     * }</pre>
     *
     * @param baseOps the base {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps}
     *                instance to delegate all operations to; must not be {@code null}
     * @deprecated Use {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps} directly instead.
     */
    @Deprecated(forRemoval = true, since = "0.4.0")
    private GsonOps(@NotNull final de.splatgames.aether.datafixers.codec.json.gson.GsonOps baseOps) {
        Preconditions.checkNotNull(baseOps, "baseOps must not be null");
        this.baseOps = baseOps;
    }

    // ==================== Empty/Null Values ====================

    /**
     * {@inheritDoc}
     *
     * <p>Returns the canonical empty/null representation for Gson JSON data,
     * which is {@link JsonNull#INSTANCE}. This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#empty()} method.</p>
     *
     * @return {@link JsonNull#INSTANCE} representing the absence of a value
     */
    @NotNull
    @Override
    public JsonElement empty() {
        return this.baseOps.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an empty JSON object ({@code {}}). This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#emptyMap()} method.</p>
     *
     * @return a new empty {@link JsonObject} instance
     */
    @NotNull
    @Override
    public JsonElement emptyMap() {
        return this.baseOps.emptyMap();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an empty JSON array ({@code []}). This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#emptyList()} method.</p>
     *
     * @return a new empty {@link JsonArray} instance
     */
    @NotNull
    @Override
    public JsonElement emptyList() {
        return this.baseOps.emptyList();
    }

    // ==================== Type Checking ====================

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON element is a map/object structure.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#isMap(JsonElement)} method.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonObject}, {@code false} otherwise
     */
    @Override
    public boolean isMap(@NotNull final JsonElement value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return this.baseOps.isMap(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON element is a list/array structure.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#isList(JsonElement)} method.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonArray}, {@code false} otherwise
     */
    @Override
    public boolean isList(@NotNull final JsonElement value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return this.baseOps.isList(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON element is a string primitive.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#isString(JsonElement)} method.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonPrimitive} containing a string,
     *         {@code false} otherwise
     */
    @Override
    public boolean isString(@NotNull final JsonElement value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return this.baseOps.isString(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON element is a numeric primitive.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#isNumber(JsonElement)} method.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonPrimitive} containing a number,
     *         {@code false} otherwise
     */
    @Override
    public boolean isNumber(@NotNull final JsonElement value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return this.baseOps.isNumber(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON element is a boolean primitive.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#isBoolean(JsonElement)} method.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonPrimitive} containing a boolean,
     *         {@code false} otherwise
     */
    @Override
    public boolean isBoolean(@NotNull final JsonElement value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return this.baseOps.isBoolean(value);
    }

    // ==================== Primitive Creation ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON string primitive from the given string value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createString(String)} method.</p>
     *
     * @param value the string value to wrap; must not be {@code null}
     * @return a new {@link JsonPrimitive} containing the string
     */
    @NotNull
    @Override
    public JsonElement createString(@NotNull final String value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return this.baseOps.createString(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric primitive from the given integer value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createInt(int)} method.</p>
     *
     * @param value the integer value to wrap
     * @return a new {@link JsonPrimitive} containing the integer
     */
    @NotNull
    @Override
    public JsonElement createInt(final int value) {
        return this.baseOps.createInt(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric primitive from the given long value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createLong(long)} method.</p>
     *
     * @param value the long value to wrap
     * @return a new {@link JsonPrimitive} containing the long
     */
    @NotNull
    @Override
    public JsonElement createLong(final long value) {
        return this.baseOps.createLong(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric primitive from the given float value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createFloat(float)} method.</p>
     *
     * @param value the float value to wrap
     * @return a new {@link JsonPrimitive} containing the float
     */
    @NotNull
    @Override
    public JsonElement createFloat(final float value) {
        return this.baseOps.createFloat(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric primitive from the given double value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createDouble(double)} method.</p>
     *
     * @param value the double value to wrap
     * @return a new {@link JsonPrimitive} containing the double
     */
    @NotNull
    @Override
    public JsonElement createDouble(final double value) {
        return this.baseOps.createDouble(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric primitive from the given byte value.
     * Since JSON has no distinct byte type, the value is stored as a number.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createByte(byte)} method.</p>
     *
     * @param value the byte value to wrap
     * @return a new {@link JsonPrimitive} containing the byte as a number
     */
    @NotNull
    @Override
    public JsonElement createByte(final byte value) {
        return this.baseOps.createByte(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric primitive from the given short value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createShort(short)} method.</p>
     *
     * @param value the short value to wrap
     * @return a new {@link JsonPrimitive} containing the short
     */
    @NotNull
    @Override
    public JsonElement createShort(final short value) {
        return this.baseOps.createShort(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON boolean primitive from the given boolean value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createBoolean(boolean)} method.</p>
     *
     * @param value the boolean value to wrap
     * @return a new {@link JsonPrimitive} containing the boolean
     */
    @NotNull
    @Override
    public JsonElement createBoolean(final boolean value) {
        return this.baseOps.createBoolean(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric primitive from the given {@link Number} value.
     * The specific numeric type is preserved in the underlying JSON representation.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createNumeric(Number)} method.</p>
     *
     * @param value the number value to wrap; must not be {@code null}
     * @return a new {@link JsonPrimitive} containing the number
     */
    @NotNull
    @Override
    public JsonElement createNumeric(@NotNull final Number value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return this.baseOps.createNumeric(value);
    }

    // ==================== Primitive Reading ====================

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the string value from a JSON element. The element must be a
     * {@link JsonPrimitive} containing a string value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#getStringValue(JsonElement)} method.</p>
     *
     * @param input the JSON element to extract the string from; must not be {@code null}
     * @return a {@link DataResult} containing the string value on success,
     *         or an error if the element is not a string primitive
     */
    @NotNull
    @Override
    public DataResult<String> getStringValue(@NotNull final JsonElement input) {
        Preconditions.checkNotNull(input, "input must not be null");
        return this.baseOps.getStringValue(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the numeric value from a JSON element. The element must be a
     * {@link JsonPrimitive} containing a numeric value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#getNumberValue(JsonElement)} method.</p>
     *
     * @param input the JSON element to extract the number from; must not be {@code null}
     * @return a {@link DataResult} containing the {@link Number} value on success,
     *         or an error if the element is not a numeric primitive
     */
    @NotNull
    @Override
    public DataResult<Number> getNumberValue(@NotNull final JsonElement input) {
        Preconditions.checkNotNull(input, "input must not be null");
        return this.baseOps.getNumberValue(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the boolean value from a JSON element. The element must be a
     * {@link JsonPrimitive} containing a boolean value.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#getBooleanValue(JsonElement)} method.</p>
     *
     * @param input the JSON element to extract the boolean from; must not be {@code null}
     * @return a {@link DataResult} containing the boolean value on success,
     *         or an error if the element is not a boolean primitive
     */
    @NotNull
    @Override
    public DataResult<Boolean> getBooleanValue(@NotNull final JsonElement input) {
        Preconditions.checkNotNull(input, "input must not be null");
        return this.baseOps.getBooleanValue(input);
    }

    // ==================== List Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON array from a stream of JSON elements.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createList(Stream)} method.</p>
     *
     * @param values the stream of JSON elements to include in the array; must not be {@code null}
     * @return a new {@link JsonArray} containing all elements from the stream
     */
    @NotNull
    @Override
    public JsonElement createList(@NotNull final Stream<JsonElement> values) {
        Preconditions.checkNotNull(values, "values must not be null");
        return this.baseOps.createList(values);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the elements of a JSON array as a stream. The input must be a
     * {@link JsonArray}.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#getList(JsonElement)} method.</p>
     *
     * @param input the JSON element to extract list elements from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of the array elements on success,
     *         or an error if the input is not a JSON array
     */
    @NotNull
    @Override
    public DataResult<Stream<JsonElement>> getList(@NotNull final JsonElement input) {
        Preconditions.checkNotNull(input, "input must not be null");
        return this.baseOps.getList(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON array by appending a value to an existing array.
     * The original array is not modified; a deep copy is created.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#mergeToList(JsonElement, JsonElement)} method.</p>
     *
     * @param list  the existing JSON array to append to; must not be {@code null}
     * @param value the JSON element to append; must not be {@code null}
     * @return a {@link DataResult} containing the new array with the appended value on success,
     *         or an error if the list is not a JSON array
     */
    @NotNull
    @Override
    public DataResult<JsonElement> mergeToList(@NotNull final JsonElement list, @NotNull final JsonElement value) {
        Preconditions.checkNotNull(list, "list must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        return this.baseOps.mergeToList(list, value);
    }

    // ==================== Map Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the value associated with a key from a JSON object.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#get(JsonElement, String)} method.</p>
     *
     * @param value the JSON object to retrieve from; must not be {@code null}
     * @param key   the key to look up; must not be {@code null}
     * @return the JSON element associated with the key, or {@code null} if not present
     *         or if the input is not a JSON object
     */
    @Override
    public @Nullable JsonElement get(@NotNull final JsonElement value, @NotNull final String key) {
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        return this.baseOps.get(value, key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object with a field set to the specified value.
     * If the input is a JSON object, a deep copy is created with the field updated.
     * If the input is not a JSON object, a new object is created containing only the specified field.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#set(JsonElement, String, JsonElement)} method.</p>
     *
     * @param value    the JSON element to modify; must not be {@code null}
     * @param key      the key for the field to set; must not be {@code null}
     * @param newValue the value to associate with the key; must not be {@code null}
     * @return a new {@link JsonObject} with the field set to the specified value
     */
    @NotNull
    @Override
    public JsonElement set(@NotNull final JsonElement value, @NotNull final String key, @NotNull final JsonElement newValue) {
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(newValue, "newValue must not be null");
        return this.baseOps.set(value, key, newValue);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object with a field removed.
     * A deep copy of the input object is created without the specified field.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#remove(JsonElement, String)} method.</p>
     *
     * @param value the JSON object to modify; must not be {@code null}
     * @param key   the key of the field to remove; must not be {@code null}
     * @return a new {@link JsonObject} without the specified field
     */
    @NotNull
    @Override
    public JsonElement remove(@NotNull final JsonElement value, @NotNull final String key) {
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        return this.baseOps.remove(value, key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether a JSON object contains a field with the specified key.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#has(JsonElement, String)} method.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @param key   the key to look for; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonObject} and contains the specified key,
     *         {@code false} otherwise
     */
    @Override
    public boolean has(@NotNull final JsonElement value, @NotNull final String key) {
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        return this.baseOps.has(value, key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON object from a stream of key-value pairs.
     * Keys must be JSON string primitives; non-string keys are skipped.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#createMap(Stream)} method.</p>
     *
     * @param entries the stream of key-value pairs; must not be {@code null}
     * @return a new {@link JsonObject} containing all valid entries from the stream
     */
    @NotNull
    @Override
    public JsonElement createMap(@NotNull final Stream<Pair<JsonElement, JsonElement>> entries) {
        Preconditions.checkNotNull(entries, "entries must not be null");
        return this.baseOps.createMap(entries);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the entries of a JSON object as a stream of key-value pairs.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#getMapEntries(JsonElement)} method.</p>
     *
     * @param input the JSON element to extract entries from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of key-value pairs on success,
     *         or an error if the input is not a JSON object
     */
    @NotNull
    @Override
    public DataResult<Stream<Pair<JsonElement, JsonElement>>> getMapEntries(@NotNull final JsonElement input) {
        Preconditions.checkNotNull(input, "input must not be null");
        return this.baseOps.getMapEntries(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object by adding a key-value pair to an existing map.
     * A deep copy of the input map is created with the new entry added.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#mergeToMap(JsonElement, JsonElement, JsonElement)} method.</p>
     *
     * @param map   the existing JSON object; must not be {@code null}
     * @param key   the key for the new entry (must be a JSON string); must not be {@code null}
     * @param value the value for the new entry; must not be {@code null}
     * @return a {@link DataResult} containing the new object with the added entry on success,
     *         or an error if the map is not a JSON object or the key is not a string
     */
    @NotNull
    @Override
    public DataResult<JsonElement> mergeToMap(@NotNull final JsonElement map, @NotNull final JsonElement key, @NotNull final JsonElement value) {
        Preconditions.checkNotNull(map, "map must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        return this.baseOps.mergeToMap(map, key, value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object by merging two maps together.
     * A deep copy of the first map is created, and all entries from the second map are added.
     * Entries in the second map override entries with the same key in the first map.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#mergeToMap(JsonElement, JsonElement)} method.</p>
     *
     * @param map   the base JSON object; must not be {@code null}
     * @param other the JSON object to merge from; must not be {@code null}
     * @return a {@link DataResult} containing the merged object on success,
     *         or an error if either argument is not a JSON object
     */
    @NotNull
    @Override
    public DataResult<JsonElement> mergeToMap(@NotNull final JsonElement map, @NotNull final JsonElement other) {
        Preconditions.checkNotNull(map, "map must not be null");
        Preconditions.checkNotNull(other, "other must not be null");
        return this.baseOps.mergeToMap(map, other);
    }

    // ==================== Conversion ====================

    /**
     * {@inheritDoc}
     *
     * <p>Converts data from another {@link DynamicOps} format to Gson's {@link JsonElement}.
     * Recursively converts primitives, lists, and maps to their Gson equivalents.
     * This delegates to the underlying
     * {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#convertTo(DynamicOps, Object)} method.</p>
     *
     * @param <U>   the type parameter of the target format
     * @param ops   the target {@link DynamicOps} implementation; must not be {@code null}
     * @param input the data to convert in the source format; must not be {@code null}
     * @return the converted data as a Gson {@link JsonElement}
     */
    @NotNull
    @Override
    public <U> JsonElement convertTo(@NotNull final DynamicOps<U> ops, @NotNull final U input) {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(input, "input must not be null");
        return this.baseOps.convertTo(ops, input);
    }

    /**
     * Returns a string representation of this deprecated wrapper.
     *
     * <p>The returned string clearly indicates that this is a deprecated wrapper
     * class and suggests using the new implementation instead.</p>
     *
     * @return a descriptive string indicating deprecated status and the recommended alternative
     */
    @Override
    public String toString() {
        return "GsonOps (deprecated, use de.splatgames.aether.datafixers.codec.json.gson.GsonOps)";
    }
}
