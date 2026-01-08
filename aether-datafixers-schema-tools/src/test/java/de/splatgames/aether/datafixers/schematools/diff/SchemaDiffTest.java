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
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.schematools.introspection.FieldInfo;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SchemaDiff}.
 */
@DisplayName("SchemaDiff")
class SchemaDiffTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference ENTITY = new TypeReference("entity");
    private static final TypeReference WORLD = new TypeReference("world");
    private static final FieldInfo NAME_FIELD = FieldInfo.create("name", false, Type.STRING, "name");

    private Schema sourceSchema;
    private Schema targetSchema;

    @BeforeEach
    void setUp() {
        sourceSchema = MockSchemas.minimal(100);
        targetSchema = MockSchemas.minimal(200);
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds with minimum required properties")
        void buildsWithMinimumRequiredProperties() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.source()).isSameAs(sourceSchema);
            assertThat(diff.target()).isSameAs(targetSchema);
            assertThat(diff.addedTypes()).isEmpty();
            assertThat(diff.removedTypes()).isEmpty();
            assertThat(diff.commonTypes()).isEmpty();
            assertThat(diff.typeDiffs()).isEmpty();
        }

        @Test
        @DisplayName("builds with all properties")
        void buildsWithAllProperties() {
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of());

            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(ENTITY))
                    .removedTypes(Set.of(WORLD))
                    .commonTypes(Set.of(PLAYER))
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            assertThat(diff.addedTypes()).containsExactly(ENTITY);
            assertThat(diff.removedTypes()).containsExactly(WORLD);
            assertThat(diff.commonTypes()).containsExactly(PLAYER);
            assertThat(diff.typeDiffs()).containsEntry(PLAYER, typeDiff);
        }

        @Test
        @DisplayName("throws on null source schema")
        void throwsOnNullSourceSchema() {
            assertThatThrownBy(() -> SchemaDiff.builder(null, targetSchema))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null target schema")
        void throwsOnNullTargetSchema() {
            assertThatThrownBy(() -> SchemaDiff.builder(sourceSchema, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null addedTypes")
        void throwsOnNullAddedTypes() {
            assertThatThrownBy(() -> SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null removedTypes")
        void throwsOnNullRemovedTypes() {
            assertThatThrownBy(() -> SchemaDiff.builder(sourceSchema, targetSchema)
                    .removedTypes(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null commonTypes")
        void throwsOnNullCommonTypes() {
            assertThatThrownBy(() -> SchemaDiff.builder(sourceSchema, targetSchema)
                    .commonTypes(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null typeDiffs")
        void throwsOnNullTypeDiffs() {
            assertThatThrownBy(() -> SchemaDiff.builder(sourceSchema, targetSchema)
                    .typeDiffs(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Accessors")
    class Accessors {

        @Test
        @DisplayName("source() returns source schema")
        void sourceReturnsSourceSchema() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.source()).isSameAs(sourceSchema);
        }

        @Test
        @DisplayName("target() returns target schema")
        void targetReturnsTargetSchema() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.target()).isSameAs(targetSchema);
        }

        @Test
        @DisplayName("addedTypes() returns unmodifiable set")
        void addedTypesReturnsUnmodifiableSet() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(PLAYER))
                    .build();

            assertThatThrownBy(() -> diff.addedTypes().add(ENTITY))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("removedTypes() returns unmodifiable set")
        void removedTypesReturnsUnmodifiableSet() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .removedTypes(Set.of(PLAYER))
                    .build();

            assertThatThrownBy(() -> diff.removedTypes().add(ENTITY))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("commonTypes() returns unmodifiable set")
        void commonTypesReturnsUnmodifiableSet() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .commonTypes(Set.of(PLAYER))
                    .build();

            assertThatThrownBy(() -> diff.commonTypes().add(ENTITY))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("typeDiffs() returns unmodifiable map")
        void typeDiffsReturnsUnmodifiableMap() {
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of());
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            assertThatThrownBy(() -> diff.typeDiffs().put(ENTITY, typeDiff))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("typeDiff()")
    class TypeDiffMethod {

        @Test
        @DisplayName("returns type diff when exists")
        void returnsTypeDiffWhenExists() {
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of());
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            assertThat(diff.typeDiff(PLAYER)).contains(typeDiff);
        }

        @Test
        @DisplayName("returns empty when type diff does not exist")
        void returnsEmptyWhenTypeDiffDoesNotExist() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.typeDiff(PLAYER)).isEmpty();
        }

        @Test
        @DisplayName("throws on null reference")
        void throwsOnNullReference() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThatThrownBy(() -> diff.typeDiff(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("hasChanges()")
    class HasChangesMethod {

        @Test
        @DisplayName("returns false for empty diff")
        void returnsFalseForEmptyDiff() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.hasChanges()).isFalse();
        }

        @Test
        @DisplayName("returns true when types added")
        void returnsTrueWhenTypesAdded() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(PLAYER))
                    .build();

            assertThat(diff.hasChanges()).isTrue();
        }

        @Test
        @DisplayName("returns true when types removed")
        void returnsTrueWhenTypesRemoved() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .removedTypes(Set.of(PLAYER))
                    .build();

            assertThat(diff.hasChanges()).isTrue();
        }

        @Test
        @DisplayName("returns true when fields changed")
        void returnsTrueWhenFieldsChanged() {
            final FieldDiff fieldDiff = FieldDiff.added(NAME_FIELD);
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(fieldDiff));
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            assertThat(diff.hasChanges()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasTypeChanges()")
    class HasTypeChangesMethod {

        @Test
        @DisplayName("returns false for empty diff")
        void returnsFalseForEmptyDiff() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.hasTypeChanges()).isFalse();
        }

        @Test
        @DisplayName("returns true for added types")
        void returnsTrueForAddedTypes() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(PLAYER))
                    .build();

            assertThat(diff.hasTypeChanges()).isTrue();
        }

        @Test
        @DisplayName("returns true for removed types")
        void returnsTrueForRemovedTypes() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .removedTypes(Set.of(PLAYER))
                    .build();

            assertThat(diff.hasTypeChanges()).isTrue();
        }

        @Test
        @DisplayName("returns false when only field changes exist")
        void returnsFalseWhenOnlyFieldChangesExist() {
            final FieldDiff fieldDiff = FieldDiff.added(NAME_FIELD);
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(fieldDiff));
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            assertThat(diff.hasTypeChanges()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasFieldChanges()")
    class HasFieldChangesMethod {

        @Test
        @DisplayName("returns false for empty diff")
        void returnsFalseForEmptyDiff() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.hasFieldChanges()).isFalse();
        }

        @Test
        @DisplayName("returns false when no type diffs")
        void returnsFalseWhenNoTypeDiffs() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(PLAYER))
                    .build();

            assertThat(diff.hasFieldChanges()).isFalse();
        }

        @Test
        @DisplayName("returns false when type diffs have no field changes")
        void returnsFalseWhenTypeDiffsHaveNoFieldChanges() {
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of());
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            assertThat(diff.hasFieldChanges()).isFalse();
        }

        @Test
        @DisplayName("returns true when type diffs have field changes")
        void returnsTrueWhenTypeDiffsHaveFieldChanges() {
            final FieldDiff fieldDiff = FieldDiff.added(NAME_FIELD);
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(fieldDiff));
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            assertThat(diff.hasFieldChanges()).isTrue();
        }

        @Test
        @DisplayName("returns false when only unchanged fields")
        void returnsFalseWhenOnlyUnchangedFields() {
            final FieldDiff unchangedField = FieldDiff.unchanged(NAME_FIELD, NAME_FIELD);
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(unchangedField));
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            assertThat(diff.hasFieldChanges()).isFalse();
        }
    }

    @Nested
    @DisplayName("totalChangeCount()")
    class TotalChangeCountMethod {

        @Test
        @DisplayName("returns zero for empty diff")
        void returnsZeroForEmptyDiff() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.totalChangeCount()).isZero();
        }

        @Test
        @DisplayName("counts added types")
        void countsAddedTypes() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(PLAYER, ENTITY))
                    .build();

            assertThat(diff.totalChangeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("counts removed types")
        void countsRemovedTypes() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .removedTypes(Set.of(PLAYER, ENTITY, WORLD))
                    .build();

            assertThat(diff.totalChangeCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("counts field changes")
        void countsFieldChanges() {
            final FieldInfo idField = FieldInfo.create("id", false, Type.INT, "id");
            final FieldDiff added = FieldDiff.added(NAME_FIELD);
            final FieldDiff removed = FieldDiff.removed(idField);
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(added, removed));
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            assertThat(diff.totalChangeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("combines all change types")
        void combinesAllChangeTypes() {
            final FieldDiff fieldDiff = FieldDiff.added(NAME_FIELD);
            final TypeDiff typeDiff = TypeDiff.of(PLAYER, Type.STRING, Type.STRING, List.of(fieldDiff));
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(ENTITY))
                    .removedTypes(Set.of(WORLD))
                    .typeDiffs(Map.of(PLAYER, typeDiff))
                    .build();

            // 1 added type + 1 removed type + 1 field change = 3
            assertThat(diff.totalChangeCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals returns true for identical diffs")
        void equalsReturnsTrueForIdenticalDiffs() {
            final SchemaDiff diff1 = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(PLAYER))
                    .build();
            final SchemaDiff diff2 = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(PLAYER))
                    .build();

            assertThat(diff1).isEqualTo(diff2);
            assertThat(diff1.hashCode()).isEqualTo(diff2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different added types")
        void equalsReturnsFalseForDifferentAddedTypes() {
            final SchemaDiff diff1 = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(PLAYER))
                    .build();
            final SchemaDiff diff2 = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(ENTITY))
                    .build();

            assertThat(diff1).isNotEqualTo(diff2);
        }

        @Test
        @DisplayName("equals returns false for different source versions")
        void equalsReturnsFalseForDifferentSourceVersions() {
            final Schema otherSource = MockSchemas.minimal(150);
            final SchemaDiff diff1 = SchemaDiff.builder(sourceSchema, targetSchema).build();
            final SchemaDiff diff2 = SchemaDiff.builder(otherSource, targetSchema).build();

            assertThat(diff1).isNotEqualTo(diff2);
        }

        @Test
        @DisplayName("equals returns false for different target versions")
        void equalsReturnsFalseForDifferentTargetVersions() {
            final Schema otherTarget = MockSchemas.minimal(250);
            final SchemaDiff diff1 = SchemaDiff.builder(sourceSchema, targetSchema).build();
            final SchemaDiff diff2 = SchemaDiff.builder(sourceSchema, otherTarget).build();

            assertThat(diff1).isNotEqualTo(diff2);
        }

        @Test
        @DisplayName("equals returns true for same instance")
        void equalsReturnsTrueForSameInstance() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff).isEqualTo(diff);
        }

        @Test
        @DisplayName("equals returns false for null")
        void equalsReturnsFalseForNull() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff).isNotEqualTo(null);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString contains source version")
        void toStringContainsSourceVersion() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.toString()).contains("source=DataVersion{version=100}");
        }

        @Test
        @DisplayName("toString contains target version")
        void toStringContainsTargetVersion() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema).build();

            assertThat(diff.toString()).contains("target=DataVersion{version=200}");
        }

        @Test
        @DisplayName("toString contains type counts")
        void toStringContainsTypeCounts() {
            final SchemaDiff diff = SchemaDiff.builder(sourceSchema, targetSchema)
                    .addedTypes(Set.of(PLAYER, ENTITY))
                    .removedTypes(Set.of(WORLD))
                    .build();

            assertThat(diff.toString())
                    .contains("addedTypes=2")
                    .contains("removedTypes=1");
        }
    }
}
