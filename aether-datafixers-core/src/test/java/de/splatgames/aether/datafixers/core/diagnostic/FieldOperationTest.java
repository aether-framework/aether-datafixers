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

package de.splatgames.aether.datafixers.core.diagnostic;

import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FieldOperation}.
 */
@DisplayName("FieldOperation")
class FieldOperationTest {

    // -------------------------------------------------------------------------
    // Constructor Validation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorValidation {

        @Test
        @DisplayName("throws NullPointerException when operationType is null")
        void throwsWhenOperationTypeIsNull() {
            assertThatThrownBy(() -> new FieldOperation(null, List.of("field"), null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("operationType");
        }

        @Test
        @DisplayName("throws NullPointerException when fieldPath is null")
        void throwsWhenFieldPathIsNull() {
            assertThatThrownBy(() -> new FieldOperation(FieldOperationType.RENAME, null, null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("fieldPath");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when fieldPath is empty")
        void throwsWhenFieldPathIsEmpty() {
            assertThatThrownBy(() -> new FieldOperation(FieldOperationType.RENAME, List.of(), null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fieldPath");
        }
    }

    // -------------------------------------------------------------------------
    // Factory Methods — Top-Level Fields
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Factory Methods — Top-Level Fields")
    class TopLevelFactoryMethods {

        @Test
        @DisplayName("rename() creates RENAME operation with target")
        void renameCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.rename("old", "new");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.RENAME);
            assertThat(op.fieldPath()).containsExactly("old");
            assertThat(op.targetFieldName()).isEqualTo("new");
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("remove() creates REMOVE operation")
        void removeCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.remove("field");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.REMOVE);
            assertThat(op.fieldPath()).containsExactly("field");
            assertThat(op.targetFieldName()).isNull();
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("add() creates ADD operation")
        void addCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.add("field");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.ADD);
            assertThat(op.fieldPath()).containsExactly("field");
            assertThat(op.targetFieldName()).isNull();
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("transform() creates TRANSFORM operation")
        void transformCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.transform("field");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.TRANSFORM);
            assertThat(op.fieldPath()).containsExactly("field");
            assertThat(op.targetFieldName()).isNull();
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("set() creates SET operation")
        void setCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.set("field");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.SET);
            assertThat(op.fieldPath()).containsExactly("field");
            assertThat(op.targetFieldName()).isNull();
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("move() creates MOVE operation with parsed path and target")
        void moveCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.move("a.b", "c.d");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.MOVE);
            assertThat(op.fieldPath()).containsExactly("a", "b");
            assertThat(op.targetFieldName()).isEqualTo("c.d");
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("copy() creates COPY operation with parsed path and target")
        void copyCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.copy("a.b", "c.d");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.COPY);
            assertThat(op.fieldPath()).containsExactly("a", "b");
            assertThat(op.targetFieldName()).isEqualTo("c.d");
            assertThat(op.description()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // Factory Methods — Nested / Path-Based Fields
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Factory Methods — Path-Based Fields")
    class PathBasedFactoryMethods {

        @Test
        @DisplayName("renamePath() creates RENAME with parsed path")
        void renamePathCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.renamePath("pos.x", "posX");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.RENAME);
            assertThat(op.fieldPath()).containsExactly("pos", "x");
            assertThat(op.targetFieldName()).isEqualTo("posX");
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("removePath() creates REMOVE with parsed path")
        void removePathCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.removePath("pos.x");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.REMOVE);
            assertThat(op.fieldPath()).containsExactly("pos", "x");
            assertThat(op.targetFieldName()).isNull();
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("addPath() creates ADD with parsed path")
        void addPathCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.addPath("pos.w");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.ADD);
            assertThat(op.fieldPath()).containsExactly("pos", "w");
            assertThat(op.targetFieldName()).isNull();
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("transformPath() creates TRANSFORM with parsed path")
        void transformPathCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.transformPath("pos.x");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.TRANSFORM);
            assertThat(op.fieldPath()).containsExactly("pos", "x");
            assertThat(op.targetFieldName()).isNull();
            assertThat(op.description()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // Factory Methods — Structural Operations
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Factory Methods — Structural Operations")
    class StructuralFactoryMethods {

        @Test
        @DisplayName("group() creates GROUP operation with source fields as path")
        void groupCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.group("position", "x", "y", "z");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.GROUP);
            assertThat(op.fieldPath()).containsExactly("x", "y", "z");
            assertThat(op.targetFieldName()).isEqualTo("position");
            assertThat(op.description()).isNull();
        }

        @Test
        @DisplayName("flatten() creates FLATTEN operation")
        void flattenCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.flatten("position");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.FLATTEN);
            assertThat(op.fieldPath()).containsExactly("position");
            assertThat(op.targetFieldName()).isNull();
            assertThat(op.description()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // Factory Methods — Conditional Operations
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Factory Methods — Conditional Operations")
    class ConditionalFactoryMethods {

        @Test
        @DisplayName("conditional() creates CONDITIONAL operation with description")
        void conditionalCreatesCorrectOperation() {
            FieldOperation op = FieldOperation.conditional("field", "exists");

            assertThat(op.operationType()).isEqualTo(FieldOperationType.CONDITIONAL);
            assertThat(op.fieldPath()).containsExactly("field");
            assertThat(op.targetFieldName()).isNull();
            assertThat(op.description()).isEqualTo("exists");
        }
    }

    // -------------------------------------------------------------------------
    // Convenience Methods
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("fieldPathString() returns dot-notation for nested path")
        void fieldPathStringReturnsDotNotation() {
            FieldOperation op = FieldOperation.renamePath("position.x", "posX");

            assertThat(op.fieldPathString()).isEqualTo("position.x");
        }

        @Test
        @DisplayName("fieldPathString() returns single segment for top-level field")
        void fieldPathStringReturnsSingleSegment() {
            FieldOperation op = FieldOperation.remove("name");

            assertThat(op.fieldPathString()).isEqualTo("name");
        }

        @Test
        @DisplayName("isNested() returns true for multi-segment path")
        void isNestedReturnsTrueForMultiSegment() {
            FieldOperation op = FieldOperation.removePath("position.x");

            assertThat(op.isNested()).isTrue();
        }

        @Test
        @DisplayName("isNested() returns false for single-segment path")
        void isNestedReturnsFalseForSingleSegment() {
            FieldOperation op = FieldOperation.remove("name");

            assertThat(op.isNested()).isFalse();
        }

        @Test
        @DisplayName("toSummary() includes operationType and fieldPath")
        void toSummaryIncludesTypeAndPath() {
            FieldOperation op = FieldOperation.remove("name");

            assertThat(op.toSummary()).contains("REMOVE").contains("name");
        }

        @Test
        @DisplayName("toSummary() includes target when present")
        void toSummaryIncludesTarget() {
            FieldOperation op = FieldOperation.rename("old", "new");

            assertThat(op.toSummary()).contains("RENAME").contains("old").contains("new");
        }

        @Test
        @DisplayName("toSummary() includes description when present")
        void toSummaryIncludesDescription() {
            FieldOperation op = FieldOperation.conditional("field", "exists");

            assertThat(op.toSummary()).contains("CONDITIONAL").contains("field").contains("exists");
        }

        @Test
        @DisplayName("targetFieldNameOpt() returns Optional with value when present")
        void targetFieldNameOptReturnsValueWhenPresent() {
            FieldOperation op = FieldOperation.rename("old", "new");

            assertThat(op.targetFieldNameOpt()).isPresent().contains("new");
        }

        @Test
        @DisplayName("targetFieldNameOpt() returns empty Optional when absent")
        void targetFieldNameOptReturnsEmptyWhenAbsent() {
            FieldOperation op = FieldOperation.remove("field");

            assertThat(op.targetFieldNameOpt()).isEmpty();
        }

        @Test
        @DisplayName("descriptionOpt() returns Optional with value when present")
        void descriptionOptReturnsValueWhenPresent() {
            FieldOperation op = FieldOperation.conditional("field", "exists");

            assertThat(op.descriptionOpt()).isPresent().contains("exists");
        }

        @Test
        @DisplayName("descriptionOpt() returns empty Optional when absent")
        void descriptionOptReturnsEmptyWhenAbsent() {
            FieldOperation op = FieldOperation.remove("field");

            assertThat(op.descriptionOpt()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Path Parsing
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Path Parsing")
    class PathParsing {

        @Test
        @DisplayName("single segment: 'name' parses to ['name']")
        void singleSegmentParsesToSingleElement() {
            FieldOperation op = FieldOperation.removePath("name");

            assertThat(op.fieldPath()).containsExactly("name");
        }

        @Test
        @DisplayName("two segments: 'position.x' parses to ['position', 'x']")
        void twoSegmentsParseCorrectly() {
            FieldOperation op = FieldOperation.removePath("position.x");

            assertThat(op.fieldPath()).containsExactly("position", "x");
        }

        @Test
        @DisplayName("deep path: 'a.b.c.d' parses to ['a', 'b', 'c', 'd']")
        void deepPathParsesCorrectly() {
            FieldOperation op = FieldOperation.removePath("a.b.c.d");

            assertThat(op.fieldPath()).containsExactly("a", "b", "c", "d");
        }
    }

    // -------------------------------------------------------------------------
    // Null Checks on Factory Methods
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Null Checks on Factory Methods")
    class NullChecksOnFactoryMethods {

        @Test
        @DisplayName("rename() throws NPE when oldName is null")
        void renameThrowsWhenOldNameIsNull() {
            assertThatThrownBy(() -> FieldOperation.rename(null, "new"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rename() throws NPE when newName is null")
        void renameThrowsWhenNewNameIsNull() {
            assertThatThrownBy(() -> FieldOperation.rename("old", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("remove() throws NPE when fieldName is null")
        void removeThrowsWhenFieldNameIsNull() {
            assertThatThrownBy(() -> FieldOperation.remove(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("add() throws NPE when fieldName is null")
        void addThrowsWhenFieldNameIsNull() {
            assertThatThrownBy(() -> FieldOperation.add(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("transform() throws NPE when fieldName is null")
        void transformThrowsWhenFieldNameIsNull() {
            assertThatThrownBy(() -> FieldOperation.transform(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("set() throws NPE when fieldName is null")
        void setThrowsWhenFieldNameIsNull() {
            assertThatThrownBy(() -> FieldOperation.set(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("move() throws NPE when sourcePath is null")
        void moveThrowsWhenSourcePathIsNull() {
            assertThatThrownBy(() -> FieldOperation.move(null, "target"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("move() throws NPE when targetPath is null")
        void moveThrowsWhenTargetPathIsNull() {
            assertThatThrownBy(() -> FieldOperation.move("source", null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("copy() throws NPE when sourcePath is null")
        void copyThrowsWhenSourcePathIsNull() {
            assertThatThrownBy(() -> FieldOperation.copy(null, "target"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("conditional() throws NPE when fieldName is null")
        void conditionalThrowsWhenFieldNameIsNull() {
            assertThatThrownBy(() -> FieldOperation.conditional(null, "exists"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("conditional() throws NPE when conditionType is null")
        void conditionalThrowsWhenConditionTypeIsNull() {
            assertThatThrownBy(() -> FieldOperation.conditional("field", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
