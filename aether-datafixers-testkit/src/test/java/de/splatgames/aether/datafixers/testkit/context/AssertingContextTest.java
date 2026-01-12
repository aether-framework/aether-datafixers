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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AssertingContext")
class AssertingContextTest {

    @Nested
    @DisplayName("Fail-On-Warn Mode")
    class FailOnWarnMode {

        @Test
        @DisplayName("info does not throw")
        void infoDoesNotThrow() {
            final AssertingContext context = AssertingContext.failOnWarn();

            assertThatCode(() -> context.info("Info message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("warn throws immediately")
        void warnThrowsImmediately() {
            final AssertingContext context = AssertingContext.failOnWarn();

            assertThatThrownBy(() -> context.warn("Warning message"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Unexpected warning")
                    .hasMessageContaining("Warning message");
        }

        @Test
        @DisplayName("warn formats message with arguments")
        void warnFormatsMessage() {
            final AssertingContext context = AssertingContext.failOnWarn();

            assertThatThrownBy(() -> context.warn("Error code: {}", 404))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Error code: 404");
        }

        @Test
        @DisplayName("does not collect warnings")
        void doesNotCollectWarnings() {
            final AssertingContext context = AssertingContext.failOnWarn();

            assertThat(context.warnings()).isEmpty();
            assertThat(context.hasWarnings()).isFalse();
        }
    }

    @Nested
    @DisplayName("Collecting Mode")
    class CollectingMode {

        @Test
        @DisplayName("info does not throw")
        void infoDoesNotThrow() {
            final AssertingContext context = AssertingContext.collectingWarns();

            assertThatCode(() -> context.info("Info message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("warn does not throw immediately")
        void warnDoesNotThrowImmediately() {
            final AssertingContext context = AssertingContext.collectingWarns();

            assertThatCode(() -> context.warn("Warning message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("collects warnings")
        void collectsWarnings() {
            final AssertingContext context = AssertingContext.collectingWarns();

            context.warn("Warning 1");
            context.warn("Warning 2");

            assertThat(context.warnings()).containsExactly("Warning 1", "Warning 2");
            assertThat(context.warningCount()).isEqualTo(2);
            assertThat(context.hasWarnings()).isTrue();
        }

        @Test
        @DisplayName("formats warnings with arguments")
        void formatsWarningsWithArguments() {
            final AssertingContext context = AssertingContext.collectingWarns();

            context.warn("Value is {} and {}", 42, "hello");

            assertThat(context.warnings()).containsExactly("Value is 42 and hello");
        }

        @Test
        @DisplayName("assertNoWarnings passes when no warnings")
        void assertNoWarningsPassesWhenEmpty() {
            final AssertingContext context = AssertingContext.collectingWarns();

            assertThatCode(context::assertNoWarnings)
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("assertNoWarnings fails when warnings exist")
        void assertNoWarningsFailsWhenWarningsExist() {
            final AssertingContext context = AssertingContext.collectingWarns();
            context.warn("Warning 1");
            context.warn("Warning 2");

            assertThatThrownBy(context::assertNoWarnings)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("Expected no warnings")
                    .hasMessageContaining("Warning 1")
                    .hasMessageContaining("Warning 2");
        }

        @Test
        @DisplayName("clear removes collected warnings")
        void clearRemovesWarnings() {
            final AssertingContext context = AssertingContext.collectingWarns();
            context.warn("Warning");

            context.clear();

            assertThat(context.warnings()).isEmpty();
            assertThat(context.hasWarnings()).isFalse();
        }
    }

    @Nested
    @DisplayName("Silent Mode")
    class SilentMode {

        @Test
        @DisplayName("info does not throw")
        void infoDoesNotThrow() {
            final AssertingContext context = AssertingContext.silent();

            assertThatCode(() -> context.info("Info message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("warn does not throw")
        void warnDoesNotThrow() {
            final AssertingContext context = AssertingContext.silent();

            assertThatCode(() -> context.warn("Warning message"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("does not collect warnings")
        void doesNotCollectWarnings() {
            final AssertingContext context = AssertingContext.silent();
            context.warn("Warning");

            assertThat(context.warnings()).isEmpty();
            assertThat(context.hasWarnings()).isFalse();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("warnings returns unmodifiable list")
        void warningsReturnsUnmodifiableList() {
            final AssertingContext context = AssertingContext.collectingWarns();
            context.warn("Warning");

            assertThatThrownBy(() -> context.warnings().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles null argument array in warn")
        void handlesNullArgumentArray() {
            final AssertingContext context = AssertingContext.collectingWarns();

            context.warn("Message", (Object[]) null);

            assertThat(context.warnings()).containsExactly("Message");
        }

        @Test
        @DisplayName("handles empty argument array in warn")
        void handlesEmptyArgumentArray() {
            final AssertingContext context = AssertingContext.collectingWarns();

            context.warn("Message");

            assertThat(context.warnings()).containsExactly("Message");
        }

        @Test
        @DisplayName("handles null argument in array")
        void handlesNullArgumentInArray() {
            final AssertingContext context = AssertingContext.collectingWarns();

            context.warn("Value is {}", (Object) null);

            assertThat(context.warnings()).containsExactly("Value is null");
        }
    }
}
