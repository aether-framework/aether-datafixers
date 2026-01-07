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

package de.splatgames.aether.datafixers.cli.bootstrap;

import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import de.splatgames.aether.datafixers.api.fix.FixRegistrar;
import de.splatgames.aether.datafixers.api.schema.SchemaRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BootstrapLoader")
class BootstrapLoaderTest {

    @Nested
    @DisplayName("load()")
    class Load {

        @Test
        @DisplayName("loads valid bootstrap class")
        void loadsValidBootstrap() {
            final DataFixerBootstrap bootstrap = BootstrapLoader.load(
                    TestBootstrap.class.getName());

            assertThat(bootstrap).isNotNull();
            assertThat(bootstrap).isInstanceOf(TestBootstrap.class);
        }

        @Test
        @DisplayName("throws BootstrapLoadException for non-existent class")
        void throwsForNonExistentClass() {
            assertThatThrownBy(() -> BootstrapLoader.load("com.nonexistent.Bootstrap"))
                    .isInstanceOf(BootstrapLoadException.class)
                    .hasMessageContaining("Bootstrap class not found");
        }

        @Test
        @DisplayName("throws BootstrapLoadException for class not implementing DataFixerBootstrap")
        void throwsForNonBootstrapClass() {
            assertThatThrownBy(() -> BootstrapLoader.load(String.class.getName()))
                    .isInstanceOf(BootstrapLoadException.class)
                    .hasMessageContaining("does not implement DataFixerBootstrap");
        }

        @Test
        @DisplayName("throws BootstrapLoadException for class without no-arg constructor")
        void throwsForNoNoArgConstructor() {
            assertThatThrownBy(() -> BootstrapLoader.load(
                    BootstrapWithoutNoArgConstructor.class.getName()))
                    .isInstanceOf(BootstrapLoadException.class)
                    .hasMessageContaining("public no-argument constructor");
        }

        @Test
        @DisplayName("throws BootstrapLoadException for abstract class")
        void throwsForAbstractClass() {
            assertThatThrownBy(() -> BootstrapLoader.load(
                    AbstractBootstrap.class.getName()))
                    .isInstanceOf(BootstrapLoadException.class);
        }
    }

    @Nested
    @DisplayName("discover()")
    class Discover {

        @Test
        @DisplayName("returns iterable")
        void returnsIterable() {
            final Iterable<DataFixerBootstrap> bootstraps = BootstrapLoader.discover();

            assertThat(bootstraps).isNotNull();
        }
    }

    // Test fixtures

    public static class TestBootstrap implements DataFixerBootstrap {
        public TestBootstrap() {
            // Public no-arg constructor
        }

        @Override
        public void registerSchemas(@NotNull final SchemaRegistry schemas) {
            // No-op for testing
        }

        @Override
        public void registerFixes(@NotNull final FixRegistrar fixes) {
            // No-op for testing
        }
    }

    public static class BootstrapWithoutNoArgConstructor implements DataFixerBootstrap {
        public BootstrapWithoutNoArgConstructor(final String required) {
            // Requires argument
        }

        @Override
        public void registerSchemas(@NotNull final SchemaRegistry schemas) {
        }

        @Override
        public void registerFixes(@NotNull final FixRegistrar fixes) {
        }
    }

    public abstract static class AbstractBootstrap implements DataFixerBootstrap {
    }
}
