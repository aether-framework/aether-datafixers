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
 *   <li>{@link de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory}
 *       — Factory for assembling a fully configured
 *       {@link de.splatgames.aether.datafixers.core.AetherDataFixer} from a
 *       bootstrap. Processes the bootstrap, builds registries, freezes the
 *       schema registry, and wires the pieces together.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AetherDataFixer fixer = new DataFixerRuntimeFactory()
 *     .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
 *
 * // The fixer is now ready to migrate data
 * TaggedDynamic updated = fixer.update(taggedData, fromVersion, toVersion);
 * }</pre>
 *
 * <h2>Factory Process</h2>
 * <p>{@link de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory#create(de.splatgames.aether.datafixers.api.DataVersion, de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap) create}
 * performs the following steps:</p>
 * <ol>
 *   <li>Creates an empty
 *       {@link de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry}
 *       and invokes the bootstrap's
 *       {@link de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap#registerSchemas(de.splatgames.aether.datafixers.api.schema.SchemaRegistry) registerSchemas}.</li>
 *   <li>Emits a warning log if the bootstrap registered no schemas or has no
 *       schema for the current version.</li>
 *   <li>Freezes the schema registry against further mutation.</li>
 *   <li>Creates a
 *       {@link de.splatgames.aether.datafixers.core.fix.DataFixerBuilder} and
 *       invokes the bootstrap's
 *       {@link de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap#registerFixes(de.splatgames.aether.datafixers.api.fix.FixRegistrar) registerFixes}.</li>
 *   <li>Builds an underlying
 *       {@link de.splatgames.aether.datafixers.api.fix.DataFixer} and wraps
 *       everything in an
 *       {@link de.splatgames.aether.datafixers.core.AetherDataFixer}.</li>
 * </ol>
 *
 * <h2>Logging and Diagnostics</h2>
 * <p>The factory itself does not accept a {@link de.splatgames.aether.datafixers.api.fix.DataFixerContext}.
 * Logging contexts are supplied per migration call instead: pass a
 * {@link de.splatgames.aether.datafixers.core.fix.Slf4jDataFixerContext},
 * {@link de.splatgames.aether.datafixers.core.fix.SimpleSystemDataFixerContext},
 * or a
 * {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}
 * to the 5-argument
 * {@link de.splatgames.aether.datafixers.api.fix.DataFixer#update(de.splatgames.aether.datafixers.api.TypeReference, de.splatgames.aether.datafixers.api.dynamic.Dynamic, de.splatgames.aether.datafixers.api.DataVersion, de.splatgames.aether.datafixers.api.DataVersion, de.splatgames.aether.datafixers.api.fix.DataFixerContext) update}
 * overload.</p>
 *
 * @see de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory
 * @see de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap
 * @see de.splatgames.aether.datafixers.core.AetherDataFixer
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.core.bootstrap;
