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

/**
 * Profunctor optics for composable, type-safe data access and transformation.
 *
 * <p>This package provides a comprehensive optics library that enables focusing on
 * and manipulating parts of data structures in a composable way. Optics are central
 * to the data fixer system, enabling type-safe transformations of nested data
 * without manual traversal code.</p>
 *
 * <h2>Optic Hierarchy</h2>
 * <p>Different optic types have different capabilities, forming a hierarchy from
 * most specific to most general:</p>
 * <pre>
 *                    {@link de.splatgames.aether.datafixers.api.optic.Optic}
 *                          ↑
 *     ┌──────────┬─────────┼─────────┬──────────┐
 *     │          │         │         │          │
 * {@link de.splatgames.aether.datafixers.api.optic.Iso}    {@link de.splatgames.aether.datafixers.api.optic.Lens}   {@link de.splatgames.aether.datafixers.api.optic.Prism}  {@link de.splatgames.aether.datafixers.api.optic.Affine}  {@link de.splatgames.aether.datafixers.api.optic.Traversal}
 * (1 ↔ 1)   (1 → 1)   (1 → 0..1)  (0..1)   (0..n)
 *                          ↑
 *                    {@link de.splatgames.aether.datafixers.api.optic.Getter}
 *                    (read-only)
 * </pre>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.optic.Optic} - Base interface for
 *       all optic types with composition support.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.optic.Iso} - Isomorphism for
 *       reversible 1-to-1 transformations between types.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.optic.Lens} - Focus on exactly
 *       one part of a product type (always succeeds).</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.optic.Prism} - Focus on one case
 *       of a sum type (may not match).</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.optic.Affine} - Combines lens and
 *       prism capabilities (0 or 1 focus).</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.optic.Traversal} - Focus on zero
 *       or more parts of a structure.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.optic.Getter} - Read-only focus
 *       (no modification capability).</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.optic.Finder} - Locates nested
 *       types within a schema for type-directed transformations.</li>
 * </ul>
 *
 * <h2>Lens Example</h2>
 * <pre>{@code
 * // Lens for accessing a field
 * Lens<Person, Person, String, String> nameLens = Lens.of(
 *     "person.name",
 *     Person::name,
 *     (person, newName) -> new Person(newName, person.age())
 * );
 *
 * Person alice = new Person("Alice", 30);
 * String name = nameLens.get(alice);           // "Alice"
 * Person bob = nameLens.set(alice, "Bob");     // Person("Bob", 30)
 * Person upper = nameLens.modify(alice, String::toUpperCase); // Person("ALICE", 30)
 * }</pre>
 *
 * <h2>Prism Example</h2>
 * <pre>{@code
 * // Prism for accessing a sum type variant
 * Prism<JsonValue, JsonValue, String, String> stringPrism = Prism.of(
 *     "json.string",
 *     json -> json instanceof JsonString js ? Optional.of(js.value()) : Optional.empty(),
 *     value -> new JsonString(value)
 * );
 *
 * stringPrism.getOption(new JsonString("hello")); // Optional.of("hello")
 * stringPrism.getOption(new JsonNumber(42));      // Optional.empty()
 * }</pre>
 *
 * <h2>Composition</h2>
 * <p>Optics can be composed to focus deeper into structures:</p>
 * <pre>{@code
 * Lens<Person, Person, Address, Address> addressLens = ...;
 * Lens<Address, Address, String, String> cityLens = ...;
 *
 * // Compose to focus on Person → Address → City
 * Lens<Person, Person, String, String> personCityLens = addressLens.compose(cityLens);
 *
 * Person alice = new Person("Alice", new Address("Main St", "Boston"));
 * String city = personCityLens.get(alice); // "Boston"
 * }</pre>
 *
 * <h2>Polymorphic Updates</h2>
 * <p>The four type parameters (S, T, A, B) support polymorphic updates where
 * modifying the focus can change the overall type. For monomorphic updates
 * (where types don't change), use S=T and A=B.</p>
 *
 * @see de.splatgames.aether.datafixers.api.optic.Optic
 * @see de.splatgames.aether.datafixers.api.optic.Lens
 * @see de.splatgames.aether.datafixers.api.optic.Prism
 * @see de.splatgames.aether.datafixers.api.optic.Iso
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.optic;
