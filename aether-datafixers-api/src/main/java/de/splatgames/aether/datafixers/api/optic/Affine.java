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
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An affine optic combines the capabilities of a lens and a prism, focusing on zero or one element.
 *
 * <p>An {@code Affine} is the generalization of both {@link Lens} and {@link Prism}. Like a lens,
 * it can set values within a structure. Like a prism, the focus may not exist. This makes affines ideal for optional
 * fields within product types—fields that might be present but aren't guaranteed.</p>
 *
 * <h2>When to Use an Affine</h2>
 * <p>Use an affine when you need to:</p>
 * <ul>
 *   <li>Access an optional field within a record or object</li>
 *   <li>Modify a value that may or may not be present, keeping the structure unchanged if absent</li>
 *   <li>Combine lens-like setting with prism-like optionality</li>
 *   <li>Work with nullable fields in immutable data structures</li>
 * </ul>
 *
 * <h2>Core Operations</h2>
 * <ul>
 *   <li>{@link #getOption(Object)} - Try to extract the focused value (may return empty)</li>
 *   <li>{@link #set(Object, Object)} - Set the focus if possible, or return source unchanged</li>
 *   <li>{@link #modify(Object, Function)} - Transform the focus if present</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // A record with an optional middle name
 * record Person(String firstName, String middleName, String lastName) {}
 *
 * // Create an affine for the optional middle name
 * Affine<Person, Person, String, String> middleNameAffine = Affine.of(
 *     "person.middleName",
 *     person -> Optional.ofNullable(person.middleName()),
 *     (person, newMiddle) -> new Person(person.firstName(), newMiddle, person.lastName())
 * );
 *
 * // Usage
 * Person alice = new Person("Alice", "Marie", "Smith");
 * Person bob = new Person("Bob", null, "Jones");
 *
 * middleNameAffine.getOption(alice);  // Optional.of("Marie")
 * middleNameAffine.getOption(bob);    // Optional.empty()
 *
 * middleNameAffine.set(alice, "Ann"); // Person("Alice", "Ann", "Smith")
 * middleNameAffine.set(bob, "Lee");   // Person("Bob", "Lee", "Jones") or unchanged
 *
 * middleNameAffine.modify(alice, String::toUpperCase); // Person("Alice", "MARIE", "Smith")
 * middleNameAffine.modify(bob, String::toUpperCase);   // Person("Bob", null, "Jones") unchanged
 * }</pre>
 *
 * <h2>Difference from Lens and Prism</h2>
 * <table>
 *   <caption>Comparison of optic types</caption>
 *   <tr><th>Aspect</th><th>Lens</th><th>Prism</th><th>Affine</th></tr>
 *   <tr><td>Get operation</td><td>Always succeeds</td><td>May fail</td><td>May fail</td></tr>
 *   <tr><td>Set operation</td><td>Requires source</td><td>Constructs new</td><td>Requires source</td></tr>
 *   <tr><td>Focus count</td><td>Exactly 1</td><td>0 or 1</td><td>0 or 1</td></tr>
 *   <tr><td>Use case</td><td>Required fields</td><td>Sum type variants</td><td>Optional fields</td></tr>
 * </table>
 *
 * <h2>Composition</h2>
 * <p>Affines compose with other affines. When composed, the resulting affine matches
 * only if both affines match:</p>
 * <pre>{@code
 * // addressAffine: Person -> Optional<Address>
 * // apartmentAffine: Address -> Optional<String>
 * Affine<Person, Person, String, String> composed =
 *     addressAffine.compose(apartmentAffine);
 * // Only matches if person has address AND address has apartment
 * }</pre>
 *
 * <h2>Creating from Lens</h2>
 * <p>Every {@link Lens} can be viewed as an affine using {@link #fromLens(Lens)}, since
 * a lens that always succeeds is a special case of an affine that may succeed.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Affine implementations should be stateless and thread-safe. All operations
 * return new values rather than mutating existing ones.</p>
 *
 * @param <S> the source/whole type (the structure containing the optional field)
 * @param <T> the modified source type (for polymorphic updates, often same as S)
 * @param <A> the focus/part type (the optional field's type)
 * @param <B> the modified focus type (for polymorphic updates, often same as A)
 * @author Erik Pförtner
 * @see Optic
 * @see Lens
 * @see Prism
 * @since 0.1.0
 */
public interface Affine<S, T, A, B> extends Optic<S, T, A, B> {

    /**
     * Creates an affine from a lens, treating the lens as an always-matching affine.
     *
     * <p>Since a {@link Lens} always succeeds in finding its focus, it can be viewed
     * as an affine that always matches. This method performs that conversion, wrapping the lens's get result in an
     * {@link Optional}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Lens<Person, Person, String, String> nameLens = Lens.of(
     *     "person.name", Person::name, (p, n) -> new Person(n, p.age())
     * );
     *
     * Affine<Person, Person, String, String> nameAffine = Affine.fromLens(nameLens);
     * // nameAffine.getOption() always returns Optional.of(...)
     * }</pre>
     *
     * @param lens the lens to convert, must not be {@code null}
     * @param <S>  the source type
     * @param <T>  the modified source type
     * @param <A>  the focus type
     * @param <B>  the modified focus type
     * @return an affine that always matches, never {@code null}
     * @throws NullPointerException if {@code lens} is {@code null}
     */
    @NotNull
    static <S, T, A, B> Affine<S, T, A, B> fromLens(@NotNull final Lens<S, T, A, B> lens) {
        Preconditions.checkNotNull(lens, "lens must not be null");
        return new Affine<>() {
            @NotNull
            @Override
            public String id() {
                return lens.id();
            }

            @NotNull
            @Override
            public Optional<A> getOption(@NotNull final S source) {
                Preconditions.checkNotNull(source, "source must not be null");
                return Optional.of(lens.get(source));
            }

            @NotNull
            @Override
            public T set(@NotNull final S source,
                         @NotNull final B value) {
                Preconditions.checkNotNull(source, "source must not be null");
                Preconditions.checkNotNull(value, "value must not be null");
                return lens.set(source, value);
            }
        };
    }

    /**
     * Creates a monomorphic affine from extraction and setter functions.
     *
     * <p>This factory method is the primary way to create affines. It constructs
     * an affine where the types don't change during updates (S=T and A=B).</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Affine for an optional email field
     * Affine<User, User, String, String> emailAffine = Affine.of(
     *     "user.email",
     *     user -> Optional.ofNullable(user.email()),
     *     (user, email) -> new User(user.name(), email)
     * );
     *
     * // Affine for a Map entry
     * Affine<Map<String, Integer>, Map<String, Integer>, Integer, Integer> entryAffine =
     *     Affine.of(
     *         "map.key",
     *         map -> Optional.ofNullable(map.get("key")),
     *         (map, value) -> { var m = new HashMap<>(map); m.put("key", value); return m; }
     *     );
     * }</pre>
     *
     * @param id        a unique identifier for this affine, must not be {@code null}
     * @param getOption the function to optionally extract the focus, must not be {@code null}
     * @param set       the function to set a new focus value in the source, must not be {@code null}
     * @param <S>       the source type
     * @param <A>       the focus type
     * @return a new monomorphic affine, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    static <S, A> Affine<S, S, A, A> of(
            @NotNull final String id,
            @NotNull final Function<S, Optional<A>> getOption,
            @NotNull final BiFunction<S, A, S> set
    ) {
        Preconditions.checkNotNull(id, "id must not be null");
        Preconditions.checkNotNull(getOption, "getOption must not be null");
        Preconditions.checkNotNull(set, "set must not be null");
        return new Affine<>() {
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
            public S set(@NotNull final S source,
                         @NotNull final A value) {
                Preconditions.checkNotNull(source, "source must not be null");
                Preconditions.checkNotNull(value, "value must not be null");
                return set.apply(source, value);
            }
        };
    }

    /**
     * Attempts to extract the focused value from the source.
     *
     * <p>Like {@link Prism#getOption}, this operation may fail if the focus
     * doesn't exist in the source. Unlike a lens, the result is wrapped in an {@link Optional}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Person withMiddle = new Person("Alice", "Marie", "Smith");
     * Person noMiddle = new Person("Bob", null, "Jones");
     *
     * middleNameAffine.getOption(withMiddle); // Optional.of("Marie")
     * middleNameAffine.getOption(noMiddle);   // Optional.empty()
     * }</pre>
     *
     * @param source the source structure to examine, must not be {@code null}
     * @return an {@link Optional} containing the focus if present, or empty; never {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @NotNull
    Optional<A> getOption(@NotNull final S source);

    /**
     * Sets a new value at the focus, returning a modified source.
     *
     * <p>Unlike {@link Prism#reverseGet} which constructs a new source, this operates
     * on an existing source like {@link Lens#set}. If the focus doesn't exist in the source, behavior is
     * implementation-defined (typically returns source unchanged).</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Person alice = new Person("Alice", "Marie", "Smith");
     * Person updated = middleNameAffine.set(alice, "Ann");
     * // updated is Person("Alice", "Ann", "Smith")
     * }</pre>
     *
     * @param source the source structure to modify, must not be {@code null}
     * @param value  the new value to set at the focus, must not be {@code null}
     * @return a new source with the focus updated, or the original source if the focus doesn't exist; never
     * {@code null}
     * @throws NullPointerException if {@code source} or {@code value} is {@code null}
     */
    @NotNull
    T set(@NotNull final S source, @NotNull final B value);

    /**
     * Transforms the focused value if present, otherwise returns the source unchanged.
     *
     * <p>Combines {@link #getOption} and {@link #set}: if the focus exists, applies
     * the modifier function and sets the result. If not present, returns the source unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Person withMiddle = new Person("Alice", "Marie", "Smith");
     * Person noMiddle = new Person("Bob", null, "Jones");
     *
     * middleNameAffine.modify(withMiddle, String::toUpperCase);
     * // Person("Alice", "MARIE", "Smith")
     *
     * middleNameAffine.modify(noMiddle, String::toUpperCase);
     * // Person("Bob", null, "Jones") - unchanged
     * }</pre>
     *
     * @param source   the source structure to modify, must not be {@code null}
     * @param modifier the function to apply to the focus, must not be {@code null}
     * @return a new source with the transformed focus, or the original if not present; never {@code null}
     * @throws NullPointerException if {@code source} or {@code modifier} is {@code null}
     */
    @NotNull
    @SuppressWarnings("unchecked")
    default T modify(@NotNull final S source, @NotNull final Function<A, B> modifier) {
        Preconditions.checkNotNull(source, "source must not be null");
        Preconditions.checkNotNull(modifier, "modifier must not be null");
        return getOption(source)
                .map(a -> set(source, modifier.apply(a)))
                .orElse((T) source);
    }

    /**
     * Composes this affine with another affine to focus deeper into nested structures.
     *
     * <p>The composed affine matches only if both affines match. This enables
     * reaching into deeply nested optional structures.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // personAddressAffine: Person -> Optional<Address>
     * // addressAptAffine: Address -> Optional<String>
     * Affine<Person, Person, String, String> aptAffine =
     *     personAddressAffine.compose(addressAptAffine);
     *
     * // Only matches if person has address AND address has apartment number
     * }</pre>
     *
     * @param other the affine to compose with, must not be {@code null}
     * @param <C>   the new focus type
     * @param <D>   the new modified focus type
     * @return a composed affine that matches only when both match, never {@code null}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @NotNull
    default <C, D> Affine<S, T, C, D> compose(@NotNull final Affine<A, B, C, D> other) {
        Preconditions.checkNotNull(other, "other must not be null");
        final Affine<S, T, A, B> self = this;
        return new Affine<>() {
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

            @Override
            @SuppressWarnings("unchecked")
            public @NotNull T set(@NotNull final S source, @NotNull final D value) {
                Preconditions.checkNotNull(source, "source must not be null");
                Preconditions.checkNotNull(value, "value must not be null");
                return self.getOption(source)
                        .map(a -> self.set(source, other.set(a, value)))
                        .orElse((T) source);
            }

            @NotNull
            @Override
            public <E, F> Optic<S, T, E, F> compose(@NotNull final Optic<C, D, E, F> next) {
                Preconditions.checkNotNull(next, "next must not be null");
                if (next instanceof Affine<C, D, E, F> affine) {
                    return this.compose(affine);
                }
                throw new UnsupportedOperationException("Cannot compose Affine with " + next.getClass().getSimpleName());
            }
        };
    }

    @NotNull
    @Override
    default <C, D> Optic<S, T, C, D> compose(@NotNull final Optic<A, B, C, D> other) {
        Preconditions.checkNotNull(other, "other must not be null");
        if (other instanceof Affine<A, B, C, D> affine) {
            return compose(affine);
        }
        throw new UnsupportedOperationException("Cannot compose Affine with " + other.getClass().getSimpleName());
    }
}
