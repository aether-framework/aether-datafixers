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

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
import de.splatgames.aether.datafixers.codec.json.gson.GsonOps;
import de.splatgames.aether.datafixers.core.fix.DataFixerBuilder;
import de.splatgames.aether.datafixers.core.fix.noop.NoOpDataFixerContext;
import de.splatgames.aether.datafixers.testkit.factory.QuickFix;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for pre-configured {@link DataFixer} instances optimized for benchmarking.
 *
 * <p>This utility class provides various DataFixer configurations for measuring
 * different aspects of migration performance. All created fixers use {@link NoOpDataFixerContext} to eliminate logging
 * overhead during benchmark measurements.</p>
 *
 * <h2>Available Fixer Configurations</h2>
 * <table border="1">
 *   <caption>Available fixer factory methods and their use cases</caption>
 *   <tr><th>Method</th><th>Fix Count</th><th>Fix Types</th><th>Use Case</th></tr>
 *   <tr>
 *     <td>{@link #createSingleFixFixer()}</td>
 *     <td>1</td>
 *     <td>Rename</td>
 *     <td>Baseline single-operation performance</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #createIdentityFixer()}</td>
 *     <td>1</td>
 *     <td>Identity (no-op)</td>
 *     <td>Framework overhead measurement</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #createChainFixer(int)}</td>
 *     <td>1-100</td>
 *     <td>Rename (homogeneous)</td>
 *     <td>Chain length scaling analysis</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #createMixedFixer(int)}</td>
 *     <td>4+</td>
 *     <td>Rename, Add, Remove, Transform</td>
 *     <td>Realistic migration scenarios</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #createPlayerFixer()}</td>
 *     <td>4</td>
 *     <td>Mixed (realistic)</td>
 *     <td>Domain-specific migration testing</td>
 *   </tr>
 * </table>
 *
 * <h2>Type References</h2>
 * <p>Two type references are provided for categorizing benchmark data:</p>
 * <ul>
 *   <li>{@link #BENCHMARK_TYPE} - Generic benchmark data (used by most benchmarks)</li>
 *   <li>{@link #PLAYER_TYPE} - Player-like data structures (for domain-specific tests)</li>
 * </ul>
 *
 * <h2>Design Considerations</h2>
 * <ul>
 *   <li><b>No-op context</b>: All fixers use {@link NoOpDataFixerContext} to prevent
 *       logging from affecting benchmark measurements</li>
 *   <li><b>GsonOps</b>: All fixes use {@link GsonOps} as the reference DynamicOps
 *       implementation for consistency</li>
 *   <li><b>Testkit integration</b>: Uses {@link QuickFix} from the testkit module
 *       for efficient fix creation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In a JMH benchmark setup method
 * @Setup(Level.Trial)
 * public void setup() {
 *     this.fixer = BenchmarkBootstrap.createChainFixer(10);
 *     this.input = BenchmarkDataGenerator.generate(GsonOps.INSTANCE, PayloadSize.MEDIUM);
 * }
 *
 * @Benchmark
 * public void benchmarkMigration(Blackhole blackhole) {
 *     Dynamic<?> result = fixer.update(
 *         BenchmarkBootstrap.BENCHMARK_TYPE,
 *         input,
 *         new DataVersion(1),
 *         new DataVersion(11)
 *     );
 *     blackhole.consume(result);
 * }
 * }</pre>
 *
 * @author Erik Pförtner
 * @see BenchmarkDataGenerator
 * @see PayloadSize
 * @see de.splatgames.aether.datafixers.testkit.factory.QuickFix
 * @since 1.0.0
 */
public final class BenchmarkBootstrap {

    /**
     * Type reference for generic benchmark data.
     *
     * <p>Used by most benchmarks as the default type for test data. The type
     * name "benchmark" is intentionally generic to avoid confusion with domain-specific types.</p>
     */
    public static final TypeReference BENCHMARK_TYPE = new TypeReference("benchmark");

    /**
     * Type reference for player-like benchmark data.
     *
     * <p>Used by benchmarks that simulate game player data migrations,
     * providing a realistic domain-specific testing scenario.</p>
     *
     * @see #createPlayerFixer()
     * @see BenchmarkDataGenerator#generatePlayerData(DynamicOps)
     */
    public static final TypeReference PLAYER_TYPE = new TypeReference("player");

    /**
     * Private constructor to prevent instantiation.
     */
    private BenchmarkBootstrap() {
        // Utility class
    }

    /**
     * Creates a DataFixer with a single field rename fix (v1 → v2).
     *
     * <p>This is the simplest non-trivial fixer configuration, useful for
     * measuring baseline single-operation performance. The fix renames a field from "oldName" to "newName".</p>
     *
     * <p>Version mapping: v1 → v2 (single step)</p>
     *
     * @return a new DataFixer configured for single-fix benchmarks
     * @see #createIdentityFixer()
     */
    @NotNull
    public static DataFixer createSingleFixFixer() {
        return new DataFixerBuilder(new DataVersion(2))
                .withDefaultContext(NoOpDataFixerContext.INSTANCE)
                .addFix(BENCHMARK_TYPE, QuickFix.renameField(
                        GsonOps.INSTANCE,
                        "rename_field_v1_v2",
                        1, 2,
                        "oldName", "newName"))
                .build();
    }

    /**
     * Creates a DataFixer with an identity fix (no-op transformation).
     *
     * <p>The identity fixer passes data through without modification, useful for
     * measuring pure framework overhead including:</p>
     * <ul>
     *   <li>Version checking and fix selection</li>
     *   <li>Dynamic wrapper creation and manipulation</li>
     *   <li>DataResult monad operations</li>
     *   <li>Type reference resolution</li>
     * </ul>
     *
     * <p>Comparing identity fixer performance against {@link #createSingleFixFixer()}
     * reveals the actual cost of field operations versus framework overhead.</p>
     *
     * <p>Version mapping: v1 → v2 (no data changes)</p>
     *
     * @return a new DataFixer with an identity (pass-through) fix
     * @see #createSingleFixFixer()
     */
    @NotNull
    public static DataFixer createIdentityFixer() {
        return new DataFixerBuilder(new DataVersion(2))
                .withDefaultContext(NoOpDataFixerContext.INSTANCE)
                .addFix(BENCHMARK_TYPE, QuickFix.identity("identity_v1_v2", 1, 2))
                .build();
    }

    /**
     * Creates a DataFixer with a chain of sequential homogeneous fixes.
     *
     * <p>Each fix in the chain performs a field rename operation (field1 → field2,
     * field2 → field3, etc.), simulating migration scenarios with multiple consecutive version upgrades. This
     * configuration is ideal for measuring how migration performance scales with chain length.</p>
     *
     * <p>Version mapping: v1 → v2 → v3 → ... → v(fixCount+1)</p>
     *
     * <h4>Typical Parameter Values for Benchmarks</h4>
     * <table border="1">
     *   <caption>Typical fixCount values and their benchmark scenarios</caption>
     *   <tr><th>fixCount</th><th>Scenario</th></tr>
     *   <tr><td>1</td><td>Baseline (compare with {@link #createSingleFixFixer()})</td></tr>
     *   <tr><td>5</td><td>Short chain (minor version updates)</td></tr>
     *   <tr><td>10</td><td>Medium chain (typical upgrade path)</td></tr>
     *   <tr><td>25</td><td>Long chain (significant version gap)</td></tr>
     *   <tr><td>50</td><td>Stress test (extended migration)</td></tr>
     *   <tr><td>100</td><td>Maximum supported (extreme case)</td></tr>
     * </table>
     *
     * @param fixCount the number of fixes in the chain (must be between 1 and 100 inclusive)
     * @return a new DataFixer with the specified number of sequential rename fixes
     * @throws IllegalArgumentException if fixCount is less than 1 or greater than 100
     * @see #createMixedFixer(int)
     */
    @NotNull
    public static DataFixer createChainFixer(final int fixCount) {
        if (fixCount < 1 || fixCount > 100) {
            throw new IllegalArgumentException("fixCount must be between 1 and 100");
        }

        final DataFixerBuilder builder = new DataFixerBuilder(new DataVersion(fixCount + 1))
                .withDefaultContext(NoOpDataFixerContext.INSTANCE);

        for (int i = 0; i < fixCount; i++) {
            final int fromVersion = i + 1;
            final int toVersion = i + 2;
            final DataFix<JsonElement> fix = QuickFix.renameField(
                    GsonOps.INSTANCE,
                    "rename_v" + fromVersion + "_v" + toVersion,
                    fromVersion, toVersion,
                    "field" + fromVersion, "field" + toVersion);
            builder.addFix(BENCHMARK_TYPE, fix);
        }

        return builder.build();
    }

    /**
     * Creates a DataFixer with mixed heterogeneous fix types for realistic benchmarking.
     *
     * <p>Unlike {@link #createChainFixer(int)} which uses only rename operations,
     * this method creates a chain with rotating fix types that more accurately represent real-world migration
     * scenarios:</p>
     *
     * <table border="1">
     *   <caption>Rotating fix types by position in the chain</caption>
     *   <tr><th>Position (mod 4)</th><th>Fix Type</th><th>Operation</th></tr>
     *   <tr><td>0</td><td>Rename</td><td>Renames a field</td></tr>
     *   <tr><td>1</td><td>Add</td><td>Adds a new string field with default value</td></tr>
     *   <tr><td>2</td><td>Remove</td><td>Removes a field</td></tr>
     *   <tr><td>3</td><td>Transform</td><td>Transforms field value (string concatenation)</td></tr>
     * </table>
     *
     * <p>Version mapping: v1 → v2 → v3 → ... → v(fixCount+1)</p>
     *
     * <p>Comparing mixed fixer performance against chain fixer performance
     * reveals the relative cost of different fix operations.</p>
     *
     * @param fixCount the number of fixes in the chain (must be at least 4 to include all fix types)
     * @return a new DataFixer with mixed fix types cycling through rename, add, remove, and transform operations
     * @throws IllegalArgumentException if fixCount is less than 4
     * @see #createChainFixer(int)
     */
    @NotNull
    public static DataFixer createMixedFixer(final int fixCount) {
        if (fixCount < 4) {
            throw new IllegalArgumentException("fixCount must be at least 4 for mixed fixes");
        }

        final DataFixerBuilder builder = new DataFixerBuilder(new DataVersion(fixCount + 1))
                .withDefaultContext(NoOpDataFixerContext.INSTANCE);

        for (int i = 0; i < fixCount; i++) {
            final int fromVersion = i + 1;
            final int toVersion = i + 2;
            final DataFix<JsonElement> fix = createMixedFix(fromVersion, toVersion, i % 4);
            builder.addFix(BENCHMARK_TYPE, fix);
        }

        return builder.build();
    }

    /**
     * Creates a DataFixer for player data migration benchmarks.
     *
     * <p>This fixer simulates a realistic game player data migration scenario
     * with four sequential fixes representing typical schema evolution:</p>
     *
     * <table border="1">
     *   <caption>Player data migration fixes by version</caption>
     *   <tr><th>Version</th><th>Fix</th><th>Description</th></tr>
     *   <tr><td>v1 → v2</td><td>Rename</td><td>{@code name} → {@code playerName}</td></tr>
     *   <tr><td>v2 → v3</td><td>Add</td><td>Add {@code score} field (default: 0)</td></tr>
     *   <tr><td>v3 → v4</td><td>Transform</td><td>Double the {@code level} value</td></tr>
     *   <tr><td>v4 → v5</td><td>Remove</td><td>Remove {@code tempField}</td></tr>
     * </table>
     *
     * <p>Use with {@link BenchmarkDataGenerator#generatePlayerData(DynamicOps)} for
     * complete domain-specific migration testing.</p>
     *
     * @return a new DataFixer configured for player data migrations (v1 → v5)
     * @see #PLAYER_TYPE
     * @see BenchmarkDataGenerator#generatePlayerData(DynamicOps)
     */
    @NotNull
    public static DataFixer createPlayerFixer() {
        return new DataFixerBuilder(new DataVersion(5))
                .withDefaultContext(NoOpDataFixerContext.INSTANCE)
                .addFix(PLAYER_TYPE, QuickFix.renameField(
                        GsonOps.INSTANCE, "rename_name_v1_v2", 1, 2,
                        "name", "playerName"))
                .addFix(PLAYER_TYPE, QuickFix.addIntField(
                        GsonOps.INSTANCE, "add_score_v2_v3", 2, 3,
                        "score", 0))
                .addFix(PLAYER_TYPE, QuickFix.transformField(
                        GsonOps.INSTANCE, "double_level_v3_v4", 3, 4,
                        "level", field -> field.createInt(
                                field.asInt().result().orElse(1) * 2)))
                .addFix(PLAYER_TYPE, QuickFix.removeField(
                        GsonOps.INSTANCE, "remove_temp_v4_v5", 4, 5,
                        "tempField"))
                .build();
    }

    /**
     * Creates a specific fix type based on the fixType selector.
     *
     * <p>Internal factory method used by {@link #createMixedFixer(int)} to create
     * different fix types in a rotating pattern.</p>
     *
     * @param fromVersion the source version for the fix
     * @param toVersion   the target version for the fix
     * @param fixType     the fix type selector (0=rename, 1=add, 2=remove, 3=transform)
     * @return a DataFix of the specified type
     */
    private static DataFix<JsonElement> createMixedFix(final int fromVersion,
                                                       final int toVersion,
                                                       final int fixType) {
        return switch (fixType) {
            case 0 -> QuickFix.renameField(
                    GsonOps.INSTANCE,
                    "rename_v" + fromVersion + "_v" + toVersion,
                    fromVersion, toVersion,
                    "renamedField", "renamedField");
            case 1 -> QuickFix.addStringField(
                    GsonOps.INSTANCE,
                    "add_v" + fromVersion + "_v" + toVersion,
                    fromVersion, toVersion,
                    "newField" + toVersion, "default");
            case 2 -> QuickFix.removeField(
                    GsonOps.INSTANCE,
                    "remove_v" + fromVersion + "_v" + toVersion,
                    fromVersion, toVersion,
                    "removedField" + fromVersion);
            case 3 -> QuickFix.transformField(
                    GsonOps.INSTANCE,
                    "transform_v" + fromVersion + "_v" + toVersion,
                    fromVersion, toVersion,
                    "transformedField",
                    field -> field.createString(
                            field.asString().result().orElse("") + "_transformed"));
            default -> QuickFix.identity(
                    "identity_v" + fromVersion + "_v" + toVersion,
                    fromVersion, toVersion);
        };
    }
}
