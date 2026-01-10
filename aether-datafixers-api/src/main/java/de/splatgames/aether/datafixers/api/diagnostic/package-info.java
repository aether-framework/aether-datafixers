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

/**
 * Migration diagnostics API for capturing detailed information about data migrations.
 *
 * <p>This package provides interfaces and records for opt-in diagnostic capture
 * during data migration operations. When enabled, the diagnostic system collects comprehensive information
 * including:</p>
 *
 * <ul>
 *   <li>Timing information for the overall migration and individual fixes</li>
 *   <li>Details about each applied {@link de.splatgames.aether.datafixers.api.fix.DataFix}</li>
 *   <li>Individual {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule} applications</li>
 *   <li>Before/after data snapshots for debugging</li>
 *   <li>Warnings and diagnostic messages</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 *
 * <dl>
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}</dt>
 *   <dd>The main entry point - an extended context that enables diagnostic capture</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.MigrationReport}</dt>
 *   <dd>The complete diagnostic report produced after a migration</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.FixExecution}</dt>
 *   <dd>Details about a single fix execution</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.RuleApplication}</dt>
 *   <dd>Details about a single rule application</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions}</dt>
 *   <dd>Configuration for controlling what data is captured</dd>
 * </dl>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a diagnostic context
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
 * System.out.println("Migration took: " + report.totalDuration().toMillis() + "ms");
 * System.out.println("Fixes applied: " + report.fixCount());
 *
 * for (FixExecution fix : report.fixExecutions()) {
 *     System.out.println("  " + fix.toSummary());
 * }
 * }</pre>
 *
 * <h2>Opt-in Design</h2>
 * <p>Diagnostics are completely opt-in. Normal migration operations using a
 * standard {@link de.splatgames.aether.datafixers.api.fix.DataFixerContext}
 * have zero overhead. Diagnostic data is only collected when a
 * {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}
 * is explicitly passed to the data fixer.</p>
 *
 * @author Erik Pförtner
 * @since 0.2.0
 */
package de.splatgames.aether.datafixers.api.diagnostic;
