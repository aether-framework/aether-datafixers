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
 * Type introspection utilities for extracting structural information from types.
 *
 * <p>This package provides tools for analyzing the structure of types defined
 * in schemas, including field enumeration and type hierarchy traversal.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.introspection.TypeIntrospector} -
 *       Entry point for type introspection</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.introspection.TypeStructure} -
 *       Structural representation of a type</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.introspection.FieldInfo} -
 *       Metadata about individual fields</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Type<?> playerType = schema.require(TypeReferences.PLAYER);
 * TypeStructure structure = TypeIntrospector.introspect(playerType);
 *
 * for (FieldInfo field : structure.fields()) {
 *     System.out.println(field.name() + ": " + field.fieldType().kind());
 * }
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.schematools.introspection.TypeIntrospector
 * @since 0.3.0
 */
package de.splatgames.aether.datafixers.schematools.introspection;
