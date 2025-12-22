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

package de.splatgames.aether.datafixers.api.fix;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import org.jetbrains.annotations.NotNull;

/**
 * The main entry point for applying data fixes across version boundaries.
 *
 * <p>A {@code DataFixer} orchestrates the application of {@link DataFix} instances
 * to migrate data from one {@link DataVersion} to another. It maintains a registry
 * of fixes and automatically determines which fixes to apply based on the source
 * and target versions.</p>
 *
 * <h2>Version Migration</h2>
 * <p>When updating data, the fixer:</p>
 * <ol>
 *   <li>Identifies all fixes between the source and target versions</li>
 *   <li>Orders fixes by version to ensure correct application order</li>
 *   <li>Applies each fix in sequence, passing the output of one to the next</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DataFixer fixer = DataFixerBootstrap.create();
 *
 * // Update player data from version 1 to current version
 * Dynamic<JsonElement> oldData = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
 * Dynamic<JsonElement> updatedData = fixer.update(
 *     TypeReferences.PLAYER,
 *     oldData,
 *     DataVersion.of(1),
 *     fixer.currentVersion()
 * );
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be thread-safe, allowing concurrent updates
 * to different data instances.</p>
 *
 * @author Erik Pförtner
 * @see DataFix
 * @see DataVersion
 * @see TypeReference
 * @since 0.1.0
 */
public interface DataFixer {

    /**
     * Returns the current (latest) data version supported by this fixer.
     *
     * <p>This represents the most recent schema version that data can be
     * upgraded to. New data should be created at this version.</p>
     *
     * @return the current data version, never {@code null}
     */
    @NotNull
    DataVersion currentVersion();

    /**
     * Updates data from one version to another using a default context.
     *
     * <p>This is a convenience method that creates a no-op context for
     * cases where logging is not required.</p>
     *
     * @param type  the type reference identifying what kind of data is being updated,
     *              must not be {@code null}
     * @param input the input data to update, must not be {@code null}
     * @param from  the source version of the input data, must not be {@code null}
     * @param to    the target version to update to, must not be {@code null}
     * @param <T>   the type of the dynamic representation
     * @return the updated data at the target version, never {@code null}
     */
    @NotNull <T> Dynamic<T> update(@NotNull final TypeReference type,
                          @NotNull final Dynamic<T> input,
                          @NotNull final DataVersion from,
                          @NotNull final DataVersion to);

    /**
     * Updates data from one version to another with a custom context.
     *
     * <p>Applies all registered fixes between the source and target versions
     * in order. The context receives logging callbacks during the update process.</p>
     *
     * @param type    the type reference identifying what kind of data is being updated,
     *                must not be {@code null}
     * @param input   the input data to update, must not be {@code null}
     * @param from    the source version of the input data, must not be {@code null}
     * @param to      the target version to update to, must not be {@code null}
     * @param context the fixer context for logging and diagnostics, must not be {@code null}
     * @param <T>     the type of the dynamic representation
     * @return the updated data at the target version, never {@code null}
     */
    @NotNull <T> Dynamic<T> update(@NotNull final TypeReference type,
                          @NotNull final Dynamic<T> input,
                          @NotNull final DataVersion from,
                          @NotNull final DataVersion to,
                          @NotNull final DataFixerContext context);
}
