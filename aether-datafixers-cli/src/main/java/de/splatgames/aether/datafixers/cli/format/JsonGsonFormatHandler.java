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

package de.splatgames.aether.datafixers.cli.format;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import org.jetbrains.annotations.NotNull;

/**
 * Format handler for JSON using Google Gson.
 *
 * <p>This handler uses {@link GsonOps} for dynamic operations and provides
 * both compact and pretty-printed serialization.</p>
 *
 * @author Erik Pfoertner
 * @since 0.3.0
 */
public class JsonGsonFormatHandler implements FormatHandler<JsonElement> {

    private static final Gson GSON = new Gson();
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public @NotNull String formatId() {
        return "json-gson";
    }

    @Override
    public @NotNull String description() {
        return "JSON format using Gson";
    }

    @Override
    public @NotNull String[] fileExtensions() {
        return new String[]{"json"};
    }

    @Override
    public @NotNull DynamicOps<JsonElement> ops() {
        return GsonOps.INSTANCE;
    }

    @Override
    public @NotNull JsonElement parse(@NotNull final String content) {
        if (content.isBlank()) {
            throw new FormatParseException("Cannot parse empty or whitespace-only content");
        }
        try {
            final JsonElement result = JsonParser.parseString(content);
            if (result.isJsonNull()) {
                throw new FormatParseException("JSON parsed to null");
            }
            return result;
        } catch (final JsonSyntaxException e) {
            throw new FormatParseException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public @NotNull String serialize(@NotNull final JsonElement data) {
        return GSON.toJson(data);
    }

    @Override
    public @NotNull String serializePretty(@NotNull final JsonElement data) {
        return GSON_PRETTY.toJson(data);
    }
}
