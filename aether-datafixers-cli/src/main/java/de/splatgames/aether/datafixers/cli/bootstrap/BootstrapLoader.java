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
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ServiceLoader;

/**
 * Loads {@link DataFixerBootstrap} implementations.
 *
 * <h2>Discovery Methods</h2>
 * <ol>
 *   <li>Explicit class name via {@code --bootstrap} option</li>
 *   <li>ServiceLoader discovery via {@code META-INF/services}</li>
 * </ol>
 *
 * @author Erik Pfoertner
 * @since 0.3.0
 */
public final class BootstrapLoader {

    private BootstrapLoader() {
        // Utility class
    }

    /**
     * Loads a bootstrap by its fully qualified class name.
     *
     * <p>The class must:</p>
     * <ul>
     *   <li>Implement {@link DataFixerBootstrap}</li>
     *   <li>Have a public no-argument constructor</li>
     * </ul>
     *
     * @param className the fully qualified class name
     * @return the bootstrap instance
     * @throws BootstrapLoadException if loading fails
     */
    @NotNull
    public static DataFixerBootstrap load(@NotNull final String className) {
        try {
            final Class<?> clazz = Class.forName(className);

            if (!DataFixerBootstrap.class.isAssignableFrom(clazz)) {
                throw new BootstrapLoadException(
                        className + " does not implement DataFixerBootstrap");
            }

            final Constructor<?> constructor = clazz.getConstructor();
            return (DataFixerBootstrap) constructor.newInstance();

        } catch (final ClassNotFoundException e) {
            throw new BootstrapLoadException(
                    "Bootstrap class not found: " + className
                            + ". Ensure the class is on the classpath.", e);
        } catch (final NoSuchMethodException e) {
            throw new BootstrapLoadException(
                    "Bootstrap class " + className
                            + " must have a public no-argument constructor", e);
        } catch (final ReflectiveOperationException e) {
            throw new BootstrapLoadException(
                    "Failed to instantiate bootstrap: " + className, e);
        }
    }

    /**
     * Discovers bootstraps via ServiceLoader.
     *
     * @return iterable of discovered bootstraps
     */
    @NotNull
    public static Iterable<DataFixerBootstrap> discover() {
        return ServiceLoader.load(DataFixerBootstrap.class);
    }

    /**
     * Loads the first available bootstrap from ServiceLoader.
     *
     * @return the first bootstrap found
     * @throws BootstrapLoadException if no bootstrap is found
     */
    @NotNull
    public static DataFixerBootstrap loadFirst() {
        for (final DataFixerBootstrap bootstrap : discover()) {
            return bootstrap;
        }
        throw new BootstrapLoadException(
                "No DataFixerBootstrap found via ServiceLoader. "
                        + "Add a META-INF/services file or use the --bootstrap option.");
    }
}
