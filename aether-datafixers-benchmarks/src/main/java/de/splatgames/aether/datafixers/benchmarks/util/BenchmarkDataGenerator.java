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

package de.splatgames.aether.datafixers.benchmarks.util;

import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.dynamic.DynamicOps;
import de.splatgames.aether.datafixers.testkit.TestData;
import de.splatgames.aether.datafixers.testkit.TestDataBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for generating benchmark test data.
 *
 * <p>Generates {@link Dynamic} objects with configurable complexity based on
 * {@link PayloadSize} settings. Uses the testkit's {@link TestDataBuilder}
 * for efficient, format-agnostic data construction.</p>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
public final class BenchmarkDataGenerator {

    private BenchmarkDataGenerator() {
        // Utility class
    }

    /**
     * Generates benchmark data with the specified payload size.
     *
     * <p>Creates a complex object structure including:
     * <ul>
     *   <li>Primitive fields (strings, integers, booleans)</li>
     *   <li>Nested objects up to the configured depth</li>
     *   <li>A list with the configured number of items</li>
     * </ul>
     *
     * @param ops  the DynamicOps to use for data creation
     * @param size the payload size configuration
     * @param <T>  the underlying value type
     * @return a new Dynamic containing the generated data
     */
    @NotNull
    public static <T> Dynamic<T> generate(
            @NotNull final DynamicOps<T> ops,
            @NotNull final PayloadSize size
    ) {
        final TestDataBuilder<T> builder = TestData.using(ops).object();

        // Add primitive fields
        for (int i = 0; i < size.getFieldCount(); i++) {
            builder.put("stringField" + i, "value" + i);
            builder.put("intField" + i, i * 100);
            builder.put("boolField" + i, i % 2 == 0);
        }

        // Add nested objects
        addNestedObject(builder, "nested", size.getNestingDepth());

        // Add list with items
        builder.putList("items", list -> {
            for (int i = 0; i < size.getListSize(); i++) {
                final int index = i;
                list.addObject(item -> item
                        .put("id", "item-" + index)
                        .put("quantity", index + 1)
                        .put("active", index % 3 == 0));
            }
        });

        return builder.build();
    }

    /**
     * Generates a player-like data structure for realistic migration benchmarks.
     *
     * <p>Creates a structure similar to game player data with:
     * <ul>
     *   <li>Identity fields (id, name)</li>
     *   <li>Stats (level, experience, health)</li>
     *   <li>Position object (x, y, z, world)</li>
     *   <li>Inventory list</li>
     *   <li>Achievements list</li>
     * </ul>
     *
     * @param ops the DynamicOps to use for data creation
     * @param <T> the underlying value type
     * @return a new Dynamic containing player-like data
     */
    @NotNull
    public static <T> Dynamic<T> generatePlayerData(@NotNull final DynamicOps<T> ops) {
        return TestData.using(ops)
                .object()
                .put("id", "player-benchmark-12345")
                .put("name", "BenchmarkPlayer")
                .put("level", 50)
                .put("experience", 125000L)
                .put("health", 100.0)
                .put("active", true)
                .putObject("position", pos -> pos
                        .put("x", 100.5)
                        .put("y", 64.0)
                        .put("z", -200.25)
                        .put("world", "overworld"))
                .putObject("stats", stats -> stats
                        .put("strength", 15)
                        .put("agility", 12)
                        .put("intelligence", 18)
                        .put("luck", 7))
                .putList("inventory", inv -> {
                    for (int i = 0; i < 36; i++) {
                        final int slot = i;
                        inv.addObject(item -> item
                                .put("slot", slot)
                                .put("itemId", "minecraft:item_" + slot)
                                .put("count", (slot % 64) + 1)
                                .put("damage", 0));
                    }
                })
                .putList("achievements", list -> list
                        .add("first_login")
                        .add("level_10")
                        .add("level_25")
                        .add("level_50")
                        .add("explorer")
                        .add("master_crafter"))
                .build();
    }

    /**
     * Generates a simple flat object for basic operation benchmarks.
     *
     * @param ops        the DynamicOps to use for data creation
     * @param fieldCount the number of fields to generate
     * @param <T>        the underlying value type
     * @return a new Dynamic containing flat data
     */
    @NotNull
    public static <T> Dynamic<T> generateFlat(
            @NotNull final DynamicOps<T> ops,
            final int fieldCount
    ) {
        final TestDataBuilder<T> builder = TestData.using(ops).object();
        for (int i = 0; i < fieldCount; i++) {
            builder.put("field" + i, "value" + i);
        }
        return builder.build();
    }

    private static <T> void addNestedObject(
            final TestDataBuilder<T> builder,
            final String key,
            final int depth
    ) {
        if (depth <= 0) {
            return;
        }
        builder.putObject(key, nested -> {
            nested.put("level", depth);
            nested.put("data", "nested-level-" + depth);
            nested.put("timestamp", System.currentTimeMillis());
            addNestedObject(nested, "child", depth - 1);
        });
    }
}
