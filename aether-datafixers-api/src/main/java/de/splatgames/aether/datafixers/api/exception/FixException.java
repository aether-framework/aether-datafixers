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
 * Exception thrown when a data fix (migration) fails.
 *
 * <p>{@code FixException} is thrown when a {@link de.splatgames.aether.datafixers.api.fix.DataFix}
 * fails to transform data from one version to another. This can happen due to:</p>
 * <ul>
 *   <li>Invalid input data that doesn't match expected schema</li>
 *   <li>Rule application failures</li>
 *   <li>Type rewrite failures</li>
 *   <li>Missing required fields during transformation</li>
 * </ul>
 *
 * <h2>Version Information</h2>
 * <p>The exception carries the fix name and version range to help identify
 * exactly which fix failed and what migration was being attempted.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try {
 *     TaggedDynamic<?> updated = fixer.update(data, fromVersion, toVersion);
 * } catch (FixException e) {
 *     log.error("Fix '{}' failed migrating from {} to {}: {}",
 *         e.getFixName(), e.getFromVersion(), e.getToVersion(), e.getMessage());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Exception instances are immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see DataFixerException
 * @see de.splatgames.aether.datafixers.api.fix.DataFix
 * @since 0.1.0
 */
public class FixException extends DataFixerException {

    @Nullable
    private final String fixName;

    @Nullable
    private final DataVersion fromVersion;

    @Nullable
    private final DataVersion toVersion;

    @Nullable
    private final TypeReference typeReference;

    /**
     * Creates a new fix exception with the specified message.
     *
     * @param message the detail message describing the fix failure
     */
    public FixException(@NotNull final String message) {
        super(message);
        this.fixName = null;
        this.fromVersion = null;
        this.toVersion = null;
        this.typeReference = null;
    }

    /**
     * Creates a new fix exception with message and cause.
     *
     * @param message the detail message describing the fix failure
     * @param cause   the underlying cause of the fix failure
     */
    public FixException(@NotNull final String message,
                        @Nullable final Throwable cause) {
        super(message, cause);
        this.fixName = null;
        this.fromVersion = null;
        this.toVersion = null;
        this.typeReference = null;
    }

    /**
     * Creates a new fix exception with fix information.
     *
     * @param message     the detail message describing the fix failure
     * @param fixName     the name of the fix that failed
     * @param fromVersion the source version of the migration
     * @param toVersion   the target version of the migration
     */
    public FixException(@NotNull final String message,
                        @Nullable final String fixName,
                        @Nullable final DataVersion fromVersion,
                        @Nullable final DataVersion toVersion) {
        super(message, buildContext(fixName, fromVersion, toVersion, null));
        this.fixName = fixName;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.typeReference = null;
    }

    /**
     * Creates a new fix exception with full context.
     *
     * @param message       the detail message describing the fix failure
     * @param fixName       the name of the fix that failed
     * @param fromVersion   the source version of the migration
     * @param toVersion     the target version of the migration
     * @param typeReference the type being fixed
     */
    public FixException(@NotNull final String message,
                        @Nullable final String fixName,
                        @Nullable final DataVersion fromVersion,
                        @Nullable final DataVersion toVersion,
                        @Nullable final TypeReference typeReference) {
        super(message, buildContext(fixName, fromVersion, toVersion, typeReference));
        this.fixName = fixName;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.typeReference = typeReference;
    }

    /**
     * Creates a new fix exception with full context and cause.
     *
     * @param message       the detail message describing the fix failure
     * @param fixName       the name of the fix that failed
     * @param fromVersion   the source version of the migration
     * @param toVersion     the target version of the migration
     * @param typeReference the type being fixed
     * @param cause         the underlying cause of the fix failure
     */
    public FixException(@NotNull final String message,
                        @Nullable final String fixName,
                        @Nullable final DataVersion fromVersion,
                        @Nullable final DataVersion toVersion,
                        @Nullable final TypeReference typeReference,
                        @Nullable final Throwable cause) {
        super(message, buildContext(fixName, fromVersion, toVersion, typeReference), cause);
        this.fixName = fixName;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.typeReference = typeReference;
    }

    /**
     * Returns the name of the fix that failed.
     *
     * @return the fix name, or {@code null} if not available
     */
    @Nullable
    public String getFixName() {
        return this.fixName;
    }

    /**
     * Returns the source version of the migration that failed.
     *
     * @return the source version, or {@code null} if not available
     */
    @Nullable
    public DataVersion getFromVersion() {
        return this.fromVersion;
    }

    /**
     * Returns the target version of the migration that failed.
     *
     * @return the target version, or {@code null} if not available
     */
    @Nullable
    public DataVersion getToVersion() {
        return this.toVersion;
    }

    /**
     * Returns the type reference being fixed when the error occurred.
     *
     * @return the type reference, or {@code null} if not available
     */
    @Nullable
    public TypeReference getTypeReference() {
        return this.typeReference;
    }

    @Nullable
    private static String buildContext(@Nullable final String fixName,
                                        @Nullable final DataVersion fromVersion,
                                        @Nullable final DataVersion toVersion,
                                        @Nullable final TypeReference typeReference) {
        final StringBuilder sb = new StringBuilder();
        if (fixName != null) {
            sb.append("fix=").append(fixName);
        }
        if (fromVersion != null && toVersion != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("version=").append(fromVersion.getVersion())
              .append("->").append(toVersion.getVersion());
        }
        if (typeReference != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("type=").append(typeReference.getId());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
