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

package de.splatgames.aether.datafixers.core.type;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple {@link HashMap}-based implementation of {@link TypeRegistry}.
 *
 * <p>{@code SimpleTypeRegistry} stores types in a {@link HashMap} keyed by
 * their {@link TypeReference}. This provides O(1) lookup and registration
 * for typical use cases.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * TypeRegistry registry = new SimpleTypeRegistry();
 * registry.register(playerType);
 * registry.register(worldType);
 *
 * Type<?> player = registry.require(TypeReferences.PLAYER);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This implementation is not thread-safe. For concurrent access, external
 * synchronization is required.</p>
 *
 * @author Erik Pförtner
 * @see TypeRegistry
 * @see Type
 * @since 0.1.0
 */
public final class SimpleTypeRegistry implements TypeRegistry {

    /**
     * The backing map storing type registrations keyed by their type reference.
     *
     * <p>This field is initially a mutable {@link HashMap} and is replaced with an
     * unmodifiable copy when the registry is {@link #freeze() frozen}.</p>
     */
    private Map<TypeReference, Type<?>> types = new HashMap<>();

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
     * <p>In this implementation, registering a type whose {@link Type#reference()}
     * matches an already registered type will silently replace the existing registration.</p>
     *
     * @param type the type to register; must not be {@code null}
     * @throws NullPointerException  if {@code type} is {@code null}
     * @throws IllegalStateException if this registry has been {@link #freeze() frozen}
     */
    @Override
    public void register(@NotNull final Type<?> type) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkState(!this.frozen, "Registry is frozen and cannot be modified");

        this.types.put(type.reference(), type);
    }

    /**
     * {@inheritDoc}
     *
     * @param ref the type reference to look up; must not be {@code null}
     * @return the type for the given reference, or {@code null} if not registered
     * @throws NullPointerException if {@code ref} is {@code null}
     */
    @Override
    @Nullable
    public Type<?> get(@NotNull final TypeReference ref) {
        Preconditions.checkNotNull(ref, "ref must not be null");

        return this.types.get(ref);
    }

    /**
     * {@inheritDoc}
     *
     * @param ref the type reference to check; must not be {@code null}
     * @return {@code true} if a type is registered for the given reference; {@code false} otherwise
     * @throws NullPointerException if {@code ref} is {@code null}
     */
    @Override
    public boolean has(@NotNull final TypeReference ref) {
        Preconditions.checkNotNull(ref, "ref must not be null");

        return this.types.containsKey(ref);
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the registry is frozen, the backing map's key set is returned directly
     * (already unmodifiable). Otherwise, a defensive copy is returned.</p>
     *
     * @return an unmodifiable set of all registered type references; never {@code null}
     */
    @NotNull
    @Override
    public Set<TypeReference> references() {
        return this.frozen ? this.types.keySet() : Set.copyOf(this.types.keySet());
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
            this.types = Map.copyOf(this.types);
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
