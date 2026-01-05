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
 * when a warning is logged, or to collect warnings for later assertion. This
 * is useful for tests that require strict "no warnings" behavior.</p>
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

    private final Mode mode;
    private final List<String> collectedWarnings;

    private AssertingContext(final Mode mode) {
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

    @Override
    public void info(@NotNull final String message, @Nullable final Object... args) {
        // Info messages are always allowed
    }

    @Override
    public void warn(@NotNull final String message, @Nullable final Object... args) {
        final String formatted = formatMessage(message, args);

        switch (this.mode) {
            case FAIL_ON_WARN -> throw new AssertionError("Unexpected warning during fix execution: " + formatted);
            case COLLECT -> this.collectedWarnings.add(formatted);
            case SILENT -> { /* ignore */ }
        }
    }

    // ==================== Query Methods ====================

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

    // ==================== Assertion Methods ====================

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

    // ==================== Internal ====================

    private static String formatMessage(final String message, final Object[] args) {
        if (args == null || args.length == 0) {
            return message;
        }
        String result = message;
        for (final Object arg : args) {
            result = result.replaceFirst("\\{}", String.valueOf(arg));
        }
        return result;
    }

    private enum Mode {
        FAIL_ON_WARN,
        COLLECT,
        SILENT
    }
}
