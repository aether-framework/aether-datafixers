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

package de.splatgames.aether.datafixers.schematools.introspection;

import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.schematools.introspection.TypeStructure.TypeKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TypeStructure}.
 */
@DisplayName("TypeStructure")
class TypeStructureTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference ENTITY = new TypeReference("entity");

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds with minimum required properties")
        void buildsWithMinimumRequiredProperties() {
            final TypeStructure structure = TypeStructure.builder(PLAYER).build();

            assertThat(structure.reference()).isEqualTo(PLAYER);
            assertThat(structure.description()).isEmpty();
            assertThat(structure.kind()).isEqualTo(TypeKind.UNKNOWN);
            assertThat(structure.fields()).isEmpty();
            assertThat(structure.children()).isEmpty();
        }

        @Test
        @DisplayName("builds with all properties")
        void buildsWithAllProperties() {
            final FieldInfo field = FieldInfo.create("name", false, Type.STRING, "name");
            final TypeStructure child = TypeStructure.builder(ENTITY).build();

            final TypeStructure structure = TypeStructure.builder(PLAYER)
                    .description("Player type")
                    .kind(TypeKind.FIELD)
                    .fields(List.of(field))
                    .children(List.of(child))
                    .build();

            assertThat(structure.reference()).isEqualTo(PLAYER);
            assertThat(structure.description()).isEqualTo("Player type");
            assertThat(structure.kind()).isEqualTo(TypeKind.FIELD);
            assertThat(structure.fields()).containsExactly(field);
            assertThat(structure.children()).containsExactly(child);
        }

        @Test
        @DisplayName("throws on null reference")
        void throwsOnNullReference() {
            assertThatThrownBy(() -> TypeStructure.builder(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null description")
        void throwsOnNullDescription() {
            assertThatThrownBy(() -> TypeStructure.builder(PLAYER).description(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null kind")
        void throwsOnNullKind() {
            assertThatThrownBy(() -> TypeStructure.builder(PLAYER).kind(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null fields")
        void throwsOnNullFields() {
            assertThatThrownBy(() -> TypeStructure.builder(PLAYER).fields(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null children")
        void throwsOnNullChildren() {
            assertThatThrownBy(() -> TypeStructure.builder(PLAYER).children(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("builder can be reused")
        void builderCanBeReused() {
            final TypeStructure.Builder builder = TypeStructure.builder(PLAYER);

            final TypeStructure first = builder.description("First").build();
            final TypeStructure second = builder.description("Second").build();

            assertThat(first.description()).isEqualTo("First");
            assertThat(second.description()).isEqualTo("Second");
        }
    }

    @Nested
    @DisplayName("Accessors")
    class Accessors {

        @Test
        @DisplayName("reference() returns correct reference")
        void referenceReturnsCorrectReference() {
            final TypeStructure structure = TypeStructure.builder(PLAYER).build();

            assertThat(structure.reference()).isEqualTo(PLAYER);
        }

        @Test
        @DisplayName("fields() returns immutable list")
        void fieldsReturnsImmutableList() {
            final FieldInfo field = FieldInfo.create("name", false, Type.STRING, "name");
            final TypeStructure structure = TypeStructure.builder(PLAYER)
                    .fields(List.of(field))
                    .build();

            assertThatThrownBy(() -> structure.fields().add(field))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("children() returns immutable list")
        void childrenReturnsImmutableList() {
            final TypeStructure child = TypeStructure.builder(ENTITY).build();
            final TypeStructure structure = TypeStructure.builder(PLAYER)
                    .children(List.of(child))
                    .build();

            assertThatThrownBy(() -> structure.children().add(child))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("structurallyEquals()")
    class StructurallyEqualsMethod {

        @Test
        @DisplayName("returns true for same kind and fields")
        void returnsTrueForSameKindAndFields() {
            final FieldInfo field = FieldInfo.create("name", false, Type.STRING, "name");

            final TypeStructure struct1 = TypeStructure.builder(PLAYER)
                    .kind(TypeKind.FIELD)
                    .fields(List.of(field))
                    .description("Description 1")
                    .build();

            final TypeStructure struct2 = TypeStructure.builder(PLAYER)
                    .kind(TypeKind.FIELD)
                    .fields(List.of(field))
                    .description("Description 2")  // Different description
                    .build();

            assertThat(struct1.structurallyEquals(struct2)).isTrue();
        }

        @Test
        @DisplayName("returns false for different kinds")
        void returnsFalseForDifferentKinds() {
            final TypeStructure struct1 = TypeStructure.builder(PLAYER)
                    .kind(TypeKind.FIELD)
                    .build();

            final TypeStructure struct2 = TypeStructure.builder(PLAYER)
                    .kind(TypeKind.PRIMITIVE)
                    .build();

            assertThat(struct1.structurallyEquals(struct2)).isFalse();
        }

        @Test
        @DisplayName("returns false for different fields")
        void returnsFalseForDifferentFields() {
            final FieldInfo field1 = FieldInfo.create("name", false, Type.STRING, "name");
            final FieldInfo field2 = FieldInfo.create("id", false, Type.STRING, "id");

            final TypeStructure struct1 = TypeStructure.builder(PLAYER)
                    .fields(List.of(field1))
                    .build();

            final TypeStructure struct2 = TypeStructure.builder(PLAYER)
                    .fields(List.of(field2))
                    .build();

            assertThat(struct1.structurallyEquals(struct2)).isFalse();
        }

        @Test
        @DisplayName("returns false for null argument")
        void returnsFalseForNullArgument() {
            final TypeStructure structure = TypeStructure.builder(PLAYER).build();

            assertThat(structure.structurallyEquals(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals returns true for identical structures")
        void equalsReturnsTrueForIdenticalStructures() {
            final TypeStructure struct1 = TypeStructure.builder(PLAYER)
                    .description("Test")
                    .kind(TypeKind.FIELD)
                    .build();

            final TypeStructure struct2 = TypeStructure.builder(PLAYER)
                    .description("Test")
                    .kind(TypeKind.FIELD)
                    .build();

            assertThat(struct1).isEqualTo(struct2);
            assertThat(struct1.hashCode()).isEqualTo(struct2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different references")
        void equalsReturnsFalseForDifferentReferences() {
            final TypeStructure struct1 = TypeStructure.builder(PLAYER).build();
            final TypeStructure struct2 = TypeStructure.builder(ENTITY).build();

            assertThat(struct1).isNotEqualTo(struct2);
        }

        @Test
        @DisplayName("equals returns false for null")
        void equalsReturnsFalseForNull() {
            final TypeStructure structure = TypeStructure.builder(PLAYER).build();

            assertThat(structure).isNotEqualTo(null);
        }

        @Test
        @DisplayName("equals returns true for same instance")
        void equalsReturnsTrueForSameInstance() {
            final TypeStructure structure = TypeStructure.builder(PLAYER).build();

            assertThat(structure).isEqualTo(structure);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString contains reference")
        void toStringContainsReference() {
            final TypeStructure structure = TypeStructure.builder(PLAYER).build();

            assertThat(structure.toString()).contains("player");
        }

        @Test
        @DisplayName("toString contains kind")
        void toStringContainsKind() {
            final TypeStructure structure = TypeStructure.builder(PLAYER)
                    .kind(TypeKind.PRIMITIVE)
                    .build();

            assertThat(structure.toString()).contains("PRIMITIVE");
        }

        @Test
        @DisplayName("toString contains field count")
        void toStringContainsFieldCount() {
            final FieldInfo field = FieldInfo.create("name", false, Type.STRING, "name");
            final TypeStructure structure = TypeStructure.builder(PLAYER)
                    .fields(List.of(field))
                    .build();

            assertThat(structure.toString()).contains("fields=1");
        }
    }

    @Nested
    @DisplayName("TypeKind enum")
    class TypeKindTests {

        @Test
        @DisplayName("all expected kinds exist")
        void allExpectedKindsExist() {
            assertThat(TypeKind.values()).contains(
                    TypeKind.PRIMITIVE,
                    TypeKind.LIST,
                    TypeKind.OPTIONAL,
                    TypeKind.FIELD,
                    TypeKind.TAGGED_CHOICE,
                    TypeKind.PRODUCT,
                    TypeKind.SUM,
                    TypeKind.PASSTHROUGH,
                    TypeKind.UNKNOWN
            );
        }
    }
}
