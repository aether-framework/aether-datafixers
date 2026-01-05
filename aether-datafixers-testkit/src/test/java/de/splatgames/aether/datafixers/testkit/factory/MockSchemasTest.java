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

package de.splatgames.aether.datafixers.testkit.factory;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockSchemas")
class MockSchemasTest {

    @Nested
    @DisplayName("minimal()")
    class MinimalSchema {

        @Test
        @DisplayName("creates schema with correct version (int)")
        void createsSchemaWithCorrectVersionInt() {
            final Schema schema = MockSchemas.minimal(100);

            assertThat(schema.version().getVersion()).isEqualTo(100);
        }

        @Test
        @DisplayName("creates schema with correct version (DataVersion)")
        void createsSchemaWithCorrectVersionDataVersion() {
            final Schema schema = MockSchemas.minimal(new DataVersion(200));

            assertThat(schema.version().getVersion()).isEqualTo(200);
        }

        @Test
        @DisplayName("creates schema with no parent by default")
        void createsSchemaWithNoParentByDefault() {
            final Schema schema = MockSchemas.minimal(100);

            assertThat(schema.parent()).isNull();
        }

        @Test
        @DisplayName("creates schema with specified parent")
        void createsSchemaWithSpecifiedParent() {
            final Schema parent = MockSchemas.minimal(100);
            final Schema child = MockSchemas.minimal(110, parent);

            assertThat(child.parent()).isSameAs(parent);
        }

        @Test
        @DisplayName("created schema has empty type registry")
        void createdSchemaHasEmptyTypeRegistry() {
            final Schema schema = MockSchemas.minimal(100);

            // Types should be queryable (won't throw), but empty
            assertThat(schema.types()).isNotNull();
        }
    }

    @Nested
    @DisplayName("chain()")
    class ChainSchemas {

        @Test
        @DisplayName("creates registry with all schemas")
        void createsRegistryWithAllSchemas() {
            final Schema s100 = MockSchemas.minimal(100);
            final Schema s110 = MockSchemas.minimal(110);
            final Schema s200 = MockSchemas.minimal(200);

            final SchemaRegistry registry = MockSchemas.chain(s100, s110, s200);

            assertThat(registry).isNotNull();
            assertThat(registry.get(new DataVersion(100))).isSameAs(s100);
            assertThat(registry.get(new DataVersion(110))).isSameAs(s110);
            assertThat(registry.get(new DataVersion(200))).isSameAs(s200);
        }

        @Test
        @DisplayName("created registry is frozen")
        void createdRegistryIsFrozen() {
            final Schema schema = MockSchemas.minimal(100);

            final SchemaRegistry registry = MockSchemas.chain(schema);

            assertThat(registry.isFrozen()).isTrue();
        }
    }

    @Nested
    @DisplayName("chainMinimal()")
    class ChainMinimalSchemas {

        @Test
        @DisplayName("creates registry with minimal schemas for versions")
        void createsRegistryWithMinimalSchemasForVersions() {
            final SchemaRegistry registry = MockSchemas.chainMinimal(100, 110, 200);

            assertThat(registry).isNotNull();
            assertThat(registry.get(new DataVersion(100))).isNotNull();
            assertThat(registry.get(new DataVersion(100)).version().getVersion()).isEqualTo(100);
            assertThat(registry.get(new DataVersion(110))).isNotNull();
            assertThat(registry.get(new DataVersion(110)).version().getVersion()).isEqualTo(110);
            assertThat(registry.get(new DataVersion(200))).isNotNull();
            assertThat(registry.get(new DataVersion(200)).version().getVersion()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("builder()")
    class SchemaBuilder {

        @Test
        @DisplayName("creates schema with correct version")
        void createsSchemaWithCorrectVersion() {
            final Schema schema = MockSchemas.builder(100).build();

            assertThat(schema.version().getVersion()).isEqualTo(100);
        }

        @Test
        @DisplayName("creates schema with correct version (DataVersion)")
        void createsSchemaWithCorrectVersionDataVersion() {
            final Schema schema = MockSchemas.builder(new DataVersion(200)).build();

            assertThat(schema.version().getVersion()).isEqualTo(200);
        }

        @Test
        @DisplayName("creates schema with specified parent")
        void createsSchemaWithSpecifiedParent() {
            final Schema parent = MockSchemas.minimal(100);
            final Schema child = MockSchemas.builder(110)
                    .withParent(parent)
                    .build();

            assertThat(child.parent()).isSameAs(parent);
        }

        @Test
        @DisplayName("creates schema with no parent by default")
        void createsSchemaWithNoParentByDefault() {
            final Schema schema = MockSchemas.builder(100).build();

            assertThat(schema.parent()).isNull();
        }
    }
}
