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

import de.splatgames.aether.datafixers.spring.autoconfigure.ActuatorAutoConfiguration;
import de.splatgames.aether.datafixers.spring.autoconfigure.DataFixerAutoConfiguration;
import de.splatgames.aether.datafixers.spring.autoconfigure.DynamicOpsAutoConfiguration;
import de.splatgames.aether.datafixers.spring.autoconfigure.MigrationServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Main auto-configuration entry point for Aether Datafixers.
 *
 * <p>This class coordinates the loading of all Aether Datafixers
 * auto-configuration components. It can be disabled via the property
 * {@code aether.datafixers.enabled=false}.</p>
 *
 * <h2>Components Configured</h2>
 * <ul>
 *   <li>{@link DynamicOpsAutoConfiguration} - GsonOps and JacksonJsonOps beans</li>
 *   <li>{@link DataFixerAutoConfiguration} - AetherDataFixer beans</li>
 *   <li>{@link MigrationServiceAutoConfiguration} - MigrationService with fluent API</li>
 *   <li>{@link ActuatorAutoConfiguration} - Health, Info, Endpoint, and Metrics</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // 1. Add the starter dependency
 * // 2. Create a DataFixerBootstrap bean
 * @Bean
 * public DataFixerBootstrap myBootstrap() {
 *     return new MyDataFixerBootstrap();
 * }
 *
 * // 3. Inject and use MigrationService
 * @Autowired
 * private MigrationService migrationService;
 *
 * public void migrate(TaggedDynamic data) {
 *     MigrationResult result = migrationService
 *         .migrate(data)
 *         .from(100)
 *         .toLatest()
 *         .execute();
 * }
 * }</pre>
 *
 * @author Erik Pfoertner
 * @see AetherDataFixersProperties
 * @see DynamicOpsAutoConfiguration
 * @see DataFixerAutoConfiguration
 * @see MigrationServiceAutoConfiguration
 * @see ActuatorAutoConfiguration
 * @since 0.4.0
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "aether.datafixers",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(AetherDataFixersProperties.class)
@Import({
        DynamicOpsAutoConfiguration.class,
        DataFixerAutoConfiguration.class,
        MigrationServiceAutoConfiguration.class,
        ActuatorAutoConfiguration.class
})
public class AetherDataFixersAutoConfiguration {
    // Marker class - all configuration is in imported classes
}
