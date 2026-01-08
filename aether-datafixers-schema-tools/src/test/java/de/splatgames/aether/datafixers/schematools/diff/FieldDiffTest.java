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

import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.schematools.introspection.FieldInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FieldDiff}.
 */
@DisplayName("FieldDiff")
class FieldDiffTest {

    private static final FieldInfo NAME_FIELD = FieldInfo.create("name", false, Type.STRING, "name");
    private static final FieldInfo ID_FIELD = FieldInfo.create("id", false, Type.INT, "id");
    private static final FieldInfo OPTIONAL_NAME = FieldInfo.create("name", true, Type.STRING, "name");

    @Nested
    @DisplayName("added() factory method")
    class AddedFactoryMethod {

        @Test
        @DisplayName("creates ADDED diff")
        void createsAddedDiff() {
            final FieldDiff diff = FieldDiff.added(NAME_FIELD);

            assertThat(diff.fieldName()).isEqualTo("name");
            assertThat(diff.kind()).isEqualTo(DiffKind.ADDED);
            assertThat(diff.sourceField()).isNull();
            assertThat(diff.targetField()).isEqualTo(NAME_FIELD);
            assertThat(diff.isChanged()).isTrue();
        }

        @Test
        @DisplayName("throws on null field")
        void throwsOnNullField() {
            assertThatThrownBy(() -> FieldDiff.added(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("removed() factory method")
    class RemovedFactoryMethod {

        @Test
        @DisplayName("creates REMOVED diff")
        void createsRemovedDiff() {
            final FieldDiff diff = FieldDiff.removed(NAME_FIELD);

            assertThat(diff.fieldName()).isEqualTo("name");
            assertThat(diff.kind()).isEqualTo(DiffKind.REMOVED);
            assertThat(diff.sourceField()).isEqualTo(NAME_FIELD);
            assertThat(diff.targetField()).isNull();
            assertThat(diff.isChanged()).isTrue();
        }

        @Test
        @DisplayName("throws on null field")
        void throwsOnNullField() {
            assertThatThrownBy(() -> FieldDiff.removed(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("modified() factory method")
    class ModifiedFactoryMethod {

        @Test
        @DisplayName("creates MODIFIED diff")
        void createsModifiedDiff() {
            final FieldDiff diff = FieldDiff.modified(NAME_FIELD, OPTIONAL_NAME);

            assertThat(diff.fieldName()).isEqualTo("name");
            assertThat(diff.kind()).isEqualTo(DiffKind.MODIFIED);
            assertThat(diff.sourceField()).isEqualTo(NAME_FIELD);
            assertThat(diff.targetField()).isEqualTo(OPTIONAL_NAME);
            assertThat(diff.isChanged()).isTrue();
        }

        @Test
        @DisplayName("throws on null source")
        void throwsOnNullSource() {
            assertThatThrownBy(() -> FieldDiff.modified(null, NAME_FIELD))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null target")
        void throwsOnNullTarget() {
            assertThatThrownBy(() -> FieldDiff.modified(NAME_FIELD, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on mismatched field names")
        void throwsOnMismatchedFieldNames() {
            assertThatThrownBy(() -> FieldDiff.modified(NAME_FIELD, ID_FIELD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }
    }

    @Nested
    @DisplayName("unchanged() factory method")
    class UnchangedFactoryMethod {

        @Test
        @DisplayName("creates UNCHANGED diff")
        void createsUnchangedDiff() {
            final FieldDiff diff = FieldDiff.unchanged(NAME_FIELD, NAME_FIELD);

            assertThat(diff.fieldName()).isEqualTo("name");
            assertThat(diff.kind()).isEqualTo(DiffKind.UNCHANGED);
            assertThat(diff.sourceField()).isEqualTo(NAME_FIELD);
            assertThat(diff.targetField()).isEqualTo(NAME_FIELD);
            assertThat(diff.isChanged()).isFalse();
        }

        @Test
        @DisplayName("throws on null source field")
        void throwsOnNullSourceField() {
            assertThatThrownBy(() -> FieldDiff.unchanged(null, NAME_FIELD))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null target field")
        void throwsOnNullTargetField() {
            assertThatThrownBy(() -> FieldDiff.unchanged(NAME_FIELD, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("compare() method")
    class CompareMethod {

        @Test
        @DisplayName("returns UNCHANGED for identical fields")
        void returnsUnchangedForIdenticalFields() {
            final FieldDiff diff = FieldDiff.compare(NAME_FIELD, NAME_FIELD);

            assertThat(diff.kind()).isEqualTo(DiffKind.UNCHANGED);
        }

        @Test
        @DisplayName("returns MODIFIED for different optionality")
        void returnsModifiedForDifferentOptionality() {
            final FieldDiff diff = FieldDiff.compare(NAME_FIELD, OPTIONAL_NAME);

            assertThat(diff.kind()).isEqualTo(DiffKind.MODIFIED);
        }

        @Test
        @DisplayName("returns MODIFIED for different types")
        void returnsModifiedForDifferentTypes() {
            final FieldInfo stringField = FieldInfo.create("value", false, Type.STRING, "value");
            final FieldInfo intField = FieldInfo.create("value", false, Type.INT, "value");

            final FieldDiff diff = FieldDiff.compare(stringField, intField);

            assertThat(diff.kind()).isEqualTo(DiffKind.MODIFIED);
        }

        @Test
        @DisplayName("throws on null source")
        void throwsOnNullSource() {
            assertThatThrownBy(() -> FieldDiff.compare(null, NAME_FIELD))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null target")
        void throwsOnNullTarget() {
            assertThatThrownBy(() -> FieldDiff.compare(NAME_FIELD, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals returns true for identical diffs")
        void equalsReturnsTrueForIdenticalDiffs() {
            final FieldDiff diff1 = FieldDiff.added(NAME_FIELD);
            final FieldDiff diff2 = FieldDiff.added(NAME_FIELD);

            assertThat(diff1).isEqualTo(diff2);
            assertThat(diff1.hashCode()).isEqualTo(diff2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different kinds")
        void equalsReturnsFalseForDifferentKinds() {
            final FieldDiff added = FieldDiff.added(NAME_FIELD);
            final FieldDiff removed = FieldDiff.removed(NAME_FIELD);

            assertThat(added).isNotEqualTo(removed);
        }

        @Test
        @DisplayName("equals returns false for null")
        void equalsReturnsFalseForNull() {
            final FieldDiff diff = FieldDiff.added(NAME_FIELD);

            assertThat(diff).isNotEqualTo(null);
        }

        @Test
        @DisplayName("equals returns true for same instance")
        void equalsReturnsTrueForSameInstance() {
            final FieldDiff diff = FieldDiff.added(NAME_FIELD);

            assertThat(diff).isEqualTo(diff);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("added field starts with +")
        void addedFieldStartsWithPlus() {
            final FieldDiff diff = FieldDiff.added(NAME_FIELD);

            assertThat(diff.toString()).startsWith("+name");
        }

        @Test
        @DisplayName("removed field starts with -")
        void removedFieldStartsWithMinus() {
            final FieldDiff diff = FieldDiff.removed(NAME_FIELD);

            assertThat(diff.toString()).startsWith("-name");
        }

        @Test
        @DisplayName("modified field starts with ~")
        void modifiedFieldStartsWithTilde() {
            final FieldDiff diff = FieldDiff.modified(NAME_FIELD, OPTIONAL_NAME);

            assertThat(diff.toString()).startsWith("~name");
        }

        @Test
        @DisplayName("unchanged field starts with =")
        void unchangedFieldStartsWithEquals() {
            final FieldDiff diff = FieldDiff.unchanged(NAME_FIELD, NAME_FIELD);

            assertThat(diff.toString()).startsWith("=name");
        }
    }

    @Nested
    @DisplayName("DiffKind enum")
    class DiffKindTests {

        @Test
        @DisplayName("all expected kinds exist")
        void allExpectedKindsExist() {
            assertThat(DiffKind.values()).containsExactlyInAnyOrder(
                    DiffKind.ADDED,
                    DiffKind.REMOVED,
                    DiffKind.MODIFIED,
                    DiffKind.UNCHANGED
            );
        }
    }
}
