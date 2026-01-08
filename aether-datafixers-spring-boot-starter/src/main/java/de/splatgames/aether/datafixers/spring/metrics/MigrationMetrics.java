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

package de.splatgames.aether.datafixers.spring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Micrometer-based metrics recorder for tracking data migration operations.
 *
 * <p>This class provides comprehensive observability for migration operations by recording
 * various metrics using Micrometer's instrumentation API. It tracks success/failure counts,
 * timing information, and migration complexity (version span), enabling detailed monitoring
 * and alerting for production deployments.</p>
 *
 * <h2>Integration</h2>
 * <p>This class is automatically configured when:</p>
 * <ul>
 *   <li>Micrometer is on the classpath</li>
 *   <li>A {@link MeterRegistry} bean exists (typically from Spring Boot Actuator)</li>
 *   <li>Metrics are enabled via {@code aether.datafixers.metrics.enabled=true}</li>
 * </ul>
 *
 * <h2>Recorded Metrics</h2>
 * <p>The following metrics are recorded with the prefix {@code aether.datafixers.migrations}:</p>
 *
 * <table border="1" cellpadding="5">
 *   <caption>Migration Metrics</caption>
 *   <tr>
 *     <th>Metric Name</th>
 *     <th>Type</th>
 *     <th>Tags</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>{@code .success}</td>
 *     <td>Counter</td>
 *     <td>domain</td>
 *     <td>Total count of successful migrations per domain</td>
 *   </tr>
 *   <tr>
 *     <td>{@code .failure}</td>
 *     <td>Counter</td>
 *     <td>domain, error_type</td>
 *     <td>Total count of failed migrations per domain and error class</td>
 *   </tr>
 *   <tr>
 *     <td>{@code .duration}</td>
 *     <td>Timer</td>
 *     <td>domain</td>
 *     <td>Execution time distribution of migrations per domain</td>
 *   </tr>
 *   <tr>
 *     <td>{@code .version.span}</td>
 *     <td>Distribution Summary</td>
 *     <td>domain</td>
 *     <td>Distribution of version spans (|toVersion - fromVersion|)</td>
 *   </tr>
 * </table>
 *
 * <h2>Tags Reference</h2>
 * <ul>
 *   <li><strong>{@code domain}</strong> - The DataFixer domain name (e.g., "default", "game", "user")</li>
 *   <li><strong>{@code error_type}</strong> - Simple class name of the exception that caused failure</li>
 * </ul>
 *
 * <h2>Usage with Prometheus</h2>
 * <p>When using Prometheus as a metrics backend, you can query these metrics with PromQL:</p>
 * <pre>{@code
 * # Success rate over the last hour
 * sum(rate(aether_datafixers_migrations_success_total[1h])) by (domain)
 *
 * # Average migration duration by domain
 * rate(aether_datafixers_migrations_duration_seconds_sum[5m])
 *   / rate(aether_datafixers_migrations_duration_seconds_count[5m])
 *
 * # Failure rate by error type
 * sum(rate(aether_datafixers_migrations_failure_total[1h])) by (domain, error_type)
 *
 * # 95th percentile migration duration
 * histogram_quantile(0.95, rate(aether_datafixers_migrations_duration_seconds_bucket[5m]))
 * }</pre>
 *
 * <h2>Grafana Dashboard Example</h2>
 * <p>A typical Grafana dashboard might include panels for:</p>
 * <ul>
 *   <li>Migration success/failure rate over time</li>
 *   <li>Average and p95 migration duration</li>
 *   <li>Version span distribution (identifying large migrations)</li>
 *   <li>Failure breakdown by error type</li>
 * </ul>
 *
 * <h2>Alerting Recommendations</h2>
 * <p>Consider setting up alerts for:</p>
 * <ul>
 *   <li>High failure rate ({@code > 1%} of migrations failing)</li>
 *   <li>Slow migrations ({@code p95 > 5 seconds})</li>
 *   <li>Large version spans ({@code avg > 100} versions, indicating outdated data)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All internal caches use {@link ConcurrentHashMap} to ensure
 * safe concurrent access from multiple threads. Micrometer meters are inherently thread-safe.</p>
 *
 * <h2>Memory Considerations</h2>
 * <p>Meters are cached per domain (and per error type for failure counters). In applications
 * with many domains or many different exception types, this could lead to increased memory
 * usage. The number of unique meters is bounded by:</p>
 * <ul>
 *   <li>Timers: 1 per domain</li>
 *   <li>Success counters: 1 per domain</li>
 *   <li>Failure counters: 1 per (domain, error_type) combination</li>
 *   <li>Version span summaries: 1 per domain</li>
 * </ul>
 *
 * @author Erik Pfoertner
 * @see io.micrometer.core.instrument.MeterRegistry
 * @see de.splatgames.aether.datafixers.spring.service.MigrationService
 * @see de.splatgames.aether.datafixers.spring.AetherDataFixersProperties.MetricsProperties
 * @since 0.4.0
 */
