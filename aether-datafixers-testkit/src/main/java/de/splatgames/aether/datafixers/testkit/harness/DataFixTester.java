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

package de.splatgames.aether.datafixers.testkit.harness;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import de.splatgames.aether.datafixers.testkit.context.AssertingContext;
import de.splatgames.aether.datafixers.testkit.context.RecordingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A fluent test harness for testing individual {@link DataFix} implementations.
 *
 * <p>{@code DataFixTester} provides a clean API for testing DataFix implementations
 * in isolation, without needing to set up a full DataFixer with schemas and registries.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * DataFixTester.forFix(myFix)
 *     .withInput(inputDynamic)
 *     .forType("player")
 *     .expectOutput(expectedDynamic)
 *     .verify();
 * }</pre>
 *
 * <h2>Getting the Result for Custom Assertions</h2>
 * <pre>{@code
 * Dynamic<?> result = DataFixTester.forFix(myFix)
 *     .withInput(inputDynamic)
 *     .forType("player")
 *     .apply();
 *
 * assertThat(result)
 *     .hasField("name")
 *     .hasStringField("name", "Alice");
 * }</pre>
 *
 * <h2>Testing with Context</h2>
 * <pre>{@code
 * // Record all context calls
 * DataFixVerification<?> verification = DataFixTester.forFix(myFix)
 *     .withInput(inputDynamic)
 *     .forType("player")
 *     .recordingContext()
 *     .verify();
 *
 * assertThat(verification.context().infoLogs()).hasSize(1);
 * verification.context().assertNoWarnings();
 *
 * // Or fail immediately on warnings
 * DataFixTester.forFix(myFix)
 *     .withInput(inputDynamic)
 *     .forType("player")
 *     .failOnWarning()
 *     .apply();
 * }</pre>
 *
 * @param <T> the underlying value type (e.g., JsonElement)
 * @author Erik Pförtner
 * @see DataFixVerification
 * @see RecordingContext
 * @since 0.2.0
 */
public final class DataFixTester<T> {

    private final DataFix<T> fix;
    private Dynamic<T> input;
    private TypeReference typeReference;
    private DataFixerContext context;
    private Dynamic<T> expectedOutput;
    private boolean useRecordingContext;

    private DataFixTester(@NotNull final DataFix<T> fix) {
        this.fix = Preconditions.checkNotNull(fix, "fix must not be null");
        this.context = AssertingContext.silent();
        this.useRecordingContext = false;
    }

    /**
     * Creates a new tester for the given {@link DataFix}.
     *
     * @param fix the DataFix to test
     * @param <T> the underlying value type
     * @return a new DataFixTester
     * @throws NullPointerException if {@code fix} is null
     */
    @NotNull
    public static <T> DataFixTester<T> forFix(@NotNull final DataFix<T> fix) {
        Preconditions.checkNotNull(fix, "fix must not be null");
        return new DataFixTester<>(fix);
    }

    // ==================== Configuration ====================

    /**
     * Sets the input data for the fix.
     *
     * @param input the input Dynamic
     * @return this tester for chaining
     * @throws NullPointerException if {@code input} is null
     */
    @NotNull
    public DataFixTester<T> withInput(@NotNull final Dynamic<T> input) {
        this.input = Preconditions.checkNotNull(input, "input must not be null");
        return this;
    }

    /**
     * Sets the type reference for the fix.
     *
     * @param type the TypeReference
     * @return this tester for chaining
     * @throws NullPointerException if {@code type} is null
     */
    @NotNull
    public DataFixTester<T> forType(@NotNull final TypeReference type) {
        this.typeReference = Preconditions.checkNotNull(type, "type must not be null");
        return this;
    }

    /**
     * Sets the type reference using a string id.
     *
     * @param typeId the type reference id
     * @return this tester for chaining
     * @throws NullPointerException if {@code typeId} is null
     */
    @NotNull
    public DataFixTester<T> forType(@NotNull final String typeId) {
        Preconditions.checkNotNull(typeId, "typeId must not be null");
        return this.forType(new TypeReference(typeId));
    }

    /**
     * Sets a custom {@link DataFixerContext}.
     *
     * @param context the context to use
     * @return this tester for chaining
     * @throws NullPointerException if {@code context} is null
     */
    @NotNull
    public DataFixTester<T> withContext(@NotNull final DataFixerContext context) {
        this.context = Preconditions.checkNotNull(context, "context must not be null");
        this.useRecordingContext = false;
        return this;
    }

    /**
     * Configures the tester to use a {@link RecordingContext}.
     *
     * <p>The recording context is accessible via {@link DataFixVerification#context()}
     * after calling {@link #verify()}.</p>
     *
     * @return this tester for chaining
     */
    @NotNull
    public DataFixTester<T> recordingContext() {
        this.useRecordingContext = true;
        return this;
    }

