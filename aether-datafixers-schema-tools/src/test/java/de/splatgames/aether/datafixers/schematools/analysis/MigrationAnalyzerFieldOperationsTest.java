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

package de.splatgames.aether.datafixers.schematools.analysis;

import com.google.gson.JsonElement;
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
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jetbrains.annotations.NotNull;

/**
 * Integration tests for {@link MigrationAnalyzer#analyzeFieldOperations()}.
 *
 * <p>These tests use real {@link SchemaDataFix} subclasses with field-aware
 * rules to verify the end-to-end static introspection flow.</p>
 */
@DisplayName("MigrationAnalyzer.analyzeFieldOperations()")
class MigrationAnalyzerFieldOperationsTest {

    private static final TypeReference PLAYER = new TypeReference("player");

    @Nested
    @DisplayName("Introspectable Fixes")
    class IntrospectableFixes {

        @Test
        @DisplayName("captures field operations from a single SchemaDataFix")
        void capturesSingleFix() {
            final DataFixerBootstrap bootstrap = bootstrapWithRenameAndRemoveFix();

            final FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyzeFieldOperations();

            org.assertj.core.api.Assertions.assertThat(report.introspectedFixCount()).isEqualTo(1);
            org.assertj.core.api.Assertions.assertThat(report.opaqueFixCount()).isZero();
            org.assertj.core.api.Assertions.assertThat(report.totalFieldOperationCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("aggregates field operations from seq composition")
        void aggregatesSeqComposition() {
            final DataFixerBootstrap bootstrap = bootstrapWithRenameAndRemoveFix();

            final FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyzeFieldOperations();

            org.assertj.core.api.Assertions.assertThat(report.affectedFieldPaths())
                    .containsExactlyInAnyOrder("oldName", "deprecated");
            org.assertj.core.api.Assertions.assertThat(
                    report.operationsOfType(FieldOperationType.RENAME)).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(
                    report.operationsOfType(FieldOperationType.REMOVE)).hasSize(1);
        }

        @Test
        @DisplayName("report is fully introspectable when all fixes are SchemaDataFix")
        void reportIsFullyIntrospectable() {
            final DataFixerBootstrap bootstrap = bootstrapWithRenameAndRemoveFix();

            final FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyzeFieldOperations();

            org.assertj.core.api.Assertions.assertThat(report.isFullyIntrospectable()).isTrue();
        }

        @Test
        @DisplayName("captures fix metadata in report entries")
        void capturesFixMetadata() {
            final DataFixerBootstrap bootstrap = bootstrapWithRenameAndRemoveFix();

            final FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyzeFieldOperations();

            org.assertj.core.api.Assertions.assertThat(report.fixOperations()).hasSize(1);
            final FixFieldOperations entry = report.fixOperations().get(0);
            org.assertj.core.api.Assertions.assertThat(entry.fixName()).isEqualTo("rename_and_remove");
            org.assertj.core.api.Assertions.assertThat(entry.fromVersion().getVersion()).isEqualTo(100);
            org.assertj.core.api.Assertions.assertThat(entry.toVersion().getVersion()).isEqualTo(200);
            org.assertj.core.api.Assertions.assertThat(entry.introspectable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Opaque Fixes")
    class OpaqueFixes {

        @Test
        @DisplayName("non-SchemaDataFix is recorded as opaque")
        void nonSchemaDataFixIsOpaque() {
            final DataFixerBootstrap bootstrap = bootstrapWithCustomFix();

            final FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyzeFieldOperations();

            org.assertj.core.api.Assertions.assertThat(report.opaqueFixCount()).isEqualTo(1);
            org.assertj.core.api.Assertions.assertThat(report.introspectedFixCount()).isZero();
            org.assertj.core.api.Assertions.assertThat(report.totalFieldOperationCount()).isZero();
            org.assertj.core.api.Assertions.assertThat(report.isFullyIntrospectable()).isFalse();
        }

        @Test
        @DisplayName("opaque entry has fix name preserved")
        void opaqueEntryPreservesName() {
            final DataFixerBootstrap bootstrap = bootstrapWithCustomFix();

            final FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyzeFieldOperations();

            org.assertj.core.api.Assertions.assertThat(report.fixOperations()).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(report.fixOperations().get(0).fixName())
                    .isEqualTo("custom_opaque_fix");
            org.assertj.core.api.Assertions.assertThat(report.fixOperations().get(0).introspectable())
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Empty Cases")
    class EmptyCases {

        @Test
        @DisplayName("empty version range produces empty report")
        void emptyVersionRange() {
            final DataFixerBootstrap bootstrap = new DataFixerBootstrap() {
                @Override
                public void registerSchemas(@NotNull final SchemaRegistry registry) {
                    registry.register(MockSchemas.builder(100).withType(PLAYER, Type.STRING).build());
                }

                @Override
                public void registerFixes(@NotNull final FixRegistrar registrar) {
                }
            };

            final FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(100)
                    .analyzeFieldOperations();

            org.assertj.core.api.Assertions.assertThat(report.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("schemas without fixes produces empty report")
        void schemasWithoutFixes() {
            final DataFixerBootstrap bootstrap = new DataFixerBootstrap() {
                @Override
                public void registerSchemas(@NotNull final SchemaRegistry registry) {
                    registry.register(MockSchemas.builder(100).withType(PLAYER, Type.STRING).build());
                    registry.register(MockSchemas.builder(200).withType(PLAYER, Type.STRING).build());
                }

                @Override
                public void registerFixes(@NotNull final FixRegistrar registrar) {
                }
            };

            final FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyzeFieldOperations();

            org.assertj.core.api.Assertions.assertThat(report.isEmpty()).isTrue();
            org.assertj.core.api.Assertions.assertThat(report.isFullyIntrospectable()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Bootstrap helpers
    // -------------------------------------------------------------------------

    private static DataFixerBootstrap bootstrapWithRenameAndRemoveFix() {
        return new RenameAndRemoveBootstrap();
    }

    private static DataFixerBootstrap bootstrapWithCustomFix() {
        return new CustomFixBootstrap();
    }

    /**
     * Bootstrap with two schema versions and one {@link SchemaDataFix} that
     * combines a rename and a remove via {@code Rules.seq()}.
     */
    private static final class RenameAndRemoveBootstrap implements DataFixerBootstrap {

        private SchemaRegistry capturedRegistry;

        @Override
        public void registerSchemas(@NotNull final SchemaRegistry registry) {
            this.capturedRegistry = registry;
            final Schema v100 = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();
            final Schema v200 = MockSchemas.builder(200)
                    .withParent(v100)
                    .withType(PLAYER, Type.STRING)
                    .build();
            registry.register(v100);
            registry.register(v200);
        }

        @Override
        public void registerFixes(@NotNull final FixRegistrar registrar) {
            registrar.register(PLAYER, new RenameAndRemoveFix(this.capturedRegistry));
        }
    }

    /**
     * Bootstrap with two schema versions and one custom (non-{@link SchemaDataFix})
     * fix.
     */
    private static final class CustomFixBootstrap implements DataFixerBootstrap {

        @Override
        public void registerSchemas(@NotNull final SchemaRegistry registry) {
            final Schema v100 = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();
            final Schema v200 = MockSchemas.builder(200)
                    .withParent(v100)
                    .withType(PLAYER, Type.STRING)
                    .build();
            registry.register(v100);
            registry.register(v200);
        }

        @Override
        public void registerFixes(@NotNull final FixRegistrar registrar) {
            registrar.register(PLAYER, new CustomOpaqueFix());
        }
    }

    /**
     * Test fix that uses field-aware rules ({@code renameField} and {@code removeField})
     * combined via {@code seq()}. Used to verify that the analyzer correctly extracts
     * field operations from compositions.
     */
    private static final class RenameAndRemoveFix extends SchemaDataFix {

        RenameAndRemoveFix(@NotNull final SchemaRegistry schemas) {
            super("rename_and_remove",
                    new DataVersion(100),
                    new DataVersion(200),
                    schemas);
        }

        @Override
        @NotNull
        protected TypeRewriteRule makeRule(@NotNull final Schema inputSchema,
                                           @NotNull final Schema outputSchema) {
            return Rules.seq(
                    Rules.renameField(GsonOps.INSTANCE, "oldName", "newName"),
                    Rules.removeField(GsonOps.INSTANCE, "deprecated")
            );
        }
    }

    /**
     * Test fix that does not extend {@link SchemaDataFix}. Used to verify the
     * analyzer correctly marks such fixes as opaque.
     */
    private static final class CustomOpaqueFix implements DataFix<Object> {

        @Override
        @NotNull
        public String name() {
            return "custom_opaque_fix";
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
