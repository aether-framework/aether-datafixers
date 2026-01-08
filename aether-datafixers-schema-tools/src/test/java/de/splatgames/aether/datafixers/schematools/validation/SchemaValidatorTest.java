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
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
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
 * Unit tests for {@link SchemaValidator}.
 */
@DisplayName("SchemaValidator")
class SchemaValidatorTest {

    private static final TypeReference PLAYER = new TypeReference("player");

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("forSchema() creates validator for single schema")
        void forSchemaCreatesValidatorForSingleSchema() {
            final Schema schema = MockSchemas.minimal(100);

            final SchemaValidator validator = SchemaValidator.forSchema(schema);

            assertThat(validator).isNotNull();
        }

        @Test
        @DisplayName("forSchema() throws on null")
        void forSchemaThrowsOnNull() {
            assertThatThrownBy(() -> SchemaValidator.forSchema(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("forRegistry() creates validator for registry")
        void forRegistryCreatesValidatorForRegistry() {
            final SchemaRegistry registry = MockSchemas.chainMinimal(100, 200);

            final SchemaValidator validator = SchemaValidator.forRegistry(registry);

            assertThat(validator).isNotNull();
        }

        @Test
        @DisplayName("forRegistry() throws on null")
        void forRegistryThrowsOnNull() {
            assertThatThrownBy(() -> SchemaValidator.forRegistry(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("forBootstrap() creates validator from bootstrap")
        void forBootstrapCreatesValidatorFromBootstrap() {
            final DataFixerBootstrap bootstrap = new TestBootstrap();

            final SchemaValidator validator = SchemaValidator.forBootstrap(bootstrap);

            assertThat(validator).isNotNull();
        }

        @Test
        @DisplayName("forBootstrap() throws on null")
        void forBootstrapThrowsOnNull() {
            assertThatThrownBy(() -> SchemaValidator.forBootstrap(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("validateStructure()")
    class ValidateStructureTests {

        @Test
        @DisplayName("validates single schema structure")
        void validatesSingleSchemaStructure() {
            final Schema schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();

            final ValidationResult result = SchemaValidator.forSchema(schema)
                    .validateStructure()
                    .validate();

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("detects empty schema warning")
        void detectsEmptySchemaWarning() {
            final Schema schema = MockSchemas.minimal(100);

            final ValidationResult result = SchemaValidator.forSchema(schema)
                    .validateStructure()
                    .validate();

            assertThat(result.hasWarnings()).isTrue();
        }

        @Test
        @DisplayName("validates registry structure")
        void validatesRegistryStructure() {
            final Schema schema1 = MockSchemas.builder(100).withType(PLAYER, Type.STRING).build();
            final Schema schema2 = MockSchemas.builder(200).withParent(schema1).withType(PLAYER, Type.STRING).build();
            final SchemaRegistry registry = MockSchemas.chain(schema1, schema2);

            final ValidationResult result = SchemaValidator.forRegistry(registry)
                    .validateStructure()
                    .validate();

            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("validateConventions()")
    class ValidateConventionsTests {

        @Test
        @DisplayName("validates with default relaxed rules")
        void validatesWithDefaultRelaxedRules() {
            final Schema schema = MockSchemas.builder(100)
                    .withType(new TypeReference("PlayerData"), Type.STRING)
                    .build();

            final ValidationResult result = SchemaValidator.forSchema(schema)
                    .validateConventions()
                    .validate();

            // Relaxed rules should not report PlayerData as error
            assertThat(result.hasErrors()).isFalse();
        }

        @Test
        @DisplayName("validates with strict rules")
        void validatesWithStrictRules() {
            final Schema schema = MockSchemas.builder(100)
                    .withType(new TypeReference("PlayerData"), Type.STRING)
                    .build();

            final ValidationResult result = SchemaValidator.forSchema(schema)
                    .validateConventions()
                    .withConventions(ConventionRules.STRICT)
                    .validate();

            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("withConventions() throws on null")
        void withConventionsThrowsOnNull() {
            final Schema schema = MockSchemas.minimal(100);

            assertThatThrownBy(() -> SchemaValidator.forSchema(schema)
                    .withConventions(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("validateFixCoverage()")
    class ValidateFixCoverageTests {

        @Test
        @DisplayName("throws when not using bootstrap")
        void throwsWhenNotUsingBootstrap() {
            final Schema schema = MockSchemas.minimal(100);

            assertThatThrownBy(() -> SchemaValidator.forSchema(schema)
                    .validateFixCoverage())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("bootstrap");
        }

        @Test
        @DisplayName("works with bootstrap")
        void worksWithBootstrap() {
            final DataFixerBootstrap bootstrap = new TestBootstrap();

            final ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
                    .validateFixCoverage()
                    .validate();

            // Should not throw
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("validateAll()")
    class ValidateAllTests {

        @Test
        @DisplayName("enables all validations for single schema")
        void enablesAllValidationsForSingleSchema() {
            final Schema schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();

            final ValidationResult result = SchemaValidator.forSchema(schema)
                    .validateAll()
                    .validate();

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("enables all validations for bootstrap")
        void enablesAllValidationsForBootstrap() {
            final DataFixerBootstrap bootstrap = new TestBootstrap();

            final ValidationResult result = SchemaValidator.forBootstrap(bootstrap)
                    .validateAll()
                    .validate();

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Fluent API Chaining")
    class FluentApiChaining {

        @Test
        @DisplayName("allows chaining multiple validations")
        void allowsChainingMultipleValidations() {
            final Schema schema = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();

            final ValidationResult result = SchemaValidator.forSchema(schema)
                    .validateStructure()
                    .validateConventions()
                    .withConventions(ConventionRules.RELAXED)
                    .validate();

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("validate() returns result")
        void validateReturnsResult() {
            final Schema schema = MockSchemas.minimal(100);

            final ValidationResult result = SchemaValidator.forSchema(schema)
                    .validate();

            assertThat(result).isNotNull();
            assertThat(result.issues()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("validates complex registry with all checks")
        void validatesComplexRegistryWithAllChecks() {
            final Schema v100 = MockSchemas.builder(100)
                    .withType(new TypeReference("player"), Type.STRING)
                    .build();
            final Schema v200 = MockSchemas.builder(200)
                    .withParent(v100)
                    .withType(new TypeReference("player"), Type.STRING)
                    .withType(new TypeReference("entity"), Type.STRING)
                    .build();
            final SchemaRegistry registry = MockSchemas.chain(v100, v200);

            final ValidationResult result = SchemaValidator.forRegistry(registry)
                    .validateStructure()
                    .validateConventions()
                    .withConventions(ConventionRules.STRICT)
                    .validate();

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("combines structure and convention issues")
        void combinesStructureAndConventionIssues() {
            final Schema schema = MockSchemas.builder(100)
                    .withType(new TypeReference("PlayerData"), Type.STRING)
                    .build();

            final ValidationResult result = SchemaValidator.forSchema(schema)
                    .validateStructure()
                    .validateConventions()
                    .withConventions(ConventionRules.STRICT)
                    .validate();

            // Empty schema warning + type name convention error
            assertThat(result.hasWarnings() || result.hasErrors()).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("validates without any checks enabled")
        void validatesWithoutAnyChecksEnabled() {
            final Schema schema = MockSchemas.minimal(100);

            final ValidationResult result = SchemaValidator.forSchema(schema)
                    .validate();

            assertThat(result.issues()).isEmpty();
        }

        @Test
        @DisplayName("validates empty registry")
        void validatesEmptyRegistry() {
            final SchemaRegistry registry = MockSchemas.chain();

            final ValidationResult result = SchemaValidator.forRegistry(registry)
                    .validateStructure()
                    .validate();

            assertThat(result).isNotNull();
        }
    }

    /**
     * Test bootstrap implementation for testing purposes.
     */
    private static class TestBootstrap implements DataFixerBootstrap {

        @Override
        public void registerSchemas(final SchemaRegistry registry) {
            registry.register(MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build());
            registry.register(MockSchemas.builder(200)
                    .withType(PLAYER, Type.STRING)
                    .build());
        }

        @Override
        public void registerFixes(final FixRegistrar registrar) {
            // No fixes for testing
        }
    }
}
