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

package de.splatgames.aether.datafixers.functional.chaos;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.functional.chaos.util.ChaosInjector;
import de.splatgames.aether.datafixers.functional.stress.util.ThreadOrchestrator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chaos tests introducing random delays into fix execution.
 *
 * <p>Tests that the DataFixer system correctly handles:
 * <ul>
 *   <li>Variable latency in fix execution</li>
 *   <li>Ordering correctness despite delays</li>
 *   <li>Concurrent execution with mixed delay patterns</li>
 * </ul>
 *
 * <p>Run with: {@code mvn verify -Pit -Dgroups=chaos}
 */
@DisplayName("Random Delays Chaos Tests")
@Tag("chaos")
@Tag("integration")
class RandomDelaysChaosIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomDelaysChaosIT.class);
    private static final TypeReference CHAOS_TYPE = new TypeReference("chaos_delay_entity");

    @Nested
    @DisplayName("Single Thread Delays")
    class SingleThreadDelays {

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("completes correctly with random delays")
        void completesCorrectlyWithRandomDelays() {
            int operationCount = 100;
            ChaosInjector chaos = ChaosInjector.builder()
                    .delayRange(1, 50)
                    .build();

            AtomicInteger fixApplyCount = new AtomicInteger(0);

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(CHAOS_TYPE, createDelayingFix("delay_fix", 1, 2, chaos, fixApplyCount))
                    .build();

            for (int i = 0; i < operationCount; i++) {
                JsonObject inputObj = new JsonObject();
                inputObj.addProperty("id", i);
                inputObj.addProperty("sequence", i);
                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                Dynamic<JsonElement> result = fixer.update(
                        CHAOS_TYPE, input,
                        new DataVersion(1), new DataVersion(2)
                );

                // Verify result correctness despite delay
                assertThat(result.get("processed").asBoolean().result()).contains(true);
                assertThat(result.get("id").asInt().result()).contains(i);
            }

            assertThat(fixApplyCount.get()).isEqualTo(operationCount);
            LOGGER.info("Completed {} operations with {} delays injected",
                    operationCount, chaos.delayCount());
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("handles extreme delay variations in fix chain")
        void handlesExtremeDelayVariationsInChain() {
            int chainLength = 5;
            int operationCount = 50;

            DataFixerBuilder builder = new DataFixerBuilder(new DataVersion(chainLength + 1));

            // Each fix in the chain has different delay characteristics
            for (int v = 1; v <= chainLength; v++) {
                final int version = v;
                // Vary delay range: some instant, some with significant delay
                ChaosInjector chaos = (v % 2 == 0)
                        ? ChaosInjector.builder().delayRange(0, 100).build()
                        : ChaosInjector.builder().build(); // No delay

                builder.addFix(CHAOS_TYPE, new DataFix<JsonElement>() {
                    @Override
                    public @NotNull String name() {
                        return "chain_delay_fix_v" + version;
                    }

                    @Override
                    public @NotNull DataVersion fromVersion() {
                        return new DataVersion(version);
                    }

                    @Override
                    public @NotNull DataVersion toVersion() {
                        return new DataVersion(version + 1);
                    }

                    @Override
                    public @NotNull Dynamic<JsonElement> apply(
                            @NotNull TypeReference type,
                            @NotNull Dynamic<JsonElement> input,
                            @NotNull DataFixerContext context
                    ) {
                        chaos.maybeDelay();
                        int step = input.get("step").asInt().result().orElse(0);
                        return input.set("step", input.createInt(step + 1));
                    }
                });
            }

            DataFixer fixer = builder.build();

            for (int i = 0; i < operationCount; i++) {
                JsonObject inputObj = new JsonObject();
                inputObj.addProperty("id", i);
                inputObj.addProperty("step", 0);
                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                Dynamic<JsonElement> result = fixer.update(
                        CHAOS_TYPE, input,
                        new DataVersion(1), new DataVersion(chainLength + 1)
                );

                // Verify all fixes in chain were applied correctly
                assertThat(result.get("step").asInt().result())
                        .as("All chain fixes applied for operation " + i)
                        .contains(chainLength);
            }

            LOGGER.info("Completed {} chain operations with varying delays", operationCount);
        }
    }

    @Nested
    @DisplayName("Concurrent Delays")
    class ConcurrentDelays {

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("handles random delays in concurrent execution")
        void handlesRandomDelaysInConcurrentExecution() throws Exception {
            int threadCount = 50;
            int opsPerThread = 100;

            ChaosInjector chaos = ChaosInjector.builder()
                    .delayRange(1, 20)
                    .build();

            AtomicInteger totalOps = new AtomicInteger(0);

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(CHAOS_TYPE, createDelayingFix("concurrent_delay_fix", 1, 2, chaos, totalOps))
                    .build();

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    orchestrator.submit(() -> {
                        for (int i = 0; i < opsPerThread; i++) {
                            JsonObject inputObj = new JsonObject();
                            inputObj.addProperty("id", threadId * 10_000L + i);
                            inputObj.addProperty("threadId", threadId);
                            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                            Dynamic<JsonElement> result = fixer.update(
                                    CHAOS_TYPE, input,
                                    new DataVersion(1), new DataVersion(2)
                            );

                            // Verify data integrity
                            assertThat(result.get("processed").asBoolean().result())
                                    .contains(true);
                            assertThat(result.get("threadId").asInt().result())
                                    .contains(threadId);
                        }
                    });
                }

                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(5));

                assertThat(completed).isTrue();
                assertThat(orchestrator.errors()).isEmpty();
                assertThat(totalOps.get()).isEqualTo(threadCount * opsPerThread);
            }

            LOGGER.info("Completed {} concurrent operations with {} delays",
                    totalOps.get(), chaos.delayCount());
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("maintains ordering within each thread despite delays")
        void maintainsOrderingWithinEachThread() throws Exception {
            int threadCount = 20;
            int opsPerThread = 50;

            ChaosInjector chaos = ChaosInjector.builder()
                    .delayRange(1, 30)
                    .build();

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(CHAOS_TYPE, new DataFix<JsonElement>() {
                        @Override
                        public @NotNull String name() {
                            return "ordering_delay_fix";
                        }

                        @Override
                        public @NotNull DataVersion fromVersion() {
                            return new DataVersion(1);
                        }

                        @Override
                        public @NotNull DataVersion toVersion() {
                            return new DataVersion(2);
                        }

                        @Override
                        public @NotNull Dynamic<JsonElement> apply(
                                @NotNull TypeReference type,
                                @NotNull Dynamic<JsonElement> input,
                                @NotNull DataFixerContext context
                        ) {
                            chaos.maybeDelay();
                            long timestamp = System.nanoTime();
                            return input
                                    .set("processed", input.createBoolean(true))
                                    .set("processedAt", input.createLong(timestamp));
                        }
                    })
                    .build();

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    orchestrator.submit(() -> {
                        long lastTimestamp = 0;
                        for (int i = 0; i < opsPerThread; i++) {
                            JsonObject inputObj = new JsonObject();
                            inputObj.addProperty("id", threadId * 10_000L + i);
                            inputObj.addProperty("sequence", i);
                            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                            Dynamic<JsonElement> result = fixer.update(
                                    CHAOS_TYPE, input,
                                    new DataVersion(1), new DataVersion(2)
                            );

                            long currentTimestamp = result.get("processedAt").asLong().result().orElse(0L);

                            // Within a single thread, processing should be sequential
                            assertThat(currentTimestamp)
                                    .as("Ordering within thread " + threadId)
                                    .isGreaterThanOrEqualTo(lastTimestamp);

                            lastTimestamp = currentTimestamp;
                        }
                    });
                }

                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(5));

                assertThat(completed).isTrue();
                assertThat(orchestrator.errors()).isEmpty();
            }

            LOGGER.info("Verified ordering across {} threads with delays", threadCount);
        }
    }

    // Helper methods

    private DataFix<JsonElement> createDelayingFix(
            String name, int from, int to,
            ChaosInjector chaos, AtomicInteger counter
    ) {
        return new DataFix<>() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull DataVersion fromVersion() {
                return new DataVersion(from);
            }

            @Override
            public @NotNull DataVersion toVersion() {
                return new DataVersion(to);
            }

            @Override
            public @NotNull Dynamic<JsonElement> apply(
                    @NotNull TypeReference type,
                    @NotNull Dynamic<JsonElement> input,
                    @NotNull DataFixerContext context
            ) {
                chaos.maybeDelay();
                counter.incrementAndGet();
                return input.set("processed", input.createBoolean(true));
            }
        };
    }
}
