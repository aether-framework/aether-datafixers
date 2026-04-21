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
 * Domain-Specific Language (DSL) for defining type templates.
 *
 * <p>This package provides a fluent API for constructing
 * {@link de.splatgames.aether.datafixers.api.type.template.TypeTemplate TypeTemplate}s
 * that describe the structure of data at a particular schema version. Type
 * templates are second-order: they are instantiated against a
 * {@link de.splatgames.aether.datafixers.api.type.template.TypeFamily} to
 * produce concrete {@link de.splatgames.aether.datafixers.api.type.Type}
 * instances.</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.dsl.DSL} — Factory class
 *       holding all primitive, compound, field, advanced, and optic-related
 *       combinators. {@code DSL} is the canonical entry point; do not
 *       instantiate {@code TypeTemplate}s by hand.</li>
 * </ul>
 *
 * <h2>Primitive Templates</h2>
 * <p>Primitives correspond to the value types supported by
 * {@link de.splatgames.aether.datafixers.api.dynamic.Dynamic}:</p>
 * <ul>
 *   <li>{@code DSL.string()}, {@code DSL.bool()}</li>
 *   <li>{@code DSL.byteType()}, {@code DSL.shortType()}, {@code DSL.intType()},
 *       {@code DSL.longType()}</li>
 *   <li>{@code DSL.floatType()}, {@code DSL.doubleType()}</li>
 * </ul>
 *
 * <h2>Compound and Field Templates</h2>
 * <p>Compound templates are assembled with the {@code and}/{@code or} product
 * and sum combinators, and fields are declared with {@code field} /
 * {@code optionalField}. Use {@link de.splatgames.aether.datafixers.api.dsl.DSL#remainder()}
 * to capture fields not covered by an explicit declaration:</p>
 * <pre>{@code
 * TypeTemplate playerTemplate = DSL.and(
 *     DSL.field("name",  DSL.string()),
 *     DSL.field("level", DSL.intType()),
 *     DSL.field("position", DSL.and(
 *         DSL.field("x", DSL.doubleType()),
 *         DSL.field("y", DSL.doubleType()),
 *         DSL.field("z", DSL.doubleType())
 *     )),
 *     DSL.optionalField("inventory", DSL.list(DSL.string())),
 *     DSL.remainder()
 * );
 *
 * // Instantiate against a type family to get a concrete Type<?>
 * Type<?> playerType = playerTemplate.apply(TypeFamily.empty());
 * }</pre>
 *
 * <h2>Discriminated Unions and Recursion</h2>
 * <p>Use {@link de.splatgames.aether.datafixers.api.dsl.DSL#taggedChoice(String, java.util.Map) taggedChoice}
 * for tagged sum types (e.g. polymorphic entities), and
 * {@link de.splatgames.aether.datafixers.api.dsl.DSL#named(String, de.splatgames.aether.datafixers.api.type.template.TypeTemplate) named}
 * + {@link de.splatgames.aether.datafixers.api.dsl.DSL#recursive(String, java.util.function.Function) recursive}
 * for self-referential data (e.g. trees, linked lists).</p>
 *
 * <h2>Optic Helpers</h2>
 * <p>The DSL also exposes {@link de.splatgames.aether.datafixers.api.dsl.DSL#fieldFinder(String) fieldFinder},
 * {@link de.splatgames.aether.datafixers.api.dsl.DSL#indexFinder(int) indexFinder}, and
 * {@link de.splatgames.aether.datafixers.api.dsl.DSL#remainderFinder(String...) remainderFinder}
 * for building {@link de.splatgames.aether.datafixers.api.optic.Finder}
 * instances used by the rewrite rules.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>All factory methods return immutable, thread-safe values. Templates can
 * be constructed once at startup and shared freely.</p>
 *
 * @see de.splatgames.aether.datafixers.api.dsl.DSL
 * @see de.splatgames.aether.datafixers.api.type.template.TypeTemplate
 * @see de.splatgames.aether.datafixers.api.type.template.TypeFamily
 * @see de.splatgames.aether.datafixers.api.schema.Schema
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.dsl;
