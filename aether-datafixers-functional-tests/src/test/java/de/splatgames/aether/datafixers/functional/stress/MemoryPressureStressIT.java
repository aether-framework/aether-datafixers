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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests memory behavior under stress.
 *
 * <p>Verifies:
 * <ul>
 *   <li>No memory leaks in repeated migration cycles</li>
 *   <li>Stable memory usage under sustained load</li>
 *   <li>Graceful handling of GC pressure</li>
 * </ul>
 *
 * <p>Run with: {@code mvn verify -Pstress -Dgroups=stress}
 */
@DisplayName("Memory Pressure Stress Tests")
@Tag("stress")
@Tag("integration")
class MemoryPressureStressIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryPressureStressIT.class);
    private static final TypeReference MEMORY_TYPE = new TypeReference("memory_entity");
    private static final MemoryMXBean MEMORY_BEAN = ManagementFactory.getMemoryMXBean();

    @Nested
    @DisplayName("Memory Leak Detection")
    class MemoryLeakDetection {

        @Test
        @Timeout(value = 10, unit = TimeUnit.MINUTES)
        @DisplayName("no memory leaks in repeated migration cycles")
        void noMemoryLeaksInRepeatedMigrationCycles() throws Exception {
            int cycles = 10;
            int operationsPerCycle = 10_000;

            LOGGER.info("Starting memory leak detection: {} cycles, {} ops/cycle",
                    cycles, operationsPerCycle);

            List<Long> heapUsageAfterGc = new ArrayList<>();

            for (int cycle = 0; cycle < cycles; cycle++) {
                DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                        .addFix(MEMORY_TYPE, createSimpleFix("memory_fix_" + cycle, 1, 2))
                        .build();

                // Perform many migrations
                for (int i = 0; i < operationsPerCycle; i++) {
                    JsonObject inputObj = new JsonObject();
                    inputObj.addProperty("id", cycle * 1_000_000L + i);
                    inputObj.addProperty("data", "test_data_" + i);
                    Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                    fixer.update(
                            MEMORY_TYPE, input,
                            new DataVersion(1), new DataVersion(2)
                    );
                }

                // Force GC and measure heap
                forceGc();
                MemoryUsage heapUsage = MEMORY_BEAN.getHeapMemoryUsage();
                heapUsageAfterGc.add(heapUsage.getUsed());

                LOGGER.info("Cycle {}: Heap after GC = {} MB",
                        cycle + 1, heapUsage.getUsed() / (1024 * 1024));
            }

            // Analyze heap growth trend
            // First few cycles may show growth due to JIT compilation and class loading
            // After warm-up (cycles 3+), heap should be relatively stable
            List<Long> stablePhase = heapUsageAfterGc.subList(3, heapUsageAfterGc.size());

            long minHeap = Collections.min(stablePhase);
            long maxHeap = Collections.max(stablePhase);
            double growthRatio = (double) maxHeap / minHeap;

            LOGGER.info("Stable phase heap range: {} MB - {} MB (ratio: {})",
                    minHeap / (1024 * 1024),
                    maxHeap / (1024 * 1024),
                    String.format("%.2f", growthRatio));

            // Allow up to 50% heap variation during stable phase
            // This accounts for GC timing variations
            assertThat(growthRatio)
                    .as("Heap growth ratio should be stable (no unbounded leak)")
                    .isLessThan(1.5);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("fixer instances can be garbage collected")
        void fixerInstancesCanBeGarbageCollected() throws Exception {
            int fixerCount = 100;
            int opsPerFixer = 1000;

            LOGGER.info("Creating and discarding {} fixers", fixerCount);

            long heapBefore = getHeapUsedAfterGc();

            for (int f = 0; f < fixerCount; f++) {
                DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                        .addFix(MEMORY_TYPE, createSimpleFix("disposable_fix_" + f, 1, 2))
                        .build();

                for (int i = 0; i < opsPerFixer; i++) {
                    JsonObject inputObj = new JsonObject();
                    inputObj.addProperty("id", i);
                    Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                    fixer.update(
                            MEMORY_TYPE, input,
                            new DataVersion(1), new DataVersion(2)
                    );
                }
                // fixer goes out of scope and should be eligible for GC
            }

            long heapAfter = getHeapUsedAfterGc();

            LOGGER.info("Heap before: {} MB, after: {} MB",
                    heapBefore / (1024 * 1024),
                    heapAfter / (1024 * 1024));

            // Heap should not have grown significantly
            // Allow some growth for class metadata and JIT artifacts
            long heapGrowth = heapAfter - heapBefore;
            long maxAllowedGrowth = 50 * 1024 * 1024; // 50 MB

            assertThat(heapGrowth)
                    .as("Heap growth after disposing fixers")
                    .isLessThan(maxAllowedGrowth);
        }
    }

    @Nested
    @DisplayName("GC Pressure Handling")
    class GcPressureHandling {

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("handles GC pressure gracefully")
        void handlesGcPressureGracefully() throws Exception {
            int threadCount = 20;
            int opsPerThread = 10_000;

            LOGGER.info("Starting GC pressure test: {} threads, {} ops/thread",
                    threadCount, opsPerThread);

            AtomicLong totalOps = new AtomicLong(0);
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(MEMORY_TYPE, createLargeObjectFix("gc_pressure_fix", 1, 2))
                    .build();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < opsPerThread; i++) {
                            // Create large input to generate allocation pressure
                            JsonObject inputObj = new JsonObject();
                            inputObj.addProperty("id", threadId * 1_000_000L + i);
                            inputObj.addProperty("largeField", createLargeString(1024));
                            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                            Dynamic<JsonElement> result = fixer.update(
                                    MEMORY_TYPE, input,
                                    new DataVersion(1), new DataVersion(2)
                            );

                            assertThat(result.get("processed").asBoolean().result())
                                    .contains(true);

                            totalOps.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(5, TimeUnit.MINUTES);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(errors).isEmpty();
            assertThat(totalOps.get())
                    .isEqualTo((long) threadCount * opsPerThread);

            LOGGER.info("Completed {} operations under GC pressure", totalOps.get());
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.MINUTES)
        @DisplayName("maintains correctness during GC pauses")
        void maintainsCorrectnessDuringGcPauses() throws Exception {
            int iterations = 100;
            int opsPerIteration = 1000;

            LOGGER.info("Testing correctness across {} iterations with GC", iterations);

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(MEMORY_TYPE, createSimpleFix("gc_correctness_fix", 1, 2))
                    .build();

            for (int iter = 0; iter < iterations; iter++) {
                // Force GC at various points to trigger potential issues
                if (iter % 10 == 0) {
                    System.gc();
                }

                for (int i = 0; i < opsPerIteration; i++) {
                    long uniqueId = iter * 1_000_000L + i;

                    JsonObject inputObj = new JsonObject();
                    inputObj.addProperty("id", uniqueId);
                    inputObj.addProperty("checksum", uniqueId * 31);
                    Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

                    Dynamic<JsonElement> result = fixer.update(
                            MEMORY_TYPE, input,
                            new DataVersion(1), new DataVersion(2)
                    );

                    // Verify data integrity
                    Long resultId = result.get("id").asLong().result().orElse(null);
                    Long resultChecksum = result.get("checksum").asLong().result().orElse(null);

                    assertThat(resultId).isEqualTo(uniqueId);
                    assertThat(resultChecksum).isEqualTo(uniqueId * 31);
                }
            }

            LOGGER.info("Completed correctness test with {} total operations",
                    iterations * opsPerIteration);
        }
    }

    // Helper methods

    private void forceGc() {
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long getHeapUsedAfterGc() {
        forceGc();
        return MEMORY_BEAN.getHeapMemoryUsage().getUsed();
    }

    private String createLargeString(int size) {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        return sb.toString();
    }

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

    private DataFix<JsonElement> createLargeObjectFix(String name, int from, int to) {
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
                // Create some intermediate objects to increase allocation pressure
                String largeField = input.get("largeField").asString().result().orElse("");
                String processed = largeField.toUpperCase();
                return input
                        .set("processed", input.createBoolean(true))
                        .set("processedField", input.createString(processed.substring(0, Math.min(100, processed.length()))));
            }
        };
    }
}
