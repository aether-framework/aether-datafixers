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

import de.splatgames.aether.datafixers.api.TypeReference;

/**
 * Defines all type references used in the game data system.
 *
 * <p>Type references serve as unique identifiers for different data types
 * in the migration system. They are used to:</p>
 * <ul>
 *   <li>Register types in schemas at each version</li>
 *   <li>Associate fixes with specific data types</li>
 *   <li>Look up types during migration</li>
 * </ul>
 *
 * <h2>Pattern</h2>
 * <p>This follows the same pattern as Minecraft's DataFixer Upper (DFU),
 * where type references are defined as static constants in a central location.</p>
 *
 * @see TypeReference
 */
public final class TypeReferences {

    /**
     * Type reference for player save data.
     *
     * <p>Player data includes name, position, health, experience, etc.</p>
     */
    public static final TypeReference PLAYER = new TypeReference("player");

    /**
     * Type reference for world/level data.
     *
     * <p>World data includes spawn position, world settings, etc.</p>
     */
    public static final TypeReference WORLD = new TypeReference("world");

    private TypeReferences() {
        // Prevent instantiation
    }
}
