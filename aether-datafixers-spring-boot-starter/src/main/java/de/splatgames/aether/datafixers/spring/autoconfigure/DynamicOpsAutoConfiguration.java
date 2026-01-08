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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.codec.jackson.JacksonOps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Auto-configuration for {@link DynamicOps} beans based on classpath detection.
 *
 * <p>This configuration class provides conditional bean definitions for the serialization
 * format adapters used by the Aether Datafixers framework. It automatically detects
 * available JSON libraries (Gson, Jackson) on the classpath and configures appropriate
 * {@link DynamicOps} implementations.</p>
 *
 * <h2>Activation Conditions</h2>
 * <p>This auto-configuration is activated when:</p>
 * <ul>
 *   <li>{@code aether.datafixers.enabled} is {@code true} (default)</li>
 * </ul>
 *
 * <h2>Automatic Format Detection</h2>
 * <p>The configuration uses classpath scanning to detect available JSON libraries:</p>
 * <table border="1" cellpadding="5">
 *   <caption>Library Detection</caption>
 *   <tr>
 *     <th>Library</th>
 *     <th>Detection Class</th>
 *     <th>DynamicOps Bean</th>
 *   </tr>
 *   <tr>
 *     <td>Gson</td>
 *     <td>{@code com.google.gson.Gson}</td>
 *     <td>{@link GsonOps}</td>
 *   </tr>
 *   <tr>
 *     <td>Jackson</td>
 *     <td>{@code com.fasterxml.jackson.databind.ObjectMapper}</td>
 *     <td>{@link JacksonOps}</td>
 *   </tr>
 * </table>
 *
 * <h2>Default Format Selection</h2>
 * <p>When both libraries are available, the default format is determined by the
 * {@code aether.datafixers.default-format} property:</p>
 * <ul>
 *   <li>{@code gson} (default) - Uses {@link GsonOps#INSTANCE} as the primary DynamicOps</li>
 *   <li>{@code jackson} - Uses {@link JacksonOps} as the primary DynamicOps</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <pre>{@code
 * # application.yml
 * aether:
 *   datafixers:
 *     enabled: true           # Enable auto-configuration (default: true)
 *     default-format: gson    # Default serialization format (gson or jackson)
 * }</pre>
 *
 * <h2>Spring ObjectMapper Integration</h2>
 * <p>When Jackson is selected, the configuration automatically integrates with Spring's
 * {@link ObjectMapper} bean if one exists. This ensures consistent JSON serialization
 * settings (date formats, naming strategies, etc.) across the application.</p>
 *
 * <h2>Bean Injection</h2>
 * <pre>{@code
 * // Inject specific format
 * @Service
 * public class DataService {
 *     private final GsonOps gsonOps;
 *
 *     public DataService(GsonOps gsonOps) {
 *         this.gsonOps = gsonOps;
 *     }
 * }
 *
 * // Inject default format (selected by default-format property)
 * @Service
 * public class MigrationService {
 *     private final DynamicOps<?> ops;
 *
 *     public MigrationService(DynamicOps<?> ops) {
 *         this.ops = ops;  // Injects primary (default) DynamicOps
 *     }
 * }
 * }</pre>
 *
 * <h2>Custom DynamicOps</h2>
 * <p>To use a custom {@link DynamicOps} implementation, define your own bean:</p>
 * <pre>{@code
 * @Configuration
 * public class CustomOpsConfig {
 *
 *     @Bean
 *     @Primary
 *     public DynamicOps<?> customOps() {
 *         return new MyCustomDynamicOps();
 *     }
 * }
 * }</pre>
 * <p>This will prevent the auto-configured default from being created.</p>
 *
 * @author Erik Pfoertner
 * @see DynamicOps
 * @see GsonOps
 * @see JacksonOps
 * @see de.splatgames.aether.datafixers.spring.config.DynamicOpsFormat
 * @since 0.4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "aether.datafixers",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DynamicOpsAutoConfiguration {

    /**
     * Nested configuration for Gson-based {@link DynamicOps} beans.
     *
     * <p>This configuration is activated only when Gson is available on the classpath.
     * It provides both the specific {@link GsonOps} bean and conditionally sets it
     * as the primary {@link DynamicOps} based on the default-format configuration.</p>
     *
     * @author Erik Pfoertner
     * @since 0.4.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.google.gson.Gson")
    static class GsonOpsConfiguration {

        /**
         * Provides the {@link GsonOps} singleton instance.
         *
         * <p>Uses the stateless singleton {@link GsonOps#INSTANCE} which is suitable
         * for most use cases. The bean is created only if no custom {@code gsonOps}
         * bean is already defined.</p>
         *
         * @return the GsonOps singleton instance
         */
        @Bean
        @ConditionalOnMissingBean(name = "gsonOps")
        public GsonOps gsonOps() {
            return GsonOps.INSTANCE;
        }

        /**
         * Registers {@link GsonOps} as the primary (default) {@link DynamicOps} bean.
         *
         * <p>This bean is created when:</p>
         * <ul>
         *   <li>No other DynamicOps bean is defined</li>
         *   <li>{@code aether.datafixers.default-format} is {@code gson} (or not set)</li>
         * </ul>
         *
         * @param gsonOps the GsonOps instance to register as primary
         * @return the primary DynamicOps instance
         */
        @Bean
        @Primary
        @ConditionalOnMissingBean(DynamicOps.class)
        @ConditionalOnProperty(
                prefix = "aether.datafixers",
                name = "default-format",
                havingValue = "gson",
                matchIfMissing = true
        )
        public DynamicOps<?> defaultGsonOps(final GsonOps gsonOps) {
            return gsonOps;
        }
    }

    /**
     * Nested configuration for Jackson-based {@link DynamicOps} beans.
     *
     * <p>This configuration is activated only when Jackson is available on the classpath.
     * It provides both the specific {@link JacksonOps} bean (with Spring ObjectMapper
     * integration) and conditionally sets it as the primary {@link DynamicOps}.</p>
     *
     * @author Erik Pfoertner
     * @since 0.4.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    static class JacksonOpsConfiguration {

        /**
         * Provides the {@link JacksonOps} instance with Spring ObjectMapper integration.
         *
         * <p>If a Spring-managed {@link ObjectMapper} bean exists, it will be used to
         * ensure consistent JSON configuration. This means features like:</p>
         * <ul>
         *   <li>Date/time formatting settings</li>
         *   <li>Property naming strategies</li>
         *   <li>Registered modules (JavaTimeModule, etc.)</li>
         *   <li>Serialization/deserialization features</li>
         * </ul>
         * <p>are shared between JacksonOps and other Jackson-based components.</p>
         *
         * <p>If no ObjectMapper bean exists, falls back to {@link JacksonOps#INSTANCE}.</p>
         *
         * @param objectMapper Spring's configured ObjectMapper, may be {@code null}
         * @return the JacksonOps instance configured with the appropriate ObjectMapper
         */
        @Bean
        @ConditionalOnMissingBean(name = "jacksonOps")
        public JacksonOps jacksonOps(
                @Autowired(required = false) final ObjectMapper objectMapper
        ) {
            // Use Spring's ObjectMapper if available for consistent configuration
            return objectMapper != null
                    ? new JacksonOps(objectMapper)
                    : JacksonOps.INSTANCE;
        }

        /**
         * Registers {@link JacksonOps} as the primary (default) {@link DynamicOps} bean.
         *
         * <p>This bean is created when:</p>
         * <ul>
         *   <li>No other DynamicOps bean is defined</li>
         *   <li>{@code aether.datafixers.default-format} is explicitly set to {@code jackson}</li>
         * </ul>
         *
         * @param jacksonOps the JacksonOps instance to register as primary
         * @return the primary DynamicOps instance
         */
        @Bean
        @Primary
        @ConditionalOnMissingBean(DynamicOps.class)
        @ConditionalOnProperty(
                prefix = "aether.datafixers",
                name = "default-format",
                havingValue = "jackson"
        )
        public DynamicOps<?> defaultJacksonOps(final JacksonOps jacksonOps) {
            return jacksonOps;
        }
    }
}
