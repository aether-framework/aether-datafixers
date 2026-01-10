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

package de.splatgames.aether.datafixers.api.type;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.codec.Codec;
import org.jetbrains.annotations.NotNull;

/**
 * A basic implementation of {@link Type} that pairs a reference with a codec.
 *
 * <p>{@code SimpleType} provides a straightforward way to create types by
 * combining a {@link TypeReference} identifier with a {@link Codec} for serialization. This is the most common way to
 * define types in the system.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define a type reference
 * TypeReference PLAYER_REF = new TypeReference("player");
 *
 * // Create a codec for the player type
 * Codec<Player> playerCodec = RecordCodecBuilder.create(instance ->
 *     instance.group(
 *         Codecs.STRING.fieldOf("name").forGetter(Player::name),
 *         Codecs.INT.fieldOf("health").forGetter(Player::health)
 *     ).apply(instance, Player::new)
 * );
 *
 * // Create the type
 * Type<Player> playerType = new SimpleType<>(PLAYER_REF, playerCodec);
 *
 * // Register with a type registry
 * typeRegistry.register(playerType);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @param <A> the Java type this type represents
 * @author Erik Pförtner
 * @see Type
 * @see TypeReference
 * @see Codec
 * @since 0.1.0
 */
public final class SimpleType<A> implements Type<A> {

    private final TypeReference reference;
    private final Codec<A> codec;

    /**
     * Creates a new simple type with the specified reference and codec.
     *
     * @param reference the type reference identifier, must not be {@code null}
     * @param codec     the codec for serialization, must not be {@code null}
     * @throws NullPointerException if reference or codec is {@code null}
     */
    public SimpleType(@NotNull final TypeReference reference,
                      @NotNull final Codec<A> codec) {
        Preconditions.checkNotNull(reference, "TypeReference reference must not be null");
        Preconditions.checkNotNull(codec, "Codec<A> codec must not be null");

        this.reference = reference;
        this.codec = codec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public TypeReference reference() {
        return this.reference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Codec<A> codec() {
        return this.codec;
    }
}
