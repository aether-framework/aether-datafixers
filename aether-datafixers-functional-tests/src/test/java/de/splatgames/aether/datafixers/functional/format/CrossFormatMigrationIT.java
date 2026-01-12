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

package de.splatgames.aether.datafixers.functional.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.codec.json.jackson.JacksonJsonOps;
import de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps;
import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for migrations across different data formats.
 */
@DisplayName("Cross-Format Migration IT")
@Tag("integration")
class CrossFormatMigrationIT {

    private static final TypeReference PLAYER = new TypeReference("player");

    @Nested
    @DisplayName("Gson Format")
    class GsonFormat {

        @Test
        @DisplayName("completes full migration chain with Gson")
        void completesFullMigrationChainWithGson() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(3))
                    .addFix(PLAYER, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_name", 1, 2, "playerName", "name"))
                    .addFix(PLAYER, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_xp", 2, 3, "xp", "experience"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("playerName", "GsonPlayer")
                    .put("xp", 1000)
                    .put("level", 10)
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(3)
            );

            assertThat(result.get("name").asString().result()).contains("GsonPlayer");
            assertThat(result.get("experience").asInt().result()).contains(1000);
            assertThat(result.get("level").asInt().result()).contains(10);
        }

        @Test
        @DisplayName("handles partial migration with Gson")
        void handlesPartialMigrationWithGson() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(3))
                    .addFix(PLAYER, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_v1_v2", 1, 2, "oldField", "midField"))
                    .addFix(PLAYER, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_v2_v3", 2, 3, "midField", "newField"))
                    .build();

            // Start from v2, should only apply v2->v3
            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("midField", "value")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(2), new DataVersion(3)
            );

            assertThat(result.get("newField").asString().result()).contains("value");
            assertThat(result.get("midField")).isNull();
        }
    }

    @Nested
    @DisplayName("Jackson JSON Format")
    class JacksonJsonFormat {

        @Test
        @DisplayName("completes full migration chain with Jackson JSON")
        void completesFullMigrationChainWithJacksonJson() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(3))
                    .addFix(PLAYER, QuickFix.renameField(
                            JacksonJsonOps.INSTANCE, "rename_name", 1, 2, "playerName", "name"))
                    .addFix(PLAYER, QuickFix.renameField(
                            JacksonJsonOps.INSTANCE, "rename_xp", 2, 3, "xp", "experience"))
                    .build();

            final Dynamic<JsonNode> input = TestData.jacksonJson().object()
                    .put("playerName", "JacksonPlayer")
                    .put("xp", 2000)
                    .put("score", 500)
                    .build();

            final Dynamic<JsonNode> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(3)
            );

            assertThat(result.get("name").asString().result()).contains("JacksonPlayer");
            assertThat(result.get("experience").asInt().result()).contains(2000);
            assertThat(result.get("score").asInt().result()).contains(500);
        }
    }

    @Nested
    @DisplayName("Jackson YAML Format")
    class JacksonYamlFormat {

        @Test
        @DisplayName("completes full migration chain with Jackson YAML")
        void completesFullMigrationChainWithJacksonYaml() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(3))
                    .addFix(PLAYER, QuickFix.renameField(
                            JacksonYamlOps.INSTANCE, "rename_name", 1, 2, "playerName", "name"))
                    .addFix(PLAYER, QuickFix.addIntField(
                            JacksonYamlOps.INSTANCE, "add_level", 2, 3, "level", 1))
                    .build();

            final Dynamic<JsonNode> input = TestData.jacksonYaml().object()
                    .put("playerName", "YamlPlayer")
                    .put("score", 3000)
                    .build();

            final Dynamic<JsonNode> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(3)
            );

            assertThat(result.get("name").asString().result()).contains("YamlPlayer");
            assertThat(result.get("score").asInt().result()).contains(3000);
            assertThat(result.get("level").asInt().result()).contains(1);
        }
    }

    @Nested
    @DisplayName("SnakeYAML Format")
    class SnakeYamlFormat {

        @Test
        @DisplayName("completes migration with SnakeYAML")
        void completesMigrationWithSnakeYaml() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(PLAYER, QuickFix.renameField(
                            SnakeYamlOps.INSTANCE, "rename_name", 1, 2, "playerName", "name"))
                    .build();

            final Dynamic<Object> input = TestData.snakeYaml().object()
                    .put("playerName", "SnakePlayer")
                    .put("score", 4000)
                    .build();

            final Dynamic<Object> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result.get("name").asString().result()).contains("SnakePlayer");
            assertThat(result.get("score").asInt().result()).contains(4000);
        }
    }

    @Nested
    @DisplayName("Format Consistency")
    class FormatConsistency {

        @Test
        @DisplayName("same migration produces consistent results across Gson and Jackson")
        void sameMigrationProducesConsistentResultsAcrossFormats() {
            // Create equivalent fixers for each format
            final DataFixer gsonFixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(PLAYER, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename", 1, 2, "oldField", "newField"))
                    .build();

            final DataFixer jacksonFixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(PLAYER, QuickFix.renameField(
                            JacksonJsonOps.INSTANCE, "rename", 1, 2, "oldField", "newField"))
                    .build();

            // Apply to equivalent inputs
            final Dynamic<JsonElement> gsonInput = TestData.gson().object()
                    .put("oldField", "testValue")
                    .put("untouched", 42)
                    .build();

            final Dynamic<JsonNode> jacksonInput = TestData.jacksonJson().object()
                    .put("oldField", "testValue")
                    .put("untouched", 42)
                    .build();

            final Dynamic<JsonElement> gsonResult = gsonFixer.update(
                    PLAYER, gsonInput,
                    new DataVersion(1), new DataVersion(2)
            );

            final Dynamic<JsonNode> jacksonResult = jacksonFixer.update(
                    PLAYER, jacksonInput,
                    new DataVersion(1), new DataVersion(2)
            );

            // Both should have the same field names and values
            assertThat(gsonResult.get("newField").asString().result())
                    .isEqualTo(jacksonResult.get("newField").asString().result())
                    .contains("testValue");

            assertThat(gsonResult.get("untouched").asInt().result())
                    .isEqualTo(jacksonResult.get("untouched").asInt().result())
                    .contains(42);

            // Both should not have old field
            assertThat(gsonResult.get("oldField")).isNull();
            assertThat(jacksonResult.get("oldField")).isNull();
        }
    }

    @Nested
    @DisplayName("Complex Nested Structures")
    class ComplexNestedStructures {

        @Test
        @DisplayName("migrates complex nested structure with Gson")
        void migratesComplexNestedStructureWithGson() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(PLAYER, QuickFix.<JsonElement>simple("rename_in_nested", 1, 2, d -> {
                        final Dynamic<JsonElement> settings = d.get("settings");
                        if (settings != null) {
                            final Dynamic<JsonElement> oldValue = settings.get("oldSetting");
                            if (oldValue != null) {
                                final Dynamic<JsonElement> newSettings = settings.remove("oldSetting")
                                        .set("newSetting", oldValue);
                                return d.set("settings", newSettings);
                            }
                        }
                        return d;
                    }))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Player")
                    .putObject("settings", s -> s
                            .put("oldSetting", "value")
                            .put("otherSetting", 100))
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result.get("name").asString().result()).contains("Player");
            assertThat(result.get("settings").get("newSetting").asString().result()).contains("value");
            assertThat(result.get("settings").get("otherSetting").asInt().result()).contains(100);
            assertThat(result.get("settings").get("oldSetting")).isNull();
        }
    }

    @Nested
    @DisplayName("No-Op Migration")
    class NoOpMigration {

        @Test
        @DisplayName("no-op migration preserves data unchanged")
        void noOpMigrationPreservesDataUnchanged() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(5))
                    .addFix(PLAYER, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename", 1, 2, "a", "b"))
                    .build();

            // Data is already at version 3, fixer only has v1->v2 fix
            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("field1", "value1")
                    .put("field2", 42)
                    .build();

            // Migration from v3 to v5 should not modify anything (no fixes in that range)
            final Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(3), new DataVersion(5)
            );

            // Data should be unchanged
            assertThat(result.get("field1").asString().result()).contains("value1");
            assertThat(result.get("field2").asInt().result()).contains(42);
        }
    }
}
