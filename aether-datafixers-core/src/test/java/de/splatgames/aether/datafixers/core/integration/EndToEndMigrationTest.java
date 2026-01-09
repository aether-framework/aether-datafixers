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

package de.splatgames.aether.datafixers.core.integration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.core.fix.SimpleSystemDataFixerContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for the data fixing system.
 */
@DisplayName("End-to-End Migration")
class EndToEndMigrationTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference WORLD = new TypeReference("world");

    @Nested
    @DisplayName("Complete Migration Scenarios")
    class CompleteMigrationScenarios {

        @Test
        @DisplayName("migrates player data through multiple versions")
        void migratesPlayerDataThroughMultipleVersions() {
            // Build fixer with multiple fixes
            DataFixer fixer = new DataFixerBuilder(new DataVersion(5))
                    // Version 1 -> 2: Rename "playerName" to "name"
                    .addFix(PLAYER, createRenameFix("v1_rename", 1, 2, "playerName", "name"))
                    // Version 2 -> 3: Add "score" field with default 0
                    .addFix(PLAYER, createAddFieldFix("v2_add_score", 2, 3, "score", 0))
                    // Version 3 -> 4: Double the score
                    .addFix(PLAYER, createTransformFix("v3_double_score", 3, 4, "score",
                            d -> d.createInt(d.asInt().result().orElse(0) * 2)))
                    // Version 4 -> 5: Add "active" field with default true
                    .addFix(PLAYER, createAddFieldFix("v4_add_active", 4, 5, "active", true))
                    .build();

            // Input: version 1 player data
            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("playerName", "Alice");
            inputObj.addProperty("level", 10);
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            // Migrate from version 1 to version 5
            Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(5)
            );

            // Verify all transformations applied
            assertThat(result.get("name").asString().result()).contains("Alice");
            assertThat(result.get("playerName")).isNull(); // Renamed
            assertThat(result.get("level").asInt().result()).contains(10); // Unchanged
            assertThat(result.get("score").asInt().result()).contains(0); // Added and doubled: 0 * 2 = 0
            assertThat(result.get("active").asBoolean().result()).contains(true); // Added
        }

        @Test
        @DisplayName("migrates world data with nested structures")
        void migratesWorldDataWithNestedStructures() {
            DataFixer fixer = new DataFixerBuilder(new DataVersion(3))
                    // Version 1 -> 2: Rename "worldName" to "name"
                    .addFix(WORLD, createRenameFix("v1_rename", 1, 2, "worldName", "name"))
                    // Version 2 -> 3: Add "dimension" field
                    .addFix(WORLD, createAddFieldFix("v2_add_dimension", 2, 3, "dimension", "overworld"))
                    .build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("worldName", "MyWorld");
            inputObj.addProperty("seed", 12345);
            JsonObject spawn = new JsonObject();
            spawn.addProperty("x", 100);
            spawn.addProperty("y", 64);
            spawn.addProperty("z", -50);
            inputObj.add("spawn", spawn);
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            Dynamic<JsonElement> result = fixer.update(
                    WORLD, input,
                    new DataVersion(1), new DataVersion(3)
            );

            assertThat(result.get("name").asString().result()).contains("MyWorld");
            assertThat(result.get("worldName")).isNull();
            assertThat(result.get("dimension").asString().result()).contains("overworld");
            // Spawn should be preserved
            assertThat(result.get("spawn").get("x").asInt().result()).contains(100);
        }

        @Test
        @DisplayName("handles partial version range migration")
        void handlesPartialVersionRangeMigration() {
            List<String> appliedFixes = new ArrayList<>();

            DataFixer fixer = new DataFixerBuilder(new DataVersion(5))
                    .addFix(PLAYER, createTrackingFix("fix1", 1, 2, appliedFixes))
                    .addFix(PLAYER, createTrackingFix("fix2", 2, 3, appliedFixes))
                    .addFix(PLAYER, createTrackingFix("fix3", 3, 4, appliedFixes))
                    .addFix(PLAYER, createTrackingFix("fix4", 4, 5, appliedFixes))
                    .build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("name", "Test");
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            // Only migrate from version 2 to version 4 (skip fix1 and fix4)
            fixer.update(PLAYER, input, new DataVersion(2), new DataVersion(4));

            assertThat(appliedFixes).containsExactly("fix2", "fix3");
        }
    }

    @Nested
    @DisplayName("Context Integration")
    class ContextIntegrationTests {

        @Test
        @DisplayName("provides context to all fixes")
        void providesContextToAllFixes() {
            List<String> logMessages = new ArrayList<>();
            DataFixerContext testContext = new DataFixerContext() {
                @Override
                public void info(@NotNull String message, Object... args) {
                    logMessages.add("INFO: " + String.format(message, args));
                }

                @Override
                public void warn(@NotNull String message, Object... args) {
                    logMessages.add("WARN: " + String.format(message, args));
                }
            };

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .withDefaultContext(testContext)
                    .addFix(PLAYER, new DataFix<JsonElement>() {
                        @Override
                        public @NotNull String name() {
                            return "logging_fix";
                        }

                        @Override
                        public @NotNull DataVersion fromVersion() {
                            return new DataVersion(1);
                        }

                        @Override
                        public @NotNull DataVersion toVersion() {
                            return new DataVersion(2);
                        }

                        @Override
                        public @NotNull Dynamic<JsonElement> apply(
                                @NotNull TypeReference type,
                                @NotNull Dynamic<JsonElement> input,
                                @NotNull DataFixerContext context
                        ) {
                            context.info("Processing %s data", type.getId());
                            return input;
                        }
                    })
                    .build();

            JsonObject inputObj = new JsonObject();
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);
            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(2));

            assertThat(logMessages).contains("INFO: Processing player data");
        }
    }

    @Nested
    @DisplayName("Type-Specific Fixes")
    class TypeSpecificFixesTests {

        @Test
        @DisplayName("only applies fixes to matching type")
        void onlyAppliesFixesToMatchingType() {
            List<String> appliedFixes = new ArrayList<>();

            DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(PLAYER, createTrackingFix("player_fix", 1, 2, appliedFixes))
                    .addFix(WORLD, createTrackingFix("world_fix", 1, 2, appliedFixes))
                    .build();

            JsonObject inputObj = new JsonObject();
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            // Update PLAYER type
            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(2));

            assertThat(appliedFixes).containsExactly("player_fix");
        }
    }

    // Helper methods for creating test fixes

    private DataFix<JsonElement> createRenameFix(
            String name, int from, int to, String oldName, String newName) {
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
                Dynamic<JsonElement> value = input.get(oldName);
                if (value != null) {
                    return input.remove(oldName).set(newName, value);
                }
                return input;
            }
        };
    }

    private DataFix<JsonElement> createAddFieldFix(
            String name, int from, int to, String fieldName, Object defaultValue) {
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
                if (input.get(fieldName) == null) {
                    if (defaultValue instanceof Integer i) {
                        return input.set(fieldName, input.createInt(i));
                    } else if (defaultValue instanceof Boolean b) {
                        return input.set(fieldName, input.createBoolean(b));
                    } else if (defaultValue instanceof String s) {
                        return input.set(fieldName, input.createString(s));
                    }
                }
                return input;
            }
        };
    }

    private DataFix<JsonElement> createTransformFix(
            String name, int from, int to, String fieldName,
            java.util.function.Function<Dynamic<JsonElement>, Dynamic<JsonElement>> transformer) {
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
                Dynamic<JsonElement> value = input.get(fieldName);
                if (value != null) {
                    return input.set(fieldName, transformer.apply(value));
                }
                return input;
            }
        };
    }

    private DataFix<JsonElement> createTrackingFix(
            String name, int from, int to, List<String> tracker) {
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
                tracker.add(name);
                return input;
            }
        };
    }
}
