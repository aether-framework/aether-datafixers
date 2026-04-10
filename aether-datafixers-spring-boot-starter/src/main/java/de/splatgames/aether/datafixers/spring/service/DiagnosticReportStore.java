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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.diagnostic.MigrationReport;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe store for the most recent {@link MigrationReport} per domain.
 *
 * <p>This store retains only the latest diagnostic report for each domain,
 * replacing any previous report when a new migration with diagnostics
 * completes. It is used by the actuator endpoint to expose field-level
 * diagnostic information.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All operations use a {@link ConcurrentHashMap}
 * for safe concurrent access from multiple threads.</p>
 *
 * @author Erik Pförtner
 * @see MigrationReport
 * @see de.splatgames.aether.datafixers.spring.actuator.DataFixerEndpoint
 * @since 1.0.0
 */
public class DiagnosticReportStore {

    /**
     * Map of domain names to their most recent diagnostic reports.
     */
    private final Map<String, MigrationReport> reports = new ConcurrentHashMap<>();

    /**
     * Stores a diagnostic report for the specified domain.
     *
     * <p>Replaces any previously stored report for this domain.</p>
     *
     * @param domain the domain name, must not be {@code null}
     * @param report the migration report to store, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void store(@NotNull final String domain, @NotNull final MigrationReport report) {
        Preconditions.checkNotNull(domain, "domain must not be null");
        Preconditions.checkNotNull(report, "report must not be null");
        this.reports.put(domain, report);
    }

    /**
     * Retrieves the most recent diagnostic report for the specified domain.
     *
     * @param domain the domain name, must not be {@code null}
     * @return an Optional containing the report, or empty if no report is stored
     * @throws NullPointerException if domain is {@code null}
     */
    @NotNull
    public Optional<MigrationReport> get(@NotNull final String domain) {
        Preconditions.checkNotNull(domain, "domain must not be null");
        return Optional.ofNullable(this.reports.get(domain));
    }

    /**
     * Returns the set of domains that have stored diagnostic reports.
     *
     * @return an unmodifiable snapshot of domain names with reports
     */
    @NotNull
    public Set<String> getDomains() {
        return Set.copyOf(this.reports.keySet());
    }
}
