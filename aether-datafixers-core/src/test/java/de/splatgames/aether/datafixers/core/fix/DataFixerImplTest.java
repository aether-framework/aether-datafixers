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
import com.google.gson.JsonPrimitive;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.exception.FixException;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DataFixerImpl}.
 */
@DisplayName("DataFixerImpl")
class DataFixerImplTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference WORLD = new TypeReference("world");

    private DataFixerBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DataFixerBuilder(new DataVersion(5));
    }

    private Dynamic<JsonElement> createDynamic(String key, String value) {
        JsonObject obj = new JsonObject();
        obj.addProperty(key, value);
        return new Dynamic<>(GsonOps.INSTANCE, obj);
    }

    private Dynamic<JsonElement> createDynamic(String key, int value) {
        JsonObject obj = new JsonObject();
        obj.addProperty(key, value);
        return new Dynamic<>(GsonOps.INSTANCE, obj);
    }

    @Nested
    @DisplayName("Basic Operations")
    class BasicOperations {

        @Test
        @DisplayName("currentVersion() returns configured version")
        void currentVersionReturnsConfiguredVersion() {
            DataFixer fixer = builder.build();

            assertThat(fixer.currentVersion()).isEqualTo(new DataVersion(5));
        }

        @Test
        @DisplayName("update() returns input unchanged when versions are equal")
        void updateReturnsUnchangedWhenVersionsEqual() {
            DataFixer fixer = builder.build();
            Dynamic<JsonElement> input = createDynamic("name", "Alice");

            Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(1)
            );

            assertThat(result).isSameAs(input);
        }

        @Test
        @DisplayName("update() returns input unchanged when no fixes registered")
        void updateReturnsUnchangedWhenNoFixes() {
            DataFixer fixer = builder.build();
            Dynamic<JsonElement> input = createDynamic("name", "Alice");

            Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(3)
            );

            assertThat(result.value()).isEqualTo(input.value());
        }
    }

    @Nested
    @DisplayName("Fix Application")
    class FixApplication {

        @Test
        @DisplayName("applies single fix correctly")
        void appliesSingleFixCorrectly() {
            // Fix that renames "playerName" to "name"
            DataFix<JsonElement> renameFix = new DataFix<>() {
                @Override
                public @NotNull String name() {
                    return "rename_player_name";
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
                    Dynamic<JsonElement> playerName = input.get("playerName");
                    if (playerName != null) {
                        return input.remove("playerName").set("name", playerName);
                    }
                    return input;
                }
            };

            DataFixer fixer = builder.addFix(PLAYER, renameFix).build();

            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("playerName", "Alice");
            Dynamic<JsonElement> input = new Dynamic<>(GsonOps.INSTANCE, inputObj);

            Dynamic<JsonElement> result = fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result.get("name").asString().result()).contains("Alice");
            assertThat(result.get("playerName")).isNull();
        }

        @Test
        @DisplayName("applies multiple fixes in order")
        void appliesMultipleFixesInOrder() {
            List<String> appliedFixes = new ArrayList<>();

            DataFix<JsonElement> fix1 = createTrackingFix("fix1", 1, 2, appliedFixes);
            DataFix<JsonElement> fix2 = createTrackingFix("fix2", 2, 3, appliedFixes);
            DataFix<JsonElement> fix3 = createTrackingFix("fix3", 3, 4, appliedFixes);

            DataFixer fixer = builder
                    .addFix(PLAYER, fix1)
                    .addFix(PLAYER, fix2)
                    .addFix(PLAYER, fix3)
                    .build();

            Dynamic<JsonElement> input = createDynamic("name", "Alice");
            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(4));

            assertThat(appliedFixes).containsExactly("fix1", "fix2", "fix3");
        }

        @Test
        @DisplayName("only applies fixes for the specified type")
        void onlyAppliesFixesForSpecifiedType() {
            List<String> appliedFixes = new ArrayList<>();

            DataFix<JsonElement> playerFix = createTrackingFix("playerFix", 1, 2, appliedFixes);
            DataFix<JsonElement> worldFix = createTrackingFix("worldFix", 1, 2, appliedFixes);

            DataFixer fixer = builder
                    .addFix(PLAYER, playerFix)
                    .addFix(WORLD, worldFix)
                    .build();

            Dynamic<JsonElement> input = createDynamic("name", "Alice");
            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(2));

            assertThat(appliedFixes).containsExactly("playerFix");
        }

        @Test
        @DisplayName("only applies fixes within version range")
        void onlyAppliesFixesWithinVersionRange() {
            List<String> appliedFixes = new ArrayList<>();

            DataFix<JsonElement> fix1 = createTrackingFix("fix1", 1, 2, appliedFixes);
            DataFix<JsonElement> fix2 = createTrackingFix("fix2", 2, 3, appliedFixes);
            DataFix<JsonElement> fix3 = createTrackingFix("fix3", 3, 4, appliedFixes);
            DataFix<JsonElement> fix4 = createTrackingFix("fix4", 4, 5, appliedFixes);

            DataFixer fixer = builder
                    .addFix(PLAYER, fix1)
                    .addFix(PLAYER, fix2)
                    .addFix(PLAYER, fix3)
                    .addFix(PLAYER, fix4)
                    .build();

            Dynamic<JsonElement> input = createDynamic("name", "Alice");
            fixer.update(PLAYER, input, new DataVersion(2), new DataVersion(4));

            assertThat(appliedFixes).containsExactly("fix2", "fix3");
        }

        private DataFix<JsonElement> createTrackingFix(String name, int from, int to, List<String> tracker) {
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

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("throws FixException when fix throws exception")
        void throwsFixExceptionWhenFixThrows() {
            DataFix<JsonElement> failingFix = new DataFix<>() {
                @Override
                public @NotNull String name() {
                    return "failing_fix";
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
                    throw new RuntimeException("Intentional failure");
                }
            };

            DataFixer fixer = builder.addFix(PLAYER, failingFix).build();
            Dynamic<JsonElement> input = createDynamic("name", "Alice");

            assertThatThrownBy(() -> fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(2)
            ))
                    .isInstanceOf(FixException.class)
                    .hasMessageContaining("failing_fix")
                    .hasMessageContaining("Intentional failure");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when fromVersion > toVersion")
        void throwsWhenFromVersionGreaterThanToVersion() {
            DataFixer fixer = builder.build();
            Dynamic<JsonElement> input = createDynamic("name", "Alice");

            assertThatThrownBy(() -> fixer.update(
                    PLAYER, input,
                    new DataVersion(3), new DataVersion(1)
            ))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when toVersion > currentVersion")
        void throwsWhenToVersionGreaterThanCurrentVersion() {
            DataFixer fixer = builder.build();
            Dynamic<JsonElement> input = createDynamic("name", "Alice");

            assertThatThrownBy(() -> fixer.update(
                    PLAYER, input,
                    new DataVersion(1), new DataVersion(10)
            ))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Context Integration")
    class ContextIntegration {

        @Test
        @DisplayName("passes context to fixes")
        void passesContextToFixes() {
            List<String> loggedMessages = new ArrayList<>();
            DataFixerContext testContext = new DataFixerContext() {
                @Override
                public void info(@NotNull String message, Object... args) {
                    loggedMessages.add(String.format(message, args));
                }

                @Override
                public void warn(@NotNull String message, Object... args) {
                    loggedMessages.add("WARN: " + String.format(message, args));
                }
            };

            DataFix<JsonElement> loggingFix = new DataFix<>() {
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
            };

            DataFixer fixer = builder
                    .withDefaultContext(testContext)
                    .addFix(PLAYER, loggingFix)
                    .build();

            Dynamic<JsonElement> input = createDynamic("name", "Alice");
            fixer.update(PLAYER, input, new DataVersion(1), new DataVersion(2));

            assertThat(loggedMessages).contains("Processing player data");
        }
    }
}
