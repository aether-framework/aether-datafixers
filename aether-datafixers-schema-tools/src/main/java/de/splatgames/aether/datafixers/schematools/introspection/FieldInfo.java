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

package de.splatgames.aether.datafixers.schematools.introspection;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.type.Type;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Metadata about a single field in a type.
 *
 * <p>This class represents information about a named field extracted from a
 * {@link Type.FieldType}, including the field name, whether it's optional,
 * and the underlying type of the field's value.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Extract field info from a FieldType
 * Type.FieldType<String> nameField = (Type.FieldType<String>) Type.field("name", Type.STRING);
 * FieldInfo info = FieldInfo.of(nameField);
 *
 * System.out.println(info.name());       // "name"
 * System.out.println(info.isOptional()); // false
 * System.out.println(info.fieldType().describe()); // "string"
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see TypeIntrospector
 * @see TypeStructure
 * @since 0.3.0
 */
public final class FieldInfo {

    /**
     * The name of this field as defined in the type schema.
     */
    private final String name;

    /**
     * Flag indicating whether this field is optional (may be absent in serialized data).
     */
    private final boolean optional;

    /**
     * The type that describes the value this field holds.
     */
    private final Type<?> fieldType;

    /**
     * The full dot-separated path to this field within the type hierarchy.
     * For nested fields, includes parent paths (e.g., "player.position.x").
     */
    private final String path;

    /**
     * Creates a new field info instance.
     *
     * @param name      the field name, must not be {@code null}
     * @param optional  whether the field is optional
     * @param fieldType the type of the field's value, must not be {@code null}
     * @param path      the full path to this field (e.g., "player.position.x"),
     *                  must not be {@code null}
     */
    private FieldInfo(
            @NotNull final String name,
            final boolean optional,
            @NotNull final Type<?> fieldType,
            @NotNull final String path
    ) {
        this.name = Preconditions.checkNotNull(name, "name must not be null");
        this.optional = optional;
        this.fieldType = Preconditions.checkNotNull(fieldType, "fieldType must not be null");
        this.path = Preconditions.checkNotNull(path, "path must not be null");
    }

    /**
     * Creates a FieldInfo from a {@link Type.FieldType}.
     *
     * @param fieldType the field type to extract info from, must not be {@code null}
     * @return a new FieldInfo instance, never {@code null}
     * @throws NullPointerException if {@code fieldType} is {@code null}
     */
    @NotNull
    public static FieldInfo of(@NotNull final Type.FieldType<?> fieldType) {
        Preconditions.checkNotNull(fieldType, "fieldType must not be null");
        return new FieldInfo(
                fieldType.name(),
                fieldType.isOptional(),
                fieldType.fieldType(),
                fieldType.name()
        );
    }

    /**
     * Creates a FieldInfo from a {@link Type.FieldType} with a custom path prefix.
     *
     * @param fieldType  the field type to extract info from, must not be {@code null}
     * @param pathPrefix the path prefix (e.g., "player.position"), must not be {@code null}
     * @return a new FieldInfo instance, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public static FieldInfo of(
            @NotNull final Type.FieldType<?> fieldType,
            @NotNull final String pathPrefix
    ) {
        Preconditions.checkNotNull(fieldType, "fieldType must not be null");
        Preconditions.checkNotNull(pathPrefix, "pathPrefix must not be null");

        final String fullPath = pathPrefix.isEmpty()
                ? fieldType.name()
                : pathPrefix + "." + fieldType.name();

        return new FieldInfo(
                fieldType.name(),
                fieldType.isOptional(),
                fieldType.fieldType(),
                fullPath
        );
    }

    /**
     * Creates a FieldInfo with explicit parameters.
     *
     * @param name      the field name, must not be {@code null}
     * @param optional  whether the field is optional
     * @param fieldType the type of the field's value, must not be {@code null}
     * @param path      the full path to this field, must not be {@code null}
     * @return a new FieldInfo instance, never {@code null}
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    @NotNull
    public static FieldInfo create(
            @NotNull final String name,
            final boolean optional,
            @NotNull final Type<?> fieldType,
            @NotNull final String path
    ) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(fieldType, "fieldType must not be null");
        Preconditions.checkNotNull(path, "path must not be null");
        return new FieldInfo(name, optional, fieldType, path);
    }

    /**
     * Returns the field name.
     *
     * @return the field name, never {@code null}
     */
    @NotNull
    public String name() {
        return this.name;
    }

    /**
     * Returns whether this field is optional.
     *
     * <p>Optional fields may be absent in the serialized data without causing
     * an error during parsing.</p>
     *
     * @return {@code true} if the field is optional, {@code false} if required
     */
    public boolean isOptional() {
        return this.optional;
    }

    /**
     * Returns the type of the field's value.
     *
     * @return the field type, never {@code null}
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Type<?> is immutable; returning it directly is safe."
    )
    @NotNull
    public Type<?> fieldType() {
        return this.fieldType;
    }

    /**
     * Returns the full path to this field.
     *
     * <p>For nested fields, this includes the parent path separated by dots.
     * For example, a field "x" inside "position" inside "player" would have
     * the path "player.position.x".</p>
     *
     * @return the full path to this field, never {@code null}
     */
    @NotNull
    public String path() {
        return this.path;
    }

    /**
     * Compares this field info to another object for equality.
     *
     * <p>Two {@code FieldInfo} instances are considered equal if they have the same
     * name, optionality, path, and their field types have the same type reference.
     * Note that the field type comparison is based on type reference equality,
     * not full structural equality.</p>
     *
     * @param obj the object to compare with, may be {@code null}
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FieldInfo other)) {
            return false;
        }
        return this.optional == other.optional
                && this.name.equals(other.name)
                && this.path.equals(other.path)
                && this.fieldType.reference().equals(other.fieldType.reference());
    }

    /**
     * Returns a hash code value for this field info.
     *
     * <p>The hash code is computed from the name, optionality, path, and field type
     * reference to ensure consistency with {@link #equals(Object)}.</p>
     *
     * @return the hash code value for this field info
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.optional, this.path, this.fieldType.reference());
    }

    /**
     * Returns a string representation of this field info.
     *
     * <p>The format includes the field name (prefixed with "?" if optional),
     * the field type description, and the full path. For example:</p>
     * <pre>{@code
     * "name: string (path: player.name)"
     * "?health: int (path: player.health)"
     * }</pre>
     *
     * @return a human-readable string representation, never {@code null}
     */
    @Override
    public String toString() {
        return (this.optional ? "?" : "") + this.name + ": " + this.fieldType.describe()
                + " (path: " + this.path + ")";
    }
}
