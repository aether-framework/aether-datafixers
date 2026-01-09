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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.type.Type;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Represents differences between two versions of a type within a schema.
 *
 * <p>A TypeDiff captures field-level changes for a specific type that exists
 * in both the source and target schemas. Types that are added or removed
 * entirely are tracked at the {@link SchemaDiff} level.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
 *     .includeFieldLevel(true)
 *     .diff();
 *
 * // Get type diff for PLAYER
 * diff.typeDiff(TypeReferences.PLAYER).ifPresent(typeDiff -> {
 *     System.out.println("Type: " + typeDiff.reference().getId());
 *
 *     // Check field changes
 *     System.out.println("Added fields: " + typeDiff.addedFields().size());
 *     System.out.println("Removed fields: " + typeDiff.removedFields().size());
 *     System.out.println("Modified fields: " + typeDiff.modifiedFields().size());
 *
 *     // Iterate all field diffs
 *     for (FieldDiff fieldDiff : typeDiff.fieldDiffs()) {
 *         System.out.println("  " + fieldDiff);
 *     }
 * });
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see SchemaDiff
 * @see FieldDiff
 * @since 0.3.0
 */
public final class TypeDiff {

    /**
     * The type reference identifying this type within the schema.
     */
    private final TypeReference reference;

    /**
     * The type definition from the source (older) schema.
     */
    private final Type<?> sourceType;

    /**
     * The type definition from the target (newer) schema.
     */
    private final Type<?> targetType;

    /**
     * The list of field-level differences between source and target types.
     * This list is immutable and may be empty if no field changes exist.
     */
    private final List<FieldDiff> fieldDiffs;

    /**
     * Creates a new TypeDiff instance with the specified parameters.
     *
     * <p>This constructor is private; use {@link #of} to create instances.</p>
     *
     * @param reference  the type reference, must not be {@code null}
     * @param sourceType the source type, must not be {@code null}
     * @param targetType the target type, must not be {@code null}
     * @param fieldDiffs the field diffs, must not be {@code null}
     */
    private TypeDiff(
            @NotNull final TypeReference reference,
            @NotNull final Type<?> sourceType,
            @NotNull final Type<?> targetType,
            @NotNull final List<FieldDiff> fieldDiffs
    ) {
        this.reference = Preconditions.checkNotNull(reference, "reference must not be null");
        this.sourceType = Preconditions.checkNotNull(sourceType, "sourceType must not be null");
        this.targetType = Preconditions.checkNotNull(targetType, "targetType must not be null");
        this.fieldDiffs = List.copyOf(Preconditions.checkNotNull(fieldDiffs, "fieldDiffs must not be null"));
    }

    /**
     * Creates a new TypeDiff.
     *
     * @param reference  the type reference, must not be {@code null}
     * @param sourceType the type from the source schema, must not be {@code null}
     * @param targetType the type from the target schema, must not be {@code null}
     * @param fieldDiffs the field-level differences, must not be {@code null}
     * @return a new TypeDiff instance, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static TypeDiff of(
            @NotNull final TypeReference reference,
            @NotNull final Type<?> sourceType,
            @NotNull final Type<?> targetType,
            @NotNull final List<FieldDiff> fieldDiffs
    ) {
        return new TypeDiff(reference, sourceType, targetType, fieldDiffs);
    }

    /**
     * Returns the type reference identifying this type.
     *
     * @return the type reference, never {@code null}
     */
    @NotNull
    public TypeReference reference() {
        return this.reference;
    }

    /**
     * Returns the type from the source schema.
     *
     * @return the source type, never {@code null}
     */
    @NotNull
    public Type<?> sourceType() {
        return this.sourceType;
    }

    /**
     * Returns the type from the target schema.
     *
     * @return the target type, never {@code null}
     */
    @NotNull
    public Type<?> targetType() {
        return this.targetType;
    }

    /**
     * Returns all field-level differences.
     *
     * @return an unmodifiable list of field diffs, never {@code null}
     */
    @NotNull
    public List<FieldDiff> fieldDiffs() {
        return this.fieldDiffs;
    }

    /**
     * Returns only the added fields.
     *
     * @return a list of field diffs with kind ADDED, never {@code null}
     */
    @NotNull
    public List<FieldDiff> addedFields() {
        return this.fieldDiffs.stream()
                .filter(diff -> diff.kind() == DiffKind.ADDED)
                .toList();
    }

    /**
     * Returns only the removed fields.
     *
     * @return a list of field diffs with kind REMOVED, never {@code null}
     */
    @NotNull
    public List<FieldDiff> removedFields() {
        return this.fieldDiffs.stream()
                .filter(diff -> diff.kind() == DiffKind.REMOVED)
                .toList();
    }

    /**
     * Returns only the modified fields.
     *
     * @return a list of field diffs with kind MODIFIED, never {@code null}
     */
    @NotNull
    public List<FieldDiff> modifiedFields() {
        return this.fieldDiffs.stream()
                .filter(diff -> diff.kind() == DiffKind.MODIFIED)
                .toList();
    }

    /**
     * Returns only the unchanged fields.
     *
     * @return a list of field diffs with kind UNCHANGED, never {@code null}
     */
    @NotNull
    public List<FieldDiff> unchangedFields() {
        return this.fieldDiffs.stream()
                .filter(diff -> diff.kind() == DiffKind.UNCHANGED)
                .toList();
    }

    /**
     * Checks if this type has any field-level changes.
     *
     * @return {@code true} if any fields were added, removed, or modified
     */
    public boolean hasFieldChanges() {
        return this.fieldDiffs.stream().anyMatch(FieldDiff::isChanged);
    }

    /**
     * Returns the number of fields that changed.
     *
     * @return the count of added, removed, and modified fields
     */
    public int changedFieldCount() {
        return (int) this.fieldDiffs.stream()
                .filter(FieldDiff::isChanged)
                .count();
    }

    /**
     * Compares this type diff to another object for equality.
     *
     * <p>Two {@code TypeDiff} instances are equal if they have the same
     * type reference and the same field diffs. Note that the source and target
     * type instances are not compared directly; only the reference and
     * field-level differences are considered.</p>
     *
     * @param obj the object to compare with, may be {@code null}
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TypeDiff other)) {
            return false;
        }
        return this.reference.equals(other.reference)
                && this.fieldDiffs.equals(other.fieldDiffs);
    }

    /**
     * Returns a hash code value for this type diff.
     *
     * <p>The hash code is computed from the type reference and field diffs
     * to ensure consistency with {@link #equals(Object)}.</p>
     *
     * @return the hash code value for this type diff
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.reference, this.fieldDiffs);
    }

    /**
     * Returns a string representation of this type diff.
     *
     * <p>The format includes the type reference and counts of different
     * field change categories for debugging and logging purposes.</p>
     *
     * @return a human-readable string representation, never {@code null}
     */
    @Override
    public String toString() {
        return "TypeDiff{" +
                "reference=" + this.reference +
                ", fieldDiffs=" + this.fieldDiffs.size() +
                ", added=" + addedFields().size() +
                ", removed=" + removedFields().size() +
                ", modified=" + modifiedFields().size() +
                '}';
    }
}
