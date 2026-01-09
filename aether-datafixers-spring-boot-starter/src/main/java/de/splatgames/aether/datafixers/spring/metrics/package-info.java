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

/**
 * Micrometer metrics integration for tracking Aether Datafixers migration operations.
 *
 * <p>This package provides comprehensive observability for data migrations through
 * integration with the Micrometer metrics library. All metrics are automatically
 * recorded when migrations are executed through the
 * {@link de.splatgames.aether.datafixers.spring.service.MigrationService}.</p>
 *
 * <h2>Package Contents</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.spring.metrics.MigrationMetrics}
 *       - Primary metrics recorder for all migration operations</li>
 * </ul>
 *
 * <h2>Available Metrics</h2>
 * <table border="1" cellpadding="5">
 *   <caption>Metrics Overview</caption>
 *   <tr>
 *     <th>Metric Name</th>
 *     <th>Type</th>
 *     <th>Tags</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>{@code aether.datafixers.migrations.success}</td>
 *     <td>Counter</td>
 *     <td>domain</td>
 *     <td>Total successful migrations per domain</td>
 *   </tr>
 *   <tr>
 *     <td>{@code aether.datafixers.migrations.failure}</td>
 *     <td>Counter</td>
 *     <td>domain, error_type</td>
 *     <td>Total failed migrations per domain and error type</td>
 *   </tr>
 *   <tr>
 *     <td>{@code aether.datafixers.migrations.duration}</td>
 *     <td>Timer</td>
 *     <td>domain</td>
 *     <td>Migration execution time distribution</td>
 *   </tr>
 *   <tr>
 *     <td>{@code aether.datafixers.migrations.version.span}</td>
 *     <td>DistributionSummary</td>
 *     <td>domain</td>
 *     <td>Distribution of version spans (to - from)</td>
 *   </tr>
 * </table>
 *
 * <h2>Prometheus Query Examples</h2>
 * <pre>{@code
 * # Success rate by domain over the last hour
 * sum(rate(aether_datafixers_migrations_success_total[1h])) by (domain)
 *
 * # Failure rate with error type breakdown
 * sum(rate(aether_datafixers_migrations_failure_total[1h])) by (domain, error_type)
 *
 * # 95th percentile migration duration
 * histogram_quantile(0.95, sum(rate(aether_datafixers_migrations_duration_seconds_bucket[5m])) by (le, domain))
 *
 * # Average version span per migration
 * sum(rate(aether_datafixers_migrations_version_span_sum[1h])) by (domain) /
 * sum(rate(aether_datafixers_migrations_version_span_count[1h])) by (domain)
 * }</pre>
 *
 * <h2>Grafana Dashboard Variables</h2>
 * <pre>{@code
 * # Dashboard variable for domain selection
 * label_values(aether_datafixers_migrations_success_total, domain)
 *
 * # Dashboard variable for error types
 * label_values(aether_datafixers_migrations_failure_total, error_type)
 * }</pre>
 *
 * <h2>Auto-Configuration</h2>
 * <p>Metrics are automatically enabled when:</p>
 * <ul>
 *   <li>Micrometer is on the classpath ({@code io.micrometer:micrometer-core})</li>
 *   <li>A {@link io.micrometer.core.instrument.MeterRegistry} bean exists</li>
 *   <li>{@code aether.datafixers.enabled=true} (default)</li>
 * </ul>
 *
 * <h2>Alerting Recommendations</h2>
 * <p>Consider setting up alerts for:</p>
 * <ul>
 *   <li><strong>High failure rate:</strong> More than 5% of migrations failing</li>
 *   <li><strong>Slow migrations:</strong> 95th percentile duration above threshold</li>
 *   <li><strong>Large version spans:</strong> May indicate missing intermediate migrations</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>The {@link de.splatgames.aether.datafixers.spring.metrics.MigrationMetrics} class
 * is thread-safe. All counters and timers use Micrometer's internally synchronized
 * implementations that can be safely used from multiple threads concurrently.</p>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.spring.metrics.MigrationMetrics
 * @see de.splatgames.aether.datafixers.spring.autoconfigure.ActuatorAutoConfiguration.MetricsConfiguration
 * @see io.micrometer.core.instrument.MeterRegistry
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.spring.metrics;
