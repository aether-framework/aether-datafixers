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
     * The type reference identifying the kind of data contained in this tagged dynamic.
     *
     * <p>This reference is used by the data fixing system to determine which
     * transformation rules should be applied to the data. It serves as a discriminator that routes the data through the
     * appropriate fixes during migration.</p>
     *
     * <p>Common type references include:</p>
     * <ul>
     *   <li>{@code "player"} - Player entity data</li>
     *   <li>{@code "entity"} - Generic entity data</li>
     *   <li>{@code "block_entity"} - Block entity (tile entity) data</li>
     *   <li>{@code "chunk"} - Chunk data</li>
     *   <li>{@code "world"} - World/level data</li>
     * </ul>
     */
    private final TypeReference type;

    /**
     * The dynamic value containing the actual data to be processed.
     *
     * <p>This value holds the raw data in a format-agnostic representation
     * that can be manipulated using the {@link Dynamic} API. The data can be in any format (JSON, YAML, NBT, etc.) as
     * long as an appropriate {@link DynamicOps} implementation is provided.</p>
     */
    private final Dynamic<?> value;

    /**
     * Creates a new TaggedDynamic with the specified type and value.
     *
     * <p>This constructor pairs a type identifier with the actual data, creating
     * a self-describing data unit that can be processed by the data fixing system.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * // Create a tagged dynamic for player data
     * TypeReference playerRef = new TypeReference("player");
     * Dynamic<JsonElement> playerData = new Dynamic<>(GsonOps.INSTANCE, jsonElement);
     * TaggedDynamic taggedPlayer = new TaggedDynamic(playerRef, playerData);
     *
     * // Use with the data fixer
     * DataFixer fixer = ...;
     * Dynamic<?> migrated = fixer.update(
     *     taggedPlayer.type(),
     *     taggedPlayer.value(),
     *     oldVersion,
     *     newVersion
     * );
     * }</pre>
     *
     * @param type  the type reference identifying the kind of data; must not be {@code null}
     * @param value the dynamic value containing the data; must not be {@code null}
     * @throws NullPointerException if {@code type} or {@code value} is {@code null}
     */
    public TaggedDynamic(@NotNull final TypeReference type, @NotNull final Dynamic<?> value) {
        Preconditions.checkNotNull(type, "type must not be null");
        Preconditions.checkNotNull(value, "value must not be null");

        this.type = type;
        this.value = value;
    }

    /**
     * Returns the type reference for this tagged dynamic.
     *
     * <p>The type reference identifies the kind of data contained in the
     * dynamic value, enabling type-specific processing and transformations. This is used by the data fixing system to
     * route the data through appropriate migration fixes.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TaggedDynamic tagged = ...;
     *
     * // Check the type before processing
     * if (tagged.type().getId().equals("player")) {
     *     // Process as player data
     *     processPlayerData(tagged.value());
     * } else if (tagged.type().getId().equals("entity")) {
     *     // Process as generic entity data
     *     processEntityData(tagged.value());
     * }
     * }</pre>
     *
     * @return the type reference identifying the data kind; never {@code null}
     * @see TypeReference
     */
    @NotNull
    public TypeReference type() {
        return this.type;
    }

    /**
     * Returns the dynamic value containing the actual data.
     *
     * <p>The dynamic value provides a format-agnostic API for reading and
     * manipulating the data. Through the {@link Dynamic} class, you can access fields, read primitives, and perform
     * transformations without knowing the underlying serialization format.</p>
     *
     * <h4>Example</h4>
     * <pre>{@code
     * TaggedDynamic tagged = ...;
     * Dynamic<?> data = tagged.value();
     *
     * // Read fields from the data
     * String name = data.get("name").asString().result().orElse("unknown");
     * int level = data.get("level").asInt().result().orElse(1);
     *
     * // Modify the data
     * Dynamic<?> updated = data
     *     .set("level", data.createInt(level + 1))
     *     .set("lastModified", data.createLong(System.currentTimeMillis()));
     * }</pre>
     *
     * @return the dynamic value containing the data; never {@code null}
     * @see Dynamic
     */
    @NotNull
    public Dynamic<?> value() {
        return this.value;
    }
}
