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

/**
 * Game data migration example demonstrating the proper DFU-style pattern.
 *
 * <p>This package provides a clean, real-world example of how to use Aether Datafixers
 * for game save data migration. It follows the same patterns as Minecraft's DataFixer Upper.</p>
 *
 * <h2>Architecture Overview</h2>
 * <pre>
 * TypeReferences.java     - Type IDs for routing (PLAYER, WORLD, etc.)
 *        |
 * Schema100.java          - Schema for Version 1
 * Schema110.java          - Schema for Version 2
 * Schema200.java          - Schema for Version 3
 *        |
 * PlayerV1ToV2Fix.java    - Migration V1 -> V2
 * PlayerV2ToV3Fix.java    - Migration V2 -> V3
 *        |
 * GameDataBootstrap.java  - Registers schemas and fixes
 *        |
 * GameExample.java        - Main: demonstrates encode/update/decode workflow
 * </pre>
 *
 * <h2>Version History</h2>
 * <table border="1">
 *   <caption>Version History</caption>
 *   <tr><th>Version</th><th>Player Data Structure</th></tr>
 *   <tr><td>1 (100)</td><td>playerName, xp, x/y/z, gameMode (int)</td></tr>
 *   <tr><td>2 (200)</td><td>name, experience, position{x,y,z}, gameMode (string)</td></tr>
 *   <tr><td>3 (300)</td><td>+ level, health, maxHealth</td></tr>
 * </table>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.TypeReferences} -
 *       Type reference constants</li>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.Schema100} -
 *       Schema for Version 1</li>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.Schema110} -
 *       Schema for Version 2</li>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.Schema200} -
 *       Schema for Version 3</li>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.PlayerV1ToV2Fix} -
 *       Fix for V1 to V2 migration</li>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.PlayerV2ToV3Fix} -
 *       Fix for V2 to V3 migration</li>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.GameDataBootstrap} -
 *       DataFixerBootstrap implementation</li>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.GameExample} -
 *       Main runnable example</li>
 * </ul>
 *
 * <h2>Running the Example</h2>
 * <pre>{@code
 * mvn exec:java -pl aether-datafixers-examples
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.examples.game.GameExample
 */
package de.splatgames.aether.datafixers.examples.game;
