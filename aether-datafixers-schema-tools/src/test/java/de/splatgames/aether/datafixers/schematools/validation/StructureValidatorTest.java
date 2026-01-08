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

package de.splatgames.aether.datafixers.schematools.validation;

import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StructureValidator}.
 */
@DisplayName("StructureValidator")
class StructureValidatorTest {

    private static final TypeReference PLAYER = new TypeReference("player");

    @Nested
    @DisplayName("validate() single schema")
    class ValidateSingleSchema {

        @Test
        @DisplayName("returns valid for schema with types")
        void returnsValidForSchemaWithTypes() {
            final Schema schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();

            final ValidationResult result = StructureValidator.validate(schema, null);

            assertThat(result.isValid()).isTrue();
            assertThat(result.hasWarnings()).isFalse();
        }

        @Test
        @DisplayName("warns for empty schema")
        void warnsForEmptySchema() {
            final Schema schema = MockSchemas.minimal(100);

            final ValidationResult result = StructureValidator.validate(schema, null);

            assertThat(result.hasWarnings()).isTrue();
            assertThat(result.byCode(StructureValidator.STRUCTURE_EMPTY_SCHEMA)).hasSize(1);
        }

        @Test
        @DisplayName("validates schema with parent")
        void validatesSchemaWithParent() {
            final Schema parent = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();
            final Schema child = MockSchemas.builder(200)
                    .withParent(parent)
                    .withType(PLAYER, Type.STRING)
                    .build();

            final ValidationResult result = StructureValidator.validate(child, null);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("throws on null schema")
        void throwsOnNullSchema() {
            assertThatThrownBy(() -> StructureValidator.validate(null, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("validateRegistry()")
    class ValidateRegistry {

        @Test
        @DisplayName("returns valid for proper registry")
        void returnsValidForProperRegistry() {
            final Schema schema1 = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();
            final Schema schema2 = MockSchemas.builder(200)
                    .withParent(schema1)
                    .withType(PLAYER, Type.STRING)
                    .build();

            final SchemaRegistry registry = MockSchemas.chain(schema1, schema2);
            final ValidationResult result = StructureValidator.validateRegistry(registry);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("warns for empty schemas in registry")
        void warnsForEmptySchemasInRegistry() {
            final Schema schema1 = MockSchemas.minimal(100);
            final Schema schema2 = MockSchemas.minimal(200);

            final SchemaRegistry registry = MockSchemas.chain(schema1, schema2);
            final ValidationResult result = StructureValidator.validateRegistry(registry);

            assertThat(result.byCode(StructureValidator.STRUCTURE_EMPTY_SCHEMA)).hasSize(2);
        }

        @Test
        @DisplayName("throws on null registry")
        void throwsOnNullRegistry() {
            assertThatThrownBy(() -> StructureValidator.validateRegistry(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Version Ordering Validation")
    class VersionOrderingValidation {

        @Test
        @DisplayName("detects parent version >= child version")
        void detectsParentVersionGreaterThanOrEqualChildVersion() {
            final Schema parent = MockSchemas.builder(200)
                    .withType(PLAYER, Type.STRING)
                    .build();
            final Schema child = MockSchemas.builder(100)
                    .withParent(parent)
                    .withType(PLAYER, Type.STRING)
                    .build();

            final ValidationResult result = StructureValidator.validate(child, null);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.byCode(StructureValidator.STRUCTURE_VERSION_ORDER)).isNotEmpty();
        }

        @Test
        @DisplayName("accepts parent version < child version")
        void acceptsParentVersionLessThanChildVersion() {
            final Schema parent = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();
            final Schema child = MockSchemas.builder(200)
                    .withParent(parent)
                    .withType(PLAYER, Type.STRING)
                    .build();

            final ValidationResult result = StructureValidator.validate(child, null);

            assertThat(result.byCode(StructureValidator.STRUCTURE_VERSION_ORDER)).isEmpty();
        }

        @Test
        @DisplayName("validates multi-level parent chain")
        void validatesMultiLevelParentChain() {
            final Schema v100 = MockSchemas.builder(100).withType(PLAYER, Type.STRING).build();
            final Schema v200 = MockSchemas.builder(200).withParent(v100).withType(PLAYER, Type.STRING).build();
            final Schema v300 = MockSchemas.builder(300).withParent(v200).withType(PLAYER, Type.STRING).build();

            final ValidationResult result = StructureValidator.validate(v300, null);

            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Issue Location and Context")
    class IssueLocationAndContext {

        @Test
        @DisplayName("empty schema issue has correct location")
        void emptySchemaIssueHasCorrectLocation() {
            final Schema schema = MockSchemas.minimal(123);

            final ValidationResult result = StructureValidator.validate(schema, null);

            assertThat(result.warnings()).isNotEmpty();
            assertThat(result.warnings().get(0).location()).contains("Schema@123");
        }

        @Test
        @DisplayName("version order issue contains context")
        void versionOrderIssueContainsContext() {
            final Schema parent = MockSchemas.builder(200).withType(PLAYER, Type.STRING).build();
            final Schema child = MockSchemas.builder(100).withParent(parent).withType(PLAYER, Type.STRING).build();

            final ValidationResult result = StructureValidator.validate(child, null);

            assertThat(result.errors()).isNotEmpty();
            final ValidationIssue issue = result.errors().get(0);
            assertThat(issue.context()).containsKey("parentVersion");
            assertThat(issue.context()).containsKey("childVersion");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles schema with no parent")
        void handlesSchemaWithNoParent() {
            final Schema schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();

            final ValidationResult result = StructureValidator.validate(schema, null);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("handles many schema versions")
        void handlesManySchemaVersions() {
            Schema previous = null;
            for (int v = 100; v <= 200; v += 10) {
                final Schema schema = MockSchemas.builder(v)
                        .withParent(previous)
                        .withType(PLAYER, Type.STRING)
                        .build();
                previous = schema;
            }

            final ValidationResult result = StructureValidator.validate(previous, null);

            assertThat(result.isValid()).isTrue();
        }
    }
}
