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
import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.codec.jackson.JacksonOps;
import org.jetbrains.annotations.NotNull;

/**
 * Format handler for JSON using the Jackson Databind library.
 *
 * <p>This handler provides JSON parsing and serialization capabilities using
 * {@link ObjectMapper} and integrates with Aether Datafixers via {@link JacksonOps}.</p>
 *
 * <h2>Format Details</h2>
 * <ul>
 *   <li><b>Format ID:</b> {@code json-jackson}</li>
 *   <li><b>File Extensions:</b> {@code .json}</li>
 *   <li><b>Data Type:</b> {@link JsonNode}</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Compact serialization via {@link #serialize(JsonNode)}</li>
 *   <li>Pretty-printed serialization via {@link #serializePretty(JsonNode)}</li>
 *   <li>Robust error handling with descriptive messages</li>
 *   <li>Full Jackson feature set available for customization</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This handler is thread-safe. The internal {@link ObjectMapper} instances are
 * configured once and can be safely shared across threads (ObjectMapper is thread-safe
 * after configuration).</p>
 *
 * <h2>When to Use</h2>
 * <p>Prefer this handler over {@link JsonGsonFormatHandler} when:</p>
 * <ul>
 *   <li>Your project already uses Jackson extensively</li>
 *   <li>You need advanced Jackson features (streaming, annotations, etc.)</li>
 *   <li>Performance is critical (Jackson is generally faster for large documents)</li>
 * </ul>
 *
 * @author Erik Pfoertner
 * @see FormatHandler
 * @see JacksonOps
 * @see JsonGsonFormatHandler
 * @since 0.3.0
 */
public class JsonJacksonFormatHandler implements FormatHandler<JsonNode> {

    /**
     * ObjectMapper instance for compact JSON serialization.
     *
     * <p>Uses default Jackson configuration without indentation.</p>
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * ObjectMapper instance for pretty-printed JSON serialization.
     *
     * <p>Configured with {@link SerializationFeature#INDENT_OUTPUT} to produce
     * human-readable output with indentation.</p>
     */
    private static final ObjectMapper MAPPER_PRETTY = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * {@inheritDoc}
     *
     * @return {@code "json-jackson"}
     */
    @Override
    @NotNull
    public String formatId() {
        return "json-jackson";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "JSON format using Jackson"}
     */
    @Override
    @NotNull
    public String description() {
        return "JSON format using Jackson";
    }

    /**
     * {@inheritDoc}
     *
     * @return an array containing {@code "json"}
     */
    @Override
    @NotNull
    public String[] fileExtensions() {
        return new String[]{"json"};
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link JacksonOps#INSTANCE}
     */
    @Override
    @NotNull
    public DynamicOps<JsonNode> ops() {
        return JacksonOps.INSTANCE;
    }

    /**
     * Parses a JSON string into a {@link JsonNode}.
     *
     * <p>This method validates the input and provides descriptive error messages:</p>
     * <ul>
     *   <li>Empty or whitespace-only content throws {@link FormatParseException}</li>
     *   <li>JSON that parses to null throws {@link FormatParseException}</li>
     *   <li>Invalid JSON syntax is wrapped in {@link FormatParseException}</li>
     * </ul>
     *
     * @param content the JSON string to parse, must not be {@code null}
     * @return the parsed {@link JsonNode}, never {@code null}
     * @throws FormatParseException if the content is empty, parses to null,
     *                              or contains invalid JSON syntax
     */
    @Override
    @NotNull
    public JsonNode parse(@NotNull final String content) {
        Preconditions.checkNotNull(content, "content must not be null");

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

    /**
     * Serializes a {@link JsonNode} to a compact JSON string.
     *
     * <p>The output contains no unnecessary whitespace, making it suitable
     * for storage and transmission where size matters.</p>
     *
     * @param data the JSON node to serialize, must not be {@code null}
     * @return the compact JSON string representation
     * @throws RuntimeException if serialization fails (should not happen with valid JsonNode)
     */
    @Override
    @NotNull
    public String serialize(@NotNull final JsonNode data) {
        Preconditions.checkNotNull(data, "data must not be null");

        try {
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    /**
     * Serializes a {@link JsonNode} to a pretty-printed JSON string.
     *
     * <p>The output is formatted with indentation and line breaks for
     * human readability.</p>
     *
     * @param data the JSON node to serialize, must not be {@code null}
     * @return the formatted JSON string representation
     * @throws RuntimeException if serialization fails (should not happen with valid JsonNode)
     */
    @Override
    @NotNull
    public String serializePretty(@NotNull final JsonNode data) {
        Preconditions.checkNotNull(data, "data must not be null");

        try {
            return MAPPER_PRETTY.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }
}
