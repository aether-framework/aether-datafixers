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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ValidationResult}.
 */
@DisplayName("ValidationResult")
class ValidationResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("empty() returns result with no issues")
        void emptyReturnsResultWithNoIssues() {
            final ValidationResult result = ValidationResult.empty();

            assertThat(result.issues()).isEmpty();
            assertThat(result.isValid()).isTrue();
            assertThat(result.hasIssues()).isFalse();
        }

        @Test
        @DisplayName("empty() returns singleton instance")
        void emptyReturnsSingletonInstance() {
            assertThat(ValidationResult.empty()).isSameAs(ValidationResult.empty());
        }

        @Test
        @DisplayName("of() with empty list returns empty result")
        void ofWithEmptyListReturnsEmptyResult() {
            final ValidationResult result = ValidationResult.of(List.of());

            assertThat(result).isSameAs(ValidationResult.empty());
        }

        @Test
        @DisplayName("of() creates result with issues")
        void ofCreatesResultWithIssues() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");
            final ValidationResult result = ValidationResult.of(List.of(issue));

            assertThat(result.issues()).containsExactly(issue);
        }

        @Test
        @DisplayName("of() throws on null list")
        void ofThrowsOnNullList() {
            assertThatThrownBy(() -> ValidationResult.of(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds empty result when no issues added")
        void buildsEmptyResultWhenNoIssuesAdded() {
            final ValidationResult result = ValidationResult.builder().build();

            assertThat(result.issues()).isEmpty();
        }

        @Test
        @DisplayName("add() adds single issue")
        void addAddsSingleIssue() {
            final ValidationIssue issue = ValidationIssue.error("CODE", "message");
            final ValidationResult result = ValidationResult.builder()
                    .add(issue)
                    .build();

            assertThat(result.issues()).containsExactly(issue);
        }

        @Test
        @DisplayName("addAll() adds multiple issues")
        void addAllAddsMultipleIssues() {
            final ValidationIssue issue1 = ValidationIssue.error("CODE1", "message1");
            final ValidationIssue issue2 = ValidationIssue.warning("CODE2", "message2");
            final ValidationResult result = ValidationResult.builder()
                    .addAll(List.of(issue1, issue2))
                    .build();

            assertThat(result.issues()).containsExactly(issue1, issue2);
        }

        @Test
        @DisplayName("error() creates and adds error issue")
        void errorCreatesAndAddsErrorIssue() {
            final ValidationResult result = ValidationResult.builder()
                    .error("CODE", "error message")
                    .build();

            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().get(0).code()).isEqualTo("CODE");
        }

        @Test
        @DisplayName("warning() creates and adds warning issue")
        void warningCreatesAndAddsWarningIssue() {
            final ValidationResult result = ValidationResult.builder()
                    .warning("CODE", "warning message")
                    .build();

            assertThat(result.warnings()).hasSize(1);
        }

        @Test
        @DisplayName("info() creates and adds info issue")
        void infoCreatesAndAddsInfoIssue() {
            final ValidationResult result = ValidationResult.builder()
                    .info("CODE", "info message")
                    .build();

            assertThat(result.infos()).hasSize(1);
        }

        @Test
        @DisplayName("add() throws on null issue")
        void addThrowsOnNullIssue() {
            assertThatThrownBy(() -> ValidationResult.builder().add(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("addAll() throws on null iterable")
        void addAllThrowsOnNullIterable() {
            assertThatThrownBy(() -> ValidationResult.builder().addAll(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Filtering Methods")
    class FilteringMethods {

        @Test
        @DisplayName("errors() returns only error issues")
        void errorsReturnsOnlyErrorIssues() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error1")
                    .warning("W1", "warning1")
                    .error("E2", "error2")
                    .info("I1", "info1")
                    .build();

            assertThat(result.errors()).hasSize(2);
            assertThat(result.errors()).allMatch(ValidationIssue::isError);
        }

        @Test
        @DisplayName("warnings() returns only warning issues")
        void warningsReturnsOnlyWarningIssues() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error1")
                    .warning("W1", "warning1")
                    .warning("W2", "warning2")
                    .build();

            assertThat(result.warnings()).hasSize(2);
            assertThat(result.warnings()).allMatch(ValidationIssue::isWarning);
        }

        @Test
        @DisplayName("infos() returns only info issues")
        void infosReturnsOnlyInfoIssues() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error1")
                    .info("I1", "info1")
                    .info("I2", "info2")
                    .build();

            assertThat(result.infos()).hasSize(2);
            assertThat(result.infos()).allMatch(ValidationIssue::isInfo);
        }

        @Test
        @DisplayName("byCode() returns issues matching code")
        void byCodeReturnsIssuesMatchingCode() {
            final ValidationResult result = ValidationResult.builder()
                    .error("STRUCT_001", "error1")
                    .warning("STRUCT_001", "warning1")
                    .error("CONV_001", "error2")
                    .build();

            assertThat(result.byCode("STRUCT_001")).hasSize(2);
        }

        @Test
        @DisplayName("byCode() throws on null code")
        void byCodeThrowsOnNullCode() {
            final ValidationResult result = ValidationResult.empty();

            assertThatThrownBy(() -> result.byCode(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("atLocation() returns issues at location")
        void atLocationReturnsIssuesAtLocation() {
            final ValidationResult result = ValidationResult.of(List.of(
                    ValidationIssue.error("E1", "error1").at("Schema@100"),
                    ValidationIssue.error("E2", "error2").at("Schema@200"),
                    ValidationIssue.error("E3", "error3").at("Schema@100")
            ));

            assertThat(result.atLocation("Schema@100")).hasSize(2);
        }

        @Test
        @DisplayName("atLocation() throws on null location")
        void atLocationThrowsOnNullLocation() {
            final ValidationResult result = ValidationResult.empty();

            assertThatThrownBy(() -> result.atLocation(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Validity Checks")
    class ValidityChecks {

        @Test
        @DisplayName("isValid() returns true when no errors")
        void isValidReturnsTrueWhenNoErrors() {
            final ValidationResult result = ValidationResult.builder()
                    .warning("W1", "warning")
                    .info("I1", "info")
                    .build();

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("isValid() returns false when has errors")
        void isValidReturnsFalseWhenHasErrors() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error")
                    .build();

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("hasIssues() returns true when has any issues")
        void hasIssuesReturnsTrueWhenHasAnyIssues() {
            final ValidationResult result = ValidationResult.builder()
                    .info("I1", "info")
                    .build();

            assertThat(result.hasIssues()).isTrue();
        }

        @Test
        @DisplayName("hasIssues() returns false when empty")
        void hasIssuesReturnsFalseWhenEmpty() {
            assertThat(ValidationResult.empty().hasIssues()).isFalse();
        }

        @Test
        @DisplayName("hasErrors() returns true when has errors")
        void hasErrorsReturnsTrueWhenHasErrors() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error")
                    .build();

            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("hasErrors() returns false when no errors")
        void hasErrorsReturnsFalseWhenNoErrors() {
            final ValidationResult result = ValidationResult.builder()
                    .warning("W1", "warning")
                    .build();

            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("hasWarnings() returns true when has warnings")
        void hasWarningsReturnsTrueWhenHasWarnings() {
            final ValidationResult result = ValidationResult.builder()
                    .warning("W1", "warning")
                    .build();

            assertThat(result.hasWarnings()).isTrue();
        }
    }

    @Nested
    @DisplayName("Count Methods")
    class CountMethods {

        @Test
        @DisplayName("issueCount() returns total count")
        void issueCountReturnsTotalCount() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error")
                    .warning("W1", "warning")
                    .info("I1", "info")
                    .build();

            assertThat(result.issueCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("errorCount() returns error count")
        void errorCountReturnsErrorCount() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error1")
                    .error("E2", "error2")
                    .warning("W1", "warning")
                    .build();

            assertThat(result.errorCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("warningCount() returns warning count")
        void warningCountReturnsWarningCount() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error")
                    .warning("W1", "warning1")
                    .warning("W2", "warning2")
                    .warning("W3", "warning3")
                    .build();

            assertThat(result.warningCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("infoCount() returns info count")
        void infoCountReturnsInfoCount() {
            final ValidationResult result = ValidationResult.builder()
                    .info("I1", "info1")
                    .info("I2", "info2")
                    .build();

            assertThat(result.infoCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("merge() method")
    class MergeMethod {

        @Test
        @DisplayName("merges two results")
        void mergesTwoResults() {
            final ValidationResult result1 = ValidationResult.builder()
                    .error("E1", "error1")
                    .build();
            final ValidationResult result2 = ValidationResult.builder()
                    .warning("W1", "warning1")
                    .build();

            final ValidationResult merged = result1.merge(result2);

            assertThat(merged.issueCount()).isEqualTo(2);
            assertThat(merged.errorCount()).isEqualTo(1);
            assertThat(merged.warningCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("merge with empty returns original")
        void mergeWithEmptyReturnsOriginal() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error")
                    .build();

            assertThat(result.merge(ValidationResult.empty())).isSameAs(result);
        }

        @Test
        @DisplayName("empty merge with result returns result")
        void emptyMergeWithResultReturnsResult() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error")
                    .build();

            assertThat(ValidationResult.empty().merge(result)).isSameAs(result);
        }

        @Test
        @DisplayName("merge throws on null")
        void mergeThrowsOnNull() {
            final ValidationResult result = ValidationResult.empty();

            assertThatThrownBy(() -> result.merge(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringMethod {

        @Test
        @DisplayName("toString for empty result")
        void toStringForEmptyResult() {
            final String str = ValidationResult.empty().toString();

            assertThat(str).contains("valid");
            assertThat(str).contains("no issues");
        }

        @Test
        @DisplayName("toString for valid result with warnings")
        void toStringForValidResultWithWarnings() {
            final ValidationResult result = ValidationResult.builder()
                    .warning("W1", "warning")
                    .build();

            assertThat(result.toString())
                    .contains("valid")
                    .contains("0 errors")
                    .contains("1 warning");
        }

        @Test
        @DisplayName("toString for invalid result")
        void toStringForInvalidResult() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error")
                    .build();

            assertThat(result.toString())
                    .contains("invalid")
                    .contains("1 error");
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("issues() returns immutable list")
        void issuesReturnsImmutableList() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error")
                    .build();

            assertThatThrownBy(() -> result.issues().add(ValidationIssue.error("E2", "error2")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("errors() returns immutable list")
        void errorsReturnsImmutableList() {
            final ValidationResult result = ValidationResult.builder()
                    .error("E1", "error")
                    .build();

            assertThatThrownBy(() -> result.errors().add(ValidationIssue.error("E2", "error2")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("warnings() returns immutable list")
        void warningsReturnsImmutableList() {
            final ValidationResult result = ValidationResult.builder()
                    .warning("W1", "warning")
                    .build();

            assertThatThrownBy(() -> result.warnings().add(ValidationIssue.warning("W2", "warning2")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
