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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.codec.jackson.JacksonOps;
import org.jetbrains.annotations.NotNull;

/**
 * Format handler for JSON using Jackson Databind.
 *
 * <p>This handler uses {@link JacksonOps} for dynamic operations and provides
 * both compact and pretty-printed serialization.</p>
 *
 * @author Erik Pfoertner
 * @since 0.3.0
 */
public class JsonJacksonFormatHandler implements FormatHandler<JsonNode> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectMapper MAPPER_PRETTY = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public @NotNull String formatId() {
        return "json-jackson";
    }

    @Override
    public @NotNull String description() {
        return "JSON format using Jackson";
    }

    @Override
    public @NotNull String[] fileExtensions() {
        return new String[]{"json"};
    }

    @Override
    public @NotNull DynamicOps<JsonNode> ops() {
        return JacksonOps.INSTANCE;
    }

    @Override
    public @NotNull JsonNode parse(@NotNull final String content) {
        if (content.isBlank()) {
            throw new FormatParseException("Cannot parse empty or whitespace-only content");
        }
        try {
            final JsonNode result = MAPPER.readTree(content);
            if (result == null || result.isNull()) {
                throw new FormatParseException("JSON parsed to null");
            }
            return result;
        } catch (final JsonProcessingException e) {
            throw new FormatParseException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public @NotNull String serialize(@NotNull final JsonNode data) {
        try {
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    @Override
    public @NotNull String serializePretty(@NotNull final JsonNode data) {
        try {
            return MAPPER_PRETTY.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }
}
