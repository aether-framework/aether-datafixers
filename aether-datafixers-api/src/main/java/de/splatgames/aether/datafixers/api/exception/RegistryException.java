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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when a registry lookup fails.
 *
 * <p>{@code RegistryException} is thrown when a required type, schema, or codec
 * cannot be found in its respective registry. This can happen due to:</p>
 * <ul>
 *   <li>Missing type registration</li>
 *   <li>Missing schema for a specific version</li>
 *   <li>Missing codec registration</li>
 *   <li>Attempting to use an unregistered type reference</li>
 * </ul>
 *
 * <h2>Registry Types</h2>
 * <p>This exception can be thrown by:</p>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.TypeRegistry}</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.schema.SchemaRegistry}</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.codec.CodecRegistry}</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     Type<?> type = registry.require(typeRef);
 * } catch (RegistryException e) {
 *     log.error("Type '{}' not found in registry: {}",
 *         e.getMissingType(), e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Exception instances are immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataFixerException
 * @since 0.1.0
 */
public class RegistryException extends DataFixerException {

    @Nullable
    private final TypeReference missingType;

    @Nullable
    private final DataVersion missingVersion;

    /**
     * Creates a new registry exception with the specified message.
     *
     * @param message the detail message describing the registry failure
     */
    public RegistryException(@NotNull final String message) {
        super(message);
        this.missingType = null;
        this.missingVersion = null;
    }

    /**
     * Creates a new registry exception with message and cause.
     *
     * @param message the detail message describing the registry failure
     * @param cause   the underlying cause of the registry failure
     */
    public RegistryException(@NotNull final String message,
                             @Nullable final Throwable cause) {
        super(message, cause);
        this.missingType = null;
        this.missingVersion = null;
    }

    /**
     * Creates a new registry exception for a missing type.
     *
     * @param message     the detail message describing the registry failure
     * @param missingType the type reference that was not found
     */
    public RegistryException(@NotNull final String message,
                             @Nullable final TypeReference missingType) {
        super(message, missingType != null ? "type=" + missingType.getId() : null);
        this.missingType = missingType;
        this.missingVersion = null;
    }

    /**
     * Creates a new registry exception for a missing schema version.
     *
     * @param message        the detail message describing the registry failure
     * @param missingVersion the version that was not found
     */
    public RegistryException(@NotNull final String message,
                             @Nullable final DataVersion missingVersion) {
        super(message, missingVersion != null ? "version=" + missingVersion.getVersion() : null, null);
        this.missingType = null;
        this.missingVersion = missingVersion;
    }

    /**
     * Creates a new registry exception for a missing type at a specific version.
     *
     * @param message        the detail message describing the registry failure
     * @param missingType    the type reference that was not found
     * @param missingVersion the version at which the type was not found
     */
    public RegistryException(@NotNull final String message,
                             @Nullable final TypeReference missingType,
                             @Nullable final DataVersion missingVersion) {
        super(message, buildContext(missingType, missingVersion));
        this.missingType = missingType;
        this.missingVersion = missingVersion;
    }

    /**
     * Creates a new registry exception with full context and cause.
     *
     * @param message        the detail message describing the registry failure
     * @param missingType    the type reference that was not found
     * @param missingVersion the version at which the type was not found
     * @param cause          the underlying cause of the registry failure
     */
    public RegistryException(@NotNull final String message,
                             @Nullable final TypeReference missingType,
                             @Nullable final DataVersion missingVersion,
                             @Nullable final Throwable cause) {
        super(message, buildContext(missingType, missingVersion), cause);
        this.missingType = missingType;
        this.missingVersion = missingVersion;
    }

    /**
     * Returns the type reference that was not found in the registry.
     *
     * @return the missing type reference, or {@code null} if not a type lookup failure
     */
    @Nullable
    public TypeReference getMissingType() {
        return this.missingType;
    }

    /**
     * Returns the version that was not found in the registry.
     *
     * @return the missing version, or {@code null} if not a version lookup failure
     */
    @Nullable
    public DataVersion getMissingVersion() {
        return this.missingVersion;
    }

    @Nullable
    private static String buildContext(@Nullable final TypeReference missingType,
                                        @Nullable final DataVersion missingVersion) {
        if (missingType == null && missingVersion == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        if (missingType != null) {
            sb.append("type=").append(missingType.getId());
        }
        if (missingVersion != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("version=").append(missingVersion.getVersion());
        }
        return sb.toString();
    }
}
