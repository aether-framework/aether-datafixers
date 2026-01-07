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

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Formats migration reports for output.
 *
 * <p>Report formatters transform migration metadata into human-readable
 * or machine-readable output formats. The CLI uses these formatters to
 * generate reports when the {@code --report} option is specified.</p>
 *
 * <h2>Built-in Formatters</h2>
 * <ul>
 *   <li>{@link TextReportFormatter} - Plain text format for human consumption</li>
 *   <li>{@link JsonReportFormatter} - JSON format for machine processing</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ReportFormatter formatter = ReportFormatter.forFormat("json");
 * String report = formatter.formatSimple("player.json", "player", 100, 200, duration);
 * }</pre>
 *
 * @author Erik Pfoertner
 * @see TextReportFormatter
 * @see JsonReportFormatter
 * @since 0.3.0
 */
public interface ReportFormatter {

    /**
     * Formats a simple migration report.
     *
     * @param fileName    the name of the migrated file
     * @param type        the type reference ID
     * @param fromVersion the source version
     * @param toVersion   the target version
     * @param duration    the migration duration
     * @return the formatted report string
     */
    @NotNull
    String formatSimple(
            @NotNull String fileName,
            @NotNull String type,
            int fromVersion,
            int toVersion,
            @NotNull Duration duration
    );

    /**
     * Gets a formatter by format name.
     *
     * @param format the format name ("text" or "json")
     * @return the appropriate formatter
     */
    @NotNull
    static ReportFormatter forFormat(@NotNull final String format) {
        Preconditions.checkNotNull(format, "format must not be null");

        return switch (format.toLowerCase()) {
            case "json" -> new JsonReportFormatter();
            default -> new TextReportFormatter();
        };
    }
}
