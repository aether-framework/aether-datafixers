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

/**
 * Jackson-based {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} implementation
 * for XML data manipulation within the Aether Datafixers framework.
 *
 * <p>This package provides {@link de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps},
 * a format-agnostic data manipulation implementation that works with Jackson Databind's
 * {@link com.fasterxml.jackson.databind.JsonNode} tree model using the
 * {@link com.fasterxml.jackson.dataformat.xml.XmlMapper} for XML serialization.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Jackson Tree Model:</strong> Works with {@link com.fasterxml.jackson.databind.JsonNode},
 *       enabling seamless integration with other Jackson-based code</li>
 *   <li><strong>Singleton Pattern:</strong> Use {@link de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps#INSTANCE}
 *       for standard operations with a default {@link com.fasterxml.jackson.dataformat.xml.XmlMapper}</li>
 *   <li><strong>Custom XmlMapper:</strong> Create instances with custom configurations using
 *       {@link de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps#JacksonXmlOps(XmlMapper)}</li>
 *   <li><strong>Format Compatibility:</strong> Since XML uses {@link com.fasterxml.jackson.databind.JsonNode},
 *       data can be easily converted between XML and other Jackson-supported formats</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Parsing XML with Jackson</h3>
 * <pre>{@code
 * // XML input
 * String xmlContent = """
 *     <player>
 *         <name>Player1</name>
 *         <score>100</score>
 *     </player>
 *     """;
 *
 * // Parse XML string using XmlMapper
 * XmlMapper mapper = new XmlMapper();
 * JsonNode data = mapper.readTree(xmlContent);
 *
 * // Create a Dynamic wrapper for manipulation
 * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonXmlOps.INSTANCE, data);
 *
 * // Access nested values
 * Optional<String> name = dynamic.get("name").asString().result();
 * Optional<Integer> score = dynamic.get("score").asNumber().result().map(Number::intValue);
 *
 * // Use with codecs for type-safe deserialization
 * DataResult<PlayerData> result = PlayerData.CODEC.parse(dynamic);
 * }</pre>
 *
 * <h3>Creating XML Data</h3>
 * <pre>{@code
 * JacksonXmlOps ops = JacksonXmlOps.INSTANCE;
 *
 * // Create structure (will be wrapped in root element on serialization)
 * JsonNode data = ops.createMap(Stream.of(
 *     Pair.of(ops.createString("name"), ops.createString("Player1")),
 *     Pair.of(ops.createString("score"), ops.createInt(100)),
 *     Pair.of(ops.createString("active"), ops.createBoolean(true))
 * ));
 *
 * // Write to XML string
 * XmlMapper mapper = ops.mapper();
 * String xmlString = mapper.writer()
 *     .withRootName("player")
 *     .writeValueAsString(data);
 * // Output:
 * // <player>
 * //     <name>Player1</name>
 * //     <score>100</score>
 * //     <active>true</active>
 * // </player>
 * }</pre>
 *
 * <h3>Custom XmlMapper Configuration</h3>
 * <pre>{@code
 * // Create a custom XmlMapper with specific settings
 * XmlMapper customMapper = XmlMapper.builder()
 *     .defaultUseWrapper(false)
 *     .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
 *     .build();
 *
 * // Create JacksonXmlOps with custom mapper
 * JacksonXmlOps customOps = new JacksonXmlOps(customMapper);
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <table border="1">
 *   <caption>Java to Jackson Node Type Mapping for XML</caption>
 *   <tr><th>Java Type</th><th>Jackson Node Type</th><th>XML Representation</th></tr>
 *   <tr><td>{@code String}</td><td>{@link com.fasterxml.jackson.databind.node.TextNode}</td><td>Text content</td></tr>
 *   <tr><td>{@code int}</td><td>{@link com.fasterxml.jackson.databind.node.IntNode}</td><td>Text content</td></tr>
 *   <tr><td>{@code long}</td><td>{@link com.fasterxml.jackson.databind.node.LongNode}</td><td>Text content</td></tr>
 *   <tr><td>{@code float}</td><td>{@link com.fasterxml.jackson.databind.node.FloatNode}</td><td>Text content</td></tr>
 *   <tr><td>{@code double}</td><td>{@link com.fasterxml.jackson.databind.node.DoubleNode}</td><td>Text content</td></tr>
 *   <tr><td>{@code boolean}</td><td>{@link com.fasterxml.jackson.databind.node.BooleanNode}</td><td>Text content</td></tr>
 *   <tr><td>{@code null}</td><td>{@link com.fasterxml.jackson.databind.node.NullNode}</td><td>Empty element or xsi:nil</td></tr>
 *   <tr><td>{@code List<T>}</td><td>{@link com.fasterxml.jackson.databind.node.ArrayNode}</td><td>Repeated elements</td></tr>
 *   <tr><td>{@code Map<String, T>}</td><td>{@link com.fasterxml.jackson.databind.node.ObjectNode}</td><td>Child elements</td></tr>
 * </table>
 *
 * <h2>XML-Specific Considerations</h2>
 * <ul>
 *   <li><strong>Root Element:</strong> XML requires a root element. When serializing,
 *       ensure your data structure is wrapped appropriately or use {@code withRootName()}.</li>
 *   <li><strong>Array Handling:</strong> XML arrays are represented as repeated elements.
 *       The mapper configuration controls wrapper elements for arrays.</li>
 *   <li><strong>Attributes:</strong> XML attributes can be mapped using Jackson's
 *       {@code @JacksonXmlProperty(isAttribute = true)} annotation on POJOs.</li>
 *   <li><strong>Namespaces:</strong> XML namespaces can be configured via XmlMapper settings.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps} class is thread-safe
 * when the underlying {@link com.fasterxml.jackson.dataformat.xml.XmlMapper} is thread-safe (which is the
 * case for the default singleton instance). All operations are stateless and create new instances
 * rather than modifying existing data.</p>
 *
 * <h2>Dependencies</h2>
 * <p>This package requires Jackson XML Dataformat as a runtime dependency:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.fasterxml.jackson.dataformat</groupId>
 *     <artifactId>jackson-dataformat-xml</artifactId>
 *     <version>2.15+</version>
 * </dependency>
 * }</pre>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see com.fasterxml.jackson.databind.JsonNode
 * @see com.fasterxml.jackson.dataformat.xml.XmlMapper
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.codec.xml.jackson;
