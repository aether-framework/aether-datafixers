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

package de.splatgames.aether.datafixers.codec.yaml.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JacksonYamlOps}.
 */
@DisplayName("JacksonYamlOps")
class JacksonYamlOpsTest {

    private final JacksonYamlOps ops = JacksonYamlOps.INSTANCE;
    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    @Nested
    @DisplayName("Singleton Instance")
    class SingletonInstance {

        @Test
        @DisplayName("INSTANCE is not null")
        void instanceIsNotNull() {
            assertThat(JacksonYamlOps.INSTANCE).isNotNull();
        }

        @Test
        @DisplayName("INSTANCE is same reference")
        void instanceIsSameReference() {
            assertThat(JacksonYamlOps.INSTANCE).isSameAs(ops);
        }

        @Test
        @DisplayName("toString() returns JacksonYamlOps")
        void toStringReturnsJacksonYamlOps() {
            assertThat(ops.toString()).isEqualTo("JacksonYamlOps");
        }

        @Test
        @DisplayName("mapper() returns YAMLMapper")
        void mapperReturnsYAMLMapper() {
            assertThat(ops.mapper()).isNotNull();
            assertThat(ops.mapper()).isInstanceOf(YAMLMapper.class);
        }
    }

    @Nested
    @DisplayName("Custom YAMLMapper")
    class CustomYAMLMapper {

        @Test
        @DisplayName("can create instance with custom YAMLMapper")
        void canCreateInstanceWithCustomYAMLMapper() {
            final YAMLMapper customMapper = new YAMLMapper();
            final JacksonYamlOps customOps = new JacksonYamlOps(customMapper);

            assertThat(customOps.mapper()).isSameAs(customMapper);
        }
    }

    @Nested
    @DisplayName("Empty Values")
    class EmptyValues {

        @Test
        @DisplayName("empty() returns NullNode")
        void emptyReturnsNullNode() {
            assertThat(ops.empty()).isEqualTo(NullNode.getInstance());
        }

        @Test
        @DisplayName("emptyList() returns empty ArrayNode")
        void emptyListReturnsEmptyArrayNode() {
            final JsonNode result = ops.emptyList();

            assertThat(result.isArray()).isTrue();
            assertThat(result.size()).isZero();
        }

        @Test
        @DisplayName("emptyMap() returns empty ObjectNode")
        void emptyMapReturnsEmptyObjectNode() {
            final JsonNode result = ops.emptyMap();

            assertThat(result.isObject()).isTrue();
            assertThat(result.size()).isZero();
        }
    }

    @Nested
    @DisplayName("Type Checks")
    class TypeChecks {

        @Test
        @DisplayName("isMap() returns true for ObjectNode")
        void isMapReturnsTrueForObjectNode() {
            assertThat(ops.isMap(factory.objectNode())).isTrue();
        }

        @Test
        @DisplayName("isMap() returns false for non-object")
        void isMapReturnsFalseForNonObject() {
            assertThat(ops.isMap(factory.arrayNode())).isFalse();
            assertThat(ops.isMap(TextNode.valueOf("test"))).isFalse();
            assertThat(ops.isMap(NullNode.getInstance())).isFalse();
        }

        @Test
        @DisplayName("isList() returns true for ArrayNode")
        void isListReturnsTrueForArrayNode() {
            assertThat(ops.isList(factory.arrayNode())).isTrue();
        }

        @Test
        @DisplayName("isList() returns false for non-array")
        void isListReturnsFalseForNonArray() {
            assertThat(ops.isList(factory.objectNode())).isFalse();
            assertThat(ops.isList(TextNode.valueOf("test"))).isFalse();
            assertThat(ops.isList(NullNode.getInstance())).isFalse();
        }

        @Test
        @DisplayName("isString() returns true for TextNode")
        void isStringReturnsTrueForTextNode() {
            assertThat(ops.isString(TextNode.valueOf("test"))).isTrue();
        }

        @Test
        @DisplayName("isString() returns false for non-text")
        void isStringReturnsFalseForNonText() {
            assertThat(ops.isString(IntNode.valueOf(42))).isFalse();
            assertThat(ops.isString(BooleanNode.TRUE)).isFalse();
            assertThat(ops.isString(factory.objectNode())).isFalse();
        }

