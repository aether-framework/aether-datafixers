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

import de.splatgames.aether.datafixers.codec.yaml.snakeyaml.SnakeYamlOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link YamlSnakeYamlFormatHandler}.
 *
 * @author Erik Pförtner
 * @since 0.5.0
 */
@DisplayName("YamlSnakeYamlFormatHandler")
class YamlSnakeYamlFormatHandlerTest {

    private YamlSnakeYamlFormatHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new YamlSnakeYamlFormatHandler();
    }

    @Nested
    @DisplayName("formatId()")
    class FormatId {

        @Test
        @DisplayName("returns 'yaml-snakeyaml'")
        void returnsCorrectId() {
            assertThat(handler.formatId()).isEqualTo("yaml-snakeyaml");
        }
    }

    @Nested
    @DisplayName("description()")
    class Description {

        @Test
        @DisplayName("returns non-empty description containing 'SnakeYAML'")
        void returnsDescription() {
            assertThat(handler.description()).isNotEmpty();
            assertThat(handler.description()).contains("SnakeYAML");
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
        @DisplayName("returns SnakeYamlOps.INSTANCE")
        void returnsSnakeYamlOps() {
            assertThat(handler.ops()).isSameAs(SnakeYamlOps.INSTANCE);
        }
    }

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("parses simple YAML object")
        @SuppressWarnings("unchecked")
        void parsesSimpleObject() {
            final String yaml = "name: test\nvalue: 42";

            final Object result = handler.parse(yaml);

            assertThat(result).isInstanceOf(Map.class);
            final Map<String, Object> map = (Map<String, Object>) result;
            assertThat(map.get("name")).isEqualTo("test");
            assertThat(map.get("value")).isEqualTo(42);
        }

        @Test
        @DisplayName("parses YAML array")
        @SuppressWarnings("unchecked")
        void parsesArray() {
            final String yaml = "- 1\n- 2\n- 3";

            final Object result = handler.parse(yaml);

            assertThat(result).isInstanceOf(List.class);
            final List<Object> list = (List<Object>) result;
            assertThat(list).hasSize(3);
            assertThat(list).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("parses nested YAML")
        @SuppressWarnings("unchecked")
        void parsesNestedYaml() {
            final String yaml = "outer:\n  inner:\n    value: deep";

            final Object result = handler.parse(yaml);

            assertThat(result).isInstanceOf(Map.class);
            final Map<String, Object> map = (Map<String, Object>) result;
            final Map<String, Object> outer = (Map<String, Object>) map.get("outer");
            final Map<String, Object> inner = (Map<String, Object>) outer.get("inner");
            assertThat(inner.get("value")).isEqualTo("deep");
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
            final Object result = handler.parse("{}");
            assertThat(result).isInstanceOf(Map.class);
            assertThat((Map<?, ?>) result).isEmpty();
        }

        @Test
        @DisplayName("parses empty YAML array")
        void parsesEmptyArray() {
            final Object result = handler.parse("[]");
            assertThat(result).isInstanceOf(List.class);
            assertThat((List<?>) result).isEmpty();
        }

        @Test
        @DisplayName("parses YAML with boolean values")
        @SuppressWarnings("unchecked")
        void parsesBooleanValues() {
            final String yaml = "enabled: true\ndisabled: false";
            final Object result = handler.parse(yaml);
            final Map<String, Object> map = (Map<String, Object>) result;
            assertThat(map.get("enabled")).isEqualTo(true);
            assertThat(map.get("disabled")).isEqualTo(false);
        }

        @Test
        @DisplayName("parses YAML with null value")
        @SuppressWarnings("unchecked")
        void parsesNullValue() {
            final String yaml = "value: null\nother: ~";
            final Object result = handler.parse(yaml);
            final Map<String, Object> map = (Map<String, Object>) result;
            assertThat(map.get("value")).isNull();
            assertThat(map.get("other")).isNull();
        }

        @Test
        @DisplayName("parses YAML with multiline string")
        @SuppressWarnings("unchecked")
        void parsesMultilineString() {
            final String yaml = "description: |\n  This is a\n  multiline string";
            final Object result = handler.parse(yaml);
            final Map<String, Object> map = (Map<String, Object>) result;
            assertThat(map.get("description")).asString().contains("This is a");
            assertThat(map.get("description")).asString().contains("multiline string");
        }

        @Test
        @DisplayName("parses YAML with inline list")
        @SuppressWarnings("unchecked")
        void parsesInlineList() {
            final String yaml = "items: [a, b, c]";
            final Object result = handler.parse(yaml);
            final Map<String, Object> map = (Map<String, Object>) result;
            assertThat(map.get("items")).isInstanceOf(List.class);
            assertThat((List<String>) map.get("items")).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("parses YAML with inline map")
        @SuppressWarnings("unchecked")
        void parsesInlineMap() {
            final String yaml = "config: {key: value, num: 42}";
            final Object result = handler.parse(yaml);
            final Map<String, Object> map = (Map<String, Object>) result;
            final Map<String, Object> config = (Map<String, Object>) map.get("config");
            assertThat(config.get("key")).isEqualTo("value");
            assertThat(config.get("num")).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("serialize()")
    class Serialize {

        @Test
        @DisplayName("serializes map to compact YAML string")
        void serializesToCompactString() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "test");
            map.put("value", 42);

            final String result = handler.serialize(map);

            assertThat(result).isNotEmpty();
            assertThat(result).contains("name");
            assertThat(result).contains("test");
            assertThat(result).contains("value");
            assertThat(result).contains("42");
        }

        @Test
        @DisplayName("serializes list to YAML")
        void serializesList() {
            final List<Integer> list = List.of(1, 2, 3);

            final String result = handler.serialize(list);

            assertThat(result).contains("1");
            assertThat(result).contains("2");
            assertThat(result).contains("3");
        }
    }

    @Nested
    @DisplayName("serializePretty()")
    class SerializePretty {

        @Test
        @DisplayName("serializes YAML with block style")
        void serializesWithBlockStyle() {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", "test");
            final Map<String, Object> nested = new LinkedHashMap<>();
            nested.put("key", "value");
            map.put("nested", nested);

            final String result = handler.serializePretty(map);

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
        @SuppressWarnings("unchecked")
        void roundTripPreservesData() {
            final String original = "name: test\nvalue: 42\nnested:\n  key: value";

            final Object parsed = handler.parse(original);
            final String serialized = handler.serializePretty(parsed);
            final Object reparsed = handler.parse(serialized);

            assertThat(reparsed).isInstanceOf(Map.class);
            final Map<String, Object> originalMap = (Map<String, Object>) parsed;
            final Map<String, Object> reparsedMap = (Map<String, Object>) reparsed;
            assertThat(reparsedMap.get("name")).isEqualTo(originalMap.get("name"));
            assertThat(reparsedMap.get("value")).isEqualTo(originalMap.get("value"));
        }
    }
}
