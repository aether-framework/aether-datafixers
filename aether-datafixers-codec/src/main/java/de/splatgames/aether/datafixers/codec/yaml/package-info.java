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
 * YAML format implementations for the Aether Datafixers codec module.
 *
 * <p>This package serves as the parent namespace for all YAML-based
 * {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} implementations
 * within the Aether Datafixers framework. It provides format-agnostic data manipulation
 * capabilities for YAML data, enabling the data fixing system to read, write, and
 * transform YAML without coupling to specific YAML library APIs.</p>
 *
 * <h2>Available Implementations</h2>
 * <table border="1">
 *   <caption>YAML DynamicOps Implementations</caption>
 *   <tr>
 *     <th>Package</th>
 *     <th>Class</th>
 *     <th>Data Type</th>
 *     <th>Library</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.codec.yaml.snakeyaml}</td>
 *     <td>{@link de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps}</td>
 *     <td>{@link java.lang.Object} (native Java types)</td>
 *     <td>SnakeYAML 2.x</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.codec.yaml.jackson}</td>
 *     <td>{@link de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps}</td>
 *     <td>{@link com.fasterxml.jackson.databind.JsonNode}</td>
 *     <td>Jackson YAML Dataformat</td>
 *   </tr>
 * </table>
 *
 * <h2>Choosing an Implementation</h2>
 * <ul>
 *   <li><strong>SnakeYamlOps:</strong> Works with native Java types ({@link java.util.Map},
 *       {@link java.util.List}, primitives). Ideal when you want direct access to Java objects
 *       without intermediate tree representations. Lightweight with minimal memory overhead.</li>
 *   <li><strong>JacksonYamlOps:</strong> Works with Jackson's {@link com.fasterxml.jackson.databind.JsonNode}
 *       tree model. Recommended when you're already using Jackson, need advanced YAML features,
 *       or want seamless integration with other Jackson dataformat modules.</li>
 * </ul>
 *
 * <h2>YAML-Specific Features</h2>
 * <p>Both implementations support YAML's rich feature set:</p>
 * <ul>
 *   <li>Multi-line strings (literal and folded block scalars)</li>
 *   <li>Comments (preserved during parsing with some libraries)</li>
 *   <li>Anchors and aliases (for reference resolution)</li>
 *   <li>Multiple documents in a single file</li>
 *   <li>Custom tags and type coercion</li>
 * </ul>
 *
 * <h2>Format Interoperability</h2>
 * <p>Both implementations can convert data to and from each other and to other formats
 * using the {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps#convertTo(DynamicOps, Object)}
 * method:</p>
 * <pre>{@code
 * // Convert from SnakeYAML to Jackson YAML
 * JsonNode jacksonNode = JacksonYamlOps.INSTANCE.convertTo(SnakeYamlOps.INSTANCE, snakeData);
 *
 * // Convert YAML to JSON
 * JsonElement gsonElement = GsonOps.INSTANCE.convertTo(SnakeYamlOps.INSTANCE, yamlData);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All implementations in this package hierarchy are thread-safe. The singleton
 * instances (e.g., {@code SnakeYamlOps.INSTANCE}, {@code JacksonYamlOps.INSTANCE}) can
 * be safely shared across multiple threads.</p>
 *
 * <h2>Dependencies</h2>
 * <p>Each sub-package requires its respective YAML library as a dependency:</p>
 * <ul>
 *   <li>{@code codec.yaml.snakeyaml} requires {@code org.yaml:snakeyaml:2.2+}</li>
 *   <li>{@code codec.yaml.jackson} requires {@code com.fasterxml.jackson.dataformat:jackson-dataformat-yaml}</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps
 * @see de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.codec.yaml;
