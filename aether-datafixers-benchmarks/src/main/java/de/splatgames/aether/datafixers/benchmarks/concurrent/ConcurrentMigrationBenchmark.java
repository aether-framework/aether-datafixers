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

package de.splatgames.aether.datafixers.benchmarks.concurrent;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator;
import de.splatgames.aether.datafixers.benchmarks.util.PayloadSize;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for concurrent DataFixer operations and thread-safety validation.
 *
 * <p>This benchmark measures the performance characteristics of the DataFixer system
 * under concurrent load. It validates thread-safety of shared components and quantifies
 * scalability across different thread counts. The results help identify contention
 * points and ensure the framework performs well in multi-threaded environments.</p>
 *
 * <h2>Benchmark Categories</h2>
 *
 * <h3>Concurrent Migration Benchmarks</h3>
 * <p>Measure DataFixer performance when multiple threads perform migrations simultaneously:</p>
 * <ul>
 *   <li>{@link #concurrentSingleFix} - Maximum parallelism with single-fix migrations</li>
 *   <li>{@link #concurrentChainMigration} - Maximum parallelism with 10-fix chain migrations</li>
 *   <li>{@link #fourThreadMigration} - Fixed 4-thread migration for baseline comparison</li>
 *   <li>{@link #eightThreadMigration} - Fixed 8-thread migration for scaling analysis</li>
 * </ul>
 *
 * <h3>Concurrent Registry Access Benchmarks</h3>
 * <p>Measure SchemaRegistry performance under concurrent read pressure:</p>
 * <ul>
 *   <li>{@link #concurrentRegistryLookup} - Random version lookups from multiple threads</li>
 *   <li>{@link #concurrentLatestLookup} - Latest schema lookups (hot path optimization)</li>
 * </ul>
 *
 * <h2>Thread Configuration</h2>
 * <table border="1">
 *   <tr><th>Benchmark</th><th>Threads</th><th>Purpose</th></tr>
 *   <tr><td>concurrentSingleFix</td><td>MAX (all available)</td><td>Maximum contention stress test</td></tr>
 *   <tr><td>concurrentChainMigration</td><td>MAX</td><td>Chain migration under full load</td></tr>
 *   <tr><td>fourThreadMigration</td><td>4</td><td>Typical server scenario baseline</td></tr>
 *   <tr><td>eightThreadMigration</td><td>8</td><td>Higher parallelism scaling point</td></tr>
 *   <tr><td>concurrentRegistryLookup</td><td>MAX</td><td>Registry contention stress test</td></tr>
 *   <tr><td>concurrentLatestLookup</td><td>MAX</td><td>Hot path contention analysis</td></tr>
 * </table>
 *
 * <h2>Parameters</h2>
 * <table border="1">
 *   <tr><th>Parameter</th><th>Values</th><th>Description</th></tr>
 *   <tr><td>payloadSize</td><td>SMALL, MEDIUM</td><td>Input data complexity per thread</td></tr>
 * </table>
 *
 * <h2>Benchmark Configuration</h2>
 * <table border="1">
 *   <tr><th>Setting</th><th>Value</th></tr>
 *   <tr><td>Warmup</td><td>3 iterations, 2 seconds each</td></tr>
 *   <tr><td>Measurement</td><td>5 iterations, 2 seconds each</td></tr>
 *   <tr><td>Forks</td><td>2 (for JIT variance mitigation)</td></tr>
 *   <tr><td>JVM Heap</td><td>2 GB min/max</td></tr>
 *   <tr><td>Time Unit</td><td>Microseconds</td></tr>
 * </table>
 *
 * <h2>State Management</h2>
 * <p>This benchmark uses two JMH state classes to properly isolate shared and
 * thread-local data:</p>
 * <ul>
 *   <li>{@link BenchmarkState} (Scope.Benchmark) - Shared across all threads: DataFixer
 *       instances, SchemaRegistry, and version constants</li>
 *   <li>{@link ThreadState} (Scope.Thread) - Per-thread isolation: input data, RNG,
 *       and pre-computed random indices to avoid contention</li>
 * </ul>
 *
 * <h2>Interpreting Results</h2>
 * <ul>
 *   <li><b>Linear throughput scaling</b>: Ideal - throughput increases proportionally with thread count</li>
 *   <li><b>Sub-linear scaling</b>: Expected due to shared resource contention (cache lines, locks)</li>
 *   <li><b>Throughput plateau</b>: Indicates saturation point; adding threads provides no benefit</li>
 *   <li><b>Throughput degradation</b>: Severe contention; may indicate lock contention or false sharing</li>
 *   <li><b>High variance (±)</b>: May indicate GC pauses, lock contention, or scheduler interference</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * # Run all concurrent benchmarks
 * java -jar benchmarks.jar ".*concurrent.*"
 *
 * # Run with specific thread count override
 * java -jar benchmarks.jar ConcurrentMigrationBenchmark -t 16
 *
 * # Run registry-only benchmarks
 * java -jar benchmarks.jar ".*concurrent.*Lookup.*"
 *
 * # Quick validation run
 * java -jar benchmarks.jar ConcurrentMigrationBenchmark -wi 1 -i 1 -f 1
 *
 * # Generate JSON report for analysis
 * java -jar benchmarks.jar ConcurrentMigrationBenchmark -rf json -rff concurrent_results.json
 *
 * # Profile with async-profiler integration
 * java -jar benchmarks.jar ConcurrentMigrationBenchmark -prof async:output=flamegraph
 * }</pre>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.benchmarks.core.SingleFixBenchmark
 * @see de.splatgames.aether.datafixers.benchmarks.core.MultiFixChainBenchmark
 * @see BenchmarkBootstrap
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class ConcurrentMigrationBenchmark {

    /**
     * Benchmarks concurrent single-fix migrations with maximum thread parallelism.
     *
     * <p>All available CPU threads simultaneously apply a single DataFix to their
     * respective input data. This benchmark stress-tests the thread-safety of the
     * DataFixer implementation and measures maximum achievable throughput.</p>
     *
     * <p>Key aspects measured:</p>
     * <ul>
     *   <li>Lock contention in shared DataFixer instance</li>
     *   <li>Memory allocation pressure under concurrent load</li>
     *   <li>Cache coherency effects from shared schema access</li>
     * </ul>
     *
     * @param s         shared benchmark state containing the DataFixer and versions
     * @param t         per-thread state containing isolated input data
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(Threads.MAX)
    public void concurrentSingleFix(final BenchmarkState s,
                                    final ThreadState t,
                                    final Blackhole blackhole) {
        final Dynamic<JsonElement> result = s.sharedFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                t.threadInput,
                s.fromVersion,
                s.toVersion
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks concurrent chain migrations with maximum thread parallelism.
     *
     * <p>All available CPU threads simultaneously apply a 10-fix chain migration.
     * This benchmark combines the stress of concurrent access with the complexity
     * of multi-step migrations, revealing performance characteristics under
     * realistic high-load scenarios.</p>
     *
     * <p>Compared to {@link #concurrentSingleFix}, this benchmark:</p>
     * <ul>
     *   <li>Increases per-operation duration, potentially reducing contention</li>
     *   <li>Exercises fix ordering and version traversal logic concurrently</li>
     *   <li>Creates higher memory allocation rates per thread</li>
     * </ul>
     *
     * @param s         shared benchmark state containing the chain DataFixer
     * @param t         per-thread state containing isolated input data
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(Threads.MAX)
    public void concurrentChainMigration(final BenchmarkState s,
                                         final ThreadState t,
                                         final Blackhole blackhole) {
        final Dynamic<JsonElement> result = s.sharedChainFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                t.threadInput,
                s.fromVersion,
                s.chainToVersion
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks migration performance with exactly 4 concurrent threads.
     *
     * <p>Provides a fixed-thread baseline for comparing against variable-thread
     * benchmarks. Four threads represent a typical server core count and help
     * establish scaling characteristics between single-threaded and maximum
     * parallelism scenarios.</p>
     *
     * <p>Use this benchmark to:</p>
     * <ul>
     *   <li>Establish baseline concurrent performance on quad-core systems</li>
     *   <li>Compare with {@link #eightThreadMigration} to measure scaling factor</li>
     *   <li>Identify the point where adding threads provides diminishing returns</li>
     * </ul>
     *
     * @param s         shared benchmark state containing the DataFixer
     * @param t         per-thread state containing isolated input data
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(4)
    public void fourThreadMigration(final BenchmarkState s,
                                    final ThreadState t,
                                    final Blackhole blackhole) {
        final Dynamic<JsonElement> result = s.sharedFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                t.threadInput,
                s.fromVersion,
                s.toVersion
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks migration performance with exactly 8 concurrent threads.
     *
     * <p>Tests scaling beyond the 4-thread baseline. Eight threads represent
     * a common server configuration and help identify whether the DataFixer
     * implementation scales efficiently with additional parallelism.</p>
     *
     * <p>Scaling analysis:</p>
     * <ul>
     *   <li><b>2x throughput vs 4 threads</b>: Perfect linear scaling</li>
     *   <li><b>1.5-2x throughput</b>: Good scaling with minor contention</li>
     *   <li><b>&lt;1.5x throughput</b>: Contention limiting scalability</li>
     *   <li><b>≤1x throughput</b>: Severe contention; investigate locking</li>
     * </ul>
     *
     * @param s         shared benchmark state containing the DataFixer
     * @param t         per-thread state containing isolated input data
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(8)
    public void eightThreadMigration(final BenchmarkState s,
                                     final ThreadState t,
                                     final Blackhole blackhole) {
        final Dynamic<JsonElement> result = s.sharedFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                t.threadInput,
                s.fromVersion,
                s.toVersion
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks concurrent random schema lookups from the registry.
     *
     * <p>All available threads perform random version lookups against a shared
     * {@link SchemaRegistry} containing 100 schema versions. This benchmark
     * validates the thread-safety and performance of registry read operations
     * under heavy concurrent access.</p>
     *
     * <p>The benchmark uses pre-computed random indices (via {@link ThreadState#nextRegistryIndex()})
     * to avoid RNG contention affecting measurements. Each thread cycles through
     * a 1024-element buffer of random indices.</p>
     *
     * <p>Performance expectations:</p>
     * <ul>
     *   <li>Registry lookups should be lock-free and scale linearly</li>
     *   <li>Cache effects may cause variance based on version access patterns</li>
     *   <li>No write contention since registry is frozen before benchmarking</li>
     * </ul>
     *
     * @param s         shared benchmark state containing the registry and versions
     * @param t         per-thread state providing random index sequence
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(Threads.MAX)
    public void concurrentRegistryLookup(final BenchmarkState s,
                                         final ThreadState t,
                                         final Blackhole blackhole) {
        final int index = t.nextRegistryIndex();
        final Schema schema = s.sharedRegistry.get(s.registryVersions[index]);
        blackhole.consume(schema);
    }

    /**
     * Benchmarks concurrent latest-schema lookups from the registry.
     *
     * <p>All available threads repeatedly call {@link SchemaRegistry#latest()}
     * on a shared registry. This represents the "hot path" optimization where
     * applications frequently need the most recent schema version.</p>
     *
     * <p>This benchmark helps validate:</p>
     * <ul>
     *   <li>Caching effectiveness for the latest schema reference</li>
     *   <li>Memory visibility of the cached latest schema across threads</li>
     *   <li>Absence of unnecessary synchronization on read-only access</li>
     * </ul>
     *
     * <p>Expected to outperform {@link #concurrentRegistryLookup} due to:</p>
     * <ul>
     *   <li>No version-to-schema map lookup required</li>
     *   <li>Single cached reference rather than computed lookup</li>
     *   <li>Better CPU cache utilization from accessing same memory location</li>
     * </ul>
     *
     * @param s         shared benchmark state containing the registry
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(Threads.MAX)
    public void concurrentLatestLookup(final BenchmarkState s,
                                       final Blackhole blackhole) {
        final Schema schema = s.sharedRegistry.latest();
        blackhole.consume(schema);
    }

    /**
     * Shared benchmark state accessible by all threads.
     *
     * <p>This state class contains all resources that are shared across benchmark
     * threads, simulating real-world scenarios where a single DataFixer instance
     * serves multiple concurrent requests.</p>
     *
     * <p>State initialization occurs once per trial (before warmup begins) to
     * ensure consistent starting conditions across all measurement iterations.</p>
     *
     * <h2>Shared Resources</h2>
     * <ul>
     *   <li>{@link #sharedFixer} - Single-fix DataFixer for basic migration benchmarks</li>
     *   <li>{@link #sharedChainFixer} - 10-fix chain DataFixer for chain migration benchmarks</li>
     *   <li>{@link #sharedRegistry} - Frozen SchemaRegistry with 100 versions for lookup benchmarks</li>
     *   <li>Version constants - Pre-computed DataVersion instances to avoid allocation during measurement</li>
     * </ul>
     */
    @State(Scope.Benchmark)
    public static class BenchmarkState {

        /**
         * The payload size parameter, injected by JMH.
         *
         * <p>Controls the complexity of generated test data for each thread.
         * Only SMALL and MEDIUM sizes are used to balance benchmark runtime
         * with meaningful performance differentiation.</p>
         *
         * @see PayloadSize
         */
        @Param({"SMALL", "MEDIUM"})
        public PayloadSize payloadSize;

        /**
         * Shared DataFixer configured with a single fix (v1 → v2).
         *
         * <p>Used by migration benchmarks that measure basic concurrent
         * fix application without chain traversal overhead.</p>
         */
        public DataFixer sharedFixer;

        /**
         * Shared DataFixer configured with a 10-fix chain (v1 → v11).
         *
         * <p>Used by {@link #concurrentChainMigration} to measure concurrent
         * performance when applying multiple sequential fixes.</p>
         */
        public DataFixer sharedChainFixer;

        /**
         * Shared SchemaRegistry containing 100 schema versions.
         *
         * <p>The registry is frozen after population to ensure thread-safe
         * read access during benchmarks. Versions range from 10 to 1000
         * in increments of 10.</p>
         */
        public SchemaRegistry sharedRegistry;

        /**
         * Source version for all migrations (v1).
         */
        public DataVersion fromVersion;

        /**
         * Target version for single-fix migrations (v2).
         */
        public DataVersion toVersion;

        /**
         * Target version for chain migrations (v11).
         */
        public DataVersion chainToVersion;

        /**
         * Pre-computed DataVersion array for registry lookup benchmarks.
         *
         * <p>Contains 100 versions (10, 20, 30, ..., 1000) matching the
         * schemas registered in {@link #sharedRegistry}. Pre-allocation
         * avoids DataVersion object creation during measurement.</p>
         */
        public DataVersion[] registryVersions;

        /**
         * Initializes all shared benchmark state.
         *
         * <p>Creates DataFixer instances, populates the SchemaRegistry with
         * 100 versions, and pre-computes all version constants. The registry
         * is frozen after population to enable lock-free concurrent reads.</p>
         */
        @Setup(Level.Trial)
        public void setup() {
            this.sharedFixer = BenchmarkBootstrap.createSingleFixFixer();
            this.sharedChainFixer = BenchmarkBootstrap.createChainFixer(10);

            this.fromVersion = new DataVersion(1);
            this.toVersion = new DataVersion(2);
            this.chainToVersion = new DataVersion(11);

            final SimpleSchemaRegistry registry = new SimpleSchemaRegistry();
            this.registryVersions = new DataVersion[100];
            for (int i = 0; i < 100; i++) {
                final int version = (i + 1) * 10;
                this.registryVersions[i] = new DataVersion(version);
                registry.register(MockSchemas.minimal(version));
            }
            registry.freeze();
            this.sharedRegistry = registry;
        }
    }

    /**
     * Per-thread benchmark state for isolated data and random access patterns.
     *
     * <p>This state class provides each benchmark thread with its own input data
     * and random number generator to eliminate false sharing and contention on
     * thread-local operations.</p>
     *
     * <h2>Design Rationale</h2>
     * <ul>
     *   <li><b>Thread-local input</b>: Each thread operates on its own Dynamic instance,
     *       preventing write contention and ensuring independent GC behavior</li>
     *   <li><b>SplittableRandom</b>: Faster and contention-free compared to
     *       {@link java.util.Random} which uses atomic CAS operations</li>
     *   <li><b>Pre-computed indices</b>: Random registry indices are generated during
     *       setup to avoid RNG overhead during measurement</li>
     * </ul>
     *
     * <h2>Index Buffer Strategy</h2>
     * <p>The {@link #registryIndexBuffer} uses a power-of-two size (1024) with
     * bitwise AND masking for efficient wraparound without modulo operations.
     * This provides pseudo-random access patterns while minimizing measurement
     * overhead.</p>
     */
    @State(Scope.Thread)
    public static class ThreadState {

        /**
         * Size of the pre-computed random index buffer.
         *
         * <p>Power of two (1024) enables efficient wraparound via bitwise AND.
         * Large enough to avoid pattern repetition affecting cache behavior
         * during typical measurement windows.</p>
         */
        private static final int INDEX_BUFFER_SIZE = 1024;

        /**
         * Bitmask for efficient modulo operation on buffer index.
         *
         * <p>Used as {@code cursor & INDEX_MASK} instead of {@code cursor % INDEX_BUFFER_SIZE}
         * for faster wraparound calculation.</p>
         */
        private static final int INDEX_MASK = INDEX_BUFFER_SIZE - 1;

        /**
         * Pre-computed random indices for registry lookup benchmarks.
         *
         * <p>Populated during iteration setup with random values in range
         * [0, registryVersions.length). Accessed via {@link #nextRegistryIndex()}.</p>
         */
        private final int[] registryIndexBuffer = new int[INDEX_BUFFER_SIZE];

        /**
         * Per-thread input data for migration benchmarks.
         *
         * <p>Regenerated at each iteration to ensure consistent memory allocation
         * patterns and prevent cross-iteration caching effects.</p>
         */
        public Dynamic<JsonElement> threadInput;

        /**
         * Current position in the {@link #registryIndexBuffer}.
         *
         * <p>Incremented on each call to {@link #nextRegistryIndex()} and
         * wrapped using {@link #INDEX_MASK}.</p>
         */
        private int registryCursor;

        /**
         * Per-thread random number generator.
         *
         * <p>{@link SplittableRandom} is used instead of {@link java.util.Random}
         * because it is faster and does not use atomic operations, eliminating
         * contention when multiple threads generate random numbers.</p>
         */
        private SplittableRandom random;

        /**
         * Initializes the per-thread random number generator.
         *
         * <p>Called once per trial. Uses a fixed seed (42) for reproducibility
         * across benchmark runs, though each thread will produce different
         * sequences due to {@link SplittableRandom}'s splittable nature.</p>
         */
        @Setup(Level.Trial)
        public void setupTrial() {
            // Per-thread RNG avoids contention and is faster than java.util.Random.
            this.random = new SplittableRandom(42L);
        }

        /**
         * Regenerates input data and random indices for each iteration.
         *
         * <p>Fresh data generation per iteration ensures:</p>
         * <ul>
         *   <li>Consistent GC pressure across iterations</li>
         *   <li>No JIT over-optimization on specific data patterns</li>
         *   <li>Independent memory allocation per thread</li>
         * </ul>
         *
         * <p>The random index buffer is refilled with new random values to
         * vary the registry access pattern across iterations.</p>
         *
         * @param s the shared benchmark state providing payload size and version array
         */
        @Setup(Level.Iteration)
        public void setupIteration(final BenchmarkState s) {
            this.threadInput = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, s.payloadSize);

            for (int i = 0; i < INDEX_BUFFER_SIZE; i++) {
                this.registryIndexBuffer[i] = this.random.nextInt(s.registryVersions.length);
            }
            this.registryCursor = 0;
        }

        /**
         * Returns the next pre-computed random index for registry lookups.
         *
         * <p>Retrieves the next value from {@link #registryIndexBuffer} and
         * advances the cursor with efficient bitwise wraparound. This method
         * is called during measurement and is optimized to minimize overhead.</p>
         *
         * @return a random index in range [0, registryVersions.length)
         */
        public int nextRegistryIndex() {
            return this.registryIndexBuffer[this.registryCursor++ & INDEX_MASK];
        }
    }
}
