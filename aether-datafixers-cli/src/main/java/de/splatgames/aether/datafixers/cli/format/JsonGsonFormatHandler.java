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

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import org.jetbrains.annotations.NotNull;

/**
 * Format handler for JSON using the Google Gson library.
 *
 * <p>This handler provides JSON parsing and serialization capabilities using
 * {@link Gson} and integrates with Aether Datafixers via {@link GsonOps}.</p>
 *
 * <h2>Format Details</h2>
 * <ul>
 *   <li><b>Format ID:</b> {@code json-gson}</li>
 *   <li><b>File Extensions:</b> {@code .json}</li>
 *   <li><b>Data Type:</b> {@link JsonElement}</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Compact serialization via {@link #serialize(JsonElement)}</li>
 *   <li>Pretty-printed serialization via {@link #serializePretty(JsonElement)}</li>
 *   <li>Robust error handling with descriptive messages</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This handler is thread-safe. The internal {@link Gson} instances are
 * immutable and can be safely shared across threads.</p>
 *
 * @author Erik Pfoertner
 * @see FormatHandler
 * @see GsonOps
 * @see JsonJacksonFormatHandler
 * @since 0.3.0
 */
public class JsonGsonFormatHandler implements FormatHandler<JsonElement> {

    /**
     * Gson instance for compact JSON serialization.
     *
     * <p>Uses default Gson configuration without pretty printing.</p>
     */
    private static final Gson GSON = new Gson();

    /**
     * Gson instance for pretty-printed JSON serialization.
     *
     * <p>Configured with {@link GsonBuilder#setPrettyPrinting()} to produce
     * human-readable output with indentation.</p>
     */
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    /**
     * {@inheritDoc}
     *
     * @return {@code "json-gson"}
     */
    @Override
    @NotNull
    public String formatId() {
        return "json-gson";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "JSON format using Gson"}
     */
    @Override
    @NotNull
    public String description() {
        return "JSON format using Gson";
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
     * @return {@link GsonOps#INSTANCE}
     */
    @Override
    @NotNull
    public DynamicOps<JsonElement> ops() {
        return GsonOps.INSTANCE;
    }

    /**
     * Parses a JSON string into a {@link JsonElement}.
     *
     * <p>This method validates the input and provides descriptive error messages:</p>
     * <ul>
     *   <li>Empty or whitespace-only content throws {@link FormatParseException}</li>
     *   <li>JSON that parses to null throws {@link FormatParseException}</li>
     *   <li>Invalid JSON syntax is wrapped in {@link FormatParseException}</li>
     * </ul>
     *
     * @param content the JSON string to parse, must not be {@code null}
     * @return the parsed {@link JsonElement}, never {@code null}
     * @throws FormatParseException if the content is empty, parses to null,
     *                              or contains invalid JSON syntax
     */
    @Override
    @NotNull
    public JsonElement parse(@NotNull final String content) {
        Preconditions.checkNotNull(content, "content must not be null");

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

    /**
     * Serializes a {@link JsonElement} to a compact JSON string.
     *
     * <p>The output contains no unnecessary whitespace, making it suitable
     * for storage and transmission where size matters.</p>
     *
     * @param data the JSON element to serialize, must not be {@code null}
     * @return the compact JSON string representation
     */
    @Override
    @NotNull
    public String serialize(@NotNull final JsonElement data) {
        Preconditions.checkNotNull(data, "data must not be null");

        return GSON.toJson(data);
    }

    /**
     * Serializes a {@link JsonElement} to a pretty-printed JSON string.
     *
     * <p>The output is formatted with indentation and line breaks for
     * human readability.</p>
     *
     * @param data the JSON element to serialize, must not be {@code null}
     * @return the formatted JSON string representation
     */
    @Override
    @NotNull
    public String serializePretty(@NotNull final JsonElement data) {
        Preconditions.checkNotNull(data, "data must not be null");

        return GSON_PRETTY.toJson(data);
    }
}
