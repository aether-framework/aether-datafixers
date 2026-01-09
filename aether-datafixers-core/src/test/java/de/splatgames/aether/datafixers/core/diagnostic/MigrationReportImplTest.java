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
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import de.splatgames.aether.datafixers.api.diagnostic.RuleApplication;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MigrationReportImpl}.
 */
@DisplayName("MigrationReportImpl")
class MigrationReportImplTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference WORLD = new TypeReference("world");

    private MigrationReportImpl.BuilderImpl builder;

    @BeforeEach
    void setUp() {
        builder = MigrationReportImpl.builder();
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("startMigration() sets metadata")
        void startMigrationSetsMetadata() {
            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(5));
            MigrationReport report = builder.build();

            assertThat(report.type()).isEqualTo(PLAYER);
            assertThat(report.fromVersion()).isEqualTo(new DataVersion(1));
            assertThat(report.toVersion()).isEqualTo(new DataVersion(5));
            assertThat(report.startTime()).isNotNull();
        }

        @Test
        @DisplayName("build() throws if migration not started")
        void buildThrowsIfMigrationNotStarted() {
            assertThatThrownBy(() -> builder.build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not started");
        }

        @Test
        @DisplayName("setInputSnapshot() captures input")
        void setInputSnapshotCapturesInput() {
            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            builder.setInputSnapshot("{\"name\":\"Alice\"}");
            MigrationReport report = builder.build();

            assertThat(report.inputSnapshot()).contains("{\"name\":\"Alice\"}");
        }

        @Test
        @DisplayName("setOutputSnapshot() captures output")
        void setOutputSnapshotCapturesOutput() {
            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            builder.setOutputSnapshot("{\"name\":\"Alice\",\"version\":2}");
            MigrationReport report = builder.build();

            assertThat(report.outputSnapshot()).contains("{\"name\":\"Alice\",\"version\":2}");
        }

        @Test
        @DisplayName("addWarning() records warnings")
        void addWarningRecordsWarnings() {
            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            builder.addWarning("Missing optional field");
            builder.addWarning("Deprecated format detected");
            MigrationReport report = builder.build();

            assertThat(report.hasWarnings()).isTrue();
            assertThat(report.warnings()).containsExactly(
                    "Missing optional field",
                    "Deprecated format detected"
            );
        }

        @Test
        @DisplayName("addTouchedType() records touched types")
        void addTouchedTypeRecordsTouchedTypes() {
            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            builder.addTouchedType(WORLD);
            MigrationReport report = builder.build();

            assertThat(report.touchedTypes()).contains(PLAYER, WORLD);
        }

        @Test
        @DisplayName("fix lifecycle records FixExecution")
        void fixLifecycleRecordsFixExecution() {
            DataFix<JsonElement> testFix = createTestFix("test_fix", 1, 2);

            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            builder.startFix(testFix);
            builder.setFixBeforeSnapshot("{\"before\":true}");
            builder.recordRuleApplication(RuleApplication.of(
                    "rename_field", "player", Instant.now(), Duration.ofMillis(10), true
            ));
            builder.endFix(testFix, Duration.ofMillis(50), "{\"after\":true}");

            MigrationReport report = builder.build();

            assertThat(report.fixExecutions()).hasSize(1);
            assertThat(report.fixExecutions().get(0).fixName()).isEqualTo("test_fix");
            assertThat(report.fixExecutions().get(0).ruleApplications()).hasSize(1);
            assertThat(report.fixExecutions().get(0).beforeSnapshotOpt()).contains("{\"before\":true}");
            assertThat(report.fixExecutions().get(0).afterSnapshotOpt()).contains("{\"after\":true}");
        }

        @Test
        @DisplayName("multiple fixes are recorded in order")
        void multipleFixesAreRecordedInOrder() {
            DataFix<JsonElement> fix1 = createTestFix("fix1", 1, 2);
            DataFix<JsonElement> fix2 = createTestFix("fix2", 2, 3);

            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(3));

            builder.startFix(fix1);
            builder.endFix(fix1, Duration.ofMillis(10), null);

            builder.startFix(fix2);
            builder.endFix(fix2, Duration.ofMillis(20), null);

            MigrationReport report = builder.build();

            assertThat(report.fixExecutions()).hasSize(2);
            assertThat(report.fixExecutions().get(0).fixName()).isEqualTo("fix1");
            assertThat(report.fixExecutions().get(1).fixName()).isEqualTo("fix2");
        }
    }

    @Nested
    @DisplayName("Report")
    class ReportTests {

        @Test
        @DisplayName("totalDuration() returns time between start and build")
        void totalDurationReturnsTimeBetweenStartAndBuild() throws InterruptedException {
            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            Thread.sleep(10); // Small delay to ensure measurable duration
            MigrationReport report = builder.build();

            assertThat(report.totalDuration().toMillis()).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("fixCount() returns number of fixes")
        void fixCountReturnsNumberOfFixes() {
            DataFix<JsonElement> fix1 = createTestFix("fix1", 1, 2);
            DataFix<JsonElement> fix2 = createTestFix("fix2", 2, 3);

            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(3));
            builder.startFix(fix1);
            builder.endFix(fix1, Duration.ZERO, null);
            builder.startFix(fix2);
            builder.endFix(fix2, Duration.ZERO, null);

            MigrationReport report = builder.build();

            assertThat(report.fixCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("ruleApplicationCount() returns total rule applications")
        void ruleApplicationCountReturnsTotalRuleApplications() {
            DataFix<JsonElement> fix1 = createTestFix("fix1", 1, 2);
            DataFix<JsonElement> fix2 = createTestFix("fix2", 2, 3);

            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(3));

            builder.startFix(fix1);
            builder.recordRuleApplication(RuleApplication.of("rule1", "t", Instant.now(), Duration.ZERO, true));
            builder.recordRuleApplication(RuleApplication.of("rule2", "t", Instant.now(), Duration.ZERO, false));
            builder.endFix(fix1, Duration.ZERO, null);

            builder.startFix(fix2);
            builder.recordRuleApplication(RuleApplication.of("rule3", "t", Instant.now(), Duration.ZERO, true));
            builder.endFix(fix2, Duration.ZERO, null);

            MigrationReport report = builder.build();

            assertThat(report.ruleApplicationCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("hasWarnings() returns false when no warnings")
        void hasWarningsReturnsFalseWhenNoWarnings() {
            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            MigrationReport report = builder.build();

            assertThat(report.hasWarnings()).isFalse();
            assertThat(report.warnings()).isEmpty();
        }

        @Test
        @DisplayName("toSummary() returns formatted summary")
        void toSummaryReturnsFormattedSummary() {
            DataFix<JsonElement> fix = createTestFix("fix1", 1, 2);

            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            builder.startFix(fix);
            builder.endFix(fix, Duration.ZERO, null);
            builder.addWarning("Test warning");

            MigrationReport report = builder.build();
            String summary = report.toSummary();

            assertThat(summary).contains("player");
            assertThat(summary).contains("v1");
            assertThat(summary).contains("v2");
            assertThat(summary).contains("1 fixes");
            assertThat(summary).contains("1 warnings");
        }

        @Test
        @DisplayName("collections are immutable")
        void collectionsAreImmutable() {
            builder.startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            builder.addWarning("Warning");
            builder.addTouchedType(WORLD);

            MigrationReport report = builder.build();

            assertThatThrownBy(() -> report.fixExecutions().add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> report.warnings().add("New warning"))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> report.touchedTypes().add(new TypeReference("entity")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private DataFix<JsonElement> createTestFix(String name, int from, int to) {
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
                return input;
            }
        };
    }
}
