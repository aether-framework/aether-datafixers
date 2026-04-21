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
 * Concurrency-focused JMH benchmarks for the Aether DataFixers framework.
 *
 * <p>This package contains benchmarks that measure performance characteristics under
 * concurrent load. These benchmarks validate thread-safety of the DataFixer system,
 * identify contention points, and quantify scalability across different thread counts.</p>
 *
 * <h2>Benchmark Classes</h2>
 * <table border="1">
 *   <caption>Concurrent benchmark classes and their focus areas</caption>
 *   <tr>
 *     <th>Class</th>
 *     <th>Focus Area</th>
 *     <th>Key Metrics</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.concurrent.ConcurrentMigrationBenchmark}</td>
 *     <td>Multi-threaded migration and registry access</td>
 *     <td>Throughput scaling, contention overhead, thread-safety validation</td>
 *   </tr>
 * </table>
 *
 * <h2>Why Concurrent Benchmarks?</h2>
 * <p>Single-threaded benchmarks measure raw operation performance, but real-world
 * applications often use the DataFixer system from multiple threads simultaneously.
 * Concurrent benchmarks reveal:</p>
 * <ul>
 *   <li><b>Lock contention</b>: Synchronization overhead in shared components</li>
 *   <li><b>Cache coherency effects</b>: Performance impact of shared data access</li>
 *   <li><b>Scalability limits</b>: Point at which adding threads stops improving throughput</li>
 *   <li><b>Thread-safety validation</b>: Correctness under concurrent access</li>
 * </ul>
 *
 * <h2>Running Concurrent Benchmarks</h2>
 * <pre>{@code
 * # Run all concurrent benchmarks with maximum threads
 * java -jar benchmarks.jar ".*concurrent.*"
 *
 * # Run with specific thread count
 * java -jar benchmarks.jar ".*concurrent.*" -t 8
 *
 * # Quick validation with reduced iterations
 * java -jar benchmarks.jar ".*concurrent.*" -wi 1 -i 1 -f 1
 *
 * # Generate detailed JSON report
 * java -jar benchmarks.jar ".*concurrent.*" -rf json -rff concurrent_results.json
 *
 * # Profile lock contention with JFR
 * java -jar benchmarks.jar ".*concurrent.*" -prof jfr
 * }</pre>
 *
 * <h2>Benchmark Design Principles</h2>
 * <ul>
 *   <li><b>State Isolation</b>: Per-thread state ({@code Scope.Thread}) for input data
 *       prevents false sharing and measurement interference</li>
 *   <li><b>Shared Resources</b>: Benchmark-scoped state ({@code Scope.Benchmark}) for
 *       DataFixer instances simulates realistic concurrent access patterns</li>
 *   <li><b>Contention-Free Setup</b>: Random number generation and data preparation
 *       occur during setup phases to avoid affecting measurements</li>
 *   <li><b>Fixed Thread Counts</b>: Benchmarks with 4 and 8 threads provide
 *       reproducible scaling data points for comparison</li>
 * </ul>
 *
 * <h2>Interpreting Concurrent Results</h2>
 * <p>Concurrent benchmark results require careful interpretation:</p>
 * <table border="1">
 *   <caption>Concurrent result patterns and recommended actions</caption>
 *   <tr><th>Pattern</th><th>Meaning</th><th>Action</th></tr>
 *   <tr>
 *     <td>Linear throughput scaling</td>
 *     <td>No contention; excellent parallelism</td>
 *     <td>None needed</td>
 *   </tr>
 *   <tr>
 *     <td>Sub-linear scaling</td>
 *     <td>Some contention; typical for shared resources</td>
 *     <td>Acceptable; monitor for degradation</td>
 *   </tr>
 *   <tr>
 *     <td>Throughput plateau</td>
 *     <td>Saturation point reached</td>
 *     <td>Identify bottleneck (CPU, memory, locks)</td>
 *   </tr>
 *   <tr>
 *     <td>Throughput degradation</td>
 *     <td>Severe contention; adding threads hurts</td>
 *     <td>Investigate locking; consider lock-free alternatives</td>
 *   </tr>
 *   <tr>
 *     <td>High variance (± error)</td>
 *     <td>GC pauses, lock contention, or scheduling</td>
 *     <td>Profile with async-profiler or JFR</td>
 *   </tr>
 * </table>
 *
 * <h2>Comparison with Core Benchmarks</h2>
 * <p>The {@link de.splatgames.aether.datafixers.benchmarks.core core} package
 * measures single-threaded baseline performance. Use concurrent benchmarks to:</p>
 * <ul>
 *   <li>Calculate <b>concurrency overhead</b>: {@code (single-threaded throughput × N threads) / actual throughput}</li>
 *   <li>Identify <b>scaling efficiency</b>: {@code actual throughput / (single-threaded throughput × N threads)}</li>
 *   <li>Detect <b>regression</b>: Compare concurrent results across code changes</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.benchmarks.concurrent.ConcurrentMigrationBenchmark
 * @see de.splatgames.aether.datafixers.benchmarks.core
 * @see de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap
 * @since 1.0.0
 */
package de.splatgames.aether.datafixers.benchmarks.concurrent;
