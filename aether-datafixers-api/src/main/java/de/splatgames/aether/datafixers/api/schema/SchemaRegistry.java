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

package de.splatgames.aether.datafixers.api.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Stream;

/**
 * A registry for managing {@link Schema} instances across data versions.
 *
 * <p>{@code SchemaRegistry} provides version-indexed storage and retrieval of
 * schemas. It is typically populated during {@link DataFixerBootstrap} initialization
 * and consulted by the {@link DataFixer} during data migration.</p>
 *
 * <h2>Registration</h2>
 * <p>Schemas can be registered using either the schema's inherent version or
 * an explicitly specified version:</p>
 * <pre>{@code
 * SchemaRegistry registry = new SimpleSchemaRegistry();
 *
 * // Register using schema's version
 * registry.register(new Schema(new DataVersion(1), v1Types));
 *
 * // Register with explicit version
 * registry.register(new DataVersion(2), schemaV2);
 * }</pre>
 *
 * <h2>Lookup</h2>
 * <pre>{@code
 * // Optional lookup
 * Schema schema = registry.get(version);
 * if (schema != null) {
 *     // Use schema
 * }
 *
 * // Required lookup (throws if missing)
 * Schema requiredSchema = registry.require(version);
 *
 * // Get the latest schema
 * Schema currentSchema = registry.latest();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be thread-safe for concurrent access.</p>
 *
 * @author Erik Pförtner
 * @see Schema
 * @see DataVersion
 * @see DataFixerBootstrap
 * @since 0.1.0
 */
public interface SchemaRegistry {

    /**
     * Registers a schema using its inherent {@link Schema#version()}.
     *
     * @param schema the schema to register, must not be {@code null}
     */
    void register(@NotNull final Schema schema);

    /**
     * Registers a schema with an explicitly specified version.
     *
     * <p>This allows registering a schema under a different version than
     * its inherent version, which can be useful for aliasing.</p>
     *
     * @param version the version to register the schema under, must not be {@code null}
     * @param schema  the schema to register, must not be {@code null}
     */
    void register(@NotNull final DataVersion version,
                  @NotNull final Schema schema);

    /**
     * Retrieves a schema by version.
     *
     * @param version the version to look up, must not be {@code null}
     * @return the schema for the given version, or {@code null} if not registered
     */
    @Nullable
    Schema get(@NotNull final DataVersion version);

    /**
     * Retrieves a schema by version, throwing if not found.
     *
     * @param version the version to look up, must not be {@code null}
     * @return the schema for the given version, never {@code null}
     * @throws IllegalStateException if no schema is registered for the version
     */
    @NotNull
    Schema require(@NotNull final DataVersion version);

    /**
     * Returns whether this registry contains any schemas.
     *
     * @return {@code true} if no schemas are registered, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Returns the schema with the highest version number.
     *
     * @return the latest schema, never {@code null}
     * @throws IllegalStateException if the registry is empty
     */
    @NotNull
    Schema latest();

    /**
     * Freezes this registry, making it immutable.
     *
     * <p>After freezing, any attempt to modify the registry (e.g., via
     * {@link #register(Schema)}) will throw an {@link IllegalStateException}.</p>
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

    /**
     * Returns a stream of all registered schemas.
     *
     * <p>The returned stream provides access to all schemas in this registry.
     * The order of schemas in the stream is implementation-dependent.</p>
     *
     * @return a stream of all schemas, never {@code null}
     * @since 0.3.0
     */
    @NotNull
    Stream<Schema> stream();

    /**
     * Returns the set of all registered versions.
     *
     * <p>The returned set is a snapshot of the registered versions at the time
     * of the call. Modifications to the registry after this call will not be
     * reflected in the returned set.</p>
     *
     * @return an unmodifiable set of all registered versions, never {@code null}
     * @since 0.3.0
     */
    @NotNull
    Set<DataVersion> versions();
}
