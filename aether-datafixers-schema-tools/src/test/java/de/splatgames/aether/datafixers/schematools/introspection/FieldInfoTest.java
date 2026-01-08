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

import de.splatgames.aether.datafixers.api.type.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FieldInfo}.
 */
@DisplayName("FieldInfo")
class FieldInfoTest {

    @Nested
    @DisplayName("of() factory method")
    class OfFactoryMethod {

        @Test
        @DisplayName("creates FieldInfo with all parameters")
        void createsFieldInfoWithAllParameters() {
            final FieldInfo field = FieldInfo.create("name", false, Type.STRING, "player.name");

            assertThat(field.name()).isEqualTo("name");
            assertThat(field.isOptional()).isFalse();
            assertThat(field.fieldType()).isSameAs(Type.STRING);
            assertThat(field.path()).isEqualTo("player.name");
        }

        @Test
        @DisplayName("creates optional field")
        void createsOptionalField() {
            final FieldInfo field = FieldInfo.create("health", true, Type.INT, "player.health");

            assertThat(field.name()).isEqualTo("health");
            assertThat(field.isOptional()).isTrue();
        }

        @Test
        @DisplayName("creates required field")
        void createsRequiredField() {
            final FieldInfo field = FieldInfo.create("id", false, Type.STRING, "entity.id");

            assertThat(field.isOptional()).isFalse();
        }

        @Test
        @DisplayName("throws on null name")
        void throwsOnNullName() {
            assertThatThrownBy(() -> FieldInfo.create(null, false, Type.STRING, "path"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null fieldType")
        void throwsOnNullFieldType() {
            assertThatThrownBy(() -> FieldInfo.create("name", false, null, "path"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null path")
        void throwsOnNullPath() {
            assertThatThrownBy(() -> FieldInfo.create("name", false, Type.STRING, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Accessors")
    class Accessors {

        @Test
        @DisplayName("name() returns field name")
        void nameReturnsFieldName() {
            final FieldInfo field = FieldInfo.create("testField", false, Type.INT, "root.testField");

            assertThat(field.name()).isEqualTo("testField");
        }

        @Test
        @DisplayName("path() returns full path")
        void pathReturnsFullPath() {
            final FieldInfo field = FieldInfo.create("x", false, Type.DOUBLE, "position.x");

            assertThat(field.path()).isEqualTo("position.x");
        }

        @Test
        @DisplayName("fieldType() returns the type")
        void fieldTypeReturnsType() {
            final FieldInfo field = FieldInfo.create("value", false, Type.LONG, "data.value");

            assertThat(field.fieldType()).isSameAs(Type.LONG);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals returns true for identical fields")
        void equalsReturnsTrueForIdenticalFields() {
            final FieldInfo field1 = FieldInfo.create("name", false, Type.STRING, "player.name");
            final FieldInfo field2 = FieldInfo.create("name", false, Type.STRING, "player.name");

            assertThat(field1).isEqualTo(field2);
            assertThat(field1.hashCode()).isEqualTo(field2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different names")
        void equalsReturnsFalseForDifferentNames() {
            final FieldInfo field1 = FieldInfo.create("name", false, Type.STRING, "player.name");
            final FieldInfo field2 = FieldInfo.create("id", false, Type.STRING, "player.name");

            assertThat(field1).isNotEqualTo(field2);
        }

        @Test
        @DisplayName("equals returns false for different optionality")
        void equalsReturnsFalseForDifferentOptionality() {
            final FieldInfo field1 = FieldInfo.create("name", false, Type.STRING, "player.name");
            final FieldInfo field2 = FieldInfo.create("name", true, Type.STRING, "player.name");

            assertThat(field1).isNotEqualTo(field2);
        }

        @Test
        @DisplayName("equals returns false for different paths")
        void equalsReturnsFalseForDifferentPaths() {
            final FieldInfo field1 = FieldInfo.create("name", false, Type.STRING, "player.name");
            final FieldInfo field2 = FieldInfo.create("name", false, Type.STRING, "entity.name");

            assertThat(field1).isNotEqualTo(field2);
        }

        @Test
        @DisplayName("equals returns false for null")
        void equalsReturnsFalseForNull() {
            final FieldInfo field = FieldInfo.create("name", false, Type.STRING, "path");

            assertThat(field).isNotEqualTo(null);
        }

        @Test
        @DisplayName("equals returns false for different type")
        void equalsReturnsFalseForDifferentType() {
            final FieldInfo field = FieldInfo.create("name", false, Type.STRING, "path");

            assertThat(field).isNotEqualTo("not a FieldInfo");
        }

        @Test
        @DisplayName("equals returns true for same instance")
        void equalsReturnsTrueForSameInstance() {
            final FieldInfo field = FieldInfo.create("name", false, Type.STRING, "path");

            assertThat(field).isEqualTo(field);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString for required field")
        void toStringForRequiredField() {
            final FieldInfo field = FieldInfo.create("name", false, Type.STRING, "player.name");

            final String result = field.toString();

            assertThat(result).contains("name");
            assertThat(result).contains("player.name");
            assertThat(result).doesNotStartWith("?");
        }

        @Test
        @DisplayName("toString for optional field starts with ?")
        void toStringForOptionalFieldStartsWithQuestionMark() {
            final FieldInfo field = FieldInfo.create("health", true, Type.INT, "player.health");

            final String result = field.toString();

            assertThat(result).startsWith("?health");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("empty name is allowed")
        void emptyNameIsAllowed() {
            final FieldInfo field = FieldInfo.create("", false, Type.STRING, "root");

            assertThat(field.name()).isEmpty();
        }

        @Test
        @DisplayName("empty path is allowed")
        void emptyPathIsAllowed() {
            final FieldInfo field = FieldInfo.create("field", false, Type.STRING, "");

            assertThat(field.path()).isEmpty();
        }

        @Test
        @DisplayName("deeply nested path works")
        void deeplyNestedPathWorks() {
            final String deepPath = "a.b.c.d.e.f.g.h.i.j.k";
            final FieldInfo field = FieldInfo.create("k", false, Type.STRING, deepPath);

            assertThat(field.path()).isEqualTo(deepPath);
        }

        @Test
        @DisplayName("unicode field name works")
        void unicodeFieldNameWorks() {
            final FieldInfo field = FieldInfo.create("名前", false, Type.STRING, "player.名前");

            assertThat(field.name()).isEqualTo("名前");
        }
    }
}
