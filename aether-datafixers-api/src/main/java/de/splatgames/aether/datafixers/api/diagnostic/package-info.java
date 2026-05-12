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
 * Migration diagnostics API for capturing detailed information about data
 * migrations.
 *
 * <p>This package provides interfaces and records for opt-in diagnostic
 * capture during data migration operations. When enabled, the diagnostic
 * system collects comprehensive information including:</p>
 *
 * <ul>
 *   <li>Timing information for the overall migration and each individual
 *       fix.</li>
 *   <li>Details about each applied
 *       {@link de.splatgames.aether.datafixers.api.fix.DataFix}.</li>
 *   <li>Per-rule
 *       {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule}
 *       applications with match/skip status and duration.</li>
 *   <li>Structured field-level operation metadata from
 *       {@link de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule}
 *       implementations (new in 1.0.0).</li>
 *   <li>Optional before/after data snapshots for debugging.</li>
 *   <li>Warning messages emitted during migration.</li>
 * </ul>
 *
 * <h2>Key Components</h2>
 *
 * <dl>
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}</dt>
 *   <dd>Entry point: a
 *       {@link de.splatgames.aether.datafixers.api.fix.DataFixerContext}
 *       extension that enables diagnostic capture when passed to
 *       {@link de.splatgames.aether.datafixers.api.fix.DataFixer#update(de.splatgames.aether.datafixers.api.TypeReference, de.splatgames.aether.datafixers.api.dynamic.Dynamic, de.splatgames.aether.datafixers.api.DataVersion, de.splatgames.aether.datafixers.api.DataVersion, de.splatgames.aether.datafixers.api.fix.DataFixerContext) DataFixer.update}.</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.MigrationReport}</dt>
 *   <dd>Immutable diagnostic report produced by a completed migration.</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.FixExecution}</dt>
 *   <dd>Record capturing details about a single fix execution, including its
 *       {@link de.splatgames.aether.datafixers.api.diagnostic.RuleApplication RuleApplication}s.</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.RuleApplication}</dt>
 *   <dd>Record capturing details about a single rule application inside a
 *       fix.</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions}</dt>
 *   <dd>Configuration record controlling what gets captured (snapshots, rule
 *       details, field-level details, max snapshot length, pretty printing).</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.FieldOperation}</dt>
 *   <dd>Structured metadata about a single field-level operation within a
 *       rule (since 1.0.0).</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType}</dt>
 *   <dd>Enum classifying the kind of field-level operation (rename, remove,
 *       add, transform, set, move, copy, group, flatten, conditional).</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContextFactory}</dt>
 *   <dd>{@link java.util.ServiceLoader}-discovered factory for supplying a
 *       {@code DiagnosticContext} implementation. The
 *       {@code aether-datafixers-core} module registers one by default.</dd>
 * </dl>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build a diagnostic context; use defaults() for full capture, minimal()
 * // for timing-only, or the builder for fine-grained control.
 * DiagnosticContext context = DiagnosticContext.create(
 *     DiagnosticOptions.builder()
 *         .captureSnapshots(true)
 *         .captureRuleDetails(true)
 *         .captureFieldDetails(true)  // since 1.0.0
 *         .build());
 *
 * // Run the migration, passing the context as the DataFixerContext
 * Dynamic<?> result = fixer.update(type, input, fromVersion, toVersion, context);
 *
 * // Inspect the completed report
 * MigrationReport report = context.getReport();
 * System.out.println(report.toSummary());
 *
 * for (FixExecution fix : report.fixExecutions()) {
 *     System.out.println("  " + fix.toSummary());
 *     for (RuleApplication rule : fix.ruleApplications()) {
 *         for (FieldOperation op : rule.fieldOperations()) {
 *             System.out.println("    " + op.toSummary());
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Opt-in Design</h2>
 * <p>Diagnostics are completely opt-in. A migration run with a standard
 * {@link de.splatgames.aether.datafixers.api.fix.DataFixerContext} incurs no
 * diagnostic overhead. Diagnostic data is only collected when a
 * {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}
 * is explicitly passed to
 * {@link de.splatgames.aether.datafixers.api.fix.DataFixer#update DataFixer.update}.</p>
 *
 * <h2>Field-Level Capture (1.0.0)</h2>
 * <p>Rules built with {@link de.splatgames.aether.datafixers.api.rewrite.Rules}
 * implement {@link de.splatgames.aether.datafixers.api.rewrite.FieldAwareRule}
 * and expose structured
 * {@link de.splatgames.aether.datafixers.api.diagnostic.FieldOperation}
 * metadata. With
 * {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions#captureFieldDetails() captureFieldDetails}
 * enabled (the default for
 * {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions#defaults() defaults()}),
 * the report attributes every field change to the exact rule that caused it.</p>
 *
 * @author Erik Pförtner
 * @since 0.2.0
 */
package de.splatgames.aether.datafixers.api.diagnostic;
