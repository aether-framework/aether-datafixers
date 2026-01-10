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

package de.splatgames.aether.datafixers.api.fix;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A context for logging and diagnostics during data fix operations.
 *
 * <p>{@code DataFixerContext} provides a simple logging abstraction that allows
 * {@link DataFix} implementations to report progress, warnings, and diagnostic information during data migration. This
 * enables integrating data fixing with the host application's logging infrastructure.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In a DataFix implementation
 * public Dynamic<T> apply(TypeReference type, Dynamic<T> input, DataFixerContext context) {
 *     context.info("Migrating player data from v{} to v{}", fromVersion(), toVersion());
 *
 *     Dynamic<?> health = input.get("health");
 *     if (health == null) {
 *         context.warn("Missing 'health' field, using default value");
 *         return input.set("health", input.createInt(20));
 *     }
 *
 *     return input;
 * }
 * }</pre>
 *
 * <h2>Implementations</h2>
 * <p>Common implementations include:</p>
 * <ul>
 *   <li>Logging to SLF4J, Log4j, or java.util.logging</li>
 *   <li>Silent/no-op context for production use</li>
 *   <li>Capturing context for testing</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see DataFix
 * @see DataFixer
 * @since 0.1.0
 */
public interface DataFixerContext {

    /**
     * Logs an informational message.
     *
     * <p>Use this for progress updates, successful operations, and general
     * diagnostic information that may be useful during debugging.</p>
     *
     * @param message the message format string (may contain {} placeholders), must not be {@code null}
     * @param args    optional arguments to substitute into the message placeholders, may be {@code null} or empty
     */
    void info(@NotNull final String message,
              @Nullable final Object... args);

    /**
     * Logs a warning message.
     *
     * <p>Use this for non-fatal issues that indicate potential problems,
     * such as missing optional fields, deprecated data formats, or recoverable errors.</p>
     *
     * @param message the message format string (may contain {} placeholders), must not be {@code null}
     * @param args    optional arguments to substitute into the message placeholders, may be {@code null} or empty
     */
    void warn(@NotNull final String message,
              @Nullable final Object... args);
}
