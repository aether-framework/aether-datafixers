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

package de.splatgames.aether.datafixers.functional.stress.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects and reports metrics during stress tests.
 *
 * <p>Thread-safe metrics collector that tracks:
 * <ul>
 *   <li>Total operations (success + failure)</li>
 *   <li>Failed operations</li>
 *   <li>Latency statistics (sum, min, max)</li>
 * </ul>
 */
public final class StressTestMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(StressTestMetrics.class);

    private final LongAdder totalOperations = new LongAdder();
    private final LongAdder failedOperations = new LongAdder();
    private final LongAdder totalLatencyNanos = new LongAdder();
    private final AtomicLong minLatencyNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatencyNanos = new AtomicLong(0);

    /**
     * Records a completed operation.
     *
     * @param latencyNanos the operation latency in nanoseconds
     * @param success      true if the operation succeeded, false otherwise
     */
    public void recordOperation(long latencyNanos, boolean success) {
        totalOperations.increment();
        totalLatencyNanos.add(latencyNanos);

        if (!success) {
            failedOperations.increment();
        }

        // Update min/max atomically
        updateMin(latencyNanos);
        updateMax(latencyNanos);
    }

    private void updateMin(long latencyNanos) {
        long current;
        do {
            current = minLatencyNanos.get();
            if (latencyNanos >= current) {
                return;
            }
        } while (!minLatencyNanos.compareAndSet(current, latencyNanos));
    }

    private void updateMax(long latencyNanos) {
        long current;
        do {
            current = maxLatencyNanos.get();
            if (latencyNanos <= current) {
                return;
            }
        } while (!maxLatencyNanos.compareAndSet(current, latencyNanos));
    }

    /**
     * Returns the total number of operations.
     *
     * @return total operation count
     */
    public long totalOperations() {
        return totalOperations.sum();
    }

    /**
     * Returns the number of successful operations.
     *
     * @return successful operation count
     */
    public long successfulOperations() {
        return totalOperations.sum() - failedOperations.sum();
    }

    /**
     * Returns the number of failed operations.
     *
     * @return failed operation count
     */
    public long failedOperations() {
        return failedOperations.sum();
    }

    /**
     * Calculates the throughput in operations per second.
     *
     * @param elapsed the elapsed time
     * @return throughput in ops/sec
     */
    public double throughputPerSecond(Duration elapsed) {
        if (elapsed.isZero()) {
            return 0.0;
        }
        return (double) totalOperations.sum() / elapsed.toSeconds();
    }

    /**
     * Returns the average latency in nanoseconds.
     *
     * @return average latency in nanoseconds, or 0 if no operations recorded
     */
    public long averageLatencyNanos() {
        long total = totalOperations.sum();
        if (total == 0) {
            return 0;
        }
        return totalLatencyNanos.sum() / total;
    }

    /**
     * Returns the minimum recorded latency in nanoseconds.
     *
     * @return minimum latency, or 0 if no operations recorded
     */
    public long minLatencyNanos() {
        long min = minLatencyNanos.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    /**
     * Returns the maximum recorded latency in nanoseconds.
     *
     * @return maximum latency
     */
    public long maxLatencyNanos() {
        return maxLatencyNanos.get();
    }

    /**
     * Logs a summary of the collected metrics.
     *
     * @param testName the name of the test for logging context
     * @param elapsed  the total elapsed time
     */
    public void report(String testName, Duration elapsed) {
        LOGGER.info("=== Stress Test Report: {} ===", testName);
        LOGGER.info("Duration: {} ms", elapsed.toMillis());
        LOGGER.info("Total operations: {}", totalOperations());
        LOGGER.info("Successful: {}", successfulOperations());
        LOGGER.info("Failed: {}", failedOperations());
        LOGGER.info("Throughput: {} ops/sec", String.format("%.2f", throughputPerSecond(elapsed)));
        LOGGER.info("Latency (avg): {} us", averageLatencyNanos() / 1000);
        LOGGER.info("Latency (min): {} us", minLatencyNanos() / 1000);
        LOGGER.info("Latency (max): {} us", maxLatencyNanos() / 1000);
    }

    /**
     * Resets all metrics to their initial state.
     */
    public void reset() {
        totalOperations.reset();
        failedOperations.reset();
        totalLatencyNanos.reset();
        minLatencyNanos.set(Long.MAX_VALUE);
        maxLatencyNanos.set(0);
    }
}
