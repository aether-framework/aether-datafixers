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
 * Data fix interfaces and the main DataFixer entry point.
 *
 * <p>This package contains the core interfaces for defining and applying data
 * migrations. It provides the primary API that applications use to update serialized data from older versions to newer
 * versions.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.DataFixer} - The main entry
 *       point for applying data fixes. Orchestrates the application of multiple
 *       fixes to migrate data across version boundaries.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.DataFix} - Represents a single
 *       migration step that transforms data from one version to another. Each fix
 *       has a source version, target version, and transformation logic.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.FixRegistrar} - Registry
 *       interface for registering data fixes during bootstrap.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.DataFixerContext} - Context
 *       providing logging and diagnostics during fix execution.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.Fixes} - Factory methods for
 *       creating common fix types.</li>
 * </ul>
 *
 * <h2>Migration Flow</h2>
 * <p>When updating data, the DataFixer:</p>
 * <ol>
 *   <li>Identifies all fixes between the source and target versions</li>
 *   <li>Orders fixes by version to ensure correct application order</li>
 *   <li>Applies each fix in sequence, passing the output of one to the next</li>
 *   <li>Returns the fully migrated data at the target version</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Get a configured DataFixer
 * DataFixer fixer = ...;
 *
 * // Load old save data
 * Dynamic<JsonElement> oldData = new Dynamic<>(GsonOps.INSTANCE, json);
 * TaggedDynamic tagged = new TaggedDynamic(TypeReferences.PLAYER, oldData);
 *
 * // Migrate from version 100 to current version
 * TaggedDynamic updated = fixer.update(
 *     tagged,
 *     new DataVersion(100),
 *     fixer.currentVersion()
 * );
 * }</pre>
 *
 * <h2>Implementing DataFix</h2>
 * <p>For simple fixes, implement {@link de.splatgames.aether.datafixers.api.fix.DataFix}
 * directly. For fixes that need access to schema information, extend
 * {@code SchemaDataFix} from the core module:</p>
 * <pre>{@code
 * public class PlayerV1ToV2Fix extends SchemaDataFix {
 *     public PlayerV1ToV2Fix() {
 *         super(new DataVersion(100), new DataVersion(110), TypeReferences.PLAYER);
 *     }
 *
 *     @Override
 *     protected TypeRewriteRule makeRule(Schema input, Schema output) {
 *         return Rules.renameField("playerName", "name");
 *     }
 * }
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.api.fix.DataFixer
 * @see de.splatgames.aether.datafixers.api.fix.DataFix
 * @see de.splatgames.aether.datafixers.api.fix.FixRegistrar
 * @see de.splatgames.aether.datafixers.api.DataVersion
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.fix;
