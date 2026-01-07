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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.splatgames.aether.datafixers.codec.jackson.JacksonOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JsonJacksonFormatHandler")
class JsonJacksonFormatHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonJacksonFormatHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JsonJacksonFormatHandler();
    }

    @Nested
    @DisplayName("formatId()")
    class FormatId {

        @Test
        @DisplayName("returns 'json-jackson'")
        void returnsCorrectId() {
            assertThat(handler.formatId()).isEqualTo("json-jackson");
        }
    }

    @Nested
    @DisplayName("description()")
    class Description {

        @Test
        @DisplayName("returns non-empty description")
        void returnsDescription() {
            assertThat(handler.description()).isNotEmpty();
            assertThat(handler.description()).contains("Jackson");
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
        @DisplayName("returns JacksonOps.INSTANCE")
        void returnsJacksonOps() {
            assertThat(handler.ops()).isSameAs(JacksonOps.INSTANCE);
        }
    }

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("parses simple JSON object")
        void parsesSimpleObject() {
            final String json = "{\"name\":\"test\",\"value\":42}";

            final JsonNode result = handler.parse(json);

            assertThat(result.isObject()).isTrue();
            assertThat(result.get("name").asText()).isEqualTo("test");
            assertThat(result.get("value").asInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("parses JSON array")
        void parsesArray() {
            final String json = "[1, 2, 3]";

            final JsonNode result = handler.parse(json);

            assertThat(result.isArray()).isTrue();
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("parses nested JSON")
        void parsesNestedJson() {
            final String json = "{\"outer\":{\"inner\":{\"value\":\"deep\"}}}";

            final JsonNode result = handler.parse(json);

            assertThat(result.get("outer").get("inner").get("value").asText()).isEqualTo("deep");
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
        @DisplayName("parses empty JSON object")
        void parsesEmptyObject() {
            final JsonNode result = handler.parse("{}");
            assertThat(result.isObject()).isTrue();
            assertThat(result.size()).isZero();
        }

        @Test
        @DisplayName("parses empty JSON array")
        void parsesEmptyArray() {
            final JsonNode result = handler.parse("[]");
            assertThat(result.isArray()).isTrue();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("parses JSON with null value")
        void parsesNullValue() {
            final String json = "{\"value\": null}";
            final JsonNode result = handler.parse(json);
            assertThat(result.get("value").isNull()).isTrue();
        }

        @Test
        @DisplayName("parses JSON with boolean values")
        void parsesBooleanValues() {
            final String json = "{\"enabled\": true, \"disabled\": false}";
            final JsonNode result = handler.parse(json);
            assertThat(result.get("enabled").asBoolean()).isTrue();
            assertThat(result.get("disabled").asBoolean()).isFalse();
        }
    }

    @Nested
    @DisplayName("serialize()")
    class Serialize {

        @Test
        @DisplayName("serializes JSON object to compact string")
        void serializesToCompactString() {
            final ObjectNode obj = MAPPER.createObjectNode();
            obj.put("name", "test");
            obj.put("value", 42);

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
            final ObjectNode obj = MAPPER.createObjectNode();
            obj.put("name", "test");

            final String result = handler.serializePretty(obj);

            assertThat(result).contains("\n");
        }
    }

    @Nested
    @DisplayName("Round-trip")
    class RoundTrip {

        @Test
        @DisplayName("parse -> serialize preserves data")
        void roundTripPreservesData() {
            final String original = "{\"name\":\"test\",\"value\":42,\"nested\":{\"key\":\"value\"}}";

            final JsonNode parsed = handler.parse(original);
            final String serialized = handler.serialize(parsed);
            final JsonNode reparsed = handler.parse(serialized);

            assertThat(reparsed).isEqualTo(parsed);
        }
    }
}
