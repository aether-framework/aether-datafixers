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

/**
 * Bootstrap loading utilities for the CLI.
 *
 * <p>This package provides utilities for loading
 * {@link de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap}
 * implementations at runtime via class name or ServiceLoader discovery.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.cli.bootstrap.BootstrapLoader}
 *       - Loads bootstraps by class name or via ServiceLoader</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.bootstrap.BootstrapLoadException}
 *       - Exception thrown when bootstrap loading fails</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Load by fully qualified class name
 * DataFixerBootstrap bootstrap = BootstrapLoader.load("com.example.MyBootstrap");
 *
 * // Discover via ServiceLoader
 * DataFixerBootstrap bootstrap = BootstrapLoader.loadFirst();
 * }</pre>
 *
 * @see de.splatgames.aether.datafixers.cli.bootstrap.BootstrapLoader
 * @see de.splatgames.aether.datafixers.api.bootstrap.DataFixerBootstrap
 * @since 0.3.0
 */
package de.splatgames.aether.datafixers.cli.bootstrap;
