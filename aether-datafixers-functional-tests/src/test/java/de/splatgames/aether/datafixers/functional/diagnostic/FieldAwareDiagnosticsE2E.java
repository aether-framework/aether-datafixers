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
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the field-aware diagnostics system.
 *
 * <p>These tests exercise the full diagnostic capture pipeline: a real
 * {@link SchemaDataFix} subclass with field-aware {@link Rules}, executed via
 * {@link DataFixer#update(TypeReference,
 * de.splatgames.aether.datafixers.api.dynamic.Dynamic,
 * DataVersion, DataVersion,
 * de.splatgames.aether.datafixers.api.fix.DataFixerContext)} with a
 * {@link DiagnosticContext}, then asserting the {@link MigrationReport}
 * contains the expected
 * {@link de.splatgames.aether.datafixers.api.diagnostic.FieldOperation field operations}.</p>
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>Single fix with single rule</li>
 *   <li>Single fix with composed {@code Rules.seq()}</li>
 *   <li>Multi-step migration aggregating across fixes</li>
 *   <li>Field operation order and type filtering</li>
 *   <li>Affected field path collection</li>
 * </ul>
 */
@DisplayName("Field-Aware Diagnostics E2E")
@Tag("e2e")
class FieldAwareDiagnosticsE2E {

    private static final TypeReference PLAYER = new TypeReference("player");

    /**
     * Builds a minimal schema chain (v100, v150, v200) sharing the same string
     * type for the player reference. The schemas need not differ structurally
     * because the field operations are introspected from the rules, not derived
     * from a schema diff.
     *
     * @return a populated schema registry
     */
    @NotNull
    private static SchemaRegistry buildSchemas() {
        final Schema v100 = MockSchemas.builder(100)
                .withType(PLAYER, DiagnosticsTestSupport.passthroughType(PLAYER)).build();
        final Schema v150 = MockSchemas.builder(150).withParent(v100)
                .withType(PLAYER, DiagnosticsTestSupport.passthroughType(PLAYER)).build();
        final Schema v200 = MockSchemas.builder(200).withParent(v150)
                .withType(PLAYER, DiagnosticsTestSupport.passthroughType(PLAYER)).build();
        return MockSchemas.chain(v100, v150, v200);
    }

    @Nested
    @DisplayName("Single Fix Capture")
    class SingleFixCapture {

        @Test
        @DisplayName("captures rename operation from a single rule")
        void capturesRenameFromSingleRule() {
            final SchemaRegistry schemas = buildSchemas();
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(200))
                    .addFix(PLAYER, new RenameOnlyFix(schemas))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("oldName", "Alice")
                    .build();

            final DiagnosticContext ctx = DiagnosticContext.create(
                    DiagnosticOptions.builder()
                            .captureSnapshots(false)
                            .captureRuleDetails(true)
                            .captureFieldDetails(true)
                            .build());

            fixer.update(PLAYER, input, new DataVersion(100), new DataVersion(150), ctx);

            final MigrationReport report = ctx.getReport();
            assertThat(report.fixCount()).isEqualTo(1);
            assertThat(report.totalFieldOperationCount()).isEqualTo(1);
            assertThat(report.fixExecutions().get(0).fixName()).isEqualTo("rename_only");

            final var op = report.fixExecutions().get(0).allFieldOperations().get(0);
            assertThat(op.operationType()).isEqualTo(FieldOperationType.RENAME);
            assertThat(op.fieldPathString()).isEqualTo("oldName");
            assertThat(op.targetFieldName()).isEqualTo("newName");
        }

        @Test
        @DisplayName("captures composed seq of rename + remove + add")
        void capturesSeqComposition() {
            final SchemaRegistry schemas = buildSchemas();
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(200))
                    .addFix(PLAYER, new ComposedFix(schemas))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("oldName", "Bob")
                    .put("deprecated", "drop me")
                    .build();

            final DiagnosticContext ctx = DiagnosticContext.create();

            fixer.update(PLAYER, input, new DataVersion(100), new DataVersion(150), ctx);

            final MigrationReport report = ctx.getReport();
            assertThat(report.totalFieldOperationCount()).isEqualTo(3);

