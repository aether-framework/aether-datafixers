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

package de.splatgames.aether.datafixers.benchmarks;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;

/**
 * Main entry point for running Aether Datafixers JMH benchmarks.
 *
 * <p>This class provides both a command-line interface and programmatic API for
 * executing benchmarks. It supports all standard JMH options while providing
 * convenient preset configurations for common benchmark scenarios.</p>
 *
 * <h2>Execution Methods</h2>
 *
 * <h3>Via Maven exec:java (Development)</h3>
 * <p>Quick way to run benchmarks during development without building a JAR:</p>
 * <pre>{@code
 * # Run all benchmarks with default settings
 * mvn exec:java -pl aether-datafixers-benchmarks
 *
 * # Run with JMH arguments
 * mvn exec:java -pl aether-datafixers-benchmarks -Dexec.args="-h"
 *
 * # Run specific benchmark pattern
 * mvn exec:java -pl aether-datafixers-benchmarks -Dexec.args="SingleFixBenchmark"
 * }</pre>
 *
 * <h3>Via Fat JAR (Production)</h3>
 * <p>Recommended for production benchmark runs with full JMH isolation:</p>
 * <pre>{@code
 * # Build the fat JAR
 * mvn clean package -pl aether-datafixers-benchmarks -DskipTests
 *
 * # Run all benchmarks
 * java -jar aether-datafixers-benchmarks/target/aether-datafixers-benchmarks-*-benchmarks.jar
 *
 * # Run specific benchmark
 * java -jar target/*-benchmarks.jar SingleFixBenchmark
 *
 * # Run with custom parameters
 * java -jar target/*-benchmarks.jar -p payloadSize=LARGE -wi 3 -i 5 -f 1
 *
 * # Output JSON results for analysis
 * java -jar target/*-benchmarks.jar -rf json -rff results.json
 *
 * # List all available benchmarks
 * java -jar target/*-benchmarks.jar -l
 *
 * # Profile with async-profiler
 * java -jar target/*-benchmarks.jar -prof async:output=flamegraph
 * }</pre>
 *
 * <h2>Available Benchmark Categories</h2>
 * <table border="1">
 *   <tr><th>Category</th><th>Benchmarks</th><th>Focus</th></tr>
 *   <tr>
 *     <td><b>Core</b></td>
 *     <td>SingleFixBenchmark, MultiFixChainBenchmark, SchemaLookupBenchmark</td>
 *     <td>DataFixer migration performance</td>
 *   </tr>
 *   <tr>
 *     <td><b>Format</b></td>
 *     <td>JsonBenchmark, YamlBenchmark, TomlXmlBenchmark, CrossFormatBenchmark</td>
 *     <td>DynamicOps format comparisons</td>
 *   </tr>
 *   <tr>
 *     <td><b>Codec</b></td>
 *     <td>PrimitiveCodecBenchmark, CollectionCodecBenchmark</td>
 *     <td>Serialization/deserialization</td>
 *   </tr>
 *   <tr>
 *     <td><b>Concurrent</b></td>
 *     <td>ConcurrentMigrationBenchmark</td>
 *     <td>Thread-safety and scalability</td>
 *   </tr>
 * </table>
 *
 * <h2>Programmatic API</h2>
 * <p>For integration with test frameworks or custom tooling:</p>
 * <pre>{@code
 * // Run all benchmarks
 * BenchmarkRunner.runAllBenchmarks();
 *
 * // Run quick validation (CI/CD)
 * BenchmarkRunner.runQuickBenchmarks();
 *
 * // Run only core benchmarks
 * BenchmarkRunner.runCoreBenchmarks();
 *
 * // Run only format benchmarks
 * BenchmarkRunner.runFormatBenchmarks();
 * }</pre>
 *
 * <h2>Default Configuration</h2>
 * <table border="1">
 *   <tr><th>Setting</th><th>Default</th><th>Quick Mode</th></tr>
 *   <tr><td>Warmup iterations</td><td>5</td><td>2</td></tr>
 *   <tr><td>Measurement iterations</td><td>10</td><td>3</td></tr>
 *   <tr><td>Forks</td><td>2</td><td>1</td></tr>
 *   <tr><td>JVM heap</td><td>2 GB</td><td>1 GB</td></tr>
 * </table>
 *
 * <h2>Common JMH Options</h2>
 * <table border="1">
 *   <tr><th>Option</th><th>Description</th><th>Example</th></tr>
 *   <tr><td>{@code -wi}</td><td>Warmup iterations</td><td>{@code -wi 3}</td></tr>
 *   <tr><td>{@code -i}</td><td>Measurement iterations</td><td>{@code -i 5}</td></tr>
 *   <tr><td>{@code -f}</td><td>Number of forks</td><td>{@code -f 1}</td></tr>
 *   <tr><td>{@code -p}</td><td>Parameter value</td><td>{@code -p payloadSize=SMALL}</td></tr>
 *   <tr><td>{@code -t}</td><td>Thread count</td><td>{@code -t 4}</td></tr>
 *   <tr><td>{@code -rf}</td><td>Result format</td><td>{@code -rf json}</td></tr>
 *   <tr><td>{@code -rff}</td><td>Result file</td><td>{@code -rff results.json}</td></tr>
 *   <tr><td>{@code -l}</td><td>List benchmarks</td><td>{@code -l}</td></tr>
 *   <tr><td>{@code -prof}</td><td>Profiler</td><td>{@code -prof gc}</td></tr>
 * </table>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.benchmarks.core
 * @see de.splatgames.aether.datafixers.benchmarks.codec
 * @see de.splatgames.aether.datafixers.benchmarks.concurrent
 * @since 1.0.0
 */
