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

package de.splatgames.aether.datafixers.spring.autoconfigure;

import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.spring.actuator.DataFixerEndpoint;
import de.splatgames.aether.datafixers.spring.actuator.DataFixerHealthIndicator;
import de.splatgames.aether.datafixers.spring.actuator.DataFixerInfoContributor;
import de.splatgames.aether.datafixers.spring.metrics.MigrationMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.info.ConditionalOnEnabledInfoContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Spring Boot Actuator integration with Aether Datafixers.
 *
 * <p>This configuration class provides comprehensive observability and monitoring
 * capabilities for DataFixer instances through Spring Boot Actuator. It conditionally
 * configures health checks, info contributions, custom endpoints, and Micrometer metrics.</p>
 *
 * <h2>Activation Conditions</h2>
 * <p>This auto-configuration is activated when all of the following conditions are met:</p>
 * <ul>
 *   <li>Spring Boot Actuator is on the classpath (detected by Endpoint annotation class)</li>
 *   <li>{@code aether.datafixers.enabled} is {@code true} (default)</li>
 *   <li>At least one {@link AetherDataFixer} bean exists in the context</li>
 * </ul>
 *
 * <h2>Provided Components</h2>
 * <table class="striped">
 *   <caption>Actuator Components</caption>
 *   <tr>
 *     <th>Component</th>
 *     <th>Purpose</th>
 *     <th>Endpoint</th>
 *     <th>Enable Property</th>
 *   </tr>
 *   <tr>
 *     <td>{@link DataFixerHealthIndicator}</td>
 *     <td>DataFixer health status</td>
 *     <td>/actuator/health</td>
 *     <td>{@code management.health.datafixer.enabled}</td>
 *   </tr>
 *   <tr>
 *     <td>{@link DataFixerInfoContributor}</td>
 *     <td>Version info in /info</td>
 *     <td>/actuator/info</td>
 *     <td>{@code management.info.datafixer.enabled}</td>
 *   </tr>
 *   <tr>
 *     <td>{@link DataFixerEndpoint}</td>
 *     <td>Custom endpoint for detailed info</td>
 *     <td>/actuator/datafixers</td>
 *     <td>{@code management.endpoint.datafixers.enabled}</td>
 *   </tr>
 *   <tr>
 *     <td>{@link MigrationMetrics}</td>
 *     <td>Micrometer metrics recording</td>
 *     <td>/actuator/metrics</td>
 *     <td>Enabled when Micrometer present</td>
 *   </tr>
 * </table>
 *
 * <h2>Configuration Properties</h2>
 * <pre>{@code
 * # application.yml
 * management:
 *   # Health indicator settings
 *   health:
 *     datafixer:
 *       enabled: true                    # Enable DataFixer health check
 *
 *   # Info contributor settings
 *   info:
 *     datafixer:
 *       enabled: true                    # Enable DataFixer info contribution
 *
 *   # Custom endpoint settings
 *   endpoint:
 *     datafixers:
 *       enabled: true                    # Enable /actuator/datafixers endpoint
 *
 *   # Endpoint exposure
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health, info, datafixers, metrics
 * }</pre>
 *
 * <h2>Health Check Details</h2>
 * <p>The health indicator reports:</p>
 * <ul>
 *   <li><strong>UP</strong> - All DataFixers operational</li>
 *   <li><strong>DOWN</strong> - At least one DataFixer failed</li>
 *   <li><strong>UNKNOWN</strong> - No DataFixers registered</li>
 * </ul>
 *
 * <h2>Metrics Recorded</h2>
 * <p>When Micrometer is available, the following metrics are recorded:</p>
 * <ul>
 *   <li>{@code aether.datafixers.migrations.success} - Counter per domain</li>
 *   <li>{@code aether.datafixers.migrations.failure} - Counter per domain/error type</li>
 *   <li>{@code aether.datafixers.migrations.duration} - Timer per domain</li>
 *   <li>{@code aether.datafixers.migrations.version.span} - Distribution per domain</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <p>The {@code /actuator/datafixers} endpoint exposes internal version information.
 * In production environments, consider securing it appropriately:</p>
 * <pre>{@code
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health, info         # Exclude datafixers from public access
 * }</pre>
 *
 * @author Erik Pförtner
 * @see DataFixerHealthIndicator
 * @see DataFixerInfoContributor
 * @see DataFixerEndpoint
 * @see MigrationMetrics
 * @since 0.4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
