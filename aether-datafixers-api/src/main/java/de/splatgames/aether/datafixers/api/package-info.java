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
 * Core API for the Aether Datafixers data migration framework.
 *
 * <p>This package provides the foundational types for the data fixing system,
 * including version identifiers and type references that form the basis for
 * all data migrations.</p>
 *
 * <h2>Overview</h2>
 * <p>Aether Datafixers is a lightweight data migration framework inspired by
 * Minecraft's DataFixer Upper (DFU). It enables forward patching of serialized
 * data through schema definitions and versioned fixers.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.DataVersion} - Integer-based
 *       version identifier for data schemas. Used to track which version a piece
 *       of serialized data belongs to.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.TypeReference} - String-based
 *       identifier for data types (e.g., "player", "entity"). Used to route data
 *       to the appropriate fixers and type definitions.</li>
 * </ul>
 *
 * <h2>Package Structure</h2>
 * <p>The API module is organized into the following sub-packages:</p>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.bootstrap} - Bootstrap interfaces
 *       for initializing the data fixer system</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.codec} - Codec interfaces for
 *       bidirectional data transformation</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.dsl} - Domain-specific language
 *       for defining type templates</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.dynamic} - Format-agnostic data
 *       representation via Dynamic and DynamicOps</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.exception} - Exception hierarchy
 *       for error handling</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.fix} - Data fix interfaces and
 *       the main DataFixer entry point</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.optic} - Profunctor optics for
 *       composable data access</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.result} - Result types for
 *       representing success/failure outcomes</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.rewrite} - Type rewrite rules
 *       for schema transformations</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.schema} - Schema definitions
 *       associating versions with type registries</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.type} - Type system for data
 *       definitions and codecs</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.util} - Utility types like
 *       Pair, Either, and Unit</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define type references for your data types
 * TypeReference PLAYER = new TypeReference("player");
 * TypeReference WORLD = new TypeReference("world");
 *
 * // Define versions using semantic versioning encoded as integers
 * DataVersion V1_0_0 = new DataVersion(100);
 * DataVersion V1_1_0 = new DataVersion(110);
 * DataVersion V2_0_0 = new DataVersion(200);
 *
 * // Use the DataFixer to migrate data
 * DataFixer fixer = ...;
 * Dynamic<?> updated = fixer.update(PLAYER, oldData, V1_0_0, V2_0_0);
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.api.DataVersion
 * @see de.splatgames.aether.datafixers.api.TypeReference
 * @see de.splatgames.aether.datafixers.api.fix.DataFixer
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api;
