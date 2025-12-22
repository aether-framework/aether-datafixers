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
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Either;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Factory class providing primitive codecs and codec combinators.
 *
 * <p>This class serves as the primary entry point for obtaining commonly-used
 * codecs for Java primitive types, collections, and common patterns. All codecs provided here are thread-safe and
 * reusable.</p>
 *
 * <h2>Primitive Codecs</h2>
 * <p>The following primitive codecs are available as static fields:</p>
 * <ul>
 *   <li>{@link #BOOL} - Boolean values</li>
 *   <li>{@link #INT} - Integer values (32-bit signed)</li>
 *   <li>{@link #LONG} - Long values (64-bit signed)</li>
 *   <li>{@link #FLOAT} - Float values (32-bit floating point)</li>
 *   <li>{@link #DOUBLE} - Double values (64-bit floating point)</li>
 *   <li>{@link #BYTE} - Byte values (8-bit signed)</li>
 *   <li>{@link #SHORT} - Short values (16-bit signed)</li>
 *   <li>{@link #STRING} - String values</li>
 * </ul>
 *
 * <h2>Collection Codecs</h2>
 * <p>Factory methods for creating collection-based codecs:</p>
 * <ul>
 *   <li>{@link #list(Codec)} - Encodes/decodes {@code List<A>}</li>
 *   <li>{@link #optional(Codec)} - Encodes/decodes {@code Optional<A>}</li>
 *   <li>{@link #either(Codec, Codec)} - Encodes/decodes {@link Either}</li>
 *   <li>{@link #pair(Codec, Codec)} - Encodes/decodes {@link Pair}</li>
 * </ul>
 *
 * <h2>Bounded Codecs</h2>
 * <p>Factory methods for creating value-constrained codecs:</p>
 * <ul>
 *   <li>{@link #intRange(int, int)} - Integer with min/max bounds</li>
 *   <li>{@link #floatRange(float, float)} - Float with min/max bounds</li>
 *   <li>{@link #doubleRange(double, double)} - Double with min/max bounds</li>
 *   <li>{@link #nonEmptyString()} - Non-empty string validation</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Using primitive codecs
 * DataResult<JsonElement> json = Codecs.INT.encodeStart(GsonOps.INSTANCE, 42);
 * DataResult<Integer> value = Codecs.INT.parse(GsonOps.INSTANCE, jsonElement);
 *
 * // Creating a list codec
 * Codec<List<String>> stringListCodec = Codecs.list(Codecs.STRING);
 *
 * // Using bounded codecs
 * Codec<Integer> ageCodec = Codecs.intRange(0, 150);
 *
 * // Combining with fieldOf for record codecs
 * Codec<Person> personCodec = RecordCodecBuilder.create(instance ->
 *     instance.group(
 *         Codecs.STRING.fieldOf("name").forGetter(Person::name),
 *         Codecs.intRange(0, 150).fieldOf("age").forGetter(Person::age)
 *     ).apply(instance, Person::new)
 * );
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All codecs provided by this class are stateless and thread-safe.
 * They can be safely shared across multiple threads.</p>
 *
 * @author Erik Pförtner
 * @see Codec
 * @see MapCodec
 * @see RecordCodecBuilder
 * @since 0.1.0
 */
public final class Codecs {

    /**
     * Codec for {@link Boolean} values.
     *
     * <p>Encodes boolean values using {@link DynamicOps#createBoolean(boolean)}
     * and decodes using {@link DynamicOps#getBooleanValue(Object)}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Encode a boolean
     * DataResult<JsonElement> json = Codecs.BOOL.encodeStart(GsonOps.INSTANCE, true);
     * // Result: JsonPrimitive(true)
     *
     * // Decode a boolean
     * DataResult<Boolean> value = Codecs.BOOL.parse(GsonOps.INSTANCE, JsonPrimitive(false));
     * // Result: false
     * }</pre>
     *
     * @see DynamicOps#createBoolean(boolean)
     * @see DynamicOps#getBooleanValue(Object)
     */
    public static final Codec<Boolean> BOOL = new Codec<>() {
        @NotNull
        @Override
        public <T> DataResult<T> encode(@NotNull final Boolean input,
                                        @NotNull final DynamicOps<T> ops,
                                        @NotNull final T prefix) {
            return DataResult.success(ops.createBoolean(input));
        }

        @NotNull
        @Override
        public <T> DataResult<Pair<Boolean, T>> decode(@NotNull final DynamicOps<T> ops,
                                                       @NotNull final T input) {
            return ops.getBooleanValue(input).map(b -> Pair.of(b, ops.empty()));
        }
    };

    // ==================== Primitive Codecs ====================
    /**
     * Codec for {@link Integer} values (32-bit signed integers).
     *
     * <p>Encodes integer values using {@link DynamicOps#createInt(int)}
     * and decodes using {@link DynamicOps#getNumberValue(Object)}, converting the result to an int with
     * {@link Number#intValue()}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Encode an integer
     * DataResult<JsonElement> json = Codecs.INT.encodeStart(GsonOps.INSTANCE, 42);
     *
     * // Decode an integer
     * DataResult<Integer> value = Codecs.INT.parse(GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @see DynamicOps#createInt(int)
     * @see DynamicOps#getNumberValue(Object)
     * @see #intRange(int, int)
     */
    public static final Codec<Integer> INT = new Codec<>() {
        @NotNull
        @Override
        public <T> DataResult<T> encode(@NotNull final Integer input,
                                        @NotNull final DynamicOps<T> ops,
                                        @NotNull final T prefix) {
            return DataResult.success(ops.createInt(input));
        }

        @NotNull
        @Override
        public <T> DataResult<Pair<Integer, T>> decode(@NotNull final DynamicOps<T> ops,
                                                       @NotNull final T input) {
            return ops.getNumberValue(input).map(n -> Pair.of(n.intValue(), ops.empty()));
        }
    };
    /**
     * Codec for {@link Long} values (64-bit signed integers).
     *
     * <p>Encodes long values using {@link DynamicOps#createLong(long)}
     * and decodes using {@link DynamicOps#getNumberValue(Object)}, converting the result to a long with
     * {@link Number#longValue()}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Encode a long
     * DataResult<JsonElement> json = Codecs.LONG.encodeStart(GsonOps.INSTANCE, 9876543210L);
     *
     * // Decode a long
     * DataResult<Long> value = Codecs.LONG.parse(GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @see DynamicOps#createLong(long)
     * @see DynamicOps#getNumberValue(Object)
     */
    public static final Codec<Long> LONG = new Codec<>() {
        @NotNull
        @Override
        public <T> DataResult<T> encode(@NotNull final Long input,
                                        @NotNull final DynamicOps<T> ops,
                                        @NotNull final T prefix) {
            return DataResult.success(ops.createLong(input));
        }

        @NotNull
        @Override
        public <T> DataResult<Pair<Long, T>> decode(@NotNull final DynamicOps<T> ops,
                                                    @NotNull final T input) {
            return ops.getNumberValue(input).map(n -> Pair.of(n.longValue(), ops.empty()));
        }
    };
    /**
     * Codec for {@link Float} values (32-bit floating point).
     *
     * <p>Encodes float values using {@link DynamicOps#createFloat(float)}
     * and decodes using {@link DynamicOps#getNumberValue(Object)}, converting the result to a float with
     * {@link Number#floatValue()}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Encode a float
     * DataResult<JsonElement> json = Codecs.FLOAT.encodeStart(GsonOps.INSTANCE, 3.14f);
     *
     * // Decode a float
     * DataResult<Float> value = Codecs.FLOAT.parse(GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @see DynamicOps#createFloat(float)
     * @see DynamicOps#getNumberValue(Object)
     * @see #floatRange(float, float)
     */
    public static final Codec<Float> FLOAT = new Codec<>() {
        @NotNull
        @Override
        public <T> DataResult<T> encode(@NotNull final Float input,
                                        @NotNull final DynamicOps<T> ops,
                                        @NotNull final T prefix) {
            return DataResult.success(ops.createFloat(input));
        }

        @NotNull
        @Override
        public <T> DataResult<Pair<Float, T>> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
            return ops.getNumberValue(input).map(n -> Pair.of(n.floatValue(), ops.empty()));
        }
    };
    /**
     * Codec for {@link Double} values (64-bit floating point).
     *
     * <p>Encodes double values using {@link DynamicOps#createDouble(double)}
     * and decodes using {@link DynamicOps#getNumberValue(Object)}, converting the result to a double with
     * {@link Number#doubleValue()}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Encode a double
     * DataResult<JsonElement> json = Codecs.DOUBLE.encodeStart(GsonOps.INSTANCE, 3.14159265358979);
     *
     * // Decode a double
     * DataResult<Double> value = Codecs.DOUBLE.parse(GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @see DynamicOps#createDouble(double)
     * @see DynamicOps#getNumberValue(Object)
     * @see #doubleRange(double, double)
     */
    public static final Codec<Double> DOUBLE = new Codec<>() {
        @NotNull
        @Override
        public <T> DataResult<T> encode(@NotNull final Double input,
                                        @NotNull final DynamicOps<T> ops,
                                        @NotNull final T prefix) {
            return DataResult.success(ops.createDouble(input));
        }

        @NotNull
        @Override
        public <T> DataResult<Pair<Double, T>> decode(@NotNull final DynamicOps<T> ops,
                                                      @NotNull final T input) {
            return ops.getNumberValue(input).map(n -> Pair.of(n.doubleValue(), ops.empty()));
        }
    };
    /**
     * Codec for {@link Byte} values (8-bit signed integers).
     *
     * <p>Encodes byte values using {@link DynamicOps#createByte(byte)}
     * and decodes using {@link DynamicOps#getNumberValue(Object)}, converting the result to a byte with
     * {@link Number#byteValue()}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Encode a byte
     * DataResult<JsonElement> json = Codecs.BYTE.encodeStart(GsonOps.INSTANCE, (byte) 127);
     *
     * // Decode a byte
     * DataResult<Byte> value = Codecs.BYTE.parse(GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @see DynamicOps#createByte(byte)
     * @see DynamicOps#getNumberValue(Object)
     */
    public static final Codec<Byte> BYTE = new Codec<>() {
        @NotNull
        @Override
        public <T> DataResult<T> encode(@NotNull final Byte input,
                                        @NotNull final DynamicOps<T> ops,
                                        @NotNull final T prefix) {
            return DataResult.success(ops.createByte(input));
        }

        @NotNull
        @Override
        public <T> DataResult<Pair<Byte, T>> decode(@NotNull final DynamicOps<T> ops,
                                                    @NotNull final T input) {
            return ops.getNumberValue(input).map(n -> Pair.of(n.byteValue(), ops.empty()));
        }
    };
    /**
     * Codec for {@link Short} values (16-bit signed integers).
     *
     * <p>Encodes short values using {@link DynamicOps#createShort(short)}
     * and decodes using {@link DynamicOps#getNumberValue(Object)}, converting the result to a short with
     * {@link Number#shortValue()}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Encode a short
     * DataResult<JsonElement> json = Codecs.SHORT.encodeStart(GsonOps.INSTANCE, (short) 32767);
     *
     * // Decode a short
     * DataResult<Short> value = Codecs.SHORT.parse(GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @see DynamicOps#createShort(short)
     * @see DynamicOps#getNumberValue(Object)
     */
    public static final Codec<Short> SHORT = new Codec<>() {
        @NotNull
        @Override
        public <T> DataResult<T> encode(@NotNull final Short input,
                                        @NotNull final DynamicOps<T> ops,
                                        @NotNull final T prefix) {
            return DataResult.success(ops.createShort(input));
        }

        @NotNull
        @Override
        public <T> DataResult<Pair<Short, T>> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
            return ops.getNumberValue(input).map(n -> Pair.of(n.shortValue(), ops.empty()));
        }
    };
    /**
     * Codec for {@link String} values.
     *
     * <p>Encodes string values using {@link DynamicOps#createString(String)}
     * and decodes using {@link DynamicOps#getStringValue(Object)}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Encode a string
     * DataResult<JsonElement> json = Codecs.STRING.encodeStart(GsonOps.INSTANCE, "Hello, World!");
     *
     * // Decode a string
     * DataResult<String> value = Codecs.STRING.parse(GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @see DynamicOps#createString(String)
     * @see DynamicOps#getStringValue(Object)
     * @see #nonEmptyString()
     */
    public static final Codec<String> STRING = new Codec<>() {
        @NotNull
        @Override
        public <T> DataResult<T> encode(@NotNull final String input,
                                        @NotNull final DynamicOps<T> ops,
                                        @NotNull final T prefix) {
            return DataResult.success(ops.createString(input));
        }

        @NotNull
        @Override
        public <T> DataResult<Pair<String, T>> decode(@NotNull final DynamicOps<T> ops,
                                                      @NotNull final T input) {
            return ops.getStringValue(input).map(s -> Pair.of(s, ops.empty()));
        }
    };

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class is a utility class containing only static fields and methods.</p>
     */
    private Codecs() {
        // private constructor to prevent instantiation
    }

    // ==================== Collection Codecs ====================

    /**
     * Creates a codec for {@link List} values from an element codec.
     *
     * <p>The resulting codec encodes lists as arrays in the target format and
     * decodes arrays back into lists. During decoding, if some elements fail to decode, the codec returns a partial
     * result containing successfully decoded elements along with error information.</p>
     *
     * <h4>Encoding Behavior</h4>
     * <ul>
     *   <li>Each element is encoded using the provided element codec</li>
     *   <li>If any element fails to encode, the entire encoding fails</li>
     *   <li>Result is created via {@link DynamicOps#createList(java.util.stream.Stream)}</li>
     * </ul>
     *
     * <h4>Decoding Behavior</h4>
     * <ul>
     *   <li>Each element in the input list is decoded individually</li>
     *   <li>Partial success is supported: successfully decoded elements are preserved</li>
     *   <li>Error messages for failed elements are aggregated</li>
     * </ul>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create a list codec for strings
     * Codec<List<String>> stringListCodec = Codecs.list(Codecs.STRING);
     *
     * // Encode a list
     * List<String> names = List.of("Alice", "Bob", "Charlie");
     * DataResult<JsonElement> json = stringListCodec.encodeStart(GsonOps.INSTANCE, names);
     * // Result: ["Alice", "Bob", "Charlie"]
     *
     * // Decode a list
     * DataResult<List<String>> decoded = stringListCodec.parse(GsonOps.INSTANCE, jsonArray);
     * }</pre>
     *
     * @param elementCodec the codec for individual list elements, must not be {@code null}
     * @param <A>          the type of elements in the list
     * @return a new codec for {@code List<A>}, never {@code null}
     * @throws NullPointerException if {@code elementCodec} is {@code null}
     * @see Codec#listOf()
     */
    @NotNull
    public static <A> Codec<List<A>> list(@NotNull final Codec<A> elementCodec) {
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final List<A> input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                final List<T> encoded = new ArrayList<>(input.size());
                for (final A element : input) {
                    final DataResult<T> result = elementCodec.encodeStart(ops, element);
                    if (result.isError()) {
                        return result.map(t -> ops.emptyList());
                    }
                    encoded.add(result.result().orElseThrow());
                }
                return DataResult.success(ops.createList(encoded.stream()));
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<List<A>, T>> decode(@NotNull final DynamicOps<T> ops,
                                                           @NotNull final T input) {
                return ops.getList(input).flatMap(stream -> {
                    final List<A> result = new ArrayList<>();
                    final List<String> errors = new ArrayList<>();

                    stream.forEach(element -> {
                        final DataResult<A> decoded = elementCodec.parse(ops, element);
                        if (decoded.isSuccess()) {
                            result.add(decoded.result().orElseThrow());
                        } else {
                            errors.add(decoded.error().orElse("Unknown error"));
                        }
                    });

                    if (!errors.isEmpty()) {
                        final String errorMessage = "Errors decoding list: " + String.join(", ", errors);
                        return DataResult.error(errorMessage, Pair.of(result, ops.empty()));
                    }
                    return DataResult.success(Pair.of(result, ops.empty()));
                });
            }
        };
    }

    /**
     * Creates a codec for {@link Optional} values from a base codec.
     *
     * <p>The resulting codec handles both present and absent values:</p>
     * <ul>
     *   <li><b>Encoding:</b> An empty Optional encodes to {@link DynamicOps#empty()};
     *       a present Optional encodes its contained value using the base codec</li>
     *   <li><b>Decoding:</b> If the base codec succeeds, wraps the result in
     *       {@code Optional.of()}; if it fails, returns {@code Optional.empty()}</li>
     * </ul>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create an optional string codec
     * Codec<Optional<String>> optionalStringCodec = Codecs.optional(Codecs.STRING);
     *
     * // Encode a present value
     * DataResult<JsonElement> present = optionalStringCodec.encodeStart(
     *     GsonOps.INSTANCE, Optional.of("hello"));
     * // Result: "hello"
     *
     * // Encode an absent value
     * DataResult<JsonElement> absent = optionalStringCodec.encodeStart(
     *     GsonOps.INSTANCE, Optional.empty());
     * // Result: JsonNull
     *
     * // Decode - returns Optional.empty() if base codec fails
     * DataResult<Optional<String>> decoded = optionalStringCodec.parse(
     *     GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @param codec the base codec for the optional value, must not be {@code null}
     * @param <A>   the type of the optional value
     * @return a new codec for {@code Optional<A>}, never {@code null}
     * @throws NullPointerException if {@code codec} is {@code null}
     * @see Codec#optionalOf()
     */
    @NotNull
    public static <A> Codec<Optional<A>> optional(@NotNull final Codec<A> codec) {
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final Optional<A> input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                return input
                        .<DataResult<T>>map(a -> codec.encode(a, ops, prefix))
                        .orElseGet(() -> DataResult.success(ops.empty()));
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<Optional<A>, T>> decode(@NotNull final DynamicOps<T> ops,
                                                               @NotNull final T input) {
                final DataResult<Pair<A, T>> result = codec.decode(ops, input);
                if (result.isSuccess()) {
                    return result.flatMap(p -> {
                        final A value = p.first();
                        if (value == null) {
                            return DataResult.error(() -> "Decoded value is null (expected non-null).");
                        }
                        return DataResult.success(Pair.of(Optional.of(value), p.second()));
                    });
                }
                return DataResult.success(Pair.of(Optional.empty(), input));
            }
        };
    }

    /**
     * Creates a codec for {@link Either} values from two component codecs.
     *
     * <p>The resulting codec attempts to use one of two codecs based on the
     * value or input:</p>
     * <ul>
     *   <li><b>Encoding:</b> Uses the left codec for {@link Either#left(Object)} values,
     *       the right codec for {@link Either#right(Object)} values</li>
     *   <li><b>Decoding:</b> Attempts the left codec first; if it fails, attempts
     *       the right codec; if both fail, returns an error with both messages</li>
     * </ul>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * // Create a codec that accepts either a string or an integer
     * Codec<Either<String, Integer>> stringOrInt =
     *     Codecs.either(Codecs.STRING, Codecs.INT);
     *
     * // Encode a left value
     * DataResult<JsonElement> left = stringOrInt.encodeStart(
     *     GsonOps.INSTANCE, Either.left("hello"));
     * // Result: "hello"
     *
     * // Encode a right value
     * DataResult<JsonElement> right = stringOrInt.encodeStart(
     *     GsonOps.INSTANCE, Either.right(42));
     * // Result: 42
     *
     * // Decode - tries left first, then right
     * DataResult<Either<String, Integer>> decoded = stringOrInt.parse(
     *     GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @param left  the codec for left values, must not be {@code null}
     * @param right the codec for right values, must not be {@code null}
     * @param <L>   the type of left values
     * @param <R>   the type of right values
     * @return a new codec for {@code Either<L, R>}, never {@code null}
     * @throws NullPointerException if {@code left} or {@code right} is {@code null}
     * @see Either
     * @see Codec#either(Codec, Codec)
     */
    @NotNull
    public static <L, R> Codec<Either<L, R>> either(@NotNull final Codec<L> left,
                                                    @NotNull final Codec<R> right) {
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final Either<L, R> input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                return input.fold(
                        l -> left.encode(l, ops, prefix),
                        r -> right.encode(r, ops, prefix)
                );
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<Either<L, R>, T>> decode(@NotNull final DynamicOps<T> ops,
                                                                @NotNull final T input) {
                final DataResult<Pair<L, T>> leftResult = left.decode(ops, input);
                if (leftResult.isSuccess()) {
                    return leftResult.flatMap(p -> {
                        final L l = p.first();
                        if (l == null) {
                            return DataResult.error(() -> "Left codec decoded null (expected non-null).");
                        }
                        return DataResult.success(Pair.of(Either.left(l), p.second()));
                    });
                }

                final DataResult<Pair<R, T>> rightResult = right.decode(ops, input);
                if (rightResult.isSuccess()) {
                    return rightResult.flatMap(p -> {
                        final R r = p.first();
                        if (r == null) {
                            return DataResult.error(() -> "Right codec decoded null (expected non-null).");
                        }
                        return DataResult.success(Pair.of(Either.right(r), p.second()));
                    });
                }

                return DataResult.error("Neither left nor right codec matched: " +
                        leftResult.error().orElse("?") + " / " + rightResult.error().orElse("?"));
            }
        };
    }

    /**
     * Creates a codec for {@link Pair} values from two component codecs.
     *
     * <p>The resulting codec sequences two codecs, where the second codec
     * operates on the remaining input from the first codec. This is useful for parsing sequential data where two values
     * appear one after another.</p>
     *
     * <h4>Behavior</h4>
     * <ul>
     *   <li><b>Encoding:</b> Encodes the first value, then encodes the second
     *       value into the result of the first encoding</li>
     *   <li><b>Decoding:</b> Decodes the first value, then decodes the second
     *       value from the remaining input of the first decode</li>
     * </ul>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * // Create a pair codec for (String, Integer)
     * Codec<Pair<String, Integer>> nameAgePair =
     *     Codecs.pair(Codecs.STRING, Codecs.INT);
     *
     * // Encode a pair
     * Pair<String, Integer> data = Pair.of("Alice", 30);
     * DataResult<JsonElement> json = nameAgePair.encodeStart(GsonOps.INSTANCE, data);
     *
     * // Decode a pair
     * DataResult<Pair<String, Integer>> decoded = nameAgePair.parse(
     *     GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @param first  the codec for the first element of the pair, must not be {@code null}
     * @param second the codec for the second element of the pair, must not be {@code null}
     * @param <F>    the type of the first element
     * @param <S>    the type of the second element
     * @return a new codec for {@code Pair<F, S>}, never {@code null}
     * @throws NullPointerException if {@code first} or {@code second} is {@code null}
     * @see Pair
     * @see Codec#pair(Codec, Codec)
     */
    @NotNull
    public static <F, S> Codec<Pair<F, S>> pair(@NotNull final Codec<F> first,
                                                @NotNull final Codec<S> second) {
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final Pair<F, S> input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix
            ) {
                return first.encode(Objects.requireNonNull(input.first()), ops, prefix)
                        .flatMap(t -> second.encode(Objects.requireNonNull(input.second()), ops, t));
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<Pair<F, S>, T>> decode(@NotNull final DynamicOps<T> ops,
                                                              @NotNull final T input) {
                return first.decode(ops, input).flatMap(p1 ->
                        second.decode(ops, Objects.requireNonNull(p1.second())).map(p2 ->
                                Pair.of(
                                        Pair.of(
                                                Objects.requireNonNull(p1.first()),
                                                Objects.requireNonNull(p2.first())
                                        ),
                                        Objects.requireNonNull(p2.second())
                                )
                        )
                );
            }
        };
    }

    // ==================== Bounded Codecs ====================

    /**
     * Creates an integer codec with bounds checking.
     *
     * <p>The resulting codec validates that integer values fall within the
     * specified range (inclusive on both ends). Values outside the range cause encoding and decoding to fail with an
     * error message.</p>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * // Create a codec for age (0-150)
     * Codec<Integer> ageCodec = Codecs.intRange(0, 150);
     *
     * // Valid value
     * DataResult<JsonElement> valid = ageCodec.encodeStart(GsonOps.INSTANCE, 25);
     * // Result: success(25)
     *
     * // Invalid value
     * DataResult<JsonElement> invalid = ageCodec.encodeStart(GsonOps.INSTANCE, -5);
     * // Result: error("Value -5 outside of range [0, 150]")
     * }</pre>
     *
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @return a new bounded integer codec, never {@code null}
     * @see #INT
     */
    @NotNull
    public static Codec<Integer> intRange(final int min,
                                          final int max) {
        return INT.flatXmap(
                value -> {
                    if (value < min || value > max) {
                        return DataResult.error("Value " + value + " outside of range [" + min + ", " + max + "]");
                    }
                    return DataResult.success(value);
                },
                value -> {
                    if (value < min || value > max) {
                        return DataResult.error("Value " + value + " outside of range [" + min + ", " + max + "]");
                    }
                    return DataResult.success(value);
                }
        );
    }

    /**
     * Creates a float codec with bounds checking.
     *
     * <p>The resulting codec validates that float values fall within the
     * specified range (inclusive on both ends). Values outside the range cause encoding and decoding to fail with an
     * error message.</p>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * // Create a codec for percentage (0.0 - 1.0)
     * Codec<Float> percentageCodec = Codecs.floatRange(0.0f, 1.0f);
     *
     * // Valid value
     * DataResult<JsonElement> valid = percentageCodec.encodeStart(GsonOps.INSTANCE, 0.5f);
     *
     * // Invalid value
     * DataResult<JsonElement> invalid = percentageCodec.encodeStart(GsonOps.INSTANCE, 1.5f);
     * // Result: error("Value 1.5 outside of range [0.0, 1.0]")
     * }</pre>
     *
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @return a new bounded float codec, never {@code null}
     * @see #FLOAT
     */
    @NotNull
    public static Codec<Float> floatRange(final float min,
                                          final float max) {
        return FLOAT.flatXmap(
                value -> {
                    if (value < min || value > max) {
                        return DataResult.error("Value " + value + " outside of range [" + min + ", " + max + "]");
                    }
                    return DataResult.success(value);
                },
                value -> {
                    if (value < min || value > max) {
                        return DataResult.error("Value " + value + " outside of range [" + min + ", " + max + "]");
                    }
                    return DataResult.success(value);
                }
        );
    }

    /**
     * Creates a double codec with bounds checking.
     *
     * <p>The resulting codec validates that double values fall within the
     * specified range (inclusive on both ends). Values outside the range cause encoding and decoding to fail with an
     * error message.</p>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * // Create a codec for latitude (-90.0 to 90.0)
     * Codec<Double> latitudeCodec = Codecs.doubleRange(-90.0, 90.0);
     *
     * // Valid value
     * DataResult<JsonElement> valid = latitudeCodec.encodeStart(GsonOps.INSTANCE, 51.5074);
     *
     * // Invalid value
     * DataResult<JsonElement> invalid = latitudeCodec.encodeStart(GsonOps.INSTANCE, 100.0);
     * // Result: error("Value 100.0 outside of range [-90.0, 90.0]")
     * }</pre>
     *
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @return a new bounded double codec, never {@code null}
     * @see #DOUBLE
     */
    @NotNull
    public static Codec<Double> doubleRange(final double min,
                                            final double max) {
        return DOUBLE.flatXmap(
                value -> {
                    if (value < min || value > max) {
                        return DataResult.error("Value " + value + " outside of range [" + min + ", " + max + "]");
                    }
                    return DataResult.success(value);
                },
                value -> {
                    if (value < min || value > max) {
                        return DataResult.error("Value " + value + " outside of range [" + min + ", " + max + "]");
                    }
                    return DataResult.success(value);
                }
        );
    }

    /**
     * Creates a string codec that only accepts non-empty strings.
     *
     * <p>The resulting codec validates that string values are not empty.
     * Empty strings cause encoding and decoding to fail with an error message.</p>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * // Create a non-empty string codec
     * Codec<String> nameCodec = Codecs.nonEmptyString();
     *
     * // Valid value
     * DataResult<JsonElement> valid = nameCodec.encodeStart(GsonOps.INSTANCE, "Alice");
     * // Result: success("Alice")
     *
     * // Invalid value
     * DataResult<JsonElement> invalid = nameCodec.encodeStart(GsonOps.INSTANCE, "");
     * // Result: error("String cannot be empty")
     * }</pre>
     *
     * @return a new non-empty string codec, never {@code null}
     * @see #STRING
     */
    @NotNull
    public static Codec<String> nonEmptyString() {
        return STRING.flatXmap(
                value -> value.isEmpty()
                        ? DataResult.error("String cannot be empty")
                        : DataResult.success(value),
                value -> value.isEmpty()
                        ? DataResult.error("String cannot be empty")
                        : DataResult.success(value)
        );
    }
}
