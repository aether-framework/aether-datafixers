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
 * DataFix implementations and supporting infrastructure.
 *
 * <p>This package contains the concrete building blocks that applications
 * extend to create their migration logic, together with the internal
 * machinery that the core module uses to execute fixes.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.SchemaDataFix} —
 *       Abstract base class for fixes that need access to input/output schemas
 *       and express their migration as a
 *       {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule}.
 *       This is the entry point for almost all application migrations.</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.DataFixerImpl} —
 *       Reference implementation of
 *       {@link de.splatgames.aether.datafixers.api.fix.DataFixer} that
 *       orchestrates the fix chain at runtime.</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.DataFixerBuilder} —
 *       Mutable builder that also implements
 *       {@link de.splatgames.aether.datafixers.api.fix.FixRegistrar}; used by
 *       {@code DataFixerRuntimeFactory} to collect registrations during
 *       bootstrap.</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.DataFixRegistry} —
 *       Internal registry that indexes fixes by {@link de.splatgames.aether.datafixers.api.TypeReference}
 *       and provides version-ordered access.</li>
 * </ul>
 *
 * <h2>Context Implementations</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.Slf4jDataFixerContext} —
 *       Writes {@code info}/{@code warn} calls to SLF4J.</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.SimpleSystemDataFixerContext} —
 *       Writes to {@code System.out} / {@code System.err}; useful for scripts
 *       and demos.</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.noop.NoOpDataFixerContext}
 *       — Silent context used when no logging is required.</li>
 * </ul>
 *
 * <h2>Implementing a Fix</h2>
 * <p>Subclass {@code SchemaDataFix} with a
 * {@code (SchemaRegistry schemas)} constructor that calls the
 * {@code (name, from, to, schemas)} super-constructor, and express the
 * migration using {@link de.splatgames.aether.datafixers.api.rewrite.Rules}:</p>
 * <pre>{@code
 * public class PlayerV1ToV2Fix extends SchemaDataFix {
 *
 *     public PlayerV1ToV2Fix(SchemaRegistry schemas) {
 *         super("player_v100_to_v110",
 *               new DataVersion(100), new DataVersion(110), schemas);
 *     }
 *
 *     @Override
 *     protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
 *         DynamicOps<JsonElement> ops = GsonOps.INSTANCE;
 *         return Rules.seq(
 *             Rules.renameFields(ops, Map.of(
 *                 "playerName", "name",
 *                 "xp",         "experience")),
 *             Rules.groupFields(ops, "position", "x", "y", "z"),
 *             Rules.transformField(ops, "gameMode", this::gameModeToString));
 *     }
 *
 *     private Dynamic<JsonElement> gameModeToString(Dynamic<JsonElement> value) {
 *         int mode = value.asInt().result().orElse(0);
 *         return value.createString(switch (mode) {
 *             case 0 -> "survival";
 *             case 1 -> "creative";
 *             case 2 -> "adventure";
 *             default -> "unknown";
 *         });
 *     }
 * }
 * }</pre>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.noop} — No-op
 *       implementations for testing and for the default silent context used
 *       when {@link de.splatgames.aether.datafixers.api.fix.DataFixer#update(de.splatgames.aether.datafixers.api.TypeReference, de.splatgames.aether.datafixers.api.dynamic.Dynamic, de.splatgames.aether.datafixers.api.DataVersion, de.splatgames.aether.datafixers.api.DataVersion) DataFixer.update}
 *       is called without a context.</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.core.fix.SchemaDataFix
 * @see de.splatgames.aether.datafixers.core.fix.DataFixerImpl
 * @see de.splatgames.aether.datafixers.api.fix.DataFix
 * @see de.splatgames.aether.datafixers.api.rewrite.Rules
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.core.fix;
