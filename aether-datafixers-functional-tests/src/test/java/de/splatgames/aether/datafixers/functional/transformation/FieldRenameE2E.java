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
 * E2E tests for field rename transformations.
 */
@DisplayName("Field Rename E2E")
@Tag("e2e")
class FieldRenameE2E {

    private static final TypeReference TEST_TYPE = new TypeReference("test_entity");

    @Nested
    @DisplayName("Basic Field Rename")
    class BasicFieldRename {

        @Test
        @DisplayName("renames string field correctly")
        void renamesStringFieldCorrectly() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_string", 1, 2, "oldName", "newName"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("oldName", "TestValue")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasStringField("newName", "TestValue");
            assertThat(result).doesNotHaveField("oldName");
        }

        @Test
        @DisplayName("renames integer field correctly")
        void renamesIntegerFieldCorrectly() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_int", 1, 2, "oldCount", "newCount"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("oldCount", 42)
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasIntField("newCount", 42);
            assertThat(result).doesNotHaveField("oldCount");
        }

        @Test
        @DisplayName("renames boolean field correctly")
        void renamesBooleanFieldCorrectly() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_bool", 1, 2, "oldFlag", "newFlag"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("oldFlag", true)
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasBooleanField("newFlag", true);
            assertThat(result).doesNotHaveField("oldFlag");
        }
    }

    @Nested
    @DisplayName("Field Rename with Other Fields")
    class RenameWithOtherFields {

        @Test
        @DisplayName("preserves other fields when renaming")
        void preservesOtherFieldsWhenRenaming() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "selective_rename", 1, 2, "targetField", "renamedField"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("targetField", "rename_me")
                    .put("preservedField1", "keep_me")
                    .put("preservedField2", 100)
                    .put("preservedField3", true)
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasStringField("renamedField", "rename_me");
            assertThat(result).hasStringField("preservedField1", "keep_me");
            assertThat(result).hasIntField("preservedField2", 100);
            assertThat(result).hasBooleanField("preservedField3", true);
            assertThat(result).doesNotHaveField("targetField");
        }
    }

    @Nested
    @DisplayName("Missing Field Handling")
    class MissingFieldHandling {

        @Test
        @DisplayName("gracefully handles missing field to rename")
        void gracefullyHandlesMissingFieldToRename() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_missing", 1, 2, "nonExistent", "newField"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("differentField", "value")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            // Original field should still exist
            assertThat(result).hasStringField("differentField", "value");
            // New field should NOT be created (no source value)
            assertThat(result).doesNotHaveField("newField");
        }
    }

    @Nested
    @DisplayName("Multiple Renames")
    class MultipleRenames {

        @Test
        @DisplayName("applies multiple renames in sequence")
        void appliesMultipleRenamesInSequence() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(4))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_a", 1, 2, "field_a", "renamed_a"))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_b", 2, 3, "field_b", "renamed_b"))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_c", 3, 4, "field_c", "renamed_c"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("field_a", "value_a")
                    .put("field_b", "value_b")
                    .put("field_c", "value_c")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(4)
            );

            assertThat(result).hasStringField("renamed_a", "value_a");
            assertThat(result).hasStringField("renamed_b", "value_b");
            assertThat(result).hasStringField("renamed_c", "value_c");
            assertThat(result).doesNotHaveField("field_a");
            assertThat(result).doesNotHaveField("field_b");
            assertThat(result).doesNotHaveField("field_c");
        }

        @Test
        @DisplayName("applies chain rename (a->b->c)")
        void appliesChainRename() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(3))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_1", 1, 2, "originalName", "intermediateName"))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_2", 2, 3, "intermediateName", "finalName"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("originalName", "chain_value")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(3)
            );

            assertThat(result).hasStringField("finalName", "chain_value");
            assertThat(result).doesNotHaveField("originalName");
            assertThat(result).doesNotHaveField("intermediateName");
        }
    }

    @Nested
    @DisplayName("Rename Complex Values")
    class RenameComplexValues {

        @Test
        @DisplayName("renames field with nested object value")
        void renamesFieldWithNestedObjectValue() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_nested", 1, 2, "oldNestedField", "newNestedField"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .putObject("oldNestedField", nested -> nested
                            .put("innerKey", "innerValue")
                            .put("innerNumber", 42))
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasField("newNestedField");
            assertThat(result.get("newNestedField")).hasStringField("innerKey", "innerValue");
            assertThat(result.get("newNestedField")).hasIntField("innerNumber", 42);
            assertThat(result).doesNotHaveField("oldNestedField");
        }

        @Test
        @DisplayName("renames field with array value")
        void renamesFieldWithArrayValue() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.renameField(
                            GsonOps.INSTANCE, "rename_list", 1, 2, "oldListField", "newListField"))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .putStrings("oldListField", "item1", "item2", "item3")
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasField("newListField");
            assertThat(result).doesNotHaveField("oldListField");
        }
    }
}
