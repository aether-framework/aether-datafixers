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
import de.splatgames.aether.datafixers.schematools.introspection.TypeStructure.TypeKind;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class for extracting structural information from types.
 *
 * <p>This class provides methods to introspect {@link Type} instances and
 * extract their structure, including fields, children, and type kind.
 * The extracted information can be used for schema comparison, validation,
 * and analysis.</p>
 *
 * <h2>Supported Type Kinds</h2>
 * <ul>
 *   <li><b>Primitives:</b> int, string, bool, long, float, double, byte, short</li>
 *   <li><b>Collections:</b> List, Optional</li>
 *   <li><b>Algebraic:</b> Product (pairs), Sum (either)</li>
 *   <li><b>Structured:</b> FieldType, TaggedChoiceType</li>
 *   <li><b>Special:</b> Named, Passthrough</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Introspect a type
 * Type<?> playerType = schema.require(TypeReferences.PLAYER);
 * TypeStructure structure = TypeIntrospector.introspect(playerType);
 *
 * // Check the type kind
 * if (structure.kind() == TypeKind.FIELD) {
 *     System.out.println("This is a field type");
 * }
 *
 * // Extract fields
 * List<FieldInfo> fields = TypeIntrospector.extractFields(playerType);
 * for (FieldInfo field : fields) {
 *     System.out.println(field.name() + ": " + field.fieldType().describe());
 * }
 *
 * // Check for a specific field
 * if (TypeIntrospector.hasField(playerType, "name")) {
 *     System.out.println("Type has a 'name' field");
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is stateless and thread-safe.</p>
 *
 * @author Erik Pfoertner
 * @see TypeStructure
 * @see FieldInfo
 * @since 0.3.0
 */
public final class TypeIntrospector {

    /**
     * Set of type reference IDs that are recognized as primitive types.
     *
     * <p>These IDs correspond to the built-in primitive types in the type system:
     * boolean, numeric types (int, long, float, double, byte, short), and string.</p>
     */
    private static final Set<String> PRIMITIVE_TYPE_IDS = Set.of(
            "bool", "int", "long", "float", "double", "byte", "short", "string"
    );

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException if called via reflection
     */
    private TypeIntrospector() {
        // Utility class - prevent instantiation
    }

    /**
     * Introspects a type and returns its structural representation.
     *
     * <p>This method analyzes the type to determine its kind, extract fields
     * (if applicable), and recursively introspect child types.</p>
     *
     * @param type the type to introspect, must not be {@code null}
     * @return the structural representation of the type, never {@code null}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    @NotNull
    public static TypeStructure introspect(@NotNull final Type<?> type) {
        Preconditions.checkNotNull(type, "type must not be null");
        return introspectInternal(type, "");
    }

    /**
     * Extracts all fields from a type.
     *
     * <p>This method recursively searches the type structure for {@link Type.FieldType}
     * instances and extracts their field information. For non-field types, the
     * result depends on the type's structure:</p>
     * <ul>
     *   <li>FieldType: Returns a single-element list with the field</li>
     *   <li>Product/Sum: Recursively extracts fields from children</li>
     *   <li>Primitive: Returns an empty list</li>
     * </ul>
     *
     * @param type the type to extract fields from, must not be {@code null}
     * @return a list of fields found in the type, never {@code null}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    @NotNull
    public static List<FieldInfo> extractFields(@NotNull final Type<?> type) {
        Preconditions.checkNotNull(type, "type must not be null");
        return extractFieldsInternal(type, "");
    }

    /**
     * Checks if a type contains a field with the specified name.
     *
     * <p>This method searches recursively through the type structure to find
     * a field with the given name. It checks FieldType instances as well as
     * TaggedChoiceType variants.</p>
     *
     * @param type      the type to search, must not be {@code null}
     * @param fieldName the field name to look for, must not be {@code null}
     * @return {@code true} if the type contains a field with the given name,
     *         {@code false} otherwise
     * @throws NullPointerException if any argument is {@code null}
     */
    public static boolean hasField(@NotNull final Type<?> type, @NotNull final String fieldName) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(fieldName, "fieldName must not be null");

        return extractFields(type).stream()
                .anyMatch(field -> field.name().equals(fieldName));
    }

    /**
     * Determines the kind of a type.
     *
     * @param type the type to analyze, must not be {@code null}
     * @return the type kind, never {@code null}
     * @throws NullPointerException if {@code type} is {@code null}
     */
    @NotNull
    public static TypeKind determineKind(@NotNull final Type<?> type) {
        Preconditions.checkNotNull(type, "type must not be null");

        // Check for specific type classes
        if (type instanceof Type.FieldType<?>) {
            return TypeKind.FIELD;
        }
        if (type instanceof Type.TaggedChoiceType) {
            return TypeKind.TAGGED_CHOICE;
        }

        // Check reference ID for other types
        final String refId = type.reference().getId();

        // Primitive types
        if (PRIMITIVE_TYPE_IDS.contains(refId)) {
            return TypeKind.PRIMITIVE;
        }

        // Passthrough
        if ("passthrough".equals(refId) || "...".equals(type.describe())) {
            return TypeKind.PASSTHROUGH;
        }

        // List type (reference ID starts with "list[")
        if (refId.startsWith("list[")) {
            return TypeKind.LIST;
        }

        // Optional type (reference ID starts with "optional[")
        if (refId.startsWith("optional[")) {
            return TypeKind.OPTIONAL;
        }

        // Product type (reference ID contains " × ")
        if (refId.contains(" × ")) {
            return TypeKind.PRODUCT;
        }

        // Sum type (reference ID contains " + ")
        if (refId.contains(" + ")) {
            return TypeKind.SUM;
        }

        // Named type (has "=" in description)
        if (type.describe().contains("=")) {
            return TypeKind.NAMED;
        }

        // Optional field type (reference ID starts with "?")
        if (refId.startsWith("?")) {
            return TypeKind.FIELD;
        }

        // Field type (reference ID contains ":")
        if (refId.contains(":")) {
            return TypeKind.FIELD;
        }

        return TypeKind.UNKNOWN;
    }

    // ==================== Internal Methods ====================

    /**
     * Internal introspection method with hierarchical path tracking.
     *
     * <p>This method performs the actual type introspection, maintaining a path prefix
     * to track the location within nested type structures. The path is used for
     * generating accurate field paths in {@link FieldInfo} instances.</p>
     *
     * @param type       the type to introspect, must not be {@code null}
     * @param pathPrefix the current path prefix (e.g., "player.position"),
     *                   empty string for root level
     * @return the structural representation of the type, never {@code null}
     */
    @NotNull
    private static TypeStructure introspectInternal(
            @NotNull final Type<?> type,
            @NotNull final String pathPrefix
    ) {
        final TypeKind kind = determineKind(type);
        final List<FieldInfo> fields = extractFieldsInternal(type, pathPrefix);

        // Recursively introspect children
        final List<TypeStructure> children = new ArrayList<>();
        for (final Type<?> child : type.children()) {
            final String childPath = computeChildPath(type, child, pathPrefix);
            children.add(introspectInternal(child, childPath));
        }

        return TypeStructure.builder(type.reference())
                .description(type.describe())
                .kind(kind)
                .fields(fields)
                .children(children)
                .build();
    }

    /**
     * Internal field extraction with hierarchical path tracking.
     *
     * <p>This method recursively extracts field information from a type, handling:</p>
     * <ul>
     *   <li>{@link Type.FieldType}: Creates a {@link FieldInfo} and recursively
     *       extracts from the field's inner type</li>
     *   <li>{@link Type.TaggedChoiceType}: Extracts fields from all choice variants</li>
     *   <li>Other types: Recursively extracts from child types</li>
     * </ul>
     *
     * @param type       the type to extract fields from, must not be {@code null}
     * @param pathPrefix the current path prefix for generating field paths,
     *                   empty string for root level
     * @return a mutable list of extracted fields, never {@code null}
     */
    @NotNull
    private static List<FieldInfo> extractFieldsInternal(
            @NotNull final Type<?> type,
            @NotNull final String pathPrefix
    ) {
        final List<FieldInfo> result = new ArrayList<>();

        // Direct FieldType
        if (type instanceof Type.FieldType<?> fieldType) {
            result.add(FieldInfo.of(fieldType, pathPrefix));

            // Recursively extract from the field's inner type
            final String fieldPath = pathPrefix.isEmpty()
                    ? fieldType.name()
                    : pathPrefix + "." + fieldType.name();
            result.addAll(extractFieldsInternal(fieldType.fieldType(), fieldPath));
            return result;
        }

        // TaggedChoiceType - has a tag field
        if (type instanceof Type.TaggedChoiceType choiceType) {
            // The tag field itself isn't a FieldType, but we can represent it
            // Extract fields from each choice
            for (final Type<?> choiceVariant : choiceType.choices().values()) {
                result.addAll(extractFieldsInternal(choiceVariant, pathPrefix));
            }
            return result;
        }

        // Recursively extract from children
        for (final Type<?> child : type.children()) {
            result.addAll(extractFieldsInternal(child, pathPrefix));
        }

        return result;
    }

    /**
     * Computes the hierarchical path for a child type based on its parent.
     *
     * <p>If the parent is a {@link Type.FieldType}, the field name is appended
     * to the parent path. Otherwise, the parent path is returned unchanged.</p>
     *
     * @param parent     the parent type, must not be {@code null}
     * @param child      the child type (used for potential future enhancements),
     *                   must not be {@code null}
     * @param parentPath the current parent path, empty string for root level
     * @return the computed child path, never {@code null}
     */
    @NotNull
    private static String computeChildPath(
            @NotNull final Type<?> parent,
            @NotNull final Type<?> child,
            @NotNull final String parentPath
    ) {
        if (parent instanceof Type.FieldType<?> fieldType) {
            return parentPath.isEmpty()
                    ? fieldType.name()
                    : parentPath + "." + fieldType.name();
        }
        return parentPath;
    }
}
