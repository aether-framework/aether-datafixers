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
 * Includes a baseline identity fix measurement to isolate framework overhead.</p>
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
public class SingleFixBenchmark {

    @Param({"SMALL", "MEDIUM", "LARGE"})
    private PayloadSize payloadSize;

    private DataFixer fixer;
    private DataFixer identityFixer;
    private Dynamic<JsonElement> input;
    private DataVersion fromVersion;
    private DataVersion toVersion;

    @Setup(Level.Trial)
    public void setup() {
        this.fixer = BenchmarkBootstrap.createSingleFixFixer();
        this.identityFixer = BenchmarkBootstrap.createIdentityFixer();
        this.input = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, this.payloadSize);
        this.fromVersion = new DataVersion(1);
        this.toVersion = new DataVersion(2);
    }

    /**
     * Benchmarks applying a single rename field fix.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void singleRenameFix(final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.fixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.input,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    /**
     * Baseline benchmark with identity fix (no transformation).
     *
     * <p>Measures framework overhead without actual data transformation.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void identityFix(final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.identityFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.input,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    /**
     * Benchmarks applying a fix to player-like data structure.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void playerDataFix(final Blackhole blackhole) {
        final Dynamic<JsonElement> playerInput = BenchmarkDataGenerator.generatePlayerData(GsonOps.INSTANCE);
        final DataFixer playerFixer = BenchmarkBootstrap.createPlayerFixer();
        final Dynamic<JsonElement> result = playerFixer.update(
                BenchmarkBootstrap.PLAYER_TYPE,
                playerInput,
                new DataVersion(1),
                new DataVersion(2));
        blackhole.consume(result);
    }
}
