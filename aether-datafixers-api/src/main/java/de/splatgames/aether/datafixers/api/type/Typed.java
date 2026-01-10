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

package de.splatgames.aether.datafixers.api.type;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.exception.EncodeException;
import de.splatgames.aether.datafixers.api.optic.Finder;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Either;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * An immutable wrapper that pairs a value with its {@link Type} information.
 *
 * <p>{@code Typed} values are central to the data fixing system, enabling
 * type-safe data manipulation. By pairing a value with its type, the system can ensure that transformations preserve
 * type correctness and can encode/decode values correctly.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Type Safety:</b> Carries type information alongside the value</li>
 *   <li><b>Encoding:</b> {@link #encode(DynamicOps)} to convert to dynamic format</li>
 *   <li><b>Updates:</b> {@link #update(Function)} for value-level transformations</li>
 *   <li><b>Navigation:</b> {@link #getAt(DynamicOps, Finder)} and {@link #updateAt(DynamicOps, Finder, Function)}
 *       for nested modifications</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a typed value
 * Type<String> stringType = Type.STRING;
 * Typed<String> typedName = new Typed<>(stringType, "Alice");
 *
 * // Encode to JSON
 * DataResult<Dynamic<JsonElement>> json = typedName.encode(GsonOps.INSTANCE);
 *
 * // Transform the value
 * Typed<String> upper = typedName.update(String::toUpperCase);
 *
 * // Read a typed value from dynamic data
 * DataResult<Typed<String>> parsed = stringType.readTyped(dynamicData);
 * }</pre>
 *
 * <h2>In DataFixers</h2>
 * <p>{@code Typed} values are produced by {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule}
 * transformations and used as the primary data carrier throughout the fixing process.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @param <A> the type of the value
 * @author Erik Pförtner
 * @see Type
 * @see Type#readTyped(Dynamic)
 * @since 0.1.0
 */
public final class Typed<A> {
    private final Type<A> type;
    private final A value;

    /**
     * Creates a new typed value pairing the given type with its value.
     *
     * <p>This constructor is typically used internally by {@link Type#readTyped(Dynamic)}
     * when parsing data, but can also be used directly to create typed wrappers for values you've created
     * programmatically.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create a typed string
     * Typed<String> typedName = new Typed<>(Type.STRING, "Alice");
     *
     * // Create a typed list
     * Type<List<Integer>> listType = Type.list(Type.INT);
     * Typed<List<Integer>> typedNumbers = new Typed<>(listType, List.of(1, 2, 3));
     *
     * // Typically obtained from parsing
     * DataResult<Typed<String>> parsed = Type.STRING.readTyped(dynamic);
     * }</pre>
     *
     * @param type  the type describing the value's structure, must not be {@code null}
     * @param value the actual value, must not be {@code null}
     * @throws NullPointerException if {@code type} or {@code value} is {@code null}
     */
    public Typed(@NotNull final Type<A> type,
                 @NotNull final A value) {
        Preconditions.checkNotNull(type, "Type<A> type must not be null");
        Preconditions.checkNotNull(value, "A value must not be null");

        this.type = type;
        this.value = value;
    }

    /**
     * Returns the type that describes this value's structure.
     *
     * <p>The type provides the codec for serialization and the type reference
     * for matching in rewrite rules.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Typed<String> typedName = new Typed<>(Type.STRING, "Alice");
     *
     * Type<String> type = typedName.type();
     * TypeReference ref = type.reference();
     * Codec<String> codec = type.codec();
     *
     * // Use type for matching
     * if (typed.type().reference().equals(Type.STRING.reference())) {
     *     System.out.println("This is a string!");
     * }
     * }</pre>
     *
     * @return the type of this value, never {@code null}
     */
    @NotNull
    public Type<A> type() {
        return this.type;
    }

    /**
     * Returns the wrapped value.
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Typed<String> typedName = new Typed<>(Type.STRING, "Alice");
     * String name = typedName.value();  // "Alice"
     *
     * Typed<List<Integer>> typedNumbers = new Typed<>(
     *     Type.list(Type.INT),
     *     List.of(1, 2, 3)
     * );
     * List<Integer> numbers = typedNumbers.value();  // [1, 2, 3]
     * }</pre>
     *
     * @return the value, never {@code null}
     */
    @NotNull
    public A value() {
        return this.value;
    }

    /**
     * Creates a new typed value with the same type but a different value.
     *
     * <p>This is useful when you want to modify the value while preserving
     * the type information. The original {@code Typed} is unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Typed<String> original = new Typed<>(Type.STRING, "hello");
     * Typed<String> updated = original.withValue("HELLO");
     *
     * System.out.println(original.value());  // "hello" (unchanged)
     * System.out.println(updated.value());   // "HELLO"
     *
     * // Same type reference
     * assert original.type() == updated.type();
     * }</pre>
     *
     * @param newValue the new value to wrap, must not be {@code null}
     * @return a new {@code Typed} with the same type and the new value, never {@code null}
     * @throws NullPointerException if {@code newValue} is {@code null}
     */
    @NotNull
    public Typed<A> withValue(@NotNull final A newValue) {
        Preconditions.checkNotNull(newValue, "A newValue must not be null");

        return new Typed<>(this.type, newValue);
    }

    /**
     * Serializes this typed value to a dynamic representation.
     *
     * <p>This method uses the type's codec to convert the Java value into
     * the format specified by the {@link DynamicOps}. The result is wrapped in a {@link DataResult} to handle potential
     * encoding failures.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Typed<String> typedName = new Typed<>(Type.STRING, "Alice");
     *
     * // Encode to JSON
     * DataResult<Dynamic<JsonElement>> jsonResult = typedName.encode(GsonOps.INSTANCE);
     * jsonResult.ifSuccess(dynamic -> {
     *     JsonElement json = dynamic.value();
     *     System.out.println(json);  // "Alice"
     * });
     *
     * // Encode to NBT
     * DataResult<Dynamic<Tag>> nbtResult = typedName.encode(NbtOps.INSTANCE);
     * }</pre>
     *
     * @param ops the dynamic operations defining the target format, must not be {@code null}
     * @param <T> the underlying data format type (e.g., JsonElement, Tag)
     * @return a {@link DataResult} containing the encoded dynamic or an error, never {@code null}
     * @throws NullPointerException if {@code ops} is {@code null}
     * @see #encodeOrThrow(DynamicOps)
     */
    @NotNull
    public <T> DataResult<Dynamic<T>> encode(@NotNull final DynamicOps<T> ops) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");

        return this.type.codec().encodeStartDynamic(ops, this.value);
    }

    /**
     * Serializes this typed value, throwing an exception on failure.
     *
     * <p>This is a convenience method for cases where encoding is expected to
     * always succeed. Use {@link #encode(DynamicOps)} when you need to handle potential failures gracefully.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Typed<String> typedName = new Typed<>(Type.STRING, "Alice");
     *
     * // When you're confident encoding will succeed
     * Dynamic<JsonElement> json = typedName.encodeOrThrow(GsonOps.INSTANCE);
     * System.out.println(json.value());  // "Alice"
     *
     * // Prefer encode() for safer error handling
     * typedName.encode(GsonOps.INSTANCE)
     *     .ifSuccess(d -> process(d))
     *     .ifError(e -> log.error("Encoding failed: " + e.message()));
     * }</pre>
     *
     * @param ops the dynamic operations defining the target format, must not be {@code null}
     * @param <T> the underlying data format type
     * @return the encoded dynamic value, never {@code null}
     * @throws EncodeException      if encoding fails
     * @throws NullPointerException if {@code ops} is {@code null}
     * @see #encode(DynamicOps)
     */
    @NotNull
    public <T> Dynamic<T> encodeOrThrow(@NotNull final DynamicOps<T> ops) {
        return encode(ops).getOrThrow(msg -> new EncodeException(msg, this.type.reference()));
    }

    /**
     * Transforms the wrapped value using a function, returning a new typed value.
     *
     * <p>This method applies a pure transformation to the value without any
     * encoding/decoding. The type remains the same. The original {@code Typed} is unchanged.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Typed<String> typedName = new Typed<>(Type.STRING, "alice");
     *
     * // Transform to uppercase
     * Typed<String> upper = typedName.update(String::toUpperCase);
     * System.out.println(upper.value());  // "ALICE"
     *
     * // Chain transformations
     * Typed<String> processed = typedName
     *     .update(String::trim)
     *     .update(String::toUpperCase)
     *     .update(s -> s + "!");
     * System.out.println(processed.value());  // "ALICE!"
     *
     * // For complex types
     * Typed<List<Integer>> numbers = new Typed<>(Type.list(Type.INT), List.of(1, 2, 3));
     * Typed<List<Integer>> doubled = numbers.update(list ->
     *     list.stream().map(n -> n * 2).toList()
     * );
     * }</pre>
     *
     * @param function the transformation to apply, must not be {@code null}
     * @return a new typed value with the transformed value, never {@code null}
     * @throws NullPointerException if {@code function} is {@code null}
     */
    @NotNull
    public Typed<A> update(@NotNull final Function<A, A> function) {
        return new Typed<>(this.type, function.apply(this.value));
    }

    /**
     * Updates this typed value by transforming its dynamic representation.
     *
     * <p>This method encodes the value to dynamic format, applies the update
     * function, then decodes back. This is useful for modifications that are easier to express on the serialized form,
     * such as adding or removing fields.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Type<Person> personType = ...;
     * Typed<Person> typed = new Typed<>(personType, person);
     *
     * // Add a new field via dynamic manipulation
     * DataResult<Typed<Person>> updated = typed.updateDynamic(
     *     GsonOps.INSTANCE,
     *     dynamic -> dynamic.set("timestamp", dynamic.createLong(System.currentTimeMillis()))
     * );
     *
     * // Rename a field
     * DataResult<Typed<Person>> renamed = typed.updateDynamic(
     *     GsonOps.INSTANCE,
     *     dynamic -> {
     *         var name = dynamic.get("name");
     *         return dynamic.remove("name").set("fullName", name);
     *     }
     * );
     * }</pre>
     *
     * @param ops     the dynamic operations for encoding/decoding, must not be {@code null}
     * @param updater the function to transform the dynamic representation, must not be {@code null}
     * @param <T>     the underlying data format type
     * @return a {@link DataResult} containing the updated typed value or an error, never {@code null}
     * @throws NullPointerException if {@code ops} or {@code updater} is {@code null}
     */
    @NotNull
    public <T> DataResult<Typed<A>> updateDynamic(@NotNull final DynamicOps<T> ops,
                                                  @NotNull final Function<Dynamic<T>, Dynamic<T>> updater) {
        return encode(ops)
                .map(updater)
                .flatMap(this.type::read)
                .map(newValue -> new Typed<>(this.type, newValue));
    }

    /**
     * Retrieves a nested value at a specific location using a finder.
     *
     * <p>This method encodes the typed value to dynamic format, then uses the
     * finder to locate and extract a specific sub-value. This is useful for accessing deeply nested fields without
     * manual traversal.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Type<Player> playerType = ...;
     * Typed<Player> typed = new Typed<>(playerType, player);
     *
     * // Find the "inventory" field
     * Finder<List<Item>> inventoryFinder = Finder.index("inventory");
     * DataResult<Dynamic<JsonElement>> inventory = typed.getAt(
     *     GsonOps.INSTANCE,
     *     inventoryFinder
     * );
     *
     * inventory.ifSuccess(dynamic -> {
     *     System.out.println("Inventory: " + dynamic.value());
     * });
     * }</pre>
     *
     * @param ops    the dynamic operations for encoding, must not be {@code null}
     * @param finder the finder that locates the desired sub-value, must not be {@code null}
     * @param <T>    the underlying data format type
     * @return a {@link DataResult} containing the found dynamic value or {@code null} if not found, never {@code null}
     * @throws NullPointerException if {@code ops} or {@code finder} is {@code null}
     * @see #updateAt(DynamicOps, Finder, Function)
     */
    @NotNull
    public <T> DataResult<Dynamic<T>> getAt(@NotNull final DynamicOps<T> ops,
                                            @NotNull final Finder<?> finder) {
        return encode(ops).map(dynamic -> {
            final Dynamic<?> found = finder.get(dynamic);
            if (found == null) {
                return null;
            }
            return found.convert(ops);
        });
    }

    /**
     * Updates a nested value at a specific location using a finder.
     *
     * <p>This method combines {@link #getAt(DynamicOps, Finder)} and
     * {@link #updateDynamic(DynamicOps, Function)} to modify a specific nested value. The typed value is encoded, the
     * finder locates and updates the sub-value, and the result is decoded back to a typed value.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Type<Player> playerType = ...;
     * Typed<Player> typed = new Typed<>(playerType, player);
     *
     * // Update the "name" field to uppercase
     * Finder<String> nameFinder = Finder.index("name");
     * DataResult<Typed<Player>> updated = typed.updateAt(
     *     GsonOps.INSTANCE,
     *     nameFinder,
     *     dynamic -> dynamic.createString(dynamic.asString("").toUpperCase())
     * );
     *
     * // Double all values in the "scores" list
     * Finder<List<Integer>> scoresFinder = Finder.index("scores");
     * DataResult<Typed<Player>> doubledScores = typed.updateAt(
     *     GsonOps.INSTANCE,
     *     scoresFinder,
     *     dynamic -> {
     *         List<Dynamic<?>> doubled = dynamic.asStream()
     *             .map(d -> d.createInt(d.asInt(0) * 2))
     *             .toList();
     *         return dynamic.createList(doubled.stream());
     *     }
     * );
     * }</pre>
     *
     * @param ops     the dynamic operations for encoding/decoding, must not be {@code null}
     * @param finder  the finder that locates the value to update, must not be {@code null}
     * @param updater the function to transform the located value, must not be {@code null}
     * @param <T>     the underlying data format type
     * @return a {@link DataResult} containing the updated typed value or an error, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @see #getAt(DynamicOps, Finder)
     * @see #updateDynamic(DynamicOps, Function)
     */
    @NotNull
    public <T> DataResult<Typed<A>> updateAt(@NotNull final DynamicOps<T> ops,
                                             @NotNull final Finder<?> finder,
                                             @NotNull final Function<Dynamic<?>, Dynamic<?>> updater) {
        return encode(ops)
                .map(dynamic -> finder.update(dynamic, updater))
                .flatMap(this.type::read)
                .map(newValue -> new Typed<>(this.type, newValue));
    }

    /**
     * Returns the child typed values of this typed value.
     *
     * <p>The children are extracted based on the type's structure. This method
     * is used by traversal combinators to recursively transform nested data structures. Each child value is paired with
     * its corresponding child type from {@link Type#children()}.</p>
     *
     * <h4>Children by Type</h4>
     * <ul>
     *   <li><b>Primitive Types:</b> No children (empty list)</li>
     *   <li><b>List Types:</b> Each list element as a child</li>
     *   <li><b>Optional Types:</b> The contained value if present</li>
     *   <li><b>Product Types:</b> First and second components</li>
     *   <li><b>Sum Types:</b> The left or right value (whichever is present)</li>
     *   <li><b>Field Types:</b> The field value as a child</li>
     * </ul>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // List type - children are list elements
     * Type<List<Integer>> listType = Type.list(Type.INT);
     * Typed<List<Integer>> typedList = new Typed<>(listType, List.of(1, 2, 3));
     * DataResult<List<Typed<?>>> children = typedList.children(GsonOps.INSTANCE);
     * // children contains: [Typed(INT, 1), Typed(INT, 2), Typed(INT, 3)]
     *
     * // Product type - children are components
     * Type<Pair<String, Integer>> pairType = Type.product(Type.STRING, Type.INT);
     * Typed<Pair<String, Integer>> typedPair = new Typed<>(pairType, Pair.of("Alice", 30));
     * DataResult<List<Typed<?>>> children = typedPair.children(GsonOps.INSTANCE);
     * // children contains: [Typed(STRING, "Alice"), Typed(INT, 30)]
     * }</pre>
     *
     * @param ops the dynamic operations for encoding, must not be {@code null}
     * @param <T> the underlying data format type
     * @return a {@link DataResult} containing the list of child typed values, never {@code null}
     * @throws NullPointerException if {@code ops} is {@code null}
     * @see Type#children()
     * @see #withChildren(DynamicOps, List)
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> DataResult<List<Typed<?>>> children(@NotNull final DynamicOps<T> ops) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");

        final List<Type<?>> childTypes = this.type.children();
        if (childTypes.isEmpty()) {
            return DataResult.success(List.of());
        }

        // Handle List type specially - each element is a child
        if (this.value instanceof List<?> list) {
            if (childTypes.size() == 1) {
                final Type<?> elementType = childTypes.get(0);
                final List<Typed<?>> children = new ArrayList<>();
                for (final Object element : list) {
                    children.add(new Typed<>((Type<Object>) elementType, element));
                }
                return DataResult.success(List.copyOf(children));
            }
        }

        // Handle Optional type - present value is the child
        if (this.value instanceof Optional<?> opt) {
            if (childTypes.size() == 1) {
                if (opt.isEmpty()) {
                    return DataResult.success(List.of());
                }
                final Type<?> elementType = childTypes.get(0);
                return DataResult.success(List.of(new Typed<>((Type<Object>) elementType, opt.get())));
            }
        }

        // Handle Pair type - first and second are children
        if (this.value instanceof Pair<?, ?> pair) {
            if (childTypes.size() == 2) {
                final Type<?> firstType = childTypes.get(0);
                final Type<?> secondType = childTypes.get(1);
                return DataResult.success(List.of(
                        new Typed<>((Type<Object>) firstType, pair.first()),
                        new Typed<>((Type<Object>) secondType, pair.second())
                ));
            }
        }

        // Handle Either type - left or right is the child
        if (this.value instanceof Either<?, ?> either) {
            if (childTypes.size() == 2) {
                final Type<?> leftType = childTypes.get(0);
                final Type<?> rightType = childTypes.get(1);
                return either.fold(
                        left -> DataResult.success(List.<Typed<?>>of(new Typed<>((Type<Object>) leftType, left))),
                        right -> DataResult.success(List.<Typed<?>>of(new Typed<>((Type<Object>) rightType, right)))
                );
            }
        }

        // For other composite types (field, named, etc.), the value itself corresponds to the child
        if (childTypes.size() == 1) {
            final Type<?> childType = childTypes.get(0);
            return DataResult.success(List.of(new Typed<>((Type<Object>) childType, this.value)));
        }

        return DataResult.error("Cannot extract children for type: " + this.type.describe());
    }

    /**
     * Creates a new typed value by replacing child values.
     *
     * <p>This method reconstructs the typed value with new child values,
     * preserving the type structure. It is the inverse of {@link #children(DynamicOps)} and is used by traversal
     * combinators to reassemble transformed data.</p>
     *
     * <h4>Constraints</h4>
     * <ul>
     *   <li>The number of new children must match the original child count</li>
     *   <li>Each new child must be compatible with its corresponding child type</li>
     * </ul>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Transform list elements
     * Type<List<Integer>> listType = Type.list(Type.INT);
     * Typed<List<Integer>> typedList = new Typed<>(listType, List.of(1, 2, 3));
     *
     * // Double each element
     * DataResult<List<Typed<?>>> children = typedList.children(GsonOps.INSTANCE);
     * List<Typed<?>> doubledChildren = children.result().orElseThrow().stream()
     *     .map(child -> child.update(v -> ((Integer) v) * 2))
     *     .toList();
     *
     * DataResult<Typed<List<Integer>>> doubled = typedList.withChildren(
     *     GsonOps.INSTANCE, doubledChildren
     * );
     * // doubled contains: Typed(List<INT>, [2, 4, 6])
     * }</pre>
     *
     * @param ops         the dynamic operations for encoding/decoding, must not be {@code null}
     * @param newChildren the new child values to use, must not be {@code null}
     * @param <T>         the underlying data format type
     * @return a {@link DataResult} containing the reconstructed typed value or an error, never {@code null}
     * @throws NullPointerException if {@code ops} or {@code newChildren} is {@code null}
     * @see #children(DynamicOps)
     * @see Type#children()
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> DataResult<Typed<A>> withChildren(@NotNull final DynamicOps<T> ops,
                                                 @NotNull final List<Typed<?>> newChildren) {
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");
        Preconditions.checkNotNull(newChildren, "List<Typed<?>> newChildren must not be null");

        final List<Type<?>> childTypes = this.type.children();
        if (childTypes.isEmpty()) {
            if (newChildren.isEmpty()) {
                return DataResult.success(this);
            }
            return DataResult.error("Cannot apply children to primitive type: " + this.type.describe());
        }

        // Handle List type - reconstruct list from children
        if (this.value instanceof List<?>) {
            final List<Object> newList = new ArrayList<>();
            for (final Typed<?> child : newChildren) {
                newList.add(child.value());
            }
            return DataResult.success(new Typed<>(this.type, (A) List.copyOf(newList)));
        }

        // Handle Optional type
        if (this.value instanceof Optional<?>) {
            if (newChildren.isEmpty()) {
                return DataResult.success(new Typed<>(this.type, (A) Optional.empty()));
            }
            if (newChildren.size() == 1) {
                return DataResult.success(new Typed<>(this.type, (A) Optional.of(newChildren.get(0).value())));
            }
            return DataResult.error("Optional type expects 0 or 1 children, got: " + newChildren.size());
        }

        // Handle Pair type
        if (this.value instanceof Pair<?, ?>) {
            if (newChildren.size() != 2) {
                return DataResult.error("Pair type expects 2 children, got: " + newChildren.size());
            }
            final Pair<?, ?> newPair = Pair.of(newChildren.get(0).value(), newChildren.get(1).value());
            return DataResult.success(new Typed<>(this.type, (A) newPair));
        }

        // Handle Either type
        if (this.value instanceof Either<?, ?> originalEither) {
            if (newChildren.size() != 1) {
                return DataResult.error("Either type expects 1 child, got: " + newChildren.size());
            }
            final Object newValue = newChildren.get(0).value();
            final Either<?, ?> newEither = originalEither.isLeft()
                    ? Either.left(newValue)
                    : Either.right(newValue);
            return DataResult.success(new Typed<>(this.type, (A) newEither));
        }

        // For other composite types (field, named, etc.), the child value becomes the new value
        if (childTypes.size() == 1 && newChildren.size() == 1) {
            return DataResult.success(new Typed<>(this.type, (A) newChildren.get(0).value()));
        }

        return DataResult.error("Cannot apply children for type: " + this.type.describe());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Typed<?> other)) {
            return false;
        }
        return this.type.reference().equals(other.type.reference()) && this.value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return 31 * this.type.reference().hashCode() + this.value.hashCode();
    }

    @Override
    public String toString() {
        return "Typed{type=" + this.type.describe() + ", value=" + this.value + "}";
    }
}
