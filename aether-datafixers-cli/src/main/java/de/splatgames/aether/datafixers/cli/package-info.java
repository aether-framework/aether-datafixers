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
 * Command-line interface for the Aether Datafixers data migration framework.
 *
 * <p>This package provides a CLI tool for migrating, validating, and inspecting
 * data files using Aether Datafixers. The CLI is built with picocli and supports
 * batch processing of multiple files.</p>
 *
 * <h2>Main Entry Point</h2>
 * <p>The {@link de.splatgames.aether.datafixers.cli.AetherCli} class is the main
 * entry point for the CLI application.</p>
 *
 * <h2>Commands</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.cli.command.MigrateCommand migrate}
 *       - Migrate data files between versions</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.command.ValidateCommand validate}
 *       - Check if files need migration</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.command.InfoCommand info}
 *       - Display configuration information</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * # Run the CLI via Maven
 * mvn exec:java -pl aether-datafixers-cli -Dexec.args="--help"
 *
 * # Run with the fat JAR
 * java -jar aether-datafixers-cli-0.3.0-fat.jar migrate --help
 * }</pre>
 *
 * <h2>Package Structure</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.cli.bootstrap} - Bootstrap loader utilities</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.command} - CLI command implementations</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.format} - Format handler SPI and implementations</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.report} - Report formatter implementations</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.util} - Utility classes</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.cli.AetherCli
 * @since 0.3.0
 */
package de.splatgames.aether.datafixers.cli;
