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

package de.splatgames.aether.datafixers.codec.json.jackson;

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
import com.google.common.base.Preconditions;
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
 * A {@link DynamicOps} implementation for Jackson Databind's {@link JsonNode} type hierarchy.
 *
 * <p>This class provides format-agnostic data manipulation capabilities for Jackson's JSON
 * tree model, enabling the Aether Datafixers system to read, write, and transform JSON data
 * without coupling application code to Jackson-specific APIs. It serves as the bridge between
 * the abstract data fixer operations and Jackson's concrete JSON model.</p>
 *
 * <h2>Design Pattern</h2>
 * <p>This class uses a hybrid Singleton/Factory pattern:</p>
 * <ul>
 *   <li>A default singleton {@link #INSTANCE} is provided for convenience with standard
 *       {@link ObjectMapper} configuration</li>
 *   <li>Custom instances can be created via {@link #JacksonJsonOps(ObjectMapper)} for
 *       specialized configurations (e.g., custom serializers, date formats, or
 *       {@link JsonNodeFactory} settings)</li>
 * </ul>
 *
 * <p>This design balances convenience with flexibility:</p>
 * <ul>
 *   <li>Most use cases can rely on the singleton for simplicity</li>
 *   <li>Advanced scenarios can customize behavior through the {@link ObjectMapper}</li>
 *   <li>The class is immutable after construction, ensuring thread safety</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Encoding/Decoding</h3>
 * <pre>{@code
 * // Encode a value to JSON using a codec
 * DataResult<JsonNode> encoded = playerCodec.encodeStart(JacksonJsonOps.INSTANCE, player);
 *
 * // Decode JSON to a typed value
 * DataResult<Player> decoded = playerCodec.decode(JacksonJsonOps.INSTANCE, jsonNode);
 * }</pre>
 *
 * <h3>Creating Dynamic Wrappers</h3>
 * <pre>{@code
 * // Wrap existing JSON data for manipulation
 * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonJsonOps.INSTANCE, existingJson);
 *
 * // Access nested fields
 * Optional<String> name = dynamic.get("player").get("name").asString();
 *
 * // Transform data
 * Dynamic<JsonNode> updated = dynamic.set("version", JacksonJsonOps.INSTANCE.createInt(2));
 * }</pre>
 *
 * <h3>Custom ObjectMapper Configuration</h3>
 * <pre>{@code
 * // Create a custom ObjectMapper with specific settings
 * ObjectMapper customMapper = new ObjectMapper()
 *     .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
 *     .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
 *     .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
 *
 * // Create JacksonJsonOps with custom configuration
 * JacksonJsonOps customOps = new JacksonJsonOps(customMapper);
 *
 * // Use for encoding/decoding
 * DataResult<JsonNode> result = codec.encodeStart(customOps, value);
 * }</pre>
 *
 * <h3>Format Conversion</h3>
 * <pre>{@code
 * // Convert from Gson JsonElement to Jackson JsonNode
 * JsonNode jacksonJson = JacksonJsonOps.INSTANCE.convertTo(GsonOps.INSTANCE, gsonElement);
 *
 * // Convert from YAML to Jackson JSON
 * JsonNode fromYaml = JacksonJsonOps.INSTANCE.convertTo(SnakeYamlOps.INSTANCE, yamlData);
 * }</pre>
 *
 * <h3>Direct Mapper Access</h3>
 * <pre>{@code
 * // Access the underlying ObjectMapper for additional operations
 * ObjectMapper mapper = JacksonJsonOps.INSTANCE.mapper();
 *
 * // Serialize to JSON string
 * String jsonString = mapper.writeValueAsString(jsonNode);
 *
 * // Parse from JSON string
 * JsonNode parsed = mapper.readTree(jsonString);
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <p>The following table shows how Java/abstract types map to Jackson JSON node types:</p>
 * <table border="1" cellpadding="5">
 *   <caption>Type Mapping between Java and Jackson</caption>
 *   <tr><th>Java Type</th><th>Jackson Type</th><th>Notes</th></tr>
 *   <tr><td>{@code boolean}</td><td>{@link BooleanNode}</td><td>Created via {@link BooleanNode#valueOf(boolean)}</td></tr>
 *   <tr><td>{@code int}</td><td>{@link IntNode}</td><td>Created via {@link IntNode#valueOf(int)}</td></tr>
 *   <tr><td>{@code long}</td><td>{@link LongNode}</td><td>Created via {@link LongNode#valueOf(long)}</td></tr>
 *   <tr><td>{@code float}</td><td>{@link FloatNode}</td><td>Created via {@link FloatNode#valueOf(float)}</td></tr>
 *   <tr><td>{@code double}</td><td>{@link DoubleNode}</td><td>Created via {@link DoubleNode#valueOf(double)}</td></tr>
 *   <tr><td>{@code byte, short}</td><td>{@link ShortNode}</td><td>Both stored as short; created via {@link ShortNode#valueOf(short)}</td></tr>
 *   <tr><td>{@code String}</td><td>{@link TextNode}</td><td>Created via {@link TextNode#valueOf(String)}</td></tr>
 *   <tr><td>{@code List/Stream}</td><td>{@link ArrayNode}</td><td>Preserves element order; created via {@link JsonNodeFactory#arrayNode()}</td></tr>
 *   <tr><td>{@code Map}</td><td>{@link ObjectNode}</td><td>Keys must be strings; created via {@link JsonNodeFactory#objectNode()}</td></tr>
 *   <tr><td>{@code null/empty}</td><td>{@link NullNode}</td><td>Singleton {@link NullNode#getInstance()}</td></tr>
 * </table>
 *
 * <h2>Immutability Contract</h2>
 * <p>All modification operations in this class preserve immutability of input data:</p>
 * <ul>
 *   <li>{@link #set(JsonNode, String, JsonNode)} creates a deep copy via
 *       {@link JsonNode#deepCopy()} before modification</li>
 *   <li>{@link #remove(JsonNode, String)} creates a deep copy before removal</li>
 *   <li>{@link #mergeToMap(JsonNode, JsonNode, JsonNode)} creates a deep copy via
 *       {@link ObjectNode#deepCopy()} of the target map</li>
 *   <li>{@link #mergeToMap(JsonNode, JsonNode)} creates a deep copy of the first map</li>
 *   <li>{@link #mergeToList(JsonNode, JsonNode)} creates a deep copy via
 *       {@link ArrayNode#deepCopy()} of the target list</li>
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
 * <p>Thread safety of this class depends on the underlying {@link ObjectMapper}:</p>
 * <ul>
 *   <li>The {@link #INSTANCE} singleton uses a default {@link ObjectMapper} which is thread-safe
 *       for read operations after construction</li>
 *   <li>Custom instances are thread-safe if the provided {@link ObjectMapper} is not modified
 *       after construction of the {@code JacksonJsonOps} instance</li>
 *   <li>All operations in this class are stateless beyond the {@link ObjectMapper} and
 *       {@link JsonNodeFactory} references</li>
 *   <li>The {@link JsonNodeFactory} obtained from the mapper is used for all node creation,
 *       ensuring consistent behavior</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Deep copy operations ({@link JsonNode#deepCopy()}) have O(n) complexity where n is the
 *       total structure size</li>
 *   <li>Prefer batch modifications via {@link #createMap(Stream)} or {@link #createList(Stream)}
 *       over repeated {@link #set} or {@link #mergeToList} calls</li>
 *   <li>Stream-based operations are lazy and support short-circuiting where possible</li>
 *   <li>The {@link JsonNodeFactory} from the {@link ObjectMapper} is cached at construction
 *       time to avoid repeated lookups</li>
 *   <li>Singleton node types ({@link NullNode}, {@link BooleanNode}) are reused via their
 *       static factory methods</li>
 * </ul>
 *
 * <h2>Comparison with GsonOps</h2>
 * <table border="1" cellpadding="5">
 *   <caption>Feature Comparison between JacksonJsonOps and GsonOps</caption>
 *   <tr><th>Feature</th><th>JacksonJsonOps</th><th>GsonOps</th></tr>
 *   <tr><td>Singleton only</td><td>No (supports custom ObjectMapper)</td><td>Yes</td></tr>
 *   <tr><td>Numeric type preservation</td><td>High (dedicated node types)</td><td>Medium (all via JsonPrimitive)</td></tr>
 *   <tr><td>Streaming support</td><td>Via ObjectMapper</td><td>Via JsonReader/JsonWriter</td></tr>
 *   <tr><td>Configuration flexibility</td><td>High (via ObjectMapper)</td><td>Low</td></tr>
 * </table>
 *
 * @author Erik Pförtner
 * @see DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see JsonNode
 * @see ObjectMapper
 * @see JsonNodeFactory
 * @see DataResult
 * @since 0.1.0
 */
public final class JacksonJsonOps implements DynamicOps<JsonNode> {

    /**
     * The singleton instance with a default {@link ObjectMapper}.
     *
     * <p>This instance should be used for most Jackson JSON operations throughout the
     * application. It uses a standard {@link ObjectMapper} with default settings, which
     * is suitable for general-purpose JSON processing.</p>
     *
     * <p>The default {@link ObjectMapper} provides:</p>
     * <ul>
     *   <li>Standard JSON parsing and generation</li>
     *   <li>Default {@link JsonNodeFactory} for node creation</li>
     *   <li>Thread-safe read operations</li>
     * </ul>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Direct usage
     * JsonNode json = JacksonJsonOps.INSTANCE.createString("hello");
     *
     * // With Dynamic
     * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonJsonOps.INSTANCE, json);
     *
     * // With Codecs
     * DataResult<JsonNode> result = myCodec.encodeStart(JacksonJsonOps.INSTANCE, value);
     *
     * // Access the mapper for additional operations
     * String jsonString = JacksonJsonOps.INSTANCE.mapper().writeValueAsString(json);
     * }</pre>
     *
     * <p>For custom {@link ObjectMapper} configurations (e.g., custom serializers, date
     * handling, or {@link JsonNodeFactory} settings), create a new instance using
     * {@link #JacksonJsonOps(ObjectMapper)}.</p>
     */
    public static final JacksonJsonOps INSTANCE = new JacksonJsonOps(new ObjectMapper());

    /**
     * The Jackson ObjectMapper used for JSON operations.
     *
     * <p>This mapper provides:</p>
     * <ul>
     *   <li>The {@link JsonNodeFactory} for creating JSON nodes</li>
     *   <li>Configuration for JSON processing behavior</li>
     *   <li>Access to serialization/deserialization capabilities beyond the {@link DynamicOps}
     *       interface</li>
     * </ul>
     *
     * <p>The mapper is stored as a final field to ensure immutability of the
     * {@code JacksonJsonOps} instance after construction.</p>
     */
    private final ObjectMapper mapper;

    /**
     * The JsonNodeFactory used for creating JSON nodes.
     *
     * <p>This factory is obtained from the {@link ObjectMapper} at construction time and
     * cached for efficient access. It determines how nodes are created, including:</p>
     * <ul>
     *   <li>Decimal precision for numeric nodes</li>
     *   <li>String interning behavior</li>
     *   <li>Array and object node initialization</li>
     * </ul>
     *
     * <p>By caching the factory, we avoid repeated calls to
     * {@link ObjectMapper#getNodeFactory()} during node creation operations.</p>
     */
    private final JsonNodeFactory nodeFactory;

    /**
     * Creates a new {@code JacksonJsonOps} with the specified {@link ObjectMapper}.
     *
     * <p>The mapper's {@link JsonNodeFactory} will be extracted and used for creating all
     * JSON nodes. This allows full customization of Jackson's JSON processing behavior,
     * including:</p>
     * <ul>
     *   <li>Custom serializers and deserializers</li>
     *   <li>Date/time handling configuration</li>
     *   <li>Decimal precision via custom {@link JsonNodeFactory}</li>
     *   <li>Feature flags (e.g., FAIL_ON_UNKNOWN_PROPERTIES)</li>
     * </ul>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * // Create mapper with custom settings
     * ObjectMapper mapper = new ObjectMapper()
     *     .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
     *     .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
     *
     * // Create JacksonJsonOps with custom mapper
     * JacksonJsonOps ops = new JacksonJsonOps(mapper);
     * }</pre>
     *
     * <p><strong>Thread Safety:</strong> The provided {@link ObjectMapper} should not be
     * modified after passing it to this constructor to ensure thread-safe operation of
     * the resulting {@code JacksonJsonOps} instance.</p>
     *
     * @param mapper the object mapper to use for JSON operations; must not be {@code null}
     */
    public JacksonJsonOps(@NotNull final ObjectMapper mapper) {
        Preconditions.checkNotNull(mapper, "mapper must not be null");
        this.mapper = mapper;
        this.nodeFactory = mapper.getNodeFactory();
    }

    /**
     * Returns the {@link ObjectMapper} used by this instance.
     *
     * <p>This accessor provides direct access to the underlying Jackson {@link ObjectMapper},
     * enabling operations beyond the {@link DynamicOps} interface, such as:</p>
     * <ul>
     *   <li>Serialization to JSON strings: {@code mapper().writeValueAsString(node)}</li>
     *   <li>Parsing from JSON strings: {@code mapper().readTree(jsonString)}</li>
     *   <li>Reading from input streams: {@code mapper().readTree(inputStream)}</li>
     *   <li>Writing to output streams: {@code mapper().writeTree(generator, node)}</li>
     *   <li>Converting between POJOs and JsonNode: {@code mapper().valueToTree(pojo)}</li>
     * </ul>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * // Serialize JsonNode to string
     * String json = JacksonJsonOps.INSTANCE.mapper().writeValueAsString(node);
     *
     * // Parse JSON string to JsonNode
     * JsonNode parsed = JacksonJsonOps.INSTANCE.mapper().readTree(json);
     *
     * // Pretty print
     * String pretty = JacksonJsonOps.INSTANCE.mapper()
     *     .writerWithDefaultPrettyPrinter()
     *     .writeValueAsString(node);
     * }</pre>
     *
     * <p><strong>Warning:</strong> Modifying the returned {@link ObjectMapper} after
     * obtaining it may affect thread safety. The mapper should be treated as read-only
     * in multi-threaded environments.</p>
     *
     * @return the object mapper used by this instance; never {@code null}
     */
    public ObjectMapper mapper() {
        return this.mapper;
    }

    // ==================== Empty/Null Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Returns the JSON null value, which represents the absence of a value in JSON.
     * This is the canonical "empty" value for the Jackson format and is used when:</p>
     * <ul>
     *   <li>A field has no value</li>
     *   <li>A conversion cannot determine the appropriate type</li>
     *   <li>An optional value is absent</li>
     * </ul>
     *
     * <p>The returned {@link NullNode} is a singleton instance obtained via
     * {@link NullNode#getInstance()}, ensuring memory efficiency.</p>
     *
     * @return {@link NullNode#getInstance()}, the singleton JSON null value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode empty() {
        return NullNode.getInstance();
    }

    // ==================== Type Check Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON node represents a map/object structure.
     * In JSON/Jackson terminology, this corresponds to an {@link ObjectNode} which contains
     * key-value pairs where keys are always strings.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ObjectNode}, {@code false} otherwise
     *         (including for {@link NullNode}, {@link ArrayNode}, and value nodes)
     */
    @Override
    public boolean isMap(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isObject();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON node represents a list/array structure.
     * In JSON/Jackson terminology, this corresponds to an {@link ArrayNode} which contains
     * an ordered sequence of elements.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ArrayNode}, {@code false} otherwise
     *         (including for {@link NullNode}, {@link ObjectNode}, and value nodes)
     */
    @Override
    public boolean isList(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isArray();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON node represents a string value.
     * In Jackson, strings are stored as {@link TextNode} instances.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is a {@link TextNode}, {@code false} otherwise
     *         (including for numeric, boolean, and container nodes)
     */
    @Override
    public boolean isString(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isTextual();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON node represents a numeric value.
     * In Jackson, numbers can be stored as various node types:</p>
     * <ul>
     *   <li>{@link IntNode} for integer values</li>
     *   <li>{@link LongNode} for long values</li>
     *   <li>{@link FloatNode} for float values</li>
     *   <li>{@link DoubleNode} for double values</li>
     *   <li>{@link ShortNode} for short and byte values</li>
     *   <li>{@link com.fasterxml.jackson.databind.node.BigIntegerNode} for arbitrary precision integers</li>
     *   <li>{@link com.fasterxml.jackson.databind.node.DecimalNode} for arbitrary precision decimals</li>
     * </ul>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is any numeric node type, {@code false} otherwise
     *         (including for string, boolean, and container nodes)
     */
    @Override
    public boolean isNumber(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isNumber();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given JSON node represents a boolean value.
     * In Jackson, booleans are stored as {@link BooleanNode} instances, which are
     * singletons for {@code true} and {@code false} values.</p>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is a {@link BooleanNode}, {@code false} otherwise
     *         (including for string, numeric, and container nodes)
     */
    @Override
    public boolean isBoolean(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isBoolean();
    }

    // ==================== Primitive Creation Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON string node from the given string value. The resulting
     * {@link TextNode} will return {@code true} for {@link JsonNode#isTextual()}.</p>
     *
     * @param value the string value to wrap; must not be {@code null}
     * @return a new {@link TextNode} containing the string value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createString(@NotNull final String value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return TextNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON integer node from the given integer value. The resulting
     * {@link IntNode} will return {@code true} for {@link JsonNode#isInt()} and the
     * value can be retrieved via {@link JsonNode#asInt()} or {@link JsonNode#intValue()}.</p>
     *
     * @param value the integer value to wrap
     * @return a new {@link IntNode} containing the integer value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createInt(final int value) {
        return IntNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON long node from the given long value. The resulting
     * {@link LongNode} will return {@code true} for {@link JsonNode#isLong()} and the
     * value can be retrieved via {@link JsonNode#asLong()} or {@link JsonNode#longValue()}.</p>
     *
     * <p><strong>Note:</strong> JSON does not distinguish between integer and long types
     * in the specification. Very large long values may lose precision when serialized to
     * JSON text and parsed by JavaScript or other languages with limited numeric precision.</p>
     *
     * @param value the long value to wrap
     * @return a new {@link LongNode} containing the long value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createLong(final long value) {
        return LongNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON float node from the given float value. The resulting
     * {@link FloatNode} will return {@code true} for {@link JsonNode#isFloat()} and the
     * value can be retrieved via {@link JsonNode#floatValue()}.</p>
     *
     * <p><strong>Note:</strong> Special float values ({@link Float#NaN},
     * {@link Float#POSITIVE_INFINITY}, {@link Float#NEGATIVE_INFINITY}) are not valid
     * JSON and may cause issues during serialization. The behavior depends on the
     * {@link ObjectMapper} configuration.</p>
     *
     * @param value the float value to wrap
     * @return a new {@link FloatNode} containing the float value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createFloat(final float value) {
        return FloatNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON double node from the given double value. The resulting
     * {@link DoubleNode} will return {@code true} for {@link JsonNode#isDouble()} and the
     * value can be retrieved via {@link JsonNode#asDouble()} or {@link JsonNode#doubleValue()}.</p>
     *
     * <p><strong>Note:</strong> Special double values ({@link Double#NaN},
     * {@link Double#POSITIVE_INFINITY}, {@link Double#NEGATIVE_INFINITY}) are not valid
     * JSON and may cause issues during serialization. The behavior depends on the
     * {@link ObjectMapper} configuration.</p>
     *
     * @param value the double value to wrap
     * @return a new {@link DoubleNode} containing the double value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createDouble(final double value) {
        return DoubleNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON numeric node from the given byte value. The resulting
     * {@link ShortNode} will return {@code true} for {@link JsonNode#isShort()} and the
     * value can be retrieved via {@link JsonNode#shortValue()}.</p>
     *
     * <p><strong>Note:</strong> Jackson does not have a dedicated byte node type. Byte values
     * are stored as {@link ShortNode} to preserve the exact value. JSON does not have a
     * dedicated byte type, so type information may be lost during round-trips through JSON
     * text serialization.</p>
     *
     * @param value the byte value to wrap
     * @return a new {@link ShortNode} containing the byte value as a short;
     *         never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createByte(final byte value) {
        return ShortNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON short node from the given short value. The resulting
     * {@link ShortNode} will return {@code true} for {@link JsonNode#isShort()} and the
     * value can be retrieved via {@link JsonNode#shortValue()}.</p>
     *
     * <p><strong>Note:</strong> JSON does not have a dedicated short type. The value is
     * stored as a generic number and type information may be lost during round-trips
     * through JSON text serialization.</p>
     *
     * @param value the short value to wrap
     * @return a new {@link ShortNode} containing the short value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createShort(final short value) {
        return ShortNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON boolean node from the given boolean value. The resulting
     * {@link BooleanNode} will return {@code true} for {@link JsonNode#isBoolean()}
     * and the value can be retrieved via {@link JsonNode#asBoolean()} or
     * {@link JsonNode#booleanValue()}.</p>
     *
     * <p>Jackson uses singleton {@link BooleanNode} instances for {@code true} and
     * {@code false}, ensuring memory efficiency when creating multiple boolean nodes.</p>
     *
     * @param value the boolean value to wrap
     * @return a {@link BooleanNode} containing the boolean value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createBoolean(final boolean value) {
        return BooleanNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON numeric node from the given {@link Number} value. This
     * method accepts any {@link Number} subclass and creates the most appropriate
     * Jackson node type based on the runtime type of the number:</p>
     * <ul>
     *   <li>{@link Integer} - creates {@link IntNode}</li>
     *   <li>{@link Long} - creates {@link LongNode}</li>
     *   <li>{@link Float} - creates {@link FloatNode}</li>
     *   <li>{@link Double} - creates {@link DoubleNode}</li>
     *   <li>{@link Short} - creates {@link ShortNode}</li>
     *   <li>{@link Byte} - creates {@link ShortNode}</li>
     *   <li>Other types - creates {@link DoubleNode} using {@link Number#doubleValue()}</li>
     * </ul>
     *
     * <p>This method preserves the numeric type as closely as possible within Jackson's
     * type system, though JSON serialization may normalize the representation.</p>
     *
     * @param value the number value to wrap; must not be {@code null}
     * @return a new numeric {@link JsonNode} containing the number value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createNumeric(@NotNull final Number value) {
        Preconditions.checkNotNull(value, "value must not be null");
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

    // ==================== Primitive Reading Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the string value from a JSON node. This operation succeeds only if
     * the input is a {@link TextNode}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is a {@link TextNode} (i.e., {@link JsonNode#isTextual()} returns {@code true})</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link TextNode} (e.g., array, object, null, numeric, boolean)</li>
     * </ul>
     *
     * @param input the JSON node to extract the string from; must not be {@code null}
     * @return a {@link DataResult} containing the string value on success, or an error
     *         message describing why extraction failed; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<String> getStringValue(@NotNull final JsonNode input) {
        Preconditions.checkNotNull(input, "input must not be null");
        if (!input.isTextual()) {
            return DataResult.error("Not a string: " + input);
        }
        return DataResult.success(input.asText());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the numeric value from a JSON node. This operation succeeds only if
     * the input is a numeric node (any of the numeric node types in Jackson's hierarchy).</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is any numeric node type ({@link IntNode}, {@link LongNode},
     *       {@link FloatNode}, {@link DoubleNode}, {@link ShortNode},
     *       {@link com.fasterxml.jackson.databind.node.BigIntegerNode},
     *       {@link com.fasterxml.jackson.databind.node.DecimalNode})</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a numeric node (e.g., array, object, null, string, boolean)</li>
     * </ul>
     *
     * <p>The returned {@link Number} preserves the original numeric type where possible,
     * allowing callers to use type-specific accessors like {@link Number#intValue()} or
     * {@link Number#doubleValue()}.</p>
     *
     * @param input the JSON node to extract the number from; must not be {@code null}
     * @return a {@link DataResult} containing the number value on success, or an error
     *         message describing why extraction failed; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<Number> getNumberValue(@NotNull final JsonNode input) {
        Preconditions.checkNotNull(input, "input must not be null");
        if (!input.isNumber()) {
            return DataResult.error("Not a number: " + input);
        }
        return DataResult.success(input.numberValue());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the boolean value from a JSON node. This operation succeeds only if
     * the input is a {@link BooleanNode}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is a {@link BooleanNode} (i.e., {@link JsonNode#isBoolean()} returns {@code true})</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link BooleanNode} (e.g., array, object, null, string, numeric)</li>
     * </ul>
     *
     * @param input the JSON node to extract the boolean from; must not be {@code null}
     * @return a {@link DataResult} containing the boolean value on success, or an error
     *         message describing why extraction failed; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<Boolean> getBooleanValue(@NotNull final JsonNode input) {
        Preconditions.checkNotNull(input, "input must not be null");
        if (!input.isBoolean()) {
            return DataResult.error("Not a boolean: " + input);
        }
        return DataResult.success(input.asBoolean());
    }

    // ==================== List Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new empty JSON array. This is the canonical way to create an empty
     * list structure in the Jackson format. The array is created using the
     * {@link JsonNodeFactory} obtained from the configured {@link ObjectMapper}.</p>
     *
     * @return a new empty {@link ArrayNode}; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode emptyList() {
        return this.nodeFactory.arrayNode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON array containing all elements from the provided stream.
     * Elements are added to the array in encounter order. The stream is consumed
     * completely by this operation.</p>
     *
     * <p>The array is created using the {@link JsonNodeFactory} obtained from the
     * configured {@link ObjectMapper}.</p>
     *
     * <p><strong>Note:</strong> {@code null} elements in the stream will be added
     * as-is (as {@link NullNode}). Ensure the stream contains valid {@link JsonNode}
     * instances for predictable behavior.</p>
     *
     * @param values a stream of JSON nodes to include in the array; must not be
     *               {@code null}; may be empty
     * @return a new {@link ArrayNode} containing all stream elements in order;
     *         never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createList(@NotNull final Stream<JsonNode> values) {
        Preconditions.checkNotNull(values, "values must not be null");
        final ArrayNode array = this.nodeFactory.arrayNode();
        values.forEach(array::add);
        return array;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the elements of a JSON array as a stream. This operation succeeds only
     * if the input is an {@link ArrayNode}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is an {@link ArrayNode} (including empty arrays)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not an {@link ArrayNode} (e.g., object, value node, null)</li>
     * </ul>
     *
     * <p>The returned stream provides sequential access to array elements in order.
     * The stream is created using {@link StreamSupport} with an iterator over the
     * array elements.</p>
     *
     * @param input the JSON node to extract array elements from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of array elements on success,
     *         or an error message if the input is not an array; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<Stream<JsonNode>> getList(@NotNull final JsonNode input) {
        Preconditions.checkNotNull(input, "input must not be null");
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
     * <p>Creates a new array by appending a value to an existing array. This operation
     * creates a deep copy of the input array to preserve immutability.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input list is an {@link ArrayNode}</li>
     *   <li>Input list is {@link NullNode} (treated as empty array)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input list is not an array or null (e.g., object, value node)</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original list is never modified. A deep copy
     * is created via {@link ArrayNode#deepCopy()} before the new element is appended.</p>
     *
     * @param list  the array to append to; must not be {@code null}; may be
     *              {@link NullNode} (treated as empty array)
     * @param value the value to append; must not be {@code null}
     * @return a {@link DataResult} containing the new array with the appended value,
     *         or an error message if the list is not valid; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<JsonNode> mergeToList(@NotNull final JsonNode list,
                                            @NotNull final JsonNode value) {
        Preconditions.checkNotNull(list, "list must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
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
     * <p>Creates a new empty JSON object. This is the canonical way to create an empty
     * map structure in the Jackson format. The object is created using the
     * {@link JsonNodeFactory} obtained from the configured {@link ObjectMapper}.</p>
     *
     * @return a new empty {@link ObjectNode}; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode emptyMap() {
        return this.nodeFactory.objectNode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new JSON object from a stream of key-value pairs. Keys must be
     * JSON text nodes; entries with {@code null} keys are skipped.</p>
     *
     * <h3>Key Handling</h3>
     * <ul>
     *   <li>Keys are converted to strings via {@link JsonNode#asText()}</li>
     *   <li>Entries with {@code null} keys are silently skipped</li>
     *   <li>Duplicate keys result in the last value being retained</li>
     * </ul>
     *
     * <h3>Value Handling</h3>
     * <ul>
     *   <li>{@code null} values are converted to {@link NullNode#getInstance()}</li>
     *   <li>All other values are added as-is</li>
     * </ul>
     *
     * <p>The object is created using the {@link JsonNodeFactory} obtained from the
     * configured {@link ObjectMapper}.</p>
     *
     * @param entries a stream of key-value pairs; must not be {@code null}; may be empty
     * @return a new {@link ObjectNode} containing all valid entries; never {@code null}
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
     * <p>Returns the entries of a JSON object as a stream of key-value pairs. This
     * operation succeeds only if the input is an {@link ObjectNode}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is an {@link ObjectNode} (including empty objects)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not an {@link ObjectNode} (e.g., array, value node, null)</li>
     * </ul>
     *
     * <p>Each entry in the returned stream has:</p>
     * <ul>
     *   <li>Key: a {@link TextNode} containing the field name as a string</li>
     *   <li>Value: the field's {@link JsonNode} value</li>
     * </ul>
     *
     * @param input the JSON node to extract object entries from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of key-value pairs on success,
     *         or an error message if the input is not an object; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<Stream<Pair<JsonNode, JsonNode>>> getMapEntries(@NotNull final JsonNode input) {
        Preconditions.checkNotNull(input, "input must not be null");
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
     * <p>Creates a new object by adding or updating a key-value pair in an existing map.
     * This operation creates a deep copy of the input map to preserve immutability.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input map is an {@link ObjectNode}</li>
     *   <li>Input map is {@link NullNode} (treated as empty object)</li>
     *   <li>Key is a {@link TextNode} (i.e., {@link JsonNode#isTextual()} returns {@code true})</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input map is not an object or null (e.g., array, value node)</li>
     *   <li>Key is not a text node</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original map is never modified. A deep copy
     * is created via {@link ObjectNode#deepCopy()} before the entry is added.</p>
     *
     * @param map   the object to add the entry to; must not be {@code null}; may be
     *              {@link NullNode} (treated as empty object)
     * @param key   the key for the entry; must not be {@code null}; must be a text node
     * @param value the value for the entry; must not be {@code null}
     * @return a {@link DataResult} containing the new object with the added entry,
     *         or an error message if parameters are invalid; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<JsonNode> mergeToMap(@NotNull final JsonNode map,
                                           @NotNull final JsonNode key,
                                           @NotNull final JsonNode value) {
        Preconditions.checkNotNull(map, "map must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
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
     * <p>Creates a new object by merging all entries from a second object into a copy
     * of the first object. This operation creates a deep copy of the first map to
     * preserve immutability.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Both inputs are {@link ObjectNode} instances</li>
     *   <li>Either input may be {@link NullNode} (treated as empty object)</li>
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
     *   <li>Order is not guaranteed (depends on {@link ObjectNode} implementation)</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> Neither input object is modified. A deep copy
     * of the first map is created via {@link ObjectNode#deepCopy()} before merging.</p>
     *
     * @param map   the base object; must not be {@code null}; may be {@link NullNode}
     * @param other the object to merge into the base; must not be {@code null}; may be
     *              {@link NullNode}
     * @return a {@link DataResult} containing the merged object, or an error message
     *         if either input is invalid; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<JsonNode> mergeToMap(@NotNull final JsonNode map,
                                           @NotNull final JsonNode other) {
        Preconditions.checkNotNull(map, "map must not be null");
        Preconditions.checkNotNull(other, "other must not be null");
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
     * <p>Retrieves a field value from a JSON object by key. Returns {@code null} if
     * the input is not an object or the key does not exist.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>Returns the field value if the input is an {@link ObjectNode} containing the key</li>
     *   <li>Returns {@code null} if the input is not an {@link ObjectNode}</li>
     *   <li>Returns {@code null} if the key does not exist in the object</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This method does not distinguish between a missing key
     * and a key mapped to {@link NullNode}. Use {@link #has(JsonNode, String)} to
     * check for key existence when this distinction matters.</p>
     *
     * @param input the JSON node to retrieve the field from; must not be {@code null}
     * @param key   the field name to retrieve; must not be {@code null}
     * @return the field value if found, or {@code null} if not found or input is not an object
     */
    @Override
    public @Nullable JsonNode get(@NotNull final JsonNode input,
                                  @NotNull final String key) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
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
     * <p>Creates a new object with a field set to the specified value. This operation
     * creates a deep copy of the input to preserve immutability.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>If input is an {@link ObjectNode}: creates a deep copy and sets the field</li>
     *   <li>If input is not an {@link ObjectNode}: creates a new object with just the field</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original input is never modified. When the
     * input is an object, {@link JsonNode#deepCopy()} is used to create the copy.</p>
     *
     * @param input    the JSON node to set the field on; must not be {@code null}
     * @param key      the field name to set; must not be {@code null}
     * @param newValue the value to set; must not be {@code null}
     * @return a new {@link ObjectNode} with the field set; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode set(@NotNull final JsonNode input,
                        @NotNull final String key,
                        @NotNull final JsonNode newValue) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(newValue, "newValue must not be null");
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
     * <p>Creates a new object with a field removed. This operation creates a deep copy
     * of the input to preserve immutability.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>If input is an {@link ObjectNode}: creates a deep copy and removes the field</li>
     *   <li>If input is not an {@link ObjectNode}: returns the input unchanged</li>
     *   <li>If the key does not exist: returns a deep copy without modification</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original input is never modified. When the
     * input is an object, {@link JsonNode#deepCopy()} is used to create the copy.</p>
     *
     * @param input the JSON node to remove the field from; must not be {@code null}
     * @param key   the field name to remove; must not be {@code null}
     * @return a {@link JsonNode} with the field removed (or unchanged if not applicable);
     *         never {@code null}
     */
    @NotNull
    @Override
    public JsonNode remove(@NotNull final JsonNode input,
                           @NotNull final String key) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
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
     * <p>Checks whether a JSON object contains a field with the specified key.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>Returns {@code true} if input is an {@link ObjectNode} and contains the key</li>
     *   <li>Returns {@code false} if input is not an {@link ObjectNode}</li>
     *   <li>Returns {@code false} if the key does not exist</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This method returns {@code true} even if the key is
     * mapped to {@link NullNode}. It only checks for key existence, not value validity.</p>
     *
     * @param input the JSON node to check; must not be {@code null}
     * @param key   the field name to check for; must not be {@code null}
     * @return {@code true} if the input is an object containing the key, {@code false} otherwise
     */
    @Override
    public boolean has(@NotNull final JsonNode input,
                       @NotNull final String key) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        if (!input.isObject()) {
            return false;
        }
        return input.has(key);
    }

    // ==================== Conversion Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Converts data from another {@link DynamicOps} format to Jackson {@link JsonNode}.
     * This method recursively converts all nested structures, handling primitives, lists,
     * and maps appropriately.</p>
     *
     * <h3>Conversion Process</h3>
     * <p>The conversion attempts to identify the input type in the following order:</p>
     * <ol>
     *   <li><strong>Boolean:</strong> If {@link DynamicOps#getBooleanValue} succeeds,
     *       creates a {@link BooleanNode} with the boolean value</li>
     *   <li><strong>Number:</strong> If {@link DynamicOps#getNumberValue} succeeds,
     *       creates the appropriate numeric node type via {@link #createNumeric(Number)}</li>
     *   <li><strong>String:</strong> If {@link DynamicOps#getStringValue} succeeds,
     *       creates a {@link TextNode} with the string value</li>
     *   <li><strong>List:</strong> If {@link DynamicOps#getList} succeeds,
     *       creates an {@link ArrayNode} with recursively converted elements</li>
     *   <li><strong>Map:</strong> If {@link DynamicOps#getMapEntries} succeeds,
     *       creates an {@link ObjectNode} with recursively converted entries</li>
     *   <li><strong>Fallback:</strong> Returns {@link NullNode#getInstance()} if no type matches</li>
     * </ol>
     *
     * <h3>Edge Cases</h3>
     * <ul>
     *   <li>Map entries with {@code null} keys are skipped</li>
     *   <li>Map entries with {@code null} values are converted to {@link NullNode}</li>
     *   <li>Empty collections are preserved as empty arrays/objects</li>
     * </ul>
     *
     * @param sourceOps the {@link DynamicOps} instance for the source format; must not be
     *                  {@code null}
     * @param input     the value to convert from the source format; must not be {@code null}
     * @param <U>       the type parameter of the source format
     * @return the converted {@link JsonNode}; never {@code null}; returns
     *         {@link NullNode#getInstance()} if conversion is not possible
     */
    @NotNull
    @Override
    public <U> JsonNode convertTo(@NotNull final DynamicOps<U> sourceOps,
                                  @NotNull final U input) {
        Preconditions.checkNotNull(sourceOps, "sourceOps must not be null");
        Preconditions.checkNotNull(input, "input must not be null");
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

        // Fallback: return JSON null for unknown types
        return empty();
    }

    /**
     * Returns a string representation of this {@code DynamicOps} instance.
     *
     * <p>This method returns a fixed string identifying the implementation, useful for
     * debugging, logging, and error messages. The string does not include information
     * about the specific {@link ObjectMapper} configuration.</p>
     *
     * @return the string {@code "JacksonJsonOps"}; never {@code null}
     */
    @Override
    public String toString() {
        return "JacksonJsonOps";
    }
}
