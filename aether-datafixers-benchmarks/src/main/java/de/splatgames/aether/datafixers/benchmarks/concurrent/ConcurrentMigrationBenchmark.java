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

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for concurrent migration and registry access performance.
 *
 * <p>Measures the thread-safety and contention characteristics of the
 * DataFixer and SchemaRegistry under concurrent load.</p>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class ConcurrentMigrationBenchmark {

    @Param({"SMALL", "MEDIUM"})
    private PayloadSize payloadSize;

    // Shared state across threads
    private DataFixer sharedFixer;
    private DataFixer sharedChainFixer;
    private SchemaRegistry sharedRegistry;
    private DataVersion fromVersion;
    private DataVersion toVersion;
    private DataVersion chainToVersion;
    private DataVersion[] registryVersions;

    @Setup(Level.Trial)
    public void setup() {
        // Create shared fixer (thread-safe after freeze)
        this.sharedFixer = BenchmarkBootstrap.createSingleFixFixer();
        this.sharedChainFixer = BenchmarkBootstrap.createChainFixer(10);
        this.fromVersion = new DataVersion(1);
        this.toVersion = new DataVersion(2);
        this.chainToVersion = new DataVersion(11);

        // Create shared registry
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

    /**
     * Per-thread state for independent test data.
     */
    @State(Scope.Thread)
    public static class ThreadState {

        private Dynamic<JsonElement> threadInput;
        private Random random;

        @Setup(Level.Iteration)
        public void setup(final ConcurrentMigrationBenchmark parent) {
            // Each thread gets its own input data
            this.threadInput = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, parent.payloadSize);
            this.random = new Random();
        }
    }

    // ==================== Concurrent Migration ====================

    /**
     * Benchmarks concurrent single-fix migrations using all available processors.
     *
     * @param state     per-thread state
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(Threads.MAX)
    public void concurrentSingleFix(final ThreadState state, final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.sharedFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                state.threadInput,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    /**
     * Benchmarks concurrent chain migrations using all available processors.
     *
     * @param state     per-thread state
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(Threads.MAX)
    public void concurrentChainMigration(final ThreadState state, final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.sharedChainFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                state.threadInput,
                this.fromVersion,
                this.chainToVersion);
        blackhole.consume(result);
    }

    /**
     * Benchmarks concurrent migrations with 4 threads.
     *
     * @param state     per-thread state
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(4)
    public void fourThreadMigration(final ThreadState state, final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.sharedFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                state.threadInput,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    /**
     * Benchmarks concurrent migrations with 8 threads.
     *
     * @param state     per-thread state
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(8)
    public void eightThreadMigration(final ThreadState state, final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.sharedFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                state.threadInput,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    // ==================== Concurrent Registry Access ====================

    /**
     * Benchmarks concurrent schema registry lookups.
     *
     * @param state     per-thread state
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(Threads.MAX)
    public void concurrentRegistryLookup(final ThreadState state, final Blackhole blackhole) {
        final int index = state.random.nextInt(this.registryVersions.length);
        final Schema schema = this.sharedRegistry.get(this.registryVersions[index]);
        blackhole.consume(schema);
    }

    /**
     * Benchmarks concurrent latest schema access.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    @Threads(Threads.MAX)
    public void concurrentLatestLookup(final Blackhole blackhole) {
        final Schema schema = this.sharedRegistry.latest();
        blackhole.consume(schema);
    }
}
