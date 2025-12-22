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
 * <p>This package contains the core implementations for defining and executing
 * data fixes. It provides the base classes that applications extend to create
 * their migration logic, as well as the internal machinery for fix execution.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.SchemaDataFix} - Abstract
 *       base class for fixes that need access to input/output schema information.
 *       Most application fixes should extend this class.</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.DataFixerImpl} - The
 *       internal implementation of {@link de.splatgames.aether.datafixers.api.fix.DataFixer}
 *       that orchestrates fix execution.</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.DataFixerBuilder} - Builder
 *       for constructing DataFixer instances with custom configuration.</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.DataFixRegistry} - Internal
 *       registry that manages registered fixes and provides version-ordered access.</li>
 * </ul>
 *
 * <h2>Context Implementations</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.Slf4jDataFixerContext} -
 *       Context that logs fix execution using SLF4J.</li>
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.SimpleSystemDataFixerContext} -
 *       Context that logs to System.out/err for simple use cases.</li>
 * </ul>
 *
 * <h2>Implementing a Fix</h2>
 * <pre>{@code
 * public class PlayerV1ToV2Fix extends SchemaDataFix {
 *
 *     public PlayerV1ToV2Fix() {
 *         super(
 *             new DataVersion(100),      // from version
 *             new DataVersion(110),      // to version
 *             TypeReferences.PLAYER      // affected type
 *         );
 *     }
 *
 *     @Override
 *     protected TypeRewriteRule makeRule(Schema inputSchema, Schema outputSchema) {
 *         return Rules.sequence(
 *             Rules.renameField("playerName", "name"),
 *             Rules.renameField("xp", "experience"),
 *             Rules.transformField("gameMode", this::gameModeToString)
 *         );
 *     }
 *
 *     private Dynamic<?> gameModeToString(Dynamic<?> value) {
 *         int mode = value.asInt(0);
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
 *   <li>{@link de.splatgames.aether.datafixers.core.fix.noop} - No-op implementations
 *       for testing and default contexts</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.core.fix.SchemaDataFix
 * @see de.splatgames.aether.datafixers.core.fix.DataFixerImpl
 * @see de.splatgames.aether.datafixers.api.fix.DataFix
 * @see de.splatgames.aether.datafixers.api.rewrite.Rules
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.core.fix;
