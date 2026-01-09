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

package de.splatgames.aether.datafixers.schematools.diff;

/**
 * Enumeration of difference types detected when comparing schemas.
 *
 * <p>This enum classifies the type of change detected between two versions
 * of a schema element (type or field).</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SchemaDiff diff = SchemaDiffer.compare(v1, v2).diff();
 *
 * for (TypeReference added : diff.addedTypes()) {
 *     // DiffKind.ADDED - type exists in v2 but not v1
 * }
 *
 * for (TypeReference removed : diff.removedTypes()) {
 *     // DiffKind.REMOVED - type exists in v1 but not v2
 * }
 *
 * diff.typeDiff(PLAYER).ifPresent(typeDiff -> {
 *     for (FieldDiff fieldDiff : typeDiff.fieldDiffs()) {
 *         switch (fieldDiff.kind()) {
 *             case ADDED -> System.out.println("New field: " + fieldDiff.fieldName());
 *             case REMOVED -> System.out.println("Removed field: " + fieldDiff.fieldName());
 *             case MODIFIED -> System.out.println("Changed field: " + fieldDiff.fieldName());
 *             case UNCHANGED -> {} // No change
 *         }
 *     }
 * });
 * }</pre>
 *
 * @author Erik Pförtner
 * @see SchemaDiff
 * @see TypeDiff
 * @see FieldDiff
 * @since 0.3.0
 */
public enum DiffKind {

    /**
     * The element was added in the target schema.
     *
     * <p>The element exists in the target (newer) schema but not in the
     * source (older) schema.</p>
     */
    ADDED,

    /**
     * The element was removed from the target schema.
     *
     * <p>The element exists in the source (older) schema but not in the
     * target (newer) schema.</p>
     */
    REMOVED,

    /**
     * The element exists in both schemas but has changed.
     *
     * <p>For types, this means the type structure (fields, codec, etc.) differs.
     * For fields, this could mean a type change, optionality change, or other
     * modification.</p>
     */
    MODIFIED,

    /**
     * The element exists in both schemas and is unchanged.
     *
     * <p>The element is structurally identical in both schemas.</p>
     */
    UNCHANGED
}
