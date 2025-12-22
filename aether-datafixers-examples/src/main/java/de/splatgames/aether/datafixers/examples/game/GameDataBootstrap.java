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

package de.splatgames.aether.datafixers.examples.game;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Bootstrap class that initializes the data fixer with schemas and fixes.
 *
 * <p>This implements the DFU pattern of using a bootstrap to register all
 * schemas (one per version) and all fixes (migrations between versions).</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * AetherDataFixer fixer = new DataFixerRuntimeFactory()
 *     .create(GameDataBootstrap.CURRENT_VERSION, new GameDataBootstrap());
 * }</pre>
 *
 * <h2>Version History (SemVer-based IDs)</h2>
 * <ul>
 *   <li>100 = Version 1.0.0 - Initial flat structure</li>
 *   <li>110 = Version 1.1.0 - Restructured with nested position</li>
 *   <li>200 = Version 2.0.0 - Extended with health and level</li>
 * </ul>
 *
 * @see DataFixerBootstrap
 * @see Schema100
 * @see Schema110
 * @see Schema200
 */
public final class GameDataBootstrap implements DataFixerBootstrap {

    /**
     * The current (latest) data version (2.0.0).
     */
    public static final DataVersion CURRENT_VERSION = new DataVersion(200);

    /**
     * Stores the schema registry reference between registerSchemas and registerFixes.
     */
    private SchemaRegistry schemas;

    /**
     * Registers all schemas with the schema registry.
     *
     * @param schemas the schema registry to populate
     */
    @Override
    public void registerSchemas(@NotNull final SchemaRegistry schemas) {
        this.schemas = schemas;

        // Register schemas in version order
        schemas.register(new Schema100());
        schemas.register(new Schema110());
        schemas.register(new Schema200());
    }

    /**
     * Registers all data fixes with the fix registrar.
     *
     * @param fixes the fix registrar to populate
     */
    @Override
    public void registerFixes(@NotNull final FixRegistrar fixes) {
        // Register fixes for player data
        fixes.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(this.schemas));
        fixes.register(TypeReferences.PLAYER, new PlayerV2ToV3Fix(this.schemas));
    }
}
