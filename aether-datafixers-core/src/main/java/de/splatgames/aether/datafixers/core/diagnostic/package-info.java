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
 * Core implementations for migration diagnostics.
 *
 * <p>This package contains the default implementations of the diagnostic
 * interfaces defined in {@link de.splatgames.aether.datafixers.api.diagnostic}.</p>
 *
 * <h2>Key Components</h2>
 *
 * <dl>
 *   <dt>{@link de.splatgames.aether.datafixers.core.diagnostic.DiagnosticContextImpl}</dt>
 *   <dd>Default implementation of
 *   {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.core.diagnostic.MigrationReportImpl}</dt>
 *   <dd>Default implementation of
 *   {@link de.splatgames.aether.datafixers.api.diagnostic.MigrationReport}</dd>
 *
 *   <dt>{@link de.splatgames.aether.datafixers.core.diagnostic.DiagnosticRuleWrapper}</dt>
 *   <dd>A rule wrapper that captures diagnostic events during rule application</dd>
 * </dl>
 *
 * @author Erik Pförtner
 * @since 0.2.0
 */
package de.splatgames.aether.datafixers.core.diagnostic;
