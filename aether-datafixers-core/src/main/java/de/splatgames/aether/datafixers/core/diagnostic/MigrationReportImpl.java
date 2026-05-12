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

package de.splatgames.aether.datafixers.core.diagnostic;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.diagnostic.FixExecution;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import de.splatgames.aether.datafixers.api.diagnostic.RuleApplication;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of {@link MigrationReport}.
 *
 * <p>This implementation is immutable and thread-safe. It is constructed
 * using the nested {@link BuilderImpl} class.</p>
 *
 * @author Erik Pförtner
 * @see MigrationReport
 * @since 0.2.0
 */
public final class MigrationReportImpl implements MigrationReport {

    /** The type reference that was migrated. */
    @NotNull
    private final TypeReference type;

    /** The source version the migration started from. */
    @NotNull
    private final DataVersion fromVersion;

    /** The target version the migration ended at. */
    @NotNull
    private final DataVersion toVersion;

    /** The instant when the migration started. */
    @NotNull
    private final Instant startTime;

    /** The instant when the migration completed. */
    @NotNull
    private final Instant endTime;

    /** The ordered list of fix executions that were applied during migration. */
    @NotNull
    private final List<FixExecution> fixExecutions;

    /** The set of type references that were processed during migration. */
    @NotNull
    private final Set<TypeReference> touchedTypes;

    /** The list of warning messages logged during migration. */
    @NotNull
    private final List<String> warnings;

    /** The optional JSON snapshot of the input data before migration. */
    @Nullable
    private final String inputSnapshot;

    /** The optional JSON snapshot of the output data after migration. */
    @Nullable
    private final String outputSnapshot;

    /**
     * Constructs a new migration report from the given builder.
     *
     * <p>All mutable collections from the builder are defensively copied into
     * unmodifiable collections to ensure immutability of the resulting report.</p>
     *
     * @param builder the builder containing the accumulated diagnostic data; must not be {@code null}
     * @throws NullPointerException if any required field in the builder is {@code null}
     */
    private MigrationReportImpl(@NotNull final BuilderImpl builder) {
        this.type = Objects.requireNonNull(builder.type, "builder.type");
        this.fromVersion = Objects.requireNonNull(builder.fromVersion, "builder.fromVersion");
        this.toVersion = Objects.requireNonNull(builder.toVersion, "builder.toVersion");
        this.startTime = Objects.requireNonNull(builder.startTime, "builder.startTime");
        this.endTime = Objects.requireNonNull(builder.endTime, "builder.endTime");
        this.fixExecutions = List.copyOf(builder.fixExecutions);
        this.touchedTypes = Set.copyOf(builder.touchedTypes);
        this.warnings = List.copyOf(builder.warnings);
        this.inputSnapshot = builder.inputSnapshot;
        this.outputSnapshot = builder.outputSnapshot;
    }

    /**
     * {@inheritDoc}
     *
     * @return the type reference that was migrated; never {@code null}
     */
    @Override
    @NotNull
    public TypeReference type() {
        return this.type;
    }

    /**
     * {@inheritDoc}
     *
     * @return the source version; never {@code null}
     */
    @Override
    @NotNull
    public DataVersion fromVersion() {
        return this.fromVersion;
    }

    /**
     * {@inheritDoc}
     *
     * @return the target version; never {@code null}
     */
    @Override
    @NotNull
    public DataVersion toVersion() {
        return this.toVersion;
    }

    /**
     * {@inheritDoc}
     *
     * @return the start time; never {@code null}
     */
    @Override
    @NotNull
    public Instant startTime() {
        return this.startTime;
    }

