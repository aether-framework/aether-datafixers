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

package de.splatgames.aether.datafixers.spring.config;

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;

/**
 * Enumeration of supported serialization formats for {@link DynamicOps}.
 *
 * <p>This enum represents the available JSON serialization libraries that can be
 * used as the underlying format for Aether Datafixers operations. The choice of
 * format determines which library's data structures are used internally for
 * representing and manipulating JSON data during migrations.</p>
 *
 * <h2>Format Selection</h2>
 * <p>The format can be configured using the {@code aether.datafixers.default-format}
 * property. When both Gson and Jackson are available on the classpath, this setting
 * determines which one is preferred.</p>
 *
 * <h2>Configuration Example</h2>
 * <pre>{@code
 * # application.yml
 * aether:
 *   datafixers:
 *     default-format: JACKSON
 *
 * # application.properties
 * aether.datafixers.default-format=JACKSON
 * }</pre>
 *
 * <h2>Library Dependencies</h2>
 * <p>Each format requires its corresponding library on the classpath:</p>
 * <ul>
 *   <li>{@link #GSON} - Requires {@code com.google.code.gson:gson}</li>
 *   <li>{@link #JACKSON} - Requires {@code com.fasterxml.jackson.core:jackson-databind}</li>
 * </ul>
 *
 * <h2>Auto-Detection</h2>
 * <p>If only one library is available, it will be used automatically regardless
 * of the configured format. The configured format only matters when both libraries
 * are present.</p>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.spring.AetherDataFixersProperties#getDefaultFormat()
 * @see de.splatgames.aether.datafixers.spring.autoconfigure.DynamicOpsAutoConfiguration
 * @since 0.4.0
 */
public enum DynamicOpsFormat {

    /**
     * Google Gson serialization format.
     *
     * <p>Uses {@code com.google.gson.JsonElement} as the underlying data type.
     * This format is provided by the {@code aether-datafixers-codec} module's
     * {@code GsonOps} implementation.</p>
     *
     * <h3>Characteristics</h3>
     * <ul>
     *   <li>Lightweight and simple API</li>
     *   <li>No external dependencies beyond Gson itself</li>
     *   <li>Good performance for most use cases</li>
     * </ul>
     *
     * <h3>Required Dependency</h3>
     * <pre>{@code
     * <dependency>
     *     <groupId>com.google.code.gson</groupId>
     *     <artifactId>gson</artifactId>
     * </dependency>
     * }</pre>
     *
     * @see de.splatgames.aether.datafixers.codec.json.gson.GsonOps
     */
    GSON,

    /**
     * Jackson Databind serialization format.
     *
     * <p>Uses {@code com.fasterxml.jackson.databind.JsonNode} as the underlying
     * data type. This format is provided by the {@code aether-datafixers-codec}
     * module's {@code JacksonJsonOps} implementation.</p>
     *
     * <h3>Characteristics</h3>
     * <ul>
     *   <li>Feature-rich with extensive customization options</li>
     *   <li>Supports streaming and incremental parsing</li>
     *   <li>Integrates well with Spring's default JSON handling</li>
     *   <li>Can reuse Spring's configured {@code ObjectMapper}</li>
     * </ul>
     *
     * <h3>Required Dependency</h3>
     * <pre>{@code
     * <dependency>
     *     <groupId>com.fasterxml.jackson.core</groupId>
     *     <artifactId>jackson-databind</artifactId>
     * </dependency>
     * }</pre>
     *
     * <h3>Spring Integration</h3>
     * <p>When using Jackson with Spring, the auto-configured {@code JacksonJsonOps}
     * will automatically use Spring's {@code ObjectMapper} if available, ensuring
     * consistent JSON handling across the application.</p>
     *
     * @see de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps
     */
    JACKSON,

    /**
     * Jackson YAML serialization format.
     *
     * <p>Uses {@code com.fasterxml.jackson.databind.JsonNode} as the underlying
     * data type with YAML serialization. This format is provided by the
     * {@code aether-datafixers-codec} module's {@code JacksonYamlOps} implementation.</p>
     *
     * <h3>Required Dependency</h3>
     * <pre>{@code
     * <dependency>
     *     <groupId>com.fasterxml.jackson.dataformat</groupId>
     *     <artifactId>jackson-dataformat-yaml</artifactId>
     * </dependency>
     * }</pre>
     *
     * @see de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps
     */
    JACKSON_YAML,

    /**
     * SnakeYAML serialization format.
     *
     * <p>Uses native Java types ({@code Map}, {@code List}, primitives) as the
     * underlying data representation. This format is provided by the
     * {@code aether-datafixers-codec} module's {@code SnakeYamlOps} implementation.</p>
     *
     * <h3>Characteristics</h3>
     * <ul>
     *   <li>Lightweight YAML processing</li>
     *   <li>Works with native Java types</li>
     *   <li>No Jackson dependency required</li>
     * </ul>
     *
     * <h3>Required Dependency</h3>
     * <pre>{@code
     * <dependency>
     *     <groupId>org.yaml</groupId>
     *     <artifactId>snakeyaml</artifactId>
     * </dependency>
     * }</pre>
     *
     * @see de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps
     */
    SNAKEYAML,

    /**
     * Jackson TOML serialization format.
     *
     * <p>Uses {@code com.fasterxml.jackson.databind.JsonNode} as the underlying
     * data type with TOML serialization. This format is provided by the
     * {@code aether-datafixers-codec} module's {@code JacksonTomlOps} implementation.</p>
     *
     * <h3>TOML Limitations</h3>
     * <ul>
     *   <li>Top-level value must be a table (object)</li>
     *   <li>Null values are not natively supported</li>
     * </ul>
     *
     * <h3>Required Dependency</h3>
     * <pre>{@code
     * <dependency>
     *     <groupId>com.fasterxml.jackson.dataformat</groupId>
     *     <artifactId>jackson-dataformat-toml</artifactId>
     * </dependency>
     * }</pre>
     *
     * @see de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps
     */
    JACKSON_TOML,

    /**
     * Jackson XML serialization format.
     *
     * <p>Uses {@code com.fasterxml.jackson.databind.JsonNode} as the underlying
     * data type with XML serialization. This format is provided by the
     * {@code aether-datafixers-codec} module's {@code JacksonXmlOps} implementation.</p>
     *
     * <h3>XML Considerations</h3>
     * <ul>
     *   <li>XML requires a root element</li>
     *   <li>XML attributes may be represented differently than elements</li>
     * </ul>
     *
     * <h3>Required Dependency</h3>
     * <pre>{@code
     * <dependency>
     *     <groupId>com.fasterxml.jackson.dataformat</groupId>
     *     <artifactId>jackson-dataformat-xml</artifactId>
     * </dependency>
     * }</pre>
     *
     * @see de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps
     */
    JACKSON_XML
}
