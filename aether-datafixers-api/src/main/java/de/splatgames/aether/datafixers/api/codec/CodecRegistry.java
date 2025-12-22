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

import de.splatgames.aether.datafixers.api.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A registry for storing and retrieving {@link Codec} instances by type reference.
 *
 * <p>{@code CodecRegistry} provides indexed storage for codecs, allowing lookup
 * by {@link TypeReference}. This enables centralized codec management, where
 * codecs can be registered once and retrieved throughout the application.</p>
 *
 * <h2>Registration</h2>
 * <pre>{@code
 * CodecRegistry registry = new SimpleCodecRegistry();
 * registry.register(TypeReferences.PLAYER, playerCodec);
 * registry.register(TypeReferences.WORLD, worldCodec);
 * registry.register(TypeReferences.ENTITY, entityCodec);
 * }</pre>
 *
 * <h2>Lookup</h2>
 * <pre>{@code
 * // Check if codec exists
 * if (registry.has(TypeReferences.PLAYER)) {
 *     Codec<?> codec = registry.get(TypeReferences.PLAYER);
 * }
 *
 * // Required lookup (throws if missing)
 * Codec<?> playerCodec = registry.require(TypeReferences.PLAYER);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be thread-safe for concurrent access.</p>
 *
 * @author Erik Pförtner
 * @see Codec
 * @see TypeReference
 * @since 0.1.0
 */
public interface CodecRegistry {

    /**
     * Registers a codec for the given type reference.
     *
     * @param ref   the type reference to associate with the codec, must not be {@code null}
     * @param codec the codec to register, must not be {@code null}
     */
    void register(@NotNull final TypeReference ref, @NotNull final Codec<?> codec);

    /**
     * Retrieves a codec by its type reference.
     *
     * @param ref the type reference to look up, must not be {@code null}
     * @return the codec for the given reference, or {@code null} if not registered
     */
    @Nullable
    Codec<?> get(@NotNull final TypeReference ref);

    /**
     * Checks whether a codec is registered for the given reference.
     *
     * @param ref the type reference to check, must not be {@code null}
     * @return {@code true} if a codec is registered, {@code false} otherwise
     */
    boolean has(@NotNull final TypeReference ref);

    /**
     * Retrieves a codec by its type reference, throwing if not found.
     *
     * @param ref the type reference to look up, must not be {@code null}
     * @return the codec for the given reference, never {@code null}
     * @throws IllegalStateException if no codec is registered for the reference
     */
    @NotNull
    default Codec<?> require(@NotNull final TypeReference ref) {
        final Codec<?> codec = this.get(ref);
        if (codec == null) {
            throw new IllegalStateException("Missing codec for reference: " + ref);
        }
        return codec;
    }

    /**
     * Freezes this registry, making it immutable.
     *
     * <p>After freezing, any attempt to modify the registry (e.g., via
     * {@link #register(TypeReference, Codec)}) will throw an {@link IllegalStateException}.</p>
     *
     * <p>This method is idempotent - calling it multiple times has no effect
     * after the first call.</p>
     *
     * <p>Default implementation does nothing. Thread-safe implementations
     * should override this to create an immutable snapshot.</p>
     */
    default void freeze() {
        // Default: no-op for backwards compatibility
    }

    /**
     * Returns whether this registry is frozen (immutable).
     *
     * @return {@code true} if frozen, {@code false} if still mutable
     */
    default boolean isFrozen() {
        return false;
    }
}
