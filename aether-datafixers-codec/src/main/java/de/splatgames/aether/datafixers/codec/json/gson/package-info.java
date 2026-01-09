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
 * Gson-based {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} implementation
 * for JSON data manipulation within the Aether Datafixers framework.
 *
 * <p>This package provides {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps},
 * a format-agnostic data manipulation implementation that works with Google Gson's
 * {@link com.google.gson.JsonElement} type hierarchy. It enables the data fixing system
 * to read, write, and transform JSON data without direct coupling to Gson-specific APIs.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Singleton Pattern:</strong> Use {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps#INSTANCE}
 *       for standard operations</li>
 *   <li><strong>Type-Safe Operations:</strong> Full support for JSON primitives, arrays, and objects</li>
 *   <li><strong>Immutable Operations:</strong> All modification methods return new instances,
 *       preserving original data integrity</li>
 *   <li><strong>Error Handling:</strong> Uses {@link de.splatgames.aether.datafixers.api.result.DataResult}
 *       for type-safe error propagation</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Usage with Codecs</h3>
 * <pre>{@code
 * // Parse JSON string to Gson element
 * JsonElement json = JsonParser.parseString("{\"name\": \"Player1\", \"score\": 100}");
 *
 * // Create a Dynamic wrapper for manipulation
 * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, json);
 *
 * // Access nested values
 * Optional<String> name = dynamic.get("name").asString().result();
 * Optional<Integer> score = dynamic.get("score").asNumber().result().map(Number::intValue);
 *
 * // Use with codecs for type-safe deserialization
 * DataResult<PlayerData> result = PlayerData.CODEC.parse(dynamic);
 * }</pre>
 *
 * <h3>Data Transformation</h3>
 * <pre>{@code
 * // Transform data using DynamicOps
 * GsonOps ops = GsonOps.INSTANCE;
 *
 * // Create primitive values
 * JsonElement stringValue = ops.createString("Hello");
 * JsonElement intValue = ops.createInt(42);
 * JsonElement boolValue = ops.createBoolean(true);
 *
 * // Create complex structures
 * JsonElement map = ops.createMap(Stream.of(
 *     Pair.of(ops.createString("key1"), ops.createInt(1)),
 *     Pair.of(ops.createString("key2"), ops.createInt(2))
 * ));
 *
 * JsonElement list = ops.createList(Stream.of(
 *     ops.createInt(1),
 *     ops.createInt(2),
 *     ops.createInt(3)
 * ));
 * }</pre>
 *
 * <h3>Format Conversion</h3>
 * <pre>{@code
 * // Convert from Jackson to Gson
 * JsonNode jacksonData = objectMapper.readTree(jsonString);
 * JsonElement gsonData = GsonOps.INSTANCE.convertTo(JacksonJsonOps.INSTANCE, jacksonData);
 *
 * // Convert to YAML
 * Object yamlData = SnakeYamlOps.INSTANCE.convertTo(GsonOps.INSTANCE, gsonData);
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <table border="1">
 *   <caption>Java to Gson Type Mapping</caption>
 *   <tr><th>Java Type</th><th>Gson Type</th></tr>
 *   <tr><td>{@code String}</td><td>{@link com.google.gson.JsonPrimitive}</td></tr>
 *   <tr><td>{@code int}, {@code long}, {@code float}, {@code double}</td><td>{@link com.google.gson.JsonPrimitive}</td></tr>
 *   <tr><td>{@code boolean}</td><td>{@link com.google.gson.JsonPrimitive}</td></tr>
 *   <tr><td>{@code null}</td><td>{@link com.google.gson.JsonNull}</td></tr>
 *   <tr><td>{@code List<T>}</td><td>{@link com.google.gson.JsonArray}</td></tr>
 *   <tr><td>{@code Map<String, T>}</td><td>{@link com.google.gson.JsonObject}</td></tr>
 * </table>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link de.splatgames.aether.datafixers.codec.json.gson.GsonOps} class is thread-safe.
 * The singleton instance can be safely shared across multiple threads. All operations
 * are stateless and create new instances rather than modifying existing data.</p>
 *
 * <h2>Dependencies</h2>
 * <p>This package requires Google Gson as a runtime dependency:</p>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.google.code.gson</groupId>
 *     <artifactId>gson</artifactId>
 *     <version>2.10+</version>
 * </dependency>
 * }</pre>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.codec.json.gson.GsonOps
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see com.google.gson.JsonElement
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.codec.json.gson;
