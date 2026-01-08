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
 * Migration path and fix coverage analysis.
 *
 * <p>This package provides tools for analyzing migration paths between schema
 * versions and verifying that all type changes are covered by appropriate
 * DataFix implementations.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.analysis.MigrationAnalyzer} -
 *       Fluent entry point for migration analysis</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.analysis.MigrationPath} -
 *       Complete migration path between versions</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.analysis.MigrationStep} -
 *       Single step in a migration path</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.analysis.FixCoverage} -
 *       Analysis of fix coverage for schema changes</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.analysis.CoverageGap} -
 *       Represents a type change without corresponding fix</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Analyze migration path
 * MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(1).to(5)
 *     .analyze();
 *
 * System.out.println("Steps: " + path.stepCount());
 * for (MigrationStep step : path.steps()) {
 *     System.out.println(step.fix().name() + " affects " + step.affectedTypes());
 * }
 *
 * // Check fix coverage
 * FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(1).to(5)
 *     .analyzeCoverage();
 *
 * if (!coverage.isFullyCovered()) {
 *     for (CoverageGap gap : coverage.gaps()) {
 *         System.err.println("Missing fix for: " + gap.type());
 *     }
 * }
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.schematools.analysis.MigrationAnalyzer
 * @since 0.3.0
 */
package de.splatgames.aether.datafixers.schematools.analysis;
