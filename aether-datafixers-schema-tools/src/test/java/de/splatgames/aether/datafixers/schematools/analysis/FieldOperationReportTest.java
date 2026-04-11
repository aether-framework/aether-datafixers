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
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FieldOperationReport} and {@link FixFieldOperations}.
 */
@DisplayName("FieldOperationReport")
class FieldOperationReportTest {

    private static final DataVersion V100 = new DataVersion(100);
    private static final DataVersion V200 = new DataVersion(200);

    @Nested
    @DisplayName("Empty Report")
    class EmptyReport {

        @Test
        @DisplayName("empty() returns singleton with no fixes")
        void emptyReturnsSingleton() {
            final FieldOperationReport report = FieldOperationReport.empty();

            assertThat(report.isEmpty()).isTrue();
            assertThat(report.fixOperations()).isEmpty();
            assertThat(report.totalFieldOperationCount()).isZero();
            assertThat(report.introspectedFixCount()).isZero();
            assertThat(report.opaqueFixCount()).isZero();
        }

        @Test
        @DisplayName("empty report is fully introspectable")
        void emptyReportIsFullyIntrospectable() {
            assertThat(FieldOperationReport.empty().isFullyIntrospectable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builder produces report with single introspectable fix")
        void builderProducesSingleFixReport() {
            final FixFieldOperations fix = FixFieldOperations.introspectable(
                    "renameFix", V100, V200,
                    List.of(FieldOperation.rename("oldName", "newName"))
            );

            final FieldOperationReport report = FieldOperationReport.builder(V100, V200)
                    .addFix(fix)
                    .build();

            assertThat(report.fixOperations()).hasSize(1);
            assertThat(report.introspectedFixCount()).isEqualTo(1);
            assertThat(report.opaqueFixCount()).isZero();
            assertThat(report.totalFieldOperationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("builder aggregates multiple fixes")
        void builderAggregatesMultipleFixes() {
            final FixFieldOperations fixA = FixFieldOperations.introspectable(
                    "fixA", V100, new DataVersion(150),
                    List.of(
                            FieldOperation.rename("a", "b"),
                            FieldOperation.remove("c")
                    )
            );
            final FixFieldOperations fixB = FixFieldOperations.introspectable(
                    "fixB", new DataVersion(150), V200,
                    List.of(FieldOperation.add("d"))
            );

            final FieldOperationReport report = FieldOperationReport.builder(V100, V200)
                    .addFix(fixA)
                    .addFix(fixB)
                    .build();

            assertThat(report.fixOperations()).hasSize(2);
            assertThat(report.introspectedFixCount()).isEqualTo(2);
            assertThat(report.totalFieldOperationCount()).isEqualTo(3);
            assertThat(report.allFieldOperations()).hasSize(3);
        }

        @Test
        @DisplayName("builder counts opaque fixes separately")
        void builderCountsOpaqueFixes() {
            final FixFieldOperations introspectable = FixFieldOperations.introspectable(
                    "schemaFix", V100, V200,
                    List.of(FieldOperation.rename("a", "b"))
            );
            final FixFieldOperations opaque = FixFieldOperations.opaque(
                    "customFix", V100, V200);

            final FieldOperationReport report = FieldOperationReport.builder(V100, V200)
                    .addFix(introspectable)
                    .addFix(opaque)
                    .build();

            assertThat(report.introspectedFixCount()).isEqualTo(1);
            assertThat(report.opaqueFixCount()).isEqualTo(1);
            assertThat(report.totalFieldOperationCount()).isEqualTo(1);
            assertThat(report.isFullyIntrospectable()).isFalse();
        }

        @Test
        @DisplayName("builder rejects null entry")
        void builderRejectsNullEntry() {
            final FieldOperationReport.Builder builder = FieldOperationReport.builder(V100, V200);

            assertThatThrownBy(() -> builder.addFix(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("entry");
        }
    }

    @Nested
    @DisplayName("Filtering and Aggregation")
    class FilteringAndAggregation {

        private FieldOperationReport buildSampleReport() {
            return FieldOperationReport.builder(V100, V200)
                    .addFix(FixFieldOperations.introspectable(
                            "fix1", V100, new DataVersion(150),
                            List.of(
                                    FieldOperation.rename("oldName", "newName"),
                                    FieldOperation.remove("deprecated"),
                                    FieldOperation.add("health")
                            )))
                    .addFix(FixFieldOperations.introspectable(
                            "fix2", new DataVersion(150), V200,
                            List.of(
                                    FieldOperation.rename("xp", "experience"),
                                    FieldOperation.transform("stats")
                            )))
                    .build();
        }

        @Test
        @DisplayName("operationsOfType filters by type")
        void operationsOfTypeFilters() {
            final FieldOperationReport report = buildSampleReport();

            assertThat(report.operationsOfType(FieldOperationType.RENAME)).hasSize(2);
            assertThat(report.operationsOfType(FieldOperationType.REMOVE)).hasSize(1);
            assertThat(report.operationsOfType(FieldOperationType.ADD)).hasSize(1);
            assertThat(report.operationsOfType(FieldOperationType.TRANSFORM)).hasSize(1);
            assertThat(report.operationsOfType(FieldOperationType.MOVE)).isEmpty();
        }

        @Test
        @DisplayName("affectedFieldPaths collects unique dot-notation paths")
        void affectedFieldPaths() {
            final FieldOperationReport report = buildSampleReport();

            assertThat(report.affectedFieldPaths()).containsExactlyInAnyOrder(
                    "oldName", "deprecated", "health", "xp", "stats");
        }

        @Test
        @DisplayName("allFieldOperations preserves order")
        void allFieldOperationsPreservesOrder() {
            final FieldOperationReport report = buildSampleReport();

            assertThat(report.allFieldOperations()).hasSize(5);
            assertThat(report.allFieldOperations().get(0).fieldPathString()).isEqualTo("oldName");
            assertThat(report.allFieldOperations().get(4).fieldPathString()).isEqualTo("stats");
        }

        @Test
        @DisplayName("operationsOfType rejects null")
        void operationsOfTypeRejectsNull() {
            final FieldOperationReport report = buildSampleReport();

            assertThatThrownBy(() -> report.operationsOfType(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("FixFieldOperations")
    class FixFieldOperationsTests {

        @Test
        @DisplayName("introspectable factory sets flag and stores operations")
        void introspectableFactory() {
            final FixFieldOperations entry = FixFieldOperations.introspectable(
                    "renameFix", V100, V200,
                    List.of(FieldOperation.rename("a", "b"))
            );

            assertThat(entry.introspectable()).isTrue();
            assertThat(entry.count()).isEqualTo(1);
            assertThat(entry.hasOperations()).isTrue();
        }

        @Test
        @DisplayName("opaque factory creates entry with empty operations")
        void opaqueFactory() {
            final FixFieldOperations entry = FixFieldOperations.opaque("customFix", V100, V200);

            assertThat(entry.introspectable()).isFalse();
            assertThat(entry.count()).isZero();
            assertThat(entry.hasOperations()).isFalse();
            assertThat(entry.operations()).isEmpty();
        }

        @Test
        @DisplayName("toSummary distinguishes opaque and introspectable")
        void toSummaryDistinguishes() {
            final FixFieldOperations introspectable = FixFieldOperations.introspectable(
                    "renameFix", V100, V200,
                    List.of(FieldOperation.rename("a", "b")));
            final FixFieldOperations opaque = FixFieldOperations.opaque("customFix", V100, V200);

            assertThat(introspectable.toSummary()).contains("1 field operations");
            assertThat(opaque.toSummary()).contains("opaque");
        }

        @Test
        @DisplayName("constructor rejects null operations")
        void constructorRejectsNullOperations() {
            assertThatThrownBy(() -> new FixFieldOperations("name", V100, V200, null, true))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("operations list is defensively copied")
        void operationsAreDefensivelyCopied() {
            final java.util.ArrayList<FieldOperation> mutable = new java.util.ArrayList<>();
            mutable.add(FieldOperation.rename("a", "b"));

            final FixFieldOperations entry = FixFieldOperations.introspectable(
                    "fix", V100, V200, mutable);

            mutable.add(FieldOperation.rename("c", "d"));
            assertThat(entry.operations()).hasSize(1);
        }
    }
}
