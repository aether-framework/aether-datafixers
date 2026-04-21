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
 * Bootstrap interfaces for initialising the data fixer system.
 *
 * <p>This package provides the contract for setting up a complete data fixer
 * instance with all required schemas and fixes. The bootstrap pattern separates
 * the <i>definition</i> of schemas and fixes from the <i>runtime construction</i>
 * of the data fixer, so an application declares its migration graph once and
 * lets the core module assemble the executable pipeline.</p>
 *
 * <h2>Key Interface</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap} —
 *       The main interface that applications implement to register their schemas
 *       and data fixes. Each implementation describes the complete migration
 *       graph for an application's data model.</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <p>Applications typically create a single bootstrap implementation that
 * registers all schemas and all fixes. Fixes are always registered against a
 * {@link de.splatgames.aether.datafixers.api.TypeReference} so the runtime knows
 * which data stream each fix applies to:</p>
 * <pre>{@code
 * public class MyGameBootstrap implements DataFixerBootstrap {
 *
 *     public static final DataVersion CURRENT_VERSION = new DataVersion(200);
 *
 *     public static final TypeReference PLAYER = new TypeReference("player");
 *     public static final TypeReference WORLD  = new TypeReference("world");
 *
 *     @Override
 *     public void registerSchemas(SchemaRegistry schemas) {
 *         // Each Schema couples a DataVersion with its TypeRegistry.
 *         schemas.register(new Schema(new DataVersion(100), buildV100Types()));
 *         schemas.register(new Schema(new DataVersion(110), buildV110Types()));
 *         schemas.register(new Schema(new DataVersion(200), buildV200Types()));
 *     }
 *
 *     @Override
 *     public void registerFixes(FixRegistrar fixes) {
 *         // Every fix targets a specific TypeReference.
 *         fixes.register(PLAYER, new PlayerV1ToV2Fix());
 *         fixes.register(PLAYER, new PlayerV2ToV3Fix());
 *         fixes.registerAll(WORLD, List.of(
 *             new WorldTerrainUpgradeFix(),
 *             new WorldBiomeMigrationFix()));
 *     }
 * }
 * }</pre>
 *
 * <h2>Integration with the Runtime</h2>
 * <p>The bootstrap is consumed by
 * {@code de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory}
 * to assemble the
 * {@link de.splatgames.aether.datafixers.api.fix.DataFixer}:</p>
 * <pre>{@code
 * DataFixer fixer = new DataFixerRuntimeFactory()
 *     .create(MyGameBootstrap.CURRENT_VERSION, new MyGameBootstrap());
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap
 * @see de.splatgames.aether.datafixers.api.schema.SchemaRegistry
 * @see de.splatgames.aether.datafixers.api.fix.FixRegistrar
 * @see de.splatgames.aether.datafixers.api.fix.DataFixer
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.bootstrap;
