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

package de.splatgames.aether.datafixers.core.codec;

import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.codec.Codecs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SimpleCodecRegistry}.
 */
@DisplayName("SimpleCodecRegistry")
class SimpleCodecRegistryTest {

    private SimpleCodecRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleCodecRegistry();
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("register() stores codec by reference")
        void registerStoresCodecByReference() {
            TypeReference ref = new TypeReference("test");
            Codec<String> codec = Codecs.STRING;

            registry.register(ref, codec);

            assertThat(registry.has(ref)).isTrue();
            assertThat(registry.get(ref)).isSameAs(codec);
        }

        @Test
        @DisplayName("register() overwrites existing codec")
        void registerOverwritesExisting() {
            TypeReference ref = new TypeReference("test");
            Codec<String> codec1 = Codecs.STRING;
            Codec<Integer> codec2 = Codecs.INT;

            registry.register(ref, codec1);
            registry.register(ref, codec2);

            assertThat(registry.get(ref)).isSameAs(codec2);
        }

        @Test
        @DisplayName("register() rejects null reference")
        void registerRejectsNullReference() {
            assertThatThrownBy(() -> registry.register(null, Codecs.STRING))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("register() rejects null codec")
        void registerRejectsNullCodec() {
            assertThatThrownBy(() -> registry.register(new TypeReference("test"), null))
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
        @DisplayName("has() returns true for registered codec")
        void hasReturnsTrueForRegistered() {
            TypeReference ref = new TypeReference("test");
            registry.register(ref, Codecs.INT);

            assertThat(registry.has(ref)).isTrue();
        }

        @Test
        @DisplayName("require() throws for unknown reference")
        void requireThrowsForUnknown() {
            TypeReference unknown = new TypeReference("unknown");

            assertThatThrownBy(() -> registry.require(unknown))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("require() returns codec for known reference")
        void requireReturnsCodecForKnown() {
            TypeReference ref = new TypeReference("test");
            Codec<Double> codec = Codecs.DOUBLE;
            registry.register(ref, codec);

            Codec<?> result = registry.require(ref);

            assertThat(result).isSameAs(codec);
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
            registry.register(new TypeReference("test"), Codecs.STRING);
            registry.freeze();

            assertThat(registry.isFrozen()).isTrue();
        }

        @Test
        @DisplayName("register() throws after freeze")
        void registerThrowsAfterFreeze() {
            registry.freeze();

            assertThatThrownBy(() -> registry.register(new TypeReference("test"), Codecs.INT))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("frozen");
        }

        @Test
        @DisplayName("freeze() is idempotent")
        void freezeIsIdempotent() {
            TypeReference ref = new TypeReference("test");
            Codec<Boolean> codec = Codecs.BOOL;
            registry.register(ref, codec);
            registry.freeze();
            registry.freeze(); // Should not throw

            assertThat(registry.isFrozen()).isTrue();
            assertThat(registry.get(ref)).isSameAs(codec);
        }

        @Test
        @DisplayName("get() works after freeze")
        void getWorksAfterFreeze() {
            TypeReference ref = new TypeReference("test");
            Codec<Long> codec = Codecs.LONG;
            registry.register(ref, codec);
            registry.freeze();

            assertThat(registry.get(ref)).isSameAs(codec);
        }

        @Test
        @DisplayName("has() works after freeze")
        void hasWorksAfterFreeze() {
            TypeReference ref = new TypeReference("test");
            registry.register(ref, Codecs.FLOAT);
            registry.freeze();

            assertThat(registry.has(ref)).isTrue();
            assertThat(registry.has(new TypeReference("other"))).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Codecs")
    class MultipleCodecs {

        @Test
        @DisplayName("can store multiple codecs")
        void canStoreMultipleCodecs() {
            TypeReference intRef = new TypeReference("int");
            TypeReference stringRef = new TypeReference("string");
            TypeReference boolRef = new TypeReference("bool");

            registry.register(intRef, Codecs.INT);
            registry.register(stringRef, Codecs.STRING);
            registry.register(boolRef, Codecs.BOOL);

            assertThat(registry.has(intRef)).isTrue();
            assertThat(registry.has(stringRef)).isTrue();
            assertThat(registry.has(boolRef)).isTrue();
        }

        @Test
        @DisplayName("freeze preserves all codecs")
        void freezePreservesAllCodecs() {
            TypeReference intRef = new TypeReference("int");
            TypeReference longRef = new TypeReference("long");
            TypeReference doubleRef = new TypeReference("double");

            registry.register(intRef, Codecs.INT);
            registry.register(longRef, Codecs.LONG);
            registry.register(doubleRef, Codecs.DOUBLE);
            registry.freeze();

            assertThat(registry.get(intRef)).isSameAs(Codecs.INT);
            assertThat(registry.get(longRef)).isSameAs(Codecs.LONG);
            assertThat(registry.get(doubleRef)).isSameAs(Codecs.DOUBLE);
        }
    }
}
