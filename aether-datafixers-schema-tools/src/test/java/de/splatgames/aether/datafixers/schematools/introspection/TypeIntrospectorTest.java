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

import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.template.TypeFamily;
import de.splatgames.aether.datafixers.api.type.template.TypeTemplate;
import de.splatgames.aether.datafixers.schematools.introspection.TypeStructure.TypeKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TypeIntrospector}.
 */
@DisplayName("TypeIntrospector")
class TypeIntrospectorTest {

    @Nested
    @DisplayName("introspect()")
    class IntrospectMethod {

        @Test
        @DisplayName("introspects primitive type")
        void introspectsPrimitiveType() {
            final TypeStructure structure = TypeIntrospector.introspect(Type.STRING);

            assertThat(structure).isNotNull();
            assertThat(structure.kind()).isEqualTo(TypeKind.PRIMITIVE);
            assertThat(structure.fields()).isEmpty();
        }

        @Test
        @DisplayName("introspects int type")
        void introspectsIntType() {
            final TypeStructure structure = TypeIntrospector.introspect(Type.INT);

            assertThat(structure.kind()).isEqualTo(TypeKind.PRIMITIVE);
        }

        @Test
        @DisplayName("introspects bool type")
        void introspectsBoolType() {
            final TypeStructure structure = TypeIntrospector.introspect(Type.BOOL);

            assertThat(structure.kind()).isEqualTo(TypeKind.PRIMITIVE);
        }

