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

package de.splatgames.aether.datafixers.api.diagnostic;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A comprehensive report of a data migration operation.
 *
 * <p>{@code MigrationReport} contains all diagnostic information captured during
 * a migration, including timing, applied fixes, touched types, and optional
 * before/after snapshots. It provides a complete view of what happened during
 * the migration for debugging, logging, and analysis purposes.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DiagnosticContext context = DiagnosticContext.create();
 * Dynamic<?> result = fixer.update(type, input, fromVersion, toVersion, context);
 *
 * MigrationReport report = context.getReport();
 *
 * System.out.println("Migration completed in " + report.totalDuration().toMillis() + "ms");
 * System.out.println("Applied " + report.fixCount() + " fixes");
 *
 * for (FixExecution fix : report.fixExecutions()) {
 *     System.out.println("  - " + fix.toSummary());
 * }
 *
 * if (report.hasWarnings()) {
 *     System.out.println("Warnings:");
 *     report.warnings().forEach(w -> System.out.println("  ! " + w));
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be immutable and thread-safe. The {@link Builder}
 * is used during migration and then produces an immutable report.</p>
 *
 * @author Erik Pförtner
 * @see DiagnosticContext
 * @see FixExecution
 * @see RuleApplication
 * @since 0.2.0
 */
public interface MigrationReport {

    // -------------------------------------------------------------------------
    // Migration Metadata
    // -------------------------------------------------------------------------

    /**
     * Returns the type reference that was migrated.
     *
     * @return the type reference, never {@code null}
     */
    @NotNull
    TypeReference type();

    /**
     * Returns the source version the migration started from.
     *
     * @return the source version, never {@code null}
     */
    @NotNull
    DataVersion fromVersion();

    /**
     * Returns the target version the migration ended at.
     *
     * @return the target version, never {@code null}
     */
    @NotNull
    DataVersion toVersion();

    /**
     * Returns the instant when the migration started.
     *
     * @return the start time, never {@code null}
     */
    @NotNull
    Instant startTime();

    /**
     * Returns the instant when the migration completed.
     *
     * @return the end time, never {@code null}
     */
    @NotNull
    Instant endTime();

    /**
     * Returns the total duration of the migration.
     *
     * @return the total duration, never {@code null}
     */
    @NotNull
    Duration totalDuration();

    // -------------------------------------------------------------------------
    // Fix Executions
    // -------------------------------------------------------------------------

    /**
     * Returns the list of fix executions in order of application.
     *
     * @return unmodifiable list of fix executions, never {@code null}
     */
    @NotNull
    List<FixExecution> fixExecutions();

    /**
     * Returns the number of fixes that were applied.
     *
     * @return number of applied fixes
     */
    default int fixCount() {
        return this.fixExecutions().size();
    }

    /**
     * Returns the total number of rule applications across all fixes.
     *
     * @return total rule application count
     */
    default int ruleApplicationCount() {
        return this.fixExecutions().stream()
                .mapToInt(FixExecution::ruleCount)
                .sum();
    }

    // -------------------------------------------------------------------------
    // Touched Types
    // -------------------------------------------------------------------------

    /**
     * Returns the set of type references that were processed during migration.
     *
     * <p>This includes the primary type and any nested types that were
     * encountered during the migration.</p>
     *
     * @return unmodifiable set of touched type references, never {@code null}
     */
    @NotNull
    Set<TypeReference> touchedTypes();

    // -------------------------------------------------------------------------
    // Warnings
    // -------------------------------------------------------------------------

    /**
     * Returns whether any warnings were logged during migration.
     *
     * @return {@code true} if warnings were logged
     */
    default boolean hasWarnings() {
        return !this.warnings().isEmpty();
    }

    /**
     * Returns the list of warning messages logged during migration.
     *
     * @return unmodifiable list of warnings, never {@code null}
     */
    @NotNull
    List<String> warnings();

    // -------------------------------------------------------------------------
    // Snapshots
    // -------------------------------------------------------------------------

    /**
     * Returns the snapshot of input data before migration.
     *
     * @return optional containing the input snapshot, or empty if not captured
     */
    @NotNull
    Optional<String> inputSnapshot();

    /**
     * Returns the snapshot of output data after migration.
     *
     * @return optional containing the output snapshot, or empty if not captured
     */
    @NotNull
    Optional<String> outputSnapshot();

    // -------------------------------------------------------------------------
    // Summary
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable summary of the migration.
     *
     * @return formatted summary string
     */
    @NotNull
    default String toSummary() {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("Migration of '%s' from v%d to v%d: %dms, %d fixes",
                this.type().getId(),
                this.fromVersion().getVersion(),
                this.toVersion().getVersion(),
                this.totalDuration().toMillis(),
                this.fixCount()
        ));

        if (this.hasWarnings()) {
            sb.append(String.format(" (%d warnings)", this.warnings().size()));
        }

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Builder for constructing {@link MigrationReport} instances incrementally.
     *
     * <p>The builder is used internally by {@link DiagnosticContext} to capture
     * migration events as they occur. After the migration completes,
     * {@link #build()} produces an immutable report.</p>
     *
     * <h2>Usage Pattern</h2>
     * <pre>{@code
     * // Called by DataFixerImpl during migration
     * builder.startMigration(type, fromVersion, toVersion);
     * builder.setInputSnapshot(inputJson);
     *
     * for (DataFix fix : fixes) {
     *     builder.startFix(fix);
     *     // ... apply fix ...
     *     builder.endFix(fix, duration);
     * }
     *
     * builder.setOutputSnapshot(outputJson);
     * MigrationReport report = builder.build();
     * }</pre>
     */
    interface Builder {

        /**
         * Marks the start of a migration operation.
         *
         * @param type        the type reference being migrated
         * @param fromVersion the source version
         * @param toVersion   the target version
         * @return this builder
         */
        @NotNull
        Builder startMigration(
                @NotNull TypeReference type,
                @NotNull DataVersion fromVersion,
                @NotNull DataVersion toVersion
        );

        /**
         * Sets the input data snapshot.
         *
         * @param snapshot the JSON representation of input data
         * @return this builder
         */
        @NotNull
        Builder setInputSnapshot(@Nullable String snapshot);

        /**
         * Marks the start of a fix execution.
         *
         * @param fix the fix being applied
         * @return this builder
         */
        @NotNull
        Builder startFix(@NotNull DataFix<?> fix);

        /**
         * Sets the before snapshot for the current fix.
         *
         * @param snapshot the JSON representation before fix application
         * @return this builder
         */
        @NotNull
        Builder setFixBeforeSnapshot(@Nullable String snapshot);

        /**
         * Records a rule application within the current fix.
         *
         * @param application the rule application details
         * @return this builder
         */
        @NotNull
        Builder recordRuleApplication(@NotNull RuleApplication application);

        /**
         * Marks the end of a fix execution.
         *
         * @param fix           the fix that was applied
         * @param duration      the time taken to apply the fix
         * @param afterSnapshot optional snapshot after fix application
         * @return this builder
         */
        @NotNull
        Builder endFix(
                @NotNull DataFix<?> fix,
                @NotNull Duration duration,
                @Nullable String afterSnapshot
        );

        /**
         * Adds a touched type reference.
         *
         * @param type the type reference that was processed
         * @return this builder
         */
        @NotNull
        Builder addTouchedType(@NotNull TypeReference type);

        /**
         * Adds a warning message.
         *
         * @param message the warning message
         * @return this builder
         */
        @NotNull
        Builder addWarning(@NotNull String message);

        /**
         * Sets the output data snapshot.
         *
         * @param snapshot the JSON representation of output data
         * @return this builder
         */
        @NotNull
        Builder setOutputSnapshot(@Nullable String snapshot);

        /**
         * Builds the immutable migration report.
         *
         * <p>This method should be called after the migration completes.
         * The builder should not be used after this method is called.</p>
         *
         * @return the constructed migration report
         * @throws IllegalStateException if the migration was not properly started
         */
        @NotNull
        MigrationReport build();
    }
}