public class MigrationMetrics {

    /**
     * Prefix for all migration-related metrics.
     * All metric names start with this prefix followed by a dot and the specific metric name.
     */
    private static final String METRIC_PREFIX = "aether.datafixers.migrations";

    /**
     * Tag name for the DataFixer domain identifier.
     */
    private static final String TAG_DOMAIN = "domain";

    /**
     * Tag name for the source version (reserved for future use).
     */
    private static final String TAG_FROM_VERSION = "from_version";

    /**
     * Tag name for the target version (reserved for future use).
     */
    private static final String TAG_TO_VERSION = "to_version";

    /**
     * Tag name for the error type in failure metrics.
     */
    private static final String TAG_ERROR_TYPE = "error_type";

    /**
     * The Micrometer registry used to create and register all meters.
     */
    private final MeterRegistry registry;

    /**
     * Cache of Timer instances by domain name for efficient reuse.
     */
    private final ConcurrentHashMap<String, Timer> domainTimers = new ConcurrentHashMap<>();

    /**
     * Cache of success Counter instances by domain name for efficient reuse.
     */
    private final ConcurrentHashMap<String, Counter> domainSuccessCounters = new ConcurrentHashMap<>();

    /**
     * Cache of failure Counter instances by composite key (domain:errorType).
     */
    private final ConcurrentHashMap<String, Counter> domainFailureCounters = new ConcurrentHashMap<>();

    /**
     * Cache of DistributionSummary instances by domain for version span tracking.
     */
    private final ConcurrentHashMap<String, DistributionSummary> domainVersionSpans = new ConcurrentHashMap<>();

    /**
     * Creates a new MigrationMetrics instance with the specified meter registry.
     *
     * <p>The provided registry is used to create and register all meters. Typically,
     * this is the auto-configured registry from Spring Boot Actuator, which handles
     * export to various monitoring backends (Prometheus, Datadog, etc.).</p>
     *
     * <h3>Example Usage</h3>
     * <pre>{@code
     * // Manual instantiation (typically done by auto-configuration)
     * MeterRegistry registry = new SimpleMeterRegistry();
     * MigrationMetrics metrics = new MigrationMetrics(registry);
     *
     * // With Spring Boot (auto-configured)
     * @Autowired
     * private MigrationMetrics metrics; // Injected automatically
     * }</pre>
     *
     * @param registry the Micrometer meter registry for creating and registering meters,
     *                 must not be {@code null}
     * @throws NullPointerException if registry is {@code null}
     */
    public MigrationMetrics(@NotNull final MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * Records metrics for a successful migration operation.
     *
     * <p>This method records the following metrics:</p>
     * <ul>
     *   <li>Increments the success counter for the domain</li>
     *   <li>Records the migration duration in the domain's timer</li>
     *   <li>Records the version span in the domain's distribution summary</li>
     * </ul>
     *
     * <p>The version span is calculated as the absolute difference between the target
     * and source versions, providing insight into how far data was migrated.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * // After a successful migration
     * metrics.recordSuccess("game", 100, 200, Duration.ofMillis(150));
     * // Records: success counter +1, timer = 150ms, version span = 100
     * }</pre>
     *
     * @param domain      the domain name identifying the DataFixer used, must not be {@code null}
     * @param fromVersion the source data version (pre-migration)
     * @param toVersion   the target data version (post-migration)
     * @param duration    the wall-clock duration of the migration, must not be {@code null}
     * @throws NullPointerException if domain or duration is {@code null}
     */
    public void recordSuccess(
            @NotNull final String domain,
            final int fromVersion,
            final int toVersion,
            @NotNull final Duration duration
    ) {
        Objects.requireNonNull(domain, "domain must not be null");
        Objects.requireNonNull(duration, "duration must not be null");

        // Record timing
        getOrCreateTimer(domain).record(duration);

        // Record success count
        getOrCreateSuccessCounter(domain).increment();

        // Record version span
        final int span = Math.abs(toVersion - fromVersion);
        getOrCreateVersionSpan(domain).record(span);
    }

