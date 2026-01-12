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

package de.splatgames.aether.datafixers.codec.yaml.snakeyaml;

import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SnakeYamlOps}.
 */
@DisplayName("SnakeYamlOps")
class SnakeYamlOpsTest {

    private final SnakeYamlOps ops = SnakeYamlOps.INSTANCE;

    @Nested
    @DisplayName("Singleton Instance")
    class SingletonInstance {

        @Test
        @DisplayName("INSTANCE is not null")
        void instanceIsNotNull() {
            assertThat(SnakeYamlOps.INSTANCE).isNotNull();
        }

        @Test
        @DisplayName("INSTANCE is same reference")
        void instanceIsSameReference() {
            assertThat(SnakeYamlOps.INSTANCE).isSameAs(ops);
        }

        @Test
        @DisplayName("toString() returns SnakeYamlOps")
        void toStringReturnsSnakeYamlOps() {
            assertThat(ops.toString()).isEqualTo("SnakeYamlOps");
        }
    }

    @Nested
    @DisplayName("Empty Values")
    class EmptyValues {

        @Test
        @DisplayName("empty() returns NULL sentinel")
        void emptyReturnsNullSentinel() {
            assertThat(ops.empty()).isSameAs(SnakeYamlOps.NULL);
            assertThat(SnakeYamlOps.isNull(ops.empty())).isTrue();
        }

        @Test
        @DisplayName("emptyList() returns empty ArrayList")
        void emptyListReturnsEmptyArrayList() {
            final Object result = ops.emptyList();

            assertThat(result).isInstanceOf(List.class);
            assertThat((List<?>) result).isEmpty();
        }

