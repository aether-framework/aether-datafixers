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

package de.splatgames.aether.datafixers.schematools.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ConventionRules}.
 */
@DisplayName("ConventionRules")
class ConventionRulesTest {

    @Nested
    @DisplayName("Predefined Rule Sets")
    class PredefinedRuleSets {

        @Test
        @DisplayName("STRICT is enabled")
        void strictIsEnabled() {
            assertThat(ConventionRules.STRICT.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("STRICT treats violations as errors")
        void strictTreatsViolationsAsErrors() {
            assertThat(ConventionRules.STRICT.treatViolationsAsErrors()).isTrue();
        }

        @Test
        @DisplayName("STRICT validates snake_case type names")
        void strictValidatesSnakeCaseTypeNames() {
            assertThat(ConventionRules.STRICT.isValidTypeName("player_data")).isTrue();
            assertThat(ConventionRules.STRICT.isValidTypeName("player")).isTrue();
            assertThat(ConventionRules.STRICT.isValidTypeName("PlayerData")).isFalse();
            assertThat(ConventionRules.STRICT.isValidTypeName("PLAYER_DATA")).isFalse();
        }

        @Test
        @DisplayName("STRICT validates snake_case field names")
        void strictValidatesSnakeCaseFieldNames() {
            assertThat(ConventionRules.STRICT.isValidFieldName("player_name")).isTrue();
            assertThat(ConventionRules.STRICT.isValidFieldName("playerName")).isFalse();
        }

        @Test
        @DisplayName("STRICT requires Schema suffix")
        void strictRequiresSchemaSuffix() {
            assertThat(ConventionRules.STRICT.isValidSchemaClassName("PlayerSchema")).isTrue();
            assertThat(ConventionRules.STRICT.isValidSchemaClassName("PlayerData")).isFalse();
        }

        @Test
        @DisplayName("STRICT requires Fix suffix")
        void strictRequiresFixSuffix() {
            assertThat(ConventionRules.STRICT.isValidFixClassName("RenamePlayerFix")).isTrue();
            assertThat(ConventionRules.STRICT.isValidFixClassName("RenamePlayer")).isFalse();
        }

        @Test
        @DisplayName("RELAXED is enabled")
        void relaxedIsEnabled() {
            assertThat(ConventionRules.RELAXED.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("RELAXED treats violations as warnings")
        void relaxedTreatsViolationsAsWarnings() {
            assertThat(ConventionRules.RELAXED.treatViolationsAsErrors()).isFalse();
        }

        @Test
        @DisplayName("RELAXED allows more flexible naming")
        void relaxedAllowsMoreFlexibleNaming() {
            assertThat(ConventionRules.RELAXED.isValidTypeName("PlayerData")).isTrue();
            assertThat(ConventionRules.RELAXED.isValidTypeName("player_data")).isTrue();
            assertThat(ConventionRules.RELAXED.isValidTypeName("123invalid")).isFalse();
        }

        @Test
        @DisplayName("NONE is disabled")
        void noneIsDisabled() {
            assertThat(ConventionRules.NONE.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("NONE accepts everything")
        void noneAcceptsEverything() {
            assertThat(ConventionRules.NONE.isValidTypeName("anything")).isTrue();
            assertThat(ConventionRules.NONE.isValidFieldName("ANYTHING")).isTrue();
            assertThat(ConventionRules.NONE.isValidSchemaClassName("NoSuffix")).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds with defaults")
        void buildsWithDefaults() {
            final ConventionRules rules = ConventionRules.builder().build();

            assertThat(rules.isEnabled()).isTrue();
            assertThat(rules.treatViolationsAsErrors()).isFalse();
        }

        @Test
        @DisplayName("enabled() sets enabled flag")
        void enabledSetsEnabledFlag() {
            final ConventionRules rules = ConventionRules.builder()
                    .enabled(false)
                    .build();

            assertThat(rules.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("typeNamePattern() sets pattern")
        void typeNamePatternSetsPattern() {
            final Pattern pattern = Pattern.compile("^entity_[a-z]+$");
            final ConventionRules rules = ConventionRules.builder()
                    .typeNamePattern(pattern)
                    .build();

            assertThat(rules.typeNamePattern()).isSameAs(pattern);
            assertThat(rules.isValidTypeName("entity_player")).isTrue();
            assertThat(rules.isValidTypeName("player")).isFalse();
        }

        @Test
        @DisplayName("fieldNamePattern() sets pattern")
        void fieldNamePatternSetsPattern() {
            final Pattern pattern = Pattern.compile("^[a-z]+$");
            final ConventionRules rules = ConventionRules.builder()
                    .fieldNamePattern(pattern)
                    .build();

            assertThat(rules.fieldNamePattern()).isSameAs(pattern);
            assertThat(rules.isValidFieldName("name")).isTrue();
            assertThat(rules.isValidFieldName("player_name")).isFalse();
        }

        @Test
        @DisplayName("requireTypePrefix() sets prefix requirement")
        void requireTypePrefixSetsPrefixRequirement() {
            final ConventionRules rules = ConventionRules.builder()
                    .requireTypePrefix("game_")
                    .build();

            assertThat(rules.typeNamePrefix()).isEqualTo("game_");
            assertThat(rules.isValidTypeName("game_player")).isTrue();
            assertThat(rules.isValidTypeName("player")).isFalse();
        }

        @Test
        @DisplayName("schemaClassSuffix() sets suffix")
        void schemaClassSuffixSetsSuffix() {
            final ConventionRules rules = ConventionRules.builder()
                    .schemaClassSuffix("Definition")
                    .build();

            assertThat(rules.schemaClassSuffix()).isEqualTo("Definition");
            assertThat(rules.isValidSchemaClassName("PlayerDefinition")).isTrue();
            assertThat(rules.isValidSchemaClassName("PlayerSchema")).isFalse();
        }

        @Test
        @DisplayName("fixClassSuffix() sets suffix")
        void fixClassSuffixSetsSuffix() {
            final ConventionRules rules = ConventionRules.builder()
                    .fixClassSuffix("Migration")
                    .build();

            assertThat(rules.fixClassSuffix()).isEqualTo("Migration");
            assertThat(rules.isValidFixClassName("PlayerMigration")).isTrue();
            assertThat(rules.isValidFixClassName("PlayerFix")).isFalse();
        }

        @Test
        @DisplayName("treatViolationsAsErrors() sets flag")
        void treatViolationsAsErrorsSetsFlag() {
            final ConventionRules rules = ConventionRules.builder()
                    .treatViolationsAsErrors(true)
                    .build();

            assertThat(rules.treatViolationsAsErrors()).isTrue();
        }

        @Test
        @DisplayName("customTypeValidator() sets validator")
        void customTypeValidatorSetsValidator() {
            final ConventionRules rules = ConventionRules.builder()
                    .customTypeValidator(name -> name.length() > 3)
                    .build();

            assertThat(rules.isValidTypeName("abcd")).isTrue();
            assertThat(rules.isValidTypeName("abc")).isFalse();
        }

        @Test
        @DisplayName("customFieldValidator() sets validator")
        void customFieldValidatorSetsValidator() {
            final ConventionRules rules = ConventionRules.builder()
                    .customFieldValidator(name -> !name.startsWith("_"))
                    .build();

            assertThat(rules.isValidFieldName("name")).isTrue();
            assertThat(rules.isValidFieldName("_internal")).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidTypeName()")
    class IsValidTypeNameMethod {

        @Test
        @DisplayName("returns true when disabled")
        void returnsTrueWhenDisabled() {
            final ConventionRules rules = ConventionRules.builder()
                    .enabled(false)
                    .typeNamePattern(Pattern.compile("^never_match$"))
                    .build();

            assertThat(rules.isValidTypeName("anything")).isTrue();
        }

        @Test
        @DisplayName("throws on null type name")
        void throwsOnNullTypeName() {
            assertThatThrownBy(() -> ConventionRules.STRICT.isValidTypeName(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("validates prefix before pattern")
        void validatesPrefixBeforePattern() {
            final ConventionRules rules = ConventionRules.builder()
                    .requireTypePrefix("entity_")
                    .typeNamePattern(Pattern.compile("^entity_[a-z]+$"))
                    .build();

            assertThat(rules.isValidTypeName("entity_player")).isTrue();
            assertThat(rules.isValidTypeName("player")).isFalse();
        }

        @Test
        @DisplayName("validates pattern before custom validator")
        void validatesPatternBeforeCustomValidator() {
            final ConventionRules rules = ConventionRules.builder()
                    .typeNamePattern(Pattern.compile("^[a-z]+$"))
                    .customTypeValidator(name -> name.length() > 2)
                    .build();

            assertThat(rules.isValidTypeName("abc")).isTrue();
            assertThat(rules.isValidTypeName("AB")).isFalse(); // Fails pattern
            assertThat(rules.isValidTypeName("ab")).isFalse(); // Fails custom
        }
    }

    @Nested
    @DisplayName("isValidFieldName()")
    class IsValidFieldNameMethod {

        @Test
        @DisplayName("returns true when disabled")
        void returnsTrueWhenDisabled() {
            final ConventionRules rules = ConventionRules.builder()
                    .enabled(false)
                    .build();

            assertThat(rules.isValidFieldName("ANYTHING")).isTrue();
        }

        @Test
        @DisplayName("throws on null field name")
        void throwsOnNullFieldName() {
            assertThatThrownBy(() -> ConventionRules.STRICT.isValidFieldName(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("validates pattern")
        void validatesPattern() {
            final ConventionRules rules = ConventionRules.builder()
                    .fieldNamePattern(Pattern.compile("^[a-z][a-zA-Z0-9]*$"))
                    .build();

            assertThat(rules.isValidFieldName("playerName")).isTrue();
            assertThat(rules.isValidFieldName("PlayerName")).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidSchemaClassName()")
    class IsValidSchemaClassNameMethod {

        @Test
        @DisplayName("returns true when disabled")
        void returnsTrueWhenDisabled() {
            final ConventionRules rules = ConventionRules.builder()
                    .enabled(false)
                    .schemaClassSuffix("Schema")
                    .build();

            assertThat(rules.isValidSchemaClassName("NoSuffix")).isTrue();
        }

        @Test
        @DisplayName("returns true when no suffix configured")
        void returnsTrueWhenNoSuffixConfigured() {
            final ConventionRules rules = ConventionRules.builder().build();

            assertThat(rules.isValidSchemaClassName("AnythingGoes")).isTrue();
        }

        @Test
        @DisplayName("throws on null class name")
        void throwsOnNullClassName() {
            assertThatThrownBy(() -> ConventionRules.STRICT.isValidSchemaClassName(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("isValidFixClassName()")
    class IsValidFixClassNameMethod {

        @Test
        @DisplayName("returns true when disabled")
        void returnsTrueWhenDisabled() {
            final ConventionRules rules = ConventionRules.builder()
                    .enabled(false)
                    .fixClassSuffix("Fix")
                    .build();

            assertThat(rules.isValidFixClassName("NoSuffix")).isTrue();
        }

        @Test
        @DisplayName("returns true when no suffix configured")
        void returnsTrueWhenNoSuffixConfigured() {
            final ConventionRules rules = ConventionRules.builder().build();

            assertThat(rules.isValidFixClassName("AnythingGoes")).isTrue();
        }

        @Test
        @DisplayName("throws on null class name")
        void throwsOnNullClassName() {
            assertThatThrownBy(() -> ConventionRules.STRICT.isValidFixClassName(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles empty type name")
        void handlesEmptyTypeName() {
            assertThat(ConventionRules.STRICT.isValidTypeName("")).isFalse();
        }

        @Test
        @DisplayName("handles empty field name")
        void handlesEmptyFieldName() {
            assertThat(ConventionRules.STRICT.isValidFieldName("")).isFalse();
        }

        @Test
        @DisplayName("handles unicode characters")
        void handlesUnicodeCharacters() {
            // Strict only allows lowercase letters and underscores
            assertThat(ConventionRules.STRICT.isValidTypeName("名前")).isFalse();
        }

        @Test
        @DisplayName("prefix check is case sensitive")
        void prefixCheckIsCaseSensitive() {
            final ConventionRules rules = ConventionRules.builder()
                    .requireTypePrefix("Entity_")
                    .build();

            assertThat(rules.isValidTypeName("Entity_Player")).isTrue();
            assertThat(rules.isValidTypeName("entity_player")).isFalse();
        }

        @Test
        @DisplayName("multiple validations all must pass")
        void multipleValidationsAllMustPass() {
            final ConventionRules rules = ConventionRules.builder()
                    .requireTypePrefix("game_")
                    .typeNamePattern(Pattern.compile("^game_[a-z_]+$"))
                    .customTypeValidator(name -> name.length() < 20)
                    .build();

            assertThat(rules.isValidTypeName("game_player")).isTrue();
            assertThat(rules.isValidTypeName("player")).isFalse();  // No prefix
            assertThat(rules.isValidTypeName("game_Player")).isFalse();  // Uppercase
            assertThat(rules.isValidTypeName("game_very_long_name_here")).isFalse();  // Too long
        }
    }
}
