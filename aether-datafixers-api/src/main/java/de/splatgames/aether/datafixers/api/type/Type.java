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

package de.splatgames.aether.datafixers.api.type;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.codec.Codecs;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Either;
import de.splatgames.aether.datafixers.api.util.Pair;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a type in the data fixing system.
 *
 * <p>Types define the structure of data and provide codecs for serialization.
 * They are the building blocks of schemas used for data migration, enabling type-safe transformations of persisted data
 * across versions.</p>
 *
 * <h2>Type Categories</h2>
 * <ul>
 *   <li><b>Primitive Types:</b> {@link #BOOL}, {@link #INT}, {@link #LONG}, {@link #FLOAT},
 *       {@link #DOUBLE}, {@link #BYTE}, {@link #SHORT}, {@link #STRING}</li>
 *   <li><b>Collection Types:</b> {@link #list(Type)}, {@link #optional(Type)}</li>
 *   <li><b>Algebraic Types:</b> {@link #product(Type, Type)} (Pair), {@link #sum(Type, Type)} (Either)</li>
 *   <li><b>Field Types:</b> {@link #field(String, Type)}, {@link #optionalField(String, Type)}</li>
 *   <li><b>Special Types:</b> {@link #PASSTHROUGH}, {@link #taggedChoice(String, Map)}, {@link #named(String, Type)}</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define a type for a Person structure
 * Type<Dynamic<?>> personType = Type.taggedChoice("type",
 *     Map.of(
 *         "player", Type.STRING,
 *         "npc", Type.product(Type.STRING, Type.INT)
 *     )
 * );
 *
 * // Read a typed value from dynamic data
 * DataResult<Typed<Dynamic<?>>> typed = personType.readTyped(dynamicData);
 *
 * // Write a value to dynamic format
 * DataResult<Dynamic<JsonElement>> encoded = personType.write(value, GsonOps.INSTANCE);
 * }</pre>
 *
 * <h2>Integration with DataFixers</h2>
 * <p>Types are used by {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule}
 * to match and transform data structures during version upgrades.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Type instances are typically immutable and thread-safe.</p>
 *
 * @param <A> the Java type this Type represents
 * @author Erik Pförtner
 * @see Typed
 * @see TypeReference
 * @see Codec
 * @since 0.1.0
 */
public interface Type<A> {

    /**
     * Returns the type reference that uniquely identifies this type.
     *
     * <p>The type reference serves as the type's identity in the data fixing system.
     * Two types with equal references are considered the same type for matching purposes in
     * {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Type<String> stringType = Type.STRING;
     * TypeReference ref = stringType.reference();
     * System.out.println(ref.getId());  // "string"
     *
     * // Use for type matching
     * if (someType.reference().equals(playerType.reference())) {
     *     // This is a player type
     * }
     * }</pre>
     *
     * @return the type reference identifier, never {@code null}
     */
    @NotNull
    TypeReference reference();

    /**
     * Returns the codec used for serializing and deserializing values of this type.
     *
     * <p>The codec handles conversion between Java objects of type {@code A} and
     * their serialized representations. It is used by {@link #read(Dynamic)} and {@link #write(Object, DynamicOps)} for
     * data transformation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Type<String> stringType = Type.STRING;
     * Codec<String> codec = stringType.codec();
     *
     * // Encode a value
     * DataResult<JsonElement> encoded = codec.encodeStart(GsonOps.INSTANCE, "hello");
     *
     * // Decode a value
     * DataResult<Pair<String, JsonElement>> decoded = codec.decode(GsonOps.INSTANCE, jsonElement);
     * }</pre>
     *
     * @return the codec for this type, never {@code null}
     */
    @NotNull
    Codec<A> codec();

    /**
     * Returns a human-readable description of this type for debugging and logging.
     *
     * <p>The description provides a textual representation of the type structure,
     * useful for error messages, logs, and debugging. By default, it returns the type reference ID, but complex types
     * override this to show their structure.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Type<String> stringType = Type.STRING;
     * System.out.println(stringType.describe());  // "string"
     *
     * Type<List<Integer>> listType = Type.list(Type.INT);
     * System.out.println(listType.describe());  // "List<int>"
     *
     * Type<Pair<String, Integer>> pairType = Type.product(Type.STRING, Type.INT);
     * System.out.println(pairType.describe());  // "(string × int)"
     * }</pre>
     *
     * @return a human-readable description of this type, never {@code null}
     */
    @NotNull
    default String describe() {
        return reference().getId();
    }

    /**
     * Returns the child types that this type contains.
     *
     * <p>Child types are used by traversal combinators to recursively transform
     * nested data structures. Primitive types have no children (return an empty list), while composite types return
     * their component types.</p>
     *
     * <h4>Child Types by Type Category</h4>
     * <ul>
     *   <li><b>Primitive Types</b> ({@code BOOL}, {@code INT}, etc.): No children</li>
     *   <li><b>List Types</b>: Single child (element type)</li>
     *   <li><b>Optional Types</b>: Single child (element type)</li>
     *   <li><b>Product Types</b>: Two children (first and second)</li>
     *   <li><b>Sum Types</b>: Two children (left and right)</li>
     *   <li><b>Field Types</b>: Single child (field type)</li>
     *   <li><b>Named Types</b>: Single child (target type)</li>
     *   <li><b>Tagged Choice Types</b>: All choice variant types</li>
     * </ul>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Primitive - no children
     * Type<String> stringType = Type.STRING;
     * List<Type<?>> empty = stringType.children();  // []
     *
     * // List - one child
     * Type<List<Integer>> listType = Type.list(Type.INT);
     * List<Type<?>> listChildren = listType.children();  // [INT]
     *
     * // Product - two children
     * Type<Pair<String, Integer>> pairType = Type.product(Type.STRING, Type.INT);
     * List<Type<?>> pairChildren = pairType.children();  // [STRING, INT]
     * }</pre>
     *
     * @return an unmodifiable list of child types, empty for primitives, never {@code null}
     * @see de.splatgames.aether.datafixers.api.rewrite.Rules#all(TypeRewriteRule)
     * @see de.splatgames.aether.datafixers.api.rewrite.Rules#everywhere(TypeRewriteRule)
     */
    @NotNull
    default List<Type<?>> children() {
        return List.of();
    }

    /**
     * Reads (deserializes) a value of this type from a dynamic representation.
     *
     * <p>This method uses the type's codec to parse the dynamic value into a
     * typed Java object. If parsing fails (e.g., missing fields, wrong format), an error result is returned.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Given JSON: {"name": "Alice", "age": 30}
     * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     *
     * Type<Person> personType = ...;
     * DataResult<Person> result = personType.read(dynamic);
     *
     * result.ifSuccess(person -> {
     *     System.out.println(person.name());  // "Alice"
     * });
     *
     * result.ifError(error -> {
     *     System.err.println("Parse failed: " + error.message());
     * });
     * }</pre>
     *
     * @param dynamic the dynamic value to read from, must not be {@code null}
     * @param <T>     the underlying data format type (e.g., JsonElement)
     * @return a {@link DataResult} containing the parsed value or an error, never {@code null}
     * @throws NullPointerException if {@code dynamic} is {@code null}
     */
    @NotNull
    default <T> DataResult<A> read(@NotNull final Dynamic<T> dynamic) {
        Preconditions.checkNotNull(dynamic, "dynamic must not be null");
        return codec().parse(dynamic.ops(), dynamic.value());
    }

    /**
     * Writes (serializes) a value of this type to a dynamic representation.
     *
     * <p>This method uses the type's codec to encode the Java object into the
     * target format specified by the {@link DynamicOps}. The result is wrapped in a {@link Dynamic} for convenient
     * manipulation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Person person = new Person("Alice", 30);
     * Type<Person> personType = ...;
     *
     * // Write to JSON format
     * DataResult<Dynamic<JsonElement>> result = personType.write(person, GsonOps.INSTANCE);
     *
     * result.ifSuccess(dynamic -> {
     *     JsonElement json = dynamic.value();
     *     System.out.println(json);  // {"name":"Alice","age":30}
     * });
     *
     * // Write to NBT format
     * DataResult<Dynamic<Tag>> nbtResult = personType.write(person, NbtOps.INSTANCE);
     * }</pre>
     *
     * @param value the value to serialize, must not be {@code null}
     * @param ops   the dynamic operations for the target format, must not be {@code null}
     * @param <T>   the underlying data format type (e.g., JsonElement, Tag)
     * @return a {@link DataResult} containing the encoded dynamic or an error, never {@code null}
     * @throws NullPointerException if {@code value} or {@code ops} is {@code null}
     */
    @NotNull
    default <T> DataResult<Dynamic<T>> write(@NotNull final A value,
                                             @NotNull final DynamicOps<T> ops) {
        Preconditions.checkNotNull(value, "value must not be null");
        Preconditions.checkNotNull(ops, "ops must not be null");
        return codec().encodeStart(ops, value).map(t -> new Dynamic<>(ops, t));
    }

    /**
     * Reads a value from dynamic data and wraps it as a {@link Typed} value.
     *
     * <p>This method combines parsing with type tagging, producing a {@link Typed}
     * that carries both the value and its type information. This is the primary entry point for reading data into the
     * type-safe data fixing system.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Parse JSON into a typed player
     * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     * Type<Player> playerType = ...;
     *
     * DataResult<Typed<Player>> result = playerType.readTyped(dynamic);
     *
     * result.ifSuccess(typed -> {
     *     Player player = typed.value();
     *     Type<Player> type = typed.type();
     *
     *     // The typed value can be used with TypeRewriteRules
     *     Typed<?> migrated = migrationRule.apply(typed);
     * });
     * }</pre>
     *
     * @param dynamic the dynamic value to read from, must not be {@code null}
     * @param <T>     the underlying data format type (e.g., JsonElement)
     * @return a {@link DataResult} containing the typed value or an error, never {@code null}
     * @throws NullPointerException if {@code dynamic} is {@code null}
     * @see Typed
     */
    @NotNull
    default <T> DataResult<Typed<A>> readTyped(@NotNull final Dynamic<T> dynamic) {
        Preconditions.checkNotNull(dynamic, "dynamic must not be null");
        return read(dynamic).map(value -> new Typed<>(this, value));
    }

    // ==================== Primitive Types ====================

    /**
     * Boolean type.
     */
    Type<Boolean> BOOL = primitive("bool", Codecs.BOOL);

    /**
     * Integer type.
     */
    Type<Integer> INT = primitive("int", Codecs.INT);

    /**
     * Long type.
     */
    Type<Long> LONG = primitive("long", Codecs.LONG);

    /**
     * Float type.
     */
    Type<Float> FLOAT = primitive("float", Codecs.FLOAT);

    /**
     * Double type.
     */
    Type<Double> DOUBLE = primitive("double", Codecs.DOUBLE);

    /**
     * Byte type.
     */
    Type<Byte> BYTE = primitive("byte", Codecs.BYTE);

    /**
     * Short type.
     */
    Type<Short> SHORT = primitive("short", Codecs.SHORT);

    /**
     * String type.
     */
    Type<String> STRING = primitive("string", Codecs.STRING);

    /**
     * Passthrough type - preserves any value as-is.
     */
    Type<Dynamic<?>> PASSTHROUGH = new Type<>() {
        @NotNull
        @Override
        public TypeReference reference() {
            return new TypeReference("passthrough");
        }

        @NotNull
        @Override
        public Codec<Dynamic<?>> codec() {
            return new Codec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final Dynamic<?> input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T prefix) {
                    Preconditions.checkNotNull(input, "input must not be null");
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(prefix, "prefix must not be null");
                    return DataResult.success(input.convert(ops).value());
                }

                @NotNull
                @Override
                public <T> DataResult<Pair<Dynamic<?>, T>> decode(@NotNull final DynamicOps<T> ops,
                                                                  @NotNull final T input) {
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(input, "input must not be null");
                    return DataResult.success(Pair.of(new Dynamic<>(ops, input), ops.empty()));
                }
            };
        }

        @NotNull
        @Override
        public String describe() {
            return "...";
        }
    };

    // ==================== Factory Methods ====================

    /**
     * Creates a primitive type from a name and codec.
     *
     * <p>Primitive types are the building blocks for more complex types. They
     * represent simple, atomic values like integers, strings, or booleans. The name becomes the type's reference
     * ID.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create a custom UUID type
     * Codec<UUID> uuidCodec = Codecs.STRING.xmap(
     *     UUID::fromString,
     *     UUID::toString
     * );
     * Type<UUID> uuidType = Type.primitive("uuid", uuidCodec);
     *
     * // Use the type
     * UUID id = UUID.randomUUID();
     * DataResult<Dynamic<JsonElement>> encoded = uuidType.write(id, GsonOps.INSTANCE);
     * }</pre>
     *
     * @param name  the type name (becomes the reference ID), must not be {@code null}
     * @param codec the codec for serialization, must not be {@code null}
     * @param <A>   the Java type this type represents
     * @return a new primitive type, never {@code null}
     * @throws NullPointerException if {@code name} or {@code codec} is {@code null}
     */
    @NotNull
    static <A> Type<A> primitive(@NotNull final String name,
                                 @NotNull final Codec<A> codec) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(codec, "codec must not be null");
        return new Type<>() {
            private final TypeReference ref = new TypeReference(name);

            @NotNull
            @Override
            public TypeReference reference() {
                return this.ref;
            }

            @NotNull
            @Override
            public Codec<A> codec() {
                return codec;
            }

            @NotNull
            @Override
            public String describe() {
                return name;
            }
        };
    }

    /**
     * Creates a list type containing elements of the specified type.
     *
     * <p>List types represent ordered, repeatable collections of elements.
     * They serialize as arrays in JSON or list tags in NBT.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create a list of integers
     * Type<List<Integer>> intListType = Type.list(Type.INT);
     *
     * // Read from JSON: [1, 2, 3]
     * DataResult<List<Integer>> result = intListType.read(dynamic);
     *
     * // Create nested lists
     * Type<List<List<String>>> nestedType = Type.list(Type.list(Type.STRING));
     * }</pre>
     *
     * @param elementType the type of elements in the list, must not be {@code null}
     * @param <A>         the Java type of list elements
     * @return a new list type, never {@code null}
     * @throws NullPointerException if {@code elementType} is {@code null}
     */
    @NotNull
    static <A> Type<List<A>> list(@NotNull final Type<A> elementType) {
        Preconditions.checkNotNull(elementType, "elementType must not be null");
        return new Type<>() {
            private final TypeReference ref = new TypeReference("list[" + elementType.reference().getId() + "]");

            @NotNull
            @Override
            public TypeReference reference() {
                return this.ref;
            }

            @NotNull
            @Override
            public Codec<List<A>> codec() {
                return Codecs.list(elementType.codec());
            }

            @NotNull
            @Override
            public String describe() {
                return "List<" + elementType.describe() + ">";
            }

            @NotNull
            @Override
            public List<Type<?>> children() {
                return List.of(elementType);
            }
        };
    }

    /**
     * Creates an optional type that may or may not contain a value.
     *
     * <p>Optional types represent nullable or absent values. When reading, a missing
     * value results in {@link Optional#empty()}. When writing, empty optionals are typically omitted from the
     * output.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create an optional string type
     * Type<Optional<String>> optStringType = Type.optional(Type.STRING);
     *
     * // Reading present value: "hello" -> Optional.of("hello")
     * // Reading absent value: null -> Optional.empty()
     *
     * // Useful for optional fields in records
     * Type<Optional<Integer>> optAge = Type.optional(Type.INT);
     * }</pre>
     *
     * @param elementType the type of the optional element, must not be {@code null}
     * @param <A>         the Java type of the optional element
     * @return a new optional type, never {@code null}
     * @throws NullPointerException if {@code elementType} is {@code null}
     */
    @NotNull
    static <A> Type<Optional<A>> optional(@NotNull final Type<A> elementType) {
        Preconditions.checkNotNull(elementType, "elementType must not be null");
        return new Type<>() {
            private final TypeReference ref = new TypeReference("optional[" + elementType.reference().getId() + "]");

            @NotNull
            @Override
            public TypeReference reference() {
                return this.ref;
            }

            @NotNull
            @Override
            public Codec<Optional<A>> codec() {
                return Codecs.optional(elementType.codec());
            }

            @NotNull
            @Override
            public String describe() {
                return "Optional<" + elementType.describe() + ">";
            }

            @NotNull
            @Override
            public List<Type<?>> children() {
                return List.of(elementType);
            }
        };
    }

    /**
     * Creates a product type (pair) combining two types.
     *
     * <p>Product types represent "AND" combinations - values that contain both
     * a first component and a second component. They are the algebraic dual of sum types and correspond to tuples or
     * pairs in most languages.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create a pair of name and age
     * Type<Pair<String, Integer>> personType = Type.product(Type.STRING, Type.INT);
     *
     * // Read from JSON: ["Alice", 30]
     * Pair<String, Integer> person = personType.read(dynamic).result().orElseThrow();
     * String name = person.first();   // "Alice"
     * Integer age = person.second();  // 30
     *
     * // Nested products for multiple fields
     * Type<Pair<String, Pair<Integer, Boolean>>> triple =
     *     Type.product(Type.STRING, Type.product(Type.INT, Type.BOOL));
     * }</pre>
     *
     * @param first  the type of the first component, must not be {@code null}
     * @param second the type of the second component, must not be {@code null}
     * @param <A>    the Java type of the first component
     * @param <B>    the Java type of the second component
     * @return a new product type, never {@code null}
     * @throws NullPointerException if {@code first} or {@code second} is {@code null}
     */
    @NotNull
    static <A, B> Type<Pair<A, B>> product(@NotNull final Type<A> first,
                                           @NotNull final Type<B> second) {
        Preconditions.checkNotNull(first, "first must not be null");
        Preconditions.checkNotNull(second, "second must not be null");
        return new Type<>() {
            private final TypeReference ref = new TypeReference(
                    "(" + first.reference().getId() + " × " + second.reference().getId() + ")"
            );

            @NotNull
            @Override
            public TypeReference reference() {
                return this.ref;
            }

            @NotNull
            @Override
            public Codec<Pair<A, B>> codec() {
                return Codecs.pair(first.codec(), second.codec());
            }

            @NotNull
            @Override
            public String describe() {
                return "(" + first.describe() + " × " + second.describe() + ")";
            }

            @NotNull
            @Override
            public List<Type<?>> children() {
                return List.of(first, second);
            }
        };
    }

    /**
     * Creates a sum type (either) representing one of two alternatives.
     *
     * <p>Sum types represent "OR" combinations - values that are either of the
     * left type or the right type, but not both. They are the algebraic dual of product types and correspond to tagged
     * unions or discriminated unions.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create an either of string or integer
     * Type<Either<String, Integer>> stringOrInt = Type.sum(Type.STRING, Type.INT);
     *
     * // Parse could result in either type
     * Either<String, Integer> value = stringOrInt.read(dynamic).result().orElseThrow();
     *
     * value.map(
     *     str -> System.out.println("Got string: " + str),
     *     num -> System.out.println("Got number: " + num)
     * );
     *
     * // Check which side
     * if (value.isLeft()) {
     *     String s = value.left().orElseThrow();
     * }
     * }</pre>
     *
     * @param left  the type of the left alternative, must not be {@code null}
     * @param right the type of the right alternative, must not be {@code null}
     * @param <A>   the Java type of the left alternative
     * @param <B>   the Java type of the right alternative
     * @return a new sum type, never {@code null}
     * @throws NullPointerException if {@code left} or {@code right} is {@code null}
     */
    @NotNull
    static <A, B> Type<Either<A, B>> sum(@NotNull final Type<A> left,
                                         @NotNull final Type<B> right) {
        Preconditions.checkNotNull(left, "left must not be null");
        Preconditions.checkNotNull(right, "right must not be null");
        return new Type<>() {
            private final TypeReference ref = new TypeReference(
                    "(" + left.reference().getId() + " + " + right.reference().getId() + ")"
            );

            @NotNull
            @Override
            public TypeReference reference() {
                return this.ref;
            }

            @NotNull
            @Override
            public Codec<Either<A, B>> codec() {
                return Codecs.either(left.codec(), right.codec());
            }

            @NotNull
            @Override
            public String describe() {
                return "(" + left.describe() + " + " + right.describe() + ")";
            }

            @NotNull
            @Override
            public List<Type<?>> children() {
                return List.of(left, right);
            }
        };
    }

    /**
     * Creates a required field type that reads/writes a named field from a map structure.
     *
     * <p>Field types wrap another type to read from a specific field in a map/object.
     * The field must be present when reading; otherwise, parsing fails. Use {@link #optionalField(String, Type)} for
     * optional fields.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create a field type for "name" field
     * Type<String> nameField = Type.field("name", Type.STRING);
     *
     * // Read from JSON: {"name": "Alice"}
     * String name = nameField.read(dynamic).result().orElseThrow();  // "Alice"
     *
     * // Combine fields using products
     * Type<Pair<String, Integer>> personFields = Type.product(
     *     Type.field("name", Type.STRING),
     *     Type.field("age", Type.INT)
     * );
     * }</pre>
     *
     * @param name      the field name in the map/object, must not be {@code null}
     * @param fieldType the type of the field's value, must not be {@code null}
     * @param <A>       the Java type of the field's value
     * @return a new field type, never {@code null}
     * @throws NullPointerException if {@code name} or {@code fieldType} is {@code null}
     */
    @NotNull
    static <A> Type<A> field(@NotNull final String name,
                             @NotNull final Type<A> fieldType) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(fieldType, "fieldType must not be null");
        return new FieldType<>(name, fieldType, false);
    }

    /**
     * Creates an optional field type that may or may not be present in a map structure.
     *
     * <p>Optional field types wrap another type to read from a specific field that
     * may be absent. When the field is missing, the result is {@link Optional#empty()}. This is ideal for fields that
     * were added in later versions or have defaults.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create an optional field for "nickname"
     * Type<Optional<String>> nicknameField = Type.optionalField("nickname", Type.STRING);
     *
     * // Read from JSON: {"name": "Alice"}
     * Optional<String> nickname = nicknameField.read(dynamic).result().orElseThrow();
     * // nickname is Optional.empty() since field is missing
     *
     * // Read from JSON: {"name": "Alice", "nickname": "Ali"}
     * // nickname is Optional.of("Ali")
     *
     * // Useful for schema evolution
     * Type<Optional<Integer>> versionField = Type.optionalField("version", Type.INT);
     * }</pre>
     *
     * @param name      the field name in the map/object, must not be {@code null}
     * @param fieldType the type of the field's value when present, must not be {@code null}
     * @param <A>       the Java type of the field's value
     * @return a new optional field type, never {@code null}
     * @throws NullPointerException if {@code name} or {@code fieldType} is {@code null}
     */
    @NotNull
    static <A> Type<Optional<A>> optionalField(@NotNull final String name,
                                               @NotNull final Type<A> fieldType) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(fieldType, "fieldType must not be null");
        return new Type<>() {
            private final TypeReference ref = new TypeReference("?" + name + ":" + fieldType.reference().getId());

            @NotNull
            @Override
            public TypeReference reference() {
                return this.ref;
            }

            @NotNull
            @Override
            public Codec<Optional<A>> codec() {
                return fieldType.codec().optionalFieldOf(name).codec();
            }

            @NotNull
            @Override
            public String describe() {
                return "?" + name + ": " + fieldType.describe();
            }

            @NotNull
            @Override
            public List<Type<?>> children() {
                return List.of(fieldType);
            }
        };
    }

    /**
     * Creates a named alias for another type, useful for recursive definitions.
     *
     * <p>Named types give an alias to another type, primarily for enabling recursive
     * type definitions where a type needs to reference itself. The name becomes the type's reference ID while
     * delegating to the target type's codec.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define a recursive tree type
     * // The "TreeNode" name allows self-reference in the codec
     * Type<TreeNode> treeType = Type.named("TreeNode", nodeType);
     *
     * // Named types also improve describe() output
     * Type<Person> personType = Type.named("Person", complexPersonType);
     * System.out.println(personType.describe());  // "Person=..."
     * }</pre>
     *
     * @param name       the name for this type (becomes the reference ID), must not be {@code null}
     * @param targetType the type to delegate to, must not be {@code null}
     * @param <A>        the Java type
     * @return a new named type, never {@code null}
     * @throws NullPointerException if {@code name} or {@code targetType} is {@code null}
     */
    @NotNull
    static <A> Type<A> named(@NotNull final String name,
                             @NotNull final Type<A> targetType) {
        Preconditions.checkNotNull(name, "name must not be null");
        Preconditions.checkNotNull(targetType, "targetType must not be null");
        return new Type<>() {
            private final TypeReference ref = new TypeReference(name);

            @NotNull
            @Override
            public TypeReference reference() {
                return this.ref;
            }

            @NotNull
            @Override
            public Codec<A> codec() {
                return targetType.codec();
            }

            @NotNull
            @Override
            public String describe() {
                return name + "=" + targetType.describe();
            }

            @NotNull
            @Override
            public List<Type<?>> children() {
                return List.of(targetType);
            }
        };
    }

    /**
     * Creates a tagged choice type (discriminated union) with the specified tag field.
     *
     * <p>Tagged choice types represent polymorphic data where a discriminator field
     * determines the structure of the remaining data. This is the primary way to model inheritance, variants, or
     * "oneOf" patterns in serialized data.</p>
     *
     * <p>When reading, the codec first extracts the tag field value, looks up the
     * corresponding type from the choices map, and then parses the rest of the data using that type. When writing, the
     * tag value is included alongside the serialized content.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Define entity types with different structures
     * Type<Pair<String, Dynamic<?>>> entityType = Type.taggedChoice("type",
     *     Map.of(
     *         "player", Type.product(
     *             Type.field("name", Type.STRING),
     *             Type.field("level", Type.INT)
     *         ),
     *         "monster", Type.product(
     *             Type.field("species", Type.STRING),
     *             Type.field("hostile", Type.BOOL)
     *         ),
     *         "item", Type.field("itemId", Type.STRING)
     *     )
     * );
     *
     * // Reading JSON: {"type": "player", "name": "Alice", "level": 42}
     * DataResult<Pair<String, Dynamic<?>>> result = entityType.read(dynamic);
     * result.ifSuccess(pair -> {
     *     String tag = pair.first();        // "player"
     *     Dynamic<?> data = pair.second();  // Contains full object including type
     * });
     *
     * // Useful for version migration where entity types changed
     * TypeRewriteRule rule = Rules.transformField(
     *     entityType.reference(),
     *     "name",
     *     name -> name.toUpperCase()
     * );
     * }</pre>
     *
     * <h4>Common Use Cases</h4>
     * <ul>
     *   <li>Entity types (player, monster, NPC) with different fields</li>
     *   <li>Block or item variants with type-specific properties</li>
     *   <li>Event types with different payloads</li>
     *   <li>Polymorphic configuration entries</li>
     * </ul>
     *
     * @param tagField the name of the discriminator field (e.g., "type", "kind"), must not be {@code null}
     * @param choices  a map from tag values to their corresponding types, must not be {@code null} or empty
     * @return a new tagged choice type that decodes based on the tag field, never {@code null}
     * @throws NullPointerException if {@code tagField} or {@code choices} is {@code null}
     * @see TaggedChoiceType
     */
    @NotNull
    static Type<Pair<String, Dynamic<?>>> taggedChoice(@NotNull final String tagField,
                                                       @NotNull final Map<String, Type<?>> choices) {
        Preconditions.checkNotNull(tagField, "tagField must not be null");
        Preconditions.checkNotNull(choices, "choices must not be null");
        return new TaggedChoiceType(tagField, choices);
    }

    // ==================== Inner Classes ====================

    /**
     * A type that extracts a named field from a map/object structure.
     *
     * <p>{@code FieldType} wraps another type and configures it to read from or
     * write to a specific field in a map-like structure. This enables building complex record types by combining
     * multiple field types using products.</p>
     *
     * <h2>Purpose</h2>
     * <p>Field types bridge between positional type composition (products) and
     * named fields in serialized data. They translate field access operations into the appropriate map get/put
     * operations.</p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // Create a field type directly
     * Type.FieldType<String> nameField = new Type.FieldType<>("name", Type.STRING, false);
     *
     * // Access field metadata
     * String fieldName = nameField.name();           // "name"
     * Type<String> innerType = nameField.fieldType(); // Type.STRING
     * boolean isOpt = nameField.isOptional();        // false
     *
     * // Read from JSON: {"name": "Alice", "age": 30}
     * DataResult<String> name = nameField.read(dynamic);  // "Alice"
     *
     * // Typically used via factory methods
     * Type<String> nameType = Type.field("name", Type.STRING);
     * Type<Optional<String>> nickType = Type.optionalField("nickname", Type.STRING);
     * }</pre>
     *
     * <h2>Thread Safety</h2>
     * <p>This class is immutable and thread-safe.</p>
     *
     * @param <A> the Java type of the field's value
     * @see Type#field(String, Type)
     * @see Type#optionalField(String, Type)
     */
    final class FieldType<A> implements Type<A> {
        private final String name;
        private final Type<A> fieldType;
        private final boolean optional;
        private final TypeReference ref;

        /**
         * Creates a new field type with the specified configuration.
         *
         * <h4>Example</h4>
         * <pre>{@code
         * // Required field
         * FieldType<Integer> ageField = new FieldType<>("age", Type.INT, false);
         *
         * // Optional field
         * FieldType<String> bioField = new FieldType<>("bio", Type.STRING, true);
         * }</pre>
         *
         * @param name      the field name in the map/object, must not be {@code null}
         * @param fieldType the type of the field's value, must not be {@code null}
         * @param optional  {@code true} if the field is optional, {@code false} if required
         */
        FieldType(@NotNull final String name,
                  @NotNull final Type<A> fieldType,
                  final boolean optional) {
            Preconditions.checkNotNull(name, "name must not be null");
            Preconditions.checkNotNull(fieldType, "fieldType must not be null");
            this.name = name;
            this.fieldType = fieldType;
            this.optional = optional;
            this.ref = new TypeReference(
                    (optional ? "?" : "") + name + ":" + fieldType.reference().getId()
            );
        }

        /**
         * {@inheritDoc}
         */
        @NotNull
        @Override
        public TypeReference reference() {
            return this.ref;
        }

        /**
         * {@inheritDoc}
         */
        @NotNull
        @Override
        public Codec<A> codec() {
            return this.fieldType.codec().fieldOf(this.name).codec();
        }

        /**
         * {@inheritDoc}
         *
         * <p>The description format is {@code "name: type"} for required fields
         * or {@code "?name: type"} for optional fields.</p>
         */
        @NotNull
        @Override
        public String describe() {
            return (this.optional ? "?" : "") + this.name + ": " + this.fieldType.describe();
        }

        /**
         * Returns the field name used to extract data from map structures.
         *
         * <h4>Example</h4>
         * <pre>{@code
         * Type.FieldType<String> nameField = new Type.FieldType<>("name", Type.STRING, false);
         * String fieldName = nameField.name();  // "name"
         * }</pre>
         *
         * @return the field name, never {@code null}
         */
        public String name() {
            return this.name;
        }

        /**
         * Returns whether this field is optional.
         *
         * <p>Optional fields return {@link Optional#empty()} when the field is
         * missing from the input. Required fields produce an error on missing input.</p>
         *
         * <h4>Example</h4>
         * <pre>{@code
         * Type.FieldType<String> required = new Type.FieldType<>("name", Type.STRING, false);
         * Type.FieldType<String> optional = new Type.FieldType<>("bio", Type.STRING, true);
         *
         * required.isOptional();  // false
         * optional.isOptional();  // true
         * }</pre>
         *
         * @return {@code true} if this field is optional, {@code false} if required
         */
        public boolean isOptional() {
            return this.optional;
        }

        /**
         * Returns the underlying type of the field's value.
         *
         * <h4>Example</h4>
         * <pre>{@code
         * Type.FieldType<Integer> ageField = new Type.FieldType<>("age", Type.INT, false);
         * Type<Integer> innerType = ageField.fieldType();  // Type.INT
         *
         * // Use to access nested type information
         * TypeReference ref = ageField.fieldType().reference();
         * }</pre>
         *
         * @return the type of the field's value, never {@code null}
         */
        @SuppressFBWarnings(
                value = "EI_EXPOSE_REP",
                justification = "Type instances are immutable descriptors intended to be shared. Exposing the inner type is part of the API contract."
        )
        public Type<A> fieldType() {
            return this.fieldType;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Returns a single-element list containing the field's inner type.</p>
         */
        @NotNull
        @Override
        public List<Type<?>> children() {
            return List.of(this.fieldType);
        }
    }

    /**
     * A discriminated union type where a tag field determines the variant's structure.
     *
     * <p>{@code TaggedChoiceType} implements polymorphic serialization by using a
     * discriminator field to identify which variant the data represents. The tag value is looked up in a choices map to
     * determine the type to use for parsing the remaining data.</p>
     *
     * <h2>Serialization Format</h2>
     * <p>The serialized form includes the tag field alongside the variant's data:</p>
     * <pre>{@code
     * // For tag "player" with data {name: "Alice", level: 5}:
     * {"type": "player", "name": "Alice", "level": 5}
     *
     * // For tag "monster" with data {species: "dragon"}:
     * {"type": "monster", "species": "dragon"}
     * }</pre>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // Create a tagged choice type
     * TaggedChoiceType entityChoice = new TaggedChoiceType("type", Map.of(
     *     "player", Type.product(
     *         Type.field("name", Type.STRING),
     *         Type.field("level", Type.INT)
     *     ),
     *     "monster", Type.field("species", Type.STRING)
     * ));
     *
     * // Access metadata
     * String discriminator = entityChoice.tagField();        // "type"
     * Type<?> playerType = entityChoice.getChoice("player"); // player's type
     * Map<String, Type<?>> allTypes = entityChoice.choices(); // all variants
     *
     * // Parse polymorphic data
     * DataResult<Pair<String, Dynamic<?>>> result = entityChoice.read(dynamic);
     * result.ifSuccess(pair -> {
     *     String variant = pair.first();  // "player" or "monster"
     *     Dynamic<?> data = pair.second(); // The full parsed data
     * });
     * }</pre>
     *
     * <h2>Thread Safety</h2>
     * <p>This class is immutable and thread-safe. The choices map is defensively
     * copied during construction.</p>
     *
     * @see Type#taggedChoice(String, Map)
     */
    final class TaggedChoiceType implements Type<Pair<String, Dynamic<?>>> {
        private final String tagField;
        private final Map<String, Type<?>> choices;
        private final TypeReference ref;

        /**
         * Creates a new tagged choice type with the specified discriminator and variants.
         *
         * <h4>Example</h4>
         * <pre>{@code
         * TaggedChoiceType messageType = new TaggedChoiceType("kind", Map.of(
         *     "text", Type.field("content", Type.STRING),
         *     "image", Type.field("url", Type.STRING),
         *     "video", Type.product(
         *         Type.field("url", Type.STRING),
         *         Type.field("duration", Type.INT)
         *     )
         * ));
         * }</pre>
         *
         * @param tagField the name of the discriminator field, must not be {@code null}
         * @param choices  mapping from tag values to their corresponding types, must not be {@code null}; the map is
         *                 defensively copied
         */
        TaggedChoiceType(@NotNull final String tagField,
                         @NotNull final Map<String, Type<?>> choices) {
            Preconditions.checkNotNull(tagField, "tagField must not be null");
            Preconditions.checkNotNull(choices, "choices must not be null");
            this.tagField = tagField;
            this.choices = Map.copyOf(choices);
            this.ref = new TypeReference("TaggedChoice<" + tagField + ">");
        }

        /**
         * {@inheritDoc}
         */
        @NotNull
        @Override
        public TypeReference reference() {
            return this.ref;
        }

        /**
         * {@inheritDoc}
         *
         * <p>The codec reads the tag field to determine the variant, then parses
         * the remaining data accordingly. When encoding, it includes the tag field alongside the variant's data.</p>
         */
        @NotNull
        @Override
        public Codec<Pair<String, Dynamic<?>>> codec() {
            return new Codec<>() {
                @NotNull
                @Override
                public <T> DataResult<T> encode(@NotNull final Pair<String, Dynamic<?>> input,
                                                @NotNull final DynamicOps<T> ops,
                                                @NotNull final T prefix) {
                    Preconditions.checkNotNull(input, "input must not be null");
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(prefix, "prefix must not be null");
                    final String tag = input.first();
                    final Dynamic<?> value = input.second();

                    if (tag == null) {
                        return DataResult.error("Tag value is null");
                    }

                    if (value == null) {
                        return DataResult.error("Dynamic value is null");
                    }

                    final Type<?> type = choices.get(tag);
                    if (type == null) {
                        return DataResult.error("Unknown tag: " + tag);
                    }

                    // Create the output map with the tag and the value
                    T result = ops.createMap(Stream.of(
                            Pair.of(ops.createString(tagField), ops.createString(tag))
                    ));

                    // Merge the value's fields into the result
                    final T convertedValue = value.convert(ops).value();
                    result = ops.mergeToMap(result, convertedValue).result().orElse(result);

                    return DataResult.success(result);
                }

                @NotNull
                @Override
                public <T> DataResult<Pair<Pair<String, Dynamic<?>>, T>> decode(
                        @NotNull final DynamicOps<T> ops,
                        @NotNull final T input
                ) {
                    Preconditions.checkNotNull(ops, "ops must not be null");
                    Preconditions.checkNotNull(input, "input must not be null");
                    // Get the tag value
                    final T tagValue = ops.get(input, tagField);
                    if (tagValue == null) {
                        return DataResult.error("Missing tag field: " + tagField);
                    }

                    final DataResult<String> tagString = ops.getStringValue(tagValue);
                    if (tagString.isError()) {
                        return DataResult.error("Tag field is not a string");
                    }

                    final String tag = tagString.result().orElseThrow();
                    final Type<?> type = choices.get(tag);
                    if (type == null) {
                        return DataResult.error("Unknown tag value: " + tag);
                    }

                    // Create a Dynamic from the input (excluding the tag field)
                    final Dynamic<T> dynamic = new Dynamic<>(ops, input);

                    return DataResult.success(Pair.of(
                            Pair.of(tag, dynamic),
                            ops.empty()
                    ));
                }
            };
        }

        /**
         * {@inheritDoc}
         *
         * <p>The description shows the tag field and all available variants,
         * formatted as {@code "TaggedChoice<tagField>{variant1 -> type1, variant2 -> type2}"}.</p>
         *
         * <h4>Example Output</h4>
         * <pre>{@code
         * "TaggedChoice<type>{player -> (name: string × level: int), monster -> species: string}"
         * }</pre>
         */
        @NotNull
        @Override
        public String describe() {
            final String choicesStr = this.choices.entrySet().stream()
                    .map(e -> e.getKey() + " -> " + e.getValue().describe())
                    .collect(Collectors.joining(", "));
            return "TaggedChoice<" + this.tagField + ">{" + choicesStr + "}";
        }

        /**
         * Returns the name of the discriminator field used to identify variants.
         *
         * <p>This is the field name that the codec looks for when decoding data
         * to determine which variant type to use.</p>
         *
         * <h4>Example</h4>
         * <pre>{@code
         * TaggedChoiceType entityType = new TaggedChoiceType("entityType", Map.of(
         *     "player", Type.STRING,
         *     "npc", Type.INT
         * ));
         *
         * String discriminator = entityType.tagField();  // "entityType"
         * }</pre>
         *
         * @return the tag field name, never {@code null}
         */
        public String tagField() {
            return this.tagField;
        }

        /**
         * Returns an unmodifiable view of all variant mappings.
         *
         * <p>The returned map contains all tag values and their corresponding types.
         * This can be used to enumerate available variants or validate tag values.</p>
         *
         * <h4>Example</h4>
         * <pre>{@code
         * TaggedChoiceType messageType = new TaggedChoiceType("kind", Map.of(
         *     "text", Type.STRING,
         *     "image", Type.STRING,
         *     "audio", Type.STRING
         * ));
         *
         * Map<String, Type<?>> variants = messageType.choices();
         * System.out.println(variants.keySet());  // [text, image, audio]
         *
         * // Check if a variant exists
         * boolean hasVideo = variants.containsKey("video");  // false
         * }</pre>
         *
         * @return an unmodifiable map of tag values to types, never {@code null}
         */
        public Map<String, Type<?>> choices() {
            return this.choices;
        }

        /**
         * Looks up the type for a specific tag value.
         *
         * <p>This is useful when you need to access a specific variant's type
         * for custom processing or validation.</p>
         *
         * <h4>Example</h4>
         * <pre>{@code
         * TaggedChoiceType entityType = new TaggedChoiceType("type", Map.of(
         *     "player", Type.product(
         *         Type.field("name", Type.STRING),
         *         Type.field("level", Type.INT)
         *     ),
         *     "item", Type.field("itemId", Type.STRING)
         * ));
         *
         * Type<?> playerType = entityType.getChoice("player");  // the player product type
         * Type<?> unknownType = entityType.getChoice("unknown"); // null
         *
         * if (playerType != null) {
         *     System.out.println(playerType.describe());
         * }
         * }</pre>
         *
         * @param tag the tag value to look up, must not be {@code null}
         * @return the type for the specified tag, or {@code null} if no such variant exists
         * @throws NullPointerException if {@code tag} is {@code null}
         */
        @Nullable
        public Type<?> getChoice(@NotNull final String tag) {
            Preconditions.checkNotNull(tag, "tag must not be null");
            return this.choices.get(tag);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Returns all variant types from the choices map.</p>
         */
        @NotNull
        @Override
        public List<Type<?>> children() {
            return List.copyOf(this.choices.values());
        }
    }
}
