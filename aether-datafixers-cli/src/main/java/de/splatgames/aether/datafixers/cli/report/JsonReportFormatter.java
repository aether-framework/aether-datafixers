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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * JSON report formatter for machine-readable output.
 *
 * <p>This formatter produces pretty-printed JSON reports suitable for automated
 * processing, CI/CD pipelines, scripting, and integration with other tools.</p>
 *
 * <h2>Output Format</h2>
 * <pre>{@code
 * {
 *   "file": "player.json",
 *   "type": "player",
 *   "fromVersion": 100,
 *   "toVersion": 200,
 *   "durationMs": 42
 * }
 * }</pre>
 *
 * <h2>JSON Properties</h2>
 * <ul>
 *   <li>{@code file} (string) - The name of the migrated file</li>
 *   <li>{@code type} (string) - The type reference ID</li>
 *   <li>{@code fromVersion} (integer) - The source schema version</li>
 *   <li>{@code toVersion} (integer) - The target schema version</li>
 *   <li>{@code durationMs} (integer) - Migration duration in milliseconds</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Parsing migration results in shell scripts with {@code jq}</li>
 *   <li>Feeding results to monitoring or logging systems</li>
 *   <li>Generating migration statistics and reports</li>
 *   <li>Integration with CI/CD pipelines for automated validation</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see ReportFormatter
 * @see TextReportFormatter
 * @since 0.3.0
 */
public class JsonReportFormatter implements ReportFormatter {

    /**
     * Gson instance configured for pretty-printed JSON output.
     *
     * <p>Uses {@link GsonBuilder#setPrettyPrinting()} for human-readable
     * JSON with proper indentation.</p>
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Formats a migration report as a JSON object.
     *
     * <p>The output is a pretty-printed JSON object with the following structure:</p>
     * <pre>{@code
     * {
     *   "file": "<fileName>",
     *   "type": "<type>",
     *   "fromVersion": <fromVersion>,
     *   "toVersion": <toVersion>,
     *   "durationMs": <duration in ms>
     * }
     * }</pre>
     *
     * @param fileName    the name of the migrated file
     * @param type        the type reference ID (e.g., "player", "world")
     * @param fromVersion the source schema version number
     * @param toVersion   the target schema version number
     * @param duration    the time taken to perform the migration
     * @return a pretty-printed JSON string representing the migration report
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

        final JsonObject json = new JsonObject();
        json.addProperty("file", fileName);
        json.addProperty("type", type);
        json.addProperty("fromVersion", fromVersion);
        json.addProperty("toVersion", toVersion);
        json.addProperty("durationMs", duration.toMillis());

        return GSON.toJson(json);
    }
}
