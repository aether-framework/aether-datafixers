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
 * Schema for Version 1.1.0 (110) - Restructured with nested position.
 *
 * <p>This version introduces several improvements over V1.0.0:</p>
 * <ul>
 *   <li>Renamed {@code playerName} to {@code name}</li>
 *   <li>Renamed {@code xp} to {@code experience}</li>
 *   <li>Grouped x/y/z into nested {@code position} object</li>
 *   <li>Changed {@code gameMode} from int to string</li>
 * </ul>
 *
 * <h2>Data Structure</h2>
 * <pre>{@code
 * {
 *   "name": "Steve",
 *   "experience": 1500,
 *   "position": { "x": 100.5, "y": 64.0, "z": -200.25 },
 *   "gameMode": "survival"
 * }
 * }</pre>
 *
 * <h2>Type Definition (DSL)</h2>
 * <p>Uses DSL to define the nested structure:</p>
 * <pre>{@code
 * DSL.and(
 *     DSL.field("name", DSL.string()),
 *     DSL.field("experience", DSL.intType()),
 *     DSL.field("position", position()),
 *     DSL.field("gameMode", DSL.string()),
 *     DSL.remainder()
 * )
 * }</pre>
 *
 * @see Schema100
 * @see PlayerV1ToV2Fix
 * @see DSL
 */
public class Schema110 extends Schema {

    /**
     * Creates the Version 1.1.0 schema.
     */
    public Schema110() {
        super(110, new Schema100());  // Extends from V1.0.0
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
     * <p>V1.1.0 structure: nested position object, renamed fields, string gameMode.</p>
     *
     * @return the player type template
     */
    @NotNull
    public static TypeTemplate player() {
        return DSL.and(
                DSL.field("name", DSL.string()),
                DSL.field("experience", DSL.intType()),
                DSL.field("position", position()),
                DSL.field("gameMode", DSL.string()),
                DSL.remainder()
        );
    }

    /**
     * Defines the position type template using DSL.
     *
     * <p>3D coordinates as a nested object.</p>
     *
     * @return the position type template
     */
    @NotNull
    public static TypeTemplate position() {
        return DSL.and(
                DSL.field("x", DSL.doubleType()),
                DSL.field("y", DSL.doubleType()),
                DSL.field("z", DSL.doubleType())
        );
    }
}
