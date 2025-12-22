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

package de.splatgames.aether.datafixers.api.bootstrap;

import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Bootstrap interface for initializing a {@link DataFixer} with schemas and fixes.
 *
 * <p>{@code DataFixerBootstrap} provides a standardized way to populate
 * a data fixer during initialization. Implementations define the schemas
 * for different data versions and the fixes that migrate between them.</p>
 *
 * <h2>Implementation Pattern</h2>
 * <pre>{@code
 * public class MyDataFixerBootstrap implements DataFixerBootstrap {
 *
 *     @Override
 *     public void registerSchemas(SchemaRegistry schemas) {
 *         // Register schema for version 1
 *         TypeRegistry v1Types = new SimpleTypeRegistry();
 *         v1Types.register(new SimpleType<>(TypeReferences.PLAYER, playerCodecV1));
 *         schemas.register(new Schema(new DataVersion(1), v1Types));
 *
 *         // Register schema for version 2
 *         TypeRegistry v2Types = new SimpleTypeRegistry();
 *         v2Types.register(new SimpleType<>(TypeReferences.PLAYER, playerCodecV2));
 *         schemas.register(new Schema(new DataVersion(2), v2Types));
 *     }
 *
 *     @Override
 *     public void registerFixes(FixRegistrar fixes) {
 *         // Register fix to migrate from v1 to v2
 *         fixes.register(TypeReferences.PLAYER, new RenamePlayerNameFix());
 *     }
 * }
 * }</pre>
 *
 * <h2>Initialization Order</h2>
 * <p>The order of registration calls should be:</p>
 * <ol>
 *   <li>{@link #registerSchemas(SchemaRegistry)} - Define all version schemas</li>
 *   <li>{@link #registerFixes(FixRegistrar)} - Register migration fixes</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DataFixerBootstrap bootstrap = new MyDataFixerBootstrap();
 * DataFixer fixer = DataFixerBuilder.create()
 *     .bootstrap(bootstrap)
 *     .build();
 * }</pre>
 *
 * @author Erik Pförtner
 * @see SchemaRegistry
 * @see FixRegistrar
 * @see DataFixer
 * @since 0.1.0
 */
public interface DataFixerBootstrap {

    /**
     * Registers all schemas with the schema registry.
     *
     * <p>This method is called during data fixer initialization to populate
     * the schema registry with {@link Schema} instances for each supported
     * data version.</p>
     *
     * @param schemas the schema registry to populate, must not be {@code null}
     */
    void registerSchemas(@NotNull final SchemaRegistry schemas);

    /**
     * Registers all data fixes with the fix registrar.
     *
     * <p>This method is called during data fixer initialization to register
     * all {@link DataFix} instances that handle migrations between versions.</p>
     *
     * @param fixes the fix registrar to populate, must not be {@code null}
     */
    void registerFixes(@NotNull final FixRegistrar fixes);
}