    /**
     * {@inheritDoc}
     *
     * @return the end time; never {@code null}
     */
    @Override
    @NotNull
    public Instant endTime() {
        return this.endTime;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Computed as the duration between {@link #startTime()} and {@link #endTime()}.</p>
     *
     * @return the total migration duration; never {@code null}
     */
    @Override
    @NotNull
    public Duration totalDuration() {
        return Duration.between(this.startTime, this.endTime);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned list is unmodifiable.</p>
     *
     * @return unmodifiable list of fix executions in application order; never {@code null}
     */
    @Override
    @NotNull
    public List<FixExecution> fixExecutions() {
        return this.fixExecutions;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned set is unmodifiable and preserves insertion order.</p>
     *
     * @return unmodifiable set of touched type references; never {@code null}
     */
    @Override
    @NotNull
    public Set<TypeReference> touchedTypes() {
        return this.touchedTypes;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned list is unmodifiable.</p>
     *
     * @return unmodifiable list of warning messages; never {@code null}
     */
    @Override
    @NotNull
    public List<String> warnings() {
        return this.warnings;
    }

    /**
     * {@inheritDoc}
     *
     * @return optional containing the input snapshot, or empty if not captured
     */
    @Override
    @NotNull
    public Optional<String> inputSnapshot() {
        return Optional.ofNullable(this.inputSnapshot);
    }

    /**
     * {@inheritDoc}
     *
     * @return optional containing the output snapshot, or empty if not captured
     */
    @Override
    @NotNull
    public Optional<String> outputSnapshot() {
        return Optional.ofNullable(this.outputSnapshot);
    }

    /**
     * Creates a new builder for constructing migration reports.
     *
     * @return a new builder instance
     */
    @NotNull
    public static BuilderImpl builder() {
        return new BuilderImpl();
    }

    /**
     * Builder implementation for {@link MigrationReportImpl}.
     *
     * <p>This builder captures diagnostic events during migration and
     * produces an immutable report when {@link #build()} is called.</p>
     */
    public static final class BuilderImpl implements MigrationReport.Builder {

        // ---- Report fields (populated during migration) ----

        /** The type reference being migrated; set by {@link #startMigration}. */
        @Nullable
        private TypeReference type;

        /** The source version; set by {@link #startMigration}. */
        @Nullable
        private DataVersion fromVersion;

        /** The target version; set by {@link #startMigration}. */
        @Nullable
        private DataVersion toVersion;

        /** The instant when the migration started; set by {@link #startMigration}. */
        @Nullable
        private Instant startTime;

        /** The instant when the migration completed; set by {@link #build()}. */
        @Nullable
        private Instant endTime;

        /** The accumulated list of completed fix executions. */
        @NotNull
        private final List<FixExecution> fixExecutions = new ArrayList<>();

        /** The insertion-ordered set of type references processed during migration. */
        @NotNull
        private final Set<TypeReference> touchedTypes = new LinkedHashSet<>();

        /** The accumulated list of warning messages. */
        @NotNull
        private final List<String> warnings = new ArrayList<>();

        /** The optional JSON snapshot of the input data before migration. */
        @Nullable
        private String inputSnapshot;

        /** The optional JSON snapshot of the output data after migration. */
        @Nullable
        private String outputSnapshot;

        // ---- Current fix tracking (transient state during a single fix) ----

        /** The name of the fix currently being applied. */
        @Nullable
        private String currentFixName;

        /** The source version of the fix currently being applied. */
        @Nullable
        private DataVersion currentFixFromVersion;

        /** The target version of the fix currently being applied. */
        @Nullable
        private DataVersion currentFixToVersion;

        /** The instant when the current fix started. */
        @Nullable
        private Instant currentFixStartTime;

        /** The rule applications recorded for the fix currently being applied. */
        @NotNull
        private final List<RuleApplication> currentRuleApplications = new ArrayList<>();

        /** The optional JSON snapshot of the data before the current fix is applied. */
        @Nullable
        private String currentFixBeforeSnapshot;

        // ---- Lifecycle state ----

        /** Whether {@link #startMigration} has been called. */
        private boolean migrationStarted;

        /** Whether a fix is currently being tracked (between {@link #startFix} and {@link #endFix}). */
        private boolean fixInProgress;

        /** Whether {@link #build()} has been called, making this builder terminal. */
        private boolean built;

        /**
         * Creates a new, empty builder.
         */
        BuilderImpl() {
        }

        /**
         * {@inheritDoc}
         *
         * @param type        the type reference being migrated; must not be {@code null}
         * @param fromVersion the source version; must not be {@code null}
         * @param toVersion   the target version; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException  if any parameter is {@code null}
         * @throws IllegalStateException if the report was already built or migration was already started
         */
        @Override
        @NotNull
        public Builder startMigration(@NotNull final TypeReference type,
                                      @NotNull final DataVersion fromVersion,
                                      @NotNull final DataVersion toVersion) {
            Preconditions.checkNotNull(type, "type must not be null");
            Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
            Preconditions.checkNotNull(toVersion, "toVersion must not be null");
            Preconditions.checkState(!this.built, "Report already built");
            Preconditions.checkState(!this.migrationStarted, "Migration already started");

            this.migrationStarted = true;
            this.type = type;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.startTime = Instant.now();
            this.touchedTypes.add(type);
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @param snapshot the JSON representation of input data; may be {@code null}
         * @return this builder for method chaining; never {@code null}
         */
        @Override
        @NotNull
        public Builder setInputSnapshot(@Nullable final String snapshot) {
            this.inputSnapshot = snapshot;
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Initializes all current-fix tracking fields and clears any residual
         * state from a previous fix.</p>
         *
         * @param fix the fix that is about to be applied; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException  if {@code fix} is {@code null}
         * @throws IllegalStateException if the report was already built, migration was not started,
         *                               or a fix is already in progress
         */
        @Override
        @NotNull
        public Builder startFix(@NotNull final DataFix<?> fix) {
            Preconditions.checkNotNull(fix, "fix must not be null");
            Preconditions.checkState(!this.built, "Report already built");
            Preconditions.checkState(this.migrationStarted, "Migration not started");
            Preconditions.checkState(!this.fixInProgress,
                    "Fix already in progress: " + this.currentFixName);
            this.fixInProgress = true;
            this.currentFixName = fix.name();
            this.currentFixFromVersion = fix.fromVersion();
            this.currentFixToVersion = fix.toVersion();
            this.currentFixStartTime = Instant.now();
            this.currentRuleApplications.clear();
            this.currentFixBeforeSnapshot = null;
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @param snapshot the JSON representation of data before fix application; may be {@code null}
         * @return this builder for method chaining; never {@code null}
         */
        @Override
        @NotNull
        public Builder setFixBeforeSnapshot(@Nullable final String snapshot) {
            this.currentFixBeforeSnapshot = snapshot;
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @param application the rule application details to record; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException if {@code application} is {@code null}
         */
        @Override
        @NotNull
        public Builder recordRuleApplication(@NotNull final RuleApplication application) {
            Preconditions.checkNotNull(application, "application must not be null");
            this.currentRuleApplications.add(application);
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Finalizes the current fix execution by constructing a {@link FixExecution}
         * record from the accumulated tracking state and then resets the fix-tracking fields
         * via {@link #resetFixState()}.</p>
         *
         * @param fix           the fix that was applied; must not be {@code null}
         * @param duration      the total time taken to apply the fix; must not be {@code null}
         * @param afterSnapshot optional JSON representation of data after fix application; may be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException  if {@code fix} or {@code duration} is {@code null}
         * @throws IllegalStateException if no fix is currently in progress
         */
        @Override
        @NotNull
        public Builder endFix(@NotNull final DataFix<?> fix,
                              @NotNull final Duration duration,
                              @Nullable final String afterSnapshot) {
            Preconditions.checkNotNull(fix, "fix must not be null");
            Preconditions.checkState(this.fixInProgress, "No fix in progress");
            Preconditions.checkNotNull(duration, "duration must not be null");

            final FixExecution execution = new FixExecution(
                    Objects.requireNonNull(this.currentFixName, "currentFixName"),
                    Objects.requireNonNull(this.currentFixFromVersion, "currentFixFromVersion"),
                    Objects.requireNonNull(this.currentFixToVersion, "currentFixToVersion"),
                    Objects.requireNonNull(this.currentFixStartTime, "currentFixStartTime"),
                    duration,
                    new ArrayList<>(this.currentRuleApplications),
                    this.currentFixBeforeSnapshot,
                    afterSnapshot
            );

            this.fixExecutions.add(execution);

            // Reset current fix tracking
            this.resetFixState();

            return this;
        }

        /**
         * Resets fix-tracking state. Called after endFix() or on error paths
         * to prevent stale state from corrupting subsequent fix records.
         */
        private void resetFixState() {
            this.fixInProgress = false;
            this.currentFixName = null;
            this.currentFixFromVersion = null;
            this.currentFixToVersion = null;
            this.currentFixStartTime = null;
            this.currentRuleApplications.clear();
            this.currentFixBeforeSnapshot = null;
        }

        /**
         * {@inheritDoc}
         *
         * @param type the type reference that was processed; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException if {@code type} is {@code null}
         */
        @Override
        @NotNull
        public Builder addTouchedType(@NotNull final TypeReference type) {
            Preconditions.checkNotNull(type, "type must not be null");
            this.touchedTypes.add(type);
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @param message the warning message to record; must not be {@code null}
         * @return this builder for method chaining; never {@code null}
         * @throws NullPointerException if {@code message} is {@code null}
         */
        @Override
        @NotNull
        public Builder addWarning(@NotNull final String message) {
            Preconditions.checkNotNull(message, "message must not be null");
            this.warnings.add(message);
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @param snapshot the JSON representation of output data; may be {@code null}
         * @return this builder for method chaining; never {@code null}
         */
        @Override
        @NotNull
        public Builder setOutputSnapshot(@Nullable final String snapshot) {
            this.outputSnapshot = snapshot;
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Records the end time, marks this builder as terminal, and constructs
         * an immutable {@link MigrationReportImpl} from the accumulated state.</p>
         *
         * @return the constructed immutable migration report; never {@code null}
         * @throws IllegalStateException if the report was already built, a fix is still in progress,
         *                               or {@link #startMigration} was never called
         */
        @Override
        @NotNull
        public MigrationReport build() {
            Preconditions.checkState(!this.built, "Report already built");
            Preconditions.checkState(!this.fixInProgress,
                    "Cannot build report while fix is in progress: " + this.currentFixName);
            if (this.type == null) {
                throw new IllegalStateException(
                        "Migration was not started. Call startMigration() first."
                );
            }

            this.endTime = Instant.now();
            this.built = true;
            return new MigrationReportImpl(this);
        }
    }
}