@ConditionalOnProperty(
        prefix = "aether.datafixers",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnBean(AetherDataFixer.class)
public class ActuatorAutoConfiguration {

    /**
     * Nested configuration for the {@link DataFixerHealthIndicator}.
     *
     * <p>Activated when the health indicator is enabled via
     * {@code management.health.datafixer.enabled=true} (enabled by default).</p>
     *
     * @author Erik Pförtner
     * @since 0.4.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnEnabledHealthIndicator("datafixer")
    static class HealthIndicatorConfiguration {

        /**
         * Creates the DataFixer health indicator bean.
         *
         * <p>The health indicator checks all registered DataFixers and reports
         * their operational status as part of the application health.</p>
         *
         * @param registry the DataFixer registry for iterating domains
         * @return a new DataFixerHealthIndicator instance
         */
        @Bean
        @ConditionalOnMissingBean(name = "dataFixerHealthIndicator")
        public DataFixerHealthIndicator dataFixerHealthIndicator(
                final DataFixerRegistry registry
        ) {
            return new DataFixerHealthIndicator(registry);
        }
    }

    /**
     * Nested configuration for the {@link DataFixerInfoContributor}.
     *
     * <p>Activated when the info contributor is enabled via
     * {@code management.info.datafixer.enabled=true} (enabled by default).</p>
     *
     * @author Erik Pförtner
     * @since 0.4.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnEnabledInfoContributor("datafixer")
    static class InfoContributorConfiguration {

        /**
         * Creates the DataFixer info contributor bean.
         *
         * <p>The info contributor adds domain and version information to the
         * {@code /actuator/info} endpoint response under the {@code aether-datafixers} key.</p>
         *
         * @param registry the DataFixer registry for reading domain information
         * @return a new DataFixerInfoContributor instance
         */
        @Bean
        @ConditionalOnMissingBean(name = "dataFixerInfoContributor")
        public DataFixerInfoContributor dataFixerInfoContributor(
                final DataFixerRegistry registry
        ) {
            return new DataFixerInfoContributor(registry);
        }
    }

    /**
     * Nested configuration for the custom {@link DataFixerEndpoint}.
     *
     * <p>Activated when the endpoint is available (exposed and not disabled).
     * The endpoint provides detailed DataFixer information at {@code /actuator/datafixers}.</p>
     *
     * @author Erik Pförtner
     * @since 0.4.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnAvailableEndpoint(endpoint = DataFixerEndpoint.class)
    static class EndpointConfiguration {

        /**
         * Creates the custom DataFixer actuator endpoint.
         *
         * <p>The endpoint provides two operations:</p>
         * <ul>
         *   <li>GET /actuator/datafixers - Summary of all domains</li>
         *   <li>GET /actuator/datafixers/{domain} - Details for specific domain</li>
         * </ul>
         *
         * @param registry the DataFixer registry for querying domain information
         * @return a new DataFixerEndpoint instance
         */
        @Bean
        @ConditionalOnMissingBean
        public DataFixerEndpoint dataFixerEndpoint(
                final DataFixerRegistry registry
        ) {
            return new DataFixerEndpoint(registry);
        }
    }

    /**
     * Nested configuration for Micrometer-based metrics.
     *
     * <p>Activated when Micrometer is available on the classpath. The metrics
     * are automatically integrated with the {@link de.splatgames.aether.datafixers.spring.service.MigrationService}
     * to record migration statistics.</p>
     *
     * @author Erik Pförtner
     * @since 0.4.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class MetricsConfiguration {

        /**
         * Creates the migration metrics bean.
         *
         * <p>The metrics bean uses Micrometer's {@link MeterRegistry} to record:</p>
         * <ul>
         *   <li>Migration success/failure counts</li>
         *   <li>Migration duration timings</li>
         *   <li>Version span distribution</li>
         * </ul>
         *
         * <p>All metrics are tagged with the domain name for filtering.</p>
         *
         * @param meterRegistry the Micrometer registry for creating meters
         * @return a new MigrationMetrics instance
         */
        @Bean
        @ConditionalOnMissingBean
        public MigrationMetrics migrationMetrics(final MeterRegistry meterRegistry) {
            return new MigrationMetrics(meterRegistry);
        }
    }
}
