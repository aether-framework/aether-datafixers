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

package de.splatgames.aether.datafixers.core.fix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SchemaDataFix}.
 */
@DisplayName("SchemaDataFix")
class SchemaDataFixTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final DataVersion VERSION_1 = new DataVersion(1);
    private static final DataVersion VERSION_2 = new DataVersion(2);

    private SimpleSchemaRegistry schemaRegistry;

    @BeforeEach
    void setUp() {
        schemaRegistry = new SimpleSchemaRegistry();

        SimpleTypeRegistry types1 = new SimpleTypeRegistry();
        types1.register(Type.named(PLAYER.getId(), Type.PASSTHROUGH));
        Schema schemaV1 = new Schema(VERSION_1, types1);
        schemaRegistry.register(schemaV1);

        SimpleTypeRegistry types2 = new SimpleTypeRegistry();
        types2.register(Type.named(PLAYER.getId(), Type.PASSTHROUGH));
        Schema schemaV2 = new Schema(VERSION_2, types2);
        schemaRegistry.register(schemaV2);

        schemaRegistry.freeze();
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("creates fix with valid arguments")
        void createsFixWithValidArguments() {
            SchemaDataFix fix = new TestSchemaDataFix("test_fix", VERSION_1, VERSION_2, schemaRegistry);

            assertThat(fix.name()).isEqualTo("test_fix");
            assertThat(fix.fromVersion()).isEqualTo(VERSION_1);
            assertThat(fix.toVersion()).isEqualTo(VERSION_2);
        }

        @Test
        @DisplayName("rejects null name")
        void rejectsNullName() {
            assertThatThrownBy(() -> new TestSchemaDataFix(null, VERSION_1, VERSION_2, schemaRegistry))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("rejects null from version")
        void rejectsNullFromVersion() {
            assertThatThrownBy(() -> new TestSchemaDataFix("test", null, VERSION_2, schemaRegistry))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("from");
        }

        @Test
        @DisplayName("rejects null to version")
        void rejectsNullToVersion() {
            assertThatThrownBy(() -> new TestSchemaDataFix("test", VERSION_1, null, schemaRegistry))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("to");
        }

        @Test
        @DisplayName("rejects null schema registry")
        void rejectsNullSchemaRegistry() {
            assertThatThrownBy(() -> new TestSchemaDataFix("test", VERSION_1, VERSION_2, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("schemas");
        }
    }

    @Nested
    @DisplayName("Accessor methods")
    class AccessorMethods {

        @Test
        @DisplayName("name() returns configured name")
        void nameReturnsConfiguredName() {
            SchemaDataFix fix = new TestSchemaDataFix("my_fix", VERSION_1, VERSION_2, schemaRegistry);
            assertThat(fix.name()).isEqualTo("my_fix");
        }

        @Test
        @DisplayName("fromVersion() returns configured from version")
        void fromVersionReturnsConfiguredFromVersion() {
            SchemaDataFix fix = new TestSchemaDataFix("test", VERSION_1, VERSION_2, schemaRegistry);
            assertThat(fix.fromVersion()).isEqualTo(VERSION_1);
        }

        @Test
        @DisplayName("toVersion() returns configured to version")
        void toVersionReturnsConfiguredToVersion() {
            SchemaDataFix fix = new TestSchemaDataFix("test", VERSION_1, VERSION_2, schemaRegistry);
            assertThat(fix.toVersion()).isEqualTo(VERSION_2);
        }
    }

    @Nested
    @DisplayName("apply()")
    class Apply {

        @Test
        @DisplayName("applies rule to input data")
        void appliesRuleToInputData() {
            SchemaDataFix fix = new TestSchemaDataFix("test_fix", VERSION_1, VERSION_2, schemaRegistry);

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("name", "Alice");
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            @SuppressWarnings("unchecked")
            Dynamic<Object> objInput = (Dynamic<Object>) (Dynamic<?>) input;

            Dynamic<?> result = fix.apply(PLAYER, objInput, SimpleSystemDataFixerContext.INSTANCE);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("rejects null type")
        void rejectsNullType() {
            SchemaDataFix fix = new TestSchemaDataFix("test_fix", VERSION_1, VERSION_2, schemaRegistry);
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, new JsonObject());

            @SuppressWarnings("unchecked")
            Dynamic<Object> objInput = (Dynamic<Object>) (Dynamic<?>) input;

            assertThatThrownBy(() -> fix.apply(null, objInput, SimpleSystemDataFixerContext.INSTANCE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("type");
        }

        @Test
        @DisplayName("rejects null input")
        void rejectsNullInput() {
            SchemaDataFix fix = new TestSchemaDataFix("test_fix", VERSION_1, VERSION_2, schemaRegistry);

            assertThatThrownBy(() -> fix.apply(PLAYER, null, SimpleSystemDataFixerContext.INSTANCE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("input");
        }

        @Test
        @DisplayName("rejects null context")
        void rejectsNullContext() {
            SchemaDataFix fix = new TestSchemaDataFix("test_fix", VERSION_1, VERSION_2, schemaRegistry);
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, new JsonObject());

            @SuppressWarnings("unchecked")
            Dynamic<Object> objInput = (Dynamic<Object>) (Dynamic<?>) input;

            assertThatThrownBy(() -> fix.apply(PLAYER, objInput, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context");
        }
    }

    private static class TestSchemaDataFix extends SchemaDataFix {

        protected TestSchemaDataFix(
                String name,
                DataVersion from,
                DataVersion to,
                SimpleSchemaRegistry schemas
        ) {
            super(name, from, to, schemas);
        }

        @Override
        protected @NotNull TypeRewriteRule makeRule(
                @NotNull Schema inputSchema,
                @NotNull Schema outputSchema
        ) {
            return Rules.noop();
        }
    }
}
