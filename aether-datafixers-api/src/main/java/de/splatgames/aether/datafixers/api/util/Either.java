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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A sum type representing either a left value of type {@code L} or a right value of type {@code R}.
 *
 * <p>This sealed interface represents a discriminated union of two types, commonly used for
 * representing computations that may produce one of two possible result types. By convention, {@code Left} represents
 * an error or alternative case, while {@code Right} represents the primary or success case. This convention follows the
 * functional programming tradition where "right" is considered the "correct" or "success" path.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create either values
 * Either<String, Integer> success = Either.right(42);
 * Either<String, Integer> failure = Either.left("Not found");
 *
 * // Check and extract values
 * if (success.isRight()) {
 *     Integer value = success.right().get();  // 42
 * }
 *
 * // Transform values using map (only affects right)
 * Either<String, String> mapped = success.map(n -> "Value: " + n);
 * // mapped = Right["Value: 42"]
 *
 * // Transform using flatMap for chained operations
 * Either<String, Integer> parsed = Either.<String, String>right("123")
 *     .flatMap(s -> {
 *         try {
 *             return Either.right(Integer.parseInt(s));
 *         } catch (NumberFormatException e) {
 *             return Either.left("Invalid number");
 *         }
 *     });
 *
 * // Pattern matching with fold
 * String result = success.fold(
 *     error -> "Error: " + error,
 *     value -> "Success: " + value
 * );
 * // result = "Success: 42"
 *
 * // Get value with default
 * Integer value = success.orElse(0);  // 42
 * Integer fallback = failure.orElse(0);  // 0
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Instances of {@code Either} are immutable and therefore thread-safe.
 * All transformation operations return new instances.</p>
 *
 * @param <L> the type of the left value (conventionally the error type)
 * @param <R> the type of the right value (conventionally the success type)
 * @author Erik Pförtner
 * @see Pair
 * @see de.splatgames.aether.datafixers.api.result.DataResult
 * @since 0.1.0
 */
public sealed interface Either<L, R> {

    /**
     * Creates a left-valued Either.
     *
     * <p>By convention, the left value represents an error or alternative case.
     * Use this factory method when you want to represent a failure or secondary result.</p>
     *
     * <pre>{@code
     * Either<String, Integer> error = Either.left("Value not found");
     * }</pre>
     *
     * @param value the left value, must not be {@code null}
     * @param <L>   the type of the left value
     * @param <R>   the type of the right value
     * @return a left-valued Either containing the specified value, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @NotNull
    static <L, R> Either<L, R> left(@NotNull final L value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return new Left<>(value);
    }

    /**
     * Creates a right-valued Either.
     *
     * <p>By convention, the right value represents a success or primary case.
     * Use this factory method when you want to represent a successful result.</p>
     *
     * <pre>{@code
     * Either<String, Integer> success = Either.right(42);
     * }</pre>
     *
     * @param value the right value, must not be {@code null}
     * @param <L>   the type of the left value
     * @param <R>   the type of the right value
     * @return a right-valued Either containing the specified value, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @NotNull
    static <L, R> Either<L, R> right(@NotNull final R value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return new Right<>(value);
    }

    /**
     * Returns {@code true} if this is a left value.
     *
     * @return {@code true} if this Either contains a left value, {@code false} otherwise
     */
    boolean isLeft();

    /**
     * Returns {@code true} if this is a right value.
     *
     * @return {@code true} if this Either contains a right value, {@code false} otherwise
     */
    boolean isRight();

    /**
     * Returns the left value wrapped in an {@link Optional}.
     *
     * @return an {@link Optional} containing the left value if this is a left, or an empty {@link Optional} if this is
     * a right; never {@code null}
     */
    @NotNull
    Optional<L> left();

    /**
     * Returns the right value wrapped in an {@link Optional}.
     *
     * @return an {@link Optional} containing the right value if this is a right, or an empty {@link Optional} if this
     * is a left; never {@code null}
     */
    @NotNull
    Optional<R> right();

