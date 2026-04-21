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
 * <p>A {@code Schema} pairs a {@link DataVersion} with a {@link TypeRegistry}
 * containing all {@link Type} definitions valid at that version. Schemas tell
 * the data fixing system what the shape of the data is at each point in time.</p>
 *
 * <h2>Two Ways to Build a Schema</h2>
 * <p>Applications can either construct a schema directly from a pre-populated
 * registry, or subclass {@code Schema} to inherit and extend a parent schema:</p>
 *
 * <h3>Direct Construction</h3>
 * <pre>{@code
 * TypeRegistry types = new SimpleTypeRegistry();
 * types.register(new SimpleType<>(TypeReferences.PLAYER, playerCodec));
 * Schema schema = new Schema(new DataVersion(100), types);
 * }</pre>
 *
 * <h3>Inheritance via Subclassing</h3>
 * <p>Subclassing is the preferred approach for incremental schema evolution: a
 * child schema inherits all types from its parent and only registers the types
 * that changed. Override {@link #createTypeRegistry()} to choose a concrete
 * registry implementation (typically {@code SimpleTypeRegistry} from the core
 * module) and {@link #registerTypes()} to add types using either the
 * {@link #registerType(Type)} overload (for pre-built {@link Type} instances)
 * or the DSL-aware {@link #registerType(TypeReference, TypeTemplate)} overload:</p>
 * <pre>{@code
 * public class Schema100 extends Schema {
 *     public Schema100() { super(100, null); } // first version, no parent
 *
 *     @Override protected TypeRegistry createTypeRegistry() {
 *         return new SimpleTypeRegistry();
 *     }
 *
 *     @Override protected void registerTypes() {
 *         registerType(TypeReferences.PLAYER, DSL.and(
 *             DSL.field("name",   DSL.string()),
 *             DSL.field("health", DSL.intType()),
 *             DSL.remainder()));
 *     }
 * }
 *
 * public class Schema110 extends Schema {
 *     public Schema110(Schema parent) { super(110, parent); }
 *
 *     @Override protected TypeRegistry createTypeRegistry() {
 *         return new SimpleTypeRegistry();
 *     }
 *
 *     @Override protected void registerTypes() {
 *         // Only re-register PLAYER; other types inherit from Schema100
 *         registerType(TypeReferences.PLAYER, updatedPlayerTemplate());
 *     }
 * }
 * }</pre>
 *
 * <h2>Versioning Convention (SemVer-encoded)</h2>
 * <p>Any monotonic integer scheme works. The examples and tests in this
 * project use a SemVer-like encoding for readability:</p>
 * <ul>
 *   <li>{@code 100} = version 1.0.0</li>
 *   <li>{@code 110} = version 1.1.0</li>
 *   <li>{@code 200} = version 2.0.0</li>
 * </ul>
 *
 * <h2>Type Lookup</h2>
 * <pre>{@code
 * Type<?> playerType = schema.require(TypeReferences.PLAYER);
 * }</pre>
 *
 * <h2>Lazy Initialisation</h2>
 * <p>Subclass-built schemas initialise their type registry lazily on the
 * first call to {@link #types()}. The initialisation is thread-safe; concurrent
 * callers observe a fully populated registry.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Once initialised, a {@code Schema} is effectively immutable and safe to
 * share between threads, provided the underlying {@link TypeRegistry} is
 * itself thread-safe (the stock {@code SimpleTypeRegistry} is).</p>
 *
 * @author Erik Pförtner
 * @see DataVersion
 * @see TypeRegistry
 * @see SchemaRegistry
 * @see de.splatgames.aether.datafixers.api.dsl.DSL
 * @since 0.1.0
 */
public class Schema {
    private final DataVersion version;
    private final Schema parent;
    private volatile TypeRegistry types;
    private TypeRegistry buildingTypes;

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
        TypeRegistry result = this.types;
        if (result == null) {
            synchronized (this) {
                result = this.types;
                if (result == null) {
                    result = this.buildTypes();
                    this.types = result;
                }
            }
        }
        return result;
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
        this.buildingTypes = registry;

        // Inherit types from parent if present
        if (this.parent != null) {
            final TypeRegistry parentTypes = this.parent.types();
            for (final TypeReference ref : parentTypes.references()) {
                final Type<?> parentType = parentTypes.get(ref);
                if (parentType != null) {
                    registry.register(parentType);
                }
            }
        }

        // Let subclass register types
        this.registerTypes();
        this.buildingTypes = null;

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
        final TypeRegistry registry = this.buildingTypes;
        Preconditions.checkState(registry != null, "Cannot register types outside of registerTypes()");
        registry.register(type);
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
        final TypeRegistry registry = this.buildingTypes;
        Preconditions.checkState(registry != null, "Cannot register types outside of registerTypes()");

        // Apply the template with an empty family to get the concrete type
        final Type<?> templateType = template.apply(TypeFamily.empty());

        // Wrap the template type with the reference
        registry.register(new TemplateBasedType<>(reference, templateType));
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
     * a {@link TypeReference} with a type that was generated from a {@link TypeTemplate}. It implements the decorator
     * pattern, delegating codec operations to the underlying DSL-generated type while providing the reference
     * information.</p>
     *
     * <h2>Purpose</h2>
     * <p>When types are registered in a schema using templates, the resulting type
     * needs both a reference (for lookup) and a codec (for serialization). This class provides that association by
     * wrapping any {@link Type} with its reference.</p>
     *
     * <h2>Thread Safety</h2>
     * <p>This class is immutable and thread-safe.</p>
     *
     * @param <A> the value type that this type can encode/decode
     * @since 0.1.0
     */
    private static final class TemplateBasedType<A> implements Type<A> {

        /**
         * The type reference identifying this type in the registry.
         */
        private final TypeReference reference;

        /**
         * The underlying type providing codec functionality.
         */
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
