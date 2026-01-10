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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MapCodec}.
 */
@DisplayName("MapCodec")
class MapCodecTest {

    private final TestOps ops = TestOps.INSTANCE;

    @Nested
    @DisplayName("Factory Method of()")
    class FactoryMethodOf {

        @Test
        @DisplayName("of() creates MapCodec from encoder and decoder")
        void ofCreatesMapCodecFromEncoderAndDecoder() {
            final MapCodec.MapEncoder<String> encoder = new MapCodec.MapEncoder<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final String input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T map) {
                    return ops.mergeToMap(map, ops.createString("name"), ops.createString(input));
                }
            };
            final MapCodec.MapDecoder<String> decoder = new MapCodec.MapDecoder<>() {
                @NotNull
                @Override
                public <T> DataResult<String> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
                    final T field = ops.get(input, "name");
                    return field != null
                            ? ops.getStringValue(field)
                            : DataResult.error("Missing field: name");
                }
            };
            final MapCodec<String> nameCodec = MapCodec.of(encoder, decoder);

            // Test encode
            final DataResult<Object> encoded = nameCodec.encode("Alice", ops, ops.emptyMap());
            assertThat(encoded.isSuccess()).isTrue();
            @SuppressWarnings("unchecked") final Map<String, Object> encodedMap = (Map<String, Object>) encoded.result().orElseThrow();
            assertThat(encodedMap).containsEntry("name", "Alice");

            // Test decode
            final Map<String, Object> input = Map.of("name", "Bob");
            final DataResult<String> decoded = nameCodec.decode(ops, input);
            assertThat(decoded.result()).contains("Bob");
        }
    }

    @Nested
    @DisplayName("encode()")
    class EncodeOperation {

        @Test
        @DisplayName("encode() adds field to existing map")
        void encodeAddsFieldToExistingMap() {
            final MapCodec<String> nameField = Codecs.STRING.fieldOf("name");

            final Map<String, Object> existingMap = new HashMap<>();
            existingMap.put("age", 30);

            final DataResult<Object> result = nameField.encode("Alice", ops, existingMap);

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked") final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("name", "Alice");
            assertThat(encoded).containsEntry("age", 30);
        }

        @Test
        @DisplayName("encode() creates map with field from empty")
        void encodeCreatesMapWithFieldFromEmpty() {
            final MapCodec<Integer> ageField = Codecs.INT.fieldOf("age");

            final DataResult<Object> result = ageField.encode(25, ops, ops.emptyMap());

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked") final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("age", 25);
        }
    }

    @Nested
    @DisplayName("decode()")
    class DecodeOperation {

        @Test
        @DisplayName("decode() extracts field from map")
        void decodeExtractsFieldFromMap() {
            final MapCodec<String> nameField = Codecs.STRING.fieldOf("name");

            final Map<String, Object> input = Map.of("name", "Alice", "age", 30);
            final DataResult<String> result = nameField.decode(ops, input);

            assertThat(result.result()).contains("Alice");
        }

        @Test
        @DisplayName("decode() returns error for missing field")
        void decodeReturnsErrorForMissingField() {
            final MapCodec<String> nameField = Codecs.STRING.fieldOf("name");

            final Map<String, Object> input = Map.of("age", 30);
            final DataResult<String> result = nameField.decode(ops, input);

            assertThat(result.isError()).isTrue();
            assertThat(result.error().orElse("")).contains("Missing field");
        }

        @Test
        @DisplayName("decode() returns error for wrong type")
        void decodeReturnsErrorForWrongType() {
            final MapCodec<Integer> ageField = Codecs.INT.fieldOf("age");

            final Map<String, Object> input = Map.of("age", "not a number");
            final DataResult<Integer> result = ageField.decode(ops, input);

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("xmap()")
    class XmapOperation {

        @Test
        @DisplayName("xmap() transforms decoded value")
        void xmapTransformsDecodedValue() {
            final MapCodec<String> upperCaseField = Codecs.STRING.fieldOf("text")
                    .xmap(String::toUpperCase, String::toLowerCase);

            final Map<String, Object> input = Map.of("text", "hello");
            final DataResult<String> result = upperCaseField.decode(ops, input);

            assertThat(result.result()).contains("HELLO");
        }

        @Test
        @DisplayName("xmap() transforms encoded value")
        void xmapTransformsEncodedValue() {
            final MapCodec<String> upperCaseField = Codecs.STRING.fieldOf("text")
                    .xmap(String::toUpperCase, String::toLowerCase);

            final DataResult<Object> result = upperCaseField.encode("HELLO", ops, ops.emptyMap());

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked") final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("text", "hello");
        }

        @Test
        @DisplayName("xmap() works with wrapper types")
        void xmapWorksWithWrapperTypes() {
            record UserId(int id) {
            }

            final MapCodec<UserId> userIdField = Codecs.INT.fieldOf("userId")
                    .xmap(UserId::new, UserId::id);

            final Map<String, Object> input = Map.of("userId", 123);
            final DataResult<UserId> decoded = userIdField.decode(ops, input);
            assertThat(decoded.result().orElseThrow().id()).isEqualTo(123);

            final DataResult<Object> encoded = userIdField.encode(new UserId(456), ops, ops.emptyMap());
            @SuppressWarnings("unchecked") final Map<String, Object> encodedMap = (Map<String, Object>) encoded.result().orElseThrow();
            assertThat(encodedMap).containsEntry("userId", 456);
        }
    }

    @Nested
    @DisplayName("flatXmap()")
    class FlatXmapOperation {

        @Test
        @DisplayName("flatXmap() allows validation during decode")
        void flatXmapAllowsValidationDuringDecode() {
            final MapCodec<Integer> positiveField = Codecs.INT.fieldOf("count").flatXmap(
                    value -> value > 0
                            ? DataResult.success(value)
                            : DataResult.error("Must be positive"),
                    DataResult::success
            );

            final DataResult<Integer> valid = positiveField.decode(ops, Map.of("count", 5));
            assertThat(valid.result()).contains(5);

            final DataResult<Integer> invalid = positiveField.decode(ops, Map.of("count", -5));
            assertThat(invalid.isError()).isTrue();
        }

        @Test
        @DisplayName("flatXmap() allows validation during encode")
        void flatXmapAllowsValidationDuringEncode() {
            final MapCodec<Integer> boundedField = Codecs.INT.fieldOf("value").flatXmap(
                    DataResult::success,
                    value -> value >= 0 && value <= 100
                            ? DataResult.success(value)
                            : DataResult.error("Must be 0-100")
            );

            final DataResult<Object> valid = boundedField.encode(50, ops, ops.emptyMap());
            assertThat(valid.isSuccess()).isTrue();

            final DataResult<Object> invalid = boundedField.encode(200, ops, ops.emptyMap());
            assertThat(invalid.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("forGetter()")
    class ForGetterOperation {

        @Test
        @DisplayName("forGetter() creates Field for RecordCodecBuilder")
        void forGetterCreatesFieldForRecordCodecBuilder() {
            final RecordCodecBuilder.Field<Person, String> nameField =
                    Codecs.STRING.fieldOf("name").forGetter(Person::name);

            assertThat(nameField).isNotNull();
            assertThat(nameField.codec()).isNotNull();
            assertThat(nameField.getter()).isNotNull();

            // Test getter
            final Person person = new Person("Alice", 30);
            assertThat(nameField.getter().apply(person)).isEqualTo("Alice");
        }

        @Test
        @DisplayName("forGetter() Field decodes correctly")
        void forGetterFieldDecodesCorrectly() {
            final RecordCodecBuilder.Field<Person, String> nameField =
                    Codecs.STRING.fieldOf("name").forGetter(Person::name);

            final Map<String, Object> input = Map.of("name", "Bob");
            final DataResult<String> result = nameField.codec().decode(ops, input);

            assertThat(result.result()).contains("Bob");
        }

        @Test
        @DisplayName("forGetter() Field encodes using getter")
        void forGetterFieldEncodesUsingGetter() {
            final RecordCodecBuilder.Field<Person, Integer> ageField =
                    Codecs.INT.fieldOf("age").forGetter(Person::age);

            final Person person = new Person("Alice", 25);
            final Integer value = ageField.getter().apply(person);

            final DataResult<Object> result = ageField.codec().encode(value, ops, ops.emptyMap());
            @SuppressWarnings("unchecked") final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("age", 25);
        }

        record Person(String name, int age) {
        }
    }

    @Nested
    @DisplayName("codec()")
    class CodecConversion {

        @Test
        @DisplayName("codec() converts MapCodec to Codec")
        void codecConvertsMapCodecToCodec() {
            final MapCodec<String> mapCodec = Codecs.STRING.fieldOf("name");
            final Codec<String> codec = mapCodec.codec();

            assertThat(codec).isNotNull();
        }

        @Test
        @DisplayName("codec() encode creates map with field")
        void codecEncodeCreatesMapWithField() {
            final Codec<String> codec = Codecs.STRING.fieldOf("name").codec();

            final DataResult<Object> result = codec.encodeStart(ops, "Alice");

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked") final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("name", "Alice");
        }

        @Test
        @DisplayName("codec() decode extracts from map")
        void codecDecodeExtractsFromMap() {
            final Codec<String> codec = Codecs.STRING.fieldOf("name").codec();

            final Map<String, Object> input = Map.of("name", "Bob");
            final DataResult<String> result = codec.parse(ops, input);

            assertThat(result.result()).contains("Bob");
        }

        @Test
        @DisplayName("codec() round-trip works")
        void codecRoundTripWorks() {
            final Codec<String> codec = Codecs.STRING.fieldOf("value").codec();
            final String original = "test-value";

            final DataResult<Object> encoded = codec.encodeStart(ops, original);
            final DataResult<String> decoded = encoded.flatMap(e -> codec.parse(ops, e));

            assertThat(decoded.result()).contains(original);
        }
    }

    @Nested
    @DisplayName("Multiple Fields")
    class MultipleFields {

        @Test
        @DisplayName("multiple MapCodecs encode to same map")
        void multipleMapCodecsEncodeToSameMap() {
            final MapCodec<String> nameField = Codecs.STRING.fieldOf("name");
            final MapCodec<Integer> ageField = Codecs.INT.fieldOf("age");

            // Start with empty map, add name
            DataResult<Object> result = nameField.encode("Alice", ops, ops.emptyMap());
            assertThat(result.isSuccess()).isTrue();

            // Add age to the result
            @SuppressWarnings("unchecked") final Map<String, Object> withName = (Map<String, Object>) result.result().orElseThrow();
            result = ageField.encode(30, ops, withName);

            @SuppressWarnings("unchecked") final Map<String, Object> complete = (Map<String, Object>) result.result().orElseThrow();
            assertThat(complete).containsEntry("name", "Alice");
            assertThat(complete).containsEntry("age", 30);
        }

        @Test
        @DisplayName("multiple MapCodecs decode from same map")
        void multipleMapCodecsDecodeFromSameMap() {
            final MapCodec<String> nameField = Codecs.STRING.fieldOf("name");
            final MapCodec<Integer> ageField = Codecs.INT.fieldOf("age");

            final Map<String, Object> input = Map.of("name", "Bob", "age", 25);

            final DataResult<String> name = nameField.decode(ops, input);
            final DataResult<Integer> age = ageField.decode(ops, input);

            assertThat(name.result()).contains("Bob");
            assertThat(age.result()).contains(25);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles empty string field")
        void handlesEmptyStringField() {
            final MapCodec<String> field = Codecs.STRING.fieldOf("value");

            final Map<String, Object> input = Map.of("value", "");
            final DataResult<String> result = field.decode(ops, input);

            assertThat(result.result()).contains("");
        }

        @Test
        @DisplayName("handles zero numeric field")
        void handlesZeroNumericField() {
            final MapCodec<Integer> field = Codecs.INT.fieldOf("count");

            final Map<String, Object> input = Map.of("count", 0);
            final DataResult<Integer> result = field.decode(ops, input);

            assertThat(result.result()).contains(0);
        }

        @Test
        @DisplayName("handles negative numeric field")
        void handlesNegativeNumericField() {
            final MapCodec<Integer> field = Codecs.INT.fieldOf("offset");

            final Map<String, Object> input = Map.of("offset", -100);
            final DataResult<Integer> result = field.decode(ops, input);

            assertThat(result.result()).contains(-100);
        }

        @Test
        @DisplayName("field name is case sensitive")
        void fieldNameIsCaseSensitive() {
            final MapCodec<String> field = Codecs.STRING.fieldOf("Name");

            final Map<String, Object> input = Map.of("name", "alice");
            final DataResult<String> result = field.decode(ops, input);

            assertThat(result.isError()).isTrue();
        }
    }
}
