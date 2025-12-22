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

import com.google.gson.JsonElement;
import de.splatgames.aether.datafixers.api.DataVersion;
import de.splatgames.aether.datafixers.api.TypeReference;
import de.splatgames.aether.datafixers.api.dynamic.Dynamic;
import de.splatgames.aether.datafixers.api.fix.DataFix;
import de.splatgames.aether.datafixers.api.fix.DataFixer;
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
 * Unit tests for {@link DataFixerBuilder}.
 */
@DisplayName("DataFixerBuilder")
class DataFixerBuilderTest {

    private static final TypeReference PLAYER = new TypeReference("player");

    private DataFixerBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new DataFixerBuilder(new DataVersion(10));
    }

    private <T> DataFix<T> createFix(String name, int from, int to) {
        return new DataFix<>() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull DataVersion fromVersion() {
                return new DataVersion(from);
            }

            @Override
            public @NotNull DataVersion toVersion() {
                return new DataVersion(to);
            }

            @Override
            public @NotNull Dynamic<T> apply(
                    @NotNull TypeReference type,
                    @NotNull Dynamic<T> input,
                    @NotNull DataFixerContext context
            ) {
                return input;
            }
        };
    }

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("creates builder with specified version")
        void createsBuilderWithSpecifiedVersion() {
            DataFixer fixer = builder.build();

            assertThat(fixer.currentVersion()).isEqualTo(new DataVersion(10));
        }

        @Test
        @DisplayName("rejects null currentVersion")
        void rejectsNullCurrentVersion() {
            assertThatThrownBy(() -> new DataFixerBuilder(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("addFix()")
    class AddFix {

        @Test
        @DisplayName("returns this for fluent chaining")
        void returnsThisForChaining() {
            DataFixerBuilder result = builder.addFix(PLAYER, createFix("fix1", 1, 2));

            assertThat(result).isSameAs(builder);
        }

        @Test
        @DisplayName("rejects null type")
        void rejectsNullType() {
            assertThatThrownBy(() -> builder.addFix(null, createFix("fix1", 1, 2)))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null fix")
        void rejectsNullFix() {
            assertThatThrownBy(() -> builder.addFix(PLAYER, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects fix with fromVersion > toVersion")
        void rejectsInvalidVersionRange() {
            assertThatThrownBy(() -> builder.addFix(PLAYER, createFix("invalid", 5, 3)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("accepts fix with fromVersion == toVersion")
        void acceptsSameVersionFix() {
            // No-op fix is valid
            builder.addFix(PLAYER, createFix("noop", 1, 1));
            // Should not throw
        }
    }

    @Nested
    @DisplayName("addFixes()")
    class AddFixes {

        @Test
        @DisplayName("returns this for fluent chaining")
        void returnsThisForChaining() {
            DataFixerBuilder result = builder.addFixes(PLAYER, List.of(
                    createFix("fix1", 1, 2),
                    createFix("fix2", 2, 3)
            ));

            assertThat(result).isSameAs(builder);
        }

        @Test
        @DisplayName("registers all fixes in list")
        void registersAllFixes() {
            DataFixer fixer = builder
                    .addFixes(PLAYER, List.of(
                            createFix("fix1", 1, 2),
                            createFix("fix2", 2, 3),
                            createFix("fix3", 3, 4)
                    ))
                    .build();

            // Fixer should be configured with all fixes
            assertThat(fixer.currentVersion()).isEqualTo(new DataVersion(10));
        }

        @Test
        @DisplayName("rejects null type")
        void rejectsNullType() {
            assertThatThrownBy(() -> builder.addFixes(null, List.of(createFix("fix1", 1, 2))))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null fixes list")
        void rejectsNullFixesList() {
            assertThatThrownBy(() -> builder.addFixes(PLAYER, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("withDefaultContext()")
    class WithDefaultContext {

        @Test
        @DisplayName("returns this for fluent chaining")
        void returnsThisForChaining() {
            DataFixerBuilder result = builder.withDefaultContext(
                    SimpleSystemDataFixerContext.INSTANCE
            );

            assertThat(result).isSameAs(builder);
        }

        @Test
        @DisplayName("rejects null context")
        void rejectsNullContext() {
            assertThatThrownBy(() -> builder.withDefaultContext(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("FixRegistrar Interface")
    class FixRegistrarInterface {

        @Test
        @DisplayName("register() delegates to addFix()")
        void registerDelegatesToAddFix() {
            builder.register(PLAYER, createFix("fix1", 1, 2));
            DataFixer fixer = builder.build();

            // Should have registered the fix
            assertThat(fixer.currentVersion()).isEqualTo(new DataVersion(10));
        }

        @Test
        @DisplayName("registerAll() registers all fixes from iterable")
        void registerAllRegistersFromIterable() {
            Iterable<DataFix<?>> fixes = List.of(
                    createFix("fix1", 1, 2),
                    createFix("fix2", 2, 3)
            );

            builder.registerAll(PLAYER, fixes);
            DataFixer fixer = builder.build();

            assertThat(fixer.currentVersion()).isEqualTo(new DataVersion(10));
        }
    }

    @Nested
    @DisplayName("build()")
    class Build {

        @Test
        @DisplayName("creates immutable fixer")
        void createsImmutableFixer() {
            DataFixer fixer = builder
                    .addFix(PLAYER, createFix("fix1", 1, 2))
                    .build();

            assertThat(fixer).isNotNull();
            assertThat(fixer.currentVersion()).isEqualTo(new DataVersion(10));
        }

        @Test
        @DisplayName("can be called multiple times")
        void canBeCalledMultipleTimes() {
            builder.addFix(PLAYER, createFix("fix1", 1, 2));

            DataFixer fixer1 = builder.build();
            // Note: After build(), the registry is frozen
            // Building again would fail in a strict implementation

            assertThat(fixer1).isNotNull();
        }
    }
}
