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
 * <p>This implementation is not thread-safe. For concurrent access, external
 * synchronization is required.</p>
 *
 * @author Erik Pförtner
 * @see SchemaRegistry
 * @see Schema
 * @since 0.1.0
 */
public final class SimpleSchemaRegistry implements SchemaRegistry {

    private NavigableMap<DataVersion, Schema> schemas = new TreeMap<>();
    private volatile boolean frozen = false;

    @Override
    public void register(@NotNull final Schema schema) {
        Preconditions.checkNotNull(schema, "schema must not be null");
        Preconditions.checkState(!this.frozen, "Registry is frozen and cannot be modified");
        this.schemas.put(schema.version(), schema);
    }

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

    @Override
    @Nullable
    public Schema get(@NotNull final DataVersion version) {
        Preconditions.checkNotNull(version, "version must not be null");

        final Map.Entry<DataVersion, Schema> entry = this.schemas.floorEntry(version);
        return entry == null ? null : entry.getValue();
    }

    @Override
    @NotNull
    public Schema require(@NotNull final DataVersion version) {
        Preconditions.checkNotNull(version, "version must not be null");

        final Schema schema = this.get(version);
        Preconditions.checkState(schema != null, "No schema found for version: %s", version);
        return schema;
    }

    @Override
    public boolean isEmpty() {
        return this.schemas.isEmpty();
    }

    @Override
    @NotNull
    public Schema latest() {
        Preconditions.checkState(!this.schemas.isEmpty(), "No schemas registered");
        return this.schemas.lastEntry().getValue();
    }

    @Override
    public void freeze() {
        if (!this.frozen) {
            this.schemas = Collections.unmodifiableNavigableMap(new TreeMap<>(this.schemas));
            this.frozen = true;
        }
    }

    @Override
    public boolean isFrozen() {
        return this.frozen;
    }

    @Override
    @NotNull
    public Stream<Schema> stream() {
        return this.schemas.values().stream();
    }

    @Override
    @NotNull
    public Set<DataVersion> versions() {
        return this.frozen ? this.schemas.keySet() : Set.copyOf(this.schemas.keySet());
    }
}
