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

import de.splatgames.aether.datafixers.api.result.DataResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DataResultAssert")
class DataResultAssertTest {

    @Nested
    @DisplayName("Status Assertions")
    class StatusAssertions {

        @Test
        @DisplayName("isSuccess passes for success result")
        void isSuccessPassesForSuccessResult() {
            final DataResult<String> result = DataResult.success("value");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(assertion::isSuccess).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isSuccess fails for error result")
        void isSuccessFailsForErrorResult() {
            final DataResult<String> result = DataResult.error("error message");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(assertion::isSuccess)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected success")
                    .hasMessageContaining("error message");
        }

        @Test
        @DisplayName("isError passes for error result")
        void isErrorPassesForErrorResult() {
            final DataResult<String> result = DataResult.error("error message");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(assertion::isError).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isError fails for success result")
        void isErrorFailsForSuccessResult() {
            final DataResult<String> result = DataResult.success("value");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(assertion::isError)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected error")
                    .hasMessageContaining("value");
        }

        @Test
        @DisplayName("hasPartialResult passes when partial exists")
        void hasPartialResultPassesWhenPartialExists() {
            final DataResult<String> result = DataResult.error("error", "partial");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(assertion::hasPartialResult).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasPartialResult fails when no partial")
        void hasPartialResultFailsWhenNoPartial() {
            final DataResult<String> result = DataResult.error("error");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(assertion::hasPartialResult)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected partial result");
        }

        @Test
        @DisplayName("hasNoPartialResult passes when no partial")
        void hasNoPartialResultPassesWhenNoPartial() {
            final DataResult<String> result = DataResult.error("error");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(assertion::hasNoPartialResult).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasNoPartialResult fails when partial exists")
        void hasNoPartialResultFailsWhenPartialExists() {
            final DataResult<String> result = DataResult.error("error", "partial");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(assertion::hasNoPartialResult)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected no partial result");
        }
    }

    @Nested
    @DisplayName("Value Assertions")
    class ValueAssertions {

        @Test
        @DisplayName("hasValue passes for matching value")
        void hasValuePassesForMatchingValue() {
            final DataResult<String> result = DataResult.success("expected");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.hasValue("expected"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasValue fails for non-matching value")
        void hasValueFailsForNonMatchingValue() {
            final DataResult<String> result = DataResult.success("actual");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(() -> assertion.hasValue("expected"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("expected")
                    .hasMessageContaining("actual");
        }

        @Test
        @DisplayName("hasValueSatisfying passes when condition met")
        void hasValueSatisfyingPassesWhenConditionMet() {
            final DataResult<Integer> result = DataResult.success(42);
            final DataResultAssert<Integer> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.hasValueSatisfying(v ->
                    assertThat(v).isGreaterThan(0)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasPartialValue passes for matching partial")
        void hasPartialValuePassesForMatchingPartial() {
            final DataResult<String> result = DataResult.error("error", "partial");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.hasPartialValue("partial"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasPartialValue fails for non-matching partial")
        void hasPartialValueFailsForNonMatchingPartial() {
            final DataResult<String> result = DataResult.error("error", "actual");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(() -> assertion.hasPartialValue("expected"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("expected")
                    .hasMessageContaining("actual");
        }
    }

    @Nested
    @DisplayName("Error Message Assertions")
    class ErrorMessageAssertions {

        @Test
        @DisplayName("hasErrorMessage passes for exact match")
        void hasErrorMessagePassesForExactMatch() {
            final DataResult<String> result = DataResult.error("exact error");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.hasErrorMessage("exact error"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasErrorMessage fails for mismatch")
        void hasErrorMessageFailsForMismatch() {
            final DataResult<String> result = DataResult.error("actual error");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(() -> assertion.hasErrorMessage("expected error"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("expected error")
                    .hasMessageContaining("actual error");
        }

        @Test
        @DisplayName("hasErrorMessageContaining passes when substring found")
        void hasErrorMessageContainingPassesWhenSubstringFound() {
            final DataResult<String> result = DataResult.error("full error message");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.hasErrorMessageContaining("error"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasErrorMessageContaining fails when substring not found")
        void hasErrorMessageContainingFailsWhenSubstringNotFound() {
            final DataResult<String> result = DataResult.error("full message");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(() -> assertion.hasErrorMessageContaining("error"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("contain")
                    .hasMessageContaining("error");
        }

        @Test
        @DisplayName("hasErrorMessageStartingWith passes for matching prefix")
        void hasErrorMessageStartingWithPassesForMatchingPrefix() {
            final DataResult<String> result = DataResult.error("Error: something happened");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.hasErrorMessageStartingWith("Error:"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasErrorMessageStartingWith fails for non-matching prefix")
        void hasErrorMessageStartingWithFailsForNonMatchingPrefix() {
            final DataResult<String> result = DataResult.error("Something happened");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(() -> assertion.hasErrorMessageStartingWith("Error:"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("start with");
        }

        @Test
        @DisplayName("hasErrorMessageEndingWith passes for matching suffix")
        void hasErrorMessageEndingWithPassesForMatchingSuffix() {
            final DataResult<String> result = DataResult.error("Field 'name' is required");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.hasErrorMessageEndingWith("required"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasErrorMessageEndingWith fails for non-matching suffix")
        void hasErrorMessageEndingWithFailsForNonMatchingSuffix() {
            final DataResult<String> result = DataResult.error("Field is missing");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(() -> assertion.hasErrorMessageEndingWith("required"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("end with");
        }

        @Test
        @DisplayName("hasErrorMessageMatching passes for matching regex")
        void hasErrorMessageMatchingPassesForMatchingRegex() {
            final DataResult<String> result = DataResult.error("Error code 404");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.hasErrorMessageMatching(".*\\d{3}.*"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasErrorMessageMatching fails for non-matching regex")
        void hasErrorMessageMatchingFailsForNonMatchingRegex() {
            final DataResult<String> result = DataResult.error("Error occurred");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatThrownBy(() -> assertion.hasErrorMessageMatching(".*\\d{3}.*"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("match pattern");
        }
    }

    @Nested
    @DisplayName("Extraction Assertions")
    class ExtractionAssertions {

        @Test
        @DisplayName("extractingValue allows further assertions")
        void extractingValueAllowsFurtherAssertions() {
            final DataResult<String> result = DataResult.success("test value");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.extractingValue()
                    .isEqualTo("test value"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("extractingError allows further assertions")
        void extractingErrorAllowsFurtherAssertions() {
            final DataResult<String> result = DataResult.error("test error");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.extractingError()
                    .contains("test"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("extractingPartial allows further assertions")
        void extractingPartialAllowsFurtherAssertions() {
            final DataResult<String> result = DataResult.error("error", "partial value");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.extractingPartial()
                    .isEqualTo("partial value"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Chaining")
    class Chaining {

        @Test
        @DisplayName("assertions can be chained")
        void assertionsCanBeChained() {
            final DataResult<String> result = DataResult.success("value");
            final DataResultAssert<String> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion
                    .isSuccess()
                    .hasValue("value")
                    .hasNoPartialResult())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("satisfies allows custom validation")
        void satisfiesAllowsCustomValidation() {
            final DataResult<Integer> result = DataResult.success(42);
            final DataResultAssert<Integer> assertion = new DataResultAssert<>(result);

            assertThatCode(() -> assertion.satisfies(r -> {
                assertThat(r.isSuccess()).isTrue();
                assertThat(r.result().orElse(0)).isEqualTo(42);
            })).doesNotThrowAnyException();
        }
    }
}
