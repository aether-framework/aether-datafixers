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
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import de.splatgames.aether.datafixers.codec.yaml.jackson.JacksonYamlOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link YamlJacksonFormatHandler}.
 *
 * @author Erik Pförtner
 * @since 0.5.0
 */
@DisplayName("YamlJacksonFormatHandler")
class YamlJacksonFormatHandlerTest {

    private static final YAMLMapper MAPPER = new YAMLMapper();
    private YamlJacksonFormatHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new YamlJacksonFormatHandler();
    }

    @Nested
    @DisplayName("formatId()")
    class FormatId {

        @Test
        @DisplayName("returns 'yaml-jackson'")
        void returnsCorrectId() {
            assertThat(handler.formatId()).isEqualTo("yaml-jackson");
        }
    }

    @Nested
    @DisplayName("description()")
    class Description {

        @Test
        @DisplayName("returns non-empty description containing 'Jackson'")
        void returnsDescription() {
            assertThat(handler.description()).isNotEmpty();
            assertThat(handler.description()).contains("Jackson");
        }
    }

    @Nested
    @DisplayName("fileExtensions()")
    class FileExtensions {

        @Test
        @DisplayName("includes 'yaml' and 'yml' extensions")
        void includesYamlExtensions() {
            assertThat(handler.fileExtensions()).contains("yaml", "yml");
        }
    }

    @Nested
    @DisplayName("ops()")
    class Ops {

        @Test
        @DisplayName("returns JacksonYamlOps.INSTANCE")
        void returnsJacksonYamlOps() {
            assertThat(handler.ops()).isSameAs(JacksonYamlOps.INSTANCE);
        }
    }

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("parses simple YAML object")
        void parsesSimpleObject() {
            final String yaml = "name: test\nvalue: 42";

            final JsonNode result = handler.parse(yaml);

            assertThat(result.isObject()).isTrue();
            assertThat(result.get("name").asText()).isEqualTo("test");
            assertThat(result.get("value").asInt()).isEqualTo(42);
        }

        @Test
        @DisplayName("parses YAML array")
        void parsesArray() {
            final String yaml = "- 1\n- 2\n- 3";

            final JsonNode result = handler.parse(yaml);

            assertThat(result.isArray()).isTrue();
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("parses nested YAML")
        void parsesNestedYaml() {
            final String yaml = "outer:\n  inner:\n    value: deep";

            final JsonNode result = handler.parse(yaml);

            assertThat(result.get("outer").get("inner").get("value").asText()).isEqualTo("deep");
        }

        @Test
        @DisplayName("throws FormatParseException on invalid YAML")
        void throwsOnInvalidYaml() {
            final String invalidYaml = "key: [invalid";

            assertThatThrownBy(() -> handler.parse(invalidYaml))
                    .isInstanceOf(FormatParseException.class)
                    .hasMessageContaining("Failed to parse YAML");
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
        @DisplayName("parses empty YAML object")
        void parsesEmptyObject() {
            final JsonNode result = handler.parse("{}");
            assertThat(result.isObject()).isTrue();
            assertThat(result.size()).isZero();
        }

        @Test
        @DisplayName("parses empty YAML array")
        void parsesEmptyArray() {
            final JsonNode result = handler.parse("[]");
            assertThat(result.isArray()).isTrue();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("parses YAML with boolean values")
        void parsesBooleanValues() {
            final String yaml = "enabled: true\ndisabled: false";
            final JsonNode result = handler.parse(yaml);
            assertThat(result.get("enabled").asBoolean()).isTrue();
            assertThat(result.get("disabled").asBoolean()).isFalse();
        }

        @Test
        @DisplayName("parses YAML with null value")
        void parsesNullValue() {
            final String yaml = "value: null\nother: ~";
            final JsonNode result = handler.parse(yaml);
            assertThat(result.get("value").isNull()).isTrue();
            assertThat(result.get("other").isNull()).isTrue();
        }

        @Test
        @DisplayName("parses YAML with multiline string")
        void parsesMultilineString() {
            final String yaml = "description: |\n  This is a\n  multiline string";
            final JsonNode result = handler.parse(yaml);
            assertThat(result.get("description").asText()).contains("This is a");
            assertThat(result.get("description").asText()).contains("multiline string");
        }

        @Test
        @DisplayName("parses YAML with inline list")
        void parsesInlineList() {
            final String yaml = "items: [a, b, c]";
            final JsonNode result = handler.parse(yaml);
            assertThat(result.get("items").isArray()).isTrue();
            assertThat(result.get("items")).hasSize(3);
        }

        @Test
        @DisplayName("parses YAML with inline map")
        void parsesInlineMap() {
            final String yaml = "config: {key: value, num: 42}";
            final JsonNode result = handler.parse(yaml);
            assertThat(result.get("config").isObject()).isTrue();
            assertThat(result.get("config").get("key").asText()).isEqualTo("value");
            assertThat(result.get("config").get("num").asInt()).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("serialize()")
    class Serialize {

        @Test
        @DisplayName("serializes JsonNode to YAML string")
        void serializesToYamlString() {
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
        @DisplayName("serializes YAML with formatting")
        void serializesWithFormatting() {
            final ObjectNode obj = MAPPER.createObjectNode();
            obj.put("name", "test");
            final ObjectNode nested = MAPPER.createObjectNode();
            nested.put("key", "value");
            obj.set("nested", nested);

            final String result = handler.serializePretty(obj);

            assertThat(result).contains("\n");
            assertThat(result).contains("name");
            assertThat(result).contains("nested");
        }
    }

    @Nested
    @DisplayName("Round-trip")
    class RoundTrip {

        @Test
        @DisplayName("parse -> serialize preserves data")
        void roundTripPreservesData() {
            final String original = "name: test\nvalue: 42\nnested:\n  key: value";

            final JsonNode parsed = handler.parse(original);
            final String serialized = handler.serializePretty(parsed);
            final JsonNode reparsed = handler.parse(serialized);

            assertThat(reparsed.get("name").asText()).isEqualTo(parsed.get("name").asText());
            assertThat(reparsed.get("value").asInt()).isEqualTo(parsed.get("value").asInt());
            assertThat(reparsed.get("nested").get("key").asText())
                    .isEqualTo(parsed.get("nested").get("key").asText());
        }
    }
}
