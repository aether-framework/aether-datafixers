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

package de.splatgames.aether.datafixers.core.fix;

import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple implementation of {@link DataFixerContext} that logs to {@code System.out}.
 *
 * <p>{@code SimpleSystemDataFixerContext} prints log messages to standard output
 * using {@link System#out}. It is the default context used by {@link DataFixerBuilder}.</p>
 *
 * <h2>Output Format</h2>
 * <pre>
 * [INFO] Migrating player data from v1 to v2
 * [WARN] Missing 'health' field, using default
 * </pre>
 *
 * <h2>Usage</h2>
 * <p>This is the default context. For production use with a proper logging framework,
 * implement a custom {@link DataFixerContext} that delegates to your logger.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The singleton instance can be shared.</p>
 *
 * @author Erik Pförtner
 * @see DataFixerContext
 * @see DataFixerBuilder
 * @since 0.1.0
 */
public final class SimpleSystemDataFixerContext implements DataFixerContext {

    /**
     * The singleton instance.
     */
    public static final SimpleSystemDataFixerContext INSTANCE = new SimpleSystemDataFixerContext();

    private SimpleSystemDataFixerContext() {
        // private constructor to prevent instantiation
    }

    @Override
    public void info(@NotNull final String message, @Nullable final Object... args) {
        System.out.printf("[INFO] " + message + "%n", args);
    }

    @Override
    public void warn(@NotNull final String message, @Nullable final Object... args) {
        System.out.printf("[WARN] " + message + "%n", args);
    }
}
