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
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A finder locates and manipulates specific parts of dynamic data structures.
 *
 * <p>A {@code Finder} is a specialized optic designed to work with {@link Dynamic} values,
 * enabling navigation and transformation of data without coupling to any specific format (JSON, NBT, YAML, etc.).
 * Finders are the bridge between the typed optics world and the dynamic data world used in the data fixing system.</p>
 *
 * <h2>When to Use a Finder</h2>
 * <p>Use a finder when you need to:</p>
 * <ul>
 *   <li>Navigate into dynamic data structures by field name or index</li>
 *   <li>Extract or modify values at specific paths within dynamic data</li>
 *   <li>Build composable paths into arbitrary data structures</li>
 *   <li>Work with data whose structure is only known at runtime</li>
 * </ul>
 *
 * <h2>Core Operations</h2>
 * <ul>
 *   <li>{@link #get(Dynamic)} - Extract the value at the focused location (nullable)</li>
 *   <li>{@link #getOptional(Dynamic)} - Extract the value as an Optional</li>
 *   <li>{@link #set(Dynamic, Dynamic)} - Replace the value at the focused location</li>
 *   <li>{@link #update(Dynamic, Function)} - Transform the value at the focused location</li>
 * </ul>
 *
 * <h2>Built-in Finders</h2>
 * <ul>
 *   <li>{@link #field(String)} - Focus on a field by name in a map/object</li>
 *   <li>{@link #index(int)} - Focus on an element by index in a list/array</li>
 *   <li>{@link #identity()} - Focus on the root value itself</li>
 *   <li>{@link #remainder(String...)} - Focus on all fields except the specified ones</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Given JSON: {"user": {"name": "Alice", "scores": [85, 92, 78]}}
 * Dynamic<?> data = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
 *
 * // Create finders for navigation
 * Finder<?> userFinder = Finder.field("user");
 * Finder<?> nameFinder = Finder.field("name");
 * Finder<?> scoresFinder = Finder.field("scores");
 * Finder<?> firstScoreFinder = Finder.index(0);
 *
 * // Compose finders for deep access
 * Finder<?> userNameFinder = userFinder.then(nameFinder);
 * Finder<?> firstScorePath = userFinder.then(scoresFinder).then(firstScoreFinder);
 *
 * // Extract values
 * Dynamic<?> name = userNameFinder.get(data);  // Dynamic containing "Alice"
 * Dynamic<?> score = firstScorePath.get(data); // Dynamic containing 85
 *
 * // Modify values
 * Dynamic<?> updated = userNameFinder.set(data, data.createString("Bob"));
 * Dynamic<?> doubled = firstScorePath.update(data, d -> d.createInt(d.asNumber().orElse(0) * 2));
 * }</pre>
 *
 * <h2>Composition with {@code then}</h2>
 * <p>Finders compose using {@link #then} to build paths through nested structures:</p>
 * <pre>{@code
 * // Path: root -> "config" -> "database" -> "host"
 * Finder<?> dbHostFinder = Finder.field("config")
 *     .then(Finder.field("database"))
 *     .then(Finder.field("host"));
 * }</pre>
 *
 * <h2>Null Handling</h2>
 * <p>Unlike typed optics, finders deal with runtime data that may not exist.
 * The {@link #get} method returns {@code null} if the path doesn't exist,
 * while {@link #getOptional} provides a safer {@link Optional} wrapper.</p>
 *
 * <h2>Relationship to Other Optics</h2>
 * <ul>
 *   <li>Finders are like dynamic/runtime analogs of {@link Lens} or {@link Affine}</li>
 *   <li>They operate on {@link Dynamic} rather than statically typed structures</li>
 *   <li>Finders are central to implementing {@code DataFix} transformations</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>Finder implementations should be stateless and thread-safe. The built-in
 * factory methods return immutable finder instances.</p>
 *
 * @param <A> the conceptual type of value this finder focuses on (often just Object since Dynamic values are
 *            dynamically typed)
 * @author Erik Pförtner
 * @see Dynamic
 * @see Lens
 * @see Affine
 * @since 0.1.0
 */
public interface Finder<A> {

    /**
     * Creates a finder that navigates to a field by name in a map/object structure.
     *
     * <p>The returned finder extracts and modifies the value associated with
     * the given field name. If the field doesn't exist, {@link #get} returns null.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Given JSON: {"name": "Alice", "age": 30}
     * Finder<?> nameFinder = Finder.field("name");
     *
     * Dynamic<?> name = nameFinder.get(data);  // Dynamic("Alice")
     * Dynamic<?> updated = nameFinder.set(data, data.createString("Bob"));
     * // updated: {"name": "Bob", "age": 30}
     * }</pre>
     *
     * @param fieldName the name of the field to focus on, must not be {@code null}
     * @return a finder that navigates to the specified field, never {@code null}
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    @NotNull
    static Finder<Object> field(@NotNull final String fieldName) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");
        return new Finder<>() {
            @NotNull
            @Override
            public String id() {
                return "field[" + fieldName + "]";
            }

            @Nullable
            @Override
            public Dynamic<?> get(@NotNull final Dynamic<?> root) {
                Preconditions.checkNotNull(root, "root must not be null");
                return root.get(fieldName);
            }

            @NotNull
            @Override
            public Dynamic<?> set(@NotNull final Dynamic<?> root,
                                  @NotNull final Dynamic<?> newValue) {
                Preconditions.checkNotNull(root, "root must not be null");
                Preconditions.checkNotNull(newValue, "newValue must not be null");
                @SuppressWarnings("unchecked") final Dynamic<Object> typedRoot = (Dynamic<Object>) root;
                @SuppressWarnings("unchecked") final Dynamic<Object> typedNewValue = (Dynamic<Object>) newValue;
                return typedRoot.set(fieldName, typedNewValue);
            }
        };
    }

    /**
     * Creates a finder that navigates to an element by index in a list/array structure.
     *
     * <p>The returned finder extracts and modifies the element at the specified
     * index position. If the index is out of bounds (negative or beyond the list size), {@link #get} returns
     * {@code null} and {@link #set} returns the root unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Given JSON: {"scores": [85, 92, 78]}
     * Dynamic<?> data = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     *
     * Finder<?> scoresFinder = Finder.field("scores");
     * Finder<?> firstFinder = Finder.index(0);
     * Finder<?> firstScoreFinder = scoresFinder.then(firstFinder);
     *
     * Dynamic<?> first = firstScoreFinder.get(data);  // Dynamic(85)
     * Dynamic<?> updated = firstScoreFinder.set(data, data.createInt(100));
     * // updated: {"scores": [100, 92, 78]}
     *
     * // Out of bounds access
     * Finder<?> outOfBounds = scoresFinder.then(Finder.index(10));
     * Dynamic<?> missing = outOfBounds.get(data);  // null
     * }</pre>
     *
     * @param index the zero-based index of the element to focus on
     * @return a finder that navigates to the element at the specified index, never {@code null}
     */
    @NotNull
    static Finder<Object> index(final int index) {
        return new Finder<>() {
            @NotNull
            @Override
            public String id() {
                return "index[" + index + "]";
            }

            @Override
            public @Nullable Dynamic<?> get(@NotNull final Dynamic<?> root) {
                Preconditions.checkNotNull(root, "root must not be null");
                return root.asListStream()
                        .result()
                        .flatMap(stream -> stream.skip(index).findFirst())
                        .orElse(null);
            }

            @Override
            @SuppressWarnings("unchecked")
            public @NotNull Dynamic<?> set(@NotNull final Dynamic<?> root,
                                           @NotNull final Dynamic<?> newValue) {
                Preconditions.checkNotNull(root, "root must not be null");
                Preconditions.checkNotNull(newValue, "newValue must not be null");
                final var listResult = root.asListStream();
                if (listResult.isError()) {
                    return root;
                }
                final var list = listResult.result().orElseThrow().toList();
                if (index < 0 || index >= list.size()) {
                    return root;
                }
                final List<Dynamic<Object>> newList = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    if (i == index) {
                        newList.add((Dynamic<Object>) newValue);
                    } else {
                        newList.add((Dynamic<Object>) list.get(i));
                    }
                }
                final Dynamic<Object> typedRoot = (Dynamic<Object>) root;
                return typedRoot.createList(newList.stream());
            }
        };
    }

    /**
     * Creates an identity finder that focuses on the root dynamic value itself.
     *
     * <p>The identity finder is the simplest possible finder—it returns the entire
     * root as its focus and replaces the entire root when set. This is useful as a starting point for composition or as
     * a neutral element in finder chains.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Given JSON: {"name": "Alice", "age": 30}
     * Dynamic<?> data = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     *
     * Finder<?> identity = Finder.identity();
     *
     * // Get returns the entire structure
     * Dynamic<?> same = identity.get(data);  // {"name": "Alice", "age": 30}
     *
     * // Set replaces the entire structure
     * Dynamic<?> replacement = data.createString("replaced");
     * Dynamic<?> updated = identity.set(data, replacement);  // "replaced"
     *
     * // Useful as composition base
     * Finder<?> nameFinder = Finder.identity().then(Finder.field("name"));
     * // Equivalent to: Finder.field("name")
     * }</pre>
     *
     * <h4>Composition Property</h4>
     * <p>Identity is the neutral element for finder composition:</p>
     * <ul>
     *   <li>{@code identity().then(f)} is equivalent to {@code f}</li>
     *   <li>{@code f.then(identity())} is equivalent to {@code f}</li>
     * </ul>
     *
     * @return an identity finder that focuses on the root itself, never {@code null}
     */
    @NotNull
    static Finder<Object> identity() {
        return new Finder<>() {
            @NotNull
            @Override
            public String id() {
                return "identity";
            }

            @NotNull
            @Override
            public Dynamic<?> get(@NotNull final Dynamic<?> root) {
                Preconditions.checkNotNull(root, "root must not be null");
                return root;
            }

            @NotNull
            @Override
            public Dynamic<?> set(@NotNull final Dynamic<?> root,
                                  @NotNull final Dynamic<?> newValue) {
                Preconditions.checkNotNull(root, "root must not be null");
                Preconditions.checkNotNull(newValue, "newValue must not be null");
                return newValue;
            }
        };
    }

    /**
     * Creates a finder that focuses on all fields except the specified ones.
     *
     * <p>The remainder finder is useful for preserving unknown or extra fields during
     * data transformations. When you extract specific known fields from a map/object, the remainder finder captures
     * everything else, allowing round-trip preservation of data you don't explicitly handle.</p>
     *
     * <p>This finder only works on map/object structures. For non-map values,
     * {@link #get} returns {@code null}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Given JSON: {"name": "Alice", "age": 30, "city": "Boston", "country": "USA"}
     * Dynamic<?> data = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     *
     * // Create a remainder finder that excludes "name" and "age"
     * Finder<?> remainderFinder = Finder.remainder("name", "age");
     *
     * // Get returns all other fields
     * Dynamic<?> remainder = remainderFinder.get(data);
     * // remainder: {"city": "Boston", "country": "USA"}
     *
     * // Useful for data migration: extract known fields, preserve rest
     * Finder<?> nameFinder = Finder.field("name");
     * Finder<?> ageFinder = Finder.field("age");
     * Finder<?> restFinder = Finder.remainder("name", "age");
     *
     * Dynamic<?> name = nameFinder.get(data);    // "Alice"
     * Dynamic<?> age = ageFinder.get(data);      // 30
     * Dynamic<?> rest = restFinder.get(data);    // {"city": "Boston", "country": "USA"}
     * }</pre>
     *
     * <h4>Note on Set Operation</h4>
     * <p>The {@link #set} operation for remainder finders is complex and not fully
     * implemented. Currently, it returns the root unchanged. For full remainder manipulation, extract the remainder,
     * modify it, and merge manually.</p>
     *
     * @param excludedFields the field names to exclude from the remainder; must not be {@code null}, but may be empty
     * @return a finder that focuses on all fields except the excluded ones, never {@code null}
     * @throws NullPointerException if {@code excludedFields} is {@code null}
     */
    @NotNull
    static Finder<Object> remainder(@NotNull final String... excludedFields) {
        Preconditions.checkNotNull(excludedFields, "excludedFields must not be null");
        final java.util.Set<String> excluded = java.util.Set.of(excludedFields);
        return new Finder<>() {
            @NotNull
            @Override
            public String id() {
                return "remainder";
            }

            @Override
            @SuppressWarnings("unchecked")
            public @Nullable Dynamic<?> get(@NotNull final Dynamic<?> root) {
                Preconditions.checkNotNull(root, "root must not be null");
                if (!root.isMap()) {
                    return null;
                }
                final var mapResult = root.asMapStream();
                if (mapResult.isError()) {
                    return null;
                }
                final Dynamic<Object> typedRoot = (Dynamic<Object>) root;
                final var filtered = mapResult.result().orElseThrow()
                        .filter(pair -> {
                            final var keyResult = Objects.requireNonNull(pair.first(), "pair.first() should be null").asString();
                            return keyResult.isError() || !excluded.contains(keyResult.result().orElse(""));
                        })
                        .map(pair -> Pair.of(
                                ((Dynamic<Object>) pair.first()).value(),
                                ((Dynamic<Object>) Objects.requireNonNull(pair.second(), "pair.second() should not be null")).value()
                        ));
                return new Dynamic<>(typedRoot.ops(), typedRoot.ops().createMap(filtered));
            }

            @NotNull
            @Override
            @SuppressWarnings("unchecked")
            public Dynamic<?> set(@NotNull final Dynamic<?> root,
                                  @NotNull final Dynamic<?> newValue) {
                Preconditions.checkNotNull(root, "root must not be null");
                Preconditions.checkNotNull(newValue, "newValue must not be null");
                // Validation: root must be a map
                if (!root.isMap()) {
                    return root;
                }

                final var rootMapResult = root.asMapStream();
                if (rootMapResult.isError()) {
                    return root;
                }

                final Dynamic<Object> typedRoot = (Dynamic<Object>) root;
                final Dynamic<Object> typedNewValue = (Dynamic<Object>) newValue;

                // Filter root: keep only the excluded fields
                final var excludedEntries = rootMapResult.result().orElseThrow()
                        .filter(pair -> {
                            final var keyResult = Objects.requireNonNull(pair.first()).asString();
                            return keyResult.result().map(excluded::contains).orElse(false);
                        })
                        .map(pair -> Pair.of(
                                ((Dynamic<Object>) pair.first()).value(),
                                ((Dynamic<Object>) Objects.requireNonNull(pair.second())).value()
                        ));

                // Create map with only the excluded fields
                final Object excludedMap = typedRoot.ops().createMap(excludedEntries);

                // Merge: excluded fields + newValue (newValue overwrites on conflicts)
                final var mergedResult = typedRoot.ops().mergeToMap(excludedMap, typedNewValue.value());

                return mergedResult.result()
                        .<Dynamic<?>>map(merged -> new Dynamic<>(typedRoot.ops(), merged))
                        .orElse(root);
            }
        };
    }

    /**
     * Returns a unique identifier for this finder.
     *
     * <p>The identifier is used for debugging, logging, and constructing
     * composite path descriptions when finders are composed.</p>
     *
     * @return a non-null string identifying this finder, such as "field[name]" or "index[0]", never {@code null}
     */
    @NotNull
    String id();

    /**
     * Extracts the focused value from the root dynamic structure.
     *
     * <p>This operation navigates into the dynamic data structure and retrieves
     * the value at the focused location. If the path doesn't exist (e.g., a missing field or out-of-bounds index),
     * returns {@code null}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Given JSON: {"name": "Alice", "age": 30}
     * Dynamic<?> data = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     *
     * Finder<?> nameFinder = Finder.field("name");
     * Dynamic<?> name = nameFinder.get(data);   // Dynamic containing "Alice"
     *
     * Finder<?> missingFinder = Finder.field("address");
     * Dynamic<?> missing = missingFinder.get(data); // null
     * }</pre>
     *
     * @param root the root dynamic value to navigate from, must not be {@code null}
     * @return the focused value if present, or {@code null} if the path doesn't exist
     * @throws NullPointerException if {@code root} is {@code null}
     */
    @Nullable
    Dynamic<?> get(@NotNull final Dynamic<?> root);

    /**
     * Sets a new value at the focused location, returning an updated structure.
     *
     * <p>This creates a new dynamic structure with the value at the focused
     * location replaced. The original structure is not modified.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Given JSON: {"name": "Alice", "age": 30}
     * Dynamic<?> data = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     *
     * Finder<?> nameFinder = Finder.field("name");
     * Dynamic<?> updated = nameFinder.set(data, data.createString("Bob"));
     * // updated is {"name": "Bob", "age": 30}
     * }</pre>
     *
     * @param root     the root dynamic value to modify, must not be {@code null}
     * @param newValue the new value to place at the focus, must not be {@code null}
     * @return a new dynamic structure with the focused value replaced, never {@code null}
     * @throws NullPointerException if {@code root} or {@code newValue} is {@code null}
     */
    @NotNull
    Dynamic<?> set(@NotNull final Dynamic<?> root,
                   @NotNull final Dynamic<?> newValue);

    /**
     * Extracts the focused value wrapped in an Optional.
     *
     * <p>This is a null-safe alternative to {@link #get(Dynamic)}. Instead of
     * returning {@code null} for missing paths, it returns {@link Optional#empty()}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Finder<?> nameFinder = Finder.field("name");
     * Optional<Dynamic<?>> name = nameFinder.getOptional(data);
     * name.ifPresent(d -> System.out.println(d.asString()));
     * }</pre>
     *
     * @param root the root dynamic value to navigate from, must not be {@code null}
     * @return an Optional containing the focused value, or empty if not present; never {@code null}
     * @throws NullPointerException if {@code root} is {@code null}
     */
    @NotNull
    default Optional<Dynamic<?>> getOptional(@NotNull final Dynamic<?> root) {
        Preconditions.checkNotNull(root, "root must not be null");
        return Optional.ofNullable(get(root));
    }

    /**
     * Transforms the focused value using the given function.
     *
     * <p>If the focus exists, applies the updater function and sets the result.
     * If the focus doesn't exist (get returns null), returns the root unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Given JSON: {"score": 85}
     * Finder<?> scoreFinder = Finder.field("score");
     *
     * Dynamic<?> updated = scoreFinder.update(data, d -> {
     *     int score = d.asInt().orElse(0);
     *     return d.createInt(score + 10);
     * });
     * // updated is {"score": 95}
     * }</pre>
     *
     * @param root    the root dynamic value to modify, must not be {@code null}
     * @param updater the function to transform the focused value, must not be {@code null}
     * @return a new dynamic structure with the transformed value, or the original if the focus doesn't exist; never
     * {@code null}
     * @throws NullPointerException if {@code root} or {@code updater} is {@code null}
     */
    @NotNull
    default Dynamic<?> update(@NotNull final Dynamic<?> root,
                              @NotNull final Function<Dynamic<?>, Dynamic<?>> updater) {
        Preconditions.checkNotNull(root, "root must not be null");
        Preconditions.checkNotNull(updater, "updater must not be null");
        final Dynamic<?> current = get(root);
        if (current == null) {
            return root;
        }
        return set(root, updater.apply(current));
    }

    /**
     * Composes this finder with another finder to navigate deeper into the structure.
     *
     * <p>The composed finder first navigates using this finder, then applies
     * the other finder to the intermediate result. This enables building paths through nested structures.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Navigate: root -> "user" -> "address" -> "city"
     * Finder<?> cityFinder = Finder.field("user")
     *     .then(Finder.field("address"))
     *     .then(Finder.field("city"));
     *
     * Dynamic<?> city = cityFinder.get(data);
     * }</pre>
     *
     * @param other the finder to apply after this one, must not be {@code null}
     * @param <B>   the new conceptual focus type
     * @return a composed finder that navigates through both paths, never {@code null}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @NotNull
    default <B> Finder<B> then(@NotNull final Finder<B> other) {
        Preconditions.checkNotNull(other, "other must not be null");
        final Finder<A> self = this;
        return new Finder<>() {
            @NotNull
            @Override
            public String id() {
                return self.id() + "." + other.id();
            }

            @Nullable
            @Override
            public Dynamic<?> get(@NotNull final Dynamic<?> root) {
                Preconditions.checkNotNull(root, "root must not be null");
                final Dynamic<?> intermediate = self.get(root);
                if (intermediate == null) {
                    return null;
                }
                return other.get(intermediate);
            }

            @NotNull
            @Override
            public Dynamic<?> set(@NotNull final Dynamic<?> root,
                                  @NotNull final Dynamic<?> newValue) {
                Preconditions.checkNotNull(root, "root must not be null");
                Preconditions.checkNotNull(newValue, "newValue must not be null");
                return self.update(root, intermediate -> other.set(intermediate, newValue));
            }
        };
    }
}
