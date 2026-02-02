/*
 * Copyright (c) 2026 Splatgames.de Software and Contributors
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
 * Factory for generating benchmark test data with configurable complexity.
 *
 * <p>This utility class creates {@link Dynamic} objects of varying sizes and
 * structures for use in JMH benchmarks. Data generation is format-agnostic, working with any {@link DynamicOps}
 * implementation.</p>
 *
 * <h2>Data Generation Methods</h2>
 * <table border="1">
 *   <tr><th>Method</th><th>Structure</th><th>Use Case</th></tr>
 *   <tr>
 *     <td>{@link #generate(DynamicOps, PayloadSize)}</td>
 *     <td>Complex (fields + nesting + lists)</td>
 *     <td>General-purpose benchmarks</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #generatePlayerData(DynamicOps)}</td>
 *     <td>Domain-specific (player data)</td>
 *     <td>Realistic migration scenarios</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #generateFlat(DynamicOps, int)}</td>
 *     <td>Flat object (fields only)</td>
 *     <td>Basic operation benchmarks</td>
 *   </tr>
 * </table>
 *
 * <h2>Generated Data Structure</h2>
 * <p>The main {@link #generate(DynamicOps, PayloadSize)} method creates objects with:</p>
 * <pre>{@code
 * {
 *   "stringField0": "value0",
 *   "intField0": 0,
 *   "boolField0": true,
 *   "stringField1": "value1",
 *   ...
 *   "nested": {
 *     "level": 4,
 *     "data": "nested-level-4",
 *     "timestamp": 1234567890,
 *     "child": {
 *       "level": 3,
 *       ...
 *     }
 *   },
 *   "items": [
 *     {"id": "item-0", "quantity": 1, "active": true},
 *     {"id": "item-1", "quantity": 2, "active": false},
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * <h2>Design Considerations</h2>
 * <ul>
 *   <li><b>Testkit integration</b>: Uses {@link TestDataBuilder} for fluent,
 *       type-safe data construction</li>
 *   <li><b>Format agnostic</b>: Works with any DynamicOps (Gson, Jackson, YAML, etc.)</li>
 *   <li><b>Deterministic</b>: Generated data is fully reproducible for benchmark consistency</li>
 *   <li><b>Configurable complexity</b>: {@link PayloadSize} controls data volume</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In a JMH benchmark
 * @Setup(Level.Iteration)
 * public void setup() {
 *     // Generate medium-complexity test data
 *     this.input = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, PayloadSize.MEDIUM);
 *
 *     // Or generate player-specific data
 *     this.playerData = BenchmarkDataGenerator.generatePlayerData(GsonOps.INSTANCE);
 * }
 * }</pre>
 *
 * @author Erik Pförtner
 * @see PayloadSize
 * @see BenchmarkBootstrap
 * @see de.splatgames.aether.datafixers.testkit.TestDataBuilder
 * @since 1.0.0
 */
public final class BenchmarkDataGenerator {

    /**
     * Fixed timestamp value used for deterministic benchmark data generation.
     *
     * <p>Using a constant timestamp ensures reproducible benchmark results
     * across different runs, eliminating variability from system time.</p>
     */
    private static final long FIXED_TIMESTAMP = 1704067200000L; // 2024-01-01 00:00:00 UTC

    /**
     * Private constructor to prevent instantiation.
     */
    private BenchmarkDataGenerator() {
        // Utility class
    }

