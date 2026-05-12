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

package de.splatgames.aether.datafixers.testkit.factory;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.TypeRegistry;
import de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Factory methods for creating mock {@link Schema} and {@link SchemaRegistry} instances.
 *
 * <p>{@code MockSchemas} provides utilities for creating lightweight schema objects
 * for testing purposes. These mocks are useful when you need schemas for testing DataFix implementations but don't want
 * to set up full production schemas.</p>
 *
 * <h2>Minimal Schema</h2>
 * <pre>{@code
 * // Create a minimal empty schema
 * Schema schema = MockSchemas.minimal(100);
 * }</pre>
 *
 * <h2>Schema Chain</h2>
 * <pre>{@code
 * // Create a registry with multiple schema versions
 * SchemaRegistry registry = MockSchemas.chain(
 *     MockSchemas.minimal(100),
 *     MockSchemas.minimal(110),
 *     MockSchemas.minimal(200)
 * );
 * }</pre>
 *
 * <h2>Custom Schema Builder</h2>
 * <pre>{@code
 * // Build a schema with specific types
 * Schema schema = MockSchemas.builder(100)
 *     .withType(TypeReferences.PLAYER, playerType)
 *     .withType(TypeReferences.WORLD, worldType)
 *     .build();
 * }</pre>
 *
 * @author Erik Pförtner
 * @see Schema
 * @see SchemaRegistry
 * @since 0.2.0
 */
public final class MockSchemas {

    /**
     * Private constructor to prevent instantiation.
     */
    private MockSchemas() {
        throw new UnsupportedOperationException("MockSchemas is a utility class and cannot be instantiated");
    }

    /**
     * Creates a minimal schema with no types.
     *
     * @param version the schema version
     * @return a new minimal schema
     */
    @NotNull
    public static Schema minimal(final int version) {
        return new MinimalSchema(version, null);
    }

    /**
     * Creates a minimal schema with no types.
     *
     * @param version the schema version
     * @return a new minimal schema
     */
    @NotNull
    public static Schema minimal(@NotNull final DataVersion version) {
        Preconditions.checkNotNull(version, "version must not be null");
        return new MinimalSchema(version.getVersion(), null);
    }

    /**
     * Creates a minimal schema that inherits from a parent.
     *
     * @param version the schema version
     * @param parent  the parent schema (may be null)
     * @return a new minimal schema
     */
    @NotNull
    public static Schema minimal(final int version, @Nullable final Schema parent) {
        return new MinimalSchema(version, parent);
    }

    /**
     * Creates a {@link SchemaRegistry} containing all provided schemas.
     *
     * @param schemas the schemas to register
     * @return a new SchemaRegistry
     * @throws NullPointerException if {@code schemas} is null
     */
    @NotNull
    public static SchemaRegistry chain(@NotNull final Schema... schemas) {
        Preconditions.checkNotNull(schemas, "schemas must not be null");

        final SimpleSchemaRegistry registry = new SimpleSchemaRegistry();
        for (final Schema schema : schemas) {
            registry.register(schema);
        }
        registry.freeze();
        return registry;
    }

    /**
     * Creates a {@link SchemaRegistry} with minimal schemas for the specified versions.
     *
     * @param versions the version numbers
     * @return a new SchemaRegistry
     * @throws NullPointerException if {@code versions} is null
     */
    @NotNull
    public static SchemaRegistry chainMinimal(final int... versions) {
        Preconditions.checkNotNull(versions, "versions must not be null");

        final Schema[] schemas = Arrays.stream(versions)
                .mapToObj(MockSchemas::minimal)
                .toArray(Schema[]::new);
        return chain(schemas);
    }

    /**
     * Creates a builder for constructing a custom schema.
     *
     * @param version the schema version
     * @return a new SchemaBuilder
     */
    @NotNull
    public static SchemaBuilder builder(final int version) {
        return new SchemaBuilder(version);
    }

    /**
     * Creates a builder for constructing a custom schema.
     *
     * @param version the schema version
     * @return a new SchemaBuilder
     */
    @NotNull
    public static SchemaBuilder builder(@NotNull final DataVersion version) {
        Preconditions.checkNotNull(version, "version must not be null");
        return new SchemaBuilder(version.getVersion());
    }

    /**
     * A minimal schema implementation with no types.
     *
     * <p>This schema serves as a simple placeholder for testing schema chains
     * without needing to define any actual types. It can be used when the presence of a schema version is required but
     * the specific types are not relevant.</p>
     */
    private static final class MinimalSchema extends Schema {

        /**
         * Creates a minimal schema with the specified version and parent.
         *
         * @param version the schema version
         * @param parent  the parent schema (may be null)
         */
        MinimalSchema(final int version, @Nullable final Schema parent) {
            super(version, parent);
        }

        /**
         * Creates an empty type registry for this schema.
         *
         * @return a new SimpleTypeRegistry
         */
        @NotNull
        @Override
        protected TypeRegistry createTypeRegistry() {
            return new SimpleTypeRegistry();
        }

