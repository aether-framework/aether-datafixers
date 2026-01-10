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

package de.splatgames.aether.datafixers.api.dynamic;

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.TypeReference;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link Dynamic} value paired with a {@link TypeReference} identifier.
 *
 * <p>{@code TaggedDynamic} associates a dynamic value with a type reference,
 * which is useful in data migration scenarios where the type of data needs to be tracked alongside the data itself.
 * This enables type-aware transformations and fixes.</p>
 *
 * <h2>Usage Context</h2>
 * <p>TaggedDynamic is primarily used in the data fixer system where different
 * types of data (entities, block entities, chunks, etc.) need to be migrated using type-specific transformation
 * rules.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a tagged dynamic for entity data
 * Dynamic<JsonElement> entityData = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
 * TaggedDynamic taggedEntity = new TaggedDynamic(TypeReferences.ENTITY, entityData);
 *
 * // Access type and value
 * TypeReference type = taggedEntity.type();
 * Dynamic<?> value = taggedEntity.value();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @author Erik Pförtner
 * @see Dynamic
 * @see TypeReference
 * @since 0.1.0
 */
public class TaggedDynamic {

    /**
     * The type reference identifying the kind of data.
     */
    private final TypeReference type;

    /**
     * The dynamic value containing the actual data.
     */
    private final Dynamic<?> value;

    /**
     * Creates a new TaggedDynamic with the specified type and value.
     *
     * @param type  the type reference identifying the kind of data, must not be {@code null}
     * @param value the dynamic value containing the data, must not be {@code null}
     * @throws NullPointerException if {@code type} or {@code value} is {@code null}
     */
    public TaggedDynamic(@NotNull final TypeReference type, @NotNull final Dynamic<?> value) {
        Preconditions.checkNotNull(type, "TypeReference type must not be null");
        Preconditions.checkNotNull(value, "Dynamic<?> value must not be null");

        this.type = type;
        this.value = value;
    }

    /**
     * Returns the type reference for this tagged dynamic.
     *
     * <p>The type reference identifies the kind of data contained in the
     * dynamic value, enabling type-specific processing and transformations.</p>
     *
     * @return the type reference, never {@code null}
     */
    @NotNull
    public TypeReference type() {
        return this.type;
    }

    /**
     * Returns the dynamic value.
     *
     * <p>The dynamic value contains the actual data and provides operations
     * for reading and manipulating it.</p>
     *
     * @return the dynamic value, never {@code null}
     */
    @NotNull
    public Dynamic<?> value() {
        return this.value;
    }
}
