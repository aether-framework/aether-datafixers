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

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Configurable rules for schema convention checking.
 *
 * <p>Convention rules define naming patterns and constraints that schemas
 * should follow. This class provides predefined rule sets as well as a
 * builder for custom configurations.</p>
 *
 * <h2>Predefined Rule Sets</h2>
 * <ul>
 *   <li>{@link #STRICT} - Enforces strict naming conventions</li>
 *   <li>{@link #RELAXED} - Minimal conventions, mostly warnings</li>
 *   <li>{@link #NONE} - No convention checking</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Use predefined rules
 * ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
 *     .validateConventions()
 *     .withConventions(ConventionRules.STRICT)
 *     .validate();
 *
 * // Custom rules
 * ConventionRules custom = ConventionRules.builder()
 *     .typeNamePattern(Pattern.compile("[a-z][a-z_0-9]*"))
 *     .fieldNamePattern(Pattern.compile("[a-z][a-zA-Z0-9]*"))
 *     .requireTypePrefix("entity_")
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pfoertner
 * @see ConventionChecker
 * @see SchemaValidator
 * @since 0.3.0
 */
public final class ConventionRules {

    /**
     * Strict convention rules.
     *
     * <p>Enforces:</p>
     * <ul>
     *   <li>Type names must be snake_case</li>
     *   <li>Field names must be snake_case</li>
     *   <li>Schema class names must start with "Schema" (e.g., Schema100, Schema200)</li>
     *   <li>DataFix class names must end with "Fix" (e.g., PlayerNameFix)</li>
     * </ul>
     */
    public static final ConventionRules STRICT = builder()
            .typeNamePattern(Pattern.compile("^[a-z][a-z0-9_]*$"))
            .fieldNamePattern(Pattern.compile("^[a-z][a-z0-9_]*$"))
            .schemaClassPrefix("Schema")
            .fixClassSuffix("Fix")
            .treatViolationsAsErrors(true)
            .build();

    /**
     * Relaxed convention rules.
     *
     * <p>Reports violations as warnings, not errors. Allows more flexible naming.</p>
     */
    public static final ConventionRules RELAXED = builder()
            .typeNamePattern(Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$"))
            .fieldNamePattern(Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$"))
            .treatViolationsAsErrors(false)
            .build();

    /**
     * No convention checking.
     *
     * <p>Disables all convention checks. Useful when you only want
     * structural validation.</p>
     */
    public static final ConventionRules NONE = builder()
            .enabled(false)
            .build();

    /**
     * Master flag enabling/disabling all convention checks.
     */
    private final boolean enabled;

    /**
     * Regex pattern that type names must match, or {@code null} to skip pattern check.
     */
    private final Pattern typeNamePattern;

    /**
     * Regex pattern that field names must match, or {@code null} to skip pattern check.
     */
    private final Pattern fieldNamePattern;

    /**
     * Required prefix for type names, or {@code null} if no prefix required.
     */
    private final String typeNamePrefix;

    /**
     * Expected prefix for schema class names (e.g., "Schema"), or {@code null} to skip.
     */
    private final String schemaClassPrefix;

    /**
     * Expected suffix for schema class names (e.g., "Schema"), or {@code null} to skip.
     */
    private final String schemaClassSuffix;

    /**
     * Expected prefix for fix class names, or {@code null} to skip.
     */
    private final String fixClassPrefix;

    /**
     * Expected suffix for fix class names (e.g., "Fix"), or {@code null} to skip.
     */
    private final String fixClassSuffix;

    /**
     * Flag indicating whether violations should be reported as errors (true) or warnings (false).
     */
    private final boolean treatViolationsAsErrors;

    /**
     * Custom validation predicate for type names, or {@code null} if not used.
     */
    private final Predicate<String> customTypeValidator;

    /**
     * Custom validation predicate for field names, or {@code null} if not used.
     */
    private final Predicate<String> customFieldValidator;

    /**
     * Creates a new immutable ConventionRules instance.
     *
     * <p>This constructor is private; use {@link #builder()} to create instances.</p>
     *
     * @param enabled                 master enable flag
     * @param typeNamePattern         pattern for type names
     * @param fieldNamePattern        pattern for field names
     * @param typeNamePrefix          required prefix for type names
     * @param schemaClassPrefix       expected prefix for schema classes
     * @param schemaClassSuffix       expected suffix for schema classes
     * @param fixClassPrefix          expected prefix for fix classes
     * @param fixClassSuffix          expected suffix for fix classes
     * @param treatViolationsAsErrors whether violations are errors
     * @param customTypeValidator     custom type name validator
     * @param customFieldValidator    custom field name validator
     */
    private ConventionRules(
            final boolean enabled,
            final Pattern typeNamePattern,
            final Pattern fieldNamePattern,
            final String typeNamePrefix,
            final String schemaClassPrefix,
            final String schemaClassSuffix,
            final String fixClassPrefix,
            final String fixClassSuffix,
            final boolean treatViolationsAsErrors,
            final Predicate<String> customTypeValidator,
            final Predicate<String> customFieldValidator
    ) {
        this.enabled = enabled;
        this.typeNamePattern = typeNamePattern;
        this.fieldNamePattern = fieldNamePattern;
        this.typeNamePrefix = typeNamePrefix;
        this.schemaClassPrefix = schemaClassPrefix;
        this.schemaClassSuffix = schemaClassSuffix;
        this.fixClassPrefix = fixClassPrefix;
        this.fixClassSuffix = fixClassSuffix;
        this.treatViolationsAsErrors = treatViolationsAsErrors;
        this.customTypeValidator = customTypeValidator;
        this.customFieldValidator = customFieldValidator;
    }

    /**
     * Creates a new builder for custom convention rules.
     *
     * @return a new builder, never {@code null}
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether convention checking is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Validates a type name against the configured pattern.
     *
     * @param typeName the type name to validate, must not be {@code null}
     * @return {@code true} if the name is valid
     */
    public boolean isValidTypeName(@NotNull final String typeName) {
        Preconditions.checkNotNull(typeName, "typeName must not be null");

        if (!this.enabled) {
            return true;
        }

        // Check prefix if required
        if (this.typeNamePrefix != null && !typeName.startsWith(this.typeNamePrefix)) {
            return false;
        }

        // Check pattern
        if (this.typeNamePattern != null && !this.typeNamePattern.matcher(typeName).matches()) {
            return false;
        }

        // Check custom validator
        if (this.customTypeValidator != null && !this.customTypeValidator.test(typeName)) {
            return false;
        }

        return true;
    }

    /**
     * Validates a field name against the configured pattern.
     *
     * @param fieldName the field name to validate, must not be {@code null}
     * @return {@code true} if the name is valid
     */
    public boolean isValidFieldName(@NotNull final String fieldName) {
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");

        if (!this.enabled) {
            return true;
        }

        // Check pattern
        if (this.fieldNamePattern != null && !this.fieldNamePattern.matcher(fieldName).matches()) {
            return false;
        }

        // Check custom validator
        if (this.customFieldValidator != null && !this.customFieldValidator.test(fieldName)) {
            return false;
        }

        return true;
    }

    /**
     * Validates a schema class name against conventions.
     *
     * <p>Checks both prefix and suffix if configured. For example, with prefix "Schema",
     * valid names include: Schema100, Schema200, SchemaV1.</p>
     *
     * @param className the class name to validate, must not be {@code null}
     * @return {@code true} if the name follows conventions
     */
    public boolean isValidSchemaClassName(@NotNull final String className) {
        Preconditions.checkNotNull(className, "className must not be null");

        if (!this.enabled) {
            return true;
        }

        // Check prefix
        if (this.schemaClassPrefix != null && !className.startsWith(this.schemaClassPrefix)) {
            return false;
        }

        // Check suffix
        if (this.schemaClassSuffix != null && !className.endsWith(this.schemaClassSuffix)) {
            return false;
        }

        return true;
    }

    /**
     * Validates a fix class name against conventions.
     *
     * <p>Checks both prefix and suffix if configured. For example, with suffix "Fix",
     * valid names include: PlayerNameFix, SwordRenameFix.</p>
     *
     * @param className the class name to validate, must not be {@code null}
     * @return {@code true} if the name follows conventions
     */
    public boolean isValidFixClassName(@NotNull final String className) {
        Preconditions.checkNotNull(className, "className must not be null");

        if (!this.enabled) {
            return true;
        }

        // Check prefix
        if (this.fixClassPrefix != null && !className.startsWith(this.fixClassPrefix)) {
            return false;
        }

        // Check suffix
        if (this.fixClassSuffix != null && !className.endsWith(this.fixClassSuffix)) {
            return false;
        }

        return true;
    }

    /**
     * Returns the configured type name pattern.
     *
     * @return the pattern, or {@code null} if not set
     */
    public Pattern typeNamePattern() {
        return this.typeNamePattern;
    }

    /**
     * Returns the configured field name pattern.
     *
     * @return the pattern, or {@code null} if not set
     */
    public Pattern fieldNamePattern() {
        return this.fieldNamePattern;
    }

    /**
     * Returns the required type name prefix.
     *
     * @return the prefix, or {@code null} if not required
     */
    public String typeNamePrefix() {
        return this.typeNamePrefix;
    }

    /**
     * Returns the expected schema class prefix.
     *
     * @return the prefix, or {@code null} if not required
     */
    public String schemaClassPrefix() {
        return this.schemaClassPrefix;
    }

    /**
     * Returns the expected schema class suffix.
     *
     * @return the suffix, or {@code null} if not required
     */
    public String schemaClassSuffix() {
        return this.schemaClassSuffix;
    }

    /**
     * Returns the expected fix class prefix.
     *
     * @return the prefix, or {@code null} if not required
     */
    public String fixClassPrefix() {
        return this.fixClassPrefix;
    }

    /**
     * Returns the expected fix class suffix.
     *
     * @return the suffix, or {@code null} if not required
     */
    public String fixClassSuffix() {
        return this.fixClassSuffix;
    }

    /**
     * Returns whether violations should be reported as errors.
     *
     * @return {@code true} if violations are errors, {@code false} if warnings
     */
    public boolean treatViolationsAsErrors() {
        return this.treatViolationsAsErrors;
    }

    /**
     * Builder for creating custom {@link ConventionRules}.
     */
    public static final class Builder {

        private boolean enabled = true;
        private Pattern typeNamePattern;
        private Pattern fieldNamePattern;
        private String typeNamePrefix;
        private String schemaClassPrefix;
        private String schemaClassSuffix;
        private String fixClassPrefix;
        private String fixClassSuffix;
        private boolean treatViolationsAsErrors = false;
        private Predicate<String> customTypeValidator;
        private Predicate<String> customFieldValidator;

        private Builder() {
        }

        /**
         * Enables or disables convention checking.
         *
         * @param enabled {@code true} to enable, {@code false} to disable
         * @return this builder for chaining
         */
        @NotNull
        public Builder enabled(final boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets the pattern for valid type names.
         *
         * @param pattern the regex pattern, or {@code null} to skip pattern check
         * @return this builder for chaining
         */
        @NotNull
        public Builder typeNamePattern(final Pattern pattern) {
            this.typeNamePattern = pattern;
            return this;
        }

        /**
         * Sets the pattern for valid field names.
         *
         * @param pattern the regex pattern, or {@code null} to skip pattern check
         * @return this builder for chaining
         */
        @NotNull
        public Builder fieldNamePattern(final Pattern pattern) {
            this.fieldNamePattern = pattern;
            return this;
        }

        /**
         * Requires type names to start with a specific prefix.
         *
         * @param prefix the required prefix, or {@code null} to not require
         * @return this builder for chaining
         */
        @NotNull
        public Builder requireTypePrefix(final String prefix) {
            this.typeNamePrefix = prefix;
            return this;
        }

        /**
         * Sets the expected prefix for schema class names.
         *
         * <p>For example, with prefix "Schema", valid class names include:
         * Schema100, Schema200, SchemaV1.</p>
         *
         * @param prefix the expected prefix (e.g., "Schema"), or {@code null} to skip
         * @return this builder for chaining
         */
        @NotNull
        public Builder schemaClassPrefix(final String prefix) {
            this.schemaClassPrefix = prefix;
            return this;
        }

        /**
         * Sets the expected suffix for schema class names.
         *
         * @param suffix the expected suffix (e.g., "Schema"), or {@code null} to skip
         * @return this builder for chaining
         */
        @NotNull
        public Builder schemaClassSuffix(final String suffix) {
            this.schemaClassSuffix = suffix;
            return this;
        }

        /**
         * Sets the expected prefix for fix class names.
         *
         * @param prefix the expected prefix, or {@code null} to skip
         * @return this builder for chaining
         */
        @NotNull
        public Builder fixClassPrefix(final String prefix) {
            this.fixClassPrefix = prefix;
            return this;
        }

        /**
         * Sets the expected suffix for fix class names.
         *
         * <p>For example, with suffix "Fix", valid class names include:
         * PlayerNameFix, SwordRenameFix.</p>
         *
         * @param suffix the expected suffix (e.g., "Fix"), or {@code null} to skip
         * @return this builder for chaining
         */
        @NotNull
        public Builder fixClassSuffix(final String suffix) {
            this.fixClassSuffix = suffix;
            return this;
        }

        /**
         * Sets whether violations are treated as errors or warnings.
         *
         * @param asErrors {@code true} for errors, {@code false} for warnings
         * @return this builder for chaining
         */
        @NotNull
        public Builder treatViolationsAsErrors(final boolean asErrors) {
            this.treatViolationsAsErrors = asErrors;
            return this;
        }

        /**
         * Sets a custom validator for type names.
         *
         * @param validator the custom validation predicate
         * @return this builder for chaining
         */
        @NotNull
        public Builder customTypeValidator(final Predicate<String> validator) {
            this.customTypeValidator = validator;
            return this;
        }

        /**
         * Sets a custom validator for field names.
         *
         * @param validator the custom validation predicate
         * @return this builder for chaining
         */
        @NotNull
        public Builder customFieldValidator(final Predicate<String> validator) {
            this.customFieldValidator = validator;
            return this;
        }

        /**
         * Builds the ConventionRules.
         *
         * @return the constructed rules, never {@code null}
         */
        @NotNull
        public ConventionRules build() {
            return new ConventionRules(
                    this.enabled,
                    this.typeNamePattern,
                    this.fieldNamePattern,
                    this.typeNamePrefix,
                    this.schemaClassPrefix,
                    this.schemaClassSuffix,
                    this.fixClassPrefix,
                    this.fixClassSuffix,
                    this.treatViolationsAsErrors,
                    this.customTypeValidator,
                    this.customFieldValidator
            );
        }
    }
}
