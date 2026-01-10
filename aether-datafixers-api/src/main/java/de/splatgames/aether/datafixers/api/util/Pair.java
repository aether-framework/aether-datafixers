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

package de.splatgames.aether.datafixers.api.util;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An immutable pair of two values.
 *
 * <p>This class represents an ordered pair of two values, commonly used for returning
 * two related values from a method or storing key-value associations. The pair is immutable; once created, its values
 * cannot be changed.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a pair
 * Pair<String, Integer> pair = Pair.of("age", 25);
 *
 * // Access values
 * String key = pair.first();   // "age"
 * Integer value = pair.second(); // 25
 *
 * // Transform values
 * Pair<String, String> mapped = pair.mapSecond(Object::toString);
 *
 * // Apply a function
 * String result = pair.apply((k, v) -> k + "=" + v); // "age=25"
 * }</pre>
 *
 * <p>This class is thread-safe due to its immutability.</p>
 *
 * @param <F> the type of the first value
 * @param <S> the type of the second value
 * @author Erik Pförtner
 * @see Either
 * @since 0.1.0
 */
public final class Pair<F, S> {

    private final F first;
    private final S second;

    /**
     * Private constructor to enforce usage of factory method.
     *
     * @param first  the first value
     * @param second the second value
     */
    private Pair(final F first,
                 final S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Creates a new pair with the given values.
     *
     * <p>Both values may be {@code null}, as the pair does not impose any
     * non-null constraints on its elements.</p>
     *
     * @param first  the first value, may be {@code null}
     * @param second the second value, may be {@code null}
     * @param <F>    the type of the first value
     * @param <S>    the type of the second value
     * @return a new pair containing the specified values, never {@code null}
     */
    @NotNull
    public static <F, S> Pair<F, S> of(@Nullable final F first,
                                       @Nullable final S second) {
        return new Pair<>(first, second);
    }

    /**
     * Returns the first value of this pair.
     *
     * @return the first value, may be {@code null} if the pair was created with a null first value
     */
    @Nullable
    public F first() {
        return this.first;
    }

    /**
     * Returns the second value of this pair.
     *
     * @return the second value, may be {@code null} if the pair was created with a null second value
     */
    @Nullable
    public S second() {
        return this.second;
    }

    /**
     * Returns a new pair with the first value transformed by the given function.
     *
     * <p>The second value remains unchanged. The original pair is not modified.</p>
     *
     * <pre>{@code
     * Pair<String, Integer> original = Pair.of("hello", 42);
     * Pair<Integer, Integer> mapped = original.mapFirst(String::length);
     * // mapped = (5, 42)
     * }</pre>
     *
     * @param mapper the mapping function to apply to the first value
     * @param <F2>   the type of the new first value
     * @return a new pair with the mapped first value and the original second value
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @NotNull
    public <F2> Pair<F2, S> mapFirst(@NotNull final Function<? super F, ? extends F2> mapper) {
        Preconditions.checkNotNull(mapper, "mapper must not be null");
        return Pair.of(mapper.apply(this.first), this.second);
    }

    /**
     * Returns a new pair with the second value transformed by the given function.
     *
     * <p>The first value remains unchanged. The original pair is not modified.</p>
     *
     * <pre>{@code
     * Pair<String, Integer> original = Pair.of("hello", 42);
     * Pair<String, String> mapped = original.mapSecond(Object::toString);
     * // mapped = ("hello", "42")
     * }</pre>
     *
     * @param mapper the mapping function to apply to the second value
     * @param <S2>   the type of the new second value
     * @return a new pair with the original first value and the mapped second value
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @NotNull
    public <S2> Pair<F, S2> mapSecond(@NotNull final Function<? super S, ? extends S2> mapper) {
        Preconditions.checkNotNull(mapper, "mapper must not be null");
        return Pair.of(this.first, mapper.apply(this.second));
    }

    /**
     * Applies the given bi-function to both values of this pair and returns the result.
     *
     * <p>This is useful for combining the two values into a single result.</p>
     *
     * <pre>{@code
     * Pair<String, Integer> pair = Pair.of("value", 42);
     * String result = pair.apply((name, num) -> name + "=" + num);
     * // result = "value=42"
     * }</pre>
     *
     * @param function the bi-function to apply to the pair's values
     * @param <R>      the return type of the function
     * @return the result of applying the function to both values
     * @throws NullPointerException if {@code function} is {@code null}
     */
    public <R> R apply(@NotNull final BiFunction<? super F, ? super S, ? extends R> function) {
        Preconditions.checkNotNull(function, "function must not be null");
        return function.apply(this.first, this.second);
    }

    /**
     * Returns a new pair with the first and second values swapped.
     *
     * <pre>{@code
     * Pair<String, Integer> original = Pair.of("key", 123);
     * Pair<Integer, String> swapped = original.swap();
     * // swapped = (123, "key")
     * }</pre>
     *
     * @return a new pair with swapped values
     */
    @NotNull
    public Pair<S, F> swap() {
        return Pair.of(this.second, this.first);
    }

    /**
     * Compares this pair to the specified object for equality.
     *
     * <p>Two pairs are considered equal if both their first and second values are equal
     * according to {@link Objects#equals(Object, Object)}.</p>
     *
     * @param obj the object to compare with
     * @return {@code true} if the specified object is a pair with equal values
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Pair<?, ?> other)) {
            return false;
        }
        return Objects.equals(this.first, other.first) && Objects.equals(this.second, other.second);
    }

    /**
     * Returns a hash code for this pair.
     *
     * <p>The hash code is computed from both values using {@link Objects#hash(Object...)}.</p>
     *
     * @return the hash code value for this pair
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.first, this.second);
    }

    /**
     * Returns a string representation of this pair.
     *
     * <p>The format is {@code (first, second)} where {@code first} and {@code second}
     * are the string representations of the respective values.</p>
     *
     * @return a string representation of this pair
     */
    @Override
    public String toString() {
        return "(" + this.first + ", " + this.second + ")";
    }
}
