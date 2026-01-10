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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps;
import org.jetbrains.annotations.NotNull;

/**
 * Format handler for TOML using the Jackson Dataformat TOML library.
 *
 * <p>This handler provides TOML parsing and serialization capabilities using
 * {@link TomlMapper} and integrates with Aether Datafixers via {@link JacksonTomlOps}.</p>
 *
 * <h2>Format Details</h2>
 * <ul>
 *   <li><b>Format ID:</b> {@code toml-jackson}</li>
 *   <li><b>File Extensions:</b> {@code .toml}</li>
 *   <li><b>Data Type:</b> {@link JsonNode}</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>TOML 1.0 specification compliant parsing and serialization</li>
 *   <li>Compact serialization via {@link #serialize(JsonNode)}</li>
 *   <li>Pretty-printed serialization via {@link #serializePretty(JsonNode)}</li>
 *   <li>Robust error handling with descriptive messages</li>
 *   <li>Full Jackson feature set available for TOML processing</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This handler is thread-safe. The internal {@link TomlMapper} instances are
 * configured once and can be safely shared across threads (TomlMapper is thread-safe
 * after configuration).</p>
 *
 * <h2>TOML-Specific Considerations</h2>
 * <p>TOML has some structural requirements that differ from JSON and YAML:</p>
 * <ul>
 *   <li>Root element must be a table (object), not an array or primitive</li>
 *   <li>Arrays can only contain elements of the same type</li>
 *   <li>Null values are not supported in TOML specification</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see FormatHandler
 * @see JacksonTomlOps
 * @since 0.5.0
 */
public class TomlJacksonFormatHandler implements FormatHandler<JsonNode> {

    /**
     * TomlMapper instance for compact TOML serialization.
     *
     * <p>Uses default Jackson TOML configuration.</p>
     */
    private static final TomlMapper MAPPER = new TomlMapper();

    /**
     * TomlMapper instance for pretty-printed TOML serialization.
     *
     * <p>Configured with {@link SerializationFeature#INDENT_OUTPUT} to produce
     * human-readable output with indentation.</p>
     */
    private static final TomlMapper MAPPER_PRETTY = TomlMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /**
     * {@inheritDoc}
     *
     * @return {@code "toml-jackson"}
     */
    @Override
    @NotNull
    public String formatId() {
        return "toml-jackson";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "TOML format using Jackson"}
     */
    @Override
    @NotNull
    public String description() {
        return "TOML format using Jackson";
    }

    /**
     * {@inheritDoc}
     *
     * @return an array containing {@code "toml"}
     */
    @Override
    @NotNull
    public String[] fileExtensions() {
        return new String[]{"toml"};
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link JacksonTomlOps#INSTANCE}
     */
    @Override
    @NotNull
    public DynamicOps<JsonNode> ops() {
        return JacksonTomlOps.INSTANCE;
    }

    /**
     * Parses a TOML string into a {@link JsonNode}.
     *
     * <p>This method validates the input and provides descriptive error messages:</p>
     * <ul>
     *   <li>Empty or whitespace-only content throws {@link FormatParseException}</li>
     *   <li>TOML that parses to null throws {@link FormatParseException}</li>
     *   <li>Invalid TOML syntax is wrapped in {@link FormatParseException}</li>
     * </ul>
     *
     * @param content the TOML string to parse, must not be {@code null}
     * @return the parsed {@link JsonNode}, never {@code null}
     * @throws FormatParseException if the content is empty, parses to null,
     *                              or contains invalid TOML syntax
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
                throw new FormatParseException("TOML parsed to null");
            }
            return result;
        } catch (final JsonProcessingException e) {
            throw new FormatParseException("Failed to parse TOML: " + e.getMessage(), e);
        }
    }

    /**
     * Serializes a {@link JsonNode} to a compact TOML string.
     *
     * <p>The output contains minimal whitespace, making it suitable
     * for storage and transmission where size matters.</p>
     *
     * @param data the JSON node to serialize, must not be {@code null}
     * @return the compact TOML string representation
     * @throws RuntimeException if serialization fails (e.g., due to TOML structural constraints)
     */
    @Override
    @NotNull
    public String serialize(@NotNull final JsonNode data) {
        Preconditions.checkNotNull(data, "data must not be null");

        try {
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize TOML", e);
        }
    }

    /**
     * Serializes a {@link JsonNode} to a pretty-printed TOML string.
     *
     * <p>The output is formatted with proper TOML structure for human readability.</p>
     *
     * @param data the JSON node to serialize, must not be {@code null}
     * @return the formatted TOML string representation
     * @throws RuntimeException if serialization fails (e.g., due to TOML structural constraints)
     */
    @Override
    @NotNull
    public String serializePretty(@NotNull final JsonNode data) {
        Preconditions.checkNotNull(data, "data must not be null");

        try {
            return MAPPER_PRETTY.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize TOML", e);
        }
    }
}
