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

package de.splatgames.aether.datafixers.core.schema;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SimpleSchemaRegistry}.
 */
@DisplayName("SimpleSchemaRegistry")
class SimpleSchemaRegistryTest {

    private SimpleSchemaRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleSchemaRegistry();
    }

    private Schema createSchema(int version) {
        return new Schema(new DataVersion(version), new SimpleTypeRegistry());
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("register(schema) stores schema by its version")
        void registerStoresSchemaByVersion() {
            Schema schema = createSchema(1);

            registry.register(schema);

            assertThat(registry.get(new DataVersion(1))).isSameAs(schema);
        }

        @Test
        @DisplayName("register(version, schema) stores schema at specified version")
        void registerWithVersionStoresSchema() {
            Schema schema = createSchema(5);

            registry.register(new DataVersion(5), schema);

            assertThat(registry.get(new DataVersion(5))).isSameAs(schema);
        }

        @Test
        @DisplayName("register(version, schema) throws on version mismatch")
        void registerWithVersionMismatchThrows() {
            Schema schema = createSchema(5);

            assertThatThrownBy(() -> registry.register(new DataVersion(10), schema))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mismatch");
        }

        @Test
        @DisplayName("register() rejects null schema")
        void registerRejectsNull() {
            assertThatThrownBy(() -> registry.register(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Floor Semantics")
    class FloorSemantics {

        @Test
        @DisplayName("get() returns exact match when available")
        void getReturnsExactMatch() {
            Schema v5 = createSchema(5);
            registry.register(v5);

            assertThat(registry.get(new DataVersion(5))).isSameAs(v5);
        }

        @Test
        @DisplayName("get() returns floor when exact match not available")
        void getReturnsFloor() {
            Schema v1 = createSchema(1);
            Schema v5 = createSchema(5);
            registry.register(v1);
            registry.register(v5);

            // Request v3, should get v1 (floor)
            assertThat(registry.get(new DataVersion(3))).isSameAs(v1);

            // Request v7, should get v5 (floor)
            assertThat(registry.get(new DataVersion(7))).isSameAs(v5);
        }

        @Test
        @DisplayName("get() returns null when no floor exists")
        void getReturnsNullWhenNoFloor() {
            Schema v5 = createSchema(5);
            registry.register(v5);

            // Request v3, which is before v5
            assertThat(registry.get(new DataVersion(3))).isNull();
        }

        @Test
        @DisplayName("get() returns null for empty registry")
        void getReturnsNullForEmptyRegistry() {
            assertThat(registry.get(new DataVersion(1))).isNull();
        }
    }

    @Nested
    @DisplayName("Require")
    class Require {

        @Test
        @DisplayName("require() returns schema when available")
        void requireReturnsSchemaWhenAvailable() {
            Schema schema = createSchema(3);
            registry.register(schema);

            assertThat(registry.require(new DataVersion(3))).isSameAs(schema);
        }

        @Test
        @DisplayName("require() throws when no schema available")
        void requireThrowsWhenNoSchemaAvailable() {
            assertThatThrownBy(() -> registry.require(new DataVersion(1)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("require() uses floor semantics")
        void requireUsesFloorSemantics() {
            Schema v1 = createSchema(1);
            registry.register(v1);

            // Request v5, should get v1 (floor)
            assertThat(registry.require(new DataVersion(5))).isSameAs(v1);
        }
    }

    @Nested
    @DisplayName("isEmpty and latest")
    class IsEmptyAndLatest {

        @Test
        @DisplayName("isEmpty() returns true for new registry")
        void isEmptyReturnsTrueForNewRegistry() {
            assertThat(registry.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("isEmpty() returns false after registration")
        void isEmptyReturnsFalseAfterRegistration() {
            registry.register(createSchema(1));

            assertThat(registry.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("latest() throws for empty registry")
        void latestThrowsForEmptyRegistry() {
            assertThatThrownBy(() -> registry.latest())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("latest() returns highest version schema")
        void latestReturnsHighestVersionSchema() {
            Schema v1 = createSchema(1);
            Schema v5 = createSchema(5);
            Schema v3 = createSchema(3);

            // Register out of order
            registry.register(v3);
            registry.register(v1);
            registry.register(v5);

            assertThat(registry.latest()).isSameAs(v5);
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
            registry.register(createSchema(1));
            registry.freeze();

            assertThat(registry.isFrozen()).isTrue();
        }

        @Test
        @DisplayName("register() throws after freeze")
        void registerThrowsAfterFreeze() {
            registry.freeze();

            assertThatThrownBy(() -> registry.register(createSchema(1)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("frozen");
        }

        @Test
        @DisplayName("freeze() is idempotent")
        void freezeIsIdempotent() {
            Schema schema = createSchema(1);
            registry.register(schema);
            registry.freeze();
            registry.freeze(); // Should not throw

            assertThat(registry.isFrozen()).isTrue();
            assertThat(registry.get(new DataVersion(1))).isSameAs(schema);
        }

        @Test
        @DisplayName("get() works after freeze")
        void getWorksAfterFreeze() {
            Schema schema = createSchema(5);
            registry.register(schema);
            registry.freeze();

            assertThat(registry.get(new DataVersion(5))).isSameAs(schema);
            assertThat(registry.get(new DataVersion(10))).isSameAs(schema); // floor
        }

        @Test
        @DisplayName("latest() works after freeze")
        void latestWorksAfterFreeze() {
            Schema v1 = createSchema(1);
            Schema v10 = createSchema(10);
            registry.register(v1);
            registry.register(v10);
            registry.freeze();

            assertThat(registry.latest()).isSameAs(v10);
        }
    }
}
