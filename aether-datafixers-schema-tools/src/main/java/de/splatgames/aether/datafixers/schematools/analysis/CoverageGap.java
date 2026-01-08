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
import de.splatgames.aether.datafixers.schematools.diff.TypeDiff;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a missing DataFix for a type change between versions.
 *
 * <p>A coverage gap indicates that a type's structure changed between two
 * versions but no DataFix exists to handle the migration. This typically
 * means data of this type cannot be properly migrated.</p>
 *
 * <h2>Gap Categories</h2>
 * <ul>
 *   <li><b>Type Added:</b> New type appeared without initialization fix</li>
 *   <li><b>Type Removed:</b> Type removed without cleanup/migration fix</li>
 *   <li><b>Type Modified:</b> Fields changed without transformation fix</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(1).to(5)
 *     .analyzeCoverage();
 *
 * for (CoverageGap gap : coverage.gaps()) {
 *     System.err.println("Missing fix for " + gap.type().id()
 *         + " from v" + gap.sourceVersion().version()
 *         + " to v" + gap.targetVersion().version());
 *     System.err.println("  Reason: " + gap.reason());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pfoertner
 * @see FixCoverage
 * @see MigrationAnalyzer
 * @since 0.3.0
 */
public final class CoverageGap {

    /**
     * The reason for a coverage gap.
     */
    public enum Reason {
        /**
         * A new type was added without a fix to initialize default values.
         */
        TYPE_ADDED("Type was added without initialization fix"),

        /**
         * A type was removed without a fix to handle cleanup or migration.
         */
        TYPE_REMOVED("Type was removed without cleanup fix"),

        /**
         * A type's structure changed without a fix to transform the data.
         */
        TYPE_MODIFIED("Type structure changed without transformation fix"),

        /**
         * A field was added without a fix to provide default values.
         */
        FIELD_ADDED("Field was added without default value fix"),

        /**
         * A field was removed without a fix to handle the removal.
         */
        FIELD_REMOVED("Field was removed without cleanup fix"),

        /**
         * A field's type changed without a fix to convert the data.
         */
        FIELD_TYPE_CHANGED("Field type changed without conversion fix");

        private final String description;

        Reason(final String description) {
            this.description = description;
        }

        /**
         * Returns a human-readable description of this reason.
         *
         * @return the description, never {@code null}
         */
        @NotNull
        public String description() {
            return this.description;
        }
    }

    /**
     * The type reference for which this coverage gap was detected.
     */
    private final TypeReference type;

    /**
     * The source version where the type change originated.
     */
    private final DataVersion sourceVersion;

    /**
     * The target version where the type change applies.
     */
    private final DataVersion targetVersion;

    /**
     * The reason why this gap exists (what kind of change has no fix).
     */
    private final Reason reason;

    /**
     * The specific field name involved, or {@code null} for type-level gaps.
     */
    private final String fieldName;

    /**
     * The detailed type diff showing what changed, or {@code null} if not available.
     */
    private final TypeDiff typeDiff;

    /**
     * Creates a new CoverageGap instance.
     *
     * <p>This constructor is private; use the factory methods
     * {@link #typeLevel} or {@link #fieldLevel} to create instances.</p>
     *
     * @param type          the affected type, must not be {@code null}
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @param reason        the gap reason, must not be {@code null}
     * @param fieldName     the field name for field-level gaps, or {@code null}
     * @param typeDiff      the type diff for context, or {@code null}
     */
    private CoverageGap(
            @NotNull final TypeReference type,
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @NotNull final Reason reason,
            @Nullable final String fieldName,
            @Nullable final TypeDiff typeDiff
    ) {
        this.type = Preconditions.checkNotNull(type, "type must not be null");
        this.sourceVersion = Preconditions.checkNotNull(sourceVersion, "sourceVersion must not be null");
        this.targetVersion = Preconditions.checkNotNull(targetVersion, "targetVersion must not be null");
        this.reason = Preconditions.checkNotNull(reason, "reason must not be null");
        this.fieldName = fieldName;
        this.typeDiff = typeDiff;
    }

    /**
     * Creates a gap for a type-level change.
     *
     * @param type          the affected type, must not be {@code null}
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @param reason        the reason for the gap, must not be {@code null}
     * @return a new coverage gap, never {@code null}
     */
    @NotNull
    public static CoverageGap typeLevel(
            @NotNull final TypeReference type,
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @NotNull final Reason reason
    ) {
        return new CoverageGap(type, sourceVersion, targetVersion, reason, null, null);
    }

    /**
     * Creates a gap for a type-level change with diff information.
     *
     * @param type          the affected type, must not be {@code null}
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @param reason        the reason for the gap, must not be {@code null}
     * @param typeDiff      the type diff showing what changed, may be {@code null}
     * @return a new coverage gap, never {@code null}
     */
    @NotNull
    public static CoverageGap typeLevel(
            @NotNull final TypeReference type,
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @NotNull final Reason reason,
            @Nullable final TypeDiff typeDiff
    ) {
        return new CoverageGap(type, sourceVersion, targetVersion, reason, null, typeDiff);
    }

    /**
     * Creates a gap for a field-level change.
     *
     * @param type          the affected type, must not be {@code null}
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @param reason        the reason for the gap, must not be {@code null}
     * @param fieldName     the affected field name, must not be {@code null}
     * @return a new coverage gap, never {@code null}
     */
    @NotNull
    public static CoverageGap fieldLevel(
            @NotNull final TypeReference type,
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @NotNull final Reason reason,
            @NotNull final String fieldName
    ) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        return new CoverageGap(type, sourceVersion, targetVersion, reason, fieldName, null);
    }

    /**
     * Returns the type affected by this gap.
     *
     * @return the type reference, never {@code null}
     */
    @NotNull
    public TypeReference type() {
        return this.type;
    }

    /**
     * Returns the source version where the change originated.
     *
     * @return the source version, never {@code null}
     */
    @NotNull
    public DataVersion sourceVersion() {
        return this.sourceVersion;
    }

    /**
     * Returns the target version where the change applies.
     *
     * @return the target version, never {@code null}
     */
    @NotNull
    public DataVersion targetVersion() {
        return this.targetVersion;
    }

    /**
     * Returns the reason for this coverage gap.
     *
     * @return the reason, never {@code null}
     */
    @NotNull
    public Reason reason() {
        return this.reason;
    }

    /**
     * Returns the field name if this is a field-level gap.
     *
     * @return an Optional containing the field name, empty for type-level gaps
     */
    @NotNull
    public Optional<String> fieldName() {
        return Optional.ofNullable(this.fieldName);
    }

    /**
     * Returns the type diff associated with this gap, if available.
     *
     * @return an Optional containing the type diff, empty if not available
     */
    @NotNull
    public Optional<TypeDiff> typeDiff() {
        return Optional.ofNullable(this.typeDiff);
    }

    /**
     * Checks if this is a field-level gap.
     *
     * @return {@code true} if a field is involved
     */
    public boolean isFieldLevel() {
        return this.fieldName != null;
    }

    /**
     * Checks if this is a type-level gap.
     *
     * @return {@code true} if no specific field is involved
     */
    public boolean isTypeLevel() {
        return this.fieldName == null;
    }

    /**
     * Compares this coverage gap to another object for equality.
     *
     * <p>Two {@code CoverageGap} instances are equal if they have the same
     * type, source version, target version, reason, and field name. The
     * type diff is not included in the comparison since it is contextual.</p>
     *
     * @param obj the object to compare with, may be {@code null}
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CoverageGap other)) {
            return false;
        }
        return this.type.equals(other.type)
                && this.sourceVersion.equals(other.sourceVersion)
                && this.targetVersion.equals(other.targetVersion)
                && this.reason == other.reason
                && Objects.equals(this.fieldName, other.fieldName);
    }

    /**
     * Returns a hash code value for this coverage gap.
     *
     * <p>The hash code is computed from the type, source version, target version,
     * reason, and field name to ensure consistency with {@link #equals(Object)}.</p>
     *
     * @return the hash code value for this coverage gap
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.sourceVersion, this.targetVersion, this.reason, this.fieldName);
    }

    /**
     * Returns a human-readable string representation of this coverage gap.
     *
     * <p>The format includes the type name, optional field name, reason,
     * and version range. For example:</p>
     * <pre>{@code CoverageGap[player.health: FIELD_ADDED (v100 -> v110)]}</pre>
     *
     * @return a formatted string representation, never {@code null}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CoverageGap[");
        sb.append(this.type.getId());
        if (this.fieldName != null) {
            sb.append(".").append(this.fieldName);
        }
        sb.append(": ").append(this.reason.name());
        sb.append(" (v").append(this.sourceVersion.getVersion());
        sb.append(" -> v").append(this.targetVersion.getVersion()).append(")");
        sb.append("]");
        return sb.toString();
    }
}
