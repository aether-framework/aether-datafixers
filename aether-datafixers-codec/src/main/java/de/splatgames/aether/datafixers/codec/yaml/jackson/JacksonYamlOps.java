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

package de.splatgames.aether.datafixers.codec.yaml.jackson;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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
 * A {@link DynamicOps} implementation for Jackson's {@link JsonNode} using YAML format.
 *
 * <p>This class provides format-agnostic data manipulation capabilities for Jackson's
 * YAML tree model, enabling the Aether Datafixers system to read, write, and transform
 * YAML data without coupling application code to Jackson-specific APIs. It serves as the
 * bridge between the abstract data fixer operations and Jackson's concrete YAML model.</p>
 *
 * <h2>Design Pattern</h2>
 * <p>This class implements a hybrid Singleton/Factory pattern:</p>
 * <ul>
 *   <li>A default {@link #INSTANCE} singleton is provided for common use cases</li>
 *   <li>Custom instances can be created via {@link #JacksonYamlOps(YAMLMapper)} for
 *       specialized configurations</li>
 * </ul>
 * <p>This design balances convenience (singleton for default use) with flexibility
 * (custom instances for specific YAML configurations).</p>
 *
 * <h2>YAML-Specific Characteristics</h2>
 * <p>While this class uses Jackson's {@link JsonNode} internally (shared with JSON
 * operations), it is specifically designed for YAML data:</p>
 * <ul>
 *   <li>The {@link YAMLMapper} handles YAML-specific serialization features such as
 *       document markers ({@code ---}), block vs. flow styles, and multi-line strings</li>
 *   <li>YAML's native support for comments, anchors, and aliases is handled at the
 *       mapper level during parsing/serialization</li>
 *   <li>YAML's type inference (e.g., {@code yes/no} as booleans) is controlled by
 *       the mapper configuration</li>
 *   <li>Multi-document YAML files require special handling outside this class</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Encoding/Decoding with Default Instance</h3>
 * <pre>{@code
 * // Encode a value to YAML using a codec
 * DataResult<JsonNode> encoded = playerCodec.encodeStart(JacksonYamlOps.INSTANCE, player);
 *
 * // Decode YAML to a typed value
 * DataResult<Player> decoded = playerCodec.decode(JacksonYamlOps.INSTANCE, yamlNode);
 *
 * // Serialize to YAML string
 * String yamlString = JacksonYamlOps.INSTANCE.mapper().writeValueAsString(encoded.result().get());
 * }</pre>
 *
 * <h3>Custom YAMLMapper Configuration</h3>
 * <pre>{@code
 * // Create a custom YAMLMapper with specific settings
 * YAMLMapper customMapper = YAMLMapper.builder()
 *     .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
 *     .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
 *     .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
 *     .build();
 *
 * // Create ops instance with custom mapper
 * JacksonYamlOps customOps = new JacksonYamlOps(customMapper);
 *
 * // Use the custom ops for encoding
 * DataResult<JsonNode> result = codec.encodeStart(customOps, value);
 * }</pre>
 *
 * <h3>Creating Dynamic Wrappers</h3>
 * <pre>{@code
 * // Wrap existing YAML data for manipulation
 * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonYamlOps.INSTANCE, existingYaml);
 *
 * // Access nested fields
 * Optional<String> name = dynamic.get("player").get("name").asString();
 *
 * // Transform data
 * Dynamic<JsonNode> updated = dynamic.set("version", JacksonYamlOps.INSTANCE.createInt(2));
 * }</pre>
 *
 * <h3>Format Conversion</h3>
 * <pre>{@code
 * // Convert from Gson JSON to Jackson YAML
 * JsonNode yamlNode = JacksonYamlOps.INSTANCE.convertTo(GsonOps.INSTANCE, gsonElement);
 *
 * // Convert from Jackson JSON to Jackson YAML (different mappers)
 * JsonNode yamlNode = JacksonYamlOps.INSTANCE.convertTo(JacksonJsonOps.INSTANCE, jsonNode);
 *
 * // Convert from SnakeYAML to Jackson YAML
 * JsonNode jacksonYaml = JacksonYamlOps.INSTANCE.convertTo(SnakeYamlOps.INSTANCE, snakeYamlData);
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <p>The following table shows how Java/abstract types map to Jackson node types:</p>
 * <table border="1" cellpadding="5">
 *   <caption>Type Mapping between Java and Jackson YAML Nodes</caption>
 *   <tr><th>Java Type</th><th>Jackson Node Type</th><th>Notes</th></tr>
 *   <tr><td>{@code boolean}</td><td>{@link BooleanNode}</td><td>Created via {@link BooleanNode#valueOf(boolean)}</td></tr>
 *   <tr><td>{@code int}</td><td>{@link IntNode}</td><td>Created via {@link IntNode#valueOf(int)}</td></tr>
 *   <tr><td>{@code long}</td><td>{@link LongNode}</td><td>Created via {@link LongNode#valueOf(long)}</td></tr>
 *   <tr><td>{@code float}</td><td>{@link FloatNode}</td><td>Created via {@link FloatNode#valueOf(float)}</td></tr>
 *   <tr><td>{@code double}</td><td>{@link DoubleNode}</td><td>Created via {@link DoubleNode#valueOf(double)}</td></tr>
 *   <tr><td>{@code short}</td><td>{@link ShortNode}</td><td>Created via {@link ShortNode#valueOf(short)}</td></tr>
 *   <tr><td>{@code byte}</td><td>{@link ShortNode}</td><td>No dedicated ByteNode; stored as short</td></tr>
 *   <tr><td>{@code String}</td><td>{@link TextNode}</td><td>Created via {@link TextNode#valueOf(String)}</td></tr>
 *   <tr><td>{@code List/Stream}</td><td>{@link ArrayNode}</td><td>Created via {@link JsonNodeFactory#arrayNode()}</td></tr>
 *   <tr><td>{@code Map}</td><td>{@link ObjectNode}</td><td>Keys must be strings; created via {@link JsonNodeFactory#objectNode()}</td></tr>
 *   <tr><td>{@code null/empty}</td><td>{@link NullNode}</td><td>Singleton {@link NullNode#getInstance()}</td></tr>
 * </table>
 *
 * <h2>Immutability Contract</h2>
 * <p>All modification operations in this class preserve immutability of input data:</p>
 * <ul>
 *   <li>{@link #set(JsonNode, String, JsonNode)} creates a deep copy before modification</li>
 *   <li>{@link #remove(JsonNode, String)} creates a deep copy before removal</li>
 *   <li>{@link #mergeToMap(JsonNode, JsonNode, JsonNode)} creates a deep copy of the target map</li>
 *   <li>{@link #mergeToMap(JsonNode, JsonNode)} creates a deep copy of the first map before merging</li>
 *   <li>{@link #mergeToList(JsonNode, JsonNode)} creates a deep copy of the target list</li>
 * </ul>
 * <p>This ensures that original data structures are never modified, enabling safe concurrent
 * access and functional programming patterns. Deep copies are created using Jackson's
 * {@link JsonNode#deepCopy()} method, which recursively copies all nested structures.</p>
 *
 * <h2>Error Handling</h2>
 * <p>Operations that may fail return {@link DataResult} instead of throwing exceptions:</p>
 * <ul>
 *   <li>Type mismatches result in {@link DataResult#error(String)} with descriptive messages
 *       that include the actual input value for debugging</li>
 *   <li>Successful operations return {@link DataResult#success(Object)}</li>
 *   <li>Callers should check {@link DataResult#isSuccess()} or use {@link DataResult#result()}</li>
 *   <li>Error messages follow the pattern: "Not a [expected type]: [actual value]"</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Instances of this class are thread-safe under the following conditions:</p>
 * <ul>
 *   <li>The {@link #INSTANCE} singleton can be safely shared across threads</li>
 *   <li>Custom instances are thread-safe if the underlying {@link YAMLMapper} is thread-safe
 *       (Jackson mappers are thread-safe after configuration is complete)</li>
 *   <li>All operations are side-effect free on this class's state</li>
 *   <li>The {@link JsonNodeFactory} obtained from the mapper is used for all node creation</li>
 * </ul>
 * <p><strong>Note:</strong> While the ops instance itself is thread-safe, the {@link JsonNode}
 * instances being manipulated may not be. Always ensure that input data is not being
 * modified concurrently by other threads.</p>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Deep copy operations have O(n) complexity where n is the total structure size
 *       (all nested elements are copied)</li>
 *   <li>Prefer batch modifications via {@link #createMap(Stream)} over repeated {@link #set} calls
 *       to minimize deep copy overhead</li>
 *   <li>Stream-based operations ({@link #getList}, {@link #getMapEntries}) are lazy and
 *       support short-circuiting</li>
 *   <li>The {@link JsonNodeFactory} is cached from the mapper to avoid repeated lookups</li>
 *   <li>Singleton node types ({@link NullNode}, {@link BooleanNode}) are reused when possible</li>
 *   <li>For high-throughput scenarios, consider reusing a single custom {@link YAMLMapper}
 *       instance across all operations</li>
 * </ul>
 *
 * <h2>Comparison with JacksonJsonOps</h2>
 * <p>This class shares the same internal representation ({@link JsonNode}) as
 * {@code JacksonJsonOps}, but differs in serialization behavior:</p>
 * <ul>
 *   <li>Uses {@link YAMLMapper} instead of {@code ObjectMapper} for YAML-specific output</li>
 *   <li>Supports YAML features like block scalars, anchors, and flow styles at the mapper level</li>
 *   <li>Data can be freely converted between the two formats since both use {@link JsonNode}</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see JsonNode
 * @see YAMLMapper
 * @see DataResult
 * @since 0.4.0
 */
public final class JacksonYamlOps implements DynamicOps<JsonNode> {

    /**
     * The singleton instance of {@code JacksonYamlOps} with a default {@link YAMLMapper}.
     *
     * <p>This instance uses a standard {@link YAMLMapper} with default settings, which includes:</p>
     * <ul>
     *   <li>Document start markers ({@code ---}) enabled by default</li>
     *   <li>Default indentation (2 spaces)</li>
     *   <li>Standard YAML 1.1 compatibility mode</li>
     * </ul>
     *
     * <p>This instance should be used for most YAML operations throughout the application.
     * It is immutable after construction and thread-safe, making it suitable for use in any
     * context including multi-threaded environments and dependency injection containers.</p>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Direct usage
     * JsonNode yaml = JacksonYamlOps.INSTANCE.createString("hello");
     *
     * // With Dynamic
     * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonYamlOps.INSTANCE, yaml);
     *
     * // With Codecs
     * DataResult<JsonNode> result = myCodec.encodeStart(JacksonYamlOps.INSTANCE, value);
     *
     * // Serialize to YAML string
     * String yamlString = JacksonYamlOps.INSTANCE.mapper().writeValueAsString(yaml);
     * }</pre>
     *
     * <p>For custom YAML configurations (e.g., disabling document markers, custom indentation),
     * create a new instance using {@link #JacksonYamlOps(YAMLMapper)}.</p>
     */
    public static final JacksonYamlOps INSTANCE = new JacksonYamlOps(new YAMLMapper());

    /**
     * The YAML mapper used for node creation and YAML-specific configuration.
     *
     * <p>This mapper provides:</p>
     * <ul>
     *   <li>The {@link JsonNodeFactory} for creating all node instances</li>
     *   <li>YAML serialization configuration when writing to strings</li>
     *   <li>YAML parsing configuration when reading from strings</li>
     * </ul>
     *
     * <p>The mapper is immutable after construction of this ops instance. Modifying
     * the mapper's configuration after construction may lead to undefined behavior.</p>
     */
    private final YAMLMapper mapper;

    /**
     * The node factory used for creating all {@link JsonNode} instances.
     *
     * <p>This factory is obtained from the {@link YAMLMapper} during construction and
     * cached for performance. All node creation operations use this factory to ensure
     * consistency with the mapper's configuration.</p>
     *
     * <p>The factory determines node creation behavior, including:</p>
     * <ul>
     *   <li>Decimal precision for floating-point numbers</li>
     *   <li>BigDecimal vs. Double handling</li>
     *   <li>Potential custom node types</li>
     * </ul>
     */
    private final JsonNodeFactory nodeFactory;

    /**
     * Creates a new {@code JacksonYamlOps} instance with the specified {@link YAMLMapper}.
     *
     * <p>The mapper's {@link JsonNodeFactory} will be used for creating all nodes. This
     * allows customization of node creation behavior and YAML-specific features such as:</p>
     * <ul>
     *   <li>Document start/end markers configuration</li>
     *   <li>Indentation and flow style preferences</li>
     *   <li>Quote minimization settings</li>
     *   <li>Literal block style for multi-line strings</li>
     * </ul>
     *
     * <h3>Example: Custom Configuration</h3>
     * <pre>{@code
     * YAMLMapper mapper = YAMLMapper.builder()
     *     .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
     *     .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
     *     .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
     *     .build();
     *
     * JacksonYamlOps ops = new JacksonYamlOps(mapper);
     * }</pre>
     *
     * <p><strong>Thread Safety:</strong> The mapper should be fully configured before
     * being passed to this constructor. Modifying the mapper after construction may
     * lead to inconsistent behavior.</p>
     *
     * @param mapper the YAML mapper to use for node creation and serialization;
     *               must not be {@code null}
     */
    public JacksonYamlOps(@NotNull final YAMLMapper mapper) {
        this.mapper = mapper;
        this.nodeFactory = mapper.getNodeFactory();
    }

    /**
     * Returns the {@link YAMLMapper} used by this instance.
     *
     * <p>This can be used for additional Jackson operations outside the {@link DynamicOps}
     * interface, such as:</p>
     * <ul>
     *   <li>Serialization to YAML strings: {@code mapper().writeValueAsString(node)}</li>
     *   <li>Parsing from YAML strings: {@code mapper().readTree(yamlString)}</li>
     *   <li>Custom serialization with specific output settings</li>
     *   <li>Reading from files or streams</li>
     * </ul>
     *
     * <h3>Example: Serialization to String</h3>
     * <pre>{@code
     * JsonNode node = JacksonYamlOps.INSTANCE.createMap(Stream.of(
     *     Pair.of(JacksonYamlOps.INSTANCE.createString("name"),
     *             JacksonYamlOps.INSTANCE.createString("Alice")),
     *     Pair.of(JacksonYamlOps.INSTANCE.createString("age"),
     *             JacksonYamlOps.INSTANCE.createInt(30))
     * ));
     *
     * String yaml = JacksonYamlOps.INSTANCE.mapper().writeValueAsString(node);
     * // Output:
     * // ---
     * // name: "Alice"
     * // age: 30
     * }</pre>
     *
     * <p><strong>Warning:</strong> Do not modify the returned mapper's configuration
     * as this may affect the behavior of this ops instance in unpredictable ways.</p>
     *
     * @return the YAML mapper used by this instance; never {@code null}
     */
    public YAMLMapper mapper() {
        return this.mapper;
    }

    // ==================== Empty/Null Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Returns the YAML null value, which represents the absence of a value. This is
     * the canonical "empty" value for the Jackson YAML format and is used when:</p>
     * <ul>
     *   <li>A field has no value</li>
     *   <li>A conversion cannot determine the appropriate type</li>
     *   <li>An optional value is absent</li>
     *   <li>Explicit null representation is needed</li>
     * </ul>
     *
     * <p>In YAML, this corresponds to various null representations including:
     * {@code null}, {@code ~}, or an empty value.</p>
     *
     * @return {@link NullNode#getInstance()}, the singleton YAML null value; never {@code null}
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
     * <p>Determines whether the given node represents a map/object structure. In YAML/Jackson
     * terminology, this corresponds to an {@link ObjectNode} which contains key-value pairs
     * (YAML mappings) where keys are always strings.</p>
     *
     * <p>In YAML, maps are typically represented as:</p>
     * <pre>{@code
     * key1: value1
     * key2: value2
     * }</pre>
     * or in flow style: {@code {key1: value1, key2: value2}}
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ObjectNode}, {@code false} otherwise
     *         (including for {@link NullNode}, {@link ArrayNode}, and primitive nodes)
     */
    @Override
    public boolean isMap(@NotNull final JsonNode value) {
        return value.isObject();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a list/array structure. In YAML/Jackson
     * terminology, this corresponds to an {@link ArrayNode} which contains an ordered
     * sequence of elements (YAML sequences).</p>
     *
     * <p>In YAML, lists are typically represented as:</p>
     * <pre>{@code
     * - item1
     * - item2
     * - item3
     * }</pre>
     * or in flow style: {@code [item1, item2, item3]}
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ArrayNode}, {@code false} otherwise
     *         (including for {@link NullNode}, {@link ObjectNode}, and primitive nodes)
     */
    @Override
    public boolean isList(@NotNull final JsonNode value) {
        return value.isArray();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a string value. In Jackson, strings
     * are stored as {@link TextNode} instances.</p>
     *
     * <p>In YAML, strings can be represented in multiple ways:</p>
     * <ul>
     *   <li>Unquoted: {@code hello world}</li>
     *   <li>Single-quoted: {@code 'hello world'}</li>
     *   <li>Double-quoted: {@code "hello world"}</li>
     *   <li>Literal block scalar: {@code |} followed by indented text</li>
     *   <li>Folded block scalar: {@code >} followed by indented text</li>
     * </ul>
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is a {@link TextNode}, {@code false} otherwise
     *         (including for numeric, boolean, and other node types)
     */
    @Override
    public boolean isString(@NotNull final JsonNode value) {
        return value.isTextual();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a numeric value. In Jackson, numeric
     * values can be stored as various node types including {@link IntNode}, {@link LongNode},
     * {@link FloatNode}, {@link DoubleNode}, and {@link ShortNode}.</p>
     *
     * <p>In YAML, numbers are represented directly:</p>
     * <ul>
     *   <li>Integers: {@code 42}, {@code -17}, {@code 0}</li>
     *   <li>Floating-point: {@code 3.14}, {@code -2.5e10}</li>
     *   <li>Special values: {@code .inf}, {@code -.inf}, {@code .nan}</li>
     *   <li>Hexadecimal: {@code 0xFF}</li>
     *   <li>Octal: {@code 0o17}</li>
     * </ul>
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is a numeric node, {@code false} otherwise
     *         (including for string, boolean, and other node types)
     */
    @Override
    public boolean isNumber(@NotNull final JsonNode value) {
        return value.isNumber();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a boolean value. In Jackson, booleans
     * are stored as {@link BooleanNode} instances.</p>
     *
     * <p>In YAML, booleans can be represented in multiple ways (depending on YAML version
     * and parser configuration):</p>
     * <ul>
     *   <li>Standard: {@code true}, {@code false}</li>
     *   <li>YAML 1.1 also accepts: {@code yes}, {@code no}, {@code on}, {@code off},
     *       {@code y}, {@code n} (case-insensitive)</li>
     * </ul>
     *
     * <p><strong>Note:</strong> The YAMLMapper configuration determines which string
     * representations are interpreted as booleans during parsing.</p>
     *
     * @param value the node to check; must not be {@code null}
     * @return {@code true} if the value is a {@link BooleanNode}, {@code false} otherwise
     *         (including for string, numeric, and other node types)
     */
    @Override
    public boolean isBoolean(@NotNull final JsonNode value) {
        return value.isBoolean();
    }

    // ==================== Primitive Creation Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new YAML string node from the given string value. The resulting
     * {@link TextNode} will return {@code true} for {@link JsonNode#isTextual()}.</p>
     *
     * <p>When serialized to YAML, the output format (quoted, unquoted, block scalar)
     * depends on the {@link YAMLMapper} configuration and the string content.</p>
     *
     * @param value the string value to wrap; must not be {@code null}
     * @return a new {@link TextNode} containing the string value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createString(@NotNull final String value) {
        return TextNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new YAML integer node from the given integer value. The resulting
     * {@link IntNode} will return {@code true} for {@link JsonNode#isNumber()} and
     * {@link JsonNode#isInt()}.</p>
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
     * <p>Creates a new YAML long node from the given long value. The resulting
     * {@link LongNode} will return {@code true} for {@link JsonNode#isNumber()} and
     * {@link JsonNode#isLong()}.</p>
     *
     * <p><strong>Note:</strong> Very large long values are preserved exactly in YAML,
     * unlike JSON where JavaScript parsers may lose precision.</p>
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
     * <p>Creates a new YAML float node from the given float value. The resulting
     * {@link FloatNode} will return {@code true} for {@link JsonNode#isNumber()} and
     * {@link JsonNode#isFloat()}.</p>
     *
     * <p><strong>Note:</strong> Special float values are represented in YAML as:</p>
     * <ul>
     *   <li>{@link Float#POSITIVE_INFINITY} maps to {@code .inf}</li>
     *   <li>{@link Float#NEGATIVE_INFINITY} maps to {@code -.inf}</li>
     *   <li>{@link Float#NaN} maps to {@code .nan}</li>
     * </ul>
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
     * <p>Creates a new YAML double node from the given double value. The resulting
     * {@link DoubleNode} will return {@code true} for {@link JsonNode#isNumber()} and
     * {@link JsonNode#isDouble()}.</p>
     *
     * <p><strong>Note:</strong> Special double values are represented in YAML as:</p>
     * <ul>
     *   <li>{@link Double#POSITIVE_INFINITY} maps to {@code .inf}</li>
     *   <li>{@link Double#NEGATIVE_INFINITY} maps to {@code -.inf}</li>
     *   <li>{@link Double#NaN} maps to {@code .nan}</li>
     * </ul>
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
     * <p>Creates a new YAML numeric node from the given byte value. Since Jackson does not
     * provide a dedicated {@code ByteNode}, the value is stored as a {@link ShortNode}.
     * The resulting node will return {@code true} for {@link JsonNode#isNumber()}.</p>
     *
     * <p><strong>Note:</strong> YAML does not have a dedicated byte type. The value is
     * serialized as a plain integer and type information may be lost during round-trips.</p>
     *
     * @param value the byte value to wrap
     * @return a new {@link ShortNode} containing the byte value; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createByte(final byte value) {
        return ShortNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new YAML short node from the given short value. The resulting
     * {@link ShortNode} will return {@code true} for {@link JsonNode#isNumber()} and
     * {@link JsonNode#isShort()}.</p>
     *
     * <p><strong>Note:</strong> YAML does not have a dedicated short type. The value is
     * serialized as a plain integer and type information may be lost during round-trips.</p>
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
     * <p>Creates a new YAML boolean node from the given boolean value. The resulting
     * {@link BooleanNode} will return {@code true} for {@link JsonNode#isBoolean()}.</p>
     *
     * <p>When serialized to YAML, boolean values are typically rendered as {@code true}
     * or {@code false}, though the exact representation may depend on the mapper
     * configuration.</p>
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
     * <p>Creates a new YAML numeric node from the given {@link Number} value. This method
     * inspects the runtime type of the number and creates the appropriate node type:</p>
     * <ul>
     *   <li>{@link Integer} creates an {@link IntNode}</li>
     *   <li>{@link Long} creates a {@link LongNode}</li>
     *   <li>{@link Float} creates a {@link FloatNode}</li>
     *   <li>{@link Double} creates a {@link DoubleNode}</li>
     *   <li>{@link Short} creates a {@link ShortNode}</li>
     *   <li>{@link Byte} creates a {@link ShortNode}</li>
     *   <li>Other {@link Number} types default to {@link DoubleNode}</li>
     * </ul>
     *
     * <p>This method preserves the numeric precision appropriate for the input type.</p>
     *
     * @param value the number value to wrap; must not be {@code null}
     * @return a numeric node containing the value with appropriate precision;
     *         never {@code null}
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

    // ==================== Primitive Reading Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the string value from a YAML node. This operation succeeds only if
     * the input is a {@link TextNode} containing a string value.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input node returns {@code true} for {@link JsonNode#isTextual()}</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a text node (e.g., number, boolean, array, object, null)</li>
     * </ul>
     *
     * @param input the YAML node to extract the string from; must not be {@code null}
     * @return a {@link DataResult} containing the string value on success, or an error
     *         message describing why extraction failed; never {@code null}
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
     * <p>Extracts the numeric value from a YAML node. This operation succeeds only if
     * the input is a numeric node.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input node returns {@code true} for {@link JsonNode#isNumber()}</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a numeric node (e.g., string, boolean, array, object, null)</li>
     * </ul>
     *
     * <p>The returned {@link Number} preserves the original numeric type where possible,
     * allowing callers to use type-specific accessors like {@link Number#intValue()} or
     * {@link Number#doubleValue()}.</p>
     *
     * @param input the YAML node to extract the number from; must not be {@code null}
     * @return a {@link DataResult} containing the number value on success, or an error
     *         message describing why extraction failed; never {@code null}
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
     * <p>Extracts the boolean value from a YAML node. This operation succeeds only if
     * the input is a {@link BooleanNode}.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input node returns {@code true} for {@link JsonNode#isBoolean()}</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a boolean node (e.g., string, number, array, object, null)</li>
     * </ul>
     *
     * @param input the YAML node to extract the boolean from; must not be {@code null}
     * @return a {@link DataResult} containing the boolean value on success, or an error
     *         message describing why extraction failed; never {@code null}
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
     * <p>Creates a new empty YAML array/sequence. This is the canonical way to create
     * an empty list structure in the Jackson YAML format.</p>
     *
     * <p>When serialized to YAML, an empty array is typically rendered as {@code []}.</p>
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
     * <p>Creates a new YAML array containing all elements from the provided stream.
     * Elements are added to the array in encounter order. The stream is consumed
     * completely by this operation.</p>
     *
     * <p><strong>Note:</strong> Elements in the stream should be valid {@link JsonNode}
     * instances. {@code null} elements may cause issues during serialization.</p>
     *
     * @param values a stream of nodes to include in the array; must not be {@code null};
     *               may be empty
     * @return a new {@link ArrayNode} containing all stream elements in order;
     *         never {@code null}
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
     * <p>Returns the elements of a YAML array as a stream. This operation succeeds only
     * if the input is an {@link ArrayNode}.</p>
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
     * The stream is created lazily using a {@link Spliterator} and does not support
     * parallel processing by default.</p>
     *
     * @param input the YAML node to extract array elements from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of array elements on success,
     *         or an error message if the input is not an array; never {@code null}
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
     * <p>Creates a new empty YAML object/mapping. This is the canonical way to create
     * an empty map structure in the Jackson YAML format.</p>
     *
     * <p>When serialized to YAML, an empty object is typically rendered as {@code {}}.</p>
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
     * <p>Creates a new YAML object from a stream of key-value pairs. Keys are converted
     * to strings using {@link JsonNode#asText()}; entries with {@code null} keys are
     * silently skipped.</p>
     *
     * <h3>Key Handling</h3>
     * <ul>
     *   <li>Keys are converted to strings via {@link JsonNode#asText()}</li>
     *   <li>Entries with {@code null} keys are silently skipped</li>
     *   <li>Duplicate keys result in the last value being retained</li>
     *   <li>YAML supports complex keys, but this implementation requires string keys</li>
     * </ul>
     *
     * <h3>Value Handling</h3>
     * <ul>
     *   <li>{@code null} values are converted to {@link NullNode}</li>
     *   <li>All other values are added as-is</li>
     * </ul>
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
     * <p>Returns the entries of a YAML object as a stream of key-value pairs. This
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
     * <p>The stream is created lazily and does not support parallel processing by default.</p>
     *
     * @param input the YAML node to extract object entries from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of key-value pairs on success,
     *         or an error message if the input is not an object; never {@code null}
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
     * <p>Creates a new object by adding or updating a key-value pair in an existing map.
     * This operation creates a deep copy of the input map to preserve immutability.</p>
     *
     * <h3>Success Conditions</h3>
     * <ul>
     *   <li>Input map is an {@link ObjectNode}</li>
     *   <li>Input map is a {@link NullNode} (treated as empty object)</li>
     *   <li>Key is a {@link TextNode} (string)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input map is not an object or null (e.g., array, primitive)</li>
     *   <li>Key is not a text node (string)</li>
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
     *   <li>Order depends on the order of entries in the input objects</li>
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
     * <p>Retrieves a field value from a YAML object by key. Returns {@code null} if
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
     * @param input the YAML node to retrieve the field from; must not be {@code null}
     * @param key   the field name to retrieve; must not be {@code null}
     * @return the field value if found, or {@code null} if not found or input is not an object
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
     * @param input    the YAML node to set the field on; must not be {@code null}
     * @param key      the field name to set; must not be {@code null}
     * @param newValue the value to set; must not be {@code null}
     * @return a new {@link ObjectNode} with the field set; never {@code null}
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
     * @param input the YAML node to remove the field from; must not be {@code null}
     * @param key   the field name to remove; must not be {@code null}
     * @return a {@link JsonNode} with the field removed (or unchanged if not applicable);
     *         never {@code null}
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
     * <p>Checks whether a YAML object contains a field with the specified key.</p>
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
     * @param input the YAML node to check; must not be {@code null}
     * @param key   the field name to check for; must not be {@code null}
     * @return {@code true} if the input is an object containing the key, {@code false} otherwise
     */
    @Override
    public boolean has(@NotNull final JsonNode input,
                       @NotNull final String key) {
        if (!input.isObject()) {
            return false;
        }
        return input.has(key);
    }

    // ==================== Conversion Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Converts data from another {@link DynamicOps} format to Jackson {@link JsonNode}
     * for YAML representation. This method recursively converts all nested structures,
     * handling primitives, lists, and maps appropriately.</p>
     *
     * <h3>Conversion Process</h3>
     * <p>The conversion attempts to identify the input type in the following order:</p>
     * <ol>
     *   <li><strong>Boolean:</strong> If {@link DynamicOps#getBooleanValue} succeeds,
     *       creates a {@link BooleanNode} with the boolean value</li>
     *   <li><strong>Number:</strong> If {@link DynamicOps#getNumberValue} succeeds,
     *       creates an appropriate numeric node based on the number type</li>
     *   <li><strong>String:</strong> If {@link DynamicOps#getStringValue} succeeds,
     *       creates a {@link TextNode} with the string value</li>
     *   <li><strong>List:</strong> If {@link DynamicOps#getList} succeeds,
     *       creates an {@link ArrayNode} with recursively converted elements</li>
     *   <li><strong>Map:</strong> If {@link DynamicOps#getMapEntries} succeeds,
     *       creates an {@link ObjectNode} with recursively converted entries</li>
     *   <li><strong>Fallback:</strong> Returns {@link NullNode} if no type matches</li>
     * </ol>
     *
     * <h3>Edge Cases</h3>
     * <ul>
     *   <li>Map entries with {@code null} keys are skipped</li>
     *   <li>Map entries with {@code null} values are converted to {@link NullNode}</li>
     *   <li>Empty collections are preserved as empty arrays/objects</li>
     *   <li>Boolean check is performed before number check to avoid ambiguity with
     *       numeric boolean representations (0/1)</li>
     * </ul>
     *
     * @param sourceOps the {@link DynamicOps} instance for the source format; must not be
     *                  {@code null}
     * @param input     the value to convert from the source format; must not be {@code null}
     * @param <U>       the type parameter of the source format
     * @return the converted {@link JsonNode}; never {@code null}; returns
     *         {@link NullNode} if conversion is not possible
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
     * Returns a string representation of this {@code DynamicOps} instance.
     *
     * <p>This method returns a fixed string identifying the implementation, useful for
     * debugging, logging, and error messages. It does not include details about the
     * configured {@link YAMLMapper}.</p>
     *
     * @return the string {@code "JacksonYamlOps"}; never {@code null}
     */
    @Override
    public String toString() {
        return "JacksonYamlOps";
    }
}
