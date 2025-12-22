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
 * Factory classes for constructing data fixers from bootstrap definitions.
 *
 * <p>This package provides the runtime infrastructure for building
 * {@link de.splatgames.aether.datafixers.core.AetherDataFixer} instances
 * from {@link de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap}
 * definitions.</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory} -
 *       The main factory for creating fully configured data fixers. It processes
 *       bootstrap definitions, builds registries, and wires all components together.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create factory
 * DataFixerRuntimeFactory factory = new DataFixerRuntimeFactory();
 *
 * // Build data fixer from bootstrap
 * AetherDataFixer fixer = factory.create(
 *     GameDataBootstrap.CURRENT_VERSION,
 *     new GameDataBootstrap()
 * );
 *
 * // The fixer is now ready to use
 * TaggedDynamic updated = fixer.update(data, fromVersion, toVersion);
 * }</pre>
 *
 * <h2>Factory Process</h2>
 * <p>The factory performs the following steps:</p>
 * <ol>
 *   <li>Creates empty schema and fix registries</li>
 *   <li>Invokes bootstrap's {@code registerSchemas()} method</li>
 *   <li>Invokes bootstrap's {@code registerFixes()} method</li>
 *   <li>Validates the registration (checks for gaps, conflicts)</li>
 *   <li>Builds the internal fix graph for efficient migration</li>
 *   <li>Returns a fully configured {@code AetherDataFixer}</li>
 * </ol>
 *
 * <h2>Configuration Options</h2>
 * <p>The factory supports configuration for different use cases:</p>
 * <pre>{@code
 * AetherDataFixer fixer = new DataFixerRuntimeFactory()
 *     .withContext(new Slf4jDataFixerContext())  // Use SLF4J for logging
 *     .create(currentVersion, bootstrap);
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory
 * @see de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap
 * @see de.splatgames.aether.datafixers.core.AetherDataFixer
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.core.bootstrap;
