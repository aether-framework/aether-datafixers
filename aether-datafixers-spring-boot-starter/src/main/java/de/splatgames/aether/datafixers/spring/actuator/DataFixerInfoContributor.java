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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot Actuator info contributor that adds DataFixer metadata to the info endpoint.
 *
 * <p>This contributor enhances the {@code /actuator/info} endpoint with information about
 * registered DataFixer domains and their current schema versions. This is useful for
 * operational visibility, debugging, and monitoring the data schema state of the application.</p>
 *
 * <h2>Purpose</h2>
 * <p>The info contributor provides:</p>
 * <ul>
 *   <li>Total count of registered DataFixer domains</li>
 *   <li>Per-domain version information</li>
 *   <li>Error information for domains that fail to report their version</li>
 * </ul>
 *
 * <h2>Info Endpoint Response</h2>
 * <p>The contributor adds an {@code aether-datafixers} section to the info response:</p>
 *
 * <h3>Normal Response</h3>
 * <pre>{@code
 * {
 *   "app": { ... },
 *   "aether-datafixers": {
 *     "domains": 2,
 *     "domainDetails": {
 *       "default": {
 *         "currentVersion": 200
 *       },
 *       "game": {
 *         "currentVersion": 150
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h3>Response with Error</h3>
 * <pre>{@code
 * {
 *   "aether-datafixers": {
 *     "domains": 2,
 *     "domainDetails": {
 *       "default": {
 *         "currentVersion": 200
 *       },
 *       "game": {
 *         "error": "Schema not initialized"
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <p>To enable the info endpoint and see the DataFixer information:</p>
 * <pre>{@code
 * # application.yml
 * management:
 *   endpoints:
 *     web:
 *       exposure:
 *         include: health, info    # Expose info endpoint
 *   info:
 *     env:
 *       enabled: true              # Include environment info
 * }</pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><strong>Version Tracking:</strong> Monitor which schema versions are deployed</li>
 *   <li><strong>Debugging:</strong> Verify DataFixer configuration in running applications</li>
 *   <li><strong>Deployment Verification:</strong> Confirm schema versions after deployments</li>
 *   <li><strong>Multi-Instance Monitoring:</strong> Compare versions across service instances</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is thread-safe. The contribute method is stateless and the underlying
 * {@link DataFixerRegistry} uses thread-safe data structures.</p>
 *
 * @author Erik Pförtner
 * @see InfoContributor
 * @see DataFixerRegistry
 * @see de.splatgames.aether.datafixers.spring.autoconfigure.ActuatorAutoConfiguration
 * @since 0.4.0
 */
public class DataFixerInfoContributor implements InfoContributor {

    /**
     * Registry containing all DataFixer instances to report information about.
     */
    private final DataFixerRegistry registry;

    /**
     * Creates a new DataFixerInfoContributor with the specified registry.
     *
     * <p>The contributor will include information about all DataFixers registered
     * in the provided registry when the info endpoint is accessed.</p>
     *
     * @param registry the DataFixer registry containing all domain fixers,
     *                 must not be {@code null}
     * @throws NullPointerException if registry is {@code null}
     */
    public DataFixerInfoContributor(@NotNull final DataFixerRegistry registry) {
        this.registry = Preconditions.checkNotNull(registry, "registry must not be null");
    }

    /**
     * Contributes DataFixer information to the actuator info endpoint.
     *
     * <p>This method is called by Spring Boot Actuator when building the info
     * endpoint response. It adds a structured {@code aether-datafixers} section
     * containing domain count and per-domain version details.</p>
     *
     * <p><b>Contributed Information</b></p>
     * <ul>
     *   <li>{@code domains} - Total number of registered DataFixer domains</li>
     *   <li>{@code domainDetails} - Map of domain names to their details</li>
     *   <li>Per domain: {@code currentVersion} or {@code error} message</li>
     * </ul>
     *
     * @param builder the info builder to add details to
     */
    @Override
    public void contribute(final Info.Builder builder) {
        final Map<String, Object> datafixersInfo = new LinkedHashMap<>();
        final Map<String, AetherDataFixer> fixers = this.registry.getAll();

        datafixersInfo.put("domains", fixers.size());

        final Map<String, Object> domainDetails = new LinkedHashMap<>();
        for (final Map.Entry<String, AetherDataFixer> entry : fixers.entrySet()) {
            final Map<String, Object> domainInfo = new LinkedHashMap<>();
            try {
                domainInfo.put("currentVersion", entry.getValue().currentVersion().getVersion());
            } catch (final Exception e) {
                domainInfo.put("error", e.getMessage());
            }
            domainDetails.put(entry.getKey(), domainInfo);
        }

        datafixersInfo.put("domainDetails", domainDetails);
        builder.withDetail("aether-datafixers", datafixersInfo);
    }
}
