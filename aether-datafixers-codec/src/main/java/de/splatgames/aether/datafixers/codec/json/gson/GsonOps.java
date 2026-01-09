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

package de.splatgames.aether.datafixers.codec.json.gson;

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
 * A {@link DynamicOps} implementation for Google Gson's {@link JsonElement} type hierarchy.
 *
 * <p>This class provides format-agnostic data manipulation capabilities for Gson's JSON
 * representation, enabling the Aether Datafixers system to read, write, and transform
 * JSON data without coupling application code to Gson-specific APIs. It serves as the
 * bridge between the abstract data fixer operations and Gson's concrete JSON model.</p>
 *
 * <h2>Design Pattern</h2>
 * <p>This class implements the Singleton pattern. Use {@link #INSTANCE} for all operations.
 * The singleton design is appropriate because:</p>
 * <ul>
 *   <li>The class maintains no mutable state</li>
 *   <li>All operations are purely functional transformations</li>
 *   <li>A single instance reduces memory overhead and GC pressure</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Encoding/Decoding</h3>
 * <pre>{@code
 * // Encode a value to JSON using a codec
 * DataResult<JsonElement> encoded = playerCodec.encodeStart(GsonOps.INSTANCE, player);
 *
 * // Decode JSON to a typed value
 * DataResult<Player> decoded = playerCodec.decode(GsonOps.INSTANCE, jsonElement);
 * }</pre>
 *
 * <h3>Creating Dynamic Wrappers</h3>
 * <pre>{@code
 * // Wrap existing JSON data for manipulation
 * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, existingJson);
 *
 * // Access nested fields
 * Optional<String> name = dynamic.get("player").get("name").asString();
 *
 * // Transform data
 * Dynamic<JsonElement> updated = dynamic.set("version", GsonOps.INSTANCE.createInt(2));
 * }</pre>
 *
 * <h3>Format Conversion</h3>
 * <pre>{@code
 * // Convert from Jackson JsonNode to Gson JsonElement
 * JsonElement gsonJson = GsonOps.INSTANCE.convertTo(JacksonJsonOps.INSTANCE, jacksonNode);
 *
 * // Convert from YAML to Gson JSON
 * JsonElement fromYaml = GsonOps.INSTANCE.convertTo(SnakeYamlOps.INSTANCE, yamlData);
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <p>The following table shows how Java/abstract types map to Gson JSON types:</p>
 * <table border="1" cellpadding="5">
 *   <caption>Type Mapping between Java and Gson</caption>
 *   <tr><th>Java Type</th><th>Gson Type</th><th>Notes</th></tr>
 *   <tr><td>{@code boolean}</td><td>{@link JsonPrimitive}</td><td>Created via {@code new JsonPrimitive(boolean)}</td></tr>
 *   <tr><td>{@code int, long, float, double, byte, short}</td><td>{@link JsonPrimitive}</td><td>All numeric types stored as Number</td></tr>
 *   <tr><td>{@code String}</td><td>{@link JsonPrimitive}</td><td>Created via {@code new JsonPrimitive(String)}</td></tr>
 *   <tr><td>{@code List/Stream}</td><td>{@link JsonArray}</td><td>Preserves element order</td></tr>
 *   <tr><td>{@code Map}</td><td>{@link JsonObject}</td><td>Keys must be strings</td></tr>
 *   <tr><td>{@code null/empty}</td><td>{@link JsonNull}</td><td>Singleton {@link JsonNull#INSTANCE}</td></tr>
 * </table>
 *
 * <h2>Immutability Contract</h2>
 * <p>All modification operations in this class preserve immutability of input data:</p>
 * <ul>
 *   <li>{@link #set(JsonElement, String, JsonElement)} creates a deep copy before modification</li>
 *   <li>{@link #remove(JsonElement, String)} creates a deep copy before removal</li>
 *   <li>{@link #mergeToMap(JsonElement, JsonElement, JsonElement)} creates a deep copy of the target map</li>
 *   <li>{@link #mergeToList(JsonElement, JsonElement)} creates a deep copy of the target list</li>
 * </ul>
 * <p>This ensures that original data structures are never modified, enabling safe concurrent
 * access and functional programming patterns.</p>
 *
 * <h2>Error Handling</h2>
 * <p>Operations that may fail return {@link DataResult} instead of throwing exceptions:</p>
 * <ul>
 *   <li>Type mismatches result in {@link DataResult#error(String)} with descriptive messages</li>
 *   <li>Successful operations return {@link DataResult#success(Object)}</li>
 *   <li>Callers should check {@link DataResult#isSuccess()} or use {@link DataResult#result()}</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is fully thread-safe:</p>
 * <ul>
 *   <li>The singleton instance can be safely shared across threads</li>
 *   <li>All operations are stateless and side-effect free</li>
 *   <li>No internal mutable state exists</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Deep copy operations have O(n) complexity where n is the structure size</li>
 *   <li>Prefer batch modifications via {@link #createMap(Stream)} over repeated {@link #set} calls</li>
 *   <li>Stream-based operations are lazy and support short-circuiting</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see JsonElement
 * @see DataResult
 * @since 0.1.0
 */
public final class GsonOps implements DynamicOps<JsonElement> {

    /**
     * The singleton instance of {@code GsonOps}.
     *
     * <p>This instance should be used for all Gson JSON operations throughout the application.
     * It is immutable, stateless, and thread-safe, making it suitable for use in any context
     * including multi-threaded environments and dependency injection containers.</p>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Direct usage
     * JsonElement json = GsonOps.INSTANCE.createString("hello");
     *
     * // With Dynamic
     * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);
     *
     * // With Codecs
     * DataResult<JsonElement> result = myCodec.encodeStart(GsonOps.INSTANCE, value);
     * }</pre>
     */
    public static final GsonOps INSTANCE = new GsonOps();

    /**
     * Private constructor to enforce singleton pattern.
     *
     * <p>This constructor is intentionally private to prevent instantiation from outside
     * the class. Use {@link #INSTANCE} to access the singleton instance.</p>
     *
     * <p>The singleton pattern is used because this class is stateless and all operations
     * are pure functions, making multiple instances unnecessary and wasteful.</p>
     */
    private GsonOps() {
        // Singleton - use INSTANCE
    }

    // ==================== Empty/Null Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Returns the JSON null value, which represents the absence of a value in JSON.
     * This is the canonical "empty" value for the Gson format and is used when:</p>
     * <ul>
     *   <li>A field has no value</li>
     *   <li>A conversion cannot determine the appropriate type</li>
     *   <li>An optional value is absent</li>
     * </ul>
     *
     * @return {@link JsonNull#INSTANCE}, the singleton JSON null value; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement empty() {
        return JsonNull.INSTANCE;
    }

    // ==================== Type Check Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON element represents a map/object structure.
     * In JSON/Gson terminology, this corresponds to a {@link JsonObject} which contains
     * key-value pairs where keys are always strings.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonObject}, {@code false} otherwise
     *         (including for {@link JsonNull}, {@link JsonArray}, and {@link JsonPrimitive})
     */
    @Override
    public boolean isMap(@NotNull final JsonElement value) {
        return value.isJsonObject();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON element represents a list/array structure.
     * In JSON/Gson terminology, this corresponds to a {@link JsonArray} which contains
     * an ordered sequence of elements.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonArray}, {@code false} otherwise
     *         (including for {@link JsonNull}, {@link JsonObject}, and {@link JsonPrimitive})
     */
    @Override
    public boolean isList(@NotNull final JsonElement value) {
        return value.isJsonArray();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON element represents a string value.
     * In Gson, strings are stored as {@link JsonPrimitive} instances with the
     * {@link JsonPrimitive#isString()} property set to {@code true}.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonPrimitive} containing a string,
     *         {@code false} otherwise (including for numeric and boolean primitives)
     */
    @Override
    public boolean isString(@NotNull final JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON element represents a numeric value.
     * In Gson, numbers are stored as {@link JsonPrimitive} instances with the
     * {@link JsonPrimitive#isNumber()} property set to {@code true}. This includes
     * all numeric types: integers, longs, floats, doubles, bytes, and shorts.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonPrimitive} containing a number,
     *         {@code false} otherwise (including for string and boolean primitives)
     */
    @Override
    public boolean isNumber(@NotNull final JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON element represents a boolean value.
     * In Gson, booleans are stored as {@link JsonPrimitive} instances with the
     * {@link JsonPrimitive#isBoolean()} property set to {@code true}.</p>
     *
     * @param value the JSON element to check; must not be {@code null}
     * @return {@code true} if the value is a {@link JsonPrimitive} containing a boolean,
     *         {@code false} otherwise (including for string and numeric primitives)
     */
    @Override
    public boolean isBoolean(@NotNull final JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
    }

    // ==================== Primitive Creation Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON string primitive from the given string value. The resulting
     * {@link JsonPrimitive} will return {@code true} for {@link JsonPrimitive#isString()}.</p>
     *
     * @param value the string value to wrap; must not be {@code null}
     * @return a new {@link JsonPrimitive} containing the string value; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement createString(@NotNull final String value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON numeric primitive from the given integer value. The resulting
     * {@link JsonPrimitive} will return {@code true} for {@link JsonPrimitive#isNumber()}
     * and the value can be retrieved via {@link JsonPrimitive#getAsInt()}.</p>
     *
     * @param value the integer value to wrap
     * @return a new {@link JsonPrimitive} containing the integer value; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement createInt(final int value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON numeric primitive from the given long value. The resulting
     * {@link JsonPrimitive} will return {@code true} for {@link JsonPrimitive#isNumber()}
     * and the value can be retrieved via {@link JsonPrimitive#getAsLong()}.</p>
     *
     * <p><strong>Note:</strong> JSON does not distinguish between integer and long types.
     * Very large long values may lose precision when serialized to JSON text and parsed
     * by other JSON libraries.</p>
     *
     * @param value the long value to wrap
     * @return a new {@link JsonPrimitive} containing the long value; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement createLong(final long value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON numeric primitive from the given float value. The resulting
     * {@link JsonPrimitive} will return {@code true} for {@link JsonPrimitive#isNumber()}
     * and the value can be retrieved via {@link JsonPrimitive#getAsFloat()}.</p>
     *
     * <p><strong>Note:</strong> Special float values ({@link Float#NaN},
     * {@link Float#POSITIVE_INFINITY}, {@link Float#NEGATIVE_INFINITY}) are not valid
     * JSON and may cause issues during serialization.</p>
     *
     * @param value the float value to wrap
     * @return a new {@link JsonPrimitive} containing the float value; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement createFloat(final float value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON numeric primitive from the given double value. The resulting
     * {@link JsonPrimitive} will return {@code true} for {@link JsonPrimitive#isNumber()}
     * and the value can be retrieved via {@link JsonPrimitive#getAsDouble()}.</p>
     *
     * <p><strong>Note:</strong> Special double values ({@link Double#NaN},
     * {@link Double#POSITIVE_INFINITY}, {@link Double#NEGATIVE_INFINITY}) are not valid
     * JSON and may cause issues during serialization.</p>
     *
     * @param value the double value to wrap
     * @return a new {@link JsonPrimitive} containing the double value; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement createDouble(final double value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON numeric primitive from the given byte value. The resulting
     * {@link JsonPrimitive} will return {@code true} for {@link JsonPrimitive#isNumber()}.
     * The byte is stored as a {@link Number} and can be retrieved via
     * {@link JsonPrimitive#getAsByte()}.</p>
     *
     * <p><strong>Note:</strong> JSON does not have a dedicated byte type. The value is
     * stored as a generic number and type information may be lost during round-trips
     * through JSON text serialization.</p>
     *
     * @param value the byte value to wrap
     * @return a new {@link JsonPrimitive} containing the byte value as a number;
     *         never {@code null}
     */
    @NotNull
    @Override
    public JsonElement createByte(final byte value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON numeric primitive from the given short value. The resulting
     * {@link JsonPrimitive} will return {@code true} for {@link JsonPrimitive#isNumber()}.
     * The short is stored as a {@link Number} and can be retrieved via
     * {@link JsonPrimitive#getAsShort()}.</p>
     *
     * <p><strong>Note:</strong> JSON does not have a dedicated short type. The value is
     * stored as a generic number and type information may be lost during round-trips
     * through JSON text serialization.</p>
     *
     * @param value the short value to wrap
     * @return a new {@link JsonPrimitive} containing the short value as a number;
     *         never {@code null}
     */
    @NotNull
    @Override
    public JsonElement createShort(final short value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON boolean primitive from the given boolean value. The resulting
     * {@link JsonPrimitive} will return {@code true} for {@link JsonPrimitive#isBoolean()}
     * and the value can be retrieved via {@link JsonPrimitive#getAsBoolean()}.</p>
     *
     * @param value the boolean value to wrap
     * @return a new {@link JsonPrimitive} containing the boolean value; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement createBoolean(final boolean value) {
        return new JsonPrimitive(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON numeric primitive from the given {@link Number} value. This
     * method accepts any {@link Number} subclass and stores it directly in the primitive.
     * The resulting {@link JsonPrimitive} will return {@code true} for
     * {@link JsonPrimitive#isNumber()}.</p>
     *
     * <p>This is the most flexible numeric creation method and preserves the exact
     * {@link Number} type internally, though JSON serialization may normalize the
     * representation.</p>
     *
     * @param value the number value to wrap; must not be {@code null}
     * @return a new {@link JsonPrimitive} containing the number value; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement createNumeric(@NotNull final Number value) {
        return new JsonPrimitive(value);
    }

    // ==================== Primitive Reading Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the string value from a JSON element. This operation succeeds only if
     * the input is a {@link JsonPrimitive} that contains a string value.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is a {@link JsonPrimitive}</li>
     *   <li>{@link JsonPrimitive#isString()} returns {@code true}</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link JsonPrimitive} (e.g., array, object, null)</li>
     *   <li>Input is a primitive but contains a number or boolean</li>
     * </ul>
     *
     * @param input the JSON element to extract the string from; must not be {@code null}
     * @return a {@link DataResult} containing the string value on success, or an error
     *         message describing why extraction failed; never {@code null}
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
     * <p>Extracts the numeric value from a JSON element. This operation succeeds only if
     * the input is a {@link JsonPrimitive} that contains a numeric value.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is a {@link JsonPrimitive}</li>
     *   <li>{@link JsonPrimitive#isNumber()} returns {@code true}</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link JsonPrimitive} (e.g., array, object, null)</li>
     *   <li>Input is a primitive but contains a string or boolean</li>
     * </ul>
     *
     * <p>The returned {@link Number} preserves the original numeric type where possible,
     * allowing callers to use type-specific accessors like {@link Number#intValue()} or
     * {@link Number#doubleValue()}.</p>
     *
     * @param input the JSON element to extract the number from; must not be {@code null}
     * @return a {@link DataResult} containing the number value on success, or an error
     *         message describing why extraction failed; never {@code null}
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
     * <p>Extracts the boolean value from a JSON element. This operation succeeds only if
     * the input is a {@link JsonPrimitive} that contains a boolean value.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is a {@link JsonPrimitive}</li>
     *   <li>{@link JsonPrimitive#isBoolean()} returns {@code true}</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link JsonPrimitive} (e.g., array, object, null)</li>
     *   <li>Input is a primitive but contains a string or number</li>
     * </ul>
     *
     * @param input the JSON element to extract the boolean from; must not be {@code null}
     * @return a {@link DataResult} containing the boolean value on success, or an error
     *         message describing why extraction failed; never {@code null}
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
     * <p>Creates a new empty JSON array. This is the canonical way to create an empty
     * list structure in the Gson format.</p>
     *
     * @return a new empty {@link JsonArray}; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement emptyList() {
        return new JsonArray();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON array containing all elements from the provided stream.
     * Elements are added to the array in encounter order. The stream is consumed
     * completely by this operation.</p>
     *
     * <p><strong>Note:</strong> {@code null} elements in the stream will be added
     * as-is, which may cause issues. Ensure the stream contains only valid
     * {@link JsonElement} instances.</p>
     *
     * @param values a stream of JSON elements to include in the array; must not be
     *               {@code null}; may be empty
     * @return a new {@link JsonArray} containing all stream elements in order;
     *         never {@code null}
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
     * <p>Returns the elements of a JSON array as a stream. This operation succeeds only
     * if the input is a {@link JsonArray}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is a {@link JsonArray} (including empty arrays)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link JsonArray} (e.g., object, primitive, null)</li>
     * </ul>
     *
     * <p>The returned stream provides sequential access to array elements in order.
     * The stream does not support parallel processing.</p>
     *
     * @param input the JSON element to extract array elements from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of array elements on success,
     *         or an error message if the input is not an array; never {@code null}
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
     * <p>Creates a new array by appending a value to an existing array. This operation
     * creates a deep copy of the input array to preserve immutability.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input list is a {@link JsonArray}</li>
     *   <li>Input list is {@link JsonNull} (treated as empty array)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input list is not an array or null (e.g., object, primitive)</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original list is never modified. A deep copy
     * is created via {@link JsonArray#deepCopy()} before the new element is appended.</p>
     *
     * @param list  the array to append to; must not be {@code null}; may be
     *              {@link JsonNull} (treated as empty array)
     * @param value the value to append; must not be {@code null}
     * @return a {@link DataResult} containing the new array with the appended value,
     *         or an error message if the list is not valid; never {@code null}
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
     * <p>Creates a new empty JSON object. This is the canonical way to create an empty
     * map structure in the Gson format.</p>
     *
     * @return a new empty {@link JsonObject}; never {@code null}
     */
    @NotNull
    @Override
    public JsonElement emptyMap() {
        return new JsonObject();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object from a stream of key-value pairs. Keys must be
     * JSON string primitives; entries with {@code null} keys are skipped.</p>
     *
     * <h3>Key Handling</h3>
     * <ul>
     *   <li>Keys are converted to strings via {@link JsonElement#getAsString()}</li>
     *   <li>Entries with {@code null} keys are silently skipped</li>
     *   <li>Duplicate keys result in the last value being retained</li>
     * </ul>
     *
     * <h3>Value Handling</h3>
     * <ul>
     *   <li>{@code null} values are converted to {@link JsonNull#INSTANCE}</li>
     *   <li>All other values are added as-is</li>
     * </ul>
     *
     * @param entries a stream of key-value pairs; must not be {@code null}; may be empty
     * @return a new {@link JsonObject} containing all valid entries; never {@code null}
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
     * <p>Returns the entries of a JSON object as a stream of key-value pairs. This
     * operation succeeds only if the input is a {@link JsonObject}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is a {@link JsonObject} (including empty objects)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link JsonObject} (e.g., array, primitive, null)</li>
     * </ul>
     *
     * <p>Each entry in the returned stream has:</p>
     * <ul>
     *   <li>Key: a {@link JsonPrimitive} containing the field name as a string</li>
     *   <li>Value: the field's {@link JsonElement} value</li>
     * </ul>
     *
     * @param input the JSON element to extract object entries from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of key-value pairs on success,
     *         or an error message if the input is not an object; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<Stream<Pair<JsonElement, JsonElement>>> getMapEntries(
            @NotNull final JsonElement input) {
        if (!input.isJsonObject()) {
            return DataResult.error("Not an object: " + input);
        }
        final JsonObject object = input.getAsJsonObject();
        return DataResult.success(
                object.entrySet().stream()
                        .map(entry -> Pair.of(
                                (JsonElement) new JsonPrimitive(entry.getKey()),
                                entry.getValue()
                        ))
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new object by adding or updating a key-value pair in an existing map.
     * This operation creates a deep copy of the input map to preserve immutability.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input map is a {@link JsonObject}</li>
     *   <li>Input map is {@link JsonNull} (treated as empty object)</li>
     *   <li>Key is a {@link JsonPrimitive} containing a string</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input map is not an object or null (e.g., array, primitive)</li>
     *   <li>Key is not a string primitive</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original map is never modified. A deep copy
     * is created via {@link JsonObject#deepCopy()} before the entry is added.</p>
     *
     * @param map   the object to add the entry to; must not be {@code null}; may be
     *              {@link JsonNull} (treated as empty object)
     * @param key   the key for the entry; must not be {@code null}; must be a string primitive
     * @param value the value for the entry; must not be {@code null}
     * @return a {@link DataResult} containing the new object with the added entry,
     *         or an error message if parameters are invalid; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<JsonElement> mergeToMap(
            @NotNull final JsonElement map,
            @NotNull final JsonElement key,
            @NotNull final JsonElement value) {
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
     * <p>Creates a new object by merging all entries from a second object into a copy
     * of the first object. This operation creates a deep copy of the first map to
     * preserve immutability.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Both inputs are {@link JsonObject} instances</li>
     *   <li>Either input may be {@link JsonNull} (treated as empty object)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>First input is not an object or null</li>
     *   <li>Second input is not an object or null</li>
     * </ul>
     *
     * <h3>Merge Behavior</h3>
     * <ul>
     *   <li>Entries from the second object override entries with the same key in the first</li>
     *   <li>Entries unique to either object are included in the result</li>
     *   <li>Order is not guaranteed (depends on {@link JsonObject} implementation)</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> Neither input object is modified. A deep copy
     * of the first map is created via {@link JsonObject#deepCopy()} before merging.</p>
     *
     * @param map   the base object; must not be {@code null}; may be {@link JsonNull}
     * @param other the object to merge into the base; must not be {@code null}; may be
     *              {@link JsonNull}
     * @return a {@link DataResult} containing the merged object, or an error message
     *         if either input is invalid; never {@code null}
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
     * <p>Retrieves a field value from a JSON object by key. Returns {@code null} if
     * the input is not an object or the key does not exist.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>Returns the field value if the input is a {@link JsonObject} containing the key</li>
     *   <li>Returns {@code null} if the input is not a {@link JsonObject}</li>
     *   <li>Returns {@code null} if the key does not exist in the object</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This method does not distinguish between a missing key
     * and a key mapped to {@link JsonNull}. Use {@link #has(JsonElement, String)} to
     * check for key existence when this distinction matters.</p>
     *
     * @param input the JSON element to retrieve the field from; must not be {@code null}
     * @param key   the field name to retrieve; must not be {@code null}
     * @return the field value if found, or {@code null} if not found or input is not an object
     */
    @Override
    @Nullable
    public JsonElement get(@NotNull final JsonElement input, @NotNull final String key) {
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
     * <p>Creates a new object with a field set to the specified value. This operation
     * creates a deep copy of the input to preserve immutability.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>If input is a {@link JsonObject}: creates a deep copy and sets the field</li>
     *   <li>If input is not a {@link JsonObject}: creates a new object with just the field</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original input is never modified. When the
     * input is an object, {@link JsonObject#deepCopy()} is used to create the copy.</p>
     *
     * @param input    the JSON element to set the field on; must not be {@code null}
     * @param key      the field name to set; must not be {@code null}
     * @param newValue the value to set; must not be {@code null}
     * @return a new {@link JsonObject} with the field set; never {@code null}
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
     * <p>Creates a new object with a field removed. This operation creates a deep copy
     * of the input to preserve immutability.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>If input is a {@link JsonObject}: creates a deep copy and removes the field</li>
     *   <li>If input is not a {@link JsonObject}: returns the input unchanged</li>
     *   <li>If the key does not exist: returns a deep copy without modification</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original input is never modified. When the
     * input is an object, {@link JsonObject#deepCopy()} is used to create the copy.</p>
     *
     * @param input the JSON element to remove the field from; must not be {@code null}
     * @param key   the field name to remove; must not be {@code null}
     * @return a {@link JsonElement} with the field removed (or unchanged if not applicable);
     *         never {@code null}
     */
    @NotNull
    @Override
    public JsonElement remove(@NotNull final JsonElement input, @NotNull final String key) {
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
     * <p>Checks whether a JSON object contains a field with the specified key.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>Returns {@code true} if input is a {@link JsonObject} and contains the key</li>
     *   <li>Returns {@code false} if input is not a {@link JsonObject}</li>
     *   <li>Returns {@code false} if the key does not exist</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This method returns {@code true} even if the key is
     * mapped to {@link JsonNull}. It only checks for key existence, not value validity.</p>
     *
     * @param input the JSON element to check; must not be {@code null}
     * @param key   the field name to check for; must not be {@code null}
     * @return {@code true} if the input is an object containing the key, {@code false} otherwise
     */
    @Override
    public boolean has(@NotNull final JsonElement input, @NotNull final String key) {
        if (!input.isJsonObject()) {
            return false;
        }
        return input.getAsJsonObject().has(key);
    }

    // ==================== Conversion Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Converts data from another {@link DynamicOps} format to Gson {@link JsonElement}.
     * This method recursively converts all nested structures, handling primitives, lists,
     * and maps appropriately.</p>
     *
     * <h3>Conversion Process</h3>
     * <p>The conversion attempts to identify the input type in the following order:</p>
     * <ol>
     *   <li><strong>Boolean:</strong> If {@link DynamicOps#getBooleanValue} succeeds,
     *       creates a {@link JsonPrimitive} with the boolean value</li>
     *   <li><strong>Number:</strong> If {@link DynamicOps#getNumberValue} succeeds,
     *       creates a {@link JsonPrimitive} with the numeric value</li>
     *   <li><strong>String:</strong> If {@link DynamicOps#getStringValue} succeeds,
     *       creates a {@link JsonPrimitive} with the string value</li>
     *   <li><strong>List:</strong> If {@link DynamicOps#getList} succeeds,
     *       creates a {@link JsonArray} with recursively converted elements</li>
     *   <li><strong>Map:</strong> If {@link DynamicOps#getMapEntries} succeeds,
     *       creates a {@link JsonObject} with recursively converted entries</li>
     *   <li><strong>Fallback:</strong> Returns {@link JsonNull#INSTANCE} if no type matches</li>
     * </ol>
     *
     * <h3>Edge Cases</h3>
     * <ul>
     *   <li>Map entries with {@code null} keys are skipped</li>
     *   <li>Map entries with {@code null} values are converted to {@link JsonNull}</li>
     *   <li>Empty collections are preserved as empty arrays/objects</li>
     * </ul>
     *
     * @param sourceOps the {@link DynamicOps} instance for the source format; must not be
     *                  {@code null}
     * @param input     the value to convert from the source format; must not be {@code null}
     * @param <U>       the type parameter of the source format
     * @return the converted {@link JsonElement}; never {@code null}; returns
     *         {@link JsonNull#INSTANCE} if conversion is not possible
     */
    @NotNull
    @Override
    public <U> JsonElement convertTo(@NotNull final DynamicOps<U> sourceOps,
                                     @NotNull final U input) {
        // Attempt boolean conversion first (before number to avoid int 0/1 ambiguity)
        final DataResult<Boolean> boolResult = sourceOps.getBooleanValue(input);
        if (boolResult.isSuccess()) {
            return createBoolean(boolResult.result().orElseThrow());
        }

        // Attempt number conversion
        final DataResult<Number> numberResult = sourceOps.getNumberValue(input);
        if (numberResult.isSuccess()) {
            return createNumeric(numberResult.result().orElseThrow());
        }

        // Attempt string conversion
        final DataResult<String> stringResult = sourceOps.getStringValue(input);
        if (stringResult.isSuccess()) {
            return createString(stringResult.result().orElseThrow());
        }

        // Attempt list conversion (recursive)
        final DataResult<Stream<U>> listResult = sourceOps.getList(input);
        if (listResult.isSuccess()) {
            return createList(
                    listResult.result().orElseThrow()
                            .map(element -> convertTo(sourceOps, element))
            );
        }

        // Attempt map conversion (recursive)
        final DataResult<Stream<Pair<U, U>>> mapResult = sourceOps.getMapEntries(input);
        if (mapResult.isSuccess()) {
            return createMap(
                    mapResult.result().orElseThrow()
                            .filter(entry -> entry.first() != null)
                            .map(entry -> {
                                final U second = entry.second();
                                return Pair.of(
                                        convertTo(sourceOps, entry.first()),
                                        second != null ? convertTo(sourceOps, second) : empty()
                                );
                            })
            );
        }

        // Fallback: return JSON null for unknown types
        return empty();
    }

    /**
     * Returns a string representation of this {@code DynamicOps} instance.
     *
     * <p>This method returns a fixed string identifying the implementation, useful for
     * debugging, logging, and error messages.</p>
     *
     * @return the string {@code "GsonOps"}; never {@code null}
     */
    @Override
    public String toString() {
        return "GsonOps";
    }
}
