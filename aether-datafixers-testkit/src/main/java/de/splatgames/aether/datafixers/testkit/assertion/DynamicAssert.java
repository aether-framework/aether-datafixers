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

package de.splatgames.aether.datafixers.testkit.assertion;

import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import org.assertj.core.api.AbstractAssert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * AssertJ assertions for {@link Dynamic} objects.
 *
 * <p>Provides fluent assertions for verifying Dynamic structures including
 * type checks, field existence, value assertions, and nested navigation.</p>
 *
 * <h2>Type Assertions</h2>
 * <pre>{@code
 * assertThat(dynamic).isMap();
 * assertThat(dynamic).isList();
 * assertThat(dynamic).isString();
 * assertThat(dynamic).isNumber();
 * assertThat(dynamic).isBoolean();
 * }</pre>
 *
 * <h2>Field Assertions</h2>
 * <pre>{@code
 * assertThat(dynamic)
 *     .hasField("name")
 *     .hasFields("name", "age", "active")
 *     .doesNotHaveField("deleted")
 *     .hasOnlyFields("name", "age");
 * }</pre>
 *
 * <h2>Value Assertions</h2>
 * <pre>{@code
 * assertThat(dynamic)
 *     .hasStringField("name", "Alice")
 *     .hasIntField("age", 30)
 *     .hasBooleanField("active", true)
 *     .hasDoubleField("score", 95.5, 0.01);
 * }</pre>
 *
 * <h2>Navigation</h2>
 * <pre>{@code
 * // Navigate to nested field
 * assertThat(dynamic)
 *     .field("position")
 *         .hasIntField("x", 100);
 *
 * // Path-based navigation
 * assertThat(dynamic)
 *     .atPath("user.address.city")
 *     .hasStringValue("Berlin");
 *
 * // List index navigation
 * assertThat(dynamic)
 *     .field("items")
 *     .atIndex(0)
 *     .hasStringField("id", "sword");
 * }</pre>
 *
 * <h2>List Assertions</h2>
 * <pre>{@code
 * assertThat(dynamic)
 *     .hasSize(3)
 *     .isEmpty()
 *     .isNotEmpty()
 *     .containsStringValues("a", "b", "c")
 *     .containsIntValues(1, 2, 3);
 * }</pre>
 *
 * @param <T> the underlying value type
 * @author Erik Pförtner
 * @see AetherAssertions
 * @since 0.2.0
 */
public final class DynamicAssert<T> extends AbstractAssert<DynamicAssert<T>, Dynamic<T>> {

    private final String path;

    /**
     * Creates a new assertion for the given Dynamic.
     *
     * @param actual the Dynamic to assert on
     */
    public DynamicAssert(final Dynamic<T> actual) {
        this(actual, "");
    }

    private DynamicAssert(final Dynamic<T> actual, final String path) {
        super(actual, DynamicAssert.class);
        this.path = path;
    }

    // ==================== Type Assertions ====================

