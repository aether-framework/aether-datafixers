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
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.splatgames.aether.datafixers.codec.xml.jackson.JacksonXmlOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link XmlJacksonFormatHandler}.
 *
 * @author Erik Pförtner
 * @since 0.5.0
 */
@DisplayName("XmlJacksonFormatHandler")
class XmlJacksonFormatHandlerTest {

    private static final XmlMapper MAPPER = new XmlMapper();
    private XmlJacksonFormatHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new XmlJacksonFormatHandler();
    }

    @Nested
    @DisplayName("formatId()")
    class FormatId {

        @Test
        @DisplayName("returns 'xml-jackson'")
        void returnsCorrectId() {
            assertThat(handler.formatId()).isEqualTo("xml-jackson");
        }
    }

    @Nested
    @DisplayName("description()")
    class Description {

        @Test
        @DisplayName("returns non-empty description containing 'XML' and 'Jackson'")
        void returnsDescription() {
            assertThat(handler.description()).isNotEmpty();
            assertThat(handler.description()).contains("XML");
            assertThat(handler.description()).contains("Jackson");
        }
    }

    @Nested
    @DisplayName("fileExtensions()")
    class FileExtensions {

        @Test
        @DisplayName("includes 'xml' extension")
        void includesXmlExtension() {
            assertThat(handler.fileExtensions()).contains("xml");
        }
    }

    @Nested
    @DisplayName("ops()")
    class Ops {

        @Test
        @DisplayName("returns JacksonXmlOps.INSTANCE")
        void returnsJacksonXmlOps() {
            assertThat(handler.ops()).isSameAs(JacksonXmlOps.INSTANCE);
        }
    }

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("parses simple XML document")
        void parsesSimpleDocument() {
            final String xml = "<root><name>test</name><value>42</value></root>";

            final JsonNode result = handler.parse(xml);

            assertThat(result.isObject()).isTrue();
            assertThat(result.get("name").asText()).isEqualTo("test");
            assertThat(result.get("value").asText()).isEqualTo("42");
        }

        @Test
        @DisplayName("parses nested XML elements")
        void parsesNestedElements() {
            final String xml = "<root><outer><inner><value>deep</value></inner></outer></root>";

            final JsonNode result = handler.parse(xml);

            assertThat(result.get("outer").get("inner").get("value").asText()).isEqualTo("deep");
        }

        @Test
        @DisplayName("throws FormatParseException on invalid XML")
        void throwsOnInvalidXml() {
            final String invalidXml = "<root><unclosed>";

            assertThatThrownBy(() -> handler.parse(invalidXml))
                    .isInstanceOf(FormatParseException.class)
                    .hasMessageContaining("Failed to parse XML");
        }

        @Test
        @DisplayName("throws FormatParseException on malformed XML")
        void throwsOnMalformedXml() {
            final String malformed = "<root><a></b></root>";

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
        @DisplayName("parses XML with attributes")
        void parsesAttributes() {
            final String xml = "<root id=\"123\"><name>test</name></root>";
            final JsonNode result = handler.parse(xml);
            assertThat(result.has("id") || result.has("name")).isTrue();
        }

        @Test
        @DisplayName("parses XML with text content in element")
        void parsesTextContent() {
            final String xml = "<root><message>Hello World</message></root>";
            final JsonNode result = handler.parse(xml);
            assertThat(result.get("message").asText()).contains("Hello");
        }

        @Test
        @DisplayName("parses XML with multiple child elements of same name")
        void parsesMultipleChildren() {
            final String xml = "<root><item>a</item><item>b</item><item>c</item></root>";
            final JsonNode result = handler.parse(xml);
            // Jackson XML may represent this as an array or repeated fields
            assertThat(result.has("item")).isTrue();
        }

        @Test
        @DisplayName("parses XML with CDATA section")
        void parsesCdata() {
            final String xml = "<root><data><![CDATA[Some <special> content]]></data></root>";
            final JsonNode result = handler.parse(xml);
            assertThat(result.get("data").asText()).contains("special");
        }

        @Test
        @DisplayName("parses XML with declaration")
        void parsesWithDeclaration() {
            final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><name>test</name></root>";
            final JsonNode result = handler.parse(xml);
            assertThat(result.get("name").asText()).isEqualTo("test");
        }

        @Test
        @DisplayName("parses empty XML element")
        void parsesEmptyElement() {
            final String xml = "<root><empty/></root>";
            final JsonNode result = handler.parse(xml);
            assertThat(result.has("empty")).isTrue();
        }
    }

    @Nested
    @DisplayName("serialize()")
    class Serialize {

        @Test
        @DisplayName("serializes JsonNode to XML string")
        void serializesToXmlString() {
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

        @Test
        @DisplayName("serializes to valid XML structure")
        void serializesToValidXml() {
            final ObjectNode obj = MAPPER.createObjectNode();
            obj.put("title", "Test Document");

            final String result = handler.serialize(obj);

            assertThat(result).contains("<");
            assertThat(result).contains(">");
        }
    }

    @Nested
    @DisplayName("serializePretty()")
    class SerializePretty {

        @Test
        @DisplayName("serializes XML with formatting")
        void serializesWithFormatting() {
            final ObjectNode obj = MAPPER.createObjectNode();
            obj.put("name", "test");
            final ObjectNode nested = MAPPER.createObjectNode();
            nested.put("key", "value");
            obj.set("config", nested);

            final String result = handler.serializePretty(obj);

            assertThat(result).contains("\n");
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
            final String original = "<root><name>test</name><value>42</value></root>";

            final JsonNode parsed = handler.parse(original);
            final String serialized = handler.serialize(parsed);
            final JsonNode reparsed = handler.parse(serialized);

            assertThat(reparsed.get("name").asText()).isEqualTo(parsed.get("name").asText());
            assertThat(reparsed.get("value").asText()).isEqualTo(parsed.get("value").asText());
        }
    }
}
