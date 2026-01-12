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

package de.splatgames.aether.datafixers.functional.transformation;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.testkit.TestData;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;

/**
 * E2E tests for field grouping and ungrouping transformations.
 */
@DisplayName("Field Grouping E2E")
@Tag("e2e")
class FieldGroupingE2E {

    private static final TypeReference TEST_TYPE = new TypeReference("test_entity");

    @Nested
    @DisplayName("Group Flat Fields into Nested Object")
    class GroupFlatFields {

        @Test
        @DisplayName("groups x, y, z into position object")
        void groupsCoordinatesIntoPosition() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, new GroupPositionFix())
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Entity1")
                    .put("x", 100.0)
                    .put("y", 64.0)
                    .put("z", -200.0)
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            // Original fields should be removed
            assertThat(result).doesNotHaveField("x");
            assertThat(result).doesNotHaveField("y");
            assertThat(result).doesNotHaveField("z");

            // Position should be a nested object
            assertThat(result).hasField("position");
            assertThat(result.get("position")).hasDoubleField("x", 100.0, 0.01);
            assertThat(result.get("position")).hasDoubleField("y", 64.0, 0.01);
            assertThat(result.get("position")).hasDoubleField("z", -200.0, 0.01);

            // Other fields preserved
            assertThat(result).hasStringField("name", "Entity1");
        }

        @Test
        @DisplayName("groups address fields into address object")
        void groupsAddressFieldsIntoAddressObject() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, new GroupAddressFix())
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "John Doe")
                    .put("street", "123 Main St")
                    .put("city", "Anytown")
                    .put("zipCode", "12345")
                    .put("country", "USA")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasStringField("name", "John Doe");
            assertThat(result).hasField("address");
            assertThat(result.get("address")).hasStringField("street", "123 Main St");
            assertThat(result.get("address")).hasStringField("city", "Anytown");
            assertThat(result.get("address")).hasStringField("zipCode", "12345");
            assertThat(result.get("address")).hasStringField("country", "USA");
        }
    }

    @Nested
    @DisplayName("Flatten Nested Object into Flat Fields")
    class FlattenNestedObject {

        @Test
        @DisplayName("flattens position object into x, y, z fields")
        void flattensPositionObjectIntoCoordinates() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, new FlattenPositionFix())
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Entity2")
                    .putObject("position", pos -> pos
                            .put("x", 50.0)
                            .put("y", 70.0)
                            .put("z", 150.0))
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).doesNotHaveField("position");
            assertThat(result).hasDoubleField("x", 50.0, 0.01);
            assertThat(result).hasDoubleField("y", 70.0, 0.01);
            assertThat(result).hasDoubleField("z", 150.0, 0.01);
            assertThat(result).hasStringField("name", "Entity2");
        }
    }

    @Nested
    @DisplayName("Partial Grouping")
    class PartialGrouping {

        @Test
        @DisplayName("handles missing fields during grouping")
        void handlesMissingFieldsDuringGrouping() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, new GroupPositionFix())
                    .build();

            // Only x and y are present, z is missing
            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "PartialEntity")
                    .put("x", 10.0)
                    .put("y", 20.0)
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            // Should remain flat since z is missing
            assertThat(result).hasDoubleField("x", 10.0, 0.01);
            assertThat(result).hasDoubleField("y", 20.0, 0.01);
            assertThat(result).doesNotHaveField("position");
        }
    }

    @Nested
    @DisplayName("Nested to Deeper Nesting")
    class DeepNesting {

        @Test
        @DisplayName("moves nested object into deeper structure")
        void movesNestedObjectIntoDeeperStructure() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, new NestSettingsFix())
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "DeepEntity")
                    .putObject("settings", s -> s
                            .put("volume", 80)
                            .put("brightness", 50))
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).doesNotHaveField("settings");
            assertThat(result).hasField("config");
            assertThat(result.get("config")).hasField("display");
            assertThat(result.get("config").get("display")).hasIntField("volume", 80);
            assertThat(result.get("config").get("display")).hasIntField("brightness", 50);
        }
    }

    @Nested
    @DisplayName("Combined Group and Ungroup")
    class CombinedGroupAndUngroup {

        @Test
        @DisplayName("groups and ungroups fields across versions")
        void groupsAndUngroupsFieldsAcrossVersions() {
            // V1 -> V2: Group into position
            // V2 -> V3: Ungroup back to flat with renamed fields
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(3))
                    .addFix(TEST_TYPE, new GroupPositionFix())
                    .addFix(TEST_TYPE, new UngroupPositionWithRenameFix())
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "TransformEntity")
                    .put("x", 1.0)
                    .put("y", 2.0)
                    .put("z", 3.0)
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(3)
            );

            // After round-trip: now flat with renamed fields
            assertThat(result).doesNotHaveField("x");
            assertThat(result).doesNotHaveField("y");
            assertThat(result).doesNotHaveField("z");
            assertThat(result).doesNotHaveField("position");
            assertThat(result).hasDoubleField("posX", 1.0, 0.01);
            assertThat(result).hasDoubleField("posY", 2.0, 0.01);
            assertThat(result).hasDoubleField("posZ", 3.0, 0.01);
        }
    }

    // ==================== Helper Fix Classes ====================

    /**
     * Groups x, y, z fields into a position object.
     */
    static class GroupPositionFix implements DataFix<JsonElement> {
        @Override
        public @NotNull String name() {
            return "group_position";
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
                @NotNull final TypeReference type,
                @NotNull final Dynamic<JsonElement> input,
                @NotNull final DataFixerContext context
        ) {
            final Dynamic<JsonElement> x = input.get("x");
            final Dynamic<JsonElement> y = input.get("y");
            final Dynamic<JsonElement> z = input.get("z");

            if (x != null && y != null && z != null) {
                // Create position object using emptyMap and set
                Dynamic<JsonElement> position = new Dynamic<>(GsonOps.INSTANCE, GsonOps.INSTANCE.emptyMap());
                position = position.set("x", x);
                position = position.set("y", y);
                position = position.set("z", z);

                return input.remove("x").remove("y").remove("z")
                        .set("position", position);
            }
            return input;
        }
    }

    /**
     * Flattens position object back to x, y, z fields.
     */
    static class FlattenPositionFix implements DataFix<JsonElement> {
        @Override
        public @NotNull String name() {
            return "flatten_position";
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
                @NotNull final TypeReference type,
                @NotNull final Dynamic<JsonElement> input,
                @NotNull final DataFixerContext context
        ) {
            final Dynamic<JsonElement> position = input.get("position");
            if (position != null) {
                final Dynamic<JsonElement> x = position.get("x");
                final Dynamic<JsonElement> y = position.get("y");
                final Dynamic<JsonElement> z = position.get("z");

                Dynamic<JsonElement> result = input.remove("position");
                if (x != null) result = result.set("x", x);
                if (y != null) result = result.set("y", y);
                if (z != null) result = result.set("z", z);
                return result;
            }
            return input;
        }
    }

    /**
     * Groups address fields into an address object.
     */
    static class GroupAddressFix implements DataFix<JsonElement> {
        @Override
        public @NotNull String name() {
            return "group_address";
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
                @NotNull final TypeReference type,
                @NotNull final Dynamic<JsonElement> input,
                @NotNull final DataFixerContext context
        ) {
            final Dynamic<JsonElement> street = input.get("street");
            final Dynamic<JsonElement> city = input.get("city");
            final Dynamic<JsonElement> zipCode = input.get("zipCode");
            final Dynamic<JsonElement> country = input.get("country");

            if (street != null && city != null) {
                Dynamic<JsonElement> address = new Dynamic<>(GsonOps.INSTANCE, GsonOps.INSTANCE.emptyMap());
                address = address.set("street", street);
                address = address.set("city", city);
                if (zipCode != null) {
                    address = address.set("zipCode", zipCode);
                }
                if (country != null) {
                    address = address.set("country", country);
                }
                return input.remove("street").remove("city").remove("zipCode").remove("country")
                        .set("address", address);
            }
            return input;
        }
    }

    /**
     * Nests settings into config.display.
     */
    static class NestSettingsFix implements DataFix<JsonElement> {
        @Override
        public @NotNull String name() {
            return "nest_settings";
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
                @NotNull final TypeReference type,
                @NotNull final Dynamic<JsonElement> input,
                @NotNull final DataFixerContext context
        ) {
            final Dynamic<JsonElement> settings = input.get("settings");
            if (settings != null) {
                Dynamic<JsonElement> config = new Dynamic<>(GsonOps.INSTANCE, GsonOps.INSTANCE.emptyMap());
                config = config.set("display", settings);
                return input.remove("settings").set("config", config);
            }
            return input;
        }
    }

    /**
     * Ungroups position to posX, posY, posZ fields.
     */
    static class UngroupPositionWithRenameFix implements DataFix<JsonElement> {
        @Override
        public @NotNull String name() {
            return "ungroup_position";
        }

        @Override
        public @NotNull DataVersion fromVersion() {
            return new DataVersion(2);
        }

        @Override
        public @NotNull DataVersion toVersion() {
            return new DataVersion(3);
        }

        @Override
        public @NotNull Dynamic<JsonElement> apply(
                @NotNull final TypeReference type,
                @NotNull final Dynamic<JsonElement> input,
                @NotNull final DataFixerContext context
        ) {
            final Dynamic<JsonElement> position = input.get("position");
            if (position != null) {
                final Dynamic<JsonElement> x = position.get("x");
                final Dynamic<JsonElement> y = position.get("y");
                final Dynamic<JsonElement> z = position.get("z");

                Dynamic<JsonElement> result = input.remove("position");
                if (x != null) result = result.set("posX", x);
                if (y != null) result = result.set("posY", y);
                if (z != null) result = result.set("posZ", z);
                return result;
            }
            return input;
        }
    }
}
