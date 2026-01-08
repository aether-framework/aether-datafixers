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

package de.splatgames.aether.datafixers.schematools.analysis;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.core.fix.DataFixRegistry;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry;
import de.splatgames.aether.datafixers.testkit.factory.MockSchemas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MigrationAnalyzer}.
 */
@DisplayName("MigrationAnalyzer")
class MigrationAnalyzerTest {

    private static final TypeReference PLAYER = new TypeReference("player");
    private static final TypeReference ENTITY = new TypeReference("entity");

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("forBootstrap creates analyzer from bootstrap")
        void forBootstrapCreatesAnalyzerFromBootstrap() {
            final DataFixerBootstrap bootstrap = new TestBootstrap();

            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(bootstrap);

            assertThat(analyzer).isNotNull();
        }

        @Test
        @DisplayName("forBootstrap throws on null bootstrap")
        void forBootstrapThrowsOnNullBootstrap() {
            assertThatThrownBy(() -> MigrationAnalyzer.forBootstrap(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("forRegistries creates analyzer from registries")
        void forRegistriesCreatesAnalyzerFromRegistries() {
            final SchemaRegistry schemaRegistry = new SimpleSchemaRegistry();
            final DataFixerBuilder builder = new DataFixerBuilder(new DataVersion(Integer.MAX_VALUE));

            final MigrationAnalyzer analyzer = MigrationAnalyzer.forRegistries(
                    schemaRegistry, builder.getFixRegistry()
            );

            assertThat(analyzer).isNotNull();
        }

        @Test
        @DisplayName("forRegistries throws on null schemaRegistry")
        void forRegistriesThrowsOnNullSchemaRegistry() {
            final DataFixerBuilder builder = new DataFixerBuilder(new DataVersion(Integer.MAX_VALUE));

            assertThatThrownBy(() -> MigrationAnalyzer.forRegistries(null, builder.getFixRegistry()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("forRegistries throws on null fixRegistry")
        void forRegistriesThrowsOnNullFixRegistry() {
            final SchemaRegistry schemaRegistry = new SimpleSchemaRegistry();

            assertThatThrownBy(() -> MigrationAnalyzer.forRegistries(schemaRegistry, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Fluent API")
    class FluentApi {

        @Test
        @DisplayName("from(int) sets source version")
        void fromIntSetsSourceVersion() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap());

            // Should not throw
            assertThat(analyzer.from(100)).isSameAs(analyzer);
        }

        @Test
        @DisplayName("from(DataVersion) sets source version")
        void fromDataVersionSetsSourceVersion() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap());

            assertThat(analyzer.from(new DataVersion(100))).isSameAs(analyzer);
        }

        @Test
        @DisplayName("from(DataVersion) throws on null")
        void fromDataVersionThrowsOnNull() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap());

            assertThatThrownBy(() -> analyzer.from((DataVersion) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("to(int) sets target version")
        void toIntSetsTargetVersion() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap());

            assertThat(analyzer.to(200)).isSameAs(analyzer);
        }

        @Test
        @DisplayName("to(DataVersion) sets target version")
        void toDataVersionSetsTargetVersion() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap());

            assertThat(analyzer.to(new DataVersion(200))).isSameAs(analyzer);
        }

        @Test
        @DisplayName("to(DataVersion) throws on null")
        void toDataVersionThrowsOnNull() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap());

            assertThatThrownBy(() -> analyzer.to((DataVersion) null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("includeFieldLevel sets flag")
        void includeFieldLevelSetsFlag() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap());

            assertThat(analyzer.includeFieldLevel(true)).isSameAs(analyzer);
        }
    }

    @Nested
    @DisplayName("analyze()")
    class AnalyzeMethod {

        @Test
        @DisplayName("throws when from version not set")
        void throwsWhenFromVersionNotSet() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .to(200);

            assertThatThrownBy(analyzer::analyze)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fromVersion");
        }

        @Test
        @DisplayName("throws when to version not set")
        void throwsWhenToVersionNotSet() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .from(100);

            assertThatThrownBy(analyzer::analyze)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("toVersion");
        }

        @Test
        @DisplayName("throws when from > to")
        void throwsWhenFromGreaterThanTo() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .from(200).to(100);

