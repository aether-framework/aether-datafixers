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

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory;
import de.splatgames.aether.datafixers.spring.AetherDataFixersProperties;
import de.splatgames.aether.datafixers.spring.config.DataFixerDomainProperties;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Auto-configuration for {@link AetherDataFixer} beans from {@link DataFixerBootstrap} definitions.
 *
 * <p>This configuration class is responsible for creating and registering {@link AetherDataFixer}
 * instances based on discovered {@link DataFixerBootstrap} beans in the application context.
 * It supports both simple single-bootstrap setups and complex multi-domain configurations.</p>
 *
 * <h2>Activation Conditions</h2>
 * <p>This auto-configuration is activated when all of the following conditions are met:</p>
 * <ul>
 *   <li>{@code aether.datafixers.enabled} is {@code true} (default)</li>
 *   <li>At least one {@link DataFixerBootstrap} bean is present in the context</li>
 * </ul>
 *
 * <h2>Single Bootstrap Setup</h2>
 * <p>For applications with a single data type or unified schema evolution, define one
 * bootstrap bean. The resulting {@link AetherDataFixer} will be registered as the primary
 * bean under the default domain.</p>
 * <pre>{@code
 * @Configuration
 * public class DataFixerConfig {
 *
 *     @Bean
 *     public DataFixerBootstrap myBootstrap() {
 *         return new MyDataFixerBootstrap();
 *     }
 * }
 * }</pre>
 *
 * <h2>Multi-Domain Setup</h2>
 * <p>For applications managing multiple independent data types, define multiple qualified
 * bootstrap beans. Each will be registered in the {@link DataFixerRegistry} under its
 * qualifier name.</p>
 * <pre>{@code
 * @Configuration
 * public class DataFixerConfig {
 *
 *     @Bean
 *     @Qualifier("game")
 *     public DataFixerBootstrap gameBootstrap() {
 *         return new GameDataBootstrap();
 *     }
 *
 *     @Bean
 *     @Qualifier("user")
 *     public DataFixerBootstrap userBootstrap() {
 *         return new UserDataBootstrap();
 *     }
 * }
 * }</pre>
 *
 * <h2>Version Resolution</h2>
 * <p>The current version for each DataFixer is resolved in the following order:</p>
 * <ol>
 *   <li><strong>Domain Properties:</strong> {@code aether.datafixers.domains.<domain>.current-version}</li>
 *   <li><strong>Bootstrap Constant:</strong> A public static {@code CURRENT_VERSION} field of type
 *       {@link DataVersion} in the bootstrap class</li>
 *   <li><strong>Default Version:</strong> {@code aether.datafixers.default-current-version}</li>
 * </ol>
 * <p>If none of these sources provide a version, startup fails with an {@link IllegalStateException}.</p>
 *
 * <h2>Bootstrap Class with CURRENT_VERSION</h2>
 * <pre>{@code
 * public class GameDataBootstrap implements DataFixerBootstrap {
 *
 *     // This constant is automatically detected via reflection
 *     public static final DataVersion CURRENT_VERSION = new DataVersion(200);
 *
 *     @Override
 *     public void registerSchemas(SchemaRegistry schemas) {
 *         // Schema definitions
 *     }
 *
 *     @Override
 *     public void registerFixes(FixRegistrar fixes) {
 *         // Fix registrations
 *     }
 * }
 * }</pre>
 *
 * <h2>Configuration Properties</h2>
 * <pre>{@code
 * # application.yml
 * aether:
 *   datafixers:
 *     enabled: true                    # Enable auto-configuration (default: true)
 *     default-current-version: 100     # Fallback version for all domains
 *     domains:
 *       game:
 *         current-version: 200         # Override version for specific domain
 *       user:
 *         current-version: 150
 * }</pre>
 *
 * <h2>Injecting the DataFixer</h2>
 * <pre>{@code
 * // Single-domain setup (unqualified injection)
 * @Service
 * public class GameService {
 *     private final AetherDataFixer fixer;
 *
 *     public GameService(AetherDataFixer fixer) {
 *         this.fixer = fixer;
 *     }
 * }
 *
 * // Multi-domain setup (qualified injection)
 * @Service
 * public class MultiDomainService {
 *     private final AetherDataFixer gameFixer;
 *     private final AetherDataFixer userFixer;
 *
 *     public MultiDomainService(
 *             @Qualifier("game") AetherDataFixer gameFixer,
 *             @Qualifier("user") AetherDataFixer userFixer) {
 *         this.gameFixer = gameFixer;
 *         this.userFixer = userFixer;
 *     }
 * }
 * }</pre>
 *
 * <h2>Order of Configuration</h2>
 * <p>This configuration runs after {@link DynamicOpsAutoConfiguration} to ensure
 * that DynamicOps beans are available if needed. The registration happens in the
 * following order:</p>
 * <ol>
 *   <li>Create {@link DataFixerRegistry} bean</li>
 *   <li>Create {@link AetherDataFixer} beans from bootstraps</li>
 *   <li>Register each fixer in the registry</li>
 * </ol>
 *
 * @author Erik Pförtner
 * @see DataFixerBootstrap
 * @see AetherDataFixer
 * @see DataFixerRegistry
 * @see DataFixerRuntimeFactory
 * @see AetherDataFixersProperties
 * @since 0.4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "aether.datafixers",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnBean(DataFixerBootstrap.class)
