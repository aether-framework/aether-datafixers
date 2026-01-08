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

package de.splatgames.aether.datafixers.spring.actuator;

import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Custom Spring Boot Actuator endpoint for comprehensive DataFixer management and monitoring.
 *
 * <p>This endpoint provides detailed operational visibility into registered DataFixer instances,
 * exposing version information, health status, and domain details through a RESTful API.
 * It is accessible at {@code /actuator/datafixers} when properly configured.</p>
 *
 * <h2>Available Operations</h2>
 * <table border="1" cellpadding="5">
 *   <caption>Endpoint Operations</caption>
 *   <tr>
 *     <th>HTTP Method</th>
 *     <th>Path</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/actuator/datafixers</td>
 *     <td>Returns summary of all registered domains with versions and status</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/actuator/datafixers/{domain}</td>
 *     <td>Returns detailed information for a specific domain (404 if not found)</td>
 *   </tr>
 * </table>
 *
 * <h2>Response Examples</h2>
 *
 * <h3>Summary Response (GET /actuator/datafixers)</h3>
 * <pre>{@code
 * {
 *   "domains": {
 *     "default": {
 *       "currentVersion": 200,
 *       "status": "UP"
 *     },
 *     "game": {
 *       "currentVersion": 150,
 *       "status": "UP"
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h3>Domain Details Response (GET /actuator/datafixers/game)</h3>
 * <pre>{@code
 * {
 *   "domain": "game",
 *   "currentVersion": 150,
 *   "status": "UP"
 * }
 * }</pre>
 *
 * <h3>Error Response (Domain Failure)</h3>
 * <pre>{@code
 * {
 *   "domain": "game",
 *   "currentVersion": -1,
 *   "status": "DOWN: Schema not initialized"
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <p>To expose this endpoint, configure Spring Boot Actuator:</p>
 * <pre>{@code
 * # application.yml
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health, info, datafixers  # Include custom endpoint
 *   endpoint:
 *     datafixers:
 *       enabled: true                         # Enable endpoint (default: true)
 * }</pre>
 *
 * <h2>Security Considerations</h2>
 * <p>This endpoint exposes internal version information. In production environments,
 * consider securing it with Spring Security:</p>
 * <pre>{@code
 * @Configuration
 * public class ActuatorSecurityConfig {
 *
 *     @Bean
 *     public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
 *         return http
 *             .securityMatcher(EndpointRequest.toAnyEndpoint())
 *             .authorizeHttpRequests(auth -> auth
 *                 .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
 *                 .requestMatchers(EndpointRequest.to("datafixers")).hasRole("ADMIN")
 *                 .anyRequest().authenticated()
 *             )
 *             .httpBasic(Customizer.withDefaults())
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><strong>Operational Monitoring:</strong> Track schema versions across deployments</li>
 *   <li><strong>Debugging:</strong> Verify domain configuration in running applications</li>
 *   <li><strong>Automation:</strong> Query version info in deployment scripts</li>
 *   <li><strong>Alerting:</strong> Monitor for DOWN domains in production</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. All operations are read-only and the underlying
 * {@link DataFixerRegistry} uses thread-safe data structures.</p>
 *
 * @author Erik Pfoertner
 * @see Endpoint
 * @see DataFixerRegistry
 * @see de.splatgames.aether.datafixers.spring.autoconfigure.ActuatorAutoConfiguration
 * @since 0.4.0
 */
@Endpoint(id = "datafixers")
public class DataFixerEndpoint {

    /**
     * Registry containing all DataFixer instances to expose through this endpoint.
     */
    private final DataFixerRegistry registry;

    /**
     * Creates a new DataFixerEndpoint with the specified registry.
     *
     * <p>The endpoint will expose information about all DataFixers registered
     * in the provided registry through its operations.</p>
     *
     * @param registry the DataFixer registry containing all domain fixers,
     *                 must not be {@code null}
     * @throws NullPointerException if registry is {@code null}
     */
    public DataFixerEndpoint(@NotNull final DataFixerRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * Returns summary information for all registered DataFixer domains.
     *
     * <p>This operation provides a high-level overview of all registered domains,
     * including their current schema version and operational status. It is useful
     * for quick health checks and monitoring dashboards.</p>
     *
     * <h3>Response Structure</h3>
     * <p>The response includes a map of domain names to their summaries, where each
     * summary contains:</p>
     * <ul>
     *   <li>{@code currentVersion} - The current schema version (or -1 on error)</li>
     *   <li>{@code status} - "UP" if operational, or "DOWN: {error}" on failure</li>
     * </ul>
     *
     * @return the summary response containing all domain information
     */
    @ReadOperation
    public DataFixersSummary summary() {
        final Map<String, DomainSummary> domains = new LinkedHashMap<>();

        for (final Map.Entry<String, AetherDataFixer> entry : registry.getAll().entrySet()) {
            final String domain = entry.getKey();
            final AetherDataFixer fixer = entry.getValue();

            try {
                domains.put(domain, new DomainSummary(
                        fixer.currentVersion().getVersion(),
                        "UP"
                ));
            } catch (final Exception e) {
                domains.put(domain, new DomainSummary(
                        -1,
                        "DOWN: " + e.getMessage()
                ));
            }
        }

        return new DataFixersSummary(domains);
    }

    /**
     * Returns detailed information for a specific DataFixer domain.
     *
     * <p>This operation provides comprehensive details about a single domain,
     * identified by its name. If the domain is not registered, Spring Boot
     * returns a 404 Not Found response.</p>
     *
     * <h3>Path Parameter</h3>
     * <p>The {@code domain} parameter is extracted from the URL path using the
     * {@link Selector} annotation. For example, {@code /actuator/datafixers/game}
     * would query the "game" domain.</p>
     *
     * <h3>Response Structure</h3>
     * <ul>
     *   <li>{@code domain} - The domain name (echoed back for clarity)</li>
     *   <li>{@code currentVersion} - The current schema version (or -1 on error)</li>
     *   <li>{@code status} - "UP" if operational, or "DOWN: {error}" on failure</li>
     * </ul>
     *
     * @param domain the domain name to retrieve details for
     * @return the domain details including version and status, or {@code null} if
     *         the domain is not registered (results in HTTP 404)
     */
    @ReadOperation
    @Nullable
    public DomainDetails domainDetails(@Selector final String domain) {
        final AetherDataFixer fixer = registry.get(domain);
        if (fixer == null) {
            return null; // Spring will return 404
        }

        try {
            return new DomainDetails(
                    domain,
                    fixer.currentVersion().getVersion(),
                    "UP"
            );
        } catch (final Exception e) {
            return new DomainDetails(
                    domain,
                    -1,
                    "DOWN: " + e.getMessage()
            );
        }
    }

    /**
     * Response object containing summary information for all registered DataFixer domains.
     *
     * <p>This record represents the root response returned by the {@link #summary()}
     * operation. It wraps a map of domain names to their respective summary objects,
     * providing a complete overview of all registered DataFixers.</p>
     *
     * <h3>JSON Serialization</h3>
     * <p>When serialized to JSON, this record produces:</p>
     * <pre>{@code
     * {
     *   "domains": {
     *     "domain1": { "currentVersion": 100, "status": "UP" },
     *     "domain2": { "currentVersion": 200, "status": "UP" }
     *   }
     * }
     * }</pre>
     *
     * @param domains map of domain names to their summary information,
     *                preserves insertion order when using LinkedHashMap
     * @author Erik Pfoertner
     * @since 0.4.0
     */
    public record DataFixersSummary(Map<String, DomainSummary> domains) {
    }

    /**
     * Summary information for a single DataFixer domain.
     *
     * <p>This record provides a concise overview of a domain's state, including
     * its current schema version and operational status. It is used as the value
     * type in the {@link DataFixersSummary#domains()} map.</p>
     *
     * <h3>Status Values</h3>
     * <ul>
     *   <li>{@code "UP"} - The DataFixer is operational and responding normally</li>
     *   <li>{@code "DOWN: {message}"} - The DataFixer failed with the given error message</li>
     * </ul>
     *
     * <h3>Version Semantics</h3>
     * <ul>
     *   <li>Positive values: Valid schema version</li>
     *   <li>{@code -1}: Indicates an error occurred while reading the version</li>
     * </ul>
     *
     * @param currentVersion the current schema version of the domain, or -1 on error
     * @param status         the operational status ("UP" or "DOWN: {error}")
     * @author Erik Pfoertner
     * @since 0.4.0
     */
    public record DomainSummary(int currentVersion, String status) {
    }

    /**
     * Detailed information for a single DataFixer domain.
     *
     * <p>This record provides comprehensive details about a specific domain,
     * returned by the {@link #domainDetails(String)} operation. It includes
     * the domain name for clarity, along with version and status information.</p>
     *
     * <h3>JSON Serialization</h3>
     * <p>When serialized to JSON, this record produces:</p>
     * <pre>{@code
     * {
     *   "domain": "game",
     *   "currentVersion": 150,
     *   "status": "UP"
     * }
     * }</pre>
     *
     * @param domain         the domain name (echoed from the request path)
     * @param currentVersion the current schema version of the domain, or -1 on error
     * @param status         the operational status ("UP" or "DOWN: {error}")
     * @author Erik Pfoertner
     * @since 0.4.0
     */
    public record DomainDetails(
            String domain,
            int currentVersion,
            String status
    ) {
    }
}
