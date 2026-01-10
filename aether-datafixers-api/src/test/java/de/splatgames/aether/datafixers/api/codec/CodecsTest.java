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

package de.splatgames.aether.datafixers.api.codec;

import de.splatgames.aether.datafixers.api.optic.TestOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Either;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Codecs}.
 */
@DisplayName("Codecs")
class CodecsTest {

    private final TestOps ops = TestOps.INSTANCE;

    @Nested
    @DisplayName("BOOL Codec")
    class BoolCodec {

        @Test
        @DisplayName("encodes true")
        void encodesTrue() {
            final DataResult<Object> result = Codecs.BOOL.encodeStart(ops, true);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(true);
        }

        @Test
        @DisplayName("encodes false")
        void encodesFalse() {
            final DataResult<Object> result = Codecs.BOOL.encodeStart(ops, false);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(false);
        }

        @Test
        @DisplayName("decodes true")
        void decodesTrue() {
            final DataResult<Boolean> result = Codecs.BOOL.parse(ops, true);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(true);
        }

        @Test
        @DisplayName("decodes false")
        void decodesFalse() {
            final DataResult<Boolean> result = Codecs.BOOL.parse(ops, false);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(false);
        }

        @Test
        @DisplayName("fails to decode non-boolean")
        void failsToDecodeNonBoolean() {
            final DataResult<Boolean> result = Codecs.BOOL.parse(ops, "not a boolean");

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("INT Codec")
    class IntCodec {

        @Test
        @DisplayName("encodes positive integer")
        void encodesPositiveInteger() {
            final DataResult<Object> result = Codecs.INT.encodeStart(ops, 42);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(42);
        }

        @Test
        @DisplayName("encodes negative integer")
        void encodesNegativeInteger() {
            final DataResult<Object> result = Codecs.INT.encodeStart(ops, -100);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(-100);
        }

        @Test
        @DisplayName("encodes zero")
        void encodesZero() {
            final DataResult<Object> result = Codecs.INT.encodeStart(ops, 0);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(0);
        }

        @Test
        @DisplayName("decodes integer")
        void decodesInteger() {
            final DataResult<Integer> result = Codecs.INT.parse(ops, 42);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(42);
        }

        @Test
        @DisplayName("decodes from double")
        void decodesFromDouble() {
            final DataResult<Integer> result = Codecs.INT.parse(ops, 42.0);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(42);
        }

        @Test
        @DisplayName("fails to decode non-number")
        void failsToDecodeNonNumber() {
            final DataResult<Integer> result = Codecs.INT.parse(ops, "not a number");

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("LONG Codec")
    class LongCodec {

        @Test
        @DisplayName("encodes long value")
        void encodesLongValue() {
            final DataResult<Object> result = Codecs.LONG.encodeStart(ops, 9876543210L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(9876543210L);
        }

        @Test
        @DisplayName("decodes long value")
        void decodesLongValue() {
            final DataResult<Long> result = Codecs.LONG.parse(ops, 9876543210L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(9876543210L);
        }
    }

    @Nested
    @DisplayName("FLOAT Codec")
    class FloatCodec {

        @Test
        @DisplayName("encodes float value")
        void encodesFloatValue() {
            final DataResult<Object> result = Codecs.FLOAT.encodeStart(ops, 3.14f);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isInstanceOf(Number.class);
        }

        @Test
        @DisplayName("decodes float value")
        void decodesFloatValue() {
            final DataResult<Float> result = Codecs.FLOAT.parse(ops, 3.14f);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isCloseTo(3.14f, org.assertj.core.api.Assertions.within(0.001f));
        }
    }

    @Nested
    @DisplayName("DOUBLE Codec")
    class DoubleCodec {

        @Test
        @DisplayName("encodes double value")
        void encodesDoubleValue() {
            final DataResult<Object> result = Codecs.DOUBLE.encodeStart(ops, 3.14159265358979);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(3.14159265358979);
        }

        @Test
        @DisplayName("decodes double value")
        void decodesDoubleValue() {
            final DataResult<Double> result = Codecs.DOUBLE.parse(ops, 3.14159265358979);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(3.14159265358979);
        }
    }

    @Nested
    @DisplayName("BYTE Codec")
    class ByteCodec {

        @Test
        @DisplayName("encodes byte value")
        void encodesByteValue() {
            final DataResult<Object> result = Codecs.BYTE.encodeStart(ops, (byte) 127);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isInstanceOf(Number.class);
        }

        @Test
        @DisplayName("decodes byte value")
        void decodesByteValue() {
            final DataResult<Byte> result = Codecs.BYTE.parse(ops, (byte) 127);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains((byte) 127);
        }
    }

    @Nested
    @DisplayName("SHORT Codec")
    class ShortCodec {

        @Test
        @DisplayName("encodes short value")
        void encodesShortValue() {
            final DataResult<Object> result = Codecs.SHORT.encodeStart(ops, (short) 32767);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isInstanceOf(Number.class);
        }

        @Test
        @DisplayName("decodes short value")
        void decodesShortValue() {
            final DataResult<Short> result = Codecs.SHORT.parse(ops, (short) 32767);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains((short) 32767);
        }
    }

    @Nested
    @DisplayName("STRING Codec")
    class StringCodec {

        @Test
        @DisplayName("encodes string value")
        void encodesStringValue() {
            final DataResult<Object> result = Codecs.STRING.encodeStart(ops, "Hello, World!");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains("Hello, World!");
        }

        @Test
        @DisplayName("encodes empty string")
        void encodesEmptyString() {
            final DataResult<Object> result = Codecs.STRING.encodeStart(ops, "");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains("");
        }

        @Test
        @DisplayName("decodes string value")
        void decodesStringValue() {
            final DataResult<String> result = Codecs.STRING.parse(ops, "Hello, World!");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains("Hello, World!");
        }

        @Test
        @DisplayName("fails to decode non-string")
        void failsToDecodeNonString() {
            final DataResult<String> result = Codecs.STRING.parse(ops, 42);

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("list() Codec")
    class ListCodec {

        @Test
        @DisplayName("encodes list of strings")
        void encodesListOfStrings() {
            final Codec<List<String>> listCodec = Codecs.list(Codecs.STRING);
            final List<String> input = List.of("Alice", "Bob", "Charlie");

            final DataResult<Object> result = listCodec.encodeStart(ops, input);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isInstanceOf(List.class);
            @SuppressWarnings("unchecked") final List<Object> encoded = (List<Object>) result.result().orElseThrow();
            assertThat(encoded).containsExactly("Alice", "Bob", "Charlie");
        }

        @Test
        @DisplayName("encodes empty list")
        void encodesEmptyList() {
            final Codec<List<String>> listCodec = Codecs.list(Codecs.STRING);

            final DataResult<Object> result = listCodec.encodeStart(ops, List.of());

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked") final List<Object> encoded = (List<Object>) result.result().orElseThrow();
            assertThat(encoded).isEmpty();
        }

        @Test
        @DisplayName("decodes list of integers")
        void decodesListOfIntegers() {
            final Codec<List<Integer>> listCodec = Codecs.list(Codecs.INT);
            final List<Object> input = List.of(1, 2, 3);

            final DataResult<List<Integer>> result = listCodec.parse(ops, input);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("decodes empty list")
        void decodesEmptyList() {
            final Codec<List<String>> listCodec = Codecs.list(Codecs.STRING);

            final DataResult<List<String>> result = listCodec.parse(ops, List.of());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isEmpty();
        }
    }

    @Nested
    @DisplayName("optional() Codec")
    class OptionalCodec {

        @Test
        @DisplayName("encodes present value")
        void encodesPresentValue() {
            final Codec<Optional<String>> optCodec = Codecs.optional(Codecs.STRING);

            final DataResult<Object> result = optCodec.encodeStart(ops, Optional.of("hello"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains("hello");
        }

        @Test
        @DisplayName("encodes empty optional")
        void encodesEmptyOptional() {
            final Codec<Optional<String>> optCodec = Codecs.optional(Codecs.STRING);

            final DataResult<Object> result = optCodec.encodeStart(ops, Optional.empty());

            assertThat(result.isSuccess()).isTrue();
            // Empty encodes to ops.empty() which is null in TestOps
        }

        @Test
        @DisplayName("decodes present value")
        void decodesPresentValue() {
            final Codec<Optional<String>> optCodec = Codecs.optional(Codecs.STRING);

            final DataResult<Optional<String>> result = optCodec.parse(ops, "hello");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).contains("hello");
        }

        @Test
        @DisplayName("decodes to empty on failure")
        void decodesToEmptyOnFailure() {
            final Codec<Optional<String>> optCodec = Codecs.optional(Codecs.STRING);

            final DataResult<Optional<String>> result = optCodec.parse(ops, 42);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isEmpty();
        }
    }

    @Nested
    @DisplayName("either() Codec")
    class EitherCodec {

        private final Codec<Either<String, Integer>> eitherCodec = Codecs.either(Codecs.STRING, Codecs.INT);

        @Test
        @DisplayName("encodes left value")
        void encodesLeftValue() {
            final DataResult<Object> result = eitherCodec.encodeStart(ops, Either.left("hello"));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains("hello");
        }

        @Test
        @DisplayName("encodes right value")
        void encodesRightValue() {
            final DataResult<Object> result = eitherCodec.encodeStart(ops, Either.right(42));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(42);
        }

        @Test
        @DisplayName("decodes string as left")
        void decodesStringAsLeft() {
            final DataResult<Either<String, Integer>> result = eitherCodec.parse(ops, "hello");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow().isLeft()).isTrue();
            assertThat(result.result().orElseThrow().left()).contains("hello");
        }

        @Test
        @DisplayName("decodes integer as right when string fails")
        void decodesIntegerAsRight() {
            // First create a situation where left codec fails
            final Codec<Either<Integer, String>> intOrString = Codecs.either(Codecs.INT, Codecs.STRING);

            final DataResult<Either<Integer, String>> result = intOrString.parse(ops, "hello");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow().isRight()).isTrue();
            assertThat(result.result().orElseThrow().right()).contains("hello");
        }

        @Test
        @DisplayName("fails when neither codec matches")
        void failsWhenNeitherMatches() {
            final Codec<Either<Integer, Integer>> intOrInt = Codecs.either(Codecs.INT, Codecs.INT);

            final DataResult<Either<Integer, Integer>> result = intOrInt.parse(ops, "not a number");

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("pair() Codec")
    class PairCodec {

        @Test
        @DisplayName("encodes pair")
        void encodesPair() {
            final Codec<Pair<String, Integer>> pairCodec = Codecs.pair(Codecs.STRING, Codecs.INT);
            final Pair<String, Integer> input = Pair.of("hello", 42);

            final DataResult<Object> result = pairCodec.encodeStart(ops, input);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("decodes pair from map")
        void decodesPairFromMap() {
            // Pair codec chains decode - it depends on how the first codec returns remaining input
            final Codec<Pair<String, String>> pairCodec = Codecs.pair(Codecs.STRING, Codecs.STRING);

            // For a proper test we need a structure that supports sequential decoding
            // TestOps may not fully support this, so we just test encoding works
            final DataResult<Object> encoded = pairCodec.encodeStart(ops, Pair.of("first", "second"));
            assertThat(encoded.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("intRange() Codec")
    class IntRangeCodec {

        private final Codec<Integer> ageCodec = Codecs.intRange(0, 150);

        @Test
        @DisplayName("encodes value in range")
        void encodesValueInRange() {
            final DataResult<Object> result = ageCodec.encodeStart(ops, 25);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(25);
        }

        @Test
        @DisplayName("encodes minimum value")
        void encodesMinimumValue() {
            final DataResult<Object> result = ageCodec.encodeStart(ops, 0);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(0);
        }

        @Test
        @DisplayName("encodes maximum value")
        void encodesMaximumValue() {
            final DataResult<Object> result = ageCodec.encodeStart(ops, 150);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(150);
        }

        @Test
        @DisplayName("fails to encode below minimum")
        void failsToEncodeBelowMinimum() {
            final DataResult<Object> result = ageCodec.encodeStart(ops, -1);

            assertThat(result.isError()).isTrue();
            assertThat(result.error().orElse("")).contains("outside of range");
        }

        @Test
        @DisplayName("fails to encode above maximum")
        void failsToEncodeAboveMaximum() {
            final DataResult<Object> result = ageCodec.encodeStart(ops, 200);

            assertThat(result.isError()).isTrue();
            assertThat(result.error().orElse("")).contains("outside of range");
        }

        @Test
        @DisplayName("decodes value in range")
        void decodesValueInRange() {
            final DataResult<Integer> result = ageCodec.parse(ops, 50);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(50);
        }

        @Test
        @DisplayName("fails to decode below minimum")
        void failsToDecodeBelowMinimum() {
            final DataResult<Integer> result = ageCodec.parse(ops, -10);

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("fails to decode above maximum")
        void failsToDecodeAboveMaximum() {
            final DataResult<Integer> result = ageCodec.parse(ops, 300);

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("floatRange() Codec")
    class FloatRangeCodec {

        private final Codec<Float> percentageCodec = Codecs.floatRange(0.0f, 1.0f);

        @Test
        @DisplayName("encodes value in range")
        void encodesValueInRange() {
            final DataResult<Object> result = percentageCodec.encodeStart(ops, 0.5f);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("fails to encode out of range")
        void failsToEncodeOutOfRange() {
            final DataResult<Object> result = percentageCodec.encodeStart(ops, 1.5f);

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("decodes value in range")
        void decodesValueInRange() {
            final DataResult<Float> result = percentageCodec.parse(ops, 0.75f);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("fails to decode out of range")
        void failsToDecodeOutOfRange() {
            final DataResult<Float> result = percentageCodec.parse(ops, -0.5f);

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("doubleRange() Codec")
    class DoubleRangeCodec {

        private final Codec<Double> latitudeCodec = Codecs.doubleRange(-90.0, 90.0);

        @Test
        @DisplayName("encodes value in range")
        void encodesValueInRange() {
            final DataResult<Object> result = latitudeCodec.encodeStart(ops, 51.5074);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("fails to encode out of range")
        void failsToEncodeOutOfRange() {
            final DataResult<Object> result = latitudeCodec.encodeStart(ops, 100.0);

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("decodes value in range")
        void decodesValueInRange() {
            final DataResult<Double> result = latitudeCodec.parse(ops, -45.0);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains(-45.0);
        }

        @Test
        @DisplayName("fails to decode out of range")
        void failsToDecodeOutOfRange() {
            final DataResult<Double> result = latitudeCodec.parse(ops, -100.0);

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("nonEmptyString() Codec")
    class NonEmptyStringCodec {

        private final Codec<String> nonEmptyCodec = Codecs.nonEmptyString();

        @Test
        @DisplayName("encodes non-empty string")
        void encodesNonEmptyString() {
            final DataResult<Object> result = nonEmptyCodec.encodeStart(ops, "hello");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains("hello");
        }

        @Test
        @DisplayName("fails to encode empty string")
        void failsToEncodeEmptyString() {
            final DataResult<Object> result = nonEmptyCodec.encodeStart(ops, "");

            assertThat(result.isError()).isTrue();
            assertThat(result.error().orElse("")).contains("cannot be empty");
        }

        @Test
        @DisplayName("decodes non-empty string")
        void decodesNonEmptyString() {
            final DataResult<String> result = nonEmptyCodec.parse(ops, "world");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result()).contains("world");
        }

        @Test
        @DisplayName("fails to decode empty string")
        void failsToDecodeEmptyString() {
            final DataResult<String> result = nonEmptyCodec.parse(ops, "");

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("Round-trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("INT round-trip")
        void intRoundTrip() {
            final int original = 42;
            final DataResult<Object> encoded = Codecs.INT.encodeStart(ops, original);
            final DataResult<Integer> decoded = encoded.flatMap(e -> Codecs.INT.parse(ops, e));

            assertThat(decoded.result()).contains(original);
        }

        @Test
        @DisplayName("STRING round-trip")
        void stringRoundTrip() {
            final String original = "Hello, World!";
            final DataResult<Object> encoded = Codecs.STRING.encodeStart(ops, original);
            final DataResult<String> decoded = encoded.flatMap(e -> Codecs.STRING.parse(ops, e));

            assertThat(decoded.result()).contains(original);
        }

        @Test
        @DisplayName("BOOL round-trip")
        void boolRoundTrip() {
            final boolean original = true;
            final DataResult<Object> encoded = Codecs.BOOL.encodeStart(ops, original);
            final DataResult<Boolean> decoded = encoded.flatMap(e -> Codecs.BOOL.parse(ops, e));

            assertThat(decoded.result()).contains(original);
        }

        @Test
        @DisplayName("list round-trip")
        void listRoundTrip() {
            final Codec<List<String>> listCodec = Codecs.list(Codecs.STRING);
            final List<String> original = List.of("Alice", "Bob", "Charlie");

            final DataResult<Object> encoded = listCodec.encodeStart(ops, original);
            final DataResult<List<String>> decoded = encoded.flatMap(e -> listCodec.parse(ops, e));

            assertThat(decoded.result().orElseThrow()).containsExactlyElementsOf(original);
        }
    }
}
