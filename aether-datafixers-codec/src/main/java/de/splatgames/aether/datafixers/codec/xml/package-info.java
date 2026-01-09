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
 * XML format implementations for the Aether Datafixers codec module.
 *
 * <p>This package serves as the parent namespace for all XML-based
 * {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} implementations
 * within the Aether Datafixers framework. It provides format-agnostic data manipulation
 * capabilities for XML data, enabling the data fixing system to read, write, and
 * transform XML documents.</p>
 *
 * <h2>Available Implementations</h2>
 * <table border="1">
 *   <caption>XML DynamicOps Implementations</caption>
 *   <tr>
 *     <th>Package</th>
 *     <th>Class</th>
 *     <th>Data Type</th>
 *     <th>Library</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.codec.xml.jackson}</td>
 *     <td>{@link de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps}</td>
 *     <td>{@link com.fasterxml.jackson.databind.JsonNode}</td>
 *     <td>Jackson XML Dataformat</td>
 *   </tr>
 * </table>
 *
 * <h2>XML Format Considerations</h2>
 * <p>XML (Extensible Markup Language) has a fundamentally different data model compared to
 * JSON-like formats. Understanding these differences is crucial when working with XML data:</p>
 *
 * <h3>Structural Differences from JSON</h3>
 * <ul>
 *   <li><strong>Root Element Required:</strong> XML documents must have exactly one root element.
 *       This maps to an {@link com.fasterxml.jackson.databind.node.ObjectNode} in the tree model.</li>
 *   <li><strong>Attributes vs. Elements:</strong> XML distinguishes between attributes
 *       ({@code <elem attr="value"/>}) and child elements ({@code <elem><child>value</child></elem>}).
 *       Jackson XML handles this distinction through configuration.</li>
 *   <li><strong>No Native Array Type:</strong> XML represents collections through repeated elements
 *       with the same name. The mapper infers arrays from structure.</li>
 *   <li><strong>Mixed Content:</strong> XML elements can contain both text and child elements,
 *       which doesn't map cleanly to JSON structures.</li>
 *   <li><strong>Element Names Required:</strong> Unlike JSON object keys, XML element names
 *       have naming restrictions (no spaces, must start with letter or underscore).</li>
 * </ul>
 *
 * <h3>XML-Specific Features</h3>
 * <ul>
 *   <li>XML namespaces and namespace prefixes</li>
 *   <li>XML declarations and processing instructions</li>
 *   <li>CDATA sections for unescaped content</li>
 *   <li>Comments (though typically not preserved in tree model)</li>
 *   <li>Document Type Definitions (DTD) and XML Schema validation</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <p>XML format support is particularly useful for:</p>
 * <ul>
 *   <li>Legacy system integration (SOAP, enterprise systems)</li>
 *   <li>Configuration files (Maven POM, Android layouts, Spring XML)</li>
 *   <li>Document-oriented data (XHTML, DocBook, Office Open XML)</li>
 *   <li>Data interchange with XML-based APIs</li>
 *   <li>Migrating data from XML to other formats</li>
 * </ul>
 *
 * <h2>Format Interoperability</h2>
 * <p>The XML implementation can convert data to and from other formats using the
 * {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps#convertTo(DynamicOps, Object)}
 * method:</p>
 * <pre>{@code
 * // Convert XML to JSON
 * JsonElement gsonElement = GsonOps.INSTANCE.convertTo(JacksonXmlOps.INSTANCE, xmlData);
 *
 * // Convert JSON to XML (may require wrapping in root element)
 * JsonNode xmlData = JacksonXmlOps.INSTANCE.convertTo(GsonOps.INSTANCE, jsonData);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All implementations in this package hierarchy are thread-safe. The singleton
 * instance ({@code JacksonXmlOps.INSTANCE}) can be safely shared across multiple threads.</p>
 *
 * <h2>Dependencies</h2>
 * <p>This package requires Jackson XML Dataformat as a runtime dependency:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.fasterxml.jackson.dataformat</groupId>
 *     <artifactId>jackson-dataformat-xml</artifactId>
 *     <version>2.15+</version>
 * </dependency>
 * }</pre>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.codec.xml;
