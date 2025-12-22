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
 * Core implementations for the Aether Datafixers framework.
 *
 * <p>This package provides the default implementations of the API interfaces
 * defined in {@link de.splatgames.aether.datafixers.api}. It contains the
 * runtime components needed to build and execute data fixers.</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.AetherDataFixer} - The main
 *       implementation of {@link de.splatgames.aether.datafixers.api.fix.DataFixer},
 *       providing the entry point for data migration operations.</li>
 * </ul>
 *
 * <h2>Package Structure</h2>
 * <p>The core module is organized into the following sub-packages:</p>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.bootstrap} - Factory classes
 *       for constructing data fixers from bootstrap definitions</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.codec} - Default codec
 *       registry implementations</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix} - DataFix implementations
 *       and supporting classes</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.schema} - Schema registry
 *       implementations</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.type} - Type registry
 *       implementations</li>
 * </ul>
 *
 * <h2>Creating a DataFixer</h2>
 * <pre>{@code
 * // Define your bootstrap
 * public class GameDataBootstrap implements DataFixerBootstrap {
 *     public static final DataVersion CURRENT_VERSION = new DataVersion(200);
 *
 *     @Override
 *     public void registerSchemas(SchemaRegistry schemas) {
 *         schemas.register(new DataVersion(100), new Schema100());
 *         schemas.register(new DataVersion(110), new Schema110());
 *         schemas.register(new DataVersion(200), new Schema200());
 *     }
 *
 *     @Override
 *     public void registerFixes(FixRegistrar fixes) {
 *         fixes.register(new PlayerV1ToV2Fix());
 *         fixes.register(new PlayerV2ToV3Fix());
 *     }
 * }
 *
 * // Create the fixer
 * AetherDataFixer fixer = new DataFixerRuntimeFactory()
 *     .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
 *
 * // Use the fixer
 * TaggedDynamic updated = fixer.update(
 *     taggedData,
 *     new DataVersion(100),
 *     fixer.currentVersion()
 * );
 * }</pre>
 *
 * <h2>Module Dependencies</h2>
 * <p>The core module depends on the API module and provides all runtime
 * functionality. Applications typically depend on core at runtime and may
 * also depend on additional codec modules for specific serialization formats.</p>
 *
 * @see de.splatgames.aether.datafixers.core.AetherDataFixer
 * @see de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory
 * @see de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.core;
