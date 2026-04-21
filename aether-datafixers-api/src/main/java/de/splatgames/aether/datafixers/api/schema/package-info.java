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
 * specific versions. A schema describes the structure of all data types at a
 * particular version, enabling the data fixer to understand what
 * transformations are needed between versions.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.schema.Schema} — Concrete
 *       class that pairs a {@link de.splatgames.aether.datafixers.api.DataVersion}
 *       with a {@link de.splatgames.aether.datafixers.api.type.TypeRegistry}.
 *       Can be instantiated directly or subclassed to inherit types from a
 *       parent schema.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.schema.SchemaRegistry} —
 *       Registry that maps {@code DataVersion}s to their {@code Schema}
 *       definitions.</li>
 * </ul>
 *
 * <h2>Schema Hierarchy</h2>
 * <p>Schemas typically form a chain where each version inherits from the
 * previous one, enabling incremental definitions: a child schema only needs
 * to register the types that changed.</p>
 * <pre>
 * Schema100 (v1.0.0) &#x2192; Schema110 (v1.1.0) &#x2192; Schema200 (v2.0.0)
 *    &#x2502;                     &#x2502;                    &#x2502;
 *    &#x2514; Initial types       &#x2514; Add fields         &#x2514; Restructure
 * </pre>
 *
 * <h2>Implementing a Schema</h2>
 * <p>Subclass {@code Schema} using the {@code (int versionId, Schema parent)}
 * constructor and override {@link de.splatgames.aether.datafixers.api.schema.Schema#registerTypes() registerTypes}.
 * Inside that method, call {@code registerType(TypeReference, TypeTemplate)}
 * with a DSL template. Use {@link de.splatgames.aether.datafixers.api.dsl.DSL#and(de.splatgames.aether.datafixers.api.type.template.TypeTemplate...) DSL.and}
 * plus {@link de.splatgames.aether.datafixers.api.dsl.DSL#remainder() DSL.remainder()}
 * to allow fields not declared explicitly to pass through:</p>
 * <pre>{@code
 * public class Schema100 extends Schema {
 *     public Schema100() {
 *         super(100, null); // First version — no parent
 *     }
 *
 *     @Override
 *     protected TypeRegistry createTypeRegistry() {
 *         return new SimpleTypeRegistry();
 *     }
 *
 *     @Override
 *     protected void registerTypes() {
 *         registerType(TypeReferences.PLAYER, DSL.and(
 *             DSL.field("playerName", DSL.string()),
 *             DSL.field("xp",         DSL.intType()),
 *             DSL.field("x",          DSL.doubleType()),
 *             DSL.field("y",          DSL.doubleType()),
 *             DSL.field("z",          DSL.doubleType()),
 *             DSL.field("gameMode",   DSL.intType()),
 *             DSL.remainder()
 *         ));
 *     }
 * }
 *
 * public class Schema110 extends Schema {
 *     public Schema110(Schema parent) {
 *         super(110, parent); // Inherits from Schema100
 *     }
 *
 *     @Override
 *     protected TypeRegistry createTypeRegistry() {
 *         return new SimpleTypeRegistry();
 *     }
 *
 *     @Override
 *     protected void registerTypes() {
 *         // Only types that changed need to be re-registered
 *         registerType(TypeReferences.PLAYER, DSL.and(
 *             DSL.field("name",       DSL.string()),   // renamed from "playerName"
 *             DSL.field("experience", DSL.intType()),  // renamed from "xp"
 *             DSL.field("position", DSL.and(
 *                 DSL.field("x", DSL.doubleType()),
 *                 DSL.field("y", DSL.doubleType()),
 *                 DSL.field("z", DSL.doubleType())
 *             )),
 *             DSL.field("gameMode",   DSL.string()),   // type changed int -> string
 *             DSL.remainder()
 *         ));
 *     }
 * }
 * }</pre>
 *
 * <h2>Registering Schemas</h2>
 * <p>Both overloads of {@link de.splatgames.aether.datafixers.api.schema.SchemaRegistry#register(de.splatgames.aether.datafixers.api.schema.Schema) register}
 * accept a schema; the single-argument version uses {@link de.splatgames.aether.datafixers.api.schema.Schema#version() Schema.version()}
 * as the key:</p>
 * <pre>{@code
 * public class MyBootstrap implements DataFixerBootstrap {
 *     @Override
 *     public void registerSchemas(SchemaRegistry schemas) {
 *         Schema s100 = new Schema100();
 *         Schema s110 = new Schema110(s100);
 *         Schema s200 = new Schema200(s110);
 *
 *         schemas.register(s100);
 *         schemas.register(s110);
 *         schemas.register(s200);
 *     }
 * }
 * }</pre>
 *
 * <h2>Direct (Non-Subclassed) Schemas</h2>
 * <p>If you already have a fully-built {@link de.splatgames.aether.datafixers.api.type.TypeRegistry},
 * use {@code new Schema(version, typeRegistry)} directly — no subclassing
 * required.</p>
 *
 * @see de.splatgames.aether.datafixers.api.schema.Schema
 * @see de.splatgames.aether.datafixers.api.schema.SchemaRegistry
 * @see de.splatgames.aether.datafixers.api.DataVersion
 * @see de.splatgames.aether.datafixers.api.dsl.DSL
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.schema;
