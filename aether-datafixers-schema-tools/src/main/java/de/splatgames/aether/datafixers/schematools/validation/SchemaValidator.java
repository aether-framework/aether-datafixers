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
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry;
import de.splatgames.aether.datafixers.schematools.analysis.CoverageGap;
import de.splatgames.aether.datafixers.schematools.analysis.FixCoverage;
import de.splatgames.aether.datafixers.schematools.analysis.MigrationAnalyzer;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/**
 * Fluent API for validating schemas and schema registries.
 *
 * <p>The SchemaValidator provides a builder-style API for configuring and
 * executing validation checks on schemas. It supports multiple validation
 * types that can be enabled independently.</p>
 *
 * <h2>Validation Types</h2>
 * <ul>
 *   <li><b>Structure:</b> Parent chain, version ordering, empty schemas</li>
 *   <li><b>Conventions:</b> Naming patterns for types, fields, and classes</li>
 *   <li><b>Fix Coverage:</b> Ensures all type changes have corresponding DataFixes</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Validate a single schema
 * ValidationResult result = SchemaValidator.forSchema(schema)
 *     .validateStructure()
 *     .validateConventions()
 *     .withConventions(ConventionRules.STRICT)
 *     .validate();
 *
 * // Validate entire registry
 * ValidationResult result = SchemaValidator.forRegistry(registry)
 *     .validateStructure()
 *     .validate();
 *
 * // Validate from bootstrap
 * ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
 *     .validateStructure()
 *     .validateConventions()
 *     .validateFixCoverage()
 *     .validate();
 *
 * // Check results
 * if (!result.isValid()) {
 *     result.errors().forEach(System.err::println);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is not thread-safe. Create a new instance for each validation.</p>
 *
 * @author Erik Pförtner
 * @see ValidationResult
 * @see ConventionRules
 * @since 0.3.0
 */
public final class SchemaValidator {

    /**
     * The single schema to validate, or {@code null} if validating a registry/bootstrap.
     */
    private final Schema singleSchema;

    /**
     * The schema registry to validate, or {@code null} if validating a single schema.
     */
    private final SchemaRegistry registry;

    /**
     * The DataFixerBuilder for fix coverage validation, or {@code null} if not available.
     * Only populated when created via {@link #forBootstrap(DataFixerBootstrap)}.
     */
    private final DataFixerBuilder fixerBuilder;

    /**
     * Flag indicating whether structure validation is enabled.
     */
    private boolean validateStructure = false;

    /**
     * Flag indicating whether convention validation is enabled.
     */
    private boolean validateConventions = false;

    /**
     * Flag indicating whether fix coverage validation is enabled.
     */
    private boolean validateFixCoverage = false;

    /**
     * The convention rules to use for naming validation.
     * Defaults to {@link ConventionRules#RELAXED}.
     */
    private ConventionRules conventionRules = ConventionRules.RELAXED;

    /**
     * Creates a new SchemaValidator instance.
     *
     * <p>This constructor is private; use one of the factory methods
     * ({@link #forSchema}, {@link #forRegistry}, {@link #forBootstrap}) instead.</p>
     *
     * @param singleSchema the single schema, or {@code null}
     * @param registry     the schema registry, or {@code null}
     * @param fixerBuilder the fixer builder for coverage validation, or {@code null}
     */
    private SchemaValidator(
            final Schema singleSchema,
            final SchemaRegistry registry,
            final DataFixerBuilder fixerBuilder
    ) {
        this.singleSchema = singleSchema;
        this.registry = registry;
        this.fixerBuilder = fixerBuilder;
    }

    /**
     * Creates a validator for a single schema.
     *
     * <p>Note: Fix coverage validation is not available when validating
     * a single schema.</p>
     *
     * @param schema the schema to validate, must not be {@code null}
     * @return a new validator, never {@code null}
     */
    @NotNull
    public static SchemaValidator forSchema(@NotNull final Schema schema) {
        Preconditions.checkNotNull(schema, "schema must not be null");
        return new SchemaValidator(schema, null, null);
    }

    /**
     * Creates a validator for a schema registry.
     *
     * <p>Note: Fix coverage validation is not available without a bootstrap.</p>
     *
     * @param registry the registry to validate, must not be {@code null}
     * @return a new validator, never {@code null}
     */
    @NotNull
    public static SchemaValidator forRegistry(@NotNull final SchemaRegistry registry) {
        Preconditions.checkNotNull(registry, "registry must not be null");
        return new SchemaValidator(null, registry, null);
    }

