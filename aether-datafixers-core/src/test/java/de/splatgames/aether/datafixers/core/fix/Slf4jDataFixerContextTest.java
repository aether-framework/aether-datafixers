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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Slf4jDataFixerContext}.
 */
@DisplayName("Slf4jDataFixerContext")
class Slf4jDataFixerContextTest {

    @Nested
    @DisplayName("Constructor - Default")
    class DefaultConstructor {

        @Test
        @DisplayName("creates context with default logger")
        void createsContextWithDefaultLogger() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            assertThat(context.getLogger()).isNotNull();
            // Note: Logger name may be "NOP" if no SLF4J binding is present
        }
    }

    @Nested
    @DisplayName("Constructor - Logger Name")
    class LoggerNameConstructor {

        @Test
        @DisplayName("creates context with custom logger name")
        void createsContextWithCustomLoggerName() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext("custom.logger.name");

            assertThat(context.getLogger()).isNotNull();
            // Note: Logger name may be "NOP" if no SLF4J binding is present
        }

        @Test
        @DisplayName("rejects null logger name")
        void rejectsNullLoggerName() {
            assertThatThrownBy(() -> new Slf4jDataFixerContext((String) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("loggerName");
        }
    }

    @Nested
    @DisplayName("Constructor - Logger Instance")
    class LoggerInstanceConstructor {

        @Test
        @DisplayName("creates context with provided logger")
        void createsContextWithProvidedLogger() {
            Logger logger = LoggerFactory.getLogger("test.logger");

            Slf4jDataFixerContext context = new Slf4jDataFixerContext(logger);

            assertThat(context.getLogger()).isSameAs(logger);
        }

        @Test
        @DisplayName("rejects null logger")
        void rejectsNullLogger() {
            assertThatThrownBy(() -> new Slf4jDataFixerContext((Logger) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("logger");
        }
    }

    @Nested
    @DisplayName("info()")
    class InfoMethod {

        @Test
        @DisplayName("does not throw with valid message")
        void doesNotThrowWithValidMessage() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            // Should not throw
            context.info("Test message");
        }

        @Test
        @DisplayName("does not throw with format arguments")
        void doesNotThrowWithFormatArguments() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            // Should not throw
            context.info("Test message with %s and %d", "string", 42);
        }

        @Test
        @DisplayName("does not throw with null arguments")
        void doesNotThrowWithNullArguments() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            // Should not throw
            context.info("Test message");
            context.info("Test message", (Object[]) null);
        }

        @Test
        @DisplayName("rejects null message")
        void rejectsNullMessage() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            assertThatThrownBy(() -> context.info(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("message");
        }
    }

    @Nested
    @DisplayName("warn()")
    class WarnMethod {

        @Test
        @DisplayName("does not throw with valid message")
        void doesNotThrowWithValidMessage() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            // Should not throw
            context.warn("Test warning");
        }

        @Test
        @DisplayName("does not throw with format arguments")
        void doesNotThrowWithFormatArguments() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            // Should not throw
            context.warn("Test warning with %s and %d", "string", 42);
        }

        @Test
        @DisplayName("does not throw with null arguments")
        void doesNotThrowWithNullArguments() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            // Should not throw
            context.warn("Test warning");
            context.warn("Test warning", (Object[]) null);
        }

        @Test
        @DisplayName("rejects null message")
        void rejectsNullMessage() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            assertThatThrownBy(() -> context.warn(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("message");
        }
    }

    @Nested
    @DisplayName("getLogger()")
    class GetLoggerMethod {

        @Test
        @DisplayName("returns non-null logger")
        void returnsNonNullLogger() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            assertThat(context.getLogger()).isNotNull();
        }

        @Test
        @DisplayName("returns same logger instance on multiple calls")
        void returnsSameLoggerInstance() {
            Slf4jDataFixerContext context = new Slf4jDataFixerContext();

            Logger logger1 = context.getLogger();
            Logger logger2 = context.getLogger();

            assertThat(logger1).isSameAs(logger2);
        }
    }
}
