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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RecordCodecBuilder}.
 */
@DisplayName("RecordCodecBuilder")
class RecordCodecBuilderTest {

    private final TestOps ops = TestOps.INSTANCE;

    // Test record types
    record Single(String value) {}
    record Person(String name, int age) {}
    record Point3D(double x, double y, double z) {}
    record Rectangle(double x, double y, double width, double height) {}
    record FullRecord(String a, int b, double c, boolean d, String e) {}
    record MaxRecord(String a, int b, double c, boolean d, String e, long f) {}

    @Nested
    @DisplayName("create() Factory")
    class CreateFactory {

        @Test
        @DisplayName("create() returns a Codec")
        void createReturnsCodec() {
            final Codec<Person> codec = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codecs.STRING.fieldOf("name").forGetter(Person::name),
                            Codecs.INT.fieldOf("age").forGetter(Person::age)
                    ).apply(instance, Person::new)
            );

            assertThat(codec).isNotNull();
        }
    }

    @Nested
    @DisplayName("mapCodec() Factory")
    class MapCodecFactory {

        @Test
        @DisplayName("mapCodec() returns a MapCodec")
        void mapCodecReturnsMapCodec() {
            final MapCodec<Person> mapCodec = RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            Codecs.STRING.fieldOf("name").forGetter(Person::name),
                            Codecs.INT.fieldOf("age").forGetter(Person::age)
                    ).apply(instance, Person::new)
            );

            assertThat(mapCodec).isNotNull();
        }
    }

    @Nested
    @DisplayName("Single Field (Builder1)")
    class SingleField {

        private final Codec<Single> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codecs.STRING.fieldOf("value").forGetter(Single::value)
                ).apply(instance, Single::new)
        );

        @Test
        @DisplayName("encodes single field record")
        void encodesSingleFieldRecord() {
            final Single record = new Single("hello");

            final DataResult<Object> result = codec.encodeStart(ops, record);

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("value", "hello");
        }

        @Test
        @DisplayName("decodes single field record")
        void decodesSingleFieldRecord() {
            final Map<String, Object> input = Map.of("value", "world");

            final DataResult<Single> result = codec.parse(ops, input);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.result().orElseThrow().value()).isEqualTo("world");
        }
    }

    @Nested
    @DisplayName("Two Fields (Builder2)")
    class TwoFields {

        private final Codec<Person> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codecs.STRING.fieldOf("name").forGetter(Person::name),
                        Codecs.INT.fieldOf("age").forGetter(Person::age)
                ).apply(instance, Person::new)
        );

        @Test
        @DisplayName("encodes two field record")
        void encodesTwoFieldRecord() {
            final Person person = new Person("Alice", 30);

            final DataResult<Object> result = codec.encodeStart(ops, person);

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("name", "Alice");
            assertThat(encoded).containsEntry("age", 30);
        }

        @Test
        @DisplayName("decodes two field record")
        void decodesTwoFieldRecord() {
            final Map<String, Object> input = Map.of("name", "Bob", "age", 25);

            final DataResult<Person> result = codec.parse(ops, input);

            assertThat(result.isSuccess()).isTrue();
            final Person person = result.result().orElseThrow();
            assertThat(person.name()).isEqualTo("Bob");
            assertThat(person.age()).isEqualTo(25);
        }

        @Test
        @DisplayName("round-trip preserves data")
        void roundTripPreservesData() {
            final Person original = new Person("Charlie", 35);

            final DataResult<Object> encoded = codec.encodeStart(ops, original);
            final DataResult<Person> decoded = encoded.flatMap(e -> codec.parse(ops, e));

            assertThat(decoded.result().orElseThrow()).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("Three Fields (Builder3)")
    class ThreeFields {

        private final Codec<Point3D> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codecs.DOUBLE.fieldOf("x").forGetter(Point3D::x),
                        Codecs.DOUBLE.fieldOf("y").forGetter(Point3D::y),
                        Codecs.DOUBLE.fieldOf("z").forGetter(Point3D::z)
                ).apply(instance, Point3D::new)
        );

        @Test
        @DisplayName("encodes three field record")
        void encodesThreeFieldRecord() {
            final Point3D point = new Point3D(1.0, 2.0, 3.0);

            final DataResult<Object> result = codec.encodeStart(ops, point);

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("x", 1.0);
            assertThat(encoded).containsEntry("y", 2.0);
            assertThat(encoded).containsEntry("z", 3.0);
        }

        @Test
        @DisplayName("decodes three field record")
        void decodesThreeFieldRecord() {
            final Map<String, Object> input = Map.of("x", 4.0, "y", 5.0, "z", 6.0);

            final DataResult<Point3D> result = codec.parse(ops, input);

            assertThat(result.isSuccess()).isTrue();
            final Point3D point = result.result().orElseThrow();
            assertThat(point.x()).isEqualTo(4.0);
            assertThat(point.y()).isEqualTo(5.0);
            assertThat(point.z()).isEqualTo(6.0);
        }
    }

    @Nested
    @DisplayName("Four Fields (Builder4)")
    class FourFields {

        private final Codec<Rectangle> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codecs.DOUBLE.fieldOf("x").forGetter(Rectangle::x),
                        Codecs.DOUBLE.fieldOf("y").forGetter(Rectangle::y),
                        Codecs.DOUBLE.fieldOf("width").forGetter(Rectangle::width),
                        Codecs.DOUBLE.fieldOf("height").forGetter(Rectangle::height)
                ).apply(instance, Rectangle::new)
        );

        @Test
        @DisplayName("encodes four field record")
        void encodesFourFieldRecord() {
            final Rectangle rect = new Rectangle(10.0, 20.0, 100.0, 50.0);

            final DataResult<Object> result = codec.encodeStart(ops, rect);

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("x", 10.0);
            assertThat(encoded).containsEntry("y", 20.0);
            assertThat(encoded).containsEntry("width", 100.0);
            assertThat(encoded).containsEntry("height", 50.0);
        }

        @Test
        @DisplayName("decodes four field record")
        void decodesFourFieldRecord() {
            final Map<String, Object> input = Map.of("x", 5.0, "y", 10.0, "width", 200.0, "height", 150.0);

            final DataResult<Rectangle> result = codec.parse(ops, input);

            assertThat(result.isSuccess()).isTrue();
            final Rectangle rect = result.result().orElseThrow();
            assertThat(rect.x()).isEqualTo(5.0);
            assertThat(rect.y()).isEqualTo(10.0);
            assertThat(rect.width()).isEqualTo(200.0);
            assertThat(rect.height()).isEqualTo(150.0);
        }
    }

    @Nested
    @DisplayName("Five Fields (Builder5)")
    class FiveFields {

        private final Codec<FullRecord> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codecs.STRING.fieldOf("a").forGetter(FullRecord::a),
                        Codecs.INT.fieldOf("b").forGetter(FullRecord::b),
                        Codecs.DOUBLE.fieldOf("c").forGetter(FullRecord::c),
                        Codecs.BOOL.fieldOf("d").forGetter(FullRecord::d),
                        Codecs.STRING.fieldOf("e").forGetter(FullRecord::e)
                ).apply(instance, FullRecord::new)
        );

        @Test
        @DisplayName("encodes five field record")
        void encodesFiveFieldRecord() {
            final FullRecord record = new FullRecord("first", 2, 3.0, true, "fifth");

            final DataResult<Object> result = codec.encodeStart(ops, record);

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("a", "first");
            assertThat(encoded).containsEntry("b", 2);
            assertThat(encoded).containsEntry("c", 3.0);
            assertThat(encoded).containsEntry("d", true);
            assertThat(encoded).containsEntry("e", "fifth");
        }

        @Test
        @DisplayName("decodes five field record")
        void decodesFiveFieldRecord() {
            final Map<String, Object> input = Map.of(
                    "a", "one",
                    "b", 22,
                    "c", 33.3,
                    "d", false,
                    "e", "five"
            );

            final DataResult<FullRecord> result = codec.parse(ops, input);

            assertThat(result.isSuccess()).isTrue();
            final FullRecord record = result.result().orElseThrow();
            assertThat(record.a()).isEqualTo("one");
            assertThat(record.b()).isEqualTo(22);
            assertThat(record.c()).isEqualTo(33.3);
            assertThat(record.d()).isFalse();
            assertThat(record.e()).isEqualTo("five");
        }
    }

    @Nested
    @DisplayName("Six Fields (Builder6)")
    class SixFields {

        private final Codec<MaxRecord> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codecs.STRING.fieldOf("a").forGetter(MaxRecord::a),
                        Codecs.INT.fieldOf("b").forGetter(MaxRecord::b),
                        Codecs.DOUBLE.fieldOf("c").forGetter(MaxRecord::c),
                        Codecs.BOOL.fieldOf("d").forGetter(MaxRecord::d),
                        Codecs.STRING.fieldOf("e").forGetter(MaxRecord::e),
                        Codecs.LONG.fieldOf("f").forGetter(MaxRecord::f)
                ).apply(instance, MaxRecord::new)
        );

        @Test
        @DisplayName("encodes six field record")
        void encodesSixFieldRecord() {
            final MaxRecord record = new MaxRecord("A", 1, 2.0, true, "E", 6L);

            final DataResult<Object> result = codec.encodeStart(ops, record);

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> encoded = (Map<String, Object>) result.result().orElseThrow();
            assertThat(encoded).containsEntry("a", "A");
            assertThat(encoded).containsEntry("b", 1);
            assertThat(encoded).containsEntry("c", 2.0);
            assertThat(encoded).containsEntry("d", true);
            assertThat(encoded).containsEntry("e", "E");
            assertThat(encoded).containsEntry("f", 6L);
        }

        @Test
        @DisplayName("decodes six field record")
        void decodesSixFieldRecord() {
            final Map<String, Object> input = Map.of(
                    "a", "X",
                    "b", 10,
                    "c", 20.5,
                    "d", false,
                    "e", "Y",
                    "f", 100L
            );

            final DataResult<MaxRecord> result = codec.parse(ops, input);

            assertThat(result.isSuccess()).isTrue();
            final MaxRecord record = result.result().orElseThrow();
            assertThat(record.a()).isEqualTo("X");
            assertThat(record.b()).isEqualTo(10);
            assertThat(record.c()).isEqualTo(20.5);
            assertThat(record.d()).isFalse();
            assertThat(record.e()).isEqualTo("Y");
            assertThat(record.f()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("Optional Fields")
    class OptionalFields {

        record Config(String name, int timeout) {}

        private final Codec<Config> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codecs.STRING.fieldOf("name").forGetter(Config::name),
                        Codecs.INT.optionalFieldOf("timeout", 30).forGetter(Config::timeout)
                ).apply(instance, Config::new)
        );

        @Test
        @DisplayName("uses default value for missing optional field")
        void usesDefaultValueForMissingOptionalField() {
            final Map<String, Object> input = Map.of("name", "test");

            final DataResult<Config> result = codec.parse(ops, input);

            assertThat(result.isSuccess()).isTrue();
            final Config config = result.result().orElseThrow();
            assertThat(config.name()).isEqualTo("test");
            assertThat(config.timeout()).isEqualTo(30);
        }

        @Test
        @DisplayName("uses provided value for optional field")
        void usesProvidedValueForOptionalField() {
            final Map<String, Object> input = Map.of("name", "test", "timeout", 60);

            final DataResult<Config> result = codec.parse(ops, input);

            assertThat(result.isSuccess()).isTrue();
            final Config config = result.result().orElseThrow();
            assertThat(config.timeout()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("Instance.point()")
    class PointMethod {

        @Test
        @DisplayName("point() creates constant MapCodec")
        void pointCreatesConstantMapCodec() {
            final MapCodec<String> constantCodec = new RecordCodecBuilder.Instance<String>().point("constant");

            final DataResult<String> decoded = constantCodec.decode(ops, Map.of());
            assertThat(decoded.result()).contains("constant");

            final DataResult<Object> encoded = constantCodec.encode("ignored", ops, ops.emptyMap());
            assertThat(encoded.isSuccess()).isTrue();
            // Encoding returns unchanged map
        }
    }

    @Nested
    @DisplayName("Field Record")
    class FieldRecord {

        @Test
        @DisplayName("Field record holds codec and getter")
        void fieldRecordHoldsCodecAndGetter() {
            final MapCodec<String> mapCodec = Codecs.STRING.fieldOf("test");
            final RecordCodecBuilder.Field<Person, String> field =
                    new RecordCodecBuilder.Field<>(mapCodec, Person::name);

            assertThat(field.codec()).isSameAs(mapCodec);
            assertThat(field.getter()).isNotNull();

            final Person person = new Person("Alice", 30);
            assertThat(field.getter().apply(person)).isEqualTo("Alice");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        private final Codec<Person> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codecs.STRING.fieldOf("name").forGetter(Person::name),
                        Codecs.INT.fieldOf("age").forGetter(Person::age)
                ).apply(instance, Person::new)
        );

        @Test
        @DisplayName("returns error for missing required field")
        void returnsErrorForMissingRequiredField() {
            final Map<String, Object> input = Map.of("name", "Alice");

            final DataResult<Person> result = codec.parse(ops, input);

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("returns error for wrong type")
        void returnsErrorForWrongType() {
            final Map<String, Object> input = Map.of("name", "Alice", "age", "not a number");

            final DataResult<Person> result = codec.parse(ops, input);

            assertThat(result.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("Nested Records")
    class NestedRecords {

        record Address(String street, String city) {}
        record PersonWithAddress(String name, Address address) {}

        @Test
        @DisplayName("handles nested record codecs")
        void handlesNestedRecordCodecs() {
            final MapCodec<Address> addressCodec = RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            Codecs.STRING.fieldOf("street").forGetter(Address::street),
                            Codecs.STRING.fieldOf("city").forGetter(Address::city)
                    ).apply(instance, Address::new)
            );

            final Codec<PersonWithAddress> personCodec = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codecs.STRING.fieldOf("name").forGetter(PersonWithAddress::name),
                            addressCodec.forGetter(PersonWithAddress::address)
                    ).apply(instance, PersonWithAddress::new)
            );

            final PersonWithAddress person = new PersonWithAddress("Alice", new Address("Main St", "Boston"));

            final DataResult<Object> encoded = personCodec.encodeStart(ops, person);
            assertThat(encoded.isSuccess()).isTrue();

            @SuppressWarnings("unchecked")
            final Map<String, Object> encodedMap = (Map<String, Object>) encoded.result().orElseThrow();
            assertThat(encodedMap).containsEntry("name", "Alice");
            assertThat(encodedMap).containsEntry("street", "Main St");
            assertThat(encodedMap).containsEntry("city", "Boston");
        }
    }
}
