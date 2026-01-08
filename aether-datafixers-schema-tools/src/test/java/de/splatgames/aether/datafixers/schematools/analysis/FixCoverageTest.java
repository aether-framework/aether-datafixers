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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FixCoverage}.
 */
@DisplayName("FixCoverage")
class FixCoverageTest {

    private static final DataVersion V100 = new DataVersion(100);
    private static final DataVersion V200 = new DataVersion(200);
    private static final DataVersion V300 = new DataVersion(300);
    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference ENTITY = new TypeReference("entity");
    private static final TypeReference WORLD = new TypeReference("world");

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("empty() returns singleton")
        void emptyReturnsSingleton() {
            final FixCoverage coverage1 = FixCoverage.empty();
            final FixCoverage coverage2 = FixCoverage.empty();

            assertThat(coverage1).isSameAs(coverage2);
            assertThat(coverage1.isFullyCovered()).isTrue();
            assertThat(coverage1.gaps()).isEmpty();
            assertThat(coverage1.orphanFixes()).isEmpty();
        }

        @Test
        @DisplayName("fullyCovered creates coverage with no gaps")
        void fullyCoveredCreatesCoverageWithNoGaps() {
            final FixCoverage coverage = FixCoverage.fullyCovered(V100, V200);

            assertThat(coverage.sourceVersion()).isEqualTo(V100);
            assertThat(coverage.targetVersion()).isEqualTo(V200);
            assertThat(coverage.isFullyCovered()).isTrue();
            assertThat(coverage.gaps()).isEmpty();
        }

