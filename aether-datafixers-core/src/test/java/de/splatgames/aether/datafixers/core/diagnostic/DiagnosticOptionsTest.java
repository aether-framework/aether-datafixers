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

import de.splatgames.aether.datafixers.api.diagnostic.DiagnosticOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DiagnosticOptions}.
 */
@DisplayName("DiagnosticOptions")
class DiagnosticOptionsTest {

    @Nested
    @DisplayName("Presets")
    class Presets {

        @Test
        @DisplayName("defaults() returns full diagnostic options")
        void defaultsReturnFullOptions() {
            DiagnosticOptions options = DiagnosticOptions.defaults();

            assertThat(options.captureSnapshots()).isTrue();
            assertThat(options.captureRuleDetails()).isTrue();
            assertThat(options.maxSnapshotLength()).isEqualTo(DiagnosticOptions.DEFAULT_MAX_SNAPSHOT_LENGTH);
            assertThat(options.prettyPrintSnapshots()).isTrue();
        }

        @Test
        @DisplayName("minimal() returns minimal diagnostic options")
        void minimalReturnsMinimalOptions() {
            DiagnosticOptions options = DiagnosticOptions.minimal();

            assertThat(options.captureSnapshots()).isFalse();
            assertThat(options.captureRuleDetails()).isFalse();
            assertThat(options.maxSnapshotLength()).isEqualTo(0);
            assertThat(options.prettyPrintSnapshots()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("builder creates options with specified values")
        void builderCreatesOptionsWithSpecifiedValues() {
            DiagnosticOptions options = DiagnosticOptions.builder()
                    .captureSnapshots(false)
                    .captureRuleDetails(true)
                    .maxSnapshotLength(5000)
                    .prettyPrintSnapshots(false)
                    .build();

            assertThat(options.captureSnapshots()).isFalse();
            assertThat(options.captureRuleDetails()).isTrue();
            assertThat(options.maxSnapshotLength()).isEqualTo(5000);
            assertThat(options.prettyPrintSnapshots()).isFalse();
        }

        @Test
        @DisplayName("builder defaults to full diagnostic options")
        void builderDefaultsToFullOptions() {
            DiagnosticOptions options = DiagnosticOptions.builder().build();

            assertThat(options.captureSnapshots()).isTrue();
            assertThat(options.captureRuleDetails()).isTrue();
            assertThat(options.maxSnapshotLength()).isEqualTo(DiagnosticOptions.DEFAULT_MAX_SNAPSHOT_LENGTH);
            assertThat(options.prettyPrintSnapshots()).isTrue();
        }

        @Test
        @DisplayName("builder throws on negative maxSnapshotLength")
        void builderThrowsOnNegativeMaxSnapshotLength() {
            assertThatThrownBy(() -> DiagnosticOptions.builder().maxSnapshotLength(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("builder allows zero maxSnapshotLength for unlimited")
        void builderAllowsZeroMaxSnapshotLength() {
            DiagnosticOptions options = DiagnosticOptions.builder()
                    .maxSnapshotLength(0)
                    .build();

            assertThat(options.maxSnapshotLength()).isEqualTo(0);
        }
    }
}
