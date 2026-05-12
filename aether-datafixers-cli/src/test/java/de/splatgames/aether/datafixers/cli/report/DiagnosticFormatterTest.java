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

package de.splatgames.aether.datafixers.cli.report;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import de.splatgames.aether.datafixers.api.diagnostic.RuleApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the {@code formatDiagnostic} method in {@link TextReportFormatter}
 * and {@link JsonReportFormatter}.
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
@DisplayName("Diagnostic Report Formatting")
class DiagnosticFormatterTest {

    /**
     * Creates a minimal diagnostic report for testing using the DiagnosticContext builder.
     *
     * @param fieldOps the field operations to include in the rule application
     * @return a migration report with one fix and one rule
     */
    private static MigrationReport createTestReport(final List<FieldOperation> fieldOps) {
        final DiagnosticContext ctx = DiagnosticContext.create(
                DiagnosticOptions.builder()
                        .captureSnapshots(false)
                        .captureRuleDetails(true)
                        .captureFieldDetails(true)
                        .build()
        );

        final TypeReference type = new TypeReference("player");
        final DataVersion from = new DataVersion(100);
        final DataVersion to = new DataVersion(200);

        final MigrationReport.Builder builder = ctx.reportBuilder();
        builder.startMigration(type, from, to);

        // Simulate a fix with a single rule application
        final de.splatgames.aether.datafixers.api.fix.DataFix<?> fakeFix =
                new FakeDataFix("TestFix", from, to);

        builder.startFix(fakeFix);
        builder.recordRuleApplication(new RuleApplication(
                "renameField", "player", Instant.now(),
                Duration.ofMillis(5), true, null, fieldOps
        ));
        builder.endFix(fakeFix, Duration.ofMillis(20), null);

        return builder.build();
    }

    /**
     * Minimal DataFix implementation for test purposes.
     */
    private static final class FakeDataFix implements de.splatgames.aether.datafixers.api.fix.DataFix<Object> {

        private final String name;
        private final DataVersion fromVersion;
        private final DataVersion toVersion;

        FakeDataFix(final String name, final DataVersion from, final DataVersion to) {
            this.name = name;
            this.fromVersion = from;
            this.toVersion = to;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public DataVersion fromVersion() {
            return this.fromVersion;
        }

        @Override
        public DataVersion toVersion() {
            return this.toVersion;
        }

        @Override
        public de.splatgames.aether.datafixers.api.dynamic.Dynamic<Object> apply(
                final de.splatgames.aether.datafixers.api.TypeReference type,
                final de.splatgames.aether.datafixers.api.dynamic.Dynamic<Object> input,
                final de.splatgames.aether.datafixers.api.fix.DataFixerContext context
        ) {
            return input;
        }
    }

    @Nested
    @DisplayName("TextReportFormatter.formatDiagnostic()")
    class TextFormat {

        private final TextReportFormatter formatter = new TextReportFormatter();

        @Test
        @DisplayName("includes diagnostic report header")
        void includesHeader() {
            final MigrationReport report = createTestReport(List.of(
                    FieldOperation.rename("oldName", "newName")));

            final String result = formatter.formatDiagnostic("player.json", "player", report);

            assertThat(result).contains("Diagnostic Report:");
            assertThat(result).contains("player.json");
            assertThat(result).contains("[player]");
            assertThat(result).contains("v100");
            assertThat(result).contains("v200");
        }

        @Test
        @DisplayName("includes fix details")
        void includesFixDetails() {
            final MigrationReport report = createTestReport(List.of(
                    FieldOperation.rename("oldName", "newName")));

            final String result = formatter.formatDiagnostic("player.json", "player", report);

            assertThat(result).contains("Fix: TestFix");
            assertThat(result).contains("Rule: renameField");
            assertThat(result).contains("[matched]");
        }

        @Test
        @DisplayName("includes field operation summaries")
        void includesFieldOperations() {
            final MigrationReport report = createTestReport(List.of(
                    FieldOperation.rename("oldName", "newName"),
                    FieldOperation.remove("deprecated"),
                    FieldOperation.add("health")
            ));

            final String result = formatter.formatDiagnostic("player.json", "player", report);

            assertThat(result).contains("RENAME(oldName -> newName)");
            assertThat(result).contains("REMOVE(deprecated)");
            assertThat(result).contains("ADD(health)");
        }

