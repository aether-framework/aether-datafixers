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
 * Utility classes for JMH benchmark infrastructure in the Aether DataFixers framework.
 *
 * <p>This package provides the foundational components that all benchmark classes depend on
 * for test data generation, DataFixer configuration, and payload management. These utilities
 * ensure consistent, reproducible benchmark conditions across different benchmark categories.</p>
 *
 * <h2>Package Contents</h2>
 * <table border="1">
 *   <caption>Utility classes provided by this package</caption>
 *   <tr>
 *     <th>Class</th>
 *     <th>Purpose</th>
 *     <th>Used By</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap}</td>
 *     <td>Factory for pre-configured DataFixer instances</td>
 *     <td>All migration benchmarks</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator}</td>
 *     <td>Factory for generating test data with configurable complexity</td>
 *     <td>All benchmarks requiring input data</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.benchmarks.util.PayloadSize}</td>
 *     <td>Configuration enum for data complexity levels</td>
 *     <td>JMH {@code @Param} annotations</td>
 *   </tr>
 * </table>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><b>Isolation</b>: Utilities are stateless and thread-safe for concurrent benchmark use</li>
 *   <li><b>Consistency</b>: All benchmarks use the same data generation logic for fair comparisons</li>
 *   <li><b>Configurability</b>: {@link de.splatgames.aether.datafixers.benchmarks.util.PayloadSize}
 *       allows parameterized benchmarks with different data volumes</li>
 *   <li><b>No-op context</b>: All DataFixers use {@code NoOpDataFixerContext} to eliminate
 *       logging overhead during measurements</li>
 * </ul>
 *
 * <h2>Typical Usage Pattern</h2>
 * <pre>{@code
 * @State(Scope.Benchmark)
 * public class MyBenchmark {
 *
 *     @Param({"SMALL", "MEDIUM", "LARGE"})
 *     private PayloadSize payloadSize;
 *
 *     private DataFixer fixer;
 *     private Dynamic<?> input;
 *
 *     @Setup(Level.Trial)
 *     public void setupTrial() {
 *         // Create fixer once per trial
 *         this.fixer = BenchmarkBootstrap.createChainFixer(10);
 *     }
 *
 *     @Setup(Level.Iteration)
 *     public void setupIteration() {
 *         // Regenerate data each iteration for consistent GC behavior
 *         this.input = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, payloadSize);
 *     }
 *
 *     @Benchmark
 *     public void migrate(Blackhole blackhole) {
 *         Dynamic<?> result = fixer.update(
 *             BenchmarkBootstrap.BENCHMARK_TYPE,
 *             input,
 *             new DataVersion(1),
 *             new DataVersion(11)
 *         );
 *         blackhole.consume(result);
 *     }
 * }
 * }</pre>
 *
 * <h2>Data Fixer Configurations</h2>
 * <p>{@link de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap} provides
 * several DataFixer configurations for different benchmark scenarios:</p>
 * <table border="1">
 *   <caption>Available DataFixer configurations</caption>
 *   <tr><th>Configuration</th><th>Fix Count</th><th>Purpose</th></tr>
 *   <tr><td>Single Fix</td><td>1</td><td>Baseline single-operation performance</td></tr>
 *   <tr><td>Identity</td><td>1 (no-op)</td><td>Framework overhead measurement</td></tr>
 *   <tr><td>Chain (N)</td><td>1-100</td><td>Chain length scaling analysis</td></tr>
 *   <tr><td>Mixed (N)</td><td>4+</td><td>Realistic heterogeneous migrations</td></tr>
 *   <tr><td>Player</td><td>4</td><td>Domain-specific scenario testing</td></tr>
 * </table>
 *
 * <h2>Payload Size Configurations</h2>
 * <p>{@link de.splatgames.aether.datafixers.benchmarks.util.PayloadSize} defines three
 * complexity levels for generated test data:</p>
 * <table border="1">
 *   <caption>Payload size levels and their characteristics</caption>
 *   <tr><th>Size</th><th>Fields</th><th>Nesting</th><th>List Items</th><th>Use Case</th></tr>
 *   <tr><td>SMALL</td><td>5</td><td>2</td><td>10</td><td>Quick iterations, CI</td></tr>
 *   <tr><td>MEDIUM</td><td>20</td><td>4</td><td>100</td><td>Standard testing</td></tr>
 *   <tr><td>LARGE</td><td>50</td><td>6</td><td>1000</td><td>Stress testing</td></tr>
 * </table>
 *
 * <h2>Integration with Testkit</h2>
 * <p>This package builds upon the {@code aether-datafixers-testkit} module:</p>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator} uses
 *       {@code TestDataBuilder} for fluent data construction</li>
 *   <li>{@link de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap} uses
 *       {@code QuickFix} for efficient fix creation</li>
 *   <li>Both utilities leverage {@code MockSchemas} for lightweight schema instances</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap
 * @see de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator
 * @see de.splatgames.aether.datafixers.benchmarks.util.PayloadSize
 * @see de.splatgames.aether.datafixers.testkit
 * @since 1.0.0
 */
package de.splatgames.aether.datafixers.benchmarks.util;
