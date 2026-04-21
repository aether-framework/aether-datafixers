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
 * Data fix interfaces and the main {@code DataFixer} entry point.
 *
 * <p>This package contains the core interfaces for defining and applying data
 * migrations. It provides the primary API that applications use to update
 * serialized data from older versions forward to newer versions.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.DataFixer} — The main
 *       entry point for applying data fixes. Orchestrates the application of
 *       multiple fixes to migrate data across version boundaries.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.DataFix} — A single
 *       migration step that transforms data from one version to the next. Each
 *       fix declares a source version, target version, and transformation
 *       logic.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.FixRegistrar} — Registry
 *       interface that bootstraps hand fixes to. Each fix is registered against
 *       the {@link de.splatgames.aether.datafixers.api.TypeReference} it
 *       applies to.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.DataFixerContext} —
 *       Per-migration context providing logging hooks and the seam through
 *       which
 *       {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}
 *       plugs in.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix.Fixes} — Factory methods
 *       for creating common fix implementations without subclassing.</li>
 * </ul>
 *
 * <h2>Migration Flow</h2>
 * <p>When {@link de.splatgames.aether.datafixers.api.fix.DataFixer#update(de.splatgames.aether.datafixers.api.TypeReference, de.splatgames.aether.datafixers.api.dynamic.Dynamic, de.splatgames.aether.datafixers.api.DataVersion, de.splatgames.aether.datafixers.api.DataVersion) DataFixer.update}
 * is called, the fixer:</p>
 * <ol>
 *   <li>Collects all registered fixes for the given {@code TypeReference}
 *       whose version range overlaps {@code [from, to]}.</li>
 *   <li>Sorts them by {@link de.splatgames.aether.datafixers.api.fix.DataFix#fromVersion()}
 *       so they form a correct forward chain.</li>
 *   <li>Applies each fix in sequence, piping the output of one into the next.</li>
 *   <li>Returns the fully migrated {@link de.splatgames.aether.datafixers.api.dynamic.Dynamic}
 *       at the target version.</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Obtain a configured DataFixer (see the bootstrap package for construction)
 * DataFixer fixer = ...;
 *
 * // Wrap raw JSON in a Dynamic
 * Dynamic<JsonElement> oldData = new Dynamic<>(GsonOps.INSTANCE, json);
 *
 * // Migrate from version 100 up to the current version
 * Dynamic<JsonElement> updated = fixer.update(
 *     TypeReferences.PLAYER,
 *     oldData,
 *     new DataVersion(100),
 *     fixer.currentVersion());
 * }</pre>
 *
 * <h2>Implementing a {@code DataFix}</h2>
 * <p>For virtually all migrations, extend
 * {@code de.splatgames.aether.datafixers.core.fix.SchemaDataFix} and express
 * the transformation as a
 * {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule} built
 * with {@link de.splatgames.aether.datafixers.api.rewrite.Rules}. This keeps
 * migrations declarative and hooks into the field-aware diagnostic system
 * automatically:</p>
 * <pre>{@code
 * public class PlayerV1ToV2Fix extends SchemaDataFix {
 *     public PlayerV1ToV2Fix(SchemaRegistry schemas) {
 *         super("player_v100_to_v110",
 *               new DataVersion(100), new DataVersion(110), schemas);
 *     }
 *
 *     @Override
 *     protected TypeRewriteRule makeRule(Schema input, Schema output) {
 *         return Rules.renameField(GsonOps.INSTANCE, "playerName", "name");
 *     }
 * }
 * }</pre>
 * <p>Implement {@link de.splatgames.aether.datafixers.api.fix.DataFix}
 * directly only when the migration cannot be expressed as a rewrite rule.</p>
 *
 * @see de.splatgames.aether.datafixers.api.fix.DataFixer
 * @see de.splatgames.aether.datafixers.api.fix.DataFix
 * @see de.splatgames.aether.datafixers.api.fix.FixRegistrar
 * @see de.splatgames.aether.datafixers.api.DataVersion
 * @see de.splatgames.aether.datafixers.api.rewrite.Rules
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.fix;
