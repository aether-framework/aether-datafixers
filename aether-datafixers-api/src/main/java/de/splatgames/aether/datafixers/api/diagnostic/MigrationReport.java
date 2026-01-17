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
 * a migration, including timing, applied fixes, touched types, and optional before/after snapshots. It provides a
 * complete view of what happened during the migration for debugging, logging, and analysis purposes.</p>
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
     * migration events as they occur. After the migration completes, {@link #build()} produces an immutable
     * report.</p>
     *
     * <p><b>Lifecycle</b></p>
     * <p>The builder follows a specific lifecycle that must be adhered to:</p>
     * <ol>
     *   <li>Call {@link #startMigration(TypeReference, DataVersion, DataVersion)} exactly once</li>
     *   <li>Optionally call {@link #setInputSnapshot(String)} to capture input data</li>
     *   <li>For each fix: call {@link #startFix(DataFix)}, record events, then {@link #endFix(DataFix, Duration, String)}</li>
     *   <li>Optionally call {@link #setOutputSnapshot(String)} to capture output data</li>
     *   <li>Call {@link #build()} exactly once to produce the report</li>
     * </ol>
     *
     * <p><b>Usage Pattern</b></p>
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
     *
     * <p><b>Thread Safety</b></p>
     * <p>This builder is not thread-safe. It should be used by a single thread
     * during the course of a migration operation. The resulting {@link MigrationReport}
     * is immutable and thread-safe.</p>
     */
    interface Builder {

        /**
         * Marks the start of a migration operation.
         *
         * <p>This method must be called exactly once before any other builder methods
         * (except for {@link #build()}). It initializes the migration metadata including the type being migrated and
         * the version range.</p>
         *
         * <h4>Timing</h4>
         * <p>The start time is recorded when this method is called, which is used to
         * calculate the total migration duration in the final report.</p>
         *
         * @param type        the type reference being migrated; must not be {@code null}
         * @param fromVersion the source version from which the migration starts; must not be {@code null}
         * @param toVersion   the target version to which the migration proceeds; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException  if any parameter is {@code null}
         * @throws IllegalStateException if migration was already started
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
         * <p>This method captures a JSON representation of the data before any fixes
         * are applied. This is useful for debugging to compare the original input with the final output or intermediate
         * states.</p>
         *
         * <h4>Snapshot Format</h4>
         * <p>The snapshot is typically a JSON string representation of the input data.
         * Depending on the {@link DiagnosticOptions}, it may be pretty-printed or compact, and may be truncated if it
         * exceeds the maximum snapshot length.</p>
         *
         * @param snapshot the JSON representation of input data; may be {@code null} to indicate no snapshot should be
         *                 captured
         * @return this builder for method chaining; never {@code null}
         */
        @NotNull
        Builder setInputSnapshot(@Nullable String snapshot);

        /**
         * Marks the start of a fix execution.
         *
         * <p>This method should be called immediately before a {@link DataFix} is applied.
         * It records the start time for the fix and prepares the builder to accept rule application events and
         * snapshots for this fix.</p>
         *
         * <h4>Pairing Requirement</h4>
         * <p>Each call to {@code startFix} must be followed by exactly one call to
         * {@link #endFix(DataFix, Duration, String)} for the same fix. Nested or overlapping fix executions are not
         * supported.</p>
         *
         * @param fix the fix that is about to be applied; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException  if {@code fix} is {@code null}
         * @throws IllegalStateException if a fix is already in progress
         * @see #endFix(DataFix, Duration, String)
         */
        @NotNull
        Builder startFix(@NotNull DataFix<?> fix);

        /**
         * Sets the before snapshot for the current fix.
         *
         * <p>This method captures the state of the data immediately before the current
         * fix is applied. It must be called after {@link #startFix(DataFix)} and before
         * {@link #endFix(DataFix, Duration, String)}.</p>
         *
         * @param snapshot the JSON representation of data before fix application; may be {@code null} to indicate no
         *                 snapshot should be captured
         * @return this builder for method chaining; never {@code null}
         * @throws IllegalStateException if no fix is currently in progress
         */
        @NotNull
        Builder setFixBeforeSnapshot(@Nullable String snapshot);

        /**
         * Records a rule application within the current fix.
         *
         * <p>This method records details about an individual
         * {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule}
         * application, including whether it matched and transformed the data. Multiple rule applications can be
         * recorded for a single fix.</p>
         *
         * <h4>Recording Order</h4>
         * <p>Rule applications are recorded in the order they are added and will appear
         * in that order in the final {@link FixExecution#ruleApplications()} list.</p>
         *
         * @param application the rule application details to record; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException  if {@code application} is {@code null}
         * @throws IllegalStateException if no fix is currently in progress
         * @see RuleApplication
         */
        @NotNull
        Builder recordRuleApplication(@NotNull RuleApplication application);

        /**
         * Marks the end of a fix execution.
         *
         * <p>This method should be called immediately after a {@link DataFix} has been
         * applied. It finalizes the fix execution record with the total duration and an optional after snapshot.</p>
         *
         * <h4>Pairing Requirement</h4>
         * <p>This method must be called after {@link #startFix(DataFix)} for the same fix.
         * The fix parameter should match the one passed to {@code startFix}.</p>
         *
         * @param fix           the fix that was applied; must not be {@code null}
         * @param duration      the total time taken to apply the fix; must not be {@code null}
         * @param afterSnapshot optional JSON representation of data after fix application; may be {@code null} to
         *                      indicate no snapshot should be captured
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException  if {@code fix} or {@code duration} is {@code null}
         * @throws IllegalStateException if no fix is currently in progress or if the fix does not match the one
         *                               started
         * @see #startFix(DataFix)
         */
        @NotNull
        Builder endFix(
                @NotNull DataFix<?> fix,
                @NotNull Duration duration,
                @Nullable String afterSnapshot
        );

        /**
         * Adds a touched type reference to the report.
         *
         * <p>This method records that a particular type was encountered and processed
         * during the migration. The touched types set includes the primary type and any nested or referenced types that
         * were traversed.</p>
         *
         * <h4>Deduplication</h4>
         * <p>Adding the same type reference multiple times has no additional effect;
         * each type appears at most once in the final {@link MigrationReport#touchedTypes()} set.</p>
         *
         * @param type the type reference that was processed; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException if {@code type} is {@code null}
         */
        @NotNull
        Builder addTouchedType(@NotNull TypeReference type);

        /**
         * Adds a warning message to the report.
         *
         * <p>This method records a warning that occurred during migration. Warnings are
         * non-fatal issues that did not prevent the migration from completing but may indicate potential problems or
         * unexpected conditions.</p>
         *
         * <h4>Common Warnings</h4>
         * <ul>
         *   <li>Missing optional fields that were expected</li>
         *   <li>Deprecated field formats that were auto-converted</li>
         *   <li>Rules that matched but produced unexpected results</li>
         * </ul>
         *
         * @param message the warning message to record; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException if {@code message} is {@code null}
         */
        @NotNull
        Builder addWarning(@NotNull String message);

        /**
         * Sets the output data snapshot.
         *
         * <p>This method captures a JSON representation of the data after all fixes
         * have been applied. This is useful for debugging to compare the final output with the original input.</p>
         *
         * <h4>Snapshot Format</h4>
         * <p>The snapshot is typically a JSON string representation of the output data.
         * Depending on the {@link DiagnosticOptions}, it may be pretty-printed or compact, and may be truncated if it
         * exceeds the maximum snapshot length.</p>
         *
         * @param snapshot the JSON representation of output data; may be {@code null} to indicate no snapshot should be
         *                 captured
         * @return this builder for method chaining; never {@code null}
         */
        @NotNull
        Builder setOutputSnapshot(@Nullable String snapshot);

        /**
         * Builds the immutable migration report.
         *
         * <p>This method finalizes the report construction, recording the end time and
         * producing an immutable {@link MigrationReport} instance. The builder transitions to a terminal state after
         * this method is called.</p>
         *
         * <h4>Builder State After Build</h4>
         * <p>After calling this method, the builder should not be used for any further
         * operations. Implementations may throw {@link IllegalStateException} if any builder methods are called after
         * {@code build()}.</p>
         *
         * <h4>Validation</h4>
         * <p>This method validates that the migration was properly started via
         * {@link #startMigration(TypeReference, DataVersion, DataVersion)} and that all started fixes have been
         * properly ended.</p>
         *
         * @return the constructed immutable migration report; never {@code null}
         * @throws IllegalStateException if the migration was not properly started or if there are unclosed fix
         *                               executions
         */
        @NotNull
        MigrationReport build();
    }
}