    /**
     * Records metrics for a failed migration operation.
     *
     * <p>This method records the following metrics:</p>
     * <ul>
     *   <li>Increments the failure counter for the domain, tagged with the error type</li>
     *   <li>Records the migration duration in the domain's timer (failures still contribute to timing stats)</li>
     * </ul>
     *
     * <p>The error type tag is derived from the exception's simple class name (e.g.,
     * "IllegalStateException", "MigrationException"). This allows for fine-grained
     * failure analysis and alerting based on error categories.</p>
     *
     * <h3>Example</h3>
     * <pre>{@code
     * try {
     *     // Perform migration
     * } catch (MigrationException e) {
     *     metrics.recordFailure("game", 100, 200, Duration.ofMillis(50), e);
     *     // Records: failure counter +1 (with error_type="MigrationException"), timer = 50ms
     * }
     * }</pre>
     *
     * <h3>Note on Duration Recording</h3>
     * <p>Duration is recorded even for failures to provide complete timing statistics.
     * This helps identify whether failures are correlated with long-running migrations
     * (e.g., timeouts) or occur quickly (e.g., validation errors).</p>
     *
     * @param domain      the domain name identifying the DataFixer used, must not be {@code null}
     * @param fromVersion the source data version (pre-migration)
     * @param toVersion   the intended target data version
     * @param duration    the wall-clock duration until failure, must not be {@code null}
     * @param error       the exception that caused the migration to fail, must not be {@code null}
     * @throws NullPointerException if domain, duration, or error is {@code null}
     */
    public void recordFailure(
            @NotNull final String domain,
            final int fromVersion,
            final int toVersion,
            @NotNull final Duration duration,
            @NotNull final Throwable error
    ) {
        Objects.requireNonNull(domain, "domain must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        Objects.requireNonNull(error, "error must not be null");

        // Record timing (even for failures)
        getOrCreateTimer(domain).record(duration);

        // Record failure count with error type
        getOrCreateFailureCounter(domain, error.getClass().getSimpleName()).increment();
    }

    /**
     * Gets or creates a Timer for the specified domain.
     *
     * <p>Timers track the distribution of migration durations, including count, total time,
     * and percentile distributions. The timer is cached for efficient reuse across
     * multiple migration recordings.</p>
     *
     * @param domain the domain name to get or create a timer for
     * @return the Timer instance for the domain, never {@code null}
     */
    private Timer getOrCreateTimer(@NotNull final String domain) {
        return domainTimers.computeIfAbsent(domain, d ->
                Timer.builder(METRIC_PREFIX + ".duration")
                        .tag(TAG_DOMAIN, d)
                        .description("Duration of data migrations")
                        .register(registry)
        );
    }

    /**
     * Gets or creates a success Counter for the specified domain.
     *
     * <p>The counter tracks the total number of successful migrations for the domain.
     * It is monotonically increasing and can be used to calculate success rates
     * when combined with the failure counter.</p>
     *
     * @param domain the domain name to get or create a counter for
     * @return the Counter instance for the domain, never {@code null}
     */
    private Counter getOrCreateSuccessCounter(@NotNull final String domain) {
        return domainSuccessCounters.computeIfAbsent(domain, d ->
                Counter.builder(METRIC_PREFIX + ".success")
                        .tag(TAG_DOMAIN, d)
                        .description("Number of successful migrations")
                        .register(registry)
        );
    }

    /**
     * Gets or creates a failure Counter for the specified domain and error type combination.
     *
     * <p>Failure counters are keyed by both domain and error type, allowing for granular
     * analysis of what types of errors occur in which domains. The composite key format
     * is "domain:errorType" for internal caching.</p>
     *
     * @param domain    the domain name for the counter
     * @param errorType the simple class name of the exception type
     * @return the Counter instance for the domain/error combination, never {@code null}
     */
    private Counter getOrCreateFailureCounter(
            @NotNull final String domain,
            @NotNull final String errorType
    ) {
        final String key = domain + ":" + errorType;
        return domainFailureCounters.computeIfAbsent(key, k ->
                Counter.builder(METRIC_PREFIX + ".failure")
                        .tag(TAG_DOMAIN, domain)
                        .tag(TAG_ERROR_TYPE, errorType)
                        .description("Number of failed migrations")
                        .register(registry)
        );
    }

    /**
     * Gets or creates a DistributionSummary for tracking version spans in the specified domain.
     *
     * <p>The distribution summary tracks the statistical distribution of version spans
     * (absolute difference between target and source versions). This helps identify:</p>
     * <ul>
     *   <li>Average migration complexity across the application</li>
     *   <li>Outliers with unusually large version spans (very outdated data)</li>
     *   <li>Trends in data age over time</li>
     * </ul>
     *
     * @param domain the domain name to get or create a summary for
     * @return the DistributionSummary instance for the domain, never {@code null}
     */
    private DistributionSummary getOrCreateVersionSpan(@NotNull final String domain) {
        return domainVersionSpans.computeIfAbsent(domain, d ->
                DistributionSummary.builder(METRIC_PREFIX + ".version.span")
                        .tag(TAG_DOMAIN, d)
                        .description("Distribution of version spans in migrations")
                        .register(registry)
        );
    }
}
