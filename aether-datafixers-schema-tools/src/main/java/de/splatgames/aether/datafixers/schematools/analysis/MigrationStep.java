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

package de.splatgames.aether.datafixers.schematools.analysis;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.schematools.diff.SchemaDiff;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a single step in a migration path.
 *
 * <p>A migration step corresponds to a schema version transition, optionally
 * associated with a DataFix that performs the actual transformation. Each step
 * includes information about:</p>
 * <ul>
 *   <li>Source and target versions</li>
 *   <li>The DataFix applied (if any)</li>
 *   <li>Schema differences between versions</li>
 *   <li>Types affected by this step</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(1).to(5)
 *     .analyze();
 *
 * for (MigrationStep step : path.steps()) {
 *     System.out.println("Version " + step.sourceVersion() + " -> " + step.targetVersion());
 *     step.fix().ifPresent(fix -> System.out.println("  Fix: " + fix.getClass().getSimpleName()));
 *     System.out.println("  Affected types: " + step.affectedTypes());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see MigrationPath
 * @see MigrationAnalyzer
 * @since 0.3.0
 */
public final class MigrationStep {

    /**
     * The version from which this migration step starts.
     */
    private final DataVersion sourceVersion;

    /**
     * The version to which this migration step migrates data.
     */
    private final DataVersion targetVersion;

    /**
     * The DataFix applied in this step, or {@code null} if no fix applies.
     */
    private final DataFix<?> fix;

    /**
     * The schema diff between source and target versions, or {@code null} if not computed.
     */
    private final SchemaDiff schemaDiff;

    /**
     * The set of type references affected by this migration step.
     * This is an immutable copy of the provided set.
     */
    private final Set<TypeReference> affectedTypes;

    /**
     * Creates a new immutable MigrationStep instance.
     *
     * <p>This constructor is private; use {@link #withFix}, {@link #withoutFix},
     * or {@link #builder} to create instances.</p>
     *
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @param fix           the DataFix for this step, may be {@code null}
     * @param schemaDiff    the schema diff, may be {@code null}
     * @param affectedTypes the affected types, must not be {@code null}
     */
    private MigrationStep(
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @Nullable final DataFix<?> fix,
            @Nullable final SchemaDiff schemaDiff,
            @NotNull final Set<TypeReference> affectedTypes
    ) {
        this.sourceVersion = Preconditions.checkNotNull(sourceVersion, "sourceVersion must not be null");
        this.targetVersion = Preconditions.checkNotNull(targetVersion, "targetVersion must not be null");
        this.fix = fix;
        this.schemaDiff = schemaDiff;
        this.affectedTypes = Set.copyOf(Preconditions.checkNotNull(affectedTypes, "affectedTypes must not be null"));
    }

    /**
     * Creates a migration step with a fix.
     *
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @param fix           the DataFix applied, must not be {@code null}
     * @param affectedTypes the types affected by this fix, must not be {@code null}
     * @return a new migration step, never {@code null}
     */
    @NotNull
    public static MigrationStep withFix(
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @NotNull final DataFix<?> fix,
            @NotNull final Set<TypeReference> affectedTypes
    ) {
        Preconditions.checkNotNull(fix, "fix must not be null");
        return new MigrationStep(sourceVersion, targetVersion, fix, null, affectedTypes);
    }

    /**
     * Creates a migration step without a fix (schema change only).
     *
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @param schemaDiff    the schema diff for this step, may be {@code null}
     * @param affectedTypes the types changed in this version, must not be {@code null}
     * @return a new migration step, never {@code null}
     */
    @NotNull
    public static MigrationStep withoutFix(
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @Nullable final SchemaDiff schemaDiff,
            @NotNull final Set<TypeReference> affectedTypes
    ) {
        return new MigrationStep(sourceVersion, targetVersion, null, schemaDiff, affectedTypes);
    }

    /**
     * Creates a builder for constructing migration steps.
     *
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @return a new builder, never {@code null}
     */
    @NotNull
    public static Builder builder(
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion
    ) {
        return new Builder(sourceVersion, targetVersion);
    }

    /**
     * Returns the source version of this step.
     *
     * @return the source version, never {@code null}
     */
    @NotNull
    public DataVersion sourceVersion() {
        return this.sourceVersion;
    }

    /**
     * Returns the target version of this step.
     *
     * @return the target version, never {@code null}
     */
    @NotNull
    public DataVersion targetVersion() {
        return this.targetVersion;
    }

    /**
     * Returns the DataFix applied in this step, if any.
     *
     * @return an Optional containing the fix, or empty if no fix is applied
     */
    @NotNull
    public Optional<DataFix<?>> fix() {
        return Optional.ofNullable(this.fix);
    }

    /**
     * Returns the schema diff for this step, if available.
     *
     * @return an Optional containing the diff, or empty if not computed
     */
    @NotNull
    public Optional<SchemaDiff> schemaDiff() {
        return Optional.ofNullable(this.schemaDiff);
    }

    /**
     * Returns the types affected by this step.
     *
     * @return an unmodifiable set of affected types, never {@code null}
     */
    @NotNull
    public Set<TypeReference> affectedTypes() {
        return this.affectedTypes;
    }

    /**
     * Checks if this step has an associated DataFix.
     *
     * @return {@code true} if a fix is present
     */
    public boolean hasFix() {
        return this.fix != null;
    }

    /**
     * Checks if this step affects any types.
     *
     * @return {@code true} if at least one type is affected
     */
    public boolean hasChanges() {
        return !this.affectedTypes.isEmpty();
    }

    /**
     * Checks if this step affects a specific type.
     *
     * @param type the type to check, must not be {@code null}
     * @return {@code true} if the type is affected
     */
    public boolean affects(@NotNull final TypeReference type) {
        Preconditions.checkNotNull(type, "type must not be null");
        return this.affectedTypes.contains(type);
    }

    /**
     * Compares this migration step to another object for equality.
     *
     * <p>Two {@code MigrationStep} instances are equal if they have the same
     * source version, target version, fix, and affected types. The schema diff
     * is not included in the comparison.</p>
     *
     * @param obj the object to compare with, may be {@code null}
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MigrationStep other)) {
            return false;
        }
        return this.sourceVersion.equals(other.sourceVersion)
                && this.targetVersion.equals(other.targetVersion)
                && Objects.equals(this.fix, other.fix)
                && this.affectedTypes.equals(other.affectedTypes);
    }

    /**
     * Returns a hash code value for this migration step.
     *
     * <p>The hash code is computed from the source version, target version,
     * fix, and affected types to ensure consistency with {@link #equals(Object)}.</p>
     *
     * @return the hash code value for this migration step
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.sourceVersion, this.targetVersion, this.fix, this.affectedTypes);
    }

    /**
     * Returns a human-readable string representation of this migration step.
     *
     * <p>The format includes the version range, optional fix name, and count
     * of affected types. For example:</p>
     * <pre>{@code MigrationStep[100 -> 110, fix=RenameFieldFix, affected=2 types]}</pre>
     *
     * @return a formatted string representation, never {@code null}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MigrationStep[");
        sb.append(this.sourceVersion.getVersion()).append(" -> ").append(this.targetVersion.getVersion());
        if (this.fix != null) {
            sb.append(", fix=").append(this.fix.getClass().getSimpleName());
        }
        sb.append(", affected=").append(this.affectedTypes.size()).append(" types");
        sb.append("]");
        return sb.toString();
    }

    /**
     * Builder for creating {@link MigrationStep} instances.
     *
     * <p>Provides a fluent API for constructing migration steps with optional
     * properties. Required properties (source and target versions) are provided
     * via {@link MigrationStep#builder(DataVersion, DataVersion)}.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * MigrationStep step = MigrationStep.builder(v1, v2)
     *     .fix(myFix)
     *     .schemaDiff(diff)
     *     .affectedTypes(Set.of(PLAYER, WORLD))
     *     .build();
     * }</pre>
     *
     * @author Erik Pförtner
     * @since 0.3.0
     */
    public static final class Builder {

        /**
         * The required source version.
         */
        private final DataVersion sourceVersion;

        /**
         * The required target version.
         */
        private final DataVersion targetVersion;

        /**
         * The optional DataFix; defaults to {@code null}.
         */
        private DataFix<?> fix;

        /**
         * The optional schema diff; defaults to {@code null}.
         */
        private SchemaDiff schemaDiff;

        /**
         * The affected types; defaults to empty set.
         */
        private Set<TypeReference> affectedTypes = Set.of();

        /**
         * Creates a new builder with the required version parameters.
         *
         * @param sourceVersion the source version, must not be {@code null}
         * @param targetVersion the target version, must not be {@code null}
         * @throws NullPointerException if any argument is {@code null}
         */
        private Builder(
                @NotNull final DataVersion sourceVersion,
                @NotNull final DataVersion targetVersion
        ) {
            this.sourceVersion = Preconditions.checkNotNull(sourceVersion, "sourceVersion must not be null");
            this.targetVersion = Preconditions.checkNotNull(targetVersion, "targetVersion must not be null");
        }

        /**
         * Sets the DataFix for this step.
         *
         * @param fix the fix to apply
         * @return this builder for chaining
         */
        @NotNull
        public Builder fix(final DataFix<?> fix) {
            this.fix = fix;
            return this;
        }

        /**
         * Sets the schema diff for this step.
         *
         * @param diff the schema diff
         * @return this builder for chaining
         */
        @NotNull
        public Builder schemaDiff(final SchemaDiff diff) {
            this.schemaDiff = diff;
            return this;
        }

        /**
         * Sets the affected types for this step.
         *
         * @param types the affected types, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder affectedTypes(@NotNull final Set<TypeReference> types) {
            this.affectedTypes = Preconditions.checkNotNull(types, "types must not be null");
            return this;
        }

        /**
         * Builds the MigrationStep.
         *
         * @return the constructed step, never {@code null}
         */
        @NotNull
        public MigrationStep build() {
            return new MigrationStep(
                    this.sourceVersion,
                    this.targetVersion,
                    this.fix,
                    this.schemaDiff,
                    this.affectedTypes
            );
        }
    }
}
