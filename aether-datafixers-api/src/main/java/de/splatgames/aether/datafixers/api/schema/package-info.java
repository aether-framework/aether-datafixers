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
 * Schema definitions associating versions with type registries.
 *
 * <p>This package provides the infrastructure for defining data schemas at
 * specific versions. A schema describes the structure of all data types at
 * a particular version, enabling the data fixer to understand what
 * transformations are needed between versions.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.schema.Schema} - Abstract base
 *       class for schema definitions. Each version of your data format should
 *       have a corresponding Schema subclass.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.schema.SchemaRegistry} - Registry
 *       for associating {@link de.splatgames.aether.datafixers.api.DataVersion}
 *       instances with their Schema definitions.</li>
 * </ul>
 *
 * <h2>Schema Hierarchy</h2>
 * <p>Schemas typically form a chain where each version inherits from or references
 * the previous version. This enables incremental schema definitions:</p>
 * <pre>
 * Schema100 (v1.0.0) → Schema110 (v1.1.0) → Schema200 (v2.0.0)
 *    │                     │                    │
 *    └─ Initial types      └─ Add fields        └─ Restructure
 * </pre>
 *
 * <h2>Implementing a Schema</h2>
 * <pre>{@code
 * public class Schema100 extends Schema {
 *
 *     public Schema100() {
 *         super(new DataVersion(100), null); // No parent for first version
 *     }
 *
 *     @Override
 *     protected void registerTypes() {
 *         // Define PLAYER type using the DSL
 *         registerType(TypeReferences.PLAYER, DSL.allWithRemainder(
 *             DSL.field("playerName", DSL.string()),
 *             DSL.field("xp", DSL.intType()),
 *             DSL.field("x", DSL.doubleType()),
 *             DSL.field("y", DSL.doubleType()),
 *             DSL.field("z", DSL.doubleType()),
 *             DSL.field("gameMode", DSL.intType())
 *         ));
 *     }
 * }
 *
 * public class Schema110 extends Schema {
 *
 *     public Schema110(Schema parent) {
 *         super(new DataVersion(110), parent);
 *     }
 *
 *     @Override
 *     protected void registerTypes() {
 *         // PLAYER type with restructured position
 *         registerType(TypeReferences.PLAYER, DSL.allWithRemainder(
 *             DSL.field("name", DSL.string()),        // Renamed
 *             DSL.field("experience", DSL.intType()), // Renamed
 *             DSL.field("position", DSL.allWithRemainder(
 *                 DSL.field("x", DSL.doubleType()),
 *                 DSL.field("y", DSL.doubleType()),
 *                 DSL.field("z", DSL.doubleType())
 *             )),
 *             DSL.field("gameMode", DSL.string())     // Changed type
 *         ));
 *     }
 * }
 * }</pre>
 *
 * <h2>Registering Schemas</h2>
 * <pre>{@code
 * public class MyBootstrap implements DataFixerBootstrap {
 *     @Override
 *     public void registerSchemas(SchemaRegistry schemas) {
 *         Schema s100 = new Schema100();
 *         Schema s110 = new Schema110(s100);
 *         Schema s200 = new Schema200(s110);
 *
 *         schemas.register(s100.version(), s100);
 *         schemas.register(s110.version(), s110);
 *         schemas.register(s200.version(), s200);
 *     }
 * }
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.api.schema.Schema
 * @see de.splatgames.aether.datafixers.api.schema.SchemaRegistry
 * @see de.splatgames.aether.datafixers.api.DataVersion
 * @see de.splatgames.aether.datafixers.api.dsl.DSL
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.schema;
