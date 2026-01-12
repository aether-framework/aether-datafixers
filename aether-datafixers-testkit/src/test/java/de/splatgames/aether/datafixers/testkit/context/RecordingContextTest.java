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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RecordingContext")
class RecordingContextTest {

    private RecordingContext context;

    @BeforeEach
    void setUp() {
        context = new RecordingContext();
    }

    @Nested
    @DisplayName("Basic Logging")
    class BasicLogging {

        @Test
        @DisplayName("records info messages")
        void recordsInfoMessages() {
            context.info("Test message");

            assertThat(context.allLogs()).hasSize(1);
            assertThat(context.infoLogs()).hasSize(1);
            assertThat(context.warnLogs()).isEmpty();
        }

        @Test
        @DisplayName("records warn messages")
        void recordsWarnMessages() {
            context.warn("Warning message");

            assertThat(context.allLogs()).hasSize(1);
            assertThat(context.warnLogs()).hasSize(1);
            assertThat(context.infoLogs()).isEmpty();
        }

        @Test
        @DisplayName("records multiple messages")
        void recordsMultipleMessages() {
            context.info("Info 1");
            context.warn("Warn 1");
            context.info("Info 2");

            assertThat(context.allLogs()).hasSize(3);
            assertThat(context.infoLogs()).hasSize(2);
            assertThat(context.warnLogs()).hasSize(1);
        }

        @Test
        @DisplayName("formats messages with arguments")
        void formatsMessagesWithArguments() {
            context.info("Value is {} and {}", 42, "hello");

            assertThat(context.infoLogs().get(0).formattedMessage())
                    .isEqualTo("Value is 42 and hello");
        }

        @Test
        @DisplayName("handles null arguments gracefully")
        void handlesNullArguments() {
            context.info("Value is {}", (Object) null);

            assertThat(context.infoLogs().get(0).formattedMessage())
                    .isEqualTo("Value is null");
        }

        @Test
        @DisplayName("handles no arguments")
        void handlesNoArguments() {
            context.info("Simple message");

            assertThat(context.infoLogs().get(0).formattedMessage())
                    .isEqualTo("Simple message");
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("hasInfo returns true when substring found")
        void hasInfoReturnsTrue() {
            context.info("Migration completed successfully");

            assertThat(context.hasInfo("completed")).isTrue();
            assertThat(context.hasInfo("Migration")).isTrue();
        }

        @Test
        @DisplayName("hasInfo returns false when substring not found")
        void hasInfoReturnsFalse() {
            context.info("Migration completed");

            assertThat(context.hasInfo("error")).isFalse();
        }

        @Test
        @DisplayName("hasWarn returns true when substring found")
        void hasWarnReturnsTrue() {
            context.warn("Deprecated field found");

            assertThat(context.hasWarn("Deprecated")).isTrue();
        }

        @Test
        @DisplayName("hasWarn returns false when substring not found")
        void hasWarnReturnsFalse() {
            context.warn("Some warning");

            assertThat(context.hasWarn("error")).isFalse();
        }

        @Test
        @DisplayName("hasLog finds in any level")
        void hasLogFindsInAnyLevel() {
            context.info("Info message");
            context.warn("Warn message");

            assertThat(context.hasLog("Info")).isTrue();
            assertThat(context.hasLog("Warn")).isTrue();
            assertThat(context.hasLog("message")).isTrue();
            assertThat(context.hasLog("missing")).isFalse();
        }

        @Test
        @DisplayName("size returns correct count")
        void sizeReturnsCorrectCount() {
            assertThat(context.size()).isZero();

            context.info("One");
            assertThat(context.size()).isEqualTo(1);

            context.warn("Two");
            assertThat(context.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("isEmpty returns true when empty")
        void isEmptyWhenEmpty() {
            assertThat(context.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("isEmpty returns false when not empty")
        void isEmptyWhenNotEmpty() {
            context.info("test");

            assertThat(context.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Assertion Helpers")
    class AssertionHelpers {

        @Test
        @DisplayName("assertNoWarnings passes when no warnings")
        void assertNoWarningsPassesWhenNoWarnings() {
            context.info("Info is fine");

            assertThatCode(() -> context.assertNoWarnings())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("assertNoWarnings fails when warnings exist")
        void assertNoWarningsFailsWhenWarningsExist() {
            context.warn("Warning 1");
            context.warn("Warning 2");

            assertThatThrownBy(() -> context.assertNoWarnings())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected no warnings")
                    .hasMessageContaining("Warning 1")
                    .hasMessageContaining("Warning 2");
        }

        @Test
        @DisplayName("assertNoLogs passes when empty")
        void assertNoLogsPassesWhenEmpty() {
            assertThatCode(() -> context.assertNoLogs())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("assertNoLogs fails when logs exist")
        void assertNoLogsFailsWhenLogsExist() {
            context.info("Info message");

            assertThatThrownBy(() -> context.assertNoLogs())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected no logs")
                    .hasMessageContaining("Info message");
        }

        @Test
        @DisplayName("clear removes all logs")
        void clearRemovesAllLogs() {
            context.info("Info");
            context.warn("Warn");

            context.clear();

            assertThat(context.isEmpty()).isTrue();
            assertThat(context.allLogs()).isEmpty();
        }
    }

    @Nested
    @DisplayName("LogEntry")
    class LogEntryTests {

        @Test
        @DisplayName("toString includes level and message")
        void toStringIncludesLevelAndMessage() {
            context.info("Test message");

            assertThat(context.infoLogs().get(0).toString())
                    .isEqualTo("[INFO] Test message");
        }

        @Test
        @DisplayName("toString formats arguments")
        void toStringFormatsArguments() {
            context.warn("Error code: {}", 404);

            assertThat(context.warnLogs().get(0).toString())
                    .isEqualTo("[WARN] Error code: 404");
        }

        @Test
        @DisplayName("level returns correct level")
        void levelReturnsCorrectLevel() {
            context.info("Info");
            context.warn("Warn");

            assertThat(context.allLogs().get(0).level())
                    .isEqualTo(RecordingContext.LogLevel.INFO);
            assertThat(context.allLogs().get(1).level())
                    .isEqualTo(RecordingContext.LogLevel.WARN);
        }

        @Test
        @DisplayName("message returns raw message")
        void messageReturnsRawMessage() {
            context.info("Value is {}", 42);

            assertThat(context.infoLogs().get(0).message())
                    .isEqualTo("Value is {}");
        }

        @Test
        @DisplayName("args returns arguments")
        void argsReturnsArguments() {
            context.info("Values: {} and {}", 1, 2);

            assertThat(context.infoLogs().get(0).args())
                    .containsExactly(1, 2);
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("allLogs returns unmodifiable list")
        void allLogsReturnsUnmodifiableList() {
            context.info("test");

            assertThatThrownBy(() -> context.allLogs().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
