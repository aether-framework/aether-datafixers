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

    private final DiagnosticOptions options;
    private final MigrationReportImpl.BuilderImpl reportBuilder;
    private final List<LogEntry> logs;
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

    @Override
    public void info(@NotNull final String message, @Nullable final Object... args) {
        Preconditions.checkNotNull(message, "message must not be null");
        this.logs.add(new LogEntry(LogLevel.INFO, message, args));
    }

    @Override
    public void warn(@NotNull final String message, @Nullable final Object... args) {
        Preconditions.checkNotNull(message, "message must not be null");
        this.logs.add(new LogEntry(LogLevel.WARN, message, args));
        this.reportBuilder.addWarning(formatMessage(message, args));
    }

    @Override
    public boolean isDiagnosticEnabled() {
        return true;
    }

    @Override
    @NotNull
    public MigrationReport.Builder reportBuilder() {
        return this.reportBuilder;
    }

    @Override
    @NotNull
    public MigrationReport getReport() {
        if (this.cachedReport == null) {
            this.cachedReport = this.reportBuilder.build();
        }
        return this.cachedReport;
    }

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
    private static String formatMessage(
            @NotNull final String message,
            @Nullable final Object... args
    ) {
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
     * Log level enumeration.
     */
    public enum LogLevel {
        /**
         * Informational log level.
         */
        INFO,

        /**
         * Warning log level.
         */
        WARN
    }

    /**
     * A recorded log entry.
     *
     * @param level   the log level
     * @param message the message format string
     * @param args    the arguments
     */
    public record LogEntry(
            @NotNull LogLevel level,
            @NotNull String message,
            @Nullable Object[] args
    ) {

        /**
         * Returns the formatted message with placeholders replaced.
         *
         * @return the formatted message
         */
        @NotNull
        public String formattedMessage() {
            return DiagnosticContextImpl.formatMessage(this.message, this.args);
        }

        @NotNull
        @Override
        public String toString() {
            return String.format("[%s] %s", this.level, this.formattedMessage());
        }
    }
}
