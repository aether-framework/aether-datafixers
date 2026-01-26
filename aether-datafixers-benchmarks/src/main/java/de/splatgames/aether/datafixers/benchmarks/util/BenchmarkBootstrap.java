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
 * Provides pre-configured {@link DataFixer} instances for benchmarking.
 *
 * <p>Creates fixers with varying numbers of fixes to measure migration
 * chain performance. All fixes use {@link NoOpDataFixerContext} to minimize
 * logging overhead during benchmarks.</p>
 *
 * @author Erik Pförtner
 * @since 1.0.0
 */
public final class BenchmarkBootstrap {

    /**
     * Type reference for benchmark data.
     */
    public static final TypeReference BENCHMARK_TYPE = new TypeReference("benchmark");

    /**
     * Type reference for player-like benchmark data.
     */
    public static final TypeReference PLAYER_TYPE = new TypeReference("player");

    private BenchmarkBootstrap() {
        // Utility class
    }

    /**
     * Creates a DataFixer with a single rename field fix.
     *
     * @return a new DataFixer configured for single-fix benchmarks
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
     * <p>Useful as a baseline to measure framework overhead without
     * actual data transformation.</p>
     *
     * @return a new DataFixer with identity fix
     */
    @NotNull
    public static DataFixer createIdentityFixer() {
        return new DataFixerBuilder(new DataVersion(2))
                .withDefaultContext(NoOpDataFixerContext.INSTANCE)
                .addFix(BENCHMARK_TYPE, QuickFix.identity("identity_v1_v2", 1, 2))
                .build();
    }

    /**
     * Creates a DataFixer with a chain of sequential fixes.
     *
     * <p>Each fix in the chain performs a field rename operation,
     * simulating real-world migration scenarios with multiple version upgrades.</p>
     *
     * @param fixCount the number of fixes in the chain (1 to 100)
     * @return a new DataFixer with the specified number of fixes
     * @throws IllegalArgumentException if fixCount is less than 1 or greater than 100
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
     * Creates a DataFixer with mixed fix types for realistic benchmarking.
     *
     * <p>Includes rename, add field, remove field, and transform operations
     * to simulate a realistic migration chain.</p>
     *
     * @param fixCount the number of fixes in the chain (must be >= 4)
     * @return a new DataFixer with mixed fix types
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
     * @return a new DataFixer configured for player data
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

    private static DataFix<JsonElement> createMixedFix(
            final int fromVersion,
            final int toVersion,
            final int fixType
    ) {
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
