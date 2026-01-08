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

package de.splatgames.aether.datafixers.spring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive unit tests for {@link MigrationMetrics}.
 */
@DisplayName("MigrationMetrics")
class MigrationMetricsTest {

    private MeterRegistry registry;
    private MigrationMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MigrationMetrics(registry);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("rejects null registry")
        void rejectsNullRegistry() {
            assertThatThrownBy(() -> new MigrationMetrics(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("registry");
        }
    }

    @Nested
    @DisplayName("Success Recording")
    class SuccessRecording {

        @Test
        @DisplayName("records success counter")
        void recordsSuccessCounter() {
            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(50));

            Counter counter = registry.find("aether.datafixers.migrations.success")
                    .tag("domain", "game")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("increments success counter on multiple calls")
        void incrementsSuccessCounterOnMultipleCalls() {
            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(50));
            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(60));
            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(70));

            Counter counter = registry.find("aether.datafixers.migrations.success")
                    .tag("domain", "game")
                    .counter();

            assertThat(counter.count()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("records timer")
        void recordsTimer() {
            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(50));

            Timer timer = registry.find("aether.datafixers.migrations.duration")
                    .tag("domain", "game")
                    .timer();

            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(50);
        }

        @Test
        @DisplayName("records version span")
        void recordsVersionSpan() {
            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(50));

            DistributionSummary summary = registry.find("aether.datafixers.migrations.version.span")
                    .tag("domain", "game")
                    .summary();

