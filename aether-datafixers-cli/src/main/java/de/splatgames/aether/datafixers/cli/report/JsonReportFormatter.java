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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * JSON report formatter for machine-readable output.
 *
 * @author Erik Pfoertner
 * @since 0.3.0
 */
public class JsonReportFormatter implements ReportFormatter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public @NotNull String formatSimple(
            @NotNull final String fileName,
            @NotNull final String type,
            final int fromVersion,
            final int toVersion,
            @NotNull final Duration duration
    ) {
        final JsonObject json = new JsonObject();
        json.addProperty("file", fileName);
        json.addProperty("type", type);
        json.addProperty("fromVersion", fromVersion);
        json.addProperty("toVersion", toVersion);
        json.addProperty("durationMs", duration.toMillis());

        return GSON.toJson(json);
    }
}
