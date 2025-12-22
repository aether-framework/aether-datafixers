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

package de.splatgames.aether.datafixers.codec.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GsonOps}.
 */
@DisplayName("GsonOps")
class GsonOpsTest {

    private final GsonOps ops = GsonOps.INSTANCE;

    @Nested
    @DisplayName("Singleton Instance")
    class SingletonInstance {

        @Test
        @DisplayName("INSTANCE is not null")
        void instanceIsNotNull() {
            assertThat(GsonOps.INSTANCE).isNotNull();
        }

        @Test
        @DisplayName("INSTANCE is same reference")
        void instanceIsSameReference() {
            assertThat(GsonOps.INSTANCE).isSameAs(ops);
        }

        @Test
        @DisplayName("toString() returns GsonOps")
        void toStringReturnsGsonOps() {
            assertThat(ops.toString()).isEqualTo("GsonOps");
        }
    }

    @Nested
    @DisplayName("Empty Values")
    class EmptyValues {

        @Test
        @DisplayName("empty() returns JsonNull")
        void emptyReturnsJsonNull() {
            assertThat(ops.empty()).isEqualTo(JsonNull.INSTANCE);
        }

        @Test
        @DisplayName("emptyList() returns empty JsonArray")
        void emptyListReturnsEmptyJsonArray() {
            final JsonElement result = ops.emptyList();

            assertThat(result).isInstanceOf(JsonArray.class);
            assertThat(result.getAsJsonArray()).isEmpty();
        }

        @Test
        @DisplayName("emptyMap() returns empty JsonObject")
        void emptyMapReturnsEmptyJsonObject() {
            final JsonElement result = ops.emptyMap();

            assertThat(result).isInstanceOf(JsonObject.class);
            assertThat(result.getAsJsonObject().size()).isZero();
        }
    }

    @Nested
    @DisplayName("Type Checks")
    class TypeChecks {

        @Test
        @DisplayName("isMap() returns true for JsonObject")
        void isMapReturnsTrueForJsonObject() {
            assertThat(ops.isMap(new JsonObject())).isTrue();
        }

        @Test
        @DisplayName("isMap() returns false for non-object")
        void isMapReturnsFalseForNonObject() {
            assertThat(ops.isMap(new JsonArray())).isFalse();
            assertThat(ops.isMap(new JsonPrimitive("test"))).isFalse();
            assertThat(ops.isMap(JsonNull.INSTANCE)).isFalse();
        }

        @Test
        @DisplayName("isList() returns true for JsonArray")
        void isListReturnsTrueForJsonArray() {
            assertThat(ops.isList(new JsonArray())).isTrue();
        }

        @Test
        @DisplayName("isList() returns false for non-array")
        void isListReturnsFalseForNonArray() {
            assertThat(ops.isList(new JsonObject())).isFalse();
            assertThat(ops.isList(new JsonPrimitive("test"))).isFalse();
            assertThat(ops.isList(JsonNull.INSTANCE)).isFalse();
        }

        @Test
        @DisplayName("isString() returns true for string primitive")
        void isStringReturnsTrueForStringPrimitive() {
            assertThat(ops.isString(new JsonPrimitive("test"))).isTrue();
        }

        @Test
        @DisplayName("isString() returns false for non-string")
        void isStringReturnsFalseForNonString() {
            assertThat(ops.isString(new JsonPrimitive(42))).isFalse();
            assertThat(ops.isString(new JsonPrimitive(true))).isFalse();
            assertThat(ops.isString(new JsonObject())).isFalse();
        }

        @Test
        @DisplayName("isNumber() returns true for number primitive")
        void isNumberReturnsTrueForNumberPrimitive() {
            assertThat(ops.isNumber(new JsonPrimitive(42))).isTrue();
            assertThat(ops.isNumber(new JsonPrimitive(3.14))).isTrue();
        }

        @Test
        @DisplayName("isNumber() returns false for non-number")
        void isNumberReturnsFalseForNonNumber() {
            assertThat(ops.isNumber(new JsonPrimitive("test"))).isFalse();
            assertThat(ops.isNumber(new JsonPrimitive(true))).isFalse();
            assertThat(ops.isNumber(new JsonObject())).isFalse();
        }