            assertThatThrownBy(analyzer::analyze)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("returns empty path when no schemas in range")
        void returnsEmptyPathWhenNoSchemasInRange() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .from(500).to(600);

            final MigrationPath path = analyzer.analyze();

            assertThat(path.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("returns path with steps for valid range")
        void returnsPathWithStepsForValidRange() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .from(100).to(200);

            final MigrationPath path = analyzer.analyze();

            assertThat(path).isNotNull();
            assertThat(path.sourceVersion()).isEqualTo(new DataVersion(100));
            assertThat(path.targetVersion()).isEqualTo(new DataVersion(200));
        }

        @Test
        @DisplayName("returns empty path when only one schema in range")
        void returnsEmptyPathWhenOnlyOneSchemaInRange() {
            final DataFixerBootstrap singleSchema = new DataFixerBootstrap() {
                @Override
                public void registerSchemas(final SchemaRegistry registry) {
                    registry.register(MockSchemas.builder(100)
                            .withType(PLAYER, Type.STRING)
                            .build());
                }

                @Override
                public void registerFixes(final FixRegistrar registrar) {
                }
            };

            final MigrationPath path = MigrationAnalyzer.forBootstrap(singleSchema)
                    .from(100).to(100)
                    .analyze();

            assertThat(path.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("detects type additions between schemas")
        void detectsTypeAdditionsBetweenSchemas() {
            final DataFixerBootstrap bootstrap = new DataFixerBootstrap() {
                @Override
                public void registerSchemas(final SchemaRegistry registry) {
                    final Schema v100 = MockSchemas.builder(100)
                            .withType(PLAYER, Type.STRING)
                            .build();
                    final Schema v200 = MockSchemas.builder(200)
                            .withParent(v100)
                            .withType(PLAYER, Type.STRING)
                            .withType(ENTITY, Type.STRING)
                            .build();
                    registry.register(v100);
                    registry.register(v200);
                }

                @Override
                public void registerFixes(final FixRegistrar registrar) {
                }
            };

            final MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyze();

            assertThat(path.steps()).hasSize(1);
            assertThat(path.affects(ENTITY)).isTrue();
        }
    }

    @Nested
    @DisplayName("analyzeCoverage()")
    class AnalyzeCoverageMethod {

        @Test
        @DisplayName("throws when from version not set")
        void throwsWhenFromVersionNotSet() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .to(200);

            assertThatThrownBy(analyzer::analyzeCoverage)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fromVersion");
        }

        @Test
        @DisplayName("throws when to version not set")
        void throwsWhenToVersionNotSet() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .from(100);

            assertThatThrownBy(analyzer::analyzeCoverage)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("toVersion");
        }

        @Test
        @DisplayName("returns fully covered when no schemas")
        void returnsFullyCoveredWhenNoSchemas() {
            final FixCoverage coverage = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .from(500).to(600)
                    .analyzeCoverage();

            assertThat(coverage.isFullyCovered()).isTrue();
        }

        @Test
        @DisplayName("returns coverage result for valid range")
        void returnsCoverageResultForValidRange() {
            final FixCoverage coverage = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .from(100).to(200)
                    .analyzeCoverage();

            assertThat(coverage).isNotNull();
            assertThat(coverage.sourceVersion()).isEqualTo(new DataVersion(100));
            assertThat(coverage.targetVersion()).isEqualTo(new DataVersion(200));
        }

