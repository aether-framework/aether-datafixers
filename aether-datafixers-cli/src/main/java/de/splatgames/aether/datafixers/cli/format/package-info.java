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
 * Format handler SPI and implementations for the CLI.
 *
 * <p>This package provides the Service Provider Interface (SPI) for format
 * handlers that enable the CLI to read and write data in various serialization
 * formats.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.cli.format.FormatHandler}
 *       - SPI interface for format handlers</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.format.FormatRegistry}
 *       - Registry for discovering and accessing format handlers</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.format.FormatParseException}
 *       - Exception thrown when parsing fails</li>
 * </ul>
 *
 * <h2>Built-in Handlers</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.cli.format.JsonGsonFormatHandler}
 *       - JSON format using Google Gson</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.format.JsonJacksonFormatHandler}
 *       - JSON format using Jackson Databind</li>
 * </ul>
 *
 * <h2>Extending with Custom Formats</h2>
 * <p>Custom format handlers can be registered via ServiceLoader by adding
 * an entry to {@code META-INF/services/de.splatgames.aether.datafixers.cli.format.FormatHandler}.</p>
 *
 * @see de.splatgames.aether.datafixers.cli.format.FormatHandler
 * @see de.splatgames.aether.datafixers.cli.format.FormatRegistry
 * @since 0.3.0
 */
package de.splatgames.aether.datafixers.cli.format;
