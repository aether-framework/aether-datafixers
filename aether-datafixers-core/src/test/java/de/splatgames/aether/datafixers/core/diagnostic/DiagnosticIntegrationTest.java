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

package de.splatgames.aether.datafixers.core.diagnostic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.FixExecution;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Migration Diagnostics feature.
 */
@DisplayName("Migration Diagnostics Integration")
class DiagnosticIntegrationTest {

    private static final TypeReference PLAYER = new TypeReference("player");

    private DataFixerBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DataFixerBuilder(new DataVersion(5));
    }

    @Nested
    @DisplayName("Full Migration with Diagnostics")
    class FullMigrationWithDiagnostics {

        @Test
        @DisplayName("captures complete migration report")
        void capturesCompleteMigrationReport() {
            // Setup fixes
            DataFix<JsonElement> renameFix = createRenameFix("rename_player_name", 1, 2);
            DataFix<JsonElement> addFieldFix = createAddFieldFix("add_health", 2, 3, "health", 100);

            DataFixer fixer = builder
                    .addFix(PLAYER, renameFix)
                    .addFix(PLAYER, addFieldFix)
                    .build();

            // Create input
            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("playerName", "Alice");
            inputObj.addProperty("level", 10);
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            // Create diagnostic context
            DiagnosticContext context = DiagnosticContext.create();

            // Run migration
            Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(3),
                    context
            );

            // Verify result
            assertThat(result.get("name").asString().result()).contains("Alice");
            assertThat(result.get("health").asInt().result()).contains(100);
            assertThat(result.get("playerName")).isNull();

            // Verify report
            MigrationReport report = context.getReport();

            assertThat(report.type()).isEqualTo(PLAYER);
            assertThat(report.fromVersion()).isEqualTo(new DataVersion(1));
            assertThat(report.toVersion()).isEqualTo(new DataVersion(3));
            assertThat(report.fixCount()).isEqualTo(2);
            assertThat(report.totalDuration().toMillis()).isGreaterThanOrEqualTo(0);

            // Verify fix executions
            assertThat(report.fixExecutions()).hasSize(2);
            assertThat(report.fixExecutions().get(0).fixName()).isEqualTo("rename_player_name");
            assertThat(report.fixExecutions().get(1).fixName()).isEqualTo("add_health");
        }

        @Test
        @DisplayName("captures snapshots when enabled")
        void capturesSnapshotsWhenEnabled() {
            DataFix<JsonElement> fix = createRenameFix("rename_fix", 1, 2);
            DataFixer fixer = builder.addFix(PLAYER, fix).build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("playerName", "Bob");
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            DiagnosticContext context = DiagnosticContext.create(
                    DiagnosticOptions.builder()
                            .captureSnapshots(true)
                            .build()
            );

            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(2), context);

            MigrationReport report = context.getReport();

            assertThat(report.inputSnapshot()).isPresent();
            assertThat(report.inputSnapshot().get()).contains("playerName");
            assertThat(report.inputSnapshot().get()).contains("Bob");

            assertThat(report.outputSnapshot()).isPresent();
            assertThat(report.outputSnapshot().get()).contains("name");
            assertThat(report.outputSnapshot().get()).contains("Bob");
        }

        @Test
        @DisplayName("does not capture snapshots when disabled")
        void doesNotCaptureSnapshotsWhenDisabled() {
            DataFix<JsonElement> fix = createRenameFix("rename_fix", 1, 2);
            DataFixer fixer = builder.addFix(PLAYER, fix).build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("playerName", "Charlie");
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            DiagnosticContext context = DiagnosticContext.create(
                    DiagnosticOptions.builder()
                            .captureSnapshots(false)
                            .build()
            );

            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(2), context);

            MigrationReport report = context.getReport();

            assertThat(report.inputSnapshot()).isEmpty();
            assertThat(report.outputSnapshot()).isEmpty();
        }

        @Test
        @DisplayName("captures warnings in report")
        void capturesWarningsInReport() {
            DataFix<JsonElement> warningFix = new DataFix<>() {
                @Override
                public @NotNull String name() {
                    return "warning_fix";
                }

                @Override
                public @NotNull DataVersion fromVersion() {
                    return new DataVersion(1);
                }

                @Override
                public @NotNull DataVersion toVersion() {
                    return new DataVersion(2);
                }

                @Override
                public @NotNull Dynamic<JsonElement> apply(
                        @NotNull TypeReference type,
                        @NotNull Dynamic<JsonElement> input,
                        @NotNull DataFixerContext context
                ) {
                    context.warn("Missing optional field: score");
                    return input;
                }
            };

            DataFixer fixer = builder.addFix(PLAYER, warningFix).build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("name", "Dave");
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            DiagnosticContext context = DiagnosticContext.create();
            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(2), context);

            MigrationReport report = context.getReport();

            assertThat(report.hasWarnings()).isTrue();
            assertThat(report.warnings()).contains("Missing optional field: score");
        }

        @Test
        @DisplayName("toSummary() provides useful overview")
        void toSummaryProvidesUsefulOverview() {
            DataFix<JsonElement> fix1 = createRenameFix("fix1", 1, 2);
            DataFix<JsonElement> fix2 = createAddFieldFix("fix2", 2, 3, "level", 1);
            DataFixer fixer = builder.addFix(PLAYER, fix1).addFix(PLAYER, fix2).build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("playerName", "Eve");
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            DiagnosticContext context = DiagnosticContext.create();
            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(3), context);

            String summary = context.getReport().toSummary();

            assertThat(summary).contains("player");
            assertThat(summary).contains("v1");
            assertThat(summary).contains("v3");
            assertThat(summary).contains("2 fixes");
        }
    }

    @Nested
    @DisplayName("No Diagnostics (Default Context)")
    class NoDiagnostics {

        @Test
        @DisplayName("migration works without diagnostic context")
        void migrationWorksWithoutDiagnosticContext() {
            DataFix<JsonElement> fix = createRenameFix("fix", 1, 2);
            DataFixer fixer = builder.addFix(PLAYER, fix).build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("playerName", "Frank");
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            // Use default context (no diagnostics)
            Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result.get("name").asString().result()).contains("Frank");
            assertThat(result.get("playerName")).isNull();
        }
    }

    @Nested
    @DisplayName("Snapshot Truncation")
    class SnapshotTruncation {

        @Test
        @DisplayName("truncates long snapshots")
        void truncatesLongSnapshots() {
            DataFix<JsonElement> fix = new DataFix<>() {
                @Override
                public @NotNull String name() {
                    return "pass_through";
                }

                @Override
                public @NotNull DataVersion fromVersion() {
                    return new DataVersion(1);
                }

                @Override
                public @NotNull DataVersion toVersion() {
                    return new DataVersion(2);
                }

                @Override
                public @NotNull Dynamic<JsonElement> apply(
                        @NotNull TypeReference type,
                        @NotNull Dynamic<JsonElement> input,
                        @NotNull DataFixerContext context
                ) {
                    return input;
                }
            };

            DataFixer fixer = builder.addFix(PLAYER, fix).build();

            // Create large input
            JsonObject inputObj = new JsonObject();
            StringBuilder largeValue = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeValue.append("very_long_value_");
            }
            inputObj.addProperty("data", largeValue.toString());
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            DiagnosticContext context = DiagnosticContext.create(
                    DiagnosticOptions.builder()
                            .captureSnapshots(true)
                            .maxSnapshotLength(100)
                            .build()
            );

            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(2), context);

            MigrationReport report = context.getReport();

            assertThat(report.inputSnapshot()).isPresent();
            assertThat(report.inputSnapshot().get().length()).isLessThanOrEqualTo(120); // 100 + "... (truncated)"
            assertThat(report.inputSnapshot().get()).endsWith("... (truncated)");
        }
    }

    // Helper methods to create test fixes

    private DataFix<JsonElement> createRenameFix(String name, int from, int to) {
        return new DataFix<>() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull DataVersion fromVersion() {
                return new DataVersion(from);
            }

            @Override
            public @NotNull DataVersion toVersion() {
                return new DataVersion(to);
            }

            @Override
            public @NotNull Dynamic<JsonElement> apply(
                    @NotNull TypeReference type,
                    @NotNull Dynamic<JsonElement> input,
                    @NotNull DataFixerContext context
            ) {
                Dynamic<JsonElement> value = input.get("playerName");
                if (value != null) {
                    return input.remove("playerName").set("name", value);
                }
                return input;
            }
        };
    }

    private DataFix<JsonElement> createAddFieldFix(String name, int from, int to, String field, int value) {
        return new DataFix<>() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull DataVersion fromVersion() {
                return new DataVersion(from);
            }

            @Override
            public @NotNull DataVersion toVersion() {
                return new DataVersion(to);
            }

            @Override
            public @NotNull Dynamic<JsonElement> apply(
                    @NotNull TypeReference type,
                    @NotNull Dynamic<JsonElement> input,
                    @NotNull DataFixerContext context
            ) {
                if (input.get(field) == null) {
                    return input.set(field, input.createInt(value));
                }
                return input;
            }
        };
    }
}