@EnableConfigurationProperties(AetherDataFixersProperties.class)
public class DataFixerAutoConfiguration {

    /**
     * Logger for this configuration class.
     * Logs at INFO level for creation events and DEBUG for version resolution details.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataFixerAutoConfiguration.class);

    /**
     * Creates the central {@link DataFixerRegistry} bean for managing domain-to-fixer mappings.
     *
     * <p>The registry is created only if no other registry bean exists in the context.
     * It serves as the central repository for all DataFixer instances and is used by
     * the {@link de.splatgames.aether.datafixers.spring.service.MigrationService} and
     * actuator components.</p>
     *
     * @return a new DataFixerRegistry instance
     */
    @Bean
    @ConditionalOnMissingBean
    public DataFixerRegistry dataFixerRegistry() {
        return new DataFixerRegistry();
    }

    /**
     * Creates the primary {@link AetherDataFixer} bean for single-bootstrap setups.
     *
     * <p>This bean is created when exactly one {@link DataFixerBootstrap} bean exists
     * in the application context (detected by {@code @ConditionalOnSingleCandidate}).
     * The fixer is marked as {@code @Primary} and registered under the default domain.</p>
     *
     * <h3>Activation Conditions</h3>
     * <ul>
     *   <li>Exactly one DataFixerBootstrap bean in context</li>
     *   <li>No AetherDataFixer bean already defined</li>
     * </ul>
     *
     * @param bootstrap  the single DataFixerBootstrap bean from the context
     * @param properties the configuration properties for version resolution
     * @param registry   the DataFixer registry for domain registration
     * @return the configured and registered AetherDataFixer instance
     */
    @Bean
    @Primary
    @ConditionalOnSingleCandidate(DataFixerBootstrap.class)
    @ConditionalOnMissingBean(AetherDataFixer.class)
    public AetherDataFixer aetherDataFixer(
            final DataFixerBootstrap bootstrap,
            final AetherDataFixersProperties properties,
            final DataFixerRegistry registry
    ) {
        LOG.info("Creating primary AetherDataFixer from bootstrap: {}",
                bootstrap.getClass().getName());

        final DataVersion version = resolveVersion(bootstrap, properties, DataFixerRegistry.DEFAULT_DOMAIN);
        final AetherDataFixer fixer = createFixer(version, bootstrap);

        registry.register(DataFixerRegistry.DEFAULT_DOMAIN, fixer);

        LOG.info("AetherDataFixer created with current version: {}", version.getVersion());
        return fixer;
    }

    /**
     * Factory method for creating {@link AetherDataFixer} instances from qualified bootstrap beans.
     *
     * <p>This static method is designed to be called from user-defined configuration classes
     * when setting up multi-domain DataFixer configurations. It handles version resolution,
     * fixer creation, and registry registration in a single operation.</p>
     *
     * <h3>Usage Example</h3>
     * <pre>{@code
     * @Configuration
     * public class MultiDomainConfig {
     *
     *     @Bean("gameDataFixer")
     *     @Qualifier("game")
     *     public AetherDataFixer gameDataFixer(
     *             @Qualifier("game") DataFixerBootstrap bootstrap,
     *             AetherDataFixersProperties properties,
     *             DataFixerRegistry registry) {
     *         return DataFixerAutoConfiguration.createQualifiedFixer(
     *             bootstrap, "game", properties, registry);
     *     }
     * }
     * }</pre>
     *
     * @param bootstrap  the qualified bootstrap bean to create the fixer from
     * @param qualifier  the domain qualifier name (used for registry and logging)
     * @param properties the configuration properties for version resolution
     * @param registry   the DataFixer registry for domain registration
     * @return the configured and registered AetherDataFixer instance
     * @throws IllegalStateException if the version cannot be determined for the domain
     */
    @NotNull
    public static AetherDataFixer createQualifiedFixer(
            @NotNull final DataFixerBootstrap bootstrap,
            @NotNull final String qualifier,
            @NotNull final AetherDataFixersProperties properties,
            @NotNull final DataFixerRegistry registry
    ) {
        LOG.info("Creating AetherDataFixer for domain '{}' from bootstrap: {}",
                qualifier, bootstrap.getClass().getName());

        final DataVersion version = resolveVersion(bootstrap, properties, qualifier);
        final AetherDataFixer fixer = createFixer(version, bootstrap);

        registry.register(qualifier, fixer);

        LOG.info("AetherDataFixer for domain '{}' created with version: {}",
                qualifier, version.getVersion());
        return fixer;
    }