        @Test
        @DisplayName("isBoolean() returns true for boolean primitive")
        void isBooleanReturnsTrueForBooleanPrimitive() {
            assertThat(ops.isBoolean(new JsonPrimitive(true))).isTrue();
            assertThat(ops.isBoolean(new JsonPrimitive(false))).isTrue();
        }

        @Test
        @DisplayName("isBoolean() returns false for non-boolean")
        void isBooleanReturnsFalseForNonBoolean() {
            assertThat(ops.isBoolean(new JsonPrimitive("test"))).isFalse();
            assertThat(ops.isBoolean(new JsonPrimitive(42))).isFalse();
            assertThat(ops.isBoolean(new JsonObject())).isFalse();
        }
    }

    @Nested
    @DisplayName("Primitive Creation")
    class PrimitiveCreation {

        @Test
        @DisplayName("createString() creates string primitive")
        void createStringCreatesStringPrimitive() {
            final JsonElement result = ops.createString("hello");

            assertThat(result.isJsonPrimitive()).isTrue();
            assertThat(result.getAsString()).isEqualTo("hello");
        }

        @Test
        @DisplayName("createInt() creates int primitive")
        void createIntCreatesIntPrimitive() {
            final JsonElement result = ops.createInt(42);

            assertThat(result.isJsonPrimitive()).isTrue();
            assertThat(result.getAsInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("createLong() creates long primitive")
        void createLongCreatesLongPrimitive() {
            final JsonElement result = ops.createLong(Long.MAX_VALUE);

            assertThat(result.isJsonPrimitive()).isTrue();
            assertThat(result.getAsLong()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("createFloat() creates float primitive")
        void createFloatCreatesFloatPrimitive() {
            final JsonElement result = ops.createFloat(3.14f);

            assertThat(result.isJsonPrimitive()).isTrue();
            assertThat(result.getAsFloat()).isEqualTo(3.14f);
        }

        @Test
        @DisplayName("createDouble() creates double primitive")
        void createDoubleCreatesDoublePrimitive() {
            final JsonElement result = ops.createDouble(3.14159);

            assertThat(result.isJsonPrimitive()).isTrue();
            assertThat(result.getAsDouble()).isEqualTo(3.14159);
        }

        @Test
        @DisplayName("createByte() creates byte primitive")
        void createByteCreatesBytePrimitive() {
            final JsonElement result = ops.createByte((byte) 127);

            assertThat(result.isJsonPrimitive()).isTrue();
            assertThat(result.getAsByte()).isEqualTo((byte) 127);
        }

        @Test
        @DisplayName("createShort() creates short primitive")
        void createShortCreatesShortPrimitive() {
            final JsonElement result = ops.createShort((short) 32767);

            assertThat(result.isJsonPrimitive()).isTrue();
            assertThat(result.getAsShort()).isEqualTo((short) 32767);
        }

        @Test
        @DisplayName("createBoolean() creates boolean primitive")
        void createBooleanCreatesBooleanPrimitive() {
            assertThat(ops.createBoolean(true).getAsBoolean()).isTrue();
            assertThat(ops.createBoolean(false).getAsBoolean()).isFalse();
        }

        @Test
        @DisplayName("createNumeric() creates number primitive")
        void createNumericCreatesNumberPrimitive() {
            assertThat(ops.createNumeric(42).getAsInt()).isEqualTo(42);
            assertThat(ops.createNumeric(3.14).getAsDouble()).isEqualTo(3.14);
        }
    }

    @Nested
    @DisplayName("Primitive Reading")
    class PrimitiveReading {

        @Test
        @DisplayName("getStringValue() returns string from primitive")
        void getStringValueReturnsStringFromPrimitive() {
            final DataResult<String> result = ops.getStringValue(new JsonPrimitive("hello"));

            assertThat(result.result()).contains("hello");
        }

        @Test
        @DisplayName("getStringValue() returns error for non-string")
        void getStringValueReturnsErrorForNonString() {
            assertThat(ops.getStringValue(new JsonPrimitive(42)).isError()).isTrue();
            assertThat(ops.getStringValue(new JsonObject()).isError()).isTrue();
        }

        @Test
        @DisplayName("getNumberValue() returns number from primitive")
        void getNumberValueReturnsNumberFromPrimitive() {
            final DataResult<Number> result = ops.getNumberValue(new JsonPrimitive(42));

            assertThat(result.result()).isPresent();
            assertThat(result.result().get().intValue()).isEqualTo(42);
        }

        @Test
        @DisplayName("getNumberValue() returns error for non-number")
        void getNumberValueReturnsErrorForNonNumber() {
            assertThat(ops.getNumberValue(new JsonPrimitive("test")).isError()).isTrue();
            assertThat(ops.getNumberValue(new JsonObject()).isError()).isTrue();
        }

        @Test
        @DisplayName("getBooleanValue() returns boolean from primitive")
        void getBooleanValueReturnsBooleanFromPrimitive() {
            assertThat(ops.getBooleanValue(new JsonPrimitive(true)).result()).contains(true);
            assertThat(ops.getBooleanValue(new JsonPrimitive(false)).result()).contains(false);
        }

        @Test
        @DisplayName("getBooleanValue() returns error for non-boolean")
        void getBooleanValueReturnsErrorForNonBoolean() {
            assertThat(ops.getBooleanValue(new JsonPrimitive("test")).isError()).isTrue();
            assertThat(ops.getBooleanValue(new JsonPrimitive(42)).isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("List Operations")
    class ListOperations {

        @Test
        @DisplayName("createList() creates JsonArray from stream")
        void createListCreatesJsonArrayFromStream() {
            final JsonElement result = ops.createList(Stream.of(
                    ops.createInt(1),
                    ops.createInt(2),
                    ops.createInt(3)
            ));

            assertThat(result.isJsonArray()).isTrue();
            final JsonArray array = result.getAsJsonArray();
            assertThat(array.size()).isEqualTo(3);
            assertThat(array.get(0).getAsInt()).isEqualTo(1);
            assertThat(array.get(1).getAsInt()).isEqualTo(2);
            assertThat(array.get(2).getAsInt()).isEqualTo(3);
        }

        @Test
        @DisplayName("getList() returns stream of elements")
        void getListReturnsStreamOfElements() {
            final JsonArray array = new JsonArray();
            array.add(1);
            array.add(2);
            array.add(3);

            final DataResult<Stream<JsonElement>> result = ops.getList(array);

            assertThat(result.isSuccess()).isTrue();
            final List<JsonElement> list = result.result().orElseThrow().toList();
            assertThat(list).hasSize(3);
        }

        @Test
        @DisplayName("getList() returns error for non-array")
        void getListReturnsErrorForNonArray() {
            assertThat(ops.getList(new JsonObject()).isError()).isTrue();
            assertThat(ops.getList(new JsonPrimitive("test")).isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToList() appends to existing array")
        void mergeToListAppendsToExistingArray() {
            final JsonArray array = new JsonArray();
            array.add(1);
            array.add(2);

            final DataResult<JsonElement> result = ops.mergeToList(array, ops.createInt(3));

            assertThat(result.isSuccess()).isTrue();
            final JsonArray merged = result.result().orElseThrow().getAsJsonArray();
            assertThat(merged.size()).isEqualTo(3);
            assertThat(merged.get(2).getAsInt()).isEqualTo(3);
        }

        @Test
        @DisplayName("mergeToList() creates new array from null")
        void mergeToListCreatesNewArrayFromNull() {
            final DataResult<JsonElement> result = ops.mergeToList(JsonNull.INSTANCE, ops.createInt(1));

            assertThat(result.isSuccess()).isTrue();
            final JsonArray array = result.result().orElseThrow().getAsJsonArray();
            assertThat(array.size()).isEqualTo(1);
            assertThat(array.get(0).getAsInt()).isEqualTo(1);
        }

        @Test
        @DisplayName("mergeToList() returns error for non-array input")
        void mergeToListReturnsErrorForNonArrayInput() {
            final DataResult<JsonElement> result = ops.mergeToList(new JsonObject(), ops.createInt(1));

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToList() preserves original array immutability")
        void mergeToListPreservesOriginalArrayImmutability() {
            final JsonArray original = new JsonArray();
            original.add(1);

            ops.mergeToList(original, ops.createInt(2));

            assertThat(original.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Map Operations")
    class MapOperations {

        @Test
        @DisplayName("createMap() creates JsonObject from stream")
        void createMapCreatesJsonObjectFromStream() {
            final JsonElement result = ops.createMap(Stream.of(
                    Pair.of(ops.createString("name"), ops.createString("Alice")),
                    Pair.of(ops.createString("age"), ops.createInt(30))
            ));

            assertThat(result.isJsonObject()).isTrue();
            final JsonObject object = result.getAsJsonObject();
            assertThat(object.get("name").getAsString()).isEqualTo("Alice");
            assertThat(object.get("age").getAsInt()).isEqualTo(30);
        }

        @Test
        @DisplayName("getMapEntries() returns stream of entries")
        void getMapEntriesReturnsStreamOfEntries() {
            final JsonObject object = new JsonObject();
            object.addProperty("name", "Bob");
            object.addProperty("age", 25);

            final DataResult<Stream<Pair<JsonElement, JsonElement>>> result = ops.getMapEntries(object);

            assertThat(result.isSuccess()).isTrue();
            final List<Pair<JsonElement, JsonElement>> entries = result.result().orElseThrow().toList();
            assertThat(entries).hasSize(2);
        }

        @Test
        @DisplayName("getMapEntries() returns error for non-object")
        void getMapEntriesReturnsErrorForNonObject() {
            assertThat(ops.getMapEntries(new JsonArray()).isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToMap() adds key-value to existing object")
        void mergeToMapAddsKeyValueToExistingObject() {
            final JsonObject object = new JsonObject();
            object.addProperty("name", "Alice");

            final DataResult<JsonElement> result = ops.mergeToMap(
                    object,
                    ops.createString("age"),
                    ops.createInt(30)
            );

            assertThat(result.isSuccess()).isTrue();
            final JsonObject merged = result.result().orElseThrow().getAsJsonObject();
            assertThat(merged.get("name").getAsString()).isEqualTo("Alice");
            assertThat(merged.get("age").getAsInt()).isEqualTo(30);
        }

        @Test
        @DisplayName("mergeToMap() creates new object from null")
        void mergeToMapCreatesNewObjectFromNull() {
            final DataResult<JsonElement> result = ops.mergeToMap(
                    JsonNull.INSTANCE,
                    ops.createString("key"),
                    ops.createString("value")
            );

            assertThat(result.isSuccess()).isTrue();
            final JsonObject object = result.result().orElseThrow().getAsJsonObject();
            assertThat(object.get("key").getAsString()).isEqualTo("value");
        }

        @Test
        @DisplayName("mergeToMap() returns error for non-string key")
        void mergeToMapReturnsErrorForNonStringKey() {
            final DataResult<JsonElement> result = ops.mergeToMap(
                    new JsonObject(),
                    ops.createInt(42),
                    ops.createString("value")
            );

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToMap() merges two objects")
        void mergeToMapMergesTwoObjects() {
            final JsonObject first = new JsonObject();
            first.addProperty("a", 1);

            final JsonObject second = new JsonObject();
            second.addProperty("b", 2);

            final DataResult<JsonElement> result = ops.mergeToMap(first, second);

            assertThat(result.isSuccess()).isTrue();
            final JsonObject merged = result.result().orElseThrow().getAsJsonObject();
            assertThat(merged.get("a").getAsInt()).isEqualTo(1);
            assertThat(merged.get("b").getAsInt()).isEqualTo(2);
        }

        @Test
        @DisplayName("mergeToMap() preserves original object immutability")
        void mergeToMapPreservesOriginalObjectImmutability() {
            final JsonObject original = new JsonObject();
            original.addProperty("key", "original");

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
            final JsonObject object = new JsonObject();
            object.addProperty("name", "Alice");

            final JsonElement result = ops.get(object, "name");

            assertThat(result).isNotNull();
            assertThat(result.getAsString()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("get() returns null for missing field")
        void getReturnsNullForMissingField() {
            final JsonObject object = new JsonObject();

            assertThat(ops.get(object, "missing")).isNull();
        }

        @Test
        @DisplayName("get() returns null for non-object")
        void getReturnsNullForNonObject() {
            assertThat(ops.get(new JsonArray(), "key")).isNull();
        }

        @Test
        @DisplayName("set() adds field to object")
        void setAddsFieldToObject() {
            final JsonObject object = new JsonObject();
            object.addProperty("existing", "value");

            final JsonElement result = ops.set(object, "new", ops.createString("newValue"));

            assertThat(result.isJsonObject()).isTrue();
            assertThat(result.getAsJsonObject().get("new").getAsString()).isEqualTo("newValue");
            assertThat(result.getAsJsonObject().get("existing").getAsString()).isEqualTo("value");
        }

        @Test
        @DisplayName("set() creates object for non-object input")
        void setCreatesObjectForNonObjectInput() {
            final JsonElement result = ops.set(new JsonArray(), "key", ops.createString("value"));

            assertThat(result.isJsonObject()).isTrue();
            assertThat(result.getAsJsonObject().get("key").getAsString()).isEqualTo("value");
        }

        @Test
        @DisplayName("set() preserves original object immutability")
        void setPreservesOriginalObjectImmutability() {
            final JsonObject original = new JsonObject();
            original.addProperty("key", "original");

            ops.set(original, "key", ops.createString("modified"));

            assertThat(original.get("key").getAsString()).isEqualTo("original");
        }

        @Test
        @DisplayName("remove() removes field from object")
        void removeRemovesFieldFromObject() {
            final JsonObject object = new JsonObject();
            object.addProperty("keep", "value1");
            object.addProperty("remove", "value2");

            final JsonElement result = ops.remove(object, "remove");

            assertThat(result.isJsonObject()).isTrue();
            assertThat(result.getAsJsonObject().has("keep")).isTrue();
            assertThat(result.getAsJsonObject().has("remove")).isFalse();
        }

        @Test
        @DisplayName("remove() returns input unchanged for non-object")
        void removeReturnsInputUnchangedForNonObject() {
            final JsonArray array = new JsonArray();

            final JsonElement result = ops.remove(array, "key");

            assertThat(result).isSameAs(array);
        }

        @Test
        @DisplayName("remove() preserves original object immutability")
        void removePreservesOriginalObjectImmutability() {
            final JsonObject original = new JsonObject();
            original.addProperty("key", "value");

            ops.remove(original, "key");

            assertThat(original.has("key")).isTrue();
        }

        @Test
        @DisplayName("has() returns true for existing field")
        void hasReturnsTrueForExistingField() {
            final JsonObject object = new JsonObject();
            object.addProperty("name", "Alice");

            assertThat(ops.has(object, "name")).isTrue();
        }

        @Test
        @DisplayName("has() returns false for missing field")
        void hasReturnsFalseForMissingField() {
            final JsonObject object = new JsonObject();

            assertThat(ops.has(object, "missing")).isFalse();
        }

        @Test
        @DisplayName("has() returns false for non-object")
        void hasReturnsFalseForNonObject() {
            assertThat(ops.has(new JsonArray(), "key")).isFalse();
        }
    }

    @Nested
    @DisplayName("Conversion")
    class Conversion {

        @Test
        @DisplayName("convertTo() converts primitives from same ops")
        void convertToConvertsPrimitivesFromSameOps() {
            final JsonElement original = ops.createString("hello");

            final JsonElement result = ops.convertTo(ops, original);

            assertThat(result.getAsString()).isEqualTo("hello");
        }

        @Test
        @DisplayName("convertTo() converts numbers")
        void convertToConvertsNumbers() {
            final JsonElement original = ops.createInt(42);

            final JsonElement result = ops.convertTo(ops, original);

            assertThat(result.getAsInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("convertTo() converts booleans")
        void convertToConvertsBooleans() {
            final JsonElement original = ops.createBoolean(true);

            final JsonElement result = ops.convertTo(ops, original);

            assertThat(result.getAsBoolean()).isTrue();
        }

        @Test
        @DisplayName("convertTo() converts lists")
        void convertToConvertsLists() {
            final JsonElement original = ops.createList(Stream.of(
                    ops.createInt(1),
                    ops.createInt(2)
            ));

            final JsonElement result = ops.convertTo(ops, original);

            assertThat(result.isJsonArray()).isTrue();
            assertThat(result.getAsJsonArray().size()).isEqualTo(2);
        }

        @Test
        @DisplayName("convertTo() converts maps")
        void convertToConvertsMaps() {
            final JsonElement original = ops.createMap(Stream.of(
                    Pair.of(ops.createString("key"), ops.createString("value"))
            ));

            final JsonElement result = ops.convertTo(ops, original);

            assertThat(result.isJsonObject()).isTrue();
            assertThat(result.getAsJsonObject().get("key").getAsString()).isEqualTo("value");
        }

        @Test
        @DisplayName("convertTo() returns empty for unrecognized type")
        void convertToReturnsEmptyForUnrecognizedType() {
            final JsonElement result = ops.convertTo(ops, JsonNull.INSTANCE);

            assertThat(result).isEqualTo(JsonNull.INSTANCE);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles empty string")
        void handlesEmptyString() {
            final JsonElement created = ops.createString("");
            final DataResult<String> read = ops.getStringValue(created);

            assertThat(read.result()).contains("");
        }

        @Test
        @DisplayName("handles zero values")
        void handlesZeroValues() {
            assertThat(ops.createInt(0).getAsInt()).isZero();
            assertThat(ops.createLong(0L).getAsLong()).isZero();
            assertThat(ops.createFloat(0f).getAsFloat()).isZero();
            assertThat(ops.createDouble(0.0).getAsDouble()).isZero();
        }

        @Test
        @DisplayName("handles negative values")
        void handlesNegativeValues() {
            assertThat(ops.createInt(-42).getAsInt()).isEqualTo(-42);
            assertThat(ops.createLong(-100L).getAsLong()).isEqualTo(-100L);
            assertThat(ops.createDouble(-3.14).getAsDouble()).isEqualTo(-3.14);
        }

        @Test
        @DisplayName("handles special floating point values")
        void handlesSpecialFloatingPointValues() {
            assertThat(ops.createDouble(Double.MAX_VALUE).getAsDouble()).isEqualTo(Double.MAX_VALUE);
            assertThat(ops.createDouble(Double.MIN_VALUE).getAsDouble()).isEqualTo(Double.MIN_VALUE);
        }

        @Test
        @DisplayName("handles empty list")
        void handlesEmptyList() {
            final JsonElement list = ops.createList(Stream.empty());

            assertThat(list.isJsonArray()).isTrue();
            assertThat(list.getAsJsonArray()).isEmpty();
        }

        @Test
        @DisplayName("handles empty map")
        void handlesEmptyMap() {
            final JsonElement map = ops.createMap(Stream.empty());

            assertThat(map.isJsonObject()).isTrue();
            assertThat(map.getAsJsonObject().size()).isZero();
        }

        @Test
        @DisplayName("handles nested structures")
        void handlesNestedStructures() {
            final JsonElement nested = ops.createMap(Stream.of(
                    Pair.of(
                            ops.createString("items"),
                            ops.createList(Stream.of(
                                    ops.createMap(Stream.of(
                                            Pair.of(ops.createString("id"), ops.createInt(1))
                                    ))
                            ))
                    )
            ));

            assertThat(nested.isJsonObject()).isTrue();
            final JsonObject obj = nested.getAsJsonObject();
            assertThat(obj.get("items").isJsonArray()).isTrue();
            assertThat(obj.get("items").getAsJsonArray().get(0).isJsonObject()).isTrue();
        }

        @Test
        @DisplayName("createMap() skips null keys")
        void createMapSkipsNullKeys() {
            final JsonElement map = ops.createMap(Stream.of(
                    Pair.of(null, ops.createString("value")),
                    Pair.of(ops.createString("key"), ops.createString("value"))
            ));

            assertThat(map.getAsJsonObject().size()).isEqualTo(1);
            assertThat(map.getAsJsonObject().has("key")).isTrue();
        }

        @Test
        @DisplayName("createMap() handles null values")
        void createMapHandlesNullValues() {
            final JsonElement map = ops.createMap(Stream.of(
                    Pair.of(ops.createString("key"), null)
            ));

            assertThat(map.getAsJsonObject().get("key")).isEqualTo(JsonNull.INSTANCE);
        }
    }
}
