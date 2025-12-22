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
 * Type registry implementations.
 *
 * <p>This package provides the default implementation of
 * {@link de.splatgames.aether.datafixers.api.type.TypeRegistry}
 * for managing type definitions at runtime.</p>
 *
 * <h2>Key Class</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry} -
 *       A map-based registry that stores {@link de.splatgames.aether.datafixers.api.type.Type}
 *       instances indexed by their {@link de.splatgames.aether.datafixers.api.TypeReference}.</li>
 * </ul>
 *
 * <h2>Registry Lifecycle</h2>
 * <p>Type registries are created and populated during schema initialization:</p>
 * <ol>
 *   <li>Schema is instantiated</li>
 *   <li>Schema creates a new TypeRegistry</li>
 *   <li>Schema's {@code registerTypes()} method populates the registry</li>
 *   <li>Registry becomes immutable after schema initialization</li>
 * </ol>
 *
 * <h2>Type Lookup</h2>
 * <pre>{@code
 * // Get type registry from schema
 * Schema schema = schemaRegistry.get(version);
 * TypeRegistry types = schema.typeRegistry();
 *
 * // Lookup type by reference
 * Type<?> playerType = types.get(TypeReferences.PLAYER);
 *
 * // Use type for encoding/decoding
 * DataResult<?> decoded = playerType.codec().decode(dynamic);
 * }</pre>
 *
 * <h2>Relationship to Schemas</h2>
 * <p>Each {@link de.splatgames.aether.datafixers.api.schema.Schema} owns a
 * TypeRegistry that defines the types available at that schema version.
 * Different schema versions may have different type definitions, enabling
 * the data fixer to understand what transformations are needed between
 * versions.</p>
 *
 * @see de.splatgames.aether.datafixers.core.type.SimpleTypeRegistry
 * @see de.splatgames.aether.datafixers.api.type.TypeRegistry
 * @see de.splatgames.aether.datafixers.api.type.Type
 * @see de.splatgames.aether.datafixers.api.TypeReference
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.core.type;
