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
 * JMH benchmark for TOML and XML format performance.
 *
 * <p>Measures DynamicOps operations and migration performance for
 * Jackson TOML and XML implementations.</p>
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
public class TomlXmlBenchmark {

    @Param({"SMALL", "MEDIUM"})
    private PayloadSize payloadSize;

    private Dynamic<JsonNode> tomlData;
    private Dynamic<JsonNode> xmlData;
    private DataFixer fixer;
    private DataVersion fromVersion;
    private DataVersion toVersion;

    @Setup(Level.Trial)
    public void setup() {
        this.tomlData = BenchmarkDataGenerator.generate(JacksonTomlOps.INSTANCE, this.payloadSize);
        this.xmlData = BenchmarkDataGenerator.generate(JacksonXmlOps.INSTANCE, this.payloadSize);
        this.fixer = BenchmarkBootstrap.createSingleFixFixer();
        this.fromVersion = new DataVersion(1);
        this.toVersion = new DataVersion(2);
    }

    // ==================== Data Generation ====================

    /**
     * Benchmarks TOML data generation.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void tomlGenerate(final Blackhole blackhole) {
        final Dynamic<JsonNode> data = BenchmarkDataGenerator.generate(
                JacksonTomlOps.INSTANCE, this.payloadSize);
        blackhole.consume(data);
    }

    /**
     * Benchmarks XML data generation.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void xmlGenerate(final Blackhole blackhole) {
        final Dynamic<JsonNode> data = BenchmarkDataGenerator.generate(
                JacksonXmlOps.INSTANCE, this.payloadSize);
        blackhole.consume(data);
    }

    // ==================== Field Access ====================

    /**
     * Benchmarks TOML field read access.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void tomlFieldRead(final Blackhole blackhole) {
        final Dynamic<JsonNode> field = this.tomlData.get("stringField0");
        blackhole.consume(field);
    }

    /**
     * Benchmarks XML field read access.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void xmlFieldRead(final Blackhole blackhole) {
        final Dynamic<JsonNode> field = this.xmlData.get("stringField0");
        blackhole.consume(field);
    }

    // ==================== Field Modification ====================

    /**
     * Benchmarks TOML field set operation.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void tomlFieldSet(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.tomlData.set(
                "newField", this.tomlData.createString("newValue"));
        blackhole.consume(result);
    }

    /**
     * Benchmarks XML field set operation.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void xmlFieldSet(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.xmlData.set(
                "newField", this.xmlData.createString("newValue"));
        blackhole.consume(result);
    }

    // ==================== Migration ====================

    /**
     * Benchmarks migration with TOML DynamicOps.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void tomlMigration(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.fixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.tomlData,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }

    /**
     * Benchmarks migration with XML DynamicOps.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void xmlMigration(final Blackhole blackhole) {
        final Dynamic<JsonNode> result = this.fixer.update(
                BenchmarkBootstrap.BENCHMARK_TYPE,
                this.xmlData,
                this.fromVersion,
                this.toVersion);
        blackhole.consume(result);
    }
}
