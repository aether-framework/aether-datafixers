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

import de.splatgames.aether.datafixers.api.diagnostic.FieldOperationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FieldOperationType}.
 */
@DisplayName("FieldOperationType")
class FieldOperationTypeTest {

    @Nested
    @DisplayName("Enum Constants")
    class EnumConstants {

        @Test
        @DisplayName("has exactly 10 values")
        void hasExactlyTenValues() {
            assertThat(FieldOperationType.values()).hasSize(10);
        }

        @Test
        @DisplayName("contains all expected constants")
        void containsAllExpectedConstants() {
            assertThat(FieldOperationType.values()).containsExactly(
                    FieldOperationType.RENAME,
                    FieldOperationType.REMOVE,
                    FieldOperationType.ADD,
                    FieldOperationType.TRANSFORM,
                    FieldOperationType.SET,
                    FieldOperationType.MOVE,
                    FieldOperationType.COPY,
                    FieldOperationType.GROUP,
                    FieldOperationType.FLATTEN,
                    FieldOperationType.CONDITIONAL
            );
        }
    }

    @Nested
    @DisplayName("valueOf()")
    class ValueOf {

        @Test
        @DisplayName("resolves RENAME")
        void resolvesRename() {
            assertThat(FieldOperationType.valueOf("RENAME")).isEqualTo(FieldOperationType.RENAME);
        }

        @Test
        @DisplayName("resolves REMOVE")
        void resolvesRemove() {
            assertThat(FieldOperationType.valueOf("REMOVE")).isEqualTo(FieldOperationType.REMOVE);
        }

        @Test
        @DisplayName("resolves ADD")
        void resolvesAdd() {
            assertThat(FieldOperationType.valueOf("ADD")).isEqualTo(FieldOperationType.ADD);
        }

        @Test
        @DisplayName("resolves TRANSFORM")
        void resolvesTransform() {
            assertThat(FieldOperationType.valueOf("TRANSFORM")).isEqualTo(FieldOperationType.TRANSFORM);
        }

        @Test
        @DisplayName("resolves SET")
        void resolvesSet() {
            assertThat(FieldOperationType.valueOf("SET")).isEqualTo(FieldOperationType.SET);
        }

        @Test
        @DisplayName("resolves MOVE")
        void resolvesMove() {
            assertThat(FieldOperationType.valueOf("MOVE")).isEqualTo(FieldOperationType.MOVE);
        }

        @Test
        @DisplayName("resolves COPY")
        void resolvesCopy() {
            assertThat(FieldOperationType.valueOf("COPY")).isEqualTo(FieldOperationType.COPY);
        }

        @Test
        @DisplayName("resolves GROUP")
        void resolvesGroup() {
            assertThat(FieldOperationType.valueOf("GROUP")).isEqualTo(FieldOperationType.GROUP);
        }

        @Test
        @DisplayName("resolves FLATTEN")
        void resolvesFlatten() {
            assertThat(FieldOperationType.valueOf("FLATTEN")).isEqualTo(FieldOperationType.FLATTEN);
        }

        @Test
        @DisplayName("resolves CONDITIONAL")
        void resolvesConditional() {
            assertThat(FieldOperationType.valueOf("CONDITIONAL")).isEqualTo(FieldOperationType.CONDITIONAL);
        }
    }
}
