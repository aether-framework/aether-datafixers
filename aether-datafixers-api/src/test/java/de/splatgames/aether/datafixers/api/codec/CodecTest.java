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

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.optic.TestOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Codec} interface methods.
 */
@DisplayName("Codec")
class CodecTest {

    private final TestOps ops = TestOps.INSTANCE;

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("of() creates codec from encoder and decoder")
        void ofCreatesCodecFromEncoderAndDecoder() {
            final Encoder<String> encoder = new Encoder<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final String input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T prefix) {
                    @SuppressWarnings("unchecked")
                    final T result = (T) ops.createString(input.toUpperCase());
                    return DataResult.success(result);
                }
            };
            final Decoder<String> decoder = new Decoder<>() {
                @NotNull
                @Override
                public <T> DataResult<Pair<String, T>> decode(@NotNull final DynamicOps<T> ops,
                                                               @NotNull final T input) {
                    return ops.getStringValue(input).map(s -> Pair.of(s.toLowerCase(), ops.empty()));
                }
            };

            final Codec<String> codec = Codec.of(encoder, decoder);

            final DataResult<Object> encoded = codec.encodeStart(ops, "hello");
            assertThat(encoded.result()).contains("HELLO");

            final DataResult<String> decoded = codec.parse(ops, "WORLD");
            assertThat(decoded.result()).contains("world");
        }

        @Test
        @DisplayName("unit() creates constant codec")
        void unitCreatesConstantCodec() {
            final Codec<String> unitCodec = Codec.unit("default");

            // Decoding always returns the constant
            final DataResult<String> decoded = unitCodec.parse(ops, "anything");
            assertThat(decoded.result()).contains("default");

            // Encoding returns the prefix unchanged
            final DataResult<Object> encoded = unitCodec.encodeStart(ops, "ignored");
            assertThat(encoded.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("xmap()")
    class XmapOperation {

        @Test
        @DisplayName("xmap() transforms decoded values")
        void xmapTransformsDecodedValues() {
            final Codec<Integer> intFromString = Codecs.STRING.xmap(
                    Integer::parseInt,
                    Object::toString
            );

            final DataResult<Integer> result = intFromString.parse(ops, "42");

            assertThat(result.result()).contains(42);
        }

        @Test
        @DisplayName("xmap() transforms encoded values")
        void xmapTransformsEncodedValues() {
            final Codec<Integer> intFromString = Codecs.STRING.xmap(
                    Integer::parseInt,
                    Object::toString
            );

            final DataResult<Object> result = intFromString.encodeStart(ops, 42);

            assertThat(result.result()).contains("42");
        }

        @Test
        @DisplayName("xmap() works with wrapper types")
        void xmapWorksWithWrapperTypes() {
            record UserId(String value) {}

            final Codec<UserId> userIdCodec = Codecs.STRING.xmap(
                    UserId::new,
                    UserId::value
            );

            final DataResult<UserId> decoded = userIdCodec.parse(ops, "user-123");
            assertThat(decoded.result().orElseThrow().value()).isEqualTo("user-123");

            final DataResult<Object> encoded = userIdCodec.encodeStart(ops, new UserId("user-456"));
            assertThat(encoded.result()).contains("user-456");
        }
    }

    @Nested
    @DisplayName("flatXmap()")
    class FlatXmapOperation {

        @Test
        @DisplayName("flatXmap() allows validation during decode")
        void flatXmapAllowsValidationDuringDecode() {
            final Codec<Integer> positiveInt = Codecs.INT.flatXmap(
                    value -> value > 0
                            ? DataResult.success(value)
                            : DataResult.error("Must be positive: " + value),
                    DataResult::success
            );

            final DataResult<Integer> valid = positiveInt.parse(ops, 5);
            assertThat(valid.result()).contains(5);

            final DataResult<Integer> invalid = positiveInt.parse(ops, -5);
            assertThat(invalid.isError()).isTrue();
        }

        @Test
        @DisplayName("flatXmap() allows validation during encode")
        void flatXmapAllowsValidationDuringEncode() {
            final Codec<Integer> boundedInt = Codecs.INT.flatXmap(
                    DataResult::success,
                    value -> value >= 0 && value <= 100
                            ? DataResult.success(value)
                            : DataResult.error("Must be 0-100: " + value)
            );

            final DataResult<Object> valid = boundedInt.encodeStart(ops, 50);
            assertThat(valid.isSuccess()).isTrue();

            final DataResult<Object> invalid = boundedInt.encodeStart(ops, 200);
            assertThat(invalid.isError()).isTrue();
        }

        @Test
        @DisplayName("flatXmap() can fail on both encode and decode")
        void flatXmapCanFailOnBothEncodeAndDecode() {
            final Codec<String> nonEmptyNonNull = Codecs.STRING.flatXmap(
                    s -> s.isEmpty() ? DataResult.error("Cannot be empty") : DataResult.success(s),
                    s -> s.isEmpty() ? DataResult.error("Cannot be empty") : DataResult.success(s)
            );

            assertThat(nonEmptyNonNull.parse(ops, "").isError()).isTrue();
            assertThat(nonEmptyNonNull.encodeStart(ops, "").isError()).isTrue();
            assertThat(nonEmptyNonNull.parse(ops, "valid").isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("comapFlatMap()")
    class ComapFlatMapOperation {

        @Test
        @DisplayName("comapFlatMap() allows failable decode with infallible encode")
        void comapFlatMapAllowsFailableDecodeWithInfallibleEncode() {
            final Codec<Integer> positiveFromString = Codecs.STRING.comapFlatMap(
                    str -> {
                        try {
                            int value = Integer.parseInt(str);
                            return value > 0
                                    ? DataResult.success(value)
                                    : DataResult.error("Not positive");
                        } catch (NumberFormatException e) {
                            return DataResult.error("Invalid number: " + str);
                        }
                    },
                    Object::toString
            );

            assertThat(positiveFromString.parse(ops, "42").result()).contains(42);
            assertThat(positiveFromString.parse(ops, "-5").isError()).isTrue();
            assertThat(positiveFromString.parse(ops, "abc").isError()).isTrue();
            assertThat(positiveFromString.encodeStart(ops, 100).result()).contains("100");
        }
    }

    @Nested
    @DisplayName("flatComapMap()")
    class FlatComapMapOperation {

        @Test
        @DisplayName("flatComapMap() allows infallible decode with failable encode")
        void flatComapMapAllowsInfallibleDecodeWithFailableEncode() {
            final Codec<String> uppercaseOnly = Codecs.STRING.flatComapMap(
                    String::toUpperCase, // decode always succeeds
                    str -> str.equals(str.toUpperCase())
                            ? DataResult.success(str)
                            : DataResult.error("Must be uppercase")
            );

            assertThat(uppercaseOnly.parse(ops, "hello").result()).contains("HELLO");
            assertThat(uppercaseOnly.encodeStart(ops, "VALID").isSuccess()).isTrue();
            assertThat(uppercaseOnly.encodeStart(ops, "Invalid").isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("listOf()")
    class ListOfOperation {

        @Test
        @DisplayName("listOf() creates list codec")
        void listOfCreatesListCodec() {
            final Codec<List<String>> listCodec = Codecs.STRING.listOf();

            final DataResult<Object> encoded = listCodec.encodeStart(ops, List.of("a", "b", "c"));
            assertThat(encoded.isSuccess()).isTrue();

            @SuppressWarnings("unchecked")
            final List<Object> encodedList = (List<Object>) encoded.result().orElseThrow();
            assertThat(encodedList).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("listOf() decodes list")
        void listOfDecodesList() {
            final Codec<List<Integer>> listCodec = Codecs.INT.listOf();

            final DataResult<List<Integer>> decoded = listCodec.parse(ops, List.of(1, 2, 3));

            assertThat(decoded.result().orElseThrow()).containsExactly(1, 2, 3);
        }
    }

    @Nested
    @DisplayName("optionalOf()")
    class OptionalOfOperation {

        @Test
        @DisplayName("optionalOf() creates optional codec")
        void optionalOfCreatesOptionalCodec() {
            final Codec<Optional<String>> optCodec = Codecs.STRING.optionalOf();

            final DataResult<Object> present = optCodec.encodeStart(ops, Optional.of("hello"));
            assertThat(present.result()).contains("hello");

            final DataResult<Object> empty = optCodec.encodeStart(ops, Optional.empty());
            assertThat(empty.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("fieldOf()")
    class FieldOfOperation {

        @Test
        @DisplayName("fieldOf() creates MapCodec for named field")
        void fieldOfCreatesMapCodecForNamedField() {
            final MapCodec<String> nameField = Codecs.STRING.fieldOf("name");

            final Map<String, Object> input = Map.of("name", "Alice", "age", 30);
            final DataResult<String> result = nameField.decode(ops, input);

            assertThat(result.result()).contains("Alice");
        }

        @Test
        @DisplayName("fieldOf() encodes to map with field")
        void fieldOfEncodesToMapWithField() {
            final MapCodec<String> nameField = Codecs.STRING.fieldOf("name");

            final Map<String, Object> emptyMap = Map.of();
            final DataResult<Object> result = nameField.encode("Bob", ops, emptyMap);

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("name", "Bob");
        }

        @Test
        @DisplayName("fieldOf() returns error for missing field")
        void fieldOfReturnsErrorForMissingField() {
            final MapCodec<String> nameField = Codecs.STRING.fieldOf("name");

            final Map<String, Object> input = Map.of("other", "value");
            final DataResult<String> result = nameField.decode(ops, input);

            assertThat(result.isError()).isTrue();
            assertThat(result.error().orElse("")).contains("Missing field");
        }
    }

    @Nested
    @DisplayName("optionalFieldOf()")
    class OptionalFieldOfOperation {

        @Test
        @DisplayName("optionalFieldOf() returns Optional.empty() for missing field")
        void optionalFieldOfReturnsEmptyForMissingField() {
            final MapCodec<Optional<String>> optField = Codecs.STRING.optionalFieldOf("nickname");

            final Map<String, Object> input = Map.of("name", "Alice");
            final DataResult<Optional<String>> result = optField.decode(ops, input);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).isEmpty();
        }

        @Test
        @DisplayName("optionalFieldOf() returns value for present field")
        void optionalFieldOfReturnsValueForPresentField() {
            final MapCodec<Optional<String>> optField = Codecs.STRING.optionalFieldOf("nickname");

            final Map<String, Object> input = Map.of("nickname", "Bob");
            final DataResult<Optional<String>> result = optField.decode(ops, input);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow()).contains("Bob");
        }

        @Test
        @DisplayName("optionalFieldOf() with default returns default for missing")
        void optionalFieldOfWithDefaultReturnsDefaultForMissing() {
            final MapCodec<Integer> timeoutField = Codecs.INT.optionalFieldOf("timeout", 30);

            final Map<String, Object> input = Map.of();
            final DataResult<Integer> result = timeoutField.decode(ops, input);

            assertThat(result.result()).contains(30);
        }

        @Test
        @DisplayName("optionalFieldOf() with default returns actual value when present")
        void optionalFieldOfWithDefaultReturnsActualValueWhenPresent() {
            final MapCodec<Integer> timeoutField = Codecs.INT.optionalFieldOf("timeout", 30);

            final Map<String, Object> input = Map.of("timeout", 60);
            final DataResult<Integer> result = timeoutField.decode(ops, input);

            assertThat(result.result()).contains(60);
        }
    }

    @Nested
    @DisplayName("withErrorContext()")
    class WithErrorContextOperation {

        @Test
        @DisplayName("withErrorContext() prefixes error messages")
        void withErrorContextPrefixesErrorMessages() {
            final Codec<Integer> contextCodec = Codecs.INT.withErrorContext("age");

            final DataResult<Integer> result = contextCodec.parse(ops, "not a number");

            assertThat(result.isError()).isTrue();
            assertThat(result.error().orElse("")).startsWith("age:");
        }

        @Test
        @DisplayName("withErrorContext() does not affect success")
        void withErrorContextDoesNotAffectSuccess() {
            final Codec<Integer> contextCodec = Codecs.INT.withErrorContext("age");

            final DataResult<Integer> result = contextCodec.parse(ops, 25);

            assertThat(result.result()).contains(25);
        }
    }

    @Nested
    @DisplayName("orElse()")
    class OrElseOperation {

        @Test
        @DisplayName("orElse() uses primary codec when it succeeds")
        void orElseUsesPrimaryCodecWhenItSucceeds() {
            final Codec<Integer> primary = Codecs.INT;
            final Codec<Integer> fallback = Codecs.STRING.xmap(Integer::parseInt, Object::toString);
            final Codec<Integer> combined = primary.orElse(fallback);

            final DataResult<Integer> result = combined.parse(ops, 42);

            assertThat(result.result()).contains(42);
        }

        @Test
        @DisplayName("orElse() uses fallback codec when primary fails")
        void orElseUsesFallbackCodecWhenPrimaryFails() {
            final Codec<Integer> primary = Codecs.INT;
            final Codec<Integer> fallback = Codecs.STRING.xmap(Integer::parseInt, Object::toString);
            final Codec<Integer> combined = primary.orElse(fallback);

            final DataResult<Integer> result = combined.parse(ops, "123");

            assertThat(result.result()).contains(123);
        }

        @Test
        @DisplayName("orElse() fails when both codecs fail")
        void orElseFailsWhenBothCodecsFail() {
            final Codec<Integer> primary = Codecs.INT;
            final Codec<Integer> fallback = Codecs.INT;
            final Codec<Integer> combined = primary.orElse(fallback);

            final DataResult<Integer> result = combined.parse(ops, "not a number");

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("orElse() works for encoding too")
        void orElseWorksForEncodingToo() {
            // Create a codec that fails to encode negative numbers
            final Codec<Integer> positiveOnly = Codecs.INT.flatXmap(
                    DataResult::success,
                    v -> v >= 0 ? DataResult.success(v) : DataResult.error("Negative")
            );
            final Codec<Integer> anyInt = Codecs.INT;
            final Codec<Integer> combined = positiveOnly.orElse(anyInt);

            // Positive encodes with primary
            assertThat(combined.encodeStart(ops, 5).isSuccess()).isTrue();

            // Negative falls back
            assertThat(combined.encodeStart(ops, -5).isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("parse() methods")
    class ParseMethods {

        @Test
        @DisplayName("parse(ops, input) discards remaining")
        void parseDiscardsRemaining() {
            final DataResult<String> result = Codecs.STRING.parse(ops, "hello");

            assertThat(result.result()).contains("hello");
        }

        @Test
        @DisplayName("decode(ops, input) returns pair with remaining")
        void decodeReturnsPairWithRemaining() {
            final DataResult<Pair<String, Object>> result = Codecs.STRING.decode(ops, "hello");

            assertThat(result.isSuccess()).isTrue();
            final Pair<String, Object> pair = result.result().orElseThrow();
            assertThat(pair.first()).isEqualTo("hello");
            // Remaining is ops.empty()
        }
    }

    @Nested
    @DisplayName("encode() methods")
    class EncodeMethods {

        @Test
        @DisplayName("encodeStart() uses empty prefix")
        void encodeStartUsesEmptyPrefix() {
            final DataResult<Object> result = Codecs.STRING.encodeStart(ops, "test");

            assertThat(result.result()).contains("test");
        }

        @Test
        @DisplayName("encode() merges with prefix")
        void encodeMergesWithPrefix() {
            final DataResult<Object> result = Codecs.STRING.encode("test", ops, ops.empty());

            assertThat(result.result()).contains("test");
        }
    }

    @Nested
    @DisplayName("Static Codec Methods")
    class StaticCodecMethods {

        @Test
        @DisplayName("Codec.either() creates either codec")
        void codecEitherCreatesEitherCodec() {
            final var eitherCodec = Codec.either(Codecs.STRING, Codecs.INT);

            assertThat(eitherCodec.parse(ops, "hello").isSuccess()).isTrue();
            assertThat(eitherCodec.parse(ops, 42).isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Codec.pair() creates pair codec")
        void codecPairCreatesPairCodec() {
            final var pairCodec = Codec.pair(Codecs.STRING, Codecs.INT);

            assertThat(pairCodec).isNotNull();
        }
    }
}
