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

package de.splatgames.aether.datafixers.api.type.template;

import de.splatgames.aether.datafixers.api.type.Type;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * A type family provides type parameters for instantiating {@link TypeTemplate} instances.
 *
 * <p>A {@code TypeFamily} acts as a context for type instantiation, mapping indices to
 * concrete {@link Type} instances. This enables parameterized and recursive type definitions where types can reference
 * other types by index, including themselves.</p>
 *
 * <h2>Purpose</h2>
 * <p>Type families serve several key purposes:</p>
 * <ul>
 *   <li>Provide type parameters for parameterized types (like generics)</li>
 *   <li>Enable recursive type definitions by allowing self-references</li>
 *   <li>Decouple type templates from their concrete instantiations</li>
 *   <li>Support polymorphic type construction</li>
 * </ul>
 *
 * <h2>Factory Methods</h2>
 * <ul>
 *   <li>{@link #of(Type[])} - Create a family from a fixed array of types</li>
 *   <li>{@link #of(java.util.function.IntFunction)} - Create a family from a function</li>
 *   <li>{@link #recursive(java.util.function.Function)} - Create a family for recursive types</li>
 *   <li>{@link #empty()} - Create an empty family (for non-parameterized types)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Simple Type Family</h3>
 * <pre>{@code
 * // Create a family with two type parameters
 * TypeFamily family = TypeFamily.of(Type.STRING, Type.INT);
 *
 * // Access types by index
 * Type<?> first = family.apply(0);  // Type.STRING
 * Type<?> second = family.apply(1); // Type.INT
 * }</pre>
 *
 * <h3>Recursive Type Family</h3>
 * <pre>{@code
 * // Define a recursive linked list type
 * TypeFamily listFamily = TypeFamily.recursive(self -> {
 *     // 'self' allows us to reference the type we're defining
 *     Type<?> listType = self.apply(0);
 *     return Type.optional(
 *         Type.product(
 *             Type.field("value", Type.INT),
 *             Type.field("next", listType)
 *         )
 *     );
 * });
 *
 * // Get the recursive type
 * Type<?> linkedListType = listFamily.apply(0);
 * }</pre>
 *
 * <h3>Empty Family for Simple Types</h3>
 * <pre>{@code
 * // For types with no parameters
 * TypeFamily empty = TypeFamily.empty();
 * Type<?> simpleType = template.apply(empty);
 * }</pre>
 *
 * <h2>Relationship to TypeTemplate</h2>
 * <p>A {@link TypeTemplate} uses a type family to resolve type parameters during
 * instantiation. The template calls {@link #apply(int)} to get types at specific
 * indices, which the family provides.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>TypeFamily implementations returned by the factory methods are thread-safe
 * after construction. However, care should be taken with recursive families during
 * the construction phase.</p>
 *
 * @author Erik Pförtner
 * @see TypeTemplate
 * @see Type
 * @since 0.1.0
 */
public interface TypeFamily {

    /**
     * Creates a type family from a fixed array of types.
     *
     * <p>This is the simplest way to create a type family. The array elements
     * become the type parameters accessible by index. This is commonly used for parameterized types like generics.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Family with string key and integer value (for a Map-like type)
     * TypeFamily mapFamily = TypeFamily.of(Type.STRING, Type.INT);
     *
     * // Access types by index
     * Type<?> keyType = mapFamily.apply(0);    // Type.STRING
     * Type<?> valueType = mapFamily.apply(1);  // Type.INT
     *
     * // Use in a template
     * TypeTemplate mapTemplate = family -> Type.map(family.apply(0), family.apply(1));
     * Type<?> stringIntMap = mapTemplate.apply(mapFamily);
     * }</pre>
     *
     * @param types the types to include in the family, must not be {@code null}
     * @return a type family providing the specified types by index, never {@code null}
     * @throws NullPointerException if {@code types} or any element is {@code null}
     */
    @NotNull
    static TypeFamily of(@NotNull final Type<?>... types) {
        final List<Type<?>> typeList = List.of(types);
        return index -> {
            if (index < 0 || index >= typeList.size()) {
                throw new IndexOutOfBoundsException("Type family index " + index + " out of bounds (size: " + typeList.size() + ")");
            }
            return typeList.get(index);
        };
    }

    /**
     * Creates a type family from a function that computes types on demand.
     *
     * <p>This factory allows dynamic type computation based on the requested
     * index. It's useful for infinite or computed type families where the types are determined at access time.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Family that returns string type at index 0, int for all others
     * TypeFamily dynamic = TypeFamily.of(index ->
     *     index == 0 ? Type.STRING : Type.INT
     * );
     *
     * Type<?> first = dynamic.apply(0);   // Type.STRING
     * Type<?> other = dynamic.apply(5);   // Type.INT
     * }</pre>
     *
     * @param function the function mapping indices to types, must not be {@code null}
     * @return a type family that delegates to the function, never {@code null}
     * @throws NullPointerException if {@code function} is {@code null}
     */
    @NotNull
    static TypeFamily of(@NotNull final IntFunction<Type<?>> function) {
        return function::apply;
    }

    /**
     * Creates a type family for recursive types that reference themselves.
     *
     * <p>The provided function receives a type family that can be used to reference
     * the type being defined at index 0, enabling recursive definitions like linked lists, trees, or nested
     * structures.</p>
     *
     * <p><strong>Important:</strong> The self-reference family must not be accessed
     * during the definition function's initial call—it's only valid after the definition returns. Accessing index 0
     * before initialization throws an exception.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a linked list type: Optional<{value: Int, next: Self}>
     * TypeFamily listFamily = TypeFamily.recursive(self -> {
     *     Type<?> listNode = Type.product(
     *         Type.field("value", Type.INT),
     *         Type.field("next", Type.optional(self.apply(0)))  // Self-reference
     *     );
     *     return Type.optional(listNode);
     * });
     *
     * // Get the recursive type
     * Type<?> linkedListType = listFamily.apply(0);
     *
     * // Define a binary tree type
     * TypeFamily treeFamily = TypeFamily.recursive(self -> Type.product(
     *     Type.field("value", Type.STRING),
     *     Type.field("left", Type.optional(self.apply(0))),
     *     Type.field("right", Type.optional(self.apply(0)))
     * ));
     * }</pre>
     *
     * @param definition the function defining the recursive type; receives a family that provides self-reference at
     *                   index 0, must not be {@code null}
     * @return a type family where index 0 is the recursive type, never {@code null}
     * @throws NullPointerException  if {@code definition} is {@code null}
     * @throws IllegalStateException if the self-reference is accessed before initialization completes
     */
    @NotNull
    static TypeFamily recursive(@NotNull final Function<TypeFamily, Type<?>> definition) {
        // Use a mutable holder to break the recursive dependency
        final Type<?>[] holder = new Type<?>[1];
        final TypeFamily self = index -> {
            if (index != 0) {
                throw new IndexOutOfBoundsException("Recursive type family only supports index 0");
            }
            if (holder[0] == null) {
                throw new IllegalStateException("Recursive type accessed before initialization");
            }
            return holder[0];
        };
        holder[0] = definition.apply(self);
        return self;
    }

    /**
     * Creates an empty type family that has no type parameters.
     *
     * <p>The empty family is used for non-parameterized types that don't require
     * any type arguments. Any attempt to access a type by index will throw an exception.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Simple type template that doesn't use parameters
     * TypeTemplate stringType = family -> Type.STRING;
     *
     * // Use empty family since no parameters are needed
     * Type<?> type = stringType.apply(TypeFamily.empty());
     *
     * // Accessing any index throws
     * TypeFamily.empty().apply(0);  // throws IndexOutOfBoundsException
     * }</pre>
     *
     * @return an empty type family with no types, never {@code null}
     */
    @NotNull
    static TypeFamily empty() {
        return index -> {
            throw new IndexOutOfBoundsException("Empty type family has no types");
        };
    }

    /**
     * Gets the type at the given index in this family.
     *
     * <p>Type families provide type parameters by index. The valid indices depend
     * on how the family was created: array-based families have indices from 0 to length-1, function-based families may
     * support any indices, and recursive families typically only support index 0.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Array-based family with two types
     * TypeFamily pair = TypeFamily.of(Type.STRING, Type.INT);
     * Type<?> first = pair.apply(0);   // Type.STRING
     * Type<?> second = pair.apply(1);  // Type.INT
     * pair.apply(2);  // throws IndexOutOfBoundsException
     *
     * // Recursive family for self-reference
     * TypeFamily recursive = TypeFamily.recursive(self -> {
     *     Type<?> listElement = self.apply(0);  // References the type being defined
     *     return Type.product(
     *         Type.field("value", Type.INT),
     *         Type.field("next", Type.optional(listElement))
     *     );
     * });
     * }</pre>
     *
     * @param index the zero-based index of the type parameter to retrieve
     * @return the type at the specified index, never {@code null}
     * @throws IndexOutOfBoundsException if the index is outside the valid range for this family
     */
    @NotNull
    Type<?> apply(final int index);
}
