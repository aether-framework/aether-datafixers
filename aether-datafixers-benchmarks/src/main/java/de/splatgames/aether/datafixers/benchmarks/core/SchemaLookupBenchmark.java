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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for schema registry lookup performance.
 *
 * <p>Measures the overhead of various schema lookup operations as registry size grows.
 * Schema lookups are performed frequently during data migration, so their performance directly impacts overall
 * migration throughput.</p>
 *
 * <h2>Benchmark Methods</h2>
 * <ul>
 *   <li>{@link #exactLookup} - Direct lookup by exact version match</li>
 *   <li>{@link #floorLookup} - Floor lookup finding closest version &le; target</li>
 *   <li>{@link #latestLookup} - Retrieval of the most recent schema</li>
 *   <li>{@link #sequentialLookup} - Sequential traversal of all registered versions</li>
 * </ul>
 *
 * <h2>Parameters</h2>
 * <table border="1">
 *   <tr><th>Parameter</th><th>Values</th><th>Description</th></tr>
 *   <tr><td>schemaCount</td><td>10, 50, 100, 500</td><td>Number of schemas in the registry</td></tr>
 * </table>
 *
 * <h2>Benchmark Configuration</h2>
 * <table border="1">
 *   <tr><th>Setting</th><th>Value</th></tr>
 *   <tr><td>Warmup</td><td>5 iterations, 1 second each</td></tr>
 *   <tr><td>Measurement</td><td>10 iterations, 1 second each</td></tr>
 *   <tr><td>Forks</td><td>2 (for statistical significance)</td></tr>
 *   <tr><td>JVM Heap</td><td>2 GB min/max</td></tr>
 *   <tr><td>Time Unit</td><td>Nanoseconds</td></tr>
 * </table>
 *
 * <h2>Interpreting Results</h2>
 * <ul>
 *   <li><b>O(1) lookups</b>: {@link #exactLookup} and {@link #latestLookup} should show constant time regardless of registry size.</li>
 *   <li><b>O(log n) lookups</b>: {@link #floorLookup} may show logarithmic scaling if implemented via binary search.</li>
 *   <li><b>O(n) lookups</b>: {@link #sequentialLookup} should scale linearly with schema count.</li>
 *   <li><b>Cache effects</b>: Larger registries may show increased lookup time due to CPU cache pressure.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * # Run only this benchmark
 * java -jar benchmarks.jar SchemaLookupBenchmark
 *
 * # Quick test with reduced iterations
 * java -jar benchmarks.jar SchemaLookupBenchmark -wi 1 -i 1 -f 1
 *
 * # Specific schema count only
 * java -jar benchmarks.jar SchemaLookupBenchmark -p schemaCount=100
 *
 * # Run specific lookup benchmark
 * java -jar benchmarks.jar SchemaLookupBenchmark.exactLookup
 * }</pre>
 *
 * @author Erik Pförtner
 * @see SchemaRegistry
 * @see SimpleSchemaRegistry
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class SchemaLookupBenchmark {

    /**
     * Benchmarks exact version lookup performance.
     *
     * <p>Measures the time to retrieve a schema by its exact registered version.
     * This is the most common lookup pattern during migration when the source version is known precisely.</p>
     *
     * <p>The benchmark uses pre-generated random indices to avoid RNG overhead
     * in the measurement loop. Each invocation looks up a different random version to prevent branch prediction
     * optimization.</p>
     *
     * @param s         the shared benchmark state containing the registry and versions
     * @param t         the per-thread state providing random lookup indices
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void exactLookup(final BenchmarkState s,
                            final ThreadState t,
                            final Blackhole blackhole) {
        final int index = t.nextExactIndex();
        final Schema schema = s.registry.get(s.versions[index]);
        blackhole.consume(schema);
    }

    /**
     * Benchmarks floor lookup performance.
     *
     * <p>Measures the time to retrieve a schema using floor semantics, where
     * the registry returns the schema with the highest version &le; the requested version. This pattern is used when
     * data may be at intermediate versions not explicitly registered.</p>
     *
     * <p>The lookup versions include both exact matches (10, 20, 30, ...) and
     * in-between values (5, 15, 25, ...) to exercise both fast-path exact matches and slower floor searches.</p>
     *
     * @param s         the shared benchmark state containing the registry and lookup versions
     * @param t         the per-thread state providing random lookup indices
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void floorLookup(final BenchmarkState s,
                            final ThreadState t,
                            final Blackhole blackhole) {
        final int index = t.nextFloorIndex();
        final Schema schema = s.registry.get(s.lookupVersions[index]);
        blackhole.consume(schema);
    }

    /**
     * Benchmarks latest schema retrieval performance.
     *
     * <p>Measures the time to retrieve the most recent schema from the registry.
     * This operation should be O(1) as the latest schema is typically cached or stored in a dedicated field.</p>
     *
     * <p>This benchmark serves as a baseline for the fastest possible lookup
     * operation and helps identify any unexpected overhead in the registry implementation.</p>
     *
     * @param s         the shared benchmark state containing the registry
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void latestLookup(final BenchmarkState s,
                             final Blackhole blackhole) {
        final Schema schema = s.registry.latest();
        blackhole.consume(schema);
    }

    /**
     * Benchmarks sequential lookup of all registered schemas.
     *
     * <p>Measures the aggregate time to look up every schema in the registry
     * in version order. This pattern occurs during schema validation, debugging, or when building migration path
     * analyses.</p>
     *
     * <p><b>Note:</b> This benchmark performs multiple lookups per invocation
     * ({@code schemaCount} lookups). The reported time is for the entire sequence, not per-lookup. Divide by
     * {@code schemaCount} to get per-lookup overhead.</p>
     *
     * @param s         the shared benchmark state containing the registry and versions
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void sequentialLookup(final BenchmarkState s,
                                 final Blackhole blackhole) {
        for (final DataVersion version : s.versions) {
            final Schema schema = s.registry.get(version);
            blackhole.consume(schema);
        }
    }

    /**
     * Shared JMH state containing the schema registry and version arrays.
     *
     * <p>This state is shared across all threads within a benchmark trial
     * ({@link Scope#Benchmark}). The registry is populated with mock schemas at versions 10, 20, 30, ... up to
     * {@code schemaCount * 10}.</p>
     *
     * <p>The registry is frozen after setup to match production usage patterns
     * where registries are immutable during normal operation.</p>
     */
    @State(Scope.Benchmark)
    public static class BenchmarkState {

        /**
         * The number of schemas to register, injected by JMH.
         *
         * <p>Controls the size of the schema registry to measure lookup
         * performance scaling:</p>
         * <ul>
         *   <li><b>10</b>: Small registry, fits entirely in L1 cache</li>
         *   <li><b>50</b>: Medium registry, typical for most applications</li>
         *   <li><b>100</b>: Large registry, may exceed L1 cache</li>
         *   <li><b>500</b>: Stress test for registry scalability</li>
         * </ul>
         */
        @Param({"10", "50", "100", "500"})
        public int schemaCount;

        /**
         * The frozen schema registry containing all registered schemas.
         */
        public SchemaRegistry registry;

        /**
         * Array of exact registered versions (10, 20, 30, ...).
         *
         * <p>Used by {@link #exactLookup} to ensure lookups always hit
         * registered versions.</p>
         */
        public DataVersion[] versions;

        /**
         * Array of lookup versions including in-between values (5, 10, 15, 20, ...).
         *
         * <p>Used by {@link #floorLookup} to exercise both exact matches
         * and floor search behavior.</p>
         */
        public DataVersion[] lookupVersions;

        /**
         * Initializes the schema registry and version arrays once per trial.
         *
         * <p>Creates a {@link SimpleSchemaRegistry} populated with minimal mock
         * schemas at regular version intervals. The registry is frozen after population to enable any internal
         * optimizations.</p>
         */
        @Setup(Level.Trial)
        public void setup() {
            final SimpleSchemaRegistry simpleRegistry = new SimpleSchemaRegistry();
            this.versions = new DataVersion[this.schemaCount];

            for (int i = 0; i < this.schemaCount; i++) {
                final int version = (i + 1) * 10;
                final DataVersion dataVersion = new DataVersion(version);
                this.versions[i] = dataVersion;
                simpleRegistry.register(MockSchemas.minimal(version));
            }

            simpleRegistry.freeze();
            this.registry = simpleRegistry;

            this.lookupVersions = new DataVersion[this.schemaCount * 2];
            for (int i = 0; i < this.lookupVersions.length; i++) {
                this.lookupVersions[i] = new DataVersion((i + 1) * 5);
            }
        }
    }

    /**
     * Per-thread JMH state providing pre-generated random lookup indices.
     *
     * <p>Random number generation is expensive and would dominate the benchmark
     * if performed in the hot path. This state pre-generates buffers of random indices during setup, allowing the
     * benchmark methods to retrieve indices via simple array access and bit masking.</p>
     *
     * <p>Each thread has its own state instance ({@link Scope#Thread}) to avoid
     * contention on shared RNG state. The fixed seed ensures reproducible results across benchmark runs.</p>
     *
     * @see BenchmarkState
     */
    @State(Scope.Thread)
    public static class ThreadState {

        /**
         * Size of the pre-generated index buffer.
         *
         * <p>Power-of-two size enables cheap index wrapping via bit masking
         * instead of modulo operation.</p>
         */
        private static final int INDEX_BUFFER_SIZE = 1024;

        /**
         * Bit mask for wrapping cursor to buffer bounds ({@code INDEX_BUFFER_SIZE - 1}).
         */
        private static final int INDEX_MASK = INDEX_BUFFER_SIZE - 1;

        /**
         * Pre-generated indices into {@link BenchmarkState#versions}.
         */
        private final int[] exactIndices = new int[INDEX_BUFFER_SIZE];

        /**
         * Pre-generated indices into {@link BenchmarkState#lookupVersions}.
         */
        private final int[] floorIndices = new int[INDEX_BUFFER_SIZE];

        /**
         * Current position in {@link #exactIndices}.
         */
        private int exactCursor;

        /**
         * Current position in {@link #floorIndices}.
         */
        private int floorCursor;

        /**
         * Thread-local random number generator for index generation.
         */
        private SplittableRandom random;

        /**
         * Initializes the random number generator once per trial.
         *
         * <p>Uses a fixed seed (42) for reproducibility. Each thread gets its
         * own {@link SplittableRandom} instance to avoid synchronization overhead.</p>
         */
        @Setup(Level.Trial)
        public void setupTrial() {
            this.random = new SplittableRandom(42L);
        }

        /**
         * Refills the index buffers at each iteration.
         *
         * <p>Generates fresh random indices based on the current
         * {@link BenchmarkState#schemaCount} parameter. Resets cursors to the beginning of each buffer.</p>
         *
         * @param s the shared benchmark state providing array bounds
         */
        @Setup(Level.Iteration)
        public void setupIteration(final BenchmarkState s) {
            for (int i = 0; i < INDEX_BUFFER_SIZE; i++) {
                this.exactIndices[i] = this.random.nextInt(s.versions.length);
                this.floorIndices[i] = this.random.nextInt(s.lookupVersions.length);
            }
            this.exactCursor = 0;
            this.floorCursor = 0;
        }

        /**
         * Returns the next random index for exact version lookup.
         *
         * <p>Uses bit masking to wrap around the buffer efficiently.</p>
         *
         * @return a random index into {@link BenchmarkState#versions}
         */
        public int nextExactIndex() {
            return this.exactIndices[this.exactCursor++ & INDEX_MASK];
        }

        /**
         * Returns the next random index for floor version lookup.
         *
         * <p>Uses bit masking to wrap around the buffer efficiently.</p>
         *
         * @return a random index into {@link BenchmarkState#lookupVersions}
         */
        public int nextFloorIndex() {
            return this.floorIndices[this.floorCursor++ & INDEX_MASK];
        }
    }
}
