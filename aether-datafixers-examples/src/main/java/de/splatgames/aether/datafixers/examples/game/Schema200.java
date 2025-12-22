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

import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.type.SimpleType;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.TypeRegistry;
import de.splatgames.aether.datafixers.api.type.template.TypeTemplate;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Schema for Version 2.0.0 (200) - Extended with health and level system.
 *
 * <p>This version adds RPG-style mechanics to V1.1.0:</p>
 * <ul>
 *   <li>{@code health} - Current health points (float, default 20.0)</li>
 *   <li>{@code maxHealth} - Maximum health points (float, default 20.0)</li>
 *   <li>{@code level} - Player level computed from experience</li>
 * </ul>
 *
 * <h2>Data Structure</h2>
 * <pre>{@code
 * {
 *   "name": "Steve",
 *   "experience": 1500,
 *   "level": 3,
 *   "health": 20.0,
 *   "maxHealth": 20.0,
 *   "position": { "x": 100.5, "y": 64.0, "z": -200.25 },
 *   "gameMode": "survival"
 * }
 * }</pre>
 *
 * <h2>Type Definition (DSL)</h2>
 * <p>Uses DSL to define the extended structure:</p>
 * <pre>{@code
 * DSL.and(
 *     DSL.field("name", DSL.string()),
 *     DSL.field("experience", DSL.intType()),
 *     DSL.field("level", DSL.intType()),
 *     DSL.field("health", DSL.floatType()),
 *     DSL.field("maxHealth", DSL.floatType()),
 *     DSL.field("position", Schema110.position()),
 *     DSL.field("gameMode", DSL.string()),
 *     DSL.remainder()
 * )
 * }</pre>
 *
 * @see Schema110
 * @see PlayerV2ToV3Fix
 * @see DSL
 */
public class Schema200 extends Schema {

    /**
     * Creates the Version 2.0.0 schema.
     */
    public Schema200() {
        super(200, new Schema110());  // Extends from V1.1.0
    }

    @Override
    @NotNull
    protected TypeRegistry createTypeRegistry() {
        return new SimpleTypeRegistry();
    }

    @Override
    protected void registerTypes() {
        // Register PLAYER type with passthrough codec
        // The DSL template defines the structure for documentation
        final Type<Dynamic<?>> playerType = new SimpleType<>(
                TypeReferences.PLAYER,
                Schema100.dynamicPassthroughCodec()
        );
        registerType(playerType);
    }

    /**
     * Defines the PLAYER type template using DSL.
     *
     * <p>V2.0.0 structure: adds health, maxHealth, and level fields.</p>
     *
     * @return the player type template
     */
    @NotNull
    public static TypeTemplate player() {
        return DSL.and(
                DSL.field("name", DSL.string()),
                DSL.field("experience", DSL.intType()),
                DSL.field("level", DSL.intType()),
                DSL.field("health", DSL.floatType()),
                DSL.field("maxHealth", DSL.floatType()),
                DSL.field("position", Schema110.position()),
                DSL.field("gameMode", DSL.string()),
                DSL.remainder()
        );
    }
}
