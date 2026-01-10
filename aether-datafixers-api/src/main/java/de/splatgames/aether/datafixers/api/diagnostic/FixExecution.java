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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Details about a single {@link de.splatgames.aether.datafixers.api.fix.DataFix} execution during a migration.
 *
 * <p>{@code FixExecution} captures comprehensive diagnostic information about
 * each data fix that is applied during a migration, including timing, rule applications, and optional before/after
 * snapshots.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MigrationReport report = context.getReport();
 * for (FixExecution fix : report.fixExecutions()) {
 *     System.out.println(fix.fixName() + " (" +
 *         fix.fromVersion() + " -> " + fix.toVersion() + "): " +
 *         fix.duration().toMillis() + "ms");
 *
 *     for (RuleApplication rule : fix.ruleApplications()) {
 *         System.out.println("  - " + rule.toSummary());
 *     }
 * }
 * }</pre>
 *
 * @param fixName          the name of the fix (from {@code DataFix.name()})
 * @param fromVersion      the source version this fix migrates from
 * @param toVersion        the target version this fix migrates to
 * @param startTime        the instant when the fix started
 * @param duration         the total time taken to apply the fix
 * @param ruleApplications list of individual rule applications within this fix
 * @param beforeSnapshot   optional snapshot of data before the fix was applied
 * @param afterSnapshot    optional snapshot of data after the fix was applied
 * @author Erik Pförtner
 * @see RuleApplication
 * @see MigrationReport
 * @since 0.2.0
 */
public record FixExecution(
        @NotNull String fixName,
        @NotNull DataVersion fromVersion,
        @NotNull DataVersion toVersion,
        @NotNull Instant startTime,
        @NotNull Duration duration,
        @NotNull List<RuleApplication> ruleApplications,
        @Nullable String beforeSnapshot,
        @Nullable String afterSnapshot
) {

    /**
     * Creates a new fix execution record.
     *
     * @param fixName          the name of the fix, must not be {@code null}
     * @param fromVersion      the source version, must not be {@code null}
     * @param toVersion        the target version, must not be {@code null}
     * @param startTime        the start instant, must not be {@code null}
     * @param duration         the duration, must not be {@code null}
     * @param ruleApplications the rule applications, must not be {@code null}
     * @param beforeSnapshot   optional before snapshot (may be {@code null})
     * @param afterSnapshot    optional after snapshot (may be {@code null})
     * @throws NullPointerException if any required parameter is {@code null}
     */
    public FixExecution {
        Preconditions.checkNotNull(fixName, "fixName must not be null");
        Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        Preconditions.checkNotNull(startTime, "startTime must not be null");
        Preconditions.checkNotNull(duration, "duration must not be null");
        Preconditions.checkNotNull(ruleApplications, "ruleApplications must not be null");
        // Defensive copy to ensure immutability
        ruleApplications = List.copyOf(ruleApplications);
    }

    /**
     * Returns the before snapshot as an {@link Optional}.
     *
     * @return optional containing the before snapshot, or empty if not captured
     */
    @NotNull
    public Optional<String> beforeSnapshotOpt() {
        return Optional.ofNullable(this.beforeSnapshot);
    }

    /**
     * Returns the after snapshot as an {@link Optional}.
     *
     * @return optional containing the after snapshot, or empty if not captured
     */
    @NotNull
    public Optional<String> afterSnapshotOpt() {
        return Optional.ofNullable(this.afterSnapshot);
    }

    /**
     * Returns the duration in milliseconds.
     *
     * @return duration in milliseconds
     */
    public long durationMillis() {
        return this.duration.toMillis();
    }

    /**
     * Returns the number of rule applications in this fix.
     *
     * @return number of rule applications
     */
    public int ruleCount() {
        return this.ruleApplications.size();
    }

    /**
     * Returns the number of rules that matched (transformed data).
     *
     * @return number of matched rules
     */
    public int matchedRuleCount() {
        return (int) this.ruleApplications.stream()
                .filter(RuleApplication::matched)
                .count();
    }

    /**
     * Returns a human-readable summary of this fix execution.
     *
     * @return formatted summary string
     */
    @NotNull
    public String toSummary() {
        return String.format("%s (v%d -> v%d): %dms, %d rules (%d matched)",
                this.fixName,
                this.fromVersion.getVersion(),
                this.toVersion.getVersion(),
                this.durationMillis(),
                this.ruleCount(),
                this.matchedRuleCount()
        );
    }
}
