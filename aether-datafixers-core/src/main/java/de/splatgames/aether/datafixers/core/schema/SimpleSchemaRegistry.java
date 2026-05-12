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

package de.splatgames.aether.datafixers.core.schema;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * A simple {@link TreeMap}-based implementation of {@link SchemaRegistry}.
 *
 * <p>{@code SimpleSchemaRegistry} stores schemas in a {@link TreeMap} ordered by
 * {@link DataVersion}. This provides efficient version-based lookups and allows
 * the {@link #get(DataVersion)} method to find the closest matching schema using
 * floor semantics.</p>
 *
 * <h2>Floor Semantics</h2>
 * <p>The {@link #get(DataVersion)} method returns the schema for the greatest
 * version less than or equal to the requested version. This allows sparse
 * schema registration where not every version needs an explicit schema.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * SchemaRegistry registry = new SimpleSchemaRegistry();
 * registry.register(schemaV1);  // version 1
 * registry.register(schemaV5);  // version 5
 *
 * Schema v1 = registry.get(new DataVersion(1));  // schemaV1
 * Schema v3 = registry.get(new DataVersion(3));  // schemaV1 (floor)
 * Schema v5 = registry.get(new DataVersion(5));  // schemaV5
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Thread-safe for concurrent reads after {@link #freeze()} is called.
 * Registration methods ({@link #register}) are not thread-safe and should
 * only be called during single-threaded initialization.</p>
 *
 * @author Erik Pförtner
 * @see SchemaRegistry
 * @see Schema
 * @since 0.1.0
 */
public final class SimpleSchemaRegistry implements SchemaRegistry {

    /**
     * The backing navigable map storing schemas ordered by {@link DataVersion}.
     *
     * <p>This field is initially a mutable {@link TreeMap} and is replaced with an
     * unmodifiable navigable map when the registry is {@link #freeze() frozen}. The
     * {@link TreeMap} ordering enables floor-based lookups in {@link #get(DataVersion)}.</p>
     */
    private NavigableMap<DataVersion, Schema> schemas = new TreeMap<>();

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
     * <p>In this implementation, registering a schema whose {@link Schema#version()}
     * matches an already registered version will silently replace the existing registration.</p>
     *
     * @param schema the schema to register; must not be {@code null}
     * @throws NullPointerException  if {@code schema} is {@code null}
     * @throws IllegalStateException if this registry has been {@link #freeze() frozen}
     */
    @Override
    public void register(@NotNull final Schema schema) {
        Preconditions.checkNotNull(schema, "schema must not be null");
        Preconditions.checkState(!this.frozen, "Registry is frozen and cannot be modified");
        this.schemas.put(schema.version(), schema);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation validates that the given {@code version} matches the
     * schema's inherent {@link Schema#version()} and throws an {@link IllegalArgumentException}
     * if they differ.</p>
     *
     * @param version the version to register the schema under; must not be {@code null}
     * @param schema  the schema to register; must not be {@code null}
     * @throws NullPointerException     if {@code version} or {@code schema} is {@code null}
     * @throws IllegalStateException    if this registry has been {@link #freeze() frozen}
     * @throws IllegalArgumentException if {@code version} does not match {@link Schema#version()}
     */
    @Override
    public void register(@NotNull final DataVersion version, @NotNull final Schema schema) {
        Preconditions.checkNotNull(version, "version must not be null");
        Preconditions.checkNotNull(schema, "schema must not be null");
        Preconditions.checkState(!this.frozen, "Registry is frozen and cannot be modified");
        Preconditions.checkArgument(
                version.equals(schema.version()),
                "Version mismatch: key %s does not match schema.version() %s",
                version,
                schema.version()
        );
        this.schemas.put(version, schema);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation uses floor semantics: it returns the schema for the
     * greatest version less than or equal to the requested version. This allows sparse
     * schema registration where not every version needs an explicit schema entry.</p>
     *
     * @param version the version to look up; must not be {@code null}
     * @return the schema for the greatest version &le; {@code version}, or {@code null} if no
     *         such schema exists
     * @throws NullPointerException if {@code version} is {@code null}
     */
    @Override
    @Nullable
    public Schema get(@NotNull final DataVersion version) {
        Preconditions.checkNotNull(version, "version must not be null");

        final Map.Entry<DataVersion, Schema> entry = this.schemas.floorEntry(version);
        return entry == null ? null : entry.getValue();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #get(DataVersion)} and throws an {@link IllegalStateException}
     * if no schema is found.</p>
     *
     * @param version the version to look up; must not be {@code null}
     * @return the schema for the given version; never {@code null}
     * @throws NullPointerException  if {@code version} is {@code null}
     * @throws IllegalStateException if no schema is found for the given version
     */
    @Override
    @NotNull
    public Schema require(@NotNull final DataVersion version) {
        Preconditions.checkNotNull(version, "version must not be null");

        final Schema schema = this.get(version);
        if (schema == null) {
            throw new IllegalStateException("No schema found for version: " + version);
        }
        return schema;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@code true} if no schemas are registered; {@code false} otherwise
     */
    @Override
    public boolean isEmpty() {
        return this.schemas.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the schema with the highest {@link DataVersion} key in the backing
     * {@link TreeMap}.</p>
     *
     * @return the latest schema; never {@code null}
     * @throws IllegalStateException if the registry is empty
     */
    @Override
    @NotNull
    public Schema latest() {
        Preconditions.checkState(!this.schemas.isEmpty(), "No schemas registered");
        return this.schemas.lastEntry().getValue();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation is {@code synchronized} and idempotent. On the first call,
     * the mutable {@link TreeMap} is wrapped in an unmodifiable navigable map via
     * {@link Collections#unmodifiableNavigableMap(NavigableMap)}, and the {@link #frozen} flag
     * is set to {@code true}. Subsequent calls have no effect.</p>
     */
    @Override
    public synchronized void freeze() {
        if (!this.frozen) {
            this.schemas = Collections.unmodifiableNavigableMap(new TreeMap<>(this.schemas));
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

    /**
     * {@inheritDoc}
     *
     * <p>The returned stream yields schemas in ascending {@link DataVersion} order,
     * reflecting the natural ordering of the backing {@link TreeMap}.</p>
     *
     * @return a stream of all schemas in version order; never {@code null}
     */
    @Override
    @NotNull
    public Stream<Schema> stream() {
        return this.schemas.values().stream();
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the registry is frozen, the backing map's key set is returned directly
     * (already unmodifiable). Otherwise, a defensive copy is returned.</p>
     *
     * @return an unmodifiable set of all registered versions; never {@code null}
     */
    @Override
    @NotNull
    public Set<DataVersion> versions() {
        return this.frozen ? this.schemas.keySet() : Set.copyOf(this.schemas.keySet());
    }
}
