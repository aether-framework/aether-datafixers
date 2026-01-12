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

package de.splatgames.aether.datafixers.codec.yaml.snakeyaml;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A {@link DynamicOps} implementation for SnakeYAML's native Java type representation.
 *
 * <p>This class provides format-agnostic data manipulation capabilities for SnakeYAML's
 * data model, enabling the Aether Datafixers system to read, write, and transform YAML
 * data without coupling application code to YAML-specific parsing details. It serves as
 * the bridge between the abstract data fixer operations and SnakeYAML's native Java
 * object representation.</p>
 *
 * <p>Unlike JSON libraries (Gson, Jackson) that use custom node types, SnakeYAML represents
 * parsed YAML data as standard Java types: {@link Map}, {@link List}, {@link String},
 * {@link Number}, {@link Boolean}, and {@code null}. This implementation adapts these
 * native types to the {@link DynamicOps} interface.</p>
 *
 * <h2>Design Pattern</h2>
 * <p>This class implements the Singleton pattern. Use {@link #INSTANCE} for all operations.
 * The singleton design is appropriate because:</p>
 * <ul>
 *   <li>The class maintains no mutable state</li>
 *   <li>All operations are purely functional transformations</li>
 *   <li>A single instance reduces memory overhead and GC pressure</li>
 *   <li>Native Java types require no library-specific configuration</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Parsing and Wrapping</h3>
 * <pre>{@code
 * // Parse YAML using SnakeYAML
 * Yaml yaml = new Yaml();
 * Object data = yaml.load(yamlString);
 *
 * // Wrap the parsed data for manipulation
 * Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, data);
 *
 * // Access nested fields
 * Optional<String> name = dynamic.get("player").get("name").asString();
 *
 * // Transform data
 * Dynamic<Object> updated = dynamic.set("version", SnakeYamlOps.INSTANCE.createInt(2));
 * }</pre>
 *
 * <h3>Encoding and Decoding with Codecs</h3>
 * <pre>{@code
 * // Encode a value to YAML-compatible format
 * DataResult<Object> encoded = playerCodec.encodeStart(SnakeYamlOps.INSTANCE, player);
 *
 * // Decode YAML data to a typed value
 * DataResult<Player> decoded = playerCodec.decode(SnakeYamlOps.INSTANCE, yamlData);
 *
 * // Serialize to YAML string
 * encoded.result().ifPresent(data -> {
 *     Yaml yaml = new Yaml();
 *     String yamlString = yaml.dump(data);
 * });
 * }</pre>
 *
 * <h3>Format Conversion</h3>
 * <pre>{@code
 * // Convert from Gson JSON to SnakeYAML native types
 * Object yamlData = SnakeYamlOps.INSTANCE.convertTo(GsonOps.INSTANCE, jsonElement);
 *
 * // Convert from SnakeYAML to Jackson JSON
 * JsonNode jacksonNode = JacksonOps.INSTANCE.convertTo(SnakeYamlOps.INSTANCE, yamlData);
 *
 * // Round-trip: YAML -> JSON -> YAML
 * JsonElement json = GsonOps.INSTANCE.convertTo(SnakeYamlOps.INSTANCE, yamlData);
 * Object backToYaml = SnakeYamlOps.INSTANCE.convertTo(GsonOps.INSTANCE, json);
 * }</pre>
 *
 * <h2>Type Mapping</h2>
 * <p>The following table shows how abstract/Java types map to SnakeYAML's native representation:</p>
 * <table class="striped">
 *   <caption>Type Mapping between Abstract Types and SnakeYAML Native Types</caption>
 *   <tr><th>Abstract Type</th><th>SnakeYAML Native Type</th><th>Notes</th></tr>
 *   <tr><td>{@code boolean}</td><td>{@link Boolean}</td><td>Java boxed boolean</td></tr>
 *   <tr><td>{@code int}</td><td>{@link Integer}</td><td>Java boxed integer</td></tr>
 *   <tr><td>{@code long}</td><td>{@link Long}</td><td>Java boxed long</td></tr>
 *   <tr><td>{@code float}</td><td>{@link Float}</td><td>Java boxed float</td></tr>
 *   <tr><td>{@code double}</td><td>{@link Double}</td><td>Java boxed double</td></tr>
 *   <tr><td>{@code byte}</td><td>{@link Byte}</td><td>Java boxed byte</td></tr>
 *   <tr><td>{@code short}</td><td>{@link Short}</td><td>Java boxed short</td></tr>
 *   <tr><td>{@code String}</td><td>{@link String}</td><td>Standard Java string</td></tr>
 *   <tr><td>{@code List/Stream}</td><td>{@link ArrayList}</td><td>Preserves element order; mutable internally</td></tr>
 *   <tr><td>{@code Map}</td><td>{@link LinkedHashMap}</td><td>Preserves insertion order; string keys only</td></tr>
 *   <tr><td>{@code null/empty}</td><td>{@link #NULL} sentinel</td><td>Singleton marker object; use {@link #unwrap(Object)} for serialization</td></tr>
 * </table>
 *
 * <h2>SnakeYAML-Specific Considerations</h2>
 * <p>Working with SnakeYAML's native types requires awareness of several characteristics:</p>
 * <ul>
 *   <li><strong>Mutable Collections:</strong> SnakeYAML produces mutable {@link ArrayList} and
 *       {@link LinkedHashMap} instances. This implementation creates defensive copies to ensure
 *       immutability guarantees.</li>
 *   <li><strong>Null Representation:</strong> YAML's {@code null} and {@code ~} values are
 *       represented by the {@link #NULL} sentinel object. Use {@link #wrap(Object)} after parsing
 *       YAML with SnakeYAML to convert Java {@code null} to the sentinel, and {@link #unwrap(Object)}
 *       before serializing to convert back.</li>
 *   <li><strong>Number Types:</strong> SnakeYAML preserves the specific numeric type from the
 *       YAML source (e.g., integers vs. floats), which is maintained in this implementation.</li>
 *   <li><strong>Key Types:</strong> While YAML supports complex keys, this implementation
 *       normalizes all map keys to {@link String} via {@link Object#toString()}.</li>
 *   <li><strong>Insertion Order:</strong> {@link LinkedHashMap} is used to preserve the order
 *       of fields as they appear in the original YAML document.</li>
 * </ul>
 *
 * <h2>Immutability Contract</h2>
 * <p>All modification operations in this class preserve immutability of input data through
 * deep copying:</p>
 * <ul>
 *   <li>{@link #set(Object, String, Object)} creates a deep copy of the map before modification</li>
 *   <li>{@link #remove(Object, String)} creates a deep copy of the map before removal</li>
 *   <li>{@link #mergeToMap(Object, Object, Object)} creates a deep copy of the target map and
 *       the value being added</li>
 *   <li>{@link #mergeToMap(Object, Object)} creates a deep copy of both maps during merge</li>
 *   <li>{@link #mergeToList(Object, Object)} creates a deep copy of the list and the value</li>
 * </ul>
 * <p>Deep copying is essential because SnakeYAML's native types ({@link ArrayList},
 * {@link LinkedHashMap}) are mutable. Without deep copying, modifications would affect the
 * original data structures. The {@link #deepCopy(Object)} helper method recursively copies
 * all nested collections while leaving immutable primitives unchanged.</p>
 *
 * <h2>Error Handling</h2>
 * <p>Operations that may fail return {@link DataResult} instead of throwing exceptions:</p>
 * <ul>
 *   <li>Type mismatches result in {@link DataResult#error(String)} with descriptive messages</li>
 *   <li>Successful operations return {@link DataResult#success(Object)}</li>
 *   <li>Callers should check {@link DataResult#isSuccess()} or use {@link DataResult#result()}</li>
 * </ul>
 * <p>Error messages include the actual value that caused the error for debugging purposes.
 * For example: {@code "Not a string: {key=value}"} or {@code "Not a map: [1, 2, 3]"}.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is fully thread-safe:</p>
 * <ul>
 *   <li>The singleton instance can be safely shared across threads</li>
 *   <li>All operations are stateless and side-effect free</li>
 *   <li>No internal mutable state exists</li>
 *   <li>Deep copying prevents shared mutable state between threads</li>
 * </ul>
 * <p>However, the data structures produced by this class ({@link ArrayList},
 * {@link LinkedHashMap}) are not thread-safe themselves. Callers must ensure proper
 * synchronization if the same data structure is accessed from multiple threads.</p>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Deep Copy Overhead:</strong> Deep copy operations have O(n) time and space
 *       complexity where n is the total number of elements in the structure. For large
 *       data structures, consider batching modifications.</li>
 *   <li><strong>No Node Wrapper:</strong> Unlike Gson/Jackson, SnakeYAML's native types
 *       have no wrapper overhead, resulting in slightly better memory efficiency for
 *       large datasets.</li>
 *   <li><strong>Batch Modifications:</strong> Prefer {@link #createMap(Stream)} and
 *       {@link #createList(Stream)} over repeated {@link #set} or {@link #mergeToList}
 *       calls to minimize copy operations.</li>
 *   <li><strong>Stream Operations:</strong> Stream-based operations are lazy and support
 *       short-circuiting where applicable.</li>
 *   <li><strong>Instanceof Checks:</strong> Type checking uses {@code instanceof} which is
 *       highly optimized in modern JVMs and has negligible performance impact.</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see DynamicOps
 * @see de.splatgames.aether.datafixers.api.dynamic.Dynamic
 * @see DataResult
 * @since 0.4.0
 */
public final class SnakeYamlOps implements DynamicOps<Object> {

    /**
     * The singleton instance of {@code SnakeYamlOps}.
     *
     * <p>This instance should be used for all SnakeYAML operations throughout the application.
     * It is immutable, stateless, and thread-safe, making it suitable for use in any context
     * including multi-threaded environments and dependency injection containers.</p>
     *
     * <p><b>Usage</b></p>
     * <pre>{@code
     * // Direct usage
     * Object yamlString = SnakeYamlOps.INSTANCE.createString("hello");
     *
     * // With Dynamic
     * Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, yamlData);
     *
     * // With Codecs
     * DataResult<Object> result = myCodec.encodeStart(SnakeYamlOps.INSTANCE, value);
     *
     * // With SnakeYAML parser
     * Yaml yaml = new Yaml();
     * Object parsed = yaml.load(inputStream);
     * Dynamic<Object> wrapped = new Dynamic<>(SnakeYamlOps.INSTANCE, parsed);
     * }</pre>
     */
    public static final SnakeYamlOps INSTANCE = new SnakeYamlOps();

    /**
     * Sentinel object representing the YAML null value.
     *
     * <p>This singleton instance represents the absence of a value in YAML, corresponding to
     * YAML's explicit {@code null} or {@code ~} values. Unlike using Java's {@code null} directly,
     * this sentinel allows the {@link DynamicOps} contract to be fulfilled (which requires
     * {@link #empty()} to return a non-null value).</p>
     *
     * <p><b>Usage</b></p>
     * <pre>{@code
     * // Check if a value is the YAML null sentinel
     * if (value == SnakeYamlOps.NULL) {
     *     // Handle null case
     * }
     *
     * // Create an explicit null value
     * Object nullValue = SnakeYamlOps.NULL;
     * }</pre>
     *
     * <p><b>Serialization Note</b></p>
     * <p>When serializing data containing this sentinel to YAML text using SnakeYAML, you should
     * convert the sentinel back to Java {@code null} before serialization. Use
     * {@link #unwrap(Object)} for this purpose:</p>
     * <pre>{@code
     * Object data = ...; // May contain YamlNull.INSTANCE
     * Object unwrapped = SnakeYamlOps.unwrap(data);
     * String yaml = new Yaml().dump(unwrapped);
     * }</pre>
     *
     * @see #empty()
     * @see #isNull(Object)
     * @see #unwrap(Object)
     */
    public static final Object NULL = YamlNull.INSTANCE;

    /**
     * Private constructor to enforce singleton pattern.
     *
     * <p>This constructor is intentionally private to prevent instantiation from outside
     * the class. Use {@link #INSTANCE} to access the singleton instance.</p>
     *
     * <p>The singleton pattern is used because this class is stateless and all operations
     * are pure functions, making multiple instances unnecessary and wasteful.</p>
     */
    private SnakeYamlOps() {
        // Singleton - use INSTANCE
    }

    // ==================== Empty/Null Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Returns the {@link #NULL} sentinel which represents the absence of a value in YAML.
     * This corresponds to YAML's explicit {@code null} or {@code ~} values.</p>
     *
     * <p>The sentinel object is used instead of Java's {@code null} to satisfy the
     * {@link DynamicOps} contract which requires this method to return a non-null value.
     * Use {@link #isNull(Object)} to check if a value is the null sentinel, or compare
     * directly with {@code == SnakeYamlOps.NULL}.</p>
     *
     * <p>When serializing data to YAML text format, use {@link #unwrap(Object)} to convert
     * the sentinel back to Java {@code null} before passing to SnakeYAML.</p>
     *
     * @return the {@link #NULL} sentinel representing the absence of a value; never {@code null}
     * @see #NULL
     * @see #isNull(Object)
     * @see #unwrap(Object)
     */
    @NotNull
    @Override
    public Object empty() {
        return YamlNull.INSTANCE;
    }

    // ==================== Type Check Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given value represents a map/object structure. In SnakeYAML's
     * native representation, maps are stored as {@link Map} instances (typically
     * {@link LinkedHashMap} to preserve insertion order).</p>
     *
     * @param value the value to check; must not be {@code null}
     * @return {@code true} if the value is a {@link Map} instance, {@code false} otherwise
     *         (including for {@link List}, primitives, and {@code null})
     */
    @Override
    public boolean isMap(@NotNull final Object value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value instanceof Map;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given value represents a list/array structure. In SnakeYAML's
     * native representation, lists are stored as {@link List} instances (typically
     * {@link ArrayList}).</p>
     *
     * @param value the value to check; must not be {@code null}
     * @return {@code true} if the value is a {@link List} instance, {@code false} otherwise
     *         (including for {@link Map}, primitives, and {@code null})
     */
    @Override
    public boolean isList(@NotNull final Object value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value instanceof List;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given value represents a string. In SnakeYAML's native
     * representation, strings are stored as standard Java {@link String} instances.</p>
     *
     * @param value the value to check; must not be {@code null}
     * @return {@code true} if the value is a {@link String} instance, {@code false} otherwise
     *         (including for numeric and boolean values)
     */
    @Override
    public boolean isString(@NotNull final Object value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value instanceof String;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given value represents a numeric value. In SnakeYAML's native
     * representation, numbers are stored as their Java wrapper types ({@link Integer},
     * {@link Long}, {@link Double}, {@link Float}, {@link Byte}, {@link Short}) which all
     * implement the {@link Number} interface.</p>
     *
     * @param value the value to check; must not be {@code null}
     * @return {@code true} if the value is a {@link Number} instance, {@code false} otherwise
     *         (including for string and boolean values)
     */
    @Override
    public boolean isNumber(@NotNull final Object value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value instanceof Number;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Determines whether the given value represents a boolean value. In SnakeYAML's native
     * representation, booleans are stored as Java {@link Boolean} instances. YAML supports
     * various boolean representations ({@code true}, {@code false}, {@code yes}, {@code no},
     * {@code on}, {@code off}) which SnakeYAML normalizes to Java {@link Boolean}.</p>
     *
     * @param value the value to check; must not be {@code null}
     * @return {@code true} if the value is a {@link Boolean} instance, {@code false} otherwise
     *         (including for string and numeric values)
     */
    @Override
    public boolean isBoolean(@NotNull final Object value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value instanceof Boolean;
    }

    // ==================== Primitive Creation Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a string value for the SnakeYAML format. Since SnakeYAML uses native Java
     * types, the input string is returned directly without wrapping.</p>
     *
     * @param value the string value to create; must not be {@code null}
     * @return the string value itself; never {@code null}
     */
    @NotNull
    @Override
    public Object createString(@NotNull final String value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates an integer value for the SnakeYAML format. The value is auto-boxed to an
     * {@link Integer} wrapper which is the standard SnakeYAML representation for integers.</p>
     *
     * @param value the integer value to create
     * @return the integer value as an {@link Integer}; never {@code null}
     */
    @NotNull
    @Override
    public Object createInt(final int value) {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a long value for the SnakeYAML format. The value is auto-boxed to a
     * {@link Long} wrapper. SnakeYAML preserves the distinction between integer and long
     * types during parsing and serialization.</p>
     *
     * <p><strong>Note:</strong> When serializing to YAML text, very large long values are
     * represented as-is and may not be parseable by all YAML implementations.</p>
     *
     * @param value the long value to create
     * @return the long value as a {@link Long}; never {@code null}
     */
    @NotNull
    @Override
    public Object createLong(final long value) {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a float value for the SnakeYAML format. The value is auto-boxed to a
     * {@link Float} wrapper. SnakeYAML represents floating-point numbers in YAML's
     * standard notation.</p>
     *
     * <p><strong>Note:</strong> YAML supports special float values ({@code .inf}, {@code -.inf},
     * {@code .nan}) which map to {@link Float#POSITIVE_INFINITY}, {@link Float#NEGATIVE_INFINITY},
     * and {@link Float#NaN} respectively.</p>
     *
     * @param value the float value to create
     * @return the float value as a {@link Float}; never {@code null}
     */
    @NotNull
    @Override
    public Object createFloat(final float value) {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a double value for the SnakeYAML format. The value is auto-boxed to a
     * {@link Double} wrapper. This is the default type for floating-point numbers in
     * SnakeYAML when no specific type hint is provided.</p>
     *
     * <p><strong>Note:</strong> YAML supports special float values ({@code .inf}, {@code -.inf},
     * {@code .nan}) which map to {@link Double#POSITIVE_INFINITY}, {@link Double#NEGATIVE_INFINITY},
     * and {@link Double#NaN} respectively.</p>
     *
     * @param value the double value to create
     * @return the double value as a {@link Double}; never {@code null}
     */
    @NotNull
    @Override
    public Object createDouble(final double value) {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a byte value for the SnakeYAML format. The value is auto-boxed to a
     * {@link Byte} wrapper.</p>
     *
     * <p><strong>Note:</strong> YAML does not have a dedicated byte type. When serialized
     * to YAML text and re-parsed, the value may be interpreted as a regular integer.
     * The specific byte type is only preserved within the Java representation.</p>
     *
     * @param value the byte value to create
     * @return the byte value as a {@link Byte}; never {@code null}
     */
    @NotNull
    @Override
    public Object createByte(final byte value) {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a short value for the SnakeYAML format. The value is auto-boxed to a
     * {@link Short} wrapper.</p>
     *
     * <p><strong>Note:</strong> YAML does not have a dedicated short type. When serialized
     * to YAML text and re-parsed, the value may be interpreted as a regular integer.
     * The specific short type is only preserved within the Java representation.</p>
     *
     * @param value the short value to create
     * @return the short value as a {@link Short}; never {@code null}
     */
    @NotNull
    @Override
    public Object createShort(final short value) {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a boolean value for the SnakeYAML format. The value is auto-boxed to a
     * {@link Boolean} wrapper. When serialized to YAML, this produces {@code true} or
     * {@code false} literals.</p>
     *
     * @param value the boolean value to create
     * @return the boolean value as a {@link Boolean}; never {@code null}
     */
    @NotNull
    @Override
    public Object createBoolean(final boolean value) {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a numeric value for the SnakeYAML format from any {@link Number} subclass.
     * The number is stored directly, preserving its exact type. This is the most flexible
     * numeric creation method and maintains type fidelity for {@link java.math.BigInteger},
     * {@link java.math.BigDecimal}, and other Number subclasses.</p>
     *
     * @param value the number value to create; must not be {@code null}
     * @return the number value; never {@code null}
     */
    @NotNull
    @Override
    public Object createNumeric(@NotNull final Number value) {
        Preconditions.checkNotNull(value, "value must not be null");
        return value;
    }

    // ==================== Primitive Reading Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the string value from a SnakeYAML native type. This operation succeeds
     * only if the input is a {@link String} instance.</p>
     *
     * <p><b>Success Conditions</b></p>
     * <ul>
     *   <li>Input is a {@link String} instance</li>
     * </ul>
     *
     * <p><b>Failure Conditions</b></p>
     * <ul>
     *   <li>Input is not a {@link String} (e.g., Map, List, Number, Boolean, null)</li>
     * </ul>
     *
     * @param input the value to extract the string from; must not be {@code null}
     * @return a {@link DataResult} containing the string value on success, or an error
     *         message describing why extraction failed; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<String> getStringValue(@NotNull final Object input) {
        Preconditions.checkNotNull(input, "input must not be null");
        if (!(input instanceof String)) {
            return DataResult.error("Not a string: " + input);
        }
        return DataResult.success((String) input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the numeric value from a SnakeYAML native type. This operation succeeds
     * only if the input is a {@link Number} instance (including {@link Integer}, {@link Long},
     * {@link Double}, {@link Float}, {@link Byte}, {@link Short}, and other Number subclasses).</p>
     *
     * <p><b>Success Conditions</b></p>
     * <ul>
     *   <li>Input is a {@link Number} instance (any subclass)</li>
     * </ul>
     *
     * <p><b>Failure Conditions</b></p>
     * <ul>
     *   <li>Input is not a {@link Number} (e.g., Map, List, String, Boolean, null)</li>
     * </ul>
     *
     * <p>The returned {@link Number} preserves the original numeric type, allowing callers
     * to use type-specific accessors like {@link Number#intValue()} or
     * {@link Number#doubleValue()}.</p>
     *
     * @param input the value to extract the number from; must not be {@code null}
     * @return a {@link DataResult} containing the number value on success, or an error
     *         message describing why extraction failed; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<Number> getNumberValue(@NotNull final Object input) {
        Preconditions.checkNotNull(input, "input must not be null");
        if (!(input instanceof Number)) {
            return DataResult.error("Not a number: " + input);
        }
        return DataResult.success((Number) input);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the boolean value from a SnakeYAML native type. This operation succeeds
     * only if the input is a {@link Boolean} instance.</p>
     *
     * <p><b>Success Conditions</b></p>
     * <ul>
     *   <li>Input is a {@link Boolean} instance</li>
     * </ul>
     *
     * <p><b>Failure Conditions</b></p>
     * <ul>
     *   <li>Input is not a {@link Boolean} (e.g., Map, List, String, Number, null)</li>
     * </ul>
     *
     * @param input the value to extract the boolean from; must not be {@code null}
     * @return a {@link DataResult} containing the boolean value on success, or an error
     *         message describing why extraction failed; never {@code null}
     */
    @NotNull
    @Override
    public DataResult<Boolean> getBooleanValue(@NotNull final Object input) {
        Preconditions.checkNotNull(input, "input must not be null");
        if (!(input instanceof Boolean)) {
            return DataResult.error("Not a boolean: " + input);
        }
        return DataResult.success((Boolean) input);
    }

    // ==================== List Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new empty list for the SnakeYAML format. Returns a new {@link ArrayList}
     * instance, which is the standard list implementation used by SnakeYAML.</p>
     *
     * <p>The returned list is mutable but should be treated as immutable by callers to
     * maintain consistency with the immutability contract of this class.</p>
     *
     * @return a new empty {@link ArrayList}; never {@code null}
     */
    @NotNull
    @Override
    public Object emptyList() {
        return new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new list containing all elements from the provided stream. Elements are
     * added to an {@link ArrayList} in encounter order. The stream is consumed completely
     * by this operation.</p>
     *
     * <p><strong>Note:</strong> {@code null} elements in the stream are added as-is, which
     * is valid in SnakeYAML's representation (unlike some JSON libraries).</p>
     *
     * @param values a stream of values to include in the list; must not be {@code null};
     *               may be empty; may contain {@code null} elements
     * @return a new {@link ArrayList} containing all stream elements in order;
     *         never {@code null}
     */
    @NotNull
    @Override
    public Object createList(@NotNull final Stream<Object> values) {
        Preconditions.checkNotNull(values, "values must not be null");
        final List<Object> list = new ArrayList<>();
        values.forEach(list::add);
        return list;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the elements of a list as a stream. This operation succeeds only if the
     * input is a {@link List} instance.</p>
     *
     * <p><b>Success Conditions</b></p>
     * <ul>
     *   <li>Input is a {@link List} instance (including empty lists)</li>
     * </ul>
     *
     * <p><b>Failure Conditions</b></p>
     * <ul>
     *   <li>Input is not a {@link List} (e.g., Map, primitive, null)</li>
     * </ul>
     *
     * <p>The returned stream provides sequential access to list elements in order. The
     * stream does not support parallel processing by default.</p>
     *
     * @param input the value to extract list elements from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of list elements on success,
     *         or an error message if the input is not a list; never {@code null}
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Stream<Object>> getList(@NotNull final Object input) {
        Preconditions.checkNotNull(input, "input must not be null");
        if (!(input instanceof List)) {
            return DataResult.error("Not a list: " + input);
        }
        return DataResult.success(((List<Object>) input).stream());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new list by appending a value to an existing list. This operation creates
     * a shallow copy of the input list and a deep copy of the value to preserve immutability.</p>
     *
     * <p><b>Success Conditions</b></p>
     * <ul>
     *   <li>Input list is a {@link List} instance</li>
     * </ul>
     *
     * <p><b>Failure Conditions</b></p>
     * <ul>
     *   <li>Input list is not a {@link List} instance (e.g., Map, primitive)</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original list is never modified. A new
     * {@link ArrayList} is created from the original elements, and the value is deep-copied
     * before being appended to ensure nested structures are also copied.</p>
     *
     * @param list  the list to append to; must not be {@code null}
     * @param value the value to append; must not be {@code null}
     * @return a {@link DataResult} containing the new list with the appended value,
     *         or an error message if the list is not valid; never {@code null}
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Object> mergeToList(@NotNull final Object list,
                                          @NotNull final Object value) {
        Preconditions.checkNotNull(list, "list must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        if (!(list instanceof List)) {
            return DataResult.error("Not a list: " + list);
        }
        final List<Object> result = new ArrayList<>((List<Object>) list);
        result.add(deepCopy(value));
        return DataResult.success(result);
    }

    // ==================== Map Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new empty map for the SnakeYAML format. Returns a new {@link LinkedHashMap}
     * instance, which preserves insertion order and is the standard map implementation used
     * by SnakeYAML for YAML objects/mappings.</p>
     *
     * <p>The returned map is mutable but should be treated as immutable by callers to
     * maintain consistency with the immutability contract of this class.</p>
     *
     * @return a new empty {@link LinkedHashMap}; never {@code null}
     */
    @NotNull
    @Override
    public Object emptyMap() {
        return new LinkedHashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new map from a stream of key-value pairs. Keys are converted to strings
     * via {@link Object#toString()}; entries with {@code null} keys are skipped.</p>
     *
     * <p><b>Key Handling</b></p>
     * <ul>
     *   <li>Keys are converted to strings via {@link Object#toString()}</li>
     *   <li>Entries with {@code null} keys are silently skipped</li>
     *   <li>Duplicate keys result in the last value being retained</li>
     * </ul>
     *
     * <p><b>Value Handling</b></p>
     * <ul>
     *   <li>Values are added as-is without modification</li>
     *   <li>{@code null} values are valid and preserved</li>
     * </ul>
     *
     * <p>A {@link LinkedHashMap} is used to preserve the insertion order of entries,
     * matching SnakeYAML's behavior of maintaining field order from the source YAML.</p>
     *
     * @param entries a stream of key-value pairs; must not be {@code null}; may be empty
     * @return a new {@link LinkedHashMap} containing all valid entries; never {@code null}
     */
    @NotNull
    @Override
    public Object createMap(@NotNull final Stream<Pair<Object, Object>> entries) {
        Preconditions.checkNotNull(entries, "entries must not be null");
        final Map<String, Object> map = new LinkedHashMap<>();
        entries.forEach(pair -> {
            final Object keyObj = pair.first();
            final Object valueObj = pair.second();
            if (keyObj == null) {
                return; // Skip entries with null keys
            }
            final String key = keyObj.toString();
            map.put(key, valueObj);
        });
        return map;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the entries of a map as a stream of key-value pairs. This operation succeeds
     * only if the input is a {@link Map} instance.</p>
     *
     * <p><b>Success Conditions</b></p>
     * <ul>
     *   <li>Input is a {@link Map} instance (including empty maps)</li>
     * </ul>
     *
     * <p><b>Failure Conditions</b></p>
     * <ul>
     *   <li>Input is not a {@link Map} (e.g., List, primitive, null)</li>
     * </ul>
     *
     * <p>Each entry in the returned stream has:</p>
     * <ul>
     *   <li>Key: the original {@link String} key from the map</li>
     *   <li>Value: the entry's value (may be any type including {@code null})</li>
     * </ul>
     *
     * @param input the value to extract map entries from; must not be {@code null}
     * @return a {@link DataResult} containing a stream of key-value pairs on success,
     *         or an error message if the input is not a map; never {@code null}
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Stream<Pair<Object, Object>>> getMapEntries(@NotNull final Object input) {
        Preconditions.checkNotNull(input, "input must not be null");
        if (!(input instanceof Map)) {
            return DataResult.error("Not a map: " + input);
        }
        final Map<String, Object> map = (Map<String, Object>) input;
        return DataResult.success(
                map.entrySet().stream()
                        .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new map by adding or updating a key-value pair in an existing map. This
     * operation creates a shallow copy of the input map and a deep copy of the value to
     * preserve immutability.</p>
     *
     * <p><b>Success Conditions</b></p>
     * <ul>
     *   <li>Input map is a {@link Map} instance</li>
     *   <li>Key is a {@link String}</li>
     * </ul>
     *
     * <p><b>Failure Conditions</b></p>
     * <ul>
     *   <li>Input map is not a {@link Map} instance (e.g., List, primitive)</li>
     *   <li>Key is not a {@link String}</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original map is never modified. A new
     * {@link LinkedHashMap} is created from the original entries, and the value is
     * deep-copied before being added.</p>
     *
     * @param map   the map to add the entry to; must not be {@code null}
     * @param key   the key for the entry; must not be {@code null}; must be a {@link String}
     * @param value the value for the entry; must not be {@code null}
     * @return a {@link DataResult} containing the new map with the added entry,
     *         or an error message if parameters are invalid; never {@code null}
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Object> mergeToMap(@NotNull final Object map,
                                         @NotNull final Object key,
                                         @NotNull final Object value) {
        Preconditions.checkNotNull(map, "map must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(value, "value must not be null");
        if (!(map instanceof Map)) {
            return DataResult.error("Not a map: " + map);
        }
        if (!(key instanceof String)) {
            return DataResult.error("Key is not a string: " + key);
        }
        final Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) map);
        result.put((String) key, deepCopy(value));
        return DataResult.success(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new map by merging all entries from a second map into a copy of the first
     * map. This operation creates deep copies of all values to preserve immutability.</p>
     *
     * <p><b>Success Conditions</b></p>
     * <ul>
     *   <li>Both inputs are {@link Map} instances</li>
     * </ul>
     *
     * <p><b>Failure Conditions</b></p>
     * <ul>
     *   <li>First input is not a {@link Map} instance</li>
     *   <li>Second input is not a {@link Map} instance</li>
     * </ul>
     *
     * <p><b>Merge Behavior</b></p>
     * <ul>
     *   <li>Entries from the second map override entries with the same key in the first</li>
     *   <li>Entries unique to either map are included in the result</li>
     *   <li>Insertion order from the first map is preserved, with new entries appended</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> Neither input map is modified. A new
     * {@link LinkedHashMap} is created, and all values from the second map are deep-copied
     * before being added.</p>
     *
     * @param map   the base map; must not be {@code null}
     * @param other the map to merge into the base; must not be {@code null}
     * @return a {@link DataResult} containing the merged map, or an error message
     *         if either input is invalid; never {@code null}
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public DataResult<Object> mergeToMap(@NotNull final Object map,
                                         @NotNull final Object other) {
        Preconditions.checkNotNull(map, "map must not be null");
        Preconditions.checkNotNull(other, "other must not be null");
        if (!(map instanceof Map)) {
            return DataResult.error("First argument is not a map: " + map);
        }
        if (!(other instanceof Map)) {
            return DataResult.error("Second argument is not a map: " + other);
        }
        final Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) map);
        for (final Map.Entry<String, Object> entry : ((Map<String, Object>) other).entrySet()) {
            result.put(entry.getKey(), deepCopy(entry.getValue()));
        }
        return DataResult.success(result);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves a field value from a map by key. Returns {@code null} if the input is
     * not a map or the key does not exist.</p>
     *
     * <p><b>Behavior</b></p>
     * <ul>
     *   <li>Returns the field value if the input is a {@link Map} containing the key</li>
     *   <li>Returns {@code null} if the input is not a {@link Map}</li>
     *   <li>Returns {@code null} if the key does not exist in the map</li>
     *   <li>Returns the actual value (which may be {@code null}) if the key exists</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This method does not distinguish between a missing key
     * and a key mapped to {@code null}. Use {@link #has(Object, String)} to check for
     * key existence when this distinction matters.</p>
     *
     * @param input the value to retrieve the field from; must not be {@code null}
     * @param key   the field name to retrieve; must not be {@code null}
     * @return the field value if found, or {@code null} if not found or input is not a map
     */
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public Object get(@NotNull final Object input,
                      @NotNull final String key) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        if (!(input instanceof Map)) {
            return null;
        }
        final Map<String, Object> map = (Map<String, Object>) input;
        return map.get(key);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new map with a field set to the specified value. This operation creates
     * a shallow copy of the input map (if it is a map) and a deep copy of the value to
     * preserve immutability.</p>
     *
     * <p><b>Behavior</b></p>
     * <ul>
     *   <li>If input is a {@link Map}: creates a copy and sets/updates the field</li>
     *   <li>If input is not a {@link Map}: creates a new map with just the field</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original input is never modified. When the
     * input is a map, a new {@link LinkedHashMap} is created from the original entries.
     * The value is deep-copied before being added to ensure nested structures are not
     * shared between the original and the copy.</p>
     *
     * @param input    the value to set the field on; must not be {@code null}
     * @param key      the field name to set; must not be {@code null}
     * @param newValue the value to set; must not be {@code null}
     * @return a new {@link LinkedHashMap} with the field set; never {@code null}
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Object set(@NotNull final Object input,
                      @NotNull final String key,
                      @NotNull final Object newValue) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(newValue, "newValue must not be null");
        final Map<String, Object> result;
        if (input instanceof Map) {
            result = new LinkedHashMap<>((Map<String, Object>) input);
        } else {
            result = new LinkedHashMap<>();
        }
        result.put(key, deepCopy(newValue));
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new map with a field removed. This operation creates a shallow copy of
     * the input map to preserve immutability.</p>
     *
     * <p><b>Behavior</b></p>
     * <ul>
     *   <li>If input is a {@link Map}: creates a copy and removes the field</li>
     *   <li>If input is not a {@link Map}: returns the input unchanged</li>
     *   <li>If the key does not exist: returns a copy without modification</li>
     * </ul>
     *
     * <p><strong>Immutability:</strong> The original input is never modified. When the
     * input is a map, a new {@link LinkedHashMap} is created from the original entries
     * before the field is removed.</p>
     *
     * @param input the value to remove the field from; must not be {@code null}
     * @param key   the field name to remove; must not be {@code null}
     * @return a new value with the field removed (or unchanged if not applicable);
     *         never {@code null}
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Object remove(@NotNull final Object input,
                         @NotNull final String key) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        if (!(input instanceof Map)) {
            return input;
        }
        final Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) input);
        result.remove(key);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks whether a map contains a field with the specified key.</p>
     *
     * <p><b>Behavior</b></p>
     * <ul>
     *   <li>Returns {@code true} if input is a {@link Map} and contains the key</li>
     *   <li>Returns {@code false} if input is not a {@link Map}</li>
     *   <li>Returns {@code false} if the key does not exist</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This method returns {@code true} even if the key is
     * mapped to {@code null}. It only checks for key existence, not value validity.
     * This is useful to distinguish between "key not present" and "key present with
     * null value".</p>
     *
     * @param input the value to check; must not be {@code null}
     * @param key   the field name to check for; must not be {@code null}
     * @return {@code true} if the input is a map containing the key, {@code false} otherwise
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean has(@NotNull final Object input,
                       @NotNull final String key) {
        Preconditions.checkNotNull(input, "input must not be null");
        Preconditions.checkNotNull(key, "key must not be null");
        if (!(input instanceof Map)) {
            return false;
        }
        return ((Map<String, Object>) input).containsKey(key);
    }

    // ==================== Conversion Operations ====================

    /**
     * {@inheritDoc}
     *
     * <p>Converts data from another {@link DynamicOps} format to SnakeYAML's native Java
     * types. This method recursively converts all nested structures, handling primitives,
     * lists, and maps appropriately.</p>
     *
     * <p><b>Conversion Process</b></p>
     * <p>The conversion attempts to identify the input type in the following order:</p>
     * <ol>
     *   <li><strong>Boolean:</strong> If {@link DynamicOps#getBooleanValue} succeeds,
     *       returns a {@link Boolean}</li>
     *   <li><strong>Number:</strong> If {@link DynamicOps#getNumberValue} succeeds,
     *       returns the {@link Number} directly</li>
     *   <li><strong>String:</strong> If {@link DynamicOps#getStringValue} succeeds,
     *       returns the {@link String}</li>
     *   <li><strong>List:</strong> If {@link DynamicOps#getList} succeeds,
     *       creates an {@link ArrayList} with recursively converted elements</li>
     *   <li><strong>Map:</strong> If {@link DynamicOps#getMapEntries} succeeds,
     *       creates a {@link LinkedHashMap} with recursively converted entries</li>
     *   <li><strong>Fallback:</strong> Returns the {@link #NULL} sentinel if no type matches</li>
     * </ol>
     *
     * <p><b>Edge Cases</b></p>
     * <ul>
     *   <li>Map entries with {@code null} keys are skipped</li>
     *   <li>Map entries with {@code null} values are converted to the {@link #NULL} sentinel</li>
     *   <li>Empty collections are preserved as empty ArrayList/LinkedHashMap</li>
     * </ul>
     *
     * <p><b>Format-Specific Notes</b></p>
     * <ul>
     *   <li>Gson's {@code JsonNull} is converted to the {@link #NULL} sentinel</li>
     *   <li>Jackson's {@code NullNode} is converted to the {@link #NULL} sentinel</li>
     *   <li>Numeric types are preserved where the source format supports them</li>
     * </ul>
     *
     * @param sourceOps the {@link DynamicOps} instance for the source format; must not be
     *                  {@code null}
     * @param input     the value to convert from the source format; must not be {@code null}
     * @param <U>       the type parameter of the source format
     * @return the converted value as a SnakeYAML native type; returns the {@link #NULL}
     *         sentinel for empty/null source values; never {@code null}
     */
    @NotNull
    @Override
    public <U> Object convertTo(@NotNull final DynamicOps<U> sourceOps,
                                @NotNull final U input) {
        Preconditions.checkNotNull(sourceOps, "sourceOps must not be null");
        Preconditions.checkNotNull(input, "input must not be null");
        // Check primitives via the source ops
        // Boolean first to avoid integer 0/1 being interpreted as boolean
        final DataResult<Boolean> boolResult = sourceOps.getBooleanValue(input);
        if (boolResult.isSuccess()) {
            return createBoolean(boolResult.result().orElseThrow());
        }

        final DataResult<Number> numberResult = sourceOps.getNumberValue(input);
        if (numberResult.isSuccess()) {
            return createNumeric(numberResult.result().orElseThrow());
        }

        final DataResult<String> stringResult = sourceOps.getStringValue(input);
        if (stringResult.isSuccess()) {
            return createString(stringResult.result().orElseThrow());
        }

        // Check list (recursive conversion)
        final DataResult<Stream<U>> listResult = sourceOps.getList(input);
        if (listResult.isSuccess()) {
            return createList(
                    listResult.result().orElseThrow()
                            .map(element -> convertTo(sourceOps, element))
            );
        }

        // Check map (recursive conversion)
        final DataResult<Stream<Pair<U, U>>> mapResult = sourceOps.getMapEntries(input);
        if (mapResult.isSuccess()) {
            return createMap(
                    mapResult.result().orElseThrow()
                            .filter(entry -> entry.first() != null) // Skip entries with null keys
                            .map(entry -> {
                                final U second = entry.second();
                                return Pair.of(
                                        convertTo(sourceOps, entry.first()),
                                        second != null ? convertTo(sourceOps, second) : empty()
                                );
                            })
            );
        }

        // Fallback: return the NULL sentinel for unknown/empty types
        return empty();
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a deep copy of the given value.
     *
     * <p>This method recursively copies all nested {@link Map} and {@link List} instances
     * to ensure complete isolation between the original data structure and the copy. This
     * is essential for maintaining the immutability contract because SnakeYAML uses mutable
     * Java collections.</p>
     *
     * <p><b>Copy Behavior by Type</b></p>
     * <ul>
     *   <li><strong>null:</strong> Returns {@code null}</li>
     *   <li><strong>YamlNull sentinel:</strong> Returns the sentinel as-is (it's a singleton)</li>
     *   <li><strong>Map:</strong> Creates a new {@link LinkedHashMap} with recursively
     *       deep-copied values (keys are assumed to be immutable strings)</li>
     *   <li><strong>List:</strong> Creates a new {@link ArrayList} with recursively
     *       deep-copied elements</li>
     *   <li><strong>Primitives:</strong> Returns the value as-is (String, Number, Boolean
     *       are immutable in Java)</li>
     * </ul>
     *
     * <p><b>Performance Note</b></p>
     * <p>Deep copying has O(n) time and space complexity where n is the total number of
     * elements in the structure. For large data structures, this can be significant.
     * Consider using batch operations ({@link #createMap(Stream)}, {@link #createList(Stream)})
     * to minimize the number of copy operations.</p>
     *
     * @param value the value to copy; may be {@code null} or the {@link #NULL} sentinel
     * @return a deep copy of the value, or the value itself if it is immutable;
     *         {@code null} if the input is {@code null}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    private Object deepCopy(@Nullable final Object value) {
        if (value == null) {
            return null;
        }
        if (value == YamlNull.INSTANCE) {
            return YamlNull.INSTANCE;
        }
        if (value instanceof Map) {
            final Map<String, Object> original = (Map<String, Object>) value;
            final Map<String, Object> copy = new LinkedHashMap<>();
            for (final Map.Entry<String, Object> entry : original.entrySet()) {
                copy.put(entry.getKey(), deepCopy(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List) {
            final List<Object> original = (List<Object>) value;
            final List<Object> copy = new ArrayList<>();
            for (final Object element : original) {
                copy.add(deepCopy(element));
            }
            return copy;
        }
        // Primitives (String, Number, Boolean) are immutable, return as-is
        return value;
    }

    /**
     * Returns a string representation of this {@code DynamicOps} instance.
     *
     * <p>This method returns a fixed string identifying the implementation, useful for
     * debugging, logging, and error messages. The returned value uniquely identifies this
     * implementation among other {@link DynamicOps} implementations in the Aether Datafixers
     * ecosystem.</p>
     *
     * @return the string {@code "SnakeYamlOps"}; never {@code null}
     */
    @Override
    public String toString() {
        return "SnakeYamlOps";
    }

    // ==================== Static Utility Methods ====================

    /**
     * Checks whether the given value is the YAML null sentinel.
     *
     * <p>This method provides a convenient way to check if a value represents YAML's null
     * without directly comparing to {@link #NULL}.</p>
     *
     * @param value the value to check; may be {@code null}
     * @return {@code true} if the value is the YAML null sentinel, {@code false} otherwise
     */
    public static boolean isNull(@Nullable final Object value) {
        return value == YamlNull.INSTANCE;
    }

    /**
     * Recursively converts the YAML null sentinel back to Java {@code null}.
     *
     * <p>This method should be used before serializing data to YAML text format using
     * SnakeYAML, as SnakeYAML expects Java {@code null} for null values, not the sentinel.</p>
     *
     * <p><b>Conversion Behavior</b></p>
     * <ul>
     *   <li>{@link #NULL} sentinel is converted to Java {@code null}</li>
     *   <li>{@link Map} instances are recursively processed (values only, keys are preserved)</li>
     *   <li>{@link List} instances are recursively processed</li>
     *   <li>All other values are returned unchanged</li>
     * </ul>
     *
     * <p><b>Example</b></p>
     * <pre>{@code
     * // Data structure with sentinel values
     * Map<String, Object> data = new LinkedHashMap<>();
     * data.put("name", "Alice");
     * data.put("nickname", SnakeYamlOps.NULL);
     *
     * // Convert for serialization
     * Object unwrapped = SnakeYamlOps.unwrap(data);
     *
     * // Now safe to serialize with SnakeYAML
     * String yaml = new Yaml().dump(unwrapped);
     * // Output: {name: Alice, nickname: null}
     * }</pre>
     *
     * @param value the value to unwrap; may be {@code null}
     * @return the value with all sentinel instances replaced by Java {@code null}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static Object unwrap(@Nullable final Object value) {
        if (value == null || value == YamlNull.INSTANCE) {
            return null;
        }
        if (value instanceof Map) {
            final Map<String, Object> original = (Map<String, Object>) value;
            final Map<String, Object> result = new LinkedHashMap<>();
            for (final Map.Entry<String, Object> entry : original.entrySet()) {
                result.put(entry.getKey(), unwrap(entry.getValue()));
            }
            return result;
        }
        if (value instanceof List) {
            final List<Object> original = (List<Object>) value;
            final List<Object> result = new ArrayList<>();
            for (final Object element : original) {
                result.add(unwrap(element));
            }
            return result;
        }
        return value;
    }

    /**
     * Recursively converts Java {@code null} values to the YAML null sentinel.
     *
     * <p>This method should be used after parsing YAML with SnakeYAML to ensure all null
     * values are represented by the sentinel, making the data safe to use with
     * {@link DynamicOps} methods that require non-null values.</p>
     *
     * <p><b>Conversion Behavior</b></p>
     * <ul>
     *   <li>Java {@code null} is converted to {@link #NULL} sentinel</li>
     *   <li>{@link Map} instances are recursively processed (values only, keys are preserved)</li>
     *   <li>{@link List} instances are recursively processed</li>
     *   <li>All other values are returned unchanged</li>
     * </ul>
     *
     * <p><b>Example</b></p>
     * <pre>{@code
     * // Parse YAML with SnakeYAML
     * Yaml yaml = new Yaml();
     * Object parsed = yaml.load("name: Alice\nnickname: null");
     *
     * // Wrap null values for use with DynamicOps
     * Object wrapped = SnakeYamlOps.wrap(parsed);
     *
     * // Now safe to use with Dynamic
     * Dynamic<Object> dynamic = new Dynamic<>(SnakeYamlOps.INSTANCE, wrapped);
     * }</pre>
     *
     * @param value the value to wrap; may be {@code null}
     * @return the value with all Java {@code null} instances replaced by the sentinel;
     *         never {@code null} (returns {@link #NULL} if input is {@code null})
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static Object wrap(@Nullable final Object value) {
        if (value == null) {
            return YamlNull.INSTANCE;
        }
        if (value instanceof Map) {
            final Map<String, Object> original = (Map<String, Object>) value;
            final Map<String, Object> result = new LinkedHashMap<>();
            for (final Map.Entry<String, Object> entry : original.entrySet()) {
                result.put(entry.getKey(), wrap(entry.getValue()));
            }
            return result;
        }
        if (value instanceof List) {
            final List<Object> original = (List<Object>) value;
            final List<Object> result = new ArrayList<>();
            for (final Object element : original) {
                result.add(wrap(element));
            }
            return result;
        }
        return value;
    }

    // ==================== Inner Classes ====================

    /**
     * Sentinel class representing the YAML null value.
     *
     * <p>This is a singleton class used to represent YAML's null value in a way that
     * satisfies the {@link DynamicOps} contract (which requires non-null return values).
     * The single instance is accessible via {@link SnakeYamlOps#NULL}.</p>
     *
     * <p>This class is intentionally package-private and should not be instantiated
     * or subclassed outside of {@link SnakeYamlOps}.</p>
     */
    static final class YamlNull {
        /**
         * The singleton instance of the YAML null sentinel.
         */
        static final YamlNull INSTANCE = new YamlNull();

        /**
         * Private constructor to enforce singleton pattern.
         */
        private YamlNull() {
            // Singleton
        }

        /**
         * Returns a string representation of this null sentinel.
         *
         * @return the string {@code "null"}
         */
        @Override
        public String toString() {
            return "null";
        }
    }
}
