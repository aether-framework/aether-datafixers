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
 * Configuration properties and enums for the Aether Datafixers Spring Boot integration.
 *
 * <p>This package contains supporting configuration classes and enums used by the
 * Spring Boot auto-configuration to customize the behavior of Aether Datafixers.
 * These classes are bound to configuration properties via Spring's property binding
 * mechanism.</p>
 *
 * <h2>Package Contents</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.spring.config.DynamicOpsFormat}
 *       - Enum defining supported serialization formats (GSON, JACKSON)</li>
 *   <li>{@link de.splatgames.aether.datafixers.spring.config.DataFixerDomainProperties}
 *       - Per-domain configuration for multi-domain setups</li>
 * </ul>
 *
 * <h2>Configuration Example</h2>
 * <pre>{@code
 * # application.yml
 * aether:
 *   datafixers:
 *     default-format: gson              # Uses DynamicOpsFormat enum
 *     domains:
 *       game:                           # Uses DataFixerDomainProperties
 *         current-version: 200
 *         primary: true
 *         description: "Game save migrations"
 *       user:
 *         current-version: 150
 *         description: "User profile migrations"
 * }</pre>
 *
 * <h2>Multi-Domain Configuration</h2>
 * <p>The domain properties support managing multiple independent DataFixer instances,
 * each with its own version history and configuration. This is useful when:</p>
 * <ul>
 *   <li>Different data types evolve at different rates</li>
 *   <li>You need isolated migration paths for different entities</li>
 *   <li>Some domains require different migration strategies</li>
 * </ul>
 *
 * <h2>Version Resolution</h2>
 * <p>Domain version resolution follows this priority:</p>
 * <ol>
 *   <li>{@code domains.<name>.current-version} from properties</li>
 *   <li>{@code CURRENT_VERSION} constant in the bootstrap class</li>
 *   <li>{@code default-current-version} from global properties</li>
 * </ol>
 *
 * @author Erik Pförtner
 * @see de.splatgames.aether.datafixers.spring.AetherDataFixersProperties
 * @see de.splatgames.aether.datafixers.spring.config.DynamicOpsFormat
 * @see de.splatgames.aether.datafixers.spring.config.DataFixerDomainProperties
 * @since 0.4.0
 */
package de.splatgames.aether.datafixers.spring.config;
