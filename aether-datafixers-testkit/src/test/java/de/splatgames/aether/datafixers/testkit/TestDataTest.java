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

package de.splatgames.aether.datafixers.testkit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestData")
class TestDataTest {

    @Nested
    @DisplayName("Primitive creation")
    class PrimitiveCreation {

        @Test
        @DisplayName("string() creates string Dynamic")
        void stringCreatesStringDynamic() {
            final Dynamic<JsonElement> dynamic = TestData.gson().string("hello");

            assertThat(dynamic.asString().result()).hasValue("hello");
        }

        @Test
        @DisplayName("integer() creates int Dynamic")
        void integerCreatesIntDynamic() {
            final Dynamic<JsonElement> dynamic = TestData.gson().integer(42);

            assertThat(dynamic.asInt().result()).hasValue(42);
        }

        @Test
        @DisplayName("longValue() creates long Dynamic")
        void longValueCreatesLongDynamic() {
            final Dynamic<JsonElement> dynamic = TestData.gson().longValue(9999999999L);

            assertThat(dynamic.asLong().result()).hasValue(9999999999L);
        }

        @Test
        @DisplayName("doubleValue() creates double Dynamic")
        void doubleValueCreatesDoubleDynamic() {
            final Dynamic<JsonElement> dynamic = TestData.gson().doubleValue(3.14);

            assertThat(dynamic.asDouble().result()).hasValue(3.14);
        }

        @Test
        @DisplayName("bool() creates boolean Dynamic")
        void boolCreatesBooleanDynamic() {
            final Dynamic<JsonElement> trueValue = TestData.gson().bool(true);
            final Dynamic<JsonElement> falseValue = TestData.gson().bool(false);

            assertThat(trueValue.asBoolean().result()).hasValue(true);
            assertThat(falseValue.asBoolean().result()).hasValue(false);
        }

        @Test
        @DisplayName("emptyMap() creates empty map Dynamic")
        void emptyMapCreatesEmptyMapDynamic() {
            final Dynamic<JsonElement> dynamic = TestData.gson().emptyMap();

            assertThat(dynamic.value()).isEqualTo(GsonOps.INSTANCE.emptyMap());
        }

        @Test
        @DisplayName("emptyList() creates empty list Dynamic")
        void emptyListCreatesEmptyListDynamic() {
            final Dynamic<JsonElement> dynamic = TestData.gson().emptyList();

            assertThat(dynamic.value()).isEqualTo(GsonOps.INSTANCE.emptyList());
        }
    }

    @Nested
    @DisplayName("Object building")
    class ObjectBuilding {

        @Test
        @DisplayName("simple object with primitives")
        void simpleObjectWithPrimitives() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Alice")
                    .put("age", 30)
                    .put("active", true)
                    .build();

            assertThat(dynamic.get("name").asString().result()).hasValue("Alice");
            assertThat(dynamic.get("age").asInt().result()).hasValue(30);
            assertThat(dynamic.get("active").asBoolean().result()).hasValue(true);
        }

        @Test
        @DisplayName("nested objects with putObject()")
        void nestedObjectsWithPutObject() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("name", "Player1")
                    .putObject("position", pos -> pos
                            .put("x", 100)
                            .put("y", 64)
                            .put("z", -200))
                    .build();

