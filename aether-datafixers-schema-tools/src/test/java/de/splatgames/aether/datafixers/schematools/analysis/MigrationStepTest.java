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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MigrationStep}.
 */
@DisplayName("MigrationStep")
class MigrationStepTest {

    private static final DataVersion V100 = new DataVersion(100);
    private static final DataVersion V200 = new DataVersion(200);
    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference ENTITY = new TypeReference("entity");

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("withoutFix creates step without fix")
        void withoutFixCreatesStepWithoutFix() {
            final MigrationStep step = MigrationStep.withoutFix(
                    V100, V200, null, Set.of(PLAYER)
            );

            assertThat(step.sourceVersion()).isEqualTo(V100);
            assertThat(step.targetVersion()).isEqualTo(V200);
            assertThat(step.fix()).isEmpty();
            assertThat(step.hasFix()).isFalse();
            assertThat(step.affectedTypes()).containsExactly(PLAYER);
        }

        @Test
        @DisplayName("throws on null sourceVersion")
        void throwsOnNullSourceVersion() {
            assertThatThrownBy(() -> MigrationStep.withoutFix(null, V200, null, Set.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null targetVersion")
        void throwsOnNullTargetVersion() {
            assertThatThrownBy(() -> MigrationStep.withoutFix(V100, null, null, Set.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null affectedTypes")
        void throwsOnNullAffectedTypes() {
            assertThatThrownBy(() -> MigrationStep.withoutFix(V100, V200, null, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds with minimum required properties")
        void buildsWithMinimumRequiredProperties() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();

            assertThat(step.sourceVersion()).isEqualTo(V100);
            assertThat(step.targetVersion()).isEqualTo(V200);
            assertThat(step.fix()).isEmpty();
            assertThat(step.schemaDiff()).isEmpty();
            assertThat(step.affectedTypes()).isEmpty();
        }

        @Test
        @DisplayName("builds with all properties")
        void buildsWithAllProperties() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER, ENTITY))
                    .build();

            assertThat(step.affectedTypes()).containsExactlyInAnyOrder(PLAYER, ENTITY);
        }

        @Test
        @DisplayName("builder throws on null versions")
        void builderThrowsOnNullVersions() {
            assertThatThrownBy(() -> MigrationStep.builder(null, V200))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> MigrationStep.builder(V100, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("affectedTypes throws on null")
        void affectedTypesThrowsOnNull() {
            assertThatThrownBy(() -> MigrationStep.builder(V100, V200).affectedTypes(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Accessors")
    class Accessors {

        @Test
        @DisplayName("sourceVersion returns correct version")
        void sourceVersionReturnsCorrectVersion() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            assertThat(step.sourceVersion()).isEqualTo(V100);
        }

        @Test
        @DisplayName("targetVersion returns correct version")
        void targetVersionReturnsCorrectVersion() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            assertThat(step.targetVersion()).isEqualTo(V200);
        }

        @Test
        @DisplayName("affectedTypes returns immutable set")
        void affectedTypesReturnsImmutableSet() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();

            assertThatThrownBy(() -> step.affectedTypes().add(ENTITY))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethods {

        @Test
        @DisplayName("hasFix returns false when no fix")
        void hasFixReturnsFalseWhenNoFix() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            assertThat(step.hasFix()).isFalse();
        }

        @Test
        @DisplayName("hasChanges returns true when has affected types")
        void hasChangesReturnsTrueWhenHasAffectedTypes() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();

            assertThat(step.hasChanges()).isTrue();
        }

        @Test
        @DisplayName("hasChanges returns false when no affected types")
        void hasChangesReturnsFalseWhenNoAffectedTypes() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            assertThat(step.hasChanges()).isFalse();
        }

        @Test
        @DisplayName("affects returns true for affected type")
        void affectsReturnsTrueForAffectedType() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();

            assertThat(step.affects(PLAYER)).isTrue();
        }

        @Test
        @DisplayName("affects returns false for non-affected type")
        void affectsReturnsFalseForNonAffectedType() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();

            assertThat(step.affects(ENTITY)).isFalse();
        }

        @Test
        @DisplayName("affects throws on null type")
        void affectsThrowsOnNullType() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();

            assertThatThrownBy(() -> step.affects(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals returns true for identical steps")
        void equalsReturnsTrueForIdenticalSteps() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();
            final MigrationStep step2 = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();

            assertThat(step1).isEqualTo(step2);
            assertThat(step1.hashCode()).isEqualTo(step2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different source versions")
        void equalsReturnsFalseForDifferentSourceVersions() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200).build();
            final MigrationStep step2 = MigrationStep.builder(new DataVersion(150), V200).build();

            assertThat(step1).isNotEqualTo(step2);
        }

        @Test
        @DisplayName("equals returns false for different affected types")
        void equalsReturnsFalseForDifferentAffectedTypes() {
            final MigrationStep step1 = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER))
                    .build();
            final MigrationStep step2 = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(ENTITY))
                    .build();

            assertThat(step1).isNotEqualTo(step2);
        }

        @Test
        @DisplayName("equals returns true for same instance")
        void equalsReturnsTrueForSameInstance() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            assertThat(step).isEqualTo(step);
        }

        @Test
        @DisplayName("equals returns false for null")
        void equalsReturnsFalseForNull() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            assertThat(step).isNotEqualTo(null);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString contains version range")
        void toStringContainsVersionRange() {
            final MigrationStep step = MigrationStep.builder(V100, V200).build();
            assertThat(step.toString()).contains("100").contains("200");
        }

        @Test
        @DisplayName("toString contains affected type count")
        void toStringContainsAffectedTypeCount() {
            final MigrationStep step = MigrationStep.builder(V100, V200)
                    .affectedTypes(Set.of(PLAYER, ENTITY))
                    .build();

            assertThat(step.toString()).contains("2 types");
        }
    }
}
