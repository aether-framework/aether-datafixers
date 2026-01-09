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

/**
 * TOML format implementations for the Aether Datafixers codec module.
 *
 * <p>This package serves as the parent namespace for all TOML-based
 * {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} implementations
 * within the Aether Datafixers framework. It provides format-agnostic data manipulation
 * capabilities for TOML data, enabling the data fixing system to read, write, and
 * transform TOML configuration files.</p>
 *
 * <h2>Available Implementations</h2>
 * <table border="1">
 *   <caption>TOML DynamicOps Implementations</caption>
 *   <tr>
 *     <th>Package</th>
 *     <th>Class</th>
 *     <th>Data Type</th>
 *     <th>Library</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.codec.toml.jackson}</td>
 *     <td>{@link de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps}</td>
 *     <td>{@link com.fasterxml.jackson.databind.JsonNode}</td>
 *     <td>Jackson TOML Dataformat</td>
 *   </tr>
 * </table>
 *
 * <h2>TOML Format Considerations</h2>
 * <p>TOML (Tom's Obvious Minimal Language) has specific structural constraints that differ
 * from JSON and YAML. Understanding these constraints is essential when working with TOML data:</p>
 *
 * <h3>Structural Limitations</h3>
 * <ul>
 *   <li><strong>Top-Level Tables Only:</strong> TOML documents must have a table (object/map)
 *       at the root level. Primitive values or arrays cannot be at the top level.</li>
 *   <li><strong>No Null Values:</strong> TOML does not natively support null values.
 *       The implementation uses {@link com.fasterxml.jackson.databind.node.NullNode} internally,
 *       but null values cannot be serialized to valid TOML.</li>
 *   <li><strong>Homogeneous Arrays:</strong> Arrays in TOML should contain elements of the same type,
 *       though arrays of tables (inline tables) are supported.</li>
 *   <li><strong>Key Restrictions:</strong> Keys in TOML can be bare (alphanumeric and underscores),
 *       quoted, or dotted. Non-string keys are not supported.</li>
 * </ul>
 *
 * <h3>TOML-Specific Features</h3>
 * <ul>
 *   <li>Native date/time types (dates, times, datetimes with timezone)</li>
 *   <li>Multi-line basic and literal strings</li>
 *   <li>Inline tables for compact representation</li>
 *   <li>Array of tables syntax ({@code [[array.of.tables]]})</li>
 *   <li>Comments (lines starting with #)</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <p>TOML is particularly well-suited for:</p>
 * <ul>
 *   <li>Application configuration files</li>
 *   <li>Build system configurations (e.g., Cargo.toml, pyproject.toml)</li>
 *   <li>Settings files with clear structure</li>
 *   <li>Human-editable configuration that needs to be version-controlled</li>
 * </ul>
 *
 * <h2>Format Interoperability</h2>
 * <p>The TOML implementation can convert data to and from other formats using the
 * {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps#convertTo(DynamicOps, Object)}
 * method:</p>
 * <pre>{@code
 * // Convert TOML to JSON
 * JsonElement gsonElement = GsonOps.INSTANCE.convertTo(JacksonTomlOps.INSTANCE, tomlData);
 *
 * // Convert JSON to TOML (ensure root is an object)
 * JsonNode tomlData = JacksonTomlOps.INSTANCE.convertTo(GsonOps.INSTANCE, jsonData);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All implementations in this package hierarchy are thread-safe. The singleton
 * instance ({@code JacksonTomlOps.INSTANCE}) can be safely shared across multiple threads.</p>
 *
 * <h2>Dependencies</h2>
 * <p>This package requires Jackson TOML Dataformat as a runtime dependency:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.fasterxml.jackson.dataformat</groupId>
 *     <artifactId>jackson-dataformat-toml</artifactId>
 *     <version>2.15+</version>
 * </dependency>
 * }</pre>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.codec.toml;
