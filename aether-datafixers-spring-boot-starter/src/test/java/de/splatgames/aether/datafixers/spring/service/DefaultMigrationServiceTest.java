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

package de.splatgames.aether.datafixers.spring.service;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry;
import de.splatgames.aether.datafixers.spring.metrics.MigrationMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit tests for {@link DefaultMigrationService}.
 */
@DisplayName("DefaultMigrationService")
class DefaultMigrationServiceTest {

    private DataFixerRegistry registry;
    private MigrationMetrics metrics;
    private DefaultMigrationService service;
    private AetherDataFixer mockFixer;
    private TaggedDynamic mockData;

    @BeforeEach
    void setUp() {
        registry = new DataFixerRegistry();
        metrics = mock(MigrationMetrics.class);
        mockFixer = mock(AetherDataFixer.class);
        mockData = mock(TaggedDynamic.class);

        when(mockFixer.currentVersion()).thenReturn(new DataVersion(200));
        registry.register(DataFixerRegistry.DEFAULT_DOMAIN, mockFixer);

        service = new DefaultMigrationService(registry, metrics);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("rejects null registry")
        void rejectsNullRegistry() {
            assertThatThrownBy(() -> new DefaultMigrationService(null, metrics))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("registry");
        }

        @Test
        @DisplayName("accepts null metrics")
        void acceptsNullMetrics() {
            DefaultMigrationService svc = new DefaultMigrationService(registry, null);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("rejects null executor")
        void rejectsNullExecutor() {
            assertThatThrownBy(() -> new DefaultMigrationService(registry, metrics, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("asyncExecutor");
        }

        @Test
        @DisplayName("accepts custom executor")
        void acceptsCustomExecutor() {
            Executor executor = Executors.newSingleThreadExecutor();
            DefaultMigrationService svc = new DefaultMigrationService(registry, metrics, executor);
            assertThat(svc).isNotNull();
        }
    }

    @Nested
    @DisplayName("Happy Path - Successful Migrations")
    class HappyPath {

        @Test
        @DisplayName("executes simple migration from version A to B")
        void executesSimpleMigration() {
            TaggedDynamic resultData = mock(TaggedDynamic.class);
            when(mockFixer.update(any(), any(), any())).thenReturn(resultData);

            MigrationResult result = service.migrate(mockData)
                    .from(new DataVersion(100))
                    .to(new DataVersion(200))
                    .execute();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isSameAs(resultData);
            assertThat(result.getFromVersion()).isEqualTo(new DataVersion(100));
            assertThat(result.getToVersion()).isEqualTo(new DataVersion(200));
            assertThat(result.getDomain()).isEqualTo(DataFixerRegistry.DEFAULT_DOMAIN);
        }

        @Test
        @DisplayName("executes migration to latest version")
        void executesToLatestVersion() {
            TaggedDynamic resultData = mock(TaggedDynamic.class);
            when(mockFixer.update(any(), any(), any())).thenReturn(resultData);

            MigrationResult result = service.migrate(mockData)
                    .from(new DataVersion(100))
                    .toLatest()
                    .execute();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getToVersion()).isEqualTo(new DataVersion(200));
        }

        @Test
        @DisplayName("executes migration for specific domain")
        void executesForSpecificDomain() {
            AetherDataFixer gameFixer = mock(AetherDataFixer.class);
            when(gameFixer.currentVersion()).thenReturn(new DataVersion(150));
            when(gameFixer.update(any(), any(), any())).thenReturn(mockData);
            registry.register("game", gameFixer);

            MigrationResult result = service.migrate(mockData)
                    .usingDomain("game")
                    .from(new DataVersion(100))
                    .to(new DataVersion(150))
                    .execute();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getDomain()).isEqualTo("game");
            verify(gameFixer).update(any(), eq(new DataVersion(100)), eq(new DataVersion(150)));
        }

        @Test
        @DisplayName("records success metrics")
        void recordsSuccessMetrics() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            service.migrate(mockData)
                    .from(new DataVersion(100))
                    .to(new DataVersion(200))
                    .execute();

            verify(metrics).recordSuccess(
                    eq(DataFixerRegistry.DEFAULT_DOMAIN),
                    eq(100),
                    eq(200),
                    any(Duration.class)
            );
        }

        @Test
        @DisplayName("returns duration in result")
        void returnsDurationInResult() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = service.migrate(mockData)
                    .from(new DataVersion(100))
                    .to(new DataVersion(200))
                    .execute();

            assertThat(result.getDuration()).isNotNull();
            assertThat(result.getDuration()).isGreaterThanOrEqualTo(Duration.ZERO);
        }

