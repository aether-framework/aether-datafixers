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

package de.splatgames.aether.datafixers.benchmarks.core;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator;
import de.splatgames.aether.datafixers.benchmarks.util.PayloadSize;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmark for chained DataFix application performance.
 *
 * <p>Measures how fix chain length affects migration performance. This benchmark
 * is essential for understanding the scalability characteristics of the DataFixer
 * system when applying multiple sequential fixes.</p>
 *
 * <h2>Benchmark Methods</h2>
 * <ul>
 *   <li>{@link #renameChain} - Chain of homogeneous field rename operations</li>
 *   <li>{@link #mixedChain} - Chain of heterogeneous operations (renames, additions, transformations)</li>
 *   <li>{@link #partialChain} - Partial chain execution stopping at halfway version</li>
 * </ul>
 *
 * <h2>Parameters</h2>
 * <table border="1">
 *   <caption>Benchmark parameters and their tested values</caption>
 *   <tr><th>Parameter</th><th>Values</th><th>Description</th></tr>
 *   <tr><td>fixCount</td><td>1, 5, 10, 25, 50</td><td>Number of fixes in the chain</td></tr>
 *   <tr><td>payloadSize</td><td>SMALL, MEDIUM</td><td>Input data complexity</td></tr>
 * </table>
 *
 * <h2>Benchmark Configuration</h2>
 * <table border="1">
 *   <caption>JMH configuration settings for this benchmark</caption>
 *   <tr><th>Setting</th><th>Value</th></tr>
 *   <tr><td>Warmup</td><td>5 iterations, 1 second each</td></tr>
 *   <tr><td>Measurement</td><td>10 iterations, 1 second each</td></tr>
 *   <tr><td>Forks</td><td>2 (for statistical significance)</td></tr>
 *   <tr><td>JVM Heap</td><td>2 GB min/max</td></tr>
 *   <tr><td>Time Unit</td><td>Microseconds</td></tr>
 * </table>
 *
 * <h2>Interpreting Results</h2>
 * <ul>
 *   <li><b>Linear scaling</b>: Ideal behavior where time scales proportionally with fix count.</li>
 *   <li><b>Sub-linear scaling</b>: Better than expected, indicates optimization opportunities being exploited.</li>
 *   <li><b>Super-linear scaling</b>: Indicates potential performance issues with long chains.</li>
 *   <li><b>Error (±)</b>: 99.9% confidence interval. Larger values with more fixes may indicate GC pressure.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * # Run only this benchmark
 * java -jar benchmarks.jar MultiFixChainBenchmark
 *
 * # Quick test with reduced iterations
 * java -jar benchmarks.jar MultiFixChainBenchmark -wi 1 -i 1 -f 1
 *
 * # Specific fix count and payload size
 * java -jar benchmarks.jar MultiFixChainBenchmark -p fixCount=10 -p payloadSize=SMALL
 *
 * # Generate CSV output for analysis
 * java -jar benchmarks.jar MultiFixChainBenchmark -rf csv -rff chain_results.csv
 * }</pre>
 *
 * @author Erik Pförtner
 * @see SingleFixBenchmark
 * @see BenchmarkBootstrap#createChainFixer(int)
 * @see BenchmarkBootstrap#createMixedFixer(int)
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class MultiFixChainBenchmark {

    /**
     * The number of fixes in the chain, injected by JMH.
     *
     * <p>This parameter controls the length of the fix chain being benchmarked.
     * Higher values test the system's ability to handle long migration paths
     * efficiently.</p>
     *
     * <ul>
     *   <li><b>1</b>: Baseline single-fix performance (compare with {@link SingleFixBenchmark})</li>
     *   <li><b>5</b>: Short chain typical of minor version updates</li>
     *   <li><b>10</b>: Medium chain representing moderate version gaps</li>
     *   <li><b>25</b>: Long chain simulating significant version jumps</li>
     *   <li><b>50</b>: Stress test for extended migration paths</li>
     * </ul>
     */
    @Param({"1", "5", "10", "25", "50"})
    private int fixCount;

    /**
     * The payload size parameter, injected by JMH.
     *
     * <p>Controls the complexity of generated test data. Only SMALL and MEDIUM
     * sizes are used to keep benchmark runtime reasonable while still capturing
     * scaling behavior.</p>
     *
     * @see PayloadSize
     */
    @Param({"SMALL", "MEDIUM"})
    private PayloadSize payloadSize;

    /**
     * DataFixer configured with a chain of homogeneous field rename fixes.
     *
     * <p>Each fix in the chain performs a simple field rename operation (v{@code n} → v{@code n+1}).
     * This represents the best-case scenario for chain execution.</p>
     */
    private DataFixer chainFixer;

    /**
     * DataFixer configured with a chain of heterogeneous fix operations.
     *
     * <p>The chain includes a mix of rename, add, and transform operations to
     * simulate realistic migration scenarios. Falls back to {@link #chainFixer}
     * if mixed fixer creation fails.</p>
     */
    private DataFixer mixedFixer;

    /**
     * Pre-generated input data matching {@link #payloadSize}.
     *
     * <p>Regenerated at each iteration to ensure consistent GC behavior
     * and avoid caching effects.</p>
     */
    private Dynamic<JsonElement> input;

    /**
     * Source version for migrations (always v1).
     */
    private DataVersion fromVersion;

    /**
     * Target version for full chain migrations (v{@link #fixCount} + 1).
     */
    private DataVersion toVersion;

    /**
     * Target version for partial chain migrations (approximately half of {@link #toVersion}).
     *
     * <p>Used by {@link #partialChain} to measure performance when only part
     * of the available fixes are applied.</p>
     */
    private DataVersion halfwayToVersion;

    /**
     * Initializes the benchmark state once per trial.
     *
     * <p>Creates the chain and mixed fixers based on the current {@link #fixCount}
     * parameter. Also calculates the version bounds for full and partial chain
     * execution.</p>
     *
     * <p>If mixed fixer creation fails (e.g., due to unsupported operations),
     * the chain fixer is used as a fallback to ensure the benchmark can still run.</p>
     */
    @Setup(Level.Trial)
    public void setupTrial() {
        this.chainFixer = BenchmarkBootstrap.createChainFixer(this.fixCount);

        try {
            this.mixedFixer = BenchmarkBootstrap.createMixedFixer(this.fixCount);
        } catch (final RuntimeException ex) {
            this.mixedFixer = this.chainFixer;
        }

        this.fromVersion = new DataVersion(1);
        this.toVersion = new DataVersion(this.fixCount + 1);

        final int halfwayVersion = Math.max(2, (this.fixCount / 2) + 1);
        this.halfwayToVersion = new DataVersion(halfwayVersion);
    }

    /**
     * Regenerates input data at each iteration.
     *
     * <p>Fresh data generation per iteration ensures that:</p>
     * <ul>
     *   <li>GC behavior is consistent across iterations</li>
     *   <li>JIT optimizations don't over-specialize on specific data patterns</li>
     *   <li>Memory allocation patterns are representative of real usage</li>
     * </ul>
     */
    @Setup(Level.Iteration)
    public void setupIteration() {
        this.input = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, this.payloadSize);
    }

    /**
     * Benchmarks a chain of homogeneous field rename operations.
     *
     * <p>Measures the performance of applying {@link #fixCount} sequential rename
     * fixes to migrate data from v1 to v{@code fixCount+1}. This represents an
     * optimistic scenario where all fixes perform the same lightweight operation.</p>
     *
     * <p>Use this benchmark to establish baseline chain performance and detect
     * any non-linear scaling behavior in the fix application pipeline.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void renameChain(@NotNull final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.chainFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.input,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    /**
     * Benchmarks a chain of heterogeneous fix operations.
     *
     * <p>Measures the performance of applying {@link #fixCount} sequential fixes
     * that include a mix of operations:</p>
     * <ul>
     *   <li>Field renames</li>
     *   <li>Field additions with default values</li>
     *   <li>Field transformations (type conversions, value mappings)</li>
     * </ul>
     *
     * <p>This benchmark provides a more realistic performance profile compared
     * to {@link #renameChain}, as real-world migrations typically involve
     * diverse operations.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void mixedChain(@NotNull final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.mixedFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.input,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    /**
     * Benchmarks partial chain execution stopping at halfway version.
     *
     * <p>Measures the performance of applying only half of the available fixes
     * in the chain. This simulates scenarios where:</p>
     * <ul>
     *   <li>Data is migrated incrementally rather than to the latest version</li>
     *   <li>Target version is not the most recent available</li>
     *   <li>Partial upgrades are performed for compatibility reasons</li>
     * </ul>
     *
     * <p>Comparing this benchmark with {@link #renameChain} reveals whether
     * fix selection and version range calculations add significant overhead.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void partialChain(@NotNull final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.chainFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.input,
                this.fromVersion,
                this.halfwayToVersion
        );
        blackhole.consume(result);
    }
}