    /**
     * Creates an {@link AetherDataFixer} instance using the {@link DataFixerRuntimeFactory}.
     *
     * <p>This method encapsulates the factory invocation for consistent fixer creation
     * across all code paths.</p>
     *
     * @param version   the current version to initialize the fixer with
     * @param bootstrap the bootstrap containing schema and fix definitions
     * @return a fully initialized AetherDataFixer instance
     */
    @NotNull
    private static AetherDataFixer createFixer(
            @NotNull final DataVersion version,
            @NotNull final DataFixerBootstrap bootstrap
    ) {
        return new DataFixerRuntimeFactory().create(version, bootstrap);
    }

    /**
     * Resolves the current schema version for a specific domain.
     *
     * <p>This method implements a cascading version resolution strategy that checks
     * multiple sources in priority order. This allows for flexible version configuration
     * from properties while supporting convention-based defaults.</p>
     *
     * <h3>Resolution Order</h3>
     * <ol>
     *   <li><strong>Domain Properties:</strong> {@code aether.datafixers.domains.<domain>.current-version}
     *       - Highest priority, allows per-domain override</li>
     *   <li><strong>Bootstrap Constant:</strong> A public static {@code CURRENT_VERSION} field of type
     *       {@link DataVersion} in the bootstrap class - Convention-based default</li>
     *   <li><strong>Global Default:</strong> {@code aether.datafixers.default-current-version}
     *       - Fallback for all domains</li>
     * </ol>
     *
     * <h3>Error Handling</h3>
     * <p>If none of the sources provide a version, this method throws an {@link IllegalStateException}
     * with a detailed message explaining the available configuration options.</p>
     *
     * @param bootstrap  the bootstrap to resolve version for
     * @param properties the configuration properties to check
     * @param domain     the domain name (for property lookup and error messages)
     * @return the resolved DataVersion
     * @throws IllegalStateException if version cannot be determined from any source
     */
    @NotNull
    private static DataVersion resolveVersion(
            @NotNull final DataFixerBootstrap bootstrap,
            @NotNull final AetherDataFixersProperties properties,
            @NotNull final String domain
    ) {
        // 1. Check domain-specific properties
        final Map<String, DataFixerDomainProperties> domains = properties.getDomains();
        final DataFixerDomainProperties domainProps = domains.get(domain);
        if (domainProps != null && domainProps.getCurrentVersion() != null) {
            LOG.debug("Using version from properties for domain '{}': {}",
                    domain, domainProps.getCurrentVersion());
            return new DataVersion(domainProps.getCurrentVersion());
        }

        // 2. Try to find CURRENT_VERSION constant via reflection
        try {
            final Field field = bootstrap.getClass().getField("CURRENT_VERSION");
            if (DataVersion.class.isAssignableFrom(field.getType())) {
                final DataVersion version = (DataVersion) field.get(null);
                LOG.debug("Using CURRENT_VERSION constant from bootstrap for domain '{}': {}",
                        domain, version.getVersion());
                return version;
            }
        } catch (final NoSuchFieldException e) {
            LOG.debug("No CURRENT_VERSION field found in bootstrap: {}", bootstrap.getClass().getName());
        } catch (final IllegalAccessException e) {
            LOG.warn("Cannot access CURRENT_VERSION field in bootstrap: {}", bootstrap.getClass().getName());
        }

        // 3. Fall back to default version
        final Integer defaultVersion = properties.getDefaultCurrentVersion();
        if (defaultVersion != null) {
            LOG.debug("Using default version for domain '{}': {}", domain, defaultVersion);
            return new DataVersion(defaultVersion);
        }

        throw new IllegalStateException(
                "Cannot determine current version for domain '" + domain + "'. " +
                "Either define a CURRENT_VERSION constant in your bootstrap class (" +
                bootstrap.getClass().getName() + "), " +
                "or configure 'aether.datafixers.domains." + domain + ".current-version' " +
                "or 'aether.datafixers.default-current-version' in your application properties."
        );
    }
}
