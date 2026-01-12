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

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of a schema validation operation.
 *
 * <p>Contains all validation issues found during validation, categorized by
 * severity. Provides convenience methods for checking validity and filtering
 * issues.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
 *     .validateStructure()
 *     .validateConventions()
 *     .validate();
 *
 * if (!result.isValid()) {
 *     System.err.println("Validation failed with " + result.errorCount() + " errors:");
 *     result.errors().forEach(System.err::println);
 * }
 *
 * // Check warnings
 * if (result.hasWarnings()) {
 *     System.out.println("Warnings:");
 *     result.warnings().forEach(System.out::println);
 * }
 * }</pre>
 *
 * <h2>Validity</h2>
 * <p>A result is considered valid if it contains no ERROR-level issues.
 * Warnings and info messages do not affect validity.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see ValidationIssue
 * @see SchemaValidator
 * @since 0.3.0
 */
public final class ValidationResult {

    /**
     * Singleton instance for empty validation results.
     * Used to avoid unnecessary object creation for successful validations.
     */
    private static final ValidationResult EMPTY = new ValidationResult(List.of());

    /**
     * Immutable list of all validation issues found.
     */
    private final List<ValidationIssue> issues;

    /**
     * Cached count of ERROR-level issues.
     */
    private final int errorCount;

    /**
     * Cached count of WARNING-level issues.
     */
    private final int warningCount;

    /**
     * Cached count of INFO-level issues.
     */
    private final int infoCount;

    /**
     * Creates a new immutable ValidationResult with the given issues.
     *
     * <p>The constructor pre-computes severity counts for efficient
     * access via {@link #errorCount()}, {@link #warningCount()}, and
     * {@link #infoCount()}.</p>
     *
     * @param issues the list of validation issues, must not be {@code null}
     */
    private ValidationResult(@NotNull final List<ValidationIssue> issues) {
        this.issues = List.copyOf(Preconditions.checkNotNull(issues, "issues must not be null"));

        int errors = 0;
        int warnings = 0;
        int infos = 0;
        for (final ValidationIssue issue : this.issues) {
            switch (issue.severity()) {
                case ERROR -> errors++;
                case WARNING -> warnings++;
                case INFO -> infos++;
            }
        }
        this.errorCount = errors;
        this.warningCount = warnings;
        this.infoCount = infos;
    }

    /**
     * Returns an empty validation result (no issues).
     *
     * @return an empty result, never {@code null}
     */
    @NotNull
    public static ValidationResult empty() {
        return EMPTY;
    }

    /**
     * Creates a validation result from a list of issues.
     *
     * @param issues the validation issues, must not be {@code null}
     * @return a new ValidationResult, never {@code null}
     */
    @NotNull
    public static ValidationResult of(@NotNull final List<ValidationIssue> issues) {
        Preconditions.checkNotNull(issues, "issues must not be null");
        return issues.isEmpty() ? EMPTY : new ValidationResult(issues);
    }

    /**
     * Creates a new builder for constructing a ValidationResult.
     *
     * @return a new builder, never {@code null}
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns all validation issues.
     *
     * @return an unmodifiable list of all issues, never {@code null}
     */
    @NotNull
    public List<ValidationIssue> issues() {
        return this.issues;
    }

    /**
     * Returns all error-level issues.
     *
     * @return an unmodifiable list of errors, never {@code null}
     */
    @NotNull
    public List<ValidationIssue> errors() {
        return this.issues.stream()
                .filter(ValidationIssue::isError)
                .toList();
    }

    /**
     * Returns all warning-level issues.
     *
     * @return an unmodifiable list of warnings, never {@code null}
     */
    @NotNull
    public List<ValidationIssue> warnings() {
        return this.issues.stream()
                .filter(ValidationIssue::isWarning)
                .toList();
    }

    /**
     * Returns all info-level issues.
     *
     * @return an unmodifiable list of info messages, never {@code null}
     */
    @NotNull
    public List<ValidationIssue> infos() {
        return this.issues.stream()
                .filter(ValidationIssue::isInfo)
                .toList();
    }

    /**
     * Returns issues filtered by code.
     *
     * @param code the issue code to filter by, must not be {@code null}
     * @return an unmodifiable list of matching issues, never {@code null}
     */
    @NotNull
    public List<ValidationIssue> byCode(@NotNull final String code) {
        Preconditions.checkNotNull(code, "code must not be null");
        return this.issues.stream()
                .filter(issue -> issue.code().equals(code))
                .toList();
    }

    /**
     * Returns issues filtered by location.
     *
     * @param location the location to filter by, must not be {@code null}
     * @return an unmodifiable list of matching issues, never {@code null}
     */
    @NotNull
    public List<ValidationIssue> atLocation(@NotNull final String location) {
        Preconditions.checkNotNull(location, "location must not be null");
        return this.issues.stream()
                .filter(issue -> issue.location().map(loc -> loc.equals(location)).orElse(false))
                .toList();
    }

