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
 * Domain-Specific Language (DSL) for defining type templates and schemas.
 *
 * <p>This package provides a fluent API for constructing type templates that
 * describe the structure of data at a particular schema version. The DSL enables
 * declarative type definitions that can be composed and referenced across schemas.</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.dsl.DSL} - The main entry point
 *       providing factory methods for all type template primitives and combinators.</li>
 * </ul>
 *
 * <h2>Type Template Primitives</h2>
 * <p>The DSL provides primitives for common data structures:</p>
 * <ul>
 *   <li>{@code DSL.string()} - String values</li>
 *   <li>{@code DSL.intType()} - Integer values</li>
 *   <li>{@code DSL.bool()} - Boolean values</li>
 *   <li>{@code DSL.optional(template)} - Optional/nullable values</li>
 *   <li>{@code DSL.list(template)} - List/array values</li>
 *   <li>{@code DSL.compoundList(keyTemplate, valueTemplate)} - Map structures</li>
 * </ul>
 *
 * <h2>Compound Types</h2>
 * <p>Complex structures are built using field combinators:</p>
 * <pre>{@code
 * // Define a player type with nested position
 * TypeTemplate playerTemplate = DSL.allWithRemainder(
 *     DSL.field("name", DSL.string()),
 *     DSL.field("level", DSL.intType()),
 *     DSL.field("position", DSL.allWithRemainder(
 *         DSL.field("x", DSL.doubleType()),
 *         DSL.field("y", DSL.doubleType()),
 *         DSL.field("z", DSL.doubleType())
 *     ))
 * );
 * }</pre>
 *
 * <h2>Type References</h2>
 * <p>The DSL supports referencing other registered types, enabling recursive
 * and cross-referenced type definitions:</p>
 * <pre>{@code
 * // Reference another type by its TypeReference
 * TypeTemplate entityTemplate = DSL.allWithRemainder(
 *     DSL.field("id", DSL.string()),
 *     DSL.field("inventory", DSL.list(DSL.ref(TypeReferences.ITEM)))
 * );
 * }</pre>
 *
 * <h2>Schema Integration</h2>
 * <p>Type templates are registered in schemas to define the data structure
 * at a particular version:</p>
 * <pre>{@code
 * public class Schema100 extends Schema {
 *     @Override
 *     protected void registerTypes() {
 *         registerType(TypeReferences.PLAYER, DSL.allWithRemainder(
 *             DSL.field("name", DSL.string()),
 *             DSL.field("xp", DSL.intType())
 *         ));
 *     }
 * }
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.api.dsl.DSL
 * @see de.splatgames.aether.datafixers.api.type.template.TypeTemplate
 * @see de.splatgames.aether.datafixers.api.schema.Schema
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.dsl;
