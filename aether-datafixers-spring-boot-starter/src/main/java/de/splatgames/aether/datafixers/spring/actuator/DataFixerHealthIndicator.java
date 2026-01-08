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
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.Map;
import java.util.Objects;

/**
 * Spring Boot Actuator health indicator for monitoring DataFixer operational status.
 *
 * <p>This health indicator provides real-time health status for all registered
 * {@link AetherDataFixer} instances. It integrates with Spring Boot Actuator's
 * health endpoint ({@code /actuator/health}) to expose DataFixer health as part
 * of the application's overall health status.</p>
 *
 * <h2>Health Status Semantics</h2>
 * <p>The indicator reports the following statuses:</p>
 * <ul>
 *   <li><strong>UP</strong> - All registered DataFixers are operational and responding correctly</li>
 *   <li><strong>DOWN</strong> - At least one DataFixer failed its health check (unable to read version)</li>
 *   <li><strong>UNKNOWN</strong> - No DataFixers are registered in the application</li>
 * </ul>
 *
 * <h2>Health Check Process</h2>
 * <p>For each registered domain, the health check:</p>
 * <ol>
 *   <li>Retrieves the DataFixer from the registry</li>
 *   <li>Attempts to read the current version (validates operational state)</li>
 *   <li>Reports individual domain status in the health details</li>
 *   <li>Fails fast on the first error, reporting the failing domain</li>
 * </ol>
 *
 * <h2>Health Response Examples</h2>
 *
 * <h3>Healthy State (All Domains UP)</h3>
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "details": {
 *     "totalDomains": 2,
 *     "default.status": "UP",
 *     "default.currentVersion": 200,
 *     "game.status": "UP",
 *     "game.currentVersion": 150
 *   }
 * }
 * }</pre>
 *
 * <h3>Unhealthy State (Domain Failure)</h3>
 * <pre>{@code
 * {
 *   "status": "DOWN",
 *   "details": {
 *     "totalDomains": 2,
 *     "game.status": "DOWN",
 *     "game.error": "Schema not initialized"
 *   }
 * }
 * }</pre>
 *
 * <h3>Unknown State (No Domains)</h3>
 * <pre>{@code
 * {
 *   "status": "UNKNOWN",
 *   "details": {
 *     "message": "No DataFixers registered"
 *   }
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <p>This health indicator can be configured through Spring Boot's standard
 * health indicator properties:</p>
 * <pre>{@code
 * # application.yml
 * management:
 *   health:
 *     datafixer:
 *       enabled: true              # Enable/disable this indicator
 *   endpoint:
 *     health:
 *       show-details: always       # Show details in response
 * }</pre>
 *
 * <h2>Integration with Spring Boot Health</h2>
 * <p>When this indicator reports DOWN, it affects the overall application health.
 * This is useful for:</p>
 * <ul>
 *   <li>Kubernetes liveness/readiness probes</li>
 *   <li>Load balancer health checks</li>
 *   <li>Monitoring systems (Prometheus, Datadog, etc.)</li>
 *   <li>Container orchestration systems</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The underlying {@link DataFixerRegistry} uses
 * thread-safe data structures, and the health check method is stateless.</p>
 *
 * @author Erik Pfoertner
 * @see HealthIndicator
 * @see DataFixerRegistry
 * @see de.splatgames.aether.datafixers.spring.autoconfigure.ActuatorAutoConfiguration
 * @since 0.4.0
 */
public class DataFixerHealthIndicator implements HealthIndicator {

    /**
     * Registry containing all DataFixer instances to check.
     */
    private final DataFixerRegistry registry;

    /**
     * Creates a new DataFixerHealthIndicator with the specified registry.
     *
     * <p>The indicator will check the health of all DataFixers registered
     * in the provided registry during each health check invocation.</p>
     *
     * @param registry the DataFixer registry containing all domain fixers,
     *                 must not be {@code null}
     * @throws NullPointerException if registry is {@code null}
     */
    public DataFixerHealthIndicator(@NotNull final DataFixerRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * Performs the health check for all registered DataFixer instances.
     *
     * <p>This method is called by Spring Boot Actuator when the health endpoint
     * is accessed. It iterates through all registered domains and verifies that
     * each DataFixer can successfully report its current version.</p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>Returns {@code UNKNOWN} if no DataFixers are registered</li>
     *   <li>Returns {@code UP} with domain details if all DataFixers are healthy</li>
     *   <li>Returns {@code DOWN} immediately upon first failure, including error details</li>
     * </ul>
     *
     * @return the health status with detailed domain information
     */
    @Override
    public Health health() {
        final Map<String, AetherDataFixer> fixers = registry.getAll();

        if (fixers.isEmpty()) {
            return Health.unknown()
                    .withDetail("message", "No DataFixers registered")
                    .build();
        }

        final Health.Builder builder = Health.up();
        builder.withDetail("totalDomains", fixers.size());

        for (final Map.Entry<String, AetherDataFixer> entry : fixers.entrySet()) {
            final String domain = entry.getKey();
            final AetherDataFixer fixer = entry.getValue();

            try {
                // Verify fixer is operational by accessing current version
                final int currentVersion = fixer.currentVersion().getVersion();
                builder.withDetail(domain + ".status", "UP");
                builder.withDetail(domain + ".currentVersion", currentVersion);
            } catch (final Exception e) {
                return Health.down()
                        .withDetail(domain + ".status", "DOWN")
                        .withDetail(domain + ".error", e.getMessage())
                        .withDetail("totalDomains", fixers.size())
                        .build();
            }
        }

        return builder.build();
    }
}
