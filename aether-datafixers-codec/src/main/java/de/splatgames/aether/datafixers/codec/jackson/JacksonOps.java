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

package de.splatgames.aether.datafixers.codec.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Backwards-compatibility wrapper for {@link JacksonJsonOps}.
 *
 * <p>This class provides API compatibility for code written against the pre-0.4.0 package structure.
 * It delegates all operations to the new {@link JacksonJsonOps} implementation in the reorganized
 * package hierarchy.</p>
 *
 * <h2>Migration Guide</h2>
 * <p>To migrate to the new API, update your imports and class references:</p>
 * <pre>{@code
 * // Old import (deprecated)
 * import de.splatgames.aether.datafixers.codec.jackson.JacksonOps;
 *
 * // New import (recommended)
 * import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
 *
 * // Old usage (deprecated)
 * JacksonOps ops = JacksonOps.INSTANCE;
 * JacksonOps customOps = new JacksonOps(customMapper);
 *
 * // New usage (recommended)
 * JacksonJsonOps ops = JacksonJsonOps.INSTANCE;
 * JacksonJsonOps customOps = new JacksonJsonOps(customMapper);
 * }</pre>
 *
 * <h2>Removal Timeline</h2>
 * <p>This class is scheduled for removal in version 1.0.0. All functionality remains
 * fully operational until removal, but users should migrate to the new package structure
 * and class name at their earliest convenience.</p>
 *
 * <h2>Delegation Pattern</h2>
 * <p>This wrapper implements the delegation pattern, forwarding all method calls to the
 * underlying {@link JacksonJsonOps} instance. This ensures identical behavior between
 * the deprecated and new implementations.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The singleton {@link #INSTANCE} can be safely shared
 * across multiple threads, as the underlying implementation is also thread-safe.
 * Custom instances created with a custom {@link ObjectMapper} are thread-safe if
 * the provided mapper is thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see JacksonJsonOps
 * @see DynamicOps
 * @since 0.1.0
 * @deprecated Since 0.4.0. Use {@link JacksonJsonOps} from the {@code codec.json.jackson}
 *             package instead. This class will be removed in version 1.0.0 as part of
 *             the package reorganization.
 */
@Deprecated(forRemoval = true, since = "0.4.0")
public class JacksonOps implements DynamicOps<JsonNode> {

    /**
     * The singleton instance of the deprecated {@code JacksonOps} wrapper.
     *
     * <p>This instance wraps {@link JacksonJsonOps#INSTANCE} and provides full backwards
     * compatibility. It uses a default {@link ObjectMapper} with standard configuration.
     * The instance is thread-safe and can be shared across the entire application.</p>
     *
     * <h3>Migration</h3>
     * <p>Replace usages with:</p>
     * <pre>{@code
     * // Old usage (deprecated)
     * JacksonOps.INSTANCE
     *
     * // New usage (recommended)
     * JacksonJsonOps.INSTANCE
     * }</pre>
     *
     * @deprecated Use {@link JacksonJsonOps#INSTANCE} instead.
     */
    @Deprecated(forRemoval = true, since = "0.4.0")
    public static final JacksonOps INSTANCE = new JacksonOps(JacksonJsonOps.INSTANCE);

    /**
     * The underlying {@link JacksonJsonOps} instance to which all operations are delegated.
     *
     * <p>This field holds the actual implementation that performs all DynamicOps operations.
     * The wrapper simply forwards all method calls to this instance, ensuring behavioral
     * equivalence between the deprecated and new implementations.</p>
     */
    private final JacksonJsonOps baseOps;

    /**
     * Creates a new deprecated {@code JacksonOps} wrapper delegating to the specified base implementation.
     *
     * <p>This constructor allows wrapping any {@link JacksonJsonOps} instance, enabling use
     * of custom configurations while maintaining backwards compatibility.</p>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Typically use the singleton instead
     * JacksonOps ops = JacksonOps.INSTANCE;
     *
     * // Or wrap a custom JacksonJsonOps instance
     * JacksonJsonOps customJsonOps = new JacksonJsonOps(customMapper);
     * JacksonOps customOps = new JacksonOps(customJsonOps);
     * }</pre>
     *
     * @param baseOps the base {@link JacksonJsonOps} instance to delegate all operations to;
     *                must not be {@code null}
     * @deprecated Use {@link JacksonJsonOps} directly instead.
     */
    @Deprecated(forRemoval = true, since = "0.4.0")
    private JacksonOps(final JacksonJsonOps baseOps) {
        this.baseOps = baseOps;
    }

    /**
     * Creates a new deprecated {@code JacksonOps} with the specified {@link ObjectMapper}.
     *
     * <p>This constructor provides backwards compatibility for code that creates custom
     * {@code JacksonOps} instances with a specific mapper configuration.</p>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Old usage (deprecated)
     * ObjectMapper customMapper = new ObjectMapper()
     *     .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
     * JacksonOps customOps = new JacksonOps(customMapper);
     *
     * // New usage (recommended)
     * JacksonJsonOps customOps = new JacksonJsonOps(customMapper);
     * }</pre>
     *
     * @param mapper the {@link ObjectMapper} to use for JSON operations; must not be {@code null}
     * @deprecated Use {@link JacksonJsonOps#JacksonJsonOps(ObjectMapper)} instead.
     */
    @Deprecated(forRemoval = true, since = "0.4.0")
    public JacksonOps(@NotNull final ObjectMapper mapper) {
        this.baseOps = new JacksonJsonOps(mapper);
    }

    /**
     * Returns the {@link ObjectMapper} used by this instance.
     *
     * <p>This method provides access to the underlying Jackson mapper, which can be
     * useful for advanced configuration or direct JSON serialization/deserialization.</p>
     *
     * <h3>Migration</h3>
     * <pre>{@code
     * // Old usage (deprecated)
     * ObjectMapper mapper = jacksonOps.mapper();
     *
     * // New usage (recommended)
     * ObjectMapper mapper = jacksonJsonOps.mapper();
     * }</pre>
     *
     * @return the {@link ObjectMapper} used by the underlying {@link JacksonJsonOps} instance
     * @deprecated Use {@link JacksonJsonOps#mapper()} instead.
     */
    @Deprecated(forRemoval = true, since = "0.4.0")
    public ObjectMapper mapper() {
        return this.baseOps.mapper();
    }

    // ==================== Empty/Null Values ====================

    /**
     * {@inheritDoc}
     *
     * <p>Returns the canonical empty/null representation for Jackson JSON data,
     * which is {@link NullNode#getInstance()}. This delegates to the underlying
     * {@link JacksonJsonOps#empty()} method.</p>
     *
     * @return {@link NullNode#getInstance()} representing the absence of a value
     */
    @NotNull
    @Override
    public JsonNode empty() {
        return this.baseOps.empty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an empty JSON object ({@code {}}). This delegates to the underlying
     * {@link JacksonJsonOps#emptyMap()} method.</p>
     *
     * @return a new empty {@link ObjectNode} instance
     */
    @NotNull
    @Override
    public JsonNode emptyMap() {
        return this.baseOps.emptyMap();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns an empty JSON array ({@code []}). This delegates to the underlying
     * {@link JacksonJsonOps#emptyList()} method.</p>
     *
     * @return a new empty {@link ArrayNode} instance
     */
    @NotNull
    @Override
    public JsonNode emptyList() {
        return this.baseOps.emptyList();
    }

    // ==================== Type Checking ====================

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON node is a map/object structure.
     * This delegates to the underlying {@link JacksonJsonOps#isMap(JsonNode)} method.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ObjectNode}, {@code false} otherwise
     */
    @Override
    public boolean isMap(@NotNull final JsonNode value) {
        return this.baseOps.isMap(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON node is a list/array structure.
     * This delegates to the underlying {@link JacksonJsonOps#isList(JsonNode)} method.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ArrayNode}, {@code false} otherwise
     */
    @Override
    public boolean isList(@NotNull final JsonNode value) {
        return this.baseOps.isList(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON node is a text/string node.
     * This delegates to the underlying {@link JacksonJsonOps#isString(JsonNode)} method.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is a text node, {@code false} otherwise
     */
    @Override
    public boolean isString(@NotNull final JsonNode value) {
        return this.baseOps.isString(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON node is a numeric node.
     * This delegates to the underlying {@link JacksonJsonOps#isNumber(JsonNode)} method.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is a numeric node, {@code false} otherwise
     */
    @Override
    public boolean isNumber(@NotNull final JsonNode value) {
        return this.baseOps.isNumber(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether the given JSON node is a boolean node.
     * This delegates to the underlying {@link JacksonJsonOps#isBoolean(JsonNode)} method.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is a boolean node, {@code false} otherwise
     */
    @Override
    public boolean isBoolean(@NotNull final JsonNode value) {
        return this.baseOps.isBoolean(value);
    }

    // ==================== Primitive Creation ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON text node from the given string value.
     * This delegates to the underlying {@link JacksonJsonOps#createString(String)} method.</p>
     *
     * @param value the string value to wrap; must not be {@code null}
     * @return a new text node containing the string
     */
    @NotNull
    @Override
    public JsonNode createString(@NotNull final String value) {
        return this.baseOps.createString(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric node from the given integer value.
     * This delegates to the underlying {@link JacksonJsonOps#createInt(int)} method.</p>
     *
     * @param value the integer value to wrap
     * @return a new int node containing the integer
     */
    @NotNull
    @Override
    public JsonNode createInt(final int value) {
        return this.baseOps.createInt(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric node from the given long value.
     * This delegates to the underlying {@link JacksonJsonOps#createLong(long)} method.</p>
     *
     * @param value the long value to wrap
     * @return a new long node containing the long
     */
    @NotNull
    @Override
    public JsonNode createLong(final long value) {
        return this.baseOps.createLong(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric node from the given float value.
     * This delegates to the underlying {@link JacksonJsonOps#createFloat(float)} method.</p>
     *
     * @param value the float value to wrap
     * @return a new float node containing the float
     */
    @NotNull
    @Override
    public JsonNode createFloat(final float value) {
        return this.baseOps.createFloat(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric node from the given double value.
     * This delegates to the underlying {@link JacksonJsonOps#createDouble(double)} method.</p>
     *
     * @param value the double value to wrap
     * @return a new double node containing the double
     */
    @NotNull
    @Override
    public JsonNode createDouble(final double value) {
        return this.baseOps.createDouble(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric node from the given byte value.
     * Since JSON has no distinct byte type, the value is stored as a short node.
     * This delegates to the underlying {@link JacksonJsonOps#createByte(byte)} method.</p>
     *
     * @param value the byte value to wrap
     * @return a new short node containing the byte value
     */
    @NotNull
    @Override
    public JsonNode createByte(final byte value) {
        return this.baseOps.createByte(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric node from the given short value.
     * This delegates to the underlying {@link JacksonJsonOps#createShort(short)} method.</p>
     *
     * @param value the short value to wrap
     * @return a new short node containing the short
     */
    @NotNull
    @Override
    public JsonNode createShort(final short value) {
        return this.baseOps.createShort(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON boolean node from the given boolean value.
     * This delegates to the underlying {@link JacksonJsonOps#createBoolean(boolean)} method.</p>
     *
     * @param value the boolean value to wrap
     * @return a new boolean node containing the boolean
     */
    @NotNull
    @Override
    public JsonNode createBoolean(final boolean value) {
        return this.baseOps.createBoolean(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON numeric node from the given {@link Number} value.
     * The specific numeric type is preserved in the underlying JSON representation.
     * This delegates to the underlying {@link JacksonJsonOps#createNumeric(Number)} method.</p>
     *
     * @param value the number value to wrap; must not be {@code null}
     * @return a new numeric node containing the number
     */
    @NotNull
    @Override
    public JsonNode createNumeric(@NotNull final Number value) {
        return this.baseOps.createNumeric(value);
    }

    // ==================== Primitive Reading ====================

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the string value from a JSON node. The node must be a text node.
     * This delegates to the underlying {@link JacksonJsonOps#getStringValue(JsonNode)} method.</p>
     *
     * @param input the JSON node to extract the string from; must not be {@code null}
     * @return a {@link DataResult} containing the string value on success,
     *         or an error if the node is not a text node
     */
    @NotNull
    @Override
    public DataResult<String> getStringValue(@NotNull final JsonNode input) {
        return this.baseOps.getStringValue(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the numeric value from a JSON node. The node must be a numeric node.
     * This delegates to the underlying {@link JacksonJsonOps#getNumberValue(JsonNode)} method.</p>
     *
     * @param input the JSON node to extract the number from; must not be {@code null}
     * @return a {@link DataResult} containing the {@link Number} value on success,
     *         or an error if the node is not a numeric node
     */
    @NotNull
    @Override
    public DataResult<Number> getNumberValue(@NotNull final JsonNode input) {
        return this.baseOps.getNumberValue(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the boolean value from a JSON node. The node must be a boolean node.
     * This delegates to the underlying {@link JacksonJsonOps#getBooleanValue(JsonNode)} method.</p>
     *
     * @param input the JSON node to extract the boolean from; must not be {@code null}
     * @return a {@link DataResult} containing the boolean value on success,
     *         or an error if the node is not a boolean node
     */
    @NotNull
    @Override
    public DataResult<Boolean> getBooleanValue(@NotNull final JsonNode input) {
        return this.baseOps.getBooleanValue(input);
    }

    // ==================== List Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON array node from a stream of JSON nodes.
     * This delegates to the underlying {@link JacksonJsonOps#createList(Stream)} method.</p>
     *
     * @param values the stream of JSON nodes to include in the array; must not be {@code null}
     * @return a new {@link ArrayNode} containing all elements from the stream
     */
    @NotNull
    @Override
    public JsonNode createList(@NotNull final Stream<JsonNode> values) {
        return this.baseOps.createList(values);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the elements of a JSON array as a stream. The input must be an
     * {@link ArrayNode}. This delegates to the underlying
     * {@link JacksonJsonOps#getList(JsonNode)} method.</p>
     *
     * @param input the JSON node to extract list elements from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of the array elements on success,
     *         or an error if the input is not an array node
     */
    @NotNull
    @Override
    public DataResult<Stream<JsonNode>> getList(@NotNull final JsonNode input) {
        return this.baseOps.getList(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON array by appending a value to an existing array.
     * The original array is not modified; a deep copy is created.
     * This delegates to the underlying
     * {@link JacksonJsonOps#mergeToList(JsonNode, JsonNode)} method.</p>
     *
     * @param list  the existing JSON array to append to; must not be {@code null}
     * @param value the JSON node to append; must not be {@code null}
     * @return a {@link DataResult} containing the new array with the appended value on success,
     *         or an error if the list is not an array node
     */
    @NotNull
    @Override
    public DataResult<JsonNode> mergeToList(@NotNull final JsonNode list, @NotNull final JsonNode value) {
        return this.baseOps.mergeToList(list, value);
    }

    // ==================== Map Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves the value associated with a key from a JSON object.
     * This delegates to the underlying {@link JacksonJsonOps#get(JsonNode, String)} method.</p>
     *
     * @param value the JSON object to retrieve from; must not be {@code null}
     * @param key   the key to look up; must not be {@code null}
     * @return the JSON node associated with the key, or {@code null} if not present
     *         or if the input is not an object node
     */
    @Override
    public @Nullable JsonNode get(@NotNull final JsonNode value, @NotNull final String key) {
        return this.baseOps.get(value, key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object with a field set to the specified value.
     * If the input is an object node, a deep copy is created with the field updated.
     * If the input is not an object node, a new object is created containing only the specified field.
     * This delegates to the underlying
     * {@link JacksonJsonOps#set(JsonNode, String, JsonNode)} method.</p>
     *
     * @param value    the JSON node to modify; must not be {@code null}
     * @param key      the key for the field to set; must not be {@code null}
     * @param newValue the value to associate with the key; must not be {@code null}
     * @return a new {@link ObjectNode} with the field set to the specified value
     */
    @NotNull
    @Override
    public JsonNode set(@NotNull final JsonNode value, @NotNull final String key, @NotNull final JsonNode newValue) {
        return this.baseOps.set(value, key, newValue);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object with a field removed.
     * A deep copy of the input object is created without the specified field.
     * This delegates to the underlying
     * {@link JacksonJsonOps#remove(JsonNode, String)} method.</p>
     *
     * @param value the JSON object to modify; must not be {@code null}
     * @param key   the key of the field to remove; must not be {@code null}
     * @return a new {@link ObjectNode} without the specified field
     */
    @NotNull
    @Override
    public JsonNode remove(@NotNull final JsonNode value, @NotNull final String key) {
        return this.baseOps.remove(value, key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether a JSON object contains a field with the specified key.
     * This delegates to the underlying
     * {@link JacksonJsonOps#has(JsonNode, String)} method.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @param key   the key to look for; must not be {@code null}
     * @return {@code true} if the value is an {@link ObjectNode} and contains the specified key,
     *         {@code false} otherwise
     */
    @Override
    public boolean has(@NotNull final JsonNode value, @NotNull final String key) {
        return this.baseOps.has(value, key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a JSON object from a stream of key-value pairs.
     * Keys must be text nodes; non-text keys are skipped.
     * This delegates to the underlying {@link JacksonJsonOps#createMap(Stream)} method.</p>
     *
     * @param entries the stream of key-value pairs; must not be {@code null}
     * @return a new {@link ObjectNode} containing all valid entries from the stream
     */
    @NotNull
    @Override
    public JsonNode createMap(@NotNull final Stream<Pair<JsonNode, JsonNode>> entries) {
        return this.baseOps.createMap(entries);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the entries of a JSON object as a stream of key-value pairs.
     * This delegates to the underlying
     * {@link JacksonJsonOps#getMapEntries(JsonNode)} method.</p>
     *
     * @param input the JSON node to extract entries from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of key-value pairs on success,
     *         or an error if the input is not an object node
     */
    @NotNull
    @Override
    public DataResult<Stream<Pair<JsonNode, JsonNode>>> getMapEntries(@NotNull final JsonNode input) {
        return this.baseOps.getMapEntries(input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object by adding a key-value pair to an existing map.
     * A deep copy of the input map is created with the new entry added.
     * This delegates to the underlying
     * {@link JacksonJsonOps#mergeToMap(JsonNode, JsonNode, JsonNode)} method.</p>
     *
     * @param map   the existing JSON object; must not be {@code null}
     * @param key   the key for the new entry (must be a text node); must not be {@code null}
     * @param value the value for the new entry; must not be {@code null}
     * @return a {@link DataResult} containing the new object with the added entry on success,
     *         or an error if the map is not an object node or the key is not a text node
     */
    @NotNull
    @Override
    public DataResult<JsonNode> mergeToMap(@NotNull final JsonNode map, @NotNull final JsonNode key, @NotNull final JsonNode value) {
        return this.baseOps.mergeToMap(map, key, value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object by merging two maps together.
     * A deep copy of the first map is created, and all entries from the second map are added.
     * Entries in the second map override entries with the same key in the first map.
     * This delegates to the underlying
     * {@link JacksonJsonOps#mergeToMap(JsonNode, JsonNode)} method.</p>
     *
     * @param map   the base JSON object; must not be {@code null}
     * @param other the JSON object to merge from; must not be {@code null}
     * @return a {@link DataResult} containing the merged object on success,
     *         or an error if either argument is not an object node
     */
    @NotNull
    @Override
    public DataResult<JsonNode> mergeToMap(@NotNull final JsonNode map, @NotNull final JsonNode other) {
        return this.baseOps.mergeToMap(map, other);
    }

    // ==================== Conversion ====================

    /**
     * {@inheritDoc}
     *
     * <p>Converts data from another {@link DynamicOps} format to Jackson's {@link JsonNode}.
     * Recursively converts primitives, lists, and maps to their Jackson equivalents.
     * This delegates to the underlying
     * {@link JacksonJsonOps#convertTo(DynamicOps, Object)} method.</p>
     *
     * @param <U>   the type parameter of the target format
     * @param ops   the target {@link DynamicOps} implementation; must not be {@code null}
     * @param input the data to convert in the source format; must not be {@code null}
     * @return the converted data as a Jackson {@link JsonNode}
     */
    @NotNull
    @Override
    public <U> JsonNode convertTo(@NotNull final DynamicOps<U> ops, @NotNull final U input) {
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
        return "JacksonOps (deprecated, use JacksonJsonOps)";
    }
}
