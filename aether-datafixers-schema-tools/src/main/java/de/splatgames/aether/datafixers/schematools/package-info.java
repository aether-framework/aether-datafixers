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
 * Schema analysis, validation, and migration path analysis tools for Aether Datafixers.
 *
 * <p>This module provides utilities for analyzing schemas, comparing schema versions,
 * validating schema structures, and analyzing migration paths.</p>
 *
 * <h2>Key Packages</h2>
 * <ul>
 *   <li>{@code diff} - Schema comparison and diff utilities</li>
 *   <li>{@code analysis} - Migration path and fix coverage analysis</li>
 *   <li>{@code validation} - Schema structure and convention validation</li>
 *   <li>{@code introspection} - Type structure introspection</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Compare two schemas
 * SchemaDiff diff = SchemaDiffer.compare(schemaV1, schemaV2)
 *     .includeFieldLevel(true)
 *     .diff();
 *
 * // Validate schema structure
 * ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
 *     .validateStructure()
 *     .validateConventions()
 *     .validate();
 *
 * // Analyze migration path
 * MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
 *     .from(1).to(5)
 *     .analyze();
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.schematools.diff.SchemaDiffer
 * @see de.splatgames.aether.datafixers.schematools.validation.SchemaValidator
 * @see de.splatgames.aether.datafixers.schematools.analysis.MigrationAnalyzer
 * @since 0.3.0
 */
package de.splatgames.aether.datafixers.schematools;
