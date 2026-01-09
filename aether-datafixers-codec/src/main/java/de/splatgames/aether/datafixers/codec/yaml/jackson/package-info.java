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
 * for YAML data manipulation within the Aether Datafixers framework.
 *
 * <p>This package provides {@link de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps},
 * a format-agnostic data manipulation implementation that works with Jackson Databind's
 * {@link com.fasterxml.jackson.databind.JsonNode} tree model using the
 * {@link com.fasterxml.jackson.dataformat.yaml.YAMLMapper} for YAML serialization.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Jackson Tree Model:</strong> Works with {@link com.fasterxml.jackson.databind.JsonNode},
 *       enabling seamless integration with other Jackson-based code</li>
 *   <li><strong>Singleton Pattern:</strong> Use {@link de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps#INSTANCE}
 *       for standard operations with a default {@link com.fasterxml.jackson.dataformat.yaml.YAMLMapper}</li>
 *   <li><strong>Custom YAMLMapper:</strong> Create instances with custom configurations using
 *       {@link de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps#JacksonYamlOps(YAMLMapper)}</li>
 *   <li><strong>JSON Compatibility:</strong> Since both JSON and YAML use {@link com.fasterxml.jackson.databind.JsonNode},
 *       data can be easily converted between formats</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Parsing YAML with Jackson</h3>
 * <pre>{@code
 * // Parse YAML string using YAMLMapper
 * YAMLMapper mapper = new YAMLMapper();
 * JsonNode data = mapper.readTree("name: Player1\nscore: 100");
 *
 * // Create a Dynamic wrapper for manipulation
 * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonYamlOps.INSTANCE, data);
 *
 * // Access nested values
 * Optional<String> name = dynamic.get("name").asString().result();
 * Optional<Integer> score = dynamic.get("score").asNumber().result().map(Number::intValue);
 *
 * // Use with codecs for type-safe deserialization
 * DataResult<PlayerData> result = PlayerData.CODEC.parse(dynamic);
 * }</pre>
 *
 * <h3>Custom YAMLMapper Configuration</h3>
 * <pre>{@code
 * // Create a custom YAMLMapper with specific settings
 * YAMLMapper customMapper = YAMLMapper.builder()
 *     .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
 *     .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
 *     .build();
 *
 * // Create JacksonYamlOps with custom mapper
 * JacksonYamlOps customOps = new JacksonYamlOps(customMapper);
 *
 * // Use the custom ops for operations
 * Dynamic<JsonNode> dynamic = new Dynamic<>(customOps, yamlNode);
 * }</pre>
 *
 * <h3>Writing YAML Output</h3>
 * <pre>{@code
 * // Create data using JacksonYamlOps
 * JacksonYamlOps ops = JacksonYamlOps.INSTANCE;
 * JsonNode data = ops.createMap(Stream.of(
 *     Pair.of(ops.createString("name"), ops.createString("Player1")),
 *     Pair.of(ops.createString("score"), ops.createInt(100))
 * ));
 *
 * // Write to YAML string using YAMLMapper
 * YAMLMapper mapper = ops.mapper();
 * String yamlString = mapper.writeValueAsString(data);
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <table border="1">
 *   <caption>Java to Jackson Node Type Mapping</caption>
 *   <tr><th>Java Type</th><th>Jackson Node Type</th></tr>
 *   <tr><td>{@code String}</td><td>{@link com.fasterxml.jackson.databind.node.TextNode}</td></tr>
 *   <tr><td>{@code int}</td><td>{@link com.fasterxml.jackson.databind.node.IntNode}</td></tr>
 *   <tr><td>{@code long}</td><td>{@link com.fasterxml.jackson.databind.node.LongNode}</td></tr>
 *   <tr><td>{@code float}</td><td>{@link com.fasterxml.jackson.databind.node.FloatNode}</td></tr>
 *   <tr><td>{@code double}</td><td>{@link com.fasterxml.jackson.databind.node.DoubleNode}</td></tr>
 *   <tr><td>{@code boolean}</td><td>{@link com.fasterxml.jackson.databind.node.BooleanNode}</td></tr>
 *   <tr><td>{@code null}</td><td>{@link com.fasterxml.jackson.databind.node.NullNode}</td></tr>
 *   <tr><td>{@code List<T>}</td><td>{@link com.fasterxml.jackson.databind.node.ArrayNode}</td></tr>
 *   <tr><td>{@code Map<String, T>}</td><td>{@link com.fasterxml.jackson.databind.node.ObjectNode}</td></tr>
 * </table>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps} class is thread-safe
 * when the underlying {@link com.fasterxml.jackson.dataformat.yaml.YAMLMapper} is thread-safe (which is the
 * case for the default singleton instance). All operations are stateless and create new instances
 * rather than modifying existing data.</p>
 *
 * <h2>Dependencies</h2>
 * <p>This package requires Jackson YAML Dataformat as a runtime dependency:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.fasterxml.jackson.dataformat</groupId>
 *     <artifactId>jackson-dataformat-yaml</artifactId>
 *     <version>2.15+</version>
 * </dependency>
 * }</pre>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see com.fasterxml.jackson.databind.JsonNode
 * @see com.fasterxml.jackson.dataformat.yaml.YAMLMapper
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.codec.yaml.jackson;