        @Test
        @DisplayName("isNumber() returns true for numeric nodes")
        void isNumberReturnsTrueForNumericNodes() {
            assertThat(ops.isNumber(IntNode.valueOf(42))).isTrue();
            assertThat(ops.isNumber(DoubleNode.valueOf(3.14))).isTrue();
        }

        @Test
        @DisplayName("isNumber() returns false for non-number")
        void isNumberReturnsFalseForNonNumber() {
            assertThat(ops.isNumber(TextNode.valueOf("test"))).isFalse();
            assertThat(ops.isNumber(BooleanNode.TRUE)).isFalse();
            assertThat(ops.isNumber(factory.objectNode())).isFalse();
        }

        @Test
        @DisplayName("isBoolean() returns true for BooleanNode")
        void isBooleanReturnsTrueForBooleanNode() {
            assertThat(ops.isBoolean(BooleanNode.TRUE)).isTrue();
            assertThat(ops.isBoolean(BooleanNode.FALSE)).isTrue();
        }

        @Test
        @DisplayName("isBoolean() returns false for non-boolean")
        void isBooleanReturnsFalseForNonBoolean() {
            assertThat(ops.isBoolean(TextNode.valueOf("test"))).isFalse();
            assertThat(ops.isBoolean(IntNode.valueOf(42))).isFalse();
            assertThat(ops.isBoolean(factory.objectNode())).isFalse();
        }
    }

    @Nested
    @DisplayName("Primitive Creation")
    class PrimitiveCreation {

        @Test
        @DisplayName("createString() creates TextNode")
        void createStringCreatesTextNode() {
            final JsonNode result = ops.createString("hello");

            assertThat(result.isTextual()).isTrue();
            assertThat(result.asText()).isEqualTo("hello");
        }

