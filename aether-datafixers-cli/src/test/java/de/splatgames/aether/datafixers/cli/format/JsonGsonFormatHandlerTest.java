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

package de.splatgames.aether.datafixers.cli.format;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JsonGsonFormatHandler")
class JsonGsonFormatHandlerTest {

    private JsonGsonFormatHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JsonGsonFormatHandler();
    }

    @Nested
    @DisplayName("formatId()")
    class FormatId {

        @Test
        @DisplayName("returns 'json-gson'")
        void returnsCorrectId() {
            assertThat(handler.formatId()).isEqualTo("json-gson");
        }
    }

    @Nested
    @DisplayName("description()")
    class Description {

        @Test
        @DisplayName("returns non-empty description")
        void returnsDescription() {
            assertThat(handler.description()).isNotEmpty();
            assertThat(handler.description()).contains("Gson");
        }
    }

    @Nested
    @DisplayName("fileExtensions()")
    class FileExtensions {

        @Test
        @DisplayName("includes 'json' extension")
        void includesJsonExtension() {
            assertThat(handler.fileExtensions()).contains("json");
        }
    }

    @Nested
    @DisplayName("ops()")
    class Ops {

        @Test
        @DisplayName("returns GsonOps.INSTANCE")
        void returnsGsonOps() {
            assertThat(handler.ops()).isSameAs(GsonOps.INSTANCE);
        }
    }

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("parses simple JSON object")
        void parsesSimpleObject() {
            final String json = "{\"name\":\"test\",\"value\":42}";

            final JsonElement result = handler.parse(json);

            assertThat(result.isJsonObject()).isTrue();
            assertThat(result.getAsJsonObject().get("name").getAsString()).isEqualTo("test");
            assertThat(result.getAsJsonObject().get("value").getAsInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("parses JSON array")
        void parsesArray() {
            final String json = "[1, 2, 3]";

            final JsonElement result = handler.parse(json);

            assertThat(result.isJsonArray()).isTrue();
            assertThat(result.getAsJsonArray()).hasSize(3);
        }

        @Test
        @DisplayName("parses nested JSON")
        void parsesNestedJson() {
            final String json = "{\"outer\":{\"inner\":{\"value\":\"deep\"}}}";

            final JsonElement result = handler.parse(json);

            assertThat(result.getAsJsonObject()
                    .getAsJsonObject("outer")
                    .getAsJsonObject("inner")
                    .get("value").getAsString()).isEqualTo("deep");
        }

        @Test
        @DisplayName("throws FormatParseException on invalid JSON")
        void throwsOnInvalidJson() {
            final String invalidJson = "{invalid}";

            assertThatThrownBy(() -> handler.parse(invalidJson))
                    .isInstanceOf(FormatParseException.class)
                    .hasMessageContaining("Failed to parse JSON");
        }

        @Test
        @DisplayName("throws FormatParseException on malformed JSON")
        void throwsOnMalformedJson() {
            final String malformed = "{\"key\": }";

            assertThatThrownBy(() -> handler.parse(malformed))
                    .isInstanceOf(FormatParseException.class);
        }

        @Test
        @DisplayName("throws FormatParseException on empty string")
        void throwsOnEmptyString() {
            assertThatThrownBy(() -> handler.parse(""))
                    .isInstanceOf(FormatParseException.class);
        }

        @Test
        @DisplayName("throws FormatParseException on whitespace only")
        void throwsOnWhitespaceOnly() {
            assertThatThrownBy(() -> handler.parse("   \n\t  "))
                    .isInstanceOf(FormatParseException.class);
        }

        @Test
        @DisplayName("throws FormatParseException on truncated JSON")
        void throwsOnTruncatedJson() {
            assertThatThrownBy(() -> handler.parse("{\"name\": \"test\""))
                    .isInstanceOf(FormatParseException.class);
        }

        @Test
        @DisplayName("parses deeply nested JSON")
        void parsesDeeplyNestedJson() {
            final String deepJson = "{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":{\"f\":\"value\"}}}}}}";
            final JsonElement result = handler.parse(deepJson);
            assertThat(result.isJsonObject()).isTrue();
        }

        @Test
        @DisplayName("parses JSON with unicode characters")
        void parsesUnicodeJson() {
            final String unicodeJson = "{\"name\": \"\u4e2d\u6587\", \"emoji\": \"\ud83d\ude00\"}";
            final JsonElement result = handler.parse(unicodeJson);
            assertThat(result.getAsJsonObject().get("name").getAsString()).isEqualTo("\u4e2d\u6587");
        }

        @Test
        @DisplayName("parses empty JSON object")
        void parsesEmptyObject() {
            final JsonElement result = handler.parse("{}");
            assertThat(result.isJsonObject()).isTrue();
            assertThat(result.getAsJsonObject().size()).isZero();
        }

        @Test
        @DisplayName("parses empty JSON array")
        void parsesEmptyArray() {
            final JsonElement result = handler.parse("[]");
            assertThat(result.isJsonArray()).isTrue();
            assertThat(result.getAsJsonArray()).isEmpty();
        }

        @Test
        @DisplayName("parses JSON with special characters in strings")
        void parsesSpecialCharacters() {
            final String json = "{\"path\": \"C:\\\\Users\\\\test\", \"quote\": \"He said \\\"hello\\\"\"}";
            final JsonElement result = handler.parse(json);
            assertThat(result.getAsJsonObject().get("path").getAsString()).isEqualTo("C:\\Users\\test");
        }

        @Test
        @DisplayName("parses JSON with null value")
        void parsesNullValue() {
            final String json = "{\"value\": null}";
            final JsonElement result = handler.parse(json);
            assertThat(result.getAsJsonObject().get("value").isJsonNull()).isTrue();
        }

        @Test
        @DisplayName("parses JSON with boolean values")
        void parsesBooleanValues() {
            final String json = "{\"enabled\": true, \"disabled\": false}";
            final JsonElement result = handler.parse(json);
            assertThat(result.getAsJsonObject().get("enabled").getAsBoolean()).isTrue();
            assertThat(result.getAsJsonObject().get("disabled").getAsBoolean()).isFalse();
        }

        @Test
        @DisplayName("parses JSON with large numbers")
        void parsesLargeNumbers() {
            final String json = "{\"big\": 9223372036854775807, \"float\": 1.7976931348623157E308}";
            final JsonElement result = handler.parse(json);
            assertThat(result.getAsJsonObject().get("big").getAsLong()).isEqualTo(Long.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("serialize()")
    class Serialize {

        @Test
        @DisplayName("serializes JSON object to compact string")
        void serializesToCompactString() {
            final JsonObject obj = new JsonObject();
            obj.addProperty("name", "test");
            obj.addProperty("value", 42);

            final String result = handler.serialize(obj);

            assertThat(result).doesNotContain("\n");
            assertThat(result).contains("\"name\":\"test\"");
            assertThat(result).contains("\"value\":42");
        }
    }

    @Nested
    @DisplayName("serializePretty()")
    class SerializePretty {

        @Test
        @DisplayName("serializes JSON with indentation")
        void serializesWithIndentation() {
            final JsonObject obj = new JsonObject();
            obj.addProperty("name", "test");

            final String result = handler.serializePretty(obj);

            assertThat(result).contains("\n");
            assertThat(result).contains("  "); // indentation
        }
    }

    @Nested
    @DisplayName("Round-trip")
    class RoundTrip {

        @Test
        @DisplayName("parse -> serialize preserves data")
        void roundTripPreservesData() {
            final String original = "{\"name\":\"test\",\"value\":42,\"nested\":{\"key\":\"value\"}}";

            final JsonElement parsed = handler.parse(original);
            final String serialized = handler.serialize(parsed);
            final JsonElement reparsed = handler.parse(serialized);

            assertThat(reparsed).isEqualTo(parsed);
        }
    }
}
