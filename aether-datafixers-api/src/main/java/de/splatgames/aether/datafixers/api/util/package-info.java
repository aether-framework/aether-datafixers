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
 * Utility types for functional programming patterns.
 *
 * <p>This package provides fundamental algebraic data types used throughout
 * the Aether Datafixers framework. These types enable functional programming patterns and are used extensively in the
 * codec and optics APIs.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.util.Pair} - A simple tuple
 *       holding two values of potentially different types. Used for representing
 *       key-value pairs, function results with multiple values, and codec outputs.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.util.Either} - A sum type
 *       representing a value that is either a "left" or "right" variant. Used for
 *       representing success/failure, alternatives, and union types.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.util.Unit} - A singleton type
 *       with only one value, representing "no information". Used as a return type
 *       for side-effecting operations and as a placeholder in generic contexts.</li>
 * </ul>
 *
 * <h2>Pair Usage</h2>
 * <pre>{@code
 * // Create a pair
 * Pair<String, Integer> pair = Pair.of("Alice", 30);
 *
 * // Access components
 * String name = pair.first();   // "Alice"
 * Integer age = pair.second();  // 30
 *
 * // Transform components
 * Pair<String, Integer> older = pair.mapSecond(a -> a + 1);
 * }</pre>
 *
 * <h2>Either Usage</h2>
 * <pre>{@code
 * // Create Either values
 * Either<String, Integer> left = Either.left("error message");
 * Either<String, Integer> right = Either.right(42);
 *
 * // Pattern match
 * String result = either.map(
 *     error -> "Failed: " + error,
 *     value -> "Success: " + value
 * );
 *
 * // Transform right value
 * Either<String, Integer> doubled = either.mapRight(x -> x * 2);
 * }</pre>
 *
 * <h2>Unit Usage</h2>
 * <pre>{@code
 * // Unit has only one value
 * Unit unit = Unit.INSTANCE;
 *
 * // Used in generic contexts where no value is needed
 * Codec<Unit> unitCodec = Codecs.unit(Unit.INSTANCE);
 * }</pre>
 *
 * <h2>Relationship to Standard Types</h2>
 * <ul>
 *   <li>{@code Pair<A, B>} is similar to {@code Map.Entry<A, B>} but immutable</li>
 *   <li>{@code Either<L, R>} is similar to {@code Optional} but with error info</li>
 *   <li>{@code Unit} is similar to {@code Void} but is instantiable</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.api.util.Pair
 * @see de.splatgames.aether.datafixers.api.util.Either
 * @see de.splatgames.aether.datafixers.api.util.Unit
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.util;
