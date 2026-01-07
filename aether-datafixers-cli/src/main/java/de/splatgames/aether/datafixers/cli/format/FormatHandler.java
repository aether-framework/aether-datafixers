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

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import org.jetbrains.annotations.NotNull;

/**
 * Service Provider Interface for format handlers.
 *
 * <p>Format handlers enable the CLI to read and write data in various formats.
 * Implementations are discovered via {@link java.util.ServiceLoader}.</p>
 *
 * <h2>Built-in Format Handlers</h2>
 * <ul>
 *   <li>{@code json-gson} — JSON format using Google Gson</li>
 *   <li>{@code json-jackson} — JSON format using Jackson Databind</li>
 * </ul>
 *
 * <h2>Implementing a Custom Format Handler</h2>
 * <pre>{@code
 * public class YamlFormatHandler implements FormatHandler<YamlNode> {
 *     @Override
 *     public String formatId() { return "yaml"; }
 *
 *     @Override
 *     public String description() { return "YAML format"; }
 *
 *     @Override
 *     public String[] fileExtensions() { return new String[]{"yaml", "yml"}; }
 *
 *     @Override
 *     public DynamicOps<YamlNode> ops() { return YamlOps.INSTANCE; }
 *
 *     @Override
 *     public YamlNode parse(String content) {
 *         return yamlParser.parse(content);
 *     }
 *
 *     @Override
 *     public String serialize(YamlNode data) {
 *         return yamlSerializer.dump(data);
 *     }
 * }
 * }</pre>
 *
 * <p>Register in {@code META-INF/services/de.splatgames.aether.datafixers.cli.format.FormatHandler}</p>
 *
 * @param <T> the underlying data representation type
 * @author Erik Pfoertner
 * @since 0.3.0
 */
public interface FormatHandler<T> {

    /**
     * Returns the unique identifier for this format.
     *
     * <p>This ID is used on the command line via the {@code --format} option.</p>
     *
     * @return format identifier (e.g., "json-gson", "json-jackson", "yaml")
     */
    @NotNull
    String formatId();

    /**
     * Returns a human-readable description of this format.
     *
     * @return format description
     */
    @NotNull
    String description();

    /**
     * Returns the file extensions typically associated with this format.
     *
     * @return array of extensions without dots (e.g., ["json"])
     */
    @NotNull
    String[] fileExtensions();

    /**
     * Returns the {@link DynamicOps} for this format.
     *
     * @return the DynamicOps instance
     */
    @NotNull
    DynamicOps<T> ops();

    /**
     * Parses string content into the format's data representation.
     *
     * @param content the string content to parse
     * @return the parsed data
     * @throws FormatParseException if parsing fails
     */
    @NotNull
    T parse(@NotNull String content);

    /**
     * Serializes data to a compact string representation.
     *
     * @param data the data to serialize
     * @return the serialized string
     */
    @NotNull
    String serialize(@NotNull T data);

    /**
     * Serializes data with pretty printing enabled.
     *
     * <p>Default implementation delegates to {@link #serialize(Object)}.</p>
     *
     * @param data the data to serialize
     * @return the formatted string
     */
    @NotNull
    default String serializePretty(@NotNull T data) {
        return serialize(data);
    }
}
