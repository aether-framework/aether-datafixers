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
 * Codec-focused JMH benchmarks for the Aether DataFixers framework.
 *
 * <p>This package contains benchmarks that measure the performance of codec operations,
 * including encoding (Java objects to serialized format) and decoding (serialized format
 * to Java objects). These benchmarks establish baseline performance for the codec system
 * and help identify bottlenecks in serialization pipelines.</p>
 *
 * <h2>Benchmark Classes</h2>
 * <table border="1">
 *   <caption>Codec benchmark classes and their focus areas</caption>
 *   <tr>
 *     <th>Class</th>
 *     <th>Focus Area</th>
 *     <th>Key Metrics</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.codec.PrimitiveCodecBenchmark}</td>
 *     <td>Primitive type codecs (bool, int, long, float, double, string)</td>
 *     <td>Baseline encode/decode latency, round-trip overhead</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.codec.CollectionCodecBenchmark}</td>
 *     <td>Collection codecs (List&lt;String&gt;, List&lt;Integer&gt;)</td>
 *     <td>Scaling with collection size, functional vs direct API overhead</td>
 *   </tr>
 * </table>
 *
 * <h2>Why Codec Benchmarks?</h2>
 * <p>Codecs are fundamental to the DataFixer system, transforming data between typed
 * Java objects and format-agnostic {@link de.splatgames.aether.datafixers.api.dynamic.Dynamic}
 * representations. Understanding codec performance is essential for:</p>
 * <ul>
 *   <li><b>Baseline establishment</b>: Primitive codecs set the lower bound for all
 *       codec operations; complex codecs compose these primitives</li>
 *   <li><b>Bottleneck identification</b>: Comparing encode vs decode reveals which
 *       direction is more expensive for a given type</li>
 *   <li><b>Scaling analysis</b>: Collection benchmarks show how performance changes
 *       with data volume</li>
 *   <li><b>API comparison</b>: Direct extraction vs functional composition may have
 *       different performance characteristics</li>
 * </ul>
 *
 * <h2>Running Codec Benchmarks</h2>
 * <pre>{@code
 * # Run all codec benchmarks
 * java -jar benchmarks.jar ".*codec.*"
 *
 * # Run only primitive codec benchmarks
 * java -jar benchmarks.jar PrimitiveCodecBenchmark
 *
 * # Run only collection codec benchmarks
 * java -jar benchmarks.jar CollectionCodecBenchmark
 *
 * # Run encode-only benchmarks across all codec types
 * java -jar benchmarks.jar ".*codec.*encode.*"
 *
 * # Run decode-only benchmarks
 * java -jar benchmarks.jar ".*codec.*decode.*"
 *
 * # Run round-trip benchmarks
 * java -jar benchmarks.jar ".*codec.*roundTrip.*"
 *
 * # Quick validation with reduced iterations
 * java -jar benchmarks.jar ".*codec.*" -wi 1 -i 1 -f 1
 *
 * # Generate CSV report for analysis
 * java -jar benchmarks.jar ".*codec.*" -rf csv -rff codec_results.csv
 * }</pre>
 *
 * <h2>Benchmark Design Principles</h2>
 * <ul>
 *   <li><b>Isolated operations</b>: Encode and decode are benchmarked separately to
 *       identify which direction is more expensive</li>
 *   <li><b>Pre-encoded data</b>: Decode benchmarks use pre-encoded JSON elements
 *       created during setup to avoid measuring encoding overhead</li>
 *   <li><b>Parameterization</b>: Collection sizes are parameterized to reveal
 *       scaling characteristics</li>
 *   <li><b>API styles</b>: Both direct extraction ({@code result().orElseThrow()})
 *       and functional composition ({@code flatMap}) are benchmarked for round-trips</li>
 *   <li><b>Time units</b>: Nanoseconds for primitives (sub-microsecond operations),
 *       microseconds for collections (longer operations)</li>
 * </ul>
 *
 * <h2>Interpreting Codec Results</h2>
 * <table border="1">
 *   <caption>Common result observations and their interpretations</caption>
 *   <tr><th>Observation</th><th>Meaning</th><th>Action</th></tr>
 *   <tr>
 *     <td>Encode slower than decode</td>
 *     <td>JSON element construction more expensive than extraction</td>
 *     <td>Consider caching encoded results if reused</td>
 *   </tr>
 *   <tr>
 *     <td>Decode slower than encode</td>
 *     <td>Type parsing/validation overhead dominates</td>
 *     <td>Review type conversion logic</td>
 *   </tr>
 *   <tr>
 *     <td>Super-linear collection scaling</td>
 *     <td>GC pressure or algorithmic inefficiency</td>
 *     <td>Profile memory allocation; consider streaming</td>
 *   </tr>
 *   <tr>
 *     <td>Functional API slower than direct</td>
 *     <td>Lambda/closure overhead measurable</td>
 *     <td>Use direct extraction for hot paths</td>
 *   </tr>
 *   <tr>
 *     <td>String codec slower than numeric</td>
 *     <td>String allocation/interning overhead</td>
 *     <td>Expected; no action needed</td>
 *   </tr>
 * </table>
 *
 * <h2>Relationship to Other Benchmarks</h2>
 * <p>Codec benchmarks complement other benchmark packages:</p>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.benchmarks.core core} - Uses codecs
 *       internally; codec performance affects fix application time</li>
 *   <li>{@link de.splatgames.aether.datafixers.benchmarks.concurrent concurrent} -
 *       Codec thread-safety is assumed; concurrent benchmarks validate this assumption</li>
 * </ul>
 *
 * <h2>Supported Serialization Formats</h2>
 * <p>These benchmarks use {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps}
 * as the reference DynamicOps implementation. The codec system supports multiple formats:</p>
 * <ul>
 *   <li><b>JSON</b>: GsonOps, JacksonJsonOps</li>
 *   <li><b>YAML</b>: SnakeYamlOps, JacksonYamlOps</li>
 *   <li><b>TOML</b>: JacksonTomlOps</li>
 *   <li><b>XML</b>: JacksonXmlOps</li>
 * </ul>
 * <p>Future benchmarks may compare performance across different DynamicOps implementations.</p>
 *
 * @see de.splatgames.aether.datafixers.benchmarks.codec.PrimitiveCodecBenchmark
 * @see de.splatgames.aether.datafixers.benchmarks.codec.CollectionCodecBenchmark
 * @see de.splatgames.aether.datafixers.api.codec.Codec
 * @see de.splatgames.aether.datafixers.api.codec.Codecs
 * @see de.splatgames.aether.datafixers.codec.json.gson.GsonOps
 * @since 1.0.0
 */
package de.splatgames.aether.datafixers.benchmarks.codec;
