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

package de.splatgames.aether.datafixers.testkit.factory;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.context.AssertingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuickFix")
class QuickFixTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final AssertingContext CONTEXT = AssertingContext.silent();

    @Nested
    @DisplayName("simple()")
    class SimpleFix {

        @Test
        @DisplayName("creates fix with lambda transform")
        void createsFixWithLambdaTransform() {
            final DataFix<JsonElement> fix = QuickFix.simple(
                    "add_version", 1, 2,
                    input -> input.set("version", input.createInt(2))
            );

            assertThat(fix.name()).isEqualTo("add_version");
            assertThat(fix.fromVersion().getVersion()).isEqualTo(1);
            assertThat(fix.toVersion().getVersion()).isEqualTo(2);

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasIntField("version", 2);
            assertThat(result).hasStringField("name", "Alice");
        }
    }

    @Nested
    @DisplayName("renameField()")
    class RenameFieldFix {

        @Test
        @DisplayName("renames existing field")
        void renamesExistingField() {
            final DataFix<JsonElement> fix = QuickFix.renameField(
                    GsonOps.INSTANCE,
                    "rename_player_name", 1, 2,
                    "playerName", "name"
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("playerName", "Alice")
                    .put("level", 10)
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasStringField("name", "Alice");
            assertThat(result).doesNotHaveField("playerName");
            assertThat(result).hasIntField("level", 10);
        }

        @Test
        @DisplayName("leaves data unchanged if field missing")
        void leavesDataUnchangedIfFieldMissing() {
            final DataFix<JsonElement> fix = QuickFix.renameField(
                    GsonOps.INSTANCE,
                    "rename_missing", 1, 2,
                    "oldField", "newField"
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("otherField", "value")
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasStringField("otherField", "value");
            assertThat(result).doesNotHaveField("oldField");
            assertThat(result).doesNotHaveField("newField");
        }
    }

    @Nested
    @DisplayName("addStringField()")
    class AddStringFieldFix {

        @Test
        @DisplayName("adds field with default value")
        void addsFieldWithDefaultValue() {
            final DataFix<JsonElement> fix = QuickFix.addStringField(
                    GsonOps.INSTANCE,
                    "add_status", 1, 2,
                    "status", "active"
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasStringField("status", "active");
        }

        @Test
        @DisplayName("does not overwrite existing field")
        void doesNotOverwriteExistingField() {
            final DataFix<JsonElement> fix = QuickFix.addStringField(
                    GsonOps.INSTANCE,
                    "add_status", 1, 2,
                    "status", "default"
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("status", "custom")
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasStringField("status", "custom");
        }
    }

    @Nested
    @DisplayName("addIntField()")
    class AddIntFieldFix {

        @Test
        @DisplayName("adds int field with default value")
        void addsIntFieldWithDefaultValue() {
            final DataFix<JsonElement> fix = QuickFix.addIntField(
                    GsonOps.INSTANCE,
                    "add_score", 1, 2,
                    "score", 0
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasIntField("score", 0);
        }
    }

    @Nested
    @DisplayName("addBooleanField()")
    class AddBooleanFieldFix {

        @Test
        @DisplayName("adds boolean field with default value")
        void addsBooleanFieldWithDefaultValue() {
            final DataFix<JsonElement> fix = QuickFix.addBooleanField(
                    GsonOps.INSTANCE,
                    "add_active", 1, 2,
                    "active", true
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasBooleanField("active", true);
        }
    }

    @Nested
    @DisplayName("removeField()")
    class RemoveFieldFix {

        @Test
        @DisplayName("removes existing field")
        void removesExistingField() {
            final DataFix<JsonElement> fix = QuickFix.removeField(
                    GsonOps.INSTANCE,
                    "remove_legacy", 1, 2,
                    "legacyField"
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .put("legacyField", "old value")
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasStringField("name", "Alice");
            assertThat(result).doesNotHaveField("legacyField");
        }
    }

    @Nested
    @DisplayName("transformField()")
    class TransformFieldFix {

        @Test
        @DisplayName("transforms field value")
        void transformsFieldValue() {
            final DataFix<JsonElement> fix = QuickFix.transformField(
                    GsonOps.INSTANCE,
                    "uppercase_name", 1, 2,
                    "name",
                    d -> d.createString(d.asString().result().orElse("").toUpperCase())
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "alice")
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasStringField("name", "ALICE");
        }
    }

    @Nested
    @DisplayName("identity()")
    class IdentityFix {

        @Test
        @DisplayName("returns input unchanged")
        void returnsInputUnchanged() {
            final DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .put("level", 10)
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("conditional()")
    class ConditionalFix {

        @Test
        @DisplayName("applies transform when condition is true")
        void appliesTransformWhenConditionIsTrue() {
            final DataFix<JsonElement> fix = QuickFix.conditional(
                    "conditional_upgrade", 1, 2,
                    input -> input.get("level").asInt().result().orElse(0) > 5,
                    input -> input.set("rank", input.createString("veteran"))
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("level", 10)
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasStringField("rank", "veteran");
        }

        @Test
        @DisplayName("skips transform when condition is false")
        void skipsTransformWhenConditionIsFalse() {
            final DataFix<JsonElement> fix = QuickFix.conditional(
                    "conditional_upgrade", 1, 2,
                    input -> input.get("level").asInt().result().orElse(0) > 5,
                    input -> input.set("rank", input.createString("veteran"))
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("level", 3)
                    .build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).doesNotHaveField("rank");
        }
    }

    @Nested
    @DisplayName("compose()")
    class ComposeFix {

        @Test
        @DisplayName("applies multiple transforms in sequence")
        void appliesMultipleTransformsInSequence() {
            final DataFix<JsonElement> fix = QuickFix.compose(
                    "multi_transform", 1, 2,
                    input -> input.set("field1", input.createString("value1")),
                    input -> input.set("field2", input.createString("value2")),
                    input -> input.set("field3", input.createString("value3"))
            );

            final Dynamic<JsonElement> input = TestData.gson().object().build();

            final Dynamic<JsonElement> result = fix.apply(PLAYER, input, CONTEXT);

            assertThat(result).hasStringField("field1", "value1");
            assertThat(result).hasStringField("field2", "value2");
            assertThat(result).hasStringField("field3", "value3");
        }
    }
}
