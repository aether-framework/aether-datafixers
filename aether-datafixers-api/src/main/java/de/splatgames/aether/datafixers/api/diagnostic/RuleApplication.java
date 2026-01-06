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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Details about a single {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule}
 * application during a migration.
 *
 * <p>{@code RuleApplication} captures diagnostic information about each rule
 * that is evaluated during a data fix, including whether the rule matched
 * and how long it took to execute.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MigrationReport report = context.getReport();
 * for (FixExecution fix : report.fixExecutions()) {
 *     for (RuleApplication rule : fix.ruleApplications()) {
 *         System.out.println(rule.ruleName() + ": " +
 *             (rule.matched() ? "matched" : "skipped") +
 *             " in " + rule.duration().toMillis() + "ms");
 *     }
 * }
 * }</pre>
 *
 * @param ruleName    the name of the rule (from {@code TypeRewriteRule.toString()})
 * @param typeName    the name of the type being processed
 * @param timestamp   the instant when the rule was applied
 * @param duration    the time taken to apply the rule
 * @param matched     whether the rule matched and transformed the data
 * @param description optional additional description or context
 * @author Erik Pförtner
 * @see FixExecution
 * @see MigrationReport
 * @since 0.2.0
 */
public record RuleApplication(
        @NotNull String ruleName,
        @NotNull String typeName,
        @NotNull Instant timestamp,
        @NotNull Duration duration,
        boolean matched,
        @Nullable String description
) {

    /**
     * Creates a new rule application record.
     *
     * @param ruleName    the name of the rule, must not be {@code null}
     * @param typeName    the name of the type being processed, must not be {@code null}
     * @param timestamp   the instant when the rule was applied, must not be {@code null}
     * @param duration    the time taken to apply the rule, must not be {@code null}
     * @param matched     whether the rule matched and transformed the data
     * @param description optional additional description (may be {@code null})
     * @throws NullPointerException if any required parameter is {@code null}
     */
    public RuleApplication {
        if (ruleName == null) {
            throw new NullPointerException("ruleName must not be null");
        }
        if (typeName == null) {
            throw new NullPointerException("typeName must not be null");
        }
        if (timestamp == null) {
            throw new NullPointerException("timestamp must not be null");
        }
        if (duration == null) {
            throw new NullPointerException("duration must not be null");
        }
    }

    /**
     * Creates a new rule application record without a description.
     *
     * @param ruleName  the name of the rule, must not be {@code null}
     * @param typeName  the name of the type being processed, must not be {@code null}
     * @param timestamp the instant when the rule was applied, must not be {@code null}
     * @param duration  the time taken to apply the rule, must not be {@code null}
     * @param matched   whether the rule matched and transformed the data
     * @return the new rule application record
     */
    @NotNull
    public static RuleApplication of(
            @NotNull final String ruleName,
            @NotNull final String typeName,
            @NotNull final Instant timestamp,
            @NotNull final Duration duration,
            final boolean matched
    ) {
        return new RuleApplication(ruleName, typeName, timestamp, duration, matched, null);
    }

    /**
     * Returns the description as an {@link Optional}.
     *
     * @return optional containing the description, or empty if none
     */
    @NotNull
    public Optional<String> descriptionOpt() {
        return Optional.ofNullable(this.description);
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
     * Returns a human-readable summary of this rule application.
     *
     * @return formatted summary string
     */
    @NotNull
    public String toSummary() {
        return String.format("%s on %s: %s in %dms",
                this.ruleName,
                this.typeName,
                this.matched ? "matched" : "skipped",
                this.durationMillis()
        );
    }
}
