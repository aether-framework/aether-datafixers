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

package de.splatgames.aether.datafixers.benchmarks.codec;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.codec.Codecs;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for collection codec encode/decode performance.
 *
 * <p>Measures the performance of encoding and decoding lists of various sizes
 * using the {@link Codecs#list(Codec)} API.</p>
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
public class CollectionCodecBenchmark {

    @Param({"10", "100", "1000"})
    private int listSize;

    private Codec<List<String>> stringListCodec;
    private Codec<List<Integer>> intListCodec;

    private List<String> stringList;
    private List<Integer> intList;

    private JsonElement encodedStringList;
    private JsonElement encodedIntList;

    @Setup(Level.Trial)
    public void setup() {
        this.stringListCodec = Codecs.list(Codecs.STRING);
        this.intListCodec = Codecs.list(Codecs.INT);

        // Generate test data
        this.stringList = new ArrayList<>(this.listSize);
        this.intList = new ArrayList<>(this.listSize);

        for (int i = 0; i < this.listSize; i++) {
            this.stringList.add("item-" + i);
            this.intList.add(i);
        }

        // Pre-encode for decode benchmarks
        this.encodedStringList = this.stringListCodec.encodeStart(GsonOps.INSTANCE, this.stringList)
                .result().orElseThrow();
        this.encodedIntList = this.intListCodec.encodeStart(GsonOps.INSTANCE, this.intList)
                .result().orElseThrow();
    }

    // ==================== String List ====================

    /**
     * Benchmarks encoding a list of strings.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeStringList(final Blackhole blackhole) {
        final DataResult<JsonElement> result = this.stringListCodec.encodeStart(
                GsonOps.INSTANCE, this.stringList);
        blackhole.consume(result);
    }

    /**
     * Benchmarks decoding a list of strings.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeStringList(final Blackhole blackhole) {
        final DataResult<Pair<List<String>, JsonElement>> result = this.stringListCodec.decode(
                GsonOps.INSTANCE, this.encodedStringList);
        blackhole.consume(result);
    }

    // ==================== Integer List ====================

    /**
     * Benchmarks encoding a list of integers.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeIntList(final Blackhole blackhole) {
        final DataResult<JsonElement> result = this.intListCodec.encodeStart(
                GsonOps.INSTANCE, this.intList);
        blackhole.consume(result);
    }

    /**
     * Benchmarks decoding a list of integers.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeIntList(final Blackhole blackhole) {
        final DataResult<Pair<List<Integer>, JsonElement>> result = this.intListCodec.decode(
                GsonOps.INSTANCE, this.encodedIntList);
        blackhole.consume(result);
    }

    // ==================== Round Trip ====================

    /**
     * Benchmarks round-trip encoding and decoding of a string list.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void roundTripStringList(final Blackhole blackhole) {
        final DataResult<JsonElement> encoded = this.stringListCodec.encodeStart(
                GsonOps.INSTANCE, this.stringList);
        final DataResult<Pair<List<String>, JsonElement>> decoded = encoded.flatMap(
                json -> this.stringListCodec.decode(GsonOps.INSTANCE, json));
        blackhole.consume(decoded);
    }

    /**
     * Benchmarks round-trip encoding and decoding of an integer list.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void roundTripIntList(final Blackhole blackhole) {
        final DataResult<JsonElement> encoded = this.intListCodec.encodeStart(
                GsonOps.INSTANCE, this.intList);
        final DataResult<Pair<List<Integer>, JsonElement>> decoded = encoded.flatMap(
                json -> this.intListCodec.decode(GsonOps.INSTANCE, json));
        blackhole.consume(decoded);
    }
}
