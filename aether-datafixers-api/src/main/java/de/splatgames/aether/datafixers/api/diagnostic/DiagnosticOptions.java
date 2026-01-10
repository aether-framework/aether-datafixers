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

/**
 * Configuration options for migration diagnostics.
 *
 * <p>{@code DiagnosticOptions} controls what diagnostic data is captured
 * during a migration operation. This allows fine-tuning the balance between diagnostic detail and performance/memory
 * overhead.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DiagnosticOptions options = DiagnosticOptions.builder()
 *     .captureSnapshots(true)
 *     .captureRuleDetails(true)
 *     .maxSnapshotLength(5000)
 *     .prettyPrintSnapshots(true)
 *     .build();
 *
 * DiagnosticContext context = DiagnosticContext.create(options);
 * }</pre>
 *
 * <h2>Presets</h2>
 * <ul>
 *   <li>{@link #defaults()} - Full diagnostics with snapshots and rule details</li>
 *   <li>{@link #minimal()} - Only timing information, no snapshots or rule details</li>
 * </ul>
 *
 * @param captureSnapshots     whether to capture before/after data snapshots
 * @param captureRuleDetails   whether to capture individual rule application details
 * @param maxSnapshotLength    maximum length for snapshot strings (0 for unlimited)
 * @param prettyPrintSnapshots whether to format snapshots for readability
 * @author Erik Pförtner
 * @see DiagnosticContext
 * @see MigrationReport
 * @since 0.2.0
 */
public record DiagnosticOptions(
        boolean captureSnapshots,
        boolean captureRuleDetails,
        int maxSnapshotLength,
        boolean prettyPrintSnapshots
) {

    /**
     * Default maximum snapshot length.
     */
    public static final int DEFAULT_MAX_SNAPSHOT_LENGTH = 10000;

    /**
     * Creates default diagnostic options with full diagnostics enabled.
     *
     * <p>Default settings:</p>
     * <ul>
     *   <li>{@code captureSnapshots} = {@code true}</li>
     *   <li>{@code captureRuleDetails} = {@code true}</li>
     *   <li>{@code maxSnapshotLength} = {@code 10000}</li>
     *   <li>{@code prettyPrintSnapshots} = {@code true}</li>
     * </ul>
     *
     * @return default diagnostic options
     */
    @NotNull
    public static DiagnosticOptions defaults() {
        return new DiagnosticOptions(true, true, DEFAULT_MAX_SNAPSHOT_LENGTH, true);
    }

    /**
     * Creates minimal diagnostic options with only timing information.
     *
     * <p>Minimal settings:</p>
     * <ul>
     *   <li>{@code captureSnapshots} = {@code false}</li>
     *   <li>{@code captureRuleDetails} = {@code false}</li>
     *   <li>{@code maxSnapshotLength} = {@code 0}</li>
     *   <li>{@code prettyPrintSnapshots} = {@code false}</li>
     * </ul>
     *
     * @return minimal diagnostic options
     */
    @NotNull
    public static DiagnosticOptions minimal() {
        return new DiagnosticOptions(false, false, 0, false);
    }

    /**
     * Creates a new builder for constructing diagnostic options.
     *
     * @return a new builder instance
     */
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DiagnosticOptions} instances.
     *
     * <p>All settings default to the values from {@link DiagnosticOptions#defaults()}.</p>
     */
    public static final class Builder {

        private boolean captureSnapshots = true;
        private boolean captureRuleDetails = true;
        private int maxSnapshotLength = DEFAULT_MAX_SNAPSHOT_LENGTH;
        private boolean prettyPrintSnapshots = true;

        private Builder() {
        }

        /**
         * Sets whether to capture before/after data snapshots.
         *
         * <p>When enabled, the migration report will include JSON representations
         * of the data before and after each fix is applied. This is useful for debugging but increases memory
         * usage.</p>
         *
         * @param captureSnapshots {@code true} to capture snapshots
         * @return this builder
         */
        @NotNull
        public Builder captureSnapshots(final boolean captureSnapshots) {
            this.captureSnapshots = captureSnapshots;
            return this;
        }

        /**
         * Sets whether to capture individual rule application details.
         *
         * <p>When enabled, the migration report will include details about each
         * {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule} application, including timing and match
         * status.</p>
         *
         * @param captureRuleDetails {@code true} to capture rule details
         * @return this builder
         */
        @NotNull
        public Builder captureRuleDetails(final boolean captureRuleDetails) {
            this.captureRuleDetails = captureRuleDetails;
            return this;
        }

        /**
         * Sets the maximum length for snapshot strings.
         *
         * <p>Snapshots exceeding this length will be truncated with a
         * {@code "... (truncated)"} suffix. Set to 0 for unlimited length.</p>
         *
         * @param maxSnapshotLength maximum length in characters (0 for unlimited)
         * @return this builder
         * @throws IllegalArgumentException if maxSnapshotLength is negative
         */
        @NotNull
        public Builder maxSnapshotLength(final int maxSnapshotLength) {
            if (maxSnapshotLength < 0) {
                throw new IllegalArgumentException("maxSnapshotLength must be non-negative");
            }
            this.maxSnapshotLength = maxSnapshotLength;
            return this;
        }

        /**
         * Sets whether to format snapshots for readability.
         *
         * <p>When enabled, JSON snapshots will be pretty-printed with indentation.
         * When disabled, snapshots will be compact single-line JSON.</p>
         *
         * @param prettyPrintSnapshots {@code true} to pretty-print snapshots
         * @return this builder
         */
        @NotNull
        public Builder prettyPrintSnapshots(final boolean prettyPrintSnapshots) {
            this.prettyPrintSnapshots = prettyPrintSnapshots;
            return this;
        }

        /**
         * Builds the diagnostic options.
         *
         * @return the constructed diagnostic options
         */
        @NotNull
        public DiagnosticOptions build() {
            return new DiagnosticOptions(
                    this.captureSnapshots,
                    this.captureRuleDetails,
                    this.maxSnapshotLength,
                    this.prettyPrintSnapshots
            );
        }
    }
}
