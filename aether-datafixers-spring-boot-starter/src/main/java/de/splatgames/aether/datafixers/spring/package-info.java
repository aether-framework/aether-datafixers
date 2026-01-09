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
 * Spring Boot auto-configuration for the Aether Datafixers data migration framework.
 *
 * <p>This is the root package of the Aether Datafixers Spring Boot Starter, providing
 * comprehensive integration with Spring Boot 3.x. The starter enables automatic configuration
 * of data migration components, making it easy to integrate schema evolution capabilities
 * into Spring-based applications.</p>
 *
 * <h2>Module Overview</h2>
 * <p>The Spring Boot Starter provides the following capabilities:</p>
 * <ul>
 *   <li><strong>Auto-Configuration:</strong> Automatic bean creation based on classpath and configuration</li>
 *   <li><strong>Multi-Domain Support:</strong> Manage multiple independent DataFixer instances</li>
 *   <li><strong>Fluent Migration API:</strong> Intuitive builder pattern for data migrations</li>
 *   <li><strong>Actuator Integration:</strong> Health checks, info endpoints, and custom endpoints</li>
 *   <li><strong>Metrics Support:</strong> Micrometer-based observability for migrations</li>
 *   <li><strong>Format Flexibility:</strong> Support for both Gson and Jackson serialization</li>
 * </ul>
 *
 * <h2>Package Structure</h2>
 * <table border="1" cellpadding="5">
 *   <caption>Sub-packages</caption>
 *   <tr>
 *     <th>Package</th>
 *     <th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.autoconfigure}</td>
 *     <td>Auto-configuration classes for beans and components</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.config}</td>
 *     <td>Configuration property classes and enums</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.service}</td>
 *     <td>Migration service with fluent API</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.actuator}</td>
 *     <td>Spring Boot Actuator components</td>
 *   </tr>
 *   <tr>
 *     <td>{@link de.splatgames.aether.datafixers.spring.metrics}</td>
 *     <td>Micrometer metrics integration</td>
 *   </tr>
 * </table>
 *
 * <h2>Quick Start Guide</h2>
 *
 * <h3>1. Add Dependency</h3>
 * <pre>{@code
 * <dependency>
 *     <groupId>de.splatgames.aether</groupId>
 *     <artifactId>aether-datafixers-spring-boot-starter</artifactId>
 *     <version>0.4.0</version>
 * </dependency>
 * }</pre>
 *
 * <h3>2. Create Bootstrap Bean</h3>
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
 * <h3>3. Use MigrationService</h3>
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
 *     public TaggedDynamic<?> migrateGameSave(TaggedDynamic<?> savedData, int version) {
 *         MigrationResult result = migrationService
 *             .migrate(savedData)
 *             .from(version)
 *             .toLatest()
 *             .execute();
 *
 *         if (result.isSuccess()) {
 *             return result.getData();
 *         }
 *         throw new MigrationException(result.getError().orElse(null));
 *     }
 * }
 * }</pre>
 *
 * <h2>Configuration Properties</h2>
 * <pre>{@code
 * # application.yml
 * aether:
 *   datafixers:
 *     enabled: true                    # Enable/disable auto-configuration
 *     default-format: gson             # Default serialization format
 *     default-current-version: 200     # Fallback version number
 *     domains:
 *       game:
 *         current-version: 200
 *         description: "Game save migrations"
 * }</pre>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.spring.AetherDataFixersAutoConfiguration}
 *       - Main entry point for auto-configuration</li>
 *   <li>{@link de.splatgames.aether.datafixers.spring.AetherDataFixersProperties}
 *       - Configuration properties binding class</li>
 * </ul>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.spring.AetherDataFixersAutoConfiguration
 * @see de.splatgames.aether.datafixers.spring.AetherDataFixersProperties
 * @see de.splatgames.aether.datafixers.spring.service.MigrationService
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.spring;
