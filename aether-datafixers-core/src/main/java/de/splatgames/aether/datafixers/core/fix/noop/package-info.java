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
 * No-op implementations for testing and silent operation.
 *
 * <p>This package provides no-operation implementations of fix-related
 * interfaces. These are useful for testing, benchmarking, or scenarios
 * where logging and diagnostics should be suppressed.</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.noop.NoOpDataFixerContext} -
 *       A context implementation that silently discards all logging and
 *       diagnostic output. Useful for production scenarios where fix
 *       execution should be completely silent.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create a silent data fixer
 * AetherDataFixer fixer = new DataFixerRuntimeFactory()
 *     .withContext(NoOpDataFixerContext.INSTANCE)
 *     .create(currentVersion, bootstrap);
 *
 * // Fix execution produces no output
 * fixer.update(data, fromVersion, toVersion);
 * }</pre>
 *
 * <h2>When to Use</h2>
 * <ul>
 *   <li>Unit tests that verify fix behavior without log noise</li>
 *   <li>Benchmarks measuring pure fix performance</li>
 *   <li>Production environments with external logging infrastructure</li>
 *   <li>Embedded scenarios where output must be suppressed</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.core.fix.noop.NoOpDataFixerContext
 * @see de.splatgames.aether.datafixers.api.fix.DataFixerContext
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.core.fix.noop;
