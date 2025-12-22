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
 * Type template definitions for schema construction.
 *
 * <p>This package provides the template abstraction for defining type structures
 * declaratively. Templates are blueprints that describe the shape of data without
 * being tied to a specific schema version. They are instantiated into concrete
 * {@link de.splatgames.aether.datafixers.api.type.Type} instances when a schema
 * is built.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.template.TypeTemplate} -
 *       A blueprint for a type structure. Templates are created using the
 *       {@link de.splatgames.aether.datafixers.api.dsl.DSL} and can be composed
 *       to describe complex nested structures.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.template.TypeFamily} -
 *       Represents a family of related types that share a common structure pattern,
 *       parameterized over different inner types.</li>
 * </ul>
 *
 * <h2>Template vs Type</h2>
 * <pre>
 * ┌─────────────────┐     instantiate()     ┌─────────────────┐
 * │  TypeTemplate   │ ─────────────────────►│      Type       │
 * │   (blueprint)   │                       │   (concrete)    │
 * └─────────────────┘                       └─────────────────┘
 *         │                                          │
 *         │ DSL.field("name", DSL.string())         │ has Codec
 *         │ DSL.list(DSL.intType())                 │ has Finder
 *         │ DSL.optional(...)                       │
 * </pre>
 *
 * <h2>Using Templates with DSL</h2>
 * <pre>{@code
 * // Define a template for player data
 * TypeTemplate playerTemplate = DSL.allWithRemainder(
 *     DSL.field("name", DSL.string()),
 *     DSL.field("level", DSL.intType()),
 *     DSL.field("inventory", DSL.list(DSL.ref(TypeReferences.ITEM))),
 *     DSL.field("position", DSL.allWithRemainder(
 *         DSL.field("x", DSL.doubleType()),
 *         DSL.field("y", DSL.doubleType()),
 *         DSL.field("z", DSL.doubleType())
 *     ))
 * );
 *
 * // Register in schema
 * registerType(TypeReferences.PLAYER, playerTemplate);
 * }</pre>
 *
 * <h2>Type Families</h2>
 * <p>Type families enable parameterized type definitions where the same structure
 * can be applied to different inner types:</p>
 * <pre>{@code
 * // A family of "container" types
 * TypeFamily containerFamily = new TypeFamily() {
 *     public Type<?> apply(Type<?> inner) {
 *         return createContainerType(inner);
 *     }
 * };
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.api.type.template.TypeTemplate
 * @see de.splatgames.aether.datafixers.api.type.template.TypeFamily
 * @see de.splatgames.aether.datafixers.api.dsl.DSL
 * @see de.splatgames.aether.datafixers.api.type.Type
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.type.template;
