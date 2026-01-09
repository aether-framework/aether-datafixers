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

package de.splatgames.aether.datafixers.spring;

import de.splatgames.aether.datafixers.spring.config.DataFixerDomainProperties;
import de.splatgames.aether.datafixers.spring.config.DynamicOpsFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the Aether Datafixers Spring Boot integration.
 *
 * <p>This class provides comprehensive configuration options for the Aether Datafixers
 * framework when used within a Spring Boot application. It supports configuration of
 * serialization formats, version management, multi-domain setups, actuator endpoints,
 * and Micrometer metrics.</p>
 *
 * <h2>Property Prefix</h2>
 * <p>All properties are prefixed with {@code aether.datafixers}.</p>
 *
 * <h2>Configuration Example (application.yml)</h2>
 * <pre>{@code
 * aether:
 *   datafixers:
 *     enabled: true
 *     default-format: gson
 *     default-current-version: 200
 *     domains:
 *       game:
 *         current-version: 200
 *         primary: true
 *         description: "Game save data migrations"
 *       user:
 *         current-version: 150
 *         description: "User profile migrations"
 *     actuator:
 *       include-schema-details: true
 *       include-fix-details: true
 *     metrics:
 *       timing: true
 *       counting: true
 *       domain-tag: domain
 * }</pre>
 *
 * <h2>Configuration Example (application.properties)</h2>
 * <pre>{@code
 * aether.datafixers.enabled=true
 * aether.datafixers.default-format=GSON
 * aether.datafixers.default-current-version=200
 * aether.datafixers.domains.game.current-version=200
 * aether.datafixers.domains.game.primary=true
 * aether.datafixers.actuator.include-schema-details=true
 * aether.datafixers.metrics.timing=true
 * }</pre>
 *
 * <h2>Property Reference</h2>
 * <table border="1">
 *   <caption>Available Configuration Properties</caption>
 *   <tr><th>Property</th><th>Type</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>enabled</td><td>boolean</td><td>true</td><td>Enable/disable auto-configuration</td></tr>
 *   <tr><td>default-format</td><td>DynamicOpsFormat</td><td>GSON</td><td>Default serialization format</td></tr>
 *   <tr><td>default-current-version</td><td>Integer</td><td>null</td><td>Fallback version number</td></tr>
 *   <tr><td>domains</td><td>Map</td><td>empty</td><td>Per-domain configuration</td></tr>
 *   <tr><td>actuator.*</td><td>ActuatorProperties</td><td>-</td><td>Actuator settings</td></tr>
 *   <tr><td>metrics.*</td><td>MetricsProperties</td><td>-</td><td>Metrics settings</td></tr>
 * </table>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is mutable and not thread-safe. It is designed to be configured
 * during application startup and should not be modified at runtime.</p>
 *
 * @author Erik Pfoertner
 * @see AetherDataFixersAutoConfiguration
 * @see DataFixerDomainProperties
 * @see DynamicOpsFormat
 * @see ActuatorProperties
 * @see MetricsProperties
 * @since 0.4.0
 */
@ConfigurationProperties(prefix = "aether.datafixers")
public class AetherDataFixersProperties {

    /**
     * Flag to enable or disable Aether Datafixers auto-configuration.
     *
     * <p>When set to {@code false}, no DataFixer beans will be automatically
     * created, allowing for manual configuration or complete disabling of
     * the functionality.</p>
     */
    private boolean enabled = true;

    /**
     * The default serialization format to use when multiple formats are available.
     *
     * <p>This setting determines which {@code DynamicOps} implementation is
     * preferred when both Gson and Jackson are on the classpath.</p>
     */
    @NotNull
    private DynamicOpsFormat defaultFormat = DynamicOpsFormat.GSON;

    /**
     * The default current version used as fallback when not specified elsewhere.
     *
     * <p>Version resolution priority:</p>
     * <ol>
     *   <li>Domain-specific version ({@code domains.<name>.current-version})</li>
     *   <li>Bootstrap's {@code CURRENT_VERSION} constant</li>
     *   <li>This default value</li>
     * </ol>
     */
    @Nullable
    private Integer defaultCurrentVersion;

    /**
     * Per-domain configuration map for multi-domain setups.
     *
     * <p>Each entry maps a domain name to its specific configuration,
     * allowing different DataFixer instances to have independent settings.</p>
     */
    @NotNull
    private Map<String, DataFixerDomainProperties> domains = new HashMap<>();

    /**
     * Configuration properties for Spring Boot Actuator integration.
     */
    @NotNull
    private ActuatorProperties actuator = new ActuatorProperties();

    /**
     * Configuration properties for Micrometer metrics collection.
     */
    @NotNull
    private MetricsProperties metrics = new MetricsProperties();