            assertThat(summary).isNotNull();
            assertThat(summary.count()).isEqualTo(1);
            assertThat(summary.totalAmount()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("records absolute version span for downgrades")
        void recordsAbsoluteVersionSpanForDowngrades() {
            metrics.recordSuccess("game", 200, 100, Duration.ofMillis(50));

            DistributionSummary summary = registry.find("aether.datafixers.migrations.version.span")
                    .tag("domain", "game")
                    .summary();

            assertThat(summary.totalAmount()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("separates metrics by domain")
        void separatesMetricsByDomain() {
            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(50));
            metrics.recordSuccess("user", 100, 200, Duration.ofMillis(50));

            Counter gameCounter = registry.find("aether.datafixers.migrations.success")
                    .tag("domain", "game")
                    .counter();
            Counter userCounter = registry.find("aether.datafixers.migrations.success")
                    .tag("domain", "user")
                    .counter();

            assertThat(gameCounter.count()).isEqualTo(1.0);
            assertThat(userCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("rejects null domain")
        void rejectsNullDomain() {
            assertThatThrownBy(() ->
                    metrics.recordSuccess(null, 100, 200, Duration.ofMillis(50))
            )
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("domain");
        }

        @Test
        @DisplayName("rejects null duration")
        void rejectsNullDuration() {
            assertThatThrownBy(() ->
                    metrics.recordSuccess("game", 100, 200, null)
            )
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("duration");
        }
    }

    @Nested
    @DisplayName("Failure Recording")
    class FailureRecording {

        @Test
        @DisplayName("records failure counter with error type")
        void recordsFailureCounterWithErrorType() {
            metrics.recordFailure("game", 100, 200, Duration.ofMillis(50),
                    new RuntimeException("Test"));

            Counter counter = registry.find("aether.datafixers.migrations.failure")
                    .tag("domain", "game")
                    .tag("error_type", "RuntimeException")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("differentiates failure counters by error type")
        void differentiatesFailureCountersByErrorType() {
            metrics.recordFailure("game", 100, 200, Duration.ofMillis(50),
                    new RuntimeException("Test"));
            metrics.recordFailure("game", 100, 200, Duration.ofMillis(50),
                    new IllegalStateException("Test"));

            Counter runtimeCounter = registry.find("aether.datafixers.migrations.failure")
                    .tag("domain", "game")
                    .tag("error_type", "RuntimeException")
                    .counter();
            Counter illegalStateCounter = registry.find("aether.datafixers.migrations.failure")
                    .tag("domain", "game")
                    .tag("error_type", "IllegalStateException")
                    .counter();

            assertThat(runtimeCounter.count()).isEqualTo(1.0);
            assertThat(illegalStateCounter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("records timer on failure")
        void recordsTimerOnFailure() {
            metrics.recordFailure("game", 100, 200, Duration.ofMillis(50),
                    new RuntimeException("Test"));

            Timer timer = registry.find("aether.datafixers.migrations.duration")
                    .tag("domain", "game")
                    .timer();

            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("does not record version span on failure")
        void doesNotRecordVersionSpanOnFailure() {
            metrics.recordFailure("game", 100, 200, Duration.ofMillis(50),
                    new RuntimeException("Test"));

            // Version span should not exist or be zero
            DistributionSummary summary = registry.find("aether.datafixers.migrations.version.span")
                    .tag("domain", "game")
                    .summary();

            // Either null or count 0
            if (summary != null) {
                assertThat(summary.count()).isZero();
            }
        }

        @Test
        @DisplayName("rejects null domain")
        void rejectsNullDomain() {
            assertThatThrownBy(() ->
                    metrics.recordFailure(null, 100, 200, Duration.ofMillis(50),
                            new RuntimeException("Test"))
            )
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("domain");
        }

        @Test
        @DisplayName("rejects null duration")
        void rejectsNullDuration() {
            assertThatThrownBy(() ->
                    metrics.recordFailure("game", 100, 200, null,
                            new RuntimeException("Test"))
            )
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("duration");
        }

        @Test
        @DisplayName("rejects null error")
        void rejectsNullError() {
            assertThatThrownBy(() ->
                    metrics.recordFailure("game", 100, 200, Duration.ofMillis(50), null)
            )
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("error");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles zero duration")
        void handlesZeroDuration() {
            metrics.recordSuccess("game", 100, 200, Duration.ZERO);

            Timer timer = registry.find("aether.datafixers.migrations.duration")
                    .tag("domain", "game")
                    .timer();

            assertThat(timer.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("handles very long duration")
        void handlesVeryLongDuration() {
            metrics.recordSuccess("game", 100, 200, Duration.ofHours(24));

            Timer timer = registry.find("aether.datafixers.migrations.duration")
                    .tag("domain", "game")
                    .timer();

            assertThat(timer.totalTime(TimeUnit.HOURS)).isGreaterThanOrEqualTo(24);
        }

        @Test
        @DisplayName("handles zero version span")
        void handlesZeroVersionSpan() {
            metrics.recordSuccess("game", 100, 100, Duration.ofMillis(50));

            DistributionSummary summary = registry.find("aether.datafixers.migrations.version.span")
                    .tag("domain", "game")
                    .summary();

            assertThat(summary.totalAmount()).isZero();
        }

        @Test
        @DisplayName("handles very large version span")
        void handlesVeryLargeVersionSpan() {
            metrics.recordSuccess("game", 0, Integer.MAX_VALUE, Duration.ofMillis(50));

            DistributionSummary summary = registry.find("aether.datafixers.migrations.version.span")
                    .tag("domain", "game")
                    .summary();

            assertThat(summary.totalAmount()).isEqualTo((double) Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("handles empty domain name")
        void handlesEmptyDomainName() {
            metrics.recordSuccess("", 100, 200, Duration.ofMillis(50));

            Counter counter = registry.find("aether.datafixers.migrations.success")
                    .tag("domain", "")
                    .counter();

            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("handles special characters in domain name")
        void handlesSpecialCharactersInDomainName() {
            metrics.recordSuccess("game-v2.1", 100, 200, Duration.ofMillis(50));

            Counter counter = registry.find("aether.datafixers.migrations.success")
                    .tag("domain", "game-v2.1")
                    .counter();

            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("handles custom exception classes")
        void handlesCustomExceptionClasses() {
            class CustomMigrationException extends RuntimeException {
                CustomMigrationException(String msg) {
                    super(msg);
                }
            }

            metrics.recordFailure("game", 100, 200, Duration.ofMillis(50),
                    new CustomMigrationException("Test"));

            Counter counter = registry.find("aether.datafixers.migrations.failure")
                    .tag("error_type", "CustomMigrationException")
                    .counter();

            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("handles anonymous exception classes")
        void handlesAnonymousExceptionClasses() {
            Exception anonymousException = new RuntimeException("Test") {
            };

            metrics.recordFailure("game", 100, 200, Duration.ofMillis(50), anonymousException);

            // Anonymous classes have empty simple name, so it uses the base class
            Counter counter = registry.find("aether.datafixers.migrations.failure")
                    .tag("domain", "game")
                    .counter();

            assertThat(counter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @Timeout(10)
        @DisplayName("handles concurrent success recordings")
        void handlesConcurrentSuccessRecordings() throws Exception {
            int threadCount = 10;
            int recordsPerThread = 100;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < recordsPerThread; j++) {
                            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(1));
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            Counter counter = registry.find("aether.datafixers.migrations.success")
                    .tag("domain", "game")
                    .counter();

            assertThat(counter.count()).isEqualTo(threadCount * recordsPerThread);
        }

        @Test
        @Timeout(10)
        @DisplayName("handles concurrent failure recordings")
        void handlesConcurrentFailureRecordings() throws Exception {
            int threadCount = 10;
            int recordsPerThread = 100;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < recordsPerThread; j++) {
                            metrics.recordFailure("game", 100, 200, Duration.ofMillis(1),
                                    new RuntimeException("Test"));
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            Counter counter = registry.find("aether.datafixers.migrations.failure")
                    .tag("domain", "game")
                    .tag("error_type", "RuntimeException")
                    .counter();

            assertThat(counter.count()).isEqualTo(threadCount * recordsPerThread);
        }

        @Test
        @Timeout(10)
        @DisplayName("handles concurrent recordings for multiple domains")
        void handlesConcurrentRecordingsForMultipleDomains() throws Exception {
            int domainCount = 5;
            int recordsPerDomain = 100;
            CountDownLatch latch = new CountDownLatch(domainCount);
            ExecutorService executor = Executors.newFixedThreadPool(domainCount);

            for (int i = 0; i < domainCount; i++) {
                final String domain = "domain-" + i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < recordsPerDomain; j++) {
                            metrics.recordSuccess(domain, 100, 200, Duration.ofMillis(1));
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            for (int i = 0; i < domainCount; i++) {
                Counter counter = registry.find("aether.datafixers.migrations.success")
                        .tag("domain", "domain-" + i)
                        .counter();

                assertThat(counter.count()).isEqualTo(recordsPerDomain);
            }
        }
    }

    @Nested
    @DisplayName("Meter Registry Integration")
    class MeterRegistryIntegration {

        @Test
        @DisplayName("uses provided meter registry")
        void usesProvidedMeterRegistry() {
            SimpleMeterRegistry customRegistry = new SimpleMeterRegistry();
            MigrationMetrics customMetrics = new MigrationMetrics(customRegistry);

            customMetrics.recordSuccess("game", 100, 200, Duration.ofMillis(50));

            // Should be in custom registry
            assertThat(customRegistry.find("aether.datafixers.migrations.success").counter())
                    .isNotNull();

            // Should not be in original registry
            assertThat(registry.find("aether.datafixers.migrations.success")
                    .tag("domain", "game").counter())
                    .isNull();
        }

        @Test
        @DisplayName("reuses meters for same domain")
        void reuseMetersForSameDomain() {
            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(50));
            metrics.recordSuccess("game", 100, 200, Duration.ofMillis(60));

            // Should be only one counter meter
            long meterCount = registry.getMeters().stream()
                    .filter(m -> m.getId().getName().equals("aether.datafixers.migrations.success"))
                    .count();

            assertThat(meterCount).isEqualTo(1);
        }
    }
}
