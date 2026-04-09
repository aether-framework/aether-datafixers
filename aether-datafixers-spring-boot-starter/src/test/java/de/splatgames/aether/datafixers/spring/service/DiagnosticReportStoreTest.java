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

package de.splatgames.aether.datafixers.spring.service;

import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DiagnosticReportStore}.
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
@DisplayName("DiagnosticReportStore")
class DiagnosticReportStoreTest {

    private DiagnosticReportStore store;

    @BeforeEach
    void setUp() {
        store = new DiagnosticReportStore();
    }

    @Test
    @DisplayName("returns empty for unknown domain")
    void returnsEmptyForUnknownDomain() {
        assertThat(store.get("unknown")).isEmpty();
    }

    @Test
    @DisplayName("stores and retrieves report for domain")
    void storesAndRetrievesReport() {
        final MigrationReport report = mock(MigrationReport.class);
        store.store("game", report);

        assertThat(store.get("game")).contains(report);
    }

    @Test
    @DisplayName("replaces previous report for same domain")
    void replacesPreviousReport() {
        final MigrationReport first = mock(MigrationReport.class);
        final MigrationReport second = mock(MigrationReport.class);
        store.store("game", first);
        store.store("game", second);

        assertThat(store.get("game")).contains(second);
    }

    @Test
    @DisplayName("tracks domains with reports")
    void tracksDomains() {
        store.store("game", mock(MigrationReport.class));
        store.store("user", mock(MigrationReport.class));

        assertThat(store.getDomains()).containsExactlyInAnyOrder("game", "user");
    }

    @Test
    @DisplayName("rejects null domain")
    void rejectsNullDomain() {
        assertThatThrownBy(() -> store.store(null, mock(MigrationReport.class)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("rejects null report")
    void rejectsNullReport() {
        assertThatThrownBy(() -> store.store("game", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("rejects null domain on get")
    void rejectsNullDomainOnGet() {
        assertThatThrownBy(() -> store.get(null))
                .isInstanceOf(NullPointerException.class);
    }
}
