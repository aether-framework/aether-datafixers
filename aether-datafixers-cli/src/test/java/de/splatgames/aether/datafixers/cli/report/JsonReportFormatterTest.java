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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JsonReportFormatter")
class JsonReportFormatterTest {

    private JsonReportFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new JsonReportFormatter();
    }

    @Nested
    @DisplayName("formatSimple()")
    class FormatSimple {

        @Test
        @DisplayName("produces valid JSON")
        void producesValidJson() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            // Should not throw
            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            assertThat(json).isNotNull();
        }

        @Test
        @DisplayName("includes file field")
        void includesFileField() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            assertThat(json.get("file").getAsString()).isEqualTo("player.json");
        }

        @Test
        @DisplayName("includes type field")
        void includesTypeField() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            assertThat(json.get("type").getAsString()).isEqualTo("player");
        }

        @Test
        @DisplayName("includes fromVersion field")
        void includesFromVersionField() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            assertThat(json.get("fromVersion").getAsInt()).isEqualTo(100);
        }

        @Test
        @DisplayName("includes toVersion field")
        void includesToVersionField() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            assertThat(json.get("toVersion").getAsInt()).isEqualTo(200);
        }

        @Test
        @DisplayName("includes durationMs field")
        void includesDurationMsField() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            assertThat(json.get("durationMs").getAsLong()).isEqualTo(50);
        }

        @Test
        @DisplayName("is pretty-printed")
        void isPrettyPrinted() {
            final String result = formatter.formatSimple(
                    "player.json", "player", 100, 200, Duration.ofMillis(50));

            assertThat(result).contains("\n");
        }

        @Test
        @DisplayName("handles special characters in file name")
        void handlesSpecialCharsInFileName() {
            final String result = formatter.formatSimple(
                    "path/to/file with spaces.json", "player", 100, 200, Duration.ofMillis(50));

            final JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            assertThat(json.get("file").getAsString()).isEqualTo("path/to/file with spaces.json");
        }
    }
}
