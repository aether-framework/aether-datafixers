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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A DataFix that can be configured to fail randomly for chaos testing.
 *
 * <p>This fix wraps the chaos injection logic and can be used to test
 * error handling in the data fixer pipeline.
 *
 * @param <T> the element type
 */
public final class FailingDataFix<T> implements DataFix<T> {

    private final String name;
    private final DataVersion fromVersion;
    private final DataVersion toVersion;
    private final ChaosInjector chaosInjector;
    private final AtomicInteger applyCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);

    private FailingDataFix(Builder<T> builder) {
        this.name = builder.name;
        this.fromVersion = builder.fromVersion;
        this.toVersion = builder.toVersion;
        this.chaosInjector = builder.chaosInjector;
    }

    /**
     * Creates a builder for a FailingDataFix.
     *
     * @param <T> the element type
     * @return a new builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public @NotNull DataVersion fromVersion() {
        return fromVersion;
    }

    @Override
    public @NotNull DataVersion toVersion() {
        return toVersion;
    }

    @Override
    public @NotNull Dynamic<T> apply(
            @NotNull TypeReference type,
            @NotNull Dynamic<T> input,
            @NotNull DataFixerContext context
    ) {
        applyCount.incrementAndGet();

        // Inject delay if configured
        chaosInjector.maybeDelay();

        // Inject failure if configured
        if (chaosInjector.shouldFail()) {
            failCount.incrementAndGet();
            throw new ChaosInjector.ChaosException(
                    "Chaos failure in fix '" + name + "' at version " + fromVersion + " -> " + toVersion
            );
        }

        // Mark as processed and return
        return input.set("chaos_processed", input.createBoolean(true));
    }

    /**
     * Returns the number of times this fix has been applied.
     *
     * @return apply count
     */
    public int applyCount() {
        return applyCount.get();
    }

    /**
     * Returns the number of times this fix has triggered a failure.
     *
     * @return fail count
     */
    public int failCount() {
        return failCount.get();
    }

    /**
     * Resets the apply and fail counters.
     */
    public void resetCounters() {
        applyCount.set(0);
        failCount.set(0);
    }

    /**
     * Builder for FailingDataFix.
     *
     * @param <T> the element type
     */
    public static final class Builder<T> {
        private String name = "chaos_fix";
        private DataVersion fromVersion = new DataVersion(1);
        private DataVersion toVersion = new DataVersion(2);
        private ChaosInjector chaosInjector = ChaosInjector.builder().build();

        private Builder() {
        }

        /**
         * Sets the fix name.
         *
         * @param name the fix name
         * @return this builder
         */
        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the version range.
         *
         * @param from the source version
         * @param to   the target version
         * @return this builder
         */
        public Builder<T> versions(int from, int to) {
            this.fromVersion = new DataVersion(from);
            this.toVersion = new DataVersion(to);
            return this;
        }

        /**
         * Sets the chaos injector to use.
         *
         * @param chaosInjector the chaos injector
         * @return this builder
         */
        public Builder<T> chaosInjector(ChaosInjector chaosInjector) {
            this.chaosInjector = chaosInjector;
            return this;
        }

        /**
         * Configures the fix to fail with the given probability.
         *
         * @param probability failure probability (0.0 to 1.0)
         * @return this builder
         */
        public Builder<T> failureProbability(double probability) {
            this.chaosInjector = ChaosInjector.withFailureProbability(probability);
            return this;
        }

        /**
         * Configures the fix to introduce random delays.
         *
         * @param minMs minimum delay in milliseconds
         * @param maxMs maximum delay in milliseconds
         * @return this builder
         */
        public Builder<T> randomDelay(int minMs, int maxMs) {
            this.chaosInjector = ChaosInjector.builder()
                    .delayRange(minMs, maxMs)
                    .failureProbability(
                            this.chaosInjector != null ? 0 : 0 // Keep existing probability if set
                    )
                    .build();
            return this;
        }

        /**
         * Builds the FailingDataFix.
         *
         * @return a new FailingDataFix
         */
        public FailingDataFix<T> build() {
            return new FailingDataFix<>(this);
        }
    }
}
