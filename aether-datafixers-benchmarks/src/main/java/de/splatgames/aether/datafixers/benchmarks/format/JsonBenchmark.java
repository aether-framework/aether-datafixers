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
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkBootstrap;
import de.splatgames.aether.datafixers.benchmarks.util.BenchmarkDataGenerator;
import de.splatgames.aether.datafixers.benchmarks.util.PayloadSize;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
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
import org.jetbrains.annotations.Nullable;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing JSON DynamicOps implementations: Gson vs Jackson.
 *
 * <p>This benchmark measures the performance of JSON-based operations using two
 * different underlying libraries: Google Gson ({@link GsonOps}) and Jackson Databind
 * ({@link JacksonJsonOps}). The results help determine which implementation is more
 * suitable for specific use cases.</p>
 *
 * <h2>Benchmark Categories</h2>
 *
 * <h3>Data Generation</h3>
 * <p>Measure Dynamic object construction performance:</p>
 * <ul>
 *   <li>{@link #gsonGenerate} - Create Dynamic using GsonOps</li>
 *   <li>{@link #jacksonGenerate} - Create Dynamic using JacksonJsonOps</li>
 * </ul>
 *
 * <h3>Field Access</h3>
 * <p>Measure field read operations on existing data:</p>
 * <ul>
 *   <li>{@link #gsonFieldRead} - Read field from Gson-backed Dynamic</li>
 *   <li>{@link #jacksonFieldRead} - Read field from Jackson-backed Dynamic</li>
 * </ul>
 *
 * <h3>Field Modification</h3>
 * <p>Measure field write/set operations:</p>
 * <ul>
 *   <li>{@link #gsonFieldSet} - Set field on Gson-backed Dynamic</li>
 *   <li>{@link #jacksonFieldSet} - Set field on Jackson-backed Dynamic</li>
 * </ul>
 *
 * <h3>Migration</h3>
 * <p>Measure DataFixer migration performance:</p>
 * <ul>
 *   <li>{@link #gsonMigration} - Apply fix to Gson-backed data</li>
 *   <li>{@link #jacksonMigration} - Apply fix to Jackson-backed data</li>
 *   <li>{@link #crossFormatMigrationJacksonInput} - Cross-format migration scenario</li>
 * </ul>
 *
 * <h2>Implementations Compared</h2>
 * <table border="1">
 *   <caption>JSON DynamicOps implementations compared by this benchmark</caption>
 *   <tr><th>Implementation</th><th>Library</th><th>Node Type</th><th>Characteristics</th></tr>
 *   <tr>
 *     <td>{@link GsonOps}</td>
 *     <td>Google Gson</td>
 *     <td>{@code JsonElement}</td>
 *     <td>Simple API, smaller footprint, widely used</td>
 *   </tr>
 *   <tr>
 *     <td>{@link JacksonJsonOps}</td>
 *     <td>Jackson Databind</td>
 *     <td>{@code JsonNode}</td>
 *     <td>Feature-rich, streaming support, high performance</td>
 *   </tr>
 * </table>
 *
 * <h2>Parameters</h2>
 * <table border="1">
 *   <caption>Benchmark parameters and their tested values</caption>
 *   <tr><th>Parameter</th><th>Values</th><th>Description</th></tr>
 *   <tr><td>payloadSize</td><td>SMALL, MEDIUM, LARGE</td><td>Test data complexity</td></tr>
 * </table>
 *
 * <h2>Benchmark Configuration</h2>
 * <table border="1">
 *   <caption>JMH configuration settings for this benchmark</caption>
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
 * # Run all JSON benchmarks
 * java -jar benchmarks.jar JsonBenchmark
 *
 * # Compare only field access performance
 * java -jar benchmarks.jar "JsonBenchmark.*FieldRead"
 *
 * # Run Gson-only benchmarks
 * java -jar benchmarks.jar "JsonBenchmark.gson.*"
 *
 * # Run with specific payload size
 * java -jar benchmarks.jar JsonBenchmark -p payloadSize=LARGE
 * }</pre>
 *
 * @author Erik Pförtner
 * @see YamlBenchmark
 * @see TomlXmlBenchmark
 * @see CrossFormatBenchmark
 * @see de.splatgames.aether.datafixers.codec.json.gson.GsonOps
 * @see de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps
 * @since 1.0.0
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class JsonBenchmark {

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
     * Google Gson DynamicOps implementation.
     */
    private GsonOps gsonOps;

    /**
     * Jackson Databind DynamicOps implementation.
     */
    private JacksonJsonOps jacksonOps;

    /**
     * Pre-generated test data using Gson.
     */
    private Dynamic<JsonElement> gsonData;

    /**
     * Pre-generated test data using Jackson.
     */
    private Dynamic<JsonNode> jacksonData;

    /**
     * DataFixer for Gson-based migrations.
     */
    private DataFixer gsonFixer;

    /**
     * Optional DataFixer for Jackson-based migrations.
     *
     * <p>May be {@code null} if no dedicated Jackson fixer is configured.
     * In that case, cross-format migration behavior is measured instead.</p>
     */
    @Nullable
    private DataFixer jacksonFixer;

    /**
     * Source version for migrations (v1).
     */
    private DataVersion fromVersion;

    /**
     * Target version for migrations (v2).
     */
    private DataVersion toVersion;

    /**
     * Initializes DynamicOps instances, test data, and DataFixers.
     *
     * <p>Both Gson and Jackson data are pre-generated to isolate benchmark
     * measurements from data creation overhead (except for generation benchmarks).</p>
     */
    @Setup(Level.Trial)
    public void setup() {
        this.gsonOps = GsonOps.INSTANCE;
        this.jacksonOps = JacksonJsonOps.INSTANCE;

        this.gsonData = BenchmarkDataGenerator.generate(this.gsonOps, this.payloadSize);
        this.jacksonData = BenchmarkDataGenerator.generate(this.jacksonOps, this.payloadSize);

        this.gsonFixer = BenchmarkBootstrap.createSingleFixFixer();

        // If you have a dedicated Jackson fixer, wire it here. Otherwise keep it null and measure cross-format explicitly.
        // Example (if you add it later): this.jacksonFixer = BenchmarkBootstrap.createSingleFixFixerJackson();
        this.jacksonFixer = null;

        this.fromVersion = new DataVersion(1);
        this.toVersion = new DataVersion(2);
    }

    // ==================== Data Generation Benchmarks ====================

    /**
     * Benchmarks Dynamic object generation using GsonOps.
     *
     * <p>Measures the time to create a complete test data structure using
     * Gson as the underlying JSON representation.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void gsonGenerate(final Blackhole blackhole) {
        final Dynamic<JsonElement> data = BenchmarkDataGenerator.generate(this.gsonOps, this.payloadSize);
        blackhole.consume(data);
    }

    /**
     * Benchmarks Dynamic object generation using JacksonJsonOps.
     *
     * <p>Measures the time to create a complete test data structure using
     * Jackson as the underlying JSON representation.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonGenerate(final Blackhole blackhole) {
        final Dynamic<JsonNode> data = BenchmarkDataGenerator.generate(this.jacksonOps, this.payloadSize);
        blackhole.consume(data);
    }

    // ==================== Field Access Benchmarks ====================

    /**
     * Benchmarks field read access on Gson-backed Dynamic.
     *
     * <p>Measures the time to retrieve a single field from a pre-existing
     * Gson-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void gsonFieldRead(final Blackhole blackhole) {
        final Dynamic<JsonElement> field = this.gsonData.get(FIELD_NAME);
        blackhole.consume(field);
    }

    /**
     * Benchmarks field read access on Jackson-backed Dynamic.
     *
     * <p>Measures the time to retrieve a single field from a pre-existing
     * Jackson-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonFieldRead(final Blackhole blackhole) {
        final Dynamic<JsonNode> field = this.jacksonData.get(FIELD_NAME);
        blackhole.consume(field);
    }

    // ==================== Field Modification Benchmarks ====================

    /**
     * Benchmarks field set operation on Gson-backed Dynamic.
     *
     * <p>Measures the time to add a new field to a Gson-based Dynamic object.
     * This operation typically creates a new Dynamic with the modified content.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void gsonFieldSet(final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.gsonData.set(
                "newField",
                this.gsonData.createString("newValue")
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks field set operation on Jackson-backed Dynamic.
     *
     * <p>Measures the time to add a new field to a Jackson-based Dynamic object.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonFieldSet(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.jacksonData.set(
                "newField",
                this.jacksonData.createString("newValue")
        );
        blackhole.consume(result);
    }

    // ==================== Migration Benchmarks ====================

    /**
     * Benchmarks DataFixer migration on Gson-backed data.
     *
     * <p>Measures the time to apply a single fix migration to Gson-based
     * Dynamic data. This represents the typical migration scenario where
     * both fixer and data use the same DynamicOps implementation.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void gsonMigration(final Blackhole blackhole) {
        final Dynamic<JsonElement> result = this.gsonFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.gsonData,
                this.fromVersion,
                this.toVersion
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks DataFixer migration on Jackson-backed data.
     *
     * <p>If a dedicated Jackson fixer is available, measures native Jackson
     * migration. Otherwise, falls back to cross-format migration using the
     * Gson-based fixer with Jackson input data.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonMigration(final Blackhole blackhole) {
        if (this.jacksonFixer == null) {
            // No dedicated Jackson fixer available -> this would not be a fair "Jackson migration" benchmark.
            // Measure the cross-format behavior explicitly instead.
            final Dynamic<JsonNode> result = this.gsonFixer.update(
                    BenchmarkBootstrap.BENCHMARK_TYPE,
                    this.jacksonData,
                    this.fromVersion,
                    this.toVersion
            );
            blackhole.consume(result);
            return;
        }

        final Dynamic<JsonNode> result = this.jacksonFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.jacksonData,
                this.fromVersion,
                this.toVersion
        );
        blackhole.consume(result);
    }

    /**
     * Benchmarks cross-format migration with Jackson input and Gson-based fixer.
     *
     * <p>Measures the performance overhead when the fixer's DynamicOps differs
     * from the input data's DynamicOps. This scenario is common when migrating
     * data from various sources through a centralized fixer.</p>
     *
     * <p>Comparing this benchmark with {@link #gsonMigration} reveals the
     * overhead of format conversion during migration.</p>
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void crossFormatMigrationJacksonInput(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.gsonFixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.jacksonData,
                this.fromVersion,
                this.toVersion
        );
        blackhole.consume(result);
    }
}
