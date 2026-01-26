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
 * Core JMH benchmarks for the Aether DataFixers framework.
 *
 * <p>This package contains benchmarks that measure the fundamental performance characteristics
 * of the data fixer system, including fix application, chain execution, and schema registry
 * operations. These benchmarks form the foundation for performance regression testing and
 * optimization efforts.</p>
 *
 * <h2>Benchmark Classes</h2>
 * <table border="1">
 *   <tr>
 *     <th>Class</th>
 *     <th>Focus Area</th>
 *     <th>Key Metrics</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.core.SingleFixBenchmark}</td>
 *     <td>Single fix application</td>
 *     <td>Per-fix overhead, payload size scaling</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.core.MultiFixChainBenchmark}</td>
 *     <td>Chained fix execution</td>
 *     <td>Chain length scaling, partial migration cost</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.core.SchemaLookupBenchmark}</td>
 *     <td>Schema registry operations</td>
 *     <td>Lookup latency, registry size scaling</td>
 *   </tr>
 * </table>
 *
 * <h2>Running Benchmarks</h2>
 * <pre>{@code
 * # Run all core benchmarks
 * java -jar benchmarks.jar ".*core.*"
 *
 * # Run with specific JVM options
 * java -jar benchmarks.jar ".*core.*" -jvmArgs "-XX:+UseG1GC"
 *
 * # Generate JSON report
 * java -jar benchmarks.jar ".*core.*" -rf json -rff core_results.json
 * }</pre>
 *
 * <h2>Benchmark Design Principles</h2>
 * <ul>
 *   <li><b>Isolation</b>: Each benchmark measures a single operation to isolate performance characteristics.</li>
 *   <li><b>Parameterization</b>: Benchmarks are parameterized to capture scaling behavior across different input sizes.</li>
 *   <li><b>Reproducibility</b>: Fixed seeds and deterministic data generation ensure reproducible results.</li>
 *   <li><b>JMH Best Practices</b>: All benchmarks follow JMH guidelines including proper use of {@code Blackhole},
 *       state scoping, and setup level annotations.</li>
 * </ul>
 *
 * <h2>Interpreting Results</h2>
 * <p>All benchmarks in this package report both throughput (ops/time) and average time (time/op).
 * When comparing results:</p>
 * <ul>
 *   <li>Compare measurements from the same JVM version and hardware</li>
 *   <li>Consider the 99.9% confidence interval (error bounds)</li>
 *   <li>Run multiple forks to account for JIT compilation variance</li>
 *   <li>Use baseline benchmarks (e.g., identity fix) to isolate framework overhead</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap
 * @see de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator
 * @since 1.0.0
 */
package de.splatgames.aether.datafixers.benchmarks.core;
