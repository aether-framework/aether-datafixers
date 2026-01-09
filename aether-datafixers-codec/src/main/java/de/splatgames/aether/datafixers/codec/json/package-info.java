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
 * JSON format implementations for the Aether Datafixers codec module.
 *
 * <p>This package serves as the parent namespace for all JSON-based
 * {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} implementations
 * within the Aether Datafixers framework. It provides format-agnostic data manipulation
 * capabilities for JSON data, enabling the data fixing system to read, write, and
 * transform JSON without coupling to specific JSON library APIs.</p>
 *
 * <h2>Available Implementations</h2>
 * <table border="1">
 *   <caption>JSON DynamicOps Implementations</caption>
 *   <tr>
 *     <th>Package</th>
 *     <th>Class</th>
 *     <th>Data Type</th>
 *     <th>Library</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.codec.json.gson}</td>
 *     <td>{@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps}</td>
 *     <td>{@link com.google.gson.JsonElement}</td>
 *     <td>Google Gson</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.codec.json.jackson}</td>
 *     <td>{@link de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps}</td>
 *     <td>{@link com.fasterxml.jackson.databind.JsonNode}</td>
 *     <td>Jackson Databind</td>
 *   </tr>
 * </table>
 *
 * <h2>Choosing an Implementation</h2>
 * <ul>
 *   <li><strong>GsonOps:</strong> Ideal when your project already uses Gson for JSON processing,
 *       or when you prefer Gson's simpler API. Gson is lightweight and has minimal dependencies.</li>
 *   <li><strong>JacksonJsonOps:</strong> Recommended when using Jackson for JSON processing,
 *       or when you need advanced features like custom ObjectMapper configurations,
 *       streaming support, or integration with other Jackson dataformat modules.</li>
 * </ul>
 *
 * <h2>Format Interoperability</h2>
 * <p>Both implementations can convert data to and from each other using the
 * {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps#convertTo(DynamicOps, Object)}
 * method. This enables seamless interoperability between different JSON libraries:</p>
 * <pre>{@code
 * // Convert from Gson to Jackson
 * JsonNode jacksonNode = JacksonJsonOps.INSTANCE.convertTo(GsonOps.INSTANCE, gsonElement);
 *
 * // Convert from Jackson to Gson
 * JsonElement gsonElement = GsonOps.INSTANCE.convertTo(JacksonJsonOps.INSTANCE, jacksonNode);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All implementations in this package hierarchy are thread-safe. The singleton
 * instances (e.g., {@code GsonOps.INSTANCE}, {@code JacksonJsonOps.INSTANCE}) can
 * be safely shared across multiple threads.</p>
 *
 * <h2>Dependencies</h2>
 * <p>Each sub-package requires its respective JSON library as a dependency:</p>
 * <ul>
 *   <li>{@code codec.json.gson} requires {@code com.google.code.gson:gson}</li>
 *   <li>{@code codec.json.jackson} requires {@code com.fasterxml.jackson.core:jackson-databind}</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see de.splatgames.aether.datafixers.codec.json.gson.GsonOps
 * @see de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.codec.json;
