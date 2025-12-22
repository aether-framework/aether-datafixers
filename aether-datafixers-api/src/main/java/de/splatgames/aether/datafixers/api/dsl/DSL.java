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

package de.splatgames.aether.datafixers.api.dsl;

import de.splatgames.aether.datafixers.api.optic.Finder;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.template.TypeFamily;
import de.splatgames.aether.datafixers.api.type.template.TypeTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Domain-Specific Language (DSL) for defining type schemas.
 *
 * <p>This class provides a declarative, fluent API for constructing
 * {@link TypeTemplate}s that define the structure of versioned data. These templates are used by the data fixing system
 * to understand and migrate data between schema versions.</p>
 *
 * <h2>Primitive Types</h2>
 * <ul>
 *   <li>{@link #bool()}, {@link #intType()}, {@link #longType()}</li>
 *   <li>{@link #floatType()}, {@link #doubleType()}</li>
 *   <li>{@link #byteType()}, {@link #shortType()}, {@link #string()}</li>
 * </ul>
 *
 * <h2>Compound Types</h2>
 * <ul>
 *   <li>{@link #list(TypeTemplate)} - List of elements</li>
 *   <li>{@link #optional(TypeTemplate)} - Optional value</li>
 *   <li>{@link #and(TypeTemplate, TypeTemplate)} - Product type (tuple)</li>
 *   <li>{@link #or(TypeTemplate, TypeTemplate)} - Sum type (union)</li>
 * </ul>
 *
 * <h2>Field Types</h2>
 * <ul>
 *   <li>{@link #field(String, TypeTemplate)} - Required field</li>
 *   <li>{@link #optionalField(String, TypeTemplate)} - Optional field</li>
 *   <li>{@link #remainder()} - Captures unmatched fields</li>
 * </ul>
 *
 * <h2>Advanced Types</h2>
 * <ul>
 *   <li>{@link #taggedChoice(String, Map)} - Discriminated union</li>
 *   <li>{@link #named(String, TypeTemplate)} - Named type for recursion</li>
 *   <li>{@link #recursive(String, java.util.function.Function)} - Recursive types</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define a schema for a Player entity
 * TypeTemplate playerSchema = DSL.and(
 *     DSL.field("name", DSL.string()),
 *     DSL.field("health", DSL.intType()),
 *     DSL.field("position", DSL.and(
 *         DSL.field("x", DSL.doubleType()),
 *         DSL.field("y", DSL.doubleType()),
 *         DSL.field("z", DSL.doubleType())
 *     )),
 *     DSL.optionalField("inventory", DSL.list(DSL.string())),
 *     DSL.remainder()  // Capture any other fields
 * );
 *
 * // Instantiate the template
 * Type<?> playerType = playerSchema.apply(TypeFamily.empty());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this class return immutable, thread-safe objects.</p>
 *
 * @author Erik Pförtner
 * @see TypeTemplate
 * @see TypeFamily
 * @see Type
 * @since 0.1.0
 */
public final class DSL {

    /**
     * Private constructor to prevent instantiation.
     */
    private DSL() {
        // private constructor to prevent instantiation
    }

    // ==================== Primitive Types ====================

    /**
     * Creates a boolean type template for true/false values.
     *
     * <p>Boolean templates are used for fields that represent binary state,
     * such as flags, toggles, or enabled/disabled settings.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a settings schema with boolean flags
     * TypeTemplate settingsSchema = DSL.and(
     *     DSL.field("soundEnabled", DSL.bool()),
     *     DSL.field("musicEnabled", DSL.bool()),
     *     DSL.field("fullscreen", DSL.bool())
     * );
     *
     * // JSON: {"soundEnabled": true, "musicEnabled": false, "fullscreen": true}
     * }</pre>
     *
     * @return a boolean type template, never {@code null}
     * @see Type#BOOL
     */
    @NotNull
    public static TypeTemplate bool() {
        return new ConstTemplate("bool", Type.BOOL);
    }

    /**
     * Creates a 32-bit integer type template.
     *
     * <p>Integer templates are the most common numeric type, suitable for
     * counts, IDs, levels, and other whole numbers within the range {@code -2,147,483,648} to
     * {@code 2,147,483,647}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a player schema with integer fields
     * TypeTemplate playerSchema = DSL.and(
     *     DSL.field("id", DSL.intType()),
     *     DSL.field("level", DSL.intType()),
     *     DSL.field("experience", DSL.intType()),
     *     DSL.field("health", DSL.intType())
     * );
     *
     * // JSON: {"id": 42, "level": 15, "experience": 12500, "health": 100}
     * }</pre>
     *
     * @return an integer type template, never {@code null}
     * @see Type#INT
     * @see #longType()
     */
    @NotNull
    public static TypeTemplate intType() {
        return new ConstTemplate("int", Type.INT);
    }

    /**
     * Creates a 64-bit long integer type template.
     *
     * <p>Long templates are used for large numbers that exceed the 32-bit
     * integer range, such as timestamps, unique IDs, or large counters.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a schema with timestamps and large IDs
     * TypeTemplate eventSchema = DSL.and(
     *     DSL.field("eventId", DSL.longType()),
     *     DSL.field("timestamp", DSL.longType()),
     *     DSL.field("userId", DSL.longType())
     * );
     *
     * // JSON: {"eventId": 9223372036854775807, "timestamp": 1703145600000, ...}
     * }</pre>
     *
     * @return a long type template, never {@code null}
     * @see Type#LONG
     * @see #intType()
     */
    @NotNull
    public static TypeTemplate longType() {
        return new ConstTemplate("long", Type.LONG);
    }

    /**
     * Creates a 32-bit floating-point type template.
     *
     * <p>Float templates are used for decimal numbers where memory efficiency
     * is important and full double precision is not required. Suitable for graphics coordinates, percentages, or
     * normalized values.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a color schema with normalized float values
     * TypeTemplate colorSchema = DSL.and(
     *     DSL.field("red", DSL.floatType()),
     *     DSL.field("green", DSL.floatType()),
     *     DSL.field("blue", DSL.floatType()),
     *     DSL.field("alpha", DSL.floatType())
     * );
     *
     * // JSON: {"red": 0.5, "green": 0.8, "blue": 1.0, "alpha": 0.9}
     * }</pre>
     *
     * @return a float type template, never {@code null}
     * @see Type#FLOAT
     * @see #doubleType()
     */
    @NotNull
    public static TypeTemplate floatType() {
        return new ConstTemplate("float", Type.FLOAT);
    }

    /**
     * Creates a 64-bit double-precision floating-point type template.
     *
     * <p>Double templates provide high-precision decimal numbers, ideal for
     * coordinates, scientific calculations, or any value requiring precision.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a 3D position schema
     * TypeTemplate positionSchema = DSL.and(
     *     DSL.field("x", DSL.doubleType()),
     *     DSL.field("y", DSL.doubleType()),
     *     DSL.field("z", DSL.doubleType())
     * );
     *
     * // JSON: {"x": 123.456789, "y": -78.123456, "z": 0.000001}
     *
     * // Use in a larger entity schema
     * TypeTemplate entitySchema = DSL.and(
     *     DSL.field("name", DSL.string()),
     *     DSL.field("position", positionSchema)
     * );
     * }</pre>
     *
     * @return a double type template, never {@code null}
     * @see Type#DOUBLE
     * @see #floatType()
     */
    @NotNull
    public static TypeTemplate doubleType() {
        return new ConstTemplate("double", Type.DOUBLE);
    }

    /**
     * Creates a string type template for text values.
     *
     * <p>String templates are used for names, identifiers, descriptions,
     * and any textual data. This is one of the most commonly used types.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a user profile schema
     * TypeTemplate profileSchema = DSL.and(
     *     DSL.field("username", DSL.string()),
     *     DSL.field("email", DSL.string()),
     *     DSL.field("displayName", DSL.string()),
     *     DSL.optionalField("bio", DSL.string())
     * );
     *
     * // JSON: {"username": "alice", "email": "alice@example.com", ...}
     *
     * // String lists for tags or categories
     * TypeTemplate articleSchema = DSL.and(
     *     DSL.field("title", DSL.string()),
     *     DSL.field("tags", DSL.list(DSL.string()))
     * );
     * }</pre>
     *
     * @return a string type template, never {@code null}
     * @see Type#STRING
     */
    @NotNull
    public static TypeTemplate string() {
        return new ConstTemplate("string", Type.STRING);
    }

    /**
     * Creates an 8-bit byte type template.
     *
     * <p>Byte templates are used for small integers in the range {@code -128}
     * to {@code 127}, or for raw binary data. Common uses include small enumerations, flags, or compact storage.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a compact data schema
     * TypeTemplate compactSchema = DSL.and(
     *     DSL.field("priority", DSL.byteType()),    // 0-10 priority level
     *     DSL.field("flags", DSL.byteType()),       // Bit flags
     *     DSL.field("version", DSL.byteType())      // Schema version
     * );
     *
     * // JSON: {"priority": 5, "flags": 3, "version": 1}
     * }</pre>
     *
     * @return a byte type template, never {@code null}
     * @see Type#BYTE
     * @see #shortType()
     */
    @NotNull
    public static TypeTemplate byteType() {
        return new ConstTemplate("byte", Type.BYTE);
    }

    /**
     * Creates a 16-bit short integer type template.
     *
     * <p>Short templates are used for medium-sized integers in the range
     * {@code -32,768} to {@code 32,767}. Useful when int is too large but byte is too small, such as for port numbers
     * or small counts.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a network configuration schema
     * TypeTemplate networkSchema = DSL.and(
     *     DSL.field("port", DSL.shortType()),
     *     DSL.field("maxConnections", DSL.shortType()),
     *     DSL.field("timeout", DSL.shortType())
     * );
     *
     * // JSON: {"port": 8080, "maxConnections": 1000, "timeout": 30000}
     * }</pre>
     *
     * @return a short type template, never {@code null}
     * @see Type#SHORT
     * @see #intType()
     * @see #byteType()
     */
    @NotNull
    public static TypeTemplate shortType() {
        return new ConstTemplate("short", Type.SHORT);
    }

    // ==================== Compound Types ====================

    /**
     * Creates a list type template for ordered collections of elements.
     *
     * <p>List templates represent repeatable, ordered collections. In JSON, they
     * serialize as arrays. Lists can contain any element type, including nested lists or complex structures.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Simple list of strings
     * TypeTemplate tagList = DSL.list(DSL.string());
     * // JSON: ["tag1", "tag2", "tag3"]
     *
     * // List of integers
     * TypeTemplate scores = DSL.list(DSL.intType());
     * // JSON: [100, 85, 92, 78]
     *
     * // List of complex objects
     * TypeTemplate itemList = DSL.list(DSL.and(
     *     DSL.field("name", DSL.string()),
     *     DSL.field("quantity", DSL.intType())
     * ));
     * // JSON: [{"name": "sword", "quantity": 1}, {"name": "potion", "quantity": 5}]
     *
     * // Nested lists (2D array)
     * TypeTemplate matrix = DSL.list(DSL.list(DSL.intType()));
     * // JSON: [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
     * }</pre>
     *
     * @param element the type template for list elements, must not be {@code null}
     * @return a list type template, never {@code null}
     * @throws NullPointerException if {@code element} is {@code null}
     * @see Type#list(Type)
     */
    @NotNull
    public static TypeTemplate list(@NotNull final TypeTemplate element) {
        return new ListTemplate(element);
    }

    /**
     * Creates a product type template combining two types (AND/tuple).
     *
     * <p>Product types represent "both A and B" - values that contain multiple
     * components. They are the foundation for building record-like structures by combining multiple fields. Products
     * are right-associative when nested.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Simple pair of two types
     * TypeTemplate nameAndAge = DSL.and(
     *     DSL.field("name", DSL.string()),
     *     DSL.field("age", DSL.intType())
     * );
     *
     * // Nested products for multiple fields
     * TypeTemplate coordinates = DSL.and(
     *     DSL.field("x", DSL.doubleType()),
     *     DSL.and(
     *         DSL.field("y", DSL.doubleType()),
     *         DSL.field("z", DSL.doubleType())
     *     )
     * );
     * }</pre>
     *
     * @param first  the first type template, must not be {@code null}
     * @param second the second type template, must not be {@code null}
     * @return a product type template, never {@code null}
     * @throws NullPointerException if {@code first} or {@code second} is {@code null}
     * @see #and(TypeTemplate...)
     * @see Type#product(Type, Type)
     */
    @NotNull
    public static TypeTemplate and(@NotNull final TypeTemplate first,
                                   @NotNull final TypeTemplate second) {
        return new ProductTemplate(first, second);
    }

    /**
     * Creates a product type template from multiple elements (variadic AND).
     *
     * <p>This is a convenience method for combining more than two types into a
     * single product. The elements are combined right-to-left, creating a nested structure: {@code and(A, B, C, D)}
     * becomes {@code and(A, and(B, and(C, D)))}.</p>
     *
     * <p>This is the primary way to define record-like structures with multiple
     * fields in the DSL.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a complete player schema
     * TypeTemplate playerSchema = DSL.and(
     *     DSL.field("id", DSL.intType()),
     *     DSL.field("name", DSL.string()),
     *     DSL.field("health", DSL.intType()),
     *     DSL.field("position", DSL.and(
     *         DSL.field("x", DSL.doubleType()),
     *         DSL.field("y", DSL.doubleType()),
     *         DSL.field("z", DSL.doubleType())
     *     )),
     *     DSL.optionalField("inventory", DSL.list(DSL.string())),
     *     DSL.remainder()  // Capture any extra fields
     * );
     *
     * // JSON: {
     * //   "id": 42,
     * //   "name": "Alice",
     * //   "health": 100,
     * //   "position": {"x": 10.5, "y": 64.0, "z": -20.3},
     * //   "inventory": ["sword", "shield"]
     * // }
     * }</pre>
     *
     * @param elements the type templates to combine, must have at least 2 elements
     * @return a product type template, never {@code null}
     * @throws IllegalArgumentException if fewer than 2 elements are provided
     * @throws NullPointerException     if {@code elements} or any element is {@code null}
     * @see #and(TypeTemplate, TypeTemplate)
     */
    @NotNull
    public static TypeTemplate and(@NotNull final TypeTemplate... elements) {
        if (elements.length < 2) {
            throw new IllegalArgumentException("Product requires at least 2 elements");
        }
        TypeTemplate result = elements[elements.length - 1];
        for (int i = elements.length - 2; i >= 0; i--) {
            result = new ProductTemplate(elements[i], result);
        }
        return result;
    }

    /**
     * Creates a sum type template representing one of two alternatives (OR/union).
     *
     * <p>Sum types represent "either A or B" - values that are one type or
     * another, but not both. They are useful for representing variants or polymorphic data without a discriminator
     * field.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // A value that is either a string or an integer
     * TypeTemplate stringOrInt = DSL.or(DSL.string(), DSL.intType());
     *
     * // Error that could be a code or a message
     * TypeTemplate errorValue = DSL.or(
     *     DSL.field("code", DSL.intType()),
     *     DSL.field("message", DSL.string())
     * );
     * }</pre>
     *
     * <p><b>Note:</b> For polymorphic data with a type discriminator field,
     * prefer {@link #taggedChoice(String, Map)} instead.</p>
     *
     * @param first  the first alternative type template, must not be {@code null}
     * @param second the second alternative type template, must not be {@code null}
     * @return a sum type template, never {@code null}
     * @throws NullPointerException if {@code first} or {@code second} is {@code null}
     * @see #or(TypeTemplate...)
     * @see #taggedChoice(String, Map)
     * @see Type#sum(Type, Type)
     */
    @NotNull
    public static TypeTemplate or(@NotNull final TypeTemplate first,
                                  @NotNull final TypeTemplate second) {
        return new SumTemplate(first, second);
    }

    /**
     * Creates a sum type template from multiple alternatives (variadic OR).
     *
     * <p>This is a convenience method for combining more than two alternatives.
     * The alternatives are combined right-to-left: {@code or(A, B, C)} becomes {@code or(A, or(B, C))}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // A value that could be any of several types
     * TypeTemplate primitiveValue = DSL.or(
     *     DSL.string(),
     *     DSL.intType(),
     *     DSL.bool(),
     *     DSL.doubleType()
     * );
     *
     * // Different response formats
     * TypeTemplate apiResponse = DSL.or(
     *     DSL.field("data", DSL.string()),           // Success with data
     *     DSL.field("error", DSL.string()),          // Error message
     *     DSL.field("redirect", DSL.string())        // Redirect URL
     * );
     * }</pre>
     *
     * @param alternatives the alternative type templates, must have at least 2 elements
     * @return a sum type template, never {@code null}
     * @throws IllegalArgumentException if fewer than 2 alternatives are provided
     * @throws NullPointerException     if {@code alternatives} or any element is {@code null}
     * @see #or(TypeTemplate, TypeTemplate)
     */
    @NotNull
    public static TypeTemplate or(@NotNull final TypeTemplate... alternatives) {
        if (alternatives.length < 2) {
            throw new IllegalArgumentException("Sum requires at least 2 alternatives");
        }
        TypeTemplate result = alternatives[alternatives.length - 1];
        for (int i = alternatives.length - 2; i >= 0; i--) {
            result = new SumTemplate(alternatives[i], result);
        }
        return result;
    }

    /**
     * Creates an optional type template for nullable or absent values.
     *
     * <p>Optional templates wrap another type to indicate that the value may be
     * absent. In JSON, this typically means the value can be {@code null} or missing entirely. When reading, absent
     * values result in {@code Optional.empty()}.</p>
     *
     * <p><b>Note:</b> For optional fields in a record, prefer {@link #optionalField(String, TypeTemplate)}
     * which handles field absence directly. Use this method for optional values within lists or other contexts.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // A list where elements may be null
     * TypeTemplate sparseList = DSL.list(DSL.optional(DSL.intType()));
     * // JSON: [1, null, 3, null, 5]
     *
     * // Optional nested structure
     * TypeTemplate optionalAddress = DSL.optional(DSL.and(
     *     DSL.field("street", DSL.string()),
     *     DSL.field("city", DSL.string())
     * ));
     *
     * // Compare with optionalField for record fields
     * TypeTemplate person = DSL.and(
     *     DSL.field("name", DSL.string()),
     *     DSL.optionalField("nickname", DSL.string())  // Prefer this for fields
     * );
     * }</pre>
     *
     * @param element the type template for the optional element, must not be {@code null}
     * @return an optional type template, never {@code null}
     * @throws NullPointerException if {@code element} is {@code null}
     * @see #optionalField(String, TypeTemplate)
     * @see Type#optional(Type)
     */
    @NotNull
    public static TypeTemplate optional(@NotNull final TypeTemplate element) {
        return new OptionalTemplate(element);
    }

    // ==================== Field Types ====================

    /**
     * Creates a required field type template that extracts a named field from objects.
     *
     * <p>Field templates specify that a value must be present in a particular field
     * of a map/object structure. If the field is missing during reading, parsing will fail with an error.</p>
     *
     * <p>Fields are typically combined using {@link #and(TypeTemplate...)} to build
     * complete record schemas.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Single required field
     * TypeTemplate nameField = DSL.field("name", DSL.string());
     *
     * // Multiple required fields in a record
     * TypeTemplate personSchema = DSL.and(
     *     DSL.field("firstName", DSL.string()),
     *     DSL.field("lastName", DSL.string()),
     *     DSL.field("age", DSL.intType()),
     *     DSL.field("email", DSL.string())
     * );
     * // JSON: {"firstName": "Alice", "lastName": "Smith", "age": 30, "email": "..."}
     *
     * // Nested object field
     * TypeTemplate userSchema = DSL.and(
     *     DSL.field("id", DSL.intType()),
     *     DSL.field("profile", DSL.and(
     *         DSL.field("bio", DSL.string()),
     *         DSL.field("avatar", DSL.string())
     *     ))
     * );
     * }</pre>
     *
     * @param name the field name in the serialized object, must not be {@code null}
     * @param type the type template for the field's value, must not be {@code null}
     * @return a field type template, never {@code null}
     * @throws NullPointerException if {@code name} or {@code type} is {@code null}
     * @see #optionalField(String, TypeTemplate)
     * @see Type#field(String, Type)
     */
    @NotNull
    public static TypeTemplate field(@NotNull final String name,
                                     @NotNull final TypeTemplate type) {
        return new FieldTemplate(name, type, false);
    }

    /**
     * Creates an optional field type template for fields that may be absent.
     *
     * <p>Optional field templates specify that a value may or may not be present
     * in a particular field. If the field is missing during reading, the result is {@code Optional.empty()} rather than
     * an error.</p>
     *
     * <p>This is the preferred way to handle nullable or optional properties in
     * schemas, especially for fields added in later versions.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Schema with required and optional fields
     * TypeTemplate userSchema = DSL.and(
     *     DSL.field("id", DSL.intType()),             // Required
     *     DSL.field("username", DSL.string()),        // Required
     *     DSL.optionalField("nickname", DSL.string()), // Optional
     *     DSL.optionalField("bio", DSL.string()),      // Optional
     *     DSL.optionalField("age", DSL.intType())      // Optional
     * );
     *
     * // All these JSON objects are valid:
     * // {"id": 1, "username": "alice"}
     * // {"id": 1, "username": "alice", "nickname": "Ali"}
     * // {"id": 1, "username": "alice", "nickname": "Ali", "bio": "Hello!", "age": 25}
     *
     * // Schema evolution - add new optional field in v2
     * TypeTemplate v2Schema = DSL.and(
     *     DSL.field("name", DSL.string()),
     *     DSL.optionalField("version", DSL.intType())  // Added in v2
     * );
     * }</pre>
     *
     * @param name the field name in the serialized object, must not be {@code null}
     * @param type the type template for the field's value when present, must not be {@code null}
     * @return an optional field type template, never {@code null}
     * @throws NullPointerException if {@code name} or {@code type} is {@code null}
     * @see #field(String, TypeTemplate)
     * @see Type#optionalField(String, Type)
     */
    @NotNull
    public static TypeTemplate optionalField(@NotNull final String name,
                                             @NotNull final TypeTemplate type) {
        return new FieldTemplate(name, type, true);
    }

    /**
     * Creates a named type template, giving a symbolic name to a type.
     *
     * <p>Named templates assign an identifier to a type, which is useful for:</p>
     * <ul>
     *   <li>Improving debug output and error messages</li>
     *   <li>Creating type aliases for complex structures</li>
     *   <li>Referencing types by name in recursive definitions</li>
     * </ul>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Name a complex type for clarity
     * TypeTemplate positionType = DSL.named("Position", DSL.and(
     *     DSL.field("x", DSL.doubleType()),
     *     DSL.field("y", DSL.doubleType()),
     *     DSL.field("z", DSL.doubleType())
     * ));
     *
     * // Use the named type in other schemas
     * TypeTemplate entitySchema = DSL.and(
     *     DSL.field("id", DSL.intType()),
     *     DSL.field("position", positionType)
     * );
     *
     * // describe() will show "Position=..." instead of the full structure
     * System.out.println(positionType.describe());
     * }</pre>
     *
     * @param name     the symbolic name for this type, must not be {@code null}
     * @param template the type template to name, must not be {@code null}
     * @return a named type template, never {@code null}
     * @throws NullPointerException if {@code name} or {@code template} is {@code null}
     * @see #recursive(String, Function)
     * @see Type#named(String, Type)
     */
    @NotNull
    public static TypeTemplate named(@NotNull final String name,
                                     @NotNull final TypeTemplate template) {
        return new NamedTemplate(name, template);
    }

    /**
     * Creates a remainder type template that captures all unmatched fields.
     *
     * <p>Remainder templates act as a "catch-all" for fields that aren't explicitly
     * defined in the schema. This is essential for forward compatibility - it allows schemas to preserve unknown fields
     * during migrations rather than discarding them.</p>
     *
     * <p>The remainder is typically placed at the end of a product (AND) type to
     * capture any extra fields present in the data.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Schema that preserves unknown fields
     * TypeTemplate flexibleSchema = DSL.and(
     *     DSL.field("id", DSL.intType()),
     *     DSL.field("name", DSL.string()),
     *     DSL.remainder()  // Captures any other fields
     * );
     *
     * // This JSON preserves "extraField" and "anotherField":
     * // {"id": 1, "name": "test", "extraField": "value", "anotherField": 42}
     *
     * // Without remainder, extra fields would be lost during migration
     * TypeTemplate strictSchema = DSL.and(
     *     DSL.field("id", DSL.intType()),
     *     DSL.field("name", DSL.string())
     *     // No remainder - extra fields will be discarded!
     * );
     *
     * // Use with version migrations to preserve forward-compatible data
     * TypeTemplate v1Schema = DSL.and(
     *     DSL.field("name", DSL.string()),
     *     DSL.remainder()  // Preserves fields added in future versions
     * );
     * }</pre>
     *
     * @return a remainder type template, never {@code null}
     * @see Type#PASSTHROUGH
     * @see #remainderFinder(String...)
     */
    @NotNull
    public static TypeTemplate remainder() {
        return new RemainderTemplate();
    }

    // ==================== Tagged Choice Types ====================

    /**
     * Creates a tagged choice type template (discriminated union) for polymorphic data.
     *
     * <p>Tagged choice templates define polymorphic structures where a discriminator
     * field determines the variant's type. This is the primary way to model inheritance, variants, or "oneOf" patterns
     * in serialized data.</p>
     *
     * <p>When reading, the codec extracts the tag field value, looks up the
     * corresponding type from the choices map, and parses the data using that type. When writing, the tag value is
     * included alongside the serialized content.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define an entity type with different variants
     * TypeTemplate entitySchema = DSL.taggedChoice("type", Map.of(
     *     "player", DSL.and(
     *         DSL.field("name", DSL.string()),
     *         DSL.field("level", DSL.intType()),
     *         DSL.field("health", DSL.intType())
     *     ),
     *     "monster", DSL.and(
     *         DSL.field("species", DSL.string()),
     *         DSL.field("hostile", DSL.bool()),
     *         DSL.field("damage", DSL.intType())
     *     ),
     *     "item", DSL.and(
     *         DSL.field("itemId", DSL.string()),
     *         DSL.field("stackSize", DSL.intType())
     *     )
     * ));
     *
     * // Valid JSON examples:
     * // {"type": "player", "name": "Alice", "level": 42, "health": 100}
     * // {"type": "monster", "species": "dragon", "hostile": true, "damage": 50}
     * // {"type": "item", "itemId": "sword", "stackSize": 1}
     *
     * // Use in a schema with a list of entities
     * TypeTemplate worldSchema = DSL.and(
     *     DSL.field("name", DSL.string()),
     *     DSL.field("entities", DSL.list(entitySchema))
     * );
     * }</pre>
     *
     * @param tagField the name of the discriminator field (e.g., "type", "kind"), must not be {@code null}
     * @param choices  a map from tag values to their corresponding type templates, must not be {@code null}
     * @return a tagged choice type template, never {@code null}
     * @throws NullPointerException if {@code tagField} or {@code choices} is {@code null}
     * @see Type#taggedChoice(String, Map)
     * @see #or(TypeTemplate, TypeTemplate)
     */
    @NotNull
    public static TypeTemplate taggedChoice(@NotNull final String tagField,
                                            @NotNull final Map<String, TypeTemplate> choices
    ) {
        return new TaggedChoiceTemplate(tagField, choices);
    }

    /**
     * Creates a tagged choice type template with explicit key type specification.
     *
     * <p>This variant allows specifying a type template for the discriminator keys.
     * Currently, this behaves identically to {@link #taggedChoice(String, Map)}, but provides a hook for future
     * extensions where key types might be validated or transformed.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Tagged choice with explicit string key type
     * TypeTemplate messageSchema = DSL.taggedChoiceTyped(
     *     "messageType",
     *     DSL.string(),  // Key type
     *     Map.of(
     *         "text", DSL.field("content", DSL.string()),
     *         "image", DSL.field("url", DSL.string()),
     *         "video", DSL.and(
     *             DSL.field("url", DSL.string()),
     *             DSL.field("duration", DSL.intType())
     *         )
     *     )
     * );
     * }</pre>
     *
     * @param tagField the name of the discriminator field, must not be {@code null}
     * @param keyType  the type template for discriminator keys, must not be {@code null}
     * @param choices  a map from tag values to type templates, must not be {@code null}
     * @return a tagged choice type template, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     * @see #taggedChoice(String, Map)
     */
    @NotNull
    public static TypeTemplate taggedChoiceTyped(@NotNull final String tagField,
                                                 @NotNull final TypeTemplate keyType,
                                                 @NotNull final Map<String, TypeTemplate> choices
    ) {
        return new TaggedChoiceTemplate(tagField, choices);
    }

    // ==================== Type Parameter References ====================

    /**
     * Creates a type parameter reference for parameterized types.
     *
     * <p>Type parameter references (also called "identity" templates) reference
     * a type from the surrounding {@link TypeFamily} by index. This enables parameterized type definitions where the
     * actual type is provided at instantiation time.</p>
     *
     * <p>This is an advanced feature primarily used internally for implementing
     * recursive types and type-level polymorphism.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Reference type parameter at index 0
     * TypeTemplate selfRef = DSL.id(0);
     *
     * // Use in a parameterized template
     * TypeTemplate listElement = family -> Type.list(family.apply(0));
     *
     * // In recursive definitions, id(0) references the type being defined
     * TypeTemplate linkedList = DSL.recursive("LinkedList", self ->
     *     DSL.optional(DSL.and(
     *         DSL.field("value", DSL.intType()),
     *         DSL.field("next", self)  // References the recursive type
     *     ))
     * );
     * }</pre>
     *
     * @param index the zero-based index of the type parameter in the family
     * @return a type parameter reference template, never {@code null}
     * @throws IndexOutOfBoundsException when applied to a family without that index
     * @see TypeFamily#apply(int)
     * @see #recursive(String, Function)
     */
    @NotNull
    public static TypeTemplate id(final int index) {
        return new IdTemplate(index);
    }

    /**
     * Creates a recursive type definition that can reference itself.
     *
     * <p>Recursive templates enable defining self-referential data structures
     * like linked lists, trees, or nested configurations. The definition function receives a template that represents
     * "self" - a reference to the type being defined.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a linked list: Optional<{value: Int, next: Self}>
     * TypeTemplate linkedList = DSL.recursive("LinkedList", self ->
     *     DSL.optional(DSL.and(
     *         DSL.field("value", DSL.intType()),
     *         DSL.field("next", self)
     *     ))
     * );
     * // JSON: {"value": 1, "next": {"value": 2, "next": {"value": 3, "next": null}}}
     *
     * // Define a binary tree
     * TypeTemplate binaryTree = DSL.recursive("BinaryTree", self ->
     *     DSL.and(
     *         DSL.field("value", DSL.intType()),
     *         DSL.optionalField("left", self),
     *         DSL.optionalField("right", self)
     *     )
     * );
     * // JSON: {"value": 5, "left": {"value": 3}, "right": {"value": 7, "left": {"value": 6}}}
     *
     * // Define a comment thread with nested replies
     * TypeTemplate commentThread = DSL.recursive("Comment", self ->
     *     DSL.and(
     *         DSL.field("author", DSL.string()),
     *         DSL.field("text", DSL.string()),
     *         DSL.field("replies", DSL.list(self))
     *     )
     * );
     * }</pre>
     *
     * @param name       the name for this recursive type (used in describe()), must not be {@code null}
     * @param definition a function that takes a self-reference template and returns the complete type definition, must
     *                   not be {@code null}
     * @return a recursive type template, never {@code null}
     * @throws NullPointerException if {@code name} or {@code definition} is {@code null}
     * @see TypeFamily#recursive(Function)
     * @see #named(String, TypeTemplate)
     */
    @NotNull
    public static TypeTemplate recursive(@NotNull final String name,
                                         @NotNull final Function<TypeTemplate, TypeTemplate> definition) {
        return new RecursiveTemplate(name, definition);
    }

    // ==================== Finders ====================

    /**
     * Creates a finder that locates a specific field in a map/object structure.
     *
     * <p>Field finders navigate into a named field within dynamic data. They are
     * used with {@link de.splatgames.aether.datafixers.api.type.Typed#getAt} and
     * {@link de.splatgames.aether.datafixers.api.type.Typed#updateAt} to access nested values.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Find the "name" field
     * Finder<Object> nameFinder = DSL.fieldFinder("name");
     *
     * // Use with typed values
     * Typed<Player> typed = ...;
     * DataResult<Dynamic<JsonElement>> name = typed.getAt(
     *     GsonOps.INSTANCE,
     *     nameFinder
     * );
     *
     * // Update a nested field
     * DataResult<Typed<Player>> updated = typed.updateAt(
     *     GsonOps.INSTANCE,
     *     DSL.fieldFinder("health"),
     *     d -> d.createInt(100)
     * );
     * }</pre>
     *
     * @param name the field name to find, must not be {@code null}
     * @return a finder that locates the specified field, never {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     * @see Finder#field(String)
     * @see de.splatgames.aether.datafixers.api.type.Typed#getAt
     */
    @NotNull
    public static Finder<Object> fieldFinder(@NotNull final String name) {
        return Finder.field(name);
    }

    /**
     * Creates a finder that locates an element at a specific index in a list.
     *
     * <p>Index finders navigate into a specific position within a list/array.
     * Indices are zero-based. Negative indices or indices beyond the list bounds will result in {@code null} when
     * accessed.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Find the first element
     * Finder<Object> firstFinder = DSL.indexFinder(0);
     *
     * // Find the third element
     * Finder<Object> thirdFinder = DSL.indexFinder(2);
     *
     * // Use with a list type
     * Typed<List<String>> typed = ...;
     * DataResult<Dynamic<JsonElement>> first = typed.getAt(
     *     GsonOps.INSTANCE,
     *     firstFinder
     * );
     *
     * // Update the second element
     * DataResult<Typed<List<String>>> updated = typed.updateAt(
     *     GsonOps.INSTANCE,
     *     DSL.indexFinder(1),
     *     d -> d.createString("modified")
     * );
     * }</pre>
     *
     * @param index the zero-based index to find
     * @return a finder that locates the element at the specified index, never {@code null}
     * @see Finder#index(int)
     */
    @NotNull
    public static Finder<Object> indexFinder(final int index) {
        return Finder.index(index);
    }

    /**
     * Creates a finder that locates all fields except the specified exclusions.
     *
     * <p>Remainder finders collect all fields that aren't in the exclusion list.
     * This is useful for accessing or modifying "extra" fields that weren't explicitly defined in the schema.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Find all fields except "id" and "name"
     * Finder<Object> extrasFinder = DSL.remainderFinder("id", "name");
     *
     * // Get all extra fields from a typed value
     * Typed<Player> typed = ...;
     * DataResult<Dynamic<JsonElement>> extras = typed.getAt(
     *     GsonOps.INSTANCE,
     *     extrasFinder
     * );
     *
     * // Process unknown fields during migration
     * DataResult<Typed<Player>> updated = typed.updateAt(
     *     GsonOps.INSTANCE,
     *     DSL.remainderFinder("id", "version"),
     *     dynamic -> {
     *         // Transform all non-core fields
     *         return dynamic;
     *     }
     * );
     * }</pre>
     *
     * @param excludedFields the field names to exclude from the remainder, must not be {@code null}
     * @return a finder that locates all fields except those specified, never {@code null}
     * @throws NullPointerException if {@code excludedFields} is {@code null}
     * @see Finder#remainder(String...)
     * @see #remainder()
     */
    @NotNull
    public static Finder<Object> remainderFinder(@NotNull final String... excludedFields) {
        return Finder.remainder(excludedFields);
    }

    // ==================== Template Implementations ====================

    /**
     * A constant type template that always produces the same fixed type.
     *
     * <p>Constant templates represent primitive or predefined types that don't
     * depend on type parameters. They always return the same {@link Type} regardless of the {@link TypeFamily}
     * provided.</p>
     *
     * <p>This is used internally by the primitive type factory methods like
     * {@link #bool()}, {@link #intType()}, {@link #string()}, etc.</p>
     */
    private static final class ConstTemplate implements TypeTemplate {
        private final String name;
        private final Type<?> type;

        ConstTemplate(final String name,
                      final Type<?> type) {
            this.name = name;
            this.type = type;
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            return type;
        }

        @NotNull
        @Override
        public String describe() {
            return name;
        }
    }

    /**
     * A type parameter reference template that retrieves a type from the family.
     *
     * <p>Identity templates (named "Id" for the identity operation in type theory)
     * reference a type at a specific index in the surrounding {@link TypeFamily}. This enables parameterized and
     * recursive type definitions.</p>
     *
     * <p>The description uses µ (mu) notation: {@code µ0} for index 0, etc.,
     * following the convention from type theory for recursive types.</p>
     */
    private static final class IdTemplate implements TypeTemplate {
        private final int index;

        IdTemplate(final int index) {
            this.index = index;
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            return family.apply(index);
        }

        @NotNull
        @Override
        public String describe() {
            return "µ" + index;
        }
    }

    /**
     * A product type template combining two types (A × B).
     *
     * <p>Product templates represent "AND" combinations - values that contain
     * both a first and second component. When applied to a family, they instantiate both sub-templates and combine them
     * using {@link Type#product}.</p>
     *
     * <p>The description uses × (times) notation: {@code (A × B)}.</p>
     */
    private static final class ProductTemplate implements TypeTemplate {
        private final TypeTemplate first;
        private final TypeTemplate second;

        ProductTemplate(final TypeTemplate first,
                        final TypeTemplate second) {
            this.first = first;
            this.second = second;
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            return Type.product(first.apply(family), second.apply(family));
        }

        @NotNull
        @Override
        public String describe() {
            return "(" + first.describe() + " × " + second.describe() + ")";
        }
    }

    /**
     * A sum type template representing one of two alternatives (A + B).
     *
     * <p>Sum templates represent "OR" combinations - values that are either
     * the left type or the right type. When applied to a family, they instantiate both sub-templates and combine them
     * using {@link Type#sum}.</p>
     *
     * <p>The description uses + (plus) notation: {@code (A + B)}.</p>
     */
    private static final class SumTemplate implements TypeTemplate {
        private final TypeTemplate left;
        private final TypeTemplate right;

        SumTemplate(final TypeTemplate left,
                    final TypeTemplate right) {
            this.left = left;
            this.right = right;
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            return Type.sum(left.apply(family), right.apply(family));
        }

        @NotNull
        @Override
        public String describe() {
            return "(" + left.describe() + " + " + right.describe() + ")";
        }
    }

    /**
     * A list type template for ordered collections of elements.
     *
     * <p>List templates represent repeatable collections. When applied to a family,
     * they instantiate the element template and wrap it with {@link Type#list}.</p>
     *
     * <p>The description uses generic notation: {@code List<E>}.</p>
     */
    private static final class ListTemplate implements TypeTemplate {
        private final TypeTemplate element;

        ListTemplate(final TypeTemplate element) {
            this.element = element;
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            return Type.list(element.apply(family));
        }

        @NotNull
        @Override
        public String describe() {
            return "List<" + element.describe() + ">";
        }
    }

    /**
     * An optional type template for nullable or absent values.
     *
     * <p>Optional templates wrap another type to indicate the value may be
     * absent. When applied to a family, they instantiate the element template and wrap it with
     * {@link Type#optional}.</p>
     *
     * <p>The description uses generic notation: {@code Optional<E>}.</p>
     */
    private static final class OptionalTemplate implements TypeTemplate {
        private final TypeTemplate element;

        OptionalTemplate(final TypeTemplate element) {
            this.element = element;
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            return Type.optional(element.apply(family));
        }

        @NotNull
        @Override
        public String describe() {
            return "Optional<" + element.describe() + ">";
        }
    }

    /**
     * A field type template for extracting named fields from objects.
     *
     * <p>Field templates specify that a value should be read from/written to
     * a specific named field in a map structure. They can be required or optional.</p>
     *
     * <p>The description uses colon notation: {@code name: Type} for required
     * fields, {@code ?name: Type} for optional fields.</p>
     */
    private static final class FieldTemplate implements TypeTemplate {
        private final String name;
        private final TypeTemplate type;
        private final boolean optional;

        FieldTemplate(final String name,
                      final TypeTemplate type,
                      final boolean optional) {
            this.name = name;
            this.type = type;
            this.optional = optional;
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            final Type<?> fieldType = type.apply(family);
            return optional ? Type.optionalField(name, fieldType) : Type.field(name, fieldType);
        }

        @NotNull
        @Override
        public String describe() {
            return (optional ? "?" : "") + name + ": " + type.describe();
        }
    }

    /**
     * A named type template that assigns a symbolic name to a type.
     *
     * <p>Named templates wrap another template and give it a symbolic name.
     * This improves debug output and enables type aliasing for complex structures.</p>
     *
     * <p>The description uses equals notation: {@code Name=Type}.</p>
     */
    private static final class NamedTemplate implements TypeTemplate {
        private final String name;
        private final TypeTemplate template;

        NamedTemplate(final String name,
                      final TypeTemplate template) {
            this.name = name;
            this.template = template;
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            return Type.named(name, template.apply(family));
        }

        @NotNull
        @Override
        public String describe() {
            return name + "=" + template.describe();
        }
    }

    /**
     * A remainder type template that captures all unmatched fields.
     *
     * <p>Remainder templates act as a passthrough for fields not explicitly
     * defined in the schema. They always produce {@link Type#PASSTHROUGH}.</p>
     *
     * <p>The description uses ellipsis notation: {@code ...}.</p>
     */
    private static final class RemainderTemplate implements TypeTemplate {

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            return Type.PASSTHROUGH;
        }

        @NotNull
        @Override
        public String describe() {
            return "...";
        }
    }

    /**
     * A tagged choice type template for discriminated unions.
     *
     * <p>Tagged choice templates define polymorphic types where a discriminator
     * field determines the variant. When applied, they resolve all choice templates and create a
     * {@link Type.TaggedChoiceType}.</p>
     *
     * <p>The description lists all choices: {@code TaggedChoice<tag>{a -> A, b -> B}}.</p>
     */
    private static final class TaggedChoiceTemplate implements TypeTemplate {
        private final String tagField;
        private final Map<String, TypeTemplate> choices;

        TaggedChoiceTemplate(final String tagField,
                             final Map<String, TypeTemplate> choices) {
            this.tagField = tagField;
            this.choices = Map.copyOf(choices);
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            final Map<String, Type<?>> resolvedChoices = choices.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().apply(family)
                    ));
            return Type.taggedChoice(tagField, resolvedChoices);
        }

        @NotNull
        @Override
        public String describe() {
            final String choicesStr = choices.entrySet().stream()
                    .map(e -> e.getKey() + " -> " + e.getValue().describe())
                    .collect(Collectors.joining(", "));
            return "TaggedChoice<" + tagField + ">{" + choicesStr + "}";
        }
    }

    /**
     * A recursive type template for self-referential type definitions.
     *
     * <p>Recursive templates enable types that reference themselves, such as
     * linked lists or trees. They use {@link TypeFamily#recursive} to create the self-reference and pass it to the
     * definition function.</p>
     *
     * <p>The description uses µ (mu) notation from type theory: {@code µName.Body},
     * where Name is bound in Body for self-reference.</p>
     */
    private static final class RecursiveTemplate implements TypeTemplate {
        private final String name;
        private final Function<TypeTemplate, TypeTemplate> definition;

        RecursiveTemplate(final String name,
                          final Function<TypeTemplate, TypeTemplate> definition) {
            this.name = name;
            this.definition = definition;
        }

        @NotNull
        @Override
        public Type<?> apply(@NotNull final TypeFamily family) {
            // Create a self-referential type using TypeFamily.recursive
            final TypeFamily recursiveFamily = TypeFamily.recursive(self -> {
                // Create a template that references the recursive type
                final TypeTemplate selfRef = new TypeTemplate() {
                    @NotNull
                    @Override
                    public Type<?> apply(@NotNull final TypeFamily f) {
                        return self.apply(0);
                    }

                    @NotNull
                    @Override
                    public String describe() {
                        return name;
                    }
                };
                return definition.apply(selfRef).apply(family);
            });
            return recursiveFamily.apply(0);
        }

        @NotNull
        @Override
        public String describe() {
            return "µ" + name + "." + definition.apply(id(0)).describe();
        }
    }
}
