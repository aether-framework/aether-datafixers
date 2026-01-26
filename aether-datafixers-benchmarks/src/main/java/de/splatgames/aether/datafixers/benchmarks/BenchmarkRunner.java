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
 * <p>This class provides a convenient way to run benchmarks programmatically
 * with default settings optimized for comprehensive performance analysis.</p>
 *
 * <h2>Usage</h2>
 *
 * <h3>Via exec:java (Quick Development Runs)</h3>
 * <pre>{@code
 * # Run all benchmarks with default settings
 * mvn exec:java -pl aether-datafixers-benchmarks
 *
 * # Run with JMH arguments
 * mvn exec:java -pl aether-datafixers-benchmarks -Dexec.args="-h"
 * }</pre>
 *
 * <h3>Via Fat JAR (Production Runs)</h3>
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
 * # Output JSON results
 * java -jar target/*-benchmarks.jar -rf json -rff results.json
 *
 * # List all available benchmarks
 * java -jar target/*-benchmarks.jar -l
 * }</pre>
 *
 * <h2>Available Benchmarks</h2>
 * <ul>
 *   <li><b>Core</b>: SingleFixBenchmark, MultiFixChainBenchmark, SchemaLookupBenchmark</li>
 *   <li><b>Format</b>: JsonBenchmark, YamlBenchmark, TomlXmlBenchmark, CrossFormatBenchmark</li>
 *   <li><b>Codec</b>: PrimitiveCodecBenchmark, CollectionCodecBenchmark</li>
 *   <li><b>Concurrent</b>: ConcurrentMigrationBenchmark</li>
 * </ul>
 *
 * <h2>Default Configuration</h2>
 * <ul>
 *   <li>Warmup: 5 iterations, 1 second each</li>
 *   <li>Measurement: 10 iterations, 1 second each</li>
 *   <li>Forks: 2 (for statistical significance)</li>
 *   <li>JVM heap: 2GB min/max</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
public final class BenchmarkRunner {

    private BenchmarkRunner() {
        // Main class
    }

    /**
     * Main entry point for running benchmarks.
     *
     * <p>When run without arguments, executes all benchmarks in the package.
     * Supports all standard JMH command-line arguments.</p>
     *
     * @param args command-line arguments (passed to JMH)
     * @throws RunnerException if benchmark execution fails
     * @throws IOException     if there is an I/O error
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
     * Runs all benchmarks with default configuration.
     *
     * @throws RunnerException if benchmark execution fails
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
     * Runs a quick subset of benchmarks for validation.
     *
     * <p>Useful for CI/CD pipelines or quick sanity checks.</p>
     *
     * @throws RunnerException if benchmark execution fails
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
     * Runs core migration benchmarks only.
     *
     * @throws RunnerException if benchmark execution fails
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
     * Runs format comparison benchmarks only.
     *
     * @throws RunnerException if benchmark execution fails
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
