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

import de.splatgames.aether.datafixers.api.result.DataResult;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * AssertJ assertions for {@link DataResult} objects.
 *
 * <p>Provides fluent assertions for verifying DataResult success/error states,
 * values, error messages, and partial results.</p>
 *
 * <h2>Success Assertions</h2>
 * <pre>{@code
 * assertThat(result)
 *     .isSuccess()
 *     .hasValue(expectedValue);
 *
 * assertThat(result)
 *     .isSuccess()
 *     .hasValueSatisfying(value -> {
 *         assertThat(value.name()).isEqualTo("Alice");
 *         assertThat(value.age()).isEqualTo(30);
 *     });
 * }</pre>
 *
 * <h2>Error Assertions</h2>
 * <pre>{@code
 * assertThat(result)
 *     .isError()
 *     .hasErrorMessage("Field 'name' is required");
 *
 * assertThat(result)
 *     .isError()
 *     .hasErrorMessageContaining("required")
 *     .hasErrorMessageStartingWith("Field");
 * }</pre>
 *
 * <h2>Partial Result Assertions</h2>
 * <pre>{@code
 * assertThat(result)
 *     .isError()
 *     .hasPartialResult()
 *     .hasPartialValue(defaultValue);
 * }</pre>
 *
 * <h2>Extracting for Further Assertions</h2>
 * <pre>{@code
 * assertThat(result)
 *     .isSuccess()
 *     .extractingValue()
 *     .isInstanceOf(Player.class);
 *
 * assertThat(result)
 *     .isError()
 *     .extractingError()
 *     .contains("validation failed");
 * }</pre>
 *
 * @param <A> the result value type
 * @author Erik Pförtner
 * @see AetherAssertions
 * @since 0.2.0
 */
public final class DataResultAssert<A> extends AbstractAssert<DataResultAssert<A>, DataResult<A>> {

    /**
     * Creates a new assertion for the given DataResult.
     *
     * @param actual the DataResult to assert on
     */
    public DataResultAssert(final DataResult<A> actual) {
        super(actual, DataResultAssert.class);
    }

    // ==================== Status Assertions ====================