    /**
     * Transforms the right value using the given mapping function.
     *
     * <p>If this is a right value, applies the mapper and returns a new right-valued Either
     * containing the result. If this is a left value, returns this Either unchanged (with an adjusted type
     * parameter).</p>
     *
     * <pre>{@code
     * Either<String, Integer> right = Either.right(42);
     * Either<String, String> mapped = right.map(n -> "Number: " + n);
     * // mapped = Right["Number: 42"]
     *
     * Either<String, Integer> left = Either.left("error");
     * Either<String, String> stillLeft = left.map(n -> "Number: " + n);
     * // stillLeft = Left["error"]
     * }</pre>
     *
     * @param mapper the mapping function to apply to the right value
     * @param <R2>   the type of the new right value
     * @return a new Either with the transformed right value, or this if left; never {@code null}
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @NotNull
    <R2> Either<L, R2> map(@NotNull final Function<? super R, ? extends R2> mapper);

    /**
     * Transforms the left value using the given mapping function.
     *
     * <p>If this is a left value, applies the mapper and returns a new left-valued Either
     * containing the result. If this is a right value, returns this Either unchanged (with an adjusted type
     * parameter).</p>
     *
     * <pre>{@code
     * Either<String, Integer> left = Either.left("error");
     * Either<String, Integer> mappedLeft = left.mapLeft(String::toUpperCase);
     * // mappedLeft = Left["ERROR"]
     * }</pre>
     *
     * @param mapper the mapping function to apply to the left value
     * @param <L2>   the type of the new left value
     * @return a new Either with the transformed left value, or this if right; never {@code null}
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @NotNull
    <L2> Either<L2, R> mapLeft(@NotNull final Function<? super L, ? extends L2> mapper);

    /**
     * Transforms the right value using a function that returns an Either.
     *
     * <p>If this is a right value, applies the mapper and returns the resulting Either.
     * If this is a left value, returns this Either unchanged. This is useful for chaining operations that may
     * fail.</p>
     *
     * <pre>{@code
     * Either<String, String> input = Either.right("123");
     * Either<String, Integer> parsed = input.flatMap(s -> {
     *     try {
     *         return Either.right(Integer.parseInt(s));
     *     } catch (NumberFormatException e) {
     *         return Either.left("Invalid number: " + s);
     *     }
     * });
     * // parsed = Right[123]
     * }</pre>
     *
     * @param mapper the mapping function returning an Either
     * @param <R2>   the type of the new right value
     * @return the result of applying the function if right, or this if left; never {@code null}
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @NotNull
    <R2> Either<L, R2> flatMap(@NotNull final Function<? super R, ? extends Either<L, R2>> mapper);

    /**
     * Applies one of two functions depending on whether this is a left or right value.
     *
     * <p>This method provides exhaustive pattern matching over the Either type.
     * Exactly one of the two functions will be applied.</p>
     *
     * <pre>{@code
     * Either<String, Integer> either = Either.right(42);
     * String result = either.fold(
     *     error -> "Failed: " + error,
     *     value -> "Success: " + value
     * );
     * // result = "Success: 42"
     * }</pre>
     *
     * @param leftMapper  the function to apply if this is a left value
     * @param rightMapper the function to apply if this is a right value
     * @param <T>         the result type of both functions
     * @return the result of applying the appropriate function
     * @throws NullPointerException if {@code leftMapper} or {@code rightMapper} is {@code null}
     */
    <T> T fold(@NotNull final Function<? super L, ? extends T> leftMapper,
               @NotNull final Function<? super R, ? extends T> rightMapper);

