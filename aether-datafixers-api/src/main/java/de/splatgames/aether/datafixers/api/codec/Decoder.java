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

import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * A functional interface for decoding typed values from dynamic representations.
 *
 * <p>A {@code Decoder} transforms format-agnostic dynamic data into typed Java objects
 * of type {@code A} using {@link DynamicOps}. This enables deserialization from various formats (JSON, NBT, etc.)
 * without coupling the decoding logic to a specific format.</p>
 *
 * <h2>Decoding with Remaining Input</h2>
 * <p>The primary {@link #decode(DynamicOps, Object)} method returns a {@link Pair} containing
 * both the decoded value and any remaining unconsumed input. This pattern enables streaming decoders and sequential
 * parsing. For most use cases, the {@link #parse(DynamicOps, Object)} method is more convenient as it discards the
 * remaining input.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Decoder<Person> personDecoder = (ops, input) -> {
 *     String name = ops.get(input, "name")
 *         .flatMap(ops::getStringValue)
 *         .getOrThrow(IllegalStateException::new);
 *     int age = ops.get(input, "age")
 *         .flatMap(ops::getNumberValue)
 *         .map(Number::intValue)
 *         .getOrThrow(IllegalStateException::new);
 *     return DataResult.success(Pair.of(new Person(name, age), input));
 * };
 *
 * // Decode from JSON
 * DataResult<Person> person = personDecoder.parse(GsonOps.INSTANCE, jsonElement);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Decoder implementations should be stateless and thread-safe.</p>
 *
 * @param <A> the type of value this decoder produces
 * @author Erik Pförtner
 * @see Encoder
 * @see Codec
 * @since 0.1.0
 */
@FunctionalInterface
public interface Decoder<A> {

    /**
     * Decodes a value from a dynamic representation.
     *
     * <p>The result includes both the decoded value and any remaining unconsumed input.
     * This pattern supports streaming and sequential parsing. For most use cases, {@link #parse(DynamicOps, Object)} is
     * more convenient.</p>
     *
     * @param ops   the dynamic operations for the source format, must not be {@code null}
     * @param input the input to decode, must not be {@code null}
     * @param <T>   the type of the dynamic representation (e.g., JsonElement)
     * @return a {@link DataResult} containing a pair of the decoded value and remaining input, or an error message
     */
    @NotNull
    <T> DataResult<Pair<A, T>> decode(@NotNull final DynamicOps<T> ops, @NotNull final T input);

    /**
     * Decodes a value from a dynamic representation, discarding any remaining input.
     *
     * <p>This is the recommended method for most decoding scenarios. It returns only
     * the decoded value without the remaining input.</p>
     *
     * <pre>{@code
     * DataResult<String> result = Codecs.STRING.parse(GsonOps.INSTANCE, jsonPrimitive);
     * }</pre>
     *
     * @param ops   the dynamic operations for the source format, must not be {@code null}
     * @param input the input to decode, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return a {@link DataResult} containing the decoded value or an error message
     */
    @NotNull
    default <T> DataResult<A> parse(@NotNull final DynamicOps<T> ops, @NotNull final T input) {
        return decode(ops, input).map(Pair::first);
    }

    /**
     * Decodes a value from a {@link Dynamic} wrapper.
     *
     * <p>This is a convenience method that extracts the ops and value from the Dynamic.</p>
     *
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     * DataResult<String> result = Codecs.STRING.parse(dynamic);
     * }</pre>
     *
     * @param input the Dynamic wrapper containing the value to decode, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return a {@link DataResult} containing the decoded value or an error message
     */
    @NotNull
    default <T> DataResult<A> parse(@NotNull final Dynamic<T> input) {
        return parse(input.ops(), input.value());
    }
}