    /**
     * Asserts that the Dynamic is a map/object.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> isMap() {
        isNotNull();
        if (!this.actual.isMap()) {
            failWithMessage("Expected%s to be a map but was: %s",
                    this.pathInfo(), this.describeActual());
        }
        return this;
    }

    /**
     * Asserts that the Dynamic is a list/array.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> isList() {
        isNotNull();
        if (!this.actual.isList()) {
            failWithMessage("Expected%s to be a list but was: %s",
                    this.pathInfo(), this.describeActual());
        }
        return this;
    }

    /**
     * Asserts that the Dynamic is a string.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> isString() {
        isNotNull();
        if (!this.actual.isString()) {
            failWithMessage("Expected%s to be a string but was: %s",
                    this.pathInfo(), this.describeActual());
        }
        return this;
    }

    /**
     * Asserts that the Dynamic is a number.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> isNumber() {
        isNotNull();
        if (!this.actual.isNumber()) {
            failWithMessage("Expected%s to be a number but was: %s",
                    this.pathInfo(), this.describeActual());
        }
        return this;
    }

    /**
     * Asserts that the Dynamic is a boolean.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> isBoolean() {
        isNotNull();
        if (!this.actual.isBoolean()) {
            failWithMessage("Expected%s to be a boolean but was: %s",
                    this.pathInfo(), this.describeActual());
        }
        return this;
    }

    // ==================== Field Existence ====================

    /**
     * Asserts that the Dynamic has a field with the given key.
     *
     * @param key the field name
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasField(@NotNull final String key) {
        isNotNull();
        if (!this.actual.has(key)) {
            failWithMessage("Expected%s to have field '%s' but it was not found. Available fields: %s",
                    this.pathInfo(), key, this.availableFields());
        }
        return this;
    }

    /**
     * Asserts that the Dynamic does NOT have a field with the given key.
     *
     * @param key the field name
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> doesNotHaveField(@NotNull final String key) {
        isNotNull();
        if (this.actual.has(key)) {
            failWithMessage("Expected%s to NOT have field '%s' but it was found with value: %s",
                    this.pathInfo(), key, this.actual.get(key));
        }
        return this;
    }

    /**
     * Asserts that the Dynamic has all specified fields.
     *
     * @param keys the field names
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasFields(@NotNull final String... keys) {
        isNotNull();
        for (final String key : keys) {
            if (!this.actual.has(key)) {
                failWithMessage("Expected%s to have field '%s' but it was not found. Available fields: %s",
                        this.pathInfo(), key, this.availableFields());
            }
        }
        return this;
    }

    /**
     * Asserts that the Dynamic has ONLY the specified fields.
     *
     * @param keys the field names
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasOnlyFields(@NotNull final String... keys) {
        isNotNull();
        final List<String> actualFields = this.availableFieldsList();
        final List<String> expectedFields = List.of(keys);

        for (final String expected : expectedFields) {
            if (!actualFields.contains(expected)) {
                failWithMessage("Expected%s to have field '%s' but it was not found. Available fields: %s",
                        this.pathInfo(), expected, actualFields);
            }
        }

        for (final String actualField : actualFields) {
            if (!expectedFields.contains(actualField)) {
                failWithMessage("Expected%s to have only fields %s but found unexpected field '%s'",
                        this.pathInfo(), expectedFields, actualField);
            }
        }
        return this;
    }

    // ==================== Field Value Assertions ====================

    /**
     * Asserts that a field has a specific string value.
     *
     * @param key      the field name
     * @param expected the expected value
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasStringField(@NotNull final String key, @NotNull final String expected) {
        this.hasField(key);
        final Dynamic<T> fieldValue = this.actual.get(key);
        final String actualValue = fieldValue.asString().orElse(null);
        if (!Objects.equals(expected, actualValue)) {
            failWithMessage("Expected%s field '%s' to be '%s' but was '%s'",
                    this.pathInfo(), key, expected, actualValue);
        }
        return this;
    }

    /**
     * Asserts that a field has a specific integer value.
     *
     * @param key      the field name
     * @param expected the expected value
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasIntField(@NotNull final String key, final int expected) {
        this.hasField(key);
        final Dynamic<T> fieldValue = this.actual.get(key);
        final Integer actualValue = fieldValue.asInt().orElse(null);
        if (!Objects.equals(expected, actualValue)) {
            failWithMessage("Expected%s field '%s' to be %d but was %s",
                    this.pathInfo(), key, expected, actualValue);
        }
        return this;
    }

    /**
     * Asserts that a field has a specific long value.
     *
     * @param key      the field name
     * @param expected the expected value
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasLongField(@NotNull final String key, final long expected) {
        this.hasField(key);
        final Dynamic<T> fieldValue = this.actual.get(key);
        final Long actualValue = fieldValue.asLong().orElse(null);
        if (!Objects.equals(expected, actualValue)) {
            failWithMessage("Expected%s field '%s' to be %d but was %s",
                    this.pathInfo(), key, expected, actualValue);
        }
        return this;
    }

    /**
     * Asserts that a field has a specific double value within epsilon.
     *
     * @param key      the field name
     * @param expected the expected value
     * @param epsilon  the tolerance
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasDoubleField(@NotNull final String key, final double expected, final double epsilon) {
        this.hasField(key);
        final Dynamic<T> fieldValue = this.actual.get(key);
        final Double actualValue = fieldValue.asDouble().orElse(null);
        if (actualValue == null || Math.abs(expected - actualValue) > epsilon) {
            failWithMessage("Expected%s field '%s' to be %f (±%f) but was %s",
                    this.pathInfo(), key, expected, epsilon, actualValue);
        }
        return this;
    }

    /**
     * Asserts that a field has a specific boolean value.
     *
     * @param key      the field name
     * @param expected the expected value
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasBooleanField(@NotNull final String key, final boolean expected) {
        this.hasField(key);
        final Dynamic<T> fieldValue = this.actual.get(key);
        final Boolean actualValue = fieldValue.asBoolean().orElse(null);
        if (!Objects.equals(expected, actualValue)) {
            failWithMessage("Expected%s field '%s' to be %b but was %s",
                    this.pathInfo(), key, expected, actualValue);
        }
        return this;
    }

    // ==================== Direct Value Assertions ====================

    /**
     * Asserts that this Dynamic has the given string value.
     *
     * @param expected the expected string
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasStringValue(@NotNull final String expected) {
        isNotNull();
        final String actualValue = this.actual.asString().orElse(null);
        if (!Objects.equals(expected, actualValue)) {
            failWithMessage("Expected%s to be '%s' but was '%s'",
                    this.pathInfo(), expected, actualValue);
        }
        return this;
    }

    /**
     * Asserts that this Dynamic has the given integer value.
     *
     * @param expected the expected integer
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasIntValue(final int expected) {
        isNotNull();
        final Integer actualValue = this.actual.asInt().orElse(null);
        if (!Objects.equals(expected, actualValue)) {
            failWithMessage("Expected%s to be %d but was %s",
                    this.pathInfo(), expected, actualValue);
        }
        return this;
    }

    /**
     * Asserts that this Dynamic has the given long value.
     *
     * @param expected the expected long
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasLongValue(final long expected) {
        isNotNull();
        final Long actualValue = this.actual.asLong().orElse(null);
        if (!Objects.equals(expected, actualValue)) {
            failWithMessage("Expected%s to be %d but was %s",
                    this.pathInfo(), expected, actualValue);
        }
        return this;
    }

    /**
     * Asserts that this Dynamic has the given double value within epsilon.
     *
     * @param expected the expected double
     * @param epsilon  the tolerance
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasDoubleValue(final double expected, final double epsilon) {
        isNotNull();
        final Double actualValue = this.actual.asDouble().orElse(null);
        if (actualValue == null || Math.abs(expected - actualValue) > epsilon) {
            failWithMessage("Expected%s to be %f (±%f) but was %s",
                    this.pathInfo(), expected, epsilon, actualValue);
        }
        return this;
    }

    /**
     * Asserts that this Dynamic has the given boolean value.
     *
     * @param expected the expected boolean
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasBooleanValue(final boolean expected) {
        isNotNull();
        final Boolean actualValue = this.actual.asBoolean().orElse(null);
        if (!Objects.equals(expected, actualValue)) {
            failWithMessage("Expected%s to be %b but was %s",
                    this.pathInfo(), expected, actualValue);
        }
        return this;
    }

    // ==================== Navigation ====================

    /**
     * Navigates to a field and returns an assertion for it.
     *
     * @param key the field name
     * @return a new assertion for the field value
     */
    @NotNull
    public DynamicAssert<T> field(@NotNull final String key) {
        isNotNull();
        this.hasField(key);
        final String newPath = this.path.isEmpty() ? key : this.path + "." + key;
        return new DynamicAssert<>(this.actual.get(key), newPath);
    }

