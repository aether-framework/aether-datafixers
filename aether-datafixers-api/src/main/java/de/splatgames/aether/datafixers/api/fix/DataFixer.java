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
 * <p>A {@code DataFixer} orchestrates the application of {@link DataFix}
 * instances to migrate data from one {@link DataVersion} to another. It holds
 * the registry of fixes assembled at bootstrap time and automatically
 * determines which fixes to apply based on the source and target versions.</p>
 *
 * <h2>Version Migration</h2>
 * <p>When updating data, the fixer:</p>
 * <ol>
 *   <li>Selects all registered fixes that sit between the source and target
 *       versions for the given {@link TypeReference}.</li>
 *   <li>Orders them by their {@link DataFix#fromVersion() fromVersion} so they
 *       form a correct forward chain.</li>
 *   <li>Applies each fix in sequence, feeding the output of one into the next.</li>
 * </ol>
 *
 * <h2>Obtaining a {@code DataFixer}</h2>
 * <p>Applications build a fixer by writing a
 * {@link de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap} and
 * handing it to
 * {@code de.splatgames.aether.datafixers.core.bootstrap.DataFixerRuntimeFactory}:</p>
 * <pre>{@code
 * DataFixer fixer = new DataFixerRuntimeFactory()
 *     .create(new DataVersion(200), new MyGameBootstrap());
 * }</pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Update player data from its stored version up to the current one
 * Dynamic<JsonElement> oldData = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
 * Dynamic<JsonElement> updated = fixer.update(
 *     TypeReferences.PLAYER,
 *     oldData,
 *     new DataVersion(1),
 *     fixer.currentVersion());
 *
 * // Or with a diagnostic context to collect a detailed MigrationReport
 * DiagnosticContext diag = DiagnosticContext.create();
 * Dynamic<JsonElement> withReport = fixer.update(
 *     TypeReferences.PLAYER, oldData,
 *     new DataVersion(1), fixer.currentVersion(), diag);
 * MigrationReport report = diag.getReport();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be thread-safe and allow concurrent updates against
 * different data instances. {@link DataFixerContext} parameters, by contrast,
 * are per-migration and should not be shared between concurrent calls.</p>
 *
 * @author Erik Pförtner
 * @see DataFix
 * @see DataVersion
 * @see TypeReference
 * @see de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap
 * @see de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext
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
     * <p>Convenience overload equivalent to {@link #update(TypeReference,
     * Dynamic, DataVersion, DataVersion, DataFixerContext)} with a no-op
     * context. If {@code from} is already greater than or equal to {@code to},
     * no fixes are applied and {@code input} is returned unchanged.</p>
     *
     * @param type  the type reference identifying what kind of data is being updated, must not be {@code null}
     * @param input the input data to update, must not be {@code null}
     * @param from  the source version of the input data, must not be {@code null}
     * @param to    the target version to update to, must not be {@code null}
     * @param <T>   the backing type of the {@link Dynamic}
     * @return the updated data at the target version, never {@code null}
     */
    @NotNull <T> Dynamic<T> update(@NotNull final TypeReference type,
                                   @NotNull final Dynamic<T> input,
                                   @NotNull final DataVersion from,
                                   @NotNull final DataVersion to);

    /**
     * Updates data from one version to another with a custom context.
     *
     * <p>Applies every registered fix whose {@code [fromVersion, toVersion]}
     * overlaps the requested range, in ascending order. The provided
     * {@link DataFixerContext} receives logging callbacks for each fix. Passing
     * a {@link de.splatgames.aether.datafixers.api.diagnostic.DiagnosticContext}
     * here enables the migration-report capture described in the package-level
     * documentation of
     * {@link de.splatgames.aether.datafixers.api.diagnostic}.</p>
     *
     * @param type    the type reference identifying what kind of data is being updated, must not be {@code null}
     * @param input   the input data to update, must not be {@code null}
     * @param from    the source version of the input data, must not be {@code null}
     * @param to      the target version to update to, must not be {@code null}
     * @param context the fixer context for logging and optional diagnostics, must not be {@code null}
     * @param <T>     the backing type of the {@link Dynamic}
     * @return the updated data at the target version, never {@code null}
     */
    @NotNull <T> Dynamic<T> update(@NotNull final TypeReference type,
                                   @NotNull final Dynamic<T> input,
                                   @NotNull final DataVersion from,
                                   @NotNull final DataVersion to,
                                   @NotNull final DataFixerContext context);
}