        @Test
        @DisplayName("emptyMap() returns empty LinkedHashMap")
        void emptyMapReturnsEmptyLinkedHashMap() {
            final Object result = ops.emptyMap();

            assertThat(result).isInstanceOf(Map.class);
            assertThat((Map<?, ?>) result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Null Sentinel Utilities")
    class NullSentinelUtilities {

        @Test
        @DisplayName("isNull() returns true for NULL sentinel")
        void isNullReturnsTrueForSentinel() {
            assertThat(SnakeYamlOps.isNull(SnakeYamlOps.NULL)).isTrue();
            assertThat(SnakeYamlOps.isNull(ops.empty())).isTrue();
        }

        @Test
        @DisplayName("isNull() returns false for other values")
        void isNullReturnsFalseForOtherValues() {
            assertThat(SnakeYamlOps.isNull(null)).isFalse();
            assertThat(SnakeYamlOps.isNull("test")).isFalse();
            assertThat(SnakeYamlOps.isNull(42)).isFalse();
            assertThat(SnakeYamlOps.isNull(new LinkedHashMap<>())).isFalse();
        }

        @Test
        @DisplayName("wrap() converts null to sentinel")
        void wrapConvertsNullToSentinel() {
            assertThat(SnakeYamlOps.wrap(null)).isSameAs(SnakeYamlOps.NULL);
        }

        @Test
        @DisplayName("wrap() preserves non-null values")
        void wrapPreservesNonNullValues() {
            assertThat(SnakeYamlOps.wrap("test")).isEqualTo("test");
            assertThat(SnakeYamlOps.wrap(42)).isEqualTo(42);
        }

        @Test
        @DisplayName("wrap() recursively converts nulls in maps")
        void wrapRecursivelyConvertsMaps() {
            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Alice");
            input.put("nickname", null);

            final Object wrapped = SnakeYamlOps.wrap(input);

            assertThat(wrapped).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            final Map<String, Object> result = (Map<String, Object>) wrapped;
            assertThat(result.get("name")).isEqualTo("Alice");
            assertThat(result.get("nickname")).isSameAs(SnakeYamlOps.NULL);
        }

        @Test
        @DisplayName("wrap() recursively converts nulls in lists")
        void wrapRecursivelyConvertsLists() {
            final List<Object> input = new ArrayList<>();
            input.add("first");
            input.add(null);
            input.add("third");

            final Object wrapped = SnakeYamlOps.wrap(input);

            assertThat(wrapped).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            final List<Object> result = (List<Object>) wrapped;
            assertThat(result.get(0)).isEqualTo("first");
            assertThat(result.get(1)).isSameAs(SnakeYamlOps.NULL);
            assertThat(result.get(2)).isEqualTo("third");
        }

        @Test
        @DisplayName("unwrap() converts sentinel to null")
        void unwrapConvertsSentinelToNull() {
            assertThat(SnakeYamlOps.unwrap(SnakeYamlOps.NULL)).isNull();
        }

        @Test
        @DisplayName("unwrap() preserves non-sentinel values")
        void unwrapPreservesNonSentinelValues() {
            assertThat(SnakeYamlOps.unwrap("test")).isEqualTo("test");
            assertThat(SnakeYamlOps.unwrap(42)).isEqualTo(42);
            assertThat(SnakeYamlOps.unwrap(null)).isNull();
        }

        @Test
        @DisplayName("unwrap() recursively converts sentinels in maps")
        void unwrapRecursivelyConvertsMaps() {
            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Alice");
            input.put("nickname", SnakeYamlOps.NULL);

            final Object unwrapped = SnakeYamlOps.unwrap(input);

            assertThat(unwrapped).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            final Map<String, Object> result = (Map<String, Object>) unwrapped;
            assertThat(result.get("name")).isEqualTo("Alice");
            assertThat(result.get("nickname")).isNull();
        }

        @Test
        @DisplayName("unwrap() recursively converts sentinels in lists")
        void unwrapRecursivelyConvertsLists() {
            final List<Object> input = new ArrayList<>();
            input.add("first");
            input.add(SnakeYamlOps.NULL);
            input.add("third");

            final Object unwrapped = SnakeYamlOps.unwrap(input);

            assertThat(unwrapped).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            final List<Object> result = (List<Object>) unwrapped;
            assertThat(result.get(0)).isEqualTo("first");
            assertThat(result.get(1)).isNull();
            assertThat(result.get(2)).isEqualTo("third");
        }

        @Test
        @DisplayName("wrap() and unwrap() are inverse operations")
        void wrapAndUnwrapAreInverse() {
            final Map<String, Object> original = new LinkedHashMap<>();
            original.put("name", "Alice");
            original.put("nickname", null);
            original.put("nested", new LinkedHashMap<>() {{
                put("value", null);
            }});

            final Object wrapped = SnakeYamlOps.wrap(original);
            final Object unwrapped = SnakeYamlOps.unwrap(wrapped);

            assertThat(unwrapped).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("Type Checks")
    class TypeChecks {

        @Test
        @DisplayName("isMap() returns true for Map")
        void isMapReturnsTrueForMap() {
            assertThat(ops.isMap(new LinkedHashMap<>())).isTrue();
        }

        @Test
        @DisplayName("isMap() returns false for non-map")
        void isMapReturnsFalseForNonMap() {
            assertThat(ops.isMap(new ArrayList<>())).isFalse();
            assertThat(ops.isMap("test")).isFalse();
        }

        @Test
        @DisplayName("isMap() throws NullPointerException for null")
        void isMapThrowsNullPointerExceptionForNull() {
            assertThatThrownBy(() -> ops.isMap(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("isList() returns true for List")
        void isListReturnsTrueForList() {
            assertThat(ops.isList(new ArrayList<>())).isTrue();
        }

        @Test
        @DisplayName("isList() returns false for non-list")
        void isListReturnsFalseForNonList() {
            assertThat(ops.isList(new LinkedHashMap<>())).isFalse();
            assertThat(ops.isList("test")).isFalse();
        }

        @Test
        @DisplayName("isList() throws NullPointerException for null")
        void isListThrowsNullPointerExceptionForNull() {
            assertThatThrownBy(() -> ops.isList(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("isString() returns true for String")
        void isStringReturnsTrueForString() {
            assertThat(ops.isString("test")).isTrue();
        }

        @Test
        @DisplayName("isString() returns false for non-string")
        void isStringReturnsFalseForNonString() {
            assertThat(ops.isString(42)).isFalse();
            assertThat(ops.isString(true)).isFalse();
            assertThat(ops.isString(new LinkedHashMap<>())).isFalse();
        }

        @Test
        @DisplayName("isNumber() returns true for Number")
        void isNumberReturnsTrueForNumber() {
            assertThat(ops.isNumber(42)).isTrue();
            assertThat(ops.isNumber(3.14)).isTrue();
            assertThat(ops.isNumber(100L)).isTrue();
        }

        @Test
        @DisplayName("isNumber() returns false for non-number")
        void isNumberReturnsFalseForNonNumber() {
            assertThat(ops.isNumber("test")).isFalse();
            assertThat(ops.isNumber(true)).isFalse();
            assertThat(ops.isNumber(new LinkedHashMap<>())).isFalse();
        }

        @Test
        @DisplayName("isBoolean() returns true for Boolean")
        void isBooleanReturnsTrueForBoolean() {
            assertThat(ops.isBoolean(true)).isTrue();
            assertThat(ops.isBoolean(false)).isTrue();
        }

        @Test
        @DisplayName("isBoolean() returns false for non-boolean")
        void isBooleanReturnsFalseForNonBoolean() {
            assertThat(ops.isBoolean("test")).isFalse();
            assertThat(ops.isBoolean(42)).isFalse();
            assertThat(ops.isBoolean(new LinkedHashMap<>())).isFalse();
        }
    }

    @Nested
    @DisplayName("Primitive Creation")
    class PrimitiveCreation {

        @Test
        @DisplayName("createString() creates String")
        void createStringCreatesString() {
            final Object result = ops.createString("hello");

            assertThat(result).isInstanceOf(String.class);
            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("createInt() creates Integer")
        void createIntCreatesInteger() {
            final Object result = ops.createInt(42);

            assertThat(result).isInstanceOf(Integer.class);
            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("createLong() creates Long")
        void createLongCreatesLong() {
            final Object result = ops.createLong(Long.MAX_VALUE);

            assertThat(result).isInstanceOf(Long.class);
            assertThat(result).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("createFloat() creates Float")
        void createFloatCreatesFloat() {
            final Object result = ops.createFloat(3.14f);

            assertThat(result).isInstanceOf(Float.class);
            assertThat(result).isEqualTo(3.14f);
        }

        @Test
        @DisplayName("createDouble() creates Double")
        void createDoubleCreatesDouble() {
            final Object result = ops.createDouble(3.14159);

            assertThat(result).isInstanceOf(Double.class);
            assertThat(result).isEqualTo(3.14159);
        }

        @Test
        @DisplayName("createByte() creates Byte")
        void createByteCreatesByte() {
            final Object result = ops.createByte((byte) 127);

            assertThat(result).isInstanceOf(Byte.class);
            assertThat(result).isEqualTo((byte) 127);
        }

        @Test
        @DisplayName("createShort() creates Short")
        void createShortCreatesShort() {
            final Object result = ops.createShort((short) 32767);

            assertThat(result).isInstanceOf(Short.class);
            assertThat(result).isEqualTo((short) 32767);
        }

        @Test
        @DisplayName("createBoolean() creates Boolean")
        void createBooleanCreatesBoolean() {
            assertThat(ops.createBoolean(true)).isEqualTo(true);
            assertThat(ops.createBoolean(false)).isEqualTo(false);
        }

        @Test
        @DisplayName("createNumeric() creates appropriate type")
        void createNumericCreatesAppropriateType() {
            assertThat(ops.createNumeric(42)).isInstanceOf(Integer.class);
            assertThat(ops.createNumeric(42L)).isInstanceOf(Long.class);
            assertThat(ops.createNumeric(3.14f)).isInstanceOf(Float.class);
            assertThat(ops.createNumeric(3.14)).isInstanceOf(Double.class);
            assertThat(ops.createNumeric((short) 10)).isInstanceOf(Short.class);
            assertThat(ops.createNumeric((byte) 5)).isInstanceOf(Byte.class);
        }
    }

    @Nested
    @DisplayName("Primitive Reading")
    class PrimitiveReading {

        @Test
        @DisplayName("getStringValue() returns string from String")
        void getStringValueReturnsStringFromString() {
            final DataResult<String> result = ops.getStringValue("hello");

            assertThat(result.result()).contains("hello");
        }

        @Test
        @DisplayName("getStringValue() returns error for non-string")
        void getStringValueReturnsErrorForNonString() {
            assertThat(ops.getStringValue(42).isError()).isTrue();
            assertThat(ops.getStringValue(new LinkedHashMap<>()).isError()).isTrue();
        }

        @Test
        @DisplayName("getNumberValue() returns number from Number")
        void getNumberValueReturnsNumberFromNumber() {
            final DataResult<Number> result = ops.getNumberValue(42);

            assertThat(result.result()).isPresent();
            assertThat(result.result().get().intValue()).isEqualTo(42);
        }

        @Test
        @DisplayName("getNumberValue() returns error for non-number")
        void getNumberValueReturnsErrorForNonNumber() {
            assertThat(ops.getNumberValue("test").isError()).isTrue();
            assertThat(ops.getNumberValue(new LinkedHashMap<>()).isError()).isTrue();
        }

        @Test
        @DisplayName("getBooleanValue() returns boolean from Boolean")
        void getBooleanValueReturnsBooleanFromBoolean() {
            assertThat(ops.getBooleanValue(true).result()).contains(true);
            assertThat(ops.getBooleanValue(false).result()).contains(false);
        }

        @Test
        @DisplayName("getBooleanValue() returns error for non-boolean")
        void getBooleanValueReturnsErrorForNonBoolean() {
            assertThat(ops.getBooleanValue("test").isError()).isTrue();
            assertThat(ops.getBooleanValue(42).isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("List Operations")
    class ListOperations {

        @Test
        @DisplayName("createList() creates List from stream")
        void createListCreatesListFromStream() {
            final Object result = ops.createList(Stream.of(
                    ops.createInt(1),
                    ops.createInt(2),
                    ops.createInt(3)
            ));

            assertThat(result).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            final List<Object> list = (List<Object>) result;
            assertThat(list).hasSize(3);
            assertThat(list.get(0)).isEqualTo(1);
            assertThat(list.get(1)).isEqualTo(2);
            assertThat(list.get(2)).isEqualTo(3);
        }

        @Test
        @DisplayName("getList() returns stream of elements")
        void getListReturnsStreamOfElements() {
            final List<Object> list = new ArrayList<>();
            list.add(1);
            list.add(2);
            list.add(3);

            final DataResult<Stream<Object>> result = ops.getList(list);

            assertThat(result.isSuccess()).isTrue();
            final List<Object> resultList = result.result().orElseThrow().toList();
            assertThat(resultList).hasSize(3);
        }

        @Test
        @DisplayName("getList() returns error for non-list")
        void getListReturnsErrorForNonList() {
            assertThat(ops.getList(new LinkedHashMap<>()).isError()).isTrue();
            assertThat(ops.getList("test").isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToList() appends to existing list")
        void mergeToListAppendsToExistingList() {
            final List<Object> list = new ArrayList<>();
            list.add(1);
            list.add(2);

            final DataResult<Object> result = ops.mergeToList(list, ops.createInt(3));

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final List<Object> merged = (List<Object>) result.result().orElseThrow();
            assertThat(merged).hasSize(3);
            assertThat(merged.get(2)).isEqualTo(3);
        }

        @Test
        @DisplayName("mergeToList() throws NullPointerException for null list")
        void mergeToListThrowsNullPointerExceptionForNullList() {
            assertThatThrownBy(() -> ops.mergeToList(null, ops.createInt(1)))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("mergeToList() returns error for non-list input")
        void mergeToListReturnsErrorForNonListInput() {
            final DataResult<Object> result = ops.mergeToList(new LinkedHashMap<>(), ops.createInt(1));

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToList() preserves original list immutability")
        void mergeToListPreservesOriginalListImmutability() {
            final List<Object> original = new ArrayList<>();
            original.add(1);

            ops.mergeToList(original, ops.createInt(2));

            assertThat(original).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Map Operations")
    class MapOperations {

        @Test
        @DisplayName("createMap() creates Map from stream")
        void createMapCreatesMapFromStream() {
            final Object result = ops.createMap(Stream.of(
                    Pair.of(ops.createString("name"), ops.createString("Alice")),
                    Pair.of(ops.createString("age"), ops.createInt(30))
            ));

            assertThat(result).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertThat(map.get("name")).isEqualTo("Alice");
            assertThat(map.get("age")).isEqualTo(30);
        }

        @Test
        @DisplayName("getMapEntries() returns stream of entries")
        void getMapEntriesReturnsStreamOfEntries() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "Bob");
            map.put("age", 25);

            final DataResult<Stream<Pair<Object, Object>>> result = ops.getMapEntries(map);

            assertThat(result.isSuccess()).isTrue();
            final List<Pair<Object, Object>> entries = result.result().orElseThrow().toList();
            assertThat(entries).hasSize(2);
        }

        @Test
        @DisplayName("getMapEntries() returns error for non-map")
        void getMapEntriesReturnsErrorForNonMap() {
            assertThat(ops.getMapEntries(new ArrayList<>()).isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToMap() adds key-value to existing map")
        void mergeToMapAddsKeyValueToExistingMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "Alice");

            final DataResult<Object> result = ops.mergeToMap(
                    map,
                    ops.createString("age"),
                    ops.createInt(30)
            );

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> merged = (Map<String, Object>) result.result().orElseThrow();
            assertThat(merged.get("name")).isEqualTo("Alice");
            assertThat(merged.get("age")).isEqualTo(30);
        }

        @Test
        @DisplayName("mergeToMap() throws NullPointerException for null map")
        void mergeToMapThrowsNullPointerExceptionForNullMap() {
            assertThatThrownBy(() -> ops.mergeToMap(
                    null,
                    ops.createString("key"),
                    ops.createString("value")
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("mergeToMap() returns error for non-string key")
        void mergeToMapReturnsErrorForNonStringKey() {
            final DataResult<Object> result = ops.mergeToMap(
                    new LinkedHashMap<>(),
                    ops.createInt(42),
                    ops.createString("value")
            );

            assertThat(result.isError()).isTrue();
        }

        @Test
        @DisplayName("mergeToMap() merges two maps")
        void mergeToMapMergesTwoMaps() {
            final Map<String, Object> first = new LinkedHashMap<>();
            first.put("a", 1);

            final Map<String, Object> second = new LinkedHashMap<>();
            second.put("b", 2);

            final DataResult<Object> result = ops.mergeToMap(first, second);

            assertThat(result.isSuccess()).isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> merged = (Map<String, Object>) result.result().orElseThrow();
            assertThat(merged.get("a")).isEqualTo(1);
            assertThat(merged.get("b")).isEqualTo(2);
        }

        @Test
        @DisplayName("mergeToMap() preserves original map immutability")
        void mergeToMapPreservesOriginalMapImmutability() {
            final Map<String, Object> original = new LinkedHashMap<>();
            original.put("key", "original");

            ops.mergeToMap(original, ops.createString("key2"), ops.createString("value2"));

            assertThat(original).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Field Operations")
    class FieldOperations {

        @Test
        @DisplayName("get() returns field value")
        void getReturnsFieldValue() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "Alice");

            final Object result = ops.get(map, "name");

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo("Alice");
        }

        @Test
        @DisplayName("get() returns null for missing field")
        void getReturnsNullForMissingField() {
            final Map<String, Object> map = new LinkedHashMap<>();

            assertThat(ops.get(map, "missing")).isNull();
        }

        @Test
        @DisplayName("get() returns null for non-map")
        void getReturnsNullForNonMap() {
            assertThat(ops.get(new ArrayList<>(), "key")).isNull();
        }

        @Test
        @DisplayName("set() adds field to map")
        void setAddsFieldToMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("existing", "value");

            final Object result = ops.set(map, "new", ops.createString("newValue"));

            assertThat(result).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            final Map<String, Object> resultMap = (Map<String, Object>) result;
            assertThat(resultMap.get("new")).isEqualTo("newValue");
            assertThat(resultMap.get("existing")).isEqualTo("value");
        }

        @Test
        @DisplayName("set() creates map for non-map input")
        void setCreatesMapForNonMapInput() {
            final Object result = ops.set(new ArrayList<>(), "key", ops.createString("value"));

            assertThat(result).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            final Map<String, Object> resultMap = (Map<String, Object>) result;
            assertThat(resultMap.get("key")).isEqualTo("value");
        }

        @Test
        @DisplayName("set() preserves original map immutability")
        void setPreservesOriginalMapImmutability() {
            final Map<String, Object> original = new LinkedHashMap<>();
            original.put("key", "original");

            ops.set(original, "key", ops.createString("modified"));

            assertThat(original.get("key")).isEqualTo("original");
        }

        @Test
        @DisplayName("remove() removes field from map")
        void removeRemovesFieldFromMap() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("keep", "value1");
            map.put("remove", "value2");

            final Object result = ops.remove(map, "remove");

            assertThat(result).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            final Map<String, Object> resultMap = (Map<String, Object>) result;
            assertThat(resultMap.containsKey("keep")).isTrue();
            assertThat(resultMap.containsKey("remove")).isFalse();
        }

        @Test
        @DisplayName("remove() returns input unchanged for non-map")
        void removeReturnsInputUnchangedForNonMap() {
            final List<Object> list = new ArrayList<>();

            final Object result = ops.remove(list, "key");

            assertThat(result).isSameAs(list);
        }

        @Test
        @DisplayName("remove() preserves original map immutability")
        void removePreservesOriginalMapImmutability() {
            final Map<String, Object> original = new LinkedHashMap<>();
            original.put("key", "value");

            ops.remove(original, "key");

            assertThat(original.containsKey("key")).isTrue();
        }

        @Test
        @DisplayName("has() returns true for existing field")
        void hasReturnsTrueForExistingField() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "Alice");

            assertThat(ops.has(map, "name")).isTrue();
        }

        @Test
        @DisplayName("has() returns false for missing field")
        void hasReturnsFalseForMissingField() {
            final Map<String, Object> map = new LinkedHashMap<>();

            assertThat(ops.has(map, "missing")).isFalse();
        }

        @Test
        @DisplayName("has() returns false for non-map")
        void hasReturnsFalseForNonMap() {
            assertThat(ops.has(new ArrayList<>(), "key")).isFalse();
        }
    }

    @Nested
    @DisplayName("Conversion")
    class Conversion {

        @Test
        @DisplayName("convertTo() converts primitives from same ops")
        void convertToConvertsPrimitivesFromSameOps() {
            final Object original = ops.createString("hello");

            final Object result = ops.convertTo(ops, original);

            assertThat(result).isEqualTo("hello");
        }

        @Test
        @DisplayName("convertTo() converts numbers")
        void convertToConvertsNumbers() {
            final Object original = ops.createInt(42);

            final Object result = ops.convertTo(ops, original);

            assertThat(result).isEqualTo(42);
        }

        @Test
        @DisplayName("convertTo() converts booleans")
        void convertToConvertsBooleans() {
            final Object original = ops.createBoolean(true);

            final Object result = ops.convertTo(ops, original);

            assertThat(result).isEqualTo(true);
        }

        @Test
        @DisplayName("convertTo() converts lists")
        void convertToConvertsLists() {
            final Object original = ops.createList(Stream.of(
                    ops.createInt(1),
                    ops.createInt(2)
            ));

            final Object result = ops.convertTo(ops, original);

            assertThat(result).isInstanceOf(List.class);
            assertThat((List<?>) result).hasSize(2);
        }

        @Test
        @DisplayName("convertTo() converts maps")
        void convertToConvertsMaps() {
            final Object original = ops.createMap(Stream.of(
                    Pair.of(ops.createString("key"), ops.createString("value"))
            ));

            final Object result = ops.convertTo(ops, original);

            assertThat(result).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) result;
            assertThat(map.get("key")).isEqualTo("value");
        }

        @Test
        @DisplayName("convertTo() throws NullPointerException for null input")
        void convertToThrowsNullPointerExceptionForNullInput() {
            assertThatThrownBy(() -> ops.convertTo(ops, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("must not be null");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles empty string")
        void handlesEmptyString() {
            final Object created = ops.createString("");
            final DataResult<String> read = ops.getStringValue(created);

            assertThat(read.result()).contains("");
        }

        @Test
        @DisplayName("handles zero values")
        void handlesZeroValues() {
            assertThat(ops.createInt(0)).isEqualTo(0);
            assertThat(ops.createLong(0L)).isEqualTo(0L);
            assertThat(ops.createFloat(0f)).isEqualTo(0f);
            assertThat(ops.createDouble(0.0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("handles negative values")
        void handlesNegativeValues() {
            assertThat(ops.createInt(-42)).isEqualTo(-42);
            assertThat(ops.createLong(-100L)).isEqualTo(-100L);
            assertThat(ops.createDouble(-3.14)).isEqualTo(-3.14);
        }

        @Test
        @DisplayName("handles special floating point values")
        void handlesSpecialFloatingPointValues() {
            assertThat(ops.createDouble(Double.MAX_VALUE)).isEqualTo(Double.MAX_VALUE);
            assertThat(ops.createDouble(Double.MIN_VALUE)).isEqualTo(Double.MIN_VALUE);
        }

        @Test
        @DisplayName("handles empty list")
        void handlesEmptyList() {
            final Object list = ops.createList(Stream.empty());

            assertThat(list).isInstanceOf(List.class);
            assertThat((List<?>) list).isEmpty();
        }

        @Test
        @DisplayName("handles empty map")
        void handlesEmptyMap() {
            final Object map = ops.createMap(Stream.empty());

            assertThat(map).isInstanceOf(Map.class);
            assertThat((Map<?, ?>) map).isEmpty();
        }

        @Test
        @DisplayName("handles nested structures")
        void handlesNestedStructures() {
            final Object nested = ops.createMap(Stream.of(
                    Pair.of(
                            ops.createString("items"),
                            ops.createList(Stream.of(
                                    ops.createMap(Stream.of(
                                            Pair.of(ops.createString("id"), ops.createInt(1))
                                    ))
                            ))
                    )
            ));

            assertThat(nested).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            final Map<String, Object> map = (Map<String, Object>) nested;
            assertThat(map.get("items")).isInstanceOf(List.class);
            @SuppressWarnings("unchecked")
            final List<Object> items = (List<Object>) map.get("items");
            assertThat(items.get(0)).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("createMap() skips null keys")
        void createMapSkipsNullKeys() {
            final Object map = ops.createMap(Stream.of(
                    Pair.of(null, ops.createString("value")),
                    Pair.of(ops.createString("key"), ops.createString("value"))
            ));

            @SuppressWarnings("unchecked")
            final Map<String, Object> resultMap = (Map<String, Object>) map;
            assertThat(resultMap).hasSize(1);
            assertThat(resultMap.containsKey("key")).isTrue();
        }

        @Test
        @DisplayName("createMap() handles null values")
        void createMapHandlesNullValues() {
            final Object map = ops.createMap(Stream.of(
                    Pair.of(ops.createString("key"), null)
            ));

            @SuppressWarnings("unchecked")
            final Map<String, Object> resultMap = (Map<String, Object>) map;
            assertThat(resultMap.get("key")).isNull();
        }
    }
}
