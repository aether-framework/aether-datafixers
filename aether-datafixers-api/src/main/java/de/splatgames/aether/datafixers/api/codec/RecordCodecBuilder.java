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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A builder for creating codecs for record/object types using a declarative, applicative-style API.
 *
 * <p>{@code RecordCodecBuilder} provides a type-safe way to define codecs for complex
 * objects with multiple fields. It uses an applicative functor pattern where fields are grouped together and a
 * constructor function is applied to create instances.</p>
 *
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><b>Field:</b> A {@link MapCodec} paired with a getter function via
 *       {@link MapCodec#forGetter(Function)}</li>
 *   <li><b>Instance:</b> The builder context that provides {@code group()} methods
 *       to collect fields</li>
 *   <li><b>Builder:</b> An intermediate object that holds grouped fields and provides
 *       an {@code apply()} method to create the final codec</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * // 1. Define a record type
 * record Person(String name, int age, String email) {}
 *
 * // 2. Create a codec using RecordCodecBuilder
 * Codec<Person> PERSON_CODEC = RecordCodecBuilder.create(instance ->
 *     instance.group(
 *         Codecs.STRING.fieldOf("name").forGetter(Person::name),
 *         Codecs.INT.fieldOf("age").forGetter(Person::age),
 *         Codecs.STRING.fieldOf("email").forGetter(Person::email)
 *     ).apply(instance, Person::new)
 * );
 *
 * // 3. Use the codec
 * DataResult<JsonElement> json = PERSON_CODEC.encodeStart(GsonOps.INSTANCE, person);
 * DataResult<Person> decoded = PERSON_CODEC.parse(GsonOps.INSTANCE, jsonElement);
 * }</pre>
 *
 * <h2>Optional Fields</h2>
 * <p>Use {@link Codec#optionalFieldOf(String)} for optional fields with default values:</p>
 * <pre>{@code
 * Codec<Config> CONFIG_CODEC = RecordCodecBuilder.create(instance ->
 *     instance.group(
 *         Codecs.STRING.fieldOf("name").forGetter(Config::name),
 *         Codecs.INT.optionalFieldOf("timeout", 30).forGetter(Config::timeout)
 *     ).apply(instance, Config::new)
 * );
 * }</pre>
 *
 * <h2>Supported Field Counts</h2>
 * <p>The builder supports records with 1 to 6 fields via the corresponding
 * {@link Builder1} through {@link Builder6} classes. For records with more fields,
 * consider using nested records or custom codec implementations.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link #create(Function)} method and resulting codecs are thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see Codec
 * @see MapCodec
 * @see Field
 * @since 0.1.0
 */
public final class RecordCodecBuilder {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class is a utility class with only static factory methods.</p>
     */
    private RecordCodecBuilder() {
        // private constructor to prevent instantiation
    }

    /**
     * Creates a {@link Codec} for a record type using the builder pattern.
     *
     * <p>This is the primary entry point for creating record codecs. The builder
     * function receives an {@link Instance} which provides {@code group()} methods to collect fields, and returns a
     * {@link MapCodec} that is automatically converted to a regular {@link Codec}.</p>
     *
     * <p><b>Example</b></p>
     * <pre>{@code
     * record Person(String name, int age) {}
     *
     * Codec<Person> codec = RecordCodecBuilder.create(instance ->
     *     instance.group(
     *         Codecs.STRING.fieldOf("name").forGetter(Person::name),
     *         Codecs.INT.fieldOf("age").forGetter(Person::age)
     *     ).apply(instance, Person::new)
     * );
     * }</pre>
     *
     * @param builder the builder function that defines fields and their mapping, must not be {@code null}
     * @param <O>     the record/object type to create a codec for
     * @return a new {@link Codec} for the record type, never {@code null}
     * @throws NullPointerException if {@code builder} is {@code null}
     * @see #mapCodec(Function)
     */
    @NotNull
    public static <O> Codec<O> create(@NotNull final Function<Instance<O>, MapCodec<O>> builder) {
        Preconditions.checkNotNull(builder, "builder must not be null");
        return builder.apply(new Instance<>()).codec();
    }

    /**
     * Creates a {@link MapCodec} for a record type using the builder pattern.
     *
     * <p>This method is similar to {@link #create(Function)} but returns a
     * {@link MapCodec} instead of a {@link Codec}. This is useful when you need to nest the record codec as a field
     * within another record codec.</p>
     *
     * <p><b>Example</b></p>
     * <pre>{@code
     * record Address(String street, String city) {}
     * record Person(String name, Address address) {}
     *
     * // Create MapCodec for Address (can be used as nested field)
     * MapCodec<Address> addressMapCodec = RecordCodecBuilder.mapCodec(instance ->
     *     instance.group(
     *         Codecs.STRING.fieldOf("street").forGetter(Address::street),
     *         Codecs.STRING.fieldOf("city").forGetter(Address::city)
     *     ).apply(instance, Address::new)
     * );
     *
     * // Use it as a nested field in Person codec
     * Codec<Person> personCodec = RecordCodecBuilder.create(instance ->
     *     instance.group(
     *         Codecs.STRING.fieldOf("name").forGetter(Person::name),
     *         addressMapCodec.forGetter(Person::address)
     *     ).apply(instance, Person::new)
     * );
     * }</pre>
     *
     * @param builder the builder function that defines fields and their mapping, must not be {@code null}
     * @param <O>     the record/object type to create a MapCodec for
     * @return a new {@link MapCodec} for the record type, never {@code null}
     * @throws NullPointerException if {@code builder} is {@code null}
     * @see #create(Function)
     */
    @NotNull
    public static <O> MapCodec<O> mapCodec(@NotNull final Function<Instance<O>, MapCodec<O>> builder) {
        Preconditions.checkNotNull(builder, "builder must not be null");
        return builder.apply(new Instance<>());
    }

    /**
     * A function that takes three arguments and produces a result.
     *
     * @param <A> the type of the first argument
     * @param <B> the type of the second argument
     * @param <C> the type of the third argument
     * @param <R> the type of the result
     */
    @FunctionalInterface
    public interface Function3<A, B, C, R> {
        /**
         * Applies this function to the given arguments.
         *
         * @param a the first argument
         * @param b the second argument
         * @param c the third argument
         * @return the function result
         */
        R apply(A a, B b, C c);
    }

    /**
     * A function that takes four arguments and produces a result.
     *
     * @param <A> the type of the first argument
     * @param <B> the type of the second argument
     * @param <C> the type of the third argument
     * @param <D> the type of the fourth argument
     * @param <R> the type of the result
     */
    @FunctionalInterface
    public interface Function4<A, B, C, D, R> {
        /**
         * Applies this function to the given arguments.
         *
         * @param a the first argument
         * @param b the second argument
         * @param c the third argument
         * @param d the fourth argument
         * @return the function result
         */
        R apply(A a, B b, C c, D d);
    }

    // ==================== Builder Classes ====================

    /**
     * A function that takes five arguments and produces a result.
     *
     * @param <A> the type of the first argument
     * @param <B> the type of the second argument
     * @param <C> the type of the third argument
     * @param <D> the type of the fourth argument
     * @param <E> the type of the fifth argument
     * @param <R> the type of the result
     */
    @FunctionalInterface
    public interface Function5<A, B, C, D, E, R> {
        /**
         * Applies this function to the given arguments.
         *
         * @param a the first argument
         * @param b the second argument
         * @param c the third argument
         * @param d the fourth argument
         * @param e the fifth argument
         * @return the function result
         */
        R apply(A a, B b, C c, D d, E e);
    }

    /**
     * A function that takes six arguments and produces a result.
     *
     * @param <A> the type of the first argument
     * @param <B> the type of the second argument
     * @param <C> the type of the third argument
     * @param <D> the type of the fourth argument
     * @param <E> the type of the fifth argument
     * @param <F> the type of the sixth argument
     * @param <R> the type of the result
     */
    @FunctionalInterface
    public interface Function6<A, B, C, D, E, F, R> {
        /**
         * Applies this function to the given arguments.
         *
         * @param a the first argument
         * @param b the second argument
         * @param c the third argument
         * @param d the fourth argument
         * @param e the fifth argument
         * @param f the sixth argument
         * @return the function result
         */
        R apply(A a, B b, C c, D d, E e, F f);
    }

    /**
     * A field definition pairing a {@link MapCodec} with a getter function.
     *
     * <p>A {@code Field} represents a single field in a record codec. It contains
     * both the codec for encoding/decoding the field value and a getter function for extracting the field value from
     * the record during encoding.</p>
     *
     * <p>Fields are typically created using {@link MapCodec#forGetter(Function)}
     * rather than directly instantiating this record.</p>
     *
     * <p><b>Example</b></p>
     * <pre>{@code
     * // Create a field using forGetter (preferred)
     * Field<Person, String> nameField = Codecs.STRING.fieldOf("name").forGetter(Person::name);
     *
     * // Or create directly (less common)
     * Field<Person, Integer> ageField = new Field<>(
     *     Codecs.INT.fieldOf("age"),
     *     Person::age
     * );
     * }</pre>
     *
     * @param codec  the MapCodec for encoding/decoding the field value, must not be {@code null}
     * @param getter the function to extract the field value from the record, must not be {@code null}
     * @param <O>    the record/object type containing this field
     * @param <A>    the type of the field value
     * @see MapCodec#forGetter(Function)
     */
    public record Field<O, A>(@NotNull MapCodec<A> codec,
                              @NotNull Function<O, A> getter) {
        /**
         * Creates a new Field with validation.
         *
         * @throws NullPointerException if {@code codec} or {@code getter} is {@code null}
         */
        public Field {
            Preconditions.checkNotNull(codec, "codec must not be null");
            Preconditions.checkNotNull(getter, "getter must not be null");
        }
    }

    /**
     * The builder context object passed to the builder function.
     *
     * <p>An {@code Instance} provides methods to group fields together and
     * create a codec for the record type. It is passed to the builder function in
     * {@link RecordCodecBuilder#create(Function)} and {@link RecordCodecBuilder#mapCodec(Function)}.</p>
     *
     * <p><b>Usage</b></p>
     * <pre>{@code
     * Codec<Person> codec = RecordCodecBuilder.create(instance ->
     *     instance.group(
     *         Codecs.STRING.fieldOf("name").forGetter(Person::name),
     *         Codecs.INT.fieldOf("age").forGetter(Person::age)
     *     ).apply(instance, Person::new)
     * );
     * }</pre>
     *
     * @param <O> the record/object type being built
     */
    public static final class Instance<O> {

        /**
         * Package-private constructor.
         */
        Instance() {
        }

        /**
         * Groups a single field into a builder.
         *
         * @param f1  the first field
         * @param <A> the type of the first field
         * @return a {@link Builder1} for applying a constructor, never {@code null}
         */
        @NotNull
        public <A> Builder1<O, A> group(@NotNull final Field<O, A> f1) {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            return new Builder1<>(f1);
        }

        /**
         * Groups two fields into a builder.
         *
         * @param f1  the first field
         * @param f2  the second field
         * @param <A> the type of the first field
         * @param <B> the type of the second field
         * @return a {@link Builder2} for applying a constructor, never {@code null}
         */
        @NotNull
        public <A, B> Builder2<O, A, B> group(
                @NotNull final Field<O, A> f1,
                @NotNull final Field<O, B> f2
        ) {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
            return new Builder2<>(f1, f2);
        }

        /**
         * Groups three fields into a builder.
         *
         * @param f1  the first field
         * @param f2  the second field
         * @param f3  the third field
         * @param <A> the type of the first field
         * @param <B> the type of the second field
         * @param <C> the type of the third field
         * @return a {@link Builder3} for applying a constructor, never {@code null}
         */
        @NotNull
        public <A, B, C> Builder3<O, A, B, C> group(@NotNull final Field<O, A> f1,
                                                    @NotNull final Field<O, B> f2,
                                                    @NotNull final Field<O, C> f3
        ) {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
            Preconditions.checkNotNull(f3, "f3 must not be null");
            return new Builder3<>(f1, f2, f3);
        }

        /**
         * Groups four fields into a builder.
         *
         * @param f1  the first field
         * @param f2  the second field
         * @param f3  the third field
         * @param f4  the fourth field
         * @param <A> the type of the first field
         * @param <B> the type of the second field
         * @param <C> the type of the third field
         * @param <D> the type of the fourth field
         * @return a {@link Builder4} for applying a constructor, never {@code null}
         */
        @NotNull
        public <A, B, C, D> Builder4<O, A, B, C, D> group(@NotNull final Field<O, A> f1,
                                                          @NotNull final Field<O, B> f2,
                                                          @NotNull final Field<O, C> f3,
                                                          @NotNull final Field<O, D> f4
        ) {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
            Preconditions.checkNotNull(f3, "f3 must not be null");
            Preconditions.checkNotNull(f4, "f4 must not be null");
            return new Builder4<>(f1, f2, f3, f4);
        }

        /**
         * Groups five fields into a builder.
         *
         * @param f1  the first field
         * @param f2  the second field
         * @param f3  the third field
         * @param f4  the fourth field
         * @param f5  the fifth field
         * @param <A> the type of the first field
         * @param <B> the type of the second field
         * @param <C> the type of the third field
         * @param <D> the type of the fourth field
         * @param <E> the type of the fifth field
         * @return a {@link Builder5} for applying a constructor, never {@code null}
         */
        @NotNull
        public <A, B, C, D, E> Builder5<O, A, B, C, D, E> group(@NotNull final Field<O, A> f1,
                                                                @NotNull final Field<O, B> f2,
                                                                @NotNull final Field<O, C> f3,
                                                                @NotNull final Field<O, D> f4,
                                                                @NotNull final Field<O, E> f5
        ) {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
            Preconditions.checkNotNull(f3, "f3 must not be null");
            Preconditions.checkNotNull(f4, "f4 must not be null");
            Preconditions.checkNotNull(f5, "f5 must not be null");
            return new Builder5<>(f1, f2, f3, f4, f5);
        }

        /**
         * Groups six fields into a builder.
         *
         * @param f1  the first field
         * @param f2  the second field
         * @param f3  the third field
         * @param f4  the fourth field
         * @param f5  the fifth field
         * @param f6  the sixth field
         * @param <A> the type of the first field
         * @param <B> the type of the second field
         * @param <C> the type of the third field
         * @param <D> the type of the fourth field
         * @param <E> the type of the fifth field
         * @param <F> the type of the sixth field
         * @return a {@link Builder6} for applying a constructor, never {@code null}
         */
        @NotNull
        public <A, B, C, D, E, F> Builder6<O, A, B, C, D, E, F> group(@NotNull final Field<O, A> f1,
                                                                      @NotNull final Field<O, B> f2,
                                                                      @NotNull final Field<O, C> f3,
                                                                      @NotNull final Field<O, D> f4,
                                                                      @NotNull final Field<O, E> f5,
                                                                      @NotNull final Field<O, F> f6
        ) {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
            Preconditions.checkNotNull(f3, "f3 must not be null");
            Preconditions.checkNotNull(f4, "f4 must not be null");
            Preconditions.checkNotNull(f5, "f5 must not be null");
            Preconditions.checkNotNull(f6, "f6 must not be null");
            return new Builder6<>(f1, f2, f3, f4, f5, f6);
        }

        /**
         * Creates a constant value codec in the applicative context.
         *
         * <p>The resulting codec always decodes to the given value and encodes
         * nothing (returns the map unchanged). This is useful for providing default values or constants that don't come
         * from the input data.</p>
         *
         * <p><b>Example</b></p>
         * <pre>{@code
         * // Always decode version as 1
         * MapCodec<Integer> versionCodec = instance.point(1);
         * }</pre>
         *
         * @param value the constant value to return when decoding, must not be {@code null}
         * @param <A>   the type of the constant value
         * @return a {@link MapCodec} that always decodes to the given value, never {@code null}
         */
        @NotNull
        public <A> MapCodec<A> point(@NotNull final A value) {
            Preconditions.checkNotNull(value, "value must not be null");
            return new MapCodec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final A input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T map) {
                    Preconditions.checkNotNull(input, "input must not be null");
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(map, "map must not be null");
                    return DataResult.success(map);
                }

                @NotNull
                @Override
                public <T> DataResult<A> decode(@NotNull final DynamicOps<T> ops,
                                                @NotNull final T input) {
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(input, "input must not be null");
                    return DataResult.success(value);
                }
            };
        }
    }

    /**
     * Builder for a single-field record codec.
     *
     * @param f1  the first field
     * @param <O> the record type
     * @param <A> the type of the first field
     */
    public record Builder1<O, A>(@NotNull Field<O, A> f1) {
        /**
         * Creates a new Builder1 with validation.
         *
         * @throws NullPointerException if {@code f1} is {@code null}
         */
        public Builder1 {
            Preconditions.checkNotNull(f1, "f1 must not be null");
        }

        /**
         * Applies a constructor function to create the final codec.
         *
         * @param instance    the builder instance (for type inference)
         * @param constructor the function to construct the record from the field value
         * @return a {@link MapCodec} for the record type, never {@code null}
         */
        @NotNull
        public MapCodec<O> apply(@NotNull final Instance<O> instance,
                                 @NotNull final Function<A, O> constructor) {
            Preconditions.checkNotNull(instance, "instance must not be null");
            Preconditions.checkNotNull(constructor, "constructor must not be null");
            return new MapCodec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final O input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T map) {
                    Preconditions.checkNotNull(input, "input must not be null");
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(map, "map must not be null");
                    return f1.codec.encode(f1.getter.apply(input), ops, map);
                }

                @NotNull
                @Override
                public <T> DataResult<O> decode(@NotNull final DynamicOps<T> ops,
                                                @NotNull final T input) {
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(input, "input must not be null");
                    return f1.codec.decode(ops, input).map(constructor);
                }
            };
        }
    }

    /**
     * Builder for a two-field record codec.
     *
     * @param f1  the first field
     * @param f2  the second field
     * @param <O> the record type
     * @param <A> the type of the first field
     * @param <B> the type of the second field
     */
    public record Builder2<O, A, B>(@NotNull Field<O, A> f1,
                                    @NotNull Field<O, B> f2) {
        /**
         * Creates a new Builder2 with validation.
         *
         * @throws NullPointerException if any field is {@code null}
         */
        public Builder2 {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
        }

        /**
         * Applies a constructor function to create the final codec.
         *
         * @param instance    the builder instance (for type inference)
         * @param constructor the function to construct the record from the field values
         * @return a {@link MapCodec} for the record type, never {@code null}
         */
        @NotNull
        public MapCodec<O> apply(@NotNull final Instance<O> instance,
                                 @NotNull final BiFunction<A, B, O> constructor) {
            Preconditions.checkNotNull(instance, "instance must not be null");
            Preconditions.checkNotNull(constructor, "constructor must not be null");
            return new MapCodec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final O input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T map) {
                    Preconditions.checkNotNull(input, "input must not be null");
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(map, "map must not be null");
                    return f1.codec.encode(f1.getter.apply(input), ops, map)
                            .flatMap(m -> f2.codec.encode(f2.getter.apply(input), ops, m));
                }

                @NotNull
                @Override
                public <T> DataResult<O> decode(@NotNull final DynamicOps<T> ops,
                                                @NotNull final T input) {
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(input, "input must not be null");
                    return f1.codec.decode(ops, input)
                            .apply2(f2.codec.decode(ops, input), constructor);
                }
            };
        }
    }

    // ==================== Function Interfaces ====================

    /**
     * Builder for a three-field record codec.
     *
     * @param f1  the first field
     * @param f2  the second field
     * @param f3  the third field
     * @param <O> the record type
     * @param <A> the type of the first field
     * @param <B> the type of the second field
     * @param <C> the type of the third field
     */
    public record Builder3<O, A, B, C>(@NotNull Field<O, A> f1,
                                       @NotNull Field<O, B> f2,
                                       @NotNull Field<O, C> f3) {
        /**
         * Creates a new Builder3 with validation.
         *
         * @throws NullPointerException if any field is {@code null}
         */
        public Builder3 {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
            Preconditions.checkNotNull(f3, "f3 must not be null");
        }

        /**
         * Applies a constructor function to create the final codec.
         *
         * @param instance    the builder instance (for type inference)
         * @param constructor the function to construct the record from the field values
         * @return a {@link MapCodec} for the record type, never {@code null}
         */
        @NotNull
        public MapCodec<O> apply(@NotNull final Instance<O> instance,
                                 @NotNull final Function3<A, B, C, O> constructor) {
            Preconditions.checkNotNull(instance, "instance must not be null");
            Preconditions.checkNotNull(constructor, "constructor must not be null");
            return new MapCodec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final O input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T map) {
                    Preconditions.checkNotNull(input, "input must not be null");
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(map, "map must not be null");
                    return f1.codec.encode(f1.getter.apply(input), ops, map)
                            .flatMap(m -> f2.codec.encode(f2.getter.apply(input), ops, m))
                            .flatMap(m -> f3.codec.encode(f3.getter.apply(input), ops, m));
                }

                @NotNull
                @Override
                public <T> DataResult<O> decode(@NotNull final DynamicOps<T> ops,
                                                @NotNull final T input) {
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(input, "input must not be null");
                    final DataResult<A> a = f1.codec.decode(ops, input);
                    final DataResult<B> b = f2.codec.decode(ops, input);
                    final DataResult<C> c = f3.codec.decode(ops, input);
                    return a.apply2(b, Pair::of).apply2(c, (ab, cv) -> constructor.apply(ab.first(), ab.second(), cv));
                }
            };
        }
    }

    /**
     * Builder for a four-field record codec.
     *
     * @param f1  the first field
     * @param f2  the second field
     * @param f3  the third field
     * @param f4  the fourth field
     * @param <O> the record type
     * @param <A> the type of the first field
     * @param <B> the type of the second field
     * @param <C> the type of the third field
     * @param <D> the type of the fourth field
     */
    public record Builder4<O, A, B, C, D>(@NotNull Field<O, A> f1,
                                          @NotNull Field<O, B> f2,
                                          @NotNull Field<O, C> f3,
                                          @NotNull Field<O, D> f4) {
        /**
         * Creates a new Builder4 with validation.
         *
         * @throws NullPointerException if any field is {@code null}
         */
        public Builder4 {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
            Preconditions.checkNotNull(f3, "f3 must not be null");
            Preconditions.checkNotNull(f4, "f4 must not be null");
        }

        /**
         * Applies a constructor function to create the final codec.
         *
         * @param instance    the builder instance (for type inference)
         * @param constructor the function to construct the record from the field values
         * @return a {@link MapCodec} for the record type, never {@code null}
         */
        @NotNull
        public MapCodec<O> apply(@NotNull final Instance<O> instance,
                                 @NotNull final Function4<A, B, C, D, O> constructor) {
            Preconditions.checkNotNull(instance, "instance must not be null");
            Preconditions.checkNotNull(constructor, "constructor must not be null");
            return new MapCodec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final O input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T map) {
                    Preconditions.checkNotNull(input, "input must not be null");
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(map, "map must not be null");
                    return f1.codec.encode(f1.getter.apply(input), ops, map)
                            .flatMap(m -> f2.codec.encode(f2.getter.apply(input), ops, m))
                            .flatMap(m -> f3.codec.encode(f3.getter.apply(input), ops, m))
                            .flatMap(m -> f4.codec.encode(f4.getter.apply(input), ops, m));
                }

                @NotNull
                @Override
                public <T> DataResult<O> decode(@NotNull final DynamicOps<T> ops,
                                                @NotNull final T input) {
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(input, "input must not be null");
                    final DataResult<A> a = f1.codec.decode(ops, input);
                    final DataResult<B> b = f2.codec.decode(ops, input);
                    final DataResult<C> c = f3.codec.decode(ops, input);
                    final DataResult<D> d = f4.codec.decode(ops, input);
                    return a.apply2(b, Pair::of)
                            .apply2(c, (ab, cv) -> new Tuple3<>(ab.first(), ab.second(), cv))
                            .apply2(d, (abc, dv) -> constructor.apply(abc.a, abc.b, abc.c, dv));
                }
            };
        }
    }

    /**
     * Builder for a five-field record codec.
     *
     * @param f1  the first field
     * @param f2  the second field
     * @param f3  the third field
     * @param f4  the fourth field
     * @param f5  the fifth field
     * @param <O> the record type
     * @param <A> the type of the first field
     * @param <B> the type of the second field
     * @param <C> the type of the third field
     * @param <D> the type of the fourth field
     * @param <E> the type of the fifth field
     */
    public record Builder5<O, A, B, C, D, E>(@NotNull Field<O, A> f1,
                                             @NotNull Field<O, B> f2,
                                             @NotNull Field<O, C> f3,
                                             @NotNull Field<O, D> f4,
                                             @NotNull Field<O, E> f5) {
        /**
         * Creates a new Builder5 with validation.
         *
         * @throws NullPointerException if any field is {@code null}
         */
        public Builder5 {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
            Preconditions.checkNotNull(f3, "f3 must not be null");
            Preconditions.checkNotNull(f4, "f4 must not be null");
            Preconditions.checkNotNull(f5, "f5 must not be null");
        }

        /**
         * Applies a constructor function to create the final codec.
         *
         * @param instance    the builder instance (for type inference)
         * @param constructor the function to construct the record from the field values
         * @return a {@link MapCodec} for the record type, never {@code null}
         */
        @NotNull
        public MapCodec<O> apply(@NotNull final Instance<O> instance,
                                 @NotNull final Function5<A, B, C, D, E, O> constructor) {
            Preconditions.checkNotNull(instance, "instance must not be null");
            Preconditions.checkNotNull(constructor, "constructor must not be null");
            return new MapCodec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final O input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T map) {
                    Preconditions.checkNotNull(input, "input must not be null");
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(map, "map must not be null");
                    return f1.codec.encode(f1.getter.apply(input), ops, map)
                            .flatMap(m -> f2.codec.encode(f2.getter.apply(input), ops, m))
                            .flatMap(m -> f3.codec.encode(f3.getter.apply(input), ops, m))
                            .flatMap(m -> f4.codec.encode(f4.getter.apply(input), ops, m))
                            .flatMap(m -> f5.codec.encode(f5.getter.apply(input), ops, m));
                }

                @NotNull
                @Override
                public <T> DataResult<O> decode(@NotNull final DynamicOps<T> ops,
                                                @NotNull final T input) {
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(input, "input must not be null");
                    final DataResult<A> a = f1.codec.decode(ops, input);
                    final DataResult<B> b = f2.codec.decode(ops, input);
                    final DataResult<C> c = f3.codec.decode(ops, input);
                    final DataResult<D> d = f4.codec.decode(ops, input);
                    final DataResult<E> e = f5.codec.decode(ops, input);
                    return a.apply2(b, Pair::of)
                            .apply2(c, (ab, cv) -> new Tuple3<>(ab.first(), ab.second(), cv))
                            .apply2(d, (abc, dv) -> new Tuple4<>(abc.a, abc.b, abc.c, dv))
                            .apply2(e, (abcd, ev) -> constructor.apply(abcd.a, abcd.b, abcd.c, abcd.d, ev));
                }
            };
        }
    }

    /**
     * Builder for a six-field record codec.
     *
     * @param f1  the first field
     * @param f2  the second field
     * @param f3  the third field
     * @param f4  the fourth field
     * @param f5  the fifth field
     * @param f6  the sixth field
     * @param <O> the record type
     * @param <A> the type of the first field
     * @param <B> the type of the second field
     * @param <C> the type of the third field
     * @param <D> the type of the fourth field
     * @param <E> the type of the fifth field
     * @param <F> the type of the sixth field
     */
    public record Builder6<O, A, B, C, D, E, F>(@NotNull Field<O, A> f1,
                                                @NotNull Field<O, B> f2,
                                                @NotNull Field<O, C> f3,
                                                @NotNull Field<O, D> f4,
                                                @NotNull Field<O, E> f5,
                                                @NotNull Field<O, F> f6) {
        /**
         * Creates a new Builder6 with validation.
         *
         * @throws NullPointerException if any field is {@code null}
         */
        public Builder6 {
            Preconditions.checkNotNull(f1, "f1 must not be null");
            Preconditions.checkNotNull(f2, "f2 must not be null");
            Preconditions.checkNotNull(f3, "f3 must not be null");
            Preconditions.checkNotNull(f4, "f4 must not be null");
            Preconditions.checkNotNull(f5, "f5 must not be null");
            Preconditions.checkNotNull(f6, "f6 must not be null");
        }

        /**
         * Applies a constructor function to create the final codec.
         *
         * @param instance    the builder instance (for type inference)
         * @param constructor the function to construct the record from the field values
         * @return a {@link MapCodec} for the record type, never {@code null}
         */
        @NotNull
        public MapCodec<O> apply(@NotNull final Instance<O> instance,
                                 @NotNull final Function6<A, B, C, D, E, F, O> constructor) {
            Preconditions.checkNotNull(instance, "instance must not be null");
            Preconditions.checkNotNull(constructor, "constructor must not be null");
            return new MapCodec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final O input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T map) {
                    Preconditions.checkNotNull(input, "input must not be null");
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(map, "map must not be null");
                    return f1.codec.encode(f1.getter.apply(input), ops, map)
                            .flatMap(m -> f2.codec.encode(f2.getter.apply(input), ops, m))
                            .flatMap(m -> f3.codec.encode(f3.getter.apply(input), ops, m))
                            .flatMap(m -> f4.codec.encode(f4.getter.apply(input), ops, m))
                            .flatMap(m -> f5.codec.encode(f5.getter.apply(input), ops, m))
                            .flatMap(m -> f6.codec.encode(f6.getter.apply(input), ops, m));
                }

                @NotNull
                @Override
                public <T> DataResult<O> decode(@NotNull final DynamicOps<T> ops,
                                                @NotNull final T input) {
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(input, "input must not be null");
                    final DataResult<A> a = f1.codec.decode(ops, input);
                    final DataResult<B> b = f2.codec.decode(ops, input);
                    final DataResult<C> c = f3.codec.decode(ops, input);
                    final DataResult<D> d = f4.codec.decode(ops, input);
                    final DataResult<E> e = f5.codec.decode(ops, input);
                    final DataResult<F> f = f6.codec.decode(ops, input);
                    return a.apply2(b, Pair::of)
                            .apply2(c, (ab, cv) -> new Tuple3<>(ab.first(), ab.second(), cv))
                            .apply2(d, (abc, dv) -> new Tuple4<>(abc.a, abc.b, abc.c, dv))
                            .apply2(e, (abcd, ev) -> new Tuple5<>(abcd.a, abcd.b, abcd.c, abcd.d, ev))
                            .apply2(f, (abcde, fv) -> constructor.apply(abcde.a, abcde.b, abcde.c, abcde.d, abcde.e, fv));
                }
            };
        }
    }

    // ==================== Tuple Classes for Accumulation ====================

    /**
     * Internal tuple for accumulating 3 decoded values before applying the constructor.
     *
     * <p>This record is used internally during the decoding process to collect
     * intermediate results when decoding records with 3 or more fields. The values are accumulated using
     * {@link DataResult#apply2} before being passed to the final constructor function.</p>
     *
     * @param a   the first decoded value
     * @param b   the second decoded value
     * @param c   the third decoded value
     * @param <A> the type of the first value
     * @param <B> the type of the second value
     * @param <C> the type of the third value
     */
    private record Tuple3<A, B, C>(A a, B b, C c) {
    }

    /**
     * Internal tuple for accumulating 4 decoded values before applying the constructor.
     *
     * <p>This record is used internally during the decoding process to collect
     * intermediate results when decoding records with 4 or more fields. The values are accumulated using
     * {@link DataResult#apply2} before being passed to the final constructor function.</p>
     *
     * @param a   the first decoded value
     * @param b   the second decoded value
     * @param c   the third decoded value
     * @param d   the fourth decoded value
     * @param <A> the type of the first value
     * @param <B> the type of the second value
     * @param <C> the type of the third value
     * @param <D> the type of the fourth value
     */
    private record Tuple4<A, B, C, D>(A a, B b, C c, D d) {
    }

    /**
     * Internal tuple for accumulating 5 decoded values before applying the constructor.
     *
     * <p>This record is used internally during the decoding process to collect
     * intermediate results when decoding records with 5 or more fields. The values are accumulated using
     * {@link DataResult#apply2} before being passed to the final constructor function.</p>
     *
     * @param a   the first decoded value
     * @param b   the second decoded value
     * @param c   the third decoded value
     * @param d   the fourth decoded value
     * @param e   the fifth decoded value
     * @param <A> the type of the first value
     * @param <B> the type of the second value
     * @param <C> the type of the third value
     * @param <D> the type of the fourth value
     * @param <E> the type of the fifth value
     */
    private record Tuple5<A, B, C, D, E>(A a, B b, C c, D d, E e) {
    }
}
