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
import de.splatgames.aether.datafixers.api.schema.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * A registry for storing and retrieving {@link Type} instances by their references.
 *
 * <p>{@code TypeRegistry} provides indexed storage for types, allowing lookup
 * by {@link TypeReference}. It is used within {@link Schema} to hold all type definitions for a specific data
 * version.</p>
 *
 * <h2>Registration</h2>
 * <p>Types register themselves using their inherent {@link Type#reference()}:</p>
 * <pre>{@code
 * TypeRegistry registry = new SimpleTypeRegistry();
 * registry.register(playerType);  // Uses playerType.reference()
 * registry.register(worldType);
 * registry.register(entityType);
 * }</pre>
 *
 * <h2>Lookup</h2>
 * <pre>{@code
 * // Check if type exists
 * if (registry.has(TypeReferences.PLAYER)) {
 *     Type<?> type = registry.get(TypeReferences.PLAYER);
 * }
 *
 * // Required lookup (throws if missing)
 * Type<?> playerType = registry.require(TypeReferences.PLAYER);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be thread-safe for concurrent access.</p>
 *
 * @author Erik Pförtner
 * @see Type
 * @see TypeReference
 * @see Schema
 * @since 0.1.0
 */
public interface TypeRegistry {

    /**
     * Registers a type using its inherent {@link Type#reference()}.
     *
     * @param type the type to register, must not be {@code null}
     */
    void register(@NotNull final Type<?> type);

    /**
     * Retrieves a type by its reference.
     *
     * @param ref the type reference to look up, must not be {@code null}
     * @return the type for the given reference, or {@code null} if not registered
     */
    @Nullable
    Type<?> get(@NotNull final TypeReference ref);

    /**
     * Checks whether a type is registered for the given reference.
     *
     * @param ref the type reference to check, must not be {@code null}
     * @return {@code true} if a type is registered, {@code false} otherwise
     */
    boolean has(@NotNull final TypeReference ref);

    /**
     * Returns all registered type references.
     *
     * <p>The returned set is a snapshot of the registered references at the time
     * of the call. Modifications to the registry after this call will not be reflected in the returned set.</p>
     *
     * @return an unmodifiable set of all registered type references, never {@code null}
     * @since 0.3.0
     */
    @NotNull
    Set<TypeReference> references();

    /**
     * Retrieves a type by its reference, throwing if not found.
     *
     * @param ref the type reference to look up, must not be {@code null}
     * @return the type for the given reference, never {@code null}
     * @throws IllegalStateException if no type is registered for the reference
     */
    @NotNull
    default Type<?> require(@NotNull final TypeReference ref) {
        Preconditions.checkNotNull(ref, "ref must not be null");
        final Type<?> type = this.get(ref);
        if (type == null) {
            throw new IllegalStateException("Missing type for reference: " + ref);
        }
        return type;
    }

    /**
     * Freezes this registry, making it immutable.
     *
     * <p>After freezing, any attempt to modify the registry (e.g., via
     * {@link #register(Type)}) will throw an {@link IllegalStateException}.</p>
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
