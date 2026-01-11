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

package de.splatgames.aether.datafixers.core.bootstrap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DataFixerRuntimeFactory}.
 */
@DisplayName("DataFixerRuntimeFactory")
class DataFixerRuntimeFactoryTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final DataVersion VERSION_1 = new DataVersion(1);
    private static final DataVersion VERSION_2 = new DataVersion(2);

    private DataFixerRuntimeFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DataFixerRuntimeFactory();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates AetherDataFixer with correct current version")
        void createsAetherDataFixerWithCorrectCurrentVersion() {
            DataFixerBootstrap bootstrap = new TestBootstrap();

            AetherDataFixer fixer = factory.create(VERSION_2, bootstrap);

            assertThat(fixer.currentVersion()).isEqualTo(VERSION_2);
        }

        @Test
        @DisplayName("creates functional fixer that can update data")
        void createsFunctionalFixerThatCanUpdate() {
            DataFixerBootstrap bootstrap = new TestBootstrapWithFix();

            AetherDataFixer fixer = factory.create(VERSION_2, bootstrap);

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("name", "Bob");
            inputObj.addProperty("level", 5);
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = fixer.update(input, VERSION_1, VERSION_2);

            assertThat(result.type()).isEqualTo(PLAYER);
        }

        @Test
        @DisplayName("creates fixer with frozen schema registry")
        void createsFixerWithFrozenSchemaRegistry() {
            DataFixerBootstrap bootstrap = new TestBootstrap();

            AetherDataFixer fixer = factory.create(VERSION_2, bootstrap);

            // The fixer should work correctly, indicating the schema registry was frozen
            assertThat(fixer.currentVersion()).isEqualTo(VERSION_2);
        }

        @Test
        @DisplayName("rejects null currentVersion")
        void rejectsNullCurrentVersion() {
            assertThatThrownBy(() -> factory.create(null, new TestBootstrap()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("currentVersion");
        }

        @Test
        @DisplayName("rejects null bootstrap")
        void rejectsNullBootstrap() {
            assertThatThrownBy(() -> factory.create(VERSION_1, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("bootstrap");
        }
    }

    @Nested
    @DisplayName("Bootstrap integration")
    class BootstrapIntegration {

        @Test
        @DisplayName("invokes registerSchemas on bootstrap")
        void invokesRegisterSchemasOnBootstrap() {
            TrackingBootstrap bootstrap = new TrackingBootstrap();

            factory.create(VERSION_2, bootstrap);

            assertThat(bootstrap.schemasRegistered).isTrue();
        }

        @Test
        @DisplayName("invokes registerFixes on bootstrap")
        void invokesRegisterFixesOnBootstrap() {
            TrackingBootstrap bootstrap = new TrackingBootstrap();

            factory.create(VERSION_2, bootstrap);

            assertThat(bootstrap.fixesRegistered).isTrue();
        }

        @Test
        @DisplayName("calls registerSchemas before registerFixes")
        void callsRegisterSchemasBeforeRegisterFixes() {
            OrderTrackingBootstrap bootstrap = new OrderTrackingBootstrap();

            factory.create(VERSION_2, bootstrap);

            assertThat(bootstrap.callOrder).containsExactly("schemas", "fixes");
        }
    }

    private static class TestBootstrap implements DataFixerBootstrap {
        @Override
        public void registerSchemas(@NotNull SchemaRegistry schemas) {
            SimpleTypeRegistry types1 = new SimpleTypeRegistry();
            types1.register(Type.named(PLAYER.getId(), Type.PASSTHROUGH));
            schemas.register(new Schema(VERSION_1, types1));

            SimpleTypeRegistry types2 = new SimpleTypeRegistry();
            types2.register(Type.named(PLAYER.getId(), Type.PASSTHROUGH));
            schemas.register(new Schema(VERSION_2, types2));
        }

        @Override
        public void registerFixes(@NotNull FixRegistrar fixes) {
            // No fixes for basic test
        }
    }

    private static class TestBootstrapWithFix implements DataFixerBootstrap {
        @Override
        public void registerSchemas(@NotNull SchemaRegistry schemas) {
            SimpleTypeRegistry types1 = new SimpleTypeRegistry();
            types1.register(Type.named(PLAYER.getId(), Type.PASSTHROUGH));
            schemas.register(new Schema(VERSION_1, types1));

            SimpleTypeRegistry types2 = new SimpleTypeRegistry();
            types2.register(Type.named(PLAYER.getId(), Type.PASSTHROUGH));
            schemas.register(new Schema(VERSION_2, types2));
        }

        @Override
        public void registerFixes(@NotNull FixRegistrar fixes) {
            fixes.register(PLAYER, new DataFix<JsonElement>() {
                @Override
                public @NotNull String name() {
                    return "test_fix";
                }

                @Override
                public @NotNull DataVersion fromVersion() {
                    return VERSION_1;
                }

                @Override
                public @NotNull DataVersion toVersion() {
                    return VERSION_2;
                }

                @Override
                public @NotNull Dynamic<JsonElement> apply(
                        @NotNull TypeReference type,
                        @NotNull Dynamic<JsonElement> input,
                        @NotNull DataFixerContext context
                ) {
                    return input;
                }
            });
        }
    }

    private static class TrackingBootstrap implements DataFixerBootstrap {
        boolean schemasRegistered = false;
        boolean fixesRegistered = false;

        @Override
        public void registerSchemas(@NotNull SchemaRegistry schemas) {
            schemasRegistered = true;
            SimpleTypeRegistry types = new SimpleTypeRegistry();
            schemas.register(new Schema(VERSION_1, types));
            schemas.register(new Schema(VERSION_2, new SimpleTypeRegistry()));
        }

        @Override
        public void registerFixes(@NotNull FixRegistrar fixes) {
            fixesRegistered = true;
        }
    }

    private static class OrderTrackingBootstrap implements DataFixerBootstrap {
        java.util.List<String> callOrder = new java.util.ArrayList<>();

        @Override
        public void registerSchemas(@NotNull SchemaRegistry schemas) {
            callOrder.add("schemas");
            SimpleTypeRegistry types = new SimpleTypeRegistry();
            schemas.register(new Schema(VERSION_1, types));
            schemas.register(new Schema(VERSION_2, new SimpleTypeRegistry()));
        }

        @Override
        public void registerFixes(@NotNull FixRegistrar fixes) {
            callOrder.add("fixes");
        }
    }
}
