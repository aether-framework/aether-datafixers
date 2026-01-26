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
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
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
 * JMH benchmark for cross-format conversion performance.
 *
 * <p>Measures the overhead of converting data between different
 * DynamicOps implementations using {@link DynamicOps#convertTo}.</p>
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
public class CrossFormatBenchmark {

    @Param({"SMALL", "MEDIUM"})
    private PayloadSize payloadSize;

    private Dynamic<JsonElement> gsonData;
    private Dynamic<JsonNode> jacksonData;
    private Dynamic<Object> snakeYamlData;
    private Dynamic<JsonNode> jacksonYamlData;

    @Setup(Level.Trial)
    public void setup() {
        this.gsonData = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, this.payloadSize);
        this.jacksonData = BenchmarkDataGenerator.generate(JacksonJsonOps.INSTANCE, this.payloadSize);
        this.snakeYamlData = BenchmarkDataGenerator.generate(SnakeYamlOps.INSTANCE, this.payloadSize);
        this.jacksonYamlData = BenchmarkDataGenerator.generate(JacksonYamlOps.INSTANCE, this.payloadSize);
    }

    // ==================== Gson <-> Jackson JSON ====================

    /**
     * Benchmarks converting from Gson to Jackson JSON.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void gsonToJackson(final Blackhole blackhole) {
        final JsonNode result = JacksonJsonOps.INSTANCE.convertTo(
                GsonOps.INSTANCE, this.gsonData.value());
        blackhole.consume(result);
    }

    /**
     * Benchmarks converting from Jackson JSON to Gson.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonToGson(final Blackhole blackhole) {
        final JsonElement result = GsonOps.INSTANCE.convertTo(
                JacksonJsonOps.INSTANCE, this.jacksonData.value());
        blackhole.consume(result);
    }

    // ==================== Gson <-> SnakeYAML ====================

    /**
     * Benchmarks converting from Gson to SnakeYAML.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void gsonToSnakeYaml(final Blackhole blackhole) {
        final Object result = SnakeYamlOps.INSTANCE.convertTo(
                GsonOps.INSTANCE, this.gsonData.value());
        blackhole.consume(result);
    }

    /**
     * Benchmarks converting from SnakeYAML to Gson.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void snakeYamlToGson(final Blackhole blackhole) {
        final JsonElement result = GsonOps.INSTANCE.convertTo(
                SnakeYamlOps.INSTANCE, this.snakeYamlData.value());
        blackhole.consume(result);
    }

    // ==================== Jackson JSON <-> Jackson YAML ====================

    /**
     * Benchmarks converting from Jackson JSON to Jackson YAML.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonJsonToYaml(final Blackhole blackhole) {
        final JsonNode result = JacksonYamlOps.INSTANCE.convertTo(
                JacksonJsonOps.INSTANCE, this.jacksonData.value());
        blackhole.consume(result);
    }

    /**
     * Benchmarks converting from Jackson YAML to Jackson JSON.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonYamlToJson(final Blackhole blackhole) {
        final JsonNode result = JacksonJsonOps.INSTANCE.convertTo(
                JacksonYamlOps.INSTANCE, this.jacksonYamlData.value());
        blackhole.consume(result);
    }

    // ==================== SnakeYAML <-> Jackson YAML ====================

    /**
     * Benchmarks converting from SnakeYAML to Jackson YAML.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void snakeYamlToJacksonYaml(final Blackhole blackhole) {
        final JsonNode result = JacksonYamlOps.INSTANCE.convertTo(
                SnakeYamlOps.INSTANCE, this.snakeYamlData.value());
        blackhole.consume(result);
    }

    /**
     * Benchmarks converting from Jackson YAML to SnakeYAML.
     *
     * @param blackhole JMH blackhole to prevent dead code elimination
     */
    @Benchmark
    public void jacksonYamlToSnakeYaml(final Blackhole blackhole) {
        final Object result = SnakeYamlOps.INSTANCE.convertTo(
                JacksonYamlOps.INSTANCE, this.jacksonYamlData.value());
        blackhole.consume(result);
    }
}
