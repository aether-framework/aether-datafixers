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

package de.splatgames.aether.datafixers.codec.toml.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
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
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
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
 * A {@link DynamicOps} implementation for Jackson's {@link JsonNode} using TOML format
 * via Jackson's {@link TomlMapper}.
 *
 * <p>This class provides format-agnostic data manipulation capabilities for Jackson's
 * TOML tree model, enabling the Aether Datafixers system to read, write, and transform
 * TOML data without coupling application code to Jackson-specific APIs. It serves as the
 * bridge between the abstract data fixer operations and Jackson's concrete TOML model.</p>
 *
 * <h2>Design Pattern</h2>
 * <p>This class provides both a singleton instance ({@link #INSTANCE}) and supports
 * custom instantiation via {@link #JacksonTomlOps(TomlMapper)}. The dual approach allows:</p>
 * <ul>
 *   <li>Simple usage via the singleton for default configuration</li>
 *   <li>Custom configuration when specific {@link TomlMapper} settings are required</li>
 *   <li>Flexibility for dependency injection frameworks requiring custom instances</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Encoding/Decoding</h3>
 * <pre>{@code
 * // Encode a value to TOML using a codec
 * DataResult<JsonNode> encoded = configCodec.encodeStart(JacksonTomlOps.INSTANCE, config);
 *
 * // Decode TOML to a typed value
 * DataResult<Config> decoded = configCodec.decode(JacksonTomlOps.INSTANCE, tomlNode);
 * }</pre>
 *
 * <h3>Creating Dynamic Wrappers</h3>
 * <pre>{@code
 * // Wrap existing TOML data for manipulation
 * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonTomlOps.INSTANCE, existingToml);
 *
 * // Access nested fields
 * Optional<String> title = dynamic.get("package").get("name").asString();
 *
 * // Transform data
 * Dynamic<JsonNode> updated = dynamic.set("version", JacksonTomlOps.INSTANCE.createString("2.0.0"));
 * }</pre>
 *
 * <h3>Using a Custom TomlMapper</h3>
 * <pre>{@code
 * // Create a custom TomlMapper with specific configuration
 * TomlMapper customMapper = TomlMapper.builder()
 *     .enable(TomlWriteFeature.FAIL_ON_NULL_WRITE)
 *     .build();
 * JacksonTomlOps customOps = new JacksonTomlOps(customMapper);
 *
 * // Use the custom ops for encoding
 * DataResult<JsonNode> result = myCodec.encodeStart(customOps, value);
 * }</pre>
 *
 * <h3>Format Conversion</h3>
 * <pre>{@code
 * // Convert from Gson JSON to Jackson TOML
 * JsonNode tomlNode = JacksonTomlOps.INSTANCE.convertTo(GsonOps.INSTANCE, gsonElement);
 *
 * // Convert from YAML to TOML
 * JsonNode fromYaml = JacksonTomlOps.INSTANCE.convertTo(SnakeYamlOps.INSTANCE, yamlData);
 * }</pre>
 *
 * <h2>TOML Format Limitations</h2>
 * <p>TOML has specific structural constraints that differ from JSON and other formats.
 * This implementation handles these constraints as follows:</p>
 *
 * <h3>Top-Level Must Be a Table</h3>
 * <p>TOML requires the root element to be a table (object). When serializing data to
 * TOML text format using the {@link TomlMapper}, ensure the root element is an
 * {@link ObjectNode}. Attempting to serialize a primitive or array at the root level
 * will result in serialization errors.</p>
 *
 * <h3>No Native Null Support</h3>
 * <p>TOML does not natively support null values. While this implementation uses
 * {@link NullNode} internally to represent absence of value (for API compatibility),
 * null values will be omitted or cause errors during TOML serialization. Consider
 * using optional fields or sentinel values when working with TOML.</p>
 *
 * <h3>Homogeneous Arrays</h3>
 * <p>Standard TOML requires arrays to contain elements of the same type. While the
 * internal {@link ArrayNode} representation allows heterogeneous arrays, serializing
 * such arrays to TOML text may fail or produce unexpected results.</p>
 *
 * <h2>Type Mapping</h2>
 * <p>The following table shows how Java/abstract types map to Jackson TOML node types:</p>
 * <table border="1" cellpadding="5">
 *   <caption>Type Mapping between Java and Jackson TOML Nodes</caption>
 *   <tr><th>Java Type</th><th>Jackson Node Type</th><th>TOML Type</th><th>Notes</th></tr>
 *   <tr><td>{@code boolean}</td><td>{@link BooleanNode}</td><td>Boolean</td><td>Native TOML type</td></tr>
 *   <tr><td>{@code int}</td><td>{@link IntNode}</td><td>Integer</td><td>Native TOML type</td></tr>
 *   <tr><td>{@code long}</td><td>{@link LongNode}</td><td>Integer</td><td>Native TOML type</td></tr>
 *   <tr><td>{@code float}</td><td>{@link FloatNode}</td><td>Float</td><td>Native TOML type</td></tr>
 *   <tr><td>{@code double}</td><td>{@link DoubleNode}</td><td>Float</td><td>Native TOML type</td></tr>
 *   <tr><td>{@code byte, short}</td><td>{@link ShortNode}</td><td>Integer</td><td>Stored as short internally</td></tr>
 *   <tr><td>{@code String}</td><td>{@link TextNode}</td><td>String</td><td>Native TOML type</td></tr>
 *   <tr><td>{@code List/Stream}</td><td>{@link ArrayNode}</td><td>Array</td><td>Must be homogeneous for TOML</td></tr>
 *   <tr><td>{@code Map}</td><td>{@link ObjectNode}</td><td>Table</td><td>String keys only</td></tr>
 *   <tr><td>{@code null/empty}</td><td>{@link NullNode}</td><td>N/A</td><td>No TOML equivalent; omitted on serialization</td></tr>
 * </table>
 *
 * <h2>Immutability Contract</h2>
 * <p>All modification operations in this class preserve immutability of input data:</p>
 * <ul>
 *   <li>{@link #set(JsonNode, String, JsonNode)} creates a deep copy via
 *       {@link JsonNode#deepCopy()} before modification</li>
 *   <li>{@link #remove(JsonNode, String)} creates a deep copy before removal</li>
 *   <li>{@link #mergeToMap(JsonNode, JsonNode, JsonNode)} creates a deep copy of
 *       the target map via {@link ObjectNode#deepCopy()}</li>
 *   <li>{@link #mergeToList(JsonNode, JsonNode)} creates a deep copy of the target
 *       list via {@link ArrayNode#deepCopy()}</li>
 * </ul>
 * <p>This ensures that original data structures are never modified, enabling safe
 * concurrent access and functional programming patterns. The immutability guarantee
 * is essential for the data fixer pipeline where multiple transformations may be
 * applied to the same source data.</p>
 *
 * <h2>Error Handling</h2>
 * <p>Operations that may fail return {@link DataResult} instead of throwing exceptions:</p>
 * <ul>
 *   <li>Type mismatches result in {@link DataResult#error(String)} with descriptive messages
 *       indicating the expected type and actual value</li>
 *   <li>Successful operations return {@link DataResult#success(Object)} containing the result</li>
 *   <li>Callers should check {@link DataResult#isSuccess()} or use
 *       {@link DataResult#result()} to access values safely</li>
 *   <li>Error messages include the problematic value's string representation for debugging</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Thread safety of this class depends on the underlying {@link TomlMapper}:</p>
 * <ul>
 *   <li>The {@link #INSTANCE} singleton is thread-safe for read operations after
 *       class initialization</li>
 *   <li>Custom instances are thread-safe if the provided {@link TomlMapper} is thread-safe
 *       (Jackson mappers are thread-safe after configuration for read operations)</li>
 *   <li>All operations are effectively stateless, operating only on input parameters</li>
 *   <li>The {@link JsonNodeFactory} obtained from the mapper is used for node creation,
 *       which is thread-safe</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Deep copy operations ({@link #set}, {@link #remove}, {@link #mergeToMap},
 *       {@link #mergeToList}) have O(n) complexity where n is the structure size</li>
 *   <li>Prefer batch modifications via {@link #createMap(Stream)} over repeated
 *       {@link #set} calls to minimize copy overhead</li>
 *   <li>Stream-based operations ({@link #getList}, {@link #getMapEntries}) are lazy
 *       and support short-circuiting for efficient partial traversal</li>
 *   <li>The {@link JsonNodeFactory} from the mapper is cached for efficient node creation</li>
 *   <li>Primitive creation methods use static factory methods (e.g., {@link IntNode#valueOf})
 *       which may employ caching for common values</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see JsonNode
 * @see TomlMapper
 * @see DataResult
 * @since 0.4.0
 */
public final class JacksonTomlOps implements DynamicOps<JsonNode> {

    /**
     * The singleton instance of {@code JacksonTomlOps} with a default {@link TomlMapper}.
     *
     * <p>This instance should be used for standard TOML operations throughout the application
     * when no custom mapper configuration is required. It uses a default {@code TomlMapper}
     * with standard settings.</p>
     *
     * <p>For custom {@link TomlMapper} configurations (e.g., custom date/time handling,
     * specific serialization features, or custom modules), create a new instance using
     * {@link #JacksonTomlOps(TomlMapper)}.</p>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Direct usage for creating nodes
     * JsonNode stringNode = JacksonTomlOps.INSTANCE.createString("hello");
     * JsonNode intNode = JacksonTomlOps.INSTANCE.createInt(42);
     *
     * // With Dynamic wrapper
     * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonTomlOps.INSTANCE, tomlNode);
     *
     * // With Codecs
     * DataResult<JsonNode> result = myCodec.encodeStart(JacksonTomlOps.INSTANCE, value);
     *
     * // Serialization to TOML string (using the mapper)
     * String tomlString = JacksonTomlOps.INSTANCE.mapper().writeValueAsString(objectNode);
     * }</pre>
     *
     * <p><strong>Thread Safety:</strong> This instance is thread-safe and can be safely
     * shared across multiple threads for concurrent operations.</p>
     */
    public static final JacksonTomlOps INSTANCE = new JacksonTomlOps(new TomlMapper());

    /**
     * The {@link TomlMapper} used for TOML-specific operations and node factory access.
     *
     * <p>This mapper is primarily used to obtain the {@link JsonNodeFactory} for creating
     * new nodes. It can also be accessed via {@link #mapper()} for direct TOML serialization
     * and deserialization operations outside the {@code DynamicOps} interface.</p>
     */
    private final TomlMapper mapper;

    /**
     * The {@link JsonNodeFactory} used for creating all {@link JsonNode} instances.
     *
     * <p>This factory is obtained from the {@link TomlMapper} during construction and
     * cached for efficient node creation. Using the mapper's factory ensures consistency
     * with any custom node factory configuration on the mapper.</p>
     */
    private final JsonNodeFactory nodeFactory;

    /**
     * Creates a new {@code JacksonTomlOps} with the specified {@link TomlMapper}.
     *
     * <p>The mapper's {@link JsonNodeFactory} will be extracted and used for creating
     * all nodes. This allows customization of node creation behavior and ensures
     * consistency with the mapper's configuration.</p>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Create with custom mapper configuration
     * TomlMapper customMapper = TomlMapper.builder()
     *     .enable(TomlWriteFeature.FAIL_ON_NULL_WRITE)
     *     .addModule(new JavaTimeModule())
     *     .build();
     * JacksonTomlOps customOps = new JacksonTomlOps(customMapper);
     *
     * // Use the custom ops
     * DataResult<JsonNode> result = codec.encodeStart(customOps, value);
     *
     * // Access the mapper for direct serialization
     * String toml = customOps.mapper().writeValueAsString(result.result().get());
     * }</pre>
     *
     * @param mapper the TOML mapper to use for node factory access and optional direct
     *               serialization; must not be {@code null}
     */
    public JacksonTomlOps(@NotNull final TomlMapper mapper) {
        Preconditions.checkNotNull(mapper, "mapper must not be null");
        this.mapper = mapper;
        this.nodeFactory = mapper.getNodeFactory();
    }

    /**
     * Returns the {@link TomlMapper} used by this instance.
     *
     * <p>This method provides access to the underlying mapper for operations outside
     * the {@code DynamicOps} interface, such as:</p>
     * <ul>
     *   <li>Serializing {@link JsonNode} to TOML string format</li>
     *   <li>Deserializing TOML strings to {@link JsonNode}</li>
     *   <li>Reading TOML from files or streams</li>
     *   <li>Writing TOML to files or streams</li>
     * </ul>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Serialize to TOML string
     * String tomlString = JacksonTomlOps.INSTANCE.mapper().writeValueAsString(objectNode);
     *
     * // Deserialize from TOML string
     * JsonNode parsed = JacksonTomlOps.INSTANCE.mapper().readTree(tomlString);
     *
     * // Read from file
     * JsonNode fromFile = JacksonTomlOps.INSTANCE.mapper().readTree(new File("config.toml"));
     * }</pre>
     *
     * @return the TOML mapper used by this instance; never {@code null}
     */
    public TomlMapper mapper() {
        return this.mapper;
    }

    // ==================== Empty/Null Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Returns the null node singleton, which represents the absence of a value.
     * This is the canonical "empty" value for the Jackson format.</p>
     *
     * <p><strong>TOML Note:</strong> TOML does not natively support null values.
     * While this method returns {@link NullNode#getInstance()} for API compatibility,
     * null nodes will be omitted or cause errors when serialized to TOML text format
     * using the {@link TomlMapper}. Consider using optional fields or sentinel values
     * when the data will be serialized to TOML.</p>
     *
     * <h3>Use Cases</h3>
     * <ul>
     *   <li>Representing absent optional values in the internal tree model</li>
     *   <li>Fallback return value when conversion cannot determine the type</li>
     *   <li>Placeholder for values that will be filtered out before serialization</li>
     * </ul>
     *
     * @return {@link NullNode#getInstance()}, the singleton null node; never {@code null}
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
     * <p>Determines whether the given node represents a map/object structure.
     * In TOML terminology, this corresponds to a "table" which contains key-value
     * pairs where keys are always strings.</p>
     *
     * <h3>TOML Context</h3>
     * <p>Tables are the fundamental structured type in TOML. They can be defined
     * using bracket notation {@code [table]} or inline notation {@code {key = value}}.
     * In the Jackson tree model, both representations are stored as {@link ObjectNode}.</p>
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ObjectNode} (TOML table),
     *         {@code false} otherwise (including for {@link NullNode}, {@link ArrayNode},
     *         and primitive nodes)
     */
    @Override
    public boolean isMap(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isObject();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a list/array structure.
     * In TOML terminology, this corresponds to an "array" which contains an ordered
     * sequence of values.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML arrays can be defined using bracket notation {@code [1, 2, 3]} and
     * support both inline and multiline formats. Standard TOML requires arrays to
     * contain homogeneous types (except for arrays of tables). While the internal
     * {@link ArrayNode} representation allows heterogeneous content, serialization
     * to TOML may enforce type homogeneity.</p>
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ArrayNode} (TOML array),
     *         {@code false} otherwise (including for {@link NullNode}, {@link ObjectNode},
     *         and primitive nodes)
     */
    @Override
    public boolean isList(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isArray();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a string value.
     * In TOML, strings can be basic (with escapes), literal (no escapes),
     * or multiline variants of either.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML supports four string types:</p>
     * <ul>
     *   <li>Basic strings: {@code "hello\nworld"} (with escape sequences)</li>
     *   <li>Literal strings: {@code 'C:\path'} (no escapes, single quotes)</li>
     *   <li>Multiline basic: {@code """..."""}</li>
     *   <li>Multiline literal: {@code '''...'''}</li>
     * </ul>
     * <p>All are stored as {@link TextNode} in the Jackson tree model.</p>
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is a {@link TextNode} (TOML string),
     *         {@code false} otherwise (including for numeric and boolean nodes)
     */
    @Override
    public boolean isString(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isTextual();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a numeric value.
     * In TOML, this includes both integers and floating-point numbers.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML distinguishes between integers and floats:</p>
     * <ul>
     *   <li>Integers: {@code 42}, {@code 0xDEADBEEF}, {@code 0o755}, {@code 0b1010}</li>
     *   <li>Floats: {@code 3.14}, {@code 5e+22}, {@code inf}, {@code nan}</li>
     * </ul>
     * <p>This method returns {@code true} for any numeric node type including
     * {@link IntNode}, {@link LongNode}, {@link FloatNode}, {@link DoubleNode},
     * and {@link ShortNode}.</p>
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is a numeric node (TOML integer or float),
     *         {@code false} otherwise (including for string and boolean nodes)
     */
    @Override
    public boolean isNumber(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isNumber();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a boolean value.
     * TOML booleans are native types with lowercase literals.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML booleans must be lowercase: {@code true} or {@code false}.
     * The Jackson parser handles this automatically, storing the result
     * as a {@link BooleanNode}.</p>
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is a {@link BooleanNode} (TOML boolean),
     *         {@code false} otherwise (including for string and numeric nodes)
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
     * <p>Creates a new text node from the given string value. The resulting
     * {@link TextNode} represents a TOML string value.</p>
     *
     * <h3>TOML Context</h3>
     * <p>When serialized to TOML, the string will be written using appropriate
     * quoting based on content (basic or literal, single or multiline) as
     * determined by the {@link TomlMapper}'s configuration.</p>
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
     * <p>Creates a new integer node from the given int value. The resulting
     * {@link IntNode} represents a TOML integer value.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML integers support 64-bit signed values. When serialized, the
     * value will be written in decimal format by default. The mapper may
     * be configured to use underscores for readability (e.g., {@code 1_000_000}).</p>
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
     * <p>Creates a new long node from the given long value. The resulting
     * {@link LongNode} represents a TOML integer value.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML integers are 64-bit signed, so all Java long values are
     * representable. Very large values will be serialized without loss
     * of precision, unlike JSON which may have precision issues with
     * large numbers in some parsers.</p>
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
     * <p>Creates a new float node from the given float value. The resulting
     * {@link FloatNode} represents a TOML float value.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML floats follow IEEE 754 binary64 representation. Special values
     * are supported:</p>
     * <ul>
     *   <li>Positive infinity: {@code inf}</li>
     *   <li>Negative infinity: {@code -inf}</li>
     *   <li>Not a number: {@code nan}</li>
     * </ul>
     * <p>These special values are valid TOML, unlike in JSON.</p>
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
     * <p>Creates a new double node from the given double value. The resulting
     * {@link DoubleNode} represents a TOML float value.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML floats follow IEEE 754 binary64 representation, which matches
     * Java's double precision. Special values ({@code inf}, {@code -inf},
     * {@code nan}) are valid TOML and will be serialized correctly.</p>
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
     * <p>Creates a new short node from the given byte value. The value is
     * widened to short for storage in a {@link ShortNode}.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML does not have a dedicated byte type. The value is stored as
     * a short internally and will be serialized as a regular TOML integer.
     * Type information may be lost during round-trips through TOML text
     * serialization.</p>
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
     * <p>Creates a new short node from the given short value. The resulting
     * {@link ShortNode} represents a TOML integer value.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML does not have a dedicated short type. The value will be
     * serialized as a regular TOML integer. Type information may be lost
     * during round-trips through TOML text serialization.</p>
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
     * <p>Creates a new boolean node from the given boolean value. The resulting
     * {@link BooleanNode} represents a TOML boolean value.</p>
     *
     * <h3>TOML Context</h3>
     * <p>TOML booleans are serialized as lowercase {@code true} or {@code false}.
     * Unlike JSON, TOML does not allow numeric representations (0/1) for booleans.</p>
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
     * <p>Creates a new numeric node from the given {@link Number} value. This method
     * selects the appropriate node type based on the runtime type of the number.</p>
     *
     * <h3>Type Selection</h3>
     * <p>The following mapping is used based on the number's runtime type:</p>
     * <ul>
     *   <li>{@link Integer} -> {@link IntNode}</li>
     *   <li>{@link Long} -> {@link LongNode}</li>
     *   <li>{@link Float} -> {@link FloatNode}</li>
     *   <li>{@link Double} -> {@link DoubleNode}</li>
     *   <li>{@link Short} -> {@link ShortNode}</li>
     *   <li>{@link Byte} -> {@link ShortNode}</li>
     *   <li>Other {@link Number} subclasses -> {@link DoubleNode} (via {@link Number#doubleValue()})</li>
     * </ul>
     *
     * <h3>TOML Context</h3>
     * <p>TOML distinguishes between integers and floats but does not have
     * type-specific variants. The specific node type primarily affects internal
     * representation; serialization to TOML will produce appropriate integer
     * or float notation.</p>
     *
     * @param value the number value to wrap; must not be {@code null}
     * @return a numeric node appropriate for the number type; never {@code null}
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
     * <p>Extracts the string value from a node. This operation succeeds only if
     * the input is a {@link TextNode}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is a {@link TextNode} (textual node)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link TextNode} (e.g., array, object, null, number, boolean)</li>
     * </ul>
     *
     * <p>The error message includes the actual node value for debugging purposes.</p>
     *
     * @param input the node to extract the string from; must not be {@code null}
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
     * <p>Extracts the numeric value from a node. This operation succeeds only if
     * the input is a numeric node ({@link IntNode}, {@link LongNode}, {@link FloatNode},
     * {@link DoubleNode}, or {@link ShortNode}).</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is any numeric node type</li>
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
     * @param input the node to extract the number from; must not be {@code null}
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
     * <p>Extracts the boolean value from a node. This operation succeeds only if
     * the input is a {@link BooleanNode}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is a {@link BooleanNode}</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link BooleanNode} (e.g., array, object, null, string, number)</li>
     * </ul>
     *
     * <p>Unlike some formats that treat 0/1 or "true"/"false" strings as booleans,
     * this method strictly requires a boolean node type.</p>
     *
     * @param input the node to extract the boolean from; must not be {@code null}
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
     * <p>Creates a new empty array node. This is the canonical way to create an empty
     * list structure in the Jackson TOML format.</p>
     *
     * <h3>TOML Context</h3>
     * <p>Empty arrays are valid TOML and serialize as {@code []}. The array can be
     * populated with elements of any type, though standard TOML requires type
     * homogeneity for serialization.</p>
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
     * <p>Creates a new array node containing all elements from the provided stream.
     * Elements are added to the array in encounter order. The stream is consumed
     * completely by this operation.</p>
     *
     * <h3>TOML Context</h3>
     * <p>When serializing to TOML, ensure all elements are of the same type
     * (or all are tables) to comply with TOML's homogeneous array requirement.
     * The internal representation allows heterogeneous arrays, but serialization
     * may fail.</p>
     *
     * @param values a stream of nodes to include in the array; must not be {@code null};
     *               may be empty; {@code null} elements in the stream will be added as-is
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
     * <p>Returns the elements of an array node as a stream. This operation succeeds
     * only if the input is an {@link ArrayNode}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is an {@link ArrayNode} (including empty arrays)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not an {@link ArrayNode} (e.g., object, primitive, null)</li>
     * </ul>
     *
     * <p>The returned stream provides sequential access to array elements in order.
     * The stream is lazy and supports short-circuiting operations.</p>
     *
     * @param input the node to extract array elements from; must not be {@code null}
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
     *   <li>Input list is a {@link NullNode} (treated as empty array)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input list is not an array or null (e.g., object, primitive)</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original list is never modified. A deep copy
     * is created via {@link ArrayNode#deepCopy()} before the new element is appended.</p>
     *
     * <h3>TOML Context</h3>
     * <p>When building arrays for TOML serialization, ensure type consistency
     * by only appending elements of the same type as existing elements.</p>
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
     * <p>Creates a new empty object node. This is the canonical way to create an empty
     * map/table structure in the Jackson TOML format.</p>
     *
     * <h3>TOML Context</h3>
     * <p>Empty tables are valid TOML. When serialized as the root document, an empty
     * table produces an empty TOML file. As a nested table, it may be omitted or
     * represented as an empty inline table {@code {}} depending on mapper configuration.</p>
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
     * <p>Creates a new object node from a stream of key-value pairs. Keys must be
     * convertible to strings; entries with {@code null} keys are skipped.</p>
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
     * <h3>TOML Context</h3>
     * <p>TOML table keys can be bare (unquoted) if they contain only alphanumeric
     * characters, underscores, and dashes. Other keys must be quoted. The mapper
     * handles this automatically during serialization.</p>
     *
     * @param entries a stream of key-value pairs; must not be {@code null}; may be empty
     * @return a new {@link ObjectNode} containing all valid entries; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createMap(@NotNull final Stream<Pair<JsonNode, JsonNode>> entries) {
        Preconditions.checkNotNull(entries, "entries must not be null");
        final ObjectNode object = this.nodeFactory.objectNode();
        entries.forEach(pair -> {
            final JsonNode keyNode = pair.first();
            final JsonNode valueNode = pair.second();
            if (keyNode == null) {
                return;
            }
            final String key = keyNode.asText();
            object.set(key, valueNode != null ? valueNode : NullNode.getInstance());
        });
        return object;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the entries of an object node as a stream of key-value pairs. This
     * operation succeeds only if the input is an {@link ObjectNode}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input is an {@link ObjectNode} (including empty objects)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not an {@link ObjectNode} (e.g., array, primitive, null)</li>
     * </ul>
     *
     * <p>Each entry in the returned stream has:</p>
     * <ul>
     *   <li>Key: a {@link TextNode} containing the field name as a string</li>
     *   <li>Value: the field's {@link JsonNode} value</li>
     * </ul>
     *
     * @param input the node to extract object entries from; must not be {@code null}
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
     *   <li>Input map is a {@link NullNode} (treated as empty object)</li>
     *   <li>Key is a {@link TextNode} (textual node)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input map is not an object or null (e.g., array, primitive)</li>
     *   <li>Key is not a textual node</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original map is never modified. A deep copy
     * is created via {@link ObjectNode#deepCopy()} before the entry is added.</p>
     *
     * @param map   the object to add the entry to; must not be {@code null}; may be
     *              {@link NullNode} (treated as empty object)
     * @param key   the key for the entry; must not be {@code null}; must be a textual node
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
     *   <li>Either input may be a {@link NullNode} (treated as empty object)</li>
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
     *   <li>The merge is shallow; nested objects are not recursively merged</li>
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
     * <p>Retrieves a field value from an object node by key. Returns {@code null} if
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
     * <h3>TOML Context</h3>
     * <p>Field access uses the exact key string. TOML dotted keys (e.g., {@code a.b.c})
     * are expanded into nested tables during parsing, so use this method to access
     * each level individually.</p>
     *
     * @param input the node to retrieve the field from; must not be {@code null}
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
     * @param input    the node to set the field on; must not be {@code null}
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
     * @param input the node to remove the field from; must not be {@code null}
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
     * <p>Checks whether an object node contains a field with the specified key.</p>
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
     * @param input the node to check; must not be {@code null}
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
     *       creates an appropriate numeric node via {@link #createNumeric(Number)}</li>
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
     * <h3>TOML Context</h3>
     * <p>When converting data intended for TOML serialization, ensure the root element
     * is an object (table) and arrays contain homogeneous types. Null values will be
     * omitted during TOML serialization.</p>
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

        final DataResult<Stream<U>> listResult = sourceOps.getList(input);
        if (listResult.isSuccess()) {
            return createList(
                    listResult.result().orElseThrow()
                            .map(element -> convertTo(sourceOps, element))
            );
        }

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

        return empty();
    }

    /**
     * Returns a string representation of this {@code DynamicOps} instance.
     *
     * <p>This method returns a fixed string identifying the implementation, useful for
     * debugging, logging, and error messages. The string clearly identifies this as the
     * Jackson TOML implementation of {@code DynamicOps}.</p>
     *
     * @return the string {@code "JacksonTomlOps"}; never {@code null}
     */
    @Override
    public String toString() {
        return "JacksonTomlOps";
    }
}
