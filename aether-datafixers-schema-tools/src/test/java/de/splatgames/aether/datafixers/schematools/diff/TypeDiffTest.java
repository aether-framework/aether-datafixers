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

import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.schematools.introspection.FieldInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TypeDiff}.
 */
@DisplayName("TypeDiff")
class TypeDiffTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final FieldInfo NAME_FIELD = FieldInfo.create("name", false, Type.STRING, "name");
    private static final FieldInfo ID_FIELD = FieldInfo.create("id", false, Type.INT, "id");

    @Nested
    @DisplayName("of() factory method")
    class OfFactoryMethod {

        @Test
        @DisplayName("creates TypeDiff with no field changes")
        void createsTypeDiffWithNoFieldChanges() {
            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of());

            assertThat(diff.reference()).isEqualTo(PLAYER);
            assertThat(diff.sourceType()).isSameAs(Type.STRING);
            assertThat(diff.targetType()).isSameAs(Type.STRING);
            assertThat(diff.fieldDiffs()).isEmpty();
            assertThat(diff.hasFieldChanges()).isFalse();
        }

        @Test
        @DisplayName("creates TypeDiff with field diffs")
        void createsTypeDiffWithFieldDiffs() {
            final FieldDiff fieldDiff = FieldDiff.added(NAME_FIELD);
            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(fieldDiff));

            assertThat(diff.fieldDiffs()).containsExactly(fieldDiff);
            assertThat(diff.hasFieldChanges()).isTrue();
        }

        @Test
        @DisplayName("throws on null reference")
        void throwsOnNullReference() {
            assertThatThrownBy(() -> TypeDiff.of(null, Type.STRING, Type.STRING, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null source type")
        void throwsOnNullSourceType() {
            assertThatThrownBy(() -> TypeDiff.of(PLAYER, null, Type.STRING, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null target type")
        void throwsOnNullTargetType() {
            assertThatThrownBy(() -> TypeDiff.of(PLAYER, Type.STRING, null, List.of()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null field diffs")
        void throwsOnNullFieldDiffs() {
            assertThatThrownBy(() -> TypeDiff.of(PLAYER, Type.STRING, Type.STRING, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Field filtering methods")
    class FieldFilteringMethods {

        @Test
        @DisplayName("addedFields returns only added fields")
        void addedFieldsReturnsOnlyAddedFields() {
            final FieldDiff added = FieldDiff.added(NAME_FIELD);
            final FieldDiff removed = FieldDiff.removed(ID_FIELD);

            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(added, removed));

            assertThat(diff.addedFields()).containsExactly(added);
        }

        @Test
        @DisplayName("removedFields returns only removed fields")
        void removedFieldsReturnsOnlyRemovedFields() {
            final FieldDiff added = FieldDiff.added(NAME_FIELD);
            final FieldDiff removed = FieldDiff.removed(ID_FIELD);

            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(added, removed));

            assertThat(diff.removedFields()).containsExactly(removed);
        }

        @Test
        @DisplayName("modifiedFields returns only modified fields")
        void modifiedFieldsReturnsOnlyModifiedFields() {
            final FieldInfo optionalName = FieldInfo.create("name", true, Type.STRING, "name");
            final FieldDiff modified = FieldDiff.modified(NAME_FIELD, optionalName);
            final FieldDiff added = FieldDiff.added(ID_FIELD);

            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(modified, added));

            assertThat(diff.modifiedFields()).containsExactly(modified);
        }

        @Test
        @DisplayName("unchangedFields returns only unchanged fields")
        void unchangedFieldsReturnsOnlyUnchangedFields() {
            final FieldDiff unchanged = FieldDiff.unchanged(NAME_FIELD, NAME_FIELD);
            final FieldDiff added = FieldDiff.added(ID_FIELD);

            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(unchanged, added));

            assertThat(diff.unchangedFields()).containsExactly(unchanged);
        }

    }

    @Nested
    @DisplayName("Utility methods")
    class UtilityMethods {

        @Test
        @DisplayName("hasFieldChanges returns true for changes")
        void hasFieldChangesReturnsTrueForChanges() {
            final FieldDiff added = FieldDiff.added(NAME_FIELD);
            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(added));

            assertThat(diff.hasFieldChanges()).isTrue();
        }

        @Test
        @DisplayName("hasFieldChanges returns false for no changes")
        void hasFieldChangesReturnsFalseForNoChanges() {
            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of());

            assertThat(diff.hasFieldChanges()).isFalse();
        }

        @Test
        @DisplayName("hasFieldChanges returns false for only unchanged")
        void hasFieldChangesReturnsFalseForOnlyUnchanged() {
            final FieldDiff unchanged = FieldDiff.unchanged(NAME_FIELD, NAME_FIELD);
            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(unchanged));

            assertThat(diff.hasFieldChanges()).isFalse();
        }

        @Test
        @DisplayName("changedFieldCount counts correctly")
        void changedFieldCountCountsCorrectly() {
            final FieldDiff added = FieldDiff.added(NAME_FIELD);
            final FieldDiff removed = FieldDiff.removed(ID_FIELD);
            final FieldInfo otherField = FieldInfo.create("other", false, Type.STRING, "other");
            final FieldDiff unchanged = FieldDiff.unchanged(otherField, otherField);

            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING,
                    List.of(added, removed, unchanged));

            assertThat(diff.changedFieldCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals returns true for identical diffs")
        void equalsReturnsTrueForIdenticalDiffs() {
            final FieldDiff fieldDiff = FieldDiff.added(NAME_FIELD);
            final TypeDiff diff1 = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(fieldDiff));
            final TypeDiff diff2 = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(fieldDiff));

            assertThat(diff1).isEqualTo(diff2);
            assertThat(diff1.hashCode()).isEqualTo(diff2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different references")
        void equalsReturnsFalseForDifferentReferences() {
            final TypeReference entity = new TypeReference("entity");
            final TypeDiff diff1 = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of());
            final TypeDiff diff2 = TypeDiff.of(entity, Type.STRING, Type.STRING, List.of());

            assertThat(diff1).isNotEqualTo(diff2);
        }

        @Test
        @DisplayName("equals returns false for different field diffs")
        void equalsReturnsFalseForDifferentFieldDiffs() {
            final FieldDiff added = FieldDiff.added(NAME_FIELD);
            final FieldDiff removed = FieldDiff.removed(NAME_FIELD);

            final TypeDiff diff1 = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(added));
            final TypeDiff diff2 = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(removed));

            assertThat(diff1).isNotEqualTo(diff2);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString contains reference")
        void toStringContainsReference() {
            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of());

            assertThat(diff.toString()).contains("player");
        }

        @Test
        @DisplayName("toString contains field counts")
        void toStringContainsFieldCounts() {
            final FieldDiff added = FieldDiff.added(NAME_FIELD);
            final TypeDiff diff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(added));

            assertThat(diff.toString()).contains("added=1");
        }
    }
}
