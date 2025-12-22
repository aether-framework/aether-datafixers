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

package de.splatgames.aether.datafixers.api.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base exception class for all data fixer related errors.
 *
 * <p>{@code DataFixerException} serves as the root of the exception hierarchy
 * for the Aether Datafixers framework. All specific exceptions (encoding,
 * decoding, fixing, registry) extend this class, allowing callers to catch
 * all data fixer errors with a single handler if desired.</p>
 *
 * <h2>Exception Hierarchy</h2>
 * <pre>
 * DataFixerException (this class)
 * ├── EncodeException    - Serialization failures
 * ├── DecodeException    - Deserialization failures
 * ├── FixException       - Migration/fix failures
 * └── RegistryException  - Registry lookup failures
 * </pre>
 *
 * <h2>Context Information</h2>
 * <p>This exception optionally carries a context string that provides
 * additional information about where the error occurred (e.g., field path,
 * type name, version). Use {@link #getContext()} to retrieve it.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     fixer.update(data, fromVersion, toVersion);
 * } catch (EncodeException e) {
 *     // Handle serialization error
 *     log.error("Failed to encode: {}", e.getMessage());
 * } catch (DecodeException e) {
 *     // Handle deserialization error
 *     log.error("Failed to decode at {}: {}", e.getPath(), e.getMessage());
 * } catch (DataFixerException e) {
 *     // Catch-all for any data fixer error
 *     log.error("Data fixer error: {}", e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Exception instances are immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see EncodeException
 * @see DecodeException
 * @see FixException
 * @see RegistryException
 * @since 0.1.0
 */
public class DataFixerException extends RuntimeException {

    @Nullable
    private final String context;

    /**
     * Creates a new data fixer exception with the specified message.
     *
     * @param message the detail message describing the error
     */
    public DataFixerException(@NotNull final String message) {
        super(message);
        this.context = null;
    }

    /**
     * Creates a new data fixer exception with the specified message and cause.
     *
     * @param message the detail message describing the error
     * @param cause   the underlying cause of the exception
     */
    public DataFixerException(@NotNull final String message,
                              @Nullable final Throwable cause) {
        super(message, cause);
        this.context = null;
    }

    /**
     * Creates a new data fixer exception with the specified message and context.
     *
     * @param message the detail message describing the error
     * @param context additional context information (e.g., field path, type name)
     */
    public DataFixerException(@NotNull final String message,
                              @Nullable final String context) {
        super(message);
        this.context = context;
    }

    /**
     * Creates a new data fixer exception with all parameters.
     *
     * @param message the detail message describing the error
     * @param context additional context information (e.g., field path, type name)
     * @param cause   the underlying cause of the exception
     */
    public DataFixerException(@NotNull final String message,
                              @Nullable final String context,
                              @Nullable final Throwable cause) {
        super(message, cause);
        this.context = context;
    }

    /**
     * Returns the context information associated with this exception.
     *
     * <p>The context provides additional details about where the error occurred,
     * such as the field path, type name, or version information.</p>
     *
     * @return the context string, or {@code null} if no context was provided
     */
    @Nullable
    public String getContext() {
        return this.context;
    }

    /**
     * Returns a detailed message including context if available.
     *
     * @return the message with context, or just the message if no context
     */
    @Override
    public String toString() {
        if (this.context != null) {
            return super.toString() + " [context: " + this.context + "]";
        }
        return super.toString();
    }
}
