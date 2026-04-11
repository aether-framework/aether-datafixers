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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import de.splatgames.aether.datafixers.schematools.analysis.FieldOperationReport;
import de.splatgames.aether.datafixers.schematools.analysis.FixFieldOperations;
import de.splatgames.aether.datafixers.schematools.analysis.MigrationAnalyzer;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for static field operation analysis via the schema-tools
 * module.
 *
 * <p>These tests verify that
 * {@link MigrationAnalyzer#analyzeFieldOperations()} correctly extracts
 * {@link de.splatgames.aether.datafixers.api.diagnostic.FieldOperation field operations}
 * from registered fixes <b>without running any data through the fixer</b>. The
 * analysis path is:</p>
 *
 * <ol>
 *   <li>{@link DataFixerBootstrap} registers schemas and fixes</li>
 *   <li>{@link MigrationAnalyzer#forBootstrap(DataFixerBootstrap)} captures
 *       both into temporary registries</li>
 *   <li>{@link MigrationAnalyzer#analyzeFieldOperations()} walks every registered
 *       fix, calls
 *       {@link SchemaDataFix#introspectRule(Schema, Schema) SchemaDataFix.introspectRule}
 *       and aggregates the field operations into a
 *       {@link FieldOperationReport}</li>
 * </ol>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>Bootstrap with multiple schema-aware fixes — fully introspectable</li>
 *   <li>Mixed bootstrap with one opaque fix — partially introspectable</li>
 *   <li>Multi-step version range — operations aggregated across steps</li>
 *   <li>Filtering and field path collection</li>
 * </ul>
 */
@DisplayName("Field Operation Analysis E2E")
@Tag("e2e")
class FieldOperationAnalysisE2E {

    private static final TypeReference PLAYER = new TypeReference("player");

    @Nested
    @DisplayName("Fully Introspectable Bootstrap")
    class FullyIntrospectableBootstrap {

        @Test
        @DisplayName("collects all field operations from a single SchemaDataFix")
        void collectsFromSingleFix() {
            final FieldOperationReport report = MigrationAnalyzer
                    .forBootstrap(new SingleFixBootstrap())
                    .from(100).to(200)
                    .analyzeFieldOperations();

            assertThat(report.isFullyIntrospectable()).isTrue();
            assertThat(report.opaqueFixCount()).isZero();
            assertThat(report.introspectedFixCount()).isEqualTo(1);
            assertThat(report.totalFieldOperationCount()).isEqualTo(3);

            assertThat(report.affectedFieldPaths())
                    .containsExactlyInAnyOrder("oldName", "deprecated", "health");
        }

        @Test
        @DisplayName("filters operations by type")
        void filtersOperationsByType() {
            final FieldOperationReport report = MigrationAnalyzer
                    .forBootstrap(new SingleFixBootstrap())
                    .from(100).to(200)
                    .analyzeFieldOperations();

            assertThat(report.operationsOfType(FieldOperationType.RENAME)).hasSize(1);
            assertThat(report.operationsOfType(FieldOperationType.REMOVE)).hasSize(1);
            assertThat(report.operationsOfType(FieldOperationType.ADD)).hasSize(1);
            assertThat(report.operationsOfType(FieldOperationType.MOVE)).isEmpty();
        }

        @Test
        @DisplayName("rename operation contains source and target field names")
        void renameOperationDetails() {
            final FieldOperationReport report = MigrationAnalyzer
                    .forBootstrap(new SingleFixBootstrap())
                    .from(100).to(200)
                    .analyzeFieldOperations();

            final var rename = report.operationsOfType(FieldOperationType.RENAME).get(0);
            assertThat(rename.fieldPathString()).isEqualTo("oldName");
            assertThat(rename.targetFieldName()).isEqualTo("newName");
        }
    }

    @Nested
    @DisplayName("Multi-Step Bootstrap")
    class MultiStepBootstrap {

        @Test
        @DisplayName("aggregates field operations across multiple fixes in version range")
        void aggregatesAcrossSteps() {
            final FieldOperationReport report = MigrationAnalyzer
                    .forBootstrap(new TwoStepBootstrap())
                    .from(100).to(200)
                    .analyzeFieldOperations();

            assertThat(report.introspectedFixCount()).isEqualTo(2);
            assertThat(report.totalFieldOperationCount()).isEqualTo(4);
            assertThat(report.affectedFieldPaths())
                    .containsExactlyInAnyOrder("oldName", "stale", "score", "level");
        }

        @Test
        @DisplayName("each fix entry retains its own version range")
        void fixEntriesRetainVersionRange() {
            final FieldOperationReport report = MigrationAnalyzer
                    .forBootstrap(new TwoStepBootstrap())
                    .from(100).to(200)
                    .analyzeFieldOperations();

            assertThat(report.fixOperations()).hasSize(2);

            final var byName = report.fixOperations().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            FixFieldOperations::fixName, f -> f));

            assertThat(byName.get("step_one").fromVersion().getVersion()).isEqualTo(100);
            assertThat(byName.get("step_one").toVersion().getVersion()).isEqualTo(150);
            assertThat(byName.get("step_two").fromVersion().getVersion()).isEqualTo(150);
            assertThat(byName.get("step_two").toVersion().getVersion()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Opaque Fix Detection")
    class OpaqueFixDetection {

        @Test
        @DisplayName("non-SchemaDataFix is recorded as opaque, not as a real failure")
        void opaqueFixCounted() {
            final FieldOperationReport report = MigrationAnalyzer
                    .forBootstrap(new MixedBootstrap())
                    .from(100).to(200)
                    .analyzeFieldOperations();

            assertThat(report.isFullyIntrospectable()).isFalse();
            assertThat(report.introspectedFixCount()).isEqualTo(1);
            assertThat(report.opaqueFixCount()).isEqualTo(1);

            // The introspectable fix's operations are still captured
            assertThat(report.totalFieldOperationCount()).isEqualTo(1);

            final var opaque = report.fixOperations().stream()
                    .filter(f -> !f.introspectable())
                    .findFirst()
                    .orElseThrow();
            assertThat(opaque.fixName()).isEqualTo("opaque_fix");
            assertThat(opaque.operations()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Test bootstrap implementations
    // -------------------------------------------------------------------------

    /**
     * Bootstrap with two schemas (v100, v200) and a single composed fix.
     */
    private static final class SingleFixBootstrap implements DataFixerBootstrap {

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
            registrar.register(PLAYER, new ComposedFix(this.capturedRegistry,
                    "single_composed", 100, 200));
        }
    }

    /**
     * Bootstrap with three schemas and two consecutive fixes covering separate
     * version ranges.
     */
    private static final class TwoStepBootstrap implements DataFixerBootstrap {

        private SchemaRegistry capturedRegistry;

        @Override
        public void registerSchemas(@NotNull final SchemaRegistry registry) {
            this.capturedRegistry = registry;
            registry.register(MockSchemas.builder(100)
                    .withType(PLAYER, DiagnosticsTestSupport.passthroughType(PLAYER)).build());
            registry.register(MockSchemas.builder(150)
                    .withType(PLAYER, DiagnosticsTestSupport.passthroughType(PLAYER)).build());
            registry.register(MockSchemas.builder(200)
                    .withType(PLAYER, DiagnosticsTestSupport.passthroughType(PLAYER)).build());
        }

        @Override
        public void registerFixes(@NotNull final FixRegistrar registrar) {
            registrar.register(PLAYER, new StepOneFix(this.capturedRegistry));
            registrar.register(PLAYER, new StepTwoFix(this.capturedRegistry));
        }
    }

    /**
     * Bootstrap that mixes a {@link SchemaDataFix} with a custom non-introspectable
     * fix. Used to verify that the analyzer marks the latter as opaque without
     * losing data from the former.
     */
    private static final class MixedBootstrap implements DataFixerBootstrap {

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
            registrar.register(PLAYER, new SimpleRenameFix(this.capturedRegistry,
                    "simple_rename", 100, 200));
            registrar.register(PLAYER, new OpaqueFix());
        }
    }

    // -------------------------------------------------------------------------
    // Test fix implementations
    // -------------------------------------------------------------------------

    /**
     * Reusable composed fix using rename + remove + add via {@code Rules.seq}.
     */
    private static final class ComposedFix extends SchemaDataFix {

        ComposedFix(@NotNull final SchemaRegistry schemas, @NotNull final String name,
                    final int fromVersion, final int toVersion) {
            super(name, new DataVersion(fromVersion), new DataVersion(toVersion), schemas);
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

    /**
     * Reusable single-rename fix.
     */
    private static final class SimpleRenameFix extends SchemaDataFix {

        SimpleRenameFix(@NotNull final SchemaRegistry schemas, @NotNull final String name,
                        final int fromVersion, final int toVersion) {
            super(name, new DataVersion(fromVersion), new DataVersion(toVersion), schemas);
        }

        @Override
        @NotNull
        protected TypeRewriteRule makeRule(@NotNull final Schema inputSchema,
                                           @NotNull final Schema outputSchema) {
            return Rules.renameField(GsonOps.INSTANCE, "oldName", "newName");
        }
    }

    /**
     * First step of a two-step migration: rename + remove.
     */
    private static final class StepOneFix extends SchemaDataFix {

        StepOneFix(@NotNull final SchemaRegistry schemas) {
            super("step_one", new DataVersion(100), new DataVersion(150), schemas);
        }

        @Override
        @NotNull
        protected TypeRewriteRule makeRule(@NotNull final Schema inputSchema,
                                           @NotNull final Schema outputSchema) {
            return Rules.seq(
                    Rules.renameField(GsonOps.INSTANCE, "oldName", "newName"),
                    Rules.removeField(GsonOps.INSTANCE, "stale")
            );
        }
    }

    /**
     * Second step of a two-step migration: add two new fields.
     */
    private static final class StepTwoFix extends SchemaDataFix {

        StepTwoFix(@NotNull final SchemaRegistry schemas) {
            super("step_two", new DataVersion(150), new DataVersion(200), schemas);
        }

        @Override
        @NotNull
        protected TypeRewriteRule makeRule(@NotNull final Schema inputSchema,
                                           @NotNull final Schema outputSchema) {
            return Rules.seq(
                    Rules.addField(GsonOps.INSTANCE, "score",
                            new Dynamic<>(GsonOps.INSTANCE, GsonOps.INSTANCE.createInt(0))),
                    Rules.addField(GsonOps.INSTANCE, "level",
                            new Dynamic<>(GsonOps.INSTANCE, GsonOps.INSTANCE.createInt(1)))
            );
        }
    }

    /**
     * Custom fix that does not extend {@link SchemaDataFix}. Should be marked
     * as opaque by the analyzer because its rule cannot be statically extracted.
     */
    private static final class OpaqueFix implements DataFix<Object> {

        @Override
        @NotNull
        public String name() {
            return "opaque_fix";
        }

        @Override
        @NotNull
        public DataVersion fromVersion() {
            return new DataVersion(100);
        }

        @Override
        @NotNull
        public DataVersion toVersion() {
            return new DataVersion(200);
        }

        @Override
        @NotNull
        public Dynamic<Object> apply(@NotNull final TypeReference type,
                                     @NotNull final Dynamic<Object> input,
                                     @NotNull final DataFixerContext context) {
            return input;
        }
    }
}
