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
 * Exception hierarchy for the data fixing system.
 *
 * <p>This package defines the exception types used throughout the Aether Datafixers
 * framework. All exceptions extend from a common base class to enable unified
 * error handling while providing specific exception types for different failure
 * modes.</p>
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * RuntimeException
 *   └── {@link de.splatgames.aether.datafixers.api.exception.DataFixerException}
 *         ├── {@link de.splatgames.aether.datafixers.api.exception.DecodeException}
 *         ├── {@link de.splatgames.aether.datafixers.api.exception.EncodeException}
 *         ├── {@link de.splatgames.aether.datafixers.api.exception.FixException}
 *         └── {@link de.splatgames.aether.datafixers.api.exception.RegistryException}
 * </pre>
 *
 * <h2>Exception Types</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.exception.DataFixerException} -
 *       Base exception for all data fixer errors. Catch this to handle any
 *       framework-related exception.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.exception.DecodeException} -
 *       Thrown when decoding data from a Dynamic representation fails. This
 *       typically indicates malformed or incompatible data.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.exception.EncodeException} -
 *       Thrown when encoding a typed value to a Dynamic representation fails.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.exception.FixException} -
 *       Thrown when a data fix fails to apply. This may indicate a bug in the
 *       fix implementation or unexpected data structure.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.exception.RegistryException} -
 *       Thrown when registry operations fail, such as duplicate registrations
 *       or missing entries.</li>
 * </ul>
 *
 * <h2>Error Handling Strategies</h2>
 * <p>The framework uses two complementary error handling approaches:</p>
 * <ul>
 *   <li><strong>DataResult:</strong> For expected failures during codec operations,
 *       {@link de.splatgames.aether.datafixers.api.result.DataResult} provides
 *       a functional approach without exceptions.</li>
 *   <li><strong>Exceptions:</strong> For programming errors and unrecoverable
 *       failures, exceptions from this package are thrown.</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.api.exception.DataFixerException
 * @see de.splatgames.aether.datafixers.api.result.DataResult
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.exception;
