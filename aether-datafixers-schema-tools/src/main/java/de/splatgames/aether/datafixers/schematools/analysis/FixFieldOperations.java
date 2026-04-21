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
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Field-level operations performed by a single {@link de.splatgames.aether.datafixers.api.fix.DataFix} during a
 * migration step, captured via static introspection.
 *
 * <p>This record is the per-fix unit of a {@link FieldOperationReport}. It carries
 * the fix's name, the version range it migrates between, and the list of {@link FieldOperation field operations}
 * extracted from its rule via {@link de.splatgames.aether.datafixers.core.fix.SchemaDataFix#introspectRule}.</p>
 *
 * <h2>Introspectable vs. Opaque Fixes</h2>
 * <p>Only fixes extending
 * {@link de.splatgames.aether.datafixers.core.fix.SchemaDataFix SchemaDataFix} can be statically introspected. Other
 * {@link de.splatgames.aether.datafixers.api.fix.DataFix DataFix} implementations are recorded with
 * {@code introspectable=false} and an empty operations list — their behavior cannot be determined without running
 * them.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(100).to(200)
 *     .analyzeFieldOperations();
 *
 * for (FixFieldOperations fix : report.fixOperations()) {
 *     if (!fix.introspectable()) {
 *         System.out.println("Opaque fix: " + fix.fixName());
 *         continue;
 *     }
 *     System.out.println(fix.toSummary());
 *     fix.operations().forEach(op -> System.out.println("  " + op.toSummary()));
 * }
 * }</pre>
 *
 * @param fixName        the name of the fix (from {@code DataFix.name()}), must not be {@code null}
 * @param fromVersion    the source version this fix migrates from, must not be {@code null}
 * @param toVersion      the target version this fix migrates to, must not be {@code null}
 * @param operations     the field operations performed by the fix's rule, must not be {@code null}; empty if the fix is
 *                       not introspectable or has no field-aware rules
 * @param introspectable whether the fix could be statically introspected; {@code false} for non-{@code SchemaDataFix}
 *                       implementations
 * @author Erik Pförtner
 * @see FieldOperationReport
 * @see FieldOperation
 * @see de.splatgames.aether.datafixers.core.fix.SchemaDataFix#introspectRule
 * @since 1.0.0
 */
public record FixFieldOperations(@NotNull String fixName,
                                 @NotNull DataVersion fromVersion,
                                 @NotNull DataVersion toVersion,
                                 @NotNull List<FieldOperation> operations,
                                 boolean introspectable) {

    /**
     * Compact constructor enforcing non-null arguments and creating a defensive copy of the operations list.
     *
     * @param fixName        the fix name, must not be {@code null}
     * @param fromVersion    the source version, must not be {@code null}
     * @param toVersion      the target version, must not be {@code null}
     * @param operations     the field operations, must not be {@code null}
     * @param introspectable whether this fix is introspectable
     * @throws NullPointerException if any required parameter is {@code null}
     */
    public FixFieldOperations {
        Preconditions.checkNotNull(fixName, "fixName must not be null");
        Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        Preconditions.checkNotNull(operations, "operations must not be null");
        operations = List.copyOf(operations);
    }

    /**
     * Creates a record for an introspectable fix with the given operations.
     *
     * @param fixName     the fix name, must not be {@code null}
     * @param fromVersion the source version, must not be {@code null}
     * @param toVersion   the target version, must not be {@code null}
     * @param operations  the captured field operations, must not be {@code null}
     * @return a new introspectable record, never {@code null}
     */
    @NotNull
    public static FixFieldOperations introspectable(@NotNull final String fixName,
                                                    @NotNull final DataVersion fromVersion,
                                                    @NotNull final DataVersion toVersion,
                                                    @NotNull final List<FieldOperation> operations) {
        return new FixFieldOperations(fixName, fromVersion, toVersion, operations, true);
    }

    /**
     * Creates a record for an opaque (non-introspectable) fix.
     *
     * <p>Used for {@link de.splatgames.aether.datafixers.api.fix.DataFix} implementations
     * that do not extend {@link de.splatgames.aether.datafixers.core.fix.SchemaDataFix} and therefore cannot expose
     * their rules statically.</p>
     *
     * @param fixName     the fix name, must not be {@code null}
     * @param fromVersion the source version, must not be {@code null}
     * @param toVersion   the target version, must not be {@code null}
     * @return a new opaque record with an empty operations list, never {@code null}
     */
    @NotNull
    public static FixFieldOperations opaque(@NotNull final String fixName,
                                            @NotNull final DataVersion fromVersion,
                                            @NotNull final DataVersion toVersion) {
        return new FixFieldOperations(fixName, fromVersion, toVersion, List.of(), false);
    }

    /**
     * Returns the number of field operations performed by this fix.
     *
     * @return the operation count, always non-negative
     */
    public int count() {
        return this.operations.size();
    }

    /**
     * Returns whether this fix has any field operations.
     *
     * @return {@code true} if the operations list is non-empty
     */
    public boolean hasOperations() {
        return !this.operations.isEmpty();
    }

    /**
     * Returns a human-readable summary of this fix's field operations.
     *
     * @return a formatted summary string, never {@code null}
     */
    @NotNull
    public String toSummary() {
        if (!this.introspectable) {
            return String.format("%s (v%d -> v%d): opaque",
                    this.fixName,
                    this.fromVersion.getVersion(),
                    this.toVersion.getVersion());
        }
        return String.format("%s (v%d -> v%d): %d field operations",
                this.fixName,
                this.fromVersion.getVersion(),
                this.toVersion.getVersion(),
                this.operations.size());
    }
}
