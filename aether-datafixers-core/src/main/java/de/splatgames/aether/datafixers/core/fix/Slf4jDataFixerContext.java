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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DataFixerContext} implementation that uses SLF4J for logging.
 *
 * <p>{@code Slf4jDataFixerContext} provides integration with SLF4J, allowing
 * datafixer logs to be routed through the application's logging framework
 * (Logback, Log4j2, etc.).</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create with default logger name
 * DataFixerContext context = new Slf4jDataFixerContext();
 *
 * // Create with custom logger name
 * DataFixerContext context = new Slf4jDataFixerContext("com.myapp.datafixer");
 *
 * // Create with existing logger
 * Logger logger = LoggerFactory.getLogger(MyApp.class);
 * DataFixerContext context = new Slf4jDataFixerContext(logger);
 *
 * // Use with builder
 * DataFixer fixer = new DataFixerBuilder(currentVersion)
 *     .withDefaultContext(new Slf4jDataFixerContext())
 *     .addFix(...)
 *     .build();
 * }</pre>
 *
 * <h2>Log Levels</h2>
 * <ul>
 *   <li>{@link #info(String, Object...)} logs at INFO level</li>
 *   <li>{@link #warn(String, Object...)} logs at WARN level</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <p>This class requires SLF4J API to be on the classpath. SLF4J is an optional
 * dependency of the core module. If SLF4J is not available, use
 * {@link SimpleSystemDataFixerContext} instead.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. Instances can be shared across threads.</p>
 *
 * @author Erik Pförtner
 * @see DataFixerContext
 * @see SimpleSystemDataFixerContext
 * @see DataFixerBuilder
 * @since 0.1.0
 */
public final class Slf4jDataFixerContext implements DataFixerContext {

    private static final String DEFAULT_LOGGER_NAME = "de.splatgames.aether.datafixers";

    private final Logger logger;

    /**
     * Creates a context with the default logger name.
     *
     * <p>The default logger name is {@code de.splatgames.aether.datafixers}.</p>
     */
    public Slf4jDataFixerContext() {
        this(LoggerFactory.getLogger(DEFAULT_LOGGER_NAME));
    }

    /**
     * Creates a context with a custom logger name.
     *
     * @param loggerName the logger name to use
     */
    public Slf4jDataFixerContext(@NotNull final String loggerName) {
        this(LoggerFactory.getLogger(loggerName));
    }

    /**
     * Creates a context with an existing logger.
     *
     * @param logger the SLF4J logger to use
     */
    public Slf4jDataFixerContext(@NotNull final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(@NotNull final String message, @Nullable final Object... args) {
        if (this.logger.isInfoEnabled()) {
            this.logger.info(formatMessage(message, args));
        }
    }

    @Override
    public void warn(@NotNull final String message, @Nullable final Object... args) {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn(formatMessage(message, args));
        }
    }

    /**
     * Returns the underlying SLF4J logger.
     *
     * @return the logger, never {@code null}
     */
    @NotNull
    public Logger getLogger() {
        return this.logger;
    }

    private static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }
}
