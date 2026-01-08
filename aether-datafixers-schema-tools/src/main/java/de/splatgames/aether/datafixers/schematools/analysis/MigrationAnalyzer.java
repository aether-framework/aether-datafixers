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

package de.splatgames.aether.datafixers.schematools.analysis;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.core.fix.DataFixRegistry;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry;
import de.splatgames.aether.datafixers.schematools.diff.SchemaDiff;
import de.splatgames.aether.datafixers.schematools.diff.SchemaDiffer;
import de.splatgames.aether.datafixers.schematools.diff.TypeDiff;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fluent API for analyzing migration paths and fix coverage.
 *
 * <p>The MigrationAnalyzer provides comprehensive analysis of data migrations
 * between schema versions. It can determine:</p>
 * <ul>
 *   <li>The steps required to migrate from one version to another</li>
 *   <li>Which types are affected at each step</li>
 *   <li>Whether all schema changes have corresponding DataFixes (coverage)</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Analyze migration path
 * MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(1).to(5)
 *     .analyze();
 *
 * path.steps().forEach(step -> {
 *     System.out.println("v" + step.sourceVersion() + " -> v" + step.targetVersion());
 *     step.fix().ifPresent(f -> System.out.println("  Fix: " + f.name()));
 * });
 *
 * // Analyze fix coverage
 * FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(1).to(5)
 *     .analyzeCoverage();
 *
 * if (!coverage.isFullyCovered()) {
 *     coverage.gaps().forEach(gap ->
 *         System.err.println("Missing fix for: " + gap.type().id()));
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is not thread-safe. Create a new instance for each analysis.</p>
 *
 * @author Erik Pfoertner
 * @see MigrationPath
 * @see FixCoverage
 * @since 0.3.0
 */
public final class MigrationAnalyzer {

    private final SchemaRegistry schemaRegistry;
    private final DataFixRegistry fixRegistry;
    private DataVersion fromVersion;
    private DataVersion toVersion;
    private boolean includeFieldLevel = false;

    private MigrationAnalyzer(
            @NotNull final SchemaRegistry schemaRegistry,
            @NotNull final DataFixRegistry fixRegistry
    ) {
        this.schemaRegistry = Preconditions.checkNotNull(schemaRegistry, "schemaRegistry must not be null");
        this.fixRegistry = Preconditions.checkNotNull(fixRegistry, "fixRegistry must not be null");
    }

    /**
     * Creates an analyzer from a DataFixer bootstrap.
     *
     * <p>The bootstrap is executed internally to populate temporary registries
     * for analysis.</p>
     *
     * @param bootstrap the bootstrap to analyze, must not be {@code null}
     * @return a new analyzer, never {@code null}
     */
    @NotNull
    public static MigrationAnalyzer forBootstrap(@NotNull final DataFixerBootstrap bootstrap) {
        Preconditions.checkNotNull(bootstrap, "bootstrap must not be null");

        // Bootstrap into capturing registries
        final SchemaRegistry schemaRegistry = new SimpleSchemaRegistry();
        final DataFixerBuilder builder = new DataFixerBuilder(new DataVersion(Integer.MAX_VALUE));

        bootstrap.registerSchemas(schemaRegistry);
        bootstrap.registerFixes(builder);

        return new MigrationAnalyzer(schemaRegistry, builder.getFixRegistry());
    }

    /**
     * Creates an analyzer from pre-built registries.
     *
     * @param schemaRegistry the schema registry, must not be {@code null}
     * @param fixRegistry    the fix registry, must not be {@code null}
     * @return a new analyzer, never {@code null}
     */
    @NotNull
    public static MigrationAnalyzer forRegistries(
            @NotNull final SchemaRegistry schemaRegistry,
            @NotNull final DataFixRegistry fixRegistry
    ) {
        return new MigrationAnalyzer(schemaRegistry, fixRegistry);
    }

    /**
     * Sets the source version for the analysis.
     *
     * @param version the source version number
     * @return this analyzer for chaining
     */
    @NotNull
    public MigrationAnalyzer from(final int version) {
        return from(new DataVersion(version));
    }

    /**
     * Sets the source version for the analysis.
     *
     * @param version the source version, must not be {@code null}
     * @return this analyzer for chaining
     */
    @NotNull
    public MigrationAnalyzer from(@NotNull final DataVersion version) {
        this.fromVersion = Preconditions.checkNotNull(version, "version must not be null");
        return this;
    }

    /**
     * Sets the target version for the analysis.
     *
     * @param version the target version number
     * @return this analyzer for chaining
     */
    @NotNull
    public MigrationAnalyzer to(final int version) {
        return to(new DataVersion(version));
    }

    /**
     * Sets the target version for the analysis.
     *
     * @param version the target version, must not be {@code null}
     * @return this analyzer for chaining
     */
    @NotNull
    public MigrationAnalyzer to(@NotNull final DataVersion version) {
        this.toVersion = Preconditions.checkNotNull(version, "version must not be null");
        return this;
    }

    /**
     * Enables field-level analysis in schema diffs.
     *
     * @param include {@code true} to include field-level changes
     * @return this analyzer for chaining
     */
    @NotNull
    public MigrationAnalyzer includeFieldLevel(final boolean include) {
        this.includeFieldLevel = include;
        return this;
    }

    /**
     * Analyzes the migration path between the configured versions.
     *
     * @return the migration path, never {@code null}
     * @throws IllegalStateException if from/to versions are not set
     */
    @NotNull
    public MigrationPath analyze() {
        validateVersionRange();

        final List<Schema> schemas = getSchemasInRange();
        if (schemas.size() < 2) {
            return MigrationPath.empty();
        }

        final MigrationPath.Builder pathBuilder = MigrationPath.builder(this.fromVersion, this.toVersion);

        for (int i = 0; i < schemas.size() - 1; i++) {
            final Schema sourceSchema = schemas.get(i);
            final Schema targetSchema = schemas.get(i + 1);
            final MigrationStep step = analyzeStep(sourceSchema, targetSchema);
            pathBuilder.addStep(step);
        }

        return pathBuilder.build();
    }

    /**
     * Analyzes fix coverage for the migration between configured versions.
     *
     * @return the coverage analysis result, never {@code null}
     * @throws IllegalStateException if from/to versions are not set
     */
    @NotNull
    public FixCoverage analyzeCoverage() {
        validateVersionRange();

        final List<Schema> schemas = getSchemasInRange();
        if (schemas.size() < 2) {
            return FixCoverage.fullyCovered(this.fromVersion, this.toVersion);
        }

        final FixCoverage.Builder coverageBuilder = FixCoverage.builder(this.fromVersion, this.toVersion);

        for (int i = 0; i < schemas.size() - 1; i++) {
            final Schema sourceSchema = schemas.get(i);
            final Schema targetSchema = schemas.get(i + 1);
            analyzeStepCoverage(sourceSchema, targetSchema, coverageBuilder);
        }

        return coverageBuilder.build();
    }

    /**
     * Validates that version range is properly configured.
     */
    private void validateVersionRange() {
        Preconditions.checkState(this.fromVersion != null, "fromVersion must be set. Use from().");
        Preconditions.checkState(this.toVersion != null, "toVersion must be set. Use to().");
        Preconditions.checkState(
                this.fromVersion.compareTo(this.toVersion) <= 0,
                "fromVersion must be <= toVersion"
        );
    }

    /**
     * Gets all schemas in the version range, sorted by version.
     */
    @NotNull
    private List<Schema> getSchemasInRange() {
        final List<Schema> schemas = new ArrayList<>();

        for (final Schema schema : this.schemaRegistry.stream().toList()) {
            final int version = schema.version().getVersion();
            if (version >= this.fromVersion.getVersion() && version <= this.toVersion.getVersion()) {
                schemas.add(schema);
            }
        }

        schemas.sort(Comparator.comparingInt(s -> s.version().getVersion()));
        return schemas;
    }

    /**
     * Analyzes a single migration step between two schemas.
     */
    @NotNull
    private MigrationStep analyzeStep(
            @NotNull final Schema sourceSchema,
            @NotNull final Schema targetSchema
    ) {
        // Diff the schemas
        final SchemaDiff diff = SchemaDiffer.compare(sourceSchema, targetSchema)
                .includeFieldLevel(this.includeFieldLevel)
                .diff();

        // Collect affected types
        final Set<TypeReference> affectedTypes = new HashSet<>();
        affectedTypes.addAll(diff.addedTypes());
        affectedTypes.addAll(diff.removedTypes());

        // Add types with field changes
        for (final TypeReference commonType : diff.commonTypes()) {
            diff.typeDiff(commonType).ifPresent(typeDiff -> {
                if (typeDiff.hasFieldChanges()) {
                    affectedTypes.add(commonType);
                }
            });
        }

        // Find fixes for this version transition
        // Fixes are looked up for the source version
        final List<DataFix<?>> fixes = new ArrayList<>();
        for (final TypeReference ref : affectedTypes) {
            fixes.addAll(this.fixRegistry.getStepFixes(ref, sourceSchema.version()));
        }

        final MigrationStep.Builder stepBuilder = MigrationStep.builder(
                sourceSchema.version(),
                targetSchema.version()
        ).schemaDiff(diff)
                .affectedTypes(affectedTypes);

        // Add first fix if present (simplified - in reality there might be multiple)
        if (!fixes.isEmpty()) {
            stepBuilder.fix(fixes.get(0));
        }

        return stepBuilder.build();
    }

    /**
     * Analyzes coverage for a single step and adds gaps to the builder.
     */
    private void analyzeStepCoverage(
            @NotNull final Schema sourceSchema,
            @NotNull final Schema targetSchema,
            @NotNull final FixCoverage.Builder coverageBuilder
    ) {
        // Diff the schemas to find changes
        final SchemaDiff diff = SchemaDiffer.compare(sourceSchema, targetSchema)
                .includeFieldLevel(true)
                .diff();

        // Check coverage for added types
        for (final TypeReference addedType : diff.addedTypes()) {
            final List<DataFix<?>> fixes = this.fixRegistry.getStepFixes(addedType, sourceSchema.version());
            if (fixes.isEmpty()) {
                coverageBuilder.addGap(CoverageGap.typeLevel(
                        addedType,
                        sourceSchema.version(),
                        targetSchema.version(),
                        CoverageGap.Reason.TYPE_ADDED
                ));
            }
        }

        // Check coverage for removed types
        for (final TypeReference removedType : diff.removedTypes()) {
            final List<DataFix<?>> fixes = this.fixRegistry.getStepFixes(removedType, sourceSchema.version());
            if (fixes.isEmpty()) {
                coverageBuilder.addGap(CoverageGap.typeLevel(
                        removedType,
                        sourceSchema.version(),
                        targetSchema.version(),
                        CoverageGap.Reason.TYPE_REMOVED
                ));
            }
        }

        // Check coverage for modified types (field changes)
        for (final TypeReference commonType : diff.commonTypes()) {
            diff.typeDiff(commonType).ifPresent(typeDiff -> {
                checkTypeDiffCoverage(commonType, sourceSchema, targetSchema, typeDiff, coverageBuilder);
            });
        }
    }

    /**
     * Checks coverage for field-level changes in a type.
     */
    private void checkTypeDiffCoverage(
            @NotNull final TypeReference type,
            @NotNull final Schema sourceSchema,
            @NotNull final Schema targetSchema,
            @NotNull final TypeDiff typeDiff,
            @NotNull final FixCoverage.Builder coverageBuilder
    ) {
        if (!typeDiff.hasFieldChanges()) {
            return;
        }

        final List<DataFix<?>> fixes = this.fixRegistry.getStepFixes(type, sourceSchema.version());

        if (fixes.isEmpty()) {
            // No fix for this type at all
            coverageBuilder.addGap(CoverageGap.typeLevel(
                    type,
                    sourceSchema.version(),
                    targetSchema.version(),
                    CoverageGap.Reason.TYPE_MODIFIED,
                    typeDiff
            ));
        }
        // Note: A more sophisticated implementation would check if the fix
        // actually handles all the specific field changes
    }

}
