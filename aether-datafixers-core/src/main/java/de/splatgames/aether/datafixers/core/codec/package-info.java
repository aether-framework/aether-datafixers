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
 * Default codec registry implementations.
 *
 * <p>This package provides implementations of the
 * {@link de.splatgames.aether.datafixers.api.codec.CodecRegistry} interface
 * for managing codec lookups at runtime.</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.codec.SimpleCodecRegistry} -
 *       A straightforward map-based registry that stores codecs by their
 *       associated type class. Suitable for most use cases.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create a codec registry
 * CodecRegistry registry = new SimpleCodecRegistry();
 *
 * // Register codecs
 * registry.register(String.class, Codecs.STRING);
 * registry.register(Integer.class, Codecs.INT);
 * registry.register(Player.class, playerCodec);
 *
 * // Lookup codecs
 * Codec<String> stringCodec = registry.get(String.class);
 * }</pre>
 *
 * <h2>Integration with Type System</h2>
 * <p>Codec registries are typically used internally by the type system to
 * resolve codecs for type definitions. Most applications won't interact
 * with them directly, instead relying on the schema and type registration
 * mechanisms.</p>
 *
 * @see de.splatgames.aether.datafixers.core.codec.SimpleCodecRegistry
 * @see de.splatgames.aether.datafixers.api.codec.CodecRegistry
 * @see de.splatgames.aether.datafixers.api.codec.Codec
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.core.codec;
