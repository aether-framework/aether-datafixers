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

package de.splatgames.aether.datafixers.core.bootstrap;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.core.AetherDataFixer;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating fully configured {@link AetherDataFixer} instances.
 *
 * <p>{@code DataFixerRuntimeFactory} simplifies the creation of data fixers by
 * orchestrating the bootstrap process. It creates the necessary registries,
 * invokes the bootstrap to populate them, and assembles the final fixer.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DataFixerRuntimeFactory factory = new DataFixerRuntimeFactory();
 * AetherDataFixer fixer = factory.create(
 *     new DataVersion(5),
 *     new MyDataFixerBootstrap()
 * );
 *
 * // Now use the fixer
 * TaggedDynamic updated = fixer.update(oldData, oldVersion, fixer.currentVersion());
 * }</pre>
 *
 * <h2>Initialization Steps</h2>
 * <p>The {@link #create} method performs:</p>
 * <ol>
 *   <li>Creates a {@link SimpleSchemaRegistry}</li>
 *   <li>Calls {@link DataFixerBootstrap#registerSchemas} to populate schemas</li>
 *   <li>Creates a {@link DataFixerBuilder}</li>
 *   <li>Calls {@link DataFixerBootstrap#registerFixes} to populate fixes</li>
 *   <li>Freezes all registries for thread-safe access</li>
 *   <li>Assembles and returns the {@link AetherDataFixer}</li>
 * </ol>
 *
 * @author Erik Pförtner
 * @see AetherDataFixer
 * @see DataFixerBootstrap
 * @since 0.1.0
 */
public final class DataFixerRuntimeFactory {

    /**
     * Creates a fully configured data fixer from a bootstrap.
     *
     * @param currentVersion the current (latest) data version, must not be {@code null}
     * @param bootstrap      the bootstrap providing schemas and fixes, must not be {@code null}
     * @return a configured data fixer, never {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    @NotNull
    public AetherDataFixer create(
            @NotNull final DataVersion currentVersion,
            @NotNull final DataFixerBootstrap bootstrap
    ) {
        Preconditions.checkNotNull(currentVersion, "DataVersion currentVersion must not be null");
        Preconditions.checkNotNull(bootstrap, "DataFixerBootstrap bootstrap must not be null");

        final SimpleSchemaRegistry schemas = new SimpleSchemaRegistry();
        bootstrap.registerSchemas(schemas);
        schemas.freeze();

        final DataFixerBuilder builder = new DataFixerBuilder(currentVersion);
        bootstrap.registerFixes(builder);

        return new AetherDataFixer(currentVersion, schemas, builder.build());
    }
}
