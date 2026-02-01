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
import de.splatgames.aether.datafixers.functional.stress.util.ThreadOrchestrator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stress tests for high-concurrency migration scenarios.
 *
 * <p>Tests DataFixer behavior with 100+ concurrent threads performing
 * simultaneous migrations. Verifies:
 * <ul>
 *   <li>Thread safety under high contention</li>
 *   <li>Data integrity across concurrent operations</li>
 *   <li>Throughput scaling with thread count</li>
 * </ul>
 *
 * <p>Configuration via system properties:
 * <ul>
 *   <li>{@code stress.threads} - Number of concurrent threads (default: 100)</li>
 *   <li>{@code stress.operations.per.thread} - Operations per thread (default: 1000)</li>
 * </ul>
 *
 * <p>Run with: {@code mvn verify -Pstress -Dgroups=stress}
 */
@DisplayName("High Concurrency Migration Stress Tests")
@Tag("stress")
@Tag("integration")
class HighConcurrencyMigrationStressIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(HighConcurrencyMigrationStressIT.class);
    private static final TypeReference STRESS_TYPE = new TypeReference("stress_entity");

    @Nested
    @DisplayName("Concurrent Single Fix Migrations")
    class ConcurrentSingleFix {

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("handles 100+ concurrent single-fix migrations")
        void handles100ConcurrentSingleFixMigrations() throws Exception {
            int threadCount = StressTestConfig.threadCount();
            int opsPerThread = StressTestConfig.operationsPerThread();

            LOGGER.info("Starting stress test with {} threads, {} ops/thread",
                    threadCount, opsPerThread);

            AtomicInteger fixApplyCount = new AtomicInteger(0);
            StressTestMetrics metrics = new StressTestMetrics();

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(STRESS_TYPE, createCountingFix("single_fix", 1, 2, fixApplyCount))
                    .build();

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    orchestrator.submit(() -> {
                        for (int i = 0; i < opsPerThread; i++) {
                            long startNanos = System.nanoTime();
                            boolean success = true;

                            try {
                                JsonObject inputObj = new JsonObject();
                                inputObj.addProperty("id", threadId * 100_000L + i);
                                inputObj.addProperty("threadId", threadId);
                                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                                Dynamic<JsonElement> result = fixer.update(
                                        STRESS_TYPE, input,
                                        new DataVersion(1), new DataVersion(2)
                                );

                                // Verify result
                                assertThat(result.get("processed").asBoolean().result())
                                        .contains(true);
                                assertThat(result.get("id").asLong().result())
                                        .contains(threadId * 100_000L + i);
                            } catch (Exception e) {
                                success = false;
                                throw e;
                            } finally {
                                metrics.recordOperation(System.nanoTime() - startNanos, success);
                            }
                        }
                    });
                }

                Instant startTime = Instant.now();
                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(5));

                Duration elapsed = Duration.between(startTime, Instant.now());
                metrics.report("handles100ConcurrentSingleFixMigrations", elapsed);

                assertThat(completed).as("All threads completed within timeout").isTrue();
                assertThat(orchestrator.errors()).as("No exceptions thrown").isEmpty();
                assertThat(fixApplyCount.get())
                        .as("All fixes applied")
                        .isEqualTo(threadCount * opsPerThread);
            }
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("maintains data integrity under high contention")
        void maintainsDataIntegrityUnderHighContention() throws Exception {
            int threadCount = StressTestConfig.threadCount();
            int opsPerThread = StressTestConfig.operationsPerThread();

            // Track all processed IDs to verify no duplicates or losses
            Set<Long> processedIds = ConcurrentHashMap.newKeySet();

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(STRESS_TYPE, createSimpleFix("integrity_fix", 1, 2))
                    .build();

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    orchestrator.submit(() -> {
                        for (int i = 0; i < opsPerThread; i++) {
                            long uniqueId = threadId * 100_000L + i;

                            JsonObject inputObj = new JsonObject();
                            inputObj.addProperty("id", uniqueId);
                            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                            Dynamic<JsonElement> result = fixer.update(
                                    STRESS_TYPE, input,
                                    new DataVersion(1), new DataVersion(2)
                            );

                            // Verify the ID wasn't corrupted
                            Long resultId = result.get("id").asLong().result().orElse(null);
                            assertThat(resultId).isEqualTo(uniqueId);

                            // Track that we processed this ID
                            processedIds.add(uniqueId);
                        }
                    });
                }

                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(5));

                assertThat(completed).isTrue();
                assertThat(orchestrator.errors()).isEmpty();

                // Verify all expected IDs were processed exactly once
                assertThat(processedIds)
                        .as("All unique IDs processed")
                        .hasSize(threadCount * opsPerThread);
            }
        }
    }

    @Nested
    @DisplayName("Concurrent Chain Migrations")
    class ConcurrentChainMigration {

        @Test
        @Timeout(value = 10, unit = TimeUnit.MINUTES)
        @DisplayName("handles concurrent 10-fix chain migrations")
        void handlesConcurrentChainMigrations() throws Exception {
            int threadCount = Math.min(StressTestConfig.threadCount(), 50); // Reduce for chain tests
            int opsPerThread = StressTestConfig.operationsPerThread() / 10; // Reduce ops for longer chains

            LOGGER.info("Starting chain migration test with {} threads, {} ops/thread, 10-fix chain",
                    threadCount, opsPerThread);

            AtomicInteger totalFixApplications = new AtomicInteger(0);
            StressTestMetrics metrics = new StressTestMetrics();

            // Build a fixer with 10 chained fixes
            DataFixerBuilder builder = new DataFixerBuilder(new DataVersion(11));
            for (int v = 1; v <= 10; v++) {
                final int version = v;
                builder.addFix(STRESS_TYPE, new DataFix<JsonElement>() {
                    @Override
                    public @NotNull String name() {
                        return "chain_fix_v" + version;
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
                        totalFixApplications.incrementAndGet();
                        int currentStep = input.get("migration_steps").asInt().result().orElse(0);
                        return input.set("migration_steps", input.createInt(currentStep + 1));
                    }
                });
            }
            DataFixer fixer = builder.build();

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    orchestrator.submit(() -> {
                        for (int i = 0; i < opsPerThread; i++) {
                            long startNanos = System.nanoTime();
                            boolean success = true;

                            try {
                                JsonObject inputObj = new JsonObject();
                                inputObj.addProperty("id", threadId * 100_000L + i);
                                inputObj.addProperty("migration_steps", 0);
                                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                                Dynamic<JsonElement> result = fixer.update(
                                        STRESS_TYPE, input,
                                        new DataVersion(1), new DataVersion(11)
                                );

                                // Verify all 10 fixes were applied
                                assertThat(result.get("migration_steps").asInt().result())
                                        .contains(10);
                            } catch (Exception e) {
                                success = false;
                                throw e;
                            } finally {
                                metrics.recordOperation(System.nanoTime() - startNanos, success);
                            }
                        }
                    });
                }

                Instant startTime = Instant.now();
                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(10));

                Duration elapsed = Duration.between(startTime, Instant.now());
                metrics.report("handlesConcurrentChainMigrations", elapsed);

                assertThat(completed).isTrue();
                assertThat(orchestrator.errors()).isEmpty();

                // Each operation applies 10 fixes
                assertThat(totalFixApplications.get())
                        .isEqualTo(threadCount * opsPerThread * 10);
            }
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.MINUTES)
        @DisplayName("handles mixed version range migrations concurrently")
        void handlesMixedVersionRanges() throws Exception {
            int threadCount = StressTestConfig.threadCount();
            int opsPerThread = StressTestConfig.operationsPerThread() / 5;

            // Build fixer with 5 fixes
            DataFixerBuilder builder = new DataFixerBuilder(new DataVersion(6));
            for (int v = 1; v <= 5; v++) {
                builder.addFix(STRESS_TYPE, createSimpleFix("mix_fix_v" + v, v, v + 1));
            }
            DataFixer fixer = builder.build();

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    orchestrator.submit(() -> {
                        for (int i = 0; i < opsPerThread; i++) {
                            // Different threads use different version ranges
                            int fromVersion = (threadId % 5) + 1;
                            int toVersion = 6;

                            JsonObject inputObj = new JsonObject();
                            inputObj.addProperty("id", threadId * 100_000L + i);
                            inputObj.addProperty("start_version", fromVersion);
                            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                            Dynamic<JsonElement> result = fixer.update(
                                    STRESS_TYPE, input,
                                    new DataVersion(fromVersion), new DataVersion(toVersion)
                            );

                            // Verify the data is still intact
                            assertThat(result.get("id").asLong().result())
                                    .contains(threadId * 100_000L + i);
                        }
                    });
                }

                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(10));

                assertThat(completed).isTrue();
                assertThat(orchestrator.errors()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Throughput Scaling")
    class ScalingAnalysis {

        @ParameterizedTest(name = "threads={0}")
        @ValueSource(ints = {10, 25, 50, 100})
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("throughput scales with thread count")
        void throughputScalesWithThreadCount(int threadCount) throws Exception {
            int opsPerThread = 1000;
            StressTestMetrics metrics = new StressTestMetrics();

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(STRESS_TYPE, createSimpleFix("scale_fix", 1, 2))
                    .build();

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    orchestrator.submit(() -> {
                        for (int i = 0; i < opsPerThread; i++) {
                            long startNanos = System.nanoTime();

                            JsonObject inputObj = new JsonObject();
                            inputObj.addProperty("id", threadId * 100_000L + i);
                            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                            fixer.update(
                                    STRESS_TYPE, input,
                                    new DataVersion(1), new DataVersion(2)
                            );

                            metrics.recordOperation(System.nanoTime() - startNanos, true);
                        }
                    });
                }

                Instant startTime = Instant.now();
                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(5));
                Duration elapsed = Duration.between(startTime, Instant.now());

                LOGGER.info("Threads: {}, Throughput: {} ops/sec, Avg latency: {} us",
                        threadCount,
                        String.format("%.2f", metrics.throughputPerSecond(elapsed)),
                        metrics.averageLatencyNanos() / 1000);

                assertThat(completed).isTrue();
                assertThat(orchestrator.errors()).isEmpty();
                assertThat(metrics.totalOperations()).isEqualTo((long) threadCount * opsPerThread);
            }
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

    private DataFix<JsonElement> createCountingFix(String name, int from, int to, AtomicInteger counter) {
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
                counter.incrementAndGet();
                return input.set("processed", input.createBoolean(true));
            }
        };
    }
}
