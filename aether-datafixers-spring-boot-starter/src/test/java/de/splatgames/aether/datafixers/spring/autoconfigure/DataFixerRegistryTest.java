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

package de.splatgames.aether.datafixers.spring.autoconfigure;

import de.splatgames.aether.datafixers.core.AetherDataFixer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DataFixerRegistry}.
 */
@DisplayName("DataFixerRegistry")
class DataFixerRegistryTest {

    private DataFixerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DataFixerRegistry();
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("register() stores fixer by domain")
        void registerStoresFixerByDomain() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);

            registry.register("game", fixer);

            assertThat(registry.get("game")).isSameAs(fixer);
        }

        @Test
        @DisplayName("register() throws for duplicate domain")
        void registerThrowsForDuplicateDomain() {
            AetherDataFixer fixer1 = mock(AetherDataFixer.class);
            AetherDataFixer fixer2 = mock(AetherDataFixer.class);

            registry.register("game", fixer1);

            assertThatThrownBy(() -> registry.register("game", fixer2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("game");
        }

        @Test
        @DisplayName("register() allows multiple domains")
        void registerAllowsMultipleDomains() {
            AetherDataFixer gameFixer = mock(AetherDataFixer.class);
            AetherDataFixer userFixer = mock(AetherDataFixer.class);

            registry.register("game", gameFixer);
            registry.register("user", userFixer);

            assertThat(registry.get("game")).isSameAs(gameFixer);
            assertThat(registry.get("user")).isSameAs(userFixer);
        }
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("get() returns null for unknown domain")
        void getReturnsNullForUnknownDomain() {
            assertThat(registry.get("unknown")).isNull();
        }

        @Test
        @DisplayName("get() returns registered fixer")
        void getReturnsRegisteredFixer() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            registry.register("game", fixer);

            assertThat(registry.get("game")).isSameAs(fixer);
        }

        @Test
        @DisplayName("require() throws for unknown domain")
        void requireThrowsForUnknownDomain() {
            assertThatThrownBy(() -> registry.require("unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("require() returns registered fixer")
        void requireReturnsRegisteredFixer() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            registry.register("game", fixer);

            assertThat(registry.require("game")).isSameAs(fixer);
        }

        @Test
        @DisplayName("getDefault() returns fixer for default domain")
        void getDefaultReturnsFixerForDefaultDomain() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            registry.register(DataFixerRegistry.DEFAULT_DOMAIN, fixer);

            assertThat(registry.getDefault()).isSameAs(fixer);
        }

        @Test
        @DisplayName("getDefault() returns null if default not registered")
        void getDefaultReturnsNullIfNotRegistered() {
            assertThat(registry.getDefault()).isNull();
        }
    }

    @Nested
    @DisplayName("Collection Operations")
    class CollectionOperations {

        @Test
        @DisplayName("getAll() returns immutable copy")
        void getAllReturnsImmutableCopy() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            registry.register("game", fixer);

            Map<String, AetherDataFixer> all = registry.getAll();

            assertThat(all).containsEntry("game", fixer);
            assertThatThrownBy(() -> all.put("new", mock(AetherDataFixer.class)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("getDomains() returns all registered domains")
        void getDomainsReturnsAllDomains() {
            registry.register("game", mock(AetherDataFixer.class));
            registry.register("user", mock(AetherDataFixer.class));

            Set<String> domains = registry.getDomains();

            assertThat(domains).containsExactlyInAnyOrder("game", "user");
        }

        @Test
        @DisplayName("getDomains() returns immutable set")
        void getDomainsReturnsImmutableSet() {
            registry.register("game", mock(AetherDataFixer.class));

            Set<String> domains = registry.getDomains();

            assertThatThrownBy(() -> domains.add("new"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("contains() returns true for registered domain")
        void containsReturnsTrueForRegisteredDomain() {
            registry.register("game", mock(AetherDataFixer.class));

            assertThat(registry.contains("game")).isTrue();
        }

        @Test
        @DisplayName("contains() returns false for unknown domain")
        void containsReturnsFalseForUnknownDomain() {
            assertThat(registry.contains("unknown")).isFalse();
        }

        @Test
        @DisplayName("size() returns number of registered fixers")
        void sizeReturnsNumberOfFixers() {
            assertThat(registry.size()).isZero();

            registry.register("game", mock(AetherDataFixer.class));
            assertThat(registry.size()).isEqualTo(1);

            registry.register("user", mock(AetherDataFixer.class));
            assertThat(registry.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("isEmpty() returns true when empty")
        void isEmptyReturnsTrueWhenEmpty() {
            assertThat(registry.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("isEmpty() returns false when not empty")
        void isEmptyReturnsFalseWhenNotEmpty() {
            registry.register("game", mock(AetherDataFixer.class));

            assertThat(registry.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Default Domain Constant")
    class DefaultDomainConstant {

        @Test
        @DisplayName("DEFAULT_DOMAIN is 'default'")
        void defaultDomainIsDefault() {
            assertThat(DataFixerRegistry.DEFAULT_DOMAIN).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @org.junit.jupiter.api.Timeout(10)
        @DisplayName("handles concurrent registrations to different domains")
        void handlesConcurrentRegistrationsToDifferentDomains() throws Exception {
            int threadCount = 10;
            java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CountDownLatch endLatch = new java.util.concurrent.CountDownLatch(threadCount);
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        registry.register("domain-" + index, mock(AetherDataFixer.class));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(registry.size()).isEqualTo(threadCount);
        }

        @Test
        @org.junit.jupiter.api.Timeout(10)
        @DisplayName("handles concurrent reads safely")
        void handlesConcurrentReadsSafely() throws Exception {
            // Pre-register some fixers
            for (int i = 0; i < 5; i++) {
                registry.register("domain-" + i, mock(AetherDataFixer.class));
            }

            int readerCount = 20;
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(readerCount);
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(readerCount);
            java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

            for (int i = 0; i < readerCount; i++) {
                final int index = i % 5;
                executor.submit(() -> {
                    try {
                        AetherDataFixer fixer = registry.get("domain-" + index);
                        if (fixer != null) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(successCount.get()).isEqualTo(readerCount);
        }

        @Test
        @org.junit.jupiter.api.Timeout(10)
        @DisplayName("handles concurrent reads and writes")
        void handlesConcurrentReadsAndWrites() throws Exception {
            int writerCount = 5;
            int readerCount = 10;
            java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CountDownLatch endLatch = new java.util.concurrent.CountDownLatch(writerCount + readerCount);
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(writerCount + readerCount);

            // Writers
            for (int i = 0; i < writerCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        registry.register("write-domain-" + index, mock(AetherDataFixer.class));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            // Readers
            for (int i = 0; i < readerCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        registry.getDomains();
                        registry.getAll();
                        registry.size();
                        registry.isEmpty();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(registry.size()).isEqualTo(writerCount);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles empty string domain")
        void handlesEmptyStringDomain() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);

            registry.register("", fixer);

            assertThat(registry.get("")).isSameAs(fixer);
            assertThat(registry.contains("")).isTrue();
        }

        @Test
        @DisplayName("handles domain with special characters")
        void handlesDomainWithSpecialCharacters() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);

            registry.register("game-v2.1_test@prod", fixer);

            assertThat(registry.get("game-v2.1_test@prod")).isSameAs(fixer);
        }

        @Test
        @DisplayName("handles unicode domain name")
        void handlesUnicodeDomainName() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);

            registry.register("游戏数据", fixer);

            assertThat(registry.get("游戏数据")).isSameAs(fixer);
        }

        @Test
        @DisplayName("handles very long domain name")
        void handlesVeryLongDomainName() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            String longDomain = "a".repeat(1000);

            registry.register(longDomain, fixer);

            assertThat(registry.get(longDomain)).isSameAs(fixer);
        }

        @Test
        @DisplayName("require message includes available domains")
        void requireMessageIncludesAvailableDomains() {
            registry.register("game", mock(AetherDataFixer.class));
            registry.register("user", mock(AetherDataFixer.class));

            assertThatThrownBy(() -> registry.require("unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("game")
                    .hasMessageContaining("user");
        }

        @Test
        @DisplayName("register error message includes domain name")
        void registerErrorMessageIncludesDomainName() {
            registry.register("existing", mock(AetherDataFixer.class));

            assertThatThrownBy(() -> registry.register("existing", mock(AetherDataFixer.class)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("existing");
        }
    }

    @Nested
    @DisplayName("Worst Case Scenarios")
    class WorstCaseScenarios {

        @Test
        @DisplayName("handles registering many domains")
        void handlesRegisteringManyDomains() {
            int domainCount = 1000;

            for (int i = 0; i < domainCount; i++) {
                registry.register("domain-" + i, mock(AetherDataFixer.class));
            }

            assertThat(registry.size()).isEqualTo(domainCount);
            assertThat(registry.getDomains()).hasSize(domainCount);
        }

        @Test
        @DisplayName("getAll returns copy even after modifications")
        void getAllReturnsCopyEvenAfterModifications() {
            registry.register("game", mock(AetherDataFixer.class));
            Map<String, AetherDataFixer> allBefore = registry.getAll();

            registry.register("user", mock(AetherDataFixer.class));
            Map<String, AetherDataFixer> allAfter = registry.getAll();

            assertThat(allBefore).hasSize(1);
            assertThat(allAfter).hasSize(2);
        }

        @Test
        @DisplayName("getDomains returns copy even after modifications")
        void getDomainsReturnsCopyEvenAfterModifications() {
            registry.register("game", mock(AetherDataFixer.class));
            Set<String> domainsBefore = registry.getDomains();

            registry.register("user", mock(AetherDataFixer.class));
            Set<String> domainsAfter = registry.getDomains();

            assertThat(domainsBefore).hasSize(1);
            assertThat(domainsAfter).hasSize(2);
        }
    }
}