        /**
         * No types to register for this minimal schema.
         */
        @Override
        protected void registerTypes() {
            // No types by default
        }
    }

    /**
     * A builder for creating custom mock schemas.
     *
     * <p><b>Note:</b> Parent schema types are <b>not</b> automatically inherited.
     * You must explicitly add all types needed for each schema version via {@link #withType}. Setting a parent with
     * {@link #withParent} only establishes the parent reference for schema chain traversal, not type inheritance.</p>
     */
    public static final class SchemaBuilder {

        /**
         * The schema version for the schema being built. This is required and immutable once the builder is created.
         */
        private final int version;
        /**
         * The type registry where types added via {@link #withType} are stored. This registry is used to build the
         * final schema.
         */
        @NotNull
        private final SimpleTypeRegistry typeRegistry;
        /**
         * The parent schema for the schema being built. This is optional and can be set via {@link #withParent}. Note
         * that parent schemas do not automatically provide their types to the child schema; you must explicitly add any
         * needed types.
         */
        @Nullable
        private Schema parent;

        /**
         * Creates a new SchemaBuilder with the specified version.
         *
         * @param version the schema version
         */
        SchemaBuilder(final int version) {
            this.version = version;
            this.typeRegistry = new SimpleTypeRegistry();
        }

        /**
         * Sets the parent schema.
         *
         * @param parent the parent schema
         * @return this builder for chaining
         */
        @NotNull
        public SchemaBuilder withParent(@Nullable final Schema parent) {
            this.parent = parent;
            return this;
        }

        /**
         * Adds a type to the schema.
         *
         * @param reference the type reference
         * @param type      the type
         * @return this builder for chaining
         */
        @NotNull
        public SchemaBuilder withType(@NotNull final TypeReference reference,
                                      @NotNull final Type<?> type) {
            Preconditions.checkNotNull(reference, "reference must not be null");
            Preconditions.checkNotNull(type, "type must not be null");
            // Wrap the type with the given reference
            this.typeRegistry.register(new WrappedType<>(reference, type));
            return this;
        }

        /**
         * Builds the schema.
         *
         * @return a new Schema
         */
        @NotNull
        public Schema build() {
            return new BuiltSchema(this.version, this.parent, this.typeRegistry);
        }
    }

    /**
     * A schema implementation that uses a pre-built type registry from the builder.
     *
     * <p>This schema is constructed by the {@link SchemaBuilder} and uses the types registered in the builder's
     * {@link SimpleTypeRegistry}. The builder allows you to add types with specific references, and this schema will
     * use those types directly without needing to override the {@link #registerTypes} method.</p>
     */
    private static final class BuiltSchema extends Schema {

        /**
         * The type registry built by the SchemaBuilder. This registry contains all types added via withType and is used
         * directly by this schema without modification.
         */
        private final SimpleTypeRegistry builtRegistry;

        /**
         * Creates a new BuiltSchema with the specified version, parent, and type registry.
         *
         * @param version      the schema version
         * @param parent       the parent schema (may be null)
         * @param typeRegistry the type registry built by the SchemaBuilder
         */
        BuiltSchema(final int version, @Nullable final Schema parent, @NotNull final SimpleTypeRegistry typeRegistry) {
            super(version, parent);
            this.builtRegistry = Preconditions.checkNotNull(typeRegistry, "typeRegistry must not be null");
        }

        /**
         * Returns the type registry built by the SchemaBuilder.
         *
         * @return the built type registry
         */
        @NotNull
        @Override
        protected TypeRegistry createTypeRegistry() {
            return this.builtRegistry;
        }

        /**
         * No additional types to register since the builder's registry is used directly.
         */
        @Override
        protected void registerTypes() {
            // Types already in the built registry
        }
    }

    /**
     * A type wrapper that associates a type with a specific reference.
     */
    @SuppressWarnings("rawtypes")
    private static final class WrappedType<A> implements Type<A> {

        /**
         * The reference associated with this type. This allows the type to be registered in the schema with the correct
         * reference, even if the underlying type does not inherently know its own reference.
         */
        private final TypeReference reference;
        /**
         * The underlying type that this wrapper delegates to. This is the actual type implementation that provides the
         * codec and other type behavior, while the wrapper simply associates it with a reference for registration
         * purposes.
         */
        private final Type delegate;

        /**
         * Creates a new WrappedType with the specified reference and delegate type.
         *
         * @param reference the type reference to associate with this type
         * @param delegate  the underlying type to delegate to
         */
        WrappedType(@NotNull final TypeReference reference, @NotNull final Type<?> delegate) {
            this.reference = Preconditions.checkNotNull(reference, "reference must not be null");
            this.delegate = Preconditions.checkNotNull(delegate, "delegate must not be null");
        }

        /**
         * Returns the reference associated with this type.
         *
         * @return the type reference
         */
        @NotNull
        @Override
        public TypeReference reference() {
            return this.reference;
        }

        /**
         * Delegates to the underlying type's codec.
         *
         * @return the codec from the delegate type
         */
        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public Codec<A> codec() {
            return this.delegate.codec();
        }
    }
}
