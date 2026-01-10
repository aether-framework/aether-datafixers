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

package de.splatgames.aether.datafixers.testkit.context;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link DataFixerContext} that records all log calls for later inspection.
 *
 * <p>This context implementation captures all {@code info()} and {@code warn()}
 * calls, allowing tests to verify that DataFix implementations log appropriate
 * messages during migration.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * RecordingContext context = new RecordingContext();
 *
 * // Run fix with recording context
 * fix.apply(typeRef, inputData, context);
 *
 * // Verify logging
 * assertThat(context.infoLogs()).hasSize(1);
 * assertThat(context.hasInfo("Migrating")).isTrue();
 * context.assertNoWarnings();
 * }</pre>
 *
 * <h2>Log Entry Format</h2>
 * <p>Each log entry records:</p>
 * <ul>
 *   <li>The log level ({@code INFO} or {@code WARN})</li>
 *   <li>The message format string</li>
 *   <li>The arguments passed</li>
 *   <li>The formatted message (with placeholders substituted)</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see AssertingContext
 * @since 0.2.0
 */
public final class RecordingContext implements DataFixerContext {

    private final List<LogEntry> logs;

    /**
     * Creates a new recording context.
     */
    public RecordingContext() {
        this.logs = new ArrayList<>();
    }

    @Override
    public void info(@NotNull final String message, @Nullable final Object... args) {
        this.logs.add(new LogEntry(LogLevel.INFO, message, args));
    }

    @Override
    public void warn(@NotNull final String message, @Nullable final Object... args) {
        this.logs.add(new LogEntry(LogLevel.WARN, message, args));
    }

    // ==================== Query Methods ====================

    /**
     * Returns all recorded log entries.
     *
     * @return unmodifiable list of all log entries
     */
    @NotNull
    public List<LogEntry> allLogs() {
        return Collections.unmodifiableList(this.logs);
    }

    /**
     * Returns all INFO-level log entries.
     *
     * @return unmodifiable list of INFO entries
     */
    @NotNull
    public List<LogEntry> infoLogs() {
        return this.logs.stream()
                .filter(e -> e.level() == LogLevel.INFO)
                .toList();
    }

    /**
     * Returns all WARN-level log entries.
     *
     * @return unmodifiable list of WARN entries
     */
    @NotNull
    public List<LogEntry> warnLogs() {
        return this.logs.stream()
                .filter(e -> e.level() == LogLevel.WARN)
                .toList();
    }

    /**
     * Checks if any INFO log contains the given substring.
     *
     * @param substring the substring to search for
     * @return true if found
     */
    public boolean hasInfo(@NotNull final String substring) {
        Preconditions.checkNotNull(substring, "substring must not be null");
        return this.infoLogs().stream()
                .anyMatch(e -> e.formattedMessage().contains(substring));
    }

    /**
     * Checks if any WARN log contains the given substring.
     *
     * @param substring the substring to search for
     * @return true if found
     */
    public boolean hasWarn(@NotNull final String substring) {
        Preconditions.checkNotNull(substring, "substring must not be null");
        return this.warnLogs().stream()
                .anyMatch(e -> e.formattedMessage().contains(substring));
    }

    /**
     * Checks if any log (INFO or WARN) contains the given substring.
     *
     * @param substring the substring to search for
     * @return true if found
     */
    public boolean hasLog(@NotNull final String substring) {
        Preconditions.checkNotNull(substring, "substring must not be null");
        return this.logs.stream()
                .anyMatch(e -> e.formattedMessage().contains(substring));
    }

    /**
     * Returns the total number of log entries.
     *
     * @return the log count
     */
    public int size() {
        return this.logs.size();
    }

    /**
     * Checks if no logs were recorded.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return this.logs.isEmpty();
    }

    // ==================== Assertion Helpers ====================

    /**
     * Asserts that no warnings were logged.
     *
     * @throws AssertionError if any warnings were logged
     */
    public void assertNoWarnings() {
        final List<LogEntry> warnings = this.warnLogs();
        if (!warnings.isEmpty()) {
            final String messages = warnings.stream()
                    .map(LogEntry::formattedMessage)
                    .collect(Collectors.joining("\n  - ", "\n  - ", ""));
            throw new AssertionError("Expected no warnings but found " + warnings.size() + ":" + messages);
        }
    }

    /**
     * Asserts that no logs were recorded at all.
     *
     * @throws AssertionError if any logs were recorded
     */
    public void assertNoLogs() {
        if (!this.logs.isEmpty()) {
            final String messages = this.logs.stream()
                    .map(e -> "[" + e.level() + "] " + e.formattedMessage())
                    .collect(Collectors.joining("\n  - ", "\n  - ", ""));
            throw new AssertionError("Expected no logs but found " + this.logs.size() + ":" + messages);
        }
    }

    /**
     * Clears all recorded logs.
     */
    public void clear() {
        this.logs.clear();
    }

    // ==================== Log Entry Record ====================

    /**
     * Log levels for recorded entries.
     */
    public enum LogLevel {
        INFO, WARN
    }

    /**
     * A recorded log entry.
     *
     * @param level   the log level
     * @param message the message format string
     * @param args    the arguments (may be null)
     */
    public record LogEntry(
            @NotNull LogLevel level,
            @NotNull String message,
            @Nullable Object[] args
    ) {

        public LogEntry {
            Preconditions.checkNotNull(level, "level must not be null");
            Preconditions.checkNotNull(message, "message must not be null");
        }

        /**
         * Returns the formatted message with placeholders substituted.
         *
         * @return the formatted message
         */
        @NotNull
        public String formattedMessage() {
            if (this.args == null || this.args.length == 0) {
                return this.message;
            }
            String result = this.message;
            for (final Object arg : this.args) {
                result = result.replaceFirst("\\{}", String.valueOf(arg));
            }
            return result;
        }

        @Override
        public String toString() {
            return "[" + this.level + "] " + this.formattedMessage();
        }
    }
}
