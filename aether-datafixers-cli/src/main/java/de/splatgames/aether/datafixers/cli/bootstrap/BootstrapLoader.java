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

import com.google.common.base.Preconditions;
import de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ServiceLoader;

/**
 * Utility class for loading {@link DataFixerBootstrap} implementations at runtime.
 *
 * <p>This class provides multiple methods for discovering and instantiating
 * bootstrap implementations:</p>
 *
 * <h2>Discovery Methods</h2>
 * <ol>
 *   <li><b>Explicit class name</b> via {@link #load(String)} - Used when the CLI
 *       receives the {@code --bootstrap} option with a fully qualified class name</li>
 *   <li><b>ServiceLoader discovery</b> via {@link #discover()} or {@link #loadFirst()} -
 *       Automatically discovers implementations registered in
 *       {@code META-INF/services/de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap}</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods are thread-safe as they do not maintain any mutable state.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Load by explicit class name
 * DataFixerBootstrap bootstrap = BootstrapLoader.load("com.example.MyBootstrap");
 *
 * // Auto-discover via ServiceLoader
 * DataFixerBootstrap bootstrap = BootstrapLoader.loadFirst();
 *
 * // Iterate over all discovered bootstraps
 * for (DataFixerBootstrap bootstrap : BootstrapLoader.discover()) {
 *     System.out.println("Found: " + bootstrap.getClass().getName());
 * }
 * }</pre>
 *
 * @author Erik Pfoertner
 * @see DataFixerBootstrap
 * @see BootstrapLoadException
 * @see java.util.ServiceLoader
 * @since 0.3.0
 */
public final class BootstrapLoader {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods and should not be instantiated.</p>
     */
    private BootstrapLoader() {
        // Utility class
    }

    /**
     * Loads a bootstrap implementation by its fully qualified class name.
     *
     * <p>This method uses reflection to load and instantiate the specified class.
     * The class must satisfy the following requirements:</p>
     * <ul>
     *   <li>Must implement {@link DataFixerBootstrap}</li>
     *   <li>Must have a public no-argument constructor</li>
     *   <li>Must be accessible on the current thread's context class loader</li>
     * </ul>
     *
     * <h4>Error Handling</h4>
     * <p>All reflection-related exceptions are wrapped in {@link BootstrapLoadException}
     * with descriptive error messages to aid debugging:</p>
     * <ul>
     *   <li>{@code ClassNotFoundException} - Class not found on classpath</li>
     *   <li>{@code NoSuchMethodException} - Missing public no-arg constructor</li>
     *   <li>{@code ReflectiveOperationException} - Instantiation failure</li>
     * </ul>
     *
     * @param className the fully qualified class name (e.g., "com.example.MyBootstrap"),
     *                  must not be {@code null}
     * @return the instantiated bootstrap instance, never {@code null}
     * @throws BootstrapLoadException if the class cannot be found, does not implement
     *                                DataFixerBootstrap, lacks a no-arg constructor,
     *                                or instantiation fails
     * @see #discover()
     * @see #loadFirst()
     */
    @NotNull
    public static DataFixerBootstrap load(@NotNull final String className) {
        Preconditions.checkNotNull(className, "className must not be null");

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
     * Discovers all bootstrap implementations via Java's {@link ServiceLoader} mechanism.
     *
     * <p>This method searches for implementations registered in:</p>
     * <pre>
     * META-INF/services/de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap
     * </pre>
     *
     * <p>The returned iterable is lazy - implementations are instantiated on demand
     * as the iterator is consumed. Each call to this method creates a fresh
     * ServiceLoader instance.</p>
     *
     * <h4>Registration</h4>
     * <p>To register a bootstrap for discovery, create a file at:</p>
     * <pre>
     * src/main/resources/META-INF/services/de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap
     * </pre>
     * <p>containing the fully qualified class name of your implementation.</p>
     *
     * @return an iterable over all discovered bootstrap implementations;
     *         may be empty if none are registered
     * @see ServiceLoader#load(Class)
     * @see #loadFirst()
     * @see #load(String)
     */
    @NotNull
    public static Iterable<DataFixerBootstrap> discover() {
        return ServiceLoader.load(DataFixerBootstrap.class);
    }

    /**
     * Loads the first available bootstrap from the ServiceLoader.
     *
     * <p>This is a convenience method that returns the first bootstrap found
     * via {@link #discover()}. Useful when only one bootstrap is expected to
     * be registered in the application.</p>
     *
     * <p>The order in which bootstraps are discovered is not guaranteed and
     * depends on the classpath order. If multiple bootstraps are registered,
     * consider using {@link #discover()} to iterate over all of them or
     * {@link #load(String)} to specify an exact implementation.</p>
     *
     * @return the first discovered bootstrap implementation, never {@code null}
     * @throws BootstrapLoadException if no bootstrap implementations are found
     *                                via ServiceLoader
     * @see #discover()
     * @see #load(String)
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
