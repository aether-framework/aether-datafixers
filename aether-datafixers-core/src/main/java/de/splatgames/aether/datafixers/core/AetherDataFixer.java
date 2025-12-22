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

package de.splatgames.aether.datafixers.core;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.api.exception.DecodeException;
import de.splatgames.aether.datafixers.api.exception.EncodeException;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import org.jetbrains.annotations.NotNull;

/**
 * High-level facade for the Aether DataFixers system.
 *
 * <p>{@code AetherDataFixer} provides a unified interface for encoding, decoding,
 * and migrating data across versions. It combines a {@link SchemaRegistry} for
 * type definitions with a {@link DataFixer} for version migrations.</p>
 *
 * <h2>Core Operations</h2>
 * <ul>
 *   <li>{@link #encode} - Serialize a Java object to a tagged dynamic format</li>
 *   <li>{@link #decode} - Deserialize a tagged dynamic to a Java object</li>
 *   <li>{@link #update} - Migrate data between versions</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create the fixer using the factory
 * AetherDataFixer fixer = new DataFixerRuntimeFactory()
 *     .create(new DataVersion(5), myBootstrap);
 *
 * // Encode an object
 * TaggedDynamic encoded = fixer.encode(
 *     fixer.currentVersion(),
 *     TypeReferences.PLAYER,
 *     player,
 *     GsonOps.INSTANCE
 * );
 *
 * // Update old data to current version
 * TaggedDynamic updated = fixer.update(
 *     oldData,
 *     DataVersion.of(1),
 *     fixer.currentVersion()
 * );
 *
 * // Decode to Java object
 * Player loadedPlayer = fixer.decode(fixer.currentVersion(), updated);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe if the underlying registries and fixer are thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataFixer
 * @see SchemaRegistry
 * @see DataFixerRuntimeFactory
 * @since 0.1.0
 */
public final class AetherDataFixer {

    private final DataVersion currentVersion;
    private final SchemaRegistry schemaRegistry;
    private final DataFixer dataFixer;

    /**
     * Creates a new Aether data fixer.
     *
     * @param currentVersion the current (latest) data version, must not be {@code null}
     * @param schemaRegistry the schema registry with type definitions, must not be {@code null}
     * @param dataFixer      the underlying data fixer for migrations, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public AetherDataFixer(
            @NotNull final DataVersion currentVersion,
            @NotNull final SchemaRegistry schemaRegistry,
            @NotNull final DataFixer dataFixer
    ) {
        Preconditions.checkNotNull(currentVersion, "DataVersion currentVersion must not be null");
        Preconditions.checkNotNull(schemaRegistry, "SchemaRegistry schemaRegistry must not be null");
        Preconditions.checkNotNull(dataFixer, "DataFixer dataFixer must not be null");

        this.currentVersion = currentVersion;
        this.schemaRegistry = schemaRegistry;
        this.dataFixer = dataFixer;
    }

    /**
     * Returns the current (latest) data version.
     *
     * @return the current data version, never {@code null}
     */
    @NotNull
    public DataVersion currentVersion() {
        return this.currentVersion;
    }

    /**
     * Encodes a Java object to a tagged dynamic representation.
     *
     * <p>The value is serialized using the codec from the schema for the
     * specified version, then wrapped with its type reference for later decoding.</p>
     *
     * @param targetVersion the version whose schema to use for encoding, must not be {@code null}
     * @param typeRef       the type reference for the value, must not be {@code null}
     * @param value         the value to encode, must not be {@code null}
     * @param ops           the dynamic ops for the target format, must not be {@code null}
     * @param <A>           the Java type of the value
     * @param <T>           the dynamic representation type
     * @return a tagged dynamic containing the encoded value and type information
     * @throws EncodeException      if encoding fails
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public <A, T> TaggedDynamic encode(
            @NotNull final DataVersion targetVersion,
            @NotNull final TypeReference typeRef,
            @NotNull final A value,
            @NotNull final DynamicOps<T> ops
    ) {
        Preconditions.checkNotNull(targetVersion, "DataVersion targetVersion must not be null");
        Preconditions.checkNotNull(typeRef, "TypeReference typeRef must not be null");
        Preconditions.checkNotNull(value, "A value must not be null");
        Preconditions.checkNotNull(ops, "DynamicOps<T> ops must not be null");

        final Schema schema = this.schemaRegistry.require(targetVersion);

        @SuppressWarnings("unchecked") final Type<A> type = (Type<A>) schema.require(typeRef);

        final Dynamic<T> encoded = type.codec().encodeStartDynamic(ops, value)
                .getOrThrow(msg -> new EncodeException("Failed to encode: " + msg, typeRef, value));
        return new TaggedDynamic(typeRef, encoded);
    }

    /**
     * Updates data from one version to another.
     *
     * <p>Applies all registered fixes between the source and target versions
     * to migrate the data.</p>
     *
     * @param input       the tagged dynamic data to update, must not be {@code null}
     * @param fromVersion the source version of the data, must not be {@code null}
     * @param toVersion   the target version to migrate to, must not be {@code null}
     * @return a new tagged dynamic with the updated data
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public TaggedDynamic update(
            @NotNull final TaggedDynamic input,
            @NotNull final DataVersion fromVersion,
            @NotNull final DataVersion toVersion
    ) {
        Preconditions.checkNotNull(input, "TaggedDynamic input must not be null");
        Preconditions.checkNotNull(fromVersion, "DataVersion fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "DataVersion toVersion must not be null");

        @SuppressWarnings("unchecked") final Dynamic<Object> dyn = (Dynamic<Object>) input.value();

        final Dynamic<Object> updated =
                this.dataFixer.update(input.type(), dyn, fromVersion, toVersion);

        return new TaggedDynamic(input.type(), updated);
    }

    /**
     * Decodes a tagged dynamic to a Java object.
     *
     * <p>The value is deserialized using the codec from the schema for the
     * specified version.</p>
     *
     * @param sourceVersion the version whose schema to use for decoding, must not be {@code null}
     * @param input         the tagged dynamic to decode, must not be {@code null}
     * @param <A>           the expected Java type
     * @return the decoded Java object
     * @throws DecodeException      if decoding fails
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public <A> A decode(
            @NotNull final DataVersion sourceVersion,
            @NotNull final TaggedDynamic input
    ) {
        Preconditions.checkNotNull(sourceVersion, "DataVersion sourceVersion must not be null");
        Preconditions.checkNotNull(input, "TaggedDynamic input must not be null");

        final Schema schema = this.schemaRegistry.require(sourceVersion);

        @SuppressWarnings("unchecked") final Type<A> type = (Type<A>) schema.require(input.type());

        @SuppressWarnings("unchecked") final Dynamic<Object> dyn = (Dynamic<Object>) input.value();

        return type.codec().parse(dyn)
                .getOrThrow(msg -> new DecodeException("Failed to decode: " + msg, input.type()));
    }
}
