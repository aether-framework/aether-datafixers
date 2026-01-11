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
 * by {@link TypeReference}. This enables centralized codec management, where codecs can be registered once and
 * retrieved throughout the application.</p>
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
     * <p>This method associates a {@link Codec} with a {@link TypeReference}, enabling
     * later retrieval via {@link #get(TypeReference)} or {@link #require(TypeReference)}.
     * If a codec is already registered for the given reference, the behavior depends on
     * the implementation (it may replace the existing codec or throw an exception).</p>
     *
     * <h4>Example Usage</h4>
     * <pre>{@code
     * CodecRegistry registry = new SimpleCodecRegistry();
     *
     * // Register codecs for various types
     * registry.register(TypeReferences.PLAYER, playerCodec);
     * registry.register(TypeReferences.WORLD, worldCodec);
     * registry.register(TypeReferences.ENTITY, entityCodec);
     * }</pre>
     *
     * <h4>Frozen Registry</h4>
     * <p>If this registry has been {@link #freeze() frozen}, this method will throw
     * an {@link IllegalStateException}.</p>
     *
     * @param ref   the type reference to associate with the codec; must not be {@code null}
     * @param codec the codec to register for the given type reference; must not be {@code null}
     * @throws NullPointerException  if {@code ref} or {@code codec} is {@code null}
     * @throws IllegalStateException if this registry has been frozen
     * @see #get(TypeReference)
     * @see #require(TypeReference)
     */
    void register(@NotNull final TypeReference ref, @NotNull final Codec<?> codec);

    /**
     * Retrieves a codec by its type reference.
     *
     * <p>This method performs a lookup in the registry and returns the codec associated
     * with the given type reference, or {@code null} if no codec has been registered for
     * that reference. For a non-null guarantee, use {@link #require(TypeReference)} instead.</p>
     *
     * <h4>Example Usage</h4>
     * <pre>{@code
     * Codec<?> playerCodec = registry.get(TypeReferences.PLAYER);
     * if (playerCodec != null) {
     *     // Use the codec
     *     Dynamic<?> encoded = playerCodec.encodeStart(ops, player).result().orElseThrow();
     * }
     * }</pre>
     *
     * @param ref the type reference to look up; must not be {@code null}
     * @return the codec associated with the given reference, or {@code null} if no codec
     *         is registered for that reference
     * @throws NullPointerException if {@code ref} is {@code null}
     * @see #has(TypeReference)
     * @see #require(TypeReference)
     */
    @Nullable
    Codec<?> get(@NotNull final TypeReference ref);

    /**
     * Checks whether a codec is registered for the given type reference.
     *
     * <p>This method provides a way to verify the existence of a codec registration
     * without actually retrieving the codec. It is more efficient than calling
     * {@link #get(TypeReference)} and checking for {@code null} if you only need to
     * test for presence.</p>
     *
     * <h4>Example Usage</h4>
     * <pre>{@code
     * if (registry.has(TypeReferences.PLAYER)) {
     *     Codec<?> codec = registry.require(TypeReferences.PLAYER);
     *     // Safely use the codec
     * } else {
     *     // Handle missing codec case
     *     logger.warn("No codec registered for PLAYER type");
     * }
     * }</pre>
     *
     * @param ref the type reference to check for registration; must not be {@code null}
     * @return {@code true} if a codec is registered for the given reference;
     *         {@code false} otherwise
     * @throws NullPointerException if {@code ref} is {@code null}
     * @see #get(TypeReference)
     * @see #require(TypeReference)
     */
    boolean has(@NotNull final TypeReference ref);

    /**
     * Retrieves a codec by its type reference, throwing an exception if not found.
     *
     * <p>This method is similar to {@link #get(TypeReference)} but guarantees a non-null
     * return value. If no codec is registered for the given reference, an
     * {@link IllegalStateException} is thrown. Use this method when the absence of a
     * codec indicates a programming error or misconfiguration.</p>
     *
     * <h4>Example Usage</h4>
     * <pre>{@code
     * // When you expect the codec to exist
     * Codec<?> playerCodec = registry.require(TypeReferences.PLAYER);
     *
     * // Encode with confidence
     * DataResult<?> result = playerCodec.encodeStart(ops, player);
     * }</pre>
     *
     * <h4>Error Handling</h4>
     * <p>If the codec is not found, the exception message includes the type reference
     * for debugging purposes:</p>
     * <pre>
     * IllegalStateException: Missing codec for reference: TypeReference{id='unknown_type'}
     * </pre>
     *
     * @param ref the type reference to look up; must not be {@code null}
     * @return the codec associated with the given reference; never {@code null}
     * @throws NullPointerException  if {@code ref} is {@code null}
     * @throws IllegalStateException if no codec is registered for the given reference
     * @see #get(TypeReference)
     * @see #has(TypeReference)
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
     * {@link #register(TypeReference, Codec)}) will throw an {@link IllegalStateException}.
     * This is useful for ensuring thread-safety after the initialization phase is complete,
     * as an immutable registry can be safely shared across threads without synchronization.</p>
     *
     * <h4>Idempotency</h4>
     * <p>This method is idempotent - calling it multiple times has no additional effect
     * after the first call. The registry remains frozen once frozen.</p>
     *
     * <h4>Example Usage</h4>
     * <pre>{@code
     * CodecRegistry registry = new SimpleCodecRegistry();
     *
     * // Registration phase
     * registry.register(TypeReferences.PLAYER, playerCodec);
     * registry.register(TypeReferences.WORLD, worldCodec);
     *
     * // Freeze the registry
     * registry.freeze();
     *
     * // This will throw IllegalStateException
     * registry.register(TypeReferences.ENTITY, entityCodec); // throws!
     * }</pre>
     *
     * <h4>Default Implementation</h4>
     * <p>The default implementation does nothing. Implementations that support freezing
     * should override this method to transition to an immutable state.</p>
     *
     * @see #isFrozen()
     */
    default void freeze() {
        // Default: no-op for backwards compatibility
    }

    /**
     * Returns whether this registry has been frozen and is now immutable.
     *
     * <p>A frozen registry cannot accept new codec registrations. Any call to
     * {@link #register(TypeReference, Codec)} on a frozen registry will throw an
     * {@link IllegalStateException}.</p>
     *
     * <h4>Example Usage</h4>
     * <pre>{@code
     * if (!registry.isFrozen()) {
     *     // Safe to register new codecs
     *     registry.register(TypeReferences.NEW_TYPE, newCodec);
     * } else {
     *     throw new IllegalStateException("Cannot modify frozen registry");
     * }
     * }</pre>
     *
     * <h4>Default Implementation</h4>
     * <p>The default implementation returns {@code false}, indicating that the registry
     * is always mutable. Implementations that support freezing should override this method.</p>
     *
     * @return {@code true} if this registry has been frozen and is immutable;
     *         {@code false} if it is still mutable and accepts new registrations
     * @see #freeze()
     */
    default boolean isFrozen() {
        return false;
    }
}
