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

package de.splatgames.aether.datafixers.api.optic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Prism}.
 */
@DisplayName("Prism")
class PrismTest {

    // Prisms for each case
    private final Prism<JsonValue, JsonValue, String, String> stringPrism = Prism.of(
            "json.string",
            json -> json instanceof JsonString js ? Optional.of(js.value()) : Optional.empty(),
            JsonString::new
    );
    private final Prism<JsonValue, JsonValue, Double, Double> numberPrism = Prism.of(
            "json.number",
            json -> json instanceof JsonNumber jn ? Optional.of(jn.value()) : Optional.empty(),
            JsonNumber::new
    );
    private final Prism<JsonValue, JsonValue, java.util.List<JsonValue>, java.util.List<JsonValue>> arrayPrism = Prism.of(
            "json.array",
            json -> json instanceof JsonArray ja ? Optional.of(ja.values()) : Optional.empty(),
            JsonArray::new
    );

    // Test sum types
    sealed interface JsonValue permits JsonString, JsonNumber, JsonNull, JsonArray {
    }

    record JsonString(String value) implements JsonValue {
    }

    record JsonNumber(double value) implements JsonValue {
    }

    record JsonNull() implements JsonValue {
    }

    record JsonArray(java.util.List<JsonValue> values) implements JsonValue {
    }

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        @DisplayName("of() creates prism with correct id")
        void ofCreatesPrismWithCorrectId() {
            assertThat(stringPrism.id()).isEqualTo("json.string");
            assertThat(numberPrism.id()).isEqualTo("json.number");
        }

