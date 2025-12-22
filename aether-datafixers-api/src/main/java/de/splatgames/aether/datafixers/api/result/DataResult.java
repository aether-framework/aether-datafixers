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

package de.splatgames.aether.datafixers.api.result;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.util.Either;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A monadic result type that represents either a successful value or an error message,
 * optionally with a partial result.
 *
 * <p>This sealed interface is the primary error handling mechanism for codec operations
 * in the Aether Datafixers framework. It provides a way to handle errors without exceptions,
 * enabling composable error handling through functional operations like {@link #map(Function)}
 * and {@link #flatMap(Function)}.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Success/Error states</b>: Clearly distinguishes between successful and failed operations</li>
 *   <li><b>Partial results</b>: Error states can carry a "best effort" partial result</li>
 *   <li><b>Monadic operations</b>: Supports functional composition with map/flatMap</li>
 *   <li><b>Lazy error messages</b>: Error messages can be lazily computed via suppliers</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Creating results
 * DataResult<Integer> success = DataResult.success(42);
 * DataResult<Integer> error = DataResult.error("Value not found");
 * DataResult<Integer> errorWithPartial = DataResult.error("Incomplete data", 0);
 *
 * // Transforming values
 * DataResult<String> mapped = success.map(n -> "Value: " + n);
 * // mapped = Success["Value: 42"]
 *
 * // Chaining operations
 * DataResult<Integer> parsed = DataResult.success("123")
 *     .flatMap(s -> {
 *         try {
 *             return DataResult.success(Integer.parseInt(s));
 *         } catch (NumberFormatException e) {
 *             return DataResult.error("Invalid number: " + s);
 *         }
 *     });
 *
 * // Extracting values
 * Integer value = success.getOrThrow(IllegalStateException::new);
 * Integer withDefault = error.orElse(0);
 *
 * // Handling partial results
 * Integer result = errorWithPartial.resultOrPartial(err ->
 *     System.err.println("Warning: " + err)
 * );
 * // Logs warning, returns 0
 *
 * // Combining results
 * DataResult<String> combined = success.apply2(
 *     DataResult.success("hello"),
 *     (num, str) -> str + " " + num
 * );
 * // combined = Success["hello 42"]
 * }</pre>
 *
 * <h2>Error Propagation</h2>
 * <p>When using {@link #flatMap(Function)} on an error result with a partial value,
 * the error message is preserved and combined with any subsequent errors. This enables
 * detailed error reporting while still attempting to process as much data as possible.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Instances of {@code DataResult} are immutable and therefore thread-safe.
 * All transformation operations return new instances.</p>
 *
 * @param <A> the type of the successful result value
 * @author Erik Pförtner
 * @see Either
 * @see de.splatgames.aether.datafixers.api.codec.Codec
 * @since 0.1.0
 */
public sealed interface DataResult<A> {

    // --- Factory Methods ---

    /**
     * Creates a successful {@code DataResult} containing the given value.
     *
     * <pre>{@code
     * DataResult<String> result = DataResult.success("Hello");
     * result.isSuccess();  // true
     * result.result();     // Optional.of("Hello")
     * }</pre>
     *
     * @param value the successful value, must not be {@code null}
     * @param <A>   the type of the value
     * @return a successful {@code DataResult} containing the value, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @NotNull
    static <A> DataResult<A> success(@NotNull final A value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return new Success<>(value);
    }

    /**
     * Creates an error {@code DataResult} with the given error message and no partial result.
     *
     * <pre>{@code
     * DataResult<String> result = DataResult.error("Field 'name' is required");
     * result.isError();  // true
     * result.error();    // Optional.of("Field 'name' is required")
     * }</pre>
     *
     * @param message the error message describing what went wrong, must not be {@code null}
     * @param <A>     the type of the expected value
     * @return an error {@code DataResult} with the specified message, never {@code null}
     * @throws NullPointerException if {@code message} is {@code null}
     */
    @NotNull
    static <A> DataResult<A> error(@NotNull final String message) {
        Preconditions.checkNotNull(message, "message must not be null");
        return new Error<>(message, null);
    }

    /**
     * Creates an error {@code DataResult} with a lazily-evaluated error message.
     *
     * <p>The message supplier is immediately evaluated. This method is useful when
     * constructing the error message is expensive and you want to defer the cost.</p>
     *
     * <pre>{@code
     * DataResult<String> result = DataResult.error(() ->
     *     String.format("Expected type %s but found %s", expected, actual)
     * );
     * }</pre>
     *
     * @param messageSupplier the supplier that provides the error message, must not be {@code null}
     * @param <A>             the type of the expected value
     * @return an error {@code DataResult} with the supplied message, never {@code null}
     * @throws NullPointerException if {@code messageSupplier} is {@code null}
     */
    @NotNull
    static <A> DataResult<A> error(@NotNull final Supplier<String> messageSupplier) {
        Preconditions.checkNotNull(messageSupplier, "messageSupplier must not be null");
        return new Error<>(messageSupplier.get(), null);
    }

    /**
     * Creates an error {@code DataResult} with the given error message and a partial result.
     *
     * <p>Partial results are useful when an operation can produce a "best effort" result
     * even though it encountered errors. This allows downstream code to use the partial
     * result if appropriate, while still being aware that errors occurred.</p>
     *
     * <pre>{@code
     * // Parsing partially succeeded - got default for missing field
     * DataResult<Config> result = DataResult.error(
     *     "Missing optional field 'timeout', using default",
     *     new Config("localhost", 8080, 30)  // partial result with default timeout
     * );
     *
     * // Use partial result with warning
     * Config config = result.resultOrPartial(System.err::println);
     * }</pre>
     *
     * @param message       the error message describing what went wrong, must not be {@code null}
     * @param partialResult the partial/best-effort result, must not be {@code null}
     * @param <A>           the type of the value
     * @return an error {@code DataResult} with the message and partial result, never {@code null}
     * @throws NullPointerException if {@code message} or {@code partialResult} is {@code null}
     */
    @NotNull
    static <A> DataResult<A> error(@NotNull final String message,
                                   @NotNull final A partialResult) {
        Preconditions.checkNotNull(message, "message must not be null");
        Preconditions.checkNotNull(partialResult, "partialResult must not be null");
        return new Error<>(message, partialResult);
    }

    // --- Instance Methods ---

    /**
     * Returns {@code true} if this result represents a successful operation.
     *
     * @return {@code true} if this is a success, {@code false} if this is an error
     */
    boolean isSuccess();

    /**
     * Returns {@code true} if this result represents an error.
     *
     * @return {@code true} if this is an error, {@code false} if this is a success
     */
    boolean isError();

    /**
     * Returns the successful result wrapped in an {@link Optional}.
     *
     * @return an {@link Optional} containing the result value if successful,
     *         or an empty {@link Optional} if this is an error; never {@code null}
     */
    @NotNull
    Optional<A> result();

    /**
     * Returns the error message wrapped in an {@link Optional}.
     *
     * @return an {@link Optional} containing the error message if this is an error,
     *         or an empty {@link Optional} if this is a success; never {@code null}
     */
    @NotNull
    Optional<String> error();

    /**
     * Returns the partial result wrapped in an {@link Optional}.
     *
     * <p>Partial results are only available for error results that were created with
     * {@link #error(String, Object)}. Success results never have partial results.</p>
     *
     * @return an {@link Optional} containing the partial result if available,
     *         or an empty {@link Optional} otherwise; never {@code null}
     */
    @NotNull
    Optional<A> partialResult();

    /**
     * Transforms the successful value using the given mapping function.
     *
     * <p>If this is a success, applies the mapper and returns a new success.
     * If this is an error with a partial result, the partial result is also mapped.
     * If this is an error without a partial result, returns this unchanged.</p>
     *
     * <pre>{@code
     * DataResult<Integer> result = DataResult.success(10);
     * DataResult<String> mapped = result.map(n -> "Value: " + n);
     * // mapped = Success["Value: 10"]
     * }</pre>
     *
     * @param mapper the mapping function to apply to the value
     * @param <B>    the type of the new value
     * @return a new {@code DataResult} with the transformed value, never {@code null}
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @NotNull
    <B> DataResult<B> map(@NotNull final Function<? super A, ? extends B> mapper);

    /**
     * Transforms the successful value using a function that returns a {@code DataResult}.
     *
     * <p>This is the monadic bind operation. If this is a success, applies the mapper
     * and returns the resulting {@code DataResult}. If this is an error with a partial
     * result, attempts to apply the mapper to the partial and preserves error messages.</p>
     *
     * <pre>{@code
     * DataResult<String> result = DataResult.success("42");
     * DataResult<Integer> parsed = result.flatMap(s -> {
     *     try {
     *         return DataResult.success(Integer.parseInt(s));
     *     } catch (NumberFormatException e) {
     *         return DataResult.error("Not a number: " + s);
     *     }
     * });
     * // parsed = Success[42]
     * }</pre>
     *
     * @param mapper the mapping function returning a {@code DataResult}
     * @param <B>    the type of the new value
     * @return the result of applying the function, never {@code null}
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @NotNull
    <B> DataResult<B> flatMap(@NotNull final Function<? super A, ? extends DataResult<B>> mapper);

    /**
     * Transforms the error message using the given function.
     *
     * <p>If this is an error, applies the function to the error message and returns
     * a new error with the transformed message. If this is a success, returns this unchanged.</p>
     *
     * <pre>{@code
     * DataResult<String> error = DataResult.error("not found");
     * DataResult<String> enhanced = error.mapError(msg -> "Error: " + msg);
     * // enhanced.error() = "Error: not found"
     * }</pre>
     *
     * @param onError the function to transform the error message
     * @return a new {@code DataResult} with the transformed error message, or this if success; never {@code null}
     * @throws NullPointerException if {@code onError} is {@code null}
     */
    @NotNull
    DataResult<A> mapError(@NotNull Function<String, String> onError);

    /**
     * Returns the successful value, or throws an exception if this is an error.
     *
     * <p>The exception is created from the error message using the provided factory.</p>
     *
     * <pre>{@code
     * DataResult<String> result = DataResult.error("Missing field");
     * try {
     *     String value = result.getOrThrow(IllegalArgumentException::new);
     * } catch (IllegalArgumentException e) {
     *     // e.getMessage() == "Missing field"
     * }
     * }</pre>
     *
     * @param exceptionFactory the factory to create the exception from the error message
     * @param <X>              the type of exception to throw
     * @return the successful value if this is a success
     * @throws X                    if this is an error
     * @throws NullPointerException if {@code exceptionFactory} is {@code null}
     */
    <X extends Throwable> A getOrThrow(@NotNull final Function<String, ? extends X> exceptionFactory) throws X;

    /**
     * Returns the result value, or the partial result if available, while handling the error.
     *
     * <p>If this is a success, returns the value. If this is an error with a partial result,
     * invokes the consumer with the error message and returns the partial result.
     * If this is an error without a partial result, throws an exception.</p>
     *
     * <pre>{@code
     * DataResult<Integer> result = DataResult.error("Using default", 42);
     * Integer value = result.resultOrPartial(System.err::println);
     * // Prints: "Using default"
     * // value = 42
     * }</pre>
     *
     * @param onError consumer to handle the error message before returning the partial result
     * @return the result value or partial result
     * @throws IllegalStateException if this is an error without a partial result
     * @throws NullPointerException  if {@code onError} is {@code null}
     */
    A resultOrPartial(@NotNull final Consumer<String> onError);

    /**
     * Returns the result value if successful, otherwise returns the given default value.
     *
     * <pre>{@code
     * DataResult<Integer> success = DataResult.success(42);
     * Integer value1 = success.orElse(0);  // 42
     *
     * DataResult<Integer> error = DataResult.error("not found");
     * Integer value2 = error.orElse(0);  // 0
     * }</pre>
     *
     * @param defaultValue the default value to return if this is an error
     * @return the result value if successful, otherwise the default value
     */
    A orElse(A defaultValue);

    /**
     * Returns the result value if successful, otherwise computes a default using the supplier.
     *
     * <p>Unlike {@link #orElse(Object)}, the default value is lazily computed only if needed.</p>
     *
     * <pre>{@code
     * DataResult<Config> result = loadConfig();
     * Config config = result.orElseGet(() -> Config.defaults());
     * }</pre>
     *
     * @param supplier the supplier to compute the default value
     * @return the result value if successful, otherwise the computed default
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    A orElseGet(@NotNull final Supplier<? extends A> supplier);

    /**
     * Executes the given consumer if this is a successful result.
     *
     * <p>This method is useful for side effects such as logging or caching
     * without transforming the result. The consumer is only invoked if this is a success.</p>
     *
     * <pre>{@code
     * result.ifSuccess(value -> cache.put(key, value));
     * }</pre>
     *
     * @param consumer the consumer to execute with the successful value
     * @return this {@code DataResult} for method chaining, never {@code null}
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    @NotNull
    DataResult<A> ifSuccess(@NotNull final Consumer<? super A> consumer);

    /**
     * Executes the given consumer if this is an error result.
     *
     * <p>This method is useful for side effects such as logging errors
     * without transforming the result. The consumer is only invoked if this is an error.</p>
     *
     * <pre>{@code
     * result.ifError(msg -> logger.warn("Parsing failed: {}", msg));
     * }</pre>
     *
     * @param consumer the consumer to execute with the error message
     * @return this {@code DataResult} for method chaining, never {@code null}
     * @throws NullPointerException if {@code consumer} is {@code null}
     */
    @NotNull
    DataResult<A> ifError(@NotNull final Consumer<String> consumer);

    /**
     * Combines this result with another result using the given combining function.
     *
     * <p>If both results are successful, applies the combiner and returns a success.
     * If either is an error, returns an error with combined error messages where applicable.
     * Partial results are preserved when possible.</p>
     *
     * <pre>{@code
     * DataResult<String> name = DataResult.success("Alice");
     * DataResult<Integer> age = DataResult.success(30);
     * DataResult<Person> person = name.apply2(age, Person::new);
     * // person = Success[Person("Alice", 30)]
     * }</pre>
     *
     * @param other    the other result to combine with
     * @param combiner the function to combine both values
     * @param <B>      the type of the other result's value
     * @param <C>      the type of the combined result
     * @return a new {@code DataResult} with the combined value, never {@code null}
     * @throws NullPointerException if {@code other} or {@code combiner} is {@code null}
     */
    @NotNull
    <B, C> DataResult<C> apply2(@NotNull final DataResult<B> other,
                                 @NotNull final BiFunction<? super A, ? super B, ? extends C> combiner);

    /**
     * Converts this {@code DataResult} to an {@link Either}.
     *
     * <p>Success is converted to a right value, error is converted to a left value
     * containing the error message. Partial results are not preserved in this conversion.</p>
     *
     * <pre>{@code
     * DataResult<Integer> success = DataResult.success(42);
     * Either<String, Integer> either = success.toEither();
     * // either = Right[42]
     *
     * DataResult<Integer> error = DataResult.error("not found");
     * Either<String, Integer> eitherError = error.toEither();
     * // eitherError = Left["not found"]
     * }</pre>
     *
     * @return an {@link Either} with error message on left, or value on right; never {@code null}
     */
    @NotNull
    Either<String, A> toEither();

    /**
     * Promotes a partial result to a full success if present.
     *
     * <p>If this is an error with a partial result, logs the error via the consumer
     * and returns a success containing the partial result. If this is a success or
     * an error without a partial result, returns this unchanged.</p>
     *
     * <pre>{@code
     * DataResult<Config> result = DataResult.error("Using defaults", defaultConfig);
     * DataResult<Config> promoted = result.promotePartial(System.err::println);
     * // Prints error, promoted = Success[defaultConfig]
     * }</pre>
     *
     * @param onError consumer to handle the error message when promoting a partial result
     * @return a success if partial result is available, otherwise this; never {@code null}
     * @throws NullPointerException if {@code onError} is {@code null}
     */
    @NotNull
    DataResult<A> promotePartial(@NotNull final Consumer<String> onError);

    // --- Implementations ---

    /**
     * Implementation of {@link DataResult} representing a successful result.
     *
     * <p>A success contains a non-null value and has no error message or partial result.</p>
     *
     * @param value the successful value, must not be {@code null}
     * @param <A>   the type of the successful value
     */
    record Success<A>(@NotNull A value) implements DataResult<A> {
        public Success {
            Preconditions.checkNotNull(value, "value must not be null");
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isError() {
            return false;
        }

        @NotNull
        @Override
        public Optional<A> result() {
            return Optional.of(this.value);
        }

        @NotNull
        @Override
        public Optional<String> error() {
            return Optional.empty();
        }

        @NotNull
        @Override
        public Optional<A> partialResult() {
            return Optional.empty();
        }

        @Override
        public <B> @NotNull DataResult<B> map(@NotNull final Function<? super A, ? extends B> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            return DataResult.success(mapper.apply(this.value));
        }

        @Override
        public <B> @NotNull DataResult<B> flatMap(@NotNull final Function<? super A, ? extends DataResult<B>> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            return mapper.apply(this.value);
        }

        @NotNull
        @Override
        public DataResult<A> mapError(@NotNull final Function<String, String> onError) {
            Preconditions.checkNotNull(onError, "onError must not be null");
            return this;
        }

        @Override
        public <X extends Throwable> A getOrThrow(@NotNull final Function<String, ? extends X> exceptionFactory) throws X {
            Preconditions.checkNotNull(exceptionFactory, "exceptionFactory must not be null");
            return this.value;
        }

        @Override
        public A resultOrPartial(@NotNull final Consumer<String> onError) {
            Preconditions.checkNotNull(onError, "onError must not be null");
            return this.value;
        }

        @Override
        public A orElse(final A defaultValue) {
            return this.value;
        }

        @Override
        public A orElseGet(@NotNull final Supplier<? extends A> supplier) {
            Preconditions.checkNotNull(supplier, "supplier must not be null");
            return this.value;
        }

        @NotNull
        @Override
        public DataResult<A> ifSuccess(@NotNull final Consumer<? super A> consumer) {
            Preconditions.checkNotNull(consumer, "consumer must not be null");
            consumer.accept(this.value);
            return this;
        }

        @NotNull
        @Override
        public DataResult<A> ifError(@NotNull final Consumer<String> consumer) {
            Preconditions.checkNotNull(consumer, "consumer must not be null");
            return this;
        }

        @Override
        public <B, C> @NotNull DataResult<C> apply2(@NotNull final DataResult<B> other,
                                                     @NotNull final BiFunction<? super A, ? super B, ? extends C> combiner) {
            Preconditions.checkNotNull(other, "other must not be null");
            Preconditions.checkNotNull(combiner, "combiner must not be null");
            return other.map(b -> combiner.apply(this.value, b));
        }

        @NotNull
        @Override
        public Either<String, A> toEither() {
            return Either.right(this.value);
        }

        @NotNull
        @Override
        public DataResult<A> promotePartial(@NotNull final Consumer<String> onError) {
            Preconditions.checkNotNull(onError, "onError must not be null");
            return this;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Success<?> other)) return false;
            return Objects.equals(this.value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }

        @NotNull
        @Override
        public String toString() {
            return "DataResult.Success[" + this.value + "]";
        }
    }

    /**
     * Implementation of {@link DataResult} representing an error result.
     *
     * <p>An error contains a non-null error message and optionally a partial result
     * that represents a "best effort" value despite the error.</p>
     *
     * @param message the error message describing what went wrong, must not be {@code null}
     * @param partial the optional partial/best-effort result, may be {@code null}
     * @param <A>     the type of the expected value
     */
    record Error<A>(@NotNull String message,
                    @Nullable A partial) implements DataResult<A> {
        public Error {
            Preconditions.checkNotNull(message, "message must not be null");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isError() {
            return true;
        }

        @NotNull
        @Override
        public Optional<A> result() {
            return Optional.empty();
        }

        @NotNull
        @Override
        public Optional<String> error() {
            return Optional.of(this.message);
        }

        @NotNull
        @Override
        public Optional<A> partialResult() {
            return Optional.ofNullable(this.partial);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <B> @NotNull DataResult<B> map(@NotNull final Function<? super A, ? extends B> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            if (this.partial != null) {
                return new Error<>(this.message, mapper.apply(this.partial));
            }
            return (DataResult<B>) this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <B> @NotNull DataResult<B> flatMap(@NotNull final Function<? super A, ? extends DataResult<B>> mapper) {
            Preconditions.checkNotNull(mapper, "mapper must not be null");
            if (this.partial != null) {
                final DataResult<B> result = mapper.apply(this.partial);
                if (result.isSuccess()) {
                    return new Error<>(this.message, result.result().orElse(null));
                }
                return new Error<>(this.message + "; " + result.error().orElse(""), result.partialResult().orElse(null));
            }
            return (DataResult<B>) this;
        }

        @NotNull
        @Override
        public DataResult<A> mapError(@NotNull final Function<String, String> onError) {
            Preconditions.checkNotNull(onError, "onError must not be null");
            return new Error<>(onError.apply(this.message), this.partial);
        }

        @Override
        public <X extends Throwable> A getOrThrow(@NotNull final Function<String, ? extends X> exceptionFactory) throws X {
            Preconditions.checkNotNull(exceptionFactory, "exceptionFactory must not be null");
            throw exceptionFactory.apply(this.message);
        }

        @Override
        public A resultOrPartial(@NotNull final Consumer<String> onError) {
            Preconditions.checkNotNull(onError, "onError must not be null");
            onError.accept(this.message);
            if (this.partial != null) {
                return this.partial;
            }
            throw new IllegalStateException("No result or partial result available: " + this.message);
        }

        @Override
        public A orElse(final A defaultValue) {
            return defaultValue;
        }

        @Override
        public A orElseGet(@NotNull final Supplier<? extends A> supplier) {
            Preconditions.checkNotNull(supplier, "supplier must not be null");
            return supplier.get();
        }

        @NotNull
        @Override
        public DataResult<A> ifSuccess(@NotNull final Consumer<? super A> consumer) {
            Preconditions.checkNotNull(consumer, "consumer must not be null");
            return this;
        }

        @NotNull
        @Override
        public DataResult<A> ifError(@NotNull final Consumer<String> consumer) {
            Preconditions.checkNotNull(consumer, "consumer must not be null");
            consumer.accept(this.message);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <B, C> @NotNull DataResult<C> apply2(@NotNull final DataResult<B> other,
                                                     @NotNull final BiFunction<? super A, ? super B, ? extends C> combiner) {
            Preconditions.checkNotNull(other, "other must not be null");
            Preconditions.checkNotNull(combiner, "combiner must not be null");
            if (this.partial != null && other.result().isPresent()) {
                return new Error<>(this.message, combiner.apply(this.partial, other.result().get()));
            }
            if (this.partial != null && other.partialResult().isPresent()) {
                final String combinedError = other.error()
                        .map(e -> this.message + "; " + e)
                        .orElse(this.message);
                return new Error<>(combinedError, combiner.apply(this.partial, other.partialResult().get()));
            }
            return (DataResult<C>) this;
        }

        @NotNull
        @Override
        public Either<String, A> toEither() {
            return Either.left(this.message);
        }

        @NotNull
        @Override
        public DataResult<A> promotePartial(@NotNull final Consumer<String> onError) {
            Preconditions.checkNotNull(onError, "onError must not be null");
            if (this.partial != null) {
                onError.accept(this.message);
                return DataResult.success(this.partial);
            }
            return this;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Error<?> other)) return false;
            return Objects.equals(this.message, other.message) && Objects.equals(this.partial, other.partial);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.message, this.partial);
        }

        @NotNull
        @Override
        public String toString() {
            if (this.partial != null) {
                return "DataResult.Error[" + this.message + ", partial=" + this.partial + "]";
            }
            return "DataResult.Error[" + this.message + "]";
        }
    }
}
