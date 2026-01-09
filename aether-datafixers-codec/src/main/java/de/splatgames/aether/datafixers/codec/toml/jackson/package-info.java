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
 * for TOML data manipulation within the Aether Datafixers framework.
 *
 * <p>This package provides {@link de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps},
 * a format-agnostic data manipulation implementation that works with Jackson Databind's
 * {@link com.fasterxml.jackson.databind.JsonNode} tree model using the
 * {@link com.fasterxml.jackson.dataformat.toml.TomlMapper} for TOML serialization.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Jackson Tree Model:</strong> Works with {@link com.fasterxml.jackson.databind.JsonNode},
 *       enabling seamless integration with other Jackson-based code</li>
 *   <li><strong>Singleton Pattern:</strong> Use {@link de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps#INSTANCE}
 *       for standard operations with a default {@link com.fasterxml.jackson.dataformat.toml.TomlMapper}</li>
 *   <li><strong>Custom TomlMapper:</strong> Create instances with custom configurations using
 *       {@link de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps#JacksonTomlOps(TomlMapper)}</li>
 *   <li><strong>Format Compatibility:</strong> Since TOML uses {@link com.fasterxml.jackson.databind.JsonNode},
 *       data can be easily converted between TOML and other Jackson-supported formats</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Parsing TOML with Jackson</h3>
 * <pre>{@code
 * // TOML input
 * String tomlContent = """
 *     [player]
 *     name = "Player1"
 *     score = 100
 *     """;
 *
 * // Parse TOML string using TomlMapper
 * TomlMapper mapper = new TomlMapper();
 * JsonNode data = mapper.readTree(tomlContent);
 *
 * // Create a Dynamic wrapper for manipulation
 * Dynamic<JsonNode> dynamic = new Dynamic<>(JacksonTomlOps.INSTANCE, data);
 *
 * // Access nested values
 * Optional<String> name = dynamic.get("player").get("name").asString().result();
 * Optional<Integer> score = dynamic.get("player").get("score").asNumber().result().map(Number::intValue);
 *
 * // Use with codecs for type-safe deserialization
 * DataResult<GameConfig> result = GameConfig.CODEC.parse(dynamic);
 * }</pre>
 *
 * <h3>Creating TOML Data</h3>
 * <pre>{@code
 * JacksonTomlOps ops = JacksonTomlOps.INSTANCE;
 *
 * // Create nested structure (TOML requires root to be a table)
 * JsonNode config = ops.createMap(Stream.of(
 *     Pair.of(ops.createString("database"), ops.createMap(Stream.of(
 *         Pair.of(ops.createString("host"), ops.createString("localhost")),
 *         Pair.of(ops.createString("port"), ops.createInt(5432)),
 *         Pair.of(ops.createString("enabled"), ops.createBoolean(true))
 *     )))
 * ));
 *
 * // Write to TOML string
 * TomlMapper mapper = ops.mapper();
 * String tomlString = mapper.writeValueAsString(config);
 * // Output:
 * // [database]
 * // host = "localhost"
 * // port = 5432
 * // enabled = true
 * }</pre>
 *
 * <h3>Custom TomlMapper Configuration</h3>
 * <pre>{@code
 * // Create a custom TomlMapper with specific settings
 * TomlMapper customMapper = TomlMapper.builder()
 *     .configure(TomlWriteFeature.FAIL_ON_NULL_WRITE, true)
 *     .build();
 *
 * // Create JacksonTomlOps with custom mapper
 * JacksonTomlOps customOps = new JacksonTomlOps(customMapper);
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <table border="1">
 *   <caption>Java to Jackson Node Type Mapping for TOML</caption>
 *   <tr><th>Java Type</th><th>Jackson Node Type</th><th>TOML Type</th></tr>
 *   <tr><td>{@code String}</td><td>{@link com.fasterxml.jackson.databind.node.TextNode}</td><td>String</td></tr>
 *   <tr><td>{@code int}</td><td>{@link com.fasterxml.jackson.databind.node.IntNode}</td><td>Integer</td></tr>
 *   <tr><td>{@code long}</td><td>{@link com.fasterxml.jackson.databind.node.LongNode}</td><td>Integer</td></tr>
 *   <tr><td>{@code float}</td><td>{@link com.fasterxml.jackson.databind.node.FloatNode}</td><td>Float</td></tr>
 *   <tr><td>{@code double}</td><td>{@link com.fasterxml.jackson.databind.node.DoubleNode}</td><td>Float</td></tr>
 *   <tr><td>{@code boolean}</td><td>{@link com.fasterxml.jackson.databind.node.BooleanNode}</td><td>Boolean</td></tr>
 *   <tr><td>{@code null}</td><td>{@link com.fasterxml.jackson.databind.node.NullNode}</td><td><em>Not supported</em></td></tr>
 *   <tr><td>{@code List<T>}</td><td>{@link com.fasterxml.jackson.databind.node.ArrayNode}</td><td>Array</td></tr>
 *   <tr><td>{@code Map<String, T>}</td><td>{@link com.fasterxml.jackson.databind.node.ObjectNode}</td><td>Table</td></tr>
 * </table>
 *
 * <h2>TOML-Specific Constraints</h2>
 * <ul>
 *   <li><strong>Root Must Be Table:</strong> The root element must be an {@link com.fasterxml.jackson.databind.node.ObjectNode}</li>
 *   <li><strong>No Null Support:</strong> TOML does not support null values; attempting to serialize null will fail</li>
 *   <li><strong>String Keys Only:</strong> All map keys must be strings</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps} class is thread-safe
 * when the underlying {@link com.fasterxml.jackson.dataformat.toml.TomlMapper} is thread-safe (which is the
 * case for the default singleton instance). All operations are stateless and create new instances
 * rather than modifying existing data.</p>
 *
 * <h2>Dependencies</h2>
 * <p>This package requires Jackson TOML Dataformat as a runtime dependency:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.fasterxml.jackson.dataformat</groupId>
 *     <artifactId>jackson-dataformat-toml</artifactId>
 *     <version>2.15+</version>
 * </dependency>
 * }</pre>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see com.fasterxml.jackson.databind.JsonNode
 * @see com.fasterxml.jackson.dataformat.toml.TomlMapper
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.codec.toml.jackson;