    /**
     * Generates benchmark data with the specified payload size and complexity.
     *
     * <p>Creates a complex object structure including:</p>
     * <ul>
     *   <li><b>Primitive fields</b>: String, integer, and boolean fields based on
     *       {@link PayloadSize#getFieldCount()}</li>
     *   <li><b>Nested objects</b>: Recursive nesting up to
     *       {@link PayloadSize#getNestingDepth()} levels</li>
     *   <li><b>List with items</b>: An "items" array with
     *       {@link PayloadSize#getListSize()} objects</li>
     * </ul>
     *
     * <h3>Field Naming Patterns</h3>
     * <table border="1">
     *   <tr><th>Field Type</th><th>Pattern</th><th>Example</th></tr>
     *   <tr><td>String</td><td>{@code stringFieldN}</td><td>{@code stringField0: "value0"}</td></tr>
     *   <tr><td>Integer</td><td>{@code intFieldN}</td><td>{@code intField0: 0}</td></tr>
     *   <tr><td>Boolean</td><td>{@code boolFieldN}</td><td>{@code boolField0: true}</td></tr>
     * </table>
     *
     * @param ops  the DynamicOps implementation to use for data creation
     * @param size the payload size configuration controlling data complexity
     * @param <T>  the underlying value type of the DynamicOps
     * @return a new Dynamic containing the generated benchmark data
     */
    @NotNull
    public static <T> Dynamic<T> generate(@NotNull final DynamicOps<T> ops,
                                          @NotNull final PayloadSize size) {
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
     * <p>Creates a structure simulating game player data, useful for domain-specific
     * migration testing with {@link BenchmarkBootstrap#createPlayerFixer()}.</p>
     *
     * <h3>Generated Structure</h3>
     * <pre>{@code
     * {
     *   "id": "player-benchmark-12345",
     *   "name": "BenchmarkPlayer",
     *   "level": 50,
     *   "experience": 125000,
     *   "health": 100.0,
     *   "active": true,
     *   "position": {"x": 100.5, "y": 64.0, "z": -200.25, "world": "overworld"},
     *   "stats": {"strength": 15, "agility": 12, "intelligence": 18, "luck": 7},
     *   "inventory": [{"slot": 0, "itemId": "minecraft:item_0", "count": 1, "damage": 0}, ...],
     *   "achievements": ["first_login", "level_10", "level_25", "level_50", ...]
     * }
     * }</pre>
     *
     * <h3>Data Characteristics</h3>
     * <table border="1">
     *   <tr><th>Component</th><th>Count</th><th>Description</th></tr>
     *   <tr><td>Top-level fields</td><td>6</td><td>id, name, level, experience, health, active</td></tr>
     *   <tr><td>Nested objects</td><td>2</td><td>position (4 fields), stats (4 fields)</td></tr>
     *   <tr><td>Inventory slots</td><td>36</td><td>Standard inventory size</td></tr>
     *   <tr><td>Achievements</td><td>6</td><td>String list</td></tr>
     * </table>
     *
     * @param ops the DynamicOps implementation to use for data creation
     * @param <T> the underlying value type of the DynamicOps
     * @return a new Dynamic containing player-like benchmark data
     * @see BenchmarkBootstrap#createPlayerFixer()
     * @see BenchmarkBootstrap#PLAYER_TYPE
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
     * Generates a simple flat object with only string fields.
     *
     * <p>Creates a minimal object structure without nesting or lists, useful for
     * benchmarking basic field access and manipulation operations with minimal traversal overhead.</p>
     *
     * <h3>Generated Structure</h3>
     * <pre>{@code
     * {
     *   "field0": "value0",
     *   "field1": "value1",
     *   "field2": "value2",
     *   ...
     * }
     * }</pre>
     *
     * <p>This method is useful for isolating field operation costs from
     * structural complexity overhead.</p>
     *
     * @param ops        the DynamicOps implementation to use for data creation
     * @param fieldCount the number of string fields to generate (field0 through field(n-1))
     * @param <T>        the underlying value type of the DynamicOps
     * @return a new Dynamic containing a flat object with string fields
     */
    @NotNull
    public static <T> Dynamic<T> generateFlat(@NotNull final DynamicOps<T> ops,
                                              final int fieldCount) {
        final TestDataBuilder<T> builder = TestData.using(ops).object();
        for (int i = 0; i < fieldCount; i++) {
            builder.put("field" + i, "value" + i);
        }
        return builder.build();
    }

    /**
     * Recursively adds nested object structures to the builder.
     *
     * <p>Creates a chain of nested objects, each containing:</p>
     * <ul>
     *   <li>{@code level} - the current nesting depth</li>
     *   <li>{@code data} - a string identifying the nesting level</li>
     *   <li>{@code timestamp} - fixed timestamp for reproducibility</li>
     *   <li>{@code child} - the next nested level (if depth &gt; 0)</li>
     * </ul>
     *
     * @param builder the TestDataBuilder to add the nested structure to
     * @param key     the field name for this nested object
     * @param depth   remaining nesting levels (stops when depth reaches 0)
     * @param <T>     the underlying value type of the builder
     */
    private static <T> void addNestedObject(final TestDataBuilder<T> builder,
                                            final String key,
                                            final int depth) {
        if (depth <= 0) {
            return;
        }
        builder.putObject(key, nested -> {
            nested.put("level", depth);
            nested.put("data", "nested-level-" + depth);
            nested.put("timestamp", FIXED_TIMESTAMP);
            addNestedObject(nested, "child", depth - 1);
        });
    }
}
