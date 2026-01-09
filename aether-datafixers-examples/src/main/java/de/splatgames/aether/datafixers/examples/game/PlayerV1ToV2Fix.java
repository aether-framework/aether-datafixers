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
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.optic.Finder;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * Migrates player data from Version 1.0.0 to Version 1.1.0.
 *
 * <p>This fix demonstrates the use of {@link Finder} optics for navigating
 * and transforming dynamic data structures.</p>
 *
 * <h2>Changes</h2>
 * <ol>
 *   <li>Rename {@code playerName} to {@code name}</li>
 *   <li>Rename {@code xp} to {@code experience}</li>
 *   <li>Convert {@code gameMode} from int to string</li>
 *   <li>Group x/y/z into nested {@code position} object</li>
 * </ol>
 *
 * <h2>Optics Usage</h2>
 * <p>Uses {@link Finder} to navigate into the dynamic structure:</p>
 * <pre>{@code
 * // Navigate to specific fields
 * Finder<?> xFinder = Finder.field("x");
 * Finder<?> yFinder = Finder.field("y");
 * Finder<?> zFinder = Finder.field("z");
 *
 * // Extract values using optics
 * double x = xFinder.get(data).asDouble().result().orElse(0.0);
 * double y = yFinder.get(data).asDouble().result().orElse(0.0);
 * double z = zFinder.get(data).asDouble().result().orElse(0.0);
 * }</pre>
 *
 * <h2>Example</h2>
 * <pre>
 * Before (V1.0.0):
 * {
 *   "playerName": "Steve",
 *   "xp": 2500,
 *   "x": 100.5, "y": 64.0, "z": -200.25,
 *   "gameMode": 0
 * }
 *
 * After (V1.1.0):
 * {
 *   "name": "Steve",
 *   "experience": 2500,
 *   "position": { "x": 100.5, "y": 64.0, "z": -200.25 },
 *   "gameMode": "survival"
 * }
 * </pre>
 *
 * @see Schema100
 * @see Schema110
 * @see Finder
 */
public final class PlayerV1ToV2Fix extends SchemaDataFix {

    // =========================================================================
    // OPTIC FINDERS - Navigate to specific fields
    // =========================================================================

    /** Finder for the x coordinate field */
    private static final Finder<?> X_FINDER = Finder.field("x");

    /** Finder for the y coordinate field */
    private static final Finder<?> Y_FINDER = Finder.field("y");

    /** Finder for the z coordinate field */
    private static final Finder<?> Z_FINDER = Finder.field("z");

    /** Finder for the gameMode field */
    private static final Finder<?> GAME_MODE_FINDER = Finder.field("gameMode");

    /**
     * Creates the V1.0.0 to V1.1.0 fix.
     *
     * @param schemas the schema registry containing all version schemas
     */
    public PlayerV1ToV2Fix(@NotNull final SchemaRegistry schemas) {
        super(
                "player_v100_to_v110",
                new DataVersion(100),
                new DataVersion(110),
                schemas
        );
    }

    @Override
    @NotNull
    protected TypeRewriteRule makeRule(@NotNull final Schema inputSchema,
                                       @NotNull final Schema outputSchema) {
        return Rules.seq(
                // Step 1: Simple field renames using Rules
                Rules.renameField(GsonOps.INSTANCE, "playerName", "name"),
                Rules.renameField(GsonOps.INSTANCE, "xp", "experience"),

                // Step 2: Transform gameMode from int to string
                Rules.transformField(GsonOps.INSTANCE, "gameMode", PlayerV1ToV2Fix::gameModeIntToString),

                // Step 3: Group x/y/z into nested position object using Finder optics
                groupPositionFieldsWithOptics()
        );
    }

    // =========================================================================
    // TRANSFORMATION HELPERS
    // =========================================================================

    /**
     * Converts gameMode from integer to string representation.
     *
     * @param dynamic the dynamic value containing the integer game mode
     * @return a dynamic containing the string game mode
     */
    @NotNull
    private static Dynamic<?> gameModeIntToString(@NotNull final Dynamic<?> dynamic) {
        final int mode = dynamic.asInt().result().orElse(0);
        final String modeString = switch (mode) {
            case 1 -> "creative";
            case 2 -> "adventure";
            case 3 -> "spectator";
            default -> "survival";
        };
        return dynamic.createString(modeString);
    }

    /**
     * Creates a rule that groups x/y/z fields into a nested position object
     * using Finder optics for field access.
     *
     * <p>Demonstrates optic-based field extraction:</p>
     * <pre>{@code
     * // Use Finder optics to extract coordinate values
     * double x = X_FINDER.getOptional(data)
     *     .flatMap(d -> d.asDouble().result())
     *     .orElse(0.0);
     * }</pre>
     *
     * @return a rule that restructures position fields
     */
    @NotNull
    private static TypeRewriteRule groupPositionFieldsWithOptics() {
        return dynamicTransform("groupPosition", dynamic -> {
            // Use Finder optics to extract coordinate values
            final double x = extractDouble(X_FINDER, dynamic);
            final double y = extractDouble(Y_FINDER, dynamic);
            final double z = extractDouble(Z_FINDER, dynamic);

            @SuppressWarnings("unchecked")
            final Dynamic<Object> objDynamic = (Dynamic<Object>) dynamic;

            // Create nested position object
            final Dynamic<Object> position = objDynamic.emptyMap()
                    .set("x", objDynamic.createDouble(x))
                    .set("y", objDynamic.createDouble(y))
                    .set("z", objDynamic.createDouble(z));

            // Remove old flat fields and add nested position
            return objDynamic
                    .remove("x")
                    .remove("y")
                    .remove("z")
                    .set("position", position);
        });
    }

    /**
     * Extracts a double value using a Finder optic.
     *
     * @param finder  the finder to use for field access
     * @param dynamic the dynamic data to extract from
     * @return the double value, or 0.0 if not found
     */
    private static double extractDouble(@NotNull final Finder<?> finder,
                                         @NotNull final Dynamic<?> dynamic) {
        return finder.getOptional(dynamic)
                .flatMap(d -> d.asDouble().result())
                .orElse(0.0);
    }

    /**
     * Creates a TypeRewriteRule from a Dynamic transformation function.
     *
     * @param name      the rule name for debugging
     * @param transform the transformation function
     * @return a TypeRewriteRule that applies the transformation
     */
    @NotNull
    private static TypeRewriteRule dynamicTransform(
            @NotNull final String name,
            @NotNull final Function<Dynamic<?>, Dynamic<?>> transform
    ) {
        return new TypeRewriteRule() {
            @Override
            @NotNull
            @SuppressWarnings({"unchecked", "rawtypes"})
            public Optional<Typed<?>> rewrite(@NotNull final Type<?> type,
                                              @NotNull final Typed<?> input) {
                return input.encode(GsonOps.INSTANCE).flatMap(dynamic -> {
                    final Dynamic<?> transformed = transform.apply(dynamic);
                    final Type rawType = input.type();
                    return rawType.read(transformed);
                }).map(value -> new Typed<>((Type) input.type(), value)).result();
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }
}
