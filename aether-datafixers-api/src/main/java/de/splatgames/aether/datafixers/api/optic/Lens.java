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

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A lens focuses on exactly one part of a product type, providing both read and write access.
 *
 * <p>A {@code Lens} is one of the most commonly used optics. It represents a functional
 * reference to a field within a data structure, allowing you to get the field's value
 * and set a new value while preserving the rest of the structure. Unlike direct field
 * access, lenses are composable and work immutably.</p>
 *
 * <h2>When to Use a Lens</h2>
 * <p>Use a lens when you need to:</p>
 * <ul>
 *   <li>Access a field that is <strong>always present</strong> (not optional)</li>
 *   <li>Modify nested data structures immutably</li>
 *   <li>Compose field accessors to reach deeply nested values</li>
 *   <li>Create reusable, type-safe accessors for data transformations</li>
 * </ul>
 *
 * <h2>Core Operations</h2>
 * <ul>
 *   <li>{@link #get(Object)} - Extract the focused value from the source</li>
 *   <li>{@link #set(Object, Object)} - Replace the focused value, returning a new source</li>
 *   <li>{@link #modify(Object, Function)} - Transform the focused value using a function</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define a record for a Person
 * record Address(String street, String city) {}
 * record Person(String name, Address address) {}
 *
 * // Create a lens to focus on the 'address' field of Person
 * Lens<Person, Person, Address, Address> addressLens = Lens.of(
 *     "person.address",
 *     Person::address,
 *     (person, newAddress) -> new Person(person.name(), newAddress)
 * );
 *
 * // Create a lens to focus on the 'city' field of Address
 * Lens<Address, Address, String, String> cityLens = Lens.of(
 *     "address.city",
 *     Address::city,
 *     (address, newCity) -> new Address(address.street(), newCity)
 * );
 *
 * // Compose lenses to focus on Person -> Address -> City
 * Lens<Person, Person, String, String> personCityLens = addressLens.compose(cityLens);
 *
 * // Use the composed lens
 * Person alice = new Person("Alice", new Address("Main St", "Boston"));
 * String city = personCityLens.get(alice);           // "Boston"
 * Person moved = personCityLens.set(alice, "Seattle"); // Alice now lives in Seattle
 * Person upper = personCityLens.modify(alice, String::toUpperCase); // City is "BOSTON"
 * }</pre>
 *
 * <h2>Lens Laws</h2>
 * <p>A well-behaved lens must satisfy three laws that ensure predictable behavior:</p>
 * <ul>
 *   <li><strong>GetPut (Identity):</strong> {@code set(s, get(s)) == s}<br>
 *       Setting a value to what it already is returns the original unchanged.</li>
 *   <li><strong>PutGet (Retention):</strong> {@code get(set(s, a)) == a}<br>
 *       Getting after setting returns exactly what was set.</li>
 *   <li><strong>PutPut (Idempotence):</strong> {@code set(set(s, a1), a2) == set(s, a2)}<br>
 *       Setting twice is equivalent to setting once with the final value.</li>
 * </ul>
 *
 * <h2>Composition</h2>
 * <p>Lenses can be composed with other lenses using {@link #compose(Lens)} to create
 * a lens that focuses deeper into nested structures. Composition is associative,
 * meaning {@code (a.compose(b)).compose(c) == a.compose(b.compose(c))}.</p>
 *
 * <h2>Polymorphic Updates</h2>
 * <p>The four type parameters support polymorphic updates where changing the focus
 * type can change the source type. For most use cases (monomorphic lenses), use
 * {@code Lens<S, S, A, A>} where the source and focus types don't change.</p>
 *
 * <h2>Relationship to Other Optics</h2>
 * <ul>
 *   <li>A lens is more powerful than a {@link Getter} (which is read-only)</li>
 *   <li>A lens always succeeds, unlike a {@link Prism} (which may not match)</li>
 *   <li>An {@link Affine} combines lens and prism capabilities</li>
 *   <li>An {@link Iso} is a reversible lens between two equivalent types</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Lens implementations should be stateless and thread-safe. The lens itself
 * performs no mutation; instead, {@link #set} and {@link #modify} return new
 * instances of the source type.</p>
 *
 * @param <S> the source/whole type (the structure containing the field)
 * @param <T> the modified source type (for polymorphic updates, often same as S)
 * @param <A> the focus/part type (the field's type)
 * @param <B> the modified focus type (for polymorphic updates, often same as A)
 * @author Erik Pförtner
 * @see Optic
 * @see Getter
 * @see Prism
 * @see Affine
 * @see Iso
 * @since 0.1.0
 */
public interface Lens<S, T, A, B> extends Optic<S, T, A, B> {

    /**
     * Extracts the focused value from the source structure.
     *
     * <p>This operation always succeeds for a lens, as the focused field
     * is guaranteed to exist within the source structure.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Person person = new Person("Alice", 30);
     * String name = nameLens.get(person); // Returns "Alice"
     * }</pre>
     *
     * @param source the source structure to extract from, must not be {@code null}
     * @return the focused value, never {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @NotNull
    A get(@NotNull final S source);

    /**
     * Returns a new source structure with the focused value replaced.
     *
     * <p>This operation creates a new instance of the source with the specified
     * value at the focus position. The original source is not modified.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Person alice = new Person("Alice", 30);
     * Person bob = nameLens.set(alice, "Bob"); // New Person with name "Bob"
     * // alice is unchanged, bob is a new instance
     * }</pre>
     *
     * @param source the source structure to modify, must not be {@code null}
     * @param value  the new value to place at the focus, must not be {@code null}
     * @return a new source structure with the updated value, never {@code null}
     * @throws NullPointerException if {@code source} or {@code value} is {@code null}
     */
    @NotNull
    T set(@NotNull final S source,
          @NotNull final B value);

    /**
     * Transforms the focused value using the given function.
     *
     * <p>This is a convenience method combining {@link #get} and {@link #set}.
     * It extracts the current value, applies the modifier function, and sets
     * the result back into a new source structure.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Person person = new Person("alice", 30);
     * Person updated = nameLens.modify(person, String::toUpperCase);
     * // updated.name() returns "ALICE"
     * }</pre>
     *
     * @param source   the source structure to modify, must not be {@code null}
     * @param modifier the function to apply to the focused value, must not be {@code null}
     * @return a new source structure with the transformed value, never {@code null}
     * @throws NullPointerException if {@code source} or {@code modifier} is {@code null}
     */
    @NotNull
    default T modify(@NotNull final S source,
                     @NotNull final Function<A, B> modifier) {
        return set(source, modifier.apply(get(source)));
    }

    /**
     * Composes this lens with another lens to focus deeper into the structure.
     *
     * <p>Lens composition chains the focus: this lens focuses from S to A,
     * and the other lens focuses from A to C, resulting in a lens that
     * directly focuses from S to C.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // addressLens: Person -> Address
     * // cityLens: Address -> String
     * Lens<Person, Person, String, String> personCityLens = addressLens.compose(cityLens);
     *
     * Person alice = new Person("Alice", new Address("Main St", "Boston"));
     * String city = personCityLens.get(alice); // "Boston"
     * }</pre>
     *
     * @param other the lens to compose with, focusing from A to C, must not be {@code null}
     * @param <C>   the new focus type (the type that {@code other} focuses on)
     * @param <D>   the new modified focus type (for polymorphic updates)
     * @return a composed lens that focuses from S directly to C, never {@code null}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @NotNull
    default <C, D> Lens<S, T, C, D> compose(@NotNull final Lens<A, B, C, D> other) {
        final Lens<S, T, A, B> self = this;
        return new Lens<>() {
            @NotNull
            @Override
            public String id() {
                return self.id() + "." + other.id();
            }

            @NotNull
            @Override
            public C get(@NotNull final S source) {
                return other.get(self.get(source));
            }

            @NotNull
            @Override
            public T set(@NotNull final S source,
                         @NotNull final D value) {
                return self.set(source, other.set(self.get(source), value));
            }

            @Override
            public @NotNull <E, F> Optic<S, T, E, F> compose(@NotNull final Optic<C, D, E, F> next) {
                if (next instanceof Lens<C, D, E, F> lens) {
                    return this.compose(lens);
                }
                throw new UnsupportedOperationException("Cannot compose Lens with " + next.getClass().getSimpleName());
            }
        };
    }

    @Override
    default @NotNull <C, D> Optic<S, T, C, D> compose(@NotNull final Optic<A, B, C, D> other) {
        if (other instanceof Lens<A, B, C, D> lens) {
            return compose(lens);
        }
        throw new UnsupportedOperationException("Cannot compose Lens with " + other.getClass().getSimpleName());
    }

    /**
     * Creates a monomorphic lens from getter and setter functions.
     *
     * <p>This factory method is the primary way to create lenses. It constructs
     * a lens where the types don't change during updates (S=T and A=B), which
     * covers the vast majority of use cases.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create a lens for a Person's name field
     * Lens<Person, Person, String, String> nameLens = Lens.of(
     *     "person.name",
     *     Person::name,
     *     (person, newName) -> new Person(newName, person.age())
     * );
     *
     * // For records, the setter creates a new instance with the modified field
     * Lens<Point, Point, Integer, Integer> xLens = Lens.of(
     *     "point.x",
     *     Point::x,
     *     (point, newX) -> new Point(newX, point.y())
     * );
     * }</pre>
     *
     * @param id     a unique identifier for this lens (used for debugging and composition),
     *               must not be {@code null}
     * @param getter the function to extract the focused value from the source,
     *               must not be {@code null}
     * @param setter the function to create a new source with an updated focus value,
     *               must not be {@code null}
     * @param <S>    the source/whole type
     * @param <A>    the focus/part type
     * @return a new monomorphic lens, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    static <S, A> Lens<S, S, A, A> of(@NotNull final String id,
                                      @NotNull final Function<S, A> getter,
                                      @NotNull final BiFunction<S, A, S> setter) {
        return new Lens<>() {
            @NotNull
            @Override
            public String id() {
                return id;
            }

            @NotNull
            @Override
            public A get(@NotNull final S source) {
                return getter.apply(source);
            }

            @NotNull
            @Override
            public S set(@NotNull final S source, @NotNull final A value) {
                return setter.apply(source, value);
            }
        };
    }
}
