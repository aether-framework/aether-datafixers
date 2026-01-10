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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import de.splatgames.aether.datafixers.codec.toml.jackson.JacksonTomlOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TomlJacksonFormatHandler}.
 *
 * @author Erik Pförtner
 * @since 0.5.0
 */
@DisplayName("TomlJacksonFormatHandler")
class TomlJacksonFormatHandlerTest {

    private static final TomlMapper MAPPER = new TomlMapper();
    private TomlJacksonFormatHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new TomlJacksonFormatHandler();
    }

    @Nested
    @DisplayName("formatId()")
    class FormatId {

        @Test
        @DisplayName("returns 'toml-jackson'")
        void returnsCorrectId() {
            assertThat(handler.formatId()).isEqualTo("toml-jackson");
        }
    }

    @Nested
    @DisplayName("description()")
    class Description {

        @Test
        @DisplayName("returns non-empty description containing 'TOML' and 'Jackson'")
        void returnsDescription() {
            assertThat(handler.description()).isNotEmpty();
            assertThat(handler.description()).contains("TOML");
            assertThat(handler.description()).contains("Jackson");
        }
    }

    @Nested
    @DisplayName("fileExtensions()")
    class FileExtensions {

        @Test
        @DisplayName("includes 'toml' extension")
        void includesTomlExtension() {
            assertThat(handler.fileExtensions()).contains("toml");
        }
    }

    @Nested
    @DisplayName("ops()")
    class Ops {

        @Test
        @DisplayName("returns JacksonTomlOps.INSTANCE")
        void returnsJacksonTomlOps() {
            assertThat(handler.ops()).isSameAs(JacksonTomlOps.INSTANCE);
        }
    }

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("parses simple TOML document")
        void parsesSimpleDocument() {
            final String toml = "name = \"test\"\nvalue = 42";

            final JsonNode result = handler.parse(toml);

            assertThat(result.isObject()).isTrue();
            assertThat(result.get("name").asText()).isEqualTo("test");
            assertThat(result.get("value").asInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("parses TOML with table (section)")
        void parsesTable() {
            final String toml = "[server]\nhost = \"localhost\"\nport = 8080";

            final JsonNode result = handler.parse(toml);

            assertThat(result.get("server").isObject()).isTrue();
            assertThat(result.get("server").get("host").asText()).isEqualTo("localhost");
            assertThat(result.get("server").get("port").asInt()).isEqualTo(8080);
        }

        @Test
        @DisplayName("parses nested TOML tables")
        void parsesNestedTables() {
            final String toml = "[database.connection]\nurl = \"jdbc:mysql://localhost\"";

            final JsonNode result = handler.parse(toml);

            assertThat(result.get("database").get("connection").get("url").asText())
                    .isEqualTo("jdbc:mysql://localhost");
        }

        @Test
        @DisplayName("throws FormatParseException on invalid TOML")
        void throwsOnInvalidToml() {
            final String invalidToml = "key = [invalid";

            assertThatThrownBy(() -> handler.parse(invalidToml))
                    .isInstanceOf(FormatParseException.class)
                    .hasMessageContaining("Failed to parse TOML");
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
        @DisplayName("parses TOML with boolean values")
        void parsesBooleanValues() {
            final String toml = "enabled = true\ndisabled = false";
            final JsonNode result = handler.parse(toml);
            assertThat(result.get("enabled").asBoolean()).isTrue();
            assertThat(result.get("disabled").asBoolean()).isFalse();
        }

        @Test
        @DisplayName("parses TOML with array")
        void parsesArray() {
            final String toml = "numbers = [1, 2, 3]";
            final JsonNode result = handler.parse(toml);
            assertThat(result.get("numbers").isArray()).isTrue();
            assertThat(result.get("numbers")).hasSize(3);
            assertThat(result.get("numbers").get(0).asInt()).isEqualTo(1);
        }

        @Test
        @DisplayName("parses TOML with string array")
        void parsesStringArray() {
            final String toml = "tags = [\"alpha\", \"beta\", \"gamma\"]";
            final JsonNode result = handler.parse(toml);
            assertThat(result.get("tags").isArray()).isTrue();
            assertThat(result.get("tags").get(0).asText()).isEqualTo("alpha");
        }

        @Test
        @DisplayName("parses TOML with inline table")
        void parsesInlineTable() {
            final String toml = "point = { x = 1, y = 2 }";
            final JsonNode result = handler.parse(toml);
            assertThat(result.get("point").isObject()).isTrue();
            assertThat(result.get("point").get("x").asInt()).isEqualTo(1);
            assertThat(result.get("point").get("y").asInt()).isEqualTo(2);
        }

        @Test
        @DisplayName("parses TOML with multiline basic string")
        void parsesMultilineString() {
            final String toml = "description = \"\"\"\nThis is a\nmultiline string\"\"\"";
            final JsonNode result = handler.parse(toml);
            assertThat(result.get("description").asText()).contains("This is a");
        }

        @Test
        @DisplayName("parses TOML with floating point numbers")
        void parsesFloatingPoint() {
            final String toml = "pi = 3.14159\nnegative = -0.5";
            final JsonNode result = handler.parse(toml);
            assertThat(result.get("pi").asDouble()).isEqualTo(3.14159);
            assertThat(result.get("negative").asDouble()).isEqualTo(-0.5);
        }
    }

    @Nested
    @DisplayName("serialize()")
    class Serialize {

        @Test
        @DisplayName("serializes JsonNode to TOML string")
        void serializesToTomlString() {
            final ObjectNode obj = MAPPER.createObjectNode();
            obj.put("name", "test");
            obj.put("value", 42);

            final String result = handler.serialize(obj);

            assertThat(result).isNotEmpty();
            assertThat(result).contains("name");
            assertThat(result).contains("test");
            assertThat(result).contains("value");
            assertThat(result).contains("42");
        }
    }

    @Nested
    @DisplayName("serializePretty()")
    class SerializePretty {

        @Test
        @DisplayName("serializes TOML with formatting")
        void serializesWithFormatting() {
            final ObjectNode obj = MAPPER.createObjectNode();
            obj.put("name", "test");
            final ObjectNode nested = MAPPER.createObjectNode();
            nested.put("key", "value");
            obj.set("config", nested);

            final String result = handler.serializePretty(obj);

            assertThat(result).contains("name");
            assertThat(result).contains("config");
        }
    }

    @Nested
    @DisplayName("Round-trip")
    class RoundTrip {

        @Test
        @DisplayName("parse -> serialize preserves data")
        void roundTripPreservesData() {
            final String original = "name = \"test\"\nvalue = 42\n\n[config]\nkey = \"value\"";

            final JsonNode parsed = handler.parse(original);
            final String serialized = handler.serializePretty(parsed);
            final JsonNode reparsed = handler.parse(serialized);

            assertThat(reparsed.get("name").asText()).isEqualTo(parsed.get("name").asText());
            assertThat(reparsed.get("value").asInt()).isEqualTo(parsed.get("value").asInt());
            assertThat(reparsed.get("config").get("key").asText())
                    .isEqualTo(parsed.get("config").get("key").asText());
        }
    }
}
