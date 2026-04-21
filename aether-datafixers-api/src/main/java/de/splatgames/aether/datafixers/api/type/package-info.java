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
 * Type system for data definitions and serialization.
 *
 * <p>This package bridges {@link de.splatgames.aether.datafixers.api.TypeReference}
 * identifiers with their associated
 * {@link de.splatgames.aether.datafixers.api.codec.Codec} implementations.
 * Types are the runtime representation of data structures declared in a
 * schema — they are what the data fixer operates on when applying rewrite
 * rules.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.Type} — Associates a
 *       {@link de.splatgames.aether.datafixers.api.TypeReference} with a
 *       {@link de.splatgames.aether.datafixers.api.codec.Codec}. A {@code Type}
 *       knows how to serialize and deserialize its data.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.SimpleType} — Canonical
 *       {@code Type} implementation for standalone data structures.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.TypeRegistry} —
 *       Registry for looking up {@code Type}s by their {@code TypeReference}.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.Typed} — Pairs a value
 *       with its {@code Type}, enabling type-safe operations on runtime data.
 *       This is the primary value that flows through
 *       {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule}.</li>
 * </ul>
 *
 * <h2>Type and Codec Relationship</h2>
 * <pre>
 * TypeReference("player")
 *         &#x2502;
 *         &#x25BC;
 *     &#x250C;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2510;
 *     &#x2502;    Type    &#x2502;
 *     &#x2502;  +------+  &#x2502;
 *     &#x2502;  | Codec|  &#x2502;  &#x2190; Handles encode/decode
 *     &#x2502;  +------+  &#x2502;
 *     &#x2514;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2518;
 * </pre>
 *
 * <h2>Working with Types</h2>
 * <pre>{@code
 * // Look up a type in the schema's registry
 * TypeRegistry registry = schema.types();
 * Type<?> playerType = registry.require(TypeReferences.PLAYER);
 *
 * // Decode raw data using the type's codec
 * DataResult<Pair<?, JsonElement>> decoded =
 *     playerType.codec().decode(GsonOps.INSTANCE, json);
 *
 * // Wrap a value with its Type for rule-based transformations
 * Typed<JsonElement> typed = new Typed<>(playerType, json);
 * }</pre>
 *
 * <h2>Type Templates</h2>
 * <p>The {@link de.splatgames.aether.datafixers.api.type.template} sub-package
 * provides {@link de.splatgames.aether.datafixers.api.type.template.TypeTemplate},
 * the second-order recipe produced by the
 * {@link de.splatgames.aether.datafixers.api.dsl.DSL}. Templates are
 * instantiated into concrete {@link de.splatgames.aether.datafixers.api.type.Type}
 * instances when the owning schema is built.</p>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.template} — Type
 *       template definitions used by schemas and the DSL.</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.api.type.Type
 * @see de.splatgames.aether.datafixers.api.type.TypeRegistry
 * @see de.splatgames.aether.datafixers.api.type.Typed
 * @see de.splatgames.aether.datafixers.api.TypeReference
 * @see de.splatgames.aether.datafixers.api.codec.Codec
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.type;
