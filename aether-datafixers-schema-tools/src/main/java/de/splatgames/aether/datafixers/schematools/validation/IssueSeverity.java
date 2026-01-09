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

package de.splatgames.aether.datafixers.schematools.validation;

/**
 * Severity levels for validation issues.
 *
 * <p>Each severity level indicates how critical a validation issue is:</p>
 * <ul>
 *   <li>{@link #ERROR} - Must be fixed, indicates a broken or invalid schema</li>
 *   <li>{@link #WARNING} - Should be reviewed, may indicate problems</li>
 *   <li>{@link #INFO} - Informational, suggestions or notes</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see ValidationIssue
 * @since 0.3.0
 */
public enum IssueSeverity {

    /**
     * Error severity - must be fixed.
     *
     * <p>Errors indicate that the schema is invalid or broken in a way that
     * will cause problems during data migration.</p>
     */
    ERROR,

    /**
     * Warning severity - should be reviewed.
     *
     * <p>Warnings indicate potential problems that may or may not cause issues.
     * They should be reviewed but don't necessarily need to be fixed.</p>
     */
    WARNING,

    /**
     * Info severity - informational only.
     *
     * <p>Info messages provide suggestions, notes, or other information that
     * doesn't indicate a problem.</p>
     */
    INFO
}
