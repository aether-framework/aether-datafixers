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
 * A single data fix that transforms data from one version to another.
 *
 * <p>A {@code DataFix} represents a migration step that upgrades (or downgrades) data
 * between two specific {@link DataVersion versions}. Fixes are typically registered with a {@link FixRegistrar} and
 * executed by a {@link DataFixer} when updating data across version boundaries.</p>
 *
 * <h2>Implementing a DataFix</h2>
 * <p>Each fix should:</p>
 * <ul>
 *   <li>Have a descriptive {@link #name()} for logging and debugging</li>
 *   <li>Declare the version range it handles via {@link #fromVersion()} and {@link #toVersion()}</li>
 *   <li>Transform data in the {@link #apply(TypeReference, Dynamic, DataFixerContext)} method</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DataFix<JsonElement> renameFix = new DataFix<>() {
 *     @Override
 *     public String name() { return "rename_player_name"; }
 *
 *     @Override
 *     public DataVersion fromVersion() { return DataVersion.of(1); }
 *
 *     @Override
 *     public DataVersion toVersion() { return DataVersion.of(2); }
 *
 *     @Override
 *     public Dynamic<JsonElement> apply(TypeReference type, Dynamic<JsonElement> input, DataFixerContext ctx) {
 *         ctx.info("Renaming 'playerName' to 'name'");
 *         Dynamic<?> name = input.get("playerName");
 *         return input.remove("playerName").set("name", name);
 *     }
 * };
 *
 * registrar.register(TypeReferences.PLAYER, renameFix);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations should be thread-safe as fixes may be applied concurrently
 * across multiple data instances.</p>
 *
 * @param <T> the type of the dynamic representation (e.g., {@code JsonElement}, {@code JsonNode})
 * @author Erik Pförtner
 * @see DataFixer
 * @see FixRegistrar
 * @see DataVersion
 * @since 0.1.0
 */
public interface DataFix<T> {

    /**
     * Returns the human-readable name of this fix.
     *
     * <p>The name should be descriptive and unique, used for logging,
     * debugging, and error reporting.</p>
     *
     * @return the fix name, never {@code null}
     */
    @NotNull
    String name();

    /**
     * Returns the source version this fix migrates from.
     *
     * <p>The fix will be applied to data at this version to upgrade it
     * to {@link #toVersion()}.</p>
     *
     * @return the source data version, never {@code null}
     */
    @NotNull
    DataVersion fromVersion();

    /**
     * Returns the target version this fix migrates to.
     *
     * <p>After applying this fix, data will be at this version.</p>
     *
     * @return the target data version, never {@code null}
     */
    @NotNull
    DataVersion toVersion();

    /**
     * Applies this fix to the input data.
     *
     * <p>This method performs the actual data transformation. The implementation
     * should handle all data of the specified type that requires migration.</p>
     *
     * @param type    the type reference identifying what kind of data is being fixed, must not be {@code null}
     * @param input   the input data to transform, must not be {@code null}
     * @param context the fixer context for logging and diagnostics, must not be {@code null}
     * @return the transformed data at the new version, never {@code null}
     */
    @NotNull
    Dynamic<T> apply(@NotNull final TypeReference type,
                     @NotNull final Dynamic<T> input,
                     @NotNull final DataFixerContext context);
}
