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

import com.google.gson.JsonPrimitive;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.optic.Finder;
import de.splatgames.aether.datafixers.api.rewrite.Rules;
import de.splatgames.aether.datafixers.api.rewrite.TypeRewriteRule;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.Typed;
import de.splatgames.aether.datafixers.codec.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.SchemaDataFix;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * Migrates player data from Version 1.1.0 to Version 2.0.0.
 *
 * <p>This fix demonstrates the use of {@link Finder} optics for reading
 * fields and computing derived values.</p>
 *
 * <h2>Changes</h2>
 * <ol>
 *   <li>Add {@code health} field with default 20.0</li>
 *   <li>Add {@code maxHealth} field with default 20.0</li>
 *   <li>Compute {@code level} from experience (level = sqrt(xp/100))</li>
 * </ol>
 *
 * <h2>Optics Usage</h2>
 * <p>Uses {@link Finder} to read the experience field:</p>
 * <pre>{@code
 * // Create finder for experience field
 * Finder<?> experienceFinder = Finder.field("experience");
 *
 * // Extract experience using optic
 * int experience = experienceFinder.getOptional(data)
 *     .flatMap(d -> d.asInt().result())
 *     .orElse(0);
 *
 * // Compute level from experience
 * int level = Math.max(1, (int) Math.sqrt(experience / 100.0));
 * }</pre>
 *
 * <h2>Example</h2>
 * <pre>
 * Before (V1.1.0):
 * {
 *   "name": "Steve",
 *   "experience": 2500,
 *   "position": { "x": 100.5, "y": 64.0, "z": -200.25 },
 *   "gameMode": "survival"
 * }
 *
 * After (V2.0.0):
 * {
 *   "name": "Steve",
 *   "experience": 2500,
 *   "level": 5,
 *   "health": 20.0,
 *   "maxHealth": 20.0,
 *   "position": { "x": 100.5, "y": 64.0, "z": -200.25 },
 *   "gameMode": "survival"
 * }
 * </pre>
 *
 * @see Schema110
 * @see Schema200
 * @see Finder
 */
public final class PlayerV2ToV3Fix extends SchemaDataFix {

    // =========================================================================
    // OPTIC FINDERS - Navigate to specific fields
    // =========================================================================

    /** Finder for the experience field */
    private static final Finder<?> EXPERIENCE_FINDER = Finder.field("experience");

    /** Finder for nested access: position -> x */
    private static final Finder<?> POSITION_X_FINDER = Finder.field("position").then(Finder.field("x"));

    /** Finder for nested access: position -> y */
    private static final Finder<?> POSITION_Y_FINDER = Finder.field("position").then(Finder.field("y"));

    /** Finder for nested access: position -> z */
    private static final Finder<?> POSITION_Z_FINDER = Finder.field("position").then(Finder.field("z"));

    /**
     * Creates the V1.1.0 to V2.0.0 fix.
     *
     * @param schemas the schema registry containing all version schemas
     */
    public PlayerV2ToV3Fix(@NotNull final SchemaRegistry schemas) {
        super(
                "player_v110_to_v200",
                new DataVersion(110),
                new DataVersion(200),
                schemas
        );
    }

    @Override
    @NotNull
    protected TypeRewriteRule makeRule(@NotNull final Schema inputSchema,
                                       @NotNull final Schema outputSchema) {
        return Rules.seqAll(
                // Add health fields with default values using Rules
                Rules.addField(
                        GsonOps.INSTANCE,
                        "health",
                        new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(20.0f))
                ),
                Rules.addField(
                        GsonOps.INSTANCE,
                        "maxHealth",
                        new Dynamic<>(GsonOps.INSTANCE, new JsonPrimitive(20.0f))
                ),

                // Compute level from experience using Finder optics
                computeLevelFromExperienceWithOptics()
        );
    }

    // =========================================================================
    // TRANSFORMATION HELPERS
    // =========================================================================

    /**
     * Creates a rule that computes the player's level from experience
     * using Finder optics for field access.
     *
     * <p>Level calculation: level = max(1, sqrt(experience / 100))</p>
     *
     * <p>Demonstrates optic-based field reading:</p>
     * <pre>{@code
     * // Use Finder optic to extract experience
     * int experience = EXPERIENCE_FINDER.getOptional(data)
     *     .flatMap(d -> d.asInt().result())
     *     .orElse(0);
     * }</pre>
     *
     * @return a rule that adds the computed level field
     */
    @NotNull
    private static TypeRewriteRule computeLevelFromExperienceWithOptics() {
        return dynamicTransform("computeLevel", dynamic -> {
            // Use Finder optic to extract experience value
            final int experience = EXPERIENCE_FINDER.getOptional(dynamic)
                    .flatMap(d -> d.asInt().result())
                    .orElse(0);

            // Compute level using square root formula
            final int level = Math.max(1, (int) Math.sqrt(experience / 100.0));

            // Add level field to dynamic
            @SuppressWarnings("unchecked")
            final Dynamic<Object> objDynamic = (Dynamic<Object>) dynamic;
            return objDynamic.set("level", objDynamic.createInt(level));
        });
    }

    /**
     * Demonstrates reading nested values with composed Finders.
     *
     * <p>This method shows how to use composed finders for nested access:</p>
     * <pre>{@code
     * // Composed finder: position -> x
     * Finder<?> posXFinder = Finder.field("position").then(Finder.field("x"));
     *
     * // Extract nested value
     * double x = posXFinder.getOptional(data)
     *     .flatMap(d -> d.asDouble().result())
     *     .orElse(0.0);
     * }</pre>
     *
     * @param dynamic the dynamic data to read from
     * @return a string describing the position
     */
    @SuppressWarnings("unused")
    private static String describePosition(@NotNull final Dynamic<?> dynamic) {
        final double x = POSITION_X_FINDER.getOptional(dynamic)
                .flatMap(d -> d.asDouble().result())
                .orElse(0.0);
        final double y = POSITION_Y_FINDER.getOptional(dynamic)
                .flatMap(d -> d.asDouble().result())
                .orElse(0.0);
        final double z = POSITION_Z_FINDER.getOptional(dynamic)
                .flatMap(d -> d.asDouble().result())
                .orElse(0.0);

        return String.format("Position(x=%.2f, y=%.2f, z=%.2f)", x, y, z);
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
