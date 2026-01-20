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
 * JMH benchmark for multi-fix chain migration performance.
 *
 * <p>Measures the performance of applying multiple sequential fixes,
 * simulating real-world migration scenarios where data may need to
 * traverse many version upgrades.</p>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class MultiFixChainBenchmark {

    @Param({"1", "5", "10", "25", "50"})
    private int fixCount;

    @Param({"SMALL", "MEDIUM"})
    private PayloadSize payloadSize;

    private DataFixer chainFixer;
    private DataFixer mixedFixer;
    private Dynamic<JsonElement> input;
    private DataVersion fromVersion;
    private DataVersion toVersion;

    @Setup(Level.Trial)
    public void setup() {
        this.chainFixer = BenchmarkBootstrap.createChainFixer(this.fixCount);
        if (this.fixCount >= 4) {
            this.mixedFixer = BenchmarkBootstrap.createMixedFixer(this.fixCount);
        }
        this.input = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, this.payloadSize);
        this.fromVersion = new DataVersion(1);
        this.toVersion = new DataVersion(this.fixCount + 1);
    }

    /**
     * Benchmarks applying a chain of rename fixes.
     *
     * <p>All fixes in the chain perform the same operation type (rename),
     * measuring sequential fix application overhead.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void renameChain(final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.chainFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.input,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    /**
     * Benchmarks applying a chain of mixed fix types.
     *
     * <p>Includes rename, add, remove, and transform operations
     * for more realistic migration scenarios.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void mixedChain(final Blackhole blackhole) {
        if (this.mixedFixer == null) {
            // Skip for fixCount < 4
            blackhole.consume(this.input);
            return;
        }
        final Dynamic<JsonElement> result = this.mixedFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.input,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    /**
     * Benchmarks partial migration (half the chain).
     *
     * <p>Measures performance when migrating to an intermediate version
     * rather than the latest version.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void partialChain(final Blackhole blackhole) {
        final int halfwayVersion = Math.max(2, (this.fixCount / 2) + 1);
        final Dynamic<JsonElement> result = this.chainFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.input,
                this.fromVersion,
                new DataVersion(halfwayVersion));
        blackhole.consume(result);
    }
}