        @Test
        @DisplayName("fullyCovered throws on null sourceVersion")
        void fullyCoveredThrowsOnNullSourceVersion() {
            assertThatThrownBy(() -> FixCoverage.fullyCovered(null, V200))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("fullyCovered throws on null targetVersion")
        void fullyCoveredThrowsOnNullTargetVersion() {
            assertThatThrownBy(() -> FixCoverage.fullyCovered(V100, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds empty coverage")
        void buildsEmptyCoverage() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();

            assertThat(coverage.sourceVersion()).isEqualTo(V100);
            assertThat(coverage.targetVersion()).isEqualTo(V200);
            assertThat(coverage.gaps()).isEmpty();
            assertThat(coverage.orphanFixes()).isEmpty();
        }

        @Test
        @DisplayName("addGap adds single gap")
        void addGapAddsSingleGap() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            assertThat(coverage.gaps()).containsExactly(gap);
        }

        @Test
        @DisplayName("addGaps adds multiple gaps")
        void addGapsAddsMultipleGaps() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    ENTITY, V100, V200, CoverageGap.Reason.TYPE_REMOVED
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGaps(List.of(gap1, gap2))
                    .build();

            assertThat(coverage.gaps()).containsExactly(gap1, gap2);
        }

        @Test
        @DisplayName("builder throws on null versions")
        void builderThrowsOnNullVersions() {
            assertThatThrownBy(() -> FixCoverage.builder(null, V200))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> FixCoverage.builder(V100, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("addGap throws on null gap")
        void addGapThrowsOnNullGap() {
            assertThatThrownBy(() -> FixCoverage.builder(V100, V200).addGap(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("addGaps throws on null iterable")
        void addGapsThrowsOnNullIterable() {
            assertThatThrownBy(() -> FixCoverage.builder(V100, V200).addGaps(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Accessors")
    class Accessors {

        @Test
        @DisplayName("sourceVersion returns correct version")
        void sourceVersionReturnsCorrectVersion() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();
            assertThat(coverage.sourceVersion()).isEqualTo(V100);
        }

        @Test
        @DisplayName("targetVersion returns correct version")
        void targetVersionReturnsCorrectVersion() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();
            assertThat(coverage.targetVersion()).isEqualTo(V200);
        }

        @Test
        @DisplayName("gaps returns immutable list")
        void gapsReturnsImmutableList() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            assertThatThrownBy(() -> coverage.gaps().add(gap))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("orphanFixes returns immutable list")
        void orphanFixesReturnsImmutableList() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();

            assertThatThrownBy(() -> coverage.orphanFixes().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("uncoveredTypes returns immutable set")
        void uncoveredTypesReturnsImmutableSet() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            assertThatThrownBy(() -> coverage.uncoveredTypes().add(ENTITY))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Filtering Methods")
    class FilteringMethods {

        @Test
        @DisplayName("gapsForType returns gaps for specific type")
        void gapsForTypeReturnsGapsForSpecificType() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    ENTITY, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap3 = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, "name"
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap1)
                    .addGap(gap2)
                    .addGap(gap3)
                    .build();

            assertThat(coverage.gapsForType(PLAYER)).containsExactly(gap1, gap3);
            assertThat(coverage.gapsForType(ENTITY)).containsExactly(gap2);
            assertThat(coverage.gapsForType(WORLD)).isEmpty();
        }

        @Test
        @DisplayName("gapsForType throws on null type")
        void gapsForTypeThrowsOnNullType() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();

            assertThatThrownBy(() -> coverage.gapsForType(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("gapsByReason returns gaps for specific reason")
        void gapsByReasonReturnsGapsForSpecificReason() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    ENTITY, V100, V200, CoverageGap.Reason.TYPE_REMOVED
            );
            final CoverageGap gap3 = CoverageGap.typeLevel(
                    WORLD, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap1)
                    .addGap(gap2)
                    .addGap(gap3)
                    .build();

            assertThat(coverage.gapsByReason(CoverageGap.Reason.TYPE_ADDED))
                    .containsExactly(gap1, gap3);
            assertThat(coverage.gapsByReason(CoverageGap.Reason.TYPE_REMOVED))
                    .containsExactly(gap2);
            assertThat(coverage.gapsByReason(CoverageGap.Reason.FIELD_ADDED))
                    .isEmpty();
        }

        @Test
        @DisplayName("gapsByReason throws on null reason")
        void gapsByReasonThrowsOnNullReason() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();

            assertThatThrownBy(() -> coverage.gapsByReason(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("State Queries")
    class StateQueries {

        @Test
        @DisplayName("isFullyCovered returns true when no gaps")
        void isFullyCoveredReturnsTrueWhenNoGaps() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();
            assertThat(coverage.isFullyCovered()).isTrue();
        }

        @Test
        @DisplayName("isFullyCovered returns false when gaps exist")
        void isFullyCoveredReturnsFalseWhenGapsExist() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            assertThat(coverage.isFullyCovered()).isFalse();
        }

        @Test
        @DisplayName("hasGaps returns true when gaps exist")
        void hasGapsReturnsTrueWhenGapsExist() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            assertThat(coverage.hasGaps()).isTrue();
        }

        @Test
        @DisplayName("hasGaps returns false when no gaps")
        void hasGapsReturnsFalseWhenNoGaps() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();
            assertThat(coverage.hasGaps()).isFalse();
        }

        @Test
        @DisplayName("hasOrphanFixes returns false when no orphans")
        void hasOrphanFixesReturnsFalseWhenNoOrphans() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();
            assertThat(coverage.hasOrphanFixes()).isFalse();
        }

        @Test
        @DisplayName("isCovered returns true for covered type")
        void isCoveredReturnsTrueForCoveredType() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            // ENTITY is covered (no gap for it)
            assertThat(coverage.isCovered(ENTITY)).isTrue();
        }

        @Test
        @DisplayName("isCovered returns false for uncovered type")
        void isCoveredReturnsFalseForUncoveredType() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            assertThat(coverage.isCovered(PLAYER)).isFalse();
        }

        @Test
        @DisplayName("isCovered throws on null type")
        void isCoveredThrowsOnNullType() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();

            assertThatThrownBy(() -> coverage.isCovered(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Count Methods")
    class CountMethods {

        @Test
        @DisplayName("gapCount returns correct count")
        void gapCountReturnsCorrectCount() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    ENTITY, V100, V200, CoverageGap.Reason.TYPE_REMOVED
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap1)
                    .addGap(gap2)
                    .build();

            assertThat(coverage.gapCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("orphanCount returns correct count")
        void orphanCountReturnsCorrectCount() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();
            assertThat(coverage.orphanCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("uncoveredTypeCount returns correct count")
        void uncoveredTypeCountReturnsCorrectCount() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, "name"
            );
            final CoverageGap gap3 = CoverageGap.typeLevel(
                    ENTITY, V100, V200, CoverageGap.Reason.TYPE_REMOVED
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap1)
                    .addGap(gap2)
                    .addGap(gap3)
                    .build();

            // PLAYER has 2 gaps but only counts as 1 uncovered type
            assertThat(coverage.uncoveredTypeCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("coveragePercent()")
    class CoveragePercentMethod {

        @Test
        @DisplayName("returns 100.0 for no gaps")
        void returns100ForNoGaps() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();
            assertThat(coverage.coveragePercent(10)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("returns 0.0 for all gaps with zero total")
        void returns0ForAllGapsWithZeroTotal() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            assertThat(coverage.coveragePercent(0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("returns 100.0 for zero total with no gaps")
        void returns100ForZeroTotalWithNoGaps() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();
            assertThat(coverage.coveragePercent(0)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("calculates correct percentage")
        void calculatesCorrectPercentage() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    ENTITY, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap1)
                    .addGap(gap2)
                    .build();

            // 2 gaps out of 10 changes = 80% coverage
            assertThat(coverage.coveragePercent(10)).isEqualTo(80.0);
        }

        @Test
        @DisplayName("handles negative total changes")
        void handlesNegativeTotalChanges() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            // Negative total should return 0.0 when gaps exist
            assertThat(coverage.coveragePercent(-5)).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString for fully covered shows fully covered")
        void toStringForFullyCoveredShowsFullyCovered() {
            final FixCoverage coverage = FixCoverage.fullyCovered(V100, V200);
            assertThat(coverage.toString()).contains("fully covered");
        }

        @Test
        @DisplayName("toString contains version info")
        void toStringContainsVersionInfo() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200).build();
            assertThat(coverage.toString()).contains("100").contains("200");
        }

        @Test
        @DisplayName("toString contains gap count when has gaps")
        void toStringContainsGapCountWhenHasGaps() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    ENTITY, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap1)
                    .addGap(gap2)
                    .build();

            assertThat(coverage.toString()).contains("2 gaps");
        }

        @Test
        @DisplayName("toString contains uncovered type count")
        void toStringContainsUncoveredTypeCount() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGap(gap)
                    .build();

            assertThat(coverage.toString()).contains("1 uncovered");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles multiple gaps for same type")
        void handlesMultipleGapsForSameType() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.fieldLevel(
                    PLAYER, V200, V300, CoverageGap.Reason.FIELD_ADDED, "name"
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V300)
                    .addGap(gap1)
                    .addGap(gap2)
                    .build();

            assertThat(coverage.gapCount()).isEqualTo(2);
            assertThat(coverage.uncoveredTypeCount()).isEqualTo(1);
            assertThat(coverage.gapsForType(PLAYER)).hasSize(2);
        }

        @Test
        @DisplayName("uncoveredTypes aggregates unique types")
        void uncoveredTypesAggregatesUniqueTypes() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    PLAYER, V200, V300, CoverageGap.Reason.TYPE_MODIFIED
            );

            final FixCoverage coverage = FixCoverage.builder(V100, V300)
                    .addGap(gap1)
                    .addGap(gap2)
                    .build();

            assertThat(coverage.uncoveredTypes()).containsExactly(PLAYER);
        }

        @Test
        @DisplayName("handles empty iterable in addGaps")
        void handlesEmptyIterableInAddGaps() {
            final FixCoverage coverage = FixCoverage.builder(V100, V200)
                    .addGaps(List.of())
                    .build();

            assertThat(coverage.gaps()).isEmpty();
        }
    }
}
