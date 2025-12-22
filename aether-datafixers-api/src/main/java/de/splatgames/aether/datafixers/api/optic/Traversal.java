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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A traversal focuses on zero or more parts of a data structure simultaneously.
 *
 * <p>A {@code Traversal} generalizes a {@link Lens} to focus on multiple elements instead
 * of exactly one. While a lens always targets a single field, a traversal can target
 * all elements of a list, all values matching a predicate, or any number of locations
 * within a data structure. This makes traversals ideal for bulk transformations.</p>
 *
 * <h2>When to Use a Traversal</h2>
 * <p>Use a traversal when you need to:</p>
 * <ul>
 *   <li>Transform all elements in a collection uniformly</li>
 *   <li>Apply the same operation to multiple matching fields</li>
 *   <li>Collect values from multiple locations in a structure</li>
 *   <li>Modify nested lists or arrays within complex data</li>
 * </ul>
 *
 * <h2>Core Operations</h2>
 * <ul>
 *   <li>{@link #getAll(Object)} - Extract all focused values as a stream</li>
 *   <li>{@link #modify(Object, Function)} - Transform all focused values</li>
 *   <li>{@link #set(Object, Object)} - Replace all focused values with a single value</li>
 *   <li>{@link #toList(Object)} - Collect all focused values into a list</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // A traversal that focuses on all elements of a list
 * Traversal<List<String>, List<String>, String, String> listTraversal = Traversal.of(
 *     "list.elements",
 *     list -> list.stream(),
 *     (list, modifier) -> list.stream().map(modifier).toList()
 * );
 *
 * // Usage
 * List<String> names = List.of("alice", "bob", "charlie");
 *
 * // Get all elements
 * Stream<String> stream = listTraversal.getAll(names);  // "alice", "bob", "charlie"
 * List<String> all = listTraversal.toList(names);       // ["alice", "bob", "charlie"]
 *
 * // Modify all elements
 * List<String> upper = listTraversal.modify(names, String::toUpperCase);
 * // ["ALICE", "BOB", "CHARLIE"]
 *
 * // Set all elements to the same value
 * List<String> replaced = listTraversal.set(names, "x"); // ["x", "x", "x"]
 * }</pre>
 *
 * <h2>Composition</h2>
 * <p>Traversals compose with other traversals. When composed, the resulting traversal
 * focuses on all nested matches - like a nested flatMap:</p>
 * <pre>{@code
 * // personsTraversal: Company -> Stream<Person>
 * // skillsTraversal: Person -> Stream<String>
 * Traversal<Company, Company, String, String> allSkills =
 *     personsTraversal.compose(skillsTraversal);
 *
 * // Focuses on every skill of every person in the company
 * }</pre>
 *
 * <h2>Relationship to Other Optics</h2>
 * <ul>
 *   <li>A {@link Lens} is a traversal with exactly one focus (use {@link #fromLens})</li>
 *   <li>A traversal generalizes to any number of foci (0, 1, or many)</li>
 *   <li>An empty traversal (0 foci) acts as a no-op for modifications</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Traversal implementations should be stateless and thread-safe. The returned
 * streams are lazy and should be consumed appropriately.</p>
 *
 * @param <S> the source/whole type (the container structure)
 * @param <T> the modified source type (for polymorphic updates, often same as S)
 * @param <A> the focus/element type (the type of each focused element)
 * @param <B> the modified focus type (for polymorphic updates, often same as A)
 * @author Erik Pförtner
 * @see Optic
 * @see Lens
 * @since 0.1.0
 */
public interface Traversal<S, T, A, B> extends Optic<S, T, A, B> {

    /**
     * Extracts all focused values from the source as a stream.
     *
     * <p>This operation returns a lazy stream of all elements that this traversal
     * focuses on. The stream may contain zero, one, or many elements depending
     * on the traversal's nature.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * List<String> names = List.of("Alice", "Bob", "Charlie");
     * Stream<String> stream = listTraversal.getAll(names);
     * // Stream contains: "Alice", "Bob", "Charlie"
     * }</pre>
     *
     * @param source the source structure to extract from, must not be {@code null}
     * @return a stream of all focused values (may be empty), never {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @NotNull
    Stream<A> getAll(@NotNull final S source);

    /**
     * Transforms all focused values using the given function.
     *
     * <p>This applies the modifier function to each focused element and returns
     * a new source structure with all modifications applied. The order of
     * modifications follows the traversal order.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * List<String> names = List.of("alice", "bob");
     * List<String> upper = listTraversal.modify(names, String::toUpperCase);
     * // upper contains: ["ALICE", "BOB"]
     * }</pre>
     *
     * @param source   the source structure to modify, must not be {@code null}
     * @param modifier the function to apply to each focused element, must not be {@code null}
     * @return a new source with all focused values transformed, never {@code null}
     * @throws NullPointerException if {@code source} or {@code modifier} is {@code null}
     */
    @NotNull
    T modify(@NotNull final S source,
             @NotNull final Function<A, B> modifier);

    /**
     * Replaces all focused values with the given value.
     *
     * <p>This is equivalent to {@code modify(source, a -> value)}, setting
     * every focused element to the same value.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * List<String> names = List.of("Alice", "Bob", "Charlie");
     * List<String> replaced = listTraversal.set(names, "X");
     * // replaced contains: ["X", "X", "X"]
     * }</pre>
     *
     * @param source the source structure to modify, must not be {@code null}
     * @param value  the value to set for all focused elements, must not be {@code null}
     * @return a new source with all focused values replaced, never {@code null}
     * @throws NullPointerException if {@code source} or {@code value} is {@code null}
     */
    @NotNull
    default T set(@NotNull final S source,
                  @NotNull final B value) {
        return modify(source, a -> value);
    }

    /**
     * Collects all focused values into an immutable list.
     *
     * <p>This is a convenience method equivalent to {@code getAll(source).toList()}.
     * It eagerly evaluates the stream and returns a collected list.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * List<String> names = List.of("Alice", "Bob");
     * List<String> collected = listTraversal.toList(names);
     * // collected is ["Alice", "Bob"]
     * }</pre>
     *
     * @param source the source structure to extract from, must not be {@code null}
     * @return an immutable list of all focused values, never {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    @NotNull
    default List<A> toList(@NotNull final S source) {
        return getAll(source).toList();
    }

    /**
     * Composes this traversal with another traversal for nested iteration.
     *
     * <p>The resulting traversal focuses on all combinations: for each element
     * focused by this traversal, it applies the other traversal to get nested
     * elements. This is similar to a nested flatMap operation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // companyEmployees: Company -> Stream<Employee>
     * // employeeSkills: Employee -> Stream<String>
     * Traversal<Company, Company, String, String> allSkills =
     *     companyEmployees.compose(employeeSkills);
     *
     * // allSkills focuses on every skill of every employee
     * Company company = ...;
     * List<String> skills = allSkills.toList(company);
     * }</pre>
     *
     * @param other the traversal to compose with, must not be {@code null}
     * @param <C>   the new focus type (the element type of the nested traversal)
     * @param <D>   the new modified focus type
     * @return a composed traversal focusing on all nested elements, never {@code null}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @NotNull
    default <C, D> Traversal<S, T, C, D> compose(@NotNull final Traversal<A, B, C, D> other) {
        final Traversal<S, T, A, B> self = this;
        return new Traversal<>() {
            @NotNull
            @Override
            public String id() {
                return self.id() + "." + other.id();
            }

            @NotNull
            @Override
            public Stream<C> getAll(@NotNull final S source) {
                return self.getAll(source).flatMap(other::getAll);
            }

            @NotNull
            @Override
            public T modify(@NotNull final S source,
                            @NotNull final Function<C, D> modifier) {
                return self.modify(source, a -> other.modify(a, modifier));
            }

            @Override
            public @NotNull <E, F> Optic<S, T, E, F> compose(@NotNull final Optic<C, D, E, F> next) {
                if (next instanceof Traversal<C, D, E, F> traversal) {
                    return this.compose(traversal);
                }
                throw new UnsupportedOperationException("Cannot compose Traversal with " + next.getClass().getSimpleName());
            }
        };
    }

    @Override
    default @NotNull <C, D> Optic<S, T, C, D> compose(@NotNull final Optic<A, B, C, D> other) {
        if (other instanceof Traversal<A, B, C, D> traversal) {
            return compose(traversal);
        }
        throw new UnsupportedOperationException("Cannot compose Traversal with " + other.getClass().getSimpleName());
    }

    /**
     * Creates a traversal from a lens, treating it as a single-element traversal.
     *
     * <p>Since a {@link Lens} always focuses on exactly one element, it can be
     * viewed as a traversal with a single focus. The resulting traversal's
     * {@link #getAll} returns a stream containing exactly that one element.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Lens<Person, Person, String, String> nameLens = Lens.of(
     *     "person.name",
     *     Person::name,
     *     (p, n) -> new Person(n, p.age())
     * );
     *
     * Traversal<Person, Person, String, String> nameTraversal =
     *     Traversal.fromLens(nameLens);
     *
     * Person alice = new Person("Alice", 30);
     * List<String> names = nameTraversal.toList(alice); // ["Alice"]
     * }</pre>
     *
     * @param lens the lens to convert, must not be {@code null}
     * @param <S>  the source type
     * @param <T>  the modified source type
     * @param <A>  the focus type
     * @param <B>  the modified focus type
     * @return a traversal that focuses on exactly one element, never {@code null}
     * @throws NullPointerException if {@code lens} is {@code null}
     */
    @NotNull
    static <S, T, A, B> Traversal<S, T, A, B> fromLens(@NotNull final Lens<S, T, A, B> lens) {
        return new Traversal<>() {
            @NotNull
            @Override
            public String id() {
                return lens.id();
            }

            @NotNull
            @Override
            public Stream<A> getAll(@NotNull final S source) {
                return Stream.of(lens.get(source));
            }

            @NotNull
            @Override
            public T modify(@NotNull final S source, @NotNull final Function<A, B> modifier) {
                return lens.modify(source, modifier);
            }
        };
    }

    /**
     * Creates a monomorphic traversal from extraction and modification functions.
     *
     * <p>This is the primary factory method for creating custom traversals. It
     * constructs a traversal where the types don't change during updates (S=T and A=B).</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Traversal over all elements of a list
     * Traversal<List<String>, List<String>, String, String> listTraversal =
     *     Traversal.of(
     *         "list.elements",
     *         list -> list.stream(),
     *         (list, modifier) -> list.stream().map(modifier).toList()
     *     );
     *
     * // Traversal over specific map values
     * Traversal<Map<String, Integer>, Map<String, Integer>, Integer, Integer> valuesTraversal =
     *     Traversal.of(
     *         "map.values",
     *         map -> map.values().stream(),
     *         (map, modifier) -> map.entrySet().stream()
     *             .collect(Collectors.toMap(Map.Entry::getKey, e -> modifier.apply(e.getValue())))
     *     );
     * }</pre>
     *
     * @param id     a unique identifier for this traversal, must not be {@code null}
     * @param getAll the function to extract all focused elements as a stream, must not be {@code null}
     * @param modify the function to apply a modifier to all elements and return a new source,
     *               must not be {@code null}
     * @param <S>    the source type
     * @param <A>    the focus/element type
     * @return a new monomorphic traversal, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    static <S, A> Traversal<S, S, A, A> of(@NotNull final String id,
                                           @NotNull final Function<S, Stream<A>> getAll,
                                           @NotNull final ModifyFunction<S, A> modify) {
        return new Traversal<>() {
            @NotNull
            @Override
            public String id() {
                return id;
            }

            @NotNull
            @Override
            public Stream<A> getAll(@NotNull final S source) {
                return getAll.apply(source);
            }

            @NotNull
            @Override
            public S modify(@NotNull final S source,
                            @NotNull final Function<A, A> modifier) {
                return modify.apply(source, modifier);
            }
        };
    }

    /**
     * Functional interface for applying a modification function to all focused elements.
     *
     * <p>This interface is used by {@link #of(String, Function, ModifyFunction)} to define
     * how a modifier should be applied to all elements of a traversal. Implementations
     * receive the source structure and a modifier function, and must return a new source
     * with all focused elements transformed.</p>
     *
     * <p><b>Example Implementation</b></p>
     * <pre>{@code
     * // For a List<String> traversal
     * ModifyFunction<List<String>, String> listModify =
     *     (list, modifier) -> list.stream().map(modifier).toList();
     *
     * // For a Map<K, V> value traversal
     * ModifyFunction<Map<K, V>, V> mapModify =
     *     (map, modifier) -> map.entrySet().stream()
     *         .collect(Collectors.toMap(
     *             Map.Entry::getKey,
     *             e -> modifier.apply(e.getValue())
     *         ));
     * }</pre>
     *
     * @param <S> the source/container type
     * @param <A> the focus/element type
     */
    @FunctionalInterface
    interface ModifyFunction<S, A> {
        /**
         * Applies the modifier function to all focused elements in the source.
         *
         * @param source   the source structure containing elements to modify
         * @param modifier the function to apply to each focused element
         * @return a new source with all focused elements transformed
         */
        S apply(final S source,
                final Function<A, A> modifier);
    }
}
