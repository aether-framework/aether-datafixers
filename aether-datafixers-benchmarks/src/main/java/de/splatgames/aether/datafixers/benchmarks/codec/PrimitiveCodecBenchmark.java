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

package de.splatgames.aether.datafixers.benchmarks.codec;

import com.google.gson.JsonElement;
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
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for primitive codec encode/decode performance.
 *
 * <p>Measures the performance of encoding and decoding primitive types
 * using the {@link Codecs} API.</p>
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
public class PrimitiveCodecBenchmark {

    // Test values
    private static final boolean TEST_BOOL = true;
    private static final int TEST_INT = 42;
    private static final long TEST_LONG = 123456789L;
    private static final float TEST_FLOAT = 3.14159f;
    private static final double TEST_DOUBLE = 2.718281828;
    private static final String TEST_STRING = "benchmark-test-string";

    // Pre-encoded values for decode benchmarks
    private JsonElement encodedBool;
    private JsonElement encodedInt;
    private JsonElement encodedLong;
    private JsonElement encodedFloat;
    private JsonElement encodedDouble;
    private JsonElement encodedString;

    @Setup(Level.Trial)
    public void setup() {
        this.encodedBool = Codecs.BOOL.encodeStart(GsonOps.INSTANCE, TEST_BOOL).result().orElseThrow();
        this.encodedInt = Codecs.INT.encodeStart(GsonOps.INSTANCE, TEST_INT).result().orElseThrow();
        this.encodedLong = Codecs.LONG.encodeStart(GsonOps.INSTANCE, TEST_LONG).result().orElseThrow();
        this.encodedFloat = Codecs.FLOAT.encodeStart(GsonOps.INSTANCE, TEST_FLOAT).result().orElseThrow();
        this.encodedDouble = Codecs.DOUBLE.encodeStart(GsonOps.INSTANCE, TEST_DOUBLE).result().orElseThrow();
        this.encodedString = Codecs.STRING.encodeStart(GsonOps.INSTANCE, TEST_STRING).result().orElseThrow();
    }

    // ==================== Boolean ====================

    @Benchmark
    public void encodeBool(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.BOOL.encodeStart(GsonOps.INSTANCE, TEST_BOOL);
        blackhole.consume(result);
    }

    @Benchmark
    public void decodeBool(final Blackhole blackhole) {
        final DataResult<Pair<Boolean, JsonElement>> result = Codecs.BOOL.decode(GsonOps.INSTANCE, this.encodedBool);
        blackhole.consume(result);
    }

    // ==================== Integer ====================

    @Benchmark
    public void encodeInt(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.INT.encodeStart(GsonOps.INSTANCE, TEST_INT);
        blackhole.consume(result);
    }

    @Benchmark
    public void decodeInt(final Blackhole blackhole) {
        final DataResult<Pair<Integer, JsonElement>> result = Codecs.INT.decode(GsonOps.INSTANCE, this.encodedInt);
        blackhole.consume(result);
    }

    // ==================== Long ====================

    @Benchmark
    public void encodeLong(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.LONG.encodeStart(GsonOps.INSTANCE, TEST_LONG);
        blackhole.consume(result);
    }

    @Benchmark
    public void decodeLong(final Blackhole blackhole) {
        final DataResult<Pair<Long, JsonElement>> result = Codecs.LONG.decode(GsonOps.INSTANCE, this.encodedLong);
        blackhole.consume(result);
    }

    // ==================== Float ====================

    @Benchmark
    public void encodeFloat(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.FLOAT.encodeStart(GsonOps.INSTANCE, TEST_FLOAT);
        blackhole.consume(result);
    }

    @Benchmark
    public void decodeFloat(final Blackhole blackhole) {
        final DataResult<Pair<Float, JsonElement>> result = Codecs.FLOAT.decode(GsonOps.INSTANCE, this.encodedFloat);
        blackhole.consume(result);
    }

    // ==================== Double ====================

    @Benchmark
    public void encodeDouble(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.DOUBLE.encodeStart(GsonOps.INSTANCE, TEST_DOUBLE);
        blackhole.consume(result);
    }

    @Benchmark
    public void decodeDouble(final Blackhole blackhole) {
        final DataResult<Pair<Double, JsonElement>> result = Codecs.DOUBLE.decode(GsonOps.INSTANCE, this.encodedDouble);
        blackhole.consume(result);
    }

    // ==================== String ====================

    @Benchmark
    public void encodeString(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.STRING.encodeStart(GsonOps.INSTANCE, TEST_STRING);
        blackhole.consume(result);
    }

    @Benchmark
    public void decodeString(final Blackhole blackhole) {
        final DataResult<Pair<String, JsonElement>> result = Codecs.STRING.decode(GsonOps.INSTANCE, this.encodedString);
        blackhole.consume(result);
    }

    // ==================== Round Trip ====================

    @Benchmark
    public void roundTripInt(final Blackhole blackhole) {
        final DataResult<JsonElement> encoded = Codecs.INT.encodeStart(GsonOps.INSTANCE, TEST_INT);
        final DataResult<Pair<Integer, JsonElement>> decoded = encoded.flatMap(
                json -> Codecs.INT.decode(GsonOps.INSTANCE, json));
        blackhole.consume(decoded);
    }

    @Benchmark
    public void roundTripString(final Blackhole blackhole) {
        final DataResult<JsonElement> encoded = Codecs.STRING.encodeStart(GsonOps.INSTANCE, TEST_STRING);
        final DataResult<Pair<String, JsonElement>> decoded = encoded.flatMap(
                json -> Codecs.STRING.decode(GsonOps.INSTANCE, json));
        blackhole.consume(decoded);
    }
}
