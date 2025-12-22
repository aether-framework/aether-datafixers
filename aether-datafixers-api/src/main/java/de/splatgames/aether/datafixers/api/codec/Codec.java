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

import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Either;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A bidirectional transformation between typed values and dynamic representations.
 *
 * <p>A {@code Codec} combines an {@link Encoder} and a {@link Decoder} to provide
 * complete serialization and deserialization capabilities. Codecs are the primary mechanism for converting between Java
 * objects and data formats like JSON, NBT, or any format supported by a {@link DynamicOps} implementation.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Bidirectional</b>: Encode Java objects to data, decode data back to objects</li>
 *   <li><b>Composable</b>: Combine simple codecs to build complex ones</li>
 *   <li><b>Format-agnostic</b>: Works with any {@link DynamicOps} implementation</li>
 *   <li><b>Error handling</b>: Uses {@link DataResult} for graceful error reporting</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Using primitive codecs
 * Codec<String> stringCodec = Codecs.STRING;
 * Codec<Integer> intCodec = Codecs.INT;
 *
 * // Encoding to JSON
 * DataResult<JsonElement> encoded = stringCodec.encodeStart(GsonOps.INSTANCE, "Hello");
 *
 * // Decoding from JSON
 * DataResult<String> decoded = stringCodec.parse(GsonOps.INSTANCE, jsonElement);
 *
 * // Transforming codecs with xmap
 * Codec<Integer> wrappedInt = Codecs.STRING.xmap(
 *     Integer::parseInt,     // decode: String -> Integer
 *     Object::toString       // encode: Integer -> String
 * );
 *
 * // Creating list codecs
 * Codec<List<String>> stringList = Codecs.STRING.listOf();
 *
 * // Creating field codecs for records
 * Codec<Person> personCodec = RecordCodecBuilder.create(instance ->
 *     instance.group(
 *         Codecs.STRING.fieldOf("name").forGetter(Person::name),
 *         Codecs.INT.fieldOf("age").forGetter(Person::age)
 *     ).apply(instance, Person::new)
 * );
 * }</pre>
 *
 * <h2>Codec Combinators</h2>
 * <p>Codecs provide several combinators for building complex codecs:</p>
 * <ul>
 *   <li>{@link #xmap(Function, Function)} - Transform values bidirectionally</li>
 *   <li>{@link #flatXmap(Function, Function)} - Transform with possible failure</li>
 *   <li>{@link #listOf()} - Create a list codec</li>
 *   <li>{@link #optionalOf()} - Create an optional codec</li>
 *   <li>{@link #fieldOf(String)} - Wrap as a named field</li>
 *   <li>{@link #orElse(Codec)} - Try alternative codec on failure</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Codec implementations should be stateless and thread-safe. The standard
 * codecs in {@link Codecs} are all thread-safe.</p>
 *
 * @param <A> the type of value this codec handles
 * @author Erik Pförtner
 * @see Encoder
 * @see Decoder
 * @see Codecs
 * @see MapCodec
 * @see RecordCodecBuilder
 * @since 0.1.0
 */
public interface Codec<A> extends Encoder<A>, Decoder<A> {

    // ==================== Core Operations ====================

    /**
     * Creates a codec from separate encoder and decoder implementations.
     *
     * <p>This factory method allows combining independent encoder and decoder
     * implementations into a single codec.</p>
     *
     * <pre>{@code
     * Codec<MyType> codec = Codec.of(myEncoder, myDecoder);
     * }</pre>
     *
     * @param encoder the encoder implementation, must not be {@code null}
     * @param decoder the decoder implementation, must not be {@code null}
     * @param <A>     the value type
     * @return a new codec combining the encoder and decoder, never {@code null}
     * @throws NullPointerException if {@code encoder} or {@code decoder} is {@code null}
     */
    @NotNull
    static <A> Codec<A> of(@NotNull final Encoder<A> encoder, @NotNull final Decoder<A> decoder) {
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final A input, @NotNull final DynamicOps<T> ops, @NotNull final T prefix) {
                return encoder.encode(input, ops, prefix);
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<A, T>> decode(@NotNull final DynamicOps<T> ops, @NotNull final T input) {
                return decoder.decode(ops, input);
            }
        };
    }

    /**
     * Creates a codec that always returns the given constant value on decode and produces no output on encode.
     *
     * <p>This is useful for providing default values or for codecs that represent constant data.</p>
     *
     * <pre>{@code
     * Codec<String> version = Codec.unit("1.0");
     * // Decodes to "1.0" regardless of input
     * }</pre>
     *
     * @param value the constant value to return on decode, must not be {@code null}
     * @param <A>   the value type
     * @return a codec that always decodes to the constant value, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @NotNull
    static <A> Codec<A> unit(@NotNull final A value) {
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final A input, @NotNull final DynamicOps<T> ops, @NotNull final T prefix) {
                return DataResult.success(prefix);
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<A, T>> decode(@NotNull final DynamicOps<T> ops, @NotNull final T input) {
                return DataResult.success(Pair.of(value, input));
            }
        };
    }

    // ==================== Convenience Methods ====================

    /**
     * Creates a codec for an {@link Either} type using two separate codecs.
     *
     * <p>The resulting codec attempts to decode using the left codec first,
     * then falls back to the right codec if the left fails.</p>
     *
     * <pre>{@code
     * Codec<Either<String, Integer>> eitherCodec = Codec.either(
     *     Codecs.STRING,
     *     Codecs.INT
     * );
     * }</pre>
     *
     * @param left  the codec for the left type, must not be {@code null}
     * @param right the codec for the right type, must not be {@code null}
     * @param <L>   the left type
     * @param <R>   the right type
     * @return a codec for {@code Either<L, R>}, never {@code null}
     * @throws NullPointerException if {@code left} or {@code right} is {@code null}
     */
    @NotNull
    static <L, R> Codec<Either<L, R>> either(@NotNull final Codec<L> left, @NotNull final Codec<R> right) {
        return Codecs.either(left, right);
    }

    /**
     * Creates a codec for a {@link Pair} type using two separate codecs.
     *
     * <p>The resulting codec encodes/decodes both values as a two-element structure.</p>
     *
     * <pre>{@code
     * Codec<Pair<String, Integer>> pairCodec = Codec.pair(
     *     Codecs.STRING,
     *     Codecs.INT
     * );
     * }</pre>
     *
     * @param first  the codec for the first value, must not be {@code null}
     * @param second the codec for the second value, must not be {@code null}
     * @param <F>    the first type
     * @param <S>    the second type
     * @return a codec for {@code Pair<F, S>}, never {@code null}
     * @throws NullPointerException if {@code first} or {@code second} is {@code null}
     */
    @NotNull
    static <F, S> Codec<Pair<F, S>> pair(@NotNull final Codec<F> first, @NotNull final Codec<S> second) {
        return Codecs.pair(first, second);
    }

    /**
     * Encodes a value to a dynamic representation, merging with the given prefix.
     *
     * <p>This is the core encoding method. Most users should use {@link #encodeStart(DynamicOps, Object)}
     * instead, which passes an empty prefix.</p>
     *
     * @param input  the value to encode, must not be {@code null}
     * @param ops    the dynamic operations for the target format, must not be {@code null}
     * @param prefix the prefix to merge the encoded value into, must not be {@code null}
     * @param <T>    the type of the dynamic representation (e.g., JsonElement)
     * @return a {@link DataResult} containing the encoded value or an error message
     */
    @NotNull
    @Override
    <T> DataResult<T> encode(@NotNull final A input,
                             @NotNull final DynamicOps<T> ops,
                             @NotNull final T prefix);

    /**
     * Decodes a value from a dynamic representation.
     *
     * <p>This is the core decoding method. The result includes both the decoded value
     * and any remaining unconsumed input. Most users should use {@link #parse(DynamicOps, Object)} which discards the
     * remaining input.</p>
     *
     * @param ops   the dynamic operations for the source format, must not be {@code null}
     * @param input the input to decode, must not be {@code null}
     * @param <T>   the type of the dynamic representation (e.g., JsonElement)
     * @return a {@link DataResult} containing a pair of the decoded value and remaining input, or an error message
     */
    @NotNull
    @Override
    <T> DataResult<Pair<A, T>> decode(@NotNull final DynamicOps<T> ops,
                                      @NotNull final T input);

    // ==================== Combinators ====================

    /**
     * Encodes a value to a dynamic representation starting from an empty prefix.
     *
     * <p>This is the recommended encoding method for most use cases. It creates
     * a fresh encoding without merging into existing data.</p>
     *
     * <pre>{@code
     * Codec<String> codec = Codecs.STRING;
     * DataResult<JsonElement> result = codec.encodeStart(GsonOps.INSTANCE, "hello");
     * // result = Success[JsonPrimitive("hello")]
     * }</pre>
     *
     * @param ops   the dynamic operations for the target format, must not be {@code null}
     * @param input the value to encode, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return a {@link DataResult} containing the encoded value or an error message
     */
    @NotNull
    default <T> DataResult<T> encodeStart(@NotNull final DynamicOps<T> ops,
                                          @NotNull final A input) {
        return encode(input, ops, ops.empty());
    }

    /**
     * Decodes a value from a dynamic representation, discarding any remaining input.
     *
     * <p>This is the recommended decoding method for most use cases. Unlike
     * {@link #decode(DynamicOps, Object)}, this method returns only the decoded value without the remaining input.</p>
     *
     * <pre>{@code
     * Codec<Integer> codec = Codecs.INT;
     * DataResult<Integer> result = codec.parse(GsonOps.INSTANCE, new JsonPrimitive(42));
     * // result = Success[42]
     * }</pre>
     *
     * @param ops   the dynamic operations for the source format, must not be {@code null}
     * @param input the input to decode, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return a {@link DataResult} containing the decoded value or an error message
     */
    @NotNull
    default <T> DataResult<A> parse(@NotNull final DynamicOps<T> ops,
                                    @NotNull final T input) {
        return decode(ops, input).map(Pair::first);
    }

    /**
     * Decodes a value from a {@link Dynamic} wrapper.
     *
     * <p>This is a convenience method that extracts the ops and value from the Dynamic.</p>
     *
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     * DataResult<String> result = Codecs.STRING.parse(dynamic);
     * }</pre>
     *
     * @param input the Dynamic wrapper containing the value to decode, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return a {@link DataResult} containing the decoded value or an error message
     */
    @NotNull
    default <T> DataResult<A> parse(@NotNull final Dynamic<T> input) {
        return parse(input.ops(), input.value());
    }

    /**
     * Encodes a value and wraps it in a {@link Dynamic}.
     *
     * <p>This is useful when you need the encoded value wrapped with its ops for further processing.</p>
     *
     * <pre>{@code
     * DataResult<Dynamic<JsonElement>> result = Codecs.STRING.encodeStartDynamic(
     *     GsonOps.INSTANCE, "hello"
     * );
     * // result = Success[Dynamic(GsonOps, "hello")]
     * }</pre>
     *
     * @param ops   the dynamic operations for the target format, must not be {@code null}
     * @param input the value to encode, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return a {@link DataResult} containing the encoded {@link Dynamic} or an error message
     */
    @NotNull
    default <T> DataResult<Dynamic<T>> encodeStartDynamic(@NotNull final DynamicOps<T> ops,
                                                          @NotNull final A input) {
        return encodeStart(ops, input).map(v -> new Dynamic<>(ops, v));
    }

    /**
     * Transforms this codec to handle a different type using bidirectional mapping functions.
     *
     * <p>This is the primary method for adapting codecs to new types. The {@code to} function
     * transforms decoded values from A to B, and the {@code from} function transforms values from B back to A for
     * encoding.</p>
     *
     * <pre>{@code
     * // Convert a string codec to an integer codec
     * Codec<Integer> intFromString = Codecs.STRING.xmap(
     *     Integer::parseInt,     // String -> Integer (decode)
     *     Object::toString       // Integer -> String (encode)
     * );
     *
     * // Wrap a value in a container type
     * Codec<UserId> userIdCodec = Codecs.STRING.xmap(
     *     UserId::new,           // String -> UserId
     *     UserId::value          // UserId -> String
     * );
     * }</pre>
     *
     * @param to   the function to apply after decoding (A to B)
     * @param from the function to apply before encoding (B to A)
     * @param <B>  the new value type
     * @return a new codec for type B, never {@code null}
     * @throws NullPointerException if {@code to} or {@code from} is {@code null}
     */
    @NotNull
    default <B> Codec<B> xmap(@NotNull final Function<? super A, ? extends B> to,
                              @NotNull final Function<? super B, ? extends A> from) {
        final Codec<A> self = this;
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final B input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                return self.encode(from.apply(input), ops, prefix);
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<B, T>> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
                return self.decode(ops, input).map(p -> Pair.of(to.apply(p.first()), p.second()));
            }
        };
    }

    /**
     * Transforms this codec using bidirectional mapping functions that may fail.
     *
     * <p>Unlike {@link #xmap(Function, Function)}, both transformation functions return
     * {@link DataResult}, allowing them to report errors. This is useful for validations or conversions that may not
     * always succeed.</p>
     *
     * <pre>{@code
     * // Parse a positive integer from a string
     * Codec<Integer> positiveInt = Codecs.STRING.flatXmap(
     *     str -> {
     *         try {
     *             int value = Integer.parseInt(str);
     *             return value > 0
     *                 ? DataResult.success(value)
     *                 : DataResult.error("Must be positive: " + value);
     *         } catch (NumberFormatException e) {
     *             return DataResult.error("Invalid number: " + str);
     *         }
     *     },
     *     num -> DataResult.success(num.toString())
     * );
     * }</pre>
     *
     * @param to   the function to apply after decoding, returning a DataResult
     * @param from the function to apply before encoding, returning a DataResult
     * @param <B>  the new value type
     * @return a new codec for type B, never {@code null}
     * @throws NullPointerException if {@code to} or {@code from} is {@code null}
     */
    @NotNull
    default <B> Codec<B> flatXmap(@NotNull final Function<? super A, ? extends DataResult<? extends B>> to,
                                  @NotNull final Function<? super B, ? extends DataResult<? extends A>> from) {
        final Codec<A> self = this;
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final B input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                return from.apply(input).flatMap(a -> self.encode(a, ops, prefix));
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<B, T>> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
                return self.decode(ops, input).flatMap(p ->
                        to.apply(p.first()).map(b -> Pair.of(b, p.second()))
                );
            }
        };
    }

    /**
     * Transforms this codec with a failable decode and infallible encode function.
     *
     * <p>The decode transformation ({@code to}) may fail, but the encode transformation
     * ({@code from}) is guaranteed to succeed. Use this when validation is only needed during decoding.</p>
     *
     * @param to   the function to apply after decoding, returning a DataResult
     * @param from the function to apply before encoding (guaranteed to succeed)
     * @param <B>  the new value type
     * @return a new codec for type B, never {@code null}
     * @throws NullPointerException if {@code to} or {@code from} is {@code null}
     */
    @NotNull
    default <B> Codec<B> comapFlatMap(@NotNull final Function<? super A, ? extends DataResult<? extends B>> to,
                                      @NotNull final Function<? super B, ? extends A> from) {
        final Codec<A> self = this;
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final B input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                return self.encode(from.apply(input), ops, prefix);
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<B, T>> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
                return self.decode(ops, input).flatMap(p ->
                        to.apply(p.first()).map(b -> Pair.of(b, p.second()))
                );
            }
        };
    }

    /**
     * Transforms this codec with an infallible decode and failable encode function.
     *
     * <p>The decode transformation ({@code to}) is guaranteed to succeed, but the encode
     * transformation ({@code from}) may fail. Use this when validation is only needed during encoding.</p>
     *
     * @param to   the function to apply after decoding (guaranteed to succeed)
     * @param from the function to apply before encoding, returning a DataResult
     * @param <B>  the new value type
     * @return a new codec for type B, never {@code null}
     * @throws NullPointerException if {@code to} or {@code from} is {@code null}
     */
    @NotNull
    default <B> Codec<B> flatComapMap(@NotNull final Function<? super A, ? extends B> to,
                                      @NotNull final Function<? super B, ? extends DataResult<? extends A>> from) {
        final Codec<A> self = this;
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final B input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                return from.apply(input).flatMap(a -> self.encode(a, ops, prefix));
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<B, T>> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
                return self.decode(ops, input).map(p -> Pair.of(to.apply(p.first()), p.second()));
            }
        };
    }

    /**
     * Creates a codec that handles lists of elements using this codec for each element.
     *
     * <pre>{@code
     * Codec<List<String>> stringList = Codecs.STRING.listOf();
     *
     * List<String> decoded = stringList.parse(GsonOps.INSTANCE, jsonArray)
     *     .getOrThrow(IllegalStateException::new);
     * }</pre>
     *
     * @return a codec for {@code List<A>}, never {@code null}
     */
    @NotNull
    default Codec<List<A>> listOf() {
        return Codecs.list(this);
    }

    /**
     * Creates a codec that handles optional values using this codec.
     *
     * <p>When decoding, if the input is null or absent, returns {@code Optional.empty()}.
     * When encoding, {@code Optional.empty()} produces a null/absent value.</p>
     *
     * @return a codec for {@code Optional<A>}, never {@code null}
     */
    @NotNull
    default Codec<Optional<A>> optionalOf() {
        return Codecs.optional(this);
    }

    /**
     * Creates a {@link MapCodec} that expects this codec's value in a named field.
     *
     * <p>This is the primary way to build record codecs. The resulting MapCodec
     * encodes/decodes the value as a field within an object/map structure.</p>
     *
     * <pre>{@code
     * MapCodec<String> nameField = Codecs.STRING.fieldOf("name");
     *
     * // Used in RecordCodecBuilder:
     * Codec<Person> personCodec = RecordCodecBuilder.create(instance ->
     *     instance.group(
     *         Codecs.STRING.fieldOf("name").forGetter(Person::name),
     *         Codecs.INT.fieldOf("age").forGetter(Person::age)
     *     ).apply(instance, Person::new)
     * );
     * }</pre>
     *
     * @param name the field name, must not be {@code null}
     * @return a MapCodec for this codec, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @NotNull
    default MapCodec<A> fieldOf(@NotNull final String name) {
        final Codec<A> self = this;
        return new MapCodec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final A input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T map) {
                return self.encodeStart(ops, input).flatMap(v ->
                        ops.mergeToMap(map, ops.createString(name), v));
            }

            @NotNull
            @Override
            public <T> DataResult<A> decode(@NotNull final DynamicOps<T> ops,
                                            @NotNull final T input) {
                final T field = ops.get(input, name);
                if (field == null) {
                    return DataResult.error("Missing field: " + name);
                }
                return self.parse(ops, field);
            }
        };
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a {@link MapCodec} for an optional field with the given name.
     *
     * <p>When decoding, if the field is missing, returns {@code Optional.empty()}.
     * When encoding {@code Optional.empty()}, the field is omitted entirely.</p>
     *
     * <pre>{@code
     * MapCodec<Optional<String>> optionalNickname = Codecs.STRING.optionalFieldOf("nickname");
     * }</pre>
     *
     * @param name the field name, must not be {@code null}
     * @return a MapCodec for {@code Optional<A>}, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @NotNull
    default MapCodec<Optional<A>> optionalFieldOf(@NotNull final String name) {
        final Codec<A> self = this;
        return new MapCodec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final Optional<A> input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T map) {
                return input.map(a -> self.encodeStart(ops, a)
                                .flatMap(v -> ops.mergeToMap(map, ops.createString(name), v)))
                        .orElseGet(() -> DataResult.success(map));
            }

            @NotNull
            @Override
            public <T> DataResult<Optional<A>> decode(@NotNull final DynamicOps<T> ops,
                                                      @NotNull final T input) {
                final T field = ops.get(input, name);
                if (field == null) {
                    return DataResult.success(Optional.empty());
                }
                return self.parse(ops, field).map(Optional::of);
            }
        };
    }

    /**
     * Creates a {@link MapCodec} for an optional field with a default value.
     *
     * <p>When decoding, if the field is missing, returns the default value.
     * When encoding, if the value equals the default, the field may be omitted.</p>
     *
     * <pre>{@code
     * MapCodec<Integer> timeoutField = Codecs.INT.optionalFieldOf("timeout", 30);
     * // Missing field decodes to 30
     * }</pre>
     *
     * @param name         the field name, must not be {@code null}
     * @param defaultValue the default value if the field is missing, must not be {@code null}
     * @return a MapCodec for this type, never {@code null}
     * @throws NullPointerException if {@code name} or {@code defaultValue} is {@code null}
     */
    @NotNull
    default MapCodec<A> optionalFieldOf(@NotNull final String name,
                                        @NotNull final A defaultValue) {
        return optionalFieldOf(name).xmap(
                o -> o.orElse(defaultValue),
                a -> a.equals(defaultValue) ? Optional.empty() : Optional.of(a)
        );
    }

    /**
     * Wraps error messages with additional context for debugging.
     *
     * <p>This is useful for providing better error messages that indicate where
     * in the data structure an error occurred.</p>
     *
     * <pre>{@code
     * Codec<Person> personCodec = baseCodec.withErrorContext("person");
     * // Errors will be prefixed with "person: "
     * }</pre>
     *
     * @param name the context name to prefix error messages with
     * @return a codec with wrapped error messages, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    @NotNull
    default Codec<A> withErrorContext(@NotNull final String name) {
        final Codec<A> self = this;
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final A input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                return self.encode(input, ops, prefix).mapError(e -> name + ": " + e);
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<A, T>> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
                return self.decode(ops, input).mapError(e -> name + ": " + e);
            }
        };
    }

    /**
     * Creates a codec that tries this codec first, falling back to another codec on failure.
     *
     * <p>This is useful for supporting multiple data formats or providing fallback parsing strategies.</p>
     *
     * <pre>{@code
     * // Try parsing as ISO date, fall back to timestamp
     * Codec<LocalDate> dateCodec = isoDateCodec.orElse(timestampCodec);
     * }</pre>
     *
     * @param other the fallback codec to try if this codec fails
     * @return a codec that tries this codec first, then the other, never {@code null}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @NotNull
    default Codec<A> orElse(@NotNull final Codec<A> other) {
        final Codec<A> self = this;
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final A input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                final DataResult<T> result = self.encode(input, ops, prefix);
                if (result.isSuccess()) {
                    return result;
                }
                return other.encode(input, ops, prefix);
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<A, T>> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
                final DataResult<Pair<A, T>> result = self.decode(ops, input);
                if (result.isSuccess()) {
                    return result;
                }
                return other.decode(ops, input);
            }
        };
    }
}
