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
 * for testing purposes. These mocks are useful when you need schemas for testing
 * DataFix implementations but don't want to set up full production schemas.</p>
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

    private MockSchemas() {
        // Factory class
    }

    // ==================== Minimal Schema ====================

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

    // ==================== Schema Chain ====================

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

    // ==================== Schema Builder ====================

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

    // ==================== Minimal Schema Implementation ====================

    private static final class MinimalSchema extends Schema {

        MinimalSchema(final int version, @Nullable final Schema parent) {
            super(version, parent);
        }

        @NotNull
        @Override
        protected TypeRegistry createTypeRegistry() {
            return new SimpleTypeRegistry();
        }

        @Override
        protected void registerTypes() {
            // No types by default
        }
    }

    // ==================== Schema Builder ====================

    /**
     * A builder for creating custom mock schemas.
     */
    public static final class SchemaBuilder {

        private final int version;
        private Schema parent;
        private final SimpleTypeRegistry typeRegistry;

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

    private static final class BuiltSchema extends Schema {

        private final SimpleTypeRegistry builtRegistry;

        BuiltSchema(final int version, @Nullable final Schema parent, @NotNull final SimpleTypeRegistry typeRegistry) {
            super(version, parent);
            this.builtRegistry = Preconditions.checkNotNull(typeRegistry, "typeRegistry must not be null");
        }

        @NotNull
        @Override
        protected TypeRegistry createTypeRegistry() {
            return this.builtRegistry;
        }

        @Override
        protected void registerTypes() {
            // Types already in the built registry
        }
    }

    // ==================== Wrapped Type ====================

    /**
     * A type wrapper that associates a type with a specific reference.
     */
    @SuppressWarnings("rawtypes")
    private static final class WrappedType<A> implements Type<A> {

        private final TypeReference reference;
        private final Type delegate;

        WrappedType(@NotNull final TypeReference reference, @NotNull final Type<?> delegate) {
            this.reference = Preconditions.checkNotNull(reference, "reference must not be null");
            this.delegate = Preconditions.checkNotNull(delegate, "delegate must not be null");
        }

        @NotNull
        @Override
        public TypeReference reference() {
            return this.reference;
        }

        @SuppressWarnings("unchecked")
        @NotNull
        @Override
        public Codec<A> codec() {
            return this.delegate.codec();
        }
    }
}
