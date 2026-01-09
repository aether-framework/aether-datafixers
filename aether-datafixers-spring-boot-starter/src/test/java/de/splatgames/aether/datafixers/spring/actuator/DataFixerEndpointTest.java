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

package de.splatgames.aether.datafixers.spring.actuator;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DataFixerEndpoint}.
 */
@DisplayName("DataFixerEndpoint")
class DataFixerEndpointTest {

    private DataFixerRegistry registry;
    private DataFixerEndpoint endpoint;

    @BeforeEach
    void setUp() {
        registry = new DataFixerRegistry();
        endpoint = new DataFixerEndpoint(registry);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("rejects null registry")
        void rejectsNullRegistry() {
            assertThatThrownBy(() -> new DataFixerEndpoint(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("registry");
        }
    }

    @Nested
    @DisplayName("Summary Operation")
    class SummaryOperation {

        @Test
        @DisplayName("returns empty domains when no fixers registered")
        void returnsEmptyDomainsWhenNoFixers() {
            DataFixerEndpoint.DataFixersSummary summary = endpoint.summary();

            assertThat(summary.domains()).isEmpty();
        }

        @Test
        @DisplayName("includes all registered domains")
        void includesAllRegisteredDomains() {
            AetherDataFixer gameFixer = mock(AetherDataFixer.class);
            AetherDataFixer userFixer = mock(AetherDataFixer.class);
            when(gameFixer.currentVersion()).thenReturn(new DataVersion(200));
            when(userFixer.currentVersion()).thenReturn(new DataVersion(150));
            registry.register("game", gameFixer);
            registry.register("user", userFixer);

            DataFixerEndpoint.DataFixersSummary summary = endpoint.summary();

            assertThat(summary.domains()).containsKeys("game", "user");
        }

        @Test
        @DisplayName("includes current version for each domain")
        void includesCurrentVersionForEachDomain() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            registry.register("game", fixer);

            DataFixerEndpoint.DataFixersSummary summary = endpoint.summary();

            assertThat(summary.domains().get("game").currentVersion()).isEqualTo(200);
        }

        @Test
        @DisplayName("sets UP status for healthy fixers")
        void setsUpStatusForHealthyFixers() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            registry.register("game", fixer);

            DataFixerEndpoint.DataFixersSummary summary = endpoint.summary();

            assertThat(summary.domains().get("game").status()).isEqualTo("UP");
        }

        @Test
        @DisplayName("sets DOWN status for unhealthy fixers")
        void setsDownStatusForUnhealthyFixers() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenThrow(new RuntimeException("Error"));
            registry.register("game", fixer);

            DataFixerEndpoint.DataFixersSummary summary = endpoint.summary();

            assertThat(summary.domains().get("game").status()).startsWith("DOWN:");
            assertThat(summary.domains().get("game").currentVersion()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Domain Details Operation")
    class DomainDetailsOperation {

        @Test
        @DisplayName("returns null for unknown domain")
        void returnsNullForUnknownDomain() {
            DataFixerEndpoint.DomainDetails details = endpoint.domainDetails("unknown");

            assertThat(details).isNull();
        }

        @Test
        @DisplayName("returns details for registered domain")
        void returnsDetailsForRegisteredDomain() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            registry.register("game", fixer);

            DataFixerEndpoint.DomainDetails details = endpoint.domainDetails("game");

            assertThat(details).isNotNull();
            assertThat(details.domain()).isEqualTo("game");
            assertThat(details.currentVersion()).isEqualTo(200);
            assertThat(details.status()).isEqualTo("UP");
        }

        @Test
        @DisplayName("handles fixer errors gracefully")
        void handlesFixerErrorsGracefully() {
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenThrow(new RuntimeException("Init failed"));
            registry.register("game", fixer);

            DataFixerEndpoint.DomainDetails details = endpoint.domainDetails("game");

            assertThat(details).isNotNull();
            assertThat(details.domain()).isEqualTo("game");
            assertThat(details.currentVersion()).isEqualTo(-1);
            assertThat(details.status()).contains("DOWN").contains("Init failed");
        }
    }

    @Nested
    @DisplayName("Record DTOs")
    class RecordDTOs {

        @Test
        @DisplayName("DataFixersSummary record works correctly")
        void dataFixersSummaryRecordWorks() {
            var domains = java.util.Map.of(
                    "game", new DataFixerEndpoint.DomainSummary(200, "UP")
            );
            var summary = new DataFixerEndpoint.DataFixersSummary(domains);

            assertThat(summary.domains()).hasSize(1);
            assertThat(summary.domains().get("game").currentVersion()).isEqualTo(200);
        }

        @Test
        @DisplayName("DomainSummary record works correctly")
        void domainSummaryRecordWorks() {
            var summary = new DataFixerEndpoint.DomainSummary(200, "UP");

            assertThat(summary.currentVersion()).isEqualTo(200);
            assertThat(summary.status()).isEqualTo("UP");
        }

        @Test
        @DisplayName("DomainDetails record works correctly")
        void domainDetailsRecordWorks() {
            var details = new DataFixerEndpoint.DomainDetails("game", 200, "UP");

            assertThat(details.domain()).isEqualTo("game");
            assertThat(details.currentVersion()).isEqualTo(200);
            assertThat(details.status()).isEqualTo("UP");
        }
    }
}
