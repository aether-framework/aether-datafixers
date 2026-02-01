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

package de.splatgames.aether.datafixers.functional.chaos.util;

import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Injects chaos into test execution for chaos engineering tests.
 *
 * <p>Provides controlled randomization for:
 * <ul>
 *   <li>Random delays to simulate variable latency</li>
 *   <li>Random failures to test error handling</li>
 * </ul>
 *
 * <p>Uses {@link SplittableRandom} for high-performance, thread-local randomness
 * without atomic contention.
 *
 * <p>Example usage:
 * <pre>{@code
 * ChaosInjector chaos = ChaosInjector.builder()
 *     .failureProbability(0.05)  // 5% failure rate
 *     .delayRange(1, 50)         // 1-50ms random delays
 *     .build();
 *
 * // In test code:
 * chaos.maybeDelay();
 * chaos.maybeFail("Simulated failure");
 * }</pre>
 */
public final class ChaosInjector {

    private final double failureProbability;
    private final int minDelayMs;
    private final int maxDelayMs;
    private final ThreadLocal<SplittableRandom> random;
    private final AtomicLong delayCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();

    private ChaosInjector(Builder builder) {
        this.failureProbability = builder.failureProbability;
        this.minDelayMs = builder.minDelayMs;
        this.maxDelayMs = builder.maxDelayMs;
        this.random = ThreadLocal.withInitial(SplittableRandom::new);
    }

    /**
     * Creates a builder for configuring a ChaosInjector.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a ChaosInjector with only failure probability configured.
     *
     * @param probability the probability of failure (0.0 to 1.0)
     * @return a new ChaosInjector
     */
    public static ChaosInjector withFailureProbability(double probability) {
        return builder().failureProbability(probability).build();
    }

    /**
     * Creates a ChaosInjector with only random delays configured.
     *
     * @param minMs minimum delay in milliseconds
     * @param maxMs maximum delay in milliseconds
     * @return a new ChaosInjector
     */
    public static ChaosInjector withRandomDelay(int minMs, int maxMs) {
        return builder().delayRange(minMs, maxMs).build();
    }

    /**
     * Introduces a random delay based on configured parameters.
     *
     * <p>Does nothing if no delay range is configured.
     */
    public void maybeDelay() {
        if (maxDelayMs <= 0) {
            return;
        }

        int delay = minDelayMs + random.get().nextInt(maxDelayMs - minDelayMs + 1);
        if (delay > 0) {
            try {
                Thread.sleep(delay);
                delayCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Throws an exception based on configured failure probability.
     *
     * @param message the exception message if failure is triggered
     * @throws ChaosException if random chance triggers failure
     */
    public void maybeFail(String message) {
        if (failureProbability <= 0) {
            return;
        }

        if (random.get().nextDouble() < failureProbability) {
            failureCount.incrementAndGet();
            throw new ChaosException(message);
        }
    }

    /**
     * Throws an exception based on configured failure probability.
     *
     * <p>Uses a default message.
     *
     * @throws ChaosException if random chance triggers failure
     */
    public void maybeFail() {
        maybeFail("Chaos-injected failure");
    }

    /**
     * Returns true based on configured failure probability.
     *
     * <p>Useful for conditional logic instead of exceptions.
     *
     * @return true if failure should be simulated
     */
    public boolean shouldFail() {
        if (failureProbability <= 0) {
            return false;
        }
        return random.get().nextDouble() < failureProbability;
    }

    /**
     * Returns the total number of delays that were injected.
     *
     * @return delay count
     */
    public long delayCount() {
        return delayCount.get();
    }

    /**
     * Returns the total number of failures that were injected.
     *
     * @return failure count
     */
    public long failureCount() {
        return failureCount.get();
    }

    /**
     * Resets the delay and failure counters.
     */
    public void resetCounters() {
        delayCount.set(0);
        failureCount.set(0);
    }

    /**
     * Builder for ChaosInjector.
     */
    public static final class Builder {
        private double failureProbability = 0.0;
        private int minDelayMs = 0;
        private int maxDelayMs = 0;

        private Builder() {
        }

        /**
         * Sets the failure probability.
         *
         * @param probability value between 0.0 (never fail) and 1.0 (always fail)
         * @return this builder
         */
        public Builder failureProbability(double probability) {
            if (probability < 0.0 || probability > 1.0) {
                throw new IllegalArgumentException(
                        "Probability must be between 0.0 and 1.0: " + probability
                );
            }
            this.failureProbability = probability;
            return this;
        }

        /**
         * Sets the random delay range.
         *
         * @param minMs minimum delay in milliseconds (inclusive)
         * @param maxMs maximum delay in milliseconds (inclusive)
         * @return this builder
         */
        public Builder delayRange(int minMs, int maxMs) {
            if (minMs < 0 || maxMs < minMs) {
                throw new IllegalArgumentException(
                        "Invalid delay range: " + minMs + " - " + maxMs
                );
            }
            this.minDelayMs = minMs;
            this.maxDelayMs = maxMs;
            return this;
        }

        /**
         * Builds the ChaosInjector.
         *
         * @return a new ChaosInjector
         */
        public ChaosInjector build() {
            return new ChaosInjector(this);
        }
    }

    /**
     * Exception thrown by chaos injection.
     */
    public static final class ChaosException extends RuntimeException {
        public ChaosException(String message) {
            super(message);
        }
    }
}
