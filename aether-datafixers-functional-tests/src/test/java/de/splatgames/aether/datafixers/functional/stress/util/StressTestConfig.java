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

import java.time.Duration;

/**
 * Configuration holder for stress test parameters.
 * Reads from system properties with sensible defaults.
 *
 * <p>Configurable via Maven system properties or command line:
 * <pre>
 * mvn verify -Pstress -Dstress.threads=200 -Dstress.duration.minutes=10
 * </pre>
 */
public final class StressTestConfig {

    /** Default number of concurrent threads. */
    public static final int DEFAULT_THREAD_COUNT = 100;

    /** Default number of operations per thread. */
    public static final int DEFAULT_OPERATIONS_PER_THREAD = 1000;

    /** Default test duration. */
    public static final Duration DEFAULT_DURATION = Duration.ofMinutes(5);

    private StressTestConfig() {
        // Utility class
    }

    /**
     * Returns the configured thread count.
     *
     * @return thread count from {@code stress.threads} system property,
     *         or {@link #DEFAULT_THREAD_COUNT} if not set
     */
    public static int threadCount() {
        return Integer.getInteger("stress.threads", DEFAULT_THREAD_COUNT);
    }

    /**
     * Returns the configured operations per thread.
     *
     * @return operations per thread from {@code stress.operations.per.thread}
     *         system property, or {@link #DEFAULT_OPERATIONS_PER_THREAD} if not set
     */
    public static int operationsPerThread() {
        return Integer.getInteger("stress.operations.per.thread", DEFAULT_OPERATIONS_PER_THREAD);
    }

    /**
     * Returns the configured test duration.
     *
     * @return duration from {@code stress.duration.minutes} system property,
     *         or {@link #DEFAULT_DURATION} if not set
     */
    public static Duration duration() {
        long minutes = Long.getLong("stress.duration.minutes", DEFAULT_DURATION.toMinutes());
        return Duration.ofMinutes(minutes);
    }

    /**
     * Returns the total number of operations (threads * operations per thread).
     *
     * @return total operation count
     */
    public static int totalOperations() {
        return threadCount() * operationsPerThread();
    }
}