    /**
     * Asserts that the DataResult is a success.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> isSuccess() {
        isNotNull();
        if (!this.actual.isSuccess()) {
            failWithMessage("Expected success but was error: %s",
                    this.actual.error().orElse("unknown error"));
        }
        return this;
    }

    /**
     * Asserts that the DataResult is an error.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> isError() {
        isNotNull();
        if (!this.actual.isError()) {
            failWithMessage("Expected error but was success with value: %s",
                    this.actual.result().orElse(null));
        }
        return this;
    }

    /**
     * Asserts that the DataResult has a partial result.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasPartialResult() {
        isNotNull();
        if (this.actual.partialResult().isEmpty()) {
            failWithMessage("Expected partial result but none was present");
        }
        return this;
    }

    /**
     * Asserts that the DataResult does NOT have a partial result.
     *
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasNoPartialResult() {
        isNotNull();
        if (this.actual.partialResult().isPresent()) {
            failWithMessage("Expected no partial result but found: %s",
                    this.actual.partialResult().get());
        }
        return this;
    }

    // ==================== Value Assertions ====================

    /**
     * Asserts that the DataResult has the expected value.
     *
     * <p>This implicitly asserts that the result is a success.</p>
     *
     * @param expected the expected value
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasValue(@NotNull final A expected) {
        this.isSuccess();
        final A actualValue = this.actual.result().orElse(null);
        if (!Objects.equals(expected, actualValue)) {
            failWithMessage("Expected value:%n  %s%nbut was:%n  %s",
                    expected, actualValue);
        }
        return this;
    }

    /**
     * Asserts that the DataResult value satisfies the given requirements.
     *
     * <p>This implicitly asserts that the result is a success.</p>
     *
     * @param requirements the validation consumer
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasValueSatisfying(@NotNull final Consumer<A> requirements) {
        this.isSuccess();
        requirements.accept(this.actual.result().orElse(null));
        return this;
    }

    /**
     * Asserts that the partial result equals the expected value.
     *
     * <p>This implicitly asserts that a partial result is present.</p>
     *
     * @param expected the expected partial value
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasPartialValue(@NotNull final A expected) {
        this.hasPartialResult();
        final A actualPartial = this.actual.partialResult().orElse(null);
        if (!Objects.equals(expected, actualPartial)) {
            failWithMessage("Expected partial value:%n  %s%nbut was:%n  %s",
                    expected, actualPartial);
        }
        return this;
    }

    // ==================== Error Message Assertions ====================

    /**
     * Asserts that the error message equals the expected message.
     *
     * <p>This implicitly asserts that the result is an error.</p>
     *
     * @param expected the expected error message
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasErrorMessage(@NotNull final String expected) {
        this.isError();
        final String actualMessage = this.actual.error().orElse(null);
        if (!Objects.equals(expected, actualMessage)) {
            failWithMessage("Expected error message:%n  \"%s\"%nbut was:%n  \"%s\"",
                    expected, actualMessage);
        }
        return this;
    }

    /**
     * Asserts that the error message contains the expected substring.
     *
     * <p>This implicitly asserts that the result is an error.</p>
     *
     * @param substring the expected substring
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasErrorMessageContaining(@NotNull final String substring) {
        this.isError();
        final String actualMessage = this.actual.error().orElse("");
        if (!actualMessage.contains(substring)) {
            failWithMessage("Expected error message to contain \"%s\" but was: \"%s\"",
                    substring, actualMessage);
        }
        return this;
    }

    /**
     * Asserts that the error message starts with the expected prefix.
     *
     * <p>This implicitly asserts that the result is an error.</p>
     *
     * @param prefix the expected prefix
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasErrorMessageStartingWith(@NotNull final String prefix) {
        this.isError();
        final String actualMessage = this.actual.error().orElse("");
        if (!actualMessage.startsWith(prefix)) {
            failWithMessage("Expected error message to start with \"%s\" but was: \"%s\"",
                    prefix, actualMessage);
        }
        return this;
    }

    /**
     * Asserts that the error message ends with the expected suffix.
     *
     * <p>This implicitly asserts that the result is an error.</p>
     *
     * @param suffix the expected suffix
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasErrorMessageEndingWith(@NotNull final String suffix) {
        this.isError();
        final String actualMessage = this.actual.error().orElse("");
        if (!actualMessage.endsWith(suffix)) {
            failWithMessage("Expected error message to end with \"%s\" but was: \"%s\"",
                    suffix, actualMessage);
        }
        return this;
    }

    /**
     * Asserts that the error message matches the expected regex pattern.
     *
     * <p>This implicitly asserts that the result is an error.</p>
     *
     * @param regex the regex pattern
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> hasErrorMessageMatching(@NotNull final String regex) {
        this.isError();
        final String actualMessage = this.actual.error().orElse("");
        if (!actualMessage.matches(regex)) {
            failWithMessage("Expected error message to match pattern \"%s\" but was: \"%s\"",
                    regex, actualMessage);
        }
        return this;
    }

    // ==================== Extraction ====================

    /**
     * Extracts the value for further assertions.
     *
     * <p>This implicitly asserts that the result is a success.</p>
     *
     * @return an ObjectAssert for the value
     */
    @NotNull
    public AbstractObjectAssert<?, A> extractingValue() {
        this.isSuccess();
        return Assertions.assertThat(this.actual.result().orElse(null));
    }

    /**
     * Extracts the error message for further assertions.
     *
     * <p>This implicitly asserts that the result is an error.</p>
     *
     * @return a CharSequenceAssert for the error message
     */
    @NotNull
    public AbstractCharSequenceAssert<?, String> extractingError() {
        this.isError();
        return Assertions.assertThat(this.actual.error().orElse(null));
    }

    /**
     * Extracts the partial result for further assertions.
     *
     * <p>This implicitly asserts that a partial result is present.</p>
     *
     * @return an ObjectAssert for the partial value
     */
    @NotNull
    public AbstractObjectAssert<?, A> extractingPartial() {
        this.hasPartialResult();
        return Assertions.assertThat(this.actual.partialResult().orElse(null));
    }

    // ==================== Utility ====================

    /**
     * Applies custom validation using a consumer.
     *
     * @param requirements the validation consumer
     * @return this assertion for chaining
     */
    @NotNull
    public DataResultAssert<A> satisfies(@NotNull final Consumer<DataResult<A>> requirements) {
        isNotNull();
        requirements.accept(this.actual);
        return this;
    }
}
