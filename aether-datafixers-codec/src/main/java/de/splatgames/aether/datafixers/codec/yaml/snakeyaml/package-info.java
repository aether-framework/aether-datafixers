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
 * SnakeYAML-based {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} implementation
 * for YAML data manipulation within the Aether Datafixers framework.
 *
 * <p>This package provides {@link de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps},
 * a format-agnostic data manipulation implementation that works with native Java types
 * as used by SnakeYAML 2.x. Unlike Jackson-based implementations that use a tree model,
 * SnakeYamlOps works directly with standard Java collections and primitives.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Native Java Types:</strong> Works with {@link java.util.Map}, {@link java.util.List},
 *       and Java primitives directly, without intermediate tree representations</li>
 *   <li><strong>Singleton Pattern:</strong> Use {@link de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps#INSTANCE}
 *       for all operations</li>
 *   <li><strong>Deep Copy Support:</strong> All modification operations create deep copies
 *       to preserve immutability</li>
 *   <li><strong>Lightweight:</strong> Minimal memory overhead compared to tree-based approaches</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Parsing YAML with SnakeYAML</h3>
 * <pre>{@code
 * // Parse YAML string using SnakeYAML
 * Yaml yaml = new Yaml();
 * Object data = yaml.load("name: Player1\nscore: 100");
 *
 * // Create a Dynamic wrapper for manipulation
 * Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, data);
 *
 * // Access nested values
 * Optional<String> name = dynamic.get("name").asString().result();
 * Optional<Integer> score = dynamic.get("score").asNumber().result().map(Number::intValue);
 *
 * // Use with codecs for type-safe deserialization
 * DataResult<PlayerData> result = PlayerData.CODEC.parse(dynamic);
 * }</pre>
 *
 * <h3>Creating YAML Data</h3>
 * <pre>{@code
 * SnakeYamlOps ops = SnakeYamlOps.INSTANCE;
 *
 * // Create primitive values
 * Object stringValue = ops.createString("Hello");  // Returns "Hello"
 * Object intValue = ops.createInt(42);             // Returns 42
 * Object boolValue = ops.createBoolean(true);      // Returns true
 *
 * // Create complex structures
 * Object map = ops.createMap(Stream.of(
 *     Pair.of(ops.createString("key1"), ops.createInt(1)),
 *     Pair.of(ops.createString("key2"), ops.createInt(2))
 * ));  // Returns LinkedHashMap
 *
 * Object list = ops.createList(Stream.of(
 *     ops.createInt(1),
 *     ops.createInt(2),
 *     ops.createInt(3)
 * ));  // Returns ArrayList
 * }</pre>
 *
 * <h3>Writing YAML Output</h3>
 * <pre>{@code
 * // Create data using SnakeYamlOps
 * Object data = ops.createMap(Stream.of(
 *     Pair.of(ops.createString("name"), ops.createString("Player1")),
 *     Pair.of(ops.createString("score"), ops.createInt(100))
 * ));
 *
 * // Write to YAML string using SnakeYAML
 * Yaml yaml = new Yaml();
 * String yamlString = yaml.dump(data);
 * // Output: name: Player1
 * //         score: 100
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <table border="1">
 *   <caption>Java Type Mapping in SnakeYamlOps</caption>
 *   <tr><th>DynamicOps Concept</th><th>Java Type Used</th></tr>
 *   <tr><td>String</td><td>{@link java.lang.String}</td></tr>
 *   <tr><td>Integer</td><td>{@link java.lang.Integer}</td></tr>
 *   <tr><td>Long</td><td>{@link java.lang.Long}</td></tr>
 *   <tr><td>Float</td><td>{@link java.lang.Float}</td></tr>
 *   <tr><td>Double</td><td>{@link java.lang.Double}</td></tr>
 *   <tr><td>Boolean</td><td>{@link java.lang.Boolean}</td></tr>
 *   <tr><td>Null/Empty</td><td>{@code null}</td></tr>
 *   <tr><td>List</td><td>{@link java.util.ArrayList}</td></tr>
 *   <tr><td>Map</td><td>{@link java.util.LinkedHashMap}</td></tr>
 * </table>
 *
 * <h2>Null Handling</h2>
 * <p>Unlike JSON-based implementations that use special null nodes, SnakeYamlOps uses
 * Java {@code null} to represent empty/null values. This matches SnakeYAML's native
 * behavior where YAML {@code null} or {@code ~} is parsed to Java {@code null}.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps} class
 * is thread-safe. The singleton instance can be safely shared across multiple threads.
 * All operations are stateless and create new collections rather than modifying existing data.</p>
 *
 * <h2>Dependencies</h2>
 * <p>This package requires SnakeYAML 2.x as a runtime dependency:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>org.yaml</groupId>
 *     <artifactId>snakeyaml</artifactId>
 *     <version>2.2</version>
 * </dependency>
 * }</pre>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.codec.yaml.snakeyaml;
