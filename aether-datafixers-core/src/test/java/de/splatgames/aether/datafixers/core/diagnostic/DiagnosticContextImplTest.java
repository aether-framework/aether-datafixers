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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DiagnosticContextImpl}.
 */
@DisplayName("DiagnosticContextImpl")
class DiagnosticContextImplTest {

    private static final TypeReference PLAYER = new TypeReference("player");

    private DiagnosticContextImpl context;

    @BeforeEach
    void setUp() {
        context = new DiagnosticContextImpl(DiagnosticOptions.defaults());
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("creates context with specified options")
        void createsContextWithSpecifiedOptions() {
            DiagnosticOptions options = DiagnosticOptions.builder()
                    .captureSnapshots(false)
                    .captureRuleDetails(true)
                    .build();

            DiagnosticContextImpl ctx = new DiagnosticContextImpl(options);

            assertThat(ctx.options()).isEqualTo(options);
            assertThat(ctx.options().captureSnapshots()).isFalse();
            assertThat(ctx.options().captureRuleDetails()).isTrue();
        }

        @Test
        @DisplayName("throws NullPointerException for null options")
        void throwsNullPointerExceptionForNullOptions() {
            assertThatThrownBy(() -> new DiagnosticContextImpl(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("isDiagnosticEnabled() returns true")
        void isDiagnosticEnabledReturnsTrue() {
            assertThat(context.isDiagnosticEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Logging")
    class Logging {

        @Test
        @DisplayName("info() records INFO log entry")
        void infoRecordsInfoLogEntry() {
            context.info("Processing {} data", "player");

            assertThat(context.logs()).hasSize(1);
            assertThat(context.infoLogs()).hasSize(1);
            assertThat(context.warnLogs()).isEmpty();
            assertThat(context.logs().get(0).level()).isEqualTo(DiagnosticContextImpl.LogLevel.INFO);
            assertThat(context.logs().get(0).formattedMessage()).isEqualTo("Processing player data");
        }

        @Test
        @DisplayName("warn() records WARN log entry and adds to report")
        void warnRecordsWarnLogEntryAndAddsToReport() {
            context.reportBuilder().startMigration(PLAYER, new DataVersion(1), new DataVersion(2));
            context.warn("Missing field: {}", "health");

            assertThat(context.logs()).hasSize(1);
            assertThat(context.warnLogs()).hasSize(1);
            assertThat(context.infoLogs()).isEmpty();
            assertThat(context.logs().get(0).level()).isEqualTo(DiagnosticContextImpl.LogLevel.WARN);

            MigrationReport report = context.getReport();
            assertThat(report.warnings()).contains("Missing field: health");
        }

        @Test
        @DisplayName("hasLog() searches all logs")
        void hasLogSearchesAllLogs() {
            context.info("Processing player");
            context.warn("Missing field");

            assertThat(context.hasLog("player")).isTrue();
            assertThat(context.hasLog("Missing")).isTrue();
            assertThat(context.hasLog("unknown")).isFalse();
        }

        @Test
        @DisplayName("hasInfo() searches INFO logs only")
        void hasInfoSearchesInfoLogsOnly() {
            context.info("Processing player");
            context.warn("Missing field");

            assertThat(context.hasInfo("player")).isTrue();
            assertThat(context.hasInfo("Missing")).isFalse();
        }

        @Test
        @DisplayName("hasWarn() searches WARN logs only")
        void hasWarnSearchesWarnLogsOnly() {
            context.info("Processing player");
            context.warn("Missing field");

            assertThat(context.hasWarn("Missing")).isTrue();
            assertThat(context.hasWarn("player")).isFalse();
        }

        @Test
        @DisplayName("logCount() returns total log count")
        void logCountReturnsTotalLogCount() {
            context.info("Info 1");
            context.info("Info 2");
            context.warn("Warn 1");

            assertThat(context.logCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("isEmpty() returns true when no logs")
        void isEmptyReturnsTrueWhenNoLogs() {
            assertThat(context.isEmpty()).isTrue();

            context.info("Log");

            assertThat(context.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("clear() removes all logs")
        void clearRemovesAllLogs() {
            context.info("Log 1");
            context.warn("Log 2");

            context.clear();

            assertThat(context.isEmpty()).isTrue();
            assertThat(context.logs()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Message Formatting")
    class MessageFormatting {

        @Test
        @DisplayName("formats message with single placeholder")
        void formatsMessageWithSinglePlaceholder() {
            context.info("Hello, {}!", "World");

            assertThat(context.logs().get(0).formattedMessage()).isEqualTo("Hello, World!");
        }

        @Test
        @DisplayName("formats message with multiple placeholders")
        void formatsMessageWithMultiplePlaceholders() {
            context.info("{} has {} items", "Alice", 5);

            assertThat(context.logs().get(0).formattedMessage()).isEqualTo("Alice has 5 items");
        }

        @Test
        @DisplayName("handles message without placeholders")
        void handlesMessageWithoutPlaceholders() {
            context.info("Simple message");

            assertThat(context.logs().get(0).formattedMessage()).isEqualTo("Simple message");
        }

        @Test
        @DisplayName("handles null args")
        void handlesNullArgs() {
            context.info("Message with null", (Object[]) null);

            assertThat(context.logs().get(0).formattedMessage()).isEqualTo("Message with null");
        }

        @Test
        @DisplayName("handles more placeholders than args")
        void handlesMorePlaceholdersThanArgs() {
            context.info("{} {} {}", "only", "two");

            assertThat(context.logs().get(0).formattedMessage()).isEqualTo("only two {}");
        }
    }

    @Nested
    @DisplayName("Report Integration")
    class ReportIntegration {

        @Test
        @DisplayName("reportBuilder() returns the builder")
        void reportBuilderReturnsBuilder() {
            assertThat(context.reportBuilder()).isNotNull();
        }

        @Test
        @DisplayName("getReport() returns built report")
        void getReportReturnsBuiltReport() {
            context.reportBuilder().startMigration(PLAYER, new DataVersion(1), new DataVersion(2));

            MigrationReport report = context.getReport();

            assertThat(report).isNotNull();
            assertThat(report.type()).isEqualTo(PLAYER);
        }

        @Test
        @DisplayName("getReport() caches report")
        void getReportCachesReport() {
            context.reportBuilder().startMigration(PLAYER, new DataVersion(1), new DataVersion(2));

            MigrationReport report1 = context.getReport();
            MigrationReport report2 = context.getReport();

            assertThat(report1).isSameAs(report2);
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("DiagnosticContext.create() returns DiagnosticContextImpl")
        void createReturnsImpl() {
            DiagnosticContext ctx = DiagnosticContext.create();

            assertThat(ctx).isInstanceOf(DiagnosticContextImpl.class);
            assertThat(ctx.options()).isEqualTo(DiagnosticOptions.defaults());
        }

        @Test
        @DisplayName("DiagnosticContext.create(options) returns configured context")
        void createWithOptionsReturnsConfiguredContext() {
            DiagnosticOptions options = DiagnosticOptions.minimal();

            DiagnosticContext ctx = DiagnosticContext.create(options);

            assertThat(ctx).isInstanceOf(DiagnosticContextImpl.class);
            assertThat(ctx.options()).isEqualTo(options);
        }
    }
}
