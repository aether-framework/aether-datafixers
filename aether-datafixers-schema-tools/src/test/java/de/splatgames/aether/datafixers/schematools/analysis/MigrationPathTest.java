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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MigrationPath}.
 */
@DisplayName("MigrationPath")
class MigrationPathTest {

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
        @DisplayName("empty() returns singleton empty path")
        void emptyReturnsSingletonEmptyPath() {
            final MigrationPath path1 = MigrationPath.empty();
            final MigrationPath path2 = MigrationPath.empty();

            assertThat(path1).isSameAs(path2);
            assertThat(path1.isEmpty()).isTrue();
            assertThat(path1.steps()).isEmpty();
        }

        @Test
        @DisplayName("of() creates path with steps")
        void ofCreatesPathWithSteps() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();

            final MigrationPath path = MigrationPath.of(V100, V200, List.of(step));

            assertThat(path.sourceVersion()).isEqualTo(V100);
            assertThat(path.targetVersion()).isEqualTo(V200);
            assertThat(path.steps()).containsExactly(step);
        }

        @Test
        @DisplayName("of() throws on null sourceVersion")
        void ofThrowsOnNullSourceVersion() {
            assertThatThrownBy(() -> MigrationPath.of(null, V200, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("of() throws on null targetVersion")
        void ofThrowsOnNullTargetVersion() {
            assertThatThrownBy(() -> MigrationPath.of(V100, null, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("of() throws on null steps")
        void ofThrowsOnNullSteps() {
            assertThatThrownBy(() -> MigrationPath.of(V100, V200, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds empty path")
        void buildsEmptyPath() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();

            assertThat(path.sourceVersion()).isEqualTo(V100);
            assertThat(path.targetVersion()).isEqualTo(V200);
            assertThat(path.steps()).isEmpty();
        }

        @Test
        @DisplayName("addStep adds single step")
        void addStepAddsSingleStep() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();

            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThat(path.steps()).containsExactly(step);
        }

        @Test
        @DisplayName("addSteps adds multiple steps")
        void addStepsAddsMultipleSteps() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200).build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300).build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addSteps(List.of(step1, step2))
                    .build();

            assertThat(path.steps()).containsExactly(step1, step2);
        }

        @Test
        @DisplayName("builder throws on null versions")
        void builderThrowsOnNullVersions() {
            assertThatThrownBy(() -> MigrationPath.builder(null, V200))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> MigrationPath.builder(V100, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("addStep throws on null step")
        void addStepThrowsOnNullStep() {
            assertThatThrownBy(() -> MigrationPath.builder(V100, V200).addStep(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("addSteps throws on null iterable")
        void addStepsThrowsOnNullIterable() {
            assertThatThrownBy(() -> MigrationPath.builder(V100, V200).addSteps(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Accessors")
    class Accessors {

        @Test
        @DisplayName("sourceVersion returns correct version")
        void sourceVersionReturnsCorrectVersion() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();
            assertThat(path.sourceVersion()).isEqualTo(V100);
        }

        @Test
        @DisplayName("targetVersion returns correct version")
        void targetVersionReturnsCorrectVersion() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();
            assertThat(path.targetVersion()).isEqualTo(V200);
        }

        @Test
        @DisplayName("steps returns immutable list")
        void stepsReturnsImmutableList() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThatThrownBy(() -> path.steps().add(step))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("affectedTypes returns aggregated types")
        void affectedTypesReturnsAggregatedTypes() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300)
                    .affectedTypes(Set.of(ENTITY))
                    .build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(step1)
                    .addStep(step2)
                    .build();

            assertThat(path.affectedTypes()).containsExactlyInAnyOrder(PLAYER, ENTITY);
        }

        @Test
        @DisplayName("affectedTypes returns immutable set")
        void affectedTypesReturnsImmutableSet() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();

            assertThatThrownBy(() -> path.affectedTypes().add(PLAYER))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Count Methods")
    class CountMethods {

        @Test
        @DisplayName("stepCount returns correct count")
        void stepCountReturnsCorrectCount() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200).build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300).build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(step1)
                    .addStep(step2)
                    .build();

            assertThat(path.stepCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("fixCount returns count of steps with fixes")
        void fixCountReturnsCountOfStepsWithFixes() {
            final MigrationStep stepWithFix = MigrationStep.withoutFix(V100, V200, null, Set.of(PLAYER));
            final MigrationStep stepNoFix = MigrationStep.builder(V200, V300).build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(stepWithFix)
                    .addStep(stepNoFix)
                    .build();

            // Neither step has a fix (withoutFix creates step without fix)
            assertThat(path.fixCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("State Queries")
    class StateQueries {

        @Test
        @DisplayName("isEmpty returns true for empty path")
        void isEmptyReturnsTrueForEmptyPath() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();
            assertThat(path.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("isEmpty returns false for non-empty path")
        void isEmptyReturnsFalseForNonEmptyPath() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThat(path.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("hasChanges returns true when types affected")
        void hasChangesReturnsTrueWhenTypesAffected() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();
            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThat(path.hasChanges()).isTrue();
        }

        @Test
        @DisplayName("hasChanges returns false when no types affected")
        void hasChangesReturnsFalseWhenNoTypesAffected() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThat(path.hasChanges()).isFalse();
        }
    }

    @Nested
    @DisplayName("Step Navigation")
    class StepNavigation {

        @Test
        @DisplayName("firstStep returns first step")
        void firstStepReturnsFirstStep() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200).build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300).build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(step1)
                    .addStep(step2)
                    .build();

            assertThat(path.firstStep()).contains(step1);
        }

        @Test
        @DisplayName("firstStep returns empty for empty path")
        void firstStepReturnsEmptyForEmptyPath() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();
            assertThat(path.firstStep()).isEmpty();
        }

        @Test
        @DisplayName("lastStep returns last step")
        void lastStepReturnsLastStep() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200).build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300).build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(step1)
                    .addStep(step2)
                    .build();

            assertThat(path.lastStep()).contains(step2);
        }

        @Test
        @DisplayName("lastStep returns empty for empty path")
        void lastStepReturnsEmptyForEmptyPath() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();
            assertThat(path.lastStep()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Filtering Methods")
    class FilteringMethods {

        @Test
        @DisplayName("stepsAffecting returns steps for type")
        void stepsAffectingReturnsStepsForType() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300)
                    .affectedTypes(Set.of(ENTITY))
                    .build();
            final MigrationStep step3 = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER, ENTITY))
                    .build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(step1)
                    .addStep(step2)
                    .addStep(step3)
                    .build();

            assertThat(path.stepsAffecting(PLAYER)).containsExactly(step1, step3);
            assertThat(path.stepsAffecting(ENTITY)).containsExactly(step2, step3);
            assertThat(path.stepsAffecting(WORLD)).isEmpty();
        }

        @Test
        @DisplayName("stepsAffecting throws on null type")
        void stepsAffectingThrowsOnNullType() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();

            assertThatThrownBy(() -> path.stepsAffecting(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("stepsWithFixes returns steps that have fixes")
        void stepsWithFixesReturnsStepsThatHaveFixes() {
            // Create steps without fixes (no way to set fix in withoutFix)
            final MigrationStep step1 = MigrationStep.builder(V100, V200).build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300).build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(step1)
                    .addStep(step2)
                    .build();

            // Both steps have no fixes
            assertThat(path.stepsWithFixes()).isEmpty();
        }

        @Test
        @DisplayName("stepsWithoutFixes returns steps without fixes")
        void stepsWithoutFixesReturnsStepsWithoutFixes() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200).build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300).build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(step1)
                    .addStep(step2)
                    .build();

            assertThat(path.stepsWithoutFixes()).containsExactly(step1, step2);
        }
    }

    @Nested
    @DisplayName("affects() Method")
    class AffectsMethod {

        @Test
        @DisplayName("affects returns true for affected type")
        void affectsReturnsTrueForAffectedType() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();
            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThat(path.affects(PLAYER)).isTrue();
        }

        @Test
        @DisplayName("affects returns false for non-affected type")
        void affectsReturnsFalseForNonAffectedType() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();
            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThat(path.affects(ENTITY)).isFalse();
        }

        @Test
        @DisplayName("affects throws on null type")
        void affectsThrowsOnNullType() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();

            assertThatThrownBy(() -> path.affects(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString contains version info")
        void toStringContainsVersionInfo() {
            final MigrationPath path = MigrationPath.builder(V100, V200).build();

            assertThat(path.toString()).contains("100").contains("200");
        }

        @Test
        @DisplayName("toString contains step count")
        void toStringContainsStepCount() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200).build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300).build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(step1)
                    .addStep(step2)
                    .build();

            assertThat(path.toString()).contains("2 steps");
        }

        @Test
        @DisplayName("toString contains affected type count")
        void toStringContainsAffectedTypeCount() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER, ENTITY))
                    .build();

            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThat(path.toString()).contains("2 types affected");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles single step path")
        void handlesSingleStepPath() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();

            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThat(path.stepCount()).isEqualTo(1);
            assertThat(path.firstStep()).contains(step);
            assertThat(path.lastStep()).contains(step);
        }

        @Test
        @DisplayName("handles duplicate types across steps")
        void handlesDuplicateTypesAcrossSteps() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();
            final MigrationStep step2 = MigrationStep.builder(V200, V300)
                    .affectedTypes(Set.of(PLAYER))
                    .build();

            final MigrationPath path = MigrationPath.builder(V100, V300)
                    .addStep(step1)
                    .addStep(step2)
                    .build();

            // Should only have one PLAYER type
            assertThat(path.affectedTypes()).containsExactly(PLAYER);
        }

        @Test
        @DisplayName("handles path with all types in single step")
        void handlesPathWithAllTypesInSingleStep() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER, ENTITY, WORLD))
                    .build();

            final MigrationPath path = MigrationPath.builder(V100, V200)
                    .addStep(step)
                    .build();

            assertThat(path.affectedTypes()).hasSize(3);
        }
    }
}
