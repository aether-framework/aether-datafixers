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
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The aggregated result of statically analyzing field-level operations across a migration path.
 *
 * <p>A {@code FieldOperationReport} is produced by
 * {@link MigrationAnalyzer#analyzeFieldOperations()} and contains, for each fix in the configured version range, the
 * {@link FieldOperation} entries that the fix's rule would perform — extracted statically without running any data
 * through the fixer.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * FieldOperationReport report = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(100).to(200)
 *     .analyzeFieldOperations();
 *
 * System.out.println(report.toSummary());
 * System.out.println("Affected fields: " + report.affectedFieldPaths());
 *
 * // Find every rename across the migration
 * report.operationsOfType(FieldOperationType.RENAME).forEach(op ->
 *     System.out.println(op.fieldPathString() + " -> " + op.targetFieldName()));
 *
 * // Detect non-introspectable fixes
 * if (!report.isFullyIntrospectable()) {
 *     System.out.println(report.opaqueFixCount() + " fixes could not be analyzed");
 * }
 * }</pre>
 *
 * <h2>Limitations</h2>
 * <p>Only fixes extending
 * {@link de.splatgames.aether.datafixers.core.fix.SchemaDataFix SchemaDataFix} can be statically introspected. Other
 * {@link de.splatgames.aether.datafixers.api.fix.DataFix DataFix} implementations are counted in
 * {@link #opaqueFixCount()} and represented as opaque entries in {@link #fixOperations()} with no operations
 * attached.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe. All collection accessors return
 * unmodifiable views.</p>
 *
 * @author Erik Pförtner
 * @see MigrationAnalyzer#analyzeFieldOperations()
 * @see FixFieldOperations
 * @see FieldOperation
 * @since 1.0.0
 */
public final class FieldOperationReport {

    /**
     * Singleton empty report used as a no-op result for empty version ranges.
     */
    private static final FieldOperationReport EMPTY = new FieldOperationReport(
            new DataVersion(0), new DataVersion(0), List.of()
    );

    /**
     * The overall source version of the analyzed migration range.
     */
    private final DataVersion fromVersion;

    /**
     * The overall target version of the analyzed migration range.
     */
    private final DataVersion toVersion;

    /**
     * The per-fix field operation entries in migration order. Immutable.
     */
    private final List<FixFieldOperations> fixOperations;

    /**
     * Cached count of fixes that were successfully introspected. Pre-computed for efficient access.
     */
    private final int introspectedFixCount;

    /**
     * Cached count of fixes that could not be introspected (opaque). Pre-computed for efficient access.
     */
    private final int opaqueFixCount;

    /**
     * Cached total number of field operations across all introspected fixes. Pre-computed for efficient access.
     */
    private final int totalFieldOperationCount;

    /**
     * Creates a new immutable {@code FieldOperationReport}.
     *
     * <p>This constructor computes aggregate values (introspected count, opaque
     * count, total operation count) from the provided fix entries for efficient query access.</p>
     *
     * @param fromVersion   the overall source version, must not be {@code null}
     * @param toVersion     the overall target version, must not be {@code null}
     * @param fixOperations the per-fix entries in migration order, must not be {@code null}
     */
    private FieldOperationReport(@NotNull final DataVersion fromVersion,
                                 @NotNull final DataVersion toVersion,
                                 @NotNull final List<FixFieldOperations> fixOperations) {
        this.fromVersion = Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        this.toVersion = Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        this.fixOperations = List.copyOf(
                Preconditions.checkNotNull(fixOperations, "fixOperations must not be null"));

        int introspected = 0;
        int opaque = 0;
        int totalOps = 0;
        for (final FixFieldOperations entry : this.fixOperations) {
            if (entry.introspectable()) {
                introspected++;
                totalOps += entry.count();
            } else {
                opaque++;
            }
        }
        this.introspectedFixCount = introspected;
        this.opaqueFixCount = opaque;
        this.totalFieldOperationCount = totalOps;
    }

    /**
     * Returns the singleton empty report instance.
     *
     * @return an empty report, never {@code null}
     */
    @NotNull
    public static FieldOperationReport empty() {
        return EMPTY;
    }

    /**
     * Creates a new report from the given fix entries.
     *
     * @param fromVersion   the overall source version, must not be {@code null}
     * @param toVersion     the overall target version, must not be {@code null}
     * @param fixOperations the per-fix entries in migration order, must not be {@code null}
     * @return a new report instance, never {@code null}
     */
    @NotNull
    public static FieldOperationReport of(@NotNull final DataVersion fromVersion,
                                          @NotNull final DataVersion toVersion,
                                          @NotNull final List<FixFieldOperations> fixOperations) {
        return new FieldOperationReport(fromVersion, toVersion, fixOperations);
    }

    /**
     * Creates a new builder for constructing a report incrementally.
     *
     * @param fromVersion the source version, must not be {@code null}
     * @param toVersion   the target version, must not be {@code null}
     * @return a new builder, never {@code null}
     */
    @NotNull
    public static Builder builder(@NotNull final DataVersion fromVersion,
                                  @NotNull final DataVersion toVersion) {
        Preconditions.checkNotNull(fromVersion, "fromVersion must not be null");
        Preconditions.checkNotNull(toVersion, "toVersion must not be null");
        return new Builder(fromVersion, toVersion);
    }

    /**
     * Returns the overall source version of the analyzed migration range.
     *
     * @return the source version, never {@code null}
     */
    @NotNull
    public DataVersion fromVersion() {
        return this.fromVersion;
    }

    /**
     * Returns the overall target version of the analyzed migration range.
     *
     * @return the target version, never {@code null}
     */
    @NotNull
    public DataVersion toVersion() {
        return this.toVersion;
    }

    /**
     * Returns the per-fix field operation entries in migration order.
     *
     * @return an unmodifiable list of fix entries, never {@code null}
     */
    @NotNull
    public List<FixFieldOperations> fixOperations() {
        return this.fixOperations;
    }

    /**
     * Returns the number of fixes that were successfully introspected (i.e., extended
     * {@link de.splatgames.aether.datafixers.core.fix.SchemaDataFix}).
     *
     * @return the introspected fix count, always non-negative
     */
    public int introspectedFixCount() {
        return this.introspectedFixCount;
    }

    /**
     * Returns the number of fixes that could not be introspected.
     *
     * <p>An opaque fix is any {@link de.splatgames.aether.datafixers.api.fix.DataFix}
     * implementation that does not extend {@link de.splatgames.aether.datafixers.core.fix.SchemaDataFix} and therefore
     * cannot expose its rule for static analysis.</p>
     *
     * @return the opaque fix count, always non-negative
     */
    public int opaqueFixCount() {
        return this.opaqueFixCount;
    }

    /**
     * Returns the total number of field operations across all introspected fixes.
     *
     * @return the total operation count, always non-negative
     */
    public int totalFieldOperationCount() {
        return this.totalFieldOperationCount;
    }

    /**
     * Returns whether every fix in the analyzed range could be introspected.
     *
     * @return {@code true} if {@link #opaqueFixCount()} is zero
     */
    public boolean isFullyIntrospectable() {
        return this.opaqueFixCount == 0;
    }

    /**
     * Returns whether this report contains no fix entries at all.
     *
     * @return {@code true} if {@link #fixOperations()} is empty
     */
    public boolean isEmpty() {
        return this.fixOperations.isEmpty();
    }

    /**
     * Returns a flat list of all field operations across all introspected fixes, preserving the migration order.
     *
     * @return an unmodifiable list of all field operations, never {@code null}
     */
    @NotNull
    public List<FieldOperation> allFieldOperations() {
        final List<FieldOperation> result = new ArrayList<>(this.totalFieldOperationCount);
        for (final FixFieldOperations entry : this.fixOperations) {
            result.addAll(entry.operations());
        }
        return List.copyOf(result);
    }

    /**
     * Returns all field operations of the specified type, across all fixes.
     *
     * @param type the operation type to filter by, must not be {@code null}
     * @return an unmodifiable list of matching operations, never {@code null}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    @NotNull
    public List<FieldOperation> operationsOfType(@NotNull final FieldOperationType type) {
        Preconditions.checkNotNull(type, "type must not be null");
        return allFieldOperations().stream()
                .filter(op -> op.operationType() == type)
                .toList();
    }

    /**
     * Returns the set of dot-notation field paths affected by any operation in the report. Useful for answering "which
     * fields does this migration touch?".
     *
     * <p>For example, an operation on path {@code ["position", "x"]} contributes
     * the entry {@code "position.x"} to the result.</p>
     *
     * @return an unmodifiable set of dot-notation field paths, preserving insertion order, never {@code null}
     */
    @NotNull
    public Set<String> affectedFieldPaths() {
        final Set<String> paths = new LinkedHashSet<>();
        for (final FieldOperation op : allFieldOperations()) {
            paths.add(op.fieldPathString());
        }
        return Set.copyOf(paths);
    }

    /**
     * Returns a human-readable summary of this report.
     *
     * @return a formatted summary string, never {@code null}
     */
    @NotNull
    public String toSummary() {
        return String.format(
                "FieldOperationReport v%d -> v%d: %d fixes introspected, %d opaque, %d field operations",
                this.fromVersion.getVersion(),
                this.toVersion.getVersion(),
                this.introspectedFixCount,
                this.opaqueFixCount,
                this.totalFieldOperationCount);
    }

    /**
     * Builder for constructing {@link FieldOperationReport} instances incrementally.
     *
     * <p>The builder is intended for use by {@link MigrationAnalyzer} as it walks
     * the configured version range and processes fixes one by one. It is not thread-safe — each analysis run should use
     * its own builder.</p>
     */
    public static final class Builder {

        /**
         * The overall source and target versions for the report being built. These are fixed at builder construction
         * time and cannot be changed later.
         */
        private final DataVersion fromVersion;
        /**
         * The overall target version for the report being built.
         */
        private final DataVersion toVersion;
        /**
         * The per-fix field operation entries collected so far, in migration order.
         */
        private final List<FixFieldOperations> fixOperations = new ArrayList<>();

        /**
         * Creates a new builder for the given version range.
         *
         * @param fromVersion the source version
         * @param toVersion   the target version
         */
        private Builder(@NotNull final DataVersion fromVersion,
                        @NotNull final DataVersion toVersion) {
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
        }

        /**
         * Adds a fix entry to the report being built.
         *
         * @param entry the fix entry to add, must not be {@code null}
         * @return this builder for method chaining, never {@code null}
         * @throws NullPointerException if {@code entry} is {@code null}
         */
        @NotNull
        public Builder addFix(@NotNull final FixFieldOperations entry) {
            Preconditions.checkNotNull(entry, "entry must not be null");
            this.fixOperations.add(entry);
            return this;
        }

        /**
         * Builds the immutable report.
         *
         * @return the constructed report, never {@code null}
         */
        @NotNull
        public FieldOperationReport build() {
            return new FieldOperationReport(this.fromVersion, this.toVersion, this.fixOperations);
        }
    }
}
