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

package de.splatgames.aether.datafixers.cli.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TextReportFormatter")
class TextReportFormatterTest {

    private TextReportFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new TextReportFormatter();
    }

    @Nested
    @DisplayName("formatSimple()")
    class FormatSimple {

        @Test
        @DisplayName("includes file name")
        void includesFileName() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            assertThat(result).contains("player.json");
        }

        @Test
        @DisplayName("includes type")
        void includesType() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            assertThat(result).contains("player");
        }

        @Test
        @DisplayName("includes from version")
        void includesFromVersion() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            assertThat(result).contains("100");
        }

        @Test
        @DisplayName("includes to version")
        void includesToVersion() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            assertThat(result).contains("200");
        }

        @Test
        @DisplayName("includes duration in milliseconds")
        void includesDuration() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            assertThat(result).contains("50ms");
        }

        @Test
        @DisplayName("formats typical migration report")
        void formatsTypicalReport() {
            final String result = formatter.formatSimple(
                    "world_data.json", "world", 1, 5, Duration.ofMillis(123));

            assertThat(result).contains("Migration:");
            assertThat(result).contains("world_data.json");
            assertThat(result).contains("[world]");
            assertThat(result).contains("v1");
            assertThat(result).contains("v5");
            assertThat(result).contains("123ms");
        }

        @Test
        @DisplayName("handles zero duration")
        void handlesZeroDuration() {
            final String result = formatter.formatSimple(
                    "fast.json", "type", 1, 2, Duration.ZERO);

            assertThat(result).contains("0ms");
        }
    }
}
