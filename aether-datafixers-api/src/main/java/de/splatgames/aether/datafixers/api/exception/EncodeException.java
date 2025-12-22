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

import de.splatgames.aether.datafixers.api.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when encoding (serialization) of a value fails.
 *
 * <p>{@code EncodeException} is thrown when a value cannot be serialized
 * to its dynamic representation. This can happen due to:</p>
 * <ul>
 *   <li>Invalid or null values that cannot be encoded</li>
 *   <li>Codec configuration errors</li>
 *   <li>Type mismatches during encoding</li>
 *   <li>Unsupported value types</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     Dynamic<JsonElement> encoded = fixer.encode(version, typeRef, value, GsonOps.INSTANCE);
 * } catch (EncodeException e) {
 *     log.error("Failed to encode {} of type {}: {}",
 *         e.getFailedValue(), e.getTypeReference(), e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Exception instances are immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataFixerException
 * @see DecodeException
 * @since 0.1.0
 */
public class EncodeException extends DataFixerException {

    @Nullable
    private final TypeReference typeReference;

    @Nullable
    private final Object failedValue;

    /**
     * Creates a new encode exception with the specified message.
     *
     * @param message the detail message describing the encoding failure
     */
    public EncodeException(@NotNull final String message) {
        super(message);
        this.typeReference = null;
        this.failedValue = null;
    }

    /**
     * Creates a new encode exception with message and cause.
     *
     * @param message the detail message describing the encoding failure
     * @param cause   the underlying cause of the encoding failure
     */
    public EncodeException(@NotNull final String message,
                           @Nullable final Throwable cause) {
        super(message, cause);
        this.typeReference = null;
        this.failedValue = null;
    }

    /**
     * Creates a new encode exception with type information.
     *
     * @param message       the detail message describing the encoding failure
     * @param typeReference the type that was being encoded
     */
    public EncodeException(@NotNull final String message,
                           @Nullable final TypeReference typeReference) {
        super(message, typeReference != null ? typeReference.getId() : null);
        this.typeReference = typeReference;
        this.failedValue = null;
    }

    /**
     * Creates a new encode exception with full context.
     *
     * @param message       the detail message describing the encoding failure
     * @param typeReference the type that was being encoded
     * @param failedValue   the value that failed to encode
     */
    public EncodeException(@NotNull final String message,
                           @Nullable final TypeReference typeReference,
                           @Nullable final Object failedValue) {
        super(message, typeReference != null ? typeReference.getId() : null);
        this.typeReference = typeReference;
        this.failedValue = failedValue;
    }

    /**
     * Creates a new encode exception with full context and cause.
     *
     * @param message       the detail message describing the encoding failure
     * @param typeReference the type that was being encoded
     * @param failedValue   the value that failed to encode
     * @param cause         the underlying cause of the encoding failure
     */
    public EncodeException(@NotNull final String message,
                           @Nullable final TypeReference typeReference,
                           @Nullable final Object failedValue,
                           @Nullable final Throwable cause) {
        super(message, typeReference != null ? typeReference.getId() : null, cause);
        this.typeReference = typeReference;
        this.failedValue = failedValue;
    }

    /**
     * Returns the type reference that was being encoded when the error occurred.
     *
     * @return the type reference, or {@code null} if not available
     */
    @Nullable
    public TypeReference getTypeReference() {
        return this.typeReference;
    }

    /**
     * Returns the value that failed to encode.
     *
     * <p><strong>Note:</strong> Be cautious when logging this value as it may
     * contain sensitive information.</p>
     *
     * @return the failed value, or {@code null} if not available
     */
    @Nullable
    public Object getFailedValue() {
        return this.failedValue;
    }
}
