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
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.FixExecution;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import de.splatgames.aether.datafixers.api.diagnostic.RuleApplication;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry;
import de.splatgames.aether.datafixers.spring.service.DiagnosticReportStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

            assertThat(summary.domains().get("game").status()).isEqualTo("DOWN");
            assertThat(summary.domains().get("game").error()).isEqualTo("Error");
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
            assertThat(details.status()).isEqualTo("DOWN");
            assertThat(details.error()).isEqualTo("Init failed");
        }
    }

    @Nested
    @DisplayName("Record DTOs")
    class RecordDTOs {

        @Test
        @DisplayName("DataFixersSummary record works correctly")
        void dataFixersSummaryRecordWorks() {
            var domains = java.util.Map.of(
                    "game", new DataFixerEndpoint.DomainSummary(200, "UP", null)
            );
            var summary = new DataFixerEndpoint.DataFixersSummary(domains);

            assertThat(summary.domains()).hasSize(1);
            assertThat(summary.domains().get("game").currentVersion()).isEqualTo(200);
        }

        @Test
        @DisplayName("DomainSummary record works correctly")
        void domainSummaryRecordWorks() {
            var summary = new DataFixerEndpoint.DomainSummary(200, "UP", null);

            assertThat(summary.currentVersion()).isEqualTo(200);
            assertThat(summary.status()).isEqualTo("UP");
            assertThat(summary.error()).isNull();
        }

        @Test
        @DisplayName("DomainDetails record works correctly")
        void domainDetailsRecordWorks() {
            var details = new DataFixerEndpoint.DomainDetails("game", 200, "UP", null, null);

            assertThat(details.domain()).isEqualTo("game");
            assertThat(details.currentVersion()).isEqualTo(200);
            assertThat(details.status()).isEqualTo("UP");
            assertThat(details.error()).isNull();
            assertThat(details.lastDiagnostics()).isNull();
        }
    }

    @Nested
    @DisplayName("Field Diagnostics")
    class FieldDiagnostics {

        @Test
        @DisplayName("domain details includes diagnostics when store has report")
        void includesDiagnosticsFromStore() {
            DataFixerRegistry reg = new DataFixerRegistry();
            DiagnosticReportStore store = new DiagnosticReportStore();

            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            reg.register("game", fixer);

            // Build a real FixExecution record (records are final, so they cannot be mocked)
            // carrying two field operations.
            RuleApplication ruleApp = new RuleApplication(
                    "renameAndRemove",
                    "player",
                    Instant.now(),
                    Duration.ofMillis(5),
                    true,
                    null,
                    List.of(
                            FieldOperation.rename("oldName", "newName"),
                            FieldOperation.remove("deprecated")
                    )
            );
            FixExecution fix = new FixExecution(
                    "test_fix",
                    new DataVersion(100),
                    new DataVersion(200),
                    Instant.now(),
                    Duration.ofMillis(20),
                    List.of(ruleApp),
                    null,
                    null
            );

            // The MigrationReport interface is mockable; only the concrete records are not.
            MigrationReport report = mock(MigrationReport.class);
            when(report.fromVersion()).thenReturn(new DataVersion(100));
            when(report.toVersion()).thenReturn(new DataVersion(200));
            when(report.totalDuration()).thenReturn(Duration.ofMillis(42));
            when(report.fixCount()).thenReturn(1);
            when(report.totalFieldOperationCount()).thenReturn(2);
            when(report.fixExecutions()).thenReturn(List.of(fix));
            store.store("game", report);

            DataFixerEndpoint ep = new DataFixerEndpoint(reg, store);
            DataFixerEndpoint.DomainDetails details = ep.domainDetails("game");

            assertThat(details).isNotNull();
            assertThat(details.lastDiagnostics()).isNotNull();
            assertThat(details.lastDiagnostics().fromVersion()).isEqualTo(100);
            assertThat(details.lastDiagnostics().toVersion()).isEqualTo(200);
            assertThat(details.lastDiagnostics().fixCount()).isEqualTo(1);
            assertThat(details.lastDiagnostics().fieldOperationCount()).isEqualTo(2);
            assertThat(details.lastDiagnostics().fieldOperations()).hasSize(2);
        }

        @Test
        @DisplayName("domain details has null diagnostics when store is empty")
        void nullDiagnosticsWhenStoreEmpty() {
            DataFixerRegistry reg = new DataFixerRegistry();
            DiagnosticReportStore store = new DiagnosticReportStore();

            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            reg.register("game", fixer);

            DataFixerEndpoint ep = new DataFixerEndpoint(reg, store);
            DataFixerEndpoint.DomainDetails details = ep.domainDetails("game");

            assertThat(details).isNotNull();
            assertThat(details.lastDiagnostics()).isNull();
        }

        @Test
        @DisplayName("domain details has null diagnostics when no store provided")
        void nullDiagnosticsWhenNoStore() {
            DataFixerRegistry reg = new DataFixerRegistry();
            AetherDataFixer fixer = mock(AetherDataFixer.class);
            when(fixer.currentVersion()).thenReturn(new DataVersion(200));
            reg.register("game", fixer);

            DataFixerEndpoint ep = new DataFixerEndpoint(reg);
            DataFixerEndpoint.DomainDetails details = ep.domainDetails("game");

            assertThat(details).isNotNull();
            assertThat(details.lastDiagnostics()).isNull();
        }

        @Test
        @DisplayName("FieldOperationSummary record works correctly")
        void fieldOperationSummaryRecordWorks() {
            var summary = new DataFixerEndpoint.FieldOperationSummary(
                    "RENAME", "oldName", "newName", null);

            assertThat(summary.type()).isEqualTo("RENAME");
            assertThat(summary.field()).isEqualTo("oldName");
            assertThat(summary.target()).isEqualTo("newName");
            assertThat(summary.description()).isNull();
        }

        @Test
        @DisplayName("FieldDiagnosticsSummary defensive copy")
        void fieldDiagnosticsSummaryDefensiveCopy() {
            var ops = List.of(
                    new DataFixerEndpoint.FieldOperationSummary("RENAME", "a", "b", null)
            );
            var summary = new DataFixerEndpoint.FieldDiagnosticsSummary(
                    100, 200, 42, 1, 1, ops);

            assertThat(summary.fieldOperations()).hasSize(1);
            assertThat(summary.fromVersion()).isEqualTo(100);
            assertThat(summary.toVersion()).isEqualTo(200);
            assertThat(summary.durationMs()).isEqualTo(42);
        }
    }
}
