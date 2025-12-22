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

package de.splatgames.aether.datafixers.api.codec;

import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import org.jetbrains.annotations.NotNull;

/**
 * A functional interface for encoding typed values into dynamic representations.
 *
 * <p>An {@code Encoder} transforms Java objects of type {@code A} into a format-agnostic
 * dynamic representation using {@link DynamicOps}. This enables serialization to various
 * formats (JSON, NBT, etc.) without coupling the encoding logic to a specific format.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Encoder<Person> personEncoder = (person, ops, prefix) -> {
 *     T nameValue = ops.createString(person.name());
 *     T ageValue = ops.createInt(person.age());
 *     T result = ops.createMap(Stream.of(
 *         Pair.of(ops.createString("name"), nameValue),
 *         Pair.of(ops.createString("age"), ageValue)
 *     ));
 *     return DataResult.success(result);
 * };
 *
 * // Encode to JSON
 * DataResult<JsonElement> json = personEncoder.encodeStart(GsonOps.INSTANCE, person);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Encoder implementations should be stateless and thread-safe.</p>
 *
 * @param <A> the type of value this encoder handles
 * @author Erik Pförtner
 * @see Decoder
 * @see Codec
 * @since 0.1.0
 */
@FunctionalInterface
public interface Encoder<A> {

    /**
     * Encodes a value to a dynamic representation, merging with the given prefix.
     *
     * <p>The prefix parameter allows the encoded value to be merged into an existing
     * structure. For most use cases, use {@link #encodeStart(DynamicOps, Object)} which
     * passes an empty prefix.</p>
     *
     * @param input  the value to encode, must not be {@code null}
     * @param ops    the dynamic operations for the target format, must not be {@code null}
     * @param prefix the prefix to merge the encoded value into, must not be {@code null}
     * @param <T>    the type of the dynamic representation (e.g., JsonElement)
     * @return a {@link DataResult} containing the encoded value or an error message
     */
    @NotNull
    <T> DataResult<T> encode(@NotNull final A input, @NotNull final DynamicOps<T> ops, @NotNull final T prefix);

    /**
     * Encodes a value to a dynamic representation starting from an empty prefix.
     *
     * <p>This is the recommended method for encoding values. It creates a fresh
     * encoding without merging into existing data.</p>
     *
     * <pre>{@code
     * DataResult<JsonElement> result = encoder.encodeStart(GsonOps.INSTANCE, myValue);
     * }</pre>
     *
     * @param ops   the dynamic operations for the target format, must not be {@code null}
     * @param input the value to encode, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return a {@link DataResult} containing the encoded value or an error message
     */
    @NotNull
    default <T> DataResult<T> encodeStart(@NotNull final DynamicOps<T> ops, @NotNull final A input) {
        return encode(input, ops, ops.empty());
    }
}