        @Test
        @DisplayName("throws on null type")
        void throwsOnNullType() {
            assertThatThrownBy(() -> TypeIntrospector.introspect(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("extractFields()")
    class ExtractFieldsMethod {

        @Test
        @DisplayName("returns empty list for primitive type")
        void returnsEmptyListForPrimitiveType() {
            final List<FieldInfo> fields = TypeIntrospector.extractFields(Type.STRING);

            assertThat(fields).isEmpty();
        }

        @Test
        @DisplayName("extracts fields from field type template")
        void extractsFieldsFromFieldTypeTemplate() {
            final TypeTemplate template = DSL.field("name", DSL.string());
            final Type<?> type = template.apply(TypeFamily.empty());

            final List<FieldInfo> fields = TypeIntrospector.extractFields(type);

            assertThat(fields).hasSize(1);
            assertThat(fields.get(0).name()).isEqualTo("name");
            assertThat(fields.get(0).isOptional()).isFalse();
        }

        @Test
        @DisplayName("handles optional field type")
        void handlesOptionalFieldType() {
            // DSL.optionalField creates an optional wrapper, not a direct FieldType
            // The field extraction only extracts direct FieldType instances
            final TypeTemplate template = DSL.optionalField("health", DSL.intType());
            final Type<?> type = template.apply(TypeFamily.empty());

            final List<FieldInfo> fields = TypeIntrospector.extractFields(type);

            // Optional field wrapper doesn't produce direct field info
            assertThat(fields).isEmpty();
        }

        @Test
        @DisplayName("extracts multiple fields from compound type")
        void extractsMultipleFieldsFromCompoundType() {
            final TypeTemplate template = DSL.and(
                    DSL.field("name", DSL.string()),
                    DSL.field("level", DSL.intType())
            );
            final Type<?> type = template.apply(TypeFamily.empty());

            final List<FieldInfo> fields = TypeIntrospector.extractFields(type);

            assertThat(fields).hasSize(2);
            assertThat(fields).extracting(FieldInfo::name)
                    .containsExactlyInAnyOrder("name", "level");
        }

        @Test
        @DisplayName("throws on null type")
        void throwsOnNullType() {
            assertThatThrownBy(() -> TypeIntrospector.extractFields(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("hasField()")
    class HasFieldMethod {

        @Test
        @DisplayName("returns true when field exists")
        void returnsTrueWhenFieldExists() {
            final TypeTemplate template = DSL.field("name", DSL.string());
            final Type<?> type = template.apply(TypeFamily.empty());

            assertThat(TypeIntrospector.hasField(type, "name")).isTrue();
        }

        @Test
        @DisplayName("returns false when field does not exist")
        void returnsFalseWhenFieldDoesNotExist() {
            final TypeTemplate template = DSL.field("name", DSL.string());
            final Type<?> type = template.apply(TypeFamily.empty());

            assertThat(TypeIntrospector.hasField(type, "age")).isFalse();
        }

        @Test
        @DisplayName("returns false for primitive type")
        void returnsFalseForPrimitiveType() {
            assertThat(TypeIntrospector.hasField(Type.STRING, "anything")).isFalse();
        }

        @Test
        @DisplayName("throws on null type")
        void throwsOnNullType() {
            assertThatThrownBy(() -> TypeIntrospector.hasField(null, "name"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null field name")
        void throwsOnNullFieldName() {
            assertThatThrownBy(() -> TypeIntrospector.hasField(Type.STRING, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("determineKind()")
    class DetermineKindMethod {

        @Test
        @DisplayName("returns PRIMITIVE for string type")
        void returnsPrimitiveForStringType() {
            assertThat(TypeIntrospector.determineKind(Type.STRING))
                    .isEqualTo(TypeKind.PRIMITIVE);
        }

        @Test
        @DisplayName("returns PRIMITIVE for int type")
        void returnsPrimitiveForIntType() {
            assertThat(TypeIntrospector.determineKind(Type.INT))
                    .isEqualTo(TypeKind.PRIMITIVE);
        }

        @Test
        @DisplayName("returns PRIMITIVE for bool type")
        void returnsPrimitiveForBoolType() {
            assertThat(TypeIntrospector.determineKind(Type.BOOL))
                    .isEqualTo(TypeKind.PRIMITIVE);
        }

        @Test
        @DisplayName("returns PRIMITIVE for long type")
        void returnsPrimitiveForLongType() {
            assertThat(TypeIntrospector.determineKind(Type.LONG))
                    .isEqualTo(TypeKind.PRIMITIVE);
        }

        @Test
        @DisplayName("returns PRIMITIVE for double type")
        void returnsPrimitiveForDoubleType() {
            assertThat(TypeIntrospector.determineKind(Type.DOUBLE))
                    .isEqualTo(TypeKind.PRIMITIVE);
        }

        @Test
        @DisplayName("returns PRIMITIVE for float type")
        void returnsPrimitiveForFloatType() {
            assertThat(TypeIntrospector.determineKind(Type.FLOAT))
                    .isEqualTo(TypeKind.PRIMITIVE);
        }

        @Test
        @DisplayName("returns FIELD for field type")
        void returnsFieldForFieldType() {
            final TypeTemplate template = DSL.field("test", DSL.string());
            final Type<?> type = template.apply(TypeFamily.empty());

            assertThat(TypeIntrospector.determineKind(type))
                    .isEqualTo(TypeKind.FIELD);
        }

        @Test
        @DisplayName("returns LIST for list type")
        void returnsListForListType() {
            final TypeTemplate template = DSL.list(DSL.string());
            final Type<?> type = template.apply(TypeFamily.empty());

            assertThat(TypeIntrospector.determineKind(type))
                    .isEqualTo(TypeKind.LIST);
        }

        @Test
        @DisplayName("throws on null type")
        void throwsOnNullType() {
            assertThatThrownBy(() -> TypeIntrospector.determineKind(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles nested compound types")
        void handlesNestedCompoundTypes() {
            final TypeTemplate inner = DSL.and(
                    DSL.field("x", DSL.intType()),
                    DSL.field("y", DSL.intType())
            );
            final TypeTemplate outer = DSL.and(
                    DSL.field("name", DSL.string()),
                    DSL.field("position", inner)
            );
            final Type<?> type = outer.apply(TypeFamily.empty());

            final List<FieldInfo> fields = TypeIntrospector.extractFields(type);

            assertThat(fields).isNotEmpty();
            assertThat(fields).extracting(FieldInfo::name)
                    .contains("name", "position", "x", "y");
        }

        @Test
        @DisplayName("handles optional field types")
        void handlesOptionalFieldTypes() {
            final TypeTemplate template = DSL.optionalField("optional", DSL.string());
            final Type<?> type = template.apply(TypeFamily.empty());

            final TypeStructure structure = TypeIntrospector.introspect(type);

            assertThat(structure.kind()).isEqualTo(TypeKind.FIELD);
        }

        @Test
        @DisplayName("handles list of complex types")
        void handlesListOfComplexTypes() {
            final TypeTemplate element = DSL.field("name", DSL.string());
            final TypeTemplate template = DSL.list(element);
            final Type<?> type = template.apply(TypeFamily.empty());

            final TypeStructure structure = TypeIntrospector.introspect(type);

            assertThat(structure.kind()).isEqualTo(TypeKind.LIST);
            assertThat(structure.children()).hasSize(1);
        }

        @Test
        @DisplayName("handles passthrough type")
        void handlesPassthroughType() {
            final Type<?> type = Type.PASSTHROUGH;

            final TypeStructure structure = TypeIntrospector.introspect(type);

            assertThat(structure.kind()).isEqualTo(TypeKind.PASSTHROUGH);
        }
    }
}
