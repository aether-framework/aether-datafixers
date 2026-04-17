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
import de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps;
import de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps;
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
 * JMH benchmark for TOML and XML DynamicOps implementations via Jackson.
 *
 * <p>This benchmark measures the performance of TOML and XML format operations
 * using Jackson-based implementations ({@link JacksonTomlOps} and {@link JacksonXmlOps}).
 * Both formats share Jackson's unified API, enabling direct performance comparison.</p>
 *
 * <h2>Benchmark Categories</h2>
 *
 * <h3>Data Generation</h3>
 * <p>Measure Dynamic object construction performance:</p>
 * <ul>
 *   <li>{@link #tomlGenerate} - Create Dynamic using JacksonTomlOps</li>
 *   <li>{@link #xmlGenerate} - Create Dynamic using JacksonXmlOps</li>
 * </ul>
 *
 * <h3>Field Access</h3>
 * <p>Measure field read operations on existing data:</p>
 * <ul>
 *   <li>{@link #tomlFieldRead} - Read field from TOML-backed Dynamic</li>
 *   <li>{@link #xmlFieldRead} - Read field from XML-backed Dynamic</li>
 * </ul>
 *
 * <h3>Field Modification</h3>
 * <p>Measure field write/set operations:</p>
 * <ul>
 *   <li>{@link #tomlFieldSet} - Set field on TOML-backed Dynamic</li>
 *   <li>{@link #xmlFieldSet} - Set field on XML-backed Dynamic</li>
 * </ul>
 *
 * <h3>Migration</h3>
 * <p>Measure DataFixer migration performance:</p>
 * <ul>
 *   <li>{@link #tomlMigration} - Apply fix to TOML-backed data</li>
 *   <li>{@link #xmlMigration} - Apply fix to XML-backed data</li>
 * </ul>
 *
 * <h2>Implementations</h2>
 * <table border="1">
 *   <tr><th>Implementation</th><th>Library</th><th>Node Type</th><th>Use Case</th></tr>
 *   <tr>
 *     <td>{@link JacksonTomlOps}</td>
 *     <td>Jackson Dataformat TOML</td>
 *     <td>{@code JsonNode}</td>
 *     <td>Configuration files, Rust ecosystem integration</td>
 *   </tr>
 *   <tr>
 *     <td>{@link JacksonXmlOps}</td>
 *     <td>Jackson Dataformat XML</td>
 *     <td>{@code JsonNode}</td>
 *     <td>Legacy systems, SOAP/REST APIs, document formats</td>
 *   </tr>
 * </table>
 *
 * <h2>Parameters</h2>
 * <table border="1">
 *   <tr><th>Parameter</th><th>Values</th><th>Description</th></tr>
 *   <tr><td>payloadSize</td><td>SMALL, MEDIUM</td><td>Test data complexity (LARGE excluded for performance)</td></tr>
 * </table>
 *
 * <p><b>Note:</b> The LARGE payload size is excluded from this benchmark because
 * TOML and XML serialization typically have higher overhead than JSON/YAML,
 * making large payloads impractical for typical use cases.</p>
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
 * # Run all TOML/XML benchmarks
 * java -jar benchmarks.jar TomlXmlBenchmark
 *
 * # Run TOML-only benchmarks
 * java -jar benchmarks.jar "TomlXmlBenchmark.toml.*"
 *
 * # Run XML-only benchmarks
 * java -jar benchmarks.jar "TomlXmlBenchmark.xml.*"
 *
 * # Compare generation performance
 * java -jar benchmarks.jar "TomlXmlBenchmark.*Generate"
 * }</pre>
 *
 * @author Erik Pförtner
 * @see JsonBenchmark
 * @see YamlBenchmark
 * @see CrossFormatBenchmark
 * @see de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps
 * @see de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class TomlXmlBenchmark {

    /**
     * Field name used for read/write benchmarks.
     *
     * <p>References the first string field generated by {@link BenchmarkDataGenerator}.</p>
     */
    private static final String FIELD_NAME = "stringField0";

    /**
     * Payload size parameter controlling test data complexity.
     *
     * <p>Limited to SMALL and MEDIUM to avoid excessive benchmark runtime
     * with the higher overhead of TOML and XML processing.</p>
     */
    @Param({"SMALL", "MEDIUM"})
    private PayloadSize payloadSize;

    /**
     * Jackson TOML DynamicOps implementation.
     */
    private JacksonTomlOps tomlOps;

    /**
     * Jackson XML DynamicOps implementation.
     */
    private JacksonXmlOps xmlOps;

    /**
     * Pre-generated test data using TOML format.
     */
    private Dynamic<JsonNode> tomlData;

    /**
     * Pre-generated test data using XML format.
     */
    private Dynamic<JsonNode> xmlData;

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
     * <p>Both TOML and XML data are pre-generated to isolate benchmark
     * measurements from data creation overhead.</p>
     */
    @Setup(Level.Trial)
    public void setup() {
        this.tomlOps = JacksonTomlOps.INSTANCE;
        this.xmlOps = JacksonXmlOps.INSTANCE;

        this.tomlData = BenchmarkDataGenerator.generate(this.tomlOps, this.payloadSize);
        this.xmlData = BenchmarkDataGenerator.generate(this.xmlOps, this.payloadSize);

        this.fixer = BenchmarkBootstrap.createSingleFixFixer();
        this.fromVersion = new DataVersion(1);
        this.toVersion = new DataVersion(2);
    }

    /**
     * Benchmarks Dynamic object generation using JacksonTomlOps.
     *
     * <p>Measures the time to create a complete test data structure using
     * Jackson's TOML dataformat module.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void tomlGenerate(final Blackhole blackhole) {
        final Dynamic<JsonNode> data = BenchmarkDataGenerator.generate(this.tomlOps, this.payloadSize);
        blackhole.consume(data);
    }

    /**
     * Benchmarks Dynamic object generation using JacksonXmlOps.
     *
     * <p>Measures the time to create a complete test data structure using
     * Jackson's XML dataformat module.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void xmlGenerate(final Blackhole blackhole) {
        final Dynamic<JsonNode> data = BenchmarkDataGenerator.generate(this.xmlOps, this.payloadSize);
        blackhole.consume(data);
    }

    /**
     * Benchmarks field read access on TOML-backed Dynamic.
     *
     * <p>Measures the time to retrieve a single field from a pre-existing
     * TOML-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void tomlFieldRead(final Blackhole blackhole) {
        final Dynamic<JsonNode> field = this.tomlData.get(FIELD_NAME);
        blackhole.consume(field);
    }

    /**
     * Benchmarks field read access on XML-backed Dynamic.
     *
     * <p>Measures the time to retrieve a single field from a pre-existing
     * XML-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void xmlFieldRead(final Blackhole blackhole) {
        final Dynamic<JsonNode> field = this.xmlData.get(FIELD_NAME);
        blackhole.consume(field);
    }

    /**
     * Benchmarks field set operation on TOML-backed Dynamic.
     *
     * <p>Measures the time to add a new field to a TOML-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void tomlFieldSet(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.tomlData.set(
                "newField",
                this.tomlData.createString("newValue")
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks field set operation on XML-backed Dynamic.
     *
     * <p>Measures the time to add a new field to an XML-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void xmlFieldSet(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.xmlData.set(
                "newField",
                this.xmlData.createString("newValue")
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks DataFixer migration on TOML-backed data.
     *
     * <p>Measures the time to apply a single fix migration to TOML-based
     * Dynamic data.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void tomlMigration(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.fixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.tomlData,
                this.fromVersion,
                this.toVersion
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks DataFixer migration on XML-backed data.
     *
     * <p>Measures the time to apply a single fix migration to XML-based
     * Dynamic data.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void xmlMigration(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.fixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.xmlData,
                this.fromVersion,
                this.toVersion
        );
        blackhole.consume(result);
    }
}
