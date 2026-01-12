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

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * A prism focuses on one case of a sum type, providing partial access that may or may not match.
 *
 * <p>A {@code Prism} is an optic that targets one variant of a sum type (also known as
 * tagged union, coproduct, or algebraic data type). Unlike a {@link Lens} which always succeeds, a prism's focus may
 * not exist in the source value. This makes prisms ideal for working with optional values, enum-like structures, or any
 * type where a value might be one of several possible forms.</p>
 *
 * <h2>When to Use a Prism</h2>
 * <p>Use a prism when you need to:</p>
 * <ul>
 *   <li>Access one variant of a sum type (e.g., {@code Left} or {@code Right} of an {@code Either})</li>
 *   <li>Work with optional fields that may be absent</li>
 *   <li>Pattern match on type hierarchies or sealed interfaces</li>
 *   <li>Parse or validate data that might not match an expected pattern</li>
 *   <li>Construct values from their components (reverse direction)</li>
 * </ul>
 *
 * <h2>Core Operations</h2>
 * <ul>
 *   <li>{@link #getOption(Object)} - Try to extract the focused value (returns {@code Optional})</li>
 *   <li>{@link #reverseGet(Object)} - Construct a source from a focus value (always succeeds)</li>
 *   <li>{@link #modify(Object, Function)} - Transform the focus if it matches</li>
 *   <li>{@link #set(Object, Object)} - Replace the focus if it matches</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Sealed interface representing a JSON value
 * sealed interface JsonValue permits JsonString, JsonNumber, JsonNull {}
 * record JsonString(String value) implements JsonValue {}
 * record JsonNumber(double value) implements JsonValue {}
 * record JsonNull() implements JsonValue {}
 *
 * // Create a prism to focus on JsonString values
 * Prism<JsonValue, JsonValue, String, String> stringPrism = Prism.of(
 *     "json.string",
 *     json -> json instanceof JsonString js ? Optional.of(js.value()) : Optional.empty(),
 *     value -> new JsonString(value)
 * );
 *
 * // Using the prism
 * JsonValue text = new JsonString("hello");
 * JsonValue number = new JsonNumber(42.0);
 *
 * stringPrism.getOption(text);    // Optional.of("hello")
 * stringPrism.getOption(number);  // Optional.empty()
 *
 * stringPrism.reverseGet("world");  // JsonString("world")
 *
 * stringPrism.modify(text, String::toUpperCase);   // JsonString("HELLO")
 * stringPrism.modify(number, String::toUpperCase); // JsonNumber(42.0) - unchanged
 * }</pre>
 *
 * <h2>Prism Laws</h2>
 * <p>A well-behaved prism must satisfy two laws that ensure predictable behavior:</p>
 * <ul>
 *   <li><strong>PartialPutGet (Round-trip from focus):</strong>
 *       {@code getOption(reverseGet(a)) == Optional.of(a)}<br>
 *       Constructing a source from a value and then extracting always succeeds
 *       and returns the original value.</li>
 *   <li><strong>GetPutPartial (Round-trip from source):</strong>
 *       if {@code getOption(s) == Optional.of(a)} then {@code reverseGet(a) == s}<br>
 *       If extraction succeeds, reconstructing from the extracted value yields
 *       an equivalent source.</li>
 * </ul>
 *
 * <h2>Composition</h2>
 * <p>Prisms compose with other prisms to focus on nested sum types. The composed
 * prism only matches if both prisms match:</p>
 * <pre>{@code
 * // response.data.error.message - only matches if all cases align
 * Prism<Response, Response, Message, Message> messagePrism =
 *     responsePrism.compose(dataPrism).compose(errorPrism);
 * }</pre>
 *
 * <h2>Difference from Lens</h2>
 * <table>
 *   <caption>Lens vs Prism comparison</caption>
 *   <tr><th>Aspect</th><th>Lens</th><th>Prism</th></tr>
 *   <tr><td>Focus target</td><td>Product type (fields)</td><td>Sum type (variants)</td></tr>
 *   <tr><td>Get operation</td><td>Always succeeds</td><td>May return empty</td></tr>
 *   <tr><td>Reverse operation</td><td>Requires existing source</td><td>Constructs new source</td></tr>
 *   <tr><td>Use case</td><td>Record/struct fields</td><td>Enum variants, optionals</td></tr>
 * </table>
 *
 * <h2>Relationship to Other Optics</h2>
 * <ul>
 *   <li>A prism extends an {@link Affine} (which combines lens and prism)</li>
 *   <li>An {@link Iso} is both a lens and a prism (1-to-1 reversible mapping)</li>
 *   <li>A prism provides more structure than a bare {@link Getter}</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Prism implementations should be stateless and thread-safe. All operations
 * return new values rather than mutating existing ones.</p>
 *
 * @param <S> the source/whole type (the sum type being examined)
 * @param <T> the modified source type (for polymorphic updates, often same as S)
 * @param <A> the focus/part type (the variant's content)
 * @param <B> the modified focus type (for polymorphic updates, often same as A)
 * @author Erik Pförtner
 * @see Optic
 * @see Lens
 * @see Affine
 * @see Iso
 * @since 0.1.0
 */
public interface Prism<S, T, A, B> extends Optic<S, T, A, B> {

    /**
     * Creates a monomorphic prism from extraction and construction functions.
     *
     * <p>This factory method is the primary way to create prisms. It constructs
     * a prism where the types don't change during updates (S=T and A=B), which covers the vast majority of use
     * cases.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Prism for extracting the 'Left' case of an Either
     * Prism<Either<String, Integer>, Either<String, Integer>, String, String> leftPrism =
     *     Prism.of(
     *         "either.left",
     *         either -> either.left(),  // Returns Optional<String>
     *         value -> Either.left(value)
     *     );
     *
     * // Prism for parsing integers from strings
     * Prism<String, String, Integer, Integer> intPrism = Prism.of(
     *     "string.int",
     *     str -> {
     *         try { return Optional.of(Integer.parseInt(str)); }
     *         catch (NumberFormatException e) { return Optional.empty(); }
     *     },
     *     num -> num.toString()
     * );
     * }</pre>
     *
     * @param id         a unique identifier for this prism (used for debugging), must not be {@code null}
     * @param getOption  the function to attempt extracting the focus from the source, must not be {@code null}
     * @param reverseGet the function to construct a source from a focus value, must not be {@code null}
     * @param <S>        the source/whole type
     * @param <A>        the focus/part type
     * @return a new monomorphic prism, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    static <S, A> Prism<S, S, A, A> of(@NotNull final String id,
                                       @NotNull final Function<S, Optional<A>> getOption,
                                       @NotNull final Function<A, S> reverseGet) {
        Preconditions.checkNotNull(id, "id must not be null");
        Preconditions.checkNotNull(getOption, "getOption must not be null");
        Preconditions.checkNotNull(reverseGet, "reverseGet must not be null");
        return new Prism<>() {
            @NotNull
            @Override
            public String id() {
                return id;
            }

            @NotNull
            @Override
            public Optional<A> getOption(@NotNull final S source) {
                Preconditions.checkNotNull(source, "source must not be null");
                return getOption.apply(source);
            }

            @NotNull
            @Override
            public S reverseGet(@NotNull final A value) {
                Preconditions.checkNotNull(value, "value must not be null");
                return reverseGet.apply(value);
            }
        };
    }

    /**
     * Attempts to extract the focused value from the source.
     *
     * <p>This is the defining operation of a prism. Unlike {@link Lens#get}, this
     * operation may fail if the source doesn't match the prism's target case. The result is wrapped in an
     * {@link Optional} to represent this possibility.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * JsonValue text = new JsonString("hello");
     * JsonValue number = new JsonNumber(42);
     *
     * stringPrism.getOption(text);   // Optional.of("hello")
     * stringPrism.getOption(number); // Optional.empty()
     * }</pre>
     *
     * @param source the source value to examine, must not be {@code null}
     * @return an {@link Optional} containing the focused value if this case matches, or {@link Optional#empty()} if it
     * doesn't match; never {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @NotNull
    Optional<A> getOption(@NotNull final S source);

    /**
     * Constructs a source value from the focused value.
     *
     * <p>This is the reverse direction of the prism. Given a value of the focus type,
     * it creates a source value representing that case. This operation always succeeds, unlike {@link #getOption} which
     * may return empty.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * JsonValue result = stringPrism.reverseGet("hello");
     * // result is JsonString("hello")
     * }</pre>
     *
     * @param value the focus value to wrap, must not be {@code null}
     * @return a new source value containing the given focus value, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    @NotNull
    T reverseGet(@NotNull final B value);

    /**
     * Transforms the focused value if this prism matches, otherwise returns the source unchanged.
     *
     * <p>If {@link #getOption} returns a value, the modifier function is applied
     * and the result is wrapped using {@link #reverseGet}. If it doesn't match, the original source is returned
     * unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * JsonValue text = new JsonString("hello");
     * JsonValue number = new JsonNumber(42);
     *
     * stringPrism.modify(text, String::toUpperCase);   // JsonString("HELLO")
     * stringPrism.modify(number, String::toUpperCase); // JsonNumber(42) unchanged
     * }</pre>
     *
     * @param source   the source value to potentially modify, must not be {@code null}
     * @param modifier the function to apply to the focused value, must not be {@code null}
     * @return a new source with the modified focus, or the original source if not matching; never {@code null}
     * @throws NullPointerException if {@code source} or {@code modifier} is {@code null}
     */
    @NotNull
    @SuppressWarnings("unchecked")
    default T modify(@NotNull final S source,
                     @NotNull final Function<A, B> modifier) {
        Preconditions.checkNotNull(source, "source must not be null");
        Preconditions.checkNotNull(modifier, "modifier must not be null");
        return getOption(source)
                .map(a -> reverseGet(modifier.apply(a)))
                .orElse((T) source);
    }

    /**
     * Replaces the focused value if this prism matches, otherwise returns the source unchanged.
     *
     * <p>Similar to {@link #modify}, but sets a fixed value instead of applying a function.
     * If the prism doesn't match, the source is returned unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * JsonValue text = new JsonString("hello");
     * JsonValue number = new JsonNumber(42);
     *
     * stringPrism.set(text, "world");   // JsonString("world")
     * stringPrism.set(number, "world"); // JsonNumber(42) unchanged
     * }</pre>
     *
     * @param source the source value to potentially modify, must not be {@code null}
     * @param value  the new value to set at the focus, must not be {@code null}
     * @return a new source with the replaced focus, or the original source if not matching; never {@code null}
     * @throws NullPointerException if {@code source} or {@code value} is {@code null}
     */
    @NotNull
    @SuppressWarnings("unchecked")
    default T set(@NotNull final S source, @NotNull final B value) {
        Preconditions.checkNotNull(source, "source must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        return getOption(source)
                .map(a -> reverseGet(value))
                .orElse((T) source);
    }

    /**
     * Composes this prism with another prism to focus deeper into nested sum types.
     *
     * <p>The composed prism only matches if both prisms match. The {@link #getOption}
     * of the composition uses {@link Optional#flatMap} to chain the extractions.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // responsePrism: Response -> ResponseData
     * // errorPrism: ResponseData -> ErrorDetails
     * Prism<Response, Response, ErrorDetails, ErrorDetails> composed =
     *     responsePrism.compose(errorPrism);
     *
     * // Only matches if response contains ResponseData AND ResponseData is an error
     * }</pre>
     *
     * @param other the prism to compose with, must not be {@code null}
     * @param <C>   the new focus type
     * @param <D>   the new modified focus type
     * @return a composed prism that matches only when both prisms match, never {@code null}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @NotNull
    default <C, D> Prism<S, T, C, D> compose(@NotNull final Prism<A, B, C, D> other) {
        Preconditions.checkNotNull(other, "other must not be null");
        final Prism<S, T, A, B> self = this;
        return new Prism<>() {
            @NotNull
            @Override
            public String id() {
                return self.id() + "." + other.id();
            }

            @NotNull
            @Override
            public Optional<C> getOption(@NotNull final S source) {
                Preconditions.checkNotNull(source, "source must not be null");
                return self.getOption(source).flatMap(other::getOption);
            }

            @NotNull
            @Override
            public T reverseGet(@NotNull final D value) {
                Preconditions.checkNotNull(value, "value must not be null");
                return self.reverseGet(other.reverseGet(value));
            }

            @Override
            public @NotNull <E, F> Optic<S, T, E, F> compose(@NotNull final Optic<C, D, E, F> next) {
                Preconditions.checkNotNull(next, "next must not be null");
                if (next instanceof Prism<C, D, E, F> prism) {
                    return this.compose(prism);
                }
                throw new UnsupportedOperationException("Cannot compose Prism with " + next.getClass().getSimpleName());
            }
        };
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation delegates to {@link #compose(Prism)} if the provided optic is a
     * {@link Prism}. For other optic types, an {@link UnsupportedOperationException} is thrown since prism composition
     * requires the other optic to also be a prism to maintain prism semantics (partial matching on sum types with
     * construction capability).</p>
     *
     * <h4>Supported Compositions</h4>
     * <ul>
     *   <li>{@link Prism} - Produces a composed {@link Prism}</li>
     * </ul>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Prism<Response, Response, ErrorData, ErrorData> errorPrism = ...;
     * Optic<ErrorData, ErrorData, String, String> messageOptic = messagePrism; // A Prism
     *
     * Optic<Response, Response, String, String> composed = errorPrism.compose(messageOptic);
     * // Returns a Prism that matches only error responses with a message
     * }</pre>
     *
     * @param other the optic to compose with, must be a {@link Prism}, must not be {@code null}
     * @param <C>   the new focus type
     * @param <D>   the new modified focus type
     * @return a composed optic (specifically a {@link Prism} if {@code other} is a Prism), never {@code null}
     * @throws NullPointerException          if {@code other} is {@code null}
     * @throws UnsupportedOperationException if {@code other} is not a {@link Prism}
     */
    @Override
    default @NotNull <C, D> Optic<S, T, C, D> compose(@NotNull final Optic<A, B, C, D> other) {
        Preconditions.checkNotNull(other, "other must not be null");
        if (other instanceof Prism<A, B, C, D> prism) {
            return compose(prism);
        }
        throw new UnsupportedOperationException("Cannot compose Prism with " + other.getClass().getSimpleName());
    }
}
