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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The result of analyzing a migration path between versions.
 *
 * <p>A MigrationPath contains all steps required to migrate data from a source
 * version to a target version. It provides convenience methods for analyzing
 * the path, such as finding affected types and steps with fixes.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(1).to(5)
 *     .analyze();
 *
 * // Iterate steps
 * for (MigrationStep step : path.steps()) {
 *     System.out.println(step);
 * }
 *
 * // Get all affected types
 * Set<TypeReference> affected = path.affectedTypes();
 *
 * // Find steps affecting a specific type
 * List<MigrationStep> playerSteps = path.stepsAffecting(TypeReferences.PLAYER);
 *
 * // Check path properties
 * System.out.println("Steps: " + path.stepCount());
 * System.out.println("Fixes: " + path.fixCount());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see MigrationStep
 * @see MigrationAnalyzer
 * @since 0.3.0
 */
public final class MigrationPath {

    /**
     * Singleton instance representing an empty migration path.
     * Used to avoid unnecessary object creation for no-op migrations.
     */
    private static final MigrationPath EMPTY = new MigrationPath(
            new DataVersion(0), new DataVersion(0), List.of()
    );

    /**
     * The overall source version for this migration path.
     */
    private final DataVersion sourceVersion;

    /**
     * The overall target version for this migration path.
     */
    private final DataVersion targetVersion;

    /**
     * The ordered list of migration steps from source to target version.
     * This is an immutable copy of the provided steps.
     */
    private final List<MigrationStep> steps;

    /**
     * The aggregated set of all types affected by any step in the path.
     * Pre-computed for efficient {@link #affects(TypeReference)} queries.
     */
    private final Set<TypeReference> affectedTypes;

    /**
     * The cached count of steps that have an associated DataFix.
     * Pre-computed for efficient {@link #fixCount()} access.
     */
    private final int fixCount;

    /**
     * Creates a new immutable MigrationPath instance.
     *
     * <p>This constructor computes aggregate values (affected types, fix count)
     * from the provided steps for efficient query access.</p>
     *
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @param steps         the migration steps in order, must not be {@code null}
     */
    private MigrationPath(
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @NotNull final List<MigrationStep> steps
    ) {
        this.sourceVersion = Preconditions.checkNotNull(sourceVersion, "sourceVersion must not be null");
        this.targetVersion = Preconditions.checkNotNull(targetVersion, "targetVersion must not be null");
        this.steps = List.copyOf(Preconditions.checkNotNull(steps, "steps must not be null"));

        // Compute aggregated values
        final Set<TypeReference> types = new HashSet<>();
        int fixes = 0;
        for (final MigrationStep step : this.steps) {
            types.addAll(step.affectedTypes());
            if (step.hasFix()) {
                fixes++;
            }
        }
        this.affectedTypes = Set.copyOf(types);
        this.fixCount = fixes;
    }

    /**
     * Returns an empty migration path.
     *
     * @return an empty path, never {@code null}
     */
    @NotNull
    public static MigrationPath empty() {
        return EMPTY;
    }

    /**
     * Creates a migration path from the given steps.
     *
     * @param sourceVersion the overall source version, must not be {@code null}
     * @param targetVersion the overall target version, must not be {@code null}
     * @param steps         the migration steps in order, must not be {@code null}
     * @return a new MigrationPath, never {@code null}
     */
    @NotNull
    public static MigrationPath of(
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion,
            @NotNull final List<MigrationStep> steps
    ) {
        return new MigrationPath(sourceVersion, targetVersion, steps);
    }

    /**
     * Creates a new builder for constructing a MigrationPath.
     *
     * @param sourceVersion the source version, must not be {@code null}
     * @param targetVersion the target version, must not be {@code null}
     * @return a new builder, never {@code null}
     */
    @NotNull
    public static Builder builder(
            @NotNull final DataVersion sourceVersion,
            @NotNull final DataVersion targetVersion
    ) {
        return new Builder(sourceVersion, targetVersion);
    }

    /**
     * Returns the overall source version.
     *
     * @return the source version, never {@code null}
     */
    @NotNull
    public DataVersion sourceVersion() {
        return this.sourceVersion;
    }

    /**
     * Returns the overall target version.
     *
     * @return the target version, never {@code null}
     */
    @NotNull
    public DataVersion targetVersion() {
        return this.targetVersion;
    }

    /**
     * Returns all migration steps in order.
     *
     * @return an unmodifiable list of steps, never {@code null}
     */
    @NotNull
    public List<MigrationStep> steps() {
        return this.steps;
    }