public final class BenchmarkRunner {

    /**
     * Private constructor to prevent instantiation.
     */
    private BenchmarkRunner() {
        // Main class
    }

    /**
     * Main entry point for running benchmarks from the command line.
     *
     * <p>Behavior depends on whether arguments are provided:</p>
     * <ul>
     *   <li><b>With arguments</b>: Delegates to JMH's main method, supporting all
     *       standard JMH command-line options</li>
     *   <li><b>Without arguments</b>: Runs all benchmarks using default configuration
     *       via {@link #runAllBenchmarks()}</li>
     * </ul>
     *
     * <h3>Exit Codes</h3>
     * <ul>
     *   <li>0 - Successful completion</li>
     *   <li>Non-zero - Error during benchmark execution</li>
     * </ul>
     *
     * @param args command-line arguments (passed directly to JMH if present)
     * @throws RunnerException if benchmark execution fails
     * @throws IOException     if there is an I/O error reading benchmark metadata
     */
    public static void main(final String[] args) throws RunnerException, IOException {
        if (args.length > 0) {
            // If arguments are provided, delegate to JMH main
            org.openjdk.jmh.Main.main(args);
        } else {
            // Run with default options
            runAllBenchmarks();
        }
    }

    /**
     * Runs all benchmarks in the benchmarks package with default configuration.
     *
     * <p>Executes every benchmark class in
     * {@code de.splatgames.aether.datafixers.benchmarks.*} with production-quality
     * settings suitable for reliable performance measurements.</p>
     *
     * <h3>Configuration</h3>
     * <ul>
     *   <li>Warmup: 5 iterations</li>
     *   <li>Measurement: 10 iterations</li>
     *   <li>Forks: 2 (for JIT variance mitigation)</li>
     *   <li>JVM heap: 2 GB min/max</li>
     * </ul>
     *
     * <p><b>Note:</b> Running all benchmarks can take significant time depending
     * on the number of parameter combinations. Consider using
     * {@link #runQuickBenchmarks()} for validation or {@link #runCoreBenchmarks()}
     * for focused testing.</p>
     *
     * @throws RunnerException if benchmark execution fails
     * @see #runQuickBenchmarks()
     * @see #runCoreBenchmarks()
     */
    public static void runAllBenchmarks() throws RunnerException {
        final Options options = new OptionsBuilder()
                .include("de\\.splatgames\\.aether\\.datafixers\\.benchmarks\\..*")
                .warmupIterations(5)
                .measurementIterations(10)
                .forks(2)
                .jvmArgs("-Xms2G", "-Xmx2G")
                .build();

        new Runner(options).run();
    }