    /**
     * Creates a validator for a DataFixer bootstrap.
     *
     * <p>This mode enables all validation types including fix coverage.
     * The bootstrap is executed internally to populate temporary registries
     * for analysis.</p>
     *
     * @param bootstrap the bootstrap to validate, must not be {@code null}
     * @return a new validator, never {@code null}
     */
    @NotNull
    public static SchemaValidator forBootstrap(@NotNull final DataFixerBootstrap bootstrap) {
        Preconditions.checkNotNull(bootstrap, "bootstrap must not be null");

        // Bootstrap into capturing registries
        final SchemaRegistry schemaRegistry = new SimpleSchemaRegistry();
        final DataFixerBuilder fixerBuilder = new DataFixerBuilder(new DataVersion(Integer.MAX_VALUE));

        bootstrap.registerSchemas(schemaRegistry);
        bootstrap.registerFixes(fixerBuilder);

        return new SchemaValidator(null, schemaRegistry, fixerBuilder);
    }

    /**
     * Enables structure validation.
     *
     * <p>Structure validation checks:</p>
     * <ul>
     *   <li>Parent chain cycles</li>
     *   <li>Version ordering (parent &lt; child)</li>
     *   <li>Empty schemas</li>
     *   <li>Missing root schema</li>
     * </ul>
     *
     * @return this validator for chaining
     */
    @NotNull
    public SchemaValidator validateStructure() {
        this.validateStructure = true;
        return this;
    }

    /**
     * Enables convention validation.
     *
     * <p>Convention validation checks naming patterns against the configured
     * {@link ConventionRules}.</p>
     *
     * @return this validator for chaining
     * @see #withConventions(ConventionRules)
     */
    @NotNull
    public SchemaValidator validateConventions() {
        this.validateConventions = true;
        return this;
    }

    /**
     * Enables fix coverage validation.
     *
     * <p>Fix coverage checks that all type changes between versions have
     * corresponding DataFixes. Only available when validating a bootstrap.</p>
     *
     * @return this validator for chaining
     * @throws IllegalStateException if not validating a bootstrap
     */
    @NotNull
    public SchemaValidator validateFixCoverage() {
        if (this.fixerBuilder == null) {
            throw new IllegalStateException(
                    "Fix coverage validation requires a bootstrap. Use forBootstrap() instead.");
        }
        this.validateFixCoverage = true;
        return this;
    }

    /**
     * Enables all available validation types.
     *
     * @return this validator for chaining
     */
    @NotNull
    public SchemaValidator validateAll() {
        this.validateStructure = true;
        this.validateConventions = true;
        if (this.fixerBuilder != null) {
            this.validateFixCoverage = true;
        }
        return this;
    }

    /**
     * Sets the convention rules to use for convention validation.
     *
     * @param rules the rules to apply, must not be {@code null}
     * @return this validator for chaining
     */
    @NotNull
    public SchemaValidator withConventions(@NotNull final ConventionRules rules) {
        Preconditions.checkNotNull(rules, "rules must not be null");
        this.conventionRules = rules;
        return this;
    }

    /**
     * Executes the configured validations and returns the result.
     *
     * @return the validation result, never {@code null}
     */
    @NotNull
    public ValidationResult validate() {
        ValidationResult result = ValidationResult.empty();

        if (this.singleSchema != null) {
            result = validateSingleSchema(this.singleSchema, result);
        } else if (this.registry != null) {
            result = validateRegistry(this.registry, result);
        }

        return result;
    }

    /**
     * Validates a single schema with the configured validation types.
     *
     * <p>This method applies structure and convention validation (if enabled)
     * to the given schema. Fix coverage validation is not available for
     * single schema validation.</p>
     *
     * @param schema the schema to validate, must not be {@code null}
     * @param result the current validation result to merge with
     * @return the merged validation result including new issues
     */
    @NotNull
    private ValidationResult validateSingleSchema(
            @NotNull final Schema schema,
            @NotNull ValidationResult result
    ) {
        Preconditions.checkNotNull(schema, "schema must not be null");
        Preconditions.checkNotNull(result, "result must not be null");
        if (this.validateStructure) {
            result = result.merge(StructureValidator.validate(schema, null));
        }

        if (this.validateConventions) {
            result = result.merge(ConventionChecker.checkSchema(schema, this.conventionRules));
        }

        return result;
    }

