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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IssueSeverity}.
 */
@DisplayName("IssueSeverity")
class IssueSeverityTest {

    @Test
    @DisplayName("all expected severities exist")
    void allExpectedSeveritiesExist() {
        assertThat(IssueSeverity.values()).containsExactlyInAnyOrder(
                IssueSeverity.ERROR,
                IssueSeverity.WARNING,
                IssueSeverity.INFO
        );
    }

    @Test
    @DisplayName("ERROR has correct name")
    void errorHasCorrectName() {
        assertThat(IssueSeverity.ERROR.name()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("WARNING has correct name")
    void warningHasCorrectName() {
        assertThat(IssueSeverity.WARNING.name()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("INFO has correct name")
    void infoHasCorrectName() {
        assertThat(IssueSeverity.INFO.name()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("valueOf works for all severities")
    void valueOfWorksForAllSeverities() {
        assertThat(IssueSeverity.valueOf("ERROR")).isEqualTo(IssueSeverity.ERROR);
        assertThat(IssueSeverity.valueOf("WARNING")).isEqualTo(IssueSeverity.WARNING);
        assertThat(IssueSeverity.valueOf("INFO")).isEqualTo(IssueSeverity.INFO);
    }

    @Test
    @DisplayName("ordinal values are defined")
    void ordinalValuesAreDefined() {
        assertThat(IssueSeverity.ERROR.ordinal()).isGreaterThanOrEqualTo(0);
        assertThat(IssueSeverity.WARNING.ordinal()).isGreaterThanOrEqualTo(0);
        assertThat(IssueSeverity.INFO.ordinal()).isGreaterThanOrEqualTo(0);
    }
}