    /**
     * Runs a quick subset of benchmarks for fast validation.
     *
     * <p>Executes only the {@code SingleFixBenchmark} with minimal iterations,
     * suitable for:</p>
     * <ul>
     *   <li>CI/CD pipeline smoke tests</li>
     *   <li>Quick sanity checks during development</li>
     *   <li>Verifying benchmark infrastructure works correctly</li>
     * </ul>
     *
     * <h3>Configuration</h3>
     * <ul>
     *   <li>Benchmark: SingleFixBenchmark only</li>
     *   <li>Warmup: 2 iterations</li>
     *   <li>Measurement: 3 iterations</li>
     *   <li>Forks: 1 (faster but less statistically robust)</li>
     *   <li>JVM heap: 1 GB min/max</li>
     *   <li>Payload size: SMALL only</li>
     * </ul>
     *
     * <p><b>Warning:</b> Results from quick benchmarks should not be used for
     * performance comparisons due to reduced statistical rigor.</p>
     *
     * @throws RunnerException if benchmark execution fails
     * @see #runAllBenchmarks()
     */
    public static void runQuickBenchmarks() throws RunnerException {
        final Options options = new OptionsBuilder()
                .include("de\\.splatgames\\.aether\\.datafixers\\.benchmarks\\.core\\.SingleFixBenchmark")
                .warmupIterations(2)
                .measurementIterations(3)
                .forks(1)
                .jvmArgs("-Xms1G", "-Xmx1G")
                .param("payloadSize", "SMALL")
                .build();

        new Runner(options).run();
    }

    /**
     * Runs only the core migration benchmarks.
     *
     * <p>Executes benchmarks in the {@code core} package that measure DataFixer
     * migration performance:</p>
     * <ul>
     *   <li>{@code SingleFixBenchmark} - Single fix application performance</li>
     *   <li>{@code MultiFixChainBenchmark} - Chain migration scaling</li>
     *   <li>{@code SchemaLookupBenchmark} - Schema registry lookup performance</li>
     * </ul>
     *
     * <h3>Configuration</h3>
     * <ul>
     *   <li>Warmup: 5 iterations</li>
     *   <li>Measurement: 10 iterations</li>
     *   <li>Forks: 2</li>
     *   <li>JVM heap: 2 GB min/max</li>
     * </ul>
     *
     * <p>Use this method when focusing on migration performance without
     * format-specific or codec overhead considerations.</p>
     *
     * @throws RunnerException if benchmark execution fails
     * @see #runFormatBenchmarks()
     * @see #runAllBenchmarks()
     */
    public static void runCoreBenchmarks() throws RunnerException {
        final Options options = new OptionsBuilder()
                .include("de\\.splatgames\\.aether\\.datafixers\\.benchmarks\\.core\\..*")
                .warmupIterations(5)
                .measurementIterations(10)
                .forks(2)
                .jvmArgs("-Xms2G", "-Xmx2G")
                .build();

        new Runner(options).run();
    }

    /**
     * Runs only the format comparison benchmarks.
     *
     * <p>Executes benchmarks in the {@code format} package that compare different
     * DynamicOps implementations:</p>
     * <ul>
     *   <li>{@code JsonBenchmark} - GsonOps vs JacksonJsonOps</li>
     *   <li>{@code YamlBenchmark} - SnakeYamlOps vs JacksonYamlOps</li>
     *   <li>{@code TomlXmlBenchmark} - JacksonTomlOps and JacksonXmlOps</li>
     *   <li>{@code CrossFormatBenchmark} - Format conversion performance</li>
     * </ul>
     *
     * <h3>Configuration</h3>
     * <ul>
     *   <li>Warmup: 5 iterations</li>
     *   <li>Measurement: 10 iterations</li>
     *   <li>Forks: 2</li>
     *   <li>JVM heap: 2 GB min/max</li>
     * </ul>
     *
     * <p>Use this method when evaluating which DynamicOps implementation
     * to use for a specific use case, or when optimizing format handling.</p>
     *
     * @throws RunnerException if benchmark execution fails
     * @see #runCoreBenchmarks()
     * @see #runAllBenchmarks()
     */
    public static void runFormatBenchmarks() throws RunnerException {
        final Options options = new OptionsBuilder()
                .include("de\\.splatgames\\.aether\\.datafixers\\.benchmarks\\.format\\..*")
                .warmupIterations(5)
                .measurementIterations(10)
                .forks(2)
                .jvmArgs("-Xms2G", "-Xmx2G")
                .build();

        new Runner(options).run();
    }
}
