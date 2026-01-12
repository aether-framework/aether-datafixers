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

package de.splatgames.aether.datafixers.testkit.harness;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SchemaTester")
class SchemaTesterTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference WORLD = new TypeReference("world");
    private static final TypeReference ENTITY = new TypeReference("entity");

    @Nested
    @DisplayName("Version Validation")
    class VersionValidation {

        @Test
        @DisplayName("hasVersion passes when version matches (int)")
        void hasVersionPassesWithInt() {
            final var schema = MockSchemas.builder(100).build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .hasVersion(100)
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasVersion passes when version matches (DataVersion)")
        void hasVersionPassesWithDataVersion() {
            final var schema = MockSchemas.builder(100).build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .hasVersion(new DataVersion(100))
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasVersion fails when version does not match")
        void hasVersionFailsOnMismatch() {
            final var schema = MockSchemas.builder(100).build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .hasVersion(200)
                            .verify()
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("version 100")
                    .hasMessageContaining("expected 200");
        }
    }

    @Nested
    @DisplayName("Type Validation")
    class TypeValidation {

        @Test
        @DisplayName("containsType passes when type exists")
        void containsTypePassesWhenExists() {
            final var schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.PASSTHROUGH)
                    .build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .containsType(PLAYER)
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("containsType passes with string type id")
        void containsTypePassesWithStringId() {
            final var schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.PASSTHROUGH)
                    .build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .containsType("player")
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("containsType fails when type does not exist")
        void containsTypeFailsWhenMissing() {
            final var schema = MockSchemas.builder(100).build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .containsType(PLAYER)
                            .verify()
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("does not contain type")
                    .hasMessageContaining("player");
        }

        @Test
        @DisplayName("containsTypes passes when all types exist")
        void containsTypesPassesWhenAllExist() {
            final var schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.PASSTHROUGH)
                    .withType(WORLD, Type.PASSTHROUGH)
                    .build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .containsTypes(PLAYER, WORLD)
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("containsTypes fails when some types missing")
        void containsTypesFailsWhenSomeMissing() {
            final var schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.PASSTHROUGH)
                    .build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .containsTypes(PLAYER, WORLD, ENTITY)
                            .verify()
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("missing types")
                    .hasMessageContaining("world")
                    .hasMessageContaining("entity");
        }

        @Test
        @DisplayName("doesNotContainType passes when type missing")
        void doesNotContainTypePassesWhenMissing() {
            final var schema = MockSchemas.builder(100).build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .doesNotContainType(PLAYER)
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("doesNotContainType passes with string type id")
        void doesNotContainTypePassesWithStringId() {
            final var schema = MockSchemas.builder(100).build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .doesNotContainType("player")
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("doesNotContainType fails when type exists")
        void doesNotContainTypeFailsWhenExists() {
            final var schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.PASSTHROUGH)
                    .build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .doesNotContainType(PLAYER)
                            .verify()
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("unexpectedly contains type")
                    .hasMessageContaining("player");
        }

        @Test
        @DisplayName("typeForReference validates type")
        void typeForReferenceValidatesType() {
            final var schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.PASSTHROUGH)
                    .build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .typeForReference(PLAYER, type -> {
                                assertThat(type).isNotNull();
                            })
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("typeForReference fails when type missing")
        void typeForReferenceFailsWhenMissing() {
            final var schema = MockSchemas.builder(100).build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .typeForReference(PLAYER, type -> {})
                            .verify()
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("does not contain type");
        }
    }

    @Nested
    @DisplayName("Parent Validation")
    class ParentValidation {

        @Test
        @DisplayName("hasParent passes when parent exists")
        void hasParentPassesWhenExists() {
            final var parent = MockSchemas.builder(100).build();
            final var schema = MockSchemas.builder(200).withParent(parent).build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .hasParent()
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasParent fails when no parent")
        void hasParentFailsWhenNoParent() {
            final var schema = MockSchemas.builder(100).build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .hasParent()
                            .verify()
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("has no parent");
        }

        @Test
        @DisplayName("hasNoParent passes when no parent")
        void hasNoParentPassesWhenNoParent() {
            final var schema = MockSchemas.builder(100).build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .hasNoParent()
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("hasNoParent fails when parent exists")
        void hasNoParentFailsWhenParentExists() {
            final var parent = MockSchemas.builder(100).build();
            final var schema = MockSchemas.builder(200).withParent(parent).build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .hasNoParent()
                            .verify()
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("has a parent");
        }

        @Test
        @DisplayName("inheritsFrom passes when correct parent")
        void inheritsFromPassesWhenCorrect() {
            final var parent = MockSchemas.builder(100).build();
            final var schema = MockSchemas.builder(200).withParent(parent).build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .inheritsFrom(parent)
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("inheritsFrom fails when no parent")
        void inheritsFromFailsWhenNoParent() {
            final var parent = MockSchemas.builder(100).build();
            final var schema = MockSchemas.builder(200).build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .inheritsFrom(parent)
                            .verify()
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("has no parent");
        }

        @Test
        @DisplayName("parentHasVersion passes when correct")
        void parentHasVersionPassesWhenCorrect() {
            final var parent = MockSchemas.builder(100).build();
            final var schema = MockSchemas.builder(200).withParent(parent).build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .parentHasVersion(100)
                            .verify()
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("parentHasVersion fails when no parent")
        void parentHasVersionFailsWhenNoParent() {
            final var schema = MockSchemas.builder(100).build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .parentHasVersion(50)
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("has no parent");
        }

        @Test
        @DisplayName("parentHasVersion fails when wrong version")
        void parentHasVersionFailsWhenWrongVersion() {
            final var parent = MockSchemas.builder(100).build();
            final var schema = MockSchemas.builder(200).withParent(parent).build();

            assertThatThrownBy(() ->
                    SchemaTester.forSchema(schema)
                            .parentHasVersion(50)
            ).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("expected version 50");
        }
    }

    @Nested
    @DisplayName("Fluent API")
    class FluentApi {

        @Test
        @DisplayName("schema() returns the schema being tested")
        void schemaReturnsSchema() {
            final var schema = MockSchemas.builder(100).build();

            assertThat(SchemaTester.forSchema(schema).schema()).isSameAs(schema);
        }

        @Test
        @DisplayName("verify() returns this for chaining")
        void verifyReturnsThis() {
            final var schema = MockSchemas.builder(100).build();
            final var tester = SchemaTester.forSchema(schema);

            assertThat(tester.verify()).isSameAs(tester);
        }

        @Test
        @DisplayName("supports method chaining")
        void supportsMethodChaining() {
            final var parent = MockSchemas.builder(100).withType(PLAYER, Type.PASSTHROUGH).build();
            final var schema = MockSchemas.builder(200)
                    .withParent(parent)
                    .withType(PLAYER, Type.PASSTHROUGH)
                    .withType(WORLD, Type.PASSTHROUGH)
                    .build();

            assertThatCode(() ->
                    SchemaTester.forSchema(schema)
                            .hasVersion(200)
                            .containsType(PLAYER)
                            .containsTypes(PLAYER, WORLD)
                            .doesNotContainType(ENTITY)
                            .hasParent()
                            .parentHasVersion(100)
                            .verify()
            ).doesNotThrowAnyException();
        }
    }
}
