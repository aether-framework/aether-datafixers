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
 * for JSON data manipulation within the Aether Datafixers framework.
 *
 * <p>This package provides {@link de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps},
 * a format-agnostic data manipulation implementation that works with Jackson Databind's
 * {@link com.fasterxml.jackson.databind.JsonNode} tree model. It enables the data fixing system
 * to read, write, and transform JSON data while leveraging Jackson's powerful features.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Singleton Pattern:</strong> Use {@link de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps#INSTANCE}
 *       for standard operations with a default {@link com.fasterxml.jackson.databind.ObjectMapper}</li>
 *   <li><strong>Custom ObjectMapper:</strong> Create instances with custom configurations using
 *       {@link de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps#JacksonJsonOps(ObjectMapper)}</li>
 *   <li><strong>Rich Type Support:</strong> Full support for all Jackson numeric node types
 *       (IntNode, LongNode, FloatNode, DoubleNode, ShortNode)</li>
 *   <li><strong>Immutable Operations:</strong> All modification methods return new instances,
 *       preserving original data integrity</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Usage with Codecs</h3>
 * <pre>{@code
 * // Parse JSON string to Jackson node
 * ObjectMapper mapper = new ObjectMapper();
 * JsonNode json = mapper.readTree("{\"name\": \"Player1\", \"score\": 100}");
 *
 * // Create a Dynamic wrapper for manipulation
 * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonJsonOps.INSTANCE, json);
 *
 * // Access nested values
 * Optional<String> name = dynamic.get("name").asString().result();
 * Optional<Integer> score = dynamic.get("score").asNumber().result().map(Number::intValue);
 *
 * // Use with codecs for type-safe deserialization
 * DataResult<PlayerData> result = PlayerData.CODEC.parse(dynamic);
 * }</pre>
 *
 * <h3>Custom ObjectMapper Configuration</h3>
 * <pre>{@code
 * // Create a custom ObjectMapper with specific settings
 * ObjectMapper customMapper = new ObjectMapper()
 *     .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
 *     .configure(SerializationFeature.INDENT_OUTPUT, true);
 *
 * // Create JacksonJsonOps with custom mapper
 * JacksonJsonOps customOps = new JacksonJsonOps(customMapper);
 *
 * // Use the custom ops for operations
 * Dynamic<JsonNode> dynamic = new Dynamic<>(customOps, jsonNode);
 * }</pre>
 *
 * <h3>Data Transformation</h3>
 * <pre>{@code
 * // Transform data using DynamicOps
 * JacksonJsonOps ops = JacksonJsonOps.INSTANCE;
 *
 * // Create primitive values
 * JsonNode stringValue = ops.createString("Hello");
 * JsonNode intValue = ops.createInt(42);
 * JsonNode boolValue = ops.createBoolean(true);
 *
 * // Create complex structures
 * JsonNode map = ops.createMap(Stream.of(
 *     Pair.of(ops.createString("key1"), ops.createInt(1)),
 *     Pair.of(ops.createString("key2"), ops.createInt(2))
 * ));
 *
 * JsonNode list = ops.createList(Stream.of(
 *     ops.createInt(1),
 *     ops.createInt(2),
 *     ops.createInt(3)
 * ));
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <table border="1">
 *   <caption>Java to Jackson Type Mapping</caption>
 *   <tr><th>Java Type</th><th>Jackson Node Type</th></tr>
 *   <tr><td>{@code String}</td><td>{@link com.fasterxml.jackson.databind.node.TextNode}</td></tr>
 *   <tr><td>{@code int}</td><td>{@link com.fasterxml.jackson.databind.node.IntNode}</td></tr>
 *   <tr><td>{@code long}</td><td>{@link com.fasterxml.jackson.databind.node.LongNode}</td></tr>
 *   <tr><td>{@code float}</td><td>{@link com.fasterxml.jackson.databind.node.FloatNode}</td></tr>
 *   <tr><td>{@code double}</td><td>{@link com.fasterxml.jackson.databind.node.DoubleNode}</td></tr>
 *   <tr><td>{@code short}, {@code byte}</td><td>{@link com.fasterxml.jackson.databind.node.ShortNode}</td></tr>
 *   <tr><td>{@code boolean}</td><td>{@link com.fasterxml.jackson.databind.node.BooleanNode}</td></tr>
 *   <tr><td>{@code null}</td><td>{@link com.fasterxml.jackson.databind.node.NullNode}</td></tr>
 *   <tr><td>{@code List<T>}</td><td>{@link com.fasterxml.jackson.databind.node.ArrayNode}</td></tr>
 *   <tr><td>{@code Map<String, T>}</td><td>{@link com.fasterxml.jackson.databind.node.ObjectNode}</td></tr>
 * </table>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps} class is thread-safe
 * when the underlying {@link com.fasterxml.jackson.databind.ObjectMapper} is thread-safe (which is the
 * case for the default singleton instance). All operations are stateless and create new instances
 * rather than modifying existing data.</p>
 *
 * <h2>Dependencies</h2>
 * <p>This package requires Jackson Databind as a runtime dependency:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.fasterxml.jackson.core</groupId>
 *     <artifactId>jackson-databind</artifactId>
 *     <version>2.15+</version>
 * </dependency>
 * }</pre>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see com.fasterxml.jackson.databind.JsonNode
 * @see com.fasterxml.jackson.databind.ObjectMapper
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.codec.json.jackson;
