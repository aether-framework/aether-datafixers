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
 * A single data fix that transforms data from one version to the next.
 *
 * <p>A {@code DataFix} represents one forward migration step that upgrades data
 * between two {@link DataVersion versions}. Fixes are registered with a
 * {@link FixRegistrar} against a {@link TypeReference} and executed by a
 * {@link DataFixer} when updating data across version boundaries. Aether
 * Datafixers is forward-patching only; fixes always migrate old data to new,
 * never the other way around.</p>
 *
 * <h2>Preferred: Extend {@code SchemaDataFix}</h2>
 * <p>In almost all cases you should extend
 * {@code de.splatgames.aether.datafixers.core.fix.SchemaDataFix} and express
 * the migration as a {@link de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule}
 * built with the {@link de.splatgames.aether.datafixers.api.rewrite.Rules}
 * DSL. The rule-based approach plugs into the field-aware diagnostic system
 * automatically and keeps migrations declarative:</p>
 * <pre>{@code
 * public class PlayerV1ToV2Fix extends SchemaDataFix {
 *     public PlayerV1ToV2Fix(SchemaRegistry schemas) {
 *         super("player_v1_to_v2", new DataVersion(1), new DataVersion(2), schemas);
 *     }
 *
 *     @Override
 *     protected TypeRewriteRule makeRule(Schema input, Schema output) {
 *         DynamicOps<JsonElement> ops = GsonOps.INSTANCE;
 *         return Rules.renameField(ops, "playerName", "name");
 *     }
 * }
 *
 * registrar.register(TypeReferences.PLAYER, new PlayerV1ToV2Fix(schemas));
 * }</pre>
 *
 * <h2>Implementing {@code DataFix} Directly</h2>
 * <p>Direct implementations of {@code DataFix} are an escape hatch for cases
 * that cannot be expressed as a rewrite rule. Every fix must:</p>
 * <ul>
 *   <li>Return a descriptive {@link #name()} used for logging and diagnostics.</li>
 *   <li>Declare the version range via {@link #fromVersion()} and
 *       {@link #toVersion()}.</li>
 *   <li>Transform data in {@link #apply(TypeReference, Dynamic, DataFixerContext)}
 *       and return the result. The input is guaranteed non-{@code null}.</li>
 * </ul>
 *
 * <pre>{@code
 * DataFix<JsonElement> renameFix = new DataFix<>() {
 *     @Override public String name()         { return "rename_player_name"; }
 *     @Override public DataVersion fromVersion() { return new DataVersion(1); }
 *     @Override public DataVersion toVersion()   { return new DataVersion(2); }
 *
 *     @Override
 *     public Dynamic<JsonElement> apply(TypeReference type,
 *                                       Dynamic<JsonElement> input,
 *                                       DataFixerContext ctx) {
 *         ctx.info("Renaming 'playerName' to 'name'");
 *         Dynamic<JsonElement> name = input.get("playerName")
 *                 .result().orElse(input.createString(""));
 *         return input.remove("playerName").set("name", name);
 *     }
 * };
 *
 * registrar.register(TypeReferences.PLAYER, renameFix);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Implementations must be stateless or otherwise thread-safe: a fix may be
 * applied concurrently to many data instances during a single migration.</p>
 *
 * @param <T> the backing type of the {@link Dynamic} the fix operates on
 *            (e.g. {@code JsonElement} for Gson, {@code JsonNode} for Jackson)
 * @author Erik Pförtner
 * @see DataFixer
 * @see FixRegistrar
 * @see DataVersion
 * @see de.splatgames.aether.datafixers.api.rewrite.Rules
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
