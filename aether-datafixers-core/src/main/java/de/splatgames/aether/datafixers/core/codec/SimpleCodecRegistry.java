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

    private Map<TypeReference, Codec<?>> codecs = new HashMap<>();
    private volatile boolean frozen = false;

    @Override
    public void register(@NotNull final TypeReference ref, @NotNull final Codec<?> codec) {
        Preconditions.checkNotNull(ref, "TypeReference ref cannot be null");
        Preconditions.checkNotNull(codec, "Codec<?> codec cannot be null");
        Preconditions.checkState(!this.frozen, "Registry is frozen and cannot be modified");

        this.codecs.put(ref, codec);
    }

    @Override
    @Nullable
    public Codec<?> get(@NotNull final TypeReference ref) {
        Preconditions.checkNotNull(ref, "TypeReference ref cannot be null");

        return this.codecs.get(ref);
    }

    @Override
    public boolean has(@NotNull final TypeReference ref) {
        Preconditions.checkNotNull(ref, "TypeReference ref cannot be null");

        return this.codecs.containsKey(ref);
    }

    @Override
    public void freeze() {
        if (!this.frozen) {
            this.codecs = Map.copyOf(this.codecs);
            this.frozen = true;
        }
    }

    @Override
    public boolean isFrozen() {
        return this.frozen;
    }
}
