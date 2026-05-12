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

package de.splatgames.aether.datafixers.functional.diagnostic;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import de.splatgames.aether.datafixers.spring.actuator.DataFixerEndpoint;
import de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry;
import de.splatgames.aether.datafixers.spring.service.DefaultMigrationService;
import de.splatgames.aether.datafixers.spring.service.MigrationResult;
import de.splatgames.aether.datafixers.spring.service.MigrationService;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the Spring Boot integration of the field-aware
 * diagnostics system.
 *
 * <p>These tests wire the Spring components manually (without
 * {@code @SpringBootTest}) to keep startup fast and dependencies minimal:</p>
 *
 * <ol>
 *   <li>Bootstrap → {@link AetherDataFixer} via {@link DataFixerRuntimeFactory}</li>
 *   <li>{@link DataFixerRegistry} populated with a single domain</li>
 *   <li>{@link DefaultMigrationService} constructed with the registry</li>
 *   <li>Migration executed via the fluent
 *       {@link MigrationService#migrate(TaggedDynamic) migrate(...)} builder
 *       with
 *       {@link de.splatgames.aether.datafixers.spring.service.MigrationService.MigrationRequestBuilder#withDiagnostics() withDiagnostics()}</li>
 *   <li>{@link MigrationResult#getDiagnosticReport()} verified</li>
 *   <li>{@link DefaultMigrationService#getDiagnosticReportStore() store} verified</li>
 *   <li>{@link DataFixerEndpoint#domainDetails(String) actuator endpoint} verified</li>
 * </ol>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>{@code withDiagnostics()} default options enable capture</li>
 *   <li>{@code withDiagnostics(DiagnosticOptions)} custom options respected</li>
 *   <li>{@code MigrationResult.getDiagnosticReport()} returns populated optional</li>
 *   <li>{@code DiagnosticReportStore} stores the latest report per domain</li>
 *   <li>{@code DataFixerEndpoint} exposes field diagnostics in domain details</li>
 *   <li>Migration without {@code withDiagnostics()} produces empty report</li>
 * </ul>
 */
@DisplayName("Spring Migration Diagnostics E2E")
@Tag("e2e")
class SpringMigrationDiagnosticsE2E {

    private static final TypeReference PLAYER = new TypeReference("player");

    private DataFixerRegistry registry;
    private DefaultMigrationService migrationService;
    private DataFixerEndpoint endpoint;

    @BeforeEach
    void setUp() {
        // Wire the Spring components by hand to avoid Spring boot context startup.
        final DataFixerBootstrap bootstrap = new TestBootstrap();
        final AetherDataFixer fixer = new DataFixerRuntimeFactory()
                .create(new DataVersion(200), bootstrap);

        this.registry = new DataFixerRegistry();
        this.registry.register(DataFixerRegistry.DEFAULT_DOMAIN, fixer);

        this.migrationService = new DefaultMigrationService(this.registry, null);
        this.endpoint = new DataFixerEndpoint(this.registry,
                this.migrationService.getDiagnosticReportStore());
    }

    @NotNull
    private TaggedDynamic createInput() {
        final Dynamic<JsonElement> data = TestData.gson().object()
                .put("oldName", "Alice")
                .put("deprecated", "legacy")
                .build();
        return new TaggedDynamic(PLAYER, data);
    }

    @Nested
    @DisplayName("withDiagnostics() Builder")
    class WithDiagnosticsBuilder {

        @Test
        @DisplayName("default withDiagnostics() captures field operations")
        void defaultWithDiagnosticsCaptures() {
            final MigrationResult result = SpringMigrationDiagnosticsE2E.this.migrationService
                    .migrate(createInput())
                    .from(100).to(200)
                    .withDiagnostics()
                    .execute();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getDiagnosticReport()).isPresent();

            final MigrationReport report = result.getDiagnosticReport().orElseThrow();
            assertThat(report.totalFieldOperationCount()).isGreaterThan(0);
        }

        @Test
        @DisplayName("custom DiagnosticOptions are respected")
        void customDiagnosticOptionsRespected() {
            final MigrationResult result = SpringMigrationDiagnosticsE2E.this.migrationService
                    .migrate(createInput())
                    .from(100).to(200)
                    .withDiagnostics(DiagnosticOptions.builder()
                            .captureSnapshots(false)
                            .captureRuleDetails(true)
                            .captureFieldDetails(true)
                            .build())
                    .execute();

            assertThat(result.getDiagnosticReport()).isPresent();
            final MigrationReport report = result.getDiagnosticReport().orElseThrow();
            // Field operations should be captured
            assertThat(report.totalFieldOperationCount()).isGreaterThan(0);
            // Snapshots should be empty (we disabled them)
            assertThat(report.inputSnapshot()).isEmpty();
            assertThat(report.outputSnapshot()).isEmpty();
        }

        @Test
        @DisplayName("migration without withDiagnostics() has empty report")
        void migrationWithoutDiagnostics() {
            final MigrationResult result = SpringMigrationDiagnosticsE2E.this.migrationService
                    .migrate(createInput())
                    .from(100).to(200)
                    .execute();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getDiagnosticReport()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DiagnosticReportStore Population")
    class DiagnosticReportStorePopulation {

        @Test
        @DisplayName("store retains the latest report after a diagnostic migration")
        void storeRetainsLatestReport() {
            SpringMigrationDiagnosticsE2E.this.migrationService
                    .migrate(createInput())
                    .from(100).to(200)
                    .withDiagnostics()
                    .execute();

            assertThat(SpringMigrationDiagnosticsE2E.this.migrationService
                    .getDiagnosticReportStore()
                    .get(DataFixerRegistry.DEFAULT_DOMAIN))
                    .isPresent();
        }

        @Test
        @DisplayName("store does not get populated for non-diagnostic migrations")
        void storeNotPopulatedWithoutDiagnostics() {
            SpringMigrationDiagnosticsE2E.this.migrationService
                    .migrate(createInput())
                    .from(100).to(200)
                    .execute();

            assertThat(SpringMigrationDiagnosticsE2E.this.migrationService
                    .getDiagnosticReportStore()
                    .get(DataFixerRegistry.DEFAULT_DOMAIN))
                    .isEmpty();
        }

        @Test
        @DisplayName("running diagnostics twice replaces the previous stored report")
        void diagnosticsReplaceStoredReport() {
            SpringMigrationDiagnosticsE2E.this.migrationService
                    .migrate(createInput())
                    .from(100).to(200)
                    .withDiagnostics()
                    .execute();
            final MigrationReport first = SpringMigrationDiagnosticsE2E.this.migrationService
                    .getDiagnosticReportStore()
                    .get(DataFixerRegistry.DEFAULT_DOMAIN)
                    .orElseThrow();

            SpringMigrationDiagnosticsE2E.this.migrationService
                    .migrate(createInput())
                    .from(100).to(200)
                    .withDiagnostics()
                    .execute();
            final MigrationReport second = SpringMigrationDiagnosticsE2E.this.migrationService
                    .getDiagnosticReportStore()
                    .get(DataFixerRegistry.DEFAULT_DOMAIN)
                    .orElseThrow();

            // Different report instance after the second migration
            assertThat(second).isNotSameAs(first);
        }
    }

    @Nested
    @DisplayName("Actuator Endpoint Field Diagnostics")
    class ActuatorEndpointFieldDiagnostics {

        @Test
        @DisplayName("domain details exposes lastDiagnostics after a diagnostic migration")
        void domainDetailsExposesLastDiagnostics() {
            SpringMigrationDiagnosticsE2E.this.migrationService
                    .migrate(createInput())
                    .from(100).to(200)
                    .withDiagnostics()
                    .execute();

            final DataFixerEndpoint.DomainDetails details = SpringMigrationDiagnosticsE2E.this.endpoint
                    .domainDetails(DataFixerRegistry.DEFAULT_DOMAIN);

            assertThat(details).isNotNull();
            assertThat(details.lastDiagnostics()).isNotNull();
            assertThat(details.lastDiagnostics().fieldOperationCount()).isGreaterThan(0);
            assertThat(details.lastDiagnostics().fixCount()).isGreaterThan(0);
            assertThat(details.lastDiagnostics().fieldOperations()).isNotEmpty();
        }

        @Test
        @DisplayName("domain details lastDiagnostics is null when no diagnostic migration ran")
        void lastDiagnosticsNullByDefault() {
            // No migration ran yet
            final DataFixerEndpoint.DomainDetails details = SpringMigrationDiagnosticsE2E.this.endpoint
                    .domainDetails(DataFixerRegistry.DEFAULT_DOMAIN);

            assertThat(details).isNotNull();
            assertThat(details.lastDiagnostics()).isNull();
        }

        @Test
        @DisplayName("field operations exposed via actuator carry type and target metadata")
        void fieldOperationMetadataExposed() {
            SpringMigrationDiagnosticsE2E.this.migrationService
                    .migrate(createInput())
                    .from(100).to(200)
                    .withDiagnostics()
                    .execute();

            final DataFixerEndpoint.DomainDetails details = SpringMigrationDiagnosticsE2E.this.endpoint
                    .domainDetails(DataFixerRegistry.DEFAULT_DOMAIN);

            assertThat(details.lastDiagnostics().fieldOperations())
                    .extracting(DataFixerEndpoint.FieldOperationSummary::type)
                    .contains(FieldOperationType.RENAME.name());
        }
    }

    // -------------------------------------------------------------------------
    // Test bootstrap and fix
    // -------------------------------------------------------------------------

    /**
     * Bootstrap with two schemas (v100, v200) and a single composed fix using
     * field-aware {@link Rules}.
     */
    private static final class TestBootstrap implements DataFixerBootstrap {

        private SchemaRegistry capturedRegistry;

        @Override
        public void registerSchemas(@NotNull final SchemaRegistry registry) {
            this.capturedRegistry = registry;
            registry.register(MockSchemas.builder(100)
                    .withType(PLAYER, DiagnosticsTestSupport.passthroughType(PLAYER)).build());
            registry.register(MockSchemas.builder(200)
                    .withType(PLAYER, DiagnosticsTestSupport.passthroughType(PLAYER)).build());
        }

        @Override
        public void registerFixes(@NotNull final FixRegistrar registrar) {
            registrar.register(PLAYER, new ComposedFix(this.capturedRegistry));
        }
    }

    /**
     * Single fix that performs rename + remove + add via {@code Rules.seq}.
     */
    private static final class ComposedFix extends SchemaDataFix {

        ComposedFix(@NotNull final SchemaRegistry schemas) {
            super("composed_player_fix", new DataVersion(100), new DataVersion(200), schemas);
        }

        @Override
        @NotNull
        protected TypeRewriteRule makeRule(@NotNull final Schema inputSchema,
                                           @NotNull final Schema outputSchema) {
            return Rules.seq(
                    Rules.renameField(GsonOps.INSTANCE, "oldName", "newName"),
                    Rules.removeField(GsonOps.INSTANCE, "deprecated"),
                    Rules.addField(GsonOps.INSTANCE, "health",
                            new Dynamic<>(GsonOps.INSTANCE, GsonOps.INSTANCE.createInt(100)))
            );
        }
    }
}
