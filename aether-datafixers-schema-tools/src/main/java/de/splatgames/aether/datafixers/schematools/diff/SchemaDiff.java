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
import de.splatgames.aether.datafixers.api.schema.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable result of comparing two schemas.
 *
 * <p>A SchemaDiff captures all differences between a source (older) schema
 * and a target (newer) schema at both the type level and field level.</p>
 *
 * <h2>Diff Information</h2>
 * <ul>
 *   <li><b>Type-level:</b> Which types were added, removed, or exist in both schemas</li>
 *   <li><b>Field-level:</b> For types in both schemas, which fields changed
 *       (only when field-level diffing is enabled)</li>
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
 * System.out.println("Common types: " + diff.commonTypes());
 *
 * // Field-level changes for a specific type
 * diff.typeDiff(TypeReferences.PLAYER).ifPresent(typeDiff -> {
 *     System.out.println("Player field changes:");
 *     typeDiff.addedFields().forEach(f ->
 *         System.out.println("  + " + f.fieldName()));
 *     typeDiff.removedFields().forEach(f ->
 *         System.out.println("  - " + f.fieldName()));
 * });
 *
 * // Check if there are any changes
 * if (!diff.hasChanges()) {
 *     System.out.println("Schemas are identical");
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see SchemaDiffer
 * @see TypeDiff
 * @since 0.3.0
 */
public final class SchemaDiff {

    /**
     * The source (older) schema being compared from.
     */
    private final Schema source;

    /**
     * The target (newer) schema being compared to.
     */
    private final Schema target;

    /**
     * Type references that exist in the target but not in the source schema.
     * These represent newly introduced types.
     */
    private final Set<TypeReference> addedTypes;

    /**
     * Type references that exist in the source but not in the target schema.
     * These represent deprecated or removed types.
     */
    private final Set<TypeReference> removedTypes;

    /**
     * Type references that exist in both schemas (intersection).
     * These types may have field-level changes tracked in {@link #typeDiffs}.
     */
    private final Set<TypeReference> commonTypes;

    /**
     * Detailed field-level diffs for types that exist in both schemas.
     * This map is empty if field-level diffing was not enabled.
     */
    private final Map<TypeReference, TypeDiff> typeDiffs;

    /**
     * Creates a new immutable SchemaDiff instance.
     *
     * <p>This constructor is private; use {@link #builder(Schema, Schema)} to create instances.</p>
     *
     * @param source       the source schema, must not be {@code null}
     * @param target       the target schema, must not be {@code null}
     * @param addedTypes   types added in target, must not be {@code null}
     * @param removedTypes types removed from target, must not be {@code null}
     * @param commonTypes  types in both schemas, must not be {@code null}
     * @param typeDiffs    field-level diffs for common types, must not be {@code null}
     */
    private SchemaDiff(
            @NotNull final Schema source,
            @NotNull final Schema target,
            @NotNull final Set<TypeReference> addedTypes,
            @NotNull final Set<TypeReference> removedTypes,
            @NotNull final Set<TypeReference> commonTypes,
            @NotNull final Map<TypeReference, TypeDiff> typeDiffs
    ) {
        this.source = Preconditions.checkNotNull(source, "source must not be null");
        this.target = Preconditions.checkNotNull(target, "target must not be null");
        this.addedTypes = Set.copyOf(Preconditions.checkNotNull(addedTypes, "addedTypes must not be null"));
        this.removedTypes = Set.copyOf(Preconditions.checkNotNull(removedTypes, "removedTypes must not be null"));
        this.commonTypes = Set.copyOf(Preconditions.checkNotNull(commonTypes, "commonTypes must not be null"));
        this.typeDiffs = Map.copyOf(Preconditions.checkNotNull(typeDiffs, "typeDiffs must not be null"));
    }

    /**
     * Creates a builder for constructing SchemaDiff instances.
     *
     * @param source the source schema, must not be {@code null}
     * @param target the target schema, must not be {@code null}
     * @return a new builder, never {@code null}
     */
    @NotNull
    static Builder builder(@NotNull final Schema source, @NotNull final Schema target) {
        return new Builder(source, target);
    }

    /**
     * Returns the source (older) schema.
     *
     * @return the source schema, never {@code null}
     */
    @NotNull
    public Schema source() {
        return this.source;
    }

    /**
     * Returns the target (newer) schema.
     *
     * @return the target schema, never {@code null}
     */
    @NotNull
    public Schema target() {
        return this.target;
    }

    /**
     * Returns the types that were added in the target schema.
     *
     * <p>These types exist in the target but not in the source schema.</p>
     *
     * @return an unmodifiable set of added type references, never {@code null}
     */
    @NotNull
    public Set<TypeReference> addedTypes() {
        return this.addedTypes;
    }

    /**
     * Returns the types that were removed from the target schema.
     *
     * <p>These types exist in the source but not in the target schema.</p>
     *
     * @return an unmodifiable set of removed type references, never {@code null}
     */
    @NotNull
    public Set<TypeReference> removedTypes() {
        return this.removedTypes;
    }

    /**
     * Returns the types that exist in both schemas.
     *
     * @return an unmodifiable set of common type references, never {@code null}
     */
    @NotNull
    public Set<TypeReference> commonTypes() {
        return this.commonTypes;
    }

    /**
     * Returns detailed diffs for types that exist in both schemas.
     *
     * <p>This map only contains entries if field-level diffing was enabled
     * when creating the diff.</p>
     *
     * @return an unmodifiable map of type reference to type diff, never {@code null}
     */
    @NotNull
    public Map<TypeReference, TypeDiff> typeDiffs() {
        return this.typeDiffs;
    }

    /**
     * Returns the type diff for a specific type reference.
     *
     * @param reference the type reference to look up, must not be {@code null}
     * @return an Optional containing the type diff if found, empty otherwise
     * @throws NullPointerException if {@code reference} is {@code null}
     */
    @NotNull
    public Optional<TypeDiff> typeDiff(@NotNull final TypeReference reference) {
        Preconditions.checkNotNull(reference, "reference must not be null");
        return Optional.ofNullable(this.typeDiffs.get(reference));
    }

    /**
     * Checks if there are any differences between the schemas.
     *
     * @return {@code true} if there are added, removed, or modified types/fields
     */
    public boolean hasChanges() {
        return hasTypeChanges() || hasFieldChanges();
    }

    /**
     * Checks if there are type-level changes (added or removed types).
     *
     * @return {@code true} if types were added or removed
     */
    public boolean hasTypeChanges() {
        return !this.addedTypes.isEmpty() || !this.removedTypes.isEmpty();
    }

    /**
     * Checks if there are field-level changes in any type.
     *
     * @return {@code true} if any type has field changes
     */
    public boolean hasFieldChanges() {
        return this.typeDiffs.values().stream().anyMatch(TypeDiff::hasFieldChanges);
    }

    /**
     * Returns the total number of changes (added + removed + modified).
     *
     * @return the total change count
     */
    public int totalChangeCount() {
        int count = this.addedTypes.size() + this.removedTypes.size();
        for (final TypeDiff typeDiff : this.typeDiffs.values()) {
            count += typeDiff.changedFieldCount();
        }
        return count;
    }

    /**
     * Compares this schema diff to another object for equality.
     *
     * <p>Two {@code SchemaDiff} instances are equal if they compare the same
     * schema versions and have identical sets of added, removed, and common types,
     * as well as identical type-level diffs.</p>
     *
     * @param obj the object to compare with, may be {@code null}
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SchemaDiff other)) {
            return false;
        }
        return this.source.version().equals(other.source.version())
                && this.target.version().equals(other.target.version())
                && this.addedTypes.equals(other.addedTypes)
                && this.removedTypes.equals(other.removedTypes)
                && this.commonTypes.equals(other.commonTypes)
                && this.typeDiffs.equals(other.typeDiffs);
    }

    /**
     * Returns a hash code value for this schema diff.
     *
     * <p>The hash code is computed from the schema versions and all type-level
     * information to ensure consistency with {@link #equals(Object)}.</p>
     *
     * @return the hash code value for this schema diff
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                this.source.version(),
                this.target.version(),
                this.addedTypes,
                this.removedTypes,
                this.commonTypes,
                this.typeDiffs
        );
    }

    /**
     * Returns a string representation of this schema diff.
     *
     * <p>The format includes the source and target versions along with
     * counts of different change categories for debugging and logging purposes.</p>
     *
     * @return a human-readable string representation, never {@code null}
     */
    @Override
    public String toString() {
        return "SchemaDiff{" +
                "source=" + this.source.version() +
                ", target=" + this.target.version() +
                ", addedTypes=" + this.addedTypes.size() +
                ", removedTypes=" + this.removedTypes.size() +
                ", commonTypes=" + this.commonTypes.size() +
                ", typeDiffs=" + this.typeDiffs.size() +
                '}';
    }

    /**
     * Internal builder for constructing {@link SchemaDiff} instances.
     *
     * <p>This builder is package-private and used by {@link SchemaDiffer}
     * to construct diff results. External code should use {@link SchemaDiffer}
     * to create schema diffs.</p>
     *
     * @author Erik Pförtner
     * @since 0.3.0
     */
    static final class Builder {

        /**
         * The source schema (required).
         */
        private final Schema source;

        /**
         * The target schema (required).
         */
        private final Schema target;

        /**
         * Added types; defaults to empty set.
         */
        private Set<TypeReference> addedTypes = Set.of();

        /**
         * Removed types; defaults to empty set.
         */
        private Set<TypeReference> removedTypes = Set.of();

        /**
         * Common types; defaults to empty set.
         */
        private Set<TypeReference> commonTypes = Set.of();

        /**
         * Type diffs; defaults to empty map.
         */
        private Map<TypeReference, TypeDiff> typeDiffs = Map.of();

        /**
         * Creates a new builder for the specified schemas.
         *
         * @param source the source schema, must not be {@code null}
         * @param target the target schema, must not be {@code null}
         * @throws NullPointerException if any argument is {@code null}
         */
        private Builder(@NotNull final Schema source, @NotNull final Schema target) {
            this.source = Preconditions.checkNotNull(source, "source must not be null");
            this.target = Preconditions.checkNotNull(target, "target must not be null");
        }

        /**
         * Sets the added types.
         *
         * @param addedTypes types added in target schema, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if {@code addedTypes} is {@code null}
         */
        @NotNull
        Builder addedTypes(@NotNull final Set<TypeReference> addedTypes) {
            this.addedTypes = Preconditions.checkNotNull(addedTypes, "addedTypes must not be null");
            return this;
        }

        /**
         * Sets the removed types.
         *
         * @param removedTypes types removed from target schema, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if {@code removedTypes} is {@code null}
         */
        @NotNull
        Builder removedTypes(@NotNull final Set<TypeReference> removedTypes) {
            this.removedTypes = Preconditions.checkNotNull(removedTypes, "removedTypes must not be null");
            return this;
        }

        /**
         * Sets the common types.
         *
         * @param commonTypes types present in both schemas, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if {@code commonTypes} is {@code null}
         */
        @NotNull
        Builder commonTypes(@NotNull final Set<TypeReference> commonTypes) {
            this.commonTypes = Preconditions.checkNotNull(commonTypes, "commonTypes must not be null");
            return this;
        }

        /**
         * Sets the type-level diffs.
         *
         * @param typeDiffs map of type reference to detailed diff, must not be {@code null}
         * @return this builder for method chaining
         * @throws NullPointerException if {@code typeDiffs} is {@code null}
         */
        @NotNull
        Builder typeDiffs(@NotNull final Map<TypeReference, TypeDiff> typeDiffs) {
            this.typeDiffs = Preconditions.checkNotNull(typeDiffs, "typeDiffs must not be null");
            return this;
        }

        /**
         * Builds and returns a new immutable {@link SchemaDiff} instance.
         *
         * @return a new SchemaDiff with the configured values, never {@code null}
         */
        @NotNull
        SchemaDiff build() {
            return new SchemaDiff(
                    this.source,
                    this.target,
                    this.addedTypes,
                    this.removedTypes,
                    this.commonTypes,
                    this.typeDiffs
            );
        }
    }
}
