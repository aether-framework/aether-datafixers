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

package de.splatgames.aether.datafixers.api;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.TypeRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * A unique identifier for a data type in the data fixing system.
 *
 * <p>{@code TypeReference} serves as a key for looking up {@link Type} definitions
 * in a {@link TypeRegistry} and for associating {@link DataFix} instances with specific data types in a
 * {@link FixRegistrar}.</p>
 *
 * <h2>Naming Convention</h2>
 * <p>Type references are typically defined as constants using descriptive,
 * lowercase identifiers with underscores:</p>
 * <pre>{@code
 * public final class TypeReferences {
 *     public static final TypeReference PLAYER = new TypeReference("player");
 *     public static final TypeReference WORLD = new TypeReference("world");
 *     public static final TypeReference ENTITY = new TypeReference("entity");
 *     public static final TypeReference BLOCK_ENTITY = new TypeReference("block_entity");
 * }
 * }</pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Register a type with the registry
 * typeRegistry.register(TypeReferences.PLAYER, playerType);
 *
 * // Look up a type
 * Type<?> playerType = typeRegistry.require(TypeReferences.PLAYER);
 *
 * // Register a fix for a type
 * fixRegistrar.register(TypeReferences.PLAYER, playerMigrationFix);
 * }</pre>
 *
 * <h2>Equality</h2>
 * <p>Two type references are equal if they have the same {@link #getId() id}.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see Type
 * @see TypeRegistry
 * @see DataFix
 * @since 0.1.0
 */
public final class TypeReference {

    /**
     * The unique string identifier for this type reference.
     *
     * <p>This identifier is used as a key for type lookups in registries and serves as
     * the canonical name for the data type throughout the data fixing system. The value
     * is guaranteed to be non-null and non-empty.</p>
     *
     * <p>By convention, type identifiers use lowercase letters with underscores to
     * separate words (e.g., "player", "block_entity", "world_data").</p>
     */
    private final String id;

    /**
     * Creates a new type reference with the specified identifier.
     *
     * @param id the unique identifier for this type reference, must not be {@code null} or empty
     * @throws NullPointerException     if id is {@code null}
     * @throws IllegalArgumentException if id is empty
     */
    public TypeReference(@NotNull final String id) {
        Preconditions.checkNotNull(id, "id must not be null");
        Preconditions.checkArgument(!id.isEmpty(), "TypeReference id cannot be empty");

        this.id = id;
    }

    /**
     * Returns the unique identifier for this type reference.
     *
     * <p>The returned identifier is the canonical name used to look up type definitions
     * in a {@link TypeRegistry} and to associate {@link DataFix} instances with this
     * data type. The identifier is guaranteed to be non-null and non-empty.</p>
     *
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * TypeReference playerRef = new TypeReference("player");
     * String id = playerRef.getId(); // Returns "player"
     *
     * // Use the ID for logging or display purposes
     * logger.info("Processing type: {}", playerRef.getId());
     * }</pre>
     *
     * @return the type reference identifier as a non-null, non-empty string
     */
    @NotNull
    public String getId() {
        return this.id;
    }

    /**
     * Returns a hash code value for this type reference.
     *
     * <p>The hash code is computed solely based on the string identifier. This
     * implementation satisfies the general contract of {@link Object#hashCode()},
     * ensuring that:</p>
     * <ul>
     *   <li>If two {@code TypeReference} objects are equal according to the
     *       {@link #equals(Object)} method, then calling {@code hashCode()} on each
     *       of the two objects produces the same integer result</li>
     *   <li>The hash code value remains consistent across multiple invocations during
     *       the same execution of the application, provided the object is not modified</li>
     * </ul>
     *
     * <p>This method enables {@code TypeReference} instances to be used effectively as
     * keys in hash-based collections such as {@link java.util.HashMap} and
     * {@link java.util.HashSet}.</p>
     *
     * @return a hash code value for this type reference
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this type reference.
     *
     * <p>Two {@code TypeReference} instances are considered equal if and only if they
     * have the same string identifier (case-sensitive comparison). This method adheres
     * to the general contract of {@link Object#equals(Object)}, providing:</p>
     * <ul>
     *   <li><b>Reflexivity:</b> For any non-null {@code TypeReference x},
     *       {@code x.equals(x)} returns {@code true}</li>
     *   <li><b>Symmetry:</b> For any non-null {@code TypeReference} instances
     *       {@code x} and {@code y}, {@code x.equals(y)} returns {@code true}
     *       if and only if {@code y.equals(x)} returns {@code true}</li>
     *   <li><b>Transitivity:</b> For any non-null {@code TypeReference} instances
     *       {@code x}, {@code y}, and {@code z}, if {@code x.equals(y)} returns
     *       {@code true} and {@code y.equals(z)} returns {@code true}, then
     *       {@code x.equals(z)} returns {@code true}</li>
     *   <li><b>Consistency:</b> Multiple invocations of {@code x.equals(y)}
     *       consistently return the same result</li>
     *   <li><b>Non-nullity:</b> For any non-null {@code TypeReference x},
     *       {@code x.equals(null)} returns {@code false}</li>
     * </ul>
     *
     * <p><b>Note:</b> Two separately constructed {@code TypeReference} instances with
     * the same identifier are considered equal:</p>
     * <pre>{@code
     * TypeReference ref1 = new TypeReference("player");
     * TypeReference ref2 = new TypeReference("player");
     * assert ref1.equals(ref2); // true
     * }</pre>
     *
     * @param obj the reference object with which to compare; may be {@code null}
     * @return {@code true} if this type reference is equal to the specified object;
     *         {@code false} otherwise
     * @see #hashCode()
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TypeReference other)) {
            return false;
        }
        return this.id.equals(other.id);
    }

    /**
     * Returns a string representation of this type reference.
     *
     * <p>The returned string follows the format {@code "TypeReference{id='<identifier>'}"}.
     * This format is intended for debugging and logging purposes and provides a clear,
     * human-readable representation of the type reference.</p>
     *
     * <p><b>Example output:</b></p>
     * <pre>{@code
     * new TypeReference("player").toString()
     * // Returns "TypeReference{id='player'}"
     *
     * new TypeReference("block_entity").toString()
     * // Returns "TypeReference{id='block_entity'}"
     * }</pre>
     *
     * <p><b>Note:</b> The format of this string is not guaranteed to remain stable across
     * versions and should not be parsed programmatically. Use {@link #getId()} to retrieve
     * the identifier for programmatic use.</p>
     *
     * @return a string representation of this type reference
     * @see #getId()
     */
    @Override
    public String toString() {
        return "TypeReference{" + "id='" + this.id + '\'' + '}';
    }
}
