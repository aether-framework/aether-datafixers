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
import de.splatgames.aether.datafixers.spring.metrics.MigrationMetrics;
import de.splatgames.aether.datafixers.spring.service.DefaultMigrationService;
import de.splatgames.aether.datafixers.spring.service.MigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the {@link MigrationService} bean.
 *
 * <p>This configuration class provides a ready-to-use {@link MigrationService} instance
 * that offers a fluent API for performing data migrations. The service integrates with
 * the {@link DataFixerRegistry} for multi-domain support and optionally with
 * {@link MigrationMetrics} for observability.</p>
 *
 * <h2>Activation Conditions</h2>
 * <p>This auto-configuration is activated when all of the following conditions are met:</p>
 * <ul>
 *   <li>{@code aether.datafixers.enabled} is {@code true} (default)</li>
 *   <li>At least one {@link AetherDataFixer} bean exists in the context</li>
 * </ul>
 *
 * <h2>Service Capabilities</h2>
 * <p>The auto-configured {@link MigrationService} provides:</p>
 * <ul>
 *   <li>Fluent builder API for configuring migrations</li>
 *   <li>Multi-domain support via {@code usingDomain()}</li>
 *   <li>Both synchronous and asynchronous execution</li>
 *   <li>Automatic metrics recording when {@link MigrationMetrics} is available</li>
 *   <li>Comprehensive result objects with success/failure status</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @Service
 * public class GameDataService {
 *
 *     private final MigrationService migrationService;
 *
 *     public GameDataService(MigrationService migrationService) {
 *         this.migrationService = migrationService;
 *     }
 *
 *     public TaggedDynamic<?> migrateGameSave(TaggedDynamic<?> savedData, int savedVersion) {
 *         MigrationResult result = migrationService
 *             .migrate(savedData)
 *             .from(savedVersion)
 *             .toLatest()
 *             .execute();
 *
 *         if (result.isSuccess()) {
 *             return result.getData();
 *         } else {
 *             throw new MigrationException("Failed to migrate", result.getError().orElse(null));
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Metrics Integration</h2>
 * <p>When {@link MigrationMetrics} is available (typically when Micrometer is on the classpath
 * and actuator is configured), the service automatically records:</p>
 * <ul>
 *   <li>Success/failure counters per domain</li>
 *   <li>Migration duration timings</li>
 *   <li>Version span distribution</li>
 * </ul>
 *
 * <h2>Custom MigrationService</h2>
 * <p>To customize the MigrationService, define your own bean:</p>
 * <pre>{@code
 * @Configuration
 * public class CustomMigrationConfig {
 *
 *     @Bean
 *     public MigrationService customMigrationService(
 *             DataFixerRegistry registry,
 *             MigrationMetrics metrics,
 *             @Qualifier("migrationExecutor") Executor executor) {
 *         return new DefaultMigrationService(registry, metrics, executor);
 *     }
 * }
 * }</pre>
 * <p>This will prevent the auto-configured service from being created.</p>
 *
 * <h2>Configuration Order</h2>
 * <p>This configuration runs after {@link DataFixerAutoConfiguration} to ensure that
 * the {@link DataFixerRegistry} and {@link AetherDataFixer} beans are available.</p>
 *
 * @author Erik Pfoertner
 * @see MigrationService
 * @see DefaultMigrationService
 * @see MigrationMetrics
 * @see DataFixerRegistry
 * @since 0.4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "aether.datafixers",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnBean(AetherDataFixer.class)
public class MigrationServiceAutoConfiguration {

    /**
     * Creates the {@link MigrationService} bean with optional metrics integration.
     *
     * <p>The service is configured with:</p>
     * <ul>
     *   <li>The {@link DataFixerRegistry} for domain lookup and multi-domain support</li>
     *   <li>Optional {@link MigrationMetrics} for observability (injected if available)</li>
     *   <li>Default executor (ForkJoinPool.commonPool) for async operations</li>
     * </ul>
     *
     * <p>This bean is only created if no other MigrationService bean is defined,
     * allowing users to provide custom implementations.</p>
     *
     * @param registry the DataFixer registry for domain resolution
     * @param metrics  the migration metrics recorder, may be {@code null} if metrics
     *                 are disabled or Micrometer is not available
     * @return a new DefaultMigrationService instance
     */
    @Bean
    @ConditionalOnMissingBean
    public MigrationService migrationService(
            final DataFixerRegistry registry,
            @Autowired(required = false) final MigrationMetrics metrics
    ) {
        return new DefaultMigrationService(registry, metrics);
    }
}
