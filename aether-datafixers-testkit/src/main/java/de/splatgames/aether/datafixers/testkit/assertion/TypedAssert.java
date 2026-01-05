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

import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * AssertJ assertions for {@link Typed} objects.
 *
 * <p>Provides fluent assertions for verifying Typed structures including
 * type checks, value assertions, and encoding operations.</p>
 *
 * <h2>Type Assertions</h2>
 * <pre>{@code
 * assertThat(typed)
 *     .hasType(expectedType)
 *     .hasTypeReference(TypeReference.of("player"));
 * }</pre>
 *
 * <h2>Value Assertions</h2>
 * <pre>{@code
 * assertThat(typed)
 *     .hasValue(expectedValue)
 *     .hasValueSatisfying(value -> {
 *         assertThat(value.name()).isEqualTo("Alice");
 *         assertThat(value.level()).isEqualTo(10);
 *     });
 * }</pre>
 *
 * <h2>Encoding Assertions</h2>
 * <pre>{@code
 * // Encode and assert on the Dynamic result
 * assertThat(typed)
 *     .encodedWith(GsonOps.INSTANCE)
 *     .hasField("name")
 *     .hasStringField("name", "Alice");
 * }</pre>
 *
 * @param <A> the typed value type
 * @author Erik Pförtner
 * @see AetherAssertions
 * @since 0.2.0
 */
public final class TypedAssert<A> extends AbstractAssert<TypedAssert<A>, Typed<A>> {

    /**
     * Creates a new assertion for the given Typed.
     *
     * @param actual the Typed to assert on
     */
    public TypedAssert(final Typed<A> actual) {
        super(actual, TypedAssert.class);
    }

    // ==================== Type Assertions ====================

    /**
     * Asserts that the Typed has the expected type.
     *
     * @param expected the expected type
     * @return this assertion for chaining
     */
    @NotNull
    public TypedAssert<A> hasType(@NotNull final Type<A> expected) {
        isNotNull();
        if (!Objects.equals(this.actual.type(), expected)) {
            failWithMessage("Expected type:%n  %s%nbut was:%n  %s",
                    expected.describe(), this.actual.type().describe());
        }
        return this;
    }

    /**
     * Asserts that the Typed has the expected type reference.
     *
     * @param expected the expected type reference
     * @return this assertion for chaining
     */
    @NotNull
    public TypedAssert<A> hasTypeReference(@NotNull final TypeReference expected) {
        isNotNull();
        final TypeReference actualRef = this.actual.type().reference();
        if (!Objects.equals(actualRef, expected)) {
            failWithMessage("Expected type reference:%n  %s%nbut was:%n  %s",
                    expected, actualRef);
        }
        return this;
    }

    /**
     * Asserts that the Typed has a type reference matching the given id.
     *
     * @param typeId the expected type reference id
     * @return this assertion for chaining
     */
    @NotNull
    public TypedAssert<A> hasTypeReference(@NotNull final String typeId) {
        return this.hasTypeReference(new TypeReference(typeId));
    }

    /**
     * Asserts that the Typed has the expected type description.
     *
     * @param expected the expected type description
     * @return this assertion for chaining
     */
    @NotNull
    public TypedAssert<A> hasTypeDescription(@NotNull final String expected) {
        isNotNull();
        final String actualDescription = this.actual.type().describe();
        if (!Objects.equals(expected, actualDescription)) {
            failWithMessage("Expected type description:%n  \"%s\"%nbut was:%n  \"%s\"",
                    expected, actualDescription);
        }
        return this;
    }

    // ==================== Value Assertions ====================

    /**
     * Asserts that the Typed has the expected value.
     *
     * @param expected the expected value
     * @return this assertion for chaining
     */
    @NotNull
    public TypedAssert<A> hasValue(@NotNull final A expected) {
        isNotNull();
        if (!Objects.equals(this.actual.value(), expected)) {
            failWithMessage("Expected value:%n  %s%nbut was:%n  %s",
                    expected, this.actual.value());
        }
        return this;
    }

    /**
     * Asserts that the Typed value satisfies the given requirements.
     *
     * @param requirements the validation consumer
     * @return this assertion for chaining
     */
    @NotNull
    public TypedAssert<A> hasValueSatisfying(@NotNull final Consumer<A> requirements) {
        isNotNull();
        requirements.accept(this.actual.value());
        return this;
    }

    /**
     * Asserts that the Typed value is an instance of the given class.
     *
     * @param expectedClass the expected class
     * @return this assertion for chaining
     */
    @NotNull
    public TypedAssert<A> hasValueInstanceOf(@NotNull final Class<?> expectedClass) {
        isNotNull();
        if (!expectedClass.isInstance(this.actual.value())) {
            failWithMessage("Expected value to be instance of %s but was: %s (%s)",
                    expectedClass.getName(),
                    this.actual.value(),
                    this.actual.value().getClass().getName());
        }
        return this;
    }

    // ==================== Encoding ====================

    /**
     * Encodes the Typed value and returns a DynamicAssert for further assertions.
     *
     * <pre>{@code
     * assertThat(typed)
     *     .encodedWith(GsonOps.INSTANCE)
     *     .hasField("name")
     *     .hasStringField("name", "Alice");
     * }</pre>
     *
     * @param ops the DynamicOps to use for encoding
     * @param <T> the underlying value type
     * @return a new DynamicAssert for the encoded value
     * @throws AssertionError if encoding fails
     */
    @NotNull
    public <T> DynamicAssert<T> encodedWith(@NotNull final DynamicOps<T> ops) {
        isNotNull();
        final Dynamic<T> encoded = this.actual.encode(ops)
                .getOrThrow(msg -> new AssertionError("Failed to encode Typed value: " + msg));
        return new DynamicAssert<>(encoded);
    }

    // ==================== Extraction ====================

    /**
     * Extracts the value for further assertions with standard AssertJ.
     *
     * @return an ObjectAssert for the value
     */
    @NotNull
    public AbstractObjectAssert<?, A> extractingValue() {
        isNotNull();
        return Assertions.assertThat(this.actual.value());
    }

    /**
     * Extracts the type for further assertions.
     *
     * @return an ObjectAssert for the type
     */
    @NotNull
    public AbstractObjectAssert<?, Type<A>> extractingType() {
        isNotNull();
        return Assertions.assertThat(this.actual.type());
    }

    // ==================== Utility ====================

    /**
     * Applies custom validation using a consumer.
     *
     * @param requirements the validation consumer
     * @return this assertion for chaining
     */
    @NotNull
    public TypedAssert<A> satisfies(@NotNull final Consumer<Typed<A>> requirements) {
        isNotNull();
        requirements.accept(this.actual);
        return this;
    }
}
