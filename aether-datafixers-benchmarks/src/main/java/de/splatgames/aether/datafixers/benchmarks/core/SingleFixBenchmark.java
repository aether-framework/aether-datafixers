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

package de.splatgames.aether.datafixers.benchmarks.core;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator;
import de.splatgames.aether.datafixers.benchmarks.util.PayloadSize;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
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

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for single DataFix application performance.
 *
 * <p>Measures the overhead of applying a single fix to data of varying sizes.
 * Includes a baseline identity fix measurement to isolate framework overhead from actual transformation costs.</p>
 *
 * <h2>Benchmark Methods</h2>
 * <ul>
 *   <li>{@link #identityFix} - Baseline measurement with no-op transformation</li>
 *   <li>{@link #singleRenameFix} - Single field rename operation</li>
 *   <li>{@link #playerDataFix} - Complex object transformation with codec roundtrip</li>
 *   <li>{@link #playerDataFixEndToEnd} - Full pipeline including setup overhead</li>
 * </ul>
 *
 * <h2>Benchmark Configuration</h2>
 * <table border="1">
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
 *   <li><b>Throughput (ops/us)</b>: Higher is better. Operations per microsecond.</li>
 *   <li><b>Average Time (us/op)</b>: Lower is better. Microseconds per operation.</li>
 *   <li><b>Error (±)</b>: 99.9% confidence interval. Smaller means more stable results.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * # Run only this benchmark
 * java -jar benchmarks.jar SingleFixBenchmark
 *
 * # Quick test with reduced iterations
 * java -jar benchmarks.jar SingleFixBenchmark -wi 1 -i 1 -f 1
 *
 * # Specific payload size only
 * java -jar benchmarks.jar SingleFixBenchmark -p payloadSize=SMALL
 * }</pre>
 *
 * @author Erik Pförtner
 * @see BenchmarkBootstrap
 * @see BenchmarkDataGenerator
 * @see PayloadSize
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class SingleFixBenchmark {

    /**
     * Benchmarks a single field rename operation.
     *
     * <p>Measures the performance of renaming one field in the input data.
     * This represents a common, lightweight migration operation. The benchmark is parameterized by {@link PayloadSize}
     * to measure scaling behavior.</p>
     *
     * <p><b>Expected performance:</b> ~0.26-0.29 μs/op (sub-microsecond)</p>
     *
     * @param s         the shared benchmark state containing fixer and input data
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void singleRenameFix(final SizedState s, final Blackhole blackhole) {
        blackhole.consume(s.fixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                s.input,
                s.fromVersion,
                s.toVersion));
    }

    /**
     * Benchmarks the identity (no-op) fix as a baseline measurement.
     *
     * <p>Measures pure framework overhead without any actual data transformation.
     * Use this as a baseline to calculate the true cost of transformations by subtracting identity time from other
     * benchmark results.</p>
     *
     * <p><b>Expected performance:</b> ~0.24-0.25 μs/op (minimal framework overhead)</p>
     *
     * @param s         the shared benchmark state containing identity fixer and input data
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void identityFix(final SizedState s, final Blackhole blackhole) {
        blackhole.consume(s.identityFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                s.input,
                s.fromVersion,
                s.toVersion));
    }

    /**
     * Benchmarks a complex player data transformation with codec roundtrip.
     *
     * <p>Measures the performance of a realistic migration scenario where data
     * is decoded via codec, transformed, and re-encoded. This represents the upper bound of migration cost for complex
     * object transformations.</p>
     *
     * <p><b>Expected performance:</b> ~17-18 μs/op (significantly slower due to codec overhead)</p>
     *
     * <p>The ~70x slowdown compared to {@link #singleRenameFix} is expected and
     * acceptable, as codec roundtrips involve reflection, object instantiation, and full serialization/deserialization
     * cycles.</p>
     *
     * @param s         the shared player benchmark state
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void playerDataFix(final PlayerState s,
                              final Blackhole blackhole) {
        blackhole.consume(s.playerFixer.update(
                BenchmarkBootstrap.PLAYER_TYPE,
                s.playerInput,
                s.fromVersion,
                s.toVersion));
    }

    /**
     * Benchmarks the complete end-to-end pipeline including setup overhead.
     *
     * <p>Measures the total cost of a migration including:</p>
     * <ul>
     *   <li>Test data generation</li>
     *   <li>DataFixer bootstrap and initialization</li>
     *   <li>Actual migration execution</li>
     * </ul>
     *
     * <p>This benchmark is useful for understanding cold-start performance
     * and the cost of creating new DataFixer instances. In production code,
     * DataFixers should be reused rather than recreated per-operation.</p>
     *
     * <p><b>Note:</b> Results will be significantly slower than {@link #playerDataFix}
     * due to setup overhead included in each iteration.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void playerDataFixEndToEnd(final Blackhole blackhole) {
        final Dynamic<JsonElement> playerInput = BenchmarkDataGenerator.generatePlayerData(GsonOps.INSTANCE);
        final DataFixer playerFixer = BenchmarkBootstrap.createPlayerFixer();
        blackhole.consume(playerFixer.update(
                BenchmarkBootstrap.PLAYER_TYPE,
                playerInput,
                new DataVersion(1),
                new DataVersion(2)));
    }

    /**
     * Shared JMH state for benchmarks parameterized by payload size.
     *
     * <p>This state is shared across all threads within a benchmark trial
     * ({@link Scope#Benchmark}). The {@link #payloadSize} parameter controls the complexity of test data:</p>
     *
     * <ul>
     *   <li><b>SMALL</b>: 5 fields, 2 nesting levels, 10 array elements</li>
     *   <li><b>MEDIUM</b>: 20 fields, 4 nesting levels, 100 array elements</li>
     *   <li><b>LARGE</b>: 50 fields, 6 nesting levels, 1000 array elements</li>
     * </ul>
     *
     * @see PayloadSize
     */
    @State(Scope.Benchmark)
    public static class SizedState {

        /**
         * The payload size parameter, injected by JMH. Controls the complexity of generated test data.
         */
        @Param({"SMALL", "MEDIUM", "LARGE"})
        public PayloadSize payloadSize;

        /**
         * DataFixer configured with a single field rename fix (v1 → v2).
         */
        public DataFixer fixer;

        /**
         * DataFixer configured with an identity (no-op) fix for baseline measurement.
         */
        public DataFixer identityFixer;

        /**
         * Pre-generated input data matching {@link #payloadSize}.
         */
        public Dynamic<JsonElement> input;

        /**
         * Source version for migrations (v1).
         */
        public DataVersion fromVersion;

        /**
         * Target version for migrations (v2).
         */
        public DataVersion toVersion;

        /**
         * Initializes the benchmark state once per trial.
         *
         * <p>Creates fixers and generates test data based on the current
         * {@link #payloadSize} parameter value.</p>
         */
        @Setup(Level.Trial)
        public void setup() {
            this.fixer = BenchmarkBootstrap.createSingleFixFixer();
            this.identityFixer = BenchmarkBootstrap.createIdentityFixer();
            this.input = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, this.payloadSize);
            this.fromVersion = new DataVersion(1);
            this.toVersion = new DataVersion(2);
        }
    }

    /**
     * Shared JMH state for player-specific benchmarks.
     *
     * <p>This state is separate from {@link SizedState} because the player benchmark
     * uses a fixed, realistic data structure rather than parameterized payload sizes. The player data simulates a
     * typical game entity with nested objects, arrays, and various field types.</p>
     *
     * <p>The player fix performs a complete codec roundtrip transformation,
     * making it representative of real-world migration scenarios where data is decoded, transformed, and
     * re-encoded.</p>
     *
     * @see BenchmarkBootstrap#createPlayerFixer()
     * @see BenchmarkDataGenerator#generatePlayerData
     */
    @State(Scope.Benchmark)
    public static class PlayerState {

        /**
         * DataFixer configured with a player-specific transformation fix. Performs codec decode → transform → encode
         * cycle.
         */
        public DataFixer playerFixer;

        /**
         * Pre-generated player data structure with realistic game entity fields.
         */
        public Dynamic<JsonElement> playerInput;

        /**
         * Source version for migrations (v1).
         */
        public DataVersion fromVersion;

        /**
         * Target version for migrations (v2).
         */
        public DataVersion toVersion;

        /**
         * Initializes the player benchmark state once per trial.
         *
         * <p>Creates the player fixer and generates realistic player test data.</p>
         */
        @Setup(Level.Trial)
        public void setup() {
            this.playerFixer = BenchmarkBootstrap.createPlayerFixer();
            this.playerInput = BenchmarkDataGenerator.generatePlayerData(GsonOps.INSTANCE);
            this.fromVersion = new DataVersion(1);
            this.toVersion = new DataVersion(2);
        }
    }
}
