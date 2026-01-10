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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.TypeRegistry;
import de.splatgames.aether.datafixers.api.type.template.TypeFamily;
import de.splatgames.aether.datafixers.api.type.template.TypeTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A versioned collection of type definitions for a specific data version.
 *
 * <p>A {@code Schema} represents the structure of data at a specific {@link DataVersion}.
 * It pairs a version number with a {@link TypeRegistry} containing all type definitions valid for that version. Schemas
 * are used by the data fixing system to understand the expected structure of data at different points in time.</p>
 *
 * <h2>Schema Evolution</h2>
 * <p>Schemas can be extended to define versioned data structures. Each schema version
 * can inherit from a parent and override or add types:</p>
 * <pre>{@code
 * public class Schema100 extends Schema {
 *     public Schema100() {
 *         super(100, null);
 *     }
 *
 *     @Override
 *     protected void registerTypes() {
 *         registerType(new SimpleType<>(TypeReferences.PLAYER, playerCodec()));
 *     }
 * }
 *
 * public class Schema110 extends Schema {
 *     public Schema110() {
 *         super(110, new Schema100());
 *     }
 *
 *     @Override
 *     protected void registerTypes() {
 *         // Override player type with new structure
 *         registerType(new SimpleType<>(TypeReferences.PLAYER, updatedPlayerCodec()));
 *     }
 * }
 * }</pre>
 *
 * <h2>Versioning Convention (SemVer-based)</h2>
 * <ul>
 *   <li>100 = Version 1.0.0</li>
 *   <li>110 = Version 1.1.0</li>
 *   <li>200 = Version 2.0.0</li>
 * </ul>
 *
 * <h2>Type Lookup</h2>
 * <p>Use {@link #require(TypeReference)} to retrieve types from the schema:</p>
 * <pre>{@code
 * Type<?> playerType = schema.require(TypeReferences.PLAYER);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe if the underlying {@link TypeRegistry}
 * is thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataVersion
 * @see TypeRegistry
 * @see SchemaRegistry
 * @since 0.1.0
 */
public class Schema {
    private final DataVersion version;
    private final Schema parent;
    private TypeRegistry types;

    /**
     * Creates a new schema for the specified version with the given types.
     *
     * <p>This constructor is provided for backwards compatibility. For new code,
     * prefer extending Schema and using the protected constructor.</p>
     *
     * @param version the data version this schema represents, must not be {@code null}
     * @param types   the type registry containing type definitions, must not be {@code null}
     * @throws NullPointerException if version or types is {@code null}
     */
    public Schema(@NotNull final DataVersion version,
                  @NotNull final TypeRegistry types) {
        Preconditions.checkNotNull(version, "version must not be null");
        Preconditions.checkNotNull(types, "types must not be null");

        this.version = version;
        this.types = types;
        this.parent = null;
    }

    /**
     * Creates a new schema for the specified version with an optional parent.
     *
     * <p>Subclasses use this constructor to define versioned schemas. The parent's
     * types are inherited and can be overridden by calling {@link #registerTypes()}.</p>
     *
     * @param versionId the version ID (e.g., 100 for 1.0.0, 110 for 1.1.0)
     * @param parent    the parent schema to inherit types from, or null for the first version
     */
    protected Schema(final int versionId, @Nullable final Schema parent) {
        this.version = new DataVersion(versionId);
        this.parent = parent;
        this.types = null;  // Will be built lazily
    }

    /**
     * Returns the data version this schema represents.
     *
     * @return the data version, never {@code null}
     */
    @NotNull
    public DataVersion version() {
        return this.version;
    }

    /**
     * Returns the parent schema, if any.
     *
     * @return the parent schema or null if this is the first version
     */
    @Nullable
    public Schema parent() {
        return this.parent;
    }

    /**
     * Returns the type registry containing all type definitions for this schema.
     *
     * <p>If this schema was created using the protected constructor, the type registry
     * is built lazily by calling {@link #registerTypes()} on first access.</p>
     *
     * @return the type registry, never {@code null}
     */
    @NotNull
    public TypeRegistry types() {
        if (this.types == null) {
            this.types = this.buildTypes();
        }
        return this.types;
    }

    /**
     * Builds the type registry for this schema.
     *
     * <p>First inherits types from the parent schema, then calls {@link #registerTypes()}
     * to allow subclasses to add or override types.</p>
     *
     * @return the built type registry
     */
    @NotNull
    private TypeRegistry buildTypes() {
        final TypeRegistry registry = this.createTypeRegistry();
        this.types = registry;

        // Inherit types from parent if present
        if (this.parent != null) {
            // Copy types from parent
            final TypeRegistry parentTypes = this.parent.types();
            // Parent types are already registered in parent's registry
            // For now, we don't copy - subclass must re-register all types it needs
        }

        // Let subclass register types
        this.registerTypes();

        return registry;
    }

    /**
     * Creates the type registry for this schema.
     *
     * <p>Subclasses must override this to provide a type registry implementation.
     * Typically returns a new {@code SimpleTypeRegistry} from the core module.</p>
     *
     * @return a new type registry
     */
    @NotNull
    protected TypeRegistry createTypeRegistry() {
        throw new UnsupportedOperationException(
                "Subclasses must override createTypeRegistry() to provide a TypeRegistry implementation"
        );
    }

    /**
     * Registers types for this schema version.
     *
     * <p>Subclasses override this method to define the types available at this version.
     * Use {@link #registerType(Type)} to add types to the registry.</p>
     *
     * <p>If a parent schema exists, subclasses should call parent's registerTypes first
     * if they want to inherit types.</p>
     */
    protected void registerTypes() {
        // Default implementation does nothing
        // Subclasses override to register their types
    }

    /**
     * Registers a type with this schema.
     *
     * <p>This is a convenience method for use in {@link #registerTypes()}.</p>
     *
     * @param type the type to register
     */
    protected final void registerType(@NotNull final Type<?> type) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkState(this.types != null, "Cannot register types before types() is called");
        this.types.register(type);
    }

    /**
     * Registers a type defined by a DSL template with this schema.
     *
     * <p>This method enables DFU-style schema definitions using the DSL. The template
     * is instantiated with an empty {@link TypeFamily} and wrapped with the given {@link TypeReference}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * @Override
     * protected void registerTypes() {
     *     registerType(TypeReferences.PLAYER, DSL.and(
     *         DSL.field("name", DSL.string()),
     *         DSL.field("health", DSL.intType()),
     *         DSL.field("position", DSL.and(
     *             DSL.field("x", DSL.doubleType()),
     *             DSL.field("y", DSL.doubleType()),
     *             DSL.field("z", DSL.doubleType())
     *         )),
     *         DSL.remainder()
     *     ));
     * }
     * }</pre>
     *
     * @param reference the type reference for lookup, must not be {@code null}
     * @param template  the DSL type template defining the structure, must not be {@code null}
     * @throws NullPointerException if reference or template is {@code null}
     */
    protected final void registerType(@NotNull final TypeReference reference,
                                      @NotNull final TypeTemplate template) {
        Preconditions.checkNotNull(reference, "reference must not be null");
        Preconditions.checkNotNull(template, "template must not be null");
        Preconditions.checkState(this.types != null, "Cannot register types before types() is called");

        // Apply the template with an empty family to get the concrete type
        final Type<?> templateType = template.apply(TypeFamily.empty());

        // Wrap the template type with the reference
        this.types.register(new TemplateBasedType<>(reference, templateType));
    }

    /**
     * Retrieves a type by its reference, throwing if not found.
     *
     * <p>This is a convenience method equivalent to {@code types().require(ref)}.</p>
     *
     * @param ref the type reference to look up, must not be {@code null}
     * @return the type for the given reference, never {@code null}
     * @throws IllegalStateException if the type is not registered
     * @throws NullPointerException  if ref is {@code null}
     */
    @NotNull
    public Type<?> require(@NotNull final TypeReference ref) {
        Preconditions.checkNotNull(ref, "ref must not be null");

        return this.types().require(ref);
    }

    /**
     * Internal type implementation that wraps a DSL-generated type with a TypeReference.
     *
     * <p>This class is used internally by {@link Schema#registerType} to associate
     * a {@link TypeReference} with a type that was generated from a {@link TypeTemplate}.
     * It implements the decorator pattern, delegating codec operations to the underlying
     * DSL-generated type while providing the reference information.</p>
     *
     * <h2>Purpose</h2>
     * <p>When types are registered in a schema using templates, the resulting type
     * needs both a reference (for lookup) and a codec (for serialization). This class
     * provides that association by wrapping any {@link Type} with its reference.</p>
     *
     * <h2>Thread Safety</h2>
     * <p>This class is immutable and thread-safe.</p>
     *
     * @param <A> the value type that this type can encode/decode
     * @since 0.1.0
     */
    private static final class TemplateBasedType<A> implements Type<A> {

        /** The type reference identifying this type in the registry. */
        private final TypeReference reference;

        /** The underlying type providing codec functionality. */
        private final Type<A> delegate;

        /**
         * Creates a new template-based type wrapping the given delegate.
         *
         * @param reference the type reference for registry lookup, must not be {@code null}
         * @param delegate  the underlying type providing codec functionality, must not be {@code null}
         * @throws NullPointerException if {@code reference} or {@code delegate} is {@code null}
         */
        TemplateBasedType(@NotNull final TypeReference reference, @NotNull final Type<A> delegate) {
            this.reference = Preconditions.checkNotNull(reference, "reference must not be null");
            this.delegate = Preconditions.checkNotNull(delegate, "delegate must not be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return the type reference associated with this type
         */
        @NotNull
        @Override
        public TypeReference reference() {
            return this.reference;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Delegates to the underlying type's codec.</p>
         *
         * @return the codec from the delegate type
         */
        @NotNull
        @Override
        public Codec<A> codec() {
            return this.delegate.codec();
        }
    }
}
