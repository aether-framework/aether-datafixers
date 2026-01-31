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
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator;
import de.splatgames.aether.datafixers.benchmarks.util.PayloadSize;
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
 * JMH benchmark comparing YAML DynamicOps implementations: SnakeYAML vs Jackson YAML.
 *
 * <p>This benchmark measures the performance of YAML-based operations using two
 * different underlying libraries: SnakeYAML ({@link SnakeYamlOps}) and Jackson YAML
 * ({@link JacksonYamlOps}). YAML is commonly used for configuration files and
 * human-readable data serialization.</p>
 *
 * <h2>Benchmark Categories</h2>
 *
 * <h3>Data Generation</h3>
 * <p>Measure Dynamic object construction performance:</p>
 * <ul>
 *   <li>{@link #snakeYamlGenerate} - Create Dynamic using SnakeYamlOps</li>
 *   <li>{@link #jacksonYamlGenerate} - Create Dynamic using JacksonYamlOps</li>
 * </ul>
 *
 * <h3>Field Access</h3>
 * <p>Measure field read operations on existing data:</p>
 * <ul>
 *   <li>{@link #snakeYamlFieldRead} - Read field from SnakeYAML-backed Dynamic</li>
 *   <li>{@link #jacksonYamlFieldRead} - Read field from Jackson YAML-backed Dynamic</li>
 * </ul>
 *
 * <h3>Field Modification</h3>
 * <p>Measure field write/set operations:</p>
 * <ul>
 *   <li>{@link #snakeYamlFieldSet} - Set field on SnakeYAML-backed Dynamic</li>
 *   <li>{@link #jacksonYamlFieldSet} - Set field on Jackson YAML-backed Dynamic</li>
 * </ul>
 *
 * <h3>Migration</h3>
 * <p>Measure DataFixer migration performance:</p>
 * <ul>
 *   <li>{@link #snakeYamlMigration} - Apply fix to SnakeYAML-backed data</li>
 *   <li>{@link #jacksonYamlMigration} - Apply fix to Jackson YAML-backed data</li>
 * </ul>
 *
 * <h2>Implementations Compared</h2>
 * <table border="1">
 *   <tr><th>Implementation</th><th>Library</th><th>Node Type</th><th>Characteristics</th></tr>
 *   <tr>
 *     <td>{@link SnakeYamlOps}</td>
 *     <td>SnakeYAML</td>
 *     <td>{@code Object} (native Java types)</td>
 *     <td>Native YAML library, uses Maps/Lists, anchors &amp; aliases support</td>
 *   </tr>
 *   <tr>
 *     <td>{@link JacksonYamlOps}</td>
 *     <td>Jackson Dataformat YAML</td>
 *     <td>{@code JsonNode}</td>
 *     <td>Unified Jackson API, shares code with JSON, streaming support</td>
 *   </tr>
 * </table>
 *
 * <h2>Parameters</h2>
 * <table border="1">
 *   <tr><th>Parameter</th><th>Values</th><th>Description</th></tr>
 *   <tr><td>payloadSize</td><td>SMALL, MEDIUM, LARGE</td><td>Test data complexity</td></tr>
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
 * <h2>Usage</h2>
 * <pre>{@code
 * # Run all YAML benchmarks
 * java -jar benchmarks.jar YamlBenchmark
 *
 * # Compare only generation performance
 * java -jar benchmarks.jar "YamlBenchmark.*Generate"
 *
 * # Run SnakeYAML-only benchmarks
 * java -jar benchmarks.jar "YamlBenchmark.snakeYaml.*"
 *
 * # Run with specific payload size
 * java -jar benchmarks.jar YamlBenchmark -p payloadSize=MEDIUM
 * }</pre>
 *
 * @author Erik Pförtner
 * @see JsonBenchmark
 * @see TomlXmlBenchmark
 * @see CrossFormatBenchmark
 * @see de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps
 * @see de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public final class YamlBenchmark {

    /**
     * Field name used for read/write benchmarks.
     *
     * <p>References the first string field generated by {@link BenchmarkDataGenerator}.</p>
     */
    private static final String FIELD_NAME = "stringField0";

    /**
     * Payload size parameter controlling test data complexity.
     *
     * <p>Injected by JMH to run benchmarks across different data sizes.</p>
     */
    @Param({"SMALL", "MEDIUM", "LARGE"})
    private PayloadSize payloadSize;

    /**
     * SnakeYAML DynamicOps implementation using native Java types.
     */
    private SnakeYamlOps snakeOps;

    /**
     * Jackson YAML DynamicOps implementation using JsonNode.
     */
    private JacksonYamlOps jacksonOps;

    /**
     * Pre-generated test data using SnakeYAML.
     */
    private Dynamic<Object> snakeYamlData;

    /**
     * Pre-generated test data using Jackson YAML.
     */
    private Dynamic<JsonNode> jacksonYamlData;

    /**
     * DataFixer for migration benchmarks.
     */
    private DataFixer fixer;

    /**
     * Source version for migrations (v1).
     */
    private DataVersion fromVersion;

    /**
     * Target version for migrations (v2).
     */
    private DataVersion toVersion;

    /**
     * Initializes DynamicOps instances, test data, and DataFixer.
     *
     * <p>Both SnakeYAML and Jackson YAML data are pre-generated to isolate
     * benchmark measurements from data creation overhead.</p>
     */
    @Setup(Level.Trial)
    public void setup() {
        this.snakeOps = SnakeYamlOps.INSTANCE;
        this.jacksonOps = JacksonYamlOps.INSTANCE;

        this.snakeYamlData = BenchmarkDataGenerator.generate(this.snakeOps, this.payloadSize);
        this.jacksonYamlData = BenchmarkDataGenerator.generate(this.jacksonOps, this.payloadSize);

        this.fixer = BenchmarkBootstrap.createSingleFixFixer();
        this.fromVersion = new DataVersion(1);
        this.toVersion = new DataVersion(2);
    }

    // ==================== Data Generation Benchmarks ====================

    /**
     * Benchmarks Dynamic object generation using SnakeYamlOps.
     *
     * <p>Measures the time to create a complete test data structure using
     * SnakeYAML's native Java type representation (Maps and Lists).</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void snakeYamlGenerate(final Blackhole blackhole) {
        final Dynamic<Object> data = BenchmarkDataGenerator.generate(this.snakeOps, this.payloadSize);
        blackhole.consume(data);
    }

    /**
     * Benchmarks Dynamic object generation using JacksonYamlOps.
     *
     * <p>Measures the time to create a complete test data structure using
     * Jackson's JsonNode representation for YAML.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonYamlGenerate(final Blackhole blackhole) {
        final Dynamic<JsonNode> data = BenchmarkDataGenerator.generate(this.jacksonOps, this.payloadSize);
        blackhole.consume(data);
    }

    // ==================== Field Access Benchmarks ====================

    /**
     * Benchmarks field read access on SnakeYAML-backed Dynamic.
     *
     * <p>Measures the time to retrieve a single field from a pre-existing
     * SnakeYAML-based Dynamic object (backed by Java Map).</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void snakeYamlFieldRead(final Blackhole blackhole) {
        final Dynamic<Object> field = this.snakeYamlData.get(FIELD_NAME);
        blackhole.consume(field);
    }

    /**
     * Benchmarks field read access on Jackson YAML-backed Dynamic.
     *
     * <p>Measures the time to retrieve a single field from a pre-existing
     * Jackson YAML-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonYamlFieldRead(final Blackhole blackhole) {
        final Dynamic<JsonNode> field = this.jacksonYamlData.get(FIELD_NAME);
        blackhole.consume(field);
    }

    // ==================== Field Modification Benchmarks ====================

    /**
     * Benchmarks field set operation on SnakeYAML-backed Dynamic.
     *
     * <p>Measures the time to add a new field to a SnakeYAML-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void snakeYamlFieldSet(final Blackhole blackhole) {
        final Dynamic<Object> result = this.snakeYamlData.set(
                "newField",
                this.snakeYamlData.createString("newValue")
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks field set operation on Jackson YAML-backed Dynamic.
     *
     * <p>Measures the time to add a new field to a Jackson YAML-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonYamlFieldSet(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.jacksonYamlData.set(
                "newField",
                this.jacksonYamlData.createString("newValue")
        );
        blackhole.consume(result);
    }

    // ==================== Migration Benchmarks ====================

    /**
     * Benchmarks DataFixer migration on SnakeYAML-backed data.
     *
     * <p>Measures the time to apply a single fix migration to SnakeYAML-based
     * Dynamic data.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void snakeYamlMigration(final Blackhole blackhole) {
        final Dynamic<Object> result = this.fixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.snakeYamlData,
                this.fromVersion,
                this.toVersion
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks DataFixer migration on Jackson YAML-backed data.
     *
     * <p>Measures the time to apply a single fix migration to Jackson YAML-based
     * Dynamic data.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonYamlMigration(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.fixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.jacksonYamlData,
                this.fromVersion,
                this.toVersion
        );
        blackhole.consume(result);
    }
}
