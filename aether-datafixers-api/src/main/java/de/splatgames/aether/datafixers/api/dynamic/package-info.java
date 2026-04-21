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
 * Format-agnostic data representation for the data fixing system.
 *
 * <p>This package provides the core abstraction that enables the data fixer to
 * operate on any serialization format (JSON, NBT, YAML, etc.) without being
 * coupled to a specific implementation. Data is represented as a generic tree
 * structure that can be traversed and manipulated uniformly.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.dynamic.Dynamic} — Wrapper
 *       pairing a value with its
 *       {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps}. This
 *       is the primary type passed through data fixes. All mutators return
 *       new instances — {@code Dynamic} is immutable.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} —
 *       Operations interface that defines how to read, write, and transform
 *       values in a specific format. Implementations ship in the
 *       {@code aether-datafixers-codec} module for Gson and Jackson.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic} —
 *       A {@code Dynamic} annotated with a
 *       {@link de.splatgames.aether.datafixers.api.TypeReference} indicating
 *       what logical type the data represents.</li>
 * </ul>
 *
 * <h2>The Dynamic Pattern</h2>
 * <p>{@code Dynamic} values are immutable. Every accessor returns a
 * {@link de.splatgames.aether.datafixers.api.result.DataResult} (never a bare
 * {@code Optional}), and every mutator returns a new {@code Dynamic} sharing
 * the same {@code DynamicOps}. {@code set} requires the new value to be
 * another {@code Dynamic} with matching ops:</p>
 * <pre>{@code
 * // Create a Dynamic from a JsonElement
 * Dynamic<JsonElement> json = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
 *
 * // Read fields without knowing the format
 * String  name  = json.get("name").asString().result().orElse("unknown");
 * int     level = json.get("level").asInt().result().orElse(0);
 *
 * // Mutate fields (returns a new Dynamic)
 * Dynamic<JsonElement> bumped = json.set("level", json.createInt(level + 1));
 *
 * // The same API works against any backing format
 * Dynamic<JsonNode> jackson = new Dynamic<>(JacksonOps.INSTANCE, jsonNode);
 * String name2 = jackson.get("name").asString().result().orElse("unknown");
 * }</pre>
 *
 * <h2>DynamicOps Implementations</h2>
 * <p>The {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps}
 * interface must be implemented once per serialization format. The
 * {@code aether-datafixers-codec} module provides:</p>
 * <ul>
 *   <li>{@code de.splatgames.aether.datafixers.codec.gson.GsonOps} — Gson
 *       {@code JsonElement}.</li>
 *   <li>{@code de.splatgames.aether.datafixers.codec.jackson.JacksonOps} —
 *       Jackson {@code JsonNode}.</li>
 * </ul>
 * <p>Additional format adapters can be written by implementing
 * {@code DynamicOps} directly.</p>
 *
 * <h2>Data Flow</h2>
 * <pre>
 * Input bytes / string (e.g., JSON file)
 *     &#x2193;
 *   format parser (Gson, Jackson, ...)
 *     &#x2193;
 *   new Dynamic&lt;T&gt;(ops, value)
 *     &#x2193;
 *   DataFixer.update() applies fixes
 *     &#x2193;
 *   dynamic.getValue() + format serializer
 *     &#x2193;
 * Output bytes / string
 * </pre>
 *
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.dynamic;
