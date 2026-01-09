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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single validation issue with context.
 *
 * <p>A validation issue contains:</p>
 * <ul>
 *   <li><b>Severity:</b> How critical the issue is (ERROR, WARNING, INFO)</li>
 *   <li><b>Code:</b> A unique identifier for the issue type</li>
 *   <li><b>Message:</b> Human-readable description</li>
 *   <li><b>Location:</b> Optional path or identifier where the issue was found</li>
 *   <li><b>Context:</b> Additional key-value metadata</li>
 * </ul>
 *
 * <h2>Issue Codes</h2>
 * <p>Issue codes follow the pattern: {@code CATEGORY_ISSUE}, for example:</p>
 * <ul>
 *   <li>{@code STRUCTURE_CYCLE} - Parent chain contains a cycle</li>
 *   <li>{@code STRUCTURE_VERSION_ORDER} - Version ordering violation</li>
 *   <li>{@code CONVENTION_TYPE_NAME} - Type naming convention violation</li>
 *   <li>{@code COVERAGE_MISSING_FIX} - Missing DataFix for type change</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create an error
 * ValidationIssue error = ValidationIssue.error(
 *         "STRUCTURE_CYCLE",
 *         "Parent chain contains a cycle"
 *     )
 *     .at("Schema110")
 *     .withContext("parentVersion", 100);
 *
 * // Create a warning
 * ValidationIssue warning = ValidationIssue.warning(
 *         "CONVENTION_TYPE_NAME",
 *         "Type name should be snake_case"
 *     )
 *     .at("PLAYER_DATA");
 *
 * // Check severity
 * if (error.severity() == IssueSeverity.ERROR) {
 *     System.err.println(error);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see ValidationResult
 * @see IssueSeverity
 * @since 0.3.0
 */
public final class ValidationIssue {

    /**
     * The severity level of this issue (ERROR, WARNING, or INFO).
     */
    private final IssueSeverity severity;

    /**
     * A unique code identifying the type of issue (e.g., "STRUCTURE_CYCLE").
     */
    private final String code;

    /**
     * A human-readable description of the issue.
     */
    private final String message;

    /**
     * The location where the issue was found (e.g., "Schema@100/player").
     * May be {@code null} if no specific location applies.
     */
    private final String location;

    /**
     * Additional contextual information as key-value pairs.
     * Always non-null but may be empty.
     */
    private final Map<String, Object> context;

    /**
     * Creates a new immutable ValidationIssue instance.
     *
     * <p>This constructor is private; use factory methods
     * ({@link #error}, {@link #warning}, {@link #info}) to create instances.</p>
     *
     * @param severity the issue severity, must not be {@code null}
     * @param code     the issue code, must not be {@code null}
     * @param message  the issue message, must not be {@code null}
     * @param location the location where found, may be {@code null}
     * @param context  additional context, must not be {@code null}
     */
    private ValidationIssue(
            @NotNull final IssueSeverity severity,
            @NotNull final String code,
            @NotNull final String message,
            @Nullable final String location,
            @NotNull final Map<String, Object> context
    ) {
        this.severity = Preconditions.checkNotNull(severity, "severity must not be null");
        this.code = Preconditions.checkNotNull(code, "code must not be null");
        this.message = Preconditions.checkNotNull(message, "message must not be null");
        this.location = location;
        this.context = Map.copyOf(Preconditions.checkNotNull(context, "context must not be null"));
    }

    /**
     * Creates an error-level issue.
     *
     * @param code    the issue code, must not be {@code null}
     * @param message the issue message, must not be {@code null}
     * @return a new error issue, never {@code null}
     */
    @NotNull
    public static ValidationIssue error(@NotNull final String code, @NotNull final String message) {
        return new ValidationIssue(IssueSeverity.ERROR, code, message, null, Map.of());
    }

    /**
     * Creates a warning-level issue.
     *
     * @param code    the issue code, must not be {@code null}
     * @param message the issue message, must not be {@code null}
     * @return a new warning issue, never {@code null}
     */
    @NotNull
    public static ValidationIssue warning(@NotNull final String code, @NotNull final String message) {
        return new ValidationIssue(IssueSeverity.WARNING, code, message, null, Map.of());
    }

    /**
     * Creates an info-level issue.
     *
     * @param code    the issue code, must not be {@code null}
     * @param message the issue message, must not be {@code null}
     * @return a new info issue, never {@code null}
     */
    @NotNull
    public static ValidationIssue info(@NotNull final String code, @NotNull final String message) {
        return new ValidationIssue(IssueSeverity.INFO, code, message, null, Map.of());
    }

    /**
     * Returns a new issue with the specified location.
     *
     * @param location the location where the issue was found, must not be {@code null}
     * @return a new issue with the location set, never {@code null}
     */
    @NotNull
    public ValidationIssue at(@NotNull final String location) {
        Preconditions.checkNotNull(location, "location must not be null");
        return new ValidationIssue(this.severity, this.code, this.message, location, this.context);
    }

    /**
     * Returns a new issue with additional context.
     *
     * @param key   the context key, must not be {@code null}
     * @param value the context value, must not be {@code null}
     * @return a new issue with the context added, never {@code null}
     */
    @NotNull
    public ValidationIssue withContext(@NotNull final String key, @NotNull final Object value) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(value, "value must not be null");

        final Map<String, Object> newContext = new HashMap<>(this.context);
        newContext.put(key, value);
        return new ValidationIssue(this.severity, this.code, this.message, this.location, newContext);
    }

    /**
     * Returns the severity of this issue.
     *
     * @return the severity, never {@code null}
     */
    @NotNull
    public IssueSeverity severity() {
        return this.severity;
    }

    /**
     * Returns the issue code.
     *
     * @return the code, never {@code null}
     */
    @NotNull
    public String code() {
        return this.code;
    }

    /**
     * Returns the human-readable message.
     *
     * @return the message, never {@code null}
     */
    @NotNull
    public String message() {
        return this.message;
    }

    /**
     * Returns the location where the issue was found.
     *
     * @return an Optional containing the location if set, empty otherwise
     */
    @NotNull
    public Optional<String> location() {
        return Optional.ofNullable(this.location);
    }

    /**
     * Returns additional context for this issue.
     *
     * @return an unmodifiable map of context values, never {@code null}
     */
    @NotNull
    public Map<String, Object> context() {
        return this.context;
    }

    /**
     * Checks if this is an error-level issue.
     *
     * @return {@code true} if severity is ERROR
     */
    public boolean isError() {
        return this.severity == IssueSeverity.ERROR;
    }

    /**
     * Checks if this is a warning-level issue.
     *
     * @return {@code true} if severity is WARNING
     */
    public boolean isWarning() {
        return this.severity == IssueSeverity.WARNING;
    }

    /**
     * Checks if this is an info-level issue.
     *
     * @return {@code true} if severity is INFO
     */
    public boolean isInfo() {
        return this.severity == IssueSeverity.INFO;
    }

    /**
     * Compares this validation issue to another object for equality.
     *
     * <p>Two {@code ValidationIssue} instances are equal if they have the same
     * severity, code, message, location, and context.</p>
     *
     * @param obj the object to compare with, may be {@code null}
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ValidationIssue other)) {
            return false;
        }
        return this.severity == other.severity
                && this.code.equals(other.code)
                && this.message.equals(other.message)
                && Objects.equals(this.location, other.location)
                && this.context.equals(other.context);
    }

    /**
     * Returns a hash code value for this validation issue.
     *
     * <p>The hash code is computed from all properties to ensure
     * consistency with {@link #equals(Object)}.</p>
     *
     * @return the hash code value for this validation issue
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.severity, this.code, this.message, this.location, this.context);
    }

    /**
     * Returns a human-readable string representation of this validation issue.
     *
     * <p>The format is: {@code [SEVERITY] CODE: message at location}</p>
     * <p>Example: {@code [ERROR] STRUCTURE_CYCLE: Parent chain contains a cycle at Schema@110}</p>
     *
     * @return a formatted string representation, never {@code null}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[").append(this.severity).append("] ");
        sb.append(this.code).append(": ");
        sb.append(this.message);
        if (this.location != null) {
            sb.append(" at ").append(this.location);
        }
        return sb.toString();
    }
}
