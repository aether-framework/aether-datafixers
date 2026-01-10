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
 * Type system for data definitions and serialization.
 *
 * <p>This package provides the type abstraction that bridges
 * {@link de.splatgames.aether.datafixers.api.TypeReference} identifiers with their associated
 * {@link de.splatgames.aether.datafixers.api.codec.Codec} implementations. Types are the runtime representation of data
 * structures defined in schemas.</p>
 *
 * <h2>Key Interfaces</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.Type} - Associates a
 *       TypeReference with a Codec and optional optic for nested access. Types
 *       know how to serialize/deserialize their data.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.SimpleType} - Basic
 *       implementation of Type for simple data structures.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.TypeRegistry} - Registry
 *       for looking up Types by their TypeReference.</li>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.Typed} - A value paired
 *       with its Type, enabling type-safe operations on runtime data.</li>
 * </ul>
 *
 * <h2>Type and Codec Relationship</h2>
 * <pre>
 * TypeReference("player")
 *         │
 *         ▼
 * ┌───────────────────┐
 * │       Type        │
 * │  ┌─────────────┐  │
 * │  │    Codec    │  │  ◄── Handles encode/decode
 * │  └─────────────┘  │
 * │  ┌─────────────┐  │
 * │  │   Finder    │  │  ◄── Locates nested types
 * │  └─────────────┘  │
 * └───────────────────┘
 * </pre>
 *
 * <h2>Working with Types</h2>
 * <pre>{@code
 * // Get type from registry
 * TypeRegistry registry = schema.typeRegistry();
 * Type<?> playerType = registry.get(TypeReferences.PLAYER);
 *
 * // Decode data using the type's codec
 * Dynamic<?> data = ...;
 * DataResult<?> decoded = playerType.codec().decode(data);
 *
 * // Create a Typed value
 * Typed<?> typed = playerType.typed(decodedValue);
 * }</pre>
 *
 * <h2>Type Templates</h2>
 * <p>The {@link de.splatgames.aether.datafixers.api.type.template} sub-package
 * provides {@link de.splatgames.aether.datafixers.api.type.template.TypeTemplate}
 * for defining type structures using the DSL. Templates are instantiated into
 * concrete Types when a schema is built.</p>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.api.type.template} - Type template
 *       definitions for schema construction</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.api.type.Type
 * @see de.splatgames.aether.datafixers.api.type.TypeRegistry
 * @see de.splatgames.aether.datafixers.api.type.Typed
 * @see de.splatgames.aether.datafixers.api.TypeReference
 * @see de.splatgames.aether.datafixers.api.codec.Codec
 * @since 0.1.0
 */
package de.splatgames.aether.datafixers.api.type;
