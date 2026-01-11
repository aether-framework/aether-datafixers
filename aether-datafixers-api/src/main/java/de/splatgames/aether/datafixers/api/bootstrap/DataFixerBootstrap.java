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
 * a data fixer during initialization. Implementations define the schemas for different data versions and the fixes that
 * migrate between them.</p>
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
     * Registers all schemas with the provided schema registry.
     *
     * <p>This method is invoked during data fixer initialization to populate the schema
     * registry with {@link Schema} instances for each supported data version. Schemas define the structure and types
     * available at each version of the data model.</p>
     *
     * <h3>Implementation Guidelines</h3>
     * <p>When implementing this method, consider the following best practices:</p>
     * <ul>
     *   <li><b>Version Ordering:</b> Register schemas in ascending version order for clarity,
     *       though the registry does not enforce any particular order</li>
     *   <li><b>Complete Coverage:</b> Ensure all versions that have associated fixes are covered
     *       by corresponding schema definitions</li>
     *   <li><b>Type Consistency:</b> Types referenced by fixes must be defined in the appropriate
     *       schema versions</li>
     *   <li><b>Parent Schemas:</b> Consider using parent schema references to share type
     *       definitions between versions when types remain unchanged</li>
     * </ul>
     *
     * <h3>Example Implementation</h3>
     * <pre>{@code
     * @Override
     * public void registerSchemas(SchemaRegistry schemas) {
     *     // Base schema for version 1
     *     TypeRegistry v1Registry = new SimpleTypeRegistry();
     *     v1Registry.register(new SimpleType<>(TypeReferences.PLAYER, playerCodecV1));
     *     v1Registry.register(new SimpleType<>(TypeReferences.WORLD, worldCodecV1));
     *     schemas.register(new Schema(new DataVersion(100), v1Registry));
     *
     *     // Extended schema for version 2 with additional fields
     *     TypeRegistry v2Registry = new SimpleTypeRegistry();
     *     v2Registry.register(new SimpleType<>(TypeReferences.PLAYER, playerCodecV2));
     *     v2Registry.register(new SimpleType<>(TypeReferences.WORLD, worldCodecV1)); // unchanged
     *     schemas.register(new Schema(new DataVersion(200), v2Registry));
     * }
     * }</pre>
     *
     * <h3>Thread Safety</h3>
     * <p>This method is typically called once during initialization on a single thread.
     * Implementations do not need to be thread-safe, but they should not retain references
     * to the registry after the method returns.</p>
     *
     * @param schemas the schema registry to populate with version-specific schemas; must not be {@code null}
     * @throws NullPointerException if {@code schemas} is {@code null}
     * @see Schema
     * @see SchemaRegistry
     */
    void registerSchemas(@NotNull final SchemaRegistry schemas);

    /**
     * Registers all data fixes with the provided fix registrar.
     *
     * <p>This method is invoked during data fixer initialization to register all
     * {@link DataFix} instances that handle migrations between data versions. Each fix defines a transformation from
     * one version to another for a specific type or set of types.</p>
     *
     * <h3>Implementation Guidelines</h3>
     * <p>When implementing this method, adhere to these best practices:</p>
     * <ul>
     *   <li><b>Version Coverage:</b> Ensure there are fixes to migrate between all
     *       consecutive schema versions</li>
     *   <li><b>Type Association:</b> Register fixes with the appropriate type references
     *       they operate on</li>
     *   <li><b>Fix Ordering:</b> While the registrar handles ordering internally, registering
     *       fixes in version order improves code readability</li>
     *   <li><b>Idempotency:</b> Fixes should be designed to work correctly regardless of
     *       the order in which they are registered</li>
     * </ul>
     *
     * <h3>Example Implementation</h3>
     * <pre>{@code
     * @Override
     * public void registerFixes(FixRegistrar fixes) {
     *     // Register fix for migrating player data from v1 to v2
     *     fixes.register(TypeReferences.PLAYER, new PlayerRenameFieldsFix(
     *         new DataVersion(100), new DataVersion(200)));
     *
     *     // Register fix for adding new fields with defaults
     *     fixes.register(TypeReferences.PLAYER, new PlayerAddNewFieldsFix(
     *         new DataVersion(200), new DataVersion(300)));
     *
     *     // Register fix that affects multiple types
     *     fixes.register(TypeReferences.WORLD, new WorldUpdateFormatFix(
     *         new DataVersion(100), new DataVersion(200)));
     * }
     * }</pre>
     *
     * <h3>Thread Safety</h3>
     * <p>This method is typically called once during initialization on a single thread.
     * Implementations do not need to be thread-safe, but they should not retain references
     * to the registrar after the method returns.</p>
     *
     * @param fixes the fix registrar to populate with data migration fixes; must not be {@code null}
     * @throws NullPointerException if {@code fixes} is {@code null}
     * @see DataFix
     * @see FixRegistrar
     */
    void registerFixes(@NotNull final FixRegistrar fixes);
}
