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
 * Schema registry implementations.
 *
 * <p>This package provides the default implementation of
 * {@link de.splatgames.aether.datafixers.api.schema.SchemaRegistry}
 * for managing schema definitions at runtime.</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry} -
 *       A straightforward map-based registry that stores schemas indexed by
 *       their {@link de.splatgames.aether.datafixers.api.DataVersion}.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>Schema registries are typically used internally during bootstrap and
 * should not need direct manipulation by application code:</p>
 * <pre>{@code
 * public class MyBootstrap implements DataFixerBootstrap {
 *     @Override
 *     public void registerSchemas(SchemaRegistry registry) {
 *         // Registry is provided by the factory
 *         registry.register(new DataVersion(100), new Schema100());
 *         registry.register(new DataVersion(110), new Schema110());
 *         registry.register(new DataVersion(200), new Schema200());
 *     }
 * }
 * }</pre>
 *
 * <h2>Version Ordering</h2>
 * <p>The registry maintains schemas in version order, enabling efficient
 * lookup of the schema for any given version. When a version falls between
 * two registered schemas, the registry returns the schema for the highest
 * version not exceeding the requested version.</p>
 *
 * @see de.splatgames.aether.datafixers.core.schema.SimpleSchemaRegistry
 * @see de.splatgames.aether.datafixers.api.schema.SchemaRegistry
 * @see de.splatgames.aether.datafixers.api.schema.Schema
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.core.schema;
