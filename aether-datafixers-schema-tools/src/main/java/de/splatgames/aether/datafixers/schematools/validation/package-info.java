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
 * Schema structure and convention validation.
 *
 * <p>This package provides comprehensive validation for schemas, including
 * structural integrity checks, naming convention enforcement, and fix coverage
 * analysis.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.validation.SchemaValidator} -
 *       Fluent entry point for schema validation</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.validation.ValidationResult} -
 *       Collection of validation issues</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.validation.ValidationIssue} -
 *       Single validation issue with context</li>
 *   <li>{@link de.splatgames.aether.datafixers.schematools.validation.ConventionRules} -
 *       Configurable naming convention rules</li>
 * </ul>
 *
 * <h2>Validation Categories</h2>
 * <ul>
 *   <li><b>Structure Validation</b> - Parent chain integrity, version ordering,
 *       type consistency across versions</li>
 *   <li><b>Convention Validation</b> - TypeReference naming, fix naming,
 *       version numbering conventions</li>
 *   <li><b>Coverage Validation</b> - Ensures all type changes have corresponding
 *       DataFixes</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
 *     .validateStructure()
 *     .validateConventions()
 *     .validateFixCoverage()
 *     .withConventions(ConventionRules.STRICT)
 *     .validate();
 *
 * if (!result.isValid()) {
 *     for (ValidationIssue error : result.errors()) {
 *         System.err.println("[" + error.code() + "] " + error.message());
 *     }
 * }
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.schematools.validation.SchemaValidator
 * @since 0.3.0
 */
package de.splatgames.aether.datafixers.schematools.validation;
