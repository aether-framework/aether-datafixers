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

package de.splatgames.aether.datafixers.api.dynamic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * An immutable wrapper pairing a value with its associated {@link DynamicOps}.
 *
 * <p>{@code Dynamic} provides a convenient, fluent API for working with dynamic
 * data structures. It encapsulates both the underlying value and the operations needed to manipulate it, enabling
 * chained operations without repeatedly passing the ops instance.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Type Checking:</b> {@link #isMap()}, {@link #isList()}, {@link #isString()}, etc.</li>
 *   <li><b>Value Reading:</b> {@link #asString()}, {@link #asInt()}, {@link #asBoolean()}, etc.</li>
 *   <li><b>Map Operations:</b> {@link #get(String)}, {@link #set(String, Dynamic)},
 *       {@link #update(String, Function)}, etc.</li>
 *   <li><b>List Operations:</b> {@link #asListStream()}, {@link #createList(Stream)}</li>
 *   <li><b>Value Creation:</b> {@link #createString(String)}, {@link #createInt(int)}, etc.</li>
 *   <li><b>Format Conversion:</b> {@link #convert(DynamicOps)}</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a Dynamic from a JSON element
 * Dynamic<JsonElement> dynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
 *
 * // Read values
 * String name = dynamic.get("name").asString().result().orElse("unknown");
 * int age = dynamic.get("age").asInt().result().orElse(0);
 *
 * // Modify values (returns new Dynamic instances)
 * Dynamic<JsonElement> updated = dynamic
 *     .set("name", dynamic.createString("Alice"))
 *     .set("age", dynamic.createInt(30));
 *
 * // Convert to a different format
 * Dynamic<JsonNode> jackson = dynamic.convert(JacksonOps.INSTANCE);
 * }</pre>
 *
 * <h2>Immutability</h2>
 * <p>{@code Dynamic} is immutable. All modification operations return new
 * {@code Dynamic} instances rather than modifying the original.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>{@code Dynamic} is thread-safe if the underlying value type and
 * {@link DynamicOps} implementation are thread-safe.</p>
 *
 * @param ops   the dynamic operations for the value's format, must not be {@code null}
 * @param value the underlying value, must not be {@code null}
 * @param <T>   the underlying value representation (e.g., {@code JsonElement})
 * @author Erik Pförtner
 * @see DynamicOps
 * @see TaggedDynamic
 * @since 0.1.0
 */
@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP"},
        justification = "DynamicOps is a stateless strategy object that is intentionally stored and exposed as part of Dynamic's public API."
)
public record Dynamic<T>(@NotNull DynamicOps<T> ops, @NotNull T value) {

    /**
     * Creates a new Dynamic with validation.
     *
     * @throws NullPointerException if {@code ops} or {@code value} is {@code null}
     */
    public Dynamic {
        Preconditions.checkNotNull(ops, "ops must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
    }

    // ==================== Type Checks ====================

    /**
     * Checks if this value represents a map or object structure.
     *
     * <p>A map is a collection of key-value pairs where keys are strings. In JSON terms,
     * this corresponds to an object ({@code {...}}). Use this method to verify the structure before calling map-related
     * operations like {@link #get(String)} or {@link #set(String, Dynamic)}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     * if (dynamic.isMap()) {
     *     String name = dynamic.get("name").asString().result().orElse("unknown");
     * }
     * }</pre>
     *
     * @return {@code true} if this value is a map/object structure; {@code false} otherwise
     * @see #isList()
     * @see #get(String)
     */
    public boolean isMap() {
        return this.ops.isMap(this.value);
    }

    /**
     * Checks if this value represents a list or array structure.
     *
     * <p>A list is an ordered collection of elements. In JSON terms, this corresponds to
     * an array ({@code [...]}). Use this method to verify the structure before calling list-related operations like
     * {@link #asListStream()}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     * if (dynamic.isList()) {
     *     dynamic.asListStream().result().ifPresent(stream ->
     *         stream.forEach(element -> System.out.println(element))
     *     );
     * }
     * }</pre>
     *
     * @return {@code true} if this value is a list/array structure; {@code false} otherwise
     * @see #isMap()
     * @see #asListStream()
     */
    public boolean isList() {
        return this.ops.isList(this.value);
    }

    /**
     * Checks if this value represents a string primitive.
     *
     * <p>Use this method to verify that the value is a string before attempting to read it
     * with {@link #asString()}. While {@code asString()} returns a {@link DataResult} that handles type mismatches
     * gracefully, this method allows for pre-emptive type checking.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     * if (dynamic.isString()) {
     *     String value = dynamic.asString().result().orElseThrow();
     * }
     * }</pre>
     *
     * @return {@code true} if this value is a string; {@code false} otherwise
     * @see #isNumber()
     * @see #asString()
     */
    public boolean isString() {
        return this.ops.isString(this.value);
    }

    /**
     * Checks if this value represents a numeric primitive.
     *
     * <p>Numbers include integers, floats, doubles, longs, and other numeric types.
     * Use this method to verify that the value is numeric before attempting to read it with methods like
     * {@link #asInt()}, {@link #asLong()}, or {@link #asDouble()}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     * if (dynamic.isNumber()) {
     *     int intValue = dynamic.asInt().result().orElseThrow();
     *     double doubleValue = dynamic.asDouble().result().orElseThrow();
     * }
     * }</pre>
     *
     * @return {@code true} if this value is a number; {@code false} otherwise
     * @see #isString()
     * @see #asNumber()
     */
    public boolean isNumber() {
        return this.ops.isNumber(this.value);
    }

    /**
     * Checks if this value represents a boolean primitive.
     *
     * <p>Use this method to verify that the value is a boolean before attempting to read it
     * with {@link #asBoolean()}. Boolean values represent true/false states.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     * if (dynamic.isBoolean()) {
     *     boolean enabled = dynamic.asBoolean().result().orElse(false);
     * }
     * }</pre>
     *
     * @return {@code true} if this value is a boolean; {@code false} otherwise
     * @see #asBoolean()
     */
    public boolean isBoolean() {
        return this.ops.isBoolean(this.value);
    }

    // ==================== Primitive Reading ====================

    /**
     * Attempts to read this value as a string.
     *
     * <p>If this value is not a string, the returned {@link DataResult} will contain an error
     * message explaining the type mismatch. Use {@link DataResult#result()} to safely extract the value as an
     * {@link Optional}, or {@link DataResult#getOrThrow()} to throw on failure.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Safe extraction with default
     * String name = dynamic.get("name").asString().result().orElse("unknown");
     *
     * // Throwing extraction
     * String required = dynamic.get("id").asString().getOrThrow();
     * }</pre>
     *
     * @return a {@link DataResult} containing the string value on success, or an error describing why the value could
     * not be read as a string
     * @see #isString()
     * @see DataResult#result()
     */
    @NotNull
    public DataResult<String> asString() {
        return this.ops.getStringValue(this.value);
    }

    /**
     * Attempts to read this value as a generic {@link Number}.
     *
     * <p>This method returns the underlying numeric value without converting to a specific
     * type. Use this when you need to inspect the number type or perform type-specific operations. For type-specific
     * extraction, use {@link #asInt()}, {@link #asLong()}, etc.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     * DataResult<Number> result = dynamic.asNumber();
     *
     * result.result().ifPresent(num -> {
     *     if (num instanceof Integer) {
     *         System.out.println("Integer: " + num.intValue());
     *     } else if (num instanceof Double) {
     *         System.out.println("Double: " + num.doubleValue());
     *     }
     * });
     * }</pre>
     *
     * @return a {@link DataResult} containing the number value on success, or an error describing why the value could
     * not be read as a number
     * @see #isNumber()
     * @see #asInt()
     * @see #asDouble()
     */
    @NotNull
    public DataResult<Number> asNumber() {
        return this.ops.getNumberValue(this.value);
    }

    /**
     * Attempts to read this value as a 32-bit integer.
     *
     * <p>If the underlying value is a number of a different type (e.g., double, long), it
     * will be converted to an integer, potentially with loss of precision or overflow. If the value is not numeric at
     * all, the result will contain an error.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * int count = dynamic.get("count").asInt().result().orElse(0);
     * int level = dynamic.get("level").asInt().getOrThrow();
     * }</pre>
     *
     * @return a {@link DataResult} containing the integer value on success, or an error describing why the value could
     * not be read as a number
     * @see #asLong()
     * @see #asNumber()
     */
    @NotNull
    public DataResult<Integer> asInt() {
        return this.ops.getNumberValue(this.value).map(Number::intValue);
    }

    /**
     * Attempts to read this value as a 64-bit long integer.
     *
     * <p>Long values are suitable for large numbers, timestamps, or IDs that exceed
     * the 32-bit integer range. If the underlying value is a different numeric type, it will be converted.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * long timestamp = dynamic.get("timestamp").asLong().result().orElse(0L);
     * long userId = dynamic.get("userId").asLong().getOrThrow();
     * }</pre>
     *
     * @return a {@link DataResult} containing the long value on success, or an error describing why the value could not
     * be read as a number
     * @see #asInt()
     */
    @NotNull
    public DataResult<Long> asLong() {
        return this.ops.getNumberValue(this.value).map(Number::longValue);
    }

    /**
     * Attempts to read this value as a 32-bit floating-point number.
     *
     * <p>Float values provide decimal precision with less memory than doubles. If the
     * underlying value is a different numeric type, it will be converted, potentially with loss of precision.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * float alpha = dynamic.get("alpha").asFloat().result().orElse(1.0f);
     * }</pre>
     *
     * @return a {@link DataResult} containing the float value on success, or an error describing why the value could
     * not be read as a number
     * @see #asDouble()
     */
    @NotNull
    public DataResult<Float> asFloat() {
        return this.ops.getNumberValue(this.value).map(Number::floatValue);
    }

    /**
     * Attempts to read this value as a 64-bit double-precision floating-point number.
     *
     * <p>Double values provide high precision for decimal numbers and are the default
     * floating-point type in most JSON parsers. Use this for coordinates, percentages, or any value requiring decimal
     * precision.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * double x = dynamic.get("x").asDouble().result().orElse(0.0);
     * double y = dynamic.get("y").asDouble().result().orElse(0.0);
     * double z = dynamic.get("z").asDouble().result().orElse(0.0);
     * }</pre>
     *
     * @return a {@link DataResult} containing the double value on success, or an error describing why the value could
     * not be read as a number
     * @see #asFloat()
     */
    @NotNull
    public DataResult<Double> asDouble() {
        return this.ops.getNumberValue(this.value).map(Number::doubleValue);
    }

    /**
     * Attempts to read this value as an 8-bit byte.
     *
     * <p>Byte values are useful for small integers, flags, or compact binary data.
     * If the underlying value exceeds the byte range ({@code -128} to {@code 127}), the value will overflow according
     * to Java's byte conversion rules.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * byte flags = dynamic.get("flags").asByte().result().orElse((byte) 0);
     * }</pre>
     *
     * @return a {@link DataResult} containing the byte value on success, or an error describing why the value could not
     * be read as a number
     * @see #asShort()
     * @see #asInt()
     */
    @NotNull
    public DataResult<Byte> asByte() {
        return this.ops.getNumberValue(this.value).map(Number::byteValue);
    }

    /**
     * Attempts to read this value as a 16-bit short integer.
     *
     * <p>Short values provide a middle ground between bytes and integers, useful for
     * moderately sized numbers or when memory efficiency is important.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * short port = dynamic.get("port").asShort().result().orElse((short) 8080);
     * }</pre>
     *
     * @return a {@link DataResult} containing the short value on success, or an error describing why the value could
     * not be read as a number
     * @see #asByte()
     * @see #asInt()
     */
    @NotNull
    public DataResult<Short> asShort() {
        return this.ops.getNumberValue(this.value).map(Number::shortValue);
    }

    /**
     * Attempts to read this value as a boolean.
     *
     * <p>Boolean values represent true/false states. The exact interpretation depends
     * on the {@link DynamicOps} implementation - some may convert numbers (0/1) or strings ("true"/"false") to
     * booleans.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * boolean enabled = dynamic.get("enabled").asBoolean().result().orElse(false);
     * boolean visible = dynamic.get("visible").asBoolean().getOrThrow();
     * }</pre>
     *
     * @return a {@link DataResult} containing the boolean value on success, or an error describing why the value could
     * not be read as a boolean
     * @see #isBoolean()
     */
    @NotNull
    public DataResult<Boolean> asBoolean() {
        return this.ops.getBooleanValue(this.value);
    }

    // ==================== List Operations ====================

    /**
     * Attempts to read this value as a stream of {@link Dynamic} elements.
     *
     * <p>If this value is a list/array, the method returns a stream where each element is
     * wrapped in its own {@link Dynamic} instance, preserving the ops reference. This enables fluent chaining for
     * processing list elements.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Process all elements
     * dynamic.get("items").asListStream().result().ifPresent(stream ->
     *     stream.forEach(item -> {
     *         String name = item.get("name").asString().result().orElse("unknown");
     *         System.out.println("Item: " + name);
     *     })
     * );
     *
     * // Collect to list
     * List<String> names = dynamic.get("tags").asListStream()
     *     .result()
     *     .map(stream -> stream
     *         .flatMap(d -> d.asString().result().stream())
     *         .collect(Collectors.toList())
     *     )
     *     .orElse(List.of());
     * }</pre>
     *
     * @return a {@link DataResult} containing a stream of Dynamic elements on success, or an error if this value is not
     * a list
     * @see #isList()
     * @see #createList(Stream)
     */
    @NotNull
    public DataResult<Stream<Dynamic<T>>> asListStream() {
        return this.ops.getList(this.value).map(s -> s.map(v -> new Dynamic<>(this.ops, v)));
    }

    /**
     * Creates a new list {@link Dynamic} from a stream of Dynamic values.
     *
     * <p>This method is used to construct list values programmatically. The resulting
     * Dynamic contains a list representation in the underlying format (e.g., a JSON array). All input Dynamic values
     * must use the same {@link DynamicOps} as this instance.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Create a list of strings
     * Dynamic<JsonElement> tagList = dynamic.createList(
     *     Stream.of("tag1", "tag2", "tag3")
     *           .map(dynamic::createString)
     * );
     *
     * // Create a list of complex objects
     * Dynamic<JsonElement> items = dynamic.createList(
     *     Stream.of(
     *         dynamic.emptyMap().set("name", dynamic.createString("sword")).set("damage", dynamic.createInt(10)),
     *         dynamic.emptyMap().set("name", dynamic.createString("shield")).set("defense", dynamic.createInt(5))
     *     )
     * );
     * }</pre>
     *
     * @param values the stream of Dynamic values to include in the list; must not be {@code null}
     * @return a new Dynamic containing a list of the provided values; never {@code null}
     * @throws NullPointerException if {@code values} is {@code null}
     * @see #asListStream()
     * @see #emptyList()
     */
    @NotNull
    public Dynamic<T> createList(@NotNull final Stream<Dynamic<T>> values) {
        Preconditions.checkNotNull(values, "values must not be null");
        return new Dynamic<>(this.ops, this.ops.createList(values.map(Dynamic::value)));
    }

    // ==================== Map Operations ====================

    /**
     * Checks if a key exists in this map/object structure.
     *
     * <p>This method performs an existence check without retrieving the value. Use this
     * to conditionally process fields or to validate structure before accessing values. If this value is not a map, the
     * behavior depends on the {@link DynamicOps} implementation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * if (dynamic.has("optionalField")) {
     *     String value = dynamic.get("optionalField").asString().result().orElseThrow();
     *     // Process the optional field
     * }
     * }</pre>
     *
     * @param key the field name to check for existence; must not be {@code null}
     * @return {@code true} if the key exists in this map; {@code false} otherwise
     * @throws NullPointerException if {@code key} is {@code null}
     * @see #get(String)
     * @see #getOptional(String)
     */
    public boolean has(@NotNull final String key) {
        Preconditions.checkNotNull(key, "key must not be null");
        return this.ops.has(this.value, key);
    }

    /**
     * Retrieves a value from this map by its key.
     *
     * <p>If the key exists, returns a new {@link Dynamic} wrapping the child value.
     * If the key does not exist or this value is not a map, returns {@code null}. For a null-safe alternative, use
     * {@link #getOptional(String)} or {@link #getOrEmpty(String)}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Direct access (may be null)
     * Dynamic<JsonElement> name = dynamic.get("name");
     * if (name != null) {
     *     String value = name.asString().result().orElse("unknown");
     * }
     *
     * // Chained access for nested fields
     * String city = dynamic.get("address").get("city").asString().result().orElse("unknown");
     * }</pre>
     *
     * @param key the field name to retrieve; must not be {@code null}
     * @return a Dynamic containing the field value, or {@code null} if the key does not exist
     * @throws NullPointerException if {@code key} is {@code null}
     * @see #getOptional(String)
     * @see #getOrEmpty(String)
     * @see #has(String)
     */
    @Nullable
    public Dynamic<T> get(@NotNull final String key) {
        Preconditions.checkNotNull(key, "key must not be null");
        final T child = this.ops.get(this.value, key);
        return child == null ? null : new Dynamic<>(this.ops, child);
    }

    /**
     * Retrieves a value from this map by key, returning an empty map if not found.
     *
     * <p>This method provides a null-safe way to access nested structures. If the key
     * does not exist, an empty map Dynamic is returned, allowing continued chaining without null checks. This is useful
     * for accessing deeply nested optional fields.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Safe nested access - never returns null
     * String city = dynamic.getOrEmpty("address")
     *                      .getOrEmpty("city")
     *                      .asString()
     *                      .result()
     *                      .orElse("unknown");
     * }</pre>
     *
     * @param key the field name to retrieve; must not be {@code null}
     * @return a Dynamic containing the field value, or an empty map Dynamic if the key does not exist; never
     * {@code null}
     * @see #get(String)
     * @see #getOptional(String)
     * @see #emptyMap()
     */
    @NotNull
    public Dynamic<T> getOrEmpty(@NotNull final String key) {
        final Dynamic<T> child = this.get(key);
        return child != null ? child : this.emptyMap();
    }

    /**
     * Retrieves a value from this map as an {@link Optional}.
     *
     * <p>This method wraps the result of {@link #get(String)} in an Optional, providing
     * a functional API for handling missing values. This is the preferred approach when working with functional-style
     * code or Optional-based APIs.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Functional style
     * dynamic.getOptional("email")
     *        .flatMap(d -> d.asString().result())
     *        .ifPresent(email -> System.out.println("Email: " + email));
     *
     * // With default value
     * String name = dynamic.getOptional("name")
     *                      .flatMap(d -> d.asString().result())
     *                      .orElse("anonymous");
     * }</pre>
     *
     * @param key the field name to retrieve; must not be {@code null}
     * @return an Optional containing the Dynamic if the key exists, or empty if not
     * @see #get(String)
     * @see #getOrEmpty(String)
     */
    @NotNull
    public Optional<Dynamic<T>> getOptional(@NotNull final String key) {
        return Optional.ofNullable(this.get(key));
    }

    /**
     * Sets a value in this map, returning a new Dynamic with the updated structure.
     *
     * <p>This operation is immutable - it creates and returns a new Dynamic rather than
     * modifying this instance. If the key already exists, its value is replaced. If this value is not a map, the
     * behavior depends on the {@link DynamicOps} implementation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Set a single field
     * Dynamic<JsonElement> updated = dynamic.set("name", dynamic.createString("Alice"));
     *
     * // Chain multiple updates (fluent style)
     * Dynamic<JsonElement> person = dynamic.emptyMap()
     *     .set("name", dynamic.createString("Alice"))
     *     .set("age", dynamic.createInt(30))
     *     .set("email", dynamic.createString("alice@example.com"));
     * }</pre>
     *
     * @param key      the field name to set; must not be {@code null}
     * @param newValue the value to set; must not be {@code null} and must use the same ops
     * @return a new Dynamic with the updated field; never {@code null}
     * @throws NullPointerException     if {@code key} or {@code newValue} is {@code null}
     * @throws IllegalArgumentException if {@code newValue} uses different DynamicOps
     * @see #remove(String)
     * @see #update(String, Function)
     */
    @NotNull
    public Dynamic<T> set(@NotNull final String key,
                          @NotNull final Dynamic<T> newValue) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(newValue, "newValue must not be null");
        Preconditions.checkArgument(this.ops == newValue.ops, "DynamicOps mismatch");

        final T updated = this.ops.set(this.value, key, newValue.value);
        return new Dynamic<>(this.ops, updated);
    }

    /**
     * Removes a key from this map, returning a new Dynamic without that field.
     *
     * <p>This operation is immutable - it creates and returns a new Dynamic rather than
     * modifying this instance. If the key does not exist, the returned Dynamic is effectively unchanged (but may be a
     * new instance depending on the ops implementation).</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Remove a single field
     * Dynamic<JsonElement> withoutPassword = dynamic.remove("password");
     *
     * // Remove multiple fields
     * Dynamic<JsonElement> sanitized = dynamic
     *     .remove("password")
     *     .remove("internalId")
     *     .remove("secretToken");
     * }</pre>
     *
     * @param key the field name to remove; must not be {@code null}
     * @return a new Dynamic without the specified field; never {@code null}
     * @throws NullPointerException if {@code key} is {@code null}
     * @see #set(String, Dynamic)
     */
    @NotNull
    public Dynamic<T> remove(@NotNull final String key) {
        Preconditions.checkNotNull(key, "key must not be null");
        final T updated = this.ops.remove(this.value, key);
        return new Dynamic<>(this.ops, updated);
    }

    /**
     * Updates a field in this map using the provided transformation function.
     *
     * <p>This method retrieves the current value at the specified key, applies the updater
     * function, and sets the result back. If the key does not exist, the original Dynamic is returned unchanged. This
     * is a convenience method that combines {@link #get(String)} and {@link #set(String, Dynamic)}.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Increment a counter
     * Dynamic<JsonElement> updated = dynamic.update("count", count ->
     *     count.createInt(count.asInt().result().orElse(0) + 1)
     * );
     *
     * // Transform a nested structure
     * Dynamic<JsonElement> normalized = dynamic.update("name", name ->
     *     name.createString(name.asString().result().orElse("").toLowerCase())
     * );
     * }</pre>
     *
     * @param key     the field name to update; must not be {@code null}
     * @param updater the function to transform the current value; must not be {@code null}
     * @return a new Dynamic with the updated field, or this Dynamic if the key does not exist
     * @throws NullPointerException if {@code key} or {@code updater} is {@code null}
     * @see #set(String, Dynamic)
     * @see #get(String)
     */
    @NotNull
    public Dynamic<T> update(@NotNull final String key,
                             @NotNull final Function<Dynamic<T>, Dynamic<T>> updater) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(updater, "updater must not be null");

        final Dynamic<T> current = this.get(key);
        if (current == null) {
            return this;
        }
        final Dynamic<T> updated = updater.apply(current);
        return this.set(key, updated);
    }

    /**
     * Reads this map as a stream of key-value pairs.
     *
     * <p>Each entry in the stream is a {@link Pair} where both the key and value are
     * wrapped as {@link Dynamic} instances. This enables processing map entries while maintaining type safety and ops
     * consistency.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Process all entries
     * dynamic.asMapStream().result().ifPresent(stream ->
     *     stream.forEach(entry -> {
     *         String key = entry.first().asString().result().orElse("?");
     *         String value = entry.second().asString().result().orElse("?");
     *         System.out.println(key + " = " + value);
     *     })
     * );
     *
     * // Collect to Java Map
     * Map<String, Dynamic<JsonElement>> map = dynamic.asMapStream()
     *     .result()
     *     .map(stream -> stream.collect(Collectors.toMap(
     *         p -> p.first().asString().result().orElse(""),
     *         Pair::second
     *     )))
     *     .orElse(Map.of());
     * }</pre>
     *
     * @return a {@link DataResult} containing a stream of key-value pairs on success, or an error if this value is not
     * a map
     * @see #isMap()
     * @see Pair
     */
    @NotNull
    public DataResult<Stream<Pair<Dynamic<T>, Dynamic<T>>>> asMapStream() {
        return this.ops.getMapEntries(this.value)
                .map(s -> s.map(p -> Pair.of(
                        new Dynamic<>(this.ops, Objects.requireNonNull(p.first(), "p.first() must not be null")),
                        new Dynamic<>(this.ops, Objects.requireNonNull(p.second(), "p.second() must not be null"))
                )));
    }

    // ==================== Creation Helpers ====================

    /**
     * Creates an empty Dynamic using the default empty representation for this ops.
     *
     * <p>The exact representation of "empty" depends on the {@link DynamicOps} implementation.
     * Typically, this returns an empty map/object, but some implementations may use a different default. Use
     * {@link #emptyMap()} or {@link #emptyList()} for specific types.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     * Dynamic<JsonElement> emptyValue = dynamic.empty();
     * }</pre>
     *
     * @return a Dynamic containing the ops-specific empty representation; never {@code null}
     * @see #emptyMap()
     * @see #emptyList()
     */
    @NotNull
    public Dynamic<T> empty() {
        return new Dynamic<>(this.ops, this.ops.empty());
    }

    /**
     * Creates a Dynamic containing an empty map/object structure.
     *
     * <p>This method is useful as a starting point for building map structures using
     * the fluent {@link #set(String, Dynamic)} method.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Build an object from scratch
     * Dynamic<JsonElement> person = dynamic.emptyMap()
     *     .set("name", dynamic.createString("Alice"))
     *     .set("age", dynamic.createInt(30));
     * // Results in: {"name": "Alice", "age": 30}
     * }</pre>
     *
     * @return a Dynamic containing an empty map; never {@code null}
     * @see #emptyList()
     * @see #set(String, Dynamic)
     */
    @NotNull
    public Dynamic<T> emptyMap() {
        return new Dynamic<>(this.ops, this.ops.emptyMap());
    }

    /**
     * Creates a Dynamic containing an empty list/array structure.
     *
     * <p>This method returns a Dynamic representing an empty list, which can be used
     * as a starting point or as a default value for optional list fields.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Use as default for optional list
     * Dynamic<JsonElement> tags = dynamic.getOptional("tags")
     *     .orElseGet(dynamic::emptyList);
     *
     * // Build a list using createList
     * Dynamic<JsonElement> items = dynamic.createList(Stream.of(
     *     dynamic.createString("item1"),
     *     dynamic.createString("item2")
     * ));
     * }</pre>
     *
     * @return a Dynamic containing an empty list; never {@code null}
     * @see #emptyMap()
     * @see #createList(Stream)
     */
    @NotNull
    public Dynamic<T> emptyList() {
        return new Dynamic<>(this.ops, this.ops.emptyList());
    }

    /**
     * Creates a Dynamic containing a string value.
     *
     * <p>This factory method creates a new Dynamic with the same ops as this instance,
     * wrapping the provided string value in the underlying format representation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * Dynamic<JsonElement> name = dynamic.createString("Alice");
     * Dynamic<JsonElement> updated = dynamic.set("name", dynamic.createString("Bob"));
     * }</pre>
     *
     * @param value the string value to wrap; must not be {@code null}
     * @return a new Dynamic containing the string value; never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     * @see #asString()
     */
    @NotNull
    public Dynamic<T> createString(@NotNull final String value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return new Dynamic<>(this.ops, this.ops.createString(value));
    }

    /**
     * Creates a Dynamic containing a 32-bit integer value.
     *
     * <p>This factory method creates a new Dynamic wrapping the provided integer
     * in the underlying format representation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * Dynamic<JsonElement> count = dynamic.createInt(42);
     * Dynamic<JsonElement> updated = dynamic.set("count", dynamic.createInt(count.asInt().result().orElse(0) + 1));
     * }</pre>
     *
     * @param value the integer value to wrap
     * @return a new Dynamic containing the integer value; never {@code null}
     * @see #asInt()
     * @see #createLong(long)
     */
    @NotNull
    public Dynamic<T> createInt(final int value) {
        return new Dynamic<>(this.ops, this.ops.createInt(value));
    }

    /**
     * Creates a Dynamic containing a 64-bit long integer value.
     *
     * <p>Use this method for large integers that exceed the 32-bit integer range,
     * such as timestamps, large IDs, or counters.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * Dynamic<JsonElement> timestamp = dynamic.createLong(System.currentTimeMillis());
     * Dynamic<JsonElement> bigId = dynamic.createLong(9223372036854775807L);
     * }</pre>
     *
     * @param value the long value to wrap
     * @return a new Dynamic containing the long value; never {@code null}
     * @see #asLong()
     * @see #createInt(int)
     */
    @NotNull
    public Dynamic<T> createLong(final long value) {
        return new Dynamic<>(this.ops, this.ops.createLong(value));
    }

    /**
     * Creates a Dynamic containing a 32-bit floating-point value.
     *
     * <p>Use this method for decimal numbers where memory efficiency is prioritized
     * over precision.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * Dynamic<JsonElement> alpha = dynamic.createFloat(0.5f);
     * Dynamic<JsonElement> ratio = dynamic.createFloat(0.75f);
     * }</pre>
     *
     * @param value the float value to wrap
     * @return a new Dynamic containing the float value; never {@code null}
     * @see #asFloat()
     * @see #createDouble(double)
     */
    @NotNull
    public Dynamic<T> createFloat(final float value) {
        return new Dynamic<>(this.ops, this.ops.createFloat(value));
    }

    /**
     * Creates a Dynamic containing a 64-bit double-precision floating-point value.
     *
     * <p>Use this method for decimal numbers requiring high precision, such as
     * coordinates, percentages, or scientific values.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * Dynamic<JsonElement> position = dynamic.emptyMap()
     *     .set("x", dynamic.createDouble(123.456789))
     *     .set("y", dynamic.createDouble(-78.123456))
     *     .set("z", dynamic.createDouble(0.000001));
     * }</pre>
     *
     * @param value the double value to wrap
     * @return a new Dynamic containing the double value; never {@code null}
     * @see #asDouble()
     * @see #createFloat(float)
     */
    @NotNull
    public Dynamic<T> createDouble(final double value) {
        return new Dynamic<>(this.ops, this.ops.createDouble(value));
    }

    /**
     * Creates a Dynamic containing an 8-bit byte value.
     *
     * <p>Use this method for small integers, flags, or compact binary data.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * Dynamic<JsonElement> flags = dynamic.createByte((byte) 0x0F);
     * Dynamic<JsonElement> priority = dynamic.createByte((byte) 5);
     * }</pre>
     *
     * @param value the byte value to wrap
     * @return a new Dynamic containing the byte value; never {@code null}
     * @see #asByte()
     * @see #createShort(short)
     */
    @NotNull
    public Dynamic<T> createByte(final byte value) {
        return new Dynamic<>(this.ops, this.ops.createByte(value));
    }

    /**
     * Creates a Dynamic containing a 16-bit short integer value.
     *
     * <p>Use this method for medium-sized integers or when memory efficiency
     * is important.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * Dynamic<JsonElement> port = dynamic.createShort((short) 8080);
     * Dynamic<JsonElement> maxCount = dynamic.createShort((short) 1000);
     * }</pre>
     *
     * @param value the short value to wrap
     * @return a new Dynamic containing the short value; never {@code null}
     * @see #asShort()
     * @see #createByte(byte)
     * @see #createInt(int)
     */
    @NotNull
    public Dynamic<T> createShort(final short value) {
        return new Dynamic<>(this.ops, this.ops.createShort(value));
    }

    /**
     * Creates a Dynamic containing a boolean value.
     *
     * <p>Use this method for true/false flags and toggle states.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * Dynamic<JsonElement> settings = dynamic.emptyMap()
     *     .set("enabled", dynamic.createBoolean(true))
     *     .set("visible", dynamic.createBoolean(false))
     *     .set("debug", dynamic.createBoolean(true));
     * }</pre>
     *
     * @param value the boolean value to wrap
     * @return a new Dynamic containing the boolean value; never {@code null}
     * @see #asBoolean()
     */
    @NotNull
    public Dynamic<T> createBoolean(final boolean value) {
        return new Dynamic<>(this.ops, this.ops.createBoolean(value));
    }

    // ==================== Conversion ====================

    /**
     * Converts this Dynamic to a different data format representation.
     *
     * <p>This method enables converting data between different serialization formats
     * (e.g., from Gson JSON to Jackson JSON, or from JSON to YAML). The conversion preserves the logical structure
     * while transforming the underlying representation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Convert from Gson to Jackson
     * Dynamic<JsonElement> gsonDynamic = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     * Dynamic<JsonNode> jacksonDynamic = gsonDynamic.convert(JacksonOps.INSTANCE);
     *
     * // Convert from JSON to YAML
     * Dynamic<JsonElement> jsonDynamic = ...;
     * Dynamic<Object> yamlDynamic = jsonDynamic.convert(SnakeYamlOps.INSTANCE);
     * }</pre>
     *
     * @param <U>       the target format's value type
     * @param targetOps the DynamicOps for the target format; must not be {@code null}
     * @return a new Dynamic in the target format representation; never {@code null}
     * @throws NullPointerException if {@code targetOps} is {@code null}
     */
    @NotNull
    public <U> Dynamic<U> convert(@NotNull final DynamicOps<U> targetOps) {
        Preconditions.checkNotNull(targetOps, "targetOps must not be null");
        final U converted = targetOps.convertTo(this.ops, this.value);
        return new Dynamic<>(targetOps, converted);
    }

    /**
     * Transforms this Dynamic's underlying value using the provided mapping function.
     *
     * <p>This method applies a transformation to the raw underlying value without
     * changing the ops. Use this for low-level manipulations when the standard fluent API doesn't provide the needed
     * operation.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * Dynamic<JsonElement> dynamic = ...;
     *
     * // Apply a custom transformation to the underlying JsonElement
     * Dynamic<JsonElement> transformed = dynamic.map(element -> {
     *     // Custom manipulation of the JsonElement
     *     if (element.isJsonObject()) {
     *         element.getAsJsonObject().addProperty("transformed", true);
     *     }
     *     return element;
     * });
     * }</pre>
     *
     * <p><b>Note:</b> Prefer the higher-level methods like {@link #set(String, Dynamic)} and
     * {@link #update(String, Function)} when possible, as they provide better type safety.</p>
     *
     * @param mapper the function to transform the underlying value; must not be {@code null}
     * @return a new Dynamic with the transformed value; never {@code null}
     * @throws NullPointerException if {@code mapper} is {@code null}
     */
    @NotNull
    public Dynamic<T> map(@NotNull final Function<T, T> mapper) {
        Preconditions.checkNotNull(mapper, "mapper must not be null");
        return new Dynamic<>(this.ops, mapper.apply(this.value));
    }
}
