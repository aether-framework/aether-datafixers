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

package de.splatgames.aether.datafixers.testkit.assertion;

import de.splatgames.aether.datafixers.api.diagnostic.FieldOperation;
import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static de.splatgames.aether.datafixers.testkit.assertion.AetherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FieldOperationAssert}.
 */
@DisplayName("FieldOperationAssert")
class FieldOperationAssertTest {

    @Nested
    @DisplayName("hasOperationType()")
    class HasOperationType {

        @Test
        @DisplayName("passes when type matches")
        void passesWhenTypeMatches() {
            final FieldOperation op = FieldOperation.rename("oldName", "newName");
            assertThat(op).hasOperationType(FieldOperationType.RENAME);
        }

        @Test
        @DisplayName("fails when type does not match")
        void failsWhenTypeDoesNotMatch() {
            final FieldOperation op = FieldOperation.rename("oldName", "newName");
            assertThatThrownBy(() -> assertThat(op).hasOperationType(FieldOperationType.REMOVE))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining(FieldOperationType.RENAME.toString())
                    .hasMessageContaining(FieldOperationType.REMOVE.toString());
        }
    }

    @Nested
    @DisplayName("hasFieldPath()")
    class HasFieldPath {

        @Test
        @DisplayName("passes for single-segment path")
        void passesForSingleSegment() {
            final FieldOperation op = FieldOperation.rename("playerName", "name");
            assertThat(op).hasFieldPath("playerName");
        }

        @Test
        @DisplayName("passes for nested path")
        void passesForNestedPath() {
            final FieldOperation op = FieldOperation.removePath("position.x");
            assertThat(op).hasFieldPath("position", "x");
        }

        @Test
        @DisplayName("fails when path differs")
        void failsWhenPathDiffers() {
            final FieldOperation op = FieldOperation.rename("a", "b");
            assertThatThrownBy(() -> assertThat(op).hasFieldPath("c"))
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    @DisplayName("hasFieldPathString()")
    class HasFieldPathString {

        @Test
        @DisplayName("passes when dot-notation matches")
        void passesWhenDotNotationMatches() {
            final FieldOperation op = FieldOperation.removePath("position.x");
            assertThat(op).hasFieldPathString("position.x");
        }

        @Test
        @DisplayName("fails on mismatch")
        void failsOnMismatch() {
            final FieldOperation op = FieldOperation.rename("a", "b");
            assertThatThrownBy(() -> assertThat(op).hasFieldPathString("c"))
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    @DisplayName("isNested() and isNotNested()")
    class Nesting {

        @Test
        @DisplayName("isNested passes for nested path")
        void isNestedPassesForNested() {
            final FieldOperation op = FieldOperation.removePath("position.x");
            assertThat(op).isNested();
        }

        @Test
        @DisplayName("isNotNested passes for top-level path")
        void isNotNestedPassesForTopLevel() {
            final FieldOperation op = FieldOperation.remove("name");
            assertThat(op).isNotNested();
        }

        @Test
        @DisplayName("isNested fails for top-level path")
        void isNestedFailsForTopLevel() {
            final FieldOperation op = FieldOperation.remove("name");
            assertThatThrownBy(() -> assertThat(op).isNested())
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    @DisplayName("hasTargetFieldName() and hasNoTargetFieldName()")
    class TargetFieldName {

        @Test
        @DisplayName("hasTargetFieldName passes when target matches")
        void hasTargetFieldNamePasses() {
            final FieldOperation op = FieldOperation.rename("a", "b");
            assertThat(op).hasTargetFieldName("b");
        }

        @Test
        @DisplayName("hasTargetFieldName fails when target differs")
        void hasTargetFieldNameFailsOnDifferent() {
            final FieldOperation op = FieldOperation.rename("a", "b");
            assertThatThrownBy(() -> assertThat(op).hasTargetFieldName("c"))
                    .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("hasNoTargetFieldName passes for remove operation")
        void hasNoTargetFieldNamePassesForRemove() {
            final FieldOperation op = FieldOperation.remove("deprecated");
            assertThat(op).hasNoTargetFieldName();
        }

        @Test
        @DisplayName("hasNoTargetFieldName fails for rename operation")
        void hasNoTargetFieldNameFailsForRename() {
            final FieldOperation op = FieldOperation.rename("a", "b");
            assertThatThrownBy(() -> assertThat(op).hasNoTargetFieldName())
                    .isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    @DisplayName("Chaining and null-safety")
    class ChainingAndNullSafety {

        @Test
        @DisplayName("assertions can be chained")
        void canBeChained() {
            final FieldOperation op = FieldOperation.rename("oldName", "newName");
            assertThat(op)
                    .hasOperationType(FieldOperationType.RENAME)
                    .hasFieldPath("oldName")
                    .hasTargetFieldName("newName")
                    .isNotNested();
        }

        @Test
        @DisplayName("fails fast on null actual")
        void failsFastOnNullActual() {
            assertThatThrownBy(() -> assertThat((FieldOperation) null).hasOperationType(FieldOperationType.RENAME))
                    .isInstanceOf(AssertionError.class);
        }
    }
}
