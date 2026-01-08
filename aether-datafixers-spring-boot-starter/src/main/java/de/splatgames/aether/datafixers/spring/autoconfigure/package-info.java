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
 * Auto-configuration classes for the Aether Datafixers Spring Boot integration.
 *
 * <p>This package contains Spring Boot auto-configuration classes that automatically
 * create and configure beans based on classpath detection, configuration properties,
 * and existing bean definitions. These configurations follow Spring Boot's conditional
 * bean pattern for flexible, non-intrusive integration.</p>
 *
 * <h2>Package Contents</h2>
 * <table border="1" cellpadding="5">
 *   <caption>Auto-Configuration Classes</caption>
 *   <tr>
 *     <th>Class</th>
 *     <th>Purpose</th>
 *     <th>Key Beans</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerAutoConfiguration}</td>
 *     <td>Creates AetherDataFixer beans from bootstraps</td>
 *     <td>AetherDataFixer, DataFixerRegistry</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.autoconfigure.DynamicOpsAutoConfiguration}</td>
 *     <td>Configures serialization format adapters</td>
 *     <td>GsonOps, JacksonOps, DynamicOps</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.autoconfigure.MigrationServiceAutoConfiguration}</td>
 *     <td>Provides the fluent migration service</td>
 *     <td>MigrationService</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.autoconfigure.ActuatorAutoConfiguration}</td>
 *     <td>Spring Boot Actuator integration</td>
 *     <td>HealthIndicator, InfoContributor, Endpoint, Metrics</td>
 *   </tr>
 * </table>
 *
 * <h2>Configuration Order</h2>
 * <p>The auto-configuration classes are processed in the following order:</p>
 * <ol>
 *   <li>{@code DynamicOpsAutoConfiguration} - Configures serialization formats</li>
 *   <li>{@code DataFixerAutoConfiguration} - Creates DataFixer beans from bootstraps</li>
 *   <li>{@code MigrationServiceAutoConfiguration} - Provides migration service</li>
 *   <li>{@code ActuatorAutoConfiguration} - Configures observability components</li>
 * </ol>
 *
 * <h2>Conditional Activation</h2>
 * <p>All auto-configurations are conditional and activate based on:</p>
 * <ul>
 *   <li><strong>Property:</strong> {@code aether.datafixers.enabled=true} (default)</li>
 *   <li><strong>Classpath:</strong> Required classes present (e.g., Gson, Jackson, Actuator)</li>
 *   <li><strong>Beans:</strong> Required beans available (e.g., DataFixerBootstrap)</li>
 * </ul>
 *
 * <h2>Supporting Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerRegistry}
 *       - Thread-safe registry for multi-domain DataFixer management</li>
 * </ul>
 *
 * <h2>Customization</h2>
 * <p>To customize or override auto-configured beans, define your own beans:</p>
 * <pre>{@code
 * @Configuration
 * public class CustomConfig {
 *
 *     @Bean
 *     public MigrationService customMigrationService(
 *             DataFixerRegistry registry,
 *             MigrationMetrics metrics,
 *             @Qualifier("custom") Executor executor) {
 *         return new DefaultMigrationService(registry, metrics, executor);
 *     }
 * }
 * }</pre>
 * <p>Custom beans prevent auto-configured beans from being created due to
 * {@code @ConditionalOnMissingBean} annotations.</p>
 *
 * @author Erik Pfoertner
 * @see de.splatgames.aether.datafixers.spring.AetherDataFixersAutoConfiguration
 * @see de.splatgames.aether.datafixers.spring.AetherDataFixersProperties
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.spring.autoconfigure;