    /**
     * Executes the given consumer if this is a left value.
     *
     * <p>This method is useful for side effects such as logging or error handling
     * without transforming the value. The consumer is only invoked if this is a left.</p>
     *
     * <pre>{@code
     * Either<String, Integer> result = Either.left("error");
     * result.ifLeft(error -> System.err.println("Error: " + error));
     * }</pre>
     *
     * @param consumer the consumer to execute with the left value
     * @return this Either for method chaining, never {@code null}
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    @NotNull
    Either<L, R> ifLeft(@NotNull final Consumer<? super L> consumer);

    /**
     * Executes the given consumer if this is a right value.
     *
     * <p>This method is useful for side effects such as logging or processing
     * without transforming the value. The consumer is only invoked if this is a right.</p>
     *
     * <pre>{@code
     * Either<String, Integer> result = Either.right(42);
     * result.ifRight(value -> System.out.println("Value: " + value));
     * }</pre>
     *
     * @param consumer the consumer to execute with the right value
     * @return this Either for method chaining, never {@code null}
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    @NotNull
    Either<L, R> ifRight(@NotNull final Consumer<? super R> consumer);

    /**
     * Returns a new Either with the left and right values swapped.
     *
     * <p>A left becomes a right and a right becomes a left. This is useful
     * when you need to change the semantics or work with a function that expects the types in a different order.</p>
     *
     * <pre>{@code
     * Either<String, Integer> original = Either.right(42);
     * Either<Integer, String> swapped = original.swap();
     * // swapped = Left[42]
     * }</pre>
     *
     * @return a new Either with swapped left and right values, never {@code null}
     */
    @NotNull
    Either<R, L> swap();

    /**
     * Returns the right value, or throws an exception if this is a left.
     *
     * <p>The exception is created from the left value using the provided mapper function.
     * This is useful for converting an Either to a throwing operation.</p>
     *
     * <pre>{@code
     * Either<String, Integer> result = Either.left("Not found");
     * try {
     *     Integer value = result.orElseThrow(IllegalStateException::new);
     * } catch (IllegalStateException e) {
     *     // e.getMessage() == "Not found"
     * }
     * }</pre>
     *
     * @param exceptionMapper function to create the exception from the left value
     * @param <X>             the type of exception to throw
     * @return the right value if this is a right
     * @throws X                    if this is a left value
     * @throws NullPointerException if {@code exceptionMapper} is {@code null}
     */
    <X extends Throwable> R orElseThrow(@NotNull final Function<? super L, ? extends X> exceptionMapper) throws X;

    /**
     * Returns the right value if present, otherwise returns the given default value.
     *
     * <pre>{@code
     * Either<String, Integer> right = Either.right(42);
     * Integer value1 = right.orElse(0);  // 42
     *
     * Either<String, Integer> left = Either.left("error");
     * Integer value2 = left.orElse(0);  // 0
     * }</pre>
     *
     * @param defaultValue the default value to return if this is a left
     * @return the right value if present, otherwise the default value
     */
    R orElse(R defaultValue);

    /**
     * Returns the right value if present, otherwise computes a default from the left value.
     *
     * <p>Unlike {@link #orElse(Object)}, this method allows the default value to be
     * computed based on the left value, enabling error recovery strategies.</p>
     *
     * <pre>{@code
     * Either<Integer, String> left = Either.left(404);
     * String result = left.orElseGet(code -> "Error code: " + code);
     * // result = "Error code: 404"
     * }</pre>
     *
     * @param other function to compute the default value from the left value
     * @return the right value if present, otherwise the computed default
     * @throws NullPointerException if {@code other} is {@code null}
     */
    R orElseGet(@NotNull final Function<? super L, ? extends R> other);

    // --- Implementations ---

    /**
     * Implementation of {@link Either} representing a left value.
     *
     * <p>By convention, the left case represents an error, failure, or alternative result.
     * This record implementation is immutable and thread-safe.</p>
     *
     * <h2>Usage</h2>
     * <p>Instances should be created via the factory method {@link Either#left(Object)}
     * rather than directly constructing this record.</p>
     *
     * <pre>{@code
     * Either<String, Integer> error = Either.left("Not found");
     * // Equivalent to: new Either.Left<>("Not found")
     * }</pre>
     *
     * <h2>Behavior</h2>
     * <ul>
     *   <li>{@link #isLeft()} always returns {@code true}</li>
     *   <li>{@link #isRight()} always returns {@code false}</li>
     *   <li>{@link #left()} returns {@code Optional.of(value)}</li>
     *   <li>{@link #right()} returns {@code Optional.empty()}</li>
     *   <li>{@link #map(Function)} returns this unchanged (type-cast)</li>
     *   <li>{@link #flatMap(Function)} returns this unchanged (type-cast)</li>
     *   <li>{@link #fold(Function, Function)} applies the left mapper</li>
     * </ul>
     *
     * @param value the left value, must not be {@code null}
     * @param <L>   the type of the left value
     * @param <R>   the type of the right value (phantom type parameter, not used in this case)
     */
    record Left<L, R>(@NotNull L value) implements Either<L, R> {