    /**
     * Navigates to a nested field using dot-separated path.
     *
     * <pre>{@code
     * assertThat(data).atPath("user.address.city").hasStringValue("Berlin");
     * }</pre>
     *
     * @param dotPath the dot-separated path
     * @return a new assertion for the value at the path
     */
    @NotNull
    public DynamicAssert<T> atPath(@NotNull final String dotPath) {
        isNotNull();
        final String[] parts = dotPath.split("\\.");
        Dynamic<T> current = this.actual;
        final StringBuilder currentPath = new StringBuilder(this.path);

        for (final String part : parts) {
            if (!current.has(part)) {
                failWithMessage("Path '%s' not found: field '%s' does not exist at '%s'. Available fields: %s",
                        dotPath, part, currentPath, this.fieldsOf(current));
            }
            current = current.get(part);
            if (currentPath.length() > 0) {
                currentPath.append(".");
            }
            currentPath.append(part);
        }
        return new DynamicAssert<>(current, currentPath.toString());
    }

    /**
     * Navigates to a list element by index.
     *
     * @param index the list index (0-based)
     * @return a new assertion for the element
     */
    @NotNull
    public DynamicAssert<T> atIndex(final int index) {
        isNotNull();
        this.isList();
        final List<Dynamic<T>> elements = this.actual.asListStream()
                .result()
                .map(s -> s.collect(Collectors.toList()))
                .orElse(List.of());

        if (index < 0 || index >= elements.size()) {
            failWithMessage("Index %d out of bounds for list%s with size %d",
                    index, this.pathInfo(), elements.size());
        }

        final String newPath = this.path.isEmpty() ? "[" + index + "]" : this.path + "[" + index + "]";
        return new DynamicAssert<>(elements.get(index), newPath);
    }

