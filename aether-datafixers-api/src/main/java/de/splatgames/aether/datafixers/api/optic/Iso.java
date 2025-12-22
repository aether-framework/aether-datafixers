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

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * An isomorphism represents a lossless, reversible transformation between two types.
 *
 * <p>An {@code Iso} (isomorphism) is the most powerful optic, representing a 1-to-1
 * correspondence between two types. It can convert from S to A and back to S without
 * any loss of information. Because of this bidirectional nature, an iso is simultaneously
 * both a {@link Lens} and a {@link Prism}—it can be used anywhere either is expected.</p>
 *
 * <h2>When to Use an Iso</h2>
 * <p>Use an iso when you need to:</p>
 * <ul>
 *   <li>Convert between equivalent representations of the same data</li>
 *   <li>Transform between different type encodings (e.g., String ↔ List&lt;Character&gt;)</li>
 *   <li>Create adapters between compatible type systems</li>
 *   <li>Wrap or unwrap newtypes/value objects</li>
 *   <li>Express that two types are structurally identical</li>
 * </ul>
 *
 * <h2>Core Operations</h2>
 * <ul>
 *   <li>{@link #to(Object)} - Convert from source type S to target type A</li>
 *   <li>{@link #from(Object)} - Convert from target type A back to source type S</li>
 *   <li>{@link #reverse()} - Flip the iso to go in the opposite direction</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Iso between Celsius and Fahrenheit temperatures
 * Iso<Double, Double, Double, Double> celsiusToFahrenheit = Iso.of(
 *     "celsius.fahrenheit",
 *     celsius -> celsius * 9.0 / 5.0 + 32.0,  // to Fahrenheit
 *     fahrenheit -> (fahrenheit - 32.0) * 5.0 / 9.0  // to Celsius
 * );
 *
 * double f = celsiusToFahrenheit.to(100.0);    // 212.0
 * double c = celsiusToFahrenheit.from(32.0);   // 0.0
 *
 * // Iso between String and List<Character>
 * Iso<String, String, List<Character>, List<Character>> stringChars = Iso.of(
 *     "string.chars",
 *     str -> str.chars().mapToObj(c -> (char) c).toList(),
 *     chars -> chars.stream().map(String::valueOf).collect(Collectors.joining())
 * );
 *
 * // Use iso as a lens
 * List<Character> chars = stringChars.get("hello");  // ['h', 'e', 'l', 'l', 'o']
 * String str = stringChars.set("ignored", List.of('h', 'i'));  // "hi"
 *
 * // Reverse an iso
 * Iso<List<Character>, String, String, List<Character>> reversed =
 *     stringChars.reverse();
 * }</pre>
 *
 * <h2>Isomorphism Laws</h2>
 * <p>A well-behaved iso must satisfy two round-trip laws:</p>
 * <ul>
 *   <li><strong>RoundTrip1:</strong> {@code from(to(s)) == s}<br>
 *       Converting to the target and back yields the original source.</li>
 *   <li><strong>RoundTrip2:</strong> {@code to(from(a)) == a}<br>
 *       Converting from the target and back yields the original target.</li>
 * </ul>
 *
 * <h2>Composition</h2>
 * <p>Isos compose with other isos to create transitive conversions:</p>
 * <pre>{@code
 * // celsiusKelvin: Celsius -> Kelvin
 * // kelvinFahrenheit: Kelvin -> Fahrenheit
 * Iso<Double, Double, Double, Double> celsiusFahrenheit =
 *     celsiusKelvin.compose(kelvinFahrenheit);
 * }</pre>
 *
 * <h2>Relationship to Other Optics</h2>
 * <ul>
 *   <li>An iso IS a {@link Lens} (get = to, set ignores source and uses from)</li>
 *   <li>An iso IS a {@link Prism} (getOption = Optional.of(to), reverseGet = from)</li>
 *   <li>The {@link #identity()} iso is the identity element for composition</li>
 * </ul>
 *
 * <h2>Common Isomorphisms</h2>
 * <ul>
 *   <li>{@code String ↔ List<Character>} - Characters in a string</li>
 *   <li>{@code URI ↔ String} - URI parsing (assuming valid URIs)</li>
 *   <li>{@code Pair<A,B> ↔ Pair<B,A>} - Swap pair elements</li>
 *   <li>{@code Either<A,B> ↔ Either<B,A>} - Swap either sides</li>
 *   <li>{@code Wrapper<T> ↔ T} - Newtype unwrapping</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Iso implementations should be stateless and thread-safe.</p>
 *
 * @param <S> the source type
 * @param <T> the modified source type (for polymorphic updates, often same as S)
 * @param <A> the target type
 * @param <B> the modified target type (for polymorphic updates, often same as A)
 * @author Erik Pförtner
 * @see Optic
 * @see Lens
 * @see Prism
 * @since 0.1.0
 */
public interface Iso<S, T, A, B> extends Lens<S, T, A, B>, Prism<S, T, A, B> {

    /**
     * Converts a value from the source type to the target type.
     *
     * <p>This is the forward direction of the isomorphism. The conversion is
     * lossless and can be reversed using {@link #from(Object)}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Iso<Double, Double, Double, Double> celsiusToFahrenheit = ...;
     * double fahrenheit = celsiusToFahrenheit.to(100.0); // 212.0
     * }</pre>
     *
     * @param source the source value to convert, must not be {@code null}
     * @return the converted target value, never {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @NotNull
    A to(@NotNull final S source);

    /**
     * Converts a value from the target type back to the source type.
     *
     * <p>This is the reverse direction of the isomorphism. Combined with
     * {@link #to(Object)}, it satisfies the round-trip law:
     * {@code from(to(s)) == s}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Iso<Double, Double, Double, Double> celsiusToFahrenheit = ...;
     * double celsius = celsiusToFahrenheit.from(212.0); // 100.0
     * }</pre>
     *
     * @param target the target value to convert back, must not be {@code null}
     * @return the converted source value, never {@code null}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    @NotNull
    T from(@NotNull final B target);

    // Implement Lens interface
    @NotNull
    @Override
    default A get(@NotNull final S source) {
        return to(source);
    }

    @NotNull
    @Override
    default T set(@NotNull final S source, @NotNull final B value) {
        return from(value);
    }

    // Implement Prism interface
    @NotNull
    @Override
    default Optional<A> getOption(@NotNull final S source) {
        return Optional.of(to(source));
    }

    @NotNull
    @Override
    default T reverseGet(@NotNull final B value) {
        return from(value);
    }

    // Override modify to resolve conflict between Lens and Prism defaults
    @NotNull
    @Override
    default T modify(@NotNull final S source,
                     @NotNull final Function<A, B> modifier) {
        return from(modifier.apply(to(source)));
    }

    /**
     * Returns the reversed isomorphism, swapping source and target types.
     *
     * <p>The reversed iso converts in the opposite direction: where the original
     * converts S→A, the reversed version converts A→S. This is useful when you
     * need to apply the transformation in the reverse direction.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Iso<String, String, List<Character>, List<Character>> stringToChars = ...;
     *
     * // Original: String -> List<Character>
     * List<Character> chars = stringToChars.to("hello");
     *
     * // Reversed: List<Character> -> String
     * Iso<List<Character>, List<Character>, String, String> charsToString =
     *     stringToChars.reverse();
     * String str = charsToString.to(chars); // "hello"
     * }</pre>
     *
     * @return an iso going in the opposite direction (A↔S instead of S↔A),
     *         never {@code null}
     */
    @NotNull
    default Iso<B, A, T, S> reverse() {
        final Iso<S, T, A, B> self = this;
        return new Iso<>() {
            @NotNull
            @Override
            public String id() {
                return self.id() + ".reverse";
            }

            @NotNull
            @Override
            public T to(@NotNull final B source) {
                return self.from(source);
            }

            @NotNull
            @Override
            public A from(@NotNull final S target) {
                return self.to(target);
            }

            @NotNull
            @Override
            public A modify(@NotNull final B source,
                            @NotNull final Function<T, S> modifier) {
                return from(modifier.apply(to(source)));
            }

            @NotNull
            @Override
            public <C, D> Optic<B, A, C, D> compose(@NotNull final Optic<T, S, C, D> other) {
                throw new UnsupportedOperationException("Compose on reversed iso");
            }
        };
    }

    /**
     * Composes this iso with another iso to create a transitive conversion.
     *
     * <p>Composition chains the conversions: this iso converts S↔A, and the
     * other iso converts A↔C, resulting in an iso that converts S↔C directly.
     * Both forward and reverse directions are composed.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Iso from Celsius to Kelvin
     * Iso<Double, Double, Double, Double> celsiusToKelvin = Iso.of(
     *     "celsius.kelvin",
     *     c -> c + 273.15,
     *     k -> k - 273.15
     * );
     *
     * // Iso from Kelvin to Fahrenheit
     * Iso<Double, Double, Double, Double> kelvinToFahrenheit = Iso.of(
     *     "kelvin.fahrenheit",
     *     k -> (k - 273.15) * 9/5 + 32,
     *     f -> (f - 32) * 5/9 + 273.15
     * );
     *
     * // Composed: Celsius -> Fahrenheit
     * Iso<Double, Double, Double, Double> celsiusToFahrenheit =
     *     celsiusToKelvin.compose(kelvinToFahrenheit);
     * }</pre>
     *
     * @param other the iso to compose with (A↔C), must not be {@code null}
     * @param <C>   the new target type
     * @param <D>   the new modified target type
     * @return a composed iso converting S↔C, never {@code null}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @NotNull
    default <C, D> Iso<S, T, C, D> compose(@NotNull final Iso<A, B, C, D> other) {
        final Iso<S, T, A, B> self = this;
        return new Iso<>() {
            @NotNull
            @Override
            public String id() {
                return self.id() + "." + other.id();
            }

            @NotNull
            @Override
            public C to(@NotNull final S source) {
                return other.to(self.to(source));
            }

            @NotNull
            @Override
            public T from(@NotNull final D target) {
                return self.from(other.from(target));
            }

            @NotNull
            @Override
            public T modify(@NotNull final S source,
                            @NotNull final Function<C, D> modifier) {
                return from(modifier.apply(to(source)));
            }

            @NotNull
            @Override
            public <E, F> Optic<S, T, E, F> compose(@NotNull final Optic<C, D, E, F> next) {
                if (next instanceof Iso<C, D, E, F> iso) {
                    return this.compose((Optic<C, D, E, F>) iso);
                }
                return Iso.super.compose(next);
            }
        };
    }

    @Override
    default @NotNull <C, D> Optic<S, T, C, D> compose(@NotNull final Optic<A, B, C, D> other) {
        if (other instanceof Iso<A, B, C, D> iso) {
            return compose(iso);
        }
        if (other instanceof Lens<A, B, C, D> lens) {
            return Lens.super.compose(lens);
        }
        if (other instanceof Prism<A, B, C, D> prism) {
            return Prism.super.compose(prism);
        }
        throw new UnsupportedOperationException("Cannot compose Iso with " + other.getClass().getSimpleName());
    }

    /**
     * Creates a monomorphic iso from forward and reverse transformation functions.
     *
     * <p>This is the primary factory method for creating isomorphisms. The provided
     * functions must satisfy the round-trip laws: {@code from(to(s)) == s} and
     * {@code to(from(a)) == a}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Iso between String and its reversed form
     * Iso<String, String, String, String> reverseIso = Iso.of(
     *     "string.reverse",
     *     s -> new StringBuilder(s).reverse().toString(),
     *     s -> new StringBuilder(s).reverse().toString()
     * );
     *
     * // Iso between a wrapper and its value
     * record Wrapper<T>(T value) {}
     * Iso<Wrapper<String>, Wrapper<String>, String, String> unwrap = Iso.of(
     *     "wrapper.unwrap",
     *     Wrapper::value,
     *     Wrapper::new
     * );
     * }</pre>
     *
     * @param id   a unique identifier for this iso, must not be {@code null}
     * @param to   the forward transformation function (S→A), must not be {@code null}
     * @param from the reverse transformation function (A→S), must not be {@code null}
     * @param <S>  the source type
     * @param <A>  the target type
     * @return a new monomorphic iso, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    static <S, A> Iso<S, S, A, A> of(@NotNull final String id,
                                     @NotNull final Function<S, A> to,
                                     @NotNull final Function<A, S> from) {
        return new Iso<>() {
            @NotNull
            @Override
            public String id() {
                return id;
            }

            @NotNull
            @Override
            public A to(@NotNull final S source) {
                return to.apply(source);
            }

            @NotNull
            @Override
            public S from(@NotNull final A target) {
                return from.apply(target);
            }

            @NotNull
            @Override
            public S modify(@NotNull final S source, @NotNull final Function<A, A> modifier) {
                return from.apply(modifier.apply(to.apply(source)));
            }
        };
    }

    /**
     * Creates an identity isomorphism that returns values unchanged.
     *
     * <p>The identity iso is the neutral element for iso composition. Composing
     * any iso with the identity iso yields the original iso. Both {@link #to}
     * and {@link #from} return their input unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Iso<String, String, String, String> id = Iso.identity();
     * String s = id.to("hello");   // "hello"
     * String r = id.from("hello"); // "hello"
     *
     * // Composing with identity has no effect
     * Iso<String, String, Integer, Integer> parseIso = ...;
     * Iso<String, String, Integer, Integer> same = id.compose(parseIso);
     * // same behaves identically to parseIso
     * }</pre>
     *
     * @param <S> the type (source and target are the same)
     * @return an identity iso where to(s) == s and from(s) == s, never {@code null}
     */
    @NotNull
    static <S> Iso<S, S, S, S> identity() {
        return new Iso<>() {
            @NotNull
            @Override
            public String id() {
                return "identity";
            }

            @NotNull
            @Override
            public S to(@NotNull final S source) {
                return source;
            }

            @NotNull
            @Override
            public S from(@NotNull final S target) {
                return target;
            }

            @NotNull
            @Override
            public S modify(@NotNull final S source,
                            @NotNull final Function<S, S> modifier) {
                return modifier.apply(source);
            }
        };
    }
}
