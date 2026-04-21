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
 * <p>This package provides the template abstraction for defining type
 * structures declaratively. Templates are second-order: they describe the
 * shape of data without binding it to a concrete
 * {@link de.splatgames.aether.datafixers.api.type.Type} until they are
 * instantiated against a
 * {@link de.splatgames.aether.datafixers.api.type.template.TypeFamily}.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.template.TypeTemplate} —
 *       A blueprint for a type structure. Templates are produced by the
 *       {@link de.splatgames.aether.datafixers.api.dsl.DSL} factory and can be
 *       composed to describe arbitrarily nested structures.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.template.TypeFamily} —
 *       Provides the type parameters (by integer index) used by a template at
 *       instantiation time. Use
 *       {@link de.splatgames.aether.datafixers.api.type.template.TypeFamily#empty() TypeFamily.empty()}
 *       for non-parameterised templates,
 *       {@link de.splatgames.aether.datafixers.api.type.template.TypeFamily#of(de.splatgames.aether.datafixers.api.type.Type...) TypeFamily.of}
 *       for fixed parameter lists, and
 *       {@link de.splatgames.aether.datafixers.api.type.template.TypeFamily#recursive(java.util.function.Function) TypeFamily.recursive}
 *       for self-referential types.</li>
 * </ul>
 *
 * <h2>Template vs Type</h2>
 * <pre>
 *   +----------------+   apply(family)   +----------------+
 *   |  TypeTemplate  | &#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2500;&#x2192;|      Type      |
 *   |   (blueprint)  |                   |   (concrete)   |
 *   +----------------+                   +----------------+
 *           &#x2502;                                  &#x2502;
 *           &#x2514;&#x2500; DSL.and(DSL.field("name",         &#x2514;&#x2500; has a Codec
 *               DSL.string()), ...)
 * </pre>
 *
 * <h2>Using Templates with DSL</h2>
 * <p>Templates are always produced through
 * {@link de.splatgames.aether.datafixers.api.dsl.DSL}, never by
 * instantiating {@code TypeTemplate} directly:</p>
 * <pre>{@code
 * TypeTemplate playerTemplate = DSL.and(
 *     DSL.field("name",  DSL.string()),
 *     DSL.field("level", DSL.intType()),
 *     DSL.field("position", DSL.and(
 *         DSL.field("x", DSL.doubleType()),
 *         DSL.field("y", DSL.doubleType()),
 *         DSL.field("z", DSL.doubleType())
 *     )),
 *     DSL.remainder()
 * );
 *
 * // Register the template against a TypeReference inside a Schema
 * registerType(TypeReferences.PLAYER, playerTemplate);
 *
 * // Or instantiate it manually for one-off use:
 * Type<?> playerType = playerTemplate.apply(TypeFamily.empty());
 * }</pre>
 *
 * <h2>Type Families</h2>
 * <p>Type families expose type parameters to a template by integer index.
 * Recursive families (for trees, linked lists, etc.) supply their own
 * self-reference via {@link de.splatgames.aether.datafixers.api.type.template.TypeFamily#recursive(java.util.function.Function)}.</p>
 * <pre>{@code
 * // Linked-list-like recursive template using the self reference
 * TypeFamily listFamily = TypeFamily.recursive(self -> Type.product(
 *     Type.field("value", Type.INT),
 *     Type.field("next",  Type.optional(self.apply(0)))
 * ));
 * Type<?> linkedListType = listFamily.apply(0);
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.api.type.template.TypeTemplate
 * @see de.splatgames.aether.datafixers.api.type.template.TypeFamily
 * @see de.splatgames.aether.datafixers.api.dsl.DSL
 * @see de.splatgames.aether.datafixers.api.type.Type
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.type.template;
