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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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

    private final TypeReference type;
    private final DataVersion fromVersion;
    private final DataVersion toVersion;
    private final Instant startTime;
    private final Instant endTime;
    private final List<FixExecution> fixExecutions;
    private final Set<TypeReference> touchedTypes;
    private final List<String> warnings;
    private final String inputSnapshot;
    private final String outputSnapshot;

    private MigrationReportImpl(final BuilderImpl builder) {
        this.type = builder.type;
        this.fromVersion = builder.fromVersion;
        this.toVersion = builder.toVersion;
        this.startTime = builder.startTime;
        this.endTime = Instant.now();
        this.fixExecutions = List.copyOf(builder.fixExecutions);
        this.touchedTypes = Set.copyOf(builder.touchedTypes);
        this.warnings = List.copyOf(builder.warnings);
        this.inputSnapshot = builder.inputSnapshot;
        this.outputSnapshot = builder.outputSnapshot;
    }

    @Override
    @NotNull
    public TypeReference type() {
        return this.type;
    }

    @Override
    @NotNull
    public DataVersion fromVersion() {
        return this.fromVersion;
    }

    @Override
    @NotNull
    public DataVersion toVersion() {
        return this.toVersion;
    }

    @Override
    @NotNull
    public Instant startTime() {
        return this.startTime;
    }

    @Override
    @NotNull
    public Instant endTime() {
        return this.endTime;
    }

    @Override
    @NotNull
    public Duration totalDuration() {
        return Duration.between(this.startTime, this.endTime);
    }

    @Override
    @NotNull
    public List<FixExecution> fixExecutions() {
        return this.fixExecutions;
    }

    @Override
    @NotNull
    public Set<TypeReference> touchedTypes() {
        return this.touchedTypes;
    }

    @Override
    @NotNull
    public List<String> warnings() {
        return this.warnings;
    }

    @Override
    @NotNull
    public Optional<String> inputSnapshot() {
        return Optional.ofNullable(this.inputSnapshot);
    }

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

        private TypeReference type;
        private DataVersion fromVersion;
        private DataVersion toVersion;
        private Instant startTime;
        private final List<FixExecution> fixExecutions = new ArrayList<>();
        private final Set<TypeReference> touchedTypes = new LinkedHashSet<>();
        private final List<String> warnings = new ArrayList<>();
        private String inputSnapshot;
        private String outputSnapshot;

        // Current fix tracking
        private String currentFixName;
        private DataVersion currentFixFromVersion;
        private DataVersion currentFixToVersion;
        private Instant currentFixStartTime;
        private final List<RuleApplication> currentRuleApplications = new ArrayList<>();
        private String currentFixBeforeSnapshot;

        BuilderImpl() {
        }

        @Override
        @NotNull
        public Builder startMigration(
                @NotNull final TypeReference type,
                @NotNull final DataVersion fromVersion,
                @NotNull final DataVersion toVersion
        ) {
            if (type == null) {
                throw new NullPointerException("type must not be null");
            }
            if (fromVersion == null) {
                throw new NullPointerException("fromVersion must not be null");
            }
            if (toVersion == null) {
                throw new NullPointerException("toVersion must not be null");
            }

            this.type = type;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.startTime = Instant.now();
            this.touchedTypes.add(type);
            return this;
        }

        @Override
        @NotNull
        public Builder setInputSnapshot(@Nullable final String snapshot) {
            this.inputSnapshot = snapshot;
            return this;
        }

        @Override
        @NotNull
        public Builder startFix(@NotNull final DataFix<?> fix) {
            if (fix == null) {
                throw new NullPointerException("fix must not be null");
            }

            this.currentFixName = fix.name();
            this.currentFixFromVersion = fix.fromVersion();
            this.currentFixToVersion = fix.toVersion();
            this.currentFixStartTime = Instant.now();
            this.currentRuleApplications.clear();
            this.currentFixBeforeSnapshot = null;
            return this;
        }

        @Override
        @NotNull
        public Builder setFixBeforeSnapshot(@Nullable final String snapshot) {
            this.currentFixBeforeSnapshot = snapshot;
            return this;
        }

        @Override
        @NotNull
        public Builder recordRuleApplication(@NotNull final RuleApplication application) {
            if (application == null) {
                throw new NullPointerException("application must not be null");
            }

            this.currentRuleApplications.add(application);
            return this;
        }

        @Override
        @NotNull
        public Builder endFix(
                @NotNull final DataFix<?> fix,
                @NotNull final Duration duration,
                @Nullable final String afterSnapshot
        ) {
            if (fix == null) {
                throw new NullPointerException("fix must not be null");
            }
            if (duration == null) {
                throw new NullPointerException("duration must not be null");
            }

            final FixExecution execution = new FixExecution(
                    this.currentFixName,
                    this.currentFixFromVersion,
                    this.currentFixToVersion,
                    this.currentFixStartTime,
                    duration,
                    new ArrayList<>(this.currentRuleApplications),
                    this.currentFixBeforeSnapshot,
                    afterSnapshot
            );

            this.fixExecutions.add(execution);

            // Reset current fix tracking
            this.currentFixName = null;
            this.currentFixFromVersion = null;
            this.currentFixToVersion = null;
            this.currentFixStartTime = null;
            this.currentRuleApplications.clear();
            this.currentFixBeforeSnapshot = null;

            return this;
        }

        @Override
        @NotNull
        public Builder addTouchedType(@NotNull final TypeReference type) {
            if (type == null) {
                throw new NullPointerException("type must not be null");
            }

            this.touchedTypes.add(type);
            return this;
        }

        @Override
        @NotNull
        public Builder addWarning(@NotNull final String message) {
            if (message == null) {
                throw new NullPointerException("message must not be null");
            }

            this.warnings.add(message);
            return this;
        }

        @Override
        @NotNull
        public Builder setOutputSnapshot(@Nullable final String snapshot) {
            this.outputSnapshot = snapshot;
            return this;
        }

        @Override
        @NotNull
        public MigrationReport build() {
            if (this.type == null) {
                throw new IllegalStateException(
                        "Migration was not started. Call startMigration() first."
                );
            }

            return new MigrationReportImpl(this);
        }
    }
}
