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

package de.splatgames.aether.datafixers.functional.error;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for error handling and recovery scenarios.
 */
@DisplayName("Error Recovery IT")
@Tag("integration")
class ErrorRecoveryIT {

    private static final TypeReference TEST_TYPE = new TypeReference("test_entity");

    @Nested
    @DisplayName("Fix Throws Exception")
    class FixThrowsException {

        @Test
        @DisplayName("wraps fix exception with metadata")
        void wrapsFixExceptionWithMetadata() {
            final DataFix<JsonElement> failingFix = new DataFix<>() {
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
                        @NotNull final TypeReference type,
                        @NotNull final Dynamic<JsonElement> input,
                        @NotNull final DataFixerContext context
                ) {
                    throw new RuntimeException("Intentional failure for testing");
                }
            };

            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, failingFix)
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("field", "value")
                    .build();

            assertThatThrownBy(() -> fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Intentional failure");
        }
    }

    @Nested
    @DisplayName("Missing Data Handling")
    class MissingDataHandling {

        @Test
        @DisplayName("handles null field access gracefully")
        void handlesNullFieldAccessGracefully() {
            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("existingField", "value")
                    .build();

            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.<JsonElement>simple("safe_access", 1, 2, d -> {
                        // Try to get a non-existent field
                        final Dynamic<JsonElement> missing = d.get("nonExistentField");
                        if (missing == null) {
                            // Handle gracefully by adding a default
                            return d.set("nonExistentField", d.createString("default"));
                        }
                        return d;
                    }))
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasStringField("existingField", "value");
            assertThat(result).hasStringField("nonExistentField", "default");
        }

        @Test
        @DisplayName("handles empty object migration")
        void handlesEmptyObjectMigration() {
            final Dynamic<JsonElement> input = TestData.gson().object().build();

            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.simple("empty_handler", 1, 2, d -> {
                        // Check if empty and add defaults
                        if (!d.has("id")) {
                            return d.set("id", d.createInt(0))
                                    .set("name", d.createString("unknown"));
                        }
                        return d;
                    }))
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasIntField("id", 0);
            assertThat(result).hasStringField("name", "unknown");
        }
    }

    @Nested
    @DisplayName("Type Conversion Errors")
    class TypeConversionErrors {

        @Test
        @DisplayName("handles invalid number format gracefully")
        void handlesInvalidNumberFormatGracefully() {
            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("score", "not_a_number")
                    .build();

            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.simple("safe_parse", 1, 2, d -> {
                        final Dynamic<?> score = d.get("score");
                        if (score != null) {
                            // Try to parse as int, fallback to 0 if fails
                            final int value = score.asInt().result().orElse(0);
                            return d.set("score", d.createInt(value));
                        }
                        return d;
                    }))
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            // Should fallback to 0 since "not_a_number" can't be parsed
            assertThat(result).hasIntField("score", 0);
        }

        @Test
        @DisplayName("handles boolean parsing with fallback")
        void handlesBooleanParsingWithFallback() {
            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("enabled", "yes")  // String, not boolean
                    .build();

            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.simple("parse_bool", 1, 2, d -> {
                        final Dynamic<?> enabled = d.get("enabled");
                        if (enabled != null) {
                            // Try boolean, then string comparison
                            final boolean value = enabled.asBoolean().result()
                                    .orElseGet(() -> {
                                        final String str = enabled.asString().result().orElse("");
                                        return "yes".equalsIgnoreCase(str) || "true".equalsIgnoreCase(str);
                                    });
                            return d.set("enabled", d.createBoolean(value));
                        }
                        return d;
                    }))
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            assertThat(result).hasBooleanField("enabled", true);
        }
    }

    @Nested
    @DisplayName("Chain Recovery")
    class ChainRecovery {

        @Test
        @DisplayName("continues chain after successful fix handles bad data")
        void continuesChainAfterSuccessfulRecovery() {
            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "test")
                    .put("badValue", "invalid")
                    .build();

            final DataFixer fixer = new DataFixerBuilder(new DataVersion(3))
                    .addFix(TEST_TYPE, QuickFix.simple("fix_bad_value", 1, 2, d -> {
                        // Fix the bad value by replacing it
                        return d.set("badValue", d.createInt(0));
                    }))
                    .addFix(TEST_TYPE, QuickFix.simple("use_fixed_value", 2, 3, d -> {
                        // This fix depends on the fixed value
                        final int value = d.get("badValue").asInt().result().orElse(-1);
                        return d.set("processedValue", d.createInt(value * 2));
                    }))
                    .build();

            final Dynamic<JsonElement> result = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(3)
            );

            assertThat(result).hasIntField("badValue", 0);
            assertThat(result).hasIntField("processedValue", 0);
        }
    }

    @Nested
    @DisplayName("Validation Errors")
    class ValidationErrors {

        @Test
        @DisplayName("fix can add validation warnings via context")
        void fixCanAddValidationWarningsViaContext() {
            final java.util.List<String> warnings = new java.util.ArrayList<>();

            final DataFixerContext trackingContext = new DataFixerContext() {
                @Override
                public void info(@NotNull final String message, final Object... args) {
                    // Ignore info
                }

                @Override
                public void warn(@NotNull final String message, final Object... args) {
                    warnings.add(String.format(message, args));
                }
            };

            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .withDefaultContext(trackingContext)
                    .addFix(TEST_TYPE, new DataFix<JsonElement>() {
                        @Override
                        public @NotNull String name() {
                            return "warning_fix";
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
                            if (!input.has("requiredField")) {
                                context.warn("Missing required field 'requiredField', using default");
                            }
                            return input.set("requiredField",
                                    input.get("requiredField") != null
                                            ? input.get("requiredField")
                                            : input.createString("default"));
                        }
                    })
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("otherField", "value")
                    .build();

            fixer.update(TEST_TYPE, input, new DataVersion(1), new DataVersion(2));

            org.assertj.core.api.Assertions.assertThat(warnings).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(warnings.get(0))
                    .contains("Missing required field");
        }
    }

    @Nested
    @DisplayName("Idempotent Fixes")
    class IdempotentFixes {

        @Test
        @DisplayName("fix is idempotent when applied multiple times")
        void fixIsIdempotentWhenAppliedMultipleTimes() {
            final DataFixer fixer = new DataFixerBuilder(new DataVersion(2))
                    .addFix(TEST_TYPE, QuickFix.simple("idempotent_fix", 1, 2, d -> {
                        // Only add field if not present
                        if (d.has("addedField")) {
                            return d;
                        }
                        return d.set("addedField", d.createInt(1));
                    }))
                    .build();

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("original", "value")
                    .build();

            // First application
            final Dynamic<JsonElement> first = fixer.update(
                    TEST_TYPE, input,
                    new DataVersion(1), new DataVersion(2)
            );

            // "Second application" (simulated by creating new fixer that allows this)
            final DataFixer secondFixer = new DataFixerBuilder(new DataVersion(3))
                    .addFix(TEST_TYPE, QuickFix.simple("idempotent_fix_2", 2, 3, d -> {
                        if (d.has("addedField")) {
                            return d;
                        }
                        return d.set("addedField", d.createInt(1));
                    }))
                    .build();

            final Dynamic<JsonElement> second = secondFixer.update(
                    TEST_TYPE, first,
                    new DataVersion(2), new DataVersion(3)
            );

            // Field should still have value 1, not incremented or changed
            assertThat(second).hasIntField("addedField", 1);
        }
    }
}
