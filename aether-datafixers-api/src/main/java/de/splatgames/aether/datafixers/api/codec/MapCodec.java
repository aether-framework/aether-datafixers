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
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * A codec that encodes and decodes values within a map/object structure.
 *
 * <p>{@code MapCodec} differs from {@link Codec} in that it operates on fields within
 * a larger map structure rather than standalone values. This is the foundation for building record codecs where each
 * field is encoded/decoded as a named entry in a map (JSON object, NBT compound, etc.).</p>
 *
 * <h2>Key Differences from Codec</h2>
 * <ul>
 *   <li>{@link Codec} encodes/decodes complete values (e.g., a JSON primitive or object)</li>
 *   <li>{@code MapCodec} encodes/decodes fields within an existing map structure</li>
 *   <li>{@code MapCodec} is typically created via {@link Codec#fieldOf(String)}</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a MapCodec for a named field
 * MapCodec<String> nameField = Codecs.STRING.fieldOf("name");
 *
 * // Use with RecordCodecBuilder
 * Codec<Person> personCodec = RecordCodecBuilder.create(instance ->
 *     instance.group(
 *         Codecs.STRING.fieldOf("name").forGetter(Person::name),
 *         Codecs.INT.fieldOf("age").forGetter(Person::age)
 *     ).apply(instance, Person::new)
 * );
 *
 * // Convert to a regular Codec
 * Codec<String> nameCodec = nameField.codec();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>MapCodec implementations should be stateless and thread-safe.</p>
 *
 * @param <A> the type of value this codec handles
 * @author Erik Pförtner
 * @see Codec
 * @see Codec#fieldOf(String)
 * @see RecordCodecBuilder
 * @since 0.1.0
 */
public interface MapCodec<A> {

    /**
     * Creates a MapCodec from separate encoder and decoder functions.
     *
     * <p>This factory method allows constructing a MapCodec from two separate
     * functional interfaces, which is useful when encoding and decoding logic are defined independently or when
     * adapting existing code.</p>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * // Create a custom MapCodec for a "status" field
     * MapCodec<Status> statusCodec = MapCodec.of(
     *     // Encoder: Status -> map with "status" field
     *     (status, ops, map) -> ops.mergeToMap(map,
     *         ops.createString("status"),
     *         ops.createString(status.name())),
     *
     *     // Decoder: map -> Status
     *     (ops, input) -> ops.get(input, "status")
     *         .flatMap(ops::getStringValue)
     *         .map(Status::valueOf)
     * );
     * }</pre>
     *
     * @param encoder the function to encode values into a map, must not be {@code null}
     * @param decoder the function to decode values from a map, must not be {@code null}
     * @param <A>     the type of value this codec handles
     * @return a new MapCodec combining the encoder and decoder, never {@code null}
     * @throws NullPointerException if {@code encoder} or {@code decoder} is {@code null}
     * @see MapEncoder
     * @see MapDecoder
     */
    @NotNull
    static <A> MapCodec<A> of(@NotNull final MapEncoder<A> encoder,
                              @NotNull final MapDecoder<A> decoder) {
        return new MapCodec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final A input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T map) {
                return encoder.encode(input, ops, map);
            }

            @NotNull
            @Override
            public <T> DataResult<A> decode(@NotNull final DynamicOps<T> ops,
                                            @NotNull final T input) {
                return decoder.decode(ops, input);
            }
        };
    }

    /**
     * Encodes a value into a map structure.
     *
     * <p>Unlike {@link Codec#encode(Object, DynamicOps, Object)}, this method merges
     * the encoded field(s) into an existing map rather than producing a standalone value.</p>
     *
     * @param input the value to encode, must not be {@code null}
     * @param ops   the dynamic operations for the target format, must not be {@code null}
     * @param map   the map to encode into, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return a {@link DataResult} containing the updated map or an error message
     */
    @NotNull
    <T> DataResult<T> encode(@NotNull final A input,
                             @NotNull final DynamicOps<T> ops,
                             @NotNull final T map);

    /**
     * Decodes a value from a map structure.
     *
     * <p>Unlike {@link Codec#decode(DynamicOps, Object)}, this method reads from
     * a map structure and does not return remaining input.</p>
     *
     * @param ops   the dynamic operations for the source format, must not be {@code null}
     * @param input the map to decode from, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return a {@link DataResult} containing the decoded value or an error message
     */
    @NotNull
    <T> DataResult<A> decode(@NotNull final DynamicOps<T> ops,
                             @NotNull final T input);

    /**
     * Transforms this codec to handle a different type using bidirectional mapping functions.
     *
     * @param to   the function to apply after decoding (A to B)
     * @param from the function to apply before encoding (B to A)
     * @param <B>  the new value type
     * @return a new MapCodec for type B, never {@code null}
     * @throws NullPointerException if {@code to} or {@code from} is {@code null}
     */
    @NotNull
    default <B> MapCodec<B> xmap(@NotNull final Function<? super A, ? extends B> to,
                                 @NotNull final Function<? super B, ? extends A> from) {
        final MapCodec<A> self = this;
        return new MapCodec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final B input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T map) {
                return self.encode(from.apply(input), ops, map);
            }

            @NotNull
            @Override
            public <T> DataResult<B> decode(@NotNull final DynamicOps<T> ops,
                                            @NotNull final T input) {
                return self.decode(ops, input).map(to);
            }
        };
    }

    /**
     * Transforms this codec using bidirectional mapping functions that may fail.
     *
     * <p>Unlike {@link #xmap(Function, Function)}, both transformation functions return
     * {@link DataResult}, allowing them to report errors.</p>
     *
     * @param to   the function to apply after decoding, returning a DataResult
     * @param from the function to apply before encoding, returning a DataResult
     * @param <B>  the new value type
     * @return a new MapCodec for type B, never {@code null}
     * @throws NullPointerException if {@code to} or {@code from} is {@code null}
     */
    @NotNull
    @SuppressWarnings("unchecked")
    default <B> MapCodec<B> flatXmap(@NotNull final Function<? super A, ? extends DataResult<? extends B>> to,
                                     @NotNull final Function<? super B, ? extends DataResult<? extends A>> from) {
        final MapCodec<A> self = this;
        return new MapCodec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final B input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T map) {
                return from.apply(input).flatMap(a -> self.encode(a, ops, map));
            }

            @NotNull
            @Override
            public <T> DataResult<B> decode(@NotNull final DynamicOps<T> ops,
                                            @NotNull final T input) {
                return self.decode(ops, input).flatMap(a -> (DataResult<B>) to.apply(a));
            }
        };
    }

    /**
     * Creates a field descriptor for use with {@link RecordCodecBuilder}.
     *
     * <p>This method binds a getter function to this MapCodec, creating a field
     * that can extract a value from a record for encoding and provide the codec for decoding. This is the standard way
     * to integrate MapCodecs into record codec definitions.</p>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * // Define a Person record
     * record Person(String name, int age) {}
     *
     * // Create a codec using forGetter
     * Codec<Person> personCodec = RecordCodecBuilder.create(instance ->
     *     instance.group(
     *         Codecs.STRING.fieldOf("name").forGetter(Person::name),
     *         Codecs.INT.fieldOf("age").forGetter(Person::age)
     *     ).apply(instance, Person::new)
     * );
     * }</pre>
     *
     * @param getter the function to extract the field value from the record type, must not be {@code null}
     * @param <O>    the record/object type that contains this field
     * @return a {@link RecordCodecBuilder.Field} that pairs this codec with the getter, never {@code null}
     * @throws NullPointerException if {@code getter} is {@code null}
     * @see RecordCodecBuilder
     * @see RecordCodecBuilder.Field
     */
    @NotNull
    default <O> RecordCodecBuilder.Field<O, A> forGetter(@NotNull final Function<O, A> getter) {
        return new RecordCodecBuilder.Field<>(this, getter);
    }

    /**
     * Converts this MapCodec to a regular {@link Codec}.
     *
     * <p>The resulting codec wraps this MapCodec's field-based operations into
     * a standalone codec. When encoding, it creates a new empty map and encodes into it. When decoding, it passes the
     * input map directly to this MapCodec and returns the original input as remaining data.</p>
     *
     * <h4>Behavior</h4>
     * <ul>
     *   <li><b>Encoding:</b> Creates an empty map via {@link DynamicOps#emptyMap()},
     *       then encodes this field into it</li>
     *   <li><b>Decoding:</b> Decodes from the input and pairs the result with
     *       the original input (unconsumed)</li>
     * </ul>
     *
     * <h4>Usage Example</h4>
     * <pre>{@code
     * // Convert a named field MapCodec to a regular Codec
     * MapCodec<String> nameField = Codecs.STRING.fieldOf("name");
     * Codec<String> nameCodec = nameField.codec();
     *
     * // The codec now encodes/decodes as a map with a single "name" field
     * // Encoding: "Alice" -> {"name": "Alice"}
     * // Decoding: {"name": "Alice"} -> "Alice"
     * }</pre>
     *
     * @return a {@link Codec} that uses this MapCodec for encoding and decoding, never {@code null}
     * @see Codec
     */
    @NotNull
    default Codec<A> codec() {
        final MapCodec<A> self = this;
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final A input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                return self.encode(input, ops, ops.emptyMap());
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<A, T>> decode(@NotNull final DynamicOps<T> ops,
                                                     @NotNull final T input) {
                return self.decode(ops, input).map(a -> Pair.of(a, input));
            }
        };
    }

    /**
     * Functional interface for encoding a value into a map structure.
     *
     * <p>A {@code MapEncoder} is responsible for adding fields to an existing
     * map representation. Unlike {@link Encoder}, which produces standalone values, a MapEncoder merges encoded data
     * into an existing map.</p>
     *
     * <p><b>Implementation Notes</b></p>
     * <p>Implementations should use {@link DynamicOps#mergeToMap(Object, Object, Object)}
     * or similar methods to add fields to the provided map rather than creating new standalone structures.</p>
     *
     * @param <A> the type of value this encoder handles
     * @see MapCodec#of(MapEncoder, MapDecoder)
     * @see Encoder
     */
    @FunctionalInterface
    interface MapEncoder<A> {
        /**
         * Encodes a value into the given map structure.
         *
         * @param input the value to encode, must not be {@code null}
         * @param ops   the dynamic operations for the target format, must not be {@code null}
         * @param map   the map to encode into, must not be {@code null}
         * @param <T>   the type of the dynamic representation
         * @return a {@link DataResult} containing the updated map with the encoded field(s), or an error message if
         * encoding fails
         */
        @NotNull
        <T> DataResult<T> encode(@NotNull final A input,
                                 @NotNull final DynamicOps<T> ops,
                                 @NotNull final T map);
    }

    /**
     * Functional interface for decoding a value from a map structure.
     *
     * <p>A {@code MapDecoder} is responsible for extracting and decoding fields
     * from a map representation. Unlike {@link Decoder}, which handles standalone values and returns remaining input, a
     * MapDecoder operates on specific fields within a map and does not track consumed input.</p>
     *
     * <p><b>Implementation Notes</b></p>
     * <p>Implementations should use {@link DynamicOps#get(Object, String)} or
     * similar methods to extract specific fields from the input map.</p>
     *
     * @param <A> the type of value this decoder produces
     * @see MapCodec#of(MapEncoder, MapDecoder)
     * @see Decoder
     */
    @FunctionalInterface
    interface MapDecoder<A> {
        /**
         * Decodes a value from the given map structure.
         *
         * @param ops   the dynamic operations for the source format, must not be {@code null}
         * @param input the map to decode from, must not be {@code null}
         * @param <T>   the type of the dynamic representation
         * @return a {@link DataResult} containing the decoded value, or an error message if decoding fails
         */
        @NotNull
        <T> DataResult<A> decode(@NotNull final DynamicOps<T> ops,
                                 @NotNull final T input);
    }
}