    /**
     * Validates all schemas in a registry with the configured validation types.
     *
     * <p>This method is used both for registry validation and bootstrap validation.
     * It applies:</p>
     * <ul>
     *   <li>Structure validation across all schemas</li>
     *   <li>Convention validation for each schema</li>
     *   <li>Fix coverage validation (if in bootstrap mode and enabled)</li>
     * </ul>
     *
     * @param registry the schema registry to validate, must not be {@code null}
     * @param result   the current validation result to merge with
     * @return the merged validation result including new issues
     */
    @NotNull
    private ValidationResult validateRegistry(
            @NotNull final SchemaRegistry registry,
            @NotNull ValidationResult result
    ) {
        Preconditions.checkNotNull(registry, "registry must not be null");
        Preconditions.checkNotNull(result, "result must not be null");
        // Structure validation
        if (this.validateStructure) {
            result = result.merge(StructureValidator.validateRegistry(registry));
        }

        // Convention validation
        if (this.validateConventions) {
            for (final Schema schema : registry.stream().toList()) {
                result = result.merge(ConventionChecker.checkSchema(schema, this.conventionRules));
            }
        }

        // Fix coverage validation (only available in bootstrap mode)
        if (this.validateFixCoverage && this.fixerBuilder != null) {
            result = result.merge(validateFixCoverageInternal());
        }

        return result;
    }

    /**
     * Validates that all type changes between schema versions have corresponding DataFixes.
     *
     * <p>This method uses the {@link MigrationAnalyzer} to analyze the migration path
     * and identify any coverage gaps where types changed without a DataFix to handle
     * the migration.</p>
     *
     * @return validation result containing coverage gap issues, never {@code null}
     */
    @NotNull
    private ValidationResult validateFixCoverageInternal() {
        // Get version range from the registry
        final List<Schema> schemas = this.registry.stream().toList();
        if (schemas.size() < 2) {
            // Not enough schemas to analyze coverage
            return ValidationResult.empty();
        }

        // Determine version range
        final int minVersion = schemas.stream()
                .map(s -> s.version().getVersion())
                .min(Comparator.naturalOrder())
                .orElse(0);
        final int maxVersion = schemas.stream()
                .map(s -> s.version().getVersion())
                .max(Comparator.naturalOrder())
                .orElse(0);

        if (minVersion == maxVersion) {
            return ValidationResult.empty();
        }

        // Use MigrationAnalyzer to analyze coverage
        final MigrationAnalyzer analyzer = MigrationAnalyzer.forRegistries(
                this.registry,
                this.fixerBuilder.getFixRegistry()
        );

        final FixCoverage coverage = analyzer
                .from(new DataVersion(minVersion))
                .to(new DataVersion(maxVersion))
                .includeFieldLevel(true)
                .analyzeCoverage();

        if (coverage.isFullyCovered()) {
            return ValidationResult.empty();
        }

        // Convert coverage gaps to validation issues
        final ValidationResult.Builder resultBuilder = ValidationResult.builder();

        for (final CoverageGap gap : coverage.gaps()) {
            final String location = String.format(
                    "v%d -> v%d",
                    gap.sourceVersion().getVersion(),
                    gap.targetVersion().getVersion()
            );

            final String message = gap.fieldName()
                    .map(field -> String.format(
                            "Missing DataFix for type '%s' field '%s': %s",
                            gap.type().getId(),
                            field,
                            gap.reason().description()
                    ))
                    .orElseGet(() -> String.format(
                            "Missing DataFix for type '%s': %s",
                            gap.type().getId(),
                            gap.reason().description()
                    ));

            resultBuilder.add(
                    ValidationIssue.warning("COVERAGE_MISSING_FIX", message)
                            .at(location)
                            .withContext("type", gap.type().getId())
                            .withContext("reason", gap.reason().name())
                            .withContext("sourceVersion", gap.sourceVersion().getVersion())
                            .withContext("targetVersion", gap.targetVersion().getVersion())
            );
        }

        return resultBuilder.build();
    }
}
