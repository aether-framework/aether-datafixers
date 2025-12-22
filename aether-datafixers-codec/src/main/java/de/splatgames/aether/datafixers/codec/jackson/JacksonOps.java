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
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ShortNode;
import com.fasterxml.jackson.databind.node.TextNode;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link DynamicOps} implementation for Jackson Databind's {@link JsonNode}.
 *
 * <p>This class provides format-agnostic data manipulation capabilities for
 * Jackson's JSON tree model, enabling the data fixing system to read, write, and transform JSON data without coupling
 * to Jackson-specific APIs.</p>
 *
 * <h2>Usage</h2>
 * <p>Use the singleton {@link #INSTANCE} for default configuration, or create
 * a custom instance with a specific {@link ObjectMapper}:</p>
 * <pre>{@code
 * // Using the default instance
 * DataResult<JsonNode> result = codec.encodeStart(JacksonOps.INSTANCE, value);
 *
 * // Using a custom ObjectMapper
 * ObjectMapper customMapper = new ObjectMapper()
 *     .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
 * JacksonOps customOps = new JacksonOps(customMapper);
 *
 * // Create a Dynamic wrapper for Jackson data
 * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonOps.INSTANCE, jsonNode);
 *
 * // Convert from another format to Jackson
 * JsonNode converted = JacksonOps.INSTANCE.convertTo(otherOps, otherData);
 * }</pre>
 *
 * <h2>Null Handling</h2>
 * <p>JSON null values are represented as {@link NullNode}. The {@link #empty()}
 * method returns a null node for representing "no value".</p>
 *
 * <h2>Type Mapping</h2>
 * <ul>
 *   <li>Booleans → {@link BooleanNode}</li>
 *   <li>Integers → {@link IntNode}</li>
 *   <li>Longs → {@link LongNode}</li>
 *   <li>Floats → {@link FloatNode}</li>
 *   <li>Doubles → {@link DoubleNode}</li>
 *   <li>Shorts/Bytes → {@link ShortNode}</li>
 *   <li>Strings → {@link TextNode}</li>
 *   <li>Lists → {@link ArrayNode}</li>
 *   <li>Maps → {@link ObjectNode} (keys must be strings)</li>
 *   <li>Empty/null → {@link NullNode}</li>
 * </ul>
 *
 * <h2>Immutability</h2>
 * <p>All modification operations (e.g., {@link #set}, {@link #remove}, {@link #mergeToMap})
 * create deep copies of the input, preserving immutability of the original data.</p>
 *
 * <h2>ObjectMapper Configuration</h2>
 * <p>The {@link ObjectMapper} provided to the constructor determines the
 * {@link JsonNodeFactory} used for creating nodes. This allows customization
 * of node creation behavior (e.g., decimal precision).</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Instances of this class are thread-safe if the underlying {@link ObjectMapper}
 * is thread-safe (which is the case for read operations after configuration).</p>
 *
 * @author Erik Pförtner
 * @see DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see JsonNode
 * @see ObjectMapper
 * @since 0.1.0
 */
public final class JacksonOps implements DynamicOps<JsonNode> {

    /**
     * The singleton instance with a default {@link ObjectMapper}.
     *
     * <p>This instance uses a standard {@code ObjectMapper} with default settings.
     * For custom configuration, create a new instance using {@link #JacksonOps(ObjectMapper)}.</p>
     */
    public static final JacksonOps INSTANCE = new JacksonOps(new ObjectMapper());

    private final ObjectMapper mapper;
    private final JsonNodeFactory nodeFactory;

    /**
     * Creates a new {@code JacksonOps} with the specified {@link ObjectMapper}.
     *
     * <p>The mapper's {@link JsonNodeFactory} will be used for creating all JSON nodes.
     * This allows customization of node creation behavior.</p>
     *
     * @param mapper the object mapper to use, must not be {@code null}
     */
    public JacksonOps(@NotNull final ObjectMapper mapper) {
        this.mapper = mapper;
        this.nodeFactory = mapper.getNodeFactory();
    }

    /**
     * Returns the {@link ObjectMapper} used by this instance.
     *
     * <p>This can be used for additional Jackson operations outside the
     * {@code DynamicOps} interface, such as serialization to/from strings.</p>
     *
     * @return the object mapper, never {@code null}
     */
    public ObjectMapper mapper() {
        return this.mapper;
    }

    // ==================== Empty/Null ====================

    /**
     * {@inheritDoc}
     *
     * @return {@link NullNode#getInstance()}
     */
    @NotNull
    @Override
    public JsonNode empty() {
        return NullNode.getInstance();
    }

    // ==================== Type Checks ====================

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is an {@link ObjectNode}
     */
    @Override
    public boolean isMap(@NotNull final JsonNode value) {
        return value.isObject();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is an {@link ArrayNode}
     */
    @Override
    public boolean isList(@NotNull final JsonNode value) {
        return value.isArray();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is a {@link TextNode}
     */
    @Override
    public boolean isString(@NotNull final JsonNode value) {
        return value.isTextual();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is a numeric node
     */
    @Override
    public boolean isNumber(@NotNull final JsonNode value) {
        return value.isNumber();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if the value is a {@link BooleanNode}
     */
    @Override
    public boolean isBoolean(@NotNull final JsonNode value) {
        return value.isBoolean();
    }

    // ==================== Primitive Creation ====================

    /**
     * {@inheritDoc}
     *
     * @return a {@link TextNode} containing the string
     */
    @NotNull
    @Override
    public JsonNode createString(@NotNull final String value) {
        return TextNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return an {@link IntNode} containing the integer
     */
    @NotNull
    @Override
    public JsonNode createInt(final int value) {
        return IntNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link LongNode} containing the long
     */
    @NotNull
    @Override
    public JsonNode createLong(final long value) {
        return LongNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link FloatNode} containing the float
     */
    @NotNull
    @Override
    public JsonNode createFloat(final float value) {
        return FloatNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link DoubleNode} containing the double
     */
    @NotNull
    @Override
    public JsonNode createDouble(final double value) {
        return DoubleNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link ShortNode} containing the byte value
     */
    @NotNull
    @Override
    public JsonNode createByte(final byte value) {
        return ShortNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link ShortNode} containing the short
     */
    @NotNull
    @Override
    public JsonNode createShort(final short value) {
        return ShortNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link BooleanNode} containing the boolean
     */
    @NotNull
    @Override
    public JsonNode createBoolean(final boolean value) {
        return BooleanNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates the appropriate node type based on the number's runtime type.</p>
     */
    @NotNull
    @Override
    public JsonNode createNumeric(@NotNull final Number value) {
        if (value instanceof Integer) {
            return IntNode.valueOf(value.intValue());
        }
        if (value instanceof Long) {
            return LongNode.valueOf(value.longValue());
        }
        if (value instanceof Float) {
            return FloatNode.valueOf(value.floatValue());
        }
        if (value instanceof Double) {
            return DoubleNode.valueOf(value.doubleValue());
        }
        if (value instanceof Short) {
            return ShortNode.valueOf(value.shortValue());
        }
        if (value instanceof Byte) {
            return ShortNode.valueOf(value.byteValue());
        }
        return DoubleNode.valueOf(value.doubleValue());
    }

    // ==================== Primitive Reading ====================

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the string value from a {@link TextNode}.</p>
     */
    @NotNull
    @Override
    public DataResult<String> getStringValue(@NotNull final JsonNode input) {
        if (!input.isTextual()) {
            return DataResult.error("Not a string: " + input);
        }
        return DataResult.success(input.asText());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the number value from a numeric node.</p>
     */
    @NotNull
    @Override
    public DataResult<Number> getNumberValue(@NotNull final JsonNode input) {
        if (!input.isNumber()) {
            return DataResult.error("Not a number: " + input);
        }
        return DataResult.success(input.numberValue());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the boolean value from a {@link BooleanNode}.</p>
     */
    @NotNull
    @Override
    public DataResult<Boolean> getBooleanValue(@NotNull final JsonNode input) {
        if (!input.isBoolean()) {
            return DataResult.error("Not a boolean: " + input);
        }
        return DataResult.success(input.asBoolean());
    }

    // ==================== List Operations ====================

    /**
     * {@inheritDoc}
     *
     * @return an empty {@link ArrayNode}
     */
    @NotNull
    @Override
    public JsonNode emptyList() {
        return this.nodeFactory.arrayNode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates an {@link ArrayNode} containing all elements from the stream.</p>
     */
    @NotNull
    @Override
    public JsonNode createList(@NotNull final Stream<JsonNode> values) {
        final ArrayNode array = this.nodeFactory.arrayNode();
        values.forEach(array::add);
        return array;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the elements of an {@link ArrayNode} as a stream.</p>
     */
    @NotNull
    @Override
    public DataResult<Stream<JsonNode>> getList(@NotNull final JsonNode input) {
        if (!input.isArray()) {
            return DataResult.error("Not an array: " + input);
        }
        return DataResult.success(
                StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(input.elements(), Spliterator.ORDERED),
                        false
                )
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the list and appends the value.</p>
     */
    @NotNull
    @Override
    public DataResult<JsonNode> mergeToList(@NotNull final JsonNode list,
                                            @NotNull final JsonNode value) {
        if (!list.isArray() && !list.isNull()) {
            return DataResult.error("Not an array: " + list);
        }
        final ArrayNode result = list.isNull() ? this.nodeFactory.arrayNode() : ((ArrayNode) list).deepCopy();
        result.add(value);
        return DataResult.success(result);
    }

    // ==================== Map Operations ====================

    /**
     * {@inheritDoc}
     *
     * @return an empty {@link ObjectNode}
     */
    @NotNull
    @Override
    public JsonNode emptyMap() {
        return this.nodeFactory.objectNode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates an {@link ObjectNode} from the stream of key-value pairs.
     * Keys must be text nodes; null keys are skipped.</p>
     */
    @NotNull
    @Override
    public JsonNode createMap(@NotNull final Stream<Pair<JsonNode, JsonNode>> entries) {
        final ObjectNode object = this.nodeFactory.objectNode();
        entries.forEach(pair -> {
            final JsonNode keyNode = pair.first();
            final JsonNode valueNode = pair.second();
            if (keyNode == null) {
                return; // Skip entries with null keys
            }
            final String key = keyNode.asText();
            object.set(key, valueNode != null ? valueNode : NullNode.getInstance());
        });
        return object;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the entries of an {@link ObjectNode} as a stream of key-value pairs.</p>
     */
    @NotNull
    @Override
    public DataResult<Stream<Pair<JsonNode, JsonNode>>> getMapEntries(@NotNull final JsonNode input) {
        if (!input.isObject()) {
            return DataResult.error("Not an object: " + input);
        }
        final Iterator<Map.Entry<String, JsonNode>> fields = input.fields();
        return DataResult.success(
                StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(fields, Spliterator.ORDERED),
                        false
                ).map(entry -> Pair.of(TextNode.valueOf(entry.getKey()), entry.getValue()))
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the map and adds the key-value pair.</p>
     */
    @NotNull
    @Override
    public DataResult<JsonNode> mergeToMap(@NotNull final JsonNode map,
                                           @NotNull final JsonNode key,
                                           @NotNull final JsonNode value) {
        if (!map.isObject() && !map.isNull()) {
            return DataResult.error("Not an object: " + map);
        }
        if (!key.isTextual()) {
            return DataResult.error("Key is not a string: " + key);
        }
        final ObjectNode result = map.isNull() ? this.nodeFactory.objectNode() : ((ObjectNode) map).deepCopy();
        result.set(key.asText(), value);
        return DataResult.success(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the first map and merges all entries from the second.</p>
     */
    @NotNull
    @Override
    public DataResult<JsonNode> mergeToMap(@NotNull final JsonNode map,
                                           @NotNull final JsonNode other) {
        if (!map.isObject() && !map.isNull()) {
            return DataResult.error("First argument is not an object: " + map);
        }
        if (!other.isObject() && !other.isNull()) {
            return DataResult.error("Second argument is not an object: " + other);
        }
        final ObjectNode result = map.isNull() ? this.nodeFactory.objectNode() : ((ObjectNode) map).deepCopy();
        if (!other.isNull()) {
            final Iterator<Map.Entry<String, JsonNode>> fields = other.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> entry = fields.next();
                result.set(entry.getKey(), entry.getValue());
            }
        }
        return DataResult.success(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves a field from an {@link ObjectNode} by key.</p>
     */
    @Override
    public @Nullable JsonNode get(@NotNull final JsonNode input,
                                  @NotNull final String key) {
        if (!input.isObject()) {
            return null;
        }
        if (!input.has(key)) {
            return null;
        }
        return input.get(key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the object and sets the field.</p>
     */
    @NotNull
    @Override
    public JsonNode set(@NotNull final JsonNode input,
                        @NotNull final String key,
                        @NotNull final JsonNode newValue) {
        if (!input.isObject()) {
            final ObjectNode result = nodeFactory.objectNode();
            result.set(key, newValue);
            return result;
        }
        final ObjectNode result = input.deepCopy();
        result.set(key, newValue);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a deep copy of the object and removes the field.</p>
     */
    @NotNull
    @Override
    public JsonNode remove(@NotNull final JsonNode input,
                           @NotNull final String key) {
        if (!input.isObject()) {
            return input;
        }
        final ObjectNode result = input.deepCopy();
        result.remove(key);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks if an {@link ObjectNode} contains the specified key.</p>
     */
    @Override
    public boolean has(@NotNull final JsonNode input,
                       @NotNull final String key) {
        if (!input.isObject()) {
            return false;
        }
        return input.has(key);
    }

    // ==================== Conversion ====================

    /**
     * {@inheritDoc}
     *
     * <p>Converts data from another {@link DynamicOps} format to Jackson {@link JsonNode}.
     * Recursively converts primitives, lists, and maps.</p>
     */
    @NotNull
    @Override
    public <U> JsonNode convertTo(@NotNull final DynamicOps<U> sourceOps,
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
     * @return {@code "JacksonOps"}
     */
    @Override
    public String toString() {
        return "JacksonOps";
    }
}
