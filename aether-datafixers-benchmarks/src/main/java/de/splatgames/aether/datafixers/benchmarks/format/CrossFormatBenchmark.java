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

package de.splatgames.aether.datafixers.benchmarks.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator;
import de.splatgames.aether.datafixers.benchmarks.util.PayloadSize;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
import de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps;
import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;
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
 * JMH benchmark for cross-format conversion performance between DynamicOps implementations.
 *
 * <p>This benchmark measures the overhead of converting data between different
 * serialization formats using the {@code DynamicOps.convertTo()} mechanism. Cross-format
 * conversion is essential when integrating systems that use different data formats
 * or when migrating data through format-agnostic DataFixers.</p>
 *
 * <h2>Conversion Pairs Benchmarked</h2>
 *
 * <h3>JSON Library Conversions</h3>
 * <ul>
 *   <li>{@link #gsonToJackson} - Gson JsonElement → Jackson JsonNode</li>
 *   <li>{@link #jacksonToGson} - Jackson JsonNode → Gson JsonElement</li>
 * </ul>
 *
 * <h3>JSON to YAML Conversions</h3>
 * <ul>
 *   <li>{@link #gsonToSnakeYaml} - Gson JsonElement → SnakeYAML Object</li>
 *   <li>{@link #snakeYamlToGson} - SnakeYAML Object → Gson JsonElement</li>
 * </ul>
 *
 * <h3>Jackson Ecosystem Conversions</h3>
 * <ul>
 *   <li>{@link #jacksonJsonToYaml} - Jackson JSON → Jackson YAML</li>
 *   <li>{@link #jacksonYamlToJson} - Jackson YAML → Jackson JSON</li>
 * </ul>
 *
 * <h3>YAML Library Conversions</h3>
 * <ul>
 *   <li>{@link #snakeYamlToJacksonYaml} - SnakeYAML → Jackson YAML</li>
 *   <li>{@link #jacksonYamlToSnakeYaml} - Jackson YAML → SnakeYAML</li>
 * </ul>
 *
 * <h2>Conversion Matrix</h2>
 * <table border="1">
 *   <tr>
 *     <th>From \ To</th>
 *     <th>Gson</th>
 *     <th>Jackson JSON</th>
 *     <th>SnakeYAML</th>
 *     <th>Jackson YAML</th>
 *   </tr>
 *   <tr>
 *     <td><b>Gson</b></td>
 *     <td>-</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *     <td>-</td>
 *   </tr>
 *   <tr>
 *     <td><b>Jackson JSON</b></td>
 *     <td>✓</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>✓</td>
 *   </tr>
 *   <tr>
 *     <td><b>SnakeYAML</b></td>
 *     <td>✓</td>
 *     <td>-</td>
 *     <td>-</td>
 *     <td>✓</td>
 *   </tr>
 *   <tr>
 *     <td><b>Jackson YAML</b></td>
 *     <td>-</td>
 *     <td>✓</td>
 *     <td>✓</td>
 *     <td>-</td>
 *   </tr>
 * </table>
 *
 * <h2>Parameters</h2>
 * <table border="1">
 *   <tr><th>Parameter</th><th>Values</th><th>Description</th></tr>
 *   <tr><td>payloadSize</td><td>SMALL, MEDIUM</td><td>Test data complexity</td></tr>
 * </table>
 *
 * <h2>Benchmark Configuration</h2>
 * <table border="1">
 *   <tr><th>Setting</th><th>Value</th></tr>
 *   <tr><td>Warmup</td><td>5 iterations, 1 second each</td></tr>
 *   <tr><td>Measurement</td><td>10 iterations, 1 second each</td></tr>
 *   <tr><td>Forks</td><td>2</td></tr>
 *   <tr><td>JVM Heap</td><td>2 GB min/max</td></tr>
 *   <tr><td>Time Unit</td><td>Microseconds</td></tr>
 * </table>
 *
 * <h2>Interpreting Results</h2>
 * <ul>
 *   <li><b>Same-ecosystem conversions</b> (e.g., Jackson JSON ↔ Jackson YAML) are
 *       typically faster due to shared internal representations</li>
 *   <li><b>Cross-ecosystem conversions</b> (e.g., Gson ↔ SnakeYAML) require full
 *       tree traversal and node creation</li>
 *   <li><b>Asymmetric performance</b>: A→B may differ from B→A due to different
 *       source iteration and target construction costs</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * # Run all cross-format benchmarks
 * java -jar benchmarks.jar CrossFormatBenchmark
 *
 * # Run JSON library conversions only
 * java -jar benchmarks.jar "CrossFormatBenchmark.*(gson|jackson)To(Gson|Jackson).*"
 *
 * # Run YAML conversions only
 * java -jar benchmarks.jar "CrossFormatBenchmark.*Yaml.*"
 *
 * # Compare with specific payload size
 * java -jar benchmarks.jar CrossFormatBenchmark -p payloadSize=MEDIUM
 * }</pre>
 *
 * @author Erik Pförtner
 * @see JsonBenchmark
 * @see YamlBenchmark
 * @see TomlXmlBenchmark
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps#convertTo(DynamicOps, Object)
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class CrossFormatBenchmark {

    /**
     * Payload size parameter controlling test data complexity.
     *
     * <p>Limited to SMALL and MEDIUM as cross-format conversion overhead
     * can be significant with large data sets.</p>
     */
    @Param({"SMALL", "MEDIUM"})
    private PayloadSize payloadSize;

    /**
     * Google Gson DynamicOps implementation.
     */
    private GsonOps gsonOps;

    /**
     * Jackson JSON DynamicOps implementation.
     */
    private JacksonJsonOps jacksonJsonOps;

    /**
     * SnakeYAML DynamicOps implementation using native Java types.
     */
    private SnakeYamlOps snakeYamlOps;

    /**
     * Jackson YAML DynamicOps implementation.
     */
    private JacksonYamlOps jacksonYamlOps;

    /**
     * Pre-generated Gson root element for conversion benchmarks.
     */
    private JsonElement gsonRoot;

    /**
     * Pre-generated Jackson JSON root node for conversion benchmarks.
     */
    private JsonNode jacksonJsonRoot;

    /**
     * Pre-generated SnakeYAML root object for conversion benchmarks.
     */
    private Object snakeYamlRoot;

    /**
     * Pre-generated Jackson YAML root node for conversion benchmarks.
     */
    private JsonNode jacksonYamlRoot;

    /**
     * Initializes all DynamicOps instances and pre-generates test data in each format.
     *
     * <p>Data is pre-generated in each format to ensure conversion benchmarks measure
     * only the conversion overhead, not data generation time.</p>
     */
    @Setup(Level.Trial)
    public void setup() {
        this.gsonOps = GsonOps.INSTANCE;
        this.jacksonJsonOps = JacksonJsonOps.INSTANCE;
        this.snakeYamlOps = SnakeYamlOps.INSTANCE;
        this.jacksonYamlOps = JacksonYamlOps.INSTANCE;

        this.gsonRoot = BenchmarkDataGenerator.generate(this.gsonOps, this.payloadSize).value();
        this.jacksonJsonRoot = BenchmarkDataGenerator.generate(this.jacksonJsonOps, this.payloadSize).value();
        this.snakeYamlRoot = BenchmarkDataGenerator.generate(this.snakeYamlOps, this.payloadSize).value();
        this.jacksonYamlRoot = BenchmarkDataGenerator.generate(this.jacksonYamlOps, this.payloadSize).value();
    }

    /**
     * Benchmarks conversion from Gson JsonElement to Jackson JsonNode.
     *
     * <p>Measures the overhead of converting between two JSON libraries.
     * Both represent JSON but use different internal tree structures.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void gsonToJackson(final Blackhole blackhole) {
        final JsonNode result = this.jacksonJsonOps.convertTo(this.gsonOps, this.gsonRoot);
        blackhole.consume(result);
    }

    /**
     * Benchmarks conversion from Jackson JsonNode to Gson JsonElement.
     *
     * <p>Measures the reverse conversion from Jackson to Gson representation.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonToGson(final Blackhole blackhole) {
        final JsonElement result = this.gsonOps.convertTo(this.jacksonJsonOps, this.jacksonJsonRoot);
        blackhole.consume(result);
    }

    /**
     * Benchmarks conversion from Gson JsonElement to SnakeYAML native types.
     *
     * <p>Measures cross-ecosystem conversion from JSON library to YAML library.
     * SnakeYAML uses native Java Maps and Lists internally.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void gsonToSnakeYaml(final Blackhole blackhole) {
        final Object result = this.snakeYamlOps.convertTo(this.gsonOps, this.gsonRoot);
        blackhole.consume(result);
    }

    /**
     * Benchmarks conversion from SnakeYAML native types to Gson JsonElement.
     *
     * <p>Measures cross-ecosystem conversion from YAML native types to JSON tree.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void snakeYamlToGson(final Blackhole blackhole) {
        final JsonElement result = this.gsonOps.convertTo(this.snakeYamlOps, this.snakeYamlRoot);
        blackhole.consume(result);
    }

    /**
     * Benchmarks conversion from Jackson JSON to Jackson YAML.
     *
     * <p>Measures conversion within the Jackson ecosystem. Both formats use
     * JsonNode internally, potentially enabling optimizations.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonJsonToYaml(final Blackhole blackhole) {
        final JsonNode result = this.jacksonYamlOps.convertTo(this.jacksonJsonOps, this.jacksonJsonRoot);
        blackhole.consume(result);
    }

    /**
     * Benchmarks conversion from Jackson YAML to Jackson JSON.
     *
     * <p>Measures reverse conversion within the Jackson ecosystem.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonYamlToJson(final Blackhole blackhole) {
        final JsonNode result = this.jacksonJsonOps.convertTo(this.jacksonYamlOps, this.jacksonYamlRoot);
        blackhole.consume(result);
    }

    /**
     * Benchmarks conversion from SnakeYAML native types to Jackson YAML JsonNode.
     *
     * <p>Measures conversion between two YAML libraries with different internal
     * representations (native Java types vs JsonNode).</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void snakeYamlToJacksonYaml(final Blackhole blackhole) {
        final JsonNode result = this.jacksonYamlOps.convertTo(this.snakeYamlOps, this.snakeYamlRoot);
        blackhole.consume(result);
    }

    /**
     * Benchmarks conversion from Jackson YAML JsonNode to SnakeYAML native types.
     *
     * <p>Measures reverse conversion from JsonNode to native Java Maps/Lists.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonYamlToSnakeYaml(final Blackhole blackhole) {
        final Object result = this.snakeYamlOps.convertTo(this.jacksonYamlOps, this.jacksonYamlRoot);
        blackhole.consume(result);
    }
}