        @Test
        @DisplayName("createInt() creates IntNode")
        void createIntCreatesIntNode() {
            final JsonNode result = ops.createInt(42);

            assertThat(result.isInt()).isTrue();
            assertThat(result.asInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("createLong() creates LongNode")
        void createLongCreatesLongNode() {
            final JsonNode result = ops.createLong(Long.MAX_VALUE);

            assertThat(result.isLong()).isTrue();
            assertThat(result.asLong()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("createFloat() creates FloatNode")
        void createFloatCreatesFloatNode() {
            final JsonNode result = ops.createFloat(3.14f);

            assertThat(result.isFloat()).isTrue();
            assertThat(result.floatValue()).isEqualTo(3.14f);
        }

        @Test
        @DisplayName("createDouble() creates DoubleNode")
        void createDoubleCreatesDoubleNode() {
            final JsonNode result = ops.createDouble(3.14159);

            assertThat(result.isDouble()).isTrue();
            assertThat(result.asDouble()).isEqualTo(3.14159);
        }

        @Test
        @DisplayName("createByte() creates ShortNode")
        void createByteCreatesShortNode() {
            final JsonNode result = ops.createByte((byte) 127);

            assertThat(result.isShort()).isTrue();
            assertThat(result.shortValue()).isEqualTo((short) 127);
        }

        @Test
        @DisplayName("createShort() creates ShortNode")
        void createShortCreatesShortNode() {
            final JsonNode result = ops.createShort((short) 32767);

            assertThat(result.isShort()).isTrue();
            assertThat(result.shortValue()).isEqualTo((short) 32767);
        }

        @Test
        @DisplayName("createBoolean() creates BooleanNode")
        void createBooleanCreatesBooleanNode() {
            assertThat(ops.createBoolean(true).asBoolean()).isTrue();
            assertThat(ops.createBoolean(false).asBoolean()).isFalse();
        }

        @Test
        @DisplayName("createNumeric() creates appropriate node type")
        void createNumericCreatesAppropriateNodeType() {
            assertThat(ops.createNumeric(42).isInt()).isTrue();
            assertThat(ops.createNumeric(42L).isLong()).isTrue();
            assertThat(ops.createNumeric(3.14f).isFloat()).isTrue();
            assertThat(ops.createNumeric(3.14).isDouble()).isTrue();
            assertThat(ops.createNumeric((short) 10).isShort()).isTrue();
            assertThat(ops.createNumeric((byte) 5).isShort()).isTrue();
        }
    }

    @Nested
    @DisplayName("Primitive Reading")
    class PrimitiveReading {

        @Test
        @DisplayName("getStringValue() returns string from TextNode")
        void getStringValueReturnsStringFromTextNode() {
            final DataResult<String> result = ops.getStringValue(TextNode.valueOf("hello"));

            assertThat(result.result()).contains("hello");
        }

        @Test
        @DisplayName("getStringValue() returns error for non-string")
        void getStringValueReturnsErrorForNonString() {
            assertThat(ops.getStringValue(IntNode.valueOf(42)).isError()).isTrue();
            assertThat(ops.getStringValue(factory.objectNode()).isError()).isTrue();
        }

        @Test
        @DisplayName("getNumberValue() returns number from numeric node")
        void getNumberValueReturnsNumberFromNumericNode() {
            final DataResult<Number> result = ops.getNumberValue(IntNode.valueOf(42));

            assertThat(result.result()).isPresent();
            assertThat(result.result().get().intValue()).isEqualTo(42);
        }

        @Test
        @DisplayName("getNumberValue() returns error for non-number")
        void getNumberValueReturnsErrorForNonNumber() {
            assertThat(ops.getNumberValue(TextNode.valueOf("test")).isError()).isTrue();
            assertThat(ops.getNumberValue(factory.objectNode()).isError()).isTrue();
        }

        @Test
        @DisplayName("getBooleanValue() returns boolean from BooleanNode")
        void getBooleanValueReturnsBooleanFromBooleanNode() {
            assertThat(ops.getBooleanValue(BooleanNode.TRUE).result()).contains(true);
            assertThat(ops.getBooleanValue(BooleanNode.FALSE).result()).contains(false);
        }

        @Test
        @DisplayName("getBooleanValue() returns error for non-boolean")
        void getBooleanValueReturnsErrorForNonBoolean() {
            assertThat(ops.getBooleanValue(TextNode.valueOf("test")).isError()).isTrue();
            assertThat(ops.getBooleanValue(IntNode.valueOf(42)).isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("List Operations")
    class ListOperations {

        @Test
        @DisplayName("createList() creates ArrayNode from stream")
        void createListCreatesArrayNodeFromStream() {
            final JsonNode result = ops.createList(Stream.of(
                    ops.createInt(1),
                    ops.createInt(2),
                    ops.createInt(3)
            ));

            assertThat(result.isArray()).isTrue();
            assertThat(result.size()).isEqualTo(3);
            assertThat(result.get(0).asInt()).isEqualTo(1);
            assertThat(result.get(1).asInt()).isEqualTo(2);
            assertThat(result.get(2).asInt()).isEqualTo(3);
        }

        @Test
        @DisplayName("getList() returns stream of elements")
        void getListReturnsStreamOfElements() {
            final ArrayNode array = factory.arrayNode();
            array.add(1);
            array.add(2);
            array.add(3);

            final DataResult<Stream<JsonNode>> result = ops.getList(array);

            assertThat(result.isSuccess()).isTrue();
            final List<JsonNode> list = result.result().orElseThrow().toList();
            assertThat(list).hasSize(3);
        }

        @Test
        @DisplayName("getList() returns error for non-array")
        void getListReturnsErrorForNonArray() {
            assertThat(ops.getList(factory.objectNode()).isError()).isTrue();
            assertThat(ops.getList(TextNode.valueOf("test")).isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToList() appends to existing array")
        void mergeToListAppendsToExistingArray() {
            final ArrayNode array = factory.arrayNode();
            array.add(1);
            array.add(2);

            final DataResult<JsonNode> result = ops.mergeToList(array, ops.createInt(3));

            assertThat(result.isSuccess()).isTrue();
            final JsonNode merged = result.result().orElseThrow();
            assertThat(merged.size()).isEqualTo(3);
            assertThat(merged.get(2).asInt()).isEqualTo(3);
        }

        @Test
        @DisplayName("mergeToList() creates new array from null")
        void mergeToListCreatesNewArrayFromNull() {
            final DataResult<JsonNode> result = ops.mergeToList(NullNode.getInstance(), ops.createInt(1));

            assertThat(result.isSuccess()).isTrue();
            final JsonNode array = result.result().orElseThrow();
            assertThat(array.size()).isEqualTo(1);
            assertThat(array.get(0).asInt()).isEqualTo(1);
        }

        @Test
        @DisplayName("mergeToList() returns error for non-array input")
        void mergeToListReturnsErrorForNonArrayInput() {
            final DataResult<JsonNode> result = ops.mergeToList(factory.objectNode(), ops.createInt(1));

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToList() preserves original array immutability")
        void mergeToListPreservesOriginalArrayImmutability() {
            final ArrayNode original = factory.arrayNode();
            original.add(1);

            ops.mergeToList(original, ops.createInt(2));

            assertThat(original.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Map Operations")
    class MapOperations {

        @Test
        @DisplayName("createMap() creates ObjectNode from stream")
        void createMapCreatesObjectNodeFromStream() {
            final JsonNode result = ops.createMap(Stream.of(
                    Pair.of(ops.createString("name"), ops.createString("Alice")),
                    Pair.of(ops.createString("age"), ops.createInt(30))
            ));

            assertThat(result.isObject()).isTrue();
            assertThat(result.get("name").asText()).isEqualTo("Alice");
            assertThat(result.get("age").asInt()).isEqualTo(30);
        }

        @Test
        @DisplayName("getMapEntries() returns stream of entries")
        void getMapEntriesReturnsStreamOfEntries() {
            final ObjectNode object = factory.objectNode();
            object.put("name", "Bob");
            object.put("age", 25);

            final DataResult<Stream<Pair<JsonNode, JsonNode>>> result = ops.getMapEntries(object);

            assertThat(result.isSuccess()).isTrue();
            final List<Pair<JsonNode, JsonNode>> entries = result.result().orElseThrow().toList();
            assertThat(entries).hasSize(2);
        }

        @Test
        @DisplayName("getMapEntries() returns error for non-object")
        void getMapEntriesReturnsErrorForNonObject() {
            assertThat(ops.getMapEntries(factory.arrayNode()).isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToMap() adds key-value to existing object")
        void mergeToMapAddsKeyValueToExistingObject() {
            final ObjectNode object = factory.objectNode();
            object.put("name", "Alice");

            final DataResult<JsonNode> result = ops.mergeToMap(
                    object,
                    ops.createString("age"),
                    ops.createInt(30)
            );

            assertThat(result.isSuccess()).isTrue();
            final JsonNode merged = result.result().orElseThrow();
            assertThat(merged.get("name").asText()).isEqualTo("Alice");
            assertThat(merged.get("age").asInt()).isEqualTo(30);
        }

        @Test
        @DisplayName("mergeToMap() creates new object from null")
        void mergeToMapCreatesNewObjectFromNull() {
            final DataResult<JsonNode> result = ops.mergeToMap(
                    NullNode.getInstance(),
                    ops.createString("key"),
                    ops.createString("value")
            );

            assertThat(result.isSuccess()).isTrue();
            final JsonNode object = result.result().orElseThrow();
            assertThat(object.get("key").asText()).isEqualTo("value");
        }

        @Test
        @DisplayName("mergeToMap() returns error for non-string key")
        void mergeToMapReturnsErrorForNonStringKey() {
            final DataResult<JsonNode> result = ops.mergeToMap(
                    factory.objectNode(),
                    ops.createInt(42),
                    ops.createString("value")
            );

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToMap() merges two objects")
        void mergeToMapMergesTwoObjects() {
            final ObjectNode first = factory.objectNode();
            first.put("a", 1);

            final ObjectNode second = factory.objectNode();
            second.put("b", 2);

            final DataResult<JsonNode> result = ops.mergeToMap(first, second);

            assertThat(result.isSuccess()).isTrue();
            final JsonNode merged = result.result().orElseThrow();
            assertThat(merged.get("a").asInt()).isEqualTo(1);
            assertThat(merged.get("b").asInt()).isEqualTo(2);
        }

        @Test
        @DisplayName("mergeToMap() preserves original object immutability")
        void mergeToMapPreservesOriginalObjectImmutability() {
            final ObjectNode original = factory.objectNode();
            original.put("key", "original");

            ops.mergeToMap(original, ops.createString("key2"), ops.createString("value2"));

            assertThat(original.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Field Operations")
    class FieldOperations {

        @Test
        @DisplayName("get() returns field value")
        void getReturnsFieldValue() {
            final ObjectNode object = factory.objectNode();
            object.put("name", "Alice");

            final JsonNode result = ops.get(object, "name");

            assertThat(result).isNotNull();
            assertThat(result.asText()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("get() returns null for missing field")
        void getReturnsNullForMissingField() {
            final ObjectNode object = factory.objectNode();

            assertThat(ops.get(object, "missing")).isNull();
        }

        @Test
        @DisplayName("get() returns null for non-object")
        void getReturnsNullForNonObject() {
            assertThat(ops.get(factory.arrayNode(), "key")).isNull();
        }

        @Test
        @DisplayName("set() adds field to object")
        void setAddsFieldToObject() {
            final ObjectNode object = factory.objectNode();
            object.put("existing", "value");

            final JsonNode result = ops.set(object, "new", ops.createString("newValue"));

            assertThat(result.isObject()).isTrue();
            assertThat(result.get("new").asText()).isEqualTo("newValue");
            assertThat(result.get("existing").asText()).isEqualTo("value");
        }

        @Test
        @DisplayName("set() creates object for non-object input")
        void setCreatesObjectForNonObjectInput() {
            final JsonNode result = ops.set(factory.arrayNode(), "key", ops.createString("value"));

            assertThat(result.isObject()).isTrue();
            assertThat(result.get("key").asText()).isEqualTo("value");
        }

        @Test
        @DisplayName("set() preserves original object immutability")
        void setPreservesOriginalObjectImmutability() {
            final ObjectNode original = factory.objectNode();
            original.put("key", "original");

            ops.set(original, "key", ops.createString("modified"));

            assertThat(original.get("key").asText()).isEqualTo("original");
        }

        @Test
        @DisplayName("remove() removes field from object")
        void removeRemovesFieldFromObject() {
            final ObjectNode object = factory.objectNode();
            object.put("keep", "value1");
            object.put("remove", "value2");

            final JsonNode result = ops.remove(object, "remove");

            assertThat(result.isObject()).isTrue();
            assertThat(result.has("keep")).isTrue();
            assertThat(result.has("remove")).isFalse();
        }

        @Test
        @DisplayName("remove() returns input unchanged for non-object")
        void removeReturnsInputUnchangedForNonObject() {
            final ArrayNode array = factory.arrayNode();

            final JsonNode result = ops.remove(array, "key");

            assertThat(result).isSameAs(array);
        }

        @Test
        @DisplayName("remove() preserves original object immutability")
        void removePreservesOriginalObjectImmutability() {
            final ObjectNode original = factory.objectNode();
            original.put("key", "value");

            ops.remove(original, "key");

            assertThat(original.has("key")).isTrue();
        }

        @Test
        @DisplayName("has() returns true for existing field")
        void hasReturnsTrueForExistingField() {
            final ObjectNode object = factory.objectNode();
            object.put("name", "Alice");

            assertThat(ops.has(object, "name")).isTrue();
        }

        @Test
        @DisplayName("has() returns false for missing field")
        void hasReturnsFalseForMissingField() {
            final ObjectNode object = factory.objectNode();

            assertThat(ops.has(object, "missing")).isFalse();
        }

        @Test
        @DisplayName("has() returns false for non-object")
        void hasReturnsFalseForNonObject() {
            assertThat(ops.has(factory.arrayNode(), "key")).isFalse();
        }
    }

    @Nested
    @DisplayName("Conversion")
    class Conversion {

        @Test
        @DisplayName("convertTo() converts primitives from same ops")
        void convertToConvertsPrimitivesFromSameOps() {
            final JsonNode original = ops.createString("hello");

            final JsonNode result = ops.convertTo(ops, original);

            assertThat(result.asText()).isEqualTo("hello");
        }

        @Test
        @DisplayName("convertTo() converts numbers")
        void convertToConvertsNumbers() {
            final JsonNode original = ops.createInt(42);

            final JsonNode result = ops.convertTo(ops, original);

            assertThat(result.asInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("convertTo() converts booleans")
        void convertToConvertsBooleans() {
            final JsonNode original = ops.createBoolean(true);

            final JsonNode result = ops.convertTo(ops, original);

            assertThat(result.asBoolean()).isTrue();
        }

        @Test
        @DisplayName("convertTo() converts lists")
        void convertToConvertsLists() {
            final JsonNode original = ops.createList(Stream.of(
                    ops.createInt(1),
                    ops.createInt(2)
            ));

            final JsonNode result = ops.convertTo(ops, original);

            assertThat(result.isArray()).isTrue();
            assertThat(result.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("convertTo() converts maps")
        void convertToConvertsMaps() {
            final JsonNode original = ops.createMap(Stream.of(
                    Pair.of(ops.createString("key"), ops.createString("value"))
            ));

            final JsonNode result = ops.convertTo(ops, original);

            assertThat(result.isObject()).isTrue();
            assertThat(result.get("key").asText()).isEqualTo("value");
        }

        @Test
        @DisplayName("convertTo() returns empty for unrecognized type")
        void convertToReturnsEmptyForUnrecognizedType() {
            final JsonNode result = ops.convertTo(ops, NullNode.getInstance());

            assertThat(result).isEqualTo(NullNode.getInstance());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles empty string")
        void handlesEmptyString() {
            final JsonNode created = ops.createString("");
            final DataResult<String> read = ops.getStringValue(created);

            assertThat(read.result()).contains("");
        }

        @Test
        @DisplayName("handles zero values")
        void handlesZeroValues() {
            assertThat(ops.createInt(0).asInt()).isZero();
            assertThat(ops.createLong(0L).asLong()).isZero();
            assertThat(ops.createFloat(0f).floatValue()).isZero();
            assertThat(ops.createDouble(0.0).asDouble()).isZero();
        }

        @Test
        @DisplayName("handles negative values")
        void handlesNegativeValues() {
            assertThat(ops.createInt(-42).asInt()).isEqualTo(-42);
            assertThat(ops.createLong(-100L).asLong()).isEqualTo(-100L);
            assertThat(ops.createDouble(-3.14).asDouble()).isEqualTo(-3.14);
        }

        @Test
        @DisplayName("handles special floating point values")
        void handlesSpecialFloatingPointValues() {
            assertThat(ops.createDouble(Double.MAX_VALUE).asDouble()).isEqualTo(Double.MAX_VALUE);
            assertThat(ops.createDouble(Double.MIN_VALUE).asDouble()).isEqualTo(Double.MIN_VALUE);
        }

        @Test
        @DisplayName("handles empty list")
        void handlesEmptyList() {
            final JsonNode list = ops.createList(Stream.empty());

            assertThat(list.isArray()).isTrue();
            assertThat(list.size()).isZero();
        }

        @Test
        @DisplayName("handles empty map")
        void handlesEmptyMap() {
            final JsonNode map = ops.createMap(Stream.empty());

            assertThat(map.isObject()).isTrue();
            assertThat(map.size()).isZero();
        }

        @Test
        @DisplayName("handles nested structures")
        void handlesNestedStructures() {
            final JsonNode nested = ops.createMap(Stream.of(
                    Pair.of(
                            ops.createString("items"),
                            ops.createList(Stream.of(
                                    ops.createMap(Stream.of(
                                            Pair.of(ops.createString("id"), ops.createInt(1))
                                    ))
                            ))
                    )
            ));

            assertThat(nested.isObject()).isTrue();
            assertThat(nested.get("items").isArray()).isTrue();
            assertThat(nested.get("items").get(0).isObject()).isTrue();
        }

        @Test
        @DisplayName("createMap() skips null keys")
        void createMapSkipsNullKeys() {
            final JsonNode map = ops.createMap(Stream.of(
                    Pair.of(null, ops.createString("value")),
                    Pair.of(ops.createString("key"), ops.createString("value"))
            ));

            assertThat(map.size()).isEqualTo(1);
            assertThat(map.has("key")).isTrue();
        }

        @Test
        @DisplayName("createMap() handles null values")
        void createMapHandlesNullValues() {
            final JsonNode map = ops.createMap(Stream.of(
                    Pair.of(ops.createString("key"), null)
            ));

            assertThat(map.get("key")).isEqualTo(NullNode.getInstance());
        }
    }
}
