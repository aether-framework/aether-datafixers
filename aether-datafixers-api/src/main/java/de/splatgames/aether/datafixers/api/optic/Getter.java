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

import java.util.function.Function;

/**
 * A getter is a read-only optic that extracts a value from a source without modification capability.
 *
 * <p>A {@code Getter} represents the most basic form of optic: a simple function from a
 * source type to a focus type. Unlike a {@link Lens}, a getter provides no way to modify the source—it is purely for
 * extraction. This makes getters ideal when you want to explicitly communicate that a transformation is one-way and
 * read-only.</p>
 *
 * <h2>When to Use a Getter</h2>
 * <p>Use a getter when you need to:</p>
 * <ul>
 *   <li>Extract a value from a structure without needing to modify it</li>
 *   <li>Create a read-only view of a field</li>
 *   <li>Compose extraction functions in a type-safe way</li>
 *   <li>Convert a lens to read-only to prevent accidental modification</li>
 *   <li>Express derivations or computed properties</li>
 * </ul>
 *
 * <h2>Core Operation</h2>
 * <ul>
 *   <li>{@link #get(Object)} - Extract the focused value from the source</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // A record representing a point
 * record Point(int x, int y) {}
 *
 * // Create a getter for the x coordinate
 * Getter<Point, Integer> xGetter = Getter.of("point.x", Point::x);
 *
 * // Create a getter for the y coordinate
 * Getter<Point, Integer> yGetter = Getter.of("point.y", Point::y);
 *
 * // Usage
 * Point p = new Point(3, 4);
 * int x = xGetter.get(p);  // 3
 * int y = yGetter.get(p);  // 4
 *
 * // Compose getters for nested access
 * record Line(Point start, Point end) {}
 * Getter<Line, Point> startGetter = Getter.of("line.start", Line::start);
 * Getter<Line, Integer> startXGetter = startGetter.compose(xGetter);
 *
 * Line line = new Line(new Point(1, 2), new Point(5, 6));
 * int startX = startXGetter.get(line);  // 1
 * }</pre>
 *
 * <h2>Composition</h2>
 * <p>Getters compose with other getters to extract from nested structures:</p>
 * <pre>{@code
 * // personGetter: Company -> Person (e.g., CEO)
 * // nameGetter: Person -> String
 * Getter<Company, String> ceoNameGetter = personGetter.compose(nameGetter);
 * }</pre>
 *
 * <h2>Relationship to Other Optics</h2>
 * <ul>
 *   <li>A {@link Lens} can be converted to a getter via {@link #fromLens}</li>
 *   <li>A getter is essentially a {@code Function<S, A>} with a name/id</li>
 *   <li>Getters cannot be "upgraded" to lenses without providing a setter</li>
 * </ul>
 *
 * <h2>Note on Type Parameters</h2>
 * <p>Unlike other optics, a getter only has two type parameters since it cannot
 * perform modifications (no polymorphic update capability).</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Getter implementations should be stateless and thread-safe.</p>
 *
 * @param <S> the source type (the structure to extract from)
 * @param <A> the focus type (the type of the extracted value)
 * @author Erik Pförtner
 * @see Lens
 * @see Optic
 * @since 0.1.0
 */
public interface Getter<S, A> {

    /**
     * Creates a getter from a lens by discarding the modification capability.
     *
     * <p>This factory method allows using a {@link Lens} in contexts that only
     * require read access. The resulting getter uses the lens's {@link Lens#get} method for extraction.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Lens<Person, Person, String, String> nameLens = Lens.of(
     *     "person.name",
     *     Person::name,
     *     (p, n) -> new Person(n, p.age())
     * );
     *
     * // Convert to read-only getter
     * Getter<Person, String> nameGetter = Getter.fromLens(nameLens);
     * // nameGetter can only read, not modify
     * }</pre>
     *
     * @param lens the lens to convert, must not be {@code null}
     * @param <S>  the source type
     * @param <T>  the modified source type (ignored in getter)
     * @param <A>  the focus type
     * @param <B>  the modified focus type (ignored in getter)
     * @return a getter that extracts using the lens's get operation, never {@code null}
     * @throws NullPointerException if {@code lens} is {@code null}
     */
    @NotNull
    static <S, T, A, B> Getter<S, A> fromLens(@NotNull final Lens<S, T, A, B> lens) {
        Preconditions.checkNotNull(lens, "lens must not be null");
        return new Getter<>() {
            @NotNull
            @Override
            public String id() {
                return lens.id();
            }

            @NotNull
            @Override
            public A get(@NotNull final S source) {
                Preconditions.checkNotNull(source, "source must not be null");
                return lens.get(source);
            }
        };
    }

