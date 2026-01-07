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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FormatRegistry")
class FormatRegistryTest {

    @Nested
    @DisplayName("get()")
    class Get {

        @Test
        @DisplayName("returns JsonGsonFormatHandler for 'json-gson'")
        void returnsGsonHandler() {
            final FormatHandler<?> handler = FormatRegistry.get("json-gson");

            assertThat(handler).isNotNull();
            assertThat(handler).isInstanceOf(JsonGsonFormatHandler.class);
        }

        @Test
        @DisplayName("returns JsonJacksonFormatHandler for 'json-jackson'")
        void returnsJacksonHandler() {
            final FormatHandler<?> handler = FormatRegistry.get("json-jackson");

            assertThat(handler).isNotNull();
            assertThat(handler).isInstanceOf(JsonJacksonFormatHandler.class);
        }

        @Test
        @DisplayName("returns null for unknown format")
        void returnsNullForUnknown() {
            final FormatHandler<?> handler = FormatRegistry.get("unknown-format");

            assertThat(handler).isNull();
        }

        @Test
        @DisplayName("is case-sensitive")
        void isCaseSensitive() {
            assertThat(FormatRegistry.get("JSON-GSON")).isNull();
            assertThat(FormatRegistry.get("Json-Gson")).isNull();
        }
    }

    @Nested
    @DisplayName("getByExtension()")
    class GetByExtension {

        @Test
        @DisplayName("returns handler for 'json' extension")
        void returnsHandlerForJsonExtension() {
            final FormatHandler<?> handler = FormatRegistry.getByExtension("json");

            assertThat(handler).isNotNull();
        }

        @Test
        @DisplayName("is case-insensitive for extensions")
        void isCaseInsensitiveForExtensions() {
            assertThat(FormatRegistry.getByExtension("JSON")).isNotNull();
            assertThat(FormatRegistry.getByExtension("Json")).isNotNull();
        }

        @Test
        @DisplayName("returns null for unknown extension")
        void returnsNullForUnknownExtension() {
            final FormatHandler<?> handler = FormatRegistry.getByExtension("xyz");

            assertThat(handler).isNull();
        }
    }

    @Nested
    @DisplayName("availableFormats()")
    class AvailableFormats {

        @Test
        @DisplayName("includes both built-in formats")
        void includesBuiltInFormats() {
            final List<String> formats = FormatRegistry.availableFormats();

            assertThat(formats).contains("json-gson", "json-jackson");
        }

        @Test
        @DisplayName("returns at least 2 formats")
        void returnsAtLeastTwoFormats() {
            assertThat(FormatRegistry.availableFormats()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("handlers()")
    class Handlers {

        @Test
        @DisplayName("returns all registered handlers")
        void returnsAllHandlers() {
            final List<FormatHandler<?>> handlers = FormatRegistry.handlers();

            assertThat(handlers).hasSizeGreaterThanOrEqualTo(2);
            assertThat(handlers).anyMatch(h -> h instanceof JsonGsonFormatHandler);
            assertThat(handlers).anyMatch(h -> h instanceof JsonJacksonFormatHandler);
        }
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("can register custom handler")
        void canRegisterCustomHandler() {
            // Create a mock handler
            final FormatHandler<String> customHandler = new FormatHandler<>() {
                @Override
                public String formatId() {
                    return "test-custom";
                }

                @Override
                public String description() {
                    return "Test handler";
                }

                @Override
                public String[] fileExtensions() {
                    return new String[]{"test"};
                }

                @Override
                public de.splatgames.aether.datafixers.api.dynamic.DynamicOps<String> ops() {
                    return null;
                }

                @Override
                public String parse(String content) {
                    return content;
                }

                @Override
                public String serialize(String data) {
                    return data;
                }
            };

            FormatRegistry.register(customHandler);

            assertThat(FormatRegistry.get("test-custom")).isSameAs(customHandler);
            assertThat(FormatRegistry.availableFormats()).contains("test-custom");
        }
    }
}
