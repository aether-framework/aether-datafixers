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
import de.splatgames.aether.datafixers.api.TypeReference;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Structural representation of a type for comparison and analysis.
 *
 * <p>This class provides a normalized view of a {@link de.splatgames.aether.datafixers.api.type.Type}
 * that can be used for structural comparison, diff generation, and analysis.
 * It extracts the essential structure including fields, children, and type kind.</p>
 *
 * <h2>Type Kinds</h2>
 * <p>Types are classified into categories via {@link TypeKind}:</p>
 * <ul>
 *   <li>{@link TypeKind#PRIMITIVE} - Basic types (int, string, bool, etc.)</li>
 *   <li>{@link TypeKind#LIST} - List/array types</li>
 *   <li>{@link TypeKind#OPTIONAL} - Optional/nullable types</li>
 *   <li>{@link TypeKind#PRODUCT} - Pair/tuple types</li>
 *   <li>{@link TypeKind#SUM} - Either/union types</li>
 *   <li>{@link TypeKind#FIELD} - Named field types</li>
 *   <li>{@link TypeKind#TAGGED_CHOICE} - Discriminated union types</li>
 *   <li>{@link TypeKind#NAMED} - Named/aliased types</li>
 *   <li>{@link TypeKind#PASSTHROUGH} - Passthrough types</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Type<?> playerType = schema.require(TypeReferences.PLAYER);
 * TypeStructure structure = TypeIntrospector.introspect(playerType);
 *
 * System.out.println("Kind: " + structure.kind());
 * System.out.println("Description: " + structure.description());
 *
 * for (FieldInfo field : structure.fields()) {
 *     System.out.println("Field: " + field.name());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see TypeIntrospector
 * @see FieldInfo
 * @since 0.3.0
 */
public final class TypeStructure {

    /**
     * Enumeration of type categories.
     */
    public enum TypeKind {
        /**
         * Primitive types (int, string, bool, etc.).
         */
        PRIMITIVE,

        /**
         * List/array types containing elements of another type.
         */
        LIST,

        /**
         * Optional types that may or may not contain a value.
         */
        OPTIONAL,

        /**
         * Product types (pairs/tuples) combining two types.
         */
        PRODUCT,

        /**
         * Sum types (either/union) representing one of two alternatives.
         */
        SUM,

        /**
         * Field types that extract named fields from structures.
         */
        FIELD,

        /**
         * Tagged choice types (discriminated unions).
         */
        TAGGED_CHOICE,

        /**
         * Named/aliased types.
         */
        NAMED,

        /**
         * Passthrough types that preserve data as-is.
         */
        PASSTHROUGH,

        /**
         * Unknown or unrecognized type kind.
         */
        UNKNOWN
    }

    /**
     * The type reference identifying this type within a schema.
     */
    private final TypeReference reference;

    /**
     * A human-readable description of this type's structure.
     */
    private final String description;

    /**
     * The category/kind of this type (primitive, list, field, etc.).
     */
    private final TypeKind kind;

    /**
     * The list of named fields if this is a structured type; empty for primitives.
     */
    private final List<FieldInfo> fields;

    /**
     * Child type structures representing component types (e.g., list element type).
     */
    private final List<TypeStructure> children;

    /**
     * Creates a new TypeStructure instance.
     *
     * @param reference   the type reference, must not be {@code null}
     * @param description the type description, must not be {@code null}
     * @param kind        the type kind, must not be {@code null}
     * @param fields      the list of fields (for structured types), must not be {@code null}
     * @param children    the list of child structures, must not be {@code null}
     */
    private TypeStructure(
            @NotNull final TypeReference reference,
            @NotNull final String description,
            @NotNull final TypeKind kind,
            @NotNull final List<FieldInfo> fields,
            @NotNull final List<TypeStructure> children
    ) {
        this.reference = Preconditions.checkNotNull(reference, "reference must not be null");
        this.description = Preconditions.checkNotNull(description, "description must not be null");
        this.kind = Preconditions.checkNotNull(kind, "kind must not be null");
        this.fields = List.copyOf(Preconditions.checkNotNull(fields, "fields must not be null"));
        this.children = List.copyOf(Preconditions.checkNotNull(children, "children must not be null"));
    }

    /**
     * Creates a builder for constructing TypeStructure instances.
     *
     * @param reference the type reference, must not be {@code null}
     * @return a new builder instance, never {@code null}
     */
    @NotNull
    public static Builder builder(@NotNull final TypeReference reference) {
        return new Builder(reference);
    }

    /**
     * Returns the type reference identifying this type.
     *
     * @return the type reference, never {@code null}
     */
    @NotNull
    public TypeReference reference() {
        return this.reference;
    }

    /**
     * Returns a human-readable description of this type.
     *
     * @return the type description, never {@code null}
     */
    @NotNull
    public String description() {
        return this.description;
    }

    /**
     * Returns the kind/category of this type.
     *
     * @return the type kind, never {@code null}
     */
    @NotNull
    public TypeKind kind() {
        return this.kind;
    }

    /**
     * Returns the list of fields for structured types.
     *
     * <p>For types that don't have named fields (primitives, lists, etc.),
     * this returns an empty list.</p>
     *
     * @return an unmodifiable list of fields, never {@code null}
     */
    @NotNull
    public List<FieldInfo> fields() {
        return this.fields;
    }

    /**
     * Returns the child type structures.
     *
     * <p>Child structures represent component types:</p>
     * <ul>
     *   <li>List: element type</li>
     *   <li>Optional: element type</li>
     *   <li>Product: first and second types</li>
     *   <li>Sum: left and right types</li>
     *   <li>Field: the field's value type</li>
     * </ul>
     *
     * @return an unmodifiable list of child structures, never {@code null}
     */
    @NotNull
    public List<TypeStructure> children() {
        return this.children;
    }

    /**
     * Checks if this structure is structurally equal to another.
     *
     * <p>Structural equality compares:</p>
     * <ul>
     *   <li>Type kind</li>
     *   <li>Type reference</li>
     *   <li>Fields (by name, optionality, and type reference)</li>
     *   <li>Children (recursively)</li>
     * </ul>
     *
     * @param other the other structure to compare, may be {@code null}
     * @return {@code true} if structurally equal, {@code false} otherwise
     */
    public boolean structurallyEquals(final TypeStructure other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (this.kind != other.kind) {
            return false;
        }
        if (!this.reference.equals(other.reference)) {
            return false;
        }
        if (this.fields.size() != other.fields.size()) {
            return false;
        }
        for (int i = 0; i < this.fields.size(); i++) {
            if (!this.fields.get(i).equals(other.fields.get(i))) {
                return false;
            }
        }
        if (this.children.size() != other.children.size()) {
            return false;
        }
        for (int i = 0; i < this.children.size(); i++) {
            if (!this.children.get(i).structurallyEquals(other.children.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares this type structure to another object for equality.
     *
     * <p>Two {@code TypeStructure} instances are considered equal if all of their
     * properties are equal: kind, reference, description, fields, and children.
     * For structural comparison (ignoring descriptions), use
     * {@link #structurallyEquals(TypeStructure)} instead.</p>
     *
     * @param obj the object to compare with, may be {@code null}
     * @return {@code true} if the objects are equal, {@code false} otherwise
     * @see #structurallyEquals(TypeStructure)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TypeStructure other)) {
            return false;
        }
        return this.kind == other.kind
                && this.reference.equals(other.reference)
                && this.description.equals(other.description)
                && this.fields.equals(other.fields)
                && this.children.equals(other.children);
    }

    /**
     * Returns a hash code value for this type structure.
     *
     * <p>The hash code is computed from all properties (reference, kind, description,
     * fields, children) to ensure consistency with {@link #equals(Object)}.</p>
     *
     * @return the hash code value for this type structure
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.reference, this.kind, this.description, this.fields, this.children);
    }

    /**
     * Returns a string representation of this type structure.
     *
     * <p>The format includes the type reference, kind, description, and counts
     * of fields and children. This is intended for debugging and logging purposes.</p>
     *
     * @return a human-readable string representation, never {@code null}
     */
    @Override
    public String toString() {
        return "TypeStructure{" +
                "reference=" + this.reference +
                ", kind=" + this.kind +
                ", description='" + this.description + '\'' +
                ", fields=" + this.fields.size() +
                ", children=" + this.children.size() +
                '}';
    }

    /**
     * Builder for constructing {@link TypeStructure} instances.
     *
     * <p>This builder provides a fluent API for creating type structure objects.
     * A type reference is required and must be provided via {@link TypeStructure#builder(TypeReference)}.
     * All other properties have sensible defaults:</p>
     * <ul>
     *   <li>{@code description}: empty string</li>
     *   <li>{@code kind}: {@link TypeKind#UNKNOWN}</li>
     *   <li>{@code fields}: empty list</li>
     *   <li>{@code children}: empty list</li>
     * </ul>
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * TypeStructure structure = TypeStructure.builder(TypeReference.of("player"))
     *     .description("Player entity data")
     *     .kind(TypeKind.FIELD)
     *     .fields(List.of(nameField, healthField))
     *     .build();
     * }</pre>
     *
     * <p><b>Thread Safety:</b>
     * Builders are not thread-safe and should not be shared between threads.</p>
     *
     * @author Erik Pförtner
     * @since 0.3.0
     */
    public static final class Builder {

        /**
         * The required type reference for the structure being built.
         */
        private final TypeReference reference;

        /**
         * The optional description text; defaults to empty string.
         */
        private String description = "";

        /**
         * The type kind/category; defaults to UNKNOWN.
         */
        private TypeKind kind = TypeKind.UNKNOWN;

        /**
         * The list of fields; defaults to empty list.
         */
        private List<FieldInfo> fields = List.of();

        /**
         * The list of child structures; defaults to empty list.
         */
        private List<TypeStructure> children = List.of();

        /**
         * Creates a new builder with the specified type reference.
         *
         * @param reference the type reference, must not be {@code null}
         * @throws NullPointerException if {@code reference} is {@code null}
         */
        private Builder(@NotNull final TypeReference reference) {
            this.reference = Preconditions.checkNotNull(reference, "reference must not be null");
        }

        /**
         * Sets the human-readable description for the type structure.
         *
         * <p>The description typically contains information about the type's
         * structure and purpose, such as the output of the type's {@code describe()} method.</p>
         *
         * @param description the description text, must not be {@code null}
         * @return this builder for method chaining, never {@code null}
         * @throws NullPointerException if {@code description} is {@code null}
         */
        @NotNull
        public Builder description(@NotNull final String description) {
            this.description = Preconditions.checkNotNull(description, "description must not be null");
            return this;
        }

        /**
         * Sets the type kind/category for the type structure.
         *
         * <p>The kind determines how the type is classified and affects how
         * comparison and analysis operations interpret the structure.</p>
         *
         * @param kind the type kind, must not be {@code null}
         * @return this builder for method chaining, never {@code null}
         * @throws NullPointerException if {@code kind} is {@code null}
         * @see TypeKind
         */
        @NotNull
        public Builder kind(@NotNull final TypeKind kind) {
            this.kind = Preconditions.checkNotNull(kind, "kind must not be null");
            return this;
        }

        /**
         * Sets the list of fields for the type structure.
         *
         * <p>Fields represent named values within structured types. For primitive
         * or collection types, this should typically be an empty list. The provided
         * list is copied to ensure immutability.</p>
         *
         * @param fields the list of fields, must not be {@code null}
         * @return this builder for method chaining, never {@code null}
         * @throws NullPointerException if {@code fields} is {@code null}
         * @see FieldInfo
         */
        @NotNull
        public Builder fields(@NotNull final List<FieldInfo> fields) {
            this.fields = Preconditions.checkNotNull(fields, "fields must not be null");
            return this;
        }

        /**
         * Sets the list of child type structures.
         *
         * <p>Children represent component types within compound types:</p>
         * <ul>
         *   <li>List types: the element type</li>
         *   <li>Optional types: the wrapped type</li>
         *   <li>Product types: the first and second types</li>
         *   <li>Sum types: the left and right types</li>
         * </ul>
         * <p>The provided list is copied to ensure immutability.</p>
         *
         * @param children the list of child structures, must not be {@code null}
         * @return this builder for method chaining, never {@code null}
         * @throws NullPointerException if {@code children} is {@code null}
         */
        @NotNull
        public Builder children(@NotNull final List<TypeStructure> children) {
            this.children = Preconditions.checkNotNull(children, "children must not be null");
            return this;
        }

        /**
         * Builds and returns a new immutable {@link TypeStructure} instance.
         *
         * <p>The builder can be reused after calling this method to create
         * additional instances with modified properties.</p>
         *
         * @return a new TypeStructure instance, never {@code null}
         */
        @NotNull
        public TypeStructure build() {
            return new TypeStructure(this.reference, this.description, this.kind, this.fields, this.children);
        }
    }
}
