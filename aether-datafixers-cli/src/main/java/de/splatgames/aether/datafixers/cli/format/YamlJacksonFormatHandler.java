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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps;
import org.jetbrains.annotations.NotNull;

/**
 * Format handler for YAML using the Jackson Dataformat YAML library.
 *
 * <p>This handler provides YAML parsing and serialization capabilities using
 * {@link YAMLMapper} and integrates with Aether Datafixers via {@link JacksonYamlOps}.</p>
 *
 * <h2>Format Details</h2>
 * <ul>
 *   <li><b>Format ID:</b> {@code yaml-jackson}</li>
 *   <li><b>File Extensions:</b> {@code .yaml}, {@code .yml}</li>
 *   <li><b>Data Type:</b> {@link JsonNode}</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Compact serialization via {@link #serialize(JsonNode)}</li>
 *   <li>Pretty-printed serialization via {@link #serializePretty(JsonNode)}</li>
 *   <li>Robust error handling with descriptive messages</li>
 *   <li>Full Jackson feature set available for YAML processing</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This handler is thread-safe. The internal {@link YAMLMapper} instances are
 * configured once and can be safely shared across threads (YAMLMapper is thread-safe
 * after configuration).</p>
 *
 * <h2>When to Use</h2>
 * <p>Prefer this handler over {@link YamlSnakeYamlFormatHandler} when:</p>
 * <ul>
 *   <li>Your project already uses Jackson extensively</li>
 *   <li>You need consistency with other Jackson-based formats (JSON, TOML, XML)</li>
 *   <li>You need advanced Jackson features (streaming, annotations, etc.)</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see FormatHandler
 * @see JacksonYamlOps
 * @see YamlSnakeYamlFormatHandler
 * @since 0.5.0
 */
public class YamlJacksonFormatHandler implements FormatHandler<JsonNode> {

    /**
     * YAMLMapper instance for compact YAML serialization.
     *
     * <p>Uses default Jackson YAML configuration without indentation.</p>
     */
    private static final YAMLMapper MAPPER = new YAMLMapper();

    /**
     * YAMLMapper instance for pretty-printed YAML serialization.
     *
     * <p>Configured with {@link SerializationFeature#INDENT_OUTPUT} to produce
     * human-readable output with indentation.</p>
     */
    private static final YAMLMapper MAPPER_PRETTY = YAMLMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    /**
     * {@inheritDoc}
     *
     * @return {@code "yaml-jackson"}
     */
    @Override
    @NotNull
    public String formatId() {
        return "yaml-jackson";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "YAML format using Jackson"}
     */
    @Override
    @NotNull
    public String description() {
        return "YAML format using Jackson";
    }

    /**
     * {@inheritDoc}
     *
     * @return an array containing {@code "yaml"} and {@code "yml"}
     */
    @Override
    @NotNull
    public String[] fileExtensions() {
        return new String[]{"yaml", "yml"};
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link JacksonYamlOps#INSTANCE}
     */
    @Override
    @NotNull
    public DynamicOps<JsonNode> ops() {
        return JacksonYamlOps.INSTANCE;
    }

    /**
     * Parses a YAML string into a {@link JsonNode}.
     *
     * <p>This method validates the input and provides descriptive error messages:</p>
     * <ul>
     *   <li>Empty or whitespace-only content throws {@link FormatParseException}</li>
     *   <li>YAML that parses to null throws {@link FormatParseException}</li>
     *   <li>Invalid YAML syntax is wrapped in {@link FormatParseException}</li>
     * </ul>
     *
     * @param content the YAML string to parse, must not be {@code null}
     * @return the parsed {@link JsonNode}, never {@code null}
     * @throws FormatParseException if the content is empty, parses to null,
     *                              or contains invalid YAML syntax
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
                throw new FormatParseException("YAML parsed to null");
            }
            return result;
        } catch (final JsonProcessingException e) {
            throw new FormatParseException("Failed to parse YAML: " + e.getMessage(), e);
        }
    }

    /**
     * Serializes a {@link JsonNode} to a compact YAML string.
     *
     * <p>The output contains minimal whitespace, making it suitable
     * for storage and transmission where size matters.</p>
     *
     * @param data the JSON node to serialize, must not be {@code null}
     * @return the compact YAML string representation
     * @throws RuntimeException if serialization fails (should not happen with valid JsonNode)
     */
    @Override
    @NotNull
    public String serialize(@NotNull final JsonNode data) {
        Preconditions.checkNotNull(data, "data must not be null");

        try {
            return MAPPER.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize YAML", e);
        }
    }

    /**
     * Serializes a {@link JsonNode} to a pretty-printed YAML string.
     *
     * <p>The output is formatted with indentation for human readability.</p>
     *
     * @param data the JSON node to serialize, must not be {@code null}
     * @return the formatted YAML string representation
     * @throws RuntimeException if serialization fails (should not happen with valid JsonNode)
     */
    @Override
    @NotNull
    public String serializePretty(@NotNull final JsonNode data) {
        Preconditions.checkNotNull(data, "data must not be null");

        try {
            return MAPPER_PRETTY.writeValueAsString(data);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize YAML", e);
        }
    }
}
