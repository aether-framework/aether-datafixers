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
import de.splatgames.aether.datafixers.schematools.introspection.FieldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents differences between two versions of a field.
 *
 * <p>A FieldDiff captures changes to a specific field between two schema versions,
 * including whether the field was added, removed, modified, or unchanged.</p>
 *
 * <h2>Field States</h2>
 * <ul>
 *   <li>{@link DiffKind#ADDED} - {@code sourceField} is {@code null}, {@code targetField} is present</li>
 *   <li>{@link DiffKind#REMOVED} - {@code sourceField} is present, {@code targetField} is {@code null}</li>
 *   <li>{@link DiffKind#MODIFIED} - Both fields present but different (type, optionality)</li>
 *   <li>{@link DiffKind#UNCHANGED} - Both fields present and structurally identical</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * TypeDiff typeDiff = schemaDiff.typeDiff(PLAYER).orElseThrow();
 *
 * for (FieldDiff fieldDiff : typeDiff.fieldDiffs()) {
 *     System.out.println(fieldDiff.fieldName() + ": " + fieldDiff.kind());
 *
 *     if (fieldDiff.kind() == DiffKind.MODIFIED) {
 *         FieldInfo before = fieldDiff.sourceField();
 *         FieldInfo after = fieldDiff.targetField();
 *         System.out.println("  Before: " + before.fieldType().describe());
 *         System.out.println("  After:  " + after.fieldType().describe());
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see TypeDiff
 * @see FieldInfo
 * @since 0.3.0
 */
public final class FieldDiff {

    /**
     * The name of the field being compared.
     * This is always non-null regardless of diff kind.
     */
    private final String fieldName;

    /**
     * The kind of difference detected (ADDED, REMOVED, MODIFIED, or UNCHANGED).
     */
    private final DiffKind kind;

    /**
     * The field from the source (older) schema, or {@code null} if the field was added.
     */
    private final FieldInfo sourceField;

    /**
     * The field from the target (newer) schema, or {@code null} if the field was removed.
     */
    private final FieldInfo targetField;

    /**
     * Creates a new FieldDiff instance with the specified parameters.
     *
     * <p>This constructor is private to enforce the use of factory methods
     * ({@link #added}, {@link #removed}, {@link #modified}, {@link #unchanged}, {@link #compare})
     * which provide better semantics and validation.</p>
     *
     * @param fieldName   the field name, must not be {@code null}
     * @param kind        the diff kind, must not be {@code null}
     * @param sourceField the source field, may be {@code null} for ADDED diffs
     * @param targetField the target field, may be {@code null} for REMOVED diffs
     */
    private FieldDiff(
            @NotNull final String fieldName,
            @NotNull final DiffKind kind,
            @Nullable final FieldInfo sourceField,
            @Nullable final FieldInfo targetField
    ) {
        this.fieldName = Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        this.kind = Preconditions.checkNotNull(kind, "kind must not be null");
        this.sourceField = sourceField;
        this.targetField = targetField;
    }

    /**
     * Creates a FieldDiff for an added field.
     *
     * @param targetField the field that was added, must not be {@code null}
     * @return a new FieldDiff with kind ADDED, never {@code null}
     * @throws NullPointerException if {@code targetField} is {@code null}
     */
    @NotNull
    public static FieldDiff added(@NotNull final FieldInfo targetField) {
        Preconditions.checkNotNull(targetField, "targetField must not be null");
        return new FieldDiff(targetField.name(), DiffKind.ADDED, null, targetField);
    }

    /**
     * Creates a FieldDiff for a removed field.
     *
     * @param sourceField the field that was removed, must not be {@code null}
     * @return a new FieldDiff with kind REMOVED, never {@code null}
     * @throws NullPointerException if {@code sourceField} is {@code null}
     */
    @NotNull
    public static FieldDiff removed(@NotNull final FieldInfo sourceField) {
        Preconditions.checkNotNull(sourceField, "sourceField must not be null");
        return new FieldDiff(sourceField.name(), DiffKind.REMOVED, sourceField, null);
    }

    /**
     * Creates a FieldDiff for a modified field.
     *
     * @param sourceField the field in the source schema, must not be {@code null}
     * @param targetField the field in the target schema, must not be {@code null}
     * @return a new FieldDiff with kind MODIFIED, never {@code null}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if field names don't match
     */
    @NotNull
    public static FieldDiff modified(
            @NotNull final FieldInfo sourceField,
            @NotNull final FieldInfo targetField
    ) {
        Preconditions.checkNotNull(sourceField, "sourceField must not be null");
        Preconditions.checkNotNull(targetField, "targetField must not be null");
        Preconditions.checkArgument(
                sourceField.name().equals(targetField.name()),
                "Field names must match for modified diff: %s vs %s",
                sourceField.name(), targetField.name()
        );
        return new FieldDiff(sourceField.name(), DiffKind.MODIFIED, sourceField, targetField);
    }

    /**
     * Creates a FieldDiff for an unchanged field.
     *
     * @param sourceField the field in the source schema, must not be {@code null}
     * @param targetField the field in the target schema, must not be {@code null}
     * @return a new FieldDiff with kind UNCHANGED, never {@code null}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if field names don't match
     */
    @NotNull
    public static FieldDiff unchanged(
            @NotNull final FieldInfo sourceField,
            @NotNull final FieldInfo targetField
    ) {
        Preconditions.checkNotNull(sourceField, "sourceField must not be null");
        Preconditions.checkNotNull(targetField, "targetField must not be null");
        Preconditions.checkArgument(
                sourceField.name().equals(targetField.name()),
                "Field names must match for unchanged diff: %s vs %s",
                sourceField.name(), targetField.name()
        );
        return new FieldDiff(sourceField.name(), DiffKind.UNCHANGED, sourceField, targetField);
    }

    /**
     * Compares two fields and creates an appropriate FieldDiff.
     *
     * <p>This method determines the diff kind by comparing the fields'
     * types and optionality.</p>
     *
     * @param sourceField the field in the source schema, must not be {@code null}
     * @param targetField the field in the target schema, must not be {@code null}
     * @return a FieldDiff with appropriate kind, never {@code null}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if field names don't match
     */
    @NotNull
    public static FieldDiff compare(
            @NotNull final FieldInfo sourceField,
            @NotNull final FieldInfo targetField
    ) {
        Preconditions.checkNotNull(sourceField, "sourceField must not be null");
        Preconditions.checkNotNull(targetField, "targetField must not be null");
        Preconditions.checkArgument(
                sourceField.name().equals(targetField.name()),
                "Field names must match: %s vs %s",
                sourceField.name(), targetField.name()
        );

        // Check if fields are structurally equal
        final boolean sameType = sourceField.fieldType().reference()
                .equals(targetField.fieldType().reference());
        final boolean sameOptional = sourceField.isOptional() == targetField.isOptional();

        if (sameType && sameOptional) {
            return unchanged(sourceField, targetField);
        } else {
            return modified(sourceField, targetField);
        }
    }

    /**
     * Returns the field name.
     *
     * @return the field name, never {@code null}
     */
    @NotNull
    public String fieldName() {
        return this.fieldName;
    }

    /**
     * Returns the kind of difference.
     *
     * @return the diff kind, never {@code null}
     */
    @NotNull
    public DiffKind kind() {
        return this.kind;
    }

    /**
     * Returns the field from the source schema.
     *
     * <p>Returns {@code null} for {@link DiffKind#ADDED} diffs.</p>
     *
     * @return the source field, or {@code null} if the field was added
     */
    @Nullable
    public FieldInfo sourceField() {
        return this.sourceField;
    }

    /**
     * Returns the field from the target schema.
     *
     * <p>Returns {@code null} for {@link DiffKind#REMOVED} diffs.</p>
     *
     * @return the target field, or {@code null} if the field was removed
     */
    @Nullable
    public FieldInfo targetField() {
        return this.targetField;
    }

    /**
     * Checks if this diff represents a change (added, removed, or modified).
     *
     * @return {@code true} if the field changed, {@code false} if unchanged
     */
    public boolean isChanged() {
        return this.kind != DiffKind.UNCHANGED;
    }

    /**
     * Compares this field diff to another object for equality.
     *
     * <p>Two {@code FieldDiff} instances are equal if they have the same
     * field name, diff kind, source field, and target field.</p>
     *
     * @param obj the object to compare with, may be {@code null}
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FieldDiff other)) {
            return false;
        }
        return this.kind == other.kind
                && this.fieldName.equals(other.fieldName)
                && Objects.equals(this.sourceField, other.sourceField)
                && Objects.equals(this.targetField, other.targetField);
    }

    /**
     * Returns a hash code value for this field diff.
     *
     * <p>The hash code is computed from all properties to ensure
     * consistency with {@link #equals(Object)}.</p>
     *
     * @return the hash code value for this field diff
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.fieldName, this.kind, this.sourceField, this.targetField);
    }

    /**
     * Returns a string representation of this field diff.
     *
     * <p>The format uses prefixes to indicate the diff kind:</p>
     * <ul>
     *   <li>{@code +fieldName: type} for added fields</li>
     *   <li>{@code -fieldName: type} for removed fields</li>
     *   <li>{@code ~fieldName: oldType -> newType} for modified fields</li>
     *   <li>{@code =fieldName} for unchanged fields</li>
     * </ul>
     *
     * @return a human-readable string representation, never {@code null}
     */
    @Override
    public String toString() {
        return switch (this.kind) {
            case ADDED -> "+" + this.fieldName + ": " + this.targetField.fieldType().describe();
            case REMOVED -> "-" + this.fieldName + ": " + this.sourceField.fieldType().describe();
            case MODIFIED -> "~" + this.fieldName + ": " + this.sourceField.fieldType().describe()
                    + " -> " + this.targetField.fieldType().describe();
            case UNCHANGED -> "=" + this.fieldName;
        };
    }
}
