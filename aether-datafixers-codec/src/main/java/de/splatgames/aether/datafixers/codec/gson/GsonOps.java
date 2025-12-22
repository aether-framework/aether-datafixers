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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link DynamicOps} implementation for Google Gson's {@link JsonElement}.
 *
 * <p>This class provides format-agnostic data manipulation capabilities for
 * Gson's JSON representation, enabling the data fixing system to read, write, and transform JSON data without coupling
 * to Gson-specific APIs.</p>
 *
 * <h2>Usage</h2>
 * <p>Use the singleton {@link #INSTANCE} for all operations:</p>
 * <pre>{@code
 * // Encode a codec value to JSON
 * DataResult<JsonElement> result = codec.encodeStart(GsonOps.INSTANCE, value);
 *
 * // Create a Dynamic wrapper for JSON data
 * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
 *
 * // Convert from another format to Gson JSON
 * JsonElement converted = GsonOps.INSTANCE.convertTo(otherOps, otherData);
 * }</pre>
 *
 * <h2>Null Handling</h2>
 * <p>JSON null values are represented as {@link JsonNull#INSTANCE}. The
 * {@link #empty()} method returns this value for representing "no value".</p>
 *
 * <h2>Type Mapping</h2>
 * <ul>
 *   <li>Booleans → {@link JsonPrimitive} with boolean</li>
 *   <li>Numbers → {@link JsonPrimitive} with number</li>
 *   <li>Strings → {@link JsonPrimitive} with string</li>
 *   <li>Lists → {@link JsonArray}</li>
 *   <li>Maps → {@link JsonObject} (keys must be strings)</li>
 *   <li>Empty/null → {@link JsonNull}</li>
 * </ul>
 *
 * <h2>Immutability</h2>
 * <p>All modification operations (e.g., {@link #set}, {@link #remove}, {@link #mergeToMap})
 * create deep copies of the input, preserving immutability of the original data.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The singleton instance can be shared across threads.</p>
 *
 * @author Erik Pförtner
 * @see DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see JsonElement
 * @since 0.1.0
 */
public final class GsonOps implements DynamicOps<JsonElement> {

    /**
     * The singleton instance of {@code GsonOps}.
     *
     * <p>This instance should be used for all Gson JSON operations.
     * It is thread-safe and can be shared across the application.</p>
     */
    public static final GsonOps INSTANCE = new GsonOps();

    private GsonOps() {
        // Singleton - use INSTANCE
    }

    // ==================== Empty/Null ====================

    /**
     * {@inheritDoc}
     *
     * @return {@link JsonNull#INSTANCE}
     */
    @NotNull
    @Override
    public JsonElement empty() {
        return JsonNull.INSTANCE;
    }

    // ==================== Type Checks ====================

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is a {@link JsonObject}
     */
    @Override
    public boolean isMap(@NotNull final JsonElement value) {
        return value.isJsonObject();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is a {@link JsonArray}
     */
    @Override
    public boolean isList(@NotNull final JsonElement value) {
        return value.isJsonArray();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is a {@link JsonPrimitive} containing a string
     */
    @Override
    public boolean isString(@NotNull final JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is a {@link JsonPrimitive} containing a number
     */
    @Override
    public boolean isNumber(@NotNull final JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is a {@link JsonPrimitive} containing a boolean
     */
    @Override
    public boolean isBoolean(@NotNull final JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
    }

    // ==================== Primitive Creation ====================

    /**
     * {@inheritDoc}
     *
     * @return a {@link JsonPrimitive} containing the string
     */
    @NotNull
    @Override
    public JsonElement createString(@NotNull final String value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link JsonPrimitive} containing the integer
     */
    @NotNull
    @Override
    public JsonElement createInt(final int value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link JsonPrimitive} containing the long
     */
    @NotNull
    @Override
    public JsonElement createLong(final long value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link JsonPrimitive} containing the float
     */
    @NotNull
    @Override
    public JsonElement createFloat(final float value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link JsonPrimitive} containing the double
     */
    @NotNull
    @Override
    public JsonElement createDouble(final double value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link JsonPrimitive} containing the byte as a number
     */
    @NotNull
    @Override
    public JsonElement createByte(final byte value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link JsonPrimitive} containing the short
     */
    @NotNull
    @Override
    public JsonElement createShort(final short value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link JsonPrimitive} containing the boolean
     */
    @NotNull
    @Override
    public JsonElement createBoolean(final boolean value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link JsonPrimitive} containing the number
     */
    @NotNull
    @Override
    public JsonElement createNumeric(@NotNull final Number value) {
        return new JsonPrimitive(value);
    }

    // ==================== Primitive Reading ====================

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the string value from a {@link JsonPrimitive}.</p>
     */
    @NotNull
    @Override
    public DataResult<String> getStringValue(@NotNull final JsonElement input) {
        if (!input.isJsonPrimitive()) {
            return DataResult.error("Not a primitive: " + input);
        }
        final JsonPrimitive primitive = input.getAsJsonPrimitive();
        if (!primitive.isString()) {
            return DataResult.error("Not a string: " + input);
        }
        return DataResult.success(primitive.getAsString());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the number value from a {@link JsonPrimitive}.</p>
     */
    @NotNull
    @Override
    public DataResult<Number> getNumberValue(@NotNull final JsonElement input) {
        if (!input.isJsonPrimitive()) {
            return DataResult.error("Not a primitive: " + input);
        }
        final JsonPrimitive primitive = input.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            return DataResult.error("Not a number: " + input);
        }
        return DataResult.success(primitive.getAsNumber());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the boolean value from a {@link JsonPrimitive}.</p>
     */
    @NotNull
    @Override
    public DataResult<Boolean> getBooleanValue(@NotNull final JsonElement input) {
        if (!input.isJsonPrimitive()) {
            return DataResult.error("Not a primitive: " + input);
        }
        final JsonPrimitive primitive = input.getAsJsonPrimitive();
        if (!primitive.isBoolean()) {
            return DataResult.error("Not a boolean: " + input);
        }
        return DataResult.success(primitive.getAsBoolean());
    }

    // ==================== List Operations ====================

    /**
     * {@inheritDoc}
     *
     * @return an empty {@link JsonArray}
     */
    @NotNull
    @Override
    public JsonElement emptyList() {
        return new JsonArray();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a {@link JsonArray} containing all elements from the stream.</p>
     */
    @NotNull
    @Override
    public JsonElement createList(@NotNull final Stream<JsonElement> values) {
        final JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the elements of a {@link JsonArray} as a stream.</p>
     */
    @NotNull
    @Override
    public DataResult<Stream<JsonElement>> getList(@NotNull final JsonElement input) {
        if (!input.isJsonArray()) {
            return DataResult.error("Not an array: " + input);
        }
        final JsonArray array = input.getAsJsonArray();
        return DataResult.success(StreamSupport.stream(array.spliterator(), false));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the list and appends the value.</p>
     */
    @NotNull
    @Override
    public DataResult<JsonElement> mergeToList(@NotNull final JsonElement list,
                                               @NotNull final JsonElement value) {
        if (!list.isJsonArray() && !list.isJsonNull()) {
            return DataResult.error("Not an array: " + list);
        }
        final JsonArray result = list.isJsonNull() ? new JsonArray() : list.getAsJsonArray().deepCopy();
        result.add(value);
        return DataResult.success(result);
    }

    // ==================== Map Operations ====================

    /**
     * {@inheritDoc}
     *
     * @return an empty {@link JsonObject}
     */
    @NotNull
    @Override
    public JsonElement emptyMap() {
        return new JsonObject();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a {@link JsonObject} from the stream of key-value pairs.
     * Keys must be JSON strings; null keys are skipped.</p>
     */
    @NotNull
    @Override
    public JsonElement createMap(@NotNull final Stream<Pair<JsonElement, JsonElement>> entries) {
        final JsonObject object = new JsonObject();
        entries.forEach(pair -> {
            final JsonElement keyElement = pair.first();
            final JsonElement valueElement = pair.second();
            if (keyElement == null) {
                return; // Skip entries with null keys
            }
            final String key = keyElement.getAsString();
            object.add(key, valueElement != null ? valueElement : JsonNull.INSTANCE);
        });
        return object;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the entries of a {@link JsonObject} as a stream of key-value pairs.</p>
     */
    @NotNull
    @Override
    public DataResult<Stream<Pair<JsonElement, JsonElement>>> getMapEntries(@NotNull final JsonElement input) {
        if (!input.isJsonObject()) {
            return DataResult.error("Not an object: " + input);
        }
        final JsonObject object = input.getAsJsonObject();
        return DataResult.success(
                object.entrySet().stream()
                        .map(entry -> Pair.of(new JsonPrimitive(entry.getKey()), entry.getValue()))
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the map and adds the key-value pair.</p>
     */
    @NotNull
    @Override
    public DataResult<JsonElement> mergeToMap(
            @NotNull final JsonElement map,
            @NotNull final JsonElement key,
            @NotNull final JsonElement value
    ) {
        if (!map.isJsonObject() && !map.isJsonNull()) {
            return DataResult.error("Not an object: " + map);
        }
        if (!key.isJsonPrimitive() || !key.getAsJsonPrimitive().isString()) {
            return DataResult.error("Key is not a string: " + key);
        }
        final JsonObject result = map.isJsonNull() ? new JsonObject() : map.getAsJsonObject().deepCopy();
        result.add(key.getAsString(), value);
        return DataResult.success(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the first map and merges all entries from the second.</p>
     */
    @NotNull
    @Override
    public DataResult<JsonElement> mergeToMap(@NotNull final JsonElement map,
                                              @NotNull final JsonElement other) {
        if (!map.isJsonObject() && !map.isJsonNull()) {
            return DataResult.error("First argument is not an object: " + map);
        }
        if (!other.isJsonObject() && !other.isJsonNull()) {
            return DataResult.error("Second argument is not an object: " + other);
        }
        final JsonObject result = map.isJsonNull() ? new JsonObject() : map.getAsJsonObject().deepCopy();
        if (!other.isJsonNull()) {
            for (final Map.Entry<String, JsonElement> entry : other.getAsJsonObject().entrySet()) {
                result.add(entry.getKey(), entry.getValue());
            }
        }
        return DataResult.success(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves a field from a {@link JsonObject} by key.</p>
     */
    @Override
    public @Nullable JsonElement get(@NotNull final JsonElement input,
                                     @NotNull final String key) {
        if (!input.isJsonObject()) {
            return null;
        }
        final JsonObject object = input.getAsJsonObject();
        if (!object.has(key)) {
            return null;
        }
        return object.get(key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the object and sets the field.</p>
     */
    @NotNull
    @Override
    public JsonElement set(@NotNull final JsonElement input,
                           @NotNull final String key,
                           @NotNull final JsonElement newValue) {
        if (!input.isJsonObject()) {
            final JsonObject result = new JsonObject();
            result.add(key, newValue);
            return result;
        }
        final JsonObject result = input.getAsJsonObject().deepCopy();
        result.add(key, newValue);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the object and removes the field.</p>
     */
    @NotNull
    @Override
    public JsonElement remove(@NotNull final JsonElement input,
                              @NotNull final String key) {
        if (!input.isJsonObject()) {
            return input;
        }
        final JsonObject result = input.getAsJsonObject().deepCopy();
        result.remove(key);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks if a {@link JsonObject} contains the specified key.</p>
     */
    @Override
    public boolean has(@NotNull final JsonElement input,
                       @NotNull final String key) {
        if (!input.isJsonObject()) {
            return false;
        }
        return input.getAsJsonObject().has(key);
    }

    // ==================== Conversion ====================

    /**
     * {@inheritDoc}
     *
     * <p>Converts data from another {@link DynamicOps} format to Gson {@link JsonElement}.
     * Recursively converts primitives, lists, and maps.</p>
     */
    @NotNull
    @Override
    public <U> JsonElement convertTo(@NotNull final DynamicOps<U> sourceOps,
                                     @NotNull final U input) {
        // Check primitives via the source ops
        final DataResult<Boolean> boolResult = sourceOps.getBooleanValue(input);
        if (boolResult.isSuccess()) {
            return createBoolean(boolResult.result().orElseThrow());
        }

        final DataResult<Number> numberResult = sourceOps.getNumberValue(input);
        if (numberResult.isSuccess()) {
            return createNumeric(numberResult.result().orElseThrow());
        }

        final DataResult<String> stringResult = sourceOps.getStringValue(input);
        if (stringResult.isSuccess()) {
            return createString(stringResult.result().orElseThrow());
        }

        // Check list
        final DataResult<Stream<U>> listResult = sourceOps.getList(input);
        if (listResult.isSuccess()) {
            return createList(
                    listResult.result().orElseThrow()
                            .map(element -> convertTo(sourceOps, element))
            );
        }

        // Check map
        final DataResult<Stream<Pair<U, U>>> mapResult = sourceOps.getMapEntries(input);
        if (mapResult.isSuccess()) {
            return createMap(
                    mapResult.result().orElseThrow()
                            .filter(entry -> entry.first() != null) // Skip entries with null keys
                            .map(entry -> {
                                final U second = entry.second();
                                return Pair.of(
                                        convertTo(sourceOps, entry.first()),
                                        second != null ? convertTo(sourceOps, second) : empty()
                                );
                            })
            );
        }

        return empty();
    }

    /**
     * Returns a string representation of this ops instance.
     *
     * @return {@code "GsonOps"}
     */
    @Override
    public String toString() {
        return "GsonOps";
    }
}
