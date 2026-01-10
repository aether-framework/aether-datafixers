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

    private final String id;

    /**
     * Creates a new type reference with the specified identifier.
     *
     * @param id the unique identifier for this type reference, must not be {@code null} or empty
     * @throws NullPointerException     if id is {@code null}
     * @throws IllegalArgumentException if id is empty
     */
    public TypeReference(@NotNull final String id) {
        Preconditions.checkNotNull(id, "TypeReference id cannot be null");
        Preconditions.checkArgument(!id.isEmpty(), "TypeReference id cannot be empty");

        this.id = id;
    }

    /**
     * Returns the unique identifier for this type reference.
     *
     * @return the type reference id, never {@code null}
     */
    @NotNull
    public String getId() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

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

    @Override
    public String toString() {
        return "TypeReference{" + "id='" + this.id + '\'' + '}';
    }
}
