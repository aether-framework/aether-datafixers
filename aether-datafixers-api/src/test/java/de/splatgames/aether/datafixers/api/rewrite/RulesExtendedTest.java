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

package de.splatgames.aether.datafixers.api.rewrite;

import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.optic.TestOps;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the extended rewrite rules added in v0.2.0.
 */
@DisplayName("Rules Extended")
class RulesExtendedTest {

    private static final TestOps OPS = TestOps.INSTANCE;

    // ==================== dynamicTransform Tests ====================

    @Nested
    @DisplayName("dynamicTransform")
    class DynamicTransformTests {

        @Test
        @DisplayName("should apply custom transformation")
        void shouldApplyCustomTransformation() {
            @SuppressWarnings("unchecked")
            final TypeRewriteRule rule = Rules.dynamicTransform("addField", OPS, dynamic -> {
                final Dynamic<Object> d = (Dynamic<Object>) dynamic;
                return d.set("newField", d.createString("added"));
            });

            final Typed<?> typed = createTyped(Map.of("name", "Alice"));

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "name", "Alice");
            assertResultHasField(result.get(), "newField", "added");
        }

        @Test
        @DisplayName("should compute derived field")
        void shouldComputeDerivedField() {
            @SuppressWarnings("unchecked")
            final TypeRewriteRule rule = Rules.dynamicTransform("computeLevel", OPS, dynamic -> {
                final Dynamic<Object> d = (Dynamic<Object>) dynamic;
                final int xp = d.get("xp").asInt().result().orElse(0);
                final int level = Math.max(1, (int) Math.sqrt(xp / 100.0));
                return d.set("level", d.createInt(level));
            });

            final Typed<?> typed = createTyped(Map.of("xp", 2500));

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "level", 5);
        }
    }

    // ==================== setField Tests ====================

    @Nested
    @DisplayName("setField")
    class SetFieldTests {

        @Test
        @DisplayName("should set field when missing")
        void shouldSetFieldWhenMissing() {
            final Dynamic<Object> value = new Dynamic<>(OPS, 2);
            final TypeRewriteRule rule = Rules.setField(OPS, "version", value);

            final Typed<?> typed = createTyped(Map.of("name", "Alice"));

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "version", 2);
        }

        @Test
        @DisplayName("should overwrite existing field")
        void shouldOverwriteExistingField() {
            final Dynamic<Object> value = new Dynamic<>(OPS, 2);
            final TypeRewriteRule rule = Rules.setField(OPS, "version", value);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Alice");
            input.put("version", 1);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "version", 2);
        }
    }

    // ==================== renameFields Tests ====================

    @Nested
    @DisplayName("renameFields")
    class RenameFieldsTests {

        @Test
        @DisplayName("should rename multiple fields")
        void shouldRenameMultipleFields() {
            final TypeRewriteRule rule = Rules.renameFields(OPS, Map.of(
                    "playerName", "name",
                    "xp", "experience"
            ));

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("playerName", "Steve");
            input.put("xp", 1500);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "name", "Steve");
            assertResultHasField(result.get(), "experience", 1500);
            assertResultDoesNotHaveField(result.get(), "playerName");
            assertResultDoesNotHaveField(result.get(), "xp");
        }

        @Test
        @DisplayName("should return identity for empty renames")
        void shouldReturnIdentityForEmptyRenames() {
            final TypeRewriteRule rule = Rules.renameFields(OPS, Map.of());

            assertThat(rule).isSameAs(TypeRewriteRule.identity());
        }
    }

    // ==================== removeFields Tests ====================

    @Nested
    @DisplayName("removeFields")
    class RemoveFieldsTests {

        @Test
        @DisplayName("should remove multiple fields")
        void shouldRemoveMultipleFields() {
            final TypeRewriteRule rule = Rules.removeFields(OPS, "deprecated1", "deprecated2");

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Alice");
            input.put("deprecated1", true);
            input.put("deprecated2", "old");
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "name", "Alice");
            assertResultDoesNotHaveField(result.get(), "deprecated1");
            assertResultDoesNotHaveField(result.get(), "deprecated2");
        }

        @Test
        @DisplayName("should return identity for empty array")
        void shouldReturnIdentityForEmptyArray() {
            final TypeRewriteRule rule = Rules.removeFields(OPS);

            assertThat(rule).isSameAs(TypeRewriteRule.identity());
        }
    }

    // ==================== groupFields Tests ====================

    @Nested
    @DisplayName("groupFields")
    class GroupFieldsTests {

        @Test
        @DisplayName("should group fields into nested object")
        void shouldGroupFieldsIntoNestedObject() {
            final TypeRewriteRule rule = Rules.groupFields(OPS, "position", "x", "y", "z");

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Steve");
            input.put("x", 100.5);
            input.put("y", 64.0);
            input.put("z", -200.25);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "name", "Steve");
            assertResultDoesNotHaveField(result.get(), "x");
            assertResultDoesNotHaveField(result.get(), "y");
            assertResultDoesNotHaveField(result.get(), "z");

            final Map<String, Object> resultMap = getResultMap(result.get());
            assertThat(resultMap).containsKey("position");

            @SuppressWarnings("unchecked")
            final Map<String, Object> position = (Map<String, Object>) resultMap.get("position");
            assertThat(position).containsEntry("x", 100.5);
            assertThat(position).containsEntry("y", 64.0);
            assertThat(position).containsEntry("z", -200.25);
        }

        @Test
        @DisplayName("should return identity for empty source fields")
        void shouldReturnIdentityForEmptySourceFields() {
            final TypeRewriteRule rule = Rules.groupFields(OPS, "position");

            assertThat(rule).isSameAs(TypeRewriteRule.identity());
        }
    }

    // ==================== flattenField Tests ====================

    @Nested
    @DisplayName("flattenField")
    class FlattenFieldTests {

        @Test
        @DisplayName("should flatten nested object to root")
        void shouldFlattenNestedObjectToRoot() {
            final TypeRewriteRule rule = Rules.flattenField(OPS, "position");

            final Map<String, Object> position = new LinkedHashMap<>();
            position.put("x", 100.5);
            position.put("y", 64.0);
            position.put("z", -200.25);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Steve");
            input.put("position", position);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "name", "Steve");
            assertResultHasField(result.get(), "x", 100.5);
            assertResultHasField(result.get(), "y", 64.0);
            assertResultHasField(result.get(), "z", -200.25);
            assertResultDoesNotHaveField(result.get(), "position");
        }

        @Test
        @DisplayName("should not change data if field is missing")
        void shouldNotChangeDataIfFieldIsMissing() {
            final TypeRewriteRule rule = Rules.flattenField(OPS, "position");

            final Typed<?> typed = createTyped(Map.of("name", "Steve"));

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "name", "Steve");
        }
    }

    // ==================== moveField Tests ====================

    @Nested
    @DisplayName("moveField")
    class MoveFieldTests {

        @Test
        @DisplayName("should move field to nested path")
        void shouldMoveFieldToNestedPath() {
            final TypeRewriteRule rule = Rules.moveField(OPS, "x", "position.x");

            final Map<String, Object> position = new LinkedHashMap<>();
            position.put("y", 64.0);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Steve");
            input.put("x", 100.5);
            input.put("position", position);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultDoesNotHaveField(result.get(), "x");

            final Map<String, Object> resultMap = getResultMap(result.get());
            @SuppressWarnings("unchecked")
            final Map<String, Object> resultPosition = (Map<String, Object>) resultMap.get("position");
            assertThat(resultPosition).containsEntry("x", 100.5);
            assertThat(resultPosition).containsEntry("y", 64.0);
        }

        @Test
        @DisplayName("should not change data if source missing")
        void shouldNotChangeDataIfSourceMissing() {
            final TypeRewriteRule rule = Rules.moveField(OPS, "missing", "position.x");

            final Typed<?> typed = createTyped(Map.of("name", "Steve"));

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "name", "Steve");
        }
    }

    // ==================== copyField Tests ====================

    @Nested
    @DisplayName("copyField")
    class CopyFieldTests {

        @Test
        @DisplayName("should copy field preserving original")
        void shouldCopyFieldPreservingOriginal() {
            final TypeRewriteRule rule = Rules.copyField(OPS, "name", "displayName");

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Steve");
            input.put("level", 10);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "name", "Steve");
            assertResultHasField(result.get(), "displayName", "Steve");
            assertResultHasField(result.get(), "level", 10);
        }
    }

    // ==================== transformFieldAt Tests ====================

    @Nested
    @DisplayName("transformFieldAt")
    class TransformFieldAtTests {

        @Test
        @DisplayName("should transform nested field")
        void shouldTransformNestedField() {
            final TypeRewriteRule rule = Rules.transformFieldAt(OPS, "position.x",
                    d -> d.createDouble(d.asDouble().result().orElse(0.0) * 2));

            final Map<String, Object> position = new LinkedHashMap<>();
            position.put("x", 100.0);
            position.put("y", 64.0);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("position", position);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            final Map<String, Object> resultMap = getResultMap(result.get());
            @SuppressWarnings("unchecked")
            final Map<String, Object> resultPosition = (Map<String, Object>) resultMap.get("position");
            assertThat(resultPosition.get("x")).isEqualTo(200.0);
        }
    }

    // ==================== renameFieldAt Tests ====================

    @Nested
    @DisplayName("renameFieldAt")
    class RenameFieldAtTests {

        @Test
        @DisplayName("should rename nested field")
        void shouldRenameNestedField() {
            final TypeRewriteRule rule = Rules.renameFieldAt(OPS, "position.posX", "x");

            final Map<String, Object> position = new LinkedHashMap<>();
            position.put("posX", 100.0);
            position.put("y", 64.0);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("position", position);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            final Map<String, Object> resultMap = getResultMap(result.get());
            @SuppressWarnings("unchecked")
            final Map<String, Object> resultPosition = (Map<String, Object>) resultMap.get("position");
            assertThat(resultPosition).containsEntry("x", 100.0);
            assertThat(resultPosition).doesNotContainKey("posX");
        }

        @Test
        @DisplayName("should delegate to renameField for simple path")
        void shouldDelegateToRenameFieldForSimplePath() {
            final TypeRewriteRule rule = Rules.renameFieldAt(OPS, "oldName", "newName");

            final Typed<?> typed = createTyped(Map.of("oldName", "value"));

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "newName", "value");
            assertResultDoesNotHaveField(result.get(), "oldName");
        }
    }

    // ==================== removeFieldAt Tests ====================

    @Nested
    @DisplayName("removeFieldAt")
    class RemoveFieldAtTests {

        @Test
        @DisplayName("should remove nested field")
        void shouldRemoveNestedField() {
            final TypeRewriteRule rule = Rules.removeFieldAt(OPS, "metadata.deprecated");

            final Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("deprecated", true);
            metadata.put("version", 1);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("data", "value");
            input.put("metadata", metadata);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            final Map<String, Object> resultMap = getResultMap(result.get());
            @SuppressWarnings("unchecked")
            final Map<String, Object> resultMetadata = (Map<String, Object>) resultMap.get("metadata");
            assertThat(resultMetadata).doesNotContainKey("deprecated");
            assertThat(resultMetadata).containsEntry("version", 1);
        }
    }

    // ==================== addFieldAt Tests ====================

    @Nested
    @DisplayName("addFieldAt")
    class AddFieldAtTests {

        @Test
        @DisplayName("should add nested field")
        void shouldAddNestedField() {
            final Dynamic<Object> defaultValue = new Dynamic<>(OPS, 0.0);
            final TypeRewriteRule rule = Rules.addFieldAt(OPS, "position.w", defaultValue);

            final Map<String, Object> position = new LinkedHashMap<>();
            position.put("x", 100.0);
            position.put("y", 64.0);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("position", position);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            final Map<String, Object> resultMap = getResultMap(result.get());
            @SuppressWarnings("unchecked")
            final Map<String, Object> resultPosition = (Map<String, Object>) resultMap.get("position");
            assertThat(resultPosition).containsEntry("w", 0.0);
        }

        @Test
        @DisplayName("should not overwrite existing field")
        void shouldNotOverwriteExistingField() {
            final Dynamic<Object> defaultValue = new Dynamic<>(OPS, 0.0);
            final TypeRewriteRule rule = Rules.addFieldAt(OPS, "position.x", defaultValue);

            final Map<String, Object> position = new LinkedHashMap<>();
            position.put("x", 100.0);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("position", position);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            final Map<String, Object> resultMap = getResultMap(result.get());
            @SuppressWarnings("unchecked")
            final Map<String, Object> resultPosition = (Map<String, Object>) resultMap.get("position");
            assertThat(resultPosition.get("x")).isEqualTo(100.0);
        }
    }

    // ==================== ifFieldExists Tests ====================

    @Nested
    @DisplayName("ifFieldExists")
    class IfFieldExistsTests {

        @Test
        @DisplayName("should execute rule when field exists")
        void shouldExecuteRuleWhenFieldExists() {
            final TypeRewriteRule innerRule = Rules.removeField(OPS, "legacy");
            final TypeRewriteRule rule = Rules.ifFieldExists(OPS, "legacy", innerRule);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Alice");
            input.put("legacy", true);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultDoesNotHaveField(result.get(), "legacy");
        }

        @Test
        @DisplayName("should not execute rule when field missing")
        void shouldNotExecuteRuleWhenFieldMissing() {
            final TypeRewriteRule innerRule = Rules.removeField(OPS, "name");
            final TypeRewriteRule rule = Rules.ifFieldExists(OPS, "legacy", innerRule);

            final Typed<?> typed = createTyped(Map.of("name", "Alice"));

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "name", "Alice");
        }
    }

    // ==================== ifFieldMissing Tests ====================

    @Nested
    @DisplayName("ifFieldMissing")
    class IfFieldMissingTests {

        @Test
        @DisplayName("should execute rule when field missing")
        void shouldExecuteRuleWhenFieldMissing() {
            final Dynamic<Object> defaultValue = new Dynamic<>(OPS, 1);
            final TypeRewriteRule innerRule = Rules.addField(OPS, "version", defaultValue);
            final TypeRewriteRule rule = Rules.ifFieldMissing(OPS, "version", innerRule);

            final Typed<?> typed = createTyped(Map.of("name", "Alice"));

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "version", 1);
        }

        @Test
        @DisplayName("should not execute rule when field exists")
        void shouldNotExecuteRuleWhenFieldExists() {
            final Dynamic<Object> defaultValue = new Dynamic<>(OPS, 999);
            final TypeRewriteRule innerRule = Rules.setField(OPS, "version", defaultValue);
            final TypeRewriteRule rule = Rules.ifFieldMissing(OPS, "version", innerRule);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Alice");
            input.put("version", 1);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "version", 1);
        }
    }

    // ==================== ifFieldEquals Tests ====================

    @Nested
    @DisplayName("ifFieldEquals")
    class IfFieldEqualsTests {

        @Test
        @DisplayName("should execute rule when field equals integer")
        void shouldExecuteRuleWhenFieldEqualsInteger() {
            final Dynamic<Object> newValue = new Dynamic<>(OPS, 2);
            final TypeRewriteRule innerRule = Rules.setField(OPS, "version", newValue);
            final TypeRewriteRule rule = Rules.ifFieldEquals(OPS, "version", 1, innerRule);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Alice");
            input.put("version", 1);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "version", 2);
        }

        @Test
        @DisplayName("should execute rule when field equals string")
        void shouldExecuteRuleWhenFieldEqualsString() {
            final TypeRewriteRule innerRule = Rules.renameField(OPS, "status", "state");
            final TypeRewriteRule rule = Rules.ifFieldEquals(OPS, "status", "active", innerRule);

            final Typed<?> typed = createTyped(Map.of("status", "active"));

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "state", "active");
        }

        @Test
        @DisplayName("should not execute rule when field does not equal")
        void shouldNotExecuteRuleWhenFieldDoesNotEqual() {
            final Dynamic<Object> newValue = new Dynamic<>(OPS, 2);
            final TypeRewriteRule innerRule = Rules.setField(OPS, "version", newValue);
            final TypeRewriteRule rule = Rules.ifFieldEquals(OPS, "version", 1, innerRule);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Alice");
            input.put("version", 5);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultHasField(result.get(), "version", 5);
        }

        @Test
        @DisplayName("should execute rule when field equals boolean")
        void shouldExecuteRuleWhenFieldEqualsBoolean() {
            final TypeRewriteRule innerRule = Rules.removeField(OPS, "active");
            final TypeRewriteRule rule = Rules.ifFieldEquals(OPS, "active", true, innerRule);

            final Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "Alice");
            input.put("active", true);
            final Typed<?> typed = createTyped(input);

            final Optional<Typed<?>> result = rule.rewrite(typed.type(), typed);

            assertThat(result).isPresent();
            assertResultDoesNotHaveField(result.get(), "active");
        }
    }

    // ==================== Helper Methods ====================

    private static Typed<?> createTyped(final Map<String, Object> map) {
        final Dynamic<Object> dynamic = new Dynamic<>(OPS, new LinkedHashMap<>(map));
        return new Typed<>(Type.PASSTHROUGH, dynamic);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getResultMap(final Typed<?> typed) {
        final Dynamic<?> dynamic = (Dynamic<?>) typed.value();
        return (Map<String, Object>) dynamic.value();
    }

    private static void assertResultHasField(final Typed<?> typed, final String field, final Object expected) {
        final Map<String, Object> map = getResultMap(typed);
        assertThat(map).containsEntry(field, expected);
    }

    private static void assertResultDoesNotHaveField(final Typed<?> typed, final String field) {
        final Map<String, Object> map = getResultMap(typed);
        assertThat(map).doesNotContainKey(field);
    }
}
