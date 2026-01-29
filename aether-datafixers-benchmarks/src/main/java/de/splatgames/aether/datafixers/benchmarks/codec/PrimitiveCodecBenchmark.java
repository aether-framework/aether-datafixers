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
 * JMH benchmark for primitive type codec encode/decode performance.
 *
 * <p>Measures the baseline performance of the fundamental codec operations for
 * primitive Java types. These benchmarks establish the lower bound for codec
 * performance and help identify overhead introduced by more complex codec
 * compositions.</p>
 *
 * <h2>Benchmark Categories</h2>
 *
 * <h3>Encode Benchmarks</h3>
 * <p>Measure Java value to JSON element conversion:</p>
 * <ul>
 *   <li>{@link #encodeBool} - Boolean encoding</li>
 *   <li>{@link #encodeInt} - Integer encoding</li>
 *   <li>{@link #encodeLong} - Long encoding</li>
 *   <li>{@link #encodeFloat} - Float encoding</li>
 *   <li>{@link #encodeDouble} - Double encoding</li>
 *   <li>{@link #encodeString} - String encoding</li>
 * </ul>
 *
 * <h3>Decode Benchmarks</h3>
 * <p>Measure JSON element to Java value conversion:</p>
 * <ul>
 *   <li>{@link #decodeBool} - Boolean decoding</li>
 *   <li>{@link #decodeInt} - Integer decoding</li>
 *   <li>{@link #decodeLong} - Long decoding</li>
 *   <li>{@link #decodeFloat} - Float decoding</li>
 *   <li>{@link #decodeDouble} - Double decoding</li>
 *   <li>{@link #decodeString} - String decoding</li>
 * </ul>
 *
 * <h3>Round-Trip Benchmarks</h3>
 * <p>Measure complete encode-then-decode cycles:</p>
 * <ul>
 *   <li>{@link #roundTripIntDirect} - Integer round-trip with direct result extraction</li>
 *   <li>{@link #roundTripStringDirect} - String round-trip with direct result extraction</li>
 * </ul>
 *
 * <h2>Benchmark Configuration</h2>
 * <table border="1">
 *   <tr><th>Setting</th><th>Value</th></tr>
 *   <tr><td>Warmup</td><td>5 iterations, 1 second each</td></tr>
 *   <tr><td>Measurement</td><td>10 iterations, 1 second each</td></tr>
 *   <tr><td>Forks</td><td>2 (for JIT variance mitigation)</td></tr>
 *   <tr><td>JVM Heap</td><td>2 GB min/max</td></tr>
 *   <tr><td>Time Unit</td><td>Nanoseconds (for fine-grained primitive ops)</td></tr>
 * </table>
 *
 * <h2>Test Values</h2>
 * <table border="1">
 *   <tr><th>Type</th><th>Value</th><th>Notes</th></tr>
 *   <tr><td>boolean</td><td>{@code true}</td><td>Single bit representation</td></tr>
 *   <tr><td>int</td><td>{@code 42}</td><td>Small positive integer</td></tr>
 *   <tr><td>long</td><td>{@code 123456789L}</td><td>Value exceeding int range representation</td></tr>
 *   <tr><td>float</td><td>{@code 3.14159f}</td><td>Pi approximation (tests decimal handling)</td></tr>
 *   <tr><td>double</td><td>{@code 2.718281828}</td><td>Euler's number (tests precision)</td></tr>
 *   <tr><td>String</td><td>{@code "benchmark-test-string"}</td><td>21-character ASCII string</td></tr>
 * </table>
 *
 * <h2>Interpreting Results</h2>
 * <ul>
 *   <li><b>Encode vs Decode</b>: Encoding typically allocates new JSON elements; decoding
 *       extracts values from existing elements. Similar performance is expected.</li>
 *   <li><b>Numeric types</b>: All numeric types should have similar performance as they
 *       map directly to JSON number primitives.</li>
 *   <li><b>String codec</b>: May show slightly different characteristics due to string
 *       interning and character encoding considerations.</li>
 *   <li><b>Round-trip overhead</b>: Should be approximately encode + decode time plus
 *       minimal DataResult unwrapping overhead.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * # Run all primitive codec benchmarks
 * java -jar benchmarks.jar PrimitiveCodecBenchmark
 *
 * # Run only encode benchmarks
 * java -jar benchmarks.jar "PrimitiveCodecBenchmark.encode.*"
 *
 * # Run only decode benchmarks
 * java -jar benchmarks.jar "PrimitiveCodecBenchmark.decode.*"
 *
 * # Compare specific types
 * java -jar benchmarks.jar "PrimitiveCodecBenchmark.*(Int|Long).*"
 *
 * # Quick validation run
 * java -jar benchmarks.jar PrimitiveCodecBenchmark -wi 1 -i 1 -f 1
 *
 * # Generate CSV for spreadsheet analysis
 * java -jar benchmarks.jar PrimitiveCodecBenchmark -rf csv -rff primitive_results.csv
 * }</pre>
 *
 * @author Erik Pförtner
 * @see CollectionCodecBenchmark
 * @see de.splatgames.aether.datafixers.api.codec.Codecs
 * @see de.splatgames.aether.datafixers.codec.json.gson.GsonOps
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class PrimitiveCodecBenchmark {

    /**
     * Test boolean value for encoding benchmarks.
     */
    private static final boolean TEST_BOOL = true;

    /**
     * Test integer value for encoding benchmarks.
     *
     * <p>A small positive integer that fits in a single JSON number token.</p>
     */
    private static final int TEST_INT = 42;

    /**
     * Test long value for encoding benchmarks.
     *
     * <p>A value that exceeds typical int range to test long-specific handling.</p>
     */
    private static final long TEST_LONG = 123456789L;

    /**
     * Test float value for encoding benchmarks.
     *
     * <p>Pi approximation to test decimal point handling and precision.</p>
     */
    private static final float TEST_FLOAT = 3.14159f;

    /**
     * Test double value for encoding benchmarks.
     *
     * <p>Euler's number with extended precision to test double encoding accuracy.</p>
     */
    private static final double TEST_DOUBLE = 2.718281828;

    /**
     * Test string value for encoding benchmarks.
     *
     * <p>A 21-character ASCII string representing typical field values.</p>
     */
    private static final String TEST_STRING = "benchmark-test-string";

    /**
     * The DynamicOps implementation used for all codec operations.
     *
     * <p>GsonOps is used as the reference implementation for JSON format benchmarks.</p>
     */
    private GsonOps ops;

    /**
     * Pre-encoded boolean JSON element for decode benchmarks.
     */
    private JsonElement encodedBool;

    /**
     * Pre-encoded integer JSON element for decode benchmarks.
     */
    private JsonElement encodedInt;

    /**
     * Pre-encoded long JSON element for decode benchmarks.
     */
    private JsonElement encodedLong;

    /**
     * Pre-encoded float JSON element for decode benchmarks.
     */
    private JsonElement encodedFloat;

    /**
     * Pre-encoded double JSON element for decode benchmarks.
     */
    private JsonElement encodedDouble;

    /**
     * Pre-encoded string JSON element for decode benchmarks.
     */
    private JsonElement encodedString;

    /**
     * Initializes pre-encoded JSON elements for decode benchmarks.
     *
     * <p>Pre-encoding ensures decode benchmarks measure only decoding performance
     * without encoding overhead. All test values are encoded once at trial start.</p>
     */
    @Setup(Level.Trial)
    public void setup() {
        this.ops = GsonOps.INSTANCE;

        this.encodedBool = Codecs.BOOL.encodeStart(this.ops, TEST_BOOL).result().orElseThrow();
        this.encodedInt = Codecs.INT.encodeStart(this.ops, TEST_INT).result().orElseThrow();
        this.encodedLong = Codecs.LONG.encodeStart(this.ops, TEST_LONG).result().orElseThrow();
        this.encodedFloat = Codecs.FLOAT.encodeStart(this.ops, TEST_FLOAT).result().orElseThrow();
        this.encodedDouble = Codecs.DOUBLE.encodeStart(this.ops, TEST_DOUBLE).result().orElseThrow();
        this.encodedString = Codecs.STRING.encodeStart(this.ops, TEST_STRING).result().orElseThrow();
    }

    // ==================== Boolean Benchmarks ====================

    /**
     * Benchmarks boolean value encoding to JSON.
     *
     * <p>Measures the performance of converting a Java {@code boolean} to a
     * JSON boolean element via {@link Codecs#BOOL}.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeBool(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.BOOL.encodeStart(this.ops, TEST_BOOL);
        blackhole.consume(result);
    }

    /**
     * Benchmarks boolean value decoding from JSON.
     *
     * <p>Measures the performance of extracting a Java {@code Boolean} from a
     * pre-encoded JSON boolean element.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeBool(final Blackhole blackhole) {
        final DataResult<Pair<Boolean, JsonElement>> result = Codecs.BOOL.decode(this.ops, this.encodedBool);
        blackhole.consume(result);
    }

    // ==================== Integer Benchmarks ====================

    /**
     * Benchmarks integer value encoding to JSON.
     *
     * <p>Measures the performance of converting a Java {@code int} to a
     * JSON number element via {@link Codecs#INT}.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeInt(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.INT.encodeStart(this.ops, TEST_INT);
        blackhole.consume(result);
    }

    /**
     * Benchmarks integer value decoding from JSON.
     *
     * <p>Measures the performance of extracting a Java {@code Integer} from a
     * pre-encoded JSON number element.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeInt(final Blackhole blackhole) {
        final DataResult<Pair<Integer, JsonElement>> result = Codecs.INT.decode(this.ops, this.encodedInt);
        blackhole.consume(result);
    }

    // ==================== Long Benchmarks ====================

    /**
     * Benchmarks long value encoding to JSON.
     *
     * <p>Measures the performance of converting a Java {@code long} to a
     * JSON number element via {@link Codecs#LONG}.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeLong(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.LONG.encodeStart(this.ops, TEST_LONG);
        blackhole.consume(result);
    }

    /**
     * Benchmarks long value decoding from JSON.
     *
     * <p>Measures the performance of extracting a Java {@code Long} from a
     * pre-encoded JSON number element.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeLong(final Blackhole blackhole) {
        final DataResult<Pair<Long, JsonElement>> result = Codecs.LONG.decode(this.ops, this.encodedLong);
        blackhole.consume(result);
    }

    // ==================== Float Benchmarks ====================

    /**
     * Benchmarks float value encoding to JSON.
     *
     * <p>Measures the performance of converting a Java {@code float} to a
     * JSON number element via {@link Codecs#FLOAT}. Float encoding involves
     * decimal representation which may differ from integer encoding.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeFloat(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.FLOAT.encodeStart(this.ops, TEST_FLOAT);
        blackhole.consume(result);
    }

    /**
     * Benchmarks float value decoding from JSON.
     *
     * <p>Measures the performance of extracting a Java {@code Float} from a
     * pre-encoded JSON number element. Decoding involves parsing the decimal
     * representation back to IEEE 754 single-precision format.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeFloat(final Blackhole blackhole) {
        final DataResult<Pair<Float, JsonElement>> result = Codecs.FLOAT.decode(this.ops, this.encodedFloat);
        blackhole.consume(result);
    }

    // ==================== Double Benchmarks ====================

    /**
     * Benchmarks double value encoding to JSON.
     *
     * <p>Measures the performance of converting a Java {@code double} to a
     * JSON number element via {@link Codecs#DOUBLE}. Double encoding preserves
     * higher precision than float but uses similar mechanisms.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeDouble(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.DOUBLE.encodeStart(this.ops, TEST_DOUBLE);
        blackhole.consume(result);
    }

    /**
     * Benchmarks double value decoding from JSON.
     *
     * <p>Measures the performance of extracting a Java {@code Double} from a
     * pre-encoded JSON number element.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeDouble(final Blackhole blackhole) {
        final DataResult<Pair<Double, JsonElement>> result = Codecs.DOUBLE.decode(this.ops, this.encodedDouble);
        blackhole.consume(result);
    }

    // ==================== String Benchmarks ====================

    /**
     * Benchmarks string value encoding to JSON.
     *
     * <p>Measures the performance of converting a Java {@code String} to a
     * JSON string element via {@link Codecs#STRING}. String encoding may involve
     * escape sequence handling for special characters.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeString(final Blackhole blackhole) {
        final DataResult<JsonElement> result = Codecs.STRING.encodeStart(this.ops, TEST_STRING);
        blackhole.consume(result);
    }

    /**
     * Benchmarks string value decoding from JSON.
     *
     * <p>Measures the performance of extracting a Java {@code String} from a
     * pre-encoded JSON string element.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeString(final Blackhole blackhole) {
        final DataResult<Pair<String, JsonElement>> result = Codecs.STRING.decode(this.ops, this.encodedString);
        blackhole.consume(result);
    }

    // ==================== Round-Trip Benchmarks ====================

    /**
     * Benchmarks complete integer round-trip (encode then decode).
     *
     * <p>Measures the combined performance of encoding a Java {@code int} to JSON
     * and immediately decoding it back. Uses direct result extraction via
     * {@code result().orElseThrow()} to measure the typical non-functional usage pattern.</p>
     *
     * <p>Round-trip performance is important for scenarios where data is temporarily
     * serialized (e.g., caching, IPC) and immediately deserialized.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void roundTripIntDirect(final Blackhole blackhole) {
        final JsonElement json = Codecs.INT.encodeStart(this.ops, TEST_INT).result().orElseThrow();
        final Pair<Integer, JsonElement> decoded = Codecs.INT.decode(this.ops, json).result().orElseThrow();
        blackhole.consume(decoded);
    }

    /**
     * Benchmarks complete string round-trip (encode then decode).
     *
     * <p>Measures the combined performance of encoding a Java {@code String} to JSON
     * and immediately decoding it back. String round-trips may involve additional
     * overhead from string object creation compared to primitive numeric types.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void roundTripStringDirect(final Blackhole blackhole) {
        final JsonElement json = Codecs.STRING.encodeStart(this.ops, TEST_STRING).result().orElseThrow();
        final Pair<String, JsonElement> decoded = Codecs.STRING.decode(this.ops, json).result().orElseThrow();
        blackhole.consume(decoded);
    }
}
