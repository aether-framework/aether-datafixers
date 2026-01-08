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
 * Schema comparison and diff utilities.
 *
 * <p>This package provides tools for comparing two schemas and identifying
 * differences at both the type level and field level.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.diff.SchemaDiffer} -
 *       Fluent entry point for schema comparison</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.diff.SchemaDiff} -
 *       Result of comparing two schemas</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.diff.TypeDiff} -
 *       Differences for a specific type</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.diff.FieldDiff} -
 *       Differences for a specific field</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.diff.DiffKind} -
 *       Enumeration of difference types (ADDED, REMOVED, MODIFIED)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
 *     .includeFieldLevel(true)
 *     .diff();
 *
 * // Type-level changes
 * System.out.println("Added types: " + diff.addedTypes());
 * System.out.println("Removed types: " + diff.removedTypes());
 *
 * // Field-level changes for a specific type
 * diff.typeDiff(TypeReferences.PLAYER).ifPresent(typeDiff -> {
 *     typeDiff.addedFields().forEach(f ->
 *         System.out.println("Added field: " + f.fieldName()));
 * });
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.schematools.diff.SchemaDiffer
 * @since 0.3.0
 */
package de.splatgames.aether.datafixers.schematools.diff;