        @Test
        @DisplayName("works without metrics")
        void worksWithoutMetrics() {
            DefaultMigrationService noMetricsService = new DefaultMigrationService(registry, null);
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = noMetricsService.migrate(mockData)
                    .from(new DataVersion(100))
                    .to(new DataVersion(200))
                    .execute();

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("executes migration from int version")
        void executesFromIntVersion() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = service.migrate(mockData)
                    .from(100)
                    .to(200)
                    .execute();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getFromVersion().getVersion()).isEqualTo(100);
            assertThat(result.getToVersion().getVersion()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Negative Tests - Error Handling")
    class NegativeTests {

        @Test
        @DisplayName("returns failure when fixer throws exception")
        void returnsFailureWhenFixerThrows() {
            RuntimeException exception = new RuntimeException("Migration failed");
            when(mockFixer.update(any(), any(), any())).thenThrow(exception);

            MigrationResult result = service.migrate(mockData)
                    .from(new DataVersion(100))
                    .to(new DataVersion(200))
                    .execute();

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get()).isSameAs(exception);
        }

        @Test
        @DisplayName("records failure metrics when migration fails")
        void recordsFailureMetrics() {
            RuntimeException exception = new RuntimeException("Test error");
            when(mockFixer.update(any(), any(), any())).thenThrow(exception);

            service.migrate(mockData)
                    .from(new DataVersion(100))
                    .to(new DataVersion(200))
                    .execute();

            verify(metrics).recordFailure(
                    eq(DataFixerRegistry.DEFAULT_DOMAIN),
                    eq(100),
                    eq(200),
                    any(Duration.class),
                    eq(exception)
            );
            verify(metrics, never()).recordSuccess(anyString(), anyInt(), anyInt(), any());
        }

        @Test
        @DisplayName("throws when source version not specified")
        void throwsWhenSourceVersionMissing() {
            assertThatThrownBy(() ->
                    service.migrate(mockData)
                            .to(new DataVersion(200))
                            .execute()
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Source version not specified");
        }

        @Test
        @DisplayName("throws when target version not specified")
        void throwsWhenTargetVersionMissing() {
            assertThatThrownBy(() ->
                    service.migrate(mockData)
                            .from(new DataVersion(100))
                            .execute()
            )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Target version not specified");
        }

        @Test
        @DisplayName("throws when domain not found")
        void throwsWhenDomainNotFound() {
            assertThatThrownBy(() ->
                    service.migrate(mockData)
                            .usingDomain("unknown")
                            .from(new DataVersion(100))
                            .to(new DataVersion(200))
                            .execute()
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown domain")
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("throws when data is null")
        void throwsWhenDataIsNull() {
            assertThatThrownBy(() -> service.migrate(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("data");
        }

        @Test
        @DisplayName("throws when from version is null")
        void throwsWhenFromVersionIsNull() {
            assertThatThrownBy(() ->
                    service.migrate(mockData).from((DataVersion) null)
            )
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws when to version is null")
        void throwsWhenToVersionIsNull() {
            assertThatThrownBy(() ->
                    service.migrate(mockData).to((DataVersion) null)
            )
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws when domain is null")
        void throwsWhenDomainIsNull() {
            assertThatThrownBy(() ->
                    service.migrate(mockData).usingDomain(null)
            )
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("getData throws on failed result")
        void getDataThrowsOnFailedResult() {
            when(mockFixer.update(any(), any(), any()))
                    .thenThrow(new RuntimeException("Test"));

            MigrationResult result = service.migrate(mockData)
                    .from(100)
                    .to(200)
                    .execute();

            assertThatThrownBy(result::getData)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Worst Case")
    class EdgeCases {

        @Test
        @DisplayName("handles migration from same version to same version")
        void handlesSameVersionMigration() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = service.migrate(mockData)
                    .from(new DataVersion(100))
                    .to(new DataVersion(100))
                    .execute();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getVersionSpan()).isZero();
        }

        @Test
        @DisplayName("handles downgrade migration")
        void handlesDowngradeMigration() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = service.migrate(mockData)
                    .from(new DataVersion(200))
                    .to(new DataVersion(100))
                    .execute();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getVersionSpan()).isEqualTo(100);
        }

        @Test
        @DisplayName("handles very large version numbers")
        void handlesLargeVersionNumbers() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = service.migrate(mockData)
                    .from(new DataVersion(Integer.MAX_VALUE - 1))
                    .to(new DataVersion(Integer.MAX_VALUE))
                    .execute();

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("handles zero version")
        void handlesZeroVersion() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = service.migrate(mockData)
                    .from(new DataVersion(0))
                    .to(new DataVersion(100))
                    .execute();

            assertThat(result.isSuccess()).isTrue();
        }

        // Note: DataVersion does not allow negative values, so we don't test for negative versions

        @Test
        @DisplayName("toLatest() overrides previous to() call")
        void toLatestOverridesToVersion() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = service.migrate(mockData)
                    .from(100)
                    .to(150)
                    .toLatest()
                    .execute();

            assertThat(result.getToVersion()).isEqualTo(new DataVersion(200));
        }

        @Test
        @DisplayName("to() overrides previous toLatest() call")
        void toVersionOverridesToLatest() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = service.migrate(mockData)
                    .from(100)
                    .toLatest()
                    .to(150)
                    .execute();

            assertThat(result.getToVersion()).isEqualTo(new DataVersion(150));
        }

        @Test
        @DisplayName("handles OutOfMemoryError gracefully")
        void handlesOutOfMemoryError() {
            when(mockFixer.update(any(), any(), any())).thenThrow(new OutOfMemoryError("Test OOM"));

            assertThatThrownBy(() ->
                    service.migrate(mockData)
                            .from(100)
                            .to(200)
                            .execute()
            ).isInstanceOf(OutOfMemoryError.class);
        }

        @Test
        @DisplayName("handles StackOverflowError gracefully")
        void handlesStackOverflowError() {
            when(mockFixer.update(any(), any(), any())).thenThrow(new StackOverflowError("Test"));

            assertThatThrownBy(() ->
                    service.migrate(mockData)
                            .from(100)
                            .to(200)
                            .execute()
            ).isInstanceOf(StackOverflowError.class);
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncExecution {

        @Test
        @Timeout(5)
        @DisplayName("executeAsync returns CompletableFuture")
        void executeAsyncReturnsCompletableFuture() throws Exception {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            CompletableFuture<MigrationResult> future = service.migrate(mockData)
                    .from(100)
                    .to(200)
                    .executeAsync();

            assertThat(future).isNotNull();
            MigrationResult result = future.get();
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Timeout(5)
        @DisplayName("executeAsync handles exceptions")
        void executeAsyncHandlesExceptions() throws Exception {
            when(mockFixer.update(any(), any(), any()))
                    .thenThrow(new RuntimeException("Async error"));

            CompletableFuture<MigrationResult> future = service.migrate(mockData)
                    .from(100)
                    .to(200)
                    .executeAsync();

            MigrationResult result = future.get();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError().get().getMessage()).isEqualTo("Async error");
        }

        @Test
        @Timeout(5)
        @DisplayName("executeAsync uses custom executor")
        void executeAsyncUsesCustomExecutor() throws Exception {
            AtomicInteger executionCount = new AtomicInteger(0);
            Executor countingExecutor = runnable -> {
                executionCount.incrementAndGet();
                runnable.run();
            };

            DefaultMigrationService customService =
                    new DefaultMigrationService(registry, metrics, countingExecutor);
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            customService.migrate(mockData)
                    .from(100)
                    .to(200)
                    .executeAsync()
                    .get();

            assertThat(executionCount.get()).isEqualTo(1);
        }

        @Test
        @Timeout(10)
        @DisplayName("multiple async migrations execute concurrently")
        void multipleAsyncMigrationsExecuteConcurrently() throws Exception {
            CountDownLatch startLatch = new CountDownLatch(3);
            CountDownLatch endLatch = new CountDownLatch(3);

            when(mockFixer.update(any(), any(), any())).thenAnswer(inv -> {
                startLatch.countDown();
                startLatch.await(5, TimeUnit.SECONDS);
                endLatch.countDown();
                return mockData;
            });

            Executor parallelExecutor = Executors.newFixedThreadPool(3);
            DefaultMigrationService parallelService =
                    new DefaultMigrationService(registry, metrics, parallelExecutor);

            CompletableFuture<MigrationResult> f1 = parallelService.migrate(mockData)
                    .from(100).to(200).executeAsync();
            CompletableFuture<MigrationResult> f2 = parallelService.migrate(mockData)
                    .from(100).to(200).executeAsync();
            CompletableFuture<MigrationResult> f3 = parallelService.migrate(mockData)
                    .from(100).to(200).executeAsync();

            // Wait for all to complete
            CompletableFuture.allOf(f1, f2, f3).get(10, TimeUnit.SECONDS);

            assertThat(f1.get().isSuccess()).isTrue();
            assertThat(f2.get().isSuccess()).isTrue();
            assertThat(f3.get().isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Service Methods")
    class ServiceMethods {

        @Test
        @DisplayName("getCurrentVersion returns default domain version")
        void getCurrentVersionReturnsDefaultDomainVersion() {
            DataVersion version = service.getCurrentVersion();
            assertThat(version).isEqualTo(new DataVersion(200));
        }

        @Test
        @DisplayName("getCurrentVersion with domain returns domain version")
        void getCurrentVersionWithDomainReturnsDomainVersion() {
            AetherDataFixer gameFixer = mock(AetherDataFixer.class);
            when(gameFixer.currentVersion()).thenReturn(new DataVersion(150));
            registry.register("game", gameFixer);

            DataVersion version = service.getCurrentVersion("game");
            assertThat(version).isEqualTo(new DataVersion(150));
        }

        @Test
        @DisplayName("getCurrentVersion throws for unknown domain")
        void getCurrentVersionThrowsForUnknownDomain() {
            assertThatThrownBy(() -> service.getCurrentVersion("unknown"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("hasDomain returns true for registered domain")
        void hasDomainReturnsTrueForRegistered() {
            assertThat(service.hasDomain(DataFixerRegistry.DEFAULT_DOMAIN)).isTrue();
        }

        @Test
        @DisplayName("hasDomain returns false for unregistered domain")
        void hasDomainReturnsFalseForUnregistered() {
            assertThat(service.hasDomain("unknown")).isFalse();
        }

        @Test
        @DisplayName("getAvailableDomains returns all domains")
        void getAvailableDomainsReturnsAllDomains() {
            registry.register("game", mock(AetherDataFixer.class));
            registry.register("user", mock(AetherDataFixer.class));

            Set<String> domains = service.getAvailableDomains();

            assertThat(domains).containsExactlyInAnyOrder(
                    DataFixerRegistry.DEFAULT_DOMAIN, "game", "user"
            );
        }

        @Test
        @DisplayName("getCurrentVersion rejects null domain")
        void getCurrentVersionRejectsNullDomain() {
            assertThatThrownBy(() -> service.getCurrentVersion(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("hasDomain rejects null domain")
        void hasDomainRejectsNullDomain() {
            assertThatThrownBy(() -> service.hasDomain(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Multi-Domain Scenarios")
    class MultiDomainScenarios {

        @BeforeEach
        void setupMultipleDomains() {
            AetherDataFixer gameFixer = mock(AetherDataFixer.class);
            AetherDataFixer userFixer = mock(AetherDataFixer.class);

            when(gameFixer.currentVersion()).thenReturn(new DataVersion(300));
            when(userFixer.currentVersion()).thenReturn(new DataVersion(150));
            when(gameFixer.update(any(), any(), any())).thenReturn(mockData);
            when(userFixer.update(any(), any(), any())).thenReturn(mockData);

            registry.register("game", gameFixer);
            registry.register("user", userFixer);
        }

        @Test
        @DisplayName("migrates in correct domain")
        void migratesInCorrectDomain() {
            MigrationResult result = service.migrate(mockData)
                    .usingDomain("game")
                    .from(100)
                    .toLatest()
                    .execute();

            assertThat(result.getDomain()).isEqualTo("game");
            assertThat(result.getToVersion()).isEqualTo(new DataVersion(300));
        }

        @Test
        @DisplayName("uses default domain when not specified")
        void usesDefaultDomainWhenNotSpecified() {
            when(mockFixer.update(any(), any(), any())).thenReturn(mockData);

            MigrationResult result = service.migrate(mockData)
                    .from(100)
                    .toLatest()
                    .execute();

            assertThat(result.getDomain()).isEqualTo(DataFixerRegistry.DEFAULT_DOMAIN);
            assertThat(result.getToVersion()).isEqualTo(new DataVersion(200));
        }

        @Test
        @DisplayName("switch domain updates toLatest version")
        void switchDomainUpdatesToLatestVersion() {
            MigrationResult result = service.migrate(mockData)
                    .usingDomain("user")
                    .from(100)
                    .toLatest()
                    .execute();

            assertThat(result.getToVersion()).isEqualTo(new DataVersion(150));
        }
    }
}