    // ==================== List Assertions ====================

    /**
     * Asserts that the Dynamic list has the expected size.
     *
     * @param expected the expected size
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> hasSize(final int expected) {
        isNotNull();
        this.isList();
        final int actualSize = this.actual.asListStream()
                .result()
                .map(s -> (int) s.count())
                .orElse(0);

        if (actualSize != expected) {
            failWithMessage("Expected%s to have size %d but was %d",
                    this.pathInfo(), expected, actualSize);
        }
        return this;
    }

    /**
     * Asserts that the Dynamic list is empty.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> isEmpty() {
        return this.hasSize(0);
    }

    /**
     * Asserts that the Dynamic list is not empty.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> isNotEmpty() {
        isNotNull();
        this.isList();
        final long size = this.actual.asListStream()
                .result()
                .map(s -> s.count())
                .orElse(0L);

        if (size == 0) {
            failWithMessage("Expected%s to not be empty", this.pathInfo());
        }
        return this;
    }

    /**
     * Asserts that the Dynamic list contains the expected string values.
     *
     * @param expected the expected values
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> containsStringValues(@NotNull final String... expected) {
        isNotNull();
        this.isList();
        final List<String> actualValues = this.actual.asListStream()
                .result()
                .map(s -> s.map(d -> d.asString().orElse(null)).collect(Collectors.toList()))
                .orElse(List.of());

        for (final String exp : expected) {
            if (!actualValues.contains(exp)) {
                failWithMessage("Expected%s to contain '%s' but values were: %s",
                        this.pathInfo(), exp, actualValues);
            }
        }
        return this;
    }

    /**
     * Asserts that the Dynamic list contains the expected integer values.
     *
     * @param expected the expected values
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> containsIntValues(final int... expected) {
        isNotNull();
        this.isList();
        final List<Integer> actualValues = this.actual.asListStream()
                .result()
                .map(s -> s.map(d -> d.asInt().orElse(null)).collect(Collectors.toList()))
                .orElse(List.of());

        for (final int exp : expected) {
            if (!actualValues.contains(exp)) {
                failWithMessage("Expected%s to contain %d but values were: %s",
                        this.pathInfo(), exp, actualValues);
            }
        }
        return this;
    }

    // ==================== Equality ====================

    /**
     * Asserts that this Dynamic equals the expected Dynamic.
     *
     * @param expected the expected Dynamic
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> isEqualTo(@NotNull final Dynamic<T> expected) {
        isNotNull();
        if (!Objects.equals(this.actual.value(), expected.value())) {
            failWithMessage("Expected%s to be equal to:%n  %s%nbut was:%n  %s",
                    this.pathInfo(), expected.value(), this.actual.value());
        }
        return this;
    }

    // ==================== Custom Validation ====================

    /**
     * Applies custom validation using a consumer.
     *
     * @param requirements the validation consumer
     * @return this assertion for chaining
     */
    @NotNull
    public DynamicAssert<T> satisfies(@NotNull final Consumer<Dynamic<T>> requirements) {
        isNotNull();
        requirements.accept(this.actual);
        return this;
    }

    // ==================== Internal Helpers ====================

    private String pathInfo() {
        return this.path.isEmpty() ? "" : " at '" + this.path + "'";
    }

    private String describeActual() {
        if (this.actual.isMap()) return "map";
        if (this.actual.isList()) return "list";
        if (this.actual.isString()) return "string: " + this.actual.asString().orElse("?");
        if (this.actual.isNumber()) return "number: " + this.actual.asNumber().orElse(null);
        if (this.actual.isBoolean()) return "boolean: " + this.actual.asBoolean().orElse(null);
        return "unknown: " + this.actual.value();
    }

    private List<String> availableFieldsList() {
        return this.actual.asMapStream()
                .result()
                .map(s -> s.map(p -> p.first().asString().orElse("?")).collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Nullable
    private String availableFields() {
        final List<String> fields = this.availableFieldsList();
        return fields.isEmpty() ? "(none)" : String.join(", ", fields);
    }

    private String fieldsOf(final Dynamic<T> d) {
        return d.asMapStream()
                .result()
                .map(s -> s.map(p -> p.first().asString().orElse("?")).collect(Collectors.joining(", ")))
                .orElse("(none)");
    }
}
