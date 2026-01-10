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
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Format handler for YAML using the SnakeYAML library.
 *
 * <p>This handler provides YAML parsing and serialization capabilities using
 * SnakeYAML's native Java type representation and integrates with Aether Datafixers
 * via {@link SnakeYamlOps}.</p>
 *
 * <h2>Format Details</h2>
 * <ul>
 *   <li><b>Format ID:</b> {@code yaml-snakeyaml}</li>
 *   <li><b>File Extensions:</b> {@code .yaml}, {@code .yml}</li>
 *   <li><b>Data Type:</b> {@link Object} (native Java types: Map, List, String, Number, Boolean)</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Compact serialization via {@link #serialize(Object)}</li>
 *   <li>Pretty-printed serialization via {@link #serializePretty(Object)} with block style</li>
 *   <li>Robust error handling with descriptive messages</li>
 *   <li>Native Java type representation for easy interoperability</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This handler is thread-safe. New {@link Yaml} instances are created for each
 * serialization operation to ensure thread safety.</p>
 *
 * @author Erik Pförtner
 * @see FormatHandler
 * @see SnakeYamlOps
 * @see YamlJacksonFormatHandler
 * @since 0.5.0
 */
public class YamlSnakeYamlFormatHandler implements FormatHandler<Object> {

    /**
     * Yaml instance for parsing.
     *
     * <p>Uses default SnakeYAML configuration.</p>
     */
    private final Yaml yaml;

    /**
     * DumperOptions for compact YAML serialization.
     *
     * <p>Uses flow style for compact output.</p>
     */
    private final DumperOptions compactOptions;

    /**
     * DumperOptions for pretty-printed YAML serialization.
     *
     * <p>Uses block style with indentation for human-readable output.</p>
     */
    private final DumperOptions prettyOptions;

    /**
     * Creates a new SnakeYAML format handler with default configuration.
     */
    public YamlSnakeYamlFormatHandler() {
        this.yaml = new Yaml();

        this.compactOptions = new DumperOptions();
        this.compactOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);

        this.prettyOptions = new DumperOptions();
        this.prettyOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.prettyOptions.setIndent(2);
        this.prettyOptions.setPrettyFlow(true);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "yaml-snakeyaml"}
     */
    @Override
    @NotNull
    public String formatId() {
        return "yaml-snakeyaml";
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code "YAML format using SnakeYAML"}
     */
    @Override
    @NotNull
    public String description() {
        return "YAML format using SnakeYAML";
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
     * @return {@link SnakeYamlOps#INSTANCE}
     */
    @Override
    @NotNull
    public DynamicOps<Object> ops() {
        return SnakeYamlOps.INSTANCE;
    }

    /**
     * Parses a YAML string into a native Java object representation.
     *
     * <p>This method validates the input and provides descriptive error messages:</p>
     * <ul>
     *   <li>Empty or whitespace-only content throws {@link FormatParseException}</li>
     *   <li>YAML that parses to null throws {@link FormatParseException}</li>
     *   <li>Invalid YAML syntax is wrapped in {@link FormatParseException}</li>
     * </ul>
     *
     * @param content the YAML string to parse, must not be {@code null}
     * @return the parsed Java object (Map, List, String, Number, or Boolean), never {@code null}
     * @throws FormatParseException if the content is empty, parses to null,
     *                              or contains invalid YAML syntax
     */
    @Override
    @NotNull
    public Object parse(@NotNull final String content) {
        Preconditions.checkNotNull(content, "content must not be null");

        if (content.isBlank()) {
            throw new FormatParseException("Cannot parse empty or whitespace-only content");
        }
        try {
            final Object result = this.yaml.load(content);
            if (result == null) {
                throw new FormatParseException("YAML parsed to null");
            }
            return result;
        } catch (final Exception e) {
            throw new FormatParseException("Failed to parse YAML: " + e.getMessage(), e);
        }
    }

    /**
     * Serializes a Java object to a compact YAML string.
     *
     * <p>The output uses flow style (inline maps and lists) for compact representation.</p>
     *
     * @param data the Java object to serialize, must not be {@code null}
     * @return the compact YAML string representation
     */
    @Override
    @NotNull
    public String serialize(@NotNull final Object data) {
        Preconditions.checkNotNull(data, "data must not be null");

        final Yaml compactYaml = new Yaml(this.compactOptions);
        return compactYaml.dump(data);
    }

    /**
     * Serializes a Java object to a pretty-printed YAML string.
     *
     * <p>The output uses block style with proper indentation for human readability.</p>
     *
     * @param data the Java object to serialize, must not be {@code null}
     * @return the formatted YAML string representation
     */
    @Override
    @NotNull
    public String serializePretty(@NotNull final Object data) {
        Preconditions.checkNotNull(data, "data must not be null");

        final Yaml prettyYaml = new Yaml(this.prettyOptions);
        return prettyYaml.dump(data);
    }
}
