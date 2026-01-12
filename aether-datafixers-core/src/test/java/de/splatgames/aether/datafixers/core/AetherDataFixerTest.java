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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.TaggedDynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
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
 * Unit tests for {@link AetherDataFixer}.
 */
@DisplayName("AetherDataFixer")
class AetherDataFixerTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final DataVersion VERSION_1 = new DataVersion(1);
    private static final DataVersion VERSION_2 = new DataVersion(2);

    private SimpleSchemaRegistry schemaRegistry;
    private DataFixer dataFixer;
    private AetherDataFixer aetherDataFixer;

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

        dataFixer = new DataFixerBuilder(VERSION_2)
                .addFix(PLAYER, createIdentityFix("v1_to_v2", 1, 2))
                .build();

        aetherDataFixer = new AetherDataFixer(VERSION_2, schemaRegistry, dataFixer);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("creates instance with valid arguments")
        void createsInstanceWithValidArguments() {
            AetherDataFixer fixer = new AetherDataFixer(VERSION_1, schemaRegistry, dataFixer);

            assertThat(fixer.currentVersion()).isEqualTo(VERSION_1);
        }

        @Test
        @DisplayName("rejects null currentVersion")
        void rejectsNullCurrentVersion() {
            assertThatThrownBy(() -> new AetherDataFixer(null, schemaRegistry, dataFixer))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("currentVersion");
        }

        @Test
        @DisplayName("rejects null schemaRegistry")
        void rejectsNullSchemaRegistry() {
            assertThatThrownBy(() -> new AetherDataFixer(VERSION_1, null, dataFixer))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("schemaRegistry");
        }

        @Test
        @DisplayName("rejects null dataFixer")
        void rejectsNullDataFixer() {
            assertThatThrownBy(() -> new AetherDataFixer(VERSION_1, schemaRegistry, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("dataFixer");
        }
    }

    @Nested
    @DisplayName("currentVersion()")
    class CurrentVersion {

        @Test
        @DisplayName("returns configured version")
        void returnsConfiguredVersion() {
            assertThat(aetherDataFixer.currentVersion()).isEqualTo(VERSION_2);
        }

        @Test
        @DisplayName("returns different version when configured differently")
        void returnsDifferentVersionWhenConfiguredDifferently() {
            AetherDataFixer fixer = new AetherDataFixer(VERSION_1, schemaRegistry, dataFixer);
            assertThat(fixer.currentVersion()).isEqualTo(VERSION_1);
        }
    }

    @Nested
    @DisplayName("update() with TaggedDynamic")
    class UpdateTaggedDynamic {

        @Test
        @DisplayName("updates tagged dynamic between versions")
        void updatesTaggedDynamicBetweenVersions() {
            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("name", "Bob");
            inputObj.addProperty("level", 5);
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = aetherDataFixer.update(input, VERSION_1, VERSION_2);

            assertThat(result.type()).isEqualTo(PLAYER);
            assertThat(result.value().get("name").asString().result()).contains("Bob");
        }

        @Test
        @DisplayName("returns same data when versions are equal")
        void returnsSameDataWhenVersionsAreEqual() {
            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("name", "Alice");
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = aetherDataFixer.update(input, VERSION_1, VERSION_1);

            assertThat(result.value().get("name").asString().result()).contains("Alice");
        }

        @Test
        @DisplayName("rejects null input")
        void rejectsNullInput() {
            assertThatThrownBy(() -> aetherDataFixer.update(null, VERSION_1, VERSION_2))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("input");
        }

        @Test
        @DisplayName("rejects null fromVersion")
        void rejectsNullFromVersion() {
            JsonObject obj = new JsonObject();
            TaggedDynamic input = new TaggedDynamic(PLAYER, new Dynamic<>(GsonOps.INSTANCE, obj));

            assertThatThrownBy(() -> aetherDataFixer.update(input, null, VERSION_2))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("fromVersion");
        }

        @Test
        @DisplayName("rejects null toVersion")
        void rejectsNullToVersion() {
            JsonObject obj = new JsonObject();
            TaggedDynamic input = new TaggedDynamic(PLAYER, new Dynamic<>(GsonOps.INSTANCE, obj));

            assertThatThrownBy(() -> aetherDataFixer.update(input, VERSION_1, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("toVersion");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("handles empty JSON object")
        void handlesEmptyJsonObject() {
            JsonObject emptyObj = new JsonObject();
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, emptyObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = aetherDataFixer.update(input, VERSION_1, VERSION_2);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("preserves nested structures during update")
        void preservesNestedStructuresDuringUpdate() {
            JsonObject nested = new JsonObject();
            nested.addProperty("x", 10);
            nested.addProperty("y", 20);

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("name", "Player");
            inputObj.add("position", nested);
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = aetherDataFixer.update(input, VERSION_1, VERSION_2);

            assertThat(result.value().get("position").get("x").asInt().result()).contains(10);
            assertThat(result.value().get("position").get("y").asInt().result()).contains(20);
        }

        @Test
        @DisplayName("handles array values in input")
        void handlesArrayValuesInInput() {
            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("name", "Player");
            JsonArray items = new JsonArray();
            items.add("sword");
            items.add("shield");
            inputObj.add("inventory", items);
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = aetherDataFixer.update(input, VERSION_1, VERSION_2);

            assertThat(result.value().get("inventory").asListStream().result())
                    .isPresent();
        }

        @Test
        @DisplayName("handles numeric values with different types")
        void handlesNumericValuesWithDifferentTypes() {
            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("intValue", 42);
            inputObj.addProperty("longValue", 9876543210L);
            inputObj.addProperty("doubleValue", 3.14159);
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = aetherDataFixer.update(input, VERSION_1, VERSION_2);

            assertThat(result.value().get("intValue").asInt().result()).contains(42);
            assertThat(result.value().get("longValue").asLong().result()).contains(9876543210L);
            assertThat(result.value().get("doubleValue").asDouble().result()).contains(3.14159);
        }

        @Test
        @DisplayName("handles boolean values")
        void handlesBooleanValues() {
            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("active", true);
            inputObj.addProperty("deleted", false);
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = aetherDataFixer.update(input, VERSION_1, VERSION_2);

            assertThat(result.value().get("active").asBoolean().result()).contains(true);
            assertThat(result.value().get("deleted").asBoolean().result()).contains(false);
        }

        @Test
        @DisplayName("handles null-like values gracefully")
        void handlesNullLikeValuesGracefully() {
            JsonObject inputObj = new JsonObject();
            inputObj.add("nullField", null);
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = aetherDataFixer.update(input, VERSION_1, VERSION_2);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("handles deeply nested structures")
        void handlesDeeplyNestedStructures() {
            JsonObject level3 = new JsonObject();
            level3.addProperty("value", "deep");

            JsonObject level2 = new JsonObject();
            level2.add("level3", level3);

            JsonObject level1 = new JsonObject();
            level1.add("level2", level2);

            JsonObject inputObj = new JsonObject();
            inputObj.add("level1", level1);
            Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            TaggedDynamic input = new TaggedDynamic(PLAYER, dynamic);

            TaggedDynamic result = aetherDataFixer.update(input, VERSION_1, VERSION_2);

            assertThat(result.value()
                    .get("level1")
                    .get("level2")
                    .get("level3")
                    .get("value")
                    .asString().result()).contains("deep");
        }
    }

    private DataFix<JsonElement> createIdentityFix(String name, int from, int to) {
        return new DataFix<>() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull DataVersion fromVersion() {
                return new DataVersion(from);
            }

            @Override
            public @NotNull DataVersion toVersion() {
                return new DataVersion(to);
            }

            @Override
            public @NotNull Dynamic<JsonElement> apply(
                    @NotNull TypeReference type,
                    @NotNull Dynamic<JsonElement> input,
                    @NotNull DataFixerContext context
            ) {
                return input;
            }
        };
    }
}