    /**
     * Configures the tester to fail immediately on warnings.
     *
     * @return this tester for chaining
     */
    @NotNull
    public DataFixTester<T> failOnWarning() {
        this.context = AssertingContext.failOnWarn();
        this.useRecordingContext = false;
        return this;
    }

    /**
     * Sets the expected output for verification.
     *
     * @param expected the expected output Dynamic
     * @return this tester for chaining
     * @throws NullPointerException if {@code expected} is null
     */
    @NotNull
    public DataFixTester<T> expectOutput(@NotNull final Dynamic<T> expected) {
        this.expectedOutput = Preconditions.checkNotNull(expected, "expected must not be null");
        return this;
    }

    // ==================== Execution ====================

    /**
     * Applies the fix and returns the result.
     *
     * @return the transformed Dynamic
     * @throws IllegalStateException if input or type reference is not set
     */
    @NotNull
    public Dynamic<T> apply() {
        this.validateConfiguration();

        final DataFixerContext effectiveContext = this.useRecordingContext
                ? new RecordingContext()
                : this.context;

        return this.fix.apply(this.typeReference, this.input, effectiveContext);
    }

    /**
     * Applies the fix and verifies the expected output.
     *
     * @return a verification result with access to the context and result
     * @throws IllegalStateException if input, type reference, or expected output is not set
     * @throws AssertionError        if the output does not match the expected value
     */
    @NotNull
    public DataFixVerification<T> verify() {
        this.validateConfiguration();

        final RecordingContext recordingContext;
        final DataFixerContext effectiveContext;

        if (this.useRecordingContext) {
            recordingContext = new RecordingContext();
            effectiveContext = recordingContext;
        } else if (this.context instanceof RecordingContext rc) {
            recordingContext = rc;
            effectiveContext = rc;
        } else {
            recordingContext = null;
            effectiveContext = this.context;
        }

        final Dynamic<T> result = this.fix.apply(this.typeReference, this.input, effectiveContext);

        // Verify expected output if set
        if (this.expectedOutput != null) {
            if (!Objects.equals(result.value(), this.expectedOutput.value())) {
                throw new AssertionError(String.format(
                        "DataFix '%s' output did not match expected.%nExpected:%n  %s%nActual:%n  %s",
                        this.fix.name(),
                        this.expectedOutput.value(),
                        result.value()
                ));
            }
        }

        return new DataFixVerification<>(result, recordingContext, true);
    }

    // ==================== Validation ====================

    private void validateConfiguration() {
        if (this.input == null) {
            throw new IllegalStateException("Input not set. Call withInput() before apply() or verify().");
        }
        if (this.typeReference == null) {
            throw new IllegalStateException("Type reference not set. Call forType() before apply() or verify().");
        }
    }

    // ==================== Verification Result ====================

    /**
     * The result of a DataFix verification.
     *
     * @param <T> the underlying value type
     */
    public static final class DataFixVerification<T> {

        private final Dynamic<T> result;
        private final RecordingContext context;
        private final boolean passed;

        DataFixVerification(
                @NotNull final Dynamic<T> result,
                @Nullable final RecordingContext context,
                final boolean passed
        ) {
            this.result = Preconditions.checkNotNull(result, "result must not be null");
            this.context = context;
            this.passed = passed;
        }

        /**
         * Returns the result of applying the fix.
         *
         * @return the resulting Dynamic
         */
        @NotNull
        public Dynamic<T> result() {
            return this.result;
        }

        /**
         * Returns the recording context if one was used.
         *
         * @return the RecordingContext, or null if not using recording
         */
        @Nullable
        public RecordingContext context() {
            return this.context;
        }

        /**
         * Returns whether the verification passed.
         *
         * @return true if verification passed
         */
        public boolean passed() {
            return this.passed;
        }

        /**
         * Asserts that no warnings were logged during fix execution.
         *
         * @return this verification for chaining
         * @throws AssertionError        if warnings were logged
         * @throws IllegalStateException if not using a recording context
         */
        @NotNull
        public DataFixVerification<T> assertNoWarnings() {
            if (this.context == null) {
                throw new IllegalStateException(
                        "Cannot assert on context. Use recordingContext() when building the tester.");
            }
            this.context.assertNoWarnings();
            return this;
        }

        /**
         * Asserts that no logs were recorded during fix execution.
         *
         * @return this verification for chaining
         * @throws AssertionError        if any logs were recorded
         * @throws IllegalStateException if not using a recording context
         */
        @NotNull
        public DataFixVerification<T> assertNoLogs() {
            if (this.context == null) {
                throw new IllegalStateException(
                        "Cannot assert on context. Use recordingContext() when building the tester.");
            }
            this.context.assertNoLogs();
            return this;
        }
    }
}