            final var ops = report.fixExecutions().get(0).allFieldOperations();
            assertThat(ops).extracting(o -> o.operationType())
                    .containsExactlyInAnyOrder(
                            FieldOperationType.RENAME,
                            FieldOperationType.REMOVE,
                            FieldOperationType.ADD);
        }
    }

    @Nested
    @DisplayName("Multi-Step Migration Capture")
    class MultiStepMigrationCapture {

        @Test
        @DisplayName("aggregates field operations across two consecutive fixes")
        void aggregatesAcrossTwoFixes() {
            final SchemaRegistry schemas = buildSchemas();
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(200))
                    .addFix(PLAYER, new RenameOnlyFix(schemas))
                    .addFix(PLAYER, new RemoveAndAddFix(schemas))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("oldName", "Carol")
                    .put("deprecated", "old")
                    .build();

            final DiagnosticContext ctx = DiagnosticContext.create();

            fixer.update(PLAYER, input, new DataVersion(100), new DataVersion(200), ctx);

            final MigrationReport report = ctx.getReport();
            assertThat(report.fixCount()).isEqualTo(2);
            assertThat(report.totalFieldOperationCount()).isEqualTo(3);

            // Operations should be in fix-application order
            assertThat(report.fixExecutions())
                    .extracting(f -> f.fixName())
                    .containsExactly("rename_only", "remove_and_add");
        }

        @Test
        @DisplayName("affected field paths collected from all fixes")
        void affectedFieldPathsCollected() {
            final SchemaRegistry schemas = buildSchemas();
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(200))
                    .addFix(PLAYER, new RenameOnlyFix(schemas))
                    .addFix(PLAYER, new RemoveAndAddFix(schemas))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("oldName", "Dave")
                    .put("deprecated", "x")
                    .build();

            final DiagnosticContext ctx = DiagnosticContext.create();
            fixer.update(PLAYER, input, new DataVersion(100), new DataVersion(200), ctx);

            final MigrationReport report = ctx.getReport();
            final var allOps = report.fixExecutions().stream()
                    .flatMap(f -> f.allFieldOperations().stream())
                    .toList();

            assertThat(allOps).extracting(o -> o.fieldPathString())
                    .containsExactlyInAnyOrder("oldName", "deprecated", "health");
        }
    }

    @Nested
    @DisplayName("Filter and Aggregation")
    class FilterAndAggregation {

        @Test
        @DisplayName("operations of a specific type can be filtered")
        void operationsOfTypeFiltered() {
            final SchemaRegistry schemas = buildSchemas();
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(200))
                    .addFix(PLAYER, new ComposedFix(schemas))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("oldName", "Eve")
                    .put("deprecated", "stale")
                    .build();

            final DiagnosticContext ctx = DiagnosticContext.create();
            fixer.update(PLAYER, input, new DataVersion(100), new DataVersion(150), ctx);

            final MigrationReport report = ctx.getReport();
            final var renames = report.fixExecutions().get(0).allFieldOperations().stream()
                    .filter(op -> op.operationType() == FieldOperationType.RENAME)
                    .toList();

            assertThat(renames).hasSize(1);
            assertThat(renames.get(0).fieldPathString()).isEqualTo("oldName");
            assertThat(renames.get(0).targetFieldName()).isEqualTo("newName");
        }

        @Test
        @DisplayName("rule application count reflects each rule executed")
        void ruleApplicationCount() {
            final SchemaRegistry schemas = buildSchemas();
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(200))
                    .addFix(PLAYER, new ComposedFix(schemas))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("oldName", "Frank")
                    .put("deprecated", "x")
                    .build();

            final DiagnosticContext ctx = DiagnosticContext.create();
            fixer.update(PLAYER, input, new DataVersion(100), new DataVersion(150), ctx);

            final MigrationReport report = ctx.getReport();
            // The composed fix wraps a single seq() rule, which is one rule application.
            // The seq itself aggregates the field operations of its children.
            assertThat(report.ruleApplicationCount()).isGreaterThanOrEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // Test fix implementations
    // -------------------------------------------------------------------------

    /**
     * Fix that performs a single field rename via {@link Rules#renameField}.
     */
    private static final class RenameOnlyFix extends SchemaDataFix {

        RenameOnlyFix(@NotNull final SchemaRegistry schemas) {
            super("rename_only", new DataVersion(100), new DataVersion(150), schemas);
        }

        @Override
        @NotNull
        protected TypeRewriteRule makeRule(@NotNull final Schema inputSchema,
                                           @NotNull final Schema outputSchema) {
            return Rules.renameField(GsonOps.INSTANCE, "oldName", "newName");
        }
    }

    /**
     * Fix that composes rename, remove, and add via {@link Rules#seq}.
     */
    private static final class ComposedFix extends SchemaDataFix {

        ComposedFix(@NotNull final SchemaRegistry schemas) {
            super("composed", new DataVersion(100), new DataVersion(150), schemas);
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
     * Fix that performs a remove then an add (chained via seq), used as a
     * second step in multi-fix migrations.
     */
    private static final class RemoveAndAddFix extends SchemaDataFix {

        RemoveAndAddFix(@NotNull final SchemaRegistry schemas) {
            super("remove_and_add", new DataVersion(150), new DataVersion(200), schemas);
        }

        @Override
        @NotNull
        protected TypeRewriteRule makeRule(@NotNull final Schema inputSchema,
                                           @NotNull final Schema outputSchema) {
            return Rules.seq(
                    Rules.removeField(GsonOps.INSTANCE, "deprecated"),
                    Rules.addField(GsonOps.INSTANCE, "health",
                            new Dynamic<>(GsonOps.INSTANCE, GsonOps.INSTANCE.createInt(100)))
            );
        }
    }
}