        @Test
        @DisplayName("of() creates functional prism")
        void ofCreatesFunctionalPrism() {
            final JsonValue text = new JsonString("hello");

            assertThat(stringPrism.getOption(text)).contains("hello");
            assertThat(stringPrism.reverseGet("world")).isEqualTo(new JsonString("world"));
        }
    }

    @Nested
    @DisplayName("getOption()")
    class GetOptionOperation {

        @Test
        @DisplayName("getOption() returns value when matching")
        void getOptionReturnsValueWhenMatching() {
            final JsonValue text = new JsonString("hello");

            assertThat(stringPrism.getOption(text)).contains("hello");
        }

        @Test
        @DisplayName("getOption() returns empty when not matching")
        void getOptionReturnsEmptyWhenNotMatching() {
            final JsonValue number = new JsonNumber(42.0);

            assertThat(stringPrism.getOption(number)).isEmpty();
        }

        @Test
        @DisplayName("getOption() works with different cases")
        void getOptionWorksWithDifferentCases() {
            final JsonValue text = new JsonString("hello");
            final JsonValue number = new JsonNumber(42.0);
            final JsonValue nil = new JsonNull();

            assertThat(stringPrism.getOption(text)).isPresent();
            assertThat(stringPrism.getOption(number)).isEmpty();
            assertThat(stringPrism.getOption(nil)).isEmpty();

            assertThat(numberPrism.getOption(text)).isEmpty();
            assertThat(numberPrism.getOption(number)).isPresent();
            assertThat(numberPrism.getOption(nil)).isEmpty();
        }
    }

    @Nested
    @DisplayName("reverseGet()")
    class ReverseGetOperation {

        @Test
        @DisplayName("reverseGet() constructs source from value")
        void reverseGetConstructsSourceFromValue() {
            final JsonValue result = stringPrism.reverseGet("hello");

            assertThat(result).isEqualTo(new JsonString("hello"));
        }

        @Test
        @DisplayName("reverseGet() creates correct variant type")
        void reverseGetCreatesCorrectVariantType() {
            final JsonValue stringResult = stringPrism.reverseGet("test");
            final JsonValue numberResult = numberPrism.reverseGet(3.14);

            assertThat(stringResult).isInstanceOf(JsonString.class);
            assertThat(numberResult).isInstanceOf(JsonNumber.class);
        }
    }

    @Nested
    @DisplayName("modify()")
    class ModifyOperation {

        @Test
        @DisplayName("modify() transforms value when matching")
        void modifyTransformsValueWhenMatching() {
            final JsonValue text = new JsonString("hello");

            final JsonValue result = stringPrism.modify(text, String::toUpperCase);

            assertThat(result).isEqualTo(new JsonString("HELLO"));
        }

        @Test
        @DisplayName("modify() returns unchanged when not matching")
        void modifyReturnsUnchangedWhenNotMatching() {
            final JsonValue number = new JsonNumber(42.0);

            final JsonValue result = stringPrism.modify(number, String::toUpperCase);

            assertThat(result).isSameAs(number);
        }

        @Test
        @DisplayName("modify() preserves variant type when matching")
        void modifyPreservesVariantTypeWhenMatching() {
            final JsonValue text = new JsonString("hello");

            final JsonValue result = stringPrism.modify(text, s -> s + "!");

            assertThat(result).isInstanceOf(JsonString.class);
            assertThat(((JsonString) result).value()).isEqualTo("hello!");
        }
    }

    @Nested
    @DisplayName("set()")
    class SetOperation {

        @Test
        @DisplayName("set() replaces value when matching")
        void setReplacesValueWhenMatching() {
            final JsonValue text = new JsonString("hello");

            final JsonValue result = stringPrism.set(text, "world");

            assertThat(result).isEqualTo(new JsonString("world"));
        }

        @Test
        @DisplayName("set() returns unchanged when not matching")
        void setReturnsUnchangedWhenNotMatching() {
            final JsonValue number = new JsonNumber(42.0);

            final JsonValue result = stringPrism.set(number, "world");

            assertThat(result).isSameAs(number);
        }
    }

    @Nested
    @DisplayName("compose()")
    class ComposeOperation {

        private final Prism<Result, Result, JsonValue, JsonValue> successPrism = Prism.of(
                "result.success",
                r -> r instanceof Success s ? Optional.of(s.data()) : Optional.empty(),
                Success::new
        );

        @Test
        @DisplayName("compose() creates prism for nested access")
        void composeCreatesPrismForNestedAccess() {
            final Prism<Result, Result, String, String> successStringPrism =
                    successPrism.compose(stringPrism);

            final Result successWithString = new Success(new JsonString("hello"));

            assertThat(successStringPrism.getOption(successWithString)).contains("hello");
        }

        @Test
        @DisplayName("compose() returns empty when outer doesn't match")
        void composeReturnsEmptyWhenOuterDoesntMatch() {
            final Prism<Result, Result, String, String> successStringPrism =
                    successPrism.compose(stringPrism);

            final Result failure = new Failure("error");

            assertThat(successStringPrism.getOption(failure)).isEmpty();
        }

        @Test
        @DisplayName("compose() returns empty when inner doesn't match")
        void composeReturnsEmptyWhenInnerDoesntMatch() {
            final Prism<Result, Result, String, String> successStringPrism =
                    successPrism.compose(stringPrism);

            final Result successWithNumber = new Success(new JsonNumber(42.0));

            assertThat(successStringPrism.getOption(successWithNumber)).isEmpty();
        }

        @Test
        @DisplayName("compose() concatenates ids with dot")
        void composeConcatenatesIds() {
            final Prism<Result, Result, String, String> successStringPrism =
                    successPrism.compose(stringPrism);

            assertThat(successStringPrism.id()).isEqualTo("result.success.json.string");
        }

        @Test
        @DisplayName("compose() reverseGet constructs nested structure")
        void composeReverseGetConstructsNestedStructure() {
            final Prism<Result, Result, String, String> successStringPrism =
                    successPrism.compose(stringPrism);

            final Result result = successStringPrism.reverseGet("hello");

            assertThat(result).isEqualTo(new Success(new JsonString("hello")));
        }

        // Nested sum type for testing
        sealed interface Result permits Success, Failure {
        }

        record Success(JsonValue data) implements Result {
        }

        record Failure(String error) implements Result {
        }
    }

    @Nested
    @DisplayName("Prism Laws")
    class PrismLaws {

        @Test
        @DisplayName("PartialPutGet: getOption(reverseGet(a)) == Optional.of(a)")
        void partialPutGetLaw() {
            final String value = "hello";

            // Constructing and extracting always succeeds with the original value
            assertThat(stringPrism.getOption(stringPrism.reverseGet(value)))
                    .contains(value);
        }

        @Test
        @DisplayName("PartialPutGet holds for different values")
        void partialPutGetLawHoldsForDifferentValues() {
            assertThat(stringPrism.getOption(stringPrism.reverseGet(""))).contains("");
            assertThat(stringPrism.getOption(stringPrism.reverseGet("test"))).contains("test");
            assertThat(numberPrism.getOption(numberPrism.reverseGet(0.0))).contains(0.0);
            assertThat(numberPrism.getOption(numberPrism.reverseGet(42.0))).contains(42.0);
        }

        @Test
        @DisplayName("GetPutPartial: if getOption(s).isPresent() then reverseGet produces equivalent")
        void getPutPartialLaw() {
            final JsonValue text = new JsonString("hello");

            // If extraction succeeds, we can reconstruct the same value
            final Optional<String> extracted = stringPrism.getOption(text);
            assertThat(extracted).isPresent();

            final JsonValue reconstructed = stringPrism.reverseGet(extracted.get());
            assertThat(reconstructed).isEqualTo(text);
        }

        @Test
        @DisplayName("Laws hold for composed prisms")
        void lawsHoldForComposedPrisms() {
            final Prism<Container, Container, JsonValue, JsonValue> wrappedPrism = Prism.of(
                    "container.wrapped",
                    c -> c instanceof Wrapped w ? Optional.of(w.value()) : Optional.empty(),
                    Wrapped::new
            );

            final Prism<Container, Container, String, String> composedPrism =
                    wrappedPrism.compose(stringPrism);

            // PartialPutGet
            final String value = "test";
            assertThat(composedPrism.getOption(composedPrism.reverseGet(value)))
                    .contains(value);

            // GetPutPartial
            final Container wrapped = new Wrapped(new JsonString("hello"));
            final Optional<String> extracted = composedPrism.getOption(wrapped);
            assertThat(extracted).isPresent();
            assertThat(composedPrism.reverseGet(extracted.get())).isEqualTo(wrapped);
        }

        // Moved outside method because sealed interfaces cannot be local
        sealed interface Container permits Wrapped, Empty {
        }

        record Wrapped(JsonValue value) implements Container {
        }

        record Empty() implements Container {
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("works with empty string values")
        void worksWithEmptyStringValues() {
            assertThat(stringPrism.getOption(new JsonString(""))).contains("");
            assertThat(stringPrism.reverseGet("")).isEqualTo(new JsonString(""));
        }

        @Test
        @DisplayName("works with zero numeric values")
        void worksWithZeroNumericValues() {
            assertThat(numberPrism.getOption(new JsonNumber(0.0))).contains(0.0);
            assertThat(numberPrism.reverseGet(0.0)).isEqualTo(new JsonNumber(0.0));
        }

        @Test
        @DisplayName("works with negative numeric values")
        void worksWithNegativeNumericValues() {
            assertThat(numberPrism.getOption(new JsonNumber(-42.5))).contains(-42.5);
            assertThat(numberPrism.reverseGet(-42.5)).isEqualTo(new JsonNumber(-42.5));
        }

        @Test
        @DisplayName("works with unit/singleton variant")
        void worksWithUnitSingletonVariant() {
            // Use a marker record for unit type instead of Void
            record Unit() {
            }
            final Unit UNIT = new Unit();

            final Prism<JsonValue, JsonValue, Unit, Unit> nullPrism = Prism.of(
                    "json.null",
                    json -> json instanceof JsonNull ? Optional.of(UNIT) : Optional.empty(),
                    v -> new JsonNull()
            );

            assertThat(nullPrism.getOption(new JsonNull())).isPresent();
            assertThat(nullPrism.getOption(new JsonString("test"))).isEmpty();
        }

        @Test
        @DisplayName("modify with identity function returns equivalent value")
        void modifyWithIdentityReturnsEquivalent() {
            final JsonValue text = new JsonString("hello");

            final JsonValue result = stringPrism.modify(text, s -> s);

            assertThat(result).isEqualTo(text);
        }
    }

    @Nested
    @DisplayName("Integer Parsing Prism")
    class IntegerParsingPrism {

        private final Prism<String, String, Integer, Integer> intPrism = Prism.of(
                "string.int",
                str -> {
                    try {
                        return Optional.of(Integer.parseInt(str));
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                },
                Object::toString
        );

        @Test
        @DisplayName("parses valid integer strings")
        void parsesValidIntegerStrings() {
            assertThat(intPrism.getOption("42")).contains(42);
            assertThat(intPrism.getOption("-10")).contains(-10);
            assertThat(intPrism.getOption("0")).contains(0);
        }

        @Test
        @DisplayName("returns empty for invalid strings")
        void returnsEmptyForInvalidStrings() {
            assertThat(intPrism.getOption("hello")).isEmpty();
            assertThat(intPrism.getOption("3.14")).isEmpty();
            assertThat(intPrism.getOption("")).isEmpty();
        }

        @Test
        @DisplayName("reverseGet converts integer to string")
        void reverseGetConvertsIntegerToString() {
            assertThat(intPrism.reverseGet(42)).isEqualTo("42");
            assertThat(intPrism.reverseGet(-10)).isEqualTo("-10");
        }

        @Test
        @DisplayName("modify transforms parsed value")
        void modifyTransformsParsedValue() {
            assertThat(intPrism.modify("10", n -> n * 2)).isEqualTo("20");
            assertThat(intPrism.modify("hello", n -> n * 2)).isEqualTo("hello");
        }
    }
}
