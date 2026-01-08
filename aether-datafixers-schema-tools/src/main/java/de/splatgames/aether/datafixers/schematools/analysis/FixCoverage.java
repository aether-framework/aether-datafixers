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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The result of analyzing DataFix coverage for schema changes.
 *
 * <p>Fix coverage analysis determines whether all schema changes between
 * versions have corresponding DataFixes to handle the migration. This helps
 * identify potential data migration issues before they occur at runtime.</p>
 *
 * <h2>Coverage Analysis</h2>
 * <p>The analyzer checks:</p>
 * <ul>
 *   <li><b>Gaps:</b> Schema changes without fixes (data cannot migrate)</li>
 *   <li><b>Orphan Fixes:</b> Fixes for non-existent type changes</li>
 *   <li><b>Full Coverage:</b> All changes have corresponding fixes</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(1).to(5)
 *     .analyzeCoverage();
 *
 * if (!coverage.isFullyCovered()) {
 *     System.err.println("Coverage gaps found:");
 *     for (CoverageGap gap : coverage.gaps()) {
 *         System.err.println("  " + gap);
 *     }
 * }
 *
 * // Check coverage for specific type
 * if (!coverage.isCovered(TypeReferences.PLAYER)) {
 *     System.err.println("Player type has gaps!");
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pfoertner
 * @see CoverageGap
 * @see MigrationAnalyzer
 * @since 0.3.0
 */
public final class FixCoverage {

    /**
     * Singleton instance representing full coverage (no gaps, no orphans).
     * Used to avoid unnecessary object creation for fully covered migrations.
     */
    private static final FixCoverage EMPTY = new FixCoverage(
            new DataVersion(0), new DataVersion(0), List.of(), List.of()
    );

    /**
     * The source version of the analyzed migration range.
     */
    private final DataVersion sourceVersion;

    /**
     * The target version of the analyzed migration range.
     */
    private final DataVersion targetVersion;

    /**
     * The list of coverage gaps (schema changes without corresponding DataFixes).
     * This is an immutable copy of the provided gaps.
     */
    private final List<CoverageGap> gaps;

    /**
     * The list of orphan fixes (DataFixes without corresponding schema changes).
     * This is an immutable copy of the provided fixes.
     */
    private final List<DataFix<?>> orphanFixes;

    /**
     * The set of types that have at least one coverage gap.
     * Pre-computed for efficient {@link #isCovered(TypeReference)} queries.
     */
    private final Set<TypeReference> uncoveredTypes;

    /**
     * Creates a new immutable FixCoverage instance.
     *
     * <p>This constructor computes the set of uncovered types from the provided
     * gaps for efficient query access.</p>
     *
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @param gaps          the coverage gaps, must not be {@code null}
     * @param orphanFixes   the orphan fixes, must not be {@code null}
     */
    private FixCoverage(
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @NotNull final List<CoverageGap> gaps,
            @NotNull final List<DataFix<?>> orphanFixes
    ) {
        this.sourceVersion = Preconditions.checkNotNull(sourceVersion, "sourceVersion must not be null");
        this.targetVersion = Preconditions.checkNotNull(targetVersion, "targetVersion must not be null");
        this.gaps = List.copyOf(Preconditions.checkNotNull(gaps, "gaps must not be null"));
        this.orphanFixes = List.copyOf(Preconditions.checkNotNull(orphanFixes, "orphanFixes must not be null"));

        // Compute uncovered types
        final Set<TypeReference> types = new HashSet<>();
        for (final CoverageGap gap : this.gaps) {
            types.add(gap.type());
        }
        this.uncoveredTypes = Set.copyOf(types);
    }

    /**
     * Returns an empty coverage result (no gaps, no orphans).
     *
     * @return an empty result, never {@code null}
     */
    @NotNull
    public static FixCoverage empty() {
        return EMPTY;
    }

    /**
     * Creates a coverage result with full coverage.
     *
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @return a result indicating full coverage, never {@code null}
     */
    @NotNull
    public static FixCoverage fullyCovered(
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion
    ) {
        return new FixCoverage(sourceVersion, targetVersion, List.of(), List.of());
    }

    /**
     * Creates a new builder for constructing a FixCoverage result.
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
     * Returns the source version of the analyzed range.
     *
     * @return the source version, never {@code null}
     */
    @NotNull
    public DataVersion sourceVersion() {
        return this.sourceVersion;
    }

    /**
     * Returns the target version of the analyzed range.
     *
     * @return the target version, never {@code null}
     */
    @NotNull
    public DataVersion targetVersion() {
        return this.targetVersion;
    }

    /**
     * Returns all coverage gaps (changes without fixes).
     *
     * @return an unmodifiable list of gaps, never {@code null}
     */
    @NotNull
    public List<CoverageGap> gaps() {
        return this.gaps;
    }

    /**
     * Returns gaps for a specific type.
     *
     * @param type the type to filter by, must not be {@code null}
     * @return an unmodifiable list of gaps for the type, never {@code null}
     */
    @NotNull
    public List<CoverageGap> gapsForType(@NotNull final TypeReference type) {
        Preconditions.checkNotNull(type, "type must not be null");
        return this.gaps.stream()
                .filter(gap -> gap.type().equals(type))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns gaps filtered by reason.
     *
     * @param reason the reason to filter by, must not be {@code null}
     * @return an unmodifiable list of matching gaps, never {@code null}
     */
    @NotNull
    public List<CoverageGap> gapsByReason(@NotNull final CoverageGap.Reason reason) {
        Preconditions.checkNotNull(reason, "reason must not be null");
        return this.gaps.stream()
                .filter(gap -> gap.reason() == reason)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all orphan fixes (fixes without corresponding schema changes).
     *
     * @return an unmodifiable list of orphan fixes, never {@code null}
     */
    @NotNull
    public List<DataFix<?>> orphanFixes() {
        return this.orphanFixes;
    }

    /**
     * Returns all types that have coverage gaps.
     *
     * @return an unmodifiable set of uncovered types, never {@code null}
     */
    @NotNull
    public Set<TypeReference> uncoveredTypes() {
        return this.uncoveredTypes;
    }

    /**
     * Checks if all schema changes are covered by fixes.
     *
     * @return {@code true} if there are no gaps
     */
    public boolean isFullyCovered() {
        return this.gaps.isEmpty();
    }

    /**
     * Checks if there are any coverage gaps.
     *
     * @return {@code true} if gaps exist
     */
    public boolean hasGaps() {
        return !this.gaps.isEmpty();
    }

    /**
     * Checks if there are any orphan fixes.
     *
     * @return {@code true} if orphan fixes exist
     */
    public boolean hasOrphanFixes() {
        return !this.orphanFixes.isEmpty();
    }

    /**
     * Checks if a specific type is fully covered.
     *
     * @param type the type to check, must not be {@code null}
     * @return {@code true} if the type has no gaps
     */
    public boolean isCovered(@NotNull final TypeReference type) {
        Preconditions.checkNotNull(type, "type must not be null");
        return !this.uncoveredTypes.contains(type);
    }

    /**
     * Returns the number of coverage gaps.
     *
     * @return the gap count
     */
    public int gapCount() {
        return this.gaps.size();
    }

    /**
     * Returns the number of orphan fixes.
     *
     * @return the orphan count
     */
    public int orphanCount() {
        return this.orphanFixes.size();
    }

    /**
     * Returns the number of uncovered types.
     *
     * @return the uncovered type count
     */
    public int uncoveredTypeCount() {
        return this.uncoveredTypes.size();
    }

    /**
     * Calculates the coverage percentage.
     *
     * <p>Returns 100.0 if there are no gaps, otherwise calculates based on
     * the ratio of gaps to total changes analyzed.</p>
     *
     * @param totalChanges the total number of changes that were analyzed
     * @return the coverage percentage (0.0 to 100.0)
     */
    public double coveragePercent(final int totalChanges) {
        if (totalChanges <= 0) {
            return this.gaps.isEmpty() ? 100.0 : 0.0;
        }
        final int coveredChanges = Math.max(0, totalChanges - this.gaps.size());
        return (coveredChanges * 100.0) / totalChanges;
    }

    @Override
    public String toString() {
        if (isFullyCovered()) {
            return String.format(
                    "FixCoverage[v%d -> v%d, fully covered]",
                    this.sourceVersion.getVersion(),
                    this.targetVersion.getVersion()
            );
        }
        return String.format(
                "FixCoverage[v%d -> v%d, %d gaps, %d orphans, %d uncovered types]",
                this.sourceVersion.getVersion(),
                this.targetVersion.getVersion(),
                this.gaps.size(),
                this.orphanFixes.size(),
                this.uncoveredTypes.size()
        );
    }

    /**
     * Builder for creating {@link FixCoverage} instances.
     */
    public static final class Builder {

        private final DataVersion sourceVersion;
        private final DataVersion targetVersion;
        private final List<CoverageGap> gaps = new ArrayList<>();
        private final List<DataFix<?>> orphanFixes = new ArrayList<>();

        private Builder(
                @NotNull final DataVersion sourceVersion,
                @NotNull final DataVersion targetVersion
        ) {
            this.sourceVersion = Preconditions.checkNotNull(sourceVersion, "sourceVersion must not be null");
            this.targetVersion = Preconditions.checkNotNull(targetVersion, "targetVersion must not be null");
        }

        /**
         * Adds a coverage gap.
         *
         * @param gap the gap to add, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder addGap(@NotNull final CoverageGap gap) {
            Preconditions.checkNotNull(gap, "gap must not be null");
            this.gaps.add(gap);
            return this;
        }

        /**
         * Adds multiple coverage gaps.
         *
         * @param gaps the gaps to add, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder addGaps(@NotNull final Iterable<CoverageGap> gaps) {
            Preconditions.checkNotNull(gaps, "gaps must not be null");
            for (final CoverageGap gap : gaps) {
                this.gaps.add(gap);
            }
            return this;
        }

        /**
         * Adds an orphan fix.
         *
         * @param fix the orphan fix to add, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder addOrphanFix(@NotNull final DataFix<?> fix) {
            Preconditions.checkNotNull(fix, "fix must not be null");
            this.orphanFixes.add(fix);
            return this;
        }

        /**
         * Builds the FixCoverage result.
         *
         * @return the constructed result, never {@code null}
         */
        @NotNull
        public FixCoverage build() {
            return new FixCoverage(
                    this.sourceVersion,
                    this.targetVersion,
                    this.gaps,
                    this.orphanFixes
            );
        }
    }
}
