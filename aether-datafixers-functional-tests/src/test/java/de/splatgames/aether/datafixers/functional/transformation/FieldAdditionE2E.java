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
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;

/**
 * E2E tests for field addition transformations.
 */
@DisplayName("Field Addition E2E")
@Tag("e2e")
class FieldAdditionE2E {

    private static final TypeReference TEST_TYPE = new TypeReference("test_entity");

    @Nested
    @DisplayName("Add String Fields")
    class AddStringFields {

        @Test
        @DisplayName("adds string field with default value")
        void addsStringFieldWithDefaultValue() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.addStringField(
                            GsonOps.INSTANCE, "add_string", 1, 2, "newString", "defaultValue"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("existingField", "existingValue")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasStringField("newString", "defaultValue");
            assertThat(result).hasStringField("existingField", "existingValue");
        }

        @Test
        @DisplayName("does not overwrite existing string field")
        void doesNotOverwriteExistingStringField() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.addStringField(
                            GsonOps.INSTANCE, "add_string", 1, 2, "targetField", "defaultValue"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("targetField", "customValue")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            // Should keep original value, not overwrite
            assertThat(result).hasStringField("targetField", "customValue");
        }
    }

    @Nested
    @DisplayName("Add Integer Fields")
    class AddIntegerFields {

        @Test
        @DisplayName("adds integer field with default value")
        void addsIntegerFieldWithDefaultValue() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.addIntField(
                            GsonOps.INSTANCE, "add_int", 1, 2, "score", 0))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "TestEntity")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasIntField("score", 0);
            assertThat(result).hasStringField("name", "TestEntity");
        }

        @Test
        @DisplayName("does not overwrite existing integer field")
        void doesNotOverwriteExistingIntegerField() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.addIntField(
                            GsonOps.INSTANCE, "add_int", 1, 2, "level", 1))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("level", 50)
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasIntField("level", 50);
        }
    }

    @Nested
    @DisplayName("Add Boolean Fields")
    class AddBooleanFields {

        @Test
        @DisplayName("adds boolean field with default value")
        void addsBooleanFieldWithDefaultValue() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.addBooleanField(
                            GsonOps.INSTANCE, "add_bool", 1, 2, "active", true))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "TestEntity")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasBooleanField("active", true);
            assertThat(result).hasStringField("name", "TestEntity");
        }

        @Test
        @DisplayName("adds false as default boolean")
        void addsFalseAsDefaultBoolean() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.addBooleanField(
                            GsonOps.INSTANCE, "add_bool", 1, 2, "disabled", false))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "TestEntity")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasBooleanField("disabled", false);
        }
    }

    @Nested
    @DisplayName("Add Double Fields")
    class AddDoubleFields {

        @Test
        @DisplayName("adds double field with default value")
        void addsDoubleFieldWithDefaultValue() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.simple("add_double", 1, 2, d -> {
                        if (d.get("coordinate") == null) {
                            return d.set("coordinate", d.createDouble(0.0));
                        }
                        return d;
                    }))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "TestEntity")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasDoubleField("coordinate", 0.0, 0.001);
            assertThat(result).hasStringField("name", "TestEntity");
        }
    }

    @Nested
    @DisplayName("Add Multiple Fields in Chain")
    class AddMultipleFieldsInChain {

        @Test
        @DisplayName("adds multiple fields across versions")
        void addsMultipleFieldsAcrossVersions() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(4))
                    .addFix(TEST_TYPE, QuickFix.addStringField(
                            GsonOps.INSTANCE, "add_type", 1, 2, "type", "unknown"))
                    .addFix(TEST_TYPE, QuickFix.addIntField(
                            GsonOps.INSTANCE, "add_score", 2, 3, "score", 0))
                    .addFix(TEST_TYPE, QuickFix.addBooleanField(
                            GsonOps.INSTANCE, "add_active", 3, 4, "active", true))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "ChainEntity")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(4)
            );

            assertThat(result).hasStringField("name", "ChainEntity");
            assertThat(result).hasStringField("type", "unknown");
            assertThat(result).hasIntField("score", 0);
            assertThat(result).hasBooleanField("active", true);
        }
    }

    @Nested
    @DisplayName("Add Computed Fields")
    class AddComputedFields {

        @Test
        @DisplayName("adds computed field based on existing data")
        void addsComputedFieldBasedOnExistingData() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.simple("add_full_name", 1, 2, d -> {
                        final String first = d.get("firstName").asString().result().orElse("");
                        final String last = d.get("lastName").asString().result().orElse("");
                        return d.set("fullName", d.createString(first + " " + last));
                    }))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("firstName", "John")
                    .put("lastName", "Doe")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasStringField("fullName", "John Doe");
            assertThat(result).hasStringField("firstName", "John");
            assertThat(result).hasStringField("lastName", "Doe");
        }

        @Test
        @DisplayName("adds version field")
        void addsVersionField() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.simple("add_version", 1, 2, d -> {
                        if (d.get("version") == null) {
                            return d.set("version", d.createInt(2));
                        }
                        return d;
                    }))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "TestEntity")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasIntField("version", 2);
        }
    }

    @Nested
    @DisplayName("Conditional Field Addition")
    class ConditionalFieldAddition {

        @Test
        @DisplayName("adds field only if condition is met")
        void addsFieldOnlyIfConditionIsMet() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.simple("conditional_add", 1, 2, d -> {
                        // Only add premium field if level > 10
                        final int level = d.get("level").asInt().result().orElse(0);
                        if (level > 10 && d.get("premium") == null) {
                            return d.set("premium", d.createBoolean(true));
                        }
                        return d;
                    }))
                    .build();

            final Dynamic<JsonElement> highLevelInput = TestData.gson().object()
                    .put("name", "HighLevel")
                    .put("level", 15)
                    .build();

            final Dynamic<JsonElement> lowLevelInput = TestData.gson().object()
                    .put("name", "LowLevel")
                    .put("level", 5)
                    .build();

            final Dynamic<JsonElement> highResult = fixer.update(
                    TEST_TYPE, highLevelInput,
                    new DataVersion(1), new DataVersion(2)
            );

            final Dynamic<JsonElement> lowResult = fixer.update(
                    TEST_TYPE, lowLevelInput,
                    new DataVersion(1), new DataVersion(2)
            );

            // High level gets premium
            assertThat(highResult).hasBooleanField("premium", true);

            // Low level does not get premium
            assertThat(lowResult).doesNotHaveField("premium");
        }
    }
}
