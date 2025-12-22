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

package de.splatgames.aether.datafixers.core.type;

import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.type.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SimpleTypeRegistry}.
 */
@DisplayName("SimpleTypeRegistry")
class SimpleTypeRegistryTest {

    private SimpleTypeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleTypeRegistry();
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("register() stores type by its reference")
        void registerStoresTypeByReference() {
            Type<String> stringType = Type.STRING;

            registry.register(stringType);

            assertThat(registry.has(stringType.reference())).isTrue();
            assertThat(registry.get(stringType.reference())).isSameAs(stringType);
        }

        @Test
        @DisplayName("register() overwrites existing type with same reference")
        void registerOverwritesExisting() {
            Type<String> stringType1 = Type.primitive("test", Type.STRING.codec());
            Type<String> stringType2 = Type.primitive("test", Type.STRING.codec());

            registry.register(stringType1);
            registry.register(stringType2);

            assertThat(registry.get(stringType1.reference())).isSameAs(stringType2);
        }

        @Test
        @DisplayName("register() rejects null type")
        void registerRejectsNull() {
            assertThatThrownBy(() -> registry.register(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Lookup")
    class Lookup {

        @Test
        @DisplayName("get() returns null for unknown reference")
        void getReturnsNullForUnknown() {
            TypeReference unknown = new TypeReference("unknown");

            assertThat(registry.get(unknown)).isNull();
        }

        @Test
        @DisplayName("has() returns false for unknown reference")
        void hasReturnsFalseForUnknown() {
            TypeReference unknown = new TypeReference("unknown");

            assertThat(registry.has(unknown)).isFalse();
        }

        @Test
        @DisplayName("has() returns true for registered type")
        void hasReturnsTrueForRegistered() {
            registry.register(Type.INT);

            assertThat(registry.has(Type.INT.reference())).isTrue();
        }

        @Test
        @DisplayName("require() throws for unknown reference")
        void requireThrowsForUnknown() {
            TypeReference unknown = new TypeReference("unknown");

            assertThatThrownBy(() -> registry.require(unknown))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("require() returns type for known reference")
        void requireReturnsTypeForKnown() {
            registry.register(Type.DOUBLE);

            Type<?> result = registry.require(Type.DOUBLE.reference());

            assertThat(result).isSameAs(Type.DOUBLE);
        }

        @Test
        @DisplayName("get() rejects null reference")
        void getRejectsNull() {
            assertThatThrownBy(() -> registry.get(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("has() rejects null reference")
        void hasRejectsNull() {
            assertThatThrownBy(() -> registry.has(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Freeze Behavior")
    class FreezeBehavior {

        @Test
        @DisplayName("isFrozen() returns false initially")
        void isFrozenReturnsFalseInitially() {
            assertThat(registry.isFrozen()).isFalse();
        }

        @Test
        @DisplayName("freeze() makes registry immutable")
        void freezeMakesRegistryImmutable() {
            registry.register(Type.STRING);
            registry.freeze();

            assertThat(registry.isFrozen()).isTrue();
        }

        @Test
        @DisplayName("register() throws after freeze")
        void registerThrowsAfterFreeze() {
            registry.freeze();

            assertThatThrownBy(() -> registry.register(Type.INT))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("frozen");
        }

        @Test
        @DisplayName("freeze() is idempotent")
        void freezeIsIdempotent() {
            registry.register(Type.BOOL);
            registry.freeze();
            registry.freeze(); // Should not throw

            assertThat(registry.isFrozen()).isTrue();
            assertThat(registry.get(Type.BOOL.reference())).isSameAs(Type.BOOL);
        }

        @Test
        @DisplayName("get() works after freeze")
        void getWorksAfterFreeze() {
            registry.register(Type.LONG);
            registry.freeze();

            assertThat(registry.get(Type.LONG.reference())).isSameAs(Type.LONG);
        }

        @Test
        @DisplayName("has() works after freeze")
        void hasWorksAfterFreeze() {
            registry.register(Type.FLOAT);
            registry.freeze();

            assertThat(registry.has(Type.FLOAT.reference())).isTrue();
            assertThat(registry.has(new TypeReference("other"))).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Types")
    class MultipleTypes {

        @Test
        @DisplayName("can store multiple types")
        void canStoreMultipleTypes() {
            registry.register(Type.INT);
            registry.register(Type.STRING);
            registry.register(Type.BOOL);

            assertThat(registry.has(Type.INT.reference())).isTrue();
            assertThat(registry.has(Type.STRING.reference())).isTrue();
            assertThat(registry.has(Type.BOOL.reference())).isTrue();
        }

        @Test
        @DisplayName("freeze preserves all types")
        void freezePreservesAllTypes() {
            registry.register(Type.INT);
            registry.register(Type.LONG);
            registry.register(Type.DOUBLE);
            registry.freeze();

            assertThat(registry.get(Type.INT.reference())).isSameAs(Type.INT);
            assertThat(registry.get(Type.LONG.reference())).isSameAs(Type.LONG);
            assertThat(registry.get(Type.DOUBLE.reference())).isSameAs(Type.DOUBLE);
        }
    }
}
