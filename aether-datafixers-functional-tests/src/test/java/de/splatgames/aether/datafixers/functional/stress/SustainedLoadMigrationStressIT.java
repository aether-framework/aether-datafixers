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

package de.splatgames.aether.datafixers.functional.stress;

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
import de.splatgames.aether.datafixers.functional.stress.util.StressTestConfig;
import de.splatgames.aether.datafixers.functional.stress.util.StressTestMetrics;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests system behavior under sustained load for extended periods.
 *
 * <p>Verifies that the DataFixer system maintains:
 * <ul>
 *   <li>Consistent throughput over time</li>
 *   <li>Stable latency without degradation</li>
 *   <li>Correct behavior under burst traffic patterns</li>
 * </ul>
 *
 * <p>Configuration via system properties:
 * <ul>
 *   <li>{@code stress.duration.minutes} - Test duration (default: 5)</li>
 *   <li>{@code stress.threads} - Worker thread count (default: 100)</li>
 * </ul>
 *
 * <p>Run with: {@code mvn verify -Pstress -Dgroups=stress}
 */
@DisplayName("Sustained Load Migration Stress Tests")
@Tag("stress")
@Tag("integration")
class SustainedLoadMigrationStressIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SustainedLoadMigrationStressIT.class);
    private static final TypeReference STRESS_TYPE = new TypeReference("sustained_entity");

    @Nested
    @DisplayName("Continuous Load")
    class ContinuousLoad {

        @Test
        @Timeout(value = 15, unit = TimeUnit.MINUTES)
        @DisplayName("sustains high throughput for configured duration")
        void sustainsHighThroughputForConfiguredDuration() throws Exception {
            Duration testDuration = StressTestConfig.duration();
            int threadCount = Math.min(StressTestConfig.threadCount(), 50);

            LOGGER.info("Starting sustained load test: {} threads for {} minutes",
                    threadCount, testDuration.toMinutes());

            StressTestMetrics metrics = new StressTestMetrics();
            AtomicBoolean running = new AtomicBoolean(true);
            AtomicLong operationCount = new AtomicLong(0);

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(STRESS_TYPE, createSimpleFix("sustained_fix", 1, 2))
                    .build();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Future<?>> futures = new ArrayList<>();
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

            Instant startTime = Instant.now();
            Instant endTime = startTime.plus(testDuration);

            // Submit worker tasks
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                futures.add(executor.submit(() -> {
                    long localOps = 0;
                    while (running.get() && Instant.now().isBefore(endTime)) {
                        try {
                            long startNanos = System.nanoTime();

                            JsonObject inputObj = new JsonObject();
                            inputObj.addProperty("id", threadId * 1_000_000_000L + localOps);
                            inputObj.addProperty("timestamp", System.currentTimeMillis());
                            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                            Dynamic<JsonElement> result = fixer.update(
                                    STRESS_TYPE, input,
                                    new DataVersion(1), new DataVersion(2)
                            );

                            assertThat(result.get("processed").asBoolean().result())
                                    .contains(true);

                            metrics.recordOperation(System.nanoTime() - startNanos, true);
                            operationCount.incrementAndGet();
                            localOps++;
                        } catch (Exception e) {
                            errors.add(e);
                            metrics.recordOperation(0, false);
                        }
                    }
                }));
            }

            // Wait for duration to elapse
            Thread.sleep(testDuration.toMillis());
            running.set(false);

            // Wait for all threads to finish
            executor.shutdown();
            boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);

            Duration actualDuration = Duration.between(startTime, Instant.now());
            metrics.report("sustainsHighThroughputForConfiguredDuration", actualDuration);

            assertThat(terminated).as("All threads terminated").isTrue();
            assertThat(errors).as("No errors during sustained load").isEmpty();
            assertThat(metrics.totalOperations())
                    .as("Significant number of operations completed")
                    .isGreaterThan(threadCount * 100L);

            LOGGER.info("Completed {} operations in {} seconds ({} ops/sec)",
                    metrics.totalOperations(),
                    actualDuration.toSeconds(),
                    String.format("%.2f", metrics.throughputPerSecond(actualDuration)));
        }

        @Test
        @Timeout(value = 15, unit = TimeUnit.MINUTES)
        @DisplayName("maintains consistent latency under sustained load")
        void maintainsConsistentLatencyUnderLoad() throws Exception {
            Duration testDuration = Duration.ofMinutes(Math.min(StressTestConfig.duration().toMinutes(), 2));
            int threadCount = 20;
            int sampleInterval = 1000; // Sample every N operations

            LOGGER.info("Starting latency consistency test: {} threads for {} minutes",
                    threadCount, testDuration.toMinutes());

            AtomicBoolean running = new AtomicBoolean(true);
            List<Long> latencySamples = Collections.synchronizedList(new ArrayList<>());

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(STRESS_TYPE, createSimpleFix("latency_fix", 1, 2))
                    .build();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

            Instant endTime = Instant.now().plus(testDuration);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    long localOps = 0;
                    while (running.get() && Instant.now().isBefore(endTime)) {
                        try {
                            long startNanos = System.nanoTime();

                            JsonObject inputObj = new JsonObject();
                            inputObj.addProperty("id", threadId * 1_000_000_000L + localOps);
                            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                            fixer.update(
                                    STRESS_TYPE, input,
                                    new DataVersion(1), new DataVersion(2)
                            );

                            long latencyNanos = System.nanoTime() - startNanos;

                            // Sample periodically to avoid memory pressure from collecting all
                            if (localOps % sampleInterval == 0) {
                                latencySamples.add(latencyNanos);
                            }

                            localOps++;
                        } catch (Exception e) {
                            errors.add(e);
                        }
                    }
                });
            }

            Thread.sleep(testDuration.toMillis());
            running.set(false);

            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

            assertThat(errors).isEmpty();
            assertThat(latencySamples).isNotEmpty();

            // Analyze latency distribution
            List<Long> sortedSamples = new ArrayList<>(latencySamples);
            Collections.sort(sortedSamples);

            int size = sortedSamples.size();
            long p50 = sortedSamples.get(size / 2);
            long p95 = sortedSamples.get((int) (size * 0.95));
            long p99 = sortedSamples.get((int) (size * 0.99));
            long max = sortedSamples.get(size - 1);

            LOGGER.info("Latency distribution (us): p50={}, p95={}, p99={}, max={}",
                    p50 / 1000, p95 / 1000, p99 / 1000, max / 1000);

            // p99 should not be excessively higher than p50 (reasonable consistency)
            // Allow up to 100x variance for stress conditions
            assertThat(p99)
                    .as("p99 latency should be within reasonable bounds of p50")
                    .isLessThan(p50 * 100);
        }
    }

    @Nested
    @DisplayName("Burst Traffic Patterns")
    class BurstTraffic {

        @Test
        @Timeout(value = 10, unit = TimeUnit.MINUTES)
        @DisplayName("handles burst traffic patterns")
        void handlesBurstTrafficPatterns() throws Exception {
            int burstThreads = 50;
            int quietThreads = 5;
            int burstOps = 1000;
            int quietOps = 100;
            int cycles = 3;

            LOGGER.info("Starting burst traffic test: {} cycles of burst ({} threads) and quiet ({} threads)",
                    cycles, burstThreads, quietThreads);

            StressTestMetrics burstMetrics = new StressTestMetrics();
            StressTestMetrics quietMetrics = new StressTestMetrics();
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(STRESS_TYPE, createSimpleFix("burst_fix", 1, 2))
                    .build();

            for (int cycle = 0; cycle < cycles; cycle++) {
                LOGGER.info("Cycle {}/{}: Burst phase", cycle + 1, cycles);

                // Burst phase
                ExecutorService burstExecutor = Executors.newFixedThreadPool(burstThreads);
                CountDownLatch burstLatch = new CountDownLatch(burstThreads);

                for (int t = 0; t < burstThreads; t++) {
                    final int threadId = t;
                    final int currentCycle = cycle;
                    burstExecutor.submit(() -> {
                        try {
                            for (int i = 0; i < burstOps; i++) {
                                long startNanos = System.nanoTime();

                                JsonObject inputObj = new JsonObject();
                                inputObj.addProperty("id", currentCycle * 1_000_000L + threadId * 10_000L + i);
                                inputObj.addProperty("phase", "burst");
                                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                                fixer.update(
                                        STRESS_TYPE, input,
                                        new DataVersion(1), new DataVersion(2)
                                );

                                burstMetrics.recordOperation(System.nanoTime() - startNanos, true);
                            }
                        } catch (Exception e) {
                            errors.add(e);
                        } finally {
                            burstLatch.countDown();
                        }
                    });
                }

                burstLatch.await(2, TimeUnit.MINUTES);
                burstExecutor.shutdown();

                LOGGER.info("Cycle {}/{}: Quiet phase", cycle + 1, cycles);

                // Quiet phase
                ExecutorService quietExecutor = Executors.newFixedThreadPool(quietThreads);
                CountDownLatch quietLatch = new CountDownLatch(quietThreads);

                for (int t = 0; t < quietThreads; t++) {
                    final int threadId = t;
                    final int currentCycle = cycle;
                    quietExecutor.submit(() -> {
                        try {
                            for (int i = 0; i < quietOps; i++) {
                                long startNanos = System.nanoTime();

                                JsonObject inputObj = new JsonObject();
                                inputObj.addProperty("id", currentCycle * 1_000_000L + threadId * 10_000L + i);
                                inputObj.addProperty("phase", "quiet");
                                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                                fixer.update(
                                        STRESS_TYPE, input,
                                        new DataVersion(1), new DataVersion(2)
                                );

                                quietMetrics.recordOperation(System.nanoTime() - startNanos, true);
                            }
                        } catch (Exception e) {
                            errors.add(e);
                        } finally {
                            quietLatch.countDown();
                        }
                    });
                }

                quietLatch.await(1, TimeUnit.MINUTES);
                quietExecutor.shutdown();
            }

            assertThat(errors).isEmpty();

            long expectedBurstOps = (long) cycles * burstThreads * burstOps;
            long expectedQuietOps = (long) cycles * quietThreads * quietOps;

            assertThat(burstMetrics.totalOperations())
                    .as("All burst operations completed")
                    .isEqualTo(expectedBurstOps);

            assertThat(quietMetrics.totalOperations())
                    .as("All quiet operations completed")
                    .isEqualTo(expectedQuietOps);

            LOGGER.info("Burst phase: {} ops, avg latency {} us",
                    burstMetrics.totalOperations(),
                    burstMetrics.averageLatencyNanos() / 1000);
            LOGGER.info("Quiet phase: {} ops, avg latency {} us",
                    quietMetrics.totalOperations(),
                    quietMetrics.averageLatencyNanos() / 1000);
        }
    }

    // Helper methods

    private DataFix<JsonElement> createSimpleFix(String name, int from, int to) {
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
                return input.set("processed", input.createBoolean(true));
            }
        };
    }
}
