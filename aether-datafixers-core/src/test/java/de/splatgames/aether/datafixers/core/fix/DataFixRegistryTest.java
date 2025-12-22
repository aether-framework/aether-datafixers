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

package de.splatgames.aether.datafixers.core.fix;

import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixerContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DataFixRegistry}.
 */
@DisplayName("DataFixRegistry")
class DataFixRegistryTest {

    private DataFixRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DataFixRegistry();
    }

    @SuppressWarnings("unchecked")
    private <T> DataFix<T> createFix(String name, int fromVersion, int toVersion) {
        return new DataFix<>() {
            @Override
            @NotNull
            public String name() {
                return name;
            }

            @Override
            @NotNull
            public DataVersion fromVersion() {
                return new DataVersion(fromVersion);
            }

            @Override
            @NotNull
            public DataVersion toVersion() {
                return new DataVersion(toVersion);
            }

            @Override
            @NotNull
            public Dynamic<T> apply(@NotNull TypeReference type,
                                    @NotNull Dynamic<T> input,
                                    @NotNull DataFixerContext context) {
                return input;
            }
        };
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("register() stores fix by type and version")
        void registerStoresFixByTypeAndVersion() {
            TypeReference type = new TypeReference("player");
            DataFix<?> fix = createFix("TestFix", 1, 2);

            registry.register(type, fix);

            List<DataFix<?>> fixes = registry.getStepFixes(type, new DataVersion(1));
            assertThat(fixes).hasSize(1).contains(fix);
        }

        @Test
        @DisplayName("register() allows multiple fixes at same version")
        void registerAllowsMultipleFixesSameVersion() {
            TypeReference type = new TypeReference("player");
            DataFix<?> fix1 = createFix("Fix1", 1, 2);
            DataFix<?> fix2 = createFix("Fix2", 1, 2);

            registry.register(type, fix1);
            registry.register(type, fix2);

            List<DataFix<?>> fixes = registry.getStepFixes(type, new DataVersion(1));
            assertThat(fixes).hasSize(2).containsExactly(fix1, fix2);
        }

        @Test
        @DisplayName("register() rejects null type")
        void registerRejectsNullType() {
            DataFix<?> fix = createFix("TestFix", 1, 2);

            assertThatThrownBy(() -> registry.register(null, fix))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("register() rejects null fix")
        void registerRejectsNullFix() {
            TypeReference type = new TypeReference("player");

            assertThatThrownBy(() -> registry.register(type, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("getStepFixes")
    class GetStepFixes {

        @Test
        @DisplayName("returns empty list for unknown type")
        void returnsEmptyListForUnknownType() {
            TypeReference unknown = new TypeReference("unknown");

            List<DataFix<?>> fixes = registry.getStepFixes(unknown, new DataVersion(1));

            assertThat(fixes).isEmpty();
        }

        @Test
        @DisplayName("returns empty list for unknown version")
        void returnsEmptyListForUnknownVersion() {
            TypeReference type = new TypeReference("player");
            registry.register(type, createFix("Fix1", 1, 2));

            List<DataFix<?>> fixes = registry.getStepFixes(type, new DataVersion(5));

            assertThat(fixes).isEmpty();
        }

        @Test
        @DisplayName("returns fixes at exact version")
        void returnsFixesAtExactVersion() {
            TypeReference type = new TypeReference("player");
            DataFix<?> fix1 = createFix("Fix1", 1, 2);
            DataFix<?> fix2 = createFix("Fix2", 2, 3);
            registry.register(type, fix1);
            registry.register(type, fix2);

            assertThat(registry.getStepFixes(type, new DataVersion(1))).containsExactly(fix1);
            assertThat(registry.getStepFixes(type, new DataVersion(2))).containsExactly(fix2);
        }

        @Test
        @DisplayName("returns immutable list")
        void returnsImmutableList() {
            TypeReference type = new TypeReference("player");
            registry.register(type, createFix("Fix1", 1, 2));

            List<DataFix<?>> fixes = registry.getStepFixes(type, new DataVersion(1));

            assertThatThrownBy(() -> fixes.add(createFix("New", 1, 2)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("getFixes (range)")
    class GetFixesRange {

        @Test
        @DisplayName("returns fixes in version range (inclusive)")
        void returnsFixesInRange() {
            TypeReference type = new TypeReference("player");
            DataFix<?> fix1 = createFix("Fix1", 1, 2);
            DataFix<?> fix2 = createFix("Fix2", 2, 3);
            DataFix<?> fix3 = createFix("Fix3", 3, 4);
            DataFix<?> fix4 = createFix("Fix4", 5, 6);

            registry.register(type, fix1);
            registry.register(type, fix2);
            registry.register(type, fix3);
            registry.register(type, fix4);

            List<DataFix<?>> fixes = registry.getFixes(type, new DataVersion(1), new DataVersion(3));

            assertThat(fixes).containsExactly(fix1, fix2, fix3);
        }

        @Test
        @DisplayName("returns empty list for empty range")
        void returnsEmptyListForEmptyRange() {
            TypeReference type = new TypeReference("player");
            registry.register(type, createFix("Fix1", 1, 2));
            registry.register(type, createFix("Fix2", 5, 6));

            List<DataFix<?>> fixes = registry.getFixes(type, new DataVersion(2), new DataVersion(4));

            assertThat(fixes).isEmpty();
        }

        @Test
        @DisplayName("returns immutable list")
        void returnsImmutableList() {
            TypeReference type = new TypeReference("player");
            registry.register(type, createFix("Fix1", 1, 2));

            List<DataFix<?>> fixes = registry.getFixes(type, new DataVersion(1), new DataVersion(5));

            assertThatThrownBy(() -> fixes.add(createFix("New", 1, 2)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("hasFixesInRange")
    class HasFixesInRange {

        @Test
        @DisplayName("returns true when fixes exist in range (exclusive start)")
        void returnsTrueWhenFixesExist() {
            TypeReference type = new TypeReference("player");
            registry.register(type, createFix("Fix1", 2, 3));

            // fromExclusive=1, toInclusive=3 should include fix at version 2
            assertThat(registry.hasFixesInRange(type, new DataVersion(1), new DataVersion(3))).isTrue();
        }

        @Test
        @DisplayName("returns false when no fixes in range")
        void returnsFalseWhenNoFixes() {
            TypeReference type = new TypeReference("player");
            registry.register(type, createFix("Fix1", 5, 6));

            assertThat(registry.hasFixesInRange(type, new DataVersion(1), new DataVersion(3))).isFalse();
        }

        @Test
        @DisplayName("excludes fromVersion (exclusive)")
        void excludesFromVersion() {
            TypeReference type = new TypeReference("player");
            registry.register(type, createFix("Fix1", 1, 2));

            // Fix at version 1, fromExclusive=1 should NOT include it
            assertThat(registry.hasFixesInRange(type, new DataVersion(1), new DataVersion(3))).isFalse();
        }

        @Test
        @DisplayName("includes toVersion (inclusive)")
        void includesToVersion() {
            TypeReference type = new TypeReference("player");
            registry.register(type, createFix("Fix1", 3, 4));

            // Fix at version 3, toInclusive=3 should include it
            assertThat(registry.hasFixesInRange(type, new DataVersion(1), new DataVersion(3))).isTrue();
        }

        @Test
        @DisplayName("returns false for unknown type")
        void returnsFalseForUnknownType() {
            TypeReference unknown = new TypeReference("unknown");

            assertThat(registry.hasFixesInRange(unknown, new DataVersion(1), new DataVersion(10))).isFalse();
        }
    }

    @Nested
    @DisplayName("Freeze Behavior")
    class FreezeBehavior {

        @Test
        @DisplayName("isFrozen() returns false initially")
        void isFrozenReturnsFalseInitially() {
            assertThat(registry.isFrozen()).isFalse();
        }

        @Test
        @DisplayName("freeze() makes registry immutable")
        void freezeMakesRegistryImmutable() {
            registry.register(new TypeReference("player"), createFix("Fix1", 1, 2));
            registry.freeze();

            assertThat(registry.isFrozen()).isTrue();
        }

        @Test
        @DisplayName("register() throws after freeze")
        void registerThrowsAfterFreeze() {
            registry.freeze();

            assertThatThrownBy(() -> registry.register(new TypeReference("player"), createFix("Fix1", 1, 2)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("frozen");
        }

        @Test
        @DisplayName("freeze() is idempotent")
        void freezeIsIdempotent() {
            TypeReference type = new TypeReference("player");
            DataFix<?> fix = createFix("Fix1", 1, 2);
            registry.register(type, fix);
            registry.freeze();
            registry.freeze(); // Should not throw

            assertThat(registry.isFrozen()).isTrue();
            assertThat(registry.getStepFixes(type, new DataVersion(1))).contains(fix);
        }

        @Test
        @DisplayName("getStepFixes() works after freeze")
        void getStepFixesWorksAfterFreeze() {
            TypeReference type = new TypeReference("player");
            DataFix<?> fix = createFix("Fix1", 1, 2);
            registry.register(type, fix);
            registry.freeze();

            assertThat(registry.getStepFixes(type, new DataVersion(1))).contains(fix);
        }

        @Test
        @DisplayName("getFixes() works after freeze")
        void getFixesWorksAfterFreeze() {
            TypeReference type = new TypeReference("player");
            DataFix<?> fix1 = createFix("Fix1", 1, 2);
            DataFix<?> fix2 = createFix("Fix2", 2, 3);
            registry.register(type, fix1);
            registry.register(type, fix2);
            registry.freeze();

            assertThat(registry.getFixes(type, new DataVersion(1), new DataVersion(2)))
                    .containsExactly(fix1, fix2);
        }

        @Test
        @DisplayName("hasFixesInRange() works after freeze")
        void hasFixesInRangeWorksAfterFreeze() {
            TypeReference type = new TypeReference("player");
            registry.register(type, createFix("Fix1", 2, 3));
            registry.freeze();

            assertThat(registry.hasFixesInRange(type, new DataVersion(1), new DataVersion(3))).isTrue();
            assertThat(registry.hasFixesInRange(type, new DataVersion(5), new DataVersion(10))).isFalse();
        }
    }

    @Nested
    @DisplayName("Multiple Types")
    class MultipleTypes {

        @Test
        @DisplayName("can store fixes for different types")
        void canStoreFixesForDifferentTypes() {
            TypeReference playerType = new TypeReference("player");
            TypeReference worldType = new TypeReference("world");

            DataFix<?> playerFix = createFix("PlayerFix", 1, 2);
            DataFix<?> worldFix = createFix("WorldFix", 1, 2);

            registry.register(playerType, playerFix);
            registry.register(worldType, worldFix);

            assertThat(registry.getStepFixes(playerType, new DataVersion(1))).containsExactly(playerFix);
            assertThat(registry.getStepFixes(worldType, new DataVersion(1))).containsExactly(worldFix);
        }

        @Test
        @DisplayName("fixes for different types are independent")
        void fixesForDifferentTypesAreIndependent() {
            TypeReference playerType = new TypeReference("player");
            TypeReference worldType = new TypeReference("world");

            registry.register(playerType, createFix("PlayerFix", 1, 2));

            assertThat(registry.getStepFixes(playerType, new DataVersion(1))).hasSize(1);
            assertThat(registry.getStepFixes(worldType, new DataVersion(1))).isEmpty();
        }
    }
}
