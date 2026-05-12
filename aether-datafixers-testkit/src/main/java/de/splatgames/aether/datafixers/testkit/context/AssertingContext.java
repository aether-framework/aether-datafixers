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

package de.splatgames.aether.datafixers.testkit.context;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link DataFixerContext} that fails immediately or collects warnings for assertion.
 *
 * <p>This context implementation can be configured to either fail immediately
 * when a warning is logged, or to collect warnings for later assertion. This is useful for tests that require strict
 * "no warnings" behavior.</p>
 *
 * <h2>Fail-Fast Mode</h2>
 * <pre>{@code
 * // Throws immediately on any warn() call
 * AssertingContext context = AssertingContext.failOnWarn();
 * fix.apply(typeRef, inputData, context);  // Throws if fix logs a warning
 * }</pre>
 *
 * <h2>Collecting Mode</h2>
 * <pre>{@code
 * // Collects warnings for later assertion
 * AssertingContext context = AssertingContext.collectingWarns();
 * fix.apply(typeRef, inputData, context);
 * context.assertNoWarnings();  // Throws if any warnings were collected
 * }</pre>
 *
 * <h2>Silent Mode</h2>
 * <pre>{@code
 * // Ignores all logs (useful when warnings are expected)
 * AssertingContext context = AssertingContext.silent();
 * fix.apply(typeRef, inputData, context);
 * }</pre>
 *
 * @author Erik Pförtner
 * @see RecordingContext
 * @since 0.2.0
 */
public final class AssertingContext implements DataFixerContext {

    /**
     * Defines the behavior of the AssertingContext when a warning is logged.
     * <ul>
     *     <li><strong>FAIL_ON_WARN</strong>: Throws an AssertionError immediately on any warn() call.</li>
     *     <li><strong>COLLECT</strong>: Collects warning messages for later
     *     assertion via warnings(), warningCount(), hasWarnings(), and assertNoWarnings().</li>
     *     <li><strong>SILENT</strong>: Ignores all warn()
     *     calls without throwing or collecting (useful when warnings are expected).</li>
     * </ul>
     */
    private final Mode mode;
    /**
     * In COLLECT mode, this list stores all formatted warning messages logged via warn().
     */
    private final List<String> collectedWarnings;

    private AssertingContext(@NotNull final Mode mode) {
        this.mode = mode;
        this.collectedWarnings = new ArrayList<>();
    }

    /**
     * Creates a context that throws immediately on any {@code warn()} call.
     *
     * @return a fail-on-warn context
     */
    @NotNull
    public static AssertingContext failOnWarn() {
        return new AssertingContext(Mode.FAIL_ON_WARN);
    }

    /**
     * Creates a context that collects warnings for later assertion.
     *
     * @return a collecting context
     */
    @NotNull
    public static AssertingContext collectingWarns() {
        return new AssertingContext(Mode.COLLECT);
    }

    /**
     * Creates a context that silently ignores all logs.
     *
     * @return a silent context
     */
    @NotNull
    public static AssertingContext silent() {
        return new AssertingContext(Mode.SILENT);
    }

    /**
     * Formats a message by replacing each occurrence of "{}" with the corresponding argument from args.
     *
     * <p>
     * This is a simple implementation that mimics the behavior of common logging frameworks. It does not support
     * escaping or advanced formatting features. If there are more placeholders than arguments, the remaining
     * placeholders will be left as-is. If there are more arguments than placeholders, the extra arguments will be
     * ignored.
     * </p>
     *
     * @param message the message template containing "{}" placeholders
     * @param args    the arguments to replace the placeholders, may be null or empty
     * @return the formatted message with placeholders replaced by argument values
     */
    @NotNull
    private static String formatMessage(@NotNull final String message, @NotNull final Object[] args) {
        if (args == null || args.length == 0) {
            return message;
        }
        final StringBuilder result = new StringBuilder(message);
        for (final Object arg : args) {
            final int idx = result.indexOf("{}");
            if (idx < 0) {
                break;
            }
            result.replace(idx, idx + 2, String.valueOf(arg));
        }
        return result.toString();
    }

    /**
     * Logs an info message. This implementation does not perform any actual logging, but it can be extended to
     * integrate with a logging framework if desired. Info messages are always allowed regardless of the mode.
     *
     * @param message the message template containing "{}" placeholders, must not be null
     * @param args    the arguments to replace the placeholders, may be null or empty
     */
    @Override
    public void info(@NotNull final String message, @Nullable final Object... args) {
        // Info messages are always allowed
    }

    /**
     * Logs a warning message. The behavior depends on the configured mode:
     * <ul>
     *     <li><strong>FAIL_ON_WARN</strong>: Throws an AssertionError immediately with the formatted message.</li>
     *     <li><strong>COLLECT</strong>: Adds the formatted message to the collectedWarnings list for later assertion.</li>
     *     <li><strong>SILENT</strong>: Does nothing, effectively ignoring the warning.</li>
     * </ul>
     *
     * @param message the message template containing "{}" placeholders, must not be null
     * @param args    the arguments to replace the placeholders, may be null or empty
     */
    @Override
    public void warn(@NotNull final String message, @Nullable final Object... args) {
        Preconditions.checkNotNull(message, "message must not be null");
        final String formatted = formatMessage(message, args);

        switch (this.mode) {
            case FAIL_ON_WARN -> throw new AssertionError("Unexpected warning during fix execution: " + formatted);
            case COLLECT -> this.collectedWarnings.add(formatted);
            case SILENT -> { /* ignore */ }
        }
    }

    /**
     * Returns all collected warnings (only in COLLECT mode).
     *
     * @return unmodifiable list of warning messages
     */
    @NotNull
    public List<String> warnings() {
        return Collections.unmodifiableList(this.collectedWarnings);
    }

    /**
     * Returns the number of collected warnings.
     *
     * @return the warning count
     */
    public int warningCount() {
        return this.collectedWarnings.size();
    }

    /**
     * Checks if any warnings were collected.
     *
     * @return true if warnings exist
     */
    public boolean hasWarnings() {
        return !this.collectedWarnings.isEmpty();
    }

    /**
     * Asserts that no warnings were collected.
     *
     * @throws AssertionError if any warnings were collected
     */
    public void assertNoWarnings() {
        if (!this.collectedWarnings.isEmpty()) {
            final String messages = this.collectedWarnings.stream()
                    .collect(Collectors.joining("\n  - ", "\n  - ", ""));
            throw new AssertionError("Expected no warnings but found " +
                    this.collectedWarnings.size() + ":" + messages);
        }
    }

    /**
     * Clears all collected warnings.
     */
    public void clear() {
        this.collectedWarnings.clear();
    }

    /**
     * Defines the behavior modes for the AssertingContext when handling warnings.
     */
    private enum Mode {
        /**
         * In this mode, any call to warn() will immediately throw an AssertionError with the formatted message. This is
         * useful for tests that require strict "no warnings" behavior.
         */
        FAIL_ON_WARN,
        /**
         * In this mode, warn() calls will add the formatted message to the collectedWarnings list for later assertion.
         * This allows tests to verify that certain warnings were logged without failing immediately.
         */
        COLLECT,
        /**
         * In this mode, warn() calls will be ignored without throwing or collecting. This is useful for tests where
         * warnings are expected and should not cause failures.
         */
        SILENT
    }
}
