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

package de.splatgames.aether.datafixers.testkit.assertion;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.testkit.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DynamicAssert")
class DynamicAssertTest {

    @Nested
    @DisplayName("Type assertions")
    class TypeAssertions {

        @Test
        @DisplayName("isMap() passes for objects")
        void isMapPassesForObjects() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("key", "value")
                    .build();

            assertThat(dynamic).isMap();
        }

        @Test
        @DisplayName("isMap() fails for non-objects")
        void isMapFailsForNonObjects() {
            final Dynamic<JsonElement> dynamic = TestData.gson().string("hello");

            assertThatThrownBy(() -> assertThat(dynamic).isMap())
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("isList() passes for arrays")
        void isListPassesForArrays() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(1).add(2).add(3)
                    .build();

            assertThat(dynamic).isList();
        }

        @Test
        @DisplayName("isString() passes for strings")
        void isStringPassesForStrings() {
            final Dynamic<JsonElement> dynamic = TestData.gson().string("hello");

            assertThat(dynamic).isString();
        }

        @Test
        @DisplayName("isNumber() passes for numbers")
        void isNumberPassesForNumbers() {
            final Dynamic<JsonElement> dynamic = TestData.gson().integer(42);

            assertThat(dynamic).isNumber();
        }

        @Test
        @DisplayName("isBoolean() passes for booleans")
        void isBooleanPassesForBooleans() {
            final Dynamic<JsonElement> dynamic = TestData.gson().bool(true);

            assertThat(dynamic).isBoolean();
        }
    }

    @Nested
    @DisplayName("Field assertions")
    class FieldAssertions {

        @Test
        @DisplayName("hasField() passes when field exists")
        void hasFieldPassesWhenFieldExists() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            assertThat(dynamic).hasField("name");
        }

        @Test
        @DisplayName("hasField() fails when field missing")
        void hasFieldFailsWhenFieldMissing() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).hasField("missing"))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("doesNotHaveField() passes when field missing")
        void doesNotHaveFieldPassesWhenFieldMissing() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            assertThat(dynamic).doesNotHaveField("missing");
        }

        @Test
        @DisplayName("hasStringField() validates string value")
        void hasStringFieldValidatesStringValue() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            assertThat(dynamic).hasStringField("name", "Alice");
        }

        @Test
        @DisplayName("hasIntField() validates int value")
        void hasIntFieldValidatesIntValue() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("age", 30)
                    .build();

            assertThat(dynamic).hasIntField("age", 30);
        }

        @Test
        @DisplayName("hasBooleanField() validates boolean value")
        void hasBooleanFieldValidatesBooleanValue() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("active", true)
                    .build();

            assertThat(dynamic).hasBooleanField("active", true);
        }
    }

    @Nested
    @DisplayName("Navigation assertions")
    class NavigationAssertions {

        @Test
        @DisplayName("field() allows chained assertions on nested field")
        void fieldAllowsChainedAssertions() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .putObject("user", u -> u.put("name", "Bob"))
                    .build();

            assertThat(dynamic)
                    .field("user")
                    .hasStringField("name", "Bob");
        }

        @Test
        @DisplayName("atPath() navigates dot-separated path")
        void atPathNavigatesDotSeparatedPath() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .putObject("level1", l1 -> l1
                            .putObject("level2", l2 -> l2
                                    .put("value", "deep")))
                    .build();

            assertThat(dynamic)
                    .atPath("level1.level2.value")
                    .hasStringValue("deep");
        }

        @Test
        @DisplayName("atIndex() accesses list element")
        void atIndexAccessesListElement() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add("first")
                    .add("second")
                    .add("third")
                    .build();

            assertThat(dynamic)
                    .atIndex(1)
                    .hasStringValue("second");
        }
    }

    @Nested
    @DisplayName("Value assertions")
    class ValueAssertions {

        @Test
        @DisplayName("hasStringValue() validates string content")
        void hasStringValueValidatesStringContent() {
            final Dynamic<JsonElement> dynamic = TestData.gson().string("hello");

            assertThat(dynamic).hasStringValue("hello");
        }

        @Test
        @DisplayName("hasIntValue() validates int content")
        void hasIntValueValidatesIntContent() {
            final Dynamic<JsonElement> dynamic = TestData.gson().integer(42);

            assertThat(dynamic).hasIntValue(42);
        }

        @Test
        @DisplayName("hasDoubleValue() validates double content")
        void hasDoubleValueValidatesDoubleContent() {
            final Dynamic<JsonElement> dynamic = TestData.gson().doubleValue(3.14);

            assertThat(dynamic).hasDoubleValue(3.14, 0.001);
        }

        @Test
        @DisplayName("hasBooleanValue() validates boolean content")
        void hasBooleanValueValidatesBooleanContent() {
            final Dynamic<JsonElement> dynamic = TestData.gson().bool(true);

            assertThat(dynamic).hasBooleanValue(true);
        }
    }

    @Nested
    @DisplayName("List assertions")
    class ListAssertions {

        @Test
        @DisplayName("hasSize() validates list size")
        void hasSizeValidatesListSize() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(1).add(2).add(3)
                    .build();

            assertThat(dynamic).hasSize(3);
        }

        @Test
        @DisplayName("isEmpty() passes for empty list")
        void isEmptyPassesForEmptyList() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list().build();

            assertThat(dynamic).isEmpty();
        }

        @Test
        @DisplayName("isNotEmpty() passes for non-empty list")
        void isNotEmptyPassesForNonEmptyList() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add("item")
                    .build();

            assertThat(dynamic).isNotEmpty();
        }

        @Test
        @DisplayName("containsStringValues() validates list contents")
        void containsStringValuesValidatesListContents() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add("a").add("b").add("c")
                    .build();

            assertThat(dynamic).containsStringValues("a", "b", "c");
        }
    }

    @Nested
    @DisplayName("Equality assertions")
    class EqualityAssertions {

        @Test
        @DisplayName("isEqualTo() compares Dynamic values")
        void isEqualToComparesDynamicValues() {
            final Dynamic<JsonElement> d1 = TestData.gson().object()
                    .put("key", "value")
                    .build();
            final Dynamic<JsonElement> d2 = TestData.gson().object()
                    .put("key", "value")
                    .build();

            assertThat(d1).isEqualTo(d2);
        }

        @Test
        @DisplayName("isNotEqualTo() passes for different values")
        void isNotEqualToPassesForDifferentValues() {
            final Dynamic<JsonElement> d1 = TestData.gson().object()
                    .put("key", "value1")
                    .build();
            final Dynamic<JsonElement> d2 = TestData.gson().object()
                    .put("key", "value2")
                    .build();

            assertThat(d1).isNotEqualTo(d2);
        }
    }

    @Nested
    @DisplayName("Chained assertions")
    class ChainedAssertions {

        @Test
        @DisplayName("multiple assertions can be chained")
        void multipleAssertionsCanBeChained() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .put("age", 30)
                    .put("active", true)
                    .putList("tags", list -> list.add("admin").add("user"))
                    .build();

            assertThat(dynamic)
                    .isMap()
                    .hasField("name")
                    .hasStringField("name", "Alice")
                    .hasIntField("age", 30)
                    .hasBooleanField("active", true)
                    .field("tags")
                    .isList()
                    .hasSize(2);
        }
    }

    @Nested
    @DisplayName("Failure paths")
    class FailurePaths {

        @Test
        @DisplayName("isList fails for non-lists")
        void isListFailsForNonLists() {
            final Dynamic<JsonElement> dynamic = TestData.gson().string("hello");

            assertThatThrownBy(() -> assertThat(dynamic).isList())
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("isString fails for non-strings")
        void isStringFailsForNonStrings() {
            final Dynamic<JsonElement> dynamic = TestData.gson().integer(42);

            assertThatThrownBy(() -> assertThat(dynamic).isString())
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("isNumber fails for non-numbers")
        void isNumberFailsForNonNumbers() {
            final Dynamic<JsonElement> dynamic = TestData.gson().string("hello");

            assertThatThrownBy(() -> assertThat(dynamic).isNumber())
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("isBoolean fails for non-booleans")
        void isBooleanFailsForNonBooleans() {
            final Dynamic<JsonElement> dynamic = TestData.gson().string("hello");

            assertThatThrownBy(() -> assertThat(dynamic).isBoolean())
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("doesNotHaveField fails when field exists")
        void doesNotHaveFieldFailsWhenFieldExists() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).doesNotHaveField("name"))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("hasStringField fails for wrong value")
        void hasStringFieldFailsForWrongValue() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Bob")
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).hasStringField("name", "Alice"))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("hasIntField fails for wrong value")
        void hasIntFieldFailsForWrongValue() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("age", 25)
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).hasIntField("age", 30))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("hasSize fails for wrong size")
        void hasSizeFailsForWrongSize() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(1).add(2)
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).hasSize(5))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("isNotEmpty fails for empty list")
        void isNotEmptyFailsForEmptyList() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list().build();

            assertThatThrownBy(() -> assertThat(dynamic).isNotEmpty())
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("atIndex fails for out of bounds")
        void atIndexFailsForOutOfBounds() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add("one")
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).atIndex(5))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("atPath fails for missing path")
        void atPathFailsForMissingPath() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).atPath("missing.path"))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("isEqualTo fails for different values")
        void isEqualToFailsForDifferentValues() {
            final Dynamic<JsonElement> d1 = TestData.gson().object()
                    .put("key", "value1")
                    .build();
            final Dynamic<JsonElement> d2 = TestData.gson().object()
                    .put("key", "value2")
                    .build();

            assertThatThrownBy(() -> assertThat(d1).isEqualTo(d2))
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    @DisplayName("Additional field assertions")
    class AdditionalFieldAssertions {

        @Test
        @DisplayName("hasFields passes when all fields exist")
        void hasFieldsPassesWhenAllFieldsExist() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .put("age", 30)
                    .put("active", true)
                    .build();

            assertThat(dynamic).hasFields("name", "age", "active");
        }

        @Test
        @DisplayName("hasOnlyFields passes for exact match")
        void hasOnlyFieldsPassesForExactMatch() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .put("age", 30)
                    .build();

            assertThat(dynamic).hasOnlyFields("name", "age");
        }

        @Test
        @DisplayName("hasOnlyFields fails for extra fields")
        void hasOnlyFieldsFailsForExtraFields() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .put("age", 30)
                    .put("extra", true)
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).hasOnlyFields("name", "age"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("extra");
        }

        @Test
        @DisplayName("hasLongField validates long value")
        void hasLongFieldValidatesLongValue() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("timestamp", 1234567890123L)
                    .build();

            assertThat(dynamic).hasLongField("timestamp", 1234567890123L);
        }

        @Test
        @DisplayName("hasDoubleField validates double value")
        void hasDoubleFieldValidatesDoubleValue() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("score", 95.5)
                    .build();

            assertThat(dynamic).hasDoubleField("score", 95.5, 0.01);
        }

        @Test
        @DisplayName("hasLongValue validates long content")
        void hasLongValueValidatesLongContent() {
            final Dynamic<JsonElement> dynamic = TestData.gson().longValue(9876543210L);

            assertThat(dynamic).hasLongValue(9876543210L);
        }
    }

    @Nested
    @DisplayName("List value assertions")
    class ListValueAssertions {

        @Test
        @DisplayName("containsIntValues validates integer contents")
        void containsIntValuesValidatesIntegerContents() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(1).add(2).add(3)
                    .build();

            assertThat(dynamic).containsIntValues(1, 2, 3);
        }

        @Test
        @DisplayName("containsStringValues fails when value missing")
        void containsStringValuesFailsWhenValueMissing() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add("a").add("b")
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).containsStringValues("c"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("contain");
        }

        @Test
        @DisplayName("containsIntValues fails when value missing")
        void containsIntValuesFailsWhenValueMissing() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(1).add(2)
                    .build();

            assertThatThrownBy(() -> assertThat(dynamic).containsIntValues(5))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("contain");
        }
    }

    @Nested
    @DisplayName("Custom validation")
    class CustomValidation {

        @Test
        @DisplayName("satisfies allows custom assertions")
        void satisfiesAllowsCustomAssertions() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("count", 5)
                    .build();

            assertThat(dynamic).satisfies(d -> {
                final int count = d.get("count").asInt().orElse(0);
                org.assertj.core.api.Assertions.assertThat(count).isGreaterThan(0);
            });
        }
    }
}
