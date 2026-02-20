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

package de.splatgames.aether.datafixers.api.optic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TestOps}.
 */
@DisplayName("TestOps")
class TestOpsTest {

    private final TestOps ops = TestOps.INSTANCE;

    @Nested
    @DisplayName("Java Object Creation")
    class JavaObjectCreation {

        @SuppressWarnings("deprecation")
        @Test
        @DisplayName("createObject() returns the value directly")
        void createObjectReturnsValueDirectly() {
            final String stringValue = "hello";
            assertThat(ops.createObject(stringValue)).isSameAs(stringValue);

            final Integer intValue = 42;
            assertThat(ops.createObject(intValue)).isSameAs(intValue);

            final List<String> listValue = List.of("a", "b");
            assertThat(ops.createObject(listValue)).isSameAs(listValue);

            final Map<String, Object> mapValue = Map.of("key", "value");
            assertThat(ops.createObject(mapValue)).isSameAs(mapValue);
        }

        @SuppressWarnings("deprecation")
        @Test
        @DisplayName("getObjectValue() returns the value as DataResult.success")
        void getObjectValueReturnsValueAsSuccess() {
            final String stringValue = "hello";
            assertThat(ops.getObjectValue(stringValue).result()).contains(stringValue);

            final Integer intValue = 42;
            assertThat(ops.getObjectValue(intValue).result()).contains(intValue);

            final List<String> listValue = List.of("a", "b");
            assertThat(ops.getObjectValue(listValue).result()).contains(listValue);

            final Map<String, Object> mapValue = Map.of("key", "value");
            assertThat(ops.getObjectValue(mapValue).result()).contains(mapValue);
        }
    }
}
