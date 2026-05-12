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
 * JMH benchmark suite for the Aether DataFixers framework.
 *
 * <p>This package and its sub-packages provide comprehensive performance benchmarks
 * for all major components of the Aether DataFixers system. The benchmarks use
 * <a href="https://openjdk.org/projects/code-tools/jmh/">JMH (Java Microbenchmark Harness)</a>
 * for accurate, reliable performance measurements.</p>
 *
 * <h2>Package Structure</h2>
 * <table border="1">
 *   <caption>Benchmark packages and their focus areas</caption>
 *   <tr>
 *     <th>Package</th>
 *     <th>Focus Area</th>
 *     <th>Key Benchmarks</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.core core}</td>
 *     <td>DataFixer migration performance</td>
 *     <td>SingleFixBenchmark, MultiFixChainBenchmark, SchemaLookupBenchmark</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.codec codec}</td>
 *     <td>Codec encode/decode performance</td>
 *     <td>PrimitiveCodecBenchmark, CollectionCodecBenchmark</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.concurrent concurrent}</td>
 *     <td>Thread-safety and scalability</td>
 *     <td>ConcurrentMigrationBenchmark</td>
 *   </tr>
 *   <tr>
 *     <td>{@code format}</td>
 *     <td>DynamicOps format comparisons</td>
 *     <td>JsonBenchmark, YamlBenchmark, CrossFormatBenchmark</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.util util}</td>
 *     <td>Benchmark infrastructure</td>
 *     <td>BenchmarkBootstrap, BenchmarkDataGenerator, PayloadSize</td>
 *   </tr>
 * </table>
 *
 * <h2>Running Benchmarks</h2>
 *
 * <h3>Via Maven (Development)</h3>
 * <pre>{@code
 * # Run all benchmarks
 * mvn exec:java -pl aether-datafixers-benchmarks
 *
 * # Run specific benchmark
 * mvn exec:java -pl aether-datafixers-benchmarks -Dexec.args="SingleFixBenchmark"
 * }</pre>
 *
 * <h3>Via Fat JAR (Production)</h3>
 * <pre>{@code
 * # Build the benchmark JAR
 * mvn clean package -pl aether-datafixers-benchmarks -DskipTests
 *
 * # Run all benchmarks
 * java -jar target/aether-datafixers-benchmarks-*-benchmarks.jar
 *
 * # List available benchmarks
 * java -jar target/*-benchmarks.jar -l
 *
 * # Run with parameters
 * java -jar target/*-benchmarks.jar -p payloadSize=LARGE -wi 5 -i 10 -f 2
 *
 * # Output JSON results
 * java -jar target/*-benchmarks.jar -rf json -rff results.json
 * }</pre>
 *
 * <h3>Programmatic API</h3>
 * <pre>{@code
 * // Run all benchmarks
 * BenchmarkRunner.runAllBenchmarks();
 *
 * // Run quick validation
 * BenchmarkRunner.runQuickBenchmarks();
 *
 * // Run specific category
 * BenchmarkRunner.runCoreBenchmarks();
 * BenchmarkRunner.runFormatBenchmarks();
 * }</pre>
 *
 * <h2>Benchmark Categories Explained</h2>
 *
 * <h3>Core Benchmarks</h3>
 * <p>Measure the fundamental DataFixer operations:</p>
 * <ul>
 *   <li><b>Single fix</b>: Baseline performance for one migration step</li>
 *   <li><b>Chain migration</b>: How performance scales with migration path length</li>
 *   <li><b>Schema lookup</b>: Registry access patterns and caching effectiveness</li>
 * </ul>
 *
 * <h3>Codec Benchmarks</h3>
 * <p>Measure serialization and deserialization performance:</p>
 * <ul>
 *   <li><b>Primitive codecs</b>: Baseline for bool, int, long, float, double, string</li>
 *   <li><b>Collection codecs</b>: List encoding/decoding with size scaling</li>
 *   <li><b>Round-trip</b>: Combined encode + decode performance</li>
 * </ul>
 *
 * <h3>Concurrent Benchmarks</h3>
 * <p>Validate thread-safety and measure scalability:</p>
 * <ul>
 *   <li><b>Multi-threaded migration</b>: Contention under concurrent load</li>
 *   <li><b>Registry access</b>: Concurrent read performance</li>
 *   <li><b>Scaling analysis</b>: Fixed thread counts (4, 8, MAX)</li>
 * </ul>
 *
 * <h3>Format Benchmarks</h3>
 * <p>Compare different DynamicOps implementations:</p>
 * <ul>
 *   <li><b>JSON</b>: GsonOps vs JacksonJsonOps</li>
 *   <li><b>YAML</b>: SnakeYamlOps vs JacksonYamlOps</li>
 *   <li><b>Other</b>: TOML and XML via Jackson</li>
 *   <li><b>Cross-format</b>: Conversion between formats</li>
 * </ul>
 *
 * <h2>Default Configuration</h2>
 * <table border="1">
 *   <caption>Default JMH benchmark configuration</caption>
 *   <tr><th>Setting</th><th>Value</th><th>Purpose</th></tr>
 *   <tr><td>Warmup iterations</td><td>5</td><td>JIT compilation stabilization</td></tr>
 *   <tr><td>Measurement iterations</td><td>10</td><td>Statistical significance</td></tr>
 *   <tr><td>Forks</td><td>2</td><td>JVM variance mitigation</td></tr>
 *   <tr><td>JVM heap</td><td>2 GB</td><td>Avoid GC interference</td></tr>
 *   <tr><td>Time unit</td><td>Varies</td><td>ns for primitives, µs for complex ops</td></tr>
 * </table>
 *
 * <h2>Interpreting Results</h2>
 * <ul>
 *   <li><b>Throughput (ops/time)</b>: Higher is better; measures operation rate</li>
 *   <li><b>Average time (time/op)</b>: Lower is better; measures latency</li>
 *   <li><b>Error (±)</b>: 99.9% confidence interval; smaller is more reliable</li>
 *   <li><b>Scaling</b>: Compare across parameter values (payload size, thread count)</li>
 * </ul>
 *
 * <h2>Common JMH Options</h2>
 * <table border="1">
 *   <caption>Common command-line options accepted by JMH</caption>
 *   <tr><th>Option</th><th>Description</th></tr>
 *   <tr><td>{@code -wi N}</td><td>Number of warmup iterations</td></tr>
 *   <tr><td>{@code -i N}</td><td>Number of measurement iterations</td></tr>
 *   <tr><td>{@code -f N}</td><td>Number of forks (JVM instances)</td></tr>
 *   <tr><td>{@code -t N}</td><td>Number of threads</td></tr>
 *   <tr><td>{@code -p key=value}</td><td>Set parameter value</td></tr>
 *   <tr><td>{@code -rf format}</td><td>Result format (json, csv, text)</td></tr>
 *   <tr><td>{@code -rff file}</td><td>Result output file</td></tr>
 *   <tr><td>{@code -prof profiler}</td><td>Enable profiler (gc, async, jfr)</td></tr>
 *   <tr><td>{@code -l}</td><td>List available benchmarks</td></tr>
 *   <tr><td>{@code -h}</td><td>Show help</td></tr>
 * </table>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li><b>Isolated environment</b>: Run on dedicated hardware with minimal background processes</li>
 *   <li><b>Multiple forks</b>: Use at least 2 forks for reliable results</li>
 *   <li><b>Sufficient warmup</b>: Allow JIT compilation to stabilize before measurement</li>
 *   <li><b>Consistent conditions</b>: Compare results from the same machine and JVM version</li>
 *   <li><b>Statistical analysis</b>: Consider error margins when comparing results</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.benchmarks.BenchmarkRunner
 * @see de.splatgames.aether.datafixers.benchmarks.core
 * @see de.splatgames.aether.datafixers.benchmarks.codec
 * @see de.splatgames.aether.datafixers.benchmarks.concurrent
 * @see de.splatgames.aether.datafixers.benchmarks.util
 * @since 1.0.0
 */
package de.splatgames.aether.datafixers.benchmarks;
