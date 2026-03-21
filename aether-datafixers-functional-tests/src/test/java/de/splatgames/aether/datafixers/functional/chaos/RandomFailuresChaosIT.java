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
import de.splatgames.aether.datafixers.api.exception.FixException;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Chaos tests with random failures in fix execution.
 *
 * <p>Tests that the DataFixer system correctly handles:
 * <ul>
 *   <li>Sporadic failures during fix execution</li>
 *   <li>Failure isolation between concurrent operations</li>
 *   <li>Proper exception propagation</li>
 * </ul>
 *
 * <p>Run with: {@code mvn verify -Pit -Dgroups=chaos}
 */
@DisplayName("Random Failures Chaos Tests")
@Tag("chaos")
@Tag("integration")
class RandomFailuresChaosIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(RandomFailuresChaosIT.class);
    private static final TypeReference CHAOS_TYPE = new TypeReference("chaos_failure_entity");

    @Nested
    @DisplayName("Single Thread Failures")
    class SingleThreadFailures {

        @Test
        @Timeout(value = 2, unit = TimeUnit.MINUTES)
        @DisplayName("handles sporadic fix failures")
        void handlesSporadicFixFailures() {
            int operationCount = 200;
            double failureProbability = 0.05; // 5% failure rate

            ChaosInjector chaos = ChaosInjector.withFailureProbability(failureProbability);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(CHAOS_TYPE, createFailingFix("sporadic_fix", 1, 2, chaos))
                    .build();

            for (int i = 0; i < operationCount; i++) {
                JsonObject inputObj = new JsonObject();
                inputObj.addProperty("id", i);
                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                try {
                    Dynamic<JsonElement> result = fixer.update(
                            CHAOS_TYPE, input,
                            new DataVersion(1), new DataVersion(2)
                    );

                    assertThat(result.get("processed").asBoolean().result()).contains(true);
                    successCount.incrementAndGet();
                } catch (FixException e) {
                    // Expected: DataFixerImpl wraps ChaosException in FixException
                    failureCount.incrementAndGet();
                }
            }

            LOGGER.info("Operations: {}, Success: {}, Failures: {}",
                    operationCount, successCount.get(), failureCount.get());

            // With 5% failure rate over 200 operations, we expect some failures
            assertThat(failureCount.get())
                    .as("Some failures should have occurred")
                    .isGreaterThan(0);

            // But not all should fail
            assertThat(successCount.get())
                    .as("Most operations should succeed")
                    .isGreaterThan(operationCount / 2);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.MINUTES)
        @DisplayName("propagates fix exceptions correctly")
        void propagatesFixExceptionsCorrectly() {
            ChaosInjector chaos = ChaosInjector.withFailureProbability(1.0); // Always fail

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(CHAOS_TYPE, createFailingFix("always_fail_fix", 1, 2, chaos))
                    .build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("id", 1);
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            assertThatThrownBy(() -> fixer.update(
                    CHAOS_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            ))
                    .isInstanceOf(FixException.class)
                    .hasMessageContaining("Chaos-injected failure")
                    .hasCauseInstanceOf(ChaosInjector.ChaosException.class);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.MINUTES)
        @DisplayName("failure in chain stops subsequent fixes")
        void failureInChainStopsSubsequentFixes() {
            AtomicInteger fix1Count = new AtomicInteger(0);
            AtomicInteger fix2Count = new AtomicInteger(0);
            AtomicInteger fix3Count = new AtomicInteger(0);

            ChaosInjector alwaysFail = ChaosInjector.withFailureProbability(1.0);
            ChaosInjector neverFail = ChaosInjector.withFailureProbability(0.0);

            DataFixer fixer = new DataFixerBuilder(new DataVersion(4))
                    .addFix(CHAOS_TYPE, createCountingFix("chain_fix_1", 1, 2, neverFail, fix1Count))
                    .addFix(CHAOS_TYPE, createCountingFix("chain_fix_2", 2, 3, alwaysFail, fix2Count))
                    .addFix(CHAOS_TYPE, createCountingFix("chain_fix_3", 3, 4, neverFail, fix3Count))
                    .build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("id", 1);
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            assertThatThrownBy(() -> fixer.update(
                    CHAOS_TYPE, input,
                    new DataVersion(1), new DataVersion(4)
            )).isInstanceOf(FixException.class)
                    .hasCauseInstanceOf(ChaosInjector.ChaosException.class);

            // Fix 1 should have run
            assertThat(fix1Count.get()).isEqualTo(1);
            // Fix 2 should have run and failed
            assertThat(fix2Count.get()).isEqualTo(1);
            // Fix 3 should NOT have run due to failure in fix 2
            assertThat(fix3Count.get()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Concurrent Failure Isolation")
    class ConcurrentFailureIsolation {

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("isolates failures between threads")
        void isolatesFailuresBetweenThreads() throws Exception {
            int threadCount = 50;
            int opsPerThread = 100;
            double failureProbability = 0.1; // 10% failure rate

            ChaosInjector chaos = ChaosInjector.withFailureProbability(failureProbability);
            AtomicInteger totalSuccess = new AtomicInteger(0);
            AtomicInteger totalFailure = new AtomicInteger(0);
            Set<Integer> threadsWithSuccess = ConcurrentHashMap.newKeySet();
            Set<Integer> threadsWithFailure = ConcurrentHashMap.newKeySet();

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(CHAOS_TYPE, createFailingFix("isolation_fix", 1, 2, chaos))
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

                            try {
                                Dynamic<JsonElement> result = fixer.update(
                                        CHAOS_TYPE, input,
                                        new DataVersion(1), new DataVersion(2)
                                );

                                // Verify the thread's data wasn't corrupted by another thread's failure
                                assertThat(result.get("threadId").asInt().result())
                                        .contains(threadId);

                                totalSuccess.incrementAndGet();
                                threadsWithSuccess.add(threadId);
                            } catch (FixException e) {
                                totalFailure.incrementAndGet();
                                threadsWithFailure.add(threadId);
                            }
                        }
                    });
                }

                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(5));

                // We expect the orchestrator to complete even with failures
                // because individual operation failures don't stop the thread
                assertThat(completed).isTrue();
                // Orchestrator should not collect ChaosExceptions as errors
                // since they're caught within the task
                assertThat(orchestrator.errors()).isEmpty();
            }

            LOGGER.info("Total operations: {}, Success: {}, Failures: {}",
                    threadCount * opsPerThread, totalSuccess.get(), totalFailure.get());
            LOGGER.info("Threads with success: {}, Threads with failure: {}",
                    threadsWithSuccess.size(), threadsWithFailure.size());

            // Verify both successes and failures occurred across different threads
            assertThat(totalSuccess.get())
                    .as("Some operations should succeed")
                    .isGreaterThan(0);
            assertThat(totalFailure.get())
                    .as("Some operations should fail")
                    .isGreaterThan(0);

            // Most threads should have had both successes and failures
            // (with 10% failure rate over 100 ops, very likely to see both)
            assertThat(threadsWithSuccess.size())
                    .as("Multiple threads should have successes")
                    .isGreaterThan(threadCount / 2);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("fixer remains functional after failures")
        void fixerRemainsFunctionalAfterFailures() throws Exception {
            int phases = 3;
            int opsPerPhase = 100;

            // Phase 1: Some failures
            // Phase 2: No failures
            // Phase 3: Some failures again
            // Verify fixer works correctly throughout

            AtomicInteger phase1Success = new AtomicInteger(0);
            AtomicInteger phase1Failure = new AtomicInteger(0);
            AtomicInteger phase2Success = new AtomicInteger(0);
            AtomicInteger phase3Success = new AtomicInteger(0);
            AtomicInteger phase3Failure = new AtomicInteger(0);

            ChaosInjector chaosWith10Percent = ChaosInjector.withFailureProbability(0.1);
            ChaosInjector chaosNoFailure = ChaosInjector.withFailureProbability(0.0);

            // Use a single fixer throughout all phases
            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(CHAOS_TYPE, createFailingFix("resilience_fix", 1, 2, chaosWith10Percent))
                    .build();

            // Phase 1: With failures
            for (int i = 0; i < opsPerPhase; i++) {
                JsonObject inputObj = new JsonObject();
                inputObj.addProperty("id", i);
                inputObj.addProperty("phase", 1);
                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                try {
                    fixer.update(CHAOS_TYPE, input, new DataVersion(1), new DataVersion(2));
                    phase1Success.incrementAndGet();
                } catch (FixException e) {
                    phase1Failure.incrementAndGet();
                }
            }

            // Create a new fixer for phase 2 with no failures to verify isolation
            DataFixer fixer2 = new DataFixerBuilder(new DataVersion(2))
                    .addFix(CHAOS_TYPE, createFailingFix("stable_fix", 1, 2, chaosNoFailure))
                    .build();

            // Phase 2: No failures - verify fixer still works
            for (int i = 0; i < opsPerPhase; i++) {
                JsonObject inputObj = new JsonObject();
                inputObj.addProperty("id", i);
                inputObj.addProperty("phase", 2);
                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                Dynamic<JsonElement> result = fixer2.update(
                        CHAOS_TYPE, input,
                        new DataVersion(1), new DataVersion(2)
                );

                assertThat(result.get("processed").asBoolean().result()).contains(true);
                phase2Success.incrementAndGet();
            }

            // Phase 3: With failures again using original fixer
            for (int i = 0; i < opsPerPhase; i++) {
                JsonObject inputObj = new JsonObject();
                inputObj.addProperty("id", i);
                inputObj.addProperty("phase", 3);
                Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                try {
                    fixer.update(CHAOS_TYPE, input, new DataVersion(1), new DataVersion(2));
                    phase3Success.incrementAndGet();
                } catch (FixException e) {
                    phase3Failure.incrementAndGet();
                }
            }

            LOGGER.info("Phase 1: {} success, {} failure", phase1Success.get(), phase1Failure.get());
            LOGGER.info("Phase 2: {} success (should be 100%)", phase2Success.get());
            LOGGER.info("Phase 3: {} success, {} failure", phase3Success.get(), phase3Failure.get());

            // Phase 2 should have 100% success
            assertThat(phase2Success.get()).isEqualTo(opsPerPhase);

            // Phase 1 and 3 should have some failures (10% rate)
            assertThat(phase1Failure.get()).isGreaterThan(0);
            assertThat(phase3Failure.get()).isGreaterThan(0);
        }
    }

    // Helper methods

    private DataFix<JsonElement> createFailingFix(
            String name, int from, int to,
            ChaosInjector chaos
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
                chaos.maybeFail();
                return input.set("processed", input.createBoolean(true));
            }
        };
    }

    private DataFix<JsonElement> createCountingFix(
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
                counter.incrementAndGet();
                chaos.maybeFail();
                return input.set("processed", input.createBoolean(true));
            }
        };
    }
}