    /**
     * Checks if the validation passed (no errors).
     *
     * <p>A result is valid if it contains no ERROR-level issues.
     * Warnings and info messages do not affect validity.</p>
     *
     * @return {@code true} if no errors were found
     */
    public boolean isValid() {
        return this.errorCount == 0;
    }

    /**
     * Checks if the validation has any issues at all.
     *
     * @return {@code true} if there are any issues (errors, warnings, or info)
     */
    public boolean hasIssues() {
        return !this.issues.isEmpty();
    }

    /**
     * Checks if there are any error-level issues.
     *
     * @return {@code true} if there are errors
     */
    public boolean hasErrors() {
        return this.errorCount > 0;
    }

    /**
     * Checks if there are any warning-level issues.
     *
     * @return {@code true} if there are warnings
     */
    public boolean hasWarnings() {
        return this.warningCount > 0;
    }

    /**
     * Returns the total number of issues.
     *
     * @return the issue count
     */
    public int issueCount() {
        return this.issues.size();
    }

    /**
     * Returns the number of error-level issues.
     *
     * @return the error count
     */
    public int errorCount() {
        return this.errorCount;
    }

    /**
     * Returns the number of warning-level issues.
     *
     * @return the warning count
     */
    public int warningCount() {
        return this.warningCount;
    }

    /**
     * Returns the number of info-level issues.
     *
     * @return the info count
     */
    public int infoCount() {
        return this.infoCount;
    }

    /**
     * Merges this result with another result.
     *
     * @param other the other result to merge, must not be {@code null}
     * @return a new result containing issues from both, never {@code null}
     */
    @NotNull
    public ValidationResult merge(@NotNull final ValidationResult other) {
        Preconditions.checkNotNull(other, "other must not be null");
        if (this.issues.isEmpty()) {
            return other;
        }
        if (other.issues.isEmpty()) {
            return this;
        }

        final List<ValidationIssue> merged = new ArrayList<>(this.issues.size() + other.issues.size());
        merged.addAll(this.issues);
        merged.addAll(other.issues);
        return new ValidationResult(merged);
    }

    @Override
    public String toString() {
        if (this.issues.isEmpty()) {
            return "ValidationResult[valid, no issues]";
        }
        return String.format(
                "ValidationResult[%s, %d errors, %d warnings, %d info]",
                isValid() ? "valid" : "invalid",
                this.errorCount,
                this.warningCount,
                this.infoCount
        );
    }

    /**
     * Builder for constructing {@link ValidationResult} instances.
     *
     * <p>Provides a fluent API for accumulating validation issues during
     * a validation operation. Issues can be added individually or in batches,
     * and convenience methods are provided for creating common issue types.</p>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * ValidationResult result = ValidationResult.builder()
     *     .error("STRUCT_001", "Missing required field")
     *     .warning("CONV_001", "Non-standard naming")
     *     .add(someExistingIssue)
     *     .build();
     * }</pre>
     *
     * <p><b>Thread Safety:</b>
     * Builders are not thread-safe and should not be shared between threads.</p>
     *
     * @author Erik Pförtner
     * @since 0.3.0
     */
    public static final class Builder {

        /**
         * Accumulated issues to be included in the result.
         */
        private final List<ValidationIssue> issues = new ArrayList<>();

        /**
         * Creates a new empty builder.
         */
        private Builder() {
            // Private constructor
        }

        /**
         * Adds an issue to the result.
         *
         * @param issue the issue to add, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder add(@NotNull final ValidationIssue issue) {
            Preconditions.checkNotNull(issue, "issue must not be null");
            this.issues.add(issue);
            return this;
        }

        /**
         * Adds multiple issues to the result.
         *
         * @param issues the issues to add, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder addAll(@NotNull final Iterable<ValidationIssue> issues) {
            Preconditions.checkNotNull(issues, "issues must not be null");
            for (final ValidationIssue issue : issues) {
                this.issues.add(issue);
            }
            return this;
        }

        /**
         * Adds an error issue.
         *
         * @param code    the issue code, must not be {@code null}
         * @param message the issue message, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder error(@NotNull final String code, @NotNull final String message) {
            Preconditions.checkNotNull(code, "code must not be null");
            Preconditions.checkNotNull(message, "message must not be null");
            return add(ValidationIssue.error(code, message));
        }

        /**
         * Adds a warning issue.
         *
         * @param code    the issue code, must not be {@code null}
         * @param message the issue message, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder warning(@NotNull final String code, @NotNull final String message) {
            Preconditions.checkNotNull(code, "code must not be null");
            Preconditions.checkNotNull(message, "message must not be null");
            return add(ValidationIssue.warning(code, message));
        }

        /**
         * Adds an info issue.
         *
         * @param code    the issue code, must not be {@code null}
         * @param message the issue message, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder info(@NotNull final String code, @NotNull final String message) {
            Preconditions.checkNotNull(code, "code must not be null");
            Preconditions.checkNotNull(message, "message must not be null");
            return add(ValidationIssue.info(code, message));
        }

        /**
         * Builds the ValidationResult.
         *
         * @return the constructed result, never {@code null}
         */
        @NotNull
        public ValidationResult build() {
            return ValidationResult.of(this.issues);
        }
    }
}
