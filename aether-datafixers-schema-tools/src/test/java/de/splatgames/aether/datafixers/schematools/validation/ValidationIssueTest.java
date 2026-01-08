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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ValidationIssue}.
 */
@DisplayName("ValidationIssue")
class ValidationIssueTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("error() creates ERROR severity issue")
        void errorCreatesErrorSeverityIssue() {
            final ValidationIssue issue = ValidationIssue.error("CODE_001", "Error message");

            assertThat(issue.severity()).isEqualTo(IssueSeverity.ERROR);
            assertThat(issue.code()).isEqualTo("CODE_001");
            assertThat(issue.message()).isEqualTo("Error message");
            assertThat(issue.isError()).isTrue();
            assertThat(issue.isWarning()).isFalse();
            assertThat(issue.isInfo()).isFalse();
        }

        @Test
        @DisplayName("warning() creates WARNING severity issue")
        void warningCreatesWarningSeverityIssue() {
            final ValidationIssue issue = ValidationIssue.warning("CODE_002", "Warning message");

            assertThat(issue.severity()).isEqualTo(IssueSeverity.WARNING);
            assertThat(issue.isWarning()).isTrue();
            assertThat(issue.isError()).isFalse();
            assertThat(issue.isInfo()).isFalse();
        }

        @Test
        @DisplayName("info() creates INFO severity issue")
        void infoCreatesInfoSeverityIssue() {
            final ValidationIssue issue = ValidationIssue.info("CODE_003", "Info message");

            assertThat(issue.severity()).isEqualTo(IssueSeverity.INFO);
            assertThat(issue.isInfo()).isTrue();
            assertThat(issue.isError()).isFalse();
            assertThat(issue.isWarning()).isFalse();
        }

        @Test
        @DisplayName("throws on null code")
        void throwsOnNullCode() {
            assertThatThrownBy(() -> ValidationIssue.error(null, "message"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null message")
        void throwsOnNullMessage() {
            assertThatThrownBy(() -> ValidationIssue.error("CODE", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("at() method")
    class AtMethod {

        @Test
        @DisplayName("adds location to issue")
        void addsLocationToIssue() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message")
                    .at("Schema@100");

            assertThat(issue.location()).contains("Schema@100");
        }

        @Test
        @DisplayName("returns new immutable instance")
        void returnsNewImmutableInstance() {
            final ValidationIssue original = ValidationIssue.error("CODE", "message");
            final ValidationIssue withLocation = original.at("location");

            assertThat(original.location()).isEmpty();
            assertThat(withLocation.location()).contains("location");
            assertThat(original).isNotSameAs(withLocation);
        }

        @Test
        @DisplayName("throws on null location")
        void throwsOnNullLocation() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");

            assertThatThrownBy(() -> issue.at(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("withContext() method")
    class WithContextMethod {

        @Test
        @DisplayName("adds context to issue")
        void addsContextToIssue() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message")
                    .withContext("key", "value");

            assertThat(issue.context()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("allows multiple context entries")
        void allowsMultipleContextEntries() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message")
                    .withContext("key1", "value1")
                    .withContext("key2", 42);

            assertThat(issue.context())
                    .containsEntry("key1", "value1")
                    .containsEntry("key2", 42);
        }

        @Test
        @DisplayName("returns new immutable instance")
        void returnsNewImmutableInstance() {
            final ValidationIssue original = ValidationIssue.error("CODE", "message");
            final ValidationIssue withContext = original.withContext("key", "value");

            assertThat(original.context()).isEmpty();
            assertThat(withContext.context()).containsEntry("key", "value");
        }

        @Test
        @DisplayName("throws on null key")
        void throwsOnNullKey() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");

            assertThatThrownBy(() -> issue.withContext(null, "value"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("throws on null value")
        void throwsOnNullValue() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");

            assertThatThrownBy(() -> issue.withContext("key", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Accessors")
    class Accessors {

        @Test
        @DisplayName("severity() returns correct severity")
        void severityReturnsCorrectSeverity() {
            final ValidationIssue issue = ValidationIssue.warning("CODE", "message");
            assertThat(issue.severity()).isEqualTo(IssueSeverity.WARNING);
        }

        @Test
        @DisplayName("code() returns correct code")
        void codeReturnsCorrectCode() {
            final ValidationIssue issue = ValidationIssue.error("MY_CODE", "message");
            assertThat(issue.code()).isEqualTo("MY_CODE");
        }

        @Test
        @DisplayName("message() returns correct message")
        void messageReturnsCorrectMessage() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "My error message");
            assertThat(issue.message()).isEqualTo("My error message");
        }

        @Test
        @DisplayName("location() returns empty when not set")
        void locationReturnsEmptyWhenNotSet() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");
            assertThat(issue.location()).isEmpty();
        }

        @Test
        @DisplayName("context() returns immutable map")
        void contextReturnsImmutableMap() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message")
                    .withContext("key", "value");

            assertThatThrownBy(() -> issue.context().put("new", "entry"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @Test
        @DisplayName("equals returns true for identical issues")
        void equalsReturnsTrueForIdenticalIssues() {
            final ValidationIssue issue1 = ValidationIssue.error("CODE", "message")
                    .at("location")
                    .withContext("key", "value");
            final ValidationIssue issue2 = ValidationIssue.error("CODE", "message")
                    .at("location")
                    .withContext("key", "value");

            assertThat(issue1).isEqualTo(issue2);
            assertThat(issue1.hashCode()).isEqualTo(issue2.hashCode());
        }

        @Test
        @DisplayName("equals returns false for different severities")
        void equalsReturnsFalseForDifferentSeverities() {
            final ValidationIssue issue1 = ValidationIssue.error("CODE", "message");
            final ValidationIssue issue2 = ValidationIssue.warning("CODE", "message");

            assertThat(issue1).isNotEqualTo(issue2);
        }

        @Test
        @DisplayName("equals returns false for different codes")
        void equalsReturnsFalseForDifferentCodes() {
            final ValidationIssue issue1 = ValidationIssue.error("CODE_1", "message");
            final ValidationIssue issue2 = ValidationIssue.error("CODE_2", "message");

            assertThat(issue1).isNotEqualTo(issue2);
        }

        @Test
        @DisplayName("equals returns false for different messages")
        void equalsReturnsFalseForDifferentMessages() {
            final ValidationIssue issue1 = ValidationIssue.error("CODE", "message1");
            final ValidationIssue issue2 = ValidationIssue.error("CODE", "message2");

            assertThat(issue1).isNotEqualTo(issue2);
        }

        @Test
        @DisplayName("equals returns false for different locations")
        void equalsReturnsFalseForDifferentLocations() {
            final ValidationIssue issue1 = ValidationIssue.error("CODE", "message").at("loc1");
            final ValidationIssue issue2 = ValidationIssue.error("CODE", "message").at("loc2");

            assertThat(issue1).isNotEqualTo(issue2);
        }

        @Test
        @DisplayName("equals returns false for different context")
        void equalsReturnsFalseForDifferentContext() {
            final ValidationIssue issue1 = ValidationIssue.error("CODE", "message")
                    .withContext("key", "value1");
            final ValidationIssue issue2 = ValidationIssue.error("CODE", "message")
                    .withContext("key", "value2");

            assertThat(issue1).isNotEqualTo(issue2);
        }

        @Test
        @DisplayName("equals returns true for same instance")
        void equalsReturnsTrueForSameInstance() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");
            assertThat(issue).isEqualTo(issue);
        }

        @Test
        @DisplayName("equals returns false for null")
        void equalsReturnsFalseForNull() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");
            assertThat(issue).isNotEqualTo(null);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString contains severity")
        void toStringContainsSeverity() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");
            assertThat(issue.toString()).contains("[ERROR]");
        }

        @Test
        @DisplayName("toString contains code")
        void toStringContainsCode() {
            final ValidationIssue issue = ValidationIssue.error("MY_CODE", "message");
            assertThat(issue.toString()).contains("MY_CODE");
        }

        @Test
        @DisplayName("toString contains message")
        void toStringContainsMessage() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "my error message");
            assertThat(issue.toString()).contains("my error message");
        }

        @Test
        @DisplayName("toString contains location when present")
        void toStringContainsLocationWhenPresent() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message")
                    .at("Schema@100");
            assertThat(issue.toString()).contains("at Schema@100");
        }

        @Test
        @DisplayName("toString does not contain 'at' when no location")
        void toStringDoesNotContainAtWhenNoLocation() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");
            assertThat(issue.toString()).doesNotContain(" at ");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles empty code")
        void handlesEmptyCode() {
            final ValidationIssue issue = ValidationIssue.error("", "message");
            assertThat(issue.code()).isEmpty();
        }

        @Test
        @DisplayName("handles empty message")
        void handlesEmptyMessage() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "");
            assertThat(issue.message()).isEmpty();
        }

        @Test
        @DisplayName("handles unicode in message")
        void handlesUnicodeInMessage() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "日本語メッセージ");
            assertThat(issue.message()).isEqualTo("日本語メッセージ");
        }

        @Test
        @DisplayName("handles special characters in location")
        void handlesSpecialCharactersInLocation() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message")
                    .at("Schema@100/player/data[0]");
            assertThat(issue.location()).contains("Schema@100/player/data[0]");
        }

        @Test
        @DisplayName("context preserves insertion order")
        void contextPreservesOrder() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message")
                    .withContext("first", 1)
                    .withContext("second", 2)
                    .withContext("third", 3);

            assertThat(issue.context().keySet())
                    .containsExactlyInAnyOrder("first", "second", "third");
        }
    }
}