    /**
     * Creates a getter from an extraction function.
     *
     * <p>This is the primary factory method for creating getters. It wraps
     * a simple extraction function with an identifier for debugging and composition purposes.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create a getter for a record field
     * record Point(int x, int y) {}
     * Getter<Point, Integer> xGetter = Getter.of("point.x", Point::x);
     *
     * // Create a getter for a computed value
     * Getter<Point, Double> distanceGetter = Getter.of(
     *     "point.distance",
     *     p -> Math.sqrt(p.x() * p.x() + p.y() * p.y())
     * );
     *
     * Point p = new Point(3, 4);
     * int x = xGetter.get(p);           // 3
     * double dist = distanceGetter.get(p); // 5.0
     * }</pre>
     *
     * @param id     a unique identifier for this getter (used for debugging and composition), must not be {@code null}
     * @param getter the extraction function that retrieves the focus from the source, must not be {@code null}
     * @param <S>    the source type (the structure to extract from)
     * @param <A>    the focus type (the type of the extracted value)
     * @return a new getter wrapping the provided function, never {@code null}
     * @throws NullPointerException if {@code id} or {@code getter} is {@code null}
     */
    @NotNull
    static <S, A> Getter<S, A> of(@NotNull final String id,
                                  @NotNull final Function<S, A> getter) {
        Preconditions.checkNotNull(id, "id must not be null");
        Preconditions.checkNotNull(getter, "getter must not be null");
        return new Getter<>() {
            @NotNull
            @Override
            public String id() {
                return id;
            }

            @NotNull
            @Override
            public A get(@NotNull final S source) {
                Preconditions.checkNotNull(source, "source must not be null");
                return getter.apply(source);
            }
        };
    }

    /**
     * Returns a unique identifier for this getter.
     *
     * <p>The identifier is used for debugging, logging, and constructing
     * composite identifiers when getters are composed.</p>
     *
     * @return a non-null string identifying this getter, never {@code null}
     */
    @NotNull
    String id();

    /**
     * Extracts the focused value from the source structure.
     *
     * <p>This is the core operation of a getter. It applies the extraction
     * function to retrieve the focused value from the source. The operation always succeeds as the focused value is
     * guaranteed to exist.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * record Person(String name, int age) {}
     * Getter<Person, String> nameGetter = Getter.of("person.name", Person::name);
     *
     * Person alice = new Person("Alice", 30);
     * String name = nameGetter.get(alice); // Returns "Alice"
     * }</pre>
     *
     * @param source the source structure to extract from, must not be {@code null}
     * @return the focused value extracted from the source, never {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @NotNull
    A get(@NotNull final S source);

    /**
     * Composes this getter with another getter to extract from nested structures.
     *
     * <p>Composition chains the extraction: this getter extracts from S to A,
     * and the other getter extracts from A to B, resulting in a getter that directly extracts from S to B.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * record Address(String city, String street) {}
     * record Person(String name, Address address) {}
     *
     * Getter<Person, Address> addressGetter = Getter.of("person.address", Person::address);
     * Getter<Address, String> cityGetter = Getter.of("address.city", Address::city);
     *
     * // Compose to get city directly from Person
     * Getter<Person, String> personCityGetter = addressGetter.compose(cityGetter);
     *
     * Person alice = new Person("Alice", new Address("Boston", "Main St"));
     * String city = personCityGetter.get(alice); // Returns "Boston"
     * }</pre>
     *
     * @param other the getter to compose with, extracting from A to B, must not be {@code null}
     * @param <B>   the new focus type (the type that {@code other} extracts)
     * @return a composed getter that extracts from S directly to B, never {@code null}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @NotNull
    default <B> Getter<S, B> compose(@NotNull final Getter<A, B> other) {
        Preconditions.checkNotNull(other, "other must not be null");
        final Getter<S, A> self = this;
        return new Getter<>() {
            @NotNull
            @Override
            public String id() {
                return self.id() + "." + other.id();
            }

            @NotNull
            @Override
            public B get(@NotNull final S source) {
                Preconditions.checkNotNull(source, "source must not be null");
                return other.get(self.get(source));
            }
        };
    }
}
