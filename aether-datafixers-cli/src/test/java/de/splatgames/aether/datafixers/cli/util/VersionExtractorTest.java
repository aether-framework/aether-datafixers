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

package de.splatgames.aether.datafixers.cli.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("VersionExtractor")
class VersionExtractorTest {

    @Nested
    @DisplayName("extract()")
    class Extract {

        @Test
        @DisplayName("extracts version from simple field")
        void extractsFromSimpleField() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", 100);

            final DataVersion version = VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion");

            assertThat(version.getVersion()).isEqualTo(100);
        }

        @Test
        @DisplayName("extracts version from nested path")
        void extractsFromNestedPath() {
            final JsonObject data = JsonParser.parseString(
                    "{\"meta\":{\"version\":200}}").getAsJsonObject();

            final DataVersion version = VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "meta.version");

            assertThat(version.getVersion()).isEqualTo(200);
        }

        @Test
        @DisplayName("extracts version from deeply nested path")
        void extractsFromDeeplyNestedPath() {
            final JsonObject data = JsonParser.parseString(
                    "{\"level1\":{\"level2\":{\"level3\":{\"version\":300}}}}").getAsJsonObject();

            final DataVersion version = VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "level1.level2.level3.version");

            assertThat(version.getVersion()).isEqualTo(300);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for missing field")
        void throwsForMissingField() {
            final JsonObject data = new JsonObject();
            data.addProperty("other", "value");

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Version field not found");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for missing nested path")
        void throwsForMissingNestedPath() {
            final JsonObject data = new JsonObject();
            data.addProperty("other", "value");

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "meta.version"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Version field not found");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for non-integer version")
        void throwsForNonIntegerVersion() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", "not-a-number");

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a valid integer");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for object as version")
        void throwsForObjectAsVersion() {
            final JsonObject data = JsonParser.parseString(
                    "{\"dataVersion\":{\"major\":1,\"minor\":0}}").getAsJsonObject();

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a valid integer");
        }

        @Test
        @DisplayName("handles zero version")
        void handlesZeroVersion() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", 0);

            final DataVersion version = VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion");

            assertThat(version.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("handles large version number")
        void handlesLargeVersion() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", Integer.MAX_VALUE);

            final DataVersion version = VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion");

            assertThat(version.getVersion()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for array as version")
        void throwsForArrayAsVersion() {
            final JsonObject data = JsonParser.parseString(
                    "{\"dataVersion\":[1,2,3]}").getAsJsonObject();

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null value as version")
        void throwsForNullValueAsVersion() {
            final JsonObject data = JsonParser.parseString(
                    "{\"dataVersion\":null}").getAsJsonObject();

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for boolean as version")
        void throwsForBooleanAsVersion() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", true);

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws for intermediate path being non-object")
        void throwsForIntermediatePathNonObject() {
            final JsonObject data = new JsonObject();
            data.addProperty("meta", "not-an-object");

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "meta.version"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws for empty field path")
        void throwsForEmptyFieldPath() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", 100);

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("extracts from empty object (field not found)")
        void throwsForEmptyObject() {
            final JsonObject data = new JsonObject();

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Version field not found");
        }

        @Test
        @DisplayName("handles path with trailing dot")
        void handlesTrailingDot() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", 100);

            // Trailing dot should fail
            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion."))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("handles path with leading dot")
        void handlesLeadingDot() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", 100);

            // Leading dot should fail
            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, ".dataVersion"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("handles path with double dots")
        void handlesDoubleDots() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", 100);

            assertThatThrownBy(() -> VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "meta..version"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("extracts double value truncated to int")
        void extractsDoubleValueTruncated() {
            final JsonObject data = new JsonObject();
            data.addProperty("dataVersion", 100.9);

            final DataVersion version = VersionExtractor.extract(
                    data, GsonOps.INSTANCE, "dataVersion");

            assertThat(version.getVersion()).isEqualTo(100);
        }
    }
}