    /**
     * Returns whether the Aether Datafixers auto-configuration is enabled.
     *
     * <p>When disabled, no automatic bean creation occurs, and the application
     * must configure DataFixer beans manually if needed.</p>
     *
     * @return {@code true} if auto-configuration is enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Sets whether the Aether Datafixers auto-configuration is enabled.
     *
     * @param enabled {@code true} to enable auto-configuration, {@code false} to disable
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the default serialization format for DynamicOps.
     *
     * <p>This determines which format (GSON or JACKSON) is used when both
     * libraries are available on the classpath.</p>
     *
     * @return the default serialization format, never {@code null}
     */
    @NotNull
    public DynamicOpsFormat getDefaultFormat() {
        return this.defaultFormat;
    }

    /**
     * Sets the default serialization format for DynamicOps.
     *
     * @param defaultFormat the format to use as default, must not be {@code null}
     * @throws NullPointerException if defaultFormat is {@code null}
     */
    public void setDefaultFormat(@NotNull final DynamicOpsFormat defaultFormat) {
        this.defaultFormat = defaultFormat;
    }

    /**
     * Returns the default current version number.
     *
     * <p>This value is used as a fallback when the version cannot be determined
     * from domain configuration or the bootstrap class.</p>
     *
     * @return the default version number, or {@code null} if not configured
     */
    @Nullable
    public Integer getDefaultCurrentVersion() {
        return this.defaultCurrentVersion;
    }

    /**
     * Sets the default current version number.
     *
     * <p>Set to {@code null} to disable the fallback and require explicit
     * version configuration in each domain or bootstrap.</p>
     *
     * @param defaultCurrentVersion the version number, or {@code null} to disable fallback
     */
    public void setDefaultCurrentVersion(@Nullable final Integer defaultCurrentVersion) {
        this.defaultCurrentVersion = defaultCurrentVersion;
    }

    /**
     * Returns the per-domain configuration map.
     *
     * <p>This map allows configuring multiple DataFixer domains with individual
     * settings. The map keys are domain names (matching {@code @Qualifier} values),
     * and the values contain domain-specific configuration.</p>
     *
     * @return the mutable domain configuration map, never {@code null}
     */
    @NotNull
    public Map<String, DataFixerDomainProperties> getDomains() {
        return this.domains;
    }

    /**
     * Sets the per-domain configuration map.
     *
     * @param domains the domain configuration map, must not be {@code null}
     * @throws NullPointerException if domains is {@code null}
     */
    public void setDomains(@NotNull final Map<String, DataFixerDomainProperties> domains) {
        this.domains = domains;
    }

    /**
     * Returns the actuator configuration properties.
     *
     * <p>These settings control the behavior of the DataFixer actuator endpoints,
     * including what information is exposed in responses.</p>
     *
     * @return the actuator properties, never {@code null}
     */
    @NotNull
    public ActuatorProperties getActuator() {
        return this.actuator;
    }

    /**
     * Sets the actuator configuration properties.
     *
     * @param actuator the actuator properties, must not be {@code null}
     * @throws NullPointerException if actuator is {@code null}
     */
    public void setActuator(@NotNull final ActuatorProperties actuator) {
        this.actuator = actuator;
    }

    /**
     * Returns the metrics configuration properties.
     *
     * <p>These settings control how migration metrics are recorded using
     * Micrometer, including which metrics are enabled and how they are tagged.</p>
     *
     * @return the metrics properties, never {@code null}
     */
    @NotNull
    public MetricsProperties getMetrics() {
        return this.metrics;
    }

    /**
     * Sets the metrics configuration properties.
     *
     * @param metrics the metrics properties, must not be {@code null}
     * @throws NullPointerException if metrics is {@code null}
     */
    public void setMetrics(@NotNull final MetricsProperties metrics) {
        this.metrics = metrics;
    }

    /**
     * Configuration properties for Spring Boot Actuator integration.
     *
     * <p>This nested class contains settings that control what information
     * is exposed through the DataFixer actuator endpoints. These endpoints
     * provide operational visibility into the DataFixer configuration and state.</p>
     *
     * <h2>Configuration Example</h2>
     * <pre>{@code
     * aether:
     *   datafixers:
     *     actuator:
     *       include-schema-details: true
     *       include-fix-details: false
     * }</pre>
     *
     * <h2>Security Considerations</h2>
     * <p>Schema and fix details may reveal internal application structure.
     * Consider disabling these in production environments if this information
     * should not be exposed.</p>
     *
     * @author Erik Pfoertner
     * @see de.splatgames.aether.datafixers.spring.actuator.DataFixerEndpoint
     * @see de.splatgames.aether.datafixers.spring.actuator.DataFixerInfoContributor
     * @since 0.4.0
     */
    public static class ActuatorProperties {

        /**
         * Flag to include detailed schema information in actuator responses.
         *
         * <p>When enabled, schema versions and type definitions may be exposed.</p>
         */
        private boolean includeSchemaDetails = true;

        /**
         * Flag to include detailed fix information in actuator responses.
         *
         * <p>When enabled, registered DataFix instances and their version
         * ranges may be exposed.</p>
         */
        private boolean includeFixDetails = true;