        /**
         * Canonical constructor for the Left record.
         *
         * <p>Validates that the provided value is not {@code null}.</p>
         *
         * @throws NullPointerException if {@code value} is {@code null}
         */
        public Left {
            Preconditions.checkNotNull(value, "value must not be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@code true} for Left
         */
        @Override
        public boolean isLeft() {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@code false} for Left
         */
        @Override
        public boolean isRight() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return an {@link Optional} containing the left value
         */
        @NotNull
        @Override
        public Optional<L> left() {
            return Optional.of(this.value);
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link Optional#empty()} for Left
         */
        @NotNull
        @Override
        public Optional<R> right() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, the mapper is not applied and this instance is returned unchanged
         * (with adjusted type parameters).</p>
         *
         * @param mapper the mapping function (not applied for Left)
         * @param <R2>   the new right type
         * @return this Left instance cast to the new type
         */
        @Override
        @SuppressWarnings("unchecked")
        public <R2> @NotNull Either<L, R2> map(@NotNull final Function<? super R, ? extends R2> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            return (Either<L, R2>) this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, the mapper is applied to transform the left value.</p>
         *
         * @param mapper the mapping function to apply to the left value
         * @param <L2>   the new left type
         * @return a new Left containing the mapped value
         */
        @Override
        public <L2> @NotNull Either<L2, R> mapLeft(@NotNull final Function<? super L, ? extends L2> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            return Either.left(mapper.apply(this.value));
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, the mapper is not applied and this instance is returned unchanged
         * (with adjusted type parameters).</p>
         *
         * @param mapper the mapping function (not applied for Left)
         * @param <R2>   the new right type
         * @return this Left instance cast to the new type
         */
        @Override
        @SuppressWarnings("unchecked")
        public <R2> @NotNull Either<L, R2> flatMap(@NotNull final Function<? super R, ? extends Either<L, R2>> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            return (Either<L, R2>) this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, applies the {@code leftMapper} to the left value.</p>
         *
         * @param leftMapper  the function to apply to the left value
         * @param rightMapper the function for right values (not used for Left)
         * @param <T>         the result type
         * @return the result of applying {@code leftMapper} to the left value
         */
        @Override
        public <T> T fold(@NotNull final Function<? super L, ? extends T> leftMapper,
                          @NotNull final Function<? super R, ? extends T> rightMapper) {
            Preconditions.checkNotNull(leftMapper, "leftMapper must not be null");
            Preconditions.checkNotNull(rightMapper, "rightMapper must not be null");
            return leftMapper.apply(this.value);
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, the consumer is invoked with the left value.</p>
         *
         * @param consumer the consumer to invoke with the left value
         * @return this instance for method chaining
         */
        @NotNull
        @Override
        public Either<L, R> ifLeft(@NotNull final Consumer<? super L> consumer) {
            Preconditions.checkNotNull(consumer, "consumer must not be null");
            consumer.accept(this.value);
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, the consumer is not invoked.</p>
         *
         * @param consumer the consumer (not invoked for Left)
         * @return this instance for method chaining
         */
        @NotNull
        @Override
        public Either<L, R> ifRight(@NotNull final Consumer<? super R> consumer) {
            Preconditions.checkNotNull(consumer, "consumer must not be null");
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, returns a Right containing the left value.</p>
         *
         * @return a Right containing this Left's value
         */
        @NotNull
        @Override
        public Either<R, L> swap() {
            return Either.right(this.value);
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, always throws the exception created from the left value.</p>
         *
         * @param exceptionMapper the function to create an exception from the left value
         * @param <X>             the exception type
         * @return never returns normally
         * @throws X always thrown, created from the left value
         */
        @Override
        public <X extends Throwable> R orElseThrow(@NotNull final Function<? super L, ? extends X> exceptionMapper) throws X {
            Preconditions.checkNotNull(exceptionMapper, "exceptionMapper must not be null");
            throw exceptionMapper.apply(this.value);
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, always returns the default value.</p>
         *
         * @param defaultValue the value to return
         * @return the provided default value
         */
        @Override
        public R orElse(final R defaultValue) {
            return defaultValue;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Left, applies the function to the left value to compute the result.</p>
         *
         * @param other the function to compute the result from the left value
         * @return the result of applying {@code other} to the left value
         */
        @Override
        public R orElseGet(@NotNull final Function<? super L, ? extends R> other) {
            Preconditions.checkNotNull(other, "other must not be null");
            return other.apply(this.value);
        }

        /**
         * Compares this Left to another object for equality.
         *
         * <p>Two Left instances are equal if they contain equal values.</p>
         *
         * @param obj the object to compare with
         * @return {@code true} if the other object is a Left with an equal value
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Left<?, ?> other)) {
                return false;
            }
            return Objects.equals(this.value, other.value);
        }

        /**
         * Returns the hash code for this Left.
         *
         * @return the hash code based on the left value
         */
        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }

        /**
         * Returns a string representation of this Left.
         *
         * @return a string in the format "Left[value]"
         */
        @NotNull
        @Override
        public String toString() {
            return "Left[" + this.value + "]";
        }
    }

    /**
     * Implementation of {@link Either} representing a right value.
     *
     * <p>By convention, the right case represents a success or primary result.
     * This record implementation is immutable and thread-safe.</p>
     *
     * <h2>Usage</h2>
     * <p>Instances should be created via the factory method {@link Either#right(Object)}
     * rather than directly constructing this record.</p>
     *
     * <pre>{@code
     * Either<String, Integer> success = Either.right(42);
     * // Equivalent to: new Either.Right<>(42)
     * }</pre>
     *
     * <h2>Behavior</h2>
     * <ul>
     *   <li>{@link #isLeft()} always returns {@code false}</li>
     *   <li>{@link #isRight()} always returns {@code true}</li>
     *   <li>{@link #left()} returns {@code Optional.empty()}</li>
     *   <li>{@link #right()} returns {@code Optional.of(value)}</li>
     *   <li>{@link #map(Function)} applies the mapper to the value</li>
     *   <li>{@link #flatMap(Function)} applies the mapper and returns its result</li>
     *   <li>{@link #fold(Function, Function)} applies the right mapper</li>
     * </ul>
     *
     * @param value the right value, must not be {@code null}
     * @param <L>   the type of the left value (phantom type parameter, not used in this case)
     * @param <R>   the type of the right value
     */
    record Right<L, R>(@NotNull R value) implements Either<L, R> {

        /**
         * Canonical constructor for the Right record.
         *
         * <p>Validates that the provided value is not {@code null}.</p>
         *
         * @throws NullPointerException if {@code value} is {@code null}
         */
        public Right {
            Preconditions.checkNotNull(value, "value must not be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@code false} for Right
         */
        @Override
        public boolean isLeft() {
            return false;
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@code true} for Right
         */
        @Override
        public boolean isRight() {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @return always {@link Optional#empty()} for Right
         */
        @NotNull
        @Override
        public Optional<L> left() {
            return Optional.empty();
        }

        /**
         * {@inheritDoc}
         *
         * @return an {@link Optional} containing the right value
         */
        @NotNull
        @Override
        public Optional<R> right() {
            return Optional.of(this.value);
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, the mapper is applied to transform the right value.</p>
         *
         * @param mapper the mapping function to apply to the right value
         * @param <R2>   the new right type
         * @return a new Right containing the mapped value
         */
        @Override
        public <R2> @NotNull Either<L, R2> map(@NotNull final Function<? super R, ? extends R2> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            return Either.right(mapper.apply(this.value));
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, the mapper is not applied and this instance is returned unchanged
         * (with adjusted type parameters).</p>
         *
         * @param mapper the mapping function (not applied for Right)
         * @param <L2>   the new left type
         * @return this Right instance cast to the new type
         */
        @Override
        @SuppressWarnings("unchecked")
        public <L2> @NotNull Either<L2, R> mapLeft(@NotNull final Function<? super L, ? extends L2> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            return (Either<L2, R>) this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, the mapper is applied to the right value and its result is returned.</p>
         *
         * @param mapper the mapping function to apply to the right value
         * @param <R2>   the new right type
         * @return the result of applying the mapper to the right value
         */
        @Override
        public <R2> @NotNull Either<L, R2> flatMap(@NotNull final Function<? super R, ? extends Either<L, R2>> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            return mapper.apply(this.value);
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, applies the {@code rightMapper} to the right value.</p>
         *
         * @param leftMapper  the function for left values (not used for Right)
         * @param rightMapper the function to apply to the right value
         * @param <T>         the result type
         * @return the result of applying {@code rightMapper} to the right value
         */
        @Override
        public <T> T fold(@NotNull final Function<? super L, ? extends T> leftMapper,
                          @NotNull final Function<? super R, ? extends T> rightMapper) {
            Preconditions.checkNotNull(leftMapper, "leftMapper must not be null");
            Preconditions.checkNotNull(rightMapper, "rightMapper must not be null");
            return rightMapper.apply(this.value);
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, the consumer is not invoked.</p>
         *
         * @param consumer the consumer (not invoked for Right)
         * @return this instance for method chaining
         */
        @NotNull
        @Override
        public Either<L, R> ifLeft(@NotNull final Consumer<? super L> consumer) {
            Preconditions.checkNotNull(consumer, "consumer must not be null");
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, the consumer is invoked with the right value.</p>
         *
         * @param consumer the consumer to invoke with the right value
         * @return this instance for method chaining
         */
        @NotNull
        @Override
        public Either<L, R> ifRight(@NotNull final Consumer<? super R> consumer) {
            Preconditions.checkNotNull(consumer, "consumer must not be null");
            consumer.accept(this.value);
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, returns a Left containing the right value.</p>
         *
         * @return a Left containing this Right's value
         */
        @NotNull
        @Override
        public Either<R, L> swap() {
            return Either.left(this.value);
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, returns the right value without throwing.</p>
         *
         * @param exceptionMapper the function to create an exception (not used for Right)
         * @param <X>             the exception type
         * @return the right value
         */
        @Override
        public <X extends Throwable> R orElseThrow(@NotNull final Function<? super L, ? extends X> exceptionMapper) throws X {
            Preconditions.checkNotNull(exceptionMapper, "exceptionMapper must not be null");
            return this.value;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, always returns the right value, ignoring the default.</p>
         *
         * @param defaultValue the default value (ignored for Right)
         * @return the right value
         */
        @Override
        public R orElse(final R defaultValue) {
            return this.value;
        }

        /**
         * {@inheritDoc}
         *
         * <p>For Right, always returns the right value without invoking the function.</p>
         *
         * @param other the function to compute a result (not invoked for Right)
         * @return the right value
         */
        @Override
        public R orElseGet(@NotNull final Function<? super L, ? extends R> other) {
            Preconditions.checkNotNull(other, "other must not be null");
            return this.value;
        }

        /**
         * Compares this Right to another object for equality.
         *
         * <p>Two Right instances are equal if they contain equal values.</p>
         *
         * @param obj the object to compare with
         * @return {@code true} if the other object is a Right with an equal value
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Right<?, ?> other)) {
                return false;
            }
            return Objects.equals(this.value, other.value);
        }

        /**
         * Returns the hash code for this Right.
         *
         * @return the hash code based on the right value
         */
        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }

        /**
         * Returns a string representation of this Right.
         *
         * @return a string in the format "Right[value]"
         */
        @NotNull
        @Override
        public String toString() {
            return "Right[" + this.value + "]";
        }
    }
}
