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
 * CLI command implementations for the Aether Datafixers CLI.
 *
 * <p>This package contains the picocli command classes that implement
 * the various CLI operations.</p>
 *
 * <h2>Available Commands</h2>
 * <ul>
 *   <li>{@link de.splatgames.aether.datafixers.cli.command.MigrateCommand migrate}
 *       - Migrate data files from one version to another</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.command.ValidateCommand validate}
 *       - Validate data files and check if migration is needed</li>
 *   <li>{@link de.splatgames.aether.datafixers.cli.command.InfoCommand info}
 *       - Display version and configuration information</li>
 * </ul>
 *
 * <h2>Exit Codes Convention</h2>
 * <ul>
 *   <li>{@code 0} - Success</li>
 *   <li>{@code 1} - Error occurred</li>
 *   <li>{@code 2} - Validation found files needing migration (validate command only)</li>
 * </ul>
 *
 * @see de.splatgames.aether.datafixers.cli.AetherCli
 * @since 0.3.0
 */
package de.splatgames.aether.datafixers.cli.command;
