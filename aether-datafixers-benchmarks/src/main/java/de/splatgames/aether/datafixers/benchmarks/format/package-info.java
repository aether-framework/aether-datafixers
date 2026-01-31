/*
 * Copyright (c) 2026 Splatgames.de Software and Contributors
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
 * Format-focused JMH benchmarks comparing DynamicOps implementations in the Aether DataFixers framework.
 *
 * <p>This package contains benchmarks that compare the performance of different serialization
 * format implementations. These benchmarks help users choose the optimal DynamicOps implementation
 * for their specific use case based on empirical performance data.</p>
 *
 * <h2>Benchmark Classes</h2>
 * <table border="1">
 *   <tr>
 *     <th>Class</th>
 *     <th>Formats Compared</th>
 *     <th>Key Metrics</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.format.JsonBenchmark}</td>
 *     <td>GsonOps vs JacksonJsonOps</td>
 *     <td>Generation, field access, modification, migration</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.format.YamlBenchmark}</td>
 *     <td>SnakeYamlOps vs JacksonYamlOps</td>
 *     <td>Generation, field access, modification, migration</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.format.TomlXmlBenchmark}</td>
 *     <td>JacksonTomlOps vs JacksonXmlOps</td>
 *     <td>Generation, field access, modification, migration</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.format.CrossFormatBenchmark}</td>
 *     <td>All format pairs</td>
 *     <td>Cross-format conversion overhead</td>
 *   </tr>
 * </table>
 *
 * <h2>Supported DynamicOps Implementations</h2>
 * <table border="1">
 *   <tr><th>Format</th><th>Implementation</th><th>Library</th><th>Node Type</th></tr>
 *   <tr><td rowspan="2">JSON</td><td>GsonOps</td><td>Google Gson</td><td>JsonElement</td></tr>
 *   <tr><td>JacksonJsonOps</td><td>Jackson Databind</td><td>JsonNode</td></tr>
 *   <tr><td rowspan="2">YAML</td><td>SnakeYamlOps</td><td>SnakeYAML</td><td>Object (native)</td></tr>
 *   <tr><td>JacksonYamlOps</td><td>Jackson Dataformat YAML</td><td>JsonNode</td></tr>
 *   <tr><td>TOML</td><td>JacksonTomlOps</td><td>Jackson Dataformat TOML</td><td>JsonNode</td></tr>
 *   <tr><td>XML</td><td>JacksonXmlOps</td><td>Jackson Dataformat XML</td><td>JsonNode</td></tr>
 * </table>
 *
 * <h2>Benchmark Operations</h2>
 * <p>Each format benchmark measures the following operations:</p>
 * <ul>
 *   <li><b>Data Generation</b>: Time to create Dynamic objects from scratch</li>
 *   <li><b>Field Read</b>: Time to retrieve a single field from existing data</li>
 *   <li><b>Field Set</b>: Time to add/modify a field (creates new immutable structure)</li>
 *   <li><b>Migration</b>: Time to apply a DataFix to format-specific data</li>
 * </ul>
 *
 * <h2>Running Format Benchmarks</h2>
 * <pre>{@code
 * # Run all format benchmarks
 * java -jar benchmarks.jar ".*format.*"
 *
 * # Run specific format benchmark
 * java -jar benchmarks.jar JsonBenchmark
 * java -jar benchmarks.jar YamlBenchmark
 * java -jar benchmarks.jar TomlXmlBenchmark
 * java -jar benchmarks.jar CrossFormatBenchmark
 *
 * # Run all JSON-related benchmarks
 * java -jar benchmarks.jar ".*Json.*"
 *
 * # Run generation benchmarks across all formats
 * java -jar benchmarks.jar ".*Benchmark.*Generate"
 *
 * # Run migration benchmarks across all formats
 * java -jar benchmarks.jar ".*Benchmark.*Migration"
 *
 * # Run with specific payload size
 * java -jar benchmarks.jar ".*format.*" -p payloadSize=MEDIUM
 * }</pre>
 *
 * <h2>Choosing a DynamicOps Implementation</h2>
 * <p>Use these benchmark results to guide implementation selection:</p>
 * <table border="1">
 *   <tr><th>Scenario</th><th>Recommended</th><th>Rationale</th></tr>
 *   <tr>
 *     <td>General JSON processing</td>
 *     <td>GsonOps or JacksonJsonOps</td>
 *     <td>Compare benchmarks; both are mature and fast</td>
 *   </tr>
 *   <tr>
 *     <td>Configuration files (YAML)</td>
 *     <td>SnakeYamlOps</td>
 *     <td>Native YAML features (anchors, aliases)</td>
 *   </tr>
 *   <tr>
 *     <td>Mixed Jackson ecosystem</td>
 *     <td>JacksonJsonOps/JacksonYamlOps</td>
 *     <td>Shared code, faster cross-format conversion</td>
 *   </tr>
 *   <tr>
 *     <td>TOML configuration</td>
 *     <td>JacksonTomlOps</td>
 *     <td>Only TOML option; good for Rust interop</td>
 *   </tr>
 *   <tr>
 *     <td>Legacy XML systems</td>
 *     <td>JacksonXmlOps</td>
 *     <td>Only XML option; document format support</td>
 *   </tr>
 * </table>
 *
 * <h2>Cross-Format Conversion</h2>
 * <p>The {@link de.splatgames.aether.datafixers.benchmarks.format.CrossFormatBenchmark}
 * measures conversion overhead between formats. Key insights:</p>
 * <ul>
 *   <li><b>Same-ecosystem</b>: Jackson JSON ↔ Jackson YAML is fastest (shared JsonNode)</li>
 *   <li><b>Cross-ecosystem</b>: Gson ↔ SnakeYAML requires full tree traversal</li>
 *   <li><b>Asymmetry</b>: A→B may differ from B→A due to construction costs</li>
 * </ul>
 *
 * <h2>Interpreting Results</h2>
 * <ul>
 *   <li><b>Throughput</b>: Higher ops/sec is better for high-volume scenarios</li>
 *   <li><b>Average time</b>: Lower latency is better for interactive applications</li>
 *   <li><b>Scaling</b>: Compare SMALL vs MEDIUM vs LARGE to understand data volume impact</li>
 *   <li><b>Variance</b>: High ± values may indicate GC sensitivity or JIT instability</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.benchmarks.format.JsonBenchmark
 * @see de.splatgames.aether.datafixers.benchmarks.format.YamlBenchmark
 * @see de.splatgames.aether.datafixers.benchmarks.format.TomlXmlBenchmark
 * @see de.splatgames.aether.datafixers.benchmarks.format.CrossFormatBenchmark
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @since 1.0.0
 */
package de.splatgames.aether.datafixers.benchmarks.format;
