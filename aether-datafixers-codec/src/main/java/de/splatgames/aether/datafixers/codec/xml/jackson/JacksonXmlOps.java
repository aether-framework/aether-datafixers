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

package de.splatgames.aether.datafixers.codec.xml.jackson;

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
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
 * A {@link DynamicOps} implementation for Jackson's {@link JsonNode} using XML format via
 * {@link XmlMapper}.
 *
 * <p>This class provides format-agnostic data manipulation capabilities for Jackson's XML
 * tree model, enabling the Aether Datafixers system to read, write, and transform XML data
 * without coupling application code to Jackson-specific APIs. It serves as the bridge between
 * the abstract data fixer operations and Jackson's XML-backed node model.</p>
 *
 * <h2>Design Pattern</h2>
 * <p>This class implements a hybrid Singleton/Factory pattern:</p>
 * <ul>
 *   <li>Use {@link #INSTANCE} for default XML configuration (recommended for most use cases)</li>
 *   <li>Use the {@link #JacksonXmlOps(XmlMapper)} constructor for custom XML configurations</li>
 * </ul>
 * <p>The singleton instance is appropriate when:</p>
 * <ul>
 *   <li>Default XML serialization settings are acceptable</li>
 *   <li>No special namespace handling is required</li>
 *   <li>Standard XML element naming conventions are used</li>
 * </ul>
 * <p>Custom instances are appropriate when:</p>
 * <ul>
 *   <li>Custom serialization features are needed (e.g., {@code defaultUseWrapper(false)})</li>
 *   <li>XML namespace configuration is required</li>
 *   <li>Specific XML formatting options are needed</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Encoding/Decoding</h3>
 * <pre>{@code
 * // Encode a value to XML using a codec
 * DataResult<JsonNode> encoded = playerCodec.encodeStart(JacksonXmlOps.INSTANCE, player);
 *
 * // Decode XML to a typed value
 * DataResult<Player> decoded = playerCodec.decode(JacksonXmlOps.INSTANCE, xmlNode);
 *
 * // Serialize to XML string
 * String xmlString = JacksonXmlOps.INSTANCE.mapper().writeValueAsString(encoded.result().get());
 * }</pre>
 *
 * <h3>Creating Dynamic Wrappers</h3>
 * <pre>{@code
 * // Parse XML string to JsonNode
 * JsonNode xmlNode = JacksonXmlOps.INSTANCE.mapper().readTree(xmlString);
 *
 * // Wrap existing XML data for manipulation
 * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, xmlNode);
 *
 * // Access nested elements
 * Optional<String> name = dynamic.get("player").get("name").asString();
 *
 * // Transform data
 * Dynamic<JsonNode> updated = dynamic.set("version", JacksonXmlOps.INSTANCE.createInt(2));
 * }</pre>
 *
 * <h3>Custom XmlMapper Configuration</h3>
 * <pre>{@code
 * // Create custom XmlMapper with specific settings
 * XmlMapper customMapper = XmlMapper.builder()
 *     .defaultUseWrapper(false)
 *     .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
 *     .build();
 *
 * // Create ops instance with custom mapper
 * JacksonXmlOps customOps = new JacksonXmlOps(customMapper);
 *
 * // Use for XML-specific operations
 * DataResult<JsonNode> result = myCodec.encodeStart(customOps, value);
 * }</pre>
 *
 * <h3>Format Conversion</h3>
 * <pre>{@code
 * // Convert from Gson JSON to Jackson XML
 * JsonNode xmlNode = JacksonXmlOps.INSTANCE.convertTo(GsonOps.INSTANCE, gsonElement);
 *
 * // Convert from Jackson JSON to Jackson XML
 * JsonNode xmlFromJson = JacksonXmlOps.INSTANCE.convertTo(JacksonJsonOps.INSTANCE, jsonNode);
 * }</pre>
 *
 * <h2>XML-Specific Considerations</h2>
 * <p>XML has fundamental structural differences compared to JSON that affect how data is
 * represented and manipulated:</p>
 *
 * <h3>Root Element Requirement</h3>
 * <p>XML documents must have exactly one root element. When serializing to XML string format,
 * ensure your data structure has a single root or wrap it appropriately:</p>
 * <pre>{@code
 * // Valid: Single root element
 * <root>
 *   <name>value</name>
 *   <count>42</count>
 * </root>
 *
 * // Invalid: Multiple root elements
 * <name>value</name>
 * <count>42</count>
 * }</pre>
 *
 * <h3>Attributes vs Elements</h3>
 * <p>XML distinguishes between attributes and child elements. Jackson's {@link XmlMapper}
 * configuration determines how object properties are serialized. By default, properties
 * become child elements, but this can be customized with Jackson annotations or mapper
 * configuration.</p>
 *
 * <h3>Arrays and Repeated Elements</h3>
 * <p>XML has no native array type. Arrays are typically represented as repeated elements
 * with the same name or wrapped in a container element. The {@link XmlMapper}'s
 * {@code defaultUseWrapper} setting controls this behavior:</p>
 * <pre>{@code
 * // With wrapper (defaultUseWrapper = true)
 * <items>
 *   <item>value1</item>
 *   <item>value2</item>
 * </items>
 *
 * // Without wrapper (defaultUseWrapper = false)
 * <item>value1</item>
 * <item>value2</item>
 * }</pre>
 *
 * <h3>Mixed Content</h3>
 * <p>XML allows mixed content (text and elements interleaved), which has no direct JSON
 * equivalent. Jackson handles this through special property naming conventions, but
 * complex mixed content scenarios may require custom handling.</p>
 *
 * <h2>Type Mapping</h2>
 * <p>The following table shows how Java/abstract types map to Jackson XML node types:</p>
 * <table border="1" cellpadding="5">
 *   <caption>Type Mapping between Java and Jackson XML Nodes</caption>
 *   <tr><th>Java Type</th><th>Jackson Node Type</th><th>XML Representation</th></tr>
 *   <tr><td>{@code boolean}</td><td>{@link BooleanNode}</td><td>{@code <field>true</field>}</td></tr>
 *   <tr><td>{@code int}</td><td>{@link IntNode}</td><td>{@code <field>42</field>}</td></tr>
 *   <tr><td>{@code long}</td><td>{@link LongNode}</td><td>{@code <field>9223372036854775807</field>}</td></tr>
 *   <tr><td>{@code float}</td><td>{@link FloatNode}</td><td>{@code <field>3.14</field>}</td></tr>
 *   <tr><td>{@code double}</td><td>{@link DoubleNode}</td><td>{@code <field>3.141592653589793</field>}</td></tr>
 *   <tr><td>{@code byte, short}</td><td>{@link ShortNode}</td><td>{@code <field>127</field>}</td></tr>
 *   <tr><td>{@code String}</td><td>{@link TextNode}</td><td>{@code <field>text</field>}</td></tr>
 *   <tr><td>{@code List/Stream}</td><td>{@link ArrayNode}</td><td>Repeated elements or wrapped array</td></tr>
 *   <tr><td>{@code Map/Object}</td><td>{@link ObjectNode}</td><td>Element with child elements</td></tr>
 *   <tr><td>{@code null/empty}</td><td>{@link NullNode}</td><td>Empty element or omitted</td></tr>
 * </table>
 *
 * <h2>Immutability Contract</h2>
 * <p>All modification operations in this class preserve immutability of input data:</p>
 * <ul>
 *   <li>{@link #set(JsonNode, String, JsonNode)} creates a deep copy before modification</li>
 *   <li>{@link #remove(JsonNode, String)} creates a deep copy before removal</li>
 *   <li>{@link #mergeToMap(JsonNode, JsonNode, JsonNode)} creates a deep copy of the target map</li>
 *   <li>{@link #mergeToList(JsonNode, JsonNode)} creates a deep copy of the target list</li>
 * </ul>
 * <p>Deep copies are performed using Jackson's {@code deepCopy()} methods on {@link ObjectNode}
 * and {@link ArrayNode}, ensuring that all nested structures are also copied. This ensures that
 * original data structures are never modified, enabling safe concurrent access and functional
 * programming patterns.</p>
 *
 * <h2>Error Handling</h2>
 * <p>Operations that may fail return {@link DataResult} instead of throwing exceptions:</p>
 * <ul>
 *   <li>Type mismatches result in {@link DataResult#error(String)} with descriptive messages</li>
 *   <li>Successful operations return {@link DataResult#success(Object)}</li>
 *   <li>Callers should check {@link DataResult#isSuccess()} or use {@link DataResult#result()}</li>
 * </ul>
 * <p>Error messages include the problematic value's string representation to aid debugging.
 * For example: {@code "Not an array: {\"name\":\"value\"}"}</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Thread safety depends on the {@link XmlMapper} configuration:</p>
 * <ul>
 *   <li>The {@link #INSTANCE} singleton is thread-safe for read operations</li>
 *   <li>Custom instances are thread-safe if their {@link XmlMapper} is not modified after
 *       construction</li>
 *   <li>All data manipulation operations are stateless and side-effect free</li>
 *   <li>The underlying {@link JsonNodeFactory} is thread-safe</li>
 * </ul>
 * <p><strong>Important:</strong> {@link XmlMapper} instances are thread-safe for parsing and
 * serialization after configuration is complete. Do not modify the mapper configuration after
 * passing it to this constructor.</p>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Deep copy operations have O(n) complexity where n is the total number of nodes in the
 *       structure</li>
 *   <li>Prefer batch modifications via {@link #createMap(Stream)} over repeated {@link #set}
 *       calls to minimize copying overhead</li>
 *   <li>Stream-based operations are lazy and support short-circuiting</li>
 *   <li>The {@link JsonNodeFactory} is cached per instance to avoid repeated lookups</li>
 *   <li>For high-throughput XML processing, consider reusing a single {@code JacksonXmlOps}
 *       instance rather than creating new ones</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see JsonNode
 * @see XmlMapper
 * @see DataResult
 * @since 0.4.0
 */
public final class JacksonXmlOps implements DynamicOps<JsonNode> {

    /**
     * The singleton instance of {@code JacksonXmlOps} with a default {@link XmlMapper}.
     *
     * <p>This instance should be used for most XML operations throughout the application.
     * It uses a standard {@link XmlMapper} with default settings, which is suitable for
     * typical XML processing scenarios.</p>
     *
     * <h3>Default Configuration</h3>
     * <p>The default {@link XmlMapper} uses:</p>
     * <ul>
     *   <li>Standard XML element naming (property names become element names)</li>
     *   <li>Wrapper elements for arrays ({@code defaultUseWrapper = true})</li>
     *   <li>No XML declaration by default</li>
     *   <li>UTF-8 encoding</li>
     * </ul>
     *
     * <h3>Usage</h3>
     * <pre>{@code
     * // Direct usage
     * JsonNode xml = JacksonXmlOps.INSTANCE.createString("hello");
     *
     * // With Dynamic
     * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, xmlNode);
     *
     * // With Codecs
     * DataResult<JsonNode> result = myCodec.encodeStart(JacksonXmlOps.INSTANCE, value);
     *
     * // Access the underlying mapper for serialization
     * String xmlString = JacksonXmlOps.INSTANCE.mapper().writeValueAsString(node);
     * }</pre>
     *
     * <p>For custom XML configuration (namespaces, formatting, etc.), create a new instance
     * using {@link #JacksonXmlOps(XmlMapper)} with a configured {@link XmlMapper}.</p>
     */
    public static final JacksonXmlOps INSTANCE = new JacksonXmlOps(new XmlMapper());

    /**
     * The XML mapper used for node creation and XML-specific operations.
     *
     * <p>This mapper provides the {@link JsonNodeFactory} used for creating all node instances
     * and can be accessed via {@link #mapper()} for additional XML operations such as
     * serialization to/from XML strings.</p>
     *
     * <p>The mapper should not be modified after construction to ensure thread safety.</p>
     */
    private final XmlMapper mapper;

    /**
     * The node factory used for creating all JSON node instances.
     *
     * <p>This factory is obtained from the {@link XmlMapper} at construction time and cached
     * to avoid repeated lookups. It is used by all node creation methods to ensure consistency
     * with the mapper's configuration.</p>
     */
    private final JsonNodeFactory nodeFactory;

    /**
     * Creates a new {@code JacksonXmlOps} instance with the specified {@link XmlMapper}.
     *
     * <p>Use this constructor when you need custom XML configuration, such as:</p>
     * <ul>
     *   <li>Custom namespace handling</li>
     *   <li>Specific XML formatting options</li>
     *   <li>Modified array wrapping behavior</li>
     *   <li>Custom serialization features</li>
     * </ul>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * XmlMapper customMapper = XmlMapper.builder()
     *     .defaultUseWrapper(false)
     *     .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
     *     .configure(SerializationFeature.INDENT_OUTPUT, true)
     *     .build();
     *
     * JacksonXmlOps customOps = new JacksonXmlOps(customMapper);
     * }</pre>
     *
     * <p><strong>Thread Safety:</strong> The provided mapper should not be modified after
     * being passed to this constructor. {@link XmlMapper} instances are thread-safe for
     * read operations after configuration is complete.</p>
     *
     * @param mapper the XML mapper to use for node creation and XML operations;
     *               must not be {@code null}
     * @throws NullPointerException if {@code mapper} is {@code null} (implicit via field access)
     */
    public JacksonXmlOps(@NotNull final XmlMapper mapper) {
        Preconditions.checkNotNull(mapper, "mapper must not be null");
        this.mapper = mapper;
        this.nodeFactory = mapper.getNodeFactory();
    }

    /**
     * Returns the {@link XmlMapper} used by this instance.
     *
     * <p>This method provides access to the underlying mapper for operations outside the
     * {@link DynamicOps} interface, such as:</p>
     * <ul>
     *   <li>Serializing nodes to XML strings: {@code mapper().writeValueAsString(node)}</li>
     *   <li>Parsing XML strings to nodes: {@code mapper().readTree(xmlString)}</li>
     *   <li>Configuring XML-specific serialization features</li>
     *   <li>Working with XML namespaces</li>
     * </ul>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * // Serialize to XML string
     * String xml = JacksonXmlOps.INSTANCE.mapper().writeValueAsString(node);
     *
     * // Parse XML string
     * JsonNode parsed = JacksonXmlOps.INSTANCE.mapper().readTree(xmlString);
     *
     * // Pretty print
     * String prettyXml = JacksonXmlOps.INSTANCE.mapper()
     *     .writerWithDefaultPrettyPrinter()
     *     .writeValueAsString(node);
     * }</pre>
     *
     * <p><strong>Warning:</strong> Do not modify the returned mapper's configuration if the
     * instance is shared across threads. Configuration changes after construction may cause
     * thread safety issues.</p>
     *
     * @return the XML mapper used by this instance; never {@code null}
     */
    public XmlMapper mapper() {
        return this.mapper;
    }

    // ==================== Empty/Null Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Returns the JSON/XML null value, which represents the absence of a value. This is
     * the canonical "empty" value for the Jackson XML format and is used when:</p>
     * <ul>
     *   <li>A field has no value</li>
     *   <li>A conversion cannot determine the appropriate type</li>
     *   <li>An optional value is absent</li>
     * </ul>
     *
     * <p><strong>XML Representation:</strong> In XML output, null values may be represented
     * as empty elements, omitted entirely, or with {@code xsi:nil="true"} depending on the
     * {@link XmlMapper} configuration and serialization context.</p>
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
     * <p>Determines whether the given node represents a map/object structure. In Jackson
     * terminology, this corresponds to an {@link ObjectNode} which contains key-value pairs
     * where keys are always strings. In XML, this represents an element with child elements.</p>
     *
     * <h3>XML Context</h3>
     * <p>In XML, object nodes correspond to elements containing child elements:</p>
     * <pre>{@code
     * <parent>
     *   <child1>value1</child1>
     *   <child2>value2</child2>
     * </parent>
     * }</pre>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ObjectNode}, {@code false} otherwise
     *         (including for {@link NullNode}, {@link ArrayNode}, and primitive nodes)
     */
    @Override
    public boolean isMap(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isObject();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a list/array structure. In Jackson
     * terminology, this corresponds to an {@link ArrayNode} which contains an ordered
     * sequence of elements.</p>
     *
     * <h3>XML Context</h3>
     * <p>XML has no native array type. Arrays are represented as repeated elements or wrapped
     * in container elements. When parsed, Jackson's {@link XmlMapper} converts these to
     * {@link ArrayNode} instances:</p>
     * <pre>{@code
     * <!-- Wrapped array (typical representation) -->
     * <items>
     *   <item>value1</item>
     *   <item>value2</item>
     * </items>
     * }</pre>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is an {@link ArrayNode}, {@code false} otherwise
     *         (including for {@link NullNode}, {@link ObjectNode}, and primitive nodes)
     */
    @Override
    public boolean isList(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isArray();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a string value. In Jackson
     * terminology, this corresponds to a {@link TextNode}.</p>
     *
     * <h3>XML Context</h3>
     * <p>In XML, string values are typically represented as text content within elements:</p>
     * <pre>{@code
     * <name>John Doe</name>
     * }</pre>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is a {@link TextNode}, {@code false} otherwise
     *         (including for numeric and boolean nodes)
     */
    @Override
    public boolean isString(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isTextual();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a numeric value. This includes all
     * numeric node types: {@link IntNode}, {@link LongNode}, {@link FloatNode},
     * {@link DoubleNode}, and {@link ShortNode}.</p>
     *
     * <h3>XML Context</h3>
     * <p>XML has no native numeric types; numbers are represented as text. Jackson parses
     * numeric text content into appropriate node types based on the value:</p>
     * <pre>{@code
     * <count>42</count>      <!-- Parsed as IntNode -->
     * <price>19.99</price>   <!-- Parsed as DoubleNode -->
     * }</pre>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is a numeric node, {@code false} otherwise
     *         (including for string and boolean nodes)
     */
    @Override
    public boolean isNumber(@NotNull final JsonNode value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value.isNumber();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given node represents a boolean value. In Jackson
     * terminology, this corresponds to a {@link BooleanNode}.</p>
     *
     * <h3>XML Context</h3>
     * <p>XML has no native boolean type. Boolean values are typically represented as
     * the text "true" or "false":</p>
     * <pre>{@code
     * <enabled>true</enabled>
     * <active>false</active>
     * }</pre>
     *
     * @param value the JSON node to check; must not be {@code null}
     * @return {@code true} if the value is a {@link BooleanNode}, {@code false} otherwise
     *         (including for string and numeric nodes)
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
     * <p>Creates a new text node from the given string value. The resulting {@link TextNode}
     * will return {@code true} for {@link JsonNode#isTextual()}.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, text nodes become element text content:</p>
     * <pre>{@code
     * <element>string value here</element>
     * }</pre>
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
     * <p>Creates a new integer node from the given value. The resulting {@link IntNode}
     * will return {@code true} for {@link JsonNode#isNumber()} and {@link JsonNode#isInt()}.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, integer nodes become numeric text content:</p>
     * <pre>{@code
     * <count>42</count>
     * }</pre>
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
     * <p>Creates a new long node from the given value. The resulting {@link LongNode}
     * will return {@code true} for {@link JsonNode#isNumber()} and {@link JsonNode#isLong()}.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, long nodes become numeric text content:</p>
     * <pre>{@code
     * <timestamp>1609459200000</timestamp>
     * }</pre>
     *
     * <p><strong>Note:</strong> XML has no native distinction between integer and long types.
     * Very large long values may lose precision when processed by other systems that parse
     * XML numeric content as standard integers or floating-point numbers.</p>
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
     * <p>Creates a new float node from the given value. The resulting {@link FloatNode}
     * will return {@code true} for {@link JsonNode#isNumber()} and {@link JsonNode#isFloat()}.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, float nodes become numeric text content:</p>
     * <pre>{@code
     * <price>19.99</price>
     * }</pre>
     *
     * <p><strong>Note:</strong> Special float values ({@link Float#NaN},
     * {@link Float#POSITIVE_INFINITY}, {@link Float#NEGATIVE_INFINITY}) may not be valid
     * XML numeric content and could cause issues during serialization or when processed
     * by other XML parsers.</p>
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
     * <p>Creates a new double node from the given value. The resulting {@link DoubleNode}
     * will return {@code true} for {@link JsonNode#isNumber()} and {@link JsonNode#isDouble()}.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, double nodes become numeric text content:</p>
     * <pre>{@code
     * <latitude>52.520008</latitude>
     * }</pre>
     *
     * <p><strong>Note:</strong> Special double values ({@link Double#NaN},
     * {@link Double#POSITIVE_INFINITY}, {@link Double#NEGATIVE_INFINITY}) may not be valid
     * XML numeric content and could cause issues during serialization or when processed
     * by other XML parsers.</p>
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
     * <p>Creates a new numeric node from the given byte value. The value is stored as a
     * {@link ShortNode} because Jackson does not have a dedicated byte node type.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, byte values become numeric text content:</p>
     * <pre>{@code
     * <flags>127</flags>
     * }</pre>
     *
     * <p><strong>Note:</strong> XML does not have a dedicated byte type. The value is
     * stored as a short integer, and type information may be lost during round-trips
     * through XML serialization.</p>
     *
     * @param value the byte value to wrap
     * @return a new {@link ShortNode} containing the byte value as a short; never {@code null}
     */
    @NotNull
    @Override
    public JsonNode createByte(final byte value) {
        return ShortNode.valueOf(value);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new short node from the given value. The resulting {@link ShortNode}
     * will return {@code true} for {@link JsonNode#isNumber()} and {@link JsonNode#isShort()}.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, short nodes become numeric text content:</p>
     * <pre>{@code
     * <port>8080</port>
     * }</pre>
     *
     * <p><strong>Note:</strong> XML does not have a dedicated short type. The value is
     * stored as a number, and specific type information may be lost during round-trips
     * through XML serialization.</p>
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
     * <p>Creates a new boolean node from the given value. The resulting {@link BooleanNode}
     * will return {@code true} for {@link JsonNode#isBoolean()}.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, boolean nodes become text content "true" or "false":</p>
     * <pre>{@code
     * <enabled>true</enabled>
     * <visible>false</visible>
     * }</pre>
     *
     * @param value the boolean value to wrap
     * @return a {@link BooleanNode} for the given value (uses cached instances);
     *         never {@code null}
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
     * inspects the runtime type of the number and creates the most appropriate node type:</p>
     * <ul>
     *   <li>{@link Integer} -> {@link IntNode}</li>
     *   <li>{@link Long} -> {@link LongNode}</li>
     *   <li>{@link Float} -> {@link FloatNode}</li>
     *   <li>{@link Double} -> {@link DoubleNode}</li>
     *   <li>{@link Short} -> {@link ShortNode}</li>
     *   <li>{@link Byte} -> {@link ShortNode}</li>
     *   <li>Other -> {@link DoubleNode} (fallback)</li>
     * </ul>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, all numeric types become numeric text content:</p>
     * <pre>{@code
     * <value>42</value>
     * <ratio>0.75</ratio>
     * }</pre>
     *
     * @param value the number value to wrap; must not be {@code null}
     * @return a new numeric node appropriate for the number's type; never {@code null}
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
     *   <li>Input is a {@link TextNode}</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link TextNode} (e.g., array, object, null, numeric, boolean)</li>
     * </ul>
     *
     * <h3>XML Context</h3>
     * <p>This method extracts text content from XML elements that were parsed as text nodes:</p>
     * <pre>{@code
     * <name>John Doe</name>  <!-- getStringValue returns "John Doe" -->
     * }</pre>
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
     *   <li>Input is not a numeric node (e.g., array, object, null, text, boolean)</li>
     * </ul>
     *
     * <p>The returned {@link Number} preserves the original numeric type where possible,
     * allowing callers to use type-specific accessors like {@link Number#intValue()} or
     * {@link Number#doubleValue()}.</p>
     *
     * <h3>XML Context</h3>
     * <p>This method extracts numeric content from XML elements that were parsed as numbers:</p>
     * <pre>{@code
     * <count>42</count>  <!-- getNumberValue returns Integer(42) -->
     * <price>19.99</price>  <!-- getNumberValue returns Double(19.99) -->
     * }</pre>
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
     *   <li>Input is a {@link BooleanNode}</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input is not a {@link BooleanNode} (e.g., array, object, null, text, numeric)</li>
     * </ul>
     *
     * <h3>XML Context</h3>
     * <p>This method extracts boolean content from XML elements that were parsed as booleans:</p>
     * <pre>{@code
     * <enabled>true</enabled>  <!-- getBooleanValue returns true -->
     * <active>false</active>  <!-- getBooleanValue returns false -->
     * }</pre>
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
     * <p>Creates a new empty array node. This is the canonical way to create an empty
     * list structure in the Jackson XML format.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, empty arrays may be represented as empty wrapper elements
     * or omitted entirely depending on the {@link XmlMapper} configuration:</p>
     * <pre>{@code
     * <items></items>  <!-- or simply omitted -->
     * }</pre>
     *
     * @return a new empty {@link ArrayNode} created via the cached {@link JsonNodeFactory};
     *         never {@code null}
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
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, arrays become repeated elements or wrapped arrays:</p>
     * <pre>{@code
     * <items>
     *   <item>value1</item>
     *   <item>value2</item>
     *   <item>value3</item>
     * </items>
     * }</pre>
     *
     * <p><strong>Note:</strong> {@code null} elements in the stream will be added as-is,
     * which may cause issues. Ensure the stream contains only valid {@link JsonNode}
     * instances.</p>
     *
     * @param values a stream of JSON nodes to include in the array; must not be {@code null};
     *               may be empty
     * @return a new {@link ArrayNode} containing all stream elements in order; never {@code null}
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
     * <p>Returns the elements of an array node as a stream. This operation succeeds only
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
     * The stream does not support parallel processing.</p>
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
     * <h3>XML Context</h3>
     * <p>When the result is serialized to XML, the appended element becomes an additional
     * repeated element:</p>
     * <pre>{@code
     * <!-- Before: <items><item>a</item><item>b</item></items> -->
     * <!-- After:  <items><item>a</item><item>b</item><item>c</item></items> -->
     * }</pre>
     *
     * @param list  the array to append to; must not be {@code null}; may be a
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
     * map structure in the Jackson XML format.</p>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, empty objects become empty elements:</p>
     * <pre>{@code
     * <element></element>  <!-- or <element/> -->
     * }</pre>
     *
     * @return a new empty {@link ObjectNode} created via the cached {@link JsonNodeFactory};
     *         never {@code null}
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
     * JSON nodes that can be converted to strings; entries with {@code null} keys are skipped.</p>
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
     *   <li>{@code null} values are converted to {@link NullNode}</li>
     *   <li>All other values are added as-is</li>
     * </ul>
     *
     * <h3>XML Representation</h3>
     * <p>When serialized to XML, object entries become child elements:</p>
     * <pre>{@code
     * <parent>
     *   <name>John</name>
     *   <age>30</age>
     * </parent>
     * }</pre>
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
     * <h3>XML Context</h3>
     * <p>For XML data, object entries correspond to child elements:</p>
     * <pre>{@code
     * <parent>
     *   <name>John</name>  <!-- Entry: ("name", TextNode("John")) -->
     *   <age>30</age>      <!-- Entry: ("age", IntNode(30)) -->
     * </parent>
     * }</pre>
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
     *   <li>Input map is a {@link NullNode} (treated as empty object)</li>
     *   <li>Key is a text node (string)</li>
     * </ul>
     *
     * <h3>Failure Conditions</h3>
     * <ul>
     *   <li>Input map is not an object or null (e.g., array, primitive)</li>
     *   <li>Key is not a text node</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original map is never modified. A deep copy
     * is created via {@link ObjectNode#deepCopy()} before the entry is added.</p>
     *
     * <h3>XML Context</h3>
     * <p>The added entry becomes a child element in XML:</p>
     * <pre>{@code
     * <!-- Before: <parent><name>John</name></parent> -->
     * <!-- After:  <parent><name>John</name><age>30</age></parent> -->
     * }</pre>
     *
     * @param map   the object to add the entry to; must not be {@code null}; may be a
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
     *   <li>Order is based on {@link ObjectNode} implementation (typically insertion order)</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> Neither input object is modified. A deep copy
     * of the first map is created via {@link ObjectNode#deepCopy()} before merging.</p>
     *
     * <h3>XML Context</h3>
     * <p>Merged entries become combined child elements:</p>
     * <pre>{@code
     * <!-- Map 1: <obj><a>1</a><b>2</b></obj> -->
     * <!-- Map 2: <obj><b>3</b><c>4</c></obj> -->
     * <!-- Result: <obj><a>1</a><b>3</b><c>4</c></obj> -->
     * }</pre>
     *
     * @param map   the base object; must not be {@code null}; may be a {@link NullNode}
     * @param other the object to merge into the base; must not be {@code null}; may be a
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
     * and a key mapped to {@link NullNode}. Use {@link #has(JsonNode, String)} to check
     * for key existence when this distinction matters.</p>
     *
     * <h3>XML Context</h3>
     * <p>For XML data, this retrieves child elements by name:</p>
     * <pre>{@code
     * <parent>
     *   <name>John</name>  <!-- get(parent, "name") returns TextNode("John") -->
     * </parent>
     * }</pre>
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
     * input is an object, {@link ObjectNode#deepCopy()} is used to create the copy.</p>
     *
     * <h3>XML Context</h3>
     * <p>The set field becomes a child element:</p>
     * <pre>{@code
     * <!-- Input: <parent><a>1</a></parent> -->
     * <!-- set(input, "b", IntNode(2)) -->
     * <!-- Result: <parent><a>1</a><b>2</b></parent> -->
     * }</pre>
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
     * input is an object, {@link ObjectNode#deepCopy()} is used to create the copy.</p>
     *
     * <h3>XML Context</h3>
     * <p>The removed field is omitted from the result:</p>
     * <pre>{@code
     * <!-- Input: <parent><a>1</a><b>2</b></parent> -->
     * <!-- remove(input, "b") -->
     * <!-- Result: <parent><a>1</a></parent> -->
     * }</pre>
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
     * <h3>XML Context</h3>
     * <p>For XML data, this checks for the existence of child elements:</p>
     * <pre>{@code
     * <parent>
     *   <name>John</name>
     * </parent>
     * <!-- has(parent, "name") returns true -->
     * <!-- has(parent, "age") returns false -->
     * }</pre>
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
     * <p>Converts data from another {@link DynamicOps} format to Jackson {@link JsonNode}
     * for XML processing. This method recursively converts all nested structures, handling
     * primitives, lists, and maps appropriately.</p>
     *
     * <h3>Conversion Process</h3>
     * <p>The conversion attempts to identify the input type in the following order:</p>
     * <ol>
     *   <li><strong>Boolean:</strong> If {@link DynamicOps#getBooleanValue} succeeds,
     *       creates a {@link BooleanNode}</li>
     *   <li><strong>Number:</strong> If {@link DynamicOps#getNumberValue} succeeds,
     *       creates an appropriate numeric node via {@link #createNumeric(Number)}</li>
     *   <li><strong>String:</strong> If {@link DynamicOps#getStringValue} succeeds,
     *       creates a {@link TextNode}</li>
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
     * </ul>
     *
     * <h3>XML Considerations</h3>
     * <p>When converting to XML format, be aware that:</p>
     * <ul>
     *   <li>The converted data should have a single root element for valid XML serialization</li>
     *   <li>Array elements will be serialized according to the {@link XmlMapper} configuration</li>
     *   <li>Map keys become element names, which must be valid XML element names</li>
     * </ul>
     *
     * @param sourceOps the {@link DynamicOps} instance for the source format; must not be
     *                  {@code null}
     * @param input     the value to convert from the source format; must not be {@code null}
     * @param <U>       the type parameter of the source format
     * @return the converted {@link JsonNode}; never {@code null}; returns {@link NullNode}
     *         if conversion is not possible
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
     * debugging, logging, and error messages. It helps distinguish this XML-based
     * implementation from other {@link DynamicOps} implementations like
     * {@code GsonOps} or {@code JacksonJsonOps}.</p>
     *
     * @return the string {@code "JacksonXmlOps"}; never {@code null}
     */
    @Override
    public String toString() {
        return "JacksonXmlOps";
    }
}
