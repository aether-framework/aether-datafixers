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

import de.splatgames.aether.datafixers.api.codec.Codec;
import de.splatgames.aether.datafixers.api.dsl.DSL;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.api.result.DataResult;
import de.splatgames.aether.datafixers.api.schema.Schema;
import de.splatgames.aether.datafixers.api.type.SimpleType;
import de.splatgames.aether.datafixers.api.type.Type;
import de.splatgames.aether.datafixers.api.type.TypeRegistry;
import de.splatgames.aether.datafixers.api.type.template.TypeTemplate;
import de.splatgames.aether.datafixers.api.util.Pair;
import de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Schema for Version 1.0.0 (100) - Initial flat structure.
 *
 * <p>This is the first schema version with a simple, flat player structure.
 * All coordinates are stored as separate top-level fields, and gameMode
 * is stored as an integer.</p>
 *
 * <h2>Data Structure</h2>
 * <pre>{@code
 * {
 *   "playerName": "Steve",
 *   "xp": 1500,
 *   "x": 100.5,
 *   "y": 64.0,
 *   "z": -200.25,
 *   "gameMode": 0
 * }
 * }</pre>
 *
 * <h2>Type Definition (DSL)</h2>
 * <p>Uses DSL to declaratively define the structure:</p>
 * <pre>{@code
 * DSL.and(
 *     DSL.field("playerName", DSL.string()),
 *     DSL.field("xp", DSL.intType()),
 *     DSL.field("x", DSL.doubleType()),
 *     DSL.field("y", DSL.doubleType()),
 *     DSL.field("z", DSL.doubleType()),
 *     DSL.field("gameMode", DSL.intType()),
 *     DSL.remainder()
 * )
 * }</pre>
 *
 * @see Schema
 * @see DSL
 */
public class Schema100 extends Schema {

    /**
     * Creates the Version 1.0.0 schema.
     */
    public Schema100() {
        super(100, null);  // No parent - this is the first version
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
                dynamicPassthroughCodec()
        );
        registerType(playerType);
    }

    /**
     * Defines the PLAYER type template using DSL.
     *
     * <p>V1.0.0 structure: flat player with coordinates as separate fields.</p>
     *
     * <p>This template is used for:</p>
     * <ul>
     *   <li>Documentation of expected data structure</li>
     *   <li>Validation of data format</li>
     *   <li>Type-safe schema evolution</li>
     * </ul>
     *
     * @return the player type template
     */
    @NotNull
    public static TypeTemplate player() {
        return DSL.and(
                DSL.field("playerName", DSL.string()),
                DSL.field("xp", DSL.intType()),
                DSL.field("x", DSL.doubleType()),
                DSL.field("y", DSL.doubleType()),
                DSL.field("z", DSL.doubleType()),
                DSL.field("gameMode", DSL.intType()),
                DSL.remainder()  // Capture any additional fields
        );
    }

    /**
     * Creates a passthrough codec that treats Dynamic as the value type.
     *
     * <p>This allows TypeRewriteRule to work directly with Dynamic values,
     * enabling field-level transformations without requiring typed Java classes.</p>
     *
     * @return a codec that passes Dynamic through unchanged
     */
    @NotNull
    protected static Codec<Dynamic<?>> dynamicPassthroughCodec() {
        return new Codec<>() {
            @NotNull
            @Override
            public <T> DataResult<T> encode(@NotNull final Dynamic<?> input,
                                            @NotNull final DynamicOps<T> ops,
                                            @NotNull final T prefix) {
                @SuppressWarnings("unchecked")
                final Dynamic<Object> dynamicObj = (Dynamic<Object>) input;
                final Dynamic<T> converted = dynamicObj.convert(ops);
                return DataResult.success(converted.value());
            }

            @NotNull
            @Override
            public <T> DataResult<Pair<Dynamic<?>, T>> decode(@NotNull final DynamicOps<T> ops,
                                                               @NotNull final T input) {
                final Dynamic<T> dynamic = new Dynamic<>(ops, input);
                return DataResult.success(Pair.of(dynamic, ops.empty()));
            }
        };
    }
}
