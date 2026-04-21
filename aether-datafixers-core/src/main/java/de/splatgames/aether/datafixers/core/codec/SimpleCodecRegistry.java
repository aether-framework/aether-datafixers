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

package de.splatgames.aether.datafixers.core.codec;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.codec.CodecRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link HashMap}-based implementation of {@link CodecRegistry}.
 *
 * <p>{@code SimpleCodecRegistry} stores codecs in a {@link HashMap} keyed by
 * their {@link TypeReference}. This provides O(1) lookup and registration
 * for typical use cases.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * CodecRegistry registry = new SimpleCodecRegistry();
 * registry.register(TypeReferences.PLAYER, playerCodec);
 * registry.register(TypeReferences.WORLD, worldCodec);
 *
 * Codec<?> playerCodec = registry.require(TypeReferences.PLAYER);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This implementation is not thread-safe. For concurrent access, external
 * synchronization is required.</p>
 *
 * @author Erik Pförtner
 * @see CodecRegistry
 * @see Codec
 * @since 0.1.0
 */
public final class SimpleCodecRegistry implements CodecRegistry {

    /**
     * The backing map storing codec registrations keyed by their type reference.
     *
     * <p>This field is initially a mutable {@link HashMap} and is replaced with an
     * unmodifiable copy when the registry is {@link #freeze() frozen}.</p>
     */
    private Map<TypeReference, Codec<?>> codecs = new HashMap<>();

    /**
     * Whether this registry has been frozen and is now immutable.
     *
     * <p>Marked {@code volatile} to ensure visibility across threads after
     * {@link #freeze()} is called.</p>
     */
    private volatile boolean frozen = false;

    /**
     * {@inheritDoc}
     *
     * <p>In this implementation, registering a codec for a {@link TypeReference}
     * that already has a codec will silently replace the existing registration.</p>
     *
     * @param ref   the type reference to associate with the codec; must not be {@code null}
     * @param codec the codec to register for the given type reference; must not be {@code null}
     * @throws NullPointerException  if {@code ref} or {@code codec} is {@code null}
     * @throws IllegalStateException if this registry has been {@link #freeze() frozen}
     */
    @Override
    public void register(@NotNull final TypeReference ref, @NotNull final Codec<?> codec) {
        Preconditions.checkNotNull(ref, "ref must not be null");
        Preconditions.checkNotNull(codec, "codec must not be null");
        Preconditions.checkState(!this.frozen, "Registry is frozen and cannot be modified");

        this.codecs.put(ref, codec);
    }

    /**
     * {@inheritDoc}
     *
     * @param ref the type reference to look up; must not be {@code null}
     * @return the codec associated with the given reference, or {@code null} if none is registered
     * @throws NullPointerException if {@code ref} is {@code null}
     */
    @Override
    @Nullable
    public Codec<?> get(@NotNull final TypeReference ref) {
        Preconditions.checkNotNull(ref, "ref must not be null");

        return this.codecs.get(ref);
    }

    /**
     * {@inheritDoc}
     *
     * @param ref the type reference to check for registration; must not be {@code null}
     * @return {@code true} if a codec is registered for the given reference; {@code false} otherwise
     * @throws NullPointerException if {@code ref} is {@code null}
     */
    @Override
    public boolean has(@NotNull final TypeReference ref) {
        Preconditions.checkNotNull(ref, "ref must not be null");

        return this.codecs.containsKey(ref);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation is {@code synchronized} and idempotent. On the first call,
     * the mutable {@link HashMap} backing store is replaced with an unmodifiable copy via
     * {@link Map#copyOf(Map)}, and the {@link #frozen} flag is set to {@code true}. Subsequent
     * calls have no effect.</p>
     */
    @Override
    public synchronized void freeze() {
        if (!this.frozen) {
            this.codecs = Map.copyOf(this.codecs);
            this.frozen = true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if this registry has been frozen; {@code false} otherwise
     */
    @Override
    public boolean isFrozen() {
        return this.frozen;
    }
}
