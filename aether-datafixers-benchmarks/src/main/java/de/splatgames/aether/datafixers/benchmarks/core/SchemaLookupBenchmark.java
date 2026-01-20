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

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for schema registry lookup performance.
 *
 * <p>Measures the performance of {@link SchemaRegistry#get(DataVersion)}
 * with varying registry sizes. Uses floor semantics (finds greatest version
 * less than or equal to requested).</p>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class SchemaLookupBenchmark {

    @Param({"10", "50", "100", "500"})
    private int schemaCount;

    private SchemaRegistry registry;
    private DataVersion[] versions;
    private DataVersion[] lookupVersions;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        // Create registry with specified number of schemas
        final SimpleSchemaRegistry simpleRegistry = new SimpleSchemaRegistry();
        this.versions = new DataVersion[this.schemaCount];

        for (int i = 0; i < this.schemaCount; i++) {
            final int version = (i + 1) * 10; // 10, 20, 30, ...
            this.versions[i] = new DataVersion(version);
            simpleRegistry.register(MockSchemas.minimal(version));
        }
        simpleRegistry.freeze();
        this.registry = simpleRegistry;

        // Create lookup versions (including versions between registered versions)
        this.lookupVersions = new DataVersion[this.schemaCount * 2];
        for (int i = 0; i < this.lookupVersions.length; i++) {
            this.lookupVersions[i] = new DataVersion((i + 1) * 5); // 5, 10, 15, ...
        }

        this.random = new Random(42); // Fixed seed for reproducibility
    }

    /**
     * Benchmarks looking up an exact registered version.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void exactLookup(final Blackhole blackhole) {
        final int index = this.random.nextInt(this.schemaCount);
        final Schema schema = this.registry.get(this.versions[index]);
        blackhole.consume(schema);
    }

    /**
     * Benchmarks looking up a version using floor semantics.
     *
     * <p>Half of the lookups will be for exact versions, half will
     * require floor resolution.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void floorLookup(final Blackhole blackhole) {
        final int index = this.random.nextInt(this.lookupVersions.length);
        final Schema schema = this.registry.get(this.lookupVersions[index]);
        blackhole.consume(schema);
    }

    /**
     * Benchmarks getting the latest schema.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void latestLookup(final Blackhole blackhole) {
        final Schema schema = this.registry.latest();
        blackhole.consume(schema);
    }

    /**
     * Benchmarks sequential lookup of all versions.
     *
     * <p>Measures cache-friendly access patterns.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void sequentialLookup(final Blackhole blackhole) {
        for (final DataVersion version : this.versions) {
            final Schema schema = this.registry.get(version);
            blackhole.consume(schema);
        }
    }
}