        /**
         * Returns whether schema details are included in actuator responses.
         *
         * <p>Schema details include information about registered schemas,
         * their versions, and defined types.</p>
         *
         * @return {@code true} if schema details are included, {@code false} otherwise
         */
        public boolean isIncludeSchemaDetails() {
            return this.includeSchemaDetails;
        }

        /**
         * Sets whether schema details are included in actuator responses.
         *
         * @param includeSchemaDetails {@code true} to include schema details,
         *                             {@code false} to exclude them
         */
        public void setIncludeSchemaDetails(final boolean includeSchemaDetails) {
            this.includeSchemaDetails = includeSchemaDetails;
        }

        /**
         * Returns whether fix details are included in actuator responses.
         *
         * <p>Fix details include information about registered DataFix instances,
         * their version ranges, and migration paths.</p>
         *
         * @return {@code true} if fix details are included, {@code false} otherwise
         */
        public boolean isIncludeFixDetails() {
            return this.includeFixDetails;
        }

        /**
         * Sets whether fix details are included in actuator responses.
         *
         * @param includeFixDetails {@code true} to include fix details,
         *                          {@code false} to exclude them
         */
        public void setIncludeFixDetails(final boolean includeFixDetails) {
            this.includeFixDetails = includeFixDetails;
        }
    }

    /**
     * Configuration properties for Micrometer metrics collection.
     *
     * <p>This nested class contains settings that control how migration
     * operations are measured and reported through Micrometer. The metrics
     * provide operational insights into migration performance and success rates.</p>
     *
     * <h2>Configuration Example</h2>
     * <pre>{@code
     * aether:
     *   datafixers:
     *     metrics:
     *       timing: true
     *       counting: true
     *       domain-tag: domain
     * }</pre>
     *
     * <h2>Available Metrics</h2>
     * <p>When enabled, the following metrics are recorded:</p>
     * <ul>
     *   <li>{@code aether.datafixers.migrations.success} - Counter for successful migrations</li>
     *   <li>{@code aether.datafixers.migrations.failure} - Counter for failed migrations</li>
     *   <li>{@code aether.datafixers.migrations.duration} - Timer for migration duration</li>
     *   <li>{@code aether.datafixers.migrations.version.span} - Distribution of version spans</li>
     * </ul>
     *
     * <h2>Metric Tags</h2>
     * <p>All metrics are tagged with the domain name, using the tag name
     * configured in {@link #domainTag}. This allows filtering and grouping
     * metrics by domain in monitoring dashboards.</p>
     *
     * @author Erik Pfoertner
     * @see de.splatgames.aether.datafixers.spring.metrics.MigrationMetrics
     * @since 0.4.0
     */
    public static class MetricsProperties {

        /**
         * Flag to enable timing metrics for migration operations.
         *
         * <p>When enabled, the duration of each migration is recorded
         * as a Timer metric.</p>
         */
        private boolean timing = true;

        /**
         * Flag to enable counting metrics for migration operations.
         *
         * <p>When enabled, success and failure counts are recorded
         * as Counter metrics.</p>
         */
        private boolean counting = true;

        /**
         * The tag name used to identify domains in metric labels.
         *
         * <p>This tag is applied to all migration metrics, allowing
         * filtering by domain in monitoring systems.</p>
         */
        @NotNull
        private String domainTag = "domain";

        /**
         * Returns whether timing metrics are enabled for migrations.
         *
         * <p>Timing metrics record the duration of migration operations,
         * useful for identifying slow migrations and performance trends.</p>
         *
         * @return {@code true} if timing metrics are enabled, {@code false} otherwise
         */
        public boolean isTiming() {
            return this.timing;
        }

        /**
         * Sets whether timing metrics are enabled for migrations.
         *
         * @param timing {@code true} to enable timing metrics, {@code false} to disable
         */
        public void setTiming(final boolean timing) {
            this.timing = timing;
        }

        /**
         * Returns whether counting metrics are enabled for migrations.
         *
         * <p>Counting metrics track the number of successful and failed
         * migrations, useful for monitoring error rates and throughput.</p>
         *
         * @return {@code true} if counting metrics are enabled, {@code false} otherwise
         */
        public boolean isCounting() {
            return this.counting;
        }

        /**
         * Sets whether counting metrics are enabled for migrations.
         *
         * @param counting {@code true} to enable counting metrics, {@code false} to disable
         */
        public void setCounting(final boolean counting) {
            this.counting = counting;
        }

        /**
         * Returns the tag name used for domain identification in metrics.
         *
         * <p>This tag name is used as the key in metric labels, with the
         * domain name as the value. For example, with the default tag name
         * "domain", a metric might be tagged as {@code domain="game"}.</p>
         *
         * @return the domain tag name, never {@code null}
         */
        @NotNull
        public String getDomainTag() {
            return this.domainTag;
        }

        /**
         * Sets the tag name used for domain identification in metrics.
         *
         * @param domainTag the tag name to use, must not be {@code null}
         * @throws NullPointerException if domainTag is {@code null}
         */
        public void setDomainTag(@NotNull final String domainTag) {
            this.domainTag = domainTag;
        }
    }
}
