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
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.schematools.diff.TypeDiff;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CoverageGap}.
 */
@DisplayName("CoverageGap")
class CoverageGapTest {

    private static final DataVersion V100 = new DataVersion(100);
    private static final DataVersion V200 = new DataVersion(200);
    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference ENTITY = new TypeReference("entity");

    @Nested
    @DisplayName("Reason Enum")
    class ReasonEnumTests {

        @Test
        @DisplayName("TYPE_ADDED has description")
        void typeAddedHasDescription() {
            assertThat(CoverageGap.Reason.TYPE_ADDED.description())
                    .contains("added").contains("initialization");
        }

        @Test
        @DisplayName("TYPE_REMOVED has description")
        void typeRemovedHasDescription() {
            assertThat(CoverageGap.Reason.TYPE_REMOVED.description())
                    .contains("removed").contains("cleanup");
        }

        @Test
        @DisplayName("TYPE_MODIFIED has description")
        void typeModifiedHasDescription() {
            assertThat(CoverageGap.Reason.TYPE_MODIFIED.description())
                    .contains("changed").contains("transformation");
        }

        @Test
        @DisplayName("FIELD_ADDED has description")
        void fieldAddedHasDescription() {
            assertThat(CoverageGap.Reason.FIELD_ADDED.description())
                    .contains("added").contains("default");
        }

        @Test
        @DisplayName("FIELD_REMOVED has description")
        void fieldRemovedHasDescription() {
            assertThat(CoverageGap.Reason.FIELD_REMOVED.description())
                    .contains("removed").contains("cleanup");
        }

        @Test
        @DisplayName("FIELD_TYPE_CHANGED has description")
        void fieldTypeChangedHasDescription() {
            assertThat(CoverageGap.Reason.FIELD_TYPE_CHANGED.description())
                    .contains("type").contains("changed").contains("conversion");
        }

        @Test
        @DisplayName("all reasons have non-empty descriptions")
        void allReasonsHaveNonEmptyDescriptions() {
            for (final CoverageGap.Reason reason : CoverageGap.Reason.values()) {
                assertThat(reason.description()).isNotNull().isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Factory Methods - Type Level")
    class TypeLevelFactoryMethods {

        @Test
        @DisplayName("typeLevel creates gap without field")
        void typeLevelCreatesGapWithoutField() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );

            assertThat(gap.type()).isEqualTo(PLAYER);
            assertThat(gap.sourceVersion()).isEqualTo(V100);
            assertThat(gap.targetVersion()).isEqualTo(V200);
            assertThat(gap.reason()).isEqualTo(CoverageGap.Reason.TYPE_ADDED);
            assertThat(gap.fieldName()).isEmpty();
            assertThat(gap.typeDiff()).isEmpty();
            assertThat(gap.isTypeLevel()).isTrue();
            assertThat(gap.isFieldLevel()).isFalse();
        }

        @Test
        @DisplayName("typeLevel with typeDiff includes diff")
        void typeLevelWithTypeDiffIncludesDiff() {
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of());

            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_MODIFIED, typeDiff
            );

            assertThat(gap.typeDiff()).contains(typeDiff);
        }

        @Test
        @DisplayName("typeLevel throws on null type")
        void typeLevelThrowsOnNullType() {
            assertThatThrownBy(() -> CoverageGap.typeLevel(
                    null, V100, V200, CoverageGap.Reason.TYPE_ADDED
            )).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("typeLevel throws on null sourceVersion")
        void typeLevelThrowsOnNullSourceVersion() {
            assertThatThrownBy(() -> CoverageGap.typeLevel(
                    PLAYER, null, V200, CoverageGap.Reason.TYPE_ADDED
            )).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("typeLevel throws on null targetVersion")
        void typeLevelThrowsOnNullTargetVersion() {
            assertThatThrownBy(() -> CoverageGap.typeLevel(
                    PLAYER, V100, null, CoverageGap.Reason.TYPE_ADDED
            )).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("typeLevel throws on null reason")
        void typeLevelThrowsOnNullReason() {
            assertThatThrownBy(() -> CoverageGap.typeLevel(
                    PLAYER, V100, V200, null
            )).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Factory Methods - Field Level")
    class FieldLevelFactoryMethods {

        @Test
        @DisplayName("fieldLevel creates gap with field")
        void fieldLevelCreatesGapWithField() {
            final CoverageGap gap = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, "playerName"
            );

            assertThat(gap.type()).isEqualTo(PLAYER);
            assertThat(gap.sourceVersion()).isEqualTo(V100);
            assertThat(gap.targetVersion()).isEqualTo(V200);
            assertThat(gap.reason()).isEqualTo(CoverageGap.Reason.FIELD_ADDED);
            assertThat(gap.fieldName()).contains("playerName");
            assertThat(gap.isFieldLevel()).isTrue();
            assertThat(gap.isTypeLevel()).isFalse();
        }

        @Test
        @DisplayName("fieldLevel throws on null fieldName")
        void fieldLevelThrowsOnNullFieldName() {
            assertThatThrownBy(() -> CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, null
            )).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("fieldLevel throws on null type")
        void fieldLevelThrowsOnNullType() {
            assertThatThrownBy(() -> CoverageGap.fieldLevel(
                    null, V100, V200, CoverageGap.Reason.FIELD_ADDED, "field"
            )).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Accessors")
    class Accessors {

        @Test
        @DisplayName("type returns correct type")
        void typeReturnsCorrectType() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            assertThat(gap.type()).isEqualTo(PLAYER);
        }

        @Test
        @DisplayName("sourceVersion returns correct version")
        void sourceVersionReturnsCorrectVersion() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            assertThat(gap.sourceVersion()).isEqualTo(V100);
        }

        @Test
        @DisplayName("targetVersion returns correct version")
        void targetVersionReturnsCorrectVersion() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            assertThat(gap.targetVersion()).isEqualTo(V200);
        }

        @Test
        @DisplayName("reason returns correct reason")
        void reasonReturnsCorrectReason() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_REMOVED
            );
            assertThat(gap.reason()).isEqualTo(CoverageGap.Reason.TYPE_REMOVED);
        }

        @Test
        @DisplayName("fieldName returns empty for type-level gap")
        void fieldNameReturnsEmptyForTypeLevelGap() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            assertThat(gap.fieldName()).isEmpty();
        }