        @Test
        @DisplayName("detects gaps for types without fixes")
        void detectsGapsForTypesWithoutFixes() {
            final DataFixerBootstrap bootstrap = new DataFixerBootstrap() {
                @Override
                public void registerSchemas(final SchemaRegistry registry) {
                    final Schema v100 = MockSchemas.builder(100)
                            .withType(PLAYER, Type.STRING)
                            .build();
                    final Schema v200 = MockSchemas.builder(200)
                            .withParent(v100)
                            .withType(PLAYER, Type.STRING)
                            .withType(ENTITY, Type.STRING) // Added without fix
                            .build();
                    registry.register(v100);
                    registry.register(v200);
                }

                @Override
                public void registerFixes(final FixRegistrar registrar) {
                    // No fixes registered
                }
            };

            final FixCoverage coverage = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyzeCoverage();

            assertThat(coverage.hasGaps()).isTrue();
            assertThat(coverage.gapsForType(ENTITY)).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("complete analysis workflow")
        void completeAnalysisWorkflow() {
            final MigrationAnalyzer analyzer = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .from(100).to(200)
                    .includeFieldLevel(true);

            // Analyze path
            final MigrationPath path = analyzer.from(100).to(200).analyze();
            assertThat(path).isNotNull();

            // Analyze coverage (reusing same analyzer)
            final FixCoverage coverage = analyzer.from(100).to(200).analyzeCoverage();
            assertThat(coverage).isNotNull();
        }

        @Test
        @DisplayName("same version returns empty results")
        void sameVersionReturnsEmptyResults() {
            final MigrationPath path = MigrationAnalyzer.forBootstrap(new TestBootstrap())
                    .from(100).to(100)
                    .analyze();

            assertThat(path.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("handles schema chain with multiple steps")
        void handlesSchemaChainWithMultipleSteps() {
            final DataFixerBootstrap bootstrap = new DataFixerBootstrap() {
                @Override
                public void registerSchemas(final SchemaRegistry registry) {
                    final Schema v100 = MockSchemas.builder(100)
                            .withType(PLAYER, Type.STRING)
                            .build();
                    final Schema v150 = MockSchemas.builder(150)
                            .withParent(v100)
                            .withType(PLAYER, Type.STRING)
                            .build();
                    final Schema v200 = MockSchemas.builder(200)
                            .withParent(v150)
                            .withType(PLAYER, Type.STRING)
                            .build();
                    registry.register(v100);
                    registry.register(v150);
                    registry.register(v200);
                }

                @Override
                public void registerFixes(final FixRegistrar registrar) {
                }
            };

            final MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyze();

            // Should have 2 steps: 100->150 and 150->200
            assertThat(path.stepCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("handles empty bootstrap")
        void handlesEmptyBootstrap() {
            final DataFixerBootstrap emptyBootstrap = new DataFixerBootstrap() {
                @Override
                public void registerSchemas(final SchemaRegistry registry) {
                }

                @Override
                public void registerFixes(final FixRegistrar registrar) {
                }
            };

            final MigrationPath path = MigrationAnalyzer.forBootstrap(emptyBootstrap)
                    .from(0).to(100)
                    .analyze();

            assertThat(path.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("schemas outside range are excluded")
        void schemasOutsideRangeAreExcluded() {
            final DataFixerBootstrap bootstrap = new DataFixerBootstrap() {
                @Override
                public void registerSchemas(final SchemaRegistry registry) {
                    registry.register(MockSchemas.builder(50).withType(PLAYER, Type.STRING).build());
                    registry.register(MockSchemas.builder(100).withType(PLAYER, Type.STRING).build());
                    registry.register(MockSchemas.builder(150).withType(PLAYER, Type.STRING).build());
                    registry.register(MockSchemas.builder(200).withType(PLAYER, Type.STRING).build());
                    registry.register(MockSchemas.builder(250).withType(PLAYER, Type.STRING).build());
                }

                @Override
                public void registerFixes(final FixRegistrar registrar) {
                }
            };

            final MigrationPath path = MigrationAnalyzer.forBootstrap(bootstrap)
                    .from(100).to(200)
                    .analyze();

            // Should only include 100, 150, 200 = 2 steps
            assertThat(path.stepCount()).isEqualTo(2);
            assertThat(path.firstStep().map(s -> s.sourceVersion().getVersion())).contains(100);
            assertThat(path.lastStep().map(s -> s.targetVersion().getVersion())).contains(200);
        }
    }

    /**
     * Test bootstrap implementation.
     */
    private static class TestBootstrap implements DataFixerBootstrap {

        @Override
        public void registerSchemas(final SchemaRegistry registry) {
            final Schema v100 = MockSchemas.builder(100)
                    .withType(PLAYER, Type.STRING)
                    .build();
            final Schema v200 = MockSchemas.builder(200)
                    .withParent(v100)
                    .withType(PLAYER, Type.STRING)
                    .build();
            registry.register(v100);
            registry.register(v200);
        }

        @Override
        public void registerFixes(final FixRegistrar registrar) {
            // No fixes for testing
        }
    }
}
