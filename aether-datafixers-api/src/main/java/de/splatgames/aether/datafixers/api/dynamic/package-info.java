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
 * operate on any serialization format (JSON, NBT, YAML, etc.) without being coupled to a specific implementation. Data
 * is represented as a generic tree structure that can be traversed and manipulated uniformly.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.dynamic.Dynamic} - A wrapper
 *       that pairs a value with its {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps},
 *       enabling manipulation without knowing the underlying format. This is the
 *       primary type passed through data fixes.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} - The operations
 *       interface that defines how to read, write, and transform values in a specific
 *       format. Implementations exist for JSON, NBT, and other formats.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic} - A Dynamic
 *       that includes a {@link de.splatgames.aether.datafixers.api.TypeReference}
 *       to indicate what type of data it represents.</li>
 * </ul>
 *
 * <h2>The Dynamic Pattern</h2>
 * <p>The Dynamic wrapper enables format-agnostic data manipulation:</p>
 * <pre>{@code
 * // Create a Dynamic from JSON
 * Dynamic<JsonElement> jsonDynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
 *
 * // Read fields without knowing the format
 * Optional<String> name = jsonDynamic.get("name").asString();
 * Optional<Integer> level = jsonDynamic.get("level").asInt();
 *
 * // Modify fields
 * Dynamic<JsonElement> updated = jsonDynamic.set("level", level.orElse(0) + 1);
 *
 * // The same code works with NBT
 * Dynamic<NbtCompound> nbtDynamic = new Dynamic<>(NbtOps.INSTANCE, nbtCompound);
 * Optional<String> name = nbtDynamic.get("name").asString(); // Same API!
 * }</pre>
 *
 * <h2>DynamicOps Implementations</h2>
 * <p>The {@link de.splatgames.aether.datafixers.api.dynamic.DynamicOps} interface
 * must be implemented for each serialization format. Common implementations include:</p>
 * <ul>
 *   <li>GsonOps - For Gson/JSON</li>
 *   <li>NbtOps - For Minecraft NBT</li>
 *   <li>JsonOps - For generic JSON</li>
 * </ul>
 *
 * <h2>Data Flow</h2>
 * <pre>
 * Input Data (e.g., JSON file)
 *     ↓
 * DynamicOps.parse() → Dynamic&lt;T&gt;
 *     ↓
 * DataFixer.update() applies fixes
 *     ↓
 * Dynamic&lt;T&gt; → DynamicOps.serialize()
 *     ↓
 * Output Data
 * </pre>
 *
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see de.splatgames.aether.datafixers.api.dynamic.DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.dynamic;
