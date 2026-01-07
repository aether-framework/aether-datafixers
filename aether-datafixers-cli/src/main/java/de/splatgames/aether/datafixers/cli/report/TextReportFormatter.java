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
 * Plain text report formatter for human-readable output.
 *
 * <p>This formatter produces single-line reports suitable for console output,
 * log files, and quick inspection of migration results.</p>
 *
 * <h2>Output Format</h2>
 * <pre>
 * Migration: player.json [player] v100 -> v200 (42ms)
 * </pre>
 *
 * <h2>Format Components</h2>
 * <ul>
 *   <li>{@code Migration:} - Fixed prefix identifying the line as a migration report</li>
 *   <li>{@code player.json} - The file name that was migrated</li>
 *   <li>{@code [player]} - The type reference ID in square brackets</li>
 *   <li>{@code v100 -> v200} - Source and target versions</li>
 *   <li>{@code (42ms)} - Migration duration in milliseconds</li>
 * </ul>
 *
 * @author Erik Pfoertner
 * @see ReportFormatter
 * @see JsonReportFormatter
 * @since 0.3.0
 */
public class TextReportFormatter implements ReportFormatter {

    /**
     * Formats a migration report as a single line of plain text.
     *
     * <p>The output follows the format:</p>
     * <pre>
     * Migration: {fileName} [{type}] v{fromVersion} -> v{toVersion} ({duration}ms)
     * </pre>
     *
     * @param fileName    the name of the migrated file
     * @param type        the type reference ID (e.g., "player", "world")
     * @param fromVersion the source schema version number
     * @param toVersion   the target schema version number
     * @param duration    the time taken to perform the migration
     * @return a single-line formatted report string
     */
    @Override
    @NotNull
    public String formatSimple(
            @NotNull final String fileName,
            @NotNull final String type,
            final int fromVersion,
            final int toVersion,
            @NotNull final Duration duration
    ) {
        Preconditions.checkNotNull(fileName, "fileName must not be null");
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(duration, "duration must not be null");

        return String.format(
                "Migration: %s [%s] v%d -> v%d (%dms)",
                fileName,
                type,
                fromVersion,
                toVersion,
                duration.toMillis()
        );
    }
}