        @Test
        @DisplayName("fieldName returns value for field-level gap")
        void fieldNameReturnsValueForFieldLevelGap() {
            final CoverageGap gap = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, "name"
            );
            assertThat(gap.fieldName()).contains("name");
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals returns true for identical gaps")
        void equalsReturnsTrueForIdenticalGaps() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );

            assertThat(gap1).isEqualTo(gap2);
            assertThat(gap1.hashCode()).isEqualTo(gap2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different types")
        void equalsReturnsFalseForDifferentTypes() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    ENTITY, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );

            assertThat(gap1).isNotEqualTo(gap2);
        }

        @Test
        @DisplayName("equals returns false for different versions")
        void equalsReturnsFalseForDifferentVersions() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    PLAYER, V100, new DataVersion(300), CoverageGap.Reason.TYPE_ADDED
            );

            assertThat(gap1).isNotEqualTo(gap2);
        }

        @Test
        @DisplayName("equals returns false for different reasons")
        void equalsReturnsFalseForDifferentReasons() {
            final CoverageGap gap1 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            final CoverageGap gap2 = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_REMOVED
            );

            assertThat(gap1).isNotEqualTo(gap2);
        }

        @Test
        @DisplayName("equals returns false for different field names")
        void equalsReturnsFalseForDifferentFieldNames() {
            final CoverageGap gap1 = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, "name"
            );
            final CoverageGap gap2 = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, "level"
            );

            assertThat(gap1).isNotEqualTo(gap2);
        }

        @Test
        @DisplayName("equals returns true for same instance")
        void equalsReturnsTrueForSameInstance() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            assertThat(gap).isEqualTo(gap);
        }

        @Test
        @DisplayName("equals returns false for null")
        void equalsReturnsFalseForNull() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            assertThat(gap).isNotEqualTo(null);
        }

        @Test
        @DisplayName("field-level gaps with same field are equal")
        void fieldLevelGapsWithSameFieldAreEqual() {
            final CoverageGap gap1 = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, "name"
            );
            final CoverageGap gap2 = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, "name"
            );

            assertThat(gap1).isEqualTo(gap2);
            assertThat(gap1.hashCode()).isEqualTo(gap2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString contains type id")
        void toStringContainsTypeId() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            assertThat(gap.toString()).contains("player");
        }

        @Test
        @DisplayName("toString contains field name for field-level gap")
        void toStringContainsFieldNameForFieldLevelGap() {
            final CoverageGap gap = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, "playerName"
            );
            assertThat(gap.toString()).contains("playerName");
        }

        @Test
        @DisplayName("toString contains reason")
        void toStringContainsReason() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            assertThat(gap.toString()).contains("TYPE_ADDED");
        }

        @Test
        @DisplayName("toString contains versions")
        void toStringContainsVersions() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_ADDED
            );
            assertThat(gap.toString()).contains("100").contains("200");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles all reason types at type level")
        void handlesAllReasonTypesAtTypeLevel() {
            for (final CoverageGap.Reason reason : CoverageGap.Reason.values()) {
                final CoverageGap gap = CoverageGap.typeLevel(PLAYER, V100, V200, reason);
                assertThat(gap.reason()).isEqualTo(reason);
            }
        }

        @Test
        @DisplayName("handles empty field name")
        void handlesEmptyFieldName() {
            final CoverageGap gap = CoverageGap.fieldLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.FIELD_ADDED, ""
            );
            assertThat(gap.fieldName()).contains("");
            assertThat(gap.isFieldLevel()).isTrue();
        }

        @Test
        @DisplayName("handles same source and target version")
        void handlesSameSourceAndTargetVersion() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V100, CoverageGap.Reason.TYPE_MODIFIED
            );
            assertThat(gap.sourceVersion()).isEqualTo(V100);
            assertThat(gap.targetVersion()).isEqualTo(V100);
        }

        @Test
        @DisplayName("typeDiff can be null in typeLevel with diff")
        void typeDiffCanBeNullInTypeLevelWithDiff() {
            final CoverageGap gap = CoverageGap.typeLevel(
                    PLAYER, V100, V200, CoverageGap.Reason.TYPE_MODIFIED, null
            );
            assertThat(gap.typeDiff()).isEmpty();
        }
    }
}
