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

/**
 * Base interface for all optic types in the profunctor optics system.
 *
 * <p>Optics provide a composable way to focus on and manipulate parts of data
 * structures. They are central to the data fixer system, enabling type-safe transformations of nested data without
 * manual traversal code.</p>
 *
 * <h2>Optic Hierarchy</h2>
 * <p>Different optic types have different capabilities, forming a hierarchy:</p>
 * <ul>
 *   <li>{@link Iso} - Isomorphism: reversible 1-to-1 transformation between S and A</li>
 *   <li>{@link Lens} - Focus on exactly one part of a product type (always succeeds)</li>
 *   <li>{@link Prism} - Focus on one case of a sum type (may not match)</li>
 *   <li>{@link Affine} - Combines lens and prism capabilities (0 or 1 focus)</li>
 *   <li>{@link Traversal} - Focus on zero or more parts</li>
 *   <li>{@link Getter} - Read-only focus (no modification)</li>
 * </ul>
 *
 * <h2>Polymorphic Updates</h2>
 * <p>The four type parameters support polymorphic updates where modifying
 * the focus can change the overall type:</p>
 * <ul>
 *   <li>{@code S} - Source type (the whole structure)</li>
 *   <li>{@code T} - Target type after modification (may differ from S)</li>
 *   <li>{@code A} - Focus type (the part we're looking at)</li>
 *   <li>{@code B} - New focus type after modification (may differ from A)</li>
 * </ul>
 * <p>For monomorphic updates (where types don't change), S=T and A=B.</p>
 *
 * <h2>Composition</h2>
 * <p>Optics can be composed to focus deeper into structures:</p>
 * <pre>{@code
 * // Compose two lenses to focus on a nested field
 * Optic<Person, Person, Address, Address> addressOptic = personToAddress;
 * Optic<Address, Address, String, String> streetOptic = addressToStreet;
 * Optic<Person, Person, String, String> personToStreet = addressOptic.compose(streetOptic);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Optic implementations should be stateless and thread-safe.</p>
 *
 * @param <S> the source/whole type
 * @param <T> the modified source type (for polymorphic updates)
 * @param <A> the focus/part type
 * @param <B> the modified focus type (for polymorphic updates)
 * @author Erik Pförtner
 * @see Lens
 * @see Prism
 * @see Iso
 * @see Affine
 * @see Traversal
 * @see Getter
 * @since 0.1.0
 */
public interface Optic<S, T, A, B> {

    /**
     * Creates an identity optic that focuses on the entire value unchanged.
     *
     * <p>The identity optic acts as a no-op: it focuses on the whole value
     * and any modification returns the modified value as-is. It serves as the identity element for optic
     * composition.</p>
     *
     * @param <S> the type of the value
     * @return an identity optic where S=T=A=B, never {@code null}
     */
    @NotNull
    static <S> Optic<S, S, S, S> identity() {
        return new Optic<>() {
            @NotNull
            @Override
            public String id() {
                return "identity";
            }

            @Override
            public @NotNull <C, D> Optic<S, S, C, D> compose(@NotNull final Optic<S, S, C, D> other) {
                return other;
            }
        };
    }

    /**
     * Returns a unique identifier for this optic.
     *
     * <p>The identifier is used for debugging, logging, and potentially
     * for optic lookup in registries.</p>
     *
     * @return the optic identifier, never {@code null}
     */
    @NotNull
    String id();

    /**
     * Composes this optic with another optic to focus deeper into the structure.
     *
     * <p>The composition chains the focus: this optic focuses from S to A,
     * and the other optic focuses from A to C, resulting in an optic that focuses from S to C.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // this: S -> A
     * // other: A -> C
     * // result: S -> C
     * Optic<S, T, C, D> composed = this.compose(other);
     * }</pre>
     *
     * @param other the optic to compose with, focusing from A to C
     * @param <C>   the new focus type
     * @param <D>   the new modified focus type
     * @return a composed optic focusing from S to C, never {@code null}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @NotNull
    <C, D> Optic<S, T, C, D> compose(@NotNull final Optic<A, B, C, D> other);
}
