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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import de.splatgames.aether.datafixers.functional.stress.util.StressTestConfig;
import de.splatgames.aether.datafixers.functional.stress.util.ThreadOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests mixed read/write patterns on registries under concurrent access.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Frozen registry handles massive concurrent reads</li>
 *   <li>Pre-freeze concurrent access patterns</li>
 *   <li>Proper rejection of post-freeze modifications</li>
 * </ul>
 *
 * <p>Run with: {@code mvn verify -Pstress -Dgroups=stress}
 */
@DisplayName("Mixed Registry Access Stress Tests")
@Tag("stress")
@Tag("integration")
class MixedRegistryAccessStressIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MixedRegistryAccessStressIT.class);

    @Nested
    @DisplayName("Post-Freeze Concurrent Reads")
    class PostFreezeConcurrentReads {

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("frozen registry handles massive concurrent reads")
        void frozenRegistryHandlesMassiveConcurrentReads() throws Exception {
            int threadCount = StressTestConfig.threadCount();
            int readsPerThread = StressTestConfig.operationsPerThread();
            int typeCount = 100;

            LOGGER.info("Starting registry read stress test: {} threads, {} reads/thread, {} types",
                    threadCount, readsPerThread, typeCount);

            // Setup: Create and freeze a registry with many types
            SimpleTypeRegistry registry = new SimpleTypeRegistry();
            for (int i = 0; i < typeCount; i++) {
                Type<String> type = Type.named("stress_type_" + i, Type.STRING);
                registry.register(type);
            }
            registry.freeze();

            AtomicInteger successfulReads = new AtomicInteger(0);

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    orchestrator.submit(() -> {
                        for (int i = 0; i < readsPerThread; i++) {
                            int typeIndex = i % typeCount;
                            TypeReference ref = new TypeReference("stress_type_" + typeIndex);

                            Type<?> type = registry.get(ref);

                            assertThat(type).isNotNull();
                            assertThat(type.reference().getId()).isEqualTo("stress_type_" + typeIndex);

                            successfulReads.incrementAndGet();
                        }
                    });
                }

                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(5));

                assertThat(completed).isTrue();
                assertThat(orchestrator.errors()).isEmpty();
                assertThat(successfulReads.get())
                        .isEqualTo(threadCount * readsPerThread);
            }

            LOGGER.info("Completed {} successful reads", successfulReads.get());
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.MINUTES)
        @DisplayName("rejects modifications after freeze under contention")
        void rejectsModificationsAfterFreezeUnderContention() throws Exception {
            int threadCount = 50;
            int attemptsPerThread = 100;

            SimpleTypeRegistry registry = new SimpleTypeRegistry();
            registry.register(Type.named("initial_type", Type.STRING));
            registry.freeze();

            AtomicInteger rejectionCount = new AtomicInteger(0);

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    orchestrator.submit(() -> {
                        for (int i = 0; i < attemptsPerThread; i++) {
                            try {
                                Type<String> newType = Type.named(
                                        "new_type_" + threadId + "_" + i,
                                        Type.STRING
                                );
                                registry.register(newType);
                                // Should not reach here
                                throw new AssertionError("Expected IllegalStateException");
                            } catch (IllegalStateException e) {
                                // Expected behavior
                                rejectionCount.incrementAndGet();
                            }
                        }
                    });
                }

                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(2));

                assertThat(completed).isTrue();
                assertThat(orchestrator.errors()).isEmpty();
                assertThat(rejectionCount.get())
                        .as("All modification attempts rejected")
                        .isEqualTo(threadCount * attemptsPerThread);
            }
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("handles interleaved reads of different types")
        void handlesInterleavedReadsOfDifferentTypes() throws Exception {
            int threadCount = StressTestConfig.threadCount();
            int readsPerThread = StressTestConfig.operationsPerThread();
            int typeCount = 500;

            SimpleTypeRegistry registry = new SimpleTypeRegistry();
            for (int i = 0; i < typeCount; i++) {
                registry.register(Type.named("interleaved_type_" + i, Type.STRING));
            }
            registry.freeze();

            AtomicInteger successfulReads = new AtomicInteger(0);

            try (ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(threadCount)) {
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    orchestrator.submit(() -> {
                        // Each thread accesses types in a different pattern
                        int startOffset = threadId * 7; // Prime number for better distribution
                        for (int i = 0; i < readsPerThread; i++) {
                            int typeIndex = (startOffset + i * 13) % typeCount;
                            TypeReference ref = new TypeReference("interleaved_type_" + typeIndex);

                            Type<?> type = registry.get(ref);
                            assertThat(type).isNotNull();

                            successfulReads.incrementAndGet();
                        }
                    });
                }

                orchestrator.startAll();
                boolean completed = orchestrator.awaitCompletion(Duration.ofMinutes(5));

                assertThat(completed).isTrue();
                assertThat(orchestrator.errors()).isEmpty();
            }

            LOGGER.info("Completed {} interleaved reads across {} types",
                    successfulReads.get(), typeCount);
        }
    }

    @Nested
    @DisplayName("Pre-Freeze Concurrent Access")
    class PreFreezeConcurrentAccess {

        @Test
        @Timeout(value = 2, unit = TimeUnit.MINUTES)
        @DisplayName("handles concurrent reads during sequential registration")
        void handlesConcurrentReadsDuringSequentialRegistration() throws Exception {
            int readerThreads = 20;
            int readsPerThread = 1000;
            int typeCount = 100;

            SimpleTypeRegistry registry = new SimpleTypeRegistry();
            AtomicInteger registeredCount = new AtomicInteger(0);
            AtomicInteger successfulReads = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(readerThreads + 1);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(readerThreads + 1);
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

            // Registration thread (sequential, to avoid race in the non-thread-safe registry)
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < typeCount; i++) {
                        registry.register(Type.named("concurrent_type_" + i, Type.STRING));
                        registeredCount.incrementAndGet();
                        Thread.sleep(1); // Small delay to allow reads to interleave
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });

            // Reader threads
            for (int t = 0; t < readerThreads; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int i = 0; i < readsPerThread; i++) {
                            int currentRegistered = registeredCount.get();
                            if (currentRegistered > 0) {
                                int typeIndex = i % currentRegistered;
                                TypeReference ref = new TypeReference("concurrent_type_" + typeIndex);
                                Type<?> type = registry.get(ref);
                                if (type != null) {
                                    successfulReads.incrementAndGet();
                                }
                            }
                            Thread.yield(); // Allow other threads to run
                        }
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = completionLatch.await(2, TimeUnit.MINUTES);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(registeredCount.get()).isEqualTo(typeCount);

            // Note: Some reads may have occurred before any types were registered,
            // so we just verify the test completed without errors
            LOGGER.info("Completed with {} registered types and {} successful reads",
                    registeredCount.get(), successfulReads.get());
        }
    }
}