        @Test
        @DisplayName("includes counts summary")
        void includesCountsSummary() {
            final MigrationReport report = createTestReport(List.of(
                    FieldOperation.rename("a", "b"),
                    FieldOperation.remove("c")
            ));

            final String result = formatter.formatDiagnostic("player.json", "player", report);

            assertThat(result).contains("Fixes applied: 1");
            assertThat(result).contains("Rules: 1");
            assertThat(result).contains("Field operations: 2");
        }

        @Test
        @DisplayName("handles empty field operations")
        void handlesEmptyFieldOps() {
            final MigrationReport report = createTestReport(List.of());

            final String result = formatter.formatDiagnostic("player.json", "player", report);

            assertThat(result).contains("Field operations: 0");
        }

        @Test
        @DisplayName("rejects null arguments")
        void rejectsNullArguments() {
            final MigrationReport report = createTestReport(List.of());

            assertThatThrownBy(() -> formatter.formatDiagnostic(null, "player", report))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> formatter.formatDiagnostic("file", null, report))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> formatter.formatDiagnostic("file", "player", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("JsonReportFormatter.formatDiagnostic()")
    class JsonFormat {

        private final JsonReportFormatter formatter = new JsonReportFormatter();

        @Test
        @DisplayName("produces valid JSON")
        void producesValidJson() {
            final MigrationReport report = createTestReport(List.of(
                    FieldOperation.rename("oldName", "newName")));

            final String result = formatter.formatDiagnostic("player.json", "player", report);

            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            assertThat(json).isNotNull();
        }

        @Test
        @DisplayName("includes all top-level fields")
        void includesTopLevelFields() {
            final MigrationReport report = createTestReport(List.of(
                    FieldOperation.rename("oldName", "newName")));

            final String result = formatter.formatDiagnostic("player.json", "player", report);
            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();

            assertThat(json.get("file").getAsString()).isEqualTo("player.json");
            assertThat(json.get("type").getAsString()).isEqualTo("player");
            assertThat(json.get("fromVersion").getAsInt()).isEqualTo(100);
            assertThat(json.get("toVersion").getAsInt()).isEqualTo(200);
            assertThat(json.get("fixCount").getAsInt()).isEqualTo(1);
            assertThat(json.get("ruleCount").getAsInt()).isEqualTo(1);
            assertThat(json.get("fieldOperationCount").getAsInt()).isEqualTo(1);
        }

        @Test
        @DisplayName("includes field operations in rules")
        void includesFieldOperations() {
            final MigrationReport report = createTestReport(List.of(
                    FieldOperation.rename("oldName", "newName"),
                    FieldOperation.remove("deprecated")
            ));

            final String result = formatter.formatDiagnostic("player.json", "player", report);
            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();

            final var fixes = json.getAsJsonArray("fixes");
            assertThat(fixes).hasSize(1);

            final var rules = fixes.get(0).getAsJsonObject().getAsJsonArray("rules");
            assertThat(rules).hasSize(1);

            final var fieldOps = rules.get(0).getAsJsonObject().getAsJsonArray("fieldOperations");
            assertThat(fieldOps).hasSize(2);

            final var renameOp = fieldOps.get(0).getAsJsonObject();
            assertThat(renameOp.get("type").getAsString()).isEqualTo("RENAME");
            assertThat(renameOp.get("field").getAsString()).isEqualTo("oldName");
            assertThat(renameOp.get("target").getAsString()).isEqualTo("newName");

            final var removeOp = fieldOps.get(1).getAsJsonObject();
            assertThat(removeOp.get("type").getAsString()).isEqualTo("REMOVE");
            assertThat(removeOp.get("field").getAsString()).isEqualTo("deprecated");
        }

        @Test
        @DisplayName("includes warnings array")
        void includesWarnings() {
            final MigrationReport report = createTestReport(List.of());

            final String result = formatter.formatDiagnostic("player.json", "player", report);
            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();

            assertThat(json.getAsJsonArray("warnings")).isNotNull();
        }

        @Test
        @DisplayName("rejects null arguments")
        void rejectsNullArguments() {
            final MigrationReport report = createTestReport(List.of());

            assertThatThrownBy(() -> formatter.formatDiagnostic(null, "player", report))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> formatter.formatDiagnostic("file", null, report))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> formatter.formatDiagnostic("file", "player", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
