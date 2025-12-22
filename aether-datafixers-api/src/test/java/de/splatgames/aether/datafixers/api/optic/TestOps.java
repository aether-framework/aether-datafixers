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

package de.splatgames.aether.datafixers.api.optic;

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A simple DynamicOps implementation for testing purposes.
 * Uses plain Java objects: String, Number, Boolean, Map, List.
 */
public final class TestOps implements DynamicOps<Object> {

    public static final TestOps INSTANCE = new TestOps();

    private TestOps() {}

    @Override
    public @NotNull Object empty() {
        return new LinkedHashMap<>();
    }

    @Override
    public @NotNull Object emptyMap() {
        return new LinkedHashMap<>();
    }

    @Override
    public @NotNull Object emptyList() {
        return List.of();
    }

    @Override
    public boolean isMap(@NotNull final Object value) {
        return value instanceof Map;
    }

    @Override
    public boolean isList(@NotNull final Object value) {
        return value instanceof List;
    }

    @Override
    public boolean isString(@NotNull final Object value) {
        return value instanceof String;
    }

    @Override
    public boolean isNumber(@NotNull final Object value) {
        return value instanceof Number;
    }

    @Override
    public boolean isBoolean(@NotNull final Object value) {
        return value instanceof Boolean;
    }

    @Override
    public @NotNull Object createString(@NotNull final String value) {
        return value;
    }

    @Override
    public @NotNull Object createInt(final int value) {
        return value;
    }

    @Override
    public @NotNull Object createLong(final long value) {
        return value;
    }

    @Override
    public @NotNull Object createFloat(final float value) {
        return value;
    }

    @Override
    public @NotNull Object createDouble(final double value) {
        return value;
    }

    @Override
    public @NotNull Object createByte(final byte value) {
        return value;
    }

    @Override
    public @NotNull Object createShort(final short value) {
        return value;
    }

    @Override
    public @NotNull Object createBoolean(final boolean value) {
        return value;
    }

    @Override
    public @NotNull Object createNumeric(@NotNull final Number value) {
        return value;
    }

    @Override
    public @NotNull DataResult<String> getStringValue(@NotNull final Object input) {
        if (input instanceof String s) {
            return DataResult.success(s);
        }
        return DataResult.error("Not a string: " + input);
    }

    @Override
    public @NotNull DataResult<Number> getNumberValue(@NotNull final Object input) {
        if (input instanceof Number n) {
            return DataResult.success(n);
        }
        return DataResult.error("Not a number: " + input);
    }

    @Override
    public @NotNull DataResult<Boolean> getBooleanValue(@NotNull final Object input) {
        if (input instanceof Boolean b) {
            return DataResult.success(b);
        }
        return DataResult.error("Not a boolean: " + input);
    }

    @Override
    public @NotNull Object createList(@NotNull final Stream<Object> values) {
        return values.toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull DataResult<Stream<Object>> getList(@NotNull final Object input) {
        if (input instanceof List<?> list) {
            return DataResult.success(((List<Object>) list).stream());
        }
        return DataResult.error("Not a list: " + input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull DataResult<Object> mergeToList(@NotNull final Object list,
                                                    @NotNull final Object value) {
        if (list instanceof List<?> l) {
            final var result = new java.util.ArrayList<>((List<Object>) l);
            result.add(value);
            return DataResult.success(result);
        }
        return DataResult.error("Not a list: " + list);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Object get(@NotNull final Object value,
                                 @NotNull final String key) {
        if (value instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).get(key);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Object set(@NotNull final Object value,
                                @NotNull final String key,
                                @NotNull final Object newValue) {
        if (value instanceof Map<?, ?> map) {
            final var result = new LinkedHashMap<>((Map<String, Object>) map);
            result.put(key, newValue);
            return result;
        }
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Object remove(@NotNull final Object value,
                                   @NotNull final String key) {
        if (value instanceof Map<?, ?> map) {
            final var result = new LinkedHashMap<>((Map<String, Object>) map);
            result.remove(key);
            return result;
        }
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean has(@NotNull final Object value,
                       @NotNull final String key) {
        if (value instanceof Map<?, ?> map) {
            return ((Map<String, Object>) map).containsKey(key);
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Object createMap(@NotNull final Stream<Pair<Object, Object>> entries) {
        final var result = new LinkedHashMap<String, Object>();
        entries.forEach(pair -> {
            final String key = (String) pair.first();
            result.put(key, pair.second());
        });
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull DataResult<Stream<Pair<Object, Object>>> getMapEntries(@NotNull final Object input) {
        if (input instanceof Map<?, ?> map) {
            return DataResult.success(
                    ((Map<String, Object>) map).entrySet().stream()
                            .map(e -> Pair.of((Object) e.getKey(), e.getValue()))
            );
        }
        return DataResult.error("Not a map: " + input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull DataResult<Object> mergeToMap(@NotNull final Object map,
                                                   @NotNull final Object key,
                                                   @NotNull final Object value) {
        if (!(key instanceof String keyStr)) {
            return DataResult.error("Key must be a string: " + key);
        }
        final var result = new LinkedHashMap<String, Object>();
        if (map instanceof Map<?, ?> m) {
            result.putAll((Map<String, Object>) m);
        }
        result.put(keyStr, value);
        return DataResult.success(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull DataResult<Object> mergeToMap(@NotNull final Object map,
                                                   @NotNull final Object other) {
        final var result = new LinkedHashMap<String, Object>();
        if (map instanceof Map<?, ?> m) {
            result.putAll((Map<String, Object>) m);
        }
        if (other instanceof Map<?, ?> o) {
            result.putAll((Map<String, Object>) o);
        }
        return DataResult.success(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> @NotNull Object convertTo(@NotNull final DynamicOps<U> ops,
                                          @NotNull final U input) {
        // Simple identity conversion for same ops type
        if (ops == this) {
            return input;
        }
        // Basic conversion support
        if (ops.isString(input)) {
            return ops.getStringValue(input).result().orElse("");
        }
        if (ops.isNumber(input)) {
            return ops.getNumberValue(input).result().orElse(0);
        }
        if (ops.isBoolean(input)) {
            return ops.getBooleanValue(input).result().orElse(false);
        }
        if (ops.isList(input)) {
            return ops.getList(input).result()
                    .map(stream -> stream.map(v -> convertTo(ops, v)).toList())
                    .orElse(List.of());
        }
        if (ops.isMap(input)) {
            final var result = new LinkedHashMap<String, Object>();
            ops.getMapEntries(input).result().ifPresent(stream ->
                    stream.forEach(pair -> {
                        final String key = ops.getStringValue(pair.first()).result().orElse("");
                        result.put(key, convertTo(ops, pair.second()));
                    })
            );
            return result;
        }
        return input;
    }
}
