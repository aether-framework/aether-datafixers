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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext;
import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of {@link DiagnosticContext}.
 *
 * <p>This implementation captures all diagnostic events during migration
 * and produces a {@link MigrationReport} when {@link #getReport()} is called.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This implementation is designed for single-threaded use during a single
 * migration operation. It is not thread-safe for concurrent modifications.</p>
 *
 * @author Erik Pförtner
 * @see DiagnosticContext
 * @see MigrationReportImpl
 * @since 0.2.0
 */
public final class DiagnosticContextImpl implements DiagnosticContext {

    /** The diagnostic options controlling what data is captured during migration. */
    @NotNull
    private final DiagnosticOptions options;

    /** The builder used to incrementally construct the {@link MigrationReport}. */
    @NotNull
    private final MigrationReportImpl.BuilderImpl reportBuilder;

    /** The chronologically ordered list of all log entries recorded during migration. */
    @NotNull
    private final List<LogEntry> logs;

    /**
     * A lazily computed and cached migration report.
     *
     * <p>Set to {@code null} initially and after {@link #clear()}, then populated
     * on the first call to {@link #getReport()}.</p>
     */
    @Nullable
    private MigrationReport cachedReport;

    /**
     * Creates a new diagnostic context with the specified options.
     *
     * @param options the diagnostic options, must not be {@code null}
     * @throws NullPointerException if options is {@code null}
     */
    public DiagnosticContextImpl(@NotNull final DiagnosticOptions options) {
        Preconditions.checkNotNull(options, "options must not be null");

        this.options = options;
        this.reportBuilder = MigrationReportImpl.builder();
        this.logs = new ArrayList<>();
        this.cachedReport = null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Records the message as an {@link LogLevel#INFO INFO}-level {@link LogEntry}.</p>
     *
     * @param message the message format string with {@code {}} placeholders; must not be {@code null}
     * @param args    optional arguments to substitute into the placeholders
     * @throws NullPointerException if {@code message} is {@code null}
     */
    @Override
    public void info(@NotNull final String message, @Nullable final Object... args) {
        Preconditions.checkNotNull(message, "message must not be null");
        this.logs.add(new LogEntry(LogLevel.INFO, message, args));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Records the message as a {@link LogLevel#WARN WARN}-level {@link LogEntry}
     * and additionally forwards the formatted message to the {@link #reportBuilder()} as a warning.</p>
     *
     * @param message the message format string with {@code {}} placeholders; must not be {@code null}
     * @param args    optional arguments to substitute into the placeholders
     * @throws NullPointerException if {@code message} is {@code null}
     */
    @Override
    public void warn(@NotNull final String message, @Nullable final Object... args) {
        Preconditions.checkNotNull(message, "message must not be null");
        this.logs.add(new LogEntry(LogLevel.WARN, message, args));
        this.reportBuilder.addWarning(formatMessage(message, args));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation always returns {@code true}, since the purpose of
     * {@code DiagnosticContextImpl} is to actively capture diagnostic data.</p>
     *
     * @return always {@code true}
     */
    @Override
    public boolean isDiagnosticEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the internal {@link MigrationReportImpl.BuilderImpl} used to
     * incrementally construct the migration report.</p>
     *
     * @return the report builder; never {@code null}
     */
    @Override
    @NotNull
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Builder exposure is intentional API design for report construction."
    )
    public MigrationReport.Builder reportBuilder() {
        return this.reportBuilder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>On the first call, the report is built from the internal builder and cached.
     * Subsequent calls return the same cached instance. Calling {@link #clear()} invalidates
     * the cache.</p>
     *
     * @return the migration report; never {@code null}
     */
    @Override
    @NotNull
    public MigrationReport getReport() {
        MigrationReport report = this.cachedReport;
        if (report == null) {
            report = this.reportBuilder.build();
            this.cachedReport = report;
        }
        return report;
    }

    /**
     * {@inheritDoc}
     *
     * @return the diagnostic options provided at construction time; never {@code null}
     */
    @Override
    @NotNull
    public DiagnosticOptions options() {
        return this.options;
    }

    /**
     * Returns all log entries recorded during migration.
     *
     * @return unmodifiable list of log entries
     */
    @NotNull
    public List<LogEntry> logs() {
        return Collections.unmodifiableList(this.logs);
    }

    /**
     * Returns all INFO-level log entries.
     *
     * @return unmodifiable list of INFO log entries
     */
    @NotNull
    public List<LogEntry> infoLogs() {
        return this.logs.stream()
                .filter(entry -> entry.level() == LogLevel.INFO)
                .toList();
    }

    /**
     * Returns all WARN-level log entries.
     *
     * @return unmodifiable list of WARN log entries
     */
    @NotNull
    public List<LogEntry> warnLogs() {
        return this.logs.stream()
                .filter(entry -> entry.level() == LogLevel.WARN)
                .toList();
    }

    /**
     * Checks if any log entry contains the given substring.
     *
     * @param substring the substring to search for
     * @return {@code true} if any log contains the substring
     */
    public boolean hasLog(@NotNull final String substring) {
        Preconditions.checkNotNull(substring, "substring must not be null");
        return this.logs.stream()
                .anyMatch(entry -> entry.formattedMessage().contains(substring));
    }

    /**
     * Checks if any INFO log entry contains the given substring.
     *
     * @param substring the substring to search for
     * @return {@code true} if any INFO log contains the substring
     */
    public boolean hasInfo(@NotNull final String substring) {
        Preconditions.checkNotNull(substring, "substring must not be null");
        return this.infoLogs().stream()
                .anyMatch(entry -> entry.formattedMessage().contains(substring));
    }

    /**
     * Checks if any WARN log entry contains the given substring.
     *
     * @param substring the substring to search for
     * @return {@code true} if any WARN log contains the substring
     */
    public boolean hasWarn(@NotNull final String substring) {
        Preconditions.checkNotNull(substring, "substring must not be null");
        return this.warnLogs().stream()
                .anyMatch(entry -> entry.formattedMessage().contains(substring));
    }

    /**
     * Returns the number of log entries.
     *
     * @return log count
     */
    public int logCount() {
        return this.logs.size();
    }

    /**
     * Checks if no logs have been recorded.
     *
     * @return {@code true} if no logs
     */
    public boolean isEmpty() {
        return this.logs.isEmpty();
    }

    /**
     * Clears all recorded logs.
     *
     * <p>This is useful for reusing the context across multiple operations
     * in testing scenarios.</p>
     */
    public void clear() {
        this.logs.clear();
        this.cachedReport = null;
    }

    /**
     * Formats a message by replacing {@code {}} placeholders with arguments.
     *
     * @param message the message format string
     * @param args    the arguments to substitute
     * @return the formatted message
     */
    @NotNull
    private static String formatMessage(@NotNull final String message,
                                        @Nullable final Object... args) {
        Preconditions.checkNotNull(message, "message must not be null");
        if (args == null || args.length == 0) {
            return message;
        }

        final StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int lastEnd = 0;

        int idx = message.indexOf("{}");
        while (idx >= 0 && argIndex < args.length) {
            sb.append(message, lastEnd, idx);
            sb.append(args[argIndex++]);
            lastEnd = idx + 2;
            idx = message.indexOf("{}", lastEnd);
        }

        sb.append(message.substring(lastEnd));
        return sb.toString();
    }

    /**
     * Log severity levels for diagnostic entries.
     *
     * @see LogEntry
     */
    public enum LogLevel {
        /**
         * Informational log level for routine diagnostic messages
         * that do not indicate a problem.
         */
        INFO,

        /**
         * Warning log level for messages that indicate a potential issue
         * or unexpected condition during migration.
         */
        WARN
    }

    /**
     * A recorded log entry.
     *
     * @param level   the log level
     * @param message the message format string
     * @param args    the arguments (defensively copied)
     */
    public record LogEntry(@NotNull LogLevel level,
                           @NotNull String message,
                           @Nullable Object[] args) {

        /**
         * Compact constructor that defensively copies the args array.
         *
         * @param level   the log level, must not be {@code null}
         * @param message the message format string, must not be {@code null}
         * @param args    the format arguments, may be {@code null}
         */
        public LogEntry {
            args = args != null ? args.clone() : null;
        }

        /**
         * Returns a defensive copy of the arguments array.
         *
         * @return a copy of the arguments, or {@code null} if no arguments
         */
        @Override
        @Nullable
        public Object[] args() {
            return this.args != null ? this.args.clone() : null;
        }

        /**
         * Returns the formatted message with placeholders replaced.
         *
         * @return the formatted message
         */
        @NotNull
        public String formattedMessage() {
            final Object[] currentArgs = this.args;
            if (currentArgs == null) {
                return this.message;
            }
            return DiagnosticContextImpl.formatMessage(this.message, currentArgs);
        }

        /**
         * Returns a human-readable representation of this log entry in the format
         * {@code [LEVEL] formatted message}.
         *
         * @return the formatted log entry string; never {@code null}
         */
        @NotNull
        @Override
        public String toString() {
            return String.format("[%s] %s", this.level, this.formattedMessage());
        }
    }
}
