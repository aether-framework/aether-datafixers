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
 * Codec interfaces for bidirectional data transformation.
 *
 * <p>This package provides the codec abstraction that enables encoding and decoding
 * of typed Java objects to and from format-agnostic {@link de.splatgames.aether.datafixers.api.dynamic.Dynamic}
 * representations. Codecs are central to the type system, allowing type-safe
 * serialization without coupling to specific formats like JSON or NBT.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.codec.Codec} - The primary interface
 *       combining both encoding and decoding capabilities. Extends both
 *       {@link de.splatgames.aether.datafixers.api.codec.Encoder} and
 *       {@link de.splatgames.aether.datafixers.api.codec.Decoder}.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.codec.Encoder} - Transforms typed
 *       values into Dynamic representations.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.codec.Decoder} - Transforms Dynamic
 *       representations back into typed values.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.codec.MapCodec} - A codec that
 *       operates on map-like structures, enabling field-based encoding/decoding.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.codec.RecordCodecBuilder} - A builder
 *       for constructing codecs for record/struct types with multiple fields.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.codec.CodecRegistry} - Registry for
 *       looking up codecs by type.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.codec.Codecs} - Factory methods for
 *       common primitive codecs (string, int, boolean, etc.).</li>
 * </ul>
 *
 * <h2>Codec Composition</h2>
 * <p>Codecs can be composed and transformed:</p>
 * <pre>{@code
 * // Primitive codecs
 * Codec<String> stringCodec = Codecs.STRING;
 * Codec<Integer> intCodec = Codecs.INT;
 *
 * // Record codec using builder
 * Codec<Player> playerCodec = RecordCodecBuilder.create(instance ->
 *     instance.group(
 *         Codecs.STRING.fieldOf("name").forGetter(Player::name),
 *         Codecs.INT.fieldOf("level").forGetter(Player::level)
 *     ).apply(instance, Player::new)
 * );
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <p>Codec operations return {@link de.splatgames.aether.datafixers.api.result.DataResult}
 * to represent success or failure without throwing exceptions:</p>
 * <pre>{@code
 * DataResult<Player> result = playerCodec.decode(dynamic);
 * result.ifSuccess(player -> System.out.println("Decoded: " + player))
 *       .ifError(error -> System.err.println("Failed: " + error.message()));
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.api.codec.Codec
 * @see de.splatgames.aether.datafixers.api.codec.Encoder
 * @see de.splatgames.aether.datafixers.api.codec.Decoder
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see de.splatgames.aether.datafixers.api.result.DataResult
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.codec;