            assertThat(dynamic.get("name").asString().result()).hasValue("Player1");
            assertThat(dynamic.get("position").get("x").asInt().result()).hasValue(100);
            assertThat(dynamic.get("position").get("y").asInt().result()).hasValue(64);
            assertThat(dynamic.get("position").get("z").asInt().result()).hasValue(-200);
        }

        @Test
        @DisplayName("deeply nested objects")
        void deeplyNestedObjects() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .putObject("level1", l1 -> l1
                            .putObject("level2", l2 -> l2
                                    .putObject("level3", l3 -> l3
                                            .put("deepValue", "found"))))
                    .build();

            assertThat(dynamic.get("level1").get("level2").get("level3").get("deepValue")
                    .asString().result()).hasValue("found");
        }
    }

    @Nested
    @DisplayName("List building")
    class ListBuilding {

        @Test
        @DisplayName("simple list with primitives")
        void simpleListWithPrimitives() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .putList("numbers", list -> list
                            .add(1)
                            .add(2)
                            .add(3))
                    .build();

            final var numbers = dynamic.get("numbers").asListStream().result().orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asInt().result().orElse(0))
                    .toList();
            assertThat(numbers).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("list with objects")
        void listWithObjects() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .putList("items", list -> list
                            .addObject(item -> item.put("id", "sword").put("count", 1))
                            .addObject(item -> item.put("id", "apple").put("count", 64)))
                    .build();

            final var items = dynamic.get("items").asListStream().result().orElse(java.util.stream.Stream.empty()).toList();
            assertThat(items).hasSize(2);
        }

        @Test
        @DisplayName("standalone list")
        void standaloneList() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add("a")
                    .add("b")
                    .add("c")
                    .build();

            final var values = dynamic.asListStream().result().orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asString().result().orElse(""))
                    .toList();
            assertThat(values).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("addAll with varargs")
        void addAllWithVarargs() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .putList("tags", list -> list.addAll("tag1", "tag2", "tag3"))
                    .build();

            final var tags = dynamic.get("tags").asListStream().result().orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asString().result().orElse(""))
                    .toList();
            assertThat(tags).containsExactly("tag1", "tag2", "tag3");
        }
    }

    @Nested
    @DisplayName("put() with Dynamic values")
    class PutWithDynamicValues {

        @Test
        @DisplayName("put() with Dynamic inlines existing Dynamic")
        void putWithDynamicInlinesExistingDynamic() {
            final Dynamic<JsonElement> nested = TestData.gson().object()
                    .put("inner", "value")
                    .build();

            final Dynamic<JsonElement> outer = TestData.gson().object()
                    .put("name", "outer")
                    .put("nested", nested)
                    .build();

            assertThat(outer.get("nested").get("inner").asString().result()).hasValue("value");
        }
    }

    @Nested
    @DisplayName("using() with custom ops")
    class UsingCustomOps {

        @Test
        @DisplayName("works with GsonOps")
        void worksWithGsonOps() {
            final Dynamic<JsonElement> dynamic = TestData.using(GsonOps.INSTANCE).object()
                    .put("key", "value")
                    .build();

            assertThat(dynamic.get("key").asString().result()).hasValue("value");
            assertThat(dynamic.ops()).isSameAs(GsonOps.INSTANCE);
        }
    }

    @Nested
    @DisplayName("Format-specific factory methods")
    class FormatSpecificFactoryMethods {

        @Test
        @DisplayName("jacksonJson() creates Jackson JSON builder")
        void jacksonJsonCreatesJacksonJsonBuilder() {
            final Dynamic<JsonNode> dynamic = TestData.jacksonJson().object()
                    .put("key", "value")
                    .build();

            assertThat(dynamic.get("key").asString().result()).hasValue("value");
            assertThat(dynamic.ops()).isSameAs(JacksonJsonOps.INSTANCE);
        }

        @Test
        @DisplayName("snakeYaml() creates SnakeYAML builder")
        void snakeYamlCreatesSnakeYamlBuilder() {
            final Dynamic<Object> dynamic = TestData.snakeYaml().object()
                    .put("key", "value")
                    .build();

            assertThat(dynamic.get("key").asString().result()).hasValue("value");
            assertThat(dynamic.ops()).isSameAs(SnakeYamlOps.INSTANCE);
        }

        @Test
        @DisplayName("jacksonYaml() creates Jackson YAML builder")
        void jacksonYamlCreatesJacksonYamlBuilder() {
            final Dynamic<JsonNode> dynamic = TestData.jacksonYaml().object()
                    .put("key", "value")
                    .build();

            assertThat(dynamic.get("key").asString().result()).hasValue("value");
        }

        @Test
        @DisplayName("jacksonToml() creates Jackson TOML builder")
        void jacksonTomlCreatesJacksonTomlBuilder() {
            final Dynamic<JsonNode> dynamic = TestData.jacksonToml().object()
                    .put("key", "value")
                    .build();

            assertThat(dynamic.get("key").asString().result()).hasValue("value");
        }

        @Test
        @DisplayName("jacksonXml() creates Jackson XML builder")
        void jacksonXmlCreatesJacksonXmlBuilder() {
            final Dynamic<JsonNode> dynamic = TestData.jacksonXml().object()
                    .put("key", "value")
                    .build();

            assertThat(dynamic.get("key").asString().result()).hasValue("value");
        }
    }

    @Nested
    @DisplayName("Static primitive helpers with explicit ops")
    class StaticPrimitiveHelpersWithOps {

        @Test
        @DisplayName("string() with ops creates string")
        void stringWithOpsCreatesString() {
            final Dynamic<JsonElement> dynamic = TestData.string(GsonOps.INSTANCE, "hello");

            assertThat(dynamic.asString().result()).hasValue("hello");
        }

        @Test
        @DisplayName("integer() with ops creates integer")
        void integerWithOpsCreatesInteger() {
            final Dynamic<JsonElement> dynamic = TestData.integer(GsonOps.INSTANCE, 42);

            assertThat(dynamic.asInt().result()).hasValue(42);
        }

        @Test
        @DisplayName("longValue() with ops creates long")
        void longValueWithOpsCreatesLong() {
            final Dynamic<JsonElement> dynamic = TestData.longValue(GsonOps.INSTANCE, 9876543210L);

            assertThat(dynamic.asLong().result()).hasValue(9876543210L);
        }

        @Test
        @DisplayName("doubleValue() with ops creates double")
        void doubleValueWithOpsCreatesDouble() {
            final Dynamic<JsonElement> dynamic = TestData.doubleValue(GsonOps.INSTANCE, 3.14159);

            assertThat(dynamic.asDouble().result()).hasValue(3.14159);
        }

        @Test
        @DisplayName("bool() with ops creates boolean")
        void boolWithOpsCreatesBoolean() {
            final Dynamic<JsonElement> trueValue = TestData.bool(GsonOps.INSTANCE, true);
            final Dynamic<JsonElement> falseValue = TestData.bool(GsonOps.INSTANCE, false);

            assertThat(trueValue.asBoolean().result()).hasValue(true);
            assertThat(falseValue.asBoolean().result()).hasValue(false);
        }

        @Test
        @DisplayName("emptyMap() with ops creates empty map")
        void emptyMapWithOpsCreatesEmptyMap() {
            final Dynamic<JsonElement> dynamic = TestData.emptyMap(GsonOps.INSTANCE);

            assertThat(dynamic.value()).isEqualTo(GsonOps.INSTANCE.emptyMap());
        }

        @Test
        @DisplayName("emptyList() with ops creates empty list")
        void emptyListWithOpsCreatesEmptyList() {
            final Dynamic<JsonElement> dynamic = TestData.emptyList(GsonOps.INSTANCE);

            assertThat(dynamic.value()).isEqualTo(GsonOps.INSTANCE.emptyList());
        }
    }

    @Nested
    @DisplayName("TestDataListBuilder additional methods")
    class TestDataListBuilderMethods {

        @Test
        @DisplayName("add(long) adds long values")
        void addLongAddsLongValues() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(9999999999L)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asLong().result().orElse(0L))
                    .toList();
            assertThat(values).containsExactly(9999999999L);
        }

        @Test
        @DisplayName("add(double) adds double values")
        void addDoubleAddsDoubleValues() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(3.14)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asDouble().result().orElse(0.0))
                    .toList();
            assertThat(values).hasSize(1);
            assertThat(values.get(0)).isCloseTo(3.14, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("add(boolean) adds boolean values")
        void addBoolAddsBooleanValues() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(true)
                    .add(false)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asBoolean().result().orElse(null))
                    .toList();
            assertThat(values).containsExactly(true, false);
        }

        @Test
        @DisplayName("addList() nests lists")
        void addListNestsLists() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .addList(inner -> inner.add(1).add(2).add(3))
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .toList();
            assertThat(values).hasSize(1);

            final var innerValues = values.get(0).asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asInt().result().orElse(0))
                    .toList();
            assertThat(innerValues).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("add() with Dynamic inlines values")
        void addWithDynamicInlinesValues() {
            final Dynamic<JsonElement> existing = TestData.gson().object()
                    .put("nested", "value")
                    .build();

            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(existing)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .toList();
            assertThat(values).hasSize(1);
            assertThat(values.get(0).get("nested").asString().result()).hasValue("value");
        }

        @Test
        @DisplayName("add(float) adds float values")
        void addFloatAddsFloatValues() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add(2.5f)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asDouble().result().orElse(0.0))
                    .toList();
            assertThat(values).hasSize(1);
            assertThat(values.get(0)).isCloseTo(2.5, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        @DisplayName("add(byte) adds byte values")
        void addByteAddsByteValues() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add((byte) 127)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asInt().result().orElse(0))
                    .toList();
            assertThat(values).containsExactly(127);
        }

        @Test
        @DisplayName("add(short) adds short values")
        void addShortAddsShortValues() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .add((short) 32000)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asInt().result().orElse(0))
                    .toList();
            assertThat(values).containsExactly(32000);
        }

        @Test
        @DisplayName("addAll(long...) adds multiple long values")
        void addAllLongsAddsMultipleLongValues() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .addAll(1L, 2L, 3L)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asLong().result().orElse(0L))
                    .toList();
            assertThat(values).containsExactly(1L, 2L, 3L);
        }

        @Test
        @DisplayName("addAll(double...) adds multiple double values")
        void addAllDoublesAddsMultipleDoubleValues() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .addAll(1.0, 2.0, 3.0)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asDouble().result().orElse(0.0))
                    .toList();
            assertThat(values).containsExactly(1.0, 2.0, 3.0);
        }

        @Test
        @DisplayName("addAll(boolean...) adds multiple boolean values")
        void addAllBooleansAddsMultipleBooleanValues() {
            final Dynamic<JsonElement> dynamic = TestData.gson().list()
                    .addAll(true, false, true)
                    .build();

            final var values = dynamic.asListStream().result()
                    .orElse(java.util.stream.Stream.empty())
                    .map(d -> d.asBoolean().result().orElse(null))
                    .toList();
            assertThat(values).containsExactly(true, false, true);
        }

        @Test
        @DisplayName("size() returns correct count")
        void sizeReturnsCorrectCount() {
            final var builder = new TestDataListBuilder<>(GsonOps.INSTANCE);
            assertThat(builder.size()).isZero();

            builder.add("one");
            assertThat(builder.size()).isEqualTo(1);

            builder.add(2);
            assertThat(builder.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("isEmpty() returns correct state")
        void isEmptyReturnsCorrectState() {
            final var builder = new TestDataListBuilder<>(GsonOps.INSTANCE);
            assertThat(builder.isEmpty()).isTrue();

            builder.add("value");
            assertThat(builder.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("TestDataBuilder additional methods")
    class TestDataBuilderMethods {

        @Test
        @DisplayName("put() with long value")
        void putWithLongValue() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("timestamp", 1234567890123L)
                    .build();

            assertThat(dynamic.get("timestamp").asLong().result()).hasValue(1234567890123L);
        }

        @Test
        @DisplayName("put() with double value")
        void putWithDoubleValue() {
            final Dynamic<JsonElement> dynamic = TestData.gson().object()
                    .put("score", 99.99)
                    .build();

            assertThat(dynamic.get("score").asDouble().result()).hasValue(99.99);
        }
    }
}
