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

package de.splatgames.aether.datafixers.testkit.harness;

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DataFixTester")
class DataFixTesterTest {

    @Nested
    @DisplayName("apply()")
    class Apply {

        @Test
        @DisplayName("applies fix and returns result")
        void appliesFixAndReturnsResult() {
            final DataFix<JsonElement> fix = QuickFix.simple(
                    "add_field", 1, 2,
                    input -> input.set("added", input.createString("new"))
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            final Dynamic<JsonElement> result = DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType("player")
                    .apply();

            assertThat(result).hasStringField("name", "Alice");
            assertThat(result).hasStringField("added", "new");
        }

        @Test
        @DisplayName("throws if input not set")
        void throwsIfInputNotSet() {
            final DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);

            assertThatThrownBy(() -> DataFixTester.forFix(fix)
                    .forType("player")
                    .apply())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Input not set");
        }

        @Test
        @DisplayName("throws if type not set")
        void throwsIfTypeNotSet() {
            final DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
            final Dynamic<JsonElement> input = TestData.gson().object().build();

            assertThatThrownBy(() -> DataFixTester.forFix(fix)
                    .withInput(input)
                    .apply())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Type reference not set");
        }
    }

    @Nested
    @DisplayName("verify()")
    class Verify {

        @Test
        @DisplayName("passes when output matches expected")
        void passesWhenOutputMatchesExpected() {
            final DataFix<JsonElement> fix = QuickFix.addStringField(
                    GsonOps.INSTANCE,
                    "add_status", 1, 2,
                    "status", "active"
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            final Dynamic<JsonElement> expected = TestData.gson().object()
                    .put("name", "Alice")
                    .put("status", "active")
                    .build();

            // Should not throw
            DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType("player")
                    .expectOutput(expected)
                    .verify();
        }

        @Test
        @DisplayName("fails when output does not match expected")
        void failsWhenOutputDoesNotMatchExpected() {
            final DataFix<JsonElement> fix = QuickFix.addStringField(
                    GsonOps.INSTANCE,
                    "add_status", 1, 2,
                    "status", "active"
            );

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            final Dynamic<JsonElement> wrongExpected = TestData.gson().object()
                    .put("name", "Alice")
                    .put("status", "inactive")  // Wrong value
                    .build();

            assertThatThrownBy(() -> DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType("player")
                    .expectOutput(wrongExpected)
                    .verify())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("did not match expected");
        }

        @Test
        @DisplayName("returns verification result with context")
        void returnsVerificationResultWithContext() {
            final DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);

            final Dynamic<JsonElement> input = TestData.gson().object()
                    .put("name", "Alice")
                    .build();

            final DataFixTester.DataFixVerification<JsonElement> verification = DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType("player")
                    .recordingContext()
                    .verify();

            org.assertj.core.api.Assertions.assertThat(verification.passed()).isTrue();
            org.assertj.core.api.Assertions.assertThat(verification.context()).isNotNull();
            assertThat(verification.result()).hasStringField("name", "Alice");
        }
    }

    @Nested
    @DisplayName("Recording context")
    class RecordingContextTests {

        @Test
        @DisplayName("records context calls during fix execution")
        void recordsContextCallsDuringFixExecution() {
            final DataFix<JsonElement> fix = QuickFix.simple(
                    "logging_fix", 1, 2,
                    (input) -> input  // Identity - we'll test context separately
            );

            final Dynamic<JsonElement> input = TestData.gson().object().build();

            final DataFixTester.DataFixVerification<JsonElement> verification = DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType("player")
                    .recordingContext()
                    .verify();

            // Context should be accessible
            org.assertj.core.api.Assertions.assertThat(verification.context()).isNotNull();
        }

        @Test
        @DisplayName("assertNoWarnings() passes when no warnings")
        void assertNoWarningsPassesWhenNoWarnings() {
            final DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
            final Dynamic<JsonElement> input = TestData.gson().object().build();

            final DataFixTester.DataFixVerification<JsonElement> verification = DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType("player")
                    .recordingContext()
                    .verify();

            // Should not throw
            verification.assertNoWarnings();
        }

        @Test
        @DisplayName("assertNoLogs() passes when no logs")
        void assertNoLogsPassesWhenNoLogs() {
            final DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
            final Dynamic<JsonElement> input = TestData.gson().object().build();

            final DataFixTester.DataFixVerification<JsonElement> verification = DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType("player")
                    .recordingContext()
                    .verify();

            // Should not throw
            verification.assertNoLogs();
        }
    }

    @Nested
    @DisplayName("Fail on warning")
    class FailOnWarningTests {

        @Test
        @DisplayName("failOnWarning() context passes when no warnings")
        void failOnWarningContextPassesWhenNoWarnings() {
            final DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
            final Dynamic<JsonElement> input = TestData.gson().object().build();

            // Should not throw
            DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType("player")
                    .failOnWarning()
                    .apply();
        }
    }

    @Nested
    @DisplayName("Type reference")
    class TypeReferenceTests {

        @Test
        @DisplayName("accepts string type id")
        void acceptsStringTypeId() {
            final DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
            final Dynamic<JsonElement> input = TestData.gson().object().build();

            // Should not throw
            final Dynamic<JsonElement> result = DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType("player")
                    .apply();

            org.assertj.core.api.Assertions.assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("accepts TypeReference object")
        void acceptsTypeReferenceObject() {
            final DataFix<JsonElement> fix = QuickFix.identity("noop", 1, 2);
            final Dynamic<JsonElement> input = TestData.gson().object().build();

            // Should not throw
            final Dynamic<JsonElement> result = DataFixTester.forFix(fix)
                    .withInput(input)
                    .forType(new de.splatgames.aether.datafixers.api.TypeReference("player"))
                    .apply();

            org.assertj.core.api.Assertions.assertThat(result).isNotNull();
        }
    }
}
