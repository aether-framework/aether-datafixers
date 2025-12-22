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

package de.splatgames.aether.datafixers.core.fix.noop;

import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A no-operation implementation of {@link DataFixerContext}.
 *
 * <p>{@code NoOpDataFixerContext} silently discards all log messages. It is
 * useful in production environments where fix logging is not needed, or in
 * tests where log output should be suppressed.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DataFixer fixer = new DataFixerBuilder(currentVersion)
 *     .withDefaultContext(NoOpDataFixerContext.INSTANCE)
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The singleton instance can be shared.</p>
 *
 * @author Erik Pförtner
 * @see DataFixerContext
 * @since 0.1.0
 */
public final class NoOpDataFixerContext implements DataFixerContext {

    /**
     * The singleton instance.
     */
    public static final NoOpDataFixerContext INSTANCE = new NoOpDataFixerContext();

    private NoOpDataFixerContext() {
        // private constructor to prevent instantiation
    }

    @Override
    public void info(@NotNull final String message, @Nullable final Object... args) {

    }

    @Override
    public void warn(@NotNull final String message, @Nullable final Object... args) {

    }
}
