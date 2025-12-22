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
 * Usage examples for the Aether Datafixers library.
 *
 * <p>This module provides a clean, real-world example demonstrating the proper
 * DFU-style pattern for data migration.</p>
 *
 * <h2>Example: Game Data Migration</h2>
 * <p>The {@link de.splatgames.aether.datafixers.examples.game} package contains
 * a complete example showing how to migrate game save data through multiple versions.</p>
 *
 * <h3>Architecture</h3>
 * <pre>
 * TypeReferences.java     - Type IDs for routing (PLAYER, WORLD, etc.)
 *        |
 * PlayerSchemas.java      - Schema definitions for each version
 *        |
 * PlayerV1ToV2Fix.java &amp; PlayerV2ToV3Fix.java - DataFix implementations
 *        |
 * GameDataBootstrap.java  - Registers schemas and fixes
 *        |
 * GameExample.java        - Main: demonstrates encode/update/decode workflow
 * </pre>
 *
 * <h3>Key Classes</h3>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.GameExample} -
 *       Main runnable demonstrating the complete workflow</li>
 *   <li>{@link de.splatgames.aether.datafixers.examples.game.GameDataBootstrap} -
 *       DataFixerBootstrap implementation</li>
 * </ul>
 *
 * <h2>Running the Example</h2>
 * <pre>
 * mvn exec:java -pl aether-datafixers-examples
 * </pre>
 *
 * @see de.splatgames.aether.datafixers.examples.game
 * @see de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap
 * @see de.splatgames.aether.datafixers.core.fix.SchemaDataFix
 */
package de.splatgames.aether.datafixers.examples;
