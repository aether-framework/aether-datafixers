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
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.TypeRegistry;
import de.splatgames.aether.datafixers.api.type.template.TypeTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Schema}.
 */
@DisplayName("Schema")
class SchemaTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference ENTITY = new TypeReference("entity");
    private static final TypeReference WORLD = new TypeReference("world");

    @Nested
    @DisplayName("Constructor with TypeRegistry")
    class ConstructorWithTypeRegistry {

        @Test
        @DisplayName("creates schema with version and types")
        void createsSchemaWithVersionAndTypes() {
            final DataVersion version = new DataVersion(100);
            final TypeRegistry types = new SimpleTypeRegistry();
            types.register(Type.STRING);

            final Schema schema = new Schema(version, types);

            assertThat(schema.version()).isEqualTo(version);
            assertThat(schema.types()).isSameAs(types);
            assertThat(schema.parent()).isNull();
        }

        @Test
        @DisplayName("throws on null version")
        void throwsOnNullVersion() {
            assertThatThrownBy(() -> new Schema(null, new SimpleTypeRegistry()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null types")
        void throwsOnNullTypes() {
            assertThatThrownBy(() -> new Schema(new DataVersion(100), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Subclass Constructor")
    class SubclassConstructor {

        @Test
        @DisplayName("creates schema with version id")
        void createsSchemaWithVersionId() {
            final TestSchema schema = new TestSchema(100, null);

            assertThat(schema.version()).isEqualTo(new DataVersion(100));
            assertThat(schema.parent()).isNull();
        }

        @Test
        @DisplayName("creates schema with parent")
        void createsSchemaWithParent() {
            final TestSchema parent = new TestSchema(100, null);
            final TestSchema child = new TestSchema(110, parent);

            assertThat(child.version()).isEqualTo(new DataVersion(110));
            assertThat(child.parent()).isSameAs(parent);
        }
    }

    @Nested
    @DisplayName("version()")
    class VersionMethod {

        @Test
        @DisplayName("returns the data version")
        void returnsTheDataVersion() {
            final Schema schema = new Schema(new DataVersion(200), new SimpleTypeRegistry());

            assertThat(schema.version().getVersion()).isEqualTo(200);
        }

        @Test
        @DisplayName("returns correct version for different values")
        void returnsCorrectVersionForDifferentValues() {
            assertThat(new Schema(new DataVersion(100), new SimpleTypeRegistry()).version().getVersion()).isEqualTo(100);
            assertThat(new Schema(new DataVersion(110), new SimpleTypeRegistry()).version().getVersion()).isEqualTo(110);
            assertThat(new Schema(new DataVersion(200), new SimpleTypeRegistry()).version().getVersion()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("parent()")
    class ParentMethod {

        @Test
        @DisplayName("returns null for root schema")
        void returnsNullForRootSchema() {
            final TestSchema root = new TestSchema(100, null);

            assertThat(root.parent()).isNull();
        }

        @Test
        @DisplayName("returns parent schema")
        void returnsParentSchema() {
            final TestSchema root = new TestSchema(100, null);
            final TestSchema child = new TestSchema(110, root);
            final TestSchema grandchild = new TestSchema(120, child);

            assertThat(child.parent()).isSameAs(root);
            assertThat(grandchild.parent()).isSameAs(child);
        }
    }

    @Nested
    @DisplayName("types()")
    class TypesMethod {

        @Test
        @DisplayName("returns the type registry")
        void returnsTheTypeRegistry() {
            final TypeRegistry registry = new SimpleTypeRegistry();
            registry.register(Type.STRING);

            final Schema schema = new Schema(new DataVersion(100), registry);

            assertThat(schema.types()).isSameAs(registry);
        }

        @Test
        @DisplayName("builds types lazily for subclass")
        void buildsTypesLazilyForSubclass() {
            final TestSchema schema = new TestSchema(100, null);

            // First call triggers building
            final TypeRegistry types1 = schema.types();
            assertThat(types1).isNotNull();

            // Subsequent calls return same instance
            final TypeRegistry types2 = schema.types();
            assertThat(types2).isSameAs(types1);
        }
    }

    @Nested
    @DisplayName("require()")
    class RequireMethod {

        @Test
        @DisplayName("returns registered type")
        void returnsRegisteredType() {
            final TypeRegistry registry = new SimpleTypeRegistry();
            final Type<String> stringType = createType(PLAYER, Type.STRING.codec());
            registry.register(stringType);

            final Schema schema = new Schema(new DataVersion(100), registry);

            assertThat(schema.require(PLAYER)).isSameAs(stringType);
        }

        @Test
        @DisplayName("throws for unregistered type")
        void throwsForUnregisteredType() {
            final Schema schema = new Schema(new DataVersion(100), new SimpleTypeRegistry());

            assertThatThrownBy(() -> schema.require(PLAYER))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("throws on null reference")
        void throwsOnNullReference() {
            final Schema schema = new Schema(new DataVersion(100), new SimpleTypeRegistry());

            assertThatThrownBy(() -> schema.require(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("registerType() with Type")
    class RegisterTypeMethod {

        @Test
        @DisplayName("registers type in schema")
        void registersTypeInSchema() {
            final TypeRegisteringSchema schema = new TypeRegisteringSchema(100);

            final TypeRegistry types = schema.types();
            assertThat(types.has(PLAYER)).isTrue();
        }
    }

    @Nested
    @DisplayName("registerType() with TypeTemplate")
    class RegisterTypeWithTemplateMethod {

        @Test
        @DisplayName("registers template-based type")
        void registersTemplateBasedType() {
            final TemplateRegisteringSchema schema = new TemplateRegisteringSchema(100);

            final TypeRegistry types = schema.types();
            assertThat(types.has(ENTITY)).isTrue();
        }
    }

    @Nested
    @DisplayName("Schema Inheritance")
    class SchemaInheritance {

        @Test
        @DisplayName("child can access parent types")
        void childCanAccessParentTypes() {
            final TypeRegisteringSchema parent = new TypeRegisteringSchema(100);
            final ChildSchema child = new ChildSchema(110, parent);

            // Parent has PLAYER registered
            assertThat(parent.types().has(PLAYER)).isTrue();

            // Child defines its own types (WORLD)
            assertThat(child.types().has(WORLD)).isTrue();
        }

        @Test
        @DisplayName("parent chain traversal works")
        void parentChainTraversalWorks() {
            final TestSchema v100 = new TestSchema(100, null);
            final TestSchema v110 = new TestSchema(110, v100);
            final TestSchema v120 = new TestSchema(120, v110);
            final TestSchema v200 = new TestSchema(200, v120);

            assertThat(v200.parent()).isSameAs(v120);
            assertThat(v200.parent().parent()).isSameAs(v110);
            assertThat(v200.parent().parent().parent()).isSameAs(v100);
            assertThat(v200.parent().parent().parent().parent()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("version id 0 works")
        void versionId0Works() {
            final TestSchema schema = new TestSchema(0, null);

            assertThat(schema.version().getVersion()).isZero();
        }

        @Test
        @DisplayName("large version id works")
        void largeVersionIdWorks() {
            final TestSchema schema = new TestSchema(Integer.MAX_VALUE, null);

            assertThat(schema.version().getVersion()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("multiple types can be registered")
        void multipleTypesCanBeRegistered() {
            final MultiTypeSchema schema = new MultiTypeSchema(100);

            assertThat(schema.types().has(PLAYER)).isTrue();
            assertThat(schema.types().has(ENTITY)).isTrue();
            assertThat(schema.types().has(WORLD)).isTrue();
        }
    }

    // Test Helpers

    private static <A> Type<A> createType(TypeReference ref, Codec<A> codec) {
        return new Type<>() {
            @NotNull
            @Override
            public TypeReference reference() {
                return ref;
            }

            @NotNull
            @Override
            public Codec<A> codec() {
                return codec;
            }
        };
    }

    private static class SimpleTypeRegistry implements TypeRegistry {
        private final Map<TypeReference, Type<?>> types = new HashMap<>();

        @Override
        public void register(@NotNull Type<?> type) {
            types.put(type.reference(), type);
        }

        @Nullable
        @Override
        public Type<?> get(@NotNull TypeReference ref) {
            return types.get(ref);
        }

        @Override
        public boolean has(@NotNull TypeReference ref) {
            return types.containsKey(ref);
        }

        @NotNull
        @Override
        public java.util.Set<TypeReference> references() {
            return java.util.Set.copyOf(types.keySet());
        }
    }

    private static class TestSchema extends Schema {
        protected TestSchema(int versionId, @Nullable Schema parent) {
            super(versionId, parent);
        }

        @NotNull
        @Override
        protected TypeRegistry createTypeRegistry() {
            return new SimpleTypeRegistry();
        }
    }

    private static class TypeRegisteringSchema extends Schema {
        protected TypeRegisteringSchema(int versionId) {
            super(versionId, null);
        }

        @NotNull
        @Override
        protected TypeRegistry createTypeRegistry() {
            return new SimpleTypeRegistry();
        }

        @Override
        protected void registerTypes() {
            registerType(createType(PLAYER, Type.STRING.codec()));
        }
    }

    private static class TemplateRegisteringSchema extends Schema {
        protected TemplateRegisteringSchema(int versionId) {
            super(versionId, null);
        }

        @NotNull
        @Override
        protected TypeRegistry createTypeRegistry() {
            return new SimpleTypeRegistry();
        }

        @Override
        protected void registerTypes() {
            registerType(ENTITY, DSL.and(
                    DSL.field("id", DSL.intType()),
                    DSL.field("name", DSL.string())
            ));
        }
    }

    private static class ChildSchema extends Schema {
        protected ChildSchema(int versionId, @Nullable Schema parent) {
            super(versionId, parent);
        }

        @NotNull
        @Override
        protected TypeRegistry createTypeRegistry() {
            return new SimpleTypeRegistry();
        }

        @Override
        protected void registerTypes() {
            registerType(createType(WORLD, Type.STRING.codec()));
        }
    }

    private static class MultiTypeSchema extends Schema {
        protected MultiTypeSchema(int versionId) {
            super(versionId, null);
        }

        @NotNull
        @Override
        protected TypeRegistry createTypeRegistry() {
            return new SimpleTypeRegistry();
        }

        @Override
        protected void registerTypes() {
            registerType(createType(PLAYER, Type.STRING.codec()));
            registerType(createType(ENTITY, Type.INT.codec()));
            registerType(createType(WORLD, Type.BOOL.codec()));
        }
    }
}
