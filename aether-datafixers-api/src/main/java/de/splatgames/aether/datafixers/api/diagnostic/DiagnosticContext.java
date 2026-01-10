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

import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;

/**
 * A diagnostic-aware context for capturing detailed migration information.
 *
 * <p>{@code DiagnosticContext} extends {@link DataFixerContext} with capabilities
 * for capturing comprehensive diagnostic data during migration operations. When passed to a data fixer, it enables
 * opt-in collection of timing information, applied fixes, rule applications, and data snapshots.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a diagnostic context with default options
 * DiagnosticContext context = DiagnosticContext.create();
 *
 * // Or with custom options
 * DiagnosticContext context = DiagnosticContext.create(
 *     DiagnosticOptions.builder()
 *         .captureSnapshots(true)
 *         .captureRuleDetails(true)
 *         .build()
 * );
 *
 * // Run migration with diagnostics
 * Dynamic<?> result = fixer.update(type, input, fromVersion, toVersion, context);
 *
 * // Access the report
 * MigrationReport report = context.getReport();
 * System.out.println(report.toSummary());
 * }</pre>
 *
 * <h2>Opt-in Behavior</h2>
 * <p>Diagnostic collection is opt-in. Only when a {@code DiagnosticContext}
 * is explicitly passed to the data fixer will diagnostic data be captured. This ensures zero overhead for normal
 * operations.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be designed for single-threaded use during a
 * single migration operation. The resulting {@link MigrationReport} is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataFixerContext
 * @see MigrationReport
 * @see DiagnosticOptions
 * @since 0.2.0
 */
public interface DiagnosticContext extends DataFixerContext {

    /**
     * Creates a new diagnostic context with default options.
     *
     * <p>Default options enable full diagnostic capture including snapshots
     * and rule details. See {@link DiagnosticOptions#defaults()}.</p>
     *
     * @return a new diagnostic context
     */
    @NotNull
    static DiagnosticContext create() {
        return create(DiagnosticOptions.defaults());
    }

    /**
     * Creates a new diagnostic context with the specified options.
     *
     * @param options the diagnostic options
     * @return a new diagnostic context
     * @throws NullPointerException if options is {@code null}
     */
    @NotNull
    static DiagnosticContext create(@NotNull final DiagnosticOptions options) {
        // Use ServiceLoader or direct instantiation
        // For now, we'll use direct instantiation via the core module
        // This will be resolved at runtime by the core implementation
        try {
            final Class<?> implClass = Class.forName(
                    "de.splatgames.aether.datafixers.core.diagnostic.DiagnosticContextImpl"
            );
            return (DiagnosticContext) implClass
                    .getConstructor(DiagnosticOptions.class)
                    .newInstance(options);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to create DiagnosticContext. " +
                            "Ensure aether-datafixers-core is on the classpath.",
                    e
            );
        }
    }

    /**
     * Returns whether diagnostic collection is enabled.
     *
     * <p>This method allows the data fixer to check if it should emit
     * diagnostic events. For {@code DiagnosticContext} implementations, this typically returns {@code true}.</p>
     *
     * @return {@code true} if diagnostics are enabled
     */
    boolean isDiagnosticEnabled();

    /**
     * Returns the report builder for recording diagnostic events.
     *
     * <p>This method is intended for internal use by the data fixer
     * implementation to record events during migration.</p>
     *
     * @return the report builder, never {@code null}
     */
    @NotNull
    MigrationReport.Builder reportBuilder();

    /**
     * Returns the completed migration report.
     *
     * <p>This method should be called after the migration completes to
     * retrieve the diagnostic information. Calling this method finalizes the report building process.</p>
     *
     * @return the migration report, never {@code null}
     * @throws IllegalStateException if called before migration starts
     */
    @NotNull
    MigrationReport getReport();

    /**
     * Returns the diagnostic options controlling what data is captured.
     *
     * @return the diagnostic options, never {@code null}
     */
    @NotNull
    DiagnosticOptions options();
}
