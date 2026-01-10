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

package de.splatgames.aether.datafixers.api.result;

import de.splatgames.aether.datafixers.api.util.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DataResult}.
 */
@DisplayName("DataResult")
class DataResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("success() creates successful result")
        void successCreatesSuccessfulResult() {
            DataResult<String> result = DataResult.success("hello");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isError()).isFalse();
            assertThat(result.result()).contains("hello");
            assertThat(result.error()).isEmpty();
            assertThat(result.partialResult()).isEmpty();
        }

        @Test
        @DisplayName("success() rejects null value")
        void successRejectsNull() {
            assertThatThrownBy(() -> DataResult.success(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("error() creates error result without partial")
        void errorCreatesErrorResult() {
            DataResult<String> result = DataResult.error("something went wrong");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isError()).isTrue();
            assertThat(result.result()).isEmpty();
            assertThat(result.error()).contains("something went wrong");
            assertThat(result.partialResult()).isEmpty();
        }

        @Test
        @DisplayName("error() with partial creates error with partial result")
        void errorWithPartialCreatesErrorWithPartial() {
            DataResult<Integer> result = DataResult.error("partial data", 42);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isError()).isTrue();
            assertThat(result.result()).isEmpty();
            assertThat(result.error()).contains("partial data");
            assertThat(result.partialResult()).contains(42);
        }

        @Test
        @DisplayName("error() with supplier evaluates lazily")
        void errorWithSupplier() {
            AtomicBoolean called = new AtomicBoolean(false);
            DataResult<String> result = DataResult.error(() -> {
                called.set(true);
                return "lazy error";
            });

            assertThat(called.get()).isTrue();
            assertThat(result.error()).contains("lazy error");
        }
    }

    @Nested
    @DisplayName("Transformation Operations")
    class TransformationOperations {

        @Test
        @DisplayName("map() transforms success value")
        void mapTransformsSuccess() {
            DataResult<Integer> result = DataResult.success(10);
            DataResult<String> mapped = result.map(n -> "Value: " + n);

            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.result()).contains("Value: 10");
        }

        @Test
        @DisplayName("map() preserves error without partial")
        void mapPreservesError() {
            DataResult<Integer> result = DataResult.error("not found");
            DataResult<String> mapped = result.map(n -> "Value: " + n);

            assertThat(mapped.isError()).isTrue();
            assertThat(mapped.error()).contains("not found");
        }

        @Test
        @DisplayName("map() transforms error partial result")
        void mapTransformsPartial() {
            DataResult<Integer> result = DataResult.error("warning", 5);
            DataResult<String> mapped = result.map(n -> "Value: " + n);

            assertThat(mapped.isError()).isTrue();
            assertThat(mapped.error()).contains("warning");
            assertThat(mapped.partialResult()).contains("Value: 5");
        }

        @Test
        @DisplayName("flatMap() chains success operations")
        void flatMapChainsSuccess() {
            DataResult<String> result = DataResult.success("42");
            DataResult<Integer> parsed = result.flatMap(s -> {
                try {
                    return DataResult.success(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    return DataResult.error("Invalid number: " + s);
                }
            });

            assertThat(parsed.isSuccess()).isTrue();
            assertThat(parsed.result()).contains(42);
        }

        @Test
        @DisplayName("flatMap() handles chain failure")
        void flatMapHandlesChainFailure() {
            DataResult<String> result = DataResult.success("not-a-number");
            DataResult<Integer> parsed = result.flatMap(s -> {
                try {
                    return DataResult.success(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    return DataResult.error("Invalid number: " + s);
                }
            });

            assertThat(parsed.isError()).isTrue();
            assertThat(parsed.error()).contains("Invalid number: not-a-number");
        }

        @Test
        @DisplayName("flatMap() preserves original error")
        void flatMapPreservesOriginalError() {
            DataResult<String> result = DataResult.error("original error");
            DataResult<Integer> mapped = result.flatMap(s -> DataResult.success(s.length()));

            assertThat(mapped.isError()).isTrue();
            assertThat(mapped.error()).contains("original error");
        }

        @Test
        @DisplayName("mapError() transforms error message")
        void mapErrorTransformsMessage() {
            DataResult<String> result = DataResult.error("not found");
            DataResult<String> enhanced = result.mapError(msg -> "Error: " + msg);

            assertThat(enhanced.error()).contains("Error: not found");
        }

        @Test
        @DisplayName("mapError() leaves success unchanged")
        void mapErrorLeavesSuccessUnchanged() {
            DataResult<String> result = DataResult.success("hello");
            DataResult<String> same = result.mapError(msg -> "Error: " + msg);

            assertThat(same.isSuccess()).isTrue();
            assertThat(same.result()).contains("hello");
        }
    }

    @Nested
    @DisplayName("Value Extraction")
    class ValueExtraction {

        @Test
        @DisplayName("getOrThrow() returns value on success")
        void getOrThrowReturnsValueOnSuccess() {
            DataResult<String> result = DataResult.success("hello");
            String value = result.getOrThrow(IllegalStateException::new);

            assertThat(value).isEqualTo("hello");
        }

        @Test
        @DisplayName("getOrThrow() throws on error")
        void getOrThrowThrowsOnError() {
            DataResult<String> result = DataResult.error("failed");

            assertThatThrownBy(() -> result.getOrThrow(IllegalStateException::new))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("failed");
        }

        @Test
        @DisplayName("orElse() returns value on success")
        void orElseReturnsValueOnSuccess() {
            DataResult<Integer> result = DataResult.success(42);
            Integer value = result.orElse(0);

            assertThat(value).isEqualTo(42);
        }

        @Test
        @DisplayName("orElse() returns default on error")
        void orElseReturnsDefaultOnError() {
            DataResult<Integer> result = DataResult.error("not found");
            Integer value = result.orElse(0);

            assertThat(value).isEqualTo(0);
        }

        @Test
        @DisplayName("orElseGet() returns value on success without calling supplier")
        void orElseGetReturnsValueOnSuccess() {
            AtomicBoolean called = new AtomicBoolean(false);
            DataResult<Integer> result = DataResult.success(42);
            Integer value = result.orElseGet(() -> {
                called.set(true);
                return 0;
            });

            assertThat(value).isEqualTo(42);
            assertThat(called.get()).isFalse();
        }

        @Test
        @DisplayName("orElseGet() calls supplier on error")
        void orElseGetCallsSupplierOnError() {
            DataResult<Integer> result = DataResult.error("not found");
            Integer value = result.orElseGet(() -> 99);

            assertThat(value).isEqualTo(99);
        }

        @Test
        @DisplayName("resultOrPartial() returns value on success")
        void resultOrPartialReturnsValueOnSuccess() {
            DataResult<String> result = DataResult.success("hello");
            AtomicBoolean called = new AtomicBoolean(false);

            String value = result.resultOrPartial(err -> called.set(true));

            assertThat(value).isEqualTo("hello");
            assertThat(called.get()).isFalse();
        }

        @Test
        @DisplayName("resultOrPartial() returns partial and logs error")
        void resultOrPartialReturnsPartialAndLogsError() {
            DataResult<Integer> result = DataResult.error("using default", 42);
            AtomicReference<String> logged = new AtomicReference<>();

            Integer value = result.resultOrPartial(logged::set);

            assertThat(value).isEqualTo(42);
            assertThat(logged.get()).isEqualTo("using default");
        }

        @Test
        @DisplayName("resultOrPartial() throws when no partial available")
        void resultOrPartialThrowsWhenNoPartial() {
            DataResult<String> result = DataResult.error("no data");

            assertThatThrownBy(() -> result.resultOrPartial(err -> {
            }))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no data");
        }
    }

    @Nested
    @DisplayName("Side Effects")
    class SideEffects {

        @Test
        @DisplayName("ifSuccess() executes consumer on success")
        void ifSuccessExecutesOnSuccess() {
            AtomicReference<String> captured = new AtomicReference<>();
            DataResult<String> result = DataResult.success("hello");

            DataResult<String> same = result.ifSuccess(captured::set);

            assertThat(captured.get()).isEqualTo("hello");
            assertThat(same).isSameAs(result);
        }

        @Test
        @DisplayName("ifSuccess() does not execute on error")
        void ifSuccessDoesNotExecuteOnError() {
            AtomicBoolean called = new AtomicBoolean(false);
            DataResult<String> result = DataResult.error("failed");

            result.ifSuccess(v -> called.set(true));

            assertThat(called.get()).isFalse();
        }

        @Test
        @DisplayName("ifError() executes consumer on error")
        void ifErrorExecutesOnError() {
            AtomicReference<String> captured = new AtomicReference<>();
            DataResult<String> result = DataResult.error("failed");

            DataResult<String> same = result.ifError(captured::set);

            assertThat(captured.get()).isEqualTo("failed");
            assertThat(same).isSameAs(result);
        }

        @Test
        @DisplayName("ifError() does not execute on success")
        void ifErrorDoesNotExecuteOnSuccess() {
            AtomicBoolean called = new AtomicBoolean(false);
            DataResult<String> result = DataResult.success("hello");

            result.ifError(e -> called.set(true));

            assertThat(called.get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Combining Results")
    class CombiningResults {

        @Test
        @DisplayName("apply2() combines two successes")
        void apply2CombinesTwoSuccesses() {
            DataResult<String> name = DataResult.success("Alice");
            DataResult<Integer> age = DataResult.success(30);

            DataResult<String> combined = name.apply2(age, (n, a) -> n + " is " + a);

            assertThat(combined.isSuccess()).isTrue();
            assertThat(combined.result()).contains("Alice is 30");
        }

        @Test
        @DisplayName("apply2() propagates first error")
        void apply2PropagatesFirstError() {
            DataResult<String> name = DataResult.error("name missing");
            DataResult<Integer> age = DataResult.success(30);

            DataResult<String> combined = name.apply2(age, (n, a) -> n + " is " + a);

            assertThat(combined.isError()).isTrue();
            assertThat(combined.error()).contains("name missing");
        }

        @Test
        @DisplayName("apply2() propagates second error")
        void apply2PropagatesSecondError() {
            DataResult<String> name = DataResult.success("Alice");
            DataResult<Integer> age = DataResult.error("age missing");

            DataResult<String> combined = name.apply2(age, (n, a) -> n + " is " + a);

            assertThat(combined.isError()).isTrue();
            assertThat(combined.error()).contains("age missing");
        }
    }

    @Nested
    @DisplayName("Conversion")
    class Conversion {

        @Test
        @DisplayName("toEither() returns Right on success")
        void toEitherReturnsRightOnSuccess() {
            DataResult<Integer> result = DataResult.success(42);
            Either<String, Integer> either = result.toEither();

            assertThat(either.isRight()).isTrue();
            assertThat(either.right()).contains(42);
        }

        @Test
        @DisplayName("toEither() returns Left on error")
        void toEitherReturnsLeftOnError() {
            DataResult<Integer> result = DataResult.error("failed");
            Either<String, Integer> either = result.toEither();

            assertThat(either.isLeft()).isTrue();
            assertThat(either.left()).contains("failed");
        }
    }

    @Nested
    @DisplayName("Partial Result Promotion")
    class PartialResultPromotion {

        @Test
        @DisplayName("promotePartial() promotes partial to success")
        void promotePartialPromotesPartialToSuccess() {
            DataResult<Integer> result = DataResult.error("using default", 42);
            AtomicReference<String> logged = new AtomicReference<>();

            DataResult<Integer> promoted = result.promotePartial(logged::set);

            assertThat(promoted.isSuccess()).isTrue();
            assertThat(promoted.result()).contains(42);
            assertThat(logged.get()).isEqualTo("using default");
        }

        @Test
        @DisplayName("promotePartial() leaves success unchanged")
        void promotePartialLeavesSuccessUnchanged() {
            DataResult<Integer> result = DataResult.success(42);
            AtomicBoolean called = new AtomicBoolean(false);

            DataResult<Integer> same = result.promotePartial(e -> called.set(true));

            assertThat(same).isSameAs(result);
            assertThat(called.get()).isFalse();
        }

        @Test
        @DisplayName("promotePartial() leaves error without partial unchanged")
        void promotePartialLeavesErrorWithoutPartialUnchanged() {
            DataResult<Integer> result = DataResult.error("no data");
            AtomicBoolean called = new AtomicBoolean(false);

            DataResult<Integer> same = result.promotePartial(e -> called.set(true));

            assertThat(same).isSameAs(result);
            assertThat(called.get()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality and Hashing")
    class EqualityAndHashing {

        @Test
        @DisplayName("equal successes are equal")
        void equalSuccessesAreEqual() {
            DataResult<String> result1 = DataResult.success("hello");
            DataResult<String> result2 = DataResult.success("hello");

            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("different successes are not equal")
        void differentSuccessesAreNotEqual() {
            DataResult<String> result1 = DataResult.success("hello");
            DataResult<String> result2 = DataResult.success("world");

            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("equal errors are equal")
        void equalErrorsAreEqual() {
            DataResult<String> result1 = DataResult.error("failed");
            DataResult<String> result2 = DataResult.error("failed");

            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("success and error are not equal")
        void successAndErrorAreNotEqual() {
            DataResult<String> success = DataResult.success("hello");
            DataResult<String> error = DataResult.error("hello");

            assertThat(success).isNotEqualTo(error);
        }
    }

    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {

        @Test
        @DisplayName("success toString() contains value")
        void successToStringContainsValue() {
            DataResult<String> result = DataResult.success("hello");

            assertThat(result.toString()).contains("Success").contains("hello");
        }

        @Test
        @DisplayName("error toString() contains message")
        void errorToStringContainsMessage() {
            DataResult<String> result = DataResult.error("failed");

            assertThat(result.toString()).contains("Error").contains("failed");
        }

        @Test
        @DisplayName("error with partial toString() contains both")
        void errorWithPartialToStringContainsBoth() {
            DataResult<Integer> result = DataResult.error("partial", 42);

            assertThat(result.toString())
                    .contains("Error")
                    .contains("partial")
                    .contains("42");
        }
    }
}
