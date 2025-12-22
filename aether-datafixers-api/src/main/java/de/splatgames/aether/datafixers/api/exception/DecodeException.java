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
 * Exception thrown when decoding (deserialization) of a value fails.
 *
 * <p>{@code DecodeException} is thrown when a dynamic value cannot be parsed
 * into its typed representation. This can happen due to:</p>
 * <ul>
 *   <li>Missing required fields</li>
 *   <li>Invalid field values or types</li>
 *   <li>Malformed input data</li>
 *   <li>Schema mismatches</li>
 * </ul>
 *
 * <h2>Path Information</h2>
 * <p>The exception optionally carries a path string indicating where in the
 * data structure the error occurred (e.g., "player.inventory[0].item").</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     Player player = fixer.decode(version, taggedDynamic);
 * } catch (DecodeException e) {
 *     log.error("Failed to decode at path '{}': {}",
 *         e.getPath(), e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Exception instances are immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataFixerException
 * @see EncodeException
 * @since 0.1.0
 */
public class DecodeException extends DataFixerException {

    @Nullable
    private final TypeReference typeReference;

    @Nullable
    private final String path;

    /**
     * Creates a new decode exception with the specified message.
     *
     * @param message the detail message describing the decoding failure
     */
    public DecodeException(@NotNull final String message) {
        super(message);
        this.typeReference = null;
        this.path = null;
    }

    /**
     * Creates a new decode exception with message and cause.
     *
     * @param message the detail message describing the decoding failure
     * @param cause   the underlying cause of the decoding failure
     */
    public DecodeException(@NotNull final String message,
                           @Nullable final Throwable cause) {
        super(message, cause);
        this.typeReference = null;
        this.path = null;
    }

    /**
     * Creates a new decode exception with type information.
     *
     * @param message       the detail message describing the decoding failure
     * @param typeReference the type that was being decoded
     */
    public DecodeException(@NotNull final String message,
                           @Nullable final TypeReference typeReference) {
        super(message, typeReference != null ? typeReference.getId() : null);
        this.typeReference = typeReference;
        this.path = null;
    }

    /**
     * Creates a new decode exception with path information.
     *
     * @param message       the detail message describing the decoding failure
     * @param typeReference the type that was being decoded
     * @param path          the path in the data structure where the error occurred
     */
    public DecodeException(@NotNull final String message,
                           @Nullable final TypeReference typeReference,
                           @Nullable final String path) {
        super(message, buildContext(typeReference, path));
        this.typeReference = typeReference;
        this.path = path;
    }

    /**
     * Creates a new decode exception with full context and cause.
     *
     * @param message       the detail message describing the decoding failure
     * @param typeReference the type that was being decoded
     * @param path          the path in the data structure where the error occurred
     * @param cause         the underlying cause of the decoding failure
     */
    public DecodeException(@NotNull final String message,
                           @Nullable final TypeReference typeReference,
                           @Nullable final String path,
                           @Nullable final Throwable cause) {
        super(message, buildContext(typeReference, path), cause);
        this.typeReference = typeReference;
        this.path = path;
    }

    /**
     * Returns the type reference that was being decoded when the error occurred.
     *
     * @return the type reference, or {@code null} if not available
     */
    @Nullable
    public TypeReference getTypeReference() {
        return this.typeReference;
    }

    /**
     * Returns the path in the data structure where the error occurred.
     *
     * <p>The path format uses dot notation for field access and brackets for
     * array indices (e.g., "player.inventory[0].item.name").</p>
     *
     * @return the path string, or {@code null} if not available
     */
    @Nullable
    public String getPath() {
        return this.path;
    }

    @Nullable
    private static String buildContext(@Nullable final TypeReference typeReference,
                                        @Nullable final String path) {
        if (typeReference == null && path == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        if (typeReference != null) {
            sb.append("type=").append(typeReference.getId());
        }
        if (path != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("path=").append(path);
        }
        return sb.toString();
    }
}
