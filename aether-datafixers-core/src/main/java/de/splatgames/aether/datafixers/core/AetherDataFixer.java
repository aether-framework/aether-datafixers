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
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import org.jetbrains.annotations.NotNull;

/**
 * High-level facade for the Aether Datafixers system.
 *
 * <p>{@code AetherDataFixer} provides a unified, object-oriented interface
 * for encoding, decoding, and migrating data across versions. It wraps a
 * {@link SchemaRegistry} for type definitions and a {@link DataFixer} for
 * version migrations, and offers conveniences for working with
 * {@link TaggedDynamic} (type-annotated data) instead of raw
 * {@link de.splatgames.aether.datafixers.api.dynamic.Dynamic}.</p>
 *
 * <h2>Core Operations</h2>
 * <ul>
 *   <li>{@link #encode(DataVersion, TypeReference, Object, DynamicOps) encode} —
 *       Serialize a Java object to a tagged dynamic format via the schema
 *       codec.</li>
 *   <li>{@link #decode(DataVersion, TaggedDynamic) decode} — Deserialize a
 *       tagged dynamic to a Java object via the schema codec.</li>
 *   <li>{@link #update(TaggedDynamic, DataVersion, DataVersion) update} — Run
 *       the fix chain to migrate data from one version to another, preserving
 *       the {@link TypeReference} tag.</li>
 *   <li>{@link #update(TaggedDynamic, DataVersion, DataVersion, DataFixerContext) update}
 *       — Same as above but with an explicit
 *       {@link DataFixerContext}; pass a
 *       {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}
 *       to collect a
 *       {@link de.splatgames.aether.datafixers.api.diagnostic.MigrationReport}.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build the fixer from a bootstrap
 * AetherDataFixer fixer = new DataFixerRuntimeFactory()
 *     .create(new DataVersion(5), myBootstrap);
 *
 * // Encode a Java object into a tagged JSON representation
 * TaggedDynamic encoded = fixer.encode(
 *     fixer.currentVersion(),
 *     TypeReferences.PLAYER,
 *     player,
 *     GsonOps.INSTANCE);
 *
 * // Migrate legacy data up to the current version
 * TaggedDynamic updated = fixer.update(
 *     oldData,
 *     new DataVersion(1),
 *     fixer.currentVersion());
 *
 * // Decode back into a Java object using the current schema
 * Player loaded = fixer.decode(fixer.currentVersion(), updated);
 * }</pre>
 *
 * <h2>When to Use What</h2>
 * <p>If you already have a plain {@link de.splatgames.aether.datafixers.api.dynamic.Dynamic},
 * call the underlying {@link DataFixer#update(TypeReference,
 * de.splatgames.aether.datafixers.api.dynamic.Dynamic, DataVersion, DataVersion)
 * DataFixer.update} directly. Use the {@code TaggedDynamic}-based methods here
 * when you want the type tag to flow through the pipeline together with the
 * data.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe provided the underlying registries and
 * {@link DataFixer} are thread-safe (the stock implementations are).</p>
 *
 * @author Erik Pförtner
 * @see DataFixer
 * @see SchemaRegistry
 * @see DataFixerRuntimeFactory
 * @see de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext
 * @since 0.1.0
 */
public final class AetherDataFixer {
    /**
     * The current (latest) data version. This is used as the default target version for encoding and can be used to
     * determine the latest schema for decoding. It should be set to the highest version number that has a registered
     * schema in the SchemaRegistry.
     */
    private final DataVersion currentVersion;
    /**
     * The schema registry containing all schemas and type definitions.
     */
    private final SchemaRegistry schemaRegistry;
    /**
     * The underlying data fixer responsible for applying registered fixes during updates.
     */
    private final DataFixer dataFixer;

    /**
     * Creates a new Aether data fixer.
     *
     * @param currentVersion the current (latest) data version, must not be {@code null}
     * @param schemaRegistry the schema registry with type definitions, must not be {@code null}
     * @param dataFixer      the underlying data fixer for migrations, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public AetherDataFixer(@NotNull final DataVersion currentVersion,
                           @NotNull final SchemaRegistry schemaRegistry,
                           @NotNull final DataFixer dataFixer) {
        Preconditions.checkNotNull(currentVersion, "currentVersion must not be null");
        Preconditions.checkNotNull(schemaRegistry, "schemaRegistry must not be null");
        Preconditions.checkNotNull(dataFixer, "dataFixer must not be null");

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
    public <A, T> TaggedDynamic encode(@NotNull final DataVersion targetVersion,
                                       @NotNull final TypeReference typeRef,
                                       @NotNull final A value,
                                       @NotNull final DynamicOps<T> ops) {
        Preconditions.checkNotNull(targetVersion, "targetVersion must not be null");
        Preconditions.checkNotNull(typeRef, "typeRef must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkNotNull(ops, "ops must not be null");

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
    public TaggedDynamic update(@NotNull final TaggedDynamic input,
                                @NotNull final DataVersion fromVersion,
                                @NotNull final DataVersion toVersion) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        Preconditions.checkArgument(
                fromVersion.compareTo(toVersion) <= 0,
                "fromVersion (%s) must be <= toVersion (%s)", fromVersion, toVersion
        );

        @SuppressWarnings("unchecked") final Dynamic<Object> dyn = (Dynamic<Object>) input.value();

        final Dynamic<Object> updated =
                this.dataFixer.update(input.type(), dyn, fromVersion, toVersion);

        return new TaggedDynamic(input.type(), updated);
    }

    /**
     * Updates data from one version to another using the specified context.
     *
     * <p>Applies all registered fixes between the source and target versions
     * to migrate the data. The provided {@link DataFixerContext} controls logging and diagnostic behavior during
     * migration. Pass a {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext} to capture detailed
     * migration diagnostics including field-level operations.</p>
     *
     * @param input       the tagged dynamic data to update, must not be {@code null}
     * @param fromVersion the source version of the data, must not be {@code null}
     * @param toVersion   the target version to migrate to, must not be {@code null}
     * @param context     the fixer context for logging and diagnostics, must not be {@code null}
     * @return a new tagged dynamic with the updated data
     * @throws NullPointerException if any argument is {@code null}
     * @since 1.0.0
     */
    @NotNull
    public TaggedDynamic update(@NotNull final TaggedDynamic input,
                                @NotNull final DataVersion fromVersion,
                                @NotNull final DataVersion toVersion,
                                @NotNull final DataFixerContext context) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        Preconditions.checkNotNull(context, "context must not be null");
        Preconditions.checkArgument(
                fromVersion.compareTo(toVersion) <= 0,
                "fromVersion (%s) must be <= toVersion (%s)", fromVersion, toVersion
        );

        @SuppressWarnings("unchecked") final Dynamic<Object> dyn = (Dynamic<Object>) input.value();

        final Dynamic<Object> updated =
                this.dataFixer.update(input.type(), dyn, fromVersion, toVersion, context);

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
    public <A> A decode(@NotNull final DataVersion sourceVersion,
                        @NotNull final TaggedDynamic input) {
        Preconditions.checkNotNull(sourceVersion, "sourceVersion must not be null");
        Preconditions.checkNotNull(input, "input must not be null");

        final Schema schema = this.schemaRegistry.require(sourceVersion);

        @SuppressWarnings("unchecked") final Type<A> type = (Type<A>) schema.require(input.type());

        @SuppressWarnings("unchecked") final Dynamic<Object> dyn = (Dynamic<Object>) input.value();

        return type.codec().parse(dyn)
                .getOrThrow(msg -> new DecodeException("Failed to decode: " + msg, input.type()));
    }
}
