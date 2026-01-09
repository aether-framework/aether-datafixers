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
 * Spring Boot Actuator integration for monitoring and managing Aether Datafixers.
 *
 * <p>This package provides comprehensive observability components that integrate
 * with Spring Boot Actuator. These components expose health status, version
 * information, and detailed configuration through standard Actuator endpoints.</p>
 *
 * <h2>Package Contents</h2>
 * <table border="1" cellpadding="5">
 *   <caption>Actuator Components</caption>
 *   <tr>
 *     <th>Class</th>
 *     <th>Actuator Feature</th>
 *     <th>Endpoint</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.actuator.DataFixerHealthIndicator}</td>
 *     <td>Health Check</td>
 *     <td>/actuator/health</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.actuator.DataFixerInfoContributor}</td>
 *     <td>Info Contribution</td>
 *     <td>/actuator/info</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.actuator.DataFixerEndpoint}</td>
 *     <td>Custom Endpoint</td>
 *     <td>/actuator/datafixers</td>
 *   </tr>
 * </table>
 *
 * <h2>Health Indicator</h2>
 * <p>The {@link de.splatgames.aether.datafixers.spring.actuator.DataFixerHealthIndicator}
 * reports the operational status of all registered DataFixers:</p>
 * <ul>
 *   <li><strong>UP:</strong> All DataFixers are operational</li>
 *   <li><strong>DOWN:</strong> At least one DataFixer failed initialization</li>
 *   <li><strong>UNKNOWN:</strong> No DataFixers are registered</li>
 * </ul>
 * <pre>{@code
 * # Example health response
 * {
 *   "status": "UP",
 *   "components": {
 *     "datafixer": {
 *       "status": "UP",
 *       "details": {
 *         "game": {
 *           "status": "UP",
 *           "currentVersion": 200,
 *           "schemaCount": 15
 *         }
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>Info Contributor</h2>
 * <p>The {@link de.splatgames.aether.datafixers.spring.actuator.DataFixerInfoContributor}
 * adds version information to the standard {@code /actuator/info} endpoint:</p>
 * <pre>{@code
 * {
 *   "aether-datafixers": {
 *     "domains": ["game", "user"],
 *     "versions": {
 *       "game": 200,
 *       "user": 150
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>Custom Endpoint</h2>
 * <p>The {@link de.splatgames.aether.datafixers.spring.actuator.DataFixerEndpoint}
 * provides detailed information at {@code /actuator/datafixers}:</p>
 * <ul>
 *   <li><strong>GET /actuator/datafixers</strong> - Summary of all domains</li>
 *   <li><strong>GET /actuator/datafixers/{domain}</strong> - Details for a specific domain</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * # application.yml
 * management:
 *   # Health indicator
 *   health:
 *     datafixer:
 *       enabled: true              # Enable health check (default: true)
 *
 *   # Info contributor
 *   info:
 *     datafixer:
 *       enabled: true              # Enable info contribution (default: true)
 *
 *   # Custom endpoint
 *   endpoint:
 *     datafixers:
 *       enabled: true              # Enable custom endpoint (default: true)
 *
 *   # Endpoint exposure
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health, info, datafixers
 * }</pre>
 *
 * <h2>Security Considerations</h2>
 * <p>The custom endpoint exposes internal configuration details. In production
 * environments, consider limiting access:</p>
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
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * <h2>Kubernetes Integration</h2>
 * <p>The health indicator integrates with Kubernetes probes:</p>
 * <pre>{@code
 * # Pod specification
 * livenessProbe:
 *   httpGet:
 *     path: /actuator/health/liveness
 *     port: 8080
 * readinessProbe:
 *   httpGet:
 *     path: /actuator/health/readiness
 *     port: 8080
 * }</pre>
 * <p>Configure health groups to include DataFixer status:</p>
 * <pre>{@code
 * management:
 *   endpoint:
 *     health:
 *       group:
 *         readiness:
 *           include: readinessState, datafixer
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All actuator components in this package are thread-safe and can handle
 * concurrent requests from multiple HTTP threads.</p>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.spring.actuator.DataFixerHealthIndicator
 * @see de.splatgames.aether.datafixers.spring.actuator.DataFixerInfoContributor
 * @see de.splatgames.aether.datafixers.spring.actuator.DataFixerEndpoint
 * @see de.splatgames.aether.datafixers.spring.autoconfigure.ActuatorAutoConfiguration
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.spring.actuator;