    /**
     * Returns all types affected anywhere in the path.
     *
     * @return an unmodifiable set of affected types, never {@code null}
     */
    @NotNull
    public Set<TypeReference> affectedTypes() {
        return this.affectedTypes;
    }

    /**
     * Returns the number of steps in the path.
     *
     * @return the step count
     */
    public int stepCount() {
        return this.steps.size();
    }

    /**
     * Returns the number of steps that have fixes.
     *
     * @return the fix count
     */
    public int fixCount() {
        return this.fixCount;
    }

    /**
     * Checks if the path is empty (no steps).
     *
     * @return {@code true} if no steps
     */
    public boolean isEmpty() {
        return this.steps.isEmpty();
    }

    /**
     * Checks if any type is affected by this migration path.
     *
     * @return {@code true} if at least one type is affected
     */
    public boolean hasChanges() {
        return !this.affectedTypes.isEmpty();
    }

    /**
     * Returns the first step in the path.
     *
     * @return an Optional containing the first step, empty if path is empty
     */
    @NotNull
    public Optional<MigrationStep> firstStep() {
        return this.steps.isEmpty() ? Optional.empty() : Optional.of(this.steps.get(0));
    }

    /**
     * Returns the last step in the path.
     *
     * @return an Optional containing the last step, empty if path is empty
     */
    @NotNull
    public Optional<MigrationStep> lastStep() {
        return this.steps.isEmpty() ? Optional.empty() : Optional.of(this.steps.get(this.steps.size() - 1));
    }

    /**
     * Returns all steps that affect a specific type.
     *
     * @param type the type to filter by, must not be {@code null}
     * @return an unmodifiable list of steps affecting the type, never {@code null}
     */
    @NotNull
    public List<MigrationStep> stepsAffecting(@NotNull final TypeReference type) {
        Preconditions.checkNotNull(type, "type must not be null");
        return this.steps.stream()
                .filter(step -> step.affects(type))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all steps that have an associated fix.
     *
     * @return an unmodifiable list of steps with fixes, never {@code null}
     */
    @NotNull
    public List<MigrationStep> stepsWithFixes() {
        return this.steps.stream()
                .filter(MigrationStep::hasFix)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all steps without an associated fix.
     *
     * <p>These steps represent schema changes without corresponding DataFixes.</p>
     *
     * @return an unmodifiable list of steps without fixes, never {@code null}
     */
    @NotNull
    public List<MigrationStep> stepsWithoutFixes() {
        return this.steps.stream()
                .filter(step -> !step.hasFix())
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Checks if a specific type is affected anywhere in the path.
     *
     * @param type the type to check, must not be {@code null}
     * @return {@code true} if the type is affected
     */
    public boolean affects(@NotNull final TypeReference type) {
        Preconditions.checkNotNull(type, "type must not be null");
        return this.affectedTypes.contains(type);
    }

    @Override
    public String toString() {
        return String.format(
                "MigrationPath[%d -> %d, %d steps, %d fixes, %d types affected]",
                this.sourceVersion.getVersion(),
                this.targetVersion.getVersion(),
                this.steps.size(),
                this.fixCount,
                this.affectedTypes.size()
        );
    }

    /**
     * Builder for creating {@link MigrationPath} instances.
     */
    public static final class Builder {

        private final DataVersion sourceVersion;
        private final DataVersion targetVersion;
        private final List<MigrationStep> steps = new ArrayList<>();

        private Builder(
                @NotNull final DataVersion sourceVersion,
                @NotNull final DataVersion targetVersion
        ) {
            this.sourceVersion = Preconditions.checkNotNull(sourceVersion, "sourceVersion must not be null");
            this.targetVersion = Preconditions.checkNotNull(targetVersion, "targetVersion must not be null");
        }

        /**
         * Adds a step to the path.
         *
         * @param step the step to add, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder addStep(@NotNull final MigrationStep step) {
            Preconditions.checkNotNull(step, "step must not be null");
            this.steps.add(step);
            return this;
        }

        /**
         * Adds multiple steps to the path.
         *
         * @param steps the steps to add, must not be {@code null}
         * @return this builder for chaining
         */
        @NotNull
        public Builder addSteps(@NotNull final Iterable<MigrationStep> steps) {
            Preconditions.checkNotNull(steps, "steps must not be null");
            for (final MigrationStep step : steps) {
                this.steps.add(step);
            }
            return this;
        }

        /**
         * Builds the MigrationPath.
         *
         * @return the constructed path, never {@code null}
         */
        @NotNull
        public MigrationPath build() {
            return new MigrationPath(this.sourceVersion, this.targetVersion, this.steps);
        }
    }
}
