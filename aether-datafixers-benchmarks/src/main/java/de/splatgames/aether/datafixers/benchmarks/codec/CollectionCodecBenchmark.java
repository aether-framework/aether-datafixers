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
 * <p>Measures the performance of list codec operations with parameterized collection
 * sizes. These benchmarks reveal how codec performance scales with data volume and
 * help identify potential bottlenecks in collection traversal and element processing.</p>
 *
 * <h2>Benchmark Categories</h2>
 *
 * <h3>String List Benchmarks</h3>
 * <p>Measure {@code List<String>} codec operations:</p>
 * <ul>
 *   <li>{@link #encodeStringList} - Encode string list to JSON array</li>
 *   <li>{@link #decodeStringList} - Decode JSON array to string list</li>
 *   <li>{@link #roundTripStringListDirect} - Complete round-trip with direct extraction</li>
 *   <li>{@link #roundTripStringListFunctional} - Complete round-trip using functional API</li>
 * </ul>
 *
 * <h3>Integer List Benchmarks</h3>
 * <p>Measure {@code List<Integer>} codec operations:</p>
 * <ul>
 *   <li>{@link #encodeIntList} - Encode integer list to JSON array</li>
 *   <li>{@link #decodeIntList} - Decode JSON array to integer list</li>
 *   <li>{@link #roundTripIntListDirect} - Complete round-trip with direct extraction</li>
 *   <li>{@link #roundTripIntListFunctional} - Complete round-trip using functional API</li>
 * </ul>
 *
 * <h2>Parameters</h2>
 * <table border="1">
 *   <tr><th>Parameter</th><th>Values</th><th>Description</th></tr>
 *   <tr><td>listSize</td><td>10, 100, 1000</td><td>Number of elements in the test list</td></tr>
 * </table>
 *
 * <h2>Benchmark Configuration</h2>
 * <table border="1">
 *   <tr><th>Setting</th><th>Value</th></tr>
 *   <tr><td>Warmup</td><td>5 iterations, 1 second each</td></tr>
 *   <tr><td>Measurement</td><td>10 iterations, 1 second each</td></tr>
 *   <tr><td>Forks</td><td>2 (for JIT variance mitigation)</td></tr>
 *   <tr><td>JVM Heap</td><td>2 GB min/max</td></tr>
 *   <tr><td>Time Unit</td><td>Microseconds (appropriate for collection operations)</td></tr>
 * </table>
 *
 * <h2>Test Data Generation</h2>
 * <table border="1">
 *   <tr><th>Collection</th><th>Element Pattern</th><th>Example (size=3)</th></tr>
 *   <tr><td>String List</td><td>{@code "item-" + index}</td><td>["item-0", "item-1", "item-2"]</td></tr>
 *   <tr><td>Integer List</td><td>{@code index}</td><td>[0, 1, 2]</td></tr>
 * </table>
 *
 * <h2>Interpreting Results</h2>
 * <ul>
 *   <li><b>Linear scaling</b>: Expected behavior where time scales proportionally with list size.
 *       If 100 elements takes 10x longer than 10 elements, scaling is linear.</li>
 *   <li><b>Sub-linear scaling</b>: Better than expected, may indicate JIT optimizations
 *       or efficient batch processing.</li>
 *   <li><b>Super-linear scaling</b>: Performance degrades faster than list size grows.
 *       May indicate memory pressure, GC overhead, or algorithmic inefficiency.</li>
 *   <li><b>String vs Integer</b>: String lists typically have higher overhead due to
 *       object allocation and potential string interning effects.</li>
 *   <li><b>Direct vs Functional</b>: Functional API (using {@code flatMap}) may show
 *       slight overhead from lambda creation and DataResult chaining.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * # Run all collection codec benchmarks
 * java -jar benchmarks.jar CollectionCodecBenchmark
 *
 * # Run with specific list size
 * java -jar benchmarks.jar CollectionCodecBenchmark -p listSize=1000
 *
 * # Run only string list benchmarks
 * java -jar benchmarks.jar "CollectionCodecBenchmark.*String.*"
 *
 * # Run only encode benchmarks
 * java -jar benchmarks.jar "CollectionCodecBenchmark.encode.*"
 *
 * # Compare direct vs functional round-trip
 * java -jar benchmarks.jar "CollectionCodecBenchmark.roundTrip.*"
 *
 * # Quick validation run
 * java -jar benchmarks.jar CollectionCodecBenchmark -wi 1 -i 1 -f 1
 *
 * # Generate JSON report for analysis
 * java -jar benchmarks.jar CollectionCodecBenchmark -rf json -rff collection_results.json
 * }</pre>
 *
 * @author Erik Pförtner
 * @see PrimitiveCodecBenchmark
 * @see de.splatgames.aether.datafixers.api.codec.Codecs#list(Codec)
 * @see de.splatgames.aether.datafixers.codec.json.gson.GsonOps
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class CollectionCodecBenchmark {

    /**
     * The number of elements in test lists, injected by JMH.
     *
     * <p>This parameter controls the size of both string and integer lists.
     * Different sizes reveal scaling characteristics of the list codec:</p>
     * <ul>
     *   <li><b>10</b>: Small list baseline, minimal memory/GC impact</li>
     *   <li><b>100</b>: Medium list, typical real-world collection size</li>
     *   <li><b>1000</b>: Large list stress test, reveals scaling behavior</li>
     * </ul>
     */
    @Param({"10", "100", "1000"})
    private int listSize;

    /**
     * The DynamicOps implementation used for all codec operations.
     *
     * <p>GsonOps is used as the reference JSON implementation for benchmarks.</p>
     */
    private GsonOps ops;

    /**
     * Codec for encoding/decoding {@code List<String>}.
     *
     * <p>Created via {@link Codecs#list(Codec)} wrapping {@link Codecs#STRING}.</p>
     */
    private Codec<List<String>> stringListCodec;

    /**
     * Codec for encoding/decoding {@code List<Integer>}.
     *
     * <p>Created via {@link Codecs#list(Codec)} wrapping {@link Codecs#INT}.</p>
     */
    private Codec<List<Integer>> intListCodec;

    /**
     * Test string list populated with {@link #listSize} elements.
     *
     * <p>Elements follow the pattern "item-0", "item-1", ..., "item-(n-1)".</p>
     */
    private List<String> stringList;

    /**
     * Test integer list populated with {@link #listSize} elements.
     *
     * <p>Elements are sequential integers: 0, 1, 2, ..., (n-1).</p>
     */
    private List<Integer> intList;

    /**
     * Pre-encoded JSON array for string list decode benchmarks.
     *
     * <p>Created during setup to isolate decode performance from encoding overhead.</p>
     */
    private JsonElement encodedStringList;

    /**
     * Pre-encoded JSON array for integer list decode benchmarks.
     *
     * <p>Created during setup to isolate decode performance from encoding overhead.</p>
     */
    private JsonElement encodedIntList;

    /**
     * Initializes codecs, test data, and pre-encoded JSON elements.
     *
     * <p>This setup method:</p>
     * <ol>
     *   <li>Creates list codecs by composing primitive codecs with {@link Codecs#list(Codec)}</li>
     *   <li>Populates test lists with {@link #listSize} elements each</li>
     *   <li>Pre-encodes both lists to JSON for decode benchmark isolation</li>
     * </ol>
     *
     * <p>Using {@link ArrayList} with pre-sized capacity avoids resizing overhead
     * during population.</p>
     */
    @Setup(Level.Trial)
    public void setup() {
        this.ops = GsonOps.INSTANCE;

        this.stringListCodec = Codecs.list(Codecs.STRING);
        this.intListCodec = Codecs.list(Codecs.INT);

        this.stringList = new ArrayList<>(this.listSize);
        this.intList = new ArrayList<>(this.listSize);

        for (int i = 0; i < this.listSize; i++) {
            this.stringList.add("item-" + i);
            this.intList.add(i);
        }

        this.encodedStringList = this.stringListCodec.encodeStart(this.ops, this.stringList)
                .result().orElseThrow();
        this.encodedIntList = this.intListCodec.encodeStart(this.ops, this.intList)
                .result().orElseThrow();
    }

    // ==================== String List Benchmarks ====================

    /**
     * Benchmarks string list encoding to JSON array.
     *
     * <p>Measures the performance of converting a {@code List<String>} to a JSON
     * array element. Each string element is individually encoded and added to the
     * resulting array.</p>
     *
     * <p>Performance factors:</p>
     * <ul>
     *   <li>List iteration overhead</li>
     *   <li>Per-element string encoding cost</li>
     *   <li>JSON array construction and element addition</li>
     * </ul>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeStringList(final Blackhole blackhole) {
        final DataResult<JsonElement> result = this.stringListCodec.encodeStart(this.ops, this.stringList);
        blackhole.consume(result);
    }

    /**
     * Benchmarks string list decoding from JSON array.
     *
     * <p>Measures the performance of extracting a {@code List<String>} from a
     * pre-encoded JSON array. Each array element is decoded to a string and
     * collected into the result list.</p>
     *
     * <p>Performance factors:</p>
     * <ul>
     *   <li>JSON array traversal</li>
     *   <li>Per-element string extraction</li>
     *   <li>Result list construction and population</li>
     * </ul>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeStringList(final Blackhole blackhole) {
        final DataResult<Pair<List<String>, JsonElement>> result = this.stringListCodec.decode(this.ops, this.encodedStringList);
        blackhole.consume(result);
    }

    // ==================== Integer List Benchmarks ====================

    /**
     * Benchmarks integer list encoding to JSON array.
     *
     * <p>Measures the performance of converting a {@code List<Integer>} to a JSON
     * array element. Integer encoding is typically faster than string encoding
     * due to simpler value representation.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void encodeIntList(final Blackhole blackhole) {
        final DataResult<JsonElement> result = this.intListCodec.encodeStart(this.ops, this.intList);
        blackhole.consume(result);
    }

    /**
     * Benchmarks integer list decoding from JSON array.
     *
     * <p>Measures the performance of extracting a {@code List<Integer>} from a
     * pre-encoded JSON array. Integer decoding involves numeric parsing from
     * JSON number elements.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void decodeIntList(final Blackhole blackhole) {
        final DataResult<Pair<List<Integer>, JsonElement>> result = this.intListCodec.decode(this.ops, this.encodedIntList);
        blackhole.consume(result);
    }

    // ==================== Round-Trip Benchmarks (Direct Style) ====================

    /**
     * Benchmarks complete string list round-trip with direct result extraction.
     *
     * <p>Measures the combined performance of encoding a {@code List<String>} to JSON
     * and immediately decoding it back. Uses {@code result().orElseThrow()} for
     * direct value extraction, representing typical imperative usage patterns.</p>
     *
     * <p>This benchmark is useful for scenarios where data is temporarily serialized
     * (e.g., caching, message passing) and immediately deserialized.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void roundTripStringListDirect(final Blackhole blackhole) {
        final JsonElement json = this.stringListCodec.encodeStart(this.ops, this.stringList)
                .result().orElseThrow();
        final Pair<List<String>, JsonElement> decoded = this.stringListCodec.decode(this.ops, json)
                .result().orElseThrow();
        blackhole.consume(decoded);
    }

    /**
     * Benchmarks complete integer list round-trip with direct result extraction.
     *
     * <p>Measures the combined performance of encoding a {@code List<Integer>} to JSON
     * and immediately decoding it back using direct value extraction.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void roundTripIntListDirect(final Blackhole blackhole) {
        final JsonElement json = this.intListCodec.encodeStart(this.ops, this.intList)
                .result().orElseThrow();
        final Pair<List<Integer>, JsonElement> decoded = this.intListCodec.decode(this.ops, json)
                .result().orElseThrow();
        blackhole.consume(decoded);
    }

    // ==================== Round-Trip Benchmarks (Functional Style) ====================

    /**
     * Benchmarks complete string list round-trip using functional API.
     *
     * <p>Measures the combined performance of encoding and decoding using
     * {@link DataResult#flatMap} for monadic composition. This represents
     * the functional programming style where operations are chained without
     * explicit result unwrapping.</p>
     *
     * <p>Comparing with {@link #roundTripStringListDirect} reveals the overhead
     * (if any) of the functional API approach versus direct extraction.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void roundTripStringListFunctional(final Blackhole blackhole) {
        final DataResult<JsonElement> encoded = this.stringListCodec.encodeStart(this.ops, this.stringList);
        final DataResult<Pair<List<String>, JsonElement>> decoded = encoded.flatMap(
                json -> this.stringListCodec.decode(this.ops, json)
        );
        blackhole.consume(decoded);
    }

    /**
     * Benchmarks complete integer list round-trip using functional API.
     *
     * <p>Measures the combined performance of encoding and decoding using
     * monadic composition via {@link DataResult#flatMap}.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void roundTripIntListFunctional(final Blackhole blackhole) {
        final DataResult<JsonElement> encoded = this.intListCodec.encodeStart(this.ops, this.intList);
        final DataResult<Pair<List<Integer>, JsonElement>> decoded = encoded.flatMap(
                json -> this.intListCodec.decode(this.ops, json)
        );
        blackhole.consume(decoded);
    }
}
